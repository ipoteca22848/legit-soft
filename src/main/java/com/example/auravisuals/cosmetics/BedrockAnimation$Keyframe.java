package com.example.auravisuals.cosmetics;

import org.joml.Vector3f;

public class BedrockAnimation$Keyframe {
    public final float time;
    public String easing="linear";
    public final Vector3f value;

    public BedrockAnimation$Keyframe(float time, float x, float y, float z) {
        this.time = time;
        this.value = new Vector3f(x, y, z);
    }
}
