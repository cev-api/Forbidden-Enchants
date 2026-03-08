package dev.cevapi.forbiddenenchants;

import dev.cevapi.forbiddenenchants.enchants.EnchantList;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Evoker;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

final class LootDeathService {
    private final Supplier<EnchantStateService> enchantStateServiceSupplier;
    private final TemporalSicknessService temporalSicknessService;

    LootDeathService(@NotNull Supplier<EnchantStateService> enchantStateServiceSupplier,
                     @NotNull TemporalSicknessService temporalSicknessService) {
        this.enchantStateServiceSupplier = enchantStateServiceSupplier;
        this.temporalSicknessService = temporalSicknessService;
    }

    void onHatedOneLoot(@NotNull EntityDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead instanceof Evoker evoker) {
            EnchantList.INSTANCE.evokersRevenge().stripTotemDropIfSpawned(evoker, event.getDrops());
        }
        Player killer = dead.getKiller();
        if (killer == null) {
            return;
        }
        temporalSicknessService.onKill(killer);

        if (dead.getType() == EntityType.EVOKER
                && enchantStateServiceSupplier.get().getEnchantLevel(killer.getInventory().getChestplate(), EnchantType.EVOKERS_REVENGE) > 0) {
            EnchantList.INSTANCE.evokersRevenge().onEvokerKilledByOwner(killer.getUniqueId());
        }
        if (dead.getType() == EntityType.ILLUSIONER
                && enchantStateServiceSupplier.get().getEnchantLevel(killer.getInventory().getChestplate(), EnchantType.ILLUSIONERS_REVENGE) > 0) {
            EnchantList.INSTANCE.illusionersRevenge().onIllusionerKilledByOwner(killer.getUniqueId());
        }

        ItemStack helmet = killer.getInventory().getHelmet();
        int hatedLevel = enchantStateServiceSupplier.get().getEnchantLevel(helmet, EnchantType.THE_HATED_ONE);
        if (hatedLevel <= 0) {
            return;
        }

        List<ItemStack> drops = event.getDrops();
        int originalDropCount = drops.size();
        for (int i = 0; i < originalDropCount; i++) {
            ItemStack drop = drops.get(i);
            int bonus = hatedLevel >= 2
                    ? ThreadLocalRandom.current().nextInt(2, 6)
                    : ThreadLocalRandom.current().nextInt(1, 3);
            drop.setAmount(Math.min(drop.getMaxStackSize(), drop.getAmount() + bonus));
            double cloneChance = hatedLevel >= 2 ? 0.35D : 0.175D;
            if (ThreadLocalRandom.current().nextDouble() < cloneChance) {
                drops.add(drop.clone());
            }
        }

        if (dead instanceof Mob mob && mob.getEquipment() != null) {
            double equipmentChance = hatedLevel >= 2 ? 1.0D : 0.5D;
            maybeAddEquipmentDrop(drops, mob.getEquipment().getItemInMainHand(), equipmentChance);
            maybeAddEquipmentDrop(drops, mob.getEquipment().getItemInOffHand(), equipmentChance);
            maybeAddEquipmentDrop(drops, mob.getEquipment().getHelmet(), equipmentChance);
            maybeAddEquipmentDrop(drops, mob.getEquipment().getChestplate(), equipmentChance);
            maybeAddEquipmentDrop(drops, mob.getEquipment().getLeggings(), equipmentChance);
            maybeAddEquipmentDrop(drops, mob.getEquipment().getBoots(), equipmentChance);
        }
    }

    private void maybeAddEquipmentDrop(@NotNull List<ItemStack> drops, @Nullable ItemStack stack, double chance) {
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        if (ThreadLocalRandom.current().nextDouble() > chance) {
            return;
        }
        drops.add(stack.clone());
    }
}

