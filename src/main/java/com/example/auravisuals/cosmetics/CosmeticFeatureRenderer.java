package com.example.auravisuals.cosmetics;

import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

public final class CosmeticFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    public CosmeticFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    public void render(
        MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float relativeHeadYaw, float headPitch
    ) {
        if (!com.anvil.client.AnvilRuntime.isActive()) return;
        if (state != null && !state.invisible) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.world != null) {
                Entity entity = client.world.getEntityById(state.id);
                PlayerEntity player;
                if (entity instanceof PlayerEntity && (player = (PlayerEntity)entity).isAlive()) {
                    CosmeticsManager manager = CosmeticsManager.INSTANCE;
                    Map equipped = manager.resolvePlayerCosmetics(player);
                    if (!equipped.isEmpty()) {
                        float animationTime = state.age * 0.05F;
                        CosmeticItem hat = (CosmeticItem)equipped.get("hats");
                        if (hat != null) {
                            matrices.push();
                            this.getContextModel().getRootPart().applyTransform(matrices);
                            this.getContextModel().head.applyTransform(matrices);
                            matrices.scale(-1.0F, -1.0F, 1.0F);
                            manager.renderFeatureItem(matrices, queue, hat, "hats", animationTime, light);
                            matrices.pop();
                        }

                        CosmeticItem wings = (CosmeticItem)equipped.get("wings");
                        CosmeticItem back = (CosmeticItem)equipped.get("back");
                        if (wings != null || back != null) {
                            matrices.push();
                            this.getContextModel().getRootPart().applyTransform(matrices);
                            this.getContextModel().body.applyTransform(matrices);
                            matrices.scale(-1.0F, -1.0F, 1.0F);
                            if (wings != null) {
                                matrices.push();
                                manager.renderFeatureItem(matrices, queue, wings, "wings", animationTime, light);
                                matrices.pop();
                            }

                            if (back != null) {
                                matrices.push();
                                manager.renderFeatureItem(matrices, queue, back, "back", animationTime, light);
                                matrices.pop();
                            }

                            matrices.pop();
                        }
                    }
                }
            }
        }
    }
}
