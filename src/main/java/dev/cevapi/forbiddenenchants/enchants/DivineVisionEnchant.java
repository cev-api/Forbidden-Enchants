package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class DivineVisionEnchant extends BaseForbiddenEnchant {
    public DivineVisionEnchant() {
        super("divine_vision",
                "divine_vision_level",
                "Divine Vision",
                ArmorSlot.HELMET,
                4,
                NamedTextColor.AQUA,
                List.of("divine", "vision"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        int range = switch (level) {
            case 1 -> 10;
            case 2 -> 20;
            case 3 -> 30;
            default -> 50;
        };
        if (level >= 4) {
            return "Glow vision through walls within " + range + " blocks + permanent night vision while worn.";
        }
        return "Glow vision through walls within " + range + " blocks.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }

    public int visionRange(int level) {
        return switch (level) {
            case 1 -> 10;
            case 2 -> 20;
            case 3 -> 30;
            default -> 50;
        };
    }

    public void onHelmetTick(@NotNull Player player, int level, @NotNull VisionApplier applier) {
        if (!isActive(level)) {
            return;
        }
        applier.apply(player, level);
    }

    @FunctionalInterface
    public interface VisionApplier {
        void apply(@NotNull Player player, int level);
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        ItemStack helmet = player.getInventory().getHelmet();
        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(helmet, EnchantType.DIVINE_VISION);
        if (!isActive(level)) {
            return;
        }
        if (level >= 4) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 220, 0, true, false, false), true);
        }
        if (ForbiddenEnchantsPlugin.instance().hasAnyVisionHelmetEnchant(helmet)) {
            ForbiddenEnchantsPlugin.instance().enforceHelmetRestrictions(player, helmet);
        }
        onHelmetTick(player, level, ForbiddenEnchantsPlugin.instance()::applyDivineVision);
    }
}

