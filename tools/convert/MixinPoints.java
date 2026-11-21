/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
import java.util.*;
import java.util.function.Function;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * Where a mixin injector lands: its target methods in the game's classes and, for INVOKE / INVOKE_ASSIGN / FIELD / NEW
 * points, the instruction in the method's body (ordinal included; slices are not narrowed, so a found point is an upper
 * bound). The converter checks each injector against the upper game with the mod's own code and against the bottom
 * game with the converted code: one that lands above and not below is a problem (it would inject nothing, or fail the
 * mixin, in the lower game).
 */
final class MixinPoints {
    static final Set<String> INJECTORS = Set.of(
            "Lorg/spongepowered/asm/mixin/injection/Inject;", "Lorg/spongepowered/asm/mixin/injection/Redirect;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyArg;", "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;", "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;",
            "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;", "Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;",
            "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;", "Lcom/llamalad7/mixinextras/injector/v2/WrapWithCondition;",
            "Lcom/llamalad7/mixinextras/injector/WrapWithCondition;", "Lcom/llamalad7/mixinextras/injector/ModifyReceiver;",
            "Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;");

    /** The verdict of an injector whose mixin targets only classes this game hasn't: it applies to nothing here. */
    static final String INACTIVE = "inactive: no target class in this game";

    /** The mod's own classes (set by the converter per mod): a mixin into one of them is not the game's change. */
    static java.util.function.Predicate<String> modClasses = name -> false;

    static boolean modClass(String name) {
        return modClasses.test(name);
    }

    private MixinPoints() {
    }

    /**
     * handler name -> null when the injector lands, else why not; injectors into classes the game hasn't (a mod's
     * own classes) and regex or wildcard selectors are left out (null).
     */
    static Map<String, String> check(ClassNode mixin, List<String> targets, Function<String, ClassNode> game) {
        Map<String, String> out = new LinkedHashMap<>();
        for (MethodNode m : mixin.methods) {
            for (AnnotationNode a : annotations(m)) {
                if (INJECTORS.contains(a.desc)) {
                    out.put(key(mixin, m), verdict(targets, m, a, game));
                }
            }
        }
        return out;
    }

    /**
     * An injector handler's key in the verdicts: its name and, when the mixin has overloads of that name, its place among
     * them ("name#1"). Descriptors change in the conversion (and in the repair), the order of the methods doesn't.
     */
    static String key(ClassNode mixin, MethodNode handler) {
        int index = 0, count = 0;
        for (MethodNode m : mixin.methods) {
            if (m.name.equals(handler.name)) {
                if (m == handler) {
                    index = count;
                }
                count++;
            }
        }
        return count > 1 ? handler.name + "#" + index : handler.name;
    }

    static List<AnnotationNode> annotations(MethodNode m) {
        List<AnnotationNode> out = new ArrayList<>();
        if (m.visibleAnnotations != null) {
            out.addAll(m.visibleAnnotations);
        }
        if (m.invisibleAnnotations != null) {
            out.addAll(m.invisibleAnnotations);
        }
        return out;
    }

    static String verdict(List<String> targets, MethodNode handler, AnnotationNode inj, Function<String, ClassNode> game) {
        List<String> selectors = strings(value(inj, "method"));
        List<AnnotationNode> ats = new ArrayList<>();
        Object at = value(inj, "at");
        if (at instanceof AnnotationNode a) {
            ats.add(a);
        } else if (at instanceof List<?> l) {
            l.forEach(o -> ats.add((AnnotationNode) o));
        }
        for (String sel : selectors) {
            if (sel.startsWith("/") || sel.contains("*")) {
                return null;
            }
        }
        boolean gameTarget = false, otherTarget = false;
        for (String t : targets) {
            ClassNode target = game.apply(t);
            if (target == null) {
                // a mod's own class (not checked here), or a class this game hasn't (another loader's or version's)
                otherTarget = true;
                continue;
            }
            gameTarget = true;
            List<MethodNode> methods = new ArrayList<>();
            for (String sel : selectors) {
                methods.addAll(match(target, sel));
            }
            if (methods.isEmpty()) {
                return "no target method " + selectors + " in " + t;
            }
            if (inj.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;") && !coerced(handler)
                    && methods.stream().noneMatch(m -> injectFits(handler, m))) {
                return "handler " + handler.desc + " doesn't fit " + t + "." + methods.get(0).name + methods.get(0).desc;
            }
            if (inj.desc.contains("WrapMethod")) {
                continue;
            }
            for (AnnotationNode a : ats) {
                String kind = (String) value(a, "value");
                String tgt = (String) value(a, "target");
                Integer ordinal = (Integer) value(a, "ordinal");
                if (kind == null || tgt == null || !(kind.equals("INVOKE") || kind.equals("INVOKE_ASSIGN") || kind.equals("FIELD") || kind.equals("NEW"))) {
                    continue;
                }
                int found = 0;
                for (MethodNode m : methods) {
                    found = Math.max(found, count(m, kind, tgt));
                }
                if (found == 0) {
                    return kind + " " + tgt + " not in " + t + "." + methods.get(0).name;
                }
                if (ordinal != null && ordinal >= 0 && found <= ordinal) {
                    return kind + " " + tgt + " ordinal " + ordinal + ", only " + found + " in " + t + "." + methods.get(0).name;
                }
            }
        }
        // no target class in this game at all: the mixin applies to nothing here (inactive), unlike one into a mod's class
        return gameTarget || otherTarget && targets.stream().anyMatch(MixinPoints::modClass) ? null : INACTIVE;
    }

