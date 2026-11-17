/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

import org.objectweb.asm.*;

/**
 * Ladder stage 0: how much of the mods' use of Minecraft + NeoForge the rungs between neighbouring versions resolve by
 * themselves. Every reference of the corpus mods (classes they use, members they call or read, methods they override)
 * is walked down the versions; at each rung it is the same, renamed (Mojang names changed, found through Fabric's
 * intermediary, which is stable across versions), changed (same member, other descriptor: needs a bridge) or missing
 * (added in the upper version: needs a carried class or an adapter). Writes report.md and the generated rows per rung.
 *
 * usage: java Stage0 <out dir> <corpus dir> <version>=<joined jar>=<mappings dir> ... (top version first)
 */
public class Stage0 {
    // ---------------------------------------------------------------- platform model of one version

    static final class Cls {
        String superName;
        String[] interfaces = new String[0];
        final Set<String> methods = new HashSet<>();
        final Set<String> fields = new HashSet<>();
        final Set<String> staticFields = new HashSet<>();
        final Set<String> staticMethods = new HashSet<>();
        /** the class's own access flags (ACC_INTERFACE: calls on it are invokeinterface) */
        int classAccess;
        /** access flags: "M name desc" / "F name desc" */
        final Map<String, Integer> access = new HashMap<>();
    }

    static final class Version {
        final String id;
        final Map<String, Cls> classes = new HashMap<>();
        // named <-> intermediary (vanilla only)
        final Map<String, String> classToInter = new HashMap<>();
        final Map<String, String> interToClass = new HashMap<>();
        final Map<String, String> memberToInter = new HashMap<>(); // "cls.name desc" -> method_/field_
        final Map<String, List<String[]>> interToMembers = new HashMap<>(); // method_X -> {cls, name, desc}
        /** The class files of the platform: the code of a class or method that would be carried down. */
        final Map<String, byte[]> bytes = new HashMap<>();

        Version(String id) {
            this.id = id;
        }

        /** The class that declares the member, searched up the hierarchy; null if none in the platform. */
        String declaring(String cls, String name, String desc, boolean method) {
            Deque<String> todo = new ArrayDeque<>();
            Set<String> seen = new HashSet<>();
            todo.add(cls);
            while (!todo.isEmpty()) {
                String c = todo.poll();
                if (c == null || !seen.add(c)) {
                    continue;
                }
                Cls info = this.classes.get(c);
                if (info == null) {
                    continue;
                }
                if ((method ? info.methods : info.fields).contains(name + " " + desc)) {
                    return c;
                }
                if (method && name.startsWith("<")) {
                    // constructors and static initializers aren't inherited: only the class itself declares them
                    return null;
                }
                todo.add(info.superName);
                todo.addAll(Arrays.asList(info.interfaces));
            }
            return null;
        }

        /** Whether the hierarchy leaves the platform (a JDK or library supertype could declare the member). */
        boolean reachesOutside(String cls) {
            Deque<String> todo = new ArrayDeque<>(List.of(cls));
            Set<String> seen = new HashSet<>();
            while (!todo.isEmpty()) {
                String c = todo.poll();
                if (c == null || !seen.add(c)) {
                    continue;
                }
                Cls info = this.classes.get(c);
                if (info == null) {
                    return true;
                }
                todo.add(info.superName);
                todo.addAll(Arrays.asList(info.interfaces));
            }
            return false;
        }
    }

    /** Fabric's intermediary names (net/minecraft/class_1234) in multi-loader mods: not this platform's names. */
    static final java.util.regex.Pattern INTERMEDIARY = java.util.regex.Pattern.compile("net/minecraft/(class|method|field)_\\d+.*");

    static boolean platform(String internal) {
        if (INTERMEDIARY.matcher(internal).matches()) {
            return false;
        }
        return internal.startsWith("net/minecraft/") || internal.startsWith("net/neoforged/") || internal.startsWith("com/mojang/blaze3d/")
                || internal.startsWith("com/mojang/math/") || internal.startsWith("com/mojang/realmsclient/")
                // Mojang's libraries of the game: their API changes between the versions too (DFU 6 -> 8, Brigadier)
                || internal.startsWith("com/mojang/serialization/") || internal.startsWith("com/mojang/datafixers/")
                || internal.startsWith("com/mojang/brigadier/");
    }

    static boolean neoforge(String internal) {
        return internal.startsWith("net/neoforged/");
    }

    static Version load(String id, Path jar, Path mappings, List<Path> loaderJars) throws IOException {
        Version v = new Version(id);
        List<Path> jars = new ArrayList<>(List.of(jar));
        jars.addAll(loaderJars);
        for (Path one : jars) {
            readClasses(v, one);
        }
        readIntermediary(v, mappings.resolve("client.txt"), mappings.resolve("mappings.tiny"));
        return v;
    }

