package dev.cevapi.forbiddenenchants;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
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
        plugin.injectorBookRarityWeights().clear();
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
            plugin.injectorBookRarityWeights().clear();
            plugin.setInjectorRarityApplyToItems(true);
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
        plugin.setInjectorRarityApplyToItems(root.getBoolean("rarity_apply_to_items", true));

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

        ConfigurationSection bookRaritySection = root.getConfigurationSection("book_rarity");
        if (bookRaritySection != null) {
            for (String key : bookRaritySection.getKeys(false)) {
                String[] parts = key.split(":", -1);
                if (parts.length != 2) {
                    continue;
                }
                EnchantType type = EnchantType.fromArg(parts[0]);
                if (type == null || plugin.isRetiredEnchant(type)) {
                    continue;
                }
                int level = parseInt(parts[1], -1);
                if (level < 1 || level > type.maxLevel) {
                    continue;
                }
                double weight = bookRaritySection.getDouble(key, 1.0D);
                plugin.setInjectorBookRarityWeight(type, level, weight);
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
        plugin.getConfig().set("structure_injector.rarity_apply_to_items", plugin.isInjectorRarityApplyToItems());
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
        plugin.getConfig().set("structure_injector.book_rarity", null);
        for (Map.Entry<String, Double> entry : plugin.injectorBookRarityWeights().entrySet()) {
            if (entry.getValue() != null && entry.getValue() >= 0.0D) {
                plugin.getConfig().set("structure_injector.book_rarity." + entry.getKey(), entry.getValue());
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

    void loadLibrarianTradeSettings() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();
        plugin.librarianTrades().clear();
        plugin.setLibrarianTradesEnabled(config.getBoolean("librarian_trades.enabled", false));

        List<?> rawEntries = config.getList("librarian_trades.trades", new ArrayList<>());
        for (Object rawEntry : rawEntries) {
            if (!(rawEntry instanceof Map<?, ?> map)) {
                continue;
            }
            String enchantArg = String.valueOf(map.get("enchant"));
            EnchantType type = EnchantType.fromArg(enchantArg);
            if (type == null || plugin.isRetiredEnchant(type)) {
                continue;
            }

            int level = parseInt(map.get("level"), 1);
            if (level < 1 || level > type.maxLevel) {
                continue;
            }

            double chance = clampChance(parseDouble(map.get("chance"), 100.0D));
            int emeraldCost = clamp(parseInt(map.get("emeralds"), 1), 1, 64);
            int bookCost = clamp(parseInt(map.get("books"), 1), 0, 64);
            if (chance > 0.0D) {
                plugin.librarianTrades().removeIf(entry -> entry.type() == type && entry.level() == level);
                plugin.librarianTrades().add(new LibrarianTradeEntry(type, level, chance, emeraldCost, bookCost));
            }
        }

        saveLibrarianTradeSettings();
    }

    void saveLibrarianTradeSettings() {
        plugin.getConfig().set("librarian_trades.enabled", plugin.isLibrarianTradesEnabled());
        plugin.getConfig().set("librarian_trades.trades", null);

        List<Map<String, Object>> serialized = new ArrayList<>();
        for (LibrarianTradeEntry entry : plugin.librarianTrades()) {
            serialized.add(Map.of(
                    "enchant", entry.type().arg,
                    "level", entry.level(),
                    "chance", entry.chancePercent(),
                    "emeralds", entry.emeraldCost(),
                    "books", entry.bookCost()
            ));
        }
        plugin.getConfig().set("librarian_trades.trades", serialized);
        plugin.saveConfig();
    }

    void loadEnchantingTableInjectorSettings() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();
        plugin.enchantingTableInjectorBooks().clear();

        ConfigurationSection root = config.getConfigurationSection("enchanting_table_injector");
        if (root == null) {
            plugin.setEnchantingTableInjectorEnabled(false);
            plugin.setEnchantingTableInjectorXpCost(35);
            saveEnchantingTableInjectorSettings();
            return;
        }

        plugin.setEnchantingTableInjectorEnabled(root.getBoolean("enabled", false));
        plugin.setEnchantingTableInjectorXpCost(clamp(parseInt(root.get("xp_cost"), 35), 1, 60));

        List<?> rawEntries = root.getList("books", new ArrayList<>());
        for (Object rawEntry : rawEntries) {
            if (!(rawEntry instanceof Map<?, ?> map)) {
                continue;
            }
            String enchantArg = String.valueOf(map.get("enchant"));
            EnchantType type = EnchantType.fromArg(enchantArg);
            if (type == null || plugin.isRetiredEnchant(type) || type.isAnvilOnlyUtilityBook()) {
                continue;
            }

            int level = parseInt(map.get("level"), 1);
            if (level < 1 || level > type.maxLevel) {
                continue;
            }

            double chance = clampChance(parseDouble(map.get("chance"), 0.0D));
            if (chance > 0.0D) {
                plugin.enchantingTableInjectorBooks().removeIf(entry -> entry.type() == type && entry.level() == level);
                plugin.enchantingTableInjectorBooks().add(new EnchantingTableBookEntry(type, level, chance));
            }
        }

        saveEnchantingTableInjectorSettings();
    }

    void saveEnchantingTableInjectorSettings() {
        plugin.getConfig().set("enchanting_table_injector.enabled", plugin.isEnchantingTableInjectorEnabled());
        plugin.getConfig().set("enchanting_table_injector.xp_cost", plugin.getEnchantingTableInjectorXpCost());
        plugin.getConfig().set("enchanting_table_injector.books", null);

        List<Map<String, Object>> serialized = new ArrayList<>();
        for (EnchantingTableBookEntry entry : plugin.enchantingTableInjectorBooks()) {
            serialized.add(Map.of(
                    "enchant", entry.type().arg,
                    "level", entry.level(),
                    "chance", entry.chancePercent()
            ));
        }
        plugin.getConfig().set("enchanting_table_injector.books", serialized);
        plugin.saveConfig();
    }

    void loadBundleDropSettings() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();

        FileConfiguration config = plugin.getConfig();
        plugin.bundleDropMobChances().clear();
        plugin.bundleDropRewards().clear();
        plugin.bundleDropExtraDrops().clear();

        ConfigurationSection root = config.getConfigurationSection("bundle_drop");
        if (root == null) {
            plugin.setBundleDropEnabled(false);
            plugin.setBundleDropChancePercent(5.0D);
            saveBundleDropSettings();
            return;
        }

        plugin.setBundleDropEnabled(root.getBoolean("enabled", false));
        plugin.setBundleDropChancePercent(clampChance(root.getDouble("chance", 5.0D)));

        ConfigurationSection mobChance = root.getConfigurationSection("mob_chance");
        if (mobChance != null) {
            for (String raw : mobChance.getKeys(false)) {
                try {
                    EntityType type = EntityType.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
                    if (!type.isAlive() || !type.isSpawnable() || type == EntityType.PLAYER) {
                        continue;
                    }
                    double chance = clampChance(mobChance.getDouble(raw, 0.0D));
                    if (chance > 0.0D) {
                        plugin.bundleDropMobChances().put(type, chance);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        // Backward compatibility: legacy mobs list uses default chance.
        List<String> legacyMobs = root.getStringList("mobs");
        for (String raw : legacyMobs) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            try {
                EntityType type = EntityType.valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
                if (!type.isAlive() || !type.isSpawnable() || type == EntityType.PLAYER) {
                    continue;
                }
                plugin.bundleDropMobChances().putIfAbsent(type, plugin.getBundleDropChancePercent());
            } catch (IllegalArgumentException ignored) {
            }
        }

        List<?> rawRewards = root.getList("rewards", new ArrayList<>());
        for (Object raw : rawRewards) {
            if (!(raw instanceof Map<?, ?> map)) {
                continue;
            }
            BundleDropRewardType type = BundleDropRewardType.fromString(String.valueOf(map.get("type")));
            if (type == null) {
                continue;
            }
            String key = String.valueOf(map.get("key"));
            int level = parseInt(map.get("level"), 1);
            int amount = parseInt(map.get("amount"), 1);
            if (key == null || key.isBlank()) {
                continue;
            }
            plugin.bundleDropRewards().add(new BundleDropReward(type, key, Math.max(1, level), Math.max(1, amount)));
        }

        List<?> extraDrops = root.getList("extra_drops", new ArrayList<>());
        for (Object raw : extraDrops) {
            if (raw instanceof ItemStack stack && stack.getType() != org.bukkit.Material.AIR) {
                ItemStack copy = stack.clone();
                copy.setAmount(1);
                plugin.bundleDropExtraDrops().add(copy);
            }
        }

        saveBundleDropSettings();
    }

    void saveBundleDropSettings() {
        plugin.getConfig().set("bundle_drop.enabled", plugin.isBundleDropEnabled());
        plugin.getConfig().set("bundle_drop.chance", plugin.getBundleDropChancePercent());

        plugin.getConfig().set("bundle_drop.mob_chance", null);
        for (Map.Entry<EntityType, Double> entry : plugin.bundleDropMobChances().entrySet()) {
            if (entry.getValue() != null && entry.getValue() > 0.0D) {
                plugin.getConfig().set("bundle_drop.mob_chance." + entry.getKey().name(), clampChance(entry.getValue()));
            }
        }

        List<Map<String, Object>> serialized = new ArrayList<>();
        for (BundleDropReward reward : plugin.bundleDropRewards()) {
            serialized.add(Map.of(
                    "type", reward.type().name(),
                    "key", reward.key(),
                    "level", reward.level(),
                    "amount", reward.amount()
            ));
        }
        plugin.getConfig().set("bundle_drop.rewards", serialized);
        plugin.getConfig().set("bundle_drop.extra_drops", new ArrayList<>(plugin.bundleDropExtraDrops()));
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

    private int parseInt(Object raw, int fallback) {
        if (raw == null) {
            return fallback;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private double parseDouble(Object raw, double fallback) {
        if (raw == null) {
            return fallback;
        }
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(raw));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}

