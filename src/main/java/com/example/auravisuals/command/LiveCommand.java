package com.example.auravisuals.command;

import com.example.auravisuals.live.LiveScriptManager;
import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import net.minecraft.util.Util;

public class LiveCommand extends Command {
    public LiveCommand() {
        super(
            "code",
            "Управление живым кодом и горячей перезагрузкой скриптов",
            "Live coding and runtime script hot-reload manager",
            ".code [reload | eval <код> | auto | clear | list | dir]"
        );
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            this.showHelp();
        } else {
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "reload":
                case "r":
                    CommandManager.addChatMessage("§e[LiveCode] Recompiling scripts...");
                    new Thread(() -> LiveScriptManager.INSTANCE.reloadScripts(true), "LiveCode-Reloader").start();
                    break;
                case "eval":
                case "run":
                case "e":
                    if (args.length < 2) {
                        CommandManager.addChatMessage("§cUsage: .code eval <java code>");
                        return;
                    }

                    StringBuilder sb = new StringBuilder();

                    for (int i = 1; i < args.length; i++) {
                        sb.append(args[i]).append(" ");
                    }

                    String snippet = sb.toString().trim();
                    CommandManager.addChatMessage("§e[LiveCode] Compiling and running snippet...");
                    new Thread(() -> LiveScriptManager.INSTANCE.evalSnippet(snippet), "LiveCode-Eval").start();
                    break;
                case "auto":
                case "watch":
                case "w":
                    boolean newState = !LiveScriptManager.INSTANCE.isAutoWatchEnabled();
                    LiveScriptManager.INSTANCE.setAutoWatchEnabled(newState);
                    CommandManager.addChatMessage("§a[LiveCode] Auto-Watch on file save: " + (newState ? "§aENABLED (Ctrl+S reloads live)" : "§cDISABLED"));
                    break;
                case "clear":
                case "stop":
                    LiveScriptManager.INSTANCE.clearVisuals();
                    CommandManager.addChatMessage("§a[LiveCode] Cleared all active live visuals.");
                    break;
                case "list":
                    int count = LiveScriptManager.INSTANCE.getActiveVisuals().size();
                    CommandManager.addChatMessage("§e[LiveCode] Active live visuals: §a" + count);
                    break;
                case "dir":
                case "folder":
                case "open":
                    File dir = LiveScriptManager.INSTANCE.getScriptsDirectory();
                    CommandManager.addChatMessage("§e[LiveCode] Scripts directory: §f" + dir.getAbsolutePath());

                    try {
                        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Action.OPEN)) {
                            Desktop.getDesktop().open(dir);
                        } else {
                            Util.getOperatingSystem().open(dir);
                        }
                    } catch (Throwable var7) {
                        CommandManager.addChatMessage("§7(Folder path printed above)");
                    }
                    break;
                default:
                    this.showHelp();
            }
        }
    }

    private void showHelp() {
        CommandManager.addChatMessage("§c§l[LiveCode System]");
        CommandManager.addChatMessage("§e.code reload §7- Перекомпилировать скрипты из папки без перезахода");
        CommandManager.addChatMessage("§e.code auto §7- Включить авто-перезагрузку при сохранении файла (Ctrl+S)");
        CommandManager.addChatMessage("§e.code eval <код> §7- Выполнить Java-код на лету");
        CommandManager.addChatMessage("§e.code dir §7- Открыть папку со скриптами");
        CommandManager.addChatMessage("§e.code clear §7- Выгрузить активные визуалы");
        CommandManager.addChatMessage("§e.code list §7- Список активных визуалов");
    }
}
