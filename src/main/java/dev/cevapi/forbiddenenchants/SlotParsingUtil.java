package dev.cevapi.forbiddenenchants;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class SlotParsingUtil {
    private SlotParsingUtil() {
    }

    public static @Nullable Material parseMaterial(@NotNull String arg) {
        Material material = Material.matchMaterial(arg);
        if (material != null) {
            return material;
        }
        String normalized = arg.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        return Material.matchMaterial(normalized);
    }

    public static @Nullable ArmorSlot parseSlotArg(@NotNull String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        return switch (normalized) {
            case "helmet", "head" -> ArmorSlot.HELMET;
            case "chest", "chestplate", "body" -> ArmorSlot.CHESTPLATE;
            case "elytra", "wings" -> ArmorSlot.ELYTRA;
            case "leggings", "legs", "pants" -> ArmorSlot.LEGGINGS;
            case "boots", "feet", "shoes" -> ArmorSlot.BOOTS;
            case "armor", "armour", "anyarmor", "any_armor" -> ArmorSlot.ARMOR;
            case "compass", "lodestone" -> ArmorSlot.COMPASS;
            case "sword" -> ArmorSlot.SWORD;
            case "ranged", "bow", "crossbow" -> ArmorSlot.RANGED;
            case "trident" -> ArmorSlot.TRIDENT;
            case "spear" -> ArmorSlot.SPEAR;
            case "hoe" -> ArmorSlot.HOE;
            case "axe" -> ArmorSlot.AXE;
            case "mace" -> ArmorSlot.MACE;
            case "brush" -> ArmorSlot.BRUSH;
            case "nametag", "name_tag", "name-tag" -> ArmorSlot.NAMETAG;
            case "lead", "leash" -> ArmorSlot.LEAD;
            case "shield" -> ArmorSlot.SHIELD;
            case "totem", "totem_of_undying" -> ArmorSlot.TOTEM;
            case "potion", "spell_potion", "spell" -> ArmorSlot.POTION;
            default -> null;
        };
    }

    public static @Nullable ArmorSlot resolveSlotForMaterial(@NotNull Material material) {
        if (material == Material.TRIDENT) {
            return ArmorSlot.TRIDENT;
        }
        for (ArmorSlot slot : ArmorSlot.values()) {
            if (slot == ArmorSlot.ARMOR) {
                continue;
            }
            if (EnchantMaterialCatalog.materialsForSlot(slot).contains(material)) {
                return slot;
            }
        }
        return null;
    }

    public static @NotNull String slotName(@NotNull ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> "helmet";
            case CHESTPLATE -> "chestplate";
            case ELYTRA -> "elytra";
            case LEGGINGS -> "leggings";
            case BOOTS -> "boots";
            case ARMOR -> "armor";
            case COMPASS -> "compass";
            case SWORD -> "sword";
            case RANGED -> "ranged";
            case TRIDENT -> "trident";
            case SPEAR -> "spear";
            case HOE -> "hoe";
            case AXE -> "axe";
            case MACE -> "mace";
            case BRUSH -> "brush";
            case NAMETAG -> "name tag";
            case LEAD -> "lead";
            case SHIELD -> "shield";
            case TOTEM -> "totem";
            case POTION -> "potion";
        };
    }
}

