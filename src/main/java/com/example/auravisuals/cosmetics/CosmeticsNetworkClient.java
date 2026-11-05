package com.example.auravisuals.cosmetics;

import com.example.auravisuals.emotions.EmotionWheelManager;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.JumpCircle;
import com.example.auravisuals.util.ClientProfile;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.HttpClient.Version;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.Vec3d;

public final class CosmeticsNetworkClient {
    public static final CosmeticsNetworkClient INSTANCE = new CosmeticsNetworkClient();
    public static final String[] SERVER_URLS = new String[]{"ws://152.89.199.138:7660", "wss://auravisuals.space/ws/cosmetics"};
    int currentUrlIndex = 0;
    volatile String pendingEmotion = null;
    static final HttpClient HTTP = HttpClient.newBuilder().version(Version.HTTP_1_1).build();
    final AtomicReference<WebSocket> socketRef = new AtomicReference<>();
    final AtomicBoolean connecting = new AtomicBoolean(false);
    long lastReconnectAttempt = 0L;
    long lastPingMs = 0L;
    volatile String lastWorldScope = "";
    final Map<String, String> pendingEquips = new ConcurrentHashMap<>();
    long lastFlushTime = 0L;

    CosmeticsNetworkClient() {
    }

    public void init() {
        this.connect();
    }

    public void tick() {
        long now = System.currentTimeMillis();
        if (this.socketRef.get() == null && !this.connecting.get()) {
            if (now - this.lastReconnectAttempt > 5000L) {
                this.lastReconnectAttempt = now;
                this.connect();
            }
        } else if (this.socketRef.get() != null) {
            if (now - this.lastPingMs > 20000L) {
                this.lastPingMs = now;
                this.sendPing();
            }

            String scope = this.getWorldScope();
            if (!scope.equals(this.lastWorldScope)) {
                this.sendHello();
            }
        }

        if (!this.pendingEquips.isEmpty() && now - this.lastFlushTime >= 300L) {
            this.flushPendingEquips();
            this.lastFlushTime = now;
        }
    }

    public void shutdown() {
        WebSocket ws=this.socketRef.getAndSet(null);
        if(ws!=null) ws.abort();
        this.pendingEquips.clear();
    }

    public synchronized void connect() {
        if (!com.bamber.client.BamberRuntime.isActive()) return;
        if (!this.connecting.get()) {
            WebSocket current = this.socketRef.get();
            if (current == null) {
                this.connecting.set(true);

                try {
                    URI uri = URI.create(SERVER_URLS[this.currentUrlIndex]);
                    HTTP.newWebSocketBuilder().buildAsync(uri, new CosmeticsNetworkClient$1(this)).whenComplete((ws, err) -> {
                        this.connecting.set(false);
                        if (err != null) {
                            this.socketRef.set(null);
                            this.currentUrlIndex = (this.currentUrlIndex + 1) % SERVER_URLS.length;
                        }
                    });
                } catch (Throwable var3) {
                    this.connecting.set(false);
                    this.currentUrlIndex = (this.currentUrlIndex + 1) % SERVER_URLS.length;
                }
            }
        }
    }

    public void sendHello() {
        WebSocket ws = this.socketRef.get();
        if (ws == null) {
            this.connect();
        } else {
            String name = this.getPlayerName();
            if (!name.isEmpty()) {
                JsonObject o = new JsonObject();
                o.addProperty("type", "hello");
                o.addProperty("name", name);
                String scope = this.getWorldScope();
                this.lastWorldScope = scope;
                o.addProperty("scope", scope);
                JsonObject eqObj = new JsonObject();

                for (Entry<String, CosmeticItem> entry : CosmeticsManager.INSTANCE.getEquippedMap().entrySet()) {
                    if (entry.getValue() != null) {
                        eqObj.addProperty(entry.getKey(), entry.getValue().getId());
                    }
                }

                o.add("equipped", eqObj);

                try {
                    ws.sendText(o.toString(), true);
                } catch (Exception var8) {
                }
            }
        }
    }

