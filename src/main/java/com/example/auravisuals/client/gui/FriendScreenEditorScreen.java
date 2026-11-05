package com.example.auravisuals.client.gui;

import com.example.auravisuals.module.render.FriendScreen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class FriendScreenEditorScreen extends Screen {
    private final Screen parent;
    private final FriendScreen module;
    private boolean dragging;
    private double grabX;
    private double grabY;

    public FriendScreenEditorScreen(Screen parent, FriendScreen module) {
        super(Text.of("FriendScreen — редактор"));
        this.parent = parent;
        this.module = module;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        context.fill(0, 0, this.width, this.height, 1426063360);
        this.module.renderPanels(context, true);
        String hint = "Перетащите панель мышью • Колесо — размер • Esc — готово";
        context.drawCenteredTextWithShadow(this.textRenderer, hint, this.width / 2, this.height - 18, -1);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int x = this.module.getPanelX(this.width);
        int y = this.module.getPanelY() - 14;
        int w = this.module.getPanelWidth();
        int h = Math.max(80, Math.round(w * 9.0F / 16.0F)) + 16;
        if (click.button() == 0 && click.x() >= x && click.x() <= x + w && click.y() >= y && click.y() <= y + h) {
            this.dragging = true;
            this.grabX = click.x() - x;
            this.grabY = click.y() - this.module.getPanelY();
            return true;
        } else {
            return super.mouseClicked(click, doubled);
        }
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (this.dragging) {
            this.module.setPanelPosition((int)(click.x() - this.grabX), (int)(click.y() - this.grabY));
            return true;
        } else {
            return super.mouseDragged(click, offsetX, offsetY);
        }
    }

    @Override
    public boolean mouseReleased(Click click) {
        this.dragging = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.module.resizePanel(verticalAmount * 10.0);
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == 256) {
            this.close();
            return true;
        } else {
            return super.keyPressed(input);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }
}
