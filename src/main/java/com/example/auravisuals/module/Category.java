package com.example.auravisuals.module;

import com.example.auravisuals.util.language.LanguageManager;

public enum Category {
    PLAYER("Player"),
    VISUALS("Visuals"),
    MISC("Misc"),
    INTERFACE("Interface"),
    UTILITY("Utility"),
    SERVER_EVENTS("Events/Mines"),
    MINES("Mines"),
    CONFIGS("Configs"),
    THEMES("Themes"),
    MARKS("Markers"),
    EVENTS("IRC chat"),
    MESSENGER("Messenger"),
    COSMETICS("Cosmetics");

    private final String name;

    Category(String name) {
        this.name = name;
    }

    public String getName() {
        return LanguageManager.repairIrcMojibake(this.name);
    }
}
