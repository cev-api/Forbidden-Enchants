package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ForbiddenEnchantDefinition {
    @NotNull String arg();

    @NotNull String pdcKey();

    @NotNull String displayName();

    @NotNull ArmorSlot slot();

    int maxLevel();

    @NotNull NamedTextColor color();

    @NotNull List<String> aliases();

    @NotNull String slotDescription();

    @NotNull String effectDescription(int level);
}

