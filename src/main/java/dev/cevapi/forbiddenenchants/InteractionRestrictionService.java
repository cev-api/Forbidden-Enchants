package dev.cevapi.forbiddenenchants;

import dev.cevapi.forbiddenenchants.enchants.EnchantList;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.LongSupplier;
import java.util.function.Supplier;

final class InteractionRestrictionService {
    private final Supplier<EnchantStateService> enchantStateServiceSupplier;
    private final MiasmaVisualService miasmaVisualService;
    private final Supplier<PlayerEffectService> playerEffectServiceSupplier;
    private final Supplier<ItemCombatService> itemCombatServiceSupplier;
    private final LongSupplier tickCounterSupplier;

    InteractionRestrictionService(@NotNull Supplier<EnchantStateService> enchantStateServiceSupplier,
                                  @NotNull MiasmaVisualService miasmaVisualService,
                                  @NotNull Supplier<PlayerEffectService> playerEffectServiceSupplier,
                                  @NotNull Supplier<ItemCombatService> itemCombatServiceSupplier,
                                  @NotNull LongSupplier tickCounterSupplier) {
        this.enchantStateServiceSupplier = enchantStateServiceSupplier;
        this.miasmaVisualService = miasmaVisualService;
        this.playerEffectServiceSupplier = playerEffectServiceSupplier;
        this.itemCombatServiceSupplier = itemCombatServiceSupplier;
        this.tickCounterSupplier = tickCounterSupplier;
    }

    void onMiasmaInteraction(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (!miasmaVisualService.hasForm(player)) {
            ItemStack boots = player.getInventory().getBoots();
            int lockedOutLevel = enchantStateServiceSupplier.get().getEnchantLevel(boots, EnchantType.LOCKED_OUT);
            if (EnchantList.INSTANCE.lockedOut().isActive(lockedOutLevel)
                    && event.getAction() == Action.RIGHT_CLICK_BLOCK
                    && event.getClickedBlock() != null
                    && playerEffectServiceSupplier.get().isLockedOutInteractionBlock(event.getClickedBlock().getType())) {
                event.setCancelled(true);
                event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
                event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
                player.sendActionBar(Component.text("You're locked out.", NamedTextColor.RED));
            }
            return;
        }

        event.setCancelled(true);
        event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
    }

    void onMiasmaBreak(@NotNull BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (miasmaVisualService.hasForm(player)) {
            event.setCancelled(true);
            return;
        }
        ItemStack tool = player.getInventory().getItemInMainHand();
        int lumberjackLevel = enchantStateServiceSupplier.get().getEnchantLevel(tool, EnchantType.LUMBERJACK);
        if (EnchantList.INSTANCE.lumberjack().shouldBreakWholeTree(
                lumberjackLevel,
                playerEffectServiceSupplier.get().isAxe(tool),
                itemCombatServiceSupplier.get().isBottomTreeLog(event.getBlock()))) {
            itemCombatServiceSupplier.get().breakWholeTree(player, event.getBlock(), tool);
        }
    }

    void onMiasmaPlace(@NotNull BlockPlaceEvent event) {
        if (miasmaVisualService.hasForm(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    void onGreedDropItem(@NotNull PlayerDropItemEvent event) {
        ItemStack leggings = event.getPlayer().getInventory().getLeggings();
        int greedLevel = enchantStateServiceSupplier.get().getEnchantLevel(leggings, EnchantType.GREED);
        if (!EnchantList.INSTANCE.greed().preventsDropping(greedLevel)) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendActionBar(Component.text("Greed prevents dropping items.", NamedTextColor.RED));
    }

    void onWingClipperEquipBlock(@NotNull InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!playerEffectServiceSupplier.get().isWingClipperBlocked(player, tickCounterSupplier.getAsLong())) {
            return;
        }
        if (event.getCurrentItem() == null && event.getCursor() == null) {
            return;
        }

        boolean equippingElytra = false;
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();
        if (cursor != null && cursor.getType() == Material.ELYTRA && event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR) {
            equippingElytra = true;
        }
        if (!equippingElytra && event.isShiftClick() && current != null && current.getType() == Material.ELYTRA) {
            equippingElytra = true;
        }
        if (!equippingElytra
                && event.getClick() == ClickType.NUMBER_KEY
                && event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.ARMOR) {
            ItemStack hotbar = player.getInventory().getItem(event.getHotbarButton());
            if (hotbar != null && hotbar.getType() == Material.ELYTRA) {
                equippingElytra = true;
            }
        }
        if (!equippingElytra) {
            return;
        }

        event.setCancelled(true);
        player.sendActionBar(Component.text("Wing Clipper prevents Elytra equip.", NamedTextColor.RED));
    }

    void onWingClipperDragBlock(@NotNull InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!playerEffectServiceSupplier.get().isWingClipperBlocked(player, tickCounterSupplier.getAsLong())) {
            return;
        }
        int chestRawSlot = event.getView().convertSlot(38);
        ItemStack chestItem = event.getNewItems().get(chestRawSlot);
        if (chestItem == null || chestItem.getType() != Material.ELYTRA) {
            return;
        }
        event.setCancelled(true);
        player.sendActionBar(Component.text("Wing Clipper prevents Elytra equip.", NamedTextColor.RED));
    }

    void onWingClipperInteractBlock(@NotNull PlayerInteractEvent event) {
        if (!playerEffectServiceSupplier.get().isWingClipperBlocked(event.getPlayer(), tickCounterSupplier.getAsLong())) {
            return;
        }
        if (event.getItem() == null || event.getItem().getType() != Material.ELYTRA) {
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        event.setCancelled(true);
        event.getPlayer().sendActionBar(Component.text("Wing Clipper prevents Elytra equip.", NamedTextColor.RED));
    }
}

