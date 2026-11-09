package com.anvil.client.atmosphere;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourcePackManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public final class AtmospherePackManager {
    public static final String sub_1 = "anvil:atmosphere_winter";
    private static CompletableFuture<Void> sub_2;
    private static int sub_3;

    private AtmospherePackManager() {
    }

    public static void init() {
        ll1i_i_iIIi1i1liil1iii_li_Il();
    }

    public static void ll1i_i_iIIi1i1liil1iii_li_Il() {
        ModContainer var0 = (ModContainer)FabricLoader.getInstance().getModContainer("anvil").orElseThrow();
        if (!ResourceLoader.registerBuiltinPack(
            Identifier.of("anvil:atmosphere_winter"), var0, Text.literal("anvil visual — зимняя атмосфера"), PackActivationType.NORMAL
        )) {
            throw new IllegalStateException("Atmosphere winter resources are missing");
        }
    }

    public static void sub_4(MinecraftClient var0) {
        AtmosphereModule var1 = AtmosphereModule.sub_5();
        boolean var2 = var1 != null && var1.usesWinterTextures();
        ResourcePackManager var3 = var0.getResourcePackManager();
        if (var3.getEnabledIds().contains("anvil:atmosphere_winter") == var2) {
            sub_3 = 0;
        } else if (++sub_3 >= 4 && var0.getOverlay() == null && (sub_2 == null || sub_2.isDone())) {
            if (var2 && var3.getProfile("anvil:atmosphere_winter") == null) {
                var3.scanPacks();
            }

            ArrayList var4 = new ArrayList(var3.getEnabledIds());
            var4.remove("anvil:atmosphere_winter");
            if (var2) {
                var4.add("anvil:atmosphere_winter");
            }

            var3.setEnabledProfiles(var4);
            sub_3 = 0;
            sub_2 = var0.reloadResources();
        }
    }}
