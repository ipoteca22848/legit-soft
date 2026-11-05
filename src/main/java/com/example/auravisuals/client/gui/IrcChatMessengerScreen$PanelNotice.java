package com.example.auravisuals.client.gui;

public class IrcChatMessengerScreen$PanelNotice {
    public String text;
    public long createdAt;

    public String text() {
        return this.text;
    }

    public long createdAt() {
        return this.createdAt;
    }

    public IrcChatMessengerScreen$PanelNotice(String text, long createdAt) {
        this.text = text;
        this.createdAt = createdAt;
    }
}
