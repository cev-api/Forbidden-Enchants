package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class VoidGraspEnchant extends BaseForbiddenEnchant {
    public VoidGraspEnchant() {
        super("void_grasp",
                "void_grasp_level",
                "Void Grasp",
                ArmorSlot.CHESTPLATE,
                1,
                NamedTextColor.DARK_PURPLE,
                List.of("voidgrasp"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "6 block reach plus through-wall interactions and attacks.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }
}

