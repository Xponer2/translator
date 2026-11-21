/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

/** Whether a registry builder asked for intrusive holders (MoreAdapters.withIntrusiveHolders; read by the mixin). */
public final class RegistryIntrusion {
    private RegistryIntrusion() {
    }

    public static boolean asked(Object builder) {
        return MoreAdapters.INTRUSIVE.contains(builder);
    }
}
