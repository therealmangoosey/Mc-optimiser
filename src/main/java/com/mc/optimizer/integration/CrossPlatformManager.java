package com.mc.optimizer.integration;

import com.mc.optimizer.OptimizerPlugin;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

/** Optional cross-platform integration with Geyser/Floodgate-style bridges. */
public final class CrossPlatformManager implements Listener {
    private static final String BEDROCK_METADATA = "mcoptimizer:bedrock_player";

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
        loadConfig();
        detectBridges();
        if (geyserLoaded || floodgateLoaded) {
            plugin.getServer().getPluginManager().registerEvents(this, plugin);
        }
    }

    private void loadConfig() {
        geyserSupport = plugin.getConfig().getBoolean("compatibility.cross-platform.geyser-support", true);
        floodgateCompatibility = plugin.getConfig().getBoolean("compatibility.cross-platform.floodgate-compatibility", true);
        bedrockOptimizations = plugin.getConfig().getBoolean("compatibility.cross-platform.bedrock-optimizations", true);
    }

    private void detectBridges() {
        geyserLoaded = geyserSupport && (isLoaded("Geyser-Spigot") || isLoaded("GeyserMC") || isLoaded("Geyser"));
        floodgateLoaded = floodgateCompatibility && (isLoaded("floodgate") || isLoaded("Floodgate"));
        if (!geyserLoaded && !floodgateLoaded) return;
        logger.info("Cross-platform bridge detected: Geyser=" + geyserLoaded + ", Floodgate=" + floodgateLoaded);
    }

    private boolean isLoaded(String name) {
        Plugin bridge = Bukkit.getPluginManager().getPlugin(name);
        return bridge != null && bridge.isEnabled();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (bedrockOptimizations && isBedrockPlayer(event.getPlayer())) {
            event.getPlayer().setMetadata(BEDROCK_METADATA, new FixedMetadataValue(plugin, true));
        }
    }

    /**
     * Uses bridge-provided metadata only. UUID/name pattern guessing was removed
     * because it produced false positives for legitimate Java players.
     */
    public boolean isBedrockPlayer(Player player) {
        if (player == null) return false;
        String[] metadataKeys = {"floodgate:prefix", "floodgate:brand", "geyser:brand", "geyser:device", "crossplatform:platform"};
        for (String key : metadataKeys) {
            if (player.hasMetadata(key)) return true;
        }
        return false;
    }

    public void reload() {
        loadConfig();
        detectBridges();
    }
}
