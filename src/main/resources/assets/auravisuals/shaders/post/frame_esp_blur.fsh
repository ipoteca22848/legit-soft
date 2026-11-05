#version 330

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BlurConfig {
    vec2 BlurDir;
    float Radius;
};

uniform sampler2D InSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 stepUv = BlurDir / max(InSize, vec2(1.0));
    vec4 sum = vec4(0.0);
    float weightSum = 0.0;
    float sigma = max(1.0, Radius * 0.60);
    for (int i = -16; i <= 16; i++) {
        float offset = float(i);
        if (abs(offset) > Radius) continue;
        float weight = exp(-0.5 * (offset * offset) / (sigma * sigma));
        sum += texture(InSampler, texCoord + stepUv * offset) * weight;
        weightSum += weight;
    }
    fragColor = sum / max(weightSum, 0.0001);
}
