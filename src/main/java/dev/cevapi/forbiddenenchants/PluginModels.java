package dev.cevapi.forbiddenenchants;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.generator.structure.Structure;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

final class FeMenuHolder implements InventoryHolder {
    private final int page;
    private Inventory inventory;

    FeMenuHolder(int page) {
        this.page = page;
    }

    int page() {
        return page;
    }

    void attach(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Objects.requireNonNull(inventory, "menu inventory");
    }
}

final class EnchantToggleMenuHolder implements InventoryHolder {
    private final int page;
    private Inventory inventory;

    EnchantToggleMenuHolder(int page) {
        this.page = page;
    }

    int page() {
        return page;
    }

    void attach(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Objects.requireNonNull(inventory, "toggle menu inventory");
    }
}

enum InjectorMenuMode {
    CONFIGURED,
    ALL
}

enum VaultEntryType {
    NORMAL,
    OMINOUS
}

final class InjectorEntry {
    final @Nullable Structure structure;
    final @Nullable VaultEntryType vaultType;

    private InjectorEntry(@Nullable Structure structure, @Nullable VaultEntryType vaultType) {
        this.structure = structure;
        this.vaultType = vaultType;
    }

    static @NotNull InjectorEntry structure(@NotNull Structure structure) {
        return new InjectorEntry(structure, null);
    }

    static @NotNull InjectorEntry vault(@NotNull VaultEntryType type) {
        return new InjectorEntry(null, type);
    }
}

final class InjectorMenuHolder implements InventoryHolder {
    private final InjectorMenuMode mode;
    private final int page;
    private Inventory inventory;

    InjectorMenuHolder(@NotNull InjectorMenuMode mode, int page) {
        this.mode = mode;
        this.page = page;
    }

    @NotNull InjectorMenuMode mode() {
        return mode;
    }

    int page() {
        return page;
    }

    void attach(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Objects.requireNonNull(inventory, "injector menu inventory");
    }
}

final class InjectorBookRarityMenuHolder implements InventoryHolder {
    private final int page;
    private Inventory inventory;

    InjectorBookRarityMenuHolder(int page) {
        this.page = page;
    }

    int page() {
        return page;
    }

    void attach(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Objects.requireNonNull(inventory, "injector rarity menu inventory");
    }
}

final class LibrarianTradeMenuHolder implements InventoryHolder {
    private final LibrarianTradeMenuMode mode;
    private final int page;
    private Inventory inventory;

    LibrarianTradeMenuHolder(@NotNull LibrarianTradeMenuMode mode, int page) {
        this.mode = mode;
        this.page = page;
    }

    @NotNull LibrarianTradeMenuMode mode() {
        return mode;
    }

    int page() {
        return page;
    }

    void attach(@NotNull Inventory inventory) {
        this.inventory = inventory;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return Objects.requireNonNull(inventory, "librarian menu inventory");
    }
}

enum LibrarianTradeMenuMode {
    CONFIGURED,
    ALL
}

record LibrarianTradeEntry(@NotNull EnchantType type, int level, double chancePercent, int emeraldCost, int bookCost) {
}

record BookSpec(EnchantType type, int level) {
}

record MiasmaVisualState(@Nullable ItemStack helmet,
                         @Nullable ItemStack chestplate,
                         @Nullable ItemStack leggings,
                         @Nullable ItemStack boots,
                         @Nullable ItemStack mainHand,
                         @Nullable ItemStack offHand,
                         int heldSlot) {
}

record TemporalArmorPiece(EquipmentSlot slot, int level) {
}

record WitherState(UUID sourcePlayerId, long nextDamageTick) {
}

record EnchantmentAllyState(UUID ownerId,
                            long expireTick,
                            @Nullable UUID forcedTargetPlayerId,
                            boolean spawnedByEnchant,
                            long createdTick) {
}

record FearState(UUID sourcePlayerId, long expireTick) {
}

record MarkedState(UUID ownerId, long expireTick) {
}

record CharmedPetState(UUID ownerId, boolean sitting) {
}

