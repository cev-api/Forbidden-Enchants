package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class LimitlessVisionEnchant extends BaseForbiddenEnchant {
    public LimitlessVisionEnchant() {
        super("limitless_vision",
                "limitless_vision_level",
                "Limitless Vision",
                ArmorSlot.POTION,
                1,
                NamedTextColor.AQUA,
                List.of("limitless_vision", "limitlessvision", "limitless"),
                "Craft by combining this enchanted book with a water bottle in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Drink for permanent night vision plus Divine Vision line-of-sight glow at max range until death.";
    }
}
