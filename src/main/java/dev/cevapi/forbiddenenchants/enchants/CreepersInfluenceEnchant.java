package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CreepersInfluenceEnchant extends BaseForbiddenEnchant {
    public CreepersInfluenceEnchant() {
        super("creepers_influence",
                "creepers_influence_level",
                "Creepers Influence",
                ArmorSlot.HELMET,
                1,
                NamedTextColor.DARK_GREEN,
                List.of("creeper", "creepers", "creepersinfluence", "creepers_influence"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Nearby creepers target and explode on other mobs; if none exist, they self-detonate.";
    }

    public boolean shouldPulse(int level, long tickCounter) {
        return level > 0 && tickCounter % 20L == 0L;
    }

    public void onHelmetPulse(@NotNull Player player, int level, long tickCounter, @NotNull PulseAction action) {
        if (!shouldPulse(level, tickCounter)) {
            return;
        }
        action.run(player);
    }

    @FunctionalInterface
    public interface PulseAction {
        void run(@NotNull Player player);
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        ItemStack helmet = player.getInventory().getHelmet();
        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(helmet, EnchantType.CREEPERS_INFLUENCE);
        onHelmetPulse(player, level, tickCounter, ForbiddenEnchantsPlugin.instance()::applyCreepersInfluence);
    }
}

