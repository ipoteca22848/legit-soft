package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.ActionSetting;
import com.example.auravisuals.setting.BindSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.setting.StringSetting;

public class Macros extends Module {
    public static final int MAX_MACROS = 10;
    public final NumberSetting macroCount = new NumberSetting("Количество макросов", 1.0, 0.0, 10.0, 1.0);
    public final ActionSetting addMacroBtn = new ActionSetting("+ Добавить макрос", this::addMacro);
    public final ActionSetting removeMacroBtn = new ActionSetting("- Удалить макрос", () -> this.removeMacro(this.getMacroCount() - 1));
    private final StringSetting[] commands = new StringSetting[10];
    private final BindSetting[] binds = new BindSetting[10];
    private final boolean[] pressed = new boolean[10];

    public Macros() {
        super("Macros", "Запускает команды по кнопкам", Category.MISC);
        this.macroCount.setDependency(() -> false);
        this.addSettings(this.macroCount, this.addMacroBtn);

        for (int i = 0; i < 10; i++) {
            int index = i;
            int num = i + 1;
            this.commands[i] = new StringSetting("Макрос " + num + " команда", "", () -> index < this.macroCount.getIntValue());
            this.binds[i] = new BindSetting("Макрос " + num + " кнопка", 0, () -> index < this.macroCount.getIntValue());
            this.addSettings(this.commands[i], this.binds[i]);
        }
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.getNetworkHandler() != null && !BindSetting.shouldBlockKeyInput()) {
            int count = this.getMacroCount();

            for (int i = 0; i < count; i++) {
                this.handle(i);
            }

            for (int var3 = count; var3 < this.pressed.length; var3++) {
                this.pressed[var3] = false;
            }
        }
    }

    public int getMacroCount() {
        return Math.max(0, Math.min(10, this.macroCount.getIntValue()));
    }

    public boolean addMacro() {
        int count = this.getMacroCount();
        if (count >= 10) {
            return false;
        }

        this.macroCount.setValue((double)(count + 1));
        return true;
    }

    public void removeMacro(int index) {
        int count = this.getMacroCount();
        if (count > 0 && index >= 0 && index < count) {
            for (int i = index; i < count - 1; i++) {
                this.commands[i].setValue(this.commands[i + 1].getValue());
                this.binds[i].setValue(this.binds[i + 1].getValue());
                this.pressed[i] = this.pressed[i + 1];
            }

            int last = count - 1;
            this.commands[last].setValue("");
            this.binds[last].setValue(0);
            this.pressed[last] = false;
            this.macroCount.setValue((double)(count - 1));
        }
    }

    public StringSetting getCommandSetting(int index) {
        return this.commands[index];
    }

    public BindSetting getBindSetting(int index) {
        return this.binds[index];
    }

    private void handle(int index) {
        BindSetting bind = this.binds[index];
        if (bind.getValue() != null && bind.getValue() != 0 && bind.getValue() != -1) {
            boolean down = BindSetting.isPressed(this.mc.getWindow().getHandle(), bind.getValue());
            if (down && !this.pressed[index]) {
                this.runCommand(this.commands[index].getValue());
            }

            this.pressed[index] = down;
        }
    }

    private void runCommand(String raw) {
        if (raw != null) {
            String command = raw.trim();
            if (!command.isEmpty()) {
                if (command.startsWith("/")) {
                    this.mc.getNetworkHandler().sendChatCommand(command.substring(1));
                } else {
                    this.mc.getNetworkHandler().sendChatMessage(command);
                }
            }
        }
    }
}
