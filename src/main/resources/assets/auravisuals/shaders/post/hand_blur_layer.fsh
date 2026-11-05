#version 150

uniform sampler2D DiffuseSampler;
uniform float GlobalAlpha;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 c = texture(DiffuseSampler, texCoord);
    fragColor = vec4(c.rgb, c.a * GlobalAlpha);
}
