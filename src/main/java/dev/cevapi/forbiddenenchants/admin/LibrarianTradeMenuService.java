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

final class LibrarianTradeMenuService {
    private static final int MENU_SIZE = 54;
    private static final int PAGE_SIZE = 45;
    private static final int PREV_SLOT = 45;
    private static final int ENABLE_SLOT = 47;
    private static final int MODE_SLOT = 49;
    private static final int CLEAR_SLOT = 51;
    private static final int NEXT_SLOT = 53;

    private static final double DEFAULT_CHANCE = 10.0D;
    private static final int DEFAULT_EMERALDS = 24;
    private static final int DEFAULT_BOOKS = 1;

    private final ForbiddenEnchantsPlugin plugin;

    LibrarianTradeMenuService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    void openMenu(@NotNull Player player, @NotNull LibrarianTradeMenuMode mode, int page) {
        List<LibrarianBookEntry> entries = entriesForMode(mode);
        int totalPages = Math.max(1, (entries.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, totalPages - 1));

        LibrarianTradeMenuHolder holder = new LibrarianTradeMenuHolder(mode, safePage);
        Inventory inventory = Bukkit.createInventory(
                holder,
                MENU_SIZE,
                Component.text(plugin.message("menu.librarian.title_prefix", "Librarian Trades "), NamedTextColor.GOLD)
                        .append(Component.text(
                                mode == LibrarianTradeMenuMode.ALL
                                        ? plugin.message("menu.librarian.mode_all", "(All Books)")
                                        : plugin.message("menu.librarian.mode_configured", "(Configured)"),
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
        inventory.setItem(ENABLE_SLOT, createEnabledPane());
        inventory.setItem(MODE_SLOT, createModePane(mode));
        inventory.setItem(CLEAR_SLOT, createClearPane());
        player.openInventory(inventory);
    }

    void openMenu(@NotNull Player player, int page) {
        openMenu(player, LibrarianTradeMenuMode.ALL, page);
    }

    void onMenuClick(@NotNull InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof LibrarianTradeMenuHolder holder)) {
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
        List<LibrarianBookEntry> entries = entriesForMode(holder.mode());
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
            LibrarianTradeMenuMode next = holder.mode() == LibrarianTradeMenuMode.CONFIGURED
                    ? LibrarianTradeMenuMode.ALL
                    : LibrarianTradeMenuMode.CONFIGURED;
            openMenu(player, next, 0);
            return;
        }
        if (rawSlot == ENABLE_SLOT) {
            plugin.setLibrarianTradesEnabled(!plugin.isLibrarianTradesEnabled());
            plugin.saveLibrarianTradeSettings();
            openMenu(player, holder.mode(), holder.page());
            return;
        }
        if (rawSlot == CLEAR_SLOT) {
            plugin.librarianTrades().clear();
            plugin.saveLibrarianTradeSettings();
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

        LibrarianBookEntry entry = entries.get(absoluteIndex);
        handleEntryMutation(entry, event.getClick(), event.isShiftClick());
        plugin.saveLibrarianTradeSettings();
        openMenu(player, holder.mode(), holder.page());
    }

    void onMenuDrag(@NotNull InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof LibrarianTradeMenuHolder)) {
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

    private void handleEntryMutation(@NotNull LibrarianBookEntry entry, @NotNull ClickType click, boolean shift) {
        LibrarianTradeEntry configured = configuredEntry(entry.type, entry.level);
        if (configured == null) {
            configured = new LibrarianTradeEntry(entry.type, entry.level, DEFAULT_CHANCE, DEFAULT_EMERALDS, DEFAULT_BOOKS);
        }

        double chance = configured.chancePercent();
        int emeralds = configured.emeraldCost();
        int books = configured.bookCost();

        if (click == ClickType.MIDDLE) {
            removeConfigured(entry.type, entry.level);
            return;
        }

        if (click.isLeftClick()) {
            chance = adjustChance(chance, true, shift);
            upsertConfigured(new LibrarianTradeEntry(entry.type, entry.level, chance, emeralds, books));
            return;
        }

        if (click.isRightClick()) {
            chance = adjustChance(chance, false, shift);
            upsertConfigured(new LibrarianTradeEntry(entry.type, entry.level, chance, emeralds, books));
            return;
        }

        if (click == ClickType.DROP) {
            emeralds = clampInt(emeralds + (shift ? 5 : 1), 1, 64);
            upsertConfigured(new LibrarianTradeEntry(entry.type, entry.level, Math.max(0.01D, chance), emeralds, books));
            return;
        }

        if (click == ClickType.CONTROL_DROP) {
            emeralds = clampInt(emeralds - (shift ? 5 : 1), 1, 64);
            upsertConfigured(new LibrarianTradeEntry(entry.type, entry.level, Math.max(0.01D, chance), emeralds, books));
            return;
        }

        if (click == ClickType.SWAP_OFFHAND) {
            books = clampInt(books + 1, 0, 64);
            upsertConfigured(new LibrarianTradeEntry(entry.type, entry.level, Math.max(0.01D, chance), emeralds, books));
        }
    }

    private @NotNull List<LibrarianBookEntry> entriesForMode(@NotNull LibrarianTradeMenuMode mode) {
        List<LibrarianBookEntry> entries = new ArrayList<>();
        if (mode == LibrarianTradeMenuMode.ALL) {
            for (EnchantType type : plugin.activeEnchantTypes()) {
                if (plugin.isRetiredEnchant(type)) {
                    continue;
                }
                for (int level = 1; level <= type.maxLevel; level++) {
                    entries.add(new LibrarianBookEntry(type, level));
                }
            }
            entries.sort(Comparator
                    .comparingInt((LibrarianBookEntry e) -> e.type.modelTypeIndex())
                    .thenComparingInt(e -> e.level));
            return entries;
        }

        for (LibrarianTradeEntry configured : plugin.librarianTrades()) {
            if (configured.chancePercent() > 0.0D) {
                entries.add(new LibrarianBookEntry(configured.type(), configured.level()));
            }
        }
        entries.sort(Comparator
                .comparingInt((LibrarianBookEntry e) -> e.type.modelTypeIndex())
                .thenComparingInt(e -> e.level));
        return entries;
    }

    private @NotNull ItemStack createEntryItem(@NotNull LibrarianBookEntry entry, @NotNull LibrarianTradeMenuMode mode) {
        LibrarianTradeEntry configured = configuredEntry(entry.type, entry.level);
        if (configured == null || configured.chancePercent() <= 0.0D) {
            ItemStack item = new ItemStack(Material.BARRIER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(Component.text(entry.type.arg + " " + RomanNumeralUtil.toRoman(entry.level), NamedTextColor.GRAY));
                meta.lore(List.of(
                        Component.text("Not configured", NamedTextColor.DARK_GRAY),
                        Component.text("Left: enable and +1% chance", NamedTextColor.GRAY),
                        Component.text("Shift+Left: enable and +5% chance", NamedTextColor.GRAY),
                        Component.text("Right: -1% (-0.1% below 1%, min 0.1%)", NamedTextColor.GRAY),
                        Component.text("Q/Ctrl+Q: +/- emerald cost", NamedTextColor.GRAY),
                        Component.text("F: +book cost", NamedTextColor.GRAY),
                        Component.text("Middle: clear entry", NamedTextColor.GRAY),
                        Component.text("Mode: " + (mode == LibrarianTradeMenuMode.ALL ? "All books" : "Configured"), NamedTextColor.DARK_GRAY)
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
        lore.add(Component.text("Chance: " + StructureInjectorUtil.formatPercent(configured.chancePercent()), NamedTextColor.YELLOW));
        lore.add(Component.text("Cost: " + configured.emeraldCost() + " emerald(s)"
                + (configured.bookCost() > 0 ? " + " + configured.bookCost() + " book(s)" : ""), NamedTextColor.GRAY));
        lore.add(Component.text("Left/Right: +/- chance (0.1% step below 1%)", NamedTextColor.DARK_GRAY));
        lore.add(Component.text("Shift Left/Right: +/-5% (0.5% step below 5%)", NamedTextColor.DARK_GRAY));
        lore.add(Component.text("Q/Ctrl+Q: +/- emerald cost", NamedTextColor.DARK_GRAY));
        lore.add(Component.text("F: +book cost", NamedTextColor.DARK_GRAY));
        lore.add(Component.text("Middle: disable this trade", NamedTextColor.DARK_GRAY));
        lore.add(Component.text("Mode: " + (mode == LibrarianTradeMenuMode.ALL ? "All books" : "Configured"), NamedTextColor.DARK_GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
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

    private @NotNull ItemStack createModePane(@NotNull LibrarianTradeMenuMode mode) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(
                    mode == LibrarianTradeMenuMode.CONFIGURED ? "Switch: All Books" : "Switch: Configured",
                    NamedTextColor.AQUA
            ));
            meta.lore(List.of(Component.text("Click to toggle list mode.", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createEnabledPane() {
        boolean enabled = plugin.isLibrarianTradesEnabled();
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(
                    "Librarian Blend " + (enabled ? "Enabled" : "Disabled"),
                    enabled ? NamedTextColor.GREEN : NamedTextColor.RED
            ));
            meta.lore(List.of(Component.text("Click to toggle blend injection.", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createClearPane() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Clear All Configured Trades", NamedTextColor.RED));
            meta.lore(List.of(Component.text("Click to disable all configured librarian trades.", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @Nullable LibrarianTradeEntry configuredEntry(@NotNull EnchantType type, int level) {
        for (LibrarianTradeEntry entry : plugin.librarianTrades()) {
            if (entry.type() == type && entry.level() == level) {
                return entry;
            }
        }
        return null;
    }

    private void removeConfigured(@NotNull EnchantType type, int level) {
        plugin.librarianTrades().removeIf(entry -> entry.type() == type && entry.level() == level);
    }

    private void upsertConfigured(@NotNull LibrarianTradeEntry updated) {
        removeConfigured(updated.type(), updated.level());
        if (updated.chancePercent() <= 0.0D) {
            return;
        }
        plugin.librarianTrades().add(updated);
    }

    private double clampChance(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(100.0D, value));
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

    private double roundToTenths(double value) {
        return Math.round(value * 10.0D) / 10.0D;
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record LibrarianBookEntry(@NotNull EnchantType type, int level) {
    }
}
