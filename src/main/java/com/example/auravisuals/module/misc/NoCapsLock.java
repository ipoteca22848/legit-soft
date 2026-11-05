package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import java.util.Locale;

public class NoCapsLock extends Module {
    public NoCapsLock() {
        super("NoCapsLock", "Не позволяет писать капсом", Category.MISC);
    }

    public static String normalizeOutgoingChat(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        if (!message.startsWith("/") && !message.startsWith(".")) {
            NoCapsLock module = ModuleManager.INSTANCE.getModule(NoCapsLock.class);
            if (module != null && module.isEnabled()) {
                int upper = 0;
                int lower = 0;

                for (int i = 0; i < message.length(); i++) {
                    char c = message.charAt(i);
                    if (Character.isUpperCase(c)) {
                        upper++;
                    } else if (Character.isLowerCase(c)) {
                        lower++;
                    }
                }

                return upper != 0 && upper > lower ? message.toLowerCase(Locale.ROOT) : message;
            } else {
                return message;
            }
        } else {
            return message;
        }
    }
}
