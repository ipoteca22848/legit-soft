package com.example.auravisuals.util;

import com.example.auravisuals.client.gui.IrcChatMessengerScreen;
import com.example.auravisuals.util.chat.ChatHubClient;
import com.example.auravisuals.util.chat.ChatHubClient$State;
import com.example.auravisuals.util.friend.FriendHubClient;
import com.example.auravisuals.util.friend.FriendHubClient$State;
import net.minecraft.client.MinecraftClient;

public final class MessengerService {
    private static final long RECONNECT_DELAY_MS = 5000L;
    private static final long PRESENCE_INTERVAL_MS = 2000L;
    private static boolean initialized;
    private static long lastReconnectAttempt;
    private static long lastPresenceUpdate;

    private MessengerService() {
    }

    public static void init() {
        if (!initialized) {
            initialized = true;
            FriendHubClient hub = FriendHubClient.INSTANCE;
            hub.setOnFriendRequestIncoming(
                entry -> IrcChatMessengerScreen.pushRichNotification("request", entry.avatarUrl, entry.login, "Заявка в друзья", entry.login, entry.login)
            );
            hub.setOnGroupInviteIncoming(
                inv -> IrcChatMessengerScreen.pushRichNotification("request", null, inv.from, "Заявка в группу: " + inv.groupName, inv.from, inv.groupId)
            );
        }
    }

    public static void tick() {
        init();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            ChatHubClient chat = ChatHubClient.INSTANCE;
            FriendHubClient friends = FriendHubClient.INSTANCE;
            chat.ensureIdentityCurrent();
            friends.ensureIdentityCurrent();
            long now = System.currentTimeMillis();
            if (now - lastReconnectAttempt >= 5000L) {
                lastReconnectAttempt = now;
                if (chat.getState() == ChatHubClient$State.DISCONNECTED) {
                    chat.connect(ChatHubClient.DEFAULT_URL);
                }

                if (friends.getState() == FriendHubClient$State.DISCONNECTED) {
                    friends.connect(FriendHubClient.DEFAULT_URL);
                }
            }

            if (friends.getState() == FriendHubClient$State.CONNECTED && now - lastPresenceUpdate >= 2000L) {
                lastPresenceUpdate = now;
                friends.sendPresenceFromMc();
            }
        }
    }
}
