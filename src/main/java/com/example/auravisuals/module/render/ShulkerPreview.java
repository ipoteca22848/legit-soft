package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BindSetting;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

public class ShulkerPreview extends Module {
    public static final BindSetting peekKey = new BindSetting("Кнопка просмотра", 340);
    private static final Identifier TEXTURE = Identifier.of("minecraft", "textures/gui/container/shulker_box.png");

    public ShulkerPreview() {
        super("ShulkerPreview", "Предпросмотр шалкеров", Category.VISUALS);
        this.addSettings(peekKey);
    }

    public boolean shouldPreview() {
        return peekKey.getValue() == 0 ? true : BindSetting.isPressed(this.mc.getWindow().getHandle(), peekKey.getValue());
    }

    public static boolean isPreviewableShulker(ItemStack stack) {
        return stack.getItem() instanceof BlockItem && ((BlockItem)stack.getItem()).getBlock() instanceof ShulkerBoxBlock
            ? stack.get(DataComponentTypes.CONTAINER) != null
            : false;
    }

    public static boolean renderPreview(DrawContext context, ItemStack stack, int x, int y) {
        if (!isPreviewableShulker(stack)) {
            return false;
        }

        ContainerComponent container = stack.get(DataComponentTypes.CONTAINER);
        if (container == null) {
            return false;
        }

        DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);
        container.copyTo(items);
        int width = 176;
        int height = 76;
        int startX = x + 10;
        int startY = y - height - 10;
        if (startY < 0) {
            startY = y + 10;
        }

        context.getMatrices().pushMatrix();

        try {
            context.getMatrices().translate(0.0F, 0.0F);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, startX, startY, 0.0F, 0.0F, 176, 71, 256, 256);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, startX, startY + 71, 0.0F, 160.0F, 176, 6, 256, 256);
            context.drawText(MinecraftClient.getInstance().textRenderer, stack.getName(), startX + 8, startY + 6, 4210752, false);
            int i = 0;

            for (ItemStack item : items) {
                int row = i / 9;
                int col = i % 9;
                int itemX = startX + 8 + col * 18;
                int itemY = startY + 18 + row * 18;
                context.drawItem(item, itemX, itemY);
                context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, item, itemX, itemY);
                i++;
            }
        } finally {
            context.getMatrices().popMatrix();
        }

        return true;
    }
}
