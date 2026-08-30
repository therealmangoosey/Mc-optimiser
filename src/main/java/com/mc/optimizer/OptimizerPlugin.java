/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.PluginCommand
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.plugin.java.JavaPlugin
 */
package com.mc.optimizer;

import com.mc.optimizer.afk.AfkManager;
import com.mc.optimizer.api.PluginIntegrationAPI;
import com.mc.optimizer.chunk.ChunkManager;
import com.mc.optimizer.chunk.config.ChunkConfigManager;
import com.mc.optimizer.commands.OptimizerCommand;
import com.mc.optimizer.config.ConfigManager;
import com.mc.optimizer.config.analyzer.ConfigurationAnalyzer;
import com.mc.optimizer.entity.EntityOptimizer;
import com.mc.optimizer.entity.culling.MobCullingManager;
import com.mc.optimizer.entity.distance.EntityDistanceLimiter;
import com.mc.optimizer.integration.CrossPlatformManager;
import com.mc.optimizer.lagprediction.LagPredictionManager;
import com.mc.optimizer.memory.MemoryLeakDetector;
import com.mc.optimizer.metrics.PerformanceMonitor;
import com.mc.optimizer.network.NetworkOptimizer;
import com.mc.optimizer.redstone.RedstoneOptimizer;
import com.mc.optimizer.report.PerformanceReportManager;
import com.mc.optimizer.stress.StressTestManager;
import com.mc.optimizer.task.TaskOptimizer;
import com.mc.optimizer.tnt.TNTOptimizer;
import com.mc.optimizer.update.UpdateChecker;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

