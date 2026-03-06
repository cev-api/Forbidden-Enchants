package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.entity.ZombieVillager;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class HealingTouchEnchant extends BaseForbiddenEnchant {
    private static final double SELF_DAMAGE = 2.0D;

    public HealingTouchEnchant() {
        super("healing_touch",
                "healing_touch_level",
                "Healing Touch",
                ArmorSlot.HOE,
                1,
                NamedTextColor.GREEN,
                List.of("heal", "healing", "healingtouch"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Hoe strikes heal targets and apply golden-apple buffs, while hurting you.";
    }

    public boolean isActive(int level) {
        return level > 0;
    }

    public void onHit(@NotNull Plugin schedulerPlugin,
                      @NotNull EntityDamageByEntityEvent event,
                      @NotNull Player attacker,
                      @NotNull LivingEntity target,
                      double selfDamage,
                      @NotNull Consumer<UUID> clearWitheringStrikeTarget,
                      @NotNull ItemDamageApplier damageItem) {
        if (target instanceof ZombieVillager zombieVillager) {
            event.setCancelled(true);
            if (!zombieVillager.isConverting()) {
                zombieVillager.setConversionTime(200);
                target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, target.getLocation().add(0.0D, 1.0D, 0.0D), 10, 0.25, 0.3, 0.25, 0.0);
            }
            damagePlayer(attacker, selfDamage);
            damageItem.apply(1);
            return;
        }

        if (target instanceof WitherSkeleton witherSkeleton) {
            event.setDamage(Math.max(2.0D, event.getDamage() * 2.0D));
            UUID witherId = witherSkeleton.getUniqueId();
            Bukkit.getScheduler().runTaskLater(schedulerPlugin, () -> {
                Entity entity = Bukkit.getEntity(witherId);
                if (!(entity instanceof WitherSkeleton live) || !live.isValid() || live.isDead()) {
                    return;
                }
                convertWitherSkeleton(live);
            }, 1L);
            damagePlayer(attacker, selfDamage);
            damageItem.apply(4);
            return;
        }

        if (isUndeadTarget(target)) {
            event.setDamage(Math.max(0.0D, event.getDamage()) * 2.0D);
            damagePlayer(attacker, selfDamage);
            damageItem.apply(4);
            return;
        }

        double prevented = Math.max(0.0D, event.getFinalDamage());
        event.setCancelled(true);

        if (prevented > 0.0D) {
            AttributeInstance maxHealth = target.getAttribute(Attribute.MAX_HEALTH);
            double cap = maxHealth == null ? target.getHealth() + prevented : maxHealth.getValue();
            target.setHealth(Math.min(cap, target.getHealth() + prevented));
        }
        target.removePotionEffect(PotionEffectType.WITHER);
        clearWitheringStrikeTarget.accept(target.getUniqueId());
        target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1, true, true, true), true);
        target.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 2400, 0, true, true, true), true);
        target.getWorld().spawnParticle(Particle.HEART, target.getLocation().add(0.0D, target.getHeight() * 0.5D, 0.0D), 8, 0.25, 0.25, 0.25, 0.0);

        damagePlayer(attacker, selfDamage);
        damageItem.apply(8);
    }

    private boolean isUndeadTarget(@NotNull LivingEntity target) {
        return switch (target.getType()) {
            case WITHER, ZOMBIE, HUSK, DROWNED, ZOMBIFIED_PIGLIN, ZOMBIE_VILLAGER,
                    SKELETON, STRAY, BOGGED, PHANTOM, SKELETON_HORSE, ZOMBIE_HORSE -> true;
            default -> false;
        };
    }

    private void convertWitherSkeleton(@NotNull WitherSkeleton witherSkeleton) {
        World world = witherSkeleton.getWorld();
        Location location = witherSkeleton.getLocation();

        Skeleton skeleton = world.spawn(location, Skeleton.class);
        skeleton.setPersistent(witherSkeleton.isPersistent());
        skeleton.setSilent(witherSkeleton.isSilent());
        skeleton.customName(witherSkeleton.customName());
        skeleton.setCustomNameVisible(witherSkeleton.isCustomNameVisible());

        if (witherSkeleton.getEquipment() != null && skeleton.getEquipment() != null) {
            skeleton.getEquipment().setHelmet(witherSkeleton.getEquipment().getHelmet());
            skeleton.getEquipment().setChestplate(witherSkeleton.getEquipment().getChestplate());
            skeleton.getEquipment().setLeggings(witherSkeleton.getEquipment().getLeggings());
            skeleton.getEquipment().setBoots(witherSkeleton.getEquipment().getBoots());
            skeleton.getEquipment().setItemInMainHand(witherSkeleton.getEquipment().getItemInMainHand());
            skeleton.getEquipment().setItemInOffHand(witherSkeleton.getEquipment().getItemInOffHand());
        }

        world.spawnParticle(Particle.SMOKE, location.add(0.0D, 1.0D, 0.0D), 16, 0.3, 0.4, 0.3, 0.01);
        world.playSound(location, Sound.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 0.7F, 1.4F);
        witherSkeleton.remove();
    }

    private void damagePlayer(@NotNull Player player, double amount) {
        if (player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.damage(amount);
        }
    }

    @FunctionalInterface
    public interface ItemDamageApplier {
        void apply(int durabilityDamage);
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        ItemStack weapon = player.getInventory().getItemInMainHand();
        if (!ForbiddenEnchantsPlugin.instance().isHoe(weapon)) {
            return;
        }

        int level = ForbiddenEnchantsPlugin.instance().getEnchantLevel(weapon, EnchantType.HEALING_TOUCH);
        if (!isActive(level)) {
            return;
        }

        ForbiddenEnchantsPlugin.instance().revealMysteryItemIfNeeded(weapon, player, EquipmentSlot.HAND);
        ItemMeta meta = weapon.getItemMeta();
        if (meta != null && ForbiddenEnchantsPlugin.instance().hasHealingTouchForbiddenEnchant(meta)) {
            ForbiddenEnchantsPlugin.instance().stripHealingTouchForbiddenEnchants(meta);
            weapon.setItemMeta(meta);
        }

        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        onHit(
                ForbiddenEnchantsPlugin.instance(),
                event,
                player,
                target,
                SELF_DAMAGE,
                ForbiddenEnchantsPlugin.instance()::clearWitheringTarget,
                durabilityDamage -> ForbiddenEnchantsPlugin.instance().damageHeldItem(player, EquipmentSlot.HAND, durabilityDamage)
        );
    }
}

