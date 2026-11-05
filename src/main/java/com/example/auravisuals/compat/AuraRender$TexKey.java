package com.example.auravisuals.compat;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import java.util.Objects;
import net.minecraft.util.Identifier;

public final class AuraRender$TexKey {
    RenderPipeline pipeline;
    Identifier texture;

    public AuraRender$TexKey(RenderPipeline pipeline, Identifier texture) {
        this.pipeline = pipeline;
        this.texture = texture;
    }

    public RenderPipeline pipeline() {
        return this.pipeline;
    }

    public Identifier texture() {
        return this.texture;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else {
            return !(o instanceof AuraRender$TexKey other)
                ? false
                : Objects.equals(this.pipeline, other.pipeline) && Objects.equals(this.texture, other.texture);
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.pipeline, this.texture);
    }

    @Override
    public String toString() {
        return "AuraRender$TexKey[pipeline=" + this.pipeline + ", texture=" + this.texture + "]";
    }
}
