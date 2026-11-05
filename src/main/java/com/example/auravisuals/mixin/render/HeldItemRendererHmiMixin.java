package com.example.auravisuals.mixin.render;

import com.example.auravisuals.module.render.Hands;
import java.util.Random;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalBlockTags;
import net.fabricmc.fabric.api.tag.convention.v2.ConventionalItemTags;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CampfireBlock;
import net.minecraft.block.ComparatorBlock;
import net.minecraft.block.RedstoneTorchBlock;
import net.minecraft.block.RepeaterBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.AttributeModifiersComponent.Entry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.BlockItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererHmiMixin {
    private float f;
    private float h;
    @Unique
    boolean hmi$repPower = false;
    @Unique
    float hmi$prevAge = 0.0F;
    @Unique
    double hmi$previousRotation = 0.0;
    @Unique
    float hmi$swingAngleY = 0.0F;
    @Unique
    float hmi$swingAngleX = 0.0F;
    @Unique
    float hmi$swingVelocityY = 0.0F;
    @Unique
    float hmi$swingVelocityX = 0.0F;
    @Unique
    float hmi$swingVelocityZ = 0.0F;
    @Unique
    float hmi$vertAngleY = 0.0F;
    @Unique
    float hmi$vertVelocityY = 0.0F;
    @Unique
    float hmi$vertVelocityYSlime = 0.0F;
    @Unique
    float hmi$vertAngleYSlime = 0.0F;
    @Unique
    float hmi$riptideCounter = 0.0F;
    @Unique
    float hmi$netherCounter = 0.0F;
    @Unique
    float hmi$fallCounter = 0.0F;
    @Unique
    float hmi$inWaterCounter = 0.0F;
    @Unique
    float hmi$inspect = 0.0F;
    @Unique
    float hmi$tilt = 0.0F;
    @Unique
    float hmi$freezeCounter = 0.0F;
    @Unique
    float hmi$clCount = 0.0F;
    @Unique
    float hmi$crawlCount = 0.0F;
    @Unique
    float hmi$directionalCrawlCount = 0.0F;
    @Unique
    float hmi$climbCount = 0.0F;
    @Unique
    float hmi$mouseHolding = 1.0F;
    @Unique
    boolean hmi$isSwinging = false;
    @Unique
    float hmi$swingProgress = 0.0F;
    @Unique
    boolean hmi$isForward = false;
    @Unique
    boolean hmi$isAttacking = false;
    @Unique
    boolean hmi$left = false;
    @Shadow
    ItemStack mainHand;
    @Shadow
    @Final
    MinecraftClient client;

    @Shadow
    protected abstract void renderArm(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, Arm var4);

    @Shadow
    protected abstract void renderArmHoldingItem(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, float var4, float var5, Arm var6);

    @Shadow
    protected abstract void renderMapInOneHand(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, float var4, Arm var5, float var6, ItemStack var7);

    @Shadow
    protected abstract void renderMapInBothHands(MatrixStack var1, OrderedRenderCommandQueue var2, int var3, float var4, float var5, float var6);

    @Shadow
    protected abstract void applyEquipOffset(MatrixStack var1, Arm var2, float var3);

    @Shadow
    protected abstract void swingArm(float var1, MatrixStack var2, int var3, Arm var4);

    @Shadow
    public abstract void renderItem(LivingEntity var1, ItemStack var2, ItemDisplayContext var3, MatrixStack var4, OrderedRenderCommandQueue var5, int var6);

    @Unique
    float hmiEaseInOutBack(float x) {
        float c1 = 1.70158F;
        float c2 = c1 * 1.525F;
        return (float)(
            x < 0.5
                ? Math.pow(2.0 * x, 2.0) * ((c2 + 1.0F) * 2.0F * x - c2) / 2.0
                : (Math.pow(2.0 * x - 2.0, 2.0) * ((c2 + 1.0F) * (x * 2.0F - 2.0F) + c2) + 2.0) / 2.0
        );
    }

    @Unique
    float hmiGetAttackDamage(ItemStack stack) {
        AttributeModifiersComponent modifiers = stack.getComponents().get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
        if (modifiers == null) {
            return 0.0F;
        }

        float totalDamage = 0.0F;

        for (Entry entry : modifiers.modifiers()) {
            if (entry.attribute().value() == EntityAttributes.ATTACK_DAMAGE.value()) {
                totalDamage += (float)entry.modifier().value();
            }
        }

        return totalDamage;
    }

    @Unique
    boolean hmiIsSharpAnimation(Hands config) {
        return config != null && config.hmiAnimationType.getSelected().equals("Шарп");
    }

    @Unique
    void hmiAltSwing(MatrixStack matrices, Arm arm, float swingProgress, ItemStack item) {
        int i = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * 3.14F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * (45.0F + f * 0.0F)));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * -45.0F));
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    void hmiOnRenderFirstPersonItem(
        AbstractClientPlayerEntity player,
        float tickDelta,
        float pitch,
        Hand hand,
        float swingProgress,
        ItemStack item,
        float equipProgress,
        MatrixStack matrices,
        OrderedRenderCommandQueue orderedRenderCommandQueue,
        int light,
        CallbackInfo ci
    ) {
        Hands swings = Hands.getInstance();
        if (swings.isState() && swings.hmiEnable.isValue()) {
            boolean isMainHand = hand == Hand.MAIN_HAND;
            Arm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
            float sideFactor = isMainHand ? 1.0F : -1.0F;
            if (swings.swapHands.isValue()) {
                arm = arm.getOpposite();
                sideFactor *= -1.0F;
            }

            this.hmiRenderCustomFirstPersonItem(
                player, tickDelta, pitch, hand, arm, sideFactor, swingProgress, item, equipProgress, matrices, orderedRenderCommandQueue, light
            );
            ci.cancel();
        }
    }

    @Unique
    void hmiRenderCustomFirstPersonItem(
        AbstractClientPlayerEntity player,
        float tickDelta,
        float pitch,
        Hand hand,
        Arm arm,
        float sideFactor,
        float swingProgress,
        ItemStack item,
        float equipProgress,
        MatrixStack matrices,
        OrderedRenderCommandQueue orderedRenderCommandQueue,
        int light
    ) {
        Hands swings = Hands.getInstance();
        if (swings.isState() && swings.hmiEnable.isValue()) {
            boolean isUsingSpyglass = player.isUsingItem() && player.getActiveItem().isOf(Items.SPYGLASS);
            if (!isUsingSpyglass) {
                Hands config = Hands.getInstance();
                float yaw = player.getYaw();
                double radians = Math.toRadians(yaw);
                double forwardX = -Math.sin(radians);
                double forwardZ = Math.cos(radians);
                Vec3d horizontalVelocity = player.getVelocity();
                double dotProduct = horizontalVelocity.x * forwardX + horizontalVelocity.z * forwardZ;
                double crossProduct = player.getVelocity().getHorizontal().x * forwardZ - horizontalVelocity.z * forwardX;
                float al = player.getPitch() != 0.0F ? 90.0F / player.getPitch() / 10.0F : 1.0F;
                if (al > 1.0F) {
                    al = 1.0F;
                }

                if (al < 0.0F) {
                    al = 1.0F;
                }

                boolean bl2 = hand == Hand.MAIN_HAND;
                matrices.push();
                matrices.push();
                double tt = 1.0 / Math.max(MinecraftClient.getInstance().getCurrentFps(), 1) * 30.0;
                float smoothness = MathHelper.clamp(config.hmiSmoothness.getFloatValue(), 0.35F, 2.5F);
                float hmiProgress = (float)Math.pow(MathHelper.clamp(swingProgress, 0.0F, 1.0F), smoothness);
                float swing_rot = hmiProgress < 0.6
                    ? MathHelper.sin(MathHelper.clamp(hmiProgress, 0.0F, 0.12506F) * 12.56F)
                    : MathHelper.sin(MathHelper.clamp(hmiProgress, 0.62532F, 0.75038F) * 12.56F);
                float swing = MathHelper.sin(hmiProgress * 3.14F);
                swing = this.hmiEaseInOutBack(swing);
                boolean sharpSword = item.isIn(ItemTags.SWORDS) && this.hmiIsSharpAnimation(config);
                if ((
                        item.isOf(Items.EXPERIENCE_BOTTLE)
                            || item.isOf(Items.WIND_CHARGE)
                            || item.isOf(Items.EGG)
                            || item.isOf(Items.ENDER_EYE)
                            || item.isOf(Items.SNOWBALL)
                            || item.getItem() instanceof SplashPotionItem
                            || item.getItem() instanceof LingeringPotionItem
                    )
                    && player.getOffHandStack().isEmpty()
                    && item.getUseAction() != UseAction.SPEAR
                    && !item.isOf(Items.FIRE_CHARGE)
                    && !player.isSwimming()
                    && !player.isCrawling()
                    && !player.isClimbing()) {
                    if (player.getMainArm() == Arm.LEFT) {
                        bl2 = !bl2;
                    }

                    matrices.push();
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-25.0F * sideFactor));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0F * sideFactor * swing));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
                    matrices.translate(-0.15 * sideFactor, 0.1, 0.1);
                    matrices.translate(0.0, -0.55 * swing, 0.4 * swing * 3.14F);
                    this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, 0.0F, arm.getOpposite());
                    matrices.pop();
                }

                if (this.client.options.attackKey.isPressed() && !this.hmi$isAttacking && swingProgress == 0.0) {
                    boolean l = this.hmi$left = !this.hmi$left;
                }

                if (!item.isEmpty()) {
                    if (player.getMainArm() == Arm.LEFT) {
                        bl2 = !bl2;
                    }

                    if ((this.hmi$left || item.isIn(ItemTags.AXES) || item.getUseAction() == UseAction.SPEAR || item.getUseAction() == UseAction.BLOCK)
                        && !item.isIn(ItemTags.SHOVELS)) {
                        if (sharpSword) {
                            matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.5 * swing);
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                        } else if (item.isIn(ItemTags.SWORDS) || item.isIn(ItemTags.AXES)) {
                            matrices.translate(0.8 * sideFactor * swing_rot, 0.3 * swing_rot, -0.5 * swing);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-20.0F * swing_rot));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70.0F * swing_rot * sideFactor));
                            if (item.isIn(ItemTags.SWORDS)) {
                                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                            } else {
                                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(30.0F * swing));
                            }
                        } else if (item.getUseAction() == UseAction.SPEAR) {
                            matrices.translate(0.0, 0.0, 0.45 * swing_rot);
                            matrices.translate(-0.25 * sideFactor * swing, -0.35 * swing_rot, -0.6 * swing);
                            matrices.translate(0.0, 0.1 * swing, 0.0);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(30.0F * swing_rot * sideFactor));
                        } else if (item.isIn(ConventionalItemTags.TOOLS) && item.getUseAction() != UseAction.BLOCK && !item.isIn(ItemTags.SHOVELS)) {
                            matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.5 * swing);
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                        } else if (item.getUseAction() != UseAction.BLOCK) {
                            matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.1 * swing);
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swing * sideFactor));
                        } else {
                            matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.2 * swing);
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-10.0F * swing_rot));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(20.0F * swing));
                        }
                    } else if (!item.isIn(ItemTags.SHOVELS)) {
                        if (sharpSword) {
                            matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.5 * swing);
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                        } else if (item.isIn(ItemTags.SWORDS)) {
                            matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.5 * swing);
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                        } else if (item.isIn(ConventionalItemTags.TOOLS) && !item.isIn(ItemTags.SHOVELS)) {
                            matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.5 * swing);
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0F * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                        } else {
                            matrices.translate(0.1 * sideFactor * swing_rot, 0.1 * swing_rot, -0.1 * swing);
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0F * swing_rot));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0F * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0F * swing));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * swing * sideFactor));
                        }
                    } else if (item.isIn(ItemTags.SHOVELS)) {
                        matrices.translate(0.0, 0.15 * swing_rot, -0.25 * swing_rot);
                        matrices.translate(0.0, 0.0, -0.2 * swing);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0F * swing_rot));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0F * swing_rot));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
                    }
                } else if (Block.getBlockFromItem(item.getItem()) != Blocks.AIR
                    && (
                        !item.isIn(ConventionalItemTags.TOOLS)
                            || item.isIn(ItemTags.TRIMMABLE_ARMOR)
                            || item.isIn(ItemTags.BOOKSHELF_BOOKS)
                            || item.getUseAction() == UseAction.EAT
                            || !item.isEnchantable()
                    )
                    && item.getUseAction() != UseAction.BOW
                    && item.getUseAction() != UseAction.SPYGLASS
                    && this.hmiGetAttackDamage(item) == 0.0F
                    && item.getUseAction() != UseAction.BLOCK
                    && !item.isOf(Items.WARPED_FUNGUS_ON_A_STICK)
                    && !item.isOf(Items.CARROT_ON_A_STICK)
                    && !item.isOf(Items.FISHING_ROD)
                    && !item.isOf(Items.SHEARS)) {
                    if ((swingProgress = (float)(swingProgress * 1.2)) > 1.0F) {
                        swingProgress = 0.0F;
                    }
                } else if (!item.isIn(ItemTags.SHOVELS) && (swingProgress = (float)(swingProgress * 1.5)) > 1.0F) {
                    swingProgress = 0.0F;
                }

                if (player.getVelocity().length() >= 0.08) {
                    this.hmi$crawlCount = (float)(this.hmi$crawlCount + 0.1 * player.getVelocity().length() * 2.0 * tt);
                    this.hmi$directionalCrawlCount = (float)(this.hmi$directionalCrawlCount + 0.1 * dotProduct * 4.0 * tt);
                    this.hmi$directionalCrawlCount = (float)(
                        this.hmi$directionalCrawlCount
                            + (dotProduct > 0.0 ? 0.1 * Math.abs(crossProduct) * 4.0 * tt : 0.1 * Math.abs(crossProduct) * -1.0 * 4.0 * tt)
                    );
                }

                if (player.getVelocity().getY() > 0.0) {
                    this.hmi$climbCount = (float)(this.hmi$climbCount + 0.1 * tt);
                }

                if (player.getVelocity().getY() < 0.0) {
                    this.hmi$climbCount = (float)(this.hmi$climbCount - 0.1 * tt);
                }

                if ((
                        player.isCrawling() && config.climbAndCrawl.isValue()
                            || player.isClimbing() && !player.isOnGround() && Math.abs(player.getVelocity().getY()) > 0.0 && config.climbAndCrawl.isValue()
                    )
                    && !player.isUsingItem()
                    && swingProgress == 0.0F) {
                    this.hmi$clCount = (float)(this.hmi$clCount + 0.1 * tt);
                    if (this.hmi$clCount > 1.0F) {
                        this.hmi$clCount = 1.0F;
                    }

                    if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20.0F * this.hmi$clCount));
                    }
                } else {
                    this.hmi$clCount = (float)(this.hmi$clCount * Math.pow(0.88F, tt));
                }

                if (swingProgress == 0.0F) {
                    matrices.translate(bl2 ? player.getPitch() / 650.0F * this.hmi$clCount * -1.0F : player.getPitch() / 650.0F * this.hmi$clCount, 0.0F, 0.0F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(player.getPitch() * this.hmi$clCount));
                }

                if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
                    matrices.translate(0.0F, 0.0F, player.getPitch() / 120.0F * this.hmi$clCount);
                } else if (swingProgress == 0.0F) {
                    matrices.translate(0.0F, 0.0F, player.getPitch() / 80.0F * this.hmi$clCount);
                }

                if (player.isClimbing()
                    && config.climbAndCrawl.isValue()
                    && !player.isOnGround()
                    && !item.isOf(Items.LANTERN)
                    && !item.isOf(Items.SOUL_LANTERN)
                    && !player.isUsingItem()) {
                    matrices.translate(0.0, 0.1, -0.2);
                }

                if (player.isInFluid() && !player.isSwimming() && !player.isSubmergedInWater()) {
                    this.hmi$inWaterCounter = (float)(this.hmi$inWaterCounter + 0.1 * tt);
                    if (this.hmi$inWaterCounter >= 1.0F) {
                        this.hmi$inWaterCounter = 1.0F;
                    }
                } else {
                    this.hmi$inWaterCounter = (float)(this.hmi$inWaterCounter * Math.pow(0.88F, tt));
                }

                this.hmi$freezeCounter = (float)(this.hmi$freezeCounter * Math.pow(0.88F, tt));
                matrices.translate(0.0, 0.02 * this.hmi$inWaterCounter, 0.0);
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(8.0F * sideFactor * this.hmi$inWaterCounter));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.3F * MathHelper.sin(this.hmi$freezeCounter * 5.0F)));
                if (player.getVelocity().getY() < -0.85 && item.isOf(Items.MACE) && player.getMainHandStack() == item) {
                    this.hmi$fallCounter = (float)(this.hmi$fallCounter + 0.1 * tt);
                    if (this.hmi$fallCounter >= 1.0F) {
                        this.hmi$fallCounter = 1.0F;
                    }
                } else {
                    this.hmi$fallCounter = (float)(this.hmi$fallCounter * Math.pow(0.88F, tt));
                }

                if (bl2) {
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F * this.hmi$fallCounter));
                    matrices.translate(0.0, -0.2 * this.hmi$fallCounter, 0.0);
                }

                this.hmi$vertAngleY = (float)(this.hmi$vertAngleY + player.getVelocity().getY() * 0.015F * tt);
                this.hmi$vertAngleY = (float)(this.hmi$vertAngleY - 0.1F * this.hmi$vertAngleY * tt);
                this.hmi$vertAngleY = (float)(this.hmi$vertAngleY * Math.pow(0.88F, tt));
                this.hmi$vertVelocityYSlime = (float)(this.hmi$vertVelocityYSlime + player.getVelocity().getY() * 0.015F * tt);
                this.hmi$vertVelocityYSlime = (float)(this.hmi$vertVelocityYSlime - 0.1F * this.hmi$vertAngleYSlime * tt);
                this.hmi$vertVelocityYSlime = (float)(this.hmi$vertVelocityYSlime * Math.pow(0.88F, tt));
                this.hmi$vertAngleYSlime = (float)(this.hmi$vertAngleYSlime + this.hmi$vertVelocityYSlime * tt);
                matrices.translate(0.0F, this.hmi$vertAngleY * -1.0F, 0.0F);
                matrices.translate(0.0, Math.sin(player.age * 0.1) * 0.007 * sideFactor, 0.0);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(0.15F * MathHelper.sin(player.age * 0.15F) * sideFactor));
                if (!item.isEmpty() || player.isCrawling() || player.isClimbing() && !player.isOnGround() || player.isSwimming()) {
                    if (player.getMainArm() == Arm.LEFT) {
                        bl2 = !bl2;
                    }

                    if (item.getUseAction() == UseAction.BLOCK) {
                        matrices.translate(0.0F, 0.0F, 0.0F);
                    } else {
                        matrices.translate(0.0, -0.1, 0.1);
                    }
                }

                if (item.isOf(Items.LANTERN) || item.isOf(Items.SOUL_LANTERN) || item.isIn(ItemTags.HANGING_SIGNS)) {
                    matrices.translate(0.0, 0.1, 0.0);
                    if (player.isSwimming()) {
                        matrices.translate(0.0, -0.1, 0.1);
                    }
                }

                if (player.isSwimming() && swingProgress == 0.0F && config.swimmingAnimation.isValue()) {
                    double distance = this.hmi$crawlCount;
                    double swingAmplitude = 1.5;
                    double frequency = 2.0;
                    double s = distance * frequency;
                    double handRotation = Math.sin(s) * swingAmplitude;
                    double smoothRotation = handRotation * 0.8 + this.hmi$previousRotation * 0.2;
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float)(bl2 ? smoothRotation : -smoothRotation)));
                    matrices.translate(0.0, 0.0, smoothRotation * 0.2F);
                    double k = this.hmi$crawlCount * 2.0F;
                    double a;
                    double b = a = Math.cos(k);
                    if (a <= 0.0) {
                        b = a * 0.5;
                    }

                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees((float)(bl2 ? b * 30.0 : b * 30.0 * -1.0)));
                    matrices.translate(0.0, 0.0, a * 0.2F);
                    if (item.isEmpty() && !bl2 && !player.isInvisible()) {
                        matrices.translate(1.0F * sideFactor, 0.0 - equipProgress * 0.3, 0.3);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * sideFactor));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * sideFactor));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                        this.hmiAltSwing(matrices, arm, swingProgress, item);
                        float c = MathHelper.sin(equipProgress * 3.14F);
                        matrices.scale(0.9F, 0.9F, 0.9F);
                        this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
                    }

                    this.hmi$previousRotation = smoothRotation;
                }

                if ((player.isClimbing() && !player.isOnGround() || player.isCrawling() && swingProgress == 0.0F) && !player.isUsingItem()) {
                    double s = this.hmi$climbCount;
                    float v = (float)player.getVelocity().getY();
                    float a = MathHelper.cos((float)s * 2.0F);
                    if (player.isClimbing()) {
                        if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN)) {
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(20.0F * a * sideFactor));
                        } else {
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(1.0F * a * sideFactor));
                        }
                    }

                    if (player.isCrawling() && !player.isUsingItem() && swingProgress == 0.0F) {
                        float crawlProgress = MathHelper.sin(this.hmi$directionalCrawlCount * 4.0F * this.hmi$mouseHolding);
                        float upAndDown = MathHelper.cos(this.hmi$directionalCrawlCount * 4.0F * this.hmi$mouseHolding);
                        if (item.isOf(Items.LANTERN) || item.isOf(Items.SOUL_LANTERN)) {
                            crawlProgress *= 0.14F;
                            upAndDown *= 0.14F;
                        }

                        matrices.translate(0.2 * crawlProgress, 0.3 * crawlProgress * sideFactor, -0.2 * crawlProgress * sideFactor * al);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0F * crawlProgress));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(MathHelper.clamp(20.0F * upAndDown * sideFactor, 0.0F, 20.0F)));
                    }

                    if (item.isEmpty() && !bl2 && !player.isInvisible() && (!player.isOnGround() && player.isClimbing() || player.isCrawling())) {
                        matrices.translate(1.0F * sideFactor, 0.0 - equipProgress * 0.3, 0.3);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * sideFactor));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * sideFactor));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                        this.hmiAltSwing(matrices, arm, swingProgress, item);
                        matrices.scale(0.9F, 0.9F, 0.9F);
                        this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
                    }
                }

                if (item.isEmpty()) {
                    if (bl2 && !player.isInvisible()) {
                        if ((player.isOnGround() || !player.isClimbing()) && !player.isSwimming() && !player.isCrawling()) {
                            if (player.getMainArm() == Arm.LEFT) {
                                bl2 = !bl2;
                            }

                            matrices.translate(0.0, 0.2 * swing_rot, 0.15 * swing_rot);
                            matrices.translate(0.1 * sideFactor * swing, 0.15 * swing, -0.45 * swing);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(35.0F * swing * sideFactor));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-30.0F * swing));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-10.0F * swing_rot * sideFactor));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F * swing_rot));
                            this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
                        } else {
                            matrices.translate(1.0F * sideFactor, 0.0 - equipProgress * 0.3, 0.3);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * sideFactor));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * sideFactor));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                            this.hmiAltSwing(matrices, arm, swingProgress, item);
                            float c = MathHelper.sin(equipProgress * 3.14F);
                            matrices.scale(0.9F, 0.9F, 0.9F);
                            this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
                        }
                    }
                } else if (item.contains(DataComponentTypes.MAP_ID)) {
                    if (bl2 && this.mainHand.isEmpty()) {
                        matrices.translate(0.0, 0.1, 0.0);
                        this.renderMapInBothHands(matrices, orderedRenderCommandQueue, light, pitch, equipProgress, swingProgress);
                    } else {
                        matrices.translate(bl2 ? -0.1 : 0.1, 0.1, 0.0);
                        this.renderMapInOneHand(matrices, orderedRenderCommandQueue, light, equipProgress, arm, swingProgress, item);
                    }
                } else if (item.getUseAction() == UseAction.CROSSBOW) {
                    matrices.push();
                    boolean bl22 = CrossbowItem.isCharged(item);
                    boolean bl32 = arm == Arm.RIGHT;
                    int i = bl32 ? 1 : -1;
                    if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                        this.applyEquipOffset(matrices, arm, equipProgress);
                        matrices.translate(i * -0.4785682F, -0.24387F, 0.05731531F);
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-11.935F));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 65.3F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 9.785F));
                        this.f = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                        float g = this.f / CrossbowItem.getPullTime(item, player);
                        if (g > 1.0F) {
                            g = 1.0F;
                        }

                        if (g > 0.1F) {
                            this.h = MathHelper.sin((this.f - 0.1F) * 1.3F);
                            float j = g - 0.1F;
                            float k = this.h * j;
                            matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                        }

                        matrices.translate(g * 0.0F, g * 0.0F, g * 0.04F);
                        matrices.scale(1.0F, 1.0F, 1.0F);
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(i * 45.0F));
                    } else {
                        this.swingArm(swingProgress, matrices, i, arm);
                        if (bl22 && swingProgress < 0.001F && bl2) {
                            matrices.translate(i * -0.341864F, 0.0F, 0.0F);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(i * 10.0F));
                        }
                    }

                    matrices.translate(0.0F, 0.0F, -1.0F);
                    matrices.translate(-0.45 * i, 0.45, 1.7);
                    matrices.translate(1.0F * sideFactor, 0.0 - equipProgress * 0.3, 0.3);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * sideFactor));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0F * sideFactor));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                    this.hmiAltSwing(matrices, arm, swingProgress, item);
                    float c = MathHelper.sin(equipProgress * 3.14F);
                    matrices.scale(0.9F, 0.9F, 0.9F);
                    this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
                    matrices.translate(-0.25 * i, 1.25, 0.05);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90 * i));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(77.0F));
                    matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(85 * i));
                    matrices.scale(1.2F, 1.2F, 1.2F);
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F));
                    matrices.translate(0.0, -0.15, 0.15);
                    this.renderItem(
                        player,
                        item,
                        bl32 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                        matrices,
                        orderedRenderCommandQueue,
                        light
                    );
                    matrices.pop();
                    if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                        float f = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                        float g = f / CrossbowItem.getPullTime(item, player);
                        if (g > 1.0F) {
                            g = 1.0F;
                        }

                        if (g > 0.1F) {
                            float h = MathHelper.sin((f - 0.1F) * 1.3F);
                            float j = g - 0.1F;
                            float k = h * j;
                            matrices.translate(k * 0.0F, k * 0.004F, k * 0.0F);
                        }

                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(g <= 0.2 ? 75.0F * g * 5.0F * i : 75 * i));
                        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(10.0F * g * 1.5F));
                        matrices.translate(-0.37 * i, 0.0, 0.6);
                        matrices.translate(0.15 * g * i, 0.0, 0.0);
                        this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm.getOpposite());
                    }
                } else {
                    boolean bl23 = arm == Arm.RIGHT;
                    int l = bl23 ? 1 : -1;
                    if (player.isUsingItem() && player.getItemUseTimeLeft() > 0 && player.getActiveHand() == hand) {
                        switch (HeldItemRendererHmiMixin$1.$SwitchMap$net$minecraft$item$consume$UseAction[item.getUseAction().ordinal()]) {
                            case 1:
                                this.applyEquipOffset(matrices, arm, equipProgress);
                                break;
                            case 2:
                            case 3:
                                float u = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                                float y = u / 5.0F;
                                if (y > 1.0F) {
                                    y = 1.0F;
                                }

                                float q = MathHelper.sin(u / 2.0F * 3.14F);
                                matrices.translate(1 * l, 0.1, 0.3);
                                matrices.translate(0.2 * l * y, -0.7 * y, -0.2 * y);
                                float var98;
                                matrices.translate(0.0, -0.2 * (var98 = q / 10.0F), -0.2 * var98);
                                matrices.translate(0.0, 0.1 * this.hmiEaseInOutBack(MathHelper.sin(y * 3.14F)), 0.0);
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * l));
                                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40 * l));
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                                this.hmiAltSwing(matrices, arm, swingProgress, item);
                                float c = MathHelper.sin(equipProgress * 3.14F);
                                matrices.scale(0.9F, 0.9F, 0.9F);
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0F * y * l));
                                this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, swingProgress, arm);
                                break;
                            case 4:
                                float k = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                                float s = k / 4.0F;
                                float s2 = k / 6.0F;
                                if (s > 1.0F) {
                                    s = 1.0F;
                                }

                                if (s2 > 1.0F) {
                                    s2 = 1.0F;
                                }

                                matrices.translate(0.0, -0.2, 0.0);
                                matrices.translate(1 * l, 0.0, 0.3);
                                matrices.translate(0.7 * s * l, 0.0, -1.3 * s);
                                matrices.translate(-0.2 * l * s2, 0.0, 0.0);
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float)(10.0 * Math.sin(s2 * 3.14))));
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(70.0F * s * l));
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * l));
                                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40 * l));
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(5 * l * s));
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-10.0F * s));
                                matrices.translate(0.0, 0.0, -0.2 * s);
                                this.hmiAltSwing(matrices, arm, swingProgress, item);
                                matrices.scale(0.9F, 0.9F, 0.9F);
                                this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, swingProgress, arm);
                                matrices.translate(0.35 * l, -0.13, -0.12);
                                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(10.0F * l));
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0F * l));
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(0.0F));
                                matrices.translate(-0.2 * l, -0.04, 0.15);
                                matrices.scale(1.0F, 1.0F, 1.0F);
                                break;
                            case 5:
                                matrices.push();
                                if (player.getMainArm() == Arm.LEFT) {
                                    bl2 = !bl2;
                                }

                                float m1 = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                                float f1 = m1 / 20.0F;
                                float f = (f1 * f1 + f1 * 2.0F) / 3.0F;
                                if (f1 > 1.0F) {
                                    f1 = 1.0F;
                                }

                                if (f1 > 0.1F) {
                                    float g1 = MathHelper.sin((m1 - 0.1F) * 1.3F);
                                    float j1 = g1 * f1;
                                    matrices.translate(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
                                }

                                matrices.translate(bl2 ? -0.1 : 0.1, 0.0, f1 * 0.15);
                                this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm);
                                matrices.pop();
                                matrices.translate(bl2 ? -0.5 : 0.5, -0.45, 0.1);
                                matrices.multiply(RotationAxis.POSITIVE_X.rotation(0.3F));
                                if (bl2) {
                                    matrices.multiply(RotationAxis.NEGATIVE_Z.rotation(-0.3F));
                                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(1.0F));
                                } else {
                                    matrices.multiply(RotationAxis.POSITIVE_Z.rotation(-0.3F));
                                    matrices.multiply(RotationAxis.POSITIVE_Y.rotation(1.0F));
                                }

                                this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm.getOpposite());
                                if (bl2) {
                                    matrices.multiply(RotationAxis.NEGATIVE_Y.rotation(2.5F));
                                } else {
                                    matrices.multiply(RotationAxis.POSITIVE_Y.rotation(2.5F));
                                }

                                matrices.translate(bl2 ? -0.65 : 0.65, -0.35, 0.27);
                                if (f1 > 1.0F) {
                                    f1 = 1.0F;
                                }

                                matrices.pop();
                                if (config.mb3DCompat.isValue()) {
                                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10 * l));
                                }

                                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(75.0F));
                                matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(-15 * l));
                                matrices.translate(0.8 * l, 0.0F - equipProgress * 0.3F, -0.1);
                                if (f > 0.1F) {
                                    float g1 = MathHelper.sin((m1 - 0.1F) * 1.3F);
                                    float h1 = f1 - 0.1F;
                                    float j1 = g1 * h1;
                                    matrices.translate(j1 * 0.0F, j1 * 0.004F, j1 * 0.0F);
                                }

                                matrices.push();
                                break;
                            case 6:
                                if (player.getOffHandStack().isEmpty() && !player.isCrawling() && !player.isSwimming() && !player.isClimbing()) {
                                    matrices.push();
                                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-25 * l));
                                    matrices.translate(-0.15 * l, 0.1, 0.1);
                                    this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm.getOpposite());
                                    matrices.pop();
                                }

                                float m;
                                if ((this.f = (m = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F)) / 10.0F) > 1.0F) {
                                    this.f = 1.0F;
                                }

                                if (this.f > 0.1F) {
                                    float g = MathHelper.sin((m - 0.1F) * 1.3F);
                                    this.h = this.f - 0.1F;
                                    float j = g * this.h;
                                    matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                                }

                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F));
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25 * l));
                                matrices.translate(0.2 * l, 0.0, 0.8);
                                this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm);
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(135.0F));
                                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-65 * l));
                                matrices.translate(0.65F * l, -1.0, -0.6);
                                break;
                            case 7:
                                float f5 = player.getItemUseTimeLeft() % 10;
                                float g5 = f5 - tickDelta + 1.0F;
                                float h5 = 1.0F - g5 / 10.0F;
                                float n2 = -15.0F + 75.0F * MathHelper.cos(h5 * 2.0F * (float) Math.PI);
                                float z = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                                float x = z / 4.0F;
                                if (x > 1.0F) {
                                    x = 1.0F;
                                }

                                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25 * l * x));
                                matrices.translate(0.3F * l * x, 0.3 * x, 0.1 * x);
                                if (x == 1.0F) {
                                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(n2 / 20.0F));
                                }

                                this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm);
                                break;
                            case 8:
                                matrices.translate(1 * l, 0.0 - equipProgress * 0.3, 0.3);
                                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * l));
                                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40 * l));
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                                this.hmiAltSwing(matrices, arm, swingProgress, item);
                                matrices.scale(0.9F, 0.9F, 0.9F);
                                this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
                        }
                    } else if (player.isUsingRiptide() && item.getUseAction() == UseAction.SPEAR) {
                        this.hmi$riptideCounter = (float)(this.hmi$riptideCounter + 0.15 * tt);
                        float m = item.getMaxUseTime(player) - (player.getItemUseTimeLeft() - tickDelta + 1.0F);
                        this.f = m / 10.0F;
                        if (this.f > 1.0F) {
                            this.f = 1.0F;
                        }

                        if (this.f > 0.1F) {
                            float g = MathHelper.sin((m - 0.1F) * 1.3F);
                            this.h = this.f - 0.1F;
                            float j = g * this.h;
                            matrices.translate(j * 0.0F, j * 0.004F, j * 0.0F);
                        }

                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F - this.hmi$riptideCounter * 2.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25 * l));
                        matrices.translate(0.2 * l, 0.0, 0.75);
                        matrices.translate(0.0, 0.0, 0.01 * MathHelper.sin(this.hmi$riptideCounter * 6.28F));
                        this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, equipProgress, swingProgress, arm);
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(135.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-65 * l));
                        matrices.translate(0.65F * l, -1.0, -0.6);
                    } else {
                        this.hmi$riptideCounter = 0.0F;
                        if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN) && !item.isIn(ItemTags.HANGING_SIGNS)) {
                            if (item.getUseAction() == UseAction.BLOCK) {
                                matrices.translate(0.0, -0.2, 0.0);
                            }
                        } else {
                            matrices.translate(0.1 * l, 0.0, -0.1);
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));
                        }

                        matrices.translate(1 * l, 0.0 - equipProgress * 0.3, 0.3);
                        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45 * l));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40 * l));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F));
                        this.hmiAltSwing(matrices, arm, swingProgress, item);
                        matrices.scale(0.9F, 0.9F, 0.9F);
                        this.renderArmHoldingItem(matrices, orderedRenderCommandQueue, light, 0.0F, 0.0F, arm);
                    }

                    matrices.translate(-0.3 * l, 0.65, -0.1);
                    matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-65 * l));
                    matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0F));
                    if (item.isIn(ItemTags.WOOL_CARPETS)) {
                        matrices.translate(0.2 * l, -0.1, 0.0);
                    }

                    if (Block.getBlockFromItem(item.getItem()) == Blocks.AIR || item.getUseAction() == UseAction.EAT || item.isIn(ConventionalItemTags.BUCKETS)
                        )
                     {
                        if ((
                                !item.isIn(ConventionalItemTags.TOOLS)
                                    || item.isIn(ItemTags.TRIMMABLE_ARMOR)
                                    || item.isIn(ItemTags.BOOKSHELF_BOOKS)
                                    || item.getUseAction() == UseAction.EAT
                                    || !item.isEnchantable()
                            )
                            && item.getUseAction() != UseAction.BOW
                            && item.getUseAction() != UseAction.SPYGLASS
                            && this.hmiGetAttackDamage(item) == 0.0F
                            && item.getUseAction() != UseAction.BLOCK
                            && !item.isOf(Items.WARPED_FUNGUS_ON_A_STICK)
                            && !item.isOf(Items.CARROT_ON_A_STICK)
                            && !item.isOf(Items.FISHING_ROD)
                            && !item.isOf(Items.SHEARS)
                            && !item.isIn(ItemTags.HOES)
                            && !config.mb3DCompat.isValue()) {
                            if (item.getUseAction() == UseAction.BRUSH) {
                                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(25.0F));
                                matrices.translate(bl2 ? 0.0 : 0.35, bl2 ? 0.0 : 0.25, bl2 ? 0.0 : 0.37);
                                if (!bl2) {
                                    matrices.scale(0.75F, 0.75F, 0.75F);
                                }

                                matrices.multiply(RotationAxis.NEGATIVE_Z.rotationDegrees(-75 * l));
                                matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(35.0F));
                                matrices.translate(bl2 ? -0.05 : 0.85, bl2 ? 0.0 : 0.05, bl2 ? 0.08 : -0.2);
                            } else {
                                matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(5 * l));
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F));
                                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l));
                                matrices.translate(0.0, -0.05, -0.1);
                                matrices.scale(0.7F, 0.7F, 0.7F);
                            }

                            if (item.isOf(Items.FEATHER) || item.isOf(Items.SLIME_BALL) || item.isOf(Items.PUFFERFISH)) {
                                this.hmi$vertVelocityYSlime = (float)(this.hmi$vertVelocityYSlime + swingProgress * 0.03 * (float)tt);
                                if ((
                                        player.getVelocity().length() > 0.09 && player.isOnGround()
                                            || player.isSwimming()
                                            || player.isCrawling()
                                            || player.isClimbing() && !player.isOnGround()
                                    )
                                    && this.client.options.getBobView().getValue()) {
                                    Random random = new Random();
                                    boolean randomBoolean = random.nextBoolean();
                                    this.hmi$vertVelocityYSlime = this.hmi$vertVelocityYSlime + (float)(-0.05 * player.getVelocity().length() * (float)tt);
                                }

                                matrices.scale(1.0F, 1.0F + this.hmi$vertAngleYSlime * -2.0F, 1.0F);
                            }
                        } else if (item.getUseAction() == UseAction.BLOCK && item.getUseAction() != UseAction.SPEAR) {
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(160 * l));
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-60 * l));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0F));
                            matrices.scale(0.75F, 0.75F, 0.75F);
                            matrices.translate(0.15 * l, bl2 ? 0.35 : 0.45, bl2 ? -0.15 : -0.1);
                            matrices.translate(0.17 * l, 0.0, 0.3);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90 * l));
                        } else if (item.getUseAction() == UseAction.SPEAR) {
                            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(75 * l));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45 * l));
                            matrices.translate(-0.3F * l, 0.0F, 0.0F);
                        } else {
                            matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(75 * l));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70.0F));
                            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45 * l));
                        }

                        if (item.getUseAction() != UseAction.BLOCK) {
                            matrices.scale(1.2F, 1.2F, 1.2F);
                        }

                        if (item.getUseAction() == UseAction.BOW && !player.isUsingItem()) {
                            matrices.translate(-0.1 * l, -0.2, 0.0);
                        }

                        if (item.isOf(Items.MACE)) {
                            if (config.mb3DCompat.isValue()) {
                                matrices.translate(-0.08, 0.17, 0.0);
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(40.0F));
                            }

                            matrices.translate(0.1 * l, 0.0, 0.0);
                            matrices.scale(0.9F, 0.9F, 0.9F);
                        }
                    } else if (item.getName().toString().toLowerCase().contains("TORCH".toLowerCase())) {
                        matrices.scale(1.5F, 1.5F, 1.5F);
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(25 * l));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l));
                        matrices.translate(0.2 * l, 0.2, 0.05);
                    } else if ((
                            item.isOf(Items.STRING)
                                || item.isOf(Items.REDSTONE)
                                || item.isOf(Items.LEVER)
                                || item.isOf(Items.TRIPWIRE_HOOK)
                                || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(ConventionalBlockTags.GLASS_PANES)
                                || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.RAILS)
                                || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.CLIMBABLE)
                                || item.isIn(ItemTags.DOORS)
                        )
                        && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.LEAVES)
                        && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)
                        && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.BANNERS)) {
                        matrices.translate(0.0, 0.0, -0.1);
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(5 * l));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l));
                    } else if (!item.isOf(Items.LANTERN) && !item.isOf(Items.SOUL_LANTERN) && !item.isIn(ItemTags.HANGING_SIGNS)) {
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(25 * l));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0F));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l));
                        matrices.translate(0.2 * l, 0.2, 0.05);
                        if (Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.BANNERS)) {
                            matrices.translate(-0.2 * l, 0.0, 0.0);
                            matrices.scale(1.1F, 1.1F, 1.1F);
                        }
                    } else {
                        float yawDelta = player.getYaw() - player.getHeadYaw();
                        float pitchDelta = player.getPitch() - player.getPitch();
                        this.hmi$swingVelocityY += yawDelta * 0.015F * (float)tt;
                        this.hmi$swingVelocityY += swingProgress * 2.0F * (float)tt;
                        this.hmi$swingVelocityX += pitchDelta * 0.015F * (float)tt;
                        this.hmi$swingVelocityY = this.hmi$swingVelocityY - 0.1F * this.hmi$swingAngleY * (float)tt;
                        this.hmi$swingVelocityX = this.hmi$swingVelocityX - 0.1F * this.hmi$swingAngleX * (float)tt;
                        this.hmi$swingVelocityY = (float)(this.hmi$swingVelocityY * Math.pow(0.88F, tt));
                        this.hmi$swingVelocityX = (float)(this.hmi$swingVelocityX * Math.pow(0.88F, tt));
                        this.hmi$swingAngleY = this.hmi$swingAngleY + this.hmi$swingVelocityY * (float)tt;
                        this.hmi$swingAngleX = this.hmi$swingAngleX + this.hmi$swingVelocityX * (float)tt;
                        double currentSpeed = player.getVelocity().length();
                        this.hmi$swingVelocityZ = (float)(
                            this.hmi$swingVelocityZ
                                + (
                                    bl2
                                        ? (currentSpeed * -1.0 * 15.0 - this.hmi$swingVelocityZ) * 0.1F * tt
                                        : (currentSpeed * 15.0 - this.hmi$swingVelocityZ) * 0.1F * tt
                                )
                        );
                        if ((currentSpeed > 0.09 && player.isOnGround() || player.isSwimming() || player.isClimbing() && !player.isOnGround())
                            && this.client.options.getBobView().getValue()) {
                            Random random = new Random();
                            boolean randomBoolean = random.nextBoolean();
                            this.hmi$swingVelocityY += (float)(randomBoolean ? -5.5 * currentSpeed * tt : 5.5 * currentSpeed * tt);
                        }

                        matrices.translate(0.0, 0.0, -0.1);
                        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(35 * l + this.hmi$swingAngleY));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0F + this.hmi$swingAngleX));
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75 * l + this.hmi$swingVelocityZ));
                        if (item.isIn(ItemTags.HANGING_SIGNS)) {
                            matrices.translate(0.0, -0.1, 0.0);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-45 * l));
                        }

                        matrices.translate(0.3 * l, -0.35, 0.0);
                        matrices.translate(0.0, 0.0, 0.1);
                        matrices.scale(1.5F, 1.5F, 1.5F);
                    }

                    if (item.getItem() instanceof BlockItem
                        && (
                            !item.isIn(ConventionalItemTags.BUCKETS)
                                    && item.getUseAction() != UseAction.EAT
                                    && !item.isIn(ItemTags.BANNERS)
                                    && !item.isOf(Items.STRING)
                                    && !item.isOf(Items.REDSTONE)
                                    && !item.isOf(Items.LEVER)
                                    && !item.isOf(Items.TRIPWIRE_HOOK)
                                    && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(ConventionalBlockTags.GLASS_PANES)
                                    && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.RAILS)
                                    && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.CLIMBABLE)
                                    && !item.isIn(ItemTags.DOORS)
                                || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.LEAVES)
                        )
                        && !Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.COMBINATION_STEP_SOUND_BLOCKS)) {
                        BlockItem blockItem = (BlockItem)item.getItem();
                        BlockRenderManager blockRenderManager = MinecraftClient.getInstance().getBlockRenderManager();
                        blockRenderManager.getModel(blockItem.getBlock().getDefaultState());
                        matrices.push();
                        if (!bl23) {
                            matrices.translate(-0.4F, 0.0F, 0.0F);
                        }

                        matrices.scale(0.4F, 0.4F, 0.4F);
                        matrices.translate(-0.9 * l, -0.45, -0.5);
                        if (Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.BUTTONS)) {
                            matrices.translate(0.2 * l, -0.15, -0.2);
                        }

                        if (Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.PRESSURE_PLATES)) {
                            matrices.translate(0.0, 0.1, 0.0);
                        }

                        if (item.isOf(Items.SLIME_BLOCK)
                            || item.isOf(Items.HONEY_BLOCK)
                            || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.FLOWERS)
                            || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.LEAVES)
                            || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.SAPLINGS)
                            || Block.getBlockFromItem(item.getItem()).getDefaultState().isIn(BlockTags.SWORD_EFFICIENT)) {
                            this.hmi$vertVelocityYSlime = (float)(this.hmi$vertVelocityYSlime + swingProgress * 0.03 * (float)tt);
                            if ((
                                    player.getVelocity().length() > 0.09 && player.isOnGround()
                                        || player.isSwimming()
                                        || player.isCrawling()
                                        || player.isClimbing() && !player.isOnGround()
                                )
                                && this.client.options.getBobView().getValue()) {
                                Random random = new Random();
                                boolean randomBoolean = random.nextBoolean();
                                this.hmi$vertVelocityYSlime = this.hmi$vertVelocityYSlime + (float)(-0.05 * player.getVelocity().length() * (float)tt);
                            }

                            matrices.scale(1.0F, 1.0F + this.hmi$vertAngleYSlime * -2.0F, 1.0F);
                        }

                        BlockState blockState = blockItem.getBlock().getDefaultState();
                        if (player.age - this.hmi$prevAge >= 100.0F) {
                            this.hmi$repPower = !this.hmi$repPower;
                            this.hmi$prevAge = player.age;
                        }

                        if (blockItem.getBlock() == Blocks.REPEATER && this.hmi$repPower) {
                            blockState = blockState.with(RepeaterBlock.POWERED, true);
                        }

                        if (blockItem.getBlock() == Blocks.COMPARATOR && this.hmi$repPower) {
                            blockState = blockState.with(ComparatorBlock.POWERED, true);
                        }

                        if (blockItem.getBlock() == Blocks.REDSTONE_TORCH && player.isSubmergedInWater()) {
                            blockState = blockState.with(RedstoneTorchBlock.LIT, false);
                        }

                        if ((blockItem.getBlock() == Blocks.CAMPFIRE || blockItem.getBlock() == Blocks.SOUL_CAMPFIRE) && player.isSubmergedInWater()) {
                            blockState = blockState.with(CampfireBlock.LIT, false);
                        }

                        if (item.isIn(ItemTags.BEDS)) {
                            if (bl2) {
                                matrices.translate(0.9, 0.0, 0.8);
                            }

                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 * l));
                        }

                        blockRenderManager.renderBlockAsEntity(
                            blockState,
                            matrices,
                            MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(),
                            light,
                            OverlayTexture.DEFAULT_UV
                        );
                        matrices.pop();
                    } else {
                        if (item.isIn(ConventionalItemTags.TOOLS)
                                && !item.isIn(ItemTags.TRIMMABLE_ARMOR)
                                && !item.isIn(ItemTags.BOOKSHELF_BOOKS)
                                && item.getUseAction() != UseAction.EAT
                                && item.isEnchantable()
                            || item.getUseAction() == UseAction.BOW
                            || item.getUseAction() == UseAction.SPYGLASS
                            || this.hmiGetAttackDamage(item) != 0.0F
                            || item.getUseAction() == UseAction.BLOCK
                            || item.isOf(Items.WARPED_FUNGUS_ON_A_STICK)
                            || item.isOf(Items.CARROT_ON_A_STICK)
                            || item.isOf(Items.FISHING_ROD)
                            || item.isOf(Items.SHEARS)) {
                            if (item.isIn(ItemTags.SWORDS) && !sharpSword) {
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-60.0F * swing));
                                matrices.translate(0.0, 0.1 * swing, -0.1 * swing);
                            }

                            if (item.isIn(ItemTags.SHOVELS)) {
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-80.0F * swing_rot));
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0F * swing));
                            } else if (item.getUseAction() == UseAction.SPEAR) {
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-40.0F * swing_rot));
                                matrices.translate(0.0, 0.1 * swing_rot, -0.1 * swing_rot);
                            } else if (item.getUseAction() != UseAction.BLOCK) {
                                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-25.0F * swing));
                                matrices.translate(0.0, 0.05 * swing, -0.05 * swing);
                            }
                        }

                        if (item.isOf(Items.NETHER_STAR) || item.isOf(Items.END_CRYSTAL) && config.mb3DCompat.isValue()) {
                            this.hmi$netherCounter = (float)(this.hmi$netherCounter + 0.9 * tt);
                            matrices.translate(0.0, 0.25 + 0.02 * MathHelper.sin(this.hmi$netherCounter * 0.1F), 0.0);
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(3.0F * MathHelper.sin(this.hmi$netherCounter * 0.2F)));
                            matrices.scale(
                                1.0F + 0.01F * MathHelper.sin(this.hmi$netherCounter),
                                1.0F + 0.01F * MathHelper.sin(this.hmi$netherCounter),
                                1.0F + 0.01F * MathHelper.sin(this.hmi$netherCounter)
                            );
                        } else {
                            this.hmi$netherCounter = 0.0F;
                        }

                        if (config.mb3DCompat.isValue()) {
                            if (item.isIn(ItemTags.SWORDS)) {
                                matrices.translate(0.0, 0.2, 0.0);
                            }

                            if (item.isOf(Items.FEATHER) || item.isOf(Items.SLIME_BALL) || item.isOf(Items.PUFFERFISH)) {
                                this.hmi$vertVelocityYSlime = (float)(this.hmi$vertVelocityYSlime + swingProgress * 0.03 * (float)tt);
                                if ((
                                        player.getVelocity().length() > 0.09 && player.isOnGround()
                                            || player.isSwimming()
                                            || player.isCrawling()
                                            || player.isClimbing() && !player.isOnGround()
                                    )
                                    && this.client.options.getBobView().getValue()) {
                                    Random random = new Random();
                                    boolean randomBoolean = random.nextBoolean();
                                    this.hmi$vertVelocityYSlime = this.hmi$vertVelocityYSlime + (float)(-0.05 * player.getVelocity().length() * (float)tt);
                                }

                                matrices.scale(1.0F, 1.0F + this.hmi$vertAngleYSlime * -2.0F, 1.0F);
                            }
                        }

                        if (item.isIn(ItemTags.SHOVELS)) {
                            matrices.translate(0.07 * l, 0.0, 0.05);
                            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90 * l));
                            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-15.0F));
                        }

                        if (item.isOf(Items.TORCH)) {
                        }

                        this.renderItem(
                            player,
                            item,
                            bl23 ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                            matrices,
                            orderedRenderCommandQueue,
                            light
                        );
                    }
                }

                matrices.pop();
                matrices.pop();
                this.hmi$isAttacking = this.client.options.attackKey.isPressed();
            }
        }
    }

    @Shadow
    protected abstract void renderFirstPersonItem(
        AbstractClientPlayerEntity var1,
        float var2,
        float var3,
        Hand var4,
        float var5,
        ItemStack var6,
        float var7,
        MatrixStack var8,
        OrderedRenderCommandQueue var9,
        int var10
    );
}
