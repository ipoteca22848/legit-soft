package com.example.auravisuals.module.render;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.theme.ThemeManager;
import com.example.auravisuals.util.render.JumpWaveRenderer;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;

public class KillEffect extends Module {
    public static final ModeSetting style = new ModeSetting("Режим", "Лазер", "Обычный", "Лазер", "Частицы");
    public static final BooleanSetting mobs = new BooleanSetting("Мобы", false);
    public static final BooleanSetting lightning = new BooleanSetting("Молния", true, () -> style.is("Обычный"));
    public static final BooleanSetting shatter = new BooleanSetting("Разрушение", true, () -> style.is("Обычный"));
    public static final ModeSetting particleMode = new ModeSetting("Частицы", "Огонь", () -> style.is("Обычный"), "Огонь", "Сердца", "Облако", "Эндер", "Нет");
    public static final NumberSetting speed = new NumberSetting("Скорость", 1.0, 0.1, 5.0, 0.1, () -> style.is("Обычный"));
    public static final NumberSetting amount = new NumberSetting("Количество", 20.0, 5.0, 100.0, 5.0, () -> style.is("Обычный"));
    public static final BooleanSetting wave = new BooleanSetting("Волна", true, () -> style.is("Лазер"));
    public static final NumberSetting waveRadius = new NumberSetting("Радиус", 10.0, 5.0, 20.0, 0.5, () -> style.is("Лазер") && wave.getValue());
    public static final NumberSetting bodyAmount = new NumberSetting("Количество частиц тела", 200.0, 100.0, 350.0, 10.0, () -> style.is("Частицы"));
    public static final NumberSetting bodySize = new NumberSetting("Размер", 1.0, 0.5, 2.0, 0.1, () -> style.is("Частицы"));
    public static final NumberSetting bodyForce = new NumberSetting("Сила разлёта частиц тела", 2.0, 1.0, 4.0, 0.1, () -> style.is("Частицы"));
    public static final NumberSetting bodyLifetime = new NumberSetting("Время жизни", 4.0, 1.5, 8.0, 0.5, () -> style.is("Частицы"));
    public static final BooleanSetting sound = new BooleanSetting("Звук", true);
    public static final Identifier BLOOM_TEX = Identifier.of("auravisuals", "textures/bloom.png");
    static final float LASER_DURATION = 1.5F;
    static final float LASER_HEIGHT = 4.0F;
    static final float LASER_MAX_RADIUS = 1.0F;
    static final int LASER_SLICES = 40;
    static final int MAX_BODY_SHARDS = 520;
    final List<KillEffect$KillGhost> ghosts = new ArrayList<>();
    final List<KillEffect$LaserEffect> lasers = new ArrayList<>();
    final List<KillEffect$LaserWave> laserWaves = new ArrayList<>();
    final List<KillEffect$BodyShard> bodyShards = new ArrayList<>();
    final Random random = new Random();
    final Set<UUID> deadEntities = new HashSet<>();
    final Map<UUID, KillEffect$TrackedTarget> attackedTargets = new HashMap<>();
    final Map<UUID, Long> hiddenBodies = new HashMap<>();

    public KillEffect() {
        super("KillEffect", "Эффекты при убийстве сущности", Category.VISUALS);
        bodyAmount.setDisplayName("Количество");
        bodyForce.setDisplayName("Сила разлёта");
        this.addSettings(style, mobs, lightning, shatter, particleMode, speed, amount, wave, waveRadius, bodyAmount, bodySize, bodyForce, bodyLifetime, sound);
    }

