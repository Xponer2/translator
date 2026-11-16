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
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.*;
import org.objectweb.asm.tree.*;

import com.google.gson.*;

/**
 * Converts a mod jar of the top version down the ladder to the bottom version in one pass: every reference to Minecraft
 * or NeoForge is walked down the rungs (Stage0.step: same / renamed / changed / missing) and the class files are
 * rewritten with the final names; mixin selectors (strings) the same way; embedded jars recursively; the metadata, data
 * folders and vanilla recipe results take the lower version's forms. Strict: whatever doesn't come down (a missing class
 * or member, a changed descriptor or constructor with no bridge yet, a mixin selector that matches nothing, a data
 * component in data) is a problem, and the mod isn't written. Formats the converter can't check (a mod's own recipe
 * types, advancements) are listed as unverified.
 *
 * usage: java Convert <in.jar | folder> <out.jar | folder> <report> <version>=<joined jar>=<mappings dir>=<loader jars list> ...
 */
public class Convert {
    static List<Stage0.Version> versions = new ArrayList<>();
    static final Set<String> problems = new TreeSet<>();
    static final Set<String> unverified = new TreeSet<>();
    static Map<String, Stage0.ModClass> mod = new HashMap<>();
    /** mixin class -> its target classes (platform internal names) */
    static Map<String, List<String>> mixinTargets = new HashMap<>();
    static final Map<String, String> classCache = new HashMap<>();
    static final Map<String, Stage0.Ref> memberCache = new HashMap<>();

    /** Where carried code goes: its own package tree (a class can't join a package of the game's module). */
    static final String CARRIED = "ladder/carried/";
    /** The version (index) the code being converted is written against: 0 for the mods, a rung's for carried code. */
    static int level = 0;
    /** Per rung: the new things the converter can carry down there (classes; static methods of existing classes). */
    static final Map<Integer, Set<String>> carriable = new HashMap<>();
    /** The carried classes of the target, shared by every mod: relocated name -> class. */
    static final Map<String, ClassNode> carriedOut = new TreeMap<>();
    static final Deque<Object[]> carryQueue = new ArrayDeque<>();
    static final Set<String> carryScheduled = new HashSet<>();
    /** walk key -> the companion class a carried static member moved to (its call sites move there too) */
    static final Map<String, String> staticRedirect = new HashMap<>();
    /** converting carried code: its calls must reach public members below */
    static boolean inCarried;
    static Remapper remapper;

    /** NeoForge classes that moved between 21.x and 20.4 (no intermediary for NeoForge): their own rows. */
    static final Map<String, String> NEOFORGE_MOVES = new HashMap<>(Map.of(
            "net/neoforged/fml/common/EventBusSubscriber", "net/neoforged/fml/common/Mod$EventBusSubscriber",
            "net/neoforged/fml/common/EventBusSubscriber$Bus", "net/neoforged/fml/common/Mod$EventBusSubscriber$Bus"));

    /** The problems a cached class or member walk found: added again for every mod that meets it. */
    static final Map<String, List<String>> cachedProblems = new HashMap<>();
    static List<String> collecting;

    static void problem(String text) {
        problems.add(text);
        if (collecting != null) {
            collecting.add(text);
        }
    }

    public static void main(String[] args) throws Exception {
        Path in = Paths.get(args[0]), out = Paths.get(args[1]), report = Paths.get(args[2]);
        for (int i = 3; i < args.length; i++) {
            String[] p = args[i].split("=");
            List<Path> loader = new ArrayList<>();
            for (String line : Files.readAllLines(Paths.get(p[3]))) {
                if (line.endsWith(".jar")) {
                    loader.add(Paths.get(line.trim()));
                }
            }
            versions.add(Stage0.load(p[0], Paths.get(p[1]), Paths.get(p[2]), loader));
        }
        loadRules();
        if (Files.isDirectory(in)) {
            // batch: every jar of the folder, one report line each; the clean ones first
            Files.createDirectories(out);
            List<String> lines = new ArrayList<>();
            int clean = 0;
            List<Path> jars;
            try (var s = Files.list(in)) {
                jars = s.filter(p -> p.toString().endsWith(".jar")).sorted().toList();
            }
            for (Path jar : jars) {
                problems.clear();
                unverified.clear();
                Path converted = out.resolve(jar.getFileName());
                try {
                    convert(jar, converted);
                } catch (Exception | LinkageError e) {
                    problems.add("conversion failed: " + e);
                }
                if (problems.isEmpty()) {
                    clean++;
                } else {
                    Files.deleteIfExists(converted);
                }
                long classes = 0;
                try (ZipFile z = new ZipFile(jar.toFile())) {
                    classes = Collections.list(z.entries()).stream().filter(e -> e.getName().endsWith(".class")).count();
                } catch (IOException ignored) {
                    // unreadable
                }
                lines.add(String.format("%05d\t%s\t%d classes\t%d unverified\t%s", problems.size(), jar.getFileName(), classes,
                        unverified.size(), problems.isEmpty() ? "" : String.join(" | ", problems)));
            }
            Collections.sort(lines);
            Files.write(report, lines);
            Path carried = out.resolve("ladder-carried-" + versions.get(versions.size() - 1).id + ".jar");
            problems.clear();
            writeCarried(carried);
            System.out.println(clean + " of " + jars.size() + " mods convert with no problem; " + carriedOut.size() + " carried classes in "
                    + carried + (problems.isEmpty() ? "" : " (carried code problems: " + problems + ")") + "; " + report);
            return;
        }
        convert(in, out);
        writeCarried(out.resolveSibling("ladder-carried-" + versions.get(versions.size() - 1).id + ".jar"));
        String bottom = versions.get(versions.size() - 1).id;
        StringBuilder text = new StringBuilder("# " + in.getFileName() + " -> " + bottom + "\n\n");
        text.append(problems.isEmpty() ? "Nothing left: every reference came down.\n" : problems.size() + " problems (strict: the mod isn't converted):\n\n");
        problems.forEach(p -> text.append("- ").append(p).append('\n'));
        if (!unverified.isEmpty()) {
            text.append("\nUnverified (formats the converter can't check yet):\n\n");
            unverified.forEach(p -> text.append("- ").append(p).append('\n'));
        }
        Files.writeString(report, text);
        System.out.println(problems.isEmpty() ? "OK " + in.getFileName() : problems.size() + " problems in " + in.getFileName() + ", see " + report);
        System.exit(problems.isEmpty() ? 0 : 1);
    }

    // ---------------------------------------------------------------- one jar

