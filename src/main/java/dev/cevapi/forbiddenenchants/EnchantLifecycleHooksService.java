package dev.cevapi.forbiddenenchants;

import dev.cevapi.forbiddenenchants.enchants.EnchantList;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

final class EnchantLifecycleHooksService {
    void onEnable(@NotNull ForbiddenEnchantsPlugin plugin) {
        EnchantList.INSTANCE.evokersRevenge().initialize(plugin);
    }

    void onDisable() {
        EnchantList.INSTANCE.appliedCurse().clearAll();
        EnchantList.INSTANCE.theUnyielding().resetAll(Bukkit.getOnlinePlayers());
        EnchantList.INSTANCE.theSeeker().resetAll();
        EnchantList.INSTANCE.forbiddenAgility().resetAll(Bukkit.getOnlinePlayers());
        EnchantList.INSTANCE.evokersRevenge().resetAll();
        EnchantList.INSTANCE.illusionersRevenge().resetAll();
        EnchantList.INSTANCE.mujahideen().resetAll();
    }

    void processTick(long tickCounter) {
        EnchantList.INSTANCE.appliedCurse().process(tickCounter);
    }
}

