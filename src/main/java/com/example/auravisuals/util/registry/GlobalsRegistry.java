package com.example.auravisuals.util.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class GlobalsRegistry {
    private static final Set<String> PLAYERS = ConcurrentHashMap.newKeySet();

    private GlobalsRegistry() {
    }

    public static void replaceAll(Collection<String> names) {
        PLAYERS.clear();
        if (names != null) {
            for (String n : names) {
                addInternal(n);
            }
        }
    }

    public static void add(String name) {
        addInternal(name);
    }

    private static void addInternal(String name) {
        if (name != null) {
            String t = name.trim();
            if (!t.isEmpty()) {
                PLAYERS.add(t.toLowerCase(Locale.ROOT));
            }
        }
    }

    public static void remove(String name) {
        if (name != null) {
            PLAYERS.remove(name.trim().toLowerCase(Locale.ROOT));
        }
    }

    public static void clear() {
        PLAYERS.clear();
    }

    public static boolean isGlobalsPlayer(String name) {
        return name == null ? false : PLAYERS.contains(name.trim().toLowerCase(Locale.ROOT));
    }

    public static Set<String> getPlayers() {
        return Collections.unmodifiableSet(PLAYERS);
    }
}
