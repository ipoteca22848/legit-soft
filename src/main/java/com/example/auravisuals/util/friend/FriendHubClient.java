package com.example.auravisuals.util.friend;

import com.example.auravisuals.client.gui.IrcChatMessengerScreen;
import com.example.auravisuals.util.ClientProfile;
import com.example.auravisuals.util.IrcIdentity;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpClient.Version;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.network.ClientConnection;
import net.minecraft.util.Identifier;

public final class FriendHubClient {
    public static final FriendHubClient INSTANCE = new FriendHubClient();
    public static final String[] SERVER_URLS = new String[]{"ws://152.89.199.138:7657", "wss://auravisuals.space/ws/friendhub"};
    public static final String DEFAULT_URL = SERVER_URLS[0];
    public static boolean hideMyStatusFromFriends = false;
    public static boolean hideGameServerFromFriends = false;
    public static boolean showMusicPresence = false;
    volatile FriendHubClient$State state = FriendHubClient$State.DISCONNECTED;
    volatile int activeUrlIndex = 0;
    volatile boolean allowFallback = true;
    volatile String lastConnectionError = "";
    volatile String authenticatedLogin = "";
    volatile boolean initialFriendsLoaded = false;
    volatile boolean initialGroupsLoaded = false;
    volatile boolean initialDmPartnersLoaded = false;
    final FriendHubConnection connection = new FriendHubConnection();
    final Map<String, FriendHubClient$FriendEntry> friendsMap = new LinkedHashMap<>();
    final Map<String, FriendHubClient$FriendEntry> presenceCache = new LinkedHashMap<>();
    final Map<String, FriendHubClient$FriendEntry> incomingRequests = new LinkedHashMap<>();
    final Map<String, FriendHubClient$GroupInviteEntry> incomingGroupInvites = new LinkedHashMap<>();
    final Map<String, FriendHubClient$GroupData> groupsMap = new LinkedHashMap<>();
    final Map<String, List<FriendHubClient$DmMessage>> dmThreads = new LinkedHashMap<>();
    final List<FriendHubClient$FriendEntry> dmPartners = new ArrayList<>();
    static final HttpClient HTTP = HttpClient.newBuilder().version(Version.HTTP_1_1).build();
    final Map<String, Identifier> avatarCache = new HashMap<>();
    final Map<String, int[]> avatarSizes = new HashMap<>();
    final Set<String> pendingAvs = new HashSet<>();
    volatile Consumer<FriendHubClient$FriendEntry> onFriendRequestIncoming;
    volatile Consumer<FriendHubClient$GroupInviteEntry> onGroupInviteIncoming;
    volatile Consumer<FriendHubClient$GroupPing> onGroupPingIncoming;
    volatile Consumer<String> onHubRequestResult;
    volatile Runnable onDataChanged;

    FriendHubClient() {
        this.connection.setOnIncoming(this::handleIncoming);
        this.connection.setOnConnected(() -> {
            this.state = FriendHubClient$State.CONNECTED;
            this.lastConnectionError = "";
            this.initialFriendsLoaded = false;
            this.initialGroupsLoaded = false;
            this.initialDmPartnersLoaded = false;
            this.notifyChanged();
        });
        this.connection.setOnDisconnected(() -> {
            this.state = FriendHubClient$State.DISCONNECTED;
            this.initialFriendsLoaded = false;
            this.initialGroupsLoaded = false;
            this.initialDmPartnersLoaded = false;
            this.notifyChanged();
            if (this.allowFallback && this.activeUrlIndex + 1 < SERVER_URLS.length) {
                this.activeUrlIndex++;
                this.connect(SERVER_URLS[this.activeUrlIndex]);
            } else if (this.allowFallback) {
                this.lastConnectionError = "Сервер друзей недоступен";
            }
        });
    }

    public void connect(String url) {
        if (this.state == FriendHubClient$State.DISCONNECTED) {
            for (int index = 0; index < SERVER_URLS.length; index++) {
                if (SERVER_URLS[index].equals(url)) {
                    this.activeUrlIndex = index;
                }
            }

            this.allowFallback = true;
            this.lastConnectionError = "";
            this.state = FriendHubClient$State.CONNECTING;
            this.initialFriendsLoaded = false;
            this.initialGroupsLoaded = false;
            this.initialDmPartnersLoaded = false;
            String login = IrcIdentity.getLogin();
            this.authenticatedLogin = login;
            String avatarUrl = ClientProfile.getAvatarUrl();
            this.connection.connect(url, login, avatarUrl);
        }
    }

