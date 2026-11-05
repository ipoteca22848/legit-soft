package com.example.auravisuals.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.joml.Vector3f;

public final class BedrockModelParser {
    private BedrockModelParser() {
    }

    public static BedrockModel parse(InputStream inputStream) {
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray geometries = null;
            if (root.has("minecraft:geometry")) {
                geometries = root.getAsJsonArray("minecraft:geometry");
            } else {
                for (Entry<String, JsonElement> entry : root.entrySet()) {
                    if (entry.getKey().startsWith("geometry.") && entry.getValue().isJsonObject()) {
                        JsonObject geomObj = entry.getValue().getAsJsonObject();
                        return parseGeometryObject(geomObj);
                    }
                }
            }

            return geometries == null || geometries.size() <= 0 ? null : parseGeometryObject(geometries.get(0).getAsJsonObject());
        } catch (Exception var10) {
            var10.printStackTrace();
            return null;
        }
    }

    private static BedrockModel parseGeometryObject(JsonObject geom) {
        float texW = 64.0F;
        float texH = 64.0F;
        if (geom.has("description")) {
            JsonObject desc = geom.getAsJsonObject("description");
            if (desc.has("texture_width")) {
                texW = desc.get("texture_width").getAsFloat();
            }

            if (desc.has("texture_height")) {
                texH = desc.get("texture_height").getAsFloat();
            }
        } else {
            if (geom.has("texturewidth")) {
                texW = geom.get("texturewidth").getAsFloat();
            }

            if (geom.has("textureheight")) {
                texH = geom.get("textureheight").getAsFloat();
            }
        }

        BedrockModel model = new BedrockModel(texW, texH);
        Map<String, BedrockModel$Bone> boneMap = new HashMap<>();
        float lowestY = Float.MAX_VALUE;
        float highestY = -Float.MAX_VALUE;
        if (geom.has("bones")) {
            for (JsonElement boneEl : geom.getAsJsonArray("bones")) {
                if (boneEl.isJsonObject()) {
                    JsonObject bObj = boneEl.getAsJsonObject();
                    BedrockModel$Bone bone = new BedrockModel$Bone();
                    bone.name = bObj.has("name") ? bObj.get("name").getAsString() : "";
                    bone.parent = bObj.has("parent") ? bObj.get("parent").getAsString() : null;
                    if (bObj.has("pivot")) {
                        JsonArray piv = bObj.getAsJsonArray("pivot");
                        bone.pivot[0] = piv.get(0).getAsFloat();
                        bone.pivot[1] = piv.get(1).getAsFloat();
                        bone.pivot[2] = piv.get(2).getAsFloat();
                    }

                    if (bObj.has("rotation")) {
                        JsonArray rot = bObj.getAsJsonArray("rotation");
                        bone.rotation[0] = rot.get(0).getAsFloat();
                        bone.rotation[1] = rot.get(1).getAsFloat();
                        bone.rotation[2] = rot.get(2).getAsFloat();
                    }

                    if (bObj.has("cubes")) {
                        for (JsonElement cEl : bObj.getAsJsonArray("cubes")) {
                            if (cEl.isJsonObject()) {
                                JsonObject cObj = cEl.getAsJsonObject();
                                BedrockModel$Cube cube = parseCube(cObj, texW, texH);
                                if (cube != null) {
                                    bone.cubes.add(cube);
                                    if (cube.origin[1] < lowestY) {
                                        lowestY = cube.origin[1];
                                    }

                                    if (cube.origin[1] + cube.size[1] > highestY) {
                                        highestY = cube.origin[1] + cube.size[1];
                                    }
                                }
                            }
                        }
                    }

                    model.bones.add(bone);
                    boneMap.put(bone.name, bone);
                }
            }

            if (lowestY != Float.MAX_VALUE) {
                model.minY = lowestY;
                model.maxY = highestY;
            }

            for (BedrockModel$Bone b : model.bones) {
                if (b.parent != null && boneMap.containsKey(b.parent)) {
                    boneMap.get(b.parent).children.add(b);
                }
            }

            model.recalculateBounds();
        }

        return model;
    }

    private static BedrockModel$Cube parseCube(JsonObject cObj, float texW, float texH) {
        BedrockModel$Cube cube = new BedrockModel$Cube();
        if (cObj.has("origin")) {
            JsonArray org = cObj.getAsJsonArray("origin");
            cube.origin[0] = org.get(0).getAsFloat();
            cube.origin[1] = org.get(1).getAsFloat();
            cube.origin[2] = org.get(2).getAsFloat();
        }

        if (cObj.has("size")) {
            JsonArray sz = cObj.getAsJsonArray("size");
            cube.size[0] = sz.get(0).getAsFloat();
            cube.size[1] = sz.get(1).getAsFloat();
            cube.size[2] = sz.get(2).getAsFloat();
        }

        if (cObj.has("inflate")) {
            cube.inflate = cObj.get("inflate").getAsFloat();
        }

        if (cObj.has("mirror")) {
            cube.mirror = cObj.get("mirror").getAsBoolean();
        }

        if (cObj.has("pivot")) {
            JsonArray piv = cObj.getAsJsonArray("pivot");
            cube.pivot[0] = piv.get(0).getAsFloat();
            cube.pivot[1] = piv.get(1).getAsFloat();
            cube.pivot[2] = piv.get(2).getAsFloat();
        }

        if (cObj.has("rotation")) {
            JsonArray rot = cObj.getAsJsonArray("rotation");
            cube.rotation[0] = rot.get(0).getAsFloat();
            cube.rotation[1] = rot.get(1).getAsFloat();
            cube.rotation[2] = rot.get(2).getAsFloat();
        }

        float x0 = (cube.origin[0] - cube.inflate) / 16.0F;
        float y0 = (cube.origin[1] - cube.inflate) / 16.0F;
        float z0 = (cube.origin[2] - cube.inflate) / 16.0F;
        float x1 = (cube.origin[0] + cube.size[0] + cube.inflate) / 16.0F;
        float y1 = (cube.origin[1] + cube.size[1] + cube.inflate) / 16.0F;
        float z1 = (cube.origin[2] + cube.size[2] + cube.inflate) / 16.0F;
        if (cObj.has("uv") && cObj.get("uv").isJsonObject()) {
            JsonObject uvObj = cObj.getAsJsonObject("uv");
            addPerFace(cube, uvObj, "north", x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0.0F, 0.0F, -1.0F, texW, texH);
            addPerFace(cube, uvObj, "south", x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0.0F, 0.0F, 1.0F, texW, texH);
            addPerFace(cube, uvObj, "east", x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1.0F, 0.0F, 0.0F, texW, texH);
            addPerFace(cube, uvObj, "west", x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1.0F, 0.0F, 0.0F, texW, texH);
            addPerFace(cube, uvObj, "up", x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0.0F, 1.0F, 0.0F, texW, texH);
            addPerFace(cube, uvObj, "down", x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0.0F, -1.0F, 0.0F, texW, texH);
        } else {
            float u = 0.0F;
            float v = 0.0F;
            if (cObj.has("uv") && cObj.get("uv").isJsonArray()) {
                JsonArray uvArr = cObj.getAsJsonArray("uv");
                u = uvArr.get(0).getAsFloat();
                v = uvArr.get(1).getAsFloat();
            }

            float dx = cube.size[0];
            float dy = cube.size[1];
            float dz = cube.size[2];
            addBoxQuad(cube, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, (u + dz) / texW, v / texH, (u + dz + dx) / texW, (v + dz) / texH, 0.0F, 1.0F, 0.0F);
            addBoxQuad(
                cube,
                x0,
                y0,
                z1,
                x1,
                y0,
                z1,
                x1,
                y0,
                z0,
                x0,
                y0,
                z0,
                (u + dz + dx) / texW,
                v / texH,
                (u + dz + dx + dx) / texW,
                (v + dz) / texH,
                0.0F,
                -1.0F,
                0.0F
            );
            addBoxQuad(
                cube,
                x1,
                y0,
                z0,
                x0,
                y0,
                z0,
                x0,
                y1,
                z0,
                x1,
                y1,
                z0,
                (u + dz) / texW,
                (v + dz) / texH,
                (u + dz + dx) / texW,
                (v + dz + dy) / texH,
                0.0F,
                0.0F,
                -1.0F
            );
            addBoxQuad(
                cube,
                x0,
                y0,
                z1,
                x1,
                y0,
                z1,
                x1,
                y1,
                z1,
                x0,
                y1,
                z1,
                (u + dz + dx + dz) / texW,
                (v + dz) / texH,
                (u + dz + dx + dz + dx) / texW,
                (v + dz + dy) / texH,
                0.0F,
                0.0F,
                1.0F
            );
            addBoxQuad(
                cube, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, u / texW, (v + dz) / texH, (u + dz) / texW, (v + dz + dy) / texH, -1.0F, 0.0F, 0.0F
            );
            addBoxQuad(
                cube,
                x1,
                y0,
                z1,
                x1,
                y0,
                z0,
                x1,
                y1,
                z0,
                x1,
                y1,
                z1,
                (u + dz + dx) / texW,
                (v + dz) / texH,
                (u + dz + dx + dz) / texW,
                (v + dz + dy) / texH,
                1.0F,
                0.0F,
                0.0F
            );
        }

        return cube;
    }

    private static void addPerFace(
        BedrockModel$Cube cube,
        JsonObject uvObj,
        String face,
        float x0,
        float y0,
        float z0,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float x3,
        float y3,
        float z3,
        float nx,
        float ny,
        float nz,
        float tw,
        float th
    ) {
        if (uvObj.has(face)) {
            JsonObject fObj = uvObj.getAsJsonObject(face);
            if (fObj.has("uv") && fObj.has("uv_size")) {
                JsonArray uv = fObj.getAsJsonArray("uv");
                JsonArray sz = fObj.getAsJsonArray("uv_size");
                float u0 = uv.get(0).getAsFloat() / tw;
                float v0 = uv.get(1).getAsFloat() / th;
                float u1 = (uv.get(0).getAsFloat() + sz.get(0).getAsFloat()) / tw;
                float v1 = (uv.get(1).getAsFloat() + sz.get(1).getAsFloat()) / th;
                addQuad(cube, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, u0, v0, u1, v1, nx, ny, nz);
            }
        }
    }

    private static void addQuad(
        BedrockModel$Cube cube,
        float x0,
        float y0,
        float z0,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float x3,
        float y3,
        float z3,
        float u0,
        float v0,
        float u1,
        float v1,
        float nx,
        float ny,
        float nz
    ) {
        BedrockModel$Vertex vA = new BedrockModel$Vertex(x0, y0, z0, u0, v1);
        BedrockModel$Vertex vB = new BedrockModel$Vertex(x1, y1, z1, u1, v1);
        BedrockModel$Vertex vC = new BedrockModel$Vertex(x2, y2, z2, u1, v0);
        BedrockModel$Vertex vD = new BedrockModel$Vertex(x3, y3, z3, u0, v0);
        cube.quads.add(new BedrockModel$Quad(vA, vB, vC, vD, new Vector3f(nx, ny, nz)));
    }

    private static void addBoxQuad(
        BedrockModel$Cube cube,
        float x0,
        float y0,
        float z0,
        float x1,
        float y1,
        float z1,
        float x2,
        float y2,
        float z2,
        float x3,
        float y3,
        float z3,
        float u0,
        float v0,
        float u1,
        float v1,
        float nx,
        float ny,
        float nz
    ) {
        if (cube.mirror) {
            float swap = u0;
            u0 = u1;
            u1 = swap;
        }

        addQuad(cube, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, u0, v0, u1, v1, nx, ny, nz);
    }
}
