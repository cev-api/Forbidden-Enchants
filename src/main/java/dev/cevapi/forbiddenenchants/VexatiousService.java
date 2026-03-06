package dev.cevapi.forbiddenenchants;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.NamespacedKey;
import dev.cevapi.forbiddenenchants.enchants.EnchantList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

final class VexatiousService {
    private final Supplier<NamespacedKey> vexatiousOwnerKeySupplier;
    private final Supplier<ItemCombatService> itemCombatServiceSupplier;
    private final Map<UUID, List<UUID>> vexesByOwner;
    private final Map<UUID, UUID> assistTargets;
    private final Map<UUID, Long> assistTargetExpire;

    VexatiousService(@NotNull Supplier<NamespacedKey> vexatiousOwnerKeySupplier,
                     @NotNull Supplier<ItemCombatService> itemCombatServiceSupplier,
                     @NotNull Map<UUID, List<UUID>> vexesByOwner,
                     @NotNull Map<UUID, UUID> assistTargets,
                     @NotNull Map<UUID, Long> assistTargetExpire) {
        this.vexatiousOwnerKeySupplier = vexatiousOwnerKeySupplier;
        this.itemCombatServiceSupplier = itemCombatServiceSupplier;
        this.vexesByOwner = vexesByOwner;
        this.assistTargets = assistTargets;
        this.assistTargetExpire = assistTargetExpire;
    }

    void maintain(@NotNull Player player, @NotNull ItemStack helmet, int level, long tickCounter) {
        EnchantList.INSTANCE.vexatious().maintain(
                vexatiousOwnerKeySupplier.get(),
                (owner, currentHelmet, fraction) -> itemCombatServiceSupplier.get().damageArmorByPercent(owner, EquipmentSlot.HEAD, currentHelmet, fraction),
                vexesByOwner,
                assistTargets,
                assistTargetExpire,
                player,
                helmet,
                level,
                tickCounter
        );
    }

    void pushAssistTarget(@NotNull UUID ownerId, @NotNull LivingEntity target, long tickCounter) {
        EnchantList.INSTANCE.vexatious().pushAssistTarget(vexesByOwner, assistTargets, assistTargetExpire, ownerId, target, tickCounter);
    }

    void clear(@NotNull UUID owner) {
        EnchantList.INSTANCE.vexatious().clear(vexesByOwner, assistTargets, assistTargetExpire, owner);
    }

    void cleanupAll() {
        EnchantList.INSTANCE.vexatious().cleanupAll(vexesByOwner);
    }
}

