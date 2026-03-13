package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

public final class EvokersRevengeEnchant extends BaseForbiddenEnchant {
    private final Map<UUID, Long> nextSpawnTickByPlayer = new HashMap<>();
    private final Map<UUID, Integer> extraSpawnCountByPlayer = new HashMap<>();
    private @Nullable NamespacedKey spawnedMarkerKey;

    public EvokersRevengeEnchant() {
        super("evokers_revenge",
                "evokers_revenge_level",
                "Evokers Revenge",
                ArmorSlot.CHESTPLATE,
                1,
                NamedTextColor.DARK_RED,
                List.of("evokerrevenge", "evokersrevenge", "evokers_revenge"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Binding curse: random evokers spawn near you over time; each kill increases future count.";
    }

    public void initialize(@NotNull JavaPlugin plugin) {
        spawnedMarkerKey = new NamespacedKey(plugin, "evokers_revenge_spawned");
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
        NamespacedKey spawnedKey = spawnedMarkerKey;
        if (spawnedKey == null) {
            return;
        }

        int spawned = 0;
        for (int i = 0; i < amount; i++) {
            Location spawn = spawnLocationProvider.apply(player);
            if (spawn == null) {
                continue;
            }
            Evoker evoker = player.getWorld().spawn(spawn, Evoker.class);
            evoker.getPersistentDataContainer().set(spawnedKey, PersistentDataType.BYTE, (byte) 1);
            evoker.setTarget(player);
            spawned++;
        }

        if (spawned <= 0) {
            nextSpawnTickByPlayer.put(id, tickCounter + 40L);
            return;
        }
        long delay = ThreadLocalRandom.current().nextLong(20L * 120L, 20L * 240L);
        nextSpawnTickByPlayer.put(id, tickCounter + delay);
    }

    public void onEvokerKilledByOwner(@NotNull UUID ownerId) {
        extraSpawnCountByPlayer.put(ownerId, extraSpawnCountByPlayer.getOrDefault(ownerId, 0) + 1);
    }

    public void stripTotemDropIfSpawned(@NotNull Evoker evoker, @NotNull List<ItemStack> drops) {
        NamespacedKey spawnedKey = spawnedMarkerKey;
        if (spawnedKey == null) {
            return;
        }
        PersistentDataContainer pdc = evoker.getPersistentDataContainer();
        if (!pdc.has(spawnedKey, PersistentDataType.BYTE)) {
            return;
        }
        drops.removeIf(stack -> stack.getType() == Material.TOTEM_OF_UNDYING);
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
        boolean active = plugin().getEnchantLevel(chestplate, EnchantType.EVOKERS_REVENGE) > 0;
        tickPlayer(player, tickCounter, active, p -> plugin().findNearbySpawnLocation(p, 8.0D, 18.0D));
    }
}

