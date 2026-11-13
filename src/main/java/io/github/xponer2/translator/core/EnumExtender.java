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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.launchwrapper.IClassTransformer;

import xyz.wagyourtail.jvmdg.shade.asm.ClassReader;
import xyz.wagyourtail.jvmdg.shade.asm.ClassWriter;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;
import xyz.wagyourtail.jvmdg.shade.asm.Type;
import xyz.wagyourtail.jvmdg.shade.asm.tree.AbstractInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.FieldInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.FieldNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.InsnList;
import xyz.wagyourtail.jvmdg.shade.asm.tree.InsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.LdcInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.TypeInsnNode;

/**
 * NeoForge's enum extension (a mod's {@code enumextensions.json}: Twilight Forest's DamageEffects.TWILIGHTFOREST_PINCH,
 * Farmer's Delight's recipe book categories) as a class transformer: when the enum loads, the new constants are made at
 * the end of its static initializer and added to its values. The arguments are constants, static fields, an EnumProxy
 * or a static method (index, type) of the mod, as in NeoForge. An extension whose constructor the enum doesn't have
 * (an enum that is a 1.12.2 enum now) is left out.
 */
public final class EnumExtender implements IClassTransformer {
	private static final Logger LOGGER = LogManager.getLogger("Translator Runtime");
	private static final String PROXY = "net/neoforged/fml/common/asm/enumextension/EnumProxy";
	private static final Map<String, List<Extension>> EXTENSIONS = new ConcurrentHashMap<>();

	/** The class argument of a constant made with a 1.12.2 constructor: nothing is one. */
	public interface NoClass {
	}

	/** One new constant: enum (internal name), name, constructor parameters, and where its arguments come from. */
	public static final class Extension {
		final String enumName;
		final String name;
		final String parameters;
		/** constants (Number, String, Boolean, null) or {class, field} references, when the arguments are listed */
		final List<Object> constants;
		/** {class, field} of an EnumProxy, or {class, method} of a static (int, Class) -> Object method */
		final String[] source;
		final boolean proxy;
		/** made with a 1.12.2 constructor: the mod's own extension its arguments come from */
		final Extension original;
		/** per 1.12.2 argument: the mod's argument of that type, ORDINAL, or -1 for none */
		final int[] from;

		public Extension(String enumName, String name, String parameters, List<Object> constants, String[] source, boolean proxy) {
			this(enumName, name, parameters, constants, source, proxy, null, null);
		}

		Extension(String enumName, String name, String parameters, List<Object> constants, String[] source, boolean proxy, Extension original,
				int[] from) {
			this.enumName = enumName;
			this.name = name;
			this.parameters = parameters;
			this.constants = constants;
			this.source = source;
			this.proxy = proxy;
			this.original = original;
			this.from = from;
		}
	}

	/** A 1.12.2 constructor's int the mod gives none for: the constant's ordinal (1.12.2's boat types are numbered so). */
	private static final int ORDINAL = -2;

	public static void register(Extension extension) {
		EXTENSIONS.computeIfAbsent(extension.enumName.replace('/', '.'), k -> new ArrayList<>()).add(extension);
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		List<Extension> extensions = basicClass == null ? null : EXTENSIONS.get(transformedName);
		if (extensions == null) {
			return basicClass;
		}
		try {
			return extend(basicClass, extensions);
		} catch (RuntimeException e) {
			LOGGER.error("Can't add the enum constants of the 1.21.1 mods to {}: {}", transformedName, e.toString());
			return basicClass;
		}
	}

