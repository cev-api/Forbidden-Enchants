package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class LootSenseEnchant extends BaseForbiddenEnchant {
    public LootSenseEnchant() {
        super("loot_sense",
                "loot_sense_level",
                "Loot Sense",
                ArmorSlot.HELMET,
                3,
                NamedTextColor.YELLOW,
                List.of("loot"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Points to chests, barrels and shulkers within " + switch (level) {
                    case 1 -> 20;
                    case 2 -> 30;
                    default -> 50;
                } + " blocks.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }

    public int detectionRange(int level) {
        return switch (level) {
            case 1 -> 20;
            case 2 -> 30;
            default -> 50;
        };
    }

    public void onHelmetPulse(@NotNull Player player, int level, @NotNull PulseApplier applier) {
        if (!isActive(level)) {
            return;
        }
        applier.apply(player, level);
    }

    @FunctionalInterface
    public interface PulseApplier {
        void apply(@NotNull Player player, int level);
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        if (tickCounter % 10L != 0L) {
            return;
        }
        ItemStack helmet = player.getInventory().getHelmet();
        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(helmet, EnchantType.LOOT_SENSE);
        if (ForbiddenEnchantsPlugin.instance().hasAnyVisionHelmetEnchant(helmet)) {
            ForbiddenEnchantsPlugin.instance().enforceHelmetRestrictions(player, helmet);
        }
        onHelmetPulse(player, level, ForbiddenEnchantsPlugin.instance()::applyLootSense);
    }
}

