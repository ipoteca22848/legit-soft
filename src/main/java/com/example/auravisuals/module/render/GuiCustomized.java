package com.example.auravisuals.module.render;

import com.example.auravisuals.module.Category;
import com.example.auravisuals.module.Module;
import com.example.auravisuals.setting.ActionSetting;
import com.example.auravisuals.setting.BooleanSetting;
import com.example.auravisuals.setting.ModeSetting;
import com.example.auravisuals.setting.NumberSetting;
import com.example.auravisuals.util.render.gui.GifOption;
import com.example.auravisuals.util.render.gui.Gifs;
import com.example.auravisuals.util.render.gui.LiveUploader;
import com.example.auravisuals.util.render.gui.MediaProcessor;
import com.example.auravisuals.util.render.gui.MediaProcessor$Prepared;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

public class GuiCustomized extends Module {
    private String lastAppliedBackground = "";
    public static final ModeSetting background = new ModeSetting("Фон", "None", () -> true, "None");
    public static final NumberSetting opacity = new NumberSetting("Прозрачность", 1.0, 0.1, 1.0, 0.05);
    public static final NumberSetting speed = new NumberSetting("Скорость", 1.0, 0.25, 3.0, 0.25);
    public static final BooleanSetting allContainers = new BooleanSetting("Все контейнеры", true);
    public static final BooleanSetting darkSlots = new BooleanSetting("Тёмные слоты", true);
    public static final ActionSetting addFile = new ActionSetting("Добавить GIF/фото...", GuiCustomized::pickCustom);
    public static final ActionSetting openFolder = new ActionSetting("Открыть папку", () -> {
        try {
            Util.getOperatingSystem().open(MediaProcessor.mediaDir().toFile());
        } catch (Throwable var1) {
        }
    });
    public static final ActionSetting refresh = new ActionSetting("Очистить фоны", () -> {
        for (int i = 0; i < Gifs.maxSlots(); i++) {
            Gifs.deleteSlot(i);
        }

        Gifs.select("none");
        updateModes();
    });

    public GuiCustomized() {
        super("GuiCustomized", "GIF и картинки на фоне инвентаря и сундуков", Category.VISUALS);
        this.addSettings(background, opacity, speed, allContainers, darkSlots, addFile, openFolder, refresh);
        updateModes();
    }

    public static void updateModes() {
        List names = Gifs.getDisplayNames();
        background.setModes(names);
        GifOption cur = Gifs.current();
        if (cur != null && names.contains(cur.name)) {
            background.setValue(cur.name);
        } else {
            background.setValue("None");
        }
    }

    @Override
    public void onEnable() {
        updateModes();
        this.applySelectedBackground();
        GifOption cur = Gifs.current();
        if (cur != null) {
            cur.ensureLoading();
        }
    }

    @Override
    public void onTick() {
        this.applySelectedBackground();
    }

    private void applySelectedBackground() {
        String selected = background.getValue();
        if (selected != null && !selected.equals(this.lastAppliedBackground)) {
            this.lastAppliedBackground = selected;
            if (selected.equalsIgnoreCase("None")) {
                Gifs.select("none");
            } else {
                for (int i = 0; i < Gifs.maxSlots(); i++) {
                    GifOption option = Gifs.slot(i);
                    if (option != null && option.name.equals(selected)) {
                        Gifs.select("slot" + i);
                        option.ensureLoading();
                        return;
                    }
                }
            }
        }
    }

    public static void pickCustom() {
        int slot = Gifs.firstFreeSlot();
        if (slot < 0) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.player != null) {
                mc.player.sendMessage(Text.literal("§c[anvil visual] §fДостигнут лимит (максимум 10 фонов)!"), false);
            }
        } else {
            new Thread(
                    () -> {
                        try {
                            MemoryStack stack = MemoryStack.stackPush();

                            String path;
                            try {
                                PointerBuffer filters = stack.mallocPointer(6);
                                filters.put(stack.UTF8("*.gif"));
                                filters.put(stack.UTF8("*.png"));
                                filters.put(stack.UTF8("*.jpg"));
                                filters.put(stack.UTF8("*.jpeg"));
                                filters.put(stack.UTF8("*.bmp"));
                                filters.put(stack.UTF8("*.webp"));
                                filters.flip();
                                path = TinyFileDialogs.tinyfd_openFileDialog(
                                    "Выберите свою GIF или картинку", "", filters, "Images / GIF (*.gif, *.png, *.jpg)", false
                                );
                            } catch (Throwable var6) {
                                if (stack != null) {
                                    try {
                                        stack.close();
                                    } catch (Throwable var5) {
                                        var6.addSuppressed(var5);
                                    }
                                }

                                throw var6;
                            }

                            if (stack != null) {
                                stack.close();
                            }

                            if (path == null || path.isEmpty()) {
                                return;
                            }

                            MediaProcessor$Prepared prepared = MediaProcessor.prepare(Path.of(path));
                            MediaProcessor.persist(prepared, slot);
                            MinecraftClient mcx = MinecraftClient.getInstance();
                            if (mcx != null) {
                                mcx.execute(
                                    () -> {
                                        GifOption o = MediaProcessor.buildOption(prepared, slot);
                                        Gifs.setSlot(slot, o);
                                        Gifs.select("slot" + slot);
                                        LiveUploader.requestFromImages(o, slot, prepared.frames.toArray(new BufferedImage[0]));
                                        updateModes();
                                        if (mcx.player != null) {
                                            mcx.player
                                                .sendMessage(
                                                    Text.literal(
                                                        "§a[anvil visual] §fФон §e"
                                                            + prepared.name
                                                            + " §fуспешно добавлен! ("
                                                            + prepared.frames.size()
                                                            + " кадров)"
                                                    ),
                                                    false
                                                );
                                        }
                                    }
                                );
                            }
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                    },
                    "anvil visual-Import"
                )
                .start();
        }
    }
}
