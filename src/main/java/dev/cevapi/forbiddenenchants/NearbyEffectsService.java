package dev.cevapi.forbiddenenchants;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

final class NearbyEffectsService {
    void clearNearbyDarkness(@NotNull Location center, double radius) {
        var world = center.getWorld();
        if (world == null) {
            return;
        }
        double radiusSquared = radius * radius;
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(center) <= radiusSquared) {
                player.removePotionEffect(PotionEffectType.DARKNESS);
            }
        }
    }

    void pullNearbyExperienceOrbs(@NotNull Player player, double radius) {
        Location feet = player.getLocation().add(0.0D, 0.1D, 0.0D);
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof ExperienceOrb orb) || !orb.isValid() || orb.isDead()) {
                continue;
            }
            Vector toward = feet.toVector().subtract(orb.getLocation().toVector());
            if (toward.lengthSquared() < 0.0001D) {
                continue;
            }
            orb.setVelocity(toward.normalize().multiply(0.55D));
        }
    }

    void pullNearbyItems(@NotNull Player player, double radius) {
        Location feet = player.getLocation().add(0.0D, 0.1D, 0.0D);
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Item item) || !item.isValid() || item.isDead()) {
                continue;
            }
            Vector toward = feet.toVector().subtract(item.getLocation().toVector());
            if (toward.lengthSquared() < 0.0001D) {
                continue;
            }
            item.setVelocity(toward.normalize().multiply(0.65D));
        }
    }
}

