package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class LaunchEnchant extends BaseForbiddenEnchant {
    public LaunchEnchant() {
        super("launch",
                "launch_level",
                "Launch",
                ArmorSlot.ELYTRA,
                1,
                NamedTextColor.AQUA,
                List.of("launch"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Double jump on ground gives rocket-like boost; costs 5% Elytra durability.";
    }

    public boolean canDoubleJump(int level, boolean wearingElytra) {
        return level > 0 && wearingElytra;
    }

    public void onDoubleJump(int level, boolean wearingElytra, @NotNull Runnable action) {
        if (!canDoubleJump(level, wearingElytra)) {
            return;
        }
        action.run();
    }

    public @NotNull Vector boostVector(@NotNull Vector facing) {
        return facing.normalize().multiply(1.1D).setY(1.0D);
    }

    @Override
    public void onToggleFlight(@NotNull PlayerToggleFlightEvent event, long tickCounter) {
        Player player = event.getPlayer();
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        ItemStack chest = player.getInventory().getChestplate();
        boolean wearingElytra = chest != null && chest.getType() == Material.ELYTRA;
        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(chest, EnchantType.LAUNCH);
        onDoubleJump(level, wearingElytra, () -> {
            event.setCancelled(true);
            player.setFlying(false);
            player.setAllowFlight(false);
            Vector boost = boostVector(player.getLocation().getDirection());
            player.setVelocity(boost);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1.0F, 1.2F);
            if (chest != null) {
                ForbiddenEnchantsPlugin.instance().damageItemByPercent(player, EquipmentSlot.CHEST, chest, 0.05D);
            }
        });
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        ForbiddenEnchantsPlugin.instance().applyLaunchFlightState(player, player.getInventory().getChestplate());
    }
}

