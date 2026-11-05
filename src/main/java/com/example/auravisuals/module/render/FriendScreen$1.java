package com.example.auravisuals.module.render;

import com.example.auravisuals.util.friendscreen.FriendScreenClient$StreamQuality;

public class FriendScreen$1 {
    static final int[] $SwitchMap$com$example$auravisuals$util$friendscreen$FriendScreenClient$StreamQuality = new int[FriendScreenClient$StreamQuality.values().length];

    static {
        try {
            $SwitchMap$com$example$auravisuals$util$friendscreen$FriendScreenClient$StreamQuality[FriendScreenClient$StreamQuality.MEDIUM.ordinal()] = 1;
        } catch (NoSuchFieldError var2) {
        }

        try {
            $SwitchMap$com$example$auravisuals$util$friendscreen$FriendScreenClient$StreamQuality[FriendScreenClient$StreamQuality.HIGH.ordinal()] = 2;
        } catch (NoSuchFieldError var1) {
        }
    }
}
