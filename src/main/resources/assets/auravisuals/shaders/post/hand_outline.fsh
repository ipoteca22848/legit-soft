#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform vec3 OutlineColor;
uniform float LayerOpacity;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 px = 1.0 / max(InSize, vec2(1.0));
    float m = texture(DiffuseSampler, texCoord).a;
    float threshold = 0.6;

    if (m >= threshold) discard;

    float neighborInside = 0.0;
    for (int dx = -2; dx <= 2; dx++) {
        for (int dy = -2; dy <= 2; dy++) {
            if (dx == 0 && dy == 0) continue;
            if (abs(dx) + abs(dy) > 3) continue;
            float n = texture(DiffuseSampler, texCoord + vec2(float(dx), float(dy)) * px).a;
            if (n >= threshold) {
                neighborInside = 1.0;
            }
        }
    }

    if (neighborInside < 0.5) discard;
    fragColor = vec4(OutlineColor, LayerOpacity);
}
