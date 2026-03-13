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

public final class SonicPanicEnchant extends BaseForbiddenEnchant {
    private final Map<UUID, Long> cooldownByPlayer = new HashMap<>();

    public SonicPanicEnchant() {
        super("sonic_panic",
                "sonic_panic_level",
                "Sonic Panic",
                ArmorSlot.SWORD,
                1,
                NamedTextColor.DARK_AQUA,
                List.of("sonic", "panic", "sonicpanic"),
                null);
    }

    @Override
    public @NotNull String effectDescription(int level) {
        return "At 50% health or lower, emits a warden-strength radial sonic blast and shatters the sword.";
    }

    public boolean shouldTrigger(int level, double postDamageHealth, double maxHealth, boolean sword) {
        return sword && level > 0 && postDamageHealth <= maxHealth * 0.50D;
    }

    public long cooldownTicks() {
        return 200L;
    }

    public boolean canTriggerNow(int level,
                                 double postDamageHealth,
                                 double maxHealth,
                                 boolean sword,
                                 long tickCounter,
                                 long readyTick) {
        return shouldTrigger(level, postDamageHealth, maxHealth, sword) && tickCounter >= readyTick;
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
        ItemStack weapon = player.getInventory().getItemInMainHand();
        int level = plugin().getEnchantLevel(weapon, EnchantType.SONIC_PANIC);
        long ready = cooldownByPlayer.getOrDefault(player.getUniqueId(), 0L);
        double postDamageHealth = player.getHealth() - event.getFinalDamage();
        boolean sword = weapon != null && weapon.getType().name().endsWith("_SWORD");
        if (!canTriggerNow(level, postDamageHealth, maxHealth, sword, tickCounter, ready)) {
            return;
        }
        plugin().triggerSonicPanic(player, weapon);
        cooldownByPlayer.put(player.getUniqueId(), tickCounter + cooldownTicks());
    }
}

