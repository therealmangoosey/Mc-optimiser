/*
 * Decompiled with CFR 0.152.
 *
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.GameMode
 *  org.bukkit.World
 *  org.bukkit.entity.Creature
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.LivingEntity
 *  org.bukkit.entity.Monster
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.mc.optimizer.entity.distance;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.config.ConfigManager;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class EntityDistanceLimiter {
    private final OptimizerPlugin plugin;
    private final ConfigManager config;
    private final Logger logger;
    private boolean enabled;
    private int fullAiRadius;
    private int reducedAiRadius;
    private int minimalAiRadius;
    private int noAiRadius;
    private Set<EntityType> exemptTypes;
    private boolean exemptNamedEntities;
    private BukkitTask limitingTask;
    private final Map<UUID, EntityAIState> entityStates = new HashMap<UUID, EntityAIState>();
    private int fullAiEntities = 0;
    private int reducedAiEntities = 0;
    private int minimalAiEntities = 0;
    private int noAiEntities = 0;

    public EntityDistanceLimiter(OptimizerPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.loadConfiguration();
        if (this.enabled) this.startLimiting();
    }

    private void loadConfiguration() {
        this.enabled = this.plugin.getConfig().getBoolean("entity-distance-limit.enabled", true);
        this.fullAiRadius = this.plugin.getConfig().getInt("entity-distance-limit.full-ai-radius", 24);
        this.reducedAiRadius = this.plugin.getConfig().getInt("entity-distance-limit.reduced-ai-radius", 32);
        this.minimalAiRadius = this.plugin.getConfig().getInt("entity-distance-limit.minimal-ai-radius", 48);
        this.noAiRadius = this.plugin.getConfig().getInt("entity-distance-limit.no-ai-radius", 64);
        this.exemptTypes = new HashSet<EntityType>(this.config.getEntityExemptTypes());
        this.exemptNamedEntities = this.plugin.getConfig().getBoolean("entity-distance-limit.exempt-named-entities", true);
    }

    public void startLimiting() {
        this.stopLimiting();
        this.limitingTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::processEntities, 20L, 40L);
        this.logger.info("Entity distance limiting started");
    }

    public void stopLimiting() {
        if (this.limitingTask != null) {
            this.limitingTask.cancel();
            this.limitingTask = null;
        }
        this.restoreAllEntityAI();
    }

    private void processEntities() {
        if (!this.enabled) return;
        this.fullAiEntities = this.reducedAiEntities = this.minimalAiEntities = this.noAiEntities = 0;
        HashSet<UUID> processedEntities = new HashSet<UUID>();
        for (World world : Bukkit.getWorlds()) {
            if (world.getPlayers().isEmpty()) continue;
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity) || this.exemptTypes.contains(entity.getType()) || (this.exemptNamedEntities && entity.getCustomName() != null)) continue;
                LivingEntity livingEntity = (LivingEntity)entity;
                processedEntities.add(entity.getUniqueId());
                EntityAILevel aiLevel = this.getAILevelForDistance(this.getNearestPlayerDistance(entity));
                this.applyAILevel(livingEntity, aiLevel);
                switch (aiLevel) {
                    case FULL: ++this.fullAiEntities; break;
                    case REDUCED: ++this.reducedAiEntities; break;
                    case MINIMAL: ++this.minimalAiEntities; break;
                    case NONE: ++this.noAiEntities; break;
                }
            }
        }
        this.cleanupRemovedEntities(processedEntities);
    }

    private double getNearestPlayerDistance(Entity entity) {
        double nearestDistanceSquared = Double.MAX_VALUE;
        for (Player player : entity.getWorld().getPlayers()) {
            if (player.getGameMode() == GameMode.SPECTATOR) continue;
            double distanceSquared = player.getLocation().distanceSquared(entity.getLocation());
            if (distanceSquared < nearestDistanceSquared) nearestDistanceSquared = distanceSquared;
        }
        return Math.sqrt(nearestDistanceSquared);
    }

    private EntityAILevel getAILevelForDistance(double distance) {
        if (distance <= this.fullAiRadius) return EntityAILevel.FULL;
        if (distance <= this.reducedAiRadius) return EntityAILevel.REDUCED;
        if (distance <= this.minimalAiRadius) return EntityAILevel.MINIMAL;
        return EntityAILevel.NONE;
    }

    private void applyAILevel(LivingEntity entity, EntityAILevel aiLevel) {
        UUID entityId = entity.getUniqueId();
        EntityAIState state = this.entityStates.computeIfAbsent(entityId, id -> new EntityAIState(entity));
        if (state.getCurrentLevel() == aiLevel) return;
        switch (aiLevel) {
            case FULL: this.restoreEntityAI(entity, state); break;
            case REDUCED: this.applyReducedAI(entity, state); break;
            case MINIMAL: this.applyMinimalAI(entity, state); break;
            case NONE: this.applyNoAI(entity, state); break;
        }
        state.setCurrentLevel(aiLevel);
    }

    private void restoreEntityAI(LivingEntity entity, EntityAIState state) {
        if (state.getCurrentLevel() != EntityAILevel.FULL) {
            entity.setAI(state.isSavedAware());
            entity.setCollidable(true);
            if (entity instanceof Creature) ((Creature)entity).setTarget(state.getSavedTarget());
        }
    }

    private void applyReducedAI(LivingEntity entity, EntityAIState state) {
        if (state.getCurrentLevel() == EntityAILevel.FULL && entity instanceof Creature) state.setSavedTarget(((Creature)entity).getTarget());
        entity.setAI(true);
        entity.setCollidable(true);
        if (entity instanceof Monster) ((Monster)entity).setTarget(null);
    }

    private void applyMinimalAI(LivingEntity entity, EntityAIState state) {
        if ((state.getCurrentLevel() == EntityAILevel.FULL || state.getCurrentLevel() == EntityAILevel.REDUCED) && entity instanceof Creature) state.setSavedTarget(((Creature)entity).getTarget());
        entity.setAI(true);
        entity.setCollidable(true);
        if (entity instanceof Creature) ((Creature)entity).setTarget(null);
    }

    private void applyNoAI(LivingEntity entity, EntityAIState state) {
        if (state.getCurrentLevel() != EntityAILevel.NONE && entity instanceof Creature) state.setSavedTarget(((Creature)entity).getTarget());
        entity.setAI(false);
        entity.setCollidable(true);
        if (entity instanceof Creature) ((Creature)entity).setTarget(null);
    }

    private void cleanupRemovedEntities(Set<UUID> activeEntities) {
        Iterator<Map.Entry<UUID, EntityAIState>> it = this.entityStates.entrySet().iterator();
        while (it.hasNext()) if (!activeEntities.contains(it.next().getKey())) it.remove();
    }

    private void restoreAllEntityAI() {
        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!(entity instanceof LivingEntity)) continue;
                LivingEntity livingEntity = (LivingEntity)entity;
                EntityAIState state = this.entityStates.get(entity.getUniqueId());
                if (state != null) this.restoreEntityAI(livingEntity, state);
            }
        }
        this.entityStates.clear();
    }

    public Map<String, Object> getStats() {
        HashMap<String, Object> stats = new HashMap<String, Object>();
        stats.put("fullAiEntities", this.fullAiEntities);
        stats.put("reducedAiEntities", this.reducedAiEntities);
        stats.put("minimalAiEntities", this.minimalAiEntities);
        stats.put("noAiEntities", this.noAiEntities);
        stats.put("totalTrackedEntities", this.entityStates.size());
        stats.put("enabled", this.enabled);
        return stats;
    }

    public void reload() {
        this.stopLimiting();
        this.loadConfiguration();
        if (this.enabled) this.startLimiting();
    }

    public void shutdown() {
        this.stopLimiting();
        this.entityStates.clear();
    }

    public enum EntityAILevel { FULL, REDUCED, MINIMAL, NONE }

    private static class EntityAIState {
        private EntityAILevel currentLevel = EntityAILevel.FULL;
        private LivingEntity savedTarget;
        private boolean savedAware = true;

        EntityAIState(LivingEntity entity) {
            if (entity instanceof Mob) this.savedAware = ((Mob)entity).isAware();
            if (entity instanceof Creature) this.savedTarget = ((Creature)entity).getTarget();
        }

        EntityAILevel getCurrentLevel() { return this.currentLevel; }
        void setCurrentLevel(EntityAILevel level) { this.currentLevel = level; }
        LivingEntity getSavedTarget() { return this.savedTarget; }
        void setSavedTarget(LivingEntity target) { this.savedTarget = target; }
        boolean isSavedAware() { return this.savedAware; }
    }
}
