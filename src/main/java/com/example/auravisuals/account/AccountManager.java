package com.example.auravisuals.account;

import com.example.auravisuals.cosmetics.CosmeticsNetworkClient;
import com.example.auravisuals.mixin.accessor.MinecraftClientAccessor;
import com.example.auravisuals.module.misc.Globals;
import com.example.auravisuals.util.chat.ChatHubClient;
import com.example.auravisuals.util.friend.FriendHubClient;
import com.example.auravisuals.util.friendlabels.FriendLabelsClient;
import com.example.auravisuals.util.friendscreen.FriendScreenClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.session.Session;

public final class AccountManager {
    static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy").withZone(ZoneId.systemDefault());
    static final Random RANDOM = new Random();
    static final String[] RANDOM_LEFT = new String[]{
        "bamber",
        "Crimson",
        "Void",
        "Night",
        "Raven",
        "Blade",
        "Nova",
        "Vex",
        "Hex",
        "Inferno",
        "Shadow",
        "Scarlet",
        "Obsidian",
        "Phantom",
        "Eclipse",
        "Ember",
        "Ash",
        "Onyx",
        "Ruby",
        "Noir",
        "Frost",
        "Storm",
        "Venom",
        "Solar",
        "Lunar",
        "Cyber",
        "Neon",
        "Quartz",
        "Abyss",
        "Chrome",
        "Specter",
        "Mirage",
        "Reaper",
        "Vector",
        "Cipher",
        "Rogue",
        "Sable",
        "Drift",
        "Feral",
        "Iris",
        "Kairo",
        "Lumen",
        "Nero",
        "Orion",
        "Rune",
        "Siren",
        "Titan",
        "Astra",
        "Echo"
    };
    static final String[] RANDOM_RIGHT = new String[]{
        "bamber",
        "Shade",
        "Strike",
        "Core",
        "Fang",
        "Rush",
        "Flame",
        "Ghost",
        "Rift",
        "Byte",
        "Warden",
        "Hunter",
        "Spark",
        "Edge",
        "Mark",
        "Crown",
        "Wing",
        "Claw",
        "Shard",
        "Vibe",
        "Ray",
        "Flux",
        "Dash",
        "Haze",
        "Bloom",
        "Prism",
        "Spire",
        "Dusk",
        "Dawn",
        "Trace",
        "Havoc",
        "Fury",
        "Glint",
        "Rage",
        "Vortex",
        "Signal",
        "Unit",
        "Prime",
        "Shift",
        "Lance",
        "Path",
        "Flick",
        "Charm",
        "Sway",
        "Lock",
        "Flash",
        "Quest",
        "Forge",
        "Vow",
        "Peak"
    };
    static final List<AccountManager$Account> accounts = new ArrayList<>();
    static boolean loaded;
    static String selectedName = "";

    AccountManager() {
    }

    public static void ensureLoaded() {
        if (!loaded) {
            loaded = true;
            load();
        }
    }

    public static List<AccountManager$Account> getAccounts() {
        ensureLoaded();
        return Collections.unmodifiableList(accounts);
    }

    public static String getSelectedName() {
        ensureLoaded();
        if (selectedName != null && !selectedName.isBlank()) {
            return selectedName;
        }

        Session session = MinecraftClient.getInstance().getSession();
        return session == null ? "" : session.getUsername();
    }

    public static boolean isUsingManagedAccount() {
        ensureLoaded();
        return selectedName != null && !selectedName.isBlank();
    }

    public static void restoreSelectedSession() {
        ensureLoaded();
        String nickname = sanitizeNickname(selectedName);
        if (nickname != null) {
            AccountManager$Account selected = findAccount(nickname);
            if (selected == null) {
                selectedName = "";
                save();
            } else {
                selectedName = selected.nickname;
                applySession(selected.nickname);
            }
        }
    }

    public static AccountManager$Account addAccount(String rawNickname) {
        ensureLoaded();
        String nickname = sanitizeNickname(rawNickname);
        if (nickname == null) {
            return null;
        }

        AccountManager$Account existing = findAccount(nickname);
        if (existing != null) {
            return existing;
        }

        AccountManager$Account account = new AccountManager$Account(nickname, System.currentTimeMillis(), false);
        accounts.add(account);
        save();
        return account;
    }

    public static AccountManager$Account addRandomAccount() {
        ensureLoaded();

        for (int i = 0; i < 100; i++) {
            String nickname = randomNickname();
            if (findAccount(nickname) == null) {
                return addAccount(nickname);
            }
        }

        return addAccount("bamber" + (1000 + RANDOM.nextInt(9000)));
    }

    public static void select(AccountManager$Account account) {
        if (account != null) {
            ensureLoaded();
            selectedName = account.nickname;
            save();
            applySession(account.nickname);
        }
    }

    public static void toggleFavorite(AccountManager$Account account) {
        if (account != null) {
            ensureLoaded();
            account.favorite = !account.favorite;
            save();
        }
    }

