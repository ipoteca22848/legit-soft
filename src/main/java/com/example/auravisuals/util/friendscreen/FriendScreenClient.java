package com.example.auravisuals.util.friendscreen;

import com.example.auravisuals.AuraVisuals;
import com.example.auravisuals.util.ClientProfile;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.HttpClient.Version;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

public final class FriendScreenClient {
    public static final FriendScreenClient INSTANCE = new FriendScreenClient();
    public static final String[] SERVER_URLS = new String[]{"ws://152.89.199.138:7658", "wss://auravisuals.space/ws/friendscreen"};
    public static final String DEFAULT_URL = SERVER_URLS[0];
    static final int MAX_JPEG_BYTES = 1200000;
    static final HttpClient HTTP = HttpClient.newBuilder().version(Version.HTTP_1_1).build();
    static final ExecutorService FRAME_WORKER = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "Aura-FriendScreen-Frames");
        thread.setDaemon(true);
        return thread;
    });
    final AtomicReference<WebSocket> socket = new AtomicReference<>();
    final Map<String, FriendScreenClient$Entry> friends = new LinkedHashMap<>();
    final Map<String, FriendScreenClient$Entry> requests = new LinkedHashMap<>();
    final Map<String, Identifier> frames = new LinkedHashMap<>();
    final Map<String, int[]> frameSizes = new LinkedHashMap<>();
    final Map<String, NativeImage> pendingFrames = new ConcurrentHashMap<>();
    final Map<String, FriendScreenClient$PlayerInfo> playerInfo = new ConcurrentHashMap<>();
    final Map<String, NativeImageBackedTexture> frameTextures = new LinkedHashMap<>();
    final Set<String> reportedFrameErrors = ConcurrentHashMap.newKeySet();
    final Set<String> confirmedFrames = ConcurrentHashMap.newKeySet();
    final Map<String, FriendScreenClient$StreamQuality> viewerQualities = new ConcurrentHashMap<>();
    final Map<String, FriendScreenClient$StreamView> viewerViews = new ConcurrentHashMap<>();
    final AtomicInteger hudFrameSendPending = new AtomicInteger();
    final AtomicInteger worldFrameSendPending = new AtomicInteger();
    final AtomicBoolean frameDecodeRunning = new AtomicBoolean();
    final AtomicReference<byte[]> latestBinaryFrame = new AtomicReference<>();
    volatile FriendScreenClient$State state = FriendScreenClient$State.DISCONNECTED;
    volatile String authenticatedIdentity = "";
    volatile int activeUrlIndex = 0;
    volatile Runnable onDataChanged;

    FriendScreenClient() {
    }

    public void setOnDataChanged(Runnable onDataChanged) {
        this.onDataChanged = onDataChanged;
    }

    public void notifyDataChanged() {
        Runnable r = this.onDataChanged;
        if (r != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) {
                mc.execute(r);
            } else {
                r.run();
            }
        }
    }

    public void connect() {
        this.connect(SERVER_URLS[this.activeUrlIndex]);
    }

    public synchronized void connect(String url) {
        if (this.state == FriendScreenClient$State.DISCONNECTED) {
            this.state = FriendScreenClient$State.CONNECTING;

            URI uri;
            try {
                uri = URI.create(url);
            } catch (Exception var4) {
                this.state = FriendScreenClient$State.DISCONNECTED;
                this.tryFallback();
                return;
            }

            for (int i = 0; i < SERVER_URLS.length; i++) {
                if (SERVER_URLS[i].equals(url)) {
                    this.activeUrlIndex = i;
                }
            }

            HTTP.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(6L))
                .buildAsync(uri, new FriendScreenClient$1(this))
                .whenComplete((webSocket, error) -> {
                    if (error != null) {
                        this.state = FriendScreenClient$State.DISCONNECTED;
                        this.tryFallback();
                    }
                });
        }
    }

    void tryFallback() {
        if (this.activeUrlIndex + 1 < SERVER_URLS.length) {
            this.activeUrlIndex++;
            this.connect(SERVER_URLS[this.activeUrlIndex]);
        } else {
            this.activeUrlIndex = 0;
        }
    }

    public void disconnect() {
        WebSocket webSocket = this.socket.getAndSet(null);
        if (webSocket != null) {
            webSocket.sendClose(1000, "disabled");
        }

        this.state = FriendScreenClient$State.DISCONNECTED;
        this.authenticatedIdentity = "";
        this.clearFrameSendPending();
        this.notifyDataChanged();
    }

    public void ensureConnectedIdentity() {
        String wanted = key(identity());
        if (this.state != FriendScreenClient$State.DISCONNECTED && !this.authenticatedIdentity.isBlank() && !this.authenticatedIdentity.equals(wanted)) {
            this.disconnect();
            this.clearRemoteState();
        }

        if (this.state == FriendScreenClient$State.DISCONNECTED) {
            this.connect();
        }
    }

    void clearRemoteState() {
        this.friends.clear();
        this.requests.clear();
        this.frames.clear();
        this.frameSizes.clear();
        this.pendingFrames.values().forEach(NativeImage::close);
        this.pendingFrames.clear();
        this.playerInfo.clear();
        this.reportedFrameErrors.clear();
        this.confirmedFrames.clear();
        this.viewerQualities.clear();
        this.viewerViews.clear();
        this.notifyDataChanged();
    }

    public void sendFriendRequest(String name) {
        if (name != null && !name.isBlank()) {
            this.ensureConnectedIdentity();
            JsonObject message = new JsonObject();
            message.addProperty("type", "friend_request");
            message.addProperty("to", name.trim());
            this.send(message);
        }
    }

    public void accept(String login) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "friend_accept");
        message.addProperty("from", login);
        this.send(message);
        this.requests.remove(key(login));
        this.notifyDataChanged();
    }

    public void decline(String login) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "friend_decline");
        message.addProperty("from", login);
        this.send(message);
        this.requests.remove(key(login));
        this.notifyDataChanged();
    }

    public boolean sendFrame(byte[] jpeg, int width, int height, JsonObject player, FriendScreenClient$StreamView view) {
        if (jpeg != null && jpeg.length != 0 && jpeg.length <= 1200000) {
            WebSocket webSocket = this.socket.get();
            FriendScreenClient$StreamView actualView = view == null ? FriendScreenClient$StreamView.HUD : view;
            AtomicInteger pending = this.framePending(actualView);
            if (webSocket != null && acquireFrameSlot(pending)) {
                try {
                    JsonObject header = new JsonObject();
                    header.addProperty("type", "screen_frame");
                    header.addProperty("width", width);
                    header.addProperty("height", height);
                    header.addProperty("view", actualView.name());
                    if (player != null) {
                        header.add("player", player);
                    }

                    byte[] headerBytes = header.toString().getBytes(StandardCharsets.UTF_8);
                    ByteBuffer packet = ByteBuffer.allocate(4 + headerBytes.length + jpeg.length);
                    packet.putInt(headerBytes.length).put(headerBytes).put(jpeg).flip();
                    webSocket.sendBinary(packet, true).whenComplete((ignoredx, error) -> pending.decrementAndGet());
                    return true;
                } catch (Throwable var12) {
                    pending.decrementAndGet();
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public boolean canSendFrame(FriendScreenClient$StreamView view) {
        return this.state == FriendScreenClient$State.CONNECTED && this.socket.get() != null && this.framePending(view).get() < 2;
    }

    AtomicInteger framePending(FriendScreenClient$StreamView view) {
        return view == FriendScreenClient$StreamView.WORLD ? this.worldFrameSendPending : this.hudFrameSendPending;
    }

    static boolean acquireFrameSlot(AtomicInteger pending) {
        int current;
        do {
            current = pending.get();
            if (current >= 2) {
                return false;
            }
        } while (!pending.compareAndSet(current, current + 1));

        return true;
    }

    void clearFrameSendPending() {
        this.hudFrameSendPending.set(0);
        this.worldFrameSendPending.set(0);
    }

    boolean send(JsonObject message) {
        WebSocket webSocket = this.socket.get();
        if (webSocket == null) {
            return false;
        }

        try {
            webSocket.sendText(message.toString(), true);
            return true;
        } catch (Exception var4) {
            return false;
        }
    }

    void dispatch(String raw) {
        FRAME_WORKER.execute(() -> {
            try {
                JsonObject message = JsonParser.parseString(raw).getAsJsonObject();
                if ("screen_frame".equals(string(message, "type"))) {
                    this.receiveFrame(message);
                } else {
                    MinecraftClient.getInstance().execute(() -> this.handle(message));
                }
            } catch (Exception var3) {
            }
        });
    }

    void dispatchBinaryFrame(byte[] packet) {
        if (packet != null && packet.length >= 7 && packet.length <= 1264000) {
            this.latestBinaryFrame.set(packet);
            if (this.frameDecodeRunning.compareAndSet(false, true)) {
                FRAME_WORKER.execute(() -> {
                    byte[] latest;
                    try {
                        while ((latest = this.latestBinaryFrame.getAndSet(null)) != null) {
                            this.receiveBinaryFrame(latest);
                        }
                    } finally {
                        this.frameDecodeRunning.set(false);
                        if (this.latestBinaryFrame.get() != null) {
                            this.dispatchBinaryFrame(this.latestBinaryFrame.getAndSet(null));
                        }
                    }
                });
            }
        }
    }

    void receiveBinaryFrame(byte[] packet) {
        try {
            ByteBuffer input = ByteBuffer.wrap(packet);
            int headerLength = input.getInt();
            if (headerLength <= 0 || headerLength > 64000 || headerLength > input.remaining()) {
                return;
            }

            byte[] headerBytes = new byte[headerLength];
            input.get(headerBytes);
            JsonObject header = JsonParser.parseString(new String(headerBytes, StandardCharsets.UTF_8)).getAsJsonObject();
            String from = key(string(header, "from"));
            if (from.isBlank() || input.remaining() <= 0 || input.remaining() > 1200000) {
                return;
            }

            if (header.has("player") && header.get("player").isJsonObject()) {
                this.playerInfo.put(from, parsePlayerInfo(header.getAsJsonObject("player")));
            }

            byte[] jpeg = new byte[input.remaining()];
            input.get(jpeg);
            NativeImage decoded = decodeJpeg(jpeg);
            NativeImage previous = this.pendingFrames.put(from, decoded);
            if (previous != null) {
                previous.close();
            }
        } catch (Throwable var10) {
            this.reportFrameError("binary", var10);
        }
    }

    void handle(JsonObject message) {
        try {
            String type = string(message, "type");
            switch (type) {
                case "friends_list":
                    this.friends.clear();
                    if (message.has("friends")) {
                        for (JsonElement element : message.getAsJsonArray("friends")) {
                            putEntry(this.friends, element.getAsJsonObject());
                        }
                    }

                    this.notifyDataChanged();
                    break;
                case "requests_list":
                    this.requests.clear();
                    if (message.has("requests")) {
                        for (JsonElement element : message.getAsJsonArray("requests")) {
                            putEntry(this.requests, element.getAsJsonObject());
                        }
                    }

                    this.notifyDataChanged();
                    break;
                case "request_incoming":
                    putEntry(this.requests, message);
                    this.notifyDataChanged();
                    break;
                case "friend_accepted": {
                    FriendScreenClient$Entry entry = entry(message);
                    entry.online = true;
                    this.friends.put(key(entry.login), entry);
                    this.requests.remove(key(entry.login));
                    this.notifyDataChanged();
                    break;
                }
                case "friend_presence": {
                    String login = string(message, "login");
                    FriendScreenClient$Entry entry = this.friends.get(key(login));
                    if (entry != null) {
                        entry.online = message.has("online") && message.get("online").getAsBoolean();
                        String name = string(message, "name");
                        if (!name.isBlank()) {
                            entry.name = name;
                        }

                        this.notifyDataChanged();
                    }
                    break;
                }
                case "quality_request":
                    String fromx = key(string(message, "from"));
                    if (!fromx.isBlank()) {
                        this.viewerQualities.put(fromx, FriendScreenClient$StreamQuality.parse(string(message, "quality")));
                    }
                    break;
                case "view_request":
                    String from = key(string(message, "from"));
                    if (!from.isBlank()) {
                        this.viewerViews.put(from, FriendScreenClient$StreamView.parse(string(message, "view")));
                    }
            }
        } catch (Exception var8) {
        }
    }

    void receiveFrame(JsonObject message) {
        String from = key(string(message, "from"));
        if (message.has("data") && !from.isBlank()) {
            try {
                if (message.has("player") && message.get("player").isJsonObject()) {
                    this.playerInfo.put(from, parsePlayerInfo(message.getAsJsonObject("player")));
                }

                byte[] data = Base64.getDecoder().decode(message.get("data").getAsString());
                if (data.length == 0 || data.length > 1200000) {
                    return;
                }

                NativeImage decoded = decodeJpeg(data);
                NativeImage previous = this.pendingFrames.put(from, decoded);
                if (previous != null) {
                    previous.close();
                }
            } catch (Throwable var6) {
                this.reportFrameError(from, var6);
            }
        }
    }

    public void uploadPendingFrames() {
        if (!this.pendingFrames.isEmpty()) {
            for (String from : new ArrayList<>(this.pendingFrames.keySet())) {
                NativeImage image = this.pendingFrames.remove(from);
                if (image != null) {
                    if (this.friends.containsKey(from)) {
                        this.uploadFrame(from, image);
                    } else {
                        image.close();
                    }
                }
            }
        }
    }

    void uploadFrame(String from, NativeImage image) {
        try {
            int width = image.getWidth();
            int height = image.getHeight();
            Identifier identifier = Identifier.of("auravisuals", "friend_screen/" + Integer.toUnsignedString(from.hashCode()));
            NativeImageBackedTexture texture = this.frameTextures.get(from);
            int[] previousSize = this.frameSizes.get(from);
            if (texture != null && previousSize != null && previousSize[0] == width && previousSize[1] == height) {
                texture.setImage(image);
                image = null;
                texture.upload();
            } else {
                NativeImageBackedTexture replacement = new NativeImageBackedTexture(() -> "FriendScreen " + from, image);
                image = null;
                MinecraftClient.getInstance().getTextureManager().registerTexture(identifier, replacement);
                replacement.upload();
                this.frameTextures.put(from, replacement);
            }

            this.frames.put(from, identifier);
            this.frameSizes.put(from, new int[]{width, height});
            this.reportedFrameErrors.remove(from);
            if (this.confirmedFrames.add(from)) {
                JsonObject received = new JsonObject();
                received.addProperty("type", "frame_received");
                received.addProperty("from", from);
                this.send(received);
            }
        } catch (Throwable var9) {
            if (image != null) {
                image.close();
            }

            this.reportFrameError(from, var9);
        }
    }

    static NativeImage decodeJpeg(byte[] data) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(data);

        BufferedImage buffered;
        try {
            buffered = ImageIO.read(input);
        } catch (Throwable var6) {
            try {
                input.close();
            } catch (Throwable var5) {
                var6.addSuppressed(var5);
            }

            throw var6;
        }

        input.close();
        if (buffered != null && buffered.getWidth() > 0 && buffered.getHeight() > 0) {
            NativeImage image = new NativeImage(buffered.getWidth(), buffered.getHeight(), false);

            try {
                for (int y = 0; y < buffered.getHeight(); y++) {
                    for (int x = 0; x < buffered.getWidth(); x++) {
                        image.setColorArgb(x, y, buffered.getRGB(x, y));
                    }
                }

                return image;
            } catch (Throwable var7) {
                image.close();
                throw var7;
            }
        } else {
            throw new IOException("Unsupported FriendScreen image");
        }
    }

    static FriendScreenClient$PlayerInfo parsePlayerInfo(JsonObject object) {
        String[] ids = new String[9];
        int[] counts = new int[9];
        if (object.has("hotbar") && object.get("hotbar").isJsonArray()) {
            int size = Math.min(9, object.getAsJsonArray("hotbar").size());

            for (int i = 0; i < size; i++) {
                JsonElement element = object.getAsJsonArray("hotbar").get(i);
                if (element.isJsonObject()) {
                    JsonObject slot = element.getAsJsonObject();
                    ids[i] = string(slot, "id");
                    counts[i] = slot.has("count") ? Math.max(0, slot.get("count").getAsInt()) : 0;
                }
            }
        }

        String[] equipmentIds = new String[6];
        int[] equipmentCounts = new int[6];
        String[][] equipmentTooltips = new String[6][];
        if (object.has("equipment") && object.get("equipment").isJsonArray()) {
            int size = Math.min(6, object.getAsJsonArray("equipment").size());

            for (int ix = 0; ix < size; ix++) {
                JsonElement element = object.getAsJsonArray("equipment").get(ix);
                if (element.isJsonObject()) {
                    JsonObject slot = element.getAsJsonObject();
                    equipmentIds[ix] = string(slot, "id");
                    equipmentCounts[ix] = slot.has("count") ? Math.max(0, slot.get("count").getAsInt()) : 0;
                    equipmentTooltips[ix] = tooltip(slot);
                }
            }
        }

        String[] inventoryIds = new String[36];
        int[] inventoryCounts = new int[36];
        String[][] inventoryTooltips = new String[36][];
        if (object.has("inventory") && object.get("inventory").isJsonArray()) {
            int size = Math.min(36, object.getAsJsonArray("inventory").size());

            for (int ixx = 0; ixx < size; ixx++) {
                JsonElement element = object.getAsJsonArray("inventory").get(ixx);
                if (element.isJsonObject()) {
                    JsonObject slot = element.getAsJsonObject();
                    inventoryIds[ixx] = string(slot, "id");
                    inventoryCounts[ixx] = slot.has("count") ? Math.max(0, slot.get("count").getAsInt()) : 0;
                    inventoryTooltips[ixx] = tooltip(slot);
                }
            }
        }

        return new FriendScreenClient$PlayerInfo(
            number(object, "x"),
            number(object, "y"),
            number(object, "z"),
            (float)number(object, "health"),
            (float)number(object, "max_health"),
            object.has("selected") ? Math.max(0, Math.min(8, object.get("selected").getAsInt())) : 0,
            ids,
            counts,
            equipmentIds,
            equipmentCounts,
            inventoryIds,
            inventoryCounts,
            equipmentTooltips,
            inventoryTooltips
        );
    }

    static String[] tooltip(JsonObject slot) {
        if (slot.has("tooltip") && slot.get("tooltip").isJsonArray()) {
            int size = Math.min(24, slot.getAsJsonArray("tooltip").size());
            String[] lines = new String[size];

            for (int i = 0; i < size; i++) {
                lines[i] = slot.getAsJsonArray("tooltip").get(i).getAsString();
            }

            return lines;
        } else {
            return new String[0];
        }
    }

    void reportFrameError(String from, Throwable error) {
        if (this.reportedFrameErrors.add(from)) {
            AuraVisuals.LOGGER.warn("FriendScreen could not display a frame from {}", from, error);
        }
    }

    static void putEntry(Map<String, FriendScreenClient$Entry> target, JsonObject object) {
        FriendScreenClient$Entry entry = entry(object);
        if (!entry.login.isBlank()) {
            target.put(key(entry.login), entry);
        }
    }

    static FriendScreenClient$Entry entry(JsonObject object) {
        String login = string(object, "login");
        String name = string(object, "name");
        boolean online = object.has("online") && object.get("online").getAsBoolean();
        return new FriendScreenClient$Entry(login, name, online);
    }

    void markOffline() {
        MinecraftClient.getInstance().execute(() -> this.friends.values().forEach(entry -> entry.online = false));
    }

    public List<FriendScreenClient$Entry> getFriends() {
        return new ArrayList<>(this.friends.values());
    }

    public List<FriendScreenClient$Entry> getRequests() {
        return new ArrayList<>(this.requests.values());
    }

    public Identifier getFrame(String login) {
        return this.frames.get(key(login));
    }

    public int[] getFrameSize(String login) {
        return this.frameSizes.get(key(login));
    }

    public FriendScreenClient$PlayerInfo getPlayerInfo(String login) {
        return this.playerInfo.get(key(login));
    }

    public void requestQuality(String login, FriendScreenClient$StreamQuality quality) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "quality_request");
        message.addProperty("to", login);
        message.addProperty("quality", (quality == null ? FriendScreenClient$StreamQuality.LOW : quality).name());
        this.send(message);
    }

    public void requestView(String login, FriendScreenClient$StreamView view) {
        JsonObject message = new JsonObject();
        message.addProperty("type", "view_request");
        message.addProperty("to", login);
        message.addProperty("view", (view == null ? FriendScreenClient$StreamView.WORLD : view).name());
        this.send(message);
    }

    public boolean needsCaptureView(FriendScreenClient$StreamView view) {
        FriendScreenClient$StreamView actual = view == null ? FriendScreenClient$StreamView.WORLD : view;
        return this.viewerViews.isEmpty() ? actual == FriendScreenClient$StreamView.WORLD : this.viewerViews.containsValue(actual);
    }

    public FriendScreenClient$StreamQuality getCaptureQuality() {
        FriendScreenClient$StreamQuality result = FriendScreenClient$StreamQuality.LOW;

        for (FriendScreenClient$StreamQuality value : this.viewerQualities.values()) {
            if (value.ordinal() > result.ordinal()) {
                result = value;
            }
        }

        return result;
    }

    public FriendScreenClient$State getState() {
        return this.state;
    }

    static String accountLogin() {
        String login = ClientProfile.getUsername();
        return login != null && !login.isBlank() ? login.trim() : minecraftName();
    }

    public void refreshIdentity() {
        if (this.state != FriendScreenClient$State.DISCONNECTED) {
            this.disconnect();
            this.clearRemoteState();
            this.connect();
        }
    }

    static String identity() {
        return ClientProfile.getUsername();
    }

    static String minecraftName() {
        return ClientProfile.getUsername();
    }

    static String string(JsonObject object, String name) {
        return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsString() : "";
    }

    static double number(JsonObject object, String name) {
        try {
            return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsDouble() : 0.0;
        } catch (Exception var3) {
            return 0.0;
        }
    }

    static String key(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    static {
        Thread daemon = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(4000L);
                    INSTANCE.ensureConnectedIdentity();
                } catch (Throwable var1) {
                }
            }
        }, "Aura-FriendScreen-KeepAlive");
        daemon.setDaemon(true);
        daemon.start();
    }
}
