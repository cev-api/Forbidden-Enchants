package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class TemporalSicknessEnchant extends BaseForbiddenEnchant {
    public TemporalSicknessEnchant() {
        super("temporal_sickness",
                "temporal_sickness_level",
                "Temporal Sickness",
                ArmorSlot.ARMOR,
                3,
                NamedTextColor.DARK_PURPLE,
                List.of("temporal", "temporalsickness", "temporal_sickness", "sickness"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return switch (level) {
                    case 1 -> "Binding curse: every 60-120s warps you between dimensions using equivalent coordinates.";
                    case 2 -> "Binding curse: every 20-120s warps you to random dimension positions within 10k blocks.";
                    default -> "Binding curse: every 30s random dimension warp + 1 heart damage.";
                };
    }

    public boolean isActive(int level) {
        return level > 0;
    }
}

