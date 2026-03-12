package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public final class AdministrativeAspirationsEnchant extends BaseForbiddenEnchant {
    private static final String LEVEL_TWO_BAN_MESSAGE = "Banned by Administrative Aspirations Sword for 5 Minutes";

    public AdministrativeAspirationsEnchant() {
        super("administrative_aspirations",
                "administrative_aspirations_level",
                "Administrative Aspirations",
                ArmorSlot.SWORD,
                2,
                NamedTextColor.DARK_RED,
                List.of("administrative_aspirations", "admin_aspirations", "admin_sword"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return switch (Math.max(1, Math.min(2, level))) {
            case 2 -> "On player hit: 5-minute ban + kick (50% durability cost). On mob hit: teleports mob to world spawn (2% durability cost).";
            default -> "On player hit: instant kick (25% durability cost). On mob hit: teleports mob to world spawn (2% durability cost).";
        };
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }

        ItemStack weapon = attacker.getInventory().getItemInMainHand();
        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(weapon, EnchantType.ADMINISTRATIVE_ASPIRATIONS);
        if (level <= 0 || !ForbiddenEnchantsPlugin.instance().isSword(weapon)) {
            return;
        }

        if (event.getEntity() instanceof Player targetPlayer) {
            applyToPlayerHit(attacker, weapon, targetPlayer, level);
            return;
        }

        if (event.getEntity() instanceof LivingEntity mobTarget) {
            mobTarget.teleport(mobTarget.getWorld().getSpawnLocation());
            attacker.sendActionBar(Component.text("Administrative Aspirations teleported the mob to spawn.", NamedTextColor.GOLD));
            ForbiddenEnchantsPlugin.instance().damageItemByPercent(attacker, EquipmentSlot.HAND, weapon, 0.02D);
        }
    }

    private void applyToPlayerHit(@NotNull Player attacker,
                                  @NotNull ItemStack weapon,
                                  @NotNull Player targetPlayer,
                                  int level) {
        if (level >= 2) {
            Date expires = Date.from(Instant.now().plus(Duration.ofMinutes(5)));
            Bukkit.getBanList(BanList.Type.NAME).addBan(targetPlayer.getName(), LEVEL_TWO_BAN_MESSAGE, expires, "Administrative Aspirations");
            targetPlayer.kick(Component.text(LEVEL_TWO_BAN_MESSAGE, NamedTextColor.RED));
            ForbiddenEnchantsPlugin.instance().damageItemByPercent(attacker, EquipmentSlot.HAND, weapon, 0.50D);
            return;
        }

        targetPlayer.kick(Component.text("Kicked by Administrative Aspirations Sword", NamedTextColor.RED));
        ForbiddenEnchantsPlugin.instance().damageItemByPercent(attacker, EquipmentSlot.HAND, weapon, 0.25D);
    }
}
