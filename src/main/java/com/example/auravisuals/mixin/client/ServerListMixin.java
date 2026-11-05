package com.example.auravisuals.mixin.client;

import com.example.auravisuals.util.ServerPinHelper;
import java.util.List;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerList.class)
public abstract class ServerListMixin {
    @Shadow
    @Final
    private List<ServerInfo> servers;

    @Inject(method = "loadFile", at = @At("RETURN"))
    private void onLoadFile(CallbackInfo ci) {
        if (this.servers != null) {
            this.servers.removeIf(ServerPinHelper::isPinned);
            this.servers.add(0, ServerPinHelper.createPinnedServer());
        }
    }

    @Inject(method = "swapEntries", at = @At("HEAD"), cancellable = true)
    private void onSwapEntries(int index1, int index2, CallbackInfo ci) {
        if (index1 == 0 || index2 == 0) {
            ci.cancel();
        }
    }

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void onRemove(ServerInfo serverInfo, CallbackInfo ci) {
        if (ServerPinHelper.isPinned(serverInfo)) {
            ci.cancel();
        }
    }

    @Inject(method = "set", at = @At("HEAD"), cancellable = true)
    private void onSet(int index, ServerInfo serverInfo, CallbackInfo ci) {
        if (index == 0 && !ServerPinHelper.isPinned(serverInfo)) {
            ci.cancel();
        }
    }
}
