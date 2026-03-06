package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class DisplayNameUtil {
    private DisplayNameUtil() {
    }

    public static @NotNull String toDisplayName(@NotNull Material material) {
        return toDisplayName(material.name());
    }

    public static @NotNull String toDisplayName(@NotNull EntityType type) {
        return toDisplayName(type.name());
    }

    public static @NotNull String actionBarEntityName(@NotNull Entity entity) {
        Component custom = entity.customName();
        if (custom != null) {
            String plain = PlainTextComponentSerializer.plainText().serialize(custom).trim();
            if (!plain.isBlank()) {
                return plain;
            }
        }
        return toDisplayName(entity.getType());
    }

    public static @NotNull String toDisplayName(@NotNull String key) {
        String[] parts = key.toLowerCase(Locale.ROOT).split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.isEmpty() ? key : builder.toString();
    }
}

