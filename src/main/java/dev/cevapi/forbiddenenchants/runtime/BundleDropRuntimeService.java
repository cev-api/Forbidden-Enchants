package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

final class BundleDropRuntimeService {
    private final ForbiddenEnchantsPlugin plugin;

    BundleDropRuntimeService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    void onEntityDeath(@NotNull EntityDeathEvent event) {
        if (!plugin.isBundleDropEnabled()) {
            return;
        }
        EntityType type = event.getEntityType();
        Double chance = plugin.bundleDropMobChances().get(type);
        if (chance == null || chance <= 0.0D) {
            return;
        }
        if (!roll(chance)) {
            return;
        }

        ItemStack bundle = createBundleDrop();
        if (bundle == null) {
            return;
        }
        event.getDrops().add(bundle);
        for (ItemStack extra : plugin.bundleDropExtraDrops()) {
            if (extra != null && !extra.getType().isAir()) {
                ItemStack copy = extra.clone();
                copy.setAmount(1);
                event.getDrops().add(copy);
            }
        }
    }

    @Nullable ItemStack createBundleDrop() {
        List<BundleDropReward> rewards = plugin.bundleDropRewards();
        if (rewards.isEmpty()) {
            return null;
        }

        ItemStack bundle = new ItemStack(Material.BUNDLE);
        ItemMeta rawMeta = bundle.getItemMeta();
        if (!(rawMeta instanceof BundleMeta meta)) {
            return null;
        }

        List<ItemStack> contents = new ArrayList<>();
        int usedWeight = 0;
        for (BundleDropReward reward : rewards) {
            ItemStack built = buildRewardStack(reward);
            if (built == null || built.getType().isAir() || built.getAmount() <= 0) {
                continue;
            }

            int pointsPerItem = bundleWeightPerItem(built);
            int maxUnits = (64 - usedWeight) / pointsPerItem;
            if (maxUnits <= 0) {
                break;
            }
            int amount = Math.min(built.getAmount(), maxUnits);
            if (amount <= 0) {
                continue;
            }
            built.setAmount(amount);
            usedWeight += amount * pointsPerItem;
            contents.add(built);
            if (usedWeight >= 64) {
                break;
            }
        }

        if (contents.isEmpty()) {
            return null;
        }

        meta.displayName(Component.text("Forbidden Bundle Drop", NamedTextColor.GOLD));
        meta.lore(List.of(
                Component.text("Mob loot bundle", NamedTextColor.GRAY),
                Component.text("Contains configured rewards", NamedTextColor.DARK_GRAY)
        ));
        meta.setItems(contents);
        bundle.setItemMeta(meta);
        return bundle;
    }

    int calculateBundleWeight(@NotNull List<BundleDropReward> rewards) {
        int usedWeight = 0;
        for (BundleDropReward reward : rewards) {
            ItemStack built = buildRewardStack(reward);
            if (built == null || built.getType().isAir() || built.getAmount() <= 0) {
                continue;
            }
            int pointsPerItem = bundleWeightPerItem(built);
            int maxUnits = (64 - usedWeight) / pointsPerItem;
            if (maxUnits <= 0) {
                break;
            }
            int amount = Math.min(built.getAmount(), maxUnits);
            usedWeight += amount * pointsPerItem;
            if (usedWeight >= 64) {
                return 64;
            }
        }
        return Math.max(0, Math.min(64, usedWeight));
    }

    private int bundleWeightPerItem(@NotNull ItemStack built) {
        return Math.max(1, (int) Math.ceil(64.0D / Math.max(1, built.getMaxStackSize())));
    }

    private boolean roll(double chancePercent) {
        double clamped = Math.max(0.0D, Math.min(100.0D, chancePercent));
        return ThreadLocalRandom.current().nextDouble(100.0D) < clamped;
    }

    private @Nullable ItemStack buildRewardStack(@NotNull BundleDropReward reward) {
        return switch (reward.type()) {
            case ENCHANT -> buildEnchantBookReward(reward);
            case POTION -> buildPotionReward(reward);
            case MATERIAL -> buildMaterialReward(reward);
        };
    }

    private @Nullable ItemStack buildEnchantBookReward(@NotNull BundleDropReward reward) {
        EnchantType type = EnchantType.fromArg(reward.key());
        if (type == null || type.slot == ArmorSlot.POTION) {
            return null;
        }
        int level = Math.max(1, Math.min(reward.level(), type.maxLevel));
        return plugin.createBook(type, level);
    }

    private @Nullable ItemStack buildPotionReward(@NotNull BundleDropReward reward) {
        EnchantType type = EnchantType.fromArg(reward.key());
        if (type == null || type.slot != ArmorSlot.POTION) {
            return null;
        }
        int level = Math.max(1, Math.min(reward.level(), type.maxLevel));
        return plugin.createEnchantedItem(type, level, Material.POTION);
    }

    private @Nullable ItemStack buildMaterialReward(@NotNull BundleDropReward reward) {
        Material material = SlotParsingUtil.parseMaterial(reward.key());
        if (material == null || material.isAir()) {
            return null;
        }
        int amount = Math.max(1, Math.min(reward.amount(), material.getMaxStackSize()));
        return new ItemStack(material, amount);
    }
}
