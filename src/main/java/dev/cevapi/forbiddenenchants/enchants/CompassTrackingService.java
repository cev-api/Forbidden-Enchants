package dev.cevapi.forbiddenenchants.enchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class CompassTrackingService {
    public void updateCompassTracking(@NotNull Player player,
                                      @NotNull ItemStack compass,
                                      @NotNull EquipmentSlot slot,
                                      int graveLevel,
                                      int seekerLevel,
                                      @NotNull Map<UUID, Location> lastPlayerDeathLocations) {
        if (graveLevel <= 0 && seekerLevel <= 0) {
            return;
        }

        if (!(compass.getItemMeta() instanceof CompassMeta meta)) {
            sendCompassReadout(player, null, false);
            return;
        }

        Location target;
        boolean hasSignal;
        if (seekerLevel > 0) {
            Location nearest = nearestLivingPlayerLocation(player);
            hasSignal = nearest != null && player.getLocation().distance(nearest) <= maxCompassSearchRange(seekerLevel);
            target = hasSignal ? nearest : spinningCompassTarget(player);
            sendCompassReadout(player, target, hasSignal);
        } else {
            Location nearestDeath = nearestDeathLocation(player, lastPlayerDeathLocations);
            hasSignal = nearestDeath != null && player.getLocation().distance(nearestDeath) <= maxCompassSearchRange(graveLevel);
            target = hasSignal ? nearestDeath : spinningCompassTarget(player);
            sendCompassReadout(player, target, hasSignal);
        }

        boolean changedMeta = false;
        if (meta.getLodestone() != null) {
            meta.setLodestone(null);
            changedMeta = true;
        }
        if (meta.isLodestoneTracked()) {
            meta.setLodestoneTracked(false);
            changedMeta = true;
        }
        if (changedMeta) {
            compass.setItemMeta(meta);
            if (slot == EquipmentSlot.HAND) {
                player.getInventory().setItemInMainHand(compass);
            } else {
                player.getInventory().setItemInOffHand(compass);
            }
        }

        if (target.getWorld() != null && target.getWorld().equals(player.getWorld())) {
            player.setCompassTarget(target);
        } else {
            player.setCompassTarget(spinningCompassTarget(player));
        }
    }

    private void sendCompassReadout(@NotNull Player player,
                                    @Nullable Location target,
                                    boolean hasSignal) {
        if (target == null || target.getWorld() == null || !target.getWorld().equals(player.getWorld())) {
            Component out = Component.empty()
                    .append(obfuscatedDigits(5, true))
                    .append(Component.text(" ", NamedTextColor.DARK_GRAY))
                    .append(obfuscatedDigits(4, true))
                    .append(Component.text(" ", NamedTextColor.DARK_GRAY))
                    .append(obfuscatedDigits(5, true));
            player.sendActionBar(out);
            return;
        }

        double distance = player.getLocation().distance(target);
        double revealFraction = hasSignal ? compassRevealFraction(distance) : 0.0D;
        Component out = Component.empty()
                .append(obfuscatedNumericByReveal(Integer.toString(target.getBlockX()), revealFraction))
                .append(Component.text(" ", NamedTextColor.DARK_GRAY))
                .append(obfuscatedNumericByReveal(Integer.toString(target.getBlockY()), revealFraction))
                .append(Component.text(" ", NamedTextColor.DARK_GRAY))
                .append(obfuscatedNumericByReveal(Integer.toString(target.getBlockZ()), revealFraction));
        player.sendActionBar(out);
    }

    private double compassRevealFraction(double distance) {
        double d = Math.max(0.0D, distance);
        if (d <= 10.0D) {
            return 1.0D;
        }
        if (d <= 25.0D) {
            double t = (d - 10.0D) / 15.0D;
            return clamp01(1.0D - (0.20D * t));
        }
        if (d <= 60.0D) {
            double t = (d - 25.0D) / 35.0D;
            return clamp01(0.80D - (0.25D * t));
        }
        if (d <= 150.0D) {
            double t = (d - 60.0D) / 90.0D;
            return clamp01(0.55D - (0.25D * t));
        }
        if (d <= 400.0D) {
            double t = (d - 150.0D) / 250.0D;
            return clamp01(0.30D - (0.15D * t));
        }
        if (d <= 1000.0D) {
            double t = (d - 400.0D) / 600.0D;
            return clamp01(0.15D - (0.10D * t));
        }
        return 0.05D;
    }

    private double clamp01(double value) {
        if (value < 0.0D) {
            return 0.0D;
        }
        if (value > 1.0D) {
            return 1.0D;
        }
        return value;
    }

    private @NotNull Component obfuscatedNumericByReveal(@NotNull String realValue, double revealFraction) {
        int digits = 0;
        for (int i = 0; i < realValue.length(); i++) {
            char c = realValue.charAt(i);
            if (c >= '0' && c <= '9') {
                digits++;
            }
        }

        int revealDigits = (int) Math.round(digits * clamp01(revealFraction));
        if (revealDigits > digits) {
            revealDigits = digits;
        } else if (revealDigits < 0) {
            revealDigits = 0;
        }

        Component out = Component.empty();
        int digitIndex = 0;
        for (int i = 0; i < realValue.length(); i++) {
            char c = realValue.charAt(i);
            if (c == '-') {
                out = out.append(Component.text("-", NamedTextColor.AQUA));
                continue;
            }
            if (c < '0' || c > '9') {
                out = out.append(Component.text(String.valueOf(c), NamedTextColor.AQUA));
                continue;
            }

            boolean reveal;
            if (digits <= 0 || revealDigits <= 0) {
                reveal = false;
            } else if (revealDigits >= digits) {
                reveal = true;
            } else {
                int left = (digitIndex * revealDigits) / digits;
                int right = ((digitIndex + 1) * revealDigits) / digits;
                reveal = right > left;
            }
            digitIndex++;
            out = reveal
                    ? out.append(Component.text(String.valueOf(c), NamedTextColor.AQUA))
                    : out.append(obfuscatedDigits(1, true));
        }
        return out;
    }

    private @NotNull Component obfuscatedDigits(int count, boolean aqua) {
        Component out = Component.empty();
        NamedTextColor color = aqua ? NamedTextColor.AQUA : NamedTextColor.DARK_AQUA;
        for (int i = 0; i < Math.max(1, count); i++) {
            out = out.append(Component.text("X", color).decorate(TextDecoration.OBFUSCATED));
        }
        return out;
    }

    private @Nullable Location nearestDeathLocation(@NotNull Player seeker,
                                                    @NotNull Map<UUID, Location> lastPlayerDeathLocations) {
        Location source = seeker.getLocation();
        Location best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Location death : lastPlayerDeathLocations.values()) {
            if (death == null || death.getWorld() == null || !death.getWorld().equals(source.getWorld())) {
                continue;
            }
            double distance = death.distanceSquared(source);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = death;
            }
        }
        return best;
    }

    private @Nullable Location nearestLivingPlayerLocation(@NotNull Player seeker) {
        Location source = seeker.getLocation();
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player candidate : Bukkit.getOnlinePlayers()) {
            if (candidate.getUniqueId().equals(seeker.getUniqueId()) || candidate.isDead() || !candidate.isValid()) {
                continue;
            }
            if (!candidate.getWorld().equals(source.getWorld())) {
                continue;
            }
            double distance = candidate.getLocation().distanceSquared(source);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best == null ? null : best.getLocation();
    }

    private double maxCompassSearchRange(int level) {
        return switch (Math.max(1, Math.min(4, level))) {
            case 1 -> 1000.0D;
            case 2 -> 2500.0D;
            case 3 -> 5000.0D;
            default -> 10000.0D;
        };
    }

    private @NotNull Location spinningCompassTarget(@NotNull Player player) {
        double angle = ThreadLocalRandom.current().nextDouble(0.0D, Math.PI * 2.0D);
        double radius = ThreadLocalRandom.current().nextDouble(48.0D, 512.0D);
        double x = player.getLocation().getX() + Math.cos(angle) * radius;
        double z = player.getLocation().getZ() + Math.sin(angle) * radius;
        int y = player.getWorld().getHighestBlockYAt((int) Math.floor(x), (int) Math.floor(z)) + 1;
        return new Location(player.getWorld(), x, y, z);
    }
}

