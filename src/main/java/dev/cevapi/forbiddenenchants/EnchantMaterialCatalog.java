package dev.cevapi.forbiddenenchants;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class EnchantMaterialCatalog {
    private EnchantMaterialCatalog() {
    }

    public static @Nullable Material materialIfPresent(@NotNull String name) {
        try {
            return Material.valueOf(name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public static @NotNull List<Material> materialsForDynamicSpear() {
        List<Material> spearMaterials = new ArrayList<>();
        for (Material material : Material.values()) {
            String name = material.name();
            if (name.endsWith("_SPEAR") || name.equals("SPEAR")) {
                spearMaterials.add(material);
            }
        }
        return spearMaterials;
    }

    public static @NotNull List<Material> materialsForSlot(@NotNull ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> {
                List<Material> helmets = new ArrayList<>(List.of(
                        Material.LEATHER_HELMET,
                        Material.CHAINMAIL_HELMET,
                        Material.IRON_HELMET,
                        Material.GOLDEN_HELMET,
                        Material.DIAMOND_HELMET,
                        Material.NETHERITE_HELMET,
                        Material.TURTLE_HELMET
                ));
                Material copperHelmet = materialIfPresent("COPPER_HELMET");
                if (copperHelmet != null) {
                    helmets.add(copperHelmet);
                }
                yield helmets;
            }
            case CHESTPLATE -> List.of(
                    Material.LEATHER_CHESTPLATE,
                    Material.CHAINMAIL_CHESTPLATE,
                    Material.IRON_CHESTPLATE,
                    Material.GOLDEN_CHESTPLATE,
                    Material.DIAMOND_CHESTPLATE,
                    Material.NETHERITE_CHESTPLATE
            );
            case ELYTRA -> List.of(Material.ELYTRA);
            case LEGGINGS -> List.of(
                    Material.LEATHER_LEGGINGS,
                    Material.CHAINMAIL_LEGGINGS,
                    Material.IRON_LEGGINGS,
                    Material.GOLDEN_LEGGINGS,
                    Material.DIAMOND_LEGGINGS,
                    Material.NETHERITE_LEGGINGS
            );
            case BOOTS -> List.of(
                    Material.LEATHER_BOOTS,
                    Material.CHAINMAIL_BOOTS,
                    Material.IRON_BOOTS,
                    Material.GOLDEN_BOOTS,
                    Material.DIAMOND_BOOTS,
                    Material.NETHERITE_BOOTS
            );
            case COMPASS -> List.of(Material.COMPASS);
            case ARMOR -> {
                List<Material> armor = new ArrayList<>(List.of(
                        Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET, Material.GOLDEN_HELMET,
                        Material.DIAMOND_HELMET, Material.NETHERITE_HELMET, Material.TURTLE_HELMET,
                        Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE, Material.GOLDEN_CHESTPLATE,
                        Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE, Material.ELYTRA,
                        Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS, Material.GOLDEN_LEGGINGS,
                        Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS,
                        Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS, Material.GOLDEN_BOOTS,
                        Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS
                ));
                Material copperHelmet = materialIfPresent("COPPER_HELMET");
                if (copperHelmet != null) {
                    armor.add(copperHelmet);
                }
                yield armor;
            }
            case SWORD -> List.of(
                    Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
                    Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
            );
            case RANGED -> List.of(Material.BOW, Material.CROSSBOW);
            case TRIDENT -> List.of(Material.TRIDENT);
            case SPEAR -> {
                List<Material> spearMaterials = materialsForDynamicSpear();
                if (!spearMaterials.isEmpty()) {
                    List<Material> out = new ArrayList<>();
                    out.add(Material.TRIDENT);
                    for (Material material : spearMaterials) {
                        if (material != Material.TRIDENT) {
                            out.add(material);
                        }
                    }
                    yield out;
                }
                yield List.of(Material.TRIDENT);
            }
            case HOE -> List.of(
                    Material.WOODEN_HOE, Material.STONE_HOE, Material.IRON_HOE,
                    Material.GOLDEN_HOE, Material.DIAMOND_HOE, Material.NETHERITE_HOE
            );
            case AXE -> List.of(
                    Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
                    Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
            );
            case MACE -> List.of(Material.MACE);
            case BRUSH -> List.of(Material.BRUSH);
            case ROD -> {
                List<Material> rods = new ArrayList<>();
                Material breezeRod = materialIfPresent("BREEZE_ROD");
                if (breezeRod != null) {
                    rods.add(breezeRod);
                }
                rods.add(Material.BLAZE_ROD);
                yield rods;
            }
            case NAMETAG -> List.of(Material.NAME_TAG);
            case LEAD -> List.of(Material.LEAD);
            case SHIELD -> List.of(Material.SHIELD);
            case TOTEM -> List.of(Material.TOTEM_OF_UNDYING);
            case POTION -> List.of(Material.POTION);
        };
    }

    public static @NotNull List<String> materialSuggestions(@NotNull ArmorSlot slot) {
        List<String> suggestions = new ArrayList<>();
        for (Material material : materialsForSlot(slot)) {
            suggestions.add(material.name().toLowerCase(Locale.ROOT));
        }
        return suggestions;
    }

    public static @NotNull List<String> materialSuggestions(@NotNull EnchantType type) {
        if (type == EnchantType.DRAGONS_BREATH || type == EnchantType.EXPLOSIVE_REACTION) {
            return List.of(Material.CROSSBOW.name().toLowerCase(Locale.ROOT));
        }
        if (type == EnchantType.WITHERING_STRIKE) {
            return List.of(Material.TRIDENT.name().toLowerCase(Locale.ROOT));
        }
        if (type == EnchantType.MAGNETISM) {
            return weaponAndToolMaterialSuggestions();
        }
        if (type == EnchantType.CURSED_MAGNETISM) {
            return weaponMaterialSuggestions();
        }
        if (type == EnchantType.FIREBALL) {
            return List.of(Material.BLAZE_ROD.name().toLowerCase(Locale.ROOT));
        }
        if (type == EnchantType.VOID_STICK) {
            Material breezeRod = materialIfPresent("BREEZE_ROD");
            if (breezeRod != null) {
                return List.of(breezeRod.name().toLowerCase(Locale.ROOT));
            }
            return List.of();
        }
        return materialSuggestions(type.slot);
    }

    public static @NotNull List<String> allEnchantableMaterialSuggestions() {
        List<String> suggestions = new ArrayList<>();
        for (ArmorSlot slot : ArmorSlot.values()) {
            if (slot == ArmorSlot.ARMOR) {
                continue;
            }
            suggestions.addAll(materialSuggestions(slot));
        }
        return suggestions;
    }

    public static @NotNull String requiredMaterialCategory(@NotNull ArmorSlot slot) {
        return switch (slot) {
            case HELMET -> "a helmet";
            case CHESTPLATE -> "a chestplate";
            case ELYTRA -> "an elytra";
            case LEGGINGS -> "leggings";
            case BOOTS -> "boots";
            case ARMOR -> "any armor piece";
            case COMPASS -> "a compass";
            case SWORD -> "a sword";
            case RANGED -> "a bow or crossbow";
            case TRIDENT -> "a trident";
            case SPEAR -> "a spear";
            case HOE -> "a hoe";
            case AXE -> "an axe";
            case MACE -> "a mace";
            case BRUSH -> "a brush";
            case ROD -> "a rod";
            case NAMETAG -> "a name tag";
            case LEAD -> "a lead";
            case SHIELD -> "a shield";
            case TOTEM -> "a totem of undying";
            case POTION -> "a water bottle";
        };
    }

    public static @NotNull String requiredMaterialCategory(@NotNull EnchantType type) {
        if (type == EnchantType.DRAGONS_BREATH || type == EnchantType.EXPLOSIVE_REACTION) {
            return "a crossbow";
        }
        if (type == EnchantType.WITHERING_STRIKE) {
            return "a trident";
        }
        if (type == EnchantType.MAGNETISM) {
            return "a weapon or tool";
        }
        if (type == EnchantType.CURSED_MAGNETISM) {
            return "a weapon";
        }
        if (type == EnchantType.FIREBALL) {
            return "a blaze rod";
        }
        if (type == EnchantType.VOID_STICK) {
            return "a breeze rod";
        }
        return requiredMaterialCategory(type.slot);
    }

    private static @NotNull List<String> weaponAndToolMaterialSuggestions() {
        List<String> out = new ArrayList<>();
        for (Material material : Material.values()) {
            String name = material.name();
            if (name.endsWith("_SWORD")
                    || name.endsWith("_AXE")
                    || name.endsWith("_PICKAXE")
                    || name.endsWith("_SHOVEL")
                    || name.endsWith("_HOE")
                    || name.endsWith("_SPEAR")
                    || name.equals("SPEAR")
                    || material == Material.BOW
                    || material == Material.CROSSBOW
                    || material == Material.TRIDENT
                    || material == Material.MACE) {
                out.add(name.toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    private static @NotNull List<String> weaponMaterialSuggestions() {
        List<String> out = new ArrayList<>();
        for (Material material : Material.values()) {
            String name = material.name();
            if (name.endsWith("_SWORD")
                    || name.endsWith("_AXE")
                    || name.endsWith("_SPEAR")
                    || name.equals("SPEAR")
                    || material == Material.BOW
                    || material == Material.CROSSBOW
                    || material == Material.TRIDENT
                    || material == Material.MACE) {
                out.add(name.toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }
}

