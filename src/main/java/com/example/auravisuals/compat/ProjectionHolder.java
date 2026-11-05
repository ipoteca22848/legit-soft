package com.example.auravisuals.compat;

import org.joml.Matrix4f;

public final class ProjectionHolder {
    public static Matrix4f lastProjectionMatrix = new Matrix4f();

    private ProjectionHolder() {
    }
}
