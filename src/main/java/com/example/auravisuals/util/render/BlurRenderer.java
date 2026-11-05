package com.example.auravisuals.util.render;

import java.nio.ByteBuffer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class BlurRenderer {
    private static int fboA = -1;
    private static int fboB = -1;
    private static int texA = -1;
    private static int texB = -1;
    private static int blurW;
    private static int blurH;
    private static boolean ready = false;
    private static long lastPrepareMs = -1L;
    private static int lastPreparePassCount = -1;
    private static int lastPrepareFw = -1;
    private static int lastPrepareFh = -1;

    public static boolean isReady() {
        return ready;
    }

    public static void prepare(int passes) {
        if (!FabricLoader.getInstance().isModLoaded("vulkanmod")) {
            MinecraftClient mc = MinecraftClient.getInstance();
            int fw = mc.getWindow().getFramebufferWidth();
            int fh = mc.getWindow().getFramebufferHeight();
            if (fw > 0 && fh > 0) {
                int ds = 3;
                int newW = Math.max(1, fw / ds);
                int newH = Math.max(1, fh / ds);
                int savedDrawFbo = GL30.glGetInteger(36006);
                int savedReadFbo = GL30.glGetInteger(36010);
                int readGame = RenderUtil.getGameColorFramebufferId(mc);
                boolean scissorWasOn = GL11.glIsEnabled(3089);
                int passCount = Math.max(4, Math.min(16, Math.max(1, passes) + 3));
                long nowMs = System.nanoTime() / 1000000L;
                if (!ready || lastPrepareMs != nowMs || lastPreparePassCount != passCount || lastPrepareFw != fw || lastPrepareFh != fh) {
                    try {
                        if (scissorWasOn) {
                            GL11.glDisable(3089);
                        }

                        while (GL11.glGetError() != 0) {
                        }

                        if (fboA == -1 || blurW != newW || blurH != newH) {
                            destroy();
                            blurW = newW;
                            blurH = newH;
                            createResources(savedDrawFbo);
                        }

                        if (fboA == -1) {
                            return;
                        }

                        GL30.glBindFramebuffer(36008, readGame);
                        GL30.glBindFramebuffer(36009, fboA);
                        GL30.glBlitFramebuffer(0, 0, fw, fh, 0, 0, blurW, blurH, 16384, 9729);
                        GL11.glColorMask(false, false, false, true);
                        GL11.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
                        GL11.glClear(16384);
                        GL11.glColorMask(true, true, true, true);
                        int halfW = Math.max(1, blurW / 2);
                        int halfH = Math.max(1, blurH / 2);

                        for (int i = 0; i < passCount; i++) {
                            GL30.glBindFramebuffer(36008, fboA);
                            GL30.glBindFramebuffer(36009, fboB);
                            GL30.glBlitFramebuffer(0, 0, blurW, blurH, 0, 0, halfW, halfH, 16384, 9729);
                            GL30.glBindFramebuffer(36008, fboB);
                            GL30.glBindFramebuffer(36009, fboA);
                            GL30.glBlitFramebuffer(0, 0, halfW, halfH, 0, 0, blurW, blurH, 16384, 9729);
                        }

                        if (GL11.glGetError() != 0) {
                            ready = false;
                            return;
                        }

                        ready = true;
                        lastPrepareMs = nowMs;
                        lastPreparePassCount = passCount;
                        lastPrepareFw = fw;
                        lastPrepareFh = fh;
                    } catch (Exception var20) {
                        ready = false;
                        return;
                    } finally {
                        if (scissorWasOn) {
                            GL11.glEnable(3089);
                        }

                        GL30.glBindFramebuffer(36009, savedDrawFbo);
                        GL30.glBindFramebuffer(36008, savedReadFbo);
                    }

                    return;
                }
            }
        }
    }

    public static void drawBlurBehind(DrawContext context, float x, float y, float w, float h) {
        if (ready && fboA != -1 && !FabricLoader.getInstance().isModLoaded("vulkanmod")) {
            MinecraftClient mc = MinecraftClient.getInstance();
            float scaledW = mc.getWindow().getScaledWidth();
            float scaledH = mc.getWindow().getScaledHeight();
            int fbW = mc.getWindow().getFramebufferWidth();
            int fbH = mc.getWindow().getFramebufferHeight();
            Matrix3x2f matrix = new Matrix3x2f(context.getMatrices());
            Vector2f tl = matrix.transformPosition(x, y, new Vector2f());
            Vector2f br = matrix.transformPosition(x + w, y + h, new Vector2f());
            int px1 = clamp((int)(tl.x * fbW / scaledW), 0, fbW);
            int py1 = clamp((int)(tl.y * fbH / scaledH), 0, fbH);
            int px2 = clamp((int)(br.x * fbW / scaledW), 0, fbW);
            int py2 = clamp((int)(br.y * fbH / scaledH), 0, fbH);
            if (px2 > px1 && py2 > py1) {
                int sx1 = px1 * blurW / fbW;
                int sy1 = blurH - py2 * blurH / fbH;
                int sx2 = px2 * blurW / fbW;
                int sy2 = blurH - py1 * blurH / fbH;
                int dx1 = px1;
                int dy1 = fbH - py2;
                int dx2 = px2;
                int dy2 = fbH - py1;
                int savedDrawFbo = GL30.glGetInteger(36006);
                int savedReadFbo = GL30.glGetInteger(36010);
                boolean scissorOn = GL11.glIsEnabled(3089);

                try {
                    if (scissorOn) {
                        GL11.glDisable(3089);
                    }

                    GL30.glBindFramebuffer(36008, fboA);
                    GL30.glBindFramebuffer(36009, savedDrawFbo);
                    GL30.glBlitFramebuffer(sx1, sy1, sx2, sy2, dx1, dy1, dx2, dy2, 16384, 9729);
                } finally {
                    GL30.glBindFramebuffer(36009, savedDrawFbo);
                    GL30.glBindFramebuffer(36008, savedReadFbo);
                    if (scissorOn) {
                        GL11.glEnable(3089);
                    }
                }
            }
        }
    }

    public static int getBlurTextureId() {
        return texA;
    }

    public static void endFrame() {
        ready = false;
        lastPrepareMs = -1L;
        lastPreparePassCount = -1;
        lastPrepareFw = -1;
        lastPrepareFh = -1;
    }

    public static void destroy() {
        ready = false;
        lastPrepareMs = -1L;
        lastPreparePassCount = -1;
        lastPrepareFw = -1;
        lastPrepareFh = -1;
        if (fboA != -1) {
            GL30.glDeleteFramebuffers(fboA);
            fboA = -1;
        }

        if (fboB != -1) {
            GL30.glDeleteFramebuffers(fboB);
            fboB = -1;
        }

        if (texA != -1) {
            GL11.glDeleteTextures(texA);
            texA = -1;
        }

        if (texB != -1) {
            GL11.glDeleteTextures(texB);
            texB = -1;
        }
    }

    private static void createResources(int restoreFbo) {
        texA = GL11.glGenTextures();
        initTexture(texA, blurW, blurH);
        fboA = GL30.glGenFramebuffers();
        attachTexture(fboA, texA, restoreFbo);
        texB = GL11.glGenTextures();
        initTexture(texB, blurW, blurH);
        fboB = GL30.glGenFramebuffers();
        attachTexture(fboB, texB, restoreFbo);
    }

    private static void initTexture(int tex, int w, int h) {
        GL11.glBindTexture(3553, tex);
        GL11.glTexImage2D(3553, 0, 32856, w, h, 0, 6408, 5121, (ByteBuffer)null);
        GL11.glTexParameteri(3553, 10241, 9729);
        GL11.glTexParameteri(3553, 10240, 9729);
        GL11.glTexParameteri(3553, 10242, 33071);
        GL11.glTexParameteri(3553, 10243, 33071);
        GL11.glBindTexture(3553, 0);
    }

    private static void attachTexture(int fbo, int tex, int restoreFbo) {
        GL30.glBindFramebuffer(36160, fbo);
        GL30.glFramebufferTexture2D(36160, 36064, 3553, tex, 0);
        int status = GL30.glCheckFramebufferStatus(36160);
        GL30.glBindFramebuffer(36160, restoreFbo);
        if (status != 36053) {
            throw new IllegalStateException("Blur framebuffer is incomplete: " + status);
        }
    }

    private static int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }
}
