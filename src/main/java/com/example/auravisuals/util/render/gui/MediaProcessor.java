package com.example.auravisuals.util.render.gui;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.util.Identifier;

public final class MediaProcessor {
    public static final String NS = "auravisuals";
    public static final int MAX_SLOTS = 10;
    static final int MAX_FRAMES = 240;

    MediaProcessor() {
    }

    public static boolean supported() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        return !os.contains("mac") && !os.contains("darwin");
    }

    public static String slotId(int slot) {
        return "slot" + slot;
    }

    public static Path mediaDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("auravisuals").resolve("media");
    }

    public static Path slotDir(int slot) {
        return mediaDir().resolve("slot" + slot);
    }

    public static Identifier frameId(int slot, int i) {
        return Identifier.of("auravisuals", "media/slot" + slot + "/frame_" + String.format("%03d", i));
    }

    public static BufferedImage[] readSlotFrames(int slot, int count) throws Exception {
        Path dir = slotDir(slot);
        BufferedImage[] imgs = new BufferedImage[count];

        for (int i = 0; i < count; i++) {
            imgs[i] = ImageIO.read(dir.resolve(String.format("frame_%03d.png", i)).toFile());
        }

        return imgs;
    }

    public static MediaProcessor$Prepared prepare(Path file) throws Exception {
        String lower = file.getFileName().toString().toLowerCase(Locale.ROOT);
        String base = stripExt(file.getFileName().toString());
        int[] delays;
        List<BufferedImage> raw;
        if (lower.endsWith(".gif")) {
            GifDecoder$Result r = GifDecoder.read(file);
            if (r == null || r.frames.isEmpty()) {
                throw new IllegalArgumentException("No frames in GIF");
            }

            raw = r.frames;
            delays = r.delaysMs;
        } else {
            BufferedImage img = ImageIO.read(file.toFile());
            if (img == null) {
                throw new IllegalArgumentException("Unsupported image format");
            }

            raw = List.of(img);
            delays = new int[]{1000};
        }

        ArrayList<BufferedImage> pickSrc = new ArrayList<>();
        ArrayList<Integer> pickDelays = new ArrayList<>();
        if (raw.size() > 240) {
            double step = raw.size() / 240.0;

            for (int i = 0; i < 240; i++) {
                int idx = (int)(i * step);
                pickSrc.add(raw.get(idx));
                pickDelays.add(delays[Math.min(idx, delays.length - 1)]);
            }
        } else {
            for (int i = 0; i < raw.size(); i++) {
                pickSrc.add(raw.get(i));
                pickDelays.add(delays[Math.min(i, delays.length - 1)]);
            }
        }

        int n = pickSrc.size();
        int ss = 2;

        for (int s : new int[]{4, 3, 2}) {
            long per = (long)(176 * s) * (166 * s) * 4L;
            if (per * n <= 134217728L) {
                ss = s;
                break;
            }
        }

        int tw = 176 * ss;
        int th = 166 * ss;
        ArrayList<BufferedImage> composed = new ArrayList<>(n);
        int[] durations = new int[n];

        for (int i = 0; i < n; i++) {
            composed.add(Overlay.composeImage(pickSrc.get(i), tw, th));
            durations[i] = Math.max(20, pickDelays.get(i));
        }

        return new MediaProcessor$Prepared(composed, durations, tw, th, base);
    }

    public static void persist(MediaProcessor$Prepared p, int slot) throws Exception {
        Path dir = slotDir(slot);
        clearDir(dir);
        Files.createDirectories(dir);

        for (int i = 0; i < p.frames.size(); i++) {
            try (OutputStream os = Files.newOutputStream(dir.resolve(String.format("frame_%03d.png", i)))) {
                ImageIO.write(p.frames.get(i), "png", os);
            }
        }

        Properties props = new Properties();
        props.setProperty("v", "2");
        props.setProperty("name", p.name);
        props.setProperty("count", Integer.toString(p.frames.size()));
        props.setProperty("w", Integer.toString(p.frameW));
        props.setProperty("h", Integer.toString(p.frameH));
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < p.durationsMs.length; i++) {
            if (i > 0) {
                sb.append(',');
            }

            sb.append(p.durationsMs[i]);
        }

        props.setProperty("delays", sb.toString());

        try (OutputStream os = Files.newOutputStream(dir.resolve("meta.properties"))) {
            props.store(os, "anvil visual media slot " + slot);
        }
    }

    public static GifOption buildOption(MediaProcessor$Prepared p, int slot) {
        int n = p.frames.size();
        Identifier[] ids = new Identifier[n];

        for (int i = 0; i < n; i++) {
            ids[i] = frameId(slot, i);
        }

        boolean single = n == 1;
        GifOption opt = new GifOption(slotId(slot), p.name, ids, single ? null : p.durationsMs, p.frameW, p.frameH, false);
        opt.setActivator(() -> LiveUploader.requestFromDisk(opt, slot));
        return opt;
    }

    public static GifOption optionFromDisk(int slot) {
        if (!supported()) {
            return null;
        }

        try {
            Path dir = slotDir(slot);
            Path meta = dir.resolve("meta.properties");
            if (!Files.exists(meta)) {
                return null;
            }

            Properties p = new Properties();

            try (InputStream in = Files.newInputStream(meta)) {
                p.load(in);
            }

            if (!"2".equals(p.getProperty("v"))) {
                clearDir(dir);
                Files.deleteIfExists(dir);
                return null;
            }

            String name = p.getProperty("name", "Custom");
            int count = Integer.parseInt(p.getProperty("count", "0"));
            if (count <= 0) {
                return null;
            }

            int fw = Integer.parseInt(p.getProperty("w", "352"));
            int fh = Integer.parseInt(p.getProperty("h", "332"));
            int[] delays = null;
            String d = p.getProperty("delays", "");
            if (!d.isEmpty()) {
                String[] parts = d.split(",");
                delays = new int[parts.length];

                for (int i = 0; i < parts.length; i++) {
                    delays[i] = Integer.parseInt(parts[i].trim());
                }
            }

            Identifier[] ids = new Identifier[count];

            for (int i = 0; i < count; i++) {
                ids[i] = frameId(slot, i);
            }

            boolean single = count == 1;
            GifOption opt = new GifOption(slotId(slot), name, ids, single ? null : delays, fw, fh, false);
            opt.setActivator(() -> LiveUploader.requestFromDisk(opt, slot));
            return opt;
        } catch (Exception var15) {
            return null;
        }
    }

    public static void deactivate(int slot) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getTextureManager() != null) {
                mc.execute(() -> destroySlotTextures(mc.getTextureManager(), slot));
            }
        } catch (Throwable var2) {
        }
    }

    public static void deleteSlot(int slot) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.getTextureManager() != null) {
                mc.execute(() -> destroySlotTextures(mc.getTextureManager(), slot));
            }
        } catch (Throwable var3) {
        }

        clearDir(slotDir(slot));

        try {
            Files.deleteIfExists(slotDir(slot));
        } catch (Exception var2) {
        }
    }

    public static void destroySlotTextures(TextureManager tm, int slot) {
        for (int i = 0; i < 240; i++) {
            try {
                tm.destroyTexture(frameId(slot, i));
            } catch (Throwable var4) {
            }
        }
    }

    public static void registerTexture(TextureManager tm, Identifier id, BufferedImage img, int slot, int idx) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        NativeImage ni = new NativeImage(w, h, false);

        for (int y = 0; y < h; y++) {
            int row = y * w;

            for (int x = 0; x < w; x++) {
                ni.setColorArgb(x, y, px[row + x]);
            }
        }

        NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "anvil visual-s" + slot + "-" + idx, ni);
        tex.upload();
        tm.registerTexture(id, tex);
    }

    static void clearDir(Path dir) {
        try {
            if (Files.exists(dir)) {
                try (Stream<Path> s = Files.list(dir)) {
                    s.forEach(f -> {
                        try {
                            Files.deleteIfExists(f);
                        } catch (Exception var2) {
                        }
                    });
                }
            }
        } catch (Exception var6) {
        }
    }

    static String stripExt(String s) {
        int dot = s.lastIndexOf(46);
        return dot > 0 ? s.substring(0, dot) : s;
    }
}
