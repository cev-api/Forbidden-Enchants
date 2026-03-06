package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class MiasmaFormEnchant extends BaseForbiddenEnchant {
    public MiasmaFormEnchant() {
        super("miasma_form",
                "miasma_form_level",
                "Miasma Form",
                ArmorSlot.CHESTPLATE,
                1,
                NamedTextColor.GRAY,
                List.of("miasmaform", "miasma_form", "smokeform"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Sneak for smoke form: phase through 1-block walls for 2 damage; non-fire damage immunity.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }

    @Override
    public void onDamage(@NotNull EntityDamageEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!ForbiddenEnchantsPlugin.instance().hasMiasmaForm(player)) {
            return;
        }
        if (ForbiddenEnchantsPlugin.instance().isAllowedMiasmaFormDamage(event)) {
            return;
        }
        event.setCancelled(true);
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        ItemStack chestplate = player.getInventory().getChestplate();
        if (player.isSneaking() && ForbiddenEnchantsPlugin.instance().hasMiasmaEnchantEquipped(player)) {
            ForbiddenEnchantsPlugin.instance().applyMiasmaForm(player, tickCounter);
            ForbiddenEnchantsPlugin.instance().setPlayerReach(player, 4.5D, 3.0D);
            return;
        }

        ForbiddenEnchantsPlugin.instance().clearMiasmaVisual(player);

        int voidGraspLevel = ForbiddenEnchantsPlugin.instance().getEnchantLevel(chestplate, EnchantType.VOID_GRASP);
        int extendedGraspLevel = ForbiddenEnchantsPlugin.instance().getEnchantLevel(chestplate, EnchantType.EXTENDED_GRASP);
        if (EnchantList.INSTANCE.voidGrasp().isActive(voidGraspLevel) || EnchantList.INSTANCE.extendedGrasp().isActive(extendedGraspLevel)) {
            ForbiddenEnchantsPlugin.instance().setPlayerReach(player, 6.0D, 6.0D);
            return;
        }
        ForbiddenEnchantsPlugin.instance().setPlayerReach(player, 4.5D, 3.0D);
    }
}

