package com.example.auravisuals.util.friendscreen;

import java.util.Locale;

public enum FriendScreenClient$StreamView {
    HUD,
    WORLD;

    public static FriendScreenClient$StreamView parse(String value) {
        if (value == null) {
            return HUD;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception var2) {
            return HUD;
        }
    }
}
