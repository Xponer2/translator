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
package modA;
/** Mod A (newer version): its own crop overrides a native method, a new method and a method whose signature changed. */
public class MyCrop extends src.CropBlock {
    public MyCrop() { super(); }
    @Override public int age() { return 2; }
    @Override public int maxAge() { return 3; }
    @Override public int grow(CharSequence s) { return 10; }
}
