/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package fac;
/** Generated dispatchers: a new member called through the native type (vanilla objects have no facade). */
public final class Bridge {
    private Bridge() { }
    public static int maxAge(eng.Crop c) { return c instanceof Facade21 f ? f.maxAge() : 7; }
    public static int grow(eng.Crop c, CharSequence s) { return c instanceof Facade21 f ? f.grow(s) : c.grow(s.toString()); }
}
