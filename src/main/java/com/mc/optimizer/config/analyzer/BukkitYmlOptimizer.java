/*
 * Decompiled with CFR 0.152.
 */
package com.mc.optimizer.config.analyzer;

import com.mc.optimizer.config.analyzer.ConfigurationAnalyzer;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BukkitYmlOptimizer
implements ConfigurationAnalyzer.YamlOptimization {
    @Override
    public String getFileName() {
        return "bukkit.yml";
    }

    @Override
    public boolean isRequired() {
        return true;
    }

    @Override
    public ConfigurationAnalyzer.ConfigAnalysisResult analyzeConfig(File configFile) throws IOException {
        Integer samplesPerTick;
        Boolean monsterSpawnLimitBypassAuth;
        Map settings;
        Boolean useJmxMonitoring;
        Integer ambientSpawns;
        Integer waterUndergroundCreatureSpawns;
        Integer waterAmbientSpawns;
        Integer waterSpawns;
        Integer animalSpawns;
        Map ticksPerSettings;
        Integer monsterSpawns;
        Integer chunkGcPeriod;
        Integer ambient;
        Integer waterUndergroundCreature;
        Integer waterAmbient;
        Integer waterAnimals;
        Integer animals;
        Map spawnLimits;
        Integer monsters;
        ConfigurationAnalyzer.ConfigAnalysisResult result = new ConfigurationAnalyzer.ConfigAnalysisResult();
        Map<String, Object> data = ConfigurationAnalyzer.loadYamlFile(configFile);
        if (data == null) {
            data = new HashMap<String, Object>();
        }
        if ((monsters = (Integer)(spawnLimits = (Map)data.getOrDefault("spawn-limits", new HashMap())).getOrDefault("monsters", 70)) > 50) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("spawn-limits.monsters", monsters, 50, "Reduce monster spawn limit to 50 to decrease entity load"));
        }
        if ((animals = (Integer)spawnLimits.getOrDefault("animals", 10)) > 10) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("spawn-limits.animals", animals, 10, "Set animal spawn limit to 10 to reduce passive mob load"));
        }
        if ((waterAnimals = (Integer)spawnLimits.getOrDefault("water-animals", 15)) > 7) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("spawn-limits.water-animals", waterAnimals, 7, "Reduce water animal spawn limit to 7 to decrease underwater entity load"));
        }
        if ((waterAmbient = (Integer)spawnLimits.getOrDefault("water-ambient", 20)) > 10) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("spawn-limits.water-ambient", waterAmbient, 10, "Reduce water ambient spawn limit to 10 to decrease underwater entity load"));
        }
        if ((waterUndergroundCreature = (Integer)spawnLimits.getOrDefault("water-underground-creature", 5)) > 5) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("spawn-limits.water-underground-creature", waterUndergroundCreature, 5, "Set water underground creature spawn limit to 5 to reduce underground entity load"));
        }
        if ((ambient = (Integer)spawnLimits.getOrDefault("ambient", 15)) > 5) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("spawn-limits.ambient", ambient, 5, "Reduce ambient spawn limit to 5 to decrease ambient entity load"));
        }
        if ((chunkGcPeriod = (Integer)data.getOrDefault("chunk-gc.period-in-ticks", 600)) > 400) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("chunk-gc.period-in-ticks", chunkGcPeriod, 400, "Reduce chunk garbage collection period to 400 ticks for more frequent cleanup"));
        }
        if ((monsterSpawns = (Integer)(ticksPerSettings = (Map)data.getOrDefault("ticks-per", new HashMap())).getOrDefault("monster-spawns", 1)) < 4) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("ticks-per.monster-spawns", monsterSpawns, 4, "Increase monster spawn interval to 4 ticks to reduce spawn calculation frequency"));
        }
        if ((animalSpawns = (Integer)ticksPerSettings.getOrDefault("animal-spawns", 400)) < 400) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("ticks-per.animal-spawns", animalSpawns, 400, "Set animal spawn interval to 400 ticks for better performance"));
        }
        if ((waterSpawns = (Integer)ticksPerSettings.getOrDefault("water-spawns", 1)) < 400) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("ticks-per.water-spawns", waterSpawns, 400, "Increase water mob spawn interval to 400 ticks for better performance"));
        }
        if ((waterAmbientSpawns = (Integer)ticksPerSettings.getOrDefault("water-ambient-spawns", 1)) < 400) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("ticks-per.water-ambient-spawns", waterAmbientSpawns, 400, "Increase water ambient spawn interval to 400 ticks for better performance"));
        }
        if ((waterUndergroundCreatureSpawns = (Integer)ticksPerSettings.getOrDefault("water-underground-creature-spawns", 1)) < 400) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("ticks-per.water-underground-creature-spawns", waterUndergroundCreatureSpawns, 400, "Increase water underground creature spawn interval to 400 ticks"));
        }
        if ((ambientSpawns = (Integer)ticksPerSettings.getOrDefault("ambient-spawns", 1)) < 400) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("ticks-per.ambient-spawns", ambientSpawns, 400, "Increase ambient spawn interval to 400 ticks for better performance"));
        }
        if ((useJmxMonitoring = (Boolean)(settings = (Map)data.getOrDefault("settings", new HashMap())).getOrDefault("use-jmx-monitoring", false)) != null && useJmxMonitoring.booleanValue()) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("settings.use-jmx-monitoring", useJmxMonitoring, false, "Disable JMX monitoring to reduce overhead if not actively used"));
        }
        if ((monsterSpawnLimitBypassAuth = (Boolean)settings.getOrDefault("monsters-spawner-bypass-mob-cap", false)) != null && monsterSpawnLimitBypassAuth.booleanValue()) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("settings.monsters-spawner-bypass-mob-cap", monsterSpawnLimitBypassAuth, false, "Disable spawner bypass of mob cap to maintain entity limits"));
        }
        if ((samplesPerTick = (Integer)settings.getOrDefault("sample-count", 12)) != null && samplesPerTick > 12) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("settings.sample-count", samplesPerTick, 12, "Reduce TPS sample count to 12 for more accurate, less resource-intensive sampling"));
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

