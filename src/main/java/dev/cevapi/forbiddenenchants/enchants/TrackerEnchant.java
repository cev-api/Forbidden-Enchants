package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class TrackerEnchant extends BaseForbiddenEnchant {
    private static final double MAX_TRAIL_DISTANCE = 30.0D;
    private static final long TRAIL_DURATION_TICKS = 20L * 60L;

    private final Map<UUID, TrailState> trackedTargets = new HashMap<>();
    private long lastProcessedTick = -1L;

    public TrackerEnchant() {
        super("tracker",
                "tracker_level",
                "Tracker",
                ArmorSlot.SWORD,
                1,
                NamedTextColor.YELLOW,
                List.of("tracker", "track"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "On hit, target leaves a 30-block breadcrumb particle trail for 1 minute. Re-hits do not extend it.";
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity livingTarget)) {
            return;
        }
        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        if (!ForbiddenEnchantsPlugin.instance().isSword(weapon)) {
            return;
        }
        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(weapon, EnchantType.TRACKER);
        if (level <= 0) {
            return;
        }

        UUID targetId = livingTarget.getUniqueId();
        TrailState existing = trackedTargets.get(targetId);
        if (existing != null && tickCounter <= existing.expireTick()) {
            return;
        }

        Location start = livingTarget.getLocation().clone().add(0.0D, 1.0D, 0.0D);
        Deque<Location> crumbs = new ArrayDeque<>();
        crumbs.addLast(start.clone());
        trackedTargets.put(targetId, new TrailState(attacker.getUniqueId(), tickCounter + TRAIL_DURATION_TICKS, crumbs, start, 0.0D));
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        if (lastProcessedTick == tickCounter) {
            return;
        }
        lastProcessedTick = tickCounter;
        processTrails(tickCounter);
    }

    private void processTrails(long tickCounter) {
        trackedTargets.entrySet().removeIf(entry -> {
            TrailState state = entry.getValue();
            if (tickCounter > state.expireTick()) {
                return true;
            }

            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living) || living.isDead()) {
                return true;
            }

            Location now = living.getLocation().clone().add(0.0D, 1.0D, 0.0D);
            double moved = now.distance(state.lastLocation());
            if (moved >= 0.4D) {
                state.crumbs().addLast(now.clone());
                state.setTrailDistance(state.trailDistance() + moved);
                state.setLastLocation(now);
                trimToMaxDistance(state.crumbs(), state);
            }
            spawnTrailParticles(state.viewerId(), state.crumbs());
            return false;
        });
    }

    private void trimToMaxDistance(@NotNull Deque<Location> crumbs, @NotNull TrailState state) {
        while (crumbs.size() > 1 && state.trailDistance() > MAX_TRAIL_DISTANCE) {
            Location first = crumbs.removeFirst();
            Location second = crumbs.peekFirst();
            if (second != null && first.getWorld() == second.getWorld()) {
                state.setTrailDistance(Math.max(0.0D, state.trailDistance() - first.distance(second)));
            }
        }
    }

    private void spawnTrailParticles(@NotNull UUID viewerId, @NotNull Deque<Location> crumbs) {
        Player viewer = Bukkit.getPlayer(viewerId);
        if (viewer == null || !viewer.isOnline()) {
            return;
        }
        int index = 0;
        for (Location point : crumbs) {
            if ((index++ % 2) == 0) {
                viewer.spawnParticle(Particle.END_ROD, point, 1, 0.02D, 0.02D, 0.02D, 0.0D);
            }
        }
    }

    private static final class TrailState {
        private final UUID viewerId;
        private final long expireTick;
        private final Deque<Location> crumbs;
        private Location lastLocation;
        private double trailDistance;

        private TrailState(@NotNull UUID viewerId,
                           long expireTick,
                           @NotNull Deque<Location> crumbs,
                           @NotNull Location lastLocation,
                           double trailDistance) {
            this.viewerId = viewerId;
            this.expireTick = expireTick;
            this.crumbs = crumbs;
            this.lastLocation = lastLocation;
            this.trailDistance = trailDistance;
        }

        private @NotNull UUID viewerId() {
            return viewerId;
        }

        private long expireTick() {
            return expireTick;
        }

        private @NotNull Deque<Location> crumbs() {
            return crumbs;
        }

        private @NotNull Location lastLocation() {
            return lastLocation;
        }

        private void setLastLocation(@NotNull Location location) {
            this.lastLocation = location;
        }

        private double trailDistance() {
            return trailDistance;
        }

        private void setTrailDistance(double distance) {
            this.trailDistance = distance;
        }
    }
}
