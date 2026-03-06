package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

final class EnchantBookFactoryService {
    private static final int BOOK_MODEL_BASE = 930000;
    private final ForbiddenEnchantsPlugin plugin;

    EnchantBookFactoryService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    @Nullable BookSpec readBookSpec(@Nullable ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return null;
        }

        if (stack.getType() != Material.BOOK && stack.getType() != Material.ENCHANTED_BOOK) {
            return null;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }

        Integer modelData = readModelData(meta);
        if (modelData != null) {
            BookSpec fromModelData = decodeBookModelData(modelData);
            if (fromModelData != null) {
                return fromModelData;
            }
        }

        NamespacedKey bookEnchantKey = plugin.bookEnchantKey();
        NamespacedKey bookLevelKey = plugin.bookLevelKey();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String enchantId = pdc.get(bookEnchantKey, PersistentDataType.STRING);
        Integer level = pdc.get(bookLevelKey, PersistentDataType.INTEGER);
        if (enchantId != null && level != null) {
            EnchantType type = EnchantType.fromArg(enchantId);
            if (type != null && level >= 1 && level <= type.maxLevel) {
                return new BookSpec(type, level);
            }
        }

        Component displayName = meta.displayName();
        if (displayName != null) {
            String plain = PlainTextComponentSerializer.plainText().serialize(displayName).trim().toLowerCase(Locale.ROOT);
            for (EnchantType type : EnchantType.values()) {
                for (int levelCandidate = 1; levelCandidate <= type.maxLevel; levelCandidate++) {
                    String expected = (type.displayName + " " + RomanNumeralUtil.toRoman(levelCandidate)).toLowerCase(Locale.ROOT);
                    if (plain.equals(expected) || plain.equals("forbidden " + expected)) {
                        return new BookSpec(type, levelCandidate);
                    }
                }
            }
        }

        return null;
    }

    @Nullable BookSpec decodeBookModelData(int modelData) {
        if (modelData <= BOOK_MODEL_BASE) {
            return null;
        }

        int encoded = modelData - BOOK_MODEL_BASE;
        int modelTypeIndex = encoded / 10;
        int level = encoded % 10;

        EnchantType type = EnchantType.fromModelTypeIndex(modelTypeIndex);
        if (type == null) {
            return null;
        }

        if (level < 1 || level > type.maxLevel) {
            return null;
        }

        return new BookSpec(type, level);
    }

    @Nullable Integer readModelData(@NotNull ItemMeta meta) {
        if (meta.hasCustomModelData()) {
            return meta.getCustomModelData();
        }

        CustomModelDataComponent component = meta.getCustomModelDataComponent();
        if (component == null || component.getFloats().isEmpty()) {
            return null;
        }

        float value = component.getFloats().get(0);
        int rounded = Math.round(value);
        if (Math.abs(value - rounded) > 0.001F) {
            return null;
        }

        return rounded;
    }

    int encodeBookModelData(@NotNull EnchantType type, int level) {
        return BOOK_MODEL_BASE + (type.modelTypeIndex() * 10) + level;
    }

    @NotNull ItemStack createBook(@NotNull EnchantType type, int level) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Forbidden " + type.displayName + " " + RomanNumeralUtil.toRoman(level), type.color));
        List<Component> lore = new java.util.ArrayList<>();
        lore.addAll(LoreWrapUtil.wrap(type.slotDescription(), NamedTextColor.GRAY, 45));
        lore.addAll(LoreWrapUtil.wrap(type.effectDescription(level), NamedTextColor.DARK_GRAY, 45));
        meta.lore(lore);

        meta.setCustomModelData(encodeBookModelData(type, level));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(plugin.bookEnchantKey(), PersistentDataType.STRING, type.arg);
        pdc.set(plugin.bookLevelKey(), PersistentDataType.INTEGER, level);

        item.setItemMeta(meta);
        return item;
    }

    @Nullable ItemStack createEnchantedItem(@NotNull EnchantType type, int level, @NotNull Material material) {
        ItemStack item = new ItemStack(material);
        if (!plugin.isMaterialValidForEnchant(material, type)) {
            return null;
        }
        if (!plugin.applyBookEnchant(item, type, level)) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null && !meta.hasCustomModelData()) {
            meta.setCustomModelData(encodeBookModelData(type, level));
            item.setItemMeta(meta);
        }
        return item;
    }
}

