/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whether a converted mod's class overrides a method of the newer API (useItemOn, appendHoverText with a
 * TooltipContext, ...): the 1.20.4 call sites call it then, else the game's own method as before.
 */
public final class Dispatch {
    private static final Map<String, Boolean> DECLARES = new ConcurrentHashMap<>();

    private Dispatch() {
    }

    /** A class between this one and the game's own classes (net.minecraft) declares the method. */
    public static boolean declares(Class<?> type, String name, Class<?>... parameters) {
        StringBuilder key = new StringBuilder(type.getName()).append('#').append(name);
        for (Class<?> p : parameters) {
            key.append(',').append(p.getName());
        }
        return DECLARES.computeIfAbsent(key.toString(), k -> {
            for (Class<?> c = type; c != null && !c.getName().startsWith("net.minecraft.") && !c.getName().startsWith("ladder.adapter."); c = c.getSuperclass()) {
                try {
                    c.getDeclaredMethod(name, parameters);
                    return true;
                } catch (NoSuchMethodException e) {
                    // its parent
                }
            }
            return false;
        });
    }
}
