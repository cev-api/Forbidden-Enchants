package dev.cevapi.forbiddenenchants;

import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import dev.cevapi.forbiddenenchants.enchants.EnchantList;
import dev.cevapi.forbiddenenchants.enchants.MasqueradeEnchant;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;

final class MasqueradeService {
    private final ForbiddenEnchantsPlugin plugin;
    private final Map<UUID, MasqueradeEnchant.MasqueradeState> masqueradeStates;

    MasqueradeService(@NotNull ForbiddenEnchantsPlugin plugin,
                      @NotNull Map<UUID, MasqueradeEnchant.MasqueradeState> masqueradeStates) {
        this.plugin = plugin;
        this.masqueradeStates = masqueradeStates;
    }

    void maintain(@NotNull Player player) {
        EnchantList.INSTANCE.masquerade().maintain(plugin, masqueradeStates, player);
    }

    void start(@NotNull Player player) {
        EnchantList.INSTANCE.masquerade().start(plugin, masqueradeStates, player);
    }

    boolean isMasquerading(@NotNull Player player) {
        return EnchantList.INSTANCE.masquerade().isMasquerading(masqueradeStates, player);
    }

    void clear(@NotNull Player player) {
        EnchantList.INSTANCE.masquerade().clear(plugin, masqueradeStates, player);
    }

    void clearAll() {
        EnchantList.INSTANCE.masquerade().clearAll(plugin, masqueradeStates);
    }

    boolean shouldIgnoreMob(@NotNull Mob mob) {
        return EnchantList.INSTANCE.masquerade().shouldIgnoreMob(mob);
    }
}

