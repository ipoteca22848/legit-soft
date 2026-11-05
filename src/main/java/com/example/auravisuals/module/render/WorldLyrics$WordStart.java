package com.example.auravisuals.module.render;

public class WorldLyrics$WordStart {
    public String text;
    public long startMs;

    public String text() {
        return this.text;
    }

    public long startMs() {
        return this.startMs;
    }

    public WorldLyrics$WordStart(String text, long startMs) {
        this.text = text;
        this.startMs = startMs;
    }
}
