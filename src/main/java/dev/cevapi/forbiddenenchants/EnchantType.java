package dev.cevapi.forbiddenenchants;

import dev.cevapi.forbiddenenchants.enchants.AppliedCurseEnchant;
import dev.cevapi.forbiddenenchants.enchants.AdministrativeAspirationsEnchant;
import dev.cevapi.forbiddenenchants.enchants.AquaticSacrificeEnchant;
import dev.cevapi.forbiddenenchants.enchants.AscensionEnchant;
import dev.cevapi.forbiddenenchants.enchants.BlindnessEnchant;
import dev.cevapi.forbiddenenchants.enchants.BorgTechnologyEnchant;
import dev.cevapi.forbiddenenchants.enchants.BedTimeEnchant;
import dev.cevapi.forbiddenenchants.enchants.CharmEnchant;
import dev.cevapi.forbiddenenchants.enchants.CharmedPetEnchant;
import dev.cevapi.forbiddenenchants.enchants.CreepersInfluenceEnchant;
import dev.cevapi.forbiddenenchants.enchants.CursedMagnetismEnchant;
import dev.cevapi.forbiddenenchants.enchants.DisarmEnchant;
import dev.cevapi.forbiddenenchants.enchants.DivineVisionEnchant;
import dev.cevapi.forbiddenenchants.enchants.DragonsBreathEnchant;
import dev.cevapi.forbiddenenchants.enchants.EvokersRevengeEnchant;
import dev.cevapi.forbiddenenchants.enchants.ExtendedGraspEnchant;
import dev.cevapi.forbiddenenchants.enchants.ExplosiveReactionEnchant;
import dev.cevapi.forbiddenenchants.enchants.ForbiddenAgilityEnchant;
import dev.cevapi.forbiddenenchants.enchants.ForbiddenEnchantDefinition;
import dev.cevapi.forbiddenenchants.enchants.FarmersDreamEnchant;
import dev.cevapi.forbiddenenchants.enchants.FireballEnchant;
import dev.cevapi.forbiddenenchants.enchants.FullForceEnchant;
import dev.cevapi.forbiddenenchants.enchants.FullPocketsEnchant;
import dev.cevapi.forbiddenenchants.enchants.GetOverHereEnchant;
import dev.cevapi.forbiddenenchants.enchants.GraveRobberEnchant;
import dev.cevapi.forbiddenenchants.enchants.GreedEnchant;
import dev.cevapi.forbiddenenchants.enchants.HealingTouchEnchant;
import dev.cevapi.forbiddenenchants.enchants.InciteFearEnchant;
import dev.cevapi.forbiddenenchants.enchants.IllusionersRevengeEnchant;
import dev.cevapi.forbiddenenchants.enchants.JointSleepEnchant;
import dev.cevapi.forbiddenenchants.enchants.KismetEnchant;
import dev.cevapi.forbiddenenchants.enchants.LavaStepEnchant;
import dev.cevapi.forbiddenenchants.enchants.LaunchEnchant;
import dev.cevapi.forbiddenenchants.enchants.LifeSpiritEnchant;
import dev.cevapi.forbiddenenchants.enchants.LifeStealEnchant;
import dev.cevapi.forbiddenenchants.enchants.LimitlessVisionEnchant;
import dev.cevapi.forbiddenenchants.enchants.LockedOutEnchant;
import dev.cevapi.forbiddenenchants.enchants.LootSenseEnchant;
import dev.cevapi.forbiddenenchants.enchants.LumberjackEnchant;
import dev.cevapi.forbiddenenchants.enchants.MarkedEnchant;
import dev.cevapi.forbiddenenchants.enchants.MasqueradeEnchant;
import dev.cevapi.forbiddenenchants.enchants.MagnetismEnchant;
import dev.cevapi.forbiddenenchants.enchants.MiasmaEnchant;
import dev.cevapi.forbiddenenchants.enchants.MiasmaFormEnchant;
import dev.cevapi.forbiddenenchants.enchants.MinersVoidStepEnchant;
import dev.cevapi.forbiddenenchants.enchants.MinersIntuitionEnchant;
import dev.cevapi.forbiddenenchants.enchants.MujahideenEnchant;
import dev.cevapi.forbiddenenchants.enchants.NoFallEnchant;
import dev.cevapi.forbiddenenchants.enchants.OnePlusEnchant;
import dev.cevapi.forbiddenenchants.enchants.OutOfPhaseEnchant;
import dev.cevapi.forbiddenenchants.enchants.PettyThiefEnchant;
import dev.cevapi.forbiddenenchants.enchants.PocketDimensionEnchant;
import dev.cevapi.forbiddenenchants.enchants.PocketSeekerEnchant;
import dev.cevapi.forbiddenenchants.enchants.ProudWarriorEnchant;
import dev.cevapi.forbiddenenchants.enchants.QuitterEnchant;
import dev.cevapi.forbiddenenchants.enchants.RicochetEnchant;
import dev.cevapi.forbiddenenchants.enchants.ShieldKnockbackEnchant;
import dev.cevapi.forbiddenenchants.enchants.ShockwaveEnchant;
import dev.cevapi.forbiddenenchants.enchants.SilenceEnchant;
import dev.cevapi.forbiddenenchants.enchants.SiskosSolutionEnchant;
import dev.cevapi.forbiddenenchants.enchants.SonicPanicEnchant;
import dev.cevapi.forbiddenenchants.enchants.StaffOfTheEvokerEnchant;
import dev.cevapi.forbiddenenchants.enchants.TemporalSicknessEnchant;
import dev.cevapi.forbiddenenchants.enchants.TemporalDisplacementEnchant;
import dev.cevapi.forbiddenenchants.enchants.TheHatedOneEnchant;
import dev.cevapi.forbiddenenchants.enchants.TheDuplicatorEnchant;
import dev.cevapi.forbiddenenchants.enchants.ThePhilosophersBookEnchant;
import dev.cevapi.forbiddenenchants.enchants.ThePretenderEnchant;
import dev.cevapi.forbiddenenchants.enchants.TheSeekerEnchant;
import dev.cevapi.forbiddenenchants.enchants.TheUnyieldingEnchant;
import dev.cevapi.forbiddenenchants.enchants.TrackerEnchant;
import dev.cevapi.forbiddenenchants.enchants.TrueSilenceEnchant;
import dev.cevapi.forbiddenenchants.enchants.VexatiousEnchant;
import dev.cevapi.forbiddenenchants.enchants.VitalityThiefEnchant;
import dev.cevapi.forbiddenenchants.enchants.VoidGraspEnchant;
import dev.cevapi.forbiddenenchants.enchants.VoidStickEnchant;
import dev.cevapi.forbiddenenchants.enchants.WingClipperEnchant;
import dev.cevapi.forbiddenenchants.enchants.InstantDeathEnchant;
import dev.cevapi.forbiddenenchants.enchants.WitheringStrikeEnchant;
import dev.cevapi.forbiddenenchants.enchants.WololoEnchant;
import dev.cevapi.forbiddenenchants.enchants.WarpNineFiveEnchant;
import dev.cevapi.forbiddenenchants.enchants.InfectedEnchant;
import dev.cevapi.forbiddenenchants.enchants.SwapEnchant;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public enum EnchantType {
    DIVINE_VISION(1, new DivineVisionEnchant()),
    MINERS_INTUITION(2, new MinersIntuitionEnchant()),
    LOOT_SENSE(3, new LootSenseEnchant()),
    EXTENDED_GRASP(4, new ExtendedGraspEnchant()),
    VOID_GRASP(5, new VoidGraspEnchant()),
    MASQUERADE(6, new MasqueradeEnchant()),
    ASCENSION(7, new AscensionEnchant()),
    INCITE_FEAR(8, new InciteFearEnchant()),
    BLINDNESS(9, new BlindnessEnchant()),
    MIASMA(10, new MiasmaEnchant()),
    CHARM(11, new CharmEnchant()),
    MIASMA_FORM(12, new MiasmaFormEnchant()),
    AQUATIC_SACRIFICE(13, new AquaticSacrificeEnchant()),
    THE_HATED_ONE(14, new TheHatedOneEnchant()),
    WITHERING_STRIKE(15, new WitheringStrikeEnchant()),
    HEALING_TOUCH(16, new HealingTouchEnchant()),
    FULL_POCKETS(17, new FullPocketsEnchant()),
    DRAGONS_BREATH(18, new DragonsBreathEnchant()),
    EXPLOSIVE_REACTION(19, new ExplosiveReactionEnchant()),
    THE_UNYIELDING(20, new TheUnyieldingEnchant()),
    FORBIDDEN_AGILITY(21, new ForbiddenAgilityEnchant()),
    POCKET_DIMENSION(22, new PocketDimensionEnchant()),
    PETTY_THIEF(23, new PettyThiefEnchant()),
    LUMBERJACK(24, new LumberjackEnchant()),
    SONIC_PANIC(25, new SonicPanicEnchant()),
    CREEPERS_INFLUENCE(26, new CreepersInfluenceEnchant()),
    STAFF_OF_THE_EVOKER(27, new StaffOfTheEvokerEnchant()),
    VEXATIOUS(28, new VexatiousEnchant()),
    WOLOLO(29, new WololoEnchant()),
    LOCKED_OUT(30, new LockedOutEnchant()),
    EVOKERS_REVENGE(31, new EvokersRevengeEnchant()),
    THE_SEEKER(32, new TheSeekerEnchant()),
    DISARM(33, new DisarmEnchant()),
    MARKED(34, new MarkedEnchant()),
    GREED(35, new GreedEnchant()),
    WING_CLIPPER(36, new WingClipperEnchant()),
    LAUNCH(37, new LaunchEnchant()),
    FULL_FORCE(38, new FullForceEnchant()),
    TEMPORAL_SICKNESS(39, new TemporalSicknessEnchant()),
    GRAVE_ROBBER(40, new GraveRobberEnchant()),
    POCKET_SEEKER(41, new PocketSeekerEnchant()),
    CHARMED_PET(42, new CharmedPetEnchant()),
    APPLIED_CURSE(43, new AppliedCurseEnchant()),
    GET_OVER_HERE(44, new GetOverHereEnchant()),
    MUJAHIDEEN(45, new MujahideenEnchant()),
    SHIELD_KNOCKBACK(46, new ShieldKnockbackEnchant()),
    RICOCHET(47, new RicochetEnchant()),
    SHOCKWAVE(48, new ShockwaveEnchant()),
    PROUD_WARRIOR(49, new ProudWarriorEnchant()),
    SISKOS_SOLUTION(50, new SiskosSolutionEnchant()),
    NO_FALL(51, new NoFallEnchant()),
    BORG_TECHNOLOGY(52, new BorgTechnologyEnchant()),
    WARP_NINE_FIVE(53, new WarpNineFiveEnchant()),
    TRACKER(54, new TrackerEnchant()),
    THE_PRETENDER(55, new ThePretenderEnchant()),
    OUT_OF_PHASE(56, new OutOfPhaseEnchant()),
    SILENCE(57, new SilenceEnchant()),
    QUITTER(58, new QuitterEnchant()),
    INFECTED(59, new InfectedEnchant()),
    THE_DUPLICATOR(60, new TheDuplicatorEnchant()),
    THE_PHILOSOPHERS_BOOK(61, new ThePhilosophersBookEnchant()),
    JOINT_SLEEP(62, new JointSleepEnchant()),
    ILLUSIONERS_REVENGE(63, new IllusionersRevengeEnchant()),
    INSTANT_DEATH(64, new InstantDeathEnchant()),
    LIMITLESS_VISION(65, new LimitlessVisionEnchant()),
    ONE_PLUS(66, new OnePlusEnchant()),
    TEMPORAL_DISPLACEMENT(67, new TemporalDisplacementEnchant()),
    KISMET(68, new KismetEnchant()),
    ADMINISTRATIVE_ASPIRATIONS(69, new AdministrativeAspirationsEnchant()),
    MINERS_VOID_STEP(70, new MinersVoidStepEnchant()),
    SWAP(71, new SwapEnchant()),
    FARMERS_DREAM(72, new FarmersDreamEnchant()),
    LAVA_STEP(73, new LavaStepEnchant()),
    VITALITY_THIEF(74, new VitalityThiefEnchant()),
    LIFE_SPIRIT(75, new LifeSpiritEnchant()),
    LIFE_STEAL(76, new LifeStealEnchant()),
    VOID_STICK(77, new VoidStickEnchant()),
    FIREBALL(78, new FireballEnchant()),
    MAGNETISM(79, new MagnetismEnchant()),
    CURSED_MAGNETISM(80, new CursedMagnetismEnchant()),
    TRUE_SILENCE(81, new TrueSilenceEnchant()),
    BED_TIME(82, new BedTimeEnchant());

    private static final Map<String, EnchantType> BY_ARG = new HashMap<>();
    private static final Map<Integer, EnchantType> BY_MODEL_TYPE_INDEX = new HashMap<>();
    private static final Map<EnchantType, String> EXCLUSIVE_GROUP = new EnumMap<>(EnchantType.class);
    private static final Map<EnchantType, Set<EnchantType>> INCOMPATIBLE_WITH = new EnumMap<>(EnchantType.class);
    private static final Set<EnchantType> APPLIES_BINDING_CURSE = EnumSet.noneOf(EnchantType.class);
    private static final Set<EnchantType> STRIPS_VANILLA_ENCHANTS = EnumSet.noneOf(EnchantType.class);
    private static final Set<EnchantType> STRIPS_MENDING_UNBREAKING = EnumSet.noneOf(EnchantType.class);
    private static final Set<EnchantType> DURABILITY_PENALTY_ENCHANTS = EnumSet.noneOf(EnchantType.class);
    private static final Set<EnchantType> REQUIRES_NO_OTHER_ENCHANTS = EnumSet.noneOf(EnchantType.class);
    private static final Set<EnchantType> REQUIRES_SOLO_ON_TRIDENT = EnumSet.noneOf(EnchantType.class);

    static {
        for (EnchantType type : values()) {
            BY_ARG.put(type.arg, type);
            EnchantType replaced = BY_MODEL_TYPE_INDEX.put(type.modelTypeIndex, type);
            if (replaced != null) {
                throw new IllegalStateException("Duplicate modelTypeIndex: " + type.modelTypeIndex);
            }
            for (String alias : type.definition.aliases()) {
                BY_ARG.putIfAbsent(alias, type);
            }
        }

        setExclusiveGroup("helmet_primary", DIVINE_VISION, MINERS_INTUITION, LOOT_SENSE, AQUATIC_SACRIFICE, THE_HATED_ONE);
        setExclusiveGroup("chest_primary", EXTENDED_GRASP, VOID_GRASP, MIASMA_FORM);
        setExclusiveGroup("boots_primary", MASQUERADE, ASCENSION, FARMERS_DREAM, LAVA_STEP, TRUE_SILENCE);
        setExclusiveGroup("ranged_primary", MIASMA, CHARM, DRAGONS_BREATH, EXPLOSIVE_REACTION);
        setExclusiveGroup("rod_primary", VOID_STICK, FIREBALL);
        setExclusiveGroup("hoe_primary", HEALING_TOUCH, PETTY_THIEF);
        setExclusiveGroup("compass_primary", GRAVE_ROBBER, POCKET_SEEKER);
        setExclusiveGroup("nametag_primary", CHARMED_PET, APPLIED_CURSE);
        setExclusiveGroup("potion_primary", OUT_OF_PHASE, SILENCE, QUITTER, INFECTED, JOINT_SLEEP, LIMITLESS_VISION, ONE_PLUS, TEMPORAL_DISPLACEMENT, MINERS_VOID_STEP, SWAP, VITALITY_THIEF, LIFE_SPIRIT, LIFE_STEAL, BED_TIME);

        addMutualIncompatibility(DRAGONS_BREATH, EXPLOSIVE_REACTION);

        APPLIES_BINDING_CURSE.addAll(EnumSet.of(
                AQUATIC_SACRIFICE,
                THE_HATED_ONE,
                WOLOLO,
                LOCKED_OUT,
                EVOKERS_REVENGE,
                ILLUSIONERS_REVENGE,
                GREED,
                TEMPORAL_SICKNESS,
                PROUD_WARRIOR,
                SISKOS_SOLUTION,
                KISMET,
                CURSED_MAGNETISM
        ));

        STRIPS_VANILLA_ENCHANTS.addAll(EnumSet.of(DIVINE_VISION, MINERS_INTUITION, LOOT_SENSE));
        STRIPS_MENDING_UNBREAKING.addAll(EnumSet.of(HEALING_TOUCH, THE_SEEKER, ADMINISTRATIVE_ASPIRATIONS, FARMERS_DREAM, LAVA_STEP));
        DURABILITY_PENALTY_ENCHANTS.addAll(EnumSet.of(
                DIVINE_VISION,
                MINERS_INTUITION,
                LOOT_SENSE,
                MASQUERADE,
                ASCENSION,
                CHARM,
                HEALING_TOUCH,
                LUMBERJACK,
                VEXATIOUS,
                THE_SEEKER,
                LAUNCH,
                NO_FALL,
                WARP_NINE_FIVE,
                KISMET,
                ADMINISTRATIVE_ASPIRATIONS
        ));

        REQUIRES_NO_OTHER_ENCHANTS.addAll(EnumSet.of(
                DRAGONS_BREATH,
                EXPLOSIVE_REACTION,
                PROUD_WARRIOR,
                SISKOS_SOLUTION,
                BORG_TECHNOLOGY,
                WARP_NINE_FIVE
        ));
        REQUIRES_SOLO_ON_TRIDENT.addAll(EnumSet.of(WITHERING_STRIKE, INSTANT_DEATH));
    }

    private final int modelTypeIndex;
    private final ForbiddenEnchantDefinition definition;
    public final String arg;
    public final String pdcKey;
    public final String displayName;
    public final ArmorSlot slot;
    public final int maxLevel;
    public final NamedTextColor color;

    EnchantType(int modelTypeIndex, @NotNull ForbiddenEnchantDefinition definition) {
        this.modelTypeIndex = modelTypeIndex;
        this.definition = definition;
        this.arg = definition.arg();
        this.pdcKey = definition.pdcKey();
        this.displayName = definition.displayName();
        this.slot = definition.slot();
        this.maxLevel = definition.maxLevel();
        this.color = definition.color();
    }

    public @NotNull String slotDescription() {
        return definition.slotDescription();
    }

    public @NotNull String effectDescription(int level) {
        return definition.effectDescription(level);
    }

    public @NotNull ForbiddenEnchantDefinition definition() {
        return definition;
    }

    public int modelTypeIndex() {
        return modelTypeIndex;
    }

    public boolean appliesBindingCurse() {
        return APPLIES_BINDING_CURSE.contains(this);
    }

    public boolean stripsVanillaEnchants() {
        return STRIPS_VANILLA_ENCHANTS.contains(this);
    }

    public boolean stripsMendingAndUnbreaking() {
        return STRIPS_MENDING_UNBREAKING.contains(this);
    }

    public boolean hasDurabilityPenalty() {
        return DURABILITY_PENALTY_ENCHANTS.contains(this);
    }

    public boolean requiresNoOtherEnchantsOnItem() {
        return REQUIRES_NO_OTHER_ENCHANTS.contains(this);
    }

    public boolean requiresSoloOnTrident() {
        return REQUIRES_SOLO_ON_TRIDENT.contains(this);
    }

    public boolean isAnvilOnlyUtilityBook() {
        return this == THE_DUPLICATOR || this == THE_PHILOSOPHERS_BOOK;
    }

    public boolean conflictsWith(@NotNull EnchantType other) {
        if (this == other) {
            return false;
        }
        String leftGroup = EXCLUSIVE_GROUP.get(this);
        String rightGroup = EXCLUSIVE_GROUP.get(other);
        if (leftGroup != null && leftGroup.equals(rightGroup)) {
            return true;
        }
        return INCOMPATIBLE_WITH.getOrDefault(this, Collections.emptySet()).contains(other);
    }

    public static @Nullable EnchantType fromModelTypeIndex(int modelTypeIndex) {
        return BY_MODEL_TYPE_INDEX.get(modelTypeIndex);
    }

    public static @Nullable EnchantType fromArg(@NotNull String input) {
        String normalized = input.toLowerCase(Locale.ROOT).trim().replace(' ', '_').replace('-', '_');
        return BY_ARG.get(normalized);
    }

    private static void setExclusiveGroup(@NotNull String group, @NotNull EnchantType... types) {
        for (EnchantType type : types) {
            EXCLUSIVE_GROUP.put(type, group);
        }
    }

    private static void addMutualIncompatibility(@NotNull EnchantType left, @NotNull EnchantType right) {
        addOneWayIncompatibility(left, right);
        addOneWayIncompatibility(right, left);
    }

    private static void addOneWayIncompatibility(@NotNull EnchantType left, @NotNull EnchantType right) {
        INCOMPATIBLE_WITH.computeIfAbsent(left, ignored -> EnumSet.noneOf(EnchantType.class)).add(right);
    }
}

