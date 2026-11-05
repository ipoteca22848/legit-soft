package com.example.auravisuals.util.render;

import com.example.auravisuals.compat.ProjectionHolder;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.RenderSystem.ShapeIndexBuffer;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import com.mojang.blaze3d.vertex.VertexFormat.IndexType;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BuiltBuffer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

public final class AmbienceRenderer {
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET)
        .withLocation(Identifier.of("auravisuals", "pipeline/ambience"))
        .withVertexShader(Identifier.of("auravisuals", "core/ambience"))
        .withFragmentShader(Identifier.of("auravisuals", "core/ambience"))
        .withVertexFormat(VertexFormats.POSITION, DrawMode.TRIANGLES)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withCull(false)
        .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
        .withDepthWrite(false)
        .build();
    private static final long startedAt = System.currentTimeMillis();

    private AmbienceRenderer() {
    }

    public static void render(int type, float opacity, float speed, float scale, float seed) {
        if (RenderSystem.tryGetDevice() != null) {
            MinecraftClient mc = MinecraftClient.getInstance();
            Framebuffer framebuffer = mc.getFramebuffer();
            if (framebuffer != null) {
                GpuTextureView colorView = RenderSystem.outputColorTextureOverride != null
                    ? RenderSystem.outputColorTextureOverride
                    : framebuffer.getColorAttachmentView();
                if (colorView != null) {
                    GpuTextureView depthView = framebuffer.useDepthAttachment
                        ? (RenderSystem.outputDepthTextureOverride != null ? RenderSystem.outputDepthTextureOverride : framebuffer.getDepthAttachmentView())
                        : null;
                    if (depthView != null) {
                        BufferBuilder bb = Tessellator.getInstance().begin(DrawMode.TRIANGLES, VertexFormats.POSITION);
                        bb.vertex(-1.0F, -1.0F, 0.0F);
                        bb.vertex(1.0F, -1.0F, 0.0F);
                        bb.vertex(1.0F, 1.0F, 0.0F);
                        bb.vertex(-1.0F, -1.0F, 0.0F);
                        bb.vertex(1.0F, 1.0F, 0.0F);
                        bb.vertex(-1.0F, 1.0F, 0.0F);
                        BuiltBuffer built = bb.endNullable();
                        if (built != null) {
                            float time = (float)(System.currentTimeMillis() - startedAt) / 1000.0F * Math.max(0.01F, speed);
                            float resW = Math.max(1.0F, mc.getWindow().getFramebufferWidth());
                            float resH = Math.max(1.0F, mc.getWindow().getFramebufferHeight());
                            float op = Math.max(0.0F, Math.min(1.0F, opacity));
                            Matrix4f basis = new Matrix4f();
                            Matrix4f proj = ProjectionHolder.lastProjectionMatrix;
                            Camera cam = mc.gameRenderer.getCamera();
                            if (proj != null && cam != null) {
                                Quaternionf rot = new Quaternionf(cam.getRotation());
                                Vector3f fwd = rot.transform(new Vector3f(0.0F, 0.0F, -1.0F));
                                Vector3f up = rot.transform(new Vector3f(0.0F, 1.0F, 0.0F));
                                Vector3f rgt = rot.transform(new Vector3f(1.0F, 0.0F, 0.0F));
                                float thx = 1.0F / proj.m00();
                                float thy = 1.0F / proj.m11();
                                basis = new Matrix4f(rgt.x, rgt.y, rgt.z, thx, up.x, up.y, up.z, thy, fwd.x, fwd.y, fwd.z, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
                            }

                            GpuBufferSlice uniforms = RenderSystem.getDynamicUniforms()
                                .write(new Matrix4f(), new Vector4f(time, op, seed, type), new Vector3f(resW, resH, Math.max(0.5F, scale)), basis);
                            BuiltBuffer var32 = built;

                            try {
                                GpuBuffer vertexBuffer = PIPELINE.getVertexFormat().uploadImmediateVertexBuffer(built.getBuffer());
                                ShapeIndexBuffer shapeIndexBuffer = RenderSystem.getSequentialBuffer(built.getDrawParameters().mode());
                                GpuBuffer indexBuffer = shapeIndexBuffer.getIndexBuffer(built.getDrawParameters().indexCount());
                                IndexType indexType = shapeIndexBuffer.getIndexType();

                                try (RenderPass pass = RenderSystem.getDevice()
                                        .createCommandEncoder()
                                        .createRenderPass(() -> "AuraAmbience", colorView, OptionalInt.empty(), depthView, OptionalDouble.empty())) {
                                    pass.setPipeline(PIPELINE);
                                    RenderSystem.bindDefaultUniforms(pass);
                                    pass.setUniform("DynamicTransforms", uniforms);
                                    pass.setVertexBuffer(0, vertexBuffer);
                                    pass.setIndexBuffer(indexBuffer, indexType);
                                    pass.drawIndexed(0, 0, built.getDrawParameters().indexCount(), 1);
                                }
                            } catch (Throwable var30) {
                                if (built != null) {
                                    try {
                                        var32.close();
                                    } catch (Throwable var27) {
                                        var30.addSuppressed(var27);
                                    }
                                }

                                throw var30;
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
}
