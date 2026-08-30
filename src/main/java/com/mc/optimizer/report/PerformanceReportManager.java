/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.World
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.mc.optimizer.report;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.config.ConfigManager;
import com.sun.management.OperatingSystemMXBean;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class PerformanceReportManager {
    private final OptimizerPlugin plugin;
    private final ConfigManager config;
    private final Logger logger;
    private boolean enabled;
    private boolean autoGenerateReports;
    private int reportIntervalMinutes;
    private int historyDays;
    private boolean includePluginMetrics;
    private boolean exportToWeb;
    private int maxReports;
    private final Map<String, PerformanceReport> reports = new ConcurrentHashMap<String, PerformanceReport>();
    private final List<PerformanceSample> currentSamples = Collections.synchronizedList(new ArrayList());
    private BukkitTask samplingTask;
    private BukkitTask reportGenerationTask;
    private final File reportDir;

    public PerformanceReportManager(OptimizerPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.reportDir = new File(plugin.getDataFolder(), "reports");
        this.loadConfiguration();
        if (this.enabled) {
            this.initialize();
        }
    }

    private void loadConfiguration() {
        this.enabled = true;
        this.autoGenerateReports = true;
        this.reportIntervalMinutes = 60;
        this.historyDays = 7;
        this.includePluginMetrics = true;
        this.exportToWeb = false;
        this.maxReports = 100;
        try {
            if (this.config != null) {
                try {
                    Method method = this.config.getClass().getMethod("isPerformanceReportsEnabled", new Class[0]);
                    this.enabled = (Boolean)method.invoke((Object)this.config, new Object[0]);
                }
                catch (Exception e) {
                    this.logger.warning("Could not load performance report configuration: " + e.getMessage());
                }
            }
        }
        catch (Exception e) {
            this.logger.warning("Error loading performance report configuration: " + e.getMessage());
        }
    }

    private void initialize() {
        if (!this.reportDir.exists()) {
            this.reportDir.mkdirs();
        }
        this.loadExistingReports();
        this.cleanupOldReports();
        this.startSamplingTask();
        if (this.autoGenerateReports) {
            this.startReportGenerationTask();
        }
        this.logger.info("Performance report manager initialized");
    }

    private void loadExistingReports() {
        File[] files = this.reportDir.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) {
            return;
        }
        for (File file : files) {
            try {
                PerformanceReport report = PerformanceReport.fromFile(file);
                this.reports.put(report.getId(), report);
            }
            catch (Exception e) {
                this.logger.log(Level.WARNING, "Failed to load report: " + file.getName(), e);
            }
        }
        this.logger.info("Loaded " + this.reports.size() + " existing performance reports");
    }

    private void cleanupOldReports() {
        if (this.historyDays <= 0) {
            return;
        }
        Calendar cutoff = Calendar.getInstance();
        cutoff.add(6, -this.historyDays);
        Date cutoffDate = cutoff.getTime();
        Iterator<Map.Entry<String, PerformanceReport>> it = this.reports.entrySet().iterator();
        while (it.hasNext()) {
            PerformanceReport report = it.next().getValue();
            if (!report.getTimestamp().before(cutoffDate)) continue;
            File file = new File(this.reportDir, report.getId() + ".json");
            if (file.exists()) {
                file.delete();
            }
            it.remove();
        }
    }

    private void enforceMaxReports() {
        if (this.maxReports <= 0 || this.reports.size() <= this.maxReports) {
            return;
        }
        ArrayList<PerformanceReport> sortedReports = new ArrayList<PerformanceReport>(this.reports.values());
        sortedReports.sort(Comparator.comparing(PerformanceReport::getTimestamp));
        int toRemove = sortedReports.size() - this.maxReports;
        for (int i = 0; i < toRemove; ++i) {
            PerformanceReport report = (PerformanceReport)sortedReports.get(i);
            File file = new File(this.reportDir, report.getId() + ".json");
            if (file.exists()) {
                file.delete();
            }
            this.reports.remove(report.getId());
        }
    }

    private void startSamplingTask() {
        if (this.samplingTask != null) {
            this.samplingTask.cancel();
        }
        this.samplingTask = Bukkit.getScheduler().runTaskTimerAsynchronously((Plugin)this.plugin, this::collectSample, 20L, 600L);
    }

    private void startReportGenerationTask() {
        if (this.reportGenerationTask != null) {
            this.reportGenerationTask.cancel();
        }
        this.reportGenerationTask = Bukkit.getScheduler().runTaskTimerAsynchronously((Plugin)this.plugin, this::generateScheduledReport, 1200L * (long)this.reportIntervalMinutes, 1200L * (long)this.reportIntervalMinutes);
    }

    private void collectSample() {
        if (!this.enabled) {
            return;
        }
        try {
            PerformanceSample sample = new PerformanceSample();
            sample.setTimestamp(new Date());
            double[] recentTps = Bukkit.getServer().getTPS();
            if (recentTps.length > 0) {
                sample.setTps(recentTps[0]);
            }
            MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
            long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
            sample.setUsedMemory(usedMemory);
            sample.setMaxMemory(maxMemory);
            try {
                java.lang.management.OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
                if (osBean instanceof OperatingSystemMXBean) {
                    OperatingSystemMXBean sunOsBean = (OperatingSystemMXBean)osBean;
                    double cpuLoad = sunOsBean.getProcessCpuLoad() * 100.0;
                    sample.setCpuUsage(cpuLoad);
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
            sample.setPlayerCount(Bukkit.getOnlinePlayers().size());
            Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> {
                try {
                    HashMap<String, Integer> entityCounts = new HashMap<String, Integer>();
                    int totalEntities = 0;
                    int totalChunks = 0;
                    for (World world : Bukkit.getWorlds()) {
                        totalEntities += world.getEntities().size();
                        totalChunks += world.getLoadedChunks().length;
                        world.getEntities().forEach(entity -> {
                            String type = entity.getType().name();
                            entityCounts.put(type, entityCounts.getOrDefault(type, 0) + 1);
                        });
                    }
                    sample.setEntityCounts(entityCounts);
                    sample.setTotalEntities(totalEntities);
                    sample.setLoadedChunks(totalChunks);
                    this.currentSamples.add(sample);
                    while (this.currentSamples.size() > 120) {
                        this.currentSamples.remove(0);
                    }
                }
                catch (Exception e) {
                    this.logger.log(Level.WARNING, "Failed to collect entity and chunk data for performance sample", e);
                }
            });
        }
        catch (Exception e) {
            this.logger.log(Level.WARNING, "Failed to collect performance sample", e);
        }
    }

    private void generateScheduledReport() {
        if (!this.enabled || !this.autoGenerateReports) {
            return;
        }
        try {
            PerformanceReport report = this.generateReport("Auto-generated report");
            this.saveReport(report);
            this.logger.info("Generated scheduled performance report: " + report.getId());
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.isOp() && !player.hasPermission("mcoptimizer.admin")) continue;
                player.sendMessage(String.valueOf(ChatColor.GREEN) + "[MCOptimizer] Generated performance report: " + String.valueOf(ChatColor.YELLOW) + report.getId());
                player.sendMessage(String.valueOf(ChatColor.GREEN) + "Use " + String.valueOf(ChatColor.YELLOW) + "/mcoptimizer report view " + report.getId() + String.valueOf(ChatColor.GREEN) + " to view it.");
            }
        }
        catch (Exception e) {
            this.logger.log(Level.WARNING, "Failed to generate scheduled report", e);
        }
    }

    public PerformanceReport generateReport(String description) {
        PerformanceReport report = new PerformanceReport();
        report.setId(this.generateReportId());
        report.setTimestamp(new Date());
        report.setDescription(description);
        report.setServerVersion(Bukkit.getVersion());
        report.setBukkitVersion(Bukkit.getBukkitVersion());
        report.setMaxPlayers(Bukkit.getMaxPlayers());
        report.setOnlinePlayers(Bukkit.getOnlinePlayers().size());
        report.setWorlds(Bukkit.getWorlds().size());
        report.setSamples(new ArrayList<PerformanceSample>(this.currentSamples));
        if (this.includePluginMetrics) {
            HashMap<String, Object> pluginInfo = new HashMap<String, Object>();
            for (Plugin serverPlugin : Bukkit.getPluginManager().getPlugins()) {
                HashMap<String, Object> info = new HashMap<String, Object>();
                info.put("version", serverPlugin.getDescription().getVersion());
                info.put("enabled", serverPlugin.isEnabled());
                info.put("authors", serverPlugin.getDescription().getAuthors());
                info.put("dependencies", serverPlugin.getDescription().getDepend());
                pluginInfo.put(serverPlugin.getName(), info);
            }
            report.setPluginInfo(pluginInfo);
        }
        HashMap<String, Object> optimizerInfo = new HashMap<String, Object>();
        if (this.plugin.getChunkManager() != null) {
            optimizerInfo.put("chunkManager", this.plugin.getChunkManager().getStats());
        }
        if (this.plugin.getEntityOptimizer() != null) {
            optimizerInfo.put("entityOptimizer", this.plugin.getEntityOptimizer().getStats());
        }
        if (this.plugin.getRedstoneOptimizer() != null) {
            optimizerInfo.put("redstoneOptimizer", this.plugin.getRedstoneOptimizer().getStats());
        }
        if (this.plugin.getMobCullingManager() != null) {
            optimizerInfo.put("mobCullingManager", this.plugin.getMobCullingManager().getStats());
        }
        if (this.plugin.getMemoryLeakDetector() != null) {
            optimizerInfo.put("memoryLeakDetector", this.plugin.getMemoryLeakDetector().getStats());
        }
        if (this.plugin.getEntityDistanceLimiter() != null) {
            optimizerInfo.put("entityDistanceLimiter", this.plugin.getEntityDistanceLimiter().getStats());
        }
        if (this.plugin.getTntOptimizer() != null) {
            optimizerInfo.put("tntOptimizer", this.plugin.getTntOptimizer().getStats());
        }
        if (this.plugin.getTaskOptimizer() != null) {
            optimizerInfo.put("taskOptimizer", this.plugin.getTaskOptimizer().getStats());
        }
        if (this.plugin.getNetworkOptimizer() != null) {
            optimizerInfo.put("networkOptimizer", this.plugin.getNetworkOptimizer().getStats());
        }
        if (this.plugin.getAfkManager() != null) {
            optimizerInfo.put("afkManager", this.plugin.getAfkManager().getStats());
        }
        report.setOptimizerInfo(optimizerInfo);
        return report;
    }

    public boolean saveReport(PerformanceReport report) {
        try {
            File file = new File(this.reportDir, report.getId() + ".json");
            report.saveToFile(file);
            this.reports.put(report.getId(), report);
            this.enforceMaxReports();
            if (this.exportToWeb) {
                this.exportReportToWeb(report);
            }
            return true;
        }
        catch (Exception e) {
            this.logger.log(Level.WARNING, "Failed to save report", e);
            return false;
        }
    }

    private void exportReportToWeb(PerformanceReport report) {
        if (this.config.isDebugEnabled()) {
            this.logger.info("Web export functionality has been removed");
        }
    }

    private String generateReportId() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
        return "report-" + sdf.format(new Date());
    }

    public PerformanceReport getReport(String id) {
        return this.reports.get(id);
    }

    public Map<String, PerformanceReport> getReports() {
        return this.reports;
    }

    public boolean deleteReport(String id) {
        PerformanceReport report = this.reports.remove(id);
        if (report != null) {
            File file = new File(this.reportDir, id + ".json");
            return file.delete();
        }
        return false;
    }

    public void displayReport(CommandSender sender, PerformanceReport report) {
        if (report == null) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Report not found");
            return;
        }
        sender.sendMessage(String.valueOf(ChatColor.GREEN) + "=== Performance Report: " + report.getId() + " ===");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Date: " + String.valueOf(report.getTimestamp()));
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Description: " + report.getDescription());
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Server Version: " + report.getServerVersion());
        double avgTps = report.getSamples().stream().mapToDouble(PerformanceSample::getTps).average().orElse(0.0);
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Average TPS: " + String.valueOf(ChatColor.WHITE) + String.format("%.2f", avgTps));
        long maxMemory = report.getSamples().stream().mapToLong(PerformanceSample::getMaxMemory).max().orElse(0L);
        long avgMemory = (long)report.getSamples().stream().mapToLong(PerformanceSample::getUsedMemory).average().orElse(0.0);
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Memory Usage: " + String.valueOf(ChatColor.WHITE) + this.formatSize(avgMemory) + " / " + this.formatSize(maxMemory));
        int maxEntities = report.getSamples().stream().mapToInt(PerformanceSample::getTotalEntities).max().orElse(0);
        int maxChunks = report.getSamples().stream().mapToInt(PerformanceSample::getLoadedChunks).max().orElse(0);
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Max Entities: " + String.valueOf(ChatColor.WHITE) + maxEntities);
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Max Chunks: " + String.valueOf(ChatColor.WHITE) + maxChunks);
        if (report.getPluginInfo() != null) {
            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Plugins: " + String.valueOf(ChatColor.WHITE) + report.getPluginInfo().size());
        }
        if (report.getOptimizerInfo() != null && !report.getOptimizerInfo().isEmpty()) {
            sender.sendMessage(String.valueOf(ChatColor.GREEN) + "=== Optimization Modules ===");
            for (Map.Entry<String, Object> entry2 : report.getOptimizerInfo().entrySet()) {
                boolean enabled;
                String module = entry2.getKey();
                if (!(entry2.getValue() instanceof Map)) continue;
                Map stats = (Map)entry2.getValue();
                boolean bl = enabled = stats.containsKey("enabled") ? (Boolean)stats.get("enabled") : false;
                if (enabled) {
                    sender.sendMessage(String.valueOf(ChatColor.GREEN) + "\u2713 " + module);
                    continue;
                }
                sender.sendMessage(String.valueOf(ChatColor.RED) + "\u2717 " + module);
            }
        }
        HashMap<String, Integer> entityCounts = new HashMap<String, Integer>();
        for (PerformanceSample sample : report.getSamples()) {
            for (Map.Entry<String, Integer> entry3 : sample.getEntityCounts().entrySet()) {
                entityCounts.merge(entry3.getKey(), entry3.getValue(), Integer::max);
            }
        }
        if (!entityCounts.isEmpty()) {
            sender.sendMessage(String.valueOf(ChatColor.GREEN) + "=== Entity Distribution ===");
            entityCounts.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(5L).forEach(entry -> sender.sendMessage(String.valueOf(ChatColor.YELLOW) + (String)entry.getKey() + ": " + String.valueOf(ChatColor.WHITE) + String.valueOf(entry.getValue())));
        }
        sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Use " + String.valueOf(ChatColor.YELLOW) + "/mcoptimizer report export " + report.getId() + String.valueOf(ChatColor.GREEN) + " to export this report.");
    }

    private String formatSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        if (bytes < 0x100000L) {
            return String.format("%.1f KB", (double)bytes / 1024.0);
        }
        if (bytes < 0x40000000L) {
            return String.format("%.1f MB", (double)bytes / 1048576.0);
        }
        return String.format("%.1f GB", (double)bytes / 1.073741824E9);
    }

    public Map<String, Object> getStats() {
        HashMap<String, Object> stats = new HashMap<String, Object>();
        stats.put("enabled", this.enabled);
        stats.put("autoGenerateReports", this.autoGenerateReports);
        stats.put("reportCount", this.reports.size());
        stats.put("sampleCount", this.currentSamples.size());
        return stats;
    }

    public void reload() {
        if (this.samplingTask != null) {
            this.samplingTask.cancel();
            this.samplingTask = null;
        }
        if (this.reportGenerationTask != null) {
            this.reportGenerationTask.cancel();
            this.reportGenerationTask = null;
        }
        this.currentSamples.clear();
        this.reports.clear();
        this.loadConfiguration();
        if (this.enabled) {
            this.initialize();
        }
    }

    public void shutdown() {
        if (this.samplingTask != null) {
            this.samplingTask.cancel();
            this.samplingTask = null;
        }
        if (this.reportGenerationTask != null) {
            this.reportGenerationTask.cancel();
            this.reportGenerationTask = null;
        }
        if (!this.currentSamples.isEmpty()) {
            try {
                PerformanceReport report = this.generateReport("Final report before shutdown");
                this.saveReport(report);
                this.logger.info("Generated final performance report: " + report.getId());
            }
            catch (Exception e) {
                this.logger.log(Level.WARNING, "Failed to generate final report", e);
            }
        }
        this.currentSamples.clear();
        this.reports.clear();
        this.logger.info("Performance report manager shutdown");
    }

    public static class PerformanceReport {
        private String id;
        private Date timestamp;
        private String description;
        private String serverVersion;
        private String bukkitVersion;
        private int maxPlayers;
        private int onlinePlayers;
        private int worlds;
        private List<PerformanceSample> samples = new ArrayList<PerformanceSample>();
        private Map<String, Object> pluginInfo;
        private Map<String, Object> optimizerInfo;

        public String getId() {
            return this.id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Date getTimestamp() {
            return this.timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        public String getDescription() {
            return this.description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getServerVersion() {
            return this.serverVersion;
        }

        public void setServerVersion(String serverVersion) {
            this.serverVersion = serverVersion;
        }

        public String getBukkitVersion() {
            return this.bukkitVersion;
        }

        public void setBukkitVersion(String bukkitVersion) {
            this.bukkitVersion = bukkitVersion;
        }

        public int getMaxPlayers() {
            return this.maxPlayers;
        }

        public void setMaxPlayers(int maxPlayers) {
            this.maxPlayers = maxPlayers;
        }

        public int getOnlinePlayers() {
            return this.onlinePlayers;
        }

        public void setOnlinePlayers(int onlinePlayers) {
            this.onlinePlayers = onlinePlayers;
        }

        public int getWorlds() {
            return this.worlds;
        }

        public void setWorlds(int worlds) {
            this.worlds = worlds;
        }

        public List<PerformanceSample> getSamples() {
            return this.samples;
        }

        public void setSamples(List<PerformanceSample> samples) {
            this.samples = samples;
        }

        public Map<String, Object> getPluginInfo() {
            return this.pluginInfo;
        }

        public void setPluginInfo(Map<String, Object> pluginInfo) {
            this.pluginInfo = pluginInfo;
        }

        public Map<String, Object> getOptimizerInfo() {
            return this.optimizerInfo;
        }

        public void setOptimizerInfo(Map<String, Object> optimizerInfo) {
            this.optimizerInfo = optimizerInfo;
        }

        public void saveToFile(File file) throws IOException {
            try (FileWriter writer = new FileWriter(file);){
                writer.write("{\n");
                writer.write("  \"id\": \"" + this.id + "\",\n");
                writer.write("  \"timestamp\": \"" + String.valueOf(this.timestamp) + "\",\n");
                writer.write("  \"description\": \"" + this.description + "\",\n");
                writer.write("  \"serverVersion\": \"" + this.serverVersion + "\",\n");
                writer.write("  \"bukkitVersion\": \"" + this.bukkitVersion + "\",\n");
                writer.write("  \"maxPlayers\": " + this.maxPlayers + ",\n");
                writer.write("  \"onlinePlayers\": " + this.onlinePlayers + ",\n");
                writer.write("  \"worlds\": " + this.worlds + ",\n");
                writer.write("  \"sampleCount\": " + this.samples.size() + "\n");
                writer.write("}\n");
            }
        }

        public static PerformanceReport fromFile(File file) throws IOException {
            PerformanceReport report = new PerformanceReport();
            report.setId(file.getName().replace(".json", ""));
            report.setTimestamp(new Date(file.lastModified()));
            report.setDescription("Loaded from file");
            return report;
        }
    }

    public static class PerformanceSample {
        private Date timestamp;
        private double tps;
        private long usedMemory;
        private long maxMemory;
        private double cpuUsage;
        private Map<String, Integer> entityCounts = new HashMap<String, Integer>();
        private int totalEntities;
        private int loadedChunks;
        private int playerCount;

        public Date getTimestamp() {
            return this.timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        public double getTps() {
            return this.tps;
        }

        public void setTps(double tps) {
            this.tps = tps;
        }

        public long getUsedMemory() {
            return this.usedMemory;
        }

        public void setUsedMemory(long usedMemory) {
            this.usedMemory = usedMemory;
        }

        public long getMaxMemory() {
            return this.maxMemory;
        }

        public void setMaxMemory(long maxMemory) {
            this.maxMemory = maxMemory;
        }

        public double getCpuUsage() {
            return this.cpuUsage;
        }

        public void setCpuUsage(double cpuUsage) {
            this.cpuUsage = cpuUsage;
        }

        public Map<String, Integer> getEntityCounts() {
            return this.entityCounts;
        }

        public void setEntityCounts(Map<String, Integer> entityCounts) {
            this.entityCounts = entityCounts;
        }

        public int getTotalEntities() {
            return this.totalEntities;
        }

        public void setTotalEntities(int totalEntities) {
            this.totalEntities = totalEntities;
        }

        public int getLoadedChunks() {
            return this.loadedChunks;
        }

        public void setLoadedChunks(int loadedChunks) {
            this.loadedChunks = loadedChunks;
        }

        public int getPlayerCount() {
            return this.playerCount;
        }

        public void setPlayerCount(int playerCount) {
            this.playerCount = playerCount;
        }
    }
}

