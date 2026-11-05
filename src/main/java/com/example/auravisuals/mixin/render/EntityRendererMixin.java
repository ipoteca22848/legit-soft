package com.example.auravisuals.mixin.render;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.NameTag;
import com.example.auravisuals.module.render.NoRender;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity> {
    @Inject(method = "hasLabel", at = @At("HEAD"), cancellable = true, require = 0)
    private void onHasLabel(T entity, double squaredDistanceToCamera, CallbackInfoReturnable<Boolean> cir) {
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
}
