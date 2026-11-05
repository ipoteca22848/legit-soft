package com.example.auravisuals.util.friend;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FriendHubClient$GroupData {
    public String id;
    public String name;
    public String owner;
    public String avatarUrl = "";
    public final List<String> members = new ArrayList<>();
    public final List<FriendHubClient$GroupMessage> messages = new CopyOnWriteArrayList<>();

    public FriendHubClient$GroupData(String id, String name, String owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
    }

    public boolean isOwner(String login) {
        return login != null && this.owner != null ? this.owner.equalsIgnoreCase(login.trim()) : false;
    }
}
