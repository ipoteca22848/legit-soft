package com.example.auravisuals.module;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ModuleProfile {
    private static final Set<String> HIDDEN_MODULES = Set.of();

    private ModuleProfile() {
    }

    public static boolean shouldRegister(String moduleName) {
        return moduleName != null && !HIDDEN_MODULES.contains(moduleName);
    }

    public static Set<String> getHiddenModules() {
        return Collections.unmodifiableSet(new HashSet<>(HIDDEN_MODULES));
    }
}
