package com.example.auravisuals.module.render;

public enum TrapTimer$StructureType {
    TRAP("Трапка", -1548963),
    INEVITABLE_TRAP("Неизбежная", -30899);

    public final String label;
    public final int color;

    TrapTimer$StructureType(String label, int color) {
        this.label = label;
        this.color = color;
    }
}
