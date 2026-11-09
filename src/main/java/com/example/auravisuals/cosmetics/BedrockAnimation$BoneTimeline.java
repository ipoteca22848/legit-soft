package com.example.auravisuals.cosmetics;

import java.util.ArrayList;
import java.util.List;
import org.joml.Vector3f;

public class BedrockAnimation$BoneTimeline {
    public final List<BedrockAnimation$Keyframe> scaleKeyframes=new ArrayList<>();
    public void evaluateScale(float time,Vector3f out){out.set(1,1,1);interpolate(scaleKeyframes,time,out);}
    public final List<BedrockAnimation$Keyframe> rotationKeyframes = new ArrayList<>();
    public final List<BedrockAnimation$Keyframe> positionKeyframes = new ArrayList<>();

    public void evaluateRotation(float t, Vector3f out) {
        this.interpolate(this.rotationKeyframes, t, out);
    }

    public void evaluatePosition(float t, Vector3f out) {
        this.interpolate(this.positionKeyframes, t, out);
    }

    private static float curve(float a,float b,float c,float d,float t){return .5f*((2*b)+(-a+c)*t+(2*a-5*b+4*c-d)*t*t+(-a+3*b-3*c+d)*t*t*t);}

    void interpolate(List<BedrockAnimation$Keyframe> frames, float t, Vector3f out) {
        if (!frames.isEmpty()) {
            if (frames.size() != 1 && !(t <= frames.get(0).time)) {
                if (t >= frames.get(frames.size() - 1).time) {
                    out.set(frames.get(frames.size() - 1).value);
                } else {
                    for (int i = 0; i < frames.size() - 1; i++) {
                        BedrockAnimation$Keyframe k1 = frames.get(i);
                        BedrockAnimation$Keyframe k2 = frames.get(i + 1);
                        if (t >= k1.time && t <= k2.time) {
                            float factor = (t - k1.time) / (k2.time - k1.time);
                            if("easeInOutQuad".equals(k2.easing))factor=factor<.5f?2*factor*factor:1-(float)Math.pow(-2*factor+2,2)/2;
                            if("catmullrom".equals(k1.easing)||"catmullrom".equals(k2.easing)){
                                var a=frames.get(Math.max(0,i-1)).value;var d=frames.get(Math.min(frames.size()-1,i+2)).value;
                                out.set(curve(a.x,k1.value.x,k2.value.x,d.x,factor),curve(a.y,k1.value.y,k2.value.y,d.y,factor),curve(a.z,k1.value.z,k2.value.z,d.z,factor));return;
                            }
                            out.x = k1.value.x + (k2.value.x - k1.value.x) * factor;
                            out.y = k1.value.y + (k2.value.y - k1.value.y) * factor;
                            out.z = k1.value.z + (k2.value.z - k1.value.z) * factor;
                            return;
                        }
                    }
                }
            } else {
                out.set(frames.get(0).value);
            }
        }
    }
}
