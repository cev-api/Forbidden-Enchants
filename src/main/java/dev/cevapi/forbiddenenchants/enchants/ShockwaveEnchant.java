package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class ShockwaveEnchant extends BaseForbiddenEnchant {
    public ShockwaveEnchant() {
        super("shockwave",
                "shockwave_level",
                "Shockwave",
                ArmorSlot.TOTEM,
                1,
                NamedTextColor.DARK_AQUA,
                List.of("shockwave", "totemshockwave", "totem_shockwave"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "On totem pop, emit shockwave: pushes entities away and deals 3 hearts.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }

    public long carryoverTicks() {
        return 60L;
    }

    public int effectiveTotemLevel(int equippedLevel, long tickCounter, long armedUntilTick) {
        if (isActive(equippedLevel)) {
            return equippedLevel;
        }
        return tickCounter <= armedUntilTick ? 1 : 0;
    }

    public void trigger(@NotNull Player player) {
        Location center = player.getLocation();
        World world = player.getWorld();
        world.spawnParticle(Particle.SONIC_BOOM, center, 1, 0.0, 0.0, 0.0, 0.0);
        world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.0F, 1.1F);

        for (Entity nearby : world.getNearbyEntities(center, 8.0D, 4.0D, 8.0D)) {
            if (!(nearby instanceof LivingEntity living) || nearby.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            Vector push = living.getLocation().toVector().subtract(center.toVector());
            if (push.lengthSquared() < 0.0001D) {
                push = new Vector(
                        ThreadLocalRandom.current().nextDouble(-1.0D, 1.0D),
                        0.0D,
                        ThreadLocalRandom.current().nextDouble(-1.0D, 1.0D));
            }
            double strength = ThreadLocalRandom.current().nextDouble(1.0D, 1.8D);
            living.setVelocity(push.normalize().multiply(strength).setY(0.42D));
            living.damage(6.0D, player);
        }
    }

    @Override
    public void onTotemPop(@NotNull EntityResurrectEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack poppedTotem = getResurrectedTotem(player, event);
        if (poppedTotem.getType() != Material.TOTEM_OF_UNDYING) {
            return;
        }
        int level = plugin().getEnchantLevel(poppedTotem, EnchantType.SHOCKWAVE);
        level = effectiveTotemLevel(level, tickCounter, plugin().shockwaveArmedUntil(player.getUniqueId()));
        if (!isActive(level)) {
            return;
        }
        trigger(player);
    }

    private @NotNull ItemStack getResurrectedTotem(@NotNull Player player, @NotNull EntityResurrectEvent event) {
        EquipmentSlot hand = event.getHand();
        if (hand == EquipmentSlot.HAND) {
            return player.getInventory().getItemInMainHand();
        }
        if (hand == EquipmentSlot.OFF_HAND) {
            return player.getInventory().getItemInOffHand();
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() == Material.TOTEM_OF_UNDYING) {
            return off;
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() == Material.TOTEM_OF_UNDYING) {
            return main;
        }
        return new ItemStack(Material.AIR);
    }
}

