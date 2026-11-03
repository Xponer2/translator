/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.translator.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.objectweb.asm.Type;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.discovery.ASMDataTable;

public final class ModList {
    private static final ModList INSTANCE = new ModList();
    @Nullable
    private static ASMDataTable scanData;

    private ModList() { }

    public static ModList get() {
        return INSTANCE;
    }

    /** The annotation data of the mod jars (FMLPreInitializationEvent#getAsmData), for getAllScanData. */
    public static void setScanData(ASMDataTable data) {
        scanData = data;
    }

    /** A 1.12.2 mod or an original 1.21.1 mod the Translator runs. */
    public boolean isLoaded(String modId) {
        return Loader.isModLoaded(modId) || io.github.xponer2.translator.runtime.neoforge.ModContainer.byId(modId) != null;
    }

    /** The container of a mod: the runtime one of a 1.21.1 mod, else a container made for the 1.12.2 mod. */
    public java.util.Optional<io.github.xponer2.translator.runtime.neoforge.ModContainer> getModContainerById(String modId) {
        var container = io.github.xponer2.translator.runtime.neoforge.ModContainer.byId(modId);
        if (container == null && Loader.isModLoaded(modId)) {
            var mod = Loader.instance().getIndexedModList().get(modId);
            container = new io.github.xponer2.translator.runtime.neoforge.ModContainer(modId, mod.getName(), mod.getVersion(), "", mod.getSource());
        }
        return java.util.Optional.ofNullable(container);
    }

    /** Each loaded mod's id and container (1.21 {@code forEachModContainer}). */
    public void forEachModContainer(java.util.function.BiConsumer<String, io.github.xponer2.translator.runtime.neoforge.ModContainer> action) {
        for (var mod : net.minecraftforge.fml.common.Loader.instance().getActiveModList()) {
            this.getModContainerById(mod.getModId()).ifPresent(container -> action.accept(mod.getModId(), container));
        }
    }

    public List<io.github.xponer2.translator.runtime.neoforge.ModInfo> getMods() {
        return io.github.xponer2.translator.runtime.neoforge.LoadingModList.get().getMods();
    }

    @Nullable
    public io.github.xponer2.translator.runtime.neoforge.ModFileInfo getModFileById(String modId) {
        return this.getModContainerById(modId).map(container -> container.getModInfo().getOwningFile()).orElse(null);
    }

    /** The display name of a mod, its id if it isn't loaded. */
    public String getDisplayName(String modId) {
        var container = Loader.instance().getIndexedModList().get(modId);
        return container == null ? modId : container.getName();
    }

    /**
     * NeoForge scan data: the original 1.21.1 mods' (one per jar), then one entry with the annotations of the 1.12.2
     * mods (from Cucumber's pre-init on).
     */
    public List<ModFileScanData> getAllScanData() {
        var list = new ArrayList<ModFileScanData>(io.github.xponer2.translator.runtime.RuntimeMods.scanData());
        if (scanData == null) {
            return list;
        }

        var annotations = new LinkedHashSet<AnnotationData>();
        for (var mod : Loader.instance().getActiveModList()) {
            // null for the built-in containers (minecraft, mcp, FML, forge): they have no scanned source
            var modData = scanData.getAnnotationsFor(mod);
            if (modData == null) {
                continue;
            }

            for (var data : modData.values()) {
                annotations.add(new AnnotationData(data.getAnnotationName(), data.getClassName(), data.getObjectName()));
            }
        }

        list.add(new ModFileScanData(annotations));
        return list;
    }

    public static final class ModFileScanData {
        private final Set<AnnotationData> annotations;
        private final Set<ClassData> classes;
        private final List<io.github.xponer2.translator.runtime.neoforge.ModFileInfo> infos;

        public ModFileScanData(Set<AnnotationData> annotations) {
            this(annotations, Collections.emptySet());
        }

