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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Nullable;

import xyz.wagyourtail.jvmdg.shade.asm.ClassReader;
import xyz.wagyourtail.jvmdg.shade.asm.ClassWriter;
import xyz.wagyourtail.jvmdg.shade.asm.Handle;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;
import xyz.wagyourtail.jvmdg.shade.asm.Type;
import xyz.wagyourtail.jvmdg.shade.asm.commons.ClassRemapper;
import xyz.wagyourtail.jvmdg.shade.asm.commons.SimpleRemapper;
import xyz.wagyourtail.jvmdg.shade.asm.tree.AbstractInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.AnnotationNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.InsnList;
import xyz.wagyourtail.jvmdg.shade.asm.tree.InsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.InvokeDynamicInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.LdcInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.TypeInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode;

/**
 * A mod's accessor mixins (only @Accessor / @Invoker methods) into a class that is a Translator class now: Mixin can't
 * apply them (the Translator class is built differently, the field may not be there), and Mixin refuses to load a
 * class of a mixin package that isn't applied, so the mod failed where it used the accessor (Rechiseled's blocks
 * through SuperMartijn642's BlockPropertiesAccessor, Create's accessors). Here the accessor moves out of the mixin
 * package ({@value #PREFIX}), its methods become static ones that reach the member by reflection ({@link AccessorBridge}),
 * and every call {@code ((Accessor) object).getX()} of the jar becomes {@code Accessor.getX(object)}.
 */
final class AccessorMixins {
	static final String PREFIX = "translator_accessor/";
	private static final String ACCESSOR = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
	private static final String INVOKER = "Lorg/spongepowered/asm/mixin/gen/Invoker;";
	private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
	private static final String BRIDGE = "io/github/xponer2/translator/runtime/AccessorBridge";
	private static final String TRANSLATOR = "io/github/xponer2/translator/";

	/** What one accessor method reaches: a field (getter or setter) or a method, by its possible names. */
	private static final class Member {
		final boolean invoker;
		final String names;

		Member(boolean invoker, String names) {
			this.invoker = invoker;
			this.names = names;
		}
	}

	/** Accessor (the mod's name) -> the Translator class it reaches. */
	private final Map<String, String> targets = new HashMap<>();
	/** Accessor (moved name) -> its methods ("name/argument count"). */
	private final Map<String, Map<String, Member>> members = new HashMap<>();
	/** The mod's name -> the moved name. */
	private final Map<String, String> moved = new HashMap<>();

	private AccessorMixins() {
	}

	boolean isEmpty() {
		return this.moved.isEmpty();
	}

	/** The name the class is written under (moved for an accessor, else its own). */
	String name(String className) {
		return this.moved.getOrDefault(className, className);
	}

