/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
import java.lang.reflect.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

/** Loads the target classes and the rewritten mods in one class loader and checks every identity case. */
public class Check {
    static int failed;

    static void expect(String what, Object got, Object want) {
        boolean ok = Objects.equals(got, want);
        if (!ok) {
            failed++;
        }
        System.out.println((ok ? "PASS " : "FAIL ") + what + ": " + got + (ok ? "" : "   (expected " + want + ")"));
    }

    public static void main(String[] args) throws Exception {
        URL[] path = {Paths.get(args[0]).toUri().toURL(), Paths.get(args[1]).toUri().toURL()};
        ClassLoader loader = new URLClassLoader(path, null);
        Class<?> crop = loader.loadClass("eng.Crop");
        Class<?> probe = loader.loadClass("modA.Probe");
        Class<?> oldCrop = loader.loadClass("modB.OldCrop");
        Object vanilla = crop.getConstructor().newInstance();
        Object a = loader.loadClass("modA.MyCrop").getConstructor().newInstance();
        Object b = oldCrop.getConstructor().newInstance();
        Method p = probe.getMethod("probe", Object.class);
        Method engine = crop.getMethod("engineCalls", crop);
        Method ageOf = oldCrop.getMethod("ageOf", Object.class);

        expect("A sees a vanilla crop (instanceof, cast, array, dispatcher fallback, reflection)", p.invoke(null, vanilla), "crop age=1 max=7 grow=0 reflect=true");
        expect("A sees its own crop (overrides of a native, a new and a changed-signature method)", p.invoke(null, a), "crop age=2 max=3 grow=10 reflect=true");
        expect("A sees mod B's crop (older version)", p.invoke(null, b), "crop age=5 max=7 grow=0 reflect=true");
        expect("the engine calls A's crop with the old signatures", engine.invoke(null, a), "engine age=2 grow=10");
        expect("the engine calls B's crop", engine.invoke(null, b), "engine age=5 grow=0");
        expect("B (older) reads the age of A's crop", ageOf.invoke(null, a), 2);
        expect("B reads the age of a vanilla crop", ageOf.invoke(null, vanilla), 1);
        Object plain = probe.getMethod("plainCrop").invoke(null);
        expect("A's `new CropBlock()` makes a facade object", plain.getClass().getName(), "fac.Facade21");
        expect("...that behaves as vanilla", p.invoke(null, plain), "crop age=1 max=7 grow=0 reflect=true");
        Object array = probe.getMethod("arrayWith", Object.class).invoke(null, vanilla);
        expect("A's CropBlock[] is the native array type and holds a vanilla crop", array.getClass().getComponentType().getName(), "eng.Crop");
        expect("one class for all: A's, B's and vanilla crops are eng.Crop", crop.isInstance(a) && crop.isInstance(b) && crop.isInstance(vanilla), true);
        expect("KNOWN EDGE: MyCrop.class.getSuperclass() == CropBlock.class (true in the source game)",
                probe.getMethod("superclassIsCropBlock").invoke(null), false);
        System.out.println(failed == 0 ? "ALL AS EXPECTED" : failed + " UNEXPECTED");
        System.exit(failed == 0 ? 0 : 1);
    }
}
