package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class WitheringStrikeEnchant extends BaseForbiddenEnchant {
    public WitheringStrikeEnchant() {
        super("withering_strike",
                "withering_strike_level",
                "Withering Strike",
                ArmorSlot.TRIDENT,
                1,
                NamedTextColor.DARK_GRAY,
                List.of("withering", "wither", "witheringstrike"),
                "Apply to a trident in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "On thrown trident hit applies withering damage every 3 seconds until cured.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }

    public void onTridentHit(int level, @NotNull Runnable effect) {
        if (!isActive(level)) {
            return;
        }
        effect.run();
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (!(event.getDamager() instanceof Trident trident) || !(trident.getShooter() instanceof Player attacker)) {
            return;
        }
        ItemStack thrown = trident.getItem();
        ForbiddenEnchantsPlugin.instance().revealMysteryItemIfNeeded(thrown, null, null);
        trident.setItem(thrown);
        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(thrown, EnchantType.WITHERING_STRIKE);
        onTridentHit(level, () -> {
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 120, 0, true, true, true), true);
            ForbiddenEnchantsPlugin.instance().addWitheringTarget(target.getUniqueId(), attacker.getUniqueId(), tickCounter);
        });
    }
}

