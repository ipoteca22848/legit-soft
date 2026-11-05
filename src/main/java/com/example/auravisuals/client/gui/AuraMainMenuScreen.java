package com.example.auravisuals.client.gui;

import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.client.gui.font.icon.IconTtfBridge;
import com.example.auravisuals.util.render.RenderUtil;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.resource.Resource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class AuraMainMenuScreen extends Screen {
    static final Identifier BACKGROUND = Identifier.of("auravisuals", "textures/mainmenu/background.png");
    static final Identifier SINGLEPLAYER_ICON = Identifier.of("auravisuals", "textures/mainmenu/player_menu.png");
    static final Identifier MULTIPLAYER_ICON = Identifier.of("auravisuals", "textures/mainmenu/multiplayer_menu.png");
    static final Identifier ACCOUNTS_ICON = Identifier.of("auravisuals", "textures/accounts.png");
    static final Identifier SETTINGS_ICON = Identifier.of("auravisuals", "textures/mainmenu/setting_minecraft.png");
    static final Identifier EXIT_ICON = Identifier.of("auravisuals", "textures/mainmenu/exit_menu_button.png");
    static final Identifier UPDATE_FILE = Identifier.of("auravisuals", "updates/update.txt");
    static final int BACKGROUND_W = 1672;
    static final int BACKGROUND_H = 941;
    static final int RED = -1824962;
    static final float FIXED_MENU_GUI_SCALE = 1.8F;
    final List<AuraMainMenuScreen$MenuButton> buttons = new ArrayList<>();
    final List<String> updateLines = new ArrayList<>();
    final Map<Integer, Float> lineReveal = new HashMap<>();
    String updateVersion = "v-2.1.1";
    String updateDate = "01.09.2026";
    float updateScroll;
    float updateTargetScroll;
    float updateScrollDragOffset;
    long lastFrameTime;
    boolean draggingUpdateScroll;
    float menuRenderScale = 1.0F;
    int menuWidth;
    int menuHeight;

    public AuraMainMenuScreen() {
        super(Text.literal("bamber client"));
    }

    @Override
    protected void init() {
        this.updateMenuMetrics();
        this.rebuildButtons();
        this.loadUpdateText();
    }

    void rebuildButtons() {
        this.buttons.clear();
        int button = 46;
        int gap = 11;
        int x = this.menuWidth - button - 42;
        int startY = this.menuHeight / 2 - (button * 5 + gap * 4) / 2;
        this.buttons
            .add(
                new AuraMainMenuScreen$MenuButton(
                    "Singleplayer", SINGLEPLAYER_ICON, '\ue101', false, x, startY, button, () -> this.client.setScreen(new SelectWorldScreen(this))
                )
            );
        this.buttons
            .add(
                new AuraMainMenuScreen$MenuButton(
                    "Multiplayer",
                    MULTIPLAYER_ICON,
                    '\ue102',
                    false,
                    x,
                    startY + button + gap,
                    button,
                    () -> this.client.setScreen(new MultiplayerScreen(this))
                )
            );
        this.buttons
            .add(
                new AuraMainMenuScreen$MenuButton(
                    "Accounts", ACCOUNTS_ICON, '-', false, x, startY + (button + gap) * 2, button, () -> this.client.setScreen(new AuraAccountsScreen(this))
                )
            );
        this.buttons
            .add(
                new AuraMainMenuScreen$MenuButton(
                    "Settings",
                    SETTINGS_ICON,
                    '\ue103',
                    false,
                    x,
                    startY + (button + gap) * 3,
                    button,
                    () -> this.client.setScreen(new OptionsScreen(this, this.client.options))
                )
            );
        this.buttons
            .add(
                new AuraMainMenuScreen$MenuButton("Exit", EXIT_ICON, '\ue104', false, x, startY + (button + gap) * 4, button, () -> this.client.scheduleStop())
            );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        float dt = this.frameDelta();
        this.renderMenuBackground(context);
        context.fill(0, 0, this.width, this.height, 1895825408);
        context.drawCenteredTextWithShadow(this.textRenderer, "bamber client", this.width / 2, 14, 0xFFFFFFFF);
        int previousMenuWidth = this.menuWidth;
        int previousMenuHeight = this.menuHeight;
        this.updateMenuMetrics();
        if (this.menuWidth != previousMenuWidth || this.menuHeight != previousMenuHeight) {
            this.rebuildButtons();
        }

        int menuMouseX = Math.round(mouseX / this.menuRenderScale);
        int menuMouseY = Math.round(mouseY / this.menuRenderScale);
        context.getMatrices().pushMatrix();

        try {
            context.getMatrices().scale(this.menuRenderScale, this.menuRenderScale);
            this.renderUpdatePanel(context, menuMouseX, menuMouseY, dt);
            this.renderButtons(context, menuMouseX, menuMouseY, dt);
        } finally {
            context.getMatrices().popMatrix();
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        double menuMouseX = mouseX / this.menuRenderScale;
        double menuMouseY = mouseY / this.menuRenderScale;
        if (button == 0) {
            if (this.isOnUpdateScrollbar(menuMouseX, menuMouseY)) {
                this.draggingUpdateScroll = true;
                this.updateScrollDragOffset = this.getScrollbarDragOffset((float)menuMouseY);
                this.dragUpdateScrollbar(menuMouseY);
                return true;
            }

            for (AuraMainMenuScreen$MenuButton menuButton : this.buttons) {
                if (menuButton.contains(menuMouseX, menuMouseY)) {
                    menuButton.action.run();
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (this.draggingUpdateScroll) {
            this.dragUpdateScrollbar(mouseY / this.menuRenderScale);
            return true;
        } else {
            return super.mouseDragged(click, offsetX, offsetY);
        }
    }

    @Override
    public boolean mouseReleased(Click click) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 0 && this.draggingUpdateScroll) {
            this.draggingUpdateScroll = false;
            return true;
        } else {
            return super.mouseReleased(click);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double menuMouseX = mouseX / this.menuRenderScale;
        double menuMouseY = mouseY / this.menuRenderScale;
        int x = 18;
        int y = this.updatePanelY();
        int w = this.updatePanelWidth();
        int h = this.updatePanelHeight();
        float max;
        if (menuMouseX >= x && menuMouseX <= x + w && menuMouseY >= y && menuMouseY <= y + h && (max = this.updateMaxScroll(h)) > 0.0F) {
            this.updateTargetScroll = MathHelper.clamp(this.updateTargetScroll - (float)verticalAmount * 34.0F, 0.0F, max);
            return true;
        } else {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    void renderMenuBackground(DrawContext context) {
        float screenRatio = (float)this.width / Math.max(1, this.height);
        float textureRatio = 1.7768332F;
        float drawW = this.width;
        float drawH = this.height;
        if (screenRatio > textureRatio) {
            drawH = this.width / textureRatio;
        } else {
            drawW = this.height * textureRatio;
        }

        float x = (this.width - drawW) * 0.5F;
        float y = (this.height - drawH) * 0.5F;
        RenderUtil.drawTexture(context, BACKGROUND, x, y, drawW, drawH, 0.0F, 0.0F, 1672.0F, 941.0F, 1672.0F, 941.0F);
    }

    void renderButtons(DrawContext context, int mouseX, int mouseY, float dt) {
        for (AuraMainMenuScreen$MenuButton button : this.buttons) {
            boolean hovered = button.contains(mouseX, mouseY);
            button.anim = this.approach(button.anim, hovered ? 1.0F : 0.0F, dt, 24.0F);
            button.separatorAnim = this.approach(button.separatorAnim, hovered ? 1.0F : 0.0F, dt, 48.0F);
            float textScale = 0.36F;
            float labelW = FontManager.getScaledTextWidth(button.label, textScale);
            float sidePadding = 10.5F;
            float expandW = button.size + (labelW + sidePadding * 2.0F) * button.anim;
            float right = button.x + button.size;
            float drawX = right - expandW;
            int base = new Color(8, 8, 12, 190).getRGB();
            float radius = 10.0F;
            this.drawSoftFrame(context, drawX, button.y, expandW, button.size, radius, base, this.withAlpha(-1824962, Math.round(150.0F + 45.0F * button.anim)));
            if (button.separatorAnim > 0.02F) {
                float separatorX = right - button.size;
                int separatorAlpha = MathHelper.clamp(Math.round(button.separatorAnim * 100.0F), 0, 100);
                float separatorH = (button.size - 3.0F) * button.separatorAnim;
                RenderUtil.drawRoundedRect(
                    context,
                    separatorX - 0.65F,
                    button.y + (button.size - separatorH) * 0.5F,
                    1.3F,
                    separatorH,
                    0.65F,
                    this.withAlpha(-1824962, separatorAlpha + 55)
                );
            }

            float iconSize = 23.0F;
            float iconX = right - button.size + (button.size - iconSize) * 0.5F;
            float iconY = button.y + (button.size - iconSize) * 0.5F;
            this.drawMenuIcon(context, button, iconX, iconY, iconSize);
            if (button.anim > 0.02F) {
                context.enableScissor((int)drawX, button.y, (int)right, button.y + button.size);

                try {
                    int alpha = MathHelper.clamp(Math.round(button.anim * 255.0F), 0, 255);
                    float textY = button.y + (button.size - FontManager.getScaledTextHeight(textScale)) * 0.5F + 1.0F;
                    FontManager.drawCustomTextScaled(
                        context, button.label, right - button.size - labelW - sidePadding, textY, alpha << 24 | 16777215, false, textScale
                    );
                } finally {
                    context.disableScissor();
                }
            }
        }
    }

    void renderUpdatePanel(DrawContext context, int mouseX, int mouseY, float dt) {
        int x = 18;
        int y = this.updatePanelY();
        int w = this.updatePanelWidth();
        int h = this.updatePanelHeight();
        float radius = 10.0F;
        this.updateTargetScroll = MathHelper.clamp(this.updateTargetScroll, 0.0F, this.updateMaxScroll(h));
        this.updateScroll = this.approach(this.updateScroll, this.updateTargetScroll, dt, 13.0F);
        RenderUtil.drawRoundedBlur(context, x, y, w, h, radius, 4);
        this.drawSoftFrame(context, x, y, w, h, radius, new Color(7, 7, 11, 178).getRGB(), this.withAlpha(-1824962, 155));
        FontManager.drawCustomTextScaled(context, "Latest Update", x + 14, y + 13, -1, false, 0.31F);
        FontManager.drawCustomTextScaled(context, this.updateVersion, x + 14, y + 35, this.withAlpha(-1824962, 255), false, 0.25F);
        FontManager.drawCustomTextScaled(
            context, this.updateDate, x + w - FontManager.getScaledTextWidth(this.updateDate, 0.23F) - 14.0F, y + 36, -1191182337, false, 0.23F
        );
        FontManager.drawCustomTextScaled(
            context, "bamber client", x + w - FontManager.getScaledTextWidth("bamber client", 0.23F) - 14.0F, y + 49, -1191182337, false, 0.23F
        );
        RenderUtil.drawRoundedRect(context, x + 1.15F, y + 55.0F, w - 2.3F, 1.15F, 0.6F, this.withAlpha(-1824962, 155));
        context.enableScissor(x + 10, y + 60, x + w - 12, y + h - 10);

        try {
            int contentX = x + 14;
            int maxTextW = w - 34;
            float lineY = y + 64 - this.updateScroll;

            for (int i = 0; i < this.updateLines.size(); i++) {
                String line = this.updateLines.get(i);
                if (lineY > y + 44 && lineY < y + h - 4) {
                    this.renderUpdateLine(context, line, i, contentX, lineY, maxTextW, mouseX, mouseY, dt);
                }

                lineY += line.isEmpty() ? 8.0F : 15.0F;
            }
        } finally {
            context.disableScissor();
        }

        this.renderUpdateScrollbar(context, x, y, w, h);
    }

    void renderUpdateScrollbar(DrawContext context, int x, int y, int w, int h) {
        float max = this.updateMaxScroll(h);
        if (!(max <= 1.0F)) {
            float trackY = y + 62.0F;
            float trackH = h - 76.0F;
            float thumbH = Math.max(24.0F, trackH * Math.min(1.0F, trackH / (trackH + max)));
            float thumbY = trackY + (trackH - thumbH) * (this.updateScroll / max);
            RenderUtil.drawRoundedRect(context, x + w - 9, trackY, 5.0F, trackH, 2.5F, 587202559);
            RenderUtil.drawRoundedRect(context, x + w - 9, thumbY, 5.0F, thumbH, 2.5F, this.withAlpha(-1824962, this.draggingUpdateScroll ? 245 : 210));
        }
    }

    void drawMenuIcon(DrawContext context, AuraMainMenuScreen$MenuButton button, float x, float y, float size) {
        RenderUtil.shaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (button.exitPng) {
            RenderUtil.drawGuiTexture(context, button.icon, x, y, size, size, false);
        } else if (!IconTtfBridge.tryDrawIconsFontChar(context, button.iconChar, x, y, size, -1)) {
            RenderUtil.drawGuiIconWithFallback(context, button.icon, x, y, size, size, true);
        }

        RenderUtil.shaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    void drawSoftFrame(DrawContext context, float x, float y, float w, float h, float radius, int fill, int border) {
        RenderUtil.drawRoundedRect(context, x, y, w, h, radius, border);
        RenderUtil.drawRoundedRect(context, x + 1.15F, y + 1.15F, w - 2.3F, h - 2.3F, Math.max(0.0F, radius - 1.15F), fill);
    }

    void renderUpdateLine(DrawContext context, String line, int index, int x, float y, int maxW, int mouseX, int mouseY, float dt) {
        float scale = 0.24F;
        float textW = FontManager.getScaledTextWidth(line, scale);
        boolean historyDate = line.matches("\\d{2}\\.\\d{2}\\.\\d{4}");
        boolean hovered = mouseX >= x && mouseX <= x + maxW && mouseY >= y - 2.0F && mouseY <= y + 13.0F;
        float target = hovered && textW > maxW ? 1.0F : 0.0F;
        float reveal = this.approach(this.lineReveal.getOrDefault(index, 0.0F), target, dt, 5.0F);
        this.lineReveal.put(index, reveal);
        float offset = 0.0F;
        if (textW > maxW) {
            offset = (textW - maxW + 18.0F) * reveal;
        }

        int color = this.isUpdateHeader(line) ? -1 : -654311425;
        float drawX = historyDate ? x + (maxW - textW) * 0.5F : x - offset;
        FontManager.drawCustomTextScaled(context, line, drawX, y, color, false, scale);
    }

    boolean isUpdateHeader(String line) {
        String value = line.toLowerCase();
        return value.contains("добав")
            || value.contains("исправ")
            || value.contains("улучш")
            || value.contains("added")
            || value.contains("fixed")
            || value.contains("improved");
    }

    void loadUpdateText() {
        this.updateLines.clear();

        try {
            Optional resource = MinecraftClient.getInstance().getResourceManager().getResource(UPDATE_FILE);
            if (resource.isEmpty()) {
                return;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(((Resource)resource.get()).getInputStream(), StandardCharsets.UTF_8))) {
                List<String> lines = reader.lines().toList();
                if (!lines.isEmpty()) {
                    this.updateVersion = lines.get(0);
                }

                if (lines.size() > 1) {
                    this.updateDate = lines.get(1);
                }

                for (int i = 2; i < lines.size(); i++) {
                    this.updateLines.add(lines.get(i));
                }
            }
        } catch (Exception var7) {
        }
    }

    float updateMaxScroll(int panelH) {
        float content = 0.0F;

        for (String line : this.updateLines) {
            content += line.isEmpty() ? 8.0F : 15.0F;
        }

        return Math.max(0.0F, content - (panelH - 76.0F));
    }

    int updatePanelWidth() {
        return Math.min(310, Math.max(220, Math.round(this.menuWidth * 0.22F)));
    }

    int updatePanelHeight() {
        return Math.min(430, Math.max(250, this.menuHeight - 130));
    }

    int updatePanelY() {
        return Math.max(24, this.menuHeight / 2 - this.updatePanelHeight() / 2);
    }

    void updateMenuMetrics() {
        if (this.client != null && this.client.getWindow() != null) {
            float minecraftScale = Math.max(1.0F, this.client.getWindow().getScaleFactor());
            this.menuRenderScale = 1.8F / minecraftScale;
        } else {
            this.menuRenderScale = 1.0F;
        }

        this.menuWidth = Math.max(1, Math.round(this.width / this.menuRenderScale));
        this.menuHeight = Math.max(1, Math.round(this.height / this.menuRenderScale));
    }

    boolean isOnUpdateScrollbar(double mouseX, double mouseY) {
        int x = 18;
        int y = this.updatePanelY();
        int w = this.updatePanelWidth();
        int h = this.updatePanelHeight();
        if (!(this.updateMaxScroll(h) <= 0.0F) && !(mouseX < x + w - 16) && !(mouseX > x + w)) {
            float[] thumb = this.scrollbarThumb(y, h);
            return mouseY >= thumb[0] - 4.0F && mouseY <= thumb[1] + 4.0F;
        } else {
            return false;
        }
    }

    void dragUpdateScrollbar(double mouseY) {
        int h = this.updatePanelHeight();
        float max = this.updateMaxScroll(h);
        if (!(max <= 0.0F)) {
            float trackY = this.updatePanelY() + 62.0F;
            float trackH = h - 76.0F;
            float thumbH = Math.max(24.0F, trackH * Math.min(1.0F, trackH / (trackH + max)));
            float ratio = (float)((mouseY - trackY - this.updateScrollDragOffset) / Math.max(1.0F, trackH - thumbH));
            this.updateScroll = this.updateTargetScroll = MathHelper.clamp(ratio, 0.0F, 1.0F) * max;
        }
    }

    float getScrollbarDragOffset(float mouseY) {
        float[] thumb = this.scrollbarThumb(this.updatePanelY(), this.updatePanelHeight());
        return MathHelper.clamp(mouseY - thumb[0], 0.0F, Math.max(1.0F, thumb[1] - thumb[0]));
    }

    float[] scrollbarThumb(int y, int h) {
        float max = this.updateMaxScroll(h);
        float trackY = y + 62.0F;
        float trackH = h - 76.0F;
        float thumbH = Math.max(24.0F, trackH * Math.min(1.0F, trackH / (trackH + max)));
        float thumbY = max <= 0.0F ? trackY : trackY + (trackH - thumbH) * (this.updateScroll / max);
        return new float[]{thumbY, thumbY + thumbH};
    }

    float frameDelta() {
        long now = System.nanoTime();
        if (this.lastFrameTime == 0L) {
            this.lastFrameTime = now;
            return 0.016666668F;
        } else {
            float dt = (float)(now - this.lastFrameTime) / 1.0E9F;
            this.lastFrameTime = now;
            return MathHelper.clamp(dt, 0.004166667F, 0.05F);
        }
    }

    float approach(float value, float target, float dt, float speed) {
        float factor = 1.0F - (float)Math.exp(-speed * dt);
        return value + (target - value) * factor;
    }

    int withAlpha(int rgb, int alpha) {
        return MathHelper.clamp(alpha, 0, 255) << 24 | rgb & 16777215;
    }
}
