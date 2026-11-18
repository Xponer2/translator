/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * NeoForge 21's {@code CommonHooks.validateComponent} on 1.20.4: in a development run, a component value must be a
 * record, an enum, a registry object, or override equals and hashCode (components are compared by value); in the
 * normal game it checks nothing, as in NeoForge 21.
 */
public final class ComponentChecks {
    private static final Set<Class<?>> CHECKED = ConcurrentHashMap.newKeySet();

    private ComponentChecks() {
    }

    public static void validateComponent(@Nullable Object component) {
        if (!SharedConstants.IS_RUNNING_IN_IDE || component == null) {
            return;
        }
        Class<?> type = component.getClass();
        if (CHECKED.contains(type)) {
            return;
        }
        if (type.isRecord() || type.isEnum() || overridesEqualsAndHashCode(type) || isRegistryObject(component)) {
            CHECKED.add(type);
            return;
        }
        throw new IllegalArgumentException("Data components must implement equals and hashCode. Keep in mind they must also be immutable. Problematic class: " + type);
    }

    private static boolean overridesEqualsAndHashCode(Class<?> type) {
        try {
            Method equals = type.getMethod("equals", Object.class);
            Method hashCode = type.getMethod("hashCode");
            return equals.getDeclaringClass() != Object.class && hashCode.getDeclaringClass() != Object.class;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to check for component equals and hashCode", e);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean isRegistryObject(Object value) {
        for (Registry registry : BuiltInRegistries.REGISTRY) {
            if (registry.containsValue(value)) {
                return true;
            }
        }
        return false;
    }
}
