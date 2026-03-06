package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.DisplayNameUtil;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MasqueradeEnchant extends BaseForbiddenEnchant {
    public MasqueradeEnchant() {
        super("masquerade",
                "masquerade_level",
                "Masquerade",
                ArmorSlot.BOOTS,
                1,
                NamedTextColor.DARK_GREEN,
                List.of("masq"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Sneak to disguise as nearest mob; no attacking while disguised.";
    }

    public void maintain(@NotNull ForbiddenEnchantsPlugin plugin,
                         @NotNull Map<UUID, MasqueradeState> masqueradeStates,
                         @NotNull Player player) {
        MasqueradeState state = masqueradeStates.get(player.getUniqueId());
        if (state == null) {
            return;
        }

        if (!player.isSneaking()) {
            clear(plugin, masqueradeStates, player);
            return;
        }

        LivingEntity disguise = state.disguise();
        if (!disguise.isValid() || disguise.isDead()) {
            clear(plugin, masqueradeStates, player);
            return;
        }

        Location location = player.getLocation();
        disguise.teleport(location);
        disguise.setRotation(location.getYaw(), location.getPitch());
        disguise.setNoPhysics(true);
        disguise.setVelocity(new Vector(0.0D, 0.0D, 0.0D));
        player.hideEntity(plugin, disguise);

        for (Entity nearby : player.getNearbyEntities(24.0D, 24.0D, 24.0D)) {
            if (nearby instanceof Mob mob
                    && shouldIgnoreMob(mob)
                    && mob.getTarget() != null
                    && mob.getTarget().equals(player)) {
                mob.setTarget(null);
            }
        }

        MasqueradeEquipment equipment = state.equipment();
        if (player.getInventory().getHeldItemSlot() != equipment.heldSlot()) {
            player.getInventory().setHeldItemSlot(equipment.heldSlot());
        }
        hideVisualEquipment(player);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 10, 0, true, false, false), true);
    }

    public void start(@NotNull ForbiddenEnchantsPlugin plugin,
                      @NotNull Map<UUID, MasqueradeState> masqueradeStates,
                      @NotNull Player player) {
        clear(plugin, masqueradeStates, player);

        EntityType nearestType = findNearestMobType(player, 18.0);
        if (nearestType == null) {
            player.sendActionBar(Component.text("Masquerade: no nearby mob to copy.", NamedTextColor.RED));
            return;
        }

        Entity created = player.getWorld().spawnEntity(player.getLocation(), nearestType);
        if (!(created instanceof LivingEntity living)) {
            created.remove();
            return;
        }

        MasqueradeEquipment equipment = captureEquipment(player);
        boolean wasCollidable = player.isCollidable();

        living.setAI(false);
        living.setSilent(true);
        living.setInvulnerable(true);
        living.setCollidable(false);
        living.setGravity(false);
        living.setNoPhysics(true);
        if (living instanceof Mob mob) {
            mob.setCanPickupItems(false);
            mob.setAware(false);
            mob.setTarget(null);
        }

        player.hideEntity(plugin, living);
        hideVisualEquipment(player);
        player.setCollidable(false);

        masqueradeStates.put(player.getUniqueId(), new MasqueradeState(nearestType, living, equipment, wasCollidable));
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 10, 0, true, false, false), true);
        player.sendActionBar(Component.text("Masquerade: " + DisplayNameUtil.toDisplayName(nearestType), NamedTextColor.GREEN));
    }

    public boolean isMasquerading(@NotNull Map<UUID, MasqueradeState> masqueradeStates,
                                  @NotNull Player player) {
        return masqueradeStates.containsKey(player.getUniqueId());
    }

    public void clear(@NotNull ForbiddenEnchantsPlugin plugin,
                      @NotNull Map<UUID, MasqueradeState> masqueradeStates,
                      @NotNull Player player) {
        MasqueradeState state = masqueradeStates.remove(player.getUniqueId());
        if (state != null) {
            player.showEntity(plugin, state.disguise());
            state.disguise().remove();
            restoreEquipment(player, state.equipment());
            player.setCollidable(state.wasCollidable());
            player.sendActionBar(Component.text("Masquerade ended.", NamedTextColor.YELLOW));
        }
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
    }

    public void clearAll(@NotNull ForbiddenEnchantsPlugin plugin,
                         @NotNull Map<UUID, MasqueradeState> masqueradeStates) {
        for (Map.Entry<UUID, MasqueradeState> entry : masqueradeStates.entrySet()) {
            MasqueradeState state = entry.getValue();
            Player player = Bukkit.getPlayer(entry.getKey());
            if (player != null && player.isOnline()) {
                player.showEntity(plugin, state.disguise());
                restoreEquipment(player, state.equipment());
                player.setCollidable(state.wasCollidable());
                player.removePotionEffect(PotionEffectType.INVISIBILITY);
            }
            state.disguise().remove();
        }
        masqueradeStates.clear();
    }

    public boolean shouldIgnoreMob(@NotNull Mob mob) {
        EntityType type = mob.getType();
        return type != EntityType.IRON_GOLEM && type != EntityType.WARDEN;
    }

    @Override
    public void onToggleSneak(@NotNull PlayerToggleSneakEvent event, long tickCounter) {
        Player player = event.getPlayer();
        if (ForbiddenEnchantsPlugin.instance().isMasquerading(player)) {
            if (!event.isSneaking()) {
                ForbiddenEnchantsPlugin.instance().clearMasquerade(player);
            }
            return;
        }

        ItemStack boots = player.getInventory().getBoots();
        if (ForbiddenEnchantsPlugin.instance().getEnchantLevel(boots, dev.cevapi.forbiddenenchants.EnchantType.MASQUERADE) <= 0) {
            return;
        }
        if (event.isSneaking()) {
            ForbiddenEnchantsPlugin.instance().startMasquerade(player);
        }
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (!ForbiddenEnchantsPlugin.instance().isMasquerading(player)
                && !ForbiddenEnchantsPlugin.instance().hasMiasmaForm(player)) {
            return;
        }
        event.setCancelled(true);
    }

    @Override
    public void onProjectileLaunch(@NotNull ProjectileLaunchEvent event, long tickCounter) {
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }
        if (!ForbiddenEnchantsPlugin.instance().isMasquerading(player)
                && !ForbiddenEnchantsPlugin.instance().hasMiasmaForm(player)) {
            return;
        }
        event.setCancelled(true);
    }

    @Override
    public void onMobTarget(@NotNull EntityTargetLivingEntityEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Mob mob) || !(event.getTarget() instanceof Player player)) {
            return;
        }
        if (ForbiddenEnchantsPlugin.instance().hasMiasmaForm(player) && mob.getType() != EntityType.BLAZE) {
            ForbiddenEnchantsPlugin.instance().cancelMobTarget(event, mob);
            return;
        }
        if (ForbiddenEnchantsPlugin.instance().hasHatedOne(player)) {
            return;
        }
        if (!ForbiddenEnchantsPlugin.instance().isMasquerading(player)) {
            return;
        }
        if (!shouldIgnoreMob(mob)) {
            return;
        }
        ForbiddenEnchantsPlugin.instance().cancelMobTarget(event, mob);
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        if (ForbiddenEnchantsPlugin.instance().isMasquerading(player)) {
            EnchantList.INSTANCE.forbiddenAgility().clearFor(player);
            ForbiddenEnchantsPlugin.instance().maintainMasquerade(player);
            return;
        }

        ItemStack boots = player.getInventory().getBoots();
        if (!ForbiddenEnchantsPlugin.instance().isArmorPieceForSlot(boots, dev.cevapi.forbiddenenchants.ArmorSlot.BOOTS)) {
            EnchantList.INSTANCE.forbiddenAgility().clearFor(player);
            return;
        }

        ForbiddenEnchantsPlugin.instance().revealMysteryItemIfNeeded(boots, player, org.bukkit.inventory.EquipmentSlot.FEET);
        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(boots, dev.cevapi.forbiddenenchants.EnchantType.MASQUERADE);
        if (level <= 0) {
            ForbiddenEnchantsPlugin.instance().clearMasquerade(player);
            return;
        }
        ForbiddenEnchantsPlugin.instance().enforceDurabilityCap(
                boots,
                org.bukkit.Material.LEATHER_BOOTS.getMaxDurability(),
                player,
                org.bukkit.inventory.EquipmentSlot.FEET
        );
        ForbiddenEnchantsPlugin.instance().maintainMasquerade(player);
    }

    private @Nullable EntityType findNearestMobType(@NotNull Player player, double radius) {
        Entity nearest = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        for (Entity entity : player.getNearbyEntities(radius, radius, radius)) {
            if (!(entity instanceof Mob)) {
                continue;
            }
            if (!entity.isValid() || entity.isDead()) {
                continue;
            }

            double distanceSquared = entity.getLocation().distanceSquared(player.getLocation());
            if (distanceSquared < nearestDistanceSquared) {
                nearestDistanceSquared = distanceSquared;
                nearest = entity;
            }
        }

        return nearest == null ? null : nearest.getType();
    }

    private @NotNull MasqueradeEquipment captureEquipment(@NotNull Player player) {
        PlayerInventory inventory = player.getInventory();
        return new MasqueradeEquipment(
                cloneStack(inventory.getHelmet()),
                cloneStack(inventory.getChestplate()),
                cloneStack(inventory.getLeggings()),
                cloneStack(inventory.getBoots()),
                cloneStack(inventory.getItemInMainHand()),
                cloneStack(inventory.getItemInOffHand()),
                inventory.getHeldItemSlot()
        );
    }

    private void hideVisualEquipment(@NotNull Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.setHelmet(new ItemStack(Material.AIR));
        inventory.setChestplate(new ItemStack(Material.AIR));
        inventory.setLeggings(new ItemStack(Material.AIR));
        inventory.setBoots(new ItemStack(Material.AIR));
        inventory.setItemInMainHand(new ItemStack(Material.AIR));
        inventory.setItemInOffHand(new ItemStack(Material.AIR));
    }

    private void restoreEquipment(@NotNull Player player, @NotNull MasqueradeEquipment equipment) {
        PlayerInventory inventory = player.getInventory();
        inventory.setHelmet(cloneOrAir(equipment.helmet()));
        inventory.setChestplate(cloneOrAir(equipment.chestplate()));
        inventory.setLeggings(cloneOrAir(equipment.leggings()));
        inventory.setBoots(cloneOrAir(equipment.boots()));
        inventory.setHeldItemSlot(Math.max(0, Math.min(8, equipment.heldSlot())));
        inventory.setItemInMainHand(cloneOrAir(equipment.mainHand()));
        inventory.setItemInOffHand(cloneOrAir(equipment.offHand()));
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

    public record MasqueradeState(@NotNull EntityType mobType,
                                  @NotNull LivingEntity disguise,
                                  @NotNull MasqueradeEquipment equipment,
                                  boolean wasCollidable) {
    }

    public record MasqueradeEquipment(@Nullable ItemStack helmet,
                                      @Nullable ItemStack chestplate,
                                      @Nullable ItemStack leggings,
                                      @Nullable ItemStack boots,
                                      @Nullable ItemStack mainHand,
                                      @Nullable ItemStack offHand,
                                      int heldSlot) {
    }
}

