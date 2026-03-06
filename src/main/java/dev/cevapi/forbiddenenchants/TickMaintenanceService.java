package dev.cevapi.forbiddenenchants;

import dev.cevapi.forbiddenenchants.enchants.EnchantList;
import dev.cevapi.forbiddenenchants.enchants.CompassTrackingService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

final class TickMaintenanceService {
    private final EnchantStateService enchantStateService;
    private final ItemCombatService itemCombatService;
    private final MysteryItemService mysteryItemService;
    private final CompassTrackingService compassTrackingService;
    private final Map<UUID, Long> fullForceSmashWindowUntil;
    private final Map<UUID, Vector> fullForceKnockbackVectors;
    private final Map<UUID, Long> fullForceKnockbackUntil;
    private final Map<UUID, Long> shockwaveTotemArmedUntil;
    private final Map<UUID, MarkedState> markedTargets;
    private final Map<UUID, CharmedPetState> charmedPets;
    private final Map<UUID, Location> charmedPetSitAnchors;
    private final Map<UUID, Location> travelDurabilityLastLocation;
    private final Map<UUID, Double> travelDurabilityDistance;
    private final Map<UUID, Location> lastPlayerDeathLocations;

    TickMaintenanceService(@NotNull EnchantStateService enchantStateService,
                           @NotNull ItemCombatService itemCombatService,
                           @NotNull MysteryItemService mysteryItemService,
                           @NotNull CompassTrackingService compassTrackingService,
                           @NotNull Map<UUID, Long> fullForceSmashWindowUntil,
                           @NotNull Map<UUID, Vector> fullForceKnockbackVectors,
                           @NotNull Map<UUID, Long> fullForceKnockbackUntil,
                           @NotNull Map<UUID, Long> shockwaveTotemArmedUntil,
                           @NotNull Map<UUID, MarkedState> markedTargets,
                           @NotNull Map<UUID, CharmedPetState> charmedPets,
                           @NotNull Map<UUID, Location> charmedPetSitAnchors,
                           @NotNull Map<UUID, Location> travelDurabilityLastLocation,
                           @NotNull Map<UUID, Double> travelDurabilityDistance,
                           @NotNull Map<UUID, Location> lastPlayerDeathLocations) {
        this.enchantStateService = enchantStateService;
        this.itemCombatService = itemCombatService;
        this.mysteryItemService = mysteryItemService;
        this.compassTrackingService = compassTrackingService;
        this.fullForceSmashWindowUntil = fullForceSmashWindowUntil;
        this.fullForceKnockbackVectors = fullForceKnockbackVectors;
        this.fullForceKnockbackUntil = fullForceKnockbackUntil;
        this.shockwaveTotemArmedUntil = shockwaveTotemArmedUntil;
        this.markedTargets = markedTargets;
        this.charmedPets = charmedPets;
        this.charmedPetSitAnchors = charmedPetSitAnchors;
        this.travelDurabilityLastLocation = travelDurabilityLastLocation;
        this.travelDurabilityDistance = travelDurabilityDistance;
        this.lastPlayerDeathLocations = lastPlayerDeathLocations;
    }

    void processFullForceKnockbackOverrides(long tickCounter) {
        if (fullForceKnockbackUntil.isEmpty()) {
            return;
        }
        fullForceKnockbackUntil.entrySet().removeIf(entry -> {
            if (tickCounter <= entry.getValue()) {
                return false;
            }
            fullForceKnockbackVectors.remove(entry.getKey());
            return true;
        });
    }

    void refreshTotemArmedStates(@NotNull Player player, long tickCounter) {
        UUID id = player.getUniqueId();
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        int shockwave = Math.max(enchantStateService.getEnchantLevel(main, EnchantType.SHOCKWAVE), enchantStateService.getEnchantLevel(off, EnchantType.SHOCKWAVE));
        int mujahideen = Math.max(enchantStateService.getEnchantLevel(main, EnchantType.MUJAHIDEEN), enchantStateService.getEnchantLevel(off, EnchantType.MUJAHIDEEN));

        if (EnchantList.INSTANCE.shockwave().isActive(shockwave)) {
            shockwaveTotemArmedUntil.put(id, tickCounter + EnchantList.INSTANCE.shockwave().carryoverTicks());
        } else if (EnchantList.INSTANCE.shockwave().effectiveTotemLevel(
                shockwave,
                tickCounter,
                shockwaveTotemArmedUntil.getOrDefault(id, 0L)) <= 0) {
            shockwaveTotemArmedUntil.remove(id);
        }

        EnchantList.INSTANCE.mujahideen().refreshArmedState(id, tickCounter, mujahideen);
    }

    void processFullForceSmashWindow(@NotNull Player player, long tickCounter) {
        UUID id = player.getUniqueId();
        long until = fullForceSmashWindowUntil.getOrDefault(id, 0L);
        if (until > 0L && tickCounter > until) {
            fullForceSmashWindowUntil.remove(id);
        }

        if (player.isOnGround()) {
            return;
        }
        if (player.getVelocity().getY() >= -0.08D) {
            return;
        }
        if (player.getFallDistance() < 2.0F) {
            return;
        }
        fullForceSmashWindowUntil.put(id, tickCounter + 20L);
    }

    void processMarkedTargets(long tickCounter) {
        if (markedTargets.isEmpty()) {
            return;
        }
        markedTargets.entrySet().removeIf(entry -> entry.getValue().expireTick() <= tickCounter);
    }

