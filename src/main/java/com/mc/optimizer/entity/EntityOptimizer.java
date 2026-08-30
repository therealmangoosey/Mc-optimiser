package com.mc.optimizer.entity;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.config.ConfigManager;
import com.mc.optimizer.utils.ServerUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Boss;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

/** Lightweight entity guard. Normal operation is event-driven; world scans only run under pressure. */
public final class EntityOptimizer implements Listener {
    private final OptimizerPlugin plugin;
    private final ConfigManager config;
    private final Logger logger;
    private final Map<String, Integer> removalsByReason = new HashMap<>();
    private BukkitTask pressureTask;
    private int maxEntitiesPerChunk;
    private int maxItemsPerChunk;
    private int checkIntervalTicks;
    private boolean enforceLimits;
    private boolean mergeItems;
    private double itemMergeRadius;
    private int entitiesRemoved;
    private int itemsMerged;
    private int spawnPrevented;

    public EntityOptimizer(OptimizerPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        loadConfiguration();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        pressureTask = Bukkit.getScheduler().runTaskTimer(plugin, this::pressureCheck, checkIntervalTicks, checkIntervalTicks);
    }

    private void loadConfiguration() {
        maxEntitiesPerChunk = Math.max(1, plugin.getConfig().getInt("entity.max-entities-per-chunk", 80));
        maxItemsPerChunk = Math.max(1, plugin.getConfig().getInt("entity.max-items-per-chunk", 40));
        checkIntervalTicks = Math.max(200, plugin.getConfig().getInt("entity.check-interval-ticks", 200));
        enforceLimits = plugin.getConfig().getBoolean("entity.enforce-limits", true);
        mergeItems = plugin.getConfig().getBoolean("entity.merge-items", false);
        itemMergeRadius = Math.max(0.5, plugin.getConfig().getDouble("entity.item-merge-radius", 2.5));
    }

    private void pressureCheck() {
        if (!enforceLimits || (!ServerUtils.isMemoryPressureHigh() && !ServerUtils.isCPUPressureHigh())) return;
        for (World world : Bukkit.getWorlds()) {
            Map<Long, Integer> entityCounts = new HashMap<>();
            Map<Long, Integer> itemCounts = new HashMap<>();
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Player) continue;
                long key = chunkKey(entity.getLocation().getChunk());
                if (entity instanceof Item) itemCounts.merge(key, 1, Integer::sum);
                else entityCounts.merge(key, 1, Integer::sum);
            }
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Player || !entity.isValid()) continue;
                long key = chunkKey(entity.getLocation().getChunk());
                if (entity instanceof Item item) {
                    int count = itemCounts.getOrDefault(key, 0);
                    if (count > maxItemsPerChunk && item.getTicksLived() > 1200) {
                        item.remove();
                        itemCounts.merge(key, -1, Integer::sum);
                        removalsByReason.merge("old-items", 1, Integer::sum);
                        entitiesRemoved++;
                    }
                } else {
                    int count = entityCounts.getOrDefault(key, 0);
                    if (count > maxEntitiesPerChunk && shouldRemoveEntity(entity)) {
                        entity.remove();
                        entityCounts.merge(key, -1, Integer::sum);
                        removalsByReason.merge("pressure", 1, Integer::sum);
                        entitiesRemoved++;
                    }
                }
            }
        }
    }

    private boolean shouldRemoveEntity(Entity entity) {
        if (entity.getCustomName() != null || entity instanceof Boss) return false;
        if (entity instanceof Tameable tameable && tameable.isTamed()) return false;
        if (!(entity instanceof Monster) && !(entity instanceof Animals)) return false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld() == entity.getWorld()
                    && player.getLocation().distanceSquared(entity.getLocation()) <= 48.0 * 48.0) return false;
        }
        return true;
    }

    private long chunkKey(Chunk chunk) {
        return ((long) chunk.getX() << 32) ^ (chunk.getZ() & 0xffffffffL);
    }

    private int countLivingEntities(Chunk chunk) {
        int count = 0;
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof LivingEntity && !(entity instanceof Player)) count++;
        }
        return count;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEntitySpawn(EntitySpawnEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LivingEntity) || entity instanceof Player || !enforceLimits) return;
        if (!ServerUtils.isMemoryPressureHigh() && !ServerUtils.isCPUPressureHigh()) return;
        if (countLivingEntities(entity.getLocation().getChunk()) > maxEntitiesPerChunk && shouldRemoveEntity(entity)) {
            event.setCancelled(true);
            spawnPrevented++;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        if (!mergeItems) return;
        Item item = event.getEntity();
        Bukkit.getScheduler().runTask(plugin, () -> tryMerge(item));
    }

    private void tryMerge(Item item) {
        if (!item.isValid()) return;
        ItemStack stack = item.getItemStack();
        if (stack.getAmount() >= stack.getMaxStackSize()) return;
        for (Entity nearby : item.getNearbyEntities(itemMergeRadius, itemMergeRadius, itemMergeRadius)) {
            if (!(nearby instanceof Item other) || other == item || !other.isValid()) continue;
            ItemStack otherStack = other.getItemStack();
            if (!stack.isSimilar(otherStack)) continue;
            int max = stack.getMaxStackSize();
            int total = stack.getAmount() + otherStack.getAmount();
            if (total > max) continue;
            stack.setAmount(total);
            item.setItemStack(stack);
            other.remove();
            itemsMerged++;
            return;
        }
    }

    public boolean isEnabled() {
        return pressureTask != null && !pressureTask.isCancelled();
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("entitiesRemoved", entitiesRemoved);
        stats.put("itemsStacked", itemsMerged);
        stats.put("spawnPrevented", spawnPrevented);
        stats.put("removalReasons", new HashMap<>(removalsByReason));
        return stats;
    }

    public void shutdown() {
        if (pressureTask != null) {
            pressureTask.cancel();
            pressureTask = null;
        }
    }
}
