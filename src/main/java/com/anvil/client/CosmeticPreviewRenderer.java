package com.anvil.client;
import java.util.Set;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import com.example.auravisuals.cosmetics.*;
import com.example.auravisuals.util.render.RenderUtil;
/** Original cosmetic model projection, independent of the old screen. */
public final class CosmeticPreviewRenderer {
    private static final Set<String> HIGHER_WING_PREVIEWS = Set.of("wings_3_81b06a4b", "wings_4_ac93f2ee");
    private static final Set<String> LOWER_WING_PREVIEWS = Set.of(
        "wings_7_ee401e1a", "wings_8_ee401e1a", "wings_10_bc304070", "wings_12_f684c6fe", "wings_13_04875af2", "wings_14_19ec3eb9"
    );
    private static final Set<String> OPPOSITE_VERTICAL_WING_PREVIEWS = Set.of(
        "wings_3_81b06a4b", "wings_4_ac93f2ee", "wings_8_ee401e1a", "wings_9_ee401e1a", "wings_13_04875af2", "wings_22_3dfa663d", "wings_23_28b34740"
    );
    public static void render(DrawContext context, CosmeticItem item, float x, float y, float w, float h, boolean hovered) {
        if (item != null) {
            CosmeticsManager.INSTANCE.ensureModelLoaded(item);
            BedrockModel model = item.getModel();
            if (model == null) {
                RenderUtil.drawRoundedTexture(context, item.getPreviewId(), x, y, w, h, 8.0F, true, true, true, true, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
            } else {
                float width = Math.max(0.01F, model.maxX - model.minX);
                float height = Math.max(0.01F, model.maxY - model.minY);
                float depth = Math.max(0.01F, model.maxZ - model.minZ);
                boolean isWings = "wings".equalsIgnoreCase(item.getCategory());
                boolean isHat = "hats".equalsIgnoreCase(item.getCategory());
                boolean isBack = "back".equalsIgnoreCase(item.getCategory());
                float autoYaw = isWings && depth > width * 1.35F ? 90.0F : 0.0F;
                float boundsW = autoYaw != 0.0F ? depth : width;
                float widthFill = isHat ? 0.9F : (isBack ? 0.86F : 0.84F);
                float heightFill = isHat ? 0.88F : 0.84F;
                float scale = Math.min(w * widthFill / Math.max(0.01F, boundsW), h * heightFill / Math.max(0.01F, height));
                float cx = (model.minX + model.maxX) * 0.5F;
                float cy = (model.minY + model.maxY) * 0.5F;
                float cz = (model.minZ + model.maxZ) * 0.5F;
                float screenCx = x + w * 0.5F;
                float previewOffsetY = HIGHER_WING_PREVIEWS.contains(item.getSlug())
                    ? -8.0F
                    : ("wings_7_ee401e1a".equals(item.getSlug()) ? 10.0F : (LOWER_WING_PREVIEWS.contains(item.getSlug()) ? 4.0F : 0.0F));
                float screenCy = y + h * 0.52F + previewOffsetY;
                long time = System.currentTimeMillis();
                float animTime = (float)(time % 100000L) * 0.001F;
                MatrixStack matrices = new MatrixStack();
                matrices.push();
                matrices.translate(screenCx, screenCy, 150.0F);
                matrices.scale(scale, -scale, scale);
                if (isWings) {
                    float hoverYaw = hovered ? (float)Math.sin(time % 3000L / 3000.0 * Math.PI * 2.0) * 14.0F : 0.0F;
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F + autoYaw + hoverYaw));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(hovered ? 6.0F : 2.0F));
                    if (OPPOSITE_VERTICAL_WING_PREVIEWS.contains(item.getSlug())) {
                        matrices.scale(-1.0F, 1.0F, 1.0F);
                    } else {
                        matrices.scale(1.0F, -1.0F, 1.0F);
                    }
                } else {
                    float hoverYaw = hovered ? (float)(time % 4200L / 4200.0 * 360.0) : 0.0F;
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F + hoverYaw));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(isHat ? 5.0F : 8.0F));
                    if ("hats_2_cf6887c0".equals(item.getSlug())) {
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-90.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
                    }
                }

                matrices.translate(-cx, -cy, -cz);
                if (!model.renderGui(context, item.getTextureId(), matrices, item.getAnimation(), animTime)) {
                    RenderUtil.drawRoundedTexture(context, item.getPreviewId(), x, y, w, h, 8.0F, true, true, true, true, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F, 1.0F);
                }

                matrices.pop();
            }
        }
    }

}
