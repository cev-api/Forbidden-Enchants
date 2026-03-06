package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class GetOverHereEnchant extends BaseForbiddenEnchant {
    public GetOverHereEnchant() {
        super("get_over_here",
                "get_over_here_level",
                "Get Over Here!",
                ArmorSlot.LEAD,
                1,
                NamedTextColor.GOLD,
                List.of("getoverhere", "get_over_here", "villagerlead", "villager_lead"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Allows enchanted lead to leash villagers.";
    }

    public boolean canLeashVillagers(int level) {
        return level > 0;
    }
}

