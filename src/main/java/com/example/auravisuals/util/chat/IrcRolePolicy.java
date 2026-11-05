package com.example.auravisuals.util.chat;

import java.util.Locale;
import java.util.Set;

public final class IrcRolePolicy {
    private static final Set<String> TRUSTED_LOGINS = Set.of("wasper", "wasperovo", "admist", "hagenezuko", "shestovv");

    private IrcRolePolicy() {
    }

    public static boolean isTrusted(String login) {
        return login == null ? false : TRUSTED_LOGINS.contains(login.trim().toLowerCase(Locale.ROOT));
    }

    public static String sanitizeRole(String login, String role) {
        return !isTrusted(login) ? "USER" : normalizeRole(role);
    }

    public static String normalizeRole(String raw) {
        if (raw == null) {
            return "USER";
        }

        String role = raw.trim();
        if (role.isBlank()) {
            return "USER";
        }

        String var2 = role.toLowerCase(Locale.ROOT);

        return switch (var2) {
            case "owner" -> "OWNER";
            case "admin", "administrator" -> "ADMIN";
            case "moderator", "mod" -> "MODERATOR";
            case "premium", "vip" -> role.toUpperCase(Locale.ROOT);
            case "user" -> "USER";
            default -> role.toUpperCase(Locale.ROOT);
        };
    }
}
