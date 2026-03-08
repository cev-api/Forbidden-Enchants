package dev.cevapi.forbiddenenchants;

import dev.cevapi.forbiddenenchants.enchants.EnchantList;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

final class PlayerLifecycleService {
    private final MasqueradeService masqueradeService;
    private final MiasmaVisualService miasmaVisualService;
    private final EnchantmentAllyService enchantmentAllyService;
    private final VexatiousService vexatiousService;
    private final CharmedPetInteractionService charmedPetInteractionService;
    private final Map<UUID, Location> lastPlayerDeathLocations;
    private final Map<UUID, Location> travelDurabilityLastLocation;
    private final Map<UUID, Double> travelDurabilityDistance;
    private final Map<UUID, MarkedState> markedTargets;
    private final Map<UUID, Long> wingClipperBlockedUntil;
    private final Map<UUID, Long> temporalSicknessNextTeleportTick;
    private final Map<UUID, Integer> temporalSicknessLastLevel;
    private final Map<UUID, Long> fullForceLastProcTick;
    private final Map<UUID, Long> fullForceExplosionImmuneUntil;
    private final Map<UUID, Vector> fullForceKnockbackVectors;
    private final Map<UUID, Long> fullForceKnockbackUntil;
    private final Map<UUID, Long> shockwaveTotemArmedUntil;
    private final Map<UUID, Long> fullForceSmashWindowUntil;
    private final Map<UUID, UUID> vexatiousAssistTargets;
    private final Map<UUID, Long> vexatiousAssistTargetExpire;
    private final Map<UUID, FearState> fearedMobs;

    PlayerLifecycleService(@NotNull MasqueradeService masqueradeService,
                           @NotNull MiasmaVisualService miasmaVisualService,
                           @NotNull EnchantmentAllyService enchantmentAllyService,
                           @NotNull VexatiousService vexatiousService,
                           @NotNull CharmedPetInteractionService charmedPetInteractionService,
                           @NotNull Map<UUID, Location> lastPlayerDeathLocations,
                           @NotNull Map<UUID, Location> travelDurabilityLastLocation,
                           @NotNull Map<UUID, Double> travelDurabilityDistance,
                           @NotNull Map<UUID, MarkedState> markedTargets,
                           @NotNull Map<UUID, Long> wingClipperBlockedUntil,
                           @NotNull Map<UUID, Long> temporalSicknessNextTeleportTick,
                           @NotNull Map<UUID, Integer> temporalSicknessLastLevel,
                           @NotNull Map<UUID, Long> fullForceLastProcTick,
                           @NotNull Map<UUID, Long> fullForceExplosionImmuneUntil,
                           @NotNull Map<UUID, Vector> fullForceKnockbackVectors,
                           @NotNull Map<UUID, Long> fullForceKnockbackUntil,
                           @NotNull Map<UUID, Long> shockwaveTotemArmedUntil,
                           @NotNull Map<UUID, Long> fullForceSmashWindowUntil,
                           @NotNull Map<UUID, UUID> vexatiousAssistTargets,
                           @NotNull Map<UUID, Long> vexatiousAssistTargetExpire,
                           @NotNull Map<UUID, FearState> fearedMobs) {
        this.masqueradeService = masqueradeService;
        this.miasmaVisualService = miasmaVisualService;
        this.enchantmentAllyService = enchantmentAllyService;
        this.vexatiousService = vexatiousService;
        this.charmedPetInteractionService = charmedPetInteractionService;
        this.lastPlayerDeathLocations = lastPlayerDeathLocations;
        this.travelDurabilityLastLocation = travelDurabilityLastLocation;
        this.travelDurabilityDistance = travelDurabilityDistance;
        this.markedTargets = markedTargets;
        this.wingClipperBlockedUntil = wingClipperBlockedUntil;
        this.temporalSicknessNextTeleportTick = temporalSicknessNextTeleportTick;
        this.temporalSicknessLastLevel = temporalSicknessLastLevel;
        this.fullForceLastProcTick = fullForceLastProcTick;
        this.fullForceExplosionImmuneUntil = fullForceExplosionImmuneUntil;
        this.fullForceKnockbackVectors = fullForceKnockbackVectors;
        this.fullForceKnockbackUntil = fullForceKnockbackUntil;
        this.shockwaveTotemArmedUntil = shockwaveTotemArmedUntil;
        this.fullForceSmashWindowUntil = fullForceSmashWindowUntil;
        this.vexatiousAssistTargets = vexatiousAssistTargets;
        this.vexatiousAssistTargetExpire = vexatiousAssistTargetExpire;
        this.fearedMobs = fearedMobs;
    }

    void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        clearFor(event.getPlayer(), false);
    }

    void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        Player player = event.getEntity();
        lastPlayerDeathLocations.put(player.getUniqueId(), player.getLocation().clone());
        clearFor(player, true);
    }

    private void clearFor(@NotNull Player player, boolean clearMarkedOwners) {
        masqueradeService.clear(player);
        miasmaVisualService.clear(player);
        EnchantList.INSTANCE.forbiddenAgility().clearFor(player);
        EnchantList.INSTANCE.theUnyielding().clearFor(player);
        vexatiousService.clear(player.getUniqueId());
        EnchantList.INSTANCE.theSeeker().clearFor(player.getUniqueId());
        travelDurabilityLastLocation.remove(player.getUniqueId());
        travelDurabilityDistance.remove(player.getUniqueId());
        markedTargets.remove(player.getUniqueId());
        wingClipperBlockedUntil.remove(player.getUniqueId());
        temporalSicknessNextTeleportTick.remove(player.getUniqueId());
        temporalSicknessLastLevel.remove(player.getUniqueId());
        fullForceLastProcTick.remove(player.getUniqueId());
        fullForceExplosionImmuneUntil.remove(player.getUniqueId());
        fullForceKnockbackVectors.remove(player.getUniqueId());
        fullForceKnockbackUntil.remove(player.getUniqueId());
        shockwaveTotemArmedUntil.remove(player.getUniqueId());
        EnchantList.INSTANCE.mujahideen().clearFor(player.getUniqueId());
        EnchantList.INSTANCE.evokersRevenge().clearFor(player.getUniqueId());
        EnchantList.INSTANCE.illusionersRevenge().clearFor(player.getUniqueId());
        fullForceSmashWindowUntil.remove(player.getUniqueId());
        charmedPetInteractionService.clearToggleDedupForPlayer(player.getUniqueId());
        vexatiousAssistTargets.remove(player.getUniqueId());
        vexatiousAssistTargetExpire.remove(player.getUniqueId());
        EnchantList.INSTANCE.appliedCurse().clearFor(player);
        if (!clearMarkedOwners) {
            UUID sourceId = player.getUniqueId();
            fearedMobs.entrySet().removeIf(entry -> entry.getValue().sourcePlayerId().equals(sourceId));
            markedTargets.entrySet().removeIf(entry -> entry.getValue().ownerId().equals(sourceId));
        } else {
            markedTargets.entrySet().removeIf(entry -> entry.getValue().ownerId().equals(player.getUniqueId()));
        }
        enchantmentAllyService.removeOwnedBy(player.getUniqueId());
    }
}

