package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class MinersVoidStepEnchant extends BaseForbiddenEnchant {
    public MinersVoidStepEnchant() {
        super("miners_void_step", "miners_void_step_level", "Miners Void Step", ArmorSlot.POTION, 1,
                NamedTextColor.AQUA, List.of("miners_void_step", "minersvoidstep", "void_step"),
                "Craft by combining this enchanted book with a water bottle in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Drink to teleport to surface if underground, or to nearest safe low cave if above ground.";
    }
}
