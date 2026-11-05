package com.example.auravisuals.mixin.gui;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.GuiCustomized;
import com.example.auravisuals.util.render.RenderUtil;
import com.example.auravisuals.util.render.gui.GifOption;
import com.example.auravisuals.util.render.gui.Gifs;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin extends HandledScreen<PlayerScreenHandler> {
    private static final Identifier OVERLAY = Identifier.of("auravisuals", "textures/gui/inventory_overlay.png");

    public InventoryScreenMixin(PlayerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Inject(
        method = "drawBackground",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIII)V",
            shift = Shift.AFTER
        ),
        require = 0
    )
    private void auravisuals$onDrawBackground(DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        GuiCustomized mod = ModuleManager.INSTANCE.getModule(GuiCustomized.class);
        if (mod != null && mod.isEnabled()) {
            GifOption cur = Gifs.current();
            if (cur != null) {
                long animationTime = (long)(System.currentTimeMillis() * GuiCustomized.speed.getValue());
                Identifier frame = cur.frameAt(animationTime);
                if (frame != null) {
                    int x = this.x;
                    int y = this.y;
                    float opacity = GuiCustomized.opacity.getValue().floatValue();
                    RenderUtil.shaderColor(1.0F, 1.0F, 1.0F, opacity);
                    context.drawTexture(RenderPipelines.GUI_TEXTURED, frame, x, y, 0.0F, 0.0F, 176, 166, cur.frameW, cur.frameH, cur.frameW, cur.frameH);
                    RenderUtil.shaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    context.drawTexture(RenderPipelines.GUI_TEXTURED, OVERLAY, x, y, 0.0F, 0.0F, 176, 166, 176, 166);
                }
            }
        }
    }
}
