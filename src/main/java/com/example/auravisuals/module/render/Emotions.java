package com.example.auravisuals.module.render;

import com.example.auravisuals.emotions.EmotionWheelManager;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BindSetting;
import net.minecraft.client.gui.DrawContext;

public class Emotions extends Module {
    public static Emotions INSTANCE;
    public final BindSetting wheelBind = new BindSetting("Колесо эмоций", 86);

    public Emotions() {
        super("Emotions", "Эмоции", Category.VISUALS);
        INSTANCE = this;
        this.addSettings(this.wheelBind);
        this.setEnabledSilently(true);
    }

    @Override
    public void onDisable() {
        EmotionWheelManager.INSTANCE.closeWheel(true);
    }

    @Override
    public void onTick() {
        EmotionWheelManager.INSTANCE.tick(this);
    }

    @Override
    public void onHudRender(DrawContext context) {
        EmotionWheelManager.INSTANCE.render(context);
    }
}
