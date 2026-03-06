package dev.cevapi.forbiddenenchants;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

final class EnchantStateService {
    private final EnchantBookFactoryService enchantBookFactoryService;
    private final EnumMap<EnchantType, NamespacedKey> enchantLevelKeys = new EnumMap<>(EnchantType.class);
    private final EnumMap<EnchantType, Boolean> enchantUseEnabled = new EnumMap<>(EnchantType.class);
    private final EnumMap<EnchantType, Boolean> enchantSpawnEnabled = new EnumMap<>(EnchantType.class);
    private NamespacedKey blindnessLegacyLevelKey;

    EnchantStateService(@NotNull EnchantBookFactoryService enchantBookFactoryService) {
        this.enchantBookFactoryService = enchantBookFactoryService;
    }

    void initializeKeys(@NotNull ForbiddenEnchantsPlugin plugin) {
        blindnessLegacyLevelKey = new NamespacedKey(plugin, "divine_insight_level");
        for (EnchantType type : EnchantType.values()) {
            enchantLevelKeys.put(type, new NamespacedKey(plugin, type.pdcKey));
        }
    }

    void clearToggles() {
        enchantUseEnabled.clear();
        enchantSpawnEnabled.clear();
    }

    int getEnchantLevel(@Nullable ItemStack item, @NotNull EnchantType type) {
        int level = getStoredEnchantLevel(item, type);
        if (level <= 0 || isEnchantUseEnabled(type)) {
            return level;
        }
        return 0;
    }

    int getStoredEnchantLevel(@Nullable ItemStack item, @NotNull EnchantType type) {
        if (item == null || item.getType() == Material.AIR) {
            return 0;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }

        return getStoredEnchantLevel(meta, type);
    }

    int getStoredEnchantLevel(@NotNull ItemMeta meta, @NotNull EnchantType type) {
        Integer level = meta.getPersistentDataContainer().get(enchantLevelKeys.get(type), PersistentDataType.INTEGER);
        if (level != null) {
            return Math.max(0, Math.min(level, type.maxLevel));
        }
        if (type == EnchantType.BLINDNESS && blindnessLegacyLevelKey != null) {
            Integer legacy = meta.getPersistentDataContainer().get(blindnessLegacyLevelKey, PersistentDataType.INTEGER);
            if (legacy != null) {
                return Math.max(0, Math.min(legacy, type.maxLevel));
            }
        }

        Integer modelData = enchantBookFactoryService.readModelData(meta);
        if (modelData != null) {
            BookSpec decoded = enchantBookFactoryService.decodeBookModelData(modelData);
            if (decoded != null && decoded.type() == type) {
                return decoded.level();
            }
        }

        return 0;
    }

    boolean isEnchantUseEnabled(@NotNull EnchantType type) {
        return enchantUseEnabled.getOrDefault(type, true);
    }

    boolean isEnchantSpawnEnabled(@NotNull EnchantType type) {
        return enchantSpawnEnabled.getOrDefault(type, true);
    }

    void setEnchantUseEnabled(@NotNull EnchantType type, boolean enabled) {
        enchantUseEnabled.put(type, enabled);
    }

    void setEnchantSpawnEnabled(@NotNull EnchantType type, boolean enabled) {
        enchantSpawnEnabled.put(type, enabled);
    }

    boolean isRetiredEnchant(@NotNull EnchantType type) {
        return false;
    }

    @NotNull List<EnchantType> activeEnchantTypes() {
        List<EnchantType> active = new ArrayList<>();
        for (EnchantType type : EnchantType.values()) {
            if (!isRetiredEnchant(type)) {
                active.add(type);
            }
        }
        return active;
    }

    @NotNull NamespacedKey enchantLevelKey(@NotNull EnchantType type) {
        return enchantLevelKeys.get(type);
    }
}

