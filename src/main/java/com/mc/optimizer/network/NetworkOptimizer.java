/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.World
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.EntitySpawnEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.util.Vector
 */
package com.mc.optimizer.network;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.config.ConfigManager;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class NetworkOptimizer
implements Listener {
    private final OptimizerPlugin plugin;
    private final ConfigManager config;
    private final Logger logger;
    private boolean enabled;
    private boolean batchEntityPackets;
    private boolean prioritizePlayerFocus;
    private boolean reduceDistantPackets;
    private int compressionLevel;
    private int visibilityPercent;
    private final Map<UUID, PlayerPacketInfo> playerInfo = new ConcurrentHashMap<UUID, PlayerPacketInfo>();
    private BukkitTask packetUpdateTask;
    private BukkitTask playerMonitorTask;

    public NetworkOptimizer(OptimizerPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.loadConfiguration();
        if (this.enabled) {
            this.initialize();
        }
    }

    private void loadConfiguration() {
        this.enabled = true;
        this.batchEntityPackets = true;
        this.prioritizePlayerFocus = true;
        this.reduceDistantPackets = true;
        this.compressionLevel = 3;
        this.visibilityPercent = 70;
        try {
            if (this.config != null) {
                try {
                    Method method = this.config.getClass().getMethod("isNetworkOptimizationEnabled", new Class[0]);
                    this.enabled = (Boolean)method.invoke((Object)this.config, new Object[0]);
                }
                catch (Exception e) {
                    this.logger.warning("Could not load network optimization configuration: " + e.getMessage());
                }
            }
        }
        catch (Exception e) {
            this.logger.warning("Error loading network optimization configuration: " + e.getMessage());
        }
    }

    private void initialize() {
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this.plugin);
        this.startUpdateTask();
        this.startPlayerMonitorTask();
        this.logger.info("Network optimizer initialized");
    }

    private void startUpdateTask() {
        if (this.packetUpdateTask != null) {
            this.packetUpdateTask.cancel();
        }
        this.packetUpdateTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, this::updatePacketPriorities, 20L, 20L);
    }

    private void startPlayerMonitorTask() {
        if (this.playerMonitorTask != null) {
            this.playerMonitorTask.cancel();
        }
        this.playerMonitorTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, this::monitorPlayerFocus, 10L, 10L);
    }

    private void updatePacketPriorities() {
        if (!this.enabled) {
            return;
        }
        for (PlayerPacketInfo info : this.playerInfo.values()) {
            Player player = Bukkit.getPlayer((UUID)info.getPlayerId());
            if (player == null || !player.isOnline()) continue;
            this.updateEntityVisibility(player, info);
        }
    }

    private void monitorPlayerFocus() {
        if (!this.enabled || !this.prioritizePlayerFocus) {
            return;
        }
        for (PlayerPacketInfo info : this.playerInfo.values()) {
            Player player = Bukkit.getPlayer((UUID)info.getPlayerId());
            if (player == null || !player.isOnline()) continue;
            Location location = player.getLocation();
            info.setFocusDirection(location.getDirection());
            info.setLastPosition(location);
        }
    }

    private void updateEntityVisibility(Player player, PlayerPacketInfo info) {
        Location playerLoc = player.getLocation();
        World world = player.getWorld();
        List entities = world.getEntities();
        info.getVisibleEntities().clear();
        for (Entity entity : entities) {
            if (entity.equals((Object)player)) continue;
            if (entity instanceof Player) {
                info.getVisibleEntities().add(entity.getUniqueId());
                continue;
            }
            double distance = entity.getLocation().distance(playerLoc);
            if (this.reduceDistantPackets && distance > (double)(player.getClientViewDistance() * 16) * ((double)this.visibilityPercent / 100.0)) continue;
            if (this.prioritizePlayerFocus) {
                Location entityLoc = entity.getLocation();
                double dotProduct = info.getFocusDirection().dot(entityLoc.toVector().subtract(playerLoc.toVector()).normalize());
                if (dotProduct < -0.5 && distance > 16.0 && System.currentTimeMillis() % 1000L > 800L) continue;
            }
            info.getVisibleEntities().add(entity.getUniqueId());
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!this.enabled) {
            return;
        }
        Player player = event.getPlayer();
        this.playerInfo.put(player.getUniqueId(), new PlayerPacketInfo(player.getUniqueId()));
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!this.enabled) {
            return;
        }
        Player player = event.getPlayer();
        this.playerInfo.remove(player.getUniqueId());
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!this.enabled || !this.prioritizePlayerFocus) {
            return;
        }
        Player player = event.getPlayer();
        PlayerPacketInfo info = this.playerInfo.get(player.getUniqueId());
        if (info != null) {
            Location from = event.getFrom();
            Location to = event.getTo();
            if (to != null && (from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch())) {
                info.setFocusDirection(to.getDirection());
                info.setLastPosition(to);
            }
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!this.enabled || !this.batchEntityPackets) {
            return;
        }
    }

    public Map<String, Object> getStats() {
        HashMap<String, Object> stats = new HashMap<String, Object>();
        stats.put("enabled", this.enabled);
        stats.put("batchEntityPackets", this.batchEntityPackets);
        stats.put("prioritizePlayerFocus", this.prioritizePlayerFocus);
        stats.put("reduceDistantPackets", this.reduceDistantPackets);
        stats.put("visibilityPercent", this.visibilityPercent);
        stats.put("trackedPlayers", this.playerInfo.size());
        if (!this.playerInfo.isEmpty()) {
            double avgVisible = this.playerInfo.values().stream().mapToInt(info -> info.getVisibleEntities().size()).average().orElse(0.0);
            stats.put("averageVisibleEntities", avgVisible);
        } else {
            stats.put("averageVisibleEntities", 0);
        }
        return stats;
    }

    public void reload() {
        if (this.packetUpdateTask != null) {
            this.packetUpdateTask.cancel();
            this.packetUpdateTask = null;
        }
        if (this.playerMonitorTask != null) {
            this.playerMonitorTask.cancel();
            this.playerMonitorTask = null;
        }
        this.playerInfo.clear();
        this.loadConfiguration();
        if (this.enabled) {
            this.initialize();
        }
    }

    public void shutdown() {
        EntitySpawnEvent.getHandlerList().unregister((Listener)this);
        PlayerJoinEvent.getHandlerList().unregister((Listener)this);
        PlayerQuitEvent.getHandlerList().unregister((Listener)this);
        PlayerMoveEvent.getHandlerList().unregister((Listener)this);
        if (this.packetUpdateTask != null) {
            this.packetUpdateTask.cancel();
            this.packetUpdateTask = null;
        }
        if (this.playerMonitorTask != null) {
            this.playerMonitorTask.cancel();
            this.playerMonitorTask = null;
        }
        this.playerInfo.clear();
        this.logger.info("Network optimizer shutdown");
    }

    private static class PlayerPacketInfo {
        private final UUID playerId;
        private final Set<UUID> visibleEntities;
        private Vector focusDirection;
        private Location lastPosition;

        public PlayerPacketInfo(UUID playerId) {
            this.playerId = playerId;
            this.visibleEntities = new HashSet<UUID>();
            this.focusDirection = new Vector(0, 0, 1);
            this.lastPosition = null;
        }

        public UUID getPlayerId() {
            return this.playerId;
        }

        public Set<UUID> getVisibleEntities() {
            return this.visibleEntities;
        }

        public Vector getFocusDirection() {
            return this.focusDirection;
        }

        public void setFocusDirection(Vector focusDirection) {
            this.focusDirection = focusDirection;
        }

        public Location getLastPosition() {
            return this.lastPosition;
        }

        public void setLastPosition(Location lastPosition) {
            this.lastPosition = lastPosition;
        }
    }
}

