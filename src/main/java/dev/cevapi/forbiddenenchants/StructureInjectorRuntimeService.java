package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.block.TileState;
import org.bukkit.block.Vault;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.generator.structure.GeneratedStructure;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class StructureInjectorRuntimeService {
    private final ForbiddenEnchantsPlugin plugin;

    StructureInjectorRuntimeService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    void onStructureInjectorOpen(@NotNull PlayerInteractEvent event) {
        if (!plugin.isStructureInjectorEnabled() || plugin.structureInjectChances().isEmpty()) {
            return;
        }

        try {
            if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
                return;
            }

            Block block = event.getClickedBlock();
            if (block == null) {
                return;
            }
            if (!(block.getState() instanceof Container) || !(block.getState() instanceof TileState tileState)) {
                return;
            }

            PersistentDataContainer data = tileState.getPersistentDataContainer();
            if (data.has(plugin.structureInjectAppliedKey(), PersistentDataType.BYTE)) {
                return;
            }

            double bestChance = 0.0D;
            Structure matchedStructure = null;
            for (Map.Entry<NamespacedKey, Double> entry : plugin.structureInjectChances().entrySet()) {
                Structure structure = Registry.STRUCTURE.get(entry.getKey());
                if (structure == null || !isBlockInsideStructure(block, structure)) {
                    continue;
                }
                if (entry.getValue() > bestChance) {
                    bestChance = entry.getValue();
                    matchedStructure = structure;
                }
            }
            if (matchedStructure == null) {
                return;
            }

            data.set(plugin.structureInjectAppliedKey(), PersistentDataType.BYTE, (byte) 1);
            tileState.update(true, false);

            if (ThreadLocalRandom.current().nextDouble(100.0D) > bestChance) {
                return;
            }

            Player player = event.getPlayer();
            InjectorLootMode lootMode = plugin.structureInjectLootMode(matchedStructure.getKey());
            InjectorMysteryState mysteryState = plugin.structureInjectMysteryState(matchedStructure.getKey());
            ItemStack injected = createRandomInjectedLoot(lootMode, mysteryState);
            if (injected == null) {
                return;
            }

            Structure finalMatchedStructure = matchedStructure;
            Block finalBlock = block;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!(finalBlock.getState() instanceof Container postContainer)) {
                    return;
                }

                Inventory destination;
                Inventory top = player.getOpenInventory().getTopInventory();
                if (isViewingBlockInventory(top, finalBlock)) {
                    destination = top;
                } else {
                    destination = postContainer.getInventory();
                }

                ItemStack toInsert = injected.clone();
                destination.addItem(toInsert).values().forEach(overflow -> finalBlock.getWorld().dropItemNaturally(finalBlock.getLocation().add(0.5D, 0.8D, 0.5D), overflow));

                if (plugin.isStructureInjectNotifyOnAdd()) {
                    player.sendActionBar(Component.text(
                            "Structure injector: added " + plugin.describeItem(injected) + " (" + finalMatchedStructure.getKey().getKey() + ", " + lootMode.id() + ", " + mysteryState.id() + ").",
                            NamedTextColor.LIGHT_PURPLE
                    ));
                }
            }, 1L);
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
                "You discovered a new forbidden enchant: " + plugin.describeItem(injected) + ".",
                NamedTextColor.LIGHT_PURPLE
        ));
    }

    private @Nullable ItemStack createRandomForbiddenBook(@Nullable Predicate<EnchantType> typeFilter) {
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

        EnchantType type = types.get(ThreadLocalRandom.current().nextInt(types.size()));
        int level = ThreadLocalRandom.current().nextInt(1, type.maxLevel + 1);
        return plugin.createBook(type, level);
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
                EnchantType hidden = detectStoredEnchantType(mystery);
                if (hidden == null || plugin.isRetiredEnchant(hidden) || !plugin.isEnchantSpawnEnabled(hidden)) {
                    continue;
                }
                if (typeFilter != null && !typeFilter.test(hidden)) {
                    continue;
                }
                return mystery;
            }
        }
        return null;
    }

    private @Nullable ItemStack createRandomEnchantedItem(@Nullable Predicate<EnchantType> typeFilter) {
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
                EnchantType hidden = detectStoredEnchantType(mystery);
                if (hidden == null || plugin.isRetiredEnchant(hidden) || !plugin.isEnchantSpawnEnabled(hidden)) {
                    continue;
                }
                if (typeFilter != null && !typeFilter.test(hidden)) {
                    continue;
                }
                return mystery;
            }
        }
        return null;
    }

    private @Nullable EnchantType detectStoredEnchantType(@NotNull ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return null;
        }
        for (EnchantType type : EnchantType.values()) {
            if (plugin.getStoredEnchantLevel(meta, type) > 0) {
                return type;
            }
        }
        return null;
    }

    private boolean isBlockInsideStructure(@NotNull Block block, @NotNull Structure structure) {
        World world = block.getWorld();
        int chunkX = block.getChunk().getX();
        int chunkZ = block.getChunk().getZ();
        for (GeneratedStructure generated : world.getStructures(chunkX, chunkZ, structure)) {
            if (generated.getBoundingBox().contains(
                    block.getX() + 0.5D,
                    block.getY() + 0.5D,
                    block.getZ() + 0.5D
            )) {
                return true;
            }
        }
        return false;
    }

    private boolean isViewingBlockInventory(@Nullable Inventory inventory, @NotNull Block block) {
        if (inventory == null) {
            return false;
        }
        InventoryHolder holder = inventory.getHolder();
        if (!(holder instanceof BlockInventoryHolder blockHolder)) {
            return false;
        }
        Block holderBlock = blockHolder.getBlock();
        return holderBlock.getWorld().equals(block.getWorld())
                && holderBlock.getX() == block.getX()
                && holderBlock.getY() == block.getY()
                && holderBlock.getZ() == block.getZ();
    }
}

