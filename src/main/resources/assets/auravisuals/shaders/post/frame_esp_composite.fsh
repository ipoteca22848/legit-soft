#version 330

layout(std140) uniform GlowParams {
    // x = glow size (1..10), y = reserved, z = reserved, w = reserved
    vec4 uGlow;
};

uniform sampler2D CoreSampler;
uniform sampler2D GlowSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 core = texture(CoreSampler, texCoord);
    vec4 glow = texture(GlowSampler, texCoord);

    float coreAlpha = clamp(core.a, 0.0, 1.0);
    float glowAlpha = clamp(glow.a, 0.0, 1.0);

    // Glow size remaps how much of the (generously blurred) aura tail stays visible:
    // small size -> crush the soft tail (tight glow); large size -> reveal it (wide glow).
    float size = clamp(uGlow.x, 1.0, 10.0);
    float falloff = mix(3.0, 0.5, (size - 1.0) / 9.0);

    // Outer soft aura: the blurred coverage that extends beyond the crisp rim.
    float halo = max(glowAlpha - coreAlpha * 0.12, 0.0);
    halo = 1.0 - pow(1.0 - clamp(halo, 0.0, 1.0), falloff);
    float rim = coreAlpha;

    float alpha = clamp(max(rim * 0.95, halo * 1.35), 0.0, 1.0);

    // NOTE: this pass writes back into minecraft:entity_outline, which still holds the solid
    // entity silhouette. We must overwrite EVERY pixel (no discard) so the interior becomes
    // transparent and the player's skin stays visible; only the rim + soft aura remain.
    if (alpha <= 0.001) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 glowColor = min(glow.rgb * 1.45 + vec3(0.02), vec3(1.0));
    vec3 rimColor = mix(core.rgb, vec3(1.0), 0.30);
    vec3 color = mix(glowColor, rimColor, clamp(rim * 1.30, 0.0, 1.0));
    color += glowColor * halo * 0.55;
    color = min(color, vec3(1.0));

    fragColor = vec4(color, alpha);
}
