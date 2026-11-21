/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.attachment;

import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** the synced attachments of one holder, to a client (21.1's SyncAttachmentsPayload, as a NeoForge 20.4 payload) */
record SyncAttachmentsPayload(AttachmentSync.Target target, List<ResourceLocation> types, byte[] data) implements CustomPacketPayload {
    static final ResourceLocation ID = new ResourceLocation("ladder_adapters", "sync_attachments");

    static SyncAttachmentsPayload read(FriendlyByteBuf buf) {
        AttachmentSync.Target target = new AttachmentSync.Target(buf.readByte(), buf.readVarInt(), buf.readBlockPos());
        return new SyncAttachmentsPayload(target, buf.readList(FriendlyByteBuf::readResourceLocation), buf.readByteArray());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeByte(this.target.kind());
        buf.writeVarInt(this.target.entity());
        buf.writeBlockPos(this.target.pos());
        buf.writeCollection(this.types, FriendlyByteBuf::writeResourceLocation);
        buf.writeByteArray(this.data);
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