    public void disconnect() {
        this.allowFallback = false;
        this.connection.disconnect();
        this.state = FriendHubClient$State.DISCONNECTED;
    }

    public void sendFriendRequest(String toLogin) {
        if (this.state == FriendHubClient$State.CONNECTED && toLogin != null && !toLogin.isBlank()) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "friend_request");
            o.addProperty("to", toLogin.trim());
            this.connection.sendJson(o);
        }
    }

    public void acceptFriendRequest(String fromLogin) {
        if (this.state == FriendHubClient$State.CONNECTED) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "friend_accept");
            o.addProperty("from", fromLogin);
            this.connection.sendJson(o);
            this.incomingRequests.remove(fromLogin.toLowerCase());
            this.notifyChanged();
        }
    }

    public void declineFriendRequest(String fromLogin) {
        if (this.state == FriendHubClient$State.CONNECTED) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "friend_decline");
            o.addProperty("from", fromLogin);
            this.connection.sendJson(o);
            this.incomingRequests.remove(fromLogin.toLowerCase());
            this.notifyChanged();
        }
    }

    public void removeFriend(String login) {
        if (this.state == FriendHubClient$State.CONNECTED) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "friend_remove");
            o.addProperty("login", login);
            this.connection.sendJson(o);
            this.friendsMap.remove(login.toLowerCase());
            this.notifyChanged();
        }
    }

    public void createGroup(String name) {
        if (this.state == FriendHubClient$State.CONNECTED && name != null && !name.isBlank()) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "group_create");
            o.addProperty("name", name.trim());
            this.connection.sendJson(o);
        }
    }

    public void inviteToGroup(String groupId, String toLogin) {
        if (this.state == FriendHubClient$State.CONNECTED) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "group_invite");
            o.addProperty("group_id", groupId);
            o.addProperty("to", toLogin);
            this.connection.sendJson(o);
        }
    }

    public void acceptGroupInvite(String groupId, String fromLogin) {
        if (this.state == FriendHubClient$State.CONNECTED) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "group_invite_accept");
            o.addProperty("group_id", groupId);
            o.addProperty("from", fromLogin);
            this.connection.sendJson(o);
            this.incomingGroupInvites.remove(FriendHubClient$GroupInviteEntry.key(groupId, fromLogin));
            this.notifyChanged();
        }
    }

    public void declineGroupInvite(String groupId, String fromLogin) {
        if (this.state == FriendHubClient$State.CONNECTED) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "group_invite_decline");
            o.addProperty("group_id", groupId);
            o.addProperty("from", fromLogin);
            this.connection.sendJson(o);
            this.incomingGroupInvites.remove(FriendHubClient$GroupInviteEntry.key(groupId, fromLogin));
            this.notifyChanged();
        }
    }

    public void kickGroupMember(String groupId, String targetLogin) {
        if (this.state == FriendHubClient$State.CONNECTED) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "group_kick");
            o.addProperty("group_id", groupId);
            o.addProperty("target", targetLogin);
            this.connection.sendJson(o);
            FriendHubClient$GroupData group = this.groupsMap.get(groupId);
            if (group != null) {
                group.members.removeIf(member -> member != null && member.equalsIgnoreCase(targetLogin));
                this.notifyChanged();
            }
        }
    }

    public void sendGroupMessage(String groupId, String message) {
        if (this.state == FriendHubClient$State.CONNECTED && message != null && !message.isBlank()) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "group_message");
            o.addProperty("group_id", groupId);
            o.addProperty("message", message.length() > 40 ? message.substring(0, 40) : message);
            this.connection.sendJson(o);
        }
    }

    public void sendGroupPing(String label, String senderName, double x, double y, double z) {
        if (this.state == FriendHubClient$State.CONNECTED) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "group_ping");
            o.addProperty("label", label != null && !label.isBlank() ? label.trim() : "Ping");
            o.addProperty("sender_name", senderName == null ? "" : senderName.trim());
            o.addProperty("x", x);
            o.addProperty("y", y);
            o.addProperty("z", z);
            this.connection.sendJson(o);
        }
    }

    public void leaveGroup(String groupId) {
        if (this.state == FriendHubClient$State.CONNECTED) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "group_leave");
            o.addProperty("group_id", groupId);
            this.connection.sendJson(o);
            this.groupsMap.remove(groupId);
            this.notifyChanged();
        }
    }

    public void deleteGroup(String groupId) {
        if (this.state == FriendHubClient$State.CONNECTED) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "group_delete");
            o.addProperty("group_id", groupId);
            this.connection.sendJson(o);
            this.groupsMap.remove(groupId);
            this.notifyChanged();
        }
    }

    public void updateGroupAvatar(String groupId, byte[] pngBytes) {
        if (this.state == FriendHubClient$State.CONNECTED && groupId != null && pngBytes != null && pngBytes.length != 0) {
            String avatarData = "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes);
            FriendHubClient$GroupData group = this.groupsMap.get(groupId);
            if (group != null) {
                group.avatarUrl = avatarData;
                this.loadAvatar(avatarData);
                this.notifyChanged();
            }

            JsonObject o = new JsonObject();
            o.addProperty("type", "group_avatar_update");
            o.addProperty("group_id", groupId);
            o.addProperty("avatar_data", avatarData);
            this.connection.sendJson(o);
        }
    }

    static String resolveLiveMultiplayerAddress(MinecraftClient mc) {
        ClientPlayNetworkHandler nh = mc.getNetworkHandler();
        if (nh == null) {
            return "";
        } else {
            ClientConnection conn = nh.getConnection();
            if (conn != null && conn.isOpen()) {
                String s = conn.getAddressAsString(true);
                return s != null && !s.isBlank() ? s : "";
            } else {
                return "";
            }
        }
    }

    public void sendPresenceFromMc() {
        if (this.state == FriendHubClient$State.CONNECTED) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) {
                String mode = "menu";
                String server = "";
                String music = "";
                String musicTitle = "";
                String musicArtist = "";
                long musicDurationMs = 0L;
                long musicPositionMs = 0L;
                boolean musicPlaying = false;
                if (mc.player != null && mc.world != null) {
                    if (mc.isInSingleplayer()) {
                        mode = "singleplayer";
                    } else {
                        ServerInfo entry = mc.getCurrentServerEntry();
                        if (entry != null && entry.address != null && !entry.address.isBlank()) {
                            mode = "multiplayer";
                            server = entry.address.trim();
                        } else {
                            String live = resolveLiveMultiplayerAddress(mc);
                            if (!live.isEmpty()) {
                                mode = "multiplayer";
                                server = live;
                            } else {
                                mode = "menu";
                            }
                        }
                    }
                }

                boolean hs = hideMyStatusFromFriends;
                boolean hgs = hideGameServerFromFriends || hs;
                if (hs) {
                    mode = "menu";
                    server = "";
                    music = "";
                    musicTitle = "";
                    musicArtist = "";
                    musicDurationMs = 0L;
                    musicPositionMs = 0L;
                    musicPlaying = false;
                } else if (hideGameServerFromFriends && "multiplayer".equals(mode)) {
                    server = "";
                }

                JsonObject o = new JsonObject();
                o.addProperty("type", "presence");
                o.addProperty("mode", mode);
                o.addProperty("server", server);
                o.addProperty("music", music);
                o.addProperty("music_title", musicTitle);
                o.addProperty("music_artist", musicArtist);
                o.addProperty("music_duration_ms", musicDurationMs);
                o.addProperty("music_position_ms", musicPositionMs);
                o.addProperty("music_playing", musicPlaying);
                o.addProperty("hide_status", hs);
                o.addProperty("hide_server", hgs);
                this.connection.sendJson(o);
            }
        }
    }

    public void sendDm(String toLogin, String message) {
        if (this.state == FriendHubClient$State.CONNECTED && toLogin != null && message != null && !message.isBlank()) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "dm_send");
            o.addProperty("to", toLogin.trim());
            String m = message.length() > 40 ? message.substring(0, 40) : message;
            o.addProperty("message", m);
            this.connection.sendJson(o);
        }
    }

    public void requestDmHistory(String peerLogin) {
        if (this.state == FriendHubClient$State.CONNECTED && peerLogin != null && !peerLogin.isBlank()) {
            JsonObject o = new JsonObject();
            o.addProperty("type", "dm_fetch");
            o.addProperty("peer", peerLogin.trim());
            this.connection.sendJson(o);
        }
    }

    void applyFriendFromJson(JsonObject f) {
        String login = str(f, "login");
        String av = str(f, "avatar_url");
        boolean online = f.has("online") && f.get("online").getAsBoolean();
        FriendHubClient$FriendEntry e = new FriendHubClient$FriendEntry(login, av, online);
        e.presenceMode = str(f, "mode");
        e.presenceServer = str(f, "server");
        e.presenceMusic = str(f, "music");
        e.presenceMusicTitle = str(f, "music_title");
        e.presenceMusicArtist = str(f, "music_artist");
        e.presenceMusicDurationMs = longVal(f, "music_duration_ms");
        e.presenceMusicPositionMs = longVal(f, "music_position_ms");
        e.presenceMusicPlaying = boolVal(f, "music_playing");
        e.privacyMinimal = online && f.has("privacy_minimal") && f.get("privacy_minimal").getAsBoolean();
        e.privacyHideServer = online && f.has("privacy_hide_server") && f.get("privacy_hide_server").getAsBoolean();
        if (e.privacyMinimal) {
            e.presenceMode = "";
            e.presenceServer = "";
            e.presenceMusic = "";
            e.presenceMusicTitle = "";
            e.presenceMusicArtist = "";
            e.presenceMusicDurationMs = 0L;
            e.presenceMusicPositionMs = 0L;
            e.presenceMusicPlaying = false;
        }

        if (f.has("last_seen") && !f.get("last_seen").isJsonNull()) {
            e.lastSeenUnix = f.get("last_seen").getAsLong();
        }

        this.friendsMap.put(login.toLowerCase(), e);
        this.presenceCache.put(login.toLowerCase(), e);
        if (av != null && !av.isBlank()) {
            this.loadAvatar(av);
        }
    }

    void handleIncoming(JsonObject json) {
        String type = json.has("type") ? json.get("type").getAsString() : "";
        switch (type) {
            case "friends_list":
                this.friendsMap.clear();
                if (json.has("friends") && json.get("friends").isJsonArray()) {
                    for (JsonElement elxxxxx : json.getAsJsonArray("friends")) {
                        if (elxxxxx.isJsonObject()) {
                            this.applyFriendFromJson(elxxxxx.getAsJsonObject());
                        }
                    }
                }

                this.notifyChanged();
                break;
            case "requests_list":
                this.incomingRequests.clear();
                if (json.has("requests") && json.get("requests").isJsonArray()) {
                    for (JsonElement elxxxx : json.getAsJsonArray("requests")) {
                        if (elxxxx.isJsonObject()) {
                            JsonObject r = elxxxx.getAsJsonObject();
                            String fromxxx = str(r, "from");
                            String avxx = str(r, "avatar_url");
                            this.incomingRequests.put(fromxxx.toLowerCase(), new FriendHubClient$FriendEntry(fromxxx, avxx, false));
                            if (!avxx.isBlank()) {
                                this.loadAvatar(avxx);
                            }
                        }
                    }
                }

                this.notifyChanged();
                break;
            case "group_invites_list":
                this.incomingGroupInvites.clear();
                if (json.has("invites") && json.get("invites").isJsonArray()) {
                    for (JsonElement elxxx : json.getAsJsonArray("invites")) {
                        if (elxxx.isJsonObject()) {
                            JsonObject inv = elxxx.getAsJsonObject();
                            String fromxxx = str(inv, "from");
                            String gidx = str(inv, "group_id");
                            String gnamex = str(inv, "group_name");
                            String avatarx = str(inv, "avatar_url");
                            FriendHubClient$GroupInviteEntry ex = new FriendHubClient$GroupInviteEntry(fromxxx, gidx, gnamex, avatarx);
                            this.incomingGroupInvites.put(FriendHubClient$GroupInviteEntry.key(gidx, fromxxx), ex);
                            if (!avatarx.isBlank()) {
                                this.loadAvatar(avatarx);
                            }
                        }
                    }
                }

                this.notifyChanged();
                break;
            case "friend_request_incoming":
                String fromxx = str(json, "from");
                String avx = str(json, "avatar_url");
                this.incomingRequests.put(fromxx.toLowerCase(), new FriendHubClient$FriendEntry(fromxx, avx, false));
                if (!avx.isBlank()) {
                    this.loadAvatar(avx);
                }

                if (this.onFriendRequestIncoming != null) {
                    this.onFriendRequestIncoming.accept(new FriendHubClient$FriendEntry(fromxx, avx, false));
                }

                this.notifyChanged();
                break;
            case "group_invite_incoming": {
                String fromx = str(json, "from");
                String gid = str(json, "group_id");
                String gname = str(json, "group_name");
                String avatar = str(json, "avatar_url");
                FriendHubClient$GroupInviteEntry e = new FriendHubClient$GroupInviteEntry(fromx, gid, gname, avatar);
                this.incomingGroupInvites.put(FriendHubClient$GroupInviteEntry.key(gid, fromx), e);
                if (!avatar.isBlank()) {
                    this.loadAvatar(avatar);
                }

                if (this.onGroupInviteIncoming != null) {
                    this.onGroupInviteIncoming.accept(e);
                }

                this.notifyChanged();
                break;
            }
            case "friend_accepted":
                String fromx = str(json, "from");
                String avx2 = str(json, "avatar_url");
                FriendHubClient$FriendEntry fe = new FriendHubClient$FriendEntry(fromx, avx2, true);
                this.friendsMap.put(fromx.toLowerCase(), fe);
                this.incomingRequests.remove(fromx.toLowerCase());
                if (!avx2.isBlank()) {
                    this.loadAvatar(avx2);
                }

                this.notifyChanged();
                break;
            case "friend_presence": {
                String login = str(json, "login");
                FriendHubClient$FriendEntry fe2 = this.friendsMap.get(login.toLowerCase());
                if (fe2 != null) {
                    fe2.online = json.has("online") && json.get("online").getAsBoolean();
                    fe2.presenceMode = str(json, "mode");
                    fe2.presenceServer = str(json, "server");
                    fe2.presenceMusic = str(json, "music");
                    fe2.presenceMusicTitle = str(json, "music_title");
                    fe2.presenceMusicArtist = str(json, "music_artist");
                    fe2.presenceMusicDurationMs = longVal(json, "music_duration_ms");
                    fe2.presenceMusicPositionMs = longVal(json, "music_position_ms");
                    fe2.presenceMusicPlaying = boolVal(json, "music_playing");
                    fe2.privacyMinimal = fe2.online && json.has("privacy_minimal") && json.get("privacy_minimal").getAsBoolean();
                    fe2.privacyHideServer = fe2.online && json.has("privacy_hide_server") && json.get("privacy_hide_server").getAsBoolean();
                    if (fe2.privacyMinimal) {
                        fe2.presenceMode = "";
                        fe2.presenceServer = "";
                        fe2.presenceMusic = "";
                        fe2.presenceMusicTitle = "";
                        fe2.presenceMusicArtist = "";
                        fe2.presenceMusicDurationMs = 0L;
                        fe2.presenceMusicPositionMs = 0L;
                        fe2.presenceMusicPlaying = false;
                    }

                    fe2.lastSeenUnix = json.has("last_seen") && !json.get("last_seen").isJsonNull() ? json.get("last_seen").getAsLong() : 0L;
                    this.presenceCache.put(login.toLowerCase(), fe2);
                } else {
                    FriendHubClient$FriendEntry cached = this.presenceCache
                        .computeIfAbsent(login.toLowerCase(), k -> new FriendHubClient$FriendEntry(login, "", false));
                    cached.online = json.has("online") && json.get("online").getAsBoolean();
                    cached.presenceMode = str(json, "mode");
                    cached.presenceServer = str(json, "server");
                    cached.presenceMusic = str(json, "music");
                    cached.presenceMusicTitle = str(json, "music_title");
                    cached.presenceMusicArtist = str(json, "music_artist");
                    cached.presenceMusicDurationMs = longVal(json, "music_duration_ms");
                    cached.presenceMusicPositionMs = longVal(json, "music_position_ms");
                    cached.presenceMusicPlaying = boolVal(json, "music_playing");
                    cached.privacyMinimal = cached.online && json.has("privacy_minimal") && json.get("privacy_minimal").getAsBoolean();
                    cached.privacyHideServer = cached.online && json.has("privacy_hide_server") && json.get("privacy_hide_server").getAsBoolean();
                    if (cached.privacyMinimal) {
                        cached.presenceMode = "";
                        cached.presenceServer = "";
                        cached.presenceMusic = "";
                        cached.presenceMusicTitle = "";
                        cached.presenceMusicArtist = "";
                        cached.presenceMusicDurationMs = 0L;
                        cached.presenceMusicPositionMs = 0L;
                        cached.presenceMusicPlaying = false;
                    }

                    cached.lastSeenUnix = json.has("last_seen") && !json.get("last_seen").isJsonNull() ? json.get("last_seen").getAsLong() : 0L;
                }

                this.initialFriendsLoaded = true;
                this.notifyChanged();
                break;
            }
            case "friend_request_result":
                boolean ok = json.has("ok") && json.get("ok").getAsBoolean();
                if (this.onHubRequestResult != null) {
                    this.onHubRequestResult.accept(ok ? "friend_ok" : "friend_not_found");
                }

                this.notifyChanged();
                break;
            case "group_invite_result":
                boolean ok2 = json.has("ok") && json.get("ok").getAsBoolean();
                if (this.onHubRequestResult != null) {
                    this.onHubRequestResult.accept(ok2 ? "group_ok" : "group_not_found");
                }

                this.notifyChanged();
                break;
            case "dm_partners_list":
                this.dmPartners.clear();
                if (json.has("partners") && json.get("partners").isJsonArray()) {
                    for (JsonElement elxx : json.getAsJsonArray("partners")) {
                        if (elxx.isJsonObject()) {
                            JsonObject p = elxx.getAsJsonObject();
                            String loginx = str(p, "login");
                            String avxx = str(p, "avatar_url");
                            this.dmPartners.add(new FriendHubClient$FriendEntry(loginx, avxx, true));
                            if (!avxx.isBlank()) {
                                this.loadAvatar(avxx);
                            }
                        }
                    }
                }

                this.notifyChanged();
                break;
            case "dm_messages":
                String peerx = str(json, "peer");
                List<FriendHubClient$DmMessage> list = new ArrayList<>();
                if (json.has("messages") && json.get("messages").isJsonArray()) {
                    for (JsonElement elx : json.getAsJsonArray("messages")) {
                        if (elx.isJsonObject()) {
                            JsonObject m = elx.getAsJsonObject();
                            list.add(
                                new FriendHubClient$DmMessage(
                                    str(m, "from"),
                                    str(m, "to"),
                                    str(m, "avatar_url"),
                                    str(m, "message"),
                                    str(m, "time"),
                                    m.has("ts") ? m.get("ts").getAsLong() : 0L
                                )
                            );
                        }
                    }
                }

                this.dmThreads.put(peerx.toLowerCase(), list);

                for (FriendHubClient$DmMessage dmx : list) {
                    if (dmx.avatarUrl != null && !dmx.avatarUrl.isBlank()) {
                        this.loadAvatar(dmx.avatarUrl);
                    }
                }

                this.notifyChanged();
                break;
            case "dm_incoming":
                String from = str(json, "from");
                String to = str(json, "to");
                String av = str(json, "avatar_url");
                String msg = str(json, "message");
                String time = str(json, "time");
                long ts = json.has("ts") ? json.get("ts").getAsLong() : System.currentTimeMillis() / 1000L;
                String me = IrcIdentity.getLogin().toLowerCase();
                String peer = from.equalsIgnoreCase(me) ? to : from;
                FriendHubClient$DmMessage dm = new FriendHubClient$DmMessage(from, to, av, msg, time, ts);
                List<FriendHubClient$DmMessage> thread = this.dmThreads.computeIfAbsent(peer.toLowerCase(), k -> new ArrayList<>());
                if (thread.stream()
                    .noneMatch(
                        existing -> existing.from.equalsIgnoreCase(from)
                            && existing.to.equalsIgnoreCase(to)
                            && existing.message.equals(msg)
                            && existing.ts == ts
                    )) {
                    thread.add(dm);
                }

                if (av != null && !av.isBlank()) {
                    this.loadAvatar(av);
                }

                if (!from.equalsIgnoreCase(me)) {
                    IrcChatMessengerScreen.pushRichNotification("dm", av, from, from, msg, from);
                }

                this.initialDmPartnersLoaded = true;
                this.notifyChanged();
                break;
            case "friend_removed": {
                String login = str(json, "login");
                this.friendsMap.remove(login.toLowerCase());
                this.notifyChanged();
                break;
            }
            case "groups_list":
                this.groupsMap.clear();
                if (json.has("groups") && json.get("groups").isJsonArray()) {
                    for (JsonElement el : json.getAsJsonArray("groups")) {
                        if (el.isJsonObject()) {
                            this.applyGroupJson(el.getAsJsonObject());
                        }
                    }
                }

                this.notifyChanged();
                break;
            case "group_created":
            case "group_updated":
                if (json.has("group") && json.get("group").isJsonObject()) {
                    this.applyGroupJson(json.getAsJsonObject("group"));
                }

                this.notifyChanged();
                break;
            case "group_message_incoming": {
                String gid = str(json, "group_id");
                String fromxxx = str(json, "from");
                String avxx = str(json, "avatar_url");
                String msgx = str(json, "message");
                String timex = str(json, "time");
                FriendHubClient$GroupData g = this.groupsMap.get(gid);
                if (g != null) {
                    boolean duplicate = !g.messages.isEmpty()
                        && g.messages.get(g.messages.size() - 1).from.equalsIgnoreCase(fromxxx)
                        && g.messages.get(g.messages.size() - 1).message.equals(msgx)
                        && g.messages.get(g.messages.size() - 1).time.equals(timex);
                    if (!duplicate) {
                        g.messages.add(new FriendHubClient$GroupMessage(fromxxx, avxx, msgx, timex));
                    }

                    if (!avxx.isBlank()) {
                        this.loadAvatar(avxx);
                    }

                    if (!fromxxx.equalsIgnoreCase(IrcIdentity.getLogin())) {
                        IrcChatMessengerScreen.pushRichNotification("group", avxx, fromxxx, "Группа: " + g.name, msgx, gid);
                    }
                }

                this.initialGroupsLoaded = true;
                this.notifyChanged();
                break;
            }
            case "group_ping_incoming":
                FriendHubClient$GroupPing ping = new FriendHubClient$GroupPing(
                    str(json, "from"), str(json, "sender_name"), str(json, "label"), doubleVal(json, "x"), doubleVal(json, "y"), doubleVal(json, "z")
                );
                if (this.onGroupPingIncoming != null) {
                    this.onGroupPingIncoming.accept(ping);
                }
                break;
            case "group_left":
            case "group_deleted": {
                String gid = str(json, "group_id");
                this.groupsMap.remove(gid);
                this.notifyChanged();
            }
        }
    }

    void applyGroupJson(JsonObject g) {
        String id = str(g, "id");
        String name = str(g, "name");
        String owner = str(g, "owner");
        FriendHubClient$GroupData gd = new FriendHubClient$GroupData(id, name, owner);
        gd.avatarUrl = str(g, "avatar_url");
        if (!gd.avatarUrl.isBlank()) {
            this.loadAvatar(gd.avatarUrl);
        }

        FriendHubClient$GroupData previous = this.groupsMap.get(id);
        if (g.has("messages") && g.get("messages").isJsonArray()) {
            for (JsonElement element : g.getAsJsonArray("messages")) {
                if (element.isJsonObject()) {
                    JsonObject message = element.getAsJsonObject();
                    gd.messages
                        .add(new FriendHubClient$GroupMessage(str(message, "from"), str(message, "avatar_url"), str(message, "message"), str(message, "time")));
                }
            }
        } else if (previous != null) {
            gd.messages.addAll(previous.messages);
        }

        if (g.has("members") && g.get("members").isJsonArray()) {
            for (JsonElement m : g.getAsJsonArray("members")) {
                gd.members.add(m.getAsString());
            }
        }

        this.groupsMap.put(id, gd);
    }

    public void loadAvatar(String url) {
        String avatarUrl = normalizeAvatarUrl(url);
        if (avatarUrl != null && !this.avatarCache.containsKey(avatarUrl) && !this.pendingAvs.contains(avatarUrl)) {
            this.pendingAvs.add(avatarUrl);
            if (avatarUrl.startsWith("data:image/") && avatarUrl.contains(";base64,")) {
                try {
                    byte[] body = Base64.getDecoder().decode(avatarUrl.substring(avatarUrl.indexOf(44) + 1));
                    this.registerAvatarTexture(avatarUrl, body);
                } catch (IllegalArgumentException var4) {
                    this.pendingAvs.remove(avatarUrl);
                }
            } else {
                HTTP.sendAsync(HttpRequest.newBuilder().uri(URI.create(avatarUrl)).build(), BodyHandlers.ofByteArray()).thenAccept(resp -> {
                    if (resp.statusCode() != 200) {
                        this.pendingAvs.remove(avatarUrl);
                    } else {
                        this.registerAvatarTexture(avatarUrl, resp.body());
                    }
                }).exceptionally(ex -> {
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
                String idStr = "friendhub_av_" + Math.abs(avatarUrl.hashCode());
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

    public FriendHubClient$State getState() {
        return this.state;
    }

    public String getLastConnectionError() {
        return this.lastConnectionError;
    }

    public List<FriendHubClient$FriendEntry> getFriends() {
        return new ArrayList<>(this.friendsMap.values());
    }

    public FriendHubClient$FriendEntry getPresenceEntry(String login) {
        return login != null && !login.isBlank() ? this.presenceCache.get(login.toLowerCase()) : null;
    }

    public List<FriendHubClient$FriendEntry> getIncomingRequests() {
        return new ArrayList<>(this.incomingRequests.values());
    }

    public List<FriendHubClient$GroupInviteEntry> getIncomingGroupInvites() {
        return new ArrayList<>(this.incomingGroupInvites.values());
    }

    public List<FriendHubClient$GroupData> getGroups() {
        return new ArrayList<>(this.groupsMap.values());
    }

    public boolean isInitialDataLoaded() {
        return this.initialFriendsLoaded && this.initialGroupsLoaded && this.initialDmPartnersLoaded;
    }

    public boolean isFriend(String login) {
        return login != null && this.friendsMap.containsKey(login.toLowerCase());
    }

    public int getIncomingRequestCount() {
        return this.incomingRequests.size() + this.incomingGroupInvites.size();
    }

    public List<FriendHubClient$DmMessage> getDmMessages(String peer) {
        if (peer == null) {
            return List.of();
        }

        List<FriendHubClient$DmMessage> l = this.dmThreads.get(peer.toLowerCase());
        return l != null ? new ArrayList<>(l) : new ArrayList<>();
    }

    public List<FriendHubClient$FriendEntry> getDmPartners() {
        return new ArrayList<>(this.dmPartners);
    }

    public void setOnFriendRequestIncoming(Consumer<FriendHubClient$FriendEntry> cb) {
        this.onFriendRequestIncoming = cb;
    }

    public void setOnGroupInviteIncoming(Consumer<FriendHubClient$GroupInviteEntry> cb) {
        this.onGroupInviteIncoming = cb;
    }

    public void setOnGroupPingIncoming(Consumer<FriendHubClient$GroupPing> cb) {
        this.onGroupPingIncoming = cb;
    }

    public void setOnHubRequestResult(Consumer<String> cb) {
        this.onHubRequestResult = cb;
    }

    public void setOnDataChanged(Runnable cb) {
        this.onDataChanged = cb;
    }

    void notifyChanged() {
        if (this.onDataChanged != null) {
            this.onDataChanged.run();
        }
    }

    static String str(JsonObject o, String key) {
        return o.has(key) ? o.get(key).getAsString() : "";
    }

    static long longVal(JsonObject o, String key) {
        if (o.has(key) && !o.get(key).isJsonNull()) {
            try {
                return o.get(key).getAsLong();
            } catch (Exception var3) {
                return 0L;
            }
        } else {
            return 0L;
        }
    }

    static double doubleVal(JsonObject o, String key) {
        if (o.has(key) && !o.get(key).isJsonNull()) {
            try {
                return o.get(key).getAsDouble();
            } catch (Exception var3) {
                return 0.0;
            }
        } else {
            return 0.0;
        }
    }

    static boolean boolVal(JsonObject o, String key) {
        if (o.has(key) && !o.get(key).isJsonNull()) {
            try {
                return o.get(key).getAsBoolean();
            } catch (Exception var3) {
                return false;
            }
        } else {
            return false;
        }
    }

    static String truncateCompact(String text, int maxLen) {
        if (text == null) {
            return "";
        }

        String trimmed = text.trim();
        return trimmed.length() <= maxLen ? trimmed : trimmed.substring(0, Math.max(0, maxLen)) + "...";
    }

    static String buildMusicPresenceLabel(String title, String artist) {
        String t = truncateCompact(title, 15);
        String a = truncateCompact(artist, 15);
        if (!t.isEmpty() && !a.isEmpty()) {
            return t + ", " + a;
        } else {
            return !t.isEmpty() ? t : a;
        }
    }

    public static String getLogin() {
        return IrcIdentity.getLogin();
    }

    public void ensureIdentityCurrent() {
        String current = IrcIdentity.getLogin();
        if (!this.authenticatedLogin.isBlank() && !this.authenticatedLogin.equalsIgnoreCase(current)) {
            this.refreshIdentity();
        }
    }

    public void refreshIdentity() {
        String current = IrcIdentity.getLogin();
        if (!this.authenticatedLogin.isBlank() || this.state != FriendHubClient$State.DISCONNECTED) {
            this.allowFallback = false;
            this.connection.disconnect();
            this.state = FriendHubClient$State.DISCONNECTED;
            this.authenticatedLogin = "";
            this.activeUrlIndex = 0;
            this.friendsMap.clear();
            this.presenceCache.clear();
            this.incomingRequests.clear();
            this.dmThreads.clear();
            this.dmPartners.clear();
            this.groupsMap.clear();
            this.incomingGroupInvites.clear();
            this.allowFallback = true;
            this.notifyChanged();
            this.connect(DEFAULT_URL);
        }
    }
}