    /** An @Inject handler takes the target's parameters (all or none), then its CallbackInfo, then captured locals. */
    static boolean injectFits(MethodNode handler, MethodNode target) {
        Type[] t = Type.getArgumentTypes(target.desc), h = Type.getArgumentTypes(handler.desc);
        int ci = -1;
        for (int i = 0; i < h.length; i++) {
            String n = h[i].getSort() == Type.OBJECT ? h[i].getInternalName() : "";
            if (n.equals("org/spongepowered/asm/mixin/injection/callback/CallbackInfo")
                    || n.equals("org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable")) {
                ci = i;
                break;
            }
        }
        if (ci < 0 || (handler.access & Opcodes.ACC_STATIC) != (target.access & Opcodes.ACC_STATIC) && !target.name.equals("<init>")) {
            return false;
        }
        if (ci == 0) {
            return true;
        }
        if (ci != t.length) {
            return false;
        }
        for (int i = 0; i < ci; i++) {
            if (!h[i].equals(t[i])) {
                return false;
            }
        }
        return true;
    }

    /** A handler with a @Coerce parameter takes a supertype on purpose: not checked here. */
    static boolean coerced(MethodNode handler) {
        for (List<AnnotationNode>[] lists : Arrays.asList(handler.visibleParameterAnnotations, handler.invisibleParameterAnnotations)) {
            if (lists == null) {
                continue;
            }
            for (List<AnnotationNode> l : lists) {
                if (l != null && l.stream().anyMatch(a -> a.desc.endsWith("/Coerce;"))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Lambdas are numbered by the compiler: an injector into {@code lambda$<outer>$<n>} whose point is not there below is
     * moved to the one lambda of the same outer method below that has all its points (and fits an @Inject handler).
     * Returns whether any injector moved.
     */
    static boolean retargetLambdas(ClassNode mixin, List<String> targets, Function<String, ClassNode> game, java.util.Set<String> moved) {
        boolean changed = false;
        for (MethodNode m : mixin.methods) {
            for (AnnotationNode inj : annotations(m)) {
                // (only an injector that landed above and not below: an optional one that matched nothing above stays)
                if (!INJECTORS.contains(inj.desc) || !moved.contains(key(mixin, m)) || verdict(targets, m, inj, game) == null) {
                    continue;
                }
                List<String> selectors = strings(value(inj, "method"));
                if (selectors.size() != 1 || !selectors.get(0).matches("lambda\\$[\\w$]+?\\$\\d+(\\(.*)?")) {
                    continue;
                }
                String sel = selectors.get(0);
                String outer = sel.substring(0, sel.lastIndexOf('$', sel.contains("(") ? sel.indexOf('(') : sel.length()) + 1);
                List<AnnotationNode> ats = new ArrayList<>();
                Object at = value(inj, "at");
                if (at instanceof AnnotationNode a) {
                    ats.add(a);
                } else if (at instanceof List<?> l) {
                    l.forEach(o -> ats.add((AnnotationNode) o));
                }
                String found = null;
                int candidates = 0;
                for (String t : targets) {
                    ClassNode target = game.apply(t);
                    if (target == null) {
                        continue;
                    }
                    for (MethodNode lambda : target.methods) {
                        if (!lambda.name.startsWith(outer) || !lambda.name.substring(outer.length()).matches("\\d+") || !fitsPoints(lambda, ats)) {
                            continue;
                        }
                        if (inj.desc.equals("Lorg/spongepowered/asm/mixin/injection/Inject;") && !coerced(m) && !injectFits(m, lambda)) {
                            continue;
                        }
                        String c = lambda.name + lambda.desc;
                        if (!c.equals(found)) {
                            candidates++;
                            found = c;
                        }
                    }
                }
                if (candidates == 1 && ats.stream().anyMatch(a -> value(a, "target") != null)) {
                    for (int i = 0; i + 1 < inj.values.size(); i += 2) {
                        if (inj.values.get(i).equals("method")) {
                            inj.values.set(i + 1, new ArrayList<>(List.of(found)));
                        }
                    }
                    changed = true;
                }
            }
        }
        return changed;
    }

    /** whether a method has every point of the @At list that names a target (INVOKE, FIELD, NEW), at its ordinal */
    static boolean fitsPoints(MethodNode m, List<AnnotationNode> ats) {
        for (AnnotationNode a : ats) {
            String kind = (String) value(a, "value");
            String tgt = (String) value(a, "target");
            Integer ordinal = (Integer) value(a, "ordinal");
            if (kind == null || tgt == null || !(kind.equals("INVOKE") || kind.equals("INVOKE_ASSIGN") || kind.equals("FIELD") || kind.equals("NEW"))) {
                continue;
            }
            int n = count(m, kind, tgt);
            if (n == 0 || ordinal != null && ordinal >= 0 && n <= ordinal) {
                return false;
            }
        }
        return true;
    }

    static List<MethodNode> match(ClassNode c, String sel) {
        String s = sel;
        if (s.startsWith("L") && s.contains(";")) {
            s = s.substring(s.indexOf(';') + 1);
        }
        String name = s.contains("(") ? s.substring(0, s.indexOf('(')) : s;
        String desc = s.contains("(") ? s.substring(s.indexOf('(')) : null;
        List<MethodNode> out = new ArrayList<>();
        for (MethodNode m : c.methods) {
            if (m.name.equals(name) && (desc == null || m.desc.equals(desc))) {
                out.add(m);
            }
        }
        return out;
    }

    static int count(MethodNode m, String kind, String target) {
        String owner = target.startsWith("L") && target.contains(";") ? target.substring(1, target.indexOf(';')) : null;
        String rest = owner == null ? target : target.substring(target.indexOf(';') + 1);
        int n = 0;
        for (AbstractInsnNode insn : m.instructions) {
            switch (kind) {
                case "INVOKE", "INVOKE_ASSIGN" -> {
                    if (insn instanceof MethodInsnNode call && (owner == null || owner.equals(call.owner))
                            && (call.name + call.desc).startsWith(rest.contains("(") ? rest : rest + "(")) {
                        n++;
                    }
                }
                case "FIELD" -> {
                    String fname = rest.contains(":") ? rest.substring(0, rest.indexOf(':')) : rest;
                    String fdesc = rest.contains(":") ? rest.substring(rest.indexOf(':') + 1) : null;
                    if (insn instanceof FieldInsnNode f && (owner == null || owner.equals(f.owner)) && f.name.equals(fname)
                            && (fdesc == null || fdesc.equals(f.desc))) {
                        n++;
                    }
                }
                case "NEW" -> {
                    String type = target.startsWith("L") && target.endsWith(";") ? target.substring(1, target.length() - 1)
                            : owner != null ? owner : target;
                    if (insn instanceof TypeInsnNode t && t.getOpcode() == Opcodes.NEW && t.desc.equals(type)) {
                        n++;
                    }
                }
                default -> {
                }
            }
        }
        return n;
    }

    static Object value(AnnotationNode a, String key) {
        if (a.values == null) {
            return null;
        }
        for (int i = 0; i + 1 < a.values.size(); i += 2) {
            if (a.values.get(i).equals(key)) {
                return a.values.get(i + 1);
            }
        }
        return null;
    }

    static List<String> strings(Object v) {
        List<String> out = new ArrayList<>();
        if (v instanceof List<?> l) {
            l.forEach(o -> out.add(String.valueOf(o)));
        } else if (v instanceof String s) {
            out.add(s);
        }
        return out;
    }

    /** Every method gets its parameter list (MethodParameters), as the Mixin tooling that edits handlers expects. */
    static void ensureParameters(ClassNode node) {
        for (MethodNode m : node.methods) {
            if (m.parameters == null) {
                m.parameters = new ArrayList<>();
                for (int i = 0; i < Type.getArgumentTypes(m.desc).length; i++) {
                    m.parameters.add(new ParameterNode("arg" + i, 0));
                }
            }
        }
    }
}
