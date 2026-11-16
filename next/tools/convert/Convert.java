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

    /** NeoForge classes that moved between 21.x and 20.4 (no intermediary for NeoForge): their own rows. */
    static final Map<String, String> NEOFORGE_MOVES = Map.of(
            "net/neoforged/fml/common/EventBusSubscriber", "net/neoforged/fml/common/Mod$EventBusSubscriber",
            "net/neoforged/fml/common/EventBusSubscriber$Bus", "net/neoforged/fml/common/Mod$EventBusSubscriber$Bus");

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
            System.out.println(clean + " of " + jars.size() + " mods convert with no problem; " + report);
            return;
        }
        convert(in, out);
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
    }

    static void convertEntries(Path in, Path out, Map<String, byte[]> entries) throws IOException {
        for (var e : entries.entrySet()) {
            if (e.getKey().endsWith(".class") && !e.getKey().startsWith("META-INF/")) {
                ClassNode node = new ClassNode();
                new ClassReader(e.getValue()).accept(node, ClassReader.SKIP_CODE);
                Stage0.ModClass mc = new Stage0.ModClass();
                mc.superName = node.superName;
                mc.interfaces = node.interfaces.toArray(new String[0]);
                mod.put(node.name, mc);
                List<String> targets = mixinTargetsOf(node);
                if (!targets.isEmpty()) {
                    mixinTargets.put(node.name, targets);
                }
            }
        }
        Remapper remapper = new Remapper() {
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
                    ClassWriter writer = new ClassWriter(0);
                    node.accept(new ClassRemapper(writer, remapper));
                    data = writer.toByteArray();
                    name = remapper.map(name.substring(0, name.length() - 6)) + ".class";
                } else if (name.equals("META-INF/mods.toml") && newMetadata) {
                    // a jar for both versions: the 1.21 metadata is the one converted, this one is the older loader's
                    continue;
                } else if (name.equals("META-INF/neoforge.mods.toml")) {
                    name = "META-INF/mods.toml";
                    data = modsToml(new String(data, StandardCharsets.UTF_8), bottom).getBytes(StandardCharsets.UTF_8);
                } else if (name.endsWith("mixins.json")) {
                    data = new String(data, StandardCharsets.UTF_8).replaceAll("\"JAVA_2[0-9]\"", "\"JAVA_17\"").getBytes(StandardCharsets.UTF_8);
                } else if (name.startsWith("META-INF/jarjar/") && name.endsWith(".jar")) {
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
        }
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

    static String mapClass(String c) {
        String cached = classCache.get(c);
        if (cached != null) {
            cachedProblems.getOrDefault("C " + c, List.of()).forEach(problems::add);
            return cached;
        }
        collecting = new ArrayList<>();
        String result;
        if (NEOFORGE_MOVES.containsKey(c)) {
            result = NEOFORGE_MOVES.get(c);
        } else {
            String cur = c;
            for (int i = 0; i + 1 < versions.size(); i++) {
                Stage0.Step s = Stage0.step(versions.get(i), versions.get(i + 1), new Stage0.Ref('C', cur, null, null));
                if (s.to() == null) {
                    problem("missing class " + c + " (" + versions.get(i).id + " -> " + versions.get(i + 1).id + ")");
                    cur = c;
                    break;
                }
                cur = s.to().cls();
            }
            result = cur;
        }
        cachedProblems.put("C " + c, collecting);
        collecting = null;
        classCache.put(c, result);
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

    /** The member's final name, or null when it isn't a platform member at all. */
    static String finalName(String owner, String name, String desc, boolean method) {
        Stage0.Version top = versions.get(0);
        for (String start : starts(owner)) {
            String decl = top.declaring(start, name, desc, method);
            if (decl == null) {
                continue;
            }
            Stage0.Ref end = walk(new Stage0.Ref(method ? 'M' : 'F', decl, name, desc));
            return end == null ? name : end.name();
        }
        return null;
    }

    static String mapMember(String owner, String name, String desc, boolean method) {
        if (owner.startsWith("[")) {
            return name;
        }
        String mapped = finalName(owner, name, desc, method);
        return mapped == null ? name : mapped;
    }

    static void validateConstructor(String owner, String desc) {
        Stage0.Version top = versions.get(0);
        if (top.declaring(owner, "<init>", desc, true) != null) {
            walk(new Stage0.Ref('M', owner, "<init>", desc));
        }
    }

    static Stage0.Ref walk(Stage0.Ref ref) {
        String key = ref.key();
        if (memberCache.containsKey(key)) {
            cachedProblems.getOrDefault(key, List.of()).forEach(problems::add);
            return memberCache.get(key);
        }
        collecting = new ArrayList<>();
        Stage0.Ref result = walkUncached(ref);
        cachedProblems.put(key, collecting);
        collecting = null;
        memberCache.put(key, result);
        return result;
    }

    static Stage0.Ref walkUncached(Stage0.Ref ref) {
        Stage0.Ref cur = ref;
        for (int i = 0; i + 1 < versions.size() && cur != null; i++) {
            Stage0.Version from = versions.get(i), to = versions.get(i + 1);
            Stage0.Step s = Stage0.step(from, to, cur);
            switch (s.outcome()) {
                case SAME, RENAMED -> cur = s.to();
                case CHANGED -> {
                    problem("changed descriptor, no bridge yet: " + cur.key() + " -> " + s.to().name() + s.to().desc() + " (" + from.id + " -> " + to.id + ")");
                    cur = s.to();
                }
                default -> {
                    problem("missing " + cur.key() + " (" + from.id + " -> " + to.id + ")");
                    cur = null;
                }
            }
        }
        return cur;
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
            return json;
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
