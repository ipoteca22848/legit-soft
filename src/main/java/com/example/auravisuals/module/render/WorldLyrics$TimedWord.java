package com.example.auravisuals.module.render;

public class WorldLyrics$TimedWord {
    public String text;
    public long startMs;
    public long endMs;

    public String text() {
        return this.text;
    }

    public long startMs() {
        return this.startMs;
    }

    public long endMs() {
        return this.endMs;
    }

    public WorldLyrics$TimedWord(String text, long startMs, long endMs) {
        this.text = text;
        this.startMs = startMs;
        this.endMs = endMs;
    }
}
