package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class NoFallEnchant extends BaseForbiddenEnchant {
    public NoFallEnchant() {
        super("no_fall",
                "no_fall_level",
                "No Fall",
                ArmorSlot.BOOTS,
                1,
                NamedTextColor.WHITE,
                List.of("nofall", "no_fall"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Negates fall damage. If a fall would kill you, consume 25% boot durability instead.";
    }

    @Override
    public void onDamage(@NotNull EntityDamageEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL) {
            return;
        }

        ItemStack boots = player.getInventory().getBoots();
        if (ForbiddenEnchantsPlugin.instance().getEnchantLevel(boots, EnchantType.NO_FALL) <= 0) {
            return;
        }

        double postDamageHealth = player.getHealth() - event.getFinalDamage();
        if (postDamageHealth <= 0.0D && boots != null) {
            ForbiddenEnchantsPlugin.instance().damageArmorByPercent(player, EquipmentSlot.FEET, boots, 0.25D);
        }
        event.setCancelled(true);
    }
}
