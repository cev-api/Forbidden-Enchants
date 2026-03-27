package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class SwapEnchant extends BaseForbiddenEnchant {
    public SwapEnchant() {
        super("swap", "swap_level", "Swap", ArmorSlot.POTION, 1,
                NamedTextColor.LIGHT_PURPLE, List.of("swap"),
                "Craft by combining this enchanted book with a water bottle in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Drink to swap location with nearest player. If no players are online, teleport to spawn.";
    }
}