    public static void remove(AccountManager$Account account) {
        if (account != null) {
            ensureLoaded();
            accounts.removeIf(a -> a.nickname.equalsIgnoreCase(account.nickname));
            if (account.nickname.equalsIgnoreCase(selectedName)) {
                selectedName = "";
            }

            save();
        }
    }

    public static void clearAll() {
        ensureLoaded();
        accounts.clear();
        selectedName = "";
        save();
    }

    public static UUID offlineUuid(String nickname) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + nickname).getBytes(StandardCharsets.UTF_8));
    }

    public static String formatDate(long createdAt) {
        return DATE_FORMAT.format(Instant.ofEpochMilli(createdAt));
    }

    public static String sanitizeNickname(String raw) {
        if (raw == null) {
            return null;
        }

        String value = raw.trim();
        StringBuilder out = new StringBuilder(16);

        for (int i = 0; i < value.length() && out.length() < 16; i++) {
            char c = value.charAt(i);
            if (c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9' || c == '_') {
                out.append(c);
            }
        }

        return out.length() >= 3 ? out.toString() : null;
    }

    static void applySession(String nickname) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null) {
            UUID uuid = offlineUuid(nickname);
            Session session = new Session(nickname, uuid, "0", Optional.empty(), Optional.empty());
            ((MinecraftClientAccessor)mc).setSession(session);
            ChatHubClient.INSTANCE.refreshIdentity();
            FriendHubClient.INSTANCE.refreshIdentity();
            FriendScreenClient.INSTANCE.refreshIdentity();
            FriendLabelsClient.INSTANCE.refreshIdentity();
            Globals.onAccountChanged(nickname);
            CosmeticsNetworkClient.INSTANCE.sendHello();
        }
    }

    static AccountManager$Account findAccount(String nickname) {
        for (AccountManager$Account account : accounts) {
            if (account.nickname.equalsIgnoreCase(nickname)) {
                return account;
            }
        }

        return null;
    }

    static String randomNickname() {
        String left = RANDOM_LEFT[RANDOM.nextInt(RANDOM_LEFT.length)];
        String right = RANDOM_RIGHT[RANDOM.nextInt(RANDOM_RIGHT.length)];
        int mode = RANDOM.nextInt(5);

        String suffix = switch (mode) {
            case 0 -> String.format(Locale.ROOT, "%02d", RANDOM.nextInt(100));
            case 1 -> String.valueOf(10 + RANDOM.nextInt(890));
            case 2 -> "_" + RANDOM.nextInt(99);
            case 3 -> "";
            default -> "" + (char)(65 + RANDOM.nextInt(26)) + RANDOM.nextInt(10);
        };

        String value = switch (mode) {
            case 1 -> right + left + suffix;
            case 2 -> left + suffix;
            case 3 -> right + left;
            default -> left + right + suffix;
        };
        return sanitizeNickname(value);
    }

    static File accountsFile() {
        return new File(MinecraftClient.getInstance().runDirectory, "bamber/accounts.json");
    }

    static void load() {
        File file = accountsFile();
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                JsonObject root = (JsonObject)GSON.fromJson(reader, JsonObject.class);
                if (root != null) {
                    if (root.has("selected") && root.get("selected").isJsonPrimitive()) {
                        selectedName = root.get("selected").getAsString();
                    }

                    accounts.clear();
                    if (!root.has("accounts") || !root.get("accounts").isJsonArray()) {
                        return;
                    }

                    for (JsonElement element : root.getAsJsonArray("accounts")) {
                        if (element.isJsonObject()) {
                            JsonObject object = element.getAsJsonObject();
                            String nickname = object.has("nickname") && object.get("nickname").isJsonPrimitive()
                                ? sanitizeNickname(object.get("nickname").getAsString())
                                : null;
                            if (nickname != null && findAccount(nickname) == null) {
                                long createdAt = object.has("createdAt") && object.get("createdAt").isJsonPrimitive()
                                    ? object.get("createdAt").getAsLong()
                                    : System.currentTimeMillis();
                                boolean favorite = object.has("favorite") && object.get("favorite").isJsonPrimitive() && object.get("favorite").getAsBoolean();
                                accounts.add(new AccountManager$Account(nickname, createdAt, favorite));
                            }
                        }
                    }

                    return;
                }

                return;
            } catch (Exception var12) {
            }
        }
    }

    static void save() {
        File file = accountsFile();
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            dir.mkdirs();
        }

        JsonObject root = new JsonObject();
        root.addProperty("selected", selectedName == null ? "" : selectedName);
        JsonArray array = new JsonArray();

        for (AccountManager$Account account : accounts) {
            JsonObject object = new JsonObject();
            object.addProperty("nickname", account.nickname);
            object.addProperty("createdAt", account.createdAt);
            object.addProperty("favorite", account.favorite);
            array.add(object);
        }

        root.add("accounts", array);

        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(root, writer);
        } catch (IOException var9) {
        }
    }
}
