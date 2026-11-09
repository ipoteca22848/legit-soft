package com.example.auravisuals.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

public final class BedrockAnimationParser {
    public static BedrockAnimation parse(InputStream stream) {
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (!root.has("animations")) {
                return null;
            }

            JsonObject anims = root.getAsJsonObject("animations");
            boolean pulse=root.has("anvil_pulse")&&root.get("anvil_pulse").getAsBoolean();

            for (Entry<String, JsonElement> entry : anims.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    JsonObject aObj = entry.getValue().getAsJsonObject();
                    float length = aObj.has("animation_length") ? aObj.get("animation_length").getAsFloat() : 1.5F;
                    boolean loop = !aObj.has("loop") || aObj.get("loop").getAsBoolean();
                    BedrockAnimation animation = new BedrockAnimation(entry.getKey(), length, loop);
                    if (aObj.has("bones")) {
                        JsonObject bonesObj = aObj.getAsJsonObject("bones");

                        for (Entry<String, JsonElement> boneEntry : bonesObj.entrySet()) {
                            if (boneEntry.getValue().isJsonObject()) {
                                String boneName = boneEntry.getKey();
                                JsonObject bData = boneEntry.getValue().getAsJsonObject();
                                BedrockAnimation$BoneTimeline timeline = new BedrockAnimation$BoneTimeline();
                                if (bData.has("rotation")) {
                                    parseKeyframes(bData.get("rotation"), timeline.rotationKeyframes,pulse);
                                }

                                if (bData.has("position")) {
                                    parseKeyframes(bData.get("position"), timeline.positionKeyframes,pulse);
                                }

                                if(pulse&&bData.has("scale"))parseKeyframes(bData.get("scale"),timeline.scaleKeyframes,true);
                                animation.bones.put(boneName, timeline);
                            }
                        }
                    }

                    return animation;
                }
            }

            return null;
        } catch (Exception var18) {
            var18.printStackTrace();
            return null;
        }
    }

    private static void parseKeyframes(JsonElement el, List<BedrockAnimation$Keyframe> outList,boolean pulse) {
        if (el.isJsonObject()) {
            JsonObject obj = el.getAsJsonObject();

            for (Entry<String, JsonElement> entry : obj.entrySet()) {
                float time;
                try {
                    time = Float.parseFloat(entry.getKey());
                } catch (NumberFormatException var7) {
                    continue;
                }

                float[] vals = parseVec3(entry.getValue(),pulse);
                if (vals != null) {
                    var keyframe=new BedrockAnimation$Keyframe(time,vals[0],vals[1],vals[2]);
                    if(pulse&&entry.getValue().isJsonObject()){var o=entry.getValue().getAsJsonObject();if(o.has("easing"))keyframe.easing=o.get("easing").getAsString();else if(o.has("lerp_mode"))keyframe.easing=o.get("lerp_mode").getAsString();}
                    outList.add(keyframe);
                }
            }

            outList.sort(Comparator.comparingDouble(k -> k.time));
        } else if (el.isJsonArray()) {
            float[] vals = parseVec3(el,pulse);
            if (vals != null) {
                outList.add(new BedrockAnimation$Keyframe(0.0F, vals[0], vals[1], vals[2]));
            }
        }
    }

    private static float[] parseVec3(JsonElement el,boolean pulse) {
        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            if (arr.size() >= 3) {
                return new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat()};
            }
        } else if (el.isJsonObject()) {
            JsonObject o = el.getAsJsonObject();
            if(pulse&&o.has("vector"))return parseVec3(o.get("vector"),true);
            if (o.has("post") && o.get("post").isJsonArray()) {
                JsonArray arr = o.getAsJsonArray("post");
                if (arr.size() >= 3) {
                    return new float[]{arr.get(0).getAsFloat(), arr.get(1).getAsFloat(), arr.get(2).getAsFloat()};
                }
            }
        }

        return null;
    }
}
