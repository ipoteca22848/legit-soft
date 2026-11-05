package com.example.auravisuals.util.friend;

public class FriendHubClient$DmMessage {
    public final String from;
    public final String to;
    public final String avatarUrl;
    public final String message;
    public final String time;
    public final long ts;

    public FriendHubClient$DmMessage(String from, String to, String avatarUrl, String message, String time, long ts) {
        this.from = from;
        this.to = to;
        this.avatarUrl = avatarUrl;
        this.message = message;
        this.time = time;
        this.ts = ts;
    }
}
