package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class BedTimeEnchant extends BaseForbiddenEnchant {
    public BedTimeEnchant() {
        super("bed_time", "bed_time_level", "Bed Time", ArmorSlot.POTION, 1,
                NamedTextColor.BLUE, List.of("bed_time", "bedtime"),
                "Craft by combining this enchanted book with a water bottle in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Drink to teleport to your bed. If no bed is set, teleport to spawn.";
    }
}
