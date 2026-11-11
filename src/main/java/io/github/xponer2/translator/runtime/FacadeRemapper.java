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
	private static final String ITEM_STACK = "net/minecraft/world/item/ItemStack";
	private static final String DYE_ITEM = "net/minecraft/world/item/DyeItem";
	private final ModRemapper names;
	private final Map<String, String> supers;
	private final TargetClasses target;
	/** The table's I lines: 1.21 class -> helper class and method that checks an object is one. */
	private final Map<String, String[]> kinds;
	/** The table's E lines: 1.21 classes whose facade is only for the classes that extend them. */
	private final java.util.Set<String> extendsOnly;
	/** The 1.21 superclass of the class being read. */
	private String superName;

	FacadeRemapper(ClassVisitor next, ModRemapper names, Map<String, String> supers, TargetClasses target, Map<String, String[]> kinds,
			java.util.Set<String> extendsOnly) {
		super(Opcodes.ASM9, next, names);
		this.names = names;
		this.supers = supers;
		this.target = target;
		this.kinds = kinds;
		this.extendsOnly = extendsOnly;
	}

	@Override
	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
		// ClassRemapper#visit, with the facade as the superclass
		this.className = name;
		this.superName = superName;
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
			if ("<init>".equals(name) || "<clinit>".equals(name)) {
				// a constructor can't take another name ("<init>$1": ClassFormatError, the class didn't load at all: Jade's
				// TextElement of a Component and of a FormattedText, both an ITextComponent). The first one stays: a call of
				// either reaches it, with an object of the type they share now
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

			/** The local an ALOAD has just pushed (nothing else since), -1 otherwise. */
			private int loaded = -1;
			/** The local of the last item stack whose item was asked for ({@code stack.getItem()}), -1 when none. */
			private int stackLocal = -1;

			@Override
			public void visitVarInsn(int opcode, int varIndex) {
				this.loaded = opcode == Opcodes.ALOAD ? varIndex : -1;
				if (opcode == Opcodes.ASTORE && varIndex == this.stackLocal) {
					this.stackLocal = -1;
				}
				super.visitVarInsn(opcode, varIndex);
			}

			@Override
			public void visitInsn(int opcode) {
				this.loaded = -1;
				super.visitInsn(opcode);
			}

			@Override
			public void visitIntInsn(int opcode, int operand) {
				this.loaded = -1;
				super.visitIntInsn(opcode, operand);
			}

			@Override
			public void visitJumpInsn(int opcode, xyz.wagyourtail.jvmdg.shade.asm.Label label) {
				this.loaded = -1;
				super.visitJumpInsn(opcode, label);
			}

			@Override
			public void visitLdcInsn(Object value) {
				this.loaded = -1;
				super.visitLdcInsn(value);
			}

			@Override
			public void visitIincInsn(int varIndex, int increment) {
				this.loaded = -1;
				super.visitIincInsn(varIndex, increment);
			}

			@Override
			public void visitTypeInsn(int opcode, String type) {
				this.loaded = -1;
				String[] kind = opcode == Opcodes.INSTANCEOF ? FacadeRemapper.this.kinds.get(type) : null;
				if (kind != null) {
					// one 1.12.2 class for several 1.21 ones: the helper tells which (the cast after it is the 1.12.2 class)
					super.visitMethodInsn(Opcodes.INVOKESTATIC, kind[0], kind[1], "(Ljava/lang/Object;)Z", false);
					return;
				}
				String facade = FacadeRemapper.this.supers.get(type);
				if (opcode == Opcodes.NEW && facade != null && FacadeRemapper.this.extendsOnly.contains(type)) {
					// made as the 1.12.2 class (its constructor helpers); its <init> below isn't the superclass's
					facade = null;
					this.pendingNew++;
				}
				super.visitTypeInsn(opcode, opcode == Opcodes.NEW && facade != null ? facade : type);
			}

			/** {@code new} of a class with an extends-only facade not yet closed by its constructor call. */
			private int pendingNew;

			@Override
			public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
				int receiver = this.loaded;
				this.loaded = -1;
				if (ITEM_STACK.equals(owner) && "getItem".equals(name) && descriptor.startsWith("()") && receiver >= 0) {
					this.stackLocal = receiver;
				}
				if (DYE_ITEM.equals(owner) && "getDyeColor".equals(name) && opcode == Opcodes.INVOKEVIRTUAL && this.stackLocal >= 0) {
					// 1.12.2 has one dye item for the 16 colours (the stack's damage): the colour of the stack whose item this is
					// (stack.getItem() instanceof DyeItem dye -> dye.getDyeColor()); the item alone can't tell
					super.visitInsn(Opcodes.POP);
					super.visitVarInsn(Opcodes.ALOAD, this.stackLocal);
					super.visitMethodInsn(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/runtime/redirect/LevelGapBridges", "dyeColor",
							"(L" + ITEM_STACK + ";)Lnet/minecraft/world/item/DyeColor;", false);
					return;
				}
				String facade = FacadeRemapper.this.supers.get(owner);
				if (facade != null && opcode == Opcodes.INVOKESPECIAL && FacadeRemapper.this.extendsOnly.contains(owner)) {
					// only the super calls of a class that extends it (not the constructor of a new one, not another class's)
					if ("<init>".equals(name) && this.pendingNew > 0) {
						this.pendingNew--;
						facade = null;
					} else if (!owner.equals(FacadeRemapper.this.superName)) {
						facade = null;
					}
				}
				if (facade != null && (opcode == Opcodes.INVOKESPECIAL || opcode == Opcodes.INVOKESTATIC
						&& FacadeRemapper.this.target.hasMember(facade, name, FacadeRemapper.this.names.mapMethodDesc(descriptor), true))) {
					owner = facade;
				}
				super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
			}

			/** A constructor reference (BlockItem::new, as Registrate makes items) makes the facade, as {@code new} does. */
			@Override
			public void visitInvokeDynamicInsn(String name, String descriptor, xyz.wagyourtail.jvmdg.shade.asm.Handle bootstrap, Object... arguments) {
				this.loaded = -1;
				for (int i = 0; i < arguments.length; i++) {
					if (arguments[i] instanceof xyz.wagyourtail.jvmdg.shade.asm.Handle handle && handle.getTag() == Opcodes.H_NEWINVOKESPECIAL) {
						String facade = FacadeRemapper.this.supers.get(handle.getOwner());
						if (facade != null && !FacadeRemapper.this.extendsOnly.contains(handle.getOwner())) {
							arguments[i] = new xyz.wagyourtail.jvmdg.shade.asm.Handle(handle.getTag(), facade, handle.getName(), handle.getDesc(), false);
						}
					}
				}
				super.visitInvokeDynamicInsn(name, descriptor, bootstrap, arguments);
			}

			@Override
			public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
				this.loaded = -1;
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
