package com.mc.optimizer.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import com.sun.management.OperatingSystemMXBean;
import org.bukkit.Bukkit;

/**
 * Updated for Paper 26.2: previously used reflection into internal NMS
 * server methods (getRecentTps, getTickTime, getServer, etc.) to work
 * around the lack of a public API. Paper has long exposed this
 * officially via Bukkit#getTPS() / Bukkit#getAverageTickTime(), which
 * removes the dependency on internal method names entirely - those are
 * exactly what's at risk of changing given Minecraft 26.1's move to a
 * fully unobfuscated server jar. This version uses the public API only.
 */
public class ServerUtils {
    private static final OperatingSystemMXBean osBean =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private static final double HIGH_CPU_THRESHOLD = 0.85;
    private static final double HIGH_MEMORY_THRESHOLD = 0.8;

    public static boolean isCPUPressureHigh() {
        double cpuLoad = osBean.getCpuLoad();
        if (cpuLoad >= 0.0) {
            return cpuLoad > HIGH_CPU_THRESHOLD;
        }
        // getCpuLoad() returns a negative value if not available yet; fall back.
        double loadAverage = osBean.getSystemLoadAverage();
        int processors = osBean.getAvailableProcessors();
        if (loadAverage < 0.0) {
            return false;
        }
        return loadAverage / (double) processors > HIGH_CPU_THRESHOLD;
    }

    public static boolean isMemoryPressureHigh() {
        long usedMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long maxMemory = memoryBean.getHeapMemoryUsage().getMax();
        double memoryUsage = (double) usedMemory / (double) maxMemory;
        return memoryUsage > HIGH_MEMORY_THRESHOLD;
    }

    public static double getEstimatedTPS() {
        try {
            double[] tps = Bukkit.getTPS();
            return tps[0];
        } catch (Exception e) {
            return 20.0;
        }
    }

    private static double getAverageTickTime() {
        try {
            return Bukkit.getAverageTickTime();
        } catch (Exception e) {
            return 50.0;
        }
    }
}
