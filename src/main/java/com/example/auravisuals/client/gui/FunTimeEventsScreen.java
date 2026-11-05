package com.example.auravisuals.client.gui;

import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.client.gui.font.icon.IconTtfBridge;
import com.example.auravisuals.module.render.ClickGui;
import com.example.auravisuals.theme.ThemeManager;
import com.example.auravisuals.util.animation.AnimationUtil;
import com.example.auravisuals.util.color.ColorUtil;
import com.example.auravisuals.util.events.FunTimeEventsClient;
import com.example.auravisuals.util.events.FunTimeEventsClient$FunTimeEvent;
import com.example.auravisuals.util.events.FunTimeEventsClient$FunTimeMine;
import com.example.auravisuals.util.events.FunTimeEventsClient$MineRarityKind;
import com.example.auravisuals.util.render.RenderUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

public class FunTimeEventsScreen extends Screen {
    static FunTimeEventsScreen$PanelTab rememberedPanelTab = FunTimeEventsScreen$PanelTab.EVENTS;
    static FunTimeEventsScreen$ListMode rememberedListMode = FunTimeEventsScreen$ListMode.ALL;
    static FunTimeEventsScreen$MineListMode rememberedMineListMode = FunTimeEventsScreen$MineListMode.ALL;
    static boolean rememberedFilterAnarchy4Digits = true;
    static boolean rememberedFilterAnarchy3Digits = false;
    final Screen parent;
    int panelLeft;
    int panelTop;
    int panelW = 448;
    int panelH;
    float openAnim = 0.0F;
    boolean closing;
    float scroll;
    float scrollTarget;
    boolean draggingScrollBar;
    int lastContentTop;
    int lastContentH;
    int lastTotalH;
    boolean loadingEvents = true;
    boolean loadingMines;
    FunTimeEventsScreen$PanelTab panelTab = FunTimeEventsScreen$PanelTab.EVENTS;
    final FunTimeEventsScreen$HitRect[] panelTabHits = new FunTimeEventsScreen$HitRect[]{new FunTimeEventsScreen$HitRect(), new FunTimeEventsScreen$HitRect()};
    FunTimeEventsScreen$ListMode listMode = FunTimeEventsScreen$ListMode.ALL;
    FunTimeEventsScreen$MineListMode mineListMode = FunTimeEventsScreen$MineListMode.ALL;
    boolean filterAnarchy4Digits = true;
    boolean filterAnarchy3Digits = false;
    static final float TOOLBAR_SCALE = 0.32F;
    static final float CARD_SUB_SCALE = 0.3F;
    static final int JOIN_BTN_SIZE = 14;
    static final float JOIN_ICON_BOX = 6.2999997F;
    static final float JOIN_ICON_INSET_X = 5.05F;
    static final float JOIN_ICON_INSET_Y = 3.8500001F;
    static final float JOIN_ICON_FALLBACK_SCALE = 0.18F;
    static final float TEXT_Y_OFFSET = -2.0F;
    int toolbarBtnY;
    int toolbarBtnH = 18;
    final FunTimeEventsScreen$HitRect[] modeHits = new FunTimeEventsScreen$HitRect[]{
        new FunTimeEventsScreen$HitRect(), new FunTimeEventsScreen$HitRect(), new FunTimeEventsScreen$HitRect()
    };
    final FunTimeEventsScreen$HitRect[] verHits = new FunTimeEventsScreen$HitRect[]{new FunTimeEventsScreen$HitRect(), new FunTimeEventsScreen$HitRect()};

