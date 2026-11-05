package com.example.auravisuals.mixin.input;

import com.example.auravisuals.client.gui.IrcChatMessengerScreen;
import com.example.auravisuals.emotions.EmotionWheelManager;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.misc.Freelook;
import com.example.auravisuals.module.player.AutoSwap;
import com.example.auravisuals.module.render.FriendScreen;
import com.example.auravisuals.module.render.Karaoke;
import com.example.auravisuals.module.render.Zoom;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class MouseMixin {
    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true, require = 0)
    private void auravisuals$clickIrcToast(long window, MouseInput input, int action, CallbackInfo ci) {
        int button = input.button();
        AutoSwap autoSwap = ModuleManager.INSTANCE.getModule(AutoSwap.class);
        if (autoSwap != null && autoSwap.shouldConsumeTripleOverlayMouseButton(button, action)) {
            ci.cancel();
        } else {
            Karaoke worldLyrics = ModuleManager.INSTANCE.getModule(Karaoke.class);
            if (worldLyrics != null && worldLyrics.handleWorldMouseButton(button, action)) {
                ci.cancel();
            } else {
                FriendScreen friendScreen = ModuleManager.INSTANCE.getModule(FriendScreen.class);
                if (friendScreen != null && friendScreen.handleWorldMouseButton(button, action)) {
                    ci.cancel();
                } else if (button == 0 && action == 1) {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc != null && mc.getWindow() != null) {
                        double scaledX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
                        double scaledY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();

                        try {
                            if (IrcChatMessengerScreen.handleToastClick(scaledX, scaledY)) {
                                ci.cancel();
                            }
                        } catch (LinkageError var16) {
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "lockCursor", at = @At("HEAD"), cancellable = true, require = 0)
    private void auravisuals$cancelLockForTripleAutoSwap(CallbackInfo ci) {
        AutoSwap autoSwap = ModuleManager.INSTANCE.getModule(AutoSwap.class);
        if (autoSwap != null && autoSwap.shouldSuppressCursorLockWhileTripleOverlay()) {
            ci.cancel();
        }
    }

    @Inject(method = "unlockCursor", at = @At("HEAD"), cancellable = true, require = 0)
    private void auravisuals$keepCursorLockedForHiddenAutoSwap(CallbackInfo ci) {
        AutoSwap autoSwap = ModuleManager.INSTANCE.getModule(AutoSwap.class);
        if (autoSwap != null && autoSwap.shouldKeepCursorLockedForHiddenAutoSwap()) {
            ci.cancel();
        }
    }

    @Redirect(method = "updateMouse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"), require = 0)
    private void redirectChangeLookDirection(ClientPlayerEntity player, double cursorDeltaX, double cursorDeltaY) {
        if (!EmotionWheelManager.INSTANCE.isWheelOpen()) {
            Freelook freelook = ModuleManager.INSTANCE.getModule(Freelook.class);
            if (freelook != null && freelook.isEnabled() && Freelook.isActive) {
                float sensitivity = Freelook.speed.getValue().floatValue() * 0.5F;
                float newYaw = freelook.getCameraYaw() + (float)cursorDeltaX * sensitivity;
                float newPitch = freelook.getCameraPitch() + (float)cursorDeltaY * sensitivity;
                newPitch = Math.max(-90.0F, Math.min(90.0F, newPitch));
                freelook.setCameraYaw(newYaw);
                freelook.setCameraPitch(newPitch);
            } else {
                player.changeLookDirection(cursorDeltaX, cursorDeltaY);
            }
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true, require = 0)
    private void onMouseScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getWindow() != null) {
            Karaoke worldLyrics = ModuleManager.INSTANCE.getModule(Karaoke.class);
            if (worldLyrics != null && worldLyrics.handleWorldScroll(vertical)) {
                ci.cancel();
                return;
            }

            FriendScreen friendScreen = ModuleManager.INSTANCE.getModule(FriendScreen.class);
            if (friendScreen != null && friendScreen.handleWorldScroll(vertical)) {
                ci.cancel();
                return;
            }

            double scaledX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
            double scaledY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
            if (friendScreen != null && friendScreen.handleChatScroll(scaledX, scaledY, vertical)) {
                ci.cancel();
                return;
            }
        }

        Zoom zoom;
        if ((zoom = ModuleManager.INSTANCE.getModule(Zoom.class)) != null && zoom.isZooming() && vertical != 0.0) {
            zoom.adjustZoomByScroll(vertical);
            ci.cancel();
        }
    }
}
