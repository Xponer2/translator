/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.client;

import net.minecraft.client.Minecraft;

/** Client values 1.21 passes where 1.20.6 doesn't (A rows of client constructors: the client itself). */
public final class ClientArgs {
    private ClientArgs() {
    }

    /** the client (1.21's VideoSettingsScreen takes it, 1.20.6's finds it) */
    public static Minecraft minecraft() {
        return Minecraft.getInstance();
    }
}
