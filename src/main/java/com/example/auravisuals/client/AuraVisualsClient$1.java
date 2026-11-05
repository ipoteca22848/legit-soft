package com.example.auravisuals.client;

import com.example.auravisuals.client.gui.render.IconRender;
import com.example.auravisuals.client.gui.render.TextRender;
import com.example.auravisuals.cosmetics.CosmeticsManager;
import com.example.auravisuals.util.render.RenderUtil;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

public class AuraVisualsClient$1 implements SimpleSynchronousResourceReloadListener {
    AuraVisualsClient$1(AuraVisualsClient this$0) {
    }

    public Identifier getFabricId() {
        return Identifier.of("auravisuals", "reload_gui_icons");
    }

    public void reload(ResourceManager manager) {
        RenderUtil.onResourcesReloaded();
        IconRender.reload();
        TextRender.reload();
        CosmeticsManager.INSTANCE.load();
    }
}
