package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class GreedEnchant extends BaseForbiddenEnchant {
    public GreedEnchant() {
        super("greed",
                "greed_level",
                "Greed",
                ArmorSlot.LEGGINGS,
                1,
                NamedTextColor.GOLD,
                List.of("greed"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Binding curse: vacuum items/xp in 30 blocks; cannot drop items.";
    }

    public boolean shouldPullItems(int level) {
        return level > 0;
    }

    public void onLeggingsTick(int level, @NotNull Runnable pullExperience, @NotNull Runnable pullItems) {
        if (!shouldPullItems(level)) {
            return;
        }
        pullExperience.run();
        pullItems.run();
    }

    public boolean preventsDropping(int level) {
        return shouldPullItems(level);
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        ItemStack leggings = player.getInventory().getLeggings();
        int level = plugin().getEnchantLevel(leggings, EnchantType.GREED);
        onLeggingsTick(
                level,
                () -> plugin().pullNearbyExperienceOrbs(player, 30.0D),
                () -> plugin().pullNearbyItems(player, 30.0D)
        );
    }
}

