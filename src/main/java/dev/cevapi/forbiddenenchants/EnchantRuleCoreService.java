package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class EnchantRuleCoreService {
    private static final List<Enchantment> MIASMA_INCOMPATIBLE_ENCHANTS = List.of(
            Enchantment.UNBREAKING,
            Enchantment.MENDING,
            Enchantment.EFFICIENCY,
            Enchantment.SHARPNESS,
            Enchantment.SMITE,
            Enchantment.BANE_OF_ARTHROPODS,
            Enchantment.POWER
    );

    private final EnchantStateService enchantStateService;

    EnchantRuleCoreService(@NotNull EnchantStateService enchantStateService) {
        this.enchantStateService = enchantStateService;
    }

    boolean applyBookEnchant(@NotNull ItemStack item, @NotNull EnchantType type, int level) {
        if (enchantStateService.isRetiredEnchant(type) || !enchantStateService.isEnchantUseEnabled(type)) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        if (hasExclusiveConflict(meta, type)) {
            return false;
        }
        if (type == EnchantType.MIASMA && hasMiasmaIncompatibleEnchant(meta)) {
            return false;
        }
        if (type.requiresNoOtherEnchantsOnItem() && hasAnyEnchantBesides(meta, type)) {
            return false;
        }
        if (hasAnyNoOtherEnchantConflict(meta, type)) {
            return false;
        }
        if (item.getType() == Material.TRIDENT) {
            if (type.requiresSoloOnTrident() && hasAnyEnchantBesides(meta, type)) {
                return false;
            }
            if (hasAnyTridentSoloConflict(meta, type)) {
                return false;
            }
        }

        int currentLevel = enchantStateService.getStoredEnchantLevel(meta, type);
        if (level <= currentLevel) {
            return false;
        }

        meta.getPersistentDataContainer().set(enchantStateService.enchantLevelKey(type), PersistentDataType.INTEGER, level);
        if (type.stripsVanillaEnchants()) {
            stripVanillaEnchants(meta);
        }
        if (type.appliesBindingCurse()) {
            meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        }
        if (type.stripsMendingAndUnbreaking()) {
            stripMendingAndUnbreaking(meta);
        }

        rebuildCustomLore(meta);
        item.setItemMeta(meta);
        return true;
    }

    void enforceHelmetRestrictions(@NotNull ItemStack helmet) {
        ItemMeta meta = helmet.getItemMeta();
        if (meta == null || !meta.hasEnchants()) {
            return;
        }
        stripVanillaEnchants(meta);
        rebuildCustomLore(meta);
        helmet.setItemMeta(meta);
    }

    boolean isMiasmaIncompatibleEnchant(@NotNull Enchantment enchantment) {
        return MIASMA_INCOMPATIBLE_ENCHANTS.contains(enchantment);
    }

    boolean hasMiasmaIncompatibleEnchant(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null && hasMiasmaIncompatibleEnchant(meta);
    }

    boolean hasMiasmaIncompatibleEnchant(@NotNull ItemMeta meta) {
        for (Enchantment enchantment : meta.getEnchants().keySet()) {
            if (isMiasmaIncompatibleEnchant(enchantment)) {
                return true;
            }
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            for (Enchantment enchantment : storageMeta.getStoredEnchants().keySet()) {
                if (isMiasmaIncompatibleEnchant(enchantment)) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean hasHealingTouchForbiddenEnchant(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null && hasMendingOrUnbreaking(meta);
    }

    boolean hasHealingTouchForbiddenEnchant(@NotNull ItemMeta meta) {
        return hasMendingOrUnbreaking(meta);
    }

    boolean hasSeekerForbiddenEnchant(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null && hasMendingOrUnbreaking(meta);
    }

    boolean hasSeekerForbiddenEnchant(@NotNull ItemMeta meta) {
        return hasMendingOrUnbreaking(meta);
    }

    boolean itemHasAnyEnchant(@Nullable ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && itemHasAnyEnchant(meta);
    }

    boolean itemHasAnyEnchant(@NotNull ItemMeta meta) {
        return hasAnyEnchantBesides(meta, null);
    }

    void stripHealingTouchForbiddenEnchants(@NotNull ItemMeta meta) {
        stripMendingAndUnbreaking(meta);
    }

    void stripSeekerForbiddenEnchants(@NotNull ItemMeta meta) {
        stripMendingAndUnbreaking(meta);
    }

    void rebuildCustomLore(@NotNull ItemMeta meta) {
        List<Component> lore = meta.lore() == null ? new ArrayList<>() : new ArrayList<>(meta.lore());
        lore.removeIf(this::isCustomLoreLine);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        for (EnchantType type : EnchantType.values()) {
            int level = pdc.getOrDefault(enchantStateService.enchantLevelKey(type), PersistentDataType.INTEGER, 0);
            if (level <= 0) {
                continue;
            }
            lore.add(Component.text(type.displayName + " " + RomanNumeralUtil.toRoman(level), type.color));
            if (type == EnchantType.FULL_FORCE) {
                lore.add(Component.text("Full Force Stats: 10 damage, ~8-block knockback", NamedTextColor.DARK_GRAY));
            }
        }

        meta.lore(lore.isEmpty() ? null : lore);
    }

    boolean hasAnyVisionHelmetEnchant(@Nullable ItemStack item) {
        return enchantStateService.getEnchantLevel(item, EnchantType.DIVINE_VISION) > 0
                || enchantStateService.getEnchantLevel(item, EnchantType.MINERS_INTUITION) > 0
                || enchantStateService.getEnchantLevel(item, EnchantType.LOOT_SENSE) > 0;
    }

    private boolean hasExclusiveConflict(@NotNull ItemMeta meta, @NotNull EnchantType adding) {
        for (EnchantType existing : EnchantType.values()) {
            if (existing == adding || enchantStateService.getStoredEnchantLevel(meta, existing) <= 0) {
                continue;
            }
            if (adding.conflictsWith(existing) || existing.conflictsWith(adding)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyNoOtherEnchantConflict(@NotNull ItemMeta meta, @NotNull EnchantType adding) {
        for (EnchantType existing : EnchantType.values()) {
            if (existing == adding || enchantStateService.getStoredEnchantLevel(meta, existing) <= 0) {
                continue;
            }
            if (existing.requiresNoOtherEnchantsOnItem()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyTridentSoloConflict(@NotNull ItemMeta meta, @NotNull EnchantType adding) {
        for (EnchantType existing : EnchantType.values()) {
            if (existing == adding || enchantStateService.getStoredEnchantLevel(meta, existing) <= 0) {
                continue;
            }
            if (existing.requiresSoloOnTrident()) {
                return true;
            }
        }
        return false;
    }

    private boolean hasAnyEnchantBesides(@NotNull ItemMeta meta, @Nullable EnchantType allowedCustom) {
        if (meta.hasEnchants()) {
            return true;
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta && !storageMeta.getStoredEnchants().isEmpty()) {
            return true;
        }
        for (EnchantType type : EnchantType.values()) {
            if (allowedCustom != null && type == allowedCustom) {
                continue;
            }
            if (enchantStateService.getStoredEnchantLevel(meta, type) > 0) {
                return true;
            }
        }
        return false;
    }

    private void stripVanillaEnchants(@NotNull ItemMeta meta) {
        if (!meta.hasEnchants()) {
            return;
        }
        List<Enchantment> enchants = new ArrayList<>(meta.getEnchants().keySet());
        for (Enchantment enchantment : enchants) {
            meta.removeEnchant(enchantment);
        }
    }

    private void stripMendingAndUnbreaking(@NotNull ItemMeta meta) {
        meta.removeEnchant(Enchantment.MENDING);
        meta.removeEnchant(Enchantment.UNBREAKING);
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            storageMeta.removeStoredEnchant(Enchantment.MENDING);
            storageMeta.removeStoredEnchant(Enchantment.UNBREAKING);
        }
    }

    private boolean hasMendingOrUnbreaking(@NotNull ItemMeta meta) {
        if (meta.hasEnchant(Enchantment.MENDING) || meta.hasEnchant(Enchantment.UNBREAKING)) {
            return true;
        }
        if (meta instanceof EnchantmentStorageMeta storageMeta) {
            return storageMeta.hasStoredEnchant(Enchantment.MENDING)
                    || storageMeta.hasStoredEnchant(Enchantment.UNBREAKING);
        }
        return false;
    }

    private boolean isCustomLoreLine(@NotNull Component component) {
        String plain = PlainTextComponentSerializer.plainText().serialize(component).trim();
        for (EnchantType type : EnchantType.values()) {
            if (plain.startsWith(type.displayName + " ")) {
                return true;
            }
        }
        return plain.startsWith("Full Force Stats: ");
    }
}

