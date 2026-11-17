/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
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
