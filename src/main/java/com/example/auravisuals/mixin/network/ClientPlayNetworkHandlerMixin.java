package com.example.auravisuals.mixin.network;

import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.misc.NoCapsLock;
import com.example.auravisuals.module.misc.PickUpLogger;
import com.example.auravisuals.module.misc.PvPSafe;
import com.example.auravisuals.module.misc.TotemTracker;
import com.example.auravisuals.module.render.HitCounter;
import com.example.auravisuals.module.render.TrapTimer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityDamageS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    @ModifyVariable(method = "sendChatMessage", at = @At("HEAD"), argsOnly = true, require = 0)
    private String auravisuals$noCapsOutgoingChat(String message) {
        return NoCapsLock.normalizeOutgoingChat(message);
    }

    @Inject(method = "onEntityAnimation", at = @At("HEAD"), require = 0)
    private void auravisuals$hitCounterOnEntityAnimation(EntityAnimationS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.world != null) {
            int anim = packet.getAnimationId();
            if (anim == 0 || anim == 3) {
                if (mc.world.getEntityById(packet.getEntityId()) instanceof PlayerEntity swinger) {
                    HitCounter hitCounter = ModuleManager.INSTANCE.getModule(HitCounter.class);
                    if (hitCounter != null && hitCounter.isEnabled()) {
                        hitCounter.onNetworkArmSwing(swinger);
                    }
                }
            }
        }
    }

    @Inject(method = "onEntityDamage", at = @At("HEAD"), require = 0)
    private void auravisuals$hitCounterOnEntityDamage(EntityDamageS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null && mc.player != null) {
            HitCounter hitCounter = ModuleManager.INSTANCE.getModule(HitCounter.class);
            if (hitCounter != null && hitCounter.isEnabled()) {
                hitCounter.onEntityDamageS2C(packet, mc.world);
            }
        }
    }

    @Inject(method = "onEntityStatus", at = @At("TAIL"), require = 0)
    private void auravisuals$totemTrackerOnEntityStatus(EntityStatusS2CPacket packet, CallbackInfo ci) {
        if (packet.getStatus() == 35) {
            TotemTracker tracker = ModuleManager.INSTANCE.getModule(TotemTracker.class);
            if (tracker != null && tracker.isEnabled()) {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.world != null && mc.player != null) {
                    Entity entity = packet.getEntity(mc.world);
                    if (entity != null) {
                        tracker.onTotemPop(entity);
                    }
                }
            }
        }
    }

    @Inject(method = "onItemPickupAnimation", at = @At("HEAD"), require = 0)
    private void auravisuals$pickupLoggerOnPickup(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world != null && mc.player != null) {
            Entity collector = mc.world.getEntityById(packet.getCollectorEntityId());
            if (collector == mc.player) {
                if (mc.world.getEntityById(packet.getEntityId()) instanceof ItemEntity itemEntity) {
                    ItemStack stack = itemEntity.getStack();
                    if (!stack.isEmpty()) {
                        int pickedAmount = Math.min(packet.getStackAmount(), stack.getCount());
                        if (pickedAmount > 0) {
                            PickUpLogger logger = ModuleManager.INSTANCE.getModule(PickUpLogger.class);
                            if (logger != null && logger.isEnabled()) {
                                ItemStack pickedStack = stack.copy();
                                pickedStack.setCount(pickedAmount);
                                logger.handlePickup(pickedStack, packet.getEntityId(), pickedAmount);
                            }
                        }
                    }
                }
            }
        }
    }

    @Inject(method = "onPlaySound", at = @At("HEAD"), require = 0)
    private void auravisuals$structureTimerOnPlaySound(PlaySoundS2CPacket packet, CallbackInfo ci) {
        TrapTimer timer = ModuleManager.INSTANCE.getModule(TrapTimer.class);
        if (timer != null && timer.isEnabled()) {
            timer.onPlaySound(packet);
        }
    }

    @Inject(method = "onPlaySoundFromEntity", at = @At("HEAD"), require = 0)
    private void auravisuals$structureTimerOnPlaySoundFromEntity(PlaySoundFromEntityS2CPacket packet, CallbackInfo ci) {
        TrapTimer timer = ModuleManager.INSTANCE.getModule(TrapTimer.class);
        if (timer != null && timer.isEnabled()) {
            timer.onPlaySoundFromEntity(packet);
        }
    }

    @Inject(method = "sendChatCommand", at = @At("HEAD"), cancellable = true, require = 0)
    private void onSendChatCommand(String command, CallbackInfo ci) {
        PvPSafe pvpSafe = ModuleManager.INSTANCE.getModule(PvPSafe.class);
        if (pvpSafe != null && pvpSafe.shouldBlockCommand(command)) {
            MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(Text.literal("§c[PvPSafe] Команда заблокирована!"));
            ci.cancel();
        }
    }
}
