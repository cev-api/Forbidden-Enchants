package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class LavaStepEnchant extends BaseForbiddenEnchant {
    public LavaStepEnchant() {
        super("lava_step", "lava_step_level", "Lava Step", ArmorSlot.BOOTS, 1,
                NamedTextColor.RED, List.of("lava_step", "lavastep"), null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Walk over lava safely with fire resistance. Boots break after 1000 blocks traveled.";
    }
}
