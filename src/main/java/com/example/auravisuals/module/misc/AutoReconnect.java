package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.server.AutoReconnectTracker;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;

public class AutoReconnect extends Module {
    private final NumberSetting reconnectDelay = new NumberSetting("Задержка перезахода", 2.0, 0.5, 10.0, 0.5);

    public AutoReconnect() {
        super("AutoReconnect", "Автоматически переподключается после кика", Category.MISC);
        this.addSettings(this.reconnectDelay);
    }

    @Override
    public void onTick() {
        if (this.mc.currentScreen instanceof DisconnectedScreen) {
            long delayMs = (long)(this.reconnectDelay.getValue() * 1000.0);
            if (AutoReconnectTracker.canAttemptReconnect(delayMs)) {
                ServerInfo lastServer = AutoReconnectTracker.getLastServer();
                if (lastServer != null) {
                    AutoReconnectTracker.markReconnectAttempted();
                    ConnectScreen.connect(new MultiplayerScreen(new TitleScreen()), this.mc, ServerAddress.parse(lastServer.address), lastServer, false, null);
                }
            }
        }
    }
}
