package com.example.auravisuals.util.chat;

import com.example.auravisuals.client.gui.IrcChatMessengerScreen;
import com.example.auravisuals.util.ClientProfile;
import com.example.auravisuals.util.IrcIdentity;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class ChatHubClient {
    public static final ChatHubClient INSTANCE = new ChatHubClient();
    public static final String[] SERVER_URLS = new String[]{"ws://152.89.199.138:7656", "wss://auravisuals.space/ws/chathub"};
    public static final String DEFAULT_URL = SERVER_URLS[0];
    volatile ChatHubClient$State state = ChatHubClient$State.DISCONNECTED;
    volatile boolean initialHistoryLoaded = false;
    volatile int activeUrlIndex = 0;
    volatile boolean allowFallback = true;
    volatile String lastConnectionError = "";
    volatile String authenticatedLogin = "";
    volatile int onlineCount;
    final List<ChatHubMessage> messages = new CopyOnWriteArrayList<>();
    final ChatHubConnection connection = new ChatHubConnection();
    static final HttpClient HTTP = HttpClient.newBuilder().version(Version.HTTP_1_1).build();
    final Map<String, Identifier> avatarCache = new HashMap<>();
    final Map<String, int[]> avatarSizes = new HashMap<>();
    final Set<String> pendingAvs = new HashSet<>();
    volatile int messageSeq = 0;
    static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    static volatile Consumer<ChatHubMessage> ircListener;
    static final ZoneId CHAT_ZONE = ZoneId.of("Europe/Moscow");

    ChatHubClient() {
        this.connection.setOnIncoming(this::handleIncoming);
        this.connection.setOnConnected(() -> {
            this.state = ChatHubClient$State.CONNECTED;
            this.initialHistoryLoaded = false;
            this.lastConnectionError = "";
        });
        this.connection.setOnDisconnected(() -> {
            this.state = ChatHubClient$State.DISCONNECTED;
            this.initialHistoryLoaded = false;
            if (this.allowFallback && this.activeUrlIndex + 1 < SERVER_URLS.length) {
                this.activeUrlIndex++;
                this.connect(SERVER_URLS[this.activeUrlIndex]);
            } else if (this.allowFallback) {
                this.lastConnectionError = "Сервер чата недоступен";
            }
        });
    }

    public void connect(String url) {
        if (this.state == ChatHubClient$State.DISCONNECTED) {
            for (int index = 0; index < SERVER_URLS.length; index++) {
                if (SERVER_URLS[index].equals(url)) {
                    this.activeUrlIndex = index;
                }
            }

            this.allowFallback = true;
            this.lastConnectionError = "";
            this.state = ChatHubClient$State.CONNECTING;
            this.initialHistoryLoaded = false;
            String login = IrcIdentity.getLogin();
            this.authenticatedLogin = login;
            String role = IrcRolePolicy.sanitizeRole(login, ClientProfile.getRole());
            String avatarUrl = ClientProfile.getAvatarUrl();
            this.connection.connect(url, login, role, avatarUrl);
        }
    }

    public void disconnect() {
        this.allowFallback = false;
        this.connection.disconnect();
        this.state = ChatHubClient$State.DISCONNECTED;
    }

    public boolean sendMessage(String text) {
        if (this.state != ChatHubClient$State.CONNECTED) {
            return false;
        } else if (text != null && !text.isBlank()) {
            String safe = text.length() > 120 ? text.substring(0, 120) : text;
            this.ensureIdentityCurrent();
            String login = IrcIdentity.getLogin();
            String role = IrcRolePolicy.sanitizeRole(login, ClientProfile.getRole());
            String avatarUrl = ClientProfile.getAvatarUrl();
            return this.connection.sendChatJson(login, role, avatarUrl, safe);
        } else {
            return false;
        }
    }

    public static void setIrcListener(Consumer<ChatHubMessage> listener) {
        ircListener = listener;
    }

    public static void mirrorFromIrc(String login, String role, String message) {
        ChatHubMessage msg = new ChatHubMessage(login, IrcRolePolicy.sanitizeRole(login, role), "", message, nowTime());
        INSTANCE.addMessage(msg);
    }

    void handleIncoming(JsonObject json) {
        String type = json.has("type") ? json.get("type").getAsString() : "chat";
        switch (type) {
            case "history":
                if (!json.has("messages")) {
                    return;
                }

                JsonArray arr = json.getAsJsonArray("messages");
                List<ChatHubMessage> hist = new ArrayList<>();

                for (JsonElement el : arr) {
                    if (el.isJsonObject()) {
                        hist.add(parseMessage(el.getAsJsonObject()));
                    }
                }

                this.messages.clear();
                this.messages.addAll(hist);
                this.initialHistoryLoaded = true;
                this.messageSeq++;
                this.preloadAvatars(hist);
                break;
            case "chat":
                ChatHubMessage raw = parseMessage(json);
                ChatHubMessage msg = new ChatHubMessage(
                    raw.login(), IrcRolePolicy.sanitizeRole(raw.login(), raw.role()), raw.avatarUrl(), raw.message(), nowTime()
                );
                this.addMessage(msg);
                this.loadAvatar(msg.avatarUrl());
                String selfLogin = IrcIdentity.getLogin();
                if (selfLogin == null || !msg.login().equalsIgnoreCase(selfLogin)) {
                    IrcChatMessengerScreen.pushRichNotification("global", msg.avatarUrl(), msg.login(), "Общий чат", msg.message());
                }
                break;
            case "system":
                String text = json.has("text") ? json.get("text").getAsString() : "";
                this.addMessage(new ChatHubMessage("⚙ Система", "SYSTEM", "", text, nowTime()));
                break;
            case "online_count":
                this.onlineCount = json.has("count") ? Math.max(0, json.get("count").getAsInt()) : 0;
        }
    }

    static ChatHubMessage parseMessage(JsonObject o) {
        return new ChatHubMessage(
            o.has("login") ? o.get("login").getAsString() : "?",
            IrcRolePolicy.sanitizeRole(o.has("login") ? o.get("login").getAsString() : "?", o.has("role") ? o.get("role").getAsString() : "USER"),
            o.has("avatar_url") ? o.get("avatar_url").getAsString() : "",
            o.has("message") ? o.get("message").getAsString() : "",
            o.has("time") ? o.get("time").getAsString() : nowTime()
        );
    }

    public void addMessage(ChatHubMessage msg) {
        if (!this.messages.isEmpty()) {
            ChatHubMessage last = this.messages.get(this.messages.size() - 1);
            if (last.login().equalsIgnoreCase(msg.login()) && last.message().equals(msg.message()) && last.time().equals(msg.time())) {
                return;
            }
        }

        this.messages.add(msg);
        this.messageSeq++;
    }

    public void deleteMessage(ChatHubMessage msg) {
        if (msg != null) {
            this.messages.remove(msg);
            this.messageSeq++;
        }
    }

    public void clearAllMessages() {
        this.messages.clear();
        this.messageSeq++;
    }

    void preloadAvatars(List<ChatHubMessage> list) {
        for (ChatHubMessage m : list) {
            this.loadAvatar(m.avatarUrl());
        }
    }

    public void loadAvatar(String url) {
        String avatarUrl = normalizeAvatarUrl(url);
        if (avatarUrl != null && !this.avatarCache.containsKey(avatarUrl) && !this.pendingAvs.contains(avatarUrl)) {
            this.pendingAvs.add(avatarUrl);
            if (avatarUrl.startsWith("data:image/") && avatarUrl.contains(";base64,")) {
                try {
                    int payloadStart = avatarUrl.indexOf(44) + 1;
                    this.registerAvatarTexture(avatarUrl, Base64.getDecoder().decode(avatarUrl.substring(payloadStart)));
                } catch (IllegalArgumentException var4) {
                    this.pendingAvs.remove(avatarUrl);
                }
            } else {
                HTTP.sendAsync(HttpRequest.newBuilder().uri(URI.create(avatarUrl)).timeout(Duration.ofSeconds(15L)).build(), BodyHandlers.ofByteArray())
                    .thenAccept(resp -> {
                        if (resp.statusCode() != 200) {
                            this.pendingAvs.remove(avatarUrl);
                        } else {
                            this.registerAvatarTexture(avatarUrl, resp.body());
                        }
                    })
                    .exceptionally(ex -> {
                        this.pendingAvs.remove(avatarUrl);
                        return null;
                    });
            }
        }
    }

    void registerAvatarTexture(String avatarUrl, byte[] body) {
        MinecraftClient.getInstance().execute(() -> {
            try {
                NativeImage img = NativeImage.read(new ByteArrayInputStream(body));
                int w = img.getWidth();
                int h = img.getHeight();
                String idStr = "chathub_avatar_" + Math.abs(avatarUrl.hashCode());
                Identifier id = Identifier.of("auravisuals", idStr);
                MinecraftClient.getInstance().getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "auravisual", img));
                this.avatarCache.put(avatarUrl, id);
                this.avatarSizes.put(avatarUrl, new int[]{w, h});
            } catch (Exception var11) {
            } finally {
                this.pendingAvs.remove(avatarUrl);
            }
        });
    }

    public Identifier getAvatarTexture(String url) {
        url = normalizeAvatarUrl(url);
        return url == null ? null : this.avatarCache.get(url);
    }

    public int[] getAvatarSize(String url) {
        url = normalizeAvatarUrl(url);
        return url == null ? null : this.avatarSizes.get(url);
    }

    static String normalizeAvatarUrl(String url) {
        if (url == null) {
            return null;
        }

        String trimmed = url.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        if (trimmed.startsWith("data:image/") && trimmed.contains(";base64,")) {
            return trimmed;
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return trimmed;
        }

        if (trimmed.startsWith("//")) {
            return "http:" + trimmed;
        }

        URI base = URI.create(DEFAULT_URL.replaceFirst("^ws", "http"));
        String prefix = base.getScheme() + "://" + base.getAuthority();
        return trimmed.startsWith("/") ? prefix + trimmed : prefix + "/" + trimmed;
    }

    public ChatHubClient$State getState() {
        return this.state;
    }

    public String getLastConnectionError() {
        return this.lastConnectionError;
    }

    public List<ChatHubMessage> getMessages() {
        return Collections.unmodifiableList(this.messages);
    }

    public int getMessageSeq() {
        return this.messageSeq;
    }

    public int getOnlineCount() {
        return this.onlineCount;
    }

    public boolean isInitialHistoryLoaded() {
        return this.initialHistoryLoaded;
    }

    public void ensureIdentityCurrent() {
        String current = IrcIdentity.getLogin();
        if (!this.authenticatedLogin.isBlank() && !this.authenticatedLogin.equalsIgnoreCase(current)) {
            this.refreshIdentity();
        }
    }

    public void refreshIdentity() {
        String current = IrcIdentity.getLogin();
        if (!this.authenticatedLogin.isBlank() || this.state != ChatHubClient$State.DISCONNECTED) {
            this.allowFallback = false;
            this.connection.disconnect();
            this.state = ChatHubClient$State.DISCONNECTED;
            this.authenticatedLogin = "";
            this.activeUrlIndex = 0;
            this.allowFallback = true;
            this.connect(DEFAULT_URL);
        }
    }

    static String nowTime() {
        return ZonedDateTime.now(CHAT_ZONE).format(TIME_FMT);
    }
}
