package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ThePhilosophersBookEnchant extends BaseForbiddenEnchant {
    public ThePhilosophersBookEnchant() {
        super("the_philosophers_book",
                "the_philosophers_book_level",
                "The Philosophers Book",
                ArmorSlot.ARMOR,
                3,
                NamedTextColor.YELLOW,
                List.of("philosophers_book", "the_philosophers_book", "philosopher", "alchemy_book"),
                "Apply in an anvil for alchemical conversion (book consumed).");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return switch (Math.max(1, Math.min(3, level))) {
            case 1 -> "I: iron ingots/blocks -> gold ingots/blocks (same amount).";
            case 2 -> "II: gold ingots/blocks -> diamonds/diamond blocks (same amount).";
            default -> "III: diamonds/diamond blocks -> netherite ingots/blocks (same amount).";
        };
    }
}
