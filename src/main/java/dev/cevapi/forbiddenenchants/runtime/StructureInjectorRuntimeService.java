package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Vault;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class StructureInjectorRuntimeService {
    private final ForbiddenEnchantsPlugin plugin;

    StructureInjectorRuntimeService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    void onLootGenerate(@NotNull LootGenerateEvent event) {
        if (!plugin.isStructureInjectorEnabled() || plugin.structureInjectChances().isEmpty()) {
            return;
        }
        try {
            if (event.getLootTable() == null || event.getLootTable().getKey() == null) {
                return;
            }
            String lootTableId = event.getLootTable().getKey().toString().toLowerCase(Locale.ROOT);
            Set<String> aliases = structureAliasesForLootTable(lootTableId);
            if (aliases.isEmpty()) {
                return;
            }

            NamespacedKey matchedKey = null;
            double bestChance = 0.0D;
            for (Map.Entry<NamespacedKey, Double> entry : plugin.structureInjectChances().entrySet()) {
                if (!aliases.contains(entry.getKey().toString().toLowerCase(Locale.ROOT))) {
                    continue;
                }
                if (entry.getValue() > bestChance) {
                    bestChance = entry.getValue();
                    matchedKey = entry.getKey();
                }
            }
            if (matchedKey == null) {
                return;
            }
            if (ThreadLocalRandom.current().nextDouble(100.0D) > bestChance) {
                return;
            }

            InjectorLootMode lootMode = plugin.structureInjectLootMode(matchedKey);
            InjectorMysteryState mysteryState = plugin.structureInjectMysteryState(matchedKey);
            ItemStack injected = createRandomInjectedLoot(lootMode, mysteryState);
            if (injected == null) {
                return;
            }

            List<ItemStack> loot = new ArrayList<>(event.getLoot());
            loot.add(injected.clone());
            event.setLoot(loot);

            if (plugin.isStructureInjectNotifyOnAdd() && event.getEntity() instanceof Player player) {
                player.sendActionBar(Component.text(
                        plugin.message(
                                "injector.added_actionbar",
                                "Structure injector: added {item} ({structure}, {loot_mode}, {mystery_state}).",
                                Map.of(
                                        "item", plugin.describeItem(injected),
                                        "structure", matchedKey.getKey(),
                                        "loot_mode", lootMode.id(),
                                        "mystery_state", mysteryState.id()
                                )
                        ),
                        NamedTextColor.LIGHT_PURPLE
                ));
            }
        } catch (Throwable t) {
            plugin.disableStructureInjectorDueToRuntimeError(t);
        }
    }

    void onTrialVaultInjector(@NotNull PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || block.getType() != Material.VAULT) {
            return;
        }
        if (!(block.getState() instanceof Vault)) {
            return;
        }

        ItemStack used = event.getItem();
        if (used == null || used.getType() == Material.AIR) {
            return;
        }

        boolean ominous = false;
        if (block.getBlockData() instanceof org.bukkit.block.data.type.Vault vaultData) {
            ominous = vaultData.isOminous();
        }

        Material expectedKey = ominous ? Material.OMINOUS_TRIAL_KEY : Material.TRIAL_KEY;
        if (used.getType() != expectedKey) {
            return;
        }

        double chance = ominous ? plugin.getTrialVaultOminousChance() : plugin.getTrialVaultNormalChance();
        if (chance <= 0.0D) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble(100.0D) > chance) {
            return;
        }

        Player player = event.getPlayer();
        InjectorLootMode mode = ominous ? plugin.getTrialVaultOminousLootMode() : plugin.getTrialVaultNormalLootMode();
        InjectorMysteryState mysteryState = ominous ? plugin.getTrialVaultOminousMysteryState() : plugin.getTrialVaultNormalMysteryState();
        ItemStack injected = createRandomInjectedLoot(mode, mysteryState);
        if (injected == null) {
            return;
        }
        plugin.giveOrDrop(player, injected);
        player.sendActionBar(Component.text(
                plugin.message(
                        "injector.vault_discovered",
                        "You discovered a new forbidden enchant: {item}.",
                        Map.of("item", plugin.describeItem(injected))
                ),
                NamedTextColor.LIGHT_PURPLE
        ));
    }

    private @Nullable ItemStack createRandomForbiddenBook(@Nullable Predicate<EnchantType> typeFilter) {
        List<WeightedBookEntry> books = weightedBookEntries(typeFilter);
        if (books.isEmpty()) {
            return null;
        }

        WeightedBookEntry selected = pickWeightedBook(books);
        if (selected == null) {
            return null;
        }
        return plugin.createBook(selected.type(), selected.level());
    }

    private @Nullable ItemStack createRandomInjectedLoot(@NotNull InjectorLootMode mode, @NotNull InjectorMysteryState mysteryState) {
        Predicate<EnchantType> curseFilter = switch (mode.curseState()) {
            case ALL -> null;
            case CURSED -> EnchantType::appliesBindingCurse;
            case UNCURSED -> type -> !type.appliesBindingCurse();
        };
        boolean allowMystery = mysteryState != InjectorMysteryState.NON_MYSTERY_ONLY;
        boolean allowNonMystery = mysteryState != InjectorMysteryState.MYSTERY_ONLY;

        List<Supplier<ItemStack>> generators = new ArrayList<>(4);
        if (mode.type() == InjectorLootMode.LootType.BOOKS || mode.type() == InjectorLootMode.LootType.ALL) {
            if (allowNonMystery) {
                generators.add(() -> createRandomForbiddenBook(curseFilter));
            }
            if (allowMystery) {
                generators.add(() -> createRandomMysteryBook(curseFilter));
            }
        }
        if (mode.type() == InjectorLootMode.LootType.ITEMS || mode.type() == InjectorLootMode.LootType.ALL) {
            if (allowNonMystery) {
                generators.add(() -> createRandomEnchantedItem(curseFilter));
            }
            if (allowMystery) {
                generators.add(() -> createRandomMysteryItem(curseFilter));
            }
        }
        return createRandomFromGenerators(generators);
    }

    private @Nullable ItemStack createRandomFromGenerators(@NotNull List<Supplier<ItemStack>> generators) {
        if (generators.isEmpty()) {
            return null;
        }
        for (int pass = 0; pass < 3; pass++) {
            List<Supplier<ItemStack>> shuffled = new ArrayList<>(generators);
            Collections.shuffle(shuffled);
            for (Supplier<ItemStack> generator : shuffled) {
                ItemStack generated = generator.get();
                if (generated != null) {
                    return generated;
                }
            }
        }
        return null;
    }

    private @Nullable ItemStack createRandomMysteryBook(@Nullable Predicate<EnchantType> typeFilter) {
        if (plugin.isInjectorRarityApplyToItems()) {
            return createWeightedMysteryBook(typeFilter);
        }

        List<ArmorSlot> slots = new ArrayList<>();
        for (EnchantType type : EnchantType.values()) {
            if (!plugin.isRetiredEnchant(type)
                    && plugin.isEnchantSpawnEnabled(type)
                    && (typeFilter == null || typeFilter.test(type))
                    && !slots.contains(type.slot)) {
                slots.add(type.slot);
            }
        }
        if (slots.isEmpty()) {
            return null;
        }
        Collections.shuffle(slots);
        for (ArmorSlot slot : slots) {
            for (int attempts = 0; attempts < 6; attempts++) {
                ItemStack mystery = plugin.createMysteryBook(slot);
                BookSpec hidden = detectStoredBookSpec(mystery);
                if (hidden == null || plugin.isRetiredEnchant(hidden.type()) || !plugin.isEnchantSpawnEnabled(hidden.type())) {
                    continue;
                }
                if (typeFilter != null && !typeFilter.test(hidden.type())) {
                    continue;
                }
                return mystery;
            }
        }
        return null;
    }

    private @Nullable ItemStack createRandomEnchantedItem(@Nullable Predicate<EnchantType> typeFilter) {
        if (plugin.isInjectorRarityApplyToItems()) {
            return createWeightedEnchantedItem(typeFilter);
        }

        List<EnchantType> types = new ArrayList<>();
        for (EnchantType type : EnchantType.values()) {
            if (!plugin.isRetiredEnchant(type) && plugin.isEnchantSpawnEnabled(type)
                    && (typeFilter == null || typeFilter.test(type))) {
                types.add(type);
            }
        }
        if (types.isEmpty()) {
            return null;
        }

        Collections.shuffle(types);
        for (EnchantType type : types) {
            List<String> materialNames = new ArrayList<>(EnchantMaterialCatalog.materialSuggestions(type));
            Collections.shuffle(materialNames);
            for (String materialName : materialNames) {
                Material material = SlotParsingUtil.parseMaterial(materialName);
                if (material == null || material == Material.AIR) {
                    continue;
                }
                int level = ThreadLocalRandom.current().nextInt(1, type.maxLevel + 1);
                ItemStack generated = plugin.createEnchantedItem(type, level, material);
                if (generated != null) {
                    return generated;
                }
            }
        }
        return null;
    }

    private @Nullable ItemStack createRandomMysteryItem(@Nullable Predicate<EnchantType> typeFilter) {
        if (plugin.isInjectorRarityApplyToItems()) {
            return createWeightedMysteryItem(typeFilter);
        }

        List<String> materials = new ArrayList<>(EnchantMaterialCatalog.allEnchantableMaterialSuggestions());
        if (materials.isEmpty()) {
            return null;
        }
        Collections.shuffle(materials);
        for (String materialName : materials) {
            Material material = SlotParsingUtil.parseMaterial(materialName);
            if (material == null || material == Material.AIR) {
                continue;
            }
            for (int attempts = 0; attempts < 4; attempts++) {
                ItemStack mystery = plugin.createMysteryItem(material);
                if (mystery == null) {
                    continue;
                }
                BookSpec hidden = detectStoredBookSpec(mystery);
                if (hidden == null || plugin.isRetiredEnchant(hidden.type()) || !plugin.isEnchantSpawnEnabled(hidden.type())) {
                    continue;
                }
                if (typeFilter != null && !typeFilter.test(hidden.type())) {
                    continue;
                }
                return mystery;
            }
        }
        return null;
    }

    private @Nullable ItemStack createWeightedEnchantedItem(@Nullable Predicate<EnchantType> typeFilter) {
        List<WeightedBookEntry> books = weightedBookEntries(typeFilter);
        if (books.isEmpty()) {
            return null;
        }

        for (int attempts = 0; attempts < 24; attempts++) {
            WeightedBookEntry selected = pickWeightedBook(books);
            if (selected == null) {
                return null;
            }
            List<String> materialNames = new ArrayList<>(EnchantMaterialCatalog.materialSuggestions(selected.type()));
            Collections.shuffle(materialNames);
            for (String materialName : materialNames) {
                Material material = SlotParsingUtil.parseMaterial(materialName);
                if (material == null || material == Material.AIR) {
                    continue;
                }
                ItemStack generated = plugin.createEnchantedItem(selected.type(), selected.level(), material);
                if (generated != null) {
                    return generated;
                }
            }
        }
        return null;
    }

    private @Nullable ItemStack createWeightedMysteryBook(@Nullable Predicate<EnchantType> typeFilter) {
        List<WeightedBookEntry> books = weightedBookEntries(typeFilter);
        if (books.isEmpty()) {
            return null;
        }

        for (int attempts = 0; attempts < 32; attempts++) {
            WeightedBookEntry selected = pickWeightedBook(books);
            if (selected == null) {
                return null;
            }
            ItemStack mystery = plugin.createMysteryBook(selected.type().slot);
            BookSpec hidden = detectStoredBookSpec(mystery);
            if (hidden == null) {
                continue;
            }
            if (hidden.type() == selected.type() && hidden.level() == selected.level()) {
                return mystery;
            }
        }
        return null;
    }

    private @Nullable ItemStack createWeightedMysteryItem(@Nullable Predicate<EnchantType> typeFilter) {
        List<WeightedBookEntry> books = weightedBookEntries(typeFilter);
        if (books.isEmpty()) {
            return null;
        }

        for (int attempts = 0; attempts < 32; attempts++) {
            WeightedBookEntry selected = pickWeightedBook(books);
            if (selected == null) {
                return null;
            }
            List<String> materialNames = new ArrayList<>(EnchantMaterialCatalog.materialSuggestions(selected.type()));
            Collections.shuffle(materialNames);
            for (String materialName : materialNames) {
                Material material = SlotParsingUtil.parseMaterial(materialName);
                if (material == null || material == Material.AIR) {
                    continue;
                }
                ItemStack mystery = plugin.createMysteryItem(material);
                if (mystery == null) {
                    continue;
                }
                BookSpec hidden = detectStoredBookSpec(mystery);
                if (hidden == null) {
                    continue;
                }
                if (hidden.type() == selected.type() && hidden.level() == selected.level()) {
                    return mystery;
                }
            }
        }
        return null;
    }

    private @NotNull List<WeightedBookEntry> weightedBookEntries(@Nullable Predicate<EnchantType> typeFilter) {
        List<WeightedBookEntry> books = new ArrayList<>();
        for (EnchantType type : EnchantType.values()) {
            if (!plugin.isRetiredEnchant(type) && plugin.isEnchantSpawnEnabled(type)
                    && (typeFilter == null || typeFilter.test(type))) {
                for (int level = 1; level <= type.maxLevel; level++) {
                    double weight = plugin.injectorBookRarityWeight(type, level);
                    if (weight > 0.0D) {
                        books.add(new WeightedBookEntry(type, level, weight));
                    }
                }
            }
        }
        return books;
    }

    private @Nullable BookSpec detectStoredBookSpec(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        for (EnchantType type : EnchantType.values()) {
            int level = plugin.getStoredEnchantLevel(meta, type);
            if (level > 0) {
                return new BookSpec(type, level);
            }
        }
        return null;
    }

    private @NotNull Set<String> structureAliasesForLootTable(@NotNull String lootTableId) {
        Set<String> out = new LinkedHashSet<>();
        int colon = lootTableId.indexOf(':');
        if (colon <= 0 || colon + 1 >= lootTableId.length()) {
            return out;
        }
        String namespace = lootTableId.substring(0, colon).toLowerCase(Locale.ROOT);
        String path = lootTableId.substring(colon + 1).toLowerCase(Locale.ROOT);
        if (!isLikelyStructureContainerTable(path)) {
            return out;
        }
        String marker = "chests/";
        int idx = path.indexOf(marker);
        String tablePath = (idx >= 0 && idx + marker.length() < path.length())
                ? path.substring(idx + marker.length())
                : path;

        addStructureAliasCandidates(out, namespace, tablePath);

        if (tablePath.startsWith("trial_chambers/") || tablePath.startsWith("trial_chamber/")) {
            if (tablePath.contains("ominous")) {
                out.add(namespace + ":trial_chambers_ominous_vault");
            } else if (tablePath.contains("vault") || tablePath.contains("reward")) {
                out.add(namespace + ":trial_chambers_vault");
            } else {
                out.add(namespace + ":trial_chambers");
            }
        }
        return out;
    }

    private void addStructureAliasCandidates(@NotNull Set<String> out,
                                             @NotNull String namespace,
                                             @NotNull String tablePath) {
        String[] parts = tablePath.split("/");
        if (parts.length > 0) {
            String first = parts[0];
            if (!first.isBlank()) {
                out.add(namespace + ":" + first);
            }
            String last = parts[parts.length - 1];
            if (!last.isBlank()) {
                out.add(namespace + ":" + last);
            }
        }

        String leaf = parts.length == 0 ? tablePath : parts[parts.length - 1];
        if (leaf.isBlank()) {
            return;
        }

        String containerKind = extractContainerKind(leaf);
        if (containerKind != null) {
            out.add(namespace + ":" + containerKind);
        }

        String trimmed = leaf;
        String[] suffixes = {"_chest", "_barrel", "_crate", "_supply", "_loot", "_cache"};
        boolean hadContainerSuffix = false;
        for (String suffix : suffixes) {
            if (trimmed.endsWith(suffix) && trimmed.length() > suffix.length()) {
                trimmed = trimmed.substring(0, trimmed.length() - suffix.length());
                hadContainerSuffix = true;
                break;
            }
        }
        if (!trimmed.isBlank() && !hadContainerSuffix) {
            out.add(namespace + ":" + trimmed);
            if (trimmed.endsWith("s") && trimmed.length() > 1) {
                out.add(namespace + ":" + trimmed.substring(0, trimmed.length() - 1));
            }
        }

        if (!hadContainerSuffix) {
            String[] tokens = trimmed.split("_");
            if (tokens.length >= 2) {
                out.add(namespace + ":" + tokens[0] + "_" + tokens[1]);
            }
            if (tokens.length >= 3) {
                out.add(namespace + ":" + tokens[0] + "_" + tokens[1] + "_" + tokens[2]);
            }
        }
    }

    private @Nullable String extractContainerKind(@NotNull String leaf) {
        String lower = leaf.toLowerCase(Locale.ROOT);
        String[] kinds = {"chest", "barrel", "crate", "supply", "loot", "cache", "treasure", "reward", "vault"};
        for (String kind : kinds) {
            if (lower.equals(kind)
                    || lower.equals(kind + "s")
                    || lower.endsWith("_" + kind)
                    || lower.endsWith("_" + kind + "s")) {
                return kind;
            }
        }
        return null;
    }

    private boolean isLikelyStructureContainerTable(@NotNull String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        String[] denyPrefixes = {
                "blocks/", "block/",
                "entities/", "entity/",
                "items/", "item/",
                "gameplay/", "gifts/", "gift/",
                "sheep/", "archeology/", "archaeology/"
        };
        for (String prefix : denyPrefixes) {
            if (lower.startsWith(prefix)) {
                return false;
            }
        }
        return lower.contains("chest")
                || lower.contains("barrel")
                || lower.contains("crate")
                || lower.contains("cache")
                || lower.contains("supply")
                || lower.contains("treasure")
                || lower.contains("vault")
                || lower.contains("reward")
                || lower.startsWith("chests/");
    }

    private @Nullable WeightedBookEntry pickWeightedBook(@NotNull List<WeightedBookEntry> books) {
        double totalWeight = 0.0D;
        for (WeightedBookEntry book : books) {
            totalWeight += book.weight();
        }
        if (totalWeight <= 0.0D) {
            return null;
        }

        double roll = ThreadLocalRandom.current().nextDouble(totalWeight);
        double cumulative = 0.0D;
        for (WeightedBookEntry book : books) {
            cumulative += book.weight();
            if (roll <= cumulative) {
                return book;
            }
        }
        return books.get(books.size() - 1);
    }

    private record WeightedBookEntry(@NotNull EnchantType type, int level, double weight) {
    }
}

