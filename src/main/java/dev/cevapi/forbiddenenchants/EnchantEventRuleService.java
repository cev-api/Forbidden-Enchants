package dev.cevapi.forbiddenenchants;

import dev.cevapi.forbiddenenchants.enchants.EnchantList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.enchantments.EnchantmentOffer;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.enchantment.PrepareItemEnchantEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.block.ShulkerBox;
import org.bukkit.potion.PotionType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

final class EnchantEventRuleService {
    private static final int MAX_EXTRA_ENCHANTING_POWER = 30;
    private static final int MAX_ENCHANTING_COST = 60;
    private static final int SKELETON_SKULL_POWER = 5;
    private static final int CANDLE_POWER = 1;

    private final ForbiddenEnchantsPlugin plugin;
    private final Supplier<EnchantStateService> enchantStateServiceSupplier;
    private final Supplier<ItemClassificationService> itemClassificationServiceSupplier;
    private final EnchantBookFactoryService enchantBookFactoryService;
    private final EnchantRuleCoreService enchantRuleCoreService;

    EnchantEventRuleService(@NotNull ForbiddenEnchantsPlugin plugin,
                            @NotNull Supplier<EnchantStateService> enchantStateServiceSupplier,
                            @NotNull Supplier<ItemClassificationService> itemClassificationServiceSupplier,
                            @NotNull EnchantBookFactoryService enchantBookFactoryService,
                            @NotNull EnchantRuleCoreService enchantRuleCoreService) {
        this.plugin = plugin;
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
            if (enchantRuleCoreService.hasAnyMendingUnbreakingForbiddenEnchant(base)
                    && enchantRuleCoreService.hasMendingOrUnbreakingEnchant(addition)) {
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
            if (enchantStateServiceSupplier.get().getEnchantLevel(base, EnchantType.NO_FALL) > 0
                    && enchantRuleCoreService.hasNoFallForbiddenEnchant(addition)) {
                event.setResult(null);
            }
            if (enchantRuleCoreService.hasAnyNoOtherEnchant(base)
                    && enchantRuleCoreService.itemHasAnyEnchant(addition)) {
                event.setResult(null);
            }
            return;
        }

        if (book.type() == EnchantType.THE_DUPLICATOR) {
            ItemStack result = buildDuplicatorResult(base);
            event.setResult(result);
            if (result != null) {
                inventory.setRepairCost(64);
            }
            return;
        }
        if (book.type() == EnchantType.THE_PHILOSOPHERS_BOOK) {
            ItemStack result = buildPhilosopherResult(base, book.level());
            event.setResult(result);
            if (result != null) {
                inventory.setRepairCost(32 + Math.max(0, (book.level() - 1) * 16));
            }
            return;
        }

        if (!itemClassificationServiceSupplier.get().isArmorPieceForSlot(base, book.type().slot)) {
            event.setResult(null);
            return;
        }
        if (book.type().slot == ArmorSlot.POTION && !isWaterBottle(base)) {
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
        if (tryInjectEnchantingTableForbiddenBook(event)) {
            return;
        }

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
        if (enchantRuleCoreService.hasAnyNoOtherEnchant(event.getItem())) {
            event.setCancelled(true);
            return;
        }
        if (enchantRuleCoreService.hasAnyMendingUnbreakingForbiddenEnchant(event.getItem())) {
            for (Enchantment enchantment : event.getEnchantsToAdd().keySet()) {
                if (enchantment == Enchantment.UNBREAKING || enchantment == Enchantment.MENDING) {
                    event.setCancelled(true);
                    return;
                }
            }
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
        if (enchantStateServiceSupplier.get().getEnchantLevel(event.getItem(), EnchantType.NO_FALL) > 0) {
            for (Enchantment enchantment : event.getEnchantsToAdd().keySet()) {
                if (enchantment == Enchantment.FEATHER_FALLING || enchantment == Enchantment.MENDING) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    void onPrepareItemEnchant(@NotNull PrepareItemEnchantEvent event) {
        Block table = event.getEnchantBlock();
        EnchantmentOffer[] offers = event.getOffers();
        if (table == null || offers == null) {
            return;
        }

        int extraPower = calculateExtraEnchantingPower(table);
        if (extraPower <= 0) {
            return;
        }
        for (EnchantmentOffer offer : offers) {
            if (offer != null) {
                offer.setCost(clampInt(offer.getCost() + extraPower, 1, MAX_ENCHANTING_COST));
            }
        }
    }

    void onPlayerItemDamage(@NotNull PlayerItemDamageEvent event) {
        if (enchantRuleCoreService.hasAnyVisionHelmetEnchant(event.getItem())) {
            event.setDamage(Math.max(1, event.getDamage() * 2));
            return;
        }
        if (enchantRuleCoreService.hasAnyMendingUnbreakingForbiddenEnchant(event.getItem())) {
            ItemMeta meta = event.getItem().getItemMeta();
            if (meta != null && enchantRuleCoreService.hasMendingOrUnbreakingEnchant(event.getItem())) {
                enchantRuleCoreService.stripMendingAndUnbreakingFromForbiddenPenaltyItem(meta);
                event.getItem().setItemMeta(meta);
            }
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
        if (enchantStateServiceSupplier.get().getEnchantLevel(event.getItem(), EnchantType.NO_FALL) > 0) {
            ItemMeta meta = event.getItem().getItemMeta();
            if (meta != null && enchantRuleCoreService.hasNoFallForbiddenEnchant(meta)) {
                enchantRuleCoreService.stripNoFallForbiddenEnchants(meta);
                event.getItem().setItemMeta(meta);
            }
        }
    }

    private boolean isWaterBottle(@NotNull ItemStack item) {
        if (item.getType() != Material.POTION) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof PotionMeta potionMeta)) {
            return false;
        }
        return potionMeta.getBasePotionType() == PotionType.WATER;
    }

    private ItemStack buildDuplicatorResult(@NotNull ItemStack base) {
        if (isFilledShulker(base) || isFilledBundle(base) || isArmorOrWeapon(base.getType())) {
            return null;
        }
        ItemStack result = base.clone();
        int doubled = Math.max(1, Math.min(127, base.getAmount() * 2));
        result.setAmount(doubled);
        return result;
    }

    private ItemStack buildPhilosopherResult(@NotNull ItemStack base, int level) {
        int clampedLevel = Math.max(1, Math.min(3, level));
        return switch (clampedLevel) {
            case 1 -> {
                if (base.getType() == Material.IRON_INGOT) {
                    yield new ItemStack(Material.GOLD_INGOT, base.getAmount());
                }
                if (base.getType() == Material.IRON_BLOCK) {
                    yield new ItemStack(Material.GOLD_BLOCK, base.getAmount());
                }
                yield null;
            }
            case 2 -> {
                if (base.getType() == Material.GOLD_INGOT) {
                    yield new ItemStack(Material.DIAMOND, base.getAmount());
                }
                if (base.getType() == Material.GOLD_BLOCK) {
                    yield new ItemStack(Material.DIAMOND_BLOCK, base.getAmount());
                }
                yield null;
            }
            default -> {
                if (base.getType() == Material.DIAMOND) {
                    yield new ItemStack(Material.NETHERITE_INGOT, base.getAmount());
                }
                if (base.getType() == Material.DIAMOND_BLOCK) {
                    yield new ItemStack(Material.NETHERITE_BLOCK, base.getAmount());
                }
                yield null;
            }
        };
    }

    private boolean isFilledShulker(@NotNull ItemStack stack) {
        if (!stack.getType().name().endsWith("SHULKER_BOX")) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (!(meta instanceof BlockStateMeta blockStateMeta)) {
            return false;
        }
        if (!(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) {
            return false;
        }
        Inventory inventory = shulkerBox.getInventory();
        for (ItemStack item : inventory.getContents()) {
            if (item != null && item.getType() != Material.AIR) {
                return true;
            }
        }
        return false;
    }

    private boolean isFilledBundle(@NotNull ItemStack stack) {
        if (stack.getType() != EnchantMaterialCatalog.materialIfPresent("BUNDLE")) {
            return false;
        }
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return false;
        }
        try {
            java.lang.reflect.Method getItems = meta.getClass().getMethod("getItems");
            Object value = getItems.invoke(meta);
            if (value instanceof java.util.Collection<?> collection) {
                return !collection.isEmpty();
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private boolean isArmorOrWeapon(@NotNull Material type) {
        String name = type.name();
        if (name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || type == Material.ELYTRA
                || type == Material.TURTLE_HELMET) {
            return true;
        }
        return name.endsWith("_SWORD")
                || name.endsWith("_AXE")
                || name.endsWith("_HOE")
                || type == Material.BOW
                || type == Material.CROSSBOW
                || type == Material.TRIDENT
                || type == Material.MACE
                || name.endsWith("_SPEAR")
                || name.equals("SPEAR");
    }

    private boolean tryInjectEnchantingTableForbiddenBook(@NotNull EnchantItemEvent event) {
        if (!plugin.isEnchantingTableInjectorEnabled()) {
            return false;
        }
        if (event.getItem().getType() != Material.BOOK) {
            return false;
        }
        int selectedCost = event.getExpLevelCost();
        int requiredCost = plugin.getEnchantingTableInjectorXpCost();
        if (selectedCost < requiredCost) {
            return false;
        }
        EnchantingTableBookEntry selected = rollConfiguredEnchantingTableBook();
        if (selected == null) {
            return false;
        }
        ItemStack injected = plugin.createBook(selected.type(), selected.level());
        if (injected == null) {
            return false;
        }

        event.setExpLevelCost(requiredCost);
        org.bukkit.inventory.Inventory inventory = event.getInventory();
        ItemStack replacement = injected.clone();
        Bukkit.getScheduler().runTask(plugin, () -> {
            ItemStack current = inventory.getItem(0);
            if (current == null || current.getType() == Material.AIR) {
                return;
            }
            replacement.setAmount(1);
            inventory.setItem(0, replacement);
        });
        return true;
    }

    private EnchantingTableBookEntry rollConfiguredEnchantingTableBook() {
        List<EnchantingTableBookEntry> entries = new ArrayList<>();
        double total = 0.0D;
        for (EnchantingTableBookEntry entry : plugin.enchantingTableInjectorBooks()) {
            if (entry.chancePercent() <= 0.0D) {
                continue;
            }
            if (plugin.isRetiredEnchant(entry.type()) || !plugin.isEnchantSpawnEnabled(entry.type())) {
                continue;
            }
            entries.add(entry);
            total += Math.max(0.0D, Math.min(100.0D, entry.chancePercent()));
        }
        if (entries.isEmpty() || total <= 0.0D) {
            return null;
        }

        double procChance = Math.min(100.0D, total);
        if (ThreadLocalRandom.current().nextDouble(100.0D) >= procChance) {
            return null;
        }

        double weightedRoll = ThreadLocalRandom.current().nextDouble(total);
        double cumulative = 0.0D;
        for (EnchantingTableBookEntry entry : entries) {
            cumulative += Math.max(0.0D, Math.min(100.0D, entry.chancePercent()));
            if (weightedRoll <= cumulative) {
                return entry;
            }
        }
        return entries.get(entries.size() - 1);
    }

    private int calculateExtraEnchantingPower(@NotNull Block enchantingTable) {
        int extra = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dy = -1; dy <= 2; dy++) {
                for (int dz = -3; dz <= 3; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    Block nearby = enchantingTable.getRelative(dx, dy, dz);
                    Material type = nearby.getType();
                    if (type == Material.SKELETON_SKULL || type == Material.SKELETON_WALL_SKULL) {
                        extra += SKELETON_SKULL_POWER;
                    } else if (Tag.CANDLES.isTagged(type)) {
                        extra += CANDLE_POWER;
                    }
                    if (extra >= MAX_EXTRA_ENCHANTING_POWER) {
                        return MAX_EXTRA_ENCHANTING_POWER;
                    }
                }
            }
        }
        return Math.min(extra, MAX_EXTRA_ENCHANTING_POWER);
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

