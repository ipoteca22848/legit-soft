package com.example.auravisuals.emotions;

import net.minecraft.util.Identifier;

public enum EmotionType {
    DEB("deb", "Деб", 2500L),
    FLOSS("floss", "Флосс", 4500L),
    MASTURBATE("masturbate", "Эмоция", 3500L),
    HELLO("hello", "Приветствие", 2500L),
    GET_GRIDDY("get_griddy", "Get Griddy", 5500L),
    HAPPY("happy", "Радость", 2500L);

    private final String name;
    private final String title;
    private final long durationMs;
    private final Identifier texture;

    EmotionType(String name, String title, long durationMs) {
        this.name = name;
        this.title = title;
        this.durationMs = durationMs;
        this.texture = Identifier.of("auravisuals", "textures/emotions/" + name + ".png");
    }

    public String title() {
        return this.title;
    }

    public long durationMs() {
        return this.durationMs;
    }

    public Identifier texture() {
        return this.texture;
    }

    public String shortLabel() {
        return this.name.length() <= 2 ? this.name.toUpperCase() : this.name.substring(0, 2).toUpperCase();
    }
}
