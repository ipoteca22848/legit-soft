package com.example.auravisuals.mixin.client;

import java.util.Optional;
import net.minecraft.util.profiling.jfr.FlightProfiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FlightProfiler.class)
public interface FlightProfilerJfrFixMixin {
    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Ljava/lang/ModuleLayer;findModule(Ljava/lang/String;)Ljava/util/Optional;"))
    private static Optional<Module> auravisuals$hideJfr(ModuleLayer layer, String name) {
        return "jdk.jfr".equals(name) ? Optional.empty() : layer.findModule(name);
    }
}
