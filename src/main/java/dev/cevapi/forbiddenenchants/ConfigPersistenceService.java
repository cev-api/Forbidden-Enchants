package dev.cevapi.forbiddenenchants;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.generator.structure.Structure;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

final class ConfigPersistenceService {
    private final ForbiddenEnchantsPlugin plugin;

    ConfigPersistenceService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    void loadStructureInjectorSettings() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        refreshAllStructures();

        plugin.structureInjectChances().clear();
        plugin.structureInjectLootModes().clear();
        plugin.structureInjectMysteryStates().clear();
        ConfigurationSection root = plugin.getConfig().getConfigurationSection("structure_injector");
        if (root == null) {
            plugin.setStructureInjectorEnabled(false);
            plugin.setStructureInjectDefaultChance(defaultStructureInjectChance());
            plugin.setStructureInjectNotifyOnAdd(false);
            plugin.setTrialVaultInjectorEnabled(false);
            plugin.setTrialVaultNormalChance(defaultVaultInjectChance());
            plugin.setTrialVaultOminousChance(defaultVaultInjectChance());
            plugin.setTrialVaultNormalLootMode(InjectorLootMode.ALL);
            plugin.setTrialVaultOminousLootMode(InjectorLootMode.ALL);
            plugin.setTrialVaultNormalMysteryState(InjectorMysteryState.ALL);
            plugin.setTrialVaultOminousMysteryState(InjectorMysteryState.ALL);
            saveStructureInjectorSettings();
            return;
        }

        plugin.setStructureInjectorEnabled(root.getBoolean("enabled", false));
        plugin.setStructureInjectDefaultChance(clampChance(root.getDouble("default_chance", defaultStructureInjectChance())));
        plugin.setStructureInjectNotifyOnAdd(root.getBoolean("notify_on_add", false));
        plugin.setTrialVaultInjectorEnabled(root.getBoolean("vaults.enabled", false));
        plugin.setTrialVaultNormalChance(clampChance(root.getDouble("vaults.normal_chance", defaultVaultInjectChance())));
        plugin.setTrialVaultOminousChance(clampChance(root.getDouble("vaults.ominous_chance", defaultVaultInjectChance())));
        InjectorLootMode normalVaultMode = InjectorLootMode.fromString(root.getString("vaults.normal_loot_mode"));
        InjectorLootMode ominousVaultMode = InjectorLootMode.fromString(root.getString("vaults.ominous_loot_mode"));
        plugin.setTrialVaultNormalLootMode(normalVaultMode == null ? InjectorLootMode.ALL : normalVaultMode);
        plugin.setTrialVaultOminousLootMode(ominousVaultMode == null ? InjectorLootMode.ALL : ominousVaultMode);

        InjectorMysteryState normalVaultMystery = InjectorMysteryState.fromString(root.getString("vaults.normal_mystery_state"));
        InjectorMysteryState ominousVaultMystery = InjectorMysteryState.fromString(root.getString("vaults.ominous_mystery_state"));
        InjectorMysteryState normalLegacyMystery = InjectorMysteryState.fromLegacyModeAlias(root.getString("vaults.normal_loot_mode"));
        InjectorMysteryState ominousLegacyMystery = InjectorMysteryState.fromLegacyModeAlias(root.getString("vaults.ominous_loot_mode"));
        plugin.setTrialVaultNormalMysteryState(normalVaultMystery != null ? normalVaultMystery : (normalLegacyMystery != null ? normalLegacyMystery : InjectorMysteryState.ALL));
        plugin.setTrialVaultOminousMysteryState(ominousVaultMystery != null ? ominousVaultMystery : (ominousLegacyMystery != null ? ominousLegacyMystery : InjectorMysteryState.ALL));

        ConfigurationSection structuresSection = root.getConfigurationSection("structures");
        if (structuresSection != null) {
            for (String rawKey : structuresSection.getKeys(false)) {
                NamespacedKey key = NamespacedKey.fromString(rawKey);
                if (key == null) {
                    continue;
                }
                if (Registry.STRUCTURE.get(key) == null) {
                    continue;
                }
                double chance = clampChance(structuresSection.getDouble(rawKey, 0.0D));
                plugin.structureInjectChances().put(key, chance);
            }
        }

        ConfigurationSection lootModeSection = root.getConfigurationSection("loot_mode");
        if (lootModeSection != null) {
            for (String rawKey : lootModeSection.getKeys(false)) {
                NamespacedKey key = NamespacedKey.fromString(rawKey);
                if (key == null || Registry.STRUCTURE.get(key) == null) {
                    continue;
                }
                InjectorLootMode mode = InjectorLootMode.fromString(lootModeSection.getString(rawKey));
                if (mode != null && mode != InjectorLootMode.ALL) {
                    plugin.structureInjectLootModes().put(key, mode);
                }
                InjectorMysteryState legacyMystery = InjectorMysteryState.fromLegacyModeAlias(lootModeSection.getString(rawKey));
                if (legacyMystery != null && legacyMystery != InjectorMysteryState.ALL) {
                    plugin.structureInjectMysteryStates().put(key, legacyMystery);
                }
            }
        }

