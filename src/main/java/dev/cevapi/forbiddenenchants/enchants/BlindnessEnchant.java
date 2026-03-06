package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class BlindnessEnchant extends BaseForbiddenEnchant {
    public BlindnessEnchant() {
        super("blindness",
                "blindness_level",
                "Blindness",
                ArmorSlot.SWORD,
                1,
                NamedTextColor.AQUA,
                List.of("blindness", "blind", "insight", "divineinsight", "divine_insight"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "On player hit, 33% chance to inflict blindness for 1.0-5.0 seconds.";
    }

    public boolean shouldApply(int level, double chance) {
        if (level <= 0) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble() < chance;
    }

    public void onSwordHit(@NotNull Player target, int level, double chance, @NotNull EffectFactory effectFactory) {
        if (!shouldApply(level, chance)) {
            return;
        }
        target.addPotionEffect(effectFactory.create(), true);
    }

    @FunctionalInterface
    public interface EffectFactory {
        @NotNull PotionEffect create();
    }
}

