package com.example.auravisuals.module.render;

import com.example.auravisuals.client.gui.render.TextRender;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.NumberSetting;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;

public class EffectRemind extends Module {
    public static final NumberSetting triggerTime = new NumberSetting("Время до окончания (старт)", 5.0, 1.0, 60.0, 0.5);
    public static final NumberSetting displayDuration = new NumberSetting("Длительность показа", 5.0, 1.0, 30.0, 0.5);
    public static final NumberSetting offsetY = new NumberSetting("Смещение Y", 20.0, 0.0, 80.0, 1.0);
    public static final NumberSetting iconSize = new NumberSetting("Размер иконки", 13.0, 8.0, 32.0, 1.0);
    public static final NumberSetting gap = new NumberSetting("Интервал", 4.0, 0.0, 16.0, 1.0);
    public static final BooleanSetting showTimer = new BooleanSetting("Отображать время", true);
    public static final BooleanSetting pulse = new BooleanSetting("Пульсация", true);
    public static final BooleanSetting onlyBeneficial = new BooleanSetting("Только баффы", false);
    final Map<RegistryEntry<StatusEffect>, Long> triggeredAt = new HashMap<>();

    public EffectRemind() {
        super("EffectRemind", "Отображает под прицелом эффекты, которые скоро закончатся", Category.VISUALS);
        this.setEnabledSilently(true);
    }

    @Override
    public void onHudRender(DrawContext context) {
        if (this.mc.player != null && !this.mc.options.hudHidden) {
            Collection<StatusEffectInstance> activeEffects = this.mc.player.getStatusEffects();
            if (activeEffects != null && !activeEffects.isEmpty()) {
                long now = System.currentTimeMillis();
                float triggerSec = triggerTime.getValue().floatValue();
                float maxDisplaySec = displayDuration.getValue().floatValue();
                ArrayList<EffectRemind$ExpiringEntry> toRender = new ArrayList<>();
                HashSet<RegistryEntry> currentTypes = new HashSet<>();

                for (StatusEffectInstance instance : activeEffects) {
                    if (!instance.isInfinite()) {
                        RegistryEntry type = instance.getEffectType();
                        currentTypes.add(type);
                        if (!onlyBeneficial.getValue() || ((StatusEffect)type.value()).getCategory() == StatusEffectCategory.BENEFICIAL) {
                            float remainingSeconds = instance.getDuration() / 20.0F;
                            if (remainingSeconds <= triggerSec && remainingSeconds > 0.0F) {
                                Long firstTrigger = this.triggeredAt.putIfAbsent(type, now);
                                long l = firstTrigger != null ? firstTrigger : now;
                                long startTime = l;
                                float elapsedSec = (float)(now - startTime) / 1000.0F;
                                if (elapsedSec <= maxDisplaySec) {
                                    toRender.add(new EffectRemind$ExpiringEntry(instance, remainingSeconds));
                                }
                            } else {
                                this.triggeredAt.remove(type);
                            }
                        }
                    }
                }

                this.triggeredAt.keySet().removeIf(t -> !currentTypes.contains(t));
                if (!toRender.isEmpty()) {
                    float size = iconSize.getValue().floatValue();
                    float spacing = gap.getValue().floatValue();
                    int count = toRender.size();
                    float totalWidth = count * size + (count - 1) * spacing;
                    float screenW = context.getScaledWindowWidth();
                    float screenH = context.getScaledWindowHeight();
                    float startX = (screenW - totalWidth) / 2.0F;
                    float startY = screenH / 2.0F + offsetY.getValue().floatValue();
                    float pulseFactor = 1.0F;
                    if (pulse.getValue()) {
                        pulseFactor = 1.0F + (float)Math.sin(now / 120.0) * 0.1F;
                    }

                    for (int i = 0; i < count; i++) {
                        EffectRemind$ExpiringEntry entry = toRender.get(i);
                        float x = startX + i * (size + spacing);
                        float y = startY;
                        context.getMatrices().pushMatrix();
                        if (pulseFactor != 1.0F) {
                            float cx = x + size / 2.0F;
                            float cy = y + size / 2.0F;
                            context.getMatrices().translate(cx, cy);
                            context.getMatrices().scale(pulseFactor, pulseFactor);
                            context.getMatrices().translate(-cx, -cy);
                        }

                        com.example.auravisuals.client.gui.hud.Hud.effect(context, entry.instance.getEffectType(), x, y, size);
                        if (showTimer.getValue()) {
                            int secInt = Math.max(0, (int)Math.ceil(entry.remainingSeconds));
                            String timerStr = secInt + "s";
                            float textScale = Math.max(5.5F, size * 0.42F);
                            float tw = TextRender.width(timerStr, textScale);
                            float tx = x + (size - tw) / 2.0F;
                            float ty = y + size + textScale + 1.0F;
                            int textColor = secInt <= 2 ? -48060 : -1;
                            TextRender.draw(context, timerStr, tx, ty, textScale, textColor);
                        }

                        context.getMatrices().popMatrix();
                    }
                }
            } else {
                this.triggeredAt.clear();
            }
        }
    }
}
