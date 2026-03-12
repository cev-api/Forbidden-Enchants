package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class TemporalDisplacementEnchant extends BaseForbiddenEnchant {
    public TemporalDisplacementEnchant() {
        super("temporal_displacement",
                "temporal_displacement_level",
                "Temporal Displacement",
                ArmorSlot.POTION,
                3,
                NamedTextColor.DARK_AQUA,
                List.of("temporal_displacement", "temporaldisplacement", "time_slow"),
                "Craft by combining this enchanted book with a water bottle in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        int clamped = Math.max(1, Math.min(3, level));
        String duration = switch (clamped) {
            case 1 -> "10s";
            case 2 -> "30s";
            default -> "1 minute";
        };
        return "Drink to heavily slow all players and mobs within 20 blocks for " + duration + ".";
    }
}
