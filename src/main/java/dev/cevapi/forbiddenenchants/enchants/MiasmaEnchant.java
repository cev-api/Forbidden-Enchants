package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class MiasmaEnchant extends BaseForbiddenEnchant {
    public MiasmaEnchant() {
        super("miasma",
                "miasma_level",
                "Miasma",
                ArmorSlot.RANGED,
                1,
                NamedTextColor.DARK_GRAY,
                List.of("mist", "smoke"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "On arrow impact: heavy smoke, close invisibility cloud and nearby blindness pulse.";
    }

    public void tagProjectile(@NotNull Projectile projectile, int level, @NotNull NamespacedKey key) {
        if (level > 0) {
            projectile.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, level);
        }
    }

    public void onImpact(@NotNull Location impact, int level) {
        if (level <= 0) {
            return;
        }
        Location center = impact.clone().add(0.0D, 1.0D, 0.0D);
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        spawnSmokeBurst(world, center);

        double invisRadiusSquared = 1.0D;
        double blindRadiusSquared = 16.0D;
        double soundRadiusSquared = 256.0D;
        for (Player nearby : world.getPlayers()) {
            double distanceSquared = nearby.getLocation().distanceSquared(center);
            if (distanceSquared <= invisRadiusSquared) {
                nearby.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 160, 0, true, true, true), true);
            }
            if (distanceSquared <= blindRadiusSquared) {
                nearby.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0, true, true, true), true);
            }
            if (distanceSquared <= soundRadiusSquared) {
                nearby.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0F, 2.0F);
            }
        }
    }

    public void spawnSmokeVisual(@NotNull World world, @NotNull Location center) {
        spawnSmokeBurst(world, center);
    }

    private void spawnSmokeBurst(@NotNull World world, @NotNull Location center) {
        world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, center, 3000, 1.5D, 1.2D, 1.5D, 0.0D, null, true);
        Particle fallback = Particle.valueOf("SMALL_FLAME");
        try {
            Particle copper = Particle.valueOf("COPPER_FIRE_FLAME");
            world.spawnParticle(copper, center, 100, 1.6D, 1.3D, 1.6D, 0.1D, null, true);
        } catch (IllegalArgumentException ignored) {
            world.spawnParticle(fallback, center, 100, 1.6D, 1.3D, 1.6D, 0.1D, null, true);
        }
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
        if (weapon == null || !plugin().isRangedWeapon(weapon)) {
            return;
        }
        EquipmentSlot hand = event.getHand() == EquipmentSlot.OFF_HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
        plugin().revealMysteryItemIfNeeded(weapon, player, hand);
        int level = plugin().getEnchantLevel(weapon, EnchantType.MIASMA);
        tagProjectile(projectile, level, plugin().miasmaProjectileKey());
    }

    @Override
    public void onProjectileHit(@NotNull ProjectileHitEvent event, long tickCounter) {
        Projectile projectile = event.getEntity();
        Integer level = projectile.getPersistentDataContainer().get(
                plugin().miasmaProjectileKey(),
                PersistentDataType.INTEGER
        );
        if (level == null || level <= 0) {
            return;
        }
        onImpact(projectile.getLocation().clone(), level);
    }
}

