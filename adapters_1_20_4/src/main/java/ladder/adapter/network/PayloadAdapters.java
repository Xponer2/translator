/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.network;

import ladder.carried.net.minecraft.network.protocol.common.custom.CustomPacketPayload$Type;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** {@code CustomPacketPayload.type()} called through the interface (receiver first). */
public final class PayloadAdapters {
    private PayloadAdapters() {
    }

    /** A converted payload's own type; a 1.20.4 payload's type made of its id. */
    public static CustomPacketPayload$Type<? extends CustomPacketPayload> type(CustomPacketPayload payload) {
        return payload instanceof TypedPayload typed ? typed.type() : new CustomPacketPayload$Type<>(payload.id());
    }
}
