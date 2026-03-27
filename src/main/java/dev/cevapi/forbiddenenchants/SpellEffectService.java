package dev.cevapi.forbiddenenchants;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.block.data.type.Bed;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.lang.reflect.Method;

final class SpellEffectService {
    private static final long OUT_OF_PHASE_DURATION = 20L * 60L;
    private static final long SILENCE_DURATION = 20L * 15L;
    private static final long QUITTER_DURATION = 20L * 30L;
    private static final long INFECTED_DURATION = 20L * 30L;
    private static final long TEMPORAL_DISPLACEMENT_LEVEL_1_DURATION = 20L * 10L;
    private static final long TEMPORAL_DISPLACEMENT_LEVEL_2_DURATION = 20L * 30L;
    private static final long TEMPORAL_DISPLACEMENT_LEVEL_3_DURATION = 20L * 60L;
    private static final long ONE_PLUS_LEVEL_1_DURATION = 20L * 60L;
    private static final long ONE_PLUS_LEVEL_2_DURATION = 20L * 300L;
    private static final long ONE_PLUS_LEVEL_3_DURATION = 20L * 600L;
    private static final long ONE_PLUS_LEVEL_4_DURATION = 20L * 1800L;
    private static final double LIMITLESS_VISION_RANGE = 50.0D;
    private static final int VOID_STICK_MAX_USES = 5;
    private static final int FIREBALL_LEVEL_1_MAX_USES = 50;
    private static final int FIREBALL_LEVEL_2_MAX_USES = 100;
    private static final double FARMERS_DREAM_MAX_DISTANCE = 500.0D;
    private static final double LAVA_STEP_MAX_DISTANCE = 1000.0D;
    private static final int LAVA_STEP_REVERT_TICKS = 80;
    private static final double MAX_GOLDEN_HEARTS_ABSORPTION = 20.0D;
    private static final double LIFE_STEAL_EXECUTE_HEALTH = 6.0D;

    private final ForbiddenEnchantsPlugin plugin;
    private final Map<UUID, Long> outOfPhaseUntil = new HashMap<>();
    private final Map<UUID, Long> silenceUntil = new HashMap<>();
    private final Map<UUID, Long> quitterUntil = new HashMap<>();
    private final Map<UUID, Long> infectedCasterUntil = new HashMap<>();
    private final Map<UUID, Long> temporalDisplacementUntil = new HashMap<>();
    private final Map<UUID, Long> onePlusUntil = new HashMap<>();
    private final Map<UUID, Byte> limitlessVisionActive = new HashMap<>();
    private final Map<UUID, Long> wallPhaseCooldownMs = new HashMap<>();
    private final Map<UUID, InfectedState> infectedZombies = new HashMap<>();
    private final Map<UUID, Integer> lifeStealHeartsGained = new HashMap<>();
    private final Map<UUID, Integer> lifeSpiritHeartsGained = new HashMap<>();
    private final Map<UUID, Long> leftClickCooldownMs = new HashMap<>();
    private final Map<UUID, Long> lastRightClickAtMs = new HashMap<>();
    private final Map<UUID, Long> lastInventoryActionAtMs = new HashMap<>();
    private final Map<UUID, Long> lastDropTick = new HashMap<>();
    private final Map<UUID, Byte> trueSilenceActive = new HashMap<>();
    private final Map<String, TempLavaBlock> temporaryLavaPath = new HashMap<>();

    SpellEffectService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    void processTick(long tickCounter) {
        processActiveInfectedCasters(tickCounter);
        expireQuitters(tickCounter);
        expireOutOfPhase(tickCounter);
        expireInfected(tickCounter);
        expireTemporaryLavaPath(tickCounter);
    }