    public void requestSync() {
        WebSocket ws = this.socketRef.get();
        if (ws != null) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "request_sync");

            try {
                ws.sendText(o.toString(), true);
            } catch (Exception var4) {
            }
        }
    }

    void sendPing() {
        WebSocket ws = this.socketRef.get();
        if (ws != null) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "ping");

            try {
                ws.sendText(o.toString(), true);
            } catch (Exception var4) {
            }
        }
    }

    public void sendEmotion(String emotionId) {
        if (emotionId != null && !emotionId.isEmpty()) {
            WebSocket ws = this.socketRef.get();
            if (ws == null) {
                this.pendingEmotion = emotionId;
                this.connect();
            } else {
                String name = this.getPlayerName();
                if (!name.isEmpty()) {
                    JsonObject o1 = new JsonObject();
                    o1.addProperty("type", "emotion");
                    o1.addProperty("name", name);
                    o1.addProperty("emotion", emotionId);

                    try {
                        ws.sendText(o1.toString(), true);
                    } catch (Exception var8) {
                    }

                    JsonObject o2 = new JsonObject();
                    o2.addProperty("type", "equip");
                    o2.addProperty("name", name);
                    o2.addProperty("category", "emotion");
                    o2.addProperty("id", emotionId);

                    try {
                        ws.sendText(o2.toString(), true);
                    } catch (Exception var7) {
                    }
                }
            }
        }
    }

    public void sendEquip(String category, String cosmeticId) {
        if (category != null && !category.isEmpty()) {
            this.pendingEquips.put(category, cosmeticId != null ? cosmeticId : "");
        }
    }

    public void sendJumpCircle(Vec3d position, JsonObject style) {
        if (position != null && style != null) {
            WebSocket ws = this.socketRef.get();
            if (ws == null) {
                this.connect();
            } else {
                String name = this.getPlayerName();
                String scope = this.getWorldScope();
                if (!name.isEmpty() && !scope.isEmpty()) {
                    JsonObject packet = new JsonObject();
                    packet.addProperty("type", "jump_circle");
                    packet.addProperty("name", name);
                    packet.addProperty("scope", scope);
                    packet.addProperty("x", position.x);
                    packet.addProperty("y", position.y);
                    packet.addProperty("z", position.z);
                    packet.add("style", style.deepCopy());

                    try {
                        ws.sendText(packet.toString(), true);
                    } catch (Exception var8) {
                    }
                }
            }
        }
    }

    void flushPendingEquips() {
        WebSocket ws = this.socketRef.get();
        if (ws == null) {
            this.connect();
        } else {
            String name = this.getPlayerName();
            if (!name.isEmpty()) {
                Map<String, String> copy = new HashMap<>(this.pendingEquips);
                this.pendingEquips.clear();

                for (Entry<String, String> entry : copy.entrySet()) {
                    JsonObject o = new JsonObject();
                    o.addProperty("type", "equip");
                    o.addProperty("name", name);
                    o.addProperty("category", entry.getKey());
                    o.addProperty("id", entry.getValue());

                    try {
                        ws.sendText(o.toString(), true);
                    } catch (Exception var8) {
                    }
                }
            }
        }
    }

    void handleMessage(String raw) {
        try {
            JsonElement root = JsonParser.parseString(raw);
            if (!root.isJsonObject()) {
                return;
            }

            JsonObject obj = root.getAsJsonObject();
            String type = obj.has("type") ? obj.get("type").getAsString() : "";
            if ("cosmetics_sync".equals(type) && obj.has("users") && obj.get("users").isJsonObject()) {
                JsonObject users = obj.getAsJsonObject("users");
                Map<String, Map<String, String>> data = new HashMap<>();

                for (String user : users.keySet()) {
                    JsonElement userEl = users.get(user);
                    if (userEl.isJsonObject()) {
                        JsonObject catMap = userEl.getAsJsonObject();
                        Map<String, String> cMap = new HashMap<>();

                        for (String cat : catMap.keySet()) {
                            cMap.put(cat, catMap.get(cat).getAsString());
                        }

                        data.put(user.toLowerCase(), cMap);
                    }
                }

                CosmeticsManager.INSTANCE.setAllPlayerCosmetics(data);
            } else if ("user_cosmetics".equals(type)) {
                String name = obj.has("name") ? obj.get("name").getAsString() : "";
                if (!name.isEmpty()) {
                    if (obj.has("category") && obj.has("id")) {
                        String cat = obj.get("category").getAsString();
                        String id = obj.get("id").getAsString();
                        if ("emotion".equalsIgnoreCase(cat)) {
                            EmotionWheelManager.INSTANCE.playRemoteEmotion(name, id);
                        } else {
                            CosmeticsManager.INSTANCE.setPlayerCosmetic(name, cat, id);
                        }
                    } else if (obj.has("equipped") && obj.get("equipped").isJsonObject()) {
                        JsonObject eq = obj.getAsJsonObject("equipped");

                        for (String cat : eq.keySet()) {
                            if ("emotion".equalsIgnoreCase(cat)) {
                                EmotionWheelManager.INSTANCE.playRemoteEmotion(name, eq.get(cat).getAsString());
                            } else {
                                CosmeticsManager.INSTANCE.setPlayerCosmetic(name, cat, eq.get(cat).getAsString());
                            }
                        }
                    }
                }
            } else if ("emotion".equals(type)) {
                String name = obj.has("name") ? obj.get("name").getAsString() : "";
                String emotion = obj.has("emotion") ? obj.get("emotion").getAsString() : "";
                if (!name.isEmpty() && !emotion.isEmpty()) {
                    EmotionWheelManager.INSTANCE.playRemoteEmotion(name, emotion);
                }
            } else if ("jump_circle".equals(type) && obj.has("style") && obj.get("style").isJsonObject()) {
                String name = obj.has("name") ? obj.get("name").getAsString() : "";
                double x = obj.has("x") ? obj.get("x").getAsDouble() : Double.NaN;
                double y = obj.has("y") ? obj.get("y").getAsDouble() : Double.NaN;
                double z = obj.has("z") ? obj.get("z").getAsDouble() : Double.NaN;
                if (!name.isEmpty() && Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z)) {
                    JumpCircle module = ModuleManager.INSTANCE.getModule(JumpCircle.class);
                    if (module != null) {
                        module.onRemoteJump(name, new Vec3d(x, y, z), obj.getAsJsonObject("style"));
                    }
                }
            }
        } catch (Exception var14) {
        }
    }

    public String getPlayerName() {
        String profileName = ClientProfile.getUsername();
        if (profileName != null && !profileName.isBlank() && !profileName.equalsIgnoreCase("User")) {
            return profileName.trim();
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.player != null) {
            if (mc.player.getGameProfile() != null && mc.player.getGameProfile().name() != null) {
                String name = mc.player.getGameProfile().name();
                if (!name.isBlank()) {
                    return name.trim();
                }
            }

            if (mc.player.getName() != null) {
                String name = mc.player.getName().getString();
                if (!name.isBlank()) {
                    return name.trim();
                }
            }
        }

        return mc != null && mc.getSession() != null && mc.getSession().getUsername() != null && !mc.getSession().getUsername().isBlank()
            ? mc.getSession().getUsername().trim()
            : "User";
    }

    String getWorldScope() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.world != null && mc.getCurrentServerEntry() != null) {
            String address = mc.getCurrentServerEntry().address;
            if (address != null && !address.isBlank()) {
                String dimension = mc.world.getRegistryKey().getValue().toString().toLowerCase(Locale.ROOT);
                return address.trim().toLowerCase(Locale.ROOT) + "|" + dimension;
            } else {
                return "";
            }
        } else {
            return "";
        }
    }
}
