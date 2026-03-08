package dev.cevapi.forbiddenenchants;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.data.type.Bed;
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

    private final ForbiddenEnchantsPlugin plugin;
    private final Map<UUID, Long> outOfPhaseUntil = new HashMap<>();
    private final Map<UUID, Long> silenceUntil = new HashMap<>();
    private final Map<UUID, Long> quitterUntil = new HashMap<>();
    private final Map<UUID, Long> infectedCasterUntil = new HashMap<>();
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
        wallPhaseCooldownMs.remove(playerId);
        revealQuitter(playerId);
    }

    void onPlayerDeath(@NotNull UUID playerId) {
        outOfPhaseUntil.remove(playerId);
        infectedCasterUntil.remove(playerId);
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
            source.sendActionBar(net.kyori.adventure.text.Component.text("Infected: no valid nearby targets."));
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
            player.sendActionBar(net.kyori.adventure.text.Component.text("Joint Sleep: warped to nearest shared bed."));
            return;
        }

        if (placeBedsInFront(player)) {
            player.sendActionBar(net.kyori.adventure.text.Component.text("Joint Sleep: placed 2 beds in front."));
            return;
        }

        ItemStack beds = new ItemStack(Material.WHITE_BED, 2);
        Map<Integer, ItemStack> overflow = player.getInventory().addItem(beds);
        for (ItemStack stack : overflow.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), stack);
        }
        player.sendActionBar(net.kyori.adventure.text.Component.text("Joint Sleep: no room to place beds, gave 2 beds."));
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
        Block foot = player.getLocation().getBlock().getRelative(facing);
        Block head = foot.getRelative(facing);
        if (!isBedPlaceable(foot) || !isBedPlaceable(head)) {
            return false;
        }
        placeBed(foot, facing, false);
        placeBed(head, facing, true);
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
