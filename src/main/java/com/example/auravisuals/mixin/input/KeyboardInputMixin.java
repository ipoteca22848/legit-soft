package com.example.auravisuals.mixin.input;

import com.example.auravisuals.mixin.accessor.InputAccessor;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.player.AutoSwap;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Vec2f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {
    @Shadow
    @Final
    private GameOptions settings;

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
    private void auravisuals$suppressMovementDuringAutoSwap(CallbackInfo ci) {
        AutoSwap autoSwap = ModuleManager.INSTANCE.getModule(AutoSwap.class);
        if (autoSwap != null && autoSwap.shouldSuppressPacketInventoryMovement()) {
            InputAccessor input = (InputAccessor)(Object)this;
            input.auravisuals$setPlayerInput(new PlayerInput(false, false, false, false, false, false, false));
            input.auravisuals$setMovementVector(Vec2f.ZERO);
            ci.cancel();
        }
    }
}
