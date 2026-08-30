/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.AsyncPlayerChatEvent
 *  org.bukkit.event.player.PlayerCommandPreprocessEvent
 *  org.bukkit.event.player.PlayerInteractEvent
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.event.player.PlayerMoveEvent
 *  org.bukkit.event.player.PlayerQuitEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.mc.optimizer.afk;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.config.ConfigManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class AfkManager
implements Listener {
    private final OptimizerPlugin plugin;
    private final ConfigManager config;
    private final Logger logger;
    private boolean enabled;
    private int afkTimeoutSeconds;
    private boolean reduceViewDistance;
    private int afkViewDistance;
    private boolean notifyPlayers;
    private boolean detectNoPositionChange;
    private final Map<UUID, AfkPlayerData> playerData = new ConcurrentHashMap<UUID, AfkPlayerData>();
    private BukkitTask afkCheckTask;

    public AfkManager(OptimizerPlugin plugin, ConfigManager config) {
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
        this.afkTimeoutSeconds = 300;
        this.reduceViewDistance = true;
        this.afkViewDistance = 4;
        this.notifyPlayers = true;
        this.detectNoPositionChange = true;
        try {
            this.enabled = this.plugin.getConfig().getBoolean("afk.enabled", true);
            this.afkTimeoutSeconds = this.plugin.getConfig().getInt("afk.timeout-seconds", 300);
            this.reduceViewDistance = this.plugin.getConfig().getBoolean("afk.reduce-view-distance", true);
            this.afkViewDistance = this.plugin.getConfig().getInt("afk.afk-view-distance", 4);
            this.notifyPlayers = this.plugin.getConfig().getBoolean("afk.notify-players", true);
            this.detectNoPositionChange = this.plugin.getConfig().getBoolean("afk.detect-no-position-change", true);
        }
        catch (Exception e) {
            this.logger.warning("Error loading AFK Manager configuration: " + e.getMessage());
        }
    }

    private void initialize() {
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this.plugin);
        for (Player player : Bukkit.getOnlinePlayers()) {
            this.addPlayer(player);
        }
        this.startAfkCheckTask();
        this.logger.info("AFK manager initialized");
    }

    private void startAfkCheckTask() {
        if (this.afkCheckTask != null) {
            this.afkCheckTask.cancel();
        }
        this.afkCheckTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, this::checkAfkPlayers, 20L, 20L);
    }

    private void checkAfkPlayers() {
        if (!this.enabled) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        long afkTimeout = (long)this.afkTimeoutSeconds * 1000L;
        for (Map.Entry<UUID, AfkPlayerData> entry : this.playerData.entrySet()) {
            Player player;
            UUID playerId = entry.getKey();
            AfkPlayerData data = entry.getValue();
            if (data.isAfk() || (player = Bukkit.getPlayer((UUID)playerId)) == null || !player.isOnline()) continue;
            if (currentTime - data.getLastActivityTime() > afkTimeout) {
                if (this.detectNoPositionChange) {
                    Location current = player.getLocation();
                    Location last = data.getLastLocation();
                    if (last != null && this.isSameLocation(current, last)) {
                        this.setPlayerAfk(player, true);
                        continue;
                    }
                    data.setLastLocation(current);
                    continue;
                }
                this.setPlayerAfk(player, true);
                continue;
            }
            if (!this.detectNoPositionChange) continue;
            data.setLastLocation(player.getLocation());
        }
    }

    private boolean isSameLocation(Location loc1, Location loc2) {
        if (loc1.getWorld() != loc2.getWorld()) {
            return false;
        }
        double distanceSquared = loc1.distanceSquared(loc2);
        return distanceSquared < 0.2;
    }

    private void setPlayerAfk(Player player, boolean afk) {
        UUID playerId = player.getUniqueId();
        AfkPlayerData data = this.playerData.get(playerId);
        if (data == null) {
            return;
        }
        data.setAfk(afk);
        if (afk) {
            data.setOriginalViewDistance(player.getClientViewDistance());
            if (this.reduceViewDistance) {
                // empty if block
            }
            if (this.notifyPlayers) {
                player.sendMessage(String.valueOf(ChatColor.GRAY) + "You are now AFK. Resources for your client have been reduced.");
            }
            if (this.config.isDebugEnabled()) {
                this.logger.info("Player " + player.getName() + " is now AFK");
            }
        } else {
            int originalViewDistance;
            if (!this.reduceViewDistance || (originalViewDistance = data.getOriginalViewDistance()) > 0) {
                // empty if block
            }
            if (this.notifyPlayers) {
                player.sendMessage(String.valueOf(ChatColor.GREEN) + "Welcome back! You are no longer AFK.");
            }
            data.updateActivityTime();
            if (this.config.isDebugEnabled()) {
                this.logger.info("Player " + player.getName() + " is no longer AFK");
            }
        }
    }

    private void addPlayer(Player player) {
        UUID playerId = player.getUniqueId();
        AfkPlayerData data = new AfkPlayerData();
        data.updateActivityTime();
        data.setLastLocation(player.getLocation());
        data.setOriginalViewDistance(player.getClientViewDistance());
        this.playerData.put(playerId, data);
    }

    private void removePlayer(Player player) {
        int originalViewDistance;
        UUID playerId = player.getUniqueId();
        AfkPlayerData data = this.playerData.get(playerId);
        if (data == null || !data.isAfk() || !this.reduceViewDistance || (originalViewDistance = data.getOriginalViewDistance()) > 0) {
            // empty if block
        }
        this.playerData.remove(playerId);
    }

    private void registerActivity(Player player) {
        if (!this.enabled) {
            return;
        }
        UUID playerId = player.getUniqueId();
        AfkPlayerData data = this.playerData.get(playerId);
        if (data == null) {
            this.addPlayer(player);
        } else {
            data.updateActivityTime();
            if (data.isAfk()) {
                this.setPlayerAfk(player, false);
            }
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!this.enabled) {
            return;
        }
        this.addPlayer(event.getPlayer());
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (!this.enabled) {
            return;
        }
        this.removePlayer(event.getPlayer());
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!this.enabled) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to != null && (from.getBlockX() != to.getBlockX() || from.getBlockY() != to.getBlockY() || from.getBlockZ() != to.getBlockZ() || from.getYaw() != to.getYaw() || from.getPitch() != to.getPitch())) {
            this.registerActivity(event.getPlayer());
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!this.enabled) {
            return;
        }
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> this.registerActivity(event.getPlayer()));
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        if (!this.enabled) {
            return;
        }
        this.registerActivity(event.getPlayer());
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!this.enabled) {
            return;
        }
        this.registerActivity(event.getPlayer());
    }

    public Map<String, Object> getStats() {
        HashMap<String, Object> stats = new HashMap<String, Object>();
        stats.put("enabled", this.enabled);
        stats.put("trackedPlayers", this.playerData.size());
        long afkPlayers = this.playerData.values().stream().filter(AfkPlayerData::isAfk).count();
        stats.put("afkPlayers", afkPlayers);
        return stats;
    }

    public boolean isPlayerAfk(Player player) {
        if (!this.enabled || player == null) {
            return false;
        }
        AfkPlayerData data = this.playerData.get(player.getUniqueId());
        return data != null && data.isAfk();
    }

    public void reload() {
        AsyncPlayerChatEvent.getHandlerList().unregister((Listener)this);
        PlayerCommandPreprocessEvent.getHandlerList().unregister((Listener)this);
        PlayerInteractEvent.getHandlerList().unregister((Listener)this);
        PlayerJoinEvent.getHandlerList().unregister((Listener)this);
        PlayerMoveEvent.getHandlerList().unregister((Listener)this);
        PlayerQuitEvent.getHandlerList().unregister((Listener)this);
        if (this.afkCheckTask != null) {
            this.afkCheckTask.cancel();
            this.afkCheckTask = null;
        }
        if (this.reduceViewDistance) {
            for (Map.Entry<UUID, AfkPlayerData> entry : this.playerData.entrySet()) {
                int originalViewDistance;
                Player player;
                if (entry.getValue().isAfk() && (player = Bukkit.getPlayer((UUID)entry.getKey())) != null && player.isOnline() && (originalViewDistance = entry.getValue().getOriginalViewDistance()) <= 0) continue;
            }
        }
        this.playerData.clear();
        this.loadConfiguration();
        if (this.enabled) {
            this.initialize();
        }
    }

    public void shutdown() {
        AsyncPlayerChatEvent.getHandlerList().unregister((Listener)this);
        PlayerCommandPreprocessEvent.getHandlerList().unregister((Listener)this);
        PlayerInteractEvent.getHandlerList().unregister((Listener)this);
        PlayerJoinEvent.getHandlerList().unregister((Listener)this);
        PlayerMoveEvent.getHandlerList().unregister((Listener)this);
        PlayerQuitEvent.getHandlerList().unregister((Listener)this);
        if (this.afkCheckTask != null) {
            this.afkCheckTask.cancel();
            this.afkCheckTask = null;
        }
        if (this.reduceViewDistance) {
            for (Map.Entry<UUID, AfkPlayerData> entry : this.playerData.entrySet()) {
                int originalViewDistance;
                Player player;
                if (entry.getValue().isAfk() && (player = Bukkit.getPlayer((UUID)entry.getKey())) != null && player.isOnline() && (originalViewDistance = entry.getValue().getOriginalViewDistance()) <= 0) continue;
            }
        }
        this.playerData.clear();
        this.logger.info("AFK manager shutdown");
    }

    private static class AfkPlayerData {
        private long lastActivityTime = System.currentTimeMillis();
        private boolean afk = false;
        private Location lastLocation = null;
        private int originalViewDistance = 0;

        public void updateActivityTime() {
            this.lastActivityTime = System.currentTimeMillis();
        }

        public long getLastActivityTime() {
            return this.lastActivityTime;
        }

        public boolean isAfk() {
            return this.afk;
        }

        public void setAfk(boolean afk) {
            this.afk = afk;
        }

        public Location getLastLocation() {
            return this.lastLocation;
        }

        public void setLastLocation(Location lastLocation) {
            this.lastLocation = lastLocation;
        }

        public int getOriginalViewDistance() {
            return this.originalViewDistance;
        }

        public void setOriginalViewDistance(int originalViewDistance) {
            this.originalViewDistance = originalViewDistance;
        }
    }
}

