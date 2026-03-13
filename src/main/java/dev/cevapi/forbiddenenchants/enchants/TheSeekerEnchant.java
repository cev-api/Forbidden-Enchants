package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class TheSeekerEnchant extends BaseForbiddenEnchant {
    @FunctionalInterface
    public interface HelmetDurabilityDamager {
        void damage(@NotNull Player player, @NotNull ItemStack helmet, double percent);
    }

    private final Map<UUID, Location> lastLocationByPlayer = new HashMap<>();
    private final Map<UUID, Double> distanceSinceDamageByPlayer = new HashMap<>();

    public TheSeekerEnchant() {
        super("the_seeker",
                "the_seeker_level",
                "The Seeker",
                ArmorSlot.HELMET,
                1,
                NamedTextColor.AQUA,
                List.of("seeker", "the_seeker", "theseeker"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Locator HUD for all online players; loses 10% helmet durability per 100 blocks traveled.";
    }

    public void tickPlayer(@NotNull Player player,
                           @NotNull ItemStack helmet,
                           long tickCounter,
                           @NotNull Predicate<ItemMeta> hasSeekerForbiddenEnchant,
                           @NotNull Consumer<ItemMeta> stripSeekerForbiddenEnchants,
                           @NotNull HelmetDurabilityDamager helmetDurabilityDamager) {
        ItemMeta meta = helmet.getItemMeta();
        if (meta != null && hasSeekerForbiddenEnchant.test(meta)) {
            stripSeekerForbiddenEnchants.accept(meta);
            helmet.setItemMeta(meta);
            player.getInventory().setHelmet(helmet);
        }

        UUID id = player.getUniqueId();
        Location current = player.getLocation();
        Location previous = lastLocationByPlayer.put(id, current.clone());
        if (previous != null) {
            double delta = previous.getWorld().equals(current.getWorld()) ? previous.distance(current) : 0.0D;
            if (delta > 0.0D) {
                double total = distanceSinceDamageByPlayer.getOrDefault(id, 0.0D) + delta;
                int steps = (int) Math.floor(total / 100.0D);
                if (steps > 0) {
                    helmetDurabilityDamager.damage(player, helmet, 0.10D * steps);
                    total -= steps * 100.0D;
                }
                distanceSinceDamageByPlayer.put(id, total);
            }
        }

        if (tickCounter % 20L != 0L) {
            return;
        }

        Vector lookDir = player.getEyeLocation().getDirection().normalize();
        double minDot = Math.cos(Math.toRadians(20.0D));
        Player best = null;
        double bestDot = minDot;
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.getUniqueId().equals(id) || !online.getWorld().equals(player.getWorld())) {
                continue;
            }
            Vector toTarget = online.getLocation().toVector().subtract(player.getEyeLocation().toVector());
            if (toTarget.lengthSquared() < 0.0001D) {
                continue;
            }
            double dot = lookDir.dot(toTarget.normalize());
            if (dot > bestDot) {
                bestDot = dot;
                best = online;
            }
        }

        if (best == null) {
            player.sendActionBar(Component.empty());
            return;
        }
        int distance = (int) Math.round(player.getLocation().distance(best.getLocation()));
        player.sendActionBar(Component.text(
                plugin().message(
                        "enchants.the_seeker.tracking",
                        "{player} {distance}m",
                        Map.of("player", best.getName(), "distance", String.valueOf(distance))
                ),
                NamedTextColor.AQUA
        ));
    }

    public void clearFor(@NotNull UUID playerId) {
        lastLocationByPlayer.remove(playerId);
        distanceSinceDamageByPlayer.remove(playerId);
    }

    public void resetAll() {
        lastLocationByPlayer.clear();
        distanceSinceDamageByPlayer.clear();
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        ItemStack helmet = player.getInventory().getHelmet();
        if (plugin().getEnchantLevel(helmet, EnchantType.THE_SEEKER) <= 0) {
            clearFor(player.getUniqueId());
            return;
        }

        tickPlayer(
                player,
                helmet,
                tickCounter,
                plugin()::hasSeekerForbiddenEnchant,
                plugin()::stripSeekerForbiddenEnchants,
                (owner, currentHelmet, percent) -> plugin().damageArmorByPercent(owner, org.bukkit.inventory.EquipmentSlot.HEAD, currentHelmet, percent)
        );
    }
}

