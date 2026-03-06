package dev.cevapi.forbiddenenchants;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

final class FearService {
    private final Map<UUID, FearState> fearedMobs;

    FearService(@NotNull Map<UUID, FearState> fearedMobs) {
        this.fearedMobs = fearedMobs;
    }

    void applyFear(@NotNull Mob mob, @NotNull Player source, long tickCounter) {
        fearedMobs.put(mob.getUniqueId(), new FearState(source.getUniqueId(), tickCounter + 50L));
    }

    void process(long tickCounter) {
        if (fearedMobs.isEmpty()) {
            return;
        }

        fearedMobs.entrySet().removeIf(entry -> {
            FearState state = entry.getValue();
            if (state.expireTick() <= tickCounter) {
                return true;
            }

            Entity entity = Bukkit.getEntity(entry.getKey());
            if (!(entity instanceof Mob mob) || !mob.isValid() || mob.isDead()) {
                return true;
            }

            Player source = Bukkit.getPlayer(state.sourcePlayerId());
            if (source == null || !source.isOnline()) {
                return true;
            }

            directMobToFlee(mob, source, tickCounter);
            return false;
        });
    }

    private void directMobToFlee(@NotNull Mob mob, @NotNull Player source, long tickCounter) {
        Vector away = mob.getLocation().toVector().subtract(source.getLocation().toVector());
        away.setY(0.0D);
        if (away.lengthSquared() < 0.0001D) {
            away = source.getLocation().getDirection().multiply(-1.0D);
            away.setY(0.0D);
        }
        away.normalize();

        var fleeTarget = mob.getLocation().clone().add(away.multiply(10.0D));
        fleeTarget.setY(mob.getLocation().getY());

        mob.setTarget(null);

        if (tickCounter % 3L == 0L || !mob.getPathfinder().hasPath()) {
            boolean moved = mob.getPathfinder().moveTo(fleeTarget, 1.35D);
            if (!moved && mob.isOnGround()) {
                mob.setVelocity(away.multiply(0.2D));
            }
        }
    }
}

