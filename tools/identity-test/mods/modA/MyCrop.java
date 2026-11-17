/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package modA;
/** Mod A (newer version): its own crop overrides a native method, a new method and a method whose signature changed. */
public class MyCrop extends src.CropBlock {
    public MyCrop() { super(); }
    @Override public int age() { return 2; }
    @Override public int maxAge() { return 3; }
    @Override public int grow(CharSequence s) { return 10; }
}
