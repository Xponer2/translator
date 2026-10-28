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
package io.github.xponer2.translator.network;

import io.github.xponer2.translator.registry.Holder;
import io.github.xponer2.translator.registry.Registry;
import io.github.xponer2.translator.registry.ResourceKey;
import io.github.xponer2.translator.util.BuiltInRegistries;
import io.github.xponer2.translator.util.NbtOps;
import io.github.xponer2.translator.util.Registries;
import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;

/**
 * Stream codecs of common types (1.21 {@code net.minecraft.network.codec.ByteBufCodecs}), plus the codecs that 1.21
 * keeps on the types themselves ({@code ItemStack.STREAM_CODEC} -> {@link #ITEM_STACK},
 * {@code Ingredient.CONTENTS_STREAM_CODEC} -> {@link #INGREDIENT}, ...; the converter rewrites those uses).
 * Registry objects are written by id ({@link ResourceLocation}), not by their numeric id.
 */
public final class ByteBufCodecs {
    public static final int MAX_COLLECTION_SIZE = 65536;
    private static final String CODEC_VALUE = "v";

    public static final StreamCodec<ByteBuf, Boolean> BOOL = StreamCodec.of(ByteBuf::writeBoolean, ByteBuf::readBoolean);
    public static final StreamCodec<ByteBuf, Byte> BYTE = StreamCodec.of((buffer, value) -> buffer.writeByte(value), ByteBuf::readByte);
    public static final StreamCodec<ByteBuf, Short> SHORT = StreamCodec.of((buffer, value) -> buffer.writeShort(value), ByteBuf::readShort);
    public static final StreamCodec<ByteBuf, Integer> INT = StreamCodec.of(ByteBuf::writeInt, ByteBuf::readInt);
    public static final StreamCodec<ByteBuf, Long> LONG = StreamCodec.of(ByteBuf::writeLong, ByteBuf::readLong);
    public static final StreamCodec<ByteBuf, Float> FLOAT = StreamCodec.of(ByteBuf::writeFloat, ByteBuf::readFloat);
    public static final StreamCodec<ByteBuf, Double> DOUBLE = StreamCodec.of(ByteBuf::writeDouble, ByteBuf::readDouble);
    public static final StreamCodec<ByteBuf, Integer> VAR_INT = StreamCodec.of(
            (buffer, value) -> packet(buffer).writeVarInt(value), buffer -> packet(buffer).readVarInt());
    public static final StreamCodec<ByteBuf, Long> VAR_LONG = StreamCodec.of(
            (buffer, value) -> packet(buffer).writeVarLong(value), buffer -> packet(buffer).readVarLong());
    public static final StreamCodec<ByteBuf, String> STRING_UTF8 = StreamCodec.of(
            (buffer, value) -> packet(buffer).writeString(value), buffer -> packet(buffer).readString(RegistryFriendlyByteBuf.MAX_STRING_LENGTH));
    public static final StreamCodec<ByteBuf, byte[]> BYTE_ARRAY = StreamCodec.of(
            (buffer, value) -> packet(buffer).writeByteArray(value), buffer -> packet(buffer).readByteArray());

    public static final StreamCodec<ByteBuf, NBTTagCompound> COMPOUND_TAG = StreamCodec.of(
            (buffer, value) -> packet(buffer).writeCompoundTag(value), ByteBufCodecs::readCompoundTag);
    public static final StreamCodec<ByteBuf, Optional<NBTTagCompound>> OPTIONAL_COMPOUND_TAG = StreamCodec.of(
            (buffer, value) -> packet(buffer).writeCompoundTag(value.orElse(null)),
            buffer -> Optional.ofNullable(readCompoundTag(buffer)));
    public static final StreamCodec<ByteBuf, ITextComponent> COMPONENT = StreamCodec.of(
            (buffer, value) -> packet(buffer).writeTextComponent(value), buffer -> {
                try {
                    return packet(buffer).readTextComponent();
                } catch (IOException e) {
                    throw new DecoderException(e);
                }
            });

    public static final StreamCodec<ByteBuf, ResourceLocation> RESOURCE_LOCATION = StreamCodec.of(
            (buffer, value) -> packet(buffer).writeResourceLocation(value), buffer -> packet(buffer).readResourceLocation());
    public static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS = StreamCodec.of(
            (buffer, value) -> packet(buffer).writeBlockPos(value), buffer -> packet(buffer).readBlockPos());
    public static final StreamCodec<ByteBuf, ChunkPos> CHUNK_POS = StreamCodec.of((buffer, value) -> {
        buffer.writeInt(value.x);
        buffer.writeInt(value.z);
    }, buffer -> new ChunkPos(buffer.readInt(), buffer.readInt()));

