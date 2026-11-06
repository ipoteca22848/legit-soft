package com.example.auravisuals.client.gui.render;

import com.example.auravisuals.compat.Gui;
import com.example.auravisuals.util.render.RenderUtil;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IconRenderer {
    private static final Logger LOGGER = LoggerFactory.getLogger("anvil/IconRenderer");
    private static final Map<String, Identifier> ICONS = new HashMap<>();
    private static final Map<Character, float[]> ATLAS_UVS = new HashMap<>();

    public static void drawAtlasIcon(DrawContext context, char c, float x, float y, float width, float height, int color) {
        float[] uvs = ATLAS_UVS.get(c);
        if (uvs != null) {
            Identifier texture = Identifier.of("auravisuals", "icons/mainicons.png");
            Identifier renderTexture = RenderUtil.getWhiteMaskTexture(texture);
            drawIconRegion(context, renderTexture, x, y, width, height, uvs[0], uvs[1], uvs[2], uvs[3], color);
        }
    }

    public static void drawIcon(DrawContext context, String name, float x, float y, float width, float height, int color) {
        Identifier texture = ICONS.computeIfAbsent(name, n -> Identifier.of("auravisuals", "icons/" + n + ".png"));
        drawIcon(context, texture, x, y, width, height, color);
    }

    public static void drawIcon(DrawContext context, Identifier texture, float x, float y, float width, float height, int color) {
        int a = color >> 24 & 0xFF;
        if (a <= 0) {
            color = 0xFF000000 | color & 16777215;
        }

        Identifier renderTexture = RenderUtil.getWhiteMaskTexture(texture);
        Gui.texQuad(context, renderTexture, x, y, x + width, y + height, 0.0F, 0.0F, 1.0F, 1.0F, color);
    }

    public static void drawIconRegion(
        DrawContext context, Identifier texture, float x, float y, float width, float height, float u1, float v1, float u2, float v2, int color
    ) {
        Gui.tex(context, texture)
            .vertex(x, y + height, u1, v2, color)
            .vertex(x + width, y + height, u2, v2, color)
            .vertex(x + width, y, u2, v1, color)
            .vertex(x, y, u1, v1, color)
            .submit(context);
    }

    static {
        ATLAS_UVS.put('B', new float[]{0.806F, 0.4023F, 0.9831F, 0.6003F});
        ATLAS_UVS.put('C', new float[]{0.0013F, 0.3607F, 0.1992F, 0.543F});
        ATLAS_UVS.put('D', new float[]{0.0013F, 0.0013F, 0.2018F, 0.1576F});
        ATLAS_UVS.put('E', new float[]{0.2044F, 0.2018F, 0.4023F, 0.3997F});
        ATLAS_UVS.put('F', new float[]{0.4049F, 0.4831F, 0.6029F, 0.681F});
        ATLAS_UVS.put('G', new float[]{0.6055F, 0.4023F, 0.8034F, 0.5794F});
        ATLAS_UVS.put('H', new float[]{0.6055F, 0.582F, 0.8034F, 0.7799F});
        ATLAS_UVS.put('I', new float[]{0.806F, 0.0013F, 0.9961F, 0.1992F});
        ATLAS_UVS.put('J', new float[]{0.6055F, 0.2018F, 0.8034F, 0.3997F});
        ATLAS_UVS.put('K', new float[]{0.0013F, 0.1602F, 0.2018F, 0.3581F});
        ATLAS_UVS.put('L', new float[]{0.4049F, 0.6836F, 0.6029F, 0.8815F});
        ATLAS_UVS.put('M', new float[]{0.806F, 0.2018F, 0.9909F, 0.3997F});
        ATLAS_UVS.put('N', new float[]{0.4049F, 0.3372F, 0.6029F, 0.4805F});
        ATLAS_UVS.put('O', new float[]{0.4049F, 0.1914F, 0.6029F, 0.3346F});
        ATLAS_UVS.put('P', new float[]{0.4049F, 0.0013F, 0.6029F, 0.1888F});
        ATLAS_UVS.put('Q', new float[]{0.2044F, 0.8034F, 0.4023F, 0.9544F});
        ATLAS_UVS.put('R', new float[]{0.2044F, 0.6029F, 0.4023F, 0.8008F});
        ATLAS_UVS.put('S', new float[]{0.2044F, 0.4023F, 0.4023F, 0.6003F});
        ATLAS_UVS.put('T', new float[]{0.806F, 0.6029F, 0.9779F, 0.8008F});
        ATLAS_UVS.put('U', new float[]{0.6055F, 0.7826F, 0.7279F, 0.9805F});
        ATLAS_UVS.put('V', new float[]{0.0013F, 0.7461F, 0.1992F, 0.944F});
        ATLAS_UVS.put('W', new float[]{0.0013F, 0.5456F, 0.1992F, 0.7435F});
        ATLAS_UVS.put('X', new float[]{0.6055F, 0.0013F, 0.8034F, 0.1992F});
        ATLAS_UVS.put('Y', new float[]{0.2044F, 0.0013F, 0.4023F, 0.1992F});
    }
}
