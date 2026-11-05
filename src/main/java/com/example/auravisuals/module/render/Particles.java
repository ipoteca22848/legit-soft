package com.example.auravisuals.module.render;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.ActionSetting;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.MultiBooleanSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.theme.ThemeManager;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.imageio.ImageIO;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public class Particles extends Module {
    static final int MAX_PARTICLES = 240;
    static final double MAX_RENDER_DISTANCE_SQ = 4096.0;
    public static final List<String> BUILT_IN_MODES = List.of(
        "Звёздочки", "Сердечко", "Снежинки", "Доллар", "Корона", "Череп", "Крест", "Молния", "Куб", "Блум", "Рандом"
    );
    public static final MultiBooleanSetting mode = new MultiBooleanSetting(
        "Режим", "Звёздочки", "Сердечко", "Снежинки", "Доллар", "Корона", "Череп", "Крест", "Молния", "Куб", "Блум", "Рандом"
    );
    public static final BooleanSetting rainbow = new BooleanSetting("Разноцветные", true);
    public static final BooleanSetting customColor = new BooleanSetting("Свой цвет", false, () -> !rainbow.getValue());
    public static final ColorSetting clientColor = new ColorSetting("Цвет", new Color(255, 255, 255), () -> !rainbow.getValue() && customColor.getValue());
    public static final MultiBooleanSetting triggers = new MultiBooleanSetting("Когда применять", "Удар", "Бег", "Прыжок", "Перка", "В мире", "Тотем");
    public static final ModeSetting flightLogic = new ModeSetting("Логика полёта", "Свободный полёт", "Свободный полёт", "Взлёт", "Падение");
    public static final NumberSetting speed = new NumberSetting("Скорость", 1.0, 0.1, 5.0, 0.1);
    public static final NumberSetting lifeTime = new NumberSetting("Время жизни", 20.0, 1.0, 100.0, 1.0);
    public static final NumberSetting size = new NumberSetting("Размер", 1.0, 0.1, 3.0, 0.1);
    public static final NumberSetting attackAmount = new NumberSetting("Кол-во при атаке", 5.0, 1.0, 50.0, 1.0, () -> triggers.isEnabled("Удар"));
    public static final NumberSetting jumpAmount = new NumberSetting("Кол-во при прыжке", 5.0, 1.0, 50.0, 1.0, () -> triggers.isEnabled("Прыжок"));
    public static final NumberSetting worldAmount = new NumberSetting("Кол-во в мире", 1.0, 0.0, 10.0, 1.0, () -> triggers.isEnabled("В мире"));
    public static final NumberSetting sprintAmount = new NumberSetting("Кол-во при беге", 2.0, 1.0, 20.0, 1.0, () -> triggers.isEnabled("Бег"));
    public static final NumberSetting totemAmount = new NumberSetting("Кол-во при тотеме", 15.0, 1.0, 50.0, 1.0, () -> triggers.isEnabled("Тотем"));
    public static final BooleanSetting glow = new BooleanSetting("Свечение", true);
    public static final NumberSetting glowIntensity = new NumberSetting("Сила свечения", 0.35, 0.05, 1.0, 0.05, () -> glow.getValue());
    public static final ActionSetting addCustom = new ActionSetting("Добавить свой PNG...", Particles::pickCustomPng);
    public static final ActionSetting openFolder = new ActionSetting("Открыть папку PNG", Particles::openCustomFolder);
    final List<Particles$CustomParticle> particles = new ArrayList<>();
    final List<Particles$CustomParticle> renderParticlesScratch = new ArrayList<>(240);
    final Map<Identifier, List<Particles$CustomParticle>> renderBucketsScratch = new LinkedHashMap<>();
    final Random random = new Random();
    final Queue<Particles$TimedPos> posHistory = new ArrayDeque<>();
    Entity currentTotemTarget = null;
    long totemStartTime = 0L;
    boolean spawningTotemParticles = false;
    static final long totemSpawnDuration = 1000L;
    long lastWorldSpawnMs = 0L;
    long lastPearlScanMs = 0L;
    long lastSprintSpawnMs = 0L;
    static final Identifier STAR = Identifier.of("auravisuals", "textures/particles/star.png");
    static final Identifier HEART = Identifier.of("auravisuals", "textures/particles/heart.png");
    static final Identifier SNOWFLAKE = Identifier.of("auravisuals", "textures/particles/snowflake.png");
    static final Identifier DOLLAR = Identifier.of("auravisuals", "textures/particles/dollar.png");
    static final Identifier CROWN = Identifier.of("auravisuals", "textures/particles/crown.png");
    static final Identifier SKULL = Identifier.of("auravisuals", "textures/particles/ded.png");
    static final Identifier CREST = Identifier.of("auravisuals", "textures/particles/crest.png");
    static final Identifier LIGHTNING = Identifier.of("auravisuals", "textures/particles/light.png");
    static final Identifier CUBE = Identifier.of("auravisuals", "textures/particles/cube.png");
    static final Identifier BLOOM = Identifier.of("auravisuals", "textures/bloom.png");
    static final Identifier GLOW_TEXTURE = Identifier.of("auravisuals", "textures/glow.png");
    static final Identifier[] PARTICLE_TEXTURES = new Identifier[]{STAR, HEART, SNOWFLAKE, DOLLAR, CROWN, SKULL, CREST, LIGHTNING, CUBE};
    static final float SPRINT_AMOUNT_FACTOR = 0.5F;
    static final Map<String, Identifier> CUSTOM_PNG_MAP = new ConcurrentHashMap<>();

    public Particles() {
        super("Particles", "Кастомные партиклы и свои PNG картинки", Category.VISUALS);
        this.addSettings(
            mode,
            flightLogic,
            rainbow,
            customColor,
            clientColor,
            triggers,
            speed,
            lifeTime,
            size,
            attackAmount,
            jumpAmount,
            worldAmount,
            sprintAmount,
            totemAmount,
            glow,
            glowIntensity,
            addCustom,
            openFolder
        );
        reloadCustomPngs();
    }

    static File getCustomParticlesFolder() {
        MinecraftClient client = MinecraftClient.getInstance();
        File runDirectory = client != null && client.runDirectory != null ? client.runDirectory : new File(".");
        File folder = new File(runDirectory, "bamber/custom_particles");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }

    public static synchronized void reloadCustomPngs() {
        File[] files = getCustomParticlesFolder().listFiles();
        ArrayList<String> modes = new ArrayList<>(BUILT_IN_MODES);
        CUSTOM_PNG_MAP.clear();
        if (files != null) {
            Arrays.sort(files, Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));

            for (File file : files) {
                String lowerName = file.getName().toLowerCase(Locale.ROOT);
                if (file.isFile()
                    && !file.getName().startsWith(".")
                    && (lowerName.endsWith(".png") || lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg") || lowerName.endsWith(".webp"))) {
                    String displayName = file.getName().replaceFirst("[.][^.]+$", "");
                    String safeName = lowerName.replaceAll("[^a-z0-9]", "_");
                    Identifier identifier = Identifier.of("auravisuals", "custom_particle_" + safeName);

                    try {
                        BufferedImage image = ImageIO.read(file);
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (image != null && client != null && client.getTextureManager() != null) {
                            int width = image.getWidth();
                            int height = image.getHeight();
                            int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
                            NativeImage nativeImage = new NativeImage(width, height, false);

                            for (int y = 0; y < height; y++) {
                                for (int x = 0; x < width; x++) {
                                    nativeImage.setColorArgb(x, y, pixels[y * width + x]);
                                }
                            }

                            NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "custom_particle_" + safeName, nativeImage);
                            texture.upload();
                            client.getTextureManager().registerTexture(identifier, texture);
                            CUSTOM_PNG_MAP.put(displayName, identifier);
                            if (!modes.contains(displayName)) {
                                modes.add(displayName);
                            }
                        }
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                }
            }
        }

        mode.setValues(modes);
    }

    public static void pickCustomPng() {
        new Thread(
                () -> {
                    try {
                        MemoryStack stack = MemoryStack.stackPush();

                        String selectedPath;
                        try {
                            PointerBuffer filters = stack.mallocPointer(5);
                            filters.put(stack.UTF8("*.png"));
                            filters.put(stack.UTF8("*.jpg"));
                            filters.put(stack.UTF8("*.jpeg"));
                            filters.put(stack.UTF8("*.webp"));
                            filters.put(stack.UTF8("*.*"));
                            filters.flip();
                            selectedPath = TinyFileDialogs.tinyfd_openFileDialog(
                                "Выберите картинку для партикла", "", filters, "PNG и изображения (*.png, *.jpg)", false
                            );
                        } catch (Throwable var5) {
                            if (stack != null) {
                                try {
                                    stack.close();
                                } catch (Throwable var4) {
                                    var5.addSuppressed(var4);
                                }
                            }

                            throw var5;
                        }

                        if (stack != null) {
                            stack.close();
                        }

                        if (selectedPath == null || selectedPath.isEmpty()) {
                            return;
                        }

                        File source = new File(selectedPath);
                        if (!source.isFile()) {
                            return;
                        }

                        Files.copy(source.toPath(), new File(getCustomParticlesFolder(), source.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                        MinecraftClient client = MinecraftClient.getInstance();
                        if (client != null) {
                            String cleanName = source.getName().replaceFirst("[.][^.]+$", "");
                            client.execute(() -> {
                                reloadCustomPngs();
                                mode.set(cleanName, true);
                                if (client.player != null) {
                                    client.player
                                        .sendMessage(Text.literal("§a[bamber client] §fКастомный партикл §e" + cleanName + " §fуспешно добавлен!"), false);
                                }
                            });
                        }
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                },
                "bamber client-Particle-Picker"
            )
            .start();
    }

    public static void openCustomFolder() {
        try {
            Util.getOperatingSystem().open(getCustomParticlesFolder());
        } catch (Throwable var1) {
        }
    }

    @Override
    public void onEnable() {
        reloadCustomPngs();
    }

    public void spawnTotemParticles(Entity target) {
        if (triggers.isEnabled("Тотем") && target != null) {
            this.currentTotemTarget = target;
            this.totemStartTime = System.currentTimeMillis();
            this.spawningTotemParticles = true;
        }
    }

    public void onEvent(String event, Vec3d pos) {
        if (triggers.isEnabled(event)) {
            int amount = (int)attackAmount.getValue().doubleValue();
            String logic = flightLogic.getValue();
            if (event.equals("Прыжок")) {
                amount = (int)jumpAmount.getValue().doubleValue();

                for (int i = 0; i < amount; i++) {
                    Vec3d motion = new Vec3d(
                        (this.random.nextDouble() - 0.5) * speed.getValue() * 0.5,
                        this.random.nextDouble() * 0.5 * speed.getValue() * 0.5,
                        (this.random.nextDouble() - 0.5) * speed.getValue() * 0.5
                    );
                    Particles$ParticleChoice choice = this.pickParticle();
                    this.addParticle(
                        new Particles$CustomParticle(
                            this, pos, motion, (float)lifeTime.getValue().doubleValue(), this.getClientColor(choice.isCustom), choice.texture, logic
                        )
                    );
                }
            } else {
                float hitSpread = 0.72F;

                for (int i = 0; i < amount; i++) {
                    Vec3d motion = new Vec3d(
                        (this.random.nextDouble() - 0.5) * speed.getValue() * hitSpread,
                        (this.random.nextDouble() - 0.5) * speed.getValue() * hitSpread,
                        (this.random.nextDouble() - 0.5) * speed.getValue() * hitSpread
                    );
                    Particles$ParticleChoice choice = this.pickParticle();
                    this.addParticle(
                        new Particles$CustomParticle(
                            this, pos, motion, (float)lifeTime.getValue().doubleValue(), this.getClientColor(choice.isCustom), choice.texture, logic
                        )
                    );
                }
            }
        }
    }

    @Override
    public void onTick() {
        if (this.mc.player != null && this.mc.world != null) {
            long nowMs = System.currentTimeMillis();
            Iterator<Particles$CustomParticle> it = this.particles.iterator();

            while (it.hasNext()) {
                Particles$CustomParticle p = it.next();
                p.update();
                if (p.isDead()) {
                    it.remove();
                }
            }

            if (triggers.isEnabled("В мире")) {
                int amount = nowMs - this.lastWorldSpawnMs >= 100L ? (int)worldAmount.getValue().doubleValue() : 0;
                if (amount > 0) {
                    this.lastWorldSpawnMs = nowMs;
                }

                for (int i = 0; i < amount; i++) {
                    double x = this.mc.player.getX() + (this.random.nextDouble() - 0.5) * 32.0;
                    double y = this.mc.player.getY() + this.random.nextDouble() * 21.0 - 5.0;
                    double z = this.mc.player.getZ() + (this.random.nextDouble() - 0.5) * 32.0;
                    Particles$ParticleChoice choice = this.pickParticle();
                    this.addParticle(
                        new Particles$CustomParticle(
                            this,
                            new Vec3d(x, y, z),
                            new Vec3d(
                                (this.random.nextDouble() - 0.5) * 0.1 * speed.getValue(),
                                (this.random.nextDouble() - 0.5) * 0.1 * speed.getValue(),
                                (this.random.nextDouble() - 0.5) * 0.1 * speed.getValue()
                            ),
                            (float)lifeTime.getValue().doubleValue(),
                            this.getClientColor(choice.isCustom),
                            choice.texture,
                            flightLogic.getValue()
                        )
                    );
                }
            }

            if (triggers.isEnabled("Перка")) {
                for (Entity entity : this.shouldScanPearls(nowMs) ? this.mc.world.getEntities() : Collections.<Entity>emptyList()) {
                    if (entity instanceof EnderPearlEntity) {
                        for (int i2 = 0; i2 < 3; i2++) {
                            Vec3d pPos = entity.getEntityPos();
                            Particles$ParticleChoice choice2 = this.pickParticle();
                            this.addParticle(
                                new Particles$CustomParticle(
                                    this,
                                    new Vec3d(
                                        pPos.x + (this.random.nextDouble() - 0.5),
                                        pPos.y + this.random.nextDouble() * entity.getHeight(),
                                        pPos.z + (this.random.nextDouble() - 0.5)
                                    ),
                                    new Vec3d(
                                        (this.random.nextDouble() - 0.5) * 0.1, (this.random.nextDouble() - 0.5) * 0.1, (this.random.nextDouble() - 0.5) * 0.1
                                    ),
                                    (float)lifeTime.getValue().doubleValue(),
                                    this.getClientColor(choice2.isCustom),
                                    choice2.texture,
                                    flightLogic.getValue()
                                )
                            );
                        }
                    }
                }
            }

            if (triggers.isEnabled("Бег")) {
                this.posHistory.add(new Particles$TimedPos(nowMs, this.mc.player.getEntityPos()));

                while (!this.posHistory.isEmpty() && nowMs - this.posHistory.peek().time > 1000L) {
                    this.posHistory.poll();
                }

                if (this.mc.player.forwardSpeed != 0.0F || this.mc.player.sidewaysSpeed != 0.0F) {
                    Vec3d spawnPos = this.mc.player.getEntityPos();
                    long targetTime = nowMs - 250L;

                    for (Particles$TimedPos tp : this.posHistory) {
                        if (tp.time >= targetTime) {
                            spawnPos = tp.pos;
                            break;
                        }
                    }

                    int amount = nowMs - this.lastSprintSpawnMs >= 50L ? Math.max(1, Math.round(sprintAmount.getValue().floatValue() * 0.5F)) : 0;
                    if (amount > 0) {
                        this.lastSprintSpawnMs = nowMs;
                    }

                    for (int i3 = 0; i3 < amount; i3++) {
                        Particles$ParticleChoice choice3 = this.pickParticle();
                        this.addParticle(
                            new Particles$CustomParticle(
                                this,
                                new Vec3d(
                                    spawnPos.x + (this.random.nextDouble() - 0.5),
                                    spawnPos.y + this.random.nextDouble() * this.mc.player.getHeight(),
                                    spawnPos.z + (this.random.nextDouble() - 0.5)
                                ),
                                new Vec3d(
                                    (this.random.nextDouble() - 0.5) * 0.1, (this.random.nextDouble() - 0.5) * 0.1, (this.random.nextDouble() - 0.5) * 0.1
                                ),
                                (float)lifeTime.getValue().doubleValue(),
                                this.getClientColor(choice3.isCustom),
                                choice3.texture,
                                flightLogic.getValue()
                            )
                        );
                    }
                }
            }

            if (this.spawningTotemParticles && triggers.isEnabled("Тотем")) {
                if (nowMs - this.totemStartTime <= 1000L && this.currentTotemTarget != null) {
                    int amount = (int)totemAmount.getValue().doubleValue();

                    for (int i = 0; i < amount; i++) {
                        double x = this.currentTotemTarget.getX() + (this.random.nextDouble() - 0.5) * 1.5;
                        double y = this.currentTotemTarget.getY() + this.random.nextDouble() * 2.0;
                        double z = this.currentTotemTarget.getZ() + (this.random.nextDouble() - 0.5) * 1.5;
                        Particles$ParticleChoice choice = this.pickParticle();
                        this.addParticle(
                            new Particles$CustomParticle(
                                this,
                                new Vec3d(x, y, z),
                                new Vec3d(
                                    (this.random.nextDouble() - 0.5) * 0.4 * speed.getValue(),
                                    (this.random.nextDouble() - 0.5) * 0.4 * speed.getValue(),
                                    (this.random.nextDouble() - 0.5) * 0.4 * speed.getValue()
                                ),
                                (float)lifeTime.getValue().doubleValue(),
                                this.getClientColor(choice.isCustom),
                                choice.texture,
                                flightLogic.getValue()
                            )
                        );
                    }
                } else {
                    this.spawningTotemParticles = false;
                    this.currentTotemTarget = null;
                }
            }
        }
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (!this.particles.isEmpty()) {
            List<Particles$CustomParticle> renderParticles = this.renderParticlesScratch;
            renderParticles.clear();
            renderParticles.addAll(this.particles);
            if (!renderParticles.isEmpty()) {
                MatrixStack matrices = context.matrixStack();
                Vec3d cameraPos = context.camera().getCameraPos();
                renderParticles.sort((a, b) -> Double.compare(b.pos.squaredDistanceTo(cameraPos), a.pos.squaredDistanceTo(cameraPos)));
                float tickDelta = this.mc.getRenderTickCounter().getTickProgress(true);

                for (Entry<Identifier, List<Particles$CustomParticle>> entry : this.collectRenderBuckets(renderParticles).entrySet()) {
                    Tessellator tessellator = Tessellator.getInstance();
                    BufferBuilder buffer = tessellator.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

                    for (Particles$CustomParticle p : entry.getValue()) {
                        this.renderParticle(p, matrices, buffer, cameraPos, tickDelta);
                    }

                    AuraRender.drawTextured(buffer, AuraRender.POS_TEX_COLOR_QUADS, entry.getKey());
                }

                if (glow.getValue()) {
                    this.renderGlowPass(renderParticles, matrices, cameraPos, tickDelta);
                }
            }
        }
    }

    Map<Identifier, List<Particles$CustomParticle>> collectRenderBuckets(List<Particles$CustomParticle> renderParticles) {
        for (List<Particles$CustomParticle> bucket : this.renderBucketsScratch.values()) {
            bucket.clear();
        }

        for (Particles$CustomParticle particle : renderParticles) {
            if (particle.texture != null) {
                this.renderBucketsScratch.computeIfAbsent(particle.texture, ignored -> new ArrayList<>()).add(particle);
            }
        }

        this.renderBucketsScratch.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        return this.renderBucketsScratch;
    }

    void renderGlowPass(List<Particles$CustomParticle> renderParticles, MatrixStack matrices, Vec3d cameraPos, float tickDelta) {
        Tessellator tess = Tessellator.getInstance();
        BufferBuilder buf = tess.begin(DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        float glowScale = (float)(size.getValue() * 0.5);
        boolean useOcclusion = renderParticles.size() <= 32 && Math.abs(cameraPos.x) < 1000000.0 && Math.abs(cameraPos.z) < 1000000.0;
        int glowStride = Optimization.customGlowStride();

        for (int i = 0; i < renderParticles.size(); i += glowStride) {
            Particles$CustomParticle p = renderParticles.get(i);
            double x = MathHelper.lerp(tickDelta, p.prevPos.x, p.pos.x);
            double dx = x - cameraPos.x;
            double z;
            double dz;
            double y;
            double dy;
            if (!(
                    dx * dx
                            + (dy = (y = MathHelper.lerp(tickDelta, p.prevPos.y, p.pos.y)) - cameraPos.y) * dy
                            + (dz = (z = MathHelper.lerp(tickDelta, p.prevPos.z, p.pos.z)) - cameraPos.z) * dz
                        > Optimization.customEffectDistanceSq()
                )
                && (!useOcclusion || !this.isOccluded(cameraPos, new Vec3d(x, y, z)))) {
                float visLife = p.getVisualLife(tickDelta);
                float lifeRatio = p.maxLife > 1.0E-4F ? MathHelper.clamp(visLife / p.maxLife, 0.0F, 1.0F) : 0.0F;
                float fade = lifeRatio * lifeRatio * (3.0F - 2.0F * lifeRatio);
                float alpha = MathHelper.clamp(fade * glowIntensity.getValue().floatValue(), 0.0F, 1.0F);
                if (!(alpha <= 0.04F)) {
                    matrices.push();

                    try {
                        matrices.translate(dx, dy, dz);
                        matrices.multiply(this.mc.gameRenderer.getCamera().getRotation());
                        float s = p.scale * glowScale;
                        matrices.scale(s, s, s);
                        Matrix4f matrix = matrices.peek().getPositionMatrix();
                        int c = p.color;
                        int r = c >> 16 & 0xFF;
                        int g = c >> 8 & 0xFF;
                        int b = c & 0xFF;
                        int a = Math.max(0, (int)(255.0F * alpha));
                        buf.vertex(matrix, -1.0F, 1.0F, 0.0F).texture(0.0F, 0.0F).color(r, g, b, a);
                        buf.vertex(matrix, -1.0F, -1.0F, 0.0F).texture(0.0F, 1.0F).color(r, g, b, a);
                        buf.vertex(matrix, 1.0F, -1.0F, 0.0F).texture(1.0F, 1.0F).color(r, g, b, a);
                        buf.vertex(matrix, 1.0F, 1.0F, 0.0F).texture(1.0F, 0.0F).color(r, g, b, a);
                    } finally {
                        matrices.pop();
                    }
                }
            }
        }

        AuraRender.drawTextured(buf, AuraRender.POS_TEX_COLOR_QUADS_ADD, GLOW_TEXTURE);
    }

    boolean isOccluded(Vec3d from, Vec3d to) {
        if (this.mc.world == null) {
            return false;
        }

        BlockHitResult hit = this.mc.world.raycast(new RaycastContext(from, to, ShapeType.COLLIDER, FluidHandling.NONE, this.mc.player));
        return hit != null && hit.getType() == Type.BLOCK && hit.getPos().squaredDistanceTo(from) + 0.04 < to.squaredDistanceTo(from);
    }

    boolean renderParticle(Particles$CustomParticle p, MatrixStack matrices, BufferBuilder buffer, Vec3d cameraPos, float tickDelta) {
        double x = MathHelper.lerp(tickDelta, p.prevPos.x, p.pos.x);
        double dx = x - cameraPos.x;
        double dz;
        double dy;
        if (dx * dx
                + (dy = MathHelper.lerp(tickDelta, p.prevPos.y, p.pos.y) - cameraPos.y) * dy
                + (dz = MathHelper.lerp(tickDelta, p.prevPos.z, p.pos.z) - cameraPos.z) * dz
            > Optimization.customEffectDistanceSq()) {
            return false;
        }

        matrices.push();

        try {
            matrices.translate(dx, dy, dz);
            matrices.multiply(this.mc.gameRenderer.getCamera().getRotation());
            float rot = MathHelper.lerp(tickDelta, p.prevRotation, p.rotation);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rot));
            float s = (float)(p.scale * size.getValue() * 0.2);
            matrices.scale(s, s, s);
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            float visLife = p.getVisualLife(tickDelta);
            float lifeRatio = p.maxLife > 1.0E-4F ? MathHelper.clamp(visLife / p.maxLife, 0.0F, 1.0F) : 0.0F;
            float fade = lifeRatio * lifeRatio * (3.0F - 2.0F * lifeRatio);
            int alpha = Math.min(255, Math.max(0, (int)(255.0F * fade)));
            int c = p.color;
            int r = c >> 16 & 0xFF;
            int g = c >> 8 & 0xFF;
            int b = c & 0xFF;
            if (alpha <= 0) {
                return false;
            }

            buffer.vertex(matrix, -1.0F, 1.0F, 0.0F).texture(0.0F, 0.0F).color(r, g, b, alpha);
            buffer.vertex(matrix, -1.0F, -1.0F, 0.0F).texture(0.0F, 1.0F).color(r, g, b, alpha);
            buffer.vertex(matrix, 1.0F, -1.0F, 0.0F).texture(1.0F, 1.0F).color(r, g, b, alpha);
            buffer.vertex(matrix, 1.0F, 1.0F, 0.0F).texture(1.0F, 0.0F).color(r, g, b, alpha);
            return true;
        } finally {
            matrices.pop();
        }
    }

    Particles$ParticleChoice pickParticle() {
        String chosen = null;
        int activeCount = 0;

        for (String m : mode.getValues()) {
            if (mode.isEnabled(m)) {
                if (this.random.nextInt(++activeCount) == 0) {
                    chosen = m;
                }
            }
        }

        if (chosen == null) {
            chosen = "Звёздочки";
        }

        if (CUSTOM_PNG_MAP.containsKey(chosen)) {
            return new Particles$ParticleChoice(CUSTOM_PNG_MAP.get(chosen), true);
        }

        if ("Рандом".equals(chosen)) {
            return new Particles$ParticleChoice(PARTICLE_TEXTURES[this.random.nextInt(PARTICLE_TEXTURES.length)], false);
        }

        Identifier tex = switch (chosen) {
            case "Сердечко" -> HEART;
            case "Снежинки" -> SNOWFLAKE;
            case "Доллар" -> DOLLAR;
            case "Корона" -> CROWN;
            case "Череп" -> SKULL;
            case "Крест" -> CREST;
            case "Молния" -> LIGHTNING;
            case "Куб" -> CUBE;
            case "Блум" -> BLOOM;
            default -> STAR;
        };
        return new Particles$ParticleChoice(tex, false);
    }

    int getClientColor(boolean isCustom) {
        if (rainbow.getValue()) {
            return Color.HSBtoRGB((float)(System.currentTimeMillis() % 2000L) / 2000.0F, 0.8F, 1.0F);
        } else if (customColor.getValue()) {
            return clientColor.getValue();
        } else {
            return isCustom ? -1 : ThemeManager.INSTANCE.getVisualModulesColor();
        }
    }

    void addParticle(Particles$CustomParticle particle) {
        if (this.particles.size() < Optimization.customParticleLimit()) {
            this.particles.add(particle);
        }
    }

    boolean shouldScanPearls(long nowMs) {
        if (nowMs - this.lastPearlScanMs < 75L) {
            return false;
        }

        this.lastPearlScanMs = nowMs;
        return true;
    }

    public boolean isSolidBlock(BlockPos pos) {
        if (this.mc.world == null) {
            return false;
        }

        BlockState state = this.mc.world.getBlockState(pos);
        return !state.isAir() && !state.getCollisionShape(this.mc.world, pos).isEmpty();
    }

    static MinecraftClient access$000(Particles x0) {
        return x0.mc;
    }
}
