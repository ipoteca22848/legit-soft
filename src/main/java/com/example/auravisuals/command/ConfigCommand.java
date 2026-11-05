package com.example.auravisuals.command;

import com.example.auravisuals.config.ConfigManager;
import java.util.List;
import net.minecraft.client.MinecraftClient;

public class ConfigCommand extends Command {
    public ConfigCommand() {
        super("cfg", "Управляет конфигами", "Manage configs", ".cfg <save/load/getcode/loadcode/list/remove> [name]");
    }

    @Override
    public void execute(String[] args) {
        if (args.length == 0) {
            CommandManager.addChatMessage(CommandManager.tr("Использование: ", "Usage: ") + this.getSyntax());
        } else {
            String action = args[0].toLowerCase();
            MinecraftClient mc = MinecraftClient.getInstance();
            switch (action) {
                case "save":
                    if (args.length < 2) {
                        CommandManager.addChatMessage(CommandManager.tr("Использование: .cfg save <имя>", "Usage: .cfg save <name>"));
                        return;
                    }

                    String name = args[1];
                    ConfigManager.INSTANCE.saveConfig(name);
                    CommandManager.addChatMessage(CommandManager.tr("§a[bamber client] §fКонфиг сохранен: §e", "§a[bamber client] §fConfig saved: §e") + name);
                    break;
                case "load":
                    if (args.length < 2) {
                        CommandManager.addChatMessage(CommandManager.tr("Использование: .cfg load <имя>", "Usage: .cfg load <name>"));
                        return;
                    }

                    String loadName = args[1];
                    ConfigManager.INSTANCE.loadConfig(loadName);
                    CommandManager.addChatMessage(CommandManager.tr("§a[bamber client] §fКонфиг загружен: §e", "§a[bamber client] §fConfig loaded: §e") + loadName);
                    break;
                case "getcode":
                case "share":
                case "code":
                    String codeName = args.length >= 2 ? args[1] : null;
                    String code = codeName != null ? ConfigManager.INSTANCE.exportConfigToCode(codeName) : ConfigManager.INSTANCE.exportToCode();
                    if (mc != null && mc.keyboard != null) {
                        mc.keyboard.setClipboard(code);
                    }

                    CommandManager.addChatMessage(
                        CommandManager.tr(
                            "§a[bamber client] §fКод конфига скопирован в буфер обмена!", "§a[bamber client] §fConfig share code copied to clipboard!"
                        )
                    );
                    break;
                case "loadcode":
                case "paste":
                case "import":
                    String codeToLoad = null;
                    if (args.length >= 2) {
                        codeToLoad = args[1];
                    } else if (mc != null && mc.keyboard != null) {
                        codeToLoad = mc.keyboard.getClipboard();
                    }

                    if (codeToLoad == null || codeToLoad.isBlank()) {
                        CommandManager.addChatMessage(
                            CommandManager.tr(
                                "§c[bamber client] Код конфига не указан и буфер обмена пуст!",
                                "§c[bamber client] No config code specified and clipboard is empty!"
                            )
                        );
                        return;
                    }

                    if (ConfigManager.INSTANCE.importFromCode(codeToLoad)) {
                        CommandManager.addChatMessage(
                            CommandManager.tr(
                                "§a[bamber client] §fКонфиг успешно загружен и применён по коду!",
                                "§a[bamber client] §fConfig successfully loaded and applied from code!"
                            )
                        );
                    } else {
                        CommandManager.addChatMessage(
                            CommandManager.tr("§c[bamber client] Ошибка: Неверный формат кода конфига!", "§c[bamber client] Error: Invalid config code format!")
                        );
                    }
                    break;
                case "list":
                    List<String> configs = ConfigManager.INSTANCE.listConfigs();
                    if (configs.isEmpty()) {
                        CommandManager.addChatMessage(CommandManager.tr("Конфиги не найдены.", "No configs found."));
                    } else {
                        CommandManager.addChatMessage(CommandManager.tr("§a[bamber client] §fСписок сохраненных конфигов:", "§a[bamber client] §fSaved configs:"));

                        for (String config : configs) {
                            CommandManager.addChatMessage("§7- §f" + config);
                        }
                    }
                    break;
                case "remove":
                case "del":
                case "delete":
                    if (args.length < 2) {
                        CommandManager.addChatMessage(CommandManager.tr("Использование: .cfg remove <имя>", "Usage: .cfg remove <codeName>"));
                        return;
                    }

                    String deleteName = args[1];
                    if (ConfigManager.INSTANCE.deleteConfig(deleteName)) {
                        CommandManager.addChatMessage(
                            CommandManager.tr("§a[bamber client] §fКонфиг удален: §e", "§a[bamber client] §fConfig removed: §e") + deleteName
                        );
                    } else {
                        CommandManager.addChatMessage(
                            CommandManager.tr("§c[bamber client] Конфиг не найден: ", "§c[bamber client] Config not found: ") + deleteName
                        );
                    }
                    break;
                default:
                    CommandManager.addChatMessage(CommandManager.tr("Неизвестное действие. Использование: ", "Unknown action. Usage: ") + this.getSyntax());
            }
        }
    }
}
