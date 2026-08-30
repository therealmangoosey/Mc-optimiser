/*
 * Decompiled with CFR 0.152.
 */
package com.mc.optimizer.config.analyzer;

import com.mc.optimizer.config.analyzer.ConfigurationAnalyzer;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class SpigotYmlOptimizer
implements ConfigurationAnalyzer.YamlOptimization {
    @Override
    public String getFileName() {
        return "spigot.yml";
    }

    @Override
    public boolean isRequired() {
        return true;
    }

    @Override
    public ConfigurationAnalyzer.ConfigAnalysisResult analyzeConfig(File configFile) throws IOException {
        Integer nettyThreads;
        Map settings;
        Boolean savePlayerData;
        Integer hopperCheck;
        Integer hopperTransfer;
        Double experienceMergeRadius;
        Double itemMergeRadius;
        Integer mobSpawnRange;
        Integer tridentDespawnRate;
        Integer arrowDespawnRate;
        Integer ticksPerWaterUndergroundCreatureSpawns;
        Integer ticksPerWaterAmbientSpawns;
        Integer ticksPerAmbientSpawns;
        Integer ticksPerWaterSpawns;
        Integer ticksPerMonsterSpawns;
        Integer ticksPerAnimalSpawns;
        Integer miscTrackingRange;
        Integer monsterTrackingRange;
        Integer animalTrackingRange;
        Map trackingRange;
        Integer playerTrackingRange;
        Integer flyingMonsterRange;
        Integer villagerRange;
        Integer waterRange;
        Integer miscRange;
        Integer raiderRange;
        Integer monstersRange;
        Map worldSettings;
        Map defaultSettings;
        Map activationRange;
        Integer animalsRange;
        ConfigurationAnalyzer.ConfigAnalysisResult result = new ConfigurationAnalyzer.ConfigAnalysisResult();
        Map<String, Object> data = ConfigurationAnalyzer.loadYamlFile(configFile);
        if (data == null) {
            data = new HashMap<String, Object>();
        }
        if ((animalsRange = (Integer)((activationRange = (Map)(defaultSettings = (Map)(worldSettings = (Map)data.getOrDefault("world-settings", new HashMap())).getOrDefault("default", new HashMap())).getOrDefault("entity-activation-range", new HashMap())).getOrDefault("animals", 32))) > 16) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.entity-activation-range.animals", animalsRange, 16, "Reduce animal activation range to 16 blocks to decrease entity processing"));
        }
        if ((monstersRange = (Integer)activationRange.getOrDefault("monsters", 32)) > 24) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.entity-activation-range.monsters", monstersRange, 24, "Reduce monster activation range to 24 blocks for better performance"));
        }
        if ((raiderRange = (Integer)activationRange.getOrDefault("raiders", 48)) > 48) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.entity-activation-range.raiders", raiderRange, 48, "Set raider activation range to 48 blocks"));
        }
        if ((miscRange = (Integer)activationRange.getOrDefault("misc", 16)) > 8) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.entity-activation-range.misc", miscRange, 8, "Reduce misc entity activation range to 8 blocks"));
        }
        if ((waterRange = (Integer)activationRange.getOrDefault("water", 16)) > 8) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.entity-activation-range.water", waterRange, 8, "Reduce water entity activation range to 8 blocks"));
        }
        if ((villagerRange = (Integer)activationRange.getOrDefault("villagers", 32)) > 16) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.entity-activation-range.villagers", villagerRange, 16, "Reduce villager activation range to 16 blocks"));
        }
        if ((flyingMonsterRange = (Integer)activationRange.getOrDefault("flying-monsters", 32)) > 32) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.entity-activation-range.flying-monsters", flyingMonsterRange, 32, "Set flying monster activation range to 32 blocks"));
        }
        if ((playerTrackingRange = (Integer)(trackingRange = (Map)defaultSettings.getOrDefault("entity-tracking-range", new HashMap())).getOrDefault("players", 48)) > 48) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.entity-tracking-range.players", playerTrackingRange, 48, "Set player tracking range to 48 blocks (vanilla default)"));
        }
        if ((animalTrackingRange = (Integer)trackingRange.getOrDefault("animals", 48)) > 32) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.entity-tracking-range.animals", animalTrackingRange, 32, "Reduce animal tracking range to 32 blocks"));
        }
        if ((monsterTrackingRange = (Integer)trackingRange.getOrDefault("monsters", 48)) > 48) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.entity-tracking-range.monsters", monsterTrackingRange, 48, "Set monster tracking range to 48 blocks"));
        }
        if ((miscTrackingRange = (Integer)trackingRange.getOrDefault("misc", 32)) > 32) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.entity-tracking-range.misc", miscTrackingRange, 32, "Set misc entity tracking range to 32 blocks"));
        }
        if ((ticksPerAnimalSpawns = (Integer)defaultSettings.getOrDefault("ticks-per.animal-spawns", 400)) < 400) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.ticks-per.animal-spawns", ticksPerAnimalSpawns, 400, "Increase ticks between animal spawns to 400 for better performance"));
        }
        if ((ticksPerMonsterSpawns = (Integer)defaultSettings.getOrDefault("ticks-per.monster-spawns", 1)) < 2) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.ticks-per.monster-spawns", ticksPerMonsterSpawns, 2, "Increase ticks between monster spawns to 2 for better performance"));
        }
        if ((ticksPerWaterSpawns = (Integer)defaultSettings.getOrDefault("ticks-per.water-spawns", 400)) < 400) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.ticks-per.water-spawns", ticksPerWaterSpawns, 400, "Increase ticks between water entity spawns to 400"));
        }
        if ((ticksPerAmbientSpawns = (Integer)defaultSettings.getOrDefault("ticks-per.ambient-spawns", 400)) < 400) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.ticks-per.ambient-spawns", ticksPerAmbientSpawns, 400, "Increase ticks between ambient entity spawns to 400"));
        }
        if ((ticksPerWaterAmbientSpawns = (Integer)defaultSettings.getOrDefault("ticks-per.water-ambient-spawns", 400)) < 400) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.ticks-per.water-ambient-spawns", ticksPerWaterAmbientSpawns, 400, "Increase ticks between water ambient entity spawns to 400"));
        }
        if ((ticksPerWaterUndergroundCreatureSpawns = (Integer)defaultSettings.getOrDefault("ticks-per.water-underground-creature-spawns", 400)) < 400) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.ticks-per.water-underground-creature-spawns", ticksPerWaterUndergroundCreatureSpawns, 400, "Increase ticks between water underground creature spawns to 400"));
        }
        if ((arrowDespawnRate = (Integer)defaultSettings.getOrDefault("arrow-despawn-rate", 1200)) instanceof Integer && arrowDespawnRate > 300) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.arrow-despawn-rate", arrowDespawnRate, 300, "Reduce arrow despawn rate to 300 ticks to clean up entities faster"));
        }
        if ((tridentDespawnRate = (Integer)defaultSettings.getOrDefault("trident-despawn-rate", 1200)) instanceof Integer && tridentDespawnRate > 300) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.trident-despawn-rate", tridentDespawnRate, 300, "Reduce trident despawn rate to 300 ticks to clean up entities faster"));
        }
        if ((mobSpawnRange = (Integer)defaultSettings.getOrDefault("mob-spawn-range", 6)) instanceof Integer && mobSpawnRange > 4) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.mob-spawn-range", mobSpawnRange, 4, "Reduce mob spawn range to 4 blocks to decrease entity count"));
        }
        if ((itemMergeRadius = (Double)defaultSettings.getOrDefault("merge-radius.item", 2.5)) instanceof Double && itemMergeRadius < 4.0) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.merge-radius.item", itemMergeRadius, 4.0, "Increase item merge radius to 4.0 blocks to reduce entity count"));
        }
        if ((experienceMergeRadius = (Double)defaultSettings.getOrDefault("merge-radius.exp", 3.0)) instanceof Double && experienceMergeRadius < 6.0) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.merge-radius.exp", experienceMergeRadius, 6.0, "Increase experience orb merge radius to 6.0 blocks to reduce entity count"));
        }
        if ((hopperTransfer = (Integer)defaultSettings.getOrDefault("hopper-transfer", 8)) < 16) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.hopper-transfer", hopperTransfer, 16, "Increase hopper transfer rate to 16 ticks to reduce hopper load"));
        }
        if ((hopperCheck = (Integer)defaultSettings.getOrDefault("hopper-check", 1)) < 8) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("world-settings.default.hopper-check", hopperCheck, 8, "Increase hopper check rate to 8 ticks to reduce hopper load"));
        }
        if ((savePlayerData = (Boolean)(settings = (Map)data.getOrDefault("settings", new HashMap())).getOrDefault("save-player-data", true)) == null || !savePlayerData.booleanValue()) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("settings.save-player-data", savePlayerData, true, "Enable save-player-data to prevent data loss"));
        }
        if ((nettyThreads = (Integer)settings.getOrDefault("netty-threads", 4)) < 4) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("settings.netty-threads", nettyThreads, 4, "Set netty-threads to at least 4 for better network performance"));
        }
        Boolean bungeeCord = (Boolean)settings.getOrDefault("bungeecord", false);
        // This used to cross-check settings.velocity-support.enabled in the old
        // single-file paper.yml. That file and schema no longer exist (see
        // ConfigurationAnalyzer.registerOptimizations for why PaperYmlOptimizer
        // is disabled). velocitySupport is left false rather than guessing at
        // the current config/paper-global.yml key path without a way to verify it.
        Boolean velocitySupport = false;
        if (velocitySupport.booleanValue() && !bungeeCord.booleanValue()) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("settings.bungeecord", bungeeCord, true, "Enable bungeecord since Velocity support is enabled in paper.yml"));
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
}

