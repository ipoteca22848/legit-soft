package com.example.auravisuals.util;

import com.example.auravisuals.util.chat.IrcRolePolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.client.MinecraftClient;

public final class MessengerPreferences {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> enabledNotifications = new HashSet<>();
    private static final Set<String> pinnedChats = new HashSet<>();
    private static final Set<String> modGrantedLogins = new HashSet<>();
    private static final List<String> customChannels = new ArrayList<>();
    private static final List<String> bannedWords = new ArrayList<>();
    private static final List<String> auditLogs = new ArrayList<>();
    private static int slowmodeSeconds = 0;
    private static boolean chatMuted = false;
    private static String customUsername = "";
    private static String customAvatarUrl = "";
    private static boolean loaded;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private MessengerPreferences() {
    }

    public static boolean hasModPermission(String login) {
        ensureLoaded();
        if (login == null) {
            return false;
        } else {
            String clean = login.trim().toLowerCase(Locale.ROOT);
            String myRole = ClientProfile.getRole();
            if (!"OWNER".equalsIgnoreCase(myRole)
                && !"ADMIN".equalsIgnoreCase(myRole)
                && !"MODER".equalsIgnoreCase(myRole)
                && !"MODERATOR".equalsIgnoreCase(myRole)) {
                return IrcRolePolicy.isTrusted(clean) ? true : modGrantedLogins.contains(clean);
            } else {
                return true;
            }
        }
    }

    public static boolean toggleModPermission(String login) {
        ensureLoaded();
        if (login == null) {
            return false;
        }

        String clean = login.trim().toLowerCase(Locale.ROOT);
        boolean granted;
        if (modGrantedLogins.contains(clean)) {
            modGrantedLogins.remove(clean);
            granted = false;
            addAuditLog("Отозваны права модератора у @" + clean);
        } else {
            modGrantedLogins.add(clean);
            granted = true;
            addAuditLog("Выданы права модератора @" + clean);
        }

        save();
        return granted;
    }

    public static Set<String> getModGrantedLogins() {
        ensureLoaded();
        return Collections.unmodifiableSet(modGrantedLogins);
    }

    public static void addModLogin(String login) {
        ensureLoaded();
        if (login != null && !login.isBlank()) {
            String clean = login.trim().toLowerCase(Locale.ROOT);
            if (modGrantedLogins.add(clean)) {
                addAuditLog("Назначен модератор @" + clean);
                save();
            }
        }
    }

    public static void removeModLogin(String login) {
        ensureLoaded();
        if (login != null && !login.isBlank()) {
            String clean = login.trim().toLowerCase(Locale.ROOT);
            if (modGrantedLogins.remove(clean)) {
                addAuditLog("Снят модератор @" + clean);
                save();
            }
        }
    }

    public static int getSlowmodeSeconds() {
        ensureLoaded();
        return slowmodeSeconds;
    }

    public static void setSlowmodeSeconds(int seconds) {
        ensureLoaded();
        slowmodeSeconds = Math.max(0, seconds);
        addAuditLog(slowmodeSeconds == 0 ? "Слоумод выключен" : "Установлен слоумод: " + slowmodeSeconds + " сек.");
        save();
    }

    public static boolean isChatMuted() {
        ensureLoaded();
        return chatMuted;
    }

    public static void setChatMuted(boolean muted) {
        ensureLoaded();
        chatMuted = muted;
        addAuditLog(chatMuted ? "Включен режим только для чтения (Мут)" : "Чат размучен");
        save();
    }

    public static List<String> getBannedWords() {
        ensureLoaded();
        return Collections.unmodifiableList(bannedWords);
    }

    public static void addBannedWord(String word) {
        ensureLoaded();
        if (word != null && !word.isBlank()) {
            String clean = word.trim().toLowerCase(Locale.ROOT);
            if (!bannedWords.contains(clean)) {
                bannedWords.add(clean);
                addAuditLog("Добавлено запрещённое слово: \"" + clean + "\"");
                save();
            }
        }
    }

