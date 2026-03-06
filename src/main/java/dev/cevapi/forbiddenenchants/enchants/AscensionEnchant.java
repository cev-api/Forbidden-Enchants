package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class AscensionEnchant extends BaseForbiddenEnchant {
    public AscensionEnchant() {
        super("ascension",
                "ascension_level",
                "Ascension",
                ArmorSlot.BOOTS,
                1,
                NamedTextColor.WHITE,
                List.of(),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Hold jump while airborne to jet upward (very weak durability).";
    }

    public boolean shouldApply(int ascensionLevel, int masqueradeLevel) {
        return ascensionLevel > 0 && masqueradeLevel <= 0;
    }

    public void onBootTick(@NotNull Player player, int ascensionLevel, int masqueradeLevel, @NotNull ApplyAction action) {
        if (!shouldApply(ascensionLevel, masqueradeLevel)) {
            return;
        }
        action.run(player);
    }

    @FunctionalInterface
    public interface ApplyAction {
        void run(@NotNull Player player);
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        ItemStack boots = player.getInventory().getBoots();
        int ascensionLevel = ForbiddenEnchantsPlugin.instance().getEnchantLevel(boots, EnchantType.ASCENSION);
        int masqueradeLevel = ForbiddenEnchantsPlugin.instance().getEnchantLevel(boots, EnchantType.MASQUERADE);
        onBootTick(player, ascensionLevel, masqueradeLevel, ForbiddenEnchantsPlugin.instance()::applyAscension);
    }
}

