/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * NeoForge 21.1's {@code IMenuProviderExtension.writeClientSideData(menu, buffer)}: data a menu provider sends with the
 * menu it opens (21.0 sends only what the opener's writer gives). A converted mod's provider that writes some gets this
 * interface (an O row); ServerPlayerMenuMixin writes it before the opener's data, as 21.1 does.
 */
public interface MenuDataApi {
    default void writeClientSideData(AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
    }

    /** the O row's defaults (a super call): nothing */
    static void writeNothing(Object self, AbstractContainerMenu menu, RegistryFriendlyByteBuf buffer) {
    }
}
