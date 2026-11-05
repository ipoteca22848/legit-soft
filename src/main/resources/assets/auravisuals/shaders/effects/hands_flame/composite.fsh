#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D SceneSampler;
uniform sampler2D BeforeSampler;
uniform sampler2D TrailSampler;
uniform sampler2D HandDepthSampler;

layout(std140) uniform HandsFlameData {
    vec4 flameColor;
    vec4 params0;
    vec4 params1;
    vec4 screen;
};

float selectedHand(vec2 uv) {
    return mix(screen.y, screen.x, smoothstep(0.46, 0.54, uv.x));
}

float handCoverage(vec2 uv) {
    vec3 afterColor = texture(SceneSampler, uv).rgb;
    vec3 beforeColor = texture(BeforeSampler, uv).rgb;
    vec3 delta = abs(afterColor - beforeColor);
    float peak = max(max(delta.r, delta.g), delta.b);
    float luma = dot(delta, vec3(0.299, 0.587, 0.114));
    float colorMask = smoothstep(0.004, 0.060, peak * 0.78 + luma * 0.88) * selectedHand(uv);
    float depth = texture(HandDepthSampler, uv).r;
    float depthMask = (1.0 - smoothstep(0.9985, 0.99995, depth)) * selectedHand(uv);
    return max(colorMask, depthMask);
}

float stableHandCoverage(vec2 uv, vec2 px) {
    float center = handCoverage(uv);
    float left = 0.0;
    float right = 0.0;
    float down = 0.0;
    float up = 0.0;
    // Four probes per direction are enough to close tiny holes around the hand;
    // the previous seven produced almost the same mask at a much higher cost.
    for (int i = 1; i <= 4; i++) {
        vec2 dx = vec2(float(i) * px.x, 0.0);
        vec2 dy = vec2(0.0, float(i) * px.y);
        left = max(left, handCoverage(clamp(uv - dx, vec2(0.0), vec2(1.0))));
        right = max(right, handCoverage(clamp(uv + dx, vec2(0.0), vec2(1.0))));
        down = max(down, handCoverage(clamp(uv - dy, vec2(0.0), vec2(1.0))));
        up = max(up, handCoverage(clamp(uv + dy, vec2(0.0), vec2(1.0))));
    }
    float crossEnclosed = min(min(left, right), min(down, up));
    return max(center, crossEnclosed);
}

void main() {
    vec2 px = screen.zw;
    float brightness = params1.x;

    vec2 blurPx = px * 2.65;
    vec4 trail = texture(TrailSampler, texCoord) * 0.39;
    trail += texture(TrailSampler, clamp(texCoord + vec2(blurPx.x, 0.0), vec2(0.0), vec2(1.0))) * 0.105;
    trail += texture(TrailSampler, clamp(texCoord - vec2(blurPx.x, 0.0), vec2(0.0), vec2(1.0))) * 0.105;
    trail += texture(TrailSampler, clamp(texCoord + vec2(0.0, blurPx.y), vec2(0.0), vec2(1.0))) * 0.105;
    trail += texture(TrailSampler, clamp(texCoord - vec2(0.0, blurPx.y), vec2(0.0), vec2(1.0))) * 0.105;
    trail += texture(TrailSampler, clamp(texCoord + blurPx, vec2(0.0), vec2(1.0))) * 0.0475;
    trail += texture(TrailSampler, clamp(texCoord - blurPx, vec2(0.0), vec2(1.0))) * 0.0475;
    trail += texture(TrailSampler, clamp(texCoord + vec2(blurPx.x, -blurPx.y), vec2(0.0), vec2(1.0))) * 0.0475;
    trail += texture(TrailSampler, clamp(texCoord + vec2(-blurPx.x, blurPx.y), vec2(0.0), vec2(1.0))) * 0.0475;
    vec4 scene = texture(SceneSampler, texCoord);
    vec4 sceneBefore = texture(BeforeSampler, texCoord);


    float mask = stableHandCoverage(texCoord, px);
    float itemCover = smoothstep(0.04, 0.40, mask);


    if (trail.a < 0.003 && itemCover < 0.01) {
        fragColor = vec4(scene.rgb, 1.0);
        return;
    }

    float behindItem = 1.0 - itemCover * 0.98;


    float field = smoothstep(0.006, 0.40, trail.a) * behindItem;

    float sceneLuma = dot(scene.rgb, vec3(0.299, 0.587, 0.114));
    float darkCover = 1.0 - smoothstep(0.16, 0.72, sceneLuma);
    float brightnessCurve = 0.42 + (1.0 - exp(-clamp(brightness, 0.0, 2.0) * 0.8)) * 0.64;


    float glowIntensity = pow(trail.a, 0.48) * field;
    float veil = clamp(glowIntensity * (0.20 + brightnessCurve * 0.22) * (1.0 + darkCover * 0.38), 0.0, 0.66);

    veil += itemCover * smoothstep(0.05, 0.32, trail.a) * 0.10;


    vec3 flameFill = min(trail.rgb * (0.78 + brightnessCurve * 0.42) + vec3(0.025), vec3(1.0));




    float blurAmount = field * smoothstep(0.10, 0.52, trail.a);
    vec3 sceneBase;
    if (blurAmount > 0.01) {
        vec2 bgPx = px * (4.0 + blurAmount * 4.5);
        vec3 blurred = sceneBefore.rgb * 0.28;
        blurred += texture(BeforeSampler, clamp(texCoord + vec2(bgPx.x, 0.0), vec2(0.0), vec2(1.0))).rgb * 0.12;
        blurred += texture(BeforeSampler, clamp(texCoord - vec2(bgPx.x, 0.0), vec2(0.0), vec2(1.0))).rgb * 0.12;
        blurred += texture(BeforeSampler, clamp(texCoord + vec2(0.0, bgPx.y), vec2(0.0), vec2(1.0))).rgb * 0.12;
        blurred += texture(BeforeSampler, clamp(texCoord - vec2(0.0, bgPx.y), vec2(0.0), vec2(1.0))).rgb * 0.12;
        blurred += texture(BeforeSampler, clamp(texCoord + bgPx, vec2(0.0), vec2(1.0))).rgb * 0.07;
        blurred += texture(BeforeSampler, clamp(texCoord - bgPx, vec2(0.0), vec2(1.0))).rgb * 0.07;
        blurred += texture(BeforeSampler, clamp(texCoord + vec2(bgPx.x, -bgPx.y), vec2(0.0), vec2(1.0))).rgb * 0.05;
        blurred += texture(BeforeSampler, clamp(texCoord + vec2(-bgPx.x, bgPx.y), vec2(0.0), vec2(1.0))).rgb * 0.05;

        sceneBase = mix(scene.rgb, blurred, blurAmount * (1.0 - itemCover));
    } else {
        sceneBase = scene.rgb;
    }


    sceneBase *= (1.0 - veil * darkCover * 0.18);


    vec3 covered = mix(sceneBase, flameFill, veil);


    float glowPulse = 0.94 + sin((texCoord.y + texCoord.x * 0.35) * 16.0 + params1.y * 3.6) * 0.035;
    vec3 glow = trail.rgb * pow(trail.a, 0.48) * field * glowPulse;


    float coreIntensity = pow(trail.a * field, 2.15);
    vec3 core = min(trail.rgb * 1.22 + vec3(0.04), vec3(1.0)) * coreIntensity;

    vec3 color = covered
        + glow * (0.78 + brightnessCurve * 0.54)
        + core * (0.10 + brightnessCurve * 0.10);


    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
