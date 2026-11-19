/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.block;

import com.mojang.authlib.minecraft.MinecraftSessionService;

/** The client's session service (a class of its own: loaded on the client only). */
final class ClientProfiles {
    private ClientProfiles() {
    }

    static MinecraftSessionService sessionService() {
        return net.minecraft.client.Minecraft.getInstance().getMinecraftSessionService();
    }
}
