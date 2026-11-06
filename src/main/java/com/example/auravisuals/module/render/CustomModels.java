package com.example.auravisuals.module.render;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.ActionSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.util.render.RenderStateExtension;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.item.ItemRenderState;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.Resource;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;

public class CustomModels extends Module {
    static final Identifier PATRICK_MODEL = Identifier.of("auravisuals", "models/custom/patrick.gltf");
    static final Identifier PATRICK_TEXTURE = Identifier.of("auravisuals", "textures/custom/patrick.png");
    static final String MODEL_PATRICK = "Патрик";
    static final float PATRICK_SCALE = 0.82F;
    static final float PATRICK_HEIGHT = 1.79F;
    static final float PATRICK_WIDTH = 1.33F;
    static final CustomModels$ModelDefinition PATRICK = new CustomModels$ModelDefinition("Патрик", PATRICK_MODEL, PATRICK_TEXTURE, 0.82F, 1.79F, 1.33F);
    static final Path MODELS_DIR = FabricLoader.getInstance().getGameDir().resolve("auravisuals").resolve("models");
    static final String MODELS_SITE_URL = "https://hub.vroid.com/en/";
    static final float USER_TARGET_HEIGHT = 1.79F;
    final ModeSetting model = new ModeSetting("Модель", "Патрик", "Патрик");
    final ActionSetting addModelAction = new ActionSetting("Добавить своего персонажа", this::openAddModel);
    final ActionSetting refreshAction = new ActionSetting("Обновить список моделей", this::refreshUserModels);
    List<CustomModels$Primitive> patrick;
    boolean patrickLoadFailed;
    long patrickLastLoadAttempt;
    final Map<String, Path> userModelFiles = new LinkedHashMap<>();
    final Map<String, List<CustomModels$Primitive>> userModelCache = new HashMap<>();
    final Map<String, CustomModels$ModelDefinition> userModelDefs = new HashMap<>();
    final Set<String> userModelFailed = new HashSet<>();
    final Set<String> registeredTextures = new HashSet<>();

