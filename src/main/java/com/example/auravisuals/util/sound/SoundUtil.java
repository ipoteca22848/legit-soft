package com.example.auravisuals.util.sound;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class SoundUtil {
    public static final Identifier OPEN_SOUND_ID = Identifier.of("auravisuals", "gui_open");
    public static final Identifier CLOSE_SOUND_ID = Identifier.of("auravisuals", "gui_close");
    public static final Identifier SIXSEVEN_SOUND_ID = Identifier.of("auravisuals", "sixseven");
    public static final Identifier IOS_TYPE_SOUND_ID = Identifier.of("auravisuals", "ios_type");
    public static final SoundEvent GUI_OPEN = SoundEvent.of(OPEN_SOUND_ID);
    public static final SoundEvent GUI_CLOSE = SoundEvent.of(CLOSE_SOUND_ID);
    public static final SoundEvent SIXSEVEN = SoundEvent.of(SIXSEVEN_SOUND_ID);
    public static final SoundEvent IOS_TYPE = SoundEvent.of(IOS_TYPE_SOUND_ID);

    public static void init() {
        Registry.register(Registries.SOUND_EVENT, OPEN_SOUND_ID, GUI_OPEN);
        Registry.register(Registries.SOUND_EVENT, CLOSE_SOUND_ID, GUI_CLOSE);
        Registry.register(Registries.SOUND_EVENT, SIXSEVEN_SOUND_ID, SIXSEVEN);
        Registry.register(Registries.SOUND_EVENT, IOS_TYPE_SOUND_ID, IOS_TYPE);
    }

    public static void playOpen() {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(GUI_OPEN, 1.0F));
    }

    public static void playClose() {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(GUI_CLOSE, 1.0F));
    }

    public static void playSixseven() {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(SIXSEVEN, 1.0F));
    }

    public static void playIosType() {
        MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.ui(IOS_TYPE, 1.0F));
    }
}
