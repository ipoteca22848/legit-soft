package com.example.auravisuals.account;

public final class AccountManager$Account {
    final String nickname;
    final long createdAt;
    boolean favorite;

    AccountManager$Account(String nickname, long createdAt, boolean favorite) {
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.favorite = favorite;
    }

    public String getNickname() {
        return this.nickname;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

    public boolean isFavorite() {
        return this.favorite;
    }
}
