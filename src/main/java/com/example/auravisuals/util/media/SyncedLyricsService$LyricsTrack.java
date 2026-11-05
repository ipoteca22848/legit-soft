package com.example.auravisuals.util.media;

import java.util.List;
import java.util.Objects;

public final class SyncedLyricsService$LyricsTrack {
    List<SyncedLyricsService$LyricLine> lines;
    SyncedLyricsService$State state;
    boolean estimated;

    public SyncedLyricsService$LyricsTrack(List<SyncedLyricsService$LyricLine> lines, SyncedLyricsService$State state, boolean estimated) {
        this.lines = lines;
        this.state = state;
        this.estimated = estimated;
    }

    public List<SyncedLyricsService$LyricLine> lines() {
        return this.lines;
    }

    public SyncedLyricsService$State state() {
        return this.state;
    }

    public boolean estimated() {
        return this.estimated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof SyncedLyricsService$LyricsTrack other)
                ? false
                : Objects.equals(this.lines, other.lines) && Objects.equals(this.state, other.state) && Objects.equals(this.estimated, other.estimated);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.lines, this.state, this.estimated);
    }

    @Override
    public String toString() {
        return "SyncedLyricsService$LyricsTrack[lines=" + this.lines + ", state=" + this.state + ", estimated=" + this.estimated + "]";
    }

    public SyncedLyricsService$LyricsTrack(List<SyncedLyricsService$LyricLine> lines, SyncedLyricsService$State state) {
        this(lines, state, false);
    }

    public int indexAt(long positionMs) {
        int low = 0;
        int high = this.lines.size() - 1;
        int result = -1;

        while (low <= high) {
            int middle = low + high >>> 1;
            if (this.lines.get(middle).timeMs() <= positionMs) {
                result = middle;
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }

        return result;
    }
}
