/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.attachment;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** the synced attachments of one holder, to a client (21.1's SyncAttachmentsPayload, as a payload of the rung's own) */
record SyncAttachmentsPayload(AttachmentSync.Target target, List<ResourceLocation> types, byte[] data) implements CustomPacketPayload {
    static final Type<SyncAttachmentsPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("ladder_adapters_1_21", "sync_attachments"));
    static final StreamCodec<FriendlyByteBuf, SyncAttachmentsPayload> CODEC = StreamCodec.of(SyncAttachmentsPayload::write, SyncAttachmentsPayload::read);

    private static void write(FriendlyByteBuf buf, SyncAttachmentsPayload payload) {
        buf.writeByte(payload.target.kind());
        buf.writeVarInt(payload.target.entity());
        buf.writeBlockPos(payload.target.pos());
        buf.writeCollection(payload.types, FriendlyByteBuf::writeResourceLocation);
        buf.writeByteArray(payload.data);
    }

    private static SyncAttachmentsPayload read(FriendlyByteBuf buf) {
        AttachmentSync.Target target = new AttachmentSync.Target(buf.readByte(), buf.readVarInt(), buf.readBlockPos());
        return new SyncAttachmentsPayload(target, buf.readList(FriendlyByteBuf::readResourceLocation), buf.readByteArray());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
