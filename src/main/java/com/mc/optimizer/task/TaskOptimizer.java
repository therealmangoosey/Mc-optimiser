/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitScheduler
 *  org.bukkit.scheduler.BukkitTask
 */
package com.mc.optimizer.task;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.config.ConfigManager;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class TaskOptimizer {
    private final OptimizerPlugin plugin;
    private final ConfigManager config;
    private final Logger logger;
    private boolean enabled;
    private boolean monitorOtherPlugins;
    private boolean warnInefficient;
    private int minTaskTimeMs;
    private int maxTasksInReport;
    private BukkitTask monitorTask;
    private final Map<Integer, TaskInfo> monitoredTasks = new ConcurrentHashMap<Integer, TaskInfo>();
    private final Map<String, PluginTaskStats> pluginStats = new ConcurrentHashMap<String, PluginTaskStats>();

    public TaskOptimizer(OptimizerPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.loadConfiguration();
        if (this.enabled) {
            this.startMonitoring();
        }
    }

    private void loadConfiguration() {
        this.enabled = true;
        this.monitorOtherPlugins = true;
        this.warnInefficient = true;
        this.minTaskTimeMs = 10;
        this.maxTasksInReport = 10;
        try {
            if (this.config != null) {
                try {
                    Method method = this.config.getClass().getMethod("isTaskOptimizationEnabled", new Class[0]);
                    this.enabled = (Boolean)method.invoke((Object)this.config, new Object[0]);
                }
                catch (Exception e) {
                    this.logger.warning("Could not load task optimization configuration: " + e.getMessage());
                }
            }
        }
        catch (Exception e) {
            this.logger.warning("Error loading task optimization configuration: " + e.getMessage());
        }
    }

    private void startMonitoring() {
        this.stopMonitoring();
        this.monitoredTasks.clear();
        this.pluginStats.clear();
        this.monitorTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, this::checkTasks, 100L, 200L);
        this.logger.info("Task optimization started");
    }

    private void stopMonitoring() {
        if (this.monitorTask != null) {
            this.monitorTask.cancel();
            this.monitorTask = null;
        }
    }

    private void checkTasks() {
        try {
            BukkitScheduler scheduler = Bukkit.getScheduler();
            HashSet<Integer> activeTasks = new HashSet<Integer>();
            for (BukkitTask task : this.getActiveTasks()) {
                int id = task.getTaskId();
                activeTasks.add(id);
                TaskInfo info = this.monitoredTasks.computeIfAbsent(id, taskId -> new TaskInfo((int)taskId, task.getOwner()));
                info.updateLastSeen();
                String pluginName = task.getOwner().getName();
                PluginTaskStats stats = this.pluginStats.computeIfAbsent(pluginName, name -> new PluginTaskStats(task.getOwner()));
                ++stats.activeTasks;
            }
            Iterator<Map.Entry<Integer, TaskInfo>> it = this.monitoredTasks.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, TaskInfo> entry = it.next();
                if (activeTasks.contains(entry.getKey())) continue;
                TaskInfo info = entry.getValue();
                String pluginName = info.getPlugin().getName();
                PluginTaskStats stats = this.pluginStats.get(pluginName);
                if (stats != null) {
                    ++stats.completedTasks;
                }
                it.remove();
            }
            if (this.warnInefficient) {
                this.checkInefficientTasks();
            }
        }
        catch (Exception e) {
            this.logger.log(Level.WARNING, "Error while monitoring tasks", e);
        }
    }

    private List<BukkitTask> getActiveTasks() {
        ArrayList<BukkitTask> tasks = new ArrayList<BukkitTask>();
        if (this.monitorOtherPlugins) {
            // empty if block
        }
        Bukkit.getScheduler().getPendingTasks().forEach(task -> {
            if (task.getOwner().equals((Object)this.plugin) || this.monitorOtherPlugins) {
                tasks.add((BukkitTask)task);
            }
        });
        return tasks;
    }

    private void checkInefficientTasks() {
        List<TaskInfo> inefficientTasks = this.monitoredTasks.values().stream().filter(info -> info.getAverageExecutionTimeMs() > (double)this.minTaskTimeMs).sorted(Comparator.comparingDouble(TaskInfo::getAverageExecutionTimeMs).reversed()).limit(this.maxTasksInReport).collect(Collectors.toList());
        if (!inefficientTasks.isEmpty()) {
            this.logger.warning("Detected " + inefficientTasks.size() + " potentially inefficient tasks:");
            for (TaskInfo task : inefficientTasks) {
                this.logger.warning(String.format("  - Task ID %d (Plugin: %s) Avg. Time: %.2f ms, Runs: %d", task.getTaskId(), task.getPlugin().getName(), task.getAverageExecutionTimeMs(), task.getExecutionCount()));
            }
        }
    }

    public Map<String, Object> getStats() {
        HashMap<String, Object> stats = new HashMap<String, Object>();
        stats.put("enabled", this.enabled);
        stats.put("activeTasks", this.monitoredTasks.size());
        stats.put("monitoredPlugins", this.pluginStats.size());
        List<Map<String, Object>> inefficientTaskList = this.monitoredTasks.values().stream().filter(info -> info.getAverageExecutionTimeMs() > (double)this.minTaskTimeMs).sorted(Comparator.comparingDouble(TaskInfo::getAverageExecutionTimeMs).reversed()).limit(this.maxTasksInReport).map(task -> {
            HashMap<String, Object> taskMap = new HashMap<String, Object>();
            taskMap.put("id", task.getTaskId());
            taskMap.put("plugin", task.getPlugin().getName());
            taskMap.put("avgTime", task.getAverageExecutionTimeMs());
            taskMap.put("runs", task.getExecutionCount());
            return taskMap;
        }).collect(Collectors.toList());
        stats.put("inefficientTasks", inefficientTaskList);
        return stats;
    }

    public void reload() {
        this.stopMonitoring();
        this.monitoredTasks.clear();
        this.pluginStats.clear();
        this.loadConfiguration();
        if (this.enabled) {
            this.startMonitoring();
        }
    }

    public void shutdown() {
        this.stopMonitoring();
        this.monitoredTasks.clear();
        this.pluginStats.clear();
        this.logger.info("Task optimizer shutdown");
    }

    private static class TaskInfo {
        private final int taskId;
        private final Plugin plugin;
        private long lastSeen;
        private int executionCount;
        private double totalExecutionTimeMs;

        public TaskInfo(int taskId, Plugin plugin) {
            this.taskId = taskId;
            this.plugin = plugin;
            this.lastSeen = System.currentTimeMillis();
            this.executionCount = 0;
            this.totalExecutionTimeMs = 0.0;
        }

        public void updateLastSeen() {
            this.lastSeen = System.currentTimeMillis();
        }

        public void recordExecution(double executionTimeMs) {
            ++this.executionCount;
            this.totalExecutionTimeMs += executionTimeMs;
        }

        public double getAverageExecutionTimeMs() {
            return this.executionCount > 0 ? this.totalExecutionTimeMs / (double)this.executionCount : 0.0;
        }

        public int getTaskId() {
            return this.taskId;
        }

        public Plugin getPlugin() {
            return this.plugin;
        }

        public long getLastSeen() {
            return this.lastSeen;
        }

        public int getExecutionCount() {
            return this.executionCount;
        }

        public double getTotalExecutionTimeMs() {
            return this.totalExecutionTimeMs;
        }
    }

    private static class PluginTaskStats {
        private final Plugin plugin;
        private int activeTasks;
        private int completedTasks;

        public PluginTaskStats(Plugin plugin) {
            this.plugin = plugin;
            this.activeTasks = 0;
            this.completedTasks = 0;
        }

        public Plugin getPlugin() {
            return this.plugin;
        }

        public int getActiveTasks() {
            return this.activeTasks;
        }

        public int getCompletedTasks() {
            return this.completedTasks;
        }
    }
}

