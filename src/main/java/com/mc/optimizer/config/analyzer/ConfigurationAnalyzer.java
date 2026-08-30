/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.command.CommandSender
 *  org.yaml.snakeyaml.DumperOptions
 *  org.yaml.snakeyaml.DumperOptions$FlowStyle
 *  org.yaml.snakeyaml.Yaml
 */
package com.mc.optimizer.config.analyzer;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.config.analyzer.BukkitYmlOptimizer;
import com.mc.optimizer.config.analyzer.PaperYmlOptimizer;
import com.mc.optimizer.config.analyzer.ServerPropertiesOptimizer;
import com.mc.optimizer.config.analyzer.SpigotYmlOptimizer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

public class ConfigurationAnalyzer {
    private final OptimizerPlugin plugin;
    private final Logger logger;
    private final List<YamlOptimization> optimizations;

    public ConfigurationAnalyzer(OptimizerPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.optimizations = new ArrayList<YamlOptimization>();
        this.registerOptimizations();
    }

    private void registerOptimizations() {
        // PaperYmlOptimizer is intentionally NOT registered as of the 26.2 update.
        // It was written against the old single-file paper.yml schema (pre-1.19).
        // Paper now splits that into config/paper-global.yml and
        // config/paper-world-defaults.yml with a different, non-overlapping key
        // structure. Faking a clean analysis result would tell server owners
        // their Paper config is optimized when it was never actually checked -
        // worse than skipping it. See PaperYmlOptimizer.java for details on what
        // a real rewrite needs to cover.
        this.optimizations.add(new SpigotYmlOptimizer());
        this.optimizations.add(new BukkitYmlOptimizer());
        this.optimizations.add(new ServerPropertiesOptimizer());
    }

    public void analyzeConfigurations(CommandSender sender, boolean applyChanges) {
        File serverDir = new File(".");
        sender.sendMessage(String.valueOf(ChatColor.GOLD) + "===== MCOptimizer Configuration Analysis =====");
        boolean foundAnyFile = false;
        int totalOptimizations = 0;
        for (YamlOptimization optimizer : this.optimizations) {
            File configFile = new File(serverDir, optimizer.getFileName());
            if (!configFile.exists()) {
                if (!optimizer.isRequired()) continue;
                sender.sendMessage(String.valueOf(ChatColor.YELLOW) + optimizer.getFileName() + ": " + String.valueOf(ChatColor.RED) + "Not found (required)");
                continue;
            }
            foundAnyFile = true;
            try {
                ConfigAnalysisResult result = optimizer.analyzeConfig(configFile);
                if (result.getRecommendations().isEmpty()) {
                    sender.sendMessage(String.valueOf(ChatColor.YELLOW) + optimizer.getFileName() + ": " + String.valueOf(ChatColor.GREEN) + "Optimized (no changes needed)");
                    continue;
                }
                totalOptimizations += result.getRecommendations().size();
                sender.sendMessage(String.valueOf(ChatColor.YELLOW) + optimizer.getFileName() + ": " + String.valueOf(ChatColor.RED) + "Found " + result.getRecommendations().size() + " optimization opportunities");
                if (result.getRecommendations().size() <= 5) {
                    for (ConfigRecommendation rec : result.getRecommendations()) {
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + " - " + rec.getDescription());
                    }
                } else {
                    for (int i = 0; i < 3; ++i) {
                        sender.sendMessage(String.valueOf(ChatColor.GRAY) + " - " + result.getRecommendations().get(i).getDescription());
                    }
                    sender.sendMessage(String.valueOf(ChatColor.GRAY) + " - And " + (result.getRecommendations().size() - 3) + " more...");
                }
                if (!applyChanges) continue;
                File backupFile = new File(configFile.getPath() + ".bak." + System.currentTimeMillis());
                Files.copy(configFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                boolean success = optimizer.applyRecommendations(configFile, result);
                if (success) {
                    sender.sendMessage(String.valueOf(ChatColor.GREEN) + "  \u2713 Applied optimizations to " + optimizer.getFileName() + " (backup created)");
                    continue;
                }
                sender.sendMessage(String.valueOf(ChatColor.RED) + "  \u2717 Failed to apply optimizations to " + optimizer.getFileName());
            }
            catch (Exception e) {
                sender.sendMessage(String.valueOf(ChatColor.YELLOW) + optimizer.getFileName() + ": " + String.valueOf(ChatColor.RED) + "Error analyzing file: " + e.getMessage());
                this.logger.warning("Error analyzing " + optimizer.getFileName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (!foundAnyFile) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "No supported configuration files found in the server directory.");
        } else if (totalOptimizations > 0) {
            if (applyChanges) {
                sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Applied " + totalOptimizations + " optimizations. A server restart is recommended.");
            } else {
                sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Found " + totalOptimizations + " potential optimizations. Use '/mcoptimizer config apply' to apply them.");
            }
        } else {
            sender.sendMessage(String.valueOf(ChatColor.GREEN) + "All configurations are already optimized!");
        }
        sender.sendMessage(String.valueOf(ChatColor.GOLD) + "============================================");
    }

    protected static Map<String, Object> loadYamlFile(File file) throws IOException {
        Yaml yaml = new Yaml();
        try (FileInputStream fis = new FileInputStream(file);){
            Map map = (Map)yaml.load((InputStream)fis);
            return map;
        }
    }

    protected static void saveYamlFile(File file, Map<String, Object> data) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(file);){
            yaml.dump(data, (Writer)writer);
        }
    }

    public static interface YamlOptimization {
        public String getFileName();

        public boolean isRequired();

        public ConfigAnalysisResult analyzeConfig(File var1) throws IOException;

        public boolean applyRecommendations(File var1, ConfigAnalysisResult var2) throws IOException;
    }

    public static class ConfigAnalysisResult {
        private final List<ConfigRecommendation> recommendations = new ArrayList<ConfigRecommendation>();

        public void addRecommendation(ConfigRecommendation recommendation) {
            this.recommendations.add(recommendation);
        }

        public List<ConfigRecommendation> getRecommendations() {
            return this.recommendations;
        }
    }

    public static class ConfigRecommendation {
        private final String path;
        private final Object currentValue;
        private final Object recommendedValue;
        private final String description;

        public ConfigRecommendation(String path, Object currentValue, Object recommendedValue, String description) {
            this.path = path;
            this.currentValue = currentValue;
            this.recommendedValue = recommendedValue;
            this.description = description;
        }

        public String getPath() {
            return this.path;
        }

        public Object getCurrentValue() {
            return this.currentValue;
        }

        public Object getRecommendedValue() {
            return this.recommendedValue;
        }

        public String getDescription() {
            return this.description;
        }
    }
}

