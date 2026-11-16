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
package ladder.adapter.component;

import com.mojang.serialization.Codec;

import net.minecraft.nbt.CompoundTag;

/** 1.20.5's {@code CustomData}: a stack's own NBT (on 1.20.4: the stack's tag, past the game's own keys). */
public final class CustomData {
    public static final CustomData EMPTY = new CustomData(new CompoundTag());
    public static final Codec<CustomData> CODEC = CompoundTag.CODEC.xmap(CustomData::new, data -> data.tag);
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
