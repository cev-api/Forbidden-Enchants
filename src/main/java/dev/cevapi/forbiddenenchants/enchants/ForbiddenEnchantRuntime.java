package dev.cevapi.forbiddenenchants.enchants;

import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface ForbiddenEnchantRuntime {
    default void onToggleSneak(@NotNull PlayerToggleSneakEvent event, long tickCounter) {}

    default void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {}

    default void onDamage(@NotNull EntityDamageEvent event, long tickCounter) {}

    default void onProjectileLaunch(@NotNull ProjectileLaunchEvent event, long tickCounter) {}

    default void onShootBow(@NotNull EntityShootBowEvent event, long tickCounter) {}

    default void onProjectileHit(@NotNull ProjectileHitEvent event, long tickCounter) {}

    default void onMobTarget(@NotNull EntityTargetLivingEntityEvent event, long tickCounter) {}

    default void onInteract(@NotNull PlayerInteractEvent event, long tickCounter) {}

    default void onToggleFlight(@NotNull PlayerToggleFlightEvent event, long tickCounter) {}

    default void onTotemPop(@NotNull EntityResurrectEvent event, long tickCounter) {}

    default void onPlayerTick(@NotNull Player player, long tickCounter) {}
}

