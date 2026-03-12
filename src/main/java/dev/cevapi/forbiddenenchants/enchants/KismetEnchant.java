package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class KismetEnchant extends BaseForbiddenEnchant {
    private static final int STATE_DOUBLE_DROPS = 1;
    private static final int STATE_NO_DROPS = 2;

    public KismetEnchant() {
        super("kismet",
                "kismet_level",
                "Kismet",
                ArmorSlot.AXE,
                1,
                NamedTextColor.DARK_RED,
                List.of("kismet", "fate_tool", "tool_fate"),
                "Apply to pickaxes, shovels, or axes in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Curse: on first use, tool fate is locked forever: either double drops or no drops.";
    }

    public boolean isSupportedTool(@NotNull ItemStack stack) {
        String name = stack.getType().name();
        return name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_AXE");
    }

    public void onToolBlockBreak(@NotNull BlockBreakEvent event, @NotNull ItemStack tool) {
        if (!isSupportedTool(tool)) {
            return;
        }
        int state = getOrAssignState(tool, event.getPlayer());
        if (state == STATE_NO_DROPS) {
            event.setDropItems(false);
            return;
        }
        if (state != STATE_DOUBLE_DROPS || !event.isDropItems()) {
            return;
        }

        Block block = event.getBlock();
        Location dropLocation = block.getLocation().add(0.5D, 0.5D, 0.5D);
        for (ItemStack drop : block.getDrops(tool, event.getPlayer())) {
            if (drop == null || drop.getType() == Material.AIR || drop.getAmount() <= 0) {
                continue;
            }
            block.getWorld().dropItemNaturally(dropLocation, sanitizeDuplicatedDrop(drop.clone()));
        }
    }

    private int getOrAssignState(@NotNull ItemStack tool, @NotNull Player player) {
        ItemMeta meta = tool.getItemMeta();
        if (meta == null) {
            return STATE_NO_DROPS;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer existing = pdc.get(kismetStateKey(), PersistentDataType.INTEGER);
        if (existing != null && (existing == STATE_DOUBLE_DROPS || existing == STATE_NO_DROPS)) {
            return existing;
        }

        int rolled = ThreadLocalRandom.current().nextBoolean() ? STATE_DOUBLE_DROPS : STATE_NO_DROPS;
        pdc.set(kismetStateKey(), PersistentDataType.INTEGER, rolled);
        tool.setItemMeta(meta);

        if (rolled == STATE_DOUBLE_DROPS) {
            player.sendActionBar(Component.text("Kismet: fate chosen - double drops.", NamedTextColor.GOLD));
        } else {
            player.sendActionBar(Component.text("Kismet: fate chosen - no drops.", NamedTextColor.DARK_RED));
        }
        return rolled;
    }

    private @NotNull NamespacedKey kismetStateKey() {
        return new NamespacedKey(ForbiddenEnchantsPlugin.instance(), "kismet_fate_state");
    }

    private @NotNull ItemStack sanitizeDuplicatedDrop(@NotNull ItemStack duplicatedDrop) {
        if (!duplicatedDrop.getType().name().endsWith("SHULKER_BOX")) {
            return duplicatedDrop;
        }
        ItemMeta meta = duplicatedDrop.getItemMeta();
        if (!(meta instanceof BlockStateMeta blockStateMeta)) {
            return duplicatedDrop;
        }
        if (!(blockStateMeta.getBlockState() instanceof ShulkerBox shulkerBox)) {
            return duplicatedDrop;
        }
        shulkerBox.getInventory().clear();
        blockStateMeta.setBlockState(shulkerBox);
        duplicatedDrop.setItemMeta(blockStateMeta);
        return duplicatedDrop;
    }
}
