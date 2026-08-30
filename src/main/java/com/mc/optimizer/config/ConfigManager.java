/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 *  org.bukkit.entity.EntityType
 */
package com.mc.optimizer.config;

import com.mc.optimizer.OptimizerPlugin;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

public class ConfigManager {
    private final OptimizerPlugin plugin;
    private final Logger logger;
    private FileConfiguration config;
    private File configFile;
    private boolean debugEnabled;
    private boolean performanceReportsEnabled;
    private int monitorInterval;
    private String prefix;
    private boolean metricsEnabled;
    private boolean autoDetectConflicts;
    private List<String> excludedPlugins;
    private boolean reloadCommandEnabled;
    private boolean chunkManagementEnabled;
    private boolean preGenerateSpawnChunks;
    private int maxChunksPerTick;
    private int spawnChunkRadius;
    private int playerChunkRadius;
    private int tickingRadius;
    private boolean unloadDynamically;
    private int chunkUnloadDelay;
    private boolean optimizeWorldSaving;
    private int worldSaveInterval;
    private boolean entityOptimizationEnabled;
    private int entityActivationRange;
    private boolean optimizeAI;
    private boolean optimizePathfinding;
    private int maxMobsPerChunk;
    private int despawnRange;
    private boolean reduceMobSpawns;
    private double mobSpawnFactor;
    private boolean mergeItems;
    private boolean smartXpMerge;
    private boolean redstoneOptimizationEnabled;
    private boolean limitRedstoneClock;
    private int maxTickRate;
    private boolean detectRedstoneClock;
    private int maxReplaceCount;
    private boolean disableRedstoneInUnusedChunks;
    private int redstoneExemptRadius;
    private boolean mobCullingEnabled;
    private int cullingIntervalTicks;
    private Map<EntityType, Integer> maxEntitiesPerType;
    private boolean intelligentCulling;
    private boolean prioritizeLaggingEntities;
    private int maxMobsPerPlayer;
    private boolean excludeBosses;
    private boolean excludeNamedMobs;
    private boolean performanceMonitorEnabled;
    private int monitorIntervalSeconds;
    private boolean autoWarnThreshold;
    private double tpsWarnThreshold;
    private boolean notifyAdmins;
    private boolean logToConsole;
    private boolean broadcastWarnings;
    private boolean configAnalyzerEnabled;
    private boolean analyzeSpigotYml;
    private boolean analyzePaperYml;
    private boolean analyzeBukkitYml;
    private boolean analyzeServerProperties;
    private boolean applyChanges;
    private boolean createBackups;
    private boolean memoryLeakDetectionEnabled;
    private int memoryCheckInterval;
    private int leakThresholdMb;
    private boolean highMemoryAlerts;
    private int highMemoryThresholdPercent;
    private boolean dumpHeapOnLeak;
    private boolean showLeakSources;
    private boolean showLeakSourcesEnabled;
    private boolean entityDistanceLimitEnabled;
    private int entityFullAIRadius;
    private int entityReducedAIRadius;
    private int entityMinimalAIRadius;
    private int entityNoAIRadius;
    private List<EntityType> entityExemptTypes;
    private boolean exemptNamedEntities;
    private boolean tntOptimizationEnabled;
    private int maxTntPerTick;
    private boolean batchPhysicsUpdates;
    private int maxExplosionsPerTick;
    private boolean dynamicExplosionScaling;
    private boolean optimizeParticles;
    private boolean taskOptimizationEnabled;
    private boolean monitorOtherPlugins;
    private boolean warnInefficient;
    private int minTaskTimeMs;
    private int maxTasksInReport;
    private boolean networkOptimizationEnabled;
    private boolean batchEntityPackets;
    private boolean prioritizePlayerFocus;
    private boolean reduceDistantPackets;
    private int compressionLevel;
    private int visibilityPercent;
    private boolean afkDetectionEnabled;
    private int afkTimeoutSeconds;
    private boolean reduceViewDistance;
    private int afkViewDistance;
    private boolean notifyPlayers;
    private boolean detectNoPositionChange;
    private boolean stressTestEnabled;
    private boolean requireOp;
    private int maxTestEntities;
    private int maxTestBlocks;
    private boolean allowInProduction;
    private boolean autoCleanup;
    private boolean autoGenerateReports;
    private int reportIntervalMinutes;
    private int historyDays;
    private boolean includePluginMetrics;
    private boolean exportToWeb;
    private int maxReports;
    private boolean integrationApiEnabled;
    private boolean logRegistrations;
    private boolean allowCustomOptimizers;
    private boolean allowMetricsAccess;
    private int defaultAccessLevel;

