/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.network;

import java.util.OptionalInt;
import java.util.function.Consumer;

import ladder.carried.net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;

/**
 * NeoForge 21.1's {@code openMenu(provider, writer)} with a writer of 1.20.5's registry buffer: 20.4's gives its writer a
 * plain FriendlyByteBuf, so the mod's writer gets it wrapped as a registry buffer (the same erased call, an X row).
 */
public final class MenuOpening {
    private MenuOpening() {
    }

    public static OptionalInt openMenu(ServerPlayer player, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> writer) {
        return player.openMenu(provider, wrap(writer));
    }

    public static OptionalInt openMenu(IPlayerExtension self, MenuProvider provider, Consumer<RegistryFriendlyByteBuf> writer) {
        return ((Player) self).openMenu(provider, wrap(writer));
    }

    private static Consumer<FriendlyByteBuf> wrap(Consumer<RegistryFriendlyByteBuf> writer) {
        return writer == null ? null : buffer -> writer.accept(Payloads.registryBuffer(buffer));
    }
}
