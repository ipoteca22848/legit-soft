package com.example.auravisuals.service;

import com.google.gson.JsonObject;
import java.util.Objects;

public final class GeminiHudService$Result {
    String message;
    JsonObject changes;

    public GeminiHudService$Result(String message, JsonObject changes) {
        this.message = message;
        this.changes = changes;
    }

    public String message() {
        return this.message;
    }

    public JsonObject changes() {
        return this.changes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof GeminiHudService$Result other)
                ? false
                : Objects.equals(this.message, other.message) && Objects.equals(this.changes, other.changes);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.message, this.changes);
    }

    @Override
    public String toString() {
        return "GeminiHudService$Result[message=" + this.message + ", changes=" + this.changes + "]";
    }
}
