/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.TooltipFlag;

/** client-only lookups (loaded only on a client) */
public final class ClientAccess {
    private ClientAccess() {
    }

    static RegistryAccess registries() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.level != null ? minecraft.level.registryAccess() : null;
    }

    /** the client's tooltip flag (advanced tooltips on or off) */
    public static TooltipFlag tooltipFlag() {
        return Minecraft.getInstance().options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL;
    }
}
