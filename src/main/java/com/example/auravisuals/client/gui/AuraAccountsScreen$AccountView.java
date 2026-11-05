package com.example.auravisuals.client.gui;

import java.util.Objects;

public final class AuraAccountsScreen$AccountView {
    String nickname;
    long createdAt;
    boolean favorite;

    public AuraAccountsScreen$AccountView(String nickname, long createdAt, boolean favorite) {
        this.nickname = nickname;
        this.createdAt = createdAt;
        this.favorite = favorite;
    }

    public String nickname() {
        return this.nickname;
    }

    public long createdAt() {
        return this.createdAt;
    }

    public boolean favorite() {
        return this.favorite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof AuraAccountsScreen$AccountView other)
                ? false
                : Objects.equals(this.nickname, other.nickname)
                    && Objects.equals(this.createdAt, other.createdAt)
                    && Objects.equals(this.favorite, other.favorite);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.nickname, this.createdAt, this.favorite);
    }

    @Override
    public String toString() {
        return "AuraAccountsScreen$AccountView[nickname=" + this.nickname + ", createdAt=" + this.createdAt + ", favorite=" + this.favorite + "]";
    }
}
