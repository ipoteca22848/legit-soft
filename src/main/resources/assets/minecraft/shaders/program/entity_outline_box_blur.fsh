#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 sampleStep;

out vec4 fragColor;

void main() {
    vec4 sum = vec4(0.0);
    float totalWeight = 0.0;
    for (int i = -7; i <= 7; i++) {
        float offset = float(i) * 1.25;
        float weight = exp(-0.5 * (offset * offset) / 12.0);
        vec4 sampleColor = texture(DiffuseSampler, texCoord + sampleStep * offset);
        sum += sampleColor * weight;
        totalWeight += weight;
    }
    vec4 result = sum / max(totalWeight, 0.0001);
    float alpha = clamp(result.a * 1.45, 0.0, 1.0);
    vec3 brightColor = min(result.rgb * 1.28 + vec3(0.035), vec3(1.0));
    fragColor = vec4(brightColor, alpha);
}
