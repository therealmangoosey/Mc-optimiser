/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Chunk
 *  org.bukkit.World
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.mc.optimizer.metrics;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.config.ConfigManager;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class PerformanceMonitor {
    private final OptimizerPlugin plugin;
    private final ConfigManager config;
    private final Logger logger;
    private int monitorInterval;
    private int historySize;
    private boolean trackTPS;
    private boolean trackMemory;
    private boolean trackEntities;
    private boolean trackChunks;
    private BukkitTask monitorTask;
    private final Queue<PerformanceSnapshot> history = new ConcurrentLinkedQueue<PerformanceSnapshot>();
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private long lastCheck = 0L;
    private int ticks = 0;
    private final DecimalFormat tpsFormat = new DecimalFormat("#0.00");
    private double tps = 20.0;

    public PerformanceMonitor(OptimizerPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.loadConfiguration();
    }

    private void loadConfiguration() {
        try {
            this.monitorInterval = this.getMonitorInterval();
            this.historySize = this.plugin.getConfig().getInt("performance-monitor.history-size", 60);
            this.trackTPS = this.plugin.getConfig().getBoolean("performance-monitor.track-tps", true);
            this.trackMemory = this.plugin.getConfig().getBoolean("performance-monitor.track-memory", true);
            this.trackEntities = this.plugin.getConfig().getBoolean("performance-monitor.track-entities", true);
            this.trackChunks = this.plugin.getConfig().getBoolean("performance-monitor.track-chunks", true);
        }
        catch (Exception e) {
            this.logger.warning("Error loading performance monitor configuration: " + e.getMessage());
            this.monitorInterval = 30;
            this.historySize = 60;
            this.trackTPS = true;
            this.trackMemory = true;
            this.trackEntities = true;
            this.trackChunks = true;
        }
    }

    private int getMonitorInterval() {
        return this.plugin.getConfig().getInt("performance-monitor.interval-seconds", 30);
    }

    public void startMonitoring() {
        if (this.trackTPS) {
            Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> ++this.ticks, 1L, 1L);
        }
        this.monitorTask = Bukkit.getScheduler().runTaskTimerAsynchronously((Plugin)this.plugin, this::captureSnapshot, 20L, (long)this.monitorInterval * 20L);
        this.logger.info("Performance monitoring started. Interval: " + this.monitorInterval + " seconds");
    }

    private void captureSnapshot() {
        try {
            PerformanceSnapshot snapshot = new PerformanceSnapshot();
            snapshot.timestamp = System.currentTimeMillis();
            if (this.trackTPS) {
                this.captureTPS(snapshot);
            }
            if (this.trackMemory) {
                this.captureMemory(snapshot);
            }
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                if (this.trackEntities) {
                    this.captureEntityCounts(snapshot);
                }
                if (this.trackChunks) {
                    this.captureChunkStats(snapshot);
                }
                this.history.add(snapshot);
                while (this.history.size() > this.historySize) {
                    this.history.poll();
                }
                if (this.isPerformanceReportsEnabled()) {
                    this.logSnapshot(snapshot);
                }
            });
        }
        catch (Exception e) {
            this.logger.warning("Error capturing performance snapshot: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void captureTPS(PerformanceSnapshot snapshot) {
        long now = System.currentTimeMillis();
        long elapsed = now - this.lastCheck;
        if (elapsed >= 1000L) {
            this.tps = (double)this.ticks * 1000.0 / (double)elapsed;
            if (this.tps > 20.0) {
                this.tps = 20.0;
            }
            this.lastCheck = now;
            this.ticks = 0;
        }
        snapshot.tps = this.tps;
    }

    private void captureMemory(PerformanceSnapshot snapshot) {
        MemoryUsage heapUsage = this.memoryBean.getHeapMemoryUsage();
        snapshot.usedMemory = heapUsage.getUsed() / 0x100000L;
        snapshot.allocatedMemory = heapUsage.getCommitted() / 0x100000L;
        snapshot.maxMemory = heapUsage.getMax() / 0x100000L;
        snapshot.nonHeapMemory = this.memoryBean.getNonHeapMemoryUsage().getUsed() / 0x100000L;
    }

    private void captureEntityCounts(PerformanceSnapshot snapshot) {
        snapshot.totalEntities = 0;
        snapshot.itemEntities = 0;
        snapshot.mobEntities = 0;
        snapshot.entitiesByWorld = new HashMap<String, Map<String, Integer>>();
        for (World world : Bukkit.getWorlds()) {
            List entities = world.getEntities();
            int worldTotal = entities.size();
            int worldItems = 0;
            int worldMobs = 0;
            for (Entity entity : entities) {
                if (entity instanceof Item) {
                    ++worldItems;
                    ++snapshot.itemEntities;
                    continue;
                }
                if (entity instanceof Player) continue;
                ++worldMobs;
                ++snapshot.mobEntities;
            }
            snapshot.totalEntities += worldTotal;
            HashMap<String, Integer> worldCounts = new HashMap<String, Integer>();
            worldCounts.put("total", worldTotal);
            worldCounts.put("items", worldItems);
            worldCounts.put("mobs", worldMobs);
            snapshot.entitiesByWorld.put(world.getName(), worldCounts);
        }
    }

    private void captureChunkStats(PerformanceSnapshot snapshot) {
        snapshot.loadedChunks = 0;
        snapshot.chunksByWorld = new HashMap<String, Integer>();
        for (World world : Bukkit.getWorlds()) {
            int worldChunks = 0;
            for (Chunk chunk : world.getLoadedChunks()) {
                ++worldChunks;
            }
            snapshot.loadedChunks += worldChunks;
            snapshot.chunksByWorld.put(world.getName(), worldChunks);
        }
    }

    private void logSnapshot(PerformanceSnapshot snapshot) {
        StringBuilder report = new StringBuilder();
        report.append("\n==== MCOptimizer Performance Report ====\n");
        if (this.trackTPS) {
            report.append("TPS: ").append(this.formatTPS(snapshot.tps)).append('\n');
        }
        if (this.trackMemory) {
            report.append(String.format("Memory: Used %d MB / Allocated %d MB / Max %d MB (%.1f%%)\n", snapshot.usedMemory, snapshot.allocatedMemory, snapshot.maxMemory, (double)snapshot.usedMemory * 100.0 / (double)snapshot.maxMemory));
        }
        if (this.trackEntities) {
            report.append(String.format("Entities: %d total (%d mobs, %d items)\n", snapshot.totalEntities, snapshot.mobEntities, snapshot.itemEntities));
            if (snapshot.entitiesByWorld.size() > 1) {
                report.append("  Per-world breakdown:\n");
                for (Map.Entry<String, Object> entry : snapshot.entitiesByWorld.entrySet()) {
                    Map counts = (Map)entry.getValue();
                    report.append(String.format("    %s: %d total (%d mobs, %d items)\n", entry.getKey(), counts.get("total"), counts.get("mobs"), counts.get("items")));
                }
            }
        }
        if (this.trackChunks) {
            report.append(String.format("Chunks: %d total\n", snapshot.loadedChunks));
            if (snapshot.chunksByWorld.size() > 1) {
                report.append("  Per-world breakdown:\n");
                for (Map.Entry<String, Integer> entry : snapshot.chunksByWorld.entrySet()) {
                    report.append(String.format("    %s: %d chunks\n", entry.getKey(), entry.getValue()));
                }
            }
        }
        if (this.plugin.getChunkManager() != null) {
            report.append("\nChunk Manager Stats:\n");
            Map<String, Object> chunkStats = this.plugin.getChunkManager().getStats();
            for (Map.Entry<String, Object> entry : chunkStats.entrySet()) {
                report.append(String.format("  %s: %s\n", entry.getKey(), entry.getValue()));
            }
        }
        if (this.plugin.getEntityOptimizer() != null) {
            report.append("\nEntity Optimizer Stats:\n");
            Map<String, Object> entityStats = this.plugin.getEntityOptimizer().getStats();
            for (Map.Entry<String, Object> entry : entityStats.entrySet()) {
                if (entry.getKey().equals("entityTypes")) continue;
                report.append(String.format("  %s: %s\n", entry.getKey(), entry.getValue()));
            }
        }
        report.append("=======================================");
        this.logger.info(report.toString());
    }

    private String formatTPS(double tps) {
        String formattedTPS = this.tpsFormat.format(tps);
        ChatColor color = tps >= 18.0 ? ChatColor.GREEN : (tps >= 15.0 ? ChatColor.YELLOW : ChatColor.RED);
        return String.format("%s%s", color, formattedTPS);
    }

    public void logServerStatus() {
        if (this.history.isEmpty()) {
            PerformanceSnapshot snapshot = new PerformanceSnapshot();
            snapshot.timestamp = System.currentTimeMillis();
            if (this.trackTPS) {
                snapshot.tps = this.tps;
            }
            if (this.trackMemory) {
                this.captureMemory(snapshot);
            }
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                if (this.trackEntities) {
                    this.captureEntityCounts(snapshot);
                }
                if (this.trackChunks) {
                    this.captureChunkStats(snapshot);
                }
                this.logSnapshot(snapshot);
            });
        } else {
            this.logSnapshot(this.history.peek());
        }
    }

    public PerformanceSnapshot getLatestSnapshot() {
        return this.history.peek();
    }

    public List<PerformanceSnapshot> getHistory() {
        return new ArrayList<PerformanceSnapshot>(this.history);
    }

    public void shutdown() {
        if (this.monitorTask != null) {
            this.monitorTask.cancel();
        }
        this.history.clear();
    }

    private boolean isPerformanceReportsEnabled() {
        try {
            return this.plugin.getConfig().getBoolean("performance-reports.enabled", false);
        }
        catch (Exception e) {
            return false;
        }
    }

    public static class PerformanceSnapshot {
        public long timestamp;
        public double tps;
        public long usedMemory;
        public long allocatedMemory;
        public long maxMemory;
        public long nonHeapMemory;
        public int totalEntities;
        public int mobEntities;
        public int itemEntities;
        public Map<String, Map<String, Integer>> entitiesByWorld;
        public int loadedChunks;
        public Map<String, Integer> chunksByWorld;
    }
}

