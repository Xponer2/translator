/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.shape;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Shape patches of 1.21: edits of NeoForge 21.0's classes that give a 1.21.1 mod's mixin the point it aims at. Declared
 * once (shape-patches.tsv); applied by the converter to the bottom classes it checks mixins against, and at run time
 * before any mixin applies (ShapePlugin). Kinds:
 * <ul>
 * <li>{@code supports-method owner - - -}: ItemStack gets 21.1's {@code supportsEnchantment(Holder)} (a mod item's
 * override, else the enchantment's own check: ladder.rung121.EnchantSupport.supportsHere);</li>
 * <li>{@code supports-call owner name desc -}: the method's {@code enchantment.canEnchant(stack)} become
 * {@code stack.supportsEnchantment(holder)}, as 21.1's calls are (fabric's item API wraps them).</li>
 * </ul>
 * Uses ASM alone (the converter loads it from the adapters' jar).
 */
public final class ShapePatches {
    public static final String RESOURCE = "/shape-patches.tsv";
    static final String STACK = "net/minecraft/world/item/ItemStack";
    static final String HOLDER = "Lnet/minecraft/core/Holder;";
    static final String CAN_ENCHANT_OWNER = "net/minecraft/world/item/enchantment/Enchantment";
    static final String CAN_ENCHANT_DESC = "(Lnet/minecraft/world/item/ItemStack;)Z";
    static final String SUPPORT = "ladder/rung121/EnchantSupport";

    public record Patch(String kind, String owner, String name, String desc, String superOwner) {
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
        for (String line : text.split("\\r?\\n")) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] c = line.split("\t");
            if (c.length != 5 || !List.of("supports-method", "supports-call").contains(c[0])) {
                throw new IllegalArgumentException("bad shape patch: " + line);
            }
            patches.add(new Patch(c[0], c[1], c[2], c[3], c[4]));
        }
        return List.copyOf(patches);
    }

    /** Applies the patches for the class (internal name); returns what was done (null), or a reason one couldn't be. */
    public static List<String> apply(ClassNode node, List<Patch> patches) {
        List<String> report = new ArrayList<>();
        for (Patch p : patches) {
            if (!p.owner.equals(node.name)) {
                continue;
            }
            report.add(p.kind.equals("supports-method") ? supportsMethod(node) : supportsCall(node, p));
        }
        return report;
    }

    private static String supportsMethod(ClassNode node) {
        String desc = "(" + HOLDER + ")Z";
        if (node.methods.stream().anyMatch(m -> m.name.equals("supportsEnchantment") && m.desc.equals(desc))) {
            return null;
        }
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "supportsEnchantment", desc, null, null);
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SUPPORT, "supportsHere", "(L" + STACK + ";" + HOLDER + ")Z", false));
        m.instructions.add(new InsnNode(Opcodes.IRETURN));
        m.maxLocals = 2;
        m.maxStack = 2;
        node.methods.add(m);
        return null;
    }

    private static String supportsCall(ClassNode node, Patch p) {
        MethodNode m = node.methods.stream().filter(x -> x.name.equals(p.name) && x.desc.equals(p.desc)).findFirst().orElse(null);
        if (m == null && p.name.startsWith("lambda$")) {
            // a lambda is numbered by its compiler: the one of the same outer method with the call (the adapters,
            // converted down a rung, meet that rung's own numbering)
            String outer = p.name.substring(0, p.name.lastIndexOf('$') + 1);
            m = node.methods.stream().filter(x -> x.name.startsWith(outer) && hasCanEnchant(x)).findFirst().orElse(null);
        }
        if (m == null) {
            return "no " + p.name + p.desc + " in " + p.owner;
        }
        return supportsCallIn(m, p);
    }

    private static boolean hasCanEnchant(MethodNode m) {
        for (AbstractInsnNode insn : m.instructions) {
            if (insn instanceof MethodInsnNode call && isCanEnchant(call)) {
                return true;
            }
        }
        return false;
    }

    private static String supportsCallIn(MethodNode m, Patch p) {
        int done = 0;
        for (AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn instanceof MethodInsnNode call && isCanEnchant(call)) {
                InsnList with = new InsnList();
                with.add(new InsnNode(Opcodes.SWAP));
                with.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SUPPORT, "holderOf", "(L" + CAN_ENCHANT_OWNER + ";)" + HOLDER, false));
                with.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, STACK, "supportsEnchantment", "(" + HOLDER + ")Z", false));
                m.instructions.insert(call, with);
                m.instructions.remove(call);
                done++;
            }
        }
        return done == 0 && !alreadyDone(m) ? "no Enchantment.canEnchant call in " + p.owner + "." + p.name : null;
    }

    private static boolean isCanEnchant(MethodInsnNode call) {
        return call.getOpcode() == Opcodes.INVOKEVIRTUAL && call.owner.equals(CAN_ENCHANT_OWNER) && call.name.equals("canEnchant")
                && call.desc.equals(CAN_ENCHANT_DESC);
    }

    private static boolean alreadyDone(MethodNode m) {
        for (AbstractInsnNode insn : m.instructions) {
            if (insn instanceof MethodInsnNode call && call.owner.equals(STACK) && call.name.equals("supportsEnchantment")) {
                return true;
            }
        }
        return false;
    }

    /**
     * The converter's check on the bottom classes: a supports-call's method has the canEnchant calls it edits; a
     * supports-method's class hasn't the method yet. Null when it holds, else why not.
     */
    public static String verify(Patch p, Function<String, ClassNode> bottom) {
        ClassNode n = bottom.apply(p.owner);
        if (n == null) {
            return "no " + p.owner + " in the bottom game";
        }
        if (p.kind.equals("supports-method")) {
            return n.methods.stream().anyMatch(m -> m.name.equals("supportsEnchantment") && m.desc.equals("(" + HOLDER + ")Z"))
                    ? p.owner + " has supportsEnchantment already" : null;
        }
        MethodNode m = n.methods.stream().filter(x -> x.name.equals(p.name) && x.desc.equals(p.desc)).findFirst().orElse(null);
        if (m == null) {
            return "no " + p.name + p.desc + " in " + p.owner;
        }
        for (AbstractInsnNode insn : m.instructions) {
            if (insn instanceof MethodInsnNode call && isCanEnchant(call)) {
                return null;
            }
        }
        return "no Enchantment.canEnchant call in " + p.owner + "." + p.name;
    }
}
