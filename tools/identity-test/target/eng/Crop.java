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
package eng;
/** The target game's own class (1.12.2's BlockCrops): vanilla objects are exactly this class. */
public class Crop {
    public int age() { return 1; }
    public int grow(String s) { return 0; }
    /** The engine calls the old signatures. */
    public static String engineCalls(Crop c) { return "engine age=" + c.age() + " grow=" + c.grow("x"); }
}
