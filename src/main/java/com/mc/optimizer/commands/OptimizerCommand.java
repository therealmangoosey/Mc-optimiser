/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.World
 *  org.bukkit.command.Command
 *  org.bukkit.command.CommandExecutor
 *  org.bukkit.command.CommandSender
 *  org.bukkit.command.TabCompleter
 *  org.bukkit.entity.Entity
 */
package com.mc.optimizer.commands;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.config.analyzer.ConfigurationAnalyzer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;

public class OptimizerCommand
implements CommandExecutor,
TabCompleter {
    private static final String PERMISSION_USE = "mcoptimizer.use";
    private static final String PERMISSION_RELOAD = "mcoptimizer.reload";
    private static final String PERMISSION_STATUS = "mcoptimizer.status";
    private static final String PERMISSION_CONFIG = "mcoptimizer.config";
    private final OptimizerPlugin plugin;

    public OptimizerCommand(OptimizerPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subCommand;
        if (!sender.hasPermission(PERMISSION_USE)) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to use this command.");
            return true;
        }
        if (args.length == 0) {
            this.showHelp(sender);
            return true;
        }
        switch (subCommand = args[0].toLowerCase()) {
            case "help": {
                this.showHelp(sender);
                break;
            }
            case "reload": {
                if (!sender.hasPermission(PERMISSION_RELOAD)) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to reload the plugin.");
                    return true;
                }
                this.plugin.reload();
                sender.sendMessage(String.valueOf(ChatColor.GREEN) + "MCOptimizer has been reloaded.");
                break;
            }
            case "status": {
                if (!sender.hasPermission(PERMISSION_STATUS)) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to view status.");
                    return true;
                }
                if (args.length > 1 && args[1].equalsIgnoreCase("lag")) {
                    this.showLagPredictionStatus(sender);
                    break;
                }
                this.showStatus(sender);
                break;
            }
            case "config": {
                if (!sender.hasPermission(PERMISSION_CONFIG)) {
                    sender.sendMessage(String.valueOf(ChatColor.RED) + "You don't have permission to analyze server configurations.");
                    return true;
                }
                this.handleConfigCommand(sender, Arrays.copyOfRange(args, 1, args.length));
                break;
            }
            case "version": {
                sender.sendMessage(String.valueOf(ChatColor.GOLD) + "MCOptimizer " + this.plugin.getDescription().getVersion());
                sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Created by " + String.join((CharSequence)", ", this.plugin.getDescription().getAuthors()));
                sender.sendMessage(String.valueOf(ChatColor.AQUA) + "Support: " + String.valueOf(ChatColor.YELLOW) + "https://dcs.gg/fruitsnacks/");
                sender.sendMessage(String.valueOf(ChatColor.AQUA) + "Upcoming Projects: " + String.valueOf(ChatColor.YELLOW) + "https://fruitsnacks.xyz");
                break;
            }
            default: {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Unknown command. Use /mcopt help for a list of commands.");
            }
        }
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(String.valueOf(ChatColor.GOLD) + "===== MCOptimizer Commands =====");
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/mcopt help" + String.valueOf(ChatColor.WHITE) + " - Show this help message");
        if (sender.hasPermission(PERMISSION_RELOAD)) {
            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/mcopt reload" + String.valueOf(ChatColor.WHITE) + " - Reload the plugin configuration");
        }
        if (sender.hasPermission(PERMISSION_STATUS)) {
            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/mcopt status" + String.valueOf(ChatColor.WHITE) + " - Show server optimization status");
            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/mcopt status lag" + String.valueOf(ChatColor.WHITE) + " - Show lag prediction status");
        }
        if (sender.hasPermission(PERMISSION_CONFIG)) {
            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/mcopt config analyze" + String.valueOf(ChatColor.WHITE) + " - Analyze server configuration files");
            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/mcopt config apply" + String.valueOf(ChatColor.WHITE) + " - Apply optimization to configuration files");
        }
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "/mcopt version" + String.valueOf(ChatColor.WHITE) + " - Show plugin version");
        sender.sendMessage(String.valueOf(ChatColor.GOLD) + "==============================");
        sender.sendMessage(String.valueOf(ChatColor.AQUA) + "Support: " + String.valueOf(ChatColor.YELLOW) + "https://dcs.gg/fruitsnacks/");
        sender.sendMessage(String.valueOf(ChatColor.AQUA) + "Upcoming Projects: " + String.valueOf(ChatColor.YELLOW) + "https://fruitsnacks.xyz");
    }

    private void showStatus(CommandSender sender) {
        sender.sendMessage(String.valueOf(ChatColor.GOLD) + "===== MCOptimizer Status =====");
        double tps = 20.0;
        try {
            double[] tpsArray = Bukkit.getServer().getTPS();
            if (tpsArray.length > 0) {
                tps = tpsArray[0];
            }
        }
        catch (Exception e) {
            this.plugin.getLogger().warning("Could not get TPS: " + e.getMessage());
        }
        String tpsColor = tps > 18.0 ? ChatColor.GREEN.toString() : (tps > 15.0 ? ChatColor.YELLOW.toString() : ChatColor.RED.toString());
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "TPS: " + tpsColor + String.format("%.2f", tps));
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        long maxMemory = Runtime.getRuntime().maxMemory();
        double memoryPercentage = (double)usedMemory / (double)maxMemory * 100.0;
        String memoryColor = memoryPercentage < 70.0 ? ChatColor.GREEN.toString() : (memoryPercentage < 85.0 ? ChatColor.YELLOW.toString() : ChatColor.RED.toString());
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Memory: " + memoryColor + String.format("%.1f%%", memoryPercentage) + String.valueOf(ChatColor.YELLOW) + " (" + this.formatSize(usedMemory) + " / " + this.formatSize(maxMemory) + ")");
        HashMap<String, Integer> entityStats = new HashMap<String, Integer>();
        int totalEntities = 0;
        for (World world : Bukkit.getWorlds()) {
            totalEntities += world.getEntities().size();
            for (Entity entity : world.getEntities()) {
                String type = entity.getType().name();
                entityStats.put(type, entityStats.getOrDefault(type, 0) + 1);
            }
        }
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Total Entities: " + String.valueOf(ChatColor.WHITE) + totalEntities);
        if (!entityStats.isEmpty()) {
            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Top entity types:");
            entityStats.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(3L).forEach(entry -> sender.sendMessage(String.valueOf(ChatColor.GRAY) + " - " + (String)entry.getKey() + ": " + String.valueOf(entry.getValue())));
        }
        int loadedChunks = 0;
        for (World world : Bukkit.getWorlds()) {
            loadedChunks += world.getLoadedChunks().length;
        }
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Loaded Chunks: " + String.valueOf(ChatColor.WHITE) + loadedChunks);
        if (this.plugin.getConfig().getBoolean("mob-culling.enabled", true)) {
            Map<String, Object> cullingStats = this.plugin.getMobCullingManager().getStats();
            int totalCulled = (Integer)cullingStats.getOrDefault("totalMobsCulled", 0);
            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Mobs Culled: " + String.valueOf(ChatColor.WHITE) + totalCulled);
        }
        sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Active Features:");
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + " - Chunk Optimization: " + (this.plugin.getConfig().getBoolean("chunk.enabled", true) ? String.valueOf(ChatColor.GREEN) + "Enabled" : String.valueOf(ChatColor.RED) + "Disabled"));
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + " - Entity Optimization: " + (this.plugin.getConfig().getBoolean("entity.enabled", true) ? String.valueOf(ChatColor.GREEN) + "Enabled" : String.valueOf(ChatColor.RED) + "Disabled"));
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + " - Mob Culling: " + (this.plugin.getConfig().getBoolean("mob-culling.enabled", true) ? String.valueOf(ChatColor.GREEN) + "Enabled" : String.valueOf(ChatColor.RED) + "Disabled"));
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + " - Redstone Optimization: " + (this.plugin.getConfig().getBoolean("redstone.enabled", true) ? String.valueOf(ChatColor.GREEN) + "Enabled" : String.valueOf(ChatColor.RED) + "Disabled"));
        sender.sendMessage(String.valueOf(ChatColor.GRAY) + " - Lag Prediction: " + (this.plugin.getConfig().getBoolean("lag-prediction.enabled", true) ? String.valueOf(ChatColor.GREEN) + "Enabled" : String.valueOf(ChatColor.RED) + "Disabled"));
        if (this.plugin.getConfig().getBoolean("lag-prediction.enabled", true) && this.plugin.getLagPredictionManager() != null && this.plugin.getLagPredictionManager().isLagPredicted()) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "\u26a0 Lag Predicted: " + String.valueOf(ChatColor.WHITE) + this.plugin.getLagPredictionManager().getPredictedCause() + String.valueOf(ChatColor.GRAY) + " (" + String.format("%.0f", this.plugin.getLagPredictionManager().getPredictionConfidence() * 100.0) + "% confidence)");
            sender.sendMessage(String.valueOf(ChatColor.RED) + "\u2713 Recommended Action: " + String.valueOf(ChatColor.WHITE) + this.plugin.getLagPredictionManager().getRecommendedAction());
            sender.sendMessage(String.valueOf(ChatColor.GRAY) + "Use '/mcopt status lag' for detailed prediction info");
        }
        sender.sendMessage(String.valueOf(ChatColor.GOLD) + "===============================");
    }

    private void handleConfigCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /mcopt config <analyze|apply>");
            return;
        }
        String subCommand = args[0].toLowerCase();
        ConfigurationAnalyzer analyzer = this.plugin.getConfigAnalyzer();
        switch (subCommand) {
            case "analyze": {
                sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Analyzing server configuration files...");
                analyzer.analyzeConfigurations(sender, false);
                break;
            }
            case "apply": {
                sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "Applying optimization to server configuration files...");
                analyzer.analyzeConfigurations(sender, true);
                break;
            }
            default: {
                sender.sendMessage(String.valueOf(ChatColor.RED) + "Usage: /mcopt config <analyze|apply>");
            }
        }
    }

    private void showLagPredictionStatus(CommandSender sender) {
        if (!this.plugin.getConfig().getBoolean("lag-prediction.enabled", true)) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Lag prediction is disabled in the configuration.");
            return;
        }
        if (this.plugin.getLagPredictionManager() == null) {
            sender.sendMessage(String.valueOf(ChatColor.RED) + "Lag prediction manager is not initialized.");
            return;
        }
        List<String> predictionStatus = this.plugin.getLagPredictionManager().getPredictionStatus(sender);
        for (String line : predictionStatus) {
            sender.sendMessage(line);
        }
        if (this.plugin.getConfig().getBoolean("lag-prediction.web-interface.enabled", false)) {
            int port = this.plugin.getConfig().getInt("lag-prediction.web-interface.port", 8080);
            String bindAddress = this.plugin.getConfig().getString("lag-prediction.web-interface.bind-address", "0.0.0.0");
            String displayAddress = bindAddress.equals("0.0.0.0") ? "localhost" : bindAddress;
            sender.sendMessage(String.valueOf(ChatColor.GOLD) + "Web Interface: " + String.valueOf(ChatColor.GREEN) + "Enabled");
            sender.sendMessage(String.valueOf(ChatColor.GOLD) + "Web URL: " + String.valueOf(ChatColor.WHITE) + "http://" + displayAddress + ":" + port + "/");
        } else {
            sender.sendMessage(String.valueOf(ChatColor.GOLD) + "Web Interface: " + String.valueOf(ChatColor.RED) + "Disabled");
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024L) {
            return bytes + " B";
        }
        if (bytes < 0x100000L) {
            return String.format("%.1f KB", (double)bytes / 1024.0);
        }
        if (bytes < 0x40000000L) {
            return String.format("%.1f MB", (double)bytes / 1048576.0);
        }
        return String.format("%.1f GB", (double)bytes / 1.073741824E9);
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> completions;
        block7: {
            String firstArg;
            block8: {
                block6: {
                    completions = new ArrayList<String>();
                    if (args.length != 1) break block6;
                    ArrayList<String> subCommands = new ArrayList<String>();
                    subCommands.add("help");
                    if (sender.hasPermission(PERMISSION_RELOAD)) {
                        subCommands.add("reload");
                    }
                    if (sender.hasPermission(PERMISSION_STATUS)) {
                        subCommands.add("status");
                    }
                    if (sender.hasPermission(PERMISSION_CONFIG)) {
                        subCommands.add("config");
                    }
                    subCommands.add("version");
                    for (String subCommand : subCommands) {
                        if (!subCommand.startsWith(args[0].toLowerCase())) continue;
                        completions.add(subCommand);
                    }
                    break block7;
                }
                if (args.length != 2) break block7;
                firstArg = args[0].toLowerCase();
                if (!firstArg.equals("config") || !sender.hasPermission(PERMISSION_CONFIG)) break block8;
                List<String> configSubCommands = Arrays.asList("analyze", "apply");
                for (String subCommand : configSubCommands) {
                    if (!subCommand.startsWith(args[1].toLowerCase())) continue;
                    completions.add(subCommand);
                }
                break block7;
            }
            if (!firstArg.equals("status") || !sender.hasPermission(PERMISSION_STATUS)) break block7;
            List<String> statusSubCommands = Arrays.asList("lag");
            for (String subCommand : statusSubCommands) {
                if (!subCommand.startsWith(args[1].toLowerCase())) continue;
                completions.add(subCommand);
            }
        }
        return completions;
    }
}

