package dev.cevapi.forbiddenenchants;

import dev.cevapi.forbiddenenchants.enchants.EnchantList;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

final class MiasmaFormService {
    private static final double MIASMA_PHASE_DAMAGE = 2.0D;

    private final Supplier<EnchantStateService> enchantStateServiceSupplier;
    private final MiasmaVisualService visualService;
    private final Map<UUID, Long> phaseCooldownTicks;

    MiasmaFormService(@NotNull Supplier<EnchantStateService> enchantStateServiceSupplier,
                      @NotNull MiasmaVisualService visualService,
                      @NotNull Map<UUID, Long> phaseCooldownTicks) {
        this.enchantStateServiceSupplier = enchantStateServiceSupplier;
        this.visualService = visualService;
        this.phaseCooldownTicks = phaseCooldownTicks;
    }

    boolean hasMiasmaEnchantEquipped(@NotNull Player player) {
        int equippedLevel = enchantStateServiceSupplier.get().getEnchantLevel(player.getInventory().getChestplate(), EnchantType.MIASMA_FORM);
        if (EnchantList.INSTANCE.miasmaForm().isActive(equippedLevel)) {
            return true;
        }
        MiasmaVisualState state = visualService.getState(player.getUniqueId());
        if (state == null) {
            return false;
        }
        int storedLevel = enchantStateServiceSupplier.get().getEnchantLevel(state.chestplate(), EnchantType.MIASMA_FORM);
        return EnchantList.INSTANCE.miasmaForm().isActive(storedLevel);
    }

    boolean hasUnyieldingEquipped(@NotNull Player player) {
        if (enchantStateServiceSupplier.get().getEnchantLevel(player.getInventory().getChestplate(), EnchantType.THE_UNYIELDING) > 0) {
            return true;
        }
        MiasmaVisualState state = visualService.getState(player.getUniqueId());
        return state != null && enchantStateServiceSupplier.get().getEnchantLevel(state.chestplate(), EnchantType.THE_UNYIELDING) > 0;
    }

    void applyForm(@NotNull Player player, long tickCounter) {
        visualService.ensure(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 10, 0, true, false, false), true);
        player.getWorld().spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, player.getLocation().add(0.0D, 1.0D, 0.0D), 8, 0.2, 0.35, 0.2, 0.0);
        for (Entity entity : player.getNearbyEntities(20.0D, 20.0D, 20.0D)) {
            if (entity instanceof Mob mob && mob.getType() != EntityType.BLAZE && player.equals(mob.getTarget())) {
                mob.setTarget(null);
            }
        }
        tryPhaseThroughThinWall(player, tickCounter);
    }

    boolean isAllowedDamage(@NotNull EntityDamageEvent event) {
        EntityDamageEvent.DamageCause cause = event.getCause();
        if (cause == EntityDamageEvent.DamageCause.FIRE
                || cause == EntityDamageEvent.DamageCause.FIRE_TICK
                || cause == EntityDamageEvent.DamageCause.LAVA
                || cause == EntityDamageEvent.DamageCause.HOT_FLOOR
                || cause == EntityDamageEvent.DamageCause.CUSTOM) {
            return true;
        }

        if (!(event instanceof EntityDamageByEntityEvent byEntityEvent)) {
            return false;
        }

        Entity damager = byEntityEvent.getDamager();
        if (damager.getType() == EntityType.BLAZE
                || damager.getType() == EntityType.FIREBALL
                || damager.getType() == EntityType.SMALL_FIREBALL) {
            return true;
        }
        if (damager instanceof org.bukkit.entity.AbstractArrow arrow) {
            return arrow.getFireTicks() > 0 || arrow.isVisualFire();
        }
        return false;
    }

    private void tryPhaseThroughThinWall(@NotNull Player player, long tickCounter) {
        long readyTick = phaseCooldownTicks.getOrDefault(player.getUniqueId(), 0L);
        if (tickCounter < readyTick) {
            return;
        }

        Vector direction = player.getLocation().getDirection();
        direction.setY(0.0D);
        if (direction.lengthSquared() < 0.0001D) {
            return;
        }
        direction.normalize();

        Location base = player.getLocation().add(0.0D, 0.05D, 0.0D);
        var wallFeet = base.clone().add(direction.clone().multiply(0.65D)).getBlock();
        var wallHead = base.clone().add(0.0D, 1.05D, 0.0D).add(direction.clone().multiply(0.65D)).getBlock();
        var exitFeet = base.clone().add(direction.clone().multiply(1.55D)).getBlock();
        var exitHead = base.clone().add(0.0D, 1.05D, 0.0D).add(direction.clone().multiply(1.55D)).getBlock();

        if (wallFeet.isPassable() && wallHead.isPassable()) {
            return;
        }
        if (!exitFeet.isPassable() || !exitHead.isPassable()) {
            return;
        }

        Location destination = base.clone().add(direction.clone().multiply(1.6D));
        destination.setYaw(base.getYaw());
        destination.setPitch(base.getPitch());
        player.teleport(destination);

        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.damage(MIASMA_PHASE_DAMAGE);
        }
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, destination.clone().add(0.0D, 1.0D, 0.0D), 24, 0.25, 0.45, 0.25, 0.03);
        phaseCooldownTicks.put(player.getUniqueId(), tickCounter + 8L);
    }
}

