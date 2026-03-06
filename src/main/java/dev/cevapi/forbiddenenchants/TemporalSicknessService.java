package dev.cevapi.forbiddenenchants;

import dev.cevapi.forbiddenenchants.enchants.EnchantList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

final class TemporalSicknessService {
    private final Supplier<EnchantStateService> enchantStateServiceSupplier;
    private final Map<UUID, Long> nextTeleportTick;
    private final Map<UUID, Integer> killCount;
    private final Map<UUID, Integer> lastLevel;

    TemporalSicknessService(@NotNull Supplier<EnchantStateService> enchantStateServiceSupplier,
                            @NotNull Map<UUID, Long> nextTeleportTick,
                            @NotNull Map<UUID, Integer> killCount,
                            @NotNull Map<UUID, Integer> lastLevel) {
        this.enchantStateServiceSupplier = enchantStateServiceSupplier;
        this.nextTeleportTick = nextTeleportTick;
        this.killCount = killCount;
        this.lastLevel = lastLevel;
    }

    void processTick(@NotNull Player player, long tickCounter) {
        TemporalArmorPiece piece = findTemporalSicknessPiece(player);
        UUID id = player.getUniqueId();
        if (piece == null) {
            nextTeleportTick.remove(id);
            lastLevel.remove(id);
            return;
        }

        int level = piece.level();
        Integer previous = lastLevel.get(id);
        if (previous == null || previous != level) {
            lastLevel.put(id, level);
            nextTeleportTick.put(id, tickCounter + nextIntervalTicks(level));
            return;
        }

        long next = nextTeleportTick.getOrDefault(id, tickCounter + nextIntervalTicks(level));
        if (tickCounter < next) {
            return;
        }

        teleportByTemporalSickness(player, level);
        if (level >= 3) {
            player.damage(2.0D);
        }
        nextTeleportTick.put(id, tickCounter + nextIntervalTicks(level));
    }

    void onKill(@NotNull Player killer) {
        TemporalArmorPiece piece = findTemporalSicknessPiece(killer);
        if (piece == null) {
            return;
        }
        int threshold = switch (piece.level()) {
            case 1 -> 10;
            case 2 -> 20;
            default -> 30;
        };
        UUID id = killer.getUniqueId();
        int next = killCount.getOrDefault(id, 0) + 1;
        if (next < threshold) {
            killCount.put(id, next);
            return;
        }

        breakTemporalSicknessArmor(killer, piece.slot());
        killCount.remove(id);
        killer.sendActionBar(Component.text("Temporal Sickness has been broken.", NamedTextColor.GREEN));
    }

    private long nextIntervalTicks(int level) {
        return switch (level) {
            case 1 -> 600L;
            case 2 -> ThreadLocalRandom.current().nextLong(100L, 601L);
            default -> ThreadLocalRandom.current().nextLong(200L, 601L);
        };
    }

    private void teleportByTemporalSickness(@NotNull Player player, int level) {
        World source = player.getWorld();
        World target = nextTemporalDestinationWorld(source);
        if (target == null) {
            return;
        }

        Location destination;
        if (level == 1) {
            destination = equivalentTemporalLocation(player.getLocation(), target);
        } else {
            destination = randomTemporalLocation(target, 10_000.0D);
        }
        if (destination == null) {
            destination = randomTemporalLocation(target, 10_000.0D);
        }
        if (destination == null) {
            return;
        }

        player.teleport(destination);
        player.sendActionBar(Component.text("Temporal Sickness warped you.", NamedTextColor.DARK_PURPLE));
    }

    private @Nullable World nextTemporalDestinationWorld(@NotNull World source) {
        List<World> worlds = temporalWorldCycle();
        if (worlds.size() <= 1) {
            return null;
        }

        int index = -1;
        for (int i = 0; i < worlds.size(); i++) {
            if (worlds.get(i).getUID().equals(source.getUID())) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            return worlds.get(0);
        }
        return worlds.get((index + 1) % worlds.size());
    }

    private @NotNull List<World> temporalWorldCycle() {
        List<World> worlds = new ArrayList<>();
        World overworld = null;
        World nether = null;
        World end = null;
        for (World world : Bukkit.getWorlds()) {
            if (world.getEnvironment() == World.Environment.NORMAL && overworld == null) {
                overworld = world;
            } else if (world.getEnvironment() == World.Environment.NETHER && nether == null) {
                nether = world;
            } else if (world.getEnvironment() == World.Environment.THE_END && end == null) {
                end = world;
            }
        }
        if (overworld != null) {
            worlds.add(overworld);
        }
        if (nether != null) {
            worlds.add(nether);
        }
        if (end != null) {
            worlds.add(end);
        }
        return worlds;
    }

    private @Nullable Location equivalentTemporalLocation(@NotNull Location source, @NotNull World target) {
        World sourceWorld = source.getWorld();
        if (sourceWorld == null) {
            return null;
        }
        double x = source.getX();
        double z = source.getZ();
        if (sourceWorld.getEnvironment() == World.Environment.NORMAL && target.getEnvironment() == World.Environment.NETHER) {
            x /= 8.0D;
            z /= 8.0D;
        } else if (sourceWorld.getEnvironment() == World.Environment.NETHER && target.getEnvironment() == World.Environment.NORMAL) {
            x *= 8.0D;
            z *= 8.0D;
        }
        return safeTemporalLocation(target, x, z, source.getY(), source.getYaw(), source.getPitch());
    }

