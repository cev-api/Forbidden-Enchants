package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class LifeSpiritEnchant extends BaseForbiddenEnchant {
    public LifeSpiritEnchant() {
        super("life_spirit", "life_spirit_level", "Life Spirit", ArmorSlot.POTION, 3,
                NamedTextColor.GREEN, List.of("life_spirit", "lifespirit"),
                "Craft by combining this enchanted book with a water bottle in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Drink to gain +" + level + " heart" + (level == 1 ? "" : "s") + ". One gained heart is removed each time you die.";
    }
}
