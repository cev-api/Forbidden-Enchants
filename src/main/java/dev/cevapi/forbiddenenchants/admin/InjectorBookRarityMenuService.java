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
import java.util.Map;

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

        inventory.setItem(PREV_SLOT, createNavPane(plugin, safePage > 0, true));
        inventory.setItem(NEXT_SLOT, createNavPane(plugin, safePage + 1 < totalPages, false));
        inventory.setItem(BACK_SLOT, createBackPane(plugin));
        inventory.setItem(APPLY_ITEMS_SLOT, createApplyItemsPane(plugin));
        inventory.setItem(RESET_SLOT, createResetPane(plugin));
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
                String yes = msg(plugin, "menu.injector.rarity.state.yes", "YES");
                String no = msg(plugin, "menu.injector.rarity.state.no", "NO");
                String disabledSuffix = weight <= 0.0D
                        ? msg(plugin, "menu.injector.rarity.entry.weight_disabled_suffix", " (disabled)")
                        : "";
                lore.add(Component.text(
                        msg(plugin, "menu.injector.rarity.entry.spawn_line", "Spawn enabled: {state}", Map.of("state", spawnEnabled ? yes : no)),
                        spawnEnabled ? NamedTextColor.GREEN : NamedTextColor.RED
                ));
                lore.add(Component.text(
                        msg(plugin, "menu.injector.rarity.entry.weight_line", "Weight: {weight}{suffix}", Map.of(
                                "weight", formatWeight(weight),
                                "suffix", disabledSuffix
                        )),
                        weight <= 0.0D ? NamedTextColor.RED : NamedTextColor.YELLOW
                ));
                lore.addAll(List.of(
                        Component.text(msg(plugin, "menu.injector.rarity.entry.left_right", "Left/Right: +/-0.1 (Shift: +/-1.0)"), NamedTextColor.GRAY),
                        Component.text(msg(plugin, "menu.injector.rarity.entry.drop_scale", "Q/Ctrl+Q: x2 / x0.5"), NamedTextColor.GRAY),
                        Component.text(msg(plugin, "menu.injector.rarity.entry.toggle_spawn", "F: toggle spawn enabled"), NamedTextColor.GRAY),
                        Component.text(msg(plugin, "menu.injector.rarity.entry.double_click_disable", "Double-click: set 0 (disabled)"), NamedTextColor.GRAY),
                        Component.text(msg(plugin, "menu.injector.rarity.entry.middle_reset", "Middle: reset to 1.0"), NamedTextColor.GRAY)
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
        String yes = msg(plugin, "menu.injector.rarity.state.yes", "YES");
        String no = msg(plugin, "menu.injector.rarity.state.no", "NO");
        lore.add(Component.empty());
        lore.add(Component.text(
                msg(plugin, "menu.injector.rarity.entry.spawn_line", "Spawn enabled: {state}", Map.of("state", spawnEnabled ? yes : no)),
                spawnEnabled ? NamedTextColor.GREEN : NamedTextColor.RED
        ));
        lore.add(Component.text(
                msg(plugin, "menu.injector.rarity.entry.rarity_weight_line", "Rarity Weight: {weight}", Map.of("weight", formatWeight(weight))),
                NamedTextColor.YELLOW
        ));
        lore.add(Component.text(msg(plugin, "menu.injector.rarity.entry.weight_usage", "Relative drop chance uses this weight."), NamedTextColor.GRAY));
        lore.add(Component.text(msg(plugin, "menu.injector.rarity.entry.left_right", "Left/Right: +/-0.1 (Shift: +/-1.0)"), NamedTextColor.DARK_GRAY));
        lore.add(Component.text(msg(plugin, "menu.injector.rarity.entry.drop_scale", "Q/Ctrl+Q: x2 / x0.5"), NamedTextColor.DARK_GRAY));
        lore.add(Component.text(msg(plugin, "menu.injector.rarity.entry.toggle_spawn", "F: toggle spawn enabled"), NamedTextColor.DARK_GRAY));
        lore.add(Component.text(msg(plugin, "menu.injector.rarity.entry.double_click_disable", "Double-click: set 0 (disabled)"), NamedTextColor.DARK_GRAY));
        lore.add(Component.text(msg(plugin, "menu.injector.rarity.entry.middle_reset", "Middle: reset to 1.0"), NamedTextColor.DARK_GRAY));
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

    private @NotNull ItemStack createNavPane(@NotNull ForbiddenEnchantsPlugin plugin, boolean enabled, boolean previous) {
        ItemStack pane = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            String label = previous
                    ? msg(plugin, "menu.injector.rarity.nav_prev_label", "<< Prev")
                    : msg(plugin, "menu.injector.rarity.nav_next_label", "Next >>");
            meta.displayName(Component.text(label, enabled ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private @NotNull ItemStack createResetPane(@NotNull ForbiddenEnchantsPlugin plugin) {
        ItemStack item = new ItemStack(Material.BRUSH);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(
                    msg(plugin, "menu.injector.rarity.reset_label", "Reset All Weights"),
                    NamedTextColor.RED
            ));
            meta.lore(List.of(Component.text(
                    msg(plugin, "menu.injector.rarity.reset_lore", "Click to reset all rarity weights to 1.0."),
                    NamedTextColor.GRAY
            )));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createBackPane(@NotNull ForbiddenEnchantsPlugin plugin) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(
                    msg(plugin, "menu.injector.rarity.back_label", "Back To Injector"),
                    NamedTextColor.AQUA
            ));
            meta.lore(List.of(Component.text(
                    msg(plugin, "menu.injector.rarity.back_lore", "Return to /fe injector gui."),
                    NamedTextColor.GRAY
            )));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createApplyItemsPane(@NotNull ForbiddenEnchantsPlugin plugin) {
        boolean enabled = plugin.isInjectorRarityApplyToItems();
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String state = enabled
                    ? msg(plugin, "menu.injector.rarity.state.on", "ON")
                    : msg(plugin, "menu.injector.rarity.state.off", "OFF");
            meta.displayName(Component.text(
                    msg(
                            plugin,
                            "menu.injector.rarity.apply_items_label",
                            "Apply Weights To Enchanted Items: {state}",
                            Map.of("state", state)
                    ),
                    enabled ? NamedTextColor.GREEN : NamedTextColor.RED
            ));
            meta.lore(List.of(
                    Component.text(msg(plugin, "menu.injector.rarity.apply_items_on", "ON: weights affect books and enchanted items."), NamedTextColor.GRAY),
                    Component.text(msg(plugin, "menu.injector.rarity.apply_items_off", "OFF: weights affect books only."), NamedTextColor.DARK_GRAY)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull String formatWeight(double weight) {
        return String.format(java.util.Locale.ROOT, "%.1f", Math.max(0.0D, Math.round(weight * 10.0D) / 10.0D));
    }

    private @NotNull String msg(@NotNull ForbiddenEnchantsPlugin plugin, @NotNull String key, @NotNull String fallback) {
        return plugin.message(key, fallback);
    }

    private @NotNull String msg(@NotNull ForbiddenEnchantsPlugin plugin,
                                @NotNull String key,
                                @NotNull String fallback,
                                @NotNull Map<String, String> placeholders) {
        return plugin.message(key, fallback, placeholders);
    }

    private record BookEntry(@NotNull EnchantType type, int level) {
    }
}
