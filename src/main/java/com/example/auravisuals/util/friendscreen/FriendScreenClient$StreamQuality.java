package com.example.auravisuals.util.friendscreen;

import java.util.Locale;

public enum FriendScreenClient$StreamQuality {
    LOW,
    MEDIUM,
    HIGH;

    public static FriendScreenClient$StreamQuality parse(String value) {
        if (value == null) {
            return LOW;
        }

        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (Exception var2) {
            return LOW;
        }
    }
}
