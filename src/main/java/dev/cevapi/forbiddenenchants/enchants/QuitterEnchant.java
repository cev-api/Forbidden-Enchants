package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class QuitterEnchant extends BaseForbiddenEnchant {
    public QuitterEnchant() {
        super("quitter",
                "quitter_level",
                "Quitter",
                ArmorSlot.POTION,
                1,
                NamedTextColor.DARK_GRAY,
                List.of("quitter", "quit"),
                "Craft by combining this enchanted book with a water bottle in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Drink to fake-leave for 30s: removed from tab list, cannot chat, then fake-rejoin.";
    }
}
