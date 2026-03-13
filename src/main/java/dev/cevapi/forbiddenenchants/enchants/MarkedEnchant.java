package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public final class MarkedEnchant extends BaseForbiddenEnchant {
    public MarkedEnchant() {
        super("marked",
                "marked_level",
                "Marked",
                ArmorSlot.RANGED,
                1,
                NamedTextColor.AQUA,
                List.of("marked", "mark"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Bow/crossbow hits mark players for 30s; your follow-up damage is +25%.";
    }

    public boolean shouldTagProjectile(int level) {
        return level > 0;
    }

    public long markDurationTicks() {
        return 600L;
    }

    public void onProjectileHit(int level, @NotNull Marker marker) {
        if (!shouldTagProjectile(level)) {
            return;
        }
        marker.mark(markDurationTicks());
    }

    public boolean shouldBoostDamage(boolean ownerMatch, boolean unexpired) {
        return ownerMatch && unexpired;
    }

    public boolean isExpired(long expireTick, long tickCounter) {
        return expireTick <= tickCounter;
    }

    public double applyDamageBoost(double baseDamage, boolean ownerMatch, boolean unexpired) {
        if (!shouldBoostDamage(ownerMatch, unexpired)) {
            return baseDamage;
        }
        return baseDamage * damageMultiplier();
    }

    public double damageMultiplier() {
        return 1.40D;
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
        int level = plugin().getEnchantLevel(weapon, EnchantType.MARKED);
        if (!shouldTagProjectile(level)) {
            return;
        }
        projectile.getPersistentDataContainer().set(
                plugin().markedProjectileKey(),
                PersistentDataType.INTEGER,
                level
        );
    }

    @Override
    public void onProjectileHit(@NotNull ProjectileHitEvent event, long tickCounter) {
        Projectile projectile = event.getEntity();
        Integer level = projectile.getPersistentDataContainer().get(
                plugin().markedProjectileKey(),
                PersistentDataType.INTEGER
        );
        if (level == null || level <= 0) {
            return;
        }
        if (!(event.getHitEntity() instanceof Player target) || !(projectile.getShooter() instanceof Player owner)) {
            return;
        }
        onProjectileHit(level, duration -> plugin().applyMarkedTarget(target, owner.getUniqueId(), tickCounter + duration));
    }

    @FunctionalInterface
    public interface Marker {
        void mark(long durationTicks);
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }

        UUID damagerId;
        if (event.getDamager() instanceof Player playerDamager) {
            damagerId = playerDamager.getUniqueId();
        } else if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            damagerId = shooter.getUniqueId();
        } else {
            return;
        }

        event.setDamage(plugin().applyMarkedDamageBoost(target, damagerId, event.getDamage(), tickCounter));
    }
}

