package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class ShieldKnockbackEnchant extends BaseForbiddenEnchant {
    public ShieldKnockbackEnchant() {
        super("shield_knockback",
                "shield_knockback_level",
                "Knockback",
                ArmorSlot.SHIELD,
                1,
                NamedTextColor.AQUA,
                List.of("shieldknockback", "shield_knockback", "knockbackshield", "knockback"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Blocking hits knock attackers back with strong force.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }

    public void apply(@NotNull Player defender, @NotNull Entity damageSource) {
        Entity source = damageSource;
        if (source instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            source = shooter;
        }
        if (!(source instanceof LivingEntity attacker)) {
            return;
        }
        Vector push = attacker.getLocation().toVector().subtract(defender.getLocation().toVector());
        if (push.lengthSquared() < 0.0001D) {
            return;
        }
        attacker.setVelocity(push.normalize().multiply(1.05D).setY(0.42D));
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player defender) || !defender.isBlocking()) {
            return;
        }
        ItemStack shield = getRaisedShield(defender);
        int level = plugin().getEnchantLevel(shield, EnchantType.SHIELD_KNOCKBACK);
        if (!isActive(level)) {
            return;
        }
        apply(defender, event.getDamager());
    }

    private @NotNull ItemStack getRaisedShield(@NotNull Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() == Material.SHIELD) {
            return main;
        }

        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() == Material.SHIELD) {
            return off;
        }

        return new ItemStack(Material.AIR);
    }
}

