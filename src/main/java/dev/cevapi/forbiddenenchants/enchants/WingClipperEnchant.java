package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class WingClipperEnchant extends BaseForbiddenEnchant {
    public WingClipperEnchant() {
        super("wing_clipper",
                "wing_clipper_level",
                "Wing Clipper",
                ArmorSlot.SWORD,
                1,
                NamedTextColor.WHITE,
                List.of("wingclipper", "wing_clipper", "clipper"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "On player hit, blocks Elytra equip for 10s and cracks worn Elytra.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }

    public boolean shouldApplyOnSwordHit(int level, boolean sword) {
        return sword && isActive(level);
    }

    public long blockDurationTicks() {
        return 200L;
    }

    public long blockedUntilTick(long tickCounter) {
        return tickCounter + blockDurationTicks();
    }

    public boolean shouldRipElytra(boolean wearingElytra) {
        return wearingElytra;
    }

    public double maxRemainingDurabilityFraction() {
        return 0.05D;
    }

    public void onPlayerHit(int level,
                            boolean sword,
                            long tickCounter,
                            @NotNull BlockSetter blockSetter,
                            @NotNull Runnable ripElytraAction) {
        if (!shouldApplyOnSwordHit(level, sword)) {
            return;
        }
        blockSetter.setBlockedUntil(blockedUntilTick(tickCounter));
        ripElytraAction.run();
    }

    @FunctionalInterface
    public interface BlockSetter {
        void setBlockedUntil(long blockedUntilTick);
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (!(event.getDamager() instanceof Player attacker) || !(event.getEntity() instanceof Player targetPlayer)) {
            return;
        }

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        int level = plugin().getEnchantLevel(weapon, EnchantType.WING_CLIPPER);
        onPlayerHit(
                level,
                plugin().isSword(weapon),
                tickCounter,
                blockedUntil -> plugin().setWingClipperBlockedUntil(targetPlayer.getUniqueId(), blockedUntil),
                () -> ripElytraIfPresent(targetPlayer)
        );
    }

    private void ripElytraIfPresent(@NotNull Player targetPlayer) {
        ItemStack chest = targetPlayer.getInventory().getChestplate();
        boolean wearingElytra = chest != null && chest.getType() == Material.ELYTRA;
        if (!shouldRipElytra(wearingElytra)) {
            return;
        }

        ItemStack clipped = chest.clone();
        targetPlayer.getInventory().setChestplate(new ItemStack(Material.AIR));
        int max = clipped.getType().getMaxDurability();
        int maxRemaining = Math.max(1, (int) Math.ceil(max * maxRemainingDurabilityFraction()));
        plugin().enforceDurabilityCap(clipped, maxRemaining, targetPlayer, EquipmentSlot.CHEST);

        java.util.Map<Integer, ItemStack> overflow = targetPlayer.getInventory().addItem(clipped);
        for (ItemStack extra : overflow.values()) {
            targetPlayer.getWorld().dropItemNaturally(targetPlayer.getLocation(), extra);
        }
        targetPlayer.sendActionBar(Component.text(
                plugin().message("enchants.wing_clipper.ripped_elytra", "Wing Clipper ripped off your Elytra."),
                NamedTextColor.RED
        ));
    }
}