    /**
     * Any item stack, also the empty one (1.21 {@code ItemStack.OPTIONAL_STREAM_CODEC}).
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> OPTIONAL_ITEM_STACK = StreamCodec.of(
            PacketBuffer::writeItemStack, buffer -> {
                try {
                    return buffer.readItemStack();
                } catch (IOException e) {
                    throw new DecoderException(e);
                }
            });
    /**
     * A non-empty item stack (1.21 {@code ItemStack.STREAM_CODEC}).
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, ItemStack> ITEM_STACK = StreamCodec.of((buffer, value) -> {
        if (value.isEmpty()) {
            throw new EncoderException("Empty ItemStack not allowed");
        }
        OPTIONAL_ITEM_STACK.encode(buffer, value);
    }, buffer -> {
        var stack = OPTIONAL_ITEM_STACK.decode(buffer);
        if (stack.isEmpty()) {
            throw new DecoderException("Empty ItemStack not allowed");
        }
        return stack;
    });
    public static final StreamCodec<RegistryFriendlyByteBuf, List<ItemStack>> ITEM_STACK_LIST = OPTIONAL_ITEM_STACK.apply(list());

    /**
     * An ingredient as the stacks it matches (1.21 {@code Ingredient.CONTENTS_STREAM_CODEC}).
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, Ingredient> INGREDIENT = StreamCodec.of((buffer, value) -> {
        var stacks = value.getMatchingStacks();
        buffer.writeVarInt(stacks.length);
        for (var stack : stacks) {
            OPTIONAL_ITEM_STACK.encode(buffer, stack);
        }
    }, buffer -> {
        int size = buffer.readVarInt();
        if (size == 0) {
            return Ingredient.EMPTY;
        }
        var stacks = new ItemStack[size];
        for (int i = 0; i < size; i++) {
            stacks[i] = OPTIONAL_ITEM_STACK.decode(buffer);
        }
        return Ingredient.fromStacks(stacks);
    });

    /**
     * A fluid stack, never null (1.21 {@code FluidStack.STREAM_CODEC}).
     */
    public static final StreamCodec<ByteBuf, FluidStack> FLUID_STACK = COMPOUND_TAG.map(tag -> {
        var stack = FluidStack.loadFluidStackFromNBT(tag);
        if (stack == null) {
            throw new DecoderException("Unknown fluid stack " + tag);
        }
        return stack;
    }, stack -> stack.writeToNBT(new NBTTagCompound()));

    public static final StreamCodec<RegistryFriendlyByteBuf, Item> ITEM = registry(Registries.ITEM);
    public static final StreamCodec<RegistryFriendlyByteBuf, Block> BLOCK = registry(Registries.BLOCK);
    public static final StreamCodec<ByteBuf, Fluid> FLUID = STRING_UTF8.map(name -> {
        var fluid = FluidRegistry.getFluid(name);
        if (fluid == null) {
            throw new DecoderException("Unknown fluid " + name);
        }
        return fluid;
    }, FluidRegistry::getFluidName);

    private ByteBufCodecs() { }

    /**
     * The 1.12.2 buffer methods on any buffer: the buffer itself if it is a {@link PacketBuffer}, else a view of it.
     */
    private static PacketBuffer packet(ByteBuf buffer) {
        return buffer instanceof PacketBuffer ? (PacketBuffer) buffer : new PacketBuffer(buffer);
    }

    private static NBTTagCompound readCompoundTag(ByteBuf buffer) {
        try {
            return packet(buffer).readCompoundTag();
        } catch (IOException e) {
            throw new DecoderException(e);
        }
    }

    private static void writeSize(ByteBuf buffer, int size, int maxSize) {
        if (size > maxSize) {
            throw new EncoderException(size + " elements exceed the maximum of " + maxSize);
        }
        packet(buffer).writeVarInt(size);
    }

