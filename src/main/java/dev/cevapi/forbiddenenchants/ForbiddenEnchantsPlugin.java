
package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import dev.cevapi.forbiddenenchants.enchants.CompassTrackingService;
import dev.cevapi.forbiddenenchants.enchants.MasqueradeEnchant;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.generator.structure.Structure;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ForbiddenEnchantsPlugin extends JavaPlugin {
    private static ForbiddenEnchantsPlugin instance;

    private static final double DEFAULT_ENTITY_REACH = 3.0D;
    private static final double FORBIDDEN_REACH = 6.0D;
    private static final double APPLIED_CURSE_LEVEL1_TICKS = 30.0D * 60.0D * 20.0D;
    private static final double APPLIED_CURSE_LEVEL2_TICKS = 60.0D * 60.0D * 20.0D;
    private static final double DEFAULT_STRUCTURE_INJECT_CHANCE = 5.0D;
    private static final double DEFAULT_VAULT_INJECT_CHANCE = 7.5D;
    private static final PotionEffect DIVINE_GLOW_EFFECT = new PotionEffect(PotionEffectType.GLOWING, 20, 0, true, false, false);

    private final Map<UUID, MasqueradeEnchant.MasqueradeState> masqueradeStates = new HashMap<>();
    private final Map<UUID, MiasmaVisualState> miasmaVisualStates = new HashMap<>();
    private final Map<UUID, FearState> fearedMobs = new HashMap<>();
    private final Map<UUID, EnchantmentAllyState> enchantmentAllies = new HashMap<>();
    private final Map<UUID, Long> miasmaPhaseCooldownTicks = new HashMap<>();
    private final Map<UUID, WitherState> witheringTargets = new HashMap<>();
    private final Map<UUID, Long> pocketDimensionCooldownTicks = new HashMap<>();
    private final Map<UUID, Long> sonicPanicCooldownTicks = new HashMap<>();
    private final Map<UUID, Long> staffOfEvokerCooldownTicks = new HashMap<>();
    private final Map<UUID, MarkedState> markedTargets = new HashMap<>();
    private final Map<UUID, Long> wingClipperBlockedUntil = new HashMap<>();
    private final Map<UUID, Long> temporalSicknessNextTeleportTick = new HashMap<>();
    private final Map<UUID, Integer> temporalSicknessKillCount = new HashMap<>();
    private final Map<UUID, Integer> temporalSicknessLastLevel = new HashMap<>();
    private final Map<UUID, Location> lastPlayerDeathLocations = new HashMap<>();
    private final Map<UUID, CharmedPetState> charmedPets = new HashMap<>();
    private final Map<UUID, Location> charmedPetSitAnchors = new HashMap<>();
    private final Map<String, Long> charmedPetToggleDedupTick = new HashMap<>();
    private final Map<UUID, UUID> vexatiousAssistTargets = new HashMap<>();
    private final Map<UUID, Long> vexatiousAssistTargetExpire = new HashMap<>();
    private final Map<UUID, List<UUID>> vexatiousVexesByOwner = new HashMap<>();
    private final Set<UUID> lumberjackActiveBreakers = new java.util.HashSet<>();
    private final Map<UUID, Location> travelDurabilityLastLocation = new HashMap<>();
    private final Map<UUID, Double> travelDurabilityDistance = new HashMap<>();
    private final Map<UUID, Long> fullForceLastProcTick = new HashMap<>();
    private final Map<UUID, Long> fullForceExplosionImmuneUntil = new HashMap<>();
    private final Map<UUID, Long> shockwaveTotemArmedUntil = new HashMap<>();
    private final Map<UUID, Long> fullForceSmashWindowUntil = new HashMap<>();
    private final Map<UUID, Vector> fullForceKnockbackVectors = new HashMap<>();
    private final Map<UUID, Long> fullForceKnockbackUntil = new HashMap<>();
    private final Map<NamespacedKey, Double> structureInjectChances = new HashMap<>();
    private final Map<NamespacedKey, InjectorLootMode> structureInjectLootModes = new HashMap<>();
    private final Map<NamespacedKey, InjectorMysteryState> structureInjectMysteryStates = new HashMap<>();
    private final Map<String, Double> injectorBookRarityWeights = new HashMap<>();
    private final List<LibrarianTradeEntry> librarianTrades = new ArrayList<>();
    private final List<Structure> allStructures = new ArrayList<>();
    private final CompassTrackingService compassTrackingService = new CompassTrackingService();
    private final MysteryItemService mysteryItemService = new MysteryItemService(
            this::enchantBookFactoryServiceInternal,
            this::enchantRuleCoreServiceInternal,
            this::enchantStateServiceInternal,
            this::itemClassificationServiceInternal,
            this::getMysteryKey
    );
    private final FeCatalogService feCatalogService = new FeCatalogService(this);
    private final FePresentationService fePresentationService = new FePresentationService(this);
    private final InjectorMenuService injectorMenuService = new InjectorMenuService(this);
    private final InjectorBookRarityMenuService injectorBookRarityMenuService = new InjectorBookRarityMenuService();
    private final LibrarianTradeMenuService librarianTradeMenuService = new LibrarianTradeMenuService(this);
    private final EnchantToggleMenuService enchantToggleMenuService = new EnchantToggleMenuService(this);
    private final FeMenuService feMenuService = new FeMenuService(this);
    private final EnchantBookFactoryService enchantBookFactoryService = new EnchantBookFactoryService(this);
    private final EnchantStateService enchantStateService = new EnchantStateService(enchantBookFactoryService);
    private final EnchantLifecycleHooksService enchantLifecycleHooksService = new EnchantLifecycleHooksService();
    private final EnchantRuleCoreService enchantRuleCoreService = new EnchantRuleCoreService(enchantStateService);
    private final PlayerAttributeService playerAttributeService = new PlayerAttributeService(enchantRuleCoreService);
    private final EnchantEventRuleService enchantEventRuleService = new EnchantEventRuleService(
            this::enchantStateServiceInternal,
            this::itemClassificationServiceInternal,
            enchantBookFactoryService,
            enchantRuleCoreService
    );
    private final ConfigPersistenceService configPersistenceService = new ConfigPersistenceService(this);
    private final StructureInjectorRuntimeService structureInjectorRuntimeService = new StructureInjectorRuntimeService(this);
    private final LibrarianTradeService librarianTradeService = new LibrarianTradeService(this);
    private final PocketDimensionService pocketDimensionService = new PocketDimensionService();
    private final TemporalSicknessService temporalSicknessService = new TemporalSicknessService(
            this::enchantStateServiceInternal,
            temporalSicknessNextTeleportTick,
            temporalSicknessKillCount,
            temporalSicknessLastLevel
    );
    private final MasqueradeService masqueradeService = new MasqueradeService(this, masqueradeStates);
    private final VexatiousService vexatiousService = new VexatiousService(
            this::vexatiousOwnerKey,
            this::itemCombatServiceInternal,
            vexatiousVexesByOwner,
            vexatiousAssistTargets,
            vexatiousAssistTargetExpire
    );
    private final FearService fearService = new FearService(fearedMobs);
    private final WitheringStrikeService witheringStrikeService = new WitheringStrikeService(witheringTargets);
    private final MiasmaVisualService miasmaVisualService = new MiasmaVisualService(masqueradeService, miasmaVisualStates);
    private final MiasmaFormService miasmaFormService = new MiasmaFormService(this::enchantStateServiceInternal, miasmaVisualService, miasmaPhaseCooldownTicks);
    private final HostileSpawnService hostileSpawnService = new HostileSpawnService();
    private final FullPocketsService fullPocketsService = new FullPocketsService(
            this::itemClassificationServiceInternal,
            this::enchantStateServiceInternal,
            this::mysteryItemServiceInternal,
            this::fullPocketsAppliedKey,
            this,
            this::tickCounterInternal
    );
    private final VisionSenseService visionSenseService = new VisionSenseService(DIVINE_GLOW_EFFECT);
    private final VoidGraspService voidGraspService = new VoidGraspService(this, this::isLootSenseTarget);
    private final GraspCombatService graspCombatService = new GraspCombatService(
            this::enchantStateServiceInternal,
            masqueradeService,
            voidGraspService,
            visionSenseService,
            FORBIDDEN_REACH,
            DEFAULT_ENTITY_REACH
    );
    private final InteractionRestrictionService interactionRestrictionService = new InteractionRestrictionService(
            this::enchantStateServiceInternal,
            miasmaVisualService,
            this::playerEffectServiceInternal,
            this::itemCombatServiceInternal,
            this::tickCounterInternal
    );
    private final ItemCombatService itemCombatService = new ItemCombatService(lumberjackActiveBreakers);
    private final PlayerItemUtilityService playerItemUtilityService = new PlayerItemUtilityService();
    private final PlayerEffectService playerEffectService = new PlayerEffectService(this::enchantStateServiceInternal, itemCombatService, this::wololoKeyInternal, wingClipperBlockedUntil);
    private final FullForceDefenseService fullForceDefenseService = new FullForceDefenseService(
            this::enchantStateServiceInternal,
            playerEffectService,
            miasmaFormService,
            this,
            fullForceExplosionImmuneUntil,
            fullForceKnockbackVectors,
            fullForceKnockbackUntil
    );
    private final EnchantmentAllyService enchantmentAllyService = new EnchantmentAllyService(
            hostileSpawnService,
            this::nearbyEffectsServiceInternal,
            this::itemClassificationServiceInternal,
            this::enchantStateServiceInternal,
            this::playerItemUtilityServiceInternal,
            enchantmentAllies
    );
    private final NearbyEffectsService nearbyEffectsService = new NearbyEffectsService();
    private final LootDeathService lootDeathService = new LootDeathService(this::enchantStateServiceInternal, temporalSicknessService);
    private final TickMaintenanceService tickMaintenanceService = new TickMaintenanceService(
            enchantStateService,
            itemCombatService,
            mysteryItemService,
            compassTrackingService,
            fullForceSmashWindowUntil,
            fullForceKnockbackVectors,
            fullForceKnockbackUntil,
            shockwaveTotemArmedUntil,
            markedTargets,
            charmedPets,
            charmedPetSitAnchors,
            travelDurabilityLastLocation,
            travelDurabilityDistance,
            lastPlayerDeathLocations
    );
    private final ItemClassificationService itemClassificationService = new ItemClassificationService(this);
    private final NameTagLeadService nameTagLeadService = new NameTagLeadService(
            this::enchantStateServiceInternal,
            this::mysteryItemServiceInternal,
            this,
            playerItemUtilityService,
            (long) APPLIED_CURSE_LEVEL1_TICKS,
            (long) APPLIED_CURSE_LEVEL2_TICKS,
            charmedPets,
            charmedPetSitAnchors
    );
    private final CharmedPetInteractionService charmedPetInteractionService = new CharmedPetInteractionService(
            this::enchantStateServiceInternal,
            this,
            playerItemUtilityService,
            charmedPets,
            charmedPetSitAnchors,
            charmedPetToggleDedupTick
    );
    private final PlayerLifecycleService playerLifecycleService = new PlayerLifecycleService(
            masqueradeService,
            miasmaVisualService,
            enchantmentAllyService,
            vexatiousService,
            charmedPetInteractionService,
            lastPlayerDeathLocations,
            travelDurabilityLastLocation,
            travelDurabilityDistance,
            markedTargets,
            wingClipperBlockedUntil,
            temporalSicknessNextTeleportTick,
            temporalSicknessLastLevel,
            fullForceLastProcTick,
            fullForceExplosionImmuneUntil,
            fullForceKnockbackVectors,
            fullForceKnockbackUntil,
            shockwaveTotemArmedUntil,
            fullForceSmashWindowUntil,
            vexatiousAssistTargets,
            vexatiousAssistTargetExpire,
            fearedMobs
    );

    private NamespacedKey bookEnchantKey;
    private NamespacedKey bookLevelKey;
    private NamespacedKey miasmaProjectileKey;
    private NamespacedKey dragonsBreathProjectileKey;
    private NamespacedKey explosiveReactionProjectileKey;
    private NamespacedKey markedProjectileKey;
    private NamespacedKey enchantmentProjectileKey;
    private NamespacedKey fullPocketsAppliedKey;
    private NamespacedKey structureInjectAppliedKey;
    private NamespacedKey wololoConvertedKey;
    private NamespacedKey vexatiousOwnerKey;
    private NamespacedKey mysteryKey;
    private NamespacedKey ricochetKey;
    private boolean structureInjectorEnabled;
    private double structureInjectDefaultChance = DEFAULT_STRUCTURE_INJECT_CHANCE;
    private boolean structureInjectNotifyOnAdd;
    private boolean trialVaultInjectorEnabled;
    private double trialVaultNormalChance = DEFAULT_VAULT_INJECT_CHANCE;
    private double trialVaultOminousChance = DEFAULT_VAULT_INJECT_CHANCE;
    private InjectorLootMode trialVaultNormalLootMode = InjectorLootMode.ALL;
    private InjectorLootMode trialVaultOminousLootMode = InjectorLootMode.ALL;
    private InjectorMysteryState trialVaultNormalMysteryState = InjectorMysteryState.ALL;
    private InjectorMysteryState trialVaultOminousMysteryState = InjectorMysteryState.ALL;
    private boolean injectorRarityApplyToItems = true;
    private boolean librarianTradesEnabled;
    private long tickCounter;

    @Override
    public void onEnable() {
        instance = this;
        bookEnchantKey = new NamespacedKey(this, "book_enchant");
        bookLevelKey = new NamespacedKey(this, "book_level");
        miasmaProjectileKey = new NamespacedKey(this, "miasma_projectile");
        dragonsBreathProjectileKey = new NamespacedKey(this, "dragons_breath_projectile");
        explosiveReactionProjectileKey = new NamespacedKey(this, "explosive_reaction_projectile");
        markedProjectileKey = new NamespacedKey(this, "marked_projectile");
        enchantmentProjectileKey = new NamespacedKey(this, "enchantment_projectile");
        fullPocketsAppliedKey = new NamespacedKey(this, "full_pockets_applied");
        structureInjectAppliedKey = new NamespacedKey(this, "structure_inject_applied");
        wololoConvertedKey = new NamespacedKey(this, "wololo_converted");
        vexatiousOwnerKey = new NamespacedKey(this, "vexatious_owner");
        mysteryKey = new NamespacedKey(this, "mystery_item");
        ricochetKey = new NamespacedKey(this, "ricochet_reflected");
        enchantLifecycleHooksService.onEnable(this);
        enchantStateService.initializeKeys(this);

        try {
            loadStructureInjectorSettings();
            loadEnchantToggleSettings();
            loadLibrarianTradeSettings();
        } catch (Throwable t) {
            structureInjectorEnabled = false;
            structureInjectChances.clear();
            structureInjectLootModes.clear();
            structureInjectMysteryStates.clear();
            injectorBookRarityWeights.clear();
            librarianTradesEnabled = false;
            librarianTrades.clear();
            getLogger().warning("Config-backed admin systems disabled due to startup error: " + t.getClass().getSimpleName() + ": " + t.getMessage());
        }
        ensureMenuPagesBuilt();

        Bukkit.getPluginManager().registerEvents(new ForbiddenEnchantsListener(
                () -> tickCounter,
                interactionRestrictionService,
                fullForceDefenseService,
                fullPocketsService,
                structureInjectorRuntimeService,
                lootDeathService,
                playerLifecycleService,
                nameTagLeadService,
                charmedPetInteractionService,
                feMenuService,
                injectorMenuService,
                injectorBookRarityMenuService,
                librarianTradeMenuService,
                enchantToggleMenuService,
                librarianTradeService,
                enchantEventRuleService,
                graspCombatService
        ), this);

        if (getCommand("fe") != null) {
            FeCommandHandler commandHandler = new FeCommandHandler(this);
            getCommand("fe").setExecutor(commandHandler);
            getCommand("fe").setTabCompleter(commandHandler);
        }

        Bukkit.getScheduler().runTaskTimer(this, this::runTickEffects, 1L, 1L);
    }

    @Override
    public void onDisable() {
        instance = null;
        masqueradeService.clearAll();
        miasmaVisualService.clearAllOnline();
        fearedMobs.clear();
        enchantmentAllies.clear();
        miasmaPhaseCooldownTicks.clear();
        witheringTargets.clear();
        staffOfEvokerCooldownTicks.clear();
        markedTargets.clear();
        wingClipperBlockedUntil.clear();
        temporalSicknessNextTeleportTick.clear();
        temporalSicknessKillCount.clear();
        temporalSicknessLastLevel.clear();
        lastPlayerDeathLocations.clear();
        charmedPets.clear();
        charmedPetSitAnchors.clear();
        charmedPetToggleDedupTick.clear();
        vexatiousAssistTargets.clear();
        vexatiousAssistTargetExpire.clear();
        travelDurabilityLastLocation.clear();
        travelDurabilityDistance.clear();
        feCatalogService.clear();
        fullForceLastProcTick.clear();
        fullForceExplosionImmuneUntil.clear();
        shockwaveTotemArmedUntil.clear();
        fullForceSmashWindowUntil.clear();
        fullForceKnockbackVectors.clear();
        fullForceKnockbackUntil.clear();
        structureInjectChances.clear();
        structureInjectLootModes.clear();
        structureInjectMysteryStates.clear();
        injectorBookRarityWeights.clear();
        librarianTrades.clear();
        allStructures.clear();
        enchantStateService.clearToggles();
        vexatiousService.cleanupAll();
        enchantLifecycleHooksService.onDisable();
    }

    private void loadStructureInjectorSettings() {
        configPersistenceService.loadStructureInjectorSettings();
    }

    void saveStructureInjectorSettings() {
        configPersistenceService.saveStructureInjectorSettings();
    }

    private void loadEnchantToggleSettings() {
        configPersistenceService.loadEnchantToggleSettings();
    }

    void saveEnchantToggleSettings() {
        configPersistenceService.saveEnchantToggleSettings();
    }

    private void loadLibrarianTradeSettings() {
        configPersistenceService.loadLibrarianTradeSettings();
    }

    void saveLibrarianTradeSettings() {
        configPersistenceService.saveLibrarianTradeSettings();
    }

    private void runTickEffects() {
        tickCounter += 1;
        tickMaintenanceService.processFullForceKnockbackOverrides(tickCounter);
        fearService.process(tickCounter);
        witheringStrikeService.process(tickCounter);
        tickMaintenanceService.processMarkedTargets(tickCounter);
        enchantmentAllyService.processTick(tickCounter);
        tickMaintenanceService.processCharmedPets();
        enchantLifecycleHooksService.processTick(tickCounter);

        for (Player player : Bukkit.getOnlinePlayers()) {
            tickMaintenanceService.processMysteryReveals(player);
            tickMaintenanceService.refreshTotemArmedStates(player, tickCounter);
            tickMaintenanceService.processFullForceSmashWindow(player, tickCounter);
            temporalSicknessService.processTick(player, tickCounter);
            dev.cevapi.forbiddenenchants.enchants.EnchantList.INSTANCE.dispatchPlayerTick(player, tickCounter);
            tickMaintenanceService.processTravelDurability(player);
            tickMaintenanceService.processCompassEffects(player);
        }
    }

    void sendActionBarForDuration(@NotNull UUID playerId, @NotNull Component message, long durationTicks) {
        long endTick = tickCounter + Math.max(1L, durationTicks);
        Bukkit.getScheduler().runTaskTimer(this, task -> {
            if (tickCounter > endTick) {
                task.cancel();
                return;
            }
            Player online = Bukkit.getPlayer(playerId);
            if (online == null || !online.isOnline()) {
                task.cancel();
                return;
            }
            online.sendActionBar(message);
        }, 0L, 20L);
    }

    private boolean isLootSenseTarget(@NotNull Material material) {
        return visionSenseService.isLootSenseTarget(material);
    }

    void clearNearbyDarkness(@NotNull Location center, double radius) {
        nearbyEffectsService.clearNearbyDarkness(center, radius);
    }

    public void damageItemByPercent(@NotNull Player player,
                                    @NotNull EquipmentSlot slot,
                                    @NotNull ItemStack item,
                                    double fractionOfMax) {
        playerItemUtilityService.damageItemByPercent(player, slot, item, fractionOfMax);
    }

    public void damageArmorByPercent(@NotNull Player player,
                                     @NotNull EquipmentSlot slot,
                                     @NotNull ItemStack item,
                                     double fractionOfMax) {
        itemCombatService.damageArmorByPercent(player, slot, item, fractionOfMax);
    }

    public void damageHeldItem(@NotNull Player player, @NotNull EquipmentSlot slot, int damageAmount) {
        itemCombatService.damageHeldItem(player, slot, damageAmount);
    }

    public @Nullable ItemStack stealRandomInventoryItem(@NotNull Player player) {
        return itemCombatService.stealRandomInventoryItem(player);
    }

    public @NotNull ItemStack randomMobLootPreview(@NotNull LivingEntity entity) {
        return itemCombatService.randomMobLootPreview(entity);
    }

    public void applyFear(@NotNull Mob mob, @NotNull Player source, long tickCounter) {
        fearService.applyFear(mob, source, tickCounter);
    }

    public void addWitheringTarget(@NotNull UUID targetId, @NotNull UUID sourceId, long tickCounter) {
        witheringStrikeService.addTarget(targetId, sourceId, tickCounter);
    }

    public void clearWitheringTarget(@NotNull UUID targetId) {
        witheringStrikeService.clearTarget(targetId);
    }

    boolean applyBookEnchant(@NotNull ItemStack item, @NotNull EnchantType type, int level) {
        return enchantRuleCoreService.applyBookEnchant(item, type, level);
    }

    void rebuildCustomLore(@NotNull ItemMeta meta) {
        enchantRuleCoreService.rebuildCustomLore(meta);
    }

    public boolean hasAnyVisionHelmetEnchant(@Nullable ItemStack item) {
        return enchantRuleCoreService.hasAnyVisionHelmetEnchant(item);
    }

    public boolean hasHealingTouchForbiddenEnchant(@NotNull ItemMeta meta) {
        return enchantRuleCoreService.hasHealingTouchForbiddenEnchant(meta);
    }

    public void stripHealingTouchForbiddenEnchants(@NotNull ItemMeta meta) {
        enchantRuleCoreService.stripHealingTouchForbiddenEnchants(meta);
    }

    public boolean hasSeekerForbiddenEnchant(@NotNull ItemMeta meta) {
        return enchantRuleCoreService.hasSeekerForbiddenEnchant(meta);
    }

    public void stripSeekerForbiddenEnchants(@NotNull ItemMeta meta) {
        enchantRuleCoreService.stripSeekerForbiddenEnchants(meta);
    }

    public int getEnchantLevel(@Nullable ItemStack item, @NotNull EnchantType type) {
        return enchantStateService.getEnchantLevel(item, type);
    }

    int getStoredEnchantLevel(@NotNull ItemMeta meta, @NotNull EnchantType type) {
        return enchantStateService.getStoredEnchantLevel(meta, type);
    }

    boolean isEnchantUseEnabled(@NotNull EnchantType type) {
        return enchantStateService.isEnchantUseEnabled(type);
    }

    boolean isEnchantSpawnEnabled(@NotNull EnchantType type) {
        return enchantStateService.isEnchantSpawnEnabled(type);
    }

    void setEnchantUseEnabled(@NotNull EnchantType type, boolean enabled) {
        enchantStateService.setEnchantUseEnabled(type, enabled);
    }

    void setEnchantSpawnEnabled(@NotNull EnchantType type, boolean enabled) {
        enchantStateService.setEnchantSpawnEnabled(type, enabled);
    }

    boolean isRetiredEnchant(@NotNull EnchantType type) {
        return enchantStateService.isRetiredEnchant(type);
    }

    @NotNull List<EnchantType> activeEnchantTypes() {
        return enchantStateService.activeEnchantTypes();
    }

    public boolean isArmorPieceForSlot(@Nullable ItemStack stack, @NotNull ArmorSlot slot) {
        return itemClassificationService.isArmorPieceForSlot(stack, slot);
    }

    public boolean isSword(@Nullable ItemStack stack) {
        return itemClassificationService.isSword(stack);
    }

    public boolean isSpear(@Nullable ItemStack stack) {
        return itemClassificationService.isSpear(stack);
    }

    public boolean isHoe(@Nullable ItemStack stack) {
        return itemClassificationService.isHoe(stack);
    }

    public boolean isAxe(@Nullable ItemStack stack) {
        return playerEffectService.isAxe(stack);
    }

    public boolean isRangedWeapon(@Nullable ItemStack stack) {
        return itemClassificationService.isRangedWeapon(stack);
    }

    public boolean isPlayerPartiallySubmerged(@NotNull Player player) {
        return playerEffectService.isPlayerPartiallySubmerged(player);
    }

    ItemStack createBook(@NotNull EnchantType type, int level) {
        return enchantBookFactoryService.createBook(type, level);
    }

    @Nullable BookSpec readBookSpec(@Nullable ItemStack stack) {
        return enchantBookFactoryService.readBookSpec(stack);
    }

    @Nullable ItemStack createEnchantedItem(@NotNull EnchantType type, int level, @NotNull Material material) {
        return enchantBookFactoryService.createEnchantedItem(type, level, material);
    }

    @NotNull ItemStack createMysteryBook(@NotNull ArmorSlot slot) {
        return mysteryItemService.createMysteryBook(slot);
    }

    @Nullable ItemStack createMysteryItem(@NotNull Material material) {
        return mysteryItemService.createMysteryItem(material);
    }

    void ensureMenuPagesBuilt() {
        feCatalogService.ensureMenuPagesBuilt();
    }

    @NotNull List<List<ItemStack>> menuPages() {
        return feCatalogService.menuPages();
    }

    void rememberLastMenuPage(@NotNull UUID playerId, int page) {
        feCatalogService.rememberLastMenuPage(playerId, page);
    }

    void openFeMenu(@NotNull Player player, int page) {
        feMenuService.openMenu(player, page);
    }

    void openEnchantToggleMenu(@NotNull Player player, int page) {
        enchantToggleMenuService.openMenu(player, page);
    }

    @NotNull ItemStack createMenuNavPane(boolean enabled, boolean previous) {
        return fePresentationService.createMenuNavPane(enabled, previous);
    }

    @NotNull ItemStack toMenuDisplayItem(@NotNull ItemStack source) {
        return fePresentationService.toMenuDisplayItem(source);
    }

    public @NotNull String describeItem(@NotNull ItemStack item) {
        return fePresentationService.describeItem(item);
    }

    void sendFeHelp(@NotNull CommandSender sender, @NotNull String label) {
        fePresentationService.sendFeHelp(sender, label);
    }

    void sendEnchantList(@NotNull CommandSender sender) {
        fePresentationService.sendEnchantList(sender);
    }

    void sendFeError(@NotNull CommandSender sender, @NotNull String message) {
        fePresentationService.sendFeError(sender, message);
    }

    void sendFeSuccess(@NotNull CommandSender sender, @NotNull String message) {
        fePresentationService.sendFeSuccess(sender, message);
    }

    int getLastMenuPage(@NotNull UUID playerId) {
        return feCatalogService.getLastMenuPage(playerId);
    }

    @NotNull NamespacedKey getMysteryKey() {
        return mysteryKey;
    }

    private @NotNull ItemClassificationService itemClassificationServiceInternal() {
        return itemClassificationService;
    }

    private @NotNull EnchantStateService enchantStateServiceInternal() {
        return enchantStateService;
    }

    private @NotNull EnchantBookFactoryService enchantBookFactoryServiceInternal() {
        return enchantBookFactoryService;
    }

    private @NotNull PlayerItemUtilityService playerItemUtilityServiceInternal() {
        return playerItemUtilityService;
    }

    private @NotNull MysteryItemService mysteryItemServiceInternal() {
        return mysteryItemService;
    }

    private @NotNull PlayerEffectService playerEffectServiceInternal() {
        return playerEffectService;
    }

    private @NotNull ItemCombatService itemCombatServiceInternal() {
        return itemCombatService;
    }

    private @NotNull MasqueradeService masqueradeServiceInternal() {
        return masqueradeService;
    }

    private @NotNull NearbyEffectsService nearbyEffectsServiceInternal() {
        return nearbyEffectsService;
    }

    private @NotNull EnchantRuleCoreService enchantRuleCoreServiceInternal() {
        return enchantRuleCoreService;
    }

    private long tickCounterInternal() {
        return tickCounter;
    }

    private @NotNull NamespacedKey miasmaProjectileKeyInternal() {
        return miasmaProjectileKey;
    }

    private @NotNull NamespacedKey dragonsBreathProjectileKeyInternal() {
        return dragonsBreathProjectileKey;
    }

    private @NotNull NamespacedKey explosiveReactionProjectileKeyInternal() {
        return explosiveReactionProjectileKey;
    }

    private @NotNull NamespacedKey enchantmentProjectileKeyInternal() {
        return enchantmentProjectileKey;
    }

    private @NotNull NamespacedKey markedProjectileKeyInternal() {
        return markedProjectileKey;
    }

    @NotNull NamespacedKey bookEnchantKey() {
        return bookEnchantKey;
    }

    @NotNull NamespacedKey bookLevelKey() {
        return bookLevelKey;
    }

    @NotNull NamespacedKey enchantLevelKeyForRules(@NotNull EnchantType type) {
        return enchantStateService.enchantLevelKey(type);
    }

    public @NotNull NamespacedKey ricochetKey() {
        return ricochetKey;
    }

    public static @NotNull ForbiddenEnchantsPlugin instance() {
        ForbiddenEnchantsPlugin current = instance;
        if (current == null) {
            throw new IllegalStateException("ForbiddenEnchantsPlugin is not enabled yet");
        }
        return current;
    }

    @NotNull NamespacedKey vexatiousOwnerKey() {
        return vexatiousOwnerKey;
    }

    private @NotNull NamespacedKey wololoKeyInternal() {
        return wololoConvertedKey;
    }

    @NotNull NamespacedKey fullPocketsAppliedKey() {
        return fullPocketsAppliedKey;
    }

    @NotNull NamespacedKey structureInjectAppliedKey() {
        return structureInjectAppliedKey;
    }

    boolean isStructureInjectorEnabled() {
        return structureInjectorEnabled;
    }

    void setStructureInjectorEnabled(boolean enabled) {
        structureInjectorEnabled = enabled;
    }

    double getStructureInjectDefaultChance() {
        return structureInjectDefaultChance;
    }

    void setStructureInjectDefaultChance(double chance) {
        structureInjectDefaultChance = chance;
    }

    boolean isStructureInjectNotifyOnAdd() {
        return structureInjectNotifyOnAdd;
    }

    void setStructureInjectNotifyOnAdd(boolean notifyOnAdd) {
        structureInjectNotifyOnAdd = notifyOnAdd;
    }

    boolean isTrialVaultInjectorEnabled() {
        return trialVaultInjectorEnabled;
    }

    void setTrialVaultInjectorEnabled(boolean enabled) {
        trialVaultInjectorEnabled = enabled;
    }

    double getTrialVaultNormalChance() {
        return trialVaultNormalChance;
    }

    void setTrialVaultNormalChance(double chance) {
        trialVaultNormalChance = chance;
    }

    @NotNull InjectorLootMode getTrialVaultNormalLootMode() {
        return trialVaultNormalLootMode;
    }

    void setTrialVaultNormalLootMode(@NotNull InjectorLootMode mode) {
        trialVaultNormalLootMode = mode == null ? InjectorLootMode.ALL : mode;
    }

    @NotNull InjectorMysteryState getTrialVaultNormalMysteryState() {
        return trialVaultNormalMysteryState;
    }

    void setTrialVaultNormalMysteryState(@NotNull InjectorMysteryState state) {
        trialVaultNormalMysteryState = state == null ? InjectorMysteryState.ALL : state;
    }

    double getTrialVaultOminousChance() {
        return trialVaultOminousChance;
    }

    void setTrialVaultOminousChance(double chance) {
        trialVaultOminousChance = chance;
    }

    @NotNull InjectorLootMode getTrialVaultOminousLootMode() {
        return trialVaultOminousLootMode;
    }

    void setTrialVaultOminousLootMode(@NotNull InjectorLootMode mode) {
        trialVaultOminousLootMode = mode == null ? InjectorLootMode.ALL : mode;
    }

    @NotNull InjectorMysteryState getTrialVaultOminousMysteryState() {
        return trialVaultOminousMysteryState;
    }

    void setTrialVaultOminousMysteryState(@NotNull InjectorMysteryState state) {
        trialVaultOminousMysteryState = state == null ? InjectorMysteryState.ALL : state;
    }

    @NotNull Map<NamespacedKey, Double> structureInjectChances() {
        return structureInjectChances;
    }

    @NotNull Map<NamespacedKey, InjectorLootMode> structureInjectLootModes() {
        return structureInjectLootModes;
    }

    @NotNull Map<NamespacedKey, InjectorMysteryState> structureInjectMysteryStates() {
        return structureInjectMysteryStates;
    }

    @NotNull Map<String, Double> injectorBookRarityWeights() {
        return injectorBookRarityWeights;
    }

    boolean isInjectorRarityApplyToItems() {
        return injectorRarityApplyToItems;
    }

    void setInjectorRarityApplyToItems(boolean applyToItems) {
        injectorRarityApplyToItems = applyToItems;
    }

    double injectorBookRarityWeight(@NotNull EnchantType type, int level) {
        return injectorBookRarityWeights.getOrDefault(injectorBookRarityKey(type, level), 1.0D);
    }

    void setInjectorBookRarityWeight(@NotNull EnchantType type, int level, double weight) {
        String key = injectorBookRarityKey(type, level);
        double clamped = Math.max(0.0D, Math.min(1000.0D, weight));
        double rounded = Math.round(clamped * 10.0D) / 10.0D;
        if (rounded <= 0.0D || Math.abs(rounded - 1.0D) < 0.0001D) {
            injectorBookRarityWeights.remove(key);
            return;
        }
        injectorBookRarityWeights.put(key, rounded);
    }

    @NotNull String injectorBookRarityKey(@NotNull EnchantType type, int level) {
        return type.arg + ":" + level;
    }

    @NotNull InjectorLootMode structureInjectLootMode(@NotNull NamespacedKey key) {
        return structureInjectLootModes.getOrDefault(key, InjectorLootMode.ALL);
    }

    void setStructureInjectLootMode(@NotNull NamespacedKey key, @NotNull InjectorLootMode mode) {
        if (mode != InjectorLootMode.ALL) {
            structureInjectLootModes.put(key, mode);
            return;
        }
        structureInjectLootModes.remove(key);
    }

    @NotNull InjectorMysteryState structureInjectMysteryState(@NotNull NamespacedKey key) {
        return structureInjectMysteryStates.getOrDefault(key, InjectorMysteryState.ALL);
    }

    void setStructureInjectMysteryState(@NotNull NamespacedKey key, @NotNull InjectorMysteryState state) {
        if (state != InjectorMysteryState.ALL) {
            structureInjectMysteryStates.put(key, state);
            return;
        }
        structureInjectMysteryStates.remove(key);
    }

    @NotNull List<Structure> allStructuresStoreForConfig() {
        return allStructures;
    }

    @NotNull List<Structure> allStructures() {
        if (allStructures.isEmpty()) {
            configPersistenceService.refreshAllStructures();
        }
        return allStructures;
    }

    void openInjectorConfigMenu(@NotNull Player target) {
        injectorMenuService.openMenu(target, InjectorMenuMode.CONFIGURED, 0);
    }

    void openInjectorBookRarityMenu(@NotNull Player target, int page) {
        injectorBookRarityMenuService.openMenu(target, this, page);
    }

    void openLibrarianTradeMenu(@NotNull Player target, int page) {
        librarianTradeMenuService.openMenu(target, page);
    }

    void disableStructureInjectorDueToRuntimeError(@NotNull Throwable t) {
        structureInjectorEnabled = false;
        getLogger().warning("Structure injector disabled at runtime due to error: " + t.getClass().getSimpleName() + ": " + t.getMessage());
    }

    boolean isMaterialValidForEnchant(@NotNull Material material, @NotNull EnchantType type) {
        return itemClassificationService.isMaterialValidForEnchant(material, type);
    }

    boolean isLibrarianTradesEnabled() {
        return librarianTradesEnabled;
    }

    void setLibrarianTradesEnabled(boolean enabled) {
        librarianTradesEnabled = enabled;
    }

    @NotNull List<LibrarianTradeEntry> librarianTrades() {
        return librarianTrades;
    }

    public void giveOrDrop(@NotNull Player target, @NotNull ItemStack item) {
        Map<Integer, ItemStack> overflow = target.getInventory().addItem(item);
        for (ItemStack stack : overflow.values()) {
            target.getWorld().dropItemNaturally(target.getLocation(), stack);
        }
    }

    public void revealMysteryItemIfNeeded(@Nullable ItemStack item, @Nullable Player owner, @Nullable EquipmentSlot slot) {
        mysteryItemService.revealMysteryItemIfNeeded(item, owner, slot);
    }

    public void pushVexatiousAssistTarget(@NotNull UUID ownerId, @NotNull LivingEntity target, long tickCounter) {
        vexatiousService.pushAssistTarget(ownerId, target, tickCounter);
    }

    public long shockwaveArmedUntil(@NotNull UUID playerId) {
        return shockwaveTotemArmedUntil.getOrDefault(playerId, 0L);
    }

    public @Nullable Location findPocketDimensionSafeLocation(@NotNull Location origin, double horizontalRadius) {
        return pocketDimensionService.findSafeLocation(origin, horizontalRadius);
    }

    public boolean isMasquerading(@NotNull Player player) {
        return masqueradeService.isMasquerading(player);
    }

    public void startMasquerade(@NotNull Player player) {
        masqueradeService.start(player);
    }

    public void maintainMasquerade(@NotNull Player player) {
        masqueradeService.maintain(player);
    }

    public void clearMasquerade(@NotNull Player player) {
        masqueradeService.clear(player);
    }

    public boolean hasMiasmaForm(@NotNull Player player) {
        return miasmaVisualService.hasForm(player);
    }

    public boolean hasUnyieldingEquipped(@NotNull Player player) {
        return miasmaFormService.hasUnyieldingEquipped(player);
    }

    public boolean hasMiasmaEnchantEquipped(@NotNull Player player) {
        return miasmaFormService.hasMiasmaEnchantEquipped(player);
    }

    public void applyMiasmaForm(@NotNull Player player, long tickCounter) {
        miasmaFormService.applyForm(player, tickCounter);
    }

    public void clearMiasmaVisual(@NotNull Player player) {
        miasmaVisualService.clear(player);
    }

    public boolean isAllowedMiasmaFormDamage(@NotNull EntityDamageEvent event) {
        return miasmaFormService.isAllowedDamage(event);
    }

    public void applyLaunchFlightState(@NotNull Player player, @Nullable ItemStack chestplate) {
        playerEffectService.applyLaunchFlightState(player, chestplate);
    }

    public void applyAscension(@NotNull Player player) {
        playerEffectService.applyAscension(player);
    }

    public void applyAquaticSacrifice(@NotNull Player player, long tickCounter) {
        playerEffectService.applyAquaticSacrifice(player, tickCounter);
    }

    public void applyCreepersInfluence(@NotNull Player player) {
        playerEffectService.applyCreepersInfluence(player);
    }

    public void applyWololo(@NotNull Player player) {
        playerEffectService.applyWololo(player);
    }

    public void applyDivineVision(@NotNull Player player, int level) {
        visionSenseService.applyDivineVision(player, level);
    }

    public void applyMinersIntuition(@NotNull Player player, @NotNull ItemStack helmet, int level) {
        visionSenseService.applyMinersIntuition(player, helmet, level);
    }

    public void applyLootSense(@NotNull Player player, int level) {
        visionSenseService.applyLootSense(player, level);
    }

    public void applyHatedOneAggro(@NotNull Player player, int level) {
        hostileSpawnService.applyHatedOneAggro(player, level);
    }

    public void trySpawnHatedOneWave(@NotNull Player player, int level) {
        hostileSpawnService.trySpawnHatedOneWave(player, level);
    }

    public @Nullable Location findNearbySpawnLocation(@NotNull Player player, double minDistance, double maxDistance) {
        return hostileSpawnService.findNearbySpawnLocation(player, minDistance, maxDistance);
    }

    public void maintainVexatious(@NotNull Player player, @NotNull ItemStack helmet, int level, long tickCounter) {
        vexatiousService.maintain(player, helmet, level, tickCounter);
    }

    public void clearVexatious(@NotNull UUID ownerId) {
        vexatiousService.clear(ownerId);
    }

    public void pullNearbyExperienceOrbs(@NotNull Player player, double radius) {
        nearbyEffectsService.pullNearbyExperienceOrbs(player, radius);
    }

    public void pullNearbyItems(@NotNull Player player, double radius) {
        nearbyEffectsService.pullNearbyItems(player, radius);
    }

    public void setPlayerReach(@NotNull Player player, double blockRange, double entityRange) {
        playerAttributeService.setPlayerReach(player, blockRange, entityRange);
    }

    public void enforceHelmetRestrictions(@NotNull Player player, @NotNull ItemStack helmet) {
        playerAttributeService.enforceHelmetRestrictions(player, helmet);
    }

    public void enforceDurabilityCap(@NotNull ItemStack item,
                                     int maxRemainingDurability,
                                     @NotNull Player owner,
                                     @NotNull EquipmentSlot slot) {
        playerAttributeService.enforceDurabilityCap(item, maxRemainingDurability, owner, slot);
    }

    public boolean triggerPocketDimension(@NotNull Player player) {
        return pocketDimensionService.trigger(player);
    }

    public void triggerSonicPanic(@NotNull Player player, @NotNull ItemStack weapon) {
        playerEffectService.triggerSonicPanic(player, weapon);
    }

    public long staffOfEvokerReadyTick(@NotNull UUID playerId) {
        return staffOfEvokerCooldownTicks.getOrDefault(playerId, 0L);
    }

    public void setStaffOfEvokerReadyTick(@NotNull UUID playerId, long readyTick) {
        staffOfEvokerCooldownTicks.put(playerId, readyTick);
    }

    public void setWingClipperBlockedUntil(@NotNull UUID playerId, long blockedUntil) {
        wingClipperBlockedUntil.put(playerId, blockedUntil);
    }

    public @NotNull NamespacedKey miasmaProjectileKey() {
        return miasmaProjectileKey;
    }

    public @NotNull NamespacedKey dragonsBreathProjectileKey() {
        return dragonsBreathProjectileKey;
    }

    public @NotNull NamespacedKey explosiveReactionProjectileKey() {
        return explosiveReactionProjectileKey;
    }

    public @NotNull NamespacedKey enchantmentProjectileKey() {
        return enchantmentProjectileKey;
    }

    public @NotNull NamespacedKey markedProjectileKey() {
        return markedProjectileKey;
    }

    public void applyCharmProjectile(@NotNull Projectile projectile, int level, @Nullable Entity hitEntity, long tickCounter) {
        enchantmentAllyService.applyProjectile(projectile, level, hitEntity, tickCounter);
    }

    public void applyMarkedTarget(@NotNull Player target, @NotNull UUID ownerId, long expireTick) {
        target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, (int) Math.max(1L, expireTick - tickCounter), 0, true, false, true), true);
        markedTargets.put(target.getUniqueId(), new MarkedState(ownerId, expireTick));
    }

    public double applyMarkedDamageBoost(@NotNull Player target, @NotNull UUID damagerId, double baseDamage, long currentTick) {
        MarkedState state = markedTargets.get(target.getUniqueId());
        if (state == null) {
            return baseDamage;
        }
        if (dev.cevapi.forbiddenenchants.enchants.EnchantList.INSTANCE.marked().isExpired(state.expireTick(), currentTick)) {
            markedTargets.remove(target.getUniqueId());
            return baseDamage;
        }
        return dev.cevapi.forbiddenenchants.enchants.EnchantList.INSTANCE.marked()
                .applyDamageBoost(baseDamage, state.ownerId().equals(damagerId), true);
    }

    public boolean isCharmedPet(@NotNull UUID mobId) {
        return charmedPets.containsKey(mobId);
    }

    public @Nullable UUID allyOwnerId(@NotNull UUID mobId) {
        EnchantmentAllyState state = enchantmentAllyService.stateFor(mobId);
        return state == null ? null : state.ownerId();
    }

    public boolean isAllyMob(@NotNull UUID mobId) {
        return enchantmentAllyService.isAlly(mobId);
    }

    public boolean hasHatedOne(@NotNull Player player) {
        return enchantStateService.getEnchantLevel(player.getInventory().getHelmet(), EnchantType.THE_HATED_ONE) > 0;
    }

    public void cancelMobTarget(@NotNull EntityTargetLivingEntityEvent event, @NotNull Mob mob) {
        event.setCancelled(true);
        mob.setTarget(null);
    }

}

