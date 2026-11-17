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
