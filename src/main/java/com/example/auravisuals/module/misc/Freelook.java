package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BindSetting;
import com.example.auravisuals.setting.NumberSetting;
import net.minecraft.client.option.Perspective;

public class Freelook extends Module {
    public static boolean isActive = false;
    private float cameraYaw = 0.0F;
    private float cameraPitch = 0.0F;
    private Perspective previousPerspective;
    public static final BindSetting activateKey = new BindSetting("Кнопка активации", 342);
    public static final NumberSetting speed = new NumberSetting("Скорость", 1.0, 0.1, 2.0, 0.1);

    public Freelook() {
        super("Freelook", "Свободная камера при удержании клавиши", Category.MISC);
        this.addSettings(activateKey, speed);
    }

    @Override
    public void onTick() {
        if (this.mc.player != null) {
            boolean isKeyDown = BindSetting.isPressed(this.mc.getWindow().getHandle(), activateKey.getValue()) && !BindSetting.shouldBlockKeyInput();
            if (isKeyDown) {
                if (!isActive) {
                    isActive = true;
                    this.previousPerspective = this.mc.options.getPerspective();
                    this.mc.options.setPerspective(Perspective.THIRD_PERSON_BACK);
                    this.cameraYaw = this.mc.player.getYaw();
                    this.cameraPitch = this.mc.player.getPitch();
                }

                float var3 = this.mc.player.getYaw() - this.mc.player.lastYaw;
            } else if (isActive) {
                isActive = false;
                if (this.previousPerspective != null) {
                    this.mc.options.setPerspective(this.previousPerspective);
                }
            }
        }
    }

    public float getCameraYaw() {
        return this.cameraYaw;
    }

    public float getCameraPitch() {
        return this.cameraPitch;
    }

    public void setCameraYaw(float yaw) {
        this.cameraYaw = yaw;
    }

    public void setCameraPitch(float pitch) {
        this.cameraPitch = pitch;
    }
}
