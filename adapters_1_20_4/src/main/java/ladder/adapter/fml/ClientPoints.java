/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.fml;

import java.util.function.Supplier;

import ladder.adapter.provided.IConfigScreenFactory;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.ConfigScreenHandler;

/** The client's extension points (loaded on the client only). */
final class ClientPoints {
    private ClientPoints() {
    }

    static void configScreen(ModContainer container, Supplier<?> supplier) {
        container.registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> ((IConfigScreenFactory) supplier.get()).createScreen(container, parent)));
    }
}