    public ConfigManager(OptimizerPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.entityExemptTypes = new ArrayList<EntityType>();
        this.maxEntitiesPerType = new HashMap<EntityType, Integer>();
        this.excludedPlugins = new ArrayList<String>();
    }

    public void loadConfig() {
        if (!this.configFile.exists()) {
            this.plugin.saveDefaultConfig();
        }
        this.plugin.reloadConfig();
        this.config = this.plugin.getConfig();
        this.loadGeneralSettings();
        this.loadChunkSettings();
        this.loadEntitySettings();
        this.loadRedstoneSettings();
        this.loadMobCullingSettings();
        this.loadPerformanceMonitorSettings();
        this.loadConfigAnalyzerSettings();
        this.loadMemoryLeakSettings();
        this.loadEntityDistanceLimitSettings();
        this.loadTntSettings();
        this.loadTaskSettings();
        this.loadNetworkSettings();
        this.loadAfkSettings();
        this.loadStressTestSettings();
        this.loadPerformanceReportSettings();
        this.loadIntegrationApiSettings();
        this.logger.info("Configuration loaded successfully");
    }

    public boolean isDebugEnabled() {
        return this.debugEnabled;
    }

    public boolean isPerformanceReportsEnabled() {
        return this.performanceReportsEnabled;
    }

    public int getMonitorInterval() {
        return this.monitorInterval;
    }

    public String getPrefix() {
        return this.prefix;
    }

    public boolean isMetricsEnabled() {
        return this.metricsEnabled;
    }

    public boolean isAutoDetectConflicts() {
        return this.autoDetectConflicts;
    }

    public List<String> getExcludedPlugins() {
        return this.excludedPlugins;
    }

    public boolean isReloadCommandEnabled() {
        return this.reloadCommandEnabled;
    }

    public boolean isChunkManagementEnabled() {
        return this.chunkManagementEnabled;
    }

    public boolean isPreGenerateSpawnChunks() {
        return this.preGenerateSpawnChunks;
    }

    public int getMaxChunksPerTick() {
        return this.maxChunksPerTick;
    }

    public int getSpawnChunkRadius() {
        return this.spawnChunkRadius;
    }

    public int getPlayerChunkRadius() {
        return this.playerChunkRadius;
    }

    public int getTickingRadius() {
        return this.tickingRadius;
    }

    public boolean isUnloadDynamically() {
        return this.unloadDynamically;
    }

    public int getChunkUnloadDelay() {
        return this.chunkUnloadDelay;
    }

    public boolean isOptimizeWorldSaving() {
        return this.optimizeWorldSaving;
    }

    public int getWorldSaveInterval() {
        return this.worldSaveInterval;
    }

    public boolean isEntityOptimizationEnabled() {
        return this.entityOptimizationEnabled;
    }

    public int getEntityActivationRange() {
        return this.entityActivationRange;
    }

    public boolean isOptimizeAI() {
        return this.optimizeAI;
    }

    public boolean isOptimizePathfinding() {
        return this.optimizePathfinding;
    }

    public int getMaxMobsPerChunk() {
        return this.maxMobsPerChunk;
    }

    public int getDespawnRange() {
        return this.despawnRange;
    }

    public boolean isReduceMobSpawns() {
        return this.reduceMobSpawns;
    }

    public double getMobSpawnFactor() {
        return this.mobSpawnFactor;
    }

