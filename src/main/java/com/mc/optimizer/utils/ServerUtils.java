package com.mc.optimizer.utils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import com.sun.management.OperatingSystemMXBean;

/** Small, API-safe server pressure helpers. */
public final class ServerUtils {
    private static final OperatingSystemMXBean OS =
            (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
    private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();
    private static final double HIGH_CPU_THRESHOLD = 0.85;
    private static final double HIGH_MEMORY_THRESHOLD = 0.80;

    private ServerUtils() {}

    public static boolean isCPUPressureHigh() {
        double processCpu = OS.getProcessCpuLoad();
        if (processCpu >= 0.0) return processCpu > HIGH_CPU_THRESHOLD;
        double systemLoad = OS.getSystemLoadAverage();
        int processors = Math.max(1, OS.getAvailableProcessors());
        return systemLoad >= 0.0 && systemLoad / processors > HIGH_CPU_THRESHOLD;
    }

    public static boolean isMemoryPressureHigh() {
        long used = MEMORY.getHeapMemoryUsage().getUsed();
        long max = MEMORY.getHeapMemoryUsage().getMax();
        if (max <= 0L) return false;
        return (double) used / max > HIGH_MEMORY_THRESHOLD;
    }

    /**
     * Spigot has no portable TPS API. Callers should prefer the plugin's
     * PerformanceMonitor snapshot. This method remains as a safe fallback for
     * optional legacy components instead of depending on Paper-only methods.
     */
    public static double getEstimatedTPS() {
        return 20.0;
    }
}
