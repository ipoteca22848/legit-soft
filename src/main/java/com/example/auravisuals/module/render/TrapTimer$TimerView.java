package com.example.auravisuals.module.render;

public class TrapTimer$TimerView {
    public String text;
    public long remaining;
    public long duration;
    public int color;

    public String text() {
        return this.text;
    }

    public long remaining() {
        return this.remaining;
    }

    public long duration() {
        return this.duration;
    }

    public int color() {
        return this.color;
    }

    public TrapTimer$TimerView(String text, long remaining, long duration, int color) {
        this.text = text;
        this.remaining = remaining;
        this.duration = duration;
        this.color = color;
    }
}
