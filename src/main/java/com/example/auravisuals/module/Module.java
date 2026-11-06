package com.example.auravisuals.module;

import com.example.auravisuals.client.gui.hud.Notifications;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.misc.Xeaklt;
import com.example.auravisuals.setting.Setting;
import com.example.auravisuals.util.language.LanguageManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public abstract class Module {
    public MinecraftClient mc = MinecraftClient.getInstance();
    private final String name;
    private final String description;
    private final Category category;
    private boolean enabled;
    private int key = 0;
    private boolean isBinding = false;
    public boolean wasPressed = false;
    private final List<Setting<?>> settings = new ArrayList<>();

    public Module(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
    }

    protected void addSettings(Setting<?>... settings) {
        this.settings.addAll(Arrays.asList(settings));
    }

    public List<Setting<?>> getSettings() {
        return this.settings;
    }

    public void toggle() {
        if (this.mc == null) {
            this.mc = MinecraftClient.getInstance();
        }

        boolean bl = this.enabled = !this.enabled;
        if (this.enabled) {
            this.onEnable();
            Notifications.post(this, true);
            Xeaklt xeaklt = ModuleManager.INSTANCE.getModule(Xeaklt.class);
            if (xeaklt != null) {
                xeaklt.playEnableSound();
            }
        } else {
            this.onDisable();
            Notifications.post(this, false);
            Xeaklt xeaklt = ModuleManager.INSTANCE.getModule(Xeaklt.class);
            if (xeaklt != null) {
                xeaklt.playDisableSound();
            }
        }
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void onTick() {
    }

    public void onHudRender(DrawContext context) {
    }

    public void onWorldRender(WorldRenderContext context) {
    }

    public String getName() {
        return LanguageManager.repairIrcMojibake(this.name);
    }

    public String getConfigKey() {
        return this.getClass().getSimpleName();
    }

    public String getDescription() {
        return LanguageManager.repairIrcMojibake(this.description);
    }

    public Category getCategory() {
        return this.category;
    }

    public boolean isEnabled() {
        if (this.mc == null) {
            this.mc = MinecraftClient.getInstance();
        }

        return com.anvil.client.AnvilRuntime.isActive() && this.enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.toggle();
        }
    }

    public void setEnabledSilently(boolean enabled) {
        this.enabled = enabled;
    }

    public int getKey() {
        return this.key;
    }

    public void setKey(int key) {
        this.key = key;
    }

    public boolean isBinding() {
        return this.isBinding;
    }

    public void setBinding(boolean binding) {
        this.isBinding = binding;
    }
}
