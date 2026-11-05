package com.example.auravisuals.mixin.client;

import com.example.auravisuals.compat.ProjectionHolder;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.AspectRatio;
import com.example.auravisuals.module.render.CustomHand;
import com.example.auravisuals.module.render.FriendScreen;
import com.example.auravisuals.module.render.NoRender;
import com.example.auravisuals.module.render.ViewModel;
import com.example.auravisuals.module.render.Zoom;
import com.example.auravisuals.util.render.HandFxPipeline;
import com.example.auravisuals.util.render.HandsFlamePipeline;
import com.example.auravisuals.util.render.MirageGlassPipeline;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.BufferBuilderStorage;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.command.RenderDispatcher;
import net.minecraft.client.render.fog.FogRenderer;
import net.minecraft.client.render.fog.FogRenderer.FogType;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    @Final
    private GuiRenderer guiRenderer;
    @Shadow
    @Final
    private GuiRenderState guiState;
    @Shadow
    @Final
    private FogRenderer fogRenderer;
    @Shadow
    @Final
    private RenderDispatcher entityRenderDispatcher;
    @Shadow
    @Final
    private BufferBuilderStorage buffers;

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void auravisuals$beginStableGlassFrame(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        MirageGlassPipeline.beginRenderFrame();
    }

    @Inject(method = "renderWorld", at = @At("RETURN"), require = 0)
    private void auravisuals$captureFriendScreenWorldView(RenderTickCounter tickCounter, CallbackInfo ci) {
        try {
            FriendScreen friendScreen = ModuleManager.INSTANCE.getModule(FriendScreen.class);
            if (friendScreen != null && friendScreen.isEnabled()) {
                friendScreen.captureWorldView();
            }
        } catch (Throwable var4) {
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;incrementFrame()V", shift = Shift.AFTER), require = 0)
    private void auravisuals$captureFriendScreenAfterGuiFlush(RenderTickCounter tickCounter, boolean tick, CallbackInfo ci) {
        try {
            FriendScreen friendScreen = ModuleManager.INSTANCE.getModule(FriendScreen.class);
            if (friendScreen == null || !friendScreen.isEnabled()) {
                return;
            }

            this.guiState.clear();
            MinecraftClient client = MinecraftClient.getInstance();
            DrawContext friendContext = new DrawContext(
                client, this.guiState, (int)client.mouse.getScaledX(client.getWindow()), (int)client.mouse.getScaledY(client.getWindow())
            );
            friendScreen.renderAfterVanillaHud(friendContext);
            this.guiRenderer.render(this.fogRenderer.getFogBuffer(FogType.NONE));
            this.guiRenderer.incrementFrame();
            friendScreen.captureHudView();
        } catch (Throwable var7) {
        }
    }

    @Inject(method = "renderWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw()V", shift = Shift.AFTER))
    private void auravisuals$afterFirstPersonFlush(RenderTickCounter tickCounter, CallbackInfo ci) {
        HandsFlamePipeline.captureAfterHands();
        HandsFlamePipeline.renderReadyHandsFlame();
        HandFxPipeline.renderPendingCustomHandEffects();
    }

    @Inject(
        method = "renderWorld",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/GameRenderer;renderHand(FZLorg/joml/Matrix4f;)V", shift = Shift.AFTER),
        require = 0
    )
    private void auravisuals$captureIsolatedFirstPersonItems(RenderTickCounter tickCounter, CallbackInfo ci) {
        if (HandFxPipeline.isCaptureArmed()) {
            this.entityRenderDispatcher.render();
            this.buffers.getEntityVertexConsumers().draw();
            HandFxPipeline.captureAfter();
        }
    }

    @Inject(method = "getProjectionMatrix", at = @At("RETURN"), require = 0)
    private void auravisuals$captureProjection(float f, CallbackInfoReturnable<Matrix4f> cir) {
        try {
            Matrix4f m = (Matrix4f)cir.getReturnValue();
            if (m != null) {
                ProjectionHolder.lastProjectionMatrix = new Matrix4f(m);
            }
        } catch (Throwable var4) {
        }
    }

    @Inject(method = "renderHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;draw()V", shift = Shift.AFTER))
    private void auravisuals$prepareHandCaptures(float tickProgress, boolean sleeping, Matrix4f positionMatrix, CallbackInfo ci) {
        CustomHand module = ModuleManager.INSTANCE.getModule(CustomHand.class);
        boolean customHandEffect = module != null && module.isEnabled() && (CustomHand.blur.getValue() || CustomHand.itemOutline.getValue());
        if (customHandEffect || ViewModel.isEditorSessionActive()) {
            HandFxPipeline.captureBefore();
        }

        if (module != null && module.isEnabled() && module.flameActive()) {
            module.syncFlame();
            HandsFlamePipeline.captureBeforeHandRender();
            HandsFlamePipeline.renderCapturedHandsFlame();
        }
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true, require = 0)
    private void onGetFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        try {
            if (ModuleManager.INSTANCE == null) {
                return;
            }

            Zoom zoom = ModuleManager.INSTANCE.getModule(Zoom.class);
            if (zoom == null || !zoom.isEnabled()) {
                return;
            }

            float vanilla = (Float)cir.getReturnValue();
            float z = zoom.getSmoothedFov(vanilla);
            if (z > 0.0F && z < 180.0F) {
                cir.setReturnValue(z);
            }
        } catch (Throwable var8) {
        }
    }

    @Inject(method = "showFloatingItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo ci) {
        NoRender noRender;
        if (ModuleManager.INSTANCE != null
            && (noRender = ModuleManager.INSTANCE.getModule(NoRender.class)) != null
            && noRender.isEnabled()
            && NoRender.totemAnimation.getValue()
            && floatingItem.getItem() == Items.TOTEM_OF_UNDYING) {
            ci.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true, require = 0)
    private void onBobView(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        try {
            NoRender noRender;
            if (ModuleManager.INSTANCE != null
                && (noRender = ModuleManager.INSTANCE.getModule(NoRender.class)) != null
                && noRender.isEnabled()
                && NoRender.hurtCam.getValue()) {
                ci.cancel();
            }
        } catch (Throwable var5) {
        }
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true, require = 0)
    private void onTiltViewWhenHurt(MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        try {
            if (ModuleManager.INSTANCE != null) {
                float ratio;
                if (ModuleManager.INSTANCE.getModule(AspectRatio.class) != null
                    && ModuleManager.INSTANCE.getModule(AspectRatio.class).isEnabled()
                    && (ratio = AspectRatio.getAspectRatio()) != -1.0F) {
                    float currentRatio = (float)MinecraftClient.getInstance().getWindow().getFramebufferWidth()
                        / MinecraftClient.getInstance().getWindow().getFramebufferHeight();
                    float scale = currentRatio / ratio;
                    matrices.scale(scale, 1.0F, 1.0F);
                }

                NoRender noRender;
                if ((noRender = ModuleManager.INSTANCE.getModule(NoRender.class)) != null && noRender.isEnabled() && NoRender.hurtCam.getValue()) {
                    ci.cancel();
                }
            }
        } catch (Throwable var8) {
        }
    }
}
