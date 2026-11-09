package com.anvil.client.mixin.performance;
import com.anvil.client.performance.PulseOptimization;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.world.attribute.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(SkyRendering.class)
public class SkyColorMixin {
 @WrapOperation(method="updateRenderState",at=@At(value="INVOKE",target="Lnet/minecraft/world/attribute/EnvironmentAttributeInterpolator;get(Lnet/minecraft/world/attribute/EnvironmentAttribute;F)Ljava/lang/Object;"))
 private Object anvil$cachedSky(EnvironmentAttributeInterpolator sampler,EnvironmentAttribute<?> attribute,float delta,Operation<Object> original){
  var module=PulseOptimization.instance();var client=MinecraftClient.getInstance();
  if(attribute!=EnvironmentAttributes.SKY_COLOR_VISUAL||module==null||client.gameRenderer==null)return original.call(sampler,attribute,delta);
  var pos=client.gameRenderer.getCamera().getCameraPos();Integer cached=module.a(pos);if(cached!=null)return cached;
  Object result=original.call(sampler,attribute,delta);if(result instanceof Integer color)module.a(pos,color);return result;
 }
}
