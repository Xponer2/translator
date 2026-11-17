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
package fac;
/** Generated dispatchers: a new member called through the native type (vanilla objects have no facade). */
public final class Bridge {
    private Bridge() { }
    public static int maxAge(eng.Crop c) { return c instanceof Facade21 f ? f.maxAge() : 7; }
    public static int grow(eng.Crop c, CharSequence s) { return c instanceof Facade21 f ? f.grow(s) : c.grow(s.toString()); }
}
