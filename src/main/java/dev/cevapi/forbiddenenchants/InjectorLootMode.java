package dev.cevapi.forbiddenenchants;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

enum InjectorLootMode {
    BOOK_ONLY("book_only", "Book only", LootType.BOOKS, CurseState.ALL),
    UNCURSED_BOOK_ONLY("uncursed_book_only", "Uncursed book only", LootType.BOOKS, CurseState.UNCURSED),
    CURSED_BOOK_ONLY("cursed_book_only", "Cursed book only", LootType.BOOKS, CurseState.CURSED),
    CURSED_ITEM_ONLY("cursed_item_only", "Cursed item only", LootType.ITEMS, CurseState.CURSED),
    ITEM_ONLY("item_only", "Item only", LootType.ITEMS, CurseState.ALL),
    UNCURSED_ITEM_ONLY("uncursed_item_only", "Uncursed item only", LootType.ITEMS, CurseState.UNCURSED),
    ALL("all", "All", LootType.ALL, CurseState.ALL),
    CURSED_ONLY("cursed_only", "Cursed only", LootType.ALL, CurseState.CURSED),
    UNCURSED_ONLY("uncursed_only", "Uncursed only", LootType.ALL, CurseState.UNCURSED);

    private final String id;
    private final String displayName;
    private final LootType type;
    private final CurseState curseState;

    InjectorLootMode(@NotNull String id,
                     @NotNull String displayName,
                     @NotNull LootType type,
                     @NotNull CurseState curseState) {
        this.id = id;
        this.displayName = displayName;
        this.type = type;
        this.curseState = curseState;
    }

    @NotNull String id() {
        return id;
    }

    @NotNull String displayName() {
        return displayName;
    }

    @NotNull LootType type() {
        return type;
    }

    @NotNull CurseState curseState() {
        return curseState;
    }

    @NotNull InjectorLootMode nextType() {
        return fromParts(type.next(), curseState);
    }

    @NotNull InjectorLootMode nextCurse() {
        return fromParts(type, curseState.next());
    }

    static @NotNull InjectorLootMode fromParts(@NotNull LootType type, @NotNull CurseState curseState) {
        for (InjectorLootMode mode : values()) {
            if (mode.type == type && mode.curseState == curseState) {
                return mode;
            }
        }
        return ALL;
    }

    static @Nullable InjectorLootMode fromString(@Nullable String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String normalized = normalize(input);
        for (InjectorLootMode mode : values()) {
            if (mode.id.equals(normalized)) {
                return mode;
            }
        }
        return switch (normalized) {
            case "book" -> BOOK_ONLY;
            case "uncursed_book", "book_uncursed" -> UNCURSED_BOOK_ONLY;
            case "cursed_book", "book_cursed" -> CURSED_BOOK_ONLY;
            case "mystery_book", "book_mystery" -> BOOK_ONLY; // legacy alias (set mystery state separately)
            case "cursed_item", "item_cursed" -> CURSED_ITEM_ONLY;
            case "item" -> ITEM_ONLY;
            case "uncursed_item", "item_uncursed" -> UNCURSED_ITEM_ONLY;
            case "mystery_item", "item_mystery" -> ITEM_ONLY; // legacy alias (set mystery state separately)
            case "cursed", "all_cursed", "cursed_all" -> CURSED_ONLY;
            case "uncursed", "all_uncursed", "uncursed_all" -> UNCURSED_ONLY;
            case "mystery", "all_mystery", "mystery_all" -> ALL; // legacy alias (set mystery state separately)
            default -> null;
        };
    }

    private static @NotNull String normalize(@NotNull String input) {
        return input.toLowerCase(Locale.ROOT).trim().replace('-', '_').replace(' ', '_');
    }

    enum LootType {
        BOOKS("books", "Books"),
        ITEMS("items", "Items"),
        ALL("all", "All");

        private final String id;
        private final String displayName;

        LootType(@NotNull String id, @NotNull String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        @NotNull String id() {
            return id;
        }

        @NotNull String displayName() {
            return displayName;
        }

        @NotNull LootType next() {
            LootType[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        static @Nullable LootType fromString(@Nullable String input) {
            if (input == null || input.isBlank()) {
                return null;
            }
            String normalized = normalize(input);
            return switch (normalized) {
                case "books", "book" -> BOOKS;
                case "items", "item" -> ITEMS;
                case "all", "any" -> ALL;
                default -> null;
            };
        }
    }

    enum CurseState {
        ALL("all", "All"),
        CURSED("cursed", "Cursed"),
        UNCURSED("uncursed", "Uncursed");

        private final String id;
        private final String displayName;

        CurseState(@NotNull String id, @NotNull String displayName) {
            this.id = id;
            this.displayName = displayName;
        }

        @NotNull String id() {
            return id;
        }

        @NotNull String displayName() {
            return displayName;
        }

        @NotNull CurseState next() {
            CurseState[] values = values();
            return values[(ordinal() + 1) % values.length];
        }

        static @Nullable CurseState fromString(@Nullable String input) {
            if (input == null || input.isBlank()) {
                return null;
            }
            String normalized = normalize(input);
            return switch (normalized) {
                case "all", "any" -> ALL;
                case "cursed", "curse" -> CURSED;
                case "uncursed", "no_curse", "not_cursed" -> UNCURSED;
                default -> null;
            };
        }
    }
}

