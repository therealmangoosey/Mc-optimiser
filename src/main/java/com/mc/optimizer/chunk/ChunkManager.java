package com.mc.optimizer.chunk;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.chunk.config.ChunkConfigManager;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.scheduler.BukkitTask;

/** Lightweight activity-based chunk manager. */
public final class ChunkManager implements Listener {
    private final OptimizerPlugin plugin;
    private final ChunkConfigManager config;
    private final Logger logger;
    private final Map<String, Long> lastAccessedChunks = new ConcurrentHashMap<>();
    private BukkitTask task;
    private int unloadAfterSeconds;
    private int chunksUnloaded;

    public ChunkManager(OptimizerPlugin plugin, ChunkConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.unloadAfterSeconds = Math.max(30, config.getUnloadAfterSeconds());
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 200L, 200L);
    }

    private void tick() {
        long now = System.currentTimeMillis() / 1000L;
        for (Player player : Bukkit.getOnlinePlayers()) markAccessed(player.getLocation().getChunk(), now);
        for (World world : Bukkit.getWorlds()) {
            for (Chunk chunk : world.getLoadedChunks()) {
                if (isPlayerChunk(chunk) || isSpawnChunk(chunk)) continue;
                String key = getChunkKey(chunk);
                long lastAccess = lastAccessedChunks.getOrDefault(key, now);
                if (now - lastAccess <= unloadAfterSeconds) continue;
                if (world.unloadChunkRequest(chunk.getX(), chunk.getZ())) {
                    chunksUnloaded++;
                    lastAccessedChunks.remove(key);
                }
            }
        }
        lastAccessedChunks.entrySet().removeIf(entry -> now - entry.getValue() > unloadAfterSeconds * 2L);
    }

    private void markAccessed(Chunk chunk, long now) { lastAccessedChunks.put(getChunkKey(chunk), now); }

    private boolean isPlayerChunk(Chunk chunk) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() == chunk.getWorld() && player.getLocation().getChunk().equals(chunk)) return true;
        }
        return false;
    }

    private boolean isSpawnChunk(Chunk chunk) {
        int spawnX = chunk.getWorld().getSpawnLocation().getBlockX() >> 4;
        int spawnZ = chunk.getWorld().getSpawnLocation().getBlockZ() >> 4;
        int dx = chunk.getX() - spawnX;
        int dz = chunk.getZ() - spawnZ;
        int radius = Math.max(0, config.getSpawnChunkRadius());
        return dx * dx + dz * dz <= radius * radius;
    }

    private String getChunkKey(Chunk chunk) { return chunk.getWorld().getUID() + ":" + chunk.getX() + ":" + chunk.getZ(); }

    public void recordPlayerActivity(Player player) {
        if (player != null) markAccessed(player.getLocation().getChunk(), System.currentTimeMillis() / 1000L);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) { markAccessed(event.getChunk(), System.currentTimeMillis() / 1000L); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) { recordPlayerActivity(event.getPlayer()); }

    public boolean isEnabled() { return task != null && !task.isCancelled(); }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("trackedChunks", lastAccessedChunks.size());
        stats.put("chunksUnloaded", chunksUnloaded);
        stats.put("unloadAfterSeconds", unloadAfterSeconds);
        return stats;
    }

    public void shutdown() {
        if (task != null) { task.cancel(); task = null; }
        lastAccessedChunks.clear();
        logger.fine("Chunk manager stopped");
    }
}
