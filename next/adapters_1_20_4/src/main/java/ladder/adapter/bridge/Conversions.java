/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package ladder.adapter.bridge;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * The value conversions of the generated bridges: where a newer version takes or gives a {@code Holder<X>} and 1.20.4
 * an {@code X} (mob effects, attributes, game events, sounds, potions, enchantments). Written once; the converter makes
 * a bridge per changed member that calls these.
 */
public final class Conversions {
    /** value class -> the built-in registry that holds its values (null: none, a direct holder is made) */
    private static final Map<Class<?>, Registry<?>> REGISTRIES = new ConcurrentHashMap<>();
    private static final Registry<?> NONE = BuiltInRegistries.REGISTRY;

    private Conversions() {
    }

    /** {@code Holder<X>} -> {@code X} */
    @Nullable
    public static Object unwrap(@Nullable Holder<?> holder) {
        return holder == null ? null : holder.value();
    }

    /** {@code X} -> the registry's own {@code Holder<X>} for it (a reference, as the newer versions give), else a direct one */
    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Holder<?> holder(@Nullable Object value) {
        if (value == null) {
            return null;
        }
        Registry registry = registryOf(value);
        if (registry != null) {
            return registry.wrapAsHolder(value);
        }
        return Holder.direct(value);
    }

    /** {@code X} -> its registry reference ({@code Holder.Reference<X>}), or null when it isn't registered */
    @Nullable
    public static Holder.Reference<?> reference(@Nullable Object value) {
        return holder(value) instanceof Holder.Reference<?> reference ? reference : null;
    }

    @Nullable
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Registry<?> registryOf(Object value) {
        // getResourceKey, not getKey: a defaulted registry's getKey gives its default key (minecraft:air) for any object
        Registry<?> found = REGISTRIES.computeIfAbsent(value.getClass(), type -> {
            for (Registry registry : BuiltInRegistries.REGISTRY) {
                if (registry.getResourceKey(value).isPresent()) {
                    return registry;
                }
            }
            return NONE;
        });
        if (found == NONE) {
            return null;
        }
        // a class's values can sit in one registry and not another value of it (two registries of one type): checked
        return ((Registry) found).getResourceKey(value).isPresent() ? found : null;
    }
}
