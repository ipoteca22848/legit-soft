package com.example.auravisuals.module.render;

import com.example.auravisuals.client.gui.CrosshairEditorScreen;
import com.example.auravisuals.compat.Gui;
import com.example.auravisuals.compat.Gui$Color;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.ActionSetting;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.setting.StringSetting;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.awt.Color;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

public class Crosshair extends Module {
    public static final StringSetting patternData = new StringSetting("Pattern", "", () -> false);
    public static final NumberSetting pixelScale = new NumberSetting("Размер", 2.0, 0.5, 2.5, 0.25);
    public static final BooleanSetting customColor = new BooleanSetting("Кастомный цвет", false);
    public static final ColorSetting color = new ColorSetting("Цвет", new Color(255, 255, 255), () -> customColor.getValue());
    private final ActionSetting openEditor = new ActionSetting("Открыть редактор", this::openEditorScreen);
    private static final int GRID_SIZE = 15;
    private final boolean[][] pattern = new boolean[15][15];

    public Crosshair() {
        super("Crosshair", "Кастомный прицел", Category.VISUALS);
        this.addSettings(this.openEditor, pixelScale, patternData, customColor, color);
        this.loadPattern();
    }

    private void openEditorScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        client.setScreen(new CrosshairEditorScreen(client.currentScreen, this));
    }

    public void setPixel(int row, int col, boolean value) {
        if (row >= 0 && row < 15 && col >= 0 && col < 15) {
            this.pattern[row][col] = value;
        }
    }

    public boolean[][] getPattern() {
        return this.pattern;
    }

    public void clearPattern() {
        for (int row = 0; row < 15; row++) {
            for (int col = 0; col < 15; col++) {
                this.pattern[row][col] = false;
            }
        }
    }

    private void createDefaultPattern() {
        this.clearPattern();
        int center = 7;
        this.setPixel(center, center, true);
        this.setPixel(center - 1, center, true);
        this.setPixel(center + 1, center, true);
        this.setPixel(center, center - 1, true);
        this.setPixel(center, center + 1, true);
    }

    public void savePattern() {
        StringBuilder sb = new StringBuilder(225);

        for (int row = 0; row < 15; row++) {
            for (int col = 0; col < 15; col++) {
                sb.append((char)(this.pattern[row][col] ? '1' : '0'));
            }
        }

        patternData.setValue(sb.toString());
    }

    public void loadPattern() {
        String data = patternData.getValue();
        if (data.length() != 225) {
            this.createDefaultPattern();
            this.savePattern();
        } else {
            for (int row = 0; row < 15; row++) {
                for (int col = 0; col < 15; col++) {
                    this.pattern[row][col] = data.charAt(row * 15 + col) == '1';
                }
            }
        }
    }

    @Override
    public void onHudRender(DrawContext context) {
        if (this.mc.options.getPerspective().isFirstPerson()) {
            int activePixels = this.countActivePixels();
            if (this.mc.currentScreen == null && activePixels != 0) {
                float pixelSize = pixelScale.getFloatValue();
                float gridSize = 15.0F * pixelSize;
                float startX = (this.mc.getWindow().getScaledWidth() - gridSize) * 0.5F;
                float startY = (this.mc.getWindow().getScaledHeight() - gridSize) * 0.5F;
                float red = 1.0F;
                float green = 1.0F;
                float blue = 1.0F;
                float alpha = 1.0F;
                boolean invert = !customColor.getValue();
                if (customColor.getValue()) {
                    int packedColor = color.getValue();
                    red = (packedColor >> 16 & 0xFF) / 255.0F;
                    green = (packedColor >> 8 & 0xFF) / 255.0F;
                    blue = (packedColor & 0xFF) / 255.0F;
                    alpha = (packedColor >> 24 & 0xFF) / 255.0F;
                }

                float shadowInset = Math.max(0.75F, pixelSize * 0.35F);
                RenderPipeline pipeline = invert ? Gui.GUI_INVERT : Gui.GUI_NOCULL;
                int shadowColor = Gui.argb(0.0F, 0.0F, 0.0F, Math.min(0.55F, alpha * 0.55F));
                Gui$Color shadow = Gui.color(context, pipeline);

                for (int row = 0; row < 15; row++) {
                    for (int col = 0; col < 15; col++) {
                        if (this.pattern[row][col]) {
                            float x1 = startX + col * pixelSize;
                            float y1 = startY + row * pixelSize;
                            float x2 = x1 + pixelSize;
                            float y2 = y1 + pixelSize;
                            shadow.quad(
                                x1 - shadowInset,
                                y2 + shadowInset,
                                x2 + shadowInset,
                                y2 + shadowInset,
                                x2 + shadowInset,
                                y1 - shadowInset,
                                x1 - shadowInset,
                                y1 - shadowInset,
                                shadowColor
                            );
                        }
                    }
                }

                shadow.submit(context);
                int mainColor = Gui.argb(red, green, blue, alpha);
                Gui$Color main = Gui.color(context, pipeline);

                for (int row = 0; row < 15; row++) {
                    for (int col = 0; col < 15; col++) {
                        if (this.pattern[row][col]) {
                            float x1 = startX + col * pixelSize;
                            float y1 = startY + row * pixelSize;
                            float x2 = x1 + pixelSize;
                            float y2 = y1 + pixelSize;
                            main.quad(x1, y2, x2, y2, x2, y1, x1, y1, mainColor);
                        }
                    }
                }

                main.submit(context);
            }
        }
    }

    private int countActivePixels() {
        int count = 0;

        for (int row = 0; row < 15; row++) {
            for (int col = 0; col < 15; col++) {
                if (this.pattern[row][col]) {
                    count++;
                }
            }
        }

        return count;
    }
}