    public boolean isMergeItems() {
        return this.mergeItems;
    }

    public boolean isSmartXpMerge() {
        return this.smartXpMerge;
    }

    public boolean isRedstoneOptimizationEnabled() {
        return this.redstoneOptimizationEnabled;
    }

    public boolean isLimitRedstoneClock() {
        return this.limitRedstoneClock;
    }

    public int getMaxTickRate() {
        return this.maxTickRate;
    }

    public boolean isDetectRedstoneClock() {
        return this.detectRedstoneClock;
    }

    public int getMaxReplaceCount() {
        return this.maxReplaceCount;
    }

    public boolean isDisableRedstoneInUnusedChunks() {
        return this.disableRedstoneInUnusedChunks;
    }

    public int getRedstoneExemptRadius() {
        return this.redstoneExemptRadius;
    }

    public boolean isMobCullingEnabled() {
        return this.mobCullingEnabled;
    }

    public int getCullingIntervalTicks() {
        return this.cullingIntervalTicks;
    }

    public Map<EntityType, Integer> getMaxEntitiesPerType() {
        return this.maxEntitiesPerType;
    }

    public boolean isIntelligentCulling() {
        return this.intelligentCulling;
    }

    public boolean isPrioritizeLaggingEntities() {
        return this.prioritizeLaggingEntities;
    }

    public int getMaxMobsPerPlayer() {
        return this.maxMobsPerPlayer;
    }

    public boolean isExcludeBosses() {
        return this.excludeBosses;
    }

    public boolean isExcludeNamedMobs() {
        return this.excludeNamedMobs;
    }

    public boolean isPerformanceMonitorEnabled() {
        return this.performanceMonitorEnabled;
    }

    public int getMonitorIntervalSeconds() {
        return this.monitorIntervalSeconds;
    }

    public boolean isAutoWarnThreshold() {
        return this.autoWarnThreshold;
    }

    public double getTpsWarnThreshold() {
        return this.tpsWarnThreshold;
    }

    public boolean isNotifyAdmins() {
        return this.notifyAdmins;
    }

    public boolean isLogToConsole() {
        return this.logToConsole;
    }

    public boolean isBroadcastWarnings() {
        return this.broadcastWarnings;
    }

    public boolean isConfigAnalyzerEnabled() {
        return this.configAnalyzerEnabled;
    }

    public boolean isAnalyzeSpigotYml() {
        return this.analyzeSpigotYml;
    }

    public boolean isAnalyzePaperYml() {
        return this.analyzePaperYml;
    }

    public boolean isAnalyzeBukkitYml() {
        return this.analyzeBukkitYml;
    }

    public boolean isAnalyzeServerProperties() {
        return this.analyzeServerProperties;
    }

    public boolean isApplyChanges() {
        return this.applyChanges;
    }

    public boolean isCreateBackups() {
        return this.createBackups;
    }

    public boolean isMemoryLeakDetectionEnabled() {
        return this.memoryLeakDetectionEnabled;
    }

    public int getMemoryCheckInterval() {
        return this.memoryCheckInterval;
    }

    public int getLeakThresholdMb() {
        return this.leakThresholdMb;
    }

    public boolean isHighMemoryAlerts() {
        return this.highMemoryAlerts;
    }

    public int getHighMemoryThresholdPercent() {
        return this.highMemoryThresholdPercent;
    }

    public boolean isDumpHeapOnLeak() {
        return this.dumpHeapOnLeak;
    }

    public boolean isShowLeakSources() {
        return this.showLeakSources;
    }

    public boolean isShowLeakSourcesEnabled() {
        return this.showLeakSourcesEnabled;
    }

    public boolean isEntityDistanceLimitEnabled() {
        return this.entityDistanceLimitEnabled;
    }

    public int getEntityFullAIRadius() {
        return this.entityFullAIRadius;
    }

    public int getEntityReducedAIRadius() {
        return this.entityReducedAIRadius;
    }

    public int getEntityMinimalAIRadius() {
        return this.entityMinimalAIRadius;
    }

