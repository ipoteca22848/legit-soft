package com.example.auravisuals.util.media;

import java.util.List;
import java.util.Objects;

public final class SyncedLyricsService$LyricLine {
    long timeMs;
    String text;
    List<SyncedLyricsService$LyricWord> words;

    public SyncedLyricsService$LyricLine(long timeMs, String text, List<SyncedLyricsService$LyricWord> words) {
        this.timeMs = timeMs;
        this.text = text;
        this.words = words;
    }

    public long timeMs() {
        return this.timeMs;
    }

    public String text() {
        return this.text;
    }

    public List<SyncedLyricsService$LyricWord> words() {
        return this.words;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof SyncedLyricsService$LyricLine other)
                ? false
                : Objects.equals(this.timeMs, other.timeMs) && Objects.equals(this.text, other.text) && Objects.equals(this.words, other.words);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.timeMs, this.text, this.words);
    }

    @Override
    public String toString() {
        return "SyncedLyricsService$LyricLine[timeMs=" + this.timeMs + ", text=" + this.text + ", words=" + this.words + "]";
    }

    public SyncedLyricsService$LyricLine(long timeMs, String text) {
        this(timeMs, text, List.of());
    }
}
