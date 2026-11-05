#version 150

uniform sampler2D Sampler0;

layout(std140) uniform KawaseParams {
    vec4 Bounds;
    vec4 TexelData;
    vec4 Tint;
};

in vec2 TexCoord;
out vec4 fragColor;

vec2 safeUv(vec2 uv) {
    return clamp(uv, vec2(0.0), vec2(1.0));
}

void main() {
    vec2 stepUv = max(TexelData.xy, vec2(0.000001));
    vec3 color = texture(Sampler0, safeUv(TexCoord)).rgb * 0.20;

    color += texture(Sampler0, safeUv(TexCoord + vec2( stepUv.x,  stepUv.y))).rgb * 0.12;
    color += texture(Sampler0, safeUv(TexCoord + vec2(-stepUv.x,  stepUv.y))).rgb * 0.12;
    color += texture(Sampler0, safeUv(TexCoord + vec2( stepUv.x, -stepUv.y))).rgb * 0.12;
    color += texture(Sampler0, safeUv(TexCoord + vec2(-stepUv.x, -stepUv.y))).rgb * 0.12;

    color += texture(Sampler0, safeUv(TexCoord + vec2( stepUv.x * 2.0, 0.0))).rgb * 0.08;
    color += texture(Sampler0, safeUv(TexCoord + vec2(-stepUv.x * 2.0, 0.0))).rgb * 0.08;
    color += texture(Sampler0, safeUv(TexCoord + vec2(0.0,  stepUv.y * 2.0))).rgb * 0.08;
    color += texture(Sampler0, safeUv(TexCoord + vec2(0.0, -stepUv.y * 2.0))).rgb * 0.08;

    fragColor = vec4(color, 1.0);
}
