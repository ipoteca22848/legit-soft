package com.anvil.client.mixin.performance;
import com.anvil.client.performance.PulseOptimization;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
 @ModifyVariable(method="runQueuedChunkUpdates",at=@At("STORE"),ordinal=1)
 private int anvil$chunkBudget(int value){var m=PulseOptimization.instance();return m!=null?m.c(value):value;}
}
