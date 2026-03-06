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

public final class PocketSeekerEnchant extends BaseForbiddenEnchant {
    public PocketSeekerEnchant() {
        super("pocket_seeker",
                "pocket_seeker_level",
                "Pocket Seeker",
                ArmorSlot.COMPASS,
                4,
                NamedTextColor.YELLOW,
                List.of("pocketseeker", "pocket_seeker", "seekercompass"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Compass points to nearest living player within " + switch (level) {
                    case 1 -> 1000;
                    case 2 -> 2500;
                    case 3 -> 5000;
                    default -> 10000;
                } + " blocks; spins wildly if none.";
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
        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(held, EnchantType.POCKET_SEEKER);
        if (!isActive(level)) {
            return;
        }
        ForbiddenEnchantsPlugin.instance().revealMysteryItemIfNeeded(held, event.getPlayer(), hand);
    }
}

