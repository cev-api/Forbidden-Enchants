package dev.cevapi.forbiddenenchants;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

final class LoreWrapUtil {
    private LoreWrapUtil() {
    }

    static @NotNull List<Component> wrap(@NotNull String text, @NotNull NamedTextColor color, int maxCharsPerLine) {
        List<Component> lines = new ArrayList<>();
        if (text.isBlank()) {
            return lines;
        }

        int max = Math.max(12, maxCharsPerLine);
        String[] words = text.trim().split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.length() > max && line.length() == 0) {
                int start = 0;
                while (start < word.length()) {
                    int end = Math.min(word.length(), start + max);
                    lines.add(Component.text(word.substring(start, end), color));
                    start = end;
                }
                continue;
            }
            if (line.length() == 0) {
                line.append(word);
                continue;
            }
            if (line.length() + 1 + word.length() <= max) {
                line.append(' ').append(word);
                continue;
            }
            lines.add(Component.text(line.toString(), color));
            line.setLength(0);
            line.append(word);
        }
        if (line.length() > 0) {
            lines.add(Component.text(line.toString(), color));
        }
        return lines;
    }
}


