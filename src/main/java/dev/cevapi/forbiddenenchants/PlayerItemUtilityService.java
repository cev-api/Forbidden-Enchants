package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

final class PlayerItemUtilityService {

    void damageItemByPercent(@NotNull Player player,
                             @NotNull EquipmentSlot slot,
                             @NotNull ItemStack item,
                             double fractionOfMax) {
        ItemMeta meta = item.getItemMeta();
        if (!(meta instanceof Damageable damageable)) {
            return;
        }
        int max = item.getType().getMaxDurability();
        if (max <= 0) {
            return;
        }
        int amount = Math.max(1, (int) Math.ceil(max * fractionOfMax));
        int newDamage = damageable.getDamage() + amount;
        if (newDamage >= max) {
            ItemStack broken = new ItemStack(Material.AIR);
            switch (slot) {
                case HAND -> player.getInventory().setItemInMainHand(broken);
                case OFF_HAND -> player.getInventory().setItemInOffHand(broken);
                case HEAD -> player.getInventory().setHelmet(broken);
                case CHEST -> player.getInventory().setChestplate(broken);
                case LEGS -> player.getInventory().setLeggings(broken);
                case FEET -> player.getInventory().setBoots(broken);
                default -> {
                }
            }
            return;
        }
        damageable.setDamage(newDamage);
        item.setItemMeta(meta);
        switch (slot) {
            case HAND -> player.getInventory().setItemInMainHand(item);
            case OFF_HAND -> player.getInventory().setItemInOffHand(item);
            case HEAD -> player.getInventory().setHelmet(item);
            case CHEST -> player.getInventory().setChestplate(item);
            case LEGS -> player.getInventory().setLeggings(item);
            case FEET -> player.getInventory().setBoots(item);
            default -> {
            }
        }
    }

    @Nullable String getNameTagText(@NotNull ItemStack nameTag) {
        ItemMeta meta = nameTag.getItemMeta();
        if (meta == null || meta.displayName() == null) {
            return null;
        }
        String plain = PlainTextComponentSerializer.plainText().serialize(meta.displayName()).trim();
        return plain.isEmpty() ? null : plain;
    }

    void consumeOneFromHand(@NotNull Player player, @NotNull EquipmentSlot hand) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        PlayerInventory inv = player.getInventory();
        ItemStack stack = hand == EquipmentSlot.OFF_HAND ? inv.getItemInOffHand() : inv.getItemInMainHand();
        if (stack.getType() == Material.AIR) {
            return;
        }
        int amount = stack.getAmount();
        if (amount <= 1) {
            if (hand == EquipmentSlot.OFF_HAND) {
                inv.setItemInOffHand(new ItemStack(Material.AIR));
            } else {
                inv.setItemInMainHand(new ItemStack(Material.AIR));
            }
            return;
        }
        stack.setAmount(amount - 1);
        if (hand == EquipmentSlot.OFF_HAND) {
            inv.setItemInOffHand(stack);
        } else {
            inv.setItemInMainHand(stack);
        }
    }

    void giveOrDrop(@NotNull Player target, @NotNull ItemStack item) {
        Map<Integer, ItemStack> overflow = target.getInventory().addItem(item);
        for (ItemStack stack : overflow.values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), stack);
        }
    }

    @NotNull String describeItem(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.displayName() != null) {
            return PlainTextComponentSerializer.plainText().serialize(meta.displayName());
        }
        return DisplayNameUtil.toDisplayName(item.getType());
    }
}

