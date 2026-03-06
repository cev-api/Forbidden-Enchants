package dev.cevapi.forbiddenenchants;

import org.jetbrains.annotations.NotNull;

public final class RomanNumeralUtil {
    private RomanNumeralUtil() {
    }

    public static @NotNull String toRoman(int number) {
        return switch (number) {
            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> String.valueOf(number);
        };
    }
}

