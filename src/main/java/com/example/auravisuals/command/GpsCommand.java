package com.example.auravisuals.command;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.Hud;

public class GpsCommand extends Command {
    public GpsCommand() {
        super("gps", "Показывает направление на координаты", "Shows direction to coordinates", ".gps <x> <z> | .gps off");
    }

    @Override
    public void execute(String[] args) {
        Hud hud = ModuleManager.INSTANCE.getModule(Hud.class);
        if (hud == null) {
            CommandManager.addChatMessage(CommandManager.tr("Hud не найден.", "Hud not found."));
        } else if (args.length == 1 && args[0].equalsIgnoreCase("off")) {
            hud.disableGps();
            CommandManager.addChatMessage(CommandManager.tr("GPS отключен.", "GPS disabled."));
        } else if (args.length < 2) {
            CommandManager.addChatMessage(CommandManager.tr("Использование: .gps <x> <z> или .gps off", "Usage: .gps <x> <z> or .gps off"));
        } else {
            double x;
            double z;
            try {
                x = Double.parseDouble(args[0]);
                z = Double.parseDouble(args[1]);
            } catch (NumberFormatException var8) {
                CommandManager.addChatMessage(CommandManager.tr("Координаты должны быть числами.", "Coordinates must be numbers."));
                return;
            }

            hud.setGpsTarget(x, z);
            CommandManager.addChatMessage(CommandManager.tr("GPS установлен на X ", "GPS set to X ") + x + " Z " + z);
        }
    }
}
