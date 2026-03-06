package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class LumberjackEnchant extends BaseForbiddenEnchant {
    public LumberjackEnchant() {
        super("lumberjack",
                "lumberjack_level",
                "Lumberjack",
                ArmorSlot.AXE,
                1,
                NamedTextColor.GREEN,
                List.of("lumber", "jack", "lumberjack"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Break the bottom log to fell the entire connected tree above ground.";
    }

    public boolean shouldFellTree(int level) {
        return level > 0;
    }

    public boolean shouldBreakWholeTree(int level, boolean axe, boolean bottomTreeLog) {
        return shouldFellTree(level) && axe && bottomTreeLog;
    }
}

