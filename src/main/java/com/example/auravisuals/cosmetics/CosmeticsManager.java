package com.example.auravisuals.cosmetics;

import com.example.auravisuals.AuraVisuals;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.mixin.accessor.AbstractTextureAccessor;
import com.example.auravisuals.util.render.WorldOcclusion;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider.Immediate;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.Resource;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class CosmeticsManager {
    public static final CosmeticsManager INSTANCE = new CosmeticsManager();
    final List<CosmeticItem> allCosmetics = new ArrayList<>();
    final Map<String, List<CosmeticItem>> byCategory = new LinkedHashMap<>();
    final Map<String, CosmeticItem> equipped = new ConcurrentHashMap<>();
    final Map<String, Map<String, CosmeticItem>> playerCosmetics = new ConcurrentHashMap<>();
    final Map<Identifier, AbstractTexture> sharpenedTextures = new ConcurrentHashMap<>();
    static final Map<String, CosmeticsManager$WingAdjustment> FIXED_WING_ADJUSTMENTS = Map.ofEntries(
        Map.entry("wings_1_cddf708b", new CosmeticsManager$WingAdjustment(0.05249998F, 0.0F)),
        Map.entry("wings_3_81b06a4b", new CosmeticsManager$WingAdjustment(0.101499975F, -0.0485F)),
        Map.entry("wings_7_ee401e1a", new CosmeticsManager$WingAdjustment(-0.055999994F, 0.11074999F)),
        Map.entry("wings_8_ee401e1a", new CosmeticsManager$WingAdjustment(0.17849997F, 0.094499975F)),
        Map.entry("wings_9_ee401e1a", new CosmeticsManager$WingAdjustment(-0.24849999F, 0.0F)),
        Map.entry("wings_12_f684c6fe", new CosmeticsManager$WingAdjustment(0.0F, 0.10100001F)),
        Map.entry("wings_14_19ec3eb9", new CosmeticsManager$WingAdjustment(-0.1435F, 0.0F)),
        Map.entry("wings_22_3dfa663d", new CosmeticsManager$WingAdjustment(0.0F, -0.064750016F)),
        Map.entry("wings_23_28b34740", new CosmeticsManager$WingAdjustment(0.048999995F, -0.0485F))
    );
    static final Map<String, CosmeticsManager$BackAdjustment> FIXED_BACK_ADJUSTMENTS = Map.ofEntries(
        Map.entry("back_2_42b31d7f", new CosmeticsManager$BackAdjustment(-0.22725F, 0.0F)),
        Map.entry("back_3_0c472e0e", new CosmeticsManager$BackAdjustment(-0.22725F, 180.0F)),
        Map.entry("back_5_a3051e7e", new CosmeticsManager$BackAdjustment(0.0F, 88.20001F)),
        Map.entry("back_10_94b04f2e", new CosmeticsManager$BackAdjustment(-0.24675F, 180.0F)),
        Map.entry("back_12_6d765ef2", new CosmeticsManager$BackAdjustment(-0.2305F, 0.0F)),
        Map.entry("back_15_3d08f827", new CosmeticsManager$BackAdjustment(-0.2305F, 0.0F)),
        Map.entry("back_16_997ccf8a", new CosmeticsManager$BackAdjustment(-0.09075001F, 0.0F)),
        Map.entry("back_17_d31ee85e", new CosmeticsManager$BackAdjustment(-0.2305F, 0.0F)),
        Map.entry("back_20_e91ba7a0", new CosmeticsManager$BackAdjustment(0.0F, 180.0F)),
        Map.entry("back_21_61fb616b", new CosmeticsManager$BackAdjustment(-0.120000005F, 0.0F)),
        Map.entry("back_23_7fef0612", new CosmeticsManager$BackAdjustment(-0.25F, 0.0F)),
        Map.entry("back_24_0212a027", new CosmeticsManager$BackAdjustment(-0.23375F, 180.0F))
    );
    static final Map<String, CosmeticsManager$PetAdjustment> FIXED_PET_ADJUSTMENTS = Map.ofEntries(
        Map.entry("pets_3_4d300ebb", new CosmeticsManager$PetAdjustment(-90.0F, 0.0F, 0.0F)),
        Map.entry("pets_6_d97f2e47", new CosmeticsManager$PetAdjustment(-90.0F, 180.0F, -23.399994F)),
        Map.entry("pets_18_95f0ae96", new CosmeticsManager$PetAdjustment(0.0F, -180.0F, 0.0F)),
        Map.entry("pets_19_95f0ae96", new CosmeticsManager$PetAdjustment(0.0F, -180.0F, 0.0F)),
        Map.entry("pets_20_95f0ae96", new CosmeticsManager$PetAdjustment(0.0F, -180.0F, 0.0F)),
        Map.entry("pets_21_95f0ae96", new CosmeticsManager$PetAdjustment(0.0F, -180.0F, 0.0F))
    );
    static final Map<String, CosmeticsManager$ModelAdjustment> FIXED_MODEL_ADJUSTMENTS = Map.ofEntries(
        Map.entry("hats_2_cf6887c0", new CosmeticsManager$ModelAdjustment(0.0F, 0.0F, -0.16F, 0.0F, 90.0F, -90.0F)),
        Map.entry("hats_13_e2b017aa", new CosmeticsManager$ModelAdjustment(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 180.0F)),
        Map.entry("back_4_73991370", new CosmeticsManager$ModelAdjustment(-0.14F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)),
        Map.entry("back_5_a3051e7e", new CosmeticsManager$ModelAdjustment(0.14F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F)),
        Map.entry("back_11_88774abf", new CosmeticsManager$ModelAdjustment(0.0F, 0.0F, 0.0F, 0.0F, 180.0F, 0.0F)),
        Map.entry("back_14_82cf5272", new CosmeticsManager$ModelAdjustment(-0.1F, -0.1F, 0.0F, 0.0F, 0.0F, 0.0F)),
        Map.entry("back_18_b9cf72a5", new CosmeticsManager$ModelAdjustment(0.0F, 0.0F, 0.15F, 0.0F, 0.0F, 0.0F)),
        Map.entry("back_19_5b7aef16", new CosmeticsManager$ModelAdjustment(0.0F, 0.0F, 0.0F, 0.0F, 180.0F, 0.0F)),
        Map.entry("back_24_0212a027", new CosmeticsManager$ModelAdjustment(0.05F, -0.1F, 0.0F, 0.0F, 0.0F, 0.0F)),
        Map.entry("pets_6_d97f2e47", new CosmeticsManager$ModelAdjustment(0.0F, 0.1F, 0.0F, 0.0F, 0.0F, 0.0F))
    );
    static final CosmeticsManager$ModelAdjustment NO_MODEL_ADJUSTMENT = new CosmeticsManager$ModelAdjustment(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    static final Map<CosmeticItem, CosmeticsManager$Placement> PLACEMENT_CACHE = new ConcurrentHashMap<>();
    boolean loaded = false;
    static final Set<String> TOP_HATS = Set.of(
        "hats_1_f83bf9c2",
        "hats_5_469c9b9e",
        "hats_8_6a481e43",
        "hats_9_5f7f08cb",
        "hats_15_5a319037",
        "hats_16_f37d302a",
        "hats_17_8f15edc6",
        "hats_20_f2a80fc7",
        "hats_25_2b9734ca"
    );
    static final Set<String> LOWER_PLAYER_WINGS = Set.of("wings_8_ee401e1a");
    static final Set<String> HIGHER_PLAYER_WINGS = Set.of("wings_3_81b06a4b");
    static final String EVEN_HIGHER_INFERNAL_WINGS = "wings_4_ac93f2ee";
    static final Set<String> SLIGHTLY_HIGHER_PLAYER_WINGS = Set.of("wings_13_04875af2", "wings_23_28b34740");
    static final String RAISED_PLAYER_WINGS = "wings_9_ee401e1a";
    static final Set<String> OPPOSITE_VERTICAL_WINGS = Set.of(
        "wings_3_81b06a4b", "wings_4_ac93f2ee", "wings_8_ee401e1a", "wings_9_ee401e1a", "wings_13_04875af2", "wings_22_3dfa663d", "wings_23_28b34740"
    );

    CosmeticsManager() {
        this.byCategory.put("all", new ArrayList<>());
        this.byCategory.put("wings", new ArrayList<>());
        this.byCategory.put("hats", new ArrayList<>());
        this.byCategory.put("back", new ArrayList<>());
        this.byCategory.put("pets", new ArrayList<>());
    }

    public synchronized void load() {
        if (!this.loaded) {
            this.loaded = true;
            PLACEMENT_CACHE.clear();
            InputStream is = null;

            try {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc != null && mc.getResourceManager() != null) {
                    Identifier catalogId = Identifier.of("auravisuals", "cosmetics/catalog.json");
                    Optional res = mc.getResourceManager().getResource(catalogId);
                    if (res.isPresent()) {
                        is = ((Resource)res.get()).getInputStream();
                    }
                }

                if (is == null) {
                    is = CosmeticsManager.class.getResourceAsStream("/assets/auravisuals/cosmetics/catalog.json");
                }

                if (is != null) {
                    try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                        for (JsonElement el : JsonParser.parseReader(reader).getAsJsonArray()) {
                            if (el.isJsonObject()) {
                                JsonObject obj = el.getAsJsonObject();
                                String id = obj.get("id").getAsString();
                                String slug = obj.get("slug").getAsString();
                                String name = obj.get("name").getAsString();
                                String cat = obj.get("category").getAsString();
                                String modelPath = obj.get("modelPath").getAsString();
                                String texPath = obj.get("texturePath").getAsString();
                                String prevPath = obj.has("previewPath") ? obj.get("previewPath").getAsString() : texPath;
                                String animPath = obj.has("animationPath") ? obj.get("animationPath").getAsString() : null;
                                String metaPath = obj.has("metaPath") ? obj.get("metaPath").getAsString() : null;
                                float offX = 0.0F;
                                float offY = 0.0F;
                                float offZ = 0.0F;
                                JsonArray offArr;
                                if (obj.has("offset") && obj.get("offset").isJsonArray() && (offArr = obj.getAsJsonArray("offset")).size() >= 3) {
                                    offX = offArr.get(0).getAsFloat();
                                    offY = offArr.get(1).getAsFloat();
                                    offZ = offArr.get(2).getAsFloat();
                                }

                                CosmeticItem item = new CosmeticItem(id, slug, name, cat, modelPath, texPath, prevPath, animPath, offX, offY, offZ);
                                applyOrientationHints(item, metaPath);
                                this.allCosmetics.add(item);
                                this.byCategory.get("all").add(item);
                                if (this.byCategory.containsKey(cat)) {
                                    this.byCategory.get(cat).add(item);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void applyOrientationHints(CosmeticItem item, String metaPath) {
        if (item != null && metaPath != null && !metaPath.isBlank()) {
            try (InputStream stream = openCosmeticResource(metaPath)) {
                if (stream != null) {
                    JsonObject meta = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
                    if (meta.has("locationRotation") && meta.get("locationRotation").isJsonObject()) {
                        JsonObject locations = meta.getAsJsonObject("locationRotation");
                        if (locations.has("normal") && locations.get("normal").isJsonObject()) {
                            JsonObject normal = locations.getAsJsonObject("normal");
                            item.setOrientation(
                                vector(normal, "rotation", new float[]{0.0F, 0.0F, 0.0F}), vector(normal, "scale", new float[]{1.0F, 1.0F, 1.0F})
                            );
                        }
                    }
                }
            } catch (Exception exception) {
                AuraVisuals.LOGGER.warn("Failed to read cosmetic transform for {}", item.getSlug(), exception);
            }
        }
    }

    static InputStream openCosmeticResource(String relativePath) throws Exception {
        MinecraftClient mc = MinecraftClient.getInstance();
        Optional resource;
        return mc != null
                && mc.getResourceManager() != null
                && (resource = mc.getResourceManager().getResource(Identifier.of("auravisuals", relativePath))).isPresent()
            ? ((Resource)resource.get()).getInputStream()
            : CosmeticsManager.class.getResourceAsStream("/assets/auravisuals/" + relativePath);
    }

    static float[] vector(JsonObject object, String key, float[] fallback) {
        if (object != null && object.has(key) && object.get(key).isJsonArray()) {
            JsonArray values = object.getAsJsonArray(key);
            if (values.size() < 3) {
                return fallback;
            }

            float[] result = (float[])fallback.clone();

            for (int index = 0; index < 3; index++) {
                try {
                    float value = values.get(index).getAsFloat();
                    if (Float.isFinite(value)) {
                        result[index] = value;
                    }
                } catch (Exception var7) {
                }
            }

            return result;
        } else {
            return fallback;
        }
    }

    public List<CosmeticItem> getAllCosmetics() {
        this.ensureLoaded();
        return this.allCosmetics;
    }

    public List<CosmeticItem> getByCategory(String category) {
        this.ensureLoaded();
        return this.byCategory.getOrDefault(category, this.allCosmetics);
    }

    public CosmeticItem getCosmeticById(String id) {
        if (id != null && !id.isEmpty()) {
            this.ensureLoaded();

            for (CosmeticItem item : this.allCosmetics) {
                if (item.getId().equalsIgnoreCase(id) || item.getSlug().equalsIgnoreCase(id)) {
                    return item;
                }
            }

            return null;
        } else {
            return null;
        }
    }

    public Map<String, CosmeticItem> getEquippedMap() {
        return Collections.unmodifiableMap(this.equipped);
    }

    public float getWingHeightAdjustment(String slug) {
        CosmeticsManager$WingAdjustment adjustment = FIXED_WING_ADJUSTMENTS.get(slug);
        return adjustment == null ? 0.0F : adjustment.height;
    }

    public float getWingDistanceAdjustment(String slug) {
        CosmeticsManager$WingAdjustment adjustment = FIXED_WING_ADJUSTMENTS.get(slug);
        return adjustment == null ? 0.0F : adjustment.distance;
    }

    public float getBackDistanceAdjustment(String slug) {
        CosmeticsManager$BackAdjustment adjustment = FIXED_BACK_ADJUSTMENTS.get(slug);
        return adjustment == null ? 0.0F : adjustment.distance;
    }

    public float getBackRotationAdjustment(String slug) {
        CosmeticsManager$BackAdjustment adjustment = FIXED_BACK_ADJUSTMENTS.get(slug);
        return adjustment == null ? 0.0F : adjustment.rotation;
    }

    public float getPetRotationX(String slug) {
        CosmeticsManager$PetAdjustment adjustment = FIXED_PET_ADJUSTMENTS.get(slug);
        return adjustment == null ? 0.0F : adjustment.x;
    }

    public float getPetRotationY(String slug) {
        CosmeticsManager$PetAdjustment adjustment = FIXED_PET_ADJUSTMENTS.get(slug);
        return adjustment == null ? 0.0F : adjustment.y;
    }

    public float getPetRotationZ(String slug) {
        CosmeticsManager$PetAdjustment adjustment = FIXED_PET_ADJUSTMENTS.get(slug);
        return adjustment == null ? 0.0F : adjustment.z;
    }

    public boolean isEquipped(CosmeticItem item) {
        if (item == null) {
            return false;
        }

        CosmeticItem eq = this.equipped.get(item.getCategory());
        return eq != null && eq.getId().equals(item.getId());
    }

    public void toggleEquip(CosmeticItem item) {
        if (item != null) {
            if (this.isEquipped(item)) {
                this.equipped.remove(item.getCategory());
                CosmeticsNetworkClient.INSTANCE.sendEquip(item.getCategory(), "");
            } else {
                this.equipped.put(item.getCategory(), item);
                this.ensureModelLoaded(item);
                CosmeticsNetworkClient.INSTANCE.sendEquip(item.getCategory(), item.getId());
            }
        }
    }

    public CosmeticItem getEquipped(String category) {
        return this.equipped.get(category);
    }

    public void applyEquippedConfig(Map<String, String> configured) {
        this.ensureLoaded();
        this.equipped.clear();
        if (configured != null) {
            configured.forEach((category, cosmeticId) -> {
                if (category != null && cosmeticId != null && !cosmeticId.isBlank()) {
                    CosmeticItem item = this.getCosmeticById(cosmeticId);
                    if (item != null && category.equalsIgnoreCase(item.getCategory())) {
                        this.ensureModelLoaded(item);
                        this.equipped.put(item.getCategory(), item);
                    }
                }
            });
        }
    }

    public void setPlayerCosmetic(String playerName, String category, String cosmeticId) {
        if (playerName != null && !playerName.isBlank() && category != null && !category.isBlank()) {
            String key = playerName.trim().toLowerCase(Locale.ROOT);
            String normalizedCategory = category.trim().toLowerCase(Locale.ROOT);
            Map playerMap = this.playerCosmetics.computeIfAbsent(key, ignored -> new ConcurrentHashMap<>());
            if (cosmeticId != null && !cosmeticId.isBlank()) {
                CosmeticItem item = this.getCosmeticById(cosmeticId);
                if (item != null) {
                    this.ensureModelLoaded(item);
                    playerMap.put(normalizedCategory, item);
                }
            } else {
                playerMap.remove(normalizedCategory);
                if (playerMap.isEmpty()) {
                    this.playerCosmetics.remove(key, playerMap);
                }
            }
        }
    }

    public void setAllPlayerCosmetics(Map<String, Map<String, String>> data) {
        this.playerCosmetics.clear();
        if (data != null) {
            data.forEach((player, categories) -> {
                if (player != null && categories != null) {
                    categories.forEach((category, id) -> this.setPlayerCosmetic(player, category, id));
                }
            });
        }
    }

    public Map<String, CosmeticItem> getPlayerEquipped(String playerName) {
        return playerName == null
            ? Collections.emptyMap()
            : this.playerCosmetics.getOrDefault(playerName.trim().toLowerCase(Locale.ROOT), Collections.emptyMap());
    }

    public void ensureLoaded() {
        if (!this.loaded || this.allCosmetics.isEmpty()) {
            this.loaded = false;
            this.load();
        }
    }

    public void ensureModelLoaded(CosmeticItem item) {
        if (item != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (item.getModel() == null) {
                InputStream is = null;

                try {
                    if (mc != null && mc.getResourceManager() != null) {
                        Identifier modelId = Identifier.of("auravisuals", item.getModelPath());
                        Optional res = mc.getResourceManager().getResource(modelId);
                        if (res.isPresent()) {
                            is = ((Resource)res.get()).getInputStream();
                        }
                    }

                    if (is == null) {
                        is = CosmeticsManager.class.getResourceAsStream("/assets/auravisuals/" + item.getModelPath());
                    }

                    if (is != null) {
                        BedrockModel model = BedrockModelParser.parse(is);
                        item.setModel(model);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (item.getAnimation() == null && item.getAnimationPath() != null) {
                InputStream is = null;

                try {
                    if (mc != null && mc.getResourceManager() != null) {
                        Identifier animId = Identifier.of("auravisuals", item.getAnimationPath());
                        Optional res = mc.getResourceManager().getResource(animId);
                        if (res.isPresent()) {
                            is = ((Resource)res.get()).getInputStream();
                        }
                    }

                    if (is == null) {
                        is = CosmeticsManager.class.getResourceAsStream("/assets/auravisuals/" + item.getAnimationPath());
                    }

                    if (is != null) {
                        BedrockAnimation anim = BedrockAnimationParser.parse(is);
                        item.setAnimation(anim);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            this.ensureNearestTexture(item.getTextureId());
        }
    }

    void ensureNearestTexture(Identifier textureId) {
        if (textureId != null) {
            try {
                AbstractTexture texture = MinecraftClient.getInstance().getTextureManager().getTexture(textureId);
                if (this.sharpenedTextures.get(textureId) == texture) {
                    return;
                }

                ((AbstractTextureAccessor)texture).auravisuals$setSampler(RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                this.sharpenedTextures.put(textureId, texture);
            } catch (Throwable throwable) {
                this.sharpenedTextures.remove(textureId);
                AuraVisuals.LOGGER.debug("Cosmetic texture is not ready yet: {}", textureId);
            }
        }
    }

    public void renderWorld(WorldRenderContext context) {
        if (!com.anvil.client.AnvilRuntime.isActive()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.world != null) {
            float tickDelta = context.tickCounter().getTickProgress(true);
            Vec3d cam = context.camera().getCameraPos();
            MatrixStack matrices = context.matrixStack();
            Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
            boolean firstPerson = mc.options.getPerspective().isFirstPerson();

            for (Entity entity : mc.world.getEntities()) {
                CosmeticItem pet;
                PlayerEntity player;
                if (entity instanceof PlayerEntity
                    && ((player = (PlayerEntity)entity) != mc.player || !firstPerson)
                    && player.isAlive()
                    && !player.isInvisible()
                    && WorldOcclusion.isEntityVisible(player, cam, tickDelta)
                    && (pet = this.resolvePlayerCosmetics(player).get("pets")) != null) {
                    this.ensureModelLoaded(pet);
                    if (pet.getModel() != null) {
                        double px = MathHelper.lerp(tickDelta, player.lastRenderX, player.getX()) - cam.x;
                        double py = MathHelper.lerp(tickDelta, player.lastRenderY, player.getY()) - cam.y;
                        double pz = MathHelper.lerp(tickDelta, player.lastRenderZ, player.getZ()) - cam.z;
                        float bodyYaw = MathHelper.lerpAngleDegrees(tickDelta, player.lastBodyYaw, player.bodyYaw);
                        matrices.push();
                        matrices.translate(px, py, pz);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F - bodyYaw));
                        float age = player.age + tickDelta;
                        matrices.translate(0.72F, 0.05F + MathHelper.sin(age * 0.12F) * 0.045F, 0.05F);
                        applyPlacement(matrices, pet, "pets");
                        VertexConsumer consumer = immediate.getBuffer(RenderLayers.entityTranslucent(pet.getTextureId()));
                        pet.getModel().render(matrices, consumer, 15728880, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F, pet.getAnimation(), age * 0.05F);
                        matrices.pop();
                    }
                }
            }

            immediate.draw();
        }
    }

    public Map<String, CosmeticItem> resolvePlayerCosmetics(PlayerEntity player) {
        if (player == null) {
            return Collections.emptyMap();
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (player == mc.player) {
            return this.equipped;
        }

        String profileName = player.getGameProfile() == null ? player.getName().getString() : player.getGameProfile().name();
        Map<String, CosmeticItem> result = this.getPlayerEquipped(profileName);
        if (result.isEmpty()) {
            result = this.getPlayerEquipped(player.getName().getString());
        }

        if (result.isEmpty()) {
            result = this.getPlayerEquipped(player.getUuidAsString());
        }

        return result;
    }

    public void renderFeatureItem(MatrixStack matrices, OrderedRenderCommandQueue queue, CosmeticItem item, String category, float animTime, int light) {
        if (item != null) {
            this.ensureModelLoaded(item);
            if (item.getModel() != null) {
                applyPlacement(matrices, item, category);
                queue.submitCustom(
                    matrices,
                    RenderLayers.entityTranslucent(item.getTextureId()),
                    (entry, consumer) -> item.getModel()
                        .render(entry, consumer, light, OverlayTexture.DEFAULT_UV, 1.0F, 1.0F, 1.0F, 1.0F, item.getAnimation(), animTime)
                );
            }
        }
    }

    static void applyPlacement(MatrixStack matrices, CosmeticItem item, String category) {
        CosmeticsManager$Placement placement = PLACEMENT_CACHE.computeIfAbsent(item, key -> placement(key, category));
        CosmeticsManager$ModelAdjustment modelAdjustment = FIXED_MODEL_ADJUSTMENTS.getOrDefault(item.getSlug(), NO_MODEL_ADJUSTMENT);
        float customHeight = "wings".equals(category) ? INSTANCE.getWingHeightAdjustment(item.getSlug()) : 0.0F;
        float customDistance = "wings".equals(category) ? INSTANCE.getWingDistanceAdjustment(item.getSlug()) : 0.0F;
        if ("back".equals(category)) {
            customDistance = INSTANCE.getBackDistanceAdjustment(item.getSlug());
        }

        matrices.translate(placement.x + modelAdjustment.x, placement.y + customHeight + modelAdjustment.y, placement.z + customDistance + modelAdjustment.z);
        matrices.scale(placement.scale, placement.scale, placement.scale);
        float adjustmentRotationX = modelAdjustment.rotationX;
        float adjustmentRotationY = modelAdjustment.rotationY;
        float adjustmentRotationZ = modelAdjustment.rotationZ;
        if ("back".equals(category)) {
            adjustmentRotationZ += INSTANCE.getBackRotationAdjustment(item.getSlug());
        } else if ("pets".equals(category)) {
            adjustmentRotationX += INSTANCE.getPetRotationX(item.getSlug());
            adjustmentRotationY += INSTANCE.getPetRotationY(item.getSlug());
            adjustmentRotationZ += INSTANCE.getPetRotationZ(item.getSlug());
        }

        applyCenteredRotation(matrices, item, placement.autoYaw, adjustmentRotationX, adjustmentRotationY, adjustmentRotationZ);
        if ("wings".equals(category)) {
            float[] bounds = orientedBounds(item, placement.autoYaw);
            float pivotX = center(bounds[0], bounds[3]);
            float pivotY = center(bounds[1], bounds[4]);
            float pivotZ = center(bounds[2], bounds[5]);
            matrices.translate(pivotX, pivotY, pivotZ);
            if (OPPOSITE_VERTICAL_WINGS.contains(item.getSlug())) {
                matrices.scale(-1.0F, 1.0F, 1.0F);
            } else {
                matrices.scale(1.0F, -1.0F, 1.0F);
            }

            matrices.translate(-pivotX, -pivotY, -pivotZ);
        }

        applyRotation(matrices, item.getRotationX(), item.getRotationY(), item.getRotationZ());
        if (placement.autoYaw != 0.0F) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(placement.autoYaw));
        }

        matrices.scale(item.getOrientationSignX(), item.getOrientationSignY(), item.getOrientationSignZ());
    }

    static void applyCenteredRotation(MatrixStack matrices, CosmeticItem item, float autoYaw, float rotationX, float rotationY, float rotationZ) {
        if (rotationX != 0.0F || rotationY != 0.0F || rotationZ != 0.0F) {
            float[] bounds = orientedBounds(item, autoYaw);
            float pivotX = center(bounds[0], bounds[3]);
            float pivotY = center(bounds[1], bounds[4]);
            float pivotZ = center(bounds[2], bounds[5]);
            matrices.translate(pivotX, pivotY, pivotZ);
            applyRotation(matrices, rotationX, rotationY, rotationZ);
            matrices.translate(-pivotX, -pivotY, -pivotZ);
        }
    }

    static CosmeticsManager$Placement placement(CosmeticItem item, String category) {
        float autoYaw = 0.0F;
        float[] bounds = orientedBounds(item, 0.0F);
        float width = bounds[3] - bounds[0];
        float height = bounds[4] - bounds[1];
        float depth = bounds[5] - bounds[2];
        if ("wings".equals(category) && depth > width * 1.35F) {
            autoYaw = 90.0F;
            bounds = orientedBounds(item, autoYaw);
            width = bounds[3] - bounds[0];
            height = bounds[4] - bounds[1];
            depth = bounds[5] - bounds[2];
        }

        if ("hats".equals(category)) {
            float scale = Math.min(1.0F, Math.min(safeRatio(1.25F, width), Math.min(safeRatio(1.15F, height), safeRatio(1.1F, depth))));
            float largest = Math.max(width, Math.max(height, depth));
            if (largest < 0.35F) {
                scale = Math.min(1.65F, safeRatio(0.52F, largest));
            } else if (largest < 0.55F) {
                scale = Math.min(1.3F, safeRatio(0.62F, largest));
            }

            float targetX = 0.0F;
            float targetY = TOP_HATS.contains(item.getSlug()) ? 0.27F : -0.03F;
            float targetZ = 0.0F;
            return new CosmeticsManager$Placement(
                targetX - center(bounds[0], bounds[3]) * scale, targetY - bounds[1] * scale, targetZ - center(bounds[2], bounds[5]) * scale, scale, autoYaw
            );
        } else if ("wings".equals(category)) {
            float scale = Math.min(1.0F, Math.min(safeRatio(2.8F, width), safeRatio(1.75F, height)));
            float targetY = -0.32F;
            float targetZ = 0.14F;
            if (LOWER_PLAYER_WINGS.contains(item.getSlug())) {
                targetY -= 0.06F;
            }

            if (HIGHER_PLAYER_WINGS.contains(item.getSlug())) {
                targetY += 0.16F;
            }

            if ("wings_4_ac93f2ee".equals(item.getSlug())) {
                targetY += 0.21F;
            }

            if (SLIGHTLY_HIGHER_PLAYER_WINGS.contains(item.getSlug())) {
                targetY += 0.08F;
            }

            if ("wings_9_ee401e1a".equals(item.getSlug())) {
                targetY += 0.11F;
                targetZ += 0.14F;
            }

            return new CosmeticsManager$Placement(
                -center(bounds[0], bounds[3]) * scale, targetY - center(bounds[1], bounds[4]) * scale, targetZ - bounds[2] * scale, scale, autoYaw
            );
        } else if ("back".equals(category)) {
            float scale = Math.min(1.0F, Math.min(safeRatio(1.18F, width), Math.min(safeRatio(1.48F, height), safeRatio(1.05F, depth))));
            return new CosmeticsManager$Placement(
                -center(bounds[0], bounds[3]) * scale, -0.3F - center(bounds[1], bounds[4]) * scale, 0.1F - bounds[2] * scale, scale, autoYaw
            );
        } else {
            float scale = Math.min(0.9F, Math.min(safeRatio(0.72F, width), Math.min(safeRatio(0.76F, height), safeRatio(0.72F, depth))));
            return new CosmeticsManager$Placement(
                -center(bounds[0], bounds[3]) * scale, -bounds[1] * scale, -center(bounds[2], bounds[5]) * scale, scale, autoYaw
            );
        }
    }

    static float[] orientedBounds(CosmeticItem item, float autoYaw) {
        BedrockModel model = item.getModel();
        Matrix4f transform = new Matrix4f();
        if (item.getRotationZ() != 0.0F) {
            transform.rotateZ((float)Math.toRadians(item.getRotationZ()));
        }

        if (item.getRotationY() != 0.0F) {
            transform.rotateY((float)Math.toRadians(item.getRotationY()));
        }

        if (item.getRotationX() != 0.0F) {
            transform.rotateX((float)Math.toRadians(item.getRotationX()));
        }

        if (autoYaw != 0.0F) {
            transform.rotateY((float)Math.toRadians(autoYaw));
        }

        transform.scale(item.getOrientationSignX(), item.getOrientationSignY(), item.getOrientationSignZ());
        float[] result = new float[]{
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY
        };
        Vector3f point = new Vector3f();
        float[] xs = new float[]{model.minX, model.maxX};
        float[] ys = new float[]{model.minY, model.maxY};
        float[] zs = new float[]{model.minZ, model.maxZ};

        for (float x : xs) {
            for (float y : ys) {
                for (float z : zs) {
                    transform.transformPosition(x, y, z, point);
                    result[0] = Math.min(result[0], point.x);
                    result[3] = Math.max(result[3], point.x);
                    result[1] = Math.min(result[1], point.y);
                    result[4] = Math.max(result[4], point.y);
                    result[2] = Math.min(result[2], point.z);
                    result[5] = Math.max(result[5], point.z);
                }
            }
        }

        return result;
    }

    static float safeRatio(float target, float value) {
        return value > 1.0E-4F ? target / value : 1.0F;
    }

    static float center(float min, float max) {
        return (min + max) * 0.5F;
    }

    static void applyRotation(MatrixStack matrices, float x, float y, float z) {
        if (z != 0.0F) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(z));
        }

        if (y != 0.0F) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(y));
        }

        if (x != 0.0F) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(x));
        }
    }
}
