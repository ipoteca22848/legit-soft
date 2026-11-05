package com.example.auravisuals.module.misc;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.util.Locale;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

public final class HWHelper extends Module {
    static final Identifier ETERNITY_MODEL = Identifier.of("talismanvisuals", "eternity");
    static final Identifier STINGER_MODEL = Identifier.of("talismanvisuals", "stinger");
    static final Identifier INFINITY_MODEL = Identifier.of("talismanvisuals", "infinity");
    public static HWHelper INSTANCE;
    public final BooleanSetting drawRadius = new BooleanSetting("Рисовать радиус", false);
    public final BooleanSetting highlightTalismans = new BooleanSetting("Подсвечивать талисманы", true);

    public HWHelper() {
        super("HW Helper", "Помощник для сервера HolyWorld", Category.MISC);
        INSTANCE = this;
        this.addSettings(this.highlightTalismans, this.drawRadius);
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.drawRadius.getValue() && this.mc.player != null && this.mc.world != null) {
            Item mainItem = this.mc.player.getMainHandStack().getItem();
            Item offItem = this.mc.player.getOffHandStack().getItem();
            Object activeItem = this.radiusFor(mainItem) != null ? mainItem : (this.radiusFor(offItem) != null ? offItem : null);
            HWHelper$RadiusBox radius = this.radiusFor((Item)activeItem);
            if (radius != null) {
                this.drawRadiusBox(context, radius);
            }
        }
    }

    HWHelper$RadiusBox radiusFor(Item item) {
        if (item == Items.NETHER_STAR) {
            return new HWHelper$RadiusBox(-30.0F, 30.0F, -2.0F, 30.0F, -30.0F, 30.0F);
        } else if (item == Items.PRISMARINE_SHARD) {
            return new HWHelper$RadiusBox(-4.0F, 4.0F, -2.0F, 3.0F, -4.0F, 4.0F);
        } else {
            return item == Items.POPPED_CHORUS_FRUIT ? new HWHelper$RadiusBox(-2.0F, 3.0F, 0.0F, 4.0F, -2.0F, 3.0F) : null;
        }
    }

    void drawRadiusBox(WorldRenderContext context, HWHelper$RadiusBox radius) {
        MatrixStack matrices = context.matrixStack();
        Vec3d cameraPos = context.camera().getCameraPos();
        int blockX = MathHelper.floor(this.mc.player.getX());
        int blockZ;
        int blockY;
        boolean danger = this.isPlayerInside(blockX, blockY = MathHelper.floor(this.mc.player.getY()), blockZ = MathHelper.floor(this.mc.player.getZ()), radius);
        float red = danger ? 0.0F : 1.0F;
        float green = danger ? 0.8F : 0.0F;
        float blue = 0.0F;
        float fillAlpha = 0.11764707F;
        matrices.push();

        try {
            matrices.translate(blockX - cameraPos.x, blockY - cameraPos.y, blockZ - cameraPos.z);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            addFace(
                buffer,
                matrix,
                radius.minX,
                radius.minY,
                radius.minZ,
                radius.minX,
                radius.minY,
                radius.maxZ,
                radius.maxX,
                radius.minY,
                radius.maxZ,
                radius.maxX,
                radius.minY,
                radius.minZ,
                red,
                green,
                blue,
                fillAlpha
            );
            addFace(
                buffer,
                matrix,
                radius.minX,
                radius.maxY,
                radius.minZ,
                radius.maxX,
                radius.maxY,
                radius.minZ,
                radius.maxX,
                radius.maxY,
                radius.maxZ,
                radius.minX,
                radius.maxY,
                radius.maxZ,
                red,
                green,
                blue,
                fillAlpha
            );
            addFace(
                buffer,
                matrix,
                radius.minX,
                radius.minY,
                radius.minZ,
                radius.minX,
                radius.maxY,
                radius.minZ,
                radius.minX,
                radius.maxY,
                radius.maxZ,
                radius.minX,
                radius.minY,
                radius.maxZ,
                red,
                green,
                blue,
                fillAlpha
            );
            addFace(
                buffer,
                matrix,
                radius.maxX,
                radius.minY,
                radius.minZ,
                radius.maxX,
                radius.minY,
                radius.maxZ,
                radius.maxX,
                radius.maxY,
                radius.maxZ,
                radius.maxX,
                radius.maxY,
                radius.minZ,
                red,
                green,
                blue,
                fillAlpha
            );
            addFace(
                buffer,
                matrix,
                radius.minX,
                radius.minY,
                radius.minZ,
                radius.maxX,
                radius.minY,
                radius.minZ,
                radius.maxX,
                radius.maxY,
                radius.minZ,
                radius.minX,
                radius.maxY,
                radius.minZ,
                red,
                green,
                blue,
                fillAlpha
            );
            addFace(
                buffer,
                matrix,
                radius.minX,
                radius.minY,
                radius.maxZ,
                radius.minX,
                radius.maxY,
                radius.maxZ,
                radius.maxX,
                radius.maxY,
                radius.maxZ,
                radius.maxX,
                radius.minY,
                radius.maxZ,
                red,
                green,
                blue,
                fillAlpha
            );
            AuraRender.drawColor(buffer, AuraRender.POS_COLOR_QUADS_NODEPTH);
            buffer = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
            addBoxEdges(buffer, matrix, radius, red, green, blue);
            AuraRender.drawColorLines(buffer, AuraRender.POS_COLOR_LINES_NODEPTH, 2.0F);
        } finally {
            matrices.pop();
        }
    }

    boolean isPlayerInside(int blockX, int blockY, int blockZ, HWHelper$RadiusBox radius) {
        for (PlayerEntity player : this.mc.world.getPlayers()) {
            if (player != this.mc.player) {
                double x = player.getX() - blockX;
                double y = player.getY() - blockY;
                double z = player.getZ() - blockZ;
                if (x >= radius.minX && x <= radius.maxX && y >= radius.minY && y <= radius.maxY && z >= radius.minZ && z <= radius.maxZ) {
                    return true;
                }
            }
        }

        return false;
    }

    static void addFace(
        BufferBuilder buffer,
        Matrix4f matrix,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float x3,
        float y3,
        float z3,
        float x4,
        float y4,
        float z4,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        vertex(buffer, matrix, x1, y1, z1, red, green, blue, alpha);
        vertex(buffer, matrix, x2, y2, z2, red, green, blue, alpha);
        vertex(buffer, matrix, x3, y3, z3, red, green, blue, alpha);
        vertex(buffer, matrix, x4, y4, z4, red, green, blue, alpha);
    }

    static void addBoxEdges(BufferBuilder buffer, Matrix4f matrix, HWHelper$RadiusBox box, float red, float green, float blue) {
        edge(buffer, matrix, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, red, green, blue);
        edge(buffer, matrix, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, red, green, blue);
        edge(buffer, matrix, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, red, green, blue);
        edge(buffer, matrix, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, red, green, blue);
        edge(buffer, matrix, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, red, green, blue);
        edge(buffer, matrix, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, red, green, blue);
        edge(buffer, matrix, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, red, green, blue);
        edge(buffer, matrix, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, red, green, blue);
        edge(buffer, matrix, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, red, green, blue);
        edge(buffer, matrix, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, red, green, blue);
        edge(buffer, matrix, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, red, green, blue);
        edge(buffer, matrix, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, red, green, blue);
    }

    static void edge(BufferBuilder buffer, Matrix4f matrix, float x1, float y1, float z1, float x2, float y2, float z2, float red, float green, float blue) {
        vertex(buffer, matrix, x1, y1, z1, red, green, blue, 1.0F);
        vertex(buffer, matrix, x2, y2, z2, red, green, blue, 1.0F);
    }

    static void vertex(BufferBuilder buffer, Matrix4f matrix, float x, float y, float z, float red, float green, float blue, float alpha) {
        buffer.vertex(matrix, x, y, z).color(red, green, blue, alpha);
    }

    public ItemStack applyTalismanVisual(ItemStack stack) {
        if (this.isEnabled() && this.highlightTalismans.getValue() && stack != null && stack.isOf(Items.TOTEM_OF_UNDYING)) {
            Identifier currentModel = stack.get(DataComponentTypes.ITEM_MODEL);
            if (isTalismanModel(currentModel)) {
                return stack;
            }

            NbtComponent customData = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (customData == null) {
                return stack;
            }

            NbtCompound nbt = customData.copyNbt();
            NbtCompound sphereEffect = nbt.getCompoundOrEmpty("sphereEffect");
            String name = sphereEffect.getString("name", "");
            String rank = sphereEffect.getString("rank", "");
            Identifier model = detectModel(rank, name, nbt.getCompoundOrEmpty("itemServiceId").getString("name", ""), nbt.toString());
            if (model == null) {
                return stack;
            }

            ItemStack visual = stack.copy();
            visual.set(DataComponentTypes.ITEM_MODEL, model);
            return visual;
        } else {
            return stack;
        }
    }

    static boolean isTalismanModel(Identifier model) {
        return ETERNITY_MODEL.equals(model) || STINGER_MODEL.equals(model) || INFINITY_MODEL.equals(model);
    }

    static Identifier detectModel(String rank, String name, String serviceId, String rawData) {
        if (equalsIgnoreCase(name, "Infinity") || endsWithIgnoreCase(serviceId, "_infinity")) {
            return INFINITY_MODEL;
        } else if (equalsIgnoreCase(name, "Stinger") || endsWithIgnoreCase(serviceId, "_stinger")) {
            return STINGER_MODEL;
        } else if (equalsIgnoreCase(name, "Eternity") || endsWithIgnoreCase(serviceId, "_eternity")) {
            return ETERNITY_MODEL;
        } else if (equalsIgnoreCase(rank, "STINGER")) {
            return STINGER_MODEL;
        } else if (equalsIgnoreCase(rank, "ETERNITY") && !equalsIgnoreCase(name, "Infinity") && !endsWithIgnoreCase(serviceId, "_infinity")) {
            return ETERNITY_MODEL;
        } else {
            String searchable = rawData == null ? "" : rawData.toLowerCase(Locale.ROOT);
            if (searchable.contains("infinity")) {
                return INFINITY_MODEL;
            } else if (searchable.contains("stinger")) {
                return STINGER_MODEL;
            } else {
                return searchable.contains("eternity") ? ETERNITY_MODEL : null;
            }
        }
    }

    static boolean equalsIgnoreCase(String value, String expected) {
        return value != null && expected.equalsIgnoreCase(value);
    }

    static boolean endsWithIgnoreCase(String value, String suffix) {
        return value != null && value.toLowerCase(Locale.ROOT).endsWith(suffix);
    }
}
