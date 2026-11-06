package com.example.auravisuals;

import com.example.auravisuals.util.ClientProfile;
import com.example.auravisuals.util.chat.ChatHubClient;
import com.example.auravisuals.util.sound.CustomSoundUtil;
import com.example.auravisuals.util.sound.SoundUtil;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuraVisuals implements ModInitializer {
    public static final String MOD_ID = "auravisuals";
    public static final Logger LOGGER = LoggerFactory.getLogger("auravisuals");

    public void onInitialize() {
        SoundUtil.init();
        CustomSoundUtil.init();
        LOGGER.info("anvil visual initialized!");
        this.prefetchAvatar();
    }

    private void prefetchAvatar() {
        Thread thread = new Thread(() -> {
            for (int i = 0; i < 20; i++) {
                try {
                    Thread.sleep(3000L);
                } catch (InterruptedException var2) {
                    return;
                }

                String url = ClientProfile.getAvatarUrl();
                if (url != null && !url.isBlank()) {
                    ChatHubClient.INSTANCE.loadAvatar(url);
                    return;
                }
            }
        }, "aura-avatar-prefetch");
        thread.setDaemon(true);
        thread.start();
    }
}