    void onPlayerTick(@NotNull Player player, long tickCounter) {
        UUID playerId = player.getUniqueId();
        long outUntil = outOfPhaseUntil.getOrDefault(playerId, 0L);
        if (tickCounter <= outUntil) {
            player.setCollidable(false);
        }
        if (limitlessVisionActive.containsKey(playerId)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 220, 0, true, false, false), true);
            plugin.applyDivineVisionLineOfSight(player, LIMITLESS_VISION_RANGE);
        }
        applyMagnetism(player);
        applyCursedMagnetism(player);
        applyTrueSilence(player);
        applyTemporalDisplacementAura(player, tickCounter);
        applyOnePlusForEquipped(player, tickCounter);
        LinkedHashMap<String, Long> timers = new LinkedHashMap<>();
        if (tickCounter <= outUntil) {
            timers.put("Out Of Phase", secondsRemaining(outUntil, tickCounter));
        }
        long silence = silenceUntil.getOrDefault(playerId, 0L);
        if (tickCounter <= silence) {
            timers.put("Silence", secondsRemaining(silence, tickCounter));
        }
        long quitter = quitterUntil.getOrDefault(playerId, 0L);
        if (tickCounter <= quitter) {
            timers.put("Quitter", secondsRemaining(quitter, tickCounter));
        }
        long infectedUntil = infectedCasterUntil.getOrDefault(playerId, 0L);
        if (tickCounter <= infectedUntil) {
            timers.put("Infected", secondsRemaining(infectedUntil, tickCounter));
        }
        long temporalDisplacement = temporalDisplacementUntil.getOrDefault(playerId, 0L);
        if (tickCounter <= temporalDisplacement) {
            timers.put("Temporal Displacement", secondsRemaining(temporalDisplacement, tickCounter));
        }
        long onePlus = onePlusUntil.getOrDefault(playerId, 0L);
        if (tickCounter <= onePlus) {
            timers.put("One Plus", secondsRemaining(onePlus, tickCounter));
        }
        if (!timers.isEmpty()) {
            StringBuilder text = new StringBuilder();
            for (Map.Entry<String, Long> entry : timers.entrySet()) {
                if (!text.isEmpty()) {
                    text.append(" | ");
                }
                text.append(entry.getKey()).append(": ").append(entry.getValue()).append("s");
            }
            player.sendActionBar(net.kyori.adventure.text.Component.text(text.toString()));
        }
    }

    void onPlayerItemConsume(@NotNull PlayerItemConsumeEvent event, long tickCounter) {
        Player player = event.getPlayer();
        if (isSilenced(player.getUniqueId(), tickCounter)) {
            event.setCancelled(true);
            return;
        }

        ItemStack consumed = event.getItem();
        if (plugin.getEnchantLevel(consumed, EnchantType.OUT_OF_PHASE) > 0) {
            outOfPhaseUntil.put(player.getUniqueId(), tickCounter + OUT_OF_PHASE_DURATION);
            return;
        }
        if (plugin.getEnchantLevel(consumed, EnchantType.SILENCE) > 0) {
            applySilencePulse(player, tickCounter);
            return;
        }
        if (plugin.getEnchantLevel(consumed, EnchantType.QUITTER) > 0) {
            if (!isQuitterActive(player.getUniqueId(), tickCounter)) {
                quitterUntil.put(player.getUniqueId(), tickCounter + QUITTER_DURATION);
                hideQuitter(player);
            }
            return;
        }
        if (plugin.getEnchantLevel(consumed, EnchantType.INFECTED) > 0) {
            infectedCasterUntil.put(player.getUniqueId(), tickCounter + INFECTED_DURATION);
            applyInfected(player, tickCounter, true);
            return;
        }
        if (plugin.getEnchantLevel(consumed, EnchantType.JOINT_SLEEP) > 0) {
            applyJointSleep(player);
            return;
        }
        if (plugin.getEnchantLevel(consumed, EnchantType.LIMITLESS_VISION) > 0) {
            limitlessVisionActive.put(player.getUniqueId(), (byte) 1);
            return;
        }
        int temporalDisplacementLevel = plugin.getEnchantLevel(consumed, EnchantType.TEMPORAL_DISPLACEMENT);
        if (temporalDisplacementLevel > 0) {
            temporalDisplacementUntil.put(player.getUniqueId(), tickCounter + temporalDisplacementDurationForLevel(temporalDisplacementLevel));
            return;
        }
        int onePlusLevel = plugin.getEnchantLevel(consumed, EnchantType.ONE_PLUS);
        if (onePlusLevel > 0) {
            onePlusUntil.put(player.getUniqueId(), tickCounter + onePlusDurationForLevel(onePlusLevel));
            return;
        }

        if (plugin.getEnchantLevel(consumed, EnchantType.MINERS_VOID_STEP) > 0) {
            consumeMinersVoidStep(player);
            return;
        }
        if (plugin.getEnchantLevel(consumed, EnchantType.SWAP) > 0) {
            consumeSwap(player);
            return;
        }
        if (plugin.getEnchantLevel(consumed, EnchantType.VITALITY_THIEF) > 0) {
            consumeVitalityThief(player);
            return;
        }
        int lifeSpiritLevel = plugin.getEnchantLevel(consumed, EnchantType.LIFE_SPIRIT);
        if (lifeSpiritLevel > 0) {
            consumeLifeSpirit(player, lifeSpiritLevel);
            return;
        }
        int lifeStealLevel = plugin.getEnchantLevel(consumed, EnchantType.LIFE_STEAL);
        if (lifeStealLevel > 0) {
            consumeLifeSteal(player, lifeStealLevel);
            return;
        }
        if (plugin.getEnchantLevel(consumed, EnchantType.BED_TIME) > 0) {
            consumeBedTime(player);
        }
    }

    void onPlayerMove(@NotNull PlayerMoveEvent event, long tickCounter) {
        processCustomBootMovement(event, tickCounter);

        Player player = event.getPlayer();
        if (!isOutOfPhase(player.getUniqueId(), tickCounter)) {
            return;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null || from.getWorld() != to.getWorld()) {
            return;
        }
        if (from.distanceSquared(to) < 0.0009D) {
            return;
        }

        long now = System.currentTimeMillis();
        long cooldownUntil = wallPhaseCooldownMs.getOrDefault(player.getUniqueId(), 0L);
        if (now < cooldownUntil) {
            return;
        }

        org.bukkit.util.Vector horizontal = to.toVector().subtract(from.toVector()).setY(0.0D);
        if (horizontal.lengthSquared() < 0.0001D) {
            return;
        }
        horizontal.normalize();

        Location frontFeet = from.clone().add(horizontal.clone().multiply(0.55D));
        Location frontHead = frontFeet.clone().add(0.0D, 1.0D, 0.0D);
        if (!isSolid(frontFeet) && !isSolid(frontHead)) {
            return;
        }
        Location destination = null;
        // Support phasing through up to ~2-block-thick walls.
        for (double distance : new double[]{1.20D, 1.50D, 1.80D, 2.10D, 2.40D, 2.70D, 3.00D}) {
            Location tryFeet = from.clone().add(horizontal.clone().multiply(distance));
            Location tryHead = tryFeet.clone().add(0.0D, 1.0D, 0.0D);
            if (isPassable(tryFeet) && isPassable(tryHead)) {
                destination = tryFeet;
                break;
            }
        }
        if (destination == null) {
            return;
        }

        destination = destination.add(0.5D, 0.0D, 0.5D);
        destination.setPitch(player.getLocation().getPitch());
        destination.setYaw(player.getLocation().getYaw());
        event.setTo(destination);
        wallPhaseCooldownMs.put(player.getUniqueId(), now + 200L);
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.5F, 1.7F);
    }

    void onPlayerInteract(@NotNull PlayerInteractEvent event, long tickCounter) {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            lastRightClickAtMs.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        }

        ItemStack hand = event.getItem();
        if (hand != null && handleCustomRodLeftClick(event.getPlayer(), hand, event.getAction(), tickCounter)) {
            event.setCancelled(true);
            return;
        }

        if (isSilenced(event.getPlayer().getUniqueId(), tickCounter)) {
            if (hand == null) {
                return;
            }
            Material type = hand.getType();
            if (type == Material.ENDER_PEARL || type == Material.TOTEM_OF_UNDYING) {
                event.setCancelled(true);
            }
        }
    }

    void onPlayerAnimation(@NotNull PlayerAnimationEvent event, long tickCounter) {
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }
        InventoryType topType = event.getPlayer().getOpenInventory().getTopInventory().getType();
        if (topType != InventoryType.CRAFTING && topType != InventoryType.CREATIVE) {
            return;
        }
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        Bukkit.getScheduler().runTask(plugin, () -> tryHandleDeferredAnimationCast(playerId, tickCounter));
    }

    void onPlayerDropItem(@NotNull PlayerDropItemEvent event, long tickCounter) {
        lastInventoryActionAtMs.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
        lastDropTick.put(event.getPlayer().getUniqueId(), tickCounter);
    }

    void onEntityResurrect(@NotNull EntityResurrectEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (isSilenced(player.getUniqueId(), tickCounter)) {
            event.setCancelled(true);
        }
    }

    void onMobTarget(@NotNull EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player target)) {
            return;
        }
        ItemStack boots = target.getInventory().getBoots();
        if (plugin.getEnchantLevel(boots, EnchantType.TRUE_SILENCE) <= 0) {
            return;
        }
        if (event.getEntity().getLocation().distanceSquared(target.getLocation()) > 4.0D) {
            event.setCancelled(true);
        }
    }

    void onEnchantItem(@NotNull EnchantItemEvent event, long tickCounter) {
        if (isSilenced(event.getEnchanter().getUniqueId(), tickCounter)) {
            event.setCancelled(true);
            return;
        }
        int farmersDream = plugin.getEnchantLevel(event.getItem(), EnchantType.FARMERS_DREAM);
        int lavaStep = plugin.getEnchantLevel(event.getItem(), EnchantType.LAVA_STEP);
        if (farmersDream > 0 || lavaStep > 0) {
            event.getEnchantsToAdd().remove(Enchantment.UNBREAKING);
            event.getEnchantsToAdd().remove(Enchantment.MENDING);
        }
    }

    void onInventoryClick(@NotNull InventoryClickEvent event, long tickCounter) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        lastInventoryActionAtMs.put(player.getUniqueId(), System.currentTimeMillis());
        if (!isSilenced(player.getUniqueId(), tickCounter)) {
            return;
        }
        if (event.getView().getTopInventory().getType() != InventoryType.ANVIL) {
            return;
        }
        if (event.getRawSlot() == 2) {
            event.setCancelled(true);
        }
    }

    void onChat(@NotNull AsyncPlayerChatEvent event, long tickCounter) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (isQuitterActive(playerId, tickCounter)) {
            event.setCancelled(true);
        }
    }

    void onPlayerQuit(@NotNull UUID playerId) {
        outOfPhaseUntil.remove(playerId);
        silenceUntil.remove(playerId);
        quitterUntil.remove(playerId);
        infectedCasterUntil.remove(playerId);
        temporalDisplacementUntil.remove(playerId);
        onePlusUntil.remove(playerId);
        limitlessVisionActive.remove(playerId);
        wallPhaseCooldownMs.remove(playerId);
        leftClickCooldownMs.remove(playerId);
        lastRightClickAtMs.remove(playerId);
        lastInventoryActionAtMs.remove(playerId);
        lastDropTick.remove(playerId);
        trueSilenceActive.remove(playerId);
        lifeStealHeartsGained.remove(playerId);
        lifeSpiritHeartsGained.remove(playerId);
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            if (player.isSilent()) {
                player.setSilent(false);
            }
            clearOnePlusForEquipped(player);
        }
        revealQuitter(playerId);
    }

    void onPlayerDeath(@NotNull UUID playerId) {
        removeLifeHeartsOnDeath(playerId);
        outOfPhaseUntil.remove(playerId);
        infectedCasterUntil.remove(playerId);
        temporalDisplacementUntil.remove(playerId);
        onePlusUntil.remove(playerId);
        limitlessVisionActive.remove(playerId);
        leftClickCooldownMs.remove(playerId);
        lastRightClickAtMs.remove(playerId);
        lastInventoryActionAtMs.remove(playerId);
        lastDropTick.remove(playerId);
        trueSilenceActive.remove(playerId);
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            if (player.isSilent()) {
                player.setSilent(false);
            }
            clearOnePlusForEquipped(player);
        }
        revealQuitter(playerId);
    }

    private void applySilencePulse(@NotNull Player source, long tickCounter) {
        long until = tickCounter + SILENCE_DURATION;
        for (Entity nearby : source.getNearbyEntities(20.0D, 20.0D, 20.0D)) {
            if (nearby instanceof Player playerTarget) {
                silenceUntil.put(playerTarget.getUniqueId(), until);
            }
        }
        source.getWorld().playSound(source.getLocation(), Sound.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 0.8F, 0.8F);
    }

    private void applyInfected(@NotNull Player source, long tickCounter, boolean showNoTargetsMessage) {
        boolean convertedAny = false;
        List<Entity> nearby = source.getNearbyEntities(5.0D, 5.0D, 5.0D);
        for (Entity entity : nearby) {
            if (!(entity instanceof LivingEntity) || entity instanceof Player) {
                continue;
            }
            EntityType type = entity.getType();
            if (type == EntityType.WARDEN || type == EntityType.WITHER || type == EntityType.ENDER_DRAGON) {
                continue;
            }
            if (isAlreadyZombieFamily(type)) {
                continue;
            }
            EntityType zombieType = zombieEquivalent(type);
            if (zombieType == null) {
                continue;
            }

            Location location = entity.getLocation().clone();
            World world = location.getWorld();
            if (world == null) {
                continue;
            }

            Entity replacement = world.spawnEntity(location, zombieType);
            if (!(replacement instanceof LivingEntity)) {
                replacement.remove();
                continue;
            }

            entity.remove();
            infectedZombies.put(replacement.getUniqueId(), new InfectedState(type, location, tickCounter + INFECTED_DURATION));
            world.playSound(location, Sound.ENTITY_ZOMBIE_INFECT, SoundCategory.HOSTILE, 0.8F, 1.0F);
            convertedAny = true;
        }
        if (!convertedAny && showNoTargetsMessage) {
            source.sendActionBar(net.kyori.adventure.text.Component.text(
                    plugin.message("spells.infected.no_targets", "Infected: no valid nearby targets.")
            ));
        }
    }

    private void processActiveInfectedCasters(long tickCounter) {
        if (infectedCasterUntil.isEmpty()) {
            return;
        }
        if (tickCounter % 10L != 0L) {
            return;
        }

        Iterator<Map.Entry<UUID, Long>> iterator = infectedCasterUntil.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            long until = entry.getValue();
            if (tickCounter > until) {
                iterator.remove();
                continue;
            }

            Player caster = Bukkit.getPlayer(entry.getKey());
            if (caster == null || !caster.isOnline() || caster.isDead()) {
                continue;
            }
            applyInfected(caster, tickCounter, false);
        }
    }

    private void applyJointSleep(@NotNull Player player) {
        Location nearest = findNearestOtherPlayerBed(player);
        if (nearest != null) {
            player.teleport(nearest.clone().add(0.5D, 0.1D, 0.5D));
            player.sendActionBar(net.kyori.adventure.text.Component.text(
                    plugin.message("spells.joint_sleep.warped", "Joint Sleep: warped to nearest shared bed.")
            ));
            return;
        }

        if (placeBedsInFront(player)) {
            player.sendActionBar(net.kyori.adventure.text.Component.text(
                    plugin.message("spells.joint_sleep.placed_beds", "Joint Sleep: placed 2 beds in front.")
            ));
            return;
        }

        ItemStack beds = new ItemStack(Material.WHITE_BED, 2);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(beds);
        for (ItemStack stack : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), stack);
        }
        player.sendActionBar(net.kyori.adventure.text.Component.text(
                plugin.message("spells.joint_sleep.gave_beds", "Joint Sleep: no room to place beds, gave 2 beds.")
        ));
    }

    private Location findNearestOtherPlayerBed(@NotNull Player player) {
        Location here = player.getLocation();
        Location nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player other : Bukkit.getOnlinePlayers()) {
            if (other.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            Location bed = other.getBedSpawnLocation();
            if (bed == null || bed.getWorld() != here.getWorld()) {
                continue;
            }
            double distance = bed.distanceSquared(here);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = bed.clone();
            }
        }
        return nearest;
    }

    private boolean placeBedsInFront(@NotNull Player player) {
        BlockFace facing = yawToHorizontalFace(player.getLocation().getYaw());
        Block anchor = player.getLocation().getBlock().getRelative(facing);
        BlockFace right = rightOf(facing);
        if (right == BlockFace.SELF) {
            return false;
        }

        if (tryPlaceAdjacentBeds(anchor, facing, right)) {
            return true;
        }
        return tryPlaceAdjacentBeds(anchor, facing, right.getOppositeFace());
    }

    private boolean tryPlaceAdjacentBeds(@NotNull Block anchor, @NotNull BlockFace facing, @NotNull BlockFace sideOffset) {
        Block firstFoot = anchor;
        Block firstHead = firstFoot.getRelative(facing);
        Block secondFoot = firstFoot.getRelative(sideOffset);
        Block secondHead = secondFoot.getRelative(facing);

        if (!isBedPlaceable(firstFoot) || !isBedPlaceable(firstHead) || !isBedPlaceable(secondFoot) || !isBedPlaceable(secondHead)) {
            return false;
        }

        placeBed(firstFoot, facing, false);
        placeBed(firstHead, facing, true);
        placeBed(secondFoot, facing, false);
        placeBed(secondHead, facing, true);
        return true;
    }

    private boolean isBedPlaceable(@NotNull Block block) {
        if (!block.isPassable()) {
            return false;
        }
        Block below = block.getRelative(BlockFace.DOWN);
        return below.getType().isSolid();
    }

    private void placeBed(@NotNull Block block, @NotNull BlockFace facing, boolean headPart) {
        block.setType(Material.WHITE_BED, false);
        if (!(block.getBlockData() instanceof Bed bedData)) {
            return;
        }
        bedData.setFacing(facing);
        bedData.setPart(headPart ? Bed.Part.HEAD : Bed.Part.FOOT);
        block.setBlockData(bedData, false);
    }

    private BlockFace yawToHorizontalFace(float yaw) {
        float wrapped = (yaw % 360 + 360) % 360;
        if (wrapped >= 45 && wrapped < 135) {
            return BlockFace.WEST;
        }
        if (wrapped >= 135 && wrapped < 225) {
            return BlockFace.NORTH;
        }
        if (wrapped >= 225 && wrapped < 315) {
            return BlockFace.EAST;
        }
        return BlockFace.SOUTH;
    }

    private @NotNull BlockFace rightOf(@NotNull BlockFace facing) {
        return switch (facing) {
            case NORTH -> BlockFace.EAST;
            case EAST -> BlockFace.SOUTH;
            case SOUTH -> BlockFace.WEST;
            case WEST -> BlockFace.NORTH;
            default -> BlockFace.SELF;
        };
    }

    private boolean isAlreadyZombieFamily(@NotNull EntityType type) {
        return type == EntityType.ZOMBIE
                || type == EntityType.ZOMBIE_VILLAGER
                || type == EntityType.ZOMBIFIED_PIGLIN
                || type == EntityType.HUSK
                || type == EntityType.DROWNED;
    }

    private EntityType zombieEquivalent(@NotNull EntityType type) {
        return switch (type) {
            case VILLAGER -> EntityType.ZOMBIE_VILLAGER;
            case PILLAGER, EVOKER, VINDICATOR, ILLUSIONER, WITCH, WANDERING_TRADER -> EntityType.ZOMBIE;
            case PIGLIN, PIGLIN_BRUTE -> EntityType.ZOMBIFIED_PIGLIN;
            default -> null;
        };
    }

    private void expireInfected(long tickCounter) {
        Iterator<Map.Entry<UUID, InfectedState>> iterator = infectedZombies.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, InfectedState> entry = iterator.next();
            InfectedState state = entry.getValue();
            if (tickCounter <= state.expireTick()) {
                continue;
            }

            iterator.remove();
            Entity current = Bukkit.getEntity(entry.getKey());
            Location spawnLocation = state.location().clone();
            if (current != null) {
                spawnLocation = current.getLocation().clone();
                current.remove();
            }
            World world = spawnLocation.getWorld();
            if (world == null) {
                continue;
            }
            try {
                world.spawnEntity(spawnLocation, state.originalType());
            } catch (Throwable ignored) {
            }
        }
    }

    private void expireOutOfPhase(long tickCounter) {
        Iterator<Map.Entry<UUID, Long>> iterator = outOfPhaseUntil.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (tickCounter <= entry.getValue()) {
                continue;
            }
            iterator.remove();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                player.setCollidable(true);
            }
        }
    }

    private void expireQuitters(long tickCounter) {
        Iterator<Map.Entry<UUID, Long>> iterator = quitterUntil.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            if (tickCounter <= entry.getValue()) {
                continue;
            }
            iterator.remove();
            UUID playerId = entry.getKey();
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                revealQuitter(playerId);
                Bukkit.broadcastMessage(player.getName() + " joined the game");
            }
        }
    }

    private void hideQuitter(@NotNull Player player) {
        Bukkit.broadcastMessage(player.getName() + " left the game");
        setListed(player, false);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) {
                viewer.hidePlayer(plugin, player);
            }
        }
    }

    private void revealQuitter(@NotNull UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            return;
        }
        setListed(player, true);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) {
                viewer.showPlayer(plugin, player);
            }
        }
    }

    private boolean isOutOfPhase(@NotNull UUID playerId, long tickCounter) {
        return tickCounter <= outOfPhaseUntil.getOrDefault(playerId, 0L);
    }

    private boolean isSilenced(@NotNull UUID playerId, long tickCounter) {
        return tickCounter <= silenceUntil.getOrDefault(playerId, 0L);
    }

    private boolean isQuitterActive(@NotNull UUID playerId, long tickCounter) {
        return tickCounter <= quitterUntil.getOrDefault(playerId, 0L);
    }

    private boolean isSolid(@NotNull Location location) {
        return location.getBlock().getType().isSolid();
    }

    private boolean isPassable(@NotNull Location location) {
        return location.getBlock().isPassable() && !location.getBlock().getType().isSolid();
    }

    private long temporalDisplacementDurationForLevel(int level) {
        return switch (Math.max(1, Math.min(3, level))) {
            case 1 -> TEMPORAL_DISPLACEMENT_LEVEL_1_DURATION;
            case 2 -> TEMPORAL_DISPLACEMENT_LEVEL_2_DURATION;
            default -> TEMPORAL_DISPLACEMENT_LEVEL_3_DURATION;
        };
    }

    private void applyTemporalDisplacementAura(@NotNull Player caster, long tickCounter) {
        long until = temporalDisplacementUntil.getOrDefault(caster.getUniqueId(), 0L);
        if (tickCounter > until) {
            temporalDisplacementUntil.remove(caster.getUniqueId());
            return;
        }

        for (Entity entity : caster.getNearbyEntities(20.0D, 20.0D, 20.0D)) {
            if (!(entity instanceof LivingEntity living)) {
                continue;
            }
            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 30, 4, true, false, true), true);
        }
    }

    private long onePlusDurationForLevel(int level) {
        return switch (Math.max(1, Math.min(4, level))) {
            case 1 -> ONE_PLUS_LEVEL_1_DURATION;
            case 2 -> ONE_PLUS_LEVEL_2_DURATION;
            case 3 -> ONE_PLUS_LEVEL_3_DURATION;
            default -> ONE_PLUS_LEVEL_4_DURATION;
        };
    }

    private void applyOnePlusForEquipped(@NotNull Player player, long tickCounter) {
        UUID playerId = player.getUniqueId();
        long until = onePlusUntil.getOrDefault(playerId, 0L);
        boolean active = tickCounter <= until;
        if (!active) {
            onePlusUntil.remove(playerId);
            clearOnePlusForEquipped(player);
            return;
        }
        PlayerInventory inventory = player.getInventory();
        applyOnePlusOnSlot(player, EquipmentSlot.HAND, inventory.getItemInMainHand());
        applyOnePlusOnSlot(player, EquipmentSlot.OFF_HAND, inventory.getItemInOffHand());
        applyOnePlusOnSlot(player, EquipmentSlot.HEAD, inventory.getHelmet());
        applyOnePlusOnSlot(player, EquipmentSlot.CHEST, inventory.getChestplate());
        applyOnePlusOnSlot(player, EquipmentSlot.LEGS, inventory.getLeggings());
        applyOnePlusOnSlot(player, EquipmentSlot.FEET, inventory.getBoots());
    }

    private void clearOnePlusForEquipped(@NotNull Player player) {
        PlayerInventory inventory = player.getInventory();
        clearOnePlusOnSlot(player, EquipmentSlot.HAND, inventory.getItemInMainHand());
        clearOnePlusOnSlot(player, EquipmentSlot.OFF_HAND, inventory.getItemInOffHand());
        clearOnePlusOnSlot(player, EquipmentSlot.HEAD, inventory.getHelmet());
        clearOnePlusOnSlot(player, EquipmentSlot.CHEST, inventory.getChestplate());
        clearOnePlusOnSlot(player, EquipmentSlot.LEGS, inventory.getLeggings());
        clearOnePlusOnSlot(player, EquipmentSlot.FEET, inventory.getBoots());
    }

    private void applyOnePlusOnSlot(@NotNull Player player, @NotNull EquipmentSlot slot, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        NamespacedKey ownerKey = onePlusOwnerKey();
        String ownerValue = pdc.get(ownerKey, PersistentDataType.STRING);
        if (ownerValue != null && ownerValue.equals(player.getUniqueId().toString())) {
            return;
        }
        if (ownerValue != null) {
            clearOnePlusBoost(item);
            meta = item.getItemMeta();
            if (meta == null) {
                return;
            }
            pdc = meta.getPersistentDataContainer();
        }

        Map<Enchantment, Integer> currentEnchants = item.getEnchantments();
        if (currentEnchants.isEmpty()) {
            return;
        }
        pdc.set(ownerKey, PersistentDataType.STRING, player.getUniqueId().toString());
        pdc.set(onePlusOriginalEnchantsKey(), PersistentDataType.STRING, serializeEnchants(currentEnchants));
        item.setItemMeta(meta);
        for (Enchantment enchantment : List.copyOf(currentEnchants.keySet())) {
            item.removeEnchantment(enchantment);
        }
        item.addUnsafeEnchantments(increaseEnchantsByOne(currentEnchants));
        setSlotItem(player, slot, item);
    }

    private void clearOnePlusOnSlot(@NotNull Player player, @NotNull EquipmentSlot slot, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return;
        }
        if (!isOnePlusBoosted(item)) {
            return;
        }
        clearOnePlusBoost(item);
        setSlotItem(player, slot, item);
    }

    private void clearOnePlusBoost(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        String serialized = pdc.get(onePlusOriginalEnchantsKey(), PersistentDataType.STRING);
        pdc.remove(onePlusOwnerKey());
        pdc.remove(onePlusOriginalEnchantsKey());
        item.setItemMeta(meta);

        if (serialized != null) {
            Map<Enchantment, Integer> original = parseEnchants(serialized);
            for (Enchantment enchantment : List.copyOf(item.getEnchantments().keySet())) {
                item.removeEnchantment(enchantment);
            }
            for (Map.Entry<Enchantment, Integer> entry : original.entrySet()) {
                item.addUnsafeEnchantment(entry.getKey(), Math.max(1, entry.getValue()));
            }
        }
    }

    private boolean isOnePlusBoosted(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return meta.getPersistentDataContainer().has(onePlusOwnerKey(), PersistentDataType.STRING);
    }

    private @NotNull Map<Enchantment, Integer> increaseEnchantsByOne(@NotNull Map<Enchantment, Integer> source) {
        Map<Enchantment, Integer> boosted = new HashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : source.entrySet()) {
            boosted.put(entry.getKey(), entry.getValue() + 1);
        }
        return boosted;
    }

    private @NotNull String serializeEnchants(@NotNull Map<Enchantment, Integer> enchants) {
        StringBuilder serialized = new StringBuilder();
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            NamespacedKey key = entry.getKey().getKey();
            if (key == null) {
                continue;
            }
            if (!serialized.isEmpty()) {
                serialized.append(';');
            }
            serialized.append(key).append('=').append(entry.getValue());
        }
        return serialized.toString();
    }

    private @NotNull Map<Enchantment, Integer> parseEnchants(@NotNull String serialized) {
        Map<Enchantment, Integer> parsed = new HashMap<>();
        if (serialized.isEmpty()) {
            return parsed;
        }
        String[] entries = serialized.split(";");
        for (String entry : entries) {
            int delimiter = entry.indexOf('=');
            if (delimiter <= 0 || delimiter >= entry.length() - 1) {
                continue;
            }
            String keyText = entry.substring(0, delimiter);
            String levelText = entry.substring(delimiter + 1);
            NamespacedKey key = NamespacedKey.fromString(keyText);
            if (key == null) {
                continue;
            }
            Enchantment enchantment = Enchantment.getByKey(key);
            if (enchantment == null) {
                continue;
            }
            int level;
            try {
                level = Integer.parseInt(levelText);
            } catch (NumberFormatException ignored) {
                continue;
            }
            if (level > 0) {
                parsed.put(enchantment, level);
            }
        }
        return parsed;
    }

    private void setSlotItem(@NotNull Player player, @NotNull EquipmentSlot slot, @NotNull ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        switch (slot) {
            case HAND -> inventory.setItemInMainHand(item);
            case OFF_HAND -> inventory.setItemInOffHand(item);
            case HEAD -> inventory.setHelmet(item);
            case CHEST -> inventory.setChestplate(item);
            case LEGS -> inventory.setLeggings(item);
            case FEET -> inventory.setBoots(item);
            default -> {
            }
        }
    }

    private @NotNull NamespacedKey onePlusOwnerKey() {
        return new NamespacedKey(plugin, "one_plus_owner");
    }

    private @NotNull NamespacedKey onePlusOriginalEnchantsKey() {
        return new NamespacedKey(plugin, "one_plus_original_enchants");
    }

    private boolean handleCustomRodLeftClick(@NotNull Player player,
                                             @Nullable ItemStack hand,
                                             @NotNull Action action,
                                             long tickCounter) {
        if (hand == null || hand.getType() == Material.AIR) {
            return false;
        }
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return false;
        }

        long now = System.currentTimeMillis();
        long cooldownUntil = leftClickCooldownMs.getOrDefault(player.getUniqueId(), 0L);
        if (now < cooldownUntil) {
            return false;
        }

        int voidStickLevel = plugin.getEnchantLevel(hand, EnchantType.VOID_STICK);
        if (voidStickLevel > 0) {
            Location destination = findFarthestSafeAlongDirection(player.getLocation(), voidStickDistanceForLevel(voidStickLevel));
            if (destination == null) {
                player.sendActionBar(net.kyori.adventure.text.Component.text("Void Stick: no safe destination found."));
                return true;
            }
            player.teleport(destination);
            player.getWorld().playSound(destination, Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.1F);
            decrementRodUse(player, hand, rodUsesKey(EnchantType.VOID_STICK), VOID_STICK_MAX_USES, "Void Stick");
            leftClickCooldownMs.put(player.getUniqueId(), now + 150L);
            return true;
        }

        int fireballLevel = plugin.getEnchantLevel(hand, EnchantType.FIREBALL);
        if (fireballLevel > 0) {
            Fireball fireball = player.launchProjectile(Fireball.class);
            fireball.setDirection(player.getLocation().getDirection().normalize());
            fireball.setVelocity(player.getLocation().getDirection().normalize().multiply(1.6D));
            fireball.setYield(2.0F);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.8F, 1.0F);
            decrementRodUse(player, hand, rodUsesKey(EnchantType.FIREBALL), fireballMaxUsesForLevel(fireballLevel), "Fireball");
            leftClickCooldownMs.put(player.getUniqueId(), now + 150L);
            return true;
        }
        return false;
    }

    private void decrementRodUse(@NotNull Player player,
                                 @NotNull ItemStack rod,
                                 @NotNull NamespacedKey key,
                                 int defaultUses,
                                 @NotNull String label) {
        ItemMeta meta = rod.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Integer stored = pdc.get(key, PersistentDataType.INTEGER);
        int uses = (stored == null ? defaultUses : stored) - 1;
        if (uses <= 0) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0F, 1.0F);
            return;
        }
        pdc.set(key, PersistentDataType.INTEGER, uses);
        rod.setItemMeta(meta);
        player.getInventory().setItemInMainHand(rod);
        player.sendActionBar(net.kyori.adventure.text.Component.text(label + ": " + uses + " uses left."));
    }

    private void processCustomBootMovement(@NotNull PlayerMoveEvent event, long tickCounter) {
        Location to = event.getTo();
        Location from = event.getFrom();
        if (to == null || from.getWorld() != to.getWorld()) {
            return;
        }
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.0001D) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack boots = player.getInventory().getBoots();
        if (boots == null || boots.getType() == Material.AIR) {
            return;
        }

        int farmers = plugin.getEnchantLevel(boots, EnchantType.FARMERS_DREAM);
        if (farmers > 0 && consumeBootDistance(player, boots, horizontal, bootDistanceKey(EnchantType.FARMERS_DREAM), FARMERS_DREAM_MAX_DISTANCE, "Farmer's Dream")) {
            growNearbyCrops(to.getBlock());
        }

        int lavaStep = plugin.getEnchantLevel(boots, EnchantType.LAVA_STEP);
        if (lavaStep > 0 && consumeBootDistance(player, boots, horizontal, bootDistanceKey(EnchantType.LAVA_STEP), LAVA_STEP_MAX_DISTANCE, "Lava Step")) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, true, false, false), true);
            solidifyLavaUnderPlayer(to, tickCounter);
        }
    }

    private boolean consumeBootDistance(@NotNull Player player,
                                        @NotNull ItemStack boots,
                                        double moved,
                                        @NotNull NamespacedKey key,
                                        double defaultDistance,
                                        @NotNull String label) {
        ItemMeta meta = boots.getItemMeta();
        if (meta == null) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        Double stored = pdc.get(key, PersistentDataType.DOUBLE);
        double remaining = (stored == null ? defaultDistance : stored) - moved;
        if (remaining <= 0.0D) {
            player.getInventory().setBoots(null);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.0F, 1.0F);
            player.sendActionBar(net.kyori.adventure.text.Component.text(label + " has broken."));
            return false;
        }
        pdc.set(key, PersistentDataType.DOUBLE, remaining);
        boots.setItemMeta(meta);
        player.getInventory().setBoots(boots);
        return true;
    }

    private void growNearbyCrops(@NotNull Block reference) {
        growCrop(reference);
        growCrop(reference.getRelative(0, -1, 0));
    }

    private void growCrop(@NotNull Block block) {
        Material type = block.getType();
        if (!Tag.CROPS.isTagged(type) && type != Material.NETHER_WART && type != Material.COCOA) {
            return;
        }
        BlockData data = block.getBlockData();
        if (!(data instanceof Ageable ageable)) {
            return;
        }
        if (ageable.getAge() >= ageable.getMaximumAge()) {
            return;
        }
        ageable.setAge(ageable.getMaximumAge());
        block.setBlockData(ageable, false);
    }

    private void solidifyLavaUnderPlayer(@NotNull Location location, long tickCounter) {
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        int baseY = (int) Math.floor(location.getY()) - 1;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block block = world.getBlockAt(location.getBlockX() + dx, baseY, location.getBlockZ() + dz);
                if (block.getType() != Material.LAVA) {
                    continue;
                }
                String key = tempKey(block);
                TempLavaBlock existing = temporaryLavaPath.get(key);
                TempLavaBlock next = existing == null
                        ? new TempLavaBlock(world.getName(), block.getX(), block.getY(), block.getZ(), block.getBlockData().clone(), tickCounter + LAVA_STEP_REVERT_TICKS)
                        : new TempLavaBlock(existing.worldName(), existing.x(), existing.y(), existing.z(), existing.originalData(), tickCounter + LAVA_STEP_REVERT_TICKS);
                temporaryLavaPath.put(key, next);
                block.setType(Material.OBSIDIAN, false);
            }
        }
    }

    private void expireTemporaryLavaPath(long tickCounter) {
        Iterator<Map.Entry<String, TempLavaBlock>> iterator = temporaryLavaPath.entrySet().iterator();
        while (iterator.hasNext()) {
            TempLavaBlock temp = iterator.next().getValue();
            if (tickCounter < temp.revertTick()) {
                continue;
            }
            World world = Bukkit.getWorld(temp.worldName());
            if (world != null) {
                Block block = world.getBlockAt(temp.x(), temp.y(), temp.z());
                if (block.getType() == Material.OBSIDIAN) {
                    block.setBlockData(temp.originalData(), false);
                }
            }
            iterator.remove();
        }
    }

    private void consumeMinersVoidStep(@NotNull Player player) {
        Location current = player.getLocation();
        World world = current.getWorld();
        if (world == null) {
            return;
        }
        int x = current.getBlockX();
        int z = current.getBlockZ();
        int highest = world.getHighestBlockYAt(x, z);
        if (current.getY() < highest - 1) {
            Location surface = findSafeAtOrAbove(world, x, z, highest + 1);
            if (surface != null) {
                player.teleport(surface);
            }
            return;
        }
        Location cave = findLowestNearbyCave(player, 24);
        if (cave == null) {
            cave = findLowestInColumn(world, x, z, (int) Math.floor(current.getY()));
        }
        if (cave != null) {
            player.teleport(cave);
        }
    }

    private void consumeSwap(@NotNull Player player) {
        Player nearest = nearestOtherPlayer(player);
        if (nearest == null) {
            player.teleport(player.getWorld().getSpawnLocation().clone().add(0.5D, 0.0D, 0.5D));
            return;
        }
        Location self = player.getLocation().clone();
        Location other = nearest.getLocation().clone();
        player.teleport(other);
        nearest.teleport(self);
    }

    private void consumeVitalityThief(@NotNull Player player) {
        Player nearest = nearestOtherPlayer(player);
        if (nearest == null) {
            int killed = 0;
            int heartsGained = 0;
            double absorption = player.getAbsorptionAmount();
            int heartsUntilCap = (int) Math.floor((MAX_GOLDEN_HEARTS_ABSORPTION - absorption) / 2.0D);
            if (heartsUntilCap <= 0) {
                return;
            }
            for (Entity entity : player.getNearbyEntities(20.0D, 20.0D, 20.0D)) {
                if (heartsUntilCap <= 0) {
                    break;
                }
                if (entity instanceof LivingEntity living && !(living instanceof Player)) {
                    living.damage(1000.0D, player);
                    killed++;
                    heartsGained++;
                    heartsUntilCap--;
                }
            }
            AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                player.setHealth(maxHealth.getValue());
            }
            double newAbsorption = Math.min(MAX_GOLDEN_HEARTS_ABSORPTION, player.getAbsorptionAmount() + (heartsGained * 2.0D));
            player.setAbsorptionAmount(newAbsorption);
            int amplifier = Math.max(0, (int) Math.ceil(newAbsorption / 4.0D) - 1);
            player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 60 * 10, amplifier, true, false, false), true);
            player.sendActionBar(net.kyori.adventure.text.Component.text("Vitality Thief: drained " + killed + " mobs."));
            return;
        }

        AttributeInstance self = player.getAttribute(Attribute.MAX_HEALTH);
        AttributeInstance other = nearest.getAttribute(Attribute.MAX_HEALTH);
        if (self == null || other == null) {
            return;
        }
        double selfBase = self.getBaseValue();
        double otherBase = other.getBaseValue();
        double selfCurrent = player.getHealth();
        double otherCurrent = nearest.getHealth();
        double selfAbsorption = player.getAbsorptionAmount();
        double otherAbsorption = nearest.getAbsorptionAmount();
        self.setBaseValue(otherBase);
        other.setBaseValue(selfBase);
        player.setHealth(Math.min(otherCurrent, self.getValue()));
        nearest.setHealth(Math.min(selfCurrent, other.getValue()));
        player.setAbsorptionAmount(otherAbsorption);
        nearest.setAbsorptionAmount(selfAbsorption);
    }

    private void consumeBedTime(@NotNull Player player) {
        Location destination = player.getRespawnLocation();
        if (destination == null) {
            destination = player.getWorld().getSpawnLocation();
        }
        player.teleport(destination.clone().add(0.5D, 0.0D, 0.5D));
    }

    private void consumeLifeSpirit(@NotNull Player player, int level) {
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        int heartsToGain = Math.max(1, Math.min(3, level));
        maxHealth.setBaseValue(maxHealth.getBaseValue() + (heartsToGain * 2.0D));
        UUID id = player.getUniqueId();
        lifeSpiritHeartsGained.put(id, lifeSpiritHeartsGained.getOrDefault(id, 0) + heartsToGain);
    }

    private void consumeLifeSteal(@NotNull Player player, int level) {
        Player nearest = nearestOtherPlayer(player);
        if (nearest == null) {
            return;
        }
        AttributeInstance self = player.getAttribute(Attribute.MAX_HEALTH);
        AttributeInstance other = nearest.getAttribute(Attribute.MAX_HEALTH);
        if (self == null || other == null) {
            return;
        }
        int heartsToSteal = Math.max(1, Math.min(3, level));
        double otherBase = other.getBaseValue();
        if (otherBase <= 2.0D) {
            return;
        }
        self.setBaseValue(self.getBaseValue() + (heartsToSteal * 2.0D));
        other.setBaseValue(Math.max(2.0D, otherBase - (heartsToSteal * 2.0D)));
        if (nearest.getHealth() <= LIFE_STEAL_EXECUTE_HEALTH) {
            nearest.setHealth(0.0D);
        } else {
            nearest.setHealth(Math.min(nearest.getHealth(), other.getValue()));
        }
        UUID id = player.getUniqueId();
        lifeStealHeartsGained.put(id, lifeStealHeartsGained.getOrDefault(id, 0) + heartsToSteal);
    }

    private double voidStickDistanceForLevel(int level) {
        if (level >= 3) {
            return 1000.0D;
        }
        if (level == 2) {
            return 500.0D;
        }
        return 200.0D;
    }

    private int fireballMaxUsesForLevel(int level) {
        return level >= 2 ? FIREBALL_LEVEL_2_MAX_USES : FIREBALL_LEVEL_1_MAX_USES;
    }

    private void removeLifeHeartsOnDeath(@NotNull UUID playerId) {
        int lifeSteal = lifeStealHeartsGained.getOrDefault(playerId, 0);
        int lifeSpirit = lifeSpiritHeartsGained.getOrDefault(playerId, 0);
        if (lifeSteal <= 0 && lifeSpirit <= 0) {
            return;
        }
        Player player = Bukkit.getPlayer(playerId);
        if (player == null) {
            lifeStealHeartsGained.remove(playerId);
            lifeSpiritHeartsGained.remove(playerId);
            return;
        }
        AttributeInstance maxHealth = player.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth == null) {
            lifeStealHeartsGained.remove(playerId);
            lifeSpiritHeartsGained.remove(playerId);
            return;
        }
        if (lifeSteal > 0) {
            maxHealth.setBaseValue(Math.max(2.0D, maxHealth.getBaseValue() - (lifeSteal * 2.0D)));
            lifeStealHeartsGained.remove(playerId);
        }
        if (lifeSpirit > 0) {
            maxHealth.setBaseValue(Math.max(2.0D, maxHealth.getBaseValue() - 2.0D));
            if (lifeSpirit > 1) {
                lifeSpiritHeartsGained.put(playerId, lifeSpirit - 1);
            } else {
                lifeSpiritHeartsGained.remove(playerId);
            }
        }
    }

    private void applyMagnetism(@NotNull Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        int level = Math.max(
                plugin.getEnchantLevel(main, EnchantType.MAGNETISM),
                plugin.getEnchantLevel(off, EnchantType.MAGNETISM)
        );
        if (level <= 0) {
            return;
        }
        if (!hasPickupSpace(player)) {
            return;
        }
        double radius = magnetismRadiusForLevel(level);
        plugin.pullNearbyItems(player, radius);
        plugin.pullNearbyExperienceOrbs(player, radius);
    }

    private void applyCursedMagnetism(@NotNull Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        ItemStack off = player.getInventory().getItemInOffHand();
        int level = Math.max(
                plugin.getEnchantLevel(main, EnchantType.CURSED_MAGNETISM),
                plugin.getEnchantLevel(off, EnchantType.CURSED_MAGNETISM)
        );
        if (level <= 0) {
            return;
        }
        double radius = level * 10.0D;
        Location center = player.getLocation();
        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof LivingEntity living) || living instanceof Player) {
                continue;
            }
            org.bukkit.util.Vector toward = center.toVector().subtract(living.getLocation().toVector());
            if (toward.lengthSquared() < 0.0001D) {
                continue;
            }
            living.setVelocity(toward.normalize().multiply(0.14D));
        }
    }

    private void applyTrueSilence(@NotNull Player player) {
        UUID playerId = player.getUniqueId();
        ItemStack boots = player.getInventory().getBoots();
        boolean active = plugin.getEnchantLevel(boots, EnchantType.TRUE_SILENCE) > 0;
        if (active) {
            if (!player.isSilent()) {
                player.setSilent(true);
            }
            trueSilenceActive.put(playerId, (byte) 1);
            return;
        }
        if (trueSilenceActive.remove(playerId) != null && player.isSilent()) {
            player.setSilent(false);
        }
    }

    private @Nullable Location findFarthestSafeAlongDirection(@NotNull Location start, double maxDistance) {
        World world = start.getWorld();
        if (world == null) {
            return null;
        }
        org.bukkit.util.Vector direction = start.getDirection().clone().normalize();
        for (int distance = (int) Math.floor(maxDistance); distance >= 1; distance--) {
            Location point = start.clone().add(direction.clone().multiply(distance));
            if (isSafeStandSpot(world, point.getBlockX(), point.getBlockY(), point.getBlockZ())) {
                return centered(world, point.getBlockX(), point.getBlockY(), point.getBlockZ(), start.getYaw(), start.getPitch());
            }
        }
        return null;
    }

    private @Nullable Location findSafeAtOrAbove(@NotNull World world, int x, int z, int startY) {
        int minY = world.getMinHeight() + 1;
        int maxY = world.getMaxHeight() - 2;
        int safeStart = Math.max(minY, Math.min(startY, maxY));
        for (int y = safeStart; y <= Math.min(maxY, safeStart + 8); y++) {
            if (isSafeStandSpot(world, x, y, z)) {
                return centered(world, x, y, z, 0.0F, 0.0F);
            }
        }
        for (int y = safeStart - 1; y >= minY; y--) {
            if (isSafeStandSpot(world, x, y, z)) {
                return centered(world, x, y, z, 0.0F, 0.0F);
            }
        }
        return null;
    }

    private @Nullable Location findLowestNearbyCave(@NotNull Player player, int radius) {
        Location origin = player.getLocation();
        World world = origin.getWorld();
        if (world == null) {
            return null;
        }
        int minY = world.getMinHeight() + 1;
        int ox = origin.getBlockX();
        int oz = origin.getBlockZ();
        int oy = origin.getBlockY();
        Location best = null;
        int bestY = Integer.MAX_VALUE;
        double bestDistance = Double.MAX_VALUE;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = ox + dx;
                int z = oz + dz;
                int surface = world.getHighestBlockYAt(x, z);
                int startY = Math.min(oy - 1, surface - 2);
                for (int y = startY; y >= minY; y--) {
                    if (!isSafeStandSpot(world, x, y, z) || y >= surface - 1) {
                        continue;
                    }
                    Location candidate = centered(world, x, y, z, origin.getYaw(), origin.getPitch());
                    double distance = candidate.distanceSquared(origin);
                    if (y < bestY || (y == bestY && distance < bestDistance)) {
                        bestY = y;
                        bestDistance = distance;
                        best = candidate;
                    }
                }
            }
        }
        return best;
    }

    private @Nullable Location findLowestInColumn(@NotNull World world, int x, int z, int fromY) {
        int minY = world.getMinHeight() + 1;
        for (int y = Math.max(minY, fromY); y >= minY; y--) {
            if (isSafeStandSpot(world, x, y, z)) {
                return centered(world, x, y, z, 0.0F, 0.0F);
            }
        }
        return null;
    }

    private boolean isSafeStandSpot(@NotNull World world, int x, int y, int z) {
        int minY = world.getMinHeight() + 1;
        int maxY = world.getMaxHeight() - 2;
        if (y < minY || y > maxY) {
            return false;
        }
        Block feet = world.getBlockAt(x, y, z);
        Block head = world.getBlockAt(x, y + 1, z);
        Block below = world.getBlockAt(x, y - 1, z);
        if (!feet.isPassable() || !head.isPassable() || !below.getType().isSolid()) {
            return false;
        }
        return !isDangerous(feet.getType()) && !isDangerous(head.getType()) && !isDangerous(below.getType());
    }

    private boolean isDangerous(@NotNull Material material) {
        return material == Material.LAVA
                || material == Material.FIRE
                || material == Material.SOUL_FIRE
                || material == Material.CACTUS
                || material == Material.CAMPFIRE
                || material == Material.SOUL_CAMPFIRE
                || material == Material.MAGMA_BLOCK
                || material == Material.SWEET_BERRY_BUSH;
    }

    private @NotNull Location centered(@NotNull World world, int x, int y, int z, float yaw, float pitch) {
        return new Location(world, x + 0.5D, y, z + 0.5D, yaw, pitch);
    }

    private @Nullable Player nearestOtherPlayer(@NotNull Player source) {
        Location src = source.getLocation();
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;
        for (Player candidate : Bukkit.getOnlinePlayers()) {
            if (candidate.getUniqueId().equals(source.getUniqueId()) || candidate.getWorld() != source.getWorld()) {
                continue;
            }
            double distance = candidate.getLocation().distanceSquared(src);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        if (nearest != null) {
            return nearest;
        }
        for (Player candidate : Bukkit.getOnlinePlayers()) {
            if (!candidate.getUniqueId().equals(source.getUniqueId())) {
                return candidate;
            }
        }
        return null;
    }

    private @NotNull String tempKey(@NotNull Block block) {
        return block.getWorld().getName() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }

    private @NotNull NamespacedKey rodUsesKey(@NotNull EnchantType type) {
        return new NamespacedKey(plugin, "uses_" + type.arg);
    }

    private @NotNull NamespacedKey bootDistanceKey(@NotNull EnchantType type) {
        return new NamespacedKey(plugin, "distance_" + type.arg);
    }

    private long secondsRemaining(long untilTick, long tickCounter) {
        return Math.max(1L, (untilTick - tickCounter + 19L) / 20L);
    }

    private boolean isActionSuppressed(@NotNull UUID playerId, long nowMs) {
        long lastRightClick = lastRightClickAtMs.getOrDefault(playerId, 0L);
        if (nowMs - lastRightClick < 250L) {
            return true;
        }
        long lastInventoryAction = lastInventoryActionAtMs.getOrDefault(playerId, 0L);
        return nowMs - lastInventoryAction < 250L;
    }

    private boolean hasPickupSpace(@NotNull Player player) {
        ItemStack[] storage = player.getInventory().getStorageContents();
        for (ItemStack slot : storage) {
            if (slot == null || slot.getType() == Material.AIR) {
                return true;
            }
        }
        return false;
    }

    private double magnetismRadiusForLevel(int level) {
        return switch (Math.max(1, Math.min(4, level))) {
            case 1 -> 5.0D;
            case 2 -> 10.0D;
            case 3 -> 15.0D;
            default -> 30.0D;
        };
    }

    private void tryHandleDeferredAnimationCast(@NotNull UUID playerId, long animationTick) {
        Player player = Bukkit.getPlayer(playerId);
        if (player == null || !player.isOnline()) {
            return;
        }
        if (lastDropTick.getOrDefault(playerId, Long.MIN_VALUE) >= animationTick) {
            return;
        }
        long now = System.currentTimeMillis();
        if (isActionSuppressed(playerId, now)) {
            return;
        }
        ItemStack hand = player.getInventory().getItemInMainHand();
        handleCustomRodLeftClick(player, hand, Action.LEFT_CLICK_AIR, animationTick);
    }

    private void setListed(@NotNull Player player, boolean listed) {
        try {
            Method method = Player.class.getMethod("setListed", boolean.class);
            method.invoke(player, listed);
        } catch (Throwable ignored) {
        }
    }

    private record InfectedState(@NotNull EntityType originalType,
                                 @NotNull Location location,
                                 long expireTick) {
    }

    private record TempLavaBlock(@NotNull String worldName,
                                 int x,
                                 int y,
                                 int z,
                                 @NotNull BlockData originalData,
                                 long revertTick) {
    }
}
