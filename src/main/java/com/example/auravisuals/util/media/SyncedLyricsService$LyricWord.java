package com.example.auravisuals.util.media;

import java.util.Objects;

public final class SyncedLyricsService$LyricWord {
    long timeMs;
    String text;

    public SyncedLyricsService$LyricWord(long timeMs, String text) {
        this.timeMs = timeMs;
        this.text = text;
    }

    public long timeMs() {
        return this.timeMs;
    }

    public String text() {
        return this.text;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof SyncedLyricsService$LyricWord other)
                ? false
                : Objects.equals(this.timeMs, other.timeMs) && Objects.equals(this.text, other.text);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.timeMs, this.text);
    }

    @Override
    public String toString() {
        return "SyncedLyricsService$LyricWord[timeMs=" + this.timeMs + ", text=" + this.text + "]";
    }
}
