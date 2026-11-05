package com.example.auravisuals.mixin.client;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.misc.Xeaklt;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem.PlayResult;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundManager.class)
public class SoundManagerMixin {
    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At("HEAD"), cancellable = true, require = 0)
    private void onPlay(SoundInstance sound, CallbackInfoReturnable<PlayResult> cir) {
        Identifier critId = SoundEvents.ENTITY_PLAYER_ATTACK_CRIT.id();
        Xeaklt xeaklt;
        if (sound.getId().equals(critId)
            && (xeaklt = ModuleManager.INSTANCE.getModule(Xeaklt.class)) != null
            && xeaklt.isEnabled()
            && xeaklt.critSound.getValue()
            && (xeaklt.shouldReplaceCritSound() || xeaklt.wasCustomCritJustPlayed() || xeaklt.wasLocalAttackJustPerformed())) {
            cir.setReturnValue(PlayResult.NOT_STARTED);
            if (!xeaklt.wasCustomCritJustPlayed()) {
                xeaklt.onCrit();
            }
        }
    }
}
