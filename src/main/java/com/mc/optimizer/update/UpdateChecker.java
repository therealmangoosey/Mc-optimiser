package com.mc.optimizer.update;

import com.mc.optimizer.OptimizerPlugin;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;

public final class UpdateChecker {
    private static final URI RELEASES_URI = URI.create("https://api.github.com/repos/therealmangoosey/Mc-optimiser/releases/latest");
    private static final Pattern TAG_PATTERN = Pattern.compile("\\\"tag_name\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"");
    private static final Pattern VERSION_PATTERN = Pattern.compile("(?:v)?(\\d+)\\.(\\d+)\\.(\\d+)");
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private UpdateChecker() {}

    public static void check(OptimizerPlugin plugin) {
        final String current = plugin.getDescription().getVersion();
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder(RELEASES_URI)
                        .header("Accept", "application/vnd.github+json")
                        .header("User-Agent", "MCOptimizer/" + current)
                        .timeout(Duration.ofSeconds(10)).GET().build();
                HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    plugin.getLogger().warning("Update check failed: GitHub HTTP " + response.statusCode() + ".");
                    return;
                }
                Matcher tag = TAG_PATTERN.matcher(response.body());
                if (!tag.find()) {
                    plugin.getLogger().warning("Update check failed: no release version was returned.");
                    return;
                }
                String latest = tag.group(1);
                int comparison = compare(current, latest);
                if (comparison < 0) {
                    long behind = versionDistance(current, latest);
                    String count = behind >= 0 ? " You are " + behind + " version" + (behind == 1 ? "" : "s") + " behind." : "";
                    plugin.getLogger().warning("Update available: MCOptimizer " + latest + " (installed: " + current + ")." + count);
                    plugin.getLogger().warning("Download: https://github.com/therealmangoosey/Mc-optimiser/releases/latest");
                } else if (comparison == 0) {
                    plugin.getLogger().info("MCOptimizer is up to date (" + current + ").");
                } else {
                    plugin.getLogger().info("MCOptimizer " + current + " is newer than the latest published release (" + latest + ").");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                plugin.getLogger().warning("Update check was interrupted.");
            } catch (IOException | RuntimeException e) {
                plugin.getLogger().warning("Update check failed: " + e.getMessage());
            }
        });
    }

    private static int compare(String current, String latest) {
        int[] a = parse(current), b = parse(latest);
        if (a == null || b == null) return 0;
        for (int i = 0; i < 3; i++) if (a[i] != b[i]) return Integer.compare(a[i], b[i]);
        return 0;
    }

    private static long versionDistance(String current, String latest) {
        int[] a = parse(current), b = parse(latest);
        if (a == null || b == null) return -1;
        return Math.max(0L, ((long)b[0] - a[0]) * 10000L + ((long)b[1] - a[1]) * 100L + b[2] - a[2]);
    }

    private static int[] parse(String value) {
        Matcher m = VERSION_PATTERN.matcher(value == null ? "" : value.trim());
        if (!m.matches()) return null;
        return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3))};
    }
}
