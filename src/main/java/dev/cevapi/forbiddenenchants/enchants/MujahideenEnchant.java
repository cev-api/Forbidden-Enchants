package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MujahideenEnchant extends BaseForbiddenEnchant {
    private final Map<UUID, Long> armedUntilByPlayer = new HashMap<>();
    private final Map<UUID, Long> explosionImmuneUntilByPlayer = new HashMap<>();

    public MujahideenEnchant() {
        super("mujahideen",
                "mujahideen_level",
                "Mujahideen",
                ArmorSlot.TOTEM,
                1,
                NamedTextColor.RED,
                List.of("mujahideen", "muj", "martyr_totem", "martyrtotem"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Totem pop detonates 2x TNT force, smoke-burst, and teleports you ~50 blocks to safety.";
    }

    public void refreshArmedState(@NotNull UUID playerId, long tickCounter, int mujahideenLevel) {
        if (mujahideenLevel > 0) {
            armedUntilByPlayer.put(playerId, tickCounter + 60L);
            return;
        }
        if (tickCounter > armedUntilByPlayer.getOrDefault(playerId, 0L)) {
            armedUntilByPlayer.remove(playerId);
        }
    }

    public boolean isArmedByCarryover(@NotNull UUID playerId, long tickCounter) {
        return tickCounter <= armedUntilByPlayer.getOrDefault(playerId, 0L);
    }

    public void markExplosionImmune(@NotNull UUID playerId, long tickCounter, long durationTicks) {
        explosionImmuneUntilByPlayer.put(playerId, tickCounter + Math.max(1L, durationTicks));
    }

    public boolean isExplosionImmune(@NotNull UUID playerId, long tickCounter) {
        long until = explosionImmuneUntilByPlayer.getOrDefault(playerId, 0L);
        if (tickCounter > until) {
            explosionImmuneUntilByPlayer.remove(playerId);
            return false;
        }
        return until > 0L;
    }

    public void clearFor(@NotNull UUID playerId) {
        armedUntilByPlayer.remove(playerId);
        explosionImmuneUntilByPlayer.remove(playerId);
    }

    public void resetAll() {
        armedUntilByPlayer.clear();
        explosionImmuneUntilByPlayer.clear();
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
        UUID playerId = player.getUniqueId();
        int level = plugin().getEnchantLevel(poppedTotem, EnchantType.MUJAHIDEEN);
        if (level <= 0 && isArmedByCarryover(playerId, tickCounter)) {
            level = 1;
        }
        if (level <= 0) {
            return;
        }

        triggerTotem(player, tickCounter);
    }

    private void triggerTotem(@NotNull Player player, long tickCounter) {
        Location center = player.getLocation().clone();
        World world = center.getWorld();
        if (world == null) {
            return;
        }

        markExplosionImmune(player.getUniqueId(), tickCounter, 80L);
        EnchantList.INSTANCE.miasma().spawnSmokeVisual(world, center.clone().add(0.0D, 1.0D, 0.0D));
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2F, 0.95F);
        try {
            world.createExplosion(player, center.clone().add(0.35D, 0.0D, 0.35D), 4.0F, false, true);
            world.createExplosion(player, center.clone().add(-0.35D, 0.0D, -0.35D), 4.0F, false, true);
        } catch (Throwable ignored) {
            world.createExplosion(center, 4.0F, false, true);
            world.createExplosion(center.clone().add(0.2D, 0.0D, -0.2D), 4.0F, false, true);
        }

        Bukkit.getScheduler().runTask(plugin(), () -> {
            if (!player.isOnline() || player.isDead()) {
                return;
            }
            Location destination = plugin().findPocketDimensionSafeLocation(player.getLocation(), 50.0D);
            if (destination == null) {
                return;
            }
            player.teleport(destination);
            World destWorld = destination.getWorld();
            if (destWorld != null) {
                destWorld.spawnParticle(Particle.PORTAL, destination.clone().add(0.0D, 1.0D, 0.0D), 80, 0.45D, 0.75D, 0.45D, 0.02D);
            }
        });
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

