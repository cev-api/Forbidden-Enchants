package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class TheDuplicatorEnchant extends BaseForbiddenEnchant {
    public TheDuplicatorEnchant() {
        super("the_duplicator",
                "the_duplicator_level",
                "The Duplicator",
                ArmorSlot.ARMOR,
                1,
                NamedTextColor.GOLD,
                List.of("duplicator", "the_duplicator", "dupe_book", "dupe"),
                "Apply in an anvil with most items to duplicate the stack (book consumed).");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Anvil utility book: duplicates the left-slot stack. Rejects non-empty shulker boxes and non-empty bundles.";
    }
}
