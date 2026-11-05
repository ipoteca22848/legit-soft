package com.example.auravisuals.mixin.accessor;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
    @Accessor("x")
    int auravisuals$getGuiLeft();

    @Accessor("y")
    int auravisuals$getGuiTop();
}
