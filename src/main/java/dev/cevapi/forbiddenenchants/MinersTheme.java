package dev.cevapi.forbiddenenchants;

import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

enum MinersTheme {
    TURTLE {
        @Override
        boolean matches(@NotNull Material material) {
            return material == Material.SPAWNER;
        }
    },
    DIAMOND {
        @Override
        boolean matches(@NotNull Material material) {
            return material == Material.DIAMOND_ORE || material == Material.DEEPSLATE_DIAMOND_ORE;
        }
    },
    NETHERITE {
        @Override
        boolean matches(@NotNull Material material) {
            return material == Material.ANCIENT_DEBRIS;
        }
    },
    COPPER {
        @Override
        boolean matches(@NotNull Material material) {
            return material == Material.COPPER_ORE || material == Material.DEEPSLATE_COPPER_ORE;
        }
    },
    IRON {
        @Override
        boolean matches(@NotNull Material material) {
            return material == Material.IRON_ORE || material == Material.DEEPSLATE_IRON_ORE;
        }
    },
    LEATHER {
        @Override
        boolean matches(@NotNull Material material) {
            return material == Material.COAL_ORE || material == Material.DEEPSLATE_COAL_ORE;
        }
    },
    GOLD {
        @Override
        boolean matches(@NotNull Material material) {
            return material == Material.GOLD_ORE
                    || material == Material.DEEPSLATE_GOLD_ORE
                    || material == Material.NETHER_GOLD_ORE;
        }
    },
    CHAINMAIL {
        @Override
        boolean matches(@NotNull Material material) {
            return material == Material.EMERALD_ORE || material == Material.DEEPSLATE_EMERALD_ORE;
        }
    };

    abstract boolean matches(@NotNull Material material);
}

