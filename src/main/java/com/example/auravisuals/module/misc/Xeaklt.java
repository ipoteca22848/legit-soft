package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.sound.CustomSoundUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

public class Xeaklt extends Module {
    private long lastRealCritAt;
    private long lastCustomCritAt;
    private long lastLocalAttackAt;
    public final NumberSetting volume = new NumberSetting("Громкость", 1.0, 0.1, 3.0, 0.1);
    public final BooleanSetting enableSound = new BooleanSetting("Включение/выключение модулей", true);
    public final ModeSetting enableMode;
    public final BooleanSetting critSound = new BooleanSetting("Крит", true);
    public final ModeSetting critMode;

    public Xeaklt() {
        super("Client Sounds", "Кастомные звуки", Category.MISC);
        this.enableMode = new ModeSetting("Выберите режим: ", "1", () -> this.enableSound.getValue(), "1", "2", "3");
        this.critMode = new ModeSetting("Режим крита", "bell", () -> this.critSound.getValue(), "bell", "uwu", "skeet", "nya");
        this.addSettings(this.volume, this.enableSound, this.enableMode, this.critSound, this.critMode);
    }

    public void playEnableSound() {
        if (this.isEnabled() && this.enableSound.getValue()) {
            CustomSoundUtil.playSound(this.getEnableSoundName(), this.volume.getValue().floatValue());
        }
    }

    public void playDisableSound() {
        if (this.isEnabled() && this.enableSound.getValue()) {
            CustomSoundUtil.playSound(this.getDisableSoundName(), this.volume.getValue().floatValue());
        }
    }

    public void onCrit() {
        if (this.isEnabled() && this.critSound.getValue()) {
            this.lastCustomCritAt = System.currentTimeMillis();
            CustomSoundUtil.playSound(this.critMode.getValue(), this.volume.getValue().floatValue());
        }
    }

    public void onAttackEntity(PlayerEntity player, Entity target) {
        if (this.isEnabled() && this.critSound.getValue() && player != null && target != null) {
            long now;
            this.lastLocalAttackAt = now = System.currentTimeMillis();
            if (this.isCriticalAttack(player)) {
                if (now - this.lastCustomCritAt >= 90L) {
                    this.lastRealCritAt = now;
                    this.onCrit();
                }
            }
        }
    }

    public void markRealCrit() {
        this.lastRealCritAt = System.currentTimeMillis();
    }

    public boolean shouldReplaceCritSound() {
        return System.currentTimeMillis() - this.lastRealCritAt <= 250L;
    }

    public boolean wasCustomCritJustPlayed() {
        return System.currentTimeMillis() - this.lastCustomCritAt <= 250L;
    }

    public boolean wasLocalAttackJustPerformed() {
        return System.currentTimeMillis() - this.lastLocalAttackAt <= 1000L;
    }

    private boolean isCriticalAttack(PlayerEntity player) {
        boolean falling = player.fallDistance > 0.0 || player.getVelocity().y < -0.01;
        return falling
            && !player.isOnGround()
            && !player.isClimbing()
            && !player.isTouchingWater()
            && !player.hasStatusEffect(StatusEffects.BLINDNESS)
            && !player.hasVehicle()
            && !player.isSprinting();
    }

    private String getEnableSoundName() {
        String mode = this.enableMode.getValue();
        if ("2".equals(mode)) {
            return "enable1";
        } else {
            return "3".equals(mode) ? "enable2" : "enable";
        }
    }

    private String getDisableSoundName() {
        String mode = this.enableMode.getValue();
        if ("2".equals(mode)) {
            return "disable1";
        } else {
            return "3".equals(mode) ? "disable2" : "disable";
        }
    }
}
