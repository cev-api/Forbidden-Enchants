package dev.cevapi.forbiddenenchants;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

final class MiasmaVisualService {
    private final MasqueradeService masqueradeService;
    private final Map<UUID, MiasmaVisualState> states;

    MiasmaVisualService(@NotNull MasqueradeService masqueradeService,
                        @NotNull Map<UUID, MiasmaVisualState> states) {
        this.masqueradeService = masqueradeService;
        this.states = states;
    }

    boolean hasForm(@NotNull Player player) {
        return states.containsKey(player.getUniqueId());
    }

    @Nullable MiasmaVisualState getState(@NotNull UUID playerId) {
        return states.get(playerId);
    }

    void ensure(@NotNull Player player) {
        UUID id = player.getUniqueId();
        if (!states.containsKey(id)) {
            PlayerInventory inventory = player.getInventory();
            MiasmaVisualState state = new MiasmaVisualState(
                    cloneStack(inventory.getHelmet()),
                    cloneStack(inventory.getChestplate()),
                    cloneStack(inventory.getLeggings()),
                    cloneStack(inventory.getBoots()),
                    cloneStack(inventory.getItemInMainHand()),
                    cloneStack(inventory.getItemInOffHand()),
                    inventory.getHeldItemSlot()
            );
            states.put(id, state);
        }

        hideEquipment(player);
    }

    void clear(@NotNull Player player) {
        MiasmaVisualState state = states.remove(player.getUniqueId());
        if (state == null) {
            return;
        }

        if (masqueradeService.isMasquerading(player)) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        inventory.setHelmet(cloneOrAir(state.helmet()));
        inventory.setChestplate(cloneOrAir(state.chestplate()));
        inventory.setLeggings(cloneOrAir(state.leggings()));
        inventory.setBoots(cloneOrAir(state.boots()));
        inventory.setHeldItemSlot(Math.max(0, Math.min(8, state.heldSlot())));
        inventory.setItemInMainHand(cloneOrAir(state.mainHand()));
        inventory.setItemInOffHand(cloneOrAir(state.offHand()));
    }

    void clearAllOnline() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            clear(player);
        }
        states.clear();
    }

    private void hideEquipment(@NotNull Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.setHelmet(new ItemStack(Material.AIR));
        inventory.setChestplate(new ItemStack(Material.AIR));
        inventory.setLeggings(new ItemStack(Material.AIR));
        inventory.setBoots(new ItemStack(Material.AIR));
        inventory.setItemInMainHand(new ItemStack(Material.AIR));
        inventory.setItemInOffHand(new ItemStack(Material.AIR));
    }

    private @Nullable ItemStack cloneStack(@Nullable ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return null;
        }
        return stack.clone();
    }

    private @NotNull ItemStack cloneOrAir(@Nullable ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return new ItemStack(Material.AIR);
        }
        return stack.clone();
    }
}

