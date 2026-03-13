package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class DisarmEnchant extends BaseForbiddenEnchant {
    public DisarmEnchant() {
        super("disarm",
                "disarm_level",
                "Disarm",
                ArmorSlot.AXE,
                1,
                NamedTextColor.RED,
                List.of("disarm"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "1 in 20 chance to force weapon drop; unarmed mobs are feared for 3s.";
    }

    public boolean shouldProc(int level) {
        return level > 0 && ThreadLocalRandom.current().nextInt(8) == 0;
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
        int level = plugin().getEnchantLevel(weapon, EnchantType.DISARM);
        if (!plugin().isAxe(weapon) || level <= 0) {
            return;
        }

        onHit(level, () -> {
            if (event.getEntity() instanceof Player targetPlayer) {
                ItemStack main = targetPlayer.getInventory().getItemInMainHand();
                if (main.getType() == Material.AIR) {
                    return;
                }
                targetPlayer.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
                plugin().giveOrDrop(player, main.clone());
                player.sendActionBar(Component.text(
                        plugin().message("enchants.disarm.triggered", "Disarm triggered."),
                        NamedTextColor.RED
                ));
                return;
            }

            if (event.getEntity() instanceof Mob mob) {
                ItemStack main = mob.getEquipment() == null ? null : mob.getEquipment().getItemInMainHand();
                if (main != null && main.getType() != Material.AIR) {
                    if (mob.getEquipment() != null) {
                        mob.getEquipment().setItemInMainHand(new ItemStack(Material.AIR));
                    }
                    plugin().giveOrDrop(player, main.clone());
                    player.sendActionBar(Component.text(
                            plugin().message("enchants.disarm.triggered", "Disarm triggered."),
                            NamedTextColor.RED
                    ));
                    return;
                }
                plugin().applyFear(mob, player, tickCounter + 10L);
                mob.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 0, true, true, true), true);
            }
        });
    }
}

