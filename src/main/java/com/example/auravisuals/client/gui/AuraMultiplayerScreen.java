package com.example.auravisuals.client.gui;

import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.client.gui.render.RoundedRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.network.ServerInfo.ServerType;
import net.minecraft.client.option.ServerList;
import net.minecraft.text.Text;

public class AuraMultiplayerScreen extends Screen {
    private static final int RED = -1824962;
    private final Screen parent;
    private ServerList serverList;
    private float scroll = 0.0F;
    private float listX;
    private float listY;
    private float listW;
    private float listH;
    private static final float ROW_H = 30.0F;
    private static final float ROW_GAP = 6.0F;

    public AuraMultiplayerScreen(Screen parent) {
        super(Text.literal("Сервера"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.serverList = new ServerList(this.client);

        try {
            this.serverList.loadFile();
        } catch (Throwable var2) {
        }

        this.listW = 320.0F;
        this.listX = this.width / 2.0F - this.listW / 2.0F;
        this.listY = 56.0F;
        this.listH = this.height - 56 - 40;
    }

    private static int withAlpha(int color, int alpha) {
        return alpha << 24 | color & 16777215;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        try {
            context.applyBlur();
        } catch (Throwable var13) {
        }

        context.fill(0, 0, this.width, this.height, -2013265920);
        float tsc = 0.5F;
        float tw = FontManager.getScaledTextWidth("Сервера", tsc);
        FontManager.drawCustomTextScaled(context, "Сервера", this.width / 2.0F - tw / 2.0F, 18.0F, -1, true, tsc);
        boolean backHover = mouseX >= 12 && mouseX <= 84 && mouseY >= 12 && mouseY <= 30;
        FontManager.drawCustomTextScaled(context, "← Назад", 14.0F, 16.0F, backHover ? -1 : -4605498, true, 0.3F);
        this.drawBtn(context, "Прямое подключение", this.width / 2.0F - 162.0F, this.height - 32, 158.0F, 24.0F, mouseX, mouseY);
        this.drawBtn(context, "Добавить сервер", this.width / 2.0F + 4.0F, this.height - 32, 158.0F, 24.0F, mouseX, mouseY);
        context.enableScissor((int)this.listX, (int)this.listY, (int)(this.listX + this.listW), (int)(this.listY + this.listH));
        float ry = this.listY - this.scroll;
        int count = this.serverList == null ? 0 : this.serverList.size();

        for (int i = 0; i < count; i++) {
            ServerInfo info = this.serverList.get(i);
            boolean hover = mouseX >= this.listX
                && mouseX <= this.listX + this.listW
                && mouseY >= ry
                && mouseY <= ry + 30.0F
                && mouseY >= this.listY
                && mouseY <= this.listY + this.listH;
            if (hover) {
                RoundedRenderer.drawVerticalGradientRoundedRect(context, this.listX, ry, this.listW, 30.0F, 6.0F, -264628702, -535555040);
                RoundedRenderer.drawRoundedOutline(context, this.listX, ry, this.listW, 30.0F, 6.0F, 1.0F, withAlpha(-1824962, 200));
            } else {
                RoundedRenderer.drawRoundedRect(context, this.listX, ry, this.listW, 30.0F, 6.0F, -871230951);
            }

            FontManager.drawCustomTextScaled(context, info.name, this.listX + 10.0F, ry + 5.0F, -1, false, 0.27F);
            FontManager.drawCustomTextScaled(context, info.address, this.listX + 10.0F, ry + 17.0F, -6645082, false, 0.2F);
            ry += 36.0F;
        }

        context.disableScissor();
    }

    private void drawBtn(DrawContext context, String label, float x, float y, float w, float h, int mouseX, int mouseY) {
        boolean hover = mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
        if (hover) {
            RoundedRenderer.drawVerticalGradientRoundedRect(context, x, y, w, h, 6.0F, -264628702, -535555040);
            RoundedRenderer.drawRoundedOutline(context, x, y, w, h, 6.0F, 1.0F, withAlpha(-1824962, 210));
        } else {
            RoundedRenderer.drawRoundedRect(context, x, y, w, h, 6.0F, -871230951);
        }

        float lsc = 0.24F;
        float ltw = FontManager.getScaledTextWidth(label, lsc);
        FontManager.drawCustomTextScaled(context, label, x + (w - ltw) / 2.0F, y + h / 2.0F - 4.0F, hover ? -1 : -2565920, false, lsc);
    }

    private void connect(ServerInfo info) {
        ConnectScreen.connect(this, this.client, ServerAddress.parse(info.address), info, false, null);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }

        if (mx >= 12.0 && mx <= 84.0 && my >= 12.0 && my <= 30.0) {
            this.client.setScreen(this.parent);
            return true;
        }

        if (my >= this.height - 32 && my <= this.height - 8) {
            if (mx >= this.width / 2.0F - 162.0F && mx <= this.width / 2.0F - 4.0F) {
                this.client.setScreen(new DirectConnectScreen(this, connected -> {
                    if (!connected) {
                        this.client.setScreen(this);
                    }
                }, new ServerInfo("", "", ServerType.OTHER)));
                return true;
            }

            if (mx >= this.width / 2.0F + 4.0F && mx <= this.width / 2.0F + 162.0F) {
                ServerInfo fresh = new ServerInfo(Text.translatable("selectServer.defaultName").getString(), "", ServerType.OTHER);
                this.client.setScreen(new AddServerScreen(this, Text.translatable("addServer.title"), added -> {
                    if (added) {
                        this.serverList.add(fresh, false);
                        this.serverList.saveFile();
                    }

                    this.client.setScreen(this);
                }, fresh));
                return true;
            }
        }

        if (mx >= this.listX && mx <= this.listX + this.listW && my >= this.listY && my <= this.listY + this.listH) {
            float ry = this.listY - this.scroll;
            int count = this.serverList == null ? 0 : this.serverList.size();

            for (int i = 0; i < count; i++) {
                if (my >= ry && my <= ry + 30.0F) {
                    this.connect(this.serverList.get(i));
                    return true;
                }

                ry += 36.0F;
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int count = this.serverList == null ? 0 : this.serverList.size();
        float max = Math.max(0.0F, count * 36.0F - this.listH);
        this.scroll = (float)Math.max(0.0, Math.min(max, this.scroll - verticalAmount * 24.0));
        return true;
    }
}
