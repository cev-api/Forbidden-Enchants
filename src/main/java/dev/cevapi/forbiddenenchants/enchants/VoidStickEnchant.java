package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class VoidStickEnchant extends BaseForbiddenEnchant {
    public VoidStickEnchant() {
        super("void_stick", "void_stick_level", "Void Stick", ArmorSlot.ROD, 3,
                NamedTextColor.DARK_AQUA, List.of("void_stick", "voidstick"), null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return switch (level) {
            case 3 -> "Left-click to teleport forward up to 1000 blocks. Item breaks after 5 teleports.";
            case 2 -> "Left-click to teleport forward up to 500 blocks. Item breaks after 5 teleports.";
            default -> "Left-click to teleport forward up to 200 blocks. Item breaks after 5 teleports.";
        };
    }
}
