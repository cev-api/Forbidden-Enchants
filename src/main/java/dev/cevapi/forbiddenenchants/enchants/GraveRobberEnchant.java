package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class GraveRobberEnchant extends BaseForbiddenEnchant {
    public GraveRobberEnchant() {
        super("grave_robber",
                "grave_robber_level",
                "Grave Robber",
                ArmorSlot.COMPASS,
                4,
                NamedTextColor.GRAY,
                List.of("grave", "graverobber", "grave_robber", "graverob"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Compass points to nearest death location within " + switch (level) {
                    case 1 -> 1000;
                    case 2 -> 2500;
                    case 3 -> 5000;
                    default -> 10000;
                } + " blocks.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }

    @Override
    public void onInteract(@NotNull PlayerInteractEvent event, long tickCounter) {
        if (event.getAction() == Action.PHYSICAL || event.getHand() == null) {
            return;
        }
        EquipmentSlot hand = event.getHand();
        ItemStack held = hand == EquipmentSlot.OFF_HAND
                ? event.getPlayer().getInventory().getItemInOffHand()
                : event.getPlayer().getInventory().getItemInMainHand();
        int level = plugin().getEnchantLevel(held, EnchantType.GRAVE_ROBBER);
        if (!isActive(level)) {
            return;
        }
        plugin().revealMysteryItemIfNeeded(held, event.getPlayer(), hand);
    }
}

