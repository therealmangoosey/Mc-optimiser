package com.mc.optimizer.memory;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.config.ConfigManager;
import com.sun.management.HotSpotDiagnosticMXBean;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class MemoryLeakDetector {
    private final OptimizerPlugin plugin;
    private final ConfigManager config;
    private final Logger logger;
    private final MemoryMXBean memoryBean;
    private BukkitTask monitorTask;
    private final List<MemorySnapshot> memoryHistory = new ArrayList<>();
    private int detectionCounter;
    private boolean leakDetected;
    private final Map<String, Long> pluginMemoryUsage = new HashMap<>();
    private int checkIntervalMinutes;
    private int detectionThreshold;
    private int minLeakThresholdMb;
    private boolean createHeapDump;
    private boolean showLeakSources;

    public MemoryLeakDetector(OptimizerPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.loadConfiguration();
        this.startMonitoring();
    }

    private void loadConfiguration() {
        this.checkIntervalMinutes = Math.max(1, this.plugin.getConfig().getInt("memory-leak.check-interval", 15));
        this.detectionThreshold = Math.max(1, this.plugin.getConfig().getInt("memory-leak.detection-threshold", 3));
        this.minLeakThresholdMb = Math.max(1, this.plugin.getConfig().getInt("memory-leak.min-leak-threshold-mb", 50));
        this.createHeapDump = this.plugin.getConfig().getBoolean("memory-leak.create-heap-dump", false);
        this.showLeakSources = this.plugin.getConfig().getBoolean("memory-leak.show-leak-sources", true);
    }

    public void startMonitoring() {
        this.stopMonitoring();
        this.monitorTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
                this.plugin,
                this::checkMemory,
                1200L,
                1200L * this.checkIntervalMinutes
        );
        this.logger.info("Memory leak detection started. Checking every " + this.checkIntervalMinutes + " minutes.");
    }

    public void stopMonitoring() {
        if (this.monitorTask != null) {
            this.monitorTask.cancel();
            this.monitorTask = null;
        }
    }

    private void checkMemory() {
        MemoryUsage heapUsage = this.memoryBean.getHeapMemoryUsage();
        long usedMemory = heapUsage.getUsed();
        long maxMemory = heapUsage.getMax();

        MemorySnapshot snapshot = new MemorySnapshot(System.currentTimeMillis(), usedMemory, maxMemory);
        this.memoryHistory.add(snapshot);
        while (this.memoryHistory.size() > this.detectionThreshold + 1) {
            this.memoryHistory.remove(0);
        }

        if (this.memoryHistory.size() >= 2) {
            boolean isIncreasing = true;
            long totalIncrease = 0L;
            for (int i = 1; i < this.memoryHistory.size(); i++) {
                MemorySnapshot current = this.memoryHistory.get(i);
                MemorySnapshot previous = this.memoryHistory.get(i - 1);
                long increase = current.getUsedMemory() - previous.getUsedMemory();
                if (increase <= 0L) {
                    isIncreasing = false;
                    break;
                }
                totalIncrease += increase;
            }

            long totalIncreaseMb = totalIncrease / (1024L * 1024L);
            if (isIncreasing && totalIncreaseMb >= this.minLeakThresholdMb) {
                this.detectionCounter++;
                if (this.detectionCounter >= this.detectionThreshold && !this.leakDetected) {
                    this.leakDetected = true;
                    this.handleLeakDetection(totalIncreaseMb);
                }
            } else {
                this.detectionCounter = 0;
                if (this.leakDetected) {
                    this.leakDetected = false;
                    this.logger.info("Memory usage has stabilized. The previous memory leak may have been resolved.");
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        for (CommandSender admin : this.getOnlineAdmins()) {
                            admin.sendMessage(ChatColor.GREEN + "[MCOptimizer] Memory usage has stabilized. The previous memory leak may have been resolved.");
                        }
                    });
                }
            }
        }

        if (this.showLeakSources && this.leakDetected) {
            this.analyzePluginMemoryUsage();
        }

        // MemoryUsage#getMax() can legitimately be -1 when the JVM cannot
        // provide a maximum heap size. Never treat that as 80% utilization.
        if (maxMemory > 0L && (double) usedMemory > 0.8 * maxMemory) {
            this.logger.warning("Memory usage is high (" + this.formatSize(usedMemory) + "/" + this.formatSize(maxMemory) + "). Suggesting garbage collection.");
            Bukkit.getScheduler().runTask(this.plugin, () -> {
                for (CommandSender admin : this.getOnlineAdmins()) {
                    admin.sendMessage(ChatColor.YELLOW + "[MCOptimizer] Memory usage is high. Suggesting garbage collection.");
                }
            });
            System.gc();
        }
    }

    private void handleLeakDetection(long totalIncreaseMb) {
        this.logger.warning("Potential memory leak detected! Memory has increased by " + totalIncreaseMb + "MB over " + this.detectionThreshold + " consecutive checks.");
        this.takePreventiveActions(totalIncreaseMb);
        if (this.createHeapDump) {
            this.createHeapDump();
        }
        Bukkit.getScheduler().runTask(this.plugin, () -> {
            for (CommandSender admin : this.getOnlineAdmins()) {
                admin.sendMessage(ChatColor.RED + "[MCOptimizer] Potential memory leak detected! Memory has increased by " + totalIncreaseMb + "MB.");
                admin.sendMessage(ChatColor.RED + "This could lead to server instability if not addressed.");
                admin.sendMessage(ChatColor.YELLOW + "Automatic preventive actions have been taken to mitigate the issue.");
                if (!this.showLeakSources || this.pluginMemoryUsage.isEmpty()) {
                    continue;
                }
                admin.sendMessage(ChatColor.YELLOW + "Plugins with significant memory usage:");
                this.pluginMemoryUsage.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue().reversed())
                        .limit(3)
                        .forEach(entry -> admin.sendMessage(
                                ChatColor.GRAY + " - " + entry.getKey() + ": " + this.formatSize(entry.getValue())
                        ));
            }
        });
    }

    private void takePreventiveActions(long totalIncreaseMb) {
        this.logger.info("Taking preventive actions to mitigate memory leak...");
        this.logger.info("Suggesting garbage collection to free unreferenced memory...");
        System.gc();
        this.logger.info("Preventive actions completed. Monitoring memory usage...");
    }

    private void createHeapDump() {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
            String timestamp = sdf.format(new Date());
            File dumpDir = new File(this.plugin.getDataFolder(), "dumps");
            if (!dumpDir.exists() && !dumpDir.mkdirs() && !dumpDir.isDirectory()) {
                throw new IllegalStateException("Could not create heap dump directory: " + dumpDir.getAbsolutePath());
            }
            File heapDumpFile = new File(dumpDir, "heapdump_" + timestamp + ".hprof");
            this.logger.info("Creating heap dump: " + heapDumpFile.getAbsolutePath());
            HotSpotDiagnosticMXBean hotspotMBean = ManagementFactory.getPlatformMXBean(HotSpotDiagnosticMXBean.class);
            if (hotspotMBean == null) {
                throw new IllegalStateException("HotSpotDiagnosticMXBean is not available");
            }
            hotspotMBean.dumpHeap(heapDumpFile.getAbsolutePath(), false);
            this.logger.info("Heap dump created successfully. Use a memory analyzer tool to examine it.");
        } catch (Exception e) {
            this.logger.log(Level.SEVERE, "Failed to create heap dump", e);
        }
    }

    private void analyzePluginMemoryUsage() {
        this.pluginMemoryUsage.clear();
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugin.getName().equals(this.plugin.getName())) {
                continue;
            }
            try {
                long estimatedMemory = this.estimatePluginMemoryUsage(plugin);
                this.pluginMemoryUsage.put(plugin.getName(), estimatedMemory);
            } catch (Exception e) {
                this.logger.fine("Could not estimate memory usage for plugin: " + plugin.getName());
            }
        }
    }

    private long estimatePluginMemoryUsage(Plugin plugin) {
        try {
            File pluginFile = new File(plugin.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
            long baseEstimate = pluginFile.length();
            return (long) (baseEstimate * 2.5);
        } catch (Exception e) {
            return 1_000_000L;
        }
    }

    private List<CommandSender> getOnlineAdmins() {
        ArrayList<CommandSender> admins = new ArrayList<>();
        admins.add(Bukkit.getConsoleSender());
        Bukkit.getOnlinePlayers().stream()
                .filter(player -> player.isOp() || player.hasPermission("mcoptimizer.admin"))
                .forEach(admins::add);
        return admins;
    }

    private String formatSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024L) {
            return String.format("%.1f KB", (double) bytes / 1024.0);
        }
        if (bytes < 1024L * 1024L * 1024L) {
            return String.format("%.1f MB", (double) bytes / (1024.0 * 1024.0));
        }
        return String.format("%.1f GB", (double) bytes / (1024.0 * 1024.0 * 1024.0));
    }

    public Map<String, Object> getMemoryStats() {
        HashMap<String, Object> stats = new HashMap<>();
        MemoryUsage heapUsage = this.memoryBean.getHeapMemoryUsage();
        long usedMemory = heapUsage.getUsed();
        long maxMemory = heapUsage.getMax();
        double usedPercentage = maxMemory > 0L ? (double) usedMemory / maxMemory * 100.0 : -1.0;

        stats.put("usedMemory", usedMemory);
        stats.put("maxMemory", maxMemory);
        stats.put("usedPercentage", usedPercentage);
        stats.put("freeMemory", maxMemory > 0L ? maxMemory - usedMemory : -1L);
        stats.put("leakDetected", this.leakDetected);
        stats.put("detectionCounter", this.detectionCounter);
        return stats;
    }

    public Map<String, Object> getStats() {
        HashMap<String, Object> stats = new HashMap<>();
        MemoryUsage heapUsage = this.memoryBean.getHeapMemoryUsage();
        long usedMemory = heapUsage.getUsed();
        long maxMemory = heapUsage.getMax();

        stats.put("used", this.formatSize(usedMemory));
        stats.put("max", maxMemory > 0L ? this.formatSize(maxMemory) : "unknown");
        stats.put("percentage", maxMemory > 0L
                ? String.format("%.1f%%", (double) usedMemory / maxMemory * 100.0)
                : "unknown");
        stats.put("leakDetected", this.leakDetected ? "Yes" : "No");
        stats.put("leakCounter", this.detectionCounter);

        if (!this.pluginMemoryUsage.isEmpty()) {
            HashMap<String, String> topPlugins = new HashMap<>();
            this.pluginMemoryUsage.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> topPlugins.put(entry.getKey(), this.formatSize(entry.getValue())));
            stats.put("topPlugins", topPlugins);
        }
        return stats;
    }

    public List<MemorySnapshot> getMemoryHistory() {
        return new ArrayList<>(this.memoryHistory);
    }

    public boolean isLeakDetected() {
        return this.leakDetected;
    }

    public void reload() {
        this.loadConfiguration();
        this.detectionCounter = 0;
        this.leakDetected = false;
        this.memoryHistory.clear();
        this.startMonitoring();
    }

    public void shutdown() {
        this.stopMonitoring();
        this.memoryHistory.clear();
        this.pluginMemoryUsage.clear();
    }

    public static class MemorySnapshot {
        private final long timestamp;
        private final long usedMemory;
        private final long maxMemory;

        public MemorySnapshot(long timestamp, long usedMemory, long maxMemory) {
            this.timestamp = timestamp;
            this.usedMemory = usedMemory;
            this.maxMemory = maxMemory;
        }

        public long getTimestamp() {
            return this.timestamp;
        }

        public long getUsedMemory() {
            return this.usedMemory;
        }

        public long getMaxMemory() {
            return this.maxMemory;
        }
    }
}
