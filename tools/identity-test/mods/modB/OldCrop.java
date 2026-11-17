/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package modB;
/** Mod B (older version): its own crop, and code that reads the age of any crop (also mod A's). */
public class OldCrop extends srcb.CropBlock {
    @Override public int age() { return 5; }
    public static int ageOf(Object o) { return ((srcb.CropBlock) o).age(); }
}
