package dev.cevapi.forbiddenenchants;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

final class EnchantmentAllyService {
    private final HostileSpawnService hostileSpawnService;
    private final Supplier<NearbyEffectsService> nearbyEffectsServiceSupplier;
    private final Supplier<ItemClassificationService> itemClassificationServiceSupplier;
    private final Supplier<EnchantStateService> enchantStateServiceSupplier;
    private final Supplier<PlayerItemUtilityService> playerItemUtilityServiceSupplier;
    private final Map<UUID, EnchantmentAllyState> allies;

    EnchantmentAllyService(@NotNull HostileSpawnService hostileSpawnService,
                           @NotNull Supplier<NearbyEffectsService> nearbyEffectsServiceSupplier,
                           @NotNull Supplier<ItemClassificationService> itemClassificationServiceSupplier,
                           @NotNull Supplier<EnchantStateService> enchantStateServiceSupplier,
                           @NotNull Supplier<PlayerItemUtilityService> playerItemUtilityServiceSupplier,
                           @NotNull Map<UUID, EnchantmentAllyState> allies) {
        this.hostileSpawnService = hostileSpawnService;
        this.nearbyEffectsServiceSupplier = nearbyEffectsServiceSupplier;
        this.itemClassificationServiceSupplier = itemClassificationServiceSupplier;
        this.enchantStateServiceSupplier = enchantStateServiceSupplier;
        this.playerItemUtilityServiceSupplier = playerItemUtilityServiceSupplier;
        this.allies = allies;
    }

    void applyProjectile(@NotNull Projectile projectile, int level, @Nullable Entity hitEntity, long tickCounter) {
        if (!(projectile.getShooter() instanceof Player owner)) {
            return;
        }
        if (level < 1) {
            return;
        }

        int maxAllies = switch (level) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            default -> 4;
        };
        long durationTicks = switch (level) {
            case 1 -> 15L * 20L;
            case 2 -> 30L * 20L;
            case 3 -> 60L * 20L;
            default -> -1L;
        };
        boolean appliedAny = false;

        if (hitEntity instanceof Mob mob) {
            charmMob(owner, mob, durationTicks, maxAllies, null, false, tickCounter);
            appliedAny = true;
        } else if (hitEntity instanceof Player target) {
            List<Mob> nearbyAggressive = findNearbyAggressiveMobs(target.getLocation(), 14.0D);
            if (nearbyAggressive.isEmpty()) {
                int spawned = spawnEnchantmentAttackers(owner, target, maxAllies, durationTicks, tickCounter);
                appliedAny = spawned > 0;
            } else {
                int applied = 0;
                for (Mob aggressive : nearbyAggressive) {
                    if (applied >= maxAllies) {
                        break;
                    }
                    charmMob(owner, aggressive, durationTicks, maxAllies, target.getUniqueId(), false, tickCounter);
                    applied++;
                }
                appliedAny = applied > 0;
            }
        }

