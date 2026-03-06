package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

final class MysteryItemService {
    private final Supplier<EnchantBookFactoryService> enchantBookFactoryServiceSupplier;
    private final Supplier<EnchantRuleCoreService> enchantRuleCoreServiceSupplier;
    private final Supplier<EnchantStateService> enchantStateServiceSupplier;
    private final Supplier<ItemClassificationService> itemClassificationServiceSupplier;
    private final Supplier<NamespacedKey> mysteryKeySupplier;

    MysteryItemService(@NotNull Supplier<EnchantBookFactoryService> enchantBookFactoryServiceSupplier,
                       @NotNull Supplier<EnchantRuleCoreService> enchantRuleCoreServiceSupplier,
                       @NotNull Supplier<EnchantStateService> enchantStateServiceSupplier,
                       @NotNull Supplier<ItemClassificationService> itemClassificationServiceSupplier,
                       @NotNull Supplier<NamespacedKey> mysteryKeySupplier) {
        this.enchantBookFactoryServiceSupplier = enchantBookFactoryServiceSupplier;
        this.enchantRuleCoreServiceSupplier = enchantRuleCoreServiceSupplier;
        this.enchantStateServiceSupplier = enchantStateServiceSupplier;
        this.itemClassificationServiceSupplier = itemClassificationServiceSupplier;
        this.mysteryKeySupplier = mysteryKeySupplier;
    }

    @NotNull ItemStack createMysteryBook(@NotNull ArmorSlot slot) {
        EnchantType type = pickRandomEnchantForSlot(slot);
        int level = ThreadLocalRandom.current().nextInt(1, type.maxLevel + 1);
        ItemStack book = enchantBookFactoryServiceSupplier.get().createBook(type, level);

        ItemMeta meta = book.getItemMeta();
        meta.getPersistentDataContainer().set(mysteryKeySupplier.get(), PersistentDataType.INTEGER, 1);
        meta.displayName(
                Component.text("Forbidden ", NamedTextColor.DARK_PURPLE)
                        .append(Component.text("????????", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.OBFUSCATED))
                        .append(Component.text(" (" + SlotParsingUtil.slotName(slot) + ")", NamedTextColor.GRAY))
        );
        meta.lore(List.of(
                Component.text("Mystery enchant for " + EnchantMaterialCatalog.requiredMaterialCategory(slot) + ".", NamedTextColor.GRAY),
                Component.text("Effect and level are hidden until use.", NamedTextColor.DARK_GRAY)
        ));
        book.setItemMeta(meta);
        return book;
    }

    @Nullable ItemStack createMysteryItem(@NotNull Material material) {
        ArmorSlot slot = SlotParsingUtil.resolveSlotForMaterial(material);
        if (slot == null) {
            return null;
        }

        List<EnchantType> options = enchantsForSlot(slot);
        if (options.isEmpty()) {
            return null;
        }

        List<EnchantType> eligible = new ArrayList<>();
        for (EnchantType option : options) {
            if (itemClassificationServiceSupplier.get().isMaterialValidForEnchant(material, option)
                    && enchantStateServiceSupplier.get().isEnchantUseEnabled(option)) {
                eligible.add(option);
            }
        }
        if (eligible.isEmpty()) {
            return null;
        }

        EnchantType type = eligible.get(ThreadLocalRandom.current().nextInt(eligible.size()));
        int level = ThreadLocalRandom.current().nextInt(1, type.maxLevel + 1);
        ItemStack item = enchantBookFactoryServiceSupplier.get().createEnchantedItem(type, level, material);
        if (item == null) {
            return null;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(mysteryKeySupplier.get(), PersistentDataType.INTEGER, 1);
            meta.displayName(
                    Component.text("Forbidden ", NamedTextColor.DARK_PURPLE)
                            .append(Component.text("????????", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.OBFUSCATED))
                            .append(Component.text(" (" + DisplayNameUtil.toDisplayName(material) + ")", NamedTextColor.GRAY))
            );
            meta.lore(List.of(
                    Component.text("Mystery enchant bound to this item.", NamedTextColor.GRAY),
                    Component.text("You know the item type, not the enchant.", NamedTextColor.DARK_GRAY)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    void revealMysteryItemIfNeeded(@Nullable ItemStack item, @Nullable Player owner, @Nullable EquipmentSlot slot) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        Integer mystery = meta.getPersistentDataContainer().get(mysteryKeySupplier.get(), PersistentDataType.INTEGER);
        if (mystery == null || mystery <= 0) {
            return;
        }

        EnchantType foundType = null;
        int foundLevel = 0;
        for (EnchantType type : EnchantType.values()) {
            int level = enchantStateServiceSupplier.get().getStoredEnchantLevel(meta, type);
            if (level > 0) {
                foundType = type;
                foundLevel = level;
                break;
            }
        }

        meta.getPersistentDataContainer().remove(mysteryKeySupplier.get());
        if (foundType != null) {
            meta.displayName(Component.text("Forbidden " + foundType.displayName + " " + RomanNumeralUtil.toRoman(foundLevel), foundType.color));
            List<Component> lore = new ArrayList<>();
            lore.addAll(LoreWrapUtil.wrap(foundType.slotDescription(), NamedTextColor.GRAY, 45));
            lore.addAll(LoreWrapUtil.wrap(foundType.effectDescription(foundLevel), NamedTextColor.DARK_GRAY, 45));
            meta.lore(lore);
        }
        enchantRuleCoreServiceSupplier.get().rebuildCustomLore(meta);
        item.setItemMeta(meta);

        if (owner == null || slot == null) {
            return;
        }
        switch (slot) {
            case HEAD -> owner.getInventory().setHelmet(item);
            case CHEST -> owner.getInventory().setChestplate(item);
            case LEGS -> owner.getInventory().setLeggings(item);
            case FEET -> owner.getInventory().setBoots(item);
            case HAND -> owner.getInventory().setItemInMainHand(item);
            case OFF_HAND -> owner.getInventory().setItemInOffHand(item);
            default -> {
            }
        }
    }

    private @NotNull EnchantType pickRandomEnchantForSlot(@NotNull ArmorSlot slot) {
        List<EnchantType> options = new ArrayList<>();
        for (EnchantType type : enchantsForSlot(slot)) {
            if (enchantStateServiceSupplier.get().isEnchantUseEnabled(type)) {
                options.add(type);
            }
        }
        if (options.isEmpty()) {
            options = enchantsForSlot(slot);
        }
        if (options.isEmpty()) {
            throw new IllegalStateException("No enchant types available for slot " + slot);
        }
        return options.get(ThreadLocalRandom.current().nextInt(options.size()));
    }

    private @NotNull List<EnchantType> enchantsForSlot(@NotNull ArmorSlot slot) {
        List<EnchantType> matches = new ArrayList<>();
        for (EnchantType type : EnchantType.values()) {
            if (type.slot == slot && !enchantStateServiceSupplier.get().isRetiredEnchant(type)) {
                matches.add(type);
            }
        }
        return matches;
    }
}

