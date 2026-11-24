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
    /**
     * The package of carried code: each target its own (a conversion to 1.21 carries 1.21.1's classes; one further
     * down carries that rung's own and converts the upper rung's carried jar like any mod). -Dladder.to names the target.
     */
    static final String CARRIED = carriedPrefix(System.getProperty("ladder.to", "1.20.4"));
    /**
     * The package of the target rung's adapters: the classes the converter's own bridges and generated code call
     * (Conversions, RecipeInputs, Packs, ModEntries, DeltaTracker...) are the target rung's, at the same places under it.
     */
    static final String ADAPTERS = adaptersPrefix(System.getProperty("ladder.to", "1.20.4"));

    static String adaptersPrefix(String target) {
        return switch (target) {
            case "1.21" -> "ladder/rung121/";
            case "1.20.6" -> "ladder/rung1206/";
            default -> "ladder/adapter/";
        };
    }

    static String carriedPrefix(String target) {
        return target.equals("1.20.4") ? "ladder/carried/" : "ladder/carried_" + target.replace('.', '_') + "/";
    }

    /** whether this conversion goes down the step from upper to lower (a rung's own changes apply only then) */
    static boolean crosses(String upper, String lower) {
        for (int i = 0; i + 1 < versions.size(); i++) {
            if (versions.get(i).id.equals(upper) && versions.get(i + 1).id.equals(lower)) {
                return true;
            }
        }
        return false;
    }
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
    /** a carried class as a mixin into it sees it (bottomClass) */
    static final Map<String, Optional<ClassNode>> carriedViews = new HashMap<>();
    static final Deque<Object[]> carryQueue = new ArrayDeque<>();
    static final Set<String> carryScheduled = new HashSet<>();
    /** walk key -> the companion class a carried static member moved to (its call sites move there too) */
    static final Map<String, String> staticRedirect = new HashMap<>();
    /** converting carried code: its calls must reach public members below */
    static boolean inCarried;
    static Remapper remapper;
    /** In a rule's lower descriptor: the bottom version's class of that name, not the top's (see the remapper) */
    static final String BOTTOM = "ladder/bottom/";

    /** NeoForge classes renamed below (no intermediary for NeoForge): the rungs' R rows. */
    static final Map<String, String> NEOFORGE_MOVES = new HashMap<>();

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
    /** -Dladder.trace=<text>: where a problem containing the text comes from (a stack trace on stderr) */
    static final String TRACE = System.getProperty("ladder.trace");

    static void report(String text) {
        if (TRACE != null && text.contains(TRACE)) {
            new Throwable("ladder.trace: " + text + (deferring != null ? " (deferred)" : "")).printStackTrace();
        }
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
                    if (!resolvable(rung, h) && !bootstrapNeed(k, h)) {
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
                }
                if (!problems.isEmpty() && !Boolean.getBoolean("ladder.keep") || Boolean.getBoolean("ladder.discard")) {
                    // (-Dladder.discard: an offline check keeps the report only, the disk fills fast; -Dladder.keep: a
                    // jar with problems stays, to look at)
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

    static final String SUBSCRIBER = "Lnet/neoforged/fml/common/EventBusSubscriber;";

    /**
     * NeoForge 21's {@code @EventBusSubscriber} with no bus puts each listener on the bus of its event (a mod bus event on
     * the mod's bus); FML 2's puts all of them on the game's bus, where a mod bus event is refused. A class whose events
     * are all the mod bus's gets bus = MOD; one with events of both buses can't be one class on FML 2.
     */
    static void subscriberBus(ClassNode node) {
        if (node.visibleAnnotations == null) {
            return;
        }
        for (AnnotationNode a : node.visibleAnnotations) {
            if (!a.desc.equals(SUBSCRIBER) || a.values != null && a.values.contains("bus")) {
                continue;
            }
            int mod = 0, game = 0;
            for (MethodNode m : node.methods) {
                if ((m.access & Opcodes.ACC_STATIC) == 0 || m.visibleAnnotations == null
                        || m.visibleAnnotations.stream().noneMatch(x -> x.desc.equals("Lnet/neoforged/bus/api/SubscribeEvent;"))) {
                    continue;
                }
                Type[] params = Type.getArgumentTypes(m.desc);
                if (params.length != 1 || params[0].getSort() != Type.OBJECT) {
                    continue;
                }
                if (modBusEvent(params[0].getInternalName())) {
                    mod++;
                } else {
                    game++;
                }
            }
            if (mod > 0 && game == 0) {
                if (a.values == null) {
                    a.values = new ArrayList<>();
                }
                a.values.add("bus");
                a.values.add(new String[] {"Lnet/neoforged/fml/common/EventBusSubscriber$Bus;", "MOD"});
            } else if (mod > 0) {
                // (splitSubscribers moved the mod bus's listeners into a class of their own before: none should be left)
                problem("an event subscriber with events of both buses (FML 2 puts a class on one): " + node.name);
            }
        }
    }

    /**
     * An {@code @EventBusSubscriber} with no bus whose listeners take events of both buses: NeoForge 21.1 puts each on its
     * event's bus, FML 4.0.23 (1.21) and older put the whole class on one. The mod bus's listeners move to a generated
     * subscriber of their own ({@code <class>$LadderModBus}, bus = MOD, the same mod id and sides), static methods that
     * call them; the class keeps the game bus's. The new class is converted like the mod's own.
     */
    static void splitSubscribers(Map<String, byte[]> entries) {
        Map<String, byte[]> added = new LinkedHashMap<>();
        for (var e : entries.entrySet()) {
            if (!e.getKey().endsWith(".class") || e.getKey().startsWith("META-INF/")) {
                continue;
            }
            ClassNode node = new ClassNode();
            new ClassReader(e.getValue()).accept(node, 0);
            AnnotationNode subscriber = node.visibleAnnotations == null ? null
                    : node.visibleAnnotations.stream().filter(a -> a.desc.equals(SUBSCRIBER)).findFirst().orElse(null);
            if (subscriber == null || subscriber.values != null && subscriber.values.contains("bus")) {
                continue;
            }
            List<MethodNode> modListeners = new ArrayList<>();
            boolean game = false;
            for (MethodNode m : node.methods) {
                if ((m.access & Opcodes.ACC_STATIC) == 0 || m.visibleAnnotations == null
                        || m.visibleAnnotations.stream().noneMatch(x -> x.desc.equals("Lnet/neoforged/bus/api/SubscribeEvent;"))) {
                    continue;
                }
                Type[] params = Type.getArgumentTypes(m.desc);
                if (params.length != 1 || params[0].getSort() != Type.OBJECT) {
                    continue;
                }
                if (modBusEvent(params[0].getInternalName())) {
                    modListeners.add(m);
                } else {
                    game = true;
                }
            }
            if (modListeners.isEmpty() || !game) {
                continue;
            }
            String companion = node.name + "$LadderModBus";
            ClassNode split = new ClassNode();
            split.visit(Opcodes.V21, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, companion, null, "java/lang/Object", null);
            AnnotationNode copy = new AnnotationNode(SUBSCRIBER);
            copy.values = new ArrayList<>(subscriber.values == null ? List.of() : subscriber.values);
            copy.values.add("bus");
            copy.values.add(new String[] {"Lnet/neoforged/fml/common/EventBusSubscriber$Bus;", "MOD"});
            split.visibleAnnotations = new ArrayList<>(List.of(copy));
            for (MethodNode m : modListeners) {
                AnnotationNode listen = m.visibleAnnotations.stream().filter(x -> x.desc.equals("Lnet/neoforged/bus/api/SubscribeEvent;")).findFirst().get();
                m.visibleAnnotations.remove(listen);
                // the companion calls it: no longer private
                m.access = (m.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
                MethodNode f = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, m.name, m.desc, m.signature, null);
                f.visibleAnnotations = new ArrayList<>(List.of(listen));
                f.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                f.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, node.name, m.name, m.desc, (node.access & Opcodes.ACC_INTERFACE) != 0));
                f.instructions.add(new InsnNode(Opcodes.RETURN));
                f.maxStack = 1;
                f.maxLocals = 1;
                split.methods.add(f);
            }
            ClassWriter w = new ClassWriter(0);
            node.accept(w);
            e.setValue(w.toByteArray());
            ClassWriter sw = new ClassWriter(0);
            split.accept(sw);
            added.put(companion + ".class", sw.toByteArray());
        }
        entries.putAll(added);
    }

    /** a class of the data generators' API (net.minecraft.data, NeoForge's data events and providers) */
    static boolean datagenApi(String cls) {
        return cls.startsWith("net/minecraft/data/") || cls.startsWith("net/neoforged/neoforge/data/")
                || cls.startsWith("net/neoforged/neoforge/common/data/");
    }

    /** whether an event class is a mod bus event (IModBusEvent among its supertypes, in the mod's version or provided) */
    static boolean modBusEvent(String event) {
        String provided = Convert.provided.get(event);
        if (provided != null) {
            return adapterSupers.getOrDefault(provided, List.of()).contains("net/neoforged/fml/event/IModBusEvent");
        }
        return subtypeIn(versions.get(level), event, "net/neoforged/fml/event/IModBusEvent");
    }

    /** reflection by name: the calls whose class and member name are constants just before them */
    static final Set<String> REFLECTION = Set.of(
            "net/neoforged/fml/util/ObfuscationReflectionHelper.findMethod", "net/neoforged/fml/util/ObfuscationReflectionHelper.findField",
            "net/neoforged/fml/util/ObfuscationReflectionHelper.getPrivateValue", "net/neoforged/fml/util/ObfuscationReflectionHelper.setPrivateValue",
            "java/lang/Class.getDeclaredMethod", "java/lang/Class.getMethod", "java/lang/Class.getDeclaredField", "java/lang/Class.getField");

    /**
     * A mod that reaches a game or loader member by reflection (its class and name as constants) gets past the walk:
     * when the bottom version has no member of that name in that class (or its supertypes), it fails when it runs
     * (NoSuchMethodException), so it is a problem like a missing reference.
     */
    static void checkReflection(ClassNode node) {
        Stage0.Version bottom = versions.get(versions.size() - 1);
        for (MethodNode m : node.methods) {
            String owner = null, name = null;
            for (AbstractInsnNode insn : m.instructions) {
                if (insn instanceof LdcInsnNode ldc) {
                    if (ldc.cst instanceof Type t && t.getSort() == Type.OBJECT) {
                        // the class reflected on comes before its member's name; the parameter types after it
                        if (name == null) {
                            owner = t.getInternalName();
                        }
                    } else if (ldc.cst instanceof String text && owner != null && name == null) {
                        name = text;
                    }
                } else if (insn instanceof MethodInsnNode call && REFLECTION.contains(call.owner + "." + call.name)) {
                    // a lookup in a try that catches its failure is the mod's own fallback (a mod for several versions)
                    if (owner != null && name != null && (Stage0.platform(owner) || owner.startsWith("net/neoforged/fml/")) && !caught(m, call)) {
                        String lower = lowerOwnerName(versions.get(level), bottom, owner);
                        // a member the mod's own version hasn't either is the mod's dead path there too (it fails the same natively)
                        if (bottom.classes.containsKey(lower) && !hasMemberNamed(bottom, lower, name) && hasMemberNamed(versions.get(level), owner, name)) {
                            problem("reflection on " + owner + "." + name + ", which the bottom version hasn't (" + node.name + ")");
                        }
                    }
                    owner = null;
                    name = null;
                }
            }
        }
    }

    /** the failures of a reflective lookup a try block can catch */
    static final Set<String> LOOKUP_FAILURES = Set.of("java/lang/NoSuchMethodException", "java/lang/NoSuchFieldException",
            "java/lang/ReflectiveOperationException", "java/lang/Exception", "java/lang/Throwable", "java/lang/RuntimeException",
            "net/neoforged/fml/util/ObfuscationReflectionHelper$UnableToFindMethodException",
            "net/neoforged/fml/util/ObfuscationReflectionHelper$UnableToFindFieldException",
            "net/neoforged/fml/util/ObfuscationReflectionHelper$UnableToAccessFieldException");

    static boolean caught(MethodNode m, AbstractInsnNode at) {
        int index = m.instructions.indexOf(at);
        for (TryCatchBlockNode block : m.tryCatchBlocks) {
            if (m.instructions.indexOf(block.start) <= index && index < m.instructions.indexOf(block.end)
                    && block.type != null && LOOKUP_FAILURES.contains(block.type)) {
                return true;
            }
        }
        return false;
    }

    static boolean hasMemberNamed(Stage0.Version v, String cls, String name) {
        for (String c = cls; c != null; ) {
            Stage0.Cls info = v.classes.get(c);
            if (info == null) {
                // a supertype outside the platform (java/lang/Object...): its members aren't the game's
                return c.equals(cls) || !Stage0.platform(c) && !c.startsWith("java/lang/Object");
            }
            for (String member : info.methods) {
                if (member.startsWith(name + " ")) {
                    return true;
                }
            }
            for (String field : info.fields) {
                if (field.startsWith(name + " ")) {
                    return true;
                }
            }
            c = info.superName;
        }
        return false;
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

    static final String BOOTSTRAP_CONTEXT = "Lnet/minecraft/data/worldgen/BootstrapContext;";

    /**
     * A registry's bootstrap (a method of the game's BootstrapContext: 1.21's enchantments, worldgen and damage types built
     * in code for a data pack) runs only in data generation, never when the game plays: its body, and those of the lambdas
     * it makes, become a throw (noted as unverified), so what it names needn't come down.
     */
    static void stubBootstraps(ClassNode node) {
        Set<String> stub = new LinkedHashSet<>();
        for (MethodNode m : node.methods) {
            if (m.desc.contains(BOOTSTRAP_CONTEXT) && m.instructions.size() > 0) {
                stub.add(m.name + m.desc);
            }
        }
        if (stub.isEmpty()) {
            return;
        }
        // the lambdas a bootstrap makes (their bodies run only from it)
        for (boolean grew = true; grew;) {
            grew = false;
            for (MethodNode m : node.methods) {
                if (!stub.contains(m.name + m.desc)) {
                    continue;
                }
                for (AbstractInsnNode insn : m.instructions) {
                    if (insn instanceof InvokeDynamicInsnNode indy) {
                        for (Object arg : indy.bsmArgs) {
                            if (arg instanceof Handle h && h.getOwner().equals(node.name) && h.getName().startsWith("lambda$")
                                    && stub.add(h.getName() + h.getDesc())) {
                                grew = true;
                            }
                        }
                    }
                }
            }
        }
        for (MethodNode m : node.methods) {
            if (!stub.contains(m.name + m.desc) || m.instructions.size() == 0) {
                continue;
            }
            InsnList code = new InsnList();
            code.add(new TypeInsnNode(Opcodes.NEW, "java/lang/UnsupportedOperationException"));
            code.add(new InsnNode(Opcodes.DUP));
            code.add(new LdcInsnNode("a registry bootstrap of data generation (the ladder's converted mod runs without it)"));
            code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "(Ljava/lang/String;)V", false));
            code.add(new InsnNode(Opcodes.ATHROW));
            m.instructions = code;
            m.tryCatchBlocks = new ArrayList<>();
            m.localVariables = null;
            m.visibleLocalVariableAnnotations = null;
            m.invisibleLocalVariableAnnotations = null;
            m.maxStack = 3;
            m.maxLocals = Type.getArgumentsAndReturnSizes(m.desc) >> 2;
            m.signature = null;
            if ((m.access & Opcodes.ACC_STATIC) != 0 && ((m.access & Opcodes.ACC_PRIVATE) != 0 || m.name.startsWith("lambda$"))) {
                // (a private helper of a bootstrap, its lambdas: called only from stubbed code, so the types it names that
                // only data generation has, 1.21's Enchantment.Builder, needn't come down)
                Type[] args = Type.getArgumentTypes(m.desc);
                for (int a = 0; a < args.length; a++) {
                    if (args[a].getSort() == Type.OBJECT && !args[a].getDescriptor().equals(BOOTSTRAP_CONTEXT) && Stage0.platform(args[a].getInternalName())) {
                        args[a] = Type.getObjectType("java/lang/Object");
                    }
                }
                Type ret = Type.getReturnType(m.desc);
                if (ret.getSort() == Type.OBJECT && Stage0.platform(ret.getInternalName())) {
                    ret = Type.getObjectType("java/lang/Object");
                }
                String erased = Type.getMethodDescriptor(ret, args);
                if (!erased.equals(m.desc) && node.methods.stream().noneMatch(x -> x.name.equals(m.name) && x.desc.equals(erased))) {
                    String before = m.desc;
                    m.desc = erased;
                    // (its calls, all in stubbed code now gone, and its lambda handles keep naming the old descriptor)
                    for (MethodNode other : node.methods) {
                        for (AbstractInsnNode insn : other.instructions) {
                            if (insn instanceof MethodInsnNode call && call.owner.equals(node.name) && call.name.equals(m.name) && call.desc.equals(before)) {
                                call.desc = erased;
                            }
                        }
                    }
                }
            }
            unverified.add("registry bootstrap " + node.name + "." + m.name + " (data generation only)");
        }
        // (the game's nested classes only the stubbed code used stay out of the class's inner class list too)
        StringBuilder used = new StringBuilder();
        for (FieldNode f : node.fields) {
            used.append(f.desc).append(f.signature);
        }
        for (MethodNode m : node.methods) {
            used.append(m.desc).append(m.signature);
            for (AbstractInsnNode insn : m.instructions) {
                if (insn instanceof MethodInsnNode call) {
                    used.append(call.owner).append(' ').append(call.desc);
                } else if (insn instanceof FieldInsnNode field) {
                    used.append(field.owner).append(' ').append(field.desc);
                } else if (insn instanceof TypeInsnNode type) {
                    used.append(type.desc).append(' ');
                } else if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Type t) {
                    used.append(t.getDescriptor());
                } else if (insn instanceof InvokeDynamicInsnNode indy) {
                    used.append(indy.desc).append(java.util.Arrays.toString(indy.bsmArgs));
                } else if (insn instanceof MultiANewArrayInsnNode multi) {
                    used.append(multi.desc);
                }
            }
        }
        String all = used.toString();
        node.innerClasses.removeIf(inner -> Stage0.platform(inner.name) && !all.contains(inner.name));
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
        boolean above = false, below = false, unused = false, mixin = false;
        for (String member : family) {
            PackClass pc = pack.get(member);
            if (pc == null || pc.mixin()) {
                // a mixin's methods are merged into the game's class: overriding is what they are for
                above = true;
                mixin = true;
                continue;
            }
            for (String p : packPlatformStarts(member)) {
                String upper = top.declaring(p, name, desc, true);
                if (upper != null) {
                    above = true;
                    unused |= unusedBelow.contains(upper + "." + name + " " + desc);
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
        // (and a family that overrides a hook the lower game never calls (a U row): its methods are its own there)
        boolean renamed = below && !above || unused && !mixin;
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
        if (newMetadata && !"1.21".equals(System.getProperty("ladder.to", "1.20.4"))) {
            // (a 1.21 target is FML 4 itself: its entry points stay the mod's own)
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
        splitSubscribers(entries);
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(out))) {
            for (var e : entries.entrySet()) {
                String name = e.getKey();
                byte[] data = e.getValue();
                if (name.endsWith(".class") && !name.startsWith("META-INF/")) {
                    ClassNode node = new ClassNode();
                    new ClassReader(data).accept(node, 0);
                    stubBootstraps(node);
                    // a data generator's class runs only in runData, never in the game: what it can't bring down doesn't
                    // keep the mod from running (its problems are listed apart, as unverified)
                    boolean datagen = datagenOnly(node.name);
                    Set<String> before = datagen ? new HashSet<>(problems) : null;
                    Map<String, String> landsAbove = null;
                    List<Boolean> shadowsAbove = null;
                    if (mixinTargets.containsKey(node.name)) {
                        MixinPoints.modClasses = mod::containsKey;
                        shadowsByArgumentPlans(node, mixinTargets.get(node.name));
                        landsAbove = MixinPoints.check(node, mixinTargets.get(node.name), Convert::topClass);
                        remapMixinSelectors(node, remapper);
                        if (!"0.8.7".equals(MIXIN_VERSIONS.getOrDefault(bottom, "0.8.7"))) {
                            interfaceSuperCalls(node, entries);
                        }
                    }
                    if (stripMod.contains(node.name) && node.visibleAnnotations != null) {
                        node.visibleAnnotations.removeIf(a -> a.desc.equals(MOD_ANNOTATION));
                    }
                    renameOwn(node);
                    checkReflection(node);
                    subscriberBus(node);
                    List<MethodNode> lowerOverrides = overrideBridges(node);
                    List<LambdaFix> lambdaFixes = lambdaBridges(node);
                    applyRules(node, true);
                    redirectCarried(node);
                    if (landsAbove != null) {
                        shadowsAbove = shadowsAbove(node, mixinTargets.get(node.name));
                    }
                    ClassNode remapped = new ClassNode();
                    node.accept(new ClassRemapper(remapped, remapper));
                    fixInterfaceCalls(remapped);
                    applyLambdaFixes(remapped, lambdaFixes);
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
                    if (landsAbove != null) {
                        bridgedShadows(remapped);
                        checkShadows(remapped, shadowsAbove);
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
                    // (1.20.5 renamed the file: a target above keeps it)
                    if (crosses("1.20.6", "1.20.4")) {
                        name = "META-INF/mods.toml";
                    }
                    data = modsToml(new String(data, StandardCharsets.UTF_8), bottom).getBytes(StandardCharsets.UTF_8);
                } else if (name.endsWith("mixins.json") || name.endsWith("mixin.json")) {
                    // the bottom version's Java and Mixin (1.20.4: Java 17, Mixin 0.8.5; 1.21's configs ask for 0.8.7)
                    String json = new String(data, StandardCharsets.UTF_8);
                    if (crosses("1.20.6", "1.20.4")) {
                        json = json.replaceAll("\"JAVA_2[0-9]\"", "\"JAVA_17\"");
                    }
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
                } else if (name.startsWith("data/") || nestedDataRoot(name, entries) != null) {
                    // the mod's data, or a data pack it ships (a folder with a pack.mcmeta: datapacks/<x>/data/...)
                    String root = name.startsWith("data/") ? "" : nestedDataRoot(name, entries);
                    String inner = dataPath(name.substring(root.length()));
                    if (inner == null) {
                        problem("data file with no " + bottom + " place: " + name);
                        continue;
                    }
                    String renamed = root + inner;
                    if (!renamed.equals(name) && entries.containsKey(renamed)) {
                        // the jar has the older folder too (a jar for both versions): that one is 1.20.4's
                        continue;
                    }
                    if (name.endsWith(".json") && crosses("1.20.6", "1.20.4")) {
                        // (item stacks in data: 1.20.5's components, the step down to 1.20.4)
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
    static final Map<String, String> MIXIN_VERSIONS = Map.of("1.20.4", "0.8.5", "1.20.6", "0.8.5", "1.21", "0.8.6", "1.21.1", "0.8.7");

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
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, ADAPTERS + "fml/ModEntries", "construct",
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
    /** whether the bottom game's class, its supers or interfaces have a concrete (or default) method of that name and descriptor */
    static boolean concreteBelow(String cls, String name, String desc, Set<String> seen) {
        ClassNode c = cls == null || !seen.add(cls) ? null : bottomClass(cls);
        if (c == null) {
            return false;
        }
        for (MethodNode m : c.methods) {
            if (m.name.equals(name) && m.desc.equals(desc) && (m.access & Opcodes.ACC_ABSTRACT) == 0) {
                return true;
            }
        }
        if (concreteBelow(c.superName, name, desc, seen)) {
            return true;
        }
        for (String i : c.interfaces) {
            if (concreteBelow(i, name, desc, seen)) {
                return true;
            }
        }
        return false;
    }

    /** S rows: owner, name, descriptor of a lower abstract method the upper type hasn't (a mod's class gets it empty) */
    static final List<String[]> lowerStubs = new ArrayList<>();
    /** X: "owner.name desc" -> {adapter class, adapter method} */
    static final Map<String, String[]> redirects = new HashMap<>();
    /** K rows: "owner.<init> upperDesc" -> {lower desc, adapter class, the lower parameters (an upper one $i, or an adapter method)} */
    static final Map<String, String[]> constructorArguments = new HashMap<>();
    /** "owner.name upperDesc" -> {lower desc, lower parameters ($i or a default)} (D rows) */
    static final Map<String, String[]> lowerDefaults = new HashMap<>();

    /**
     * The upper parameter each lower one is ($i -> i), or -1 for a default, when a D row covers the member and the
     * lower descriptor is the row's; else null.
     */
    static int[] defaultedLower(String upperKey, String lowerDesc) {
        String[] row = defaultsRow(upperKey);
        if (row == null || !row[0].equals(lowerDesc)) {
            return null;
        }
        String[] spec = specTokens(row[1]);
        int[] from = new int[spec.length];
        for (int j = 0; j < spec.length; j++) {
            from[j] = spec[j].startsWith("$") ? Integer.parseInt(spec[j].substring(1)) : -1;
        }
        return from;
    }

    /**
     * The D row of "owner.name upperDesc": the owner's own, else one of a class it extends (an override of the same
     * method: Zombie.finalizeSpawn under Mob's row), by the top version's classes.
     */
    static String[] defaultsRow(String upperKey) {
        String[] row = lowerDefaults.get(upperKey);
        if (row != null || lowerDefaults.isEmpty()) {
            return row;
        }
        int dot = upperKey.indexOf('.');
        String cls = upperKey.substring(0, dot), member = upperKey.substring(dot);
        for (var e : lowerDefaults.entrySet()) {
            int d = e.getKey().indexOf('.');
            if (e.getKey().substring(d).equals(member) && extendsClass(versions.get(0), cls, e.getKey().substring(0, d))) {
                return e.getValue();
            }
        }
        return null;
    }

    /** a D row's default for a lower parameter of that type */
    /** a D row's lower parameters: its commas outside an adapter call's parentheses split them (none: no parameters) */
    static String[] specTokens(String spec) {
        if (spec.trim().equals("-")) {
            // (a lower method without parameters: the row writes "-")
            return new String[0];
        }
        List<String> tokens = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < spec.length(); i++) {
            char c = spec.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (c == ',' && depth == 0) {
                tokens.add(spec.substring(start, i).trim());
                start = i + 1;
            }
        }
        String last = spec.substring(start).trim();
        if (!last.isEmpty() || !tokens.isEmpty()) {
            tokens.add(last);
        }
        return tokens.toArray(new String[0]);
    }

    /**
     * A D row's default of a lower parameter: a constant, or an adapter's value "owner.method(args)" of the receiver
     * ("this": the member's object, typed as the row's owner) and upper parameters ("$k", of their upper types).
     */
    static void pushDefault(InsnList code, String value, Type type, Type[] up, int[] upSlots, String receiver) {
        int open = value.indexOf('(');
        if (open < 0) {
            pushDefault(code, value, type);
            return;
        }
        String call = value.substring(0, open), args = value.substring(open + 1, value.length() - 1).trim();
        StringBuilder desc = new StringBuilder("(");
        for (String a : args.isEmpty() ? new String[0] : args.split(",")) {
            a = a.trim();
            if (a.equals("this")) {
                if (receiver == null) {
                    throw new IllegalArgumentException("D row default " + value + " of a static member");
                }
                code.add(new VarInsnNode(Opcodes.ALOAD, 0));
                desc.append('L').append(receiver).append(';');
            } else {
                int k = Integer.parseInt(a.substring(1));
                code.add(new VarInsnNode(up[k].getOpcode(Opcodes.ILOAD), upSlots[k]));
                desc.append(up[k].getDescriptor());
            }
        }
        desc.append(')').append(type.getDescriptor());
        String owner = call.substring(0, call.lastIndexOf('.')), name = call.substring(call.lastIndexOf('.') + 1);
        checkAdapter(owner, name, desc.toString(), "D default " + value);
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc.toString(), false));
    }

    /** the zero of a type on the stack (0, 0L, 0.0, null) */
    static void pushZero(InsnList code, Type type) {
        switch (type.getSort()) {
            case Type.LONG -> code.add(new InsnNode(Opcodes.LCONST_0));
            case Type.FLOAT -> code.add(new InsnNode(Opcodes.FCONST_0));
            case Type.DOUBLE -> code.add(new InsnNode(Opcodes.DCONST_0));
            case Type.OBJECT, Type.ARRAY -> code.add(new InsnNode(Opcodes.ACONST_NULL));
            default -> code.add(new InsnNode(Opcodes.ICONST_0));
        }
    }

    static void pushDefault(InsnList code, String value, Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> code.add(new LdcInsnNode(
                    value.equals("true") ? 1 : value.equals("false") ? 0 : Integer.parseInt(value)));
            case Type.LONG -> code.add(new LdcInsnNode(Long.parseLong(value)));
            case Type.FLOAT -> code.add(new LdcInsnNode(Float.parseFloat(value)));
            case Type.DOUBLE -> code.add(new LdcInsnNode(Double.parseDouble(value)));
            default -> {
                if (!value.equals("null")) {
                    throw new IllegalArgumentException("D row default " + value + " for " + type);
                }
                code.add(new InsnNode(Opcodes.ACONST_NULL));
            }
        }
    }
    /** F rows: "owner.name desc" of a static field -> {adapter class, getter}: its reads go to the getter */
    static final Map<String, String[]> fieldRedirects = new HashMap<>();
    /** U rows: "owner.name desc" of hooks the lower game never calls */
    static final Set<String> unusedBelow = new HashSet<>();
    /** A rows: owner, name, upper desc, lower desc, plan */
    static final List<String[]> argumentPlans = new ArrayList<>();

    /**
     * The receiver type of an adapter call (an X row's, an O row's defaults): the member's declaring class, or, when that
     * class is missing below (an interface new above that a game class implements, like NeoForge 21.1's
     * IAttributeExtension on Attribute), the game class up the call owner's chain that implements it there.
     */
    /**
     * The X row of a supertype's instance method that a class's own method overrides: {the row's owner, adapter class,
     * adapter method}; null when none covers it.
     */
    static String[] inheritedX(Stage0.Ref ref) {
        if (ref.kind() != 'M' || ref.name().startsWith("<") || redirects.isEmpty()) {
            return null;
        }
        Stage0.Version here = versions.get(level);
        Stage0.Cls own = here.classes.get(ref.cls());
        if (own == null || own.staticMethods.contains(ref.name() + " " + ref.desc())) {
            return null;
        }
        Deque<String> todo = new ArrayDeque<>();
        todo.add(own.superName == null ? "" : own.superName);
        todo.addAll(Arrays.asList(own.interfaces));
        Set<String> seen = new HashSet<>();
        while (!todo.isEmpty()) {
            String c = todo.poll();
            if (c.isEmpty() || !seen.add(c)) {
                continue;
            }
            String[] x = redirects.get(c + "." + ref.name() + " " + ref.desc());
            Stage0.Cls info = here.classes.get(c);
            if (x != null && info != null && info.methods.contains(ref.name() + " " + ref.desc())
                    && !info.staticMethods.contains(ref.name() + " " + ref.desc())) {
                return new String[] {c, x[0], x[1]};
            }
            if (info != null) {
                todo.add(info.superName == null ? "" : info.superName);
                todo.addAll(Arrays.asList(info.interfaces));
            }
        }
        return null;
    }

    static String adapterReceiver(String declaring, String owner) {
        Stage0.Version here = versions.get(level), bottom = versions.get(versions.size() - 1);
        if (!Stage0.platform(declaring) || Stage0.mapClass(here, bottom, declaring) != null || provided.containsKey(declaring)) {
            // (a class the adapters provide, DataComponentHolder: the adapter takes its own)
            return declaring;
        }
        Set<String> seen = new HashSet<>();
        for (String c = owner; c != null && seen.add(c);) {
            if (Stage0.platform(c)) {
                Stage0.Cls info = here.classes.get(c);
                if (info == null) {
                    break;
                }
                if ((Arrays.asList(info.interfaces).contains(declaring) || declaring.equals(info.superName)) && Stage0.mapClass(here, bottom, c) != null) {
                    // (or a superclass new above: 1.21's GenerationChunkHolder over ChunkHolder)
                    return c;
                }
                c = info.superName;
            } else {
                Stage0.ModClass mc = mod.get(c);
                c = mc == null ? null : mc.superName;
            }
        }
        return declaring;
    }

    /** the plan's tokens for a handler fix (its upper descriptor in lower names, its lower member), or null */
    static String[] argumentPlan(String upperDesc, Stage0.Ref end) {
        String[] row = argumentPlanRow(upperDesc, end);
        return row == null ? null : row[4].trim().split(" +");
    }

    /**
     * The A row of a lower member: its owner's own, else one of a class it extends below (an override of the same
     * method: Zombie.dropCustomDeathLoot under LivingEntity's row); null when none covers it.
     */
    static String[] argumentPlanRow(String upperDesc, Stage0.Ref end) {
        return argumentPlanRow(upperDesc, end, end.name());
    }

    /** (the row names the upper method: a hook renamed below, canUsePortal as canChangeDimensions, by its upper name) */
    static String[] argumentPlanRow(String upperDesc, Stage0.Ref end, String upperName) {
        String[] inherited = null;
        for (String[] a : argumentPlans) {
            boolean named = a[1].equals(end.name()) || a[1].equals(upperName);
            if (!named && a[3].equals(end.desc()) && remapper.mapMethodDesc(a[2]).equals(upperDesc)) {
                // (a method renamed below: the row's upper method is the hook below of that name)
                Stage0.Ref below = hookBelow(a[0], a[1], a[2]);
                named = below != null && below.name().equals(end.name());
            }
            if (named && a[3].equals(end.desc()) && remapper.mapMethodDesc(a[2]).equals(upperDesc)) {
                String owner = remapper.map(a[0]);
                if (owner.equals(end.cls())) {
                    return a;
                }
                if (inherited == null && subtypeIn(versions.get(versions.size() - 1), end.cls(), owner)) {
                    inherited = a;
                }
            }
        }
        return inherited;
    }
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
                    case "S" -> lowerStubs.add(new String[] {c[1], c[2], c[3]});
                    case "X" -> redirects.put(c[1] + "." + c[2] + " " + c[3], new String[] {c[4], c[5]});
                    // a constructor that takes more below: each lower parameter is an upper one or an adapter's value
                    case "K" -> constructorArguments.put(c[1] + ".<init> " + c[2], new String[] {c[3], c[4], c[5]});
                    // a method that takes more below: each lower parameter is an upper one ($0, $1..) or a default written
                    // in the row (false, true, a number, null); calls, overrides and mixin selectors go to the lower one
                    case "D" -> lowerDefaults.put(c[1] + "." + c[2] + " " + c[3], new String[] {c[4], c[5], c[1]});
                    // a class renamed below with the same members: its uses get the lower name
                    case "R" -> NEOFORGE_MOVES.put(c[1], c[2]);
                    // a member (of the owner and its subclasses) that is another one below: bridged to it
                    case "M" -> memberMoves.add(new String[] {c[1], c[2], c[3], c[4], c[5]});
                    // a static field below that the adapters give: its reads go to their getter
                    // (an optional 7th column: the only methods of the value a mod may call, "getWidth,getY,...": the
                    // getter gives an object that holds that much of the upper one)
                    case "F" -> fieldRedirects.put(c[1] + "." + c[2] + " " + c[3], new String[] {c[4], c[5], c.length > 6 ? c[6] : null});
                    // a hook the lower game never calls (no counterpart below: AbstractSelectionList.renderListSeparators,
                    // drawn by 1.20.4's renderWidget itself): a mod's override is a method of its own there, its super
                    // call and other calls go to the member's X row
                    case "U" -> unusedBelow.add(c[1] + "." + c[2] + " " + c[3]);
                    // the arguments of a method whose parameters changed below, from the lower method's (a mixin
                    // handler's wrapper): per upper parameter "$k" (the lower one, converted) or "Class.method(k,...)"
                    case "A" -> argumentPlans.add(new String[] {c[1], c[2], c[3], c[4], c[5]});
                    // a data folder the adapters give a place below (a data pack registry they add: jukebox_song)
                    case "G" -> adapterDataFolders.add(c[1]);
                    // a mixin's injection point in one method of the game, where 1.20.6's code makes another call for the
                    // same value (1.21's looting level, getEnchantmentLevel(looting, entity); 20.6's LootContext's own)
                    case "T" -> pointRetargets.put(c[1] + "." + c[2] + " " + c[3], c[4]);
                    // a class carried down whose registering (its bootstrap into a registry) the adapters run
                    case "B" -> bootstrapped.add(c[1]);
                    default -> throw new IOException("unknown rule " + line);
                }
            }
        }
        String jar = System.getProperty("ladder.adapters");
        if (jar != null) {
            try (ZipFile zip = new ZipFile(jar)) {
                ZipEntry at = zip.getEntry("META-INF/accesstransformer.cfg");
                if (at != null) {
                    try (InputStream s = zip.getInputStream(at)) {
                        readAccessTransformer(new String(s.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
                    }
                }
                for (ZipEntry e : Collections.list(zip.entries())) {
                    if (!e.getName().endsWith(".class")) {
                        continue;
                    }
                    try (InputStream s = zip.getInputStream(e)) {
                        byte[] bytes = s.readAllBytes();
                        ClassNode node = new ClassNode();
                        new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE);
                        adapterBytes.put(node.name, bytes);
                        collectOverwrites(bytes);
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
                        if ((node.access & Opcodes.ACC_INTERFACE) != 0) {
                            adapterInterfaces.add(node.name);
                        }
                    }
                }
            }
            loadShapePatches(Path.of(jar));
        }
    }

    /**
     * The adapters' {@code @Overwrite}s of game methods (a method made the upper version's shape, so the mods' injectors
     * into it land): target class -> the overwriting methods. The mixin checks see the game class with these bodies.
     */
    static final Map<String, List<MethodNode>> adapterOverwrites = new HashMap<>();

    static void collectOverwrites(byte[] bytes) {
        ClassNode c = new ClassNode();
        new ClassReader(bytes).accept(c, 0);
        List<String> targets = new ArrayList<>();
        for (AnnotationNode a : c.invisibleAnnotations == null ? List.<AnnotationNode>of() : c.invisibleAnnotations) {
            if (!a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;") || a.values == null) {
                continue;
            }
            for (int i = 0; i + 1 < a.values.size(); i += 2) {
                Object v = a.values.get(i + 1);
                if (a.values.get(i).equals("value") && v instanceof List<?> l) {
                    l.forEach(t -> targets.add(((Type) t).getInternalName()));
                } else if (a.values.get(i).equals("targets") && v instanceof List<?> l) {
                    l.forEach(t -> targets.add(((String) t).replace('.', '/')));
                }
            }
        }
        for (MethodNode m : c.methods) {
            if (MixinPoints.annotations(m).stream().anyMatch(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/Overwrite;"))) {
                targets.forEach(t -> adapterOverwrites.computeIfAbsent(t, k -> new ArrayList<>()).add(m));
            } else if (!targets.isEmpty() && (c.access & Opcodes.ACC_INTERFACE) == 0 && added(m)) {
                // a method the mixin merges into its target (a member the game below hasn't, in the upper game's shape):
                // the mods' references to it are the target's own, and the mixin checks see it there
                targets.forEach(t -> adapterAdded.computeIfAbsent(t, k -> new ArrayList<>()).add(m));
            }
        }
        if (!targets.isEmpty() && (c.access & Opcodes.ACC_INTERFACE) == 0) {
            for (org.objectweb.asm.tree.FieldNode f : c.fields) {
                boolean annotated = (f.visibleAnnotations != null && !f.visibleAnnotations.isEmpty())
                        || (f.invisibleAnnotations != null && !f.invisibleAnnotations.isEmpty());
                if (!annotated && !f.name.startsWith("ladder$") && (f.access & Opcodes.ACC_SYNTHETIC) == 0) {
                    // a field the mixin merges into its target (1.21's OptionsSubScreen.list on 1.20.6)
                    targets.forEach(t -> adapterAddedFields.add(t + " " + f.name + " " + f.desc));
                }
            }
        }
    }

    /** the fields the adapters' mixins add to the game's classes: "class name desc" */
    static final Set<String> adapterAddedFields = new HashSet<>();
    /** T rows: "upper owner.method upper target" -> the lower target (an injection point in that method) */
    static final Map<String, String> pointRetargets = new HashMap<>();
    /** the upper methods (names) the injector being remapped names, for the T rows */
    static List<String> injectorMethods = List.of();
    static final Set<String> addedFieldClasses = new HashSet<>();

    /** the methods the adapters' mixins add to the game's classes (not their handlers, shadows or own helpers) */
    static final Map<String, List<MethodNode>> adapterAdded = new HashMap<>();

    static boolean added(MethodNode m) {
        if (m.name.startsWith("<") || m.name.startsWith("ladder$") || m.name.startsWith("lambda$") || (m.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_SYNTHETIC)) != 0) {
            return false;
        }
        // annotated: an injector, a shadow, an accessor... (a plain method is merged as it is)
        return MixinPoints.annotations(m).stream().noneMatch(a -> a.desc.startsWith("Lorg/spongepowered/asm/mixin/")
                || a.desc.startsWith("Lcom/llamalad7/mixinextras/"));
    }

    /** whether an adapters' mixin adds this method to the class (the game below hasn't it otherwise) */
    static boolean adapterAddsMethod(String cls, String name, String desc) {
        // (or an inherited one to a superclass below: 1.21's KeyBindsScreen.addContents is OptionsSubScreen's there)
        Stage0.Version bottom = versions.get(versions.size() - 1);
        boolean own = true;
        for (String c = cls; c != null; own = false) {
            boolean inherited = !own;
            // (the adapters name a carried class by its carried name; the reference by the upper game's)
            if (adapterAdded.getOrDefault(c, List.of()).stream().anyMatch(m -> m.name.equals(name)
                    && (!inherited || (m.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) == 0)
                    && (m.desc.equals(desc) || m.desc.replace("L" + CARRIED, "L").equals(desc)
                            // (or a provided class: 1.21's DeltaTracker of Minecraft.getTimer() is the adapters' own)
                            || m.desc.equals(remapper.mapMethodDesc(desc))))) {
                return true;
            }
            Stage0.Cls info = bottom.classes.get(c);
            c = info == null ? null : info.superName;
        }
        return false;
    }

    /** the adapters' classes (a provided class is a mixin target below: checked on its own code) */
    static final Map<String, byte[]> adapterBytes = new HashMap<>();
    static final Map<String, Optional<ClassNode>> adapterClasses = new HashMap<>();

    /** the adapters' interfaces: a static call into one (an O row's defaults, an X row's adapter there) is an interface call */
    static final Set<String> adapterInterfaces = new HashSet<>();

    /** the adapters' shape patches (bottom class -> its patches), applied to the bottom classes mixins are checked on */
    static final Map<String, List<Object>> shapePatches = new HashMap<>();
    static java.lang.reflect.Method shapeApply;

    /**
     * The adapters' shape patches (shape-patches.tsv, ladder.adapter.shape.ShapePatches: the same code the game runs):
     * each checked to keep 1.20.4's behaviour on the unpatched bottom classes, and its class one the adapters' shape
     * mixin aims at (so the game applies it too). A patch that fails either check stops the conversion.
     */
    @SuppressWarnings("unchecked")
    static void loadShapePatches(Path jar) throws IOException {
        String tsv;
        Set<String> aimed = new HashSet<>();
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            ZipEntry e = zip.getEntry("shape-patches.tsv");
            if (e == null) {
                return;
            }
            try (InputStream in = zip.getInputStream(e)) {
                tsv = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            ZipEntry mixin = zip.getEntry(ADAPTERS + "mixin/ShapeTargetsMixin.class");
            if (mixin != null) {
                try (InputStream in = zip.getInputStream(mixin)) {
                    ClassNode node = new ClassNode();
                    new ClassReader(in).accept(node, ClassReader.SKIP_CODE);
                    for (AnnotationNode a : node.invisibleAnnotations == null ? List.<AnnotationNode>of() : node.invisibleAnnotations) {
                        for (int i = 0; a.values != null && i + 1 < a.values.size(); i += 2) {
                            if (a.values.get(i).equals("targets")) {
                                ((List<String>) a.values.get(i + 1)).forEach(t -> aimed.add(t.replace('.', '/')));
                            }
                        }
                    }
                }
            }
        }
        try {
            ClassLoader loader = new java.net.URLClassLoader(new java.net.URL[] {jar.toUri().toURL()}, Convert.class.getClassLoader());
            Class<?> type = Class.forName(ADAPTERS.replace('/', '.') + "shape.ShapePatches", true, loader);
            List<Object> patches = (List<Object>) type.getMethod("parse", String.class).invoke(null, tsv);
            java.lang.reflect.Method verify = type.getMethod("verify", Class.forName(ADAPTERS.replace('/', '.') + "shape.ShapePatches$Patch", true, loader),
                    java.util.function.Function.class);
            shapeApply = type.getMethod("apply", ClassNode.class, List.class);
            for (Object patch : patches) {
                String owner = (String) patch.getClass().getMethod("owner").invoke(patch);
                Object why = verify.invoke(null, patch, (java.util.function.Function<String, ClassNode>) Convert::unpatchedBottomClass);
                if (why != null) {
                    throw new IOException("shape patch " + patch + ": " + why);
                }
                if (!aimed.contains(owner)) {
                    throw new IOException("shape patch " + patch + ": " + owner + " isn't a target of the adapters' ShapeTargetsMixin");
                }
                shapePatches.computeIfAbsent(owner, k -> new ArrayList<>()).add(patch);
            }
        } catch (ReflectiveOperationException e) {
            throw new IOException("the adapters' shape patches", e);
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
        applyRules0(node, modClass);
        // a static call into an adapter interface needs an interface method reference (else IncompatibleClassChangeError)
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode insn : m.instructions) {
                if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESTATIC && adapterInterfaces.contains(call.owner)) {
                    call.itf = true;
                }
            }
        }
    }

    static void applyRules0(ClassNode node, boolean modClass) {
        if (mixinTargets.containsKey(node.name) && !fieldRedirects.isEmpty()) {
            // a mixin's @Shadow of a target field an F row gives (OptionsScreen.layout): its reads go to the getter
            // below, and the field, which the lower class hasn't, is no shadow to locate
            node.fields.removeIf(f -> {
                boolean shadow = java.util.stream.Stream.of(f.visibleAnnotations, f.invisibleAnnotations).filter(java.util.Objects::nonNull)
                        .flatMap(List::stream).anyMatch(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/Shadow;"));
                if (!shadow) {
                    return false;
                }
                Stage0.Ref declared = resolve(node.name, f.name, f.desc, false);
                return declared != null && fieldRedirects.containsKey(declared.cls() + "." + f.name + " " + f.desc);
            });
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
                    if (f.length > 2 && f[2] != null) {
                        AbstractInsnNode use = field.getNext();
                        while (use != null && use.getOpcode() < 0) {
                            use = use.getNext();
                        }
                        boolean allowed = use instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKEVIRTUAL
                                && ("L" + call.owner + ";").equals(field.desc) && Arrays.asList(f[2].split(",")).contains(call.name);
                        if (!allowed) {
                            problem("the adapters' " + declaring + "." + field.name + " holds only " + f[2] + ": another use of it in " + node.name + "." + m.name);
                        }
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
                        if (indy.bsmArgs[a] instanceof Handle h && h.getTag() == Opcodes.H_NEWINVOKESPECIAL && Stage0.platform(h.getOwner())) {
                            // a constructor reference (ReceivingLevelScreen::new) of an X row's constructor: its factory
                            Stage0.Ref ref = resolve(h.getOwner(), "<init>", h.getDesc(), true);
                            String[] x = ref == null ? null : redirects.get(ref.cls() + "." + ref.name() + " " + ref.desc());
                            if (x != null) {
                                String desc = h.getDesc().substring(0, h.getDesc().indexOf(')') + 1) + "L" + h.getOwner() + ";";
                                checkAdapter(x[0], x[1], desc, "X " + ref.key() + " (constructor reference)");
                                indy.bsmArgs[a] = new Handle(Opcodes.H_INVOKESTATIC, x[0], x[1], desc, false);
                            }
                            continue;
                        }
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
                        Type[] captured = Type.getArgumentTypes(indy.desc);
                        if (h.getTag() != Opcodes.H_INVOKESTATIC && captured.length > 0 && !captured[0].getDescriptor().equals("L" + ref.cls() + ";")) {
                            // a bound receiver (value::getRaw on a BooleanValue): the factory's captured types must be the
                            // adapter's own, exactly (the receiver is one of them, a subtype passed as its supertype)
                            captured[0] = Type.getObjectType(ref.cls());
                            indy.desc = Type.getMethodDescriptor(Type.getReturnType(indy.desc), captured);
                        }
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
                // (an abstract mod base right under Entity gets it too: its concrete subclasses inherit it)
                if (o[6] != null && (node.access & Opcodes.ACC_INTERFACE) == 0 && abstractBelow(node.superName, o[6])) {
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
        if (modClass && !mixinTargets.containsKey(node.name) && (node.access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT)) == 0) {
            // a concrete mod class under a game type whose lower version has abstract methods the upper hasn't (an S row):
            // an empty one each (VertexConsumer's endVertex: 1.21 ends a vertex by the next)
            List<String> platform = starts(node.name);
            for (String[] stub : lowerStubs) {
                boolean under = platform.stream().anyMatch(p -> subtypeOf(here, p, stub[0], new HashSet<>()));
                boolean inherited = platform.stream().anyMatch(p -> concreteBelow(remapper.map(p), stub[1], stub[2], new HashSet<>()));
                if (under && !inherited && node.methods.stream().noneMatch(m -> m.name.equals(stub[1]) && m.desc.equals(stub[2]))) {
                    MethodNode empty = new MethodNode(Opcodes.ACC_PUBLIC, stub[1], stub[2], null, null);
                    Type ret = Type.getReturnType(stub[2]);
                    if (ret.getSort() == Type.VOID) {
                        empty.instructions.add(new InsnNode(Opcodes.RETURN));
                    } else if (ret.getSort() == Type.OBJECT && ret.getInternalName().equals(stub[0])) {
                        // a builder's method: itself
                        empty.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
                        empty.instructions.add(new InsnNode(Opcodes.ARETURN));
                    } else {
                        problem("S row " + stub[0] + "." + stub[1] + stub[2] + ": only void or self-returning methods get an empty one");
                        continue;
                    }
                    empty.maxLocals = Type.getArgumentsAndReturnSizes(stub[2]) >> 2;
                    empty.maxStack = 1;
                    addAfterRemap.computeIfAbsent(node.name, k -> new ArrayList<>()).add(empty);
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
                    // new X(args) or super(args) of a constructor that takes more below (a K row)
                    String[] k = ref == null ? null : constructorArguments.get(ref.cls() + "." + ref.name() + " " + ref.desc());
                    if (x != null && k != null && newOf(call) == null) {
                        // a super(...) call no factory can make: the K row's lower arguments (a mod's own BufferSource)
                        x = null;
                    }
                    if (x != null) {
                        checkAdapter(x[0], x[1], call.desc.substring(0, call.desc.indexOf(')') + 1) + "L" + call.owner + ";", "X " + ref.key());
                        newToFactory(m, call, x, ref);
                    }
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
                String xOwner = ref.cls();
                if (x == null && call.getOpcode() != Opcodes.INVOKESTATIC && call.getOpcode() != Opcodes.INVOKESPECIAL) {
                    // a class's own override of a member an X row covers on its supertype (NeoForge 21's
                    // QuadBakingVertexConsumer.addVertex under VertexConsumer's): the same adapter, which dispatches
                    String[] inherited = inheritedX(ref);
                    if (inherited != null) {
                        xOwner = inherited[0];
                        x = new String[] {inherited[1], inherited[2]};
                    }
                }
                if (x != null && call.getOpcode() == Opcodes.INVOKESPECIAL && overrideRule(ref.cls(), ref.name(), ref.desc()) != null) {
                    // a super call of a member with both rows: the O row's defaults (the X row's adapter dispatches to the
                    // override, which would call itself)
                    x = null;
                }
                if (x != null) {
                    String desc = call.getOpcode() == Opcodes.INVOKESTATIC ? call.desc
                            : "(L" + adapterReceiver(xOwner, call.owner) + ";" + call.desc.substring(1);
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
                boolean throughMod = call.getOpcode() == Opcodes.INVOKESPECIAL && !Stage0.platform(call.owner)
                        && !modChainDeclares(call.owner, call.name + " " + call.desc);
                if (call.getOpcode() == Opcodes.INVOKESPECIAL && (Stage0.platform(call.owner) || throughMod)) {
                    // (a super call through a mod class that doesn't declare it lands on the game's: the same defaults)
                    // super into the game's class: exact when that class doesn't override it in the newer version
                    // (an O row of its own covers the ones that do, like BlockItem.appendHoverText)
                    // (a row whose lower hook 1.20.4 runs for every game class itself, defineSynchedData(), has defaults
                    // that are right under any game class: the game's own overrides ran already)
                    if (!ref.cls().equals(o[0]) && o[6] == null) {
                        problem("super call to " + ref.key() + ", overridden by the game's " + ref.cls() + ": no bridge for that override");
                    }
                    String desc = "(L" + adapterReceiver(o[0], call.owner) + ";" + call.desc.substring(1);
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

    /** whether a mod class or one of the mod's superclasses above it (up to the first game class) declares the method */
    static boolean modChainDeclares(String cls, String method) {
        for (int depth = 0; cls != null && depth < 32; depth++) {
            Stage0.ModClass mc = mod.get(cls);
            if (mc == null) {
                return false;
            }
            if (mc.methods.contains(method)) {
                return true;
            }
            cls = mc.superName;
        }
        return false;
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
                    } else if (i == level && !versions.get(i).classes.containsKey(cur) && !versions.get(i).bytes.containsKey(cur)) {
                        // not in the mod's own version either (another loader's or another version's class, as in a mod
                        // made for several): code that can't run natively, so nothing the step changes; listed apart
                        unverified.add("a class its own version hasn't either: " + c);
                        cur = c;
                    } else if (datagenApi(c)) {
                        unverified.add("data generation API class " + c + " (" + versions.get(i).id + " -> " + versions.get(i + 1).id + ")");
                        cur = c;
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
            if (decl != null && method && !Stage0.platform(owner) && !decl.equals(owner)) {
                // a reference through a mod class never links to a private method up in the game (a mixin's own
                // lambda$init$0 beside 1.21's OptionsSubScreen one): private members aren't inherited, so it is the mod's
                Stage0.Cls info = here.classes.get(decl);
                Integer access = info == null ? null : info.access.get("M " + name + " " + desc);
                if (access != null && (access & Opcodes.ACC_PRIVATE) != 0) {
                    return null;
                }
            }
            if (decl != null) {
                if (method && !decl.equals(start) && declaredLowerBelow(start, decl, name, desc)) {
                    // missing below in its declaring class but declared below in a class between it and the reference's
                    // (1.20.4's Block.propagatesSkylightDown, 1.21's BlockBehaviour's): the reference's own class
                    // resolves it there (walk retries from it)
                    return new Stage0.Ref('M', start, name, desc);
                }
                return new Stage0.Ref(method ? 'M' : 'F', decl, name, desc);
            }
        }
        return null;
    }

    /** "start decl.name desc" -> whether the member moved down the hierarchy below (declaredLowerBelow) */
    static final Map<String, Boolean> declaredLower = new HashMap<>();

    /**
     * Whether a method the top version declares in decl, which a reference reaches from start (a subclass), is missing
     * in decl's class at some rung but resolved there from start's class (a class between them declares it below). No
     * rule may cover the member (a rule keyed by its declaring class is meant for it).
     */
    static boolean declaredLowerBelow(String start, String decl, String name, String desc) {
        String cacheKey = level + " " + start + " " + decl + "." + name + " " + desc;
        Boolean known = declaredLower.get(cacheKey);
        if (known != null) {
            return known;
        }
        boolean result = false;
        String key = decl + "." + name + " " + desc;
        if (!redirects.containsKey(key) && !lowerDefaults.containsKey(key) && !unusedBelow.contains(key) && overrideRule(decl, name, desc) == null
                && memberMoves.stream().noneMatch(m -> m[0].equals(decl) && m[1].equals(name) && m[2].equals(desc))
                && !provided.containsKey(decl)) {
            Stage0.Ref cur = new Stage0.Ref('M', decl, name, desc);
            String from = start;
            for (int i = level; i + 1 < versions.size() && cur != null && from != null; i++) {
                Stage0.Version a = versions.get(i), b = versions.get(i + 1);
                Stage0.Step st = memberStep(a, b, cur);
                if (st.outcome() == Stage0.Outcome.SAME || st.outcome() == Stage0.Outcome.RENAMED) {
                    cur = st.to();
                    from = Stage0.mapClass(a, b, from);
                    continue;
                }
                if (st.outcome() == Stage0.Outcome.MISSING_MEMBER) {
                    Stage0.Step again = memberStep(a, b, new Stage0.Ref('M', from, cur.name(), cur.desc()));
                    result = (again.outcome() == Stage0.Outcome.SAME || again.outcome() == Stage0.Outcome.RENAMED)
                            && !again.to().cls().equals(Stage0.mapClass(a, b, cur.cls()));
                }
                break;
            }
        }
        declaredLower.put(cacheKey, result);
        return result;
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

    /**
     * The lower descriptor of a constructor that takes other values below, by a K row (its lower descriptor) or an A row
     * (the mixin handler's plan); null when it is the same below or no row covers it.
     */
    static String lowerConstructor(String owner, String desc) {
        if (versions.get(versions.size() - 1).declaring(remapper.map(owner), "<init>", remapper.mapMethodDesc(desc), true) != null) {
            return null;
        }
        String[] k = constructorArguments.get(owner + ".<init> " + desc);
        if (k != null) {
            return k[0];
        }
        for (String[] a : argumentPlans) {
            if (a[0].equals(owner) && a[1].equals("<init>") && a[2].equals(desc)) {
                return a[3];
            }
        }
        return null;
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
        // the reference's own class, at each rung's version: a member missing in its declaring class may be declared
        // below in a class between (declaredLowerBelow)
        String start = ref.cls();
        for (int i = level; i + 1 < versions.size() && cur != null; i++) {
            if (provided.containsKey(cur.cls())) {
                // a class the adapters provide: its members are theirs, at every rung (whatever the versions between do)
                return new Stage0.Ref(cur.kind(), provided.get(cur.cls()), cur.name(), cur.desc());
            }
            Stage0.Version from = versions.get(i), to = versions.get(i + 1);
            Stage0.Step s = memberStep(from, to, cur);
            if (s.outcome() == Stage0.Outcome.MISSING_MEMBER && cur.kind() == 'M' && start != null && !start.equals(cur.cls())) {
                Stage0.Step again = memberStep(from, to, new Stage0.Ref('M', start, cur.name(), cur.desc()));
                if (again.outcome() == Stage0.Outcome.SAME || again.outcome() == Stage0.Outcome.RENAMED) {
                    s = again;
                }
            }
            start = start == null ? null : Stage0.mapClass(from, to, start);
            switch (s.outcome()) {
                case SAME, RENAMED -> cur = s.to();
                case CHANGED -> {
                    // (an M row names the lower hook itself: it wins over the one found by the name, 1.21's
                    // getRecipeFor(type, input, level, last) whose game call sites below are getRecipeFor(type, container, level))
                    Stage0.Ref named = cur.name().equals("<init>") ? null : movedMember(i, cur);
                    String[] bridge = cur.name().equals("<init>") ? constructorBridge(i, cur) : bridgeFor(i, cur, named != null ? named : s.to());
                    if (bridge != null) {
                        // the call moves to a generated bridge that converts the values (Holder<X> <-> X) and calls below
                        callRedirect.put(key, bridge);
                        return new Stage0.Ref(cur.kind(), bridge[0], bridge[1], cur.desc());
                    }
                    if (ref.kind() == 'F' && ruled(ref.key())) {
                        // a field read an F row's getter takes (a method's X row covers calls, not overrides: no skip)
                        return cur;
                    }
                    if (datagenApi(cur.cls())) {
                        // the data generators' API runs in runData only, never in the game
                        unverified.add("data generation API " + cur.key() + ", changed (" + from.id + " -> " + to.id + ")");
                        return null;
                    }
                    if (cur.kind() == 'M' && i + 2 == versions.size() && redirects.containsKey(cur.cls() + "." + cur.name() + " " + cur.desc())
                            && argumentPlanRow(remapper.mapMethodDesc(cur.desc()), s.to(), cur.name()) != null) {
                        // calls take the X row's adapter; a mod's override is called from the lower method by the A row's
                        // plan (an entity's own changeDimension(transition) from 1.20.6's changeDimension(level)): its
                        // bridge has no call side
                        String[] only = {OVERRIDE_ONLY + cur.cls(), cur.name() + "$" + Integer.toHexString(cur.desc().hashCode())};
                        callRedirect.put(key, only);
                        bridgeEnds.put(only[0] + "." + only[1], s.to());
                        return new Stage0.Ref(cur.kind(), only[0], only[1], cur.desc());
                    }
                    if (privateShadowable(from, cur, s.to())) {
                        // a private method only a mixin reaches (its @Shadow): the shadow becomes the lower one and its
                        // calls go to a converting helper once the mixin is remapped (shadowHelpers)
                        return s.to();
                    }
                    problem("changed descriptor, no bridge yet: " + cur.key() + " -> " + s.to().name() + s.to().desc() + " (" + from.id + " -> " + to.id + ")");
                    cur = s.to();
                }
                default -> {
                    if (provided.containsKey(cur.cls())) {
                        // a class the adapters provide: its members are theirs
                        return new Stage0.Ref(cur.kind(), provided.get(cur.cls()), cur.name(), cur.desc());
                    }
                    Stage0.Ref renamedTypes = renamedInDescriptor(cur);
                    if (renamedTypes != null) {
                        // a member whose descriptor names a class an R row renames: the bottom's member of the renamed
                        // types (the remapper renames them in the reference)
                        return renamedTypes;
                    }
                    Stage0.Ref moved = movedMember(i, cur);
                    if (moved != null) {
                        // an M row: the same hook below under another name and descriptor, bridged to it
                        String[] bridge = bridgeFor(i, cur, moved);
                        if (bridge == null && i + 2 < versions.size()) {
                            // the hook found at the bottom (missing on the way down): bridged across the last step to it
                            bridge = bridgeFor(versions.size() - 2, cur, moved);
                        }
                        if (bridge != null) {
                            callRedirect.put(key, bridge);
                            return new Stage0.Ref(cur.kind(), bridge[0], bridge[1], cur.desc());
                        }
                        problem("changed below (an M row), no bridge: " + cur.key() + " -> " + moved.name() + moved.desc());
                        return null;
                    }
                    if (NEOFORGE_MOVES.containsKey(cur.cls())) {
                        // a class renamed below (an R row): its members are the renamed class's, never carried (an M
                        // row of its member, above, comes first: the same member below under another name)
                        return renamedMember(cur);
                    }
                    if (i + 2 == versions.size() && cur.kind() == 'F' && adapterAddedFields.contains((Stage0.mapClass(from, to, cur.cls()) == null ? cur.cls()
                            : Stage0.mapClass(from, to, cur.cls())) + " " + cur.name() + " " + cur.desc())) {
                        // the adapters' mixin gives the bottom class this field
                        return cur;
                    }
                    if (i + 2 == versions.size() && cur.kind() == 'M' && adapterAddsMethod(Stage0.mapClass(from, to, cur.cls()) == null ? cur.cls()
                            : Stage0.mapClass(from, to, cur.cls()), cur.name(), cur.desc())) {
                        // the adapters' mixin gives the bottom class this method (in the upper game's shape)
                        return cur;
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
                    if (datagenApi(cur.cls())) {
                        // the data generators' API runs in runData only, never in the game (GatherDataEvent's members)
                        unverified.add("data generation API " + cur.key() + " (" + from.id + " -> " + to.id + ")");
                        return null;
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
                if (lowerOwner == null && NEOFORGE_MOVES.containsKey(cur.cls())) {
                    // a class an R row renames below: the member is the renamed class's
                    lowerOwner = NEOFORGE_MOVES.get(cur.cls());
                }
                String decl = lowerOwner == null ? null : to.declaring(lowerOwner, row[3], row[4], true);
                if (decl == null) {
                    // the hook is missing on the way down but there at the bottom (NeoForge 21.0 has no writeClientSideData,
                    // 20.4 has its FriendlyByteBuf form): the bottom's
                    Stage0.Version bottom = versions.get(versions.size() - 1);
                    String bottomOwner = Stage0.mapClass(from, bottom, cur.cls());
                    if (bottomOwner == null && NEOFORGE_MOVES.containsKey(cur.cls())) {
                        bottomOwner = NEOFORGE_MOVES.get(cur.cls());
                    }
                    decl = bottomOwner == null ? null : bottom.declaring(bottomOwner, row[3], row[4], true);
                }
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

    /**
     * A member whose descriptor names classes R rows rename (NeoForge's ItemAbility, ToolAction below): itself, when the
     * bottom version has it with those classes renamed; else null.
     */
    static Stage0.Ref renamedInDescriptor(Stage0.Ref cur) {
        boolean method = cur.kind() == 'M';
        if (cur.desc() == null || NEOFORGE_MOVES.isEmpty() || method != cur.desc().startsWith("(") || !method && cur.kind() != 'F') {
            return null;
        }
        String desc = method ? remapper.mapMethodDesc(cur.desc()) : remapper.mapDesc(cur.desc());
        if (desc.equals(cur.desc())) {
            return null;
        }
        String owner = NEOFORGE_MOVES.getOrDefault(cur.cls(), cur.cls());
        return versions.get(versions.size() - 1).declaring(owner, cur.name(), desc, method) == null ? null : cur;
    }

    /** Carried code lives in its own package: what it calls below must be public (or it fails with IllegalAccessError). */
    static void checkAccess(Stage0.Ref end) {
        Stage0.Version bottom = versions.get(versions.size() - 1);
        Stage0.Cls info = bottom.classes.get(end.cls());
        if (info == null) {
            return;
        }
        Integer access = info.access.get(end.kind() + " " + end.name() + " " + end.desc());
        if (access != null && (access & Opcodes.ACC_PUBLIC) == 0 && (access & Opcodes.ACC_PROTECTED) == 0 && !madePublic(end)) {
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
        if (key.charAt(0) == 'M' && !redirects.containsKey(rest) && !constructorArguments.containsKey(rest)) {
            // (an override of a supertype's member an X row covers)
            int space = rest.indexOf(' '), dot = rest.lastIndexOf('.', space);
            return space > 0 && dot > 0 && inheritedX(new Stage0.Ref('M', rest.substring(0, dot), rest.substring(dot + 1, space), rest.substring(space + 1))) != null;
        }
        return key.charAt(0) == 'M' || key.charAt(0) == 'F' && fieldRedirects.containsKey(rest);
    }

    /** the B rows: carried classes whose registrations (a bootstrap into a registry) the adapters make when they should */
    static final Set<String> bootstrapped = new HashSet<>();

    /** a hard need a B row's class may have: its own registering */
    static boolean bootstrapNeed(String thing, String hard) {
        return hard.startsWith("registers: ") && thing.startsWith("C ") && bootstrapped.contains(thing.substring(2));
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
            if (n.hard.stream().allMatch(h -> resolvable(i, h) || bootstrapNeed(k, h))) {
                base.add(k);
            }
        });
        boolean shrunk = true;
        while (shrunk) {
            shrunk = false;
            for (var it = base.iterator(); it.hasNext();) {
                for (String d : plan.needs.get(it.next()).deps) {
                    if (plan.needs.containsKey(d) && !base.contains(d) && !provided.containsKey(d.substring(2)) && !ruled(d) && !addedBelow(i, d)) {
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
                    if (plan.needs.containsKey(d) && !ok.contains(d) && !ruled(d) && !addedBelow(i, d)) {
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
    /** a member ("F owner.name desc", "M ...") the adapters' mixins add to its class below: there for carried code */
    static boolean addedBelow(int i, String key) {
        if (!(key.startsWith("F ") || key.startsWith("M ")) || key.indexOf(' ', 2) < 0) {
            return false;
        }
        String member = key.substring(2, key.indexOf(' ', 2)), mdesc = key.substring(key.indexOf(' ', 2) + 1);
        int mdot = member.lastIndexOf('.');
        if (mdot < 0) {
            return false;
        }
        String owner = member.substring(0, mdot), mname = member.substring(mdot + 1);
        String below = i + 1 < versions.size() ? Stage0.mapClass(versions.get(i), versions.get(i + 1), owner) : null;
        below = below == null ? owner : below;
        return key.startsWith("F ") ? adapterAddedFields.contains(below + " " + mname + " " + mdesc) : adapterAddsMethod(below, mname, mdesc);
    }

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
        if (addedBelow(i, key)) {
            // a member the adapters' mixins add to the class below (1.21's OptionsSubScreen.list)
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
        if (kind == 'M' && defaultsRow(ref.cls() + "." + ref.name() + " " + ref.desc()) != null) {
            // a D row: the calls go to the lower method with its defaults
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
            if (access == null || (access & Opcodes.ACC_PUBLIC) != 0 || madePublic(end)) {
                // (public below, or made public by the adapters' access transformer)
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

    /**
     * The members the adapters' access transformer makes public below ("C owner", "F owner name", "M owner name desc";
     * the bottom game's names): carried code may reach them.
     */
    static final Set<String> madePublic = new HashSet<>();

    static void readAccessTransformer(String text) {
        for (String line : text.split("\\R")) {
            String l = line.contains("#") ? line.substring(0, line.indexOf('#')).trim() : line.trim();
            String[] parts = l.split("\\s+");
            if (parts.length < 2 || !parts[0].startsWith("public")) {
                continue;
            }
            String owner = parts[1].replace('.', '/');
            if (parts.length == 2) {
                madePublic.add("C " + owner);
            } else if (parts[2].contains("(")) {
                int open = parts[2].indexOf('(');
                madePublic.add("M " + owner + " " + parts[2].substring(0, open) + " " + parts[2].substring(open));
            } else {
                madePublic.add("F " + owner + " " + parts[2]);
            }
        }
    }

    static boolean madePublic(Stage0.Ref end) {
        return end.kind() == 'F' ? madePublic.contains("F " + end.cls() + " " + end.name())
                : madePublic.contains("M " + end.cls() + " " + end.name() + " " + end.desc());
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
        // (what the lower supertypes implement themselves, defaults of their interfaces too: 20.6's ContainerEventHandler
        // gives isFocused)
        Set<String> theirs = new HashSet<>();
        Deque<String> walk = new ArrayDeque<>(List.of(end == null ? supertype : end.cls()));
        Set<String> walked = new HashSet<>();
        while (!walk.isEmpty()) {
            String t = walk.poll();
            Stage0.Cls info = bottom.classes.get(t);
            if (info == null || !walked.add(t)) {
                continue;
            }
            for (var e : info.access.entrySet()) {
                String[] k = e.getKey().split(" ");
                if (k[0].equals("M") && (e.getValue() & Opcodes.ACC_ABSTRACT) == 0) {
                    theirs.add(k[1] + "/" + Type.getArgumentTypes(k[2]).length);
                }
            }
            walk.addAll(Arrays.asList(info.interfaces));
            if (info.superName != null) {
                walk.add(info.superName);
            }
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
                if (k[0].equals("M") && (e.getValue() & Opcodes.ACC_ABSTRACT) != 0 && !theirs.contains(k[1] + "/" + Type.getArgumentTypes(k[2]).length)
                        && !mine.contains(quietName(rung, t, k[1]) + "/" + Type.getArgumentTypes(k[2]).length)) {
                    if (Boolean.getBoolean("ladder.contracts")) {
                        System.err.println("contract: " + cls + " lacks " + t + "." + k[1] + k[2]);
                    }
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
    static final String CONVERSIONS = ADAPTERS + "bridge/Conversions";
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
        if (upper.getSort() == Type.VOID && lower.getSort() != Type.VOID) {
            // a result the upper member hasn't (1.20.6's MultiLineLabel.renderCentered gives its height): a call drops it;
            // an override or a lambda can't make one (overriddenReturn)
            return 14;
        }
        if (upper.getSort() == Type.FLOAT && lower.getSort() == Type.DOUBLE) {
            // 1.21's float where the lower member takes a double (a vertex's position)
            return 12;
        }
        if (upper.getSort() == Type.OBJECT && DELTA_TRACKERS.contains(upper.getInternalName()) && lower.getSort() == Type.FLOAT) {
            // 1.21's DeltaTracker where 1.20.4 passes the partial tick
            return 9;
        }
        if (upper.getSort() == Type.OBJECT && upper.getInternalName().equals(TRI_STATE) && lower.getSort() == Type.BOOLEAN) {
            // NeoForge 21's TriState where 20.6 answers yes or no (canSustainPlant: DEFAULT, the game decides, is no)
            return 18;
        }
        if (upper.getDescriptor().equals("Ljava/lang/Integer;") && lower.getDescriptor().equals("[F") && ADAPTERS.equals("ladder/rung1206/")) {
            // NeoForge 21's colour Integer (null: none) where 20.6 gives red, green, blue floats (a beacon's colour multiplier)
            return 19;
        }
        if (upper.getSort() == Type.INT && lower.getDescriptor().equals("[F") && ADAPTERS.equals("ladder/rung1206/")) {
            // 1.21's ARGB colour int where 1.20.6 gives red, green, blue floats (DyeColor, a beacon beam's section)
            return 17;
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
        if (PACK_LOCATIONS.contains(u) && l.equals("java/lang/String")) {
            // 1.20.5's pack location where 1.20.4 passes the pack's id (Packs: the id down, the location up)
            return 11;
        }
        if ((u.equals(REGISTRY_BUFFER) || u.equals(CARRIED + REGISTRY_BUFFER)) && l.equals(FRIENDLY_BUFFER)) {
            // 1.20.5's buffer with the registries where 1.20.4 passes its plain one (Payloads.registryBuffer wraps it)
            return 13;
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
        if (u.equals(ADAPTERS + "provided/MeshData") && l.equals(RENDERED_BUFFER)) {
            // 1.21's finished vertices, where the lower game passes its rendered buffer (the provided MeshData wraps it)
            return 16;
        }
        if (isSubtype(u, l)) {
            // the upper type is the lower one's subtype (1.21's MultiLineLabel.create of a Component, 1.20.6's of a
            // FormattedText): passed as it is, read cast
            return 15;
        }
        return -1;
    }

    static final String PACKS = ADAPTERS + "pack/Packs";
    static final String PACK_LOCATION = "net/minecraft/server/packs/PackLocationInfo";
    static final Set<String> PACK_LOCATIONS = Set.of(PACK_LOCATION, CARRIED + PACK_LOCATION);
    static final String RECIPE_INPUT_CONVERSIONS = ADAPTERS + "recipe/RecipeInputs";
    static final String REGISTRY_BUFFER = "net/minecraft/network/RegistryFriendlyByteBuf";
    static final String FRIENDLY_BUFFER = "net/minecraft/network/FriendlyByteBuf";
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

    static final Set<String> DELTA_TRACKERS = Set.of("net/minecraft/client/DeltaTracker", ADAPTERS + "provided/DeltaTracker");
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

    static final String RENDERED_BUFFER = "com/mojang/blaze3d/vertex/BufferBuilder$RenderedBuffer";
    static final String CALLBACK_RETURNABLE = "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
    /** the primitive number descriptors a callback's value converts between */
    static final String NUMBERS = "BSIJFD";

    static final String TRI_STATE = "net/neoforged/neoforge/common/util/TriState";

    static boolean convertible(Type upper, Type lower) {
        return conversion(upper, lower) >= 0;
    }

    /** whether the result of a mod's method (an override, a lambda) of the upper type is the lower member's */
    static boolean overriddenReturn(Type upper, Type lower) {
        int c = conversion(upper, lower);
        return c >= 0 && c != 14;
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
    /**
     * The member a mixin selector names, below, when it is the same method there (each rung's step finds it by its
     * mappings) but its parameters changed and no call bridge exists: the injector's target. Null when it isn't so.
     */
    static Stage0.Ref hookBelow(String owner, String name, String desc) {
        if (!Stage0.platform(owner)) {
            return null;
        }
        Stage0.Ref ref = resolve(owner, name, desc, true);
        if (ref == null || provided.containsKey(ref.cls())) {
            return null;
        }
        Stage0.Ref cur = ref;
        boolean changed = false;
        for (int i = level; i + 1 < versions.size(); i++) {
            Stage0.Step s = memberStep(versions.get(i), versions.get(i + 1), cur);
            switch (s.outcome()) {
                case SAME, RENAMED -> cur = s.to();
                case CHANGED -> {
                    cur = s.to();
                    changed = true;
                }
                default -> {
                    // (an M row: the same hook below under another descriptor, NeoForge 21's Gui.renderHotbar of the
                    // DeltaTracker where 20.6's private one takes the partial tick)
                    Stage0.Ref moved = movedMember(i, cur);
                    if (moved == null) {
                        return null;
                    }
                    cur = moved;
                    changed = true;
                }
            }
        }
        if (!changed || bridgeOf(owner, name, desc, true) != null) {
            return null;
        }
        return cur;
    }

    /**
     * A handler's parameters from the lower method's (the wrapper's direction): each upper parameter is a lower one in
     * order, converted up; a lower parameter the upper method hasn't (1.20.4's nanoTime long, its PoseStack) is dropped,
     * only when no upper parameter is of its type (so none is matched to the wrong one). Null when they don't map so.
     */
    static int[] handlerMap(Type[] up, Type[] low) {
        int[] map = new int[up.length];
        boolean[] used = new boolean[low.length];
        int k = 0;
        for (int j = 0; j < up.length; j++) {
            while (k < low.length && !convertible(up[j], low[k])) {
                k++;
            }
            if (k == low.length) {
                return null;
            }
            used[k] = true;
            map[j] = k++;
        }
        for (int i = 0; i < low.length; i++) {
            if (!used[i]) {
                for (Type u : up) {
                    if (convertible(u, low[i])) {
                        return null;
                    }
                }
            }
        }
        return map;
    }

    static int[] paramMap(Type[] up, Type[] low) {
        // the lower member may take more only where a colour int is its four floats (colorAt)
        if (low.length > up.length + 3) {
            return null;
        }
        int[] map = new int[up.length];
        int k = 0;
        for (int j = 0; j < up.length; j++) {
            boolean context = up[j].getSort() == Type.OBJECT && CONTEXT_PARAMETERS.contains(up[j].getInternalName());
            int channels = colorChannels(up, j, low, k);
            if (channels > 0) {
                // 1.21's ARGB colour int where the lower member takes red, green, blue (, alpha) floats: -(k + 2)
                map[j] = -(k + 2);
                k += channels;
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
        return colorChannels(up, j, low, k) > 0;
    }

    /**
     * The lower floats a colour int of the upper member at j is, at k: 4 (red, green, blue, alpha), 3 (red, green, blue:
     * 1.20.6's RenderLayer.renderColoredCutoutModel, opaque) or 0 (not a colour), the rest of both lining up.
     */
    static int colorChannels(Type[] up, int j, Type[] low, int k) {
        if (up[j].getSort() != Type.INT) {
            return 0;
        }
        for (int n = 4; n >= 3; n--) {
            if (k + n > low.length || up.length - j - 1 != low.length - k - n) {
                continue;
            }
            boolean floats = true;
            for (int c = 0; c < n; c++) {
                floats &= low[k + c].getSort() == Type.FLOAT;
            }
            if (floats) {
                return n;
            }
        }
        return 0;
    }

    /** the colour int in the local at slot, as its floats (red, green, blue, and alpha of 4 channels) on the stack */
    static void unpackColor(InsnList code, int slot, int channels) {
        for (String channel : Arrays.copyOf(new String[] {"red", "green", "blue", "alpha"}, channels)) {
            code.add(new VarInsnNode(Opcodes.ILOAD, slot));
            code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, channel, "(I)F", false));
        }
    }

    /** the floats in the locals at slots[k..] (red, green, blue, alpha; opaque of 3 channels), as the colour int on the stack */
    static void packColor(InsnList code, int[] slots, int k, int channels) {
        for (int c = 0; c < channels; c++) {
            code.add(new VarInsnNode(Opcodes.FLOAD, slots[k + c]));
        }
        if (channels == 3) {
            code.add(new LdcInsnNode(1.0F));
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
            case 14 -> throw new IllegalStateException("no " + lower + " result of a void method");
            case 16 -> code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, ADAPTERS + "provided/MeshData", "buffer", "()L" + RENDERED_BUFFER + ";", false));
            case 17 -> code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ADAPTERS + "misc/SmallAdapters", "rgb", "(I)[F", false));
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
            case 11 -> code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, PACKS, "id", "(Ljava/lang/Object;)Ljava/lang/String;", false));
            case 13 -> {
                // a registry buffer is a FriendlyByteBuf: passed as it is
            }
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
            case 12 -> code.add(new InsnNode(Opcodes.F2D));
            case 19 -> {
                checkAdapter(ADAPTERS + "misc/SmallAdapters", "rgbOrNull", "(Ljava/lang/Integer;)[F", "floats of an Integer colour");
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ADAPTERS + "misc/SmallAdapters", "rgbOrNull", "(Ljava/lang/Integer;)[F", false));
            }
            case 18 -> {
                // (an adapter's: 20.4's TriState has no isTrue)
                checkAdapter(CONVERSIONS, "isTrue", "(L" + TRI_STATE + ";)Z", "a boolean of a TriState");
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "isTrue", "(L" + TRI_STATE + ";)Z", false));
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
            boolean defaultedProtected = defaultedLower(upper.cls() + "." + upper.name() + " " + upper.desc(), lower.desc()) != null;
            // (a D row's method whose lower one returns nothing: its callers get zero, ServerPlaceRecipe.moveItemToGrid)
            boolean noResult = defaultedProtected && Type.getReturnType(lower.desc()).getSort() == Type.VOID;
            if (!defaultedProtected && paramMap(ua, la) == null || !noResult && !convertible(Type.getReturnType(upper.desc()), Type.getReturnType(lower.desc()))) {
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
        boolean defaulted = defaultedLower(upper.cls() + "." + upper.name() + " " + upper.desc(), lower.desc()) != null;
        if (!defaulted && paramMap(ua, la) == null || !convertible(Type.getReturnType(upper.desc()), Type.getReturnType(lower.desc()))) {
            return null;
        }
        String[] bridge = {owner, upper.name() + "$" + Integer.toHexString(upper.desc().hashCode())};
        schedule(rung + 1, new Stage0.Ref('B', owner, bridge[1], job));
        bridgeEnds.put(owner + "." + bridge[1], end);
        return bridge;
    }

    /** the owner of an override-only bridge record (no class: the calls take an X row) */
    static final String OVERRIDE_ONLY = "ladder/override/";

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
    /** the NEW a constructor call initializes (DUPed), or null: a super(...) or this(...) call, or an unusual new */
    static AbstractInsnNode newOf(MethodInsnNode call) {
        int depth = 0;
        for (AbstractInsnNode back = call.getPrevious(); back != null; back = back.getPrevious()) {
            if (back instanceof MethodInsnNode other && other.getOpcode() == Opcodes.INVOKESPECIAL && other.name.equals("<init>")
                    && other.owner.equals(call.owner)) {
                depth++;
            } else if (back instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW && type.desc.equals(call.owner)) {
                if (depth == 0) {
                    AbstractInsnNode dup = back.getNext();
                    return dup != null && dup.getOpcode() == Opcodes.DUP ? back : null;
                }
                depth--;
            }
        }
        return null;
    }

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
    /**
     * A mixin's {@code Interface.super.m()} call (invokespecial of an interface's default method). Mixin below 0.8.7 binds
     * every invokespecial of a mixin whose target inherits from a mixed class to the target's superclass: the call becomes
     * an interface method reference of a class, which fails verification ("indirect superinterface"). The default
     * method's body is copied into the mixin as a private method, and the call goes there.
     */
    static void interfaceSuperCalls(ClassNode node, Map<String, byte[]> entries) {
        if ((node.access & Opcodes.ACC_INTERFACE) != 0 || node.interfaces == null || node.interfaces.isEmpty()) {
            return;
        }
        Map<String, MethodNode> copies = new LinkedHashMap<>();
        String simple = node.name.substring(node.name.lastIndexOf('/') + 1).replace('$', '_');
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode insn : m.instructions) {
                if (!(insn instanceof MethodInsnNode call) || call.getOpcode() != Opcodes.INVOKESPECIAL || !call.itf
                        || call.owner.equals(node.name)) {
                    continue;
                }
                String key = call.owner + "." + call.name + call.desc;
                MethodNode copy = copies.get(key);
                if (copy == null && !copies.containsKey(key)) {
                    copy = defaultBodyCopy(call.owner, call.name, call.desc, entries, "ladder$super$" + simple + "$" + call.name, new HashSet<>());
                    copies.put(key, copy);
                    if (copy == null && superclassDeclares(mixinTargets.getOrDefault(node.name, List.of()), call.name, call.desc, entries)) {
                        problem("mixin " + node.name + " calls " + key + " as an interface super method, which Mixin below 0.8.7"
                                + " binds to the target's superclass (the default body can not be copied)");
                    }
                }
                if (copy != null) {
                    call.owner = node.name;
                    call.name = copy.name;
                    call.itf = false;
                }
            }
        }
        for (MethodNode copy : copies.values()) {
            if (copy != null && node.methods.stream().noneMatch(x -> x.name.equals(copy.name) && x.desc.equals(copy.desc))) {
                node.methods.add(copy);
            }
        }
    }

    /** whether a superclass of one of the targets declares the method (Mixin below 0.8.7 then binds a super call there) */
    static boolean superclassDeclares(List<String> targets, String name, String desc, Map<String, byte[]> entries) {
        for (String target : targets) {
            ClassNode c = classBelow(target, entries);
            Set<String> seen = new HashSet<>();
            while (c != null && c.superName != null && seen.add(c.superName)) {
                c = classBelow(c.superName, entries);
                if (c != null && c.methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.equals(desc) && (m.access & Opcodes.ACC_STATIC) == 0)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** a class as the bottom game or the mod has it */
    static ClassNode classBelow(String name, Map<String, byte[]> entries) {
        byte[] data = entries.get(name + ".class");
        if (data != null) {
            ClassNode c = new ClassNode();
            new ClassReader(data).accept(c, ClassReader.SKIP_CODE);
            return c;
        }
        return bottomClass(name);
    }

    /** a copy of interface {@code owner}'s default method (or a superinterface's), if its body uses nothing private of the interface */
    static MethodNode defaultBodyCopy(String owner, String name, String desc, Map<String, byte[]> entries, String newName, Set<String> seen) {
        byte[] data = entries.get(owner + ".class");
        if (data == null || !seen.add(owner)) {
            return null;
        }
        ClassNode itf = new ClassNode();
        new ClassReader(data).accept(itf, 0);
        MethodNode found = itf.methods.stream().filter(x -> x.name.equals(name) && x.desc.equals(desc)).findFirst().orElse(null);
        if (found == null) {
            for (String sup : itf.interfaces) {
                MethodNode c = defaultBodyCopy(sup, name, desc, entries, newName, seen);
                if (c != null) {
                    return c;
                }
            }
            return null;
        }
        if ((found.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC)) != 0 || found.instructions.size() == 0) {
            return null;
        }
        Set<String> privates = new HashSet<>();
        for (MethodNode x : itf.methods) {
            if ((x.access & Opcodes.ACC_PRIVATE) != 0) {
                privates.add(x.name + x.desc);
            }
        }
        for (AbstractInsnNode insn : found.instructions) {
            if (insn instanceof MethodInsnNode c && c.owner.equals(owner) && privates.contains(c.name + c.desc)) {
                return null;
            }
            if (insn instanceof InvokeDynamicInsnNode indy) {
                for (Object a : indy.bsmArgs) {
                    if (a instanceof Handle h && h.getOwner().equals(owner) && privates.contains(h.getName() + h.getDesc())) {
                        return null;
                    }
                }
            }
        }
        MethodNode copy = new MethodNode(Opcodes.ACC_PRIVATE, newName, desc, found.signature,
                found.exceptions == null ? null : found.exceptions.toArray(new String[0]));
        found.accept(copy);
        copy.access = Opcodes.ACC_PRIVATE;
        copy.name = newName;
        copy.visibleAnnotations = null;
        copy.invisibleAnnotations = null;
        copy.visibleParameterAnnotations = null;
        copy.invisibleParameterAnnotations = null;
        return copy;
    }

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
        // (a D row's method: the lower parameters are upper ones or the row's defaults, of the receiver too)
        String[] row = null;
        if (map == null) {
            for (String start : starts(call.owner)) {
                Stage0.Ref ref = resolve(start, call.name, call.desc, true);
                if (ref != null && defaultedLower(ref.cls() + "." + call.name + " " + call.desc, end.desc()) != null) {
                    row = defaultsRow(ref.cls() + "." + call.name + " " + call.desc);
                    break;
                }
            }
        }
        if (map == null && row == null || !convertible(upRet, lowRet) && !(row != null && lowRet.getSort() == Type.VOID)) {
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
        if (row != null) {
            String[] spec = specTokens(row[1]);
            int[] from = defaultedLower(row[2] + "." + call.name + " " + call.desc, end.desc());
            if (from == null) {
                from = new int[spec.length];
                for (int j = 0; j < spec.length; j++) {
                    from[j] = spec[j].startsWith("$") ? Integer.parseInt(spec[j].substring(1)) : -1;
                }
            }
            int[] slots = new int[up.length];
            for (int a = 0; a < up.length; a++) {
                slots[a] = slot;
                slot += up[a].getSize();
            }
            for (int j = 0; j < low.length; j++) {
                if (from[j] >= 0) {
                    code.add(new VarInsnNode(up[from[j]].getOpcode(Opcodes.ILOAD), slots[from[j]]));
                    convertDown(code, up[from[j]], low[j]);
                } else {
                    pushDefault(code, spec[j], low[j], up, slots, isStatic ? null : remapper.map(row[2]));
                }
            }
            map = new int[0];
            up = new Type[0];
        }
        for (int a = 0; a < up.length; a++) {
            if (map[a] >= 0) {
                code.add(new VarInsnNode(up[a].getOpcode(Opcodes.ILOAD), slot));
                convertDown(code, up[a], low[map[a]]);
            } else if (map[a] <= -2) {
                unpackColor(code, slot, colorChannels(up, a, low, -map[a] - 2));
            }
            slot += up[a].getSize();
        }
        code.add(new MethodInsnNode(isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKEVIRTUAL, isStatic ? end.cls() : remapper.map(call.owner),
                end.name(), end.desc(), false));
        if (lowRet.getSort() == Type.VOID && upRet.getSort() != Type.VOID) {
            pushZero(code, upRet);
        } else {
            convertUp(code, lowRet, upRet);
        }
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
        String defaultsKey = call.owner + "." + call.name + " " + call.desc;
        if (map == null && defaultsRow(defaultsKey) == null) {
            // a mod class between (AetherAnimal under Aether's MountableAnimal): the game class it reaches
            for (String start : starts(call.owner)) {
                Stage0.Ref platformRef = resolve(start, call.name, call.desc, true);
                if (platformRef != null && defaultsRow(platformRef.cls() + "." + call.name + " " + call.desc) != null) {
                    defaultsKey = platformRef.cls() + "." + call.name + " " + call.desc;
                    break;
                }
            }
        }
        int[] from = map == null ? defaultedLower(defaultsKey, end.desc()) : null;
        if (map == null && from == null || !convertible(upRet, lowRet) && !(from != null && lowRet.getSort() == Type.VOID)) {
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
        if (from != null) {
            // a D row: the lower parameters in their order, each an upper one or the row's default
            String[] row = defaultsRow(defaultsKey);
            String[] spec = specTokens(row[1]);
            int[] slots = new int[up.length];
            for (int a = 0; a < up.length; a++) {
                slots[a] = slot;
                slot += up[a].getSize();
            }
            for (int j = 0; j < low.length; j++) {
                if (from[j] >= 0) {
                    code.add(new VarInsnNode(up[from[j]].getOpcode(Opcodes.ILOAD), slots[from[j]]));
                    convertDown(code, up[from[j]], low[j]);
                } else {
                    pushDefault(code, spec[j], low[j], up, slots, remapper.map(row[2]));
                }
            }
            map = new int[0];
            up = new Type[0];
        }
        for (int a = 0; a < up.length; a++) {
            if (map[a] >= 0) {
                code.add(new VarInsnNode(up[a].getOpcode(Opcodes.ILOAD), slot));
                convertDown(code, up[a], low[map[a]]);
            } else if (map[a] <= -2) {
                unpackColor(code, slot, colorChannels(up, a, low, -map[a] - 2));
            }
            slot += up[a].getSize();
        }
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, remapper.map(call.owner), end.name(), end.desc(), false));
        if (lowRet.getSort() == Type.VOID && upRet.getSort() != Type.VOID) {
            pushZero(code, upRet);
        } else {
            convertUp(code, lowRet, upRet);
        }
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
            // (a mixin's method: its bridge is added to the targets with it, when none of them declares the lower method)
            MethodNode g = end == null || mixinTargets.containsKey(node.name) && !bridgeableMixin(node, end, itf) ? null : overrideBridge(node, m, end, itf);
            if (g == null) {
                problem("an override of " + ref.key() + ", whose parameters changed below: no override bridge for it (it would never be called)");
                continue;
            }
            made.add(g);
        }
        return made;
    }

    /** A lambda of a mod method (its ordinal among the method's lambdas) whose interface method changed below. */
    record LambdaFix(String method, int ordinal, String upperSam, Handle impl, Stage0.Ref end) {
    }

    static boolean lambdaFactory(AbstractInsnNode insn) {
        return insn instanceof InvokeDynamicInsnNode indy && indy.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")
                && indy.bsmArgs.length >= 3 && indy.bsmArgs[0] instanceof Type && indy.bsmArgs[1] instanceof Handle;
    }

    /**
     * Lambdas and method references implementing a game interface whose method changed below (an M row's hook, or its
     * parameters' types): the game calls the lower method, so the lambda is made for it, through a static bridge of the
     * class that converts the values up and calls the implementation (applyLambdaFixes, after the remap). When the
     * values don't convert it is a problem: the lambda would never run.
     */
    static List<LambdaFix> lambdaBridges(ClassNode node) {
        List<LambdaFix> fixes = new ArrayList<>();
        for (MethodNode m : node.methods) {
            int ordinal = 0;
            for (AbstractInsnNode insn : m.instructions) {
                if (!lambdaFactory(insn)) {
                    continue;
                }
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                int here = ordinal++;
                String face = Type.getReturnType(indy.desc).getInternalName();
                if (!Stage0.platform(face)) {
                    continue;
                }
                String sam = ((Type) indy.bsmArgs[0]).getDescriptor();
                Stage0.Ref ref = resolve(face, indy.name, sam, true);
                if (ref == null) {
                    continue;
                }
                walk(ref);
                String[] bridge = callRedirect.get(level + " " + ref.key());
                Stage0.Ref end = bridge == null ? null : bridgeEnds.get(bridge[0] + "." + bridge[1]);
                if (end == null) {
                    continue;
                }
                Type[] up = Type.getArgumentTypes(remapper.mapMethodDesc(sam)), low = Type.getArgumentTypes(end.desc());
                if (paramMap(up, low) == null || !overriddenReturn(Type.getReturnType(remapper.mapMethodDesc(sam)), Type.getReturnType(end.desc()))) {
                    problem("a lambda of " + ref.key() + ", whose parameters changed below: no bridge for it (it would never be called)");
                    continue;
                }
                fixes.add(new LambdaFix(remapper.mapMethodName(node.name, m.name, m.desc) + remapper.mapMethodDesc(m.desc), here, sam,
                        (Handle) indy.bsmArgs[1], end));
            }
        }
        return fixes;
    }

    /** The lambdas lambdaBridges found, made for the lower interface method through their bridges (in the remapped class). */
    static void applyLambdaFixes(ClassNode node, List<LambdaFix> fixes) {
        int n = 0;
        boolean itf = (node.access & Opcodes.ACC_INTERFACE) != 0;
        for (LambdaFix fix : fixes) {
            for (MethodNode m : node.methods) {
                if (!(m.name + m.desc).equals(fix.method())) {
                    continue;
                }
                int ordinal = 0;
                for (AbstractInsnNode insn : m.instructions) {
                    if (!lambdaFactory(insn) || ordinal++ != fix.ordinal()) {
                        continue;
                    }
                    InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) insn;
                    MethodNode bridge = lambdaBridge("ladder$lambda$" + n++, fix, Type.getArgumentTypes(indy.desc), (Handle) indy.bsmArgs[1],
                            (Type) indy.bsmArgs[2]);
                    node.methods.add(bridge);
                    Type lower = Type.getMethodType(fix.end().desc());
                    indy.bsmArgs[0] = lower;
                    indy.bsmArgs[1] = new Handle(Opcodes.H_INVOKESTATIC, node.name, bridge.name, bridge.desc, itf);
                    indy.bsmArgs[2] = lower;
                    break;
                }
                break;
            }
        }
    }

    /** A lambda's bridge: (captured values, the lower method's parameters) -> the implementation, its values converted. */
    static MethodNode lambdaBridge(String name, LambdaFix fix, Type[] captured, Handle h, Type instantiated) {
        // (the remapped call site's implementation: the rules may have moved it to an adapter or a bridge; its
        // instantiated types, more specific than the erased interface method's, are what the implementation takes)
        String upSam = remapper.mapMethodDesc(fix.upperSam());
        Type[] erased = Type.getArgumentTypes(upSam), low = Type.getArgumentTypes(fix.end().desc());
        Type[] up = instantiated.getArgumentTypes().length == erased.length ? instantiated.getArgumentTypes() : erased;
        Type upRet = Type.getReturnType(upSam), lowRet = Type.getReturnType(fix.end().desc());
        int[] map = paramMap(erased, low);
        Type[] params = new Type[captured.length + low.length];
        System.arraycopy(captured, 0, params, 0, captured.length);
        System.arraycopy(low, 0, params, captured.length, low.length);
        MethodNode g = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, name, Type.getMethodDescriptor(lowRet, params), null, null);
        InsnList code = g.instructions;
        String owner = h.getOwner(), implName = h.getName(), implDesc = h.getDesc();
        if (h.getTag() == Opcodes.H_NEWINVOKESPECIAL) {
            code.add(new TypeInsnNode(Opcodes.NEW, owner));
            code.add(new InsnNode(Opcodes.DUP));
        }
        int slot = 0;
        for (Type c : captured) {
            code.add(new VarInsnNode(c.getOpcode(Opcodes.ILOAD), slot));
            slot += c.getSize();
        }
        int[] slots = new int[low.length];
        for (int a = 0; a < low.length; a++) {
            slots[a] = slot;
            slot += low[a].getSize();
        }
        for (int a = 0; a < up.length; a++) {
            if (map[a] >= 0) {
                code.add(new VarInsnNode(low[map[a]].getOpcode(Opcodes.ILOAD), slots[map[a]]));
                convertUp(code, low[map[a]], up[a]);
                if (up[a].getSort() == Type.OBJECT && !up[a].equals(low[map[a]]) && conversion(up[a], low[map[a]]) <= 0) {
                    // the instantiated type, more specific than the erased one the value has
                    code.add(new TypeInsnNode(Opcodes.CHECKCAST, up[a].getInternalName()));
                }
            } else if (map[a] <= -2) {
                packColor(code, slots, -map[a] - 2, colorChannels(up, a, low, -map[a] - 2));
            } else {
                supplyContext(code, up[a]);
            }
        }
        switch (h.getTag()) {
            case Opcodes.H_INVOKESTATIC -> code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, implName, implDesc, h.isInterface()));
            case Opcodes.H_INVOKEINTERFACE -> code.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, owner, implName, implDesc, true));
            case Opcodes.H_INVOKESPECIAL, Opcodes.H_NEWINVOKESPECIAL -> code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, owner, implName, implDesc, h.isInterface()));
            default -> code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, implName, implDesc, h.isInterface()));
        }
        Type implRet = h.getTag() == Opcodes.H_NEWINVOKESPECIAL ? Type.getObjectType(owner) : Type.getReturnType(implDesc);
        if (implRet.getSort() != Type.VOID && upRet.getSort() == Type.VOID) {
            code.add(new InsnNode(implRet.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
        } else {
            convertDown(code, upRet, lowRet);
        }
        code.add(new InsnNode(lowRet.getOpcode(Opcodes.IRETURN)));
        g.maxLocals = slot;
        g.maxStack = slot + 6 + 2 * up.length;
        return g;
    }

    /**
     * Whether a mixin class's override may get its override bridge: a class mixin whose targets are game classes that
     * don't declare the lower method themselves (the bridge is then a method the mixin adds, never one it replaces).
     */
    static boolean bridgeableMixin(ClassNode node, Stage0.Ref end, boolean itf) {
        if (itf) {
            // an interface mixin into a mod's own interfaces (Zeta's block extensions): its bridge is a default there
            List<String> targets = mixinTargets.getOrDefault(node.name, List.of());
            return !targets.isEmpty() && targets.stream().allMatch(mod::containsKey);
        }
        Stage0.Version bottom = versions.get(versions.size() - 1);
        for (String t : mixinTargets.getOrDefault(node.name, List.of())) {
            if (mod.containsKey(t)) {
                // a mod's class (supplementaries' mixin into its own planter): converted with the same bridges, the
                // mixin's bridge replaces the class's own when it has one
                continue;
            }
            Stage0.Cls info = bottom.classes.get(remapper.map(t));
            if (!Stage0.platform(t) || info == null || info.methods.contains(end.name() + " " + end.desc())) {
                return false;
            }
        }
        return true;
    }

    /** The lower method of an override bridge, in the bottom version's names; null when the values don't convert. */
    static MethodNode overrideBridge(ClassNode node, MethodNode m, Stage0.Ref end, boolean itf) {
        String ownName = remapper.mapMethodName(node.name, m.name, m.desc), ownDesc = remapper.mapMethodDesc(m.desc);
        Type[] up = Type.getArgumentTypes(ownDesc), low = Type.getArgumentTypes(end.desc());
        Type upRet = Type.getReturnType(ownDesc), lowRet = Type.getReturnType(end.desc());
        int[] map = paramMap(up, low);
        if (map == null) {
            // a D row's method (it takes more below): each upper parameter is the lower one it is passed as
            map = defaultedOverride(node.name, m.name, m.desc, end.desc(), up.length);
        }
        // else an A row's: each upper parameter made of the lower ones (or of the object: a level it is in)
        String[] planRow = map == null ? argumentPlanRow(ownDesc, end, m.name) : null;
        String[] plan = planRow == null ? null : planRow[4].trim().split(" +");
        if (plan != null && plan.length != up.length) {
            problem("an A row's plan for " + end.key() + " has " + plan.length + " arguments, not " + up.length);
            plan = null;
        }
        boolean zeroResult = upRet.getSort() == Type.VOID && NUMBERS.indexOf(lowRet.getDescriptor()) >= 0 && lowRet.getDescriptor().length() == 1;
        // (a result the lower hook hasn't: dropped; 1.21's moveItemToGrid gives the count left, 1.20.6's nothing)
        boolean dropResult = lowRet.getSort() == Type.VOID && upRet.getSort() != Type.VOID;
        if (map == null && plan == null || !overriddenReturn(upRet, lowRet) && !zeroResult && !dropResult) {
            return null;
        }
        // (a TriState answer, 1.21's canSustainPlant: DEFAULT is the game's own decision, the superclass's lower method;
        // a class whose superclass the bridge can't call so has none. A class mixin's super call is bound to its
        // target's superclass by Mixin; an interface's default would lose to Block's own method below anyway)
        boolean decide = conversion(upRet, lowRet) == 18;
        if (decide && (itf || node.superName == null || node.superName.equals("java/lang/Object"))) {
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
            if (plan != null) {
                planArgument(code, plan[a], low, slots, up[a], 0, remapper.map(planRow[0]));
            } else if (map[a] >= 0) {
                code.add(new VarInsnNode(low[map[a]].getOpcode(Opcodes.ILOAD), slots[map[a]]));
                // (a javac bridge's erased recipe input: made of the type the method casts it to)
                convertUp(code, low[map[a]], wanted[a] != null ? wanted[a] : up[a]);
            } else if (map[a] <= -2) {
                packColor(code, slots, -map[a] - 2, colorChannels(up, a, low, -map[a] - 2));
            } else {
                supplyContext(code, up[a]);
            }
        }
        code.add(new MethodInsnNode(itf ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, remapper.map(node.name), ownName, ownDesc, itf));
        if (decide) {
            // the superclass's answer too (no branch: the classes are written without computed frames), then the
            // mod's unless DEFAULT
            code.add(new VarInsnNode(Opcodes.ALOAD, 0));
            for (int a = 0; a < low.length; a++) {
                code.add(new VarInsnNode(low[a].getOpcode(Opcodes.ILOAD), slots[a]));
            }
            code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, remapper.map(node.superName), end.name(), end.desc(), false));
            checkAdapter(CONVERSIONS, "decide", "(L" + TRI_STATE + ";Z)Z", "an override bridge of " + end.key());
            code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "decide", "(L" + TRI_STATE + ";Z)Z", false));
        } else if (dropResult) {
            code.add(new InsnNode(upRet.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
        } else if (zeroResult) {
            // the upper method gives nothing the lower one returns a number of (1.21's IConfigSpec.correct, 1.20.6's
            // count of corrections): none
            switch (lowRet.getSort()) {
                case Type.LONG -> code.add(new InsnNode(Opcodes.LCONST_0));
                case Type.FLOAT -> code.add(new InsnNode(Opcodes.FCONST_0));
                case Type.DOUBLE -> code.add(new InsnNode(Opcodes.DCONST_0));
                default -> code.add(new InsnNode(Opcodes.ICONST_0));
            }
        } else {
            convertDown(code, upRet, lowRet);
        }
        if (end.name().equals("getRemainingItems") && low.length == 1 && low[0].getInternalName().equals(CONTAINER)
                && lowRet.getInternalName().equals("net/minecraft/core/NonNullList")) {
            // the mod's remainders are of its trimmed input: 1.20.4 takes them slot by slot of the whole grid
            code.add(new VarInsnNode(Opcodes.ALOAD, slots[0]));
            code.add(new InsnNode(Opcodes.SWAP));
            code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, RECIPE_INPUT_CONVERSIONS, "remainders",
                    "(L" + CONTAINER + ";Lnet/minecraft/core/NonNullList;)Lnet/minecraft/core/NonNullList;", false));
        }
        code.add(new InsnNode(lowRet.getOpcode(Opcodes.IRETURN)));
        g.maxLocals = slot;
        g.maxStack = slot + 4 + 2 * up.length + (decide ? slot + 1 : 0);
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

    /**
     * For an override (in a mod class) of a method a D row covers: upper parameter -> the lower one, by the row (the
     * lower-only parameters, the row's defaults, aren't the override's); null when no row covers it.
     */
    static int[] defaultedOverride(String modClass, String name, String upperDesc, String lowerDesc, int upCount) {
        for (String start : starts(modClass)) {
            Stage0.Ref ref = resolve(start, name, upperDesc, true);
            int[] from = ref == null ? null : defaultedLower(ref.cls() + "." + name + " " + upperDesc, lowerDesc);
            if (from != null) {
                int[] map = new int[upCount];
                java.util.Arrays.fill(map, -1);
                for (int j = 0; j < from.length; j++) {
                    if (from[j] >= 0 && from[j] < upCount) {
                        map[from[j]] = j;
                    }
                }
                for (int i : map) {
                    if (i < 0) {
                        return null;
                    }
                }
                return map;
            }
        }
        return null;
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
        } else if (defaultedLower(d[1] + "." + d[2] + " " + upperDesc, lowerDesc) != null) {
            // a D row: the lower parameters are upper ones or the row's defaults
            Type[] ua = Type.getArgumentTypes(upperDesc), la = Type.getArgumentTypes(lowerDesc);
            int[] from = defaultedLower(d[1] + "." + d[2] + " " + upperDesc, lowerDesc);
            String[] row = defaultsRow(d[1] + "." + d[2] + " " + upperDesc);
            String[] spec = specTokens(row[1]);
            int[] slots = new int[ua.length];
            StringBuilder b = new StringBuilder("(");
            if (!lowerStatic) {
                b.append("L").append(lowerOwner).append(";");
                locals = 1;
            }
            for (int a = 0; a < ua.length; a++) {
                b.append(ua[a].getDescriptor());
                slots[a] = locals;
                locals += ua[a].getSize();
            }
            if (!lowerStatic) {
                code.add(new VarInsnNode(Opcodes.ALOAD, 0));
            }
            for (int j = 0; j < la.length; j++) {
                if (from[j] >= 0) {
                    code.add(new VarInsnNode(ua[from[j]].getOpcode(Opcodes.ILOAD), slots[from[j]]));
                    convertDown(code, ua[from[j]], la[j]);
                } else {
                    pushDefault(code, spec[j], la[j], ua, slots, lowerStatic ? null : remapper.map(row[2]));
                }
                stack += la[j].getSize();
            }
            Type upperReturn = Type.getReturnType(upperDesc), lowerReturn = Type.getReturnType(lowerDesc);
            desc = b.append(")").append(upperReturn.getDescriptor()).toString();
            code.add(new MethodInsnNode(lowerStatic ? Opcodes.INVOKESTATIC : lowerInterface ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL,
                    lowerOwner, lowerName, lowerDesc, lowerInterface));
            convertUp(code, lowerReturn, upperReturn);
            code.add(new InsnNode(upperReturn.getOpcode(Opcodes.IRETURN)));
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
                    unpackColor(code, locals, colorChannels(ua, a, la, -map[a] - 2));
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
            case 14 -> code.add(new InsnNode(lower.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
            case 15 -> code.add(new TypeInsnNode(Opcodes.CHECKCAST, upper.getInternalName()));
            case 17 -> code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ADAPTERS + "misc/SmallAdapters", "argb", "([F)I", false));
            case 19 -> {
                checkAdapter(ADAPTERS + "misc/SmallAdapters", "argbOrNull", "([F)Ljava/lang/Integer;", "an Integer colour of floats");
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ADAPTERS + "misc/SmallAdapters", "argbOrNull", "([F)Ljava/lang/Integer;", false));
            }
            case 18 -> {
                checkAdapter(CONVERSIONS, "triState", "(Z)L" + TRI_STATE + ";", "a TriState of a boolean");
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "triState", "(Z)L" + TRI_STATE + ";", false));
            }
            case 16 -> code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "meshData",
                    "(L" + RENDERED_BUFFER + ";)L" + ADAPTERS + "provided/MeshData;", false));
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
            case 12 -> code.add(new InsnNode(Opcodes.D2F));
            case 13 -> code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ADAPTERS + "network/Payloads", "registryBuffer",
                    "(L" + FRIENDLY_BUFFER + ";)L" + CARRIED + REGISTRY_BUFFER + ";", false));
            case 11 -> {
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, PACKS, "location", "(Ljava/lang/String;)Ljava/lang/Object;", false));
                code.add(new TypeInsnNode(Opcodes.CHECKCAST, CARRIED + PACK_LOCATION));
            }
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
                if (provided.containsKey(internal)) {
                    // a class the adapters provide (a P row): its name below is theirs, never carried
                    return provided.get(internal);
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
                    // a carried class is unsealed: its permitted ones may stay behind (FML 4's ILoadedConfig, whose
                    // LoadedConfig is the loader's own), and the adapters may have to give its values
                    source.permittedSubclasses = null;
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
        // each rung's carried library is a module of its own (a pack holds several: rung 1's goes down with its mods)
        manifest.getMainAttributes().putValue("Automatic-Module-Name", CARRIED.substring(0, CARRIED.length() - 1).replace('/', '.'));
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

    /** a bottom game class as the game has it once the adapters' shape patches are applied (the mixin checks' view) */
    @SuppressWarnings("unchecked")
    static ClassNode bottomClass(String name) {
        ClassNode node = parsed(bottomClasses, versions.get(versions.size() - 1), name);
        if (node == null && name.startsWith(CARRIED)) {
            // a class carried down (1.21's enchantment effects, NeoForge 21's configuration screen): a mixin into it lands on
            // the carried copy, whose code is the upper one's in the converted class names
            return carriedViews.computeIfAbsent(name, n -> {
                ClassNode done = carriedOut.get(n);
                if (done != null) {
                    return Optional.of(done);
                }
                ClassNode top = topClass(n.substring(CARRIED.length()));
                if (top == null) {
                    return Optional.empty();
                }
                ClassNode view = new ClassNode();
                top.accept(new ClassRemapper(view, new org.objectweb.asm.commons.Remapper() {
                    @Override
                    public String map(String internalName) {
                        return remapper.map(internalName);
                    }
                }));
                return Optional.of(view);
            }).orElse(null);
        }
        if (node == null && adapterBytes.containsKey(name)) {
            // a class the adapters provide (a P row's): a mixin into the upper game's class lands on it below
            return adapterClasses.computeIfAbsent(name, n -> {
                ClassNode c = new ClassNode();
                new ClassReader(adapterBytes.get(n)).accept(c, ClassReader.EXPAND_FRAMES);
                return Optional.of(c);
            }).orElse(null);
        }
        List<MethodNode> addedHere = adapterAdded.get(name);
        if (node != null && addedHere != null && addedClasses.add(name)) {
            for (MethodNode a : addedHere) {
                if (node.methods.stream().noneMatch(m -> m.name.equals(a.name) && m.desc.equals(a.desc))) {
                    node.methods.add(a);
                }
            }
        }
        if (node != null && addedFieldClasses.add(name)) {
            // (the fields the adapters' mixins add: a mod's mixin may shadow them, 1.21's loot functions' enchantment)
            for (String added : adapterAddedFields) {
                String[] parts = added.split(" ");
                if (parts.length == 3 && parts[0].equals(name) && node.fields.stream().noneMatch(f -> f.name.equals(parts[1]) && f.desc.equals(parts[2]))) {
                    node.fields.add(new FieldNode(Opcodes.ACC_PRIVATE, parts[1], parts[2], null, null));
                }
            }
        }
        List<MethodNode> overwrites = adapterOverwrites.get(name);
        if (node != null && overwrites != null && overwrittenClasses.add(name)) {
            for (MethodNode o : overwrites) {
                for (MethodNode m : node.methods) {
                    if (m.name.equals(o.name) && m.desc.equals(o.desc)) {
                        m.instructions = o.instructions;
                        m.tryCatchBlocks = o.tryCatchBlocks;
                        m.localVariables = o.localVariables;
                    }
                }
            }
        }
        List<Object> patches = shapePatches.get(name);
        if (node != null && patches != null && shapedClasses.add(name)) {
            try {
                for (Object why : (List<Object>) shapeApply.invoke(null, node, patches)) {
                    if (why != null) {
                        throw new IllegalStateException("shape patch on " + name + ": " + why);
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
        return node;
    }

    static final Set<String> shapedClasses = new HashSet<>();

    /** whether a shape patch adds the method to the bottom class (the unpatched class hasn't it) */
    static boolean shapeAdded(String owner, String name, String desc) {
        if (!shapePatches.containsKey(owner)) {
            return false;
        }
        ClassNode unpatched = unpatchedBottomClass(owner), patched = bottomClass(owner);
        return unpatched != null && patched != null && unpatched.methods.stream().noneMatch(m -> m.name.equals(name) && m.desc.equals(desc))
                && patched.methods.stream().anyMatch(m -> m.name.equals(name) && m.desc.equals(desc));
    }
    static final Set<String> overwrittenClasses = new HashSet<>();
    static final Set<String> addedClasses = new HashSet<>();
    static final Map<String, Optional<ClassNode>> unpatchedBottom = new HashMap<>();

    static ClassNode unpatchedBottomClass(String name) {
        return parsed(unpatchedBottom, versions.get(versions.size() - 1), name);
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
     * A mixin's {@code @Shadow} members must be in its target classes below (declared there): Mixin refuses to apply a
     * mixin whose shadow it can't find. Only the shadows of members the upper game has count (a mixin for another
     * loader or version shadows names neither has). A shadow named with the prefix (shadow$ by default) is the member
     * without it; one with aliases is left to Mixin.
     */
    record Shadowed(boolean method, String name, String desc, String member) {
    }

    static List<Shadowed> shadows(ClassNode mixin) {
        List<Shadowed> out = new ArrayList<>();
        for (MethodNode m : mixin.methods) {
            AnnotationNode shadow = shadowOf(MixinPoints.annotations(m));
            if (shadow != null && MixinPoints.value(shadow, "aliases") == null) {
                Object prefix = MixinPoints.value(shadow, "prefix");
                String p = prefix instanceof String ps ? ps : "shadow$";
                out.add(new Shadowed(true, m.name.startsWith(p) ? m.name.substring(p.length()) : m.name, m.desc, m.name));
            }
        }
        for (FieldNode f : mixin.fields) {
            List<AnnotationNode> list = new ArrayList<>();
            if (f.visibleAnnotations != null) {
                list.addAll(f.visibleAnnotations);
            }
            if (f.invisibleAnnotations != null) {
                list.addAll(f.invisibleAnnotations);
            }
            AnnotationNode shadow = shadowOf(list);
            if (shadow != null && MixinPoints.value(shadow, "aliases") == null) {
                out.add(new Shadowed(false, f.name, f.desc, f.name));
            }
        }
        return out;
    }

    /**
     * For each shadow of the (unconverted) mixin, in order: whether the upper game's targets have it. The converted mixin
     * keeps its members in the same order (no names are converted here: a walk would report the shadow's member).
     */
    static List<Boolean> shadowsAbove(ClassNode mixin, List<String> targets) {
        List<Boolean> out = new ArrayList<>();
        for (Shadowed s : shadows(mixin)) {
            out.add(targets.stream().anyMatch(t -> hasMember(Convert::topClass, t, s.method(), s.name(), s.desc())));
        }
        return out;
    }

    /**
     * A @Shadow of a method whose types changed below: the remapper gave it its bridge's name (the mixin's calls of it go
     * to the bridge, which calls the lower method). Nothing calls the shadow any more: it becomes the lower method's own
     * shadow (Camera.move(FFF) as 1.20.6's move(DDD)), so Mixin finds it.
     */
    static void bridgedShadows(ClassNode mixin) {
        for (MethodNode m : mixin.methods) {
            AnnotationNode shadow = shadowOf(MixinPoints.annotations(m));
            if (shadow == null || MixinPoints.value(shadow, "aliases") != null || m.name.indexOf('$') < 0) {
                continue;
            }
            Stage0.Ref end = null;
            for (var e : bridgeEnds.entrySet()) {
                if (e.getKey().endsWith("." + m.name)) {
                    end = e.getValue();
                    break;
                }
            }
            if (end == null || !end.name().equals(m.name.substring(0, m.name.indexOf('$')))) {
                continue;
            }
            boolean called = false;
            for (MethodNode other : mixin.methods) {
                for (AbstractInsnNode insn : other.instructions) {
                    called |= insn instanceof MethodInsnNode call && call.name.equals(m.name) && call.desc.equals(m.desc);
                }
            }
            if (!called) {
                m.name = end.name();
                m.desc = end.desc();
            }
        }
        shadowHelpers(mixin);
    }

    /**
     * A @Shadow of a method the targets have below under the same name with other values (a private one no bridge
     * reaches: 1.21's Camera.getMaxZoom(F)F, 1.20.6's (D)D): the shadow becomes the lower method; a private helper of the
     * upper descriptor converts the values and calls it, and the mixin's calls of the shadow go to the helper.
     */
    static void shadowHelpers(ClassNode mixin) {
        List<String> targets = mixinTargetsOf(mixin);
        List<MethodNode> helpers = new ArrayList<>();
        for (MethodNode m : mixin.methods) {
            AnnotationNode shadow = shadowOf(MixinPoints.annotations(m));
            if (shadow == null || MixinPoints.value(shadow, "aliases") != null || m.name.startsWith("<") || targets.isEmpty()) {
                continue;
            }
            boolean everywhere = true;
            String lowerDesc = null;
            for (String t : targets) {
                ClassNode c = bottomClass(t);
                if (c == null || hasMember(Convert::bottomClass, t, true, m.name, m.desc)) {
                    everywhere = false;
                    break;
                }
                String found = null;
                int count = 0;
                for (MethodNode x : c.methods) {
                    if (x.name.equals(m.name) && (x.access & Opcodes.ACC_STATIC) == (m.access & Opcodes.ACC_STATIC)
                            && paramMap(Type.getArgumentTypes(m.desc), Type.getArgumentTypes(x.desc)) != null
                            && twoWayResult(Type.getReturnType(m.desc), Type.getReturnType(x.desc))) {
                        found = x.desc;
                        count++;
                    }
                }
                if (count != 1 || lowerDesc != null && !lowerDesc.equals(found)) {
                    everywhere = false;
                    break;
                }
                lowerDesc = found;
            }
            if (!everywhere || lowerDesc == null) {
                continue;
            }
            String upper = m.desc;
            Type[] up = Type.getArgumentTypes(upper), low = Type.getArgumentTypes(lowerDesc);
            int[] map = paramMap(up, low);
            boolean isStatic = (m.access & Opcodes.ACC_STATIC) != 0;
            String helperName = "ladder$shadow$" + m.name + "$" + Integer.toHexString(upper.hashCode());
            MethodNode g = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC | (isStatic ? Opcodes.ACC_STATIC : 0), helperName, upper, null, null);
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
                    unpackColor(code, slot, colorChannels(up, a, low, -map[a] - 2));
                }
                slot += up[a].getSize();
            }
            boolean privateShadow = (m.access & Opcodes.ACC_PRIVATE) != 0;
            code.add(new MethodInsnNode(isStatic ? Opcodes.INVOKESTATIC : privateShadow ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL, mixin.name,
                    m.name, lowerDesc, false));
            Type upRet = Type.getReturnType(upper), lowRet = Type.getReturnType(lowerDesc);
            if (!upRet.equals(lowRet)) {
                convertUp(code, lowRet, upRet);
            }
            code.add(new InsnNode(upRet.getOpcode(Opcodes.IRETURN)));
            g.maxLocals = slot;
            g.maxStack = slot + 6;
            for (MethodNode other : mixin.methods) {
                for (AbstractInsnNode insn : other.instructions) {
                    if (insn instanceof MethodInsnNode call && call.owner.equals(mixin.name) && call.name.equals(m.name) && call.desc.equals(upper)) {
                        call.name = helperName;
                        call.setOpcode(isStatic ? Opcodes.INVOKESTATIC : Opcodes.INVOKESPECIAL);
                        call.itf = false;
                    }
                }
            }
            m.desc = lowerDesc;
            helpers.add(g);
        }
        mixin.methods.addAll(helpers);
    }

    /**
     * Whether a changed method is private above and below and its values convert both ways: only a mixin's @Shadow can
     * reach it, and shadowHelpers bridges that one.
     */
    static boolean privateShadowable(Stage0.Version from, Stage0.Ref upper, Stage0.Ref lower) {
        if (upper.kind() != 'M' || lower == null) {
            return false;
        }
        Stage0.Cls above = from.classes.get(upper.cls()), below = versions.get(versions.size() - 1).classes.get(lower.cls());
        Integer a = above == null ? null : above.access.get("M " + upper.name() + " " + upper.desc());
        Integer b = below == null ? null : below.access.get("M " + lower.name() + " " + lower.desc());
        if (a == null || b == null || (a & Opcodes.ACC_PRIVATE) == 0 || (b & Opcodes.ACC_PRIVATE) == 0) {
            return false;
        }
        Type[] up = Type.getArgumentTypes(remapper.mapMethodDesc(upper.desc())), low = Type.getArgumentTypes(lower.desc());
        return paramMap(up, low) != null && twoWayResult(Type.getReturnType(remapper.mapMethodDesc(upper.desc())), Type.getReturnType(lower.desc()));
    }

    /** whether a method's result converts both ways between an upper and a lower type (or both are void) */
    static boolean twoWayResult(Type upper, Type lower) {
        return upper.getSort() == Type.VOID ? lower.getSort() == Type.VOID : lower.getSort() != Type.VOID && twoWay(upper, lower);
    }

    static void checkShadows(ClassNode mixin, List<Boolean> above) {
        List<String> targets = mixinTargetsOf(mixin);
        List<Shadowed> shadows = shadows(mixin);
        if (above == null || above.size() != shadows.size()) {
            // the converted mixin's shadows aren't the unconverted one's (a row removed one): not checked here
            return;
        }
        for (int i = 0; i < shadows.size(); i++) {
            Shadowed s = shadows.get(i);
            if (!above.get(i)) {
                continue;
            }
            for (String t : targets) {
                if (bottomClass(t) != null && !hasMember(Convert::bottomClass, t, s.method(), s.name(), s.desc())) {
                    report("mixin shadow missing below: " + s.name() + (s.method() ? "" : " ") + s.desc() + " in " + t + " (" + mixin.name + "." + s.member() + ")");
                }
            }
        }
    }

    static AnnotationNode shadowOf(List<AnnotationNode> annotations) {
        for (AnnotationNode a : annotations) {
            if (a.desc.equals("Lorg/spongepowered/asm/mixin/Shadow;")) {
                return a;
            }
        }
        return null;
    }

    /** whether the class of that game declares the member itself (Mixin finds a shadow in the target class only) */
    static boolean hasMember(java.util.function.Function<String, ClassNode> game, String cls, boolean method, String name, String desc) {
        ClassNode c = game.apply(cls);
        if (c == null) {
            return false;
        }
        return method ? c.methods.stream().anyMatch(x -> x.name.equals(name) && x.desc.equals(desc))
                : c.fields.stream().anyMatch(x -> x.name.equals(name) && x.desc.equals(desc));
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
        if (!moved.isEmpty() && MixinPoints.retargetLambdas(converted, mixinTargetsOf(converted), Convert::bottomClass, moved)) {
            // an injector into a lambda numbered otherwise below: moved to that lambda, checked again
            below.putAll(MixinPoints.check(converted, mixinTargetsOf(converted), Convert::bottomClass));
            moved.removeIf(h -> {
                String lower = below.containsKey(h) ? h : "ladder$wrap$" + h;
                return below.containsKey(lower) && below.get(lower) == null;
            });
        }
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
            boolean matchesNothingAbove = deferred.getOrDefault(h, List.of()).stream().anyMatch(t -> t.contains(" matches nothing in " + versions.get(0).id));
            // (per injector: its mod runs without it in the upper game, the same below, whatever its mixin's other
            // injectors do; one that matches above but not below stays a problem)
            if (!landsAbove && !landsBelow && matchesNothingAbove) {
                // an injector whose selector matches nothing in the mod's own game (another version's names, another
                // loader's): inert there (the mod runs without it), and inert below too, so the same
                unverified.add("mixin injector that applies neither in " + versions.get(0).id + " nor below (" + mixin + "." + h + ")");
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

    /**
     * A mixin's {@code @Shadow} of a method an A row changed (the same method below with fewer parameters, the plan
     * passing the first ones as they are, the rest made by the adapters): the shadow becomes the lower method, and the
     * mixin's calls of it drop the trailing values the lower one doesn't take (21.1's renderArmorPiece with the animation
     * values, 21.0's without). Before the names (top names).
     */
    static void shadowsByArgumentPlans(ClassNode mixin, List<String> targets) {
        for (MethodNode m : mixin.methods) {
            if (shadowOf(MixinPoints.annotations(m)) == null) {
                continue;
            }
            for (String[] a : argumentPlans) {
                if (!a[1].equals(m.name) || !a[2].equals(m.desc) || targets.stream().noneMatch(t -> t.equals(a[0]) || extendsClass(versions.get(0), t, a[0]))) {
                    continue;
                }
                String[] plan = a[4].trim().split(" +");
                Type[] up = Type.getArgumentTypes(a[2]), low = Type.getArgumentTypes(a[3]);
                boolean prefix = plan.length == up.length && low.length < up.length;
                for (int i = 0; prefix && i < low.length; i++) {
                    prefix = plan[i].equals("$" + i) && low[i].equals(up[i]);
                }
                if (!prefix) {
                    continue;
                }
                String upper = m.desc;
                m.desc = a[3];
                for (MethodNode caller : mixin.methods) {
                    for (AbstractInsnNode insn : caller.instructions.toArray()) {
                        if (insn instanceof MethodInsnNode call && call.owner.equals(mixin.name) && call.name.equals(m.name) && call.desc.equals(upper)) {
                            // the trailing values the lower method doesn't take, off the stack (the last first)
                            for (int i = up.length - 1; i >= low.length; i--) {
                                caller.instructions.insertBefore(call, new InsnNode(up[i].getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
                            }
                            call.desc = a[3];
                        }
                    }
                }
                break;
            }
        }
    }

    /**
     * The lower name of a lambda a mixin selector names, when the bottom class hasn't it under that name and descriptor
     * but has one lambda of the same method with that descriptor (javac numbers lambdas in the class's order); else null.
     */
    static String lambdaBelow(String owner, String name, String desc) {
        ClassNode c = bottomClass(remapper.map(owner));
        int last = name.lastIndexOf('$');
        if (c == null || last <= "lambda$".length()) {
            return null;
        }
        String lowerDesc = remapper.mapMethodDesc(desc), prefix = name.substring(0, last + 1);
        String found = null;
        for (MethodNode m : c.methods) {
            if (m.name.equals(name) && m.desc.equals(lowerDesc)) {
                return null;
            }
            if (m.name.startsWith(prefix) && m.desc.equals(lowerDesc)) {
                if (found != null) {
                    return null;
                }
                found = m.name;
            }
        }
        return found;
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
    /** injectors whose handler takes and returns a value of the method itself (a local, an argument, its return) */
    static final String MODIFY_VARIABLE = "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;";

    static final Set<String> VALUE_INJECTORS = Set.of("Lorg/spongepowered/asm/mixin/injection/ModifyVariable;",
            "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;");

    static void remapAnnotation(AnnotationNode a, List<String> targets, Remapper remapper, String where) {
        if (a.values == null || !(a.desc.startsWith("Lorg/spongepowered/asm/mixin/") || a.desc.startsWith("Lcom/llamalad7/mixinextras/"))) {
            return;
        }
        int methods = a.values.indexOf("method");
        if (methods >= 0 && methods % 2 == 0 && a.values.get(methods + 1) instanceof List<?> selectors) {
            // (the injector's methods, as the mod names them: a T row's point is in one of them)
            List<String> names = new ArrayList<>();
            for (Object sel : selectors) {
                if (sel instanceof String name) {
                    String n = name.contains(";") && name.startsWith("L") ? name.substring(name.indexOf(';') + 1) : name;
                    names.add(n.contains("(") ? n.substring(0, n.indexOf('(')) : n);
                }
            }
            injectorMethods = names;
        }
        boolean at = a.desc.equals("Lorg/spongepowered/asm/mixin/injection/At;");
        // a MixinExtras expression's definition names the calls and fields it means as an @At target does (an
        // inherited member through the class the code calls it on), not methods of the mixin's targets
        boolean definition = a.desc.equals("Lcom/llamalad7/mixinextras/expression/Definition;");
        for (int i = 0; i + 1 < a.values.size(); i += 2) {
            String key = (String) a.values.get(i);
            Object value = a.values.get(i + 1);
            if (definition && (key.equals("method") || key.equals("field")) && value instanceof List<?> refs) {
                List<Object> mapped = new ArrayList<>();
                for (Object r : refs) {
                    mapped.add(r instanceof String ref ? targetSelector(ref, remapper, where) : r);
                }
                a.values.set(i + 1, mapped);
            } else if (key.equals("method") && value instanceof List<?> selectors) {
                List<Object> mapped = new ArrayList<>();
                for (Object s : selectors) {
                    mapped.add(s instanceof String sel ? methodSelector(sel, targets, remapper, where) : s);
                }
                a.values.set(i + 1, mapped);
            } else if (at && key.equals("target") && value instanceof String target) {
                String retarget = null;
                for (String t : targets) {
                    for (String method : injectorMethods) {
                        retarget = retarget != null ? retarget : pointRetargets.get(t + "." + method + " " + target);
                    }
                }
                if (retarget != null) {
                    // a T row: the call 1.20.6's code makes there for the same value
                    a.values.set(i + 1, retarget);
                    continue;
                }
                String made = FACTORY_NEW.get(target);
                int kind = a.values.indexOf("value");
                if (made != null && kind >= 0 && kind % 2 == 0 && ("INVOKE".equals(a.values.get(kind + 1)) || "INVOKE_ASSIGN".equals(a.values.get(kind + 1)))
                        && crosses("1.21", "1.20.6")) {
                    // 1.21's factory where 1.20.6's code makes the object itself (ResourceLocation.parse: new
                    // ResourceLocation(String)): the point is that NEW, its handler the same shape
                    a.values.set(i + 1, made);
                    a.values.set(kind + 1, "NEW");
                    continue;
                }
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
                    String lowerInit = currentHandler == null ? null : lowerConstructor(o, mDesc);
                    if (lowerInit != null) {
                        // a constructor that takes other values below (a K or an A row): the injector goes to that one,
                        // its handler's values by the A row's plan
                        handlerFixes.add(new HandlerFix(currentHandler[0], currentHandler[1], currentHandler[2], currentHandler[3],
                                remapper.mapMethodDesc(mDesc), new Stage0.Ref('M', remapper.map(o), "<init>", lowerInit), false));
                        results.add("<init>" + lowerInit);
                        continue;
                    }
                    validateConstructor(o, mDesc);
                    results.add("<init>" + remapper.mapMethodDesc(mDesc));
                    continue;
                }
                String lambda = mName.startsWith("lambda$") ? lambdaBelow(o, mName, mDesc) : null;
                if (lambda != null) {
                    // a lambda numbered otherwise below (1.21's lambda$startSleepInBed$13 is 1.20.6's $12): the one of the
                    // same method and descriptor
                    results.add(lambda + (desc != null ? remapper.mapMethodDesc(mDesc) : ""));
                    qualified.add(lambda + remapper.mapMethodDesc(mDesc));
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
                Stage0.Ref hook = currentHandler == null ? null : hookBelow(o, mName, mDesc);
                if (hook != null) {
                    // the same hook below with other parameters and no call bridge (1.21's GameRenderer.render(DeltaTracker, Z)
                    // is render(F, J, Z)): the injector goes to the lower method, its handler's parameters converted
                    handlerFixes.add(new HandlerFix(currentHandler[0], currentHandler[1], currentHandler[2], currentHandler[3],
                            remapper.mapMethodDesc(mDesc), hook, false));
                    // (a name-only selector stays one: the lower method's descriptor goes along when it names one method)
                    results.add(hook.name() + (desc != null ? hook.desc() : ""));
                    qualified.add(hook.name() + hook.desc());
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
        if (!target.startsWith("L") && !target.startsWith("(")) {
            // Mixin's other form, "owner.name(desc)" (the owner with slashes or dots): as "Lowner;name(desc)"
            int end = target.indexOf('(') >= 0 ? target.indexOf('(') : target.indexOf(':') >= 0 ? target.indexOf(':') : target.length();
            int dot = target.lastIndexOf('.', end);
            if (dot > 0 && end < target.length()) {
                target = "L" + target.substring(0, dot).replace('.', '/') + ";" + target.substring(dot + 1);
            }
        }
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
        if (!field && shapeAdded(owner, name, desc)) {
            // a method the adapters' shape patches give the bottom class (and its game calls): the mixin's point there
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
            Stage0.Ref hook = field || currentHandler == null ? null : hookBelow(owner, name, desc);
            if (hook != null) {
                // a call of the same method below with other parameters (no call bridge): the game's own code calls that one
                handlerFixes.add(new HandlerFix(currentHandler[0], currentHandler[1], currentHandler[2], currentHandler[3],
                        remapper.mapMethodDesc(desc), hook, true));
                return "L" + remapper.map(owner) + ";" + hook.name() + hook.desc();
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

    /** one upper argument of an A row's plan on the stack: "$k" (lower parameter k, converted) or "Class.method(k,...)" */
    static void planArgument(InsnList code, String token, Type[] low, int[] slots, Type up) {
        planArgument(code, token, low, slots, up, -1, null);
    }

    /**
     * An upper argument by an A row's plan token: "$k" (the lower parameter k, converted up) or an adapter's value
     * "owner.method(args)" of lower parameters (k) and of the member's object ("this": in the local receiverSlot, typed
     * as receiver; none for a static member).
     */
    static void planArgument(InsnList code, String token, Type[] low, int[] slots, Type up, int receiverSlot, String receiver) {
        if (token.startsWith("$")) {
            int k = Integer.parseInt(token.substring(1));
            code.add(new VarInsnNode(low[k].getOpcode(Opcodes.ILOAD), slots[k]));
            convertUp(code, low[k], up);
            return;
        }
        if (token.indexOf('(') < 0) {
            // a value the lower method hasn't: the constant the upper game passes there (1.0: white)
            pushDefault(code, token, up);
            return;
        }
        String call = token.substring(0, token.indexOf('('));
        String args = token.substring(token.indexOf('(') + 1, token.length() - 1);
        StringBuilder desc = new StringBuilder("(");
        for (String a : args.isEmpty() ? new String[0] : args.split(",")) {
            if (a.trim().equals("this")) {
                if (receiver == null || receiverSlot < 0) {
                    throw new IllegalArgumentException("A plan " + token + " of a static member");
                }
                code.add(new VarInsnNode(Opcodes.ALOAD, receiverSlot));
                desc.append('L').append(receiver).append(';');
                continue;
            }
            int k = Integer.parseInt(a.trim());
            code.add(new VarInsnNode(low[k].getOpcode(Opcodes.ILOAD), slots[k]));
            desc.append(low[k].getDescriptor());
        }
        desc.append(')').append(up.getDescriptor());
        String owner = call.substring(0, call.lastIndexOf('.')), name = call.substring(call.lastIndexOf('.') + 1);
        checkAdapter(owner, name, desc.toString(), "A plan " + token);
        code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, name, desc.toString(), false));
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
            // an injector other than @Inject (a @WrapOperation, @Redirect, @ModifyArg) sees a call's or a value's types,
            // not its method's: the moved method selector is all it needs, unless it takes the method's arguments at the
            // end (a @Redirect's optional capture)
            // (and an @Inject sees its method's, never those of a call its @At names)
            list.removeIf(x -> {
                boolean inject = x.annotation().equals("Lorg/spongepowered/asm/mixin/injection/Inject;");
                if (inject) {
                    if (x.call()) {
                        return true;
                    }
                    // a handler that takes the lower method's parameters (then its CallbackInfo) already fits it: a
                    // name-only selector that also names the upper overload the lower one stands for (21.1's
                    // renderArmorPiece of 6 parameters beside the one of 12; 21.0's is the 6 alone)
                    Type[] hp0 = Type.getArgumentTypes(remapper.mapMethodDesc(x.desc())), low0 = Type.getArgumentTypes(x.end().desc());
                    Type[] up0 = Type.getArgumentTypes(x.upperDesc());
                    return !Arrays.equals(up0, low0) && hp0.length > low0.length && Arrays.equals(Arrays.copyOf(hp0, low0.length), low0)
                            && hp0[low0.length].getSort() == Type.OBJECT && hp0[low0.length].getInternalName().startsWith("org/spongepowered/asm/mixin/injection/callback/");
                }
                if (!x.call() && VALUE_INJECTORS.contains(x.annotation())) {
                    // a value injector modifies a local, an argument or the return value of the method: of a changed
                    // type, its value's type changed too
                    Type[] up0 = Type.getArgumentTypes(x.upperDesc()), low0 = Type.getArgumentTypes(x.end().desc());
                    Set<Type> gone = new HashSet<>(Arrays.asList(up0));
                    gone.removeAll(Arrays.asList(low0));
                    if (!Type.getReturnType(x.upperDesc()).equals(Type.getReturnType(x.end().desc()))) {
                        gone.add(Type.getReturnType(x.upperDesc()));
                    }
                    Type value = Type.getReturnType(remapper.mapMethodDesc(x.desc()));
                    // (a handler that captures the method's arguments after its value takes the upper ones: changed too)
                    Type[] hp0 = Type.getArgumentTypes(remapper.mapMethodDesc(x.desc()));
                    boolean captures = hp0.length > 1 && up0.length > 0 && !Arrays.equals(up0, low0)
                            && Arrays.asList(hp0).subList(1, hp0.length).stream().anyMatch(Arrays.asList(up0)::contains);
                    return !gone.contains(value) && !captures;
                }
                if (x.call() || x.annotation().equals("Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;")) {
                    return false;
                }
                Type[] params = Type.getArgumentTypes(remapper.mapMethodDesc(x.desc())), method = Type.getArgumentTypes(x.upperDesc());
                return method.length == 0 || params.length < method.length
                        || !Arrays.equals(Arrays.copyOfRange(params, params.length - method.length, params.length), method)
                        && !Arrays.equals(Arrays.copyOf(params, method.length), method);
            });
            if (list.isEmpty()) {
                continue;
            }
            // (a call injector into a method whose own types changed too: the call's fix, and the method's for the target's
            // arguments the handler captures after the call's)
            HandlerFix methodFix = null;
            if (list.size() == 2 && list.get(0).call() != list.get(1).call()) {
                methodFix = list.get(0).call() ? list.get(1) : list.get(0);
                list = new ArrayList<>(List.of(list.get(0).call() ? list.get(0) : list.get(1)));
            }
            HandlerFix f = list.get(0);
            String where = original + "." + f.method();
            String hDesc = remapper.mapMethodDesc(f.desc());
            MethodNode h = out.methods.stream().filter(m -> m.name.equals(f.method()) && m.desc.equals(hDesc)).findFirst().orElse(null);
            Type[] up = Type.getArgumentTypes(f.upperDesc()), low = Type.getArgumentTypes(f.end().desc());
            Type[] hp = h == null ? new Type[0] : Type.getArgumentTypes(h.desc);
            int[] map = paramMap(up, low);
            int[] from = map == null ? defaultedLower(f.end().cls() + "." + f.end().name() + " " + f.upperDesc(), f.end().desc()) : null;
            if (from != null) {
                // a D row: each upper parameter is the lower one it is passed as (the lower-only ones are defaults)
                map = new int[up.length];
                Arrays.fill(map, -1);
                for (int j = 0; j < from.length; j++) {
                    if (from[j] >= 0) {
                        map[from[j]] = j;
                    }
                }
            }
            if (map == null) {
                map = handlerMap(up, low);
            }
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
            // (a handler that takes the target's parameters takes them all: a lower method with more needs the wrapper)
            boolean takesAll = !f.call() && hp.length >= up.length && up.length > 0 && Arrays.equals(Arrays.copyOf(hp, up.length), up);
            holdsChanged |= takesAll && low.length != up.length;
            String[] plan = f.call() ? null : argumentPlan(f.upperDesc(), f.end());
            String[] planRow = argumentPlanRow(f.upperDesc(), f.end());
            String planOwner = planRow == null ? null : remapper.map(planRow[0]);
            if (plan != null && plan.length != up.length) {
                problem("an A row's plan for " + f.end().key() + " has " + plan.length + " arguments, not " + up.length);
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
            String[] callPlan = f.call() ? argumentPlan(f.upperDesc(), f.end()) : null;
            // (a static call's handler takes no receiver)
            int callBase = f.call() && staticBelow(f.end()) ? 0 : 1;
            boolean callShape = callPlan != null && callPlan.length == up.length && !returnChanged && distinct && hp.length >= callBase + up.length
                    && (f.annotation().equals("Lcom/llamalad7/mixinextras/injector/v2/WrapWithCondition;")
                            || f.annotation().equals("Lcom/llamalad7/mixinextras/injector/WrapWithCondition;")
                            || f.annotation().equals("Lorg/spongepowered/asm/mixin/injection/Redirect;"))
                    && Arrays.equals(Arrays.copyOfRange(hp, callBase, callBase + up.length), up);
            if (callShape) {
                Type[] tailUp = methodFix == null ? null : Type.getArgumentTypes(methodFix.upperDesc());
                Type[] tailLow = methodFix == null ? null : Type.getArgumentTypes(methodFix.end().desc());
                String[] tailPlan = methodFix == null ? null : argumentPlan(methodFix.upperDesc(), methodFix.end());
                int[] tailMap = methodFix == null || tailPlan != null ? null : paramMap(tailUp, tailLow);
                if (methodFix != null && (tailPlan == null && tailMap == null
                        || !Arrays.equals(Arrays.copyOfRange(hp, callBase + up.length, hp.length), tailUp))) {
                    problem("mixin handler " + where + " captures the arguments of " + methodFix.end().name() + ", whose types changed below too: no bridge for both");
                    continue;
                }
                callBridge(out, h, hp, up, low, callPlan, planOwner, tailUp, tailLow, tailPlan, tailMap, callBase);
                continue;
            }
            // an @Inject whose method's result changed between numbers (1.21's Camera.getMaxZoom(F)F, 1.20.6's (D)D): its
            // CallbackInfoReturnable gets the upper number, and what it sets goes back as the lower one
            Type upRet = Type.getReturnType(f.upperDesc()), lowRet = Type.getReturnType(f.end().desc());
            int callback = -1;
            for (int i = up.length; i < hp.length && returnChanged; i++) {
                if (hp[i].getSort() == Type.OBJECT && hp[i].getInternalName().equals(CALLBACK_RETURNABLE)) {
                    callback = i;
                    break;
                }
            }
            boolean numberResult = returnChanged && inject && !f.call() && callback >= 0 && (NUMBERS.indexOf(upRet.getDescriptor()) >= 0
                    && NUMBERS.indexOf(lowRet.getDescriptor()) >= 0 && upRet.getDescriptor().length() == 1 && lowRet.getDescriptor().length() == 1
                    // (or NeoForge 21's TriState of 20.6's boolean, its colour Integer of 20.6's floats: the callback's value
                    // converts the same way)
                    || conversion(upRet, lowRet) == 18 || conversion(upRet, lowRet) == 19
                    // (1.21's MeshData of 1.20.6's rendered buffer: the adapters' Conversions.convert)
                    || conversion(upRet, lowRet) == 16);
            if (!inject && distinct && valueBridge(out, h, hp, f, up, low, upRet, lowRet)) {
                // an injector of another kind whose values convert both ways (a float of 1.21 a double of 1.20.6)
                continue;
            }
            if (f.call() || !inject || !prefix || returnChanged && !numberResult || !distinct) {
                problem("mixin handler " + where + " takes types that changed below (" + f.upperDesc() + " -> " + f.end().desc()
                        + "): no handler bridge for it");
                continue;
            }
            if (map == null && plan == null) {
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
            // (parameter annotations, @Local and @Share on the handler's own parameters, follow their parameters into
            // the wrapper's slots: an upper parameter to the lower one it comes from, the handler's own after the lower ones)
            int[] placeOf = new int[hp.length];
            for (int i = 0; i < hp.length; i++) {
                placeOf[i] = i >= up.length ? low.length + i - up.length : plan == null && map[i] >= 0 ? map[i] : -1;
            }
            g.visibleParameterAnnotations = moveParameterAnnotations(h.visibleParameterAnnotations, placeOf, wrapped.length);
            g.invisibleParameterAnnotations = moveParameterAnnotations(h.invisibleParameterAnnotations, placeOf, wrapped.length);
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
                if (plan != null) {
                    planArgument(code, plan[i], low, slots, up[i], isStatic ? -1 : 0, planOwner);
                } else if (map[i] >= 0) {
                    code.add(new VarInsnNode(low[map[i]].getOpcode(Opcodes.ILOAD), slots[map[i]]));
                    convertUp(code, low[map[i]], up[i]);
                } else if (map[i] <= -2) {
                    packColor(code, slots, -map[i] - 2, colorChannels(up, i, low, -map[i] - 2));
                } else {
                    supplyContext(code, up[i]);
                }
            }
            int upperCallback = slot;
            for (int i = low.length; i < wrapped.length; i++) {
                code.add(new VarInsnNode(wrapped[i].getOpcode(Opcodes.ILOAD), slots[i]));
                if (numberResult && i - low.length + up.length == callback) {
                    // the upper callback: the lower one's value as the upper number (kept in a local of its own)
                    code.add(new LdcInsnNode(upRet.getDescriptor()));
                    checkAdapter(CONVERSIONS, "callback", "(L" + CALLBACK_RETURNABLE + ";Ljava/lang/String;)L" + CALLBACK_RETURNABLE + ";", where);
                    code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "callback",
                            "(L" + CALLBACK_RETURNABLE + ";Ljava/lang/String;)L" + CALLBACK_RETURNABLE + ";", false));
                    code.add(new InsnNode(Opcodes.DUP));
                    code.add(new VarInsnNode(Opcodes.ASTORE, upperCallback));
                }
            }
            boolean privateHandler = (h.access & Opcodes.ACC_PRIVATE) != 0;
            code.add(new MethodInsnNode(isStatic ? Opcodes.INVOKESTATIC : privateHandler ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL, out.name,
                    h.name, h.desc, false));
            if (numberResult) {
                // what the handler set (a cancel with a value) goes to the lower callback as the lower number
                code.add(new VarInsnNode(Opcodes.ALOAD, upperCallback));
                code.add(new VarInsnNode(Opcodes.ALOAD, slots[low.length + callback - up.length]));
                code.add(new LdcInsnNode(lowRet.getDescriptor()));
                checkAdapter(CONVERSIONS, "result", "(L" + CALLBACK_RETURNABLE + ";L" + CALLBACK_RETURNABLE + ";Ljava/lang/String;)V", where);
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "result",
                        "(L" + CALLBACK_RETURNABLE + ";L" + CALLBACK_RETURNABLE + ";Ljava/lang/String;)V", false));
                slot++;
            }
            code.add(new InsnNode(Type.getReturnType(h.desc).getOpcode(Opcodes.IRETURN)));
            g.maxLocals = slot;
            g.maxStack = slot + 2 + 2 * up.length;
            out.methods.add(g);
        }
    }

    /** 1.21's factories whose call 1.20.6's code makes as a NEW of the class: @At target -> the NEW target */
    static final Map<String, String> FACTORY_NEW = Map.of(
            "Lnet/minecraft/resources/ResourceLocation;parse(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;",
            "(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;",
            "Lnet/minecraft/resources/ResourceLocation;withDefaultNamespace(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;",
            "(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;",
            "Lnet/minecraft/resources/ResourceLocation;fromNamespaceAndPath(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;",
            "(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;");

    static final String OPERATION = "com/llamalad7/mixinextras/injector/wrapoperation/Operation";
    static final Set<String> REDIRECTS = Set.of("Lorg/spongepowered/asm/mixin/injection/Redirect;",
            "Lcom/llamalad7/mixinextras/injector/v2/WrapWithCondition;", "Lcom/llamalad7/mixinextras/injector/WrapWithCondition;");
    static final String WRAP_OPERATION = "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;";
    static final String WRAP_METHOD = "Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;";
    static final String MODIFY_EXPRESSION = "Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;";
    static final String MODIFY_RETURN = "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;";
    static final String MODIFY_ARG = "Lorg/spongepowered/asm/mixin/injection/ModifyArg;";

    /** whether a value converts both ways between an upper and a lower type (a handler's argument up, its result down) */
    static boolean twoWay(Type upper, Type lower) {
        int c = conversion(upper, lower);
        return c == 0 || c == 1 || c == 2 || c == 6 || c == 8 || c == 9 || c == 12 || c == 15 || c == 16 || c == 17 || c == 18 || c == 19;
    }

    /** whether the lower member is static (a call's receiver isn't among a call injector's values then) */
    static boolean staticBelow(Stage0.Ref end) {
        Stage0.Cls info = versions.get(versions.size() - 1).classes.get(end.cls());
        return info != null && info.staticMethods.contains(end.name() + " " + end.desc());
    }

    /**
     * The wrapper of a handler of another injector than @Inject (@Redirect, @WrapWithCondition, @WrapOperation,
     * @ModifyArg, @ModifyExpressionValue, @ModifyReturnValue, @WrapMethod) whose call or method has other types below,
     * each converting both ways (1.21's floats 1.20.6's doubles, a holder its value, the DeltaTracker the partial tick):
     * the wrapper takes the lower values, gives the handler the upper ones, gives back its result as the lower one. An
     * Operation the handler calls takes the upper values and converts them down (Conversions.operation). The injector's
     * annotation moves to the wrapper; the handler keeps its name and descriptor. False when the shape isn't one of these.
     */
    /**
     * A @ModifyReturnValue (a @ModifyVariable) of a changed method that captures the method's arguments after the result
     * (the local; the first of them, as many as it takes): the wrapper takes the lower method's result (the local) and
     * all its arguments, and makes the upper ones the handler takes by the method's A row plan, or its map.
     */
    static boolean resultCaptureBridge(ClassNode out, MethodNode h, Type[] hp, HandlerFix f, Type[] up, Type[] low, Type upRet, Type lowRet) {
        int c = hp.length - 1;
        if (c > up.length || !Arrays.equals(Arrays.copyOfRange(hp, 1, hp.length), Arrays.copyOf(up, c))) {
            return false;
        }
        String[] plan = argumentPlan(f.upperDesc(), f.end());
        int[] map = plan != null ? null : paramMap(up, low);
        if (plan == null && map == null) {
            return false;
        }
        if (plan != null && plan.length != up.length) {
            return false;
        }
        Type hRet = Type.getReturnType(h.desc);
        Type[] wrapped = new Type[1 + low.length];
        wrapped[0] = lowRet;
        System.arraycopy(low, 0, wrapped, 1, low.length);
        MethodNode g = new MethodNode(h.access, "ladder$wrap$" + h.name, Type.getMethodDescriptor(lowRet, wrapped), null,
                h.exceptions.toArray(new String[0]));
        g.visibleAnnotations = h.visibleAnnotations;
        g.invisibleAnnotations = h.invisibleAnnotations;
        h.visibleAnnotations = null;
        h.invisibleAnnotations = null;
        // (the captured parameters' annotations stay on the handler: the wrapper's are the lower method's own)
        if (h.visibleParameterAnnotations != null || h.invisibleParameterAnnotations != null) {
            int[] placeOf = new int[hp.length];
            Arrays.fill(placeOf, -1);
            placeOf[0] = 0;
            g.visibleParameterAnnotations = moveParameterAnnotations(h.visibleParameterAnnotations, placeOf, wrapped.length);
            g.invisibleParameterAnnotations = moveParameterAnnotations(h.invisibleParameterAnnotations, placeOf, wrapped.length);
        }
        boolean isStatic = (h.access & Opcodes.ACC_STATIC) != 0;
        InsnList code = g.instructions;
        int slot = isStatic ? 0 : 1;
        int[] slots = new int[wrapped.length];
        for (int i = 0; i < wrapped.length; i++) {
            slots[i] = slot;
            slot += wrapped[i].getSize();
        }
        int[] lowSlots = Arrays.copyOfRange(slots, 1, slots.length);
        if (!isStatic) {
            code.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        code.add(new VarInsnNode(lowRet.getOpcode(Opcodes.ILOAD), slots[0]));
        if (!lowRet.equals(upRet)) {
            convertUp(code, lowRet, upRet);
        }
        for (int i = 0; i < c; i++) {
            if (plan != null) {
                planArgument(code, plan[i], low, lowSlots, up[i], isStatic ? -1 : 0, null);
            } else if (map[i] >= 0) {
                code.add(new VarInsnNode(low[map[i]].getOpcode(Opcodes.ILOAD), lowSlots[map[i]]));
                convertUp(code, low[map[i]], up[i]);
            } else if (map[i] <= -2) {
                packColor(code, lowSlots, -map[i] - 2, colorChannels(up, i, low, -map[i] - 2));
            } else {
                supplyContext(code, up[i]);
            }
        }
        boolean privateHandler = (h.access & Opcodes.ACC_PRIVATE) != 0;
        code.add(new MethodInsnNode(isStatic ? Opcodes.INVOKESTATIC : privateHandler ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL, out.name,
                h.name, h.desc, false));
        if (!lowRet.equals(hRet)) {
            convertDown(code, hRet, lowRet);
        }
        code.add(new InsnNode(lowRet.getOpcode(Opcodes.IRETURN)));
        g.maxLocals = slot;
        g.maxStack = slot + 4 + 2 * c;
        out.methods.add(g);
        return true;
    }

    static boolean valueBridge(ClassNode out, MethodNode h, Type[] hp, HandlerFix f, Type[] up, Type[] low, Type upRet, Type lowRet) {
        if (!twoWay(upRet, lowRet) && !(upRet.getSort() == Type.VOID && lowRet.getSort() == Type.VOID)) {
            return false;
        }
        String ann = f.annotation();
        if (!f.call() && ann.equals(MODIFY_VARIABLE) && hp.length > 1 && !Arrays.equals(up, low) && hp[0].equals(Type.getReturnType(h.desc))
                && !Arrays.asList(up).contains(hp[0])) {
            // a local of the method (its type the same below), then the method's arguments it captures (LeavesBeGone's
            // chunk read: 1.21's RegionStorageInfo)
            return resultCaptureBridge(out, h, hp, f, up, low, hp[0], hp[0]);
        }
        // (a value injector of a result sees only the result: the arguments may change as they like; the others see them)
        boolean resultOnly = f.call() && ann.equals(MODIFY_EXPRESSION) || !f.call() && ann.equals(MODIFY_RETURN);
        boolean argumentsConvert = up.length == low.length;
        for (int i = 0; i < up.length && argumentsConvert; i++) {
            argumentsConvert = twoWay(up[i], low[i]);
        }
        if (!resultOnly && !argumentsConvert) {
            return false;
        }
        Type hRet = Type.getReturnType(h.desc);
        Type[] wrapped = hp.clone();
        Type wrappedRet = hRet;
        int operation = -1;
        boolean recv = f.call() && !staticBelow(f.end());
        int base = recv ? 1 : 0;
        if (f.call() && (REDIRECTS.contains(ann) || ann.equals(WRAP_OPERATION))) {
            // [the receiver,] the call's arguments, [the Operation,] then the target method's captured ones
            if (hp.length < base + up.length || !Arrays.equals(Arrays.copyOfRange(hp, base, base + up.length), up)) {
                return false;
            }
            System.arraycopy(low, 0, wrapped, base, low.length);
            if (ann.equals(WRAP_OPERATION)) {
                if (hp.length <= base + up.length || !hp[base + up.length].getInternalName().equals(OPERATION)) {
                    return false;
                }
                operation = base + up.length;
            }
            if (!ann.contains("WrapWithCondition")) {
                if (!hRet.equals(upRet)) {
                    return false;
                }
                wrappedRet = lowRet;
            }
        } else if (resultOnly) {
            // the call's (the method's) result, then captured values (a method's own arguments when they changed: made
            // of the lower method's)
            if (!f.call() && !Arrays.equals(up, low) && hp.length > 1) {
                return hp[0].equals(upRet) && hRet.equals(upRet) && resultCaptureBridge(out, h, hp, f, up, low, upRet, lowRet);
            }
            if (hp.length < 1 || !hp[0].equals(upRet) || !hRet.equals(upRet)) {
                return false;
            }
            wrapped[0] = lowRet;
            wrappedRet = lowRet;
        } else if (f.call() && ann.equals(MODIFY_ARG)) {
            // one argument of the call (or all of them, the modified one's type the result)
            if (hp.length == 1 && hp[0].equals(hRet)) {
                int index = -1;
                for (int i = 0; i < up.length; i++) {
                    if (up[i].equals(hRet) && !up[i].equals(low[i])) {
                        if (index >= 0) {
                            return false;
                        }
                        index = i;
                    }
                }
                if (index < 0) {
                    return false;
                }
                wrapped[0] = low[index];
                wrappedRet = low[index];
            } else if (hp.length == up.length && Arrays.equals(hp, up)) {
                int index = -1;
                for (int i = 0; i < up.length; i++) {
                    if (up[i].equals(hRet) && !up[i].equals(low[i])) {
                        index = index < 0 ? i : -2;
                    }
                }
                if (index < 0) {
                    return false;
                }
                System.arraycopy(low, 0, wrapped, 0, low.length);
                wrappedRet = low[index];
            } else {
                return false;
            }
        } else if (!f.call() && ann.equals(WRAP_METHOD)) {
            // the method's arguments, then the Operation
            if (hp.length != up.length + 1 || !Arrays.equals(Arrays.copyOf(hp, up.length), up) || !hp[up.length].getInternalName().equals(OPERATION)
                    || !hRet.equals(upRet)) {
                return false;
            }
            System.arraycopy(low, 0, wrapped, 0, low.length);
            operation = up.length;
            wrappedRet = lowRet;
        } else {
            return false;
        }
        String spec = null;
        if (operation >= 0) {
            // the Operation's values: [the receiver,] the arguments, and its result, each "upper>lower" or "" when the same
            StringBuilder b = new StringBuilder();
            if (recv) {
                b.append(',');
            }
            for (int i = 0; i < up.length; i++) {
                b.append(up[i].equals(low[i]) ? "" : up[i].getDescriptor() + ">" + low[i].getDescriptor()).append(',');
            }
            b.append('|').append(upRet.equals(lowRet) ? "" : upRet.getDescriptor() + ">" + lowRet.getDescriptor());
            spec = b.toString();
            checkAdapter(CONVERSIONS, "operation", "(L" + OPERATION + ";Ljava/lang/String;)L" + OPERATION + ";", "a value bridge of " + h.name);
        }
        MethodNode g = new MethodNode(h.access, "ladder$wrap$" + h.name, Type.getMethodDescriptor(wrappedRet, wrapped), null,
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
        for (int i = 0; i < wrapped.length; i++) {
            code.add(new VarInsnNode(wrapped[i].getOpcode(Opcodes.ILOAD), slot));
            if (i == operation) {
                code.add(new LdcInsnNode(spec));
                code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, "operation", "(L" + OPERATION + ";Ljava/lang/String;)L" + OPERATION + ";", false));
            } else if (!wrapped[i].equals(hp[i])) {
                convertUp(code, wrapped[i], hp[i]);
            }
            slot += wrapped[i].getSize();
        }
        boolean privateHandler = (h.access & Opcodes.ACC_PRIVATE) != 0;
        code.add(new MethodInsnNode(isStatic ? Opcodes.INVOKESTATIC : privateHandler ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL, out.name,
                h.name, h.desc, false));
        if (!wrappedRet.equals(hRet)) {
            convertDown(code, hRet, wrappedRet);
        }
        code.add(new InsnNode(wrappedRet.getOpcode(Opcodes.IRETURN)));
        g.maxLocals = slot;
        g.maxStack = slot + 8;
        out.methods.add(g);
        return true;
    }

    /**
     * A call injector's wrapper (a @WrapWithCondition's, a @Redirect's handler: the call's receiver, its arguments, then
     * the handler's own): it takes the lower call's, makes the upper arguments by the A row's plan and calls the handler.
     * The injector's annotation moves to the wrapper; the handler keeps its name and descriptor.
     */
    static void callBridge(ClassNode out, MethodNode h, Type[] hp, Type[] up, Type[] low, String[] plan, String planOwner) {
        callBridge(out, h, hp, up, low, plan, planOwner, null, null, null, null);
    }

    /**
     * (tailUp, tailLow: the target method's own parameters the handler captures after the call's, when they changed below
     * too: made of the lower ones by the method's plan, or its map)
     */
    static void callBridge(ClassNode out, MethodNode h, Type[] hp, Type[] up, Type[] low, String[] plan, String planOwner,
            Type[] tailUp, Type[] tailLow, String[] tailPlan, int[] tailMap) {
        callBridge(out, h, hp, up, low, plan, planOwner, tailUp, tailLow, tailPlan, tailMap, 1);
    }

    /** (base: 1 when the handler takes the call's receiver first, 0 for a static call's) */
    static void callBridge(ClassNode out, MethodNode h, Type[] hp, Type[] up, Type[] low, String[] plan, String planOwner,
            Type[] tailUp, Type[] tailLow, String[] tailPlan, int[] tailMap, int base) {
        Type[] tail = tailUp == null ? Arrays.copyOfRange(hp, base + up.length, hp.length) : tailLow;
        Type[] wrapped = new Type[base + low.length + tail.length];
        if (base == 1) {
            wrapped[0] = hp[0];
        }
        System.arraycopy(low, 0, wrapped, base, low.length);
        System.arraycopy(tail, 0, wrapped, base + low.length, tail.length);
        MethodNode g = new MethodNode(h.access, "ladder$wrap$" + h.name, Type.getMethodDescriptor(Type.getReturnType(h.desc), wrapped), null,
                h.exceptions.toArray(new String[0]));
        g.visibleAnnotations = h.visibleAnnotations;
        g.invisibleAnnotations = h.invisibleAnnotations;
        int[] placeOf = new int[hp.length];
        for (int i = 0; i < hp.length; i++) {
            placeOf[i] = i < base ? i : i >= base + up.length && tailUp == null ? base + low.length + i - base - up.length : -1;
        }
        g.visibleParameterAnnotations = moveParameterAnnotations(h.visibleParameterAnnotations, placeOf, wrapped.length);
        g.invisibleParameterAnnotations = moveParameterAnnotations(h.invisibleParameterAnnotations, placeOf, wrapped.length);
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
        if (base == 1) {
            code.add(new VarInsnNode(wrapped[0].getOpcode(Opcodes.ILOAD), slots[0]));
        }
        int[] lowSlots = Arrays.copyOfRange(slots, base, base + low.length);
        for (int i = 0; i < up.length; i++) {
            // (the member's object: the call's receiver)
            planArgument(code, plan[i], low, lowSlots, up[i], base == 1 ? slots[0] : -1, planOwner);
        }
        if (tailUp == null) {
            for (int i = base + low.length; i < wrapped.length; i++) {
                code.add(new VarInsnNode(wrapped[i].getOpcode(Opcodes.ILOAD), slots[i]));
            }
        } else {
            int[] tailSlots = Arrays.copyOfRange(slots, base + low.length, wrapped.length);
            for (int i = 0; i < tailUp.length; i++) {
                if (tailPlan != null) {
                    planArgument(code, tailPlan[i], tailLow, tailSlots, tailUp[i], isStatic ? -1 : 0, null);
                } else if (tailMap[i] >= 0) {
                    code.add(new VarInsnNode(tailLow[tailMap[i]].getOpcode(Opcodes.ILOAD), tailSlots[tailMap[i]]));
                    convertUp(code, tailLow[tailMap[i]], tailUp[i]);
                } else if (tailMap[i] <= -2) {
                    packColor(code, tailSlots, -tailMap[i] - 2, colorChannels(tailUp, i, tailLow, -tailMap[i] - 2));
                } else {
                    supplyContext(code, tailUp[i]);
                }
            }
        }
        boolean privateHandler = (h.access & Opcodes.ACC_PRIVATE) != 0;
        code.add(new MethodInsnNode(isStatic ? Opcodes.INVOKESTATIC : privateHandler ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL, out.name,
                h.name, h.desc, false));
        code.add(new InsnNode(Type.getReturnType(h.desc).getOpcode(Opcodes.IRETURN)));
        g.maxLocals = slot;
        g.maxStack = slot + 4 + 2 * up.length + (tailUp == null ? 0 : 2 * tailUp.length);
        out.methods.add(g);
    }

    /** A method's parameter annotations at new places (placeOf[i] < 0: the parameter is gone); null when there were none. */
    @SuppressWarnings("unchecked")
    static List<AnnotationNode>[] moveParameterAnnotations(List<AnnotationNode>[] annotations, int[] placeOf, int count) {
        if (annotations == null) {
            return null;
        }
        List<AnnotationNode>[] moved = (List<AnnotationNode>[]) new List[count];
        for (int i = 0; i < annotations.length && i < placeOf.length; i++) {
            if (annotations[i] != null && placeOf[i] >= 0 && placeOf[i] < count) {
                moved[placeOf[i]] = annotations[i];
            }
        }
        return moved;
    }

    // ---------------------------------------------------------------- data

    /** 1.21's singular data folders -> 1.20.4's plural ones; null for data 1.20.4 has no place for. */
    /** The folder of a pack the jar ships whose data the entry is in ("datapacks/x/" for datapacks/x/data/...), or null. */
    static String nestedDataRoot(String name, Map<String, byte[]> entries) {
        for (int i = name.indexOf("/data/"); i >= 0; i = name.indexOf("/data/", i + 1)) {
            String root = name.substring(0, i + 1);
            if (entries.containsKey(root + "pack.mcmeta") || overlayRoots(entries).contains(root)) {
                return root;
            }
        }
        return null;
    }

    private static Map<String, byte[]> overlayEntries;
    private static Set<String> overlayRoots = Set.of();

    /**
     * The overlay folders the jar's packs declare ("overlays" and "neoforge:overlays" of each pack.mcmeta, the mod's
     * own at the root included): their data is data of the pack too.
     */
    static Set<String> overlayRoots(Map<String, byte[]> entries) {
        if (entries == overlayEntries) {
            return overlayRoots;
        }
        Set<String> roots = new HashSet<>();
        for (var e : entries.entrySet()) {
            if (!e.getKey().equals("pack.mcmeta") && !e.getKey().endsWith("/pack.mcmeta")) {
                continue;
            }
            String base = e.getKey().substring(0, e.getKey().length() - "pack.mcmeta".length());
            try {
                JsonObject meta = JsonParser.parseString(new String(e.getValue(), StandardCharsets.UTF_8)).getAsJsonObject();
                for (String section : List.of("overlays", "neoforge:overlays")) {
                    if (meta.has(section) && meta.get(section).isJsonObject() && meta.getAsJsonObject(section).has("entries")) {
                        for (JsonElement entry : meta.getAsJsonObject(section).getAsJsonArray("entries")) {
                            // only an overlay the bottom game applies (its formats hold the bottom's data pack format): one for
                            // 1.21.2+ applies on neither 1.21.1 nor 1.20.4
                            if (entry.isJsonObject() && entry.getAsJsonObject().has("directory") && appliesBelow(entry.getAsJsonObject().get("formats"))) {
                                roots.add(base + entry.getAsJsonObject().get("directory").getAsString() + "/");
                            }
                        }
                    }
                }
            } catch (RuntimeException ignored) {
                // not JSON the game reads either
            }
        }
        overlayEntries = entries;
        overlayRoots = roots;
        return roots;
    }

    /** the bottom version's data pack format (1.20.4: 26) */
    static final int BOTTOM_DATA_FORMAT = 26;

    /** whether an overlay's "formats" (a number, [min, max] or {min_inclusive, max_inclusive}) hold the bottom's data format */
    static boolean appliesBelow(JsonElement formats) {
        if (formats == null) {
            return false;
        }
        try {
            if (formats.isJsonPrimitive()) {
                return formats.getAsInt() == BOTTOM_DATA_FORMAT;
            }
            if (formats.isJsonArray() && formats.getAsJsonArray().size() > 0) {
                var range = formats.getAsJsonArray();
                int min = range.get(0).getAsInt(), max = range.get(range.size() - 1).getAsInt();
                return min <= BOTTOM_DATA_FORMAT && BOTTOM_DATA_FORMAT <= max;
            }
            if (formats.isJsonObject()) {
                var range = formats.getAsJsonObject();
                return range.get("min_inclusive").getAsInt() <= BOTTOM_DATA_FORMAT && BOTTOM_DATA_FORMAT <= range.get("max_inclusive").getAsInt();
            }
        } catch (RuntimeException e) {
            return false;
        }
        return false;
    }

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
        // 1.21's singular folders, the step down to 1.20.6
        if (crosses("1.21", "1.20.6")) {
            for (String[] f : folders) {
                if (rest.startsWith(f[0] + "/")) {
                    return parts[0] + "/" + parts[1] + "/" + f[1] + rest.substring(f[0].length());
                }
            }
        }
        // registries the lower version fills from code or doesn't have (trim materials and patterns and damage types it
        // reads: kept): 1.21's (enchantments, jukebox songs, paintings), 1.20.5's (banner patterns, wolf variants)
        String[] gone = crosses("1.21", "1.20.6") && crosses("1.20.6", "1.20.4")
                ? new String[] {"enchantment/", "enchantment_provider/", "jukebox_song/", "painting_variant/", "banner_pattern/", "wolf_variant/"}
                : crosses("1.21", "1.20.6") ? new String[] {"enchantment/", "enchantment_provider/", "jukebox_song/", "painting_variant/"}
                : crosses("1.20.6", "1.20.4") ? new String[] {"banner_pattern/", "wolf_variant/"} : new String[0];
        for (String g : gone) {
            if (rest.startsWith(g) && adapterDataFolders.stream().noneMatch(f -> rest.startsWith(f + "/"))) {
                return null;
            }
        }
        return path;
    }

    /** G rows: data folders (under data/<namespace>/) whose registry the adapters add below */
    static final Set<String> adapterDataFolders = new HashSet<>();

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
            lootFunctions(object, path);
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
        if (!type.isEmpty() && !type.contains(":")) {
            // an id without its namespace is the game's (crafting_shaped = minecraft:crafting_shaped)
            type = "minecraft:" + type;
        }
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

    /**
     * A loot table's item functions in 1.20.4's form: set_components as the adapters' ladder:set_components (the same
     * patch through the component store), copy_components of a block entity's custom name alone as copy_name, set_lore's
     * replace_all / append modes as its replace flag, set_name's custom_name target dropped (1.20.4's only one). Any
     * other component copy, lore mode or name target has no 1.20.4 form: a problem.
     */
    static void lootFunctions(JsonElement element, String path) {
        if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(e -> lootFunctions(e, path));
            return;
        }
        if (!element.isJsonObject()) {
            return;
        }
        JsonObject object = element.getAsJsonObject();
        if (object.has("function") && object.get("function").isJsonPrimitive()) {
            String function = object.get("function").getAsString();
            function = function.contains(":") ? function : "minecraft:" + function;
            switch (function) {
                case "minecraft:set_components" -> {
                    object.addProperty("function", "ladder:set_components");
                    checkLootComponents(object.get("components"), path);
                }
                case "minecraft:copy_components" -> {
                    JsonElement include = object.get("include");
                    boolean nameOnly = include != null && include.isJsonArray() && include.getAsJsonArray().size() == 1
                            && List.of("minecraft:custom_name", "custom_name").contains(include.getAsJsonArray().get(0).getAsString());
                    if (nameOnly && !object.has("exclude") && object.has("source") && "block_entity".equals(object.get("source").getAsString())) {
                        object.addProperty("function", "minecraft:copy_name");
                        object.remove("include");
                    } else {
                        problem("data components in a loot table (no 1.20.4 form): " + path);
                    }
                }
                case "minecraft:set_lore" -> {
                    if (object.has("mode")) {
                        String mode = object.remove("mode").getAsString();
                        if (mode.equals("replace_all")) {
                            object.addProperty("replace", true);
                        } else if (!mode.equals("append")) {
                            problem("set_lore mode " + mode + " in a loot table (1.20.4 replaces or appends): " + path);
                        }
                    }
                }
                case "minecraft:set_name" -> {
                    if (object.has("target")) {
                        String target = object.remove("target").getAsString();
                        if (target.equals("item_name") && !object.has("entity") && object.has("name")) {
                            // the item's name (1.20.5's item_name component, not a custom name): set as that component
                            JsonObject components = new JsonObject();
                            // (item_name's codec is the flat one: the text's JSON as a string)
                            components.addProperty("minecraft:item_name", object.remove("name").toString());
                            object.addProperty("function", "ladder:set_components");
                            object.add("components", components);
                        } else if (!target.equals("custom_name")) {
                            problem("set_name target " + target + " in a loot table (1.20.4 sets the custom name): " + path);
                        }
                    }
                }
                default -> {
                }
            }
        }
        for (var e : object.entrySet()) {
            // a function's component values are data, not loot structure
            if (!(object.has("function") && e.getKey().equals("components"))) {
                lootFunctions(e.getValue(), path);
            }
        }
    }

    /**
     * The component types a loot table sets must be ones the game has below: the adapters' (their DataComponents) for
     * minecraft:, a mod's own (registered by the mod, unchecked) otherwise.
     */
    static void checkLootComponents(JsonElement components, String path) {
        if (components == null || !components.isJsonObject()) {
            return;
        }
        for (String key : components.getAsJsonObject().keySet()) {
            String id = key.startsWith("!") ? key.substring(1) : key;
            id = id.contains(":") ? id : "minecraft:" + id;
            if (id.startsWith("minecraft:")) {
                if (!adapterComponents().contains(id.substring("minecraft:".length()))) {
                    problem("component " + id + " the adapters haven't, in a loot table: " + path);
                }
            } else {
                unverified.add("mod component " + id + " in a loot table (the mod registers it): " + path);
            }
        }
    }

    static Set<String> adapterComponentNames;

    /** the names of the game's components the adapters give: their DataComponents' Game constants (read from the jar) */
    static Set<String> adapterComponents() {
        if (adapterComponentNames != null) {
            return adapterComponentNames;
        }
        Set<String> names = new HashSet<>();
        String jar = System.getProperty("ladder.adapters");
        if (jar != null) {
            try (ZipFile zip = new ZipFile(jar)) {
                ZipEntry e = zip.getEntry(ADAPTERS + "component/DataComponents.class");
                if (e != null) {
                    try (InputStream in = zip.getInputStream(e)) {
                        ClassNode node = new ClassNode();
                        new ClassReader(in).accept(node, 0);
                        for (MethodNode m : node.methods) {
                            if (!m.name.equals("<clinit>")) {
                                continue;
                            }
                            // new Game("name", ...): the first string after each new Game
                            boolean game = false;
                            for (AbstractInsnNode insn : m.instructions) {
                                if (insn instanceof TypeInsnNode t && t.getOpcode() == Opcodes.NEW && t.desc.endsWith("DataComponents$Game")) {
                                    game = true;
                                } else if (game && insn instanceof LdcInsnNode ldc && ldc.cst instanceof String name) {
                                    names.add(name);
                                    game = false;
                                }
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
        adapterComponentNames = names;
        return names;
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
        String neoforge = switch (bottom) {
            case "1.20.4" -> "[20.4,)";
            case "1.20.6" -> "[20.6,)";
            case "1.21" -> "[21.0,)";
            default -> "[" + bottom + ",)";
        };
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
