package com.example.auravisuals.command;

public class HelpCommand extends Command {
    public HelpCommand() {
        super("help", "Показывает список доступных команд", "Shows a list of available commands", ".help");
    }

    @Override
    public void execute(String[] args) {
        CommandManager.addChatMessage(CommandManager.tr("Вот доступные команды:", "Bot available commands:"));

        for (Command command : CommandManager.INSTANCE.getCommands()) {
            CommandManager.addChatMessage("§c" + command.getName() + " §7- " + command.getDescription());
        }
    }
}
