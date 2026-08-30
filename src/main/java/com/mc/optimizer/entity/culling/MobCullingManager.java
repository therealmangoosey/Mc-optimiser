/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.World
 *  org.bukkit.entity.Boss
 *  org.bukkit.entity.ElderGuardian
 *  org.bukkit.entity.EnderDragon
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Player
 *  org.bukkit.entity.Tameable
 *  org.bukkit.entity.Wither
 *  org.bukkit.inventory.EntityEquipment
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.mc.optimizer.entity.culling;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.config.ConfigManager;
import com.mc.optimizer.utils.ServerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Boss;
import org.bukkit.entity.ElderGuardian;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wither;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class MobCullingManager {
    private final OptimizerPlugin plugin;
    private final ConfigManager config;
    private final Logger logger;
    private boolean enabled;
    private int maxMobsPerWorld;
    private Map<EntityType, Integer> maxMobsPerType;
    private int playerProtectionRadius;
    private boolean preserveNamedMobs;
    private boolean preserveTamedMobs;
    private boolean preserveEquippedMobs;
    private boolean preserveBossMobs;
    private final Map<World, Map<EntityType, Integer>> mobCountsByWorld = new ConcurrentHashMap<World, Map<EntityType, Integer>>();
    private final Set<UUID> preservedEntities = Collections.newSetFromMap(new ConcurrentHashMap());
    private BukkitTask cullingTask;
    private BukkitTask countTask;
    private int totalMobsCulled = 0;
    private int cullingRuns = 0;
    private final Map<EntityType, Integer> culledByType = new ConcurrentHashMap<EntityType, Integer>();

    public MobCullingManager(OptimizerPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.loadConfiguration();
        if (this.enabled) {
            this.countTask = Bukkit.getScheduler().runTaskTimer((Plugin)plugin, this::countMobsByWorld, 200L, 600L);
            this.cullingTask = Bukkit.getScheduler().runTaskTimer((Plugin)plugin, this::performCulling, 1200L, 2400L);
            this.logger.info("Mob Culling Manager initialized. Max mobs per world: " + this.maxMobsPerWorld);
        } else {
            this.logger.info("Mob Culling Manager disabled in configuration");
        }
    }

    private void loadConfiguration() {
        try {
            this.enabled = true;
            this.maxMobsPerWorld = 1000;
            this.playerProtectionRadius = 48;
            this.preserveNamedMobs = true;
            this.preserveTamedMobs = true;
            this.preserveEquippedMobs = true;
            this.preserveBossMobs = true;
            this.maxMobsPerType = new HashMap<EntityType, Integer>();
            this.enabled = this.plugin.getConfig().getBoolean("entities.culling.enabled", true);
            this.maxMobsPerWorld = this.plugin.getConfig().getInt("entities.culling.max-mobs-per-world", 1000);
            this.playerProtectionRadius = this.plugin.getConfig().getInt("entities.culling.player-protection-radius", 48);
            this.preserveNamedMobs = this.plugin.getConfig().getBoolean("entities.culling.preserve-named-mobs", true);
            this.preserveTamedMobs = this.plugin.getConfig().getBoolean("entities.culling.preserve-tamed-mobs", true);
            this.preserveEquippedMobs = this.plugin.getConfig().getBoolean("entities.culling.preserve-equipped-mobs", true);
            this.preserveBossMobs = this.plugin.getConfig().getBoolean("entities.culling.preserve-boss-mobs", true);
            if (this.plugin.getConfig().isConfigurationSection("entities.culling.max-per-type")) {
                for (String key : this.plugin.getConfig().getConfigurationSection("entities.culling.max-per-type").getKeys(false)) {
                    try {
                        EntityType type = EntityType.valueOf((String)key.toUpperCase());
                        int limit = this.plugin.getConfig().getInt("entities.culling.max-per-type." + key, 50);
                        this.maxMobsPerType.put(type, limit);
                    }
                    catch (IllegalArgumentException e) {
                        this.logger.warning("Invalid entity type in config: " + key);
                    }
                }
            }
            if (!this.maxMobsPerType.containsKey(EntityType.ZOMBIE)) {
                this.maxMobsPerType.put(EntityType.ZOMBIE, 50);
            }
            if (!this.maxMobsPerType.containsKey(EntityType.SKELETON)) {
                this.maxMobsPerType.put(EntityType.SKELETON, 50);
            }
            if (!this.maxMobsPerType.containsKey(EntityType.SPIDER)) {
                this.maxMobsPerType.put(EntityType.SPIDER, 40);
            }
            if (!this.maxMobsPerType.containsKey(EntityType.CREEPER)) {
                this.maxMobsPerType.put(EntityType.CREEPER, 30);
            }
        }
        catch (Exception e) {
            this.logger.warning("Error loading mob culling configuration: " + e.getMessage());
        }
    }

    private void countMobsByWorld() {
        if (!this.enabled) {
            return;
        }
        this.mobCountsByWorld.clear();
        this.preservedEntities.clear();
        for (World world : Bukkit.getWorlds()) {
            HashMap<EntityType, Integer> worldCounts = new HashMap<EntityType, Integer>();
            this.mobCountsByWorld.put(world, worldCounts);
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity) || entity instanceof Player) continue;
                EntityType type = entity.getType();
                worldCounts.put(type, worldCounts.getOrDefault(type, 0) + 1);
                if (!this.shouldPreserveEntity((LivingEntity)entity)) continue;
                this.preservedEntities.add(entity.getUniqueId());
            }
        }
        if (this.config.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder("Mob counts by world:\n");
            for (Map.Entry<World, Map<EntityType, Integer>> entry : this.mobCountsByWorld.entrySet()) {
                World world = entry.getKey();
                Map<EntityType, Integer> counts = entry.getValue();
                int total = counts.values().stream().mapToInt(Integer::intValue).sum();
                sb.append("  ").append(world.getName()).append(": ").append(total).append(" total mobs\n");
                counts.entrySet().stream().sorted(Map.Entry.comparingByValue().reversed()).limit(5L).forEach(typeEntry -> sb.append("    ").append(((EntityType)typeEntry.getKey()).name()).append(": ").append(typeEntry.getValue()).append("\n"));
            }
            this.logger.fine(sb.toString());
        }
    }

    private void performCulling() {
        if (!this.enabled) {
            return;
        }
        if (this.mobCountsByWorld.isEmpty()) {
            this.countMobsByWorld();
            return;
        }
        boolean highMemory = ServerUtils.isMemoryPressureHigh();
        boolean highCPU = ServerUtils.isCPUPressureHigh();
        int effectiveMaxMobsPerWorld = this.maxMobsPerWorld;
        if (highMemory || highCPU) {
            effectiveMaxMobsPerWorld = (int)((double)this.maxMobsPerWorld * 0.7);
        }
        HashMap<World, Map<EntityType, Integer>> cullTargets = new HashMap<World, Map<EntityType, Integer>>();
        for (Map.Entry<World, Map<EntityType, Integer>> entry : this.mobCountsByWorld.entrySet()) {
            World world = entry.getKey();
            Map<EntityType, Integer> counts = entry.getValue();
            HashMap<EntityType, Integer> worldCullTargets = new HashMap<EntityType, Integer>();
            cullTargets.put(world, worldCullTargets);
            int totalMobs = counts.values().stream().mapToInt(Integer::intValue).sum();
            if (totalMobs > effectiveMaxMobsPerWorld) {
                int excessMobs = totalMobs - effectiveMaxMobsPerWorld;
                ArrayList<Map.Entry<EntityType, Integer>> sortedCounts = new ArrayList<Map.Entry<EntityType, Integer>>(counts.entrySet());
                sortedCounts.sort(Map.Entry.comparingByValue().reversed());
                int remainingExcess = excessMobs;
                for (Map.Entry entry2 : sortedCounts) {
                    if (remainingExcess <= 0) break;
                    EntityType type = (EntityType)entry2.getKey();
                    int typeCount = (Integer)entry2.getValue();
                    int typeMax = this.maxMobsPerType.getOrDefault(type, Integer.MAX_VALUE);
                    if (highMemory || highCPU) {
                        typeMax = (int)((double)typeMax * 0.7);
                    }
                    int toRemove = 0;
                    if (typeCount > typeMax) {
                        toRemove += Math.min(typeCount - typeMax, remainingExcess);
                    }
                    double proportion = (double)typeCount / (double)totalMobs;
                    int proportionalRemoval = (int)Math.ceil((double)excessMobs * proportion);
                    toRemove = Math.min(toRemove + proportionalRemoval, typeCount / 2);
                    if ((toRemove = Math.min(toRemove, remainingExcess)) <= 0) continue;
                    worldCullTargets.put(type, toRemove);
                    remainingExcess -= toRemove;
                }
            }
            for (Map.Entry<EntityType, Integer> typeLimit : this.maxMobsPerType.entrySet()) {
                int n;
                EntityType type = typeLimit.getKey();
                int typeMax = typeLimit.getValue();
                if (highMemory || highCPU) {
                    typeMax = (int)((double)typeMax * 0.7);
                }
                if ((n = counts.getOrDefault(type, 0).intValue()) <= typeMax) continue;
                int toRemove = n - typeMax;
                worldCullTargets.put(type, worldCullTargets.getOrDefault(type, 0) + toRemove);
            }
        }
        int totalCulled = this.performActualCulling(cullTargets);
        if (totalCulled > 0) {
            this.logger.info("Culled " + totalCulled + " mobs across all worlds");
            this.totalMobsCulled += totalCulled;
            ++this.cullingRuns;
            this.countMobsByWorld();
        }
    }

    private int performActualCulling(Map<World, Map<EntityType, Integer>> cullTargets) {
        int totalCulled = 0;
        for (Map.Entry<World, Map<EntityType, Integer>> worldEntry : cullTargets.entrySet()) {
            EntityType type;
            World world = worldEntry.getKey();
            Map<EntityType, Integer> typeTargets = worldEntry.getValue();
            if (typeTargets.isEmpty()) continue;
            HashMap<EntityType, Integer> removed = new HashMap<EntityType, Integer>();
            HashMap<EntityType, List> eligibleByType = new HashMap<EntityType, List>();
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity) || entity instanceof Player || !typeTargets.containsKey(type = entity.getType()) || this.preservedEntities.contains(entity.getUniqueId()) || this.isNearPlayer(entity, this.playerProtectionRadius)) continue;
                eligibleByType.computeIfAbsent(type, k -> new ArrayList()).add(entity);
            }
            for (Map.Entry entry : eligibleByType.entrySet()) {
                type = (EntityType)entry.getKey();
                List eligible = (List)entry.getValue();
                eligible.sort((e1, e2) -> {
                    double d1 = e1.getLocation().distanceSquared(world.getSpawnLocation());
                    double d2 = e2.getLocation().distanceSquared(world.getSpawnLocation());
                    return Double.compare(d2, d1);
                });
                int toRemove = Math.min(typeTargets.get(type), eligible.size());
                for (int i = 0; i < toRemove; ++i) {
                    Entity entity = (Entity)eligible.get(i);
                    entity.remove();
                    removed.put(type, removed.getOrDefault(type, 0) + 1);
                    this.culledByType.put(type, this.culledByType.getOrDefault(type, 0) + 1);
                    ++totalCulled;
                }
            }
            if (!this.config.isDebugEnabled() || removed.isEmpty()) continue;
            StringBuilder sb = new StringBuilder("Culled mobs in world ").append(world.getName()).append(":\n");
            for (Map.Entry entry : removed.entrySet()) {
                sb.append("  ").append(((EntityType)entry.getKey()).name()).append(": ").append(entry.getValue()).append("\n");
            }
            this.logger.fine(sb.toString());
        }
        return totalCulled;
    }

    private boolean shouldPreserveEntity(LivingEntity entity) {
        EntityEquipment equipment;
        if (this.preserveBossMobs && (entity instanceof Boss || entity instanceof Wither || entity instanceof EnderDragon || entity instanceof ElderGuardian)) {
            return true;
        }
        if (this.preserveNamedMobs && entity.getCustomName() != null) {
            return true;
        }
        if (this.preserveTamedMobs && entity instanceof Tameable && ((Tameable)entity).isTamed()) {
            return true;
        }
        return this.preserveEquippedMobs && (equipment = entity.getEquipment()) != null && (equipment.getHelmet() != null || equipment.getChestplate() != null || equipment.getLeggings() != null || equipment.getBoots() != null || equipment.getItemInMainHand().getType().isItem() || equipment.getItemInOffHand().getType().isItem());
    }

    private boolean isNearPlayer(Entity entity, double radius) {
        double radiusSquared = radius * radius;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getWorld().equals((Object)entity.getWorld()) || !(player.getLocation().distanceSquared(entity.getLocation()) <= radiusSquared)) continue;
            return true;
        }
        return false;
    }

    public Map<String, Object> getStats() {
        HashMap<String, Object> stats = new HashMap<String, Object>();
        stats.put("totalMobsCulled", this.totalMobsCulled);
        stats.put("cullingRuns", this.cullingRuns);
        stats.put("preservedEntities", this.preservedEntities.size());
        HashMap<String, Integer> worldCounts = new HashMap<String, Integer>();
        HashMap<String, Integer> typeCounts = new HashMap<String, Integer>();
        for (Map.Entry<World, Map<EntityType, Integer>> entry : this.mobCountsByWorld.entrySet()) {
            String worldName = entry.getKey().getName();
            Map<EntityType, Integer> counts = entry.getValue();
            int total = counts.values().stream().mapToInt(Integer::intValue).sum();
            worldCounts.put(worldName, total);
            for (Map.Entry<EntityType, Integer> typeEntry : counts.entrySet()) {
                String typeName = typeEntry.getKey().name();
                int count = typeEntry.getValue();
                typeCounts.put(typeName, typeCounts.getOrDefault(typeName, 0) + count);
            }
        }
        stats.put("mobsByWorld", worldCounts);
        stats.put("mobsByType", typeCounts);
        HashMap<String, Integer> culledCounts = new HashMap<String, Integer>();
        for (Map.Entry<EntityType, Integer> entry : this.culledByType.entrySet()) {
            culledCounts.put(entry.getKey().name(), entry.getValue());
        }
        stats.put("culledByType", culledCounts);
        return stats;
    }

    public void shutdown() {
        if (this.cullingTask != null) {
            this.cullingTask.cancel();
        }
        if (this.countTask != null) {
            this.countTask.cancel();
        }
        this.mobCountsByWorld.clear();
        this.preservedEntities.clear();
    }

    public boolean isEnabled() {
        try {
            return this.plugin.getConfig().getBoolean("entities.culling.enabled", true);
        }
        catch (Exception e) {
            this.logger.warning("Error checking if mob culling is enabled: " + e.getMessage());
            return true;
        }
    }
}

