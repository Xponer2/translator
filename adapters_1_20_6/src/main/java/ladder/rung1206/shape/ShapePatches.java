/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.shape;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Shape patches of 1.20.6's own classes: what a 1.21 mod's mixin aims at that a mixin can't add (Mixin refuses a mixin's
 * non-private static method). Declared once (shape-patches.tsv); applied by the converter to the bottom classes it
 * checks mixins against, and at run time before any mixin applies (ShapePlugin). Kinds:
 * <ul>
 * <li>{@code add-static owner name desc template}: the class gets the template's static method (1.21's public static
 * EnchantmentHelper.getRandomItemWith of a DataComponentType), its references to the template made the class's own;
 * the template is an adapters class marked {@link AddedTo}.</li>
 * </ul>
 * Uses ASM alone (the converter loads it from the adapters' jar).
 */
public final class ShapePatches {
    public static final String RESOURCE = "/shape-patches.tsv";

    public record Patch(String kind, String owner, String name, String desc, String source) {
    }

    private static List<Patch> loaded;

    private ShapePatches() {
    }

    public static synchronized List<Patch> load() {
        if (loaded == null) {
            try (InputStream in = ShapePatches.class.getResourceAsStream(RESOURCE)) {
                loaded = in == null ? List.of() : parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
        return loaded;
    }

    public static List<Patch> parse(String text) {
        List<Patch> patches = new ArrayList<>();
        for (String line : text.split("\r?\n")) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] c = line.split("\t");
            if (c.length != 5 || !c[0].equals("add-static")) {
                throw new IllegalArgumentException("bad shape patch: " + line);
            }
            patches.add(new Patch(c[0], c[1], c[2], c[3], c[4]));
        }
        return List.copyOf(patches);
    }

    /** Applies the patches for the class (internal name); returns what was done, or a reason one couldn't be. */
    public static List<String> apply(ClassNode node, List<Patch> patches) {
        List<String> report = new ArrayList<>();
        for (Patch p : patches) {
            if (p.owner.equals(node.name)) {
                report.add(addStatic(node, p));
            }
        }
        return report;
    }

    private static MethodNode template(Patch p) {
        try (InputStream in = ShapePatches.class.getResourceAsStream("/" + p.source + ".class")) {
            if (in == null) {
                return null;
            }
            ClassNode t = new ClassNode();
            new ClassReader(in.readAllBytes()).accept(t, 0);
            return t.methods.stream().filter(m -> m.name.equals(p.name) && m.desc.equals(p.desc) && (m.access & Opcodes.ACC_STATIC) != 0)
                    .findFirst().orElse(null);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String addStatic(ClassNode node, Patch p) {
        if (node.methods.stream().anyMatch(m -> m.name.equals(p.name) && m.desc.equals(p.desc))) {
            return p.owner + " declares " + p.name + p.desc + " already";
        }
        MethodNode m = template(p);
        if (m == null) {
            return "no static " + p.name + p.desc + " in " + p.source;
        }
        // the template's own references (its other static helpers, none expected) are the class's
        for (AbstractInsnNode insn : m.instructions) {
            if (insn instanceof MethodInsnNode call && call.owner.equals(p.source)) {
                call.owner = p.owner;
            } else if (insn instanceof FieldInsnNode field && field.owner.equals(p.source)) {
                field.owner = p.owner;
            } else if (insn instanceof InvokeDynamicInsnNode indy) {
                for (Object a : indy.bsmArgs) {
                    if (a instanceof Handle h && h.getOwner().equals(p.source)) {
                        return "the template " + p.source + "." + p.name + " has a lambda of its own (not copied)";
                    }
                }
            }
        }
        m.invisibleAnnotations = null;
        m.visibleAnnotations = null;
        node.methods.add(m);
        return null;
    }

    /**
     * The converter's check, on the bottom classes: the class exists and hasn't the method yet; the template has it,
     * static. Null when it holds, else why not.
     */
    public static String verify(Patch p, Function<String, ClassNode> bottom) {
        ClassNode owner = bottom.apply(p.owner);
        if (owner == null) {
            return "no " + p.owner + " in the bottom game";
        }
        if (owner.methods.stream().anyMatch(m -> m.name.equals(p.name) && m.desc.equals(p.desc))) {
            return p.owner + " has " + p.name + p.desc + " already";
        }
        return template(p) == null ? "no static " + p.name + p.desc + " in the template " + p.source : null;
    }
}
