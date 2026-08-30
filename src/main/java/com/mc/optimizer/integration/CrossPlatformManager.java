/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.PlayerJoinEvent
 *  org.bukkit.metadata.FixedMetadataValue
 *  org.bukkit.metadata.MetadataValue
 *  org.bukkit.plugin.Plugin
 */
package com.mc.optimizer.integration;

import com.mc.optimizer.OptimizerPlugin;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class CrossPlatformManager
implements Listener {
    private final OptimizerPlugin plugin;
    private final Logger logger;
    private boolean geyserSupport;
    private boolean floodgateCompatibility;
    private boolean bedrockOptimizations;
    private boolean floodgateLoaded;
    private boolean geyserLoaded;

    public CrossPlatformManager(OptimizerPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.loadConfig();
        this.checkPlugins();
        plugin.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)plugin);
        this.logger.info("Universal cross-platform support initialized - compatible with all bridge solutions");
    }

    private void loadConfig() {
        this.geyserSupport = true;
        this.floodgateCompatibility = true;
        this.bedrockOptimizations = true;
        this.plugin.getConfig().getBoolean("compatibility.cross-platform.geyser-support", true);
        this.plugin.getConfig().getBoolean("compatibility.cross-platform.floodgate-compatibility", true);
        this.plugin.getConfig().getBoolean("compatibility.cross-platform.bedrock-optimizations", true);
    }

    private void checkPlugins() {
        String[] crossPlatformBridges = new String[]{"Geyser-Spigot", "GeyserMC", "Geyser", "floodgate", "Floodgate", "ViaVersion", "ViaBackwards", "ViaRewind", "ProtocolSupport", "CrossPlatform"};
        boolean foundAny = false;
        for (String pluginName : crossPlatformBridges) {
            Plugin bridgePlugin = Bukkit.getPluginManager().getPlugin(pluginName);
            if (bridgePlugin == null) continue;
            this.logger.info("Cross-platform bridge detected: " + pluginName + " v" + bridgePlugin.getDescription().getVersion());
            foundAny = true;
            if (pluginName.startsWith("Geyser")) {
                this.geyserLoaded = true;
                continue;
            }
            if (!pluginName.startsWith("flood") && !pluginName.startsWith("Flood")) continue;
            this.floodgateLoaded = true;
        }
        if (!foundAny) {
            this.logger.info("No specific cross-platform bridges detected, enabling universal compatibility mode");
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (this.isBedrockPlayer(player)) {
            this.applyBedrockOptimizations(player);
            this.logger.info("Bedrock player " + player.getName() + " joined, applied compatibility settings");
        }
    }

    public boolean isBedrockPlayer(Player player) {
        String uuidStr;
        if (player == null) {
            return false;
        }
        String playerName = player.getName();
        UUID playerId = player.getUniqueId();
        if (playerId != null && ((uuidStr = playerId.toString()).startsWith("00000000") || uuidStr.startsWith("0000") || uuidStr.contains("0000-0000-0000") || uuidStr.startsWith("3333"))) {
            this.logger.fine("Detected non-Java player by UUID pattern: " + playerName);
            return true;
        }
        if (playerName != null && (playerName.startsWith(".") || playerName.startsWith("*") || playerName.contains(".") && playerName.length() > 18 || playerName.contains(":") || playerName.startsWith("[") && playerName.contains("]") || playerName.length() > 16)) {
            this.logger.fine("Detected non-Java player by username pattern: " + playerName);
            return true;
        }
        try {
            String[] bedrockMetadataKeys;
            String[] stringArray = bedrockMetadataKeys = new String[]{"bedrock_player", "floodgate:prefix", "floodgate:brand", "floodgate:locale", "bedrock", "geyser:brand", "geyser:locale", "geyser:device", "crossplatform:platform", "viaversion:platform", "platform"};
            int n = stringArray.length;
            for (int i = 0; i < n; ++i) {
                String key = stringArray[i];
                if (!player.hasMetadata(key)) continue;
                this.logger.fine("Detected non-Java player by metadata (" + key + "): " + playerName);
                return true;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        try {
            for (String methodName : new String[]{"getPlatform", "getClientBrand", "getClientId"}) {
                try {
                    Method method = player.getClass().getMethod(methodName, new Class[0]);
                    Object result = method.invoke((Object)player, new Object[0]);
                    if (result == null || !result.toString().toLowerCase().contains("bedrock")) continue;
                    this.logger.fine("Detected Bedrock player by property (" + methodName + "): " + playerName);
                    return true;
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return false;
    }

    private void applyBedrockOptimizations(Player player) {
        if (player == null) {
            return;
        }
        try {
            this.logger.info("Applying Bedrock-specific optimizations for player: " + player.getName());
            try {
                Object clientViewDistance = player.getClass().getMethod("getViewDistance", new Class[0]).invoke((Object)player, new Object[0]);
                if (clientViewDistance instanceof Integer && (Integer)clientViewDistance > 8) {
                    player.getClass().getMethod("setViewDistance", Integer.TYPE).invoke((Object)player, 8);
                    this.logger.fine("Reduced view distance for Bedrock player " + player.getName());
                }
            }
            catch (Exception clientViewDistance) {
                // empty catch block
            }
            try {
                player.setMetadata("mcoptimizer:bedrock_player", (MetadataValue)new FixedMetadataValue((Plugin)this.plugin, (Object)true));
            }
            catch (Exception clientViewDistance) {
                // empty catch block
            }
            try {
                Object networkManager;
                Object playerConnection = player.getClass().getMethod("getHandle", new Class[0]).invoke((Object)player, new Object[0]);
                if (playerConnection != null && (networkManager = playerConnection.getClass().getField("networkManager").get(playerConnection)) != null) {
                    this.logger.fine("Found network manager for Bedrock player " + player.getName());
                }
            }
            catch (Exception playerConnection) {}
        }
        catch (Exception e) {
            this.logger.warning("Error applying Bedrock optimizations: " + e.getMessage());
        }
    }

    public void reload() {
        this.loadConfig();
        this.checkPlugins();
        this.logger.info("Cross-platform support reloaded");
    }
}

