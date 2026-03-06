package dev.cevapi.forbiddenenchants;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

final class WitheringStrikeService {
    private final Map<UUID, WitherState> witheringTargets;

    WitheringStrikeService(@NotNull Map<UUID, WitherState> witheringTargets) {
        this.witheringTargets = witheringTargets;
    }

    void addTarget(@NotNull UUID targetId, @NotNull UUID sourceId, long tickCounter) {
        witheringTargets.put(targetId, new WitherState(sourceId, tickCounter + 20L));
    }

    void clearTarget(@NotNull UUID targetId) {
        witheringTargets.remove(targetId);
    }

    void process(long tickCounter) {
        if (witheringTargets.isEmpty()) {
            return;
        }

        var iterator = witheringTargets.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, WitherState> entry = iterator.next();
            Entity rawTarget = Bukkit.getEntity(entry.getKey());
            if (!(rawTarget instanceof LivingEntity target) || !target.isValid() || target.isDead()) {
                iterator.remove();
                continue;
            }

            if (target.getPotionEffect(PotionEffectType.WITHER) == null) {
                iterator.remove();
                continue;
            }

            WitherState state = entry.getValue();
            if (tickCounter < state.nextDamageTick()) {
                continue;
            }

            Player source = Bukkit.getPlayer(state.sourcePlayerId());
            if (source != null && source.isOnline()) {
                target.damage(1.0D, source);
            } else {
                target.damage(1.0D);
            }
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 120, 0, true, true, true), true);
            entry.setValue(new WitherState(state.sourcePlayerId(), tickCounter + 60L));
        }
    }
}

