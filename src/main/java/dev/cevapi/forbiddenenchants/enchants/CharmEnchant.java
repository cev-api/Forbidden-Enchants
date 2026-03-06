package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public final class CharmEnchant extends BaseForbiddenEnchant {
    public CharmEnchant() {
        super("charm",
                "enchantment_level",
                "Charm",
                ArmorSlot.RANGED,
                4,
                NamedTextColor.LIGHT_PURPLE,
                List.of("charm"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Hit mobs become allied for " + switch (level) {
                    case 1 -> "15s (1 ally)";
                    case 2 -> "30s (2 allies)";
                    case 3 -> "60s (3 allies)";
                    default -> "permanent until death (4 allies)";
                } + ". Warden hits also clear nearby Darkness while charmed.";
    }

    public boolean shouldTagProjectile(int level) {
        return level > 0;
    }

    public void onProjectileHit(int level, @NotNull Runnable action) {
        if (!shouldTagProjectile(level)) {
            return;
        }
        action.run();
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
        if (weapon == null || !ForbiddenEnchantsPlugin.instance().isRangedWeapon(weapon)) {
            return;
        }
        EquipmentSlot hand = event.getHand() == EquipmentSlot.OFF_HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
        ForbiddenEnchantsPlugin.instance().revealMysteryItemIfNeeded(weapon, player, hand);
        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(weapon, EnchantType.CHARM);
        if (!shouldTagProjectile(level)) {
            return;
        }
        projectile.getPersistentDataContainer().set(
                ForbiddenEnchantsPlugin.instance().enchantmentProjectileKey(),
                PersistentDataType.INTEGER,
                level
        );
    }

    @Override
    public void onProjectileHit(@NotNull ProjectileHitEvent event, long tickCounter) {
        Projectile projectile = event.getEntity();
        Integer level = projectile.getPersistentDataContainer().get(
                ForbiddenEnchantsPlugin.instance().enchantmentProjectileKey(),
                PersistentDataType.INTEGER
        );
        if (level == null) {
            return;
        }
        onProjectileHit(level, () -> ForbiddenEnchantsPlugin.instance().applyCharmProjectile(projectile, level, event.getHitEntity(), tickCounter));
    }

    @Override
    public void onMobTarget(@NotNull EntityTargetLivingEntityEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (ForbiddenEnchantsPlugin.instance().isCharmedPet(mob.getUniqueId())) {
            ForbiddenEnchantsPlugin.instance().cancelMobTarget(event, mob);
            return;
        }
        UUID ownerId = ForbiddenEnchantsPlugin.instance().allyOwnerId(mob.getUniqueId());
        if (ownerId == null) {
            return;
        }
        if (event.getTarget() instanceof Player targetPlayer && ownerId.equals(targetPlayer.getUniqueId())) {
            ForbiddenEnchantsPlugin.instance().cancelMobTarget(event, mob);
            return;
        }
        if (event.getTarget() instanceof Mob targetMob && ForbiddenEnchantsPlugin.instance().isAllyMob(targetMob.getUniqueId())) {
            ForbiddenEnchantsPlugin.instance().cancelMobTarget(event, mob);
        }
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        Entity rawDamager = event.getDamager();
        if (rawDamager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            rawDamager = shooter;
        }
        if (!(rawDamager instanceof Mob damagerMob)) {
            return;
        }
        if (ForbiddenEnchantsPlugin.instance().isCharmedPet(damagerMob.getUniqueId())) {
            event.setCancelled(true);
            damagerMob.setTarget(null);
            return;
        }
        UUID ownerId = ForbiddenEnchantsPlugin.instance().allyOwnerId(damagerMob.getUniqueId());
        if (ownerId == null) {
            return;
        }
        if (event.getEntity() instanceof Player targetPlayer && ownerId.equals(targetPlayer.getUniqueId())) {
            event.setCancelled(true);
            damagerMob.setTarget(null);
            return;
        }
        if (event.getEntity() instanceof Mob targetMob && ForbiddenEnchantsPlugin.instance().isAllyMob(targetMob.getUniqueId())) {
            event.setCancelled(true);
            damagerMob.setTarget(null);
        }
    }
}

