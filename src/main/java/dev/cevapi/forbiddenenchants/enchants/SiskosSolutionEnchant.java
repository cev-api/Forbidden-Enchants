package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public final class SiskosSolutionEnchant extends BaseForbiddenEnchant {
    private static final double TRIGGER_RADIUS = 30.0D;
    private static final double VILLAGER_CHAIN_RADIUS = 50.0D;
    private static final double SMOKE_AFFECT_RADIUS = 8.0D;

    private final Map<UUID, CloudState> activeClouds = new HashMap<>();
    private final Random random = new Random();
    private long lastProcessedTick = Long.MIN_VALUE;

    public SiskosSolutionEnchant() {
        super("siskos_solution",
                "siskos_solution_level",
                "Sisko's Solution",
                ArmorSlot.CHESTPLATE,
                1,
                NamedTextColor.BLACK,
                List.of("sisko", "siskos", "siskos_solution"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Near 3+ villagers, black ground-smoke spreads village-to-village while summoned withers ignore you.";
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        ItemStack chestplate = player.getInventory().getChestplate();
        if (plugin().getEnchantLevel(chestplate, EnchantType.SISKOS_SOLUTION) > 0
                && !activeClouds.containsKey(player.getUniqueId())) {
            List<Villager> nearby = villagersNear(player.getWorld(), player.getLocation(), TRIGGER_RADIUS);
            if (nearby.size() >= 3) {
                triggerCloud(player, nearby);
            }
        }

        if (tickCounter != lastProcessedTick) {
            processClouds(tickCounter);
            lastProcessedTick = tickCounter;
        }
    }

    @Override
    public void onDamage(@NotNull EntityDamageEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (activeClouds.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onMobTarget(@NotNull EntityTargetLivingEntityEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof WitherSkeleton skeleton) || !(event.getTarget() instanceof Player player)) {
            return;
        }
        if (activeClouds.containsKey(player.getUniqueId())) {
            event.setCancelled(true);
            skeleton.setTarget(null);
        }
    }

    private void triggerCloud(@NotNull Player player, @NotNull List<Villager> seedVillagers) {
        Set<UUID> ids = new HashSet<>();
        for (Villager villager : seedVillagers) {
            ids.add(villager.getUniqueId());
        }
        activeClouds.put(player.getUniqueId(), new CloudState(player.getUniqueId(), player.getWorld().getUID(), ids));

        player.getInventory().setChestplate(null);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0F, 0.5F);
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.9F, 0.75F);
    }

    private void processClouds(long tickCounter) {
        if (activeClouds.isEmpty()) {
            return;
        }
        List<UUID> removeOwners = new ArrayList<>();
        for (Map.Entry<UUID, CloudState> entry : activeClouds.entrySet()) {
            UUID ownerId = entry.getKey();
            Player owner = org.bukkit.Bukkit.getPlayer(ownerId);
            CloudState state = entry.getValue();
            if (owner == null || !owner.isOnline()) {
                removeOwners.add(ownerId);
                continue;
            }
            World world = owner.getWorld();
            if (!world.getUID().equals(state.worldId())) {
                removeOwners.add(ownerId);
                continue;
            }

            Set<Villager> networkVillagers = expandVillagerNetwork(world, state.villagerIds());
            if (networkVillagers.isEmpty()) {
                removeOwners.add(ownerId);
                continue;
            }
            Set<UUID> refreshedIds = new HashSet<>();
            for (Villager villager : networkVillagers) {
                refreshedIds.add(villager.getUniqueId());
            }
            activeClouds.put(ownerId, new CloudState(state.ownerId(), state.worldId(), refreshedIds));

            if (tickCounter % 5L == 0L) {
                spawnGroundSmoke(world, networkVillagers);
            }
            if (tickCounter % 20L == 0L) {
                applyWitherField(owner, world, networkVillagers);
                maybeSpawnWithers(world, networkVillagers);
            }
        }
        for (UUID ownerId : removeOwners) {
            activeClouds.remove(ownerId);
        }
    }

    private Set<Villager> expandVillagerNetwork(@NotNull World world, @NotNull Set<UUID> seeds) {
        Set<UUID> visited = new HashSet<>();
        ArrayDeque<Villager> queue = new ArrayDeque<>();
        for (UUID id : seeds) {
            Entity entity = world.getEntity(id);
            if (entity instanceof Villager villager && villager.isValid() && !villager.isDead()) {
                visited.add(id);
                queue.add(villager);
            }
        }
        while (!queue.isEmpty()) {
            Villager current = queue.poll();
            for (Entity nearby : current.getWorld().getNearbyEntities(
                    current.getLocation(),
                    VILLAGER_CHAIN_RADIUS,
                    18.0D,
                    VILLAGER_CHAIN_RADIUS,
                    entity -> entity instanceof Villager)) {
                Villager villager = (Villager) nearby;
                if (!villager.isValid() || villager.isDead()) {
                    continue;
                }
                if (visited.add(villager.getUniqueId())) {
                    queue.add(villager);
                }
            }
        }
        Set<Villager> out = new HashSet<>();
        for (UUID id : visited) {
            Entity entity = world.getEntity(id);
            if (entity instanceof Villager villager && villager.isValid() && !villager.isDead()) {
                out.add(villager);
            }
        }
        return out;
    }

    private void spawnGroundSmoke(@NotNull World world, @NotNull Set<Villager> villagers) {
        for (Villager villager : villagers) {
            Location at = villager.getLocation().clone().add(0.0D, 0.1D, 0.0D);
            world.spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, at, 45, 2.2D, 0.15D, 2.2D, 0.0D);
            world.spawnParticle(Particle.SMOKE, at, 35, 2.2D, 0.12D, 2.2D, 0.02D);
        }
    }

    private void applyWitherField(@NotNull Player owner, @NotNull World world, @NotNull Set<Villager> villagers) {
        Set<UUID> affected = new HashSet<>();
        for (Villager villager : villagers) {
            for (Entity entity : world.getNearbyEntities(
                    villager.getLocation(),
                    SMOKE_AFFECT_RADIUS,
                    6.0D,
                    SMOKE_AFFECT_RADIUS,
                    e -> e instanceof LivingEntity)) {
                if (!(entity instanceof LivingEntity living)) {
                    continue;
                }
                if (!affected.add(living.getUniqueId())) {
                    continue;
                }
                if (living.getUniqueId().equals(owner.getUniqueId())) {
                    continue;
                }
                if (living.getType() == EntityType.WITHER_SKELETON) {
                    continue;
                }
                living.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 100, 1, true, true, true), true);
                living.damage(1.0D, owner);
            }
        }
    }

    private void maybeSpawnWithers(@NotNull World world, @NotNull Set<Villager> villagers) {
        long activeWithers = villagers.stream()
                .flatMap(v -> world.getNearbyEntities(v.getLocation(), 40.0D, 20.0D, 40.0D,
                        entity -> entity.getType() == EntityType.WITHER_SKELETON).stream())
                .map(Entity::getUniqueId)
                .distinct()
                .count();
        if (activeWithers >= 24L) {
            return;
        }

        for (Villager villager : villagers) {
            if (random.nextDouble() > 0.14D) {
                continue;
            }
            Location base = villager.getLocation();
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = 4.0D + random.nextDouble() * 8.0D;
            Location spawn = base.clone().add(Math.cos(angle) * distance, 1.0D, Math.sin(angle) * distance);
            WitherSkeleton skeleton = (WitherSkeleton) world.spawnEntity(spawn, EntityType.WITHER_SKELETON);
            skeleton.setTarget(null);
        }
    }

    private List<Villager> villagersNear(@NotNull World world, @NotNull Location center, double radius) {
        List<Villager> villagers = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(center, radius, 18.0D, radius, e -> e instanceof Villager)) {
            Villager villager = (Villager) entity;
            if (villager.isValid() && !villager.isDead()) {
                villagers.add(villager);
            }
        }
        return villagers;
    }

    private record CloudState(@NotNull UUID ownerId, @NotNull UUID worldId, @NotNull Set<UUID> villagerIds) {
    }
}
