/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Chunk
 *  org.bukkit.World
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.mc.optimizer.lagprediction;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.memory.MemoryLeakDetector;
import com.mc.optimizer.metrics.PerformanceMonitor;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class LagPredictionManager {
    private final OptimizerPlugin plugin;
    private final Logger logger;
    private final PerformanceMonitor performanceMonitor;
    private boolean enabled;
    private int predictionInterval;
    private int dataPointsToKeep;
    private double predictionThreshold;
    private double cpuThreshold;
    private long memoryThreshold;
    private boolean notifyAdmins;
    private boolean autoTuneSettings;
    private boolean webInterfaceEnabled;
    private int webInterfacePort;
    private String webInterfaceBindAddress;
    private String webInterfaceAccessToken;
    private boolean requireToken;
    private ServerSocket webServer;
    private ExecutorService webExecutor;
    private final Queue<PerformanceDataPoint> historicalData;
    private final Map<UUID, LagPredictionResult> lastPredictionByWorld;
    private BukkitTask predictionTask;
    private int predictionCounter;
    private boolean lagPredicted;
    private String predictedCause;
    private double predictionConfidence;
    private String recommendedAction;
    private long lastPredictionTime;
    private List<PredictionEvent> predictionHistory;
    private final Map<String, Socket> sseClients = new ConcurrentHashMap<String, Socket>();

    public LagPredictionManager(OptimizerPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        this.performanceMonitor = plugin.getPerformanceMonitor();
        this.historicalData = new LinkedList<PerformanceDataPoint>();
        this.lastPredictionByWorld = new HashMap<UUID, LagPredictionResult>();
        this.predictionCounter = 0;
        this.predictionHistory = new ArrayList<PredictionEvent>();
        this.loadConfig();
        if (this.enabled) {
            this.startPrediction();
        }
        if (this.webInterfaceEnabled) {
            this.startWebInterface();
        }
    }

    private void loadConfig() {
        this.enabled = this.plugin.getConfig().getBoolean("lag-prediction.enabled", true);
        this.predictionInterval = this.plugin.getConfig().getInt("lag-prediction.interval", 30);
        this.dataPointsToKeep = this.plugin.getConfig().getInt("lag-prediction.data-points", 60);
        this.predictionThreshold = this.plugin.getConfig().getDouble("lag-prediction.tps-threshold", 18.0);
        this.cpuThreshold = this.plugin.getConfig().getDouble("lag-prediction.cpu-threshold", 0.8);
        this.memoryThreshold = this.plugin.getConfig().getLong("lag-prediction.memory-threshold", 2048L);
        this.notifyAdmins = this.plugin.getConfig().getBoolean("lag-prediction.notify-admins", true);
        this.autoTuneSettings = this.plugin.getConfig().getBoolean("lag-prediction.auto-tune", false);
        this.webInterfaceEnabled = this.plugin.getConfig().getBoolean("lag-prediction.web-interface.enabled", false);
        this.webInterfacePort = this.plugin.getConfig().getInt("lag-prediction.web-interface.port", 8080);
        this.webInterfaceBindAddress = this.plugin.getConfig().getString("lag-prediction.web-interface.bind-address", "0.0.0.0");
        this.webInterfaceAccessToken = this.plugin.getConfig().getString("lag-prediction.web-interface.access-token", "");
        this.requireToken = !this.webInterfaceAccessToken.isEmpty();
        this.logger.info("Lag prediction " + (this.enabled ? "enabled" : "disabled") + " (interval: " + this.predictionInterval + "s, points: " + this.dataPointsToKeep + ")");
        if (this.webInterfaceEnabled) {
            this.logger.info("Web interface " + (this.webInterfaceEnabled ? "enabled" : "disabled") + " (port: " + this.webInterfacePort + ", auth: " + (this.requireToken ? "required" : "disabled") + ")");
        }
    }

    private void startPrediction() {
        if (this.predictionTask != null) {
            this.predictionTask.cancel();
        }
        this.predictionTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            this.collectData();
            if (this.predictionCounter % 5 == 0) {
                this.predictLag();
            }
            ++this.predictionCounter;
        }, 200L, 20L * (long)this.predictionInterval);
        this.logger.info("Started lag prediction task (interval: " + this.predictionInterval + "s)");
    }

    private void startWebInterface() {
        if (!this.webInterfaceEnabled) {
            return;
        }
        try {
            if (this.webServer != null && !this.webServer.isClosed()) {
                try {
                    this.webServer.close();
                }
                catch (IOException e) {
                    this.logger.warning("Error closing existing web server: " + e.getMessage());
                }
            }
            if (this.webExecutor != null) {
                this.webExecutor.shutdown();
            }
            this.webServer = new ServerSocket();
            this.webServer.bind(new InetSocketAddress(this.webInterfaceBindAddress, this.webInterfacePort));
            this.webExecutor = Executors.newFixedThreadPool(3);
            Bukkit.getScheduler().runTaskAsynchronously((Plugin)this.plugin, () -> {
                this.logger.info("Started web interface on " + this.webInterfaceBindAddress + ":" + this.webInterfacePort);
                while (!this.webServer.isClosed() && this.plugin.isEnabled()) {
                    try {
                        Socket socket = this.webServer.accept();
                        this.webExecutor.submit(() -> {
                            try {
                                this.handleWebRequest(socket);
                            }
                            catch (Exception e) {
                                this.logger.warning("Error handling web request: " + e.getMessage());
                            }
                            finally {
                                try {
                                    socket.close();
                                }
                                catch (IOException iOException) {}
                            }
                        });
                    }
                    catch (IOException e) {
                        if (this.webServer.isClosed() || !this.plugin.isEnabled()) continue;
                        this.logger.warning("Error accepting web connection: " + e.getMessage());
                    }
                }
            });
        }
        catch (IOException e) {
            this.logger.severe("Failed to start web interface: " + e.getMessage());
            this.webInterfaceEnabled = false;
        }
    }

    /*
     * WARNING - void declaration
     */
    private void handleWebRequest(Socket socket) {
        block30: {
            try {
                byte[] responseBytes;
                String responseBody = null;
                String contentType;
                int statusCode;
                String query;
                String line;
                socket.setSoTimeout(10000);
                InputStream input = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(input));
                String requestLine = reader.readLine();
                if (requestLine == null) {
                    return;
                }
                String[] parts = requestLine.split(" ");
                if (parts.length < 3) {
                    return;
                }
                String method = parts[0];
                String path = parts[1];
                HashMap<String, String> headers = new HashMap<String, String>();
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    int colonIndex = line.indexOf(58);
                    if (colonIndex <= 0) continue;
                    String name = line.substring(0, colonIndex).trim();
                    String string = line.substring(colonIndex + 1).trim();
                    headers.put(name.toLowerCase(), string);
                }
                boolean authenticated = !this.requireToken;
                String string = query = path.contains("?") ? path.substring(path.indexOf(63) + 1) : "";
                if (this.requireToken && !authenticated) {
                    String string2;
                    for (String param : query.split("&")) {
                        String[] paramParts = param.split("=", 2);
                        if (paramParts.length != 2 || !paramParts[0].equals("token") || !paramParts[1].equals(this.webInterfaceAccessToken)) continue;
                        authenticated = true;
                        break;
                    }
                    if (!authenticated && headers.containsKey("authorization") && (string2 = (String)headers.get("authorization")).startsWith("Bearer ") && string2.substring(7).trim().equals(this.webInterfaceAccessToken)) {
                        authenticated = true;
                    }
                }
                if (!authenticated) {
                    statusCode = 401;
                    contentType = "text/html";
                    responseBody = this.generateErrorPage("401 Unauthorized", "Authentication required");
                } else if (method.equals("GET")) {
                    if (path.equals("/") || path.startsWith("/?")) {
                        statusCode = 200;
                        contentType = "text/html";
                        responseBody = this.generateStatusPage();
                    } else if (path.equals("/api/status") || path.startsWith("/api/status?")) {
                        statusCode = 200;
                        contentType = "application/json";
                        responseBody = this.generateStatusJson();
                    } else if (path.equals("/api/history") || path.startsWith("/api/history?")) {
                        statusCode = 200;
                        contentType = "application/json";
                        responseBody = this.generateHistoryJson();
                    } else {
                        if (path.equals("/api/stream") || path.startsWith("/api/stream?")) {
                            this.handleSSERequest(socket, headers);
                            return;
                        }
                        if (path.equals("/favicon.ico")) {
                            statusCode = 404;
                            contentType = "text/plain";
                            responseBody = "Not found";
                        } else {
                            statusCode = 404;
                            contentType = "text/html";
                            responseBody = this.generateErrorPage("404 Not Found", "The requested resource was not found");
                        }
                    }
                } else {
                    statusCode = 405;
                    contentType = "text/html";
                    responseBody = this.generateErrorPage("405 Method Not Allowed", "Only GET requests are supported");
                }
                boolean acceptsGzip = false;
                String acceptEncoding = headers.getOrDefault("accept-encoding", "");
                if (acceptEncoding.contains("gzip")) {
                    acceptsGzip = true;
                }
                OutputStream output = socket.getOutputStream();
                StringBuilder headerBuilder = new StringBuilder(512);
                headerBuilder.append("HTTP/1.1 ").append(statusCode).append(" ").append(this.getStatusMessage(statusCode)).append("\r\n");
                headerBuilder.append("Content-Type: ").append(contentType).append("\r\n");
                headerBuilder.append("Server: MCOptimizer/").append(this.plugin.getDescription().getVersion()).append("\r\n");
                if (contentType.equals("text/html")) {
                    headerBuilder.append("Cache-Control: public, max-age=60\r\n");
                } else if (contentType.equals("application/json")) {
                    headerBuilder.append("Cache-Control: public, max-age=30\r\n");
                }
                String etag = "\"" + Integer.toHexString(responseBody.hashCode()) + "\"";
                headerBuilder.append("ETag: ").append(etag).append("\r\n");
                String ifNoneMatch = headers.getOrDefault("if-none-match", "");
                if (ifNoneMatch.equals(etag)) {
                    headerBuilder = new StringBuilder(256);
                    headerBuilder.append("HTTP/1.1 304 Not Modified\r\n");
                    headerBuilder.append("ETag: ").append(etag).append("\r\n");
                    headerBuilder.append("Server: MCOptimizer/").append(this.plugin.getDescription().getVersion()).append("\r\n");
                    headerBuilder.append("Connection: close\r\n\r\n");
                    output.write(headerBuilder.toString().getBytes(StandardCharsets.UTF_8));
                    output.flush();
                    return;
                }
                byte[] contentBytes = responseBody.getBytes(StandardCharsets.UTF_8);
                if (acceptsGzip && contentBytes.length > 500) {
                    try {
                        ByteArrayOutputStream compressedOut = new ByteArrayOutputStream(contentBytes.length);
                        GZIPOutputStream gzipOut = new GZIPOutputStream(compressedOut);
                        gzipOut.write(contentBytes);
                        gzipOut.close();
                        responseBytes = compressedOut.toByteArray();
                        headerBuilder.append("Content-Encoding: gzip\r\n");
                    }
                    catch (Exception e) {
                        responseBytes = contentBytes;
                    }
                } else {
                    responseBytes = contentBytes;
                }
                headerBuilder.append("Content-Length: ").append(responseBytes.length).append("\r\n");
                headerBuilder.append("Connection: close\r\n\r\n");
                output.write(headerBuilder.toString().getBytes(StandardCharsets.UTF_8));
                output.write(responseBytes);
                output.flush();
            }
            catch (IOException e) {
                if (!this.plugin.isEnabled()) break block30;
                this.logger.warning("Error handling web request: " + e.getMessage());
            }
        }
    }

    private String generateStatusPage() {
        Object sdf;
        StringBuilder html = new StringBuilder(4096);
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"><meta http-equiv=\"Cache-Control\" content=\"max-age=60\"><title>MCOptimizer - Server Status</title><style>");
        html.append("body{font-family:Arial,sans-serif;line-height:1.6;margin:0;padding:20px;color:#333;background-color:#f8f8f8}");
        html.append("header{background-color:#2c3e50;color:#fff;padding:1em;margin-bottom:20px;border-radius:5px}");
        html.append("h1{margin:0;font-size:24px}");
        html.append(".container{max-width:1200px;margin:0 auto}");
        html.append(".card{background:#fff;border-radius:5px;box-shadow:0 2px 5px rgba(0,0,0,.1);padding:20px;margin-bottom:20px}");
        html.append(".flex-container{display:flex;flex-wrap:wrap;gap:20px}");
        html.append(".metric{flex:1;min-width:250px}");
        html.append(".metric h3{margin-top:0;color:#2c3e50}");
        html.append(".status-ok{color:#27ae60}");
        html.append(".status-warning{color:#f39c12}");
        html.append(".status-error{color:#e74c3c}");
        html.append(".value{font-size:20px;font-weight:700;margin:10px 0}");
        html.append(".worlds{margin-top:20px}");
        html.append("table{width:100%;border-collapse:collapse}");
        html.append("table th,table td{text-align:left;padding:12px;border-bottom:1px solid #ddd}");
        html.append("table th{background-color:#f2f2f2}");
        html.append(".connection-status{font-size:14px;margin-top:5px;margin-bottom:10px}");
        html.append(".connected{color:#27ae60}");
        html.append(".disconnected{color:#e74c3c}");
        html.append("footer{margin-top:30px;text-align:center;font-size:14px;color:#7f8c8d}");
        html.append(".update-time{font-size:12px;color:#7f8c8d;margin-top:5px}");
        html.append("@media (max-width:768px){.flex-container{flex-direction:column}}");
        html.append(".emotion-wheel-container{display:flex;flex-direction:column;align-items:center;margin:20px 0}");
        html.append(".emotion-wheel{position:relative;width:200px;height:200px;border-radius:50%;margin:20px auto;transition:transform 0.5s ease}");
        html.append(".emotion-face{position:absolute;top:0;left:0;width:100%;height:100%;border-radius:50%;display:flex;justify-content:center;align-items:center;font-size:80px;background:#f0f0f0;box-shadow:0 4px 10px rgba(0,0,0,0.2);opacity:0;transition:opacity 0.3s ease}");
        html.append(".emotion-face.active{opacity:1}");
        html.append(".emotion-face-happy{background:linear-gradient(135deg,#b4ec51,#429321)}");
        html.append(".emotion-face-good{background:linear-gradient(135deg,#8fd16a,#4caf50)}");
        html.append(".emotion-face-neutral{background:linear-gradient(135deg,#f8d209,#f2c718)}");
        html.append(".emotion-face-concerned{background:linear-gradient(135deg,#f89406,#e67e22)}");
        html.append(".emotion-face-stressed{background:linear-gradient(135deg,#ff5e3a,#e74c3c)}");
        html.append(".emotion-face-critical{background:linear-gradient(135deg,#cb356b,#bd3f32)}");
        html.append(".emotion-wheel-label{font-size:16px;font-weight:bold;margin-top:10px;text-align:center}");
        html.append(".emotion-wheel-description{font-size:14px;text-align:center;max-width:300px;margin:0 auto}");
        html.append("</style></head><body><div class=\"container\"><header><h1>MCOptimizer - Server Status</h1><div id=\"connection-status\" class=\"connection-status disconnected\">Loading...</div></header>");
        html.append("<div class=\"card\"><h2>Server Health Status</h2>");
        html.append("<div class=\"emotion-wheel-container\">");
        html.append("<div class=\"emotion-wheel\">");
        html.append("<div class=\"emotion-face emotion-face-happy\" id=\"face-5\">\ud83d\ude04</div>");
        html.append("<div class=\"emotion-face emotion-face-good\" id=\"face-4\">\ud83d\ude42</div>");
        html.append("<div class=\"emotion-face emotion-face-neutral\" id=\"face-3\">\ud83d\ude10</div>");
        html.append("<div class=\"emotion-face emotion-face-concerned\" id=\"face-2\">\ud83d\ude1f</div>");
        html.append("<div class=\"emotion-face emotion-face-stressed\" id=\"face-1\">\ud83d\ude30</div>");
        html.append("<div class=\"emotion-face emotion-face-critical\" id=\"face-0\">\ud83d\ude31</div>");
        html.append("<div class=\"emotion-face\" id=\"face-unknown\">\u2753</div>");
        html.append("</div>");
        html.append("<div class=\"emotion-wheel-label\" id=\"health-status-label\">Calculating...</div>");
        html.append("<div class=\"emotion-wheel-description\" id=\"health-status-description\">Gathering server metrics...</div>");
        html.append("</div></div>");
        html.append("<div class=\"card\"><h2>Server Overview</h2><div class=\"flex-container\">");
        PerformanceDataPoint latest = this.getLatestDataPoint();
        String tpsStatus = "status-ok";
        if (latest != null && latest.tps < 18.0) {
            tpsStatus = latest.tps < 15.0 ? "status-error" : "status-warning";
        }
        html.append("        <div class=\"metric\">\n");
        html.append("          <h3>TPS (Ticks Per Second)</h3>\n");
        html.append("          <div class=\"value " + tpsStatus + "\">" + (latest != null ? String.format("%.1f", latest.tps) : "N/A") + "</div>\n");
        html.append("          <div>Target: 20.0</div>\n");
        html.append("        </div>\n");
        String memoryStatus = "status-ok";
        long usedMemory = 0L;
        long totalMemory = 0L;
        long freeMemory = 0L;
        if (latest != null) {
            usedMemory = latest.memoryUsed / 1024L / 1024L;
            totalMemory = (latest.memoryUsed + latest.memoryFree) / 1024L / 1024L;
            freeMemory = latest.memoryFree / 1024L / 1024L;
            if (freeMemory < this.memoryThreshold) {
                memoryStatus = freeMemory < this.memoryThreshold / 2L ? "status-error" : "status-warning";
            }
        }
        html.append("        <div class=\"metric\">\n");
        html.append("          <h3>Memory Usage</h3>\n");
        html.append("          <div class=\"value " + memoryStatus + "\">" + (String)(latest != null ? usedMemory + " MB / " + totalMemory + " MB" : "N/A") + "</div>\n");
        html.append("          <div>Free: " + (String)(latest != null ? freeMemory + " MB" : "N/A") + "</div>\n");
        html.append("        </div>\n");
        String cpuStatus = "status-ok";
        if (latest != null && latest.cpuLoad > this.cpuThreshold * 0.8) {
            cpuStatus = latest.cpuLoad > this.cpuThreshold ? "status-error" : "status-warning";
        }
        html.append("        <div class=\"metric\">\n");
        html.append("          <h3>CPU Load</h3>\n");
        html.append("          <div class=\"value " + cpuStatus + "\">" + (String)(latest != null ? String.format("%.1f", latest.cpuLoad * 100.0) + "%" : "N/A") + "</div>\n");
        html.append("          <div>Threshold: " + String.format("%.0f", this.cpuThreshold * 100.0) + "%</div>\n");
        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        String predictionStatus = this.lagPredicted ? (this.predictionConfidence > 0.8 ? "status-error" : "status-warning") : "status-ok";
        html.append("    <div class=\"card\">\n");
        html.append("      <h2>Lag Prediction</h2>\n");
        html.append("      <div class=\"flex-container\">\n");
        html.append("        <div class=\"metric\">\n");
        html.append("          <h3>Status</h3>\n");
        html.append("          <div class=\"value " + predictionStatus + "\">" + (this.lagPredicted ? "Lag Predicted" : "No Lag Predicted") + "</div>\n");
        if (this.lagPredicted) {
            html.append("          <div><strong>Confidence:</strong> " + String.format("%.0f", this.predictionConfidence * 100.0) + "%</div>\n");
            html.append("          <div><strong>Cause:</strong> " + this.predictedCause + "</div>\n");
            html.append("          <div><strong>Recommendation:</strong> " + this.recommendedAction + "</div>\n");
            if (this.lastPredictionTime > 0L) {
                SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                html.append("          <div><strong>Predicted at:</strong> " + sdf2.format(new Date(this.lastPredictionTime)) + "</div>\n");
            }
        }
        html.append("        </div>\n");
        html.append("      </div>\n");
        html.append("    </div>\n");
        html.append("    <div class=\"card\">\n");
        html.append("      <h2>Memory Leak History</h2>\n");
        MemoryLeakDetector leakDetector = this.plugin.getMemoryLeakDetector();
        boolean leakDetected = leakDetector.isLeakDetected();
        List<MemoryLeakDetector.MemorySnapshot> memoryHistory = leakDetector.getMemoryHistory();
        if (leakDetected) {
            html.append("      <div class=\"alert alert-danger\">\n");
            html.append("        <strong>Warning:</strong> Memory leak detected!\n");
            html.append("      </div>\n");
        }
        if (memoryHistory.isEmpty()) {
            html.append("      <p>No memory leak data available yet.</p>\n");
        } else {
            int startIndex;
            html.append("      <table>\n");
            html.append("        <tr>\n");
            html.append("          <th>Time</th>\n");
            html.append("          <th>Used Memory</th>\n");
            html.append("          <th>Max Memory</th>\n");
            html.append("          <th>Usage %</th>\n");
            html.append("        </tr>\n");
            sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            for (int i = startIndex = Math.max(0, memoryHistory.size() - 10); i < memoryHistory.size(); ++i) {
                MemoryLeakDetector.MemorySnapshot snapshot = memoryHistory.get(i);
                double usagePercent = (double)snapshot.getUsedMemory() / (double)snapshot.getMaxMemory() * 100.0;
                String usageClass = usagePercent > 80.0 ? "status-error" : (usagePercent > 60.0 ? "status-warning" : "status-ok");
                html.append("        <tr>\n");
                html.append("          <td>" + ((DateFormat)sdf).format(new Date(snapshot.getTimestamp())) + "</td>\n");
                html.append("          <td>" + this.formatSize(snapshot.getUsedMemory()) + "</td>\n");
                html.append("          <td>" + this.formatSize(snapshot.getMaxMemory()) + "</td>\n");
                html.append("          <td class=\"" + usageClass + "\">" + String.format("%.1f%%", usagePercent) + "</td>\n");
                html.append("        </tr>\n");
            }
            html.append("      </table>\n");
        }
        html.append("    </div>\n");
        html.append("    <div class=\"card worlds\">\n");
        html.append("      <h2>World Status</h2>\n");
        html.append("      <table>\n");
        html.append("        <tr>\n");
        html.append("          <th>World</th>\n");
        html.append("          <th>Entities</th>\n");
        html.append("          <th>Chunks</th>\n");
        html.append("          <th>Players</th>\n");
        html.append("          <th>Status</th>\n");
        html.append("        </tr>\n");
        if (latest != null) {
            for (World world : Bukkit.getWorlds()) {
                UUID worldId = world.getUID();
                WorldStats stats = latest.worldStats.get(worldId);
                LagPredictionResult worldResult = this.lastPredictionByWorld.get(worldId);
                String worldLagStatus = "status-ok";
                String worldStatusText = "Good";
                if (worldResult != null && worldResult.predicted) {
                    worldLagStatus = worldResult.confidence > 0.7 ? "status-error" : "status-warning";
                    worldStatusText = worldResult.cause;
                }
                html.append("        <tr>\n");
                html.append("          <td>" + world.getName() + "</td>\n");
                html.append("          <td>" + String.valueOf(stats != null ? Integer.valueOf(stats.entityCount) : "N/A") + "</td>\n");
                html.append("          <td>" + String.valueOf(stats != null ? Integer.valueOf(stats.chunkCount) : "N/A") + "</td>\n");
                html.append("          <td>" + String.valueOf(stats != null ? Integer.valueOf(stats.playerCount) : "N/A") + "</td>\n");
                html.append("          <td class=\"" + worldLagStatus + "\">" + worldStatusText + "</td>\n");
                html.append("        </tr>\n");
            }
        } else {
            html.append("        <tr><td colspan=\"5\">No world data available</td></tr>\n");
        }
        html.append("      </table>\n");
        html.append("    </div>\n");
        if (!this.predictionHistory.isEmpty()) {
            html.append("    <div class=\"card\">\n");
            html.append("      <h2>Recent Prediction History</h2>\n");
            html.append("      <table>\n");
            html.append("        <tr>\n");
            html.append("          <th>Time</th>\n");
            html.append("          <th>Cause</th>\n");
            html.append("          <th>Confidence</th>\n");
            html.append("          <th>TPS</th>\n");
            html.append("        </tr>\n");
            sdf = new SimpleDateFormat("HH:mm:ss");
            ArrayList<PredictionEvent> recentEvents = new ArrayList<PredictionEvent>(this.predictionHistory);
            int endIndex = Math.min(recentEvents.size(), 10);
            for (int i = 0; i < endIndex; ++i) {
                PredictionEvent event = (PredictionEvent)recentEvents.get(recentEvents.size() - 1 - i);
                String eventClass = event.confidence > 0.7 ? "status-error" : "status-warning";
                html.append("        <tr>\n");
                html.append("          <td>" + ((DateFormat)sdf).format(new Date(event.timestamp)) + "</td>\n");
                html.append("          <td>" + event.cause + "</td>\n");
                html.append("          <td class=\"" + eventClass + "\">" + String.format("%.0f", event.confidence * 100.0) + "%</td>\n");
                html.append("          <td>" + String.format("%.1f", event.tps) + "</td>\n");
                html.append("        </tr>\n");
            }
            html.append("      </table>\n");
            html.append("    </div>\n");
        }
        html.append("<footer><p>MCOptimizer v").append(this.plugin.getDescription().getVersion()).append(" | Server: ").append(Bukkit.getName()).append(" ").append(Bukkit.getVersion()).append("</p><p id=\"update-time\">Last updated: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("</p></footer></div>");
        html.append("<script>");
        html.append("document.addEventListener('DOMContentLoaded',function(){");
        html.append("const connectionStatus=document.getElementById('connection-status');");
        html.append("const updateTime=document.getElementById('update-time');");
        html.append("let eventSource;");
        html.append("function connect(){");
        html.append("if(eventSource){eventSource.close();}");
        Object tokenParam = "";
        if (this.requireToken && this.webInterfaceAccessToken != null && !this.webInterfaceAccessToken.isEmpty()) {
            tokenParam = "?token=" + this.webInterfaceAccessToken;
        }
        html.append("eventSource=new EventSource('/api/stream").append((String)tokenParam).append("');");
        html.append("eventSource.addEventListener('connected',function(e){");
        html.append("connectionStatus.textContent='Connected - Live Updates';");
        html.append("connectionStatus.className='connection-status connected';");
        html.append("});");
        html.append("eventSource.addEventListener('status',function(e){");
        html.append("const data=JSON.parse(e.data);");
        html.append("updateStatusValues(data);");
        html.append("updateTime.textContent='Last updated: '+new Date().toLocaleString();");
        html.append("});");
        html.append("eventSource.onerror=function(){");
        html.append("connectionStatus.textContent='Disconnected - Trying to reconnect...';");
        html.append("connectionStatus.className='connection-status disconnected';");
        html.append("};");
        html.append("}");
        html.append("function updateStatusValues(data){");
        html.append("if(data.performance&&data.performance.tps!==undefined){");
        html.append("const tps=data.performance.tps;");
        html.append("const tpsEl=document.querySelector('.metric:nth-child(1) .value');");
        html.append("if(tpsEl){");
        html.append("tpsEl.textContent=tps.toFixed(1);");
        html.append("tpsEl.className='value '+(tps<15.0?'status-error':(tps<18.0?'status-warning':'status-ok'));");
        html.append("}");
        html.append("}");
        html.append("if(data.performance){");
        html.append("const memEl=document.querySelector('.metric:nth-child(2) .value');");
        html.append("const memFreeEl=document.querySelector('.metric:nth-child(2) div:nth-child(4)');");
        html.append("if(memEl&&memFreeEl){");
        html.append("const usedMB=Math.floor(data.performance.memory_used/1024/1024);");
        html.append("const totalMB=Math.floor((data.performance.memory_used+data.performance.memory_free)/1024/1024);");
        html.append("const freeMB=Math.floor(data.performance.memory_free/1024/1024);");
        html.append("memEl.textContent=usedMB+' MB / '+totalMB+' MB';");
        html.append("memFreeEl.textContent='Free: '+freeMB+' MB';");
        html.append("const threshold=").append(this.memoryThreshold).append(";");
        html.append("memEl.className='value '+(freeMB<threshold/2?'status-error':(freeMB<threshold?'status-warning':'status-ok'));");
        html.append("}");
        html.append("}");
        html.append("if(data.performance&&data.performance.cpu_load!==undefined){");
        html.append("const cpuEl=document.querySelector('.metric:nth-child(3) .value');");
        html.append("if(cpuEl){");
        html.append("const cpuPercent=(data.performance.cpu_load*100).toFixed(1);");
        html.append("cpuEl.textContent=cpuPercent+'%';");
        html.append("cpuEl.className='value '+(data.performance.cpu_load>").append(this.cpuThreshold).append("?'status-error':(data.performance.cpu_load>").append(this.cpuThreshold * 0.8).append("?'status-warning':'status-ok'));");
        html.append("}");
        html.append("}");
        html.append("if(data.performance && data.performance.server_health !== undefined){");
        html.append("updateEmotionWheel(data.performance.server_health);");
        html.append("}");
        html.append("function updateEmotionWheel(healthValue){");
        html.append("const faces = {");
        html.append("'-1': {face: 'face-unknown', label: 'Unknown', description: 'Server status information unavailable.'},");
        html.append("'0': {face: 'face-0', label: 'Critical', description: 'Server performance is critical! Immediate action required.'},");
        html.append("'1': {face: 'face-1', label: 'Stressed', description: 'Server is experiencing significant stress. Action recommended.'},");
        html.append("'2': {face: 'face-2', label: 'Concerned', description: 'Some performance issues detected. Consider optimization.'},");
        html.append("'3': {face: 'face-3', label: 'Neutral', description: 'Your server is stable but could use some optimization.'},");
        html.append("'4': {face: 'face-4', label: 'Good', description: 'Your server is running well with no issues.'},");
        html.append("'5': {face: 'face-5', label: 'Excellent', description: 'Your server is running optimally!'}");
        html.append("};");
        html.append("const faceInfo = faces[healthValue] || faces['-1'];");
        html.append("const wheel = document.querySelector('.emotion-wheel');");
        html.append("const currentActive = document.querySelector('.emotion-face.active');");
        html.append("const nextActive = document.getElementById(faceInfo.face);");
        html.append("if (currentActive && currentActive.id === faceInfo.face) return;");
        html.append("if (wheel && currentActive && nextActive) {");
        html.append("  // Spin out current face");
        html.append("  wheel.style.transform = 'scale(0.8) rotate(-15deg)';");
        html.append("  currentActive.style.opacity = '0';");
        html.append("  setTimeout(() => {");
        html.append("    // Remove active class from all faces");
        html.append("    document.querySelectorAll('.emotion-face').forEach(face => face.classList.remove('active'));");
        html.append("    // Set new active face");
        html.append("    nextActive.classList.add('active');");
        html.append("    // Spin in new face");
        html.append("    setTimeout(() => {");
        html.append("      wheel.style.transform = 'scale(1) rotate(0deg)';");
        html.append("      nextActive.style.opacity = '1';");
        html.append("    }, 50);");
        html.append("  }, 300);");
        html.append("} else {");
        html.append("  // Fallback if animation can't work");
        html.append("  document.querySelectorAll('.emotion-face').forEach(face => face.classList.remove('active'));");
        html.append("  nextActive.classList.add('active');");
        html.append("}");
        html.append("document.getElementById('health-status-label').textContent = faceInfo.label;");
        html.append("document.getElementById('health-status-description').textContent = faceInfo.description;");
        html.append("}");
        html.append("if(data.prediction){");
        html.append("const predEl=document.querySelector('.card:nth-child(3) .metric .value');");
        html.append("if(predEl){");
        html.append("predEl.textContent=data.prediction.lag_predicted?'Lag Predicted':'No Lag Predicted';");
        html.append("predEl.className='value '+(data.prediction.lag_predicted?(data.prediction.confidence>0.8?'status-error':'status-warning'):'status-ok');");
        html.append("}");
        html.append("}");
        html.append("}");
        html.append("if('EventSource' in window){connect();}else{");
        html.append("connectionStatus.textContent='Real-time updates not supported in this browser';");
        html.append("}");
        html.append("});");
        html.append("</script></body></html>");
        return html.toString();
    }

    private String generateStatusJson() {
        StringBuilder json = new StringBuilder(2048);
        json.append("{");
        json.append("\"server\":{");
        json.append("\"name\":\"").append(this.escapeJson(Bukkit.getName())).append("\",");
        json.append("\"version\":\"").append(this.escapeJson(Bukkit.getVersion())).append("\",");
        json.append("\"plugin_version\":\"").append(this.escapeJson(this.plugin.getDescription().getVersion())).append("\",");
        json.append("\"online_players\":").append(Bukkit.getOnlinePlayers().size());
        json.append("},");
        PerformanceDataPoint latest = this.getLatestDataPoint();
        json.append("\"performance\":{");
        if (latest != null) {
            int serverHealth = this.calculateServerHealth(latest);
            json.append("\"tps\":").append(String.format("%.2f", latest.tps)).append(",");
            json.append("\"cpu_load\":").append(String.format("%.4f", latest.cpuLoad)).append(",");
            json.append("\"memory_used\":").append(latest.memoryUsed).append(",");
            json.append("\"memory_free\":").append(latest.memoryFree).append(",");
            json.append("\"memory_total\":").append(latest.memoryUsed + latest.memoryFree).append(",");
            json.append("\"server_health\":").append(serverHealth);
        } else {
            json.append("\"tps\":0,");
            json.append("\"cpu_load\":0,");
            json.append("\"memory_used\":0,");
            json.append("\"memory_free\":0,");
            json.append("\"memory_total\":0,");
            json.append("\"server_health\":-1");
        }
        json.append("},");
        json.append("\"prediction\":{");
        json.append("\"lag_predicted\":").append(this.lagPredicted).append(",");
        json.append("\"confidence\":").append(String.format("%.4f", this.predictionConfidence)).append(",");
        json.append("\"cause\":");
        if (this.predictedCause != null) {
            json.append("\"").append(this.escapeJson(this.predictedCause)).append("\"");
        } else {
            json.append("null");
        }
        json.append(",");
        json.append("\"recommendation\":");
        if (this.recommendedAction != null) {
            json.append("\"").append(this.escapeJson(this.recommendedAction)).append("\"");
        } else {
            json.append("null");
        }
        json.append(",");
        json.append("\"prediction_time\":").append(this.lastPredictionTime);
        json.append("},");
        json.append("\"worlds\":[");
        if (latest != null) {
            List worlds = Bukkit.getWorlds();
            for (int i = 0; i < worlds.size(); ++i) {
                World world = (World)worlds.get(i);
                UUID worldId = world.getUID();
                WorldStats stats = latest.worldStats.get(worldId);
                LagPredictionResult worldResult = this.lastPredictionByWorld.get(worldId);
                if (i > 0) {
                    json.append(",");
                }
                json.append("{");
                json.append("\"name\":\"").append(this.escapeJson(world.getName())).append("\",");
                json.append("\"environment\":\"").append(world.getEnvironment().name()).append("\",");
                if (stats != null) {
                    json.append("\"entity_count\":").append(stats.entityCount).append(",");
                    json.append("\"tile_entity_count\":").append(stats.tileEntityCount).append(",");
                    json.append("\"chunk_count\":").append(stats.chunkCount).append(",");
                    json.append("\"player_count\":").append(stats.playerCount).append(",");
                } else {
                    json.append("\"entity_count\":0,");
                    json.append("\"tile_entity_count\":0,");
                    json.append("\"chunk_count\":0,");
                    json.append("\"player_count\":0,");
                }
                if (worldResult != null && worldResult.predicted) {
                    json.append("\"lag_predicted\":true,");
                    json.append("\"prediction_confidence\":").append(String.format("%.4f", worldResult.confidence)).append(",");
                    json.append("\"prediction_cause\":\"").append(this.escapeJson(worldResult.cause)).append("\"");
                } else {
                    json.append("\"lag_predicted\":false,");
                    json.append("\"prediction_confidence\":0,");
                    json.append("\"prediction_cause\":null");
                }
                json.append("}");
            }
        }
        json.append("]");
        json.append("}");
        return json.toString();
    }

    private String generateHistoryJson() {
        StringBuilder json = new StringBuilder(2048);
        json.append("{\"history\":[");
        for (int i = 0; i < this.predictionHistory.size(); ++i) {
            PredictionEvent event = this.predictionHistory.get(i);
            if (i > 0) {
                json.append(",");
            }
            json.append("{");
            json.append("\"timestamp\":").append(event.timestamp).append(",");
            json.append("\"lag_predicted\":true,");
            json.append("\"confidence\":").append(String.format("%.4f", event.confidence)).append(",");
            json.append("\"cause\":\"").append(this.escapeJson(event.cause)).append("\",");
            json.append("\"tps\":").append(String.format("%.2f", event.tps));
            json.append("}");
        }
        json.append("]}");
        return json.toString();
    }

    private String generateErrorPage(String title, String message) {
        StringBuilder html = new StringBuilder(1024);
        html.append("<!DOCTYPE html><html lang=\"en\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1.0\"><meta http-equiv=\"Cache-Control\" content=\"max-age=60\"><title>").append(title).append(" - MCOptimizer</title><style>").append("body{font-family:Arial,sans-serif;line-height:1.6;margin:0;padding:20px;color:#333;background-color:#f8f8f8;text-align:center}").append(".container{max-width:600px;margin:50px auto;background:#fff;padding:20px;border-radius:5px;box-shadow:0 2px 5px rgba(0,0,0,.1)}").append("h1{color:#e74c3c}p{margin-bottom:20px}a{color:#3498db;text-decoration:none}a:hover{text-decoration:underline}").append("</style></head><body><div class=\"container\"><h1>").append(title).append("</h1><p>").append(message).append("</p><p><a href=\"/\">Return to Home</a></p></div></body></html>");
        return html.toString();
    }

    private String getStatusMessage(int statusCode) {
        switch (statusCode) {
            case 200: {
                return "OK";
            }
            case 400: {
                return "Bad Request";
            }
            case 401: {
                return "Unauthorized";
            }
            case 403: {
                return "Forbidden";
            }
            case 404: {
                return "Not Found";
            }
            case 405: {
                return "Method Not Allowed";
            }
            case 500: {
                return "Internal Server Error";
            }
        }
        return "Unknown";
    }

    private String escapeJson(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void handleSSERequest(Socket socket, Map<String, String> headers) {
        try {
            if (this.requireToken) {
                String auth;
                String token = null;
                if (headers.containsKey("authorization") && (auth = headers.get("authorization")).startsWith("Bearer ")) {
                    token = auth.substring(7).trim();
                }
                if (token == null || !token.equals(this.webInterfaceAccessToken)) {
                    OutputStream output = socket.getOutputStream();
                    String response = "HTTP/1.1 401 Unauthorized\r\nContent-Type: text/plain\r\nConnection: close\r\n\r\nAuthentication required";
                    output.write(response.getBytes(StandardCharsets.UTF_8));
                    output.flush();
                    socket.close();
                    return;
                }
            }
            OutputStream output = socket.getOutputStream();
            String headerString = "HTTP/1.1 200 OK\r\nContent-Type: text/event-stream\r\nCache-Control: no-cache\r\nConnection: keep-alive\r\nAccess-Control-Allow-Origin: *\r\n\r\n";
            output.write(headerString.getBytes(StandardCharsets.UTF_8));
            output.flush();
            String clientId = UUID.randomUUID().toString();
            this.sendSSEEvent(output, "connected", "{\"clientId\":\"" + clientId + "\"}");
            this.sendSSEEvent(output, "status", this.generateStatusJson());
            Map<String, Socket> map = this.sseClients;
            synchronized (map) {
                this.sseClients.put(clientId, socket);
            }
            try {
                while (this.plugin.isEnabled() && !socket.isClosed() && socket.isConnected()) {
                    this.sendSSEComment(output, "keep-alive");
                    try {
                        Thread.sleep(30000L);
                    }
                    catch (InterruptedException e) {
                        // empty catch block
                        break;
                    }
                }
            }
            finally {
                map = this.sseClients;
                synchronized (map) {
                    this.sseClients.remove(clientId);
                }
                try {
                    socket.close();
                }
                catch (IOException iOException) {}
            }
        }
        catch (IOException e) {
            this.logger.warning("Error handling SSE request: " + e.getMessage());
            try {
                socket.close();
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
    }

    private void sendSSEEvent(OutputStream output, String eventName, String data) throws IOException {
        StringBuilder event = new StringBuilder();
        event.append("event: ").append(eventName).append("\n");
        event.append("data: ").append(data).append("\n\n");
        output.write(event.toString().getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    private void sendSSEComment(OutputStream output, String comment) throws IOException {
        String commentStr = ": " + comment + "\n\n";
        output.write(commentStr.getBytes(StandardCharsets.UTF_8));
        output.flush();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void broadcastStatusUpdate() {
        if (this.sseClients.isEmpty()) {
            return;
        }
        String statusJson = this.generateStatusJson();
        Map<String, Socket> map = this.sseClients;
        synchronized (map) {
            Iterator<Map.Entry<String, Socket>> it = this.sseClients.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Socket> entry = it.next();
                Socket socket = entry.getValue();
                try {
                    if (socket.isConnected() && !socket.isClosed()) {
                        this.sendSSEEvent(socket.getOutputStream(), "status", statusJson);
                        continue;
                    }
                    it.remove();
                }
                catch (IOException e) {
                    it.remove();
                    try {
                        socket.close();
                    }
                    catch (IOException iOException) {}
                }
            }
        }
    }

    private String formatSize(long bytes) {
        long KB = 1024L;
        long MB = 0x100000L;
        long GB = 0x40000000L;
        if (bytes >= 0x40000000L) {
            return String.format("%.2f GB", (double)bytes / 1.073741824E9);
        }
        if (bytes >= 0x100000L) {
            return String.format("%.2f MB", (double)bytes / 1048576.0);
        }
        if (bytes >= 1024L) {
            return String.format("%.2f KB", (double)bytes / 1024.0);
        }
        return bytes + " bytes";
    }

    private int calculateServerHealth(PerformanceDataPoint data) {
        if (data == null) {
            return -1;
        }
        int health = 5;
        if (data.tps < 10.0) {
            return 0;
        }
        if (data.tps < 15.0) {
            health = Math.min(health, 1);
        } else if (data.tps < 18.0) {
            health = Math.min(health, 2);
        } else if (data.tps < 19.5) {
            health = Math.min(health, 3);
        }
        long freeMemory = data.memoryFree / 1024L / 1024L;
        double memoryUsagePercent = (double)data.memoryUsed / (double)(data.memoryUsed + data.memoryFree);
        if (freeMemory < this.memoryThreshold / 4L) {
            health = Math.min(health, 0);
        } else if (freeMemory < this.memoryThreshold / 2L) {
            health = Math.min(health, 1);
        } else if (freeMemory < this.memoryThreshold) {
            health = Math.min(health, 2);
        } else if (memoryUsagePercent > 0.8) {
            health = Math.min(health, 3);
        } else if (memoryUsagePercent > 0.7) {
            health = Math.min(health, 4);
        }
        if (data.cpuLoad > this.cpuThreshold * 1.5) {
            health = Math.min(health, 0);
        } else if (data.cpuLoad > this.cpuThreshold * 1.2) {
            health = Math.min(health, 1);
        } else if (data.cpuLoad > this.cpuThreshold) {
            health = Math.min(health, 2);
        } else if (data.cpuLoad > this.cpuThreshold * 0.8) {
            health = Math.min(health, 3);
        } else if (data.cpuLoad > this.cpuThreshold * 0.6) {
            health = Math.min(health, 4);
        }
        if (this.lagPredicted && this.predictionConfidence > 0.8) {
            health = Math.min(health, 1);
        } else if (this.lagPredicted && this.predictionConfidence > 0.5) {
            health = Math.min(health, 2);
        }
        MemoryLeakDetector leakDetector = this.plugin.getMemoryLeakDetector();
        if (leakDetector != null && leakDetector.isLeakDetected()) {
            health = Math.min(health, 1);
        }
        return health;
    }

    private void collectData() {
        if (this.performanceMonitor == null) {
            return;
        }
        PerformanceDataPoint dataPoint = new PerformanceDataPoint();
        dataPoint.timestamp = System.currentTimeMillis();
        PerformanceMonitor.PerformanceSnapshot latestSnapshot = this.performanceMonitor.getLatestSnapshot();
        if (latestSnapshot != null) {
            dataPoint.tps = latestSnapshot.tps;
            dataPoint.cpuLoad = Math.min(1.0, Math.max(0.0, (20.0 - latestSnapshot.tps) / 10.0));
            dataPoint.memoryUsed = latestSnapshot.usedMemory * 1024L * 1024L;
            dataPoint.memoryFree = (latestSnapshot.maxMemory - latestSnapshot.usedMemory) * 1024L * 1024L;
        } else {
            dataPoint.tps = 20.0;
            dataPoint.cpuLoad = 0.1;
            dataPoint.memoryUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            dataPoint.memoryFree = Runtime.getRuntime().freeMemory();
        }
        dataPoint.playersOnline = Bukkit.getOnlinePlayers().size();
        HashMap<UUID, WorldStats> worldData = new HashMap<UUID, WorldStats>();
        for (World world : Bukkit.getWorlds()) {
            WorldStats stats = new WorldStats();
            stats.entityCount = world.getEntities().size();
            stats.chunkCount = world.getLoadedChunks().length;
            stats.playerCount = world.getPlayers().size();
            stats.tileEntityCount = this.countTileEntities(world);
            worldData.put(world.getUID(), stats);
        }
        dataPoint.worldStats = worldData;
        this.historicalData.add(dataPoint);
        while (this.historicalData.size() > this.dataPointsToKeep) {
            this.historicalData.remove();
        }
        if (this.predictionCounter % 10 == 0) {
            this.logger.fine("Collected performance data - TPS: " + String.format("%.2f", dataPoint.tps) + ", CPU: " + String.format("%.2f", dataPoint.cpuLoad * 100.0) + "%, Memory: " + dataPoint.memoryUsed / 1024L / 1024L + "MB used");
            if (!this.sseClients.isEmpty()) {
                this.broadcastStatusUpdate();
            }
        }
    }

    private void predictLag() {
        if (this.historicalData.size() < 10) {
            return;
        }
        try {
            this.lagPredicted = false;
            this.predictedCause = null;
            this.predictionConfidence = 0.0;
            this.recommendedAction = null;
            PerformanceDataPoint latest = this.getLatestDataPoint();
            if (latest == null) {
                return;
            }
            double tpsDownwardTrend = this.calculateTpsTrend();
            double memoryUptakeTrend = this.calculateMemoryUptakeTrend();
            double cpuLoadTrend = this.calculateCpuLoadTrend();
            ArrayList<LagIndicator> indicators = new ArrayList<LagIndicator>();
            if (latest.tps < this.predictionThreshold) {
                indicators.add(new LagIndicator("Low TPS", 0.8, "Server is already experiencing lag"));
            } else if (tpsDownwardTrend > 0.5) {
                indicators.add(new LagIndicator("TPS Decline", 0.6, "TPS is trending downward"));
            }
            if (latest.memoryFree < this.memoryThreshold * 1024L * 1024L) {
                indicators.add(new LagIndicator("Low Memory", 0.7, "Server is running low on memory"));
            } else if (memoryUptakeTrend > 0.4) {
                indicators.add(new LagIndicator("Memory Uptake", 0.5, "Memory usage is increasing rapidly"));
            }
            if (latest.cpuLoad > this.cpuThreshold) {
                indicators.add(new LagIndicator("High CPU", 0.75, "CPU usage is high"));
            } else if (cpuLoadTrend > 0.3) {
                indicators.add(new LagIndicator("CPU Trend", 0.4, "CPU usage is trending upward"));
            }
            HashMap<UUID, LagPredictionResult> worldPredictions = new HashMap<UUID, LagPredictionResult>();
            for (World world : Bukkit.getWorlds()) {
                UUID worldId = world.getUID();
                double entityGrowthTrend = this.calculateEntityGrowthTrend(worldId);
                double chunkGrowthTrend = this.calculateChunkGrowthTrend(worldId);
                ArrayList<LagIndicator> worldIndicators = new ArrayList<LagIndicator>();
                WorldStats stats = latest.worldStats.get(worldId);
                if (stats != null) {
                    if (stats.entityCount > 3000) {
                        worldIndicators.add(new LagIndicator("Entity Count", 0.6, "World has " + stats.entityCount + " entities"));
                    }
                    if (stats.tileEntityCount > 5000) {
                        worldIndicators.add(new LagIndicator("Tile Entity Count", 0.65, "World has " + stats.tileEntityCount + " tile entities"));
                    }
                    if (stats.chunkCount > 2500) {
                        worldIndicators.add(new LagIndicator("Chunk Count", 0.5, "World has " + stats.chunkCount + " chunks loaded"));
                    }
                    if (entityGrowthTrend > 0.4) {
                        worldIndicators.add(new LagIndicator("Entity Growth", 0.55, "Entity count is increasing rapidly"));
                    }
                    if (chunkGrowthTrend > 0.3) {
                        worldIndicators.add(new LagIndicator("Chunk Growth", 0.45, "Chunk loading is increasing rapidly"));
                    }
                }
                if (worldIndicators.isEmpty()) continue;
                LagPredictionResult worldResult = this.analyzePredictionIndicators(worldIndicators);
                worldResult.worldName = world.getName();
                worldPredictions.put(worldId, worldResult);
                if (!(worldResult.confidence > 0.65)) continue;
                indicators.add(new LagIndicator("World: " + world.getName(), worldResult.confidence, worldResult.cause));
            }
            this.lastPredictionByWorld.clear();
            this.lastPredictionByWorld.putAll(worldPredictions);
            LagPredictionResult result = this.analyzePredictionIndicators(indicators);
            this.lagPredicted = result.predicted;
            this.predictedCause = result.cause;
            this.predictionConfidence = result.confidence;
            this.recommendedAction = result.recommendation;
            if (this.lagPredicted && this.predictionConfidence > 0.7) {
                this.lastPredictionTime = System.currentTimeMillis();
                PredictionEvent event = new PredictionEvent();
                event.timestamp = this.lastPredictionTime;
                event.cause = this.predictedCause;
                event.confidence = this.predictionConfidence;
                event.tps = latest.tps;
                this.predictionHistory.add(event);
                while (this.predictionHistory.size() > 50) {
                    this.predictionHistory.remove(0);
                }
                this.logger.warning("Lag predicted! Cause: " + this.predictedCause + " (Confidence: " + String.format("%.1f", this.predictionConfidence * 100.0) + "%)");
                this.logger.warning("Recommended action: " + this.recommendedAction);
                if (this.notifyAdmins) {
                    this.notifyAdmins();
                }
                if (this.autoTuneSettings) {
                    this.applyAutomaticTuning();
                }
            }
            if (!this.sseClients.isEmpty()) {
                this.broadcastStatusUpdate();
            }
        }
        catch (Exception e) {
            this.logger.warning("Error during lag prediction: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void notifyAdmins() {
        String message = String.valueOf(ChatColor.RED) + "[MCOptimizer] " + String.valueOf(ChatColor.GOLD) + "Lag prediction: " + String.valueOf(ChatColor.WHITE) + this.predictedCause + String.valueOf(ChatColor.GOLD) + " (Confidence: " + String.format("%.0f", this.predictionConfidence * 100.0) + "%)";
        String actionMessage = String.valueOf(ChatColor.RED) + "[MCOptimizer] " + String.valueOf(ChatColor.GOLD) + "Recommended: " + String.valueOf(ChatColor.WHITE) + this.recommendedAction;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("mcoptimizer.admin") && !player.isOp()) continue;
            player.sendMessage(message);
            player.sendMessage(actionMessage);
        }
        Bukkit.getConsoleSender().sendMessage(message);
        Bukkit.getConsoleSender().sendMessage(actionMessage);
    }

    private void applyAutomaticTuning() {
        if (this.predictedCause == null) {
            return;
        }
        this.logger.info("Applying automatic tuning for: " + this.predictedCause);
        if (this.predictedCause.contains("Memory")) {
            System.gc();
            this.logger.info("Requested garbage collection to free memory");
        } else if (this.predictedCause.contains("Entity")) {
            for (World world : Bukkit.getWorlds()) {
                LagPredictionResult worldResult = this.lastPredictionByWorld.get(world.getUID());
                if (worldResult == null || !worldResult.predicted || !worldResult.cause.contains("Entity")) continue;
                this.logger.info("Applying stricter entity limits for world: " + world.getName());
            }
        } else if (this.predictedCause.contains("Chunk")) {
            this.logger.info("Optimizing chunk loading and unloading");
        }
    }

    private LagPredictionResult analyzePredictionIndicators(List<LagIndicator> indicators) {
        LagPredictionResult result = new LagPredictionResult();
        if (indicators.isEmpty()) {
            result.predicted = false;
            result.confidence = 0.0;
            result.cause = "No lag indicators detected";
            result.recommendation = "No action needed";
            return result;
        }
        LagIndicator strongest = null;
        double maxConfidence = 0.0;
        for (LagIndicator indicator : indicators) {
            if (!(indicator.confidence > maxConfidence)) continue;
            maxConfidence = indicator.confidence;
            strongest = indicator;
        }
        double totalConfidence = 0.0;
        for (LagIndicator indicator : indicators) {
            totalConfidence += indicator.confidence;
        }
        double weightedConfidence = 0.0;
        if (!indicators.isEmpty()) {
            weightedConfidence = strongest.confidence * 0.6 + totalConfidence / (double)indicators.size() * 0.4;
        }
        result.predicted = weightedConfidence > 0.6;
        result.confidence = weightedConfidence;
        result.cause = strongest != null ? strongest.description : "Unknown";
        result.recommendation = this.generateRecommendation(strongest, indicators);
        return result;
    }

    private String generateRecommendation(LagIndicator strongest, List<LagIndicator> indicators) {
        if (strongest == null) {
            return "No action needed";
        }
        String cause = strongest.description.toLowerCase();
        if (cause.contains("tps")) {
            return "Check server tasks and plugins using '/mcopt status' command";
        }
        if (cause.contains("memory")) {
            return "Increase server memory allocation or reduce view distance";
        }
        if (cause.contains("cpu")) {
            return "Reduce server load by optimizing redstone or scheduled tasks";
        }
        if (cause.contains("entity")) {
            return "Consider reducing mob spawn limits or clearing entities";
        }
        if (cause.contains("tile entity")) {
            return "Look for tile entity concentrations (hoppers, chests)";
        }
        if (cause.contains("chunk")) {
            return "Adjust view distance or pregenerate chunks";
        }
        if (cause.contains("world")) {
            int colonIndex = strongest.name.indexOf(":");
            if (colonIndex > 0) {
                String worldName = strongest.name.substring(colonIndex + 1).trim();
                return "Optimize world '" + worldName + "' - check for entity or chunk issues";
            }
            return "Check world-specific settings and entity counts";
        }
        return "Monitor server performance and consider reducing load";
    }

    private PerformanceDataPoint getLatestDataPoint() {
        if (this.historicalData.isEmpty()) {
            return null;
        }
        return new ArrayList<PerformanceDataPoint>(this.historicalData).get(this.historicalData.size() - 1);
    }

    private double calculateTpsTrend() {
        if (this.historicalData.size() < 5) {
            return 0.0;
        }
        ArrayList<PerformanceDataPoint> points = new ArrayList<PerformanceDataPoint>(this.historicalData);
        double initialTps = ((PerformanceDataPoint)points.get((int)0)).tps;
        double latestTps = ((PerformanceDataPoint)points.get((int)(points.size() - 1))).tps;
        double diff = initialTps - latestTps;
        double normalizedTrend = Math.min(1.0, Math.max(0.0, diff / 10.0));
        double weightedDiff = 0.0;
        double totalWeight = 0.0;
        for (int i = 0; i < points.size() - 1; ++i) {
            double weight = 0.5 + 0.5 * (double)i / (double)(points.size() - 1);
            double pointDiff = ((PerformanceDataPoint)points.get((int)i)).tps - ((PerformanceDataPoint)points.get((int)(i + 1))).tps;
            weightedDiff += pointDiff * weight;
            totalWeight += weight;
        }
        double weightedTrend = totalWeight > 0.0 ? Math.min(1.0, Math.max(0.0, weightedDiff / totalWeight / 2.0)) : 0.0;
        return normalizedTrend * 0.3 + weightedTrend * 0.7;
    }

    private double calculateMemoryUptakeTrend() {
        if (this.historicalData.size() < 5) {
            return 0.0;
        }
        ArrayList<PerformanceDataPoint> points = new ArrayList<PerformanceDataPoint>(this.historicalData);
        long initialMemory = ((PerformanceDataPoint)points.get((int)0)).memoryUsed;
        long latestMemory = ((PerformanceDataPoint)points.get((int)(points.size() - 1))).memoryUsed;
        long totalMemory = latestMemory + ((PerformanceDataPoint)points.get((int)(points.size() - 1))).memoryFree;
        double memoryIncrease = latestMemory - initialMemory;
        double normalizedTrend = Math.min(1.0, Math.max(0.0, memoryIncrease / ((double)totalMemory * 0.3)));
        return normalizedTrend;
    }

    private double calculateCpuLoadTrend() {
        if (this.historicalData.size() < 5) {
            return 0.0;
        }
        ArrayList<PerformanceDataPoint> points = new ArrayList<PerformanceDataPoint>(this.historicalData);
        double initialLoad = ((PerformanceDataPoint)points.get((int)0)).cpuLoad;
        double latestLoad = ((PerformanceDataPoint)points.get((int)(points.size() - 1))).cpuLoad;
        double loadIncrease = latestLoad - initialLoad;
        double normalizedTrend = Math.min(1.0, Math.max(0.0, loadIncrease / 0.5));
        return normalizedTrend;
    }

    private double calculateEntityGrowthTrend(UUID worldId) {
        if (this.historicalData.size() < 5) {
            return 0.0;
        }
        ArrayList<PerformanceDataPoint> points = new ArrayList<PerformanceDataPoint>(this.historicalData);
        WorldStats initialStats = null;
        for (PerformanceDataPoint point : points) {
            if (!point.worldStats.containsKey(worldId)) continue;
            initialStats = point.worldStats.get(worldId);
            break;
        }
        WorldStats latestStats = null;
        for (int i = points.size() - 1; i >= 0; --i) {
            PerformanceDataPoint point = (PerformanceDataPoint)points.get(i);
            if (!point.worldStats.containsKey(worldId)) continue;
            latestStats = point.worldStats.get(worldId);
            break;
        }
        if (initialStats == null || latestStats == null) {
            return 0.0;
        }
        int initialCount = initialStats.entityCount;
        int latestCount = latestStats.entityCount;
        int increase = latestCount - initialCount;
        double normalizedTrend = Math.min(1.0, Math.max(0.0, (double)increase / 1000.0));
        return normalizedTrend;
    }

    private double calculateChunkGrowthTrend(UUID worldId) {
        if (this.historicalData.size() < 5) {
            return 0.0;
        }
        ArrayList<PerformanceDataPoint> points = new ArrayList<PerformanceDataPoint>(this.historicalData);
        WorldStats initialStats = null;
        for (PerformanceDataPoint point : points) {
            if (!point.worldStats.containsKey(worldId)) continue;
            initialStats = point.worldStats.get(worldId);
            break;
        }
        WorldStats latestStats = null;
        for (int i = points.size() - 1; i >= 0; --i) {
            PerformanceDataPoint point = (PerformanceDataPoint)points.get(i);
            if (!point.worldStats.containsKey(worldId)) continue;
            latestStats = point.worldStats.get(worldId);
            break;
        }
        if (initialStats == null || latestStats == null) {
            return 0.0;
        }
        int initialCount = initialStats.chunkCount;
        int latestCount = latestStats.chunkCount;
        int increase = latestCount - initialCount;
        double normalizedTrend = Math.min(1.0, Math.max(0.0, (double)increase / 500.0));
        return normalizedTrend;
    }

    private int countTileEntities(World world) {
        int estimate = 0;
        int sampledChunks = 0;
        int maxSamples = 50;
        for (Chunk chunk : world.getLoadedChunks()) {
            if (sampledChunks >= maxSamples) break;
            estimate += chunk.getTileEntities().length;
            ++sampledChunks;
        }
        if (sampledChunks > 0) {
            double avgPerChunk = (double)estimate / (double)sampledChunks;
            estimate = (int)(avgPerChunk * (double)world.getLoadedChunks().length);
        }
        return estimate;
    }

    public void shutdown() {
        if (this.predictionTask != null) {
            this.predictionTask.cancel();
            this.predictionTask = null;
        }
        if (this.webServer != null) {
            try {
                this.webServer.close();
            }
            catch (IOException e) {
                this.logger.warning("Error closing web server: " + e.getMessage());
            }
            this.webServer = null;
        }
        if (this.webExecutor != null) {
            this.webExecutor.shutdown();
            this.webExecutor = null;
        }
        this.logger.info("Lag prediction manager shutdown");
    }

    public boolean isLagPredicted() {
        return this.lagPredicted;
    }

    public String getPredictedCause() {
        return this.predictedCause;
    }

    public double getPredictionConfidence() {
        return this.predictionConfidence;
    }

    public String getRecommendedAction() {
        return this.recommendedAction;
    }

    public List<String> getPredictionStatus(CommandSender sender) {
        ArrayList<String> status = new ArrayList<String>();
        status.add(String.valueOf(ChatColor.GOLD) + "=== Lag Prediction Status ===");
        status.add(String.valueOf(ChatColor.YELLOW) + "Enabled: " + String.valueOf(ChatColor.WHITE) + this.enabled);
        status.add(String.valueOf(ChatColor.YELLOW) + "Data points: " + String.valueOf(ChatColor.WHITE) + this.historicalData.size() + "/" + this.dataPointsToKeep);
        if (this.webInterfaceEnabled) {
            status.add(String.valueOf(ChatColor.YELLOW) + "Web Interface: " + String.valueOf(ChatColor.WHITE) + "Enabled on port " + this.webInterfacePort);
            status.add(String.valueOf(ChatColor.YELLOW) + "Web URL: " + String.valueOf(ChatColor.WHITE) + "http://" + (this.webInterfaceBindAddress.equals("0.0.0.0") ? "localhost" : this.webInterfaceBindAddress) + ":" + this.webInterfacePort + "/");
        }
        if (this.lagPredicted) {
            status.add(String.valueOf(ChatColor.RED) + "Lag Predicted: " + String.valueOf(ChatColor.WHITE) + "Yes" + String.valueOf(ChatColor.GRAY) + " (" + String.format("%.0f", this.predictionConfidence * 100.0) + "% confidence)");
            status.add(String.valueOf(ChatColor.RED) + "Predicted Cause: " + String.valueOf(ChatColor.WHITE) + this.predictedCause);
            status.add(String.valueOf(ChatColor.RED) + "Recommended Action: " + String.valueOf(ChatColor.WHITE) + this.recommendedAction);
        } else {
            status.add(String.valueOf(ChatColor.GREEN) + "Lag Predicted: " + String.valueOf(ChatColor.WHITE) + "No");
        }
        status.add(String.valueOf(ChatColor.YELLOW) + "World Predictions:");
        boolean anyWorldPredictions = false;
        for (Map.Entry<UUID, LagPredictionResult> entry : this.lastPredictionByWorld.entrySet()) {
            UUID worldId = entry.getKey();
            LagPredictionResult result = entry.getValue();
            if (!result.predicted) continue;
            anyWorldPredictions = true;
            String worldName = result.worldName != null ? result.worldName : "Unknown";
            status.add(String.valueOf(ChatColor.RED) + "  - " + worldName + ": " + String.valueOf(ChatColor.WHITE) + result.cause + String.valueOf(ChatColor.GRAY) + " (" + String.format("%.0f", result.confidence * 100.0) + "%)");
        }
        if (!anyWorldPredictions) {
            status.add(String.valueOf(ChatColor.GREEN) + "  - No world-specific lag predicted");
        }
        return status;
    }

    public PerformanceDataPoint getMostRecentData() {
        return this.getLatestDataPoint();
    }

    public static class PerformanceDataPoint {
        long timestamp;
        double tps;
        double cpuLoad;
        long memoryUsed;
        long memoryFree;
        int playersOnline;
        Map<UUID, WorldStats> worldStats = new HashMap<UUID, WorldStats>();
    }

    public static class WorldStats {
        int entityCount;
        int tileEntityCount;
        int chunkCount;
        int playerCount;
    }

    public static class LagPredictionResult {
        boolean predicted;
        double confidence;
        String cause;
        String recommendation;
        String worldName;
    }

    private static class PredictionEvent {
        long timestamp;
        String cause;
        double confidence;
        double tps;

        private PredictionEvent() {
        }
    }

    private static class LagIndicator {
        String name;
        double confidence;
        String description;

        public LagIndicator(String name, double confidence, String description) {
            this.name = name;
            this.confidence = confidence;
            this.description = description;
        }
    }
}