        if (appliedAny) {
            damageCharmWeaponOnProc(owner);
        }
    }

    void processTick(long tickCounter) {
        if (allies.isEmpty()) {
            return;
        }

        var iterator = allies.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, EnchantmentAllyState> entry = iterator.next();
            Entity raw = Bukkit.getEntity(entry.getKey());
            if (!(raw instanceof Mob mob) || !mob.isValid() || mob.isDead()) {
                iterator.remove();
                continue;
            }

            EnchantmentAllyState state = entry.getValue();
            Player owner = Bukkit.getPlayer(state.ownerId());
            if (owner == null || !owner.isOnline() || (state.expireTick() >= 0 && state.expireTick() <= tickCounter)) {
                if (state.spawnedByEnchant()) {
                    mob.remove();
                } else {
                    mob.setTarget(null);
                }
                iterator.remove();
                continue;
            }
            if (tickCounter % 2L == 0L) {
                Location heartLoc = mob.getLocation().add(0.0D, mob.getHeight() + 0.25D, 0.0D);
                mob.getWorld().spawnParticle(Particle.HEART, heartLoc, 3, 0.25D, 0.15D, 0.25D, 0.01D);
            }
            if (mob.getType() == EntityType.WARDEN) {
                nearbyEffectsServiceSupplier.get().clearNearbyDarkness(mob.getLocation(), 48.0D);
            }

            if (state.forcedTargetPlayerId() != null) {
                Entity forced = Bukkit.getEntity(state.forcedTargetPlayerId());
                if (forced instanceof org.bukkit.entity.LivingEntity forcedTarget
                        && forcedTarget.isValid()
                        && !forcedTarget.isDead()
                        && forcedTarget.getWorld().equals(mob.getWorld())) {
                    mob.setTarget(forcedTarget);
                    continue;
                }
            }

            Mob aggressive = findNearestAggressiveMob(owner, 16.0D);
            if (aggressive != null && !aggressive.getUniqueId().equals(mob.getUniqueId())) {
                mob.setTarget(aggressive);
                continue;
            }

            mob.setTarget(null);
            double distanceSquared = mob.getLocation().distanceSquared(owner.getLocation());
            if (owner.isFlying() || owner.isGliding()) {
                mob.lookAt(owner);
                continue;
            }
            if (distanceSquared > 10.0D) {
                mob.lookAt(owner);
                mob.getPathfinder().moveTo(owner, 1.25D);
            } else {
                mob.lookAt(owner);
            }
        }
    }

    void removeOwnedBy(@NotNull UUID ownerId) {
        var iterator = allies.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, EnchantmentAllyState> entry = iterator.next();
            if (!entry.getValue().ownerId().equals(ownerId)) {
                continue;
            }

            Entity entity = Bukkit.getEntity(entry.getKey());
            if (entity instanceof Mob mob) {
                if (entry.getValue().spawnedByEnchant()) {
                    mob.remove();
                } else {
                    mob.setTarget(null);
                }
            }
            iterator.remove();
        }
    }

    @Nullable EnchantmentAllyState stateFor(@NotNull UUID mobId) {
        return allies.get(mobId);
    }

    boolean isAlly(@NotNull UUID mobId) {
        return allies.containsKey(mobId);
    }

    private void charmMob(@NotNull Player owner,
                          @NotNull Mob mob,
                          long durationTicks,
                          int maxAllies,
                          @Nullable UUID forcedTargetPlayerId,
                          boolean spawnedByEnchant,
                          long tickCounter) {
        if (!mob.isValid() || mob.isDead()) {
            return;
        }

        enforceEnchantmentAllyLimit(owner.getUniqueId(), maxAllies);
        allies.put(
                mob.getUniqueId(),
                new EnchantmentAllyState(
                        owner.getUniqueId(),
                        durationTicks < 0 ? -1L : tickCounter + durationTicks,
                        forcedTargetPlayerId,
                        spawnedByEnchant,
                        tickCounter
                )
        );
        if (mob.getType() == EntityType.WARDEN) {
            nearbyEffectsServiceSupplier.get().clearNearbyDarkness(mob.getLocation(), 48.0D);
        }
        if (forcedTargetPlayerId == null) {
            mob.setTarget(null);
        }
    }

    private void enforceEnchantmentAllyLimit(@NotNull UUID ownerId, int maxAllies) {
        List<Map.Entry<UUID, EnchantmentAllyState>> owned = new ArrayList<>();
        for (Map.Entry<UUID, EnchantmentAllyState> entry : allies.entrySet()) {
            if (entry.getValue().ownerId().equals(ownerId)) {
                owned.add(entry);
            }
        }

        while (owned.size() >= maxAllies && !owned.isEmpty()) {
            Map.Entry<UUID, EnchantmentAllyState> oldest = owned.get(0);
            for (Map.Entry<UUID, EnchantmentAllyState> entry : owned) {
                if (entry.getValue().createdTick() < oldest.getValue().createdTick()) {
                    oldest = entry;
                }
            }

            EnchantmentAllyState removed = allies.remove(oldest.getKey());
            if (removed != null) {
                Entity entity = Bukkit.getEntity(oldest.getKey());
                if (entity instanceof Mob mob) {
                    if (removed.spawnedByEnchant()) {
                        mob.remove();
                    } else {
                        mob.setTarget(null);
                    }
                }
            }
            owned.remove(oldest);
        }
    }

    private int spawnEnchantmentAttackers(@NotNull Player owner,
                                          @NotNull Player target,
                                          int amount,
                                          long durationTicks,
                                          long tickCounter) {
        World world = target.getWorld();
        int spawned = 0;
        for (int i = 0; i < amount; i++) {
            Location spawn = findNearbySpawnLocationAround(target.getLocation(), 3.0D, 6.0D);
            if (spawn == null) {
                continue;
            }

            EntityType type = hostileSpawnService.randomHostileType();
            Entity entity = world.spawnEntity(spawn, type);
            if (!(entity instanceof Mob mob)) {
                entity.remove();
                continue;
            }

            charmMob(owner, mob, durationTicks, amount, target.getUniqueId(), true, tickCounter);
            mob.setTarget(target);
            spawned++;
        }
        return spawned;
    }

    private void damageCharmWeaponOnProc(@NotNull Player owner) {
        ItemStack main = owner.getInventory().getItemInMainHand();
        if (itemClassificationServiceSupplier.get().isRangedWeapon(main)
                && enchantStateServiceSupplier.get().getEnchantLevel(main, EnchantType.CHARM) > 0) {
            playerItemUtilityServiceSupplier.get().damageItemByPercent(owner, EquipmentSlot.HAND, main, 0.10D);
            return;
        }
        ItemStack off = owner.getInventory().getItemInOffHand();
        if (itemClassificationServiceSupplier.get().isRangedWeapon(off)
                && enchantStateServiceSupplier.get().getEnchantLevel(off, EnchantType.CHARM) > 0) {
            playerItemUtilityServiceSupplier.get().damageItemByPercent(owner, EquipmentSlot.OFF_HAND, off, 0.10D);
        }
    }

    private @Nullable Mob findNearestAggressiveMob(@NotNull Player owner, double radius) {
        Mob nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (Entity nearby : owner.getNearbyEntities(radius, radius, radius)) {
            if (!(nearby instanceof Mob mob) || !isAggressiveMob(mob) || !mob.isValid() || mob.isDead()) {
                continue;
            }
            EnchantmentAllyState allied = allies.get(mob.getUniqueId());
            if (allied != null) {
                continue;
            }

            double dist = mob.getLocation().distanceSquared(owner.getLocation());
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = mob;
            }
        }

        return nearest;
    }

    private @NotNull List<Mob> findNearbyAggressiveMobs(@NotNull Location center, double radius) {
        List<Mob> mobs = new ArrayList<>();
        World world = center.getWorld();
        if (world == null) {
            return mobs;
        }

        for (Entity nearby : world.getNearbyEntities(center, radius, radius, radius)) {
            if (nearby instanceof Mob mob && isAggressiveMob(mob) && mob.isValid() && !mob.isDead()) {
                mobs.add(mob);
            }
        }
        return mobs;
    }

    private boolean isAggressiveMob(@NotNull Mob mob) {
        return mob instanceof Monster
                || mob.getType() == EntityType.IRON_GOLEM
                || mob.getType() == EntityType.WOLF
                || mob.getType() == EntityType.ZOMBIFIED_PIGLIN;
    }

    private @Nullable Location findNearbySpawnLocationAround(@NotNull Location center, double minDistance, double maxDistance) {
        World world = center.getWorld();
        if (world == null) {
            return null;
        }

        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 10; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = random.nextDouble(minDistance, maxDistance);
            int x = (int) Math.floor(center.getX() + Math.cos(angle) * radius);
            int z = (int) Math.floor(center.getZ() + Math.sin(angle) * radius);
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location spawn = new Location(world, x + 0.5D, y, z + 0.5D);
            if (spawn.getBlock().isPassable() && spawn.clone().add(0.0D, 1.0D, 0.0D).getBlock().isPassable()) {
                return spawn;
            }
        }
        return null;
    }
}

