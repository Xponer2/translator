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
package io.github.xponer2.translator.component;

import io.github.xponer2.translator.util.BuiltInRegistries;
import io.github.xponer2.translator.util.NbtOps;
import io.github.xponer2.translator.util.Registries;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The components a stack has on top of its item's defaults (1.21 {@code net.minecraft.core.component.DataComponentPatch}).
 * 1.12.2 stacks can't remove a default component, so a patch only sets values. Its codec is the 1.21 one: a map of
 * component type id to value.
 */
public final class DataComponentPatch {
    public static final DataComponentPatch EMPTY = new DataComponentPatch(Collections.emptyMap());
    public static final Codec<DataComponentPatch> CODEC = Codec.unboundedMap(
            Codec.STRING.xmap(io.github.xponer2.translator.util.ResourceLocations::parse, ResourceLocation::toString), Codec.PASSTHROUGH)
            .comapFlatMap(DataComponentPatch::decode, DataComponentPatch::encode);

    /**
     * 1.21 {@code DataComponentPatch.STREAM_CODEC}: the components a stack sends (each type by its id, its value by the
     * type's stream codec; a type without one stays on the server). The carried one read the 1.21 patch's own map, which
     * this class hasn't: every stack with components a mod sent in its packets failed (Sophisticated Backpacks' contents).
     */
    public static final io.github.xponer2.translator.network.StreamCodec<io.github.xponer2.translator.network.RegistryFriendlyByteBuf, DataComponentPatch> STREAM_CODEC =
            io.github.xponer2.translator.network.StreamCodec.of(DataComponentPatch::write, DataComponentPatch::read);

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void write(io.github.xponer2.translator.network.RegistryFriendlyByteBuf buffer, DataComponentPatch patch) {
        var sent = new java.util.ArrayList<Map.Entry<DataComponentType<?>, Object>>();
        for (var entry : patch.values.entrySet()) {
            if (entry.getKey().streamCodec() != null && id(entry.getKey()) != null) {
                sent.add(entry);
            }
        }
        buffer.writeVarInt(sent.size());
        for (var entry : sent) {
            buffer.writeResourceLocation(id(entry.getKey()));
            ((io.github.xponer2.translator.network.StreamCodec) entry.getKey().streamCodec()).encode(buffer, entry.getValue());
        }
    }

    private static DataComponentPatch read(io.github.xponer2.translator.network.RegistryFriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count == 0) {
            return EMPTY;
        }
        var values = new LinkedHashMap<DataComponentType<?>, Object>();
        var registry = io.github.xponer2.translator.util.BuiltInRegistries.byKey(io.github.xponer2.translator.util.Registries.DATA_COMPONENT_TYPE);
        for (int i = 0; i < count; i++) {
            ResourceLocation id = buffer.readResourceLocation();
            DataComponentType<?> type = (DataComponentType<?>) registry.get(id);
            if (type == null || type.streamCodec() == null) {
                throw new IllegalStateException("Unknown data component type " + id);
            }
            values.put(type, type.streamCodec().decode(buffer));
        }
        return new DataComponentPatch(Collections.unmodifiableMap(values));
    }

    @Nullable
    private static ResourceLocation id(DataComponentType<?> type) {
        return io.github.xponer2.translator.util.BuiltInRegistries.byKey(io.github.xponer2.translator.util.Registries.DATA_COMPONENT_TYPE).getKey(type);
    }

    private final Map<DataComponentType<?>, Object> values;

    public DataComponentPatch(Map<DataComponentType<?>, Object> values) {
        this.values = values;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * The value of a type if the patch sets it, else null (1.21 returns an empty Optional for a removed component).
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> Optional<? extends T> get(DataComponentType<? extends T> type) {
        var value = (T) this.values.get(type);
        return value == null ? null : Optional.of(value);
    }

    public Set<Map.Entry<DataComponentType<?>, Optional<?>>> entrySet() {
        var entries = new LinkedHashMap<DataComponentType<?>, Optional<?>>();
        this.values.forEach((type, value) -> entries.put(type, Optional.of(value)));
        return entries.entrySet();
    }

    public int size() {
        return this.values.size();
    }

    public boolean isEmpty() {
        return this.values.isEmpty();
    }

    Map<DataComponentType<?>, Object> values() {
        return this.values;
    }

    private static DataResult<DataComponentPatch> decode(Map<ResourceLocation, Dynamic<?>> map) {
        var registry = BuiltInRegistries.byKey(Registries.DATA_COMPONENT_TYPE);
        var values = new LinkedHashMap<DataComponentType<?>, Object>();
        for (var entry : map.entrySet()) {
            var type = registry.get(entry.getKey());
            if (type == null) {
                return DataResult.error("Unknown data component type " + entry.getKey());
            }
            if (type.isTransient()) {
                return DataResult.error("Data component type " + entry.getKey() + " isn't persistent");
            }
            var value = type.codecOrThrow().parse(entry.getValue());
            if (!value.result().isPresent()) {
                return DataResult.error("Bad value of data component " + entry.getKey() + ": " + value.error().map(e -> e.message()).orElse(""));
            }
            values.put(type, value.result().get());
        }
        return DataResult.success(values.isEmpty() ? EMPTY : new DataComponentPatch(Collections.unmodifiableMap(values)));
    }

    private static Map<ResourceLocation, Dynamic<?>> encode(DataComponentPatch patch) {
        var map = new LinkedHashMap<ResourceLocation, Dynamic<?>>();
        patch.values.forEach((type, value) -> {
            var id = type.id();
            if (id != null && !type.isTransient()) {
                encodeValue(type, value).result().ifPresent(nbt -> map.put(id, new Dynamic<>(NbtOps.INSTANCE, nbt)));
            }
        });
        return map;
    }

    @SuppressWarnings("unchecked")
    private static <T> DataResult<net.minecraft.nbt.NBTBase> encodeValue(DataComponentType<T> type, Object value) {
        return type.codecOrThrow().encodeStart(NbtOps.INSTANCE, (T) value);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DataComponentPatch && ((DataComponentPatch) o).values.equals(this.values);
    }

    @Override
    public int hashCode() {
        return this.values.hashCode();
    }

    @Override
    public String toString() {
        return this.values.toString();
    }

    public static final class Builder {
        private final Map<DataComponentType<?>, Object> values = new LinkedHashMap<>();

        public <T> Builder set(DataComponentType<T> type, T value) {
            this.values.put(type, value);
            return this;
        }

        public <T> Builder set(Supplier<? extends DataComponentType<T>> type, T value) {
            return this.set(type.get(), value);
        }

        /**
         * Only drops a value set on this builder: 1.12.2 stacks can't remove a default component.
         */
        public Builder remove(DataComponentType<?> type) {
            this.values.remove(type);
            return this;
        }

        public DataComponentPatch build() {
            return this.values.isEmpty() ? EMPTY : new DataComponentPatch(Collections.unmodifiableMap(new LinkedHashMap<>(this.values)));
        }
    }
}
