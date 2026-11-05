package com.example.auravisuals.command;

import com.example.auravisuals.util.language.LanguageManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public class CommandManager {
    public static final CommandManager INSTANCE = new CommandManager();
    final List<Command> commands = new ArrayList<>();
    final String prefix = ".";

    CommandManager() {
        this.register(new HelpCommand());
        this.register(new ConfigCommand());
        this.register(new GpsCommand());
        this.register(new CommandManager$1(this, "reload", "Quick reload live code", ".reload"));
    }

    public void register(Command command) {
        this.commands.add(command);
    }

    public List<Command> getCommands() {
        return this.commands;
    }

    public boolean execute(String message) {
        if (!message.startsWith(".")) {
            return false;
        }

        String[] args = message.substring(".".length()).split(" ");
        if (args.length == 0) {
            return false;
        }

        String commandName = args[0];

        for (Command command : this.commands) {
            if (command.getName().equalsIgnoreCase(commandName)) {
                command.execute(Arrays.copyOfRange(args, 1, args.length));
                return true;
            }
        }

        addChatMessage(tr("Неизвестная команда! Введите .help для списка команд.", "Unknown command! Type .help for a list of commands."));
        return true;
    }

    public static void addChatMessage(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            String normalized = message.replace("§d", "§c").replace("§5", "§c").replace("В§d", "§c").replace("В§5", "§c");
            mc.player.sendMessage(Text.of("§c§l[bamber client] §7" + normalized), false);
        }
    }

    public static String tr(String ru, String en) {
        return LanguageManager.isEnglish() ? en : ru;
    }
}
