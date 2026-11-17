/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package fac;
/** Upper rung facade (newer -> older), already composed down onto Facade0: adds the new API. */
public class Facade21 extends Facade0 {
    public Facade21() { }
    /** New method: vanilla default. */
    public int maxAge() { return 7; }
    /** New signature: by default the old behaviour. */
    public int grow(CharSequence s) { return super.grow(s.toString()); }
    /** The engine calls the old signature: it reaches the mod's override of the new one. */
    @Override public int grow(String s) { return this.grow((CharSequence) s); }
}
