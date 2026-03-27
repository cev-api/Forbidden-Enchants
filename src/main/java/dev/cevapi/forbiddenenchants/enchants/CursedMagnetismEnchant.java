package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CursedMagnetismEnchant extends BaseForbiddenEnchant {
    public CursedMagnetismEnchant() {
        super("cursed_magnetism", "cursed_magnetism_level", "Cursed Magnetism", ArmorSlot.SWORD, 4,
                NamedTextColor.DARK_PURPLE, List.of("cursed_magnetism", "cursedmagnetism"),
                "Apply to weapons in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "While held, pulls nearby mobs toward you in a " + (level * 10) + "-block radius.";
    }
}
