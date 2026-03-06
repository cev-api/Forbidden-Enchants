package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProudWarriorEnchant extends BaseForbiddenEnchant {
    private final Map<UUID, Long> immunityUntilTick = new HashMap<>();

    public ProudWarriorEnchant() {
        super("proud_warrior",
                "proud_warrior_level",
                "Proud Warrior",
                ArmorSlot.CHESTPLATE,
                1,
                NamedTextColor.DARK_RED,
                List.of("proud", "warrior", "proud_warrior"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "First hit triggers a heavy blast, smoke burst, leaves you at one heart, and breaks the chestplate.";
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!isValidAttacker(event.getDamager())) {
            return;
        }

        ItemStack chestplate = player.getInventory().getChestplate();
        if (ForbiddenEnchantsPlugin.instance().getEnchantLevel(chestplate, EnchantType.PROUD_WARRIOR) <= 0) {
            return;
        }

        event.setCancelled(true);
        immunityUntilTick.put(player.getUniqueId(), tickCounter + 30L);
        player.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, player.getLocation(), 260, 1.6, 1.0, 1.6, 0.01);
        player.getWorld().spawnParticle(Particle.LARGE_SMOKE, player.getLocation(), 140, 1.4, 0.7, 1.4, 0.01);
        player.getWorld().createExplosion(player, player.getLocation(), 4.5F, false, true);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2F, 0.75F);

        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.getInventory().setChestplate(null);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.65F);
            player.setNoDamageTicks(30);
            Bukkit.getScheduler().runTask(ForbiddenEnchantsPlugin.instance(), () -> {
                if (!player.isOnline() || player.isDead()) {
                    return;
                }
                double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue();
                player.setHealth(Math.max(0.5D, Math.min(1.0D, maxHealth)));
            });
        }
    }

    @Override
    public void onDamage(@NotNull EntityDamageEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID id = player.getUniqueId();
        long until = immunityUntilTick.getOrDefault(id, 0L);
        if (tickCounter > until) {
            immunityUntilTick.remove(id);
            return;
        }
        event.setCancelled(true);
    }

    private boolean isValidAttacker(@NotNull Entity damager) {
        if (damager instanceof LivingEntity) {
            return true;
        }
        if (damager instanceof Projectile projectile) {
            return projectile.getShooter() instanceof LivingEntity;
        }
        return false;
    }
}
