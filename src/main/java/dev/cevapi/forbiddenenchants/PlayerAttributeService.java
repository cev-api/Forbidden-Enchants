package dev.cevapi.forbiddenenchants;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

final class PlayerAttributeService {
    private final EnchantRuleCoreService enchantRuleCoreService;

    PlayerAttributeService(@NotNull EnchantRuleCoreService enchantRuleCoreService) {
        this.enchantRuleCoreService = enchantRuleCoreService;
    }

    void setPlayerReach(@NotNull Player player, double blockRange, double entityRange) {
        setAttributeBaseByAnyName(player, blockRange, "PLAYER_BLOCK_INTERACTION_RANGE", "BLOCK_INTERACTION_RANGE");
        setAttributeBaseByAnyName(player, entityRange, "PLAYER_ENTITY_INTERACTION_RANGE", "ENTITY_INTERACTION_RANGE");
    }

    void enforceHelmetRestrictions(@NotNull Player player, @NotNull ItemStack helmet) {
        enchantRuleCoreService.enforceHelmetRestrictions(helmet);
        player.getInventory().setHelmet(helmet);
    }

    void enforceDurabilityCap(@NotNull ItemStack item,
                              int maxRemainingDurability,
                              @NotNull Player player,
                              @NotNull EquipmentSlot slot) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        if (!enforceDurabilityCap(item, meta, maxRemainingDurability)) {
            return;
        }

        item.setItemMeta(meta);
        switch (slot) {
            case HEAD -> player.getInventory().setHelmet(item);
            case CHEST -> player.getInventory().setChestplate(item);
            case LEGS -> player.getInventory().setLeggings(item);
            case FEET -> player.getInventory().setBoots(item);
            default -> {
            }
        }
    }

    private void setAttributeBase(@NotNull Player player, @NotNull Attribute attribute, double value) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance != null && Math.abs(instance.getBaseValue() - value) > 0.001) {
            instance.setBaseValue(value);
        }
    }

    private void setAttributeBaseByAnyName(@NotNull Player player, double value, @NotNull String... attributeNames) {
        for (String attributeName : attributeNames) {
            try {
                Attribute attribute = Attribute.valueOf(attributeName);
                setAttributeBase(player, attribute, value);
                return;
            } catch (IllegalArgumentException ignored) {
                // Try the next candidate name.
            }
        }
    }

    private boolean enforceDurabilityCap(@NotNull ItemStack item, @NotNull ItemMeta meta, int maxRemainingDurability) {
        if (!(meta instanceof Damageable damageable)) {
            return false;
        }

        int maxDurability = item.getType().getMaxDurability();
        if (maxDurability <= 0) {
            return false;
        }

        int clampedRemaining = Math.max(1, Math.min(maxRemainingDurability, maxDurability));
        int minimumDamage = maxDurability - clampedRemaining;
        if (damageable.getDamage() >= minimumDamage) {
            return false;
        }

        damageable.setDamage(minimumDamage);
        return true;
    }
}

