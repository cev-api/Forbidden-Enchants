package dev.cevapi.forbiddenenchants;

import dev.cevapi.forbiddenenchants.enchants.EnchantList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

final class NameTagLeadService {
    private final Supplier<EnchantStateService> enchantStateServiceSupplier;
    private final Supplier<MysteryItemService> mysteryItemServiceSupplier;
    private final Plugin schedulerPlugin;
    private final PlayerItemUtilityService playerItemUtilityService;
    private final long appliedCurseLevel1Ticks;
    private final long appliedCurseLevel2Ticks;
    private final Map<UUID, CharmedPetState> charmedPets;
    private final Map<UUID, org.bukkit.Location> charmedPetSitAnchors;

    NameTagLeadService(@NotNull Supplier<EnchantStateService> enchantStateServiceSupplier,
                       @NotNull Supplier<MysteryItemService> mysteryItemServiceSupplier,
                       @NotNull Plugin schedulerPlugin,
                       @NotNull PlayerItemUtilityService playerItemUtilityService,
                       long appliedCurseLevel1Ticks,
                       long appliedCurseLevel2Ticks,
                       @NotNull Map<UUID, CharmedPetState> charmedPets,
                       @NotNull Map<UUID, org.bukkit.Location> charmedPetSitAnchors) {
        this.enchantStateServiceSupplier = enchantStateServiceSupplier;
        this.mysteryItemServiceSupplier = mysteryItemServiceSupplier;
        this.schedulerPlugin = schedulerPlugin;
        this.playerItemUtilityService = playerItemUtilityService;
        this.appliedCurseLevel1Ticks = appliedCurseLevel1Ticks;
        this.appliedCurseLevel2Ticks = appliedCurseLevel2Ticks;
        this.charmedPets = charmedPets;
        this.charmedPetSitAnchors = charmedPetSitAnchors;
    }

    void onSpecialNameTagUse(@NotNull PlayerInteractEntityEvent event, long tickCounter) {
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand() == EquipmentSlot.OFF_HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
        ItemStack held = hand == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand()
                : player.getInventory().getItemInMainHand();
        if (held.getType() != Material.NAME_TAG) {
            return;
        }

        int charmedLevel = enchantStateServiceSupplier.get().getEnchantLevel(held, EnchantType.CHARMED_PET);
        int curseLevel = enchantStateServiceSupplier.get().getEnchantLevel(held, EnchantType.APPLIED_CURSE);
        if (!EnchantList.INSTANCE.charmedPet().canCharm(charmedLevel) && curseLevel <= 0) {
            return;
        }
        mysteryItemServiceSupplier.get().revealMysteryItemIfNeeded(held, player, hand);

        Entity clicked = event.getRightClicked();
        String tagText = playerItemUtilityService.getNameTagText(held);

        if (curseLevel > 0 && clicked instanceof Player target && !target.getUniqueId().equals(player.getUniqueId())) {
            long expireTick = switch (curseLevel) {
                case 1 -> tickCounter + appliedCurseLevel1Ticks;
                case 2 -> tickCounter + appliedCurseLevel2Ticks;
                default -> -1L;
            };
            String cursedName = tagText == null || tagText.isBlank() ? ("Cursed_" + target.getName()) : tagText;
            EnchantList.INSTANCE.appliedCurse().applyTo(target, cursedName, expireTick);
            playerItemUtilityService.consumeOneFromHand(player, hand);
            event.setCancelled(true);
            player.sendActionBar(Component.text(
                    ForbiddenEnchantsPlugin.instance().message(
                            "applied_curse.set_name",
                            "Applied Curse set to {name}",
                            Map.of("name", cursedName)
                    ),
                    NamedTextColor.DARK_RED
            ));
            target.sendActionBar(Component.text(
                    ForbiddenEnchantsPlugin.instance().message(
                            "applied_curse.target_notified",
                            "Your name has been cursed."
                    ),
                    NamedTextColor.RED
            ));
            Component broadcast = Component.text(ForbiddenEnchantsPlugin.instance().message("fe.prefix", "[Forbidden Enchants] "), NamedTextColor.DARK_PURPLE)
                    .append(Component.text(player.getName(), NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text(ForbiddenEnchantsPlugin.instance().message("applied_curse.broadcast_mid_1", " cursed "), NamedTextColor.GRAY))
                    .append(Component.text(target.getName(), NamedTextColor.RED))
                    .append(Component.text(ForbiddenEnchantsPlugin.instance().message("applied_curse.broadcast_mid_2", " with "), NamedTextColor.GRAY))
                    .append(Component.text(cursedName, NamedTextColor.DARK_RED))
                    .append(Component.text(ForbiddenEnchantsPlugin.instance().message("applied_curse.broadcast_suffix", "."), NamedTextColor.GRAY));
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.sendMessage(broadcast);
            }
            return;
        }

        if (EnchantList.INSTANCE.charmedPet().canCharm(charmedLevel) && clicked instanceof Mob mob) {
            if (tagText != null && !tagText.isBlank()) {
                mob.customName(Component.text(tagText, NamedTextColor.LIGHT_PURPLE));
                mob.setCustomNameVisible(true);
            }
            mob.setTarget(null);
            mob.setAware(true);
            charmedPets.put(mob.getUniqueId(), new CharmedPetState(player.getUniqueId(), false));
            charmedPetSitAnchors.remove(mob.getUniqueId());
            playerItemUtilityService.consumeOneFromHand(player, hand);
            event.setCancelled(true);
            player.sendActionBar(Component.text(
                    ForbiddenEnchantsPlugin.instance().message("charmed_pet.bound", "Charmed Pet bound."),
                    NamedTextColor.LIGHT_PURPLE
            ));
        }
    }

    void onPlayerLeashVillager(@NotNull PlayerLeashEntityEvent event) {
        if (!(event.getEntity() instanceof Villager)) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        int getOverHereMain = enchantStateServiceSupplier.get().getEnchantLevel(main, EnchantType.GET_OVER_HERE);
        int getOverHereOff = enchantStateServiceSupplier.get().getEnchantLevel(off, EnchantType.GET_OVER_HERE);

        boolean validMain = main.getType() == Material.LEAD && EnchantList.INSTANCE.getOverHere().canLeashVillagers(getOverHereMain);
        boolean validOff = off.getType() == Material.LEAD && EnchantList.INSTANCE.getOverHere().canLeashVillagers(getOverHereOff);
        if (!validMain && !validOff) {
            return;
        }
        if (!player.isSneaking()) {
            event.setCancelled(true);
        }
    }

    void onPlayerLeashVillagerFallback(@NotNull PlayerLeashEntityEvent event) {
        if (!event.isCancelled() || !(event.getEntity() instanceof Villager villager)) {
            return;
        }
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        boolean validMain = main.getType() == Material.LEAD
                && EnchantList.INSTANCE.getOverHere().canLeashVillagers(enchantStateServiceSupplier.get().getEnchantLevel(main, EnchantType.GET_OVER_HERE));
        boolean validOff = off.getType() == Material.LEAD
                && EnchantList.INSTANCE.getOverHere().canLeashVillagers(enchantStateServiceSupplier.get().getEnchantLevel(off, EnchantType.GET_OVER_HERE));
        if (!validMain && !validOff) {
            return;
        }

        Bukkit.getScheduler().runTask(schedulerPlugin, () -> {
            villager.setLeashHolder(player);
            if (player.getOpenInventory() != null) {
                player.closeInventory();
            }
        });
    }
}

