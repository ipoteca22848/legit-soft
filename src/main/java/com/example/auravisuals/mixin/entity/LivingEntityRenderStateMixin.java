package com.example.auravisuals.mixin.entity;

import com.example.auravisuals.util.render.RenderStateExtension;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
public class LivingEntityRenderStateMixin implements RenderStateExtension {
    @Unique
    private boolean isLocalPlayer;
    @Unique
    private boolean hiddenByKillEffect;

    @Override
    public void AuraVisuals$setIsLocalPlayer(boolean isLocalPlayer) {
        this.isLocalPlayer = isLocalPlayer;
    }

    @Override
    public boolean AuraVisuals$isLocalPlayer() {
        return this.isLocalPlayer;
    }

    @Override
    public void AuraVisuals$setHiddenByKillEffect(boolean hidden) {
        this.hiddenByKillEffect = hidden;
    }

    @Override
    public boolean AuraVisuals$isHiddenByKillEffect() {
        return this.hiddenByKillEffect;
    }
}
