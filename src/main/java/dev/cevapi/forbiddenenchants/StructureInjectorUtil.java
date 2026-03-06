package dev.cevapi.forbiddenenchants;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.generator.structure.Structure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class StructureInjectorUtil {
    private StructureInjectorUtil() {
    }

    public static @Nullable Double parseChancePercent(@NotNull String input) {
        try {
            return clampChance(Double.parseDouble(input));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static @NotNull String formatPercent(double value) {
        return String.format(Locale.ROOT, "%.1f%%", value);
    }

    public static @Nullable Structure parseStructure(@NotNull String raw) {
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
        if (normalized.isBlank()) {
            return null;
        }
        try {
            NamespacedKey direct = NamespacedKey.fromString(normalized);
            if (direct != null) {
                Structure directStructure = Registry.STRUCTURE.get(direct);
                if (directStructure != null) {
                    return directStructure;
                }
            }
            NamespacedKey minecraftKey = NamespacedKey.fromString("minecraft:" + normalized);
            return minecraftKey == null ? null : Registry.STRUCTURE.get(minecraftKey);
        } catch (Throwable t) {
            return null;
        }
    }

    public static @NotNull List<Structure> parseStructureList(@NotNull String csv) {
        List<Structure> parsed = new ArrayList<>();
        String[] tokens = csv.split(",");
        for (String token : tokens) {
            Structure structure = parseStructure(token);
            if (structure != null && !parsed.contains(structure)) {
                parsed.add(structure);
            }
        }
        return parsed;
    }

    private static double clampChance(double value) {
        if (value < 0.0D) {
            return 0.0D;
        }
        if (value > 100.0D) {
            return 100.0D;
        }
        return value;
    }
}

