package com.anvil.smoke.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
@Mixin(CreateWorldScreen.class)
public interface CreateWorldInvoker {
 @Invoker("createLevel") void anvil$createLevel();
}
