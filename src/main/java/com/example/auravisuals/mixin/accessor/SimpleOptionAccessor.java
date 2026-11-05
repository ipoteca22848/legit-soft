package com.example.auravisuals.mixin.accessor;

import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SimpleOption.class)
public interface SimpleOptionAccessor {
    @Accessor("value")
    Object getRawValue();

    @Accessor("value")
    void setRawValue(Object var1);
}