        ConfigurationSection mysteryStateSection = root.getConfigurationSection("mystery_state");
        if (mysteryStateSection != null) {
            for (String rawKey : mysteryStateSection.getKeys(false)) {
                NamespacedKey key = NamespacedKey.fromString(rawKey);
                if (key == null || Registry.STRUCTURE.get(key) == null) {
                    continue;
                }
                InjectorMysteryState state = InjectorMysteryState.fromString(mysteryStateSection.getString(rawKey));
                if (state == null || state == InjectorMysteryState.ALL) {
                    continue;
                }
                plugin.structureInjectMysteryStates().put(key, state);
            }
        }

        // Backward compatibility: migrate legacy mystery_only booleans.
        ConfigurationSection mysteryOnlySection = root.getConfigurationSection("mystery_only");
        if (mysteryOnlySection != null) {
            for (String rawKey : mysteryOnlySection.getKeys(false)) {
                if (!mysteryOnlySection.getBoolean(rawKey, false)) {
                    continue;
                }
                NamespacedKey key = NamespacedKey.fromString(rawKey);
                if (key == null || Registry.STRUCTURE.get(key) == null) {
                    continue;
                }
                if (!plugin.structureInjectMysteryStates().containsKey(key)) {
                    plugin.structureInjectMysteryStates().put(key, InjectorMysteryState.MYSTERY_ONLY);
                }
            }
        }
    }

    void saveStructureInjectorSettings() {
        plugin.getConfig().set("structure_injector.enabled", plugin.isStructureInjectorEnabled());
        plugin.getConfig().set("structure_injector.default_chance", plugin.getStructureInjectDefaultChance());
        plugin.getConfig().set("structure_injector.notify_on_add", plugin.isStructureInjectNotifyOnAdd());
        plugin.getConfig().set("structure_injector.vaults.enabled", plugin.isTrialVaultInjectorEnabled());
        plugin.getConfig().set("structure_injector.vaults.normal_chance", plugin.getTrialVaultNormalChance());
        plugin.getConfig().set("structure_injector.vaults.ominous_chance", plugin.getTrialVaultOminousChance());
        plugin.getConfig().set("structure_injector.vaults.normal_loot_mode", plugin.getTrialVaultNormalLootMode().id());
        plugin.getConfig().set("structure_injector.vaults.ominous_loot_mode", plugin.getTrialVaultOminousLootMode().id());
        plugin.getConfig().set("structure_injector.vaults.normal_mystery_state", plugin.getTrialVaultNormalMysteryState().id());
        plugin.getConfig().set("structure_injector.vaults.ominous_mystery_state", plugin.getTrialVaultOminousMysteryState().id());
        plugin.getConfig().set("structure_injector.structures", null);
        for (Map.Entry<NamespacedKey, Double> entry : plugin.structureInjectChances().entrySet()) {
            plugin.getConfig().set("structure_injector.structures." + entry.getKey(), entry.getValue());
        }
        plugin.getConfig().set("structure_injector.loot_mode", null);
        for (Map.Entry<NamespacedKey, InjectorLootMode> entry : plugin.structureInjectLootModes().entrySet()) {
            if (entry.getValue() != null && entry.getValue() != InjectorLootMode.ALL) {
                plugin.getConfig().set("structure_injector.loot_mode." + entry.getKey(), entry.getValue().id());
            }
        }
        plugin.getConfig().set("structure_injector.mystery_state", null);
        for (Map.Entry<NamespacedKey, InjectorMysteryState> entry : plugin.structureInjectMysteryStates().entrySet()) {
            if (entry.getValue() != null && entry.getValue() != InjectorMysteryState.ALL) {
                plugin.getConfig().set("structure_injector.mystery_state." + entry.getKey(), entry.getValue().id());
            }
        }
        plugin.getConfig().set("structure_injector.mystery_only", null);
        plugin.saveConfig();
    }

    void loadEnchantToggleSettings() {
        for (EnchantType type : EnchantType.values()) {
            String basePath = "enchant_controls." + type.arg;
            boolean useEnabled = plugin.getConfig().getBoolean(basePath + ".use_enabled", true);
            boolean spawnEnabled = plugin.getConfig().getBoolean(basePath + ".spawn_enabled", true);
            plugin.setEnchantUseEnabled(type, useEnabled);
            plugin.setEnchantSpawnEnabled(type, spawnEnabled);
        }
        saveEnchantToggleSettings();
    }

    void saveEnchantToggleSettings() {
        plugin.getConfig().set("enchant_controls", null);
        for (EnchantType type : EnchantType.values()) {
            String basePath = "enchant_controls." + type.arg;
            plugin.getConfig().set(basePath + ".use_enabled", plugin.isEnchantUseEnabled(type));
            plugin.getConfig().set(basePath + ".spawn_enabled", plugin.isEnchantSpawnEnabled(type));
        }
        plugin.saveConfig();
    }

    void refreshAllStructures() {
        plugin.allStructuresStoreForConfig().clear();
        try {
            for (Structure structure : Registry.STRUCTURE) {
                plugin.allStructuresStoreForConfig().add(structure);
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Could not read structure registry on this server build; injector GUI structure list may be limited.");
        }
        plugin.allStructuresStoreForConfig().sort((left, right) -> left.getKey().toString().compareToIgnoreCase(right.getKey().toString()));
    }

    private double clampChance(double chance) {
        return Math.max(0.0D, Math.min(100.0D, chance));
    }

    private double defaultStructureInjectChance() {
        return 5.0D;
    }

    private double defaultVaultInjectChance() {
        return 7.5D;
    }
}

