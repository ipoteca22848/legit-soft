package com.example.auravisuals.cosmetics;

import java.util.Objects;

public final class BedrockModel$GuiQuad {
    BedrockModel$GuiVertex[] vertices;
    float depth;

    public BedrockModel$GuiQuad(BedrockModel$GuiVertex[] vertices, float depth) {
        this.vertices = vertices;
        this.depth = depth;
    }

    public BedrockModel$GuiVertex[] vertices() {
        return this.vertices;
    }

    public float depth() {
        return this.depth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof BedrockModel$GuiQuad other)
                ? false
                : Objects.equals(this.vertices, other.vertices) && Objects.equals(this.depth, other.depth);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.vertices, this.depth);
    }

    @Override
    public String toString() {
        return "BedrockModel$GuiQuad[vertices=" + this.vertices + ", depth=" + this.depth + "]";
    }
}
