package com.example.auravisuals.util.chat;

import java.util.Objects;

public final class ChatHubMessage {
    String login;
    String role;
    String avatarUrl;
    String message;
    String time;

    public ChatHubMessage(String login, String role, String avatarUrl, String message, String time) {
        this.login = login;
        this.role = role;
        this.avatarUrl = avatarUrl;
        this.message = message;
        this.time = time;
        login = login == null ? "?" : login;
        role = role == null ? "USER" : role;
        avatarUrl = avatarUrl == null ? "" : avatarUrl;
        message = message == null ? "" : message;
        time = time == null ? "" : time;
        this.login = login;
        this.role = role;
        this.avatarUrl = avatarUrl;
        this.message = message;
        this.time = time;
    }

    public String login() {
        return this.login;
    }

    public String role() {
        return this.role;
    }

    public String avatarUrl() {
        return this.avatarUrl;
    }

    public String message() {
        return this.message;
    }

    public String time() {
        return this.time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof ChatHubMessage other)
                ? false
                : Objects.equals(this.login, other.login)
                    && Objects.equals(this.role, other.role)
                    && Objects.equals(this.avatarUrl, other.avatarUrl)
                    && Objects.equals(this.message, other.message)
                    && Objects.equals(this.time, other.time);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.login, this.role, this.avatarUrl, this.message, this.time);
    }

    @Override
    public String toString() {
        return "ChatHubMessage[login="
            + this.login
            + ", role="
            + this.role
            + ", avatarUrl="
            + this.avatarUrl
            + ", message="
            + this.message
            + ", time="
            + this.time
            + "]";
    }
}
