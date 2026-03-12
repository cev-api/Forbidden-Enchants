package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

final class PlayerEffectService {
    private final Supplier<EnchantStateService> enchantStateServiceSupplier;
    private final ItemCombatService itemCombatService;
    private final Supplier<org.bukkit.NamespacedKey> wololoConvertedKeySupplier;
    private final Map<UUID, Long> wingClipperBlockedUntil;

    PlayerEffectService(@NotNull Supplier<EnchantStateService> enchantStateServiceSupplier,
                        @NotNull ItemCombatService itemCombatService,
                        @NotNull Supplier<org.bukkit.NamespacedKey> wololoConvertedKeySupplier,
                        @NotNull Map<UUID, Long> wingClipperBlockedUntil) {
        this.enchantStateServiceSupplier = enchantStateServiceSupplier;
        this.itemCombatService = itemCombatService;
        this.wololoConvertedKeySupplier = wololoConvertedKeySupplier;
        this.wingClipperBlockedUntil = wingClipperBlockedUntil;
    }

    void applyAquaticSacrifice(@NotNull Player player, long tickCounter) {
        if (isPlayerPartiallySubmerged(player)) {
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.WATER_BREATHING, 80, 0, true, false, false), true);
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.DOLPHINS_GRACE, 80, 0, true, false, false), true);
            player.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.CONDUIT_POWER, 80, 0, true, false, false), true);
            player.setRemainingAir(player.getMaximumAir());
            return;
        }

        if (tickCounter % 40L != 0L) {
            return;
        }
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        player.damage(1.0D);
        player.getWorld().spawnParticle(Particle.BUBBLE, player.getLocation().add(0.0D, 1.0D, 0.0D), 6, 0.18, 0.2, 0.18, 0.0);
    }

    boolean isPlayerPartiallySubmerged(@NotNull Player player) {
        Location base = player.getLocation();
        World world = player.getWorld();
        return world.getBlockAt(base).isLiquid()
                || world.getBlockAt(base.clone().add(0.0D, 0.6D, 0.0D)).isLiquid()
                || world.getBlockAt(base.clone().add(0.0D, 1.1D, 0.0D)).isLiquid();
    }

    void applyLaunchFlightState(@NotNull Player player, @Nullable ItemStack chestplate) {
        if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        boolean hasLaunch = chestplate != null
                && chestplate.getType() == Material.ELYTRA
                && enchantStateServiceSupplier.get().getEnchantLevel(chestplate, EnchantType.LAUNCH) > 0;
        if (hasLaunch && player.isOnGround() && !player.isFlying()) {
            if (!player.getAllowFlight()) {
                player.setAllowFlight(true);
            }
            return;
        }
        if (!hasLaunch && player.getAllowFlight()) {
            player.setAllowFlight(false);
        }
    }

    boolean isWingClipperBlocked(@NotNull Player player, long tickCounter) {
        long until = wingClipperBlockedUntil.getOrDefault(player.getUniqueId(), 0L);
        if (tickCounter > until) {
            wingClipperBlockedUntil.remove(player.getUniqueId());
            return false;
        }
        return true;
    }

    boolean isMace(@Nullable ItemStack stack) {
        return stack != null && stack.getType() == Material.MACE;
    }

    void triggerSonicPanic(@NotNull Player player, @NotNull ItemStack sword) {
        World world = player.getWorld();
        Location center = player.getEyeLocation();
        world.playSound(center, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 2.0F, 1.0F);

        int rays = 16;
        double radius = 14.0D;
        for (int i = 0; i < rays; i++) {
            double angle = (Math.PI * 2.0D * i) / rays;
            Vector dir = new Vector(Math.cos(angle), 0.0D, Math.sin(angle));
            for (double step = 1.0D; step <= radius; step += 1.1D) {
                Location point = center.clone().add(dir.clone().multiply(step));
                world.spawnParticle(Particle.SONIC_BOOM, point, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }

        for (Entity nearby : player.getNearbyEntities(radius, 4.0D, radius)) {
            if (!(nearby instanceof LivingEntity living) || nearby.getUniqueId().equals(player.getUniqueId())) {
                continue;
            }
            living.damage(20.0D, player);
        }

        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() == sword.getType() && main.isSimilar(sword)) {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }
        world.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, SoundCategory.PLAYERS, 1.4F, 0.5F);
        player.sendActionBar(Component.text("Sonic Panic shattered your blade!", NamedTextColor.DARK_AQUA));
    }

    void applyCreepersInfluence(@NotNull Player player) {
        for (Entity nearby : player.getNearbyEntities(24.0D, 12.0D, 24.0D)) {
            if (!(nearby instanceof Creeper creeper) || !creeper.isValid() || creeper.isDead()) {
                continue;
            }
            LivingEntity target = null;
            double nearest = Double.MAX_VALUE;
            for (Entity mobNearby : creeper.getNearbyEntities(12.0D, 8.0D, 12.0D)) {
                if (!(mobNearby instanceof LivingEntity living) || living instanceof Creeper || living.equals(player)) {
                    continue;
                }
                if (living.isDead()) {
                    continue;
                }
                double dist = living.getLocation().distanceSquared(creeper.getLocation());
                if (dist < nearest) {
                    nearest = dist;
                    target = living;
                }
            }
            if (target != null) {
                creeper.setTarget(target);
                continue;
            }
            creeper.setTarget(null);
            if (!creeper.isIgnited()) {
                creeper.ignite();
            }
        }
    }

    void applyWololo(@NotNull Player player) {
        org.bukkit.NamespacedKey wololoConvertedKey = wololoConvertedKeySupplier.get();
        int conversions = 0;
        for (Entity nearby : player.getNearbyEntities(3.0D, 2.0D, 3.0D)) {
            if (!(nearby instanceof Sheep sheep)) {
                continue;
            }
            PersistentDataContainer data = sheep.getPersistentDataContainer();
            if (data.has(wololoConvertedKey, PersistentDataType.BYTE)) {
                continue;
            }
            DyeColor[] colors = DyeColor.values();
            sheep.setColor(colors[ThreadLocalRandom.current().nextInt(colors.length)]);
            data.set(wololoConvertedKey, PersistentDataType.BYTE, (byte) 1);
            conversions++;
        }
        if (conversions > 0 && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.damage(2.0D * conversions);
        }
    }

    boolean isLockedOutInteractionBlock(@NotNull Material type) {
        String name = type.name();
        return name.endsWith("_DOOR")
                || name.endsWith("_BUTTON")
                || name.endsWith("_PRESSURE_PLATE")
                || name.endsWith("_TRAPDOOR")
                || name.endsWith("_GATE")
                || type == Material.LEVER
                || type == Material.NETHER_PORTAL
                || type == Material.END_PORTAL;
    }

    boolean isAxe(@Nullable ItemStack stack) {
        return stack != null && stack.getType() != Material.AIR && stack.getType().name().endsWith("_AXE");
    }

    void applyAscension(@NotNull Player player) {
        if (player.isFlying() || player.isGliding() || player.isSwimming()) {
            return;
        }

        if (!isJumpHeld(player)) {
            return;
        }

        Vector velocity = player.getVelocity();
        if (player.isOnGround()) {
            velocity.setY(Math.max(0.55, velocity.getY() + 0.45));
        } else {
            double boost = velocity.getY() < 0.0 ? 0.18 : 0.12;
            velocity.setY(Math.min(0.95, velocity.getY() + boost));
        }
        player.setVelocity(velocity);
        player.spawnParticle(Particle.CLOUD, player.getLocation().add(0.0, 0.1, 0.0), 3, 0.2, 0.1, 0.2, 0.0);
        itemCombatService.damageEquippedArmor(player, EquipmentSlot.FEET, 5, false);
    }

    private boolean isJumpHeld(@NotNull Player player) {
        try {
            org.bukkit.Input input = player.getCurrentInput();
            return input != null && input.isJump();
        } catch (NoSuchMethodError | NoClassDefFoundError ignored) {
            return false;
        }
    }
}

