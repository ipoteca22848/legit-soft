package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.MultiBooleanSetting;
import com.example.auravisuals.setting.StringSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public class AutoMessage extends Module {
    private PlayerEntity target;
    private long targetHitMs = 0L;
    private long lastTimeSendMs = 0L;
    private static final double MAX_TARGET_DISTANCE = 200.0;
    private static final double MAX_TARGET_DISTANCE_SQ = 40000.0;
    private static final long KILL_TARGET_WINDOW_MS = 30000L;
    public final MultiBooleanSetting mode = new MultiBooleanSetting("Режим:", "После убийства", "По времени");
    public final StringSetting afterKillMessage = new StringSetting("Текст после убийства: ", "GG", () -> this.mode.isEnabled("После убийства"));
    public final StringSetting timeIntervalSec = new StringSetting("Частота отправки (в сек): ", "5", () -> this.mode.isEnabled("По времени"));
    public final StringSetting timeText = new StringSetting("Текст: ", "!gg", () -> this.mode.isEnabled("По времени"));

    public AutoMessage() {
        super("AutoMessage", "Автоматические сообщения", Category.MISC);
        this.mode.disableAll();
        this.mode.set("После убийства", true);
        this.mode.set("По времени", false);
        this.addSettings(this.mode, this.afterKillMessage, this.timeIntervalSec, this.timeText);
    }

    public void setTarget(Entity entity) {
        if (this.mc.player != null) {
            if (entity instanceof PlayerEntity player && entity != this.mc.player) {
                this.target = player;
                this.targetHitMs = System.currentTimeMillis();
            }
        }
    }

    @Override
    public void onEnable() {
        this.lastTimeSendMs = System.currentTimeMillis();
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.world != null) {
            boolean afterKillEnabled = this.mode.isEnabled("После убийства");
            boolean timeEnabled = this.mode.isEnabled("По времени");
            if (afterKillEnabled && this.target != null) {
                long now = System.currentTimeMillis();
                if (this.mc.player.isDead() || this.mc.player.getHealth() <= 0.0F || now - this.targetHitMs > 30000L) {
                    this.target = null;
                } else if (!this.target.isAlive() || this.target.isDead() || this.target.getHealth() <= 0.0F) {
                    String msg = this.afterKillMessage.getValue();
                    if (msg != null && !msg.isBlank()) {
                        this.mc.player.networkHandler.sendChatMessage(msg);
                    }

                    this.target = null;
                } else if (this.mc.player.squaredDistanceTo(this.target) > 40000.0) {
                    this.target = null;
                }
            }

            if (timeEnabled) {
                long now = System.currentTimeMillis();

                int intervalSec;
                try {
                    intervalSec = Integer.parseInt(this.timeIntervalSec.getValue().trim());
                } catch (Exception e) {
                    intervalSec = 0;
                }

                long intervalMs = intervalSec * 1000L;
                if (intervalSec > 0 && now - this.lastTimeSendMs >= intervalMs) {
                    String msg = this.timeText.getValue();
                    if (msg != null && !msg.isBlank()) {
                        this.mc.player.networkHandler.sendChatMessage(msg);
                    }

                    this.lastTimeSendMs = now;
                }
            }
        }
    }
}
