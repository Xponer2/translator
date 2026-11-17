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
package ladder.adapter.bridge;

import net.minecraft.core.RegistryAccess;

/** The client's registries when there is no server here (a client connected to a server). */
final class ClientRegistries {
    private ClientRegistries() {
    }

    static RegistryAccess access() {
        var connection = net.neoforged.fml.loading.FMLEnvironment.dist.isClient() ? Client.connection() : null;
        if (connection != null) {
            return connection;
        }
        return RegistryAccess.fromRegistryOfRegistries(net.minecraft.core.registries.BuiltInRegistries.REGISTRY);
    }

    /** Loaded on the client only. */
    private static final class Client {
        static RegistryAccess connection() {
            var listener = net.minecraft.client.Minecraft.getInstance().getConnection();
            return listener == null ? null : listener.registryAccess();
        }
    }
}
