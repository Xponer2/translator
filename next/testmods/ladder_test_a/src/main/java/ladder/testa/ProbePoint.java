/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testa;

import net.neoforged.fml.IExtensionPoint;

/** An FML 4 extension point that is an ordinary class (FML 2 keeps records only). */
public final class ProbePoint implements IExtensionPoint {
    public final String value;

    public ProbePoint(String value) {
        this.value = value;
    }
}
