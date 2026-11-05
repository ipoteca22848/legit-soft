package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.ModeSetting;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.slot.SlotActionType;

public class AhResell extends Module {
    public final ModeSetting mode = new ModeSetting("Режим", "Князь", "Князь", "Обычный игрок");
    private long lastExitTime = 0L;
    private boolean isWorking = false;
    private int stage = 0;
    private long delayTimer = 0L;

    public AhResell() {
        super("AhResell", "Автоматическая перепродажа на аукционе", Category.MISC);
        this.addSettings(this.mode);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.lastExitTime = System.currentTimeMillis();
        this.isWorking = false;
        this.stage = 0;
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.world != null) {
            if (this.mode.is("Князь")) {
                if (System.currentTimeMillis() - this.lastExitTime >= 61000L) {
                    if (this.mc.getNetworkHandler() != null) {
                        this.mc.getNetworkHandler().sendChatCommand("ah resell");
                    }

                    this.lastExitTime = System.currentTimeMillis();
                }
            } else if (this.mode.is("Обычный игрок")) {
                if (!this.isWorking) {
                    if (System.currentTimeMillis() - this.lastExitTime >= 61000L) {
                        if (this.mc.getNetworkHandler() != null) {
                            this.mc.getNetworkHandler().sendChatCommand("ah");
                        }

                        this.isWorking = true;
                        this.stage = 1;
                        this.delayTimer = System.currentTimeMillis();
                    }
                } else if (this.mc.currentScreen instanceof HandledScreen screen) {
                    String title = screen.getTitle().getString().toLowerCase();
                    if (this.stage == 1 && title.contains("аукцион")) {
                        if (System.currentTimeMillis() - this.delayTimer > 300L) {
                            this.mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, 46, 0, SlotActionType.PICKUP, this.mc.player);
                            this.stage = 2;
                            this.delayTimer = System.currentTimeMillis();
                        }
                    } else if (this.stage == 2 && title.contains("хран") && System.currentTimeMillis() - this.delayTimer > 300L) {
                        this.mc.interactionManager.clickSlot(screen.getScreenHandler().syncId, 52, 0, SlotActionType.PICKUP, this.mc.player);
                        this.mc.player.closeHandledScreen();
                        this.mc.setScreen(null);
                        this.isWorking = false;
                        this.stage = 0;
                        this.lastExitTime = System.currentTimeMillis();
                    }
                } else if (this.mc.currentScreen == null && this.stage > 0 && System.currentTimeMillis() - this.delayTimer > 2000L) {
                    this.isWorking = false;
                    this.stage = 0;
                    this.lastExitTime = System.currentTimeMillis();
                }
            }
        }
    }
}
