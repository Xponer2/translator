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
	/**
	 * Accessors into 1.12.2 classes kept as mixins: their methods ("name/argument count") -> the 1.12.2 name of the
	 * member (the 1.21 name in the annotation finds nothing there: Mixin's InvalidAccessorException).
	 */
	private final Map<String, Map<String, String>> renamed = new HashMap<>();

	private AccessorMixins() {
	}

	boolean isEmpty() {
		return this.moved.isEmpty() && this.renamed.isEmpty();
	}

	/** Whether the mixin (the mod's class name) works through reflection now: it is left out of the mixin config. */
	boolean isMoved(String className) {
		return this.moved.containsKey(className);
	}

	/** The name the class is written under (moved for an accessor, else its own). */
	String name(String className) {
		return this.moved.getOrDefault(className, className);
	}

	/**
	 * The jar's accessor mixins. Into Translator classes (MethodGuard's mixable ones excepted): reflection. Into 1.12.2
	 * classes: the annotations get the 1.12.2 names when 1.12.2 has every member, else reflection too (a member 1.12.2
	 * lacks lives beside the object).
	 */
	static AccessorMixins find(ModJar jar, ZipFile zip, ModRemapper remapper, Predicate<String> mixable, TargetClasses classes) throws IOException {
		var found = new AccessorMixins();
		for (var entry : jar.classes.entrySet()) {
			ClassInfo info = entry.getValue();
			if (info.mixinTargets.isEmpty()) {
				continue;
			}
			boolean translatorOnly = true;
			boolean mixableTarget = false;
			for (String target : info.mixinTargets) {
				String mapped = remapper.map(target);
				translatorOnly &= mapped.startsWith(TRANSLATOR) && !mixable.test(mapped);
				mixableTarget |= mixable.test(mapped);
			}
			ZipEntry classEntry = mixableTarget ? null : zip.getEntry(entry.getKey() + ".class");
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
			var names121 = new HashMap<String, String>();
			boolean accessorOnly = true;
			// a 1.12.2 target: the 1.12.2 name of each member, null when 1.12.2 has none
			ClassInfo gameClass = translatorOnly || info.mixinTargets.size() != 1 ? null : classes.get(remapper.map(target));
			var gameNames = new HashMap<String, String>();
			boolean allInGame = gameClass != null;
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
				String key = method.name + "/" + type.getArgumentTypes().length;
				methods.put(key, new Member(invoker, String.join(",", names)));
				names121.put(key, name);
				if (gameClass != null) {
					String gameName = gameMember(gameClass, invoker, name, names, method, remapper);
					if (gameName == null) {
						allInGame = false;
					} else {
						gameNames.put(key, gameName);
					}
				}
			}
			if (gameClass != null && !(accessorOnly && !allInGame)) {
				// kept as a mixin (an accessor interface whose members 1.12.2 has, or a mixin with injectors too): the
				// accessor names in its annotations become the 1.12.2 ones (those already right stay)
				var changes = new HashMap<String, String>();
				gameNames.forEach((key, gameName) -> {
					if (!gameName.equals(names121.get(key))) {
						changes.put(key, gameName);
					}
				});
				if (!changes.isEmpty()) {
					found.renamed.put(entry.getKey(), changes);
					Conversion.LOGGER.info("{}: mixin {} of {}: {} accessor names of 1.12.2", jar.displayName(),
							entry.getKey().substring(entry.getKey().lastIndexOf('/') + 1), remapper.map(target), changes.size());
				}
				continue;
			}
			if (!translatorOnly && gameClass == null) {
				// a target that isn't a 1.12.2 class the converter knows (a carried 1.21 class, a mod's): left as it is
				continue;
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
	 * The name the 1.12.2 class declares the member under (the 1.21 name or the table's), null when it has none: a
	 * field of the accessor's type, a method of the invoker's descriptor, a constructor of its parameters.
	 */
	@Nullable
	private static String gameMember(ClassInfo gameClass, boolean invoker, String name, java.util.Set<String> names, MethodNode method,
			ModRemapper remapper) {
		Type type = Type.getMethodType(method.desc);
		if (invoker) {
			String desc = remapper.mapMethodDesc("<init>".equals(name)
					? Type.getMethodDescriptor(Type.VOID_TYPE, type.getArgumentTypes()) : method.desc);
			for (String candidate : names) {
				if (gameClass.methods.contains(candidate + " " + desc)) {
					return candidate;
				}
			}
			return null;
		}
		Type field = type.getArgumentTypes().length == 0 ? type.getReturnType() : type.getArgumentTypes()[0];
		String desc = remapper.mapDesc(field.getDescriptor());
		for (String candidate : names) {
			if (gameClass.fields.contains(candidate + " " + desc)) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * A class of the jar (remapped already): the accessor itself made a class of static bridges, references to an
	 * accessor moved, calls through it made static calls taking the object; an accessor kept as a mixin gets the
	 * 1.12.2 names in its annotations.
	 */
	byte[] rewrite(String className, byte[] bytes) {
		Map<String, String> changes = this.renamed.get(className);
		if (changes != null) {
			var node = new ClassNode();
			new ClassReader(bytes).accept(node, 0);
			for (MethodNode method : node.methods) {
				AnnotationNode annotation = annotation(method);
				String gameName = annotation == null ? null : changes.get(method.name + "/" + Type.getArgumentTypes(method.desc).length);
				if (gameName != null) {
					setValue(annotation, gameName);
				}
			}
			var writer = new ClassWriter(0);
			node.accept(writer);
			bytes = writer.toByteArray();
		}
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
		if (this.moved.isEmpty()) {
			return false;
		}
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

	private static void setValue(AnnotationNode annotation, String value) {
		if (annotation.values == null) {
			annotation.values = new java.util.ArrayList<>();
		}
		for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
			if ("value".equals(annotation.values.get(i))) {
				annotation.values.set(i + 1, value);
				return;
			}
		}
		annotation.values.add("value");
		annotation.values.add(value);
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
