package dev.cevapi.forbiddenenchants;

import dev.cevapi.forbiddenenchants.enchants.EnchantList;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityKnockbackEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.VillagerAcquireTradeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.LongSupplier;

final class ForbiddenEnchantsListener implements Listener {
    private final LongSupplier tickCounterSupplier;
    private final InteractionRestrictionService interactionRestrictionService;
    private final FullForceDefenseService fullForceDefenseService;
    private final FullPocketsService fullPocketsService;
    private final StructureInjectorRuntimeService structureInjectorRuntimeService;
    private final LootDeathService lootDeathService;
    private final PlayerLifecycleService playerLifecycleService;
    private final NameTagLeadService nameTagLeadService;
    private final CharmedPetInteractionService charmedPetInteractionService;
    private final FeMenuService feMenuService;
    private final InjectorMenuService injectorMenuService;
    private final InjectorBookRarityMenuService injectorBookRarityMenuService;
    private final LibrarianTradeMenuService librarianTradeMenuService;
    private final BundleDropMenuService bundleDropMenuService;
    private final EnchantToggleMenuService enchantToggleMenuService;
    private final LibrarianTradeService librarianTradeService;
    private final EnchantEventRuleService enchantEventRuleService;
    private final GraspCombatService graspCombatService;
    private final SpellEffectService spellEffectService;
    private final BundleDropRuntimeService bundleDropRuntimeService;

    ForbiddenEnchantsListener(@NotNull LongSupplier tickCounterSupplier,
                              @NotNull InteractionRestrictionService interactionRestrictionService,
                              @NotNull FullForceDefenseService fullForceDefenseService,
                              @NotNull FullPocketsService fullPocketsService,
                              @NotNull StructureInjectorRuntimeService structureInjectorRuntimeService,
                              @NotNull LootDeathService lootDeathService,
                              @NotNull PlayerLifecycleService playerLifecycleService,
                              @NotNull NameTagLeadService nameTagLeadService,
                              @NotNull CharmedPetInteractionService charmedPetInteractionService,
                              @NotNull FeMenuService feMenuService,
                              @NotNull InjectorMenuService injectorMenuService,
                              @NotNull InjectorBookRarityMenuService injectorBookRarityMenuService,
                              @NotNull LibrarianTradeMenuService librarianTradeMenuService,
                              @NotNull BundleDropMenuService bundleDropMenuService,
                              @NotNull EnchantToggleMenuService enchantToggleMenuService,
                              @NotNull LibrarianTradeService librarianTradeService,
                              @NotNull EnchantEventRuleService enchantEventRuleService,
                              @NotNull GraspCombatService graspCombatService,
                              @NotNull SpellEffectService spellEffectService,
                              @NotNull BundleDropRuntimeService bundleDropRuntimeService) {
        this.tickCounterSupplier = tickCounterSupplier;
        this.interactionRestrictionService = interactionRestrictionService;
        this.fullForceDefenseService = fullForceDefenseService;
        this.fullPocketsService = fullPocketsService;
        this.structureInjectorRuntimeService = structureInjectorRuntimeService;
        this.lootDeathService = lootDeathService;
        this.playerLifecycleService = playerLifecycleService;
        this.nameTagLeadService = nameTagLeadService;
        this.charmedPetInteractionService = charmedPetInteractionService;
        this.feMenuService = feMenuService;
        this.injectorMenuService = injectorMenuService;
        this.injectorBookRarityMenuService = injectorBookRarityMenuService;
        this.librarianTradeMenuService = librarianTradeMenuService;
        this.bundleDropMenuService = bundleDropMenuService;
        this.enchantToggleMenuService = enchantToggleMenuService;
        this.librarianTradeService = librarianTradeService;
        this.enchantEventRuleService = enchantEventRuleService;
        this.graspCombatService = graspCombatService;
        this.spellEffectService = spellEffectService;
        this.bundleDropRuntimeService = bundleDropRuntimeService;
    }

