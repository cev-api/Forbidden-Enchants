package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class StaffOfTheEvokerEnchant extends BaseForbiddenEnchant {
    private static final long LOOP_CHAIN_WINDOW_TICKS = 20L;

    private final Map<UUID, Long> lastCastTickByPlayer = new HashMap<>();
    private final Map<UUID, Integer> castChainByPlayer = new HashMap<>();

    public StaffOfTheEvokerEnchant() {
        super("staff_of_the_evoker",
                "staff_of_the_evoker_level",
                "Staff Of The Evoker",
                ArmorSlot.SPEAR,
                1,
                NamedTextColor.DARK_PURPLE,
                List.of("staff", "evokerstaff", "staffoftheevoker", "staff_of_the_evoker"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Spear swings and casts launch a long line of evoker fangs that damage mobs.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }

    public boolean isCastAction(@NotNull Action action) {
        return action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;
    }

    public long cooldownTicks() {
        return 8L;
    }

    public boolean canUse(int level, boolean spear, double horizontalDirectionLengthSquared) {
        return spear && isActive(level) && horizontalDirectionLengthSquared >= 0.001D;
    }

    public double range() {
        return 14.0D;
    }

    public int fangCount() {
        return 10;
    }

    public boolean cast(@NotNull Plugin schedulerPlugin,
                        @NotNull Player player,
                        @NotNull Vector direction) {
        World world = player.getWorld();
        Location from = player.getLocation().add(0.0D, 0.1D, 0.0D);
        double step = range() / Math.max(1, fangCount());
        List<Location> fangPositions = new ArrayList<>();
        for (int i = 1; i <= fangCount(); i++) {
            Location fangLoc = resolveFangLocation(from.clone().add(direction.clone().multiply(i * step)));
            if (fangLoc == null) {
                continue;
            }
            world.spawn(fangLoc, org.bukkit.entity.EvokerFangs.class);
            fangPositions.add(fangLoc.clone());
        }
        if (fangPositions.isEmpty()) {
            return false;
        }
        world.playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 1.0F, 1.05F);
        Bukkit.getScheduler().runTaskLater(schedulerPlugin, () -> {
            for (Location fangLoc : fangPositions) {
                for (Entity nearby : world.getNearbyEntities(fangLoc, 0.9D, 1.2D, 0.9D)) {
                    if (!(nearby instanceof Mob mob) || nearby.getUniqueId().equals(player.getUniqueId())) {
                        continue;
                    }
                    mob.setNoDamageTicks(0);
                    mob.damage(6.0D, player);
                }
            }
        }, 8L);
        return true;
    }

    private Location resolveFangLocation(@NotNull Location raw) {
        World world = raw.getWorld();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        int x = raw.getBlockX();
        int z = raw.getBlockZ();
        int y = Math.max(minY + 1, Math.min(maxY - 1, raw.getBlockY() + 1));

        while (y > minY + 1 && world.getBlockAt(x, y - 1, z).isPassable()) {
            y--;
        }
        while (y < maxY - 1 && !world.getBlockAt(x, y, z).isPassable()) {
            y++;
        }
        if (y >= maxY - 1) {
            return null;
        }
        return new Location(world, raw.getX(), y + 0.05D, raw.getZ());
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        ItemStack weapon = player.getInventory().getItemInMainHand();
        ForbiddenEnchantsPlugin.instance().revealMysteryItemIfNeeded(weapon, player, org.bukkit.inventory.EquipmentSlot.HAND);
        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(weapon, EnchantType.STAFF_OF_THE_EVOKER);
        Vector direction = player.getEyeLocation().getDirection().setY(0.0D);
        if (!canUse(level, ForbiddenEnchantsPlugin.instance().isSpear(weapon), direction.lengthSquared())) {
            return;
        }
        if (!cast(ForbiddenEnchantsPlugin.instance(), player, direction.normalize())) {
            return;
        }
        applyDurabilityCost(player, weapon, tickCounter);
    }

    @Override
    public void onInteract(@NotNull PlayerInteractEvent event, long tickCounter) {
        if (!isCastAction(event.getAction())) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack weapon = player.getInventory().getItemInMainHand();
        ForbiddenEnchantsPlugin.instance().revealMysteryItemIfNeeded(weapon, player, org.bukkit.inventory.EquipmentSlot.HAND);
        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(weapon, EnchantType.STAFF_OF_THE_EVOKER);

        long readyTick = ForbiddenEnchantsPlugin.instance().staffOfEvokerReadyTick(player.getUniqueId());
        if (tickCounter < readyTick) {
            return;
        }

        Vector direction = player.getEyeLocation().getDirection().setY(0.0D);
        if (!canUse(level, ForbiddenEnchantsPlugin.instance().isSpear(weapon), direction.lengthSquared())) {
            return;
        }

        if (!cast(ForbiddenEnchantsPlugin.instance(), player, direction.normalize())) {
            return;
        }
        applyDurabilityCost(player, weapon, tickCounter);
        ForbiddenEnchantsPlugin.instance().setStaffOfEvokerReadyTick(player.getUniqueId(), tickCounter + cooldownTicks());
    }

    private void applyDurabilityCost(@NotNull Player player, @NotNull ItemStack weapon, long tickCounter) {
        int maxDurability = weapon.getType().getMaxDurability();
        if (maxDurability <= 0) {
            return;
        }

        UUID playerId = player.getUniqueId();
        long lastCast = lastCastTickByPlayer.getOrDefault(playerId, Long.MIN_VALUE);
        int chain = (tickCounter - lastCast) <= LOOP_CHAIN_WINDOW_TICKS
                ? castChainByPlayer.getOrDefault(playerId, 0) + 1
                : 1;

        lastCastTickByPlayer.put(playerId, tickCounter);
        castChainByPlayer.put(playerId, chain);

        int durabilityDamage = 1;
        if (chain >= 2) {
            durabilityDamage = Math.max(6, (int) Math.ceil(maxDurability * 0.04D));
        }
        if (chain >= 5) {
            durabilityDamage = Math.max(durabilityDamage, (int) Math.ceil(maxDurability * 0.08D));
        }

        ForbiddenEnchantsPlugin.instance().damageHeldItem(player, EquipmentSlot.HAND, durabilityDamage);
    }
}