    /** The platform classes of one jar: the joined game jar, or one of the loader's (FML, event bus, SPI). */
    static void readClasses(Version v, Path jar) throws IOException {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (ZipEntry e : Collections.list(zip.entries())) {
                if (!e.getName().endsWith(".class") || !platform(e.getName())) {
                    continue;
                }
                try (InputStream in = zip.getInputStream(e)) {
                    byte[] data = in.readAllBytes();
                    v.bytes.put(e.getName().substring(0, e.getName().length() - 6), data);
                    new ClassReader(data).accept(new ClassVisitor(Opcodes.ASM9) {
                        Cls info;

                        @Override
                        public void visit(int ver, int access, String name, String sig, String sup, String[] itfs) {
                            this.info = new Cls();
                            this.info.classAccess = access;
                            this.info.superName = sup;
                            this.info.interfaces = itfs == null ? new String[0] : itfs;
                            v.classes.put(name, this.info);
                        }

                        @Override
                        public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
                            this.info.fields.add(name + " " + desc);
                            this.info.access.put("F " + name + " " + desc, access);
                            if ((access & Opcodes.ACC_STATIC) != 0) {
                                this.info.staticFields.add(name + " " + desc);
                            }
                            return null;
                        }

                        @Override
                        public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                            this.info.methods.add(name + " " + desc);
                            this.info.access.put("M " + name + " " + desc, access);
                            if ((access & Opcodes.ACC_STATIC) != 0) {
                                this.info.staticMethods.add(name + " " + desc);
                            }
                            return null;
                        }
                    }, ClassReader.SKIP_CODE);
                }
            }
        }
    }

    // ---------------------------------------------------------------- Mojang mappings + intermediary

    static String typeDesc(String t) {
        int dims = 0;
        while (t.endsWith("[]")) {
            dims++;
            t = t.substring(0, t.length() - 2);
        }
        String base = switch (t) {
            case "int" -> "I";
            case "boolean" -> "Z";
            case "byte" -> "B";
            case "char" -> "C";
            case "short" -> "S";
            case "long" -> "J";
            case "float" -> "F";
            case "double" -> "D";
            case "void" -> "V";
            default -> "L" + t.replace('.', '/') + ";";
        };
        return "[".repeat(dims) + base;
    }

    static String mapDesc(String desc, java.util.function.Function<String, String> classes) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < desc.length(); i++) {
            char c = desc.charAt(i);
            if (c == 'L') {
                int end = desc.indexOf(';', i);
                String mapped = classes.apply(desc.substring(i + 1, end));
                out.append('L').append(mapped).append(';');
                i = end;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    static void readIntermediary(Version v, Path client, Path tiny) throws IOException {
        // Mojang: named -> obfuscated, members with named descriptors
        Map<String, String> namedToObf = new HashMap<>();
        List<String[]> members = new ArrayList<>(); // {namedCls, name, namedDesc, obfName, method?}
        String current = null;
        for (String line : Files.readAllLines(client)) {
            if (line.startsWith("#") || line.isBlank()) {
                continue;
            }
            if (!line.startsWith(" ")) {
                String[] parts = line.substring(0, line.length() - 1).split(" -> ");
                current = parts[0].replace('.', '/');
                namedToObf.put(current, parts[1].replace('.', '/'));
                continue;
            }
            String body = line.trim();
            String obf = body.substring(body.lastIndexOf(" -> ") + 4);
            body = body.substring(0, body.lastIndexOf(" -> "));
            if (body.contains("(")) {
                // [a:b:]type name(args)[:c:d]
                body = body.replaceFirst("^\\d+:\\d+:", "");
                String type = body.substring(0, body.indexOf(' '));
                String name = body.substring(body.indexOf(' ') + 1, body.indexOf('('));
                String args = body.substring(body.indexOf('(') + 1, body.indexOf(')'));
                StringBuilder desc = new StringBuilder("(");
                if (!args.isEmpty()) {
                    for (String a : args.split(",")) {
                        desc.append(typeDesc(a));
                    }
                }
                desc.append(')').append(typeDesc(type));
                members.add(new String[] {current, name, desc.toString(), obf, "m"});
            } else {
                String type = body.substring(0, body.indexOf(' '));
                String name = body.substring(body.indexOf(' ') + 1);
                members.add(new String[] {current, name, typeDesc(type), obf, "f"});
            }
        }
        // intermediary: obfuscated -> intermediary
        Map<String, String> obfClassToInter = new HashMap<>();
        Map<String, String> obfMemberToInter = new HashMap<>(); // "obfCls.obfName obfDesc" (fields: no desc)
        String cls = null;
        for (String line : Files.readAllLines(tiny)) {
            String[] cols = line.split("\t", -1);
            if (line.startsWith("c\t")) {
                cls = cols[1];
                obfClassToInter.put(cols[1], cols[2]);
            } else if (line.startsWith("\tm\t")) {
                obfMemberToInter.put(cls + "." + cols[3] + " " + cols[2], cols[4]);
            } else if (line.startsWith("\tf\t")) {
                obfMemberToInter.put(cls + "." + cols[3], cols[4]);
            }
        }
        for (var e : namedToObf.entrySet()) {
            String inter = obfClassToInter.get(e.getValue());
            if (inter != null) {
                v.classToInter.put(e.getKey(), inter);
                v.interToClass.put(inter, e.getKey());
            }
        }
        for (String[] m : members) {
            String obfCls = namedToObf.get(m[0]);
            String key = m[4].equals("m")
                    ? obfCls + "." + m[3] + " " + mapDesc(m[2], c -> namedToObf.getOrDefault(c, c))
                    : obfCls + "." + m[3];
            String inter = obfMemberToInter.get(key);
            if (inter == null) {
                continue;
            }
            v.memberToInter.put(m[0] + "." + m[1] + " " + m[2], inter);
            v.interToMembers.computeIfAbsent(inter, k -> new ArrayList<>()).add(new String[] {m[0], m[1], m[2]});
        }
    }

    // ---------------------------------------------------------------- references of the mods

    /** A reference: kind C (class), M (method), F (field); for members the declaring platform class. */
    record Ref(char kind, String cls, String name, String desc) {
        String key() {
            return kind == 'C' ? "C " + cls : kind + " " + cls + "." + name + " " + desc;
        }
    }

    static final class ModClass {
        String superName;
        String[] interfaces;
        /** "name desc" of the class's own non-static methods (the converter's super calls) */
        Set<String> methods = new HashSet<>();
    }

    static Set<Ref> modRefs(Path jar, Version top, Map<String, Integer> libraryRefs) throws IOException {
        Map<String, byte[]> bytes = new HashMap<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            for (ZipEntry e : Collections.list(zip.entries())) {
                if (e.getName().endsWith(".class") && !e.getName().startsWith("META-INF/")) {
                    try (InputStream in = zip.getInputStream(e)) {
                        bytes.put(e.getName().substring(0, e.getName().length() - 6), in.readAllBytes());
                    }
                }
            }
        } catch (ZipException e) {
            return Set.of();
        }
        Map<String, ModClass> mod = new HashMap<>();
        for (var e : bytes.entrySet()) {
            try {
                ClassReader r = new ClassReader(e.getValue());
                ModClass mc = new ModClass();
                mc.superName = r.getSuperName();
                mc.interfaces = r.getInterfaces();
                mod.put(r.getClassName(), mc);
            } catch (RuntimeException ignored) {
                // not a class file ASM reads
            }
        }
        Set<Ref> refs = new HashSet<>();
        for (var e : bytes.entrySet()) {
            try {
                new ClassReader(e.getValue()).accept(new ClassVisitor(Opcodes.ASM9) {
                    String self;

                    void type(Type t) {
                        if (t.getSort() == Type.ARRAY) {
                            t = t.getElementType();
                        }
                        if (t.getSort() == Type.OBJECT && platform(t.getInternalName())) {
                            refs.add(new Ref('C', t.getInternalName(), null, null));
                        } else if (t.getSort() == Type.METHOD) {
                            for (Type a : t.getArgumentTypes()) {
                                type(a);
                            }
                            type(t.getReturnType());
                        }
                    }

                    void cls(String internal) {
                        if (internal != null) {
                            type(internal.startsWith("[") ? Type.getType(internal) : Type.getObjectType(internal));
                        }
                    }

                    void member(String owner, String name, String desc, boolean method) {
                        if (owner.startsWith("[")) {
                            return;
                        }
                        List<String> starts = new ArrayList<>();
                        platformStarts(owner, mod, starts, new HashSet<>());
                        for (String start : starts) {
                            String decl = top.declaring(start, name, desc, method);
                            if (decl != null) {
                                refs.add(new Ref(method ? 'M' : 'F', decl, name, desc));
                                return;
                            }
                        }
                        if (!starts.isEmpty() && starts.stream().allMatch(top::reachesOutside)) {
                            libraryRefs.merge("JDK/library member through a platform class", 1, Integer::sum);
                        } else if (!starts.isEmpty()) {
                            libraryRefs.merge("platform member not found in " + top.id + " (other mod, mixin)", 1, Integer::sum);
                        }
                    }

                    @Override
                    public void visit(int ver, int access, String name, String sig, String sup, String[] itfs) {
                        this.self = name;
                        cls(sup);
                        if (itfs != null) {
                            for (String i : itfs) {
                                cls(i);
                            }
                        }
                    }

                    @Override
                    public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
                        type(Type.getType(desc));
                        return null;
                    }

                    @Override
                    public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                        type(Type.getMethodType(desc));
                        if ((access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) == 0 && !name.startsWith("<")) {
                            // an override of a platform method
                            List<String> starts = new ArrayList<>();
                            ModClass mc = mod.get(this.self);
                            if (mc != null) {
                                platformStarts(mc.superName, mod, starts, new HashSet<>());
                                for (String i : mc.interfaces) {
                                    platformStarts(i, mod, starts, new HashSet<>());
                                }
                            }
                            for (String start : starts) {
                                String decl = top.declaring(start, name, desc, true);
                                if (decl != null) {
                                    refs.add(new Ref('M', decl, name, desc));
                                    break;
                                }
                            }
                        }
                        return new MethodVisitor(Opcodes.ASM9) {
                            @Override
                            public void visitTypeInsn(int op, String t) {
                                cls(t);
                            }

                            @Override
                            public void visitMultiANewArrayInsn(String d, int dims) {
                                type(Type.getType(d));
                            }

                            @Override
                            public void visitFieldInsn(int op, String owner, String name, String d) {
                                member(owner, name, d, false);
                                type(Type.getType(d));
                            }

                            @Override
                            public void visitMethodInsn(int op, String owner, String name, String d, boolean itf) {
                                member(owner, name, d, true);
                                type(Type.getMethodType(d));
                            }

                            @Override
                            public void visitInvokeDynamicInsn(String name, String d, Handle bsm, Object... args) {
                                for (Object a : args) {
                                    if (a instanceof Handle h) {
                                        member(h.getOwner(), h.getName(), h.getDesc(), h.getTag() >= Opcodes.H_INVOKEVIRTUAL);
                                    } else if (a instanceof Type t) {
                                        type(t);
                                    }
                                }
                            }

                            @Override
                            public void visitLdcInsn(Object value) {
                                if (value instanceof Type t) {
                                    type(t);
                                }
                            }
                        };
                    }
                }, ClassReader.SKIP_FRAMES);
            } catch (RuntimeException ignored) {
                // a class ASM can't read
            }
        }
        return refs;
    }

    /** The first platform classes above a class (itself if it is one); through the mod's own classes. */
    static void platformStarts(String cls, Map<String, ModClass> mod, List<String> out, Set<String> seen) {
        if (cls == null || !seen.add(cls)) {
            return;
        }
        if (platform(cls)) {
            out.add(cls);
            return;
        }
        ModClass mc = mod.get(cls);
        if (mc == null) {
            return;
        }
        platformStarts(mc.superName, mod, out, seen);
        for (String i : mc.interfaces) {
            platformStarts(i, mod, out, seen);
        }
    }

    // ---------------------------------------------------------------- one rung

    enum Outcome { SAME, RENAMED, CHANGED, MISSING_CLASS, MISSING_MEMBER }

    record Step(Outcome outcome, Ref to, String row) {
    }

    static String mapClass(Version from, Version to, String cls) {
        if (to.classes.containsKey(cls)) {
            return cls;
        }
        String inter = from.classToInter.get(cls);
        String other = inter == null ? null : to.interToClass.get(inter);
        return other != null && to.classes.containsKey(other) ? other : null;
    }

    static Step step(Version from, Version to, Ref ref) {
        if (ref.kind() == 'C') {
            String c = mapClass(from, to, ref.cls());
            if (c == null) {
                return new Step(Outcome.MISSING_CLASS, null, null);
            }
            Ref next = new Ref('C', c, null, null);
            return c.equals(ref.cls()) ? new Step(Outcome.SAME, next, null) : new Step(Outcome.RENAMED, next, "C\t" + ref.cls() + "\t" + c);
        }
        boolean method = ref.kind() == 'M';
        String owner = mapClass(from, to, ref.cls());
        if (owner == null) {
            return new Step(Outcome.MISSING_CLASS, null, null);
        }
        boolean[] lost = {false};
        String desc = mapDesc(ref.desc(), c -> {
            if (!platform(c)) {
                return c;
            }
            String m = mapClass(from, to, c);
            if (m == null) {
                lost[0] = true;
                return c;
            }
            return m;
        });
        if (!lost[0]) {
            String decl = to.declaring(owner, ref.name(), desc, method);
            if (decl != null) {
                Ref next = new Ref(ref.kind(), decl, ref.name(), desc);
                boolean same = owner.equals(ref.cls()) && desc.equals(ref.desc());
                return same ? new Step(Outcome.SAME, next, null)
                        : new Step(Outcome.RENAMED, next, ref.kind() + "\t" + ref.cls() + "\t" + ref.name() + ref.desc() + "\t" + ref.name() + desc);
            }
        }
        String inter = from.memberToInter.get(ref.cls() + "." + ref.name() + " " + ref.desc());
        if (inter != null) {
            for (String[] m : to.interToMembers.getOrDefault(inter, List.of())) {
                if (!m[0].equals(owner) && to.declaring(owner, m[1], m[2], method) == null) {
                    continue;
                }
                Ref next = new Ref(ref.kind(), m[0], m[1], m[2]);
                String row = ref.kind() + "\t" + ref.cls() + "\t" + ref.name() + ref.desc() + "\t" + m[1] + m[2];
                return !lost[0] && m[2].equals(desc) ? new Step(Outcome.RENAMED, next, row) : new Step(Outcome.CHANGED, next, row);
            }
        }
        if (lost[0]) {
            return new Step(Outcome.MISSING_CLASS, null, null);
        }
        // NeoForge (no intermediary) or a vanilla member whose descriptor changed: one member of that name below
        if (method) {
            Cls info = to.classes.get(owner);
            if (info != null) {
                String found = null;
                int count = 0;
                for (String m : info.methods) {
                    if (m.startsWith(ref.name() + " ")) {
                        found = m;
                        count++;
                    }
                }
                if (count == 1) {
                    String d = found.substring(found.indexOf(' ') + 1);
                    return new Step(Outcome.CHANGED, new Ref('M', owner, ref.name(), d),
                            "M\t" + ref.cls() + "\t" + ref.name() + ref.desc() + "\t" + ref.name() + d);
                }
            }
        }
        return new Step(Outcome.MISSING_MEMBER, null, null);
    }

    // ---------------------------------------------------------------- carrying the upper version's own code down

    /** The references of a platform class's code: per method ("name desc"), and "" for the class itself. */
    static Map<String, Set<Ref>> codeRefs(Version v, String cls) {
        Map<String, Set<Ref>> out = new HashMap<>();
        byte[] data = v.bytes.get(cls);
        if (data == null) {
            return out;
        }
        Set<Ref> classLevel = new HashSet<>();
        out.put("", classLevel);
        new ClassReader(data).accept(new ClassVisitor(Opcodes.ASM9) {
            void type(Set<Ref> into, Type t) {
                if (t.getSort() == Type.ARRAY) {
                    t = t.getElementType();
                }
                if (t.getSort() == Type.OBJECT && platform(t.getInternalName())) {
                    into.add(new Ref('C', t.getInternalName(), null, null));
                } else if (t.getSort() == Type.METHOD) {
                    for (Type a : t.getArgumentTypes()) {
                        type(into, a);
                    }
                    type(into, t.getReturnType());
                }
            }

            void cls(Set<Ref> into, String internal) {
                if (internal != null) {
                    type(into, internal.startsWith("[") ? Type.getType(internal) : Type.getObjectType(internal));
                }
            }

            void member(Set<Ref> into, String owner, String name, String desc, boolean method) {
                if (owner.startsWith("[") || !platform(owner)) {
                    return;
                }
                String decl = v.declaring(owner, name, desc, method);
                if (decl != null) {
                    into.add(new Ref(method ? 'M' : 'F', decl, name, desc));
                }
            }

            @Override
            public void visit(int ver, int access, String name, String sig, String sup, String[] itfs) {
                cls(classLevel, sup);
                if (itfs != null) {
                    for (String i : itfs) {
                        cls(classLevel, i);
                    }
                }
            }

            @Override
            public FieldVisitor visitField(int access, String name, String desc, String sig, Object value) {
                type(classLevel, Type.getType(desc));
                return null;
            }

            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String sig, String[] ex) {
                Set<Ref> refs = new HashSet<>();
                if ((access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0) {
                    // no body: its types count for the class, but it has no code to carry ("no code" for a new member)
                    type(classLevel, Type.getMethodType(desc));
                    return null;
                }
                out.put(name + " " + desc, refs);
                type(refs, Type.getMethodType(desc));
                boolean clinit = name.equals("<clinit>");
                // the static initializer in slices: the code before each static field's store makes that field's value
                Set<Ref>[] slice = new Set[] {new HashSet<Ref>()};
                int[] previous = {-1};
                return new MethodVisitor(Opcodes.ASM9) {
                    void add(Ref r) {
                        refs.add(r);
                        if (clinit) {
                            slice[0].add(r);
                        }
                    }

                    @Override
                    public void visitInsn(int op) {
                        previous[0] = op;
                    }

                    @Override
                    public void visitTypeInsn(int op, String t) {
                        previous[0] = op;
                        Set<Ref> into = new HashSet<>();
                        cls(into, t);
                        into.forEach(this::add);
                    }

                    @Override
                    public void visitMultiANewArrayInsn(String d, int dims) {
                        previous[0] = Opcodes.MULTIANEWARRAY;
                        Set<Ref> into = new HashSet<>();
                        type(into, Type.getType(d));
                        into.forEach(this::add);
                    }

                    @Override
                    public void visitFieldInsn(int op, String owner, String name, String d) {
                        Set<Ref> into = new HashSet<>();
                        member(into, owner, name, d, false);
                        type(into, Type.getType(d));
                        // A = B = expr: expr; DUP; PUTSTATIC B; PUTSTATIC A — the store after a DUP leaves the value for the next one
                        boolean chained = previous[0] == Opcodes.DUP || previous[0] == Opcodes.DUP_X1 || previous[0] == Opcodes.DUP_X2
                                || previous[0] == Opcodes.DUP2;
                        previous[0] = op;
                        if (clinit && op == Opcodes.PUTSTATIC && owner.equals(cls)) {
                            slice[0].addAll(into);
                            out.put("F " + name + " " + d, slice[0]);
                            // a duplicated value (A = B = ...) goes on to the next store: that field gets the same code
                            slice[0] = chained ? new HashSet<>(slice[0]) : new HashSet<>();
                            refs.addAll(into);
                            return;
                        }
                        into.forEach(this::add);
                    }

                    @Override
                    public void visitVarInsn(int op, int var) {
                        previous[0] = op;
                    }

                    @Override
                    public void visitIntInsn(int op, int operand) {
                        previous[0] = op;
                    }

                    @Override
                    public void visitJumpInsn(int op, Label label) {
                        previous[0] = op;
                    }

                    @Override
                    public void visitMethodInsn(int op, String owner, String name, String d, boolean itf) {
                        Set<Ref> into = new HashSet<>();
                        member(into, owner, name, d, true);
                        type(into, Type.getMethodType(d));
                        into.forEach(this::add);
                    }

                    @Override
                    public void visitInvokeDynamicInsn(String name, String d, Handle bsm, Object... args) {
                        Set<Ref> into = new HashSet<>();
                        for (Object a : args) {
                            if (a instanceof Handle h) {
                                member(into, h.getOwner(), h.getName(), h.getDesc(), h.getTag() >= Opcodes.H_INVOKEVIRTUAL);
                                // a lambda's body is a synthetic method of this class: its code counts for the slice
                                if (h.getOwner().equals(cls) && h.getName().startsWith("lambda$")) {
                                    into.add(new Ref('L', cls, h.getName(), h.getDesc()));
                                }
                            } else if (a instanceof Type t) {
                                type(into, t);
                            }
                        }
                        into.forEach(this::add);
                    }

                    @Override
                    public void visitLdcInsn(Object value) {
                        if (value instanceof Type t) {
                            Set<Ref> into = new HashSet<>();
                            type(into, t);
                            into.forEach(this::add);
                        }
                    }
                };
            }
        }, ClassReader.SKIP_FRAMES);
        // a slice that calls a lambda of the class: the lambda's own references too
        for (var e : out.entrySet()) {
            Set<Ref> extra = new HashSet<>();
            Deque<Ref> todo = new ArrayDeque<>();
            e.getValue().stream().filter(r -> r.kind() == 'L').forEach(todo::add);
            Set<String> seen = new HashSet<>();
            while (!todo.isEmpty()) {
                Ref l = todo.poll();
                if (!seen.add(l.name() + l.desc())) {
                    continue;
                }
                Set<Ref> body = out.get(l.name() + " " + l.desc());
                if (body != null) {
                    for (Ref r : body) {
                        if (r.kind() == 'L') {
                            todo.add(r);
                        } else {
                            extra.add(r);
                        }
                    }
                }
            }
            e.getValue().removeIf(r -> r.kind() == 'L');
            e.getValue().addAll(extra);
        }
        return out;
    }

    /** What a piece of upper-version code needs below: other new things to carry with it, or what can't be carried. */
    static final class Needs {
        final Set<String> deps = new HashSet<>();
        final Set<String> hard = new HashSet<>();
    }

    static boolean registration(Ref r) {
        return r.kind() == 'M' && (r.cls().equals("net/minecraft/core/Registry") && r.name().startsWith("register")
                || r.cls().equals("net/minecraft/server/Bootstrap"));
    }

    /**
     * The new things of the upper version (no counterpart below): classes, and methods and fields of classes that exist
     * below. Instance fields are state the lower class hasn't: they can't be carried.
     */
    static Map<String, Ref> newThings(Version from, Version to, Set<String> absent) {
        Map<String, Ref> things = new HashMap<>();
        for (String c : absent) {
            things.put("C " + c, new Ref('C', c, null, null));
        }
        for (var e : from.classes.entrySet()) {
            String c = e.getKey();
            if (absent.contains(c)) {
                continue;
            }
            for (String m : e.getValue().methods) {
                Ref r = new Ref('M', c, m.substring(0, m.indexOf(' ')), m.substring(m.indexOf(' ') + 1));
                if (r.name().equals("<clinit>")) {
                    continue;
                }
                Outcome o = step(from, to, r).outcome();
                if (o == Outcome.MISSING_MEMBER || o == Outcome.MISSING_CLASS) {
                    things.put(r.key(), r);
                }
            }
            for (String f : e.getValue().fields) {
                Ref r = new Ref('F', c, f.substring(0, f.indexOf(' ')), f.substring(f.indexOf(' ') + 1));
                Outcome o = step(from, to, r).outcome();
                if (o == Outcome.MISSING_MEMBER || o == Outcome.MISSING_CLASS) {
                    things.put(r.key(), r);
                }
            }
        }
        return things;
    }

    static Needs needs(Version from, Version to, Set<String> absent, Map<String, Ref> things, Collection<Ref> refs, String self) {
        Needs n = new Needs();
        for (Ref r : refs) {
            if (registration(r)) {
                n.hard.add("registers: " + r.key());
            }
            if (absent.contains(r.cls())) {
                if (!r.cls().equals(self)) {
                    n.deps.add("C " + r.cls());
                }
                continue;
            }
            if (r.kind() == 'C') {
                continue;
            }
            if (things.containsKey(r.key())) {
                n.deps.add(r.key());
                continue;
            }
            Outcome o = step(from, to, r).outcome();
            if (o == Outcome.MISSING_MEMBER || o == Outcome.MISSING_CLASS) {
                n.hard.add("missing below: " + r.key());
            } else if (o == Outcome.CHANGED) {
                // same member, another descriptor below: carried code calling it wouldn't link without a bridge
                n.hard.add("changed below: " + r.key());
            }
        }
        return n;
    }

    /** What a rung can carry down by itself: the classes absent below, the new things, what each needs, the carried ones. */
    static final class Plan {
        Set<String> absent;
        Map<String, Ref> things;
        Map<String, Needs> needs;
        Set<String> carried;
    }

    static Plan plan(Version from, Version to) {
        Set<String> absent = new HashSet<>();
        for (String c : from.classes.keySet()) {
            if (mapClass(from, to, c) == null) {
                absent.add(c);
            }
        }
        Map<String, Ref> things = newThings(from, to, absent);
        Map<String, Map<String, Set<Ref>>> code = new HashMap<>();
        Map<String, Needs> needs = new HashMap<>();
        for (var e : things.entrySet()) {
            Ref r = e.getValue();
            var classCode = code.computeIfAbsent(r.cls(), c -> codeRefs(from, c));
            Needs n;
            if (r.kind() == 'C') {
                Set<Ref> all = new HashSet<>();
                classCode.values().forEach(all::addAll);
                n = needs(from, to, absent, things, all, r.cls());
            } else if (r.kind() == 'M') {
                Set<Ref> body = classCode.get(r.name() + " " + r.desc());
                n = needs(from, to, absent, things, body == null ? Set.of() : body, r.cls());
                if (body == null) {
                    n.hard.add("no code: " + r.key());
                }
            } else {
                boolean isStatic = from.classes.get(r.cls()).staticFields.contains(r.name() + " " + r.desc());
                Set<Ref> init = classCode.get("F " + r.name() + " " + r.desc());
                n = needs(from, to, absent, things, init == null ? Set.of() : init, r.cls());
                if (!isStatic) {
                    // state the lower class hasn't: an adapter keeps it (an attached map, a capability, a mixin field)
                    n.hard.add("new state: " + r.key());
                }
            }
            n.deps.remove(e.getKey());
            needs.put(e.getKey(), n);
        }
        // carried: nothing hard in it or in what it needs (a fixed point)
        Set<String> carried = new HashSet<>();
        needs.forEach((k, n) -> {
            if (n.hard.isEmpty()) {
                carried.add(k);
            }
        });
        boolean changed = true;
        while (changed) {
            changed = false;
            for (var it = carried.iterator(); it.hasNext();) {
                Needs n = needs.get(it.next());
                for (String d : n.deps) {
                    if (needs.containsKey(d) && !carried.contains(d)) {
                        it.remove();
                        changed = true;
                        break;
                    }
                }
            }
        }
        Plan plan = new Plan();
        plan.absent = absent;
        plan.things = things;
        plan.needs = needs;
        plan.carried = carried;
        return plan;
    }

    static void carry(Version from, Version to, Map<Ref, Integer> stopped, StringBuilder details, StringBuilder table, Path out)
            throws IOException {
        Plan plan = plan(from, to);
        Set<String> absent = plan.absent;
        Map<String, Ref> things = plan.things;
        Map<String, Needs> needs = plan.needs;
        Set<String> carried = plan.carried;
        // the roots: the hard things the held-back ones come down to, propagated to a fixed point (dependency cycles)
        Map<String, Set<String>> rootSets = new HashMap<>();
        needs.forEach((k, n) -> {
            if (!carried.contains(k)) {
                rootSets.put(k, new HashSet<>(n.hard));
            }
        });
        boolean grew = true;
        while (grew) {
            grew = false;
            for (var e : rootSets.entrySet()) {
                for (String d : needs.get(e.getKey()).deps) {
                    Set<String> other = rootSets.get(d);
                    if (other != null && e.getValue().addAll(other)) {
                        grew = true;
                    }
                }
            }
        }
        java.util.function.Function<String, Set<String>> roots = k -> rootSets.getOrDefault(k, Set.of());
        long total = 0, wCarried = 0, wBlocked = 0;
        Map<String, Long> rootWeight = new HashMap<>();
        List<String> lines = new ArrayList<>();
        for (var e : stopped.entrySet()) {
            Ref r = e.getKey();
            long w = e.getValue();
            total += w;
            String k = absent.contains(r.cls()) ? "C " + r.cls() : r.key();
            boolean ok = carried.contains(k);
            if (ok) {
                wCarried += w;
            } else {
                wBlocked += w;
                for (String root : roots.apply(k)) {
                    rootWeight.merge(root, w, Long::sum);
                }
            }
            lines.add(w + "\t" + (ok ? "carried" : "adapter") + "\t" + r.key());
        }
        lines.sort(Comparator.comparingInt((String s) -> Integer.parseInt(s.substring(0, s.indexOf('\t')))).reversed());
        Files.write(out.resolve("rungs").resolve(from.id + "-" + to.id + "-carry.tsv"), lines);
        List<String> rootLines = new ArrayList<>();
        rootWeight.entrySet().stream().sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(b -> rootLines.add(b.getValue() + "\t" + b.getKey()));
        Files.write(out.resolve("rungs").resolve(from.id + "-" + to.id + "-roots.tsv"), rootLines);
        long carriedThings = things.keySet().stream().filter(carried::contains).count();
        table.append(String.format("| %s → %s | %d | %d (%.0f%%) | %d (%.0f%%) | %d | %d of %d |%n", from.id, to.id, total,
                wCarried, 100.0 * wCarried / Math.max(1, total), wBlocked, 100.0 * wBlocked / Math.max(1, total), rootWeight.size(),
                carriedThings, things.size()));
        details.append("\nNew things of ").append(from.id).append(": ").append(things.size()).append(", carried as they are: ")
                .append(carriedThings).append(". The roots the rest comes down to, by the weight they hold back (first 40 of ")
                .append(rootWeight.size()).append("; all in rungs/").append(from.id).append('-').append(to.id).append("-roots.tsv):\n\n");
        rootLines.stream().limit(40).forEach(l -> details.append("- ").append(l.replace("\t", ": `")).append("`\n"));
    }

    // ---------------------------------------------------------------- main

    public static void main(String[] args) throws Exception {
        Path out = Paths.get(args[0]);
        Path corpus = Paths.get(args[1]);
        List<Version> versions = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            String[] p = args[i].split("=");
            long t = System.nanoTime();
            List<Path> loader = new ArrayList<>();
            if (p.length > 3) {
                for (String line : Files.readAllLines(Paths.get(p[3]))) {
                    if (line.endsWith(".jar")) {
                        loader.add(Paths.get(line.trim()));
                    }
                }
            }
            versions.add(load(p[0], Paths.get(p[1]), Paths.get(p[2]), loader));
            Version v = versions.get(versions.size() - 1);
            System.out.printf("%s: %d classes, %d intermediary classes, %d intermediary members (%d ms)%n", v.id, v.classes.size(),
                    v.classToInter.size(), v.memberToInter.size(), (System.nanoTime() - t) / 1_000_000);
        }
        Version top = versions.get(0);

        // the references of every mod
        Map<Ref, Set<String>> users = new HashMap<>();
        Map<String, Set<Ref>> byMod = new TreeMap<>();
        Map<String, Integer> library = new TreeMap<>();
        List<Path> jars;
        try (var s = Files.list(corpus)) {
            jars = s.filter(p -> p.toString().endsWith(".jar")).sorted().toList();
        }
        for (Path jar : jars) {
            Set<Ref> refs = modRefs(jar, top, library);
            String mod = jar.getFileName().toString();
            byMod.put(mod, refs);
            for (Ref r : refs) {
                users.computeIfAbsent(r, k -> new HashSet<>()).add(mod);
            }
        }
        System.out.printf("%d mods, %d distinct references%n", byMod.size(), users.size());

        // walk every reference down the rungs
        Files.createDirectories(out.resolve("rungs"));
        StringBuilder report = new StringBuilder("# Ladder stage 0: references of " + byMod.size() + " mods of " + top.id
                + " walked down the rungs\n\nDistinct references (and mod-weighted: one per mod that uses it). `same` and `renamed`"
                + " are resolved by the generated tables; `changed` needs a bridge (same member, another descriptor); `missing`"
                + " needs a carried class or an adapter. Mixin targets and references to other mods are not counted.\n\n");
        report.append("| Rung | In | Same | Renamed | Changed | Missing class (vanilla / NeoForge) | Missing member (vanilla / NeoForge) | Auto % (weighted) |\n");
        report.append("|---|---|---|---|---|---|---|---|\n");
        Map<Ref, Ref> current = new HashMap<>();
        for (Ref r : users.keySet()) {
            current.put(r, r); // original -> its form at the current version
        }
        Map<Ref, String> stoppedAt = new HashMap<>();
        StringBuilder details = new StringBuilder();
        StringBuilder carryTable = new StringBuilder();
        for (int i = 0; i + 1 < versions.size(); i++) {
            Version from = versions.get(i);
            Version to = versions.get(i + 1);
            EnumMap<Outcome, int[]> count = new EnumMap<>(Outcome.class);
            for (Outcome o : Outcome.values()) {
                count.put(o, new int[4]); // distinct vanilla, distinct neoforge, weighted vanilla, weighted neoforge
            }
            Set<String> rows = new TreeSet<>();
            Map<String, Integer> missing = new HashMap<>();
            Map<String, Integer> changed = new HashMap<>();
            Map<Ref, Integer> stoppedRefs = new HashMap<>();
            int in = 0;
            for (var e : current.entrySet()) {
                if (stoppedAt.containsKey(e.getKey())) {
                    continue;
                }
                in++;
                Ref ref = e.getValue();
                Step s = step(from, to, ref);
                int weight = users.get(e.getKey()).size();
                int nf = neoforge(ref.cls()) ? 1 : 0;
                count.get(s.outcome())[nf]++;
                count.get(s.outcome())[2 + nf] += weight;
                if (s.row() != null) {
                    rows.add(s.row());
                }
                switch (s.outcome()) {
                    case SAME, RENAMED -> e.setValue(s.to());
                    case CHANGED -> {
                        e.setValue(s.to());
                        changed.merge(ref.key() + "  ->  " + s.to().name() + s.to().desc(), weight, Integer::sum);
                    }
                    default -> {
                        stoppedAt.put(e.getKey(), from.id + " -> " + to.id);
                        missing.merge(s.outcome() + " " + ref.key(), weight, Integer::sum);
                        stoppedRefs.merge(ref, weight, Integer::sum);
                    }
                }
            }
            Files.write(out.resolve("rungs").resolve(from.id + "-" + to.id + ".tsv"), rows);
            // everything the rung doesn't resolve by itself: outcome, reference, mods using it
            List<String> residual = new ArrayList<>();
            missing.forEach((k, w) -> residual.add(w + "\t" + k));
            changed.forEach((k, w) -> residual.add(w + "\tCHANGED " + k));
            residual.sort(Comparator.comparingInt((String s) -> Integer.parseInt(s.substring(0, s.indexOf('\t')))).reversed());
            Files.write(out.resolve("rungs").resolve(from.id + "-" + to.id + "-residual.tsv"), residual);
            int[] same = count.get(Outcome.SAME), ren = count.get(Outcome.RENAMED), ch = count.get(Outcome.CHANGED);
            int[] mc = count.get(Outcome.MISSING_CLASS), mm = count.get(Outcome.MISSING_MEMBER);
            long weighted = 0;
            for (int[] c : count.values()) {
                weighted += c[2] + c[3];
            }
            double auto = 100.0 * (same[2] + same[3] + ren[2] + ren[3]) / Math.max(1, weighted);
            report.append(String.format("| %s → %s | %d | %d | %d | %d | %d / %d | %d / %d | %.2f%% |%n", from.id, to.id, in,
                    same[0] + same[1], ren[0] + ren[1], ch[0] + ch[1], mc[0], mc[1], mm[0], mm[1], auto));
            details.append("\n## ").append(from.id).append(" → ").append(to.id).append("\n\n");
            details.append("Generated rows: ").append(rows.size()).append(" (rungs/").append(from.id).append('-').append(to.id).append(".tsv)\n\n");
            details.append("Missing, by mods using it (first 60 of ").append(missing.size()).append("):\n\n");
            missing.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(60)
                    .forEach(m -> details.append("- ").append(m.getValue()).append(" mods: `").append(m.getKey()).append("`\n"));
            details.append("\nChanged descriptors, by mods using it (first 30 of ").append(changed.size()).append("):\n\n");
            changed.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).limit(30)
                    .forEach(m -> details.append("- ").append(m.getValue()).append(" mods: `").append(m.getKey()).append("`\n"));
            carry(from, to, stoppedRefs, details, carryTable, out);
        }
        report.append("\n### What the missing part is\n\nThe references that stop at a rung, weighted by the mods using them: "
                + "**carried** = the upper version's own code of that class, method or static constant links below once the other "
                + "carried code is there: copied down automatically; **adapter** = it comes down to a root that can't be "
                + "carried: new state of an existing class (an instance field), a registration, a member missing below with "
                + "no code. Roots = how many distinct such roots (the adapter work list).\n\n| Rung | Stopped refs (weighted) | "
                + "Carried | Adapter | Roots | New things carried |\n|---|---|---|---|---|---|\n")
                .append(carryTable);

        // per mod: does everything it uses reach the bottom by generated tables only?
        Version bottom = versions.get(versions.size() - 1);
        int clean = 0;
        List<String> worst = new ArrayList<>();
        for (var e : byMod.entrySet()) {
            long stopped = e.getValue().stream().filter(stoppedAt::containsKey).count();
            if (stopped == 0 && !e.getValue().isEmpty()) {
                clean++;
            }
            worst.add(String.format("%05d %s (%d of %d references stop)", stopped, e.getKey(), stopped, e.getValue().size()));
        }
        Collections.sort(worst, Collections.reverseOrder());
        report.append("\n").append(clean).append(" of ").append(byMod.size()).append(" mods reach ").append(bottom.id)
                .append(" with no missing reference (changed descriptors still need bridges).\n\nOutside the walk: ");
        library.forEach((k, n) -> report.append(k).append(" ").append(n).append("; "));
        report.append("\n\nMods with the most references that stop:\n\n");
        worst.stream().limit(15).forEach(w -> report.append("- ").append(w.substring(6)).append('\n'));
        report.append(details);
        Files.writeString(out.resolve("report.md"), report);
        System.out.println(report.substring(0, report.indexOf("\nOutside")));
    }
}
