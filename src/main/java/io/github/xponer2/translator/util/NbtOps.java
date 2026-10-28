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
package io.github.xponer2.translator.util;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagByte;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagDouble;
import net.minecraft.nbt.NBTTagFloat;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagIntArray;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagLong;
import net.minecraft.nbt.NBTTagShort;
import net.minecraft.nbt.NBTTagString;
import net.minecraftforge.common.util.Constants;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * DataFixerUpper ops on 1.12.2 NBT (1.21 {@code net.minecraft.nbt.NbtOps}): encodes and decodes codecs to and from
 * {@link NBTBase}. A list with elements of different types stores each element in a compound under the key "",
 * like 1.21 does.
 */
public final class NbtOps implements DynamicOps<NBTBase> {
    public static final NbtOps INSTANCE = new NbtOps();
    // NBTTagEnd has no public constructor; an empty list hands one out for a missing index
    private static final NBTBase END = new NBTTagList().get(0);
    private static final String WRAPPER = "";

    private NbtOps() { }

    @Override
    public NBTBase empty() {
        return END;
    }

    @Override
    public <U> U convertTo(DynamicOps<U> outOps, NBTBase input) {
        switch (input.getId()) {
            case Constants.NBT.TAG_END:
                return outOps.empty();
            case Constants.NBT.TAG_BYTE:
                return outOps.createByte(((NBTPrimitive) input).getByte());
            case Constants.NBT.TAG_SHORT:
                return outOps.createShort(((NBTPrimitive) input).getShort());
            case Constants.NBT.TAG_INT:
                return outOps.createInt(((NBTPrimitive) input).getInt());
            case Constants.NBT.TAG_LONG:
                return outOps.createLong(((NBTPrimitive) input).getLong());
            case Constants.NBT.TAG_FLOAT:
                return outOps.createFloat(((NBTPrimitive) input).getFloat());
            case Constants.NBT.TAG_DOUBLE:
                return outOps.createDouble(((NBTPrimitive) input).getDouble());
            case Constants.NBT.TAG_BYTE_ARRAY:
                return outOps.createByteList(ByteBuffer.wrap(((NBTTagByteArray) input).getByteArray()));
            case Constants.NBT.TAG_STRING:
                return outOps.createString(((NBTTagString) input).getString());
            case Constants.NBT.TAG_LIST:
                return this.convertList(outOps, input);
            case Constants.NBT.TAG_COMPOUND:
                return this.convertMap(outOps, input);
            case Constants.NBT.TAG_INT_ARRAY:
                return outOps.createIntList(IntStream.of(((NBTTagIntArray) input).getIntArray()));
            default:
                throw new IllegalStateException("Unknown NBT tag type " + input.getId());
        }
    }

    @Override
    public DataResult<Number> getNumberValue(NBTBase input) {
        switch (input.getId()) {
            case Constants.NBT.TAG_BYTE:
                return DataResult.success(((NBTPrimitive) input).getByte());
            case Constants.NBT.TAG_SHORT:
                return DataResult.success(((NBTPrimitive) input).getShort());
            case Constants.NBT.TAG_INT:
                return DataResult.success(((NBTPrimitive) input).getInt());
            case Constants.NBT.TAG_LONG:
                return DataResult.success(((NBTPrimitive) input).getLong());
            case Constants.NBT.TAG_FLOAT:
                return DataResult.success(((NBTPrimitive) input).getFloat());
            case Constants.NBT.TAG_DOUBLE:
                return DataResult.success(((NBTPrimitive) input).getDouble());
            default:
                return DataResult.error("Not a number: " + input);
        }
    }

    @Override
    public NBTBase createNumeric(Number i) {
        return new NBTTagDouble(i.doubleValue());
    }

    @Override
    public NBTBase createByte(byte value) {
        return new NBTTagByte(value);
    }

    @Override
    public NBTBase createShort(short value) {
        return new NBTTagShort(value);
    }

    @Override
    public NBTBase createInt(int value) {
        return new NBTTagInt(value);
    }

    @Override
    public NBTBase createLong(long value) {
        return new NBTTagLong(value);
    }

    @Override
    public NBTBase createFloat(float value) {
        return new NBTTagFloat(value);
    }

    @Override
    public NBTBase createDouble(double value) {
        return new NBTTagDouble(value);
    }

    @Override
    public NBTBase createBoolean(boolean value) {
        return new NBTTagByte((byte) (value ? 1 : 0));
    }

    @Override
    public DataResult<String> getStringValue(NBTBase input) {
        return input instanceof NBTTagString ? DataResult.success(((NBTTagString) input).getString()) : DataResult.error("Not a string: " + input);
    }

    @Override
    public NBTBase createString(String value) {
        return new NBTTagString(value);
    }

    @Override
    public DataResult<NBTBase> mergeToList(NBTBase list, NBTBase value) {
        return this.mergeToList(list, java.util.Collections.singletonList(value));
    }

