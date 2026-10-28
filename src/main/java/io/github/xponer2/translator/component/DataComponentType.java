package io.github.xponer2.translator.component;

import io.github.xponer2.translator.network.RegistryFriendlyByteBuf;
import io.github.xponer2.translator.network.StreamCodec;
import io.github.xponer2.translator.util.BuiltInRegistries;
import io.github.xponer2.translator.util.Registries;
import com.mojang.serialization.Codec;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;

/**
 * A type of data an item stack can carry (1.21 {@code net.minecraft.core.component.DataComponentType}). Registered
 * like on 1.21 ({@code DeferredRegister} of {@code Registries.DATA_COMPONENT_TYPE}); its registry id is the key of the
 * value in the stack's NBT ({@link StackComponents}). A type without a codec ({@code persistent}) isn't saved.
 * Some vanilla types keep their value where 1.12.2 keeps it ({@link StackStorage}, see {@link DataComponents}).
 */
public final class DataComponentType<T> {
    @Nullable
    private final Codec<T> codec;
    @Nullable
    private final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec;
    private final boolean cacheEncoding;
    @Nullable
    private final StackStorage<T> storage;

    private DataComponentType(@Nullable Codec<T> codec, @Nullable StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec,
                              boolean cacheEncoding, @Nullable StackStorage<T> storage) {
        this.codec = codec;
        this.streamCodec = streamCodec;
        this.cacheEncoding = cacheEncoding;
        this.storage = storage;
    }

    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    @Nullable
    public Codec<T> codec() {
        return this.codec;
    }

    public Codec<T> codecOrThrow() {
        if (this.codec == null) {
            throw new IllegalStateException(this + " is not a persistent component");
        }
        return this.codec;
    }

    @Nullable
    public StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec() {
        return this.streamCodec;
    }

    public boolean isTransient() {
        return this.codec == null;
    }

    public boolean cachesEncoding() {
        return this.cacheEncoding;
    }

    /**
     * Where 1.12.2 keeps this value on a stack, null for the components tag.
     */
    @Nullable
    public StackStorage<T> storage() {
        return this.storage;
    }

    /**
     * The registry id, null while the type isn't registered.
     */
    @Nullable
    public ResourceLocation id() {
        return BuiltInRegistries.byKey(Registries.DATA_COMPONENT_TYPE).getKey(this);
    }

    @Override
    public String toString() {
        var id = this.id();
        return id != null ? id.toString() : "DataComponentType@" + Integer.toHexString(this.hashCode());
    }

    public static final class Builder<T> {
        @Nullable
        private Codec<T> codec;
        @Nullable
        private StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec;
        private boolean cacheEncoding;
        @Nullable
        private StackStorage<T> storage;

        public Builder<T> persistent(Codec<T> codec) {
            this.codec = codec;
            return this;
        }

        public Builder<T> networkSynchronized(StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
            this.streamCodec = streamCodec;
            return this;
        }

        public Builder<T> cacheEncoding() {
            this.cacheEncoding = true;
            return this;
        }

        /**
         * Keeps the value where 1.12.2 keeps it instead of the components tag (not in 1.21).
         */
        public Builder<T> storage(StackStorage<T> storage) {
            this.storage = storage;
            return this;
        }

        public DataComponentType<T> build() {
            return new DataComponentType<>(this.codec, this.streamCodec, this.cacheEncoding, this.storage);
        }
    }
}