    public FunTimeEventsScreen(Screen parent) {
        super(Text.of("FunTime Events"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.panelW = Math.min(448, this.width - 30);
        this.panelH = Math.min(336, this.height - 30);
        this.panelLeft = (this.width - this.panelW) / 2;
        this.panelTop = (this.height - this.panelH) / 2;
        this.openAnim = 1.0F;
        this.closing = false;
        this.loadingEvents = true;
        this.loadingMines = false;
        this.panelTab = rememberedPanelTab;
        this.listMode = rememberedListMode;
        this.mineListMode = rememberedMineListMode;
        this.filterAnarchy4Digits = rememberedFilterAnarchy4Digits;
        this.filterAnarchy3Digits = rememberedFilterAnarchy3Digits;
        this.scroll = this.scrollTarget = 0.0F;
        FunTimeEventsClient.refreshAsync(() -> this.loadingEvents = false);
        if (this.panelTab == FunTimeEventsScreen$PanelTab.MINES) {
            this.loadingMines = true;
            FunTimeEventsClient.refreshMinesAsync(() -> this.loadingMines = false);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        if (this.closing) {
            if (this.client != null) {
                this.client.setScreen(this.parent);
            }
        } else {
            this.openAnim = 1.0F;
            int alphaInt = 255;
            int bg = ClickGui.applyBackgroundOpacity(ThemeManager.INSTANCE.getCurrentTheme().getPanelColor());
            if (ClickGui.blur.getValue()) {
                RenderUtil.prepareBlurLight(ClickGui.getBlurStrength());
                RenderUtil.drawRoundedBlurBehind(context, this.panelLeft + 2, this.panelTop + 2, this.panelW - 4, this.panelH - 4, 14.0F);
            }

            RenderUtil.drawRoundedRect(context, this.panelLeft, this.panelTop, this.panelW, this.panelH, 14.0F, bg);
            int prim = ThemeManager.INSTANCE.getCurrentTheme().getPrimaryColor();
            this.drawPanelTabs(context, mouseX, mouseY, alphaInt, prim);
            int closeX = this.panelLeft + this.panelW - 22;
            int closeY = this.panelTop + 8;
            boolean closeHover = mouseX >= closeX && mouseX <= closeX + 16 && mouseY >= closeY && mouseY <= closeY + 14;
            int closeCol = closeHover ? new Color(255, 120, 120, alphaInt).getRGB() : new Color(180, 180, 185, alphaInt).getRGB();
            drawTextScaled(context, "✕", closeX, closeY, closeCol, false, 0.5F);
            int btnW = 78;
            int btnH = 18;
            int btnX = this.panelLeft + 14;
            int btnY = this.panelTop + 36;
            this.toolbarBtnY = btnY;
            this.toolbarBtnH = btnH;
            boolean refHover = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
            boolean loading = this.panelTab == FunTimeEventsScreen$PanelTab.EVENTS ? this.loadingEvents : this.loadingMines;
            int btnBg = 0xFF000000 | prim & 16777215;
            if (loading) {
                btnBg = -14013902;
            } else if (refHover) {
                btnBg = ColorUtil.brighten(btnBg, 0.12F);
            }

            RenderUtil.drawRoundedRect(context, btnX, btnY, btnW, btnH, 5.0F, applyAlpha(btnBg, alphaInt / 255.0F));
            String refreshLabel = loading ? "Загрузка…" : "Обновить";
            float refreshW = FontManager.getScaledTextWidth(refreshLabel, 0.34F);
            drawTextScaled(context, refreshLabel, btnX + (btnW - refreshW) / 2.0F, btnY + 3, new Color(255, 255, 255, alphaInt).getRGB(), false, 0.34F);
            if (this.panelTab == FunTimeEventsScreen$PanelTab.EVENTS) {
                int modeStartX = this.filterModeToolbarStartX();
                this.drawFilterToolbar(context, mouseX, mouseY, alphaInt, prim, modeStartX, btnY, btnH);
            } else if (this.panelTab == FunTimeEventsScreen$PanelTab.MINES) {
                int modeStartX = this.filterModeToolbarStartX();
                this.drawMineFilterToolbar(context, mouseX, mouseY, alphaInt, prim, modeStartX, btnY, btnH);
            }

            this.drawVersionToolbar(context, mouseX, mouseY, alphaInt, prim, btnY, btnH);
            int contentTop = this.panelTop + 64;
            int contentH = this.panelH - 76;
            int cx = this.panelLeft + 10;
            int cw = this.panelW - 20;
            this.scroll = AnimationUtil.smoothScroll(this.scrollTarget, this.scroll, 0.82F, deltaTicks);
            String err = this.panelTab == FunTimeEventsScreen$PanelTab.EVENTS ? FunTimeEventsClient.getLastError() : FunTimeEventsClient.getLastMineError();
            List<FunTimeEventsClient$FunTimeEvent> rawEvents = FunTimeEventsClient.getEvents();
            List<FunTimeEventsClient$FunTimeEvent> events = this.applyFilters(rawEvents);
            List<FunTimeEventsClient$FunTimeMine> rawMines = FunTimeEventsClient.getMines();
            List<FunTimeEventsClient$FunTimeMine> mines = this.displayedMines(rawMines);
            if (this.panelTab == FunTimeEventsScreen$PanelTab.EVENTS && loading && rawEvents.isEmpty()) {
                String t = "Загрузка…";
                float sw = FontManager.getScaledTextWidth(t, 0.38F);
                drawTextScaled(context, t, cx + (cw - sw) / 2.0F, contentTop + contentH / 2.0F, new Color(160, 160, 170, alphaInt).getRGB(), false, 0.38F);
            } else if (this.panelTab == FunTimeEventsScreen$PanelTab.MINES && loading && rawMines.isEmpty()) {
                String t = "Загрузка…";
                float sw = FontManager.getScaledTextWidth(t, 0.38F);
                drawTextScaled(context, t, cx + (cw - sw) / 2.0F, contentTop + contentH / 2.0F, new Color(160, 160, 170, alphaInt).getRGB(), false, 0.38F);
            } else if (err.isEmpty() || (this.panelTab == FunTimeEventsScreen$PanelTab.EVENTS ? !rawEvents.isEmpty() : !rawMines.isEmpty())) {
                if (this.panelTab == FunTimeEventsScreen$PanelTab.EVENTS && events.isEmpty()) {
                    String t = "Нет ивентов по выбранным фильтрам";
                    float sw = FontManager.getScaledTextWidth(t, 0.36F);
                    drawTextScaled(context, t, cx + (cw - sw) / 2.0F, contentTop + contentH / 2.0F, new Color(150, 150, 160, alphaInt).getRGB(), false, 0.36F);
                } else if (this.panelTab == FunTimeEventsScreen$PanelTab.MINES && mines.isEmpty()) {
                    String t = "Нет шахт по выбранным фильтрам";
                    float sw = FontManager.getScaledTextWidth(t, 0.36F);
                    drawTextScaled(context, t, cx + (cw - sw) / 2.0F, contentTop + contentH / 2.0F, new Color(150, 150, 160, alphaInt).getRGB(), false, 0.36F);
                } else if (this.panelTab == FunTimeEventsScreen$PanelTab.EVENTS) {
                    context.enableScissor(cx, contentTop, cx + cw, contentTop + contentH);
                    float drawY = contentTop + this.scroll;
                    int col = 0;
                    int gap = 8;
                    int cardH = 36;
                    int rowH = 40;
                    int cardW = Math.max(120, (cw - 8) / 2);

                    for (FunTimeEventsClient$FunTimeEvent ev : events) {
                        int x = cx + col * (cardW + 8);
                        RenderUtil.drawRoundedRect(
                            context,
                            x,
                            (int)drawY,
                            cardW,
                            36.0F,
                            6.0F,
                            ClickGui.applyBackgroundOpacity(ThemeManager.INSTANCE.getCurrentTheme().getPanelColor())
                        );
                        boolean showJoin = ev.anarchyId > 0;
                        float joinLeft = x + cardW - 6 - 14;
                        int joinTop = (int)drawY + 11;
                        drawTextScaled(context, ev.name, x + 8, drawY + 6.0F, new Color(255, 255, 255, alphaInt).getRGB(), false, 0.36F);
                        if (showJoin) {
                            context.enableScissor(x + 8, (int)drawY + 18, (int)joinLeft - 2, (int)drawY + 34);
                        }

                        this.drawEventCardSecondLine(context, ev, x + 8, drawY + 20.0F, alphaInt);
                        if (showJoin) {
                            context.disableScissor();
                        }

                        if (showJoin) {
                            boolean jHover = mouseX >= joinLeft && mouseX <= joinLeft + 14.0F && mouseY >= joinTop && mouseY <= joinTop + 14;
                            int jBg = jHover
                                ? ColorUtil.brighten(ThemeManager.INSTANCE.getPrimaryColor(), 0.1F)
                                : ClickGui.applyBackgroundOpacity(ThemeManager.INSTANCE.getCurrentTheme().getButtonsColor());
                            RenderUtil.drawRoundedRect(context, (int)joinLeft, joinTop, 14.0F, 14.0F, 4.0F, jBg);
                            int ic = new Color(255, 255, 255, alphaInt).getRGB();
                            float ix = joinLeft + 5.05F;
                            float iy = joinTop + 3.8500001F;
                            if (!IconTtfBridge.tryDrawIconsFontChar(context, 'x', ix, iy, 6.2999997F, ic)) {
                                drawTextScaled(context, "→", ix + 1.0F, iy + 1.0F, ic, false, 0.18F);
                            }
                        }

                        if (++col >= 2) {
                            col = 0;
                            drawY += 40.0F;
                        }
                    }

                    if (col != 0) {
                        drawY += 40.0F;
                    }

                    context.disableScissor();
                    int totalH = Math.max(0, (int)Math.ceil(drawY - contentTop - this.scroll) + 4);
                    this.lastContentTop = contentTop;
                    this.lastContentH = contentH;
                    this.lastTotalH = totalH;
                    int maxScr = Math.max(0, totalH - contentH);
                    if (this.scrollTarget < -maxScr) {
                        this.scrollTarget = -maxScr;
                    }

                    if (this.scrollTarget > 0.0F) {
                        this.scrollTarget = 0.0F;
                    }

                    this.drawThemedScrollbar(context, cx + cw + 2, contentTop, contentH, totalH, alphaInt);
                } else {
                    context.enableScissor(cx, contentTop, cx + cw, contentTop + contentH);
                    float drawY = contentTop + this.scroll;
                    int col = 0;
                    int gap = 8;
                    int cardH = 36;
                    int rowH = 40;
                    int cardW = Math.max(120, (cw - 8) / 2);

                    for (FunTimeEventsClient$FunTimeMine m : mines) {
                        int xx = cx + col * (cardW + 8);
                        RenderUtil.drawRoundedRect(
                            context,
                            xx,
                            (int)drawY,
                            cardW,
                            36.0F,
                            6.0F,
                            ClickGui.applyBackgroundOpacity(ThemeManager.INSTANCE.getCurrentTheme().getPanelColor())
                        );
                        boolean showJoinx = m.anarchyId > 0;
                        float joinLeftx = xx + cardW - 6 - 14;
                        int joinTopx = (int)drawY + 11;
                        if (showJoinx) {
                            context.enableScissor(xx + 8, (int)drawY + 6, (int)joinLeftx - 2, (int)drawY + 34);
                        }

                        this.drawMineCardTitleLine(context, m, xx + 8, drawY + 6.0F, alphaInt);
                        this.drawMineCardSecondLine(context, m, xx + 8, drawY + 20.0F, alphaInt);
                        if (showJoinx) {
                            context.disableScissor();
                        }

                        if (showJoinx) {
                            boolean jHover = mouseX >= joinLeftx && mouseX <= joinLeftx + 14.0F && mouseY >= joinTopx && mouseY <= joinTopx + 14;
                            int jBg = jHover
                                ? ColorUtil.brighten(ThemeManager.INSTANCE.getPrimaryColor(), 0.1F)
                                : ClickGui.applyBackgroundOpacity(ThemeManager.INSTANCE.getCurrentTheme().getButtonsColor());
                            RenderUtil.drawRoundedRect(context, (int)joinLeftx, joinTopx, 14.0F, 14.0F, 4.0F, jBg);
                            int ic = new Color(255, 255, 255, alphaInt).getRGB();
                            float ix = joinLeftx + 5.05F;
                            float iy = joinTopx + 3.8500001F;
                            if (!IconTtfBridge.tryDrawIconsFontChar(context, 'x', ix, iy, 6.2999997F, ic)) {
                                drawTextScaled(context, "→", ix + 1.0F, iy + 1.0F, ic, false, 0.18F);
                            }
                        }

                        if (++col >= 2) {
                            col = 0;
                            drawY += 40.0F;
                        }
                    }

                    if (col != 0) {
                        drawY += 40.0F;
                    }

                    context.disableScissor();
                    int totalHx = Math.max(0, (int)Math.ceil(drawY - contentTop - this.scroll) + 4);
                    this.lastContentTop = contentTop;
                    this.lastContentH = contentH;
                    this.lastTotalH = totalHx;
                    int maxScrx = Math.max(0, totalHx - contentH);
                    if (this.scrollTarget < -maxScrx) {
                        this.scrollTarget = -maxScrx;
                    }

                    if (this.scrollTarget > 0.0F) {
                        this.scrollTarget = 0.0F;
                    }

                    this.drawThemedScrollbar(context, cx + cw + 2, contentTop, contentH, totalHx, alphaInt);
                }
            } else {
                this.drawWrapped(context, err, cx, contentTop + 8, cw, 0.34F, new Color(255, 140, 140, alphaInt).getRGB());
            }
        }
    }

    void drawThemedScrollbar(DrawContext context, int x, int y, int h, int totalH, int alphaInt) {
        if (totalH > h) {
            float barH = Math.max(18.0F, h * ((float)h / totalH));
            float range = totalH - h;
            float frac = range > 0.0F ? -this.scroll / range : 0.0F;
            float barY = y + frac * (h - barH);
            int track = applyAlpha(ClickGui.applyBackgroundOpacity(ThemeManager.INSTANCE.getCurrentTheme().getPanelColor()), 0.35F * (alphaInt / 255.0F));
            int thumb = applyAlpha(0xFF000000 | ThemeManager.INSTANCE.getCurrentTheme().getPrimaryColor() & 16777215, alphaInt / 255.0F);
            RenderUtil.drawRoundedRect(context, x, y, 3.0F, h, 1.5F, track);
            RenderUtil.drawRoundedRect(context, x, (int)barY, 3.0F, (int)barH, 1.5F, thumb);
        }
    }

    boolean hitScrollbar(double mouseX, double mouseY) {
        int cx = this.panelLeft + 10;
        int cw = this.panelW - 20;
        return this.lastTotalH > this.lastContentH
            && mouseX >= cx + cw - 2
            && mouseX <= cx + cw + 9
            && mouseY >= this.lastContentTop
            && mouseY <= this.lastContentTop + this.lastContentH;
    }

    void updateDraggedScroll(double mouseY) {
        if (this.lastTotalH > this.lastContentH) {
            float barH = Math.max(18.0F, this.lastContentH * ((float)this.lastContentH / this.lastTotalH));
            float range = this.lastTotalH - this.lastContentH;
            float frac = (float)((mouseY - this.lastContentTop - barH / 2.0F) / Math.max(1.0F, this.lastContentH - barH));
            frac = Math.max(0.0F, Math.min(1.0F, frac));
            this.scrollTarget = -range * frac;
            this.scroll = this.scrollTarget;
        }
    }

    void drawPanelTabs(DrawContext context, int mouseX, int mouseY, int alphaInt, int prim) {
        float scale = 0.36F;
        int gap = 8;
        String[] lbl = new String[]{"Ивенты", "Шахты"};
        FunTimeEventsScreen$PanelTab[] tabs = new FunTimeEventsScreen$PanelTab[]{FunTimeEventsScreen$PanelTab.EVENTS, FunTimeEventsScreen$PanelTab.MINES};
        int totalW = 0;

        for (String s : lbl) {
            totalW += (int)FontManager.getScaledTextWidth(s, scale) + 18 + gap;
        }

        totalW -= gap;
        int tx = this.panelLeft + (this.panelW - totalW) / 2;
        int ty = this.panelTop + 8;
        int th = 20;

        for (int i = 0; i < 2; i++) {
            int tw = (int)FontManager.getScaledTextWidth(lbl[i], scale) + 18;
            boolean sel = this.panelTab == tabs[i];
            boolean hover = mouseX >= tx && mouseX <= tx + tw && mouseY >= ty && mouseY <= ty + th;
            FunTimeEventsScreen$HitRect hr = this.panelTabHits[i];
            hr.x = tx;
            hr.y = ty;
            hr.w = tw;
            hr.h = th;
            int base = sel ? 0xFF000000 | prim & 16777215 : -14013902;
            if (hover && !sel) {
                base = ColorUtil.brighten(base, 0.08F);
            }

            RenderUtil.drawRoundedRect(context, tx, ty, tw, th, 5.0F, applyAlpha(base, alphaInt / 255.0F));
            float twt = FontManager.getScaledTextWidth(lbl[i], scale);
            drawTextScaled(context, lbl[i], tx + (tw - twt) / 2.0F, ty + 4, new Color(255, 255, 255, alphaInt).getRGB(), false, scale);
            tx += tw + gap;
        }
    }

    void drawMineCardTitleLine(DrawContext context, FunTimeEventsClient$FunTimeMine m, float lineX, float lineY, int alpha) {
        float scale = 0.36F;
        String rLabel = m.mineRarity.displayLabel();
        int cR = mineRarityRgb(m.mineRarity, alpha);
        drawTextScaled(context, rLabel, lineX, lineY, cR, false, scale);
    }

    static int mineRarityRgb(FunTimeEventsClient$MineRarityKind r, int alpha) {
        return switch (r) {
            case LEGENDARY -> new Color(255, 220, 70, alpha).getRGB();
            case MYTHICAL -> new Color(200, 120, 255, alpha).getRGB();
            case NORMAL -> new Color(255, 255, 255, alpha).getRGB();
        };
    }

    void drawMineCardSecondLine(DrawContext context, FunTimeEventsClient$FunTimeMine m, float lineX, float lineY, int alpha) {
        int cSep = new Color(160, 160, 170, alpha).getRGB();
        int cRed = new Color(255, 100, 100, alpha).getRGB();
        int cGreen = new Color(80, 220, 120, alpha).getRGB();
        int cYellow = new Color(255, 220, 70, alpha).getRGB();
        String server = m.getDisplayServer();
        String time = m.getDisplayTime();
        String phase = m.isDoIEvintaTitle() ? "" : m.getDisplayPhaseLabel();
        drawTextScaled(context, server, lineX, lineY, cRed, false, 0.3F);
        float cx = lineX + FontManager.getScaledTextWidth(server, 0.3F);
        String dot = "  ";
        drawTextScaled(context, dot, cx, lineY, cSep, false, 0.3F);
        cx += FontManager.getScaledTextWidth(dot, 0.3F);
        drawTextScaled(context, time, cx, lineY, cGreen, false, 0.3F);
        cx += FontManager.getScaledTextWidth(time, 0.3F);
        if (phase != null && !phase.isEmpty()) {
            drawTextScaled(context, dot, cx, lineY, cSep, false, 0.3F);
            cx += FontManager.getScaledTextWidth(dot, 0.3F);
            drawTextScaled(context, phase, cx, lineY, cYellow, false, 0.3F);
        }
    }

    void drawWrapped(DrawContext context, String text, int x, int y, int maxW, float scale, int color) {
        if (text != null) {
            String[] words = text.split(" ");
            StringBuilder line = new StringBuilder();
            float ly = y;

            for (String w : words) {
                String tryLine = line.length() == 0 ? w : line + " " + w;
                if (FontManager.getScaledTextWidth(tryLine, scale) > maxW && line.length() > 0) {
                    drawTextScaled(context, line.toString(), x, ly, color, false, scale);
                    ly += 10.0F;
                    line = new StringBuilder(w);
                } else {
                    line = new StringBuilder(tryLine);
                }
            }

            if (line.length() > 0) {
                drawTextScaled(context, line.toString(), x, ly, color, false, scale);
            }
        }
    }

    static int applyAlpha(int rgb, float a) {
        int al = Math.max(0, Math.min(255, Math.round((rgb >>> 24 & 0xFF) * a)));
        return al << 24 | rgb & 16777215;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 1 && this.hitScrollbar(mouseX, mouseY)) {
            this.draggingScrollBar = true;
            this.updateDraggedScroll(mouseY);
            return true;
        }

        if (button != 0) {
            return super.mouseClicked(click, doubled);
        }

        int closeX = this.panelLeft + this.panelW - 22;
        int closeY = this.panelTop + 8;
        if (mouseX >= closeX && mouseX <= closeX + 16 && mouseY >= closeY && mouseY <= closeY + 14) {
            this.closing = true;
            return true;
        }

        int btnX = this.panelLeft + 14;
        int btnY = this.panelTop + 36;
        int btnW = 78;
        int btnH = 18;
        FunTimeEventsScreen$PanelTab[] tabs = new FunTimeEventsScreen$PanelTab[]{FunTimeEventsScreen$PanelTab.EVENTS, FunTimeEventsScreen$PanelTab.MINES};

        for (int i = 0; i < 2; i++) {
            FunTimeEventsScreen$HitRect tr = this.panelTabHits[i];
            if (mouseX >= tr.x && mouseX <= tr.x + tr.w && mouseY >= tr.y && mouseY <= tr.y + tr.h) {
                if (this.panelTab != tabs[i]) {
                    this.panelTab = tabs[i];
                    this.rememberUiState();
                    this.scrollTarget = this.scroll = 0.0F;
                    if (this.panelTab == FunTimeEventsScreen$PanelTab.MINES) {
                        this.loadingMines = true;
                        FunTimeEventsClient.refreshMinesAsync(() -> this.loadingMines = false);
                    }
                }

                return true;
            }
        }

        boolean loading = this.panelTab == FunTimeEventsScreen$PanelTab.EVENTS ? this.loadingEvents : this.loadingMines;
        if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH && !loading) {
            if (this.panelTab == FunTimeEventsScreen$PanelTab.EVENTS) {
                this.loadingEvents = true;
                FunTimeEventsClient.refreshAsync(() -> this.loadingEvents = false);
            } else {
                this.loadingMines = true;
                FunTimeEventsClient.refreshMinesAsync(() -> this.loadingMines = false);
            }

            return true;
        } else {
            FunTimeEventsScreen$ListMode[] modeOrd = new FunTimeEventsScreen$ListMode[]{
                FunTimeEventsScreen$ListMode.ACTIVE, FunTimeEventsScreen$ListMode.NEAREST, FunTimeEventsScreen$ListMode.ALL
            };
            FunTimeEventsScreen$MineListMode[] mineModeOrd = new FunTimeEventsScreen$MineListMode[]{
                FunTimeEventsScreen$MineListMode.LEGENDARY, FunTimeEventsScreen$MineListMode.MYTHICAL, FunTimeEventsScreen$MineListMode.ALL
            };
            if (this.panelTab == FunTimeEventsScreen$PanelTab.EVENTS) {
                for (int ix = 0; ix < 3; ix++) {
                    FunTimeEventsScreen$HitRect r = this.modeHits[ix];
                    if (mouseX >= r.x && mouseX <= r.x + r.w && mouseY >= r.y && mouseY <= r.y + r.h) {
                        if (this.listMode != modeOrd[ix]) {
                            this.listMode = modeOrd[ix];
                            this.rememberUiState();
                            this.scrollTarget = this.scroll = 0.0F;
                        }

                        return true;
                    }
                }
            } else if (this.panelTab == FunTimeEventsScreen$PanelTab.MINES) {
                for (int ixx = 0; ixx < 3; ixx++) {
                    FunTimeEventsScreen$HitRect r = this.modeHits[ixx];
                    if (mouseX >= r.x && mouseX <= r.x + r.w && mouseY >= r.y && mouseY <= r.y + r.h) {
                        if (this.mineListMode != mineModeOrd[ixx]) {
                            this.mineListMode = mineModeOrd[ixx];
                            this.rememberUiState();
                            this.scrollTarget = this.scroll = 0.0F;
                        }

                        return true;
                    }
                }
            }

            for (int ixxx = 0; ixxx < 2; ixxx++) {
                FunTimeEventsScreen$HitRect r = this.verHits[ixxx];
                if (mouseX >= r.x && mouseX <= r.x + r.w && mouseY >= r.y && mouseY <= r.y + r.h) {
                    this.toggleVersionFilter(ixxx);
                    return true;
                }
            }

            int joinAid = this.panelTab == FunTimeEventsScreen$PanelTab.EVENTS
                ? this.hitTestJoinButton(mouseX, mouseY)
                : this.hitTestJoinMineButton(mouseX, mouseY);
            if (joinAid > 0) {
                if (this.client != null && this.client.getNetworkHandler() != null) {
                    this.client.getNetworkHandler().sendChatCommand("an" + joinAid);
                }

                this.closing = true;
                return true;
            } else {
                return super.mouseClicked(click, doubled);
            }
        }
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (this.draggingScrollBar && button == 1) {
            this.updateDraggedScroll(mouseY);
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
        if (button == 1) {
            this.draggingScrollBar = false;
        }

        return super.mouseReleased(click);
    }

    void drawEventCardSecondLine(DrawContext context, FunTimeEventsClient$FunTimeEvent ev, float lineX, float lineY, int alpha) {
        int cSep = new Color(160, 160, 170, alpha).getRGB();
        int cRed = new Color(255, 100, 100, alpha).getRGB();
        int cGreen = new Color(80, 220, 120, alpha).getRGB();
        int cYellow = new Color(255, 220, 70, alpha).getRGB();
        String server = ev.getDisplayServer();
        String time = ev.getDisplayTime();
        String phase = ev.isDoIEvintaTitle() ? "" : ev.getDisplayPhaseLabel();
        drawTextScaled(context, server, lineX, lineY, cRed, false, 0.3F);
        float cx = lineX + FontManager.getScaledTextWidth(server, 0.3F);
        String dot = "  ";
        drawTextScaled(context, dot, cx, lineY, cSep, false, 0.3F);
        cx += FontManager.getScaledTextWidth(dot, 0.3F);
        drawTextScaled(context, time, cx, lineY, cGreen, false, 0.3F);
        cx += FontManager.getScaledTextWidth(time, 0.3F);
        if (phase != null && !phase.isEmpty()) {
            drawTextScaled(context, dot, cx, lineY, cSep, false, 0.3F);
            cx += FontManager.getScaledTextWidth(dot, 0.3F);
            drawTextScaled(context, phase, cx, lineY, cYellow, false, 0.3F);
        }
    }

    int hitTestJoinButton(double mouseX, double mouseY) {
        int contentTop = this.panelTop + 64;
        int contentH = this.panelH - 76;
        int cx = this.panelLeft + 10;
        int cw = this.panelW - 20;
        if (!(mouseX < cx) && !(mouseX > cx + cw) && !(mouseY < contentTop) && !(mouseY > contentTop + contentH)) {
            List<FunTimeEventsClient$FunTimeEvent> events = this.applyFilters(FunTimeEventsClient.getEvents());
            int gap = 8;
            int cardH = 36;
            int rowH = 40;
            int cardW = Math.max(120, (cw - gap) / 2);
            float drawY = contentTop + this.scroll;
            int col = 0;

            for (FunTimeEventsClient$FunTimeEvent ev : events) {
                if (ev.anarchyId > 0) {
                    int x = cx + col * (cardW + gap);
                    float joinLeft = x + cardW - 6 - 14;
                    int joinTop = (int)drawY + (cardH - 14) / 2;
                    if (mouseX >= joinLeft && mouseX <= joinLeft + 14.0F && mouseY >= joinTop && mouseY <= joinTop + 14) {
                        return ev.anarchyId;
                    }
                }

                if (++col >= 2) {
                    col = 0;
                    drawY += rowH;
                }
            }

            return 0;
        } else {
            return 0;
        }
    }

    int hitTestJoinMineButton(double mouseX, double mouseY) {
        int contentTop = this.panelTop + 64;
        int contentH = this.panelH - 76;
        int cx = this.panelLeft + 10;
        int cw = this.panelW - 20;
        if (!(mouseX < cx) && !(mouseX > cx + cw) && !(mouseY < contentTop) && !(mouseY > contentTop + contentH)) {
            List<FunTimeEventsClient$FunTimeMine> mines = this.displayedMines(FunTimeEventsClient.getMines());
            int gap = 8;
            int cardH = 36;
            int rowH = 40;
            int cardW = Math.max(120, (cw - gap) / 2);
            float drawY = contentTop + this.scroll;
            int col = 0;

            for (FunTimeEventsClient$FunTimeMine m : mines) {
                if (m.anarchyId > 0) {
                    int x = cx + col * (cardW + gap);
                    float joinLeft = x + cardW - 6 - 14;
                    int joinTop = (int)drawY + (cardH - 14) / 2;
                    if (mouseX >= joinLeft && mouseX <= joinLeft + 14.0F && mouseY >= joinTop && mouseY <= joinTop + 14) {
                        return m.anarchyId;
                    }
                }

                if (++col >= 2) {
                    col = 0;
                    drawY += rowH;
                }
            }

            return 0;
        } else {
            return 0;
        }
    }

    int modeToolbarWidth() {
        int gap = 6;
        float scale = 0.32F;
        int w = 0;

        for (String s : new String[]{"Активные", "Ближайшие", "Все"}) {
            w += (int)FontManager.getScaledTextWidth(s, scale) + 14 + gap;
        }

        return w - gap;
    }

    int mineModeToolbarWidth() {
        int gap = 6;
        float scale = 0.32F;
        int w = 0;

        for (String s : new String[]{"Легендарные", "Мифические", "Все"}) {
            w += (int)FontManager.getScaledTextWidth(s, scale) + 14 + gap;
        }

        return w - gap;
    }

    int modeFilterToolbarWidth() {
        return this.panelTab == FunTimeEventsScreen$PanelTab.EVENTS ? this.modeToolbarWidth() : this.mineModeToolbarWidth();
    }

    static void drawTextScaled(DrawContext context, String text, float x, float y, int color, boolean shadow, float scale) {
        FontManager.drawCustomTextScaled(context, text, x, y + -2.0F, color, shadow, scale);
    }

    int versionToolbarWidth() {
        int gap = 6;
        float scale = 0.32F;
        int w = 0;

        for (String s : new String[]{"1.16", "1.21"}) {
            w += (int)FontManager.getScaledTextWidth(s, scale) + 14 + gap;
        }

        return w - gap;
    }

    int versionToolbarStartX() {
        int marginRight = 36;
        return this.panelLeft + this.panelW - marginRight - this.versionToolbarWidth();
    }

    int filterModeToolbarStartX() {
        int leftMin = this.panelLeft + 14 + 78 + 10;
        int verStart = this.versionToolbarStartX();
        int avail = Math.max(0, verStart - 8 - leftMin);
        int mw = this.modeFilterToolbarWidth();
        return leftMin + Math.max(0, (avail - mw) / 2);
    }

    void drawFilterToolbar(DrawContext context, int mouseX, int mouseY, int alphaInt, int prim, int tx, int ty, int th) {
        int gap = 6;
        float scale = 0.32F;
        String[] modeLbl = new String[]{"Активные", "Ближайшие", "Все"};
        FunTimeEventsScreen$ListMode[] modeOrd = new FunTimeEventsScreen$ListMode[]{
            FunTimeEventsScreen$ListMode.ACTIVE, FunTimeEventsScreen$ListMode.NEAREST, FunTimeEventsScreen$ListMode.ALL
        };

        for (int i = 0; i < 3; i++) {
            int tw = (int)FontManager.getScaledTextWidth(modeLbl[i], scale) + 14;
            boolean sel = this.listMode == modeOrd[i];
            boolean hover = mouseX >= tx && mouseX <= tx + tw && mouseY >= ty && mouseY <= ty + th;
            FunTimeEventsScreen$HitRect hr = this.modeHits[i];
            hr.x = tx;
            hr.y = ty;
            hr.w = tw;
            hr.h = th;
            int base = sel ? 0xFF000000 | prim & 16777215 : -14013902;
            if (hover && !sel) {
                base = ColorUtil.brighten(base, 0.08F);
            }

            RenderUtil.drawRoundedRect(context, tx, ty, tw, th, 4.0F, applyAlpha(base, alphaInt / 255.0F));
            float twt = FontManager.getScaledTextWidth(modeLbl[i], scale);
            drawTextScaled(context, modeLbl[i], tx + (tw - twt) / 2.0F, ty + 4, new Color(255, 255, 255, alphaInt).getRGB(), false, scale);
            tx += tw + gap;
        }
    }

    void drawMineFilterToolbar(DrawContext context, int mouseX, int mouseY, int alphaInt, int prim, int tx, int ty, int th) {
        int gap = 6;
        float scale = 0.32F;
        String[] modeLbl = new String[]{"Легендарные", "Мифические", "Все"};
        FunTimeEventsScreen$MineListMode[] modeOrd = new FunTimeEventsScreen$MineListMode[]{
            FunTimeEventsScreen$MineListMode.LEGENDARY, FunTimeEventsScreen$MineListMode.MYTHICAL, FunTimeEventsScreen$MineListMode.ALL
        };

        for (int i = 0; i < 3; i++) {
            int tw = (int)FontManager.getScaledTextWidth(modeLbl[i], scale) + 14;
            boolean sel = this.mineListMode == modeOrd[i];
            boolean hover = mouseX >= tx && mouseX <= tx + tw && mouseY >= ty && mouseY <= ty + th;
            FunTimeEventsScreen$HitRect hr = this.modeHits[i];
            hr.x = tx;
            hr.y = ty;
            hr.w = tw;
            hr.h = th;
            int base = sel ? 0xFF000000 | prim & 16777215 : -14013902;
            if (hover && !sel) {
                base = ColorUtil.brighten(base, 0.08F);
            }

            RenderUtil.drawRoundedRect(context, tx, ty, tw, th, 4.0F, applyAlpha(base, alphaInt / 255.0F));
            float twt = FontManager.getScaledTextWidth(modeLbl[i], scale);
            drawTextScaled(context, modeLbl[i], tx + (tw - twt) / 2.0F, ty + 4, new Color(255, 255, 255, alphaInt).getRGB(), false, scale);
            tx += tw + gap;
        }
    }

    void drawVersionToolbar(DrawContext context, int mouseX, int mouseY, int alphaInt, int prim, int ty, int th) {
        int gap = 6;
        float scale = 0.32F;
        int tx = this.versionToolbarStartX();
        String[] verLbl = new String[]{"1.16", "1.21"};
        boolean[] verSel = new boolean[]{this.filterAnarchy4Digits, this.filterAnarchy3Digits};

        for (int i = 0; i < 2; i++) {
            int tw = (int)FontManager.getScaledTextWidth(verLbl[i], scale) + 14;
            boolean sel = verSel[i];
            boolean hover = mouseX >= tx && mouseX <= tx + tw && mouseY >= ty && mouseY <= ty + th;
            FunTimeEventsScreen$HitRect hr = this.verHits[i];
            hr.x = tx;
            hr.y = ty;
            hr.w = tw;
            hr.h = th;
            int base = sel ? 0xFF000000 | prim & 16777215 : -14013902;
            if (hover && !sel) {
                base = ColorUtil.brighten(base, 0.08F);
            }

            RenderUtil.drawRoundedRect(context, tx, ty, tw, th, 4.0F, applyAlpha(base, alphaInt / 255.0F));
            float twt = FontManager.getScaledTextWidth(verLbl[i], scale);
            drawTextScaled(context, verLbl[i], tx + (tw - twt) / 2.0F, ty + 4, new Color(255, 255, 255, alphaInt).getRGB(), false, scale);
            tx += tw + gap;
        }
    }

    void toggleVersionFilter(int index) {
        if (index == 0) {
            this.filterAnarchy4Digits = true;
            this.filterAnarchy3Digits = false;
        } else {
            this.filterAnarchy4Digits = false;
            this.filterAnarchy3Digits = true;
        }

        this.rememberUiState();
        this.scrollTarget = this.scroll = 0.0F;
    }

    List<FunTimeEventsClient$FunTimeEvent> applyFilters(List<FunTimeEventsClient$FunTimeEvent> src) {
        List<FunTimeEventsClient$FunTimeEvent> out = new ArrayList<>();

        for (FunTimeEventsClient$FunTimeEvent ev : src) {
            if (!FunTimeEventsClient.isHiddenEventId(ev.id) && this.passesVersionDigits(ev.anarchyNumberDigitCount())) {
                switch (this.listMode) {
                    case ACTIVE:
                        if (ev.matchesActiveListFilter()) {
                            out.add(ev);
                        }
                        break;
                    case NEAREST:
                        if (ev.isDoIEvintaTitle()) {
                            out.add(ev);
                        }
                        break;
                    case ALL:
                        out.add(ev);
                }
            }
        }

        return out;
    }

    void rememberUiState() {
        rememberedPanelTab = this.panelTab;
        rememberedListMode = this.listMode;
        rememberedMineListMode = this.mineListMode;
        rememberedFilterAnarchy4Digits = this.filterAnarchy4Digits;
        rememberedFilterAnarchy3Digits = this.filterAnarchy3Digits;
    }

    List<FunTimeEventsClient$FunTimeMine> displayedMines(List<FunTimeEventsClient$FunTimeMine> src) {
        List<FunTimeEventsClient$FunTimeMine> out = new ArrayList<>();

        for (FunTimeEventsClient$FunTimeMine m : src) {
            if (this.passesVersionDigits(m.anarchyNumberDigitCount())) {
                switch (this.mineListMode) {
                    case LEGENDARY:
                        if (m.mineRarity == FunTimeEventsClient$MineRarityKind.LEGENDARY) {
                            out.add(m);
                        }
                        break;
                    case MYTHICAL:
                        if (m.mineRarity == FunTimeEventsClient$MineRarityKind.MYTHICAL) {
                            out.add(m);
                        }
                        break;
                    case ALL:
                        out.add(m);
                }
            }
        }

        out.sort(Comparator.<FunTimeEventsClient$FunTimeMine>comparingInt(mx -> mx.mineRarity.listGroupOrder()).thenComparingInt(mx -> mx.timeSecondsLeft));
        return out;
    }

    boolean passesVersionDigits(int digitCount) {
        if (digitCount == 0) {
            return false;
        } else {
            return this.filterAnarchy4Digits && digitCount == 4 ? true : this.filterAnarchy3Digits && digitCount == 3;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int contentTop = this.panelTop + 64;
        int contentH = this.panelH - 76;
        int cx = this.panelLeft + 10;
        int cw = this.panelW - 20;
        if (mouseX >= cx && mouseX <= cx + cw && mouseY >= contentTop && mouseY <= contentTop + contentH) {
            this.scrollTarget = (float)(this.scrollTarget + verticalAmount * 28.0);
            return true;
        } else {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();
        if (keyCode == 256) {
            this.closing = true;
            return true;
        } else {
            return super.keyPressed(input);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
