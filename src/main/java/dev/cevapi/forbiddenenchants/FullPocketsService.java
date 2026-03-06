package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

final class FullPocketsService {
    private final Supplier<ItemClassificationService> itemClassificationServiceSupplier;
    private final Supplier<EnchantStateService> enchantStateServiceSupplier;
    private final Supplier<MysteryItemService> mysteryItemServiceSupplier;
    private final Supplier<NamespacedKey> fullPocketsAppliedKeySupplier;
    private final org.bukkit.plugin.Plugin schedulerPlugin;
    private final LongSupplier tickCounterSupplier;

    FullPocketsService(@NotNull Supplier<ItemClassificationService> itemClassificationServiceSupplier,
                       @NotNull Supplier<EnchantStateService> enchantStateServiceSupplier,
                       @NotNull Supplier<MysteryItemService> mysteryItemServiceSupplier,
                       @NotNull Supplier<NamespacedKey> fullPocketsAppliedKeySupplier,
                       @NotNull org.bukkit.plugin.Plugin schedulerPlugin,
                       @NotNull LongSupplier tickCounterSupplier) {
        this.itemClassificationServiceSupplier = itemClassificationServiceSupplier;
        this.enchantStateServiceSupplier = enchantStateServiceSupplier;
        this.mysteryItemServiceSupplier = mysteryItemServiceSupplier;
        this.fullPocketsAppliedKeySupplier = fullPocketsAppliedKeySupplier;
        this.schedulerPlugin = schedulerPlugin;
        this.tickCounterSupplier = tickCounterSupplier;
    }

    void onOpen(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack leggings = player.getInventory().getLeggings();
        if (!itemClassificationServiceSupplier.get().isArmorPieceForSlot(leggings, ArmorSlot.LEGGINGS)) {
            return;
        }
        int level = enchantStateServiceSupplier.get().getEnchantLevel(leggings, EnchantType.FULL_POCKETS);
        if (level <= 0) {
            return;
        }
        mysteryItemServiceSupplier.get().revealMysteryItemIfNeeded(leggings, player, EquipmentSlot.LEGS);

        if (!(block.getState() instanceof Container) || !(block.getState() instanceof TileState tileState)) {
            return;
        }
        PersistentDataContainer data = tileState.getPersistentDataContainer();
        if (data.has(fullPocketsAppliedKeySupplier.get(), PersistentDataType.BYTE)) {
            return;
        }

        data.set(fullPocketsAppliedKeySupplier.get(), PersistentDataType.BYTE, (byte) 1);
        tileState.update(true, false);

        double chance = Math.min(0.40D, level * 0.10D);
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return;
        }

        ItemStack bonus = createLoot();
        if (bonus == null) {
            return;
        }

        Location dropLocation = block.getLocation().add(0.5D, 0.8D, 0.5D);
        UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTaskLater(schedulerPlugin, () -> {
            Player online = Bukkit.getPlayer(playerId);
            if (online == null || !online.isOnline()) {
                return;
            }

            Inventory destination = null;
            Inventory top = online.getOpenInventory().getTopInventory();
            if (isViewingBlockInventory(top, block)) {
                destination = top;
            } else if (block.getState() instanceof Container postContainer) {
                destination = postContainer.getInventory();
            }
            if (destination == null) {
                return;
            }

            Map<Integer, ItemStack> overflow = destination.addItem(bonus.clone());
            for (ItemStack overflowItem : overflow.values()) {
                block.getWorld().dropItemNaturally(dropLocation, overflowItem);
            }

            int overflowAmount = countItemAmount(overflow);
            int insertedAmount = Math.max(0, bonus.getAmount() - overflowAmount);
            if (insertedAmount <= 0) {
                sendActionBarForDuration(
                        online.getUniqueId(),
                        Component.text("Full Pockets triggered, but no space was available.", NamedTextColor.RED),
                        100L
                );
                return;
            }

            sendActionBarForDuration(
                    online.getUniqueId(),
                    Component.text(
                            "Full Pockets: +" + insertedAmount + " " + DisplayNameUtil.toDisplayName(bonus.getType()) + " (" + (level * 10) + "% roll).",
                            NamedTextColor.GOLD
                    ),
                    100L
            );
        }, 1L);
    }

    private void sendActionBarForDuration(@NotNull UUID playerId, @NotNull Component message, long durationTicks) {
        long endTick = tickCounterSupplier.getAsLong() + Math.max(1L, durationTicks);
        Bukkit.getScheduler().runTaskTimer(schedulerPlugin, task -> {
            if (tickCounterSupplier.getAsLong() > endTick) {
                task.cancel();
                return;
            }
            Player online = Bukkit.getPlayer(playerId);
            if (online == null || !online.isOnline()) {
                task.cancel();
                return;
            }
            online.sendActionBar(message);
        }, 0L, 20L);
    }

    private @Nullable ItemStack createLoot() {
        double roll = ThreadLocalRandom.current().nextDouble();
        if (roll < 0.40D) {
            return new ItemStack(Material.DIAMOND, ThreadLocalRandom.current().nextInt(1, 3));
        }
        if (roll < 0.72D) {
            return new ItemStack(Material.EMERALD, ThreadLocalRandom.current().nextInt(2, 6));
        }
        if (roll < 0.86D) {
            return new ItemStack(Material.NETHERITE_SCRAP, 1);
        }
        if (roll < 0.96D) {
            return new ItemStack(Material.EXPERIENCE_BOTTLE, ThreadLocalRandom.current().nextInt(8, 17));
        }
        return new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1);
    }

    private int countItemAmount(@NotNull Map<Integer, ItemStack> stacks) {
        int total = 0;
        for (ItemStack stack : stacks.values()) {
            if (stack != null && stack.getType() != Material.AIR) {
                total += stack.getAmount();
            }
        }
        return total;
    }

    private boolean isViewingBlockInventory(@Nullable Inventory inventory, @NotNull Block block) {
        if (inventory == null) {
            return false;
        }
        InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof BlockInventoryHolder blockHolder)) {
            return false;
        }
        Block holderBlock = blockHolder.getBlock();
        return holderBlock.getWorld().equals(block.getWorld())
                && holderBlock.getX() == block.getX()
                && holderBlock.getY() == block.getY()
                && holderBlock.getZ() == block.getZ();
    }
}

