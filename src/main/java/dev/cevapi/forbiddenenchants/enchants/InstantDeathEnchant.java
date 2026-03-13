package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class InstantDeathEnchant extends BaseForbiddenEnchant {
    private static final float EXPLOSION_POWER = 20.0F; // Equivalent to ~5 TNT blasts.

    private final Map<UUID, Long> directHitNoTotemUntil = new HashMap<>();

    public InstantDeathEnchant() {
        super("instant_death",
                "instant_death_level",
                "Instant Death",
                ArmorSlot.TRIDENT,
                1,
                NamedTextColor.DARK_RED,
                List.of("instant_death", "instantdeath", "death_trident"),
                "Apply to a trident in an anvil.");
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "Thrown trident instantly kills direct-hit targets (totems fail + drop), then detonates with 5x TNT force and disappears.";
    }

    @Override
    public void onDamageByEntity(@NotNull EntityDamageByEntityEvent event, long tickCounter) {
        if (!(event.getDamager() instanceof Trident trident)) {
            return;
        }
        ItemStack thrown = trident.getItem();
        plugin().revealMysteryItemIfNeeded(thrown, null, null);
        trident.setItem(thrown);
        int level = plugin().getEnchantLevel(thrown, EnchantType.INSTANT_DEATH);
        if (level <= 0) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (target instanceof Player playerTarget) {
            dropAllTotems(playerTarget);
            directHitNoTotemUntil.put(playerTarget.getUniqueId(), tickCounter + 60L);
        }

        target.setNoDamageTicks(0);
        target.damage(2048.0D, trident.getShooter() instanceof Entity shooter ? shooter : trident);

        explodeAndConsumeTrident(trident, trident.getLocation().clone());
    }

    @Override
    public void onProjectileHit(@NotNull ProjectileHitEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Trident trident)) {
            return;
        }
        ItemStack thrown = trident.getItem();
        int level = plugin().getEnchantLevel(thrown, EnchantType.INSTANT_DEATH);
        if (level <= 0 || isResolved(trident)) {
            return;
        }
        explodeAndConsumeTrident(trident, trident.getLocation().clone());
    }

    @Override
    public void onTotemPop(@NotNull EntityResurrectEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        UUID playerId = player.getUniqueId();
        long until = directHitNoTotemUntil.getOrDefault(playerId, 0L);
        if (tickCounter > until) {
            directHitNoTotemUntil.remove(playerId);
            return;
        }
        event.setCancelled(true);
    }

    private void explodeAndConsumeTrident(@NotNull Trident trident, @NotNull Location center) {
        if (isResolved(trident)) {
            return;
        }
        markResolved(trident);

        World world = center.getWorld();
        if (world != null) {
            Entity source = trident.getShooter() instanceof Entity shooter ? shooter : trident;
            world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5F, 0.7F);
            world.createExplosion(source, center, EXPLOSION_POWER, false, true);
        }
        trident.remove();
    }

    private void dropAllTotems(@NotNull Player player) {
        ItemStack[] contents = player.getInventory().getContents();
        for (int slot = 0; slot < contents.length; slot++) {
            ItemStack stack = contents[slot];
            if (stack == null || stack.getType() != Material.TOTEM_OF_UNDYING || stack.getAmount() <= 0) {
                continue;
            }
            ItemStack drop = stack.clone();
            player.getInventory().setItem(slot, new ItemStack(Material.AIR));
            player.getWorld().dropItemNaturally(player.getLocation(), drop);
        }
    }

    private void markResolved(@NotNull Trident trident) {
        trident.getPersistentDataContainer().set(resolvedKey(), PersistentDataType.BYTE, (byte) 1);
    }

    private boolean isResolved(@NotNull Trident trident) {
        return trident.getPersistentDataContainer().has(resolvedKey(), PersistentDataType.BYTE);
    }

    private @NotNull NamespacedKey resolvedKey() {
        return new NamespacedKey(plugin(), "instant_death_resolved");
    }
}
