package com.example.auravisuals.client.gui;

import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.client.gui.render.RoundedRenderer;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.BindSetting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class KeybindManagerScreen extends Screen {
    static final int RED = -1824962;
    final Screen parent;
    final List<KeybindManagerScreen$Key> keys = new ArrayList<>();
    int selectedCode = -1;
    float listX;
    float listY;
    float listW;
    float listH;
    float listScroll = 0.0F;
    float mbX;
    float mbY;
    float mbW;
    float mbH;

    public KeybindManagerScreen(Screen parent) {
        super(Text.literal("Кейбинд менеджер"));
        this.parent = parent;
    }

    static Object[] k(String label, int code) {
        return new Object[]{label, code, 1.0F};
    }

    static Object[] k(String label, int code, float w) {
        return new Object[]{label, code, w};
    }

    @Override
    protected void init() {
        this.keys.clear();
        Object[][][] rows = new Object[][][]{
            {
                    k("`", 96),
                    k("1", 49),
                    k("2", 50),
                    k("3", 51),
                    k("4", 52),
                    k("5", 53),
                    k("6", 54),
                    k("7", 55),
                    k("8", 56),
                    k("9", 57),
                    k("0", 48),
                    k("-", 45),
                    k("=", 61),
                    k("BACK", 259, 2.0F)
            },
            {
                    k("TAB", 258, 1.5F),
                    k("Q", 81),
                    k("W", 87),
                    k("E", 69),
                    k("R", 82),
                    k("T", 84),
                    k("Y", 89),
                    k("U", 85),
                    k("I", 73),
                    k("O", 79),
                    k("P", 80),
                    k("[", 91),
                    k("]", 93),
                    k("\\", 92, 1.5F)
            },
            {
                    k("CAPS", 280, 1.8F),
                    k("A", 65),
                    k("S", 83),
                    k("D", 68),
                    k("F", 70),
                    k("G", 71),
                    k("H", 72),
                    k("J", 74),
                    k("K", 75),
                    k("L", 76),
                    k(";", 59),
                    k("'", 39),
                    k("ENTER", 257, 2.2F)
            },
            {
                    k("SHIFT", 340, 2.3F),
                    k("Z", 90),
                    k("X", 88),
                    k("C", 67),
                    k("V", 86),
                    k("B", 66),
                    k("N", 78),
                    k("M", 77),
                    k(",", 44),
                    k(".", 46),
                    k("/", 47),
                    k("SHIFT", 344, 2.7F)
            },
            {k("CTRL", 341, 1.6F), k("ALT", 342, 1.4F), k("SPACE", 32, 7.0F), k("ALT", 346, 1.4F), k("CTRL", 345, 1.6F)}
        };
        float unit = 26.0F;
        float kh = 24.0F;
        float kgap = 4.0F;
        float totalW = 15.0F * unit + 14.0F * kgap;
        float kbH = rows.length * (kh + kgap);
        this.listW = 150.0F;
        this.mbW = 78.0F;
        float gap2 = 22.0F;
        float layoutW = totalW + gap2 + this.mbW + gap2 + this.listW;
        float startX = this.width / 2.0F - layoutW / 2.0F;
        float startY = this.height / 2.0F - kbH / 2.0F + 6.0F;
        float y = startY;

        for (Object[][] row : rows) {
            float x = startX;

            for (Object[] e : row) {
                KeybindManagerScreen$Key key = new KeybindManagerScreen$Key((String)e[0], (Integer)e[1], (Float)e[2]);
                key.x = x;
                key.y = y;
                key.w = key.wUnits * unit + (key.wUnits - 1.0F) * kgap;
                key.h = kh;
                this.keys.add(key);
                x += key.w + kgap;
            }

            y += kh + kgap;
        }

        this.mbX = startX + totalW + gap2;
        this.mbH = 108.0F;
        this.mbY = startY + (kbH - this.mbH) / 2.0F;
        this.addMouseZone("M1", 0, this.mbX + 6.0F, this.mbY + 6.0F, this.mbW * 0.5F - 9.0F, 40.0F);
        this.addMouseZone("M2", 1, this.mbX + this.mbW * 0.5F + 3.0F, this.mbY + 6.0F, this.mbW * 0.5F - 9.0F, 40.0F);
        this.addMouseZone("M3", 2, this.mbX + this.mbW * 0.5F - 7.0F, this.mbY + 10.0F, 14.0F, 30.0F);
        this.addMouseZone("M4", 3, this.mbX + 2.0F, this.mbY + 58.0F, 13.0F, 16.0F);
        this.addMouseZone("M5", 4, this.mbX + 2.0F, this.mbY + 40.0F, 13.0F, 16.0F);
        this.addMouseZone("M6", 5, this.mbX + this.mbW - 15.0F, this.mbY + 58.0F, 13.0F, 16.0F);
        this.addMouseZone("M7", 6, this.mbX + this.mbW - 15.0F, this.mbY + 40.0F, 13.0F, 16.0F);
        this.listX = this.mbX + this.mbW + gap2;
        this.listY = startY;
        this.listH = kbH + 40.0F;
    }

    void addMouseZone(String label, int button, float x, float y, float w, float h) {
        KeybindManagerScreen$Key key = new KeybindManagerScreen$Key(label, BindSetting.encodeMouseButton(button), 1.0F);
        key.x = x;
        key.y = y;
        key.w = w;
        key.h = h;
        this.keys.add(key);
    }

    Module moduleForKey(int code) {
        if (code == -1) {
            return null;
        }

        for (Module m : ModuleManager.INSTANCE.getModules()) {
            if (m.getKey() == code) {
                return m;
            }
        }

        return null;
    }

    static int withAlpha(int color, int alpha) {
        return alpha << 24 | color & 16777215;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        try {
            context.applyBlur();
        } catch (Throwable var17) {
        }

        context.fill(0, 0, this.width, this.height, -2013265920);
        float titleX = this.keys.isEmpty() ? 20.0F : this.keys.get(0).x;
        float titleY = this.keys.isEmpty() ? 20.0F : this.keys.get(0).y - 34.0F;
        FontManager.drawCustomTextScaled(context, "Кейбинд менеджер", titleX, titleY, -1, true, 0.42F);
        boolean backHover = mouseX >= 12 && mouseX <= 84 && mouseY >= 10 && mouseY <= 30;
        FontManager.drawCustomTextScaled(context, "← Назад", 14.0F, 14.0F, backHover ? -1 : -4605498, true, 0.3F);
        RoundedRenderer.drawRoundedRect(context, this.mbX, this.mbY, this.mbW, this.mbH, 22.0F, -871691505);
        RoundedRenderer.drawRoundedOutline(context, this.mbX, this.mbY, this.mbW, this.mbH, 22.0F, 1.0F, 1728053247);
        context.fill((int)(this.mbX + this.mbW / 2.0F), (int)(this.mbY + 6.0F), (int)(this.mbX + this.mbW / 2.0F + 1.0F), (int)(this.mbY + 44.0F), 1157627903);
        float mlw = FontManager.getScaledTextWidth("Мышь", 0.24F);
        FontManager.drawCustomTextScaled(context, "Мышь", this.mbX + this.mbW / 2.0F - mlw / 2.0F, this.mbY + this.mbH + 5.0F, -4605498, false, 0.24F);

        for (KeybindManagerScreen$Key key : this.keys) {
            Module bound = this.moduleForKey(key.code);
            boolean sel = key.code == this.selectedCode;
            boolean hover = mouseX >= key.x && mouseX <= key.x + key.w && mouseY >= key.y && mouseY <= key.y + key.h;
            if (sel) {
                RoundedRenderer.drawVerticalGradientRoundedRect(context, key.x, key.y, key.w, key.h, 4.0F, -1824962, -8776670);
                RoundedRenderer.drawRoundedOutline(context, key.x, key.y, key.w, key.h, 4.0F, 1.2F, -1);
            } else if (bound != null) {
                RoundedRenderer.drawVerticalGradientRoundedRect(context, key.x, key.y, key.w, key.h, 4.0F, -259384792, -535620840);
                RoundedRenderer.drawRoundedOutline(context, key.x, key.y, key.w, key.h, 4.0F, 1.0F, withAlpha(-1824962, 220));
            } else {
                RoundedRenderer.drawRoundedRect(context, key.x, key.y, key.w, key.h, 4.0F, hover ? -434365404 : -871494125);
                if (hover) {
                    RoundedRenderer.drawRoundedOutline(context, key.x, key.y, key.w, key.h, 4.0F, 1.0F, withAlpha(-1824962, 150));
                }
            }

            float ksc = 0.2F;
            float ktw = FontManager.getScaledTextWidth(key.label, ksc);
            int kcol = !sel && bound == null ? -3487020 : -1;
            FontManager.drawCustomTextScaled(context, key.label, key.x + (key.w - ktw) / 2.0F, key.y + key.h / 2.0F - 4.0F, kcol, false, ksc);
        }

        RoundedRenderer.drawVerticalGradientRoundedRect(context, this.listX, this.listY, this.listW, this.listH, 6.0F, -535291872, -536015598);
        RoundedRenderer.drawRoundedRect(context, this.listX, this.listY, this.listW, 2.0F, 1.0F, withAlpha(-1824962, 220));
        String header = this.selectedCode != -1 ? "Выбери модуль" : "Все бинды";
        FontManager.drawCustomTextScaled(context, header, this.listX + 8.0F, this.listY + 7.0F, -1, true, 0.3F);
        context.enableScissor((int)this.listX, (int)this.listY + 22, (int)(this.listX + this.listW), (int)(this.listY + this.listH));
        float ry = this.listY + 26.0F - this.listScroll;

        for (Module m : ModuleManager.INSTANCE.getModules()) {
            if (this.selectedCode != -1 || m.getKey() != -1) {
                boolean rowHover = mouseX >= this.listX + 4.0F && mouseX <= this.listX + this.listW - 4.0F && mouseY >= ry && mouseY <= ry + 16.0F;
                if (rowHover) {
                    RoundedRenderer.drawRoundedRect(context, this.listX + 4.0F, ry, this.listW - 8.0F, 16.0F, 3.0F, withAlpha(-1824962, 70));
                }

                FontManager.drawCustomTextScaled(context, trim(m.getName(), 16), this.listX + 8.0F, ry + 4.0F, -2565920, false, 0.24F);
                if (m.getKey() != -1) {
                    String kn = this.keyName(m.getKey());
                    float kw = FontManager.getScaledTextWidth(kn, 0.24F);
                    FontManager.drawCustomTextScaled(context, kn, this.listX + this.listW - 8.0F - kw, ry + 4.0F, withAlpha(-1824962, 255), false, 0.24F);
                }

                ry += 18.0F;
            }
        }

        context.disableScissor();
    }

    static String trim(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    String keyName(int code) {
        for (KeybindManagerScreen$Key k : this.keys) {
            if (k.code == code) {
                return k.label;
            }
        }

        return "#" + code;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        int button = click.button();
        if (mx >= 12.0 && mx <= 70.0 && my >= 8.0 && my <= 24.0) {
            this.client.setScreen(this.parent);
            return true;
        }

        for (KeybindManagerScreen$Key key : this.keys) {
            if (mx >= key.x && mx <= key.x + key.w && my >= key.y && my <= key.y + key.h) {
                if (button == 1) {
                    Module bound = this.moduleForKey(key.code);
                    if (bound != null) {
                        bound.setKey(-1);
                    }

                    if (this.selectedCode == key.code) {
                        this.selectedCode = -1;
                    }
                } else {
                    this.selectedCode = this.selectedCode == key.code ? -1 : key.code;
                }

                return true;
            }
        }

        if (mx >= this.listX && mx <= this.listX + this.listW && my >= this.listY + 20.0F && my <= this.listY + this.listH) {
            float ry = this.listY + 24.0F - this.listScroll;

            for (Module m : ModuleManager.INSTANCE.getModules()) {
                if (this.selectedCode != -1 || m.getKey() != -1) {
                    if (my >= ry && my <= ry + 16.0F) {
                        if (this.selectedCode != -1) {
                            m.setKey(this.selectedCode);
                            this.selectedCode = -1;
                        }

                        return true;
                    }

                    ry += 18.0F;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseX >= this.listX && mouseX <= this.listX + this.listW) {
            this.listScroll = Math.max(0.0F, this.listScroll - (float)verticalAmount * 16.0F);
            return true;
        } else {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
    }

    @Override
    public boolean shouldPause() {
        return true;
    }
}
