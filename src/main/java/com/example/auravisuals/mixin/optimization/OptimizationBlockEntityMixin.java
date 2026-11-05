package com.example.auravisuals.mixin.optimization;

import com.example.auravisuals.module.render.Optimization;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.render.block.entity.BlockEntityRenderManager;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer.CrumblingOverlayCommand;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockEntityRenderManager.class)
public abstract class OptimizationBlockEntityMixin {
    @Shadow
    private Vec3d cameraPos;

    @Inject(method = "getRenderState", at = @At("HEAD"), cancellable = true, require = 0)
    private <E extends BlockEntity, S extends BlockEntityRenderState> void aura$cullDistantBlockEntities(
        E blockEntity, float partialTick, CrumblingOverlayCommand crumblingOverlay, CallbackInfoReturnable<S> cir
    ) {
        if (this.cameraPos != null) {
            double distSq = blockEntity.getPos().getSquaredDistance(this.cameraPos);
            if (!Optimization.allowBlockEntity(distSq)) {
                cir.setReturnValue(null);
            }
        }
    }
}
