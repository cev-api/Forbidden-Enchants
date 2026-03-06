package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final class FeMenuService {
    private static final int FE_MENU_SIZE = 54;
    private static final int FE_MENU_PAGE_SIZE = 45;
    private static final int FE_MENU_PREV_SLOT = 45;
    private static final int FE_MENU_NEXT_SLOT = 53;

    private final ForbiddenEnchantsPlugin plugin;

    FeMenuService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    void openMenu(@NotNull Player player, int page) {
        plugin.ensureMenuPagesBuilt();

        int totalPages = Math.max(1, plugin.menuPages().size());
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        plugin.rememberLastMenuPage(player.getUniqueId(), safePage);

        List<EnchantType> visible = plugin.activeEnchantTypes();
        if (visible.isEmpty()) {
            return;
        }
        EnchantType pageType = visible.get(Math.min(safePage, visible.size() - 1));

        FeMenuHolder holder = new FeMenuHolder(safePage);
        Inventory inventory = Bukkit.createInventory(
                holder,
                FE_MENU_SIZE,
                Component.text("Forbidden " + pageType.displayName + " ", pageType.color)
                        .append(Component.text("(" + (safePage + 1) + "/" + totalPages + ")", NamedTextColor.WHITE))
        );
        holder.attach(inventory);

        List<ItemStack> pageItems = plugin.menuPages().get(safePage);
        for (int slot = 0; slot < Math.min(FE_MENU_PAGE_SIZE, pageItems.size()); slot++) {
            inventory.setItem(slot, plugin.toMenuDisplayItem(pageItems.get(slot)));
        }

        inventory.setItem(FE_MENU_PREV_SLOT, plugin.createMenuNavPane(totalPages > 1, true));
        inventory.setItem(FE_MENU_NEXT_SLOT, plugin.createMenuNavPane(safePage + 1 < totalPages, false));
        player.openInventory(inventory);
    }

    void onMenuClick(@NotNull InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof FeMenuHolder holder)) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        Inventory clicked = event.getClickedInventory();
        if (clicked == null) {
            return;
        }

        if (!clicked.equals(top)) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
            }
            return;
        }

        event.setCancelled(true);
        int rawSlot = event.getRawSlot();
        int totalPages = Math.max(1, plugin.menuPages().size());
        int pageStep = event.isRightClick() ? 5 : 1;

        if (rawSlot == FE_MENU_PREV_SLOT) {
            if (totalPages > 1) {
                int target = (holder.page() - pageStep) % totalPages;
                if (target < 0) {
                    target += totalPages;
                }
                openMenu(player, target);
            }
            return;
        }
        if (rawSlot == FE_MENU_NEXT_SLOT) {
            if (holder.page() + 1 < totalPages) {
                openMenu(player, holder.page() + pageStep);
            }
            return;
        }

        if (rawSlot < 0 || rawSlot >= FE_MENU_PAGE_SIZE) {
            return;
        }

        if (holder.page() < 0 || holder.page() >= plugin.menuPages().size()) {
            return;
        }
        List<ItemStack> pageItems = plugin.menuPages().get(holder.page());
        if (rawSlot >= pageItems.size()) {
            return;
        }

        ItemStack reward = pageItems.get(rawSlot).clone();
        plugin.giveOrDrop(player, reward);
        player.sendMessage(Component.text("[Forbidden Enchants] ", NamedTextColor.DARK_PURPLE)
                .append(Component.text("Claimed " + plugin.describeItem(reward) + ".", NamedTextColor.GREEN)));
    }

    void onMenuDrag(@NotNull InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof FeMenuHolder)) {
            return;
        }

        int topSize = top.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }
}

