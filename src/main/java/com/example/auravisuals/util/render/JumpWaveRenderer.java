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
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class JumpWaveRenderer {
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
        .withLocation(Identifier.of("auravisuals", "pipeline/jump_wave"))
        .withVertexShader(Identifier.of("auravisuals", "core/jump_wave"))
        .withFragmentShader(Identifier.of("auravisuals", "core/jump_wave"))
        .withSampler("Sampler0")
        .withVertexFormat(VertexFormats.POSITION_TEXTURE, DrawMode.TRIANGLES)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withCull(false)
        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        .withDepthWrite(false)
        .build();
    private static SimpleFramebuffer sceneCopy;

    private JumpWaveRenderer() {
    }

    public static void render(Matrix4f modelView, Vec3d cam, Vec3d center, float maxRadius, float ringProgress, float strength, float fade, int crestColor) {
        renderInternal(modelView, cam, center, maxRadius, ringProgress, strength, fade, crestColor, null, 0, 0.0F, 0);
    }

    public static void renderAmbience(
        Matrix4f modelView, Vec3d cam, Vec3d center, float maxRadius, float ringProgress, float fade, float speed, String effect, int tintColor
    ) {
        if (modelView != null && cam != null && center != null && !(maxRadius <= 0.001F) && !(fade <= 0.003F)) {
            float ringRadius = maxRadius * Math.max(0.025F, ringProgress);
            float innerRadius = ringRadius * 0.9F;
            float outerRadius = ringRadius * 1.34F;
            int segments = 64;
            float[] vertices = new float[2304];
            double cx = center.x - cam.x;
            double cy = center.y - cam.y + 0.035;
            double cz = center.z - cam.z;
            int offset = 0;

            for (int i = 0; i < 64; i++) {
                float a0 = (float)((Math.PI * 2) * i / 64.0);
                float a1 = (float)((Math.PI * 2) * (i + 1) / 64.0);
                offset = shaderVertex(
                    vertices,
                    offset,
                    cx + Math.cos(a0) * innerRadius,
                    cy,
                    cz + Math.sin(a0) * innerRadius,
                    0.5F + (float)Math.cos(a0) * (innerRadius / outerRadius) * 0.5F,
                    0.5F + (float)Math.sin(a0) * (innerRadius / outerRadius) * 0.5F
                );
                offset = shaderVertex(
                    vertices,
                    offset,
                    cx + Math.cos(a0) * outerRadius,
                    cy,
                    cz + Math.sin(a0) * outerRadius,
                    0.5F + (float)Math.cos(a0) * 0.5F,
                    0.5F + (float)Math.sin(a0) * 0.5F
                );
                offset = shaderVertex(
                    vertices,
                    offset,
                    cx + Math.cos(a1) * outerRadius,
                    cy,
                    cz + Math.sin(a1) * outerRadius,
                    0.5F + (float)Math.cos(a1) * 0.5F,
                    0.5F + (float)Math.sin(a1) * 0.5F
                );
                offset = shaderVertex(
                    vertices,
                    offset,
                    cx + Math.cos(a0) * innerRadius,
                    cy,
                    cz + Math.sin(a0) * innerRadius,
                    0.5F + (float)Math.cos(a0) * (innerRadius / outerRadius) * 0.5F,
                    0.5F + (float)Math.sin(a0) * (innerRadius / outerRadius) * 0.5F
                );
                offset = shaderVertex(
                    vertices,
                    offset,
                    cx + Math.cos(a1) * outerRadius,
                    cy,
                    cz + Math.sin(a1) * outerRadius,
                    0.5F + (float)Math.cos(a1) * 0.5F,
                    0.5F + (float)Math.sin(a1) * 0.5F
                );
                offset = shaderVertex(
                    vertices,
                    offset,
                    cx + Math.cos(a1) * innerRadius,
                    cy,
                    cz + Math.sin(a1) * innerRadius,
                    0.5F + (float)Math.cos(a1) * (innerRadius / outerRadius) * 0.5F,
                    0.5F + (float)Math.sin(a1) * (innerRadius / outerRadius) * 0.5F
                );
            }

            float seed = (float)Math.abs(Math.sin(center.x * 0.173 + center.y * 0.317 + center.z * 0.619));
            BlockOutlineShaderUtil.renderRing(vertices, 4, modelView, Math.min(1.0F, fade * 0.82F), effect, speed, seed, tintColor & 16777215);
        }
    }

    private static int shaderVertex(float[] out, int offset, double x, double y, double z, float u, float v) {
        out[offset++] = (float)x;
        out[offset++] = (float)y;
        out[offset++] = (float)z;
        out[offset++] = u;
        out[offset++] = 0.0F;
        out[offset++] = v;
        return offset;
    }

    public static void renderTerrain(
        Matrix4f modelView, Vec3d cam, Vec3d center, float maxRadius, float ringProgress, float strength, float fade, int crestColor, float[] terrainVertices
    ) {
        if (terrainVertices != null && terrainVertices.length >= 30) {
            renderInternal(modelView, cam, center, maxRadius, ringProgress, strength, fade, crestColor, terrainVertices, 0, 0.0F, 0);
        }
    }

    private static void renderInternal(
        Matrix4f modelView,
        Vec3d cam,
        Vec3d center,
        float maxRadius,
        float ringProgress,
        float strength,
        float fade,
        int crestColor,
        float[] terrainVertices,
        int mode,
        float time,
        int effect
    ) {
        if (modelView != null && !(maxRadius <= 0.001F) && !(fade <= 0.003F)) {
            GpuDevice device = RenderSystem.tryGetDevice();
            if (device != null) {
                MinecraftClient mc = MinecraftClient.getInstance();
                Framebuffer main = mc.getFramebuffer();
                if (main != null && main.getColorAttachment() != null && main.getColorAttachmentView() != null) {
                    if (sceneCopy == null || sceneCopy.textureWidth != main.textureWidth || sceneCopy.textureHeight != main.textureHeight) {
                        if (sceneCopy != null) {
                            sceneCopy.delete();
                        }

                        sceneCopy = new SimpleFramebuffer("bamber client-jumpwave-scene", main.textureWidth, main.textureHeight, false);
                    }

                    GpuTexture sceneTex = sceneCopy.getColorAttachment();
                    GpuTextureView sceneView = sceneCopy.getColorAttachmentView();
                    if (sceneTex != null && sceneView != null) {
                        FramebufferBlitter.blit(main.getColorAttachmentView(), sceneView);
                        BufferBuilder bb = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE);
                        if (terrainVertices == null) {
                            double cx = center.x - cam.x;
                            double cy = center.y - cam.y + 0.03;
                            double cz = center.z - cam.z;
                            vtx(bb, modelView, cx + maxRadius, cy, cz + maxRadius, 1.0F, 1.0F);
                            vtx(bb, modelView, cx + maxRadius, cy, cz - maxRadius, 1.0F, 0.0F);
                            vtx(bb, modelView, cx - maxRadius, cy, cz - maxRadius, 0.0F, 0.0F);
                            vtx(bb, modelView, cx + maxRadius, cy, cz + maxRadius, 1.0F, 1.0F);
                            vtx(bb, modelView, cx - maxRadius, cy, cz - maxRadius, 0.0F, 0.0F);
                            vtx(bb, modelView, cx - maxRadius, cy, cz + maxRadius, 0.0F, 1.0F);
                        } else {
                            for (int i = 0; i + 4 < terrainVertices.length; i += 5) {
                                vtx(
                                    bb,
                                    modelView,
                                    terrainVertices[i] - cam.x,
                                    terrainVertices[i + 1] - cam.y,
                                    terrainVertices[i + 2] - cam.z,
                                    terrainVertices[i + 3],
                                    terrainVertices[i + 4]
                                );
                            }
                        }

                        BuiltBuffer built = bb.endNullable();
                        if (built != null) {
                            Matrix4f texMat = new Matrix4f(
                                ringProgress,
                                strength,
                                fade,
                                mode,
                                (crestColor >> 16 & 0xFF) / 255.0F,
                                (crestColor >> 8 & 0xFF) / 255.0F,
                                (crestColor & 0xFF) / 255.0F,
                                (crestColor >>> 24 & 0xFF) / 255.0F,
                                time,
                                effect,
                                0.0F,
                                0.0F,
                                0.0F,
                                0.0F,
                                0.0F,
                                1.0F
                            );
                            GpuBufferSlice uniforms = RenderSystem.getDynamicUniforms()
                                .write(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), texMat);
                            GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
                            GpuTextureView colorView = main.getColorAttachmentView();
                            GpuTextureView depthView = main.useDepthAttachment ? main.getDepthAttachmentView() : null;
                            BuiltBuffer var24 = built;

                            try {
                                GpuBuffer vbo = PIPELINE.getVertexFormat().uploadImmediateVertexBuffer(built.getBuffer());
                                ShapeIndexBuffer sib = RenderSystem.getSequentialBuffer(built.getDrawParameters().mode());
                                GpuBuffer ibo = sib.getIndexBuffer(built.getDrawParameters().indexCount());
                                IndexType it = sib.getIndexType();

                                try (RenderPass pass = device.createCommandEncoder()
                                        .createRenderPass(() -> "AuraJumpWave", colorView, OptionalInt.empty(), depthView, OptionalDouble.empty())) {
                                    pass.setPipeline(PIPELINE);
                                    RenderSystem.bindDefaultUniforms(pass);
                                    pass.setUniform("DynamicTransforms", uniforms);
                                    pass.bindTexture("Sampler0", sceneView, sampler);
                                    pass.setVertexBuffer(0, vbo);
                                    pass.setIndexBuffer(ibo, it);
                                    pass.drawIndexed(0, 0, built.getDrawParameters().indexCount(), 1);
                                }
                            } catch (Throwable var35) {
                                if (built != null) {
                                    try {
                                        var24.close();
                                    } catch (Throwable var32) {
                                        var35.addSuppressed(var32);
                                    }
                                }

                                throw var35;
                            }

                            if (built != null) {
                                built.close();
                            }
                        }
                    }
                }
            }
        }
    }

    private static void vtx(BufferBuilder bb, Matrix4f mv, double x, double y, double z, float u, float v) {
        Vector3f p = new Vector3f((float)x, (float)y, (float)z);
        mv.transformPosition(p);
        bb.vertex(p.x, p.y, p.z).texture(u, v);
    }
}
