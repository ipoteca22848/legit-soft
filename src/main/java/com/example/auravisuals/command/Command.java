package com.example.auravisuals.command;

import com.example.auravisuals.util.language.LanguageManager;

public abstract class Command {
    private final String name;
    private final String description;
    private final String englishDescription;
    private final String syntax;

    public Command(String name, String description, String syntax) {
        this(name, description, description, syntax);
    }

    public Command(String name, String description, String englishDescription, String syntax) {
        this.name = name;
        this.description = description;
        this.englishDescription = englishDescription;
        this.syntax = syntax;
    }

    public abstract void execute(String[] var1);

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return LanguageManager.isEnglish() ? this.englishDescription : this.description;
    }

    public String getSyntax() {
        return this.syntax;
    }
}
