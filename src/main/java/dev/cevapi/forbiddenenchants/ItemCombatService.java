package dev.cevapi.forbiddenenchants;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

final class ItemCombatService {
    private final Set<UUID> lumberjackActiveBreakers;

    ItemCombatService(@NotNull Set<UUID> lumberjackActiveBreakers) {
        this.lumberjackActiveBreakers = lumberjackActiveBreakers;
    }

    boolean isBottomTreeLog(@NotNull Block log) {
        if (!isLog(log.getType())) {
            return false;
        }
        return !isLog(log.getRelative(0, -1, 0).getType());
    }

    void breakWholeTree(@NotNull Player player, @NotNull Block start, @NotNull ItemStack tool) {
        if (!lumberjackActiveBreakers.add(player.getUniqueId())) {
            return;
        }
        try {
            List<Block> queue = new ArrayList<>();
            List<Block> connected = new ArrayList<>();
            queue.add(start);
            int index = 0;
            while (index < queue.size() && connected.size() < 256) {
                Block current = queue.get(index++);
                if (connected.contains(current)) {
                    continue;
                }
                Material type = current.getType();
                if (!isLog(type) && !isLeaves(type)) {
                    continue;
                }
                connected.add(current);
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {
                            if (dx == 0 && dy == 0 && dz == 0) {
                                continue;
                            }
                            queue.add(current.getRelative(dx, dy, dz));
                        }
                    }
                }
            }

