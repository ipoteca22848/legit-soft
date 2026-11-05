package com.example.auravisuals.client.gui;

import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.client.gui.render.RoundedRenderer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.client.gui.screen.advancement.AdvancementsScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.text.Text;

public class AuraPauseScreen extends Screen {
    static final int RED = -1824962;
    final List<AuraPauseScreen$Btn> buttons = new ArrayList<>();

    public AuraPauseScreen() {
        super(Text.literal("Меню"));
    }

    @Override
    protected void init() {
        this.buttons.clear();
        int w = 220;
        int h = 24;
        int gap = 7;
        int half = (w - gap) / 2;
        int cx = this.width / 2;
        int left = cx - w / 2;
        int y = this.height / 2 - 56;
        this.add("Вернуться к игре", left, y, w, h, () -> this.client.setScreen(null));
        int var8;
        this.add("Достижения", left, var8 = y + h + gap, half, h, () -> {
            if (this.client.player != null) {
                this.client.setScreen(new AdvancementsScreen(this.client.player.networkHandler.getAdvancementHandler(), this));
            }
        });
        this.add("Статистика", left + half + gap, var8, half, h, () -> {
            if (this.client.player != null) {
                this.client.setScreen(new StatsScreen(this, this.client.player.getStatHandler()));
            }
        });
        this.add("Настройки", left, y = var8 + h + gap, half, h, () -> this.client.setScreen(new OptionsScreen(this, this.client.options)));
        this.add("Кейбинд менеджер", left + half + gap, y, half, h, () -> this.client.setScreen(new KeybindManagerScreen(this)));
        int var10;
        this.add("Сохранить и выйти", left, var10 = y + h + gap, w, h, () -> this.client.disconnect(ClientWorld.QUITTING_MULTIPLAYER_TEXT));
    }

    void add(String label, int x, int y, int w, int h, Runnable action) {
        AuraPauseScreen$Btn b = new AuraPauseScreen$Btn(label, action);
        b.x = x;
        b.y = y;
        b.w = w;
        b.h = h;
        this.buttons.add(b);
    }

    static int withAlpha(int color, int alpha) {
        return alpha << 24 | color & 16777215;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        try {
            context.applyBlur();
        } catch (Throwable var15) {
        }

        context.fill(0, 0, this.width, this.height, 1711276032);
        float ty = this.height / 2.0F - 94.0F;
        float tsc = 0.55F;
        float ttw = FontManager.getScaledTextWidth("Меню", tsc);
        FontManager.drawCustomTextScaled(context, "Меню", this.width / 2.0F - ttw / 2.0F, ty, -1, true, tsc);

        for (AuraPauseScreen$Btn b : this.buttons) {
            boolean hover = mouseX >= b.x && mouseX <= b.x + b.w && mouseY >= b.y && mouseY <= b.y + b.h;
            if (hover) {
                RoundedRenderer.drawVerticalGradientRoundedRect(context, b.x, b.y, b.w, b.h, 6.0F, -264628702, -535555040);
                RoundedRenderer.drawRoundedOutline(context, b.x, b.y, b.w, b.h, 6.0F, 1.0F, withAlpha(-1824962, 210));
            } else {
                RoundedRenderer.drawRoundedRect(context, b.x, b.y, b.w, b.h, 6.0F, -871230951);
            }

            float lsc = 0.26F;
            float ltw = FontManager.getScaledTextWidth(b.label, lsc);
            int textColor = hover ? -1 : -2565920;
            FontManager.drawCustomTextScaled(context, b.label, b.x + (b.w - ltw) / 2.0F, b.y + b.h / 2.0F - 4.0F, textColor, false, lsc);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        if (click.button() == 0) {
            for (AuraPauseScreen$Btn b : this.buttons) {
                if (mx >= b.x && mx <= b.x + b.w && my >= b.y && my <= b.y + b.h) {
                    b.action.run();
                    return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean shouldPause() {
        return true;
    }
}
