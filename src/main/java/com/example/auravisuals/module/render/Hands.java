package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;

public final class Hands extends Module {
    public final BooleanSetting hmiEnable = new BooleanSetting("Анимации", true);
    public final ModeSetting hmiAnimationType = new ModeSetting("Тип анимации", "Плавный", "Плавный", "Шарп");
    public final NumberSetting hmiSmoothness = new NumberSetting("Плавность", 1.0, 0.35, 2.5, 0.05);
    public final BooleanSetting swapHands = new BooleanSetting("Поменять руки", false);
    public final BooleanSetting mb3DCompat = new BooleanSetting("3D предметы (MB совместимость)", false);
    public final BooleanSetting climbAndCrawl = new BooleanSetting("Лазанье и ползание", true);
    public final BooleanSetting swimmingAnimation = new BooleanSetting("Плавание", true);

    public Hands() {
        super("Hands", "Живые анимации предмета в руке", Category.VISUALS);
        this.addSettings(this.hmiEnable, this.hmiAnimationType, this.hmiSmoothness, this.swapHands, this.mb3DCompat, this.climbAndCrawl, this.swimmingAnimation);
    }

    public static Hands getInstance() {
        return ModuleManager.INSTANCE == null ? null : ModuleManager.INSTANCE.getModule(Hands.class);
    }

    public boolean isState() {
        return this.isEnabled();
    }
}
