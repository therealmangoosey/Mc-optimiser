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

public class OptimizerPlugin extends JavaPlugin {
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
        logger.info("╔═══════════════════════════════════════════════╗");
        logger.info("║            MCOptimizer Plugin              ║");
        logger.info("║    Optimize your server with minimal       ║");
        logger.info("║            CPU/RAM overhead!               ║");
        logger.info("╠═══════════════════════════════════════════════╣");
        logger.info("║ Created by: juicyyfruittsnackss            ║");
        logger.info("║ Support: https://dcs.gg/fruitsnacks/       ║");
        logger.info("║ Projects: https://fruitsnacks.xyz          ║");
        logger.info("║ Version: " + this.getDescription().getVersion() + "                            ║");
        logger.info("╚═══════════════════════════════════════════════╝");
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
        } catch (Exception e) {
            logger.warning("Error checking if auto-detect conflicts is enabled: " + e.getMessage());
        }
        this.initializeComponents();
        final OptimizerCommand optimizerCommand = new OptimizerCommand(this);
        PluginCommand mcOptimizerCmd = Bukkit.getPluginCommand("mcoptimizer");
        if (mcOptimizerCmd == null) {
            this.getLogger().info("Registering mcoptimizer command manually");
            Command mainCommand = new Command("mcoptimizer") {
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
            mcOptimizerCmd.setExecutor(optimizerCommand);
            mcOptimizerCmd.setTabCompleter(optimizerCommand);
        }
        try {
            if (this.getConfig().getBoolean("general.metrics-enabled", true)) {
                this.setupMetrics();
            }
        } catch (Exception e) {
            logger.warning("Error checking if metrics are enabled: " + e.getMessage());
        }
        String version = this.getDescription().getVersion();
        logger.info("MCOptimizer v" + version + " enabled successfully");
        UpdateChecker.check(this);
        if (!this.detectedConflicts.isEmpty()) {
            logger.warning("Potential conflicts detected with: " + String.join(", ", this.detectedConflicts));
            logger.warning("Some features may be disabled to avoid conflicts.");
        }
    }

    public void onDisable() {
        if (this.crossPlatformManager != null) { }
        if (this.lagPredictionManager != null) this.lagPredictionManager.shutdown();
        if (this.integrationAPI != null) this.integrationAPI.shutdown();
        if (this.reportManager != null) this.reportManager.shutdown();
        if (this.stressTestManager != null) this.stressTestManager.shutdown();
        if (this.afkManager != null) this.afkManager.shutdown();
        if (this.networkOptimizer != null) this.networkOptimizer.shutdown();
        if (this.taskOptimizer != null) this.taskOptimizer.shutdown();
        if (this.tntOptimizer != null) this.tntOptimizer.shutdown();
        if (this.entityDistanceLimiter != null) this.entityDistanceLimiter.shutdown();
        if (this.memoryLeakDetector != null) this.memoryLeakDetector.shutdown();
        if (this.performanceMonitor != null) this.performanceMonitor.shutdown();
        if (this.mobCullingManager != null) this.mobCullingManager.shutdown();
        if (this.redstoneOptimizer != null) this.redstoneOptimizer.shutdown();
        if (this.entityOptimizer != null) this.entityOptimizer.shutdown();
        if (this.chunkManager != null) this.chunkManager.shutdown();
        this.getLogger().info("MCOptimizer disabled");
        this.getLogger().info("For support go to: https://dcs.gg/fruitsnacks/");
        this.getLogger().info("Check https://fruitsnacks.xyz for upcoming projects");
    }

    private void initializeComponents() {
        Logger logger = this.getLogger();
        try { if (this.getConfig().getBoolean("chunk.enabled", true) && !this.isFeatureConflicting("chunk")) { logger.info("Initializing chunk management..."); this.chunkManager = new ChunkManager(this, new ChunkConfigManager(this)); } else logger.info("Chunk management disabled"); } catch (Exception e) { logger.warning("Error initializing chunk management: " + e.getMessage()); }
        try { if (this.getConfig().getBoolean("entity.enabled", true) && !this.isFeatureConflicting("entity")) { logger.info("Initializing entity optimization..."); this.entityOptimizer = new EntityOptimizer(this); } else logger.info("Entity optimization disabled"); } catch (Exception e) { logger.warning("Error initializing entity optimization: " + e.getMessage()); }
        try { if (this.getConfig().getBoolean("redstone.enabled", true) && !this.isFeatureConflicting("redstone")) { logger.info("Initializing redstone optimization..."); this.redstoneOptimizer = new RedstoneOptimizer(this); } else logger.info("Redstone optimization disabled"); } catch (Exception e) { logger.warning("Error initializing redstone optimization: " + e.getMessage()); }
        try { if (this.getConfig().getBoolean("mob-culling.enabled", true) && !this.isFeatureConflicting("mob-culling")) { logger.info("Initializing mob culling..."); this.mobCullingManager = new MobCullingManager(this); } else logger.info("Mob culling disabled"); } catch (Exception e) { logger.warning("Error initializing mob culling: " + e.getMessage()); }
        try { if (this.getConfig().getBoolean("performance-monitor.enabled", true) && !this.isFeatureConflicting("performance-monitor")) { logger.info("Initializing performance monitor..."); this.performanceMonitor = new PerformanceMonitor(this); } else logger.info("Performance monitor disabled"); } catch (Exception e) { logger.warning("Error initializing performance monitor: " + e.getMessage()); }
        try { if (this.getConfig().getBoolean("memory-leak.enabled", true)) { logger.info("Initializing memory leak detector..."); this.memoryLeakDetector = new MemoryLeakDetector(this); } } catch (Exception e) { logger.warning("Error initializing memory leak detector: " + e.getMessage()); }
        try { if (this.getConfig().getBoolean("entity-distance-limit.enabled", true)) { logger.info("Initializing entity distance limiter..."); this.entityDistanceLimiter = new EntityDistanceLimiter(this); } } catch (Exception e) { logger.warning("Error initializing entity distance limiter: " + e.getMessage()); }
        try { if (this.getConfig().getBoolean("tnt-optimization.enabled", true)) { logger.info("Initializing TNT optimizer..."); this.tntOptimizer = new TNTOptimizer(this); } } catch (Exception e) { logger.warning("Error initializing TNT optimizer: " + e.getMessage()); }
        try { if (this.getConfig().getBoolean("task-optimization.enabled", true)) { logger.info("Initializing task optimizer..."); this.taskOptimizer = new TaskOptimizer(this); } } catch (Exception e) { logger.warning("Error initializing task optimizer: " + e.getMessage()); }
        try { if (this.getConfig().getBoolean("network-optimization.enabled", false)) { logger.info("Initializing network optimizer..."); this.networkOptimizer = new NetworkOptimizer(this); } } catch (Exception e) { logger.warning("Error initializing network optimizer: " + e.getMessage()); }
        try { if (this.getConfig().getBoolean("afk-detection.enabled", true)) { logger.info("Initializing AFK detection..."); this.afkManager = new AfkManager(this); } } catch (Exception e) { logger.warning("Error initializing AFK detection: " + e.getMessage()); }
        try { if (this.getConfig().getBoolean("stress-test.enabled", false)) { logger.info("Initializing stress test manager..."); this.stressTestManager = new StressTestManager(this); } } catch (Exception e) { logger.warning("Error initializing stress test manager: " + e.getMessage()); }
        try { if (this.getConfig().getBoolean("performance-reports.enabled", true)) { logger.info("Initializing performance reports..."); this.reportManager = new PerformanceReportManager(this); } } catch (Exception e) { logger.warning("Error initializing performance reports: " + e.getMessage()); }
        try { if (this.getConfig().getBoolean("integration-api.enabled", true)) { logger.info("Initializing integration API..."); this.integrationAPI = new PluginIntegrationAPI(this); } } catch (Exception e) { logger.warning("Error initializing integration API: " + e.getMessage()); }
        try { if (this.getConfig().getBoolean("compatibility.cross-platform.geyser-support", true) || this.getConfig().getBoolean("compatibility.cross-platform.floodgate-compatibility", true)) { logger.info("Initializing cross-platform support..."); this.crossPlatformManager = new CrossPlatformManager(this); } } catch (Exception e) { logger.warning("Error initializing cross-platform support: " + e.getMessage()); }
        try { if (this.getConfig().getBoolean("lag-prediction.enabled", true)) { logger.info("Initializing lag prediction..."); this.lagPredictionManager = new LagPredictionManager(this); } } catch (Exception e) { logger.warning("Error initializing lag prediction: " + e.getMessage()); }
    }
}
