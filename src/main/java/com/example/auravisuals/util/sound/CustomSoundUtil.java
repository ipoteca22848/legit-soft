package com.example.auravisuals.util.sound;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class CustomSoundUtil {
    private static final String[] SOUND_NAMES = new String[]{
        "enable", "enable1", "enable2", "disable", "disable1", "disable2", "kill2", "nya", "skeet", "uwu", "welcome", "bell"
    };
    private static final Map<String, SoundEvent> SOUND_EVENTS = new HashMap<>();

    public static void init() {
        for (String name : SOUND_NAMES) {
            Identifier id = Identifier.of("auravisuals", name);
            SoundEvent event = SoundEvent.of(id);
            Registry.register(Registries.SOUND_EVENT, id, event);
            SOUND_EVENTS.put(name, event);
        }
    }

    public static void playSound(String soundName) {
        playSound(soundName, 1.0F);
    }

    public static void playSound(String soundName, float volume) {
        if (soundName != null && !soundName.equals("None")) {
            SoundEvent soundEvent = SOUND_EVENTS.get(soundName);
            if (soundEvent != null) {
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(soundEvent, 1.0F, volume));
            }
        }
    }
}
