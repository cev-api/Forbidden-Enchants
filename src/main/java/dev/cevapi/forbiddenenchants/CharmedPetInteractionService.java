package dev.cevapi.forbiddenenchants;

import dev.cevapi.forbiddenenchants.enchants.EnchantList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

final class CharmedPetInteractionService {
    private final Supplier<EnchantStateService> enchantStateServiceSupplier;
    private final Plugin schedulerPlugin;
    private final PlayerItemUtilityService playerItemUtilityService;
    private final Map<UUID, CharmedPetState> charmedPets;
    private final Map<UUID, org.bukkit.Location> sitAnchors;
    private final Map<String, Long> toggleDedupTicks;

    CharmedPetInteractionService(@NotNull Supplier<EnchantStateService> enchantStateServiceSupplier,
                                 @NotNull Plugin schedulerPlugin,
                                 @NotNull PlayerItemUtilityService playerItemUtilityService,
                                 @NotNull Map<UUID, CharmedPetState> charmedPets,
                                 @NotNull Map<UUID, org.bukkit.Location> sitAnchors,
                                 @NotNull Map<String, Long> toggleDedupTicks) {
        this.enchantStateServiceSupplier = enchantStateServiceSupplier;
        this.schedulerPlugin = schedulerPlugin;
        this.playerItemUtilityService = playerItemUtilityService;
        this.charmedPets = charmedPets;
        this.sitAnchors = sitAnchors;
        this.toggleDedupTicks = toggleDedupTicks;
    }

    void clearToggleDedupForPlayer(@NotNull UUID playerId) {
        String prefix = playerId + ":";
        toggleDedupTicks.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
    }

    void handleCharmedPetToggle(@NotNull Player player,
                                @NotNull Entity clicked,
                                @NotNull Cancellable event,
                                long tickCounter) {
        CharmedPetState state = charmedPets.get(clicked.getUniqueId());
        if (state == null || !state.ownerId().equals(player.getUniqueId())) {
            return;
        }
        String dedupeKey = player.getUniqueId() + ":" + clicked.getUniqueId();
        Long lastToggleTick = toggleDedupTicks.get(dedupeKey);
        if (lastToggleTick != null && lastToggleTick == tickCounter) {
            event.setCancelled(true);
            return;
        }
        toggleDedupTicks.put(dedupeKey, tickCounter);

        boolean nextSitting = !state.sitting();
        charmedPets.put(clicked.getUniqueId(), new CharmedPetState(state.ownerId(), nextSitting));
        if (clicked instanceof Mob mob) {
            mob.setTarget(null);
            mob.setAware(!nextSitting);
            if (!nextSitting) {
                mob.lookAt(player);
                mob.getPathfinder().moveTo(player, 1.2D);
                Vector toward = player.getLocation().toVector().subtract(mob.getLocation().toVector());
                if (toward.lengthSquared() > 0.04D) {
                    mob.setVelocity(toward.normalize().multiply(0.24D));
                }
            }
        }
        if (nextSitting) {
            sitAnchors.put(clicked.getUniqueId(), clicked.getLocation().clone());
        } else {
            sitAnchors.remove(clicked.getUniqueId());
        }
        event.setCancelled(true);
        String petName = DisplayNameUtil.actionBarEntityName(clicked);
        player.sendActionBar(Component.text(
                petName + (nextSitting ? " is sitting." : " is following."),
                NamedTextColor.GREEN
        ));
        Bukkit.getScheduler().runTask(schedulerPlugin, () -> {
            if (player.getOpenInventory() != null) {
                player.closeInventory();
            }
        });
    }

    void handleSpecialLeadUse(@NotNull Player player,
                              @NotNull Entity clicked,
                              @Nullable EquipmentSlot rawHand,
                              @NotNull Cancellable event) {
        EquipmentSlot hand = rawHand == EquipmentSlot.OFF_HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
        ItemStack held = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (held.getType() != Material.LEAD) {
            return;
        }
        int getOverHereLevel = enchantStateServiceSupplier.get().getEnchantLevel(held, EnchantType.GET_OVER_HERE);
        if (!EnchantList.INSTANCE.getOverHere().canLeashVillagers(getOverHereLevel)) {
            return;
        }
        if (!(clicked instanceof LivingEntity target) || target instanceof Player || target.isLeashed()) {
            return;
        }

        if (target instanceof Villager && !player.isSneaking()) {
            event.setCancelled(true);
            player.sendActionBar(Component.text("Sneak-right-click to leash villagers.", NamedTextColor.YELLOW));
            return;
        }

        if (!(target instanceof Villager villager)) {
            return;
        }

        event.setCancelled(true);
        Bukkit.getScheduler().runTask(schedulerPlugin, () -> {
            if (!villager.isValid() || villager.isDead() || !player.isOnline()) {
                return;
            }
            boolean attached = villager.setLeashHolder(player);
            if (!attached) {
                return;
            }
            playerItemUtilityService.consumeOneFromHand(player, hand);
            if (player.getOpenInventory() != null) {
                player.closeInventory();
            }
        });
    }

    void handleVillagerSneakUnleash(@NotNull Player player,
                                    @NotNull Entity clicked,
                                    @NotNull Cancellable event) {
        if (!(clicked instanceof Villager villager) || !player.isSneaking() || !villager.isLeashed()) {
            return;
        }
        Entity holder = villager.getLeashHolder();
        if (holder == null || !holder.getUniqueId().equals(player.getUniqueId())) {
            return;
        }
        event.setCancelled(true);
        Bukkit.getScheduler().runTask(schedulerPlugin, () -> {
            if (!villager.isValid() || villager.isDead()) {
                return;
            }
            villager.setLeashHolder(null);
            if (player.getOpenInventory() != null) {
                player.closeInventory();
            }
            player.sendActionBar(Component.text(
                    "Removed leash from " + DisplayNameUtil.actionBarEntityName(villager) + ".",
                    NamedTextColor.YELLOW
            ));
        });
    }
}

