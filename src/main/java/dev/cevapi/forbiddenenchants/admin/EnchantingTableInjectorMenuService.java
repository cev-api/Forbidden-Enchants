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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

final class EnchantingTableInjectorMenuService {
    private static final int MENU_SIZE = 54;
    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int XP_SLOT = 46;
    private static final int ENABLE_SLOT = 47;
    private static final int MODE_SLOT = 49;
    private static final int CLEAR_SLOT = 51;
    private static final int NEXT_SLOT = 53;
    private static final double DEFAULT_CHANCE = 10.0D;

    private final ForbiddenEnchantsPlugin plugin;

    EnchantingTableInjectorMenuService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    void openMenu(@NotNull Player player, @NotNull EnchantingTableMenuMode mode, int page) {
        List<EnchantingBookEntry> entries = entriesForMode(mode);
        int totalPages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, totalPages - 1));

        EnchantingTableMenuHolder holder = new EnchantingTableMenuHolder(mode, safePage);
        Inventory inventory = Bukkit.createInventory(
                holder,
                MENU_SIZE,
                Component.text(plugin.message("menu.enchanting.title_prefix", "Enchanting Table Injector "), NamedTextColor.GOLD)
                        .append(Component.text(
                                mode == EnchantingTableMenuMode.ALL
                                        ? plugin.message("menu.enchanting.mode_all", "(All Books)")
                                        : plugin.message("menu.enchanting.mode_configured", "(Configured)"),
                                NamedTextColor.WHITE
                        ))
                        .append(Component.text(" [" + (safePage + 1) + "/" + totalPages + "]", NamedTextColor.GRAY))
        );
        holder.attach(inventory);

        int start = safePage * PAGE_SIZE;
        int end = Math.min(entries.size(), start + PAGE_SIZE);
        int slot = 0;
        for (int i = start; i < end; i++) {
            inventory.setItem(slot++, createEntryItem(entries.get(i), mode));
        }

        inventory.setItem(PREV_SLOT, createNavPane(safePage > 0, true));
        inventory.setItem(NEXT_SLOT, createNavPane(safePage + 1 < totalPages, false));
        inventory.setItem(XP_SLOT, createXpPane());
        inventory.setItem(ENABLE_SLOT, createEnabledPane());
        inventory.setItem(MODE_SLOT, createModePane(mode));
        inventory.setItem(CLEAR_SLOT, createClearPane());
        player.openInventory(inventory);
    }

    void openMenu(@NotNull Player player, int page) {
        openMenu(player, EnchantingTableMenuMode.ALL, page);
    }

    void onMenuClick(@NotNull InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EnchantingTableMenuHolder holder)) {
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
        List<EnchantingBookEntry> entries = entriesForMode(holder.mode());
        int totalPages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);

        if (rawSlot == PREV_SLOT) {
            if (holder.page() > 0) {
                openMenu(player, holder.mode(), holder.page() - 1);
            }
            return;
        }
        if (rawSlot == NEXT_SLOT) {
            if (holder.page() + 1 < totalPages) {
                openMenu(player, holder.mode(), holder.page() + 1);
            }
            return;
        }
        if (rawSlot == MODE_SLOT) {
            EnchantingTableMenuMode next = holder.mode() == EnchantingTableMenuMode.CONFIGURED
                    ? EnchantingTableMenuMode.ALL
                    : EnchantingTableMenuMode.CONFIGURED;
            openMenu(player, next, 0);
            return;
        }
        if (rawSlot == XP_SLOT) {
            int current = plugin.getEnchantingTableInjectorXpCost();
            int delta = event.isShiftClick() ? 5 : 1;
            int updated = switch (event.getClick()) {
                case LEFT -> clampInt(current + delta, 1, 60);
                case RIGHT -> clampInt(current - delta, 1, 60);
                case MIDDLE -> 35;
                default -> current;
            };
            if (updated != current) {
                plugin.setEnchantingTableInjectorXpCost(updated);
                plugin.saveEnchantingTableInjectorSettings();
            }
            openMenu(player, holder.mode(), holder.page());
            return;
        }
        if (rawSlot == ENABLE_SLOT) {
            plugin.setEnchantingTableInjectorEnabled(!plugin.isEnchantingTableInjectorEnabled());
            plugin.saveEnchantingTableInjectorSettings();
            openMenu(player, holder.mode(), holder.page());
            return;
        }
        if (rawSlot == CLEAR_SLOT) {
            plugin.enchantingTableInjectorBooks().clear();
            plugin.saveEnchantingTableInjectorSettings();
            openMenu(player, holder.mode(), 0);
            return;
        }
        if (rawSlot < 0 || rawSlot >= PAGE_SIZE) {
            return;
        }

        int absoluteIndex = holder.page() * PAGE_SIZE + rawSlot;
        if (absoluteIndex < 0 || absoluteIndex >= entries.size()) {
            return;
        }

        EnchantingBookEntry entry = entries.get(absoluteIndex);
        handleEntryMutation(entry, event.getClick(), event.isShiftClick());
        plugin.saveEnchantingTableInjectorSettings();
        openMenu(player, holder.mode(), holder.page());
    }

    void onMenuDrag(@NotNull InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EnchantingTableMenuHolder)) {
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

    private void handleEntryMutation(@NotNull EnchantingBookEntry entry, @NotNull ClickType click, boolean shift) {
        EnchantingTableBookEntry configured = configuredEntry(entry.type, entry.level);
        if (configured == null) {
            configured = new EnchantingTableBookEntry(entry.type, entry.level, DEFAULT_CHANCE);
        }

        if (click == ClickType.MIDDLE) {
            removeConfigured(entry.type, entry.level);
            return;
        }
        if (click.isLeftClick()) {
            double chance = adjustChance(configured.chancePercent(), true, shift);
            upsertConfigured(new EnchantingTableBookEntry(entry.type, entry.level, chance));
            return;
        }
        if (click.isRightClick()) {
            double chance = adjustChance(configured.chancePercent(), false, shift);
            upsertConfigured(new EnchantingTableBookEntry(entry.type, entry.level, chance));
        }
    }

    private @NotNull List<EnchantingBookEntry> entriesForMode(@NotNull EnchantingTableMenuMode mode) {
        List<EnchantingBookEntry> entries = new ArrayList<>();
        if (mode == EnchantingTableMenuMode.ALL) {
            for (EnchantType type : plugin.activeEnchantTypes()) {
                if (plugin.isRetiredEnchant(type) || type.isAnvilOnlyUtilityBook()) {
                    continue;
                }
                for (int level = 1; level <= type.maxLevel; level++) {
                    entries.add(new EnchantingBookEntry(type, level));
                }
            }
        } else {
            for (EnchantingTableBookEntry configured : plugin.enchantingTableInjectorBooks()) {
                if (configured.chancePercent() > 0.0D) {
                    entries.add(new EnchantingBookEntry(configured.type(), configured.level()));
                }
            }
        }
        entries.sort(Comparator
                .comparingInt((EnchantingBookEntry e) -> e.type.modelTypeIndex())
                .thenComparingInt(e -> e.level));
        return entries;
    }

    private @NotNull ItemStack createEntryItem(@NotNull EnchantingBookEntry entry, @NotNull EnchantingTableMenuMode mode) {
        EnchantingTableBookEntry configured = configuredEntry(entry.type, entry.level);
        if (configured == null || configured.chancePercent() <= 0.0D) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(entry.type.arg + " " + RomanNumeralUtil.toRoman(entry.level), NamedTextColor.GRAY));
                meta.lore(List.of(
                        Component.text(msg("menu.enchanting.entry.not_configured", "Not configured"), NamedTextColor.DARK_GRAY),
                        Component.text(msg("menu.enchanting.entry.left_enable", "Left: enable and +1% chance"), NamedTextColor.GRAY),
                        Component.text(msg("menu.enchanting.entry.shift_left_enable", "Shift+Left: enable and +5% chance"), NamedTextColor.GRAY),
                        Component.text(msg("menu.enchanting.entry.right_adjust", "Right: -1% (-0.1% below 1%, min 0.1%)"), NamedTextColor.GRAY),
                        Component.text(msg("menu.enchanting.entry.middle_clear", "Middle: clear entry"), NamedTextColor.GRAY),
                        Component.text(
                                msg("menu.enchanting.entry.mode_line", "Mode: {mode}", Map.of(
                                        "mode",
                                        mode == EnchantingTableMenuMode.ALL
                                                ? msg("menu.enchanting.entry.mode_all_books", "All books")
                                                : msg("menu.enchanting.entry.mode_configured", "Configured")
                                )),
                                NamedTextColor.DARK_GRAY
                        )
                ));
                item.setItemMeta(meta);
            }
            return item;
        }

        ItemStack item = plugin.createBook(configured.type(), configured.level());
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.add(Component.empty());
        lore.add(Component.text(
                msg("menu.enchanting.entry.chance_line", "Chance: {chance}", Map.of("chance", StructureInjectorUtil.formatPercent(configured.chancePercent()))),
                NamedTextColor.YELLOW
        ));
        lore.add(Component.text(msg("menu.enchanting.entry.left_right", "Left/Right: +/- chance (0.1% step below 1%)"), NamedTextColor.DARK_GRAY));
        lore.add(Component.text(msg("menu.enchanting.entry.shift_left_right", "Shift Left/Right: +/-5% (0.5% step below 5%)"), NamedTextColor.DARK_GRAY));
        lore.add(Component.text(msg("menu.enchanting.entry.middle_disable", "Middle: disable this book"), NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private @NotNull ItemStack createNavPane(boolean enabled, boolean previous) {
        ItemStack pane = new ItemStack(Material.WHITE_STAINED_GLASS_PANE);
        ItemMeta meta = pane.getItemMeta();
        if (meta != null) {
            String label = previous
                    ? msg("menu.enchanting.nav_prev_label", "<< Prev")
                    : msg("menu.enchanting.nav_next_label", "Next >>");
            meta.displayName(Component.text(label, enabled ? NamedTextColor.GREEN : NamedTextColor.DARK_GRAY));
            pane.setItemMeta(meta);
        }
        return pane;
    }

    private @NotNull ItemStack createModePane(@NotNull EnchantingTableMenuMode mode) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(
                    mode == EnchantingTableMenuMode.CONFIGURED
                            ? msg("menu.enchanting.mode_switch_all_label", "Switch: All Books")
                            : msg("menu.enchanting.mode_switch_configured_label", "Switch: Configured"),
                    NamedTextColor.AQUA
            ));
            meta.lore(List.of(Component.text(msg("menu.enchanting.mode_pane_lore", "Click to toggle list mode."), NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createEnabledPane() {
        boolean enabled = plugin.isEnchantingTableInjectorEnabled();
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String state = enabled
                    ? msg("menu.enchanting.state.enabled", "Enabled")
                    : msg("menu.enchanting.state.disabled", "Disabled");
            meta.displayName(Component.text(
                    msg("menu.enchanting.enabled_pane_label", "Enchanting Injector {state}", Map.of("state", state)),
                    enabled ? NamedTextColor.GREEN : NamedTextColor.RED
            ));
            meta.lore(List.of(Component.text(msg("menu.enchanting.enabled_pane_lore", "Click to toggle enchanting-table injection."), NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createXpPane() {
        ItemStack item = new ItemStack(Material.EXPERIENCE_BOTTLE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            int xpCost = plugin.getEnchantingTableInjectorXpCost();
            meta.displayName(Component.text(
                    msg("menu.enchanting.xp_pane_label", "Injected XP Cost: {xp}", Map.of("xp", String.valueOf(xpCost))),
                    NamedTextColor.GOLD
            ));
            meta.lore(List.of(
                    Component.text(msg("menu.enchanting.xp_pane_lore_1", "Left/Right: +/-1 XP (Shift +/-5)"), NamedTextColor.GRAY),
                    Component.text(msg("menu.enchanting.xp_pane_lore_2", "Middle: reset to 35"), NamedTextColor.DARK_GRAY)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createClearPane() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(
                    msg("menu.enchanting.clear_pane_label", "Clear All Configured Books"),
                    NamedTextColor.RED
            ));
            meta.lore(List.of(Component.text(
                    msg("menu.enchanting.clear_pane_lore", "Click to disable all configured enchanting-table books."),
                    NamedTextColor.GRAY
            )));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @Nullable EnchantingTableBookEntry configuredEntry(@NotNull EnchantType type, int level) {
        for (EnchantingTableBookEntry entry : plugin.enchantingTableInjectorBooks()) {
            if (entry.type() == type && entry.level() == level) {
                return entry;
            }
        }
        return null;
    }

    private void removeConfigured(@NotNull EnchantType type, int level) {
        plugin.enchantingTableInjectorBooks().removeIf(entry -> entry.type() == type && entry.level() == level);
    }

    private void upsertConfigured(@NotNull EnchantingTableBookEntry updated) {
        removeConfigured(updated.type(), updated.level());
        if (updated.chancePercent() <= 0.0D) {
            return;
        }
        plugin.enchantingTableInjectorBooks().add(updated);
    }

    private double adjustChance(double current, boolean increase, boolean shift) {
        double safe = clampChance(current);
        if (safe <= 0.0D) {
            return safe;
        }
        double step;
        if (shift) {
            step = increase ? (safe < 5.0D ? 0.5D : 5.0D) : (safe <= 5.0D ? 0.5D : 5.0D);
        } else {
            step = increase ? (safe < 1.0D ? 0.1D : 1.0D) : (safe <= 1.0D ? 0.1D : 1.0D);
        }
        double changed = increase ? safe + step : safe - step;
        if (!increase && changed < 0.1D) {
            changed = 0.1D;
        }
        return roundToTenths(clampChance(changed));
    }

    private double clampChance(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(100.0D, value));
    }

    private double roundToTenths(double value) {
        return Math.round(value * 10.0D) / 10.0D;
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private @NotNull String msg(@NotNull String key, @NotNull String fallback) {
        return plugin.message(key, fallback);
    }

    private @NotNull String msg(@NotNull String key,
                                @NotNull String fallback,
                                @NotNull Map<String, String> placeholders) {
        return plugin.message(key, fallback, placeholders);
    }

    private record EnchantingBookEntry(@NotNull EnchantType type, int level) {
    }
}
