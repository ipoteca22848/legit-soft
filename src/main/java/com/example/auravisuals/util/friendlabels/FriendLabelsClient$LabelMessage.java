package com.example.auravisuals.util.friendlabels;

import java.util.Objects;

public final class FriendLabelsClient$LabelMessage {
    String sender;
    double x;
    double y;
    double z;
    int color;
    String dimension;
    String targetUuid;
    String targetName;

    public FriendLabelsClient$LabelMessage(String sender, double x, double y, double z, int color, String dimension, String targetUuid, String targetName) {
        this.sender = sender;
        this.x = x;
        this.y = y;
        this.z = z;
        this.color = color;
        this.dimension = dimension;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    public String sender() {
        return this.sender;
    }

    public double x() {
        return this.x;
    }

    public double y() {
        return this.y;
    }

    public double z() {
        return this.z;
    }

    public int color() {
        return this.color;
    }

    public String dimension() {
        return this.dimension;
    }

    public String targetUuid() {
        return this.targetUuid;
    }

    public String targetName() {
        return this.targetName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof FriendLabelsClient$LabelMessage other)
                ? false
                : Objects.equals(this.sender, other.sender)
                    && Objects.equals(this.x, other.x)
                    && Objects.equals(this.y, other.y)
                    && Objects.equals(this.z, other.z)
                    && Objects.equals(this.color, other.color)
                    && Objects.equals(this.dimension, other.dimension)
                    && Objects.equals(this.targetUuid, other.targetUuid)
                    && Objects.equals(this.targetName, other.targetName);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.sender, this.x, this.y, this.z, this.color, this.dimension, this.targetUuid, this.targetName);
    }

    @Override
    public String toString() {
        return "FriendLabelsClient$LabelMessage[sender="
            + this.sender
            + ", x="
            + this.x
            + ", y="
            + this.y
            + ", z="
            + this.z
            + ", color="
            + this.color
            + ", dimension="
            + this.dimension
            + ", targetUuid="
            + this.targetUuid
            + ", targetName="
            + this.targetName
            + "]";
    }
}
