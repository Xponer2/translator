/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.network;

import ladder.carried.net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * A 1.20.5 payload on 1.20.4 (an O row: every converted class implementing {@code type()} gets this interface). 1.20.5
 * names a payload by its {@code Type} and writes it with the {@code StreamCodec} it was registered with; 1.20.4's
 * NeoForge asks the payload itself for its {@code id()} and to {@code write} itself: both come from the type and the
 * registered codec.
 */
public interface TypedPayload extends CustomPacketPayload {
    CustomPacketPayload$Type<? extends CustomPacketPayload> type();

    @Override
    default ResourceLocation id() {
        return this.type().id();
    }

    @Override
    default void write(FriendlyByteBuf buffer) {
        Payloads.write(this, buffer);
    }
}
