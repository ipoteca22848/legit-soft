package com.example.auravisuals.module.misc;

import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.inventory.InventoryPresetManager;
import com.example.auravisuals.inventory.InventoryPresetManager$MissingEntry;
import com.example.auravisuals.inventory.InventoryPresetManager$Preset;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.ClickGui;
import com.example.auravisuals.util.render.RenderUtil;
import java.awt.Color;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;

public class InventoryManager extends Module {
    private static String activePresetName;
    private static float missingScroll;

    public InventoryManager() {
        super("Inventory Manager", "Save and preview inventory presets", Category.MISC);
    }

    @Override
    public void onEnable() {
        if (this.mc != null && this.mc.player != null) {
            this.mc.execute(() -> {
                if (!(this.mc.currentScreen instanceof InventoryScreen)) {
                    this.mc.setScreen(new InventoryScreen(this.mc.player));
                }
            });
        }
    }

    public static void setActivePreset(String presetName) {
        activePresetName = presetName;
    }

    public static InventoryPresetManager$Preset getActivePreset() {
        InventoryPresetManager$Preset preset = InventoryPresetManager.INSTANCE.findPreset(activePresetName);
        return preset != null ? preset : InventoryPresetManager.INSTANCE.getFallbackPreset();
    }

    public static void renderInventoryOverlay(DrawContext context, int mouseX, int mouseY, int guiX, int guiY, int backgroundWidth) {
        MinecraftClient client = MinecraftClient.getInstance();
        InventoryManager module = ModuleManager.INSTANCE.getModule(InventoryManager.class);
        if (module != null && module.isEnabled() && client.player != null) {
            InventoryPresetManager$Preset preset = getActivePreset();
            if (preset != null) {
                List<InventoryPresetManager$MissingEntry> entries = InventoryPresetManager.INSTANCE.getMissingEntries(preset);
                int panelX = guiX + backgroundWidth + 8;
                int panelY = guiY + 8;
                int panelW = 78;
                int panelH = Math.max(46, Math.min(142, entries.size() * 21 + 25));
                RenderUtil.drawRoundedRect(context, panelX, panelY, panelW, panelH, 4.0F, ClickGui.applyBackgroundOpacity(new Color(15, 15, 23, 214).getRGB()));
                int titleColor = entries.isEmpty() ? new Color(94, 230, 132).getRGB() : new Color(238, 74, 92).getRGB();
                FontManager.drawCustomTextScaled(context, entries.isEmpty() ? "Ready" : "Не хватает:", panelX + 7, panelY + 7, titleColor, false, 0.31F);
                if (!entries.isEmpty()) {
                    int listY = panelY + 22;
                    int listH = panelH - 26;
                    int maxScroll = Math.max(0, entries.size() * 21 - listH);
                    missingScroll = Math.max(0.0F, Math.min(missingScroll, maxScroll));
                    context.enableScissor(panelX, listY, panelX + panelW, panelY + panelH - 3);
                    float y = listY - missingScroll;

                    for (InventoryPresetManager$MissingEntry entry : entries) {
                        boolean hover = mouseX >= panelX + 4 && mouseX <= panelX + panelW - 4 && mouseY >= y - 1.0F && mouseY <= y + 19.0F;
                        if (hover) {
                            RenderUtil.drawRoundedRect(context, panelX + 4, y - 1.0F, panelW - 8, 20.0F, 3.0F, new Color(255, 255, 255, 24).getRGB());
                        }

                        drawTinyStack(context, entry.stack().toItemStack(), panelX + 7, Math.round(y + 1.0F));
                        FontManager.drawCustomTextScaled(context, "x" + entry.missingCount(), panelX + 29, y + 6.0F, -1, false, 0.32F);
                        y += 21.0F;
                    }

                    context.disableScissor();
                }
            }
        }
    }

    public static boolean mouseClickedInventoryOverlay(double mouseX, double mouseY, int button, int guiX, int guiY, int backgroundWidth) {
        MinecraftClient client = MinecraftClient.getInstance();
        InventoryManager module = ModuleManager.INSTANCE.getModule(InventoryManager.class);
        if (button == 0 && module != null && module.isEnabled() && client.player != null) {
            InventoryPresetManager$Preset preset = getActivePreset();
            List<InventoryPresetManager$MissingEntry> entries = InventoryPresetManager.INSTANCE.getMissingEntries(preset);
            int panelX = guiX + backgroundWidth + 8;
            int panelY = guiY + 8;
            int panelW = 78;
            int panelH = Math.max(46, Math.min(142, entries.size() * 21 + 25));
            if (!(mouseX < panelX) && !(mouseX > panelX + panelW) && !(mouseY < panelY) && !(mouseY > panelY + panelH)) {
                float y = panelY + 22 - missingScroll;

                for (InventoryPresetManager$MissingEntry entry : entries) {
                    if (mouseX >= panelX + 4 && mouseX <= panelX + panelW - 4 && mouseY >= y - 1.0F && mouseY <= y + 19.0F) {
                        AucSearch.searchStack(client, entry.stack().toItemStack());
                        client.player.closeHandledScreen();
                        return true;
                    }

                    y += 21.0F;
                }

                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public static boolean mouseScrolledInventoryOverlay(double mouseX, double mouseY, double amount, int guiX, int guiY, int backgroundWidth) {
        InventoryManager module = ModuleManager.INSTANCE.getModule(InventoryManager.class);
        if (module != null && module.isEnabled()) {
            InventoryPresetManager$Preset preset = getActivePreset();
            List<InventoryPresetManager$MissingEntry> entries = InventoryPresetManager.INSTANCE.getMissingEntries(preset);
            int panelX = guiX + backgroundWidth + 8;
            int panelY = guiY + 8;
            int panelW = 78;
            int panelH = Math.max(46, Math.min(142, entries.size() * 21 + 25));
            if (!(mouseX < panelX) && !(mouseX > panelX + panelW) && !(mouseY < panelY) && !(mouseY > panelY + panelH)) {
                int maxScroll = Math.max(0, entries.size() * 21 - (panelH - 26));
                missingScroll = Math.max(0.0F, Math.min(maxScroll, missingScroll - (float)amount * 18.0F));
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private static void drawTinyStack(DrawContext context, ItemStack stack, int x, int y) {
        RenderUtil.drawRoundedRect(context, x - 1, y - 1, 18.0F, 18.0F, 3.0F, new Color(255, 255, 255, 18).getRGB());
        if (!stack.isEmpty()) {
            context.drawItem(stack, x, y);
        }
    }
}
