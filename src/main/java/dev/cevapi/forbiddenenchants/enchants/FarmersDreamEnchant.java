package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class FarmersDreamEnchant extends BaseForbiddenEnchant {
    public FarmersDreamEnchant() {
        super("farmers_dream", "farmers_dream_level", "Farmer's Dream", ArmorSlot.BOOTS, 1,
                NamedTextColor.GREEN, List.of("farmers_dream", "farmersdream"), null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Walking over crops instantly grows them. Boots break after 500 blocks traveled.";
    }
}
