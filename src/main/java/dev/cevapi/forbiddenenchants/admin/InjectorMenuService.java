package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class InjectorMenuService {
    private static final int INJECTOR_MENU_SIZE = 54;
    private static final int INJECTOR_MENU_PAGE_SIZE = 45;
    private static final int INJECTOR_MENU_PREV_SLOT = 45;
    private static final int INJECTOR_MENU_ENABLE_SLOT = 47;
    private static final int INJECTOR_MENU_NOTIFY_SLOT = 48;
    private static final int INJECTOR_MENU_MODE_SLOT = 49;
    private static final int INJECTOR_MENU_RARITY_SLOT = 50;
    private static final int INJECTOR_MENU_CLEAR_SLOT = 51;
    private static final int INJECTOR_MENU_NEXT_SLOT = 53;
    private static final double DEFAULT_VAULT_INJECT_CHANCE = 7.5D;

    private final ForbiddenEnchantsPlugin plugin;

    InjectorMenuService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    void openMenu(@NotNull Player player, @NotNull InjectorMenuMode mode, int page) {
        List<InjectorEntry> entries = entriesForMode(mode);
        int totalPages = Math.max(1, (entries.size() + INJECTOR_MENU_PAGE_SIZE - 1) / INJECTOR_MENU_PAGE_SIZE);
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        InjectorMenuHolder holder = new InjectorMenuHolder(mode, safePage);
        Inventory inventory = Bukkit.createInventory(
                holder,
                INJECTOR_MENU_SIZE,
                Component.text(plugin.message("menu.injector.title_prefix", "Structure Injector "), NamedTextColor.LIGHT_PURPLE)
                        .append(Component.text(
                                mode == InjectorMenuMode.CONFIGURED
                                        ? plugin.message("menu.injector.mode_configured", "(Configured)")
                                        : plugin.message("menu.injector.mode_all", "(All)"),
                                NamedTextColor.WHITE
                        ))
                        .append(Component.text(" [" + (safePage + 1) + "/" + totalPages + "]", NamedTextColor.GRAY))
        );
        holder.attach(inventory);

        int start = safePage * INJECTOR_MENU_PAGE_SIZE;
        int end = Math.min(entries.size(), start + INJECTOR_MENU_PAGE_SIZE);
        int slot = 0;
        for (int i = start; i < end; i++) {
            inventory.setItem(slot++, createEntryItem(entries.get(i), mode));
        }

        inventory.setItem(INJECTOR_MENU_PREV_SLOT, createNavPane(safePage > 0, true));
        inventory.setItem(INJECTOR_MENU_NEXT_SLOT, createNavPane(safePage + 1 < totalPages, false));
        inventory.setItem(INJECTOR_MENU_MODE_SLOT, createModePane(mode));
        inventory.setItem(INJECTOR_MENU_RARITY_SLOT, createRarityPane());
        inventory.setItem(INJECTOR_MENU_ENABLE_SLOT, createEnabledPane());
        inventory.setItem(INJECTOR_MENU_NOTIFY_SLOT, createNotifyPane());
        inventory.setItem(INJECTOR_MENU_CLEAR_SLOT, createClearPane());
        player.openInventory(inventory);
    }

    void onMenuClick(@NotNull InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof InjectorMenuHolder holder)) {
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
        List<InjectorEntry> entries = entriesForMode(holder.mode());
        int totalPages = Math.max(1, (entries.size() + INJECTOR_MENU_PAGE_SIZE - 1) / INJECTOR_MENU_PAGE_SIZE);

        if (rawSlot == INJECTOR_MENU_PREV_SLOT) {
            if (holder.page() > 0) {
                openMenu(player, holder.mode(), holder.page() - 1);
            }
            return;
        }
        if (rawSlot == INJECTOR_MENU_NEXT_SLOT) {
            if (holder.page() + 1 < totalPages) {
                openMenu(player, holder.mode(), holder.page() + 1);
            }
            return;
        }
        if (rawSlot == INJECTOR_MENU_MODE_SLOT) {
            InjectorMenuMode nextMode = holder.mode() == InjectorMenuMode.CONFIGURED ? InjectorMenuMode.ALL : InjectorMenuMode.CONFIGURED;
            openMenu(player, nextMode, 0);
            return;
        }
        if (rawSlot == INJECTOR_MENU_RARITY_SLOT) {
            plugin.openInjectorBookRarityMenu(player, 0);
            return;
        }
        if (rawSlot == INJECTOR_MENU_ENABLE_SLOT) {
            plugin.setStructureInjectorEnabled(!plugin.isStructureInjectorEnabled());
            plugin.saveStructureInjectorSettings();
            openMenu(player, holder.mode(), holder.page());
            return;
        }
        if (rawSlot == INJECTOR_MENU_NOTIFY_SLOT) {
            plugin.setStructureInjectNotifyOnAdd(!plugin.isStructureInjectNotifyOnAdd());
            plugin.saveStructureInjectorSettings();
            openMenu(player, holder.mode(), holder.page());
            return;
        }
        if (rawSlot == INJECTOR_MENU_CLEAR_SLOT) {
            plugin.structureInjectChances().clear();
            plugin.structureInjectLootModes().clear();
            plugin.structureInjectMysteryStates().clear();
            plugin.setTrialVaultNormalChance(0.0D);
            plugin.setTrialVaultOminousChance(0.0D);
            plugin.setTrialVaultNormalLootMode(InjectorLootMode.ALL);
            plugin.setTrialVaultOminousLootMode(InjectorLootMode.ALL);
            plugin.setTrialVaultNormalMysteryState(InjectorMysteryState.ALL);
            plugin.setTrialVaultOminousMysteryState(InjectorMysteryState.ALL);
            plugin.setTrialVaultInjectorEnabled(false);
            plugin.saveStructureInjectorSettings();
            openMenu(player, holder.mode(), 0);
            return;
        }

        if (rawSlot < 0 || rawSlot >= INJECTOR_MENU_PAGE_SIZE) {
            return;
        }

        int absoluteIndex = holder.page() * INJECTOR_MENU_PAGE_SIZE + rawSlot;
        if (absoluteIndex < 0 || absoluteIndex >= entries.size()) {
            return;
        }

        InjectorEntry entry = entries.get(absoluteIndex);
        if (entry.vaultType != null) {
            handleVaultEntryClick(player, holder, event, entry);
            return;
        }

        handleStructureEntryClick(player, holder, event, entry);
    }

    void onMenuDrag(@NotNull InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof InjectorMenuHolder)) {
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

    private void handleVaultEntryClick(@NotNull Player player,
                                       @NotNull InjectorMenuHolder holder,
                                       @NotNull InventoryClickEvent event,
                                       @NotNull InjectorEntry entry) {
        boolean ominous = entry.vaultType == VaultEntryType.OMINOUS;
        double current = ominous ? plugin.getTrialVaultOminousChance() : plugin.getTrialVaultNormalChance();
        InjectorLootMode currentMode = ominous ? plugin.getTrialVaultOminousLootMode() : plugin.getTrialVaultNormalLootMode();
        InjectorMysteryState currentMystery = ominous ? plugin.getTrialVaultOminousMysteryState() : plugin.getTrialVaultNormalMysteryState();

        if (event.getClick() == ClickType.MIDDLE) {
            current = 0.0D;
            currentMode = InjectorLootMode.ALL;
            currentMystery = InjectorMysteryState.ALL;
        } else if (event.getClick() == ClickType.DROP
                || event.getClick() == ClickType.CONTROL_DROP
                || event.getClick() == ClickType.SWAP_OFFHAND) {
            if (current <= 0.0D) {
                current = DEFAULT_VAULT_INJECT_CHANCE;
            }
            if (event.getClick() == ClickType.CONTROL_DROP) {
                currentMode = currentMode.nextCurse();
            } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
                currentMystery = currentMystery.next();
            } else {
                currentMode = currentMode.nextType();
            }
        } else if (event.getClick().isLeftClick()) {
            if (current <= 0.0D) {
                current = DEFAULT_VAULT_INJECT_CHANCE;
            } else {
                current = adjustChance(current, true, event.isShiftClick());
            }
        } else if (event.getClick().isRightClick()) {
            if (current <= 0.0D) {
                return;
            }
            current = adjustChance(current, false, event.isShiftClick());
        } else {
            return;
        }

        current = clampChance(current);
        if (ominous) {
            plugin.setTrialVaultOminousChance(current);
            plugin.setTrialVaultOminousLootMode(currentMode);
            plugin.setTrialVaultOminousMysteryState(currentMystery);
        } else {
            plugin.setTrialVaultNormalChance(current);
            plugin.setTrialVaultNormalLootMode(currentMode);
            plugin.setTrialVaultNormalMysteryState(currentMystery);
        }
        plugin.setTrialVaultInjectorEnabled(plugin.getTrialVaultNormalChance() > 0.0D || plugin.getTrialVaultOminousChance() > 0.0D);
        plugin.saveStructureInjectorSettings();
        openMenu(player, holder.mode(), holder.page());
    }

    private void handleStructureEntryClick(@NotNull Player player,
                                           @NotNull InjectorMenuHolder holder,
                                           @NotNull InventoryClickEvent event,
                                           @NotNull InjectorEntry entry) {
        Structure structure = Objects.requireNonNull(entry.structure);
        NamespacedKey key = structure.getKey();
        Double current = plugin.structureInjectChances().get(key);

        if (event.getClick() == ClickType.MIDDLE) {
            plugin.setStructureInjectLootMode(key, InjectorLootMode.ALL);
            plugin.setStructureInjectMysteryState(key, InjectorMysteryState.ALL);
            if (current != null) {
                plugin.structureInjectChances().remove(key);
            }
            plugin.saveStructureInjectorSettings();
            openMenu(player, holder.mode(), holder.page());
            return;
        }

        if (event.getClick() == ClickType.DROP
                || event.getClick() == ClickType.CONTROL_DROP
                || event.getClick() == ClickType.SWAP_OFFHAND) {
            if (current == null) {
                current = plugin.getStructureInjectDefaultChance();
                plugin.structureInjectChances().put(key, clampChance(current));
            }
            InjectorLootMode currentMode = plugin.structureInjectLootMode(key);
            InjectorMysteryState currentMystery = plugin.structureInjectMysteryState(key);
            if (event.getClick() == ClickType.CONTROL_DROP) {
                plugin.setStructureInjectLootMode(key, currentMode.nextCurse());
            } else if (event.getClick() == ClickType.SWAP_OFFHAND) {
                plugin.setStructureInjectMysteryState(key, currentMystery.next());
            } else {
                plugin.setStructureInjectLootMode(key, currentMode.nextType());
            }
            plugin.saveStructureInjectorSettings();
            openMenu(player, holder.mode(), holder.page());
            return;
        }

        if (event.getClick().isLeftClick()) {
            if (current == null) {
                current = plugin.getStructureInjectDefaultChance();
            }
            current = adjustChance(current, true, event.isShiftClick());
            plugin.structureInjectChances().put(key, clampChance(current));
            plugin.saveStructureInjectorSettings();
            openMenu(player, holder.mode(), holder.page());
            return;
        }

        if (event.getClick().isRightClick()) {
            if (current == null) {
                return;
            }
            current = adjustChance(current, false, event.isShiftClick());
            plugin.structureInjectChances().put(key, clampChance(current));
            plugin.saveStructureInjectorSettings();
            openMenu(player, holder.mode(), holder.page());
        }
    }

    private @NotNull List<InjectorEntry> entriesForMode(@NotNull InjectorMenuMode mode) {
        List<InjectorEntry> entries = new ArrayList<>();
        if (mode == InjectorMenuMode.ALL || plugin.getTrialVaultNormalChance() > 0.0D) {
            entries.add(InjectorEntry.vault(VaultEntryType.NORMAL));
        }
        if (mode == InjectorMenuMode.ALL || plugin.getTrialVaultOminousChance() > 0.0D) {
            entries.add(InjectorEntry.vault(VaultEntryType.OMINOUS));
        }

        List<Structure> structures = new ArrayList<>();
        if (mode == InjectorMenuMode.ALL) {
            structures.addAll(plugin.allStructures());
        } else {
            for (NamespacedKey key : plugin.structureInjectChances().keySet()) {
                Structure structure = Registry.STRUCTURE.get(key);
                if (structure != null) {
                    structures.add(structure);
                }
            }
            structures.sort((left, right) -> left.getKey().toString().compareToIgnoreCase(right.getKey().toString()));
        }

        for (Structure structure : structures) {
            entries.add(InjectorEntry.structure(structure));
        }
        return entries;
    }

    private @NotNull ItemStack createEntryItem(@NotNull InjectorEntry entry, @NotNull InjectorMenuMode mode) {
        if (entry.vaultType != null) {
            boolean ominous = entry.vaultType == VaultEntryType.OMINOUS;
            double chance = ominous ? plugin.getTrialVaultOminousChance() : plugin.getTrialVaultNormalChance();
            InjectorLootMode vaultMode = ominous ? plugin.getTrialVaultOminousLootMode() : plugin.getTrialVaultNormalLootMode();
            InjectorMysteryState vaultMystery = ominous ? plugin.getTrialVaultOminousMysteryState() : plugin.getTrialVaultNormalMysteryState();
            boolean configured = chance > 0.0D;
            ItemStack item = new ItemStack(ominous ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY);
            ItemMeta meta = item.getItemMeta();
            if (meta == null) {
                return item;
            }
            meta.displayName(Component.text(
                    ominous ? "minecraft:trial_chambers_ominous_vault" : "minecraft:trial_chambers_vault",
                    configured ? NamedTextColor.GREEN : NamedTextColor.GRAY
            ));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(configured ? "Configured chance: " + StructureInjectorUtil.formatPercent(chance) : "Not configured", configured ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY));
            lore.add(Component.text("Loot type: " + vaultMode.type().id(), vaultMode.type() == InjectorLootMode.LootType.ALL ? NamedTextColor.GRAY : NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("Curse state: " + vaultMode.curseState().id(), vaultMode.curseState() == InjectorLootMode.CurseState.ALL ? NamedTextColor.GRAY : NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("Mystery state: " + vaultMystery.id(), vaultMystery == InjectorMysteryState.ALL ? NamedTextColor.GRAY : NamedTextColor.LIGHT_PURPLE));
            lore.add(Component.text("Acts like structure injector entry.", NamedTextColor.DARK_GRAY));
            lore.add(Component.empty());
            lore.add(Component.text("Left: +1% (+0.1% below 1%)", NamedTextColor.GRAY));
            lore.add(Component.text("Shift+Left: +5% (+0.5% below 5%)", NamedTextColor.GRAY));
            lore.add(Component.text("Right: -1% (-0.1% below 1%, min 0.1%)", NamedTextColor.GRAY));
            lore.add(Component.text("Middle: remove entry (set 0%)", NamedTextColor.GRAY));
            lore.add(Component.text("Drop (Q): cycle loot type", NamedTextColor.GRAY));
            lore.add(Component.text("Ctrl+Q: cycle curse state", NamedTextColor.GRAY));
            lore.add(Component.text("F: cycle mystery state", NamedTextColor.GRAY));
            meta.lore(lore);
            item.setItemMeta(meta);
            return item;
        }

        Structure structure = Objects.requireNonNull(entry.structure);
        Double chance = plugin.structureInjectChances().get(structure.getKey());
        boolean configured = chance != null;
        InjectorLootMode lootMode = plugin.structureInjectLootMode(structure.getKey());
        InjectorMysteryState mysteryState = plugin.structureInjectMysteryState(structure.getKey());
        ItemStack item = new ItemStack(configured ? Material.CHEST : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(Component.text(structure.getKey().toString(), configured ? NamedTextColor.GREEN : NamedTextColor.GRAY));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(configured ? "Configured chance: " + StructureInjectorUtil.formatPercent(chance) : "Not configured", configured ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY));
        lore.add(Component.text("Loot type: " + lootMode.type().id(), lootMode.type() == InjectorLootMode.LootType.ALL ? NamedTextColor.GRAY : NamedTextColor.LIGHT_PURPLE));
        lore.add(Component.text("Curse state: " + lootMode.curseState().id(), lootMode.curseState() == InjectorLootMode.CurseState.ALL ? NamedTextColor.GRAY : NamedTextColor.LIGHT_PURPLE));
        lore.add(Component.text("Mystery state: " + mysteryState.id(), mysteryState == InjectorMysteryState.ALL ? NamedTextColor.GRAY : NamedTextColor.LIGHT_PURPLE));
        lore.add(Component.text("Combined mode: " + lootMode.id() + " + " + mysteryState.id(), NamedTextColor.DARK_GRAY));
        lore.add(Component.text("Mode: " + (mode == InjectorMenuMode.CONFIGURED ? "Configured only" : "All structures"), NamedTextColor.DARK_GRAY));
        lore.add(Component.empty());
        lore.add(Component.text("Left: +1% (+0.1% below 1%)", NamedTextColor.GRAY));
        lore.add(Component.text("Shift+Left: +5% (+0.5% below 5%)", NamedTextColor.GRAY));
        lore.add(Component.text("Right: -1% (-0.1% below 1%, min 0.1%)", NamedTextColor.GRAY));
        lore.add(Component.text("Middle: remove structure", NamedTextColor.GRAY));
        lore.add(Component.text("Drop (Q): cycle loot type", NamedTextColor.GRAY));
        lore.add(Component.text("Ctrl+Q: cycle curse state", NamedTextColor.GRAY));
        lore.add(Component.text("F: cycle mystery state", NamedTextColor.GRAY));
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

    private @NotNull ItemStack createModePane(@NotNull InjectorMenuMode mode) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(
                    mode == InjectorMenuMode.CONFIGURED ? "Switch: All Structures" : "Switch: Configured Only",
                    NamedTextColor.AQUA
            ));
            meta.lore(List.of(Component.text("Click to cycle view mode.", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createEnabledPane() {
        boolean enabled = plugin.isStructureInjectorEnabled();
        ItemStack item = new ItemStack(enabled ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(
                    "Injector " + (enabled ? "Enabled" : "Disabled"),
                    enabled ? NamedTextColor.GREEN : NamedTextColor.RED
            ));
            meta.lore(List.of(Component.text("Click to toggle global injector.", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createRarityPane() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Enchantment Rarity Editor", NamedTextColor.AQUA));
            meta.lore(List.of(
                    Component.text("Click to edit weighted rarity for each enchantment level.", NamedTextColor.GRAY),
                    Component.text("Higher weight = more common in injector books.", NamedTextColor.DARK_GRAY)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createNotifyPane() {
        boolean enabled = plugin.isStructureInjectNotifyOnAdd();
        ItemStack item = new ItemStack(enabled ? Material.BELL : Material.NOTE_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(
                    "Chest Add Message " + (enabled ? "Enabled" : "Disabled"),
                    enabled ? NamedTextColor.GREEN : NamedTextColor.RED
            ));
            meta.lore(List.of(Component.text("Click to toggle action-bar message on chest injection.", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack createClearPane() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Clear All Structures", NamedTextColor.RED));
            meta.lore(List.of(Component.text("Click to remove all configured structures.", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static double clampChance(double chance) {
        if (Double.isNaN(chance) || Double.isInfinite(chance)) {
            return 0.0D;
        }
        return Math.max(0.0D, Math.min(100.0D, chance));
    }

    private static double adjustChance(double current, boolean increase, boolean shift) {
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

    private static double roundToTenths(double value) {
        return Math.round(value * 10.0D) / 10.0D;
    }
}

