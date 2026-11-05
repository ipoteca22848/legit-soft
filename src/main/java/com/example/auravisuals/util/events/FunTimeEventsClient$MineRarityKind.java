package com.example.auravisuals.util.events;

import java.util.Locale;

public enum FunTimeEventsClient$MineRarityKind {
    LEGENDARY,
    MYTHICAL,
    NORMAL;

    public static FunTimeEventsClient$MineRarityKind fromApiString(String raw) {
        if (raw != null && !raw.isBlank()) {
            String s = raw.trim().toLowerCase(Locale.ROOT);

            return switch (s) {
                case "legendary" -> LEGENDARY;
                case "mythical", "mythic", "mystic" -> MYTHICAL;
                default -> NORMAL;
            };
        } else {
            return NORMAL;
        }
    }

    public int listGroupOrder() {
        return switch (this) {
            case LEGENDARY -> 0;
            case MYTHICAL -> 1;
            case NORMAL -> 2;
        };
    }

    public String displayLabel() {
        return switch (this) {
            case LEGENDARY -> "Легендарная";
            case MYTHICAL -> "Мифическая";
            case NORMAL -> "Обычная";
        };
    }
}
