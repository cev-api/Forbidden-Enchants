package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.EnchantType;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PocketDimensionEnchant extends BaseForbiddenEnchant {
    private final Map<UUID, Long> cooldownByPlayer = new HashMap<>();

    public PocketDimensionEnchant() {
        super("pocket_dimension",
                "pocket_dimension_level",
                "Pocket Dimension",
                ArmorSlot.LEGGINGS,
                1,
                NamedTextColor.LIGHT_PURPLE,
                List.of("pocket", "dimension", "pocketdimension", "pocket_dimension"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "At 5% health or lower, teleports you ~50 blocks to safety, then leggings break.";
    }

    public boolean shouldTrigger(int level, double postDamageHealth, double maxHealth) {
        return level > 0 && postDamageHealth <= maxHealth * 0.05D;
    }

    public long cooldownTicks() {
        return 200L;
    }

    public boolean canTriggerNow(int level,
                                 double postDamageHealth,
                                 double maxHealth,
                                 long tickCounter,
                                 long readyTick) {
        return shouldTrigger(level, postDamageHealth, maxHealth) && tickCounter >= readyTick;
    }

    @Override
    public void onDamage(@NotNull EntityDamageEvent event, long tickCounter) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        double maxHealth = player.getAttribute(Attribute.MAX_HEALTH) == null
                ? 20.0D
                : player.getAttribute(Attribute.MAX_HEALTH).getValue();
        if (maxHealth <= 0.0D) {
            return;
        }
        ItemStack leggings = player.getInventory().getLeggings();
        int level = plugin().getEnchantLevel(leggings, EnchantType.POCKET_DIMENSION);
        long ready = cooldownByPlayer.getOrDefault(player.getUniqueId(), 0L);
        double postDamageHealth = player.getHealth() - event.getFinalDamage();
        if (!canTriggerNow(level, postDamageHealth, maxHealth, tickCounter, ready)) {
            return;
        }
        if (!plugin().triggerPocketDimension(player)) {
            return;
        }
        cooldownByPlayer.put(player.getUniqueId(), tickCounter + cooldownTicks());
        event.setCancelled(true);
    }
}

