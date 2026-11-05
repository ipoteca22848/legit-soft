#version 150

uniform sampler2D BlurSampler;
uniform sampler2D DepthSampler;

layout(std140) uniform FogBlurData {
    vec4 BlurData;
    vec4 FogData;
    vec4 Reserved;
};

in vec2 texCoord;
out vec4 fragColor;

float linearizeDepth(float depth, float nearPlane, float farPlane) {
    float ndcDepth = depth * 2.0 - 1.0;
    return (2.0 * nearPlane * farPlane) / (farPlane + nearPlane - ndcDepth * (farPlane - nearPlane));
}

void main() {
    float depth = texture(DepthSampler, texCoord).r;
    float linearDistance = linearizeDepth(depth, FogData.x, FogData.y) / FogData.y;
    float fogMask = smoothstep(FogData.z, FogData.w, linearDistance);
    vec3 blurred = texture(BlurSampler, texCoord).rgb;

    // A regular screen blur mixes sky and terrain colors across silhouettes.
    // Suppress that contribution only on sharp depth discontinuities so the
    // result reads as atmospheric fog instead of a dark horizon outline.
    vec2 texel = 1.5 / vec2(textureSize(DepthSampler, 0));
    float d0 = linearDistance;
    float d1 = linearizeDepth(texture(DepthSampler, texCoord + vec2( texel.x, 0.0)).r, FogData.x, FogData.y) / FogData.y;
    float d2 = linearizeDepth(texture(DepthSampler, texCoord + vec2(-texel.x, 0.0)).r, FogData.x, FogData.y) / FogData.y;
    float d3 = linearizeDepth(texture(DepthSampler, texCoord + vec2(0.0,  texel.y)).r, FogData.x, FogData.y) / FogData.y;
    float d4 = linearizeDepth(texture(DepthSampler, texCoord + vec2(0.0, -texel.y)).r, FogData.x, FogData.y) / FogData.y;
    float depthEdge = max(max(abs(d0 - d1), abs(d0 - d2)), max(abs(d0 - d3), abs(d0 - d4)));
    float edgeSafe = 1.0 - smoothstep(0.006, 0.045, depthEdge);
    fogMask *= mix(0.18, 1.0, edgeSafe);

    fragColor = vec4(blurred, clamp(fogMask * BlurData.w, 0.0, 1.0));
}
