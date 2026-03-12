package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class OnePlusEnchant extends BaseForbiddenEnchant {
    public OnePlusEnchant() {
        super("one_plus",
                "one_plus_level",
                "One Plus",
                ArmorSlot.POTION,
                4,
                NamedTextColor.GREEN,
                List.of("one_plus", "oneplus", "plusone"),
                "Craft by combining this enchanted book with a water bottle in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Drink to increase all enchant levels on worn/held gear by +1 for " + switch (Math.max(1, Math.min(4, level))) {
            case 1 -> "1 minute";
            case 2 -> "5 minutes";
            case 3 -> "10 minutes";
            default -> "30 minutes";
        } + ".";
    }
}