    public int getEntityNoAIRadius() {
        return this.entityNoAIRadius;
    }

    public List<EntityType> getEntityExemptTypes() {
        return this.entityExemptTypes;
    }

    public boolean isExemptNamedEntities() {
        return this.exemptNamedEntities;
    }

    public boolean isTntOptimizationEnabled() {
        return this.tntOptimizationEnabled;
    }

    public int getMaxTntPerTick() {
        return this.maxTntPerTick;
    }

    public boolean isBatchPhysicsUpdates() {
        return this.batchPhysicsUpdates;
    }

    public int getMaxExplosionsPerTick() {
        return this.maxExplosionsPerTick;
    }

    public boolean isDynamicExplosionScaling() {
        return this.dynamicExplosionScaling;
    }

    public boolean isOptimizeParticles() {
        return this.optimizeParticles;
    }

    public boolean isTaskOptimizationEnabled() {
        return this.taskOptimizationEnabled;
    }

    public boolean isMonitorOtherPlugins() {
        return this.monitorOtherPlugins;
    }

    public boolean isWarnInefficient() {
        return this.warnInefficient;
    }

    public int getMinTaskTimeMs() {
        return this.minTaskTimeMs;
    }

    public int getMaxTasksInReport() {
        return this.maxTasksInReport;
    }

    public boolean isNetworkOptimizationEnabled() {
        return this.networkOptimizationEnabled;
    }

    public boolean isBatchEntityPackets() {
        return this.batchEntityPackets;
    }

    public boolean isPrioritizePlayerFocus() {
        return this.prioritizePlayerFocus;
    }

    public boolean isReduceDistantPackets() {
        return this.reduceDistantPackets;
    }

    public int getCompressionLevel() {
        return this.compressionLevel;
    }

    public int getVisibilityPercent() {
        return this.visibilityPercent;
    }

    public boolean isAfkDetectionEnabled() {
        return this.afkDetectionEnabled;
    }

    public int getAfkTimeoutSeconds() {
        return this.afkTimeoutSeconds;
    }

    public boolean isReduceViewDistance() {
        return this.reduceViewDistance;
    }

    public int getAfkViewDistance() {
        return this.afkViewDistance;
    }

    public boolean isNotifyPlayers() {
        return this.notifyPlayers;
    }

    public boolean isDetectNoPositionChange() {
        return this.detectNoPositionChange;
    }

    public boolean isStressTestEnabled() {
        return this.stressTestEnabled;
    }

    public boolean isRequireOp() {
        return this.requireOp;
    }

    public int getMaxTestEntities() {
        return this.maxTestEntities;
    }

    public int getMaxTestBlocks() {
        return this.maxTestBlocks;
    }

    public boolean isAllowInProduction() {
        return this.allowInProduction;
    }

    public boolean isAutoCleanup() {
        return this.autoCleanup;
    }

    public boolean isAutoGenerateReports() {
        return this.autoGenerateReports;
    }

    public int getReportIntervalMinutes() {
        return this.reportIntervalMinutes;
    }

    public int getHistoryDays() {
        return this.historyDays;
    }

    public boolean isIncludePluginMetrics() {
        return this.includePluginMetrics;
    }

    public boolean isExportToWeb() {
        return this.exportToWeb;
    }

    public int getMaxReports() {
        return this.maxReports;
    }

    public boolean isIntegrationApiEnabled() {
        return this.integrationApiEnabled;
    }

    public boolean isLogRegistrations() {
        return this.logRegistrations;
    }

    public boolean isAllowCustomOptimizers() {
        return this.allowCustomOptimizers;
    }

    public boolean isAllowMetricsAccess() {
        return this.allowMetricsAccess;
    }

    public int getDefaultAccessLevel() {
        return this.defaultAccessLevel;
    }

