package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class SilenceEnchant extends BaseForbiddenEnchant {
    public SilenceEnchant() {
        super("silence",
                "silence_level",
                "Silence",
                ArmorSlot.POTION,
                1,
                NamedTextColor.GRAY,
                List.of("silence", "silenced"),
                "Craft by combining this enchanted book with a water bottle in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Drink to silence players within 20 blocks for 15s: no potions, pearls, totems, or enchanting.";
    }
}
