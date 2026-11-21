/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.client;

import javax.annotation.Nullable;

import ladder.carried.net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;

/** 1.20.5's connecting with a server transfer's state on 1.20.4, which has no transfers (a fresh connection). */
public final class ConnectAdapters {
    private ConnectAdapters() {
    }

    /** {@code ConnectScreen.startConnecting(parent, minecraft, address, data, quickPlay, transfer)} */
    public static void startConnecting(Screen parent, Minecraft minecraft, ServerAddress address, ServerData data, boolean quickPlay,
            @Nullable TransferState transfer) {
        ConnectScreen.startConnecting(parent, minecraft, address, data, quickPlay);
    }
}
