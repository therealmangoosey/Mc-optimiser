/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.plugin.Plugin
 */
package com.mc.optimizer.api;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.api.AccessLevel;
import com.mc.optimizer.config.ConfigManager;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;

public class PluginIntegrationAPI {
    private final OptimizerPlugin plugin;
    private final ConfigManager config;
    private final Logger logger;
    private boolean enabled;
    private boolean logRegistrations;
    private boolean allowCustomOptimizers;
    private boolean allowMetricsAccess;
    private AccessLevel defaultAccessLevel;
    private final Map<String, RegisteredPlugin> registeredPlugins = new HashMap<String, RegisteredPlugin>();
    private final Map<String, CustomOptimizer> customOptimizers = new HashMap<String, CustomOptimizer>();

    public PluginIntegrationAPI(OptimizerPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.loadConfiguration();
        if (this.enabled) {
            this.initialize();
        }
    }

    private void loadConfiguration() {
        this.enabled = true;
        this.logRegistrations = true;
        this.allowCustomOptimizers = false;
        this.allowMetricsAccess = false;
        this.defaultAccessLevel = AccessLevel.READ_ONLY;
        try {
            this.enabled = this.plugin.getConfig().getBoolean("api.enabled", true);
            this.logRegistrations = this.plugin.getConfig().getBoolean("api.log-registrations", true);
            this.allowCustomOptimizers = this.plugin.getConfig().getBoolean("api.allow-custom-optimizers", false);
            this.allowMetricsAccess = this.plugin.getConfig().getBoolean("api.allow-metrics-access", false);
            String accessLevelStr = this.plugin.getConfig().getString("api.default-access-level", "READ_ONLY");
            try {
                this.defaultAccessLevel = AccessLevel.valueOf(accessLevelStr.toUpperCase());
            }
            catch (Exception e) {
                this.defaultAccessLevel = AccessLevel.READ_ONLY;
            }
        }
        catch (Exception e) {
            this.logger.warning("Error loading API configuration: " + e.getMessage());
        }
    }

    private void initialize() {
        this.logger.info("Plugin integration API initialized");
    }

    public boolean registerPlugin(Plugin plugin, String apiVersion) {
        if (!this.enabled) {
            return false;
        }
        String pluginName = plugin.getName();
        if (this.registeredPlugins.containsKey(pluginName)) {
            return false;
        }
        RegisteredPlugin registration = new RegisteredPlugin(plugin, apiVersion, this.defaultAccessLevel, new Date());
        this.registeredPlugins.put(pluginName, registration);
        if (this.logRegistrations) {
            this.logger.info("Plugin " + pluginName + " registered with MCOptimizer API (v" + apiVersion + ")");
        }
        return true;
    }

    public boolean unregisterPlugin(Plugin plugin) {
        if (!this.enabled) {
            return false;
        }
        String pluginName = plugin.getName();
        if (!this.registeredPlugins.containsKey(pluginName)) {
            return false;
        }
        this.customOptimizers.entrySet().removeIf(entry -> ((CustomOptimizer)entry.getValue()).getPlugin().getName().equals(pluginName));
        this.registeredPlugins.remove(pluginName);
        if (this.logRegistrations) {
            this.logger.info("Plugin " + pluginName + " unregistered from MCOptimizer API");
        }
        return true;
    }

    public boolean registerCustomOptimizer(Plugin plugin, String name, CustomOptimizer optimizer) {
        if (!this.enabled || !this.allowCustomOptimizers) {
            return false;
        }
        String pluginName = plugin.getName();
        if (!this.registeredPlugins.containsKey(pluginName)) {
            return false;
        }
        RegisteredPlugin registration = this.registeredPlugins.get(pluginName);
        if (registration.getAccessLevel() < 2) {
            return false;
        }
        String key = pluginName + ":" + name;
        this.customOptimizers.put(key, optimizer);
        if (this.logRegistrations) {
            this.logger.info("Custom optimizer " + key + " registered");
        }
        return true;
    }

    public boolean unregisterCustomOptimizer(Plugin plugin, String name) {
        if (!this.enabled) {
            return false;
        }
        String pluginName = plugin.getName();
        String key = pluginName + ":" + name;
        if (!this.customOptimizers.containsKey(key)) {
            return false;
        }
        this.customOptimizers.remove(key);
        if (this.logRegistrations) {
            this.logger.info("Custom optimizer " + key + " unregistered");
        }
        return true;
    }

