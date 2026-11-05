package com.bamber.client.mixin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.fog.FogRenderer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(GameRenderer.class)
public abstract class FrontendRenderMixin {
 @Shadow @Final private MinecraftClient client;
 @Shadow @Final GuiRenderState guiState;
 @Shadow @Final private GuiRenderer guiRenderer;
 @Shadow @Final private FogRenderer fogRenderer;
 @Inject(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V",shift=At.Shift.AFTER))
 private void bamber$render(RenderTickCounter counter,boolean tick,CallbackInfo ci){
  if(!com.bamber.client.BamberRuntime.isActive()||client.getOverlay()!=null)return;
  if(!(client.currentScreen instanceof rich.screens.clickgui.ClickGui gui))return;
  guiState.clear();
  DrawContext context=new DrawContext(client,guiState,(int)client.mouse.getScaledX(client.getWindow()),(int)client.mouse.getScaledY(client.getWindow()));
  rich.util.render.Render2D.beginOverlay();
  try{gui.renderOverlay(context,counter);}finally{rich.util.render.Render2D.endOverlay();}
  guiRenderer.render(fogRenderer.getFogBuffer(FogRenderer.FogType.NONE));
 }
}