            int blocksBroken = 0;
            for (Block block : connected) {
                if (block.equals(start)) {
                    if (isLog(block.getType()) || isLeaves(block.getType())) {
                        blocksBroken++;
                    }
                    continue;
                }
                if (isLog(block.getType()) || isLeaves(block.getType())) {
                    blocksBroken++;
                    block.breakNaturally(tool);
                }
            }
            // Start block already consumes one normal durability from the initiating break.
            int desiredTotalDurability = Math.max(1, (int) Math.ceil(blocksBroken * 0.5D));
            int extraDamage = Math.max(0, desiredTotalDurability - 1);
            if (extraDamage > 0) {
                damageHeldItem(player, EquipmentSlot.HAND, extraDamage);
            }
        } finally {
            lumberjackActiveBreakers.remove(player.getUniqueId());
        }
    }

    @Nullable ItemStack stealRandomInventoryItem(@NotNull Player victim) {
        ItemStack[] contents = victim.getInventory().getContents();
        List<Integer> candidates = new ArrayList<>();
        for (int i = 0; i < contents.length; i++) {
            ItemStack stack = contents[i];
            if (stack == null || stack.getType() == Material.AIR) {
                continue;
            }
            candidates.add(i);
        }
        if (candidates.isEmpty()) {
            return null;
        }
        int slot = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        ItemStack stack = contents[slot];
        if (stack == null || stack.getType() == Material.AIR) {
            return null;
        }
        ItemStack stolen = stack.clone();
        stolen.setAmount(1);
        if (stack.getAmount() <= 1) {
            victim.getInventory().setItem(slot, new ItemStack(Material.AIR));
        } else {
            stack.setAmount(stack.getAmount() - 1);
            victim.getInventory().setItem(slot, stack);
        }
        return stolen;
    }

    @NotNull ItemStack randomMobLootPreview(@NotNull LivingEntity entity) {
        EntityType type = entity.getType();
        return switch (type) {
            case CREEPER -> new ItemStack(Material.GUNPOWDER, 1);
            case ENDERMAN -> new ItemStack(Material.ENDER_PEARL, 1);
            case PIGLIN, ZOMBIFIED_PIGLIN -> new ItemStack(Material.GOLD_NUGGET, 1);
            case SKELETON -> new ItemStack(Material.BONE, 1);
            case SPIDER -> new ItemStack(Material.STRING, 1);
            case BLAZE -> new ItemStack(Material.BLAZE_ROD, 1);
            case WITCH -> new ItemStack(Material.REDSTONE, 1);
            default -> {
                ItemStack fallback = entity.getEquipment() != null ? entity.getEquipment().getItemInMainHand() : null;
                if (fallback != null && fallback.getType() != Material.AIR) {
                    ItemStack one = fallback.clone();
                    one.setAmount(1);
                    yield one;
                }
                yield new ItemStack(Material.ROTTEN_FLESH, 1);
            }
        };
    }

    void damageArmorByPercent(@NotNull Player player,
                              @NotNull EquipmentSlot slot,
                              @NotNull ItemStack item,
                              double fractionOfMax) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }
        meta.setUnbreakable(false);
        int max = item.getType().getMaxDurability();
        if (max <= 0) {
            return;
        }
        int amount = Math.max(1, (int) Math.ceil(max * fractionOfMax));
        int newDamage = damageable.getDamage() + amount;
        if (newDamage >= max) {
            switch (slot) {
                case HEAD -> player.getInventory().setHelmet(new ItemStack(Material.AIR));
                case CHEST -> player.getInventory().setChestplate(new ItemStack(Material.AIR));
                case LEGS -> player.getInventory().setLeggings(new ItemStack(Material.AIR));
                case FEET -> player.getInventory().setBoots(new ItemStack(Material.AIR));
                default -> {
                }
            }
            return;
        }
        damageable.setDamage(newDamage);
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

    void damageEquippedArmor(@NotNull Player player, @NotNull EquipmentSlot slot, int damageAmount, boolean respectUnbreaking) {
        PlayerInventory inventory = player.getInventory();
        ItemStack piece = switch (slot) {
            case HEAD -> inventory.getHelmet();
            case CHEST -> inventory.getChestplate();
            case LEGS -> inventory.getLeggings();
            case FEET -> inventory.getBoots();
            default -> null;
        };

        if (piece == null || piece.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = piece.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }
        meta.setUnbreakable(false);

        int appliedDamage = respectUnbreaking ? applyUnbreakingReduction(piece, damageAmount) : damageAmount;
        if (appliedDamage <= 0) {
            return;
        }

        int maxDurability = piece.getType().getMaxDurability();
        if (maxDurability <= 0) {
            return;
        }
        int nextDamage = damageable.getDamage() + appliedDamage;
        if (nextDamage >= maxDurability) {
            switch (slot) {
                case HEAD -> inventory.setHelmet(null);
                case CHEST -> inventory.setChestplate(null);
                case LEGS -> inventory.setLeggings(null);
                case FEET -> inventory.setBoots(null);
                default -> {
                }
            }
            return;
        }

        damageable.setDamage(nextDamage);
        piece.setItemMeta(meta);

        switch (slot) {
            case HEAD -> inventory.setHelmet(piece);
            case CHEST -> inventory.setChestplate(piece);
            case LEGS -> inventory.setLeggings(piece);
            case FEET -> inventory.setBoots(piece);
            default -> {
            }
        }
    }

    void damageHeldItem(@NotNull Player player, @NotNull EquipmentSlot slot, int damageAmount) {
        PlayerInventory inventory = player.getInventory();
        ItemStack held = slot == EquipmentSlot.OFF_HAND ? inventory.getItemInOffHand() : inventory.getItemInMainHand();
        if (held == null || held.getType() == Material.AIR) {
            return;
        }

        ItemMeta meta = held.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }
        meta.setUnbreakable(false);
        int maxDurability = held.getType().getMaxDurability();
        if (maxDurability <= 0) {
            return;
        }

        int nextDamage = damageable.getDamage() + Math.max(1, damageAmount);
        if (nextDamage >= maxDurability) {
            if (slot == EquipmentSlot.OFF_HAND) {
                inventory.setItemInOffHand(new ItemStack(Material.AIR));
            } else {
                inventory.setItemInMainHand(new ItemStack(Material.AIR));
            }
            return;
        }

        damageable.setDamage(nextDamage);
        held.setItemMeta(meta);
        if (slot == EquipmentSlot.OFF_HAND) {
            inventory.setItemInOffHand(held);
        } else {
            inventory.setItemInMainHand(held);
        }
    }

    private int applyUnbreakingReduction(@NotNull ItemStack item, int baseDamage) {
        int level = item.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.UNBREAKING);
        if (level <= 0) {
            return baseDamage;
        }

        int applied = 0;
        double damageChance = 1.0D / (level + 1.0D);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < baseDamage; i++) {
            if (random.nextDouble() <= damageChance) {
                applied++;
            }
        }
        return applied;
    }

    private boolean isLog(@NotNull Material material) {
        String name = material.name();
        return name.endsWith("_LOG") || name.endsWith("_STEM") || name.endsWith("_HYPHAE");
    }

    private boolean isLeaves(@NotNull Material material) {
        String name = material.name();
        return name.endsWith("_LEAVES") || name.equals("NETHER_WART_BLOCK") || name.equals("WARPED_WART_BLOCK");
    }
}

