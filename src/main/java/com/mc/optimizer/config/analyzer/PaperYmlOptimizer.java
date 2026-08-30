/*
 * Decompiled with CFR 0.152.
 */
package com.mc.optimizer.config.analyzer;

import com.mc.optimizer.config.analyzer.ConfigurationAnalyzer;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * NOT CURRENTLY REGISTERED - see ConfigurationAnalyzer.registerOptimizations().
 *
 * This entire class is written against the old single-file paper.yml schema
 * (pre-Paper-1.19). Every key path below (world-settings.default.*,
 * settings.velocity-support.*, settings.async-chunks.*) belongs to that old
 * format. Paper now uses two files with a different, non-overlapping
 * structure: config/paper-global.yml and config/paper-world-defaults.yml.
 *
 * A real fix means rewriting this against the current schema
 * (docs.papermc.io/paper/reference/global-configuration and
 * .../world-configuration), most likely splitting it into two analyzers,
 * one per file, and re-mapping each setting to wherever it landed (some may
 * have been renamed, merged, or removed since this was written). That's real
 * work that needs a live 26.2 server to verify against, which wasn't
 * available when this patch was done, so it's left disabled rather than
 * guessing at key paths that could silently write nonsense into a real
 * server's config.
 */
public class PaperYmlOptimizer
implements ConfigurationAnalyzer.YamlOptimization {
    @Override
    public String getFileName() {
        return "paper.yml";
    }

    @Override
    public boolean isRequired() {
        return true;
    }

    @Override
    public ConfigurationAnalyzer.ConfigAnalysisResult analyzeConfig(File configFile) throws IOException {
        Map asyncChunks;
        Boolean enableAsyncChunks;
        Object lagCompensateBlockBreaking;
        Boolean announceBehindProxy;
        Map globalSettings;
        Boolean velocitySupport;
        Boolean elytraHitDetection;
        Boolean enableTreasureMaps;
        Integer viewDistance;
        Object maxEntityCollisions;
        Integer grassSpread;
        Map tickRates;
        Integer containerUpdate;
        Integer ticksPerSpawns;
        Integer mobSpawnRange;
        Map entities;
        Boolean optimizeExplosions;
        Integer autoSaveInterval;
        Boolean preventMovingIntoUnloadedChunks;
        Integer chunkTicketExpiry;
        Map worldSettings;
        Map defaultSettings;
        Map chunks;
        Integer maxChunkSendRate;
        ConfigurationAnalyzer.ConfigAnalysisResult result = new ConfigurationAnalyzer.ConfigAnalysisResult();
        Map<String, Object> data = ConfigurationAnalyzer.loadYamlFile(configFile);
        if (data == null) {
            data = new HashMap<String, Object>();
        }
        if ((maxChunkSendRate = (Integer)this.getNestedValue(chunks = (Map)(defaultSettings = (Map)(worldSettings = (Map)data.getOrDefault("world-settings", new HashMap())).getOrDefault("default", new HashMap())).getOrDefault("chunks", new HashMap()), "max-chunk-send-rate")) == null || maxChunkSendRate < 100) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.chunks.max-chunk-send-rate", maxChunkSendRate, 100, "Increase max-chunk-send-rate to 100 for faster chunk loading"));
        }
        if ((chunkTicketExpiry = (Integer)this.getNestedValue(chunks, "chunk-priority-ticket-expiry-ticks")) == null || chunkTicketExpiry > 10) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.chunks.chunk-priority-ticket-expiry-ticks", chunkTicketExpiry, 10, "Lower chunk-priority-ticket-expiry-ticks to 10 to reduce unnecessary chunk loads"));
        }
        if ((preventMovingIntoUnloadedChunks = (Boolean)this.getNestedValue(chunks, "prevent-moving-into-unloaded-chunks")) == null || !preventMovingIntoUnloadedChunks.booleanValue()) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.chunks.prevent-moving-into-unloaded-chunks", preventMovingIntoUnloadedChunks, true, "Enable prevent-moving-into-unloaded-chunks to avoid chunk loading lag spikes"));
        }
        if ((autoSaveInterval = (Integer)this.getNestedValue(chunks, "auto-save-interval")) != null && autoSaveInterval < 30) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.chunks.auto-save-interval", autoSaveInterval, 30, "Increase auto-save-interval to 30 seconds to reduce save-related lag spikes"));
        }
        if ((optimizeExplosions = (Boolean)this.getNestedValue(entities = (Map)defaultSettings.getOrDefault("entities", new HashMap()), "optimize-explosions")) == null || !optimizeExplosions.booleanValue()) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.entities.optimize-explosions", optimizeExplosions, true, "Enable optimize-explosions to reduce lag from explosion calculations"));
        }
        if ((mobSpawnRange = (Integer)this.getNestedValue(entities, "mob-spawn-range")) == null || mobSpawnRange > 4) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.entities.mob-spawn-range", mobSpawnRange, 4, "Reduce mob-spawn-range to 4 to decrease entity load on the server"));
        }
        if ((ticksPerSpawns = (Integer)this.getNestedValue(entities, "ticks-per-spawns.monster")) == null || ticksPerSpawns < 4) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.entities.ticks-per-spawns.monster", ticksPerSpawns, 4, "Increase monster spawn interval to 4 ticks to reduce spawn calculation frequency"));
        }
        if ((containerUpdate = (Integer)this.getNestedValue(tickRates = (Map)defaultSettings.getOrDefault("tick-rates", new HashMap()), "container-update")) == null || containerUpdate < 3) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.tick-rates.container-update", containerUpdate, 3, "Increase container-update rate to 3 ticks for better performance without noticeable effects"));
        }
        if ((grassSpread = (Integer)this.getNestedValue(tickRates, "grass-spread")) == null || grassSpread < 4) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.tick-rates.grass-spread", grassSpread, 4, "Increase grass-spread rate to 4 ticks to reduce calculation overhead"));
        }
        if ((maxEntityCollisions = this.getNestedValue(defaultSettings, "max-entity-collisions")) == null || maxEntityCollisions instanceof Number && ((Number)maxEntityCollisions).intValue() > 4) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.max-entity-collisions", maxEntityCollisions, 4, "Limit max-entity-collisions to 4 to reduce collision calculations"));
        }
        if ((viewDistance = (Integer)this.getNestedValue(defaultSettings, "view-distance")) != null && viewDistance > 8) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.view-distance", viewDistance, 8, "Reduce view-distance to 8 to improve server performance without significant visual impact"));
        }
        if ((enableTreasureMaps = (Boolean)this.getNestedValue(defaultSettings, "game-mechanics.disable-treasure-maps")) == null || !enableTreasureMaps.booleanValue()) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.game-mechanics.disable-treasure-maps", enableTreasureMaps, true, "Disable treasure maps to avoid expensive chunk generation"));
        }
        if ((elytraHitDetection = (Boolean)this.getNestedValue(defaultSettings, "game-mechanics.disable-elytra-hit-detection")) == null || !elytraHitDetection.booleanValue()) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.game-mechanics.disable-elytra-hit-detection", elytraHitDetection, true, "Disable elytra hit detection for better performance when many players are flying"));
        }
        if ((velocitySupport = (Boolean)this.getNestedValue(globalSettings = (Map)data.getOrDefault("settings", new HashMap()), "velocity-support.enabled")) != null && velocitySupport.booleanValue() && ((announceBehindProxy = (Boolean)this.getNestedValue(globalSettings, "velocity-support.announce-player-achievements")) == null || announceBehindProxy.booleanValue())) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("settings.velocity-support.announce-player-achievements", announceBehindProxy, false, "Disable announcement of achievements behind proxy to improve network performance"));
        }
        if ((lagCompensateBlockBreaking = this.getNestedValue(globalSettings, "lag-compensate-block-breaking")) == null || !(lagCompensateBlockBreaking instanceof Boolean) || !((Boolean)lagCompensateBlockBreaking).booleanValue()) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("settings.lag-compensate-block-breaking", lagCompensateBlockBreaking, true, "Enable lag compensation for block breaking to improve player experience"));
        }
        if ((enableAsyncChunks = (Boolean)this.getNestedValue(asyncChunks = (Map)globalSettings.getOrDefault("async-chunks", new HashMap()), "enable")) == null || !enableAsyncChunks.booleanValue()) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("settings.async-chunks.enable", enableAsyncChunks, true, "Enable async chunk loading/generation for better performance"));
        }
        return result;
    }

    @Override
    public boolean applyRecommendations(File configFile, ConfigurationAnalyzer.ConfigAnalysisResult result) throws IOException {
        try {
            Map data = ConfigurationAnalyzer.loadYamlFile(configFile);
            if (data == null) {
                data = new HashMap<String, Object>();
            }
            for (ConfigurationAnalyzer.ConfigRecommendation recommendation : result.getRecommendations()) {
                String[] pathComponents = recommendation.getPath().split("\\.");
                Map current = data;
                for (int i = 0; i < pathComponents.length - 1; ++i) {
                    String component = pathComponents[i];
                    if (!current.containsKey(component) || !(current.get(component) instanceof Map)) {
                        current.put((String)component, new HashMap());
                    }
                    current = (Map)current.get(component);
                }
                current.put((String)pathComponents[pathComponents.length - 1], (Object)recommendation.getRecommendedValue());
            }
            ConfigurationAnalyzer.saveYamlFile(configFile, data);
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private Object getNestedValue(Map<String, Object> map, String path) {
        if (map == null) {
            return null;
        }
        String[] parts = path.split("\\.");
        Map current = map;
        for (int i = 0; i < parts.length - 1; ++i) {
            Map nestedMap;
            Object value = current.get(parts[i]);
            if (!(value instanceof Map)) {
                return null;
            }
            current = nestedMap = (Map)value;
        }
        return current.get(parts[parts.length - 1]);
    }
}

