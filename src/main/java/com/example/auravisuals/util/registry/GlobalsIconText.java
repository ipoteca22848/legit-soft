package com.example.auravisuals.util.registry;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.StyleSpriteSource.Font;
import net.minecraft.util.Identifier;

public final class GlobalsIconText {
    private static final String GLYPH = "\ue000";
    private static final Font FONT = new Font(Identifier.of("auravisuals", "globals_icon"));

    private GlobalsIconText() {
    }

    public static Text prefix(Text original) {
        if (original != null && !original.getString().startsWith("\ue000")) {
            MutableText icon = Text.literal("\ue000").styled(style -> style.withFont(FONT));
            return Text.empty().append(icon).append(Text.literal(" ")).append(original.copy());
        } else {
            return original;
        }
    }
}
