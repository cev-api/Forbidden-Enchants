package dev.cevapi.forbiddenenchants.enchants;

import dev.cevapi.forbiddenenchants.ArmorSlot;
import dev.cevapi.forbiddenenchants.ForbiddenEnchantsPlugin;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public abstract class BaseForbiddenEnchant implements ForbiddenEnchantDefinition, ForbiddenEnchantRuntime {
    private final String arg;
    private final String pdcKey;
    private final String displayName;
    private final ArmorSlot slot;
    private final int maxLevel;
    private final NamedTextColor color;
    private final List<String> aliases;
    private final String slotDescriptionOverride;
    private @Nullable ForbiddenEnchantsPlugin plugin;

    protected BaseForbiddenEnchant(@NotNull String arg,
                                   @NotNull String pdcKey,
                                   @NotNull String displayName,
                                   @NotNull ArmorSlot slot,
                                   int maxLevel,
                                   @NotNull NamedTextColor color,
                                   @NotNull List<String> aliases,
                                   String slotDescriptionOverride) {
        this.arg = arg;
        this.pdcKey = pdcKey;
        this.displayName = displayName;
        this.slot = slot;
        this.maxLevel = maxLevel;
        this.color = color;
        this.aliases = List.copyOf(aliases);
        this.slotDescriptionOverride = slotDescriptionOverride;
    }

    final void bindPlugin(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    protected final @NotNull ForbiddenEnchantsPlugin plugin() {
        if (plugin == null) {
            throw new IllegalStateException("Enchant runtime context is not bound yet.");
        }
        return plugin;
    }

    @Override
    public final @NotNull String arg() {
        return arg;
    }

    @Override
    public final @NotNull String pdcKey() {
        return pdcKey;
    }

    @Override
    public final @NotNull String displayName() {
        return displayName;
    }

    @Override
    public final @NotNull ArmorSlot slot() {
        return slot;
    }

    @Override
    public final int maxLevel() {
        return maxLevel;
    }

    @Override
    public final @NotNull NamedTextColor color() {
        return color;
    }

    @Override
    public final @NotNull List<String> aliases() {
        return aliases;
    }

    @Override
    public final @NotNull String slotDescription() {
        if (slotDescriptionOverride != null) {
            return slotDescriptionOverride;
        }

        return switch (slot) {
            case HELMET -> "Apply to any helmet in an anvil.";
            case CHESTPLATE -> "Apply to any chestplate in an anvil.";
            case ELYTRA -> "Apply to elytra in an anvil.";
            case LEGGINGS -> "Apply to any leggings in an anvil.";
            case BOOTS -> "Apply to any boots in an anvil.";
            case ARMOR -> "Apply to any armor piece in an anvil.";
            case COMPASS -> "Apply to a compass in an anvil.";
            case SWORD -> "Apply to any sword in an anvil.";
            case RANGED -> "Apply to bow or crossbow in an anvil.";
            case TRIDENT -> "Apply to a trident in an anvil.";
            case SPEAR -> "Apply to spear weapons in an anvil.";
            case HOE -> "Apply to any hoe in an anvil.";
            case AXE -> "Apply to any axe in an anvil.";
            case MACE -> "Apply to a mace in an anvil.";
            case BRUSH -> "Apply to a brush in an anvil.";
            case ROD -> "Apply to a rod in an anvil.";
            case NAMETAG -> "Apply to a name tag in an anvil.";
            case LEAD -> "Apply to a lead in an anvil.";
            case SHIELD -> "Apply to a shield in an anvil.";
            case TOTEM -> "Apply to a totem of undying in an anvil.";
            case POTION -> "Apply to a water bottle in an anvil to craft a spell potion.";
        };
    }
}