	/** The jar's accessor mixins whose targets are all Translator classes (MethodGuard's mixable ones excepted). */
	static AccessorMixins find(ModJar jar, ZipFile zip, ModRemapper remapper, Predicate<String> mixable) throws IOException {
		var found = new AccessorMixins();
		for (var entry : jar.classes.entrySet()) {
			ClassInfo info = entry.getValue();
			if (info.mixinTargets.isEmpty()) {
				continue;
			}
			boolean translatorOnly = true;
			for (String target : info.mixinTargets) {
				String mapped = remapper.map(target);
				translatorOnly &= mapped.startsWith(TRANSLATOR) && !mixable.test(mapped);
			}
			ZipEntry classEntry = translatorOnly ? zip.getEntry(entry.getKey() + ".class") : null;
			if (classEntry == null) {
				continue;
			}
			byte[] bytes;
			try (InputStream in = zip.getInputStream(classEntry)) {
				bytes = ModJar.readAll(in);
			}
			var node = new ClassNode();
			new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
			if ((node.access & Opcodes.ACC_INTERFACE) == 0) {
				continue;
			}
			String target = info.mixinTargets.get(0);
			var methods = new HashMap<String, Member>();
			boolean accessorOnly = true;
			for (MethodNode method : node.methods) {
				AnnotationNode annotation = annotation(method);
				if (annotation == null) {
					// a static helper of the interface is fine; anything else isn't an accessor
					accessorOnly &= (method.access & Opcodes.ACC_STATIC) != 0 && (method.access & Opcodes.ACC_ABSTRACT) == 0;
					continue;
				}
				boolean invoker = INVOKER.equals(annotation.desc);
				String value = value(annotation);
				String name = value != null && !value.isEmpty() ? value : derived(method.name, invoker);
				var names = new LinkedHashSet<String>();
				names.add(name);
				Type type = Type.getMethodType(method.desc);
				try {
					if (invoker && !"<init>".equals(name)) {
						names.add(remapper.mapMethodName(target, name, method.desc));
					} else if (!invoker) {
						Type field = type.getArgumentTypes().length == 0 ? type.getReturnType() : type.getArgumentTypes()[0];
						names.add(remapper.mapFieldName(target, name, field.getDescriptor()));
					}
				} catch (RuntimeException e) {
					// the 1.21 name only
				}
				methods.put(method.name + "/" + type.getArgumentTypes().length, new Member(invoker, String.join(",", names)));
			}
			if (accessorOnly && !methods.isEmpty()) {
				String movedName = PREFIX + entry.getKey();
				found.targets.put(entry.getKey(), remapper.map(target));
				found.members.put(movedName, methods);
				found.moved.put(entry.getKey(), movedName);
				Conversion.LOGGER.info("{}: accessor mixin {} of {} works through reflection", jar.displayName(),
						entry.getKey().substring(entry.getKey().lastIndexOf('/') + 1), remapper.map(target));
			}
		}
		return found;
	}

	/**
	 * A class of the jar (remapped already): the accessor itself made a class of static bridges, references to an
	 * accessor moved, calls through it made static calls taking the object.
	 */
	byte[] rewrite(String className, byte[] bytes) {
		if (!this.moved.containsKey(className) && !this.mentions(bytes)) {
			return bytes;
		}
		var node = new ClassNode();
		new ClassReader(bytes).accept(new ClassRemapper(node, new SimpleRemapper(this.moved)), 0);
		String target = this.targets.get(className);
		if (target != null) {
			this.makeBridges(node, target);
		}
		for (MethodNode method : node.methods) {
			this.rewriteCalls(method);
		}
		var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		node.accept(writer);
		return writer.toByteArray();
	}

	private boolean mentions(byte[] bytes) {
		String text = new String(bytes, StandardCharsets.ISO_8859_1);
		for (String name : this.moved.keySet()) {
			if (text.contains(name)) {
				return true;
			}
		}
		return false;
	}

	@Nullable
	private Member member(String owner, String name, String desc) {
		Map<String, Member> methods = this.members.get(owner);
		return methods == null ? null : methods.get(name + "/" + Type.getArgumentTypes(desc).length);
	}

