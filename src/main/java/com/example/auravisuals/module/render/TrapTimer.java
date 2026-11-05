package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.language.LanguageManager;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class TrapTimer extends Module {
    static final String TRAP_SOUND = "block.piston.extend";
    static final String INEVITABLE_TRAP_SOUND = "block.anvil.place";
    static final String DRAGON_SOUND = "entity.ender_dragon.growl";
    static final long TRAP_DURATION = 15000L;
    static final long INEVITABLE_TRAP_DURATION = 20000L;
    static final int MAX_VISIBLE_TIMERS = 5;
    public static final NumberSetting x = new NumberSetting("TrapTimer X", 360.0, 0.0, 2000.0, 1.0);
    public static final NumberSetting y = new NumberSetting("TrapTimer Y", 80.0, 0.0, 1000.0, 1.0);
    public static final NumberSetting scale = new NumberSetting("TrapTimer Scale", 0.88, 0.5, 2.0, 0.05);
    final List<TrapTimer$StructureEntry> entries = new ArrayList<>();
    int previousNetheriteHoeCount = -1;

    public TrapTimer() {
        super("TrapTimer", "Показывает таймеры трапок", Category.VISUALS);
    }

    @Override
    public void onTick() {
        if (!this.canWork()) {
            this.entries.clear();
            this.previousNetheriteHoeCount = -1;
        } else {
            this.updateEntries();
            int currentCount = this.countNetheriteHoe();
            if (this.previousNetheriteHoeCount == -1) {
                this.previousNetheriteHoeCount = currentCount;
            } else {
                if (this.previousNetheriteHoeCount > 0 && currentCount < this.previousNetheriteHoeCount) {
                    this.addEntry(this.mc.player.getEntityPos(), TrapTimer$StructureType.TRAP, 15000L, true, false);
                }

                this.previousNetheriteHoeCount = currentCount;
            }
        }
    }

    public void onPlaySound(PlaySoundS2CPacket packet) {
        if (this.canWork() && packet != null) {
            Identifier id = packet.getSound().value().id();
            String sound = id.getPath();
            float pitch = packet.getPitch();
            float volume = packet.getVolume();
            Vec3d position = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
            this.detectSound(sound, pitch, volume, position);
        }
    }

    public void onPlaySoundFromEntity(PlaySoundFromEntityS2CPacket packet) {
        if (this.canWork() && packet != null) {
            Entity entity = this.mc.world.getEntityById(packet.getEntityId());
            if (entity != null) {
                Identifier id = packet.getSound().value().id();
                this.detectSound(id.getPath(), packet.getPitch(), packet.getVolume(), entity.getEntityPos());
            }
        }
    }

    @Override
    public void onDisable() {
        this.entries.clear();
        this.previousNetheriteHoeCount = -1;
    }

    void detectSound(String sound, float pitch, float volume, Vec3d position) {
        if (this.matchesSound(sound, "block.piston.extend")) {
            this.addEntry(position, TrapTimer$StructureType.TRAP, 15000L, true, false);
        } else if (this.matchesSound(sound, "block.anvil.place")) {
            this.addEntry(position, TrapTimer$StructureType.INEVITABLE_TRAP, 20000L, true, false);
        } else {
            if (this.matchesSound(sound, "entity.ender_dragon.growl")) {
                this.addEntry(position, TrapTimer$StructureType.INEVITABLE_TRAP, 20000L, true, false);
            }
        }
    }

    public List<TrapTimer$TimerView> getVisibleTimers(boolean editorMode) {
        if (this.isEnabled() && this.mc.player != null && this.mc.world != null) {
            this.updateEntries();
            long now = System.currentTimeMillis();
            List<TrapTimer$TimerView> visible = this.entries
                .stream()
                .filter(entry -> entry.typeDetermined)
                .sorted(Comparator.comparingLong(entry -> entry.createdAt))
                .limit(5L)
                .map(
                    entry -> {
                        long remaining = Math.max(0L, entry.duration - (now - entry.createdAt));
                        return remaining > 0L
                            ? new TrapTimer$TimerView(this.buildTimerText(entry.type, remaining), remaining, entry.duration, entry.type.color)
                            : null;
                    }
                )
                .filter(view -> view != null)
                .toList();
            return visible.isEmpty() && editorMode
                ? List.of(
                    new TrapTimer$TimerView(this.buildTimerText(TrapTimer$StructureType.TRAP, 12000L), 12000L, 15000L, TrapTimer$StructureType.TRAP.color),
                    new TrapTimer$TimerView(
                        this.buildTimerText(TrapTimer$StructureType.INEVITABLE_TRAP, 18000L), 18000L, 20000L, TrapTimer$StructureType.INEVITABLE_TRAP.color
                    )
                )
                : visible;
        } else {
            return List.of();
        }
    }

    TrapTimer$StructureEntry addEntry(Vec3d position, TrapTimer$StructureType type, long duration, boolean typeDetermined, boolean needsNetheriteCheck) {
        long now = System.currentTimeMillis();

        for (TrapTimer$StructureEntry entry : this.entries) {
            if (entry.type == type && entry.position.squaredDistanceTo(position) < 4.0 && now - entry.createdAt < 1200L) {
                return entry;
            }
        }

        TrapTimer$StructureEntry entry = new TrapTimer$StructureEntry(position, now, type, duration, typeDetermined, needsNetheriteCheck);
        this.entries.add(entry);
        return entry;
    }

    void updateEntries() {
        long now = System.currentTimeMillis();

        for (TrapTimer$StructureEntry entry2 : this.entries) {
            if (entry2.needsNetheriteCheck && now - entry2.createdAt >= 500L) {
                entry2.type = TrapTimer$StructureType.INEVITABLE_TRAP;
                entry2.duration = 20000L;
                entry2.needsNetheriteCheck = false;
                entry2.typeDetermined = true;
            }

            if (!entry2.typeDetermined && !entry2.needsNetheriteCheck && now - entry2.createdAt >= 50L && now - entry2.createdAt > 300L) {
                entry2.typeDetermined = true;
            }
        }

        this.entries
            .removeIf(entry -> entry.typeDetermined && now - entry.createdAt > entry.duration || !entry.typeDetermined && now - entry.createdAt > 1000L);
    }

    int countNetheriteHoe() {
        if (this.mc.player == null) {
            return 0;
        }

        int count = 0;

        for (int i = 0; i < this.mc.player.getInventory().size(); i++) {
            if (this.mc.player.getInventory().getStack(i).getItem() == Items.NETHERITE_HOE) {
                count += this.mc.player.getInventory().getStack(i).getCount();
            }
        }

        return count;
    }

    boolean canWork() {
        return this.isEnabled() && this.mc.player != null && this.mc.world != null && this.isAllowedServer();
    }

    boolean isAllowedServer() {
        if (this.mc.getCurrentServerEntry() != null && this.mc.getCurrentServerEntry().address != null) {
            String address = this.mc.getCurrentServerEntry().address.toLowerCase(Locale.ROOT);
            return address.contains("funtime")
                || address.contains("spookytime")
                || address.contains("space-times")
                || address.contains("space-time")
                || address.contains("spacetime");
        } else {
            return false;
        }
    }

    boolean matchesSound(String sound, String expectedSound) {
        return expectedSound.equals(sound);
    }

    boolean close(float value, float expected, float tolerance) {
        return Math.abs(value - expected) <= tolerance;
    }

    String buildTimerText(TrapTimer$StructureType type, long remaining) {
        long seconds = Math.max(1L, (long)Math.ceil(remaining / 1000.0));
        String label = LanguageManager.translateForCategory(Category.VISUALS, type.label);
        String secondsLabel = LanguageManager.translateForCategory(Category.VISUALS, "секунд");
        return label + ": " + seconds + " " + secondsLabel;
    }
}
