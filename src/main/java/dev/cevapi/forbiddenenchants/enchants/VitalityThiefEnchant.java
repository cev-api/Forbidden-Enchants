package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class VitalityThiefEnchant extends BaseForbiddenEnchant {
    public VitalityThiefEnchant() {
        super("vitality_thief", "vitality_thief_level", "Vitality Thief", ArmorSlot.POTION, 1,
                NamedTextColor.RED, List.of("vitality_thief", "vitalitythief"),
                "Craft by combining this enchanted book with a water bottle in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Drink to swap health and vitality with nearest player. If none, kill nearby mobs to gain golden hearts.";
    }
}
