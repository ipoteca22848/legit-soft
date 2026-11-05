package com.example.auravisuals.util;

import fun.shape.profile.Profile;
import fun.shape.profile.Role;
import net.minecraft.client.MinecraftClient;

public final class ClientProfile {
    private ClientProfile() {
    }

    public static String getUsername() {
        String custom = MessengerPreferences.getCustomUsername();
        if (custom != null && !custom.isBlank()) {
            return custom.trim();
        }

        String username = Profile.username;
        if (username != null && !username.isBlank() && !username.equalsIgnoreCase("User")) {
            return username.trim();
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null && client.player.getGameProfile() != null) {
            String name = client.player.getGameProfile().name();
            if (name != null && !name.isBlank()) {
                return name.trim();
            }
        }

        if (client != null && client.getSession() != null) {
            String fallback = client.getSession().getUsername();
            if (fallback != null && !fallback.isBlank()) {
                return fallback.trim();
            }
        }

        if (client != null && client.player != null && client.player.getName() != null) {
            String name = client.player.getName().getString();
            if (name != null && !name.isBlank()) {
                return name.trim();
            }
        }

        return "User";
    }

    public static String getRole() {
        Role role = Profile.role;
        return role != null ? role.name() : "USER";
    }

    public static int getUid() {
        return Profile.uid;
    }

    public static String getSubscriptionEndDate() {
        return Profile.subscriptionEndDate != null ? Profile.subscriptionEndDate : "";
    }

    public static String getHwid() {
        return Profile.hwid != null ? Profile.hwid : "";
    }

    public static String getAvatarUrl() {
        String custom = MessengerPreferences.getCustomAvatarUrl();
        if (custom != null && !custom.isBlank()) {
            return custom.trim();
        } else if (Profile.avatarUrl != null && !Profile.avatarUrl.isBlank()) {
            return Profile.avatarUrl.trim();
        } else {
            return Profile.uid > 0 ? "https://auravisuals.space/api/account/avatar/" + Profile.uid : "";
        }
    }
}
