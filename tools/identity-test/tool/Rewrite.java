/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.objectweb.asm.*;

/**
 * The identity rules as a converter would apply them, from three rule tables (the Translator's C, S and X rows):
 * USE      - every use of a source type (descriptors, instanceof, checkcast, arrays, class literals) -> the native type;
 * SUPER    - extends, super calls and `new` of a source type -> its facade;
 * DISPATCH - a member the native type hasn't, called through the source type -> a static dispatcher.
 * usage: java Rewrite <in classes dir> <out classes dir>
 */
public class Rewrite {
    static final Map<String, String> USE = Map.of("src/CropBlock", "eng/Crop", "srcb/CropBlock", "eng/Crop");
    static final Map<String, String> SUPER = Map.of("src/CropBlock", "fac/Facade21", "srcb/CropBlock", "fac/Facade0");
    static final Map<String, String[]> DISPATCH = Map.of(
            "src/CropBlock.maxAge()I", new String[] {"fac/Bridge", "maxAge", "(Leng/Crop;)I"},
            "src/CropBlock.grow(Ljava/lang/CharSequence;)I", new String[] {"fac/Bridge", "grow", "(Leng/Crop;Ljava/lang/CharSequence;)I"});

    static String use(String internal) {
        return internal == null ? null : USE.getOrDefault(internal, internal);
    }

    static String desc(String d) {
        if (d == null) {
            return null;
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < d.length(); i++) {
            char c = d.charAt(i);
            if (c == 'L') {
                int end = d.indexOf(';', i);
                out.append('L').append(use(d.substring(i + 1, end))).append(';');
                i = end;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    static String typeOperand(String t) {
        return t.startsWith("[") ? desc(t) : use(t);
    }

    public static void main(String[] args) throws IOException {
        Path in = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        List<Path> classes;
        try (var files = Files.walk(in)) {
            classes = files.filter(f -> f.toString().endsWith(".class")).toList();
        }
        for (Path p : classes) {
            ClassReader reader = new ClassReader(Files.readAllBytes(p));
            ClassWriter writer = new ClassWriter(0);
            reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
                @Override
                public void visit(int v, int a, String name, String sig, String sup, String[] itfs) {
                    super.visit(v, a, name, null, SUPER.containsKey(sup) ? SUPER.get(sup) : use(sup), itfs);
                }

                @Override
                public FieldVisitor visitField(int a, String n, String d, String sig, Object v) {
                    return super.visitField(a, n, desc(d), null, v);
                }

                @Override
                public MethodVisitor visitMethod(int a, String n, String d, String sig, String[] ex) {
                    return new MethodVisitor(Opcodes.ASM9, super.visitMethod(a, n, desc(d), null, ex)) {
                        @Override
                        public void visitTypeInsn(int op, String t) {
                            super.visitTypeInsn(op, op == Opcodes.NEW && SUPER.containsKey(t) ? SUPER.get(t) : typeOperand(t));
                        }

                        @Override
                        public void visitMethodInsn(int op, String owner, String name, String d, boolean itf) {
                            if (op == Opcodes.INVOKESPECIAL && SUPER.containsKey(owner)) {
                                // super(...), super.m() and the constructor after `new`: the facade's
                                super.visitMethodInsn(op, SUPER.get(owner), name, desc(d), itf);
                                return;
                            }
                            // -Dnodispatch: only the four rules (to show that the fifth, the dispatcher, is needed)
                            String[] to = Boolean.getBoolean("nodispatch") ? null : DISPATCH.get(owner + "." + name + d);
                            if (to != null) {
                                super.visitMethodInsn(Opcodes.INVOKESTATIC, to[0], to[1], to[2], false);
                                return;
                            }
                            super.visitMethodInsn(op, use(owner), name, desc(d), itf);
                        }

                        @Override
                        public void visitFieldInsn(int op, String owner, String name, String d) {
                            super.visitFieldInsn(op, use(owner), name, desc(d));
                        }

                        @Override
                        public void visitLdcInsn(Object value) {
                            super.visitLdcInsn(value instanceof Type t && t.getSort() == Type.OBJECT ? Type.getObjectType(use(t.getInternalName())) : value);
                        }

                        @Override
                        public void visitFrame(int type, int nl, Object[] locals, int ns, Object[] stack) {
                            super.visitFrame(type, nl, frame(locals), ns, frame(stack));
                        }

                        @Override
                        public void visitLocalVariable(String n, String d, String sig, Label s, Label e, int i) {
                            super.visitLocalVariable(n, desc(d), null, s, e, i);
                        }
                    };
                }
            }, 0);
            Path target = out.resolve(in.relativize(p));
            Files.createDirectories(target.getParent());
            Files.write(target, writer.toByteArray());
        }
    }

    static Object[] frame(Object[] entries) {
        if (entries == null) {
            return null;
        }
        Object[] copy = entries.clone();
        for (int i = 0; i < copy.length; i++) {
            if (copy[i] instanceof String s) {
                copy[i] = typeOperand(s);
            }
        }
        return copy;
    }
}