    static void convert(Path in, Path out) throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        try (ZipFile zip = new ZipFile(in.toFile())) {
            for (ZipEntry e : Collections.list(zip.entries())) {
                if (!e.isDirectory()) {
                    try (InputStream s = zip.getInputStream(e)) {
                        entries.put(e.getName(), s.readAllBytes());
                    }
                }
            }
        }
        // an embedded jar is converted with its own classes: keep the outer jar's
        Map<String, Stage0.ModClass> outerMod = mod;
        Map<String, List<String>> outerMixins = mixinTargets;
        mod = new HashMap<>();
        mixinTargets = new HashMap<>();
        try {
            convertEntries(in, out, entries);
        } finally {
            mod = outerMod;
            mixinTargets = outerMixins;
        }
        drainCarry();
    }

    static void convertEntries(Path in, Path out, Map<String, byte[]> entries) throws IOException {
        for (var e : entries.entrySet()) {
            if (e.getKey().endsWith(".class") && !e.getKey().startsWith("META-INF/")) {
                ClassNode node = new ClassNode();
                new ClassReader(e.getValue()).accept(node, ClassReader.SKIP_CODE);
                Stage0.ModClass mc = new Stage0.ModClass();
                mc.superName = node.superName;
                mc.interfaces = node.interfaces.toArray(new String[0]);
                for (MethodNode m : node.methods) {
                    if ((m.access & Opcodes.ACC_STATIC) == 0) {
                        mc.methods.add(m.name + " " + m.desc);
                    }
                }
                mod.put(node.name, mc);
                List<String> targets = mixinTargetsOf(node);
                if (!targets.isEmpty()) {
                    mixinTargets.put(node.name, targets);
                }
            }
        }
        remapper = remapper != null ? remapper : new Remapper() {
            @Override
            public String map(String internal) {
                return Stage0.platform(internal) || NEOFORGE_MOVES.containsKey(internal) ? mapClass(internal) : internal;
            }

            @Override
            public String mapMethodName(String owner, String name, String desc) {
                if (name.startsWith("<")) {
                    // constructors aren't inherited: a call to a platform class's constructor must exist below as it is
                    if (name.equals("<init>") && Stage0.platform(owner)) {
                        validateConstructor(owner, desc);
                    }
                    return name;
                }
                return mapMember(owner, name, desc, true);
            }

            @Override
            public String mapFieldName(String owner, String name, String desc) {
                return mapMember(owner, name, desc, false);
            }
        };
        String bottom = versions.get(versions.size() - 1).id;
        boolean newMetadata = entries.containsKey("META-INF/neoforge.mods.toml");
        Set<String> written = new HashSet<>();
        // FML 4's entry points FML 2 can't take (@Mod(dist), several @Mod classes, no @Mod class): one generated each
        Map<String, byte[]> generatedEntries = new LinkedHashMap<>();
        Set<String> stripMod = new HashSet<>();
        if (newMetadata) {
            modEntries(new String(entries.get("META-INF/neoforge.mods.toml"), StandardCharsets.UTF_8), entries, generatedEntries, stripMod);
        }
        // the embedded jars the loader loads: the ones jarjar's metadata names (anywhere), and those in its folder
        Set<String> nestedJars = new HashSet<>();
        byte[] jarjar = entries.get("META-INF/jarjar/metadata.json");
        if (jarjar != null) {
            try {
                for (JsonElement j : JsonParser.parseString(new String(jarjar, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("jars")) {
                    nestedJars.add(j.getAsJsonObject().get("path").getAsString());
                }
            } catch (RuntimeException e) {
                problem("META-INF/jarjar/metadata.json doesn't parse: " + e.getMessage());
            }
        }
        for (String path : nestedJars) {
            if (!entries.containsKey(path)) {
                problem("jarjar names " + path + ", which the jar hasn't");
            }
        }
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(out))) {
            for (var e : entries.entrySet()) {
                String name = e.getKey();
                byte[] data = e.getValue();
                if (name.endsWith(".class") && !name.startsWith("META-INF/")) {
                    ClassNode node = new ClassNode();
                    new ClassReader(data).accept(node, 0);
                    if (mixinTargets.containsKey(node.name)) {
                        remapMixinSelectors(node, remapper);
                    }
                    if (stripMod.contains(node.name) && node.visibleAnnotations != null) {
                        node.visibleAnnotations.removeIf(a -> a.desc.equals(MOD_ANNOTATION));
                    }
                    List<MethodNode> lowerOverrides = overrideBridges(node);
                    applyRules(node, true);
                    redirectCarried(node);
                    ClassNode remapped = new ClassNode();
                    node.accept(new ClassRemapper(remapped, remapper));
                    fixInterfaceCalls(remapped);
                    lowerOverrides.addAll(addAfterRemap.getOrDefault(node.name, List.of()));
                    addAfterRemap.remove(node.name);
                    for (MethodNode g : lowerOverrides) {
                        if (remapped.methods.stream().noneMatch(x -> x.name.equals(g.name) && x.desc.equals(g.desc))) {
                            remapped.methods.add(g);
                        }
                    }
                    ClassWriter writer = new ClassWriter(0);
                    remapped.accept(writer);
                    data = writer.toByteArray();
                    name = remapper.map(name.substring(0, name.length() - 6)) + ".class";
                } else if (name.equals("META-INF/mods.toml") && newMetadata) {
                    // a jar for both versions: the 1.21 metadata is the one converted, this one is the older loader's
                    continue;
                } else if (name.equals("META-INF/neoforge.mods.toml")) {
                    name = "META-INF/mods.toml";
                    data = modsToml(new String(data, StandardCharsets.UTF_8), bottom).getBytes(StandardCharsets.UTF_8);
                } else if (name.endsWith("mixins.json") || name.endsWith("mixin.json")) {
                    // the bottom version's Java and Mixin (1.20.4: Java 17, Mixin 0.8.5; 1.21's configs ask for 0.8.7)
                    String json = new String(data, StandardCharsets.UTF_8).replaceAll("\"JAVA_2[0-9]\"", "\"JAVA_17\"");
                    String mixin = MIXIN_VERSIONS.get(bottom);
                    if (mixin != null) {
                        json = json.replaceAll("(\"minVersion\"\\s*:\\s*)\"0\\.8\\.([6-9]|[1-9][0-9])[^\"]*\"", "$1\"" + mixin + "\"");
                    }
                    data = json.getBytes(StandardCharsets.UTF_8);
                } else if (name.endsWith(".jar") && (nestedJars.contains(name) || name.startsWith("META-INF/jarjar/"))) {
                    data = nested(name, data);
                    if (data == null) {
                        continue;
                    }
                } else if (name.startsWith("data/")) {
                    String renamed = dataPath(name);
                    if (renamed == null) {
                        problem("data file with no " + bottom + " place: " + name);
                        continue;
                    }
                    if (!renamed.equals(name) && entries.containsKey(renamed)) {
                        // the jar has the older folder too (a jar for both versions): that one is 1.20.4's
                        continue;
                    }
                    if (name.endsWith(".json")) {
                        data = dataJson(renamed, new String(data, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
                    }
                    name = renamed;
                }
                if (!written.add(name)) {
                    problem("two entries for " + name);
                    continue;
                }
                zip.putNextEntry(new ZipEntry(name));
                zip.write(data);
                zip.closeEntry();
            }
            for (var e : generatedEntries.entrySet()) {
                if (!written.add(e.getKey())) {
                    problem("two entries for " + e.getKey());
                    continue;
                }
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue());
                zip.closeEntry();
            }
        }
    }

    // ---------------------------------------------------------------- mod entry points

    static final String MOD_ANNOTATION = "Lnet/neoforged/fml/common/Mod;";
    /** the Mixin each version's loader has (its sponge-mixin) */
    static final Map<String, String> MIXIN_VERSIONS = Map.of("1.20.4", "0.8.5", "1.20.6", "0.8.5", "1.21", "0.8.7", "1.21.1", "0.8.7");

    /**
     * FML 4 constructs each {@code @Mod} class of a mod whose {@code dist} holds the running side, and loads a javafml
     * mod with no {@code @Mod} class; FML 2 takes one class per mod, constructed on both sides. Such a mod gets one
     * generated entry class ({@code ladder/entry/<mod>/ModEntry}) that constructs the mod's own classes through the
     * adapters' {@code ModEntries}; their {@code @Mod} annotations go.
     */
    static void modEntries(String toml, Map<String, byte[]> entries, Map<String, byte[]> out, Set<String> stripMod) {
        // the loader's own TOML parser (night-config), so the file reads as FML reads it
        com.electronwill.nightconfig.core.Config config;
        try {
            config = new com.electronwill.nightconfig.toml.TomlParser().parse(toml);
        } catch (RuntimeException e) {
            problem("neoforge.mods.toml doesn't parse: " + e.getMessage());
            return;
        }
        if (!"javafml".equals(config.get("modLoader"))) {
            return;
        }
        List<String> ids = new ArrayList<>();
        Object mods = config.get("mods");
        if (mods instanceof List<?> list) {
            for (Object m : list) {
                if (m instanceof com.electronwill.nightconfig.core.Config modConfig && modConfig.get("modId") instanceof String id) {
                    ids.add(id);
                }
            }
        }
        // mod id -> its @Mod classes, each "name<TAB>sides"
        Map<String, List<String>> classes = new LinkedHashMap<>();
        Set<String> sided = new HashSet<>();
        for (var e : entries.entrySet()) {
            if (!e.getKey().endsWith(".class") || e.getKey().startsWith("META-INF/")) {
                continue;
            }
            ClassNode node = new ClassNode();
            new ClassReader(e.getValue()).accept(node, ClassReader.SKIP_CODE);
            if (node.visibleAnnotations == null) {
                continue;
            }
            for (AnnotationNode a : node.visibleAnnotations) {
                if (!a.desc.equals(MOD_ANNOTATION) || a.values == null) {
                    continue;
                }
                String id = null;
                List<String> sides = new ArrayList<>();
                for (int i = 0; i + 1 < a.values.size(); i += 2) {
                    Object v = a.values.get(i + 1);
                    if ("value".equals(a.values.get(i))) {
                        id = (String) v;
                    } else if ("dist".equals(a.values.get(i)) && v instanceof List<?> list) {
                        for (Object d : list) {
                            sides.add(((String[]) d)[1]);
                        }
                    }
                }
                if (id != null) {
                    classes.computeIfAbsent(id, k -> new ArrayList<>()).add(node.name.replace('/', '.') + "\t" + String.join(",", sides));
                    if (!sides.isEmpty()) {
                        sided.add(id);
                    }
                }
            }
        }
        for (String id : ids) {
            List<String> own = classes.getOrDefault(id, List.of());
            if (own.size() == 1 && !sided.contains(id)) {
                continue;
            }
            for (String line : own) {
                stripMod.add(line.substring(0, line.indexOf('\t')).replace('.', '/'));
            }
            String name = "ladder/entry/" + id.replaceAll("[^A-Za-z0-9_]", "_") + "/ModEntry";
            out.put(name + ".class", entryClass(name, id, String.join("\n", own)));
        }
    }

    static byte[] entryClass(String name, String id, String lines) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, name, null, "java/lang/Object", null);
        AnnotationVisitor mod = cw.visitAnnotation(MOD_ANNOTATION, true);
        mod.visit("value", id);
        mod.visitEnd();
        String desc = "(Lnet/neoforged/bus/api/IEventBus;Lnet/neoforged/fml/ModContainer;Lnet/neoforged/api/distmarker/Dist;)V";
        MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", desc, null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitLdcInsn(Type.getObjectType(name));
        mv.visitLdcInsn(lines);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "ladder/adapter/fml/ModEntries", "construct",
                "(Ljava/lang/Class;Ljava/lang/String;Lnet/neoforged/bus/api/IEventBus;Lnet/neoforged/fml/ModContainer;Lnet/neoforged/api/distmarker/Dist;)V", false);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        cw.visitEnd();
        return cw.toByteArray();
    }

    /** An embedded library jar, converted like the mod; its problems are the mod's. */
    static byte[] nested(String name, byte[] data) throws IOException {
        Path tmpIn = Files.createTempFile("ladder-in", ".jar");
        Path tmpOut = Files.createTempFile("ladder-out", ".jar");
        try {
            Files.write(tmpIn, data);
            int before = problems.size();
            Set<String> mine = new TreeSet<>(problems);
            problems.clear();
            convert(tmpIn, tmpOut);
            Set<String> inner = new TreeSet<>(problems);
            problems.clear();
            problems.addAll(mine);
            inner.forEach(p -> problem("in " + name + ": " + p));
            return problems.size() > before && !inner.isEmpty() ? null : Files.readAllBytes(tmpOut);
        } finally {
            Files.deleteIfExists(tmpIn);
            Files.deleteIfExists(tmpOut);
        }
    }

    // ---------------------------------------------------------------- names

    // ---------------------------------------------------------------- the adapters' rules (ladder-rules.tsv)

    /** P: top-version class -> the class the adapters provide */
    static final Map<String, String> provided = new HashMap<>();
    /** O rows: {owner, name, desc, interface, defaults class, defaults method} */
    static final List<String[]> overrides = new ArrayList<>();
    /** X: "owner.name desc" -> {adapter class, adapter method} */
    static final Map<String, String[]> redirects = new HashMap<>();
    /** the adapters jar: class -> its methods "name desc" (each rule's target is checked against it) */
    static final Map<String, Set<String>> adapterMethods = new HashMap<>();

    static void loadRules() throws IOException {
        String rules = System.getProperty("ladder.rules");
        if (rules != null) {
            for (String line : Files.readAllLines(Paths.get(rules))) {
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] c = line.split("\t");
                switch (c[0]) {
                    case "P" -> provided.put(c[1], c[2]);
                    case "O" -> overrides.add(new String[] {c[1], c[2], c[3], c[4], c[5], c[6]});
                    case "X" -> redirects.put(c[1] + "." + c[2] + " " + c[3], new String[] {c[4], c[5]});
                    // a class renamed below with the same members: its uses get the lower name
                    case "R" -> NEOFORGE_MOVES.put(c[1], c[2]);
                    default -> throw new IOException("unknown rule " + line);
                }
            }
        }
        String jar = System.getProperty("ladder.adapters");
        if (jar != null) {
            try (ZipFile zip = new ZipFile(jar)) {
                for (ZipEntry e : Collections.list(zip.entries())) {
                    if (!e.getName().endsWith(".class")) {
                        continue;
                    }
                    try (InputStream s = zip.getInputStream(e)) {
                        ClassNode node = new ClassNode();
                        new ClassReader(s).accept(node, ClassReader.SKIP_CODE);
                        Set<String> methods = new HashSet<>();
                        node.methods.forEach(m -> methods.add(m.name + " " + m.desc));
                        adapterMethods.put(node.name, methods);
                    }
                }
            }
        }
    }

    /** The O row a platform method is (owner or a subclass of its owner, same name and descriptor), or null. */
    static String[] overrideRule(String decl, String name, String desc) {
        Stage0.Version here = versions.get(level);
        for (String[] o : overrides) {
            if (o[1].equals(name) && o[2].equals(desc) && extendsClass(here, decl, o[0])) {
                return o;
            }
        }
        return null;
    }

    /** A member of a provided class the mod uses must be in the adapters' class (or it fails at run time). */
    static void checkProvided(String cls, String name, String upperDesc) {
        if (adapterMethods.isEmpty()) {
            return;
        }
        Set<String> methods = adapterMethods.get(cls);
        String desc = remapper.mapMethodDesc(upperDesc);
        if (methods == null || !methods.contains(name + " " + desc)) {
            problem("the adapters' " + cls + " has no " + name + desc);
        }
    }

    /** The adapters' static method a rule names must be in the adapters jar, with the converted descriptor. */
    static void checkAdapter(String owner, String name, String upperDesc, String rule) {
        if (adapterMethods.isEmpty()) {
            return;
        }
        String desc = remapper.mapMethodDesc(upperDesc);
        Set<String> methods = adapterMethods.get(owner);
        if (methods == null || !methods.contains(name + " " + desc)) {
            problem("adapter " + owner + "." + name + desc + " missing (" + rule + ")");
        }
    }

    /**
     * Before the names: a mod class under an O row's owner gets the row's interface (its overrides public); a super
     * call into the game's own class goes to the defaults; an X row's call goes to its adapter.
     */
    static void applyRules(ClassNode node, boolean modClass) {
        if (overrides.isEmpty() && redirects.isEmpty()) {
            return;
        }
        Stage0.Version here = versions.get(level);
        // a mixin's interfaces would go onto the game's own class it targets: only the mod's own classes get them
        if (modClass && !mixinTargets.containsKey(node.name)) {
            List<String> platform = starts(node.name);
            for (String[] o : overrides) {
                boolean under = platform.stream().anyMatch(p -> extendsClass(here, p, o[0]));
                if (under && !node.interfaces.contains(o[3]) && (node.access & Opcodes.ACC_INTERFACE) == 0) {
                    node.interfaces.add(o[3]);
                }
                if (!under) {
                    continue;
                }
                for (MethodNode m : node.methods) {
                    if (m.name.equals(o[1]) && m.desc.equals(o[2]) && (m.access & Opcodes.ACC_STATIC) == 0) {
                        // the interface's method is public: so is its implementation
                        m.access = (m.access & ~(Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE)) | Opcodes.ACC_PUBLIC;
                    }
                }
            }
        }
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode insn : m.instructions) {
                if (!(insn instanceof MethodInsnNode call) || call.owner.startsWith("[") || call.name.startsWith("<")) {
                    continue;
                }
                Stage0.Ref ref = resolve(call.owner, call.name, call.desc, true);
                if (ref == null) {
                    continue;
                }
                String[] x = redirects.get(ref.cls() + "." + ref.name() + " " + ref.desc());
                if (x != null) {
                    String desc = call.getOpcode() == Opcodes.INVOKESTATIC ? call.desc : "(L" + ref.cls() + ";" + call.desc.substring(1);
                    checkAdapter(x[0], x[1], desc, "X " + ref.key());
                    call.setOpcode(Opcodes.INVOKESTATIC);
                    call.owner = x[0];
                    call.name = x[1];
                    call.desc = desc;
                    call.itf = false;
                    continue;
                }
                String[] o = overrideRule(ref.cls(), ref.name(), ref.desc());
                if (o == null) {
                    continue;
                }
                if (call.getOpcode() == Opcodes.INVOKESPECIAL && Stage0.platform(call.owner)) {
                    // super into the game's class: exact when that class doesn't override it in the newer version
                    // (an O row of its own covers the ones that do, like BlockItem.appendHoverText)
                    if (!ref.cls().equals(o[0])) {
                        problem("super call to " + ref.key() + ", overridden by the game's " + ref.cls() + ": no bridge for that override");
                    }
                    String desc = "(L" + o[0] + ";" + call.desc.substring(1);
                    checkAdapter(o[4], o[5], desc, "O " + o[0] + "." + o[1]);
                    call.setOpcode(Opcodes.INVOKESTATIC);
                    call.owner = o[4];
                    call.name = o[5];
                    call.desc = desc;
                    call.itf = false;
                } else if (Stage0.platform(call.owner)) {
                    problem("call of the newer " + ref.key() + " through the game's own " + call.owner + ": no dispatcher yet");
                }
                // through a mod class: it has the method (its own, or the interface's default)
            }
        }
    }

    static String mapClass(String c) {
        String cacheKey = level + " " + c;
        String cached = classCache.get(cacheKey);
        if (cached != null) {
            cachedProblems.getOrDefault("C " + cacheKey, List.of()).forEach(problems::add);
            return cached;
        }
        List<String> outer = collecting;
        collecting = new ArrayList<>();
        String result;
        if (provided.containsKey(c)) {
            result = provided.get(c);
        } else if (NEOFORGE_MOVES.containsKey(c)) {
            result = NEOFORGE_MOVES.get(c);
        } else {
            String cur = c;
            for (int i = level; i + 1 < versions.size(); i++) {
                Stage0.Step s = Stage0.step(versions.get(i), versions.get(i + 1), new Stage0.Ref('C', cur, null, null));
                if (s.to() == null) {
                    if (carriable(i).contains("C " + cur)) {
                        // new in the upper version and its code works below: carried, in its own package tree
                        schedule(i, new Stage0.Ref('C', cur, null, null));
                        cur = CARRIED + cur;
                    } else {
                        problem("missing class " + c + " (" + versions.get(i).id + " -> " + versions.get(i + 1).id + ")");
                        cur = c;
                    }
                    break;
                }
                cur = s.to().cls();
            }
            result = cur;
        }
        cachedProblems.put("C " + cacheKey, collecting);
        if (outer != null) {
            outer.addAll(collecting);
        }
        collecting = outer;
        classCache.put(cacheKey, result);
        return result;
    }

    /** The first platform classes above a class: through the mod's own classes, and a mixin's targets. */
    static List<String> starts(String owner) {
        List<String> starts = new ArrayList<>();
        Deque<String> todo = new ArrayDeque<>(List.of(owner));
        Set<String> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            String c = todo.poll();
            if (c == null || !seen.add(c)) {
                continue;
            }
            List<String> targets = mixinTargets.get(c);
            if (targets != null) {
                // a mixin's members (@Shadow, overrides, accessors) are those of the classes it is applied to
                starts.addAll(targets);
            }
            Stage0.platformStarts(c, mod, starts, new HashSet<>());
        }
        return starts;
    }

    /** The platform member a reference resolves to at the current level, or null when it isn't one. */
    static Stage0.Ref resolve(String owner, String name, String desc, boolean method) {
        Stage0.Version here = versions.get(level);
        for (String start : starts(owner)) {
            String decl = here.declaring(start, name, desc, method);
            if (decl != null) {
                return new Stage0.Ref(method ? 'M' : 'F', decl, name, desc);
            }
        }
        return null;
    }

    /** The member's final name, or null when it isn't a platform member at all. */
    static String finalName(String owner, String name, String desc, boolean method) {
        Stage0.Ref ref = resolve(owner, name, desc, method);
        if (ref == null) {
            return null;
        }
        Stage0.Ref end = walk(ref);
        return end == null ? name : end.name();
    }

    static String mapMember(String owner, String name, String desc, boolean method) {
        if (owner.startsWith("[") || owner.startsWith(CARRIED) || provided.containsValue(owner)) {
            return name;
        }
        if (method) {
            Stage0.Ref ref = resolve(owner, name, desc, true);
            if (ref != null && provided.containsKey(ref.cls())) {
                checkProvided(provided.get(ref.cls()), name, desc);
                return name;
            }
            if (ref != null && overrideRule(ref.cls(), name, desc) != null) {
                // an O row's method: the mod's override, the interface's default, the defaults for super calls
                return name;
            }
        } else {
            Stage0.Ref ref = resolve(owner, name, desc, false);
            if (ref != null && provided.containsKey(ref.cls())) {
                return name;
            }
        }
        String mapped = finalName(owner, name, desc, method);
        return mapped == null ? name : mapped;
    }

    static void validateConstructor(String owner, String desc) {
        if (versions.get(level).declaring(owner, "<init>", desc, true) != null) {
            walk(new Stage0.Ref('M', owner, "<init>", desc));
        }
    }

    static Stage0.Ref walk(Stage0.Ref ref) {
        String key = level + " " + ref.key();
        if (memberCache.containsKey(key)) {
            cachedProblems.getOrDefault(key, List.of()).forEach(problems::add);
            return memberCache.get(key);
        }
        List<String> outer = collecting;
        collecting = new ArrayList<>();
        Stage0.Ref result = walkUncached(ref, key);
        cachedProblems.put(key, collecting);
        if (outer != null) {
            outer.addAll(collecting);
        }
        collecting = outer;
        memberCache.put(key, result);
        return result;
    }

    static Stage0.Ref walkUncached(Stage0.Ref ref, String key) {
        Stage0.Ref cur = ref;
        for (int i = level; i + 1 < versions.size() && cur != null; i++) {
            if (provided.containsKey(cur.cls())) {
                // a class the adapters provide: its members are theirs, at every rung (whatever the versions between do)
                return new Stage0.Ref(cur.kind(), provided.get(cur.cls()), cur.name(), cur.desc());
            }
            Stage0.Version from = versions.get(i), to = versions.get(i + 1);
            Stage0.Step s = Stage0.step(from, to, cur);
            switch (s.outcome()) {
                case SAME, RENAMED -> cur = s.to();
                case CHANGED -> {
                    String[] bridge = cur.name().equals("<init>") ? constructorBridge(i, cur) : bridgeFor(i, cur, s.to());
                    if (bridge != null) {
                        // the call moves to a generated bridge that converts the values (Holder<X> <-> X) and calls below
                        callRedirect.put(key, bridge);
                        return new Stage0.Ref(cur.kind(), bridge[0], bridge[1], cur.desc());
                    }
                    problem("changed descriptor, no bridge yet: " + cur.key() + " -> " + s.to().name() + s.to().desc() + " (" + from.id + " -> " + to.id + ")");
                    cur = s.to();
                }
                default -> {
                    if (provided.containsKey(cur.cls())) {
                        // a class the adapters provide: its members are theirs
                        return new Stage0.Ref(cur.kind(), provided.get(cur.cls()), cur.name(), cur.desc());
                    }
                    if (NEOFORGE_MOVES.containsKey(cur.cls())) {
                        // a class renamed below (an R row): its members are the renamed class's, never carried
                        return renamedMember(cur);
                    }
                    Set<String> ok = carriable(i);
                    if (Stage0.mapClass(from, to, cur.cls()) == null) {
                        // the owner itself is new: its member comes down with the carried class (mapClass relocates it)
                        if (ok.contains("C " + cur.cls())) {
                            schedule(i, new Stage0.Ref('C', cur.cls(), null, null));
                            return new Stage0.Ref(cur.kind(), CARRIED + cur.cls(), cur.name(), cur.desc());
                        }
                    } else if (ok.contains(cur.key())) {
                        // a new static method of a class that exists below: into the companion class, the calls move there
                        schedule(i, cur);
                        staticRedirect.put(key, CARRIED + cur.cls());
                        return new Stage0.Ref(cur.kind(), CARRIED + cur.cls(), cur.name(), cur.desc());
                    }
                    if (cur.name().equals("<init>")) {
                        String[] bridge = constructorBridge(i, cur);
                        if (bridge != null) {
                            callRedirect.put(key, bridge);
                            return new Stage0.Ref(cur.kind(), bridge[0], bridge[1], cur.desc());
                        }
                    }
                    problem("missing " + cur.key() + " (" + from.id + " -> " + to.id + ")");
                    return null;
                }
            }
        }
        if (inCarried && cur != null && !cur.cls().startsWith(CARRIED)) {
            checkAccess(cur);
        }
        return cur;
    }

    /** A member of a class an R row renames: the same member of the renamed class, which the bottom version must have. */
    static Stage0.Ref renamedMember(Stage0.Ref cur) {
        String cls = NEOFORGE_MOVES.get(cur.cls());
        boolean method = cur.kind() == 'M';
        String desc = method ? remapper.mapMethodDesc(cur.desc()) : remapper.mapDesc(cur.desc());
        if (versions.get(versions.size() - 1).declaring(cls, cur.name(), desc, method) == null) {
            problem("missing " + cur.key() + " in its renamed class " + cls + " below");
            return null;
        }
        return new Stage0.Ref(cur.kind(), cls, cur.name(), cur.desc());
    }

    /** Carried code lives in its own package: what it calls below must be public (or it fails with IllegalAccessError). */
    static void checkAccess(Stage0.Ref end) {
        Stage0.Version bottom = versions.get(versions.size() - 1);
        Stage0.Cls info = bottom.classes.get(end.cls());
        if (info == null) {
            return;
        }
        Integer access = info.access.get(end.kind() + " " + end.name() + " " + end.desc());
        if (access != null && (access & Opcodes.ACC_PUBLIC) == 0 && (access & Opcodes.ACC_PROTECTED) == 0) {
            problem("carried code needs a non-public " + end.key() + " in " + bottom.id);
        }
    }

    // ---------------------------------------------------------------- carried code

    /** What the converter can carry down at rung i: classes, static methods; everything they need carried too. */
    static Set<String> carriable(int i) {
        Set<String> got = carriable.get(i);
        if (got != null) {
            return got;
        }
        Stage0.Version from = versions.get(i);
        Stage0.Plan plan = Stage0.plan(from, versions.get(i + 1));
        Set<String> ok = new HashSet<>();
        Map<String, Map<String, Set<Stage0.Ref>>> code = new HashMap<>();
        for (String key : plan.carried) {
            Stage0.Ref r = plan.things.get(key);
            if (!(r.kind() == 'C' || r.kind() == 'M' && from.classes.get(r.cls()).staticMethods.contains(r.name() + " " + r.desc()))) {
                continue;
            }
            // carried code sits in its own package: what it reaches below must be public there (or protected in a
            // class it extends), else it fails at run time with IllegalAccessError
            var classCode = code.computeIfAbsent(r.cls(), c -> Stage0.codeRefs(from, c));
            Set<Stage0.Ref> refs = new HashSet<>();
            if (r.kind() == 'C') {
                classCode.values().forEach(refs::addAll);
            } else {
                refs.addAll(classCode.getOrDefault(r.name() + " " + r.desc(), Set.of()));
            }
            if (reachable(i, r, refs) && contractsHold(i, r)) {
                ok.add(key);
            }
        }
        // what the adapters provide, and what an R row renames, is there below too (and never carried)
        Set<String> there = new HashSet<>(provided.keySet());
        there.addAll(NEOFORGE_MOVES.keySet());
        ok.removeIf(k -> there.contains(k.substring(2).split(" ")[0]));
        for (String p : there) {
            if (plan.needs.containsKey("C " + p)) {
                ok.add("C " + p);
            }
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (var it = ok.iterator(); it.hasNext();) {
                Stage0.Needs n = plan.needs.get(it.next());
                for (String d : n.deps) {
                    if (plan.needs.containsKey(d) && !ok.contains(d)) {
                        it.remove();
                        changed = true;
                        break;
                    }
                }
            }
        }
        carriable.put(i, ok);
        return ok;
    }

    /** Whether every member the carried code reaches is accessible from the carried package at the bottom version. */
    static boolean reachable(int rung, Stage0.Ref thing, Set<Stage0.Ref> refs) {
        Stage0.Version bottom = versions.get(versions.size() - 1);
        for (Stage0.Ref ref : refs) {
            if (ref.kind() == 'C' || ref.cls().equals(thing.cls()) && thing.kind() == 'C') {
                // types, and a carried class's own members (they come down with it)
                continue;
            }
            Stage0.Ref end = quietWalk(rung, ref);
            if (end == null) {
                // missing below: the plan's dependencies decide (a carried thing or not carriable)
                continue;
            }
            Stage0.Cls info = bottom.classes.get(end.cls());
            Integer access = info == null ? null : info.access.get(end.kind() + " " + end.name() + " " + end.desc());
            if (access == null || (access & Opcodes.ACC_PUBLIC) != 0) {
                continue;
            }
            if ((access & Opcodes.ACC_PROTECTED) != 0 && thing.kind() == 'C' && extendsClass(versions.get(rung), thing.cls(), ref.cls())) {
                continue;
            }
            if (thing.kind() == 'M' && copyAlong(rung, thing, ref, new HashSet<>())) {
                continue;
            }
            return false;
        }
        return true;
    }

    /** rung + carried static method -> the private static helpers of its class copied along with it ("name desc") */
    static final Map<String, Set<String>> helpers = new HashMap<>();

    /**
     * A private static helper of the carried method's own class (ResourceLocation.assertValidNamespace): its upper-version
     * code is copied into the companion with the method, when that code is reachable itself.
     */
    static boolean copyAlong(int rung, Stage0.Ref thing, Stage0.Ref ref, Set<String> seen) {
        Stage0.Version from = versions.get(rung);
        Stage0.Cls owner = from.classes.get(thing.cls());
        if (ref.kind() != 'M' || !ref.cls().equals(thing.cls()) || owner == null || !owner.staticMethods.contains(ref.name() + " " + ref.desc())
                || !seen.add(ref.name() + ref.desc()) || seen.size() > 8) {
            return false;
        }
        Set<Stage0.Ref> body = Stage0.codeRefs(from, thing.cls()).get(ref.name() + " " + ref.desc());
        if (body == null) {
            return false;
        }
        Stage0.Version bottom = versions.get(versions.size() - 1);
        for (Stage0.Ref inner : body) {
            if (inner.kind() == 'C') {
                continue;
            }
            Stage0.Ref end = quietWalk(rung, inner);
            if (end == null) {
                return false;
            }
            Stage0.Cls info = bottom.classes.get(end.cls());
            Integer access = info == null ? null : info.access.get(end.kind() + " " + end.name() + " " + end.desc());
            if (access != null && (access & Opcodes.ACC_PUBLIC) == 0 && !copyAlong(rung, thing, inner, seen)) {
                return false;
            }
        }
        helpers.computeIfAbsent(rung + " " + thing.key(), k -> new HashSet<>()).add(ref.name() + " " + ref.desc());
        return true;
    }

    /** A reference's form at the bottom version through the rungs, with no carrying and no problems: null if it stops. */
    static Stage0.Ref quietWalk(int rung, Stage0.Ref ref) {
        Stage0.Ref cur = ref;
        for (int i = rung; i + 1 < versions.size() && cur != null; i++) {
            Stage0.Step s = Stage0.step(versions.get(i), versions.get(i + 1), cur);
            cur = s.outcome() == Stage0.Outcome.SAME || s.outcome() == Stage0.Outcome.RENAMED ? s.to() : null;
        }
        return cur;
    }

    /**
     * What carried code is to the game below must be what the game expects of it there:
     * <ul>
     * <li>no event class: the game below never posts it, a mod's handler would silently never run (an event bridge
     * is an adapter);</li>
     * <li>a carried class extending or implementing a game type implements that type's abstract methods as the
     * lower version has them;</li>
     * <li>a lambda of carried code made for a game interface implements that interface's abstract method as the lower
     * version has it (EntityDataSerializer.ForValueType: codec() above, read/write below).</li>
     * </ul>
     */
    static boolean contractsHold(int rung, Stage0.Ref thing) {
        Stage0.Version from = versions.get(rung);
        if (thing.kind() == 'C') {
            if (extendsClass(from, thing.cls(), "net/neoforged/bus/api/Event")) {
                return false;
            }
            Stage0.Cls info = from.classes.get(thing.cls());
            if (info != null) {
                List<String> supers = new ArrayList<>(Arrays.asList(info.interfaces));
                supers.add(info.superName);
                for (String s : supers) {
                    if (s != null && Stage0.platform(s) && Stage0.mapClass(from, versions.get(rung + 1), s) != null
                            && !implementsBelow(rung, thing.cls(), s)) {
                        return false;
                    }
                }
            }
        }
        byte[] bytes = from.bytes.get(thing.cls());
        if (bytes == null) {
            return true;
        }
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_FRAMES);
        for (MethodNode m : node.methods) {
            if (thing.kind() == 'M' && !(m.name.equals(thing.name()) && m.desc.equals(thing.desc()))) {
                continue;
            }
            for (AbstractInsnNode insn : m.instructions) {
                if (insn instanceof InvokeDynamicInsnNode indy && indy.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")) {
                    String face = Type.getReturnType(indy.desc).getInternalName();
                    if (Stage0.platform(face) && Stage0.mapClass(from, versions.get(rung + 1), face) != null
                            && !hasAbstractBelow(rung, face, indy.name, Type.getArgumentTypes(((Type) indy.bsmArgs[0]).getDescriptor()).length)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** The lower version's interface (or its parents) has an abstract method of that name and parameter count. */
    static boolean hasAbstractBelow(int rung, String type, String name, int parameters) {
        Stage0.Version bottom = versions.get(versions.size() - 1);
        Stage0.Ref end = quietWalk(rung, new Stage0.Ref('C', type, null, null));
        Deque<String> todo = new ArrayDeque<>(List.of(end == null ? type : end.cls()));
        Set<String> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            Stage0.Cls info = bottom.classes.get(todo.poll());
            if (info == null) {
                continue;
            }
            for (var e : info.access.entrySet()) {
                String[] k = e.getKey().split(" ");
                if (k[0].equals("M") && k[1].equals(name) && (e.getValue() & Opcodes.ACC_ABSTRACT) != 0
                        && Type.getArgumentTypes(k[2]).length == parameters) {
                    return true;
                }
            }
            for (String i : info.interfaces) {
                if (seen.add(i)) {
                    todo.add(i);
                }
            }
        }
        return false;
    }

    /** Every abstract method the lower version's supertype asks for has a method of that name in the carried class. */
    static boolean implementsBelow(int rung, String cls, String supertype) {
        Stage0.Version from = versions.get(rung);
        Stage0.Version bottom = versions.get(versions.size() - 1);
        Stage0.Ref end = quietWalk(rung, new Stage0.Ref('C', supertype, null, null));
        Set<String> mine = new HashSet<>();
        for (String c = cls; c != null && from.classes.containsKey(c); c = from.classes.get(c).superName) {
            from.classes.get(c).methods.forEach(m -> mine.add(m.substring(0, m.indexOf(' ')) + "/" + Type.getArgumentTypes(m.substring(m.indexOf(' ') + 1)).length));
        }
        Deque<String> todo = new ArrayDeque<>(List.of(end == null ? supertype : end.cls()));
        Set<String> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            String t = todo.poll();
            Stage0.Cls info = bottom.classes.get(t);
            if (info == null || !seen.add(t)) {
                continue;
            }
            for (var e : info.access.entrySet()) {
                String[] k = e.getKey().split(" ");
                if (k[0].equals("M") && (e.getValue() & Opcodes.ACC_ABSTRACT) != 0
                        && !mine.contains(quietName(rung, t, k[1]) + "/" + Type.getArgumentTypes(k[2]).length)) {
                    return false;
                }
            }
            todo.addAll(Arrays.asList(info.interfaces));
            if (info.superName != null) {
                todo.add(info.superName);
            }
        }
        return true;
    }

    /** A bottom-version method name as the upper version calls it (a renamed one: by intermediary), else as it is. */
    static String quietName(int rung, String bottomOwner, String bottomName) {
        return bottomName;
    }

    static boolean extendsClass(Stage0.Version v, String cls, String ancestor) {
        Set<String> seen = new HashSet<>();
        for (String c = cls; c != null && seen.add(c);) {
            if (c.equals(ancestor)) {
                return true;
            }
            Stage0.Cls info = v.classes.get(c);
            c = info == null ? null : info.superName;
        }
        return false;
    }

    static void schedule(int rung, Stage0.Ref thing) {
        if (carryScheduled.add(rung + " " + thing.key())) {
            carryQueue.add(new Object[] {rung, thing});
        }
    }

    /** Calls and method handles of carried static members move to their companion classes. */
    static void redirectCarried(ClassNode node) {
        for (MethodNode m : node.methods) {
            bridgeConstructors(m);
            for (AbstractInsnNode insn : m.instructions) {
                if (insn instanceof MethodInsnNode call && !call.name.startsWith("<") && call.getOpcode() != Opcodes.INVOKESPECIAL
                        && bridgeCall(call)) {
                    continue;
                }
                if (insn instanceof MethodInsnNode call && !call.name.startsWith("<") && call.getOpcode() == Opcodes.INVOKESPECIAL) {
                    String[] bridge = bridgeOf(call.owner, call.name, call.desc, true);
                    if (bridge != null && modDeclares(call.owner, call.name + " " + call.desc)) {
                        // the super implementation is a mod class's own (renamed as this call is): a plain super call
                        // to it; going through its lower override bridge would dispatch back to the subclass
                        continue;
                    }
                    if (bridge != null) {
                        // super.x(...) must stay a super call (a static bridge would call the receiver's own override):
                        // to a private helper of the class that converts and calls the lower method with invokespecial
                        MethodNode helper = inCarried || (node.access & Opcodes.ACC_INTERFACE) != 0 ? null : superHelper(node, call, bridge);
                        if (helper == null) {
                            problem("a super call of " + call.owner + "." + call.name + call.desc + ", whose parameters changed below: no bridge for it");
                        } else {
                            call.owner = node.name;
                            call.name = helper.name;
                            call.itf = false;
                        }
                        continue;
                    }
                }
                if (insn instanceof InvokeDynamicInsnNode indy) {
                    bridgeHandles(indy);
                }
                if (insn instanceof FieldInsnNode field && bridgeField(m, field)) {
                    continue;
                }
                if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESTATIC) {
                    String to = staticTarget(call.owner, call.name, call.desc);
                    if (to != null) {
                        call.owner = to;
                        call.itf = false;
                    }
                } else if (insn instanceof InvokeDynamicInsnNode indy) {
                    for (int i = 0; i < indy.bsmArgs.length; i++) {
                        if (indy.bsmArgs[i] instanceof Handle h && h.getTag() == Opcodes.H_INVOKESTATIC) {
                            String to = staticTarget(h.getOwner(), h.getName(), h.getDesc());
                            if (to != null) {
                                indy.bsmArgs[i] = new Handle(h.getTag(), to, h.getName(), h.getDesc(), false);
                            }
                        }
                    }
                }
            }
        }
    }

    static String staticTarget(String owner, String name, String desc) {
        if (owner.startsWith(CARRIED) || owner.startsWith("[")) {
            return null;
        }
        Stage0.Ref ref = resolve(owner, name, desc, true);
        if (ref == null) {
            return null;
        }
        walk(ref);
        return staticRedirect.get(level + " " + ref.key());
    }


    // ---------------------------------------------------------------- generated bridges (type changes)

    /** walk key -> {bridge class, bridge method}: a changed member's calls go there */
    static final Map<String, String[]> callRedirect = new HashMap<>();
    static final String CONVERSIONS = "ladder/adapter/bridge/Conversions";
    static final String HOLDER = "net/minecraft/core/Holder";
    static final String REFERENCE = "net/minecraft/core/Holder$Reference";

    static final String MAP_CODEC = "com/mojang/serialization/MapCodec";
    static final String CODEC = "com/mojang/serialization/Codec";
    static final String PROVIDER = "net/minecraft/core/HolderLookup$Provider";
    static final String REGISTRY_ACCESS = "net/minecraft/core/RegistryAccess";
    static final String OPTIONAL = "java/util/Optional";

    /**
     * The value-type pairs the bridges convert (upper type, lower type): Holder<X> / X (and Holder.Reference<X> / X),
     * MapCodec / Codec, HolderLookup.Provider / RegistryAccess, X / Optional<X>. 0 when the types are the same, -1 when
     * no conversion is known.
     */
    static int conversion(Type upper, Type lower) {
        if (upper.equals(lower)) {
            return 0;
        }
        if (upper.getSort() == Type.OBJECT && DELTA_TRACKERS.contains(upper.getInternalName()) && lower.getSort() == Type.FLOAT) {
            // 1.21's DeltaTracker where 1.20.4 passes the partial tick
            return 9;
        }
        if (upper.getSort() != Type.OBJECT || lower.getSort() != Type.OBJECT) {
            return -1;
        }
        String u = upper.getInternalName(), l = lower.getInternalName();
        if ((u.equals(HOLDER) || u.equals(REFERENCE)) && !l.equals(HOLDER) && !l.equals(REFERENCE) && !l.startsWith("java/")) {
            return u.equals(HOLDER) ? 1 : 2;
        }
        if (u.equals(MAP_CODEC) && l.equals(CODEC)) {
            return 3;
        }
        if (u.equals(PROVIDER) && l.equals(REGISTRY_ACCESS)) {
            return 4;
        }
        if (l.equals(OPTIONAL) && !u.equals(OPTIONAL) && !u.startsWith("java/")) {
            return 5;
        }
        if (u.equals(RESOURCE_KEY) && l.equals(RESOURCE_LOCATION)) {
            return 7;
        }
        if (u.equals(RESOURCE_KEY) && !l.startsWith("java/") && !l.equals(RESOURCE_KEY)) {
            return 6;
        }
        if (isSubtype(l, u)) {
            // the lower type is the upper one's subtype (a DefaultedRegistry for a Registry): read as it is, passed cast
            return 8;
        }
        return -1;
    }

    static final Set<String> DELTA_TRACKERS = Set.of("net/minecraft/client/DeltaTracker", "ladder/adapter/provided/DeltaTracker");
    static final String RESOURCE_KEY = "net/minecraft/resources/ResourceKey";
    static final String RESOURCE_LOCATION = "net/minecraft/resources/ResourceLocation";

    /** Whether the bottom version's class sub extends or implements sup. */
    static boolean isSubtype(String sub, String sup) {
        Stage0.Version bottom = versions.get(versions.size() - 1);
        Deque<String> todo = new ArrayDeque<>(List.of(sub));
        Set<String> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            String c = todo.poll();
            if (c == null || !seen.add(c)) {
                continue;
            }
            if (c.equals(sup) && !c.equals(sub)) {
                return true;
            }
            Stage0.Cls info = bottom.classes.get(c);
            if (info != null) {
                todo.add(info.superName);
                todo.addAll(Arrays.asList(info.interfaces));
            }
        }
        return false;
    }

    static boolean convertible(Type upper, Type lower) {
        return conversion(upper, lower) >= 0;
    }

    /** A value of the upper type on the stack becomes the lower type's. */
    static void convertDown(InsnList code, Type upper, Type lower) {
        switch (conversion(upper, lower)) {
            case 1, 2 -> {
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "unwrap", "(L" + HOLDER + ";)Ljava/lang/Object;", false));
                code.add(new TypeInsnNode(Opcodes.CHECKCAST, lower.getInternalName()));
            }
            case 3 -> code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, MAP_CODEC, "codec", "()L" + CODEC + ";", false));
            case 4 -> code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "registryAccess", "(L" + PROVIDER + ";)L" + REGISTRY_ACCESS + ";", false));
            case 5 -> code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "optional", "(Ljava/lang/Object;)L" + OPTIONAL + ";", false));
            case 6 -> {
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "valueOfKey", "(L" + RESOURCE_KEY + ";)Ljava/lang/Object;", false));
                code.add(new TypeInsnNode(Opcodes.CHECKCAST, lower.getInternalName()));
            }
            case 7 -> code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "location", "(L" + RESOURCE_KEY + ";)L" + RESOURCE_LOCATION + ";", false));
            case 8 -> code.add(new TypeInsnNode(Opcodes.CHECKCAST, lower.getInternalName()));
            case 9 -> {
                code.add(new InsnNode(Opcodes.ICONST_1));
                code.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, upper.getInternalName(), "getGameTimeDeltaPartialTick", "(Z)F", true));
            }
            default -> {
            }
        }
    }

    /** The bridge for a member whose types changed at rung i, when every change is a known conversion; else null. */
    static String[] bridgeFor(int rung, Stage0.Ref upper, Stage0.Ref lower) {
        if (adapterMethods.get(CONVERSIONS) == null) {
            return null;
        }
        Stage0.Version to = versions.get(rung + 1);
        if (to.classes.get(lower.cls()) == null) {
            return null;
        }
        Stage0.Ref end = quietWalk(rung + 1, lower);
        if (end == null) {
            return null;
        }
        Stage0.Cls bottomOwner = versions.get(versions.size() - 1).classes.get(end.cls());
        Integer access = bottomOwner == null ? null : bottomOwner.access.get(end.kind() + " " + end.name() + " " + end.desc());
        if (access == null || (access & Opcodes.ACC_PUBLIC) == 0) {
            // the bridge sits in its own package: only a public member below can be called from there
            return null;
        }
        String owner = CARRIED + "bridges/" + lower.cls();
        String job = upper.kind() + "|" + upper.cls() + "|" + upper.name() + "|" + upper.desc() + "|" + lower.cls() + "|" + lower.name() + "|"
                + lower.desc();
        if (upper.kind() == 'F') {
            if (!convertible(Type.getType(upper.desc()), Type.getType(lower.desc()))) {
                return null;
            }
            String[] bridge = {owner, "get$" + upper.name()};
            schedule(rung + 1, new Stage0.Ref('B', owner, bridge[1], job));
            return bridge;
        }
        Type[] ua = Type.getArgumentTypes(upper.desc()), la = Type.getArgumentTypes(lower.desc());
        if (ua.length != la.length || !convertible(Type.getReturnType(upper.desc()), Type.getReturnType(lower.desc()))) {
            return null;
        }
        for (int a = 0; a < ua.length; a++) {
            if (!convertible(ua[a], la[a])) {
                return null;
            }
        }
        String[] bridge = {owner, upper.name() + "$" + Integer.toHexString(upper.desc().hashCode())};
        schedule(rung + 1, new Stage0.Ref('B', owner, bridge[1], job));
        bridgeEnds.put(owner + "." + bridge[1], end);
        return bridge;
    }

    /** bridge class.name -> the bottom version's member it calls */
    static final Map<String, Stage0.Ref> bridgeEnds = new HashMap<>();


    /**
     * A constructor of the upper version whose parameter types changed below (new MobEffectInstance(Holder, int) ->
     * (MobEffect, int)): the lower constructor of the same arity whose parameters all convert, through a factory bridge.
     */
    static String[] constructorBridge(int rung, Stage0.Ref upper) {
        if (adapterMethods.get(CONVERSIONS) == null) {
            return null;
        }
        Stage0.Version from = versions.get(rung), to = versions.get(rung + 1);
        String lowerOwner = Stage0.mapClass(from, to, upper.cls());
        Stage0.Cls info = lowerOwner == null ? null : to.classes.get(lowerOwner);
        if (info == null) {
            return null;
        }
        Type[] ua = Type.getArgumentTypes(upper.desc());
        String match = null;
        for (String m : info.methods) {
            if (!m.startsWith("<init> ")) {
                continue;
            }
            String desc = m.substring(7);
            Type[] la = Type.getArgumentTypes(desc);
            if (la.length != ua.length) {
                continue;
            }
            boolean ok = true;
            for (int a = 0; a < la.length && ok; a++) {
                ok = convertible(ua[a], la[a]) || ua[a].getSort() == Type.OBJECT && la[a].getSort() == Type.OBJECT
                        && lowerOwnerName(from, to, ua[a].getInternalName()).equals(la[a].getInternalName());
            }
            if (ok) {
                if (match != null) {
                    return null;
                }
                match = desc;
            }
        }
        if (match == null) {
            return null;
        }
        Stage0.Ref end = quietWalk(rung + 1, new Stage0.Ref('M', lowerOwner, "<init>", match));
        Stage0.Cls bottomOwner = end == null ? null : versions.get(versions.size() - 1).classes.get(end.cls());
        Integer access = bottomOwner == null ? null : bottomOwner.access.get("M <init> " + end.desc());
        if (access == null || (access & Opcodes.ACC_PUBLIC) == 0) {
            return null;
        }
        String owner = CARRIED + "bridges/" + lowerOwner;
        String[] bridge = {owner, "new$" + Integer.toHexString(upper.desc().hashCode())};
        schedule(rung + 1, new Stage0.Ref('B', owner, bridge[1], "N|" + upper.cls() + "|<init>|" + upper.desc() + "|" + lowerOwner + "|<init>|" + match));
        return bridge;
    }

    static String lowerOwnerName(Stage0.Version from, Stage0.Version to, String cls) {
        String mapped = Stage0.platform(cls) ? Stage0.mapClass(from, to, cls) : cls;
        return mapped == null ? cls : mapped;
    }

    /**
     * new X(upper args) of a bridged constructor: the NEW and DUP go, the constructor call becomes the factory bridge
     * returning the object. A super(...) call of a subclass can't be bridged so.
     */
    static void bridgeConstructors(MethodNode m) {
        List<MethodInsnNode> calls = new ArrayList<>();
        for (AbstractInsnNode insn : m.instructions) {
            if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESPECIAL && call.name.equals("<init>") && Stage0.platform(call.owner)) {
                calls.add(call);
            }
        }
        for (MethodInsnNode call : calls) {
            Stage0.Ref ref = resolve(call.owner, "<init>", call.desc, true);
            if (ref == null) {
                continue;
            }
            walk(ref);
            String[] bridge = callRedirect.get(level + " " + ref.key());
            if (bridge == null) {
                continue;
            }
            int depth = 0;
            AbstractInsnNode made = null;
            for (AbstractInsnNode back = call.getPrevious(); back != null; back = back.getPrevious()) {
                if (back instanceof MethodInsnNode other && other.getOpcode() == Opcodes.INVOKESPECIAL && other.name.equals("<init>")
                        && other.owner.equals(call.owner)) {
                    depth++;
                } else if (back instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW && type.desc.equals(call.owner)) {
                    if (depth == 0) {
                        made = back;
                        break;
                    }
                    depth--;
                }
            }
            AbstractInsnNode dup = made == null ? null : made.getNext();
            if (made == null || dup == null || dup.getOpcode() != Opcodes.DUP) {
                problem("a super(...) or unusual new of " + ref.key() + ", whose parameters changed below: no bridge for it");
                continue;
            }
            dropUninitialized(m, made);
            m.instructions.remove(made);
            m.instructions.remove(dup);
            m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, bridge[0], bridge[1],
                    call.desc.substring(0, call.desc.indexOf(')') + 1) + "L" + call.owner + ";", false));
        }
    }

    /**
     * A NEW that goes: the stack map frames between it and its constructor call hold its two uninitialized values (the
     * NEW's and the DUP's), named by the label at the NEW. They go from the frames too (the class is written with the
     * frames it has), or the method fails verification.
     */
    static void dropUninitialized(MethodNode m, AbstractInsnNode made) {
        Set<LabelNode> labels = Collections.newSetFromMap(new IdentityHashMap<>());
        for (AbstractInsnNode back = made.getPrevious(); back != null && back.getOpcode() < 0; back = back.getPrevious()) {
            if (back instanceof LabelNode label) {
                labels.add(label);
            }
        }
        if (labels.isEmpty()) {
            return;
        }
        for (AbstractInsnNode insn : m.instructions) {
            if (!(insn instanceof FrameNode frame)) {
                continue;
            }
            if (frame.stack != null) {
                frame.stack.removeIf(labels::contains);
                if (frame.type == Opcodes.F_SAME1 && frame.stack.isEmpty()) {
                    frame.type = Opcodes.F_SAME;
                }
            }
            if (frame.local != null && frame.local.stream().anyMatch(labels::contains)) {
                problem("a constructor bridge whose new object sits in a local before its constructor runs");
            }
        }
    }

    /** A call of a bridged member: to its bridge, the receiver first. */
    static boolean bridgeCall(MethodInsnNode call) {
        if (call.owner.startsWith(CARRIED) || call.owner.startsWith("[") || !(Stage0.platform(call.owner) || mod.containsKey(call.owner))) {
            return false;
        }
        Stage0.Ref ref = resolve(call.owner, call.name, call.desc, true);
        if (ref == null) {
            return false;
        }
        walk(ref);
        String[] bridge = callRedirect.get(level + " " + ref.key());
        if (bridge == null) {
            return false;
        }
        boolean isStatic = call.getOpcode() == Opcodes.INVOKESTATIC;
        call.desc = isStatic ? call.desc : "(L" + ref.cls() + ";" + call.desc.substring(1);
        call.setOpcode(Opcodes.INVOKESTATIC);
        call.owner = bridge[0];
        call.name = bridge[1];
        call.itf = false;
        return true;
    }

    /**
     * A platform type that is an interface in one version and a class in another (DFU 8's DataResult is an interface,
     * DFU 6's a class): the calls and method handles take the bottom version's kind, or the JVM refuses them
     * (IncompatibleClassChangeError).
     */
    static void fixInterfaceCalls(ClassNode node) {
        Stage0.Version bottom = versions.get(versions.size() - 1);
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode insn : m.instructions) {
                if (insn instanceof MethodInsnNode call && Stage0.platform(call.owner)) {
                    Stage0.Cls info = bottom.classes.get(call.owner);
                    if (info == null) {
                        continue;
                    }
                    boolean itf = (info.classAccess & Opcodes.ACC_INTERFACE) != 0;
                    if (call.itf == itf) {
                        continue;
                    }
                    call.itf = itf;
                    if (call.getOpcode() == Opcodes.INVOKEINTERFACE && !itf) {
                        call.setOpcode(Opcodes.INVOKEVIRTUAL);
                    } else if (call.getOpcode() == Opcodes.INVOKEVIRTUAL && itf) {
                        call.setOpcode(Opcodes.INVOKEINTERFACE);
                    }
                } else if (insn instanceof InvokeDynamicInsnNode indy) {
                    for (int i = 0; i < indy.bsmArgs.length; i++) {
                        if (indy.bsmArgs[i] instanceof Handle h && h.getTag() >= Opcodes.H_INVOKEVIRTUAL && Stage0.platform(h.getOwner())) {
                            Stage0.Cls info = bottom.classes.get(h.getOwner());
                            if (info == null) {
                                continue;
                            }
                            boolean itf = (info.classAccess & Opcodes.ACC_INTERFACE) != 0;
                            if (h.isInterface() == itf) {
                                continue;
                            }
                            int tag = h.getTag();
                            if (tag == Opcodes.H_INVOKEINTERFACE && !itf) {
                                tag = Opcodes.H_INVOKEVIRTUAL;
                            } else if (tag == Opcodes.H_INVOKEVIRTUAL && itf) {
                                tag = Opcodes.H_INVOKEINTERFACE;
                            }
                            indy.bsmArgs[i] = new Handle(tag, h.getOwner(), h.getName(), h.getDesc(), itf);
                        }
                    }
                }
            }
        }
    }

    /** Whether a mod class at or above owner (below the platform) declares that method ("name desc"). */
    static boolean modDeclares(String owner, String method) {
        Deque<String> todo = new ArrayDeque<>(List.of(owner));
        Set<String> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            Stage0.ModClass mc = mod.get(todo.poll());
            if (mc == null) {
                continue;
            }
            if (mc.methods.contains(method)) {
                return true;
            }
            if (mc.superName != null && seen.add(mc.superName)) {
                todo.add(mc.superName);
            }
        }
        return false;
    }

    /** class -> methods to add once it is remapped (written in the bottom version's names) */
    static final Map<String, List<MethodNode>> addAfterRemap = new HashMap<>();

    /**
     * A super call of a bridged method: a private helper of the class (added after remapping, in the bottom version's
     * names) converts the values down, calls the superclass's lower method with invokespecial, converts the result up.
     */
    static MethodNode superHelper(ClassNode node, MethodInsnNode call, String[] bridge) {
        Stage0.Ref end = bridgeEnds.get(bridge[0] + "." + bridge[1]);
        if (end == null) {
            return null;
        }
        String desc = remapper.mapMethodDesc(call.desc);
        Type[] up = Type.getArgumentTypes(desc), low = Type.getArgumentTypes(end.desc());
        Type upRet = Type.getReturnType(desc), lowRet = Type.getReturnType(end.desc());
        if (up.length != low.length || !convertible(upRet, lowRet)) {
            return null;
        }
        for (int a = 0; a < up.length; a++) {
            if (!convertible(up[a], low[a])) {
                return null;
            }
        }
        String name = "ladder$super$" + call.name + "$" + Integer.toHexString((call.owner + call.desc).hashCode());
        List<MethodNode> pending = addAfterRemap.computeIfAbsent(node.name, k -> new ArrayList<>());
        for (MethodNode done : pending) {
            if (done.name.equals(name)) {
                return done;
            }
        }
        MethodNode g = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC, name, desc, null, null);
        InsnList code = g.instructions;
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int slot = 1;
        for (int a = 0; a < up.length; a++) {
            code.add(new VarInsnNode(up[a].getOpcode(Opcodes.ILOAD), slot));
            slot += up[a].getSize();
            convertDown(code, up[a], low[a]);
        }
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, remapper.map(call.owner), end.name(), end.desc(), false));
        convertUp(code, lowRet, upRet);
        code.add(new InsnNode(upRet.getOpcode(Opcodes.IRETURN)));
        g.maxLocals = slot;
        g.maxStack = slot + 2;
        pending.add(g);
        return g;
    }

    /** The bridge of a platform member (or of a mod class's inherited one), or null when it has none. */
    static String[] bridgeOf(String owner, String name, String desc, boolean method) {
        if (owner.startsWith(CARRIED) || owner.startsWith("[") || !(Stage0.platform(owner) || mod.containsKey(owner))) {
            return null;
        }
        Stage0.Ref ref = resolve(owner, name, desc, method);
        if (ref == null) {
            return null;
        }
        walk(ref);
        String[] bridge = callRedirect.get(level + " " + ref.key());
        return bridge == null ? null : new String[] {bridge[0], bridge[1], ref.cls()};
    }

    /**
     * Method references (lambda handles) of bridged members: to the bridge, a static method taking the receiver first
     * (the lambda's captured or first argument), or the factory of a bridged constructor.
     */
    static void bridgeHandles(InvokeDynamicInsnNode indy) {
        for (int i = 0; i < indy.bsmArgs.length; i++) {
            if (!(indy.bsmArgs[i] instanceof Handle h)) {
                continue;
            }
            int tag = h.getTag();
            boolean field = tag <= Opcodes.H_PUTSTATIC;
            String[] bridge = tag == Opcodes.H_NEWINVOKESPECIAL ? bridgeOf(h.getOwner(), "<init>", h.getDesc(), true)
                    : bridgeOf(h.getOwner(), h.getName(), h.getDesc(), !field);
            if (bridge == null) {
                continue;
            }
            String desc;
            switch (tag) {
                case Opcodes.H_INVOKEVIRTUAL, Opcodes.H_INVOKEINTERFACE -> desc = "(L" + bridge[2] + ";" + h.getDesc().substring(1);
                case Opcodes.H_INVOKESTATIC -> desc = h.getDesc();
                case Opcodes.H_NEWINVOKESPECIAL -> desc = h.getDesc().substring(0, h.getDesc().indexOf(')') + 1) + "L" + h.getOwner() + ";";
                case Opcodes.H_GETFIELD -> desc = "(L" + bridge[2] + ";)" + h.getDesc();
                case Opcodes.H_GETSTATIC -> desc = "()" + h.getDesc();
                default -> {
                    problem("a method handle (" + tag + ") of " + h.getOwner() + "." + h.getName() + h.getDesc() + ", whose types changed below: no bridge for it");
                    continue;
                }
            }
            indy.bsmArgs[i] = new Handle(Opcodes.H_INVOKESTATIC, bridge[0], bridge[1], desc, false);
            // a lambda's captured values must have the implementation's parameter types exactly (the bound receiver
            // becomes the bridge's first parameter, its declaring class): the call site takes them so
            Type[] captured = Type.getArgumentTypes(indy.desc), params = Type.getArgumentTypes(desc);
            if (captured.length > 0 && captured.length <= params.length) {
                System.arraycopy(params, 0, captured, 0, captured.length);
                indy.desc = Type.getMethodDescriptor(Type.getReturnType(indy.desc), captured);
            }
        }
    }

    /**
     * A mod method overriding a platform method whose types changed below: the game calls the lower method, and the
     * bridges of the mod's own calls call it too. The mod class gets the lower method (an override bridge): it converts
     * the values up, calls the mod's method, converts its result down. In the bottom version's names: added after the
     * class is remapped. When the values don't convert (or the class is a mixin), it is a problem: the override would
     * never run. An O row of the adapters covers its own methods.
     */
    static List<MethodNode> overrideBridges(ClassNode node) {
        List<MethodNode> made = new ArrayList<>();
        boolean itf = (node.access & Opcodes.ACC_INTERFACE) != 0;
        for (MethodNode m : node.methods) {
            if (m.name.startsWith("<") || (m.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_ABSTRACT)) != 0) {
                continue;
            }
            Stage0.Ref ref = resolve(node.name, m.name, m.desc, true);
            if (ref == null || overrideRule(ref.cls(), m.name, m.desc) != null) {
                continue;
            }
            walk(ref);
            String[] bridge = callRedirect.get(level + " " + ref.key());
            if (bridge == null) {
                continue;
            }
            Stage0.Ref end = bridgeEnds.get(bridge[0] + "." + bridge[1]);
            MethodNode g = end == null || mixinTargets.containsKey(node.name) ? null : overrideBridge(node, m, end, itf);
            if (g == null) {
                problem("an override of " + ref.key() + ", whose parameters changed below: no override bridge for it (it would never be called)");
                continue;
            }
            made.add(g);
        }
        return made;
    }

    /** The lower method of an override bridge, in the bottom version's names; null when the values don't convert. */
    static MethodNode overrideBridge(ClassNode node, MethodNode m, Stage0.Ref end, boolean itf) {
        String ownName = remapper.mapMethodName(node.name, m.name, m.desc), ownDesc = remapper.mapMethodDesc(m.desc);
        Type[] up = Type.getArgumentTypes(ownDesc), low = Type.getArgumentTypes(end.desc());
        Type upRet = Type.getReturnType(ownDesc), lowRet = Type.getReturnType(end.desc());
        if (up.length != low.length || !convertible(upRet, lowRet)) {
            return null;
        }
        for (int a = 0; a < up.length; a++) {
            if (!convertible(up[a], low[a])) {
                return null;
            }
        }
        MethodNode g = new MethodNode(Opcodes.ACC_PUBLIC, end.name(), end.desc(), null, null);
        InsnList code = g.instructions;
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int slot = 1;
        for (int a = 0; a < low.length; a++) {
            code.add(new VarInsnNode(low[a].getOpcode(Opcodes.ILOAD), slot));
            slot += low[a].getSize();
            convertUp(code, low[a], up[a]);
        }
        code.add(new MethodInsnNode(itf ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, remapper.map(node.name), ownName, ownDesc, itf));
        convertDown(code, upRet, lowRet);
        code.add(new InsnNode(lowRet.getOpcode(Opcodes.IRETURN)));
        g.maxLocals = slot;
        g.maxStack = slot + 2;
        return g;
    }

    /** A read of a bridged field: to its getter bridge; a write can't be bridged. */
    static boolean bridgeField(MethodNode in, FieldInsnNode field) {
        if (field.owner.startsWith(CARRIED) || !(Stage0.platform(field.owner) || mod.containsKey(field.owner))) {
            return false;
        }
        Stage0.Ref ref = resolve(field.owner, field.name, field.desc, false);
        if (ref == null) {
            return false;
        }
        walk(ref);
        String[] bridge = callRedirect.get(level + " " + ref.key());
        if (bridge == null) {
            return false;
        }
        int op = field.getOpcode();
        if (op == Opcodes.PUTSTATIC || op == Opcodes.PUTFIELD) {
            problem("a write to " + ref.key() + ", whose type changed below: no bridge for writes");
            return false;
        }
        String desc = op == Opcodes.GETSTATIC ? "()" + field.desc : "(L" + ref.cls() + ";)" + field.desc;
        in.instructions.set(field, new MethodInsnNode(Opcodes.INVOKESTATIC, bridge[0], bridge[1], desc, false));
        return true;
    }

    /** The code of a bridge, as code of the lower version: converts the values and calls (or reads) the lower member. */
    static MethodNode bridgeCode(Stage0.Ref job) {
        String[] d = job.desc().split("[|]");
        char kind = d[0].charAt(0);
        String upperDesc = d[3], lowerOwner = d[4], lowerName = d[5], lowerDesc = d[6];
        Stage0.Cls info = versions.get(level).classes.get(lowerOwner);
        boolean lowerStatic = kind == 'F' ? info.staticFields.contains(lowerName + " " + lowerDesc) : info.staticMethods.contains(lowerName + " " + lowerDesc);
        boolean lowerInterface = (info.classAccess & Opcodes.ACC_INTERFACE) != 0;
        InsnList code = new InsnList();
        String desc;
        int locals = 0;
        int stack = 2;
        if (kind == 'N') {
            // a factory: new lower(converted args)
            Type[] ua = Type.getArgumentTypes(upperDesc), la = Type.getArgumentTypes(lowerDesc);
            StringBuilder b = new StringBuilder("(");
            code.add(new TypeInsnNode(Opcodes.NEW, lowerOwner));
            code.add(new InsnNode(Opcodes.DUP));
            for (int a = 0; a < ua.length; a++) {
                b.append(ua[a].getDescriptor());
                code.add(new VarInsnNode(ua[a].getOpcode(Opcodes.ILOAD), locals));
                locals += ua[a].getSize();
                stack += ua[a].getSize();
                convertDown(code, ua[a], la[a]);
            }
            desc = b.append(")L").append(lowerOwner).append(";").toString();
            code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, lowerOwner, "<init>", lowerDesc, false));
            code.add(new InsnNode(Opcodes.ARETURN));
        } else if (kind == 'F') {
            desc = lowerStatic ? "()" + upperDesc : "(L" + lowerOwner + ";)" + upperDesc;
            if (!lowerStatic) {
                code.add(new VarInsnNode(Opcodes.ALOAD, 0));
                locals = 1;
            }
            code.add(new FieldInsnNode(lowerStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD, lowerOwner, lowerName, lowerDesc));
            convertUp(code, Type.getType(lowerDesc), Type.getType(upperDesc));
            code.add(new InsnNode(Type.getType(upperDesc).getOpcode(Opcodes.IRETURN)));
        } else {
            Type[] ua = Type.getArgumentTypes(upperDesc), la = Type.getArgumentTypes(lowerDesc);
            StringBuilder b = new StringBuilder("(");
            if (!lowerStatic) {
                b.append("L").append(lowerOwner).append(";");
                code.add(new VarInsnNode(Opcodes.ALOAD, 0));
                locals = 1;
            }
            for (int a = 0; a < ua.length; a++) {
                b.append(ua[a].getDescriptor());
                code.add(new VarInsnNode(ua[a].getOpcode(Opcodes.ILOAD), locals));
                locals += ua[a].getSize();
                stack += ua[a].getSize();
                convertDown(code, ua[a], la[a]);
            }
            Type upperReturn = Type.getReturnType(upperDesc), lowerReturn = Type.getReturnType(lowerDesc);
            desc = b.append(")").append(upperReturn.getDescriptor()).toString();
            code.add(new MethodInsnNode(lowerStatic ? Opcodes.INVOKESTATIC : lowerInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL,
                    lowerOwner, lowerName, lowerDesc, lowerInterface));
            convertUp(code, lowerReturn, upperReturn);
            code.add(new InsnNode(upperReturn.getOpcode(Opcodes.IRETURN)));
        }
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, job.name(), desc, null, null);
        method.instructions = code;
        method.maxLocals = Math.max(locals, 1);
        method.maxStack = stack + 2;
        return method;
    }

    /** A value of the lower type on the stack becomes the upper type's. */
    static void convertUp(InsnList code, Type lower, Type upper) {
        switch (conversion(upper, lower)) {
            case 1 -> code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "holder", "(Ljava/lang/Object;)L" + HOLDER + ";", false));
            case 2 -> code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "reference", "(Ljava/lang/Object;)L" + REFERENCE + ";", false));
            case 3 -> code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "mapCodec", "(L" + CODEC + ";)L" + MAP_CODEC + ";", false));
            case 5 -> {
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "orNull", "(L" + OPTIONAL + ";)Ljava/lang/Object;", false));
                code.add(new TypeInsnNode(Opcodes.CHECKCAST, upper.getInternalName()));
            }
            case 6 -> code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "keyOfValue", "(Ljava/lang/Object;)L" + RESOURCE_KEY + ";", false));
            case 7 -> code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "lootTableKey", "(L" + RESOURCE_LOCATION + ";)L" + RESOURCE_KEY + ";", false));
            case 9 -> code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "deltaTracker", "(F)" + upper.getDescriptor(), false));
            default -> {
                // the same type, or a subtype (RegistryAccess is a HolderLookup.Provider)
            }
        }
    }

    /** Converts the scheduled carried code, each piece from its own rung down. */
    static void drainCarry() {
        int savedLevel = level;
        boolean savedCarried = inCarried;
        Map<String, Stage0.ModClass> savedMod = mod;
        Map<String, List<String>> savedMixins = mixinTargets;
        mod = new HashMap<>();
        mixinTargets = new HashMap<>();
        inCarried = true;
        try {
            while (!carryQueue.isEmpty()) {
                Object[] job = carryQueue.poll();
                level = (Integer) job[0];
                Stage0.Ref thing = (Stage0.Ref) job[1];
                if (thing.kind() == 'B') {
                    // a generated bridge: lower-version code, converted down from its rung like carried code
                    ClassNode holder = new ClassNode();
                    holder.version = Opcodes.V17;
                    holder.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER | Opcodes.ACC_FINAL;
                    holder.name = thing.cls();
                    holder.superName = "java/lang/Object";
                    holder.methods.add(bridgeCode(thing));
                    redirectCarried(holder);
                    ClassNode converted = new ClassNode();
                    holder.accept(new ClassRemapper(converted, remapper));
                fixInterfaceCalls(converted);
                    fixInterfaceCalls(converted);
                    ClassNode companion = carriedOut.computeIfAbsent(converted.name, n -> {
                        ClassNode c = new ClassNode();
                        c.version = converted.version;
                        c.access = converted.access;
                        c.name = converted.name;
                        c.superName = converted.superName;
                        return c;
                    });
                    for (MethodNode mm : converted.methods) {
                        if (companion.methods.stream().noneMatch(x -> x.name.equals(mm.name) && x.desc.equals(mm.desc))) {
                            companion.methods.add(mm);
                        }
                    }
                    continue;
                }
                byte[] bytes = versions.get(level).bytes.get(thing.cls());
                if (bytes == null) {
                    problem("no code for carried " + thing.key() + " in " + versions.get(level).id);
                    continue;
                }
                ClassNode source = new ClassNode();
                new ClassReader(bytes).accept(source, 0);
                if (thing.kind() == 'C') {
                    redirectCarried(source);
                    ClassNode converted = new ClassNode();
                    source.accept(new ClassRemapper(converted, remapper));
                    fixInterfaceCalls(converted);
                    if (!converted.name.startsWith(CARRIED)) {
                        // a carried class outside its own package tree would split a game package (JPMS refuses it)
                        problem("carried class " + thing.cls() + " would be written as " + converted.name);
                        continue;
                    }
                    carriedOut.put(converted.name, converted);
                    continue;
                }
                MethodNode method = source.methods.stream().filter(mm -> mm.name.equals(thing.name()) && mm.desc.equals(thing.desc()))
                        .findFirst().orElse(null);
                if (method == null) {
                    problem("no code for carried " + thing.key());
                    continue;
                }
                ClassNode holder = new ClassNode();
                holder.version = source.version;
                holder.access = Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER | Opcodes.ACC_FINAL;
                holder.name = CARRIED + thing.cls();
                holder.superName = "java/lang/Object";
                method.access = (method.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
                holder.methods.add(method);
                // its private static helpers come along, and its calls to them stay in the companion
                Set<String> copied = helpers.getOrDefault(level + " " + thing.key(), Set.of());
                for (MethodNode helper : source.methods) {
                    if (copied.contains(helper.name + " " + helper.desc)) {
                        helper.access = (helper.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
                        holder.methods.add(helper);
                    }
                }
                for (MethodNode m : holder.methods) {
                    for (AbstractInsnNode insn : m.instructions) {
                        if (insn instanceof MethodInsnNode call && call.owner.equals(thing.cls()) && copied.contains(call.name + " " + call.desc)) {
                            call.owner = holder.name;
                            call.itf = false;
                        }
                    }
                }
                redirectCarried(holder);
                ClassNode converted = new ClassNode();
                holder.accept(new ClassRemapper(converted, remapper));
                ClassNode companion = carriedOut.computeIfAbsent(converted.name, n -> {
                    ClassNode c = new ClassNode();
                    c.version = converted.version;
                    c.access = converted.access;
                    c.name = converted.name;
                    c.superName = converted.superName;
                    return c;
                });
                for (MethodNode mm : converted.methods) {
                    if (companion.methods.stream().noneMatch(x -> x.name.equals(mm.name) && x.desc.equals(mm.desc))) {
                        companion.methods.add(mm);
                    }
                }
            }
        } finally {
            level = savedLevel;
            inCarried = savedCarried;
            mod = savedMod;
            mixinTargets = savedMixins;
        }
    }

    /** The target's carried code as one game library jar, loaded beside the game for every converted mod. */
    static void writeCarried(Path jar) throws IOException {
        if (carriedOut.isEmpty()) {
            return;
        }
        // one library per target for every mod: a conversion adds to the one that is there (two jars with the same
        // packages wouldn't load), its own classes replacing the older copies
        Map<String, byte[]> kept = new TreeMap<>();
        if (Files.exists(jar)) {
            try (ZipFile old = new ZipFile(jar.toFile())) {
                for (ZipEntry e : Collections.list(old.entries())) {
                    String name = e.getName();
                    if (!name.endsWith(".class")) {
                        continue;
                    }
                    byte[] bytes;
                    try (InputStream s = old.getInputStream(e)) {
                        bytes = s.readAllBytes();
                    }
                    ClassNode now = carriedOut.get(name.substring(0, name.length() - 6));
                    if (now == null) {
                        kept.put(name, bytes);
                        continue;
                    }
                    // a companion class both have: the other mods' carried methods stay in it
                    ClassNode before = new ClassNode();
                    new ClassReader(bytes).accept(before, 0);
                    for (MethodNode m : before.methods) {
                        if (now.methods.stream().noneMatch(x -> x.name.equals(m.name) && x.desc.equals(m.desc))) {
                            now.methods.add(m);
                        }
                    }
                    for (FieldNode f : before.fields) {
                        if (now.fields.stream().noneMatch(x -> x.name.equals(f.name))) {
                            now.fields.add(f);
                        }
                    }
                }
            }
        }
        java.util.jar.Manifest manifest = new java.util.jar.Manifest();
        manifest.getMainAttributes().put(java.util.jar.Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("FMLModType", "GAMELIBRARY");
        manifest.getMainAttributes().putValue("Automatic-Module-Name", "ladder.carried");
        try (java.util.jar.JarOutputStream zip = new java.util.jar.JarOutputStream(Files.newOutputStream(jar), manifest)) {
            for (ClassNode node : carriedOut.values()) {
                ClassWriter writer = new ClassWriter(0);
                node.accept(writer);
                zip.putNextEntry(new ZipEntry(node.name + ".class"));
                zip.write(writer.toByteArray());
                zip.closeEntry();
            }
            for (var e : kept.entrySet()) {
                zip.putNextEntry(new ZipEntry(e.getKey()));
                zip.write(e.getValue());
                zip.closeEntry();
            }
        }
    }

    // ---------------------------------------------------------------- mixins

    static List<String> mixinTargetsOf(ClassNode node) {
        List<String> targets = new ArrayList<>();
        for (List<AnnotationNode> list : Arrays.asList(node.visibleAnnotations, node.invisibleAnnotations)) {
            if (list == null) {
                continue;
            }
            for (AnnotationNode a : list) {
                if (!"Lorg/spongepowered/asm/mixin/Mixin;".equals(a.desc) || a.values == null) {
                    continue;
                }
                for (int i = 0; i + 1 < a.values.size(); i += 2) {
                    Object v = a.values.get(i + 1);
                    if ("value".equals(a.values.get(i)) && v instanceof List<?> types) {
                        for (Object t : types) {
                            if (t instanceof Type type) {
                                targets.add(type.getInternalName());
                            }
                        }
                    } else if ("targets".equals(a.values.get(i)) && v instanceof List<?> names) {
                        for (Object n : names) {
                            targets.add(String.valueOf(n).replace('.', '/'));
                        }
                    }
                }
            }
        }
        return targets;
    }

    /** The string selectors of a mixin's injectors (method = ..., @At(target = ...)) in the bottom version's names. */
    static void remapMixinSelectors(ClassNode node, Remapper remapper) {
        List<String> targets = mixinTargets.get(node.name);
        for (MethodNode m : node.methods) {
            for (List<AnnotationNode> list : Arrays.asList(m.visibleAnnotations, m.invisibleAnnotations)) {
                if (list != null) {
                    for (AnnotationNode a : list) {
                        remapAnnotation(a, targets, remapper, node.name + "." + m.name);
                    }
                }
            }
        }
    }

    static void remapAnnotation(AnnotationNode a, List<String> targets, Remapper remapper, String where) {
        if (a.values == null || !(a.desc.startsWith("Lorg/spongepowered/asm/mixin/") || a.desc.startsWith("Lcom/llamalad7/mixinextras/"))) {
            return;
        }
        boolean at = a.desc.equals("Lorg/spongepowered/asm/mixin/injection/At;");
        for (int i = 0; i + 1 < a.values.size(); i += 2) {
            String key = (String) a.values.get(i);
            Object value = a.values.get(i + 1);
            if (key.equals("method") && value instanceof List<?> selectors) {
                List<Object> mapped = new ArrayList<>();
                for (Object s : selectors) {
                    mapped.add(s instanceof String sel ? methodSelector(sel, targets, remapper, where) : s);
                }
                a.values.set(i + 1, mapped);
            } else if (at && key.equals("target") && value instanceof String target) {
                a.values.set(i + 1, targetSelector(target, remapper, where));
            } else if (value instanceof AnnotationNode inner) {
                remapAnnotation(inner, targets, remapper, where);
            } else if (value instanceof List<?> list) {
                for (Object o : list) {
                    if (o instanceof AnnotationNode inner) {
                        remapAnnotation(inner, targets, remapper, where);
                    }
                }
            }
        }
    }

    /** "name", "name(desc)ret" or "Lowner;name(desc)ret" on the mixin's targets. */
    static String methodSelector(String selector, List<String> targets, Remapper remapper, String where) {
        if (selector.contains("*") || selector.startsWith("^") || selector.contains("<clinit>")) {
            unverified.add("mixin selector pattern " + selector + " in " + where);
            return selector;
        }
        String owner = null;
        String rest = selector;
        if (rest.startsWith("L") && rest.contains(";")) {
            owner = rest.substring(1, rest.indexOf(';'));
            rest = rest.substring(rest.indexOf(';') + 1);
        }
        String name = rest.contains("(") ? rest.substring(0, rest.indexOf('(')) : rest;
        String desc = rest.contains("(") ? rest.substring(rest.indexOf('(')) : null;
        List<String> owners = owner != null ? List.of(owner) : targets;
        Stage0.Version top = versions.get(0);
        Set<String> results = new TreeSet<>();
        for (String o : owners) {
            if (!Stage0.platform(o)) {
                continue;
            }
            Stage0.Cls info = top.classes.get(o);
            if (info == null) {
                continue;
            }
            for (String m : info.methods) {
                String mName = m.substring(0, m.indexOf(' '));
                String mDesc = m.substring(m.indexOf(' ') + 1);
                if (!mName.equals(name) || desc != null && !mDesc.equals(desc)) {
                    continue;
                }
                if (name.equals("<init>")) {
                    validateConstructor(o, mDesc);
                    results.add("<init>" + remapper.mapMethodDesc(mDesc));
                    continue;
                }
                String mapped = mapMember(o, mName, mDesc, true);
                results.add(mapped + (desc != null ? remapper.mapMethodDesc(mDesc) : ""));
            }
        }
        if (results.isEmpty()) {
            if (owners.stream().anyMatch(Stage0::platform)) {
                problem("mixin selector " + selector + " matches nothing in " + top.id + " (" + where + ")");
            }
            return selector;
        }
        if (results.size() > 1 && desc == null) {
            Set<String> names = new TreeSet<>();
            results.forEach(r -> names.add(r));
            if (names.size() > 1) {
                problem("mixin selector " + selector + " becomes several methods " + names + " (" + where + ")");
                return selector;
            }
        }
        String first = results.iterator().next();
        return (owner != null ? "L" + remapper.map(owner) + ";" : "") + first;
    }

    /** An @At target: "Lowner;name(desc)ret" (a call), "Lowner;name:desc" (a field), "Lowner;<init>(desc)V". */
    static String targetSelector(String target, Remapper remapper, String where) {
        if (!target.startsWith("L") || !target.contains(";")) {
            return target;
        }
        String owner = target.substring(1, target.indexOf(';'));
        String rest = target.substring(target.indexOf(';') + 1);
        if (!Stage0.platform(owner)) {
            return "L" + remapper.map(owner) + ";" + rest;
        }
        if (rest.isEmpty()) {
            return "L" + remapper.map(owner) + ";";
        }
        boolean field = rest.contains(":");
        String name = field ? rest.substring(0, rest.indexOf(':')) : rest.contains("(") ? rest.substring(0, rest.indexOf('(')) : rest;
        String desc = field ? rest.substring(rest.indexOf(':') + 1) : rest.contains("(") ? rest.substring(rest.indexOf('(')) : null;
        if (desc == null) {
            unverified.add("mixin target without descriptor " + target + " in " + where);
            return "L" + remapper.map(owner) + ";" + rest;
        }
        String newName;
        if (name.equals("<init>")) {
            validateConstructor(owner, desc);
            newName = name;
        } else {
            newName = finalName(owner, name, desc, !field);
            if (newName == null) {
                problem("mixin target " + target + " matches nothing in " + versions.get(0).id + " (" + where + ")");
                return target;
            }
        }
        String newDesc = field ? remapper.mapDesc(desc) : remapper.mapMethodDesc(desc);
        return "L" + remapper.map(owner) + ";" + newName + (field ? ":" : "") + newDesc;
    }

    // ---------------------------------------------------------------- data

    /** 1.21's singular data folders -> 1.20.4's plural ones; null for data 1.20.4 has no place for. */
    static String dataPath(String path) {
        String[][] folders = {{"recipe", "recipes"}, {"loot_table", "loot_tables"}, {"advancement", "advancements"},
                {"structure", "structures"}, {"item_modifier", "item_modifiers"}, {"predicate", "predicates"}, {"function", "functions"},
                {"tags/block", "tags/blocks"}, {"tags/item", "tags/items"}, {"tags/entity_type", "tags/entity_types"},
                {"tags/fluid", "tags/fluids"}, {"tags/game_event", "tags/game_events"}, {"tags/function", "tags/functions"}};
        String[] parts = path.split("/", 3);
        if (parts.length < 3) {
            return path;
        }
        String rest = parts[2];
        for (String[] f : folders) {
            if (rest.startsWith(f[0] + "/")) {
                return parts[0] + "/" + parts[1] + "/" + f[1] + rest.substring(f[0].length());
            }
        }
        // registries 1.20.4 fills from code or doesn't have (trim materials and patterns and damage types it reads: kept)
        for (String gone : new String[] {"enchantment/", "enchantment_provider/", "jukebox_song/", "painting_variant/", "banner_pattern/",
                "wolf_variant/"}) {
            if (rest.startsWith(gone)) {
                return null;
            }
        }
        return path;
    }

    static final Set<String> SHAPED_RESULT = Set.of("minecraft:crafting_shaped", "minecraft:crafting_shapeless", "minecraft:smithing_transform");
    static final Set<String> COOKING = Set.of("minecraft:smelting", "minecraft:blasting", "minecraft:smoking", "minecraft:campfire_cooking");

    /** A data file in 1.20.4's form: vanilla recipe results by their serializer, advancement icons; components can't come down. */
    static String dataJson(String path, String json) {
        if (!path.contains("/recipes/") && !path.contains("/advancements/") && !path.contains("/loot_tables/")) {
            return json;
        }
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        } catch (JsonParseException e) {
            problem("data file that isn't JSON: " + path);
            return json;
        }
        if (!root.isJsonObject()) {
            return json;
        }
        JsonObject object = root.getAsJsonObject();
        if (path.contains("/loot_tables/")) {
            if (json.contains("set_components") || json.contains("copy_components")) {
                problem("data components in a loot table (no 1.20.4 form): " + path);
            }
            itemPredicates(object, path);
            return object.toString();
        }
        if (path.contains("/advancements/")) {
            JsonObject display = object.has("display") && object.get("display").isJsonObject() ? object.getAsJsonObject("display") : null;
            if (display != null && display.has("icon") && display.get("icon").isJsonObject()) {
                JsonObject icon = display.getAsJsonObject("icon");
                if (icon.has("id")) {
                    icon.add("item", icon.remove("id"));
                }
                icon.remove("components");
            }
            unverified.add("advancement criteria " + path);
            return object.toString();
        }
        String type = object.has("type") ? object.get("type").getAsString() : "";
        JsonElement result = object.get("result");
        if (result != null && result.isJsonObject() && result.getAsJsonObject().has("components")) {
            problem("data components in a recipe result (no 1.20.4 form): " + path);
        }
        if (result == null || !result.isJsonObject()) {
            if (!type.startsWith("minecraft:") && result != null) {
                unverified.add("recipe type " + type + " " + path);
            }
            return json;
        }
        JsonObject stack = result.getAsJsonObject();
        String id = stack.has("id") ? stack.get("id").getAsString() : stack.has("item") ? stack.get("item").getAsString() : null;
        int count = stack.has("count") ? stack.get("count").getAsInt() : 1;
        if (SHAPED_RESULT.contains(type)) {
            JsonObject r = new JsonObject();
            r.addProperty("item", id);
            if (count != 1) {
                r.addProperty("count", count);
            }
            object.add("result", r);
        } else if (type.equals("minecraft:stonecutting")) {
            // SingleItemRecipe: "result": "<id>", "count": n
            object.addProperty("result", id);
            object.addProperty("count", count);
        } else if (COOKING.contains(type)) {
            if (count != 1) {
                problem("cooking recipe with a count (1.20.4 makes one): " + path);
            }
            object.addProperty("result", id);
        } else {
            unverified.add("recipe type " + type + " " + path);
            return json;
        }
        return object.toString();
    }

    /** The keys of a 1.21 item predicate; anything else is ignored there (and must not count on 1.20.4). */
    static final Set<String> ITEM_PREDICATE_KEYS = Set.of("items", "count", "components", "predicates");

    /** Every match_tool predicate of a loot table, as 1.21 reads it, in 1.20.4's form. */
    static void itemPredicates(JsonElement element, String path) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(e -> itemPredicates(e, path));
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("condition") && "minecraft:match_tool".equals(object.get("condition").getAsString()) && object.has("predicate")
                && object.get("predicate").isJsonObject()) {
            object.add("predicate", itemPredicate(object.getAsJsonObject("predicate"), path));
        }
        for (var e : object.entrySet()) {
            if (!e.getKey().equals("predicate")) {
                itemPredicates(e.getValue(), path);
            }
        }
    }

    /**
     * A 1.21 item predicate in 1.20.4's form: "items" as a list or "tag"; the enchantment sub-predicates as 1.20.4's
     * lists; keys 1.21 doesn't read are left out (1.21 ignored them: a mod's old "enchantments" there matched any tool).
     */
    static JsonObject itemPredicate(JsonObject in, String path) {
        JsonObject out = new JsonObject();
        for (var e : in.entrySet()) {
            String key = e.getKey();
            JsonElement value = e.getValue();
            if (!ITEM_PREDICATE_KEYS.contains(key)) {
                unverified.add("item predicate key " + key + " that 1.21 ignores, left out: " + path);
                continue;
            }
            switch (key) {
                case "items" -> {
                    if (value.isJsonPrimitive() && value.getAsString().startsWith("#")) {
                        out.addProperty("tag", value.getAsString().substring(1));
                    } else if (value.isJsonPrimitive()) {
                        JsonArray list = new JsonArray();
                        list.add(value.getAsString());
                        out.add("items", list);
                    } else {
                        out.add("items", value);
                    }
                }
                case "count" -> out.add("count", value);
                case "components" -> problem("item predicate on data components (no 1.20.4 form): " + path);
                case "predicates" -> {
                    for (var sub : value.getAsJsonObject().entrySet()) {
                        String name = sub.getKey();
                        if (name.equals("minecraft:enchantments") || name.equals("minecraft:stored_enchantments")) {
                            JsonArray list = new JsonArray();
                            for (JsonElement entry : sub.getValue().getAsJsonArray()) {
                                JsonObject enchantment = entry.getAsJsonObject();
                                JsonObject old = new JsonObject();
                                JsonElement ids = enchantment.get("enchantments");
                                if (ids != null && ids.isJsonPrimitive() && !ids.getAsString().startsWith("#")) {
                                    old.addProperty("enchantment", ids.getAsString());
                                } else if (ids != null) {
                                    problem("enchantment predicate on a tag or list (no 1.20.4 form): " + path);
                                }
                                if (enchantment.has("levels")) {
                                    old.add("levels", enchantment.get("levels"));
                                }
                                list.add(old);
                            }
                            out.add(name.equals("minecraft:enchantments") ? "enchantments" : "stored_enchantments", list);
                        } else {
                            problem("item sub-predicate " + name + " (no 1.20.4 form yet): " + path);
                        }
                    }
                }
                default -> {
                }
            }
        }
        return out;
    }

    /** neoforge.mods.toml -> mods.toml with the bottom version's loader, NeoForge and Minecraft ranges. */
    static String modsToml(String toml, String bottom) {
        String neoforge = bottom.equals("1.20.4") ? "[20.4,)" : "[" + bottom + ",)";
        StringBuilder out = new StringBuilder();
        String dependency = null;
        for (String line : toml.split("\r?\n", -1)) {
            String t = line.trim();
            if (t.startsWith("loaderVersion")) {
                line = "loaderVersion=\"[1,)\"";
            } else if (t.startsWith("modId")) {
                dependency = t.replaceAll(".*=\\s*\"([^\"]*)\".*", "$1");
            } else if (t.startsWith("versionRange") && "neoforge".equals(dependency)) {
                line = "versionRange=\"" + neoforge + "\"";
            } else if (t.startsWith("versionRange") && "minecraft".equals(dependency)) {
                line = "versionRange=\"[" + bottom + ",)\"";
            }
            out.append(line).append('\n');
        }
        return out.toString();
    }
}
