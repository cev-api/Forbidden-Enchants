package dev.cevapi.forbiddenenchants;

import dev.cevapi.forbiddenenchants.enchants.AppliedCurseEnchant;
import dev.cevapi.forbiddenenchants.enchants.AquaticSacrificeEnchant;
import dev.cevapi.forbiddenenchants.enchants.AscensionEnchant;
import dev.cevapi.forbiddenenchants.enchants.BlindnessEnchant;
import dev.cevapi.forbiddenenchants.enchants.BorgTechnologyEnchant;
import dev.cevapi.forbiddenenchants.enchants.CharmEnchant;
import dev.cevapi.forbiddenenchants.enchants.CharmedPetEnchant;
import dev.cevapi.forbiddenenchants.enchants.CreepersInfluenceEnchant;
import dev.cevapi.forbiddenenchants.enchants.DisarmEnchant;
import dev.cevapi.forbiddenenchants.enchants.DivineVisionEnchant;
import dev.cevapi.forbiddenenchants.enchants.DragonsBreathEnchant;
import dev.cevapi.forbiddenenchants.enchants.EvokersRevengeEnchant;
import dev.cevapi.forbiddenenchants.enchants.ExtendedGraspEnchant;
import dev.cevapi.forbiddenenchants.enchants.ExplosiveReactionEnchant;
import dev.cevapi.forbiddenenchants.enchants.ForbiddenAgilityEnchant;
import dev.cevapi.forbiddenenchants.enchants.ForbiddenEnchantDefinition;
import dev.cevapi.forbiddenenchants.enchants.FullForceEnchant;
import dev.cevapi.forbiddenenchants.enchants.FullPocketsEnchant;
import dev.cevapi.forbiddenenchants.enchants.GetOverHereEnchant;
import dev.cevapi.forbiddenenchants.enchants.GraveRobberEnchant;
import dev.cevapi.forbiddenenchants.enchants.GreedEnchant;
import dev.cevapi.forbiddenenchants.enchants.HealingTouchEnchant;
import dev.cevapi.forbiddenenchants.enchants.InciteFearEnchant;
import dev.cevapi.forbiddenenchants.enchants.LaunchEnchant;
import dev.cevapi.forbiddenenchants.enchants.LockedOutEnchant;
import dev.cevapi.forbiddenenchants.enchants.LootSenseEnchant;
import dev.cevapi.forbiddenenchants.enchants.LumberjackEnchant;
import dev.cevapi.forbiddenenchants.enchants.MarkedEnchant;
import dev.cevapi.forbiddenenchants.enchants.MasqueradeEnchant;
import dev.cevapi.forbiddenenchants.enchants.MiasmaEnchant;
import dev.cevapi.forbiddenenchants.enchants.MiasmaFormEnchant;
import dev.cevapi.forbiddenenchants.enchants.MinersIntuitionEnchant;
import dev.cevapi.forbiddenenchants.enchants.MujahideenEnchant;
import dev.cevapi.forbiddenenchants.enchants.NoFallEnchant;
import dev.cevapi.forbiddenenchants.enchants.PettyThiefEnchant;
import dev.cevapi.forbiddenenchants.enchants.PocketDimensionEnchant;
import dev.cevapi.forbiddenenchants.enchants.PocketSeekerEnchant;
import dev.cevapi.forbiddenenchants.enchants.ProudWarriorEnchant;
import dev.cevapi.forbiddenenchants.enchants.RicochetEnchant;
import dev.cevapi.forbiddenenchants.enchants.ShieldKnockbackEnchant;
import dev.cevapi.forbiddenenchants.enchants.ShockwaveEnchant;
import dev.cevapi.forbiddenenchants.enchants.SiskosSolutionEnchant;
import dev.cevapi.forbiddenenchants.enchants.SonicPanicEnchant;
import dev.cevapi.forbiddenenchants.enchants.StaffOfTheEvokerEnchant;
import dev.cevapi.forbiddenenchants.enchants.TemporalSicknessEnchant;
import dev.cevapi.forbiddenenchants.enchants.TheHatedOneEnchant;
import dev.cevapi.forbiddenenchants.enchants.TheSeekerEnchant;
import dev.cevapi.forbiddenenchants.enchants.TheUnyieldingEnchant;
import dev.cevapi.forbiddenenchants.enchants.VexatiousEnchant;
import dev.cevapi.forbiddenenchants.enchants.VoidGraspEnchant;
import dev.cevapi.forbiddenenchants.enchants.WingClipperEnchant;
import dev.cevapi.forbiddenenchants.enchants.WitheringStrikeEnchant;
import dev.cevapi.forbiddenenchants.enchants.WololoEnchant;
import dev.cevapi.forbiddenenchants.enchants.WarpNineFiveEnchant;
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
    WARP_NINE_FIVE(53, new WarpNineFiveEnchant());

    private static final Map<String, EnchantType> BY_ARG = new HashMap<>();
    private static final Map<Integer, EnchantType> BY_MODEL_TYPE_INDEX = new HashMap<>();
    private static final Map<EnchantType, String> EXCLUSIVE_GROUP = new EnumMap<>(EnchantType.class);
    private static final Map<EnchantType, Set<EnchantType>> INCOMPATIBLE_WITH = new EnumMap<>(EnchantType.class);
    private static final Set<EnchantType> APPLIES_BINDING_CURSE = EnumSet.noneOf(EnchantType.class);
    private static final Set<EnchantType> STRIPS_VANILLA_ENCHANTS = EnumSet.noneOf(EnchantType.class);
    private static final Set<EnchantType> STRIPS_MENDING_UNBREAKING = EnumSet.noneOf(EnchantType.class);
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
        setExclusiveGroup("boots_primary", MASQUERADE, ASCENSION);
        setExclusiveGroup("ranged_primary", MIASMA, CHARM, DRAGONS_BREATH, EXPLOSIVE_REACTION);
        setExclusiveGroup("hoe_primary", HEALING_TOUCH, PETTY_THIEF);
        setExclusiveGroup("compass_primary", GRAVE_ROBBER, POCKET_SEEKER);
        setExclusiveGroup("nametag_primary", CHARMED_PET, APPLIED_CURSE);

        addMutualIncompatibility(DRAGONS_BREATH, EXPLOSIVE_REACTION);

        APPLIES_BINDING_CURSE.addAll(EnumSet.of(
                AQUATIC_SACRIFICE,
                THE_HATED_ONE,
                WOLOLO,
                LOCKED_OUT,
                EVOKERS_REVENGE,
                GREED,
                TEMPORAL_SICKNESS,
                PROUD_WARRIOR,
                SISKOS_SOLUTION
        ));

        STRIPS_VANILLA_ENCHANTS.addAll(EnumSet.of(DIVINE_VISION, MINERS_INTUITION, LOOT_SENSE));
        STRIPS_MENDING_UNBREAKING.addAll(EnumSet.of(HEALING_TOUCH, THE_SEEKER));

        REQUIRES_NO_OTHER_ENCHANTS.addAll(EnumSet.of(
                DRAGONS_BREATH,
                EXPLOSIVE_REACTION,
                PROUD_WARRIOR,
                SISKOS_SOLUTION,
                BORG_TECHNOLOGY,
                WARP_NINE_FIVE
        ));
        REQUIRES_SOLO_ON_TRIDENT.add(WITHERING_STRIKE);
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

    public boolean requiresNoOtherEnchantsOnItem() {
        return REQUIRES_NO_OTHER_ENCHANTS.contains(this);
    }

    public boolean requiresSoloOnTrident() {
        return REQUIRES_SOLO_ON_TRIDENT.contains(this);
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

