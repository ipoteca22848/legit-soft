package com.example.auravisuals.util.friend;

public class FriendHubClient$GroupInviteEntry {
    public final String from;
    public final String groupId;
    public final String groupName;
    public final String avatarUrl;

    public FriendHubClient$GroupInviteEntry(String from, String groupId, String groupName, String avatarUrl) {
        this.from = from;
        this.groupId = groupId;
        this.groupName = groupName;
        this.avatarUrl = avatarUrl;
    }

    public static String key(String groupId, String from) {
        return groupId + "|" + from.toLowerCase();
    }
}
