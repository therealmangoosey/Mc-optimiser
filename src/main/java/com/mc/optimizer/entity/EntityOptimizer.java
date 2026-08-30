/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.Chunk
 *  org.bukkit.World
 *  org.bukkit.entity.Animals
 *  org.bukkit.entity.Boss
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Item
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Mob
 *  org.bukkit.entity.Monster
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Tameable
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.EventPriority
 *  org.bukkit.event.Listener
 *  org.bukkit.event.entity.CreatureSpawnEvent
 *  org.bukkit.event.entity.EntitySpawnEvent
 *  org.bukkit.event.entity.ItemSpawnEvent
 *  org.bukkit.inventory.ItemStack
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 *  org.bukkit.util.Vector
 */
package com.mc.optimizer.entity;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.config.ConfigManager;
import com.mc.optimizer.utils.ServerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class EntityOptimizer
implements Listener {
    private final OptimizerPlugin plugin;
    private final ConfigManager config;
    private final Logger logger;
    private int maxEntitiesPerChunk;
    private int maxItemsPerChunk;
    private int checkIntervalTicks;
    private boolean mergeItems;
    private double itemMergeRadius;
    private boolean limitDistantMobAI;
    private int distantMobThreshold;
    private boolean acceleratedItemDespawn;
    private BukkitTask optimizationTask;
    private final Map<UUID, Long> entityLastSeen = new ConcurrentHashMap<UUID, Long>();
    private final Set<UUID> limitedAIMobs = Collections.newSetFromMap(new ConcurrentHashMap());
    private int entitiesRemoved = 0;
    private int itemsRemoved = 0;
    private int itemsStacked = 0;
    private int aiLimited = 0;

    public EntityOptimizer(OptimizerPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.loadConfiguration();
        plugin.getServer().getPluginManager().registerEvents((Listener)this, (Plugin)plugin);
        this.startOptimizationTask();
        this.logger.info("Entity Optimizer initialized. Max entities per chunk: " + this.maxEntitiesPerChunk);
    }

    private void loadConfiguration() {
        try {
            this.maxEntitiesPerChunk = this.plugin.getConfig().getInt("entity.max-entities-per-chunk", 25);
            this.maxItemsPerChunk = this.plugin.getConfig().getInt("entity.max-items-per-chunk", 20);
            this.checkIntervalTicks = this.plugin.getConfig().getInt("entity.check-interval-ticks", 100);
            this.mergeItems = this.plugin.getConfig().getBoolean("entity.merge-items", true);
            this.itemMergeRadius = this.plugin.getConfig().getDouble("entity.item-merge-radius", 2.5);
            this.limitDistantMobAI = this.plugin.getConfig().getBoolean("entity.limit-distant-mob-ai", true);
            this.distantMobThreshold = this.plugin.getConfig().getInt("entity.distant-mob-threshold", 48);
            this.acceleratedItemDespawn = this.plugin.getConfig().getBoolean("entity.accelerated-item-despawn", true);
        }
        catch (Exception e) {
            this.logger.warning("Error loading entity optimization configuration: " + e.getMessage());
            this.maxEntitiesPerChunk = 25;
            this.maxItemsPerChunk = 20;
            this.checkIntervalTicks = 100;
            this.mergeItems = true;
            this.itemMergeRadius = 2.5;
            this.limitDistantMobAI = true;
            this.distantMobThreshold = 48;
            this.acceleratedItemDespawn = true;
        }
    }

    private void startOptimizationTask() {
        this.optimizationTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, this::performOptimization, (long)this.checkIntervalTicks, (long)this.checkIntervalTicks);
    }

    private void performOptimization() {
        long startTime = System.currentTimeMillis();
        for (World world : Bukkit.getWorlds()) {
            this.optimizeWorldEntities(world);
        }
        this.cleanupTrackingData();
        if (this.isDebugEnabled()) {
            long duration = System.currentTimeMillis() - startTime;
            this.logger.fine("Entity optimization completed in " + duration + "ms");
        }
    }

    private void optimizeWorldEntities(World world) {
        long chunkKey;
        Chunk chunk;
        long currentTime = System.currentTimeMillis();
        HashMap<Long, Integer> entitiesInChunk = new HashMap<Long, Integer>();
        HashMap<Long, Integer> itemsInChunk = new HashMap<Long, Integer>();
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Player) continue;
            chunk = entity.getLocation().getChunk();
            chunkKey = this.getChunkKey(chunk);
            this.entityLastSeen.put(entity.getUniqueId(), currentTime);
            if (entity instanceof Item) {
                itemsInChunk.put(chunkKey, itemsInChunk.getOrDefault(chunkKey, 0) + 1);
            } else {
                entitiesInChunk.put(chunkKey, entitiesInChunk.getOrDefault(chunkKey, 0) + 1);
            }
            if (!this.limitDistantMobAI || !(entity instanceof Mob)) continue;
            this.handleMobAI((Mob)entity);
        }
        for (Entity entity : world.getEntities()) {
            if (entity instanceof Player) continue;
            chunk = entity.getLocation().getChunk();
            chunkKey = this.getChunkKey(chunk);
            if (entity instanceof Item) {
                Item item = (Item)entity;
                int itemCount = itemsInChunk.getOrDefault(chunkKey, 0);
                if (itemCount > this.maxItemsPerChunk && !this.isRecentlySpawned((Entity)item, currentTime)) {
                    item.remove();
                    ++this.itemsRemoved;
                    itemsInChunk.put(chunkKey, itemCount - 1);
                    continue;
                }
                if (this.mergeItems) {
                    this.attemptItemMerge(item);
                }
                if (!this.acceleratedItemDespawn || itemCount <= this.maxItemsPerChunk / 2 || currentTime - this.entityLastSeen.getOrDefault(item.getUniqueId(), currentTime) <= 30000L) continue;
                item.setTicksLived(Math.min(item.getTicksLived() + 20, 5900));
                continue;
            }
            int entityCount = entitiesInChunk.getOrDefault(chunkKey, 0);
            if (entityCount <= this.maxEntitiesPerChunk || !this.shouldRemoveEntity(entity)) continue;
            entity.remove();
            ++this.entitiesRemoved;
            entitiesInChunk.put(chunkKey, entityCount - 1);
        }
    }

    private boolean isRecentlySpawned(Entity entity, long currentTime) {
        Long lastSeen = this.entityLastSeen.get(entity.getUniqueId());
        return lastSeen == null || currentTime - lastSeen < 10000L;
    }

    private void attemptItemMerge(Item item) {
        if (!item.isValid()) {
            return;
        }
        ItemStack itemStack = item.getItemStack();
        if (itemStack.getAmount() >= itemStack.getMaxStackSize()) {
            return;
        }
        List nearbyEntities = item.getNearbyEntities(this.itemMergeRadius, this.itemMergeRadius, this.itemMergeRadius);
        for (Entity nearby : nearbyEntities) {
            int maxStackSize;
            Item nearbyItem;
            ItemStack nearbyStack;
            if (!(nearby instanceof Item) || !nearby.isValid() || !this.canStackItems(itemStack, nearbyStack = (nearbyItem = (Item)nearby).getItemStack())) continue;
            int totalAmount = itemStack.getAmount() + nearbyStack.getAmount();
            if (totalAmount <= (maxStackSize = itemStack.getMaxStackSize())) {
                itemStack.setAmount(totalAmount);
                item.setItemStack(itemStack);
                nearbyItem.remove();
                ++this.itemsStacked;
                break;
            }
            itemStack.setAmount(maxStackSize);
            item.setItemStack(itemStack);
            nearbyStack.setAmount(totalAmount - maxStackSize);
            nearbyItem.setItemStack(nearbyStack);
            ++this.itemsStacked;
        }
    }

    private boolean canStackItems(ItemStack first, ItemStack second) {
        if (first.getType() != second.getType()) {
            return false;
        }
        if (first.hasItemMeta() != second.hasItemMeta()) {
            return false;
        }
        if (first.hasItemMeta() && second.hasItemMeta()) {
            return first.getItemMeta().equals((Object)second.getItemMeta());
        }
        return true;
    }

    private void handleMobAI(Mob mob) {
        if (mob instanceof Boss || mob.getCustomName() != null) {
            return;
        }
        boolean shouldLimit = true;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() != mob.getWorld() || !(player.getLocation().distance(mob.getLocation()) < (double)this.distantMobThreshold)) continue;
            shouldLimit = false;
            break;
        }
        if (shouldLimit && !this.limitedAIMobs.contains(mob.getUniqueId())) {
            this.limitMobAI(mob);
            this.limitedAIMobs.add(mob.getUniqueId());
            ++this.aiLimited;
        } else if (!shouldLimit && this.limitedAIMobs.contains(mob.getUniqueId())) {
            this.restoreMobAI(mob);
            this.limitedAIMobs.remove(mob.getUniqueId());
        }
    }

    private void limitMobAI(Mob mob) {
        mob.setAware(false);
        if (mob.getTarget() != null && mob.getTarget().getLocation().distance(mob.getLocation()) > (double)this.distantMobThreshold) {
            mob.setTarget(null);
        }
        mob.setVelocity(new Vector(0, 0, 0));
        if (this.isDebugEnabled()) {
            this.logger.fine("Limited AI for distant mob: " + String.valueOf(mob.getType()) + " at " + mob.getLocation().getBlockX() + "," + mob.getLocation().getBlockY() + "," + mob.getLocation().getBlockZ());
        }
    }

    private void restoreMobAI(Mob mob) {
        mob.setAware(true);
        if (this.isDebugEnabled()) {
            this.logger.fine("Restored AI for mob: " + String.valueOf(mob.getType()));
        }
    }

    private boolean shouldRemoveEntity(Entity entity) {
        if (entity.getCustomName() != null) {
            return false;
        }
        if (entity instanceof Boss) {
            return false;
        }
        if (entity instanceof Tameable && ((Tameable)entity).isTamed()) {
            return false;
        }
        if (entity instanceof Monster) {
            boolean anyPlayerNearby = false;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld() != entity.getWorld() || !(player.getLocation().distance(entity.getLocation()) < 48.0)) continue;
                anyPlayerNearby = true;
                break;
            }
            return !anyPlayerNearby;
        }
        if (entity instanceof Animals) {
            int count = 0;
            for (Entity nearby : entity.getNearbyEntities(32.0, 32.0, 32.0)) {
                if (nearby.getClass() != entity.getClass()) continue;
                ++count;
            }
            return count > 8;
        }
        return ServerUtils.isMemoryPressureHigh();
    }

    private void cleanupTrackingData() {
        long currentTime = System.currentTimeMillis();
        this.entityLastSeen.entrySet().removeIf(entry -> currentTime - (Long)entry.getValue() > 300000L);
        this.limitedAIMobs.removeIf(uuid -> !this.entityLastSeen.containsKey(uuid));
    }

    private long getChunkKey(Chunk chunk) {
        return (long)chunk.getX() << 32 | (long)chunk.getZ() & 0xFFFFFFFFL;
    }

    public Map<String, Object> getStats() {
        HashMap<String, Object> stats = new HashMap<String, Object>();
        stats.put("entitiesRemoved", this.entitiesRemoved);
        stats.put("itemsRemoved", this.itemsRemoved);
        stats.put("itemsStacked", this.itemsStacked);
        stats.put("aiLimited", this.aiLimited);
        stats.put("trackedEntities", this.entityLastSeen.size());
        HashMap<String, Integer> entityCounts = new HashMap<String, Integer>();
        int totalEntities = 0;
        int totalItems = 0;
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Item) {
                    ++totalItems;
                    continue;
                }
                ++totalEntities;
                String type = entity.getType().toString();
                entityCounts.put(type, entityCounts.getOrDefault(type, 0) + 1);
            }
        }
        stats.put("totalEntities", totalEntities);
        stats.put("totalItems", totalItems);
        stats.put("entityTypes", entityCounts);
        return stats;
    }

    public void shutdown() {
        if (this.optimizationTask != null) {
            this.optimizationTask.cancel();
        }
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof Mob) || !this.limitedAIMobs.contains(entity.getUniqueId())) continue;
                this.restoreMobAI((Mob)entity);
            }
        }
        this.entityLastSeen.clear();
        this.limitedAIMobs.clear();
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!event.isCancelled()) {
            Item item = event.getEntity();
            this.entityLastSeen.put(item.getUniqueId(), System.currentTimeMillis());
            if (this.mergeItems) {
                Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                    if (item.isValid()) {
                        this.attemptItemMerge(item);
                    }
                }, 1L);
            }
            Chunk chunk = item.getLocation().getChunk();
            int itemCount = 0;
            for (Entity entity : chunk.getEntities()) {
                if (!(entity instanceof Item)) continue;
                ++itemCount;
            }
            if (this.acceleratedItemDespawn && itemCount > this.maxItemsPerChunk) {
                ArrayList<Object> chunkItems = new ArrayList<Object>();
                for (Entity entity : chunk.getEntities()) {
                    if (!(entity instanceof Item)) continue;
                    chunkItems.add((Item)entity);
                }
                chunkItems.sort(Comparator.comparingInt(Entity::getTicksLived).reversed());
                int toAccelerate = itemCount - this.maxItemsPerChunk;
                for (int i = 0; i < Math.min(toAccelerate, chunkItems.size()); ++i) {
                    Item oldItem = (Item)chunkItems.get(i);
                    oldItem.setTicksLived(Math.min(oldItem.getTicksLived() + 1000, 5980));
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (!event.isCancelled()) {
            Entity entity = event.getEntity();
            if (entity instanceof Item) {
                return;
            }
            this.entityLastSeen.put(entity.getUniqueId(), System.currentTimeMillis());
            if (entity instanceof LivingEntity && !(entity instanceof Player)) {
                Chunk chunk = entity.getLocation().getChunk();
                int entityCount = 0;
                for (Entity e : chunk.getEntities()) {
                    if (!(e instanceof LivingEntity) || e instanceof Player) continue;
                    ++entityCount;
                }
                if (entityCount > this.maxEntitiesPerChunk && (ServerUtils.isMemoryPressureHigh() || ServerUtils.isCPUPressureHigh())) {
                    event.setCancelled(true);
                    if (this.isDebugEnabled()) {
                        this.logger.fine("Prevented spawn of " + String.valueOf(entity.getType()) + " due to too many entities in chunk: " + entityCount);
                    }
                }
            }
        }
    }

    @EventHandler(priority=EventPriority.MONITOR)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!event.isCancelled() && event.getEntity() instanceof Mob) {
            Mob mob = (Mob)event.getEntity();
            if (this.limitDistantMobAI) {
                Bukkit.getScheduler().runTaskLater((Plugin)this.plugin, () -> {
                    if (mob.isValid()) {
                        this.handleMobAI(mob);
                    }
                }, 1L);
            }
        }
    }

    public boolean isEnabled() {
        try {
            return this.plugin.getConfig().getBoolean("entities.enabled", true);
        }
        catch (Exception e) {
            this.logger.warning("Error checking if entity optimizer is enabled: " + e.getMessage());
            return true;
        }
    }

    private boolean isDebugEnabled() {
        try {
            return this.plugin.getConfig().getBoolean("debug", false);
        }
        catch (Exception e) {
            return false;
        }
    }
}

