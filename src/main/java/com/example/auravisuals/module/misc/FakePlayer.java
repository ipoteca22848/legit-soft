package com.example.auravisuals.module.misc;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.StringSetting;
import com.mojang.authlib.GameProfile;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.Entity.RemovalReason;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class FakePlayer extends Module {
    public static final BooleanSetting armor = new BooleanSetting("В броне", true);
    public static final BooleanSetting totem = new BooleanSetting("С тотемом", true);
    public static final StringSetting name = new StringSetting("Введите имя", "FakePlayer");
    private OtherClientPlayerEntity fakePlayer;
    private boolean wasSwinging = false;
    private String lastName = "FakePlayer";

    public FakePlayer() {
        super("FakePlayer", "Спавнит фейкового игрока", Category.MISC);
        this.addSettings(armor, totem, name);
    }

    public OtherClientPlayerEntity getFakePlayer() {
        return this.fakePlayer;
    }

    @Override
    public void onEnable() {
        if (this.mc.world != null && this.mc.player != null) {
            this.spawnFakePlayer();
        } else {
            this.setEnabled(false);
        }
    }

    private void updateEquipment() {
        if (this.fakePlayer != null) {
            if (armor.getValue()) {
                this.fakePlayer.equipStack(EquipmentSlot.HEAD, new ItemStack(Items.NETHERITE_HELMET));
                this.fakePlayer.equipStack(EquipmentSlot.CHEST, new ItemStack(Items.NETHERITE_CHESTPLATE));
                this.fakePlayer.equipStack(EquipmentSlot.LEGS, new ItemStack(Items.NETHERITE_LEGGINGS));
                this.fakePlayer.equipStack(EquipmentSlot.FEET, new ItemStack(Items.NETHERITE_BOOTS));
                this.fakePlayer.equipStack(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
            } else {
                this.fakePlayer.equipStack(EquipmentSlot.HEAD, ItemStack.EMPTY);
                this.fakePlayer.equipStack(EquipmentSlot.CHEST, ItemStack.EMPTY);
                this.fakePlayer.equipStack(EquipmentSlot.LEGS, ItemStack.EMPTY);
                this.fakePlayer.equipStack(EquipmentSlot.FEET, ItemStack.EMPTY);
                this.fakePlayer.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }

            if (totem.getValue()) {
                this.fakePlayer.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            } else {
                this.fakePlayer.setStackInHand(Hand.OFF_HAND, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void onDisable() {
        if (this.fakePlayer != null && this.mc.world != null) {
            this.mc.world.removeEntity(this.fakePlayer.getId(), RemovalReason.DISCARDED);
            this.fakePlayer = null;
        }
    }

    @Override
    public void onTick() {
        if (this.mc.world != null && this.mc.player != null) {
            if (this.fakePlayer != null && !this.fakePlayer.isAlive()) {
            }

            if (this.fakePlayer != null) {
                String desiredName = name.getValue();
                if (desiredName == null || desiredName.isBlank()) {
                    desiredName = "FakePlayer";
                }

                if (!desiredName.equals(this.lastName)) {
                    this.respawnFakePlayer(desiredName);
                }
            }

            this.updateEquipment();
            if (this.mc.player.handSwinging && !this.wasSwinging) {
                HitResult class_2392 = this.mc.crosshairTarget;
                EntityHitResult hitResult;
                if (this.mc.crosshairTarget instanceof EntityHitResult && (hitResult = (EntityHitResult)class_2392).getEntity() == this.fakePlayer) {
                    this.handleHit();
                }
            }

            this.wasSwinging = this.mc.player.handSwinging;
        } else {
            this.setEnabled(false);
        }
    }

    private void handleHit() {
        try {
            this.fakePlayer.handleStatus((byte)2);
            this.mc
                .world
                .playSound(
                    null,
                    this.fakePlayer.getX(),
                    this.fakePlayer.getY(),
                    this.fakePlayer.getZ(),
                    SoundEvents.ENTITY_PLAYER_HURT,
                    SoundCategory.PLAYERS,
                    1.0F,
                    1.0F
                );
            if (totem.getValue()) {
                this.mc.particleManager.addEmitter(this.fakePlayer, ParticleTypes.TOTEM_OF_UNDYING, 30);
                this.mc
                    .world
                    .playSound(
                        null,
                        this.fakePlayer.getX(),
                        this.fakePlayer.getY(),
                        this.fakePlayer.getZ(),
                        SoundEvents.ITEM_TOTEM_USE,
                        SoundCategory.PLAYERS,
                        1.0F,
                        1.0F
                    );
                this.fakePlayer.handleStatus((byte)35);
                TotemTracker totemTracker = ModuleManager.INSTANCE.getModule(TotemTracker.class);
                if (totemTracker != null && totemTracker.isEnabled()) {
                    totemTracker.onTotemPop(this.fakePlayer);
                }

                this.fakePlayer.setStackInHand(Hand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void spawnFakePlayer() {
        try {
            String desiredName = name.getValue();
            if (desiredName == null || desiredName.isBlank()) {
                desiredName = "FakePlayer";
            }

            this.lastName = desiredName;
            this.fakePlayer = new OtherClientPlayerEntity(this.mc.world, new GameProfile(UUID.randomUUID(), desiredName));
            this.fakePlayer.setId(-10000 - new Random().nextInt(10000));
            this.fakePlayer.copyPositionAndRotation(this.mc.player);
            this.fakePlayer.setHeadYaw(this.mc.player.getHeadYaw());
            this.fakePlayer.setBodyYaw(this.mc.player.getBodyYaw());
            this.fakePlayer.getInventory().clone(this.mc.player.getInventory());
            this.fakePlayer.noClip = true;
            this.fakePlayer.setNoGravity(true);
            this.fakePlayer.setInvulnerable(true);
            this.updateEquipment();
            this.mc.world.addEntity(this.fakePlayer);
        } catch (Exception e) {
            e.printStackTrace();
            this.setEnabled(false);
        }
    }

    private void respawnFakePlayer(String desiredName) {
        try {
            if (this.fakePlayer != null && this.mc.world != null) {
                this.mc.world.removeEntity(this.fakePlayer.getId(), RemovalReason.DISCARDED);
            }

            this.lastName = desiredName;
            this.fakePlayer = new OtherClientPlayerEntity(this.mc.world, new GameProfile(UUID.randomUUID(), desiredName));
            this.fakePlayer.setId(-10000 - new Random().nextInt(10000));
            this.fakePlayer.copyPositionAndRotation(this.mc.player);
            this.fakePlayer.setHeadYaw(this.mc.player.getHeadYaw());
            this.fakePlayer.setBodyYaw(this.mc.player.getBodyYaw());
            this.fakePlayer.getInventory().clone(this.mc.player.getInventory());
            this.fakePlayer.noClip = true;
            this.fakePlayer.setNoGravity(true);
            this.fakePlayer.setInvulnerable(true);
            this.updateEquipment();
            this.mc.world.addEntity(this.fakePlayer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
