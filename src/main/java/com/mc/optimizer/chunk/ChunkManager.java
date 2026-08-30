/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Chunk
 *  org.bukkit.World
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.event.world.ChunkLoadEvent
 *  org.bukkit.event.world.ChunkUnloadEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.mc.optimizer.chunk;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.chunk.config.ChunkConfigManager;
import com.mc.optimizer.utils.ServerUtils;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class ChunkManager
implements Listener {
    private final OptimizerPlugin plugin;
    private final ChunkConfigManager config;
    private final Logger logger;
    private BukkitTask chunkManagementTask;
    private final Map<String, Long> lastAccessedChunks = new ConcurrentHashMap<String, Long>();
    private final Map<String, Integer> playerActivityInChunks = new ConcurrentHashMap<String, Integer>();
    private int maxViewDistance;
    private int spawnChunkRadius;
    private int maxChunksPerTick;
    private int unloadAfterSeconds;
    private boolean prioritizePlayerChunks;
    private int chunksLoadedThisTick = 0;
    private int chunksUnloadedTotal = 0;
    private int chunksLoadedTotal = 0;

    public ChunkManager(OptimizerPlugin plugin, ChunkConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.loadConfiguration();
        plugin.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)plugin);
        this.startChunkManagementTask();
        this.logger.info("Chunk Manager initialized. Max chunks per tick: " + this.maxChunksPerTick);
    }

    private void loadConfiguration() {
        this.maxViewDistance = this.config.getMaxViewDistance();
        this.spawnChunkRadius = this.config.getSpawnChunkRadius();
        this.maxChunksPerTick = this.config.getMaxChunksPerTick();
        this.unloadAfterSeconds = this.config.getUnloadAfterSeconds();
        this.prioritizePlayerChunks = this.config.isPrioritizePlayerChunks();
        if (this.maxViewDistance > 0) {
            for (World world : Bukkit.getWorlds()) {
                int currentViewDistance = world.getViewDistance();
                if (currentViewDistance <= this.maxViewDistance) continue;
                world.setViewDistance(this.maxViewDistance);
                this.logger.info("Reduced view distance in world " + world.getName() + " from " + currentViewDistance + " to " + this.maxViewDistance);
            }
        }
    }

    private void startChunkManagementTask() {
        this.chunkManagementTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, this::managedChunks, 20L, 20L);
    }

    private void managedChunks() {
        this.chunksLoadedThisTick = 0;
        long currentTime = System.currentTimeMillis() / 1000L;
        for (World world : Bukkit.getWorlds()) {
            if (this.shouldKeepWorldFullyLoaded(world)) continue;
            this.unloadUnusedChunks(world, currentTime);
            if (this.spawnChunkRadius <= 0) continue;
            this.ensureSpawnChunksLoaded(world);
        }
        if (currentTime % 60L == 0L) {
            this.cleanTrackingMaps();
        }
    }

    private boolean shouldKeepWorldFullyLoaded(World world) {
        return false;
    }

    private void unloadUnusedChunks(World world, long currentTime) {
        Chunk[] loadedChunks = world.getLoadedChunks();
        if (this.prioritizePlayerChunks) {
            Arrays.sort(loadedChunks, (c1, c2) -> {
                String key1 = this.getChunkKey((Chunk)c1);
                String key2 = this.getChunkKey((Chunk)c2);
                int activity1 = this.playerActivityInChunks.getOrDefault(key1, 0);
                int activity2 = this.playerActivityInChunks.getOrDefault(key2, 0);
                return Integer.compare(activity2, activity1);
            });
        }
        for (Chunk chunk : loadedChunks) {
            Long lastAccessed;
            String key = this.getChunkKey(chunk);
            if (this.isSpawnChunk(chunk) || (lastAccessed = this.lastAccessedChunks.get(key)) == null || currentTime - lastAccessed <= (long)this.unloadAfterSeconds || !this.isChunkSafeToUnload(chunk)) continue;
            world.unloadChunkRequest(chunk.getX(), chunk.getZ());
            ++this.chunksUnloadedTotal;
            if (!this.config.isDebugEnabled()) continue;
            this.logger.fine("Unloaded inactive chunk at " + key);
        }
    }

    private boolean isChunkSafeToUnload(Chunk chunk) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getLocation().getChunk().equals((Object)chunk)) continue;
            return false;
        }
        return true;
    }

    private void ensureSpawnChunksLoaded(World world) {
        int spawnX = world.getSpawnLocation().getBlockX() >> 4;
        int spawnZ = world.getSpawnLocation().getBlockZ() >> 4;
        for (int x = -this.spawnChunkRadius; x <= this.spawnChunkRadius; ++x) {
            for (int z = -this.spawnChunkRadius; z <= this.spawnChunkRadius; ++z) {
                int chunkZ;
                int chunkX;
                if (this.chunksLoadedThisTick >= this.maxChunksPerTick) {
                    return;
                }
                if (x * x + z * z > this.spawnChunkRadius * this.spawnChunkRadius || world.isChunkLoaded(chunkX = spawnX + x, chunkZ = spawnZ + z)) continue;
                world.loadChunk(chunkX, chunkZ, true);
                ++this.chunksLoadedThisTick;
                ++this.chunksLoadedTotal;
                this.lastAccessedChunks.put(world.getName() + ":" + chunkX + ":" + chunkZ, System.currentTimeMillis() / 1000L);
            }
        }
    }

    private boolean isSpawnChunk(Chunk chunk) {
        int dz;
        World world = chunk.getWorld();
        int spawnX = world.getSpawnLocation().getBlockX() >> 4;
        int spawnZ = world.getSpawnLocation().getBlockZ() >> 4;
        int dx = chunk.getX() - spawnX;
        return dx * dx + (dz = chunk.getZ() - spawnZ) * dz <= this.spawnChunkRadius * this.spawnChunkRadius;
    }

    private void cleanTrackingMaps() {
        long currentTime = System.currentTimeMillis() / 1000L;
        this.lastAccessedChunks.entrySet().removeIf(entry -> currentTime - (Long)entry.getValue() > (long)(this.unloadAfterSeconds * 2));
        this.playerActivityInChunks.replaceAll((k, v) -> v > 0 ? v - 1 : 0);
        this.playerActivityInChunks.entrySet().removeIf(entry -> (Integer)entry.getValue() <= 0);
    }

    private String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    public void recordPlayerActivity(Player player) {
        Chunk chunk = player.getLocation().getChunk();
        String key = this.getChunkKey(chunk);
        this.playerActivityInChunks.put(key, this.playerActivityInChunks.getOrDefault(key, 0) + 1);
        this.lastAccessedChunks.put(key, System.currentTimeMillis() / 1000L);
    }

    public Map<String, Object> getStats() {
        HashMap<String, Object> stats = new HashMap<String, Object>();
        stats.put("chunksLoadedTotal", this.chunksLoadedTotal);
        stats.put("chunksUnloadedTotal", this.chunksUnloadedTotal);
        stats.put("trackedChunks", this.lastAccessedChunks.size());
        stats.put("activePlayerChunks", this.playerActivityInChunks.size());
        int loadedChunksCount = 0;
        for (World world : Bukkit.getWorlds()) {
            loadedChunksCount += world.getLoadedChunks().length;
        }
        stats.put("currentlyLoadedChunks", loadedChunksCount);
        return stats;
    }

    public void shutdown() {
        if (this.chunkManagementTask != null) {
            this.chunkManagementTask.cancel();
        }
        this.lastAccessedChunks.clear();
        this.playerActivityInChunks.clear();
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onChunkLoad(ChunkLoadEvent event) {
        String key = this.getChunkKey(event.getChunk());
        this.lastAccessedChunks.put(key, System.currentTimeMillis() / 1000L);
        if (!event.isNewChunk() && this.config.isDebugEnabled()) {
            this.logger.fine("Chunk loaded: " + key);
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onChunkUnload(ChunkUnloadEvent event) {
        String key;
        Integer activity;
        if (this.prioritizePlayerChunks && (activity = this.playerActivityInChunks.get(key = this.getChunkKey(event.getChunk()))) != null && activity > 10 && !ServerUtils.isMemoryPressureHigh()) {
            block5: {
                try {
                    Method setCancelledMethod = event.getClass().getMethod("setCancelled", Boolean.TYPE);
                    setCancelledMethod.invoke((Object)event, true);
                }
                catch (Exception e) {
                    if (!this.config.isDebugEnabled()) break block5;
                    this.logger.warning("Cannot cancel chunk unload in this version: " + e.getMessage());
                }
            }
            if (this.config.isDebugEnabled()) {
                this.logger.fine("Prevented unload of high-activity chunk: " + key);
            }
            return;
        }
        if (this.config.isDebugEnabled()) {
            this.logger.fine("Chunk unloaded: " + this.getChunkKey(event.getChunk()));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player;
        if (this.maxViewDistance > 0 && (player = event.getPlayer()).getClientViewDistance() > this.maxViewDistance) {
            player.sendMessage("\u00a76[MCOptimizer] \u00a7eServer has limited view distance to " + this.maxViewDistance + " chunks.");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
            Chunk chunk = player.getLocation().getChunk();
            String key = this.getChunkKey(chunk);
            int currentActivity = this.playerActivityInChunks.getOrDefault(key, 0);
            if (currentActivity > 0) {
                this.playerActivityInChunks.put(key, currentActivity - 1);
            }
        }, 1L);
    }

    public boolean isEnabled() {
        try {
            return this.plugin.getConfig().getBoolean("chunks.enabled", true);
        }
        catch (Exception e) {
            this.logger.warning("Error checking if chunk manager is enabled: " + e.getMessage());
            return true;
        }
    }
}

