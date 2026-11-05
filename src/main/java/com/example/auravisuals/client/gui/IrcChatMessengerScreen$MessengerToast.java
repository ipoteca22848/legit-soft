package com.example.auravisuals.client.gui;

public final class IrcChatMessengerScreen$MessengerToast {
    public final String type;
    public final String avatarUrl;
    public final String login;
    public final String title;
    public final String body;
    public final String extra;
    public final long createdAt;
    public float currentY = Float.NaN;
    public float drawX;
    public float drawY;
    public float drawW;
    public float drawH;

    public IrcChatMessengerScreen$MessengerToast(String type, String avatarUrl, String login, String title, String body, String extra, long createdAt) {
        this.type = type == null ? "" : type;
        this.avatarUrl = avatarUrl == null ? "" : avatarUrl;
        this.login = login == null ? "" : login;
        this.title = title == null ? "Messenger" : title;
        this.body = body == null ? "" : body;
        this.extra = extra == null ? "" : extra;
        this.createdAt = createdAt;
    }
}
