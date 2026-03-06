package dev.cevapi.forbiddenenchants;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

final class VoidGraspService {
    private final Plugin plugin;
    private final Predicate<Material> lootSenseTarget;

    VoidGraspService(@NotNull Plugin plugin, @NotNull Predicate<Material> lootSenseTarget) {
        this.plugin = plugin;
        this.lootSenseTarget = lootSenseTarget;
    }

    @Nullable Block findFirstInteractableBlockInView(@NotNull Player player, double reach, boolean throughWalls) {
        Location eye = player.getEyeLocation();
        Vector direction = eye.getDirection().normalize();
        Block previous = null;

        for (double distance = 0.6; distance <= reach; distance += 0.25) {
            Location sample = eye.clone().add(direction.clone().multiply(distance));
            Block block = sample.getBlock();

            if (previous != null
                    && previous.getX() == block.getX()
                    && previous.getY() == block.getY()
                    && previous.getZ() == block.getZ()) {
                continue;
            }

            previous = block;
            if (isInteractable(block.getType())) {
                return block;
            }

            if (!throughWalls && !block.isPassable()) {
                return null;
            }
        }

        return null;
    }

    boolean performInteraction(@NotNull Player player, @NotNull Block block) {
        if (block.getState() instanceof Container container) {
            player.openInventory(container.getInventory());
            return true;
        }

        BlockData data = block.getBlockData();

        if (block.getType().name().endsWith("_BUTTON") && data instanceof Powerable powerable) {
            powerable.setPowered(true);
            block.setBlockData(data, true);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (block.getType().name().endsWith("_BUTTON") && block.getBlockData() instanceof Powerable currentPower) {
                    currentPower.setPowered(false);
                    block.setBlockData(currentPower, true);
                }
            }, 14L);
            return true;
        }

        if (data instanceof Powerable powerable) {
            powerable.setPowered(!powerable.isPowered());
            block.setBlockData(data, true);
            return true;
        }

        if (data instanceof Openable openable) {
            openable.setOpen(!openable.isOpen());
            block.setBlockData(data, true);
            return true;
        }

        return false;
    }

    private boolean isInteractable(@NotNull Material material) {
        if (lootSenseTarget.test(material)) {
            return true;
        }
        String name = material.name();
        return name.endsWith("_BUTTON")
                || material == Material.LEVER
                || material == Material.REPEATER
                || material == Material.COMPARATOR
                || name.endsWith("_DOOR")
                || name.endsWith("_TRAPDOOR")
                || name.endsWith("_FENCE_GATE");
    }
}

