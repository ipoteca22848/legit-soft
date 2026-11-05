package com.example.auravisuals.module.player;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.BooleanSetting;

public class LockSlot extends Module {
    public final BooleanSetting slot1 = new BooleanSetting("Слот 1", false);
    public final BooleanSetting slot2 = new BooleanSetting("Слот 2", false);
    public final BooleanSetting slot3 = new BooleanSetting("Слот 3", false);
    public final BooleanSetting slot4 = new BooleanSetting("Слот 4", false);
    public final BooleanSetting slot5 = new BooleanSetting("Слот 5", false);
    public final BooleanSetting slot6 = new BooleanSetting("Слот 6", false);
    public final BooleanSetting slot7 = new BooleanSetting("Слот 7", false);
    public final BooleanSetting slot8 = new BooleanSetting("Слот 8", false);
    public final BooleanSetting slot9 = new BooleanSetting("Слот 9", false);

    public LockSlot() {
        super("LockSlot", "Запрещает выбрасывать предметы из выбранных слотов", Category.PLAYER);
        this.addSettings(this.slot1, this.slot2, this.slot3, this.slot4, this.slot5, this.slot6, this.slot7, this.slot8, this.slot9);
    }

    public boolean isSlotLocked(int slotIndex) {
        switch (slotIndex) {
            case 0:
                return this.slot1.getValue();
            case 1:
                return this.slot2.getValue();
            case 2:
                return this.slot3.getValue();
            case 3:
                return this.slot4.getValue();
            case 4:
                return this.slot5.getValue();
            case 5:
                return this.slot6.getValue();
            case 6:
                return this.slot7.getValue();
            case 7:
                return this.slot8.getValue();
            case 8:
                return this.slot9.getValue();
            default:
                return false;
        }
    }
}
