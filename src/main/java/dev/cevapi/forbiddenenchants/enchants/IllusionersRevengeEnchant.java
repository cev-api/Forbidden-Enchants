package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.entity.Illusioner;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public final class IllusionersRevengeEnchant extends BaseForbiddenEnchant {
    private final Map<UUID, Long> nextSpawnTickByPlayer = new HashMap<>();
    private final Map<UUID, Integer> extraSpawnCountByPlayer = new HashMap<>();

    public IllusionersRevengeEnchant() {
        super("illusioners_revenge",
                "illusioners_revenge_level",
                "Illusioners Revenge",
                ArmorSlot.CHESTPLATE,
                1,
                NamedTextColor.DARK_RED,
                List.of("illusioner_revenge", "illusioners_revenge", "illusionersrevenge"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Binding curse: random illusioners spawn near you over time; each kill increases future count.";
    }

    public void tickPlayer(@NotNull Player player,
                           long tickCounter,
                           boolean active,
                           @NotNull Function<Player, @Nullable Location> spawnLocationProvider) {
        UUID id = player.getUniqueId();
        if (!active) {
            clearFor(id);
            return;
        }

        long next = nextSpawnTickByPlayer.getOrDefault(id, 0L);
        if (next <= 0L) {
            nextSpawnTickByPlayer.put(id, tickCounter + ThreadLocalRandom.current().nextLong(20L * 30L, 20L * 60L));
            return;
        }
        if (tickCounter < next) {
            return;
        }

        int amount = Math.min(8, 1 + extraSpawnCountByPlayer.getOrDefault(id, 0));
        int spawned = 0;
        for (int i = 0; i < amount; i++) {
            Location spawn = spawnLocationProvider.apply(player);
            if (spawn == null) {
                continue;
            }
            Illusioner illusioner = player.getWorld().spawn(spawn, Illusioner.class);
            illusioner.setTarget(player);
            spawned++;
        }

        if (spawned <= 0) {
            nextSpawnTickByPlayer.put(id, tickCounter + 40L);
            return;
        }
        long delay = ThreadLocalRandom.current().nextLong(20L * 120L, 20L * 240L);
        nextSpawnTickByPlayer.put(id, tickCounter + delay);
    }

    public void onIllusionerKilledByOwner(@NotNull UUID ownerId) {
        extraSpawnCountByPlayer.put(ownerId, extraSpawnCountByPlayer.getOrDefault(ownerId, 0) + 1);
    }

    public void clearFor(@NotNull UUID playerId) {
        nextSpawnTickByPlayer.remove(playerId);
        extraSpawnCountByPlayer.remove(playerId);
    }

    public void resetAll() {
        nextSpawnTickByPlayer.clear();
        extraSpawnCountByPlayer.clear();
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        ItemStack chestplate = player.getInventory().getChestplate();
        boolean active = plugin().getEnchantLevel(chestplate, EnchantType.ILLUSIONERS_REVENGE) > 0;
        tickPlayer(player, tickCounter, active, p -> plugin().findNearbySpawnLocation(p, 8.0D, 18.0D));
    }
}
