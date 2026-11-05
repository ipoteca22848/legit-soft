package com.example.auravisuals.module.render;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.math.Vec3d;

public class Hats$Ghost {
    public final List<Vec3d> history = new ArrayList<>();
    public static final int MAX_HISTORY = 20;

    public void update(Vec3d pos) {
        this.history.add(pos);
        if (this.history.size() > 20) {
            this.history.remove(0);
        }
    }
}