    public Map<String, Object> getStatistics(Plugin plugin) {
        if (!this.enabled || !this.allowMetricsAccess) {
            return null;
        }
        String pluginName = plugin.getName();
        if (!this.registeredPlugins.containsKey(pluginName)) {
            return null;
        }
        RegisteredPlugin registration = this.registeredPlugins.get(pluginName);
        if (registration.getAccessLevel() < 1) {
            return null;
        }
        HashMap<String, Object> stats = new HashMap<String, Object>();
        stats.put("version", this.plugin.getDescription().getVersion());
        stats.put("apiVersion", "1.0");
        if (this.plugin.getChunkManager() != null) {
            stats.put("chunkManagerEnabled", this.plugin.getChunkManager().isEnabled());
        }
        if (this.plugin.getEntityOptimizer() != null) {
            stats.put("entityOptimizerEnabled", this.plugin.getEntityOptimizer().isEnabled());
        }
        if (this.plugin.getRedstoneOptimizer() != null) {
            stats.put("redstoneOptimizerEnabled", this.plugin.getRedstoneOptimizer().isEnabled());
        }
        if (this.plugin.getMobCullingManager() != null) {
            stats.put("mobCullingEnabled", this.plugin.getMobCullingManager().isEnabled());
        }
        if (registration.getAccessLevel() >= 2) {
            stats.put("customOptimizers", this.customOptimizers.size());
            if (registration.getAccessLevel() >= 3 && this.plugin.getPerformanceMonitor() != null) {
                stats.put("performance", Map.of("tps", this.plugin.getPerformanceMonitor().getLatestSnapshot().tps, "entities", this.plugin.getPerformanceMonitor().getLatestSnapshot().totalEntities, "chunks", this.plugin.getPerformanceMonitor().getLatestSnapshot().loadedChunks));
            }
        }
        return stats;
    }

    public boolean setAccessLevel(String pluginName, int level) {
        if (!this.enabled) {
            return false;
        }
        if (!this.registeredPlugins.containsKey(pluginName)) {
            return false;
        }
        RegisteredPlugin registration = this.registeredPlugins.get(pluginName);
        registration.setAccessLevel(level);
        return true;
    }

    public Map<String, RegisteredPlugin> getRegisteredPlugins() {
        return Collections.unmodifiableMap(this.registeredPlugins);
    }

    public Map<String, CustomOptimizer> getCustomOptimizers() {
        return Collections.unmodifiableMap(this.customOptimizers);
    }

    public Map<String, Object> getStats() {
        HashMap<String, Object> stats = new HashMap<String, Object>();
        stats.put("enabled", this.enabled);
        stats.put("registeredPlugins", this.registeredPlugins.size());
        stats.put("customOptimizers", this.customOptimizers.size());
        return stats;
    }

    public void reload() {
        this.customOptimizers.clear();
        this.registeredPlugins.clear();
        this.loadConfiguration();
        if (this.enabled) {
            this.initialize();
        }
    }

    public void shutdown() {
        for (RegisteredPlugin registration : this.registeredPlugins.values()) {
            Plugin registeredPlugin = registration.getPlugin();
            if (!registeredPlugin.isEnabled()) continue;
            try {
                this.logger.fine("Notifying plugin " + registeredPlugin.getName() + " of shutdown");
            }
            catch (Exception exception) {}
        }
        this.customOptimizers.clear();
        this.registeredPlugins.clear();
        this.logger.info("Plugin integration API shutdown");
    }

    public static class RegisteredPlugin {
        private final Plugin plugin;
        private final String apiVersion;
        private AccessLevel accessLevel;
        private final Date registrationDate;

        public RegisteredPlugin(Plugin plugin, String apiVersion, AccessLevel accessLevel, Date registrationDate) {
            this.plugin = plugin;
            this.apiVersion = apiVersion;
            this.accessLevel = accessLevel;
            this.registrationDate = registrationDate;
        }

        public Plugin getPlugin() {
            return this.plugin;
        }

        public String getApiVersion() {
            return this.apiVersion;
        }

        public int getAccessLevel() {
            return this.accessLevel.getLevel();
        }

        public void setAccessLevel(int level) {
            this.accessLevel = level <= 0 ? AccessLevel.NONE : (level == 1 ? AccessLevel.READ_ONLY : (level == 2 ? AccessLevel.STANDARD : AccessLevel.ADMIN));
        }

        public Date getRegistrationDate() {
            return this.registrationDate;
        }
    }

    public static interface CustomOptimizer {
        public Plugin getPlugin();

        public String getName();

        public boolean isEnabled();

        public void run();

        public String getDescription();

        public Map<String, Object> getStats();
    }
}

