/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package modA;
import src.CropBlock;
/** Mod A's code that receives any crop (vanilla, its own, mod B's). */
public class Probe {
    public static String probe(Object o) {
        if (!(o instanceof CropBlock)) {
            return "not a crop";
        }
        CropBlock c = (CropBlock) o;
        CropBlock[] array = new CropBlock[1];
        array[0] = c;
        boolean reflective = CropBlock.class.isInstance(o);
        return "crop age=" + c.age() + " max=" + array[0].maxAge() + " grow=" + c.grow("x") + " reflect=" + reflective;
    }
    public static Object plainCrop() { return new CropBlock(); }
    public static Object[] arrayWith(Object vanilla) { CropBlock[] a = new CropBlock[1]; a[0] = (CropBlock) vanilla; return a; }
    public static boolean superclassIsCropBlock() { return MyCrop.class.getSuperclass() == CropBlock.class; }
}
