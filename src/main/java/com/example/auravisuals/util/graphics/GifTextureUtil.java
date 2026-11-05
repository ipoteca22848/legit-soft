package com.example.auravisuals.util.graphics;

import com.example.auravisuals.AuraVisuals;
import com.example.auravisuals.util.render.RenderUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

public final class GifTextureUtil {
    static final Map<Identifier, GifTextureUtil$GifAnimation> CACHE = new ConcurrentHashMap<>();
    static final Set<Identifier> LOADING = ConcurrentHashMap.newKeySet();
    static final ExecutorService LOADER = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "bamber client-GifLoader");
        t.setDaemon(true);
        return t;
    });

    GifTextureUtil() {
    }

    public static void renderGif(DrawContext ctx, Identifier gifId, float x, float y, float w, float h, boolean play) {
        renderGif(ctx, gifId, gifId == null ? "gif:null" : gifId.toString(), x, y, w, h, play);
    }

    public static void renderGif(DrawContext ctx, Identifier gifId, String stateKey, float x, float y, float w, float h, boolean play) {
        GifTextureUtil$GifAnimation animation = CACHE.get(gifId);
        if (animation == null) {
            queuePreload(gifId);
            RenderUtil.drawRoundedRect(ctx, x, y, w, h, 4.0F, -1440406226);
        } else {
            Identifier frameId = animation.getCurrentFrame(stateKey, play);
            RenderUtil.shaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            GL11.glTexParameteri(3553, 10241, 9729);
            GL11.glTexParameteri(3553, 10240, 9729);
            RenderUtil.drawRoundedTexture(
                ctx, frameId, x, y, w, h, 4.0F, true, true, true, true, 0.0F, 0.0F, animation.width, animation.height, animation.width, animation.height
            );
            RenderUtil.shaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public static void queuePreload(Identifier gifId) {
        if (gifId != null && !CACHE.containsKey(gifId) && LOADING.add(gifId)) {
            LOADER.execute(() -> {
                try {
                    GifTextureUtil$DecodedGif decoded = decodeGifSafe(gifId);
                    if (decoded == null) {
                        return;
                    }

                    MinecraftClient mc = MinecraftClient.getInstance();
                    if (mc == null) {
                        return;
                    }

                    mc.execute(() -> {
                        try {
                            GifTextureUtil$GifAnimation animation = registerDecodedGif(gifId, decoded);
                            if (animation != null) {
                                CACHE.put(gifId, animation);
                            }
                        } catch (Exception var6) {
                            AuraVisuals.LOGGER.warn("Failed to register gif {} on render thread", gifId, var6);
                        } finally {
                            LOADING.remove(gifId);
                        }
                    });
                } catch (Exception var3) {
                    AuraVisuals.LOGGER.warn("Failed to decode gif {}", gifId, var3);
                    LOADING.remove(gifId);
                }
            });
        }
    }

    static GifTextureUtil$DecodedGif decodeGifSafe(Identifier gifId) {
        try {
            return decodeGif(gifId);
        } catch (Exception var4) {
            AuraVisuals.LOGGER.warn("Animated decode failed for {}, using static frame", gifId, var4);

            try {
                return decodeStaticFallback(gifId);
            } catch (Exception var3) {
                AuraVisuals.LOGGER.warn("Fallback decode failed for {}", gifId, var3);
                return null;
            }
        }
    }

    static GifTextureUtil$DecodedGif decodeGif(Identifier gifId) throws IOException {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            throw new IOException("MinecraftClient is not ready");
        }

        try (
            InputStream input = mc.getResourceManager().open(gifId);
            ImageInputStream imageInput = ImageIO.createImageInputStream(input);
        ) {
            if (imageInput == null) {
                throw new IOException("ImageIO could not open stream for " + gifId);
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw new IOException("No GIF reader available");
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(imageInput, false, false);
                int frameCount = safeGetFrameCount(reader);
                int canvasWidth = readLogicalSize(reader.getStreamMetadata(), "logicalScreenWidth");
                int canvasHeight = readLogicalSize(reader.getStreamMetadata(), "logicalScreenHeight");
                BufferedImage firstFrame = reader.read(0);
                if (canvasWidth <= 0) {
                    canvasWidth = firstFrame.getWidth();
                }

                if (canvasHeight <= 0) {
                    canvasHeight = firstFrame.getHeight();
                }

                if (frameCount <= 0) {
                    frameCount = 1;
                }

                int step = chooseFrameStep(frameCount, canvasWidth, canvasHeight);
                BufferedImage canvas = new BufferedImage(canvasWidth, canvasHeight, 2);
                Graphics2D graphics = canvas.createGraphics();

                try {
                    List<NativeImage> frames = new ArrayList<>();
                    List<Integer> delays = new ArrayList<>();
                    int mergedDelay = 0;
                    int i = 0;

                    while (true) {
                        BufferedImage frame;
                        try {
                            frame = i == 0 ? firstFrame : reader.read(i);
                        } catch (IndexOutOfBoundsException var39) {
                            break;
                        }

                        IIOMetadata metadata = reader.getImageMetadata(i);
                        GifTextureUtil$GifFrameMeta frameMeta = readFrameMeta(metadata);
                        BufferedImage previousCanvas = deepCopy(canvas);
                        graphics.drawImage(frame, frameMeta.left, frameMeta.top, null);
                        mergedDelay += Math.max(20, frameMeta.delayMs);
                        if (i % step == 0 || i + 1 >= frameCount) {
                            frames.add(toNativeImage(canvas));
                            delays.add(Math.max(20, mergedDelay));
                            mergedDelay = 0;
                        }

                        applyDisposal(canvas, previousCanvas, frameMeta);
                        if (frameCount > 0 && i + 1 >= frameCount) {
                            break;
                        }

                        i++;
                    }

                    if (frames.isEmpty()) {
                        frames.add(toNativeImage(firstFrame));
                        delays.add(1000);
                    }

                    AuraVisuals.LOGGER
                        .info(
                            "Decoded gif {} to {} frame(s) from {} source frame(s), size {}x{}",
                            new Object[]{gifId, frames.size(), frameCount, canvasWidth, canvasHeight}
                        );
                    return new GifTextureUtil$DecodedGif(frames, delays, canvasWidth, canvasHeight);
                } finally {
                    graphics.dispose();
                }
            } finally {
                reader.dispose();
            }
        }
    }

    static GifTextureUtil$DecodedGif decodeStaticFallback(Identifier gifId) throws IOException {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            throw new IOException("MinecraftClient is not ready");
        }

        try (InputStream input = mc.getResourceManager().open(gifId)) {
            BufferedImage image = ImageIO.read(input);
            if (image == null) {
                throw new IOException("ImageIO returned null for " + gifId);
            }

            List<NativeImage> frames = new ArrayList<>(1);
            frames.add(toNativeImage(image));
            List<Integer> delays = new ArrayList<>(1);
            delays.add(1000);
            return new GifTextureUtil$DecodedGif(frames, delays, image.getWidth(), image.getHeight());
        }
    }

    static GifTextureUtil$GifAnimation registerDecodedGif(Identifier gifId, GifTextureUtil$DecodedGif decoded) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) {
            return null;
        }

        List<Identifier> frameIds = new ArrayList<>(decoded.frames.size());

        for (int i = 0; i < decoded.frames.size(); i++) {
            Identifier frameId = Identifier.of("auravisuals", "dynamic/gif/" + gifId.getNamespace() + "/" + sanitizePath(gifId.getPath()) + "_" + i);
            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "auravisual", decoded.frames.get(i));
            mc.getTextureManager().registerTexture(frameId, texture);
            texture.upload();
            frameIds.add(frameId);
        }

        AuraVisuals.LOGGER.info("Registered gif {} with {} frame(s)", gifId, frameIds.size());
        return new GifTextureUtil$GifAnimation(frameIds, decoded.delays, decoded.width, decoded.height);
    }

    static int chooseFrameStep(int frameCount, int width, int height) {
        int maxFrames = 140;
        long area = (long)width * height;
        if (area >= 350000L) {
            maxFrames = 100;
        }

        if (area >= 500000L) {
            maxFrames = 80;
        }

        return frameCount <= maxFrames ? 1 : Math.max(1, (int)Math.ceil((double)frameCount / maxFrames));
    }

    static int safeGetFrameCount(ImageReader reader) {
        try {
            return reader.getNumImages(true);
        } catch (Exception var2) {
            return -1;
        }
    }

    static void applyDisposal(BufferedImage canvas, BufferedImage previousCanvas, GifTextureUtil$GifFrameMeta meta) {
        if ("restoreToPrevious".equals(meta.disposalMethod)) {
            Graphics2D graphics = canvas.createGraphics();

            try {
                graphics.drawImage(previousCanvas, 0, 0, null);
            } finally {
                graphics.dispose();
            }
        } else if ("restoreToBackgroundColor".equals(meta.disposalMethod)) {
            Graphics2D graphics = canvas.createGraphics();

            try {
                graphics.setBackground(new Color(0, 0, 0, 0));
                graphics.clearRect(meta.left, meta.top, meta.width, meta.height);
            } finally {
                graphics.dispose();
            }
        }
    }

    static NativeImage toNativeImage(BufferedImage image) {
        NativeImage nativeImage = new NativeImage(image.getWidth(), image.getHeight(), false);

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                nativeImage.setColorArgb(x, y, image.getRGB(x, y));
            }
        }

        return nativeImage;
    }

    static BufferedImage deepCopy(BufferedImage source) {
        BufferedImage copy = new BufferedImage(source.getWidth(), source.getHeight(), 2);
        Graphics2D graphics = copy.createGraphics();

        try {
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }

        return copy;
    }

    static int readLogicalSize(IIOMetadata metadata, String attributeName) {
        if (metadata == null) {
            return -1;
        }

        Node root = getMetadataRoot(metadata, "javax_imageio_gif_stream_1.0");
        if (root == null) {
            return -1;
        }

        Node logical = findChild(root, "LogicalScreenDescriptor");
        if (logical == null) {
            return -1;
        }

        NamedNodeMap attributes = logical.getAttributes();
        Node node = attributes == null ? null : attributes.getNamedItem(attributeName);
        if (node == null) {
            return -1;
        }

        try {
            return Integer.parseInt(node.getNodeValue());
        } catch (NumberFormatException var7) {
            return -1;
        }
    }

    static GifTextureUtil$GifFrameMeta readFrameMeta(IIOMetadata metadata) {
        GifTextureUtil$GifFrameMeta meta = new GifTextureUtil$GifFrameMeta();
        if (metadata == null) {
            return meta;
        }

        Node root = getMetadataRoot(metadata, "javax_imageio_gif_image_1.0");
        if (root == null) {
            return meta;
        }

        Node imageDescriptor = findChild(root, "ImageDescriptor");
        if (imageDescriptor != null) {
            NamedNodeMap attributes = imageDescriptor.getAttributes();
            meta.left = parseInt(attributes, "imageLeftPosition", 0);
            meta.top = parseInt(attributes, "imageTopPosition", 0);
            meta.width = Math.max(1, parseInt(attributes, "imageWidth", 1));
            meta.height = Math.max(1, parseInt(attributes, "imageHeight", 1));
        }

        Node gce = findChild(root, "GraphicControlExtension");
        if (gce != null) {
            NamedNodeMap attributes = gce.getAttributes();
            meta.delayMs = Math.max(20, parseInt(attributes, "delayTime", 1) * 10);
            Node disposal = attributes == null ? null : attributes.getNamedItem("disposalMethod");
            if (disposal != null) {
                meta.disposalMethod = disposal.getNodeValue();
            }
        }

        return meta;
    }

    static Node getMetadataRoot(IIOMetadata metadata, String fallbackFormat) {
        String formatName = metadata.getNativeMetadataFormatName();
        if (formatName == null || formatName.isBlank()) {
            formatName = fallbackFormat;
        }

        try {
            return metadata.getAsTree(formatName);
        } catch (Exception var4) {
            return null;
        }
    }

    static Node findChild(Node parent, String name) {
        if (parent == null) {
            return null;
        }

        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (name.equals(child.getNodeName())) {
                return child;
            }
        }

        return null;
    }

    static int parseInt(NamedNodeMap attributes, String name, int fallback) {
        if (attributes == null) {
            return fallback;
        }

        Node node = attributes.getNamedItem(name);
        if (node == null) {
            return fallback;
        }

        try {
            return Integer.parseInt(node.getNodeValue());
        } catch (NumberFormatException var5) {
            return fallback;
        }
    }

    static String sanitizePath(String path) {
        return path.replace('\\', '_').replace('/', '_').replace('.', '_');
    }
}
