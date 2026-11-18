/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
import java.util.*;
import java.util.function.Function;

import com.google.common.collect.ImmutableMultimap;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.sinytra.adapter.env.ctx.PatchEnvironment;
import org.sinytra.adapter.env.ctx.RefmapHolder;
import org.sinytra.adapter.patch.DynamicPatches;
import org.sinytra.adapter.patch.Patcher;
import org.sinytra.adapter.patch.TxPhase;
import org.sinytra.adapter.transform.MethodTransformer;
import org.sinytra.adapter.types.BytecodeFixerUpper;
import org.sinytra.adapter.util.provider.ClassLookup;
import org.spongepowered.asm.mixin.FabricUtil;

/**
 * A mixin whose injection points moved below is patched by Sinytra Adapter's dynamic mixin patches (MIT, a library of
 * the converter): it compares the target method as the mixin was written for it (the upper game) with the method
 * below (the bottom game), and moves the injection point, retypes or drops handler parameters, or wraps a changed
 * call. The converter checks the result again: a point still missing is a problem.
 */
final class MixinRepair {
    private final Patcher patcher;
    private final PatchEnvironment env;

    /** upper: the game the mixins were written for; lower: the bottom game (both in the converted names). */
    MixinRepair(Function<String, ClassNode> upper, Function<String, ClassNode> lower) {
        // Mixin's class metadata (used to rebuild handler locals) needs its environment, served by OfflineMixinService
        org.spongepowered.asm.launch.MixinBootstrap.init();
        ClassLookup clean = name -> Optional.ofNullable(upper.apply(name));
        ClassLookup dirty = name -> Optional.ofNullable(lower.apply(name));
        this.env = PatchEnvironment.create(new RefmapHolder() {
            @Override
            public String remap(String cls, String reference) {
                // NeoForge mods name the game's members as they are at run time: no refmap
                return reference;
            }

            @Override
            public void copyEntries(String from, String to) {
            }
        }, clean, dirty, new BytecodeFixerUpper(clean, dirty, List.of(), providers()), FabricUtil.COMPATIBILITY_LATEST);
        this.patcher = Patcher.builder(env).classTransformers(DynamicPatches.CLASS_PATCHES)
                .methodTransformers(ImmutableMultimap.<TxPhase, MethodTransformer>builder().putAll(DynamicPatches.methodTransformers(List.of())).build())
                .build();
    }

    /**
     * Patches the mixin in place; the mod's own classes and the bottom game serve Mixin's class metadata. Returns null,
     * or why the patch failed (the mixin is then left as it was).
     */
    String patch(ClassNode mixin, Map<String, byte[]> modClasses, Function<String, ClassNode> lower) {
        OfflineMixinService.lookup = name -> {
            byte[] b = modClasses.get(name + ".class");
            if (b == null) {
                return lower.apply(name);
            }
            ClassNode c = new ClassNode();
            new ClassReader(b).accept(c, 0);
            return c;
        };
        Set<MethodNode> hadNone = new HashSet<>();
        for (MethodNode m : mixin.methods) {
            if (m.parameters == null) {
                hadNone.add(m);
            }
        }
        MixinPoints.ensureParameters(mixin);
        try {
            patcher.process(mixin);
            return null;
        } catch (Throwable t) {
            return t.toString();
        } finally {
            // the parameter lists were only for the patches; parameter annotations (@Local, @Share...) follow the
            // handler's new parameters: as many slots as the descriptor has
            for (MethodNode m : mixin.methods) {
                if (hadNone.contains(m)) {
                    m.parameters = null;
                }
                int n = org.objectweb.asm.Type.getArgumentTypes(m.desc).length;
                m.visibleParameterAnnotations = fit(m.visibleParameterAnnotations, n);
                m.invisibleParameterAnnotations = fit(m.invisibleParameterAnnotations, n);
                m.visibleAnnotableParameterCount = 0;
                m.invisibleAnnotableParameterCount = 0;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static List<org.objectweb.asm.tree.AnnotationNode>[] fit(List<org.objectweb.asm.tree.AnnotationNode>[] annotations, int n) {
        if (annotations == null || annotations.length == n) {
            return annotations;
        }
        return Arrays.copyOf(annotations, n);
    }

    /** Our value conversions first (Holder of X and X, MapCodec and Codec...), then Adapter's own. */
    static List<org.sinytra.adapter.types.TypeAdapterProvider> providers() {
        List<org.sinytra.adapter.types.TypeAdapterProvider> out = new ArrayList<>();
        out.add(new LadderTypeAdapters());
        out.addAll(BytecodeFixerUpper.DEFAULT_PROVIDERS);
        return out;
    }

    /** Adapter's own account of what it matched and changed (-Dladder.mixinRepair.audit=true prints it). */
    String audit() {
        return env.auditTrail().getCompleteReport();
    }

    /** Mixin classes the patches made (an injector split off into a class of its own): the converter can't place them yet. */
    Collection<String> generated() {
        List<String> out = new ArrayList<>();
        env.classGenerator().getGeneratedMixinClasses().values().forEach(g -> out.add(g.generatedName()));
        return out;
    }
}
