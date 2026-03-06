package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CharmedPetEnchant extends BaseForbiddenEnchant {
    public CharmedPetEnchant() {
        super("charmed_pet",
                "charmed_pet_level",
                "Charmed Pet",
                ArmorSlot.NAMETAG,
                1,
                NamedTextColor.LIGHT_PURPLE,
                List.of("charmedpet", "charmed_pet", "charmed"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Name-tagged mobs become passive pets that follow you; right-click to sit/stay.";
    }

    public boolean canCharm(int level) {
        return level > 0;
    }
}

