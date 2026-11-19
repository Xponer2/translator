/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
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
    /** rung + key of the new instance methods of existing classes carried as static helpers (receiver first) */
    static final Set<String> instanceCarried = new HashSet<>();
    /** version id -> "name desc" -> the classes declaring a non-static method so (for the override check) */
    static final Map<String, Map<String, List<String>>> declarers = new HashMap<>();
    /** The carried classes of the target, shared by every mod: relocated name -> class. */
    static final Map<String, ClassNode> carriedOut = new TreeMap<>();
    static final Deque<Object[]> carryQueue = new ArrayDeque<>();
    static final Set<String> carryScheduled = new HashSet<>();
    /** walk key -> the companion class a carried static member moved to (its call sites move there too) */
    static final Map<String, String> staticRedirect = new HashMap<>();
    /** converting carried code: its calls must reach public members below */
    static boolean inCarried;
    static Remapper remapper;
    /** In a rule's lower descriptor: the bottom version's class of that name, not the top's (see the remapper) */
    static final String BOTTOM = "ladder/bottom/";

    /** NeoForge classes that moved between 21.x and 20.4 (no intermediary for NeoForge): their own rows. */
    static final Map<String, String> NEOFORGE_MOVES = new HashMap<>(Map.of(
            "net/neoforged/fml/common/EventBusSubscriber", "net/neoforged/fml/common/Mod$EventBusSubscriber",
            "net/neoforged/fml/common/EventBusSubscriber$Bus", "net/neoforged/fml/common/Mod$EventBusSubscriber$Bus"));

    /** The problems a cached class or member walk found: added again for every mod that meets it. */
    static final Map<String, List<String>> cachedProblems = new HashMap<>();
    static List<String> collecting;

    static void problem(String text) {
        report(text);
        if (collecting != null) {
            collecting.add(text);
        }
    }

    /**
     * The problems a mixin handler's selectors raise while they are remapped wait here (per mixin class and handler):
     * whether they stand is decided once the converted mixin is checked against the bottom game (after the mixin
     * repair). Null outside a handler's remapping.
     */
    static List<String> deferring;
    static final Map<String, Map<String, List<String>>> deferredMixin = new HashMap<>();

    /** A problem into the report, or into the handler's deferred list while its selectors are remapped. */
    static void report(String text) {
        if (deferring != null) {
            deferring.add(text);
        } else {
            problems.add(text);
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
        // the N rows are the bottom adapters': a class of the same name there can be another class (1.20.5's ArmorMaterial
        // record and 1.20.4's ArmorMaterial interface)
        Stage0.notTheSameAt = versions.get(versions.size() - 1).id;
        String why = System.getProperty("ladder.why") == null ? null : System.getProperty("ladder.why").replaceFirst("^([CMF])_", "$1 ").replace("_SP_", " ");
        if (why != null) {
            // what still keeps a thing from being carried at a rung: the hard needs the converter can't resolve, down its chain
            int rung = Integer.getInteger("ladder.whyRung", versions.size() - 2);
            Stage0.Plan plan = Stage0.plan(versions.get(rung), versions.get(rung + 1));
            Deque<String> todo = new ArrayDeque<>(List.of(why));
            Set<String> seen = new HashSet<>();
            Map<String, String> roots = new TreeMap<>();
            Map<String, String> parent = new HashMap<>();
            while (!todo.isEmpty()) {
                String k = todo.poll();
                Stage0.Needs n = plan.needs.get(k);
                // what the adapters provide or an X/F row covers isn't carried: its own needs don't count
                if (n == null || !seen.add(k) || provided.containsKey(k.substring(2)) || !k.equals(why) && ruled(k)) {
                    continue;
                }
                for (String h : n.hard) {
                    if (!resolvable(rung, h)) {
                        roots.putIfAbsent(h, k);
                    }
                }
                for (String d : n.deps) {
                    parent.putIfAbsent(d, k);
                    todo.add(d);
                }
            }
            System.out.println(why + ": " + seen.size() + " things in its chain; unresolved roots (and the path to them):");
            roots.forEach((r, by) -> {
                StringBuilder path = new StringBuilder(by);
                for (String up = parent.get(by); up != null && path.length() < 600; up = parent.get(up)) {
                    path.append("  <-  ").append(up);
                }
                System.out.println("  " + r + System.lineSeparator() + "      <- " + path);
            });
            // the chain's things that still aren't carried, and the first reason each (access below, a contract, a need)
            Set<String> ok = carriable(rung);
            Stage0.Version from = versions.get(rung);
            for (String k : new TreeSet<>(seen)) {
                Stage0.Ref r = plan.things.get(k);
                if (ok.contains(k) || r == null || provided.containsKey(k.substring(2)) || ruled(k)) {
                    continue;
                }
                String reason;
                if (!plan.needs.get(k).hard.stream().allMatch(h -> resolvable(rung, h))) {
                    reason = "an unresolved root";
                } else if (r.kind() == 'C' || r.kind() == 'M' && from.classes.get(r.cls()).staticMethods.contains(r.name() + " " + r.desc())) {
                    var classCode = Stage0.codeRefs(from, r.cls());
                    Set<Stage0.Ref> refs = new HashSet<>();
                    if (r.kind() == 'C') {
                        classCode.values().forEach(refs::addAll);
                    } else {
                        refs.addAll(classCode.getOrDefault(r.name() + " " + r.desc(), Set.of()));
                    }
                    Stage0.Ref bad = unreachable(rung, r, refs);
                    reason = bad != null ? "not public below: " + bad.key() : !contractsHold(rung, r) ? "a contract below breaks"
                            : "needs " + plan.needs.get(k).deps.stream().filter(d -> plan.needs.containsKey(d) && !ok.contains(d) && !ruled(d)).findFirst().orElse("?");
                } else {
                    reason = "not a class or static method (instance members come with their class or an adapter)";
                }
                System.out.println("  not carried: " + k + " -- " + reason);
            }
            return;
        }
        String measure = System.getProperty("ladder.measure");
        if (measure != null) {
            measureInstanceCarrying(Paths.get(measure));
            return;
        }
        if (Files.isDirectory(in)) {
            // batch: every jar of the folder, one report line each; the clean ones first
            Files.createDirectories(out);
            List<String> lines = new ArrayList<>();
            int clean = 0;
            List<Path> jars;
            try (var s = Files.list(in)) {
                jars = s.filter(p -> p.toString().endsWith(".jar")).sorted().toList();
            }
            // the accidental-override renames are decided once for every jar of the folder (a library's and its users')
            scanPack(jars);
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
        scanPack(List.of(in));
        convert(in, out);
        if (Boolean.getBoolean("ladder.mixinRepair.audit") && mixinRepair != null) {
            System.out.println(mixinRepair.audit());
        }
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

    /** The prefix of a mod method renamed because it would override a game method below by accident. */
    static final String OWN = "ladder$own$";

    /** A class of the jars converted together: its supertypes and its own non-static methods ("name desc"). */
    record PackClass(String superName, String[] interfaces, Set<String> methods, Set<String> privates, boolean mixin) {
    }

    /** Every class of the jars converted together (the batch folder, or the one jar), embedded jars included. */
    static final Map<String, PackClass> pack = new HashMap<>();
    /** class -> the pack classes that extend or implement it */
    static final Map<String, List<String>> packChildren = new HashMap<>();
    /** "class name desc" -> whether that method's override family is renamed */
    static final Map<String, Boolean> ownDecided = new HashMap<>();
    /** the pack classes whose supertypes leave the pack for a class nobody gave (reported once each) */
    static final Map<String, String> packUnknown = new HashMap<>();

    static void scanPack(List<Path> jars) throws IOException {
        pack.clear();
        packChildren.clear();
        ownDecided.clear();
        packUnknown.clear();
        for (Path jar : jars) {
            try (InputStream in = Files.newInputStream(jar)) {
                scanJar(in, 0);
            } catch (IOException | RuntimeException e) {
                // an unreadable jar: its own conversion reports it
            }
        }
        for (var e : pack.entrySet()) {
            List<String> supers = new ArrayList<>();
            supers.add(e.getValue().superName());
            supers.addAll(Arrays.asList(e.getValue().interfaces()));
            for (String sup : supers) {
                if (sup != null) {
                    packChildren.computeIfAbsent(sup, k -> new ArrayList<>()).add(e.getKey());
                }
            }
        }
    }

    static void scanJar(InputStream jar, int depth) throws IOException {
        ZipInputStream zip = new ZipInputStream(jar);
        for (ZipEntry e; (e = zip.getNextEntry()) != null;) {
            String name = e.getName();
            if (name.endsWith(".jar") && depth < 3) {
                // an embedded jar (jarjar): its classes are converted with it and seen by the jars that use them
                scanJar(new java.io.ByteArrayInputStream(zip.readAllBytes()), depth + 1);
            } else if (name.endsWith(".class") && !name.startsWith("META-INF/")) {
                ClassNode node = new ClassNode();
                try {
                    new ClassReader(zip.readAllBytes()).accept(node, ClassReader.SKIP_CODE);
                } catch (RuntimeException bad) {
                    continue;
                }
                Set<String> methods = new HashSet<>(), privates = new HashSet<>();
                for (MethodNode m : node.methods) {
                    if ((m.access & Opcodes.ACC_STATIC) == 0 && !m.name.startsWith("<")) {
                        ((m.access & Opcodes.ACC_PRIVATE) == 0 ? methods : privates).add(m.name + " " + m.desc);
                    }
                }
                pack.putIfAbsent(node.name, new PackClass(node.superName, node.interfaces.toArray(new String[0]), methods, privates,
                        !mixinTargetsOf(node).isEmpty()));
            }
        }
    }

    /**
     * A data generator's class: a data provider or sub-provider of the game or NeoForge (net/minecraft/data/...,
     * NeoForge's data and model generators) by its supertypes, a class nested in one, or a class of a "datagen"
     * package. Its code runs in runData only, never when the game plays.
     */
    static boolean datagenOnly(String cls) {
        if (cls.contains("/datagen/") || cls.contains("/data/gen/")) {
            return true;
        }
        int nested = cls.indexOf('$');
        if (nested > 0 && datagenOnly(cls.substring(0, nested))) {
            return true;
        }
        Deque<String> todo = new ArrayDeque<>(List.of(cls));
        Set<String> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            String c = todo.poll();
            if (c == null || !seen.add(c)) {
                continue;
            }
            if (c.startsWith("net/minecraft/data/") || c.startsWith("net/neoforged/neoforge/common/data/")
                    || c.startsWith("net/neoforged/neoforge/client/model/generators/")) {
                return true;
            }
            PackClass pc = pack.get(c);
            if (pc != null) {
                if (pc.superName() != null) {
                    todo.add(pc.superName());
                }
                todo.addAll(List.of(pc.interfaces()));
            }
        }
        return false;
    }

    /** A supertype outside the pack that no mod class can be: the JDK, the game, and the libraries the game ships. */
    static boolean outsideKnown(String cls) {
        return Stage0.platform(cls) || cls.startsWith("java/") || cls.startsWith("javax/") || cls.startsWith("jdk/") || cls.startsWith("sun/")
                || cls.startsWith("com/sun/") || cls.startsWith("com/mojang/") || cls.startsWith("com/google/") || cls.startsWith("org/apache/")
                || cls.startsWith("it/unimi/") || cls.startsWith("io/netty/") || cls.startsWith("org/joml/") || cls.startsWith("org/slf4j/")
                || cls.startsWith("org/lwjgl/") || cls.startsWith("com/electronwill/") || cls.startsWith("org/spongepowered/")
                || cls.startsWith("com/llamalad7/") || cls.startsWith("org/objectweb/") || cls.startsWith("kotlin/") || cls.startsWith("kotlinx/")
                || cls.startsWith("org/jetbrains/") || cls.startsWith("org/intellij/") || cls.startsWith("cpw/mods/") || cls.startsWith("net/minecraftforge/");
    }

    /** The game's supertypes of a pack class, across the pack's jars; an unknown non-library supertype is noted. */
    static List<String> packPlatformStarts(String cls) {
        List<String> out = new ArrayList<>();
        Deque<String> todo = new ArrayDeque<>(List.of(cls));
        Set<String> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            String c = todo.poll();
            if (c == null || !seen.add(c)) {
                continue;
            }
            if (Stage0.platform(c)) {
                out.add(c);
                continue;
            }
            PackClass pc = pack.get(c);
            if (pc == null) {
                if (!outsideKnown(c)) {
                    packUnknown.putIfAbsent(cls, c);
                }
                continue;
            }
            todo.add(pc.superName());
            todo.addAll(Arrays.asList(pc.interfaces()));
        }
        return out;
    }

    /** The pack class (this one or a pack supertype) declaring that non-static, non-private method, or null. */
    static String packDeclaring(String cls, String name, String desc) {
        Deque<String> todo = new ArrayDeque<>(List.of(cls));
        Set<String> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            String c = todo.poll();
            PackClass pc = c == null || !seen.add(c) ? null : pack.get(c);
            if (pc == null) {
                continue;
            }
            if (pc.methods().contains(name + " " + desc)) {
                return c;
            }
            todo.add(pc.superName());
            todo.addAll(Arrays.asList(pc.interfaces()));
        }
        return null;
    }

    /** Every pack class whose supertypes (transitively) include that one. */
    static Set<String> packDescendants(String cls) {
        Set<String> out = new HashSet<>();
        Deque<String> todo = new ArrayDeque<>(List.of(cls));
        while (!todo.isEmpty()) {
            for (String child : packChildren.getOrDefault(todo.poll(), List.of())) {
                if (out.add(child)) {
                    todo.add(child);
                }
            }
        }
        return out;
    }

    /**
     * Whether a pack method is renamed: its override family (the pack classes that declare it above or below it, across
     * jars) overrides nothing in the upper version, but some member would override or implement a game method of the
     * same name and descriptor below (a 1.21 payload's own {@code write(FriendlyByteBuf)} would become 1.20.4's
     * {@code CustomPacketPayload.write}: a package-private one fails with IllegalAccessError, a public one is called in
     * place of the payload's codec). The whole family is renamed with all its calls, in every jar of the pack.
     */
    static boolean ownRenamed(String cls, String name, String desc) {
        String key = cls + " " + name + " " + desc;
        Boolean known = ownDecided.get(key);
        if (known != null) {
            return known;
        }
        // the family: up to the declaring supertypes, down to the declaring subtypes of any member, until it stops growing
        Set<String> family = new LinkedHashSet<>(List.of(cls));
        Deque<String> todo = new ArrayDeque<>(family);
        while (!todo.isEmpty()) {
            String c = todo.poll();
            Set<String> near = new HashSet<>();
            PackClass pc = pack.get(c);
            if (pc != null) {
                List<String> supers = new ArrayList<>();
                supers.add(pc.superName());
                supers.addAll(Arrays.asList(pc.interfaces()));
                for (String sup : supers) {
                    String decl = sup == null ? null : packDeclaring(sup, name, desc);
                    if (decl != null) {
                        near.add(decl);
                    }
                }
            }
            for (String d : packDescendants(c)) {
                if (pack.get(d).methods().contains(name + " " + desc)) {
                    near.add(d);
                }
            }
            for (String n : near) {
                if (family.add(n)) {
                    todo.add(n);
                }
            }
        }
        Stage0.Version top = versions.get(0), bottom = versions.get(versions.size() - 1);
        boolean above = false, below = false;
        for (String member : family) {
            PackClass pc = pack.get(member);
            if (pc == null || pc.mixin()) {
                // a mixin's methods are merged into the game's class: overriding is what they are for
                above = true;
                continue;
            }
            for (String p : packPlatformStarts(member)) {
                if (top.declaring(p, name, desc, true) != null) {
                    above = true;
                }
                String lower = remapper.map(p);
                String lowerDesc = remapper.mapMethodDesc(desc);
                String decl = lower.startsWith(CARRIED) ? null : bottom.declaring(lower, name, lowerDesc, true);
                Stage0.Cls info = decl == null ? null : bottom.classes.get(decl);
                Integer access = info == null ? null : info.access.get("M " + name + " " + lowerDesc);
                if (access != null && (access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) == 0) {
                    below = true;
                }
            }
        }
        // an override above is the same hook below (renamed or bridged by the walk): only a family that overrides
        // nothing above and something below is renamed
        boolean renamed = below && !above;
        for (String member : family) {
            ownDecided.put(member + " " + name + " " + desc, renamed);
        }
        ownDecided.put(key, renamed);
        return renamed;
    }

    /** The accidental overrides of a class get their own names; so do the calls and method references to them. */
    static void renameOwn(ClassNode node) {
        PackClass self = pack.get(node.name);
        if (self != null && !self.mixin()) {
            boolean checked = false;
            for (MethodNode m : node.methods) {
                if ((m.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) != 0 || m.name.startsWith("<")) {
                    continue;
                }
                if (!checked) {
                    checked = true;
                    packPlatformStarts(node.name);
                }
                String unknown = packUnknown.get(node.name);
                if (unknown != null && onlyBelow(m.name, m.desc)) {
                    // a game method of that name and descriptor exists below only: the unseen supertype could make it one
                    problem("can't check " + node.name + "." + m.name + m.desc + " for overriding a game method below by accident: its supertype "
                            + unknown + " isn't in the jars converted together");
                }
                if (ownRenamed(node.name, m.name, m.desc)) {
                    m.name = OWN + m.name;
                }
            }
        }
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode insn : m.instructions) {
                if (insn instanceof MethodInsnNode call && renamedCall(call.owner, call.name, call.desc)) {
                    call.name = OWN + call.name;
                } else if (insn instanceof InvokeDynamicInsnNode indy) {
                    for (int a = 0; a < indy.bsmArgs.length; a++) {
                        if (indy.bsmArgs[a] instanceof Handle h && h.getTag() != Opcodes.H_INVOKESTATIC && renamedCall(h.getOwner(), h.getName(), h.getDesc())) {
                            indy.bsmArgs[a] = new Handle(h.getTag(), h.getOwner(), OWN + h.getName(), h.getDesc(), h.isInterface());
                        }
                    }
                }
            }
        }
    }

    /** "name desc" of the game's non-static, non-private methods: in the bottom version, in the top one (made once). */
    static Set<String> bottomSignatures, topSignatures;

    /** Whether a game method of that name and (converted) descriptor exists below but none of that name and descriptor above. */
    static boolean onlyBelow(String name, String desc) {
        if (bottomSignatures == null) {
            bottomSignatures = signatures(versions.get(versions.size() - 1));
            topSignatures = signatures(versions.get(0));
        }
        return bottomSignatures.contains(name + " " + remapper.mapMethodDesc(desc)) && !topSignatures.contains(name + " " + desc);
    }

    static Set<String> signatures(Stage0.Version v) {
        Set<String> out = new HashSet<>();
        for (Stage0.Cls info : v.classes.values()) {
            for (String m : info.methods) {
                Integer access = info.access.get("M " + m);
                if (access != null && (access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) == 0 && !m.startsWith("<")) {
                    out.add(m);
                }
            }
        }
        return out;
    }

    static boolean renamedCall(String owner, String name, String desc) {
        if (name.startsWith("<") || owner.startsWith("[") || !pack.containsKey(owner)) {
            return false;
        }
        if (pack.get(owner).privates().contains(name + " " + desc)) {
            // a private method of the call's own class is the target (never inherited, never renamed)
            return false;
        }
        String decl = packDeclaring(owner, name, desc);
        return decl != null && ownRenamed(decl, name, desc);
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
                for (var f : node.fields) {
                    mc.fields.add(f.name + " " + f.desc);
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
                if (internal.startsWith(BOTTOM)) {
                    // a rule's name of the bottom class a top class of the same name hides (1.20.4's ArmorMaterial
                    // interface beside 1.20.5's carried record): the bottom one, as it is
                    return internal.substring(BOTTOM.length());
                }
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
                    // a data generator's class runs only in runData, never in the game: what it can't bring down doesn't
                    // keep the mod from running (its problems are listed apart, as unverified)
                    boolean datagen = datagenOnly(node.name);
                    Set<String> before = datagen ? new HashSet<>(problems) : null;
                    Map<String, String> landsAbove = null;
                    if (mixinTargets.containsKey(node.name)) {
                        MixinPoints.modClasses = mod::containsKey;
                        landsAbove = MixinPoints.check(node, mixinTargets.get(node.name), Convert::topClass);
                        remapMixinSelectors(node, remapper);
                    }
                    if (stripMod.contains(node.name) && node.visibleAnnotations != null) {
                        node.visibleAnnotations.removeIf(a -> a.desc.equals(MOD_ANNOTATION));
                    }
                    renameOwn(node);
                    List<MethodNode> lowerOverrides = overrideBridges(node);
                    applyRules(node, true);
                    redirectCarried(node);
                    ClassNode remapped = new ClassNode();
                    node.accept(new ClassRemapper(remapped, remapper));
                    fixInterfaceCalls(remapped);
                    applyHandlerFixes(node.name, remapped);
                    lowerOverrides.addAll(addAfterRemap.getOrDefault(node.name, List.of()));
                    addAfterRemap.remove(node.name);
                    for (MethodNode g : lowerOverrides) {
                        if (remapped.methods.stream().noneMatch(x -> x.name.equals(g.name) && x.desc.equals(g.desc))) {
                            remapped.methods.add(g);
                        }
                    }
                    int writeFlags = 0;
                    if (landsAbove != null) {
                        ClassNode checked = checkMixinPoints(node.name, landsAbove, remapped, entries);
                        if (checked != remapped) {
                            remapped = checked;
                            writeFlags = ClassWriter.COMPUTE_MAXS;
                        }
                    }
                    ClassWriter writer = new ClassWriter(writeFlags);
                    remapped.accept(writer);
                    data = writer.toByteArray();
                    name = remapper.map(name.substring(0, name.length() - 6)) + ".class";
                    if (datagen) {
                        for (String p : new ArrayList<>(problems)) {
                            if (!before.contains(p)) {
                                problems.remove(p);
                                unverified.add("data generator " + node.name + " (runs in runData only): " + p);
                            }
                        }
                    }
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
    /** K rows: "owner.<init> upperDesc" -> {lower desc, adapter class, the lower parameters (an upper one $i, or an adapter method)} */
    static final Map<String, String[]> constructorArguments = new HashMap<>();
    /** F rows: "owner.name desc" of a static field -> {adapter class, getter}: its reads go to the getter */
    static final Map<String, String[]> fieldRedirects = new HashMap<>();
    /** M rows: owner, name, desc (top names), lower name, lower desc (bottom names): the same hook below, renamed */
    static final List<String[]> memberMoves = new ArrayList<>();
    /** the adapters jar: class -> its methods "name desc" (each rule's target is checked against it) */
    static final Map<String, Set<String>> adapterMethods = new HashMap<>();
    /** An adapters class's superclass and interfaces (a provided class's members may be inherited, from the JDK too). */
    static final Map<String, List<String>> adapterSupers = new HashMap<>();

    /** Whether a provided class has the member ("name desc" or "F name desc"), declared or inherited. */
    static boolean providedHas(String cls, String member, Set<String> seen) {
        if (cls == null || !seen.add(cls)) {
            return false;
        }
        Set<String> members = adapterMethods.get(cls);
        if (members != null) {
            if (members.contains(member)) {
                return true;
            }
            for (String s : adapterSupers.getOrDefault(cls, List.of())) {
                if (providedHas(s, member, seen)) {
                    return true;
                }
            }
            return false;
        }
        if (cls.startsWith("java/")) {
            // a JDK supertype (Iterable, Comparable, Record...): its public methods
            try {
                Class<?> c = Class.forName(cls.replace('/', '.'), false, ClassLoader.getPlatformClassLoader());
                for (java.lang.reflect.Method m : c.getMethods()) {
                    if (member.equals(m.getName() + " " + Type.getMethodDescriptor(m))) {
                        return true;
                    }
                }
            } catch (ClassNotFoundException ignored) {
                // not a JDK class
            }
        }
        return false;
    }

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
                    // a class whose intermediary twin below is a different class: new above (carried, not renamed)
                    case "N" -> Stage0.notTheSame.add(c[1]);
                    // (an optional 8th column: the lower method, "name desc", that 1.20.4 has abstract where the newer
                    // one is: a concrete mod class right under it gets an empty one, the O row's hook doing the work)
                    case "O" -> overrides.add(new String[] {c[1], c[2], c[3], c[4], c[5], c[6], c.length > 7 ? c[7] : null});
                    case "X" -> redirects.put(c[1] + "." + c[2] + " " + c[3], new String[] {c[4], c[5]});
                    // a constructor that takes more below: each lower parameter is an upper one or an adapter's value
                    case "K" -> constructorArguments.put(c[1] + ".<init> " + c[2], new String[] {c[3], c[4], c[5]});
                    // a class renamed below with the same members: its uses get the lower name
                    case "R" -> NEOFORGE_MOVES.put(c[1], c[2]);
                    // a member (of the owner and its subclasses) that is another one below: bridged to it
                    case "M" -> memberMoves.add(new String[] {c[1], c[2], c[3], c[4], c[5]});
                    // a static field below that the adapters give: its reads go to their getter
                    case "F" -> fieldRedirects.put(c[1] + "." + c[2] + " " + c[3], new String[] {c[4], c[5]});
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
                        // fields too, marked (a provided class's fields are checked like its methods)
                        node.fields.forEach(f -> methods.add("F " + f.name + " " + f.desc));
                        adapterMethods.put(node.name, methods);
                        List<String> supers = new ArrayList<>(node.interfaces);
                        if (node.superName != null) {
                            supers.add(0, node.superName);
                        }
                        adapterSupers.put(node.name, supers);
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
        String desc = remapper.mapMethodDesc(upperDesc);
        if (!providedHas(cls, name + " " + desc, new HashSet<>())) {
            problem("the adapters' " + cls + " has no " + name + desc);
        }
    }

    /** A field of a provided class the mod uses must be in the adapters' class (a game component not given yet, say). */
    static void checkProvidedField(String cls, String name, String upperDesc) {
        if (adapterMethods.isEmpty()) {
            return;
        }
        String desc = remapper.mapDesc(upperDesc);
        if (!providedHas(cls, "F " + name + " " + desc, new HashSet<>())) {
            problem("the adapters' " + cls + " has no field " + name + " " + desc);
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
        if (overrides.isEmpty() && redirects.isEmpty() && fieldRedirects.isEmpty()) {
            return;
        }
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode insn : m.instructions) {
                if (insn instanceof FieldInsnNode field) {
                    String[] f = fieldRedirects.get(field.owner + "." + field.name + " " + field.desc);
                    String declaring = field.owner;
                    if (f == null && (Stage0.platform(field.owner) || field.getOpcode() == Opcodes.GETFIELD)) {
                        // an inherited field read through a subclass (SwordItem.BASE_ATTACK_DAMAGE_UUID, Item's; a mod's
                        // armour item reading ArmorItem.material)
                        Stage0.Ref declared = resolve(field.owner, field.name, field.desc, false);
                        f = declared == null ? null : fieldRedirects.get(declared.cls() + "." + field.name + " " + field.desc);
                        declaring = declared == null ? declaring : declared.cls();
                    }
                    if (f == null) {
                        continue;
                    }
                    if (field.getOpcode() == Opcodes.GETFIELD) {
                        // an instance field: the getter takes the object (1.20.5's ArmorItem.material holder of 1.20.4's material)
                        String desc = "(L" + declaring + ";)" + field.desc;
                        checkAdapter(f[0], f[1], desc, "F " + declaring + "." + field.name);
                        m.instructions.set(field, new MethodInsnNode(Opcodes.INVOKESTATIC, f[0], f[1], desc, false));
                        continue;
                    }
                    if (field.getOpcode() != Opcodes.GETSTATIC) {
                        problem("a write to " + field.owner + "." + field.name + ", which the adapters give: not possible");
                        continue;
                    }
                    checkAdapter(f[0], f[1], "()" + field.desc, "F " + field.owner + "." + field.name);
                    m.instructions.set(field, new MethodInsnNode(Opcodes.INVOKESTATIC, f[0], f[1], "()" + field.desc, false));
                } else if (insn instanceof InvokeDynamicInsnNode indy && !redirects.isEmpty()) {
                    // a method reference to an X row's member (idMap::getIdOrThrow): to the adapter, the receiver first
                    // (the lambda's captured or first argument, as for a call)
                    for (int a = 0; a < indy.bsmArgs.length; a++) {
                        if (!(indy.bsmArgs[a] instanceof Handle h) || h.getName().startsWith("<") || h.getOwner().startsWith("[")
                                || h.getTag() != Opcodes.H_INVOKEVIRTUAL && h.getTag() != Opcodes.H_INVOKEINTERFACE && h.getTag() != Opcodes.H_INVOKESTATIC) {
                            continue;
                        }
                        Stage0.Ref ref = resolve(h.getOwner(), h.getName(), h.getDesc(), true);
                        String[] x = ref == null ? null : redirects.get(ref.cls() + "." + ref.name() + " " + ref.desc());
                        if (x == null) {
                            continue;
                        }
                        String desc = h.getTag() == Opcodes.H_INVOKESTATIC ? h.getDesc() : "(L" + ref.cls() + ";" + h.getDesc().substring(1);
                        checkAdapter(x[0], x[1], desc, "X " + ref.key() + " (method reference)");
                        indy.bsmArgs[a] = new Handle(Opcodes.H_INVOKESTATIC, x[0], x[1], desc, false);
                    }
                }
            }
        }
        Stage0.Version here = versions.get(level);
        // a mixin's interfaces would go onto the game's own class it targets: only the mod's own classes get them
        if (modClass && !mixinTargets.containsKey(node.name)) {
            List<String> platform = starts(node.name);
            for (String[] o : overrides) {
                boolean under = platform.stream().anyMatch(p -> subtypeOf(here, p, o[0], new HashSet<>()));
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
                if (o[6] != null && (node.access & Opcodes.ACC_ABSTRACT) == 0 && abstractBelow(node.superName, o[6])) {
                    String[] lower = o[6].split(" ");
                    if (node.methods.stream().noneMatch(m -> m.name.equals(lower[0]) && m.desc.equals(lower[1]))) {
                        MethodNode stub = new MethodNode(Opcodes.ACC_PROTECTED, lower[0], lower[1], null, null);
                        stub.instructions.add(new InsnNode(Opcodes.RETURN));
                        stub.maxLocals = Type.getArgumentsAndReturnSizes(lower[1]) >> 2;
                        node.methods.add(stub);
                    }
                }
            }
        }
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode insn : m.instructions.toArray()) {
                if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESPECIAL && call.name.equals("<init>")
                        && Stage0.platform(call.owner)) {
                    // new X(args) of a constructor an adapter's factory gives (an X row on <init>)
                    Stage0.Ref ref = resolve(call.owner, call.name, call.desc, true);
                    String[] x = ref == null ? null : redirects.get(ref.cls() + "." + ref.name() + " " + ref.desc());
                    if (x != null) {
                        checkAdapter(x[0], x[1], call.desc.substring(0, call.desc.indexOf(')') + 1) + "L" + call.owner + ";", "X " + ref.key());
                        newToFactory(m, call, x, ref);
                    }
                    // new X(args) or super(args) of a constructor that takes more below (a K row)
                    String[] k = ref == null ? null : constructorArguments.get(ref.cls() + "." + ref.name() + " " + ref.desc());
                    if (x == null && k != null) {
                        spreadArguments(m, call, k, ref);
                    }
                    continue;
                }
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
                    // (a row whose lower hook 1.20.4 runs for every game class itself, defineSynchedData(), has defaults
                    // that are right under any game class: the game's own overrides ran already)
                    if (!ref.cls().equals(o[0]) && o[6] == null) {
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

    /**
     * Whether the bottom game's class (or the first of its superclasses that declares it) has the method abstract: a
     * mod class right under Entity must implement 1.20.4's abstract defineSynchedData(). A mod superclass isn't the
     * game's: it gets its own empty one, so its subclasses need none.
     */
    static boolean abstractBelow(String superName, String method) {
        Stage0.Version bottom = versions.get(versions.size() - 1);
        for (String c = superName; c != null; ) {
            Stage0.Cls info = bottom.classes.get(c);
            if (info == null) {
                return false;
            }
            Integer access = info.access.get("M " + method);
            if (access != null) {
                return (access & Opcodes.ACC_ABSTRACT) != 0;
            }
            c = info.superName;
        }
        return false;
    }

    static String mapClass(String c) {
        String cacheKey = level + " " + c;
        String cached = classCache.get(cacheKey);
        if (cached != null) {
            cachedProblems.getOrDefault("C " + cacheKey, List.of()).forEach(Convert::report);
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
        if (!method && modHides(owner, name + " " + desc, new HashSet<>())) {
            // the mod's own field (a mod class on the way up declares it): not the game's of the same name
            return null;
        }
        Stage0.Version here = versions.get(level);
        for (String start : starts(owner)) {
            String decl = here.declaring(start, name, desc, method);
            if (decl != null) {
                return new Stage0.Ref(method ? 'M' : 'F', decl, name, desc);
            }
        }
        return null;
    }

    /** whether a mod class met before the game's classes (the field lookup's order) declares the field */
    static boolean modHides(String cls, String field, Set<String> seen) {
        Stage0.ModClass mc = cls == null || !seen.add(cls) ? null : mod.get(cls);
        if (mc == null || mixinTargets.containsKey(cls)) {
            // (a mixin's fields are its target's: @Shadow)
            return false;
        }
        if (mc.fields.contains(field)) {
            return true;
        }
        for (String i : mc.interfaces) {
            if (modHides(i, field, seen)) {
                return true;
            }
        }
        return modHides(mc.superName, field, seen);
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
                checkProvidedField(provided.get(ref.cls()), name, desc);
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
            cachedProblems.getOrDefault(key, List.of()).forEach(Convert::report);
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
            Stage0.Step s = memberStep(from, to, cur);
            switch (s.outcome()) {
                case SAME, RENAMED -> cur = s.to();
                case CHANGED -> {
                    String[] bridge = cur.name().equals("<init>") ? constructorBridge(i, cur) : bridgeFor(i, cur, s.to());
                    if (bridge != null) {
                        // the call moves to a generated bridge that converts the values (Holder<X> <-> X) and calls below
                        callRedirect.put(key, bridge);
                        return new Stage0.Ref(cur.kind(), bridge[0], bridge[1], cur.desc());
                    }
                    if (ref.kind() == 'F' && ruled(ref.key())) {
                        // a field read an F row's getter takes (a method's X row covers calls, not overrides: no skip)
                        return cur;
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
                    Stage0.Ref moved = i + 2 == versions.size() ? movedMember(i, cur) : null;
                    if (moved != null) {
                        // an M row: the same hook below under another name and descriptor, bridged to it
                        String[] bridge = bridgeFor(i, cur, moved);
                        if (bridge != null) {
                            callRedirect.put(key, bridge);
                            return new Stage0.Ref(cur.kind(), bridge[0], bridge[1], cur.desc());
                        }
                        problem("changed below (an M row), no bridge: " + cur.key() + " -> " + moved.name() + moved.desc());
                        return null;
                    }
                    Set<String> ok = carriable(i);
                    if (Stage0.mapClass(from, to, cur.cls()) == null) {
                        // the owner itself is new: its member comes down with the carried class (mapClass relocates it)
                        if (ok.contains("C " + cur.cls())) {
                            schedule(i, new Stage0.Ref('C', cur.cls(), null, null));
                            return new Stage0.Ref(cur.kind(), CARRIED + cur.cls(), cur.name(), cur.desc());
                        }
                    } else if (ok.contains(cur.key()) && instanceCarried.contains(i + " " + cur.key())) {
                        // a new instance method of a class that exists below: a static helper in the companion class, the
                        // receiver first (none of the game overrides it; a mod class of the pack mustn't either)
                        if (!overriddenInPack(cur.cls(), cur.name() + " " + cur.desc())) {
                            schedule(i, cur);
                            String[] helper = {CARRIED + cur.cls(), cur.name()};
                            callRedirect.put(key, helper);
                            return new Stage0.Ref(cur.kind(), helper[0], helper[1], cur.desc());
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

    /** An M row's lower member for a member missing at the last step (the owner or a subclass of the row's); else null. */
    static Stage0.Ref movedMember(int i, Stage0.Ref cur) {
        if (cur.kind() != 'M' || memberMoves.isEmpty()) {
            return null;
        }
        Stage0.Version from = versions.get(i), to = versions.get(i + 1);
        for (String[] row : memberMoves) {
            if (row[1].equals(cur.name()) && row[2].equals(cur.desc()) && (cur.cls().equals(row[0]) || extendsClass(from, cur.cls(), row[0]))) {
                String lowerOwner = Stage0.mapClass(from, to, cur.cls());
                String decl = lowerOwner == null ? null : to.declaring(lowerOwner, row[3], row[4], true);
                return decl == null ? null : new Stage0.Ref('M', decl, row[3], row[4]);
            }
        }
        return null;
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
    /**
     * A member new above that an adapter rule covers (X: its calls go to an adapter, F: its reads to a getter): carried
     * code that uses it gets the rule too, so it is there below like a provided class.
     */
    static boolean ruled(String key) {
        if (key.length() < 3 || key.charAt(1) != ' ') {
            return false;
        }
        String rest = key.substring(2);
        return key.charAt(0) == 'M' && (redirects.containsKey(rest) || constructorArguments.containsKey(rest))
                || key.charAt(0) == 'F' && fieldRedirects.containsKey(rest);
    }

    static Set<String> carriable(int i) {
        Set<String> got = carriable.get(i);
        if (got != null) {
            return got;
        }
        Stage0.Version from = versions.get(i);
        Stage0.Plan plan = Stage0.plan(from, versions.get(i + 1));
        // Stage0 counts every member whose types changed below as a root; the converter bridges many of them, and the
        // adapters' rules cover others: what is left hard decides
        Set<String> base = new HashSet<>();
        plan.needs.forEach((k, n) -> {
            if (n.hard.stream().allMatch(h -> resolvable(i, h))) {
                base.add(k);
            }
        });
        boolean shrunk = true;
        while (shrunk) {
            shrunk = false;
            for (var it = base.iterator(); it.hasNext();) {
                for (String d : plan.needs.get(it.next()).deps) {
                    if (plan.needs.containsKey(d) && !base.contains(d) && !provided.containsKey(d.substring(2)) && !ruled(d)) {
                        it.remove();
                        shrunk = true;
                        break;
                    }
                }
            }
        }
        Set<String> ok = new HashSet<>();
        Map<String, Map<String, Set<Stage0.Ref>>> code = new HashMap<>();
        for (String key : base) {
            Stage0.Ref r = plan.things.get(key);
            boolean instance = r.kind() == 'M' && from.classes.get(r.cls()) != null && instanceCarriable(from, r);
            if (instance) {
                instanceCarried.add(i + " " + key);
            }
            if (!(r.kind() == 'C' || instance || r.kind() == 'M' && from.classes.get(r.cls()).staticMethods.contains(r.name() + " " + r.desc()))) {
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
                String key = it.next();
                if (there.contains(key.substring(2))) {
                    // a provided or renamed class is there below whatever its upper code needs: never dropped
                    continue;
                }
                Stage0.Needs n = plan.needs.get(key);
                for (String d : n.deps) {
                    if (plan.needs.containsKey(d) && !ok.contains(d) && !ruled(d)) {
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

    /**
     * A new instance method of a class that exists below, carried as a static helper with the receiver first: its own
     * code must not dispatch (no super or private call of its class: invokespecial), and nothing may override it, no
     * game class of its version (a pack's mod classes are checked where the call is), else a static call would skip
     * the override.
     */
    static boolean instanceCarriable(Stage0.Version from, Stage0.Ref r) {
        if (r.name().startsWith("<") || from.classes.get(r.cls()).staticMethods.contains(r.name() + " " + r.desc())) {
            return false;
        }
        byte[] bytes = from.bytes.get(r.cls());
        if (bytes == null) {
            return false;
        }
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_FRAMES);
        MethodNode m = node.methods.stream().filter(x -> x.name.equals(r.name()) && x.desc.equals(r.desc())).findFirst().orElse(null);
        if (m == null || (m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_NATIVE)) != 0 || m.instructions.size() == 0) {
            return false;
        }
        for (AbstractInsnNode insn : m.instructions) {
            if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESPECIAL && !call.name.equals("<init>")) {
                return false;
            }
        }
        Map<String, List<String>> byMethod = declarers.computeIfAbsent(from.id, id -> {
            Map<String, List<String>> all = new HashMap<>();
            from.classes.forEach((cls, info) -> {
                for (String method : info.methods) {
                    if (!info.staticMethods.contains(method)) {
                        all.computeIfAbsent(method, k -> new ArrayList<>()).add(cls);
                    }
                }
            });
            return all;
        });
        for (String other : byMethod.getOrDefault(r.name() + " " + r.desc(), List.of())) {
            if (!other.equals(r.cls()) && subtypeIn(from, other, r.cls())) {
                return false;
            }
        }
        return true;
    }

    /** whether a class of a version extends or implements another (its supertypes in that version) */
    static boolean subtypeIn(Stage0.Version v, String sub, String sup) {
        Deque<String> todo = new ArrayDeque<>(List.of(sub));
        Set<String> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            String c = todo.poll();
            if (c == null || !seen.add(c)) {
                continue;
            }
            if (c.equals(sup)) {
                return true;
            }
            Stage0.Cls info = v.classes.get(c);
            if (info != null) {
                if (info.superName != null) {
                    todo.add(info.superName);
                }
                todo.addAll(Arrays.asList(info.interfaces));
            }
        }
        return false;
    }

    /** whether a mod class of the pack overrides the method (then a static helper would skip the mod's own) */
    static boolean overriddenInPack(String cls, String nameDesc) {
        for (var e : pack.entrySet()) {
            if (e.getValue().methods().contains(nameDesc) && packExtends(e.getKey(), cls, new HashSet<>())) {
                return true;
            }
        }
        return false;
    }

    static boolean packExtends(String c, String sup, Set<String> seen) {
        if (c == null || !seen.add(c)) {
            return false;
        }
        if (c.equals(sup)) {
            return true;
        }
        PackClass pc = pack.get(c);
        if (pc == null) {
            return Stage0.platform(c) && subtypeIn(versions.get(level), c, sup);
        }
        if (packExtends(pc.superName(), sup, seen)) {
            return true;
        }
        for (String i : pc.interfaces()) {
            if (packExtends(i, sup, seen)) {
                return true;
            }
        }
        return false;
    }

    /**
     * A measurement: the new instance methods of existing classes whose own code would link below (Stage0's plan, and
     * every member it reaches accessible from the carried package), and the batch report's "missing M" problems they
     * are. Carried as static extensions (receiver first), these would come down with no adapter.
     */
    static void measureInstanceCarrying(Path report) throws IOException {
        Set<String> ok = new HashSet<>();
        Map<String, Integer> blocked = new TreeMap<>();
        for (int i = 0; i + 1 < versions.size(); i++) {
            Stage0.Version from = versions.get(i);
            Stage0.Plan plan = Stage0.plan(from, versions.get(i + 1));
            level = i;
            for (String key : plan.needs.keySet()) {
                Stage0.Ref r = plan.things.get(key);
                if (r == null || r.kind() != 'M' || r.name().startsWith("<") || plan.absent.contains(r.cls())
                        || from.classes.get(r.cls()).staticMethods.contains(r.name() + " " + r.desc())) {
                    continue;
                }
                if (!plan.carried.contains(key)) {
                    String why = plan.needs.get(key).hard.isEmpty() ? "needs uncarried code" : plan.needs.get(key).hard.iterator().next().replaceAll(":.*", "");
                    blocked.merge(why, 1, Integer::sum);
                    continue;
                }
                Set<Stage0.Ref> refs = new HashSet<>(Stage0.codeRefs(from, r.cls()).getOrDefault(r.name() + " " + r.desc(), Set.of()));
                if (reachable(i, r, refs)) {
                    ok.add(from.id + "|" + r.cls() + "." + r.name() + " " + r.desc());
                } else {
                    blocked.merge("reaches a non-public member", 1, Integer::sum);
                }
            }
        }
        java.util.regex.Pattern missing = java.util.regex.Pattern.compile("missing M (\\S+)\\.(\\S+) (\\S+) \\((\\S+) -> \\S+\\)");
        int problemsAll = 0, problemsOk = 0;
        Set<String> modsAll = new HashSet<>(), modsAllOk = new HashSet<>();
        Map<String, Integer> top = new HashMap<>();
        for (String line : Files.readAllLines(report)) {
            String[] parts = line.split("\t");
            if (parts.length < 5) {
                continue;
            }
            boolean every = true, any = false;
            for (String problem : parts[4].split(" \\| ")) {
                java.util.regex.Matcher m = missing.matcher(problem);
                if (!m.find()) {
                    continue;
                }
                any = true;
                problemsAll++;
                if (ok.contains(m.group(4) + "|" + m.group(1) + "." + m.group(2) + " " + m.group(3))) {
                    problemsOk++;
                    top.merge(m.group(1) + "." + m.group(2), 1, Integer::sum);
                } else {
                    every = false;
                }
            }
            if (any) {
                modsAll.add(parts[1]);
                if (every) {
                    modsAllOk.add(parts[1]);
                }
            }
        }
        System.out.println("new instance methods that would link below: " + ok.size() + "; not carriable: " + blocked);
        System.out.println("'missing M' problems in the report: " + problemsAll + ", of them carriable instance methods: " + problemsOk);
        System.out.println("mods with 'missing M' problems: " + modsAll.size() + ", all of them carriable: " + modsAllOk.size());
        top.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(25)
                .forEach(e -> System.out.println(String.format("%5d %s", e.getValue(), e.getKey())));
    }

    /**
     * Whether one of Stage0's hard needs is something the converter resolves itself: a member whose types changed that
     * a generated bridge (public below) or an adapter rule covers, a missing member an adapter rule covers.
     */
    static boolean resolvable(int i, String hard) {
        int colon = hard.indexOf(": ");
        if (colon < 0) {
            return false;
        }
        String what = hard.substring(0, colon), key = hard.substring(colon + 2);
        if (ruled(key)) {
            // an adapter rule covers it, whatever kind of root it is (a library member with no code below, say)
            return true;
        }
        if (!what.equals("changed below") && !what.equals("missing below")) {
            return false;
        }
        char kind = key.charAt(0);
        String rest = key.substring(2);
        int space = rest.indexOf(' ');
        if (kind == 'C' || space < 0) {
            return false;
        }
        String member = rest.substring(0, space), desc = rest.substring(space + 1);
        int dot = member.lastIndexOf('.');
        Stage0.Ref ref = new Stage0.Ref(kind, member.substring(0, dot), member.substring(dot + 1), desc);
        if (kind == 'M' && redirects.containsKey(ref.cls() + "." + ref.name() + " " + ref.desc())) {
            return true;
        }
        if (kind == 'F' && fieldRedirects.containsKey(ref.cls() + "." + ref.name() + " " + ref.desc())) {
            return true;
        }
        if (provided.containsKey(ref.cls())) {
            return true;
        }
        if (!what.equals("changed below") || ref.name().startsWith("<") || adapterMethods.get(CONVERSIONS) == null) {
            return false;
        }
        Stage0.Version from = versions.get(i), to = versions.get(i + 1);
        Stage0.Step step = memberStep(from, to, ref);
        Stage0.Ref lower = step.to();
        if (lower == null || to.classes.get(lower.cls()) == null) {
            return false;
        }
        Stage0.Ref end = quietWalk(i + 1, lower);
        Stage0.Cls bottomOwner = end == null ? null : versions.get(versions.size() - 1).classes.get(end.cls());
        Integer access = bottomOwner == null ? null : bottomOwner.access.get(end.kind() + " " + end.name() + " " + end.desc());
        if (access == null || (access & Opcodes.ACC_PUBLIC) == 0) {
            return false;
        }
        if (kind == 'F') {
            return convertible(Type.getType(ref.desc()), Type.getType(lower.desc()));
        }
        return paramMap(Type.getArgumentTypes(ref.desc()), Type.getArgumentTypes(lower.desc())) != null
                && convertible(Type.getReturnType(ref.desc()), Type.getReturnType(lower.desc()));
    }

    /** Whether every member the carried code reaches is accessible from the carried package at the bottom version. */
    static boolean reachable(int rung, Stage0.Ref thing, Set<Stage0.Ref> refs) {
        return unreachable(rung, thing, refs) == null;
    }

    /** The first reference of a carried thing's code that isn't public below (null: all reachable). */
    static Stage0.Ref unreachable(int rung, Stage0.Ref thing, Set<Stage0.Ref> refs) {
        Stage0.Version bottom = versions.get(versions.size() - 1);
        for (Stage0.Ref ref : refs) {
            if (ref.kind() == 'C' || ref.cls().equals(thing.cls()) && thing.kind() == 'C') {
                // types, and a carried class's own members (they come down with it)
                continue;
            }
            if (ruled(ref.key())) {
                // an X/F row's adapter takes the use (a protected field below read through a public getter)
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
            return ref;
        }
        return null;
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
    /**
     * A step down; a method whose parameter type is missing below and that no intermediary pairs (1.21's
     * AbstractCookingRecipe.assemble(SingleRecipeInput, Provider), a new overload) is the one method of its name and
     * arity below when its values convert (assemble(Container, Provider)): a change the bridges take.
     */
    static Stage0.Step memberStep(Stage0.Version from, Stage0.Version to, Stage0.Ref ref) {
        Stage0.Step s = Stage0.step(from, to, ref);
        if (s.outcome() != Stage0.Outcome.MISSING_CLASS || ref.kind() != 'M' || ref.name().startsWith("<") || !ref.desc().startsWith("(")) {
            return s;
        }
        String owner = Stage0.mapClass(from, to, ref.cls());
        Stage0.Cls info = owner == null ? null : to.classes.get(owner);
        if (info == null) {
            return s;
        }
        Type[] up = Type.getArgumentTypes(ref.desc());
        // only for the recipe inputs (types the bridges convert to the lower class): other types missing below have
        // their own ways down (provided classes, carried ones)
        boolean inputs = false;
        for (Type t : up) {
            if (t.getSort() == Type.OBJECT && Stage0.platform(t.getInternalName()) && Stage0.mapClass(from, to, t.getInternalName()) == null) {
                if (!RECIPE_INPUTS.contains(t.getInternalName())) {
                    return s;
                }
                inputs = true;
            }
        }
        if (!inputs) {
            return s;
        }
        // the methods of that name and arity whose values convert; of several, the one that isn't a javac bridge
        // (1.20.6's ShapedRecipe: assemble(CraftingContainer, Provider) and its bridge assemble(Container, Provider))
        List<String> fits = new ArrayList<>();
        for (String m : info.methods) {
            String d0 = m.substring(m.indexOf(' ') + 1);
            if (m.startsWith(ref.name() + " ") && d0.startsWith("(") && d0.indexOf(')') > 0 && Type.getArgumentTypes(d0).length == up.length
                    && paramMap(up, Type.getArgumentTypes(d0)) != null && convertible(Type.getReturnType(ref.desc()), Type.getReturnType(d0))) {
                fits.add(d0);
            }
        }
        if (fits.size() > 1) {
            fits.removeIf(d0 -> {
                Integer access = info.access.get("M " + ref.name() + " " + d0);
                return access != null && (access & (Opcodes.ACC_BRIDGE | Opcodes.ACC_SYNTHETIC)) != 0;
            });
        }
        if (fits.size() != 1) {
            return s;
        }
        String d = fits.get(0);
        return new Stage0.Step(Stage0.Outcome.CHANGED, new Stage0.Ref('M', owner, ref.name(), d), "M	" + ref.cls() + "	" + ref.name() + ref.desc() + "	" + ref.name() + d);
    }

    /** The bottom member a member reaches when each change on the way is one the bridges convert, else null. */
    static Stage0.Ref convertibleWalk(int rung, Stage0.Ref ref) {
        Stage0.Ref cur = ref;
        for (int i = rung; i + 1 < versions.size() && cur != null; i++) {
            Stage0.Step s = memberStep(versions.get(i), versions.get(i + 1), cur);
            if (s.outcome() == Stage0.Outcome.SAME || s.outcome() == Stage0.Outcome.RENAMED) {
                cur = s.to();
            } else if (s.outcome() == Stage0.Outcome.CHANGED && cur.kind() == 'M' && !cur.name().equals("<init>")
                    && paramMap(Type.getArgumentTypes(cur.desc()), Type.getArgumentTypes(s.to().desc())) != null
                    && convertible(Type.getReturnType(cur.desc()), Type.getReturnType(s.to().desc()))) {
                cur = s.to();
            } else {
                cur = null;
            }
        }
        return cur;
    }

    static Stage0.Ref quietWalk(int rung, Stage0.Ref ref) {
        Stage0.Ref cur = ref;
        for (int i = rung; i + 1 < versions.size() && cur != null; i++) {
            Stage0.Step s = memberStep(versions.get(i), versions.get(i + 1), cur);
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

    /** whether the class is the type or extends or implements it (an O row of an interface: NeoForge's extensions) */
    static boolean subtypeOf(Stage0.Version v, String cls, String type, Set<String> seen) {
        if (cls == null || !seen.add(cls)) {
            return false;
        }
        if (cls.equals(type)) {
            return true;
        }
        Stage0.Cls info = v.classes.get(cls);
        if (info == null) {
            return false;
        }
        for (String i : info.interfaces) {
            if (subtypeOf(v, i, type, seen)) {
                return true;
            }
        }
        return subtypeOf(v, info.superName, type, seen);
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
                        && bridgeCall(node, call)) {
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
        if (RECIPE_INPUTS.contains(u) && RECIPE_CONTAINERS.contains(l)) {
            // 1.21's recipe inputs where 1.20.4's recipes take a container (RecipeInputs: a container over the input,
            // the input of the recipe's type made of a container)
            return 10;
        }
        if (isSubtype(l, u)) {
            // the lower type is the upper one's subtype (a DefaultedRegistry for a Registry): read as it is, passed cast
            return 8;
        }
        return -1;
    }

    static final String RECIPE_INPUT_CONVERSIONS = "ladder/adapter/recipe/RecipeInputs";
    static final String CONTAINER = "net/minecraft/world/Container";
    static final Set<String> RECIPE_CONTAINERS = Set.of(CONTAINER, "net/minecraft/world/inventory/CraftingContainer");
    /** 1.21's recipe input types, in the top's and the carried names */
    static final Set<String> RECIPE_INPUTS;

    static {
        Set<String> inputs = new HashSet<>();
        for (String c : List.of("RecipeInput", "CraftingInput", "SingleRecipeInput", "SmithingRecipeInput")) {
            inputs.add("net/minecraft/world/item/crafting/" + c);
            inputs.add(CARRIED + "net/minecraft/world/item/crafting/" + c);
        }
        RECIPE_INPUTS = Set.copyOf(inputs);
    }

    static final Set<String> DELTA_TRACKERS = Set.of("net/minecraft/client/DeltaTracker", "ladder/adapter/provided/DeltaTracker");
    static final String RESOURCE_KEY = "net/minecraft/resources/ResourceKey";
    static final String RESOURCE_LOCATION = "net/minecraft/resources/ResourceLocation";

    /** library types the game's members give where newer versions give their interfaces (Guava's immutables) */
    static final Map<String, Set<String>> LIBRARY_SUPERTYPES = Map.of(
            "com/google/common/collect/ImmutableMap", Set.of("java/util/Map"),
            "com/google/common/collect/ImmutableList", Set.of("java/util/List", "java/util/Collection", "java/lang/Iterable"),
            "com/google/common/collect/ImmutableSet", Set.of("java/util/Set", "java/util/Collection", "java/lang/Iterable"),
            "com/google/common/collect/ImmutableCollection", Set.of("java/util/Collection", "java/lang/Iterable"));

    /** Whether the bottom version's class sub extends or implements sup. */
    static boolean isSubtype(String sub, String sup) {
        if (LIBRARY_SUPERTYPES.getOrDefault(sub, Set.of()).contains(sup)) {
            return true;
        }
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

    /**
     * Context parameters a newer version added that the lower one gets by itself: dropped from calls, supplied to
     * overrides (the registries: HolderLookup.Provider in 1.20.5's saving and loading).
     */
    static final Set<String> CONTEXT_PARAMETERS = Set.of(PROVIDER, REGISTRY_ACCESS);

    /**
     * upper parameter -> lower parameter (-1: a context parameter the lower member hasn't), the lower ones in order and
     * each convertible; null when the parameters don't map so.
     */
    static int[] paramMap(Type[] up, Type[] low) {
        // the lower member may take more only where a colour int is its four floats (colorAt)
        if (low.length > up.length + 3) {
            return null;
        }
        int[] map = new int[up.length];
        int k = 0;
        for (int j = 0; j < up.length; j++) {
            boolean context = up[j].getSort() == Type.OBJECT && CONTEXT_PARAMETERS.contains(up[j].getInternalName());
            if (colorAt(up, j, low, k)) {
                // 1.21's ARGB colour int where the lower member takes red, green, blue, alpha floats: -(k + 2)
                map[j] = -(k + 2);
                k += 4;
                continue;
            }
            // a context parameter stays only when the lower member has the same one there
            if (k < low.length && convertible(up[j], low[k]) && (!context || up.length - j == low.length - k || up[j].equals(low[k]))) {
                map[j] = k++;
            } else if (context) {
                map[j] = -1;
            } else {
                return null;
            }
        }
        return k == low.length ? map : null;
    }

    /**
     * 1.21 packed the colour of its render calls (Model.renderToBuffer, ModelPart.render...) into one ARGB int where 1.20.6
     * took four floats (red, green, blue, alpha): an int of the upper member at j is those four of the lower at k, the
     * rest of both lining up.
     */
    static boolean colorAt(Type[] up, int j, Type[] low, int k) {
        if (up[j].getSort() != Type.INT || k + 4 > low.length || up.length - j - 1 != low.length - k - 4) {
            return false;
        }
        for (int c = 0; c < 4; c++) {
            if (low[k + c].getSort() != Type.FLOAT) {
                return false;
            }
        }
        return true;
    }

    /** the colour int in the local at slot, as the four floats (red, green, blue, alpha) on the stack */
    static void unpackColor(InsnList code, int slot) {
        for (String channel : new String[] {"red", "green", "blue", "alpha"}) {
            code.add(new VarInsnNode(Opcodes.ILOAD, slot));
            code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, channel, "(I)F", false));
        }
    }

    /** the four floats in the locals at slots[k..k+3], as the colour int on the stack */
    static void packColor(InsnList code, int[] slots, int k) {
        for (int c = 0; c < 4; c++) {
            code.add(new VarInsnNode(Opcodes.FLOAD, slots[k + c]));
        }
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "color", "(FFFF)I", false));
    }

    /** The value of a context parameter the lower version doesn't pass (the current registries). */
    static void supplyContext(InsnList code, Type type) {
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "registries", "()L" + REGISTRY_ACCESS + ";", false));
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
            case 10 -> {
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RECIPE_INPUT_CONVERSIONS, "container", "(Ljava/lang/Object;)Lnet/minecraft/world/inventory/CraftingContainer;", false));
                if (!lower.getInternalName().equals("net/minecraft/world/inventory/CraftingContainer")) {
                    code.add(new TypeInsnNode(Opcodes.CHECKCAST, lower.getInternalName()));
                }
            }
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
        if (end == null && !carriable(rung + 1).contains("C " + lower.cls())) {
            // changed again further down (Recipe.assemble: RecipeInput -> Container at 1.20.6, Provider -> RegistryAccess
            // at 1.20.4): the bridge's call is bridged in turn when its code is converted down
            end = convertibleWalk(rung + 1, lower);
        }
        Integer access;
        if (end == null) {
            // the lower member's class isn't below either but is carried down (a 1.20.5 class whose method changed
            // between 1.21 and 1.20.6, ItemEnchantments.set(Holder, int) -> set(Enchantment, int)): the carried copy is
            // the end, in its converted names
            if (!carriable(rung + 1).contains("C " + lower.cls())) {
                return null;
            }
            Stage0.Cls lowerOwner = to.classes.get(lower.cls());
            access = lowerOwner.access.get(lower.kind() + " " + lower.name() + " " + lower.desc());
            schedule(rung + 1, new Stage0.Ref('C', lower.cls(), null, null));
            end = new Stage0.Ref(lower.kind(), CARRIED + lower.cls(), lower.name(),
                    lower.kind() == 'F' ? remapper.mapDesc(lower.desc()) : remapper.mapMethodDesc(lower.desc()));
        } else {
            Stage0.Cls bottomOwner = versions.get(versions.size() - 1).classes.get(end.cls());
            access = bottomOwner == null ? null : bottomOwner.access.get(end.kind() + " " + end.name() + " " + end.desc());
        }
        if (access == null) {
            return null;
        }
        if ((access & Opcodes.ACC_PUBLIC) == 0) {
            if ((access & Opcodes.ACC_PROTECTED) == 0 || upper.kind() != 'M') {
                return null;
            }
            // protected: a static bridge in its own package couldn't call it; overrides and super calls need none, and
            // a subclass's calls go through a private helper of its own (protectedCall)
            Type[] ua = Type.getArgumentTypes(upper.desc()), la = Type.getArgumentTypes(lower.desc());
            if (paramMap(ua, la) == null || !convertible(Type.getReturnType(upper.desc()), Type.getReturnType(lower.desc()))) {
                return null;
            }
            String[] bridge = {PROTECTED + lower.cls(), upper.name() + "$" + Integer.toHexString(upper.desc().hashCode())};
            bridgeEnds.put(bridge[0] + "." + bridge[1], end);
            return bridge;
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
        if (paramMap(ua, la) == null || !convertible(Type.getReturnType(upper.desc()), Type.getReturnType(lower.desc()))) {
            return null;
        }
        String[] bridge = {owner, upper.name() + "$" + Integer.toHexString(upper.desc().hashCode())};
        schedule(rung + 1, new Stage0.Ref('B', owner, bridge[1], job));
        bridgeEnds.put(owner + "." + bridge[1], end);
        return bridge;
    }

    /** the owner of a protected member's bridge record (no class: its calls go through helpers in the calling class) */
    static final String PROTECTED = "ladder/protected/";

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
        List<String> holderOf = holderArguments(from, upper, ua.length);
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
                // a Holder<X> becomes an X below, not any other class (new MobEffectInstance(Holder<MobEffect>) is the
                // (MobEffect) constructor, not the copy constructor (MobEffectInstance))
                String x = holderOf.get(a);
                if (ok && x != null && la[a].getSort() == Type.OBJECT && !la[a].getInternalName().equals(HOLDER)
                        && !la[a].getInternalName().equals(REFERENCE)) {
                    ok = lowerOwnerName(from, to, x).equals(la[a].getInternalName());
                }
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

    /**
     * The value class X of each Holder<X> parameter of an upper member, by parameter (null where the parameter isn't a
     * Holder or the generic signature doesn't name a class), from the member's generic signature.
     */
    static List<String> holderArguments(Stage0.Version v, Stage0.Ref ref, int count) {
        List<String> out = new ArrayList<>(Collections.nCopies(count, null));
        byte[] code = v.bytes.get(ref.cls());
        if (code == null) {
            return out;
        }
        ClassNode cn = new ClassNode();
        new ClassReader(code).accept(cn, ClassReader.SKIP_CODE);
        for (MethodNode m : cn.methods) {
            if (!m.name.equals(ref.name()) || !m.desc.equals(ref.desc()) || m.signature == null) {
                continue;
            }
            List<TypeSignature> params = new ArrayList<>();
            new org.objectweb.asm.signature.SignatureReader(m.signature).accept(new org.objectweb.asm.signature.SignatureVisitor(Opcodes.ASM9) {
                @Override
                public org.objectweb.asm.signature.SignatureVisitor visitParameterType() {
                    TypeSignature t = new TypeSignature();
                    params.add(t);
                    return t;
                }
            });
            // a signature without the synthetic parameters (an inner class's outer instance) doesn't line up
            if (params.size() == count) {
                for (int a = 0; a < count; a++) {
                    TypeSignature t = params.get(a);
                    if ((HOLDER.equals(t.cls) || REFERENCE.equals(t.cls)) && t.args.size() == 1) {
                        out.set(a, t.args.get(0).cls);
                    }
                }
            }
        }
        return out;
    }

    /** One type of a generic signature: its class (null for a type variable or an array) and its type arguments. */
    static final class TypeSignature extends org.objectweb.asm.signature.SignatureVisitor {
        String cls;
        final List<TypeSignature> args = new ArrayList<>();

        TypeSignature() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visitClassType(String name) {
            cls = name;
        }

        @Override
        public void visitInnerClassType(String name) {
            cls = cls + "$" + name;
            args.clear();
        }

        @Override
        public org.objectweb.asm.signature.SignatureVisitor visitTypeArgument(char wildcard) {
            TypeSignature a = new TypeSignature();
            args.add(a);
            return a;
        }

        @Override
        public org.objectweb.asm.signature.SignatureVisitor visitArrayType() {
            return new TypeSignature();
        }
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
            if (bridge != null) {
                newToFactory(m, call, bridge, ref);
            }
        }
    }

    /**
     * A K row's constructor call: the upper arguments go to new locals, then the lower constructor's arguments are
     * loaded, each an upper one ($i) or the value of an adapter method given all the upper ones. The object being made
     * (a NEW's, or the uninitialized this of a super call) stays under them on the stack. The new locals live only
     * here, before any later frame, so the method's frames still hold.
     */
    static void spreadArguments(MethodNode m, MethodInsnNode call, String[] rule, Stage0.Ref ref) {
        Type[] upper = Type.getArgumentTypes(call.desc);
        Type[] lower = Type.getArgumentTypes(rule[0]);
        String[] specs = rule[2].split(",");
        if (specs.length != lower.length) {
            problem("K " + ref.key() + ": " + specs.length + " parameters given for " + rule[0]);
            return;
        }
        int[] slot = new int[upper.length];
        int next = m.maxLocals;
        StringBuilder all = new StringBuilder("(");
        for (int a = 0; a < upper.length; a++) {
            slot[a] = next;
            next += upper[a].getSize();
            all.append(upper[a].getDescriptor());
        }
        all.append(')');
        // the lower arguments sit on the stack where the upper ones did, and an adapter call loads all the upper ones
        // on top of those before it: room for both (the class is written with the method's own maxStack)
        int lowerSize = 0;
        for (Type t : lower) {
            lowerSize += t.getSize();
        }
        int upperSize = next - m.maxLocals;
        m.maxStack += lowerSize + upperSize;
        m.maxLocals = next;
        InsnList code = new InsnList();
        for (int a = upper.length - 1; a >= 0; a--) {
            code.add(new VarInsnNode(upper[a].getOpcode(Opcodes.ISTORE), slot[a]));
        }
        for (int p = 0; p < lower.length; p++) {
            String spec = specs[p].trim();
            if (spec.startsWith("$")) {
                int a = Integer.parseInt(spec.substring(1));
                code.add(new VarInsnNode(upper[a].getOpcode(Opcodes.ILOAD), slot[a]));
            } else {
                for (int a = 0; a < upper.length; a++) {
                    code.add(new VarInsnNode(upper[a].getOpcode(Opcodes.ILOAD), slot[a]));
                }
                String desc = all + lower[p].getDescriptor();
                checkAdapter(rule[1], spec, desc, "K " + ref.key());
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, rule[1], spec, desc, false));
            }
        }
        m.instructions.insertBefore(call, code);
        call.desc = rule[0];
    }

    /**
     * new X(args) -> factory(args): the NEW and its DUP go, the constructor call becomes a static factory returning the
     * object (a constructor bridge, or an adapter's factory of an X rule). False, with a problem, for a super(...)
     * call or a NEW whose object isn't DUPed.
     */
    static boolean newToFactory(MethodNode m, MethodInsnNode call, String[] factory, Stage0.Ref ref) {
        {
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
                return false;
            }
            dropUninitialized(m, made);
            m.instructions.remove(made);
            m.instructions.remove(dup);
            m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, factory[0], factory[1],
                    call.desc.substring(0, call.desc.indexOf(')') + 1) + "L" + call.owner + ";", false));
            return true;
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
    static boolean bridgeCall(ClassNode node, MethodInsnNode call) {
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
        if (bridge[0].startsWith(PROTECTED)) {
            // a protected method below: only the class itself (a subclass) can call it, through a helper of its own
            MethodNode helper = inCarried || (node.access & Opcodes.ACC_INTERFACE) != 0 ? null : protectedCall(node, call, bridge, isStatic);
            if (helper == null) {
                problem("a call of the protected " + ref.key() + ", whose types changed below: no helper for it");
                return true;
            }
            call.desc = isStatic ? call.desc : "(L" + call.owner + ";" + call.desc.substring(1);
            call.setOpcode(Opcodes.INVOKESTATIC);
            call.owner = node.name;
            call.name = helper.name;
            call.itf = false;
            return true;
        }
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

    /**
     * A subclass's call of a protected method whose types changed below: a private static helper of the class (added
     * after remapping, in the bottom version's names) takes the receiver and the upper values, converts them and calls
     * the lower method.
     */
    static MethodNode protectedCall(ClassNode node, MethodInsnNode call, String[] bridge, boolean isStatic) {
        Stage0.Ref end = bridgeEnds.get(bridge[0] + "." + bridge[1]);
        if (end == null) {
            return null;
        }
        String topDesc = isStatic ? call.desc : "(L" + call.owner + ";" + call.desc.substring(1);
        String desc = remapper.mapMethodDesc(topDesc);
        Type[] all = Type.getArgumentTypes(desc), low = Type.getArgumentTypes(end.desc());
        Type[] up = isStatic ? all : Arrays.copyOfRange(all, 1, all.length);
        Type upRet = Type.getReturnType(desc), lowRet = Type.getReturnType(end.desc());
        int[] map = paramMap(up, low);
        if (map == null || !convertible(upRet, lowRet)) {
            return null;
        }
        String name = "ladder$call$" + call.name + "$" + Integer.toHexString(topDesc.hashCode());
        List<MethodNode> pending = addAfterRemap.computeIfAbsent(node.name, k -> new ArrayList<>());
        for (MethodNode done : pending) {
            if (done.name.equals(name)) {
                return done;
            }
        }
        MethodNode g = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, name, desc, null, null);
        InsnList code = g.instructions;
        int slot = 0;
        if (!isStatic) {
            code.add(new VarInsnNode(Opcodes.ALOAD, 0));
            slot = 1;
        }
        for (int a = 0; a < up.length; a++) {
            if (map[a] >= 0) {
                code.add(new VarInsnNode(up[a].getOpcode(Opcodes.ILOAD), slot));
                convertDown(code, up[a], low[map[a]]);
            } else if (map[a] <= -2) {
                unpackColor(code, slot);
            }
            slot += up[a].getSize();
        }
        code.add(new MethodInsnNode(isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL, isStatic ? end.cls() : remapper.map(call.owner),
                end.name(), end.desc(), false));
        convertUp(code, lowRet, upRet);
        code.add(new InsnNode(upRet.getOpcode(Opcodes.IRETURN)));
        g.maxLocals = slot;
        g.maxStack = slot + 6;
        pending.add(g);
        return g;
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
        int[] map = paramMap(up, low);
        if (map == null || !convertible(upRet, lowRet)) {
            return null;
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
            if (map[a] >= 0) {
                code.add(new VarInsnNode(up[a].getOpcode(Opcodes.ILOAD), slot));
                convertDown(code, up[a], low[map[a]]);
            } else if (map[a] <= -2) {
                unpackColor(code, slot);
            }
            slot += up[a].getSize();
        }
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, remapper.map(call.owner), end.name(), end.desc(), false));
        convertUp(code, lowRet, upRet);
        code.add(new InsnNode(upRet.getOpcode(Opcodes.IRETURN)));
        g.maxLocals = slot;
        g.maxStack = slot + 6;
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
            if (bridge[0].startsWith(PROTECTED)) {
                problem("a method reference to the protected " + h.getOwner() + "." + h.getName() + h.getDesc() + ", whose types changed below");
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
        int[] map = paramMap(up, low);
        if (map == null || !convertible(upRet, lowRet)) {
            return null;
        }
        int access = Opcodes.ACC_PUBLIC;
        Stage0.Cls bottomOwner = versions.get(versions.size() - 1).classes.get(end.cls());
        Integer lowerAccess = bottomOwner == null ? null : bottomOwner.access.get("M " + end.name() + " " + end.desc());
        if (lowerAccess != null && (lowerAccess & Opcodes.ACC_PROTECTED) != 0) {
            // an override of a protected method stays protected (the mod's own may be protected too)
            access = Opcodes.ACC_PROTECTED;
        }
        MethodNode g = new MethodNode(access, end.name(), end.desc(), null, null);
        InsnList code = g.instructions;
        code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int[] slots = new int[low.length];
        int slot = 1;
        for (int a = 0; a < low.length; a++) {
            slots[a] = slot;
            slot += low[a].getSize();
        }
        Type[] wanted = castTypes(m, up);
        for (int a = 0; a < up.length; a++) {
            if (map[a] >= 0) {
                code.add(new VarInsnNode(low[map[a]].getOpcode(Opcodes.ILOAD), slots[map[a]]));
                // (a javac bridge's erased recipe input: made of the type the method casts it to)
                convertUp(code, low[map[a]], wanted[a] != null ? wanted[a] : up[a]);
            } else if (map[a] <= -2) {
                packColor(code, slots, -map[a] - 2);
            } else {
                supplyContext(code, up[a]);
            }
        }
        code.add(new MethodInsnNode(itf ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, remapper.map(node.name), ownName, ownDesc, itf));
        convertDown(code, upRet, lowRet);
        code.add(new InsnNode(lowRet.getOpcode(Opcodes.IRETURN)));
        g.maxLocals = slot;
        g.maxStack = slot + 2 + 2 * up.length;
        return g;
    }

    /**
     * For each parameter of a recipe input type converted from a container: the more specific type a javac bridge
     * method casts it to first (a mod's {@code Recipe<CraftingInput>} gets matches(RecipeInput, Level) casting to
     * CraftingInput), in the remapped names; null where there is none.
     */
    static Type[] castTypes(MethodNode m, Type[] up) {
        Type[] wanted = new Type[up.length];
        if ((m.access & Opcodes.ACC_BRIDGE) == 0) {
            return wanted;
        }
        int slot = 1;
        for (int a = 0; a < up.length; a++) {
            if (up[a].getSort() == Type.OBJECT && RECIPE_INPUTS.contains(up[a].getInternalName())) {
                for (AbstractInsnNode insn = m.instructions.getFirst(); insn != null; insn = insn.getNext()) {
                    if (insn instanceof VarInsnNode load && load.getOpcode() == Opcodes.ALOAD && load.var == slot
                            && load.getNext() instanceof TypeInsnNode cast && cast.getOpcode() == Opcodes.CHECKCAST) {
                        String to = remapper.map(cast.desc);
                        if (!to.equals(up[a].getInternalName())) {
                            wanted[a] = Type.getObjectType(to);
                        }
                        break;
                    }
                }
            }
            slot += up[a].getSize();
        }
        return wanted;
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
            int[] map = paramMap(ua, la);
            StringBuilder b = new StringBuilder("(");
            if (!lowerStatic) {
                b.append("L").append(lowerOwner).append(";");
                code.add(new VarInsnNode(Opcodes.ALOAD, 0));
                locals = 1;
            }
            for (int a = 0; a < ua.length; a++) {
                b.append(ua[a].getDescriptor());
                if (map[a] >= 0) {
                    code.add(new VarInsnNode(ua[a].getOpcode(Opcodes.ILOAD), locals));
                    stack += ua[a].getSize();
                    convertDown(code, ua[a], la[map[a]]);
                } else if (map[a] <= -2) {
                    unpackColor(code, locals);
                    stack += 4;
                }
                locals += ua[a].getSize();
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
            case 10 -> {
                // the input of the type the recipe takes (a javac bridge's erased RecipeInput: the type it casts to)
                // (named as carried: the bridges' code is lower-version code, where the input classes are the carried ones)
                String input = upper.getInternalName().startsWith(CARRIED) ? upper.getInternalName() : CARRIED + upper.getInternalName();
                code.add(new LdcInsnNode(Type.getObjectType(input)));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RECIPE_INPUT_CONVERSIONS, "recipeInput", "(L" + CONTAINER + ";Ljava/lang/Class;)Ljava/lang/Object;", false));
                code.add(new TypeInsnNode(Opcodes.CHECKCAST, input));
            }
            default -> {
                // the same type, or a subtype (RegistryAccess is a HolderLookup.Provider)
            }
        }
    }

    /**
     * A bridge's code is code of its rung, but its values may be of a type only the versions above have (a 1.21 recipe
     * input in the bridge of a 1.21 -> 1.20.6 change): such types are the carried classes (carried from where they are).
     */
    static ClassNode upperTypesCarried(ClassNode holder) {
        Stage0.Version here = versions.get(level);
        ClassNode out = new ClassNode();
        holder.accept(new ClassRemapper(out, new org.objectweb.asm.commons.Remapper() {
            @Override
            public String map(String internal) {
                if (internal.startsWith(CARRIED) || !Stage0.platform(internal) || here.classes.containsKey(internal)) {
                    return internal;
                }
                for (int i = level - 1; i >= 0; i--) {
                    if (versions.get(i).classes.containsKey(internal)) {
                        schedule(i, new Stage0.Ref('C', internal, null, null));
                        return CARRIED + internal;
                    }
                }
                return internal;
            }
        }));
        return out;
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
                    holder = upperTypesCarried(holder);
                    redirectCarried(holder);
                    ClassNode converted = new ClassNode();
                    holder.accept(new ClassRemapper(converted, remapper));
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
                    applyRules(source, false);
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
                if ((method.access & Opcodes.ACC_STATIC) == 0) {
                    // an instance method as a static helper: its this (local 0) is the first parameter now
                    method.desc = "(L" + thing.cls() + ";" + method.desc.substring(1);
                    method.signature = null;
                }
                method.access = (method.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED | Opcodes.ACC_ABSTRACT | Opcodes.ACC_FINAL
                        | Opcodes.ACC_SYNCHRONIZED)) | Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
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
                applyRules(holder, false);
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

    /** The game classes of the top and bottom versions, parsed once (the mixin checks and repairs read them). */
    static final Map<String, Optional<ClassNode>> topClasses = new HashMap<>(), bottomClasses = new HashMap<>();
    static MixinRepair mixinRepair;

    static ClassNode topClass(String name) {
        return parsed(topClasses, versions.get(0), name);
    }

    static ClassNode bottomClass(String name) {
        return parsed(bottomClasses, versions.get(versions.size() - 1), name);
    }

    static ClassNode parsed(Map<String, Optional<ClassNode>> cache, Stage0.Version v, String name) {
        return cache.computeIfAbsent(name, n -> {
            byte[] b = v.bytes.get(n);
            if (b == null) {
                return Optional.empty();
            }
            ClassNode c = new ClassNode();
            new ClassReader(b).accept(c, ClassReader.EXPAND_FRAMES);
            return Optional.of(c);
        }).orElse(null);
    }

    /**
     * A mixin's injectors must land below where they land above: the converted mixin is checked against the bottom
     * game's method bodies; when a point moved, Sinytra Adapter's patches (MixinRepair) adapt the mixin and it is
     * checked again. Each injector that lands above and still not below is a problem. Returns the class to write (the
     * patched one, or the converted one as it was).
     */
    static ClassNode checkMixinPoints(String mixin, Map<String, String> above, ClassNode converted, Map<String, byte[]> modClasses) {
        Map<String, List<String>> deferred = deferredMixin.getOrDefault(mixin, Map.of());
        deferredMixin.remove(mixin);
        Map<String, String> below = new HashMap<>(MixinPoints.check(converted, mixinTargetsOf(converted), Convert::bottomClass));
        Set<String> moved = new TreeSet<>();
        above.forEach((handler, verdict) -> {
            String lower = below.containsKey(handler) ? handler : "ladder$wrap$" + handler;
            if (verdict == null && below.containsKey(lower) && below.get(lower) != null) {
                moved.add(handler);
            }
        });
        ClassNode result = converted;
        String repairError = null;
        if (!moved.isEmpty() && !"false".equals(System.getProperty("ladder.mixinRepair"))) {
            if (mixinRepair == null) {
                mixinRepair = new MixinRepair(Convert::topClass, Convert::bottomClass);
            }
            // the patches edit a copy with full frames; the converted class stays as it was when they fail
            ClassWriter w = new ClassWriter(0);
            converted.accept(w);
            ClassNode copy = new ClassNode();
            new ClassReader(w.toByteArray()).accept(copy, ClassReader.EXPAND_FRAMES);
            int before = mixinRepair.generated().size();
            repairError = mixinRepair.patch(copy, modClasses, Convert::bottomClass);
            if (repairError == null && mixinRepair.generated().size() != before) {
                repairError = "the patch split an injector into a generated mixin class (not placed yet)";
            }
            if (repairError == null) {
                result = copy;
                below.putAll(MixinPoints.check(copy, mixinTargetsOf(copy), Convert::bottomClass));
            }
        }
        Set<String> handlers = new TreeSet<>(above.keySet());
        handlers.addAll(deferred.keySet());
        for (String h : handlers) {
            // a handler bridge carries the injector below under its own name (ladder$wrap$<handler>)
            String lower = below.containsKey(h) ? h : below.containsKey("ladder$wrap$" + h) ? "ladder$wrap$" + h : h;
            boolean landsAbove = above.containsKey(h) && above.get(h) == null;
            boolean landsBelow = below.containsKey(lower) && below.get(lower) == null;
            if (landsAbove && landsBelow) {
                // what its selectors raised is settled: it lands below as it does above
                continue;
            }
            if (MixinPoints.INACTIVE.equals(above.get(h))) {
                // a mixin for another loader or version: it applies to nothing above, and must apply to nothing below
                if (MixinPoints.INACTIVE.equals(below.get(lower))) {
                    continue;
                }
                report("mixin applies below only: its target classes aren't in the upper game but are in the bottom one ("
                        + mixin + "." + h + ")");
                continue;
            }
            deferred.getOrDefault(h, List.of()).forEach(Convert::report);
            if (landsAbove) {
                report((MixinPoints.INACTIVE.equals(below.get(lower)) ? "mixin into a class the bottom game hasn't" : "mixin injection point moved below: " + below.get(lower)) + " (" + mixin + "." + h + ")"
                        + (repairError == null ? "" : "; repair failed: " + repairError));
            }
        }
        return result;
    }

    /** The string selectors of a mixin's injectors (method = ..., @At(target = ...)) in the bottom version's names. */
    static void remapMixinSelectors(ClassNode node, Remapper remapper) {
        List<String> targets = mixinTargets.get(node.name);
        for (MethodNode m : node.methods) {
            for (List<AnnotationNode> list : Arrays.asList(m.visibleAnnotations, m.invisibleAnnotations)) {
                if (list != null) {
                    for (AnnotationNode a : list) {
                        currentHandler = new String[] {node.name, m.name, m.desc, a.desc};
                        boolean injector = MixinPoints.INJECTORS.contains(a.desc);
                        if (injector) {
                            deferring = new ArrayList<>();
                        }
                        try {
                            remapAnnotation(a, targets, remapper, node.name + "." + m.name);
                        } finally {
                            currentHandler = null;
                            if (injector) {
                                List<String> held = deferring;
                                deferring = null;
                                if (!held.isEmpty()) {
                                    deferredMixin.computeIfAbsent(node.name, k -> new HashMap<>()).computeIfAbsent(MixinPoints.key(node, m), k -> new ArrayList<>()).addAll(held);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /** the mixin handler whose annotation is being remapped: class, name, desc, annotation */
    static String[] currentHandler;

    /**
     * A mixin handler whose target's (or target call's) types changed below: the selector goes to the lower member; the
     * handler, when its parameters hold the changed types, gets a wrapper of the lower types that converts them and
     * calls it (added once the class is remapped). {@code call}: an @At target (the handler's parameters are a call's).
     */
    record HandlerFix(String cls, String method, String desc, String annotation, String upperDesc, Stage0.Ref end, boolean call) {
    }

    static final List<HandlerFix> handlerFixes = new ArrayList<>();

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
        Set<String> qualified = new TreeSet<>();
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
                String[] bridged = bridgeOf(o, mName, mDesc, true);
                if (bridged != null) {
                    // its types changed below: the lower method is the target; the handler's parameters are converted
                    Stage0.Ref end = bridgeEnds.get(bridged[0] + "." + bridged[1]);
                    if (end == null || currentHandler == null) {
                        problem("mixin into " + o + "." + mName + mDesc + ", whose types changed below: no handler bridge (" + where + ")");
                        results.add(mName + (desc != null ? mDesc : ""));
                        continue;
                    }
                    handlerFixes.add(new HandlerFix(currentHandler[0], currentHandler[1], currentHandler[2], currentHandler[3],
                            remapper.mapMethodDesc(mDesc), end, false));
                    results.add(end.name() + end.desc());
                    continue;
                }
                String mapped = mapMember(o, mName, mDesc, true);
                results.add(mapped + (desc != null ? remapper.mapMethodDesc(mDesc) : ""));
                // a name-only selector that names one method above: the descriptor it meant goes along, so a method
                // whose parameters changed below is found missing (and repaired) instead of matching the lower one
                qualified.add(mapped + remapper.mapMethodDesc(mDesc));
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
        String first = desc == null && results.size() == 1 && qualified.size() == 1 ? qualified.iterator().next() : results.iterator().next();
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
            String[] bridged = bridgeOf(owner, name, desc, !field);
            if (bridged != null) {
                // the game's own code below calls (or reads) the lower member: that is the target
                Stage0.Ref end = bridgeEnds.get(bridged[0] + "." + bridged[1]);
                if (end == null || field || currentHandler == null) {
                    problem("mixin target " + target + ", whose types changed below: no target bridge (" + where + ")");
                    return target;
                }
                handlerFixes.add(new HandlerFix(currentHandler[0], currentHandler[1], currentHandler[2], currentHandler[3],
                        remapper.mapMethodDesc(desc), end, true));
                return "L" + remapper.map(owner) + ";" + end.name() + end.desc();
            }
            newName = finalName(owner, name, desc, !field);
            if (newName == null) {
                problem("mixin target " + target + " matches nothing in " + versions.get(0).id + " (" + where + ")");
                return target;
            }
        }
        String newDesc = field ? remapper.mapDesc(desc) : remapper.mapMethodDesc(desc);
        return "L" + remapper.map(owner) + ";" + newName + (field ? ":" : "") + newDesc;
    }

    /**
     * The handlers of a remapped mixin class whose targets' types changed below. A handler that doesn't hold the
     * changed types needs nothing more (its selector was moved). An @Inject into a changed method whose parameters
     * begin with the target's: a wrapper of the lower types takes the annotations, converts the values up and calls the
     * handler (which keeps its name and descriptor, so the mixin's own calls of it still link). The rest are problems.
     */
    static void applyHandlerFixes(String original, ClassNode out) {
        List<HandlerFix> fixes = new ArrayList<>();
        for (Iterator<HandlerFix> it = handlerFixes.iterator(); it.hasNext();) {
            HandlerFix f = it.next();
            if (f.cls().equals(original)) {
                fixes.add(f);
                it.remove();
            }
        }
        Map<String, List<HandlerFix>> byHandler = new LinkedHashMap<>();
        for (HandlerFix f : fixes) {
            byHandler.computeIfAbsent(f.method() + f.desc(), k -> new ArrayList<>()).add(f);
        }
        for (List<HandlerFix> list : byHandler.values()) {
            HandlerFix f = list.get(0);
            String where = original + "." + f.method();
            String hDesc = remapper.mapMethodDesc(f.desc());
            MethodNode h = out.methods.stream().filter(m -> m.name.equals(f.method()) && m.desc.equals(hDesc)).findFirst().orElse(null);
            Type[] up = Type.getArgumentTypes(f.upperDesc()), low = Type.getArgumentTypes(f.end().desc());
            Type[] hp = h == null ? new Type[0] : Type.getArgumentTypes(h.desc);
            int[] map = paramMap(up, low);
            Set<Type> changed = new HashSet<>();
            for (int i = 0; i < up.length; i++) {
                if (map == null || map[i] < 0 || !up[i].equals(low[map[i]])) {
                    changed.add(up[i]);
                }
            }
            boolean returnChanged = !Type.getReturnType(f.upperDesc()).equals(Type.getReturnType(f.end().desc()));
            boolean holdsChanged = Arrays.stream(hp).anyMatch(changed::contains);
            boolean returns = Arrays.stream(hp).anyMatch(t -> t.getInternalName().endsWith("CallbackInfoReturnable"));
            if (h == null) {
                problem("mixin handler " + where + " not found after remapping");
                continue;
            }
            if (!holdsChanged && !(f.call() && returnChanged && Type.getReturnType(h.desc).equals(Type.getReturnType(f.upperDesc())))
                    && !(!f.call() && returnChanged && returns)) {
                // the handler doesn't see the changed types: the moved selector is all it needs
                continue;
            }
            boolean inject = f.annotation().equals("Lorg/spongepowered/asm/mixin/injection/Inject;");
            boolean prefix = hp.length >= up.length && Arrays.equals(Arrays.copyOf(hp, up.length), up);
            boolean distinct = list.stream().allMatch(x -> x.upperDesc().equals(f.upperDesc()) && x.end().equals(f.end()) && x.call() == f.call());
            if (f.call() || !inject || !prefix || returnChanged || !distinct) {
                problem("mixin handler " + where + " takes types that changed below (" + f.upperDesc() + " -> " + f.end().desc()
                        + "): no handler bridge for it");
                continue;
            }
            if (map == null) {
                problem("mixin handler " + where + ": the target's types changed below without a known conversion");
                continue;
            }
            // the lower target's parameters, then the handler's own (CallbackInfo, locals)
            Type[] wrapped = new Type[low.length + hp.length - up.length];
            System.arraycopy(low, 0, wrapped, 0, low.length);
            System.arraycopy(hp, up.length, wrapped, low.length, hp.length - up.length);
            // the wrapper gets a name of its own (the injector's annotation picks the target, not the name); the
            // handler keeps its name and descriptor, so the mixin's own calls of it still link
            MethodNode g = new MethodNode(h.access, "ladder$wrap$" + h.name, Type.getMethodDescriptor(Type.getReturnType(h.desc), wrapped), null,
                    h.exceptions.toArray(new String[0]));
            g.visibleAnnotations = h.visibleAnnotations;
            g.invisibleAnnotations = h.invisibleAnnotations;
            g.visibleParameterAnnotations = h.visibleParameterAnnotations;
            g.invisibleParameterAnnotations = h.invisibleParameterAnnotations;
            h.visibleAnnotations = null;
            h.invisibleAnnotations = null;
            h.visibleParameterAnnotations = null;
            h.invisibleParameterAnnotations = null;
            boolean isStatic = (h.access & Opcodes.ACC_STATIC) != 0;
            InsnList code = g.instructions;
            int slot = 0;
            if (!isStatic) {
                code.add(new VarInsnNode(Opcodes.ALOAD, 0));
                slot = 1;
            }
            int[] slots = new int[wrapped.length];
            for (int i = 0; i < wrapped.length; i++) {
                slots[i] = slot;
                slot += wrapped[i].getSize();
            }
            for (int i = 0; i < up.length; i++) {
                if (map[i] >= 0) {
                    code.add(new VarInsnNode(low[map[i]].getOpcode(Opcodes.ILOAD), slots[map[i]]));
                    convertUp(code, low[map[i]], up[i]);
                } else {
                    supplyContext(code, up[i]);
                }
            }
            for (int i = low.length; i < wrapped.length; i++) {
                code.add(new VarInsnNode(wrapped[i].getOpcode(Opcodes.ILOAD), slots[i]));
            }
            boolean privateHandler = (h.access & Opcodes.ACC_PRIVATE) != 0;
            code.add(new MethodInsnNode(isStatic ? Opcodes.INVOKESTATIC : privateHandler ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL, out.name,
                    h.name, h.desc, false));
            code.add(new InsnNode(Type.getReturnType(h.desc).getOpcode(Opcodes.IRETURN)));
            g.maxLocals = slot;
            g.maxStack = slot + 2 + 2 * up.length;
            out.methods.add(g);
        }
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
