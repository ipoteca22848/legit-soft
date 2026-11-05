package com.example.auravisuals.util.web;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.util.Identifier;

public final class WebBrowserBridge {
    private static Boolean present;
    private static Class<?> mcefClass;
    private static Method createBrowser;
    private static Method isInitialized;

    private WebBrowserBridge() {
    }

    private static boolean resolve() {
        if (present != null) {
            return present;
        }

        try {
            mcefClass = Class.forName("com.cinemamod.mcef.MCEF");

            try {
                isInitialized = mcefClass.getMethod("isInitialized");
            } catch (NoSuchMethodException var1) {
            }

            createBrowser = mcefClass.getMethod("createBrowser", String.class, boolean.class);
            present = true;
        } catch (Throwable var2) {
            present = false;
        }

        return present;
    }

    public static boolean available() {
        if (!resolve()) {
            return false;
        }

        if (isInitialized == null) {
            return true;
        }

        try {
            Object r = isInitialized.invoke(null);
            return r instanceof Boolean && (Boolean)r;
        } catch (Throwable var1) {
            return false;
        }
    }

    public static Object create(String url, int width, int height) {
        if (!available()) {
            return null;
        }

        try {
            Object browser = createBrowser.invoke(null, url, false);
            resize(browser, width, height);
            disableCursorChange(browser);
            return browser;
        } catch (Throwable var4) {
            return null;
        }
    }

    public static void disableCursorChange(Object browser) {
        if (browser != null) {
            try {
                Class<?> listener = Class.forName("com.cinemamod.mcef.listeners.MCEFCursorChangeListener");
                Object proxy = Proxy.newProxyInstance(listener.getClassLoader(), new Class[]{listener}, (p, method, args) -> {
                    Class<?> rt = method.getReturnType();
                    if (rt == boolean.class) {
                        return false;
                    } else {
                        return rt == int.class ? 0 : null;
                    }
                });
                Method m = browser.getClass().getMethod("setCursorChangeListener", listener);
                m.invoke(browser, proxy);
            } catch (Throwable var4) {
            }
        }
    }

    public static Identifier textureIdentifier(Object browser) {
        if (browser == null) {
            return null;
        }

        try {
            Object id = invoke(browser, "getTextureIdentifier");
            return id instanceof Identifier ? (Identifier)id : null;
        } catch (Throwable var2) {
            return null;
        }
    }

    public static boolean isTextureReady(Object browser) {
        if (browser == null) {
            return false;
        }

        try {
            Object r = invoke(browser, "isTextureReady");
            return r instanceof Boolean && (Boolean)r;
        } catch (Throwable var2) {
            return false;
        }
    }

    public static int textureId(Object browser) {
        if (browser == null) {
            return 0;
        }

        try {
            Object renderer = invoke(browser, "getRenderer");
            if (renderer == null) {
                return 0;
            }

            Object id = invoke(renderer, "getTextureID");
            if (id == null) {
                id = invoke(renderer, "getTextureId");
            }

            return id instanceof Integer ? (Integer)id : 0;
        } catch (Throwable var3) {
            return 0;
        }
    }

    public static void resize(Object browser, int w, int h) {
        call(browser, "resize", new Class[]{int.class, int.class}, w, h);
    }

    public static void mouseMove(Object browser, int x, int y) {
        call(browser, "sendMouseMove", new Class[]{int.class, int.class}, x, y);
    }

    public static void mousePress(Object browser, int x, int y, int button) {
        call(browser, "sendMousePress", new Class[]{int.class, int.class, int.class}, x, y, button);
    }

    public static void mouseRelease(Object browser, int x, int y, int button) {
        call(browser, "sendMouseRelease", new Class[]{int.class, int.class, int.class}, x, y, button);
    }

    public static void mouseWheel(Object browser, int x, int y, double delta) {
        if (!call(browser, "sendMouseWheel", new Class[]{int.class, int.class, double.class, int.class}, x, y, delta, 0)) {
            call(browser, "sendMouseWheel", new Class[]{int.class, int.class, double.class}, x, y, delta);
        }
    }

    public static void keyPress(Object browser, int keyCode, int scanCode, int modifiers) {
        call(browser, "sendKeyPress", new Class[]{int.class, long.class, int.class}, keyCode, (long)scanCode, modifiers);
    }

    public static void keyRelease(Object browser, int keyCode, int scanCode, int modifiers) {
        call(browser, "sendKeyRelease", new Class[]{int.class, long.class, int.class}, keyCode, (long)scanCode, modifiers);
    }

    public static void keyTyped(Object browser, char c, int modifiers) {
        call(browser, "sendKeyTyped", new Class[]{char.class, int.class}, c, modifiers);
    }

    public static void setFocus(Object browser, boolean focus) {
        call(browser, "setFocus", new Class[]{boolean.class}, focus);
    }

    public static void loadUrl(Object browser, String url) {
        if (!call(browser, "loadURL", new Class[]{String.class}, url)) {
            call(browser, "loadUrl", new Class[]{String.class}, url);
        }
    }

    public static void close(Object browser) {
        call(browser, "close", new Class[0]);
    }

    private static Object invoke(Object target, String name) throws Exception {
        Method m = target.getClass().getMethod(name);
        return m.invoke(target);
    }

    private static boolean call(Object target, String name, Class<?>[] sig, Object... args) {
        if (target == null) {
            return false;
        }

        try {
            Method m = target.getClass().getMethod(name, sig);
            m.invoke(target, args);
            return true;
        } catch (Throwable var5) {
            return false;
        }
    }
}
