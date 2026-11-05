package com.example.auravisuals.mixin.gui;

import com.example.auravisuals.compat.Inputs;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.misc.InventoryManager;
import com.example.auravisuals.module.player.AutoSwap;
import com.example.auravisuals.module.player.ItemScroller;
import com.example.auravisuals.module.render.Cooldowns;
import com.example.auravisuals.module.render.GuiCustomized;
import com.example.auravisuals.module.render.ItemHelper;
import com.example.auravisuals.module.render.PotionHighlight;
import com.example.auravisuals.module.render.ShulkerPreview;
import com.example.auravisuals.util.render.RenderUtil;
import com.example.auravisuals.util.render.gui.GifOption;
import com.example.auravisuals.util.render.gui.Gifs;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {
    @Shadow
    protected int x;
    @Shadow
    protected int y;
    @Shadow
    protected int backgroundWidth;
    @Shadow
    protected int backgroundHeight;
    @Shadow
    @Nullable
    protected Slot focusedSlot;
    @Shadow
    protected ScreenHandler handler;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, require = 0)
    private void auravisuals$hideAutoSwapInventoryRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if ((Object)this instanceof InventoryScreen) {
            AutoSwap autoSwap = ModuleManager.INSTANCE.getModule(AutoSwap.class);
            if (autoSwap != null && autoSwap.shouldHideAutoSwapInventoryScreen()) {
                ci.cancel();
            }
        }
    }

    @Inject(
        method = "render",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V", shift = Shift.AFTER),
        require = 0
    )
    private void auravisuals$drawCustomContainerBackground(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!((Object)this instanceof InventoryScreen) && GuiCustomized.allContainers.getValue()) {
            GuiCustomized module = ModuleManager.INSTANCE.getModule(GuiCustomized.class);
            GifOption current = Gifs.current();
            if (module != null && module.isEnabled() && current != null) {
                Identifier frame = current.frameAt((long)(System.currentTimeMillis() * GuiCustomized.speed.getValue()));
                if (frame != null) {
                    float opacity = GuiCustomized.opacity.getValue().floatValue();
                    RenderUtil.shaderColor(1.0F, 1.0F, 1.0F, opacity);
                    context.drawTexture(
                        RenderPipelines.GUI_TEXTURED,
                        frame,
                        this.x,
                        this.y,
                        0.0F,
                        0.0F,
                        this.backgroundWidth,
                        this.backgroundHeight,
                        current.frameW,
                        current.frameH,
                        current.frameW,
                        current.frameH
                    );
                    RenderUtil.shaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                }
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void onRender(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (ItemScroller.isDragging()) {
            if (!Inputs.hasShiftDown()) {
                ItemScroller.stopDrag();
            } else if (this.focusedSlot != null) {
                ItemScroller.onDragOver(this.focusedSlot, this.handler);
            }
        }

        ShulkerPreview shulkerPreview;
        if ((shulkerPreview = ModuleManager.INSTANCE.getModule(ShulkerPreview.class)) != null
            && shulkerPreview.isEnabled()
            && this.focusedSlot != null
            && this.focusedSlot.hasStack()
            && shulkerPreview.shouldPreview()) {
            ShulkerPreview.renderPreview(context, this.focusedSlot.getStack(), mouseX, mouseY);
        }

        if ((Object)this instanceof InventoryScreen) {
            InventoryManager.renderInventoryOverlay(context, mouseX, mouseY, this.x, this.y, this.backgroundWidth);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, require = 0)
    private void onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        AutoSwap autoSwap = ModuleManager.INSTANCE.getModule(AutoSwap.class);
        if (autoSwap != null && autoSwap.tryConsumeTriplePickClick((HandledScreen<?>)(Object)this, mouseX, mouseY, button)) {
            cir.setReturnValue(true);
        } else if ((Object)this instanceof InventoryScreen
            && InventoryManager.mouseClickedInventoryOverlay(mouseX, mouseY, button, this.x, this.y, this.backgroundWidth)) {
            cir.setReturnValue(true);
        } else {
            ItemScroller scroller;
            if (button == 0 && Inputs.hasShiftDown() && (scroller = ModuleManager.INSTANCE.getModule(ItemScroller.class)) != null && scroller.isEnabled()) {
                ItemScroller.startDrag(this.focusedSlot);
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true, require = 0)
    private void auravisuals$inventoryManagerScroll(
        double mouseX, double mouseY, double horizontalAmount, double verticalAmount, CallbackInfoReturnable<Boolean> cir
    ) {
        if ((Object)this instanceof InventoryScreen
            && InventoryManager.mouseScrolledInventoryOverlay(mouseX, mouseY, verticalAmount, this.x, this.y, this.backgroundWidth)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true, require = 0)
    private void onMouseReleased(Click click, CallbackInfoReturnable<Boolean> cir) {
        int button = click.button();
        if (button == 0 && ItemScroller.isDragging()) {
            ItemScroller.stopDrag();
        }
    }

    @Inject(method = "mouseDragged", at = @At("HEAD"), cancellable = true, require = 0)
    private void onMouseDragged(Click click, double deltaX, double deltaY, CallbackInfoReturnable<Boolean> cir) {
        int button = click.button();
        if (button == 0 && ItemScroller.isDragging() && Inputs.hasShiftDown() && this.focusedSlot != null) {
            ItemScroller.onDragOver(this.focusedSlot, this.handler);
        }
    }

    @Inject(method = "drawSlot", at = @At("HEAD"), require = 0)
    private void onDrawSlot(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        GuiCustomized guiCustom = ModuleManager.INSTANCE.getModule(GuiCustomized.class);
        if (guiCustom != null && guiCustom.isEnabled() && GuiCustomized.darkSlots.getValue()) {
            RenderUtil.drawRoundedRect(context, slot.x - 1.0F, slot.y - 1.0F, 18.0F, 18.0F, 2.5F, 1140850688);
        }

        int color;
        ItemStack stack;
        ItemHelper itemHelper;
        if ((itemHelper = ModuleManager.INSTANCE.getModule(ItemHelper.class)) != null
            && itemHelper.isEnabled()
            && !(stack = slot.getStack()).isEmpty()
            && (color = itemHelper.getColor(stack.getItem())) != -1) {
            float radius = itemHelper.isCircleShape() ? 4.0F : 0.0F;
            RenderUtil.drawRoundedRect(context, slot.x, slot.y, 16.0F, 16.0F, radius, color);
        }

        PotionHighlight.renderSlotHighlight(context, slot);
    }

    @Inject(method = "drawSlot", at = @At("TAIL"), require = 0)
    private void auravisuals$renderInventoryCooldown(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        Cooldowns.renderInventoryCooldown(context, slot.getStack(), slot.x, slot.y);
    }

    @Inject(method = "drawMouseoverTooltip", at = @At("HEAD"), cancellable = true, require = 0)
    private void onDrawMouseoverTooltip(DrawContext context, int x, int y, CallbackInfo ci) {
        if (this.focusedSlot != null && this.focusedSlot.hasStack()) {
            ItemStack stack = this.focusedSlot.getStack();
            ShulkerPreview module = ModuleManager.INSTANCE.getModule(ShulkerPreview.class);
            if (module != null && module.isEnabled() && module.shouldPreview() && ShulkerPreview.isPreviewableShulker(stack)) {
                ci.cancel();
            }
        }
    }
}
