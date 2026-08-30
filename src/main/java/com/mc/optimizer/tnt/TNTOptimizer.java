/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.TNTPrimed
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.block.BlockIgniteEvent
 *  org.bukkit.event.block.BlockPlaceEvent
 *  org.bukkit.event.entity.EntityExplodeEvent
 *  org.bukkit.event.entity.ExplosionPrimeEvent
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.mc.optimizer.tnt;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.config.ConfigManager;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class TNTOptimizer
implements Listener {
    private final OptimizerPlugin plugin;
    private final ConfigManager config;
    private final Logger logger;
    private boolean enabled;
    private int maxTntPerTick;
    private boolean batchPhysicsUpdates;
    private int maxExplosionsPerTick;
    private boolean dynamicExplosionScaling;
    private boolean optimizeParticles;
    private final Queue<TNTPrimed> explosionQueue = new ConcurrentLinkedQueue<TNTPrimed>();
    private BukkitTask explosionTask;
    private final AtomicInteger tntCounter = new AtomicInteger(0);
    private final AtomicInteger explosionCounter = new AtomicInteger(0);
    private final Map<Location, Integer> tntActivity = new HashMap<Location, Integer>();
    private BukkitTask activityCleanupTask;

    public TNTOptimizer(OptimizerPlugin plugin, ConfigManager config) {
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
        this.maxTntPerTick = 20;
        this.batchPhysicsUpdates = true;
        this.maxExplosionsPerTick = 5;
        this.dynamicExplosionScaling = true;
        this.optimizeParticles = true;
        try {
            if (this.config != null) {
                try {
                    Method method = this.config.getClass().getMethod("isTntOptimizationEnabled", new Class[0]);
                    this.enabled = (Boolean)method.invoke((Object)this.config, new Object[0]);
                }
                catch (Exception e) {
                    this.logger.warning("Could not load TNT optimization configuration: " + e.getMessage());
                }
            }
        }
        catch (Exception e) {
            this.logger.warning("Error loading TNT optimization configuration: " + e.getMessage());
        }
    }

    private void initialize() {
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this.plugin);
        this.startExplosionProcessor();
        this.startActivityCleanup();
        this.logger.info("TNT optimizer initialized");
    }

    private void startExplosionProcessor() {
        if (this.explosionTask != null) {
            this.explosionTask.cancel();
        }
        this.explosionTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            this.tntCounter.set(0);
            this.explosionCounter.set(0);
            int processedCount = 0;
            while (!this.explosionQueue.isEmpty() && processedCount < this.maxExplosionsPerTick) {
                TNTPrimed tnt = this.explosionQueue.poll();
                if (tnt == null || tnt.isDead()) continue;
                this.processExplosion(tnt);
                ++processedCount;
            }
        }, 1L, 1L);
    }

    private void startActivityCleanup() {
        if (this.activityCleanupTask != null) {
            this.activityCleanupTask.cancel();
        }
        this.activityCleanupTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, () -> {
            Iterator<Map.Entry<Location, Integer>> it = this.tntActivity.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Location, Integer> entry = it.next();
                int value = entry.getValue() - 1;
                if (value <= 0) {
                    it.remove();
                    continue;
                }
                entry.setValue(value);
            }
        }, 20L, 20L);
    }

    private void processExplosion(TNTPrimed tnt) {
        if (tnt.isDead()) {
            return;
        }
        Location location = tnt.getLocation();
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        float power = 4.0f;
        if (this.dynamicExplosionScaling) {
            int activityLevel = this.getActivityLevel(location);
            if (activityLevel > 10) {
                power = 3.0f;
            } else if (activityLevel > 20) {
                power = 2.0f;
            }
        }
        boolean breakBlocks = true;
        boolean setFire = false;
        if (this.optimizeParticles) {
            world.createExplosion(location.getX(), location.getY(), location.getZ(), power, setFire, breakBlocks, (Entity)tnt);
        } else {
            world.createExplosion(location, power, setFire, breakBlocks, (Entity)tnt);
        }
        Bukkit.getScheduler().runTask((Plugin)this.plugin, () -> ((TNTPrimed)tnt).remove());
        this.explosionCounter.incrementAndGet();
    }

    private int getActivityLevel(Location location) {
        Integer exact = this.tntActivity.get(location);
        if (exact != null) {
            return exact;
        }
        int total = 0;
        for (Map.Entry<Location, Integer> entry : this.tntActivity.entrySet()) {
            if (!entry.getKey().getWorld().equals((Object)location.getWorld()) || !(entry.getKey().distanceSquared(location) < 25.0)) continue;
            total += entry.getValue().intValue();
        }
        return total;
    }

    private void incrementActivityLevel(Location location) {
        Location key = location.getBlock().getLocation();
        this.tntActivity.merge(key, 1, Integer::sum);
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!this.enabled) {
            return;
        }
        if (event.getBlock().getType() == Material.TNT) {
            this.incrementActivityLevel(event.getBlock().getLocation());
        }
    }

    @EventHandler(priority=EventPriority.MONITOR, ignoreCancelled=true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!this.enabled) {
            return;
        }
        Block block = event.getBlock();
        if (block.getType() == Material.TNT) {
            this.incrementActivityLevel(block.getLocation());
        }
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onExplosionPrime(ExplosionPrimeEvent event) {
        if (!this.enabled) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof TNTPrimed) {
            if (this.tntCounter.incrementAndGet() > this.maxTntPerTick) {
                this.explosionQueue.add((TNTPrimed)entity);
                event.setCancelled(true);
            }
            this.incrementActivityLevel(entity.getLocation());
        }
    }

    @EventHandler(priority=EventPriority.HIGH, ignoreCancelled=true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!this.enabled) {
            return;
        }
        Entity entity = event.getEntity();
        if (entity instanceof TNTPrimed) {
            if (this.explosionCounter.incrementAndGet() > this.maxExplosionsPerTick) {
                event.setCancelled(true);
                this.explosionQueue.add((TNTPrimed)entity);
                return;
            }
            if (this.batchPhysicsUpdates) {
                this.optimizeExplosion(event);
            }
            this.incrementActivityLevel(entity.getLocation());
        }
    }

    private void optimizeExplosion(EntityExplodeEvent event) {
        List blocks;
        int newSize;
        if (event.blockList().isEmpty()) {
            return;
        }
        int activityLevel = this.getActivityLevel(event.getLocation());
        float reductionFactor = 1.0f;
        if (activityLevel > 10) {
            reductionFactor = 0.75f;
        } else if (activityLevel > 20) {
            reductionFactor = 0.5f;
        } else if (activityLevel > 30) {
            reductionFactor = 0.25f;
        }
        if (reductionFactor < 1.0f && (newSize = Math.max(1, (int)((float)(blocks = event.blockList()).size() * reductionFactor))) < blocks.size()) {
            event.blockList().subList(newSize, blocks.size()).clear();
        }
    }

    public Map<String, Object> getStats() {
        HashMap<String, Object> stats = new HashMap<String, Object>();
        stats.put("enabled", this.enabled);
        stats.put("explosionQueueSize", this.explosionQueue.size());
        stats.put("tntActivityLocations", this.tntActivity.size());
        stats.put("tntProcessedLastTick", this.tntCounter.get());
        stats.put("explosionsProcessedLastTick", this.explosionCounter.get());
        return stats;
    }

    public void reload() {
        this.shutdown();
        this.loadConfiguration();
        if (this.enabled) {
            this.initialize();
        }
    }

    public void shutdown() {
        if (this.explosionTask != null) {
            this.explosionTask.cancel();
            this.explosionTask = null;
        }
        if (this.activityCleanupTask != null) {
            this.activityCleanupTask.cancel();
            this.activityCleanupTask = null;
        }
        this.explosionQueue.clear();
        this.tntActivity.clear();
        this.tntCounter.set(0);
        this.explosionCounter.set(0);
        this.logger.info("TNT optimizer shutdown");
    }
}

