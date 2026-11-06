package com.example.auravisuals.client.gui;

import com.example.auravisuals.account.AccountManager;
import com.example.auravisuals.account.AccountManager$Account;
import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.client.gui.font.icon.IconTtfBridge;
import com.example.auravisuals.util.render.RenderUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class AuraAccountsScreen extends Screen {
    static final Identifier BACKGROUND = Identifier.of("auravisuals", "textures/mainmenu/background.png");
    static final int BACKGROUND_W = 1672;
    static final int BACKGROUND_H = 941;
    static final int RED = -1824962;
    static final int YELLOW = -10128;
    final Screen parent;
    final Map<String, AuraAccountsScreen$CardState> cardStates = new HashMap<>();
    final Set<String> removingNames = new HashSet<>();
    String nicknameInput = "";
    String searchInput = "";
    boolean nicknameFocused;
    boolean searchFocused;
    boolean sortByDate = true;
    boolean closing;
    boolean clearWhenAnimationsDone;
    float accountScroll;
    float accountTargetScroll;
    float openAnim;
    long lastFrameTime;
    long lastClearClick;

    public AuraAccountsScreen(Screen parent) {
        super(Text.literal("anvil visual — Accounts"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        AccountManager.ensureLoaded();
        this.openAnim = 0.0F;
        this.closing = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        float dt = this.frameDelta();
        this.openAnim = this.approach(this.openAnim, this.closing ? 0.0F : 1.0F, dt, this.closing ? 15.0F : 12.0F);
        if (!this.closing || !(this.openAnim <= 0.025F)) {
            this.renderMenuBackground(context);
            context.fill(0, 0, this.width, this.height, this.withUiAlpha(1979711488, 1.0F));
            AuraAccountsScreen$Layout layout = this.layout();
            float eased = this.easeOut(this.openAnim);
            context.getMatrices().pushMatrix();

            try {
                context.getMatrices().translate(0.0F, (1.0F - eased) * 9.0F);
                this.renderTitle(context, layout);
                this.renderTopControls(context, layout, mouseX, mouseY);
                this.renderAccountsPanel(context, layout, mouseX, mouseY, dt);
                this.renderBottomControls(context, layout, mouseX, mouseY);
            } finally {
                context.getMatrices().popMatrix();
            }
        } else if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (!this.closing && !(this.openAnim < 0.7F) && button == 0) {
            AuraAccountsScreen$Layout layout = this.layout();
            this.nicknameFocused = this.contains(mouseX, mouseY, layout.nickX, layout.topY, layout.nickW, layout.controlH);
            this.searchFocused = this.contains(mouseX, mouseY, layout.searchX, layout.bottomY, layout.searchW, layout.controlH);
            if (this.contains(mouseX, mouseY, layout.nickX + layout.nickW - layout.controlH, layout.topY, layout.controlH, layout.controlH)) {
                this.addTypedAccount();
                this.nicknameFocused = true;
                return true;
            }

            if (this.contains(mouseX, mouseY, layout.randomX, layout.topY, layout.randomW, layout.controlH)) {
                AccountManager.addRandomAccount();
                return true;
            }

            if (this.contains(mouseX, mouseY, layout.exitX, layout.topY, layout.exitW, layout.controlH)) {
                this.close();
                return true;
            }

            if (this.contains(mouseX, mouseY, layout.sortX, layout.bottomY, layout.sortW, layout.controlH)) {
                this.sortByDate = !this.sortByDate;
                this.accountTargetScroll = 0.0F;
                return true;
            }

            if (this.contains(mouseX, mouseY, layout.clearX, layout.bottomY, layout.clearW, layout.controlH)) {
                long now = System.currentTimeMillis();
                if (now - this.lastClearClick <= 650L) {
                    this.startClearAll();
                }

                this.lastClearClick = now;
                return true;
            } else if (this.contains(mouseX, mouseY, layout.panelX, layout.panelY, layout.panelW, layout.panelH)) {
                if (this.clickAccountList(mouseX, mouseY, layout)) {
                    this.nicknameFocused = false;
                    this.searchFocused = false;
                }

                return true;
            } else {
                return super.mouseClicked(click, doubled);
            }
        } else {
            return super.mouseClicked(click, doubled);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        AuraAccountsScreen$Layout layout = this.layout();
        if (this.contains(mouseX, mouseY, layout.panelX, layout.panelY, layout.panelW, layout.panelH)) {
            float max = this.accountMaxScroll(layout);
            if (max > 0.0F) {
                this.accountTargetScroll = MathHelper.clamp(this.accountTargetScroll - (float)verticalAmount * 28.0F, 0.0F, max);
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        int scanCode = input.scancode();
        int modifiers = input.modifiers();
        if (keyCode == 256) {
            this.close();
            return true;
        }

        if (this.nicknameFocused) {
            if (keyCode == 259 && !this.nicknameInput.isEmpty()) {
                this.nicknameInput = this.nicknameInput.substring(0, this.nicknameInput.length() - 1);
                return true;
            }

            if (keyCode == 257 || keyCode == 335) {
                this.addTypedAccount();
                return true;
            }
        }

        if (this.searchFocused && keyCode == 259 && !this.searchInput.isEmpty()) {
            this.searchInput = this.searchInput.substring(0, this.searchInput.length() - 1);
            this.accountTargetScroll = 0.0F;
            return true;
        } else {
            return super.keyPressed(input);
        }
    }

    @Override
    public boolean charTyped(CharInput input) {
        char chr = (char)input.codepoint();
        int modifiers = input.modifiers();
        if (this.nicknameFocused) {
            if (this.isNicknameChar(chr) && this.nicknameInput.length() < 16) {
                this.nicknameInput = this.nicknameInput + chr;
                return true;
            } else {
                return false;
            }
        } else if (this.searchFocused) {
            if (this.isNicknameChar(chr) && this.searchInput.length() < 32) {
                this.searchInput = this.searchInput + chr;
                this.accountTargetScroll = 0.0F;
                return true;
            } else {
                return false;
            }
        } else {
            return super.charTyped(input);
        }
    }

    @Override
    public void close() {
        this.nicknameFocused = false;
        this.searchFocused = false;
        this.closing = true;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    void renderTopControls(DrawContext context, AuraAccountsScreen$Layout layout, int mouseX, int mouseY) {
        this.drawInput(
            context, layout.nickX, layout.topY, layout.nickW, layout.controlH, this.nicknameInput, "Enter nickname", this.nicknameFocused, '-', mouseX, mouseY
        );
        this.drawActionButton(context, layout.randomX, layout.topY, layout.randomW, layout.controlH, "Random", '>', mouseX, mouseY);
        this.drawActionButton(context, layout.exitX, layout.topY, layout.exitW, layout.controlH, "Exit", '\\', mouseX, mouseY);
    }

    void renderBottomControls(DrawContext context, AuraAccountsScreen$Layout layout, int mouseX, int mouseY) {
        this.drawInput(
            context, layout.searchX, layout.bottomY, layout.searchW, layout.controlH, this.searchInput, "Search", this.searchFocused, '/', mouseX, mouseY
        );
        this.drawActionButton(context, layout.sortX, layout.bottomY, layout.sortW, layout.controlH, this.sortByDate ? "Date" : "Alphabet", ',', mouseX, mouseY);
        this.drawActionButton(context, layout.clearX, layout.bottomY, layout.clearW, layout.controlH, "Clear all", '.', mouseX, mouseY);
    }

    void renderTitle(DrawContext context, AuraAccountsScreen$Layout layout) {
        String title = "ACCOUNTS";
        float scale = 0.82F;
        float letterSpacing = 2.5F;
        float totalW = FontManager.getScaledTextWidth(title, scale) + letterSpacing * (title.length() - 1);
        float x = layout.panelX + (layout.panelW - totalW) * 0.5F;
        float y = layout.titleY;
        float phase = (float)(System.currentTimeMillis() % 5820L) / 5820.0F;
        float breathe = (float)((Math.sin(phase * Math.PI * 2.0) + 1.0) * 0.5);
        float span = title.length() + 5.0F;
        float waveA = phase * span - 2.2F;
        float waveB = (phase + 0.48F) % 1.0F * span - 2.2F;
        this.drawTitleLetters(context, title, x + 1.0F, y + 1.4F, scale, letterSpacing, this.withUiAlpha(-1728053248, 0.68F));
        float cursor = x;

        for (int i = 0; i < title.length(); i++) {
            String ch = title.substring(i, i + 1);
            float letterT = title.length() <= 1 ? 0.0F : (float)i / (title.length() - 1);
            float influenceA = this.gaussian(i, waveA, 1.75F);
            float influenceB = this.gaussian(i, waveB, 1.45F) * 0.78F;
            float shimmer = MathHelper.clamp(influenceA + influenceB, 0.0F, 1.0F);
            int baseColor = this.blendColor(-13630713, -9301990, 0.24F + letterT * 0.18F + breathe * 0.05F);
            int color = this.blendColor(baseColor, -3660747, shimmer * 0.72F);
            FontManager.drawCustomTextScaled(context, ch, cursor, y, this.withUiAlpha(color, 1.0F), false, scale);
            cursor += FontManager.getScaledTextWidth(ch, scale) + letterSpacing;
        }
    }

    float gaussian(float value, float center, float width) {
        float d = (value - center) / Math.max(0.001F, width);
        return (float)Math.exp(-d * d * 0.5F);
    }

    void drawTitleLetters(DrawContext context, String title, float x, float y, float scale, float letterSpacing, int color) {
        float cursor = x;

        for (int i = 0; i < title.length(); i++) {
            String ch = title.substring(i, i + 1);
            FontManager.drawCustomTextScaled(context, ch, cursor, y, color, false, scale);
            cursor += FontManager.getScaledTextWidth(ch, scale) + letterSpacing;
        }
    }

    void renderAccountsPanel(DrawContext context, AuraAccountsScreen$Layout layout, int mouseX, int mouseY, float dt) {
        float radius = 8.0F;
        this.accountTargetScroll = MathHelper.clamp(this.accountTargetScroll, 0.0F, this.accountMaxScroll(layout));
        this.accountScroll = this.approach(this.accountScroll, this.accountTargetScroll, dt, 12.0F);
        RenderUtil.drawRoundedBlur(context, layout.panelX, layout.panelY, layout.panelW, layout.panelH, radius, 4);
        this.drawSoftFrame(
            context,
            layout.panelX,
            layout.panelY,
            layout.panelW,
            layout.panelH,
            radius,
            this.withUiAlpha(new Color(6, 6, 10, 188).getRGB(), 1.0F),
            this.withAlpha(-1824962, 128)
        );
        int innerX = layout.panelX + layout.pad;
        int innerY = layout.panelY + layout.pad;
        int innerW = layout.panelW - layout.pad * 2;
        int innerH = layout.panelH - layout.pad * 2;
        List<AccountManager$Account> accounts = this.visibleAccounts();
        this.updateCardStates(accounts, layout, innerX, innerY, dt);
        context.enableScissor(innerX, innerY, innerX + innerW, innerY + innerH);

        try {
            if (accounts.isEmpty() && this.noVisibleCardStates()) {
                this.drawEmptyState(context, innerX, innerY, innerW, innerH);
            } else {
                List<AuraAccountsScreen$CardState> drawStates = new ArrayList<>(this.cardStates.values());
                drawStates.sort(Comparator.comparingDouble(s -> s.displayY));

                for (AuraAccountsScreen$CardState state : drawStates) {
                    if (!(state.alpha <= 0.01F) && state.displayY + layout.cardH >= innerY && state.displayY <= innerY + innerH) {
                        this.renderAccountCard(context, state, state.displayX, state.displayY, layout.cardW, layout.cardH, mouseX, mouseY);
                    }
                }
            }
        } finally {
            context.disableScissor();
        }

        this.finishDeferredRemoval();
        this.renderScrollbar(context, layout);
    }

    void updateCardStates(List<AccountManager$Account> accounts, AuraAccountsScreen$Layout layout, int innerX, int innerY, float dt) {
        Set<String> active = new HashSet<>();

        for (int i = 0; i < accounts.size(); i++) {
            AccountManager$Account account = accounts.get(i);
            String key = account.getNickname().toLowerCase(Locale.ROOT);
            active.add(key);
            int col = i % 2;
            int row = i / 2;
            float targetX = innerX + col * (layout.cardW + layout.cardGap);
            float targetY = innerY - this.accountScroll + row * (layout.cardH + layout.rowGap);
            AuraAccountsScreen$CardState state = this.cardStates.get(key);
            if (state == null) {
                state = new AuraAccountsScreen$CardState(
                    new AuraAccountsScreen$AccountView(account.getNickname(), account.getCreatedAt(), account.isFavorite()), targetX, targetY + 8.0F
                );
                this.cardStates.put(key, state);
            }

            state.view = new AuraAccountsScreen$AccountView(account.getNickname(), account.getCreatedAt(), account.isFavorite());
            state.targetX = targetX;
            state.targetY = targetY;
            state.removing = false;
            state.alphaTarget = 1.0F;
        }

        List<String> toRemove = new ArrayList<>();

        for (Entry<String, AuraAccountsScreen$CardState> entry : this.cardStates.entrySet()) {
            AuraAccountsScreen$CardState state = entry.getValue();
            if (!active.contains(entry.getKey())) {
                state.removing = true;
                state.alphaTarget = 0.0F;
            }

            state.displayX = this.approach(state.displayX, state.targetX, dt, state.removing ? 11.0F : 16.0F);
            state.displayY = this.approach(state.displayY, state.targetY, dt, state.removing ? 11.0F : 16.0F);
            state.alpha = this.approach(state.alpha, state.alphaTarget, dt, state.removing ? 14.0F : 12.0F);
            if (state.removing && state.alpha <= 0.035F) {
                toRemove.add(entry.getKey());
            }
        }

        for (String key : toRemove) {
            this.cardStates.remove(key);
            this.removingNames.remove(key);
        }
    }

    void renderAccountCard(DrawContext context, AuraAccountsScreen$CardState state, float x, float y, int w, int h, int mouseX, int mouseY) {
        AuraAccountsScreen$AccountView account = state.view;
        boolean hovered = this.contains(mouseX, mouseY, x, y, w, h);
        boolean selected = account.nickname.equalsIgnoreCase(AccountManager.getSelectedName());
        float alpha = state.alpha;
        int fill = selected ? new Color(30, 25, 29, 220).getRGB() : (hovered ? new Color(20, 17, 21, 210).getRGB() : new Color(12, 11, 15, 205).getRGB());
        int border = this.withAlpha(-1824962, Math.round((selected ? 158 : (hovered ? 126 : 70)) * alpha));
        if (selected) {
            RenderUtil.drawRoundedRect(context, x - 0.7F, y - 0.7F, w + 1.4F, h + 1.4F, 7.0F, this.withAlpha(-1824962, Math.round(24.0F * alpha)));
        }

        this.drawSoftFrame(context, x, y, w, h, 6.5F, this.withUiAlpha(fill, alpha), border);
        float face = Math.min(25.0F, h - 12.0F);
        float faceX = x + 8.0F;
        float faceY = y + (h - face) * 0.5F;
        RenderUtil.drawRoundedRect(
            context, faceX - 1.5F, faceY - 1.5F, face + 3.0F, face + 3.0F, 4.0F, this.withUiAlpha(new Color(31, 18, 23, 210).getRGB(), alpha)
        );
        this.drawPlayerFace(context, account.nickname, faceX, faceY, face, alpha);
        float iconBox = 11.3F;
        float iconX = x + w - iconBox - 7.0F;
        float favY = y + 6.0F;
        float delY = favY + 18.5F;
        int favoriteColor = account.favorite ? -10128 : -2697253;
        this.drawIcon(context, (char)(account.favorite ? '=' : '+'), iconX, favY, iconBox, this.withUiAlpha(favoriteColor, alpha));
        this.drawIcon(context, 'g', iconX, delY + 2.0F, iconBox, this.withAlpha(-1824962, Math.round(255.0F * alpha)));
        float textX = faceX + face + 8.0F;
        int textMax = Math.max(18, Math.round(iconX - textX - 5.0F));
        FontManager.drawCustomTextScaled(context, this.fitText(account.nickname, textMax, 0.25F), textX, y + 10.0F, this.withUiAlpha(-1, alpha), false, 0.25F);
        FontManager.drawCustomTextScaled(
            context, AccountManager.formatDate(account.createdAt), textX, y + 27.0F, this.withUiAlpha(-1459617793, alpha), false, 0.198F
        );
    }

    boolean clickAccountList(double mouseX, double mouseY, AuraAccountsScreen$Layout layout) {
        int innerX = layout.panelX + layout.pad;
        int innerY = layout.panelY + layout.pad;
        int innerW = layout.panelW - layout.pad * 2;
        int innerH = layout.panelH - layout.pad * 2;
        if (!this.contains(mouseX, mouseY, innerX, innerY, innerW, innerH)) {
            return false;
        }

        for (AccountManager$Account account : this.visibleAccounts()) {
            String key = account.getNickname().toLowerCase(Locale.ROOT);
            AuraAccountsScreen$CardState state = this.cardStates.get(key);
            if (state != null && !state.removing && this.contains(mouseX, mouseY, state.displayX, state.displayY, layout.cardW, layout.cardH)) {
                float iconBox = 11.3F;
                float iconX = state.displayX + layout.cardW - iconBox - 7.0F;
                float favY = state.displayY + 6.0F;
                float delY = favY + 20.5F;
                if (this.contains(mouseX, mouseY, iconX - 4.0F, favY - 4.0F, iconBox + 8.0F, iconBox + 8.0F)) {
                    AccountManager.toggleFavorite(account);
                    this.accountTargetScroll = 0.0F;
                    return true;
                }

                if (this.contains(mouseX, mouseY, iconX - 4.0F, delY - 4.0F, iconBox + 8.0F, iconBox + 8.0F)) {
                    this.startRemove(account);
                    return true;
                }

                AccountManager.select(account);
                return true;
            }
        }

        return false;
    }

    void startRemove(AccountManager$Account account) {
        String key = account.getNickname().toLowerCase(Locale.ROOT);
        this.removingNames.add(key);
        AuraAccountsScreen$CardState state = this.cardStates.get(key);
        if (state != null) {
            state.removing = true;
            state.alphaTarget = 0.0F;
        }

        AccountManager.remove(account);
    }

    void startClearAll() {
        this.clearWhenAnimationsDone = true;

        for (AccountManager$Account account : AccountManager.getAccounts()) {
            String key = account.getNickname().toLowerCase(Locale.ROOT);
            this.removingNames.add(key);
            AuraAccountsScreen$CardState state = this.cardStates.get(key);
            if (state != null) {
                state.removing = true;
                state.alphaTarget = 0.0F;
            }
        }

        AccountManager.clearAll();
        this.accountTargetScroll = 0.0F;
    }

    void finishDeferredRemoval() {
        if (this.clearWhenAnimationsDone && this.cardStates.isEmpty()) {
            this.clearWhenAnimationsDone = false;
        }
    }

    boolean noVisibleCardStates() {
        for (AuraAccountsScreen$CardState state : this.cardStates.values()) {
            if (state.alpha > 0.04F) {
                return false;
            }
        }

        return true;
    }

    void drawInput(DrawContext context, int x, int y, int w, int h, String value, String placeholder, boolean focused, char icon, int mouseX, int mouseY) {
        boolean hovered = this.contains(mouseX, mouseY, x, y, w, h);
        int fill = new Color(8, 8, 12, focused ? 214 : (hovered ? 198 : 184)).getRGB();
        int border = this.withAlpha(-1824962, focused ? 190 : (hovered ? 135 : 96));
        this.drawSoftFrame(context, x, y, w, h, 6.5F, this.withUiAlpha(fill, 1.0F), border);
        String text = value.isEmpty() ? placeholder : value;
        int color = value.isEmpty() ? 2113929215 : -1;
        int iconBox = Math.max(9, Math.round((h - 8) * 0.9F));
        int textMax = Math.max(18, w - h - 12);
        FontManager.drawCustomTextScaled(
            context,
            this.fitText(text, textMax, 0.21F),
            x + 8.0F,
            y + (h - FontManager.getScaledTextHeight(0.21F)) * 0.5F + 1.0F,
            this.withUiAlpha(color, 1.0F),
            false,
            0.21F
        );
        if (focused && !value.isEmpty() && System.currentTimeMillis() % 1000L > 500L) {
            float cursorX = x + 8.0F + Math.min(textMax, FontManager.getScaledTextWidth(value, 0.21F)) + 2.0F;
            RenderUtil.drawRoundedRect(context, cursorX, y + 6.0F, 1.0F, h - 12.0F, 0.5F, this.withUiAlpha(-855638017, 1.0F));
        }

        this.drawIcon(context, icon, x + w - iconBox - 5.5F, y + (h - iconBox) * 0.5F, iconBox, this.withUiAlpha(focused ? -4013366 : -7171430, 1.0F));
    }

    void drawActionButton(DrawContext context, int x, int y, int w, int h, String label, char icon, int mouseX, int mouseY) {
        boolean hovered = this.contains(mouseX, mouseY, x, y, w, h);
        int fill = new Color(8, 8, 12, hovered ? 210 : 188).getRGB();
        int border = this.withAlpha(-1824962, hovered ? 158 : 102);
        this.drawSoftFrame(context, x, y, w, h, 6.5F, this.withUiAlpha(fill, 1.0F), border);
        float scale = 0.21F;
        if (icon == 0) {
            FontManager.drawCustomTextScaled(
                context,
                this.fitText(label, w - 14, scale),
                x + (w - FontManager.getScaledTextWidth(label, scale)) * 0.5F,
                y + (h - FontManager.getScaledTextHeight(scale)) * 0.5F + 1.0F,
                this.withUiAlpha(-1, 1.0F),
                false,
                scale
            );
        } else {
            int iconBox = Math.max(9, Math.round((h - 8) * 0.9F));
            int textMax = Math.max(18, w - iconBox - 19);
            FontManager.drawCustomTextScaled(
                context,
                this.fitText(label, textMax, scale),
                x + 8.0F,
                y + (h - FontManager.getScaledTextHeight(scale)) * 0.5F + 1.0F,
                this.withUiAlpha(-1, 1.0F),
                false,
                scale
            );
            this.drawIcon(context, icon, x + w - iconBox - 6.0F, y + (h - iconBox) * 0.5F, iconBox, this.withUiAlpha(hovered ? -4802882 : -7171430, 1.0F));
        }
    }

    void drawPlayerFace(DrawContext context, String nickname, float x, float y, float size, float alpha) {
        UUID uuid = AccountManager.offlineUuid(nickname);
        int color = (int)(MathHelper.clamp(this.openAnim * alpha, 0.0F, 1.0F) * 255.0F) << 24 | 16777215;
        PlayerSkinDrawer.draw(context, DefaultSkinHelper.getSkinTextures(uuid), Math.round(x), Math.round(y), Math.round(size), color);
    }

    void drawIcon(DrawContext context, char ch, float x, float y, float size, int color) {
        if (!IconTtfBridge.tryDrawIconsFontChar(context, ch, x, y, size, color)) {
            FontManager.drawCustomTextScaled(context, String.valueOf(ch), x + 2.0F, y + 1.0F, color, false, 0.21F);
        }
    }

    void drawEmptyState(DrawContext context, int x, int y, int w, int h) {
        String text = "No accounts";
        float scale = 0.27F;
        FontManager.drawCustomTextScaled(
            context, text, x + (w - FontManager.getScaledTextWidth(text, scale)) * 0.5F, y + h * 0.5F - 7.0F, this.withUiAlpha(1929379839, 1.0F), false, scale
        );
    }

    void renderScrollbar(DrawContext context, AuraAccountsScreen$Layout layout) {
        float max = this.accountMaxScroll(layout);
        if (!(max <= 1.0F)) {
            int x = layout.panelX + layout.panelW - 7;
            int y = layout.panelY + layout.pad;
            int h = layout.panelH - layout.pad * 2;
            float thumbH = Math.max(22.0F, h * Math.min(1.0F, h / (h + max)));
            float thumbY = y + (h - thumbH) * (this.accountScroll / max);
            RenderUtil.drawRoundedRect(context, x, y, 3.0F, h, 1.5F, this.withUiAlpha(587202559, 1.0F));
            RenderUtil.drawRoundedRect(context, x, thumbY, 3.0F, thumbH, 1.5F, this.withAlpha(-1824962, 180));
        }
    }

    List<AccountManager$Account> visibleAccounts() {
        List<AccountManager$Account> result = new ArrayList<>();
        String query = this.searchInput.trim().toLowerCase(Locale.ROOT);

        for (AccountManager$Account account : AccountManager.getAccounts()) {
            String key = account.getNickname().toLowerCase(Locale.ROOT);
            if (!this.removingNames.contains(key) && (query.isEmpty() || key.contains(query))) {
                result.add(account);
            }
        }

        Comparator<AccountManager$Account> comparator = Comparator.comparing(AccountManager$Account::isFavorite).reversed();
        if (this.sortByDate) {
            comparator = comparator.thenComparing(Comparator.comparingLong(AccountManager$Account::getCreatedAt).reversed());
        } else {
            comparator = comparator.thenComparing(AccountManager$Account::getNickname, String.CASE_INSENSITIVE_ORDER);
        }

        result.sort(comparator);
        return result;
    }

    float accountMaxScroll(AuraAccountsScreen$Layout layout) {
        int count = this.visibleAccounts().size();
        int rows = (count + 1) / 2;
        int contentH = rows == 0 ? 0 : rows * layout.cardH + (rows - 1) * layout.rowGap;
        int visibleH = layout.panelH - layout.pad * 2;
        return Math.max(0.0F, contentH - visibleH);
    }

    void addTypedAccount() {
        AccountManager$Account account = AccountManager.addAccount(this.nicknameInput);
        if (account != null) {
            this.nicknameInput = "";
            this.searchInput = "";
            this.accountTargetScroll = 0.0F;
        }
    }

    AuraAccountsScreen$Layout layout() {
        int base = MathHelper.clamp(Math.min(this.width - 170, this.height - 126), 280, 430);
        int panelW = Math.min(this.width - 112, Math.round(base * 1.08F));
        int panelH = Math.min(this.height - 120, Math.round(base * 0.7F));
        panelW = MathHelper.clamp(panelW, 302, 464);
        panelH = MathHelper.clamp(panelH, 196, 302);
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2 + 15;
        int controlH = 21;
        int gap = 8;
        int controlGapY = 9;
        int topY = panelY - controlH - controlGapY;
        int bottomY = panelY + panelH + controlGapY;
        int titleY = topY - 48;
        if (titleY < 10) {
            int shift = 10 - titleY;
            panelY += shift;
            topY += shift;
            bottomY += shift;
            titleY += shift;
        }

        if (bottomY + controlH > this.height - 10) {
            int shift = bottomY + controlH - (this.height - 10);
            panelY -= shift;
            topY -= shift;
            bottomY -= shift;
            titleY -= shift;
            if (titleY < 10) {
                int fix = 10 - titleY;
                titleY += fix;
                topY += fix;
                panelY += fix;
                bottomY += fix;
            }
        }

        int exitW = Math.min(74, Math.max(58, Math.round(panelW * 0.17F)));
        int randomW = Math.min(106, Math.max(84, Math.round(panelW * 0.25F)));
        int nickW = panelW - randomW - exitW - gap * 2;
        if (nickW < 118) {
            nickW = 118;
            randomW = Math.max(74, panelW - nickW - exitW - gap * 2);
        }

        int randomX = panelX + nickW + gap;
        int exitX = randomX + randomW + gap;
        int clearW = Math.min(112, Math.max(90, Math.round(panelW * 0.24F)));
        int sortW = Math.min(122, Math.max(94, Math.round(panelW * 0.29F)));
        int searchW = panelW - sortW - clearW - gap * 2;
        int sortX = panelX + searchW + gap;
        int clearX = sortX + sortW + gap;
        int pad = 12;
        int cardGap = 10;
        int rowGap = 8;
        int cardW = (panelW - pad * 2 - cardGap) / 2;
        int cardH = MathHelper.clamp(Math.round(panelH * 0.17F), 40, 45);
        return new AuraAccountsScreen$Layout(
            panelX,
            panelY,
            panelW,
            panelH,
            titleY,
            topY,
            bottomY,
            controlH,
            panelX,
            nickW,
            randomX,
            randomW,
            exitX,
            exitW,
            panelX,
            searchW,
            sortX,
            sortW,
            clearX,
            clearW,
            pad,
            cardGap,
            rowGap,
            cardW,
            cardH
        );
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

    void drawSoftFrame(DrawContext context, float x, float y, float w, float h, float radius, int fill, int border) {
        float inset = 0.75F;
        RenderUtil.drawRoundedRect(context, x, y, w, h, radius, border);
        RenderUtil.drawRoundedRect(context, x + inset, y + inset, w - inset * 2.0F, h - inset * 2.0F, Math.max(0.0F, radius - inset), fill);
    }

    String fitText(String text, int maxW, float scale) {
        if (FontManager.getScaledTextWidth(text, scale) <= maxW) {
            return text;
        }

        String suffix = "...";
        int end = text.length();

        while (end > 0 && FontManager.getScaledTextWidth(text.substring(0, end) + suffix, scale) > maxW) {
            end--;
        }

        return end <= 0 ? "" : text.substring(0, end) + suffix;
    }

    boolean isNicknameChar(char chr) {
        return chr >= 'A' && chr <= 'Z' || chr >= 'a' && chr <= 'z' || chr >= '0' && chr <= '9' || chr == '_';
    }

    boolean contains(double mouseX, double mouseY, float x, float y, float w, float h) {
        return mouseX >= x && mouseX <= x + w && mouseY >= y && mouseY <= y + h;
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

    float easeOut(float value) {
        float v = MathHelper.clamp(value, 0.0F, 1.0F);
        return 1.0F - (1.0F - v) * (1.0F - v);
    }

    int withAlpha(int rgb, int alpha) {
        int a = MathHelper.clamp(Math.round(MathHelper.clamp(alpha, 0, 255) * this.openAnim), 0, 255);
        return a << 24 | rgb & 16777215;
    }

    int withUiAlpha(int color, float multiplier) {
        int a = color >>> 24 & 0xFF;
        int next = MathHelper.clamp(Math.round(a * this.openAnim * MathHelper.clamp(multiplier, 0.0F, 1.0F)), 0, 255);
        return next << 24 | color & 16777215;
    }

    int blendColor(int from, int to, float amount) {
        float t = MathHelper.clamp(amount, 0.0F, 1.0F);
        int a = Math.round((from >>> 24 & 0xFF) + ((to >>> 24 & 0xFF) - (from >>> 24 & 0xFF)) * t);
        int r = Math.round((from >>> 16 & 0xFF) + ((to >>> 16 & 0xFF) - (from >>> 16 & 0xFF)) * t);
        int g = Math.round((from >>> 8 & 0xFF) + ((to >>> 8 & 0xFF) - (from >>> 8 & 0xFF)) * t);
        int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }
}
