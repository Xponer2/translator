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
package io.github.xponer2.translator.runtime;

import java.util.Map;

import xyz.wagyourtail.jvmdg.shade.asm.ClassVisitor;
import xyz.wagyourtail.jvmdg.shade.asm.MethodVisitor;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;
import xyz.wagyourtail.jvmdg.shade.asm.commons.ClassRemapper;
import xyz.wagyourtail.jvmdg.shade.asm.commons.MethodRemapper;

/**
 * The class remapper with the facades (neoforge-1.21.1-supers.tsv): a 1.21.1 class with a facade means the facade
 * where the mod creates or extends it ({@code extends}, {@code new}, constructor and super calls) and for the static
 * members the facade declares; everywhere else (descriptors, calls on any instance) it means the 1.12.2 class.
 */
final class FacadeRemapper extends ClassRemapper {
	private final ModRemapper names;
	private final Map<String, String> supers;
	private final TargetClasses target;

	FacadeRemapper(ClassVisitor next, ModRemapper names, Map<String, String> supers, TargetClasses target) {
		super(Opcodes.ASM9, next, names);
		this.names = names;
		this.supers = supers;
		this.target = target;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		// ClassRemapper#visit, with the facade as the superclass
		this.className = name;
		String facade = superName == null ? null : this.supers.get(superName);
		this.cv.visit(version, access, this.remapper.mapType(name), this.remapper.mapSignature(signature, false),
				facade != null ? facade : this.remapper.mapType(superName), interfaces == null ? null : this.interfaces(interfaces));
	}

	/**
	 * The interfaces, without those that are 1.12.2 / Translator classes now (Equipable is ItemArmor, CraftingContainer
	 * InventoryCrafting): the JVM refuses to load a class that "implements" a class. The class loses that type.
	 */
	private String[] interfaces(String[] interfaces) {
		var out = new java.util.LinkedHashSet<String>();
		for (String mapped : this.remapper.mapTypes(interfaces)) {
			ClassInfo info = this.names.isOwn(mapped) ? null : this.target.get(mapped);
			if (info == null || info.isInterface()) {
				out.add(mapped);
			}
			// a 1.21 BakedModel (IBakedModel now, or NeoForge's IDynamicBakedModel): 1.12.2's getQuads(state, side, long)
			// on to the mod's getQuads with a random (and model data)
			if (BAKED_MODELS.contains(mapped)) {
				out.add(BAKED_MODEL_BRIDGE);
			}
		}
		return out.toArray(new String[0]);
	}

	private static final String BAKED_MODEL_BRIDGE = "io/github/xponer2/translator/client/model/BakedModelBridge";
	private static final java.util.Set<String> BAKED_MODELS = new java.util.HashSet<>(java.util.Arrays.asList(
			"net/minecraft/client/renderer/block/model/IBakedModel", "net/neoforged/neoforge/client/model/IDynamicBakedModel"));

	// --- mixins: the names in the strings of their annotations (method = "tick", @At(target = "L...;m(..)V")) are 1.21
	// names; the class literals are remapped like any type

