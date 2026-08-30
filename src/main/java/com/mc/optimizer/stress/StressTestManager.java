/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.Bukkit
 *  org.bukkit.ChatColor
 *  org.bukkit.Location
 *  org.bukkit.Material
 *  org.bukkit.World
 *  org.bukkit.block.Block
 *  org.bukkit.command.CommandSender
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.EntityType
 *  org.bukkit.entity.Player
 *  org.bukkit.plugin.Plugin
 *  org.bukkit.scheduler.BukkitTask
 */
package com.mc.optimizer.stress;

import com.mc.optimizer.OptimizerPlugin;
import com.mc.optimizer.config.ConfigManager;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public class StressTestManager {
    private final OptimizerPlugin plugin;
    private final ConfigManager config;
    private final Logger logger;
    private boolean enabled;
    private boolean requireOp;
    private int maxTestEntities;
    private int maxTestBlocks;
    private boolean allowInProduction;
    private boolean autoCleanup;
    private final Map<UUID, StressTest> activeTests = new ConcurrentHashMap<UUID, StressTest>();
    private final AtomicInteger nextTestId = new AtomicInteger(1);

    public StressTestManager(OptimizerPlugin plugin, ConfigManager config) {
        this.plugin = plugin;
        this.config = config;
        this.logger = plugin.getLogger();
        this.loadConfiguration();
    }

    private void loadConfiguration() {
        this.enabled = false;
        this.requireOp = true;
        this.maxTestEntities = 500;
        this.maxTestBlocks = 10000;
        this.allowInProduction = false;
        this.autoCleanup = true;
        try {
            this.enabled = false;
            if (this.config != null) {
                try {
                    Method method = this.config.getClass().getMethod("isStressTestEnabled", new Class[0]);
                    this.enabled = (Boolean)method.invoke((Object)this.config, new Object[0]);
                }
                catch (Exception e) {
                    this.logger.warning("Could not load stress test configuration: " + e.getMessage());
                }
            }
        }
        catch (Exception e) {
            this.logger.warning("Error loading stress test configuration: " + e.getMessage());
        }
    }

    public boolean canRunTests(Player player) {
        if (!this.enabled) {
            return false;
        }
        if (this.requireOp && !player.isOp()) {
            return false;
        }
        return this.allowInProduction || Bukkit.getServer().getWorldType().equals("FLAT");
    }

    public boolean startEntityTest(final Player player, final EntityType entityType, final int count) {
        if (!this.canRunTests(player)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "You do not have permission to run stress tests.");
            return false;
        }
        final UUID playerId = player.getUniqueId();
        if (this.activeTests.containsKey(playerId)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "You already have an active stress test. Please end it first.");
            return false;
        }
        if (count > this.maxTestEntities) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "The maximum number of test entities is " + this.maxTestEntities + ".");
            return false;
        }
        int testId = this.nextTestId.getAndIncrement();
        final StressTest test = new StressTest(testId, player, StressTestType.ENTITY);
        this.activeTests.put(playerId, test);
        final Location location = player.getLocation();
        final World world = player.getWorld();
        this.logger.info("Starting entity stress test: " + count + " " + entityType.name() + " entities by " + player.getName());
        player.sendMessage(String.valueOf(ChatColor.GREEN) + "Starting entity stress test #" + testId + ". Spawning " + count + " " + entityType.name() + " entities.");
        final int batchSize = 20;
        final int batches = (count + batchSize - 1) / batchSize;
        BukkitTask spawnTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, new Runnable(){
            private int batchesCompleted = 0;
            private int entitiesSpawned = 0;

            @Override
            public void run() {
                if (this.batchesCompleted >= batches) {
                    player.sendMessage(String.valueOf(ChatColor.GREEN) + "Entity stress test completed. " + this.entitiesSpawned + " entities spawned.");
                    Bukkit.getScheduler().cancelTask(test.getTaskId());
                    test.setTaskId(-1);
                    if (StressTestManager.this.autoCleanup) {
                        Bukkit.getScheduler().runTaskLater((Plugin)StressTestManager.this.plugin, () -> {
                            if (StressTestManager.this.activeTests.containsKey(playerId)) {
                                StressTestManager.this.endTest(player);
                            }
                        }, 1200L);
                    }
                    return;
                }
                int toSpawn = Math.min(batchSize, count - this.entitiesSpawned);
                for (int i = 0; i < toSpawn; ++i) {
                    double offsetX = (Math.random() - 0.5) * 10.0;
                    double offsetZ = (Math.random() - 0.5) * 10.0;
                    Location spawnLoc = location.clone().add(offsetX, 1.0, offsetZ);
                    Entity entity = world.spawnEntity(spawnLoc, entityType);
                    test.addEntity(entity);
                    ++this.entitiesSpawned;
                }
                ++this.batchesCompleted;
                if (this.batchesCompleted % 5 == 0 || this.batchesCompleted == batches) {
                    player.sendMessage(String.valueOf(ChatColor.AQUA) + "Spawned " + this.entitiesSpawned + "/" + count + " entities (" + this.batchesCompleted * 100 / batches + "%)");
                }
            }
        }, 1L, 5L);
        test.setTaskId(spawnTask.getTaskId());
        return true;
    }

    public boolean startBlockTest(final Player player, final Material material, final int radius) {
        if (!this.canRunTests(player)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "You do not have permission to run stress tests.");
            return false;
        }
        final UUID playerId = player.getUniqueId();
        if (this.activeTests.containsKey(playerId)) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "You already have an active stress test. Please end it first.");
            return false;
        }
        int size = radius * 2 + 1;
        final int count = size * size * size;
        if (count > this.maxTestBlocks) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "The maximum number of test blocks is " + this.maxTestBlocks + ". Your parameters would create " + count + " blocks.");
            return false;
        }
        int testId = this.nextTestId.getAndIncrement();
        final StressTest test = new StressTest(testId, player, StressTestType.BLOCK);
        this.activeTests.put(playerId, test);
        final Location center = player.getLocation();
        World world = player.getWorld();
        this.logger.info("Starting block stress test: " + count + " " + material.name() + " blocks by " + player.getName());
        player.sendMessage(String.valueOf(ChatColor.GREEN) + "Starting block stress test #" + testId + ". Placing " + count + " " + material.name() + " blocks.");
        final HashMap originalBlocks = new HashMap();
        final int batchSize = 100;
        final int batches = (count + batchSize - 1) / batchSize;
        BukkitTask placeTask = Bukkit.getScheduler().runTaskTimer((Plugin)this.plugin, new Runnable(){
            private int batchesCompleted = 0;
            private int blocksPlaced = 0;
            private int x = -radius;
            private int y = -radius;
            private int z = -radius;

            @Override
            public void run() {
                if (this.batchesCompleted >= batches) {
                    player.sendMessage(String.valueOf(ChatColor.GREEN) + "Block stress test completed. " + this.blocksPlaced + " blocks placed.");
                    Bukkit.getScheduler().cancelTask(test.getTaskId());
                    test.setTaskId(-1);
                    test.setOriginalBlocks(originalBlocks);
                    if (StressTestManager.this.autoCleanup) {
                        Bukkit.getScheduler().runTaskLater((Plugin)StressTestManager.this.plugin, () -> {
                            if (StressTestManager.this.activeTests.containsKey(playerId)) {
                                StressTestManager.this.endTest(player);
                            }
                        }, 1200L);
                    }
                    return;
                }
                int toPlace = Math.min(batchSize, count - this.blocksPlaced);
                for (int i = 0; i < toPlace; ++i) {
                    if (this.x > radius) {
                        this.x = -radius;
                        ++this.z;
                        if (this.z > radius) {
                            this.z = -radius;
                            ++this.y;
                            if (this.y > radius) break;
                        }
                    }
                    Location blockLoc = center.clone().add((double)this.x, (double)this.y, (double)this.z);
                    Block block = blockLoc.getBlock();
                    originalBlocks.put(blockLoc, block.getType());
                    block.setType(material);
                    test.addBlock(blockLoc);
                    ++this.blocksPlaced;
                    ++this.x;
                }
                ++this.batchesCompleted;
                if (this.batchesCompleted % 5 == 0 || this.batchesCompleted == batches) {
                    player.sendMessage(String.valueOf(ChatColor.AQUA) + "Placed " + this.blocksPlaced + "/" + count + " blocks (" + this.blocksPlaced * 100 / count + "%)");
                }
            }
        }, 1L, 1L);
        test.setTaskId(placeTask.getTaskId());
        return true;
    }

    public boolean endTest(Player player) {
        UUID playerId = player.getUniqueId();
        StressTest test = this.activeTests.get(playerId);
        if (test == null) {
            player.sendMessage(String.valueOf(ChatColor.RED) + "You don't have an active stress test.");
            return false;
        }
        if (test.getTaskId() != -1) {
            Bukkit.getScheduler().cancelTask(test.getTaskId());
        }
        if (test.getType() == StressTestType.ENTITY) {
            for (Entity entity : test.getEntities()) {
                if (entity == null || entity.isDead()) continue;
                entity.remove();
            }
            player.sendMessage(String.valueOf(ChatColor.GREEN) + "Removed " + test.getEntities().size() + " test entities.");
        } else if (test.getType() == StressTestType.BLOCK) {
            Map<Location, Material> originalBlocks = test.getOriginalBlocks();
            if (originalBlocks != null) {
                int restored = 0;
                for (Map.Entry<Location, Material> entry : originalBlocks.entrySet()) {
                    Location loc = entry.getKey();
                    Material material = entry.getValue();
                    Block block = loc.getBlock();
                    block.setType(material);
                    ++restored;
                }
                player.sendMessage(String.valueOf(ChatColor.GREEN) + "Restored " + restored + " original blocks.");
            } else {
                for (Location loc : test.getBlocks()) {
                    Block block = loc.getBlock();
                    block.setType(Material.AIR);
                }
                player.sendMessage(String.valueOf(ChatColor.GREEN) + "Removed " + test.getBlocks().size() + " test blocks.");
            }
        }
        this.activeTests.remove(playerId);
        player.sendMessage(String.valueOf(ChatColor.GREEN) + "Stress test #" + test.getId() + " ended.");
        return true;
    }

    public int endAllTests(CommandSender sender) {
        int count = 0;
        for (UUID playerId : new ArrayList<UUID>(this.activeTests.keySet())) {
            Player player = Bukkit.getPlayer((UUID)playerId);
            if (player != null && player.isOnline()) {
                if (!this.endTest(player)) continue;
                ++count;
                continue;
            }
            StressTest test = this.activeTests.remove(playerId);
            if (test == null) continue;
            if (test.getTaskId() != -1) {
                Bukkit.getScheduler().cancelTask(test.getTaskId());
            }
            if (test.getType() == StressTestType.ENTITY) {
                for (Entity entity : test.getEntities()) {
                    if (entity == null || entity.isDead()) continue;
                    entity.remove();
                }
            } else if (test.getType() == StressTestType.BLOCK) {
                Map<Location, Material> originalBlocks = test.getOriginalBlocks();
                if (originalBlocks != null) {
                    for (Map.Entry<Location, Material> entry : originalBlocks.entrySet()) {
                        Location loc = entry.getKey();
                        Material material = entry.getValue();
                        Block block = loc.getBlock();
                        block.setType(material);
                    }
                } else {
                    for (Location loc : test.getBlocks()) {
                        Block block = loc.getBlock();
                        block.setType(Material.AIR);
                    }
                }
            }
            ++count;
        }
        if (count > 0) {
            sender.sendMessage(String.valueOf(ChatColor.GREEN) + "Ended " + count + " stress tests.");
        } else {
            sender.sendMessage(String.valueOf(ChatColor.YELLOW) + "No active stress tests to end.");
        }
        return count;
    }

    public Map<String, Object> getStats() {
        HashMap<String, Object> stats = new HashMap<String, Object>();
        stats.put("enabled", this.enabled);
        stats.put("activeTests", this.activeTests.size());
        int totalEntities = 0;
        int totalBlocks = 0;
        for (StressTest test : this.activeTests.values()) {
            if (test.getType() == StressTestType.ENTITY) {
                totalEntities += test.getEntities().size();
                continue;
            }
            if (test.getType() != StressTestType.BLOCK) continue;
            totalBlocks += test.getBlocks().size();
        }
        stats.put("totalEntities", totalEntities);
        stats.put("totalBlocks", totalBlocks);
        return stats;
    }

    public void reload() {
        this.endAllTests((CommandSender)Bukkit.getConsoleSender());
        this.loadConfiguration();
    }

    public void shutdown() {
        this.endAllTests((CommandSender)Bukkit.getConsoleSender());
        this.logger.info("Stress test manager shutdown");
    }

    private static class StressTest {
        private final int id;
        private final Player player;
        private final StressTestType type;
        private final List<Entity> entities;
        private final List<Location> blocks;
        private Map<Location, Material> originalBlocks;
        private int taskId;

        public StressTest(int id, Player player, StressTestType type) {
            this.id = id;
            this.player = player;
            this.type = type;
            this.entities = new ArrayList<Entity>();
            this.blocks = new ArrayList<Location>();
            this.originalBlocks = null;
            this.taskId = -1;
        }

        public int getId() {
            return this.id;
        }

        public Player getPlayer() {
            return this.player;
        }

        public StressTestType getType() {
            return this.type;
        }

        public List<Entity> getEntities() {
            return this.entities;
        }

        public void addEntity(Entity entity) {
            this.entities.add(entity);
        }

        public List<Location> getBlocks() {
            return this.blocks;
        }

        public void addBlock(Location location) {
            this.blocks.add(location);
        }

        public Map<Location, Material> getOriginalBlocks() {
            return this.originalBlocks;
        }

        public void setOriginalBlocks(Map<Location, Material> originalBlocks) {
            this.originalBlocks = originalBlocks;
        }

        public int getTaskId() {
            return this.taskId;
        }

        public void setTaskId(int taskId) {
            this.taskId = taskId;
        }
    }

    private static enum StressTestType {
        ENTITY,
        BLOCK;

    }
}

