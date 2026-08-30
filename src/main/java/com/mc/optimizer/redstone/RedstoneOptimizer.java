/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Chunk
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockPhysicsEvent
 *  org.bukkit.event.block.BlockPistonExtendEvent
 *  org.bukkit.event.block.BlockPistonRetractEvent
 *  org.bukkit.event.block.BlockRedstoneEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.mc.optimizer.redstone;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.config.ConfigManager;
import com.mc.optimizer.utils.ServerUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class RedstoneOptimizer
implements Listener {
    private final OptimizerPlugin plugin;
    private final ConfigManager config;
    private final Logger logger;
    private boolean enabled;
    private int maxUpdatesPerTick;
    private boolean disableWhenNoPlayersNearby;
    private int nearbyDistance;
    private int limitPistonChainLength;
    private boolean smartHoppers;
    private int updatesThisTick = 0;
    private final Map<Location, Long> lastRedstoneActivity = new ConcurrentHashMap<Location, Long>();
    private final Map<Location, Integer> pistonChains = new ConcurrentHashMap<Location, Integer>();
    private final Set<Location> activeHoppers = Collections.newSetFromMap(new ConcurrentHashMap());
    private BukkitTask cleanupTask;
    private BukkitTask hopperCheckTask;
    private int cancelledUpdates = 0;
    private int cancelledPistons = 0;
    private int optimizedHoppers = 0;

    public RedstoneOptimizer(OptimizerPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.loadConfiguration();
        if (this.enabled) {
            plugin.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)plugin);
            this.cleanupTask = Bukkit.getScheduler().runTaskTimer((Plugin)plugin, this::cleanupTrackingAndResetTickCounter, 1200L, 1200L);
            Bukkit.getScheduler().runTaskTimer((Plugin)plugin, this::resetTickCounter, 1L, 1L);
            if (this.smartHoppers) {
                this.hopperCheckTask = Bukkit.getScheduler().runTaskTimer((Plugin)plugin, this::optimizeHoppers, 200L, 600L);
            }
            this.logger.info("Redstone Optimizer initialized. Max updates per tick: " + this.maxUpdatesPerTick);
        } else {
            this.logger.info("Redstone Optimizer disabled in configuration");
        }
    }

    private void loadConfiguration() {
        try {
            this.enabled = this.plugin.getConfig().getBoolean("redstone.enabled", true);
            this.maxUpdatesPerTick = this.plugin.getConfig().getInt("redstone.max-updates-per-tick", 100);
            this.disableWhenNoPlayersNearby = this.plugin.getConfig().getBoolean("redstone.disable-when-no-players-nearby", true);
            this.nearbyDistance = this.plugin.getConfig().getInt("redstone.nearby-distance", 16);
            this.limitPistonChainLength = this.plugin.getConfig().getInt("redstone.limit-piston-chain-length", 12);
            this.smartHoppers = this.plugin.getConfig().getBoolean("redstone.smart-hoppers", true);
        }
        catch (Exception e) {
            this.logger.warning("Error loading redstone optimization configuration: " + e.getMessage());
            this.enabled = true;
            this.maxUpdatesPerTick = 100;
            this.disableWhenNoPlayersNearby = true;
            this.nearbyDistance = 16;
            this.limitPistonChainLength = 12;
            this.smartHoppers = true;
        }
    }

    private void cleanupTracking() {
        long currentTime = System.currentTimeMillis();
        this.lastRedstoneActivity.entrySet().removeIf(entry -> currentTime - (Long)entry.getValue() > 300000L);
        this.pistonChains.clear();
    }

    private boolean arePlayersNearby(Location location) {
        if (!this.disableWhenNoPlayersNearby) {
            return true;
        }
        int distanceSquared = this.nearbyDistance * this.nearbyDistance;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals((Object)location.getWorld()) || !(player.getLocation().distanceSquared(location) <= (double)distanceSquared)) continue;
            return true;
        }
        return false;
    }

    public void resetTickCounter() {
        this.updatesThisTick = 0;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onRedstone(BlockRedstoneEvent event) {
        if (!this.enabled) {
            return;
        }
        if (this.updatesThisTick < 0) {
            this.updatesThisTick = 0;
        }
        Block block = event.getBlock();
        Location location = block.getLocation();
        if (this.shouldProcessRedstoneUpdate(block)) {
            this.lastRedstoneActivity.put(location, System.currentTimeMillis());
            ++this.updatesThisTick;
        } else {
            event.setNewCurrent(event.getOldCurrent());
            ++this.cancelledUpdates;
        }
    }

    private boolean shouldProcessRedstoneUpdate(Block block) {
        if (!ServerUtils.isMemoryPressureHigh() && !ServerUtils.isCPUPressureHigh()) {
            return true;
        }
        if (this.disableWhenNoPlayersNearby && !this.arePlayersNearby(block.getLocation())) {
            return false;
        }
        return this.updatesThisTick < this.maxUpdatesPerTick;
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!this.enabled || this.limitPistonChainLength <= 0) {
            return;
        }
        if (event.getBlocks().size() > this.limitPistonChainLength) {
            Block piston = event.getBlock();
            Location pistonLoc = piston.getLocation();
            event.setCancelled(true);
            ++this.cancelledPistons;
            if (this.config.isDebugEnabled()) {
                this.logger.fine("Cancelled piston extension at " + pistonLoc.getBlockX() + "," + pistonLoc.getBlockY() + "," + pistonLoc.getBlockZ() + " (moved blocks: " + event.getBlocks().size() + ")");
            }
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!this.enabled || this.limitPistonChainLength <= 0) {
            return;
        }
        if (event.getBlocks().size() > this.limitPistonChainLength) {
            Block piston = event.getBlock();
            Location pistonLoc = piston.getLocation();
            event.setCancelled(true);
            ++this.cancelledPistons;
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (!this.enabled) {
            return;
        }
        Block block = event.getBlock();
        Material type = block.getType();
        if (this.isRedstoneComponent(type) && this.updatesThisTick >= this.maxUpdatesPerTick && !this.arePlayersNearby(block.getLocation())) {
            event.setCancelled(true);
            ++this.cancelledUpdates;
        }
    }

    private boolean isRedstoneComponent(Material type) {
        switch (type) {
            case REDSTONE_WIRE: 
            case REPEATER: 
            case COMPARATOR: 
            case REDSTONE_TORCH: 
            case REDSTONE_WALL_TORCH: 
            case REDSTONE_LAMP: 
            case REDSTONE_BLOCK: 
            case LEVER: 
            case STONE_BUTTON: 
            case OAK_BUTTON: 
            case SPRUCE_BUTTON: 
            case BIRCH_BUTTON: 
            case JUNGLE_BUTTON: 
            case ACACIA_BUTTON: 
            case DARK_OAK_BUTTON: 
            case CRIMSON_BUTTON: 
            case WARPED_BUTTON: 
            case HOPPER: 
            case DROPPER: 
            case DISPENSER: 
            case OBSERVER: 
            case PISTON: 
            case STICKY_PISTON: 
            case TRIPWIRE: 
            case TRIPWIRE_HOOK: 
            case DAYLIGHT_DETECTOR: 
            case TARGET: {
                return true;
            }
        }
        return false;
    }

    private void optimizeHoppers() {
        if (!this.smartHoppers) {
            return;
        }
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                this.optimizeHoppersInChunk(chunk);
            }
        }
    }

    private void optimizeHoppersInChunk(Chunk chunk) {
        for (int x = 0; x < 16; ++x) {
            for (int z = 0; z < 16; ++z) {
                for (int y = 0; y < chunk.getWorld().getMaxHeight(); ++y) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block.getType() != Material.HOPPER) continue;
                    this.optimizeHopper(block);
                }
            }
        }
    }

    private void optimizeHopper(Block hopper) {
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
            Location hopperLoc = hopper.getLocation();
            if (!this.activeHoppers.contains(hopperLoc)) {
                this.activeHoppers.add(hopperLoc);
                ++this.optimizedHoppers;
            }
        });
    }

    public Map<String, Object> getStats() {
        HashMap<String, Object> stats = new HashMap<String, Object>();
        stats.put("cancelledUpdates", this.cancelledUpdates);
        stats.put("cancelledPistons", this.cancelledPistons);
        stats.put("optimizedHoppers", this.optimizedHoppers);
        stats.put("activeRedstoneLocations", this.lastRedstoneActivity.size());
        stats.put("activeHoppers", this.activeHoppers.size());
        stats.put("updatesThisTick", this.updatesThisTick);
        return stats;
    }

    public void shutdown() {
        if (this.cleanupTask != null) {
            this.cleanupTask.cancel();
        }
        if (this.hopperCheckTask != null) {
            this.hopperCheckTask.cancel();
        }
        this.lastRedstoneActivity.clear();
        this.pistonChains.clear();
        this.activeHoppers.clear();
    }

    public boolean isEnabled() {
        try {
            return this.plugin.getConfig().getBoolean("redstone.enabled", true);
        }
        catch (Exception e) {
            this.logger.warning("Error checking if redstone optimizer is enabled: " + e.getMessage());
            return true;
        }
    }
}