    @Override
    public DataResult<NBTBase> mergeToList(NBTBase list, List<NBTBase> values) {
        List<NBTBase> elements = new ArrayList<>();
        if (list.getId() == Constants.NBT.TAG_LIST) {
            var tags = (NBTTagList) list;
            for (int i = 0; i < tags.tagCount(); i++) {
                elements.add(tags.get(i));
            }
        } else if (list.getId() != Constants.NBT.TAG_END) {
            return DataResult.error("Not a list: " + list, list);
        }
        elements.addAll(values);
        return DataResult.success(this.createList(elements.stream()));
    }

    @Override
    public DataResult<NBTBase> mergeToMap(NBTBase map, NBTBase key, NBTBase value) {
        if (!(map instanceof NBTTagCompound) && map.getId() != Constants.NBT.TAG_END) {
            return DataResult.error("Not a map: " + map, map);
        }
        if (!(key instanceof NBTTagString)) {
            return DataResult.error("Key is not a string: " + key, map);
        }
        var result = map instanceof NBTTagCompound ? ((NBTTagCompound) map).copy() : new NBTTagCompound();
        result.setTag(((NBTTagString) key).getString(), value);
        return DataResult.success(result);
    }

    @Override
    public DataResult<Stream<Pair<NBTBase, NBTBase>>> getMapValues(NBTBase input) {
        if (!(input instanceof NBTTagCompound)) {
            return DataResult.error("Not a map: " + input);
        }
        var compound = (NBTTagCompound) input;
        return DataResult.success(compound.getKeySet().stream().map(key -> Pair.of(this.createString(key), compound.getTag(key))));
    }

    @Override
    public NBTBase createMap(Stream<Pair<NBTBase, NBTBase>> map) {
        var compound = new NBTTagCompound();
        map.forEach(pair -> compound.setTag(((NBTTagString) pair.getFirst()).getString(), pair.getSecond()));
        return compound;
    }

    @Override
    public DataResult<Stream<NBTBase>> getStream(NBTBase input) {
        if (input instanceof NBTTagList) {
            var list = (NBTTagList) input;
            var elements = new ArrayList<NBTBase>(list.tagCount());
            for (int i = 0; i < list.tagCount(); i++) {
                elements.add(unwrap(list.get(i)));
            }
            return DataResult.success(elements.stream());
        }
        if (input instanceof NBTTagByteArray) {
            var bytes = ((NBTTagByteArray) input).getByteArray();
            return DataResult.success(IntStream.range(0, bytes.length).mapToObj(i -> new NBTTagByte(bytes[i])));
        }
        if (input instanceof NBTTagIntArray) {
            return DataResult.success(IntStream.of(((NBTTagIntArray) input).getIntArray()).mapToObj(NBTTagInt::new));
        }
        return DataResult.error("Not a list: " + input);
    }

    @Override
    public NBTBase createList(Stream<NBTBase> input) {
        List<NBTBase> elements = input.collect(Collectors.toList());
        var list = new NBTTagList();
        boolean mixed = elements.stream().map(NBTBase::getId).distinct().count() > 1;
        for (NBTBase element : elements) {
            if (mixed) {
                var wrapper = new NBTTagCompound();
                wrapper.setTag(WRAPPER, element);
                list.appendTag(wrapper);
            } else {
                list.appendTag(element);
            }
        }
        return list;
    }

    @Override
    public DataResult<ByteBuffer> getByteBuffer(NBTBase input) {
        return input instanceof NBTTagByteArray ? DataResult.success(ByteBuffer.wrap(((NBTTagByteArray) input).getByteArray()))
                : DynamicOps.super.getByteBuffer(input);
    }

    @Override
    public NBTBase createByteList(ByteBuffer input) {
        var bytes = new byte[input.remaining()];
        input.duplicate().get(bytes);
        return new NBTTagByteArray(bytes);
    }

    @Override
    public DataResult<IntStream> getIntStream(NBTBase input) {
        return input instanceof NBTTagIntArray ? DataResult.success(IntStream.of(((NBTTagIntArray) input).getIntArray()))
                : DynamicOps.super.getIntStream(input);
    }

    @Override
    public NBTBase createIntList(IntStream input) {
        return new NBTTagIntArray(input.toArray());
    }

    @Override
    public NBTBase remove(NBTBase input, String key) {
        if (input instanceof NBTTagCompound) {
            var copy = ((NBTTagCompound) input).copy();
            copy.removeTag(key);
            return copy;
        }
        return input;
    }

    @Override
    public String toString() {
        return "NBT";
    }

    private static NBTBase unwrap(NBTBase element) {
        if (element instanceof NBTTagCompound) {
            var compound = (NBTTagCompound) element;
            if (compound.getSize() == 1 && compound.hasKey(WRAPPER)) {
                return compound.getTag(WRAPPER);
            }
        }
        return element;
    }
}
