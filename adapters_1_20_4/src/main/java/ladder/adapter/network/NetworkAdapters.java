/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.network;

import com.mojang.serialization.DynamicOps;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.IdMap;
import net.minecraft.resources.RegistryOps;

/** Small members of 1.20.5's network code on 1.20.4 (receiver first), each in 1.20.4's own terms. */
public final class NetworkAdapters {
    private NetworkAdapters() {
    }

    /** 1.20.5's {@code IdMap.getIdOrThrow}: the id, or the same exception as 1.21 when the map hasn't the value. */
    public static <T> int getIdOrThrow(IdMap<T> map, T value) {
        int id = map.getId(value);
        if (id == -1) {
            throw new IllegalArgumentException("Can't find id for '" + value + "' in map " + map);
        }
        return id;
    }

    /** 1.20.5's {@code HolderLookup.Provider.createSerializationContext(ops)}: 1.20.4's {@code RegistryOps.create(ops, provider)}. */
    public static <V> RegistryOps<V> createSerializationContext(HolderLookup.Provider provider, DynamicOps<V> ops) {
        return RegistryOps.create(ops, provider);
    }
}
