package com.example.auravisuals.client.gui;

import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.module.render.FriendLabels;
import com.example.auravisuals.setting.BindSetting;
import com.example.auravisuals.theme.ThemeManager;
import com.example.auravisuals.util.friendlabels.FriendLabelsClient$Entry;
import com.example.auravisuals.util.render.RenderUtil;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public final class FriendLabelsScreen extends Screen {
    private static final int[] COLORS = new int[]{-1, -39322, -10033782, -10364417, -11430, -3635969};
    private final Screen parent;
    private final FriendLabels module;
    private TextFieldWidget nameField;
    private boolean groups;
    private boolean requests;
    private boolean listeningBind;
    private float animation;

    public FriendLabelsScreen(Screen parent, FriendLabels module) {
        super(Text.literal("FriendLabels"));
        this.parent = parent;
        this.module = module;
    }

    @Override
    protected void init() {
        int x = (this.width - 360) / 2;
        this.nameField = new TextFieldWidget(this.textRenderer, x + 18, (this.height - 286) / 2 + 48, 235, 20, Text.literal("Никнейм"));
        this.nameField.setMaxLength(32);
        this.addDrawableChild(this.nameField);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this.animation = this.animation + (1.0F - this.animation) * Math.min(1.0F, deltaTicks * 0.18F);
        context.fill(0, 0, this.width, this.height, 1912602624);
        int w = 360;
        int h = 286;
        int x = (this.width - w) / 2;
        int y = (this.height - h) / 2;
        context.getMatrices().pushMatrix();
        float e = 0.86F + this.animation * 0.14F;
        context.getMatrices().translate(this.width / 2.0F, this.height / 2.0F);
        context.getMatrices().scale(e, e);
        context.getMatrices().translate(-this.width / 2.0F, -this.height / 2.0F);
        panel(context, x, y, w, h);
        this.button(context, x + 16, y + 13, 88, 24, "Друзья", !this.groups, mouseX, mouseY);
        this.button(context, x + 110, y + 13, 88, 24, "Группы", this.groups, mouseX, mouseY);
        if (this.groups) {
            FontManager.drawCenteredText(context, "Раздел групп будет добавлен позже", x + w / 2.0F, y + 130, -4670524);
        } else {
            this.renderFriends(context, x, y, mouseX, mouseY);
        }

        context.getMatrices().popMatrix();
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    private void renderFriends(DrawContext c, int x, int y, int mx, int my) {
        this.button(c, x + 260, y + 48, 82, 20, "Добавить", false, mx, my);
        this.button(c, x + 260, y + 74, 82, 20, "Заявки (" + this.module.getClient().getRequests().size() + ")", this.requests, mx, my);
        FontManager.drawCustomText(c, "Поставить метку", x + 18, y + 108, -1);
        this.button(
            c,
            x + 18,
            y + 124,
            150,
            20,
            this.listeningBind ? "Нажмите кнопку..." : BindSetting.getBindName(this.module.getLabelBind().getValue()),
            this.listeningBind,
            mx,
            my
        );
        FontManager.drawCustomText(c, "Цвет метки", x + 18, y + 154, -1);

        for (int i = 0; i < COLORS.length; i++) {
            int cx = x + 18 + i * 27;
            int cy = y + 170;
            RenderUtil.drawRoundedRect(c, cx, cy, 20.0F, 20.0F, 6.0F, COLORS[i]);
            if ((this.module.getLabelColor().getValue() | 0xFF000000) == COLORS[i]) {
                RenderUtil.drawRoundedRect(c, cx - 2, cy - 2, 24.0F, 24.0F, 8.0F, 1442840575);
            }
        }

        FontManager.drawCustomText(c, this.requests ? "Входящие заявки" : "Друзья", x + 18, y + 205, -1);
        int rowY = y + 220;
        if (this.requests) {
            int ix = 0;

            for (FriendLabelsClient$Entry entry : this.module.getClient().getRequests()) {
                FontManager.drawCustomText(c, entry.name, x + 22, rowY + ix * 22 + 6, -2499615);
                this.button(c, x + 190, rowY + ix * 22, 70, 18, "Принять", false, mx, my);
                this.button(c, x + 266, rowY + ix * 22, 70, 18, "Отклонить", false, mx, my);
                if (++ix >= 2) {
                    break;
                }
            }
        } else {
            int ix = 0;

            for (FriendLabelsClient$Entry entryx : this.module.getClient().getFriends()) {
                int color = entryx.online ? -9836665 : -6710107;
                FontManager.drawCustomText(c, entryx.name + (entryx.online ? "" : " • не в сети"), x + 22, rowY + ix * 18 + 4, color);
                if (++ix >= 3) {
                    break;
                }
            }
        }
    }

    private void button(DrawContext c, int x, int y, int w, int h, String text, boolean active, int mx, int my) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        int color = active ? -867014488 : (hover ? -1203681968 : -1691800524);
        RenderUtil.drawRoundedRect(c, x, y, w, h, 6.0F, color);
        FontManager.drawCustomText(c, text, x + (w - FontManager.getWidth(text)) / 2.0F, y + (h - FontManager.getHeight()) / 2.0F, -1);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        if (this.listeningBind) {
            this.module.getLabelBind().setValue(BindSetting.encodeMouseButton(click.button()));
            this.listeningBind = false;
            return true;
        }

        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        int x = (this.width - 360) / 2;
        int y = (this.height - 286) / 2;
        if (in(mx, my, x + 16, y + 13, 88, 24)) {
            this.groups = false;
            this.requests = false;
            return true;
        }

        if (in(mx, my, x + 110, y + 13, 88, 24)) {
            this.groups = true;
            this.requests = false;
            return true;
        }

        if (this.groups) {
            return super.mouseClicked(click, doubled);
        }

        if (in(mx, my, x + 260, y + 48, 82, 20)) {
            String name = this.nameField.getText().trim();
            if (!name.isEmpty()) {
                this.module.getClient().requestFriend(name);
                this.nameField.setText("");
            }

            return true;
        } else {
            if (in(mx, my, x + 260, y + 74, 82, 20)) {
                this.requests = !this.requests;
                return true;
            }

            if (in(mx, my, x + 18, y + 124, 150, 20)) {
                this.listeningBind = true;
                return true;
            }

            for (int i = 0; i < COLORS.length; i++) {
                if (in(mx, my, x + 18 + i * 27, y + 170, 20, 20)) {
                    this.module.getLabelColor().setValue(COLORS[i]);
                    return true;
                }
            }

            if (this.requests) {
                int rowY = y + 220;
                int ix = 0;

                for (FriendLabelsClient$Entry e : this.module.getClient().getRequests()) {
                    if (in(mx, my, x + 190, rowY + ix * 22, 70, 18)) {
                        this.module.getClient().accept(e.login);
                        return true;
                    }

                    if (in(mx, my, x + 266, rowY + ix * 22, 70, 18)) {
                        this.module.getClient().decline(e.login);
                        return true;
                    }

                    if (++ix >= 2) {
                        break;
                    }
                }
            }

            return super.mouseClicked(click, doubled);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (this.listeningBind) {
            this.module.getLabelBind().setValue(input.key() == 256 ? -1 : input.key());
            this.listeningBind = false;
            return true;
        } else {
            return super.keyPressed(input);
        }
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static void panel(DrawContext c, int x, int y, int w, int h) {
        int base = ThemeManager.INSTANCE.getHudBackgroundColor();
        RenderUtil.drawRoundedRect(c, x, y, w, h, 10.0F, -352321536 | base & 16777215);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }
}