	private static byte[] extend(byte[] bytes, List<Extension> extensions) {
		var node = new ClassNode();
		new ClassReader(bytes).accept(node, 0);
		String self = node.name;
		String arrayDesc = "[L" + self + ";";
		int count = 0;
		FieldNode values = null;
		for (FieldNode field : node.fields) {
			if ((field.access & Opcodes.ACC_ENUM) != 0) {
				count++;
			} else if ((field.access & Opcodes.ACC_STATIC) != 0 && arrayDesc.equals(field.desc) && (field.access & Opcodes.ACC_SYNTHETIC) != 0) {
				values = field;
			}
		}
		MethodNode clinit = null;
		for (MethodNode method : node.methods) {
			if ("<clinit>".equals(method.name)) {
				clinit = method;
			}
		}
		if (values == null || clinit == null) {
			LOGGER.warn("{} isn't an enum the 1.21.1 mods' constants can be added to", self);
			return bytes;
		}
		// the constants the enum can take, then their code before the returns of the static initializer (not the one of
		// the handler of a guarded initializer: that one ends a failed initialization)
		var added = new ArrayList<Extension>();
		for (Extension extension : extensions) {
			String constructor = "(Ljava/lang/String;I" + extension.parameters.substring(1);
			boolean exists = false;
			MethodNode fewest = null;
			for (MethodNode method : node.methods) {
				if ("<init>".equals(method.name)) {
					exists |= constructor.equals(method.desc);
					if (fewest == null || Type.getArgumentTypes(method.desc).length < Type.getArgumentTypes(fewest.desc).length) {
						fewest = method;
					}
				}
			}
			if (!exists) {
				if (fewest == null || !fewest.desc.startsWith("(Ljava/lang/String;I")) {
					LOGGER.warn("Enum constant {}.{} left out: {} has no constructor {} on 1.12.2", self, extension.name, self, extension.parameters);
					continue;
				}
				// a 1.12.2 enum now (ItemDisplayContext is TransformType): its own constructor, the rest left empty;
				// the constant exists, the 1.21 data of it is lost
				// the constructor's arguments are the mod's arguments of the same types, in order (Twilight Forest's boat
				// types: their name; it was null, and building the vanilla model layers failed on the boats for every mod)
				String older = "(" + fewest.desc.substring("(Ljava/lang/String;I".length());
				Type[] olderTypes = Type.getArgumentTypes(older);
				Type[] newerTypes = Type.getArgumentTypes(extension.parameters);
				int[] from = new int[olderTypes.length];
				boolean[] used = new boolean[newerTypes.length];
				boolean ordinal = false;
				for (int i = 0; i < olderTypes.length; i++) {
					from[i] = -1;
					for (int j = 0; j < newerTypes.length; j++) {
						if (!used[j] && newerTypes[j].equals(olderTypes[i])) {
							from[i] = j;
							used[j] = true;
							break;
						}
					}
					if (from[i] == -1 && olderTypes[i].getSort() == Type.INT && !ordinal) {
						from[i] = ORDINAL;
						ordinal = true;
					}
				}
				LOGGER.warn("Enum constant {}.{} made with the 1.12.2 constructor {} (from the 1.21 arguments of the same types)", self, extension.name,
						fewest.desc);
				extension = new Extension(extension.enumName, extension.name, older, new ArrayList<>(), null, false, extension, from);
			}
			node.fields.add(new FieldNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL | Opcodes.ACC_ENUM, extension.name, "L" + self + ";", null, null));
			added.add(extension);
		}
		if (added.isEmpty()) {
			return bytes;
		}
		var returns = new ArrayList<AbstractInsnNode>();
		for (AbstractInsnNode insn : clinit.instructions) {
			AbstractInsnNode previous = insn.getPrevious();
			if (insn.getOpcode() == Opcodes.RETURN && !(previous instanceof MethodInsnNode call && "failed".equals(call.name)
					&& call.owner.endsWith("/RuntimeStubs"))) {
				returns.add(insn);
			}
		}
		for (AbstractInsnNode ret : returns) {
			clinit.instructions.insertBefore(ret, constants(self, values.name, count, added));
		}
		var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		node.accept(writer);
		var names = new ArrayList<String>();
		for (Extension extension : added) {
			names.add(extension.name);
		}
		LOGGER.info("Enum constants of 1.21.1 mods added to {}: {}", self, names);
		return writer.toByteArray();
	}

	/** Makes the new constants and the new $VALUES. */
	private static InsnList constants(String self, String values, int count, List<Extension> added) {
		String arrayDesc = "[L" + self + ";";
		var code = new InsnList();
		for (int k = 0; k < added.size(); k++) {
			Extension extension = added.get(k);
			String constructor = "(Ljava/lang/String;I" + extension.parameters.substring(1);
			code.add(new TypeInsnNode(Opcodes.NEW, self));
			code.add(new InsnNode(Opcodes.DUP));
			code.add(new LdcInsnNode(extension.name));
			code.add(new LdcInsnNode(count + k));
			Type[] types = Type.getArgumentTypes(extension.parameters);
			for (int i = 0; i < types.length; i++) {
				if (extension.from != null && extension.from[i] >= 0) {
					code.add(argument(extension.original, extension.from[i], types[i]));
				} else if (extension.from != null && extension.from[i] == ORDINAL) {
					code.add(new LdcInsnNode(count + k));
				} else {
					code.add(argument(extension, i, types[i]));
				}
			}
			code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, self, "<init>", constructor, false));
			code.add(new FieldInsnNode(Opcodes.PUTSTATIC, self, extension.name, "L" + self + ";"));
			if (extension.proxy) {
				code.add(new FieldInsnNode(Opcodes.GETSTATIC, extension.source[0], extension.source[1], "L" + PROXY + ";"));
				code.add(new FieldInsnNode(Opcodes.GETSTATIC, self, extension.name, "L" + self + ";"));
				code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, PROXY, "setValue", "(Ljava/lang/Enum;)V", false));
			}
		}
		// $VALUES with the new constants: values(), valueOf and switches see them
		code.add(new FieldInsnNode(Opcodes.GETSTATIC, self, values, arrayDesc));
		code.add(new LdcInsnNode(count + added.size()));
		code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/util/Arrays", "copyOf", "([Ljava/lang/Object;I)[Ljava/lang/Object;", false));
		code.add(new TypeInsnNode(Opcodes.CHECKCAST, arrayDesc));
		for (int i = 0; i < added.size(); i++) {
			code.add(new InsnNode(Opcodes.DUP));
			code.add(new LdcInsnNode(count + i));
			code.add(new FieldInsnNode(Opcodes.GETSTATIC, self, added.get(i).name, "L" + self + ";"));
			code.add(new InsnNode(Opcodes.AASTORE));
		}
		code.add(new FieldInsnNode(Opcodes.PUTSTATIC, self, values, arrayDesc));
		return code;
	}

	/** The i-th constructor argument, as the type the constructor takes. */
	private static InsnList argument(Extension extension, int i, Type type) {
		var code = new InsnList();
		if (extension.constants != null) {
			Object constant = i < extension.constants.size() ? extension.constants.get(i) : null;
			if (constant instanceof String[] reference) {
				code.add(new FieldInsnNode(Opcodes.GETSTATIC, reference[0], reference[1], type.getDescriptor()));
			} else if (constant == null && "Ljava/lang/Class;".equals(type.getDescriptor())) {
				// a class no object is an instance of, not null: Aether's mob categories, made with EnumCreatureType's
				// 1.12.2 constructor, crashed the mob spawner every tick (type.getCreatureClass().isAssignableFrom)
				code.add(new LdcInsnNode(Type.getObjectType("io/github/xponer2/translator/core/EnumExtender$NoClass")));
			} else if (constant == null) {
				code.add(switch (type.getSort()) {
					case Type.ARRAY, Type.OBJECT -> new InsnNode(Opcodes.ACONST_NULL);
					case Type.LONG -> new LdcInsnNode(0L);
					case Type.FLOAT -> new LdcInsnNode(0f);
					case Type.DOUBLE -> new LdcInsnNode(0d);
					default -> new LdcInsnNode(0);
				});
			} else if (constant instanceof Number number) {
				code.add(new LdcInsnNode(switch (type.getSort()) {
					case Type.LONG -> (Object) number.longValue();
					case Type.FLOAT -> number.floatValue();
					case Type.DOUBLE -> number.doubleValue();
					default -> number.intValue();
				}));
			} else if (constant instanceof Boolean flag) {
				code.add(new InsnNode(flag ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
			} else if (type.getSort() == Type.CHAR) {
				code.add(new LdcInsnNode((int) constant.toString().charAt(0)));
			} else {
				code.add(new LdcInsnNode(constant.toString()));
			}
			return code;
		}
		if (extension.proxy) {
			code.add(new FieldInsnNode(Opcodes.GETSTATIC, extension.source[0], extension.source[1], "L" + PROXY + ";"));
			code.add(new LdcInsnNode(i));
			code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, PROXY, "getParameter", "(I)Ljava/lang/Object;", false));
		} else {
			code.add(new LdcInsnNode(i));
			code.add(classLiteral(type));
			code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, extension.source[0], extension.source[1], "(ILjava/lang/Class;)Ljava/lang/Object;", false));
		}
		code.add(unbox(type));
		return code;
	}

	private static AbstractInsnNode classLiteral(Type type) {
		String box = box(type);
		return box == null ? new LdcInsnNode(type) : new FieldInsnNode(Opcodes.GETSTATIC, box, "TYPE", "Ljava/lang/Class;");
	}

	private static String box(Type type) {
		return switch (type.getSort()) {
			case Type.BOOLEAN -> "java/lang/Boolean";
			case Type.BYTE -> "java/lang/Byte";
			case Type.CHAR -> "java/lang/Character";
			case Type.SHORT -> "java/lang/Short";
			case Type.INT -> "java/lang/Integer";
			case Type.LONG -> "java/lang/Long";
			case Type.FLOAT -> "java/lang/Float";
			case Type.DOUBLE -> "java/lang/Double";
			default -> null;
		};
	}

	/** An Object on the stack as the type: a cast, or the value of a boxed primitive. */
	private static InsnList unbox(Type type) {
		var code = new InsnList();
		String box = box(type);
		if (box == null) {
			code.add(new TypeInsnNode(Opcodes.CHECKCAST, type.getSort() == Type.ARRAY ? type.getDescriptor() : type.getInternalName()));
		} else if (type.getSort() == Type.BOOLEAN || type.getSort() == Type.CHAR) {
			code.add(new TypeInsnNode(Opcodes.CHECKCAST, box));
			code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, box, type.getClassName() + "Value", "()" + type.getDescriptor(), false));
		} else {
			code.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Number"));
			code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Number", type.getClassName() + "Value", "()" + type.getDescriptor(), false));
		}
		return code;
	}
}
