package dev.cevapi.forbiddenenchants;

import dev.cevapi.forbiddenenchants.enchants.EnchantList;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

final class FullForceDefenseService {
    private final Supplier<EnchantStateService> enchantStateServiceSupplier;
    private final PlayerEffectService playerEffectService;
    private final MiasmaFormService miasmaFormService;
    private final Plugin schedulerPlugin;
    private final Supplier<MessagesService> messagesServiceSupplier;
    private final Map<UUID, Long> fullForceExplosionImmuneUntil;
    private final Map<UUID, Vector> fullForceKnockbackVectors;
    private final Map<UUID, Long> fullForceKnockbackUntil;

    FullForceDefenseService(@NotNull Supplier<EnchantStateService> enchantStateServiceSupplier,
                            @NotNull PlayerEffectService playerEffectService,
                            @NotNull MiasmaFormService miasmaFormService,
                            @NotNull Plugin schedulerPlugin,
                            @NotNull Supplier<MessagesService> messagesServiceSupplier,
                            @NotNull Map<UUID, Long> fullForceExplosionImmuneUntil,
                            @NotNull Map<UUID, Vector> fullForceKnockbackVectors,
                            @NotNull Map<UUID, Long> fullForceKnockbackUntil) {
        this.enchantStateServiceSupplier = enchantStateServiceSupplier;
        this.playerEffectService = playerEffectService;
        this.miasmaFormService = miasmaFormService;
        this.schedulerPlugin = schedulerPlugin;
        this.messagesServiceSupplier = messagesServiceSupplier;
        this.fullForceExplosionImmuneUntil = fullForceExplosionImmuneUntil;
        this.fullForceKnockbackVectors = fullForceKnockbackVectors;
        this.fullForceKnockbackUntil = fullForceKnockbackUntil;
    }

    void onFullForceImpact(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK
                && event.getCause() != EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
            return;
        }
        ItemStack weapon = player.getInventory().getItemInMainHand();
        int fullForceLevel = enchantStateServiceSupplier.get().getEnchantLevel(weapon, EnchantType.FULL_FORCE);
        if (!playerEffectService.isMace(weapon) || !EnchantList.INSTANCE.fullForce().isActive(fullForceLevel)) {
            return;
        }

        event.setDamage(10.0D);
        Vector knock = fullForceKnockbackVelocity(player, target);
        fullForceKnockbackVectors.put(target.getUniqueId(), knock.clone());
        fullForceKnockbackUntil.put(target.getUniqueId(), tickCounter + 2L);
        target.setVelocity(knock.clone());
        UUID targetId = target.getUniqueId();
        Bukkit.getScheduler().runTask(schedulerPlugin, () -> {
            Entity live = Bukkit.getEntity(targetId);
            if (live instanceof LivingEntity living && living.isValid() && !living.isDead()) {
                living.setVelocity(knock.clone());
            }
        });
        playFullForceSmashEffects(target.getWorld(), target.getLocation());
    }

    void onFullForceKnockback(@NotNull EntityKnockbackEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof LivingEntity living)) {
            return;
        }
        UUID id = living.getUniqueId();
        long until = fullForceKnockbackUntil.getOrDefault(id, 0L);
        if (tickCounter > until) {
            fullForceKnockbackUntil.remove(id);
            fullForceKnockbackVectors.remove(id);
            return;
        }
        Vector knock = fullForceKnockbackVectors.get(id);
        if (knock == null) {
            return;
        }
        event.setKnockback(knock.clone());
    }

    void onFullForceLanding(@NotNull EntityDamageEvent event) {
        // Full Force no longer procs on landing.
    }

    void onFullForceSelfExplosionImmunity(@NotNull EntityDamageEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION
                && event.getCause() != EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {
            return;
        }
        UUID id = player.getUniqueId();
        long fullForceUntil = fullForceExplosionImmuneUntil.getOrDefault(id, 0L);
        boolean mujahideenImmune = EnchantList.INSTANCE.mujahideen().isExplosionImmune(id, tickCounter);
        if (tickCounter > fullForceUntil) {
            fullForceExplosionImmuneUntil.remove(player.getUniqueId());
        }
        if (tickCounter <= fullForceUntil || mujahideenImmune) {
            event.setCancelled(true);
        }
    }

    void onLockedOutPortal(@NotNull PlayerPortalEvent event) {
        Player player = event.getPlayer();
        ItemStack boots = player.getInventory().getBoots();
        int lockedOutLevel = enchantStateServiceSupplier.get().getEnchantLevel(boots, EnchantType.LOCKED_OUT);
        if (!EnchantList.INSTANCE.lockedOut().isActive(lockedOutLevel)) {
            return;
        }

        if (event.getCause() == PlayerTeleportEvent.TeleportCause.NETHER_PORTAL) {
            org.bukkit.block.Block from = player.getLocation().getBlock();
            if (from.getType() == Material.NETHER_PORTAL) {
                from.setType(Material.AIR);
            }
            event.setCancelled(true);
            player.sendActionBar(Component.text(
                    messagesServiceSupplier.get().get(
                            "locked_out.nether_portal_blocked",
                            "Locked Out shattered the Nether portal."
                    ),
                    NamedTextColor.RED
            ));
            return;
        }
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.END_PORTAL) {
            event.setCancelled(true);
            player.sendActionBar(Component.text(
                    messagesServiceSupplier.get().get("locked_out.end_portal_blocked", "You're locked out."),
                    NamedTextColor.RED
            ));
        }
    }

    void onUnyieldingVelocity(@NotNull PlayerVelocityEvent event) {
        Player player = event.getPlayer();
        if (!miasmaFormService.hasUnyieldingEquipped(player)) {
            return;
        }

        event.setCancelled(true);
        event.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
    }

    void onUnyieldingKnockback(@NotNull EntityKnockbackEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!miasmaFormService.hasUnyieldingEquipped(player)) {
            return;
        }
        event.setCancelled(true);
    }

    private @NotNull Vector fullForceKnockbackVelocity(@NotNull Player attacker, @NotNull Entity target) {
        Vector push = target.getLocation().toVector().subtract(attacker.getLocation().toVector());
        push.setY(0.0D);
        if (push.lengthSquared() < 0.0001D) {
            push = attacker.getLocation().getDirection().setY(0.0D);
        }
        if (push.lengthSquared() < 0.0001D) {
            push = new Vector(1.0D, 0.0D, 0.0D);
        }

        // Tuned to launch targets by roughly 8 blocks on level ground.
        return push.normalize().multiply(1.65D).setY(0.55D);
    }

    private void playFullForceSmashEffects(@NotNull World world, @NotNull Location location) {
        Location center = location.clone().add(0.0D, 0.25D, 0.0D);
        try {
            Sound smash = Sound.valueOf("ITEM_MACE_SMASH_GROUND");
            world.playSound(center, smash, SoundCategory.PLAYERS, 1.25F, 0.95F);
        } catch (IllegalArgumentException ignored) {
            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0F, 1.2F);
        }
        world.spawnParticle(Particle.EXPLOSION, center, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        world.spawnParticle(Particle.SWEEP_ATTACK, center, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        world.spawnParticle(Particle.CRIT, center, 18, 0.45D, 0.20D, 0.45D, 0.15D);
    }
}

