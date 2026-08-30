/*
 * Decompiled with CFR 0.152.
 */
package com.mc.optimizer.config.analyzer;

import com.mc.optimizer.config.analyzer.ConfigurationAnalyzer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ServerPropertiesOptimizer
implements ConfigurationAnalyzer.YamlOptimization {
    @Override
    public String getFileName() {
        return "server.properties";
    }

    @Override
    public boolean isRequired() {
        return true;
    }

    @Override
    public ConfigurationAnalyzer.ConfigAnalysisResult analyzeConfig(File configFile) throws IOException {
        ConfigurationAnalyzer.ConfigAnalysisResult result = new ConfigurationAnalyzer.ConfigAnalysisResult();
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile);){
            properties.load(fis);
        }
        String viewDistanceStr = properties.getProperty("view-distance", "10");
        try {
            int viewDistance = Integer.parseInt(viewDistanceStr);
            if (viewDistance > 8) {
                result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("view-distance", viewDistance, 8, "Reduce view distance to 8 for better server performance"));
            }
        }
        catch (NumberFormatException viewDistance) {
            // empty catch block
        }
        String simulationDistanceStr = properties.getProperty("simulation-distance", "10");
        try {
            int simulationDistance = Integer.parseInt(simulationDistanceStr);
            if (simulationDistance > 6) {
                result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("simulation-distance", simulationDistance, 6, "Reduce simulation distance to 6 for better entity processing performance"));
            }
        }
        catch (NumberFormatException simulationDistance) {
            // empty catch block
        }
        String entityRangeStr = properties.getProperty("entity-broadcast-range-percentage", "100");
        try {
            int entityRange = Integer.parseInt(entityRangeStr);
            if (entityRange > 65) {
                result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("entity-broadcast-range-percentage", entityRange, 65, "Reduce entity broadcast range to 65% for better network performance"));
            }
        }
        catch (NumberFormatException entityRange) {
            // empty catch block
        }
        String maxTickTimeStr = properties.getProperty("max-tick-time", "60000");
        try {
            long maxTickTime = Long.parseLong(maxTickTimeStr);
            if (maxTickTime != -1L && maxTickTime < 120000L) {
                result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("max-tick-time", maxTickTime, -1, "Set max-tick-time to -1 to prevent the server from shutting down during lag spikes"));
            }
        }
        catch (NumberFormatException maxTickTime) {
            // empty catch block
        }
        String networkCompressionStr = properties.getProperty("network-compression-threshold", "256");
        try {
            int networkCompression = Integer.parseInt(networkCompressionStr);
            if (networkCompression != -1 && networkCompression < 512) {
                result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("network-compression-threshold", networkCompression, 512, "Increase network compression threshold to 512 to reduce CPU usage for small packets"));
            }
        }
        catch (NumberFormatException networkCompression) {
            // empty catch block
        }
        String rateLimitStr = properties.getProperty("rate-limit", "0");
        try {
            int rateLimit = Integer.parseInt(rateLimitStr);
            if (rateLimit < 1000) {
                result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("rate-limit", rateLimit, 1000, "Set rate limit to 1000 to prevent malicious packet spam while not affecting normal players"));
            }
        }
        catch (NumberFormatException rateLimit) {
            // empty catch block
        }
        String spawnProtectionStr = properties.getProperty("spawn-protection", "16");
        try {
            int spawnProtection = Integer.parseInt(spawnProtectionStr);
            if (spawnProtection > 16) {
                result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("spawn-protection", spawnProtection, 16, "Reduce spawn protection radius to 16 blocks to minimize permission checks"));
            }
        }
        catch (NumberFormatException spawnProtection) {
            // empty catch block
        }
        String allowFlightStr = properties.getProperty("allow-flight", "false");
        boolean allowFlight = Boolean.parseBoolean(allowFlightStr);
        if (!allowFlight) {
            result.addRecommendation(new ConfigurationAnalyzer.ConfigRecommendation("allow-flight", allowFlight, true, "Enable allow-flight for compatibility with optimization plugins and to prevent false positives in anti-cheat"));
        }
        return result;
    }

    @Override
    public boolean applyRecommendations(File configFile, ConfigurationAnalyzer.ConfigAnalysisResult result) throws IOException {
        try {
            Properties properties = new Properties();
            try (FileInputStream fis = new FileInputStream(configFile);){
                properties.load(fis);
            }
            for (ConfigurationAnalyzer.ConfigRecommendation recommendation : result.getRecommendations()) {
                String key = recommendation.getPath();
                Object value = recommendation.getRecommendedValue();
                String strValue = String.valueOf(value);
                properties.setProperty(key, strValue);
            }
            try (FileOutputStream fos = new FileOutputStream(configFile);){
                properties.store(fos, "Optimized by MCOptimizer");
            }
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

