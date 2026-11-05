package com.example.auravisuals.util.friendscreen;

public final class FriendScreenClient$Entry {
    public final String login;
    public String name;
    public boolean online;

    public FriendScreenClient$Entry(String login, String name, boolean online) {
        this.login = login;
        this.name = name != null && !name.isBlank() ? name : login;
        this.online = online;
    }
}