    private void loadGeneralSettings() {
        this.debugEnabled = this.config.getBoolean("general.debug", false);
        this.performanceReportsEnabled = this.config.getBoolean("general.performance-reports", true);
        this.monitorInterval = this.config.getInt("general.monitor-interval", 60);
        this.prefix = this.config.getString("general.prefix", "&8[&bMCOptimizer&8]&r");
        this.metricsEnabled = this.config.getBoolean("general.metrics-enabled", true);
        this.autoDetectConflicts = this.config.getBoolean("general.auto-detect-conflicts", true);
        this.excludedPlugins = this.config.getStringList("general.excluded-plugins");
        this.reloadCommandEnabled = this.config.getBoolean("general.reload-command-enabled", true);
    }

    private void loadChunkSettings() {
        this.chunkManagementEnabled = this.config.getBoolean("chunk.enabled", true);
        this.preGenerateSpawnChunks = this.config.getBoolean("chunk.pre-generate-spawn", true);
        this.maxChunksPerTick = this.config.getInt("chunk.max-chunks-per-tick", 45);
        this.spawnChunkRadius = this.config.getInt("chunk.spawn-chunk-radius", 10);
        this.playerChunkRadius = this.config.getInt("chunk.player-chunk-radius", 6);
        this.tickingRadius = this.config.getInt("chunk.ticking-radius", 4);
        this.unloadDynamically = this.config.getBoolean("chunk.unload-dynamically", true);
        this.chunkUnloadDelay = this.config.getInt("chunk.unload-delay", 30);
        this.optimizeWorldSaving = this.config.getBoolean("chunk.optimize-world-saving", true);
        this.worldSaveInterval = this.config.getInt("chunk.world-save-interval", 300);
    }

    private void loadEntitySettings() {
        this.entityOptimizationEnabled = this.config.getBoolean("entity.enabled", true);
        this.entityActivationRange = this.config.getInt("entity.activation-range", 32);
        this.optimizeAI = this.config.getBoolean("entity.optimize-ai", true);
        this.optimizePathfinding = this.config.getBoolean("entity.optimize-pathfinding", true);
        this.maxMobsPerChunk = this.config.getInt("entity.max-mobs-per-chunk", 8);
        this.despawnRange = this.config.getInt("entity.despawn-range", 128);
        this.reduceMobSpawns = this.config.getBoolean("entity.reduce-mob-spawns", true);
        this.mobSpawnFactor = this.config.getDouble("entity.mob-spawn-factor", 0.8);
        this.mergeItems = this.config.getBoolean("entity.merge-items", true);
        this.smartXpMerge = this.config.getBoolean("entity.smart-xp-merge", true);
    }

    private void loadRedstoneSettings() {
        this.redstoneOptimizationEnabled = this.config.getBoolean("redstone.enabled", true);
        this.limitRedstoneClock = this.config.getBoolean("redstone.limit-redstone-clock", true);
        this.maxTickRate = this.config.getInt("redstone.max-tick-rate", 10);
        this.detectRedstoneClock = this.config.getBoolean("redstone.detect-redstone-clock", true);
        this.maxReplaceCount = this.config.getInt("redstone.max-replace-count", 100);
        this.disableRedstoneInUnusedChunks = this.config.getBoolean("redstone.disable-in-unused-chunks", true);
        this.redstoneExemptRadius = this.config.getInt("redstone.exempt-radius", 8);
    }

    private void loadMobCullingSettings() {
        this.mobCullingEnabled = this.config.getBoolean("mob-culling.enabled", true);
        this.cullingIntervalTicks = this.config.getInt("mob-culling.interval-ticks", 600);
        this.intelligentCulling = this.config.getBoolean("mob-culling.intelligent-culling", true);
        this.prioritizeLaggingEntities = this.config.getBoolean("mob-culling.prioritize-lagging-entities", true);
        this.maxMobsPerPlayer = this.config.getInt("mob-culling.max-mobs-per-player", 20);
        this.excludeBosses = this.config.getBoolean("mob-culling.exclude-bosses", true);
        this.excludeNamedMobs = this.config.getBoolean("mob-culling.exclude-named-mobs", true);
    }

