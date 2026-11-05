package com.example.auravisuals.client.gui;

import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.config.ConfigManager;
import com.example.auravisuals.config.HudPresetManager;
import com.example.auravisuals.service.GeminiHudService;
import com.example.auravisuals.util.animation.AnimationUtil;
import com.example.auravisuals.util.render.RenderUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionException;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public final class HudAiEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 430;
    private static final int PANEL_HEIGHT = 278;
    private final String presetName;
    private TextFieldWidget promptField;
    private int panelX;
    private int panelY;
    private float opening;
    private volatile String status = "Опишите, как оформить существующие элементы HUD.";
    private volatile String lastUserMessage = "";
    private volatile boolean waiting;

    public HudAiEditorScreen(String presetName) {
        super(Text.of("Редактор HUD"));
        this.presetName = presetName;
    }

    @Override
    protected void init() {
        super.init();
        int panelWidth = Math.min(430, this.width - 24);
        int panelHeight = Math.min(278, this.height - 24);
        this.panelX = (this.width - panelWidth) / 2;
        this.panelY = (this.height - panelHeight) / 2;
        this.promptField = new TextFieldWidget(
            this.textRenderer, this.panelX + 16, this.panelY + panelHeight - 38, panelWidth - 102, 22, Text.of("Запрос для редактора HUD")
        );
        this.promptField.setMaxLength(500);
        this.addDrawableChild(this.promptField);
        this.setInitialFocus(this.promptField);
        this.opening = 0.0F;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this.opening = AnimationUtil.smoothRate(1.0F, this.opening, 0.72F, deltaTicks);
        int panelWidth = Math.min(430, this.width - 24);
        int panelHeight = Math.min(278, this.height - 24);
        int alpha = Math.max(0, Math.min(255, Math.round(255.0F * this.opening)));
        context.fill(0, 0, this.width, this.height, new Color(0, 0, 0, Math.round(86.0F * this.opening)).getRGB());
        RenderUtil.drawRoundedRect(context, this.panelX, this.panelY, panelWidth, panelHeight, 13.0F, new Color(18, 18, 22, alpha).getRGB());
        RenderUtil.drawRoundedRect(context, this.panelX + 1, this.panelY + 1, panelWidth - 2, panelHeight - 2, 12.0F, new Color(25, 25, 30, alpha).getRGB());
        int titleColor = new Color(246, 246, 248, alpha).getRGB();
        int mutedColor = new Color(157, 157, 166, alpha).getRGB();
        FontManager.drawCustomTextScaled(context, "Редактор HUD", this.panelX + 16, this.panelY + 12, titleColor, false, 0.38F);
        FontManager.drawCustomTextScaled(context, "Режим: " + this.presetName, this.panelX + 16, this.panelY + 30, mutedColor, false, 0.31F);
        int bubbleWidth = panelWidth - 32;
        if (!this.lastUserMessage.isEmpty()) {
            RenderUtil.drawRoundedRect(context, this.panelX + 74, this.panelY + 56, bubbleWidth - 58, 48.0F, 9.0F, new Color(83, 62, 163, alpha).getRGB());
            drawWrapped(context, this.lastUserMessage, this.panelX + 85, this.panelY + 68, bubbleWidth - 80, 2, new Color(245, 245, 248, alpha).getRGB());
        }

        int assistantY = this.lastUserMessage.isEmpty() ? this.panelY + 57 : this.panelY + 113;
        int assistantHeight = this.lastUserMessage.isEmpty() ? 91 : 77;
        RenderUtil.drawRoundedRect(context, this.panelX + 16, assistantY, bubbleWidth, assistantHeight, 9.0F, new Color(36, 36, 43, alpha).getRGB());
        FontManager.drawCustomTextScaled(
            context, this.waiting ? "Gemini думает..." : "Помощник HUD", this.panelX + 27, assistantY + 11, titleColor, false, 0.31F
        );
        drawWrapped(context, this.status, this.panelX + 27, assistantY + 33, bubbleWidth - 22, this.lastUserMessage.isEmpty() ? 3 : 2, mutedColor);
        this.renderSendButton(context, mouseX, mouseY, this.panelX + panelWidth - 78, this.panelY + panelHeight - 38, 62, 22, alpha);
        this.renderCloseButton(context, mouseX, mouseY, this.panelX + panelWidth - 24, this.panelY + 10, alpha);
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void renderSendButton(DrawContext context, int mouseX, int mouseY, int x, int y, int buttonWidth, int buttonHeight, int alpha) {
        boolean hovered = inside(mouseX, mouseY, x, y, buttonWidth, buttonHeight);
        boolean enabled = !this.waiting;
        Color color = !enabled ? new Color(64, 64, 72, alpha) : (hovered ? new Color(111, 82, 219, alpha) : new Color(91, 68, 183, alpha));
        RenderUtil.drawRoundedRect(context, x, y, buttonWidth, buttonHeight, 6.0F, color.getRGB());
        String label = "Отправить";
        float scale = 0.29F;
        float textWidth = FontManager.getScaledTextWidth(label, scale);
        FontManager.drawCustomTextScaled(context, label, x + (buttonWidth - textWidth) / 2.0F, y + 5, new Color(255, 255, 255, alpha).getRGB(), false, scale);
    }

    private void renderCloseButton(DrawContext context, int mouseX, int mouseY, int x, int y, int alpha) {
        boolean hovered = inside(mouseX, mouseY, x - 2, y - 2, 18, 18);
        int color = hovered ? new Color(255, 100, 100, alpha).getRGB() : new Color(170, 170, 178, alpha).getRGB();
        FontManager.drawCustomTextScaled(context, "x", x, y, color, false, 0.38F);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int panelWidth = Math.min(430, this.width - 24);
        int panelHeight = Math.min(278, this.height - 24);
        if (inside(mouseX, mouseY, this.panelX + panelWidth - 26, this.panelY + 8, 20, 20)) {
            this.close();
            return true;
        } else if (inside(mouseX, mouseY, this.panelX + panelWidth - 78, this.panelY + panelHeight - 38, 62, 22)) {
            this.submitPrompt();
            return true;
        } else {
            return super.mouseClicked(click, doubled);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() != 257 && input.key() != 335) {
            return super.keyPressed(input);
        }

        this.submitPrompt();
        return true;
    }

    private void submitPrompt() {
        if (!this.waiting) {
            if (this.promptField != null && !this.promptField.getText().isBlank()) {
                String prompt = this.promptField.getText().strip();
                this.lastUserMessage = prompt;
                this.status = "Анализирую запрос и подбираю безопасные изменения...";
                this.waiting = true;
                this.promptField.setText("");
                this.setInitialFocus(this.promptField);
                GeminiHudService.request(prompt).whenComplete((result, throwable) -> {
                    if (this.client != null) {
                        this.client.execute(() -> {
                            this.waiting = false;
                            if (throwable == null) {
                                HudPresetManager.applyAiChanges(this.presetName, result.changes());
                                ConfigManager.INSTANCE.saveConfig("default");
                                this.status = result.message();
                            } else {
                                Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null ? throwable.getCause() : throwable;
                                this.status = cause.getMessage() == null ? "Не удалось связаться с Gemini." : cause.getMessage();
                            }
                        });
                    }
                });
            } else {
                this.status = "Сначала напишите, что нужно изменить в оформлении HUD.";
            }
        }
    }

    private static void drawWrapped(DrawContext context, String text, float x, float y, float maxWidth, int maxLines, int color) {
        List<String> lines = wrap(text, maxWidth, 0.285F, maxLines);

        for (int index = 0; index < lines.size(); index++) {
            FontManager.drawCustomTextScaled(context, lines.get(index), x, y + index * 15.0F, color, false, 0.285F);
        }
    }

    private static List<String> wrap(String text, float maxWidth, float scale, int maxLines) {
        List<String> result = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            String[] words = text.replace('\n', ' ').strip().split("\\s+");
            StringBuilder line = new StringBuilder();

            for (String word : words) {
                String candidate = line.isEmpty() ? word : line + " " + word;
                if (!line.isEmpty() && FontManager.getScaledTextWidth(candidate, scale) > maxWidth) {
                    result.add(line.toString());
                    line.setLength(0);
                    if (result.size() == maxLines) {
                        break;
                    }
                }

                if (!word.isEmpty()) {
                    if (!line.isEmpty()) {
                        line.append(' ');
                    }

                    line.append(word);
                }
            }

            if (result.size() < maxLines && !line.isEmpty()) {
                result.add(line.toString());
            }

            if (result.size() == maxLines) {
                int last = result.size() - 1;
                String value = result.get(last);

                while (!value.isEmpty() && FontManager.getScaledTextWidth(value + "…", scale) > maxWidth) {
                    value = value.substring(0, value.length() - 1);
                }

                if (words.length > 0) {
                    result.set(last, value.stripTrailing() + "…");
                }
            }

            return result;
        } else {
            return result;
        }
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
