package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Vex;
import org.bukkit.NamespacedKey;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class VexatiousEnchant extends BaseForbiddenEnchant {
    public VexatiousEnchant() {
        super("vexatious",
                "vexatious_level",
                "Vexatious",
                ArmorSlot.HELMET,
                3,
                NamedTextColor.AQUA,
                List.of("vex", "vexatious"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Maintains 1/2/3 bound vex allies (extra vexes require pristine helmet durability).";
    }

    public void maintain(@NotNull NamespacedKey vexatiousOwnerKey,
                         @NotNull ArmorDamageApplier armorDamageApplier,
                         @NotNull Map<UUID, List<UUID>> vexesByOwner,
                         @NotNull Map<UUID, UUID> assistTargets,
                         @NotNull Map<UUID, Long> assistTargetExpire,
                         @NotNull Player player,
                         @NotNull ItemStack helmet,
                         int level,
                         long tickCounter) {
        UUID owner = player.getUniqueId();
        List<UUID> tracked = vexesByOwner.computeIfAbsent(owner, ignored -> new ArrayList<>());
        int before = tracked.size();
        tracked.removeIf(id -> {
            Entity entity = Bukkit.getEntity(id);
            return !(entity instanceof Vex vex) || !vex.isValid() || vex.isDead();
        });
        int deaths = before - tracked.size();
        if (deaths > 0) {
            armorDamageApplier.apply(player, helmet, 0.25D * deaths);
        }

        int allowed = 1;
        ItemMeta meta = helmet.getItemMeta();
        boolean pristine = false;
        if (meta instanceof Damageable damageable) {
            pristine = damageable.getDamage() <= 0;
        }
        if (pristine) {
            allowed = Math.max(1, Math.min(3, level));
        }

        while (tracked.size() < allowed) {
            Vex vex = player.getWorld().spawn(player.getLocation().add(0.5D, 1.0D, 0.5D), Vex.class);
            vex.setSilent(true);
            vex.setAware(true);
            vex.setCanPickupItems(false);
            vex.setRemoveWhenFarAway(false);
            vex.getPersistentDataContainer().set(vexatiousOwnerKey, PersistentDataType.STRING, owner.toString());
            tracked.add(vex.getUniqueId());
        }

        for (UUID id : tracked) {
            Entity entity = Bukkit.getEntity(id);
            if (!(entity instanceof Vex vex) || !vex.isValid() || vex.isDead()) {
                continue;
            }
            if (!vex.getWorld().equals(player.getWorld())
                    || vex.getLocation().distanceSquared(player.getLocation()) > 100.0D) {
                vex.teleport(player.getLocation().add(0.5D, 1.0D, 0.5D));
            }
            LivingEntity target = resolveTarget(assistTargets, assistTargetExpire, player, tickCounter);
            vex.setTarget(target);
            if (target != null) {
                vex.lookAt(target);
                vex.getPathfinder().moveTo(target, 1.35D);
                try {
                    vex.setCharging(true);
                } catch (Throwable ignored) {
                }
                continue;
            }
            try {
                vex.setCharging(false);
            } catch (Throwable ignored) {
            }
            vex.lookAt(player);
            Vector follow = player.getLocation().add(0.0D, 1.2D, 0.0D).toVector().subtract(vex.getLocation().toVector());
            if (follow.lengthSquared() > 16.0D) {
                vex.teleport(player.getLocation().add(0.5D, 1.0D, 0.5D));
            } else if (follow.lengthSquared() > 1.0D) {
                vex.getPathfinder().moveTo(player, 1.2D);
                vex.setVelocity(follow.normalize().multiply(0.22D));
            }
        }
    }

    public void pushAssistTarget(@NotNull Map<UUID, List<UUID>> vexesByOwner,
                                 @NotNull Map<UUID, UUID> assistTargets,
                                 @NotNull Map<UUID, Long> assistTargetExpire,
                                 @NotNull UUID ownerId,
                                 @NotNull LivingEntity target,
                                 long tickCounter) {
        assistTargets.put(ownerId, target.getUniqueId());
        assistTargetExpire.put(ownerId, tickCounter + 200L);

        List<UUID> tracked = vexesByOwner.get(ownerId);
        if (tracked == null || tracked.isEmpty()) {
            return;
        }
        for (UUID vexId : tracked) {
            Entity raw = Bukkit.getEntity(vexId);
            if (!(raw instanceof Vex vex) || !vex.isValid() || vex.isDead()) {
                continue;
            }
            if (!vex.getWorld().equals(target.getWorld())) {
                continue;
            }
            vex.setTarget(target);
            vex.lookAt(target);
            vex.getPathfinder().moveTo(target, 1.35D);
            try {
                vex.setCharging(true);
            } catch (Throwable ignored) {
            }
        }
    }

    public void clear(@NotNull Map<UUID, List<UUID>> vexesByOwner,
                      @NotNull Map<UUID, UUID> assistTargets,
                      @NotNull Map<UUID, Long> assistTargetExpire,
                      @NotNull UUID owner) {
        assistTargets.remove(owner);
        assistTargetExpire.remove(owner);
        List<UUID> tracked = vexesByOwner.remove(owner);
        if (tracked == null) {
            return;
        }
        for (UUID id : tracked) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    public void cleanupAll(@NotNull Map<UUID, List<UUID>> vexesByOwner) {
        for (List<UUID> tracked : vexesByOwner.values()) {
            for (UUID id : tracked) {
                Entity entity = Bukkit.getEntity(id);
                if (entity != null) {
                    entity.remove();
                }
            }
        }
        vexesByOwner.clear();
    }

    private @Nullable LivingEntity resolveTarget(@NotNull Map<UUID, UUID> assistTargets,
                                                 @NotNull Map<UUID, Long> assistTargetExpire,
                                                 @NotNull Player owner,
                                                 long tickCounter) {
        UUID ownerId = owner.getUniqueId();
        long expire = assistTargetExpire.getOrDefault(ownerId, 0L);
        UUID forcedId = assistTargets.get(ownerId);
        if (forcedId != null && tickCounter <= expire) {
            Entity forced = Bukkit.getEntity(forcedId);
            if (forced instanceof LivingEntity living
                    && living.isValid()
                    && !living.isDead()
                    && living.getWorld().equals(owner.getWorld())
                    && !living.getUniqueId().equals(ownerId)) {
                return living;
            }
        }
        if (forcedId != null && tickCounter > expire) {
            assistTargets.remove(ownerId);
            assistTargetExpire.remove(ownerId);
        }

        LivingEntity target = null;
        double nearest = Double.MAX_VALUE;
        for (Entity nearby : owner.getNearbyEntities(16.0D, 8.0D, 16.0D)) {
            if (!(nearby instanceof LivingEntity living) || living.equals(owner) || living instanceof Vex) {
                continue;
            }
            if (!(living instanceof Monster)) {
                continue;
            }
            double dist = living.getLocation().distanceSquared(owner.getLocation());
            if (dist < nearest) {
                nearest = dist;
                target = living;
            }
        }
        return target;
    }

    @FunctionalInterface
    public interface ArmorDamageApplier {
        void apply(@NotNull Player player, @NotNull ItemStack helmet, double fractionOfMax);
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (event.getDamager() instanceof Player attacker) {
            ItemStack helmet = attacker.getInventory().getHelmet();
            if (plugin().getEnchantLevel(helmet, EnchantType.VEXATIOUS) > 0
                    && event.getEntity() instanceof LivingEntity living) {
                plugin().pushVexatiousAssistTarget(attacker.getUniqueId(), living, tickCounter);
            }
        }

        if (event.getEntity() instanceof Player defender) {
            ItemStack helmet = defender.getInventory().getHelmet();
            if (plugin().getEnchantLevel(helmet, EnchantType.VEXATIOUS) <= 0) {
                return;
            }
            Entity source = event.getDamager();
            if (source instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
                source = shooter;
            }
            if (source instanceof LivingEntity living) {
                plugin().pushVexatiousAssistTarget(defender.getUniqueId(), living, tickCounter);
            }
        }
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        ItemStack helmet = player.getInventory().getHelmet();
        int level = plugin().getEnchantLevel(helmet, EnchantType.VEXATIOUS);
        if (level > 0 && tickCounter % 20L == 0L) {
            plugin().maintainVexatious(player, helmet, level, tickCounter);
            return;
        }
        if (level <= 0) {
            plugin().clearVexatious(player.getUniqueId());
        }
    }
}

