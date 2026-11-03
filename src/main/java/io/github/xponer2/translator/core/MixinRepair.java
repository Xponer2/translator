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
package io.github.xponer2.translator.core;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.Nullable;

import xyz.wagyourtail.jvmdg.shade.asm.ClassReader;
import xyz.wagyourtail.jvmdg.shade.asm.ClassWriter;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;
import xyz.wagyourtail.jvmdg.shade.asm.Type;
import xyz.wagyourtail.jvmdg.shade.asm.tree.AnnotationNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.FieldInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.FieldNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.InsnList;
import xyz.wagyourtail.jvmdg.shade.asm.tree.InsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.TypeInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.analysis.Analyzer;
import xyz.wagyourtail.jvmdg.shade.asm.tree.analysis.AnalyzerException;
import xyz.wagyourtail.jvmdg.shade.asm.tree.analysis.BasicVerifier;

/**
 * Makes a Minecraft class that Mixin left unloadable load with the mixins in it (see {@link SafeMixins}): what a 1.21
 * mixin needs and 1.12.2 lacks gets a stand-in, so the other mixins into the class (other mods') keep working.
 * <ul>
 * <li>an {@code @Accessor} of a field 1.12.2 doesn't have gets a field of its own ({@code translator$name}): the getter
 * reads what the setter wrote;</li>
 * <li>an {@code @Invoker} or {@code @Shadow} of a method 1.12.2 doesn't have does nothing (a default result);</li>
 * <li>a handler that doesn't verify (it reads a local 1.12.2's method doesn't have) is neutral: an {@code @Inject}
 * returns, a {@code @Modify...} gives back the value it got, a {@code @WrapWithCondition} lets the call happen.</li>
 * </ul>
 * A {@code @Redirect} or {@code @WrapOperation} that doesn't verify can't be neutral (the original call is gone): then
 * the class isn't repaired.
 */
public final class MixinRepair {
	private static final String ACCESSOR = "Lorg/spongepowered/asm/mixin/gen/Accessor;";

	private MixinRepair() {
	}

	/** The repaired class, or null if there's nothing to repair or it can't be. */
	@Nullable
	static byte[] repair(byte[] before, byte[] after, String name) {
		var original = new ClassNode();
		new ClassReader(before).accept(original, ClassReader.SKIP_CODE);
		Set<String> had = new HashSet<>();
		for (MethodNode method : original.methods) {
			had.add(method.name + method.desc);
		}
		var node = new ClassNode();
		try {
			new ClassReader(after).accept(node, 0);
		} catch (RuntimeException e) {
			return null;
		}
		if ((node.access & Opcodes.ACC_INTERFACE) != 0) {
			return null;
		}
		boolean changed = false;

		// abstract methods Mixin added (a @Shadow of a method 1.12.2 doesn't have)
		for (MethodNode method : node.methods) {
			if ((method.access & Opcodes.ACC_ABSTRACT) != 0 && !had.contains(method.name + method.desc)) {
				method.access &= ~Opcodes.ACC_ABSTRACT;
				implement(node, method, accessorField(method));
				changed = true;
			}
		}

		// methods of the mixins' interfaces (accessors, invokers) the class doesn't have
		for (String itf : node.interfaces) {
			if (original.interfaces.contains(itf)) {
				continue;
			}
			var todo = new java.util.ArrayDeque<String>();
			todo.add(itf);
			var seen = new HashSet<String>();
			while (!todo.isEmpty()) {
				ClassNode type = SafeMixins.read(todo.poll(), seen);
				if (type == null) {
					continue;
				}
				todo.addAll(type.interfaces);
				for (MethodNode abstractMethod : type.methods) {
					if ((abstractMethod.access & Opcodes.ACC_ABSTRACT) == 0 || declares(node, abstractMethod.name + abstractMethod.desc)
							|| SafeMixins.unimplemented(type.name, node).stream().noneMatch(m -> m.equals(abstractMethod.name + abstractMethod.desc))) {
						continue;
					}
					var method = new MethodNode(Opcodes.ACC_PUBLIC, abstractMethod.name, abstractMethod.desc, null, null);
					implement(node, method, accessorField(abstractMethod));
					node.methods.add(method);
					changed = true;
				}
			}
		}

		// handlers that don't verify
		for (MethodNode method : node.methods) {
			if (method.instructions.size() == 0 || verifies(node.name, method)) {
				continue;
			}
			if (had.contains(method.name + method.desc) || !neutralize(method)) {
				return null;
			}
			changed = true;
		}
		if (!changed) {
			return null;
		}
		var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		node.accept(writer);
		return writer.toByteArray();
	}

