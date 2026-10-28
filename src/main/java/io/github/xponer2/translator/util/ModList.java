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

    public boolean isLoaded(String modId) {
        return Loader.isModLoaded(modId);
    }

    /** The display name of a mod, its id if it isn't loaded. */
    public String getDisplayName(String modId) {
        var container = Loader.instance().getIndexedModList().get(modId);
        return container == null ? modId : container.getName();
    }

    /** NeoForge scan data: one entry with the annotations of all mods (empty before Cucumber's pre-init). */
    public List<ModFileScanData> getAllScanData() {
        if (scanData == null) {
            return Collections.emptyList();
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

        var list = new ArrayList<ModFileScanData>();
        list.add(new ModFileScanData(annotations));
        return list;
    }

    public static final class ModFileScanData {
        private final Set<AnnotationData> annotations;

        ModFileScanData(Set<AnnotationData> annotations) {
            this.annotations = annotations;
        }

        public Set<AnnotationData> getAnnotations() {
            return this.annotations;
        }
    }

    public static final class AnnotationData {
        private final String annotation;
        private final String className;
        private final String objectName;

        AnnotationData(String annotation, String className, String objectName) {
            this.annotation = annotation;
            this.className = className;
            this.objectName = objectName;
        }

        public Type annotationType() {
            return Type.getObjectType(this.annotation.replace('.', '/'));
        }

        /** The annotated class (class annotations) or member. */
        public String memberName() {
            return this.objectName == null || this.objectName.equals(this.className) ? this.className : this.objectName;
        }

        public String clazz() {
            return this.className;
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
