package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.server.FunTimeUtil;

public class AutoEvent extends Module {
    public final BooleanSetting autoPoint = new BooleanSetting("Авто точка", false);
    private final BooleanSetting timerMode = new BooleanSetting("Прописывать по таймеру", false);
    private final NumberSetting timerSeconds = new NumberSetting("Таймер", 60.0, 20.0, 600.0, 1.0, () -> this.timerMode.getValue());
    private long autoEventStart = 0L;
    private boolean autoEventPending = false;
    private boolean autoEventSent = false;
    private long lastTimerCommand = 0L;
    private boolean wasInWorld = false;
    private int lastWorldId = -1;
    private String lastWorldKey = "";

    public AutoEvent() {
        super("AutoEvent", "Автоматический /event delay", Category.MISC);
        this.addSettings(this.autoPoint, this.timerMode, this.timerSeconds);
    }

    @Override
    public void onDisable() {
        this.resetState();
    }

    @Override
    public void onTick() {
        boolean inWorld = this.mc.player != null && this.mc.world != null && this.mc.getNetworkHandler() != null;
        if (!inWorld) {
            this.resetState();
        } else {
            int worldId = System.identityHashCode(this.mc.world);
            String worldKey = this.mc.world.getRegistryKey().getValue().toString();
            boolean worldChanged = worldId != this.lastWorldId || !worldKey.equals(this.lastWorldKey);
            if (!this.wasInWorld || worldChanged) {
                this.autoEventPending = false;
                this.autoEventSent = false;
                this.lastTimerCommand = 0L;
            }

            this.wasInWorld = true;
            this.lastWorldId = worldId;
            this.lastWorldKey = worldKey;
            boolean funTime = FunTimeUtil.isFunTimeServer(this.mc);
            if (!this.timerMode.getValue()) {
                if (!this.autoEventSent && !this.autoEventPending && funTime) {
                    this.autoEventStart = System.currentTimeMillis();
                    this.autoEventPending = true;
                }

                if (this.autoEventPending && System.currentTimeMillis() - this.autoEventStart >= 500L) {
                    this.sendEventCommands();
                    this.autoEventPending = false;
                    this.autoEventSent = true;
                }
            } else {
                long delay = Math.max(20L, Math.min(600L, this.timerSeconds.getIntValue())) * 1000L;
                if (funTime && (this.lastTimerCommand == 0L || System.currentTimeMillis() - this.lastTimerCommand >= delay)) {
                    this.sendEventCommands();
                    this.lastTimerCommand = System.currentTimeMillis();
                }
            }
        }
    }

    private void sendEventCommands() {
        if (this.mc.player != null && this.mc.player.networkHandler != null) {
            this.mc.player.networkHandler.sendChatCommand("event delay");
            if (this.autoPoint.getValue()) {
                this.mc.player.networkHandler.sendChatCommand("point");
            }
        }
    }

    private void resetState() {
        this.wasInWorld = false;
        this.autoEventPending = false;
        this.autoEventSent = false;
        this.autoEventStart = 0L;
        this.lastTimerCommand = 0L;
        this.lastWorldId = -1;
        this.lastWorldKey = "";
    }
}
