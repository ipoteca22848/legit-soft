package com.example.auravisuals.module.misc;

import com.example.auravisuals.command.CommandManager;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Formatting;

public class PickUpLogger extends Module {
    private static final long DUPLICATE_WINDOW_MS = 900L;
    private final BooleanSetting donorOnly = new BooleanSetting("Только донатерские", false);
    private final Map<String, Long> recentPickups = new LinkedHashMap<>();

    public PickUpLogger() {
        super("PickUpLogger", "Показывает информацию о поднятых предметах", Category.MISC);
        this.addSettings(this.donorOnly);
    }

    @Override
    public void onDisable() {
        this.recentPickups.clear();
    }

    public void handlePickup(ItemStack stack, int entityId, int amount) {
        if (this.isEnabled() && stack != null && !stack.isEmpty()) {
            String displayName = sanitizeName(stack.getName().getString());
            if (!displayName.isEmpty()) {
                if (!this.donorOnly.getValue() || isDonorItemName(displayName)) {
                    long now = System.currentTimeMillis();
                    this.cleanupRecentPickups(now);
                    String itemId = Registries.ITEM.getId(stack.getItem()).toString();
                    String signature = entityId + "|" + itemId + "|" + amount + "|" + displayName;
                    Long lastSeenAt = this.recentPickups.get(signature);
                    if (lastSeenAt == null || now - lastSeenAt >= 900L) {
                        this.recentPickups.put(signature, now);
                        CommandManager.addChatMessage("Поднят предмет: " + displayName + " [x" + stack.getCount() + "]");
                    }
                }
            }
        }
    }

    private void cleanupRecentPickups(long now) {
        Iterator<Entry<String, Long>> iterator = this.recentPickups.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<String, Long> entry = iterator.next();
            if (now - entry.getValue() > 900L) {
                iterator.remove();
            }
        }
    }

    private static String sanitizeName(String name) {
        String stripped = Formatting.strip(name);
        if (stripped == null) {
            stripped = name;
        }

        return stripped.replaceAll("\\s+", " ").trim();
    }

    private static boolean isDonorItemName(String displayName) {
        if (displayName != null && !displayName.isEmpty()) {
            String normalized = displayName.toLowerCase(Locale.ROOT);
            return displayName.contains("★")
                || normalized.contains("круш")
                || normalized.contains("талис")
                || normalized.contains("сфера")
                || normalized.contains("шар")
                || normalized.contains("осколок");
        } else {
            return false;
        }
    }
}
