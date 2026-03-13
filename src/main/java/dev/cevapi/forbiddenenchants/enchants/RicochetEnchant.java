package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Shulker;
import org.bukkit.entity.ShulkerBullet;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class RicochetEnchant extends BaseForbiddenEnchant {
    public RicochetEnchant() {
        super("ricochet",
                "ricochet_level",
                "Ricochet",
                ArmorSlot.SHIELD,
                1,
                NamedTextColor.WHITE,
                List.of("ricochet"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "While blocking: 33% arrow reflection, 100% vs ghast and shulker projectiles.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }

    public boolean shouldReflect(boolean forceReflect) {
        return forceReflect || ThreadLocalRandom.current().nextDouble() < 0.55D;
    }

    public void onProjectileBlocked(@NotNull EntityDamageByEntityEvent event,
                                    @NotNull Player defender,
                                    @NotNull Projectile projectile,
                                    @NotNull NamespacedKey ricochetKey) {
        if (projectile.getPersistentDataContainer().has(ricochetKey, PersistentDataType.BYTE)) {
            return;
        }

        Entity shooter = projectile.getShooter() instanceof Entity entity ? entity : null;
        boolean forceReflect = shooter instanceof Ghast || shooter instanceof Shulker;
        if (!forceReflect && !defender.isBlocking()) {
            return;
        }
        if (!shouldReflect(forceReflect)) {
            return;
        }

        event.setCancelled(true);
        Location origin = defender.getEyeLocation().add(defender.getLocation().getDirection().multiply(0.4D));
        Vector direction = reflectionDirection(defender, origin, shooter);

        if (projectile instanceof AbstractArrow arrow) {
            AbstractArrow reflected = defender.getWorld().spawnArrow(
                    origin,
                    direction,
                    (float) Math.max(1.2D, arrow.getVelocity().length()),
                    0.0F
            );
            reflected.setShooter(defender);
            reflected.getPersistentDataContainer().set(ricochetKey, PersistentDataType.BYTE, (byte) 1);
            projectile.remove();
            return;
        }

        if (projectile instanceof ShulkerBullet && shooter instanceof LivingEntity livingShooter) {
            ShulkerBullet reflected = defender.getWorld().spawn(origin, ShulkerBullet.class);
            reflected.setShooter(defender);
            reflected.setTarget(livingShooter);
            reflected.setVelocity(direction.multiply(0.8D));
            reflected.getPersistentDataContainer().set(ricochetKey, PersistentDataType.BYTE, (byte) 1);
            projectile.remove();
            return;
        }

        Projectile reflected = (Projectile) defender.getWorld().spawnEntity(origin, projectile.getType());
        reflected.setShooter(defender);
        reflected.setVelocity(direction.multiply(Math.max(1.0D, projectile.getVelocity().length())));
        reflected.getPersistentDataContainer().set(ricochetKey, PersistentDataType.BYTE, (byte) 1);
        projectile.remove();
    }

    private @NotNull Vector reflectionDirection(@NotNull Player defender,
                                                @NotNull Location origin,
                                                @Nullable Entity shooter) {
        if (shooter != null && shooter.isValid() && shooter.getWorld().equals(defender.getWorld())) {
            return shooter.getLocation().toVector().subtract(origin.toVector()).normalize();
        }
        return defender.getLocation().getDirection().normalize();
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player defender)) {
            return;
        }
        if (!(event.getDamager() instanceof Projectile projectile)) {
            return;
        }

        ItemStack shield = getRaisedShield(defender);
        int level = plugin().getEnchantLevel(shield, EnchantType.RICOCHET);
        if (!isActive(level)) {
            return;
        }
        onProjectileBlocked(event, defender, projectile, plugin().ricochetKey());
    }

    private @NotNull ItemStack getRaisedShield(@NotNull Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() == Material.SHIELD) {
            return main;
        }

        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() == Material.SHIELD) {
            return off;
        }

        return new ItemStack(Material.AIR);
    }
}

