package com.example.auravisuals.util.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class LiquidGlassRenderer {
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
        .withLocation(Identifier.of("auravisuals", "pipeline/liquid_glass"))
        .withVertexShader(Identifier.of("auravisuals", "core/liquid_glass"))
        .withFragmentShader(Identifier.of("auravisuals", "core/liquid_glass"))
        .withSampler("Sampler0")
        .withVertexFormat(VertexFormats.POSITION_TEXTURE, DrawMode.TRIANGLES)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withCull(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false)
        .build();
    private static SimpleFramebuffer sceneCopy;

    private LiquidGlassRenderer() {
    }

    public static void render(float opacity, float time, float refraction) {
        GpuDevice device = RenderSystem.tryGetDevice();
        if (device != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            Framebuffer main = mc.getFramebuffer();
            if (main != null && main.getColorAttachment() != null && main.getColorAttachmentView() != null) {
                if (sceneCopy == null || sceneCopy.textureWidth != main.textureWidth || sceneCopy.textureHeight != main.textureHeight) {
                    if (sceneCopy != null) {
                        sceneCopy.delete();
                    }

                    sceneCopy = new SimpleFramebuffer("bamber client-liquidglass-scene", main.textureWidth, main.textureHeight, false);
                }

                GpuTexture sceneTex = sceneCopy.getColorAttachment();
                GpuTextureView sceneView = sceneCopy.getColorAttachmentView();
                if (sceneTex != null && sceneView != null) {
                    device.createCommandEncoder()
                        .copyTextureToTexture(main.getColorAttachment(), sceneTex, 0, 0, 0, 0, 0, main.textureWidth, main.textureHeight);
                    BufferBuilder bb = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE);
                    vtx(bb, -1.0F, -1.0F, 0.0F, 0.0F);
                    vtx(bb, 1.0F, -1.0F, 1.0F, 0.0F);
                    vtx(bb, 1.0F, 1.0F, 1.0F, 1.0F);
                    vtx(bb, -1.0F, -1.0F, 0.0F, 0.0F);
                    vtx(bb, 1.0F, 1.0F, 1.0F, 1.0F);
                    vtx(bb, -1.0F, 1.0F, 0.0F, 1.0F);
                    BuiltBuffer built = bb.endNullable();
                    if (built != null) {
                        Vector4f color = new Vector4f(time, opacity, refraction, 0.0F);
                        Vector3f offset = new Vector3f(main.textureWidth, main.textureHeight, 0.0F);
                        GpuBufferSlice uniforms = RenderSystem.getDynamicUniforms().write(RenderSystem.getModelViewMatrix(), color, offset, new Matrix4f());
                        GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
                        GpuTextureView colorView = main.getColorAttachmentView();
                        BuiltBuffer var15 = built;

                        try {
                            GpuBuffer vbo = PIPELINE.getVertexFormat().uploadImmediateVertexBuffer(built.getBuffer());
                            ShapeIndexBuffer sib = RenderSystem.getSequentialBuffer(built.getDrawParameters().mode());
                            GpuBuffer ibo = sib.getIndexBuffer(built.getDrawParameters().indexCount());
                            IndexType it = sib.getIndexType();

                            try (RenderPass pass = device.createCommandEncoder()
                                    .createRenderPass(() -> "AuraLiquidGlass", colorView, OptionalInt.empty(), null, OptionalDouble.empty())) {
                                pass.setPipeline(PIPELINE);
                                RenderSystem.bindDefaultUniforms(pass);
                                pass.setUniform("DynamicTransforms", uniforms);
                                pass.bindTexture("Sampler0", sceneView, sampler);
                                pass.setVertexBuffer(0, vbo);
                                pass.setIndexBuffer(ibo, it);
                                pass.drawIndexed(0, 0, built.getDrawParameters().indexCount(), 1);
                            }
                        } catch (Throwable var26) {
                            if (built != null) {
                                try {
                                    var15.close();
                                } catch (Throwable var23) {
                                    var26.addSuppressed(var23);
                                }
                            }

                            throw var26;
                        }

                        if (built != null) {
                            built.close();
                        }
                    }
                }
            }
        }
    }

    private static void vtx(BufferBuilder bb, float x, float y, float u, float v) {
        bb.vertex(x, y, 0.0F).texture(u, v);
    }
}
