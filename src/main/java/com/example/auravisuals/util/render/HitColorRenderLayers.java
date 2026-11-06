package com.example.auravisuals.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.RenderSetup.OutlineMode;
import net.minecraft.util.Identifier;

public final class HitColorRenderLayers {
    private static final RenderPipeline TINT_PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
        .withLocation(Identifier.of("auravisuals", "pipeline/hit_color_tint"))
        .withFragmentShader(Identifier.of("auravisuals", "core/hit_color_entity"))
        .withShaderDefine("ALPHA_CUTOUT", 0.1F)
        .withShaderDefine("PER_FACE_LIGHTING")
        .withSampler("Sampler1")
        .withCull(false)
        .build();
    private static final RenderPipeline FULL_PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
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

    private HitColorRenderLayers() {
    }

    public static RenderLayer tint(Identifier texture) {
        return TINT_LAYERS.computeIfAbsent(texture, id -> create("anvil visual_hit_color_tint", TINT_PIPELINE, id));
    }

    public static RenderLayer full(Identifier texture) {
        return FULL_LAYERS.computeIfAbsent(texture, id -> create("anvil visual_hit_color_full", FULL_PIPELINE, id));
    }

    private static RenderLayer create(String name, RenderPipeline pipeline, Identifier texture) {
        RenderSetup setup = RenderSetup.builder(pipeline)
            .texture("Sampler0", texture)
            .useLightmap()
            .useOverlay()
            .crumbling()
            .outlineMode(OutlineMode.AFFECTS_OUTLINE)
            .build();
        return RenderLayer.of(name, setup);
    }
}
