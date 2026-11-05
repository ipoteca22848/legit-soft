package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BindSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Formatting;

public class AucSearch extends Module {
    private final BindSetting searchBind = new BindSetting("Кнопка поиска", -1);
    private boolean searchBindPressed;

    public AucSearch() {
        super("AucSearch", "Поиск предмета на аукционе", Category.MISC);
        this.addSettings(this.searchBind);
    }

    @Override
    public void onDisable() {
        this.searchBindPressed = false;
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.world != null && this.mc.getNetworkHandler() != null) {
            int key = this.searchBind.getValue();
            if (key != -1 && this.mc.currentScreen == null) {
                boolean pressed = BindSetting.isPressed(this.mc.getWindow().getHandle(), key);
                if (pressed && !this.searchBindPressed) {
                    this.searchHeldItem();
                }

                this.searchBindPressed = pressed;
            } else {
                this.searchBindPressed = false;
            }
        } else {
            this.searchBindPressed = false;
        }
    }

    private void searchHeldItem() {
        ItemStack stack = this.mc.player.getMainHandStack();
        if (!stack.isEmpty()) {
            String query = sanitizeQuery(stack.getName().getString());
            if (!query.isEmpty()) {
                this.mc.player.networkHandler.sendChatCommand("ah search " + query);
            }
        }
    }

    public static boolean searchStack(MinecraftClient client, ItemStack stack) {
        if (client != null && client.player != null && client.player.networkHandler != null && stack != null && !stack.isEmpty()) {
            String query = sanitizeQuery(stack.getName().getString());
            if (query.isEmpty()) {
                return false;
            }

            client.player.networkHandler.sendChatCommand("ah search " + query);
            return true;
        } else {
            return false;
        }
    }

    public static String sanitizeQuery(String raw) {
        String stripped = Formatting.strip(raw);
        if (stripped == null) {
            stripped = raw;
        }

        StringBuilder builder = new StringBuilder(stripped.length());
        boolean pendingSpace = false;
        int i = 0;

        while (i < stripped.length()) {
            int codePoint = stripped.codePointAt(i);
            i += Character.charCount(codePoint);
            if (Character.isLetterOrDigit(codePoint)) {
                if (pendingSpace && !builder.isEmpty()) {
                    builder.append(' ');
                }

                builder.appendCodePoint(codePoint);
                pendingSpace = false;
            } else if (Character.isWhitespace(codePoint)) {
                pendingSpace = builder.length() > 0;
            }
        }

        return builder.toString().trim();
    }
}
