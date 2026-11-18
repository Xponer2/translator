/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

/**
 * Checks converted jars the way the lower JVM will link and verify them, without starting the game: every method call,
 * field access, type and method reference must resolve on the class path (the bottom game, its loader, the adapters,
 * the carried library, the JDK, the jars checked), with the right kind (an interface call on an interface, a static
 * call on a static method: the IncompatibleClassChangeError family), and every method's bytecode must pass ASM's
 * verifier with the class path's hierarchy. Mixin classes are left out (Mixin rewrites them into their targets; the
 * converter's mixin check covers them).
 *
 * usage: VerifyJar <class path file: one jar per line> <jar to check>...   (exit 1 when anything fails)
 */
public class VerifyJar {
    static final java.util.regex.Pattern GAME = java.util.regex.Pattern.compile(
            "(^|[ ;L(])(net/minecraft/|com/mojang/|net/neoforged/|ladder/|java/|javax/|jdk/|sun/|it/unimi/|com/google/|org/joml/|io/netty/|org/slf4j/|org/apache/)");
    static final Map<String, byte[]> bytes = new HashMap<>();
    static final Map<String, ClassNode> nodes = new HashMap<>();
    static FileSystem jrt;

    public static void main(String[] args) throws Exception {
        jrt = FileSystems.getFileSystem(URI.create("jrt:/"));
        // the class path file: jars one per line or ';'-separated (a JVM -p line), Windows or Git Bash paths
        Set<Path> jars = new LinkedHashSet<>();
        for (String line : Files.readAllLines(Paths.get(args[0]))) {
            for (String part : line.replace("\\\\", "\\").split(";")) {
                String p = part.trim().replaceAll("^\"|\"$", "");
                if (p.matches("^/[a-zA-Z]/.*")) {
                    p = p.charAt(1) + ":" + p.substring(2);
                }
                if (!p.isEmpty() && p.endsWith(".jar") && Files.isRegularFile(Paths.get(p))) {
                    jars.add(Paths.get(p).toAbsolutePath().normalize());
                }
            }
        }
        for (Path jar : jars) {
            read(jar, false);
        }
        List<String> checked = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            checked.addAll(read(Paths.get(args[i]), true));
        }
        Map<String, Integer> kinds = new TreeMap<>();
        List<String> issues = new ArrayList<>();
        int classes = 0, methods = 0;
        for (String name : checked) {
            ClassNode c = node(name);
            if (c == null || isMixin(c)) {
                continue;
            }
            classes++;
            for (MethodNode m : c.methods) {
                methods++;
                for (String issue : link(c, m)) {
                    kinds.merge(issue.substring(0, issue.indexOf(':')), 1, Integer::sum);
                    issues.add(c.name + "." + m.name + m.desc + ": " + issue);
                }
                if ((m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) == 0) {
                    try {
                        new Analyzer<>(new LadderVerifier(c)).analyze(c.name, m);
                    } catch (AnalyzerException | RuntimeException e) {
                        if (Boolean.getBoolean("verify.trace") && kinds.getOrDefault("verify", 0) == 0) {
                            e.printStackTrace();
                        }
                        kinds.merge("verify", 1, Integer::sum);
                        issues.add(c.name + "." + m.name + m.desc + ": verify: " + e.getMessage());
                    }
                }
            }
        }
        // what names another mod's classes (an optional integration whose mod isn't installed) is listed apart: the game's
        // and the ladder's own references are the ones that must link
        List<String> game = new ArrayList<>(), other = new ArrayList<>();
        for (String issue : issues) {
            String what = issue.substring(issue.indexOf(": ", issue.indexOf(")") + 1) + 2);
            // the owner the issue names: the first class name after the issue's kind words
            String ref = what.replaceFirst("^[a-z ]+: ", "").replaceFirst("^[a-z ]*?(?=[a-z]+/)", "").split("[ (]")[0];
            (what.startsWith("verify") || GAME.matcher(ref).find() ? game : other).add(issue);
        }
        game.stream().limit(200).forEach(System.out::println);
        if (game.size() > 200) {
            System.out.println("... " + (game.size() - 200) + " more");
        }
        System.out.println(classes + " classes, " + methods + " methods checked; " + game.size() + " issues in the game's and the ladder's "
                + "references " + kinds + "; " + other.size() + " references to other mods' classes (optional integrations, not checked)");
        System.exit(game.isEmpty() ? 0 : 1);
    }

    /** The jar's classes (and its embedded jars'); the names of the ones to check when {@code mine}. */
    static List<String> read(Path jar, boolean mine) throws IOException {
        List<String> names = new ArrayList<>();
        try (ZipInputStream in = new ZipInputStream(new BufferedInputStream(Files.newInputStream(jar)))) {
            for (ZipEntry e; (e = in.getNextEntry()) != null;) {
                String n = e.getName();
                if (n.endsWith(".class") && !n.startsWith("META-INF/versions/") && !n.endsWith("module-info.class")) {
                    String cls = n.substring(0, n.length() - 6);
                    if (mine || !bytes.containsKey(cls)) {
                        bytes.put(cls, in.readAllBytes());
                    }
                    if (mine) {
                        names.add(cls);
                    }
                } else if (n.endsWith(".jar")) {
                    Path tmp = Files.createTempFile("verify", ".jar");
                    Files.write(tmp, in.readAllBytes());
                    names.addAll(read(tmp, mine));
                    Files.delete(tmp);
                }
            }
        }
        return names;
    }

    static ClassNode node(String name) {
        if (nodes.containsKey(name)) {
            return nodes.get(name);
        }
        byte[] b = bytes.get(name);
        if (b == null && jrt != null) {
            // the JDK's own classes, from the running JDK's modules
            try (var mods = Files.list(jrt.getPath("/modules"))) {
                for (Path mod : (Iterable<Path>) mods::iterator) {
                    Path p = mod.resolve(name + ".class");
                    if (Files.exists(p)) {
                        b = Files.readAllBytes(p);
                        break;
                    }
                }
            } catch (IOException ignored) {
                // not found
            }
        }
        ClassNode c = null;
        if (b != null) {
            c = new ClassNode();
            new ClassReader(b).accept(c, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            if (bytes.containsKey(name)) {
                // the classes checked need their code
                c = new ClassNode();
                new ClassReader(b).accept(c, ClassReader.EXPAND_FRAMES);
            }
        }
        nodes.put(name, c);
        return c;
    }

    static boolean isMixin(ClassNode c) {
        for (List<AnnotationNode> l : Arrays.asList(c.visibleAnnotations, c.invisibleAnnotations)) {
            if (l != null && l.stream().anyMatch(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;"))) {
                return true;
            }
        }
        return false;
    }

    /** The method's references that won't link below, each "kind: what". */
    static List<String> link(ClassNode c, MethodNode m) {
        List<String> out = new ArrayList<>();
        for (AbstractInsnNode insn : m.instructions) {
            if (insn instanceof MethodInsnNode call) {
                String issue = checkMethod(call.owner, call.name, call.desc, call.getOpcode(), call.itf);
                if (issue != null) {
                    out.add(issue);
                }
            } else if (insn instanceof FieldInsnNode f) {
                String issue = checkField(f.owner, f.name, f.desc, f.getOpcode() == Opcodes.GETSTATIC || f.getOpcode() == Opcodes.PUTSTATIC);
                if (issue != null) {
                    out.add(issue);
                }
            } else if (insn instanceof TypeInsnNode t) {
                String issue = checkType(t.desc);
                if (issue != null) {
                    out.add(issue);
                }
            } else if (insn instanceof InvokeDynamicInsnNode indy) {
                for (Object arg : indy.bsmArgs) {
                    if (arg instanceof Handle h && h.getTag() >= Opcodes.H_INVOKEVIRTUAL) {
                        int op = switch (h.getTag()) {
                            case Opcodes.H_INVOKESTATIC -> Opcodes.INVOKESTATIC;
                            case Opcodes.H_INVOKEINTERFACE -> Opcodes.INVOKEINTERFACE;
                            case Opcodes.H_INVOKESPECIAL, Opcodes.H_NEWINVOKESPECIAL -> Opcodes.INVOKESPECIAL;
                            default -> Opcodes.INVOKEVIRTUAL;
                        };
                        String issue = checkMethod(h.getOwner(), h.getName(), h.getDesc(), op, h.isInterface());
                        if (issue != null) {
                            out.add(issue);
                        }
                    }
                }
            }
        }
        return out;
    }

    static String checkType(String desc) {
        String name = desc.startsWith("[") ? Type.getType(desc).getElementType().getSort() == Type.OBJECT
                ? Type.getType(desc).getElementType().getInternalName() : null : desc;
        if (name == null || node(name) != null) {
            return null;
        }
        return "missing class: " + name;
    }

    static String checkMethod(String owner, String name, String desc, int opcode, boolean itf) {
        if (owner.startsWith("[")) {
            return null; // array clone() and Object's methods
        }
        ClassNode o = node(owner);
        if (o == null) {
            return "missing class: " + owner + " (for " + name + desc + ")";
        }
        boolean isItf = (o.access & Opcodes.ACC_INTERFACE) != 0;
        if (opcode == Opcodes.INVOKEINTERFACE && !isItf) {
            return "kind: invokeinterface on the class " + owner + "." + name + desc;
        }
        if (opcode == Opcodes.INVOKEVIRTUAL && isItf) {
            return "kind: invokevirtual on the interface " + owner + "." + name + desc;
        }
        if (itf != isItf && opcode != Opcodes.INVOKEVIRTUAL && opcode != Opcodes.INVOKEINTERFACE) {
            return "kind: " + (itf ? "an interface" : "a class") + " reference to " + owner + "." + name + desc;
        }
        MethodNode found = findMethod(owner, name, desc, new HashSet<>());
        if (found == null) {
            if (isPolymorphic(owner, name)) {
                return null;
            }
            return "missing method: " + owner + "." + name + desc;
        }
        boolean isStatic = (found.access & Opcodes.ACC_STATIC) != 0;
        if (opcode == Opcodes.INVOKESTATIC && !isStatic) {
            return "kind: invokestatic of the instance method " + owner + "." + name + desc;
        }
        if (opcode != Opcodes.INVOKESTATIC && isStatic) {
            return "kind: instance call of the static method " + owner + "." + name + desc;
        }
        return null;
    }

    static boolean isPolymorphic(String owner, String name) {
        return (owner.equals("java/lang/invoke/MethodHandle") || owner.equals("java/lang/invoke/VarHandle"));
    }

    static MethodNode findMethod(String owner, String name, String desc, Set<String> seen) {
        ClassNode c = owner == null || !seen.add(owner) ? null : node(owner);
        if (c == null) {
            return null;
        }
        for (MethodNode m : c.methods) {
            if (m.name.equals(name) && m.desc.equals(desc)) {
                return m;
            }
        }
        MethodNode up = findMethod(c.superName, name, desc, seen);
        if (up != null) {
            return up;
        }
        for (String i : c.interfaces) {
            MethodNode m = findMethod(i, name, desc, seen);
            if (m != null) {
                return m;
            }
        }
        return null;
    }

    static String checkField(String owner, String name, String desc, boolean isStatic) {
        ClassNode o = node(owner);
        if (o == null) {
            return "missing class: " + owner + " (for field " + name + ")";
        }
        FieldNode f = findField(owner, name, desc, new HashSet<>());
        if (f == null) {
            return "missing field: " + owner + "." + name + " " + desc;
        }
        if (((f.access & Opcodes.ACC_STATIC) != 0) != isStatic) {
            return "kind: " + (isStatic ? "static" : "instance") + " access to the field " + owner + "." + name;
        }
        return null;
    }

    static FieldNode findField(String owner, String name, String desc, Set<String> seen) {
        ClassNode c = owner == null || !seen.add(owner) ? null : node(owner);
        if (c == null) {
            return null;
        }
        for (FieldNode f : c.fields) {
            if (f.name.equals(name) && f.desc.equals(desc)) {
                return f;
            }
        }
        for (String i : c.interfaces) {
            FieldNode f = findField(i, name, desc, seen);
            if (f != null) {
                return f;
            }
        }
        return findField(c.superName, name, desc, seen);
    }

    /** ASM's verifier with the class path's hierarchy instead of loaded classes. */
    static final class LadderVerifier extends SimpleVerifier {
        LadderVerifier(ClassNode c) {
            super(Opcodes.ASM9, Type.getObjectType(c.name), c.superName == null ? null : Type.getObjectType(c.superName),
                    c.interfaces.stream().map(Type::getObjectType).toList(), (c.access & Opcodes.ACC_INTERFACE) != 0);
        }

        @Override
        protected boolean isInterface(Type type) {
            ClassNode c = node(type.getInternalName());
            return c != null && (c.access & Opcodes.ACC_INTERFACE) != 0;
        }

        @Override
        protected Type getSuperClass(Type type) {
            ClassNode c = node(type.getInternalName());
            return c == null || c.superName == null ? null : Type.getObjectType(c.superName);
        }

        @Override
        protected boolean isAssignableFrom(Type type, Type other) {
            if (type.equals(other)) {
                return true;
            }
            if (type.getSort() == Type.ARRAY) {
                if (other.getSort() != Type.ARRAY) {
                    return false;
                }
                Type a = Type.getType(type.getDescriptor().substring(1)), b = Type.getType(other.getDescriptor().substring(1));
                return a.getSort() >= Type.ARRAY && b.getSort() >= Type.ARRAY ? isAssignableFrom(a, b) : a.equals(b);
            }
            if (other.getSort() == Type.ARRAY) {
                String n = type.getInternalName();
                return n.equals("java/lang/Object") || n.equals("java/lang/Cloneable") || n.equals("java/io/Serializable");
            }
            if (type.getInternalName().equals("java/lang/Object") || isInterface(type)) {
                // interfaces are checked at the call (the JVM's verifier treats them as Object too)
                return true;
            }
            for (String c = other.getInternalName(); c != null;) {
                if (c.equals(type.getInternalName())) {
                    return true;
                }
                ClassNode n = node(c);
                c = n == null ? null : n.superName;
            }
            return false;
        }

        @Override
        protected boolean isSubTypeOf(BasicValue value, BasicValue expected) {
            Type e = expected.getType(), t = value.getType();
            if (e != null && (e.getSort() == Type.OBJECT || e.getSort() == Type.ARRAY)) {
                if (t == null || t.getSort() != Type.OBJECT && t.getSort() != Type.ARRAY) {
                    return false;
                }
                // null fits any reference
                return t.getSort() == Type.OBJECT && t.getInternalName().equals("null") || isAssignableFrom(e, t);
            }
            return super.isSubTypeOf(value, expected);
        }

        @Override
        protected Class<?> getClass(Type type) {
            throw new UnsupportedOperationException("no class loading: " + type);
        }
    }
}
