package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ThePretenderEnchant extends BaseForbiddenEnchant {
    private static final long STEALTH_DURATION_TICKS = 20L * 30L;

    private final Map<UUID, Long> hiddenUntil = new HashMap<>();

    public ThePretenderEnchant() {
        super("the_pretender",
                "the_pretender_level",
                "The Pretender",
                ArmorSlot.TOTEM,
                1,
                NamedTextColor.GRAY,
                List.of("pretender", "thepretender", "the_pretender"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "On totem pop: fake death message + death-style loot drop, then 30s of silent invisibility with hostile aggro ignored.";
    }

    @Override
    public void onDamage(@NotNull EntityDamageEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getFinalDamage() < player.getHealth()) {
            return;
        }

        TotemRef ref = findPretenderTotem(player);
        if (ref == null) {
            return;
        }

        event.setCancelled(true);
        consumeTotem(player, ref);
        applyPretenderRescueEffects(player);
        triggerPretender(player, tickCounter);
    }

    @Override
    public void onTotemPop(@NotNull EntityResurrectEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        ItemStack poppedTotem = getResurrectedTotem(player, event);
        if (poppedTotem.getType() != Material.TOTEM_OF_UNDYING) {
            return;
        }
        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(poppedTotem, EnchantType.THE_PRETENDER);
        if (level <= 0) {
            return;
        }

        triggerPretender(player, tickCounter);
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        UUID playerId = player.getUniqueId();
        Long until = hiddenUntil.get(playerId);
        if (until == null) {
            return;
        }
        if (tickCounter > until || !player.isOnline() || player.isDead()) {
            revealPlayer(playerId);
            return;
        }
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 30, 0, true, false, false), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 30, 1, true, false, false), true);
        player.setSilent(true);
        keepHiddenFromAllPlayers(player);
        suppressNearbyHostileTargets(player);
    }

    @Override
    public void onMobTarget(@NotNull EntityTargetLivingEntityEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Mob mob)) {
            return;
        }
        if (!(mob instanceof Monster)) {
            return;
        }
        if (!(event.getTarget() instanceof Player target)) {
            return;
        }
        Long until = hiddenUntil.get(target.getUniqueId());
        if (until == null) {
            return;
        }
        if (tickCounter <= until) {
            event.setCancelled(true);
            mob.setTarget(null);
        }
    }

    public void revealPlayer(@NotNull UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        hiddenUntil.remove(playerId);
        if (player == null) {
            return;
        }
        player.setSilent(false);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) {
                viewer.showPlayer(ForbiddenEnchantsPlugin.instance(), player);
            }
        }
    }

    private void hidePlayer(@NotNull Player player, long untilTick) {
        hiddenUntil.put(player.getUniqueId(), untilTick);
        player.setSilent(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (int) STEALTH_DURATION_TICKS, 0, true, false, false), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 120, 1, true, false, false), true);
        keepHiddenFromAllPlayers(player);
        suppressNearbyHostileTargets(player);
    }

    private void triggerPretender(@NotNull Player player, long tickCounter) {
        Location location = player.getLocation().clone();
        World world = location.getWorld();
        if (world == null) {
            return;
        }
        dropPretenderLoot(world, location);
        Bukkit.broadcastMessage(player.getName() + " died");
        world.playSound(location, Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, SoundCategory.PLAYERS, 0.8F, 0.7F);
        hidePlayer(player, tickCounter + STEALTH_DURATION_TICKS);
    }

    private void applyPretenderRescueEffects(@NotNull Player player) {
        double health = Math.max(1.0D, Math.min(player.getMaxHealth(), 1.0D));
        player.setHealth(health);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 900, 1, true, false, false), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 1, true, false, false), true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 800, 0, true, false, false), true);
        player.setFireTicks(0);
    }

    private void keepHiddenFromAllPlayers(@NotNull Player player) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.equals(player)) {
                viewer.hidePlayer(ForbiddenEnchantsPlugin.instance(), player);
            }
        }
    }

    private void suppressNearbyHostileTargets(@NotNull Player player) {
        for (Entity entity : player.getNearbyEntities(48.0D, 24.0D, 48.0D)) {
            if (entity instanceof Monster monster) {
                monster.setTarget(null);
            }
        }
    }

    private @org.jetbrains.annotations.Nullable TotemRef findPretenderTotem(@NotNull Player player) {
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() == Material.TOTEM_OF_UNDYING
                && ForbiddenEnchantsPlugin.instance().getEnchantLevel(off, EnchantType.THE_PRETENDER) > 0) {
            return new TotemRef(EquipmentSlot.OFF_HAND, off);
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() == Material.TOTEM_OF_UNDYING
                && ForbiddenEnchantsPlugin.instance().getEnchantLevel(main, EnchantType.THE_PRETENDER) > 0) {
            return new TotemRef(EquipmentSlot.HAND, main);
        }
        return null;
    }

    private void consumeTotem(@NotNull Player player, @NotNull TotemRef ref) {
        ItemStack stack = ref.stack();
        if (stack.getAmount() <= 1) {
            if (ref.slot() == EquipmentSlot.OFF_HAND) {
                player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
            } else {
                player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
            }
            return;
        }
        stack.setAmount(stack.getAmount() - 1);
        if (ref.slot() == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(stack);
        } else {
            player.getInventory().setItemInMainHand(stack);
        }
    }

    private void dropPretenderLoot(@NotNull World world, @NotNull Location location) {
        drop(world, location, new ItemStack(Material.DIAMOND_HELMET), 360);
        drop(world, location, new ItemStack(Material.DIAMOND_CHESTPLATE), 420);
        drop(world, location, new ItemStack(Material.DIAMOND_LEGGINGS), 390);
        drop(world, location, new ItemStack(Material.DIAMOND_BOOTS), 320);
        drop(world, location, new ItemStack(Material.DIAMOND_SWORD), 500);

        drop(world, location, new ItemStack(Material.DIRT, 10));
        drop(world, location, new ItemStack(Material.ENDER_PEARL, 2));
        drop(world, location, new ItemStack(Material.REDSTONE, 2));
        drop(world, location, new ItemStack(Material.BEETROOT_SEEDS, 4));
        drop(world, location, new ItemStack(Material.WHEAT, 4));
        drop(world, location, new ItemStack(Material.CARROT, 10));
        drop(world, location, new ItemStack(Material.MELON_SLICE, 3));
        drop(world, location, new ItemStack(Material.PUMPKIN, 2));
        drop(world, location, new ItemStack(Material.COBBLESTONE, 20));
        drop(world, location, new ItemStack(Material.BONE, 10));
        drop(world, location, new ItemStack(Material.BONE_MEAL, 2));
        drop(world, location, new ItemStack(Material.POTION, 5));
        drop(world, location, new ItemStack(Material.SAND, 10));
    }

    private void drop(@NotNull World world, @NotNull Location location, @NotNull ItemStack item, int damage) {
        item.editMeta(meta -> {
            if (meta instanceof org.bukkit.inventory.meta.Damageable damageable) {
                damageable.setDamage(damage);
            }
        });
        drop(world, location, item);
    }

    private void drop(@NotNull World world, @NotNull Location location, @NotNull ItemStack item) {
        Entity dropped = world.dropItem(location.clone().add(0.0D, 0.25D, 0.0D), item);
        if (dropped instanceof org.bukkit.entity.Item droppedItem) {
            droppedItem.setPickupDelay(Integer.MAX_VALUE);
            Vector push = new Vector(
                    (Math.random() - 0.5D) * 0.35D,
                    0.25D + (Math.random() * 0.1D),
                    (Math.random() - 0.5D) * 0.35D
            );
            droppedItem.setVelocity(push);
            Bukkit.getScheduler().runTaskLater(
                    ForbiddenEnchantsPlugin.instance(),
                    () -> {
                        if (droppedItem.isValid()) {
                            droppedItem.remove();
                        }
                    },
                    20L * 30L
            );
        }
    }

    private @NotNull ItemStack getResurrectedTotem(@NotNull Player player, @NotNull EntityResurrectEvent event) {
        EquipmentSlot hand = event.getHand();
        if (hand == EquipmentSlot.HAND) {
            return player.getInventory().getItemInMainHand();
        }
        if (hand == EquipmentSlot.OFF_HAND) {
            return player.getInventory().getItemInOffHand();
        }
        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() == Material.TOTEM_OF_UNDYING) {
            return off;
        }
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() == Material.TOTEM_OF_UNDYING) {
            return main;
        }
        return new ItemStack(Material.AIR);
    }

    private record TotemRef(@NotNull EquipmentSlot slot, @NotNull ItemStack stack) {
    }
}
