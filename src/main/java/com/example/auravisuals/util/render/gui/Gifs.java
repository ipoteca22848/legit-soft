package com.example.auravisuals.util.render.gui;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.fabricmc.loader.api.FabricLoader;

public final class Gifs {
    public static final String NONE = "none";
    private static final Path CONFIG = FabricLoader.getInstance().getConfigDir().resolve("anvil visual_livinggui.properties");
    private static final GifOption[] slots = new GifOption[10];
    private static String selectedId = "none";

    private Gifs() {
    }

    public static int maxSlots() {
        return slots.length;
    }

    public static GifOption slot(int i) {
        return i >= 0 && i < slots.length ? slots[i] : null;
    }

    public static int firstFreeSlot() {
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] == null) {
                return i;
            }
        }

        return -1;
    }

    public static String selectedId() {
        return selectedId;
    }

    public static GifOption current() {
        int s = slotIndexOf(selectedId);
        return s >= 0 ? slots[s] : null;
    }

    public static void select(String id) {
        for (int i = 0; i < slots.length; i++) {
            GifOption o = slots[i];
            if (o != null && o.isReady() && !("slot" + i).equals(id)) {
                MediaProcessor.deactivate(i);
                o.reset();
            }
        }

        selectedId = id;
        save();
    }

    public static void setSlot(int i, GifOption option) {
        if (i >= 0 && i < slots.length) {
            slots[i] = option;
        }
    }

    public static void deleteSlot(int i) {
        if (i >= 0 && i < slots.length) {
            MediaProcessor.deleteSlot(i);
            slots[i] = null;
            if (selectedId.equals(MediaProcessor.slotId(i))) {
                selectedId = "none";
            }

            save();
        }
    }

    public static int slotIndexOf(String id) {
        if (id != null && id.startsWith("slot")) {
            try {
                int i = Integer.parseInt(id.substring(4));
                if (i >= 0 && i < slots.length) {
                    return i;
                }
            } catch (NumberFormatException var2) {
            }
        }

        return -1;
    }

    public static List<String> getDisplayNames() {
        ArrayList<String> list = new ArrayList<>();
        list.add("None");

        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) {
                list.add(slots[i].name);
            }
        }

        return list;
    }

    public static void init() {
        for (int i = 0; i < slots.length; i++) {
            slots[i] = MediaProcessor.optionFromDisk(i);
        }

        try {
            if (Files.exists(CONFIG)) {
                Properties p = new Properties();

                try (InputStream in = Files.newInputStream(CONFIG)) {
                    p.load(in);
                }

                selectedId = p.getProperty("selected", "none");
            }
        } catch (Exception var6) {
        }

        int s = slotIndexOf(selectedId);
        if (s < 0 || slots[s] == null) {
            selectedId = "none";
        }
    }

    public static void save() {
        try {
            Properties p = new Properties();
            p.setProperty("selected", selectedId);

            try (OutputStream out = Files.newOutputStream(CONFIG)) {
                p.store(out, "anvil visual Living GUI selection");
            }
        } catch (Exception var6) {
        }
    }
}
