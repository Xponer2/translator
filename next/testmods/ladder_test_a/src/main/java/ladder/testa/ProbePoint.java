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
package ladder.testa;

import net.neoforged.fml.IExtensionPoint;

/** An FML 4 extension point that is an ordinary class (FML 2 keeps records only). */
public final class ProbePoint implements IExtensionPoint {
    public final String value;

    public ProbePoint(String value) {
        this.value = value;
    }
}
