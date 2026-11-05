package com.example.auravisuals.util.friendscreen;

public final class FriendScreenClient$PlayerInfo {
    public final double x;
    public final double y;
    public final double z;
    public final float health;
    public final float maxHealth;
    public final int selectedSlot;
    public final String[] itemIds;
    public final int[] itemCounts;
    public final String[] equipmentIds;
    public final int[] equipmentCounts;
    public final String[] inventoryIds;
    public final int[] inventoryCounts;
    public final String[][] equipmentTooltips;
    public final String[][] inventoryTooltips;

    FriendScreenClient$PlayerInfo(
        double x,
        double y,
        double z,
        float health,
        float maxHealth,
        int selectedSlot,
        String[] itemIds,
        int[] itemCounts,
        String[] equipmentIds,
        int[] equipmentCounts,
        String[] inventoryIds,
        int[] inventoryCounts,
        String[][] equipmentTooltips,
        String[][] inventoryTooltips
    ) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.health = health;
        this.maxHealth = maxHealth;
        this.selectedSlot = selectedSlot;
        this.itemIds = itemIds;
        this.itemCounts = itemCounts;
        this.equipmentIds = equipmentIds;
        this.equipmentCounts = equipmentCounts;
        this.inventoryIds = inventoryIds;
        this.inventoryCounts = inventoryCounts;
        this.equipmentTooltips = equipmentTooltips;
        this.inventoryTooltips = inventoryTooltips;
    }
}
