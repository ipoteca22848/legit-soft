package com.example.auravisuals.util.events;

import java.util.Objects;
import java.util.regex.Matcher;

public final class HolyWorldEventsClient$HolyWorldEvent {
    String serverId;
    String serverName;
    String id;
    String name;
    String rarity;
    String rarityRaw;

    public HolyWorldEventsClient$HolyWorldEvent(String serverId, String serverName, String id, String name, String rarity, String rarityRaw) {
        this.serverId = serverId;
        this.serverName = serverName;
        this.id = id;
        this.name = name;
        this.rarity = rarity;
        this.rarityRaw = rarityRaw;
    }

    public String serverId() {
        return this.serverId;
    }

    public String serverName() {
        return this.serverName;
    }

    public String id() {
        return this.id;
    }

    public String name() {
        return this.name;
    }

    public String rarity() {
        return this.rarity;
    }

    public String rarityRaw() {
        return this.rarityRaw;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof HolyWorldEventsClient$HolyWorldEvent other)
                ? false
                : Objects.equals(this.serverId, other.serverId)
                    && Objects.equals(this.serverName, other.serverName)
                    && Objects.equals(this.id, other.id)
                    && Objects.equals(this.name, other.name)
                    && Objects.equals(this.rarity, other.rarity)
                    && Objects.equals(this.rarityRaw, other.rarityRaw);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.serverId, this.serverName, this.id, this.name, this.rarity, this.rarityRaw);
    }

    @Override
    public String toString() {
        return "HolyWorldEventsClient$HolyWorldEvent[serverId="
            + this.serverId
            + ", serverName="
            + this.serverName
            + ", id="
            + this.id
            + ", name="
            + this.name
            + ", rarity="
            + this.rarity
            + ", rarityRaw="
            + this.rarityRaw
            + "]";
    }

    int serverNumber() {
        Matcher matcher = HolyWorldEventsClient.SERVER_NUMBER.matcher(this.serverName);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : Integer.MAX_VALUE;
    }
}
