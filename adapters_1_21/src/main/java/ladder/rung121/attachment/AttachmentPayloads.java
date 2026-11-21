/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.attachment;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** the attachment sync payload's registration (optional: a server or client without the adapters ignores it) */
public final class AttachmentPayloads {
    private AttachmentPayloads() {
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("ladder_adapters_1_21").optional().playToClient(SyncAttachmentsPayload.TYPE, SyncAttachmentsPayload.CODEC,
                (payload, context) -> context.enqueueWork(() -> AttachmentSync.receive(payload, context.player().level())));
    }
}