    private void loadPerformanceMonitorSettings() {
        this.performanceMonitorEnabled = this.config.getBoolean("performance-monitor.enabled", true);
        this.monitorIntervalSeconds = this.config.getInt("performance-monitor.interval-seconds", 30);
        this.autoWarnThreshold = this.config.getBoolean("performance-monitor.auto-warn-threshold", true);
        this.tpsWarnThreshold = this.config.getDouble("performance-monitor.tps-warn-threshold", 17.5);
        this.notifyAdmins = this.config.getBoolean("performance-monitor.notify-admins", true);
        this.logToConsole = this.config.getBoolean("performance-monitor.log-to-console", true);
        this.broadcastWarnings = this.config.getBoolean("performance-monitor.broadcast-warnings", false);
    }

    private void loadConfigAnalyzerSettings() {
        this.configAnalyzerEnabled = this.config.getBoolean("config-analyzer.enabled", true);
        this.analyzeSpigotYml = this.config.getBoolean("config-analyzer.analyze-spigot-yml", true);
        this.analyzePaperYml = this.config.getBoolean("config-analyzer.analyze-paper-yml", true);
        this.analyzeBukkitYml = this.config.getBoolean("config-analyzer.analyze-bukkit-yml", true);
        this.analyzeServerProperties = this.config.getBoolean("config-analyzer.analyze-server-properties", true);
        this.applyChanges = this.config.getBoolean("config-analyzer.apply-changes", false);
        this.createBackups = this.config.getBoolean("config-analyzer.create-backups", true);
    }

    private void loadMemoryLeakSettings() {
        this.memoryLeakDetectionEnabled = this.config.getBoolean("memory-leak-detection.enabled", true);
        this.memoryCheckInterval = this.config.getInt("memory-leak-detection.check-interval", 300);
        this.leakThresholdMb = this.config.getInt("memory-leak-detection.leak-threshold-mb", 500);
        this.highMemoryAlerts = this.config.getBoolean("memory-leak-detection.high-memory-alerts", true);
        this.highMemoryThresholdPercent = this.config.getInt("memory-leak-detection.high-memory-threshold-percent", 80);
        this.dumpHeapOnLeak = this.config.getBoolean("memory-leak-detection.dump-heap-on-leak", false);
        this.showLeakSourcesEnabled = this.showLeakSources = this.config.getBoolean("memory-leak-detection.show-leak-sources", true);
    }

    private void loadEntityDistanceLimitSettings() {
        this.entityDistanceLimitEnabled = this.config.getBoolean("entity-distance-limit.enabled", true);
        this.entityFullAIRadius = this.config.getInt("entity-distance-limit.full-ai-radius", 24);
        this.entityReducedAIRadius = this.config.getInt("entity-distance-limit.reduced-ai-radius", 32);
        this.entityMinimalAIRadius = this.config.getInt("entity-distance-limit.minimal-ai-radius", 48);
        this.entityNoAIRadius = this.config.getInt("entity-distance-limit.no-ai-radius", 64);
        List<String> exemptTypeStrings = this.config.getStringList("entity-distance-limit.exempt-types");
        this.entityExemptTypes = new ArrayList<EntityType>();
        for (String typeString : exemptTypeStrings) {
            try {
                EntityType type = EntityType.valueOf((String)typeString.toUpperCase());
                this.entityExemptTypes.add(type);
            }
            catch (IllegalArgumentException e) {
                this.logger.warning("Invalid entity type in exempt-types: " + typeString);
            }
        }
        this.exemptNamedEntities = this.config.getBoolean("entity-distance-limit.exempt-named-entities", true);
    }

    private void loadTntSettings() {
        this.tntOptimizationEnabled = this.config.getBoolean("tnt-optimization.enabled", true);
        this.maxTntPerTick = this.config.getInt("tnt-optimization.max-tnt-per-tick", 20);
        this.batchPhysicsUpdates = this.config.getBoolean("tnt-optimization.batch-physics-updates", true);
        this.maxExplosionsPerTick = this.config.getInt("tnt-optimization.max-explosions-per-tick", 5);
        this.dynamicExplosionScaling = this.config.getBoolean("tnt-optimization.dynamic-explosion-scaling", true);
        this.optimizeParticles = this.config.getBoolean("tnt-optimization.optimize-particles", true);
    }

