package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ForbiddenAgilityEnchant extends BaseForbiddenEnchant {
    private final Map<UUID, Double> baseMoveSpeedByPlayer = new HashMap<>();

    public ForbiddenAgilityEnchant() {
        super("forbidden_agility",
                "forbidden_agility_level",
                "Forbidden Agility",
                ArmorSlot.BOOTS,
                4,
                NamedTextColor.GREEN,
                List.of("agility", "forbidden_agility", "forbiddenagility"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Increases movement speed by " + String.format(Locale.ROOT, "%.3f", 0.006D * level) + ".";
    }

    public void applyTo(@NotNull Player player, int level, double perLevelBonus) {
        UUID id = player.getUniqueId();
        AttributeInstance moveSpeed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (moveSpeed == null) {
            return;
        }

        baseMoveSpeedByPlayer.putIfAbsent(id, moveSpeed.getBaseValue());
        double base = baseMoveSpeedByPlayer.getOrDefault(id, moveSpeed.getBaseValue());
        double target = Math.max(0.01D, base + (perLevelBonus * level));
        if (Math.abs(moveSpeed.getBaseValue() - target) > 0.0001D) {
            moveSpeed.setBaseValue(target);
        }
    }

    public void clearFor(@NotNull Player player) {
        Double base = baseMoveSpeedByPlayer.remove(player.getUniqueId());
        if (base == null) {
            return;
        }
        AttributeInstance moveSpeed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        if (moveSpeed != null) {
            moveSpeed.setBaseValue(Math.max(0.01D, base));
        }
    }

    public void resetAll(@NotNull Collection<? extends Player> onlinePlayers) {
        for (Player player : onlinePlayers) {
            clearFor(player);
        }
        baseMoveSpeedByPlayer.clear();
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        if (ForbiddenEnchantsPlugin.instance().isMasquerading(player)) {
            clearFor(player);
            return;
        }
        ItemStack boots = player.getInventory().getBoots();
        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(boots, EnchantType.FORBIDDEN_AGILITY);
        if (level > 0) {
            applyTo(player, level, 0.006D);
            return;
        }
        clearFor(player);
    }
}

