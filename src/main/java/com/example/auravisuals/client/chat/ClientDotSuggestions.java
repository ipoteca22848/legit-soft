package com.example.auravisuals.client.chat;

import com.example.auravisuals.command.Command;
import com.example.auravisuals.command.CommandManager;
import com.example.auravisuals.config.ConfigManager;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public final class ClientDotSuggestions {
    private ClientDotSuggestions() {
    }

    public static CompletableFuture<Suggestions> buildFuture(String input, int cursor) {
        String typed = input == null ? "" : input.substring(0, Math.min(cursor, input.length()));
        if (!typed.startsWith(".")) {
            return CompletableFuture.completedFuture((Suggestions)Suggestions.empty().join());
        }

        ArrayList<Suggestion> suggestions = new ArrayList<>();
        int firstSpace = typed.indexOf(32);
        StringRange range;
        if (firstSpace < 0) {
            String query = typed.toLowerCase(Locale.ROOT);
            String exactCommand = typed.substring(1).toLowerCase(Locale.ROOT);
            List<String> exactArguments = argumentSuggestions(exactCommand, "", 0);
            boolean registeredExact = CommandManager.INSTANCE.getCommands().stream().anyMatch(command -> command.getName().equalsIgnoreCase(exactCommand));
            if (registeredExact && !exactArguments.isEmpty()) {
                range = StringRange.at(cursor);
                exactArguments.stream().sorted().forEach(candidate -> suggestions.add(new Suggestion(range, " " + candidate)));
            } else {
                range = StringRange.between(0, cursor);

                for (Command command2 : CommandManager.INSTANCE.getCommands()) {
                    String fullName = "." + command2.getName();
                    if (fullName.toLowerCase(Locale.ROOT).startsWith(query)) {
                        suggestions.add(new Suggestion(range, fullName));
                    }
                }
            }
        } else {
            String command3 = typed.substring(1, firstSpace).toLowerCase(Locale.ROOT);
            int tokenStart = typed.lastIndexOf(32) + 1;
            String query = typed.substring(tokenStart).toLowerCase(Locale.ROOT);
            range = StringRange.between(tokenStart, cursor);
            String args = typed.substring(firstSpace + 1);
            int argumentIndex = args.isEmpty() ? 0 : args.split(" ", -1).length - 1;
            List<String> candidates = argumentSuggestions(command3, args, argumentIndex);
            candidates.stream()
                .filter(candidate -> candidate.toLowerCase(Locale.ROOT).startsWith(query))
                .sorted(Comparator.naturalOrder())
                .forEach(candidate -> suggestions.add(new Suggestion(range, candidate)));
        }

        return CompletableFuture.completedFuture(new Suggestions(range, suggestions));
    }

    private static List<String> argumentSuggestions(String command, String args, int argumentIndex) {
        if (command.equals("cfg")) {
            if (argumentIndex == 0) {
                return List.of("save", "load", "getcode", "loadcode", "list", "remove");
            }

            String action = args.split(" ", -1)[0].toLowerCase(Locale.ROOT);
            if (argumentIndex == 1 && (action.equals("load") || action.equals("remove") || action.equals("getcode"))) {
                return ConfigManager.INSTANCE.listConfigs();
            }
        }

        return command.equals("gps") && argumentIndex == 0 ? List.of("off") : List.of();
    }
}
