package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.ModeSetting;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.SlotActionType;

public class AutoResell extends Module {
    private final ModeSetting mode = new ModeSetting("Режим", "Обычный игрок", "Обычный игрок", "Князь");
    private long resellLastExitTime = 0L;
    private boolean resellIsWorking = false;
    private int resellStage = 0;
    private long resellDelayTimer = 0L;
    private long closeContainerUntil = 0L;
    private long lastCloseContainerAttempt = 0L;

    public AutoResell() {
        super("AutoResell", "Автоматически перевыставляет вещи на аукционе", Category.MISC);
        this.addSettings(this.mode);
    }

    @Override
    public void onDisable() {
        this.resetState();
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.world != null && this.mc.getNetworkHandler() != null) {
            this.closePendingContainer();
            if (this.mode.is("Князь")) {
                if (System.currentTimeMillis() - this.resellLastExitTime >= 61000L) {
                    this.mc.getNetworkHandler().sendChatCommand("ah resell");
                    this.resellLastExitTime = System.currentTimeMillis();
                }
            } else if (!this.resellIsWorking) {
                if (System.currentTimeMillis() - this.resellLastExitTime >= 61000L) {
                    this.mc.getNetworkHandler().sendChatCommand("ah");
                    this.resellIsWorking = true;
                    this.resellStage = 1;
                    this.resellDelayTimer = System.currentTimeMillis();
                }
            } else {
                if (this.mc.currentScreen instanceof HandledScreen screen) {
                    if (this.mc.interactionManager == null) {
                        return;
                    }

                    String title = screen.getTitle().getString().toLowerCase();
                    if (this.resellStage == 1 && title.contains("аукцион")) {
                        if (System.currentTimeMillis() - this.resellDelayTimer > 300L) {
                            this.mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, 46, 0, SlotActionType.PICKUP, this.mc.player);
                            this.resellStage = 2;
                            this.resellDelayTimer = System.currentTimeMillis();
                        }
                    } else if (this.resellStage == 2 && title.contains("хран") && System.currentTimeMillis() - this.resellDelayTimer > 300L) {
                        this.mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, 52, 0, SlotActionType.PICKUP, this.mc.player);
                        this.scheduleContainerClose();
                        this.resellIsWorking = false;
                        this.resellStage = 0;
                        this.resellLastExitTime = System.currentTimeMillis();
                    }
                } else if (this.mc.currentScreen == null && this.resellStage > 0 && System.currentTimeMillis() - this.resellDelayTimer > 2000L) {
                    this.resellIsWorking = false;
                    this.resellStage = 0;
                    this.resellLastExitTime = System.currentTimeMillis();
                }
            }
        } else {
            this.resetState();
        }
    }

    private void scheduleContainerClose() {
        this.closeContainerUntil = System.currentTimeMillis() + 1500L;
        this.lastCloseContainerAttempt = 0L;
        this.closePendingContainer();
    }

    private void closePendingContainer() {
        if (this.closeContainerUntil > 0L) {
            long now = System.currentTimeMillis();
            if (now > this.closeContainerUntil) {
                this.closeContainerUntil = 0L;
            } else if (this.mc.currentScreen instanceof HandledScreen) {
                if (now - this.lastCloseContainerAttempt >= 100L) {
                    this.lastCloseContainerAttempt = now;
                    if (this.mc.player != null) {
                        this.mc.player.closeHandledScreen();
                    }

                    this.mc.setScreen(null);
                }
            }
        }
    }

    private void resetState() {
        this.resellIsWorking = false;
        this.resellStage = 0;
        this.resellDelayTimer = 0L;
        this.resellLastExitTime = 0L;
        this.closeContainerUntil = 0L;
        this.lastCloseContainerAttempt = 0L;
    }
}