    private @Nullable Location randomTemporalLocation(@NotNull World world, double maxAbs) {
        WorldBorder border = world.getWorldBorder();
        double half = border.getSize() / 2.0D - 1.0D;
        double minX = Math.max(-maxAbs, border.getCenter().getX() - half);
        double maxX = Math.min(maxAbs, border.getCenter().getX() + half);
        double minZ = Math.max(-maxAbs, border.getCenter().getZ() - half);
        double maxZ = Math.min(maxAbs, border.getCenter().getZ() + half);
        if (minX >= maxX || minZ >= maxZ) {
            return null;
        }

        for (int i = 0; i < 40; i++) {
            double x = ThreadLocalRandom.current().nextDouble(minX, maxX);
            double z = ThreadLocalRandom.current().nextDouble(minZ, maxZ);
            Location safe = safeTemporalLocation(world, x, z, world.getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1.0D, 0.0F, 0.0F);
            if (safe != null) {
                return safe;
            }
        }
        return null;
    }

    private @Nullable Location safeTemporalLocation(@NotNull World world,
                                                    double x,
                                                    double z,
                                                    double preferredY,
                                                    float yaw,
                                                    float pitch) {
        WorldBorder border = world.getWorldBorder();
        Location proposed = new Location(world, x, preferredY, z, yaw, pitch);
        if (!border.isInside(proposed)) {
            return null;
        }

        int blockX = (int) Math.floor(x);
        int blockZ = (int) Math.floor(z);
        int minY = world.getMinHeight() + 1;
        int maxY = world.getMaxHeight() - 2;
        int highest = Math.max(minY, Math.min(maxY, world.getHighestBlockYAt(blockX, blockZ) + 1));
        int preferred = Math.max(minY, Math.min(maxY, (int) Math.floor(preferredY)));

        int topSearchY = Math.min(maxY, Math.max(highest, preferred + 2));
        int bottomSearchY = Math.max(minY + 4, world.getEnvironment() == World.Environment.NETHER ? 16 : minY + 1);
        if (world.getEnvironment() == World.Environment.NETHER) {
            topSearchY = Math.min(topSearchY, 120);
        }

        for (int y = topSearchY; y >= bottomSearchY; y--) {
            Location check = new Location(world, x, y, z, yaw, pitch);
            if (isValidTemporalStandingLocation(world, check)) {
                return check.add(0.5D, 0.0D, 0.5D);
            }
        }
        return null;
    }

    private boolean isValidTemporalStandingLocation(@NotNull World world, @NotNull Location check) {
        var feet = check.getBlock();
        var head = check.clone().add(0.0D, 1.0D, 0.0D).getBlock();
        var floor = check.clone().add(0.0D, -1.0D, 0.0D).getBlock();
        if (!feet.isPassable() || !head.isPassable() || floor.isPassable()) {
            return false;
        }
        if (isDangerousTemporalMaterial(feet.getType()) || isDangerousTemporalMaterial(head.getType())) {
            return false;
        }

        Material floorType = floor.getType();
        if (floorType == Material.BEDROCK && world.getEnvironment() == World.Environment.NETHER) {
            return false;
        }
        if (isDangerousTemporalMaterial(floorType)) {
            return false;
        }

        int y = check.getBlockY();
        if (world.getEnvironment() == World.Environment.NETHER && y > 120) {
            return false;
        }
        if (y <= world.getMinHeight() + 3) {
            return false;
        }
        return y < world.getMaxHeight() - 3;
    }

    private boolean isDangerousTemporalMaterial(@NotNull Material material) {
        return switch (material) {
            case LAVA, FIRE, SOUL_FIRE, MAGMA_BLOCK, CAMPFIRE, SOUL_CAMPFIRE, CACTUS, SWEET_BERRY_BUSH -> true;
            default -> false;
        };
    }

    private @Nullable TemporalArmorPiece findTemporalSicknessPiece(@NotNull Player player) {
        PlayerInventory inv = player.getInventory();
        TemporalArmorPiece best = null;
        best = betterTemporalPiece(best, EquipmentSlot.HEAD, inv.getHelmet());
        best = betterTemporalPiece(best, EquipmentSlot.CHEST, inv.getChestplate());
        best = betterTemporalPiece(best, EquipmentSlot.LEGS, inv.getLeggings());
        best = betterTemporalPiece(best, EquipmentSlot.FEET, inv.getBoots());
        return best;
    }

    private @Nullable TemporalArmorPiece betterTemporalPiece(@Nullable TemporalArmorPiece current,
                                                             @NotNull EquipmentSlot slot,
                                                             @Nullable ItemStack item) {
        int level = enchantStateServiceSupplier.get().getEnchantLevel(item, EnchantType.TEMPORAL_SICKNESS);
        if (!EnchantList.INSTANCE.temporalSickness().isActive(level)) {
            return current;
        }
        if (current == null || level > current.level()) {
            return new TemporalArmorPiece(slot, level);
        }
        return current;
    }

    private void breakTemporalSicknessArmor(@NotNull Player player, @NotNull EquipmentSlot slot) {
        switch (slot) {
            case HEAD -> player.getInventory().setHelmet(new ItemStack(Material.AIR));
            case CHEST -> player.getInventory().setChestplate(new ItemStack(Material.AIR));
            case LEGS -> player.getInventory().setLeggings(new ItemStack(Material.AIR));
            case FEET -> player.getInventory().setBoots(new ItemStack(Material.AIR));
            default -> {
            }
        }
    }
}

