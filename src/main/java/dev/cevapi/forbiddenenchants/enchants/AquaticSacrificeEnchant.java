package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class AquaticSacrificeEnchant extends BaseForbiddenEnchant {
    public AquaticSacrificeEnchant() {
        super("aquatic_sacrifice",
                "aquatic_sacrifice_level",
                "Aquatic Sacrifice",
                ArmorSlot.HELMET,
                1,
                NamedTextColor.BLUE,
                List.of("aquatic", "sacrifice", "aquaticsacrifice"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Binding curse: strong underwater buffs, but you drown while out of water.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }

    public double bonusDamage() {
        return 2.0D;
    }

    public void onMeleeHit(@NotNull EntityDamageByEntityEvent event, int level, boolean partiallySubmerged) {
        if (!isActive(level) || !partiallySubmerged) {
            return;
        }
        event.setDamage(event.getDamage() + bonusDamage());
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        ItemStack helmet = player.getInventory().getHelmet();
        int level = plugin().getEnchantLevel(helmet, EnchantType.AQUATIC_SACRIFICE);
        onMeleeHit(event, level, plugin().isPlayerPartiallySubmerged(player));
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        ItemStack helmet = player.getInventory().getHelmet();
        int level = plugin().getEnchantLevel(helmet, EnchantType.AQUATIC_SACRIFICE);
        if (!isActive(level)) {
            return;
        }
        plugin().applyAquaticSacrifice(player, tickCounter);
    }
}

