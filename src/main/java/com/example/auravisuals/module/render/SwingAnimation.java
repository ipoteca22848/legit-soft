package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class SwingAnimation extends Module {
    private static final String MODE_JELLY = "Желейка";
    public static final ModeSetting swingType = new ModeSetting(
        "Тип взмаха", "Swipe", "Swipe", "Down", "Smooth", "Smooth 2", "Power", "Feast", "Twist", "Default", "Желейка"
    );
    public static final NumberSetting hitStrength = new NumberSetting("Сила взмаха", 1.0, 0.5, 2.0, 0.1);
    public static final NumberSetting swingSpeed = new NumberSetting("Длительность взмаха", 0.8, 0.5, 1.5, 0.1);
    public static final BooleanSetting onlySwing = new BooleanSetting("Только при взмахе", false);

    public SwingAnimation() {
        super("SwingAnimation", "Анимации удара", Category.VISUALS);
        this.addSettings(swingType, hitStrength, swingSpeed, onlySwing);
    }

    public static int getSwingDurationOverride() {
        float speed = MathHelper.clamp(swingSpeed.getFloatValue(), 0.5F, 1.5F);
        return Math.max(1, Math.round(6.0F / speed));
    }

    public static void applyAnimation(MatrixStack matrix, Hand hand, float swingProgress) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            boolean jelly = swingType.is("Желейка");
            if (hand == Hand.MAIN_HAND || jelly) {
                float baseX = 0.0F;
                float baseY = 0.0F;
                float baseZ = 0.0F;
                float amplitudeScale = 1.0F;
                int i = hand == Hand.MAIN_HAND ? 1 : -1;
                if (onlySwing.getValue() && client.player.handSwingTicks == 0) {
                    matrix.translate(i * baseX, baseY, baseZ);
                } else {
                    float sin1 = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
                    float sin2 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
                    float sinSmooth = MathHelper.sin(swingProgress * (float) Math.PI) * 0.5F;
                    float strength = hitStrength.getFloatValue();
                    switch (swingType.getValue()) {
                        case "Twist":
                            matrix.translate(i * baseX, baseY + 0.16F, baseZ);
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(80.0F * i));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -90.0F * strength * amplitudeScale));
                            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((sin1 - sin2) * 60.0F * i * strength * amplitudeScale));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F));
                            matrix.translate(0.0F, -0.1F, 0.05F);
                            break;
                        case "Swipe":
                            matrix.translate(baseX * i, baseY + 0.2F, baseZ);
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70.0F * i));
                            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * i));
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * sin1 * -5.0F * strength * amplitudeScale));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * sin1 * -120.0F * strength * amplitudeScale));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
                            break;
                        case "Default":
                            matrix.translate(i * baseX, baseY - sin2 * 0.5F * strength * amplitudeScale, baseZ);
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * i));
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45.0F * i));
                            break;
                        case "Down":
                            matrix.translate(i * baseX, baseY + 0.2F, baseZ);
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(76.0F * i));
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * -5.0F * strength * amplitudeScale));
                            matrix.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(sin2 * -100.0F * strength * amplitudeScale));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -155.0F * strength * amplitudeScale));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-100.0F));
                            break;
                        case "Smooth":
                            matrix.translate(i * baseX, baseY + 0.1F, baseZ);
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + sin1 * -20.0F * strength * amplitudeScale)));
                            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * sin2 * -20.0F * strength * amplitudeScale));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength * amplitudeScale));
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
                            matrix.translate(0.0, -0.1, 0.0);
                            break;
                        case "Smooth 2":
                            matrix.translate(i * baseX, baseY + 0.1F, baseZ);
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -80.0F * strength * amplitudeScale));
                            matrix.translate(0.0, -0.1, 0.0);
                            break;
                        case "Power":
                            matrix.translate(i * baseX, baseY + 0.2F, baseZ);
                            matrix.translate(-sinSmooth * sinSmooth * sin1 * i * strength * amplitudeScale, 0.0F, 0.0F);
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(61.0F * i));
                            matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sin2 * strength * amplitudeScale));
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * sin1 * -5.0F * strength * amplitudeScale));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * sin1 * -30.0F * strength * amplitudeScale));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sinSmooth * -60.0F * strength * amplitudeScale));
                            break;
                        case "Feast":
                            matrix.translate(i * baseX, baseY + 0.2F, baseZ);
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0F * i));
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(sin2 * 75.0F * i * strength * amplitudeScale));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(sin2 * -45.0F * strength * amplitudeScale));
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(30.0F * i));
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F));
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35.0F * i));
                            break;
                        case "Желейка":
                            float jellyWave = MathHelper.sin(swingProgress * (float) Math.PI);
                            matrix.translate(i * (baseX + jellyWave * 0.08F), baseY + 0.18F - sin2 * 0.1F, baseZ + sin1 * 0.08F);
                            matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (66.0F + sin2 * 28.0F * strength * amplitudeScale)));
                            matrix.multiply(
                                RotationAxis.POSITIVE_Z.rotationDegrees(i * ((sin1 - sin2) * 52.0F * strength * amplitudeScale + jellyWave * 12.0F))
                            );
                            matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-72.0F - sin2 * 64.0F * strength * amplitudeScale));
                            matrix.translate(0.0F, -0.12F * jellyWave, 0.0F);
                    }
                }
            }
        }
    }
}
