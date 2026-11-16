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

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import ladder.adapter.provided.IConfigScreenFactory;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;

/** FML 4's {@code ModContainer} calls on FML 2 (1.20.4). */
public final class FmlAdapters {
    private FmlAdapters() {
    }

    public static void registerConfig(ModContainer container, ModConfig.Type type, IConfigSpec<?> spec) {
        container.addConfig(new ModConfig(type, spec, container));
    }

    public static void registerConfig(ModContainer container, ModConfig.Type type, IConfigSpec<?> spec, String fileName) {
        container.addConfig(new ModConfig(type, spec, container, fileName));
    }

    /** FML 4's {@code registerExtensionPoint(type, point)} */
    public static void registerExtensionPoint(ModContainer container, Class<?> type, net.neoforged.fml.IExtensionPoint<?> point) {
        registerExtensionPoint(container, type, (Supplier<?>) () -> point);
    }

    /** container -> FML 4 extension point type -> its supplier (FML 2 keeps records only: FML 4's can be any class) */
    private static final Map<ModContainer, Map<Class<?>, Supplier<?>>> POINTS = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * FML 4's {@code registerExtensionPoint(type, supplier)}: the config screen factory as 1.20.4's; any other point is
     * kept here, where FML 4's {@code getCustomExtension} (redirected to {@link #getCustomExtension}) finds it.
     */
    public static void registerExtensionPoint(ModContainer container, Class<?> type, Supplier<?> supplier) {
        if (type == IConfigScreenFactory.class) {
            if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) {
                ClientPoints.configScreen(container, supplier);
            }
            return;
        }
        POINTS.computeIfAbsent(container, c -> new ConcurrentHashMap<>()).put(type, supplier);
    }

    /** FML 4's {@code getCustomExtension(type)}: a point registered as above, else FML 2's own. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Optional<?> getCustomExtension(ModContainer container, Class<?> type) {
        Map<Class<?>, Supplier<?>> own = POINTS.get(container);
        Supplier<?> supplier = own == null ? null : own.get(type);
        if (supplier != null) {
            return Optional.ofNullable(supplier.get());
        }
        return container.getCustomExtension((Class) type);
    }
}
