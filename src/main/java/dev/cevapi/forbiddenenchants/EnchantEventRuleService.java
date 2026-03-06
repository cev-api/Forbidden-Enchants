package dev.cevapi.forbiddenenchants;

import dev.cevapi.forbiddenenchants.enchants.EnchantList;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

final class EnchantEventRuleService {
    private final Supplier<EnchantStateService> enchantStateServiceSupplier;
    private final Supplier<ItemClassificationService> itemClassificationServiceSupplier;
    private final EnchantBookFactoryService enchantBookFactoryService;
    private final EnchantRuleCoreService enchantRuleCoreService;

    EnchantEventRuleService(@NotNull Supplier<EnchantStateService> enchantStateServiceSupplier,
                            @NotNull Supplier<ItemClassificationService> itemClassificationServiceSupplier,
                            @NotNull EnchantBookFactoryService enchantBookFactoryService,
                            @NotNull EnchantRuleCoreService enchantRuleCoreService) {
        this.enchantStateServiceSupplier = enchantStateServiceSupplier;
        this.itemClassificationServiceSupplier = itemClassificationServiceSupplier;
        this.enchantBookFactoryService = enchantBookFactoryService;
        this.enchantRuleCoreService = enchantRuleCoreService;
    }

    void onPrepareAnvil(@NotNull PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack base = inventory.getItem(0);
        ItemStack addition = inventory.getItem(1);

        if (base == null || base.getType() == Material.AIR || addition == null || addition.getType() == Material.AIR) {
            return;
        }

        BookSpec book = enchantBookFactoryService.readBookSpec(addition);
        if (book == null) {
            if (enchantRuleCoreService.hasAnyVisionHelmetEnchant(base)) {
                event.setResult(null);
            }
            if (enchantStateServiceSupplier.get().getEnchantLevel(base, EnchantType.MIASMA) > 0
                    && enchantRuleCoreService.hasMiasmaIncompatibleEnchant(addition)) {
                event.setResult(null);
            }
            int healingTouchLevel = enchantStateServiceSupplier.get().getEnchantLevel(base, EnchantType.HEALING_TOUCH);
            if (EnchantList.INSTANCE.healingTouch().isActive(healingTouchLevel)
                    && enchantRuleCoreService.hasHealingTouchForbiddenEnchant(addition)) {
                event.setResult(null);
            }
            if (enchantStateServiceSupplier.get().getEnchantLevel(base, EnchantType.THE_SEEKER) > 0
                    && enchantRuleCoreService.hasSeekerForbiddenEnchant(addition)) {
                event.setResult(null);
            }
            if ((enchantStateServiceSupplier.get().getEnchantLevel(base, EnchantType.EXPLOSIVE_REACTION) > 0
                    || enchantStateServiceSupplier.get().getEnchantLevel(base, EnchantType.DRAGONS_BREATH) > 0)
                    && enchantRuleCoreService.itemHasAnyEnchant(addition)) {
                event.setResult(null);
            }
            return;
        }

        if (!itemClassificationServiceSupplier.get().isArmorPieceForSlot(base, book.type().slot)) {
            event.setResult(null);
            return;
        }
        if (!itemClassificationServiceSupplier.get().isMaterialValidForEnchant(base.getType(), book.type())) {
            event.setResult(null);
            return;
        }

        ItemStack result = base.clone();
        if (!enchantRuleCoreService.applyBookEnchant(result, book.type(), book.level())) {
            event.setResult(null);
            return;
        }

        event.setResult(result);
        inventory.setRepairCost(6 + (book.level() * 2));
    }

    void onEnchantItem(@NotNull EnchantItemEvent event) {
        if (enchantRuleCoreService.hasAnyVisionHelmetEnchant(event.getItem())) {
            event.setCancelled(true);
            return;
        }

        if (enchantStateServiceSupplier.get().getEnchantLevel(event.getItem(), EnchantType.MIASMA) > 0) {
            for (Enchantment enchantment : event.getEnchantsToAdd().keySet()) {
                if (enchantRuleCoreService.isMiasmaIncompatibleEnchant(enchantment)) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (enchantStateServiceSupplier.get().getEnchantLevel(event.getItem(), EnchantType.EXPLOSIVE_REACTION) > 0
                || enchantStateServiceSupplier.get().getEnchantLevel(event.getItem(), EnchantType.DRAGONS_BREATH) > 0) {
            event.setCancelled(true);
            return;
        }
        if (EnchantList.INSTANCE.healingTouch().isActive(
                enchantStateServiceSupplier.get().getEnchantLevel(event.getItem(), EnchantType.HEALING_TOUCH))) {
            for (Enchantment enchantment : event.getEnchantsToAdd().keySet()) {
                if (enchantment == Enchantment.UNBREAKING || enchantment == Enchantment.MENDING) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
        if (enchantStateServiceSupplier.get().getEnchantLevel(event.getItem(), EnchantType.THE_SEEKER) > 0) {
            for (Enchantment enchantment : event.getEnchantsToAdd().keySet()) {
                if (enchantment == Enchantment.UNBREAKING || enchantment == Enchantment.MENDING) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    void onPlayerItemDamage(@NotNull PlayerItemDamageEvent event) {
        if (enchantRuleCoreService.hasAnyVisionHelmetEnchant(event.getItem())) {
            event.setDamage(Math.max(1, event.getDamage() * 2));
            return;
        }
        if (EnchantList.INSTANCE.healingTouch().isActive(
                enchantStateServiceSupplier.get().getEnchantLevel(event.getItem(), EnchantType.HEALING_TOUCH))) {
            event.setDamage(Math.max(1, event.getDamage() * 4));
            ItemMeta meta = event.getItem().getItemMeta();
            if (meta != null && enchantRuleCoreService.hasHealingTouchForbiddenEnchant(meta)) {
                enchantRuleCoreService.stripHealingTouchForbiddenEnchants(meta);
                event.getItem().setItemMeta(meta);
            }
        }
        if (enchantStateServiceSupplier.get().getEnchantLevel(event.getItem(), EnchantType.THE_SEEKER) > 0) {
            ItemMeta meta = event.getItem().getItemMeta();
            if (meta != null && enchantRuleCoreService.hasSeekerForbiddenEnchant(meta)) {
                enchantRuleCoreService.stripSeekerForbiddenEnchants(meta);
                event.getItem().setItemMeta(meta);
            }
        }
    }
}

