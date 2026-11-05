package com.example.auravisuals.util.friendlabels;

import com.example.auravisuals.util.ClientProfile;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicReference;

public final class FriendLabelsClient {
    public static final FriendLabelsClient INSTANCE = new FriendLabelsClient();
    public static final String[] SERVER_URLS = new String[]{"ws://152.89.199.138:7659", "wss://auravisuals.space/ws/friendlabels"};
    public static final String DEFAULT_URL = SERVER_URLS[0];
    static final HttpClient HTTP = HttpClient.newHttpClient();
    final AtomicReference<WebSocket> socket = new AtomicReference<>();
    final Map<String, FriendLabelsClient$Entry> friends = new LinkedHashMap<>();
    final Map<String, FriendLabelsClient$Entry> requests = new LinkedHashMap<>();
    final Queue<FriendLabelsClient$LabelMessage> incoming = new ConcurrentLinkedQueue<>();
    final Queue<JsonObject> pendingMessages = new ConcurrentLinkedQueue<>();
    volatile FriendLabelsClient$State state = FriendLabelsClient$State.DISCONNECTED;
    volatile String identity = "";

    FriendLabelsClient() {
    }

    public void connect() {
        if (this.state == FriendLabelsClient$State.DISCONNECTED) {
            this.state = FriendLabelsClient$State.CONNECTING;
            HTTP.newWebSocketBuilder().buildAsync(URI.create(DEFAULT_URL), new FriendLabelsClient$1(this)).whenComplete((ws, error) -> {
                if (error != null) {
                    this.state = FriendLabelsClient$State.DISCONNECTED;
                }
            });
        }
    }

    void disconnected(WebSocket ws) {
        if (this.socket.compareAndSet(ws, null)) {
            this.state = FriendLabelsClient$State.DISCONNECTED;
            this.identity = "";
            this.friends.values().forEach(entry -> entry.online = false);
        }
    }

    public void disconnect() {
        WebSocket ws = this.socket.getAndSet(null);
        if (ws != null) {
            ws.sendClose(1000, "disabled");
        }

        this.state = FriendLabelsClient$State.DISCONNECTED;
        this.identity = "";
    }

    public void ensureConnectedIdentity() {
        String wanted = key(minecraftName());
        if (this.state != FriendLabelsClient$State.DISCONNECTED && !this.identity.isBlank() && !this.identity.equals(wanted)) {
            this.disconnect();
            this.friends.clear();
            this.requests.clear();
            this.incoming.clear();
        }

        if (this.state == FriendLabelsClient$State.DISCONNECTED) {
            this.connect();
        }
    }

    public void refreshIdentity() {
        if (this.state != FriendLabelsClient$State.DISCONNECTED) {
            this.disconnect();
            this.friends.clear();
            this.requests.clear();
            this.incoming.clear();
            this.connect();
        }
    }

    public void requestFriend(String name) {
        this.send(this.simple("friend_request", "to", name));
    }

    public void accept(String login) {
        this.send(this.simple("friend_accept", "from", login));
    }

    public void decline(String login) {
        this.send(this.simple("friend_decline", "from", login));
    }

    public void sendLabel(double x, double y, double z, int color, String dimension, String targetUuid, String targetName) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "label");
        json.addProperty("x", x);
        json.addProperty("y", y);
        json.addProperty("z", z);
        json.addProperty("color", color);
        json.addProperty("dimension", dimension);
        json.addProperty("target_uuid", targetUuid == null ? "" : targetUuid);
        json.addProperty("target_name", targetName == null ? "" : targetName);
        this.send(json);
    }

    JsonObject simple(String type, String key, String value) {
        JsonObject json = new JsonObject();
        json.addProperty("type", type);
        json.addProperty(key, value);
        return json;
    }

    void send(JsonObject json) {
        WebSocket ws = this.socket.get();
        if (ws != null) {
            ws.sendText(json.toString(), true);
        } else {
            this.pendingMessages.add(json.deepCopy());
            if (this.state == FriendLabelsClient$State.DISCONNECTED) {
                this.connect();
            }
        }
    }

    void handle(String raw) {
        try {
            JsonObject json = JsonParser.parseString(raw).getAsJsonObject();
            String type = string(json, "type");
            if ("friends_list".equals(type)) {
                this.friends.clear();

                for (JsonElement value : json.getAsJsonArray("friends")) {
                    put(this.friends, value.getAsJsonObject());
                }
            } else if ("requests_list".equals(type)) {
                this.requests.clear();

                for (JsonElement value : json.getAsJsonArray("requests")) {
                    put(this.requests, value.getAsJsonObject());
                }
            } else if ("friend_presence".equals(type)) {
                FriendLabelsClient$Entry entry = this.friends.get(key(string(json, "login")));
                if (entry != null) {
                    entry.online = json.has("online") && json.get("online").getAsBoolean();
                }
            } else if ("label".equals(type)) {
                this.incoming
                    .add(
                        new FriendLabelsClient$LabelMessage(
                            string(json, "name"),
                            number(json, "x"),
                            number(json, "y"),
                            number(json, "z"),
                            json.has("color") ? json.get("color").getAsInt() : -1,
                            string(json, "dimension"),
                            string(json, "target_uuid"),
                            string(json, "target_name")
                        )
                    );
            }
        } catch (Exception var6) {
        }
    }

    static void put(Map<String, FriendLabelsClient$Entry> map, JsonObject json) {
        FriendLabelsClient$Entry entry = new FriendLabelsClient$Entry(
            string(json, "login"), string(json, "name"), json.has("online") && json.get("online").getAsBoolean()
        );
        if (!entry.login.isBlank()) {
            map.put(key(entry.login), entry);
        }
    }

    public FriendLabelsClient$LabelMessage pollLabel() {
        return this.incoming.poll();
    }

    public List<FriendLabelsClient$Entry> getFriends() {
        return new ArrayList<>(this.friends.values());
    }

    public List<FriendLabelsClient$Entry> getRequests() {
        return new ArrayList<>(this.requests.values());
    }

    public FriendLabelsClient$State getState() {
        return this.state;
    }

    static String string(JsonObject o, String n) {
        return o.has(n) && !o.get(n).isJsonNull() ? o.get(n).getAsString() : "";
    }

    static double number(JsonObject o, String n) {
        try {
            return o.get(n).getAsDouble();
        } catch (Exception var3) {
            return 0.0;
        }
    }

    static String key(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    static String minecraftName() {
        return ClientProfile.getUsername();
    }

    static String accountLogin() {
        return ClientProfile.getUsername();
    }
}
