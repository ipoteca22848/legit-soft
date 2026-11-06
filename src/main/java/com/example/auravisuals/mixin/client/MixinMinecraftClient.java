package com.example.auravisuals.mixin.client;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.misc.FakePlayer;
import com.example.auravisuals.module.misc.Xeaklt;
import com.example.auravisuals.module.render.FrameESP;
import com.example.auravisuals.module.render.HitCounter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {
    @Shadow
    public Entity targetedEntity;

    @Shadow
    public abstract boolean doAttack();

    @Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true, require = 0)
    private void auravisuals$frameEspOutline(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (FrameESP.shouldOutline(entity)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), require = 0)
    private void onDoAttack(CallbackInfoReturnable<Boolean> cir) {
        FakePlayer fakePlayerModule;
        if (this.targetedEntity != null
            && this.targetedEntity instanceof OtherClientPlayerEntity
            && (fakePlayerModule = ModuleManager.INSTANCE.getModule(FakePlayer.class)) != null
            && fakePlayerModule.isEnabled()
            && fakePlayerModule.getFakePlayer() == this.targetedEntity) {
            ((LivingEntity)(Object)this.targetedEntity).animateDamage(0.0F);
            Xeaklt clientSounds = ModuleManager.INSTANCE.getModule(Xeaklt.class);
            if (clientSounds != null && clientSounds.isEnabled()) {
                clientSounds.onAttackEntity(MinecraftClient.getInstance().player, this.targetedEntity);
            }
        }

        HitCounter hitCounter;
        if ((hitCounter = ModuleManager.INSTANCE.getModule(HitCounter.class)) != null) {
            hitCounter.onLocalAttackSwing();
        }
    }

    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true, require = 0)
    private void onGetWindowTitle(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("anvil visual");
    }
}
