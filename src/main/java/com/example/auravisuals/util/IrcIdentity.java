package com.example.auravisuals.util;

public final class IrcIdentity {
    private IrcIdentity() {
    }

    public static String getLogin() {
        return ClientProfile.getUsername();
    }
}
