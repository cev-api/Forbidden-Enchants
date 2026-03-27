package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class TrueSilenceEnchant extends BaseForbiddenEnchant {
    public TrueSilenceEnchant() {
        super("true_silence", "true_silence_level", "True Silence", ArmorSlot.BOOTS, 1,
                NamedTextColor.GRAY, List.of("true_silence", "truesilence"), null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "You make no sound and mobs ignore you unless they are within 2 blocks.";
    }
}
