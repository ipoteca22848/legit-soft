package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.BooleanSetting;

public class ChatHelper extends Module {
    public static final BooleanSetting antiSpam = new BooleanSetting("Анти-Спам", true);
    public static final BooleanSetting keepHistory = new BooleanSetting("Сохранять историю", true);
    public static final BooleanSetting commandCorrection = new BooleanSetting("Исправление команд", true);
    private static final String RUSSIAN_LAYOUT = "ёйцукенгшщзхъфывапролджэячсмитьбюЁЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ";
    private static final String ENGLISH_LAYOUT = "`qwertyuiop[]asdfghjkl;'zxcvbnm,.~QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>";

    public ChatHelper() {
        super("Chat Helper", "Улучшение чата", Category.MISC);
        this.addSettings(antiSpam, keepHistory, commandCorrection);
    }

    public static String correctCommandLayout(String message) {
        if (message != null && message.startsWith("/") && commandCorrection.getValue()) {
            ChatHelper module = ModuleManager.INSTANCE.getModule(ChatHelper.class);
            return module != null && module.isEnabled() ? "/" + toEnglishLayout(message.substring(1)) : message;
        } else {
            return message;
        }
    }

    public static String toEnglishLayout(String text) {
        if (text != null && !text.isEmpty()) {
            StringBuilder corrected = new StringBuilder(text.length());

            for (int index = 0; index < text.length(); index++) {
                char character = text.charAt(index);
                int layoutIndex = "ёйцукенгшщзхъфывапролджэячсмитьбюЁЙЦУКЕНГШЩЗХЪФЫВАПРОЛДЖЭЯЧСМИТЬБЮ".indexOf(character);
                corrected.append(layoutIndex >= 0 ? "`qwertyuiop[]asdfghjkl;'zxcvbnm,.~QWERTYUIOP{}ASDFGHJKL:\"ZXCVBNM<>".charAt(layoutIndex) : character);
            }

            return corrected.toString();
        } else {
            return text;
        }
    }
}