    @EventHandler(ignoreCancelled = true)
    public void onToggleSneak(@NotNull PlayerToggleSneakEvent event) {
        long tickCounter = tickCounterSupplier.getAsLong();
        EnchantList.INSTANCE.dispatchToggleSneak(event, tickCounter);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMasqueradeProjectile(@NotNull ProjectileLaunchEvent event) {
        long tickCounter = tickCounterSupplier.getAsLong();
        EnchantList.INSTANCE.dispatchProjectileLaunch(event, tickCounter);
    }

    @EventHandler(ignoreCancelled = true)
    public void onShootBow(@NotNull EntityShootBowEvent event) {
        long tickCounter = tickCounterSupplier.getAsLong();
        EnchantList.INSTANCE.dispatchShootBow(event, tickCounter);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMiasmaImpact(@NotNull ProjectileHitEvent event) {
        long tickCounter = tickCounterSupplier.getAsLong();
        EnchantList.INSTANCE.dispatchProjectileHit(event, tickCounter);
    }

    @EventHandler(ignoreCancelled = true)
    public void onMobTarget(@NotNull EntityTargetLivingEntityEvent event) { EnchantList.INSTANCE.dispatchMobTarget(event, tickCounterSupplier.getAsLong()); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpellUseBlockedBySilence(@NotNull PlayerInteractEvent event) {
        spellEffectService.onPlayerInteract(event, tickCounterSupplier.getAsLong());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpellPotionConsume(@NotNull PlayerItemConsumeEvent event) {
        spellEffectService.onPlayerItemConsume(event, tickCounterSupplier.getAsLong());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpellMove(@NotNull PlayerMoveEvent event) {
        spellEffectService.onPlayerMove(event, tickCounterSupplier.getAsLong());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMiasmaInteraction(@NotNull PlayerInteractEvent event) {
        interactionRestrictionService.onMiasmaInteraction(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMiasmaBreak(@NotNull BlockBreakEvent event) { interactionRestrictionService.onMiasmaBreak(event); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onMiasmaPlace(@NotNull BlockPlaceEvent event) { interactionRestrictionService.onMiasmaPlace(event); }

    @EventHandler(ignoreCancelled = true)
    public void onGreedDropItem(@NotNull PlayerDropItemEvent event) { interactionRestrictionService.onGreedDropItem(event); }

    @EventHandler(ignoreCancelled = true)
    public void onLaunchDoubleJump(@NotNull PlayerToggleFlightEvent event) {
        long tickCounter = tickCounterSupplier.getAsLong();
        EnchantList.INSTANCE.dispatchToggleFlight(event, tickCounter);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWingClipperEquipBlock(@NotNull InventoryClickEvent event) { interactionRestrictionService.onWingClipperEquipBlock(event); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWingClipperDragBlock(@NotNull InventoryDragEvent event) { interactionRestrictionService.onWingClipperDragBlock(event); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onWingClipperInteractBlock(@NotNull PlayerInteractEvent event) { interactionRestrictionService.onWingClipperInteractBlock(event); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onFullForceImpact(@NotNull EntityDamageByEntityEvent event) { fullForceDefenseService.onFullForceImpact(event, tickCounterSupplier.getAsLong()); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFullForceKnockback(@NotNull EntityKnockbackEvent event) { fullForceDefenseService.onFullForceKnockback(event, tickCounterSupplier.getAsLong()); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFullForceLanding(@NotNull EntityDamageEvent event) { fullForceDefenseService.onFullForceLanding(event); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFullForceSelfExplosionImmunity(@NotNull EntityDamageEvent event) { fullForceDefenseService.onFullForceSelfExplosionImmunity(event, tickCounterSupplier.getAsLong()); }

    @EventHandler(ignoreCancelled = true)
    public void onLockedOutPortal(@NotNull PlayerPortalEvent event) { fullForceDefenseService.onLockedOutPortal(event); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFullPocketsOpen(@NotNull PlayerInteractEvent event) { fullPocketsService.onOpen(event); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onStructureInjectorOpen(@NotNull PlayerInteractEvent event) { structureInjectorRuntimeService.onStructureInjectorOpen(event); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTrialVaultInjector(@NotNull PlayerInteractEvent event) { structureInjectorRuntimeService.onTrialVaultInjector(event); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUnyieldingVelocity(@NotNull PlayerVelocityEvent event) { fullForceDefenseService.onUnyieldingVelocity(event); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onUnyieldingKnockback(@NotNull EntityKnockbackEvent event) { fullForceDefenseService.onUnyieldingKnockback(event); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHatedOneLoot(@NotNull EntityDeathEvent event) { lootDeathService.onHatedOneLoot(event); }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBundleDropLoot(@NotNull EntityDeathEvent event) { bundleDropRuntimeService.onEntityDeath(event); }

    @EventHandler
    public void onPlayerQuit(@NotNull PlayerQuitEvent event) {
        playerLifecycleService.onPlayerQuit(event);
        spellEffectService.onPlayerQuit(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerDeath(@NotNull PlayerDeathEvent event) {
        playerLifecycleService.onPlayerDeath(event);
        spellEffectService.onPlayerDeath(event.getEntity().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpecialNameTagUse(@NotNull PlayerInteractEntityEvent event) { nameTagLeadService.onSpecialNameTagUse(event, tickCounterSupplier.getAsLong()); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCharmedPetToggle(@NotNull PlayerInteractEntityEvent event) { charmedPetInteractionService.handleCharmedPetToggle(event.getPlayer(), event.getRightClicked(), event, tickCounterSupplier.getAsLong()); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCharmedPetToggleAt(@NotNull PlayerInteractAtEntityEvent event) { charmedPetInteractionService.handleCharmedPetToggle(event.getPlayer(), event.getRightClicked(), event, tickCounterSupplier.getAsLong()); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSpecialLeadUse(@NotNull PlayerInteractEntityEvent event) { charmedPetInteractionService.handleSpecialLeadUse(event.getPlayer(), event.getRightClicked(), event.getHand(), event); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSpecialLeadUseAt(@NotNull PlayerInteractAtEntityEvent event) { charmedPetInteractionService.handleSpecialLeadUse(event.getPlayer(), event.getRightClicked(), event.getHand(), event); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onVillagerSneakUnleash(@NotNull PlayerInteractEntityEvent event) { charmedPetInteractionService.handleVillagerSneakUnleash(event.getPlayer(), event.getRightClicked(), event); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onVillagerSneakUnleashAt(@NotNull PlayerInteractAtEntityEvent event) { charmedPetInteractionService.handleVillagerSneakUnleash(event.getPlayer(), event.getRightClicked(), event); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onPlayerLeashVillager(@NotNull PlayerLeashEntityEvent event) { nameTagLeadService.onPlayerLeashVillager(event); }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
    public void onPlayerLeashVillagerFallback(@NotNull PlayerLeashEntityEvent event) { nameTagLeadService.onPlayerLeashVillagerFallback(event); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onShockwaveTotemPop(@NotNull EntityResurrectEvent event) {
        spellEffectService.onEntityResurrect(event, tickCounterSupplier.getAsLong());
        if (event.isCancelled()) {
            return;
        }
        long tickCounter = tickCounterSupplier.getAsLong();
        EnchantList.INSTANCE.dispatchTotemPop(event, tickCounter);
    }

    @EventHandler
    public void onAppliedCurseChat(@NotNull AsyncPlayerChatEvent event) {
        long tickCounter = tickCounterSupplier.getAsLong();
        spellEffectService.onChat(event, tickCounter);
        if (event.isCancelled()) {
            return;
        }
        EnchantList.INSTANCE.appliedCurse().onChat(event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRuntimeDamageByEntityDispatch(@NotNull EntityDamageByEntityEvent event) {
        EnchantList.INSTANCE.dispatchDamageByEntity(event, tickCounterSupplier.getAsLong());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRuntimeDamageDispatch(@NotNull EntityDamageEvent event) {
        EnchantList.INSTANCE.dispatchDamage(event, tickCounterSupplier.getAsLong());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRuntimeInteractDispatch(@NotNull PlayerInteractEvent event) {
        EnchantList.INSTANCE.dispatchInteract(event, tickCounterSupplier.getAsLong());
    }

    @EventHandler(ignoreCancelled = true)
    public void onFeMenuClick(@NotNull InventoryClickEvent event) { feMenuService.onMenuClick(event); }

    @EventHandler(ignoreCancelled = true)
    public void onFeMenuDrag(@NotNull InventoryDragEvent event) { feMenuService.onMenuDrag(event); }

    @EventHandler(ignoreCancelled = true)
    public void onInjectorMenuClick(@NotNull InventoryClickEvent event) { injectorMenuService.onMenuClick(event); }

    @EventHandler(ignoreCancelled = true)
    public void onInjectorMenuDrag(@NotNull InventoryDragEvent event) { injectorMenuService.onMenuDrag(event); }

    @EventHandler(ignoreCancelled = true)
    public void onInjectorRarityMenuClick(@NotNull InventoryClickEvent event) {
        injectorBookRarityMenuService.onMenuClick(event, ForbiddenEnchantsPlugin.instance());
    }

    @EventHandler(ignoreCancelled = true)
    public void onInjectorRarityMenuDrag(@NotNull InventoryDragEvent event) { injectorBookRarityMenuService.onMenuDrag(event); }

    @EventHandler(ignoreCancelled = true)
    public void onLibrarianTradeMenuClick(@NotNull InventoryClickEvent event) { librarianTradeMenuService.onMenuClick(event); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onSilenceAnvilBlock(@NotNull InventoryClickEvent event) {
        spellEffectService.onInventoryClick(event, tickCounterSupplier.getAsLong());
    }

    @EventHandler(ignoreCancelled = true)
    public void onLibrarianTradeMenuDrag(@NotNull InventoryDragEvent event) { librarianTradeMenuService.onMenuDrag(event); }

    @EventHandler(ignoreCancelled = true)
    public void onBundleDropMenuClick(@NotNull InventoryClickEvent event) { bundleDropMenuService.onMenuClick(event); }

    @EventHandler(ignoreCancelled = true)
    public void onBundleDropMenuDrag(@NotNull InventoryDragEvent event) { bundleDropMenuService.onMenuDrag(event); }

    @EventHandler(ignoreCancelled = true)
    public void onEnchantToggleMenuClick(@NotNull InventoryClickEvent event) { enchantToggleMenuService.onMenuClick(event); }

    @EventHandler(ignoreCancelled = true)
    public void onEnchantToggleMenuDrag(@NotNull InventoryDragEvent event) { enchantToggleMenuService.onMenuDrag(event); }

    @EventHandler(ignoreCancelled = true)
    public void onPrepareAnvil(@NotNull PrepareAnvilEvent event) { enchantEventRuleService.onPrepareAnvil(event); }

    @EventHandler(ignoreCancelled = true)
    public void onEnchantItem(@NotNull EnchantItemEvent event) {
        long tickCounter = tickCounterSupplier.getAsLong();
        spellEffectService.onEnchantItem(event, tickCounter);
        if (event.isCancelled()) {
            return;
        }
        enchantEventRuleService.onEnchantItem(event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerItemDamage(@NotNull PlayerItemDamageEvent event) { enchantEventRuleService.onPlayerItemDamage(event); }

    @EventHandler(ignoreCancelled = true)
    public void onVillagerAcquireTrade(@NotNull VillagerAcquireTradeEvent event) { librarianTradeService.onVillagerAcquireTrade(event); }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGraspInteract(@NotNull PlayerInteractEvent event) { graspCombatService.onGraspInteract(event); }

    @EventHandler(ignoreCancelled = true)
    public void onGraspAttack(@NotNull PlayerAnimationEvent event) { graspCombatService.onGraspAttack(event); }
}

