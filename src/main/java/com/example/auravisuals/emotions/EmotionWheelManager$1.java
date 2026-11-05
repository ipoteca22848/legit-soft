package com.example.auravisuals.emotions;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.SkinTextures;

public class EmotionWheelManager$1 extends OtherClientPlayerEntity {
    EmotionWheelManager$1(EmotionWheelManager this$0, ClientWorld clientWorld, GameProfile gameProfile) {
        super(clientWorld, gameProfile);
    }

    @Override
    public SkinTextures getSkin() {
        return DefaultSkinHelper.getSteve();
    }
}