	private static boolean declares(ClassNode node, String member) {
		for (MethodNode method : node.methods) {
			if ((method.name + method.desc).equals(member)) {
				return true;
			}
		}
		return false;
	}

	private static boolean verifies(String owner, MethodNode method) {
		try {
			new Analyzer<>(new BasicVerifier()).analyze(owner, method);
			return true;
		} catch (AnalyzerException | RuntimeException e) {
			return false;
		}
	}

	/** The field an @Accessor reads or writes (its value, else the method name without get/set/is), or null. */
	@Nullable
	private static String accessorField(MethodNode method) {
		var annotations = new java.util.ArrayList<AnnotationNode>();
		if (method.invisibleAnnotations != null) {
			annotations.addAll(method.invisibleAnnotations);
		}
		if (method.visibleAnnotations != null) {
			annotations.addAll(method.visibleAnnotations);
		}
		for (AnnotationNode annotation : annotations) {
			if (!ACCESSOR.equals(annotation.desc)) {
				continue;
			}
			if (annotation.values != null) {
				for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
					if ("value".equals(annotation.values.get(i)) && annotation.values.get(i + 1) instanceof String value && !value.isEmpty()) {
						return value;
					}
				}
			}
			String name = method.name.substring(method.name.lastIndexOf('$') + 1);
			for (String prefix : new String[] {"get", "set", "is"}) {
				if (name.length() > prefix.length() && name.startsWith(prefix) && Character.isUpperCase(name.charAt(prefix.length()))) {
					name = Character.toLowerCase(name.charAt(prefix.length())) + name.substring(prefix.length() + 1);
					break;
				}
			}
			return name;
		}
		return null;
	}

	/** The body of a method 1.12.2 lacks: an accessor of a field of its own, else a default result. */
	private static void implement(ClassNode owner, MethodNode method, @Nullable String field) {
		boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
		Type[] args = Type.getArgumentTypes(method.desc);
		Type result = Type.getReturnType(method.desc);
		clear(method);
		InsnList code = method.instructions;
		if (field != null && args.length == 0 && result.getSort() != Type.VOID) {
			String fieldName = field(owner, field, result, isStatic);
			if (!isStatic) {
				code.add(new VarInsnNode(Opcodes.ALOAD, 0));
			}
			code.add(new FieldInsnNode(isStatic ? Opcodes.GETSTATIC : Opcodes.GETFIELD, owner.name, fieldName, result.getDescriptor()));
			if (collection(result) != null) {
				// a collection is there when first read (the mod adds to it): this.f = orNew(this.f), straight code
				if (!isStatic) {
					code.insert(new VarInsnNode(Opcodes.ALOAD, 0));
				}
				code.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.LdcInsnNode(collection(result)));
				code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/core/MixinRepair", "orNew",
						"(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/Object;", false));
				code.add(new TypeInsnNode(Opcodes.CHECKCAST, result.getInternalName()));
				code.add(new InsnNode(isStatic ? Opcodes.DUP : Opcodes.DUP_X1));
				code.add(new FieldInsnNode(isStatic ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD, owner.name, fieldName, result.getDescriptor()));
			}
			code.add(new InsnNode(result.getOpcode(Opcodes.IRETURN)));
			return;
		}
		if (field != null && args.length == 1 && result.getSort() == Type.VOID) {
			String fieldName = field(owner, field, args[0], isStatic);
			if (!isStatic) {
				code.add(new VarInsnNode(Opcodes.ALOAD, 0));
			}
			code.add(new VarInsnNode(args[0].getOpcode(Opcodes.ILOAD), isStatic ? 0 : 1));
			code.add(new FieldInsnNode(isStatic ? Opcodes.PUTSTATIC : Opcodes.PUTFIELD, owner.name, fieldName, args[0].getDescriptor()));
			code.add(new InsnNode(Opcodes.RETURN));
			return;
		}
		returnDefault(code, result);
	}

	/** The collection class made for a collection type (List: ArrayList, ...), or null. */
	@Nullable
	private static String collection(Type type) {
		if (type.getSort() != Type.OBJECT) {
			return null;
		}
		return switch (type.getInternalName()) {
		case "java/util/List", "java/util/Collection", "java/lang/Iterable", "java/util/ArrayList" -> "java/util/ArrayList";
		case "java/util/Map", "java/util/HashMap" -> "java/util/HashMap";
		case "java/util/Set", "java/util/HashSet" -> "java/util/HashSet";
		default -> null;
		};
	}

	/** The value, or a new collection of that class when there is none yet (repaired accessor getters). */
	public static Object orNew(@Nullable Object value, String type) {
		if (value != null) {
			return value;
		}
		switch (type) {
		case "java/util/HashMap":
			return new java.util.HashMap<>();
		case "java/util/HashSet":
			return new HashSet<>();
		default:
			return new java.util.ArrayList<>();
		}
	}

	/** The class's own field of that name and type, or a new one ({@code translator$name}). */
	private static String field(ClassNode owner, String name, Type type, boolean isStatic) {
		for (FieldNode field : owner.fields) {
			if (field.name.equals(name) && field.desc.equals(type.getDescriptor()) && ((field.access & Opcodes.ACC_STATIC) != 0) == isStatic) {
				return field.name;
			}
		}
		String own = "translator$" + name;
		for (FieldNode field : owner.fields) {
			if (field.name.equals(own)) {
				return own;
			}
		}
		owner.fields.add(new FieldNode(Opcodes.ACC_PRIVATE | (isStatic ? Opcodes.ACC_STATIC : 0), own, type.getDescriptor(), null, null));
		return own;
	}

	/**
	 * A handler that does what the code would do without it: an @Inject returns, a @Modify... returns the value it
	 * was given, a @WrapWithCondition lets the call happen. False for a handler that replaced the original code.
	 */
	private static boolean neutralize(MethodNode method) {
		String kind = method.name.contains("$") ? method.name.substring(0, method.name.indexOf('$')) : "";
		if (kind.equals("redirect") || kind.equals("wrapOperation")) {
			return false;
		}
		boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
		Type[] args = Type.getArgumentTypes(method.desc);
		Type result = Type.getReturnType(method.desc);
		clear(method);
		InsnList code = method.instructions;
		if (kind.equals("wrapWithCondition") && result.getSort() == Type.BOOLEAN) {
			code.add(new InsnNode(Opcodes.ICONST_1));
			code.add(new InsnNode(Opcodes.IRETURN));
			return true;
		}
		if (result.getSort() != Type.VOID && args.length > 0 && args[0].equals(result)) {
			code.add(new VarInsnNode(result.getOpcode(Opcodes.ILOAD), isStatic ? 0 : 1));
			code.add(new InsnNode(result.getOpcode(Opcodes.IRETURN)));
			return true;
		}
		returnDefault(code, result);
		return true;
	}

	private static void clear(MethodNode method) {
		method.instructions = new InsnList();
		method.tryCatchBlocks = new java.util.ArrayList<>();
		method.localVariables = null;
		method.visibleLocalVariableAnnotations = null;
		method.invisibleLocalVariableAnnotations = null;
		method.visibleTypeAnnotations = null;
		method.invisibleTypeAnnotations = null;
	}

	/** A default result: nothing, zero, false, an empty collection or Optional, else null. */
	private static void returnDefault(InsnList code, Type result) {
		switch (result.getSort()) {
		case Type.VOID:
			code.add(new InsnNode(Opcodes.RETURN));
			return;
		case Type.BOOLEAN:
		case Type.BYTE:
		case Type.CHAR:
		case Type.SHORT:
		case Type.INT:
			code.add(new InsnNode(Opcodes.ICONST_0));
			code.add(new InsnNode(Opcodes.IRETURN));
			return;
		case Type.LONG:
			code.add(new InsnNode(Opcodes.LCONST_0));
			code.add(new InsnNode(Opcodes.LRETURN));
			return;
		case Type.FLOAT:
			code.add(new InsnNode(Opcodes.FCONST_0));
			code.add(new InsnNode(Opcodes.FRETURN));
			return;
		case Type.DOUBLE:
			code.add(new InsnNode(Opcodes.DCONST_0));
			code.add(new InsnNode(Opcodes.DRETURN));
			return;
		default:
			break;
		}
		String type = result.getSort() == Type.OBJECT ? result.getInternalName() : "";
		String created = switch (type) {
		case "java/util/List", "java/util/Collection", "java/lang/Iterable", "java/util/ArrayList" -> "java/util/ArrayList";
		case "java/util/Map", "java/util/HashMap" -> "java/util/HashMap";
		case "java/util/Set", "java/util/HashSet" -> "java/util/HashSet";
		default -> null;
		};
		if (created != null) {
			code.add(new TypeInsnNode(Opcodes.NEW, created));
			code.add(new InsnNode(Opcodes.DUP));
			code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, created, "<init>", "()V", false));
		} else if ("java/util/Optional".equals(type)) {
			code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Optional", "empty", "()Ljava/util/Optional;", false));
		} else {
			code.add(new InsnNode(Opcodes.ACONST_NULL));
		}
		code.add(new InsnNode(Opcodes.ARETURN));
	}
}
