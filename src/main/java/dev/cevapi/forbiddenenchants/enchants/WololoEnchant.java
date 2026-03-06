package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class WololoEnchant extends BaseForbiddenEnchant {
    public WololoEnchant() {
        super("wololo",
                "wololo_level",
                "Wololo",
                ArmorSlot.HELMET,
                1,
                NamedTextColor.BLUE,
                List.of("wololo"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Binding curse: sheep within 3 blocks recolor once; each conversion costs you 1 heart.";
    }

    public boolean shouldPulse(int level, long tickCounter) {
        return level > 0 && tickCounter % 10L == 0L;
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
        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(helmet, EnchantType.WOLOLO);
        onHelmetPulse(player, level, tickCounter, ForbiddenEnchantsPlugin.instance()::applyWololo);
    }
}

