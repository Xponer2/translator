/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.translator.registry;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * NeoForge registry callbacks ({@code Registry.addCallback}, {@code RegistryBuilder.onBake}): NeoForge bakes every
 * registry once registration is done and calls its bake callbacks (AE2 makes its list of key types there, without it
 * no ME storage works). The callbacks are the mods' NeoForge interfaces (carried), called by their method name.
 */
public final class RegistryCallbacks {
    private static final Map<Registry<?>, List<Object>> CALLBACKS = new java.util.IdentityHashMap<>();

    private RegistryCallbacks() {
    }

    static synchronized void add(Registry<?> registry, Object callback) {
        if (callback != null) {
            CALLBACKS.computeIfAbsent(registry, k -> new ArrayList<>()).add(callback);
        }
    }

    /** Every registry's bake callbacks (again after late registrations: a bake makes its lists anew). */
    public static synchronized void bakeAll() {
        for (MappedRegistry<?> registry : io.github.xponer2.translator.util.BuiltInRegistries.mappedRegistries()) {
            registry.runPendingCallbacks();
        }
        for (var entry : CALLBACKS.entrySet()) {
            for (Object callback : entry.getValue()) {
                Method bake = method(callback, "onBake", 1);
                if (bake == null) {
                    continue;
                }
                try {
                    bake.invoke(callback, entry.getKey());
                } catch (ReflectiveOperationException | RuntimeException e) {
                    Throwable cause = e instanceof java.lang.reflect.InvocationTargetException ? e.getCause() : e;
                    org.apache.logging.log4j.LogManager.getLogger("Translator").warn("A bake callback of {} failed: {}",
                            entry.getKey().key().location(), String.valueOf(cause));
                }
            }
        }
    }

    private static Method method(Object callback, String name, int parameters) {
        for (Method method : callback.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == parameters && !method.isBridge()) {
                method.setAccessible(true);
                return method;
            }
        }
        return null;
    }
}
