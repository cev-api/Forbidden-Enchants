package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class InjectorBookRarityMenuService {
    private static final int MENU_SIZE = 54;
    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int BACK_SLOT = 46;
    private static final int APPLY_ITEMS_SLOT = 48;
    private static final int RESET_SLOT = 49;
    private static final int NEXT_SLOT = 53;

    InjectorBookRarityMenuService() {
    }

    void openMenu(@NotNull Player player, @NotNull ForbiddenEnchantsPlugin plugin, int page) {
        List<BookEntry> entries = allBookEntries(plugin);
        int totalPages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, totalPages - 1));

        InjectorBookRarityMenuHolder holder = new InjectorBookRarityMenuHolder(safePage);
        Inventory inventory = Bukkit.createInventory(
                holder,
                MENU_SIZE,
                Component.text(plugin.message("menu.injector.rarity_title", "Enchantment Rarity Editor"), NamedTextColor.LIGHT_PURPLE)
                        .append(Component.text(" [" + (safePage + 1) + "/" + totalPages + "]", NamedTextColor.GRAY))
        );
        holder.attach(inventory);

        int start = safePage * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        int slot = 0;
        for (int i = start; i < end; i++) {
            inventory.setItem(slot++, createEntryItem(plugin, entries.get(i)));
        }

        inventory.setItem(PREV_SLOT, createNavPane(safePage > 0, true));
        inventory.setItem(NEXT_SLOT, createNavPane(safePage + 1 < totalPages, false));
        inventory.setItem(BACK_SLOT, createBackPane());
        inventory.setItem(APPLY_ITEMS_SLOT, createApplyItemsPane(plugin));
        inventory.setItem(RESET_SLOT, createResetPane());
        player.openInventory(inventory);
    }

    void onMenuClick(@NotNull InventoryClickEvent event, @NotNull ForbiddenEnchantsPlugin plugin) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof InjectorBookRarityMenuHolder holder)) {
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
        List<BookEntry> entries = allBookEntries(plugin);
        int totalPages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);

        if (rawSlot == PREV_SLOT) {
            if (holder.page() > 0) {
                openMenu(player, plugin, holder.page() - 1);
            }
            return;
        }
        if (rawSlot == NEXT_SLOT) {
            if (holder.page() + 1 < totalPages) {
                openMenu(player, plugin, holder.page() + 1);
            }
            return;
        }
        if (rawSlot == BACK_SLOT) {
            plugin.openInjectorConfigMenu(player);
            return;
        }
        if (rawSlot == APPLY_ITEMS_SLOT) {
            plugin.setInjectorRarityApplyToItems(!plugin.isInjectorRarityApplyToItems());
            plugin.saveStructureInjectorSettings();
            openMenu(player, plugin, holder.page());
            return;
        }
        if (rawSlot == RESET_SLOT) {
            plugin.injectorBookRarityWeights().clear();
            plugin.saveStructureInjectorSettings();
            openMenu(player, plugin, holder.page());
            return;
        }

        if (rawSlot < 0 || rawSlot >= PAGE_SIZE) {
            return;
        }

        int absoluteIndex = holder.page() * PAGE_SIZE + rawSlot;
        if (absoluteIndex < 0 || absoluteIndex >= entries.size()) {
            return;
        }

        BookEntry entry = entries.get(absoluteIndex);
        double weight = plugin.injectorBookRarityWeight(entry.type, entry.level);

        if (event.getClick() == ClickType.SWAP_OFFHAND) {
            plugin.setEnchantSpawnEnabled(entry.type, !plugin.isEnchantSpawnEnabled(entry.type));
            plugin.saveEnchantToggleSettings();
        } else if (event.getClick() == ClickType.MIDDLE) {
            plugin.setInjectorBookRarityWeight(entry.type, entry.level, 1.0D);
        } else if (event.getClick().isLeftClick()) {
            weight += event.isShiftClick() ? 1.0D : 0.1D;
            plugin.setInjectorBookRarityWeight(entry.type, entry.level, weight);
        } else if (event.getClick().isRightClick()) {
            weight -= event.isShiftClick() ? 1.0D : 0.1D;
            plugin.setInjectorBookRarityWeight(entry.type, entry.level, weight);
        } else if (event.getClick() == ClickType.DROP) {
            plugin.setInjectorBookRarityWeight(entry.type, entry.level, weight * 2.0D);
        } else if (event.getClick() == ClickType.CONTROL_DROP) {
            plugin.setInjectorBookRarityWeight(entry.type, entry.level, weight * 0.5D);
        } else if (event.getClick() == ClickType.DOUBLE_CLICK) {
            plugin.setInjectorBookRarityWeight(entry.type, entry.level, 0.0D);
        } else {
            return;
        }

        plugin.saveStructureInjectorSettings();
        openMenu(player, plugin, holder.page());
    }

    void onMenuDrag(@NotNull InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof InjectorBookRarityMenuHolder)) {
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

    private @NotNull ItemStack createEntryItem(@NotNull ForbiddenEnchantsPlugin plugin, @NotNull BookEntry entry) {
        double weight = plugin.injectorBookRarityWeight(entry.type, entry.level);
        boolean spawnEnabled = plugin.isEnchantSpawnEnabled(entry.type);
        if (!spawnEnabled || weight <= 0.0D) {
            ItemStack barrier = new ItemStack(Material.BARRIER);
            ItemMeta meta = barrier.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(entry.type.arg + " " + RomanNumeralUtil.toRoman(entry.level), NamedTextColor.GRAY));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Spawn enabled: " + (spawnEnabled ? "YES" : "NO"), spawnEnabled ? NamedTextColor.GREEN : NamedTextColor.RED));
                lore.add(Component.text("Weight: " + formatWeight(weight) + (weight <= 0.0D ? " (disabled)" : ""), weight <= 0.0D ? NamedTextColor.RED : NamedTextColor.YELLOW));
                lore.addAll(List.of(
                        Component.text("Left/Right: +/-0.1 (Shift: +/-1.0)", NamedTextColor.GRAY),
                        Component.text("Q/Ctrl+Q: x2 / x0.5", NamedTextColor.GRAY),
                        Component.text("F: toggle spawn enabled", NamedTextColor.GRAY),
                        Component.text("Double-click: set 0 (disabled)", NamedTextColor.GRAY),
                        Component.text("Middle: reset to 1.0", NamedTextColor.GRAY)
                ));
                meta.lore(lore);
                barrier.setItemMeta(meta);
            }
            return barrier;
        }

        ItemStack item = plugin.createBook(entry.type, entry.level);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(Component.empty());
        lore.add(Component.text("Spawn enabled: " + (spawnEnabled ? "YES" : "NO"), spawnEnabled ? NamedTextColor.GREEN : NamedTextColor.RED));
        lore.add(Component.text("Rarity Weight: " + formatWeight(weight), NamedTextColor.YELLOW));
        lore.add(Component.text("Relative drop chance uses this weight.", NamedTextColor.GRAY));
        lore.add(Component.text("Left/Right: +/-0.1 (Shift: +/-1.0)", NamedTextColor.DARK_GRAY));
        lore.add(Component.text("Q/Ctrl+Q: x2 / x0.5", NamedTextColor.DARK_GRAY));
        lore.add(Component.text("F: toggle spawn enabled", NamedTextColor.DARK_GRAY));
        lore.add(Component.text("Double-click: set 0 (disabled)", NamedTextColor.DARK_GRAY));
        lore.add(Component.text("Middle: reset to 1.0", NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private @NotNull List<BookEntry> allBookEntries(@NotNull ForbiddenEnchantsPlugin plugin) {
        List<BookEntry> entries = new ArrayList<>();
        for (EnchantType type : plugin.activeEnchantTypes()) {
            if (plugin.isRetiredEnchant(type)) {
                continue;
            }
            for (int level = 1; level <= type.maxLevel; level++) {
                entries.add(new BookEntry(type, level));
            }
        }
        entries.sort(Comparator
                .comparingInt((BookEntry entry) -> entry.type.modelTypeIndex())
                .thenComparingInt(entry -> entry.level));
        return entries;
    }

    private @NotNull ItemStack createNavPane(boolean enabled, boolean previous) {
        ItemStack pane = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            String label = previous ? "<< Prev" : "Next >>";
            meta.displayName(Component.text(label, enabled ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private @NotNull ItemStack createResetPane() {
        ItemStack item = new ItemStack(Material.BRUSH);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Reset All Weights", NamedTextColor.RED));
            meta.lore(List.of(Component.text("Click to reset all rarity weights to 1.0.", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createBackPane() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Back To Injector", NamedTextColor.AQUA));
            meta.lore(List.of(Component.text("Return to /fe injector gui.", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createApplyItemsPane(@NotNull ForbiddenEnchantsPlugin plugin) {
        boolean enabled = plugin.isInjectorRarityApplyToItems();
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(
                    "Apply Weights To Enchanted Items: " + (enabled ? "ON" : "OFF"),
                    enabled ? NamedTextColor.GREEN : NamedTextColor.RED
            ));
            meta.lore(List.of(
                    Component.text("ON: weights affect books and enchanted items.", NamedTextColor.GRAY),
                    Component.text("OFF: weights affect books only.", NamedTextColor.DARK_GRAY)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull String formatWeight(double weight) {
        return String.format(java.util.Locale.ROOT, "%.1f", Math.max(0.0D, Math.round(weight * 10.0D) / 10.0D));
    }

    private record BookEntry(@NotNull EnchantType type, int level) {
    }
}
