package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class LifeStealEnchant extends BaseForbiddenEnchant {
    public LifeStealEnchant() {
        super("life_steal", "life_steal_level", "Life Steal", ArmorSlot.POTION, 3,
                NamedTextColor.DARK_RED, List.of("life_steal", "lifesteal"),
                "Craft by combining this enchanted book with a water bottle in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Drink to steal " + level + " heart" + (level == 1 ? "" : "s")
                + " from nearest player and add it to yourself until death.";
    }
}
