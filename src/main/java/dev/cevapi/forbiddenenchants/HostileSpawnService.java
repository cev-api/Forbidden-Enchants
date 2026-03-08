package dev.cevapi.forbiddenenchants;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

final class HostileSpawnService {
    void applyHatedOneAggro(@NotNull Player player, int level) {
        double radius = level >= 2 ? 28.0D : 14.0D;
        for (Entity nearby : player.getNearbyEntities(radius, radius, radius)) {
            if (nearby instanceof Mob mob && mob.isValid() && !mob.isDead()) {
                mob.setTarget(player);
            }
        }
    }

    void trySpawnHatedOneWave(@NotNull Player player, int level) {
        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.NORMAL) {
            return;
        }
        double spawnChance = level >= 2 ? 0.60D : 0.30D;
        if (ThreadLocalRandom.current().nextDouble() > spawnChance) {
            return;
        }

        int minSpawns = level >= 2 ? 2 : 1;
        int maxSpawnsExclusive = level >= 2 ? 5 : 3;
        int spawns = ThreadLocalRandom.current().nextInt(minSpawns, maxSpawnsExclusive);
        for (int i = 0; i < spawns; i++) {
            Location spawn = findNearbySpawnLocation(player, 10.0D, 20.0D);
            if (spawn == null) {
                continue;
            }

            double raiderChance = level >= 2 ? 0.70D : 0.35D;
            EntityType type = ThreadLocalRandom.current().nextDouble() < raiderChance
                    ? randomRaiderType(level)
                    : randomHostileType();
            Entity created = world.spawnEntity(spawn, type);
            if (created instanceof Mob mob) {
                mob.setTarget(player);
            }
        }
    }

    @NotNull EntityType randomHostileType() {
        EntityType[] hostiles = new EntityType[]{
                EntityType.ZOMBIE,
                EntityType.SKELETON,
                EntityType.SPIDER,
                EntityType.CREEPER,
                EntityType.HUSK,
                EntityType.STRAY,
                EntityType.DROWNED,
                EntityType.SLIME
        };
        return hostiles[ThreadLocalRandom.current().nextInt(hostiles.length)];
    }

    @Nullable Location findNearbySpawnLocation(@NotNull Player player, double minDistance, double maxDistance) {
        World world = player.getWorld();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        int playerY = Math.max(minY + 1, Math.min(maxY - 1, player.getLocation().getBlockY()));

        for (int i = 0; i < 8; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = random.nextDouble(minDistance, maxDistance);
            int x = (int) Math.floor(player.getLocation().getX() + Math.cos(angle) * radius);
            int z = (int) Math.floor(player.getLocation().getZ() + Math.sin(angle) * radius);

            Location localY = findSpawnAtOrNearY(world, x, z, playerY, minY, maxY);
            if (localY != null) {
                return localY;
            }

            int y = Math.max(minY + 1, Math.min(maxY - 1, world.getHighestBlockYAt(x, z) + 1));
            Location surface = new Location(world, x + 0.5D, y, z + 0.5D);
            if (isSafeSpawnLocation(surface)) {
                return surface;
            }
        }

        return null;
    }

    private @Nullable Location findSpawnAtOrNearY(@NotNull World world, int x, int z, int playerY, int minY, int maxY) {
        for (int offset = 0; offset <= 8; offset++) {
            int downY = playerY - offset;
            if (downY > minY + 1) {
                Location candidate = new Location(world, x + 0.5D, downY, z + 0.5D);
                if (isSafeSpawnLocation(candidate)) {
                    return candidate;
                }
            }
            if (offset == 0) {
                continue;
            }
            int upY = playerY + offset;
            if (upY < maxY - 1) {
                Location candidate = new Location(world, x + 0.5D, upY, z + 0.5D);
                if (isSafeSpawnLocation(candidate)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private boolean isSafeSpawnLocation(@NotNull Location location) {
        if (!location.getBlock().isPassable()) {
            return false;
        }
        if (!location.clone().add(0.0D, 1.0D, 0.0D).getBlock().isPassable()) {
            return false;
        }
        return !location.clone().add(0.0D, -1.0D, 0.0D).getBlock().isPassable();
    }

    private @NotNull EntityType randomRaiderType(int level) {
        EntityType[] raiders = level >= 2
                ? new EntityType[]{
                EntityType.PILLAGER,
                EntityType.VINDICATOR,
                EntityType.WITCH,
                EntityType.ILLUSIONER,
                EntityType.EVOKER,
                EntityType.RAVAGER
        }
                : new EntityType[]{
                EntityType.PILLAGER,
                EntityType.VINDICATOR,
                EntityType.WITCH,
                EntityType.EVOKER,
                EntityType.RAVAGER
        };
        return raiders[ThreadLocalRandom.current().nextInt(raiders.length)];
    }
}

