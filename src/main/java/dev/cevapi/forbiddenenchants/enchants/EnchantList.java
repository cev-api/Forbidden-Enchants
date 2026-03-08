package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.EnchantType;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EnchantList {
    public static final EnchantList INSTANCE = new EnchantList();

    private final List<ForbiddenEnchantDefinition> all;
    private final List<ForbiddenEnchantRuntime> runtimeEnchants;
    private final Map<String, ForbiddenEnchantDefinition> byArg = new HashMap<>();

    private EnchantList() {
        all = Arrays.stream(EnchantType.values())
                .map(EnchantType::definition)
                .toList();
        runtimeEnchants = all.stream()
                .filter(ForbiddenEnchantRuntime.class::isInstance)
                .map(ForbiddenEnchantRuntime.class::cast)
                .toList();

        for (ForbiddenEnchantDefinition enchant : all) {
            byArg.put(enchant.arg(), enchant);
            for (String alias : enchant.aliases()) {
                byArg.putIfAbsent(alias, enchant);
            }
        }
    }

    public @NotNull DivineVisionEnchant divineVision() {
        return (DivineVisionEnchant) EnchantType.DIVINE_VISION.definition();
    }

    public @NotNull MinersIntuitionEnchant minersIntuition() {
        return (MinersIntuitionEnchant) EnchantType.MINERS_INTUITION.definition();
    }

    public @NotNull LootSenseEnchant lootSense() {
        return (LootSenseEnchant) EnchantType.LOOT_SENSE.definition();
    }

    public @NotNull ExtendedGraspEnchant extendedGrasp() {
        return (ExtendedGraspEnchant) EnchantType.EXTENDED_GRASP.definition();
    }

    public @NotNull VoidGraspEnchant voidGrasp() {
        return (VoidGraspEnchant) EnchantType.VOID_GRASP.definition();
    }

    public @NotNull MasqueradeEnchant masquerade() {
        return (MasqueradeEnchant) EnchantType.MASQUERADE.definition();
    }

    public @NotNull AscensionEnchant ascension() {
        return (AscensionEnchant) EnchantType.ASCENSION.definition();
    }

    public @NotNull InciteFearEnchant inciteFear() {
        return (InciteFearEnchant) EnchantType.INCITE_FEAR.definition();
    }

    public @NotNull BlindnessEnchant blindness() {
        return (BlindnessEnchant) EnchantType.BLINDNESS.definition();
    }

    public @NotNull MiasmaEnchant miasma() {
        return (MiasmaEnchant) EnchantType.MIASMA.definition();
    }

    public @NotNull CharmEnchant charm() {
        return (CharmEnchant) EnchantType.CHARM.definition();
    }

    public @NotNull MiasmaFormEnchant miasmaForm() {
        return (MiasmaFormEnchant) EnchantType.MIASMA_FORM.definition();
    }

    public @NotNull AquaticSacrificeEnchant aquaticSacrifice() {
        return (AquaticSacrificeEnchant) EnchantType.AQUATIC_SACRIFICE.definition();
    }

    public @NotNull TheHatedOneEnchant theHatedOne() {
        return (TheHatedOneEnchant) EnchantType.THE_HATED_ONE.definition();
    }

    public @NotNull WitheringStrikeEnchant witheringStrike() {
        return (WitheringStrikeEnchant) EnchantType.WITHERING_STRIKE.definition();
    }

    public @NotNull HealingTouchEnchant healingTouch() {
        return (HealingTouchEnchant) EnchantType.HEALING_TOUCH.definition();
    }

    public @NotNull FullPocketsEnchant fullPockets() {
        return (FullPocketsEnchant) EnchantType.FULL_POCKETS.definition();
    }

    public @NotNull DragonsBreathEnchant dragonsBreath() {
        return (DragonsBreathEnchant) EnchantType.DRAGONS_BREATH.definition();
    }

    public @NotNull ExplosiveReactionEnchant explosiveReaction() {
        return (ExplosiveReactionEnchant) EnchantType.EXPLOSIVE_REACTION.definition();
    }

    public @NotNull TheUnyieldingEnchant theUnyielding() {
        return (TheUnyieldingEnchant) EnchantType.THE_UNYIELDING.definition();
    }

    public @NotNull ForbiddenAgilityEnchant forbiddenAgility() {
        return (ForbiddenAgilityEnchant) EnchantType.FORBIDDEN_AGILITY.definition();
    }

    public @NotNull PocketDimensionEnchant pocketDimension() {
        return (PocketDimensionEnchant) EnchantType.POCKET_DIMENSION.definition();
    }

    public @NotNull PettyThiefEnchant pettyThief() {
        return (PettyThiefEnchant) EnchantType.PETTY_THIEF.definition();
    }

    public @NotNull LumberjackEnchant lumberjack() {
        return (LumberjackEnchant) EnchantType.LUMBERJACK.definition();
    }

    public @NotNull SonicPanicEnchant sonicPanic() {
        return (SonicPanicEnchant) EnchantType.SONIC_PANIC.definition();
    }

    public @NotNull CreepersInfluenceEnchant creepersInfluence() {
        return (CreepersInfluenceEnchant) EnchantType.CREEPERS_INFLUENCE.definition();
    }

    public @NotNull StaffOfTheEvokerEnchant staffOfTheEvoker() {
        return (StaffOfTheEvokerEnchant) EnchantType.STAFF_OF_THE_EVOKER.definition();
    }

    public @NotNull VexatiousEnchant vexatious() {
        return (VexatiousEnchant) EnchantType.VEXATIOUS.definition();
    }

    public @NotNull WololoEnchant wololo() {
        return (WololoEnchant) EnchantType.WOLOLO.definition();
    }

    public @NotNull LockedOutEnchant lockedOut() {
        return (LockedOutEnchant) EnchantType.LOCKED_OUT.definition();
    }

    public @NotNull EvokersRevengeEnchant evokersRevenge() {
        return (EvokersRevengeEnchant) EnchantType.EVOKERS_REVENGE.definition();
    }

    public @NotNull TheSeekerEnchant theSeeker() {
        return (TheSeekerEnchant) EnchantType.THE_SEEKER.definition();
    }

    public @NotNull DisarmEnchant disarm() {
        return (DisarmEnchant) EnchantType.DISARM.definition();
    }

    public @NotNull MarkedEnchant marked() {
        return (MarkedEnchant) EnchantType.MARKED.definition();
    }

    public @NotNull GreedEnchant greed() {
        return (GreedEnchant) EnchantType.GREED.definition();
    }

    public @NotNull WingClipperEnchant wingClipper() {
        return (WingClipperEnchant) EnchantType.WING_CLIPPER.definition();
    }

    public @NotNull LaunchEnchant launch() {
        return (LaunchEnchant) EnchantType.LAUNCH.definition();
    }

    public @NotNull FullForceEnchant fullForce() {
        return (FullForceEnchant) EnchantType.FULL_FORCE.definition();
    }

    public @NotNull TemporalSicknessEnchant temporalSickness() {
        return (TemporalSicknessEnchant) EnchantType.TEMPORAL_SICKNESS.definition();
    }

    public @NotNull GraveRobberEnchant graveRobber() {
        return (GraveRobberEnchant) EnchantType.GRAVE_ROBBER.definition();
    }

    public @NotNull PocketSeekerEnchant pocketSeeker() {
        return (PocketSeekerEnchant) EnchantType.POCKET_SEEKER.definition();
    }

    public @NotNull CharmedPetEnchant charmedPet() {
        return (CharmedPetEnchant) EnchantType.CHARMED_PET.definition();
    }

    public @NotNull AppliedCurseEnchant appliedCurse() {
        return (AppliedCurseEnchant) EnchantType.APPLIED_CURSE.definition();
    }

    public @NotNull GetOverHereEnchant getOverHere() {
        return (GetOverHereEnchant) EnchantType.GET_OVER_HERE.definition();
    }

    public @NotNull MujahideenEnchant mujahideen() {
        return (MujahideenEnchant) EnchantType.MUJAHIDEEN.definition();
    }

    public @NotNull ShieldKnockbackEnchant shieldKnockback() {
        return (ShieldKnockbackEnchant) EnchantType.SHIELD_KNOCKBACK.definition();
    }

    public @NotNull RicochetEnchant ricochet() {
        return (RicochetEnchant) EnchantType.RICOCHET.definition();
    }

    public @NotNull ShockwaveEnchant shockwave() {
        return (ShockwaveEnchant) EnchantType.SHOCKWAVE.definition();
    }

    public @NotNull ProudWarriorEnchant proudWarrior() {
        return (ProudWarriorEnchant) EnchantType.PROUD_WARRIOR.definition();
    }

    public @NotNull SiskosSolutionEnchant siskosSolution() {
        return (SiskosSolutionEnchant) EnchantType.SISKOS_SOLUTION.definition();
    }

    public @NotNull NoFallEnchant noFall() {
        return (NoFallEnchant) EnchantType.NO_FALL.definition();
    }

    public @NotNull BorgTechnologyEnchant borgTechnology() {
        return (BorgTechnologyEnchant) EnchantType.BORG_TECHNOLOGY.definition();
    }

    public @NotNull WarpNineFiveEnchant warpNineFive() {
        return (WarpNineFiveEnchant) EnchantType.WARP_NINE_FIVE.definition();
    }

    public @NotNull TrackerEnchant tracker() {
        return (TrackerEnchant) EnchantType.TRACKER.definition();
    }

    public @NotNull ThePretenderEnchant thePretender() {
        return (ThePretenderEnchant) EnchantType.THE_PRETENDER.definition();
    }

    public @NotNull OutOfPhaseEnchant outOfPhase() {
        return (OutOfPhaseEnchant) EnchantType.OUT_OF_PHASE.definition();
    }

    public @NotNull SilenceEnchant silence() {
        return (SilenceEnchant) EnchantType.SILENCE.definition();
    }

    public @NotNull QuitterEnchant quitter() {
        return (QuitterEnchant) EnchantType.QUITTER.definition();
    }

    public @NotNull InfectedEnchant infected() {
        return (InfectedEnchant) EnchantType.INFECTED.definition();
    }

    public @NotNull JointSleepEnchant jointSleep() {
        return (JointSleepEnchant) EnchantType.JOINT_SLEEP.definition();
    }

    public @NotNull IllusionersRevengeEnchant illusionersRevenge() {
        return (IllusionersRevengeEnchant) EnchantType.ILLUSIONERS_REVENGE.definition();
    }

    public @NotNull TheDuplicatorEnchant theDuplicator() {
        return (TheDuplicatorEnchant) EnchantType.THE_DUPLICATOR.definition();
    }

    public @NotNull ThePhilosophersBookEnchant thePhilosophersBook() {
        return (ThePhilosophersBookEnchant) EnchantType.THE_PHILOSOPHERS_BOOK.definition();
    }

    public @NotNull List<ForbiddenEnchantDefinition> all() {
        return all;
    }

    public @Nullable ForbiddenEnchantDefinition byArg(@NotNull String normalizedArg) {
        return byArg.get(normalizedArg);
    }

    public void dispatchToggleSneak(@NotNull PlayerToggleSneakEvent event, long tickCounter) {
        for (ForbiddenEnchantRuntime runtime : runtimeEnchants) {
            runtime.onToggleSneak(event, tickCounter);
        }
    }

    public void dispatchDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        for (ForbiddenEnchantRuntime runtime : runtimeEnchants) {
            runtime.onDamageByEntity(event, tickCounter);
            if (event.isCancelled()) {
                return;
            }
        }
    }

    public void dispatchDamage(@NotNull EntityDamageEvent event, long tickCounter) {
        for (ForbiddenEnchantRuntime runtime : runtimeEnchants) {
            runtime.onDamage(event, tickCounter);
            if (event.isCancelled()) {
                return;
            }
        }
    }

    public void dispatchProjectileLaunch(@NotNull ProjectileLaunchEvent event, long tickCounter) {
        for (ForbiddenEnchantRuntime runtime : runtimeEnchants) {
            runtime.onProjectileLaunch(event, tickCounter);
        }
    }

    public void dispatchShootBow(@NotNull EntityShootBowEvent event, long tickCounter) {
        for (ForbiddenEnchantRuntime runtime : runtimeEnchants) {
            runtime.onShootBow(event, tickCounter);
        }
    }

    public void dispatchProjectileHit(@NotNull ProjectileHitEvent event, long tickCounter) {
        for (ForbiddenEnchantRuntime runtime : runtimeEnchants) {
            runtime.onProjectileHit(event, tickCounter);
        }
    }

    public void dispatchMobTarget(@NotNull EntityTargetLivingEntityEvent event, long tickCounter) {
        for (ForbiddenEnchantRuntime runtime : runtimeEnchants) {
            runtime.onMobTarget(event, tickCounter);
        }
    }

    public void dispatchInteract(@NotNull PlayerInteractEvent event, long tickCounter) {
        for (ForbiddenEnchantRuntime runtime : runtimeEnchants) {
            runtime.onInteract(event, tickCounter);
        }
    }

    public void dispatchToggleFlight(@NotNull PlayerToggleFlightEvent event, long tickCounter) {
        for (ForbiddenEnchantRuntime runtime : runtimeEnchants) {
            runtime.onToggleFlight(event, tickCounter);
        }
    }

    public void dispatchTotemPop(@NotNull EntityResurrectEvent event, long tickCounter) {
        for (ForbiddenEnchantRuntime runtime : runtimeEnchants) {
            runtime.onTotemPop(event, tickCounter);
        }
    }

    public void dispatchPlayerTick(@NotNull Player player, long tickCounter) {
        for (ForbiddenEnchantRuntime runtime : runtimeEnchants) {
            runtime.onPlayerTick(player, tickCounter);
        }
    }
}

