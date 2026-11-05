package com.example.auravisuals.module.render;

import com.example.auravisuals.compat.AuraRender;
import com.example.auravisuals.compat.WorldRenderContext;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ColorSetting;
import com.example.auravisuals.theme.ThemeManager;
import com.mojang.blaze3d.vertex.VertexFormat.DrawMode;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.PotionEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.hit.HitResult.Type;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.RaycastContext.FluidHandling;
import net.minecraft.world.RaycastContext.ShapeType;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Prediction extends Module {
    final BooleanSetting enderPearl = new BooleanSetting("Эндер пёрл", true);
    final BooleanSetting arrow = new BooleanSetting("Стрела", true);
    final BooleanSetting trident = new BooleanSetting("Трезубец", true);
    final BooleanSetting potions = new BooleanSetting("Зелья", false);
    final BooleanSetting windCharge = new BooleanSetting("Заряд ветра", true);
    final BooleanSetting useCustomColor = new BooleanSetting("Свой цвет", false);
    final ColorSetting color = new ColorSetting("Цвет", new Color(0, 255, 0), () -> this.useCustomColor.getValue());
    static final int MAX_SIMULATION_TICKS = 300;

    public Prediction() {
        super("Prediction", "Рисует траекторию полёта снарядов", Category.VISUALS);
        this.addSettings(this.enderPearl, this.arrow, this.trident, this.potions, this.windCharge, this.useCustomColor, this.color);
    }

    @Override
    public void onWorldRender(WorldRenderContext context) {
        if (this.mc.world != null && this.mc.player != null) {
            for (Entity entity : this.mc.world.getEntities()) {
                if (this.isValidEntity(entity) && this.isLocalOwner(entity) && !(entity.getVelocity().lengthSquared() < 0.01)) {
                    this.calculateAndRender(context, entity);
                }
            }

            this.renderHandPreview(context);
        }
    }

    boolean isValidEntity(Entity entity) {
        if (entity instanceof EnderPearlEntity) {
            return this.enderPearl.getValue();
        } else if (entity instanceof ArrowEntity) {
            return this.arrow.getValue();
        } else if (entity instanceof TridentEntity) {
            return this.trident.getValue();
        } else if (entity instanceof PotionEntity) {
            return this.potions.getValue();
        } else {
            return entity.getType() == EntityType.WIND_CHARGE ? this.windCharge.getValue() : false;
        }
    }

    boolean isLocalOwner(Entity entity) {
        Entity owner = null;
        if (entity instanceof PersistentProjectileEntity projectile) {
            owner = projectile.getOwner();
        } else if (entity instanceof ThrownItemEntity thrown) {
            owner = thrown.getOwner();
        }

        return owner == this.mc.player;
    }

    void calculateAndRender(WorldRenderContext context, Entity entity) {
        Prediction$SimulationResult simulation = this.simulateTrajectory(
            entity.getEntityPos(),
            entity.getVelocity(),
            entity,
            (motion, position) -> this.updateEntityMotion(entity, motion, position),
            entity instanceof ThrownItemEntity
        );
        Vec3d visualPos = entity.getLerpedPos(context.tickCounter().getTickProgress(true));
        if (!simulation.path.isEmpty()) {
            ArrayList<Vec3d> connection = new ArrayList<>(2);
            connection.add(visualPos);
            connection.add(simulation.path.get(0));
            int lineColor = simulation.hitEntity ? -65536 : this.getActiveColor();
            this.renderLine(context, connection, lineColor);
            this.renderLine(context, simulation.path, lineColor);
        }

        if (simulation.hitPos != null) {
            int circleColor = simulation.hitEntity ? -65536 : this.getActiveColor();
            float radius = this.getImpactRadius(entity);
            this.renderWorldCircle(context, simulation.hitPos, radius, this.getHitNormal(simulation.hitResult), circleColor);
            if (entity instanceof PotionEntity) {
                this.renderWorldCircle(context, simulation.hitPos, 0.25F, this.getHitNormal(simulation.hitResult), circleColor);
            }
        }
    }

    Vec3d calculateHandPreviewMotion(float tickDelta, float rollDeg, float yawOffsetDeg, float throwSpeed) {
        float pitch = this.mc.player.getPitch(tickDelta);
        float yaw = this.mc.player.getYaw(tickDelta) + yawOffsetDeg;
        float radPitch = pitch * (float) (Math.PI / 180.0);
        float radYaw = yaw * (float) (Math.PI / 180.0);
        float cosPitch = MathHelper.cos(radPitch);
        Vec3d aim = new Vec3d(
            -MathHelper.sin(radYaw) * cosPitch, -MathHelper.sin((pitch + rollDeg) * (float) (Math.PI / 180.0)), MathHelper.cos(radYaw) * cosPitch
        );
        if (aim.lengthSquared() > 1.0E-12) {
            aim = aim.normalize();
        }

        Vec3d playerVelocity = this.mc.player.getVelocity();
        Vec3d motion = aim.multiply(throwSpeed);
        return motion.add(playerVelocity.x, this.mc.player.isOnGround() ? 0.0 : playerVelocity.y, playerVelocity.z);
    }

    void renderHandPreview(WorldRenderContext context) {
        if (this.mc.player != null && this.mc.world != null) {
            float tickDelta = context.tickCounter().getTickProgress(true);
            Vec3d startPos = this.mc.player.getCameraPosVec(tickDelta);
            ItemStack mainHand = this.mc.player.getMainHandStack();
            ItemStack offHand = this.mc.player.getOffHandStack();
            if (this.enderPearl.getValue()) {
                ItemStack pearlStack = mainHand;
                if (!mainHand.isOf(Items.ENDER_PEARL)) {
                    pearlStack = offHand.isOf(Items.ENDER_PEARL) ? offHand : ItemStack.EMPTY;
                }

                if (!pearlStack.isEmpty()) {
                    Vec3d motion2 = this.calculateHandPreviewMotion(tickDelta, 0.0F, 0.0F, 1.5F);
                    this.renderPreviewTrajectory(context, Items.ENDER_PEARL, startPos, motion2, 0.25F);
                }
            }

            float pull;
            ItemStack active;
            if (this.arrow.getValue()
                && this.mc.player.isUsingItem()
                && (active = this.mc.player.getActiveItem()).isOf(Items.BOW)
                && (pull = BowItem.getPullProgress(active.getMaxUseTime(this.mc.player) - this.mc.player.getItemUseTimeLeft())) > 0.05F) {
                Vec3d motion = this.calculateHandPreviewMotion(tickDelta, 0.0F, 0.0F, pull * 3.0F);
                this.renderPreviewTrajectory(context, Items.ARROW, startPos, motion, 0.25F);
            }

            if (this.arrow.getValue()) {
                ItemStack crossbowStack = mainHand;
                if (!mainHand.isOf(Items.CROSSBOW)) {
                    crossbowStack = offHand.isOf(Items.CROSSBOW) ? offHand : ItemStack.EMPTY;
                }

                if (!crossbowStack.isEmpty() && CrossbowItem.isCharged(crossbowStack)) {
                    ChargedProjectilesComponent charged = crossbowStack.get(DataComponentTypes.CHARGED_PROJECTILES);
                    boolean multishot = charged != null && charged.getProjectiles().size() >= 3;
                    float[] fArray;
                    if (multishot) {
                        float[] fArray2 = new float[]{-10.0F, 0.0F, 0.0F};
                        fArray = fArray2;
                        fArray2[2] = 10.0F;
                    } else {
                        float[] fArray3 = new float[1];
                        fArray = fArray3;
                        fArray3[0] = 0.0F;
                    }

                    for (float yawOffset : fArray) {
                        Vec3d motion3 = this.calculateHandPreviewMotion(tickDelta, 0.0F, yawOffset, 3.15F);
                        this.renderPreviewTrajectory(context, Items.ARROW, startPos, motion3, 0.25F);
                    }
                }
            }

            if (this.trident.getValue()
                && this.mc.player.isUsingItem()
                && (active = this.mc.player.getActiveItem()).isOf(Items.TRIDENT)
                && (pull = MathHelper.clamp((active.getMaxUseTime(this.mc.player) - this.mc.player.getItemUseTimeLeft()) / 10.0F, 0.0F, 1.0F)) > 0.05F) {
                Vec3d motion = this.calculateHandPreviewMotion(tickDelta, 0.0F, 0.0F, pull * 2.5F);
                this.renderPreviewTrajectory(context, Items.TRIDENT, startPos, motion, 0.25F);
            }

            if (this.potions.getValue()) {
                ItemStack potionStack = mainHand;
                if (!this.isThrowablePotion(mainHand)) {
                    potionStack = this.isThrowablePotion(offHand) ? offHand : ItemStack.EMPTY;
                }

                if (!potionStack.isEmpty()) {
                    Vec3d motion4 = this.calculateHandPreviewMotion(tickDelta, -20.0F, 0.0F, 0.5F);
                    this.renderPreviewTrajectory(context, potionStack.getItem(), startPos.add(0.0, -0.1, 0.0), motion4, 2.8F);
                }
            }

            if (this.windCharge.getValue()) {
                ItemStack windStack = mainHand;
                if (!mainHand.isOf(Items.WIND_CHARGE)) {
                    windStack = offHand.isOf(Items.WIND_CHARGE) ? offHand : ItemStack.EMPTY;
                }

                if (!windStack.isEmpty()) {
                    Vec3d motion5 = this.calculateHandPreviewMotion(tickDelta, 0.0F, 0.0F, 1.5F);
                    this.renderPreviewTrajectory(context, Items.WIND_CHARGE, startPos, motion5, 0.3F);
                }
            }
        }
    }

    void renderPreviewTrajectory(WorldRenderContext context, Item item, Vec3d startPos, Vec3d startMotion, float endCircleRadius) {
        Prediction$SimulationResult simulation = this.simulateTrajectory(
            startPos,
            startMotion,
            this.mc.player,
            (motion, position) -> this.updateItemMotion(item, motion, position),
            item == Items.ENDER_PEARL || this.isThrowablePotion(item)
        );
        boolean hasOtherPlayer = false;
        if (simulation.hitPos != null) {
            Vec3d hitNormal = this.getHitNormal(simulation.hitResult);
            int circleColor = this.getActiveColor();
            if (this.isThrowablePotion(item) && this.mc.world != null) {
                double radiusSq = endCircleRadius * endCircleRadius;

                for (PlayerEntity player : this.mc.world.getPlayers()) {
                    if (player != null && player.isAlive() && player != this.mc.player && player.squaredDistanceTo(simulation.hitPos) <= radiusSq) {
                        hasOtherPlayer = true;
                        break;
                    }
                }

                if (hasOtherPlayer) {
                    circleColor = -65536;
                }
            }

            if (simulation.hitEntity) {
                circleColor = -65536;
            }

            this.renderWorldCircle(context, simulation.hitPos, endCircleRadius, hitNormal, circleColor);
            if (this.isThrowablePotion(item)) {
                this.renderWorldCircle(context, simulation.hitPos, 0.25F, hitNormal, circleColor);
            }
        }

        int lineColor = !simulation.hitEntity && !hasOtherPlayer ? this.getActiveColor() : -65536;
        List<Vec3d> renderPath = this.createPreviewRenderPath(context, simulation.path, this.isThrowablePotion(item));
        this.renderLine(context, this.clipPreviewPathAtImpact(renderPath, simulation.hitPos, simulation.hitResult), lineColor);
    }

    List<Vec3d> clipPreviewPathAtImpact(List<Vec3d> path, Vec3d impactPos, HitResult hitResult) {
        if (path.size() >= 2 && impactPos != null && hitResult != null) {
            Vec3d normal = this.getHitNormal(hitResult);
            if (normal.lengthSquared() < 1.0E-8) {
                return path;
            }

            normal = normal.normalize();
            ArrayList<Vec3d> clipped = new ArrayList<>(path.size());

            Vec3d point;
            for (int i = 0; i < path.size() - 1 && !((point = path.get(i)).subtract(impactPos).dotProduct(normal) < -1.0E-4); i++) {
                clipped.add(point);
            }

            if (clipped.isEmpty()) {
                clipped.add(path.get(0));
            }

            clipped.add(impactPos);
            return clipped;
        } else {
            return path;
        }
    }

    List<Vec3d> createPreviewRenderPath(WorldRenderContext context, List<Vec3d> simulatedPath, boolean levelPotionArc) {
        if (simulatedPath.size() >= 2 && this.mc.options.getPerspective().isFirstPerson()) {
            Vec3d cameraPos = context.camera().getCameraPos();
            Vec3d viewDirection = this.getCrosshairDirection(context);
            Vec3d crosshairStart = cameraPos.add(viewDirection.multiply(0.2));
            Vec3d crosshairGuide = cameraPos.add(viewDirection.multiply(0.38));
            Vec3d originCorrection = crosshairStart.subtract(simulatedPath.get(0));
            ArrayList<Vec3d> controlPath = new ArrayList<>(simulatedPath.size());
            controlPath.add(crosshairGuide);
            int transitionPoints = Math.min(4, simulatedPath.size() - 1);

            for (int i = 1; i < simulatedPath.size(); i++) {
                double progress = Math.min(1.0, (double)i / transitionPoints);
                double smoothProgress = progress * progress * (3.0 - 2.0 * progress);
                Vec3d correction = originCorrection.multiply(1.0 - smoothProgress);
                controlPath.add(simulatedPath.get(i).add(correction));
            }

            List<Vec3d> smoothTail = this.smoothPath(controlPath, 3);
            ArrayList<Vec3d> renderPath = new ArrayList<>(smoothTail.size() + 1);
            renderPath.add(crosshairStart);
            renderPath.addAll(smoothTail);
            return levelPotionArc ? this.levelPotionPreviewArc(renderPath, viewDirection) : renderPath;
        } else {
            return simulatedPath;
        }
    }

    List<Vec3d> levelPotionPreviewArc(List<Vec3d> path, Vec3d viewDirection) {
        if (path.size() < 4) {
            return path;
        }

        Vec3d side = viewDirection.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        side = side.lengthSquared() < 1.0E-8 ? new Vec3d(1.0, 0.0, 0.0) : side.normalize();
        Vec3d screenUp = side.crossProduct(viewDirection).normalize();
        Vec3d start = path.get(0);
        Vec3d end = path.get(path.size() - 1);
        double endScreenHeight = end.subtract(start).dotProduct(screenUp);
        int guideIndex = 1;
        int transitionLength = path.size() - 1 - guideIndex;
        ArrayList<Vec3d> leveled = new ArrayList<>(path.size());

        for (int i = 0; i < path.size(); i++) {
            if (i <= guideIndex) {
                leveled.add(path.get(i));
            } else {
                double t = (double)(i - guideIndex) / transitionLength;
                double smooth = t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
                Vec3d point = path.get(i);
                double currentScreenHeight = point.subtract(start).dotProduct(screenUp);
                double desiredScreenHeight = endScreenHeight * smooth;
                leveled.add(point.add(screenUp.multiply(desiredScreenHeight - currentScreenHeight)));
            }
        }

        return leveled;
    }

    Vec3d getCrosshairDirection(WorldRenderContext context) {
        Vector3f forward = new Vector3f(context.camera().getHorizontalPlane());
        if (Float.isFinite(forward.x) && Float.isFinite(forward.y) && Float.isFinite(forward.z) && !(forward.lengthSquared() < 1.0E-6F)) {
            forward.normalize();
            return new Vec3d(forward.x, forward.y, forward.z);
        } else {
            return this.mc.player.getRotationVec(context.tickCounter().getTickProgress(true));
        }
    }

    List<Vec3d> smoothPath(List<Vec3d> controlPath, int subdivisions) {
        if (controlPath.size() >= 3 && subdivisions > 1) {
            ArrayList<Vec3d> smoothed = new ArrayList<>((controlPath.size() - 1) * subdivisions + 1);
            smoothed.add(controlPath.get(0));

            for (int i = 0; i < controlPath.size() - 1; i++) {
                Vec3d p0 = controlPath.get(Math.max(0, i - 1));
                Vec3d p1 = controlPath.get(i);
                Vec3d p2 = controlPath.get(i + 1);
                Vec3d p3 = controlPath.get(Math.min(controlPath.size() - 1, i + 2));

                for (int step = 1; step <= subdivisions; step++) {
                    double t = (double)step / subdivisions;
                    smoothed.add(this.catmullRom(p0, p1, p2, p3, t));
                }
            }

            return smoothed;
        } else {
            return controlPath;
        }
    }

    Vec3d catmullRom(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, double t) {
        double t2 = t * t;
        double t3 = t2 * t;
        double x = 0.5 * (2.0 * p1.x + (-p0.x + p2.x) * t + (2.0 * p0.x - 5.0 * p1.x + 4.0 * p2.x - p3.x) * t2 + (-p0.x + 3.0 * p1.x - 3.0 * p2.x + p3.x) * t3);
        double y = 0.5 * (2.0 * p1.y + (-p0.y + p2.y) * t + (2.0 * p0.y - 5.0 * p1.y + 4.0 * p2.y - p3.y) * t2 + (-p0.y + 3.0 * p1.y - 3.0 * p2.y + p3.y) * t3);
        double z = 0.5 * (2.0 * p1.z + (-p0.z + p2.z) * t + (2.0 * p0.z - 5.0 * p1.z + 4.0 * p2.z - p3.z) * t2 + (-p0.z + 3.0 * p1.z - 3.0 * p2.z + p3.z) * t3);
        return new Vec3d(x, y, z);
    }

    Prediction$SimulationResult simulateTrajectory(
        Vec3d startPos, Vec3d startMotion, Entity ignore, Prediction$MotionUpdater motionUpdater, boolean updateBeforeMove
    ) {
        ArrayList<Vec3d> path = new ArrayList<>();
        Vec3d lastPos = startPos;
        Vec3d motion = startMotion;
        path.add(lastPos);
        HitResult hitResult = null;
        boolean hitEntity = false;
        int ticks = 0;

        for (int i = 0; i < 300; ticks = i++) {
            if (updateBeforeMove) {
                motion = motionUpdater.update(motion, lastPos);
            }

            Vec3d nextPos = lastPos.add(motion);
            int cx = (int)Math.floor(nextPos.x) >> 4;
            if (!this.mc.world.isChunkLoaded(cx, (int)Math.floor(nextPos.z) >> 4)) {
                ticks = i;
                break;
            }

            BlockHitResult blockHit = this.mc.world.raycast(new RaycastContext(lastPos, nextPos, ShapeType.COLLIDER, FluidHandling.NONE, ignore));
            EntityHitResult entityHit = this.raycastEntity(lastPos, nextPos, ignore);
            if (entityHit != null) {
                double entityDistSq = lastPos.squaredDistanceTo(entityHit.getPos());
                double blockDistSq = blockHit.getType() != Type.MISS ? lastPos.squaredDistanceTo(blockHit.getPos()) : Double.MAX_VALUE;
                if (entityDistSq <= blockDistSq) {
                    hitResult = entityHit;
                    hitEntity = true;
                    path.add(entityHit.getPos());
                    ticks = i;
                    break;
                }
            }

            if (blockHit.getType() != Type.MISS) {
                hitResult = blockHit;
                path.add(blockHit.getPos());
                ticks = i;
                break;
            }

            path.add(nextPos);
            lastPos = nextPos;
            if (!updateBeforeMove) {
                motion = motionUpdater.update(motion, lastPos);
            }
        }

        Vec3d hitPos = hitResult != null ? hitResult.getPos() : path.get(path.size() - 1);
        return new Prediction$SimulationResult(path, hitPos, hitResult, hitEntity, ticks / 20.0F);
    }

    Vec3d updateEntityMotion(Entity entity, Vec3d motion, Vec3d position) {
        if (entity instanceof ThrownItemEntity) {
            double gravity = entity instanceof EnderPearlEntity ? 0.03 : 0.05;
            double drag = this.isInWater(position) ? 0.8 : 0.99;
            return motion.subtract(0.0, gravity, 0.0).multiply(drag);
        }

        motion = this.isInWater(position) && !(entity instanceof TridentEntity)
            ? motion.multiply(entity instanceof EnderPearlEntity ? 0.8F : 0.6F)
            : motion.multiply(0.99F);
        if (entity.getType() != EntityType.WIND_CHARGE && !entity.hasNoGravity()) {
            motion = motion.subtract(0.0, entity instanceof EnderPearlEntity ? 0.03 : 0.05, 0.0);
        }

        return motion;
    }

    Vec3d updateItemMotion(Item item, Vec3d motion, Vec3d position) {
        if (item != Items.ENDER_PEARL && !this.isThrowablePotion(item)) {
            motion = this.isInWater(position) && item != Items.TRIDENT ? motion.multiply(item == Items.ENDER_PEARL ? 0.8F : 0.6F) : motion.multiply(0.99F);
            return item == Items.WIND_CHARGE ? motion : motion.subtract(0.0, item == Items.ENDER_PEARL ? 0.03 : 0.05, 0.0);
        } else {
            double gravity = item == Items.ENDER_PEARL ? 0.03 : 0.05;
            double drag = this.isInWater(position) ? 0.8 : 0.99;
            return motion.subtract(0.0, gravity, 0.0).multiply(drag);
        }
    }

    boolean isInWater(Vec3d position) {
        return this.mc.world != null && this.mc.world.getBlockState(BlockPos.ofFloored(position)).getBlock() == Blocks.WATER;
    }

    boolean isThrowablePotion(ItemStack stack) {
        return !stack.isEmpty() && this.isThrowablePotion(stack.getItem());
    }

    boolean isThrowablePotion(Item item) {
        return item == Items.SPLASH_POTION || item == Items.LINGERING_POTION;
    }

    float getImpactRadius(Entity entity) {
        if (entity instanceof PotionEntity) {
            return 2.8F;
        } else {
            return entity.getType() == EntityType.WIND_CHARGE ? 0.3F : 0.25F;
        }
    }

    EntityHitResult raycastEntity(Vec3d start, Vec3d end, Entity ignore) {
        if (this.mc.world == null) {
            return null;
        }

        Box searchBox = new Box(start, end).expand(0.5);
        double closestDistSq = Double.MAX_VALUE;
        EntityHitResult closestHit = null;

        for (Entity entity : this.mc
            .world
            .getOtherEntities(
                ignore,
                searchBox,
                target -> target.isAlive()
                    && target != this.mc.player
                    && target instanceof LivingEntity
                    && !target.isInvisible()
                    && !target.isSpectator()
                    && !(target instanceof ArmorStandEntity)
            )) {
            double distSq;
            Optional hitPos;
            if (entity.getId() >= -1000
                && !(hitPos = entity.getBoundingBox().expand(0.1).raycast(start, end)).isEmpty()
                && (distSq = start.squaredDistanceTo((Vec3d)hitPos.get())) < closestDistSq) {
                closestDistSq = distSq;
                closestHit = new EntityHitResult(entity, (Vec3d)hitPos.get());
            }
        }

        return closestHit;
    }

    void renderWorldCircle(WorldRenderContext context, Vec3d center, float radius, Vec3d normal, int color) {
        if (!(radius <= 0.0F)) {
            MatrixStack matrices = context.matrixStack();
            matrices.push();

            try {
                Vec3d cameraPos = context.camera().getCameraPos();
                matrices.translate(center.x - cameraPos.x, center.y - cameraPos.y, center.z - cameraPos.z);
                Vector3f targetNormal = new Vector3f((float)normal.x, (float)normal.y, (float)normal.z);
                if (targetNormal.lengthSquared() < 1.0E-4F) {
                    targetNormal.set(0.0F, 1.0F, 0.0F);
                } else {
                    targetNormal.normalize();
                }

                Quaternionf rotation = new Quaternionf().rotateTo(new Vector3f(0.0F, 1.0F, 0.0F), targetNormal);
                matrices.multiply(rotation);
                Matrix4f matrix = matrices.peek().getPositionMatrix();
                int r = color >> 16 & 0xFF;
                int g = color >> 8 & 0xFF;
                int b = color & 0xFF;
                int a = radius >= 3.0F ? 220 : 255;
                Tessellator tessellator = Tessellator.getInstance();
                int segments = 48;
                float ringThickness = Math.max(0.08F, radius * 0.075F);
                BufferBuilder fill = tessellator.begin(DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);

                for (int i = 0; i <= segments; i++) {
                    double angle = (Math.PI * 2) * i / segments;
                    float cos = (float)Math.cos(angle);
                    float sin = (float)Math.sin(angle);
                    fill.vertex(matrix, cos * (radius + ringThickness), 0.0F, sin * (radius + ringThickness)).color(r, g, b, Math.max(1, (int)(a * 0.55F)));
                    fill.vertex(matrix, cos * Math.max(0.01F, radius - ringThickness), 0.0F, sin * Math.max(0.01F, radius - ringThickness))
                        .color(r, g, b, Math.max(1, (int)(a * 0.18F)));
                }

                AuraRender.drawColor(fill, AuraRender.color(DrawMode.TRIANGLE_STRIP, false, false));

                for (int glowLayer = 2; glowLayer >= 1; glowLayer--) {
                    BufferBuilder glow = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
                    int glowAlpha = Math.max(1, (int)(a * 0.07F * glowLayer));

                    for (int i = 0; i < segments; i++) {
                        double angle1 = (Math.PI * 2) * i / segments;
                        double angle2 = (Math.PI * 2) * (i + 1) / segments;
                        float x1 = (float)(Math.cos(angle1) * radius);
                        float z1 = (float)(Math.sin(angle1) * radius);
                        float x2 = (float)(Math.cos(angle2) * radius);
                        float z2 = (float)(Math.sin(angle2) * radius);
                        glow.vertex(matrix, x1, 0.0F, z1).color(r, g, b, glowAlpha);
                        glow.vertex(matrix, x2, 0.0F, z2).color(r, g, b, glowAlpha);
                    }

                    AuraRender.drawColorLines(glow, AuraRender.color(DrawMode.DEBUG_LINES, false, false), 1.8F + glowLayer * 1.0F);
                }

                BufferBuilder buffer = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

                for (int i = 0; i < segments; i++) {
                    double angle1 = (Math.PI * 2) * i / segments;
                    double angle2 = (Math.PI * 2) * (i + 1) / segments;
                    float x1 = (float)(Math.cos(angle1) * radius);
                    float z1 = (float)(Math.sin(angle1) * radius);
                    float x2 = (float)(Math.cos(angle2) * radius);
                    float z2 = (float)(Math.sin(angle2) * radius);
                    buffer.vertex(matrix, x1, 0.0F, z1).color(r, g, b, a);
                    buffer.vertex(matrix, x2, 0.0F, z2).color(r, g, b, a);
                }

                AuraRender.drawColorLines(buffer, AuraRender.color(DrawMode.DEBUG_LINES, false, false), radius >= 3.0F ? 2.8F : 2.2F);
            } finally {
                matrices.pop();
            }
        }
    }

    Vec3d getHitNormal(HitResult hitResult) {
        if (hitResult instanceof EntityHitResult entityHit) {
            Entity entity = entityHit.getEntity();
            Vec3d hitPos = entityHit.getPos();
            Box box = entity.getBoundingBox();
            Vec3d center = new Vec3d((box.minX + box.maxX) * 0.5, (box.minY + box.maxY) * 0.5, (box.minZ + box.maxZ) * 0.5);
            Vec3d normal = hitPos.subtract(center);
            if (normal.lengthSquared() > 1.0E-6) {
                return normal.normalize();
            }
        }

        if (hitResult instanceof BlockHitResult blockHit) {
            Direction side = blockHit.getSide();
            Vec3i vec = side.getVector();
            return new Vec3d(vec.getX(), vec.getY(), vec.getZ());
        } else {
            return new Vec3d(0.0, 1.0, 0.0);
        }
    }

    void renderLine(WorldRenderContext context, List<Vec3d> path) {
        this.renderLine(context, path, this.getActiveColor());
    }

    void renderLine(WorldRenderContext context, List<Vec3d> path, int color) {
        if (path.size() >= 2) {
            MatrixStack matrices = context.matrixStack();
            matrices.push();

            try {
                Vec3d cameraPos = context.camera().getCameraPos();
                Tessellator tessellator = Tessellator.getInstance();
                Matrix4f matrix = matrices.peek().getPositionMatrix();
                int r = color >> 16 & 0xFF;
                int g = color >> 8 & 0xFF;
                int b = color & 0xFF;
                BufferBuilder glowBuffer = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

                for (int i = 0; i < path.size() - 1; i++) {
                    Vec3d p1 = path.get(i).subtract(cameraPos);
                    Vec3d p2 = path.get(i + 1).subtract(cameraPos);
                    glowBuffer.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(r, g, b, 40);
                    glowBuffer.vertex(matrix, (float)p2.x, (float)p2.y, (float)p2.z).color(r, g, b, 40);
                }

                AuraRender.drawColorLines(glowBuffer, AuraRender.color(DrawMode.DEBUG_LINES, true, false), 6.0F);
                BufferBuilder buffer = tessellator.begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

                for (int i = 0; i < path.size() - 1; i++) {
                    Vec3d p1 = path.get(i).subtract(cameraPos);
                    Vec3d p2 = path.get(i + 1).subtract(cameraPos);
                    buffer.vertex(matrix, (float)p1.x, (float)p1.y, (float)p1.z).color(r, g, b, 255);
                    buffer.vertex(matrix, (float)p2.x, (float)p2.y, (float)p2.z).color(r, g, b, 255);
                }

                AuraRender.drawColorLines(buffer, AuraRender.color(DrawMode.DEBUG_LINES, false, false), 2.0F);
            } finally {
                matrices.pop();
            }
        }
    }

    int getActiveColor() {
        return this.useCustomColor.getValue() ? this.color.getValue() : ThemeManager.INSTANCE.getVisualModulesColor();
    }
}
