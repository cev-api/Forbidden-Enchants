package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class MagnetismEnchant extends BaseForbiddenEnchant {
    public MagnetismEnchant() {
        super("magnetism", "magnetism_level", "Magnetism", ArmorSlot.AXE, 4,
                NamedTextColor.AQUA, List.of("magnetism"),
                "Apply to weapons and tools in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "While held, pulls nearby dropped items and XP in a " + radiusForLevel(level) + "-block radius.";
    }

    private int radiusForLevel(int level) {
        return switch (Math.max(1, Math.min(4, level))) {
            case 1 -> 5;
            case 2 -> 10;
            case 3 -> 15;
            default -> 30;
        };
    }
}