    private static int readSize(ByteBuf buffer, int maxSize) {
        int size = packet(buffer).readVarInt();
        if (size > maxSize) {
            throw new DecoderException(size + " elements exceed the maximum of " + maxSize);
        }
        return size;
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> factory,
                                                                                             StreamCodec<? super B, V> codec, int maxSize) {
        return StreamCodec.of((buffer, value) -> {
            writeSize(buffer, value.size(), maxSize);
            for (V v : value) {
                codec.encode(buffer, v);
            }
        }, buffer -> {
            int size = readSize(buffer, maxSize);
            C collection = factory.apply(Math.min(size, MAX_COLLECTION_SIZE));
            for (int i = 0; i < size; i++) {
                collection.add(codec.decode(buffer));
            }
            return collection;
        });
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec<B, C> collection(IntFunction<C> factory,
                                                                                             StreamCodec<? super B, V> codec) {
        return collection(factory, codec, Integer.MAX_VALUE);
    }

    public static <B extends ByteBuf, V, C extends Collection<V>> StreamCodec.CodecOperation<B, V, C> collection(IntFunction<C> factory) {
        return codec -> collection(factory, codec);
    }

    public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list() {
        return codec -> collection(ArrayList::new, codec);
    }

    public static <B extends ByteBuf, V> StreamCodec.CodecOperation<B, V, List<V>> list(int maxSize) {
        return codec -> collection(ArrayList::new, codec, maxSize);
    }

    public static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(IntFunction<? extends M> factory,
                                                                                     StreamCodec<? super B, K> keyCodec,
                                                                                     StreamCodec<? super B, V> valueCodec, int maxSize) {
        return StreamCodec.of((buffer, value) -> {
            writeSize(buffer, value.size(), maxSize);
            for (Map.Entry<K, V> entry : value.entrySet()) {
                keyCodec.encode(buffer, entry.getKey());
                valueCodec.encode(buffer, entry.getValue());
            }
        }, buffer -> {
            int size = readSize(buffer, maxSize);
            M map = factory.apply(Math.min(size, MAX_COLLECTION_SIZE));
            for (int i = 0; i < size; i++) {
                K key = keyCodec.decode(buffer);
                map.put(key, valueCodec.decode(buffer));
            }
            return map;
        });
    }

    public static <B extends ByteBuf, K, V, M extends Map<K, V>> StreamCodec<B, M> map(IntFunction<? extends M> factory,
                                                                                     StreamCodec<? super B, K> keyCodec,
                                                                                     StreamCodec<? super B, V> valueCodec) {
        return map(factory, keyCodec, valueCodec, Integer.MAX_VALUE);
    }

    public static <B extends ByteBuf, V> StreamCodec<B, Optional<V>> optional(StreamCodec<B, V> codec) {
        return StreamCodec.of((buffer, value) -> {
            buffer.writeBoolean(value.isPresent());
            value.ifPresent(v -> codec.encode(buffer, v));
        }, buffer -> buffer.readBoolean() ? Optional.of(codec.decode(buffer)) : Optional.empty());
    }

    /**
     * A value by a (DataFixerUpper) codec, written as NBT.
     */
    public static <T> StreamCodec<ByteBuf, T> fromCodec(Codec<T> codec) {
        return COMPOUND_TAG.map(tag -> codec.parse(NbtOps.INSTANCE, tag.getTag(CODEC_VALUE))
                .getOrThrow(false, error -> { throw new DecoderException("Failed to decode: " + error); }), value -> {
            var tag = new NBTTagCompound();
            tag.setTag(CODEC_VALUE, codec.encodeStart(NbtOps.INSTANCE, value)
                    .getOrThrow(false, error -> { throw new EncoderException("Failed to encode " + value + ": " + error); }));
            return tag;
        });
    }

    /**
     * {@link #fromCodec}; there is no registry access to pass on 1.12.2.
     */
    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> fromCodecWithRegistries(Codec<T> codec) {
        return fromCodec(codec).cast();
    }

    /**
     * An enum constant by its ordinal (NeoForge {@code NeoForgeStreamCodecs.enumCodec}).
     */
    public static <B extends ByteBuf, E extends Enum<E>> StreamCodec<B, E> enumCodec(Class<E> enumClass) {
        E[] values = enumClass.getEnumConstants();
        return StreamCodec.of((buffer, value) -> packet(buffer).writeVarInt(value.ordinal()), buffer -> {
            int ordinal = packet(buffer).readVarInt();
            if (ordinal < 0 || ordinal >= values.length) {
                throw new DecoderException("No " + enumClass.getSimpleName() + " with ordinal " + ordinal);
            }
            return values[ordinal];
        });
    }

    /**
     * An object of a registry by id.
     */
    public static <T> StreamCodec<RegistryFriendlyByteBuf, T> registry(ResourceKey<? extends Registry<T>> registryKey) {
        return StreamCodec.of((buffer, value) -> {
            var id = BuiltInRegistries.byKey(registryKey).getKey(value);
            if (id == null) {
                throw new EncoderException("Unregistered object in " + registryKey.location() + ": " + value);
            }
            buffer.writeResourceLocation(id);
        }, buffer -> {
            var id = buffer.readResourceLocation();
            var value = BuiltInRegistries.byKey(registryKey).get(id);
            if (value == null) {
                throw new DecoderException("Unknown id in " + registryKey.location() + ": " + id);
            }
            return value;
        });
    }

    /**
     * A holder of a registered object by id.
     */
    public static <T> StreamCodec<RegistryFriendlyByteBuf, Holder<T>> holderRegistry(ResourceKey<? extends Registry<T>> registryKey) {
        StreamCodec<RegistryFriendlyByteBuf, T> values = registry(registryKey);
        return values.map(value -> BuiltInRegistries.byKey(registryKey).wrapAsHolder(value), Holder::value);
    }
}
