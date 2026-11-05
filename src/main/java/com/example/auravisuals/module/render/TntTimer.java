package com.example.auravisuals.module.render;

import com.example.auravisuals.client.gui.font.FontManager;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.util.render.RenderUtil;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.TntEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public final class TntTimer extends Module {
    static final double DETECTION_RADIUS = 10.0;
    static final double DETECTION_RADIUS_SQUARED = 100.0;
    static final int PANEL_HEIGHT = 36;
    static final int PANEL_GAP = 6;
    static final int MIN_PANEL_WIDTH = 132;
    static final ItemStack TNT_ICON = new ItemStack(Items.TNT);
    final Map<UUID, TntTimer$TimerEntry> entries = new HashMap<>();
    long nextOrder;
    long lastFrameNanos;

    public TntTimer() {
        super("TNT Timer", "Показывает время до взрыва динамита", Category.VISUALS);
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.world != null) {
            for (TntTimer$TimerEntry entry : this.entries.values()) {
                Entity entity = this.mc.world.getEntityById(entry.entityId);
                TntEntity tnt2;
                if (entity instanceof TntEntity && !(tnt2 = (TntEntity)entity).isRemoved() && tnt2.getFuse() > 0) {
                    updateEntry(entry, tnt2);
                } else {
                    entry.leaving = true;
                }
            }

            for (TntEntity tnt3 : this.mc
                .world
                .getEntitiesByClass(
                    TntEntity.class,
                    this.mc.player.getBoundingBox().expand(10.0),
                    tnt -> !tnt.isRemoved() && tnt.getFuse() > 0 && tnt.squaredDistanceTo(this.mc.player) <= 100.0
                )) {
                TntTimer$TimerEntry entry = this.entries.get(tnt3.getUuid());
                if (entry == null) {
                    entry = new TntTimer$TimerEntry(tnt3.getUuid(), tnt3.getId(), ++this.nextOrder, displayName(tnt3));
                    this.entries.put(tnt3.getUuid(), entry);
                }

                updateEntry(entry, tnt3);
            }
        } else {
            this.clearEntries();
        }
    }

    @Override
    public void onHudRender(DrawContext context) {
        if (!this.entries.isEmpty() && this.mc.player != null && this.mc.world != null && !this.mc.options.hudHidden) {
            long now = System.nanoTime();
            float deltaSeconds = this.lastFrameNanos == 0L ? 0.016666668F : MathHelper.clamp((float)(now - this.lastFrameNanos) / 1.0E9F, 0.0F, 0.05F);
            this.lastFrameNanos = now;
            int baseY = Math.max(42, context.getScaledWindowHeight() / 7);
            ArrayList<TntTimer$TimerEntry> ordered = new ArrayList<>(this.entries.values());
            ordered.sort(Comparator.<TntTimer$TimerEntry>comparingLong(entry -> entry.order).reversed());
            int activeIndex = 0;

            for (TntTimer$TimerEntry entry2 : ordered) {
                float targetY;
                if (entry2.leaving) {
                    targetY = Float.isNaN(entry2.currentY) ? baseY : entry2.currentY;
                } else {
                    targetY = baseY + activeIndex * 42;
                    activeIndex++;
                }

                if (Float.isNaN(entry2.currentY)) {
                    entry2.currentY = targetY - 10.0F;
                }

                entry2.currentY = smooth(entry2.currentY, targetY, 13.0F, deltaSeconds);
                entry2.alpha = smooth(entry2.alpha, entry2.leaving ? 0.0F : 1.0F, 11.0F, deltaSeconds);
                this.renderEntry(context, entry2);
            }

            this.entries.values().removeIf(entry -> entry.leaving && entry.alpha < 0.025F);
        }
    }

    @Override
    public void onDisable() {
        this.clearEntries();
    }

    void renderEntry(DrawContext context, TntTimer$TimerEntry entry) {
        float tickDelta = this.mc.getRenderTickCounter().getTickProgress(true);
        float remainingSeconds = Math.max(0.0F, (entry.fuseTicks - tickDelta) / 20.0F);
        String timerText = String.format(Locale.US, "%.1f сек", remainingSeconds);
        int panelWidth = Math.max(132, FontManager.getWidth(entry.name) + 50);
        float panelX = (context.getScaledWindowWidth() - panelWidth) * 0.5F;
        float panelY = entry.currentY;
        float animationScale = 0.92F + 0.08F * entry.alpha;
        float centerX = panelX + panelWidth * 0.5F;
        float centerY = panelY + 18.0F;
        context.getMatrices().pushMatrix();

        try {
            context.getMatrices().translate(centerX, centerY);
            context.getMatrices().scale(animationScale, animationScale);
            context.getMatrices().translate(-centerX, -centerY);
            int background = withAlpha(1052950, Math.round(166.0F * entry.alpha));
            int border = withAlpha(16777215, Math.round(22.0F * entry.alpha));
            RenderUtil.drawRoundedRect(context, panelX - 0.7F, panelY - 0.7F, panelWidth + 1.4F, 37.4F, 8.7F, border);
            RenderUtil.drawRoundedRect(context, panelX, panelY, panelWidth, 36.0F, 8.0F, background);
            float iconX = panelX + 9.0F;
            float iconY = panelY + 8.0F;
            context.getMatrices().pushMatrix();

            try {
                context.getMatrices().translate(iconX, iconY);
                context.getMatrices().scale(1.25F, 1.25F);
                context.drawItem(TNT_ICON, 0, 0);
            } finally {
                context.getMatrices().popMatrix();
            }

            float textX = panelX + 38.0F;
            FontManager.drawCustomText(context, entry.name, textX, panelY + 6.0F, withAlpha(16777215, Math.round(255.0F * entry.alpha)));
            FontManager.drawCustomText(context, timerText, textX, panelY + 20.0F, withAlpha(timerColor(remainingSeconds), Math.round(235.0F * entry.alpha)));
        } finally {
            context.getMatrices().popMatrix();
        }
    }

    static void updateEntry(TntTimer$TimerEntry entry, TntEntity tnt) {
        entry.entityId = tnt.getId();
        entry.fuseTicks = tnt.getFuse();
        entry.name = displayName(tnt);
        entry.leaving = false;
    }

    static String displayName(TntEntity tnt) {
        Text customName = tnt.getCustomName();
        return customName != null && !customName.getString().isBlank() ? customName.getString() : "Динамит";
    }

    static float smooth(float current, float target, float speed, float deltaSeconds) {
        float factor = 1.0F - (float)Math.exp(-speed * deltaSeconds);
        return current + (target - current) * factor;
    }

    static int timerColor(float seconds) {
        if (seconds <= 1.0F) {
            return 16737380;
        } else {
            return seconds <= 2.0F ? 16757854 : 15198702;
        }
    }

    static int withAlpha(int rgb, int alpha) {
        return MathHelper.clamp(alpha, 0, 255) << 24 | rgb & 16777215;
    }

    void clearEntries() {
        this.entries.clear();
        this.lastFrameNanos = 0L;
    }
}
