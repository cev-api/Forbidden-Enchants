package dev.cevapi.forbiddenenchants;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class FeCatalogService {
    private final ForbiddenEnchantsPlugin plugin;
    private final List<List<ItemStack>> menuPages = new ArrayList<>();
    private final Map<UUID, Integer> lastMenuPageByPlayer = new HashMap<>();

    FeCatalogService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    void clear() {
        menuPages.clear();
        lastMenuPageByPlayer.clear();
    }

    void ensureMenuPagesBuilt() {
        if (!menuPages.isEmpty()) {
            return;
        }
        rebuildMenuItems();
    }

    @NotNull List<List<ItemStack>> menuPages() {
        return menuPages;
    }

    void rememberLastMenuPage(@NotNull UUID playerId, int page) {
        lastMenuPageByPlayer.put(playerId, page);
    }

    int getLastMenuPage(@NotNull UUID playerId) {
        return lastMenuPageByPlayer.getOrDefault(playerId, 0);
    }

    private void rebuildMenuItems() {
        menuPages.clear();
        for (EnchantType type : plugin.activeEnchantTypes()) {
            List<Material> guiMaterials = guiMaterialsForType(type);
            List<ItemStack> pageItems = new ArrayList<>();
            pageItems.add(plugin.createMysteryBook(type.slot));
            for (Material material : guiMaterials) {
                ItemStack mysteryItem = plugin.createMysteryItem(material);
                if (mysteryItem != null) {
                    pageItems.add(mysteryItem);
                }
            }

            for (int level = 1; level <= type.maxLevel; level++) {
                pageItems.add(plugin.createBook(type, level));
            }

            for (Material material : guiMaterials) {
                for (int level = 1; level <= type.maxLevel; level++) {
                    ItemStack item = plugin.createEnchantedItem(type, level, material);
                    if (item != null) {
                        pageItems.add(item);
                    }
                }
            }
            menuPages.add(pageItems);
        }
    }

    private @NotNull List<Material> guiMaterialsForType(@NotNull EnchantType type) {
        List<Material> materials = new ArrayList<>(EnchantMaterialCatalog.materialsForSlot(type.slot));
        if (type == EnchantType.WITHERING_STRIKE) {
            materials.clear();
            materials.add(Material.TRIDENT);
        }
        if (type == EnchantType.STAFF_OF_THE_EVOKER) {
            materials.remove(Material.TRIDENT);
        }
        if (type == EnchantType.MINERS_INTUITION) {
            Material copperHelmet = EnchantMaterialCatalog.materialIfPresent("COPPER_HELMET");
            if (copperHelmet != null) {
                materials.remove(copperHelmet);
                materials.add(0, copperHelmet);
            }
        }
        return materials;
    }
}

