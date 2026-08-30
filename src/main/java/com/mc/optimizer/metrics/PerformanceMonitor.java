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
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

/**
 * Lightweight server metrics collector.
 *
 * All Bukkit state is sampled from one synchronous task. Expensive entity/chunk
 * scans are disabled by default and only run when explicitly enabled.
 */
public final class PerformanceMonitor {
    private final OptimizerPlugin plugin;
    private final ConfigManager config;
    private final Logger logger;
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final DecimalFormat tpsFormat = new DecimalFormat("#0.00");
    private final List<PerformanceSnapshot> history = new ArrayList<>();

    private int intervalSeconds;
    private int historySize;
    private boolean trackTPS;
    private boolean trackMemory;
    private boolean trackEntities;
    private boolean trackChunks;
    private BukkitTask monitorTask;
    private long lastSampleNanos;
    private double tps = 20.0;

    public PerformanceMonitor(OptimizerPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        loadConfiguration();
    }

    private void loadConfiguration() {
        intervalSeconds = Math.max(10, plugin.getConfig().getInt("performance-monitor.interval-seconds", 60));
        historySize = Math.max(1, plugin.getConfig().getInt("performance-monitor.history-size", 30));
        trackTPS = plugin.getConfig().getBoolean("performance-monitor.track-tps", true);
        trackMemory = plugin.getConfig().getBoolean("performance-monitor.track-memory", true);
        trackEntities = plugin.getConfig().getBoolean("performance-monitor.track-entities", false);
        trackChunks = plugin.getConfig().getBoolean("performance-monitor.track-chunks", false);
    }

    public void startMonitoring() {
        if (monitorTask != null) {
            monitorTask.cancel();
        }
        lastSampleNanos = System.nanoTime();
        long periodTicks = Math.max(200L, intervalSeconds * 20L);
        monitorTask = Bukkit.getScheduler().runTaskTimer(plugin, this::captureSnapshot, periodTicks, periodTicks);
        logger.info("Performance monitoring started. Interval: " + intervalSeconds + " seconds");
    }

    private void captureSnapshot() {
        long now = System.nanoTime();
        if (trackTPS && lastSampleNanos != 0L) {
            double elapsedSeconds = (now - lastSampleNanos) / 1_000_000_000.0;
            double expectedSeconds = intervalSeconds;
            if (elapsedSeconds > 0.0) {
                tps = Math.max(0.0, Math.min(20.0, 20.0 * expectedSeconds / elapsedSeconds));
            }
        }
        lastSampleNanos = now;

        PerformanceSnapshot snapshot = new PerformanceSnapshot();
        snapshot.timestamp = System.currentTimeMillis();
        snapshot.tps = tps;

        if (trackMemory) {
            captureMemory(snapshot);
        }
        if (trackEntities) {
            captureEntityCounts(snapshot);
        }
        if (trackChunks) {
            captureChunkStats(snapshot);
        }

        history.add(snapshot);
        if (history.size() > historySize) {
            history.subList(0, history.size() - historySize).clear();
        }

        if (isPerformanceReportsEnabled()) {
            logSnapshot(snapshot);
        }
    }

    private void captureMemory(PerformanceSnapshot snapshot) {
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        snapshot.usedMemory = heap.getUsed() / 0x100000L;
        snapshot.allocatedMemory = heap.getCommitted() / 0x100000L;
        snapshot.maxMemory = heap.getMax() > 0 ? heap.getMax() / 0x100000L : 0L;
        snapshot.nonHeapMemory = memoryBean.getNonHeapMemoryUsage().getUsed() / 0x100000L;
    }

    private void captureEntityCounts(PerformanceSnapshot snapshot) {
        snapshot.entitiesByWorld = new HashMap<>();
        for (World world : Bukkit.getWorlds()) {
            int total = 0;
            int items = 0;
            int mobs = 0;
            for (Entity entity : world.getEntities()) {
                total++;
                if (entity instanceof Item) {
                    items++;
                } else if (!(entity instanceof Player)) {
                    mobs++;
                }
            }
            snapshot.totalEntities += total;
            snapshot.itemEntities += items;
            snapshot.mobEntities += mobs;
            Map<String, Integer> counts = new HashMap<>();
            counts.put("total", total);
            counts.put("items", items);
            counts.put("mobs", mobs);
            snapshot.entitiesByWorld.put(world.getName(), counts);
        }
    }

    private void captureChunkStats(PerformanceSnapshot snapshot) {
        snapshot.chunksByWorld = new HashMap<>();
        for (World world : Bukkit.getWorlds()) {
            int count = world.getLoadedChunks().length;
            snapshot.loadedChunks += count;
            snapshot.chunksByWorld.put(world.getName(), count);
        }
    }

    private void logSnapshot(PerformanceSnapshot snapshot) {
        StringBuilder report = new StringBuilder("\n==== MCOptimizer Performance Report ====\n");
        if (trackTPS) {
            report.append("TPS: ").append(formatTPS(snapshot.tps)).append('\n');
        }
        if (trackMemory) {
            double percent = snapshot.maxMemory > 0
                    ? snapshot.usedMemory * 100.0 / snapshot.maxMemory
                    : 0.0;
            report.append(String.format(
                    "Memory: Used %d MB / Allocated %d MB / Max %d MB (%.1f%%)%n",
                    snapshot.usedMemory, snapshot.allocatedMemory, snapshot.maxMemory, percent));
        }
        if (trackEntities) {
            report.append(String.format(
                    "Entities: %d total (%d mobs, %d items)%n",
                    snapshot.totalEntities, snapshot.mobEntities, snapshot.itemEntities));
        }
        if (trackChunks) {
            report.append("Chunks: ").append(snapshot.loadedChunks).append(" total\n");
        }
        report.append("=======================================");
        logger.info(report.toString());
    }

    private String formatTPS(double value) {
        ChatColor color = value >= 18.0 ? ChatColor.GREEN : value >= 15.0 ? ChatColor.YELLOW : ChatColor.RED;
        return color + tpsFormat.format(value);
    }

    public void logServerStatus() {
        if (history.isEmpty()) {
            captureSnapshot();
        } else {
            logSnapshot(history.get(history.size() - 1));
        }
    }

    public PerformanceSnapshot getLatestSnapshot() {
        return history.isEmpty() ? null : history.get(history.size() - 1);
    }

    public List<PerformanceSnapshot> getHistory() {
        return new ArrayList<>(history);
    }

    public void shutdown() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
        history.clear();
    }

    private boolean isPerformanceReportsEnabled() {
        return plugin.getConfig().getBoolean("performance-reports.enabled", false);
    }

    public static final class PerformanceSnapshot {
        public long timestamp;
        public double tps;
        public long usedMemory;
        public long allocatedMemory;
        public long maxMemory;
        public long nonHeapMemory;
        public int totalEntities;
        public int mobEntities;
        public int itemEntities;
        public Map<String, Map<String, Integer>> entitiesByWorld = new HashMap<>();
        public int loadedChunks;
        public Map<String, Integer> chunksByWorld = new HashMap<>();
    }
}
