/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.configuration.file.FileConfiguration
 */
package com.mc.optimizer.chunk.config;

import com.mc.optimizer.OptimizerPlugin;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;

public class ChunkConfigManager {
    private final OptimizerPlugin plugin;
    private final Logger logger;
    private FileConfiguration config;
    private File configFile;
    private boolean debugEnabled;
    private boolean performanceReportsEnabled;
    private int monitorInterval;
    private boolean chunkManagementEnabled;
    private int maxChunksPerTick;
    private int unloadAfterSeconds;
    private int maxViewDistance;
    private int spawnChunkRadius;
    private boolean prioritizePlayerChunks;
    private boolean entityOptimizationEnabled;
    private int maxEntitiesPerChunk;
    private int maxItemsPerChunk;
    private int entityCheckIntervalTicks;
    private boolean mergeItemsEnabled;
    private double itemMergeRadius;
    private boolean limitDistantMobAI;
    private int distantMobThreshold;
    private boolean acceleratedItemDespawn;
    private boolean performanceMonitorEnabled;
    private int monitorHistorySize;
    private boolean trackTPS;
    private boolean trackMemory;
    private boolean trackEntities;
    private boolean trackChunks;

    public ChunkConfigManager(OptimizerPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }

    public void loadConfig() {
        if (!this.plugin.getDataFolder().exists()) {
            this.plugin.getDataFolder().mkdirs();
        }
        if (!this.configFile.exists()) {
            this.plugin.saveDefaultConfig();
        }
        this.config = this.plugin.getConfig();
        this.loadGeneralSettings();
        this.loadChunkSettings();
        this.loadEntitySettings();
        this.loadMonitorSettings();
        this.logger.info("Configuration loaded successfully");
    }

    private void loadGeneralSettings() {
        this.debugEnabled = this.config.getBoolean("general.debug", false);
        this.performanceReportsEnabled = this.config.getBoolean("general.performance-reports", true);
        this.monitorInterval = this.config.getInt("general.monitor-interval", 60);
    }

    private void loadChunkSettings() {
        this.chunkManagementEnabled = this.config.getBoolean("chunk.enabled", true);
        this.maxChunksPerTick = this.config.getInt("chunk.max-chunks-per-tick", 10);
        this.unloadAfterSeconds = this.config.getInt("chunk.unload-after-seconds", 30);
        this.maxViewDistance = this.config.getInt("chunk.max-view-distance", -1);
        this.spawnChunkRadius = this.config.getInt("chunk.spawn-chunk-radius", 8);
        this.prioritizePlayerChunks = this.config.getBoolean("chunk.prioritize-player-chunks", true);
    }

    private void loadEntitySettings() {
        this.entityOptimizationEnabled = this.config.getBoolean("entity.enabled", true);
        this.maxEntitiesPerChunk = this.config.getInt("entity.max-entities-per-chunk", 25);
        this.maxItemsPerChunk = this.config.getInt("entity.max-items-per-chunk", 20);
        this.entityCheckIntervalTicks = this.config.getInt("entity.check-interval-ticks", 100);
        this.mergeItemsEnabled = this.config.getBoolean("entity.merge-items", true);
        this.itemMergeRadius = this.config.getDouble("entity.item-merge-radius", 2.5);
        this.limitDistantMobAI = this.config.getBoolean("entity.limit-distant-mob-ai", true);
        this.distantMobThreshold = this.config.getInt("entity.distant-mob-threshold", 48);
        this.acceleratedItemDespawn = this.config.getBoolean("entity.accelerated-item-despawn", true);
    }

    private void loadMonitorSettings() {
        this.performanceMonitorEnabled = this.config.getBoolean("monitor.enabled", true);
        this.monitorHistorySize = this.config.getInt("monitor.history-size", 30);
        this.trackTPS = this.config.getBoolean("monitor.track-tps", true);
        this.trackMemory = this.config.getBoolean("monitor.track-memory", true);
        this.trackEntities = this.config.getBoolean("monitor.track-entities", true);
        this.trackChunks = this.config.getBoolean("monitor.track-chunks", true);
    }

    public void saveConfig() {
        try {
            this.config.save(this.configFile);
        }
        catch (IOException e) {
            this.logger.severe("Failed to save config: " + e.getMessage());
        }
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

    public boolean isChunkManagementEnabled() {
        return this.chunkManagementEnabled;
    }

    public int getMaxChunksPerTick() {
        return this.maxChunksPerTick;
    }

    public int getUnloadAfterSeconds() {
        return this.unloadAfterSeconds;
    }

    public int getMaxViewDistance() {
        return this.maxViewDistance;
    }

    public int getSpawnChunkRadius() {
        return this.spawnChunkRadius;
    }

    public boolean isPrioritizePlayerChunks() {
        return this.prioritizePlayerChunks;
    }

    public boolean isEntityOptimizationEnabled() {
        return this.entityOptimizationEnabled;
    }

    public int getMaxEntitiesPerChunk() {
        return this.maxEntitiesPerChunk;
    }

    public int getMaxItemsPerChunk() {
        return this.maxItemsPerChunk;
    }

    public int getEntityCheckIntervalTicks() {
        return this.entityCheckIntervalTicks;
    }

    public boolean isMergeItemsEnabled() {
        return this.mergeItemsEnabled;
    }

    public double getItemMergeRadius() {
        return this.itemMergeRadius;
    }

    public boolean isLimitDistantMobAI() {
        return this.limitDistantMobAI;
    }

    public int getDistantMobThreshold() {
        return this.distantMobThreshold;
    }

    public boolean isAcceleratedItemDespawn() {
        return this.acceleratedItemDespawn;
    }

    public boolean isPerformanceMonitorEnabled() {
        return this.performanceMonitorEnabled;
    }

    public int getMonitorHistorySize() {
        return this.monitorHistorySize;
    }

    public boolean isTrackTPS() {
        return this.trackTPS;
    }

    public boolean isTrackMemory() {
        return this.trackMemory;
    }

    public boolean isTrackEntities() {
        return this.trackEntities;
    }

    public boolean isTrackChunks() {
        return this.trackChunks;
    }
}

