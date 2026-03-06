package dev.cevapi.forbiddenenchants;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class ItemClassificationService {
    private final ForbiddenEnchantsPlugin plugin;

    ItemClassificationService(@NotNull ForbiddenEnchantsPlugin plugin) {
        this.plugin = plugin;
    }

    boolean isArmorPieceForSlot(@Nullable ItemStack stack, @NotNull ArmorSlot slot) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }

        String name = stack.getType().name();
        return switch (slot) {
            case HELMET -> name.endsWith("_HELMET") || stack.getType() == Material.TURTLE_HELMET;
            case CHESTPLATE -> name.endsWith("_CHESTPLATE");
            case ELYTRA -> stack.getType() == Material.ELYTRA;
            case LEGGINGS -> name.endsWith("_LEGGINGS");
            case BOOTS -> name.endsWith("_BOOTS");
            case ARMOR -> name.endsWith("_HELMET")
                    || stack.getType() == Material.TURTLE_HELMET
                    || name.endsWith("_CHESTPLATE")
                    || stack.getType() == Material.ELYTRA
                    || name.endsWith("_LEGGINGS")
                    || name.endsWith("_BOOTS");
            case COMPASS -> stack.getType() == Material.COMPASS;
            case SWORD -> name.endsWith("_SWORD");
            case RANGED -> isRangedWeapon(stack);
            case TRIDENT -> isTrident(stack);
            case SPEAR -> isSpear(stack);
            case HOE -> isHoe(stack);
            case AXE -> isAxe(stack);
            case MACE -> isMace(stack);
            case NAMETAG -> isNameTag(stack);
            case LEAD -> isLead(stack);
            case SHIELD -> isShield(stack);
            case TOTEM -> isTotem(stack);
        };
    }

    boolean isSword(@Nullable ItemStack stack) {
        return stack != null
                && stack.getType() != Material.AIR
                && stack.getType().name().endsWith("_SWORD");
    }

    boolean isRangedWeapon(@Nullable ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        return stack.getType() == Material.BOW || stack.getType() == Material.CROSSBOW;
    }

    boolean isSpear(@Nullable ItemStack stack) {
        if (stack == null || stack.getType() == Material.AIR) {
            return false;
        }
        String name = stack.getType().name();
        if (name.equals("SPEAR") || name.endsWith("_SPEAR")) {
            return true;
        }
        List<Material> spearMaterials = EnchantMaterialCatalog.materialsForDynamicSpear();
        if (!spearMaterials.isEmpty()) {
            return spearMaterials.contains(stack.getType());
        }
        // Fallback for builds without spear materials.
        return isTrident(stack);
    }

    boolean isHoe(@Nullable ItemStack stack) {
        return stack != null
                && stack.getType() != Material.AIR
                && stack.getType().name().endsWith("_HOE");
    }

    boolean isMaterialValidForEnchant(@NotNull Material material, @NotNull EnchantType type) {
        if (plugin.isRetiredEnchant(type)) {
            return false;
        }
        if ((type == EnchantType.DRAGONS_BREATH || type == EnchantType.EXPLOSIVE_REACTION) && material != Material.CROSSBOW) {
            return false;
        }
        if (type == EnchantType.WITHERING_STRIKE) {
            return material == Material.TRIDENT;
        }

        ItemStack probe = new ItemStack(material);
        return switch (type.slot) {
            case HELMET, CHESTPLATE, ELYTRA, LEGGINGS, BOOTS, ARMOR, COMPASS, RANGED, TRIDENT, SPEAR, HOE, AXE, MACE, NAMETAG, LEAD, SHIELD, TOTEM ->
                    isArmorPieceForSlot(probe, type.slot);
            case SWORD -> isSword(probe);
        };
    }

    private boolean isTrident(@Nullable ItemStack stack) {
        return stack != null && stack.getType() == Material.TRIDENT;
    }

    private boolean isNameTag(@Nullable ItemStack stack) {
        return stack != null && stack.getType() == Material.NAME_TAG;
    }

    private boolean isLead(@Nullable ItemStack stack) {
        return stack != null && stack.getType() == Material.LEAD;
    }

    private boolean isShield(@Nullable ItemStack stack) {
        return stack != null && stack.getType() == Material.SHIELD;
    }

    private boolean isTotem(@Nullable ItemStack stack) {
        return stack != null && stack.getType() == Material.TOTEM_OF_UNDYING;
    }

    private boolean isAxe(@Nullable ItemStack stack) {
        return stack != null && stack.getType() != Material.AIR && stack.getType().name().endsWith("_AXE");
    }

    private boolean isMace(@Nullable ItemStack stack) {
        return stack != null && stack.getType() == Material.MACE;
    }
}

