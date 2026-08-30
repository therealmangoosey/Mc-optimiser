package com.mc.optimizer.commands;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.config.analyzer.ConfigurationAnalyzer;
import com.mc.optimizer.metrics.PerformanceMonitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

/** Command handler with cheap status reporting and null-safe optional features. */
public final class OptimizerCommand implements CommandExecutor, TabCompleter {
    private static final String USE = "mcoptimizer.use";
    private static final String RELOAD = "mcoptimizer.reload";
    private static final String STATUS = "mcoptimizer.status";
    private static final String CONFIG = "mcoptimizer.config";
    private final OptimizerPlugin plugin;

    public OptimizerCommand(OptimizerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission(USE)) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
            return true;
        }
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "help" -> showHelp(sender);
            case "reload" -> {
                if (!sender.hasPermission(RELOAD)) return deny(sender, "reload");
                plugin.reload();
                sender.sendMessage(ChatColor.GREEN + "MCOptimizer has been reloaded.");
            }
            case "status" -> {
                if (!sender.hasPermission(STATUS)) return deny(sender, "view status");
                if (args.length > 1 && args[1].equalsIgnoreCase("lag")) showLagPredictionStatus(sender);
                else showStatus(sender);
            }
            case "config" -> {
                if (!sender.hasPermission(CONFIG)) return deny(sender, "analyze server configurations");
                handleConfigCommand(sender, Arrays.copyOfRange(args, 1, args.length));
            }
            case "version" -> {
                sender.sendMessage(ChatColor.GOLD + "MCOptimizer " + plugin.getDescription().getVersion());
                sender.sendMessage(ChatColor.GRAY + "Created by " + String.join(", ", plugin.getDescription().getAuthors()));
            }
            default -> sender.sendMessage(ChatColor.RED + "Unknown command. Use /mcopt help.");
        }
        return true;
    }

    private boolean deny(CommandSender sender, String action) {
        sender.sendMessage(ChatColor.RED + "You don't have permission to " + action + ".");
        return true;
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== MCOptimizer =====");
        sender.sendMessage(ChatColor.YELLOW + "/mcopt help" + ChatColor.WHITE + " - Show help");
        if (sender.hasPermission(RELOAD)) sender.sendMessage(ChatColor.YELLOW + "/mcopt reload" + ChatColor.WHITE + " - Reload configuration");
        if (sender.hasPermission(STATUS)) {
            sender.sendMessage(ChatColor.YELLOW + "/mcopt status" + ChatColor.WHITE + " - Show status");
            sender.sendMessage(ChatColor.YELLOW + "/mcopt status lag" + ChatColor.WHITE + " - Show lag prediction status");
        }
        if (sender.hasPermission(CONFIG)) {
            sender.sendMessage(ChatColor.YELLOW + "/mcopt config analyze" + ChatColor.WHITE + " - Analyze configs");
            sender.sendMessage(ChatColor.YELLOW + "/mcopt config apply" + ChatColor.WHITE + " - Apply config optimization");
        }
        sender.sendMessage(ChatColor.YELLOW + "/mcopt version" + ChatColor.WHITE + " - Show version");
    }

    private void showStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "===== MCOptimizer Status =====");
        PerformanceMonitor monitor = plugin.getPerformanceMonitor();
        PerformanceMonitor.PerformanceSnapshot snapshot = monitor == null ? null : monitor.getLatestSnapshot();
        if (snapshot == null) {
            sender.sendMessage(ChatColor.YELLOW + "Metrics: " + ChatColor.GRAY + "not sampled yet");
        } else {
            double tps = snapshot.tps;
            ChatColor tpsColor = tps >= 18.0 ? ChatColor.GREEN : tps >= 15.0 ? ChatColor.YELLOW : ChatColor.RED;
            sender.sendMessage(ChatColor.YELLOW + "TPS: " + tpsColor + String.format("%.2f", tps));
            if (snapshot.maxMemory > 0) {
                double memory = snapshot.usedMemory * 100.0 / snapshot.maxMemory;
                ChatColor memoryColor = memory < 70 ? ChatColor.GREEN : memory < 85 ? ChatColor.YELLOW : ChatColor.RED;
                sender.sendMessage(ChatColor.YELLOW + "Memory: " + memoryColor + String.format("%.1f%%", memory)
                        + ChatColor.YELLOW + " (" + snapshot.usedMemory + " MB / " + snapshot.maxMemory + " MB)");
            }
            if (snapshot.totalEntities > 0 || snapshot.loadedChunks > 0) {
                sender.sendMessage(ChatColor.YELLOW + "Entities: " + ChatColor.WHITE + snapshot.totalEntities);
                sender.sendMessage(ChatColor.YELLOW + "Loaded Chunks: " + ChatColor.WHITE + snapshot.loadedChunks);
            }
        }
        sender.sendMessage(ChatColor.YELLOW + "Active Features:");
        reportFeature(sender, "Chunk Optimization", "chunk.enabled");
        reportFeature(sender, "Entity Optimization", "entity.enabled");
        reportFeature(sender, "Mob Culling", "mob-culling.enabled");
        reportFeature(sender, "Redstone Optimization", "redstone.enabled");
        reportFeature(sender, "Lag Prediction", "lag-prediction.enabled");
        sender.sendMessage(ChatColor.GOLD + "===============================");
    }

    private void reportFeature(CommandSender sender, String name, String path) {
        boolean enabled = plugin.getConfig().getBoolean(path, false);
        sender.sendMessage(ChatColor.GRAY + " - " + name + ": " + (enabled ? ChatColor.GREEN + "Enabled" : ChatColor.RED + "Disabled"));
    }

    private void handleConfigCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /mcopt config <analyze|apply>");
            return;
        }
        ConfigurationAnalyzer analyzer = plugin.getConfigAnalyzer();
        if (analyzer == null) {
            sender.sendMessage(ChatColor.RED + "Configuration analyzer is disabled. Enable config-analyzer.enabled first.");
            return;
        }
        if (args[0].equalsIgnoreCase("analyze")) analyzer.analyzeConfigurations(sender, false);
        else if (args[0].equalsIgnoreCase("apply")) analyzer.analyzeConfigurations(sender, true);
        else sender.sendMessage(ChatColor.RED + "Usage: /mcopt config <analyze|apply>");
    }

    private void showLagPredictionStatus(CommandSender sender) {
        if (!plugin.getConfig().getBoolean("lag-prediction.enabled", false)) {
            sender.sendMessage(ChatColor.RED + "Lag prediction is disabled in the configuration.");
            return;
        }
        if (plugin.getLagPredictionManager() == null) {
            sender.sendMessage(ChatColor.RED + "Lag prediction manager is not initialized.");
            return;
        }
        for (String line : plugin.getLagPredictionManager().getPredictionStatus(sender)) sender.sendMessage(line);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            List<String> commands = new ArrayList<>(Arrays.asList("help", "version"));
            if (sender.hasPermission(RELOAD)) commands.add("reload");
            if (sender.hasPermission(STATUS)) commands.add("status");
            if (sender.hasPermission(CONFIG)) commands.add("config");
            for (String value : commands) if (value.startsWith(args[0].toLowerCase())) result.add(value);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("status") && sender.hasPermission(STATUS)) {
            if ("lag".startsWith(args[1].toLowerCase())) result.add("lag");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("config") && sender.hasPermission(CONFIG)) {
            for (String value : Arrays.asList("analyze", "apply")) if (value.startsWith(args[1].toLowerCase())) result.add(value);
        }
        return result;
    }
}
