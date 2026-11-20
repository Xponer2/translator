/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.shape;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Shape patches: small edits of 1.20.4's own classes that give a 1.21 mod's mixin the point it aims at, without changing
 * what 1.20.4 does. Declared once (shape-patches.tsv); applied by the converter to the bottom classes it checks mixins
 * against, and at run time before any mixin applies (ShapePlugin). Kinds:
 * <ul>
 * <li>{@code super-return owner name desc super}: the method's last {@code return false} becomes
 * {@code return super.name(args)}, the super method's own answer being false;</li>
 * <li>{@code super-override owner name desc super}: the class gets {@code name(args) { return super.name(args); }} for a
 * method it only inherits;</li>
 * <li>{@code layout-hook owner name ()V super}: 1.21's screen lays itself out in its own {@code name()} (repositionElements),
 * called at the end of init and on a resize; 1.20.4's lays out in init, run again on a resize (the inherited name()
 * rebuilds the widgets). The class gets name() that rebuilds as before, except when init's end calls it: then it does
 * nothing more, so a mixin at its TAIL runs once after every layout, as in 1.21.</li>
 * </ul>
 * Uses ASM alone (the converter loads it from the adapters' jar).
 */
public final class ShapePatches {
    public static final String RESOURCE = "/shape-patches.tsv";

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
        for (String line : text.split("\r?\n")) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] c = line.split("\t");
            if (c.length != 5 || !List.of("super-return", "super-override", "layout-hook").contains(c[0])) {
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
            if (!p.owner.equals(node.name)) {
                continue;
            }
            MethodNode m = node.methods.stream().filter(x -> x.name.equals(p.name) && x.desc.equals(p.desc)).findFirst().orElse(null);
            if (p.kind.equals("super-return")) {
                report.add(superReturn(node, m, p));
            } else if (p.kind.equals("super-override")) {
                report.add(superOverride(node, m, p));
            } else {
                report.add(layoutHook(node, m, p));
            }
        }
        return report;
    }

    private static String superReturn(ClassNode node, MethodNode m, Patch p) {
        if (m == null || Type.getReturnType(p.desc) != Type.BOOLEAN_TYPE) {
            return "no boolean method " + p.name + p.desc + " in " + p.owner;
        }
        AbstractInsnNode ret = null;
        for (AbstractInsnNode insn = m.instructions.getLast(); insn != null; insn = insn.getPrevious()) {
            if (insn.getOpcode() == Opcodes.IRETURN) {
                ret = insn;
                break;
            }
        }
        AbstractInsnNode value = ret == null ? null : ret.getPrevious();
        while (value != null && value.getOpcode() < 0) {
            value = value.getPrevious();
        }
        if (value == null || value.getOpcode() != Opcodes.ICONST_0) {
            return "the last return of " + p.owner + "." + p.name + " isn't return false";
        }
        InsnList call = new InsnList();
        call.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int slot = 1;
        for (Type t : Type.getArgumentTypes(p.desc)) {
            call.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), slot));
            slot += t.getSize();
        }
        call.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, p.superOwner, p.name, p.desc, false));
        m.instructions.insertBefore(value, call);
        m.instructions.remove(value);
        m.maxStack = Math.max(m.maxStack, slot + 1);
        return null;
    }

    private static String superOverride(ClassNode node, MethodNode existing, Patch p) {
        if (existing != null) {
            return p.owner + " declares " + p.name + p.desc + " already";
        }
        MethodNode m = new MethodNode(Opcodes.ACC_PROTECTED, p.name, p.desc, null, null);
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int slot = 1;
        for (Type t : Type.getArgumentTypes(p.desc)) {
            m.instructions.add(new VarInsnNode(t.getOpcode(Opcodes.ILOAD), slot));
            slot += t.getSize();
        }
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, p.superOwner, p.name, p.desc, false));
        m.instructions.add(new InsnNode(Type.getReturnType(p.desc).getOpcode(Opcodes.IRETURN)));
        m.maxLocals = slot;
        m.maxStack = slot + 1;
        node.methods.add(m);
        return null;
    }

    private static final String LAYING_OUT = "ladder$layingOut";

    private static String layoutHook(ClassNode node, MethodNode existing, Patch p) {
        if (existing != null || !p.desc.equals("()V")) {
            return p.owner + " declares " + p.name + p.desc + " already (or it isn't ()V)";
        }
        MethodNode init = node.methods.stream().filter(x -> x.name.equals("init") && x.desc.equals("()V")).findFirst().orElse(null);
        if (init == null) {
            return p.owner + " has no init()V";
        }
        node.fields.add(new org.objectweb.asm.tree.FieldNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, LAYING_OUT, "Z", null, null));
        // name(): if (!layingOut) { super.name(); return; } return;   (TAIL is the last return: init's call)
        MethodNode m = new MethodNode(Opcodes.ACC_PROTECTED, p.name, p.desc, null, null);
        org.objectweb.asm.tree.LabelNode last = new org.objectweb.asm.tree.LabelNode();
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETFIELD, node.name, LAYING_OUT, "Z"));
        m.instructions.add(new org.objectweb.asm.tree.JumpInsnNode(Opcodes.IFNE, last));
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, p.superOwner, p.name, p.desc, false));
        m.instructions.add(new InsnNode(Opcodes.RETURN));
        m.instructions.add(last);
        m.instructions.add(new org.objectweb.asm.tree.FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        m.instructions.add(new InsnNode(Opcodes.RETURN));
        m.maxLocals = 1;
        m.maxStack = 2;
        node.methods.add(m);
        // init's end: layingOut = true; name(); layingOut = false;
        for (AbstractInsnNode insn : init.instructions.toArray()) {
            if (insn.getOpcode() == Opcodes.RETURN) {
                InsnList call = new InsnList();
                call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                call.add(new InsnNode(Opcodes.ICONST_1));
                call.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTFIELD, node.name, LAYING_OUT, "Z"));
                call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                call.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, node.name, p.name, p.desc, false));
                call.add(new VarInsnNode(Opcodes.ALOAD, 0));
                call.add(new InsnNode(Opcodes.ICONST_0));
                call.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.PUTFIELD, node.name, LAYING_OUT, "Z"));
                init.instructions.insertBefore(insn, call);
            }
        }
        init.maxStack = Math.max(init.maxStack, 2);
        return null;
    }

    /**
     * The converter's check that a patch keeps 1.20.4's behaviour, on the bottom classes: the super method a
     * super-return calls answers false (its body is {@code return false}); a super-override's super method exists and
     * keeps its access (protected or public). Null when it holds, else why not.
     */
    public static String verify(Patch p, Function<String, ClassNode> bottom) {
        MethodNode target = null;
        ClassNode owner = null;
        for (String c = p.superOwner; c != null && target == null; ) {
            ClassNode n = bottom.apply(c);
            if (n == null) {
                break;
            }
            target = n.methods.stream().filter(x -> x.name.equals(p.name) && x.desc.equals(p.desc) && (x.access & Opcodes.ACC_ABSTRACT) == 0)
                    .findFirst().orElse(null);
            owner = n;
            if (target == null) {
                // an interface's default (GuiEventListener.mouseClicked) or the superclass's
                for (String i : n.interfaces) {
                    target = defaultIn(i, p, bottom);
                    if (target != null) {
                        break;
                    }
                }
                c = n.superName;
            }
        }
        if (target == null) {
            return "no " + p.name + p.desc + " above " + p.owner + " in the bottom game";
        }
        if (p.kind.equals("super-return")) {
            AbstractInsnNode first = target.instructions.getFirst();
            while (first != null && first.getOpcode() < 0) {
                first = first.getNext();
            }
            AbstractInsnNode next = first == null ? null : first.getNext();
            while (next != null && next.getOpcode() < 0) {
                next = next.getNext();
            }
            if (first == null || first.getOpcode() != Opcodes.ICONST_0 || next == null || next.getOpcode() != Opcodes.IRETURN) {
                return "the super method of " + p.owner + "." + p.name + " (in " + (owner == null ? "?" : owner.name) + ") doesn't return false";
            }
        } else if ((target.access & Opcodes.ACC_PROTECTED) == 0) {
            // (the override is protected: a public super method would need a public one)
            return "the super method of " + p.owner + "." + p.name + " isn't protected";
        }
        return null;
    }

    private static MethodNode defaultIn(String itf, Patch p, Function<String, ClassNode> bottom) {
        ClassNode n = bottom.apply(itf);
        if (n == null) {
            return null;
        }
        MethodNode m = n.methods.stream().filter(x -> x.name.equals(p.name) && x.desc.equals(p.desc) && (x.access & Opcodes.ACC_ABSTRACT) == 0)
                .findFirst().orElse(null);
        if (m != null) {
            return m;
        }
        for (String i : n.interfaces) {
            m = defaultIn(i, p, bottom);
            if (m != null) {
                return m;
            }
        }
        return null;
    }
}
