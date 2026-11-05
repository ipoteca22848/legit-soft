package com.example.auravisuals.util.policy;

import com.example.auravisuals.module.Module;
import com.example.auravisuals.module.ModuleManager;
import com.example.auravisuals.util.server.FunTimeUtil;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

public final class HolyServerPolicy {
    private static final String AUTO_EAT = "AutoEat";
    private static final String AUTO_SWAP = "AutoSwap";
    private static final String FAST_EXP = "Fast Exp";
    private static final String ITEM_SCROLLER = "ItemScroller";
    private static final String SHIFT_TAP = "ShiftTap";
    private static final String TAPE_MOUSE = "TapeMouse";
    private static final String SEE_INVISIBLE = "SeeInvisible";
    private static final Set<String> TEMPORARILY_HIDDEN_CLICK_GUI_MODULES = Set.of("Events", "AnarchyFinder");
    private static final Set<String> SERVER_LIMITED_MODULES = Set.of("AutoEat", "AutoSwap", "Fast Exp", "ItemScroller", "ShiftTap", "TapeMouse", "SeeInvisible");
    private static final Map<String, Set<String>> SERVER_ALLOWED_MODULES = Map.ofEntries(
        Map.entry("holyworld", Set.of("AutoEat", "ItemScroller", "TapeMouse", "SeeInvisible")),
        Map.entry("spookytime", SERVER_LIMITED_MODULES),
        Map.entry("reallyworld", SERVER_LIMITED_MODULES),
        Map.entry("mineblaze", SERVER_LIMITED_MODULES),
        Map.entry("vime", SERVER_LIMITED_MODULES),
        Map.entry("dexland", SERVER_LIMITED_MODULES),
        Map.entry("prostotrainer", Set.of("ItemScroller")),
        Map.entry("spacetime", Set.of("ItemScroller", "Fast Exp", "AutoEat", "TapeMouse")),
        Map.entry("holytime", SERVER_LIMITED_MODULES),
        Map.entry("whiterise", Set.of("ItemScroller", "AutoEat")),
        Map.entry("skytime", Set.of("ItemScroller", "AutoEat", "AutoSwap")),
        Map.entry("ht", Set.of("ItemScroller")),
        Map.entry("saturn", Set.of("ItemScroller")),
        Map.entry("cakeworld", SERVER_LIMITED_MODULES),
        Map.entry("agerapvp", Set.of("ItemScroller")),
        Map.entry("topiacube", Set.of("ItemScroller", "AutoEat", "TapeMouse")),
        Map.entry("aresmine", Set.of("ItemScroller", "AutoEat", "AutoSwap"))
    );

    private HolyServerPolicy() {
    }

    public static boolean isHolyServerAddress() {
        return isAutoSwapBlockedOnCurrentServer();
    }

    public static boolean isAutoSwapBlockedOnCurrentServer() {
        return isBlockedOnCurrentServer("AutoSwap");
    }

    public static boolean isAllowedOnCurrentServer(String moduleName) {
        MinecraftClient mc = MinecraftClient.getInstance();
        return mc == null ? false : isAllowedOnCurrentServer(mc, moduleName);
    }

    public static boolean shouldHideAutoSwapFromClickGui(Module module) {
        return shouldHideModuleFromClickGui(module);
    }

    public static boolean shouldHideModuleFromClickGui(Module module) {
        return module != null && (TEMPORARILY_HIDDEN_CLICK_GUI_MODULES.contains(module.getName()) || isBlockedOnCurrentServer(module));
    }

    public static boolean shouldBlockAutoSwapKeybindToggle(Module module) {
        return shouldBlockKeybindToggle(module);
    }

    public static boolean shouldBlockKeybindToggle(Module module) {
        return isBlockedOnCurrentServer(module) && module != null && !module.isEnabled();
    }

    public static void enforceAutoSwapDisabledIfHolyServer() {
        enforceDisabledModulesForCurrentServer();
    }

    public static void enforceDisabledModulesForCurrentServer() {
        if (ModuleManager.INSTANCE != null) {
            for (Module module : ModuleManager.INSTANCE.getModules()) {
                if (isBlockedOnCurrentServer(module) && module.isEnabled()) {
                    module.setEnabled(false);
                }
            }
        }
    }

    private static boolean isBlockedOnCurrentServer(Module module) {
        return module != null && isBlockedOnCurrentServer(module.getName());
    }

    private static boolean isBlockedOnCurrentServer(String moduleName) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && moduleName != null) {
            return !SERVER_LIMITED_MODULES.contains(moduleName) ? false : !isAllowedOnCurrentServer(mc, moduleName);
        } else {
            return false;
        }
    }

    private static boolean isAllowedOnCurrentServer(MinecraftClient mc, String moduleName) {
        if (mc == null || moduleName == null) {
            return false;
        }

        if (!SERVER_LIMITED_MODULES.contains(moduleName)) {
            return true;
        }

        if ("SeeInvisible".equals(moduleName)) {
            ServerInfo entry = mc.getCurrentServerEntry();
            return entry != null && entry.address != null && entry.address.toLowerCase(Locale.ROOT).contains("holyworld");
        }

        if (mc.isInSingleplayer()) {
            return true;
        }

        if (FunTimeUtil.isFunTimeServer(mc)) {
            return true;
        }

        ServerInfo entry = mc.getCurrentServerEntry();
        if (entry == null) {
            return false;
        }

        String address = entry.address;
        if (address != null && !address.isEmpty()) {
            String normalizedAddress = address.toLowerCase(Locale.ROOT);

            for (Entry<String, Set<String>> rule : SERVER_ALLOWED_MODULES.entrySet()) {
                if (normalizedAddress.contains(rule.getKey()) && rule.getValue().contains(moduleName)) {
                    return true;
                }
            }

            return false;
        } else {
            return false;
        }
    }
}