    public static void removeBannedWord(String word) {
        ensureLoaded();
        if (word != null && !word.isBlank()) {
            String clean = word.trim().toLowerCase(Locale.ROOT);
            if (bannedWords.remove(clean)) {
                addAuditLog("Удалено запрещённое слово: \"" + clean + "\"");
                save();
            }
        }
    }

    public static boolean containsBannedWord(String text) {
        ensureLoaded();
        if (text != null && !text.isBlank()) {
            String lower = text.toLowerCase(Locale.ROOT);

            for (String w : bannedWords) {
                if (lower.contains(w)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    public static List<String> getAuditLogs() {
        ensureLoaded();
        return Collections.unmodifiableList(auditLogs);
    }

    public static void addAuditLog(String action) {
        ensureLoaded();
        if (action != null && !action.isBlank()) {
            String time = LocalTime.now().format(TIME_FMT);
            auditLogs.add(0, "[" + time + "] " + action.trim());

            while (auditLogs.size() > 50) {
                auditLogs.remove(auditLogs.size() - 1);
            }

            save();
        }
    }

    public static String getCustomUsername() {
        ensureLoaded();
        return customUsername;
    }

    public static void setCustomUsername(String username) {
        ensureLoaded();
        customUsername = username == null ? "" : username.trim();
        save();
    }

    public static String getCustomAvatarUrl() {
        ensureLoaded();
        return customAvatarUrl;
    }

    public static void setCustomAvatarUrl(String avatarUrl) {
        ensureLoaded();
        customAvatarUrl = avatarUrl == null ? "" : avatarUrl.trim();
        save();
    }

    public static boolean isPinned(String key) {
        ensureLoaded();
        return pinnedChats.contains(normalize(key));
    }

    public static boolean togglePin(String key) {
        ensureLoaded();
        String normalized = normalize(key);
        boolean pinned;
        if (pinnedChats.contains(normalized)) {
            pinnedChats.remove(normalized);
            pinned = false;
        } else {
            pinnedChats.add(normalized);
            pinned = true;
        }

        save();
        return pinned;
    }

    public static List<String> getCustomChannels() {
        ensureLoaded();
        return new ArrayList<>(customChannels);
    }

    public static void addCustomChannel(String channelName) {
        ensureLoaded();
        if (channelName != null && !channelName.isBlank()) {
            String clean = channelName.trim();
            if (!customChannels.contains(clean)) {
                customChannels.add(clean);
                addAuditLog("Создан канал: " + clean);
                save();
            }
        }
    }

    public static boolean isEnabled(String channelKey) {
        ensureLoaded();
        return enabledNotifications.contains(normalize(channelKey));
    }

    public static boolean toggle(String channelKey) {
        ensureLoaded();
        String key = normalize(channelKey);
        boolean enabled;
        if (enabledNotifications.contains(key)) {
            enabledNotifications.remove(key);
            enabled = false;
        } else {
            enabledNotifications.add(key);
            enabled = true;
        }

        save();
        return enabled;
    }

    public static String keyFor(String type, String extra, String login) {
        if ("global".equalsIgnoreCase(type)) {
            return "global";
        } else {
            return "group".equalsIgnoreCase(type) ? "group:" + normalize(extra) : "dm:" + normalize(extra != null && !extra.isBlank() ? extra : login);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static void ensureLoaded() {
        if (!loaded) {
            loaded = true;
            File file = file();
            if (file.exists()) {
                try (FileReader reader = new FileReader(file)) {
                    JsonObject root = (JsonObject)GSON.fromJson(reader, JsonObject.class);
                    if (root != null) {
                        if (root.has("customUsername") && root.get("customUsername").isJsonPrimitive()) {
                            customUsername = root.get("customUsername").getAsString();
                        }

                        if (root.has("customAvatarUrl") && root.get("customAvatarUrl").isJsonPrimitive()) {
                            customAvatarUrl = root.get("customAvatarUrl").getAsString();
                        }

                        if (root.has("slowmode") && root.get("slowmode").isJsonPrimitive()) {
                            slowmodeSeconds = root.get("slowmode").getAsInt();
                        }

                        if (root.has("chatMuted") && root.get("chatMuted").isJsonPrimitive()) {
                            chatMuted = root.get("chatMuted").getAsBoolean();
                        }

                        if (root.has("enabled") && root.get("enabled").isJsonArray()) {
                            for (JsonElement element : root.getAsJsonArray("enabled")) {
                                if (element.isJsonPrimitive()) {
                                    enabledNotifications.add(normalize(element.getAsString()));
                                }
                            }
                        }

                        if (root.has("pinned") && root.get("pinned").isJsonArray()) {
                            for (JsonElement elementx : root.getAsJsonArray("pinned")) {
                                if (elementx.isJsonPrimitive()) {
                                    pinnedChats.add(normalize(elementx.getAsString()));
                                }
                            }
                        }

                        if (root.has("modGranted") && root.get("modGranted").isJsonArray()) {
                            for (JsonElement elementxx : root.getAsJsonArray("modGranted")) {
                                if (elementxx.isJsonPrimitive()) {
                                    modGrantedLogins.add(normalize(elementxx.getAsString()));
                                }
                            }
                        }

                        if (root.has("bannedWords") && root.get("bannedWords").isJsonArray()) {
                            for (JsonElement elementxxx : root.getAsJsonArray("bannedWords")) {
                                if (elementxxx.isJsonPrimitive()) {
                                    bannedWords.add(elementxxx.getAsString());
                                }
                            }
                        }

                        if (root.has("auditLogs") && root.get("auditLogs").isJsonArray()) {
                            for (JsonElement elementxxxx : root.getAsJsonArray("auditLogs")) {
                                if (elementxxxx.isJsonPrimitive()) {
                                    auditLogs.add(elementxxxx.getAsString());
                                }
                            }
                        }

                        if (!root.has("channels") || !root.get("channels").isJsonArray()) {
                            return;
                        }

                        for (JsonElement elementxxxxx : root.getAsJsonArray("channels")) {
                            if (elementxxxxx.isJsonPrimitive()) {
                                customChannels.add(elementxxxxx.getAsString());
                            }
                        }

                        return;
                    }

                    return;
                } catch (Exception var7) {
                }
            }
        }
    }

    private static void save() {
        File file = file();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        JsonObject root = new JsonObject();
        root.addProperty("customUsername", customUsername);
        root.addProperty("customAvatarUrl", customAvatarUrl);
        root.addProperty("slowmode", slowmodeSeconds);
        root.addProperty("chatMuted", chatMuted);
        JsonArray enabled = new JsonArray();
        enabledNotifications.stream().sorted().forEach(enabled::add);
        root.add("enabled", enabled);
        JsonArray pinned = new JsonArray();
        pinnedChats.stream().sorted().forEach(pinned::add);
        root.add("pinned", pinned);
        JsonArray mods = new JsonArray();
        modGrantedLogins.stream().sorted().forEach(mods::add);
        root.add("modGranted", mods);
        JsonArray bWords = new JsonArray();
        bannedWords.forEach(bWords::add);
        root.add("bannedWords", bWords);
        JsonArray logs = new JsonArray();
        auditLogs.forEach(logs::add);
        root.add("auditLogs", logs);
        JsonArray channels = new JsonArray();
        customChannels.forEach(channels::add);
        root.add("channels", channels);

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(root, writer);
        } catch (Exception var14) {
        }
    }

    private static File file() {
        MinecraftClient client = MinecraftClient.getInstance();
        File runDirectory = client == null ? new File(".") : client.runDirectory;
        return new File(runDirectory, "anvil/messenger_preferences.json");
    }
}
