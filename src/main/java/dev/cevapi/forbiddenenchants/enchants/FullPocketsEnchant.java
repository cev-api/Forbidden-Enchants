package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class FullPocketsEnchant extends BaseForbiddenEnchant {
    public FullPocketsEnchant() {
        super("full_pockets",
                "full_pockets_level",
                "Full Pockets",
                ArmorSlot.LEGGINGS,
                4,
                NamedTextColor.YELLOW,
                List.of("pockets", "fullpockets", "full_pockets"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Container opens: +" + (level * 10) + "% rare-loot chance and XP orbs in 30 blocks pull to you.";
    }

    public boolean shouldPullExperience(int level) {
        return level > 0;
    }

    public void onLeggingsTick(int level, @NotNull Runnable pullExperience) {
        if (!shouldPullExperience(level)) {
            return;
        }
        pullExperience.run();
    }

    public int rareLootBonusPercent(int level) {
        return Math.max(0, level) * 10;
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        ItemStack leggings = player.getInventory().getLeggings();
        int level = plugin().getEnchantLevel(leggings, EnchantType.FULL_POCKETS);
        onLeggingsTick(level, () -> plugin().pullNearbyExperienceOrbs(player, 30.0D));
    }
}

