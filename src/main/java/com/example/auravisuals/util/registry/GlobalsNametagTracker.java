package com.example.auravisuals.util.registry;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.text.Text;

public final class GlobalsNametagTracker {
    private static final Map<Text, Boolean> LABELS = Collections.synchronizedMap(new WeakHashMap<>());

    private GlobalsNametagTracker() {
    }

    public static void mark(Text text) {
        if (text != null) {
            LABELS.put(text, Boolean.TRUE);
        }
    }

    public static boolean consume(Text text) {
        return text != null && LABELS.remove(text) != null;
    }
}
