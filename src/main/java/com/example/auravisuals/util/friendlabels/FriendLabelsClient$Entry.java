package com.example.auravisuals.util.friendlabels;

public final class FriendLabelsClient$Entry {
    public final String login;
    public String name;
    public boolean online;

    public FriendLabelsClient$Entry(String login, String name, boolean online) {
        this.login = login;
        this.name = name != null && !name.isBlank() ? name : login;
        this.online = online;
    }
}
