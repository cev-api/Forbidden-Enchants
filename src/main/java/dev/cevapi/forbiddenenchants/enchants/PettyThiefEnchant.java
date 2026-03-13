package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.DisplayNameUtil;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class PettyThiefEnchant extends BaseForbiddenEnchant {
    public PettyThiefEnchant() {
        super("petty_thief",
                "petty_thief_level",
                "Petty Thief",
                ArmorSlot.HOE,
                1,
                NamedTextColor.GOLD,
                List.of("petty", "thief", "pettythief"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "1 in 10 chance to steal one random item in PvP, or pull a mob-drop themed item.";
    }

    public boolean shouldProc(int level) {
        return level > 0 && ThreadLocalRandom.current().nextInt(10) == 0;
    }

    public void onHit(int level, @NotNull Runnable action) {
        if (!shouldProc(level)) {
            return;
        }
        action.run();
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        ItemStack weapon = player.getInventory().getItemInMainHand();
        int level = plugin().getEnchantLevel(weapon, EnchantType.PETTY_THIEF);
        if (!plugin().isHoe(weapon) || level <= 0) {
            return;
        }
        onHit(level, () -> {
            if (event.getEntity() instanceof Player victim) {
                ItemStack stolen = plugin().stealRandomInventoryItem(victim);
                if (stolen == null) {
                    return;
                }
                plugin().giveOrDrop(player, stolen);
                player.sendActionBar(Component.text(
                        plugin().message(
                                "petty_thief.stole_attacker",
                                "Petty Thief stole {item}.",
                                Map.of("item", plugin().describeItem(stolen))
                        ),
                        NamedTextColor.GOLD
                ));
                victim.sendActionBar(Component.text(
                        plugin().message(
                                "petty_thief.stole_victim",
                                "An item was stolen from your inventory!"
                        ),
                        NamedTextColor.RED
                ));
                return;
            }
            if (event.getEntity() instanceof LivingEntity living) {
                ItemStack stolen = plugin().randomMobLootPreview(living);
                plugin().giveOrDrop(player, stolen);
                player.sendActionBar(Component.text(
                        plugin().message(
                                "petty_thief.pulled_from_mob",
                                "Petty Thief pulled {item} from {mob}.",
                                Map.of(
                                        "item", plugin().describeItem(stolen),
                                        "mob", DisplayNameUtil.toDisplayName(living.getType())
                                )
                        ),
                        NamedTextColor.GOLD
                ));
            }
        });
    }
}

