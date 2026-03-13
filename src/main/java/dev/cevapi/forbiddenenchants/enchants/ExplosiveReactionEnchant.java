package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ExplosiveReactionEnchant extends BaseForbiddenEnchant {
    public ExplosiveReactionEnchant() {
        super("explosive_reaction",
                "explosive_reaction_level",
                "Explosive Reaction",
                ArmorSlot.RANGED,
                1,
                NamedTextColor.GOLD,
                List.of("explosive", "reaction", "explosive_reaction"),
                "Apply to a crossbow in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Crossbow shots trigger a short smoke burst and reduced TNT-style blast.";
    }

    public void tagProjectile(@NotNull Projectile projectile, int level, @NotNull NamespacedKey key) {
        if (level > 0) {
            projectile.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, level);
        }
    }

    public void onImpact(@NotNull Projectile projectile, @NotNull Location impact, int level) {
        if (level <= 0) {
            return;
        }
        World world = impact.getWorld();
        if (world == null) {
            return;
        }

        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, impact, 130, 1.0, 0.6, 1.0, 0.02);
        world.spawnParticle(Particle.FLAME, impact, 70, 0.9, 0.5, 0.9, 0.02);
        world.spawnParticle(Particle.LARGE_SMOKE, impact, 60, 0.9, 0.5, 0.9, 0.02);
        Entity source = projectile.getShooter() instanceof Entity entity ? entity : null;
        if (source != null) {
            world.createExplosion(source, impact, 3.0F, false, true);
        } else {
            world.createExplosion(impact, 3.0F, false, true);
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
        if (weapon == null || weapon.getType() != Material.CROSSBOW || !plugin().isRangedWeapon(weapon)) {
            return;
        }
        EquipmentSlot hand = event.getHand() == EquipmentSlot.OFF_HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
        plugin().revealMysteryItemIfNeeded(weapon, player, hand);
        int level = plugin().getEnchantLevel(weapon, EnchantType.EXPLOSIVE_REACTION);
        tagProjectile(projectile, level, plugin().explosiveReactionProjectileKey());
    }

    @Override
    public void onProjectileHit(@NotNull ProjectileHitEvent event, long tickCounter) {
        Projectile projectile = event.getEntity();
        Integer level = projectile.getPersistentDataContainer().get(
                plugin().explosiveReactionProjectileKey(),
                PersistentDataType.INTEGER
        );
        if (level == null || level <= 0) {
            return;
        }
        onImpact(projectile, projectile.getLocation().clone(), level);
    }
}

