package com.example.auravisuals.mixin.client;

import com.example.auravisuals.module.misc.ServerPackUnlocker;
import com.google.common.hash.HashCode;
import java.net.URL;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.client.resource.server.PackStateChangeCallback;
import net.minecraft.client.resource.server.ServerResourcePackManager;
import net.minecraft.client.resource.server.PackStateChangeCallback.FinishState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerResourcePackManager.class)
public class ServerResourcePackManagerMixin {
    @Shadow
    @Final
    PackStateChangeCallback stateChangeCallback;

    @Inject(method = "addResourcePack(Ljava/util/UUID;Ljava/net/URL;Lcom/google/common/hash/HashCode;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void auravisuals$blockRemoteServerPack(UUID id, URL url, HashCode hashCode, CallbackInfo ci) {
        if (ServerPackUnlocker.shouldBlockServerPacks()) {
            this.stateChangeCallback.onFinish(id, FinishState.DECLINED);
            ci.cancel();
        }
    }

    @Inject(method = "addResourcePack(Ljava/util/UUID;Ljava/nio/file/Path;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void auravisuals$blockLocalServerPack(UUID id, Path path, CallbackInfo ci) {
        if (ServerPackUnlocker.shouldBlockServerPacks()) {
            this.stateChangeCallback.onFinish(id, FinishState.DECLINED);
            ci.cancel();
        }
    }
}
