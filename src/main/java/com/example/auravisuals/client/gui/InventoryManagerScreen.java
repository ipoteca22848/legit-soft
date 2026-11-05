package com.example.auravisuals.client.gui;

import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.inventory.InventoryPresetManager;
import com.example.auravisuals.inventory.InventoryPresetManager$MissingEntry;
import com.example.auravisuals.inventory.InventoryPresetManager$Preset;
import com.example.auravisuals.inventory.InventoryPresetManager$SavedStack;
import com.example.auravisuals.module.misc.AucSearch;
import com.example.auravisuals.module.misc.InventoryManager;
import com.example.auravisuals.module.render.ClickGui;
import com.example.auravisuals.theme.ThemeManager;
import com.example.auravisuals.util.render.RenderUtil;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class InventoryManagerScreen extends Screen {
    static final Identifier BACKGROUND = Identifier.of("auravisuals", "textures/mainmenu/background.png");
    final Screen parent;
    List<InventoryPresetManager$Preset> presets = new ArrayList<>();
    String nameInput = "";
    boolean nameFocused;
    int selectedIndex = -1;
    float listScroll;
    float listTargetScroll;
    float missingScroll;
    float missingTargetScroll;
    float openAnim;
    boolean closing;
    long statusUntil;
    String status = "";

    public InventoryManagerScreen(Screen parent) {
        super(Text.literal("Inventory Manager"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.reloadPresets();
        this.openAnim = 0.0F;
        this.closing = false;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        this.openAnim = this.approach(this.openAnim, this.closing ? 0.0F : 1.0F, deltaTicks, 0.18F);
        if (!this.closing || !(this.openAnim <= 0.03F)) {
            this.renderBackgroundLayer(context);
            InventoryManagerScreen$Layout layout = this.layout();
            float ease = this.easeOut(this.openAnim);
            context.getMatrices().pushMatrix();
            context.getMatrices().translate(0.0F, (1.0F - ease) * 10.0F);
            this.renderHeader(context, layout, mouseX, mouseY);
            this.renderLeftPanel(context, layout, mouseX, mouseY, deltaTicks);
            this.renderPreview(context, layout);
            this.renderStatus(context, layout);
            context.getMatrices().popMatrix();
        } else if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mouseX = click.x();
        double mouseY = click.y();
        int button = click.button();
        if (button == 0 && !this.closing) {
            InventoryManagerScreen$Layout layout = this.layout();
            if (this.contains(mouseX, mouseY, layout.backX, layout.backY, 62.0, 18.0)) {
                this.close();
                return true;
            } else {
                this.nameFocused = this.contains(mouseX, mouseY, layout.inputX, layout.inputY, layout.inputW, layout.controlH);
                if (this.contains(mouseX, mouseY, layout.saveX, layout.inputY, layout.saveW, layout.controlH)) {
                    this.savePreset();
                    return true;
                } else if (this.contains(mouseX, mouseY, layout.listX, layout.listY, layout.listW, layout.listH)) {
                    this.clickPresetList(mouseX, mouseY, layout);
                    return true;
                } else if (this.contains(mouseX, mouseY, layout.missingX, layout.missingY, layout.missingW, layout.missingH)) {
                    this.clickMissingList(mouseX, mouseY, layout);
                    return true;
                } else {
                    return super.mouseClicked(click, doubled);
                }
            }
        } else {
            return super.mouseClicked(click, doubled);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        InventoryManagerScreen$Layout layout = this.layout();
        if (this.contains(mouseX, mouseY, layout.listX, layout.listY, layout.listW, layout.listH)) {
            float max = this.maxScroll(layout);
            this.listTargetScroll = MathHelper.clamp(this.listTargetScroll - (float)verticalAmount * 28.0F, 0.0F, max);
            return true;
        } else if (this.contains(mouseX, mouseY, layout.missingX, layout.missingY, layout.missingW, layout.missingH)) {
            float max = this.maxMissingScroll(layout);
            this.missingTargetScroll = MathHelper.clamp(this.missingTargetScroll - (float)verticalAmount * 24.0F, 0.0F, max);
            return true;
        } else {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int keyCode = input.key();
        if (keyCode == 256) {
            this.close();
            return true;
        }

        if (this.nameFocused) {
            if (keyCode == 259 && !this.nameInput.isEmpty()) {
                this.nameInput = this.nameInput.substring(0, this.nameInput.length() - 1);
                return true;
            }

            if (keyCode == 257 || keyCode == 335) {
                this.savePreset();
                return true;
            }
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        char chr = (char)input.codepoint();
        if (this.nameFocused
            && this.nameInput.length() < 24
            && chr >= ' '
            && chr != '\\'
            && chr != '/'
            && chr != ':'
            && chr != '*'
            && chr != '?'
            && chr != '"'
            && chr != '<'
            && chr != '>'
            && chr != '|') {
            this.nameInput = this.nameInput + chr;
            return true;
        } else {
            return super.charTyped(input);
        }
    }

    void renderBackgroundLayer(DrawContext context) {
        RenderUtil.drawTexture(context, BACKGROUND, 0.0F, 0.0F, this.width, this.height, 0.0F, 0.0F, this.width, this.height, this.width, this.height);
        context.fill(0, 0, this.width, this.height, new Color(4, 5, 9, 205).getRGB());
    }

    void renderHeader(DrawContext context, InventoryManagerScreen$Layout layout, int mouseX, int mouseY) {
        int text = new Color(245, 246, 250, this.alpha(255)).getRGB();
        int muted = new Color(158, 160, 170, this.alpha(255)).getRGB();
        FontManager.drawCustomTextScaled(
            context,
            "<- Back",
            layout.backX,
            layout.backY + 5,
            this.contains(mouseX, mouseY, layout.backX, layout.backY, 62.0, 18.0) ? text : muted,
            false,
            0.36F
        );
        FontManager.drawCustomTextScaled(context, "Inventory Manager", layout.leftX, layout.topY, text, false, 0.54F);
        FontManager.drawCustomTextScaled(
            context, this.presets.size() + " presets", layout.previewX + layout.previewW - 58, layout.topY + 4, muted, false, 0.32F
        );
    }

    void renderLeftPanel(DrawContext context, InventoryManagerScreen$Layout layout, int mouseX, int mouseY, float delta) {
        int panel = ClickGui.applyBackgroundOpacity(this.withAlpha(ThemeManager.INSTANCE.getPanelColor(), this.alpha(220)));
        int button = ClickGui.applyBackgroundOpacity(this.withAlpha(ThemeManager.INSTANCE.getButtonsColor(), this.alpha(230)));
        RenderUtil.drawRoundedRect(context, layout.leftX, layout.panelY, layout.leftW, layout.panelH, 8.0F, panel);
        RenderUtil.drawRoundedRect(context, layout.inputX, layout.inputY, layout.inputW, layout.controlH, 6.0F, button);
        String input = this.nameInput.isEmpty() && !this.nameFocused ? "Preset name" : this.nameInput;
        int inputColor = this.nameInput.isEmpty() && !this.nameFocused
            ? new Color(136, 138, 148, this.alpha(255)).getRGB()
            : new Color(245, 246, 250, this.alpha(255)).getRGB();
        FontManager.drawCustomTextScaled(
            context,
            input + (this.nameFocused && System.currentTimeMillis() % 1000L > 500L ? "_" : ""),
            layout.inputX + 10,
            layout.inputY + 8,
            inputColor,
            false,
            0.34F
        );
        int accent = ThemeManager.INSTANCE.getGuiIconColor() | 0xFF000000;
        RenderUtil.drawRoundedRect(context, layout.saveX, layout.inputY, layout.saveW, layout.controlH, 6.0F, accent);
        FontManager.drawCustomTextScaled(context, "+ Save", layout.saveX + 16, layout.inputY + 8, -1, false, 0.34F);
        this.listScroll = this.approach(this.listScroll, this.listTargetScroll, delta, 0.22F);
        context.enableScissor(layout.listX, layout.listY, layout.listX + layout.listW, layout.listY + layout.listH);
        float y = layout.listY - this.listScroll;

        for (int i = 0; i < this.presets.size(); i++) {
            InventoryPresetManager$Preset preset = this.presets.get(i);
            this.renderPresetRow(context, preset, i, layout.listX, Math.round(y), layout.listW, layout.rowH, mouseX, mouseY);
            y += layout.rowH + layout.rowGap;
        }

        context.disableScissor();
        if (this.presets.isEmpty()) {
            FontManager.drawCustomTextScaled(
                context, "No saved inventories", layout.listX + 10, layout.listY + 12, new Color(145, 147, 158, this.alpha(255)).getRGB(), false, 0.34F
            );
        }
    }

    void renderPresetRow(DrawContext context, InventoryPresetManager$Preset preset, int index, int x, int y, int w, int h, int mouseX, int mouseY) {
        boolean selected = index == this.selectedIndex;
        boolean hovered = this.contains(mouseX, mouseY, x, y, w, h);
        int bg = selected
            ? this.withAlpha(ThemeManager.INSTANCE.getGuiIconColor(), this.alpha(86))
            : new Color(255, 255, 255, hovered ? this.alpha(20) : this.alpha(10)).getRGB();
        RenderUtil.drawRoundedRect(context, x, y, w, h, 6.0F, bg);
        FontManager.drawCustomTextScaled(context, this.fit(preset.name(), 120), x + 10, y + 7, -1, false, 0.36F);
        FontManager.drawCustomTextScaled(
            context, preset.filledSlots() + " slots", x + 10, y + 22, new Color(150, 152, 162, this.alpha(255)).getRGB(), false, 0.3F
        );
        int loadX = x + w - 96;
        int delX = x + w - 30;
        RenderUtil.drawRoundedRect(context, loadX, y + 9, 56.0F, 18.0F, 5.0F, new Color(255, 255, 255, hovered ? this.alpha(28) : this.alpha(18)).getRGB());
        FontManager.drawCustomTextScaled(context, "Load", loadX + 16, y + 14, new Color(225, 226, 232, this.alpha(255)).getRGB(), false, 0.29F);
        RenderUtil.drawRoundedRect(context, delX, y + 9, 20.0F, 18.0F, 5.0F, new Color(228, 39, 62, hovered ? this.alpha(95) : this.alpha(65)).getRGB());
        FontManager.drawCustomTextScaled(context, "x", delX + 8, y + 13, -1, false, 0.32F);
    }

    void renderPreview(DrawContext context, InventoryManagerScreen$Layout layout) {
        int panel = ClickGui.applyBackgroundOpacity(this.withAlpha(ThemeManager.INSTANCE.getPanelColor(), this.alpha(220)));
        RenderUtil.drawRoundedRect(context, layout.previewX, layout.panelY, layout.previewW, layout.panelH, 8.0F, panel);
        FontManager.drawCustomTextScaled(context, "Preview", layout.previewX + 14, layout.panelY + 12, -1, false, 0.42F);
        InventoryPresetManager$Preset preset = this.selectedPreset();
        if (preset == null) {
            FontManager.drawCustomTextScaled(
                context, "Select or save preset", layout.previewX + 14, layout.panelY + 34, new Color(145, 147, 158, this.alpha(255)).getRGB(), false, 0.34F
            );
        } else {
            int slot = 18;
            int gap = 3;
            int gridX = layout.previewX + 18;
            int gridY = layout.panelY + 42;

            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 9; col++) {
                    int idx = row == 3 ? col : 9 + row * 9 + col;
                    this.drawSlot(context, preset.main()[idx], gridX + col * (slot + gap), gridY + row * (slot + gap), slot);
                }
            }

            int gearY = gridY + 4 * (slot + gap) + 9;

            for (int i = 0; i < preset.armor().length; i++) {
                this.drawSlot(context, preset.armor()[i], gridX + i * (slot + gap), gearY, slot);
            }

            this.drawSlot(context, preset.offhand(), gridX + 5 * (slot + gap), gearY, slot);
            this.renderMissingList(context, preset, layout);
        }
    }

    void drawSlot(DrawContext context, InventoryPresetManager$SavedStack saved, int x, int y, int size) {
        RenderUtil.drawRoundedRect(context, x, y, size, size, 5.0F, new Color(255, 255, 255, this.alpha(14)).getRGB());
        ItemStack stack = saved == null ? ItemStack.EMPTY : saved.toItemStack();
        if (!stack.isEmpty()) {
            context.drawItem(stack, x + 1, y + 1);
            context.drawStackOverlay(MinecraftClient.getInstance().textRenderer, stack, x + 1, y + 1);
        }
    }

    void renderMissingList(DrawContext context, InventoryPresetManager$Preset preset, InventoryManagerScreen$Layout layout) {
        List<InventoryPresetManager$MissingEntry> entries = InventoryPresetManager.INSTANCE.getMissingEntries(preset);
        int titleColor = entries.isEmpty() ? new Color(110, 230, 150, this.alpha(255)).getRGB() : new Color(238, 74, 92, this.alpha(255)).getRGB();
        FontManager.drawCustomTextScaled(context, entries.isEmpty() ? "Ready" : "Missing", layout.missingX, layout.missingY - 16, titleColor, false, 0.34F);
        if (entries.isEmpty()) {
            FontManager.drawCustomTextScaled(
                context, "All set", layout.missingX, layout.missingY + 4, new Color(145, 147, 158, this.alpha(255)).getRGB(), false, 0.31F
            );
        } else {
            this.missingScroll = this.approach(this.missingScroll, this.missingTargetScroll, 1.0F, 0.2F);
            context.enableScissor(layout.missingX - 2, layout.missingY, layout.missingX + layout.missingW, layout.missingY + layout.missingH);
            int rowH = 22;
            float y = layout.missingY - this.missingScroll;

            for (InventoryPresetManager$MissingEntry entry : entries) {
                boolean hover = this.contains(this.lastMouseX(), this.lastMouseY(), layout.missingX - 2, y - 2.0F, layout.missingW, rowH);
                if (hover) {
                    RenderUtil.drawRoundedRect(
                        context, layout.missingX - 3, y - 3.0F, layout.missingW, rowH, 5.0F, new Color(255, 255, 255, this.alpha(22)).getRGB()
                    );
                }

                this.drawSlot(context, entry.stack(), layout.missingX, Math.round(y), 18);
                FontManager.drawCustomTextScaled(context, "x" + entry.missingCount(), layout.missingX + 23, y + 6.0F, -1, false, 0.34F);
                y += rowH;
            }

            context.disableScissor();
        }
    }

    void renderStatus(DrawContext context, InventoryManagerScreen$Layout layout) {
        if (System.currentTimeMillis() <= this.statusUntil && !this.status.isEmpty()) {
            FontManager.drawCustomTextScaled(
                context, this.status, layout.leftX, layout.panelY + layout.panelH + 10, new Color(185, 187, 196, this.alpha(255)).getRGB(), false, 0.32F
            );
        }
    }

    void clickPresetList(double mouseX, double mouseY, InventoryManagerScreen$Layout layout) {
        float y = layout.listY - this.listScroll;

        for (int i = 0; i < this.presets.size(); i++) {
            if (this.contains(mouseX, mouseY, layout.listX, y, layout.listW, layout.rowH)) {
                InventoryPresetManager$Preset preset = this.presets.get(i);
                this.selectedIndex = i;
                InventoryManager.setActivePreset(preset.name());
                int loadX = layout.listX + layout.listW - 96;
                int delX = layout.listX + layout.listW - 30;
                if (this.contains(mouseX, mouseY, loadX, y + 9.0F, 56.0, 18.0)) {
                    this.applyPreset(preset);
                } else if (this.contains(mouseX, mouseY, delX, y + 9.0F, 20.0, 18.0)) {
                    InventoryPresetManager.INSTANCE.deletePreset(preset.name());
                    this.reloadPresets();
                    this.selectedIndex = Math.min(this.selectedIndex, this.presets.size() - 1);
                    this.showStatus("Deleted");
                }

                return;
            }

            y += layout.rowH + layout.rowGap;
        }
    }

    void clickMissingList(double mouseX, double mouseY, InventoryManagerScreen$Layout layout) {
        InventoryPresetManager$Preset preset = this.selectedPreset();
        if (preset != null) {
            List<InventoryPresetManager$MissingEntry> entries = InventoryPresetManager.INSTANCE.getMissingEntries(preset);
            int rowH = 22;
            float y = layout.missingY - this.missingScroll;

            for (InventoryPresetManager$MissingEntry entry : entries) {
                if (this.contains(mouseX, mouseY, layout.missingX - 2, y - 2.0F, layout.missingW, rowH)) {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (AucSearch.searchStack(mc, entry.stack().toItemStack())) {
                        this.showStatus("Auction search: " + entry.stack().toItemStack().getName().getString());
                        mc.setScreen(null);
                    } else {
                        this.showStatus("Auction search failed");
                    }

                    return;
                }

                y += rowH;
            }
        }
    }

    void savePreset() {
        InventoryPresetManager$Preset preset = InventoryPresetManager.INSTANCE.saveCurrent(this.nameInput);
        if (preset == null) {
            this.showStatus("Join a world first");
        } else {
            this.reloadPresets();

            for (int i = 0; i < this.presets.size(); i++) {
                if (this.presets.get(i).name().equals(preset.name())) {
                    this.selectedIndex = i;
                    InventoryManager.setActivePreset(preset.name());
                    break;
                }
            }

            this.nameInput = "";
            this.nameFocused = false;
            this.showStatus("Saved");
        }
    }

    void applyPreset(InventoryPresetManager$Preset preset) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.interactionManager != null) {
            if (!(mc.currentScreen instanceof InventoryScreen)) {
                mc.setScreen(new InventoryScreen(mc.player));
            }

            for (int target = 0; target < preset.main().length; target++) {
                InventoryPresetManager$SavedStack desired = preset.main()[target];
                if (desired != null && !desired.isEmpty() && !this.sameItem(mc.player.getInventory().getStack(target), desired)) {
                    int source = this.findMatchingSlot(desired, target);
                    if (source >= 0) {
                        this.swapSlots(source, target);
                    }
                }
            }

            this.showStatus("Loaded matching items");
        } else {
            this.showStatus("Join a world first");
        }
    }

    int findMatchingSlot(InventoryPresetManager$SavedStack desired, int except) {
        MinecraftClient mc = MinecraftClient.getInstance();

        for (int i = 0; i < 36; i++) {
            if (i != except && this.sameItem(mc.player.getInventory().getStack(i), desired)) {
                return i;
            }
        }

        return -1;
    }

    boolean sameItem(ItemStack stack, InventoryPresetManager$SavedStack saved) {
        return stack != null && !stack.isEmpty() && InventoryPresetManager$SavedStack.from(stack).itemId().equals(saved.itemId());
    }

    void swapSlots(int a, int b) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int syncId = mc.player.currentScreenHandler.syncId;
        this.click(this.slotId(a), syncId);
        this.click(this.slotId(b), syncId);
        if (!mc.player.currentScreenHandler.getCursorStack().isEmpty()) {
            this.click(this.slotId(a), syncId);
        }
    }

    void click(int slotId, int syncId) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.interactionManager.clickSlot(syncId, slotId, 0, SlotActionType.PICKUP, mc.player);
    }

    int slotId(int inventoryIndex) {
        return inventoryIndex < 9 ? 36 + inventoryIndex : inventoryIndex;
    }

    void reloadPresets() {
        this.presets = InventoryPresetManager.INSTANCE.listPresets();
        if (this.selectedIndex >= this.presets.size()) {
            this.selectedIndex = this.presets.size() - 1;
        }
    }

    InventoryPresetManager$Preset selectedPreset() {
        return this.selectedIndex >= 0 && this.selectedIndex < this.presets.size() ? this.presets.get(this.selectedIndex) : null;
    }

    float maxScroll(InventoryManagerScreen$Layout layout) {
        float content = this.presets.size() * (layout.rowH + layout.rowGap);
        return Math.max(0.0F, content - layout.listH);
    }

    float maxMissingScroll(InventoryManagerScreen$Layout layout) {
        InventoryPresetManager$Preset preset = this.selectedPreset();
        float content = preset == null ? 0.0F : InventoryPresetManager.INSTANCE.getMissingEntries(preset).size() * 22.0F;
        return Math.max(0.0F, content - layout.missingH);
    }

    int lastMouseX() {
        return (int)(MinecraftClient.getInstance().mouse.getX() * this.width / MinecraftClient.getInstance().getWindow().getWidth());
    }

    int lastMouseY() {
        return (int)(MinecraftClient.getInstance().mouse.getY() * this.height / MinecraftClient.getInstance().getWindow().getHeight());
    }

    @Override
    public void close() {
        this.closing = true;
    }

    void showStatus(String value) {
        this.status = value;
        this.statusUntil = System.currentTimeMillis() + 1800L;
    }

    InventoryManagerScreen$Layout layout() {
        int panelW = Math.min(620, this.width - 44);
        int panelH = Math.min(310, this.height - 64);
        int leftX = (this.width - panelW) / 2;
        int topY = Math.max(28, (this.height - panelH) / 2);
        int leftW = 310;
        int gap = 12;
        int previewW = panelW - leftW - gap;
        return new InventoryManagerScreen$Layout(leftX, topY, leftW, previewW, panelH, gap);
    }

    boolean contains(double mx, double my, double x, double y, double w, double h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    int withAlpha(int color, int alpha) {
        return MathHelper.clamp(alpha, 0, 255) << 24 | color & 16777215;
    }

    int alpha(int value) {
        return Math.round(value * MathHelper.clamp(this.openAnim, 0.0F, 1.0F));
    }

    float approach(float current, float target, float delta, float speed) {
        return current + (target - current) * Math.min(1.0F, delta * speed);
    }

    float easeOut(float value) {
        return 1.0F - (float)Math.pow(1.0F - MathHelper.clamp(value, 0.0F, 1.0F), 3.0);
    }

    String fit(String text, int maxWidth) {
        if (FontManager.getWidth(text) <= maxWidth) {
            return text;
        }

        String suffix = "...";

        while (!text.isEmpty() && FontManager.getWidth(text + suffix) > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }

        return text + suffix;
    }
}