        public ModFileScanData(Set<AnnotationData> annotations, Set<ClassData> classes) {
            this(annotations, classes, Collections.emptyList());
        }

        public ModFileScanData(Set<AnnotationData> annotations, Set<ClassData> classes, List<io.github.xponer2.translator.runtime.neoforge.ModFileInfo> infos) {
            this.annotations = annotations;
            this.classes = classes;
            this.infos = infos;
        }

        /** NeoForge {@code getIModInfoData}: the jar's mod file (its mods), what an annotation's mod is found by. */
        public List<io.github.xponer2.translator.runtime.neoforge.ModFileInfo> getIModInfoData() {
            return this.infos;
        }

        public Set<AnnotationData> getAnnotations() {
            return this.annotations;
        }

        /** NeoForge {@code getClasses}: every class of the jar with its superclass and interfaces. */
        public Set<ClassData> getClasses() {
            return this.classes;
        }

        /** NeoForge {@code getAnnotatedBy}: the annotations of that type on that kind of element. */
        public java.util.stream.Stream<AnnotationData> getAnnotatedBy(Class<? extends java.lang.annotation.Annotation> type,
                java.lang.annotation.ElementType elementType) {
            Type wanted = Type.getType(type);
            return this.annotations.stream().filter(data -> data.annotationType().equals(wanted) && data.targetType() == elementType);
        }
    }

    /** NeoForge {@code ModFileScanData.ClassData}: a class, its superclass and its interfaces. */
    public static final class ClassData {
        private final Type clazz;
        private final Type parent;
        private final Set<Type> interfaces;

        public ClassData(Type clazz, Type parent, Set<Type> interfaces) {
            this.clazz = clazz;
            this.parent = parent;
            this.interfaces = interfaces;
        }

        public Type clazz() {
            return this.clazz;
        }

        public Type parent() {
            return this.parent;
        }

        public Set<Type> interfaces() {
            return this.interfaces;
        }
    }

    /** NeoForge {@code ModFileScanData.AnnotationData}: annotation type, element type, class, member, values. */
    public static final class AnnotationData {
        private final String annotation;
        private final String className;
        private final String objectName;
        private final java.lang.annotation.ElementType targetType;
        private final java.util.Map<String, Object> values;

        AnnotationData(String annotation, String className, String objectName) {
            this(annotation, className, objectName, objectName == null || objectName.equals(className) ? java.lang.annotation.ElementType.TYPE
                    : objectName.contains("(") ? java.lang.annotation.ElementType.METHOD : java.lang.annotation.ElementType.FIELD,
                    Collections.emptyMap());
        }

        /** {@code annotation}: a descriptor (Lpkg/Name;) or a class name; {@code className} with dots. */
        public AnnotationData(String annotation, String className, String objectName, java.lang.annotation.ElementType targetType,
                java.util.Map<String, Object> values) {
            this.annotation = annotation.startsWith("L") && annotation.endsWith(";") ? annotation.substring(1, annotation.length() - 1) : annotation;
            this.className = className;
            this.objectName = objectName;
            this.targetType = targetType;
            this.values = values;
        }

        public Type annotationType() {
            return Type.getObjectType(this.annotation.replace('.', '/'));
        }

        public java.lang.annotation.ElementType targetType() {
            return this.targetType;
        }

        /** The annotated class (class annotations) or member. */
        public String memberName() {
            return this.objectName == null || this.objectName.equals(this.className) ? this.className : this.objectName;
        }

        public Type clazz() {
            return Type.getObjectType(this.className.replace('.', '/'));
        }

        public java.util.Map<String, Object> annotationData() {
            return this.values;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof AnnotationData other && other.annotation.equals(this.annotation) && other.className.equals(this.className)
                    && String.valueOf(other.objectName).equals(String.valueOf(this.objectName));
        }

        @Override
        public int hashCode() {
            return (this.annotation + this.className + this.objectName).hashCode();
        }
    }
}