    public CustomModels() {
        super("CustomModels", "Кастомные модели игроков", Category.VISUALS);
        this.addSettings(this.model);
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.mc.player != null && this.mc.world != null && this.mc.gameRenderer != null) {
            CustomModels$ModelDefinition definition = this.getDefinition();
            List<CustomModels$Primitive> activeModel = this.getModel(definition);
            if (!activeModel.isEmpty()) {
                MatrixStack stack = context.matrixStack();
                Vec3d camera = context.camera().getCameraPos();
                float tickDelta = context.tickCounter().getTickProgress(true);
                stack.push();

                try {
                    this.setupModelRenderState(definition);
                    if (!this.mc.options.getPerspective().isFirstPerson()
                        && this.mc.player.isAlive()
                        && this.isVisibleFromCamera(this.mc.player, camera, tickDelta, definition)) {
                        this.renderCustomModel(stack, context.commandQueue(), activeModel, definition, this.mc.player, camera, tickDelta);
                    }
                } finally {
                    stack.pop();
                }
            }
        }
    }

    boolean isVisibleFromCamera(PlayerEntity player, Vec3d camera, float tickDelta, CustomModels$ModelDefinition definition) {
        double x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX());
        double baseY = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY());
        double z = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ());
        double half = Math.max(0.22, definition.width * 0.25);

        for (Vec3d point : new Vec3d[]{
            new Vec3d(x, baseY + definition.height * 0.16, z),
            new Vec3d(x, baseY + definition.height * 0.48, z),
            new Vec3d(x, baseY + definition.height * 0.8, z),
            new Vec3d(x + half, baseY + definition.height * 0.55, z),
            new Vec3d(x - half, baseY + definition.height * 0.55, z),
            new Vec3d(x, baseY + definition.height * 0.55, z + half),
            new Vec3d(x, baseY + definition.height * 0.55, z - half)
        }) {
            if (this.isPointVisible(camera, point)) {
                return true;
            }
        }

        return false;
    }

    boolean isPointVisible(Vec3d camera, Vec3d target) {
        BlockHitResult hit = this.mc.world.raycast(new RaycastContext(camera, target, ShapeType.COLLIDER, FluidHandling.NONE, this.mc.player));
        return hit != null && hit.getType() != Type.MISS ? hit.getPos().squaredDistanceTo(camera) + 0.16 >= target.squaredDistanceTo(camera) : true;
    }

    void renderCustomModel(
        MatrixStack stack,
        OrderedRenderCommandQueue queue,
        List<CustomModels$Primitive> model,
        CustomModels$ModelDefinition definition,
        PlayerEntity player,
        Vec3d camera,
        float tickDelta
    ) {
        double x = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX()) - camera.x;
        double y = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY()) - camera.y;
        double z = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ()) - camera.z;
        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, player.lastBodyYaw, player.bodyYaw);
        float time = (player.age + tickDelta) * 0.16F;
        float walkPhase = player.limbAnimator.getAnimationProgress() * 0.6662F;
        float walkAmount = MathHelper.clamp(player.limbAnimator.getAmplitude(tickDelta) * 1.35F, 0.0F, 1.0F);
        float swingProgress = player.getHandSwingProgress(tickDelta);
        float attackSwing = swingProgress > 0.0F ? MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI) : 0.0F;
        boolean sneaking = player.isSneaking();
        float bob = MathHelper.sin(time * 1.7F) * 0.025F;
        if (walkAmount > 0.01F) {
            bob += Math.abs(MathHelper.sin(walkPhase)) * 0.035F * walkAmount;
        }

        stack.push();

        try {
            stack.translate(x, y + bob, z);
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
            CustomModels$Bounds bounds = this.getBounds(model);
            float scale = definition.scale;
            stack.scale(scale, scale, scale);
            stack.translate(-bounds.centerX, -bounds.minY, -bounds.centerZ);
            if (sneaking) {
                stack.translate(0.0F, -0.1F, 0.07F);
                stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(7.5F));
                stack.scale(1.02F, 0.94F, 1.02F);
            }

            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.sin(time) * 1.2F));
            boolean humanoid = definition == PATRICK;
            CustomModels$Primitive rightHand = humanoid ? this.findHandPrimitive(model, bounds, true) : null;
            CustomModels$Primitive leftHand = humanoid ? this.findHandPrimitive(model, bounds, false) : null;

            for (CustomModels$Primitive primitive : model) {
                stack.push();

                try {
                    if (humanoid) {
                        this.applyPrimitiveAnimation(stack, primitive, bounds, time, walkPhase, walkAmount, attackSwing, sneaking);
                    }

                    this.drawPrimitive(stack, primitive);
                } finally {
                    stack.pop();
                }
            }

            if (rightHand != null) {
                this.renderHeldItemAttached(
                    stack,
                    queue,
                    definition,
                    rightHand,
                    bounds,
                    player,
                    player.getMainArm() == Arm.RIGHT ? player.getMainHandStack() : player.getOffHandStack(),
                    true,
                    player.getMainArm() == Arm.RIGHT ? attackSwing : 0.0F,
                    time,
                    walkPhase,
                    walkAmount,
                    sneaking,
                    0
                );
            } else {
                this.renderHeldItemFallback(
                    stack,
                    queue,
                    definition,
                    player,
                    player.getMainArm() == Arm.RIGHT ? player.getMainHandStack() : player.getOffHandStack(),
                    true,
                    player.getMainArm() == Arm.RIGHT ? attackSwing : 0.0F,
                    0
                );
            }

            if (leftHand != null) {
                this.renderHeldItemAttached(
                    stack,
                    queue,
                    definition,
                    leftHand,
                    bounds,
                    player,
                    player.getMainArm() == Arm.LEFT ? player.getMainHandStack() : player.getOffHandStack(),
                    false,
                    player.getMainArm() == Arm.LEFT ? attackSwing : 0.0F,
                    time,
                    walkPhase,
                    walkAmount,
                    sneaking,
                    1
                );
            } else {
                this.renderHeldItemFallback(
                    stack,
                    queue,
                    definition,
                    player,
                    player.getMainArm() == Arm.LEFT ? player.getMainHandStack() : player.getOffHandStack(),
                    false,
                    player.getMainArm() == Arm.LEFT ? attackSwing : 0.0F,
                    1
                );
            }
        } finally {
            stack.pop();
        }
    }

    CustomModels$Primitive findHandPrimitive(List<CustomModels$Primitive> model, CustomModels$Bounds bounds, boolean right) {
        CustomModels$Primitive best = null;
        float bestY = Float.MAX_VALUE;

        for (CustomModels$Primitive primitive : model) {
            float relativeX = primitive.centerX - bounds.centerX;
            boolean primitiveRight = relativeX > 0.0F;
            if (primitiveRight == right
                && !(primitive.centerY <= 0.46F)
                && !(primitive.centerY >= 1.38F)
                && !(Math.abs(relativeX) <= 0.34F)
                && primitive.minY < bestY) {
                bestY = primitive.minY;
                best = primitive;
            }
        }

        return best;
    }

    void renderHeldItemAttached(
        MatrixStack stack,
        OrderedRenderCommandQueue queue,
        CustomModels$ModelDefinition definition,
        CustomModels$Primitive handPrimitive,
        CustomModels$Bounds bounds,
        PlayerEntity player,
        ItemStack item,
        boolean right,
        float attackSwing,
        float time,
        float walkPhase,
        float walkAmount,
        boolean sneaking,
        int seed
    ) {
        if (item != null && !item.isEmpty() && this.mc.world != null) {
            float side = right ? 1.0F : -1.0F;
            stack.push();

            try {
                this.applyPrimitiveAnimation(stack, handPrimitive, bounds, time, walkPhase, walkAmount, attackSwing, sneaking);
                float handX = handPrimitive.centerX + side * 0.02F;
                float handY = handPrimitive.minY - 0.1F - 0.02F * attackSwing;
                float handZ = handPrimitive.centerZ - 0.04F;
                stack.translate(handX, handY, handZ);
                stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
                stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
                stack.translate(side * 0.04F, 0.055F, -0.42F);
                stack.scale(0.58F, 0.58F, 0.58F);
                this.renderHeldItem(stack, queue, player, item, right, seed);
            } finally {
                this.setupModelRenderState(definition);
                stack.pop();
            }
        }
    }

    void renderHeldItemFallback(
        MatrixStack stack,
        OrderedRenderCommandQueue queue,
        CustomModels$ModelDefinition definition,
        PlayerEntity player,
        ItemStack item,
        boolean right,
        float attackSwing,
        int seed
    ) {
        if (item != null && !item.isEmpty() && this.mc.world != null) {
            float side = right ? 1.0F : -1.0F;
            stack.push();

            try {
                stack.translate(side * 0.49F, 0.76F - 0.03F * attackSwing, -0.18F);
                stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
                stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
                stack.translate(side / 16.0F, 0.125F, -0.625F);
                stack.scale(0.62F, 0.62F, 0.62F);
                this.renderHeldItem(stack, queue, player, item, right, seed);
            } finally {
                this.setupModelRenderState(definition);
                stack.pop();
            }
        }
    }

    void renderHeldItem(MatrixStack stack, OrderedRenderCommandQueue queue, PlayerEntity player, ItemStack item, boolean right, int seed) {
        ItemDisplayContext displayContext = right ? ItemDisplayContext.THIRD_PERSON_RIGHT_HAND : ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
        ItemRenderState itemState = new ItemRenderState();
        this.mc.getItemModelManager().clearAndUpdate(itemState, item, displayContext, this.mc.world, player, player.getId() + seed);
        itemState.render(stack, queue, 15728880, OverlayTexture.DEFAULT_UV, 0);
    }

    void setupModelRenderState(CustomModels$ModelDefinition definition) {
    }

    void applyPrimitiveAnimation(
        MatrixStack stack,
        CustomModels$Primitive primitive,
        CustomModels$Bounds bounds,
        float time,
        float walkPhase,
        float walkAmount,
        float attackSwing,
        boolean sneaking
    ) {
        float relativeX = primitive.centerX - bounds.centerX;
        float absX = Math.abs(relativeX);
        if (primitive.centerY < 0.46F) {
            float sidePhase = relativeX > 0.0F ? 0.0F : (float) Math.PI;
            float angle = MathHelper.cos(walkPhase + sidePhase) * 20.0F * walkAmount;
            float sideSway = MathHelper.sin(walkPhase + sidePhase) * 2.2F * walkAmount;
            if (sneaking) {
                angle *= 0.45F;
                sideSway *= 0.45F;
            }

            this.rotateAround(stack, primitive.centerX, primitive.maxY, primitive.centerZ, angle, CustomModels$Axis.X);
            this.rotateAround(stack, primitive.centerX, primitive.maxY, primitive.centerZ, sideSway, CustomModels$Axis.Z);
        } else if (primitive.centerY < 1.38F && absX > 0.34F) {
            boolean rightSide = relativeX > 0.0F;
            float sidePhase = rightSide ? (float) Math.PI : 0.0F;
            float angle = MathHelper.cos(walkPhase + sidePhase) * 16.0F * walkAmount;
            angle += MathHelper.sin(time * 1.8F + primitive.centerX) * 1.5F;
            if (attackSwing > 0.0F) {
                angle += rightSide ? 68.0F * attackSwing : -22.0F * attackSwing;
            }

            if (sneaking) {
                angle -= 3.0F;
            }

            float pivotX = rightSide ? primitive.minX : primitive.maxX;
            this.rotateAround(stack, pivotX, primitive.maxY - 0.04F, primitive.centerZ, angle, CustomModels$Axis.X);
            if (attackSwing > 0.0F && rightSide) {
                this.rotateAround(stack, pivotX, primitive.maxY - 0.04F, primitive.centerZ, 12.0F * attackSwing, CustomModels$Axis.Y);
            }
        } else if (primitive.centerY >= 1.38F) {
            float idle = MathHelper.sin(time * 1.25F) * 1.0F;
            float hitNod = -4.0F * attackSwing;
            this.rotateAround(stack, primitive.centerX, primitive.minY + 0.05F, primitive.centerZ, idle + hitNod, CustomModels$Axis.X);
        }
    }

    void rotateAround(MatrixStack stack, float pivotX, float pivotY, float pivotZ, float angle, CustomModels$Axis axis) {
        stack.translate(pivotX, pivotY, pivotZ);
        if (axis == CustomModels$Axis.X) {
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle));
        }

        if (axis == CustomModels$Axis.Y) {
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
        }

        if (axis == CustomModels$Axis.Z) {
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(angle));
        }

        stack.translate(-pivotX, -pivotY, -pivotZ);
    }

    void drawPrimitive(MatrixStack stack, CustomModels$Primitive primitive) {
        Matrix4f matrix = stack.peek().getPositionMatrix();
        if (primitive.textureId != null) {
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE_COLOR);

            for (int index : primitive.indices) {
                int positionIndex = index * 3;
                int uvIndex = index * 2;
                buffer.vertex(matrix, primitive.positions[positionIndex], primitive.positions[positionIndex + 1], primitive.positions[positionIndex + 2])
                    .texture(primitive.uvs[uvIndex], 1.0F - primitive.uvs[uvIndex + 1])
                    .color(primitive.color);
            }

            AuraRender.drawTextured(buffer, AuraRender.POS_TEX_COLOR_TRIS_SOLID, primitive.textureId);
        } else {
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);

            for (int index : primitive.indices) {
                int positionIndex = index * 3;
                buffer.vertex(matrix, primitive.positions[positionIndex], primitive.positions[positionIndex + 1], primitive.positions[positionIndex + 2])
                    .color(primitive.color);
            }

            AuraRender.drawColor(buffer, AuraRender.POS_COLOR_TRIS_SOLID);
        }
    }

    CustomModels$ModelDefinition getDefinition() {
        return PATRICK;
    }

    List<CustomModels$Primitive> getModel(CustomModels$ModelDefinition definition) {
        return this.getPatrick();
    }

    List<CustomModels$Primitive> getUserModel(String name) {
        List<CustomModels$Primitive> cached = this.userModelCache.get(name);
        if (cached != null) {
            return cached;
        }

        if (this.userModelFailed.contains(name)) {
            return List.of();
        }

        Path path = this.userModelFiles.get(name);
        if (path == null) {
            this.userModelFailed.add(name);
            return List.of();
        }

        try {
            List<CustomModels$Primitive> primitives = this.loadUserModel(path, name);
            if (primitives.isEmpty()) {
                this.userModelFailed.add(name);
                return List.of();
            } else {
                this.userModelCache.put(name, primitives);
                this.userModelDefs.put(name, this.buildUserDefinition(name, primitives));
                return primitives;
            }
        } catch (Throwable t) {
            this.userModelFailed.add(name);
            return List.of();
        }
    }

    CustomModels$ModelDefinition buildUserDefinition(String name, List<CustomModels$Primitive> primitives) {
        float minY = Float.MAX_VALUE;
        float maxY = -Float.MAX_VALUE;
        float minX = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;

        for (CustomModels$Primitive p : primitives) {
            minY = Math.min(minY, p.minY);
            maxY = Math.max(maxY, p.maxY);
            minX = Math.min(minX, p.minX);
            maxX = Math.max(maxX, p.maxX);
            minZ = Math.min(minZ, p.minZ);
            maxZ = Math.max(maxZ, p.maxZ);
        }

        float h = Math.max(1.0E-4F, maxY - minY);
        float w = Math.max(1.0E-4F, maxX - minX);
        float d = Math.max(1.0E-4F, maxZ - minZ);
        float maxDim = Math.max(h, Math.max(w, d));
        float scale = MathHelper.clamp(1.6F / maxDim, 0.001F, 500.0F);
        float height = Math.max(0.3F, h * scale);
        float width = Math.max(0.3F, Math.max(w, d) * scale);
        return new CustomModels$ModelDefinition(name, null, null, scale, height, width);
    }

    List<CustomModels$Primitive> getPatrick() {
        if (this.patrick != null) {
            return this.patrick;
        }

        long now = System.currentTimeMillis();
        if (this.patrickLoadFailed && now - this.patrickLastLoadAttempt < 2000L) {
            return List.of();
        }

        this.patrickLastLoadAttempt = now;

        try {
            this.patrick = this.loadModel(PATRICK);
            this.patrickLoadFailed = this.patrick.isEmpty();
        } catch (Throwable ignored) {
            this.patrickLoadFailed = true;
            this.patrick = List.of();
        }

        return this.patrick;
    }

    List<CustomModels$Primitive> loadModel(CustomModels$ModelDefinition definition) throws Exception {
        Optional resource = this.mc.getResourceManager().getResource(definition.modelId);
        if (resource.isEmpty()) {
            return List.of();
        }

        JsonObject root;
        try (InputStreamReader reader = new InputStreamReader(((Resource)resource.get()).getInputStream(), StandardCharsets.UTF_8)) {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        }

        byte[] bytes = this.readBuffer(root, definition.modelId);
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        JsonArray bufferViews = root.getAsJsonArray("bufferViews");
        JsonArray accessors = root.getAsJsonArray("accessors");
        JsonArray meshes = root.getAsJsonArray("meshes");
        float[][] meshTransforms = this.readMeshTransforms(root, meshes.size());
        List materials = this.readMaterials(root, definition);
        ArrayList primitives = new ArrayList();

        for (int meshIndex = 0; meshIndex < meshes.size(); meshIndex++) {
            JsonArray meshPrimitives = meshes.get(meshIndex).getAsJsonObject().getAsJsonArray("primitives");

            for (int primitiveIndex = 0; primitiveIndex < meshPrimitives.size(); primitiveIndex++) {
                JsonObject primitive = meshPrimitives.get(primitiveIndex).getAsJsonObject();
                JsonObject attributes = primitive.getAsJsonObject("attributes");
                float[] positions = this.readFloats(buffer, bufferViews, accessors, attributes.get("POSITION").getAsInt(), 3);
                this.applyTransform(positions, meshTransforms[meshIndex]);
                float[] uvs = this.readFloats(buffer, bufferViews, accessors, attributes.get("TEXCOORD_0").getAsInt(), 2);
                int[] indices = this.readIndices(buffer, bufferViews, accessors, primitive.get("indices").getAsInt());
                CustomModels$MaterialInfo material = this.getMaterial(materials, primitive, definition);
                primitives.add(new CustomModels$Primitive(positions, uvs, indices, material.textureId, material.color));
            }
        }

        return primitives;
    }

    byte[] readBuffer(JsonObject root, Identifier modelId) throws Exception {
        JsonArray buffers = root.getAsJsonArray("buffers");
        String uri = buffers.get(0).getAsJsonObject().get("uri").getAsString();
        int split = uri.indexOf(44);
        if (uri.startsWith("data:")) {
            return Base64.getDecoder().decode(split >= 0 ? uri.substring(split + 1) : uri);
        }

        String path = this.resolveResourcePath(modelId.getPath(), uri);
        Optional resource = this.mc.getResourceManager().getResource(Identifier.of(modelId.getNamespace(), path));
        if (resource.isEmpty()) {
            return new byte[0];
        }

        try (InputStream inputStream = ((Resource)resource.get()).getInputStream()) {
            return inputStream.readAllBytes();
        }
    }

    List<CustomModels$MaterialInfo> readMaterials(JsonObject root, CustomModels$ModelDefinition definition) {
        ArrayList<CustomModels$MaterialInfo> materials = new ArrayList<>();
        if (!root.has("materials")) {
            materials.add(new CustomModels$MaterialInfo(definition.textureId, -1));
            return materials;
        }

        JsonArray array = root.getAsJsonArray("materials");

        for (int i = 0; i < array.size(); i++) {
            JsonObject material = array.get(i).getAsJsonObject();
            Identifier texture = this.readMaterialTexture(root, definition, material);
            int color = texture != null ? -1 : this.readMaterialColor(material);
            if (texture == null && definition.textureId != null) {
                texture = definition.textureId;
            }

            materials.add(new CustomModels$MaterialInfo(texture, color));
        }

        return materials;
    }

    CustomModels$MaterialInfo getMaterial(List<CustomModels$MaterialInfo> materials, JsonObject primitive, CustomModels$ModelDefinition definition) {
        int index;
        return primitive.has("material") && (index = primitive.get("material").getAsInt()) >= 0 && index < materials.size()
            ? materials.get(index)
            : new CustomModels$MaterialInfo(definition.textureId, -1);
    }

    Identifier readMaterialTexture(JsonObject root, CustomModels$ModelDefinition definition, JsonObject material) {
        int textureIndex = -1;
        if (material.has("emissiveTexture")) {
            textureIndex = material.getAsJsonObject("emissiveTexture").get("index").getAsInt();
        } else {
            JsonObject pbr;
            if (material.has("pbrMetallicRoughness") && (pbr = material.getAsJsonObject("pbrMetallicRoughness")).has("baseColorTexture")) {
                textureIndex = pbr.getAsJsonObject("baseColorTexture").get("index").getAsInt();
            }
        }

        if (textureIndex >= 0 && root.has("textures") && root.has("images")) {
            JsonArray textures = root.getAsJsonArray("textures");
            if (textureIndex >= textures.size()) {
                return null;
            }

            JsonObject texture = textures.get(textureIndex).getAsJsonObject();
            if (!texture.has("source")) {
                return null;
            }

            int imageIndex = texture.get("source").getAsInt();
            JsonArray images = root.getAsJsonArray("images");
            if (imageIndex >= 0 && imageIndex < images.size()) {
                JsonObject image = images.get(imageIndex).getAsJsonObject();
                if (!image.has("uri")) {
                    return null;
                }

                String uri = image.get("uri").getAsString();
                return uri.startsWith("data:")
                    ? definition.textureId
                    : Identifier.of(definition.modelId.getNamespace(), this.resolveResourcePath(definition.modelId.getPath(), uri));
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    int readMaterialColor(JsonObject material) {
        if (material.has("emissiveFactor") && !material.has("emissiveTexture")) {
            JsonArray factor = material.getAsJsonArray("emissiveFactor");
            return this.argb(factor.get(0).getAsFloat(), factor.get(1).getAsFloat(), factor.get(2).getAsFloat(), 1.0F);
        } else {
            JsonObject pbr;
            if (material.has("pbrMetallicRoughness") && (pbr = material.getAsJsonObject("pbrMetallicRoughness")).has("baseColorFactor")) {
                JsonArray factor = pbr.getAsJsonArray("baseColorFactor");
                return this.argb(factor.get(0).getAsFloat(), factor.get(1).getAsFloat(), factor.get(2).getAsFloat(), factor.get(3).getAsFloat());
            } else {
                return -1;
            }
        }
    }

    int argb(float r, float g, float b, float a) {
        int ai = MathHelper.clamp((int)(a * 255.0F), 0, 255);
        int ri = MathHelper.clamp((int)(r * 255.0F), 0, 255);
        int gi = MathHelper.clamp((int)(g * 255.0F), 0, 255);
        int bi = MathHelper.clamp((int)(b * 255.0F), 0, 255);
        return ai << 24 | ri << 16 | gi << 8 | bi;
    }

    String resolveResourcePath(String modelPath, String uri) {
        String base = "";
        int slash = modelPath.lastIndexOf(47);
        if (slash >= 0) {
            base = modelPath.substring(0, slash + 1);
        }

        String[] parts = (base + uri).replace('\\', '/').split("/");
        ArrayDeque<String> clean = new ArrayDeque<>();

        for (String part : parts) {
            if (!part.isEmpty() && !".".equals(part)) {
                if ("..".equals(part)) {
                    if (!clean.isEmpty()) {
                        clean.removeLast();
                    }
                } else {
                    clean.addLast(part);
                }
            }
        }

        return String.join("/", clean);
    }

    float[][] readMeshTransforms(JsonObject root, int meshCount) {
        float[][] transforms = new float[meshCount][16];

        for (int i = 0; i < meshCount; i++) {
            transforms[i] = this.identityMatrix();
        }

        if (!root.has("nodes")) {
            return transforms;
        }

        JsonArray nodes = root.getAsJsonArray("nodes");
        boolean[] visited = new boolean[nodes.size()];
        if (root.has("scenes")) {
            int sceneIndex = this.getInt(root, "scene", 0);
            JsonObject scene = root.getAsJsonArray("scenes").get(sceneIndex).getAsJsonObject();
            if (scene.has("nodes")) {
                JsonArray roots = scene.getAsJsonArray("nodes");

                for (int i = 0; i < roots.size(); i++) {
                    this.visitNode(nodes, roots.get(i).getAsInt(), this.identityMatrix(), transforms, visited);
                }
            }
        }

        for (int i = 0; i < nodes.size(); i++) {
            if (!visited[i]) {
                this.visitNode(nodes, i, this.identityMatrix(), transforms, visited);
            }
        }

        return transforms;
    }

    void visitNode(JsonArray nodes, int nodeIndex, float[] parent, float[][] transforms, boolean[] visited) {
        if (nodeIndex >= 0 && nodeIndex < nodes.size()) {
            visited[nodeIndex] = true;
            JsonObject node = nodes.get(nodeIndex).getAsJsonObject();
            float[] local = this.readNodeMatrix(node);
            float[] combined = this.multiply(parent, local);
            int meshIndex;
            if (node.has("mesh") && (meshIndex = node.get("mesh").getAsInt()) >= 0 && meshIndex < transforms.length) {
                transforms[meshIndex] = combined;
            }

            if (node.has("children")) {
                JsonArray children = node.getAsJsonArray("children");

                for (int i = 0; i < children.size(); i++) {
                    this.visitNode(nodes, children.get(i).getAsInt(), combined, transforms, visited);
                }
            }
        }
    }

    float[] readNodeMatrix(JsonObject node) {
        if (!node.has("matrix")) {
            float[] matrix = this.identityMatrix();
            if (node.has("rotation")) {
                JsonArray rotation = node.getAsJsonArray("rotation");
                matrix = this.quaternionMatrix(
                    rotation.get(0).getAsFloat(), rotation.get(1).getAsFloat(), rotation.get(2).getAsFloat(), rotation.get(3).getAsFloat()
                );
            }

            if (node.has("translation")) {
                JsonArray translation = node.getAsJsonArray("translation");
                matrix[12] = translation.get(0).getAsFloat();
                matrix[13] = translation.get(1).getAsFloat();
                matrix[14] = translation.get(2).getAsFloat();
            }

            if (node.has("scale")) {
                JsonArray scale = node.getAsJsonArray("scale");
                matrix[0] *= scale.get(0).getAsFloat();
                matrix[5] *= scale.get(1).getAsFloat();
                matrix[10] *= scale.get(2).getAsFloat();
            }

            return matrix;
        } else {
            JsonArray matrix = node.getAsJsonArray("matrix");
            float[] values = new float[16];

            for (int i = 0; i < 16; i++) {
                values[i] = matrix.get(i).getAsFloat();
            }

            return values;
        }
    }

    float[] identityMatrix() {
        float[] matrix = new float[16];
        matrix[0] = 1.0F;
        matrix[5] = 1.0F;
        matrix[10] = 1.0F;
        matrix[15] = 1.0F;
        return matrix;
    }

    float[] quaternionMatrix(float x, float y, float z, float w) {
        float[] matrix = this.identityMatrix();
        float xx = x * x;
        float yy = y * y;
        float zz = z * z;
        float xy = x * y;
        float xz = x * z;
        float yz = y * z;
        float wx = w * x;
        float wy = w * y;
        float wz = w * z;
        matrix[0] = 1.0F - 2.0F * (yy + zz);
        matrix[1] = 2.0F * (xy + wz);
        matrix[2] = 2.0F * (xz - wy);
        matrix[4] = 2.0F * (xy - wz);
        matrix[5] = 1.0F - 2.0F * (xx + zz);
        matrix[6] = 2.0F * (yz + wx);
        matrix[8] = 2.0F * (xz + wy);
        matrix[9] = 2.0F * (yz - wx);
        matrix[10] = 1.0F - 2.0F * (xx + yy);
        return matrix;
    }

    float[] multiply(float[] a, float[] b) {
        float[] result = new float[16];

        for (int col = 0; col < 4; col++) {
            for (int row = 0; row < 4; row++) {
                result[col * 4 + row] = a[0 + row] * b[col * 4 + 0] + a[4 + row] * b[col * 4 + 1] + a[8 + row] * b[col * 4 + 2] + a[12 + row] * b[col * 4 + 3];
            }
        }

        return result;
    }

    void applyTransform(float[] positions, float[] matrix) {
        for (int i = 0; i < positions.length; i += 3) {
            float x = positions[i];
            float y = positions[i + 1];
            float z = positions[i + 2];
            positions[i] = matrix[0] * x + matrix[4] * y + matrix[8] * z + matrix[12];
            positions[i + 1] = matrix[1] * x + matrix[5] * y + matrix[9] * z + matrix[13];
            positions[i + 2] = matrix[2] * x + matrix[6] * y + matrix[10] * z + matrix[14];
        }
    }

    float[] readFloats(ByteBuffer buffer, JsonArray bufferViews, JsonArray accessors, int accessorIndex, int components) {
        JsonObject accessor = accessors.get(accessorIndex).getAsJsonObject();
        JsonObject view = bufferViews.get(accessor.get("bufferView").getAsInt()).getAsJsonObject();
        int count = accessor.get("count").getAsInt();
        int base = this.getInt(view, "byteOffset", 0) + this.getInt(accessor, "byteOffset", 0);
        int stride = this.getInt(view, "byteStride", components * 4);
        float[] values = new float[count * components];

        for (int i = 0; i < count; i++) {
            int offset = base + i * stride;

            for (int component = 0; component < components; component++) {
                values[i * components + component] = buffer.getFloat(offset + component * 4);
            }
        }

        return values;
    }

    int[] readIndices(ByteBuffer buffer, JsonArray bufferViews, JsonArray accessors, int accessorIndex) {
        JsonObject accessor = accessors.get(accessorIndex).getAsJsonObject();
        JsonObject view = bufferViews.get(accessor.get("bufferView").getAsInt()).getAsJsonObject();
        int count = accessor.get("count").getAsInt();
        int componentType = accessor.get("componentType").getAsInt();
        int componentSize = componentType == 5125 ? 4 : (componentType == 5123 ? 2 : 1);
        int base = this.getInt(view, "byteOffset", 0) + this.getInt(accessor, "byteOffset", 0);
        int stride = this.getInt(view, "byteStride", componentSize);
        int[] values = new int[count];

        for (int i = 0; i < count; i++) {
            int offset = base + i * stride;
            values[i] = componentType == 5125
                ? buffer.getInt(offset)
                : (componentType == 5123 ? Short.toUnsignedInt(buffer.getShort(offset)) : Byte.toUnsignedInt(buffer.get(offset)));
        }

        return values;
    }

    int getInt(JsonObject object, String key, int defaultValue) {
        return object.has(key) ? object.get(key).getAsInt() : defaultValue;
    }

    List<CustomModels$Primitive> loadUserModel(Path path, String name) throws Exception {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return !fileName.endsWith(".glb") && !fileName.endsWith(".vrm") ? this.loadGltfDisk(path, name) : this.loadGlb(path, name);
    }

    List<CustomModels$Primitive> loadGltfDisk(Path path, String name) throws Exception {
        InputStreamReader reader = new InputStreamReader(Files.newInputStream(path), StandardCharsets.UTF_8);

        JsonObject root;
        try {
            root = JsonParser.parseReader(reader).getAsJsonObject();
        } catch (Throwable var26) {
            try {
                reader.close();
            } catch (Throwable var25) {
                var26.addSuppressed(var25);
            }

            throw var26;
        }

        reader.close();
        Path var27 = path.getParent();
        JsonArray buffersArr = root.getAsJsonArray("buffers");
        if (buffersArr != null && buffersArr.size() != 0) {
            String uri = buffersArr.get(0).getAsJsonObject().get("uri").getAsString();
            byte[] bin = this.readDiskBuffer(var27, uri);
            ByteBuffer buffer = ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN);
            JsonArray bufferViews = root.getAsJsonArray("bufferViews");
            JsonArray accessors = root.getAsJsonArray("accessors");
            JsonArray meshes = root.getAsJsonArray("meshes");
            if (meshes == null) {
                return List.of();
            }

            float[][] meshTransforms = this.readMeshTransforms(root, meshes.size());
            List<CustomModels$MaterialInfo> materials = this.readMaterialsDisk(root, var27, name);
            CustomModels$ModelDefinition fallback = new CustomModels$ModelDefinition(name, null, null, 1.0F, 1.79F, 1.0F);
            ArrayList<CustomModels$Primitive> primitives = new ArrayList<>();

            for (int meshIndex = 0; meshIndex < meshes.size(); meshIndex++) {
                JsonArray meshPrimitives = meshes.get(meshIndex).getAsJsonObject().getAsJsonArray("primitives");
                if (meshPrimitives != null) {
                    for (int primitiveIndex = 0; primitiveIndex < meshPrimitives.size(); primitiveIndex++) {
                        JsonObject primitive = meshPrimitives.get(primitiveIndex).getAsJsonObject();
                        JsonObject attributes = primitive.getAsJsonObject("attributes");
                        if (attributes != null && attributes.has("POSITION") && primitive.has("indices")) {
                            float[] positions = this.readFloats(buffer, bufferViews, accessors, attributes.get("POSITION").getAsInt(), 3);
                            this.applyTransform(positions, meshTransforms[meshIndex]);
                            float[] uvs = attributes.has("TEXCOORD_0")
                                ? this.readFloats(buffer, bufferViews, accessors, attributes.get("TEXCOORD_0").getAsInt(), 2)
                                : new float[positions.length / 3 * 2];
                            int[] indices = this.readIndices(buffer, bufferViews, accessors, primitive.get("indices").getAsInt());
                            CustomModels$MaterialInfo material = this.getMaterial(materials, primitive, fallback);
                            primitives.add(new CustomModels$Primitive(positions, uvs, indices, material.textureId, material.color));
                        }
                    }
                }
            }

            return primitives;
        } else {
            return List.of();
        }
    }

    byte[] readDiskBuffer(Path dir, String uri) throws Exception {
        if (uri.startsWith("data:")) {
            int split = uri.indexOf(44);
            return Base64.getDecoder().decode(split >= 0 ? uri.substring(split + 1) : uri);
        } else {
            String decoded = URLDecoder.decode(uri, StandardCharsets.UTF_8);
            return Files.readAllBytes(dir.resolve(decoded));
        }
    }

    List<CustomModels$MaterialInfo> readMaterialsDisk(JsonObject root, Path dir, String name) {
        ArrayList<CustomModels$MaterialInfo> materials = new ArrayList<>();
        if (!root.has("materials")) {
            materials.add(new CustomModels$MaterialInfo(null, -1));
            return materials;
        }

        JsonArray array = root.getAsJsonArray("materials");

        for (int i = 0; i < array.size(); i++) {
            JsonObject material = array.get(i).getAsJsonObject();
            Identifier texture = this.readMaterialTextureDisk(root, dir, material, name);
            int color = texture != null ? -1 : this.readMaterialColor(material);
            materials.add(new CustomModels$MaterialInfo(texture, color));
        }

        return materials;
    }

    Identifier readMaterialTextureDisk(JsonObject root, Path dir, JsonObject material, String name) {
        int textureIndex = -1;
        JsonObject pbr;
        if (material.has("pbrMetallicRoughness") && (pbr = material.getAsJsonObject("pbrMetallicRoughness")).has("baseColorTexture")) {
            textureIndex = pbr.getAsJsonObject("baseColorTexture").get("index").getAsInt();
        }

        if (textureIndex < 0 && material.has("emissiveTexture")) {
            textureIndex = material.getAsJsonObject("emissiveTexture").get("index").getAsInt();
        }

        if (textureIndex >= 0 && root.has("textures") && root.has("images")) {
            JsonArray textures = root.getAsJsonArray("textures");
            if (textureIndex >= textures.size()) {
                return null;
            }

            JsonObject texture = textures.get(textureIndex).getAsJsonObject();
            if (!texture.has("source")) {
                return null;
            }

            int imageIndex = texture.get("source").getAsInt();
            JsonArray images = root.getAsJsonArray("images");
            if (imageIndex >= 0 && imageIndex < images.size()) {
                JsonObject image = images.get(imageIndex).getAsJsonObject();
                if (!image.has("uri")) {
                    return null;
                }

                try {
                    byte[] imageBytes = this.readDiskBuffer(dir, image.get("uri").getAsString());
                    return this.registerImageTexture(imageBytes, name, imageIndex);
                } catch (Throwable t) {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    List<CustomModels$Primitive> loadGlb(Path path, String name) throws Exception {
        byte[] bytes = Files.readAllBytes(path);
        if (bytes.length < 12) {
            return List.of();
        }

        ByteBuffer head = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int magic = head.getInt();
        if (magic != 1179937895) {
            return List.of();
        }

        head.getInt();
        head.getInt();
        JsonObject root = null;
        byte[] bin = null;
        int pos = 12;

        while (pos + 8 <= bytes.length) {
            ByteBuffer ch = ByteBuffer.wrap(bytes, pos, 8).order(ByteOrder.LITTLE_ENDIAN);
            int chunkLength = ch.getInt();
            int chunkType = ch.getInt();
            if (chunkLength < 0) {
                break;
            }

            pos += 8;
            if (pos + chunkLength > bytes.length) {
                break;
            }

            if (chunkType == 1313821514) {
                root = JsonParser.parseString(new String(bytes, pos, chunkLength, StandardCharsets.UTF_8)).getAsJsonObject();
            } else if (chunkType == 5130562) {
                bin = Arrays.copyOfRange(bytes, pos, pos + chunkLength);
            }

            pos += chunkLength;
        }

        if (root != null && bin != null) {
            ByteBuffer buffer = ByteBuffer.wrap(bin).order(ByteOrder.LITTLE_ENDIAN);
            JsonArray bufferViews = root.getAsJsonArray("bufferViews");
            JsonArray accessors = root.getAsJsonArray("accessors");
            JsonArray meshes = root.getAsJsonArray("meshes");
            if (meshes == null) {
                return List.of();
            }

            float[][] meshTransforms = this.readMeshTransforms(root, meshes.size());
            List<CustomModels$MaterialInfo> materials = this.readMaterialsGlb(root, bin, bufferViews, name);
            CustomModels$ModelDefinition fallback = new CustomModels$ModelDefinition(name, null, null, 1.0F, 1.79F, 1.0F);
            ArrayList<CustomModels$Primitive> primitives = new ArrayList<>();

            for (int meshIndex = 0; meshIndex < meshes.size(); meshIndex++) {
                JsonArray meshPrimitives = meshes.get(meshIndex).getAsJsonObject().getAsJsonArray("primitives");
                if (meshPrimitives != null) {
                    for (int primitiveIndex = 0; primitiveIndex < meshPrimitives.size(); primitiveIndex++) {
                        JsonObject primitive = meshPrimitives.get(primitiveIndex).getAsJsonObject();
                        JsonObject attributes = primitive.getAsJsonObject("attributes");
                        if (attributes != null && attributes.has("POSITION") && primitive.has("indices")) {
                            float[] positions = this.readFloats(buffer, bufferViews, accessors, attributes.get("POSITION").getAsInt(), 3);
                            this.applyTransform(positions, meshTransforms[meshIndex]);
                            float[] uvs = attributes.has("TEXCOORD_0")
                                ? this.readFloats(buffer, bufferViews, accessors, attributes.get("TEXCOORD_0").getAsInt(), 2)
                                : new float[positions.length / 3 * 2];
                            int[] indices = this.readIndices(buffer, bufferViews, accessors, primitive.get("indices").getAsInt());
                            CustomModels$MaterialInfo material = this.getMaterial(materials, primitive, fallback);
                            primitives.add(new CustomModels$Primitive(positions, uvs, indices, material.textureId, material.color));
                        }
                    }
                }
            }

            return primitives;
        } else {
            return List.of();
        }
    }

    List<CustomModels$MaterialInfo> readMaterialsGlb(JsonObject root, byte[] bin, JsonArray bufferViews, String name) {
        ArrayList<CustomModels$MaterialInfo> materials = new ArrayList<>();
        if (!root.has("materials")) {
            materials.add(new CustomModels$MaterialInfo(null, -1));
            return materials;
        }

        JsonArray array = root.getAsJsonArray("materials");

        for (int i = 0; i < array.size(); i++) {
            JsonObject material = array.get(i).getAsJsonObject();
            Identifier texture = this.readMaterialTextureGlb(root, bin, bufferViews, material, name);
            int color = texture != null ? -1 : this.readMaterialColor(material);
            materials.add(new CustomModels$MaterialInfo(texture, color));
        }

        return materials;
    }

    Identifier readMaterialTextureGlb(JsonObject root, byte[] bin, JsonArray bufferViews, JsonObject material, String name) {
        int textureIndex = -1;
        JsonObject pbr;
        if (material.has("pbrMetallicRoughness") && (pbr = material.getAsJsonObject("pbrMetallicRoughness")).has("baseColorTexture")) {
            textureIndex = pbr.getAsJsonObject("baseColorTexture").get("index").getAsInt();
        }

        if (textureIndex < 0 && material.has("emissiveTexture")) {
            textureIndex = material.getAsJsonObject("emissiveTexture").get("index").getAsInt();
        }

        if (textureIndex >= 0 && root.has("textures") && root.has("images")) {
            JsonArray textures = root.getAsJsonArray("textures");
            if (textureIndex >= textures.size()) {
                return null;
            }

            JsonObject texture = textures.get(textureIndex).getAsJsonObject();
            if (!texture.has("source")) {
                return null;
            }

            int imageIndex = texture.get("source").getAsInt();
            JsonArray images = root.getAsJsonArray("images");
            if (imageIndex >= 0 && imageIndex < images.size()) {
                JsonObject image = images.get(imageIndex).getAsJsonObject();

                try {
                    byte[] imageBytes = null;
                    if (image.has("bufferView")) {
                        JsonObject view = bufferViews.get(image.get("bufferView").getAsInt()).getAsJsonObject();
                        int offset = this.getInt(view, "byteOffset", 0);
                        int length = view.get("byteLength").getAsInt();
                        imageBytes = Arrays.copyOfRange(bin, offset, offset + length);
                    } else {
                        String uri;
                        if (image.has("uri") && (uri = image.get("uri").getAsString()).startsWith("data:")) {
                            int split = uri.indexOf(44);
                            imageBytes = Base64.getDecoder().decode(split >= 0 ? uri.substring(split + 1) : uri);
                        }
                    }

                    return imageBytes == null ? null : this.registerImageTexture(imageBytes, name, imageIndex);
                } catch (Throwable t) {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    Identifier registerImageTexture(byte[] bytes, String name, int imageIndex) throws Exception {
        String key = sanitize(name) + "_" + imageIndex;
        Identifier id = Identifier.of("auravisuals", "usermodel/" + key);
        if (this.registeredTextures.contains(key)) {
            return id;
        }

        NativeImage image = NativeImage.read(bytes);
        this.mc.getTextureManager().registerTexture(id, new NativeImageBackedTexture(() -> "auravisuals/usermodel/" + key, image));
        this.registeredTextures.add(key);
        return id;
    }

    static String sanitize(String input) {
        StringBuilder sb = new StringBuilder();

        for (char c : input.toLowerCase(Locale.ROOT).toCharArray()) {
            if ((c < 'a' || c > 'z') && (c < '0' || c > '9') && c != '_' && c != '-' && c != '.') {
                sb.append('_');
            } else {
                sb.append(c);
            }
        }

        return sb.length() == 0 ? "model" : sb.toString();
    }

    void scanUserModels() {
        this.userModelFiles.clear();

        try {
            Files.createDirectories(MODELS_DIR);

            try (Stream<Path> stream = Files.list(MODELS_DIR)) {
                stream.filter(p -> {
                    String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                    return n.endsWith(".glb") || n.endsWith(".gltf") || n.endsWith(".vrm");
                }).sorted().forEach(p -> {
                    String fileName = p.getFileName().toString();
                    int dot = fileName.lastIndexOf(46);
                    String displayName = dot > 0 ? fileName.substring(0, dot) : fileName;
                    this.userModelFiles.put(displayName, p);
                });
            }
        } catch (Throwable var6) {
        }

        ArrayList<String> modes = new ArrayList<>();
        modes.add("Патрик");
        modes.addAll(this.userModelFiles.keySet());
        this.model.setModes(modes);
    }

    void refreshUserModels() {
        this.userModelCache.clear();
        this.userModelDefs.clear();
        this.userModelFailed.clear();
        this.scanUserModels();
    }

    void openAddModel() {
        try {
            Files.createDirectories(MODELS_DIR);
            Path readme = MODELS_DIR.resolve("КАК_ДОБАВИТЬ_МОДЕЛЬ.txt");
            this.writeInstructions(readme);
            Util.getOperatingSystem().open("https://hub.vroid.com/en/");
            Util.getOperatingSystem().open(MODELS_DIR);
            Util.getOperatingSystem().open(readme);
        } catch (Throwable var2) {
        }
    }

    void writeInstructions(Path path) {
        try {
            String text = String.join(
                "\r\n",
                "=== Как добавить свою модель в anvil visual ===",
                "",
                "1) Скачай модель-персонажа (аниме-аватары, бесплатные есть):",
                "   https://hub.vroid.com/en/",
                "   (VRoid Hub: выбери аватар -> Download -> файл .vrm.",
                "    Свою девушку можно слепить в VRoid Studio и экспортнуть .vrm.",
                "    Поддерживаются .vrm, .glb (один файл) и .gltf (+ .bin/текстуры).)",
                "2) Положи файл(ы) модели в ЭТУ папку:",
                "   " + MODELS_DIR,
                "3) В игре открой ClickGUI -> CustomModels -> нажми \"Обновить список моделей\".",
                "4) Выбери свою модель в настройке \"Модель\" и включи \"На себя\".",
                "",
                "Подсказки:",
                "- Имя модели в списке = имя файла без .glb",
                "- Лучше всего работают humanoid-модели (руки/ноги анимируются сами).",
                "- Размер подгоняется автоматически под рост игрока.",
                ""
            );
            Files.writeString(path, text, StandardCharsets.UTF_8);
        } catch (Throwable var3) {
        }
    }

    public static boolean shouldReplaceState(LivingEntityRenderState state) {
        if (!(state instanceof PlayerEntityRenderState)) {
            return false;
        }

        CustomModels customModels = getActive();
        if (customModels == null) {
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        RenderStateExtension extension;
        return client.currentScreen instanceof InventoryScreen
            ? false
            : state instanceof RenderStateExtension && (extension = (RenderStateExtension)state).AuraVisuals$isLocalPlayer();
    }

    public static boolean isReplacing(PlayerEntity player) {
        CustomModels customModels = getActive();
        if (customModels != null && player != null) {
            MinecraftClient client = MinecraftClient.getInstance();
            return player == client.player;
        } else {
            return false;
        }
    }

    public static float getVisualScale(PlayerEntity player) {
        CustomModels customModels = getActive();
        return customModels != null && player != null ? customModels.getDefinition().scale : 1.0F;
    }

    public static float getVisualHeight(PlayerEntity player) {
        CustomModels customModels = getActive();
        return customModels != null && player != null && isReplacing(player)
            ? (player.isSneaking() ? customModels.getDefinition().height * 0.88F : customModels.getDefinition().height)
            : 1.8F;
    }

    public static float getVisualWidth(PlayerEntity player) {
        CustomModels customModels = getActive();
        return customModels != null && player != null && isReplacing(player) ? customModels.getDefinition().width : 0.6F;
    }

    public static boolean shouldUsePatrickFirstPersonHands() {
        CustomModels customModels = getActive();
        if (customModels != null && customModels.model.is("Патрик")) {
            MinecraftClient client = MinecraftClient.getInstance();
            return client.player != null && client.options != null && client.options.getPerspective().isFirstPerson();
        } else {
            return false;
        }
    }

    static CustomModels getActive() {
        if (ModuleManager.INSTANCE == null) {
            return null;
        }

        CustomModels customModels = ModuleManager.INSTANCE.getModule(CustomModels.class);
        return customModels != null && customModels.isEnabled() ? customModels : null;
    }

    CustomModels$Bounds getBounds(List<CustomModels$Primitive> primitives) {
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE;
        float maxZ = -Float.MAX_VALUE;

        for (CustomModels$Primitive primitive : primitives) {
            minX = Math.min(minX, primitive.minX);
            minY = Math.min(minY, primitive.minY);
            minZ = Math.min(minZ, primitive.minZ);
            maxX = Math.max(maxX, primitive.maxX);
            maxZ = Math.max(maxZ, primitive.maxZ);
        }

        return new CustomModels$Bounds((minX + maxX) * 0.5F, minY, (minZ + maxZ) * 0.5F);
    }
}
