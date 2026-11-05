package com.example.auravisuals.friend;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class FriendManager {
    public static final FriendManager INSTANCE = new FriendManager();
    private final Set<String> friends = new LinkedHashSet<>();

    private FriendManager() {
    }

    public void addFriend(String name) {
        if (name != null && !name.isBlank()) {
            this.friends.add(name.trim());
        }
    }

    public void removeFriend(String name) {
        if (name != null) {
            this.friends.removeIf(friend -> friend.equalsIgnoreCase(name.trim()));
        }
    }

    public void clearFriends() {
        this.friends.clear();
    }

    public Set<String> getFriends() {
        return Collections.unmodifiableSet(this.friends);
    }
}
