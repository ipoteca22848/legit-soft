package com.example.auravisuals.module.render;

import com.example.auravisuals.util.friendscreen.FriendScreenClient$StreamQuality;

public class FriendScreen$StreamProfile {
    public int width;
    public int height;
    public long intervalMs;
    public float jpegQuality;
    public int initialBuffer;
    public int maxFrameBytes;

    public FriendScreen$StreamProfile(int width, int height, long intervalMs, float jpegQuality, int initialBuffer, int maxFrameBytes) {
        this.width = width;
        this.height = height;
        this.intervalMs = intervalMs;
        this.jpegQuality = jpegQuality;
        this.initialBuffer = initialBuffer;
        this.maxFrameBytes = maxFrameBytes;
    }

    public static FriendScreen$StreamProfile forQuality(FriendScreenClient$StreamQuality quality) {
        return switch (FriendScreen$1.$SwitchMap$com$example$auravisuals$util$friendscreen$FriendScreenClient$StreamQuality[(quality == null
                ? FriendScreenClient$StreamQuality.MEDIUM
                : quality)
            .ordinal()]) {
            case 1 -> new FriendScreen$StreamProfile(960, 540, 16L, 0.86F, 220000, 190000);
            case 2 -> new FriendScreen$StreamProfile(1280, 720, 16L, 0.9F, 340000, 280000);
            default -> new FriendScreen$StreamProfile(640, 360, 25L, 0.78F, 130000, 120000);
        };
    }
}