    void processCharmedPets() {
        if (charmedPets.isEmpty()) {
            return;
        }
        charmedPets.entrySet().removeIf(entry -> {
            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity pet) || !pet.isValid() || pet.isDead()) {
                charmedPetSitAnchors.remove(entry.getKey());
                return true;
            }
            CharmedPetState state = entry.getValue();
            Player owner = Bukkit.getPlayer(state.ownerId());
            if (owner == null || !owner.isOnline()) {
                charmedPetSitAnchors.remove(entry.getKey());
                return true;
            }

            if (state.sitting()) {
                if (pet instanceof Mob mob) {
                    mob.setTarget(null);
                    mob.setAware(false);
                }
                Location anchor = charmedPetSitAnchors.get(entry.getKey());
                if (anchor != null && anchor.getWorld() != null && anchor.getWorld().equals(pet.getWorld())
                        && pet.getLocation().distanceSquared(anchor) > 0.16D) {
                    pet.teleport(anchor.clone());
                }
                pet.setVelocity(new Vector(0.0D, Math.max(-0.1D, pet.getVelocity().getY()), 0.0D));
                return false;
            }

            charmedPetSitAnchors.remove(entry.getKey());
            Location ownerLoc = owner.getLocation();
            Location petLoc = pet.getLocation();
            if (!ownerLoc.getWorld().equals(petLoc.getWorld())) {
                pet.teleport(ownerLoc.clone().add(0.5D, 0.0D, 0.5D));
                return false;
            }
            if (owner.isFlying() || owner.isGliding()) {
                if (pet instanceof Mob mob) {
                    mob.setAware(true);
                    mob.setTarget(null);
                    mob.lookAt(owner);
                }
                return false;
            }
            double distSq = ownerLoc.distanceSquared(petLoc);
            if (distSq > 256.0D) {
                pet.teleport(ownerLoc.clone().add(0.5D, 0.0D, 0.5D));
                return false;
            }
            if (pet instanceof Mob mob) {
                mob.setAware(true);
                mob.setTarget(null);
                mob.lookAt(owner);
                if (distSq > 9.0D) {
                    mob.getPathfinder().moveTo(owner, 1.2D);
                }
            }
            return false;
        });
    }

    void processTravelDurability(@NotNull Player player) {
        UUID id = player.getUniqueId();
        Location current = player.getLocation();
        Location previous = travelDurabilityLastLocation.get(id);
        if (previous == null) {
            travelDurabilityLastLocation.put(id, current.clone());
            return;
        }
        if (!previous.getWorld().equals(current.getWorld())) {
            travelDurabilityLastLocation.put(id, current.clone());
            return;
        }

        double delta = previous.distance(current);
        if (delta <= 0.001D) {
            return;
        }
        travelDurabilityLastLocation.put(id, current.clone());

        ItemStack helmet = player.getInventory().getHelmet();
        ItemStack boots = player.getInventory().getBoots();
        int divineVisionLevel = enchantStateService.getEnchantLevel(helmet, EnchantType.DIVINE_VISION);
        int minersIntuitionLevel = enchantStateService.getEnchantLevel(helmet, EnchantType.MINERS_INTUITION);
        int lootSenseLevel = enchantStateService.getEnchantLevel(helmet, EnchantType.LOOT_SENSE);
        boolean active = EnchantList.INSTANCE.divineVision().isActive(divineVisionLevel)
                || EnchantList.INSTANCE.minersIntuition().isActive(minersIntuitionLevel)
                || EnchantList.INSTANCE.lootSense().isActive(lootSenseLevel)
                || enchantStateService.getEnchantLevel(boots, EnchantType.MASQUERADE) > 0;
        if (!active) {
            travelDurabilityDistance.remove(id);
            return;
        }

        double total = travelDurabilityDistance.getOrDefault(id, 0.0D) + delta;
        int steps = (int) Math.floor(total / 100.0D);
        if (steps > 0) {
            if (EnchantList.INSTANCE.divineVision().isActive(divineVisionLevel)
                    || EnchantList.INSTANCE.minersIntuition().isActive(minersIntuitionLevel)
                    || EnchantList.INSTANCE.lootSense().isActive(lootSenseLevel)) {
                itemCombatService.damageArmorByPercent(player, EquipmentSlot.HEAD, helmet, 0.10D * steps);
            }
            if (enchantStateService.getEnchantLevel(boots, EnchantType.MASQUERADE) > 0) {
                itemCombatService.damageArmorByPercent(player, EquipmentSlot.FEET, boots, 0.10D * steps);
            }
            total -= steps * 100.0D;
        }
        travelDurabilityDistance.put(id, total);
    }

    void processCompassEffects(@NotNull Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() == Material.COMPASS) {
            compassTrackingService.updateCompassTracking(
                    player,
                    main,
                    EquipmentSlot.HAND,
                    enchantStateService.getEnchantLevel(main, EnchantType.GRAVE_ROBBER),
                    enchantStateService.getEnchantLevel(main, EnchantType.POCKET_SEEKER),
                    lastPlayerDeathLocations
            );
        }

        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() == Material.COMPASS) {
            compassTrackingService.updateCompassTracking(
                    player,
                    off,
                    EquipmentSlot.OFF_HAND,
                    enchantStateService.getEnchantLevel(off, EnchantType.GRAVE_ROBBER),
                    enchantStateService.getEnchantLevel(off, EnchantType.POCKET_SEEKER),
                    lastPlayerDeathLocations
            );
        }
    }

    void processMysteryReveals(@NotNull Player player) {
        mysteryItemService.revealMysteryItemIfNeeded(player.getInventory().getHelmet(), player, EquipmentSlot.HEAD);
        mysteryItemService.revealMysteryItemIfNeeded(player.getInventory().getChestplate(), player, EquipmentSlot.CHEST);
        mysteryItemService.revealMysteryItemIfNeeded(player.getInventory().getLeggings(), player, EquipmentSlot.LEGS);
        mysteryItemService.revealMysteryItemIfNeeded(player.getInventory().getBoots(), player, EquipmentSlot.FEET);
    }
}

