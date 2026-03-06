package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TheUnyieldingEnchant extends BaseForbiddenEnchant {
    private final Map<UUID, Double> baseKnockbackResistanceByPlayer = new HashMap<>();

    public TheUnyieldingEnchant() {
        super("the_unyielding",
                "the_unyielding_level",
                "The Unyielding",
                ArmorSlot.CHESTPLATE,
                1,
                NamedTextColor.WHITE,
                List.of("unyielding", "the_unyielding"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Prevents pushback from damage, explosions, wind bursts and flowing water.";
    }

    public void applyTo(@NotNull Player player) {
        AttributeInstance resistance = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (resistance == null) {
            return;
        }

        UUID id = player.getUniqueId();
        baseKnockbackResistanceByPlayer.putIfAbsent(id, resistance.getBaseValue());
        if (Math.abs(resistance.getBaseValue() - 1.0D) > 0.0001D) {
            resistance.setBaseValue(1.0D);
        }
    }

    public void clearFor(@NotNull Player player) {
        Double base = baseKnockbackResistanceByPlayer.remove(player.getUniqueId());
        if (base == null) {
            return;
        }
        AttributeInstance resistance = player.getAttribute(Attribute.KNOCKBACK_RESISTANCE);
        if (resistance != null) {
            resistance.setBaseValue(base);
        }
    }

    public void resetAll(@NotNull Collection<? extends Player> onlinePlayers) {
        for (Player player : onlinePlayers) {
            clearFor(player);
        }
        baseKnockbackResistanceByPlayer.clear();
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        if (ForbiddenEnchantsPlugin.instance().hasUnyieldingEquipped(player)) {
            applyTo(player);
            return;
        }
        clearFor(player);
    }
}

