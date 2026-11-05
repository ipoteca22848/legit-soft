package com.example.auravisuals.mixin.render;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.misc.Globals;
import com.example.auravisuals.module.render.CustomModels;
import com.example.auravisuals.module.render.FrameESP;
import com.example.auravisuals.module.render.HitColor;
import com.example.auravisuals.module.render.KillEffect;
import com.example.auravisuals.module.render.NameTag;
import com.example.auravisuals.module.render.NoRender;
import com.example.auravisuals.module.render.SeeInvisible;
import com.example.auravisuals.util.registry.GlobalsIconText;
import com.example.auravisuals.util.registry.GlobalsRegistry;
import com.example.auravisuals.util.render.HitColorRenderLayers;
import com.example.auravisuals.util.render.RenderStateExtension;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
    @Shadow
    public abstract Identifier getTexture(LivingEntityRenderState var1);

    @Inject(method = "getOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private static void onGetOverlay(LivingEntityRenderState state, float whiteOverlayProgress, CallbackInfoReturnable<Integer> cir) {
        if (ModuleManager.INSTANCE != null) {
            HitColor hitColor = ModuleManager.INSTANCE.getModule(HitColor.class);
            if (hitColor != null && hitColor.isEnabled() && state.hurt) {
                cir.setReturnValue(OverlayTexture.DEFAULT_UV);
            }
        }
    }

    @Inject(method = "getMixColor", at = @At("HEAD"), cancellable = true, require = 0)
    private void onGetMixColor(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
        if (ModuleManager.INSTANCE != null) {
            HitColor hitColor = ModuleManager.INSTANCE.getModule(HitColor.class);
            if (hitColor != null && hitColor.isEnabled() && state.hurt) {
                cir.setReturnValue(hitColor.getColorValue());
            }
        }
    }

    @Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true, require = 0)
    private void onGetRenderLayer(
        LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline, CallbackInfoReturnable<RenderLayer> cir
    ) {
        RenderStateExtension extension;
        if (state instanceof RenderStateExtension && (extension = (RenderStateExtension)state).AuraVisuals$isHiddenByKillEffect()) {
            cir.setReturnValue(null);
        } else if (CustomModels.shouldReplaceState(state)) {
            cir.setReturnValue(null);
        } else {
            HitColor hitColor;
            if (ModuleManager.INSTANCE != null
                && state.hurt
                && showBody
                && (hitColor = ModuleManager.INSTANCE.getModule(HitColor.class)) != null
                && hitColor.isEnabled()) {
                Identifier texture = this.getTexture(state);
                cir.setReturnValue(HitColor.isFullPaintMode() ? HitColorRenderLayers.full(texture) : HitColorRenderLayers.tint(texture));
            }
        }
    }

    @Inject(method = "shouldRenderFeatures", at = @At("HEAD"), cancellable = true, require = 0)
    private void auravisuals$hideKillEffectFeatures(LivingEntityRenderState state, CallbackInfoReturnable<Boolean> cir) {
        RenderStateExtension extension;
        if (state instanceof RenderStateExtension && (extension = (RenderStateExtension)state).AuraVisuals$isHiddenByKillEffect()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;D)Z", at = @At("HEAD"), cancellable = true, require = 0)
    private void onHasLabel(LivingEntity entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
        if (ModuleManager.INSTANCE != null) {
            NoRender noRender = ModuleManager.INSTANCE.getModule(NoRender.class);
            if (noRender != null && noRender.isEnabled() && NoRender.nameTags.getValue()) {
                cir.setReturnValue(false);
            } else {
                MinecraftClient mc = MinecraftClient.getInstance();
                NameTag nameTag;
                if (entity == mc.player
                    && (nameTag = ModuleManager.INSTANCE.getModule(NameTag.class)) != null
                    && nameTag.isEnabled()
                    && !mc.options.getPerspective().isFirstPerson()) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "updateRenderState", at = @At("RETURN"), require = 0)
    private void onUpdateRenderState(LivingEntity entity, LivingEntityRenderState state, float tickDelta, CallbackInfo ci) {
        if (FrameESP.shouldOutline(entity)) {
            state.outlineColor = FrameESP.outlineColor(entity);
        } else if (FrameESP.manages(entity)) {
            state.outlineColor = 0;
        }

        PlayerEntity player;
        if (entity instanceof PlayerEntity && SeeInvisible.shouldReveal(player = (PlayerEntity)entity)) {
            state.invisibleToPlayer = false;
        }

        if (entity instanceof PlayerEntity playerx
            && Globals.shouldShowIcon()
            && state.displayName != null
            && (
                playerx.getGameProfile() != null && GlobalsRegistry.isGlobalsPlayer(playerx.getGameProfile().name())
                    || playerx.getName() != null && GlobalsRegistry.isGlobalsPlayer(playerx.getName().getString())
            )) {
            state.displayName = GlobalsIconText.prefix(state.displayName);
        }

        if (state instanceof RenderStateExtension) {
            boolean isLocal = entity == MinecraftClient.getInstance().player;
            RenderStateExtension extension = (RenderStateExtension)state;
            extension.AuraVisuals$setIsLocalPlayer(isLocal);
            boolean hiddenByKillEffect = KillEffect.shouldHideBody(entity);
            extension.AuraVisuals$setHiddenByKillEffect(hiddenByKillEffect);
            if (hiddenByKillEffect) {
                state.invisible = true;
                state.invisibleToPlayer = true;
                state.outlineColor = 0;
                state.shadowRadius = 0.0F;
            }
        }
    }
}