    private void loadTaskSettings() {
        this.taskOptimizationEnabled = this.config.getBoolean("task-optimization.enabled", true);
        this.monitorOtherPlugins = this.config.getBoolean("task-optimization.monitor-other-plugins", true);
        this.warnInefficient = this.config.getBoolean("task-optimization.warn-inefficient", true);
        this.minTaskTimeMs = this.config.getInt("task-optimization.min-task-time-ms", 50);
        this.maxTasksInReport = this.config.getInt("task-optimization.max-tasks-in-report", 10);
    }

    private void loadNetworkSettings() {
        this.networkOptimizationEnabled = this.config.getBoolean("network-optimization.enabled", true);
        this.batchEntityPackets = this.config.getBoolean("network-optimization.batch-entity-packets", true);
        this.prioritizePlayerFocus = this.config.getBoolean("network-optimization.prioritize-player-focus", true);
        this.reduceDistantPackets = this.config.getBoolean("network-optimization.reduce-distant-packets", true);
        this.compressionLevel = this.config.getInt("network-optimization.compression-level", 3);
        this.visibilityPercent = this.config.getInt("network-optimization.visibility-percent", 70);
    }

    private void loadAfkSettings() {
        this.afkDetectionEnabled = this.config.getBoolean("afk-detection.enabled", true);
        this.afkTimeoutSeconds = this.config.getInt("afk-detection.timeout-seconds", 300);
        this.reduceViewDistance = this.config.getBoolean("afk-detection.reduce-view-distance", true);
        this.afkViewDistance = this.config.getInt("afk-detection.afk-view-distance", 3);
        this.notifyPlayers = this.config.getBoolean("afk-detection.notify-players", true);
        this.detectNoPositionChange = this.config.getBoolean("afk-detection.detect-no-position-change", true);
    }

    private void loadStressTestSettings() {
        this.stressTestEnabled = this.config.getBoolean("stress-test.enabled", false);
        this.requireOp = this.config.getBoolean("stress-test.require-op", true);
        this.maxTestEntities = this.config.getInt("stress-test.max-test-entities", 500);
        this.maxTestBlocks = this.config.getInt("stress-test.max-test-blocks", 10000);
        this.allowInProduction = this.config.getBoolean("stress-test.allow-in-production", false);
        this.autoCleanup = this.config.getBoolean("stress-test.auto-cleanup", true);
    }

    private void loadPerformanceReportSettings() {
        this.autoGenerateReports = this.config.getBoolean("performance-reports.auto-generate", true);
        this.reportIntervalMinutes = this.config.getInt("performance-reports.interval-minutes", 60);
        this.historyDays = this.config.getInt("performance-reports.history-days", 7);
        this.includePluginMetrics = this.config.getBoolean("performance-reports.include-plugin-metrics", true);
        this.exportToWeb = this.config.getBoolean("performance-reports.export-to-web", false);
        this.maxReports = this.config.getInt("performance-reports.max-reports", 100);
    }

    private void loadIntegrationApiSettings() {
        this.integrationApiEnabled = this.config.getBoolean("integration-api.enabled", true);
        this.logRegistrations = this.config.getBoolean("integration-api.log-registrations", true);
        this.allowCustomOptimizers = this.config.getBoolean("integration-api.allow-custom-optimizers", true);
        this.allowMetricsAccess = this.config.getBoolean("integration-api.allow-metrics-access", true);
        this.defaultAccessLevel = this.config.getInt("integration-api.default-access-level", 1);
    }

    public void saveConfig() {
        try {
            this.config.save(this.configFile);
            this.logger.info("Configuration saved successfully");
        }
        catch (IOException e) {
            this.logger.severe("Could not save config to " + this.configFile.getAbsolutePath() + ": " + e.getMessage());
        }
    }
}

