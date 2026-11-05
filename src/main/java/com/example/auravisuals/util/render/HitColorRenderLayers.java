package com.example.auravisuals.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderPipeline.Snippet;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.RenderSetup.OutlineMode;
import net.minecraft.util.Identifier;

public final class HitColorRenderLayers {
    private static final RenderPipeline TINT_PIPELINE = RenderPipeline.builder(snippet("ENTITY_SNIPPET"))
        .withLocation(Identifier.of("auravisuals", "pipeline/hit_color_tint"))
        .withFragmentShader(Identifier.of("auravisuals", "core/hit_color_entity"))
        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
        .withShaderDefine("PER_FACE_LIGHTING")
        .withSampler("Sampler1")
        .withCull(false)
        .build();
    private static final RenderPipeline FULL_PIPELINE = RenderPipeline.builder(snippet("ENTITY_SNIPPET"))
        .withLocation(Identifier.of("auravisuals", "pipeline/hit_color_full"))
        .withFragmentShader(Identifier.of("auravisuals", "core/hit_color_entity"))
        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
        .withShaderDefine("PER_FACE_LIGHTING")
        .withShaderDefine("FULL_COLOR")
        .withSampler("Sampler1")
        .withCull(false)
        .build();
    private static final Map<Identifier, RenderLayer> TINT_LAYERS = new ConcurrentHashMap<>();
    private static final Map<Identifier, RenderLayer> FULL_LAYERS = new ConcurrentHashMap<>();

    private static Snippet snippet(String name) {
        try {
            Field f = RenderPipelines.class.getDeclaredField(name);
            f.setAccessible(true);
            return (Snippet)f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static RenderLayer createLayer(String name, RenderSetup setup) {
        try {
            Method m = RenderLayer.class.getDeclaredMethod("of", String.class, RenderSetup.class);
            m.setAccessible(true);
            return (RenderLayer)m.invoke(null, name, setup);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private HitColorRenderLayers() {
    }

    public static RenderLayer tint(Identifier texture) {
        return TINT_LAYERS.computeIfAbsent(texture, id -> create("bamber client_hit_color_tint", TINT_PIPELINE, id));
    }

    public static RenderLayer full(Identifier texture) {
        return FULL_LAYERS.computeIfAbsent(texture, id -> create("bamber client_hit_color_full", FULL_PIPELINE, id));
    }

    private static RenderLayer create(String name, RenderPipeline pipeline, Identifier texture) {
        RenderSetup setup = RenderSetup.builder(pipeline)
            .texture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .crumbling()
            .outlineMode(OutlineMode.AFFECTS_OUTLINE)
            .build();
        return createLayer(name, setup);
    }
}
