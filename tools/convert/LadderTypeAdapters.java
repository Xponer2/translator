/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
import java.util.function.BiConsumer;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.sinytra.adapter.types.SimpleTypeAdapter;
import org.sinytra.adapter.types.TypeAdapter;
import org.sinytra.adapter.types.TypeAdapterProvider;

/**
 * The ladder's value conversions (the converter's generated bridges use the same, ladder.adapter.bridge.Conversions) for
 * Sinytra Adapter's mixin patches: when a handler's parameter changed type below (a Holder of X became X, a MapCodec a
 * Codec...), the value the lower game passes is converted into what the handler's code expects. {@code from} is the
 * value's type, {@code to} the type needed.
 */
final class LadderTypeAdapters implements TypeAdapterProvider {
    static final String CONVERSIONS = "ladder/adapter/bridge/Conversions";
    static final String HOLDER = "net/minecraft/core/Holder", REFERENCE = "net/minecraft/core/Holder$Reference";
    static final String OPTIONAL = "java/util/Optional", CODEC = "com/mojang/serialization/Codec", MAP_CODEC = "com/mojang/serialization/MapCodec";
    static final String PROVIDER = "net/minecraft/core/HolderLookup$Provider", REGISTRY_ACCESS = "net/minecraft/core/RegistryAccess";
    static final String RESOURCE_KEY = "net/minecraft/resources/ResourceKey", RESOURCE_LOCATION = "net/minecraft/resources/ResourceLocation";
    static final String DELTA_TRACKER = "ladder/adapter/provided/DeltaTracker";

    @Override
    public TypeAdapter provide(Type from, Type to) {
        if (from.equals(to)) {
            return null;
        }
        String f = from.getSort() == Type.OBJECT ? from.getInternalName() : null;
        String t = to.getSort() == Type.OBJECT ? to.getInternalName() : null;
        if (from.getSort() == Type.FLOAT && DELTA_TRACKER.equals(t)) {
            return call(from, to, "deltaTracker", "(F)L" + DELTA_TRACKER + ";", null);
        }
        if (f == null || t == null) {
            return null;
        }
        if (t.equals(HOLDER) && !f.equals(HOLDER) && !f.equals(REFERENCE)) {
            return call(from, to, "holder", "(Ljava/lang/Object;)L" + HOLDER + ";", null);
        }
        if (t.equals(REFERENCE) && !f.equals(HOLDER) && !f.equals(REFERENCE)) {
            return call(from, to, "reference", "(Ljava/lang/Object;)L" + REFERENCE + ";", null);
        }
        if ((f.equals(HOLDER) || f.equals(REFERENCE)) && !t.equals(HOLDER)) {
            return call(from, to, "unwrap", "(L" + HOLDER + ";)Ljava/lang/Object;", t);
        }
        if (t.equals(OPTIONAL)) {
            return call(from, to, "optional", "(Ljava/lang/Object;)L" + OPTIONAL + ";", null);
        }
        if (f.equals(OPTIONAL)) {
            return call(from, to, "orNull", "(L" + OPTIONAL + ";)Ljava/lang/Object;", t);
        }
        if (f.equals(CODEC) && t.equals(MAP_CODEC)) {
            return call(from, to, "mapCodec", "(L" + CODEC + ";)L" + MAP_CODEC + ";", null);
        }
        if (f.equals(PROVIDER) && t.equals(REGISTRY_ACCESS)) {
            return call(from, to, "registryAccess", "(L" + PROVIDER + ";)L" + REGISTRY_ACCESS + ";", null);
        }
        if (f.equals(RESOURCE_LOCATION) && t.equals(RESOURCE_KEY)) {
            // which registry the key is of can't be told from a location (a loot table's isn't a registered value):
            // no conversion, the repair fails and the converter reports the injector
            return null;
        }
        if (f.equals(RESOURCE_KEY) && t.equals(RESOURCE_LOCATION)) {
            return call(from, to, "location", "(L" + RESOURCE_KEY + ";)L" + RESOURCE_LOCATION + ";", null);
        }
        if (f.equals(RESOURCE_KEY)) {
            return call(from, to, "valueOfKey", "(L" + RESOURCE_KEY + ";)Ljava/lang/Object;", t);
        }
        if (t.equals(RESOURCE_KEY)) {
            return call(from, to, "keyOfValue", "(Ljava/lang/Object;)L" + RESOURCE_KEY + ";", null);
        }
        return null;
    }

    /** After the instruction that leaves the value: the conversion's call, then a cast to the needed type when it returns Object. */
    static TypeAdapter call(Type from, Type to, String name, String desc, String cast) {
        BiConsumer<InsnList, AbstractInsnNode> patch = (list, insn) -> {
            InsnList code = new InsnList();
            code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONVERSIONS, name, desc, false));
            if (cast != null) {
                code.add(new TypeInsnNode(Opcodes.CHECKCAST, cast));
            }
            list.insert(insn, code);
        };
        return new SimpleTypeAdapter(from, to, patch::accept);
    }
}