	private void rewriteCalls(MethodNode method) {
		for (AbstractInsnNode insn : method.instructions.toArray()) {
			if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKEINTERFACE && this.member(call.owner, call.name, call.desc) != null) {
				call.setOpcode(Opcodes.INVOKESTATIC);
				call.desc = "(Ljava/lang/Object;" + call.desc.substring(1);
				call.itf = true;
			} else if (insn instanceof TypeInsnNode type && this.members.containsKey(type.desc)) {
				if (type.getOpcode() == Opcodes.CHECKCAST) {
					// the object doesn't implement the accessor now: no cast (the verifier takes any object for an interface)
					method.instructions.remove(type);
				} else if (type.getOpcode() == Opcodes.INSTANCEOF) {
					method.instructions.insertBefore(type, new InsnNode(Opcodes.POP));
					method.instructions.set(type, new InsnNode(Opcodes.ICONST_1));
				}
			} else if (insn instanceof InvokeDynamicInsnNode indy) {
				this.rewriteReference(indy);
			}
		}
	}

	/** A method reference to an accessor method ({@code Accessor::getX}): to its static bridge. */
	private void rewriteReference(InvokeDynamicInsnNode indy) {
		boolean changed = false;
		for (int i = 0; i < indy.bsmArgs.length; i++) {
			if (indy.bsmArgs[i] instanceof Handle handle && handle.getTag() == Opcodes.H_INVOKEINTERFACE
					&& this.member(handle.getOwner(), handle.getName(), handle.getDesc()) != null) {
				indy.bsmArgs[i] = new Handle(Opcodes.H_INVOKESTATIC, handle.getOwner(), handle.getName(),
						"(Ljava/lang/Object;" + handle.getDesc().substring(1), true);
				changed = true;
			}
		}
		if (!changed) {
			return;
		}
		// the lambda casts its receiver to the types named here: the object isn't an accessor any more
		for (int i = 0; i < indy.bsmArgs.length; i++) {
			if (indy.bsmArgs[i] instanceof Type type && type.getSort() == Type.METHOD) {
				indy.bsmArgs[i] = Type.getMethodType(this.erase(type.getDescriptor()));
			}
		}
		indy.desc = this.erase(indy.desc);
	}

	private String erase(String desc) {
		for (String accessor : this.members.keySet()) {
			desc = desc.replace("L" + accessor + ";", "Ljava/lang/Object;");
		}
		return desc;
	}

	/** The accessor's annotated methods: static ones reaching the member of the object (first argument) by reflection. */
	private void makeBridges(ClassNode node, String target) {
		if (node.invisibleAnnotations != null) {
			node.invisibleAnnotations.removeIf(a -> MIXIN.equals(a.desc));
		}
		if (node.visibleAnnotations != null) {
			node.visibleAnnotations.removeIf(a -> MIXIN.equals(a.desc));
		}
		for (MethodNode method : node.methods) {
			AnnotationNode annotation = annotation(method);
			if (annotation == null) {
				continue;
			}
			Member member = this.member(node.name, method.name, method.desc);
			if (member == null) {
				continue;
			}
			boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
			Type type = Type.getMethodType(method.desc);
			Type[] params = type.getArgumentTypes();
			Type result = type.getReturnType();
			var code = new InsnList();
			code.add(isStatic ? new InsnNode(Opcodes.ACONST_NULL) : new VarInsnNode(Opcodes.ALOAD, 0));
			code.add(new LdcInsnNode(target));
			int local = isStatic ? 0 : 1;
			if (member.invoker) {
				code.add(new LdcInsnNode("<init>".equals(member.names.split(",")[0]) ? "<init>" : member.names));
				code.add(new LdcInsnNode(params.length));
				code.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
				for (int i = 0; i < params.length; i++) {
					code.add(new InsnNode(Opcodes.DUP));
					code.add(new LdcInsnNode(i));
					code.add(new VarInsnNode(params[i].getOpcode(Opcodes.ILOAD), local));
					box(code, params[i]);
					code.add(new InsnNode(Opcodes.AASTORE));
					local += params[i].getSize();
				}
				code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "invoke",
						"(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;", false));
				unbox(code, result);
			} else if (params.length == 0) {
				code.add(new LdcInsnNode(member.names));
				code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "get",
						"(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", false));
				unbox(code, result);
			} else {
				code.add(new LdcInsnNode(member.names));
				code.add(new VarInsnNode(params[0].getOpcode(Opcodes.ILOAD), local));
				box(code, params[0]);
				code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, "set",
						"(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V", false));
				if (result.getSort() != Type.VOID) {
					// a setter returning something (chained): what the mod would get back is the object
					code.add(isStatic ? new InsnNode(Opcodes.ACONST_NULL) : new VarInsnNode(Opcodes.ALOAD, 0));
					unbox(code, result);
				} else {
					code.add(new InsnNode(Opcodes.RETURN));
				}
			}
			method.instructions = code;
			method.tryCatchBlocks.clear();
			method.localVariables = null;
			method.desc = isStatic ? method.desc : "(Ljava/lang/Object;" + method.desc.substring(1);
			method.signature = null;
			method.access = (method.access & ~Opcodes.ACC_ABSTRACT & ~Opcodes.ACC_PRIVATE & ~Opcodes.ACC_PROTECTED)
					| Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
			// the parameters moved by one: their annotations would be off
			method.visibleParameterAnnotations = null;
			method.invisibleParameterAnnotations = null;
			method.visibleAnnotableParameterCount = 0;
			method.invisibleAnnotableParameterCount = 0;
			method.parameters = null;
		}
	}

	private static void box(InsnList code, Type type) {
		String owner = switch (type.getSort()) {
			case Type.BOOLEAN -> "java/lang/Boolean";
			case Type.CHAR -> "java/lang/Character";
			case Type.BYTE -> "java/lang/Byte";
			case Type.SHORT -> "java/lang/Short";
			case Type.INT -> "java/lang/Integer";
			case Type.LONG -> "java/lang/Long";
			case Type.FLOAT -> "java/lang/Float";
			case Type.DOUBLE -> "java/lang/Double";
			default -> null;
		};
		if (owner != null) {
			code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, owner, "valueOf", "(" + type.getDescriptor() + ")L" + owner + ";", false));
		}
	}

	/** The Object on the stack as the method's result, returned. */
	private static void unbox(InsnList code, Type result) {
		String helper = switch (result.getSort()) {
			case Type.BOOLEAN -> "booleanValue";
			case Type.CHAR -> "charValue";
			case Type.BYTE -> "byteValue";
			case Type.SHORT -> "shortValue";
			case Type.INT -> "intValue";
			case Type.LONG -> "longValue";
			case Type.FLOAT -> "floatValue";
			case Type.DOUBLE -> "doubleValue";
			default -> null;
		};
		if (result.getSort() == Type.VOID) {
			code.add(new InsnNode(Opcodes.POP));
			code.add(new InsnNode(Opcodes.RETURN));
		} else if (helper != null) {
			code.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGE, helper, "(Ljava/lang/Object;)" + result.getDescriptor(), false));
			code.add(new InsnNode(result.getOpcode(Opcodes.IRETURN)));
		} else {
			code.add(new TypeInsnNode(Opcodes.CHECKCAST, result.getInternalName()));
			code.add(new InsnNode(Opcodes.ARETURN));
		}
	}

	@Nullable
	private static AnnotationNode annotation(MethodNode method) {
		for (List<AnnotationNode> list : java.util.Arrays.asList(method.visibleAnnotations, method.invisibleAnnotations)) {
			if (list != null) {
				for (AnnotationNode annotation : list) {
					if (ACCESSOR.equals(annotation.desc) || INVOKER.equals(annotation.desc)) {
						return annotation;
					}
				}
			}
		}
		return null;
	}

	@Nullable
	private static String value(AnnotationNode annotation) {
		if (annotation.values != null) {
			for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
				if ("value".equals(annotation.values.get(i)) && annotation.values.get(i + 1) instanceof String value) {
					return value;
				}
			}
		}
		return null;
	}

	/** The member's name from the method's (Mixin's rule): getX / isX / setX -> x; callX / invokeX -> x; newX -> <init>. */
	private static String derived(String method, boolean invoker) {
		String[] prefixes = invoker ? new String[] {"call", "invoke", "new", "create"} : new String[] {"get", "is", "set"};
		for (String prefix : prefixes) {
			if (method.length() > prefix.length() && method.startsWith(prefix) && Character.isUpperCase(method.charAt(prefix.length()))) {
				if (invoker && ("new".equals(prefix) || "create".equals(prefix))) {
					return "<init>";
				}
				String rest = method.substring(prefix.length());
				// an all-caps name (getMAX_SIZE) stays as it is
				return rest.equals(rest.toUpperCase(java.util.Locale.ROOT)) ? rest : Character.toLowerCase(rest.charAt(0)) + rest.substring(1);
			}
		}
		return method;
	}
}
