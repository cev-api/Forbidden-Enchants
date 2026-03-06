package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadLocalRandom;

final class PocketDimensionService {
    boolean trigger(@NotNull Player player) {
        ItemStack leggings = player.getInventory().getLeggings();
        if (leggings == null || leggings.getType() == Material.AIR) {
            return false;
        }
        Location destination = findSafeLocation(player.getLocation(), 50.0D);
        if (destination == null) {
            return false;
        }
        player.teleport(destination);
        player.getWorld().spawnParticle(Particle.PORTAL, destination.clone().add(0.0D, 1.0D, 0.0D), 80, 0.5, 0.8, 0.5, 0.02);
        player.sendActionBar(Component.text("Pocket Dimension triggered!", NamedTextColor.AQUA));
        player.getInventory().setLeggings(new ItemStack(Material.AIR));
        return true;
    }

    @Nullable Location findSafeLocation(@NotNull Location origin, double horizontalDistance) {
        World world = origin.getWorld();
        if (world == null) {
            return null;
        }
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 0; i < 20; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double radius = horizontalDistance + random.nextDouble(-8.0D, 8.0D);
            int x = (int) Math.floor(origin.getX() + Math.cos(angle) * radius);
            int z = (int) Math.floor(origin.getZ() + Math.sin(angle) * radius);
            int y = world.getHighestBlockYAt(x, z) + 1;
            Location candidate = new Location(world, x + 0.5D, y, z + 0.5D, origin.getYaw(), origin.getPitch());
            Block feet = candidate.getBlock();
            Block head = candidate.clone().add(0.0D, 1.0D, 0.0D).getBlock();
            Block floor = candidate.clone().add(0.0D, -1.0D, 0.0D).getBlock();
            if (!feet.isPassable() || !head.isPassable()) {
                continue;
            }
            if (floor.isPassable() || floor.getType() == Material.LAVA || floor.getType() == Material.MAGMA_BLOCK) {
                continue;
            }
            return candidate;
        }
        return null;
    }
}

