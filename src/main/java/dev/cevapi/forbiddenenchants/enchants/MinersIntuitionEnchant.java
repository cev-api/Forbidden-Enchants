package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class MinersIntuitionEnchant extends BaseForbiddenEnchant {
    public MinersIntuitionEnchant() {
        super("miners_intuition",
                "miners_intuition_level",
                "Miners Intuition",
                ArmorSlot.HELMET,
                3,
                NamedTextColor.GOLD,
                List.of("miners", "miner", "intuition", "xray"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        int range = switch (level) {
            case 1 -> 10;
            case 2 -> 20;
            default -> 30;
        };
        return "Nearest ore by helmet material within " + range + " blocks.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }

    public int detectionRange(int level) {
        return switch (level) {
            case 1 -> 10;
            case 2 -> 20;
            default -> 30;
        };
    }

    public void onHelmetPulse(@NotNull Player player,
                              @NotNull ItemStack helmet,
                              int level,
                              @NotNull PulseApplier applier) {
        if (!isActive(level)) {
            return;
        }
        applier.apply(player, helmet, level);
    }

    @FunctionalInterface
    public interface PulseApplier {
        void apply(@NotNull Player player, @NotNull ItemStack helmet, int level);
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        if (tickCounter % 10L != 0L) {
            return;
        }
        ItemStack helmet = player.getInventory().getHelmet();
        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(helmet, EnchantType.MINERS_INTUITION);
        if (ForbiddenEnchantsPlugin.instance().hasAnyVisionHelmetEnchant(helmet)) {
            ForbiddenEnchantsPlugin.instance().enforceHelmetRestrictions(player, helmet);
        }
        onHelmetPulse(player, helmet, level, ForbiddenEnchantsPlugin.instance()::applyMinersIntuition);
    }
}

