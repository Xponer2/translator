/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.extensions.IKeyMappingExtension;

/** NeoForge 21.1's client-only additions on 1.21 (loaded only on a client) */
public final class ClientAdapters {
    private ClientAdapters() {
    }

    /** {@code IKeyMappingExtension.getDisplayName()}: 21.1's default, the mapping's name translated */
    public static Component getDisplayName(IKeyMappingExtension self) {
        return Component.translatable(((KeyMapping) self).getName());
    }
}
