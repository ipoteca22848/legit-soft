package com.example.auravisuals.util.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.GpuSampler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class ImpactLensRenderer {
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
        .withLocation(Identifier.of("auravisuals", "pipeline/impact_lens"))
        .withVertexShader(Identifier.of("auravisuals", "core/impact_lens"))
        .withFragmentShader(Identifier.of("auravisuals", "core/impact_lens"))
        .withSampler("Sampler0")
        .withVertexFormat(VertexFormats.POSITION_TEXTURE, DrawMode.TRIANGLES)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withCull(false)
        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        .withDepthWrite(false)
        .build();

    private ImpactLensRenderer() {
    }

    public static void render(Matrix4f modelView, Camera camera, Vec3d center, float radius, float progress, float strength, float fade, float spin, int color) {
        if (camera != null) {
            Quaternionf rotation = new Quaternionf(camera.getRotation());
            Vector3f right = rotation.transform(new Vector3f(1.0F, 0.0F, 0.0F));
            Vector3f up = rotation.transform(new Vector3f(0.0F, 1.0F, 0.0F));
            renderOriented(modelView, camera, center, radius, progress, strength, fade, spin, color, right, up);
        }
    }

    public static void renderHorizontal(
        Matrix4f modelView, Camera camera, Vec3d center, float radius, float progress, float strength, float fade, float spin, int color
    ) {
        renderOriented(modelView, camera, center, radius, progress, strength, fade, spin, color, new Vector3f(1.0F, 0.0F, 0.0F), new Vector3f(0.0F, 0.0F, 1.0F));
    }

    private static void renderOriented(
        Matrix4f modelView,
        Camera camera,
        Vec3d center,
        float radius,
        float progress,
        float strength,
        float fade,
        float spin,
        int color,
        Vector3f right,
        Vector3f up
    ) {
        if (modelView != null && camera != null && center != null && !(radius <= 0.01F) && !(fade <= 0.003F)) {
            GpuDevice device = RenderSystem.tryGetDevice();
            if (device != null) {
                MinecraftClient mc = MinecraftClient.getInstance();
                Framebuffer main = mc.getFramebuffer();
                if (main != null && main.getColorAttachmentView() != null) {
                    GpuTextureView sceneView = StableWorldFrameCapture.colorView();
                    if (sceneView != null && StableWorldFrameCapture.width() == main.textureWidth && StableWorldFrameCapture.height() == main.textureHeight) {
                        CommandEncoder encoder = device.createCommandEncoder();
                        Vec3d relative = center.subtract(camera.getCameraPos());
                        BufferBuilder builder = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION_TEXTURE);
                        vertex(builder, modelView, relative, right, up, -radius, -radius, 0.0F, 0.0F);
                        vertex(builder, modelView, relative, right, up, radius, -radius, 1.0F, 0.0F);
                        vertex(builder, modelView, relative, right, up, radius, radius, 1.0F, 1.0F);
                        vertex(builder, modelView, relative, right, up, -radius, -radius, 0.0F, 0.0F);
                        vertex(builder, modelView, relative, right, up, radius, radius, 1.0F, 1.0F);
                        vertex(builder, modelView, relative, right, up, -radius, radius, 0.0F, 1.0F);
                        BuiltBuffer built = builder.endNullable();
                        if (built != null) {
                            Matrix4f textureMatrix = new Matrix4f(
                                progress,
                                strength,
                                fade,
                                spin,
                                (color >> 16 & 0xFF) / 255.0F,
                                (color >> 8 & 0xFF) / 255.0F,
                                (color & 0xFF) / 255.0F,
                                (color >>> 24 & 0xFF) / 255.0F,
                                0.0F,
                                0.0F,
                                0.0F,
                                0.0F,
                                0.0F,
                                0.0F,
                                0.0F,
                                1.0F
                            );
                            GpuBufferSlice uniforms = RenderSystem.getDynamicUniforms()
                                .write(RenderSystem.getModelViewMatrix(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F), new Vector3f(), textureMatrix);
                            GpuSampler sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR);
                            GpuTextureView colorView = main.getColorAttachmentView();
                            GpuTextureView depthView = main.useDepthAttachment ? main.getDepthAttachmentView() : null;
                            BuiltBuffer var24 = built;

                            try {
                                GpuBuffer vertexBuffer = PIPELINE.getVertexFormat().uploadImmediateVertexBuffer(built.getBuffer());
                                ShapeIndexBuffer sequential = RenderSystem.getSequentialBuffer(built.getDrawParameters().mode());
                                GpuBuffer indexBuffer = sequential.getIndexBuffer(built.getDrawParameters().indexCount());
                                IndexType indexType = sequential.getIndexType();

                                try (RenderPass pass = encoder.createRenderPass(
                                        () -> "AuraImpactLens", colorView, OptionalInt.empty(), depthView, OptionalDouble.empty()
                                    )) {
                                    pass.setPipeline(PIPELINE);
                                    RenderSystem.bindDefaultUniforms(pass);
                                    pass.setUniform("DynamicTransforms", uniforms);
                                    pass.bindTexture("Sampler0", sceneView, sampler);
                                    pass.setVertexBuffer(0, vertexBuffer);
                                    pass.setIndexBuffer(indexBuffer, indexType);
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

    private static void vertex(BufferBuilder builder, Matrix4f modelView, Vec3d center, Vector3f right, Vector3f up, float x, float y, float u, float v) {
        Vector3f position = new Vector3f(
            (float)center.x + right.x * x + up.x * y, (float)center.y + right.y * x + up.y * y, (float)center.z + right.z * x + up.z * y
        );
        modelView.transformPosition(position);
        builder.vertex(position.x, position.y, position.z).texture(u, v);
    }
}
