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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

/** Main MCOptimizer plugin entry point. */
public final class OptimizerPlugin extends JavaPlugin {
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
    private final List<String> detectedConflicts = new ArrayList<>();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        detectConflictsIfEnabled();
        registerCommand();
        initializeComponents();
        getLogger().info("MCOptimizer v" + getDescription().getVersion() + " enabled.");
        if (!detectedConflicts.isEmpty()) {
            getLogger().warning("Potential conflicts detected: " + String.join(", ", detectedConflicts));
        }
    }

    private void registerCommand() {
        OptimizerCommand command = new OptimizerCommand(this);
        if (getCommand("mcoptimizer") == null) {
            getLogger().severe("Command 'mcoptimizer' is missing from plugin.yml; disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        getCommand("mcoptimizer").setExecutor(command);
        getCommand("mcoptimizer").setTabCompleter(command);
    }

    private void initializeComponents() {
        initialize("chunk", getConfig().getBoolean("chunk.enabled", true),
                () -> chunkManager = new ChunkManager(this, new ChunkConfigManager(this)));
        initialize("entity", getConfig().getBoolean("entity.enabled", true),
                () -> entityOptimizer = new EntityOptimizer(this, configManager));
        initialize("redstone", getConfig().getBoolean("redstone.enabled", false),
                () -> redstoneOptimizer = new RedstoneOptimizer(this, configManager));
        initialize("mob-culling", getConfig().getBoolean("mob-culling.enabled", false),
                () -> mobCullingManager = new MobCullingManager(this, configManager));
        initialize("performance-monitor", getConfig().getBoolean("performance-monitor.enabled", true),
                () -> {
                    performanceMonitor = new PerformanceMonitor(this, configManager);
                    performanceMonitor.startMonitoring();
                });
        initialize("config-analyzer", getConfig().getBoolean("config-analyzer.enabled", false),
                () -> configAnalyzer = new ConfigurationAnalyzer(this));
        initialize("memory-leak", getConfig().getBoolean("memory-leak.enabled", false),
                () -> memoryLeakDetector = new MemoryLeakDetector(this, configManager));
        initialize("entity-distance-limit", getConfig().getBoolean("entity-distance-limit.enabled", false)
                        && !isFeatureConflicting("entity"),
                () -> entityDistanceLimiter = new EntityDistanceLimiter(this, configManager));
        initialize("tnt-optimization", getConfig().getBoolean("tnt-optimization.enabled", false),
                () -> tntOptimizer = new TNTOptimizer(this, configManager));
        initialize("task-optimization", getConfig().getBoolean("task-optimization.enabled", false),
                () -> taskOptimizer = new TaskOptimizer(this, configManager));
        initialize("network-optimization", getConfig().getBoolean("network-optimization.enabled", false),
                () -> networkOptimizer = new NetworkOptimizer(this, configManager));
        initialize("afk-detection", getConfig().getBoolean("afk-detection.enabled", false),
                () -> afkManager = new AfkManager(this, configManager));
        initialize("stress-test", getConfig().getBoolean("stress-test.enabled", false),
                () -> stressTestManager = new StressTestManager(this, configManager));
        initialize("performance-reports", getConfig().getBoolean("performance-reports.enabled", false),
                () -> reportManager = new PerformanceReportManager(this, configManager));
        initialize("integration-api", getConfig().getBoolean("integration-api.enabled", true),
                () -> integrationAPI = new PluginIntegrationAPI(this, configManager));
        initialize("cross-platform", true, () -> crossPlatformManager = new CrossPlatformManager(this));
        initialize("lag-prediction", getConfig().getBoolean("lag-prediction.enabled", false),
                () -> lagPredictionManager = new LagPredictionManager(this));
    }

    private void initialize(String name, boolean enabled, Runnable initializer) {
        if (!enabled || isFeatureConflicting(name)) return;
        try {
            initializer.run();
        } catch (Exception e) {
            getLogger().warning("Failed to initialize " + name + ": " + e.getMessage());
        }
    }

    private void detectConflictsIfEnabled() {
        if (!getConfig().getBoolean("general.auto-detect-conflicts", true)) return;
        detectedConflicts.clear();
        String[] pluginNames = {"ClearLag", "EntityManager", "EntityTracker", "MobStacker", "ChunkMaster",
                "FastChunkPregenerator", "NoLagg", "PaperTweaks", "RedstoneOptimizer", "RedProtect"};
        List<String> excluded = getConfig().getStringList("general.excluded-plugins");
        PluginManager manager = getServer().getPluginManager();
        for (String pluginName : pluginNames) {
            if (!excluded.contains(pluginName) && manager.getPlugin(pluginName) != null) {
                detectedConflicts.add(pluginName);
            }
        }
    }

    private boolean isFeatureConflicting(String feature) {
        for (String pluginName : detectedConflicts) {
            if (feature.equals("entity") && (pluginName.equals("ClearLag") || pluginName.equals("EntityManager")
                    || pluginName.equals("EntityTracker") || pluginName.equals("MobStacker")
                    || pluginName.equals("NoLagg") || pluginName.equals("PaperTweaks"))) return true;
            if (feature.equals("mob-culling") && (pluginName.equals("MobStacker") || pluginName.equals("NoLagg"))) return true;
            if (feature.equals("chunk") && (pluginName.equals("ChunkMaster") || pluginName.equals("FastChunkPregenerator")
                    || pluginName.equals("NoLagg") || pluginName.equals("PaperTweaks"))) return true;
            if (feature.equals("redstone") && (pluginName.equals("NoLagg") || pluginName.equals("PaperTweaks")
                    || pluginName.equals("RedstoneOptimizer") || pluginName.equals("RedProtect"))) return true;
        }
        return false;
    }

    public void reload() {
        shutdownComponents();
        reloadConfig();
        configManager.loadConfig();
        detectConflictsIfEnabled();
        initializeComponents();
        getLogger().info("MCOptimizer reloaded.");
    }

    @Override
    public void onDisable() {
        shutdownComponents();
        instance = null;
        getLogger().info("MCOptimizer disabled.");
    }

    private void shutdownComponents() {
        shutdown(lagPredictionManager, LagPredictionManager::shutdown); lagPredictionManager = null;
        shutdown(integrationAPI, PluginIntegrationAPI::shutdown); integrationAPI = null;
        shutdown(reportManager, PerformanceReportManager::shutdown); reportManager = null;
        shutdown(stressTestManager, StressTestManager::shutdown); stressTestManager = null;
        shutdown(afkManager, AfkManager::shutdown); afkManager = null;
        shutdown(networkOptimizer, NetworkOptimizer::shutdown); networkOptimizer = null;
        shutdown(taskOptimizer, TaskOptimizer::shutdown); taskOptimizer = null;
        shutdown(tntOptimizer, TNTOptimizer::shutdown); tntOptimizer = null;
        shutdown(entityDistanceLimiter, EntityDistanceLimiter::shutdown); entityDistanceLimiter = null;
        shutdown(memoryLeakDetector, MemoryLeakDetector::shutdown); memoryLeakDetector = null;
        shutdown(performanceMonitor, PerformanceMonitor::shutdown); performanceMonitor = null;
        shutdown(mobCullingManager, MobCullingManager::shutdown); mobCullingManager = null;
        shutdown(redstoneOptimizer, RedstoneOptimizer::shutdown); redstoneOptimizer = null;
        shutdown(entityOptimizer, EntityOptimizer::shutdown); entityOptimizer = null;
        shutdown(chunkManager, ChunkManager::shutdown); chunkManager = null;
        crossPlatformManager = null;
    }

    private <T> void shutdown(T component, Consumer<T> action) {
        if (component == null) return;
        try {
            action.accept(component);
        } catch (Exception e) {
            getLogger().warning("Error shutting down component: " + e.getMessage());
        }
    }

    public static OptimizerPlugin getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public ChunkManager getChunkManager() { return chunkManager; }
    public EntityOptimizer getEntityOptimizer() { return entityOptimizer; }
    public PerformanceMonitor getPerformanceMonitor() { return performanceMonitor; }
    public RedstoneOptimizer getRedstoneOptimizer() { return redstoneOptimizer; }
    public MobCullingManager getMobCullingManager() { return mobCullingManager; }
    public ConfigurationAnalyzer getConfigAnalyzer() { return configAnalyzer; }
    public MemoryLeakDetector getMemoryLeakDetector() { return memoryLeakDetector; }
    public EntityDistanceLimiter getEntityDistanceLimiter() { return entityDistanceLimiter; }
    public TNTOptimizer getTntOptimizer() { return tntOptimizer; }
    public TaskOptimizer getTaskOptimizer() { return taskOptimizer; }
    public NetworkOptimizer getNetworkOptimizer() { return networkOptimizer; }
    public AfkManager getAfkManager() { return afkManager; }
    public StressTestManager getStressTestManager() { return stressTestManager; }
    public PerformanceReportManager getReportManager() { return reportManager; }
    public PluginIntegrationAPI getIntegrationAPI() { return integrationAPI; }
    public CrossPlatformManager getCrossPlatformManager() { return crossPlatformManager; }
    public LagPredictionManager getLagPredictionManager() { return lagPredictionManager; }
}
