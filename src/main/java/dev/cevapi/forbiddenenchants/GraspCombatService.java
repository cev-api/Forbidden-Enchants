package dev.cevapi.forbiddenenchants;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

final class GraspCombatService {
    private final Supplier<EnchantStateService> enchantStateServiceSupplier;
    private final MasqueradeService masqueradeService;
    private final VoidGraspService voidGraspService;
    private final VisionSenseService visionSenseService;
    private final double forbiddenReach;
    private final double defaultEntityReach;

    GraspCombatService(@NotNull Supplier<EnchantStateService> enchantStateServiceSupplier,
                       @NotNull MasqueradeService masqueradeService,
                       @NotNull VoidGraspService voidGraspService,
                       @NotNull VisionSenseService visionSenseService,
                       double forbiddenReach,
                       double defaultEntityReach) {
        this.enchantStateServiceSupplier = enchantStateServiceSupplier;
        this.masqueradeService = masqueradeService;
        this.voidGraspService = voidGraspService;
        this.visionSenseService = visionSenseService;
        this.forbiddenReach = forbiddenReach;
        this.defaultEntityReach = defaultEntityReach;
    }

    void onGraspInteract(@NotNull PlayerInteractEvent event) {
        Player player = event.getPlayer();
        GraspMode graspMode = getGraspMode(player.getInventory().getChestplate());
        if (graspMode == GraspMode.NONE) {
            return;
        }

        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block target = voidGraspService.findFirstInteractableBlockInView(player, forbiddenReach, graspMode == GraspMode.VOID);
        if (target == null) {
            return;
        }

        if (voidGraspService.performInteraction(player, target)) {
            event.setCancelled(true);
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
            event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
            if (graspMode == GraspMode.VOID) {
                Location from = player.getEyeLocation();
                Location to = target.getLocation().add(0.5, 0.6, 0.5);
                visionSenseService.spawnWorldTrailParticles(player.getWorld(), from, to, Particle.ENCHANT, 24);
                player.getWorld().spawnParticle(Particle.ENCHANT, to, 12, 0.2, 0.2, 0.2, 0.0);
            } else {
                player.spawnParticle(Particle.ENCHANT, target.getLocation().add(0.5, 0.6, 0.5), 8, 0.2, 0.2, 0.2, 0.0);
            }
        }
    }

    void onGraspAttack(@NotNull PlayerAnimationEvent event) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }

        Player player = event.getPlayer();
        if (masqueradeService.isMasquerading(player)) {
            return;
        }
        GraspMode graspMode = getGraspMode(player.getInventory().getChestplate());
        if (graspMode == GraspMode.NONE) {
            return;
        }

        if (player.getAttackCooldown() < 0.92F) {
            return;
        }

        RayTraceResult trace = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                forbiddenReach,
                0.3,
                entity -> entity instanceof LivingEntity && !entity.equals(player)
        );

        if (trace == null || !(trace.getHitEntity() instanceof LivingEntity target)) {
            return;
        }

        boolean lineOfSight = player.hasLineOfSight(target);
        if (graspMode == GraspMode.EXTENDED && !lineOfSight) {
            return;
        }

        double distance = player.getEyeLocation().distance(target.getLocation().add(0.0, target.getHeight() * 0.5, 0.0));
        if (lineOfSight && distance <= defaultEntityReach + 0.2D) {
            return;
        }

            player.attack(target);
            if (graspMode == GraspMode.VOID) {
                Location from = player.getEyeLocation();
                Location to = target.getLocation().add(0.0, target.getHeight() * 0.5, 0.0);
            visionSenseService.spawnWorldTrailParticles(player.getWorld(), from, to, Particle.ENCHANT, 26);
            player.getWorld().spawnParticle(Particle.ENCHANT, to, 10, 0.18, 0.18, 0.18, 0.0);
        }
    }

    private @NotNull GraspMode getGraspMode(@Nullable ItemStack chestplate) {
        if (enchantStateServiceSupplier.get().getEnchantLevel(chestplate, EnchantType.VOID_GRASP) > 0) {
            return GraspMode.VOID;
        }
        if (enchantStateServiceSupplier.get().getEnchantLevel(chestplate, EnchantType.EXTENDED_GRASP) > 0) {
            return GraspMode.EXTENDED;
        }
        return GraspMode.NONE;
    }

    private enum GraspMode {
        NONE,
        EXTENDED,
        VOID
    }
}

