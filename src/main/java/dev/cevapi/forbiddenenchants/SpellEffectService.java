package dev.cevapi.forbiddenenchants;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
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

    SpellEffectService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    void processTick(long tickCounter) {
        processActiveInfectedCasters(tickCounter);
        expireQuitters(tickCounter);
        expireOutOfPhase(tickCounter);
        expireInfected(tickCounter);
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
        }
    }

    void onPlayerMove(@NotNull PlayerMoveEvent event, long tickCounter) {
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
        if (!isSilenced(event.getPlayer().getUniqueId(), tickCounter)) {
            return;
        }
        ItemStack hand = event.getItem();
        if (hand == null) {
            return;
        }
        Material type = hand.getType();
        if (type == Material.ENDER_PEARL || type == Material.TOTEM_OF_UNDYING) {
            event.setCancelled(true);
        }
    }

    void onEntityResurrect(@NotNull EntityResurrectEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (isSilenced(player.getUniqueId(), tickCounter)) {
            event.setCancelled(true);
        }
    }

    void onEnchantItem(@NotNull EnchantItemEvent event, long tickCounter) {
        if (isSilenced(event.getEnchanter().getUniqueId(), tickCounter)) {
            event.setCancelled(true);
        }
    }

    void onInventoryClick(@NotNull InventoryClickEvent event, long tickCounter) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
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
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            clearOnePlusForEquipped(player);
        }
        revealQuitter(playerId);
    }

    void onPlayerDeath(@NotNull UUID playerId) {
        outOfPhaseUntil.remove(playerId);
        infectedCasterUntil.remove(playerId);
        temporalDisplacementUntil.remove(playerId);
        onePlusUntil.remove(playerId);
        limitlessVisionActive.remove(playerId);
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
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

    private long secondsRemaining(long untilTick, long tickCounter) {
        return Math.max(1L, (untilTick - tickCounter + 19L) / 20L);
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
}
