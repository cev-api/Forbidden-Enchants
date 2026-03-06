package dev.cevapi.forbiddenenchants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

enum InjectorMysteryState {
    ALL("all", "All"),
    MYSTERY_ONLY("mystery_only", "Mystery only"),
    NON_MYSTERY_ONLY("non_mystery_only", "Non-mystery only");

    private final String id;
    private final String displayName;

    InjectorMysteryState(@NotNull String id, @NotNull String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    @NotNull String id() {
        return id;
    }

    @NotNull String displayName() {
        return displayName;
    }

    @NotNull InjectorMysteryState next() {
        InjectorMysteryState[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    static @Nullable InjectorMysteryState fromString(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = normalize(input);
        for (InjectorMysteryState state : values()) {
            if (state.id.equals(normalized)) {
                return state;
            }
        }
        return switch (normalized) {
            case "all", "any" -> ALL;
            case "mystery", "hidden" -> MYSTERY_ONLY;
            case "non_mystery", "not_mystery", "normal", "known", "revealed" -> NON_MYSTERY_ONLY;
            default -> null;
        };
    }

    static @Nullable InjectorMysteryState fromLegacyModeAlias(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = normalize(input);
        return switch (normalized) {
            case "mystery_book_only", "mystery_item_only", "mystery_only", "book_mystery", "item_mystery", "all_mystery", "mystery_all" -> MYSTERY_ONLY;
            default -> null;
        };
    }

    private static @NotNull String normalize(@NotNull String input) {
        return input.toLowerCase(Locale.ROOT).trim().replace('-', '_').replace(' ', '_');
    }
}

