package com.example.auravisuals.module.player;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.StringSetting;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket.Mode;

public class QuickRespawn extends Module {
    private final BooleanSetting sendRespawnMessage = new BooleanSetting("Сообщение после респавна", true);
    private final StringSetting respawnMessage = new StringSetting("Текст сообщения", "/back", () -> this.sendRespawnMessage.getValue());
    private boolean wasDead;
    private boolean pendingRespawnMessage;

    public QuickRespawn() {
        super("QuickRespawn", "Автоматический респавн после смерти", Category.PLAYER);
        this.addSettings(this.sendRespawnMessage, this.respawnMessage);
    }

    @Override
    public void onDisable() {
        this.wasDead = false;
        this.pendingRespawnMessage = false;
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.getNetworkHandler() != null) {
            boolean deadNow = this.mc.player.isDead() || this.mc.currentScreen instanceof DeathScreen;
            if (deadNow && !this.wasDead) {
                this.mc.player.networkHandler.sendPacket(new ClientStatusC2SPacket(Mode.PERFORM_RESPAWN));
                this.pendingRespawnMessage = this.sendRespawnMessage.getValue();
            }

            if (this.mc.currentScreen instanceof DeathScreen) {
                this.mc.setScreen(null);
            }

            if (!deadNow && this.wasDead && this.pendingRespawnMessage) {
                this.sendConfiguredRespawnMessage();
                this.pendingRespawnMessage = false;
            }

            this.wasDead = deadNow;
        } else {
            this.wasDead = false;
            this.pendingRespawnMessage = false;
        }
    }

    private void sendConfiguredRespawnMessage() {
        String message = this.respawnMessage.getValue();
        if (message != null) {
            String trimmed = message.trim();
            if (!trimmed.isEmpty()) {
                if (trimmed.startsWith("/")) {
                    String command = trimmed.substring(1).trim();
                    if (!command.isEmpty()) {
                        this.mc.getNetworkHandler().sendChatCommand(command);
                    }
                } else {
                    this.mc.getNetworkHandler().sendChatMessage(trimmed);
                }
            }
        }
    }
}
