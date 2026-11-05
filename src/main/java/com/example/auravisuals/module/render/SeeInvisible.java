package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.util.policy.HolyServerPolicy;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

public final class SeeInvisible extends Module {
    public static final String MODULE_NAME = "SeeInvisible";

    public SeeInvisible() {
        super("SeeInvisible", "Показывает невидимых игроков полупрозрачными", Category.VISUALS);
    }

    public static boolean shouldReveal(PlayerEntity player) {
        if (player != null && player.hasStatusEffect(StatusEffects.INVISIBILITY)) {
            SeeInvisible module = ModuleManager.INSTANCE.getModule(SeeInvisible.class);
            return module != null && module.isEnabled() && HolyServerPolicy.isAllowedOnCurrentServer("SeeInvisible");
        } else {
            return false;
        }
    }
}
