package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.BooleanSetting;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.particle.ParticleTypes;

public class NoRender extends Module {
    public static final BooleanSetting fire = new BooleanSetting("Огонь", true);
    public static final BooleanSetting pumpkin = new BooleanSetting("Тыква", true);
    public static final BooleanSetting glow = new BooleanSetting("Свечение", false);
    public static final BooleanSetting scoreboard = new BooleanSetting("Таблица", false);
    public static final BooleanSetting nameTags = new BooleanSetting("Ники", false);
    public static final BooleanSetting hurtCam = new BooleanSetting("Тряска", true);
    public static final BooleanSetting totemAnimation = new BooleanSetting("Тотем", true);
    public static final BooleanSetting eatParticles = new BooleanSetting("Еда", true);
    public static final BooleanSetting explosions = new BooleanSetting("Взрывы", true);

    public NoRender() {
        super("NoRender", "Отключает рендер определенных элементов", Category.VISUALS);
        this.addSettings(fire, pumpkin, glow, scoreboard, nameTags, hurtCam, totemAnimation, eatParticles, explosions);
    }

    public static boolean isActive(BooleanSetting setting) {
        NoRender mod = ModuleManager.INSTANCE != null ? ModuleManager.INSTANCE.getModule(NoRender.class) : null;
        return mod != null && mod.isEnabled() && setting.getValue();
    }

    public static boolean shouldBlockParticle(ParticleEffect parameters) {
        if (parameters == null) {
            return false;
        }

        ParticleType type = parameters.getType();
        return isActive(eatParticles) && type == ParticleTypes.ITEM
            ? true
            : isActive(explosions) && (type == ParticleTypes.EXPLOSION || type == ParticleTypes.EXPLOSION_EMITTER || type == ParticleTypes.POOF);
    }
}
