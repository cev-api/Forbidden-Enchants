package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class AppliedCurseEnchant extends BaseForbiddenEnchant {
    private final Map<UUID, AppliedCurseState> cursesByPlayer = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> nameTagByPlayer = new HashMap<>();

    public AppliedCurseEnchant() {
        super("applied_curse",
                "applied_curse_level",
                "Applied Curse",
                ArmorSlot.NAMETAG,
                3,
                NamedTextColor.DARK_RED,
                List.of("appliedcurse", "applied_curse", "curse"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return switch (level) {
            case 1 -> "Name-tagged players are renamed for 30 minutes (name + chat).";
            case 2 -> "Name-tagged players are renamed for 1 hour (name + chat).";
            default -> "Name-tagged players are renamed until death (name + chat).";
        };
    }

    public void applyTo(@NotNull Player target, @NotNull String cursedName, long expireTick) {
        cursesByPlayer.put(target.getUniqueId(), new AppliedCurseState(cursedName, expireTick));
        target.displayName(Component.text(cursedName, NamedTextColor.DARK_RED));
        target.playerListName(Component.text(cursedName, NamedTextColor.DARK_RED));
        syncDisplay(target, cursedName);
    }

    public void process(long tickCounter) {
        if (cursesByPlayer.isEmpty()) {
            return;
        }
        for (Map.Entry<UUID, AppliedCurseState> entry : new ArrayList<>(cursesByPlayer.entrySet())) {
            UUID playerId = entry.getKey();
            AppliedCurseState state = entry.getValue();
            if (state.expireTick() >= 0 && tickCounter >= state.expireTick()) {
                Player player = Bukkit.getPlayer(playerId);
                if (player != null) {
                    clearFor(player);
                } else {
                    cursesByPlayer.remove(playerId);
                    removeNameTag(playerId);
                }
                continue;
            }
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                syncDisplay(player, state.cursedName());
            }
        }
    }

    public void onChat(@NotNull AsyncPlayerChatEvent event) {
        AppliedCurseState state = cursesByPlayer.get(event.getPlayer().getUniqueId());
        if (state == null) {
            return;
        }
        String format = event.getFormat();
        if (format.contains("%1$s")) {
            event.setFormat(format.replace("%1$s", state.cursedName()));
        }
    }

    public void clearFor(@NotNull Player player) {
        cursesByPlayer.remove(player.getUniqueId());
        player.displayName(Component.text(player.getName(), NamedTextColor.WHITE));
        player.playerListName(Component.text(player.getName(), NamedTextColor.WHITE));
        player.customName(null);
        player.setCustomNameVisible(false);
        Team team = getHiddenNameTeam(false);
        if (team != null) {
            team.removeEntry(player.getName());
        }
        removeNameTag(player.getUniqueId());
    }

    public void clearAll() {
        Team team = getHiddenNameTeam(false);
        for (UUID playerId : new ArrayList<>(cursesByPlayer.keySet())) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                clearFor(player);
                continue;
            }
            if (team != null) {
                OfflinePlayer offline = Bukkit.getOfflinePlayer(playerId);
                if (offline.getName() != null) {
                    team.removeEntry(offline.getName());
                }
            }
            removeNameTag(playerId);
        }
        cursesByPlayer.clear();
        nameTagByPlayer.clear();
    }

    private void syncDisplay(@NotNull Player player, @NotNull String cursedName) {
        Team team = getHiddenNameTeam(true);
        if (team != null && !team.hasEntry(player.getName())) {
            team.addEntry(player.getName());
        }

        UUID tagId = nameTagByPlayer.get(player.getUniqueId());
        ArmorStand stand = null;
        if (tagId != null) {
            Entity existing = Bukkit.getEntity(tagId);
            if (existing instanceof ArmorStand armorStand && armorStand.isValid() && !armorStand.isDead()) {
                stand = armorStand;
            }
        }
        if (stand == null) {
            Location spawnAt = player.getLocation().clone().add(0.0D, 2.25D, 0.0D);
            stand = player.getWorld().spawn(spawnAt, ArmorStand.class, as -> {
                as.setVisible(false);
                as.setMarker(true);
                as.setGravity(false);
                as.setSmall(true);
                as.setSilent(true);
                as.setInvulnerable(true);
                as.setPersistent(false);
                as.setCustomNameVisible(true);
            });
            nameTagByPlayer.put(player.getUniqueId(), stand.getUniqueId());
        }
        stand.customName(Component.text(cursedName, NamedTextColor.DARK_RED));
        stand.teleport(player.getLocation().clone().add(0.0D, 2.25D, 0.0D));
    }

    private @Nullable Team getHiddenNameTeam(boolean create) {
        if (Bukkit.getScoreboardManager() == null) {
            return null;
        }
        Scoreboard board = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = board.getTeam("fe_cursed_hidden");
        if (team != null || !create) {
            return team;
        }
        team = board.registerNewTeam("fe_cursed_hidden");
        team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.NEVER);
        return team;
    }

    private void removeNameTag(@NotNull UUID playerId) {
        UUID tagId = nameTagByPlayer.remove(playerId);
        if (tagId == null) {
            return;
        }
        Entity entity = Bukkit.getEntity(tagId);
        if (entity != null) {
            entity.remove();
        }
    }

    private record AppliedCurseState(String cursedName, long expireTick) {
    }
}

