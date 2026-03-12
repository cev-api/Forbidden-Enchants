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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class FeMenuService {
    private static final int FE_MENU_SIZE = 54;
    private static final int FE_MENU_PAGE_SIZE = 45;
    private static final int FE_MENU_PREV_SLOT = 45;
    private static final int FE_MENU_ARMORS_SLOT = 46;
    private static final int FE_MENU_WEAPONS_SLOT = 47;
    private static final int FE_MENU_TOTEMS_SLOT = 48;
    private static final int FE_MENU_BOOKS_SLOT = 49;
    private static final int FE_MENU_POTIONS_SLOT = 50;
    private static final int FE_MENU_OTHER_SLOT = 51;
    private static final int FE_MENU_RETURN_SLOT = 52;
    private static final int FE_MENU_NEXT_SLOT = 53;

    private final ForbiddenEnchantsPlugin plugin;

    FeMenuService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    void openMenu(@NotNull Player player, int page) {
        openMenu(player, FeMenuCategory.ALL, page);
    }

    private void openMenu(@NotNull Player player, @NotNull FeMenuCategory category, int page) {
        plugin.ensureMenuPagesBuilt();

        List<EnchantType> visible = typesForCategory(category);
        int totalPages = Math.max(1, visible.size());
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        if (category == FeMenuCategory.ALL) {
            plugin.rememberLastMenuPage(player.getUniqueId(), safePage);
        }

        FeMenuHolder holder = new FeMenuHolder(safePage, category);
        EnchantType pageType = visible.isEmpty() ? null : visible.get(Math.min(safePage, visible.size() - 1));
        String title = pageType == null
                ? "Forbidden " + categoryLabel(category)
                : "Forbidden " + pageType.displayName;
        Inventory inventory = Bukkit.createInventory(
                holder,
                FE_MENU_SIZE,
                Component.text(title + " ", pageType == null ? NamedTextColor.WHITE : pageType.color)
                        .append(Component.text("(" + (safePage + 1) + "/" + totalPages + ")", NamedTextColor.WHITE))
        );
        holder.attach(inventory);

        if (pageType != null) {
            List<ItemStack> pageItems = pageItemsForType(pageType);
            for (int slot = 0; slot < Math.min(FE_MENU_PAGE_SIZE, pageItems.size()); slot++) {
                inventory.setItem(slot, plugin.toMenuDisplayItem(pageItems.get(slot)));
            }
        }

        inventory.setItem(FE_MENU_PREV_SLOT, plugin.createMenuNavPane(totalPages > 1, true));
        inventory.setItem(FE_MENU_NEXT_SLOT, plugin.createMenuNavPane(totalPages > 1, false));
        inventory.setItem(FE_MENU_ARMORS_SLOT, categoryButton(FeMenuCategory.ARMORS, category));
        inventory.setItem(FE_MENU_WEAPONS_SLOT, categoryButton(FeMenuCategory.WEAPONS, category));
        inventory.setItem(FE_MENU_TOTEMS_SLOT, categoryButton(FeMenuCategory.TOTEMS, category));
        inventory.setItem(FE_MENU_BOOKS_SLOT, categoryButton(FeMenuCategory.BOOKS, category));
        inventory.setItem(FE_MENU_POTIONS_SLOT, categoryButton(FeMenuCategory.POTIONS, category));
        inventory.setItem(FE_MENU_OTHER_SLOT, categoryButton(FeMenuCategory.OTHER, category));
        inventory.setItem(FE_MENU_RETURN_SLOT, returnButton());
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
        FeMenuCategory category = holder.category();
        List<EnchantType> visible = typesForCategory(category);
        int totalPages = Math.max(1, visible.size());
        int pageStep = event.isRightClick() ? 5 : 1;

        if (rawSlot == FE_MENU_RETURN_SLOT) {
            openMenu(player, FeMenuCategory.ALL, plugin.getLastMenuPage(player.getUniqueId()));
            return;
        }

        FeMenuCategory chosenCategory = categoryForSlot(rawSlot);
        if (chosenCategory != null) {
            openMenu(player, chosenCategory, 0);
            return;
        }

        if (rawSlot == FE_MENU_PREV_SLOT) {
            if (totalPages > 1) {
                int target = (holder.page() - pageStep) % totalPages;
                if (target < 0) {
                    target += totalPages;
                }
                openMenu(player, category, target);
            }
            return;
        }
        if (rawSlot == FE_MENU_NEXT_SLOT) {
            if (totalPages > 1) {
                int target = (holder.page() + pageStep) % totalPages;
                openMenu(player, category, target);
            }
            return;
        }

        if (rawSlot < 0 || rawSlot >= FE_MENU_PAGE_SIZE) {
            return;
        }

        if (holder.page() < 0 || holder.page() >= visible.size()) {
            return;
        }
        List<ItemStack> pageItems = pageItemsForType(visible.get(holder.page()));
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

    private @NotNull List<EnchantType> typesForCategory(@NotNull FeMenuCategory category) {
        List<EnchantType> all = plugin.activeEnchantTypes();
        if (category == FeMenuCategory.ALL) {
            return all;
        }
        List<EnchantType> filtered = new ArrayList<>();
        for (EnchantType type : all) {
            if (matchesCategory(type, category)) {
                filtered.add(type);
            }
        }
        return filtered;
    }

    private boolean matchesCategory(@NotNull EnchantType type, @NotNull FeMenuCategory category) {
        return switch (category) {
            case ALL -> true;
            case ARMORS -> type.slot == ArmorSlot.HELMET
                    || type.slot == ArmorSlot.CHESTPLATE
                    || type.slot == ArmorSlot.ELYTRA
                    || type.slot == ArmorSlot.LEGGINGS
                    || type.slot == ArmorSlot.BOOTS
                    || type.slot == ArmorSlot.ARMOR;
            case WEAPONS -> type.slot == ArmorSlot.SWORD
                    || type.slot == ArmorSlot.RANGED
                    || type.slot == ArmorSlot.TRIDENT
                    || type.slot == ArmorSlot.SPEAR
                    || type.slot == ArmorSlot.AXE
                    || type.slot == ArmorSlot.MACE;
            case TOTEMS -> type.slot == ArmorSlot.TOTEM;
            case BOOKS -> type.isAnvilOnlyUtilityBook();
            case POTIONS -> type.slot == ArmorSlot.POTION;
            case OTHER -> !type.isAnvilOnlyUtilityBook()
                    && type.slot != ArmorSlot.POTION
                    && type.slot != ArmorSlot.TOTEM
                    && type.slot != ArmorSlot.HELMET
                    && type.slot != ArmorSlot.CHESTPLATE
                    && type.slot != ArmorSlot.ELYTRA
                    && type.slot != ArmorSlot.LEGGINGS
                    && type.slot != ArmorSlot.BOOTS
                    && type.slot != ArmorSlot.ARMOR
                    && type.slot != ArmorSlot.SWORD
                    && type.slot != ArmorSlot.RANGED
                    && type.slot != ArmorSlot.TRIDENT
                    && type.slot != ArmorSlot.SPEAR
                    && type.slot != ArmorSlot.AXE
                    && type.slot != ArmorSlot.MACE;
        };
    }

    private @NotNull List<ItemStack> pageItemsForType(@NotNull EnchantType type) {
        List<EnchantType> all = plugin.activeEnchantTypes();
        int index = all.indexOf(type);
        if (index < 0 || index >= plugin.menuPages().size()) {
            return List.of();
        }
        return plugin.menuPages().get(index);
    }

    private @Nullable FeMenuCategory categoryForSlot(int slot) {
        return switch (slot) {
            case FE_MENU_ARMORS_SLOT -> FeMenuCategory.ARMORS;
            case FE_MENU_WEAPONS_SLOT -> FeMenuCategory.WEAPONS;
            case FE_MENU_TOTEMS_SLOT -> FeMenuCategory.TOTEMS;
            case FE_MENU_BOOKS_SLOT -> FeMenuCategory.BOOKS;
            case FE_MENU_POTIONS_SLOT -> FeMenuCategory.POTIONS;
            case FE_MENU_OTHER_SLOT -> FeMenuCategory.OTHER;
            default -> null;
        };
    }

    private @NotNull ItemStack categoryButton(@NotNull FeMenuCategory button, @NotNull FeMenuCategory selected) {
        Material icon = switch (button) {
            case ALL -> Material.BARRIER;
            case ARMORS -> Material.NETHERITE_CHESTPLATE;
            case WEAPONS -> Material.NETHERITE_SWORD;
            case TOTEMS -> Material.TOTEM_OF_UNDYING;
            case BOOKS -> Material.ENCHANTED_BOOK;
            case POTIONS -> Material.POTION;
            case OTHER -> Material.CHEST;
        };
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            boolean active = button == selected;
            NamedTextColor color = active ? NamedTextColor.GREEN : NamedTextColor.GRAY;
            meta.displayName(Component.text(categoryLabel(button), color));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull ItemStack returnButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Return", NamedTextColor.AQUA));
            meta.lore(List.of(Component.text("Back to full enchant list.", NamedTextColor.GRAY)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private @NotNull String categoryLabel(@NotNull FeMenuCategory category) {
        return switch (category) {
            case ALL -> "All";
            case ARMORS -> "Armors";
            case WEAPONS -> "Weapons";
            case TOTEMS -> "Totems";
            case BOOKS -> "Books";
            case POTIONS -> "Potions";
            case OTHER -> "Other";
        };
    }
}

