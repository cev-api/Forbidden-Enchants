package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class FireballEnchant extends BaseForbiddenEnchant {
    public FireballEnchant() {
        super("fireball", "fireball_level", "Fireball", ArmorSlot.ROD, 2,
                NamedTextColor.GOLD, List.of("fireball"), null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return level >= 2
                ? "Left-click to launch a fireball. Item breaks after 100 casts."
                : "Left-click to launch a fireball. Item breaks after 50 casts.";
    }
}
