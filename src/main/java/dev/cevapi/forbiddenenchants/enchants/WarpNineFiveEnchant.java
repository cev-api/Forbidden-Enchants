package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class WarpNineFiveEnchant extends BaseForbiddenEnchant {
    private static final double MAX_WARP_DISTANCE = 3333.0D;

    private final Map<UUID, Location> lastLocation = new HashMap<>();
    private final Map<UUID, Double> traveledDistance = new HashMap<>();
    private final Map<UUID, Double> durabilityStepDistance = new HashMap<>();

    public WarpNineFiveEnchant() {
        super("warp_9_5",
                "warp_9_5_level",
                "Warp 9.5",
                ArmorSlot.BOOTS,
                1,
                NamedTextColor.BLUE,
                List.of("warp", "warp95", "warp_9_5"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "On bedrock: extreme speed while boots steadily wear down over 3333 blocks of travel.";
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        UUID id = player.getUniqueId();
        ItemStack boots = player.getInventory().getBoots();
        if (ForbiddenEnchantsPlugin.instance().getEnchantLevel(boots, EnchantType.WARP_NINE_FIVE) <= 0) {
            clearState(id);
            return;
        }
        if (!isOnBedrock(player)) {
            lastLocation.put(id, player.getLocation().clone());
            return;
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 14, 16, true, false, true), true);

        Location current = player.getLocation();
        Location previous = lastLocation.get(id);
        if (previous == null || !previous.getWorld().equals(current.getWorld())) {
            lastLocation.put(id, current.clone());
            return;
        }
        double moved = previous.distance(current);
        if (moved <= 0.001D) {
            return;
        }
        lastLocation.put(id, current.clone());

        double total = traveledDistance.getOrDefault(id, 0.0D) + moved;
        traveledDistance.put(id, total);

        double stepDistance = durabilityStepDistance.getOrDefault(id, 0.0D) + moved;
        int steps = (int) Math.floor(stepDistance / 50.0D);
        if (steps > 0) {
            ForbiddenEnchantsPlugin.instance().damageArmorByPercent(player, EquipmentSlot.FEET, boots, 0.015D * steps);
            stepDistance -= steps * 50.0D;
        }
        durabilityStepDistance.put(id, stepDistance);

        if (total >= MAX_WARP_DISTANCE) {
            ItemStack currentBoots = player.getInventory().getBoots();
            if (currentBoots != null && currentBoots.getType() != Material.AIR) {
                player.getInventory().setBoots(null);
                player.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, player.getLocation(), 120, 0.7, 0.4, 0.7, 0.0);
                player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.1F, 0.7F);
            }
            clearState(id);
        }
    }

    private boolean isOnBedrock(@NotNull Player player) {
        Location under = player.getLocation().clone().subtract(0.0D, 0.1D, 0.0D);
        return under.getBlock().getType() == Material.BEDROCK;
    }

    private void clearState(@NotNull UUID id) {
        lastLocation.remove(id);
        traveledDistance.remove(id);
        durabilityStepDistance.remove(id);
    }
}
