/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.fml;

import java.util.Optional;
import java.util.function.Supplier;

import ladder.rung1206.provided.IConfigScreenFactory;
import net.neoforged.fml.ModContainer;

/** The client's extension points (loaded on the client only). */
final class ClientPoints {
    private ClientPoints() {
    }

    /** a mod's NeoForge 21 config screen factory, as 20.6's (made for the mod's container) */
    static void configScreen(ModContainer container, Supplier<?> supplier) {
        container.registerExtensionPoint(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class,
                (Supplier<net.neoforged.neoforge.client.gui.IConfigScreenFactory>) () -> (minecraft, parent) -> ((IConfigScreenFactory) supplier.get()).createScreen(container, parent));
    }

    /** 20.6's config screen factory of a mod, as NeoForge 21's */
    static Optional<?> configScreen(ModContainer container) {
        return container.getCustomExtension(net.neoforged.neoforge.client.gui.IConfigScreenFactory.class)
                .map(factory -> (IConfigScreenFactory) (mod, parent) -> factory.createScreen(net.minecraft.client.Minecraft.getInstance(), parent));
    }
}
