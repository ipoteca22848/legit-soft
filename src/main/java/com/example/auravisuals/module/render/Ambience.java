package com.example.auravisuals.module.render;

import com.example.auravisuals.AuraVisuals;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.theme.ThemeManager;
import com.example.auravisuals.util.render.SkyEffectPipeline;
import java.awt.Color;
import net.minecraft.client.render.Camera;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Ambience extends Module {
    static final int CLASSIC_MODE_COUNT = 7;
    static final String[] MODES = new String[]{
        "Волны",
        "Плазма",
        "Вода",
        "Тень",
        "Звёзды",
        "Солнце",
        "Комета",
        "WebShader",
        "Aurora",
        "Pulse",
        "Neon",
        "Smoke",
        "Ripple",
        "Crystal",
        "Plasma2",
        "Nebula",
        "Galaxy",
        "Ocean",
        "Sunset",
        "Cloud",
        "Shimmer",
        "Prism",
        "Radiance"
    };
    public static final ModeSetting shader = new Ambience$1("Шейдер", "Солнце", MODES);
    public static final NumberSetting opacity = new NumberSetting("Прозрачность", 100.0, 0.0, 100.0, 1.0);
    public static final NumberSetting speed = new NumberSetting("Скорость", 0.35, 0.1, 3.0, 0.05);
    public static final NumberSetting scale = new NumberSetting("Масштаб", 4.0, 0.5, 12.0, 0.5, Ambience::isClassicMode);
    public static final BooleanSetting useClientColor = new BooleanSetting("Цвет клиента", true, () -> !isClassicMode());
    public static final ColorSetting color = new ColorSetting("Цвет", new Color(204, 26, 26), () -> !isClassicMode() && !useClientColor.getValue());
    boolean loggedRenderError;
    final Quaternionf cameraRotation = new Quaternionf();
    final Vector3f forward = new Vector3f();
    final Vector3f up = new Vector3f();
    final Vector3f right = new Vector3f();

    public Ambience() {
        super("Ambience", "Кастомные шейдеры на небо", Category.VISUALS);
        this.addSettings(shader, opacity, speed, scale, useClientColor, color);
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.mc.world != null && this.mc.player != null) {
            Camera camera = context.camera();
            if (camera != null) {
                int rgb = !isClassicMode() && !useClientColor.getValue()
                    ? color.getValue() & 16777215
                    : ThemeManager.INSTANCE.getVisualModulesColor() & 16777215;
                float red = (rgb >> 16 & 0xFF) / 255.0F;
                float green = (rgb >> 8 & 0xFF) / 255.0F;
                float blue = (rgb & 0xFF) / 255.0F;
                float alpha = opacity.getIntValue() / 100.0F;
                float time = (float)(System.currentTimeMillis() % 1000000L) / 1000.0F * speed.getFloatValue();
                this.cameraRotation.set(camera.getRotation());
                this.forward.set(0.0F, 0.0F, -1.0F);
                this.up.set(0.0F, 1.0F, 0.0F);
                this.right.set(1.0F, 0.0F, 0.0F);
                this.cameraRotation.transform(this.forward);
                this.cameraRotation.transform(this.up);
                this.cameraRotation.transform(this.right);
                float projectionY = (float)Math.tan(Math.toRadians(this.mc.options.getFov().getValue().intValue() * 0.5));
                float aspect = (float)this.mc.getWindow().getFramebufferWidth() / Math.max(1, this.mc.getWindow().getFramebufferHeight());
                float projectionX = projectionY * aspect;

                try {
                    SkyEffectPipeline.render(
                        this.forward, this.up, this.right, projectionX, projectionY, red, green, blue, alpha, time, scale.getFloatValue(), shader.getIndex()
                    );
                } catch (Throwable error) {
                    if (!this.loggedRenderError) {
                        this.loggedRenderError = true;
                        AuraVisuals.LOGGER.error("Unable to render Ambience sky shader", error);
                    }
                }
            }
        }
    }

    static boolean isClassicMode() {
        return shader.getIndex() < 7;
    }

    public static String[] shaderModes() {
        return (String[])MODES.clone();
    }
}
