package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class DragonsBreathEnchant extends BaseForbiddenEnchant {
    public DragonsBreathEnchant() {
        super("dragons_breath",
                "dragons_breath_level",
                "Dragons Breath",
                ArmorSlot.RANGED,
                2,
                NamedTextColor.DARK_PURPLE,
                List.of("dragon", "dragonsbreath", "dragon_breath"),
                "Apply to a crossbow in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Crossbow shots leave dragon-breath clouds for " + (level >= 2 ? "15s" : "5s") + " that harm mobs and players.";
    }

    public void tagProjectile(@NotNull Projectile projectile, int level, @NotNull NamespacedKey key) {
        if (level > 0) {
            projectile.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, level);
        }
    }

    public void onImpact(@NotNull JavaPlugin plugin,
                         @NotNull Projectile projectile,
                         @NotNull Location impact,
                         int level) {
        World world = impact.getWorld();
        if (world == null || level <= 0) {
            return;
        }

        int clampedLevel = Math.max(1, Math.min(2, level));
        int durationTicks = clampedLevel == 2 ? 300 : 100;
        float visibleRadius = clampedLevel == 2 ? 3.0F : 2.8F;
        double harmfulRadius = clampedLevel == 2 ? 2.0D : 1.8D;
        double pulseDamage = clampedLevel == 2 ? 2.0D : 1.0D;

        AreaEffectCloud cloud = world.spawn(impact.clone().add(0.0D, 0.2D, 0.0D), AreaEffectCloud.class);
        cloud.setRadius(visibleRadius);
        cloud.setDuration(durationTicks);
        cloud.setWaitTime(0);
        cloud.setReapplicationDelay(20);
        cloud.setRadiusOnUse(0.0F);
        cloud.setDurationOnUse(0);
        cloud.setRadiusPerTick(0.0F);
        cloud.setSource(null);

        Bukkit.getScheduler().runTaskTimer(plugin, task -> {
            if (!cloud.isValid() || cloud.isDead()) {
                task.cancel();
                return;
            }

            Location center = cloud.getLocation().clone().add(0.0D, 0.2D, 0.0D);
            try {
                world.spawnParticle(Particle.DRAGON_BREATH, center, 120, visibleRadius * 0.5D, 0.4D, visibleRadius * 0.5D, 0.05D, Float.valueOf(0.0F), true);
            } catch (IllegalArgumentException ignored) {
                world.spawnParticle(Particle.PORTAL, center, 90, visibleRadius * 0.5D, 0.35D, visibleRadius * 0.5D, 0.01D);
            }
            for (Entity nearby : world.getNearbyEntities(center, harmfulRadius, 1.3D, harmfulRadius)) {
                if (nearby instanceof LivingEntity living) {
                    living.setNoDamageTicks(0);
                    living.damage(pulseDamage);
                }
            }
        }, 0L, 5L);
    }

    @Override
    public void onShootBow(@NotNull EntityShootBowEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getProjectile() instanceof Projectile projectile)) {
            return;
        }
        ItemStack weapon = event.getBow();
        if (weapon == null || weapon.getType() != Material.CROSSBOW || !plugin().isRangedWeapon(weapon)) {
            return;
        }
        EquipmentSlot hand = event.getHand() == EquipmentSlot.OFF_HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
        plugin().revealMysteryItemIfNeeded(weapon, player, hand);
        int level = plugin().getEnchantLevel(weapon, EnchantType.DRAGONS_BREATH);
        tagProjectile(projectile, level, plugin().dragonsBreathProjectileKey());
    }

    @Override
    public void onProjectileHit(@NotNull ProjectileHitEvent event, long tickCounter) {
        Projectile projectile = event.getEntity();
        Integer level = projectile.getPersistentDataContainer().get(
                plugin().dragonsBreathProjectileKey(),
                PersistentDataType.INTEGER
        );
        if (level == null || level <= 0) {
            return;
        }
        onImpact(plugin(), projectile, projectile.getLocation().clone(), level);
    }
}