	@Override
	public xyz.wagyourtail.jvmdg.shade.asm.AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		var visitor = super.visitAnnotation(descriptor, visible);
		return visitor != null && ClassInfo.MIXIN.equals(descriptor) ? new MixinStrings(visitor, null) : visitor;
	}

	private static boolean isMixinAnnotation(String descriptor) {
		return descriptor.startsWith("Lorg/spongepowered/asm/mixin/") || descriptor.startsWith("Lcom/llamalad7/mixinextras/");
	}

	private static final java.util.regex.Pattern MEMBER = java.util.regex.Pattern.compile("L([\\w/$]+);([\\w$<>*]+)(\\(.*|:.*)");
	private static final java.util.regex.Pattern OWN_METHOD = java.util.regex.Pattern.compile("([\\w$<>*]+)(\\(.*)");
	private static final java.util.regex.Pattern NAME = java.util.regex.Pattern.compile("[\\w$]+");
	private static final java.util.regex.Pattern CLASS = java.util.regex.Pattern.compile("L[\\w/$]+;");

	/** A string of a mixin annotation attribute with the 1.12.2 names. */
	private String mixinString(String attribute, String value) {
		if ("targets".equals(attribute)) {
			return this.remapper.mapType(value.replace('.', '/')).replace('/', value.indexOf('.') >= 0 ? '.' : '/');
		}
		if (!"method".equals(attribute) && !"target".equals(attribute)) {
			return value;
		}
		var member = MEMBER.matcher(value);
		if (member.matches()) {
			String owner = member.group(1);
			String name = member.group(2);
			String rest = member.group(3);
			if (rest.startsWith(":")) {
				String desc = rest.substring(1);
				return "L" + this.remapper.mapType(owner) + ";" + this.remapper.mapFieldName(owner, name, desc) + ":" + this.remapper.mapDesc(desc);
			}
			return "L" + this.remapper.mapType(owner) + ";" + this.remapper.mapMethodName(owner, name, rest) + this.remapper.mapMethodDesc(rest);
		}
		if (CLASS.matcher(value).matches()) {
			return this.remapper.mapDesc(value);
		}
		if (!"method".equals(attribute)) {
			return value;
		}
		var own = OWN_METHOD.matcher(value);
		for (String target : this.names.mixinTargets(this.className)) {
			if (own.matches()) {
				String mapped = this.remapper.mapMethodName(target, own.group(1), own.group(2));
				if (!mapped.equals(own.group(1))) {
					return mapped + this.remapper.mapMethodDesc(own.group(2));
				}
			} else if (NAME.matcher(value).matches()) {
				String mapped = this.names.mapBareMethodName(target, value);
				if (!mapped.equals(value)) {
					return mapped;
				}
			}
		}
		return own.matches() ? own.group(1) + this.remapper.mapMethodDesc(own.group(2)) : value;
	}

	/** Remaps the strings of a mixin annotation (and of its nested annotations and arrays) on the way to the writer. */
	private final class MixinStrings extends xyz.wagyourtail.jvmdg.shade.asm.AnnotationVisitor {
		private final String array;

		MixinStrings(xyz.wagyourtail.jvmdg.shade.asm.AnnotationVisitor next, String array) {
			super(Opcodes.ASM9, next);
			this.array = array;
		}

		@Override
		public void visit(String name, Object value) {
			if ("require".equals(name)) {
				// an injection that finds no target on 1.12.2 is left out, it doesn't stop the game (the configs say
				// defaultRequire 0, an explicit require would override that)
				return;
			}
			String attribute = name != null ? name : this.array;
			super.visit(name, value instanceof String string && attribute != null ? FacadeRemapper.this.mixinString(attribute, string) : value);
		}

		@Override
		public void visitEnum(String name, String descriptor, String value) {
			// a callback whose captured locals differ on 1.12.2 is left out, it doesn't stop the game
			if ("Lorg/spongepowered/asm/mixin/injection/callback/LocalCapture;".equals(descriptor)
					&& ("CAPTURE_FAILHARD".equals(value) || "CAPTURE_FAILEXCEPTION".equals(value))) {
				value = "CAPTURE_FAILSOFT";
			}
			super.visitEnum(name, descriptor, value);
		}

		@Override
		public xyz.wagyourtail.jvmdg.shade.asm.AnnotationVisitor visitArray(String name) {
			var visitor = super.visitArray(name);
			return visitor == null ? null : new MixinStrings(visitor, name);
		}

		@Override
		public xyz.wagyourtail.jvmdg.shade.asm.AnnotationVisitor visitAnnotation(String name, String descriptor) {
			var visitor = super.visitAnnotation(name, descriptor);
			return visitor == null ? null : new MixinStrings(visitor, null);
		}
	}

	/** name + desc of the methods written so far: two 1.21 methods can get the same 1.12.2 name and descriptor. */
	private final java.util.Set<String> written = new java.util.HashSet<>();

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		String mappedName = this.remapper.mapMethodName(this.className, name, descriptor);
		String mappedDesc = this.remapper.mapMethodDesc(descriptor);
		if (!this.written.add(mappedName + mappedDesc)) {
			if ((access & Opcodes.ACC_BRIDGE) != 0) {
				// a bridge to a method whose types are the same now (BlockHitResult and HitResult are RayTraceResult)
				return null;
			}
			// the other one keeps its 1.21 name (or a new one if that is taken too)
			String own = mappedName.equals(name) || !this.written.add(name + mappedDesc) ? name + "$" + this.written.size() : name;
			this.written.add(own + mappedDesc);
			MethodVisitor visitor = this.cv.visitMethod(access, own, mappedDesc, this.remapper.mapSignature(signature, false),
					exceptions == null ? null : this.remapper.mapTypes(exceptions));
			return visitor == null ? null : this.createMethodRemapper(visitor);
		}
		return super.visitMethod(access, name, descriptor, signature, exceptions);
	}

	@Override
	public void visitInnerClass(String name, String outerName, String innerName, int access) {
		// a 1.21.1 nested class that became a top-level class (RegistryAccess$Frozen -> RegistryAccess) loses its entry:
		// the JVM rejects a class "inner of itself", and reflection a nesting the class itself doesn't declare
		String outer = outerName == null ? null : this.remapper.mapType(outerName);
		if (outer != null && !this.remapper.mapType(name).startsWith(outer + "$")) {
			return;
		}
		super.visitInnerClass(name, outerName, innerName, access);
	}

	// Nest and sealed attributes name the classes of the same compilation: a nest host or member, or a permitted
	// subclass, that is mapped to a 1.12.2 / Translator class (an outer class with a facade, its nested class carried
	// from the 1.21.1 API) isn't in this jar any more, and the downgrader needs it to be. Those entries go.

	@Override
	public void visitNestHost(String nestHost) {
		if (this.names.isOwn(nestHost)) {
			super.visitNestHost(nestHost);
		}
	}

	@Override
	public void visitNestMember(String nestMember) {
		if (this.names.isOwn(nestMember)) {
			super.visitNestMember(nestMember);
		}
	}

	@Override
	public void visitPermittedSubclass(String permittedSubclass) {
		if (this.names.isOwn(permittedSubclass)) {
			super.visitPermittedSubclass(permittedSubclass);
		}
	}

	@Override
	protected MethodVisitor createMethodRemapper(MethodVisitor methodVisitor) {
		return new MethodRemapper(Opcodes.ASM9, methodVisitor, this.remapper) {
			@Override
			public xyz.wagyourtail.jvmdg.shade.asm.AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				var visitor = super.visitAnnotation(descriptor, visible);
				return visitor != null && isMixinAnnotation(descriptor) ? new MixinStrings(visitor, null) : visitor;
			}

			@Override
			public void visitTypeInsn(int opcode, String type) {
				String facade = FacadeRemapper.this.supers.get(type);
				super.visitTypeInsn(opcode, opcode == Opcodes.NEW && facade != null ? facade : type);
			}

			@Override
			public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
				String facade = FacadeRemapper.this.supers.get(owner);
				if (facade != null && (opcode == Opcodes.INVOKESPECIAL || opcode == Opcodes.INVOKESTATIC
						&& FacadeRemapper.this.target.hasMember(facade, name, FacadeRemapper.this.names.mapMethodDesc(descriptor), true))) {
					owner = facade;
				}
				super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
			}

			/** A constructor reference (BlockItem::new, as Registrate makes items) makes the facade, as {@code new} does. */
			@Override
			public void visitInvokeDynamicInsn(String name, String descriptor, xyz.wagyourtail.jvmdg.shade.asm.Handle bootstrap, Object... arguments) {
				for (int i = 0; i < arguments.length; i++) {
					if (arguments[i] instanceof xyz.wagyourtail.jvmdg.shade.asm.Handle handle && handle.getTag() == Opcodes.H_NEWINVOKESPECIAL) {
						String facade = FacadeRemapper.this.supers.get(handle.getOwner());
						if (facade != null) {
							arguments[i] = new xyz.wagyourtail.jvmdg.shade.asm.Handle(handle.getTag(), facade, handle.getName(), handle.getDesc(), false);
						}
					}
				}
				super.visitInvokeDynamicInsn(name, descriptor, bootstrap, arguments);
			}

			@Override
			public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
				String facade = FacadeRemapper.this.supers.get(owner);
				if (facade != null && (opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC)
						&& FacadeRemapper.this.target.hasMember(facade, name, FacadeRemapper.this.names.mapDesc(descriptor), false)) {
					owner = facade;
				}
				super.visitFieldInsn(opcode, owner, name, descriptor);
			}
		};
	}
}
