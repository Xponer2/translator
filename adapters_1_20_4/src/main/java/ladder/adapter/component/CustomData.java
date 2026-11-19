/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.component;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;

/** 1.20.5's {@code CustomData}: a stack's own NBT (on 1.20.4: the stack's tag, past the game's own keys). */
public final class CustomData {
    public static final CustomData EMPTY = new CustomData(new CompoundTag());
    public static final Codec<CustomData> CODEC = CompoundTag.CODEC.xmap(CustomData::new, data -> data.tag);
    /** 1.21's {@code ByteBufCodecs.COMPOUND_TAG.map(CustomData::new, data -> data.tag)} */
    public static final ladder.carried.net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, CustomData> STREAM_CODEC =
            new ladder.carried.net.minecraft.network.codec.StreamCodec<>() {
                @Override
                public CustomData decode(io.netty.buffer.ByteBuf buf) {
                    CompoundTag tag = new net.minecraft.network.FriendlyByteBuf(buf).readNbt();
                    return new CustomData(tag == null ? new CompoundTag() : tag);
                }

                @Override
                public void encode(io.netty.buffer.ByteBuf buf, CustomData data) {
                    new net.minecraft.network.FriendlyByteBuf(buf).writeNbt(data.tag);
                }
            };
    private final CompoundTag tag;

    private CustomData(CompoundTag tag) {
        this.tag = tag;
    }

    public static CustomData of(CompoundTag tag) {
        return new CustomData(tag.copy());
    }

    public CompoundTag copyTag() {
        return this.tag.copy();
    }

    public boolean isEmpty() {
        return this.tag.isEmpty();
    }

    public boolean contains(String key) {
        return this.tag.contains(key);
    }

    /** read only: a copy is the way to change it */
    CompoundTag unsafe() {
        return this.tag;
    }

    /** 1.20.5's {@code getUnsafe()}: the tag itself (read only) */
    public CompoundTag getUnsafe() {
        return this.tag;
    }

    /** 1.20.5's {@code update(changer)}: a copy changed */
    public CustomData update(java.util.function.Consumer<CompoundTag> changer) {
        CompoundTag copy = this.tag.copy();
        changer.accept(copy);
        return new CustomData(copy);
    }

    /** 1.20.5's {@code CustomData.update(type, stack, changer)}: the stack's value of that component changed */
    public static void update(DataComponentType<CustomData> type, net.minecraft.world.item.ItemStack stack, java.util.function.Consumer<CompoundTag> changer) {
        CustomData before = (CustomData) ComponentStore.get((DataComponentHolder) (Object) stack, type);
        CustomData after = (before == null ? EMPTY : before).update(changer);
        ComponentStore.set(stack, type, after.isEmpty() ? null : after);
    }

    /** 1.20.5's {@code CustomData.set(type, stack, tag)} */
    public static void set(DataComponentType<CustomData> type, net.minecraft.world.item.ItemStack stack, CompoundTag tag) {
        ComponentStore.set(stack, type, tag.isEmpty() ? null : of(tag));
    }

    /** 1.20.5's {@code read(ops, decoder)}: the data read by a map decoder */
    public <T> com.mojang.serialization.DataResult<T> read(com.mojang.serialization.DynamicOps<net.minecraft.nbt.Tag> ops,
            com.mojang.serialization.MapDecoder<T> decoder) {
        return ops.getMap(this.tag).flatMap(map -> decoder.decode(ops, map));
    }

    public <T> com.mojang.serialization.DataResult<T> read(com.mojang.serialization.MapDecoder<T> decoder) {
        return this.read(net.minecraft.nbt.NbtOps.INSTANCE, decoder);
    }

    /** 1.20.5's {@code update(ops, encoder, value)}: a copy with the value written into it */
    public <T> com.mojang.serialization.DataResult<CustomData> update(com.mojang.serialization.DynamicOps<net.minecraft.nbt.Tag> ops,
            com.mojang.serialization.MapEncoder<T> encoder, T value) {
        return encoder.encode(value, ops, ops.mapBuilder()).build(this.tag).map(tag -> new CustomData((CompoundTag) tag));
    }

    /** 1.20.5's {@code loadInto(entity)}: the entity's data with this merged in (keeping its UUID) */
    public void loadInto(net.minecraft.world.entity.Entity entity) {
        CompoundTag data = entity.saveWithoutId(new CompoundTag());
        java.util.UUID id = entity.getUUID();
        data.merge(this.tag);
        entity.load(data);
        entity.setUUID(id);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CustomData data && this.tag.equals(data.tag);
    }

    @Override
    public int hashCode() {
        return this.tag.hashCode();
    }

    @Override
    public String toString() {
        return this.tag.toString();
    }
}
