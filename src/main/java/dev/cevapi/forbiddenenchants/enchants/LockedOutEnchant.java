package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class LockedOutEnchant extends BaseForbiddenEnchant {
    public LockedOutEnchant() {
        super("locked_out",
                "locked_out_level",
                "Locked Out",
                ArmorSlot.BOOTS,
                1,
                NamedTextColor.RED,
                List.of("locked", "lockedout", "locked_out"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Binding curse: blocks interactions with doors/buttons/switches and breaks Nether portal use.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }

    public void ensureBindingCurse(@NotNull ItemStack boots) {
        ItemMeta meta = boots.getItemMeta();
        if (meta != null && !meta.hasEnchant(Enchantment.BINDING_CURSE)) {
            meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            boots.setItemMeta(meta);
        }
    }

    @Override
    public void onPlayerTick(@NotNull org.bukkit.entity.Player player, long tickCounter) {
        ItemStack boots = player.getInventory().getBoots();
        int level = plugin().getEnchantLevel(boots, EnchantType.LOCKED_OUT);
        if (!isActive(level)) {
            return;
        }
        ensureBindingCurse(boots);
    }
}