    public void setTarget(LivingEntity target) {
        if (target != null && target != this.mc.player) {
            this.attackedTargets.put(target.getUuid(), new KillEffect$TrackedTarget(target, System.currentTimeMillis()));
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.deadEntities.clear();
        this.ghosts.clear();
        this.lasers.clear();
        this.laserWaves.clear();
        this.bodyShards.clear();
        this.attackedTargets.clear();
        this.hiddenBodies.clear();
    }

    @Override
    public void onDisable() {
        this.ghosts.clear();
        this.lasers.clear();
        this.laserWaves.clear();
        this.bodyShards.clear();
        this.attackedTargets.clear();
        this.deadEntities.clear();
        this.hiddenBodies.clear();
    }

    @Override
    public void onTick() {
        if (this.mc.world != null && this.mc.player != null) {
            for (Entity entity : this.mc.world.getEntities()) {
                LivingEntity living;
                if (entity instanceof LivingEntity && (living = (LivingEntity)entity) != this.mc.player) {
                    if (living.getHealth() > 0.0F && living.isAlive()) {
                        this.deadEntities.remove(living.getUuid());
                    } else if (!this.deadEntities.contains(living.getUuid())) {
                        if (living.squaredDistanceTo(this.mc.player) < 144.0) {
                            this.onKill(living);
                        }

                        this.markDead(living);
                    }
                }
            }

            long now = System.currentTimeMillis();
            this.hiddenBodies.entrySet().removeIf(entry -> entry.getValue() <= now);
            Iterator<Entry<UUID, KillEffect$TrackedTarget>> tracked = this.attackedTargets.entrySet().iterator();

            while (tracked.hasNext()) {
                KillEffect$TrackedTarget target = tracked.next().getValue();
                LivingEntity living = target.entity;
                boolean confirmedDeath = living.getHealth() <= 0.0F || !living.isAlive();
                boolean removedAfterHit = living.isRemoved() && now - target.lastHitAt >= 75L && now - target.lastHitAt <= 1800L;
                if ((confirmedDeath || removedAfterHit) && !this.deadEntities.contains(target.uuid)) {
                    this.onKillAt(target.uuid, living, target.lastPosition, target.player);
                    tracked.remove();
                } else if (now - target.lastHitAt > 2500L) {
                    tracked.remove();
                } else if (!living.isRemoved()) {
                    target.lastPosition = living.getEntityPos();
                }
            }

            this.ghosts.removeIf(KillEffect$KillGhost::update);
            this.updateBodyShards();
        }
    }

    public void onEntityDeath(Entity entity) {
        if (this.mc.player != null && entity instanceof LivingEntity living && entity != this.mc.player) {
            if (!this.deadEntities.contains(living.getUuid())) {
                if (living.squaredDistanceTo(this.mc.player) < 144.0) {
                    this.onKill(living);
                }

                this.markDead(living);
            }
        }
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (!this.lasers.isEmpty()) {
            long now = System.currentTimeMillis();
            Iterator<KillEffect$LaserEffect> lit = this.lasers.iterator();

            while (lit.hasNext()) {
                KillEffect$LaserEffect laser = lit.next();
                float progress = (float)(now - laser.startTime) / 1500.0F;
                if (progress >= 1.0F) {
                    lit.remove();
                } else {
                    try {
                        laser.render(context, progress);
                    } catch (Throwable ignored) {
                        lit.remove();
                    }
                }
            }
        }

        this.renderLaserWaves(context);
        this.renderBodyShards(context);
        if (!this.ghosts.isEmpty()) {
            Iterator<KillEffect$KillGhost> it = this.ghosts.iterator();

            while (it.hasNext()) {
                KillEffect$KillGhost ghost = it.next();

                boolean remove;
                try {
                    remove = ghost.render(context);
                } catch (Throwable ignored) {
                    remove = true;
                }

                if (remove) {
                    it.remove();
                }
            }
        }
    }

    void onKill(LivingEntity entity) {
        if (mobs.getValue() || entity instanceof PlayerEntity) {
            if (sound.getValue()) {
                this.mc.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8F, 1.15F));
                this.mc.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 0.85F));
            }

            if (style.is("Лазер")) {
                this.lasers.add(new KillEffect$LaserEffect(this, entity.getEntityPos(), System.currentTimeMillis()));
                if (wave.getValue()) {
                    this.addLaserWave(entity.getEntityPos());
                }
            } else if (style.is("Частицы")) {
                this.spawnBodyShards(entity, entity.getEntityPos());
            } else {
                if (lightning.getValue()) {
                    LightningEntity bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, this.mc.world);
                    bolt.setPosition(entity.getEntityPos());
                    bolt.setId(-this.random.nextInt(100000));
                    this.mc.world.addEntity(bolt);
                    this.mc
                        .world
                        .playSound(
                            this.mc.player,
                            entity.getX(),
                            entity.getY(),
                            entity.getZ(),
                            SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER,
                            SoundCategory.WEATHER,
                            0.9F,
                            1.1F
                        );
                }

                if (shatter.getValue()) {
                    this.spawnShatterParticles(entity);
                }

                this.spawnCustomParticles(entity);
            }
        }
    }

    void onKillAt(UUID uuid, LivingEntity entity, Vec3d position, boolean player) {
        if (!mobs.getValue() && !player) {
            this.deadEntities.add(uuid);
        } else {
            this.deadEntities.add(uuid);
            if (sound.getValue()) {
                this.mc.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, 0.8F, 1.15F));
                this.mc.getSoundManager().play(PositionedSoundInstance.ui(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8F, 0.85F));
            }

            if (style.is("Лазер")) {
                this.lasers.add(new KillEffect$LaserEffect(this, position, System.currentTimeMillis()));
                if (wave.getValue()) {
                    this.addLaserWave(position);
                }
            } else if (style.is("Частицы")) {
                this.spawnBodyShards(entity, position);
            } else {
                if (lightning.getValue()) {
                    LightningEntity bolt = new LightningEntity(EntityType.LIGHTNING_BOLT, this.mc.world);
                    bolt.setPosition(position);
                    bolt.setId(-this.random.nextInt(100000));
                    this.mc.world.addEntity(bolt);
                    this.mc
                        .world
                        .playSound(
                            this.mc.player, position.x, position.y, position.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.WEATHER, 0.9F, 1.1F
                        );
                }

                if (shatter.getValue()) {
                    this.spawnShatterParticles(entity);
                }

                this.spawnCustomParticles(entity);
            }
        }
    }

    void markDead(LivingEntity living) {
        UUID uuid = living.getUuid();
        this.deadEntities.add(uuid);
        this.attackedTargets.remove(uuid);
    }

    void spawnBodyShards(LivingEntity entity, Vec3d feetPosition) {
        this.hiddenBodies.put(entity.getUuid(), System.currentTimeMillis() + 1350L);
        float width = Math.max(0.45F, entity.getWidth());
        float height = Math.max(0.65F, entity.getHeight());
        int shardCount = bodyAmount.getIntValue();
        int overflow = this.bodyShards.size() + shardCount - 520;
        if (overflow > 0) {
            this.bodyShards.subList(0, Math.min(overflow, this.bodyShards.size())).clear();
        }

        int[] palette = this.bodyPalette(entity);
        double centerY = feetPosition.y + height * 0.52;
        float force = bodyForce.getFloatValue();
        float sizeMultiplier = bodySize.getFloatValue();
        int baseLifetime = Math.max(20, Math.round(bodyLifetime.getFloatValue() * 20.0F));

        for (int i = 0; i < shardCount; i++) {
            float vertical = this.random.nextFloat();
            float silhouette = this.bodySilhouette(entity, vertical);
            double localX = (this.random.nextDouble() * 2.0 - 1.0) * width * 0.48 * silhouette;
            double localZ = (this.random.nextDouble() * 2.0 - 1.0) * width * 0.4 * silhouette;
            double x = feetPosition.x + localX;
            double y = feetPosition.y + 0.05 + vertical * Math.max(0.2F, height - 0.1F);
            double z = feetPosition.z + localZ;
            double radialLength = Math.max(0.08, Math.sqrt(localX * localX + localZ * localZ));
            double outward = (0.014 + this.random.nextDouble() * 0.022) * force;
            double velocityNoise = 0.016 * force;
            double vx = localX / radialLength * outward + (this.random.nextDouble() - 0.5) * velocityNoise;
            double verticalForce = Math.sqrt(force);
            double vy = (0.018 + this.random.nextDouble() * 0.052) * verticalForce + Math.max(0.0, y - centerY) * 0.004;
            double vz = localZ / radialLength * outward + (this.random.nextDouble() - 0.5) * velocityNoise;
            float shardSize = 0.043F * sizeMultiplier * (0.92F + this.random.nextFloat() * 0.16F);
            int baseColor = palette[this.random.nextInt(palette.length)];
            int color = varyColor(baseColor, 0.82F + this.random.nextFloat() * 0.3F);
            int life = Math.max(20, Math.round(baseLifetime * (0.92F + this.random.nextFloat() * 0.16F)));
            this.bodyShards
                .add(
                    new KillEffect$BodyShard(
                        x,
                        y,
                        z,
                        vx,
                        vy,
                        vz,
                        shardSize,
                        color,
                        life,
                        this.random.nextFloat() * 360.0F,
                        this.random.nextFloat() * 360.0F,
                        this.random.nextFloat() * 360.0F,
                        (this.random.nextFloat() - 0.5F) * 16.0F,
                        (this.random.nextFloat() - 0.5F) * 16.0F,
                        (this.random.nextFloat() - 0.5F) * 16.0F
                    )
                );
        }
    }

    public static boolean shouldHideBody(LivingEntity entity) {
        if (entity != null && ModuleManager.INSTANCE != null) {
            KillEffect effect = ModuleManager.INSTANCE.getModule(KillEffect.class);
            if (effect != null && effect.isEnabled() && style.is("Частицы")) {
                Long until = effect.hiddenBodies.get(entity.getUuid());
                return until != null && until > System.currentTimeMillis();
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    float bodySilhouette(LivingEntity entity, float vertical) {
        String id = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
        boolean humanoid = entity instanceof PlayerEntity
            || id.contains("zombie")
            || id.contains("skeleton")
            || id.contains("villager")
            || id.contains("piglin")
            || id.contains("witch");
        if (!humanoid) {
            return vertical > 0.78F ? 0.78F : 1.0F;
        } else if (vertical < 0.42F) {
            return 0.52F;
        } else {
            return vertical > 0.78F ? 0.76F : 1.0F;
        }
    }

    int[] bodyPalette(LivingEntity entity) {
        String id = Registries.ENTITY_TYPE.getId(entity.getType()).getPath();
        if (id.contains("drowned")) {
            return new int[]{5077100, 6198661, 3244177, 3631746, 7952954};
        } else if (id.contains("zombie") || id.contains("husk")) {
            return new int[]{6061634, 7575117, 3232605, 4353400, 2505264};
        } else if (id.contains("skeleton") || id.contains("stray")) {
            return new int[]{14013119, 11184534, 7830904, 14934222, 5593952};
        } else if (id.contains("creeper")) {
            return new int[]{5020230, 6468955, 3042610, 7980650, 1522722};
        } else if (id.contains("enderman")) {
            return new int[]{1512219, 2629678, 3679805, 10172854};
        } else if (id.contains("spider")) {
            return new int[]{2892834, 4995122, 1512983, 10167592};
        } else if (id.contains("pig")) {
            return new int[]{14981786, 13203833, 15904936, 9262416};
        } else if (id.contains("cow")) {
            return new int[]{4796453, 6833978, 15130320, 2892061};
        } else if (id.contains("sheep")) {
            return new int[]{15000024, 13157562, 9210243, 4933186};
        } else if (id.contains("chicken")) {
            return new int[]{15658469, 13619144, 12135979, 14989371};
        } else if (id.contains("villager") || id.contains("witch")) {
            return new int[]{10118992, 7029560, 7755082, 12159600};
        } else if (id.contains("blaze") || id.contains("magma")) {
            return new int[]{15901995, 14248470, 16763978, 8205333};
        } else if (id.contains("slime")) {
            return new int[]{7980138, 5086800, 10739336, 3235383};
        } else if (entity instanceof PlayerEntity) {
            int seed = entity.getUuid().hashCode();
            int cloth = Color.HSBtoRGB(Math.floorMod(seed, 360) / 360.0F, 0.48F, 0.72F) & 16777215;
            return new int[]{12946024, 11103826, 14723715, cloth, varyColor(cloth, 0.68F), 3420728};
        } else {
            int fallback = ThemeManager.INSTANCE.getVisualModulesColor() & 16777215;
            return new int[]{fallback, varyColor(fallback, 0.72F), varyColor(fallback, 1.18F), 7829367};
        }
    }

    static int varyColor(int rgb, float factor) {
        int r = MathHelper.clamp(Math.round((rgb >> 16 & 0xFF) * factor), 0, 255);
        int g = MathHelper.clamp(Math.round((rgb >> 8 & 0xFF) * factor), 0, 255);
        int b = MathHelper.clamp(Math.round((rgb & 0xFF) * factor), 0, 255);
        return r << 16 | g << 8 | b;
    }

    void updateBodyShards() {
        if (!this.bodyShards.isEmpty() && this.mc.world != null) {
            Box playerBox = this.mc.player == null ? null : this.mc.player.getBoundingBox();
            Vec3d playerVelocity = this.mc.player == null ? Vec3d.ZERO : this.mc.player.getVelocity();

            for (int i = this.bodyShards.size() - 1; i >= 0; i--) {
                KillEffect$BodyShard shard = this.bodyShards.get(i);
                shard.prevX = shard.x;
                shard.prevY = shard.y;
                shard.prevZ = shard.z;
                shard.prevRotX = shard.rotX;
                shard.prevRotY = shard.rotY;
                shard.prevRotZ = shard.rotZ;
                shard.life--;
                if (shard.life <= 0) {
                    this.bodyShards.remove(i);
                } else {
                    if (playerBox != null) {
                        this.pushShardFromPlayer(shard, playerBox, playerVelocity);
                    }

                    if (!shard.grounded) {
                        shard.vy -= 0.0135;
                        shard.vx *= 0.986;
                        shard.vz *= 0.986;
                        double nextX = shard.x + shard.vx;
                        double nextY = shard.y + shard.vy;
                        double nextZ = shard.z + shard.vz;
                        double floor = this.collisionTop(nextX, nextY - shard.size, nextZ);
                        if (Double.isFinite(floor) && shard.y - shard.size >= floor - 0.12 && nextY - shard.size <= floor + 0.035) {
                            nextY = floor + shard.size;
                            shard.vy = -shard.vy * 0.24;
                            shard.vx *= 0.62;
                            shard.vz *= 0.62;
                            if (Math.abs(shard.vy) < 0.018) {
                                shard.vy = 0.0;
                                shard.grounded = true;
                            }
                        }

                        shard.x = nextX;
                        shard.y = nextY;
                        shard.z = nextZ;
                    } else {
                        shard.vx *= 0.72;
                        shard.vz *= 0.72;
                        double nextX = shard.x + shard.vx;
                        double nextZ = shard.z + shard.vz;
                        double floor = this.collisionTop(nextX, shard.y - shard.size - 0.05, nextZ);
                        shard.x = nextX;
                        shard.z = nextZ;
                        if (Double.isFinite(floor)) {
                            shard.y = floor + shard.size;
                        } else {
                            shard.grounded = false;
                        }
                    }

                    float spin = shard.grounded ? 0.28F : 1.0F;
                    shard.rotX = shard.rotX + shard.spinX * spin;
                    shard.rotY = shard.rotY + shard.spinY * spin;
                    shard.rotZ = shard.rotZ + shard.spinZ * spin;
                    if (shard.grounded) {
                        shard.spinX *= 0.82F;
                        shard.spinY *= 0.82F;
                        shard.spinZ *= 0.82F;
                    }
                }
            }
        }
    }

    void pushShardFromPlayer(KillEffect$BodyShard shard, Box playerBox, Vec3d playerVelocity) {
        double margin = shard.size + 0.1;
        if (!(shard.x < playerBox.minX - margin)
            && !(shard.x > playerBox.maxX + margin)
            && !(shard.z < playerBox.minZ - margin)
            && !(shard.z > playerBox.maxZ + margin)
            && !(shard.y + shard.size < playerBox.minY - 0.08)
            && !(shard.y - shard.size > playerBox.maxY + 0.08)) {
            double centerX = (playerBox.minX + playerBox.maxX) * 0.5;
            double dx = shard.x - centerX;
            double centerZ = (playerBox.minZ + playerBox.maxZ) * 0.5;
            double dz = shard.z - centerZ;
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length < 1.0E-4) {
                double movement = Math.sqrt(playerVelocity.x * playerVelocity.x + playerVelocity.z * playerVelocity.z);
                if (movement > 1.0E-4) {
                    dx = playerVelocity.x / movement;
                    dz = playerVelocity.z / movement;
                } else {
                    double angle = Math.toRadians(shard.rotY);
                    dx = Math.cos(angle);
                    dz = Math.sin(angle);
                }

                length = 1.0;
            }

            double movementSpeed = Math.sqrt(playerVelocity.x * playerVelocity.x + playerVelocity.z * playerVelocity.z);
            double push = 0.024 + Math.min(0.075, movementSpeed * 0.55);
            double nx = dx / length;
            double nz = dz / length;
            shard.vx = MathHelper.clamp(shard.vx * 0.58 + nx * push + playerVelocity.x * 0.28, -0.16, 0.16);
            shard.vz = MathHelper.clamp(shard.vz * 0.58 + nz * push + playerVelocity.z * 0.28, -0.16, 0.16);
            shard.spinX += (float)(-nz * 1.8);
            shard.spinZ += (float)(nx * 1.8);
        }
    }

    double collisionTop(double x, double bottomY, double z) {
        BlockPos pos = BlockPos.ofFloored(x, bottomY, z);
        BlockState state = this.mc.world.getBlockState(pos);
        if (state.isAir()) {
            return Double.NaN;
        }

        VoxelShape shape = state.getCollisionShape(this.mc.world, pos);
        return shape.isEmpty() ? Double.NaN : pos.getY() + shape.getMax(Axis.Y);
    }

    void renderBodyShards(WorldRenderContext context) {
        if (!this.bodyShards.isEmpty()) {
            float tickDelta = context.tickCounter().getTickProgress(true);
            Vec3d camera = context.camera().getCameraPos();
            MatrixStack matrices = context.matrixStack();
            BufferBuilder buffer = Tessellator.getInstance().begin(DrawMode.QUADS, VertexFormats.POSITION_COLOR);
            int rendered = 0;

            for (KillEffect$BodyShard shard : this.bodyShards) {
                double x = MathHelper.lerp(tickDelta, shard.prevX, shard.x) - camera.x;
                double z;
                double y;
                if (!(
                    x * x
                            + (y = MathHelper.lerp(tickDelta, shard.prevY, shard.y) - camera.y) * y
                            + (z = MathHelper.lerp(tickDelta, shard.prevZ, shard.z) - camera.z) * z
                        > 2304.0
                )) {
                    float fade = MathHelper.clamp(shard.life / 28.0F, 0.0F, 1.0F);
                    int alpha = MathHelper.clamp(Math.round(235.0F * smoothStep(fade)), 0, 235);
                    float rotX = MathHelper.lerp(tickDelta, shard.prevRotX, shard.rotX);
                    float rotY = MathHelper.lerp(tickDelta, shard.prevRotY, shard.rotY);
                    float rotZ = MathHelper.lerp(tickDelta, shard.prevRotZ, shard.rotZ);
                    matrices.push();
                    matrices.translate(x, y, z);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotX));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotZ));
                    appendBodyShard(buffer, matrices.peek().getPositionMatrix(), shard.size, shard.color, alpha);
                    matrices.pop();
                    rendered++;
                }
            }

            if (rendered > 0) {
                try {
                    AuraRender.drawColor(buffer, AuraRender.POS_COLOR_QUADS);
                } catch (Throwable var20) {
                }
            }
        }
    }

    static void appendBodyShard(BufferBuilder buffer, Matrix4f m, float s, int rgb, int alpha) {
        int light = varyColor(rgb, 1.16F);
        int side = varyColor(rgb, 0.88F);
        int dark = varyColor(rgb, 0.68F);
        shardQuad(buffer, m, -s, -s, s, s, -s, s, s, s, s, -s, s, s, rgb, alpha);
        shardQuad(buffer, m, s, -s, -s, -s, -s, -s, -s, s, -s, s, s, -s, dark, alpha);
        shardQuad(buffer, m, -s, s, s, s, s, s, s, s, -s, -s, s, -s, light, alpha);
        shardQuad(buffer, m, -s, -s, -s, s, -s, -s, s, -s, s, -s, -s, s, dark, alpha);
        shardQuad(buffer, m, s, -s, s, s, -s, -s, s, s, -s, s, s, s, side, alpha);
        shardQuad(buffer, m, -s, -s, -s, -s, -s, s, -s, s, s, -s, s, -s, side, alpha);
    }

    static void shardQuad(
        BufferBuilder b,
        Matrix4f m,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float x3,
        float y3,
        float z3,
        float x4,
        float y4,
        float z4,
        int rgb,
        int alpha
    ) {
        int r = rgb >> 16 & 0xFF;
        int g = rgb >> 8 & 0xFF;
        int blue = rgb & 0xFF;
        b.vertex(m, x1, y1, z1).color(r, g, blue, alpha);
        b.vertex(m, x2, y2, z2).color(r, g, blue, alpha);
        b.vertex(m, x3, y3, z3).color(r, g, blue, alpha);
        b.vertex(m, x4, y4, z4).color(r, g, blue, alpha);
    }

    void addLaserWave(Vec3d position) {
        while (this.laserWaves.size() >= 4) {
            this.laserWaves.remove(0);
        }

        int color = ThemeManager.INSTANCE.getVisualModulesColor() | 0xFF000000;
        float radius = waveRadius.getFloatValue();
        this.laserWaves.add(new KillEffect$LaserWave(position, System.currentTimeMillis(), color, radius, this.buildTerrainMesh(position, radius)));
    }

    void renderLaserWaves(WorldRenderContext context) {
        if (!this.laserWaves.isEmpty()) {
            long now = System.currentTimeMillis();
            Matrix4f modelView = context.matrixStack().peek().getPositionMatrix();
            Vec3d camera = context.camera().getCameraPos();
            Iterator<KillEffect$LaserWave> iterator = this.laserWaves.iterator();

            while (iterator.hasNext()) {
                KillEffect$LaserWave glass = iterator.next();
                float raw = MathHelper.clamp((float)(now - glass.createdAt) / 1750.0F, 0.0F, 1.0F);
                if (raw >= 1.0F) {
                    iterator.remove();
                } else {
                    float inverse = 1.0F - raw;
                    float ring = 1.0F - inverse * inverse * inverse;
                    float appear = smoothStep(raw / 0.18F);
                    float disappear = smoothStep((1.0F - raw) / 0.82F);
                    float fade = MathHelper.clamp(appear * disappear, 0.0F, 1.0F);

                    try {
                        JumpWaveRenderer.renderTerrain(modelView, camera, glass.position, glass.radius, ring, 0.5F, fade, glass.color, glass.terrainVertices);
                    } catch (Throwable ignored) {
                        iterator.remove();
                    }
                }
            }
        }
    }

    static float smoothStep(float value) {
        float t = MathHelper.clamp(value, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    float[] buildTerrainMesh(Vec3d center, float radius) {
        if (this.mc.world == null) {
            return new float[0];
        }

        ArrayList<Float> vertices = new ArrayList<>();
        BlockPos origin = BlockPos.ofFloored(center);
        int extent = MathHelper.ceil(radius);

        for (int x = -extent; x <= extent; x++) {
            for (int z = -extent; z <= extent; z++) {
                double cellX = origin.getX() + x + 0.5 - center.x;
                BlockPos top;
                double cellZ;
                if (!(cellX * cellX + (cellZ = origin.getZ() + z + 0.5 - center.z) * cellZ > (radius + 0.8F) * (radius + 0.8F))
                    && (top = this.findTerrainTop(origin, x, z)) != null) {
                    float minX = top.getX();
                    float maxX = minX + 1.0F;
                    float y = top.getY() + 1.015F;
                    float minZ = top.getZ();
                    float maxZ = minZ + 1.0F;
                    float u0 = MathHelper.clamp((minX - (float)center.x + radius) / (radius * 2.0F), 0.0F, 1.0F);
                    float u1 = MathHelper.clamp((maxX - (float)center.x + radius) / (radius * 2.0F), 0.0F, 1.0F);
                    float v0 = MathHelper.clamp((minZ - (float)center.z + radius) / (radius * 2.0F), 0.0F, 1.0F);
                    float v1 = MathHelper.clamp((maxZ - (float)center.z + radius) / (radius * 2.0F), 0.0F, 1.0F);
                    terrainVertex(vertices, maxX, y, maxZ, u1, v1);
                    terrainVertex(vertices, maxX, y, minZ, u1, v0);
                    terrainVertex(vertices, minX, y, minZ, u0, v0);
                    terrainVertex(vertices, maxX, y, maxZ, u1, v1);
                    terrainVertex(vertices, minX, y, minZ, u0, v0);
                    terrainVertex(vertices, minX, y, maxZ, u0, v1);
                }
            }
        }

        float[] mesh = new float[vertices.size()];

        for (int i = 0; i < vertices.size(); i++) {
            mesh[i] = vertices.get(i);
        }

        return mesh;
    }

    BlockPos findTerrainTop(BlockPos origin, int x, int z) {
        for (int dy = 0; dy <= 18; dy++) {
            BlockPos down = origin.add(x, -1 - dy, z);
            if (this.isTerrainSurface(down)) {
                return down;
            }

            BlockPos up;
            if (dy > 0 && this.isTerrainSurface(up = origin.add(x, -1 + dy, z))) {
                return up;
            }
        }

        return null;
    }

    boolean isTerrainSurface(BlockPos pos) {
        if (this.mc.world == null) {
            return false;
        }

        BlockState state = this.mc.world.getBlockState(pos);
        return !state.isAir() && !(state.getBlock() instanceof LeavesBlock)
            ? state.isFullCube(this.mc.world, pos) && state.isOpaqueFullCube() && !this.isTerrainSolid(pos.up())
            : false;
    }

    boolean isTerrainSolid(BlockPos pos) {
        if (this.mc.world == null) {
            return false;
        }

        BlockState state = this.mc.world.getBlockState(pos);
        return !state.isAir() && !(state.getBlock() instanceof LeavesBlock) && state.isFullCube(this.mc.world, pos) && state.isOpaqueFullCube();
    }

    static void terrainVertex(List<Float> out, float x, float y, float z, float u, float v) {
        out.add(x);
        out.add(y);
        out.add(z);
        out.add(u);
        out.add(v);
    }

    void spawnShatterParticles(LivingEntity entity) {
        Box box = entity.getBoundingBox();
        int count = Math.max(8, amount.getIntValue());

        for (int i = 0; i < count; i++) {
            double x = MathHelper.lerp(this.random.nextDouble(), box.minX, box.maxX);
            double y = MathHelper.lerp(this.random.nextDouble(), box.minY, box.maxY);
            double z = MathHelper.lerp(this.random.nextDouble(), box.minZ, box.maxZ);
            double vx = (this.random.nextDouble() - 0.5) * 0.45 * speed.getValue();
            double vy = (this.random.nextDouble() - 0.15) * 0.45 * speed.getValue();
            double vz = (this.random.nextDouble() - 0.5) * 0.45 * speed.getValue();
            this.mc.world.addParticleClient(ParticleTypes.CRIT, x, y, z, vx, vy, vz);
            this.mc.world.addParticleClient(ParticleTypes.SMOKE, x, y, z, vx * 0.3, vy * 0.15, vz * 0.3);
        }
    }

    void spawnCustomParticles(LivingEntity entity) {
        String selectedMode = particleMode.getValue();
        if (!selectedMode.equals("Нет")) {
            Box box = entity.getBoundingBox();
            int count = amount.getIntValue();

            for (int i = 0; i < count; i++) {
                double x = MathHelper.lerp(this.random.nextDouble(), box.minX, box.maxX);
                double y = MathHelper.lerp(this.random.nextDouble(), box.minY, box.maxY);
                double z = MathHelper.lerp(this.random.nextDouble(), box.minZ, box.maxZ);
                double vx = (this.random.nextDouble() - 0.5) * 0.45 * speed.getValue();
                double vy = (this.random.nextDouble() - 0.5) * 0.45 * speed.getValue();
                double vz = (this.random.nextDouble() - 0.5) * 0.45 * speed.getValue();
                switch (selectedMode) {
                    case "Огонь":
                        this.mc.world.addParticleClient(ParticleTypes.FLAME, x, y, z, vx, vy, vz);
                        break;
                    case "Сердца":
                        this.mc.world.addParticleClient(ParticleTypes.HEART, x, y, z, vx, vy, vz);
                        break;
                    case "Облако":
                        this.mc.world.addParticleClient(ParticleTypes.CLOUD, x, y, z, vx, vy, vz);
                        break;
                    case "Эндер":
                        this.mc.world.addParticleClient(ParticleTypes.PORTAL, x, y, z, vx, vy, vz);
                }
            }
        }
    }
}
