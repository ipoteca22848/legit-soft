package com.example.auravisuals.cosmetics;

import java.util.HashMap;
import java.util.Map;
import org.joml.Vector3f;

public class BedrockAnimation {
    public final String name;
    public final float length;
    public final boolean loop;
    public final Map<String, BedrockAnimation$BoneTimeline> bones = new HashMap<>();

    public BedrockAnimation(String name, float length, boolean loop) {
        this.name = name;
        this.length = length > 0.0F ? length : 1.0F;
        this.loop = loop;
    }

    public void apply(String boneName, float timeSeconds, Vector3f outRot, Vector3f outPos) {
        BedrockAnimation$BoneTimeline timeline = this.bones.get(boneName);
        if (timeline != null) {
            float t = this.loop ? timeSeconds % this.length : Math.min(timeSeconds, this.length);
            timeline.evaluateRotation(t, outRot);
            timeline.evaluatePosition(t, outPos);
        }
    }
}
