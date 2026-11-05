package com.example.auravisuals.cosmetics;

import java.util.Objects;

public final class BedrockModel$GuiVertex {
    float x;
    float y;
    float z;
    float u;
    float v;

    public BedrockModel$GuiVertex(float x, float y, float z, float u, float v) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.u = u;
        this.v = v;
    }

    public float x() {
        return this.x;
    }

    public float y() {
        return this.y;
    }

    public float z() {
        return this.z;
    }

    public float u() {
        return this.u;
    }

    public float v() {
        return this.v;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof BedrockModel$GuiVertex other)
                ? false
                : Objects.equals(this.x, other.x)
                    && Objects.equals(this.y, other.y)
                    && Objects.equals(this.z, other.z)
                    && Objects.equals(this.u, other.u)
                    && Objects.equals(this.v, other.v);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.x, this.y, this.z, this.u, this.v);
    }

    @Override
    public String toString() {
        return "BedrockModel$GuiVertex[x=" + this.x + ", y=" + this.y + ", z=" + this.z + ", u=" + this.u + ", v=" + this.v + "]";
    }
}