public class OptimizerPlugin
extends JavaPlugin {
    private static OptimizerPlugin instance;
    private ConfigManager configManager;
    private ChunkManager chunkManager;
    private EntityOptimizer entityOptimizer;
    private PerformanceMonitor performanceMonitor;
    private RedstoneOptimizer redstoneOptimizer;
    private MobCullingManager mobCullingManager;
    private ConfigurationAnalyzer configAnalyzer;
    private MemoryLeakDetector memoryLeakDetector;
    private EntityDistanceLimiter entityDistanceLimiter;
    private TNTOptimizer tntOptimizer;
    private TaskOptimizer taskOptimizer;
    private NetworkOptimizer networkOptimizer;
    private AfkManager afkManager;
    private StressTestManager stressTestManager;
    private PerformanceReportManager reportManager;
    private PluginIntegrationAPI integrationAPI;
    private CrossPlatformManager crossPlatformManager;
    private LagPredictionManager lagPredictionManager;
    private final List<String> detectedConflicts = new ArrayList<String>();

    public void onEnable() {
        instance = this;
        Logger logger = this.getLogger();
        logger.info("");
        logger.info("\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557");
        logger.info("\u2551            MCOptimizer Plugin              \u2551");
        logger.info("\u2551    Optimize your server with minimal       \u2551");
        logger.info("\u2551            CPU/RAM overhead!               \u2551");
        logger.info("\u2560\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2563");
        logger.info("\u2551 Created by: juicyyfruittsnackss            \u2551");
        logger.info("\u2551 Support: https://dcs.gg/fruitsnacks/       \u2551");
        logger.info("\u2551 Projects: https://fruitsnacks.xyz          \u2551");
        logger.info("\u2551 Version: " + this.getDescription().getVersion() + "                            \u2551");
        logger.info("\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d");
        logger.info("");
        logger.info("For support go to: https://dcs.gg/fruitsnacks/");
        logger.info("Check https://fruitsnacks.xyz for upcoming projects");
        logger.info("");
        File dataFolder = this.getDataFolder();
        if (!dataFolder.exists()) {
            logger.info("Creating plugin data folder...");
            if (dataFolder.mkdirs()) {
                logger.info("Data folder created successfully at: " + dataFolder.getAbsolutePath());
            } else {
                logger.warning("Failed to create data folder at: " + dataFolder.getAbsolutePath());
            }
        } else {
            logger.info("Using existing data folder at: " + dataFolder.getAbsolutePath());
        }
        logger.info("Loading configuration...");
        this.configManager = new ConfigManager(this);
        this.configManager.loadConfig();
        try {
            if (this.getConfig().getBoolean("general.auto-detect-conflicts", true)) {
                this.detectConflicts();
            }
        }
        catch (Exception e) {
            logger.warning("Error checking if auto-detect conflicts is enabled: " + e.getMessage());
        }
        this.initializeComponents();
        final OptimizerCommand optimizerCommand = new OptimizerCommand(this);
        PluginCommand mcOptimizerCmd = Bukkit.getPluginCommand((String)"mcoptimizer");
        if (mcOptimizerCmd == null) {
            this.getLogger().info("Registering mcoptimizer command manually");
            Command mainCommand = new Command("mcoptimizer"){

                public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                    return optimizerCommand.onCommand(sender, this, commandLabel, args);
                }

                public List<String> tabComplete(CommandSender sender, String alias, String[] args) {
                    return optimizerCommand.onTabComplete(sender, this, alias, args);
                }
            };
            mainCommand.setDescription("Main command for the MCOptimizer plugin");
            mainCommand.setUsage("/<command> [help|status|reload|config|version]");
            mainCommand.setPermission("mcoptimizer.use");
            mainCommand.setAliases(Arrays.asList("mcopt", "mco"));
            Bukkit.getCommandMap().register("mcoptimizer", mainCommand);
        } else {
            this.getLogger().info("Using existing mcoptimizer command registration");
            mcOptimizerCmd.setExecutor((CommandExecutor)optimizerCommand);
            mcOptimizerCmd.setTabCompleter((TabCompleter)optimizerCommand);
        }
        try {
            if (this.getConfig().getBoolean("general.metrics-enabled", true)) {
                this.setupMetrics();
            }
        }
        catch (Exception e) {
            logger.warning("Error checking if metrics are enabled: " + e.getMessage());
        }
        UpdateChecker.check(this);
        String version = this.getDescription().getVersion();
        logger.info("MCOptimizer v" + version + " enabled successfully");
        if (!this.detectedConflicts.isEmpty()) {
            logger.warning("Potential conflicts detected with: " + String.join((CharSequence)", ", this.detectedConflicts));
            logger.warning("Some features may be disabled to avoid conflicts.");
        }
    }

    public void onDisable() {
        if (this.crossPlatformManager != null) {
            // empty if block
        }
        if (this.lagPredictionManager != null) {
            this.lagPredictionManager.shutdown();
        }
        if (this.integrationAPI != null) {
            this.integrationAPI.shutdown();
        }
        if (this.reportManager != null) {
            this.reportManager.shutdown();
        }
        if (this.stressTestManager != null) {
            this.stressTestManager.shutdown();
        }
        if (this.afkManager != null) {
            this.afkManager.shutdown();
        }
        if (this.networkOptimizer != null) {
            this.networkOptimizer.shutdown();
        }
        if (this.taskOptimizer != null) {
            this.taskOptimizer.shutdown();
        }
        if (this.tntOptimizer != null) {
            this.tntOptimizer.shutdown();
        }
        if (this.entityDistanceLimiter != null) {
            this.entityDistanceLimiter.shutdown();
        }
        if (this.memoryLeakDetector != null) {
            this.memoryLeakDetector.shutdown();
        }
        if (this.performanceMonitor != null) {
            this.performanceMonitor.shutdown();
        }
        if (this.mobCullingManager != null) {
            this.mobCullingManager.shutdown();
        }
        if (this.redstoneOptimizer != null) {
            this.redstoneOptimizer.shutdown();
        }
        if (this.entityOptimizer != null) {
            this.entityOptimizer.shutdown();
        }
        if (this.chunkManager != null) {
            this.chunkManager.shutdown();
        }
        this.getLogger().info("MCOptimizer disabled");
        this.getLogger().info("For support go to: https://dcs.gg/fruitsnacks/");
        this.getLogger().info("Check https://fruitsnacks.xyz for upcoming projects");
    }

    private void initializeComponents() {
        Logger logger = this.getLogger();
        try {
            if (this.getConfig().getBoolean("chunk.enabled", true) && !this.isFeatureConflicting("chunk")) {
                logger.info("Initializing chunk management...");
                this.chunkManager = new ChunkManager(this, new ChunkConfigManager(this));
            } else {
                logger.info("Chunk management disabled");
            }
        }
        catch (Exception e) {
            logger.warning("Error initializing chunk management: " + e.getMessage());
        }
        try {
            if (this.getConfig().getBoolean("entity.enabled", true) && !this.isFeatureConflicting("entity")) {
                logger.info("Initializing entity optimization...");
                this.entityOptimizer = new EntityOptimizer(this, this.configManager);
            } else {
                logger.info("Entity optimization disabled");
            }
        }
        catch (Exception e) {
            logger.warning("Error initializing entity optimization: " + e.getMessage());
        }
        try {
            if (this.getConfig().getBoolean("redstone.enabled", true) && !this.isFeatureConflicting("redstone")) {
                logger.info("Initializing redstone optimization...");
                this.redstoneOptimizer = new RedstoneOptimizer(this, this.configManager);
            } else {
                logger.info("Redstone optimization disabled");
            }
        }
        catch (Exception e) {
            logger.warning("Error initializing redstone optimization: " + e.getMessage());
        }
        try {
            if (this.getConfig().getBoolean("mob-culling.enabled", true) && !this.isFeatureConflicting("mob-culling")) {
                logger.info("Initializing mob culling...");
                this.mobCullingManager = new MobCullingManager(this, this.configManager);
            } else {
                logger.info("Mob culling disabled");
            }
        }
        catch (Exception e) {
            logger.warning("Error initializing mob culling: " + e.getMessage());
        }
        try {
            if (this.getConfig().getBoolean("performance-monitor.enabled", true)) {
                logger.info("Initializing performance monitor...");
                this.performanceMonitor = new PerformanceMonitor(this, this.configManager);
            } else {
                logger.info("Performance monitoring disabled");
            }
        }
        catch (Exception e) {
            logger.warning("Error initializing performance monitor: " + e.getMessage());
        }
        logger.info("Web panel functionality has been removed");
        try {
            if (this.getConfig().getBoolean("config-analyzer.enabled", true)) {
                logger.info("Initializing configuration analyzer...");
                this.configAnalyzer = new ConfigurationAnalyzer(this);
            } else {
                logger.info("Configuration analyzer disabled");
            }
        }
        catch (Exception e) {
            logger.warning("Error initializing configuration analyzer: " + e.getMessage());
        }
        try {
            if (this.getConfig().getBoolean("memory-leak.enabled", true)) {
                logger.info("Initializing memory leak detector...");
                this.memoryLeakDetector = new MemoryLeakDetector(this, this.configManager);
            } else {
                logger.info("Memory leak detection disabled");
            }
        }
        catch (Exception e) {
            logger.warning("Error initializing memory leak detector: " + e.getMessage());
        }
        try {
            if (this.getConfig().getBoolean("entity-distance-limit.enabled", true) && !this.isFeatureConflicting("entity")) {
                logger.info("Initializing entity distance limiter...");
                this.entityDistanceLimiter = new EntityDistanceLimiter(this, this.configManager);
            } else {
                logger.info("Entity distance limiting disabled");
            }
        }
        catch (Exception e) {
            logger.warning("Error initializing entity distance limiter: " + e.getMessage());
        }
        try {
            if (this.getConfig().getBoolean("tnt-optimization.enabled", false)) {
                logger.info("Initializing TNT optimizer...");
                this.tntOptimizer = new TNTOptimizer(this, this.configManager);
            } else {
                logger.info("TNT optimization disabled");
            }
        }
        catch (Exception e) {
            logger.warning("Error initializing TNT optimizer: " + e.getMessage());
        }
        try {
            if (this.getConfig().getBoolean("task-optimization.enabled", false)) {
                logger.info("Initializing task optimizer...");
                this.taskOptimizer = new TaskOptimizer(this, this.configManager);
            } else {
                logger.info("Task optimization disabled");
            }
        }
        catch (Exception e) {
            logger.warning("Error initializing task optimizer: " + e.getMessage());
        }
        try {
            if (this.getConfig().getBoolean("network-optimization.enabled", false)) {
                logger.info("Initializing network optimizer...");
                this.networkOptimizer = new NetworkOptimizer(this, this.configManager);
            } else {
                logger.info("Network optimization disabled");
            }
        }
        catch (Exception e) {
            logger.warning("Error initializing network optimizer: " + e.getMessage());
        }
        try {
            if (this.getConfig().getBoolean("afk-detection.enabled", false)) {
                logger.info("Initializing AFK manager...");
                this.afkManager = new AfkManager(this, this.configManager);
            } else {
                logger.info("AFK detection disabled");
            }
        }
        catch (Exception e) {
            logger.warning("Error initializing AFK manager: " + e.getMessage());
        }
        try {
            if (this.getConfig().getBoolean("stress-test.enabled", false)) {
                logger.info("Initializing stress test manager...");
                this.stressTestManager = new StressTestManager(this, this.configManager);
            } else {
                logger.info("Stress testing disabled");
            }
        }
        catch (Exception e) {
            logger.warning("Error initializing stress test manager: " + e.getMessage());
        }
        try {
            if (this.getConfig().getBoolean("performance-reports.enabled", false)) {
                logger.info("Initializing performance report manager...");
                this.reportManager = new PerformanceReportManager(this, this.configManager);
            } else {
                logger.info("Performance reports disabled");
            }
        }
        catch (Exception e) {
            logger.warning("Error initializing performance report manager: " + e.getMessage());
        }
        try {
            if (this.getConfig().getBoolean("integration-api.enabled", false)) {
                logger.info("Initializing plugin integration API...");
                this.integrationAPI = new PluginIntegrationAPI(this, this.configManager);
            } else {
                logger.info("Plugin integration API disabled");
            }
        }
        catch (Exception e) {
            logger.warning("Error initializing plugin integration API: " + e.getMessage());
        }
        try {
            logger.info("Initializing cross-platform support (forced enabled)...");
            this.crossPlatformManager = new CrossPlatformManager(this);
        }
        catch (Exception e) {
            logger.warning("Error initializing cross-platform support: " + e.getMessage());
        }
        try {
            if (this.getConfig().getBoolean("lag-prediction.enabled", true)) {
                logger.info("Initializing lag prediction system...");
                this.lagPredictionManager = new LagPredictionManager(this);
                if (this.getConfig().getBoolean("lag-prediction.web-interface.enabled", false)) {
                    int port = this.getConfig().getInt("lag-prediction.web-interface.port", 8080);
                    String bindAddress = this.getConfig().getString("lag-prediction.web-interface.bind-address", "0.0.0.0");
                    logger.info("Lag prediction web interface available at: http://" + (bindAddress.equals("0.0.0.0") ? "localhost" : bindAddress) + ":" + port + "/");
                }
            } else {
                logger.info("Lag prediction system disabled");
            }
        }
        catch (Exception e) {
            logger.warning("Error initializing lag prediction system: " + e.getMessage());
        }
    }

    private void detectConflicts() {
        String[][] potentialConflicts;
        List excludedPlugins = new ArrayList();
        try {
            if (this.getConfig().contains("conflicts.excluded-plugins")) {
                excludedPlugins = this.getConfig().getStringList("conflicts.excluded-plugins");
            }
        }
        catch (Exception e) {
            this.getLogger().warning("Error getting excluded plugins from config: " + e.getMessage());
        }
        for (String[] conflict : potentialConflicts = new String[][]{{"ClearLag", "entity"}, {"EntityManager", "entity"}, {"EntityTracker", "entity"}, {"MobStacker", "entity", "mob-culling"}, {"ChunkMaster", "chunk"}, {"FastChunkPregenerator", "chunk"}, {"NoLagg", "entity", "chunk", "redstone"}, {"PaperTweaks", "entity", "chunk", "redstone"}, {"RedstoneOptimizer", "redstone"}, {"RedProtect", "redstone"}}) {
            String pluginName = conflict[0];
            if (excludedPlugins.contains(pluginName) || Bukkit.getPluginManager().getPlugin(pluginName) == null) continue;
            this.detectedConflicts.add(pluginName);
            this.getLogger().warning("Potential conflict detected with " + pluginName);
            for (int i = 1; i < conflict.length; ++i) {
                String feature = conflict[i];
                if (this.isFeatureConflicting(feature)) continue;
                this.getLogger().warning("- Disabling " + feature + " optimization to avoid conflicts");
            }
        }
    }

    private boolean isFeatureConflicting(String feature) {
        for (String pluginName : this.detectedConflicts) {
            String[][] potentialConflicts;
            for (String[] conflict : potentialConflicts = new String[][]{{"ClearLag", "entity"}, {"EntityManager", "entity"}, {"EntityTracker", "entity"}, {"MobStacker", "entity", "mob-culling"}, {"ChunkMaster", "chunk"}, {"FastChunkPregenerator", "chunk"}, {"NoLagg", "entity", "chunk", "redstone"}, {"PaperTweaks", "entity", "chunk", "redstone"}, {"RedstoneOptimizer", "redstone"}, {"RedProtect", "redstone"}}) {
                if (!conflict[0].equals(pluginName)) continue;
                for (int i = 1; i < conflict.length; ++i) {
                    if (!conflict[i].equals(feature)) continue;
                    return true;
                }
            }
        }
        return false;
    }

    private void setupMetrics() {
        this.getLogger().info("Metrics collection enabled");
    }

    public void reload() {
        if (this.crossPlatformManager != null) {
            this.crossPlatformManager.reload();
        }
        if (this.lagPredictionManager != null) {
            this.lagPredictionManager.shutdown();
            this.lagPredictionManager = null;
        }
        if (this.integrationAPI != null) {
            this.integrationAPI.shutdown();
            this.integrationAPI = null;
        }
        if (this.reportManager != null) {
            this.reportManager.shutdown();
            this.reportManager = null;
        }
        if (this.stressTestManager != null) {
            this.stressTestManager.shutdown();
            this.stressTestManager = null;
        }
        if (this.afkManager != null) {
            this.afkManager.shutdown();
            this.afkManager = null;
        }
        if (this.networkOptimizer != null) {
            this.networkOptimizer.shutdown();
            this.networkOptimizer = null;
        }
        if (this.taskOptimizer != null) {
            this.taskOptimizer.shutdown();
            this.taskOptimizer = null;
        }
        if (this.tntOptimizer != null) {
            this.tntOptimizer.shutdown();
            this.tntOptimizer = null;
        }
        if (this.entityDistanceLimiter != null) {
            this.entityDistanceLimiter.shutdown();
            this.entityDistanceLimiter = null;
        }
        if (this.memoryLeakDetector != null) {
            this.memoryLeakDetector.shutdown();
            this.memoryLeakDetector = null;
        }
        if (this.performanceMonitor != null) {
            this.performanceMonitor.shutdown();
            this.performanceMonitor = null;
        }
        if (this.mobCullingManager != null) {
            this.mobCullingManager.shutdown();
            this.mobCullingManager = null;
        }
        if (this.redstoneOptimizer != null) {
            this.redstoneOptimizer.shutdown();
            this.redstoneOptimizer = null;
        }
        if (this.entityOptimizer != null) {
            this.entityOptimizer.shutdown();
            this.entityOptimizer = null;
        }
        if (this.chunkManager != null) {
            this.chunkManager.shutdown();
            this.chunkManager = null;
        }
        this.detectedConflicts.clear();
        this.configManager.loadConfig();
        try {
            if (this.getConfig().getBoolean("general.auto-detect-conflicts", true)) {
                this.detectConflicts();
            }
        }
        catch (Exception e) {
            this.getLogger().warning("Error checking if auto-detect conflicts is enabled: " + e.getMessage());
        }
        this.initializeComponents();
        this.getLogger().info("MCOptimizer reloaded");
    }

    public static OptimizerPlugin getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public ChunkManager getChunkManager() {
        return this.chunkManager;
    }

    public EntityOptimizer getEntityOptimizer() {
        return this.entityOptimizer;
    }

    public PerformanceMonitor getPerformanceMonitor() {
        return this.performanceMonitor;
    }

    public RedstoneOptimizer getRedstoneOptimizer() {
        return this.redstoneOptimizer;
    }

    public MobCullingManager getMobCullingManager() {
        return this.mobCullingManager;
    }

    public ConfigurationAnalyzer getConfigAnalyzer() {
        return this.configAnalyzer;
    }

    public MemoryLeakDetector getMemoryLeakDetector() {
        return this.memoryLeakDetector;
    }

    public EntityDistanceLimiter getEntityDistanceLimiter() {
        return this.entityDistanceLimiter;
    }

    public TNTOptimizer getTntOptimizer() {
        return this.tntOptimizer;
    }

    public TaskOptimizer getTaskOptimizer() {
        return this.taskOptimizer;
    }

    public NetworkOptimizer getNetworkOptimizer() {
        return this.networkOptimizer;
    }

    public AfkManager getAfkManager() {
        return this.afkManager;
    }

    public StressTestManager getStressTestManager() {
        return this.stressTestManager;
    }

    public PerformanceReportManager getReportManager() {
        return this.reportManager;
    }

    public PluginIntegrationAPI getIntegrationAPI() {
        return this.integrationAPI;
    }

    public CrossPlatformManager getCrossPlatformManager() {
        return this.crossPlatformManager;
    }

    public LagPredictionManager getLagPredictionManager() {
        return this.lagPredictionManager;
    }
}

