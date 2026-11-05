package com.example.auravisuals.util.friend;

public class FriendHubClient$FriendEntry {
    public final String login;
    public String avatarUrl;
    public boolean online;
    public String presenceMode = "";
    public String presenceServer = "";
    public String presenceMusic = "";
    public String presenceMusicTitle = "";
    public String presenceMusicArtist = "";
    public long presenceMusicDurationMs = 0L;
    public long presenceMusicPositionMs = 0L;
    public boolean presenceMusicPlaying = false;
    public long lastSeenUnix;
    public boolean privacyMinimal;
    public boolean privacyHideServer;

    public FriendHubClient$FriendEntry(String login, String avatarUrl, boolean online) {
        this.login = login;
        this.avatarUrl = avatarUrl;
        this.online = online;
    }
}
