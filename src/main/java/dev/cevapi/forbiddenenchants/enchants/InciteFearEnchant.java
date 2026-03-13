package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class InciteFearEnchant extends BaseForbiddenEnchant {
    public InciteFearEnchant() {
        super("incite_fear",
                "incite_fear_level",
                "Incite Fear",
                ArmorSlot.SWORD,
                2,
                NamedTextColor.RED,
                List.of("fear", "incite", "incitefear"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "On sword hit, nearby mobs have a " + (level == 1 ? "25%" : "50%") + " chance to flee.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }

    public double fearChance(int level) {
        return level >= 2 ? 0.50D : 0.25D;
    }

    public void onSwordHit(int level, @NotNull Iterable<Entity> nearbyEntities, @NotNull FearConsumer consumer) {
        if (!isActive(level)) {
            return;
        }
        double chance = fearChance(level);
        for (Entity nearby : nearbyEntities) {
            if (!(nearby instanceof Mob mob) || !mob.isValid() || mob.isDead()) {
                continue;
            }
            if (ThreadLocalRandom.current().nextDouble() <= chance) {
                consumer.apply(mob);
            }
        }
    }

    @FunctionalInterface
    public interface FearConsumer {
        void apply(@NotNull Mob mob);
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!plugin().isSword(weapon)) {
            return;
        }

        plugin().revealMysteryItemIfNeeded(weapon, player, EquipmentSlot.HAND);

        int fearLevel = plugin().getEnchantLevel(weapon, EnchantType.INCITE_FEAR);
        int blindnessLevel = plugin().getEnchantLevel(weapon, EnchantType.BLINDNESS);
        if (fearLevel <= 0 && blindnessLevel <= 0) {
            return;
        }

        onSwordHit(
                fearLevel,
                player.getNearbyEntities(10.0D, 10.0D, 10.0D),
                mob -> plugin().applyFear(mob, player, tickCounter)
        );

        if (event.getEntity() instanceof Player target) {
            EnchantList.INSTANCE.blindness().onSwordHit(
                    target,
                    blindnessLevel,
                    0.33D,
                    () -> new PotionEffect(PotionEffectType.BLINDNESS, ThreadLocalRandom.current().nextInt(20, 101), 0, true, true, true)
            );
        }
    }
}

