/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.network;

import java.util.UUID;

import javax.annotation.Nullable;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 1.20.5 made some {@code FriendlyByteBuf} helpers static, on any {@code ByteBuf} (that is what lets {@code ByteBufCodecs}
 * and the game's {@code STREAM_CODEC}s work on plain buffers). 1.20.4 has each of them as an instance method writing
 * the same bytes: the buffer is used as a FriendlyByteBuf (wrapped when it isn't one; the wrapper shares its indices).
 */
public final class ByteBufAdapters {
    private ByteBufAdapters() {
    }

    private static FriendlyByteBuf friendly(ByteBuf buf) {
        return buf instanceof FriendlyByteBuf friendly ? friendly : new FriendlyByteBuf(buf);
    }

    public static byte[] readByteArray(ByteBuf buf) {
        return friendly(buf).readByteArray();
    }

    public static byte[] readByteArray(ByteBuf buf, int maxLength) {
        return friendly(buf).readByteArray(maxLength);
    }

    public static void writeByteArray(ByteBuf buf, byte[] array) {
        friendly(buf).writeByteArray(array);
    }

    public static BlockPos readBlockPos(ByteBuf buf) {
        return friendly(buf).readBlockPos();
    }

    public static void writeBlockPos(ByteBuf buf, BlockPos pos) {
        friendly(buf).writeBlockPos(pos);
    }

    public static Vector3f readVector3f(ByteBuf buf) {
        return friendly(buf).readVector3f();
    }

    public static void writeVector3f(ByteBuf buf, Vector3f vector) {
        friendly(buf).writeVector3f(vector);
    }

    public static Quaternionf readQuaternion(ByteBuf buf) {
        return friendly(buf).readQuaternion();
    }

    public static void writeQuaternion(ByteBuf buf, Quaternionf quaternion) {
        friendly(buf).writeQuaternion(quaternion);
    }

    public static UUID readUUID(ByteBuf buf) {
        return friendly(buf).readUUID();
    }

    public static void writeUUID(ByteBuf buf, UUID uuid) {
        friendly(buf).writeUUID(uuid);
    }

    /** 1.21's {@code writeNbt(ByteBuf, Tag)}: null as an end tag, the network NBT form; 1.20.4's writeNbt(Tag) is the same. */
    public static void writeNbt(ByteBuf buf, @Nullable Tag tag) {
        friendly(buf).writeNbt(tag);
    }

    /** A compound or null, within 2 MiB, as 1.21's {@code readNbt(ByteBuf)} and 1.20.4's {@code readNbt()}. */
    @Nullable
    public static CompoundTag readNbt(ByteBuf buf) {
        return friendly(buf).readNbt();
    }

    @Nullable
    public static Tag readNbt(ByteBuf buf, NbtAccounter accounter) {
        return friendly(buf).readNbt(accounter);
    }
}
