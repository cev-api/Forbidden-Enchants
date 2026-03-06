package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class FullForceEnchant extends BaseForbiddenEnchant {
    public FullForceEnchant() {
        super("full_force",
                "full_force_level",
                "Full Force",
                ArmorSlot.MACE,
                1,
                NamedTextColor.DARK_RED,
                List.of("fullforce", "full_force"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Grounded mace hits deal 10 damage and launch targets about 8 blocks with smash impact.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }
}

