package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ExtendedGraspEnchant extends BaseForbiddenEnchant {
    public ExtendedGraspEnchant() {
        super("extended_grasp",
                "extended_grasp_level",
                "Extended Grasp",
                ArmorSlot.CHESTPLATE,
                1,
                NamedTextColor.LIGHT_PURPLE,
                List.of("extended", "grasp"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Sets block/entity reach to 6 blocks.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }
}

