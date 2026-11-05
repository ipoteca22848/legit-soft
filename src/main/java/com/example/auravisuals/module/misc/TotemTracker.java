package com.example.auravisuals.module.misc;

import com.example.auravisuals.client.gui.hud.Notifications;
import com.example.auravisuals.command.CommandManager;
import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.module.render.Particles;
import com.example.auravisuals.util.item.TotemUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class TotemTracker extends Module {
    public TotemTracker() {
        super("TotemTracker", "Отслеживает потерю тотема игроками", Category.MISC);
    }

    public void onTotemPop(Entity entity) {
        if (this.mc.player != null && this.mc.world != null) {
            if (entity instanceof PlayerEntity player) {
                if (this.isInFieldOfView(player)) {
                    boolean enchanted = false;
                    ItemStack main = player.getMainHandStack();
                    ItemStack off = player.getOffHandStack();
                    if (TotemUtil.isEnchantedTotem(main)) {
                        enchanted = true;
                    } else if (TotemUtil.isEnchantedTotem(off)) {
                        enchanted = true;
                    }

                    CommandManager.addChatMessage("Игрок " + player.getName().getString() + " потерял тотем. Зачарован: " + (enchanted ? "да" : "нет"));
                    Notifications.postMessage("Тотем: " + player.getName().getString() + (enchanted ? " (зачар.)" : " (не зачар.)"));
                    Particles particles = ModuleManager.INSTANCE.getModule(Particles.class);
                    if (particles != null && particles.isEnabled()) {
                        particles.spawnTotemParticles(player);
                    }
                }
            }
        }
    }

    private boolean isInFieldOfView(PlayerEntity player) {
        if (player == this.mc.player) {
            return true;
        }

        Vec3d eyePos = this.mc.player.getCameraPosVec(1.0F);
        Vec3d targetPos = player.getBoundingBox().getCenter();
        Vec3d toTarget = targetPos.subtract(eyePos);
        double lengthSquared = toTarget.lengthSquared();
        if (lengthSquared < 1.0E-4) {
            return true;
        }

        Vec3d look = this.mc.player.getRotationVec(1.0F).normalize();
        Vec3d direction = toTarget.normalize();
        double dot = look.dotProduct(direction);
        double fov = 90.0;

        try {
            fov = this.mc.options.getFov().getValue().intValue();
        } catch (Throwable var14) {
        }

        fov = MathHelper.clamp(fov + 20.0, 30.0, 170.0);
        return dot >= Math.cos(Math.toRadians(fov * 0.5));
    }
}
