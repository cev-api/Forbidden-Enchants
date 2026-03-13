package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BorgTechnologyEnchant extends BaseForbiddenEnchant {
    private static final double SHIELD_DISTANCE_BLOCKS = 337.0D;

    private final Map<UUID, Integer> consecutiveHits = new HashMap<>();
    private final Map<UUID, String> lastHitProfile = new HashMap<>();
    private final Map<UUID, ShieldState> activeShield = new HashMap<>();
    private final Map<UUID, Location> shieldLastLocation = new HashMap<>();
    private final Map<UUID, Long> hitVisualUntil = new HashMap<>();

    public BorgTechnologyEnchant() {
        super("borg_technology",
                "borg_technology_level",
                "Borg Technology",
                ArmorSlot.CHESTPLATE,
                1,
                NamedTextColor.GREEN,
                List.of("borg", "borgtech", "borg_technology"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "After two hits, adapt on the third: gain a temporary barrier that blocks matching damage profile for 337 blocks of movement.";
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID id = player.getUniqueId();
        ItemStack chestplate = player.getInventory().getChestplate();
        if (plugin().getEnchantLevel(chestplate, EnchantType.BORG_TECHNOLOGY) <= 0) {
            clearState(id);
            return;
        }

        hitVisualUntil.put(id, tickCounter + 20L);
        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 25, 0, true, false, true), true);

        String profile = profileFromByEntity(event);
        ShieldState shield = activeShield.get(id);
        if (shield != null) {
            if (shield.profile().equals(profile)) {
                blockDamage(player, event);
            }
            return;
        }

        if (profile.equals(lastHitProfile.get(id))) {
            consecutiveHits.put(id, consecutiveHits.getOrDefault(id, 1) + 1);
        } else {
            lastHitProfile.put(id, profile);
            consecutiveHits.put(id, 1);
        }

        if (consecutiveHits.getOrDefault(id, 0) >= 3) {
            activeShield.put(id, new ShieldState(profile, 0.0D));
            shieldLastLocation.put(id, player.getLocation().clone());
            consecutiveHits.put(id, 0);
            lastHitProfile.remove(id);
            playShieldActivation(player);
            blockDamage(player, event);
        }
    }

    @Override
    public void onDamage(@NotNull EntityDamageEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID id = player.getUniqueId();
        ShieldState shield = activeShield.get(id);
        if (shield == null) {
            return;
        }
        String profile = profileFromCause(event.getCause());
        if (shield.profile().equals(profile)) {
            blockDamage(player, event);
        }
    }

    @Override
    public void onPlayerTick(@NotNull Player player, long tickCounter) {
        UUID id = player.getUniqueId();
        long visualUntil = hitVisualUntil.getOrDefault(id, 0L);
        if (visualUntil > tickCounter && tickCounter % 2L == 0L) {
            spawnBarrierVisual(player.getLocation().clone().add(0.0D, 1.0D, 0.0D));
        } else if (visualUntil <= tickCounter) {
            hitVisualUntil.remove(id);
        }

        ShieldState shield = activeShield.get(id);
        if (shield == null) {
            return;
        }

        ItemStack chestplate = player.getInventory().getChestplate();
        if (plugin().getEnchantLevel(chestplate, EnchantType.BORG_TECHNOLOGY) <= 0) {
            clearState(id);
            return;
        }

        Location previous = shieldLastLocation.get(id);
        Location current = player.getLocation();
        if (previous == null || !previous.getWorld().equals(current.getWorld())) {
            shieldLastLocation.put(id, current.clone());
            return;
        }

        double moved = previous.distance(current);
        if (moved > 0.001D) {
            double total = shield.distanceUsed() + moved;
            activeShield.put(id, new ShieldState(shield.profile(), total));
            shieldLastLocation.put(id, current.clone());
            if (total >= SHIELD_DISTANCE_BLOCKS) {
                breakShield(player);
                clearState(id);
                return;
            }
        }

        player.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 25, 0, true, false, true), true);
        if (tickCounter % 3L == 0L) {
            spawnBarrierVisual(player.getLocation().clone().add(0.0D, 1.0D, 0.0D));
        }
    }

    private void blockDamage(@NotNull Player player, @NotNull EntityDamageEvent event) {
        event.setCancelled(true);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1.0F, 0.8F);
        spawnBarrierVisual(player.getLocation().clone().add(0.0D, 1.0D, 0.0D));
    }

    private void playShieldActivation(@NotNull Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1.0F, 1.3F);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation().clone().add(0.0D, 1.0D, 0.0D), 90, 0.8, 1.1, 0.8, 0.01);
        spawnBarrierVisual(player.getLocation().clone().add(0.0D, 1.0D, 0.0D));
    }

    private void breakShield(@NotNull Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1.2F, 0.6F);
        player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().clone().add(0.0D, 1.0D, 0.0D), 120, 0.9, 1.0, 0.9, 0.01);
    }

    private void spawnBarrierVisual(@NotNull Location center) {
        if (center.getWorld() == null) {
            return;
        }
        for (int i = 0; i < 18; i++) {
            double angle = (Math.PI * 2.0D * i) / 18.0D;
            Vector offset = new Vector(Math.cos(angle) * 1.25D, 0.0D, Math.sin(angle) * 1.25D);
            center.getWorld().spawnParticle(Particle.BLOCK, center.clone().add(offset), 1, 0.0D, 0.0D, 0.0D,
                    Material.BARRIER.createBlockData());
        }
    }

    private String profileFromByEntity(@NotNull EntityDamageByEntityEvent event) {
        String cause = event.getCause().name();
        Entity source = event.getDamager();
        if (source instanceof Projectile projectile) {
            String projectileType = projectile.getType().name();
            Entity shooter = projectile.getShooter() instanceof Entity entity ? entity : null;
            return cause + "|projectile:" + projectileType + "|" + shooterProfile(shooter);
        }
        if (source instanceof Player attacker) {
            ItemStack weapon = attacker.getInventory().getItemInMainHand();
            return cause + "|melee:" + weaponProfile(weapon == null ? Material.AIR : weapon.getType());
        }
        if (source instanceof Mob mob) {
            ItemStack weapon = mob.getEquipment() == null ? null : mob.getEquipment().getItemInMainHand();
            return cause + "|mob:" + weaponProfile(weapon == null ? Material.AIR : weapon.getType());
        }
        return cause + "|entity:" + source.getType().name();
    }

    private String profileFromCause(@NotNull EntityDamageEvent.DamageCause cause) {
        return cause.name() + "|generic";
    }

    private String shooterProfile(Entity shooter) {
        if (shooter instanceof Player player) {
            ItemStack weapon = player.getInventory().getItemInMainHand();
            return "player:" + weaponProfile(weapon == null ? Material.AIR : weapon.getType());
        }
        if (shooter instanceof Mob mob) {
            ItemStack weapon = mob.getEquipment() == null ? null : mob.getEquipment().getItemInMainHand();
            return "mob:" + weaponProfile(weapon == null ? Material.AIR : weapon.getType());
        }
        if (shooter != null) {
            return "entity:" + shooter.getType().name();
        }
        return "unknown";
    }

    private String weaponProfile(@NotNull Material material) {
        if (material == Material.AIR) {
            return "unarmed";
        }
        String name = material.name();
        int split = name.indexOf('_');
        if (split > 0) {
            return name.substring(0, split) + ":" + name.substring(split + 1);
        }
        return name;
    }

    private void clearState(@NotNull UUID id) {
        consecutiveHits.remove(id);
        lastHitProfile.remove(id);
        activeShield.remove(id);
        shieldLastLocation.remove(id);
        hitVisualUntil.remove(id);
    }

    private record ShieldState(@NotNull String profile, double distanceUsed) {
    }
}
