/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.fml;

import java.util.Optional;
import java.util.function.Supplier;

import ladder.rung1206.provided.IConfigScreenFactory;
import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.FMLEnvironment;

/** FML 4's {@code ModContainer} extension points on FML 3 (1.20.6): the config screen factory is 20.6's own type there. */
public final class FmlAdapters {
    private FmlAdapters() {
    }

    /** FML 4's {@code registerExtensionPoint(type, point)} */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerExtensionPoint(ModContainer container, Class<?> type, IExtensionPoint point) {
        registerExtensionPoint(container, type, (Supplier<?>) () -> point);
    }

    /** FML 4's {@code registerExtensionPoint(type, supplier)}: the config screen factory as 20.6's, any other as it is */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void registerExtensionPoint(ModContainer container, Class<?> type, Supplier<?> supplier) {
        if (type == IConfigScreenFactory.class) {
            if (FMLEnvironment.dist.isClient()) {
                ClientPoints.configScreen(container, supplier);
            }
            return;
        }
        container.registerExtensionPoint((Class) type, (Supplier) supplier);
    }

    /** FML 4's {@code getCustomExtension(type)}: a config screen factory is 20.6's, as NeoForge 21's */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional<?> getCustomExtension(ModContainer container, Class<?> type) {
        if (type == IConfigScreenFactory.class) {
            return FMLEnvironment.dist.isClient() ? ClientPoints.configScreen(container) : Optional.empty();
        }
        return container.getCustomExtension((Class) type);
    }
}
