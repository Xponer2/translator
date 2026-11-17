/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.fml;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * FML 4's mod entry points on FML 2 (1.20.4). FML 4 constructs every {@code @Mod} class of a mod whose {@code dist}
 * holds the running side, and takes a mod with no {@code @Mod} class at all; FML 2 knows one class per mod and no
 * {@code dist}. The converter gives such a mod one generated entry class, whose constructor calls this with the mod's
 * own entry classes (and their sides).
 */
public final class ModEntries {
    private ModEntries() {
    }

    /**
     * @param anchor  the generated entry class (its loader loads the mod's classes)
     * @param entries one line per {@code @Mod} class: {@code name<TAB>sides} (sides: comma separated, empty for both)
     */
    public static void construct(Class<?> anchor, String entries, IEventBus bus, ModContainer container, Dist dist) {
        List<String[]> common = new ArrayList<>(), sided = new ArrayList<>();
        for (String line : entries.split("\n")) {
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split("\t", -1);
            (parts[1].isEmpty() ? common : sided).add(parts);
        }
        // the side-free classes first (the common setup), then the ones of this side
        for (List<String[]> group : List.of(common, sided)) {
            for (String[] entry : group) {
                if (!entry[1].isEmpty() && !List.of(entry[1].split(",")).contains(dist.name())) {
                    continue;
                }
                constructOne(anchor, entry[0], bus, container, dist);
            }
        }
    }

    private static void constructOne(Class<?> anchor, String name, IEventBus bus, ModContainer container, Dist dist) {
        try {
            Class<?> type = Class.forName(name, true, anchor.getClassLoader());
            Constructor<?>[] constructors = type.getConstructors();
            if (constructors.length != 1) {
                throw new IllegalStateException(name + " has " + constructors.length + " public constructors, a mod class needs one");
            }
            Class<?>[] params = constructors[0].getParameterTypes();
            Object[] args = new Object[params.length];
            for (int i = 0; i < params.length; i++) {
                if (params[i] == IEventBus.class) {
                    args[i] = bus;
                } else if (params[i].isInstance(container)) {
                    // ModContainer, FMLModContainer
                    args[i] = container;
                } else if (params[i] == Dist.class) {
                    args[i] = dist;
                } else {
                    throw new IllegalStateException(name + ": a mod constructor can't take " + params[i].getName());
                }
            }
            constructors[0].newInstance(args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new RuntimeException("constructing " + name, cause);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("constructing " + name, e);
        }
    }
}
