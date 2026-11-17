/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;

import ladder.carried.net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

/**
 * 1.20.5's {@code DataComponentType} (a typed piece of an item's data). Registered in the component registry the
 * adapters make (ComponentRegistry); a stack's values live in its NBT (ComponentStore), the one place its data is kept.
 * The game's own components (DataComponents) read and write the stack's 1.20.4 data instead.
 */
public interface DataComponentType<T> {
    static <T> Builder<T> builder() {
        return new Builder<>();
    }

    @Nullable
    Codec<T> codec();

    default Codec<T> codecOrThrow() {
        Codec<T> codec = this.codec();
        if (codec == null) {
            throw new IllegalStateException(this + " is not a persistent component");
        }
        return codec;
    }

    default boolean isTransient() {
        return this.codec() == null;
    }

    @Nullable
    StreamCodec<?, T> streamCodec();

    /** a component of the game's own, kept in 1.20.4's data of the stack (DataComponents); null for a mod's */
    @Nullable
    default Vanilla<T> vanilla() {
        return null;
    }

    /** 1.20.5's {@code DataComponentType.Builder} */
    final class Builder<T> {
        @Nullable
        private Codec<T> codec;
        @Nullable
        private StreamCodec<?, T> streamCodec;

        public Builder<T> persistent(Codec<T> codec) {
            this.codec = codec;
            return this;
        }

        public Builder<T> networkSynchronized(StreamCodec<?, T> streamCodec) {
            this.streamCodec = streamCodec;
            return this;
        }

        /** 1.20.5 caches the encoded value; the store decodes each time (correctness first) */
        public Builder<T> cacheEncoding() {
            return this;
        }

        /** as 1.20.5: the stream codec is the codec's (over NBT, with the registries) unless one is given */
        public DataComponentType<T> build() {
            StreamCodec<?, T> stream = this.streamCodec != null ? this.streamCodec
                    : ComponentStreams.fromCodec(java.util.Objects.requireNonNull(this.codec, "Missing Codec for component"));
            return new Simple<>(this.codec, stream);
        }
    }

    /** a mod's component */
    record Simple<T>(@Nullable Codec<T> codec, @Nullable StreamCodec<?, T> streamCodec) implements DataComponentType<T> {
        @Override
        public String toString() {
            return ComponentRegistry.name(this);
        }

        @Override
        public boolean equals(Object other) {
            return this == other;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(this);
        }
    }

    /** how one of the game's components reads and writes a 1.20.4 stack */
    interface Vanilla<T> {
        /** the value, or null when the stack hasn't it */
        @Nullable
        T get(ItemStack stack);

        /** the item's own value (its prototype), or null */
        @Nullable
        T prototype(ItemStack stack);

        void set(ItemStack stack, @Nullable T value);
    }
}
