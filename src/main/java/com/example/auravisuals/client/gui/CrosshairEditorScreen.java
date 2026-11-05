package com.example.auravisuals.client.gui;

import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.module.render.Crosshair;
import com.example.auravisuals.util.animation.AnimationUtil;
import com.example.auravisuals.util.render.RenderUtil;
import java.awt.Color;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class CrosshairEditorScreen extends Screen {
    private static final int GRID_SIZE = 15;
    private final Screen parent;
    private final Crosshair module;
    private int guiLeft;
    private int guiTop;
    private final int guiWidth = 280;
    private final int guiHeight = 306;
    private int gridX;
    private int gridY;
    private final int cellSize = 14;
    private float openingAnimation = 0.0F;
    private boolean closing = false;
    private boolean isMouseDown = false;
    private int currentMouseButton = -1;

    public CrosshairEditorScreen(Screen parent, Crosshair module) {
        super(Text.of("Редактор прицела"));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width - 280) / 2;
        this.guiTop = (this.height - 306) / 2;
        this.gridX = this.guiLeft + 35;
        this.gridY = this.guiTop + 38;
        this.openingAnimation = 0.0F;
        this.closing = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (this.closing) {
            this.openingAnimation = AnimationUtil.smoothRate(0.0F, this.openingAnimation, 0.75F, deltaTicks);
            if (this.openingAnimation < 0.05F) {
                this.client.setScreen(this.parent);
                return;
            }
        } else {
            this.openingAnimation = AnimationUtil.smoothRate(1.0F, this.openingAnimation, 0.75F, deltaTicks);
        }

        int alphaInt = Math.max(0, Math.min(255, (int)(255.0F * this.openingAnimation)));
        context.fill(0, 0, this.width, this.height, new Color(0, 0, 0, (int)(72.0F * this.openingAnimation)).getRGB());
        int bgCol = new Color(20, 20, 24, alphaInt).getRGB();
        RenderUtil.drawRoundedRect(context, this.guiLeft, this.guiTop, 280.0F, 306.0F, 14.0F, bgCol);
        int titleColor = new Color(255, 255, 255, alphaInt).getRGB();
        float titleScale = 0.38F;
        String titleStr = "Редактор прицела";
        float titleW = FontManager.getScaledTextWidth(titleStr, titleScale);
        FontManager.drawCustomTextScaled(context, titleStr, this.guiLeft + (280.0F - titleW) / 2.0F, this.guiTop + 4, titleColor, false, titleScale);
        int closeX = this.guiLeft + 280 - 18;
        int closeY = this.guiTop + 6;
        boolean closeHover = mouseX >= closeX - 2 && mouseX <= closeX + 14 && mouseY >= closeY + 2 && mouseY <= closeY + 18;
        int closeColor = closeHover ? new Color(255, 90, 90, alphaInt).getRGB() : new Color(170, 170, 170, alphaInt).getRGB();
        FontManager.drawCustomTextScaled(context, "x", closeX, closeY, closeColor, false, 0.48F);
        int hintColor = new Color(170, 170, 170, alphaInt).getRGB();
        float hintScale = 0.34F;
        String hintStr = "ЛКМ - добавить  |  ПКМ - убрать";
        float hintW = FontManager.getScaledTextWidth(hintStr, hintScale);
        FontManager.drawCustomTextScaled(context, hintStr, this.guiLeft + (280.0F - hintW) / 2.0F, this.guiTop + 22, hintColor, false, hintScale);
        this.renderGrid(context, mouseX, mouseY, alphaInt);
        int btnWidth = 76;
        int btnHeight = 22;
        int spacing = 10;
        int totalBtnsWidth = btnWidth * 3 + spacing * 2;
        int startX = this.guiLeft + (280 - totalBtnsWidth) / 2;
        int btnY = this.guiTop + 306 - 35;
        this.renderButton(context, "Сохранить", startX, btnY, btnWidth, btnHeight, mouseX, mouseY, alphaInt, new Color(40, 180, 80));
        this.renderButton(context, "Очистить", startX + btnWidth + spacing, btnY, btnWidth, btnHeight, mouseX, mouseY, alphaInt, new Color(180, 180, 40));
        this.renderButton(context, "Отмена", startX + (btnWidth + spacing) * 2, btnY, btnWidth, btnHeight, mouseX, mouseY, alphaInt, new Color(180, 60, 60));
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void renderGrid(DrawContext context, int mouseX, int mouseY, int alphaInt) {
        boolean[][] pattern = this.module.getPattern();

        for (int row = 0; row < 15; row++) {
            for (int col = 0; col < 15; col++) {
                int x = this.gridX + col * 14;
                int y = this.gridY + row * 14;
                boolean isCenter = row == 7 && col == 7;
                int cellColor = pattern[row][col]
                    ? new Color(255, 255, 255, alphaInt).getRGB()
                    : (isCenter ? new Color(70, 70, 80, alphaInt).getRGB() : new Color(30, 30, 35, alphaInt).getRGB());
                if (mouseX >= x && mouseX < x + 14 && mouseY >= y && mouseY < y + 14 && !pattern[row][col]) {
                    cellColor = new Color(50, 50, 60, alphaInt).getRGB();
                }

                RenderUtil.drawRoundedRect(context, x, y, 13.0F, 13.0F, 2.0F, cellColor);
            }
        }

        if (this.isMouseDown) {
            this.handleMouseInput(mouseX, mouseY, this.currentMouseButton);
        }
    }

    private void renderButton(DrawContext context, String text, int x, int y, int w, int h, int mouseX, int mouseY, int alphaInt, Color accent) {
        boolean hovered = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        int bgAlpha = hovered ? 200 : 120;
        int color = new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), (int)(bgAlpha * (alphaInt / 255.0F))).getRGB();
        RenderUtil.drawRoundedRect(context, x, y, w, h, 6.0F, color);
        int textColor = new Color(255, 255, 255, alphaInt).getRGB();
        float ts = 0.31F;
        float textWidth = FontManager.getScaledTextWidth(text, ts);
        float lineH = FontManager.getScaledTextHeight(ts);
        FontManager.drawCustomTextScaled(context, text, x + (w - textWidth) / 2.0F, y + (h - lineH) / 2.0F, textColor, false, ts);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        this.isMouseDown = true;
        this.currentMouseButton = button;
        int closeX = this.guiLeft + 280 - 18;
        int closeY = this.guiTop + 6;
        if (mouseX >= closeX - 2 && mouseX <= closeX + 14 && mouseY >= closeY + 2 && mouseY <= closeY + 18) {
            this.cancelAndClose();
            return true;
        } else if (this.handleMouseInput((int)mouseX, (int)mouseY, button)) {
            return true;
        } else {
            int btnWidth = 76;
            int btnHeight = 22;
            int spacing = 10;
            int totalBtnsWidth = btnWidth * 3 + spacing * 2;
            int startX = this.guiLeft + (280 - totalBtnsWidth) / 2;
            int btnY = this.guiTop + 306 - 35;
            if (mouseX >= startX && mouseX <= startX + btnWidth && mouseY >= btnY && mouseY <= btnY + btnHeight) {
                this.module.savePattern();
                this.closing = true;
                return true;
            } else if (mouseX >= startX + btnWidth + spacing && mouseX <= startX + btnWidth * 2 + spacing && mouseY >= btnY && mouseY <= btnY + btnHeight) {
                this.module.clearPattern();
                return true;
            } else if (mouseX >= startX + (btnWidth + spacing) * 2
                && mouseX <= startX + btnWidth * 3 + spacing * 2
                && mouseY >= btnY
                && mouseY <= btnY + btnHeight) {
                this.cancelAndClose();
                return true;
            } else {
                return super.mouseClicked(click, doubled);
            }
        }
    }

    @Override
    public boolean mouseReleased(Click click) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        this.isMouseDown = false;
        this.currentMouseButton = -1;
        return super.mouseReleased(click);
    }

    private boolean handleMouseInput(int mouseX, int mouseY, int button) {
        if (mouseX >= this.gridX && mouseX < this.gridX + 210 && mouseY >= this.gridY && mouseY < this.gridY + 210) {
            int col = (mouseX - this.gridX) / 14;
            int row = (mouseY - this.gridY) / 14;
            if (row >= 0 && row < 15 && col >= 0 && col < 15) {
                if (button == 0) {
                    this.module.setPixel(row, col, true);
                } else if (button == 1) {
                    this.module.setPixel(row, col, false);
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();
        if (keyCode == 256) {
            this.cancelAndClose();
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
        this.cancelAndClose();
    }

    private void cancelAndClose() {
        this.module.loadPattern();
        this.closing = true;
    }
}
