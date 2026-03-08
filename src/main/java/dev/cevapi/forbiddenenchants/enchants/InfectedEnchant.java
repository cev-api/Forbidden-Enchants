package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class InfectedEnchant extends BaseForbiddenEnchant {
    public InfectedEnchant() {
        super("infected",
                "infected_level",
                "Infected",
                ArmorSlot.POTION,
                1,
                NamedTextColor.DARK_GREEN,
                List.of("infected", "infection"),
                "Craft by combining this enchanted book with a water bottle in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Drink to convert nearby mobs/villagers (5 blocks) into zombies for 30s, then revert.";
    }
}
