package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class EnchantToggleMenuService {
    private static final int TOGGLE_MENU_SIZE = 54;
    private static final int TOGGLE_MENU_PAGE_SIZE = 45;
    private static final int TOGGLE_MENU_PREV_SLOT = 45;
    private static final int TOGGLE_MENU_NEXT_SLOT = 53;

    private final ForbiddenEnchantsPlugin plugin;

    EnchantToggleMenuService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    void openMenu(@NotNull Player player, int page) {
        int totalPages = getPageCount();
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        EnchantToggleMenuHolder holder = new EnchantToggleMenuHolder(safePage);
        Inventory inventory = Bukkit.createInventory(
                holder,
                TOGGLE_MENU_SIZE,
                Component.text(plugin.message("menu.toggles.title", "Enchant Toggles"), NamedTextColor.AQUA)
                        .append(Component.text(" [" + (safePage + 1) + "/" + totalPages + "]", NamedTextColor.GRAY))
        );
        holder.attach(inventory);

        List<EnchantType> visible = plugin.activeEnchantTypes();
        int start = safePage * TOGGLE_MENU_PAGE_SIZE;
        int end = Math.min(visible.size(), start + TOGGLE_MENU_PAGE_SIZE);
        int slot = 0;
        for (int i = start; i < end; i++) {
            inventory.setItem(slot++, createToggleItem(visible.get(i)));
        }

        inventory.setItem(TOGGLE_MENU_PREV_SLOT, plugin.createMenuNavPane(safePage > 0, true));
        inventory.setItem(TOGGLE_MENU_NEXT_SLOT, plugin.createMenuNavPane(safePage + 1 < totalPages, false));
        player.openInventory(inventory);
    }

    void onMenuClick(@NotNull InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EnchantToggleMenuHolder holder)) {
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
        int totalPages = getPageCount();
        if (rawSlot == TOGGLE_MENU_PREV_SLOT) {
            if (holder.page() > 0) {
                openMenu(player, holder.page() - 1);
            }
            return;
        }
        if (rawSlot == TOGGLE_MENU_NEXT_SLOT) {
            if (holder.page() + 1 < totalPages) {
                openMenu(player, holder.page() + 1);
            }
            return;
        }
        if (rawSlot < 0 || rawSlot >= TOGGLE_MENU_PAGE_SIZE) {
            return;
        }

        int absolute = holder.page() * TOGGLE_MENU_PAGE_SIZE + rawSlot;
        List<EnchantType> types = plugin.activeEnchantTypes();
        if (absolute < 0 || absolute >= types.size()) {
            return;
        }
        EnchantType type = types.get(absolute);

        if (event.getClick() == org.bukkit.event.inventory.ClickType.MIDDLE) {
            boolean next = !(plugin.isEnchantUseEnabled(type) && plugin.isEnchantSpawnEnabled(type));
            plugin.setEnchantUseEnabled(type, next);
            plugin.setEnchantSpawnEnabled(type, next);
        } else if (event.getClick().isLeftClick()) {
            plugin.setEnchantUseEnabled(type, !plugin.isEnchantUseEnabled(type));
        } else if (event.getClick().isRightClick()) {
            plugin.setEnchantSpawnEnabled(type, !plugin.isEnchantSpawnEnabled(type));
        } else {
            return;
        }

        plugin.saveEnchantToggleSettings();
        openMenu(player, holder.page());
    }

    void onMenuDrag(@NotNull InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!(top.getHolder() instanceof EnchantToggleMenuHolder)) {
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

    private int getPageCount() {
        return Math.max(1, (plugin.activeEnchantTypes().size() + TOGGLE_MENU_PAGE_SIZE - 1) / TOGGLE_MENU_PAGE_SIZE);
    }

    private @NotNull ItemStack createToggleItem(@NotNull EnchantType type) {
        boolean useEnabled = plugin.isEnchantUseEnabled(type);
        boolean spawnEnabled = plugin.isEnchantSpawnEnabled(type);
        boolean fullyEnabled = useEnabled && spawnEnabled;

        ItemStack item = new ItemStack(useEnabled ? toggleIcon(type) : Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(Component.text(type.displayName, fullyEnabled ? NamedTextColor.GREEN : NamedTextColor.RED));
        List<Component> lore = new ArrayList<>();
        String enabled = msg("menu.toggles.state.enabled", "Enabled");
        String disabled = msg("menu.toggles.state.disabled", "Disabled");
        lore.add(Component.text(
                msg("menu.toggles.entry.use_line", "Use: {state}", Map.of("state", useEnabled ? enabled : disabled)),
                useEnabled ? NamedTextColor.GREEN : NamedTextColor.RED
        ));
        lore.add(Component.text(
                msg("menu.toggles.entry.spawn_line", "Loot spawn: {state}", Map.of("state", spawnEnabled ? enabled : disabled)),
                spawnEnabled ? NamedTextColor.GREEN : NamedTextColor.RED
        ));
        lore.add(Component.text(
                msg("menu.toggles.entry.slot_line", "Slot: {slot}", Map.of("slot", EnchantMaterialCatalog.requiredMaterialCategory(type))),
                NamedTextColor.DARK_GRAY
        ));
        lore.add(Component.empty());
        lore.add(Component.text(msg("menu.toggles.entry.left_click", "Left-click: Toggle use"), NamedTextColor.GRAY));
        lore.add(Component.text(msg("menu.toggles.entry.right_click", "Right-click: Toggle chest/vault spawn"), NamedTextColor.GRAY));
        lore.add(Component.text(msg("menu.toggles.entry.middle_click", "Middle-click: Toggle both"), NamedTextColor.GRAY));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private @NotNull Material toggleIcon(@NotNull EnchantType type) {
        if (type.isAnvilOnlyUtilityBook()) {
            return Material.WRITABLE_BOOK;
        }
        return switch (type.slot) {
            case HELMET -> Material.NETHERITE_HELMET;
            case CHESTPLATE, ELYTRA, ARMOR -> Material.NETHERITE_CHESTPLATE;
            case LEGGINGS -> Material.NETHERITE_LEGGINGS;
            case BOOTS -> Material.NETHERITE_BOOTS;
            case COMPASS -> Material.COMPASS;
            case SWORD -> Material.NETHERITE_SWORD;
            case RANGED -> Material.CROSSBOW;
            case TRIDENT, SPEAR -> Material.TRIDENT;
            case HOE -> Material.NETHERITE_HOE;
            case AXE -> Material.NETHERITE_AXE;
            case MACE -> Material.MACE;
            case BRUSH -> Material.BRUSH;
            case NAMETAG -> Material.NAME_TAG;
            case LEAD -> Material.LEAD;
            case SHIELD -> Material.SHIELD;
            case TOTEM -> Material.TOTEM_OF_UNDYING;
            case POTION -> Material.POTION;
            case ROD -> Material.BLAZE_ROD;
        };
    }

    private @NotNull String msg(@NotNull String key, @NotNull String fallback) {
        return plugin.message(key, fallback);
    }

    private @NotNull String msg(@NotNull String key,
                                @NotNull String fallback,
                                @NotNull Map<String, String> placeholders) {
        return plugin.message(key, fallback, placeholders);
    }
}

