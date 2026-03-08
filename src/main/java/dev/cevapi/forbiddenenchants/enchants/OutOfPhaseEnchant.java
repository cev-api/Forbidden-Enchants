package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class OutOfPhaseEnchant extends BaseForbiddenEnchant {
    public OutOfPhaseEnchant() {
        super("out_of_phase",
                "out_of_phase_level",
                "Out Of Phase",
                ArmorSlot.POTION,
                1,
                NamedTextColor.AQUA,
                List.of("out_of_phase", "outofphase", "phase"),
                "Craft by combining this enchanted book with a water bottle in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Drink to phase through entities and pass through 1-block-thick walls for 1 minute.";
    }
}
