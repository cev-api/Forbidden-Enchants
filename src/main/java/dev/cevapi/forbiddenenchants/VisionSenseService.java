package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

final class VisionSenseService {
    private final PotionEffect divineGlowEffect;

    VisionSenseService(@NotNull PotionEffect divineGlowEffect) {
        this.divineGlowEffect = divineGlowEffect;
    }

    void applyDivineVision(@NotNull Player player, int level) {
        double radius = switch (level) {
            case 1 -> 10.0D;
            case 2 -> 20.0D;
            case 3 -> 30.0D;
            default -> 50.0D;
        };

        double radiusSquared = radius * radius;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || entity.equals(player)) {
                continue;
            }
            if (entity.getLocation().distanceSquared(player.getLocation()) > radiusSquared) {
                continue;
            }
            living.addPotionEffect(divineGlowEffect, true);
        }
    }

    void applyDivineVisionLineOfSight(@NotNull Player player, double radius) {
        double radiusSquared = radius * radius;
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || entity.equals(player)) {
                continue;
            }
            if (entity.getLocation().distanceSquared(player.getLocation()) > radiusSquared) {
                continue;
            }
            if (!player.hasLineOfSight(entity)) {
                continue;
            }
            living.addPotionEffect(divineGlowEffect, true);
        }
    }

    void applyMinersIntuition(@NotNull Player player, @NotNull ItemStack helmet, int level) {
        int radius = switch (level) {
            case 1 -> 10;
            case 2 -> 20;
            case 3 -> 30;
            default -> 50;
        };

        MinersTheme theme = getMinersThemeForHelmet(helmet);
        World world = player.getWorld();
        Location trailStart = player.getEyeLocation();
        Location origin = player.getLocation();

        int minY = Math.max(world.getMinHeight(), origin.getBlockY() - radius);
        int maxY = Math.min(world.getMaxHeight() - 1, origin.getBlockY() + radius);
        int centerX = origin.getBlockX();
        int centerZ = origin.getBlockZ();

        Location nearestTarget = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    Material material = world.getBlockAt(x, y, z).getType();
                    if (!theme.matches(material)) {
                        continue;
                    }

                    Location candidate = new Location(world, x + 0.5, y + 0.5, z + 0.5);
                    double distanceSquared = candidate.distanceSquared(origin);
                    if (distanceSquared < nearestDistanceSquared) {
                        nearestDistanceSquared = distanceSquared;
                        nearestTarget = candidate;
                    }
                }
            }
        }

        if (nearestTarget == null) {
            return;
        }

        Location cueTarget = nearestTarget.clone();
        var towardPlayer = player.getEyeLocation().toVector().subtract(nearestTarget.toVector());
        if (towardPlayer.lengthSquared() > 0.0001D) {
            double oreDistance = Math.sqrt(nearestDistanceSquared);
            double cueDistance = Math.min(5.0D, Math.max(0.75D, oreDistance - 0.75D));
            cueTarget = nearestTarget.clone().add(towardPlayer.normalize().multiply(cueDistance));
        }

        player.spawnParticle(Particle.ENCHANT, cueTarget, 18, 0.18, 0.18, 0.18, 0.0);
        spawnTracerParticles(player, trailStart, cueTarget, Particle.ENCHANT, 24);
        applyCloseEndHint(player, trailStart, nearestTarget, nearestDistanceSquared);
    }

    void applyLootSense(@NotNull Player player, int level) {
        int radius = switch (level) {
            case 1 -> 20;
            case 2 -> 30;
            default -> 50;
        };

        int verticalRadius = Math.min(12, Math.max(6, radius / 2));
        World world = player.getWorld();
        Location origin = player.getLocation();
        Location trailStart = player.getEyeLocation();

        int minY = Math.max(world.getMinHeight(), origin.getBlockY() - verticalRadius);
        int maxY = Math.min(world.getMaxHeight() - 1, origin.getBlockY() + verticalRadius);
        int centerX = origin.getBlockX();
        int centerZ = origin.getBlockZ();

        Location nearestTarget = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                    Material material = world.getBlockAt(x, y, z).getType();
                    if (!isLootSenseTarget(material)) {
                        continue;
                    }

                    Location candidate = new Location(world, x + 0.5, y + 0.5, z + 0.5);
                    double distanceSquared = candidate.distanceSquared(origin);
                    if (distanceSquared < nearestDistanceSquared) {
                        nearestDistanceSquared = distanceSquared;
                        nearestTarget = candidate;
                    }
                }
            }
        }

        if (nearestTarget == null) {
            return;
        }

        Location cueTarget = nearestTarget.clone();
        var towardPlayer = player.getEyeLocation().toVector().subtract(nearestTarget.toVector());
        if (towardPlayer.lengthSquared() > 0.0001D) {
            double lootDistance = Math.sqrt(nearestDistanceSquared);
            double cueDistance = Math.min(5.0D, Math.max(0.75D, lootDistance - 0.75D));
            cueTarget = nearestTarget.clone().add(towardPlayer.normalize().multiply(cueDistance));
        }

        player.spawnParticle(Particle.ENCHANT, cueTarget, 18, 0.18, 0.18, 0.18, 0.0);
        spawnTracerParticles(player, trailStart, cueTarget, Particle.ENCHANT, 24);
        applyCloseEndHint(player, trailStart, nearestTarget, nearestDistanceSquared);
    }

    boolean isLootSenseTarget(@NotNull Material material) {
        if (material == Material.CHEST || material == Material.TRAPPED_CHEST || material == Material.BARREL) {
            return true;
        }
        return material.name().endsWith("SHULKER_BOX");
    }

    void spawnWorldTrailParticles(@NotNull World world,
                                  @NotNull Location from,
                                  @NotNull Location to,
                                  @NotNull Particle particle,
                                  int maxSteps) {
        var step = to.toVector().subtract(from.toVector());
        double distance = step.length();
        if (distance <= 0.001) {
            return;
        }

        step.normalize().multiply(0.75);
        Location point = from.clone();
        int steps = Math.min(maxSteps, (int) (distance / 0.75));

        for (int i = 0; i < steps; i++) {
            point.add(step);
            world.spawnParticle(particle, point, 2, 0.04, 0.04, 0.04, 0.0);
        }
    }

    private void applyCloseEndHint(@NotNull Player player,
                                   @NotNull Location from,
                                   @NotNull Location target,
                                   double distanceSquared) {
        if (distanceSquared > 9.0D) {
            return;
        }
        spawnTracerParticles(player, from, target, Particle.PORTAL, 16);
        player.spawnParticle(Particle.PORTAL, target, 20, 0.22, 0.22, 0.22, 0.02);
    }

    private void spawnTracerParticles(@NotNull Player player,
                                      @NotNull Location from,
                                      @NotNull Location to,
                                      @NotNull Particle particle,
                                      int maxSteps) {
        var step = to.toVector().subtract(from.toVector());
        double distance = step.length();
        if (distance <= 0.001) {
            return;
        }

        step.normalize().multiply(0.8);
        Location point = from.clone();
        int steps = Math.min(maxSteps, (int) (distance / 0.8));

        for (int i = 0; i < steps; i++) {
            point.add(step);
            player.spawnParticle(particle, point, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    private @NotNull MinersTheme getMinersThemeForHelmet(@NotNull ItemStack helmet) {
        Material material = helmet.getType();
        String typeName = material.name();

        if (material == Material.TURTLE_HELMET || typeName.contains("TURTLE")) {
            return MinersTheme.TURTLE;
        }
        if (typeName.contains("NETHERITE")) {
            return MinersTheme.NETHERITE;
        }
        if (typeName.contains("DIAMOND")) {
            return MinersTheme.DIAMOND;
        }
        if (typeName.contains("GOLDEN") || typeName.contains("GOLD")) {
            return MinersTheme.GOLD;
        }
        if (typeName.contains("IRON")) {
            return MinersTheme.IRON;
        }
        if (typeName.contains("CHAINMAIL")) {
            return MinersTheme.CHAINMAIL;
        }
        if (typeName.contains("LEATHER")) {
            return MinersTheme.LEATHER;
        }
        if (typeName.contains("COPPER")) {
            return MinersTheme.COPPER;
        }

        ItemMeta meta = helmet.getItemMeta();
        if (meta != null && meta.displayName() != null) {
            String plainName = PlainTextComponentSerializer.plainText()
                    .serialize(meta.displayName())
                    .toLowerCase(Locale.ROOT);
            if (plainName.contains("netherite")) {
                return MinersTheme.NETHERITE;
            }
            if (plainName.contains("turtle")) {
                return MinersTheme.TURTLE;
            }
            if (plainName.contains("diamond")) {
                return MinersTheme.DIAMOND;
            }
            if (plainName.contains("gold")) {
                return MinersTheme.GOLD;
            }
            if (plainName.contains("iron")) {
                return MinersTheme.IRON;
            }
            if (plainName.contains("chainmail")) {
                return MinersTheme.CHAINMAIL;
            }
            if (plainName.contains("leather")) {
                return MinersTheme.LEATHER;
            }
            if (plainName.contains("copper")) {
                return MinersTheme.COPPER;
            }
        }

        return MinersTheme.DIAMOND;
    }
}

