package com.example.auravisuals.module.misc;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BindSetting;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public class FTHelper extends Module {
    public final BindSetting disorientationKey = new BindSetting("Кнопка дезориентации", -1);
    public final BindSetting trapKey = new BindSetting("Кнопка трапки", -1);
    public final BindSetting blatantKey = new BindSetting("Кнопка явной пыли", -1);
    public final BindSetting auraKey = new BindSetting("Кнопка божьей ауры", -1);
    public final BindSetting plastKey = new BindSetting("Кнопка пласта", -1);
    public final BooleanSetting drawRadius = new BooleanSetting("Рисовать радиус", false);
    public final NumberSetting alpha = new NumberSetting("Прозрачность", 100.0, 0.0, 255.0, 1.0);
    final Map<BindSetting, FTHelper$Action> keyActionMap = new HashMap<>();
    final Map<BindSetting, Boolean> keyStateMap = new HashMap<>();

    public FTHelper() {
        super("FT Helper", "Помощник для FunTime", Category.MISC);
        this.addSettings(this.disorientationKey, this.trapKey, this.blatantKey, this.auraKey, this.plastKey, this.drawRadius, this.alpha);
        this.keyActionMap.put(this.disorientationKey, FTHelper$Action.DISORIENTATION);
        this.keyActionMap.put(this.trapKey, FTHelper$Action.TRAP);
        this.keyActionMap.put(this.blatantKey, FTHelper$Action.BLATANT);
        this.keyActionMap.put(this.auraKey, FTHelper$Action.AURA);
        this.keyActionMap.put(this.plastKey, FTHelper$Action.PLAST);
    }

    @Override
    public void onDisable() {
        this.keyStateMap.clear();
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.world != null && this.mc.getNetworkHandler() != null) {
            boolean screenOpen = this.mc.currentScreen != null;

            for (Entry<BindSetting, FTHelper$Action> entry : this.keyActionMap.entrySet()) {
                BindSetting bind = entry.getKey();
                FTHelper$Action action = entry.getValue();
                int key = bind.getValue();
                if (key != -1) {
                    boolean isPressed = !screenOpen && BindSetting.isPressed(this.mc.getWindow().getHandle(), key);
                    boolean wasPressed = this.keyStateMap.getOrDefault(bind, false);
                    if (isPressed && !wasPressed) {
                        this.startAction(action);
                    }

                    this.keyStateMap.put(bind, isPressed);
                }
            }
        } else {
            this.keyStateMap.clear();
        }
    }

    void startAction(FTHelper$Action action) {
        int slot = this.findItemSlot(action.item);
        if (slot != -1) {
            this.mc.player.getInventory().setSelectedSlot(slot);
            this.mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    int findItemSlot(Item item) {
        for (int i = 0; i < 9; i++) {
            if (this.mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }

        return -1;
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.drawRadius.getValue() && this.mc.player != null) {
            Item mainItem = this.mc.player.getMainHandStack().getItem();
            Item offItem = this.mc.player.getOffHandStack().getItem();
            Item activeItem = null;
            if (this.hasRadius(mainItem)) {
                activeItem = mainItem;
            } else if (this.hasRadius(offItem)) {
                activeItem = offItem;
            }

            if (activeItem != null) {
                if (activeItem != Items.NETHERITE_SCRAP && activeItem != Items.DRIED_KELP) {
                    float radius = this.getRadius(activeItem);
                    if (radius > 0.0F) {
                        this.drawCircle(context, radius);
                    }
                } else {
                    this.drawTrapBox(context);
                }
            }
        }
    }

    boolean isPlayerInRadius(float radius) {
        if (this.mc.player != null && this.mc.world != null) {
            for (PlayerEntity player : this.mc.world.getPlayers()) {
                if (player != this.mc.player && this.mc.player.distanceTo(player) <= radius) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }

    boolean hasRadius(Item item) {
        return item == Items.ENDER_EYE
            || item == Items.SUGAR
            || item == Items.PHANTOM_MEMBRANE
            || item == Items.TNT
            || item == Items.NETHERITE_SCRAP
            || item == Items.DRIED_KELP
            || item == Items.SNOWBALL;
    }

    float getRadius(Item item) {
        if (item == Items.ENDER_EYE) {
            return 10.0F;
        } else if (item == Items.SUGAR) {
            return 10.0F;
        } else if (item == Items.PHANTOM_MEMBRANE) {
            return 2.0F;
        } else {
            return item != Items.TNT && item != Items.SNOWBALL ? 0.0F : 7.0F;
        }
    }

    void drawCircle(WorldRenderContext context, float radius) {
        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getCameraPos();
        float tickDelta = context.tickCounter().getTickProgress(true);
        double x = MathHelper.lerp(tickDelta, this.mc.player.lastRenderX, this.mc.player.getX()) - cameraPos.x;
        double y = MathHelper.lerp(tickDelta, this.mc.player.lastRenderY, this.mc.player.getY()) - cameraPos.y;
        double z = MathHelper.lerp(tickDelta, this.mc.player.lastRenderZ, this.mc.player.getZ()) - cameraPos.z;
        boolean danger = this.isPlayerInRadius(radius);
        float red = danger ? 0.0F : 1.0F;
        float green = danger ? 0.8F : 0.0F;
        float blue = 0.0F;
        float alphaValue = this.alpha.getValue().floatValue() / 255.0F;
        int segments = 64;
        matrices.push();

        try {
            matrices.translate(x, y, z);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, 0.0F, 0.1F, 0.0F).color(red, green, blue, alphaValue);

            for (int i = 0; i <= segments; i++) {
                double angle = Math.toRadians(i * 360.0 / segments);
                float px = (float)(Math.cos(angle) * radius);
                float pz = (float)(Math.sin(angle) * radius);
                buffer.vertex(matrix, px, 0.1F, pz).color(red, green, blue, alphaValue);
            }

            AuraRender.drawColor(buffer, AuraRender.POS_COLOR_TRI_FAN_NODEPTH);
            buffer = tessellator.begin(DrawMode.DEBUG_LINE_STRIP, VertexFormats.POSITION_COLOR);

            for (int var32 = 0; var32 <= segments; var32++) {
                double angle = Math.toRadians(var32 * 360.0 / segments);
                float px = (float)(Math.cos(angle) * radius);
                float pz = (float)(Math.sin(angle) * radius);
                buffer.vertex(matrix, px, 0.1F, pz).color(red, green, blue, 1.0F);
            }

            AuraRender.drawColorLines(buffer, AuraRender.POS_COLOR_LINE_STRIP_NODEPTH, 3.0F);
        } finally {
            matrices.pop();
        }
    }

    void drawTrapBox(WorldRenderContext context) {
        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getCameraPos();
        int bx = MathHelper.floor(this.mc.player.getX());
        int by = MathHelper.floor(this.mc.player.getY());
        int bz = MathHelper.floor(this.mc.player.getZ());
        double ox = bx - cameraPos.x;
        double oy = by - cameraPos.y;
        double oz = bz - cameraPos.z;
        boolean danger = false;

        for (PlayerEntity player : this.mc.world.getPlayers()) {
            if (player != this.mc.player && this.mc.player.distanceTo(player) <= 1.5) {
                danger = true;
                break;
            }
        }

        float red = danger ? 0.0F : 1.0F;
        float green = danger ? 0.8F : 0.0F;
        float blue = 0.0F;
        float fillAlpha = this.alpha.getValue().floatValue() / 255.0F * 0.3F;
        float lineAlpha = 1.0F;
        float x0 = -2.0F;
        float z0 = -2.0F;
        float x1 = 3.0F;
        float z1 = 3.0F;
        float wallHeight = 4.0F;
        matrices.push();

        try {
            matrices.translate(ox, oy, oz);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            buffer.vertex(matrix, x0, 0.01F, z0).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x0, 0.01F, z1).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x1, 0.01F, z1).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x1, 0.01F, z0).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x0, wallHeight, z0).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x1, wallHeight, z0).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x1, wallHeight, z1).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x0, wallHeight, z1).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x0, 0.0F, z0).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x0, wallHeight, z0).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x0, wallHeight, z1).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x0, 0.0F, z1).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x1, 0.0F, z0).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x1, 0.0F, z1).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x1, wallHeight, z1).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x1, wallHeight, z0).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x0, 0.0F, z0).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x1, 0.0F, z0).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x1, wallHeight, z0).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x0, wallHeight, z0).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x0, 0.0F, z1).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x0, wallHeight, z1).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x1, wallHeight, z1).color(red, green, blue, fillAlpha);
            buffer.vertex(matrix, x1, 0.0F, z1).color(red, green, blue, fillAlpha);
            AuraRender.drawColor(buffer, AuraRender.POS_COLOR_QUADS_NODEPTH);
            buffer = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            this.drawEdgeLine(buffer, matrix, x0, 0.0F, z0, x1, 0.0F, z0, red, green, blue, lineAlpha);
            this.drawEdgeLine(buffer, matrix, x1, 0.0F, z0, x1, 0.0F, z1, red, green, blue, lineAlpha);
            this.drawEdgeLine(buffer, matrix, x1, 0.0F, z1, x0, 0.0F, z1, red, green, blue, lineAlpha);
            this.drawEdgeLine(buffer, matrix, x0, 0.0F, z1, x0, 0.0F, z0, red, green, blue, lineAlpha);
            this.drawEdgeLine(buffer, matrix, x0, wallHeight, z0, x1, wallHeight, z0, red, green, blue, lineAlpha);
            this.drawEdgeLine(buffer, matrix, x1, wallHeight, z0, x1, wallHeight, z1, red, green, blue, lineAlpha);
            this.drawEdgeLine(buffer, matrix, x1, wallHeight, z1, x0, wallHeight, z1, red, green, blue, lineAlpha);
            this.drawEdgeLine(buffer, matrix, x0, wallHeight, z1, x0, wallHeight, z0, red, green, blue, lineAlpha);
            this.drawEdgeLine(buffer, matrix, x0, 0.0F, z0, x0, wallHeight, z0, red, green, blue, lineAlpha);
            this.drawEdgeLine(buffer, matrix, x1, 0.0F, z0, x1, wallHeight, z0, red, green, blue, lineAlpha);
            this.drawEdgeLine(buffer, matrix, x1, 0.0F, z1, x1, wallHeight, z1, red, green, blue, lineAlpha);
            this.drawEdgeLine(buffer, matrix, x0, 0.0F, z1, x0, wallHeight, z1, red, green, blue, lineAlpha);
            AuraRender.drawColorLines(buffer, AuraRender.POS_COLOR_LINES_NODEPTH, 2.0F);
        } finally {
            matrices.pop();
        }
    }

    void drawEdgeLine(
        BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float red, float green, float blue, float alphaValue
    ) {
        buffer.vertex(matrix, x1, y1, z1).color(red, green, blue, alphaValue);
        buffer.vertex(matrix, x2, y2, z2).color(red, green, blue, alphaValue);
    }
}
