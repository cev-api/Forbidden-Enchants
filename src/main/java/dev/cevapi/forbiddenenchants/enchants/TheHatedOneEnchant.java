package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class TheHatedOneEnchant extends BaseForbiddenEnchant {
    public TheHatedOneEnchant() {
        super("the_hated_one",
                "the_hated_one_level",
                "The Hated One",
                ArmorSlot.HELMET,
                2,
                NamedTextColor.DARK_RED,
                List.of("hated", "hatedone", "thehatedone"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return level == 1
                        ? "Binding curse: aggressive mobs and boosted loot (reduced strength)."
                        : "Binding curse: extreme aggro pressure and greatly boosted loot/equipment drops.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }

    public long spawnIntervalTicks(int level) {
        return level >= 2 ? 80L : 160L;
    }

    public boolean shouldSpawnWave(int level, long tickCounter) {
        return isActive(level) && tickCounter % spawnIntervalTicks(level) == 0L;
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        ItemStack helmet = player.getInventory().getHelmet();
        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(helmet, EnchantType.THE_HATED_ONE);
        if (!isActive(level)) {
            return;
        }
        ForbiddenEnchantsPlugin.instance().applyHatedOneAggro(player, level);
        if (shouldSpawnWave(level, tickCounter)) {
            ForbiddenEnchantsPlugin.instance().trySpawnHatedOneWave(player, level);
        }
    }
}

