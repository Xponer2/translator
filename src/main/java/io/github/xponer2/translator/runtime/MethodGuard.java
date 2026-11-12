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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import xyz.wagyourtail.jvmdg.shade.asm.ClassReader;
import xyz.wagyourtail.jvmdg.shade.asm.ClassWriter;
import xyz.wagyourtail.jvmdg.shade.asm.Handle;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;
import xyz.wagyourtail.jvmdg.shade.asm.Type;
import xyz.wagyourtail.jvmdg.shade.asm.tree.AbstractInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.FieldInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.FieldNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.FrameNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.InsnList;
import xyz.wagyourtail.jvmdg.shade.asm.tree.InsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.InvokeDynamicInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.LabelNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.LdcInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.TypeInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.analysis.Analyzer;
import xyz.wagyourtail.jvmdg.shade.asm.tree.analysis.AnalyzerException;
import xyz.wagyourtail.jvmdg.shade.asm.tree.analysis.BasicValue;
import xyz.wagyourtail.jvmdg.shade.asm.tree.analysis.SimpleVerifier;

/**
 * Last step of the conversion: every method of the converted mod is checked against what 1.12.2 and the Translator
 * have (each class, method and field it uses exists; its bytecode verifies with the new types). A method that fails
 * gets a body that throws {@link UnsupportedOperationException} with the reason, so one method the Translator can't
 * run yet doesn't keep the whole class (and the mod) from loading. The reasons are the work list.
 */
final class MethodGuard {
	private final TargetClasses target;
	private final Map<String, List<String>> redirects;
	private final Map<String, ClassInfo> mod = new HashMap<>();
	final List<String> stubbed = Collections.synchronizedList(new ArrayList<>());
	final java.util.concurrent.atomic.AtomicInteger redirected = new java.util.concurrent.atomic.AtomicInteger();
	final java.util.concurrent.atomic.AtomicInteger bridged = new java.util.concurrent.atomic.AtomicInteger();
	/** Methods that run without their missing parts instead of being stubbed whole. */
	final java.util.concurrent.atomic.AtomicInteger softened = new java.util.concurrent.atomic.AtomicInteger();
	/** "owner.name:desc" -> the int constant reads of that field give (K lines, see RemapTable#constants). */
	Map<String, Object> constants = java.util.Collections.emptyMap();
	/** X lines of the table ("owner.name desc" -> helper): calls redirected although the member exists. */
	Map<String, String> forced = java.util.Collections.emptyMap();
	/** The 1.12.2 class of an S row -> its facade (Screen's GuiScreen -> the Translator's Screen). */
	Map<String, String> facades = java.util.Collections.emptyMap();

	/** The 1.21 names of a 1.12.2 / Translator class (the classes the tables map to it): where its shadows are. */
	private final java.util.function.Function<String, List<String>> originals;

	MethodGuard(TargetClasses target, Map<String, List<String>> redirects, java.util.function.Function<String, List<String>> originals) {
		this.target = target;
		this.redirects = redirects;
		this.originals = originals;
	}

	/**
	 * The shadow (ApiCarry.shadow) of a 1.21.1 class that stands for the owner and has the static member, or null:
	 * the 1.21.1 code of a static method or field the 1.12.2 / Translator class lacks.
	 */
	private String shadowWith(String owner, String name, String desc, boolean method) {
		for (String original : this.originals.apply(owner)) {
			ClassInfo info = this.info(ApiCarry.SHADOW + original);
			if (info != null && (method ? info.methods : info.fields).contains(name + " " + desc)) {
				return ApiCarry.SHADOW + original;
			}
		}
		return null;
	}

	/**
	 * A static initializer that throws takes its class down for good: NoClassDefFoundError at every later use
	 * (ExtraCodecs, which every codec uses, stopped at a record codec of an authlib class). Guarded, the error is
	 * logged once (RuntimeStubs.failed) and the class initializes with the fields it didn't get to null.
	 */
	private static void guardInitializer(String owner, MethodNode clinit, boolean frames) {
		var start = new LabelNode();
		var end = new LabelNode();
		var handler = new LabelNode();
		clinit.instructions.insert(start);
		clinit.instructions.add(end);
		clinit.instructions.add(handler);
		if (frames) {
			// (class files before Java 6 have no stack map frames: JOML is Java 1.4)
			clinit.instructions.add(new FrameNode(Opcodes.F_FULL, 0, new Object[0], 1, new Object[] { "java/lang/Throwable" }));
		}
		clinit.instructions.add(new LdcInsnNode(owner.replace('/', '.') + ".<clinit>"));
		clinit.instructions.add(new InsnNode(Opcodes.SWAP));
		clinit.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/runtime/RuntimeStubs", "failed",
				"(Ljava/lang/String;Ljava/lang/Throwable;)V", false));
		clinit.instructions.add(new InsnNode(Opcodes.RETURN));
		clinit.tryCatchBlocks.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.TryCatchBlockNode(start, end, handler, "java/lang/Throwable"));
	}

	/**
	 * NeoForge calls private @SubscribeEvent methods; Forge 1.12.2 refuses to load a class with one
	 * (EventSubscriberTransformer: "Cannot apply @SubscribeEvent to private method"). They are made public.
	 */
	private static boolean publicSubscribers(ClassNode node) {
		boolean changed = false;
		for (MethodNode method : node.methods) {
			for (var annotations : java.util.Arrays.asList(method.visibleAnnotations, method.invisibleAnnotations)) {
				for (var annotation : annotations == null ? java.util.Collections.<xyz.wagyourtail.jvmdg.shade.asm.tree.AnnotationNode>emptyList() : annotations) {
					if (annotation.desc.endsWith("/SubscribeEvent;") && (method.access & Opcodes.ACC_PUBLIC) == 0) {
						method.access = (method.access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
						changed = true;
					}
				}
			}
		}
		return changed;
	}

	/**
	 * The handlers of a mixin run inside Minecraft's code, every tick: one that throws on 1.12.2 (a field the 1.21 mod
	 * expects, a null) would stop the server. Each handler is guarded: the error is logged once and the game goes on
	 * as without the handler (an @Inject returns, a @Modify* gives back the value it got, a @WrapWithCondition lets the
	 * call happen, others give 0 / null).
	 */
	private boolean guardHandlers(ClassNode node) {
		boolean changed = false;
		for (MethodNode method : node.methods) {
			String injector = injector(method);
			if (injector == null || method.instructions.size() == 0 || (method.access & Opcodes.ACC_ABSTRACT) != 0) {
				continue;
			}
			boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
			Type[] args = Type.getArgumentTypes(method.desc);
			Type returned = Type.getReturnType(method.desc);
			var locals = new ArrayList<Object>();
			if (!isStatic) {
				locals.add(node.name);
			}
			for (Type arg : args) {
				locals.add(switch (arg.getSort()) {
					case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> Opcodes.INTEGER;
					case Type.LONG -> Opcodes.LONG;
					case Type.FLOAT -> Opcodes.FLOAT;
					case Type.DOUBLE -> Opcodes.DOUBLE;
					case Type.ARRAY -> arg.getDescriptor();
					default -> arg.getInternalName();
				});
			}
			var start = new LabelNode();
			var end = new LabelNode();
			var handler = new LabelNode();
			method.instructions.insert(start);
			method.instructions.add(end);
			method.instructions.add(handler);
			method.instructions.add(new FrameNode(Opcodes.F_FULL, locals.size(), locals.toArray(), 1, new Object[] { "java/lang/Throwable" }));
			method.instructions.add(new LdcInsnNode(node.name.replace('/', '.') + "." + method.name + " (mixin " + injector + ")"));
			method.instructions.add(new InsnNode(Opcodes.SWAP));
			method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/runtime/RuntimeStubs", "failed",
					"(Ljava/lang/String;Ljava/lang/Throwable;)V", false));
			if (returned.getSort() == Type.VOID) {
				method.instructions.add(new InsnNode(Opcodes.RETURN));
			} else if (injector.equals("WrapWithCondition") && returned.getSort() == Type.BOOLEAN) {
				method.instructions.add(new InsnNode(Opcodes.ICONST_1));
				method.instructions.add(new InsnNode(Opcodes.IRETURN));
			} else if (injector.startsWith("Modify") && args.length > 0 && args[0].equals(returned)) {
				method.instructions.add(new VarInsnNode(returned.getOpcode(Opcodes.ILOAD), isStatic ? 0 : 1));
				method.instructions.add(new InsnNode(returned.getOpcode(Opcodes.IRETURN)));
			} else {
				method.instructions.add(defaultValue(returned));
				method.instructions.add(new InsnNode(returned.getOpcode(Opcodes.IRETURN)));
			}
			method.tryCatchBlocks.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.TryCatchBlockNode(start, end, handler, "java/lang/Throwable"));
			changed = true;
		}
		return changed;
	}

	/** The simple name of the Mixin / MixinExtras injector annotation of a method (Inject, ModifyReturnValue...), or null. */
	private static String injector(MethodNode method) {
		for (var annotations : java.util.Arrays.asList(method.visibleAnnotations, method.invisibleAnnotations)) {
			for (var annotation : annotations == null ? java.util.Collections.<xyz.wagyourtail.jvmdg.shade.asm.tree.AnnotationNode>emptyList() : annotations) {
				String desc = annotation.desc;
				if (desc.startsWith("Lorg/spongepowered/asm/mixin/injection/") || desc.startsWith("Lcom/llamalad7/mixinextras/injector/")) {
					String name = desc.substring(desc.lastIndexOf('/') + 1, desc.length() - 1);
					if (name.equals("Inject") || name.equals("Redirect") || name.startsWith("Modify") || name.startsWith("Wrap")) {
						return name;
					}
				}
			}
		}
		return null;
	}

	/**
	 * A lambda or method reference whose target takes an ItemLike gets 1.12.2 Items and Blocks, which don't implement
	 * the Translator's ItemLike: the lambda factory refuses that (LambdaConversionException, a creative tab's
	 * {@code output::accept}). Its target becomes a static bridge in the class whose ItemLike parameters are Object and
	 * which calls the target (an interface parameter takes any object). Done before anything reads the class.
	 */
	static byte[] itemLikeBridges(byte[] bytes) {
		String itemLike = "L" + ITEM_LIKE + ";";
		if (!new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1).contains(ITEM_LIKE)) {
			return bytes;
		}
		var node = new ClassNode();
		new ClassReader(bytes).accept(node, 0);
		var bridges = new ArrayList<MethodNode>();
		var made = new java.util.HashMap<String, Handle>();
		boolean isInterface = (node.access & Opcodes.ACC_INTERFACE) != 0;
		for (MethodNode method : node.methods) {
			for (AbstractInsnNode insn : method.instructions) {
				if (!(insn instanceof InvokeDynamicInsnNode indy) || !"java/lang/invoke/LambdaMetafactory".equals(indy.bsm.getOwner())
						|| indy.bsmArgs.length < 3 || !(indy.bsmArgs[1] instanceof Handle target)) {
					continue;
				}
				boolean instance = target.getTag() != Opcodes.H_INVOKESTATIC && target.getTag() != Opcodes.H_NEWINVOKESPECIAL;
				String params = (instance ? "L" + target.getOwner() + ";" : "") + target.getDesc().substring(1, target.getDesc().indexOf(')'));
				if (!params.contains(itemLike)) {
					continue;
				}
				String key = target.getTag() + target.getOwner() + "." + target.getName() + target.getDesc();
				Handle bridge = made.get(key);
				if (bridge == null) {
					String returned = target.getTag() == Opcodes.H_NEWINVOKESPECIAL ? "L" + target.getOwner() + ";"
							: target.getDesc().substring(target.getDesc().indexOf(')') + 1);
					String desc = "(" + params.replace(itemLike, "Ljava/lang/Object;") + ")" + returned;
					var m = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC | Opcodes.ACC_SYNTHETIC, "translator$itemLike$" + made.size(), desc, null, null);
					if (isInterface) {
						m.access = (m.access & ~Opcodes.ACC_PRIVATE) | Opcodes.ACC_PUBLIC;
					}
					if (target.getTag() == Opcodes.H_NEWINVOKESPECIAL) {
						m.instructions.add(new TypeInsnNode(Opcodes.NEW, target.getOwner()));
						m.instructions.add(new InsnNode(Opcodes.DUP));
					}
					int slot = 0;
					for (Type arg : Type.getArgumentTypes(desc)) {
						m.instructions.add(new VarInsnNode(arg.getOpcode(Opcodes.ILOAD), slot));
						slot += arg.getSize();
					}
					int opcode = switch (target.getTag()) {
						case Opcodes.H_INVOKESTATIC -> Opcodes.INVOKESTATIC;
						case Opcodes.H_INVOKEINTERFACE -> Opcodes.INVOKEINTERFACE;
						case Opcodes.H_INVOKESPECIAL, Opcodes.H_NEWINVOKESPECIAL -> Opcodes.INVOKESPECIAL;
						default -> Opcodes.INVOKEVIRTUAL;
					};
					m.instructions.add(new MethodInsnNode(opcode, target.getOwner(), target.getName(), target.getDesc(), target.isInterface()));
					m.instructions.add(new InsnNode(Type.getReturnType(desc).getOpcode(Opcodes.IRETURN)));
					bridges.add(m);
					bridge = new Handle(Opcodes.H_INVOKESTATIC, node.name, m.name, desc, isInterface);
					made.put(key, bridge);
				}
				indy.bsmArgs[1] = bridge;
				// captured values must have the bridge's types exactly (an ItemLike value passes as Object)
				indy.desc = indy.desc.substring(0, indy.desc.indexOf(')')).replace(itemLike, "Ljava/lang/Object;") + indy.desc.substring(indy.desc.indexOf(')'));
				// the instantiated type takes the bridge's parameters where the target took an ItemLike
				Type instantiated = (Type) indy.bsmArgs[2];
				Type[] args = instantiated.getArgumentTypes();
				Type[] bridgeArgs = Type.getArgumentTypes(bridge.getDesc());
				int captured = bridgeArgs.length - args.length;
				for (int i = 0; i < args.length; i++) {
					if (captured + i >= 0 && captured + i < bridgeArgs.length && itemLike.equals(args[i].getDescriptor())) {
						args[i] = Type.getType(Object.class);
					}
				}
				indy.bsmArgs[2] = Type.getMethodType(instantiated.getReturnType(), args);
			}
		}
		if (bridges.isEmpty()) {
			return bytes;
		}
		node.methods.addAll(bridges);
		var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		node.accept(writer);
		return writer.toByteArray();
	}

	private static boolean isMixin(ClassNode node) {
		for (var annotations : java.util.Arrays.asList(node.visibleAnnotations, node.invisibleAnnotations)) {
			for (var annotation : annotations == null ? java.util.Collections.<xyz.wagyourtail.jvmdg.shade.asm.tree.AnnotationNode>emptyList() : annotations) {
				if (ClassInfo.MIXIN.equals(annotation.desc)) {
					return true;
				}
			}
		}
		return false;
	}

	/** The classes a mixin applies to: its {@code @Mixin} value and targets. */
	private static List<String> mixinTargets(ClassNode node) {
		var targets = new ArrayList<String>();
		for (var annotations : java.util.Arrays.asList(node.visibleAnnotations, node.invisibleAnnotations)) {
			for (var annotation : annotations == null ? java.util.Collections.<xyz.wagyourtail.jvmdg.shade.asm.tree.AnnotationNode>emptyList() : annotations) {
				if (!ClassInfo.MIXIN.equals(annotation.desc) || annotation.values == null) {
					continue;
				}
				for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
					if (annotation.values.get(i + 1) instanceof List<?> values) {
						for (Object value : values) {
							if (value instanceof Type type) {
								targets.add(type.getInternalName());
							} else if (value instanceof String target) {
								targets.add(target.replace('.', '/'));
							}
						}
					}
				}
			}
		}
		return targets;
	}

	/**
	 * A mixin's {@code @Shadow} of a field the 1.12.2 target doesn't have (Xaero's ClientPacketListener
	 * {@code serverChunkRadius}): Mixin refuses the whole mixin, the interface it adds with it (a ClassCastException
	 * later). The field becomes the mixin's own ({@code @Unique}): the mixin applies, the field holds what the mod
	 * writes into it.
	 */
	private boolean uniqueMissingShadows(ClassNode node) {
		List<String> targets = mixinTargets(node);
		if (targets.isEmpty()) {
			return false;
		}
		boolean changed = false;
		for (FieldNode field : node.fields) {
			for (var annotations : java.util.Arrays.asList(field.visibleAnnotations, field.invisibleAnnotations)) {
				if (annotations == null || annotations.stream().noneMatch(annotation -> SHADOW.equals(annotation.desc))) {
					continue;
				}
				boolean missing = false;
				for (String target : targets) {
					missing |= this.info(target) != null && !this.hasMember(target, field.name, field.desc, false);
				}
				if (missing) {
					annotations.removeIf(annotation -> SHADOW.equals(annotation.desc) || FINAL.equals(annotation.desc)
							|| MUTABLE.equals(annotation.desc));
					annotations.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.AnnotationNode(UNIQUE));
					field.access &= ~Opcodes.ACC_FINAL;
					this.stubbed.add(node.name + "." + field.name + ": @Shadow of a field 1.12.2 doesn't have, the mixin's own field now");
					changed = true;
				}
			}
		}
		return changed;
	}

	private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";
	private static final String UNIQUE = "Lorg/spongepowered/asm/mixin/Unique;";
	private static final String FINAL = "Lorg/spongepowered/asm/mixin/Final;";
	private static final String MUTABLE = "Lorg/spongepowered/asm/mixin/Mutable;";

	private boolean hasPublicInstanceField(String owner, String member) {
		var seen = new java.util.HashSet<String>();
		for (String current = owner; current != null && seen.add(current); ) {
			ClassInfo info = this.info(current);
			if (info == null) {
				return false;
			}
			if (info.publicInstanceFields.contains(member)) {
				return true;
			}
			current = info.superName;
		}
		return false;
	}

	private static final String BUILT_IN_REGISTRIES = "io/github/xponer2/translator/util/BuiltInRegistries";
	private static final String REGISTRIES = "net/minecraft/core/registries/Registries";
	private static final String RESOURCE_KEY = "Lio/github/xponer2/translator/registry/ResourceKey;";
	private static final String REGISTRY = "io/github/xponer2/translator/registry/Registry";

	/**
	 * BuiltInRegistries.X the Translator has no field for (PLACEMENT_MODIFIER_TYPE, COMMAND_ARGUMENT_TYPE): the
	 * registry of the key Registries.X, made on first use, so what mods register there is kept (and found again).
	 */
	private static final String ENUM_CODECS = "io/github/xponer2/translator/util/EnumCodecs";
	private static final String ENUM_CODEC = "Lio/github/xponer2/translator/util/StringRepresentable$EnumCodec;";
	private static final String STREAM_CODEC = "Lio/github/xponer2/translator/network/StreamCodec;";

	/** A missing CODEC / STREAM_CODEC of a 1.12.2 enum: EnumCodecs.codec / streamCodec of that enum class. */
	private boolean enumCodec(MethodNode method, FieldInsnNode field) {
		if (field.getOpcode() != Opcodes.GETSTATIC) {
			return false;
		}
		boolean codec = "CODEC".equals(field.name) && ("Lcom/mojang/serialization/Codec;".equals(field.desc) || ENUM_CODEC.equals(field.desc));
		boolean stream = "STREAM_CODEC".equals(field.name) && STREAM_CODEC.equals(field.desc);
		if (!codec && !stream) {
			return false;
		}
		ClassInfo info = this.info(field.owner);
		if (info == null || (info.access & Opcodes.ACC_ENUM) == 0) {
			return false;
		}
		var replacement = new InsnList();
		replacement.add(new LdcInsnNode(Type.getObjectType(field.owner)));
		replacement.add(codec
				? new MethodInsnNode(Opcodes.INVOKESTATIC, ENUM_CODECS, "codec", "(Ljava/lang/Class;)" + ENUM_CODEC, false)
				: new MethodInsnNode(Opcodes.INVOKESTATIC, ENUM_CODECS, "streamCodec", "(Ljava/lang/Class;)" + STREAM_CODEC, false));
		method.instructions.insertBefore(field, replacement);
		method.instructions.remove(field);
		this.redirected.incrementAndGet();
		return true;
	}

	/** The Registries field of a BuiltInRegistries one: the same name, else one that differs only in underscores. */
	private String registryKeyName(String name) {
		if (this.hasMember(REGISTRIES, name, RESOURCE_KEY, false)) {
			return name;
		}
		ClassInfo info = this.info(REGISTRIES);
		if (info == null) {
			return null;
		}
		String wanted = name.replace("_", "");
		for (String member : info.fields) {
			int space = member.indexOf(' ');
			if (space > 0 && member.substring(space + 1).equals(RESOURCE_KEY) && member.substring(0, space).replace("_", "").equals(wanted)) {
				return member.substring(0, space);
			}
		}
		return null;
	}

	/**
	 * {@code Blocks.X.defaultBlockState()} of a 1.21 block that is a variant of a 1.12.2 block (FlattenedBlocks.X: granite
	 * is stone 1; 1.12.2's Blocks.TALLGRASS, which only 1.21's SHORT_GRASS maps to: tall grass 1): the flattening's
	 * state (FlattenedStates), not the 1.12.2 block's default (plain stone, a dead bush).
	 */
	private static boolean flattenedDefaults(MethodNode method) {
		boolean changed = false;
		for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
			if (!(insn instanceof FieldInsnNode field) || field.getOpcode() != Opcodes.GETSTATIC) {
				continue;
			}
			String name = "io/github/xponer2/translator/runtime/redirect/FlattenedBlocks".equals(field.owner) ? field.name
					: "net/minecraft/init/Blocks".equals(field.owner) && "TALLGRASS".equals(field.name) ? "SHORT_GRASS" : null;
			if (name == null) {
				continue;
			}
			AbstractInsnNode next = field.getNext();
			while (next != null && next.getOpcode() < 0) {
				next = next.getNext();
			}
			if (!(next instanceof MethodInsnNode call) || call.getOpcode() != Opcodes.INVOKEVIRTUAL || !"getDefaultState".equals(call.name)
					|| !"()Lnet/minecraft/block/state/IBlockState;".equals(call.desc)) {
				continue;
			}
			method.instructions.insertBefore(call, new LdcInsnNode("minecraft:" + name.toLowerCase(java.util.Locale.ROOT)));
			method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/runtime/redirect/FlattenedStates",
					"defaultState", "(Lnet/minecraft/block/Block;Ljava/lang/String;)Lnet/minecraft/block/state/IBlockState;", false));
			changed = true;
		}
		return changed;
	}

	private boolean registryByKey(MethodNode method, FieldInsnNode field) {
		if (field.getOpcode() != Opcodes.GETSTATIC || !BUILT_IN_REGISTRIES.equals(field.owner)) {
			return false;
		}
		// the key in Registries: the same name, or the same name but for underscores (1.21 has
		// BuiltInRegistries.BLOCKSTATE_PROVIDER_TYPE for Registries.BLOCK_STATE_PROVIDER_TYPE)
		String key = this.registryKeyName(field.name);
		if (key == null) {
			return false;
		}
		var replacement = new InsnList();
		replacement.add(new FieldInsnNode(Opcodes.GETSTATIC, REGISTRIES, key, RESOURCE_KEY));
		replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BUILT_IN_REGISTRIES, "byKey", "(" + RESOURCE_KEY + ")L" + REGISTRY + ";", false));
		Type expected = Type.getType(field.desc);
		if (expected.getSort() == Type.OBJECT && !REGISTRY.equals(expected.getInternalName())) {
			replacement.add(new TypeInsnNode(Opcodes.CHECKCAST, expected.getInternalName()));
		}
		method.instructions.insertBefore(field, replacement);
		method.instructions.remove(field);
		this.redirected.incrementAndGet();
		return true;
	}

	void guard(File input, File output) throws IOException {
		var classes = new HashMap<String, byte[]>();
		try (var zip = new ZipFile(input)) {
			for (ZipEntry entry : Collections.list(zip.entries())) {
				if (entry.getName().endsWith(".class")) {
					try (InputStream in = zip.getInputStream(entry)) {
						byte[] bytes = InterfaceBridges.add(itemLikeBridges(ModJar.readAll(in)));
						ClassInfo info = ClassInfo.read(bytes);
						this.mod.put(info.name, info);
						classes.put(entry.getName(), bytes);
					}
				}
			}
			// the classes are checked in parallel (all cores), then written in the jar's order
			var guarded = new java.util.concurrent.ConcurrentHashMap<String, byte[]>();
			classes.entrySet().parallelStream().forEach(entry -> guarded.put(entry.getKey(), this.guardClass(entry.getValue())));
			try (var out = new ZipOutputStream(new FileOutputStream(output))) {
				for (ZipEntry entry : Collections.list(zip.entries())) {
					if (entry.isDirectory()) {
						continue;
					}
					byte[] bytes = guarded.get(entry.getName());
					if (bytes == null) {
						try (InputStream in = zip.getInputStream(entry)) {
							bytes = ModJar.readAll(in);
						}
					}
					out.putNextEntry(new ZipEntry(entry.getName()));
					out.write(bytes);
					out.closeEntry();
				}
			}
		}
	}

	/**
	 * The {@code @Mod} of a jar built for Forge 1.13+ (a multi-loader library a mod bundles: MixinSquared's Forge jar)
	 * has the name of the 1.12.2 one: Forge 1.12.2 found it on the class path, took it for a 1.13 mod and stopped the
	 * game. The Translator starts the 1.21.1 mods itself, nothing converted is a 1.12.2 mod.
	 */
	private static boolean dropForgeModAnnotation(ClassNode node) {
		boolean dropped = false;
		for (var annotations : java.util.Arrays.asList(node.visibleAnnotations, node.invisibleAnnotations)) {
			if (annotations != null) {
				dropped |= annotations.removeIf(annotation -> "Lnet/minecraftforge/fml/common/Mod;".equals(annotation.desc));
			}
		}
		return dropped;
	}

	/**
	 * Two methods of one name and descriptor (the JVM refuses the class: ClassFormatError "Duplicate method name"): the
	 * downgrade to Java 8 can make them, a Java 21 type that has no Java 8 counterpart becoming its supertype (Mekanism's
	 * record accessor typedModules() returns a SequencedMap, its interface bridge a Map: both a Map now, and the class
	 * that holds every module of a gear item didn't load). The bridge goes, the method itself stays; with no bridge, the
	 * first one.
	 */
	private static boolean dropDuplicateMethods(ClassNode node) {
		var byKey = new java.util.HashMap<String, MethodNode>();
		var drop = new java.util.ArrayList<MethodNode>();
		for (MethodNode method : node.methods) {
			MethodNode seen = byKey.putIfAbsent(method.name + method.desc, method);
			if (seen == null) {
				continue;
			}
			boolean seenBridge = (seen.access & Opcodes.ACC_BRIDGE) != 0;
			boolean thisBridge = (method.access & Opcodes.ACC_BRIDGE) != 0;
			if (seenBridge && !thisBridge) {
				drop.add(seen);
				byKey.put(method.name + method.desc, method);
			} else {
				drop.add(method);
			}
		}
		return node.methods.removeAll(drop);
	}

	private byte[] guardClass(byte[] bytes) {
		var node = new ClassNode();
		new ClassReader(bytes).accept(node, 0);
		boolean changed = dropDuplicateMethods(node);
		changed |= this.renameFinalOverrides(node);
		changed |= dropForgeModAnnotation(node);
		changed |= publicSubscribers(node);
		if (isMixin(node)) {
			changed |= guardHandlers(node);
			changed |= this.uniqueMissingShadows(node);
		}
		changed |= this.addBridges(node);
		changed |= this.addEntityInitBridge(node);
		changed |= this.addDefaultConstructor(node);
		if (!isMixin(node)) {
			changed |= this.implementAbstract(node);
		}
		for (int index = 0; index < node.methods.size(); index++) {
			MethodNode method = node.methods.get(index);
			if (method.instructions.size() == 0) {
				continue;
			}
			changed |= this.redirect(method, node.name);
			// after redirect: the 1.21 Blocks fields 1.12.2 lacks are FlattenedBlocks' then
			changed |= flattenedDefaults(method);
			changed |= this.fixInvokeKinds(method);
			if ("<clinit>".equals(method.name)) {
				// a throwing static initializer would take the whole class down: what is missing there gives null / 0,
				// and what throws anyway is logged, with the fields it didn't get to left null
				this.soften(node.name, method);
				// (not a mixin's: Mixin merges its instructions into the target's static initializer without the
				// exception table, the handler would run as plain code there)
				if (!isMixin(node)) {
					guardInitializer(node.name, method, (node.version & 0xFFFF) >= Opcodes.V1_6);
				}
				changed = true;
				continue;
			}
			String reason = this.missing(method);
			if (reason != null && this.canSoften(node, method)) {
				// the rest of the method runs; only what is missing gives null / 0 (and is reported when reached)
				MethodNode softened = copy(method);
				this.soften(node.name, softened, true);
				// the stack map frames may still name a missing class (a local of that type): the JVM can't check
				// those against the code (VerifyError), the ASM verifier doesn't look at frames; then the stub is safer
				String still = this.missing(softened);
				String rejected = still != null ? "still " + still
						: this.framesNameMissing(softened) ? "a stack map frame names a missing class" : this.verify(node.name, softened);
				if (rejected != null) {
					this.stubbed.add(node.name + "." + method.name + ": not softened (" + rejected + ")");
				}
				if (rejected == null) {
					if (!"<init>".equals(method.name) && !isMixin(node)) {
						// no worse than the stub when the rest of it throws
						MethodNode guarded = copy(softened);
						guardSoftened(node.name, guarded);
						if (this.verify(node.name, guarded) == null) {
							softened = guarded;
						}
					}
					node.methods.set(index, softened);
					this.softened.incrementAndGet();
					changed = true;
					continue;
				}
			}
			if (reason == null) {
				reason = this.verify(node.name, method);
			}
			if (reason != null) {
				this.stub(node.name, node.superName, method, reason);
				changed = true;
			} else if (this.isTick(node, method) && !isMixin(node)) {
				// a mod's entity or block entity whose tick throws crashed the server, and the world crashed again on every
				// load (Iron's Spells' Dead King corpse): logged once, the tick ends there and the world goes on
				MethodNode guarded = copy(method);
				guardSoftened(node.name, guarded);
				if (this.verify(node.name, guarded) == null) {
					node.methods.set(index, guarded);
					changed = true;
				}
			}
		}
		if (!changed) {
			return bytes;
		}
		var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		node.accept(writer);
		return writer.toByteArray();
	}

	/**
	 * Remapping can turn a mod method into an override javac would have bridged: {@code createMenu} returning the
	 * Translator AbstractContainerMenu where MenuProvider declares Container. For such a method of a supertype outside
	 * the mod, a bridge with the supertype's descriptor calls the mod's method: when only the return type differs (then
	 * it was an override in 1.21), or when the parameters are narrower and the supertype's method is abstract (no
	 * bridge for an overload like equals(Foo) next to equals(Object)).
	 */
	private static final String DATA_BUILDER = "io/github/xponer2/translator/entity/SynchedDataBuilder";
	private static final String DEFINE_DATA = "entityInit (L" + DATA_BUILDER + ";)V";

	/**
	 * A mod's mob (a LivingEntity or Mob, 1.12.2 classes) defines its data in {@code defineSynchedData(builder)}, which
	 * the table names entityInit but 1.12.2 never calls with a builder: its data parameters stayed unregistered (every
	 * read of them threw). The first class of the chain that defines them gets the 1.12.2 entityInit(), which calls
	 * the one with the builder after its superclass's.
	 */
	/**
	 * A 1.12.2 method that is abstract where the 1.21 one had a default (Container.startOpen is IInventory's abstract
	 * openInventory): a mod class that didn't override it throws AbstractMethodError when the game calls it (Tom's
	 * Simple Storage's level emitter menu, opened). It gets one that does nothing: logged once, returns 0, false,
	 * an empty name or null.
	 */
	private boolean implementAbstract(ClassNode node) {
		// module-info (JOML has one) has no superclass: nothing to implement, and a null in the deque below throws
		if ((node.access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT | Opcodes.ACC_MODULE)) != 0 || node.superName == null) {
			return false;
		}
		var concrete = new java.util.HashSet<String>();
		var names = new java.util.HashSet<String>();
		for (MethodNode method : node.methods) {
			concrete.add(method.name + " " + method.desc);
			names.add(method.name);
		}
		var wanted = new java.util.LinkedHashMap<String, String>();
		var todo = new java.util.ArrayDeque<String>();
		var seen = new java.util.HashSet<String>();
		todo.add(node.superName);
		todo.addAll(node.interfaces);
		while (!todo.isEmpty()) {
			String current = todo.poll();
			ClassInfo info = current == null || !seen.add(current) || isPlatform(current) ? null : this.info(current);
			if (info == null) {
				continue;
			}
			boolean modClass = this.mod.containsKey(current);
			for (String member : info.methods) {
				if (!info.abstractMethods.contains(member)) {
					concrete.add(member);
				} else if (!modClass) {
					wanted.putIfAbsent(member, current);
				}
				if (modClass) {
					// the converter may give a mod class bridges its listing doesn't have yet: same name, other types
					names.add(member.substring(0, member.indexOf(' ')));
				}
			}
			todo.addAll(info.supertypes());
		}
		boolean changed = false;
		for (var entry : wanted.entrySet()) {
			String member = entry.getKey();
			String name = member.substring(0, member.indexOf(' '));
			String desc = member.substring(member.indexOf(' ') + 1);
			if (concrete.contains(member) || names.contains(name) || name.startsWith("<")) {
				continue;
			}
			var stub = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, desc, null, null);
			MethodInsnNode helper = this.helperFor(node.superName, name, desc);
			if (helper != null) {
				// what a 1.12.2 superclass gives through a helper (1.21's level() of an entity is 1.12.2's field world):
				// the stub returning null stood in for it, also for the mod's own calls (BOP's chest boats: null level)
				stub.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
				int slot = 1;
				for (Type argument : Type.getArgumentTypes(desc)) {
					stub.instructions.add(new VarInsnNode(argument.getOpcode(Opcodes.ILOAD), slot));
					slot += argument.getSize();
				}
				stub.instructions.add(helper);
				stub.instructions.add(new InsnNode(Type.getReturnType(desc).getOpcode(Opcodes.IRETURN)));
				stub.maxStack = slot + 1;
				stub.maxLocals = slot;
				node.methods.add(stub);
				this.bridged.incrementAndGet();
				changed = true;
				continue;
			}
			stub.instructions.add(new LdcInsnNode(node.name.replace('/', '.') + "." + name + " (abstract on 1.12.2 in "
					+ entry.getValue().replace('/', '.') + ", a default in 1.21)"));
			stub.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/runtime/RuntimeStubs", "missing",
					"(Ljava/lang/String;)V", false));
			Type returned = Type.getReturnType(desc);
			switch (returned.getSort()) {
				case Type.VOID -> stub.instructions.add(new InsnNode(Opcodes.RETURN));
				case Type.LONG -> stub.instructions.add(new InsnNode(Opcodes.LCONST_0));
				case Type.FLOAT -> stub.instructions.add(new InsnNode(Opcodes.FCONST_0));
				case Type.DOUBLE -> stub.instructions.add(new InsnNode(Opcodes.DCONST_0));
				case Type.OBJECT, Type.ARRAY -> {
					if ("java/lang/String".equals(returned.getInternalName())) {
						stub.instructions.add(new LdcInsnNode(""));
					} else if ("net/minecraft/util/text/ITextComponent".equals(returned.getInternalName())) {
						stub.instructions.add(new TypeInsnNode(Opcodes.NEW, "net/minecraft/util/text/TextComponentString"));
						stub.instructions.add(new InsnNode(Opcodes.DUP));
						stub.instructions.add(new LdcInsnNode(""));
						stub.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/minecraft/util/text/TextComponentString", "<init>",
								"(Ljava/lang/String;)V", false));
					} else {
						stub.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
					}
				}
				default -> stub.instructions.add(new InsnNode(Opcodes.ICONST_0));
			}
			if (returned.getSort() != Type.VOID) {
				stub.instructions.add(new InsnNode(returned.getOpcode(Opcodes.IRETURN)));
			}
			stub.maxStack = 3;
			stub.maxLocals = 1 + java.util.Arrays.stream(Type.getArgumentTypes(desc)).mapToInt(Type::getSize).sum();
			node.methods.add(stub);
			this.bridged.incrementAndGet();
			changed = true;
		}
		return changed;
	}

	/**
	 * A static helper registered for a class of the superclass chain from {@code superName} that gives 1.21's
	 * {@code name desc} with the instance first (as a mod's call of it is redirected), or null.
	 */
	@javax.annotation.Nullable
	private MethodInsnNode helperFor(@javax.annotation.Nullable String superName, String name, String desc) {
		var seen = new java.util.HashSet<String>();
		for (String owner = superName; owner != null && seen.add(owner);) {
			String helperDesc = "(L" + owner + ";" + desc.substring(1);
			for (String helper : this.redirects.getOrDefault(owner, java.util.Collections.emptyList())) {
				if (this.hasMember(helper, name, helperDesc, true)) {
					return new MethodInsnNode(Opcodes.INVOKESTATIC, helper, name, helperDesc, false);
				}
			}
			ClassInfo info = this.info(owner);
			owner = info == null ? null : info.superName;
		}
		return null;
	}

	private boolean addEntityInitBridge(ClassNode node) {
		boolean defines = false;
		for (MethodNode method : node.methods) {
			if ("entityInit ()V".equals(method.name + " " + method.desc)) {
				return false;
			}
			defines |= DEFINE_DATA.equals(method.name + " " + method.desc);
		}
		if (!defines) {
			return false;
		}
		// a mod superclass that defines data has the bridge already: one call of the builder method per entity
		for (ClassInfo info = this.info(node.superName); info != null; info = info.superName == null ? null : this.info(info.superName)) {
			if (info.methods.contains(DEFINE_DATA)) {
				return false;
			}
		}
		var bridge = new MethodNode(Opcodes.ACC_PROTECTED, "entityInit", "()V", null, null);
		bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
		bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, node.superName, "entityInit", "()V", false));
		bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
		bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
		bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DATA_BUILDER, "of", "(Lnet/minecraft/entity/Entity;)L" + DATA_BUILDER + ";", false));
		bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, node.name, "entityInit", "(L" + DATA_BUILDER + ";)V", false));
		bridge.instructions.add(new InsnNode(Opcodes.RETURN));
		bridge.maxStack = 2;
		bridge.maxLocals = 1;
		node.methods.add(bridge);
		return true;
	}

	private boolean addBridges(ClassNode node) {
		if ((node.access & Opcodes.ACC_INTERFACE) != 0) {
			return false;
		}
		var declared = new java.util.HashSet<String>();
		for (MethodNode method : node.methods) {
			declared.add(method.name + " " + method.desc);
		}
		var bridges = new ArrayList<MethodNode>();
		for (MethodNode method : node.methods) {
			if ((method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE | Opcodes.ACC_BRIDGE)) != 0 || method.name.startsWith("<")) {
				continue;
			}
			Type[] args = Type.getArgumentTypes(method.desc);
			Type returned = Type.getReturnType(method.desc);
			for (String[] inherited : this.inheritedMethods(node.name, method.name)) {
				String desc = inherited[0];
				if (declared.contains(method.name + " " + desc)) {
					continue;
				}
				Type[] superArgs = Type.getArgumentTypes(desc);
				Type superReturned = Type.getReturnType(desc);
				if (superArgs.length != args.length || !this.assignable(returned, superReturned)) {
					continue;
				}
				boolean sameArgs = true;
				boolean narrower = true;
				for (int i = 0; i < args.length; i++) {
					sameArgs &= args[i].equals(superArgs[i]);
					narrower &= this.assignable(args[i], superArgs[i]);
				}
				boolean isAbstract = inherited[1] != null;
				if (!(sameArgs || narrower && isAbstract && !this.hasConcreteMember(node.superName, method.name, desc))) {
					continue;
				}
				var bridge = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC | Opcodes.ACC_BRIDGE, method.name, desc, null,
						method.exceptions == null ? null : method.exceptions.toArray(new String[0]));
				bridge.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
				int slot = 1;
				for (int i = 0; i < args.length; i++) {
					bridge.instructions.add(new VarInsnNode(superArgs[i].getOpcode(Opcodes.ILOAD), slot));
					if (!args[i].equals(superArgs[i])) {
						bridge.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, args[i].getInternalName()));
					}
					slot += superArgs[i].getSize();
				}
				bridge.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, node.name, method.name, method.desc, false));
				bridge.instructions.add(new InsnNode(returned.getOpcode(Opcodes.IRETURN)));
				bridge.maxLocals = slot;
				bridge.maxStack = Math.max(slot, returned.getSize());
				bridges.add(bridge);
				declared.add(method.name + " " + desc);
				this.bridged.incrementAndGet();
			}
		}
		node.methods.addAll(bridges);
		return !bridges.isEmpty();
	}

	private static final String ITEM_LIKE = "io/github/xponer2/translator/util/ItemLike";
	/**
	 * A mod method with the name and descriptor of a final 1.12.2 method of a superclass (AE2's own getRegistryName next
	 * to Forge's final one, Create's fireImmune that is the final isImmuneToFire now): the JVM refuses the whole class.
	 * The mod's method gets another name; calls keep reaching the 1.12.2 one.
	 */
	private boolean renameFinalOverrides(ClassNode node) {
		boolean changed = false;
		for (MethodNode method : node.methods) {
			if ((method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) != 0 || method.name.startsWith("<")) {
				continue;
			}
			String member = method.name + " " + method.desc;
			var seen = new java.util.HashSet<String>();
			for (String current = node.superName; current != null && seen.add(current); ) {
				ClassInfo info = this.info(current);
				if (info == null) {
					break;
				}
				if (info.finalMethods.contains(member)) {
					this.stubbed.add(node.name + "." + method.name + method.desc + ": renamed to " + method.name + "$final, "
							+ current + " declares it final");
					method.name = method.name + "$final";
					changed = true;
					break;
				}
				current = info.superName;
			}
		}
		return changed;
	}

	private static final String TILE_ENTITY_INIT ="(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)V";
	private static final String TILE_ENTITY_TYPED_INIT = "(Lio/github/xponer2/translator/registry/BlockEntityType;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;)V";

	/**
	 * 1.12.2 creates a saved tile entity with its no-arg constructor (TileEntity.create, newInstance): a 1.21 block
	 * entity with only the (pos, state) one gets {@code this(BlockPos.ORIGIN, null)}, like the source ports
	 * (project.json default_constructors). Without it the block entity is lost when its chunk loads again.
	 */
	private boolean addDefaultConstructor(ClassNode node) {
		if ((node.access & (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT)) != 0
				|| !this.isSubtype("L" + node.name + ";", "Lnet/minecraft/tileentity/TileEntity;")) {
			return false;
		}
		boolean withState = false;
		boolean withType = false;
		for (MethodNode method : node.methods) {
			if ("<init>".equals(method.name) && "()V".equals(method.desc)) {
				return false;
			}
			withState |= "<init>".equals(method.name) && TILE_ENTITY_INIT.equals(method.desc);
			withType |= "<init>".equals(method.name) && TILE_ENTITY_TYPED_INIT.equals(method.desc);
		}
		if (!withState && !withType) {
			return false;
		}
		// the state (and type) of its registered type (BlockEntityDefaults): not null, which a block entity reading its
		// state when made threw on (Iron's Spells' portal frame); a block entity made with its type only got none at all
		// (Iron's Spells' statues)
		var defaults = "io/github/xponer2/translator/registry/BlockEntityDefaults";
		var init = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
		init.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
		if (!withState) {
			init.instructions.add(new LdcInsnNode(Type.getObjectType(node.name)));
			init.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, defaults, "type", "(Ljava/lang/Class;)Lio/github/xponer2/translator/registry/BlockEntityType;", false));
		}
		init.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC, "net/minecraft/util/math/BlockPos", "ORIGIN", "Lnet/minecraft/util/math/BlockPos;"));
		init.instructions.add(new LdcInsnNode(Type.getObjectType(node.name)));
		init.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, defaults, "state", "(Ljava/lang/Class;)Lnet/minecraft/block/state/IBlockState;", false));
		init.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, node.name, "<init>", withState ? TILE_ENTITY_INIT : TILE_ENTITY_TYPED_INIT, false));
		init.instructions.add(new InsnNode(Opcodes.RETURN));
		init.maxLocals = 1;
		init.maxStack = 4;
		node.methods.add(init);
		return true;
	}

	/**
	 * The lambda version of {@link #addBridges}: the interface method a lambda implements (the SAM type of
	 * LambdaMetafactory) can have other types after remapping than the interface outside the mod declares (a menu
	 * factory returning the Translator AbstractContainerMenu where the Translator interface says Container); the class
	 * the factory makes would then miss the interface method. The interface's abstract method of that name becomes the
	 * SAM type when the lambda's own (instantiated) type fits it.
	 */
	private boolean fixLambdaInterfaceType(InvokeDynamicInsnNode indy) {
		if (!"java/lang/invoke/LambdaMetafactory".equals(indy.bsm.getOwner()) || indy.bsmArgs.length < 3
				|| !(indy.bsmArgs[0] instanceof Type sam) || !(indy.bsmArgs[2] instanceof Type instantiated)) {
			return false;
		}
		Type returned = Type.getReturnType(indy.desc);
		if (returned.getSort() != Type.OBJECT || this.mod.containsKey(returned.getInternalName())
				|| this.hasMember(returned.getInternalName(), indy.name, sam.getDescriptor(), true)) {
			return false;
		}
		Type[] args = instantiated.getArgumentTypes();
		for (String[] candidate : this.methodsNamed(returned.getInternalName(), indy.name, true)) {
			Type type = Type.getMethodType(candidate[0]);
			Type[] candidateArgs = type.getArgumentTypes();
			if (candidate[1] == null || candidateArgs.length != args.length) {
				continue;
			}
			boolean fits = type.getReturnType().getSort() == Type.VOID || this.assignable(instantiated.getReturnType(), type.getReturnType());
			for (int i = 0; fits && i < args.length; i++) {
				fits = this.assignable(args[i], candidateArgs[i]);
			}
			if (fits) {
				indy.bsmArgs[0] = type;
				this.bridged.incrementAndGet();
				return true;
			}
		}
		return false;
	}

	/** {descriptor, "abstract" or null} of the methods with that name in the supertypes outside the mod. */
	private List<String[]> inheritedMethods(String owner, String name) {
		return this.methodsNamed(owner, name, false);
	}

	/** {descriptor, "abstract" or null} of the methods with that name in the class (or not) and its supertypes outside the mod. */
	private List<String[]> methodsNamed(String owner, String name, boolean self) {
		var found = new ArrayList<String[]>();
		var todo = new java.util.ArrayDeque<String>();
		var seen = new java.util.HashSet<String>();
		ClassInfo info0 = this.info(owner);
		if (self) {
			todo.add(owner);
		} else if (info0 != null) {
			todo.addAll(info0.supertypes());
		}
		while (!todo.isEmpty()) {
			String current = todo.poll();
			ClassInfo info = !seen.add(current) || isPlatform(current) ? null : this.info(current);
			if (info == null) {
				continue;
			}
			if (!this.mod.containsKey(current)) {
				for (String member : info.methods) {
					if (member.startsWith(name + " ")) {
						found.add(new String[] {member.substring(name.length() + 1), info.abstractMethods.contains(member) ? "abstract" : null});
					}
				}
			}
			todo.addAll(info.supertypes());
		}
		return found;
	}

	/**
	 * Calls of methods a 1.12.2 class doesn't have become calls of a static stand-in with the same name in one of the
	 * Translator classes registered for that class or a supertype (the receiver as the first parameter).
	 */
	private boolean redirect(MethodNode method, String currentClass) {
		boolean changed = false;
		for (AbstractInsnNode insn : method.instructions.toArray()) {
			// an X line: the member exists but means something else in 1.21 (its helper gives the 1.21 meaning)
			String forcedHelper = insn instanceof MethodInsnNode candidate && !this.forced.isEmpty() && candidate.getOpcode() != Opcodes.INVOKESPECIAL
					? this.forced.get(candidate.owner + "." + candidate.name + candidate.desc) : null;
			if (forcedHelper != null && insn instanceof MethodInsnNode forcedCall) {
				String desc = forcedCall.getOpcode() == Opcodes.INVOKESTATIC ? forcedCall.desc : "(L" + forcedCall.owner + ";" + forcedCall.desc.substring(1);
				int space = forcedHelper.indexOf(' ');
				if (space > 0) {
					desc = forcedHelper.substring(space + 1);
					forcedHelper = forcedHelper.substring(0, space);
				}
				method.instructions.set(forcedCall, new MethodInsnNode(Opcodes.INVOKESTATIC, forcedHelper, forcedCall.name, desc, false));
				this.redirected.incrementAndGet();
				changed = true;
				continue;
			}
			// a library switch the Translator sets (K lines): the read gives the constant (the receiver is dropped)
			if (insn instanceof FieldInsnNode read && (read.getOpcode() == Opcodes.GETFIELD || read.getOpcode() == Opcodes.GETSTATIC)
					&& !this.constants.isEmpty() && this.constants.get(read.owner + "." + read.name + ":" + read.desc) instanceof Integer value) {
				var constant = new InsnList();
				if (read.getOpcode() == Opcodes.GETFIELD) {
					constant.add(new InsnNode(Opcodes.POP));
				}
				constant.add(new LdcInsnNode(value));
				method.instructions.insertBefore(read, constant);
				method.instructions.remove(read);
				changed = true;
				continue;
			}
			// 1.21 items and blocks are ItemLikes, 1.12.2 ones aren't (the Translator's are): no cast to it (an interface
			// parameter takes any object), and asItem() through ItemLike.item(Object), which knows 1.12.2 items and blocks
			if (insn instanceof TypeInsnNode cast && cast.getOpcode() == Opcodes.CHECKCAST && ITEM_LIKE.equals(cast.desc)) {
				method.instructions.remove(cast);
				changed = true;
				continue;
			}
			if (insn instanceof MethodInsnNode call && ITEM_LIKE.equals(call.owner) && "asItem".equals(call.name)
					&& "()Lnet/minecraft/item/Item;".equals(call.desc)) {
				method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, ITEM_LIKE, "item", "(Ljava/lang/Object;)Lnet/minecraft/item/Item;", true));
				changed = true;
				continue;
			}
			if (insn instanceof TypeInsnNode check && check.getOpcode() == Opcodes.INSTANCEOF && !this.exists(check.desc)) {
				// nothing can be an instance of a class 1.12.2 doesn't have (LanternBlock, ...): false
				var replacement = new InsnList();
				replacement.add(new InsnNode(Opcodes.POP));
				replacement.add(new InsnNode(Opcodes.ICONST_0));
				method.instructions.insertBefore(check, replacement);
				method.instructions.remove(check);
				changed = true;
				continue;
			}
			// a member of a facade reached through its 1.21 class (javac names Screen as the owner of a subclass's
			// this.renderables): 1.12.2's class (GuiScreen) hasn't it, the facade has; the receiver is cast to it
			// (a stand-in for the 1.12.2 class comes first: it takes any object of that class, a facade's or not, where the
			// cast fails for the game's own objects: Blocks.AIR.defaultBlockState() in BOP's updateShape)
			if (insn instanceof FieldInsnNode member && (member.getOpcode() == Opcodes.GETFIELD || member.getOpcode() == Opcodes.PUTFIELD)
					&& !this.facades.isEmpty() && this.facades.containsKey(member.owner) && !this.hasMember(member.owner, member.name, member.desc, false)
					&& this.hasMember(this.facades.get(member.owner), member.name, member.desc, false)
					&& (member.getOpcode() == Opcodes.GETFIELD || Type.getType(member.desc).getSize() == 1)
					&& (member.getOpcode() != Opcodes.GETFIELD || this.findRedirect(member.owner, member.name, "()" + member.desc, false) == null)) {
				String facade = this.facades.get(member.owner);
				if (!this.extendsClass(currentClass, facade)) {
					// reached from outside the facade's classes: the object may be another facade of the same 1.12.2 class
					// (a container screen is no Screen: Sophisticated's GuiHelper read screen.font and its backpack screen
					// failed every frame), and the member may be protected (mods widen Screen.font with an access
					// transformer): read or written by name on the object itself
					var dynamic = new InsnList();
					Type type = Type.getType(member.desc);
					if (member.getOpcode() == Opcodes.GETFIELD) {
						dynamic.add(new LdcInsnNode(facade));
						dynamic.add(new LdcInsnNode(member.name));
						dynamic.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ACCESSOR_BRIDGE, "get",
								"(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/Object;", false));
						unboxTo(dynamic, type);
					} else {
						boxFrom(dynamic, type);
						dynamic.add(new LdcInsnNode(facade));
						dynamic.add(new LdcInsnNode(member.name));
						dynamic.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ACCESSOR_BRIDGE, "put",
								"(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V", false));
					}
					method.instructions.insertBefore(member, dynamic);
					method.instructions.remove(member);
					method.maxStack += 4;
					this.redirected.incrementAndGet();
					changed = true;
					continue;
				}
				String receiver = this.receiverType(facade, member.name, member.desc, currentClass);
				var cast = new InsnList();
				if (member.getOpcode() == Opcodes.PUTFIELD) {
					cast.add(new InsnNode(Opcodes.SWAP));
					cast.add(new TypeInsnNode(Opcodes.CHECKCAST, receiver));
					cast.add(new InsnNode(Opcodes.SWAP));
				} else {
					cast.add(new TypeInsnNode(Opcodes.CHECKCAST, receiver));
				}
				method.instructions.insertBefore(member, cast);
				member.owner = facade;
				this.redirected.incrementAndGet();
				changed = true;
				continue;
			}
			if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKEVIRTUAL && !this.facades.isEmpty()
					&& this.facades.containsKey(call.owner) && !this.hasMember(call.owner, call.name, call.desc, true)
					&& this.hasMember(this.facades.get(call.owner), call.name, call.desc, true)
					&& this.findRedirect(call.owner, call.name, call.desc, false) == null) {
				// the same for a method only the facade has: the arguments into new locals, the receiver cast, back
				String facade = this.facades.get(call.owner);
				Type[] args = Type.getArgumentTypes(call.desc);
				if (!this.extendsClass(currentClass, facade)) {
					// from outside the facade's classes (see the fields above): invoked by name on the object itself
					var dynamic = new InsnList();
					int array = method.maxLocals++;
					dynamic.add(new LdcInsnNode(args.length));
					dynamic.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
					dynamic.add(new VarInsnNode(Opcodes.ASTORE, array));
					for (int i = args.length - 1; i >= 0; i--) {
						// the last argument is on top: boxed into the array from the end
						boxFrom(dynamic, args[i]);
						dynamic.add(new VarInsnNode(Opcodes.ALOAD, array));
						dynamic.add(new InsnNode(Opcodes.SWAP));
						dynamic.add(new LdcInsnNode(i));
						dynamic.add(new InsnNode(Opcodes.SWAP));
						dynamic.add(new InsnNode(Opcodes.AASTORE));
					}
					dynamic.add(new LdcInsnNode(facade));
					dynamic.add(new LdcInsnNode(call.name));
					dynamic.add(new VarInsnNode(Opcodes.ALOAD, array));
					dynamic.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ACCESSOR_BRIDGE, "invoke",
							"(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/Object;", false));
					Type returned = Type.getReturnType(call.desc);
					if (returned.getSort() == Type.VOID) {
						dynamic.add(new InsnNode(Opcodes.POP));
					} else {
						unboxTo(dynamic, returned);
					}
					method.instructions.insertBefore(call, dynamic);
					method.instructions.remove(call);
					method.maxStack += 6;
					this.redirected.incrementAndGet();
					changed = true;
					continue;
				}
				int[] slots = new int[args.length];
				var spill = new InsnList();
				for (int i = args.length - 1; i >= 0; i--) {
					slots[i] = method.maxLocals;
					method.maxLocals += args[i].getSize();
					spill.add(new VarInsnNode(args[i].getOpcode(Opcodes.ISTORE), slots[i]));
				}
				spill.add(new TypeInsnNode(Opcodes.CHECKCAST, this.receiverType(facade, call.name, call.desc, currentClass)));
				for (int i = 0; i < args.length; i++) {
					spill.add(new VarInsnNode(args[i].getOpcode(Opcodes.ILOAD), slots[i]));
				}
				method.instructions.insertBefore(call, spill);
				call.owner = facade;
				this.redirected.incrementAndGet();
				changed = true;
				continue;
			}
			// a write of a static field the 1.12.2 class lacks: the shadow's, as the reads (a mod widens a 1.21 map and
			// sets it: GlitchCore's AxeItem.STRIPPABLES = new HashMap(...) was skipped, the put that followed threw)
			if (insn instanceof FieldInsnNode write && write.getOpcode() == Opcodes.PUTSTATIC && !this.hasMember(write.owner, write.name, write.desc, false)) {
				String shadow = this.shadowWith(write.owner, write.name, write.desc, false);
				if (shadow != null) {
					write.owner = shadow;
					this.redirected.incrementAndGet();
					changed = true;
					continue;
				}
			}
			// (and a write to an instance field the 1.12.2 class lacks: its setter helper, as a read goes to its getter; writes
			// were never looked at and were dropped: Particle.friction set in the constructor of every 1.21 particle, BOP's
			// leaves and the vanilla particles of the carried 1.21 code kept 1.12.2's)
			if (insn instanceof FieldInsnNode field
					&& (field.getOpcode() == Opcodes.GETSTATIC || field.getOpcode() == Opcodes.GETFIELD || field.getOpcode() == Opcodes.PUTFIELD)
					&& !this.hasMember(field.owner, field.name, field.desc, false)) {
				if (this.registryByKey(method, field)) {
					changed = true;
					continue;
				}
				if (field.getOpcode() == Opcodes.GETSTATIC && "net/minecraft/init/SoundEvents".equals(field.owner)
						&& "Lnet/minecraft/util/SoundEvent;".equals(field.desc)) {
					// a 1.21 sound 1.12.2 doesn't have: the 1.12.2 sound of the same kind, never null
					var replacement = new InsnList();
					replacement.add(new LdcInsnNode(field.name));
					replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/runtime/redirect/SoundFallback", "byName",
							"(Ljava/lang/String;)Lnet/minecraft/util/SoundEvent;", false));
					method.instructions.insertBefore(field, replacement);
					method.instructions.remove(field);
					this.redirected.incrementAndGet();
					changed = true;
					continue;
				}
				if (field.getOpcode() == Opcodes.GETSTATIC && "Lio/github/xponer2/translator/registry/ResourceKey;".equals(field.desc)
						&& ("net/minecraft/init/Biomes".equals(field.owner) || "net/minecraft/world/storage/loot/LootTableList".equals(field.owner))) {
					// a vanilla key 1.12.2 has no field for (Biomes.PLAINS, BuiltInLootTables.SIMPLE_DUNGEON): made from the name
					var replacement = new InsnList();
					replacement.add(new LdcInsnNode(field.owner));
					replacement.add(new LdcInsnNode(field.name));
					replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/registry/VanillaKeys", "of",
							"(Ljava/lang/String;Ljava/lang/String;)Lio/github/xponer2/translator/registry/ResourceKey;", false));
					method.instructions.insertBefore(field, replacement);
					method.instructions.remove(field);
					this.redirected.incrementAndGet();
					changed = true;
					continue;
				}
				if (field.getOpcode() == Opcodes.GETSTATIC && "io/github/xponer2/translator/registry/EntityType".equals(field.owner)
						&& "Lio/github/xponer2/translator/registry/EntityType;".equals(field.desc)) {
					// a vanilla entity type (EntityType.ZOMBIE): the 1.12.2 one of that id, found when the code runs
					var replacement = new InsnList();
					replacement.add(new LdcInsnNode(field.name));
					replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, field.owner, "vanilla", "(Ljava/lang/String;)" + field.desc, false));
					method.instructions.insertBefore(field, replacement);
					method.instructions.remove(field);
					this.redirected.incrementAndGet();
					changed = true;
					continue;
				}
				if (field.getOpcode() == Opcodes.GETSTATIC && "net/minecraft/init/SoundEvents".equals(field.owner)
						&& ("Lio/github/xponer2/translator/registry/Holder;".equals(field.desc)
								|| "Lio/github/xponer2/translator/registry/Registry$Reference;".equals(field.desc))) {
					// a 1.21 sound held as a holder (SoundEvents.UI_BUTTON_CLICK): the 1.12.2 sound of that name or kind, held
					var replacement = new InsnList();
					if (this.hasMember(field.owner, field.name, "Lnet/minecraft/util/SoundEvent;", false)) {
						replacement.add(new FieldInsnNode(Opcodes.GETSTATIC, field.owner, field.name, "Lnet/minecraft/util/SoundEvent;"));
					} else {
						replacement.add(new LdcInsnNode(field.name));
						replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/runtime/redirect/SoundFallback", "byName",
								"(Ljava/lang/String;)Lnet/minecraft/util/SoundEvent;", false));
					}
					replacement.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/runtime/redirect/SoundFallback", "reference",
							"(Lnet/minecraft/util/SoundEvent;)Lio/github/xponer2/translator/registry/Registry$Reference;", false));
					method.instructions.insertBefore(field, replacement);
					method.instructions.remove(field);
					this.redirected.incrementAndGet();
					changed = true;
					continue;
				}
				// a static field the 1.12.2 class lacks: the same field of a helper class registered for it, before the
				// carried 1.21 class (as for methods): the carried Stats makes its stat types for 1.21 registries and
				// leaves Stats.BLOCK_MINED null, the Translator's StatType gives the 1.12.2 statistics
				String helper = field.getOpcode() == Opcodes.GETSTATIC ? this.fieldHelper(field.owner, field.name, field.desc) : null;
				if (helper != null) {
					field.owner = helper;
					this.redirected.incrementAndGet();
					changed = true;
					continue;
				}
				// CODEC / STREAM_CODEC of a vanilla enum 1.12.2 declares without them (Direction, DyeColor, Rotation, ...): a
				// real codec of its constants (EnumCodecs), not the carried class's, which can't make one for a 1.12.2 enum
				if (this.enumCodec(method, field)) {
					changed = true;
					continue;
				}
				String shadow = field.getOpcode() == Opcodes.GETSTATIC ? this.shadowWith(field.owner, field.name, field.desc, false) : null;
				if (shadow != null) {
					field.owner = shadow;
					this.redirected.incrementAndGet();
					changed = true;
					continue;
				}
				// 1.12.2 declares the field with a narrower type (Blocks.WATER is a BlockStaticLiquid): read it as that
				String actual = this.fieldDesc(field.owner, field.name);
				if (actual != null && field.getOpcode() != Opcodes.PUTFIELD && this.isSubtype(actual, field.desc)) {
					field.desc = actual;
					this.redirected.incrementAndGet();
					changed = true;
					continue;
				}
				// a field typed by a type variable with a wider bound (T menu erased to Container), or a 1.12.2 field of the
				// class the Translator's facade extends (HumanoidModel.head, a ModelPart, is ModelBiped's ModelRenderer
				// bipedHead): read it, cast it back; a write needs no cast
				boolean facade = field.desc.startsWith("Lio/github/xponer2/translator/");
				// or an instance field 1.21 declares with a narrower game type than 1.12.2 (Particle.level is a ClientLevel,
				// 1.12.2's world a World): without this every particle tick that read it was left empty (BOP's leaves and
				// drips never moved nor aged)
				boolean narrowed = field.desc.startsWith("Lnet/minecraft/") && (field.getOpcode() == Opcodes.GETFIELD || field.getOpcode() == Opcodes.PUTFIELD);
				if (actual != null && this.isSubtype(field.desc, actual) && (facade || narrowed || this.isTypeVariable(field.owner, field.name + " " + actual, false))) {
					if (field.getOpcode() == Opcodes.PUTFIELD || field.getOpcode() == Opcodes.PUTSTATIC) {
						field.desc = actual;
						this.redirected.incrementAndGet();
						changed = true;
						continue;
					}
					String expected = field.desc;
					field.desc = actual;
					if ("Lio/github/xponer2/translator/client/model/ModelPart;".equals(expected)) {
						// a vanilla model's part is a plain ModelRenderer: a stand-in with its pose, not a failed cast
						method.instructions.insert(field, new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/client/model/ModelPart", "of",
								"(Lnet/minecraft/client/model/ModelRenderer;)Lio/github/xponer2/translator/client/model/ModelPart;", false));
					} else if (this.adapter(expected, actual) != null) {
						// a Translator type with an adapter of the 1.12.2 one (DispenseItemBehavior.of(IBehaviorDispenseItem)):
						// the 1.12.2 value adapted, not a failed cast (Artifacts' umbrella read ArmorItem.DISPENSE_ITEM_BEHAVIOR)
						method.instructions.insert(field, new MethodInsnNode(Opcodes.INVOKESTATIC, Type.getType(expected).getInternalName(), "of",
								this.adapter(expected, actual), this.isInterfaceType(Type.getType(expected).getInternalName())));
					} else {
						method.instructions.insert(field, new TypeInsnNode(Opcodes.CHECKCAST, Type.getType(expected).getInternalName()));
					}
					this.redirected.incrementAndGet();
					changed = true;
					continue;
				}
				if (field.getOpcode() == Opcodes.GETFIELD) {
					// an instance field the 1.12.2 class lacks (player.containerMenu): a static getter of the same name
					String[] getter = this.findRedirect(field.owner, field.name, "()" + field.desc, false);
					if (getter != null) {
						var read = new MethodInsnNode(Opcodes.INVOKESTATIC, getter[0], field.name, getter[1], false);
						method.instructions.set(field, read);
						if (getter.length > 2 && getter[2] != null) {
							method.instructions.insert(read, new TypeInsnNode(Opcodes.CHECKCAST, getter[2]));
						}
						this.redirected.incrementAndGet();
						changed = true;
					}
					continue;
				}
				if (field.getOpcode() == Opcodes.PUTFIELD) {
					// a write to such a field (gui.leftHeight += 10): a static setter of the same name taking the value
					String[] setter = this.findRedirect(field.owner, field.name, "(" + field.desc + ")V", false);
					if (setter != null && (setter.length < 3 || setter[2] == null)) {
						method.instructions.set(field, new MethodInsnNode(Opcodes.INVOKESTATIC, setter[0], field.name, setter[1], false));
						this.redirected.incrementAndGet();
						changed = true;
					}
					continue;
				}
				continue;
			}
			if (insn instanceof MethodInsnNode init && "<init>".equals(init.name) && init.getOpcode() == Opcodes.INVOKESPECIAL
					&& !init.owner.startsWith("[") && !this.hasMember(init.owner, init.name, init.desc, true)) {
				changed |= this.redirectConstructor(method, init) || this.adaptErasure(method, init);
				continue;
			}
			if (insn instanceof InvokeDynamicInsnNode indy) {
				// an X line for a method reference too (ClientboundBossEventPacket::createUpdateProgressPacket): the helper,
				// as a call gets it. Iron's Spells' boss bars sent the packet of the reference's own body, null, and the server
				// crashed sending it (EnumConnectionState.getFromPacket)
				for (int i = 0; i < indy.bsmArgs.length && !this.forced.isEmpty(); i++) {
					if (indy.bsmArgs[i] instanceof Handle ref && (ref.getTag() == Opcodes.H_INVOKESTATIC || ref.getTag() == Opcodes.H_INVOKEVIRTUAL
							|| ref.getTag() == Opcodes.H_INVOKEINTERFACE)) {
						String helper = this.forced.get(ref.getOwner() + "." + ref.getName() + ref.getDesc());
						if (helper != null) {
							String desc = ref.getTag() == Opcodes.H_INVOKESTATIC ? ref.getDesc() : "(L" + ref.getOwner() + ";" + ref.getDesc().substring(1);
							int space = helper.indexOf(' ');
							if (space > 0) {
								desc = helper.substring(space + 1);
								helper = helper.substring(0, space);
							}
							indy.bsmArgs[i] = new Handle(Opcodes.H_INVOKESTATIC, helper, ref.getName(), desc, false);
							this.redirected.incrementAndGet();
							changed = true;
						}
					}
				}
				// a method reference (Blocks.AIR::defaultBlockState) of a missing method: a reference to the stand-in
				for (int i = 0; i < indy.bsmArgs.length; i++) {
					if (indy.bsmArgs[i] instanceof Handle ctor && ctor.getTag() == Opcodes.H_NEWINVOKESPECIAL
							&& !this.hasMember(ctor.getOwner(), ctor.getName(), ctor.getDesc(), true)) {
						// a constructor reference (ChunkPos::new) of a constructor 1.12.2 lacks: the helper's newChunkPos
						String simple = ctor.getOwner().substring(ctor.getOwner().lastIndexOf('/') + 1).replace('$', '_');
						String desc = ctor.getDesc().substring(0, ctor.getDesc().indexOf(')') + 1) + "L" + ctor.getOwner() + ";";
						for (String helper : this.redirects.getOrDefault(ctor.getOwner(), java.util.Collections.emptyList())) {
							if (this.hasMember(helper, "new" + simple, desc, true)) {
								indy.bsmArgs[i] = new Handle(Opcodes.H_INVOKESTATIC, helper, "new" + simple, desc, false);
								this.redirected.incrementAndGet();
								changed = true;
								break;
							}
						}
						continue;
					}
					if (i == 1 && indy.bsmArgs[i] instanceof Handle jdk && isPlatform(jdk.getOwner())
							&& (jdk.getTag() == Opcodes.H_INVOKEVIRTUAL || jdk.getTag() == Opcodes.H_INVOKEINTERFACE)
							&& "java/lang/invoke/LambdaMetafactory".equals(indy.bsm.getOwner()) && indy.bsmArgs[2] instanceof Type instantiated) {
						// a method reference to a JDK method of a 1.21 class that was a JDK type (list::add of a ListTag is
						// AbstractList.add): 1.12.2's class (NBTTagList) isn't one, so the reference is to its helper's stand-in
						Type[] captured = Type.getArgumentTypes(indy.desc);
						Type[] params = instantiated.getArgumentTypes();
						Type receiver = captured.length > 0 ? captured[0] : params.length > 0 ? params[0] : null;
						if (receiver != null && receiver.getSort() == Type.OBJECT && !isPlatform(receiver.getInternalName())
								&& !this.extendsPlatform(receiver.getInternalName(), jdk.getOwner())) {
							String[] found = this.findRedirect(receiver.getInternalName(), jdk.getName(), jdk.getDesc(), false);
							if (found != null && (found.length < 3 || found[2] == null)) {
								indy.bsmArgs[i] = new Handle(Opcodes.H_INVOKESTATIC, found[0], jdk.getName(), found[1], false);
								this.redirected.incrementAndGet();
								changed = true;
							}
						}
						continue;
					}
					if (indy.bsmArgs[i] instanceof Handle handle && handle.getTag() >= Opcodes.H_INVOKEVIRTUAL && handle.getTag() != Opcodes.H_INVOKESPECIAL
							&& handle.getTag() != Opcodes.H_NEWINVOKESPECIAL && !this.hasMember(handle.getOwner(), handle.getName(), handle.getDesc(), true)) {
						String[] found = this.findRedirect(handle.getOwner(), handle.getName(), handle.getDesc(), handle.getTag() == Opcodes.H_INVOKESTATIC);
						if (found != null && found.length > 2 && found[2] != null) {
							// a method reference adapts no narrowed result: not this stand-in
							found = null;
						}
						String shadow = found == null && handle.getTag() == Opcodes.H_INVOKESTATIC
								? this.shadowWith(handle.getOwner(), handle.getName(), handle.getDesc(), true) : null;
						if (found != null) {
							indy.bsmArgs[i] = new Handle(Opcodes.H_INVOKESTATIC, found[0], handle.getName(), found[1], false);
							this.redirected.incrementAndGet();
							changed = true;
						} else if (shadow != null) {
							indy.bsmArgs[i] = new Handle(Opcodes.H_INVOKESTATIC, shadow, handle.getName(), handle.getDesc(), false);
							this.redirected.incrementAndGet();
							changed = true;
						} else {
							// a method reference to a method that takes wider types now (event::registerEntityRenderer)
							String wider = this.widerErasure(handle.getOwner(), handle.getName(), handle.getDesc());
							if (wider != null) {
								indy.bsmArgs[i] = new Handle(handle.getTag(), handle.getOwner(), handle.getName(), wider, handle.isInterface());
								this.redirected.incrementAndGet();
								changed = true;
							}
						}
					}
				}
				changed |= this.fixLambdaInterfaceType(indy);
				continue;
			}
			if (!(insn instanceof MethodInsnNode call) || call.owner.startsWith("[") || this.hasMember(call.owner, call.name, call.desc, true)) {
				continue;
			}
			String[] found = this.findRedirect(call.owner, call.name, call.desc, call.getOpcode() == Opcodes.INVOKESTATIC);
			if (found != null) {
				var redirect = new MethodInsnNode(Opcodes.INVOKESTATIC, found[0], call.name, found[1], false);
				method.instructions.set(call, redirect);
				if (found.length > 2 && found[2] != null) {
					method.instructions.insert(redirect, new TypeInsnNode(Opcodes.CHECKCAST, found[2]));
				}
				this.redirected.incrementAndGet();
				changed = true;
				continue;
			}
			String shadow = call.getOpcode() == Opcodes.INVOKESTATIC ? this.shadowWith(call.owner, call.name, call.desc, true) : null;
			if (shadow != null) {
				call.owner = shadow;
				call.itf = false;
				this.redirected.incrementAndGet();
				changed = true;
				continue;
			}
			// a default method a newer library added to an interface the game has an older version of (fastutil 8
			// Object2IntMap.getOrDefault on the game's fastutil 7): its static bridge in the library gap's shadow
			String[] bridge = call.getOpcode() == Opcodes.INVOKEVIRTUAL || call.getOpcode() == Opcodes.INVOKEINTERFACE
					? this.defaultBridge(call.owner, call.name, call.desc) : null;
			if (bridge != null) {
				method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, bridge[0], call.name, bridge[1], false));
				this.redirected.incrementAndGet();
				changed = true;
				continue;
			}
			// a record-style accessor (Property.name()) where the 1.12.2 class has the getter (getName(), isEmpty())
			if ((call.getOpcode() == Opcodes.INVOKEVIRTUAL || call.getOpcode() == Opcodes.INVOKEINTERFACE) && call.desc.startsWith("()")
					&& !call.name.isEmpty() && Character.isLowerCase(call.name.charAt(0))) {
				String capitalized = Character.toUpperCase(call.name.charAt(0)) + call.name.substring(1);
				String getter = this.hasMember(call.owner, "get" + capitalized, call.desc, true) ? "get" + capitalized
						: "()Z".equals(call.desc) && this.hasMember(call.owner, "is" + capitalized, call.desc, true) ? "is" + capitalized : null;
				if (getter != null) {
					call.name = getter;
					this.redirected.incrementAndGet();
					changed = true;
					continue;
				}
			}
			// a record-style accessor (Vec3.x()) where the 1.12.2 class has the public field (Vec3d.x): the field is read
			if (call.getOpcode() == Opcodes.INVOKEVIRTUAL && call.desc.startsWith("()")
					&& this.hasPublicInstanceField(call.owner, call.name + " " + Type.getReturnType(call.desc).getDescriptor())) {
				method.instructions.set(call, new FieldInsnNode(Opcodes.GETFIELD, call.owner, call.name, Type.getReturnType(call.desc).getDescriptor()));
				this.redirected.incrementAndGet();
				changed = true;
				continue;
			}
			if (this.adaptErasure(method, call)) {
				changed = true;
				continue;
			}
			// f(x) where only f(x...) exists (1.21 single-argument overloads of varargs methods): wrap x in an array
			Type[] args = Type.getArgumentTypes(call.desc);
			if (args.length > 0 && args[args.length - 1].getSort() == Type.OBJECT) {
				Type last = args[args.length - 1];
				String varargs = call.desc.substring(0, call.desc.lastIndexOf(last.getDescriptor())) + "[" + last.getDescriptor()
						+ call.desc.substring(call.desc.indexOf(')'));
				if (this.hasMember(call.owner, call.name, varargs, true)) {
					var wrap = new InsnList();
					wrap.add(new InsnNode(Opcodes.ICONST_1));
					wrap.add(new TypeInsnNode(Opcodes.ANEWARRAY, last.getInternalName()));
					wrap.add(new InsnNode(Opcodes.DUP_X1));
					wrap.add(new InsnNode(Opcodes.SWAP));
					wrap.add(new InsnNode(Opcodes.ICONST_0));
					wrap.add(new InsnNode(Opcodes.SWAP));
					wrap.add(new InsnNode(Opcodes.AASTORE));
					method.instructions.insertBefore(call, wrap);
					call.desc = varargs;
					this.redirected.incrementAndGet();
					changed = true;
				}
			}
		}
		changed |= alignCapturedTypes(method);
		return changed;
	}

	/**
	 * Java 8's LambdaMetafactory wants each captured value's type to be the implementation's parameter type exactly
	 * (later JDKs take a subtype): a bound method reference redirected to a static helper ({@code this::setChanged} to
	 * MiscBridges.setChanged(TileEntity)) captured the mod's class and threw "Type mismatch in captured lambda parameter"
	 * (a placed backpack's data packet failed). The call site then captures the helper's type (the value is one).
	 */
	private static boolean alignCapturedTypes(MethodNode method) {
		boolean changed = false;
		for (AbstractInsnNode insn : method.instructions) {
			if (!(insn instanceof InvokeDynamicInsnNode indy) || !"java/lang/invoke/LambdaMetafactory".equals(indy.bsm.getOwner())
					|| indy.bsmArgs.length < 2 || !(indy.bsmArgs[1] instanceof Handle impl)) {
				continue;
			}
			Type[] captured = Type.getArgumentTypes(indy.desc);
			if (captured.length == 0) {
				continue;
			}
			var implTypes = new java.util.ArrayList<Type>();
			if (impl.getTag() == Opcodes.H_INVOKEVIRTUAL || impl.getTag() == Opcodes.H_INVOKEINTERFACE || impl.getTag() == Opcodes.H_INVOKESPECIAL) {
				implTypes.add(Type.getObjectType(impl.getOwner()));
			}
			if (impl.getTag() != Opcodes.H_NEWINVOKESPECIAL) {
				implTypes.addAll(java.util.Arrays.asList(Type.getArgumentTypes(impl.getDesc())));
			}
			boolean differs = false;
			for (int i = 0; i < captured.length && i < implTypes.size(); i++) {
				Type want = implTypes.get(i);
				if (captured[i].getSort() == Type.OBJECT && want.getSort() == Type.OBJECT && !captured[i].equals(want)
						&& impl.getTag() == Opcodes.H_INVOKESTATIC) {
					captured[i] = want;
					differs = true;
				}
			}
			if (differs) {
				indy.desc = Type.getMethodDescriptor(Type.getReturnType(indy.desc), captured);
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * The static bridge of an interface default method (ApiCarry.shadow with defaults) for a call on the owner or a
	 * subtype of the interface: {shadow class, its descriptor}, or null.
	 */
	private String[] defaultBridge(String owner, String name, String desc) {
		var todo = new java.util.ArrayDeque<String>();
		var seen = new java.util.HashSet<String>();
		todo.add(owner);
		while (!todo.isEmpty()) {
			String current = todo.poll();
			if (!seen.add(current) || isPlatform(current)) {
				continue;
			}
			ClassInfo shadow = this.info(ApiCarry.SHADOW + current);
			String bridgeDesc = "(L" + current + ";" + desc.substring(1);
			if (shadow != null && shadow.methods.contains(name + " " + bridgeDesc)) {
				return new String[] {ApiCarry.SHADOW + current, bridgeDesc};
			}
			ClassInfo info = this.info(current);
			if (info != null) {
				todo.addAll(info.supertypes());
			}
		}
		return null;
	}

	/** The helper class registered for the owner that declares a static field of that name and type, or null. */
	private String fieldHelper(String owner, String name, String desc) {
		// the helpers of the class and of its superclasses (ServerLevel.OVERWORLD is Level's: WorldServer, then World)
		var seen = new java.util.HashSet<String>();
		for (String current = owner; current != null && seen.add(current); ) {
			for (String helper : this.redirects.getOrDefault(current, java.util.Collections.emptyList())) {
				ClassInfo info = this.info(helper);
				if (info != null && info.fields.contains(name + " " + desc)) {
					return helper;
				}
			}
			ClassInfo info = this.info(current);
			current = info == null ? null : info.superName;
		}
		return null;
	}

	/**
	 * The static stand-in for a method the class lacks: {helper class, its descriptor}, or null. Instance methods get
	 * the receiver as the first parameter, typed as the class (or supertype) the helper is registered for.
	 */
	private String[] findRedirect(String callOwner, String name, String callDesc, boolean isStatic) {
		var todo = new java.util.ArrayDeque<String>();
		var seen = new java.util.HashSet<String>();
		todo.add(callOwner);
		todo.add("*");
		while (!todo.isEmpty()) {
			String owner = todo.poll();
			if (!seen.add(owner)) {
				continue;
			}
			String desc = isStatic ? callDesc : "(L" + ("*".equals(owner) ? callOwner : owner) + ";" + callDesc.substring(1);
			for (String helper : this.redirects.getOrDefault(owner, java.util.Collections.emptyList())) {
				if (this.hasMember(helper, name, desc, true)) {
					return new String[] {helper, desc};
				}
			}
			for (String helper : this.redirects.getOrDefault(owner, java.util.Collections.emptyList())) {
				String loose = this.looseRedirect(helper, name, desc);
				if (loose != null) {
					Type wanted = Type.getReturnType(desc);
					return new String[] {helper, loose, Type.getReturnType(loose).equals(wanted) ? null : wanted.getInternalName()};
				}
			}
			ClassInfo info = "*".equals(owner) ? null : this.info(owner);
			if (info != null) {
				todo.addAll(info.supertypes());
			}
		}
		return null;
	}

	/**
	 * A helper method of that name whose parameters take the call's arguments (the same types or wider: a 1.21 type the
	 * Translator has no compile-time class for is an Object parameter, {@code getHeight(World, Object types, int, int)})
	 * and whose result is the call's or a supertype of it (cast back by the caller): its descriptor, or null.
	 */
	private String looseRedirect(String helper, String name, String desc) {
		ClassInfo info = this.info(helper);
		if (info == null) {
			return null;
		}
		Type[] args = Type.getArgumentTypes(desc);
		Type returned = Type.getReturnType(desc);
		for (String member : info.methods) {
			if (!member.startsWith(name + " ")) {
				continue;
			}
			String candidate = member.substring(name.length() + 1);
			Type[] params = Type.getArgumentTypes(candidate);
			Type result = Type.getReturnType(candidate);
			boolean fits = params.length == args.length
					&& (result.equals(returned) || returned.getSort() >= Type.ARRAY && this.assignable(returned, result));
			for (int i = 0; i < params.length && fits; i++) {
				fits = this.assignable(args[i], params[i]);
			}
			if (fits) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * A call whose descriptor is another erasure of the same method: generic parameters erase to their bounds, and
	 * the Translator's bounds are 1.12.2 classes ({@code Screen<T extends Container>(T menu, ..)}) where 1.21 has its
	 * own ({@code AbstractContainerMenu}). The method whose parameters are the same or wider is called; a result that
	 * is a wider type variable is cast back to what the mod expects.
	 */
	private boolean adaptErasure(MethodNode method, MethodInsnNode call) {
		Type[] args = Type.getArgumentTypes(call.desc);
		Type returned = Type.getReturnType(call.desc);
		var todo = new java.util.ArrayDeque<String>();
		var seen = new java.util.HashSet<String>();
		todo.add(call.owner);
		while (!todo.isEmpty()) {
			String current = todo.poll();
			ClassInfo info = !seen.add(current) || isPlatform(current) ? null : this.info(current);
			if (info == null) {
				continue;
			}
			for (String member : info.methods) {
				if (!member.startsWith(call.name + " ")) {
					continue;
				}
				String desc = member.substring(call.name.length() + 1);
				Type[] params = Type.getArgumentTypes(desc);
				if (params.length != args.length) {
					continue;
				}
				boolean wider = true;
				for (int i = 0; i < params.length && wider; i++) {
					wider = this.assignable(args[i], params[i]);
				}
				if (!wider) {
					continue;
				}
				Type result = Type.getReturnType(desc);
				boolean cast = false;
				if (!this.assignable(result, returned)) {
					// T getMenu() erased to Container where the mod expects its bound, or a Translator method that gives
					// the 1.12.2 class where the mod expects the Translator's facade of it (MenuProvider.createMenu gives
					// a Container, Powah expects an AbstractContainerMenu): cast back
					boolean facade = returned.getSort() == Type.OBJECT && returned.getInternalName().startsWith("io/github/xponer2/translator/");
					if (!(info.typeVariables.contains(member) || facade) || !this.assignable(returned, result)) {
						continue;
					}
					cast = true;
				}
				call.desc = desc;
				if (cast) {
					method.instructions.insert(call, new TypeInsnNode(Opcodes.CHECKCAST, returned.getInternalName()));
				}
				this.redirected.incrementAndGet();
				return true;
			}
			if (!"<init>".equals(call.name)) {
				todo.addAll(info.supertypes());
			}
		}
		return false;
	}

	/** The descriptor of a method of the class (or a supertype) of that name with wider parameters and the same result. */
	private String widerErasure(String owner, String name, String desc) {
		Type[] args = Type.getArgumentTypes(desc);
		Type returned = Type.getReturnType(desc);
		var todo = new java.util.ArrayDeque<String>();
		var seen = new java.util.HashSet<String>();
		todo.add(owner);
		while (!todo.isEmpty()) {
			String current = todo.poll();
			ClassInfo info = !seen.add(current) || isPlatform(current) ? null : this.info(current);
			if (info == null) {
				continue;
			}
			for (String member : info.methods) {
				if (!member.startsWith(name + " ")) {
					continue;
				}
				String candidate = member.substring(name.length() + 1);
				Type[] params = Type.getArgumentTypes(candidate);
				boolean fits = params.length == args.length && Type.getReturnType(candidate).equals(returned);
				for (int i = 0; i < params.length && fits; i++) {
					fits = this.assignable(args[i], params[i]);
				}
				if (fits) {
					return candidate;
				}
			}
			todo.addAll(info.supertypes());
		}
		return null;
	}

	/** A value of type {@code from} can be passed as {@code to}: the same type, or an object type and its supertype. */
	private boolean assignable(Type from, Type to) {
		if (from.equals(to)) {
			return true;
		}
		// an array of objects is an Object, and an Object[] of a wider element type (FluidType[] as Object[])
		if (from.getSort() == Type.ARRAY && to.getSort() == Type.OBJECT) {
			return "java/lang/Object".equals(to.getInternalName());
		}
		if (from.getSort() == Type.ARRAY && to.getSort() == Type.ARRAY) {
			return from.getDimensions() == to.getDimensions() && this.assignable(from.getElementType(), to.getElementType());
		}
		if (from.getSort() != Type.OBJECT || to.getSort() != Type.OBJECT) {
			return false;
		}
		return "java/lang/Object".equals(to.getInternalName()) || this.isSubtype(from.getDescriptor(), to.getDescriptor());
	}

	/** Whether the member (field or method "name desc") of the class or a supertype is typed by a type variable. */
	private boolean isTypeVariable(String owner, String member, boolean method) {
		var todo = new java.util.ArrayDeque<String>();
		var seen = new java.util.HashSet<String>();
		todo.add(owner);
		while (!todo.isEmpty()) {
			String current = todo.poll();
			ClassInfo info = !seen.add(current) || isPlatform(current) ? null : this.info(current);
			if (info == null) {
				continue;
			}
			if ((method ? info.methods : info.fields).contains(member)) {
				return info.typeVariables.contains(member);
			}
			todo.addAll(info.supertypes());
		}
		return false;
	}

	/** The descriptor of a field of the class or a supertype by name, or null. */
	private String fieldDesc(String owner, String name) {
		var todo = new java.util.ArrayDeque<String>();
		var seen = new java.util.HashSet<String>();
		todo.add(owner);
		while (!todo.isEmpty()) {
			String current = todo.poll();
			ClassInfo info = !seen.add(current) || isPlatform(current) ? null : this.info(current);
			if (info == null) {
				continue;
			}
			for (String field : info.fields) {
				if (field.startsWith(name + " ")) {
					return field.substring(name.length() + 1);
				}
			}
			todo.addAll(info.supertypes());
		}
		return null;
	}

	/** Whether a value of type {@code desc} is a {@code expected} (object types; same or a subclass / implementation). */
	private boolean isSubtype(String desc, String expected) {
		if (!desc.startsWith("L") || !expected.startsWith("L")) {
			return false;
		}
		String target = expected.substring(1, expected.length() - 1);
		var todo = new java.util.ArrayDeque<String>();
		var seen = new java.util.HashSet<String>();
		todo.add(desc.substring(1, desc.length() - 1));
		while (!todo.isEmpty()) {
			String current = todo.poll();
			if (current.equals(target)) {
				return true;
			}
			ClassInfo info = !seen.add(current) || isPlatform(current) ? null : this.info(current);
			if (info != null) {
				todo.addAll(info.supertypes());
			}
		}
		return false;
	}

	/** Whether the class is the JDK class or interface {@code platform} (through its own supertypes and the JDK's). */
	private boolean extendsPlatform(String name, String platform) {
		Class<?> wanted = platformClass(platform);
		var todo = new java.util.ArrayDeque<String>();
		var seen = new java.util.HashSet<String>();
		todo.add(name);
		while (!todo.isEmpty()) {
			String current = todo.poll();
			if (!seen.add(current)) {
				continue;
			}
			if (isPlatform(current)) {
				Class<?> found = platformClass(current);
				if (current.equals(platform) || wanted != null && found != null && wanted.isAssignableFrom(found)) {
					return true;
				}
				continue;
			}
			ClassInfo info = this.info(current);
			if (info == null) {
				// a class the converter doesn't know: taken as one, the reference stays as it is
				return true;
			}
			todo.addAll(info.supertypes());
		}
		return false;
	}

	/**
	 * {@code new C(args)} with a constructor C lacks: {@code NEW C; DUP; args; INVOKESPECIAL C.<init>} becomes
	 * {@code args; INVOKESTATIC helper.newC(args)} when a helper registered for C has that static factory.
	 */
	private boolean redirectConstructor(MethodNode method, MethodInsnNode init) {
		String simple = init.owner.substring(init.owner.lastIndexOf('/') + 1).replace('$', '_');
		String desc = init.desc.substring(0, init.desc.indexOf(')') + 1) + "L" + init.owner + ";";
		String helper = null;
		for (String candidate : this.redirects.getOrDefault(init.owner, java.util.Collections.emptyList())) {
			if (this.hasMember(candidate, "new" + simple, desc, true)) {
				helper = candidate;
				break;
			}
		}
		if (helper == null) {
			// a factory taking wider parameters (Object for a class the Translator can't name: fastutil 8's
			// BooleanConsumer of 1.21 ConfirmScreen, a carried 1.21 type): its own descriptor
			Type[] args = Type.getArgumentTypes(init.desc);
			for (String candidate : this.redirects.getOrDefault(init.owner, java.util.Collections.emptyList())) {
				ClassInfo info = this.info(candidate);
				for (String member : info == null ? java.util.Collections.<String>emptySet() : info.methods) {
					if (!member.startsWith("new" + simple + " ")) {
						continue;
					}
					String candidateDesc = member.substring(member.indexOf(' ') + 1);
					Type[] params = Type.getArgumentTypes(candidateDesc);
					if (params.length != args.length || !Type.getReturnType(candidateDesc).getDescriptor().equals("L" + init.owner + ";")) {
						continue;
					}
					boolean fits = true;
					for (int i = 0; i < params.length && fits; i++) {
						fits = this.assignable(args[i], params[i]);
					}
					if (fits) {
						helper = candidate;
						desc = candidateDesc;
						break;
					}
				}
				if (helper != null) {
					break;
				}
			}
		}
		if (helper == null) {
			return false;
		}
		TypeInsnNode type = findNew(init);
		if (type == null || !(type.getNext() instanceof InsnNode dup) || dup.getOpcode() != Opcodes.DUP) {
			return false;
		}
		// NEW C; DUP become null; null (the stack map frames in between can name them), the factory's object replaces both
		nullNew(method, type);
		var factory = new MethodInsnNode(Opcodes.INVOKESTATIC, helper, "new" + simple, desc, false);
		method.instructions.set(init, factory);
		var after = new InsnList();
		after.add(new InsnNode(Opcodes.DUP_X2));
		after.add(new InsnNode(Opcodes.POP));
		after.add(new InsnNode(Opcodes.POP2));
		method.instructions.insert(factory, after);
		this.redirected.incrementAndGet();
		return true;
	}

	/** The NEW of a constructor call: the nearest one before it that isn't closed by a later <init> of the class. */
	private static TypeInsnNode findNew(MethodInsnNode init) {
		int open = 0;
		for (AbstractInsnNode insn = init.getPrevious(); insn != null; insn = insn.getPrevious()) {
			if (insn instanceof MethodInsnNode other && other.getOpcode() == Opcodes.INVOKESPECIAL && "<init>".equals(other.name)
					&& other.owner.equals(init.owner)) {
				open++;
			} else if (insn instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW && type.desc.equals(init.owner) && open-- == 0) {
				return type;
			}
		}
		return null;
	}

	/**
	 * NEW C becomes null. The stack map frames name an uninitialized object by the label of its NEW: they name null
	 * instead, or the JVM rejects the method (a VerifyError, "bad offset for Uninitialized").
	 */
	private static void nullNew(MethodNode method, TypeInsnNode type) {
		var labels = new java.util.HashSet<LabelNode>();
		for (AbstractInsnNode insn = type.getPrevious(); insn != null && insn.getOpcode() < 0; insn = insn.getPrevious()) {
			if (insn instanceof LabelNode label) {
				labels.add(label);
			}
		}
		if (!labels.isEmpty()) {
			for (AbstractInsnNode insn : method.instructions) {
				if (insn instanceof FrameNode frame) {
					for (List<Object> types : java.util.Arrays.asList(frame.local, frame.stack)) {
						if (types != null) {
							types.replaceAll(t -> t instanceof LabelNode && labels.contains(t) ? Opcodes.NULL : t);
						}
					}
				}
			}
		}
		method.instructions.set(type, new InsnNode(Opcodes.ACONST_NULL));
	}

	/**
	 * A 1.21 class can be a 1.12.2 interface (BlockState -> IBlockState) and the other way round: the call
	 * instruction has to match what the owner is now.
	 */
	private boolean fixInvokeKinds(MethodNode method) {
		boolean changed = false;
		for (AbstractInsnNode insn : method.instructions) {
			if (insn instanceof InvokeDynamicInsnNode indy) {
				// method references too (IBlockState::getBlock is an interface method now)
				for (int i = 0; i < indy.bsmArgs.length; i++) {
					if (!(indy.bsmArgs[i] instanceof Handle handle) || handle.getOwner().startsWith("[")) {
						continue;
					}
					Boolean handleItf = this.isInterface(handle.getOwner());
					int tag = handle.getTag();
					if (handleItf == null) {
						continue;
					}
					if (tag == Opcodes.H_INVOKEVIRTUAL && handleItf) {
						tag = Opcodes.H_INVOKEINTERFACE;
					} else if (tag == Opcodes.H_INVOKEINTERFACE && !handleItf) {
						tag = Opcodes.H_INVOKEVIRTUAL;
					}
					if (tag != handle.getTag() || (tag == Opcodes.H_INVOKESTATIC || tag == Opcodes.H_INVOKESPECIAL) && handle.isInterface() != handleItf) {
						indy.bsmArgs[i] = new Handle(tag, handle.getOwner(), handle.getName(), handle.getDesc(),
								tag == Opcodes.H_INVOKEINTERFACE || (tag == Opcodes.H_INVOKESTATIC || tag == Opcodes.H_INVOKESPECIAL) && handleItf);
						changed = true;
					}
				}
				continue;
			}
			if (!(insn instanceof MethodInsnNode call) || call.owner.startsWith("[")) {
				continue;
			}
			Boolean itf = this.isInterface(call.owner);
			if (itf == null) {
				continue;
			}
			if (call.getOpcode() == Opcodes.INVOKEVIRTUAL && itf) {
				call.setOpcode(Opcodes.INVOKEINTERFACE);
				call.itf = true;
				changed = true;
			} else if (call.getOpcode() == Opcodes.INVOKEINTERFACE && !itf) {
				call.setOpcode(Opcodes.INVOKEVIRTUAL);
				call.itf = false;
				changed = true;
			} else if ((call.getOpcode() == Opcodes.INVOKESTATIC || call.getOpcode() == Opcodes.INVOKESPECIAL) && call.itf != itf) {
				call.itf = itf;
				changed = true;
			}
		}
		return changed;
	}

	private Boolean isInterface(String name) {
		if (isPlatform(name)) {
			Class<?> c = platformClass(name);
			return c == null ? null : c.isInterface();
		}
		ClassInfo info = this.info(name);
		return info == null ? null : info.isInterface();
	}

	/**
	 * In a static initializer: a missing static field reads as null / 0, a missing call drops its arguments and gives
	 * null / 0, a missing class's object is null. The class loads; the fields that needed them stay empty.
	 */
	private boolean soften(String owner, MethodNode method) {
		return this.soften(owner, method, false);
	}

	/**
	 * {@link #soften(String, MethodNode)} for any method; with {@code report} each skipped site tells
	 * {@link RuntimeStubs#missing} when it runs (the log and the self-test see it like a stubbed method).
	 */
	private boolean soften(String owner, MethodNode method, boolean report) {
		boolean changed = false;
		String where = owner.replace('/', '.') + "." + method.name;
		for (AbstractInsnNode insn : method.instructions.toArray()) {
			var replacement = new InsnList();
			String what = null;
			if (insn instanceof FieldInsnNode field && !this.hasMember(field.owner, field.name, field.desc, false)) {
				what = field.owner + "." + field.name;
				Type type = Type.getType(field.desc);
				switch (field.getOpcode()) {
					case Opcodes.GETSTATIC -> replacement.add(defaultValue(type));
					case Opcodes.PUTSTATIC -> replacement.add(new InsnNode(type.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
					case Opcodes.GETFIELD -> {
						replacement.add(new InsnNode(Opcodes.POP));
						replacement.add(defaultValue(type));
					}
					default -> {
						replacement.add(new InsnNode(type.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
						replacement.add(new InsnNode(Opcodes.POP));
					}
				}
			} else if (insn instanceof MethodInsnNode call && !call.owner.startsWith("[") && !this.hasMember(call.owner, call.name, call.desc, true)) {
				what = call.owner + "." + call.name;
				if ("<init>".equals(call.name)) {
					// new C(args) without that constructor: the object is null (the NEW of a missing class is null already)
					TypeInsnNode created = findNew(call);
					if (created != null) {
						nullNew(method, created);
					}
				}
				Type[] args = Type.getArgumentTypes(call.desc);
				for (int i = args.length - 1; i >= 0; i--) {
					replacement.add(new InsnNode(args[i].getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
				}
				if (call.getOpcode() != Opcodes.INVOKESTATIC) {
					replacement.add(new InsnNode(Opcodes.POP));
				}
				Type returned = Type.getReturnType(call.desc);
				if (returned.getSort() != Type.VOID) {
					replacement.add(defaultValue(returned));
				}
			} else if (insn instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW && !this.exists(type.desc)) {
				if (report) {
					method.instructions.insertBefore(insn, reportMissing(where + " (no class " + type.desc + ", skipped)"));
				}
				nullNew(method, type);
				this.stubbed.add(owner + "." + method.name + ": " + type.desc + " missing, left empty");
				changed = true;
				continue;
			} else if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Type constant && constant.getSort() == Type.OBJECT
					&& !this.exists(constant.getInternalName())) {
				// the class of a missing class (ListenerTypes adds the event class of bus.addListener(Compat::onEvent),
				// an optional mod's event: Twilight Forest's CosArmorDeathDrops): null, the bus ignores that listener
				what = "class " + constant.getInternalName();
				replacement.add(new InsnNode(Opcodes.ACONST_NULL));
			} else if (insn instanceof TypeInsnNode type && !this.exists(type.desc)) {
				what = type.desc;
				switch (type.getOpcode()) {
					case Opcodes.ANEWARRAY -> {
						replacement.add(new InsnNode(Opcodes.POP));
						replacement.add(new InsnNode(Opcodes.ACONST_NULL));
					}
					case Opcodes.INSTANCEOF -> {
						replacement.add(new InsnNode(Opcodes.POP));
						replacement.add(new InsnNode(Opcodes.ICONST_0));
					}
					default -> replacement.add(new InsnNode(Opcodes.NOP));
				}
			}
			if (insn instanceof InvokeDynamicInsnNode indy) {
				// a lambda or method reference of a missing method (Entity::getType): the function is null
				for (Object arg : indy.bsmArgs) {
					if (arg instanceof Handle handle && handle.getTag() >= Opcodes.H_INVOKEVIRTUAL
							&& !this.hasMember(handle.getOwner(), handle.getName(), handle.getDesc(), true)) {
						what = handle.getOwner() + "." + handle.getName();
					}
					// a reference whose types include a class that isn't there (a compat method of an optional mod:
					// Twilight Forest's CosmeticArmorCompat::keepCosmeticArmor behind isLoaded): the function is null,
					// the rest of the method (a mod's constructor) runs
					String absent = arg instanceof Handle handle ? this.absentClass(handle.getDesc())
							: arg instanceof Type type && type.getSort() == Type.METHOD ? this.absentClass(type.getDescriptor()) : null;
					if (what == null && absent != null) {
						what = absent;
					}
				}
				if (what != null) {
					Type[] args = Type.getArgumentTypes(indy.desc);
					for (int i = args.length - 1; i >= 0; i--) {
						replacement.add(new InsnNode(args[i].getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
					}
					replacement.add(new InsnNode(Opcodes.ACONST_NULL));
				}
			}
			if (what != null) {
				if (report) {
					method.instructions.insertBefore(insn, reportMissing(where + " (no " + what + ", skipped)"));
				}
				method.instructions.insertBefore(insn, replacement);
				method.instructions.remove(insn);
				this.stubbed.add(owner + "." + method.name + ": " + what + " missing, left empty");
				changed = true;
			}
		}
		return changed;
	}

	/**
	 * What a stubbed method gives or a missing field reads as: 0 / false, an empty Optional, Stream or collection (a
	 * caller goes on with nothing rather than a NullPointerException), else null.
	 */
	private static InsnList defaultValue(Type type) {
		var list = new InsnList();
		if (type.getSort() == Type.OBJECT) {
			String name = type.getInternalName();
			switch (name) {
				case "java/util/Optional", "java/util/OptionalInt", "java/util/OptionalLong", "java/util/OptionalDouble" -> {
					list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, name, "empty", "()L" + name + ";", false));
					return list;
				}
				case "java/util/stream/Stream", "java/util/stream/IntStream", "java/util/stream/LongStream", "java/util/stream/DoubleStream" -> {
					list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, name, "empty", "()L" + name + ";", true));
					return list;
				}
				case "java/util/List", "java/util/Collection", "java/lang/Iterable", "java/util/ArrayList" -> {
					return newInstance(list, "java/util/ArrayList");
				}
				case "java/util/Set", "java/util/HashSet" -> {
					return newInstance(list, "java/util/HashSet");
				}
				case "java/util/Map", "java/util/HashMap" -> {
					return newInstance(list, "java/util/HashMap");
				}
				default -> {
				}
			}
		}
		list.add(new InsnNode(switch (type.getSort()) {
			case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> Opcodes.ICONST_0;
			case Type.LONG -> Opcodes.LCONST_0;
			case Type.FLOAT -> Opcodes.FCONST_0;
			case Type.DOUBLE -> Opcodes.DCONST_0;
			default -> Opcodes.ACONST_NULL;
		}));
		return list;
	}

	private static InsnList newInstance(InsnList list, String type) {
		list.add(new TypeInsnNode(Opcodes.NEW, type));
		list.add(new InsnNode(Opcodes.DUP));
		list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, type, "<init>", "()V", false));
		return list;
	}

	/** A class of a method descriptor that doesn't exist (arrays by their element), or null. */
	@javax.annotation.Nullable
	private String absentClass(String descriptor) {
		var types = new java.util.ArrayList<Type>(java.util.Arrays.asList(Type.getArgumentTypes(descriptor)));
		types.add(Type.getReturnType(descriptor));
		for (Type type : types) {
			Type element = type.getSort() == Type.ARRAY ? type.getElementType() : type;
			if (element.getSort() == Type.OBJECT && !this.exists(element.getInternalName())) {
				return element.getInternalName();
			}
		}
		return null;
	}

	/** The first class or member the method uses that doesn't exist, or null. */
	private String missing(MethodNode method) {
		// all of them (the report is the work list; one at a time would take one game start per gap)
		var reasons = new java.util.LinkedHashSet<String>();
		for (AbstractInsnNode insn : method.instructions) {
			if (insn instanceof MethodInsnNode call) {
				if (!this.hasMember(call.owner, call.name, call.desc, true)) {
					reasons.add("no method " + call.owner + "." + call.name + call.desc);
				}
			} else if (insn instanceof FieldInsnNode field) {
				if (!this.hasMember(field.owner, field.name, field.desc, false)) {
					reasons.add("no field " + field.owner + "." + field.name + ":" + field.desc);
				}
			} else if (insn instanceof TypeInsnNode type) {
				if (!this.exists(type.desc)) {
					reasons.add("no class " + type.desc);
				}
			} else if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof Type type && type.getSort() == Type.OBJECT) {
				if (!this.exists(type.getInternalName())) {
					reasons.add("no class " + type.getInternalName());
				}
			} else if (insn instanceof InvokeDynamicInsnNode indy) {
				for (Object arg : indy.bsmArgs) {
					if (arg instanceof Handle handle && handle.getTag() >= Opcodes.H_INVOKEVIRTUAL
							&& !this.hasMember(handle.getOwner(), handle.getName(), handle.getDesc(), true)) {
						reasons.add("no method " + handle.getOwner() + "." + handle.getName() + handle.getDesc());
					}
				}
			}
		}
		return reasons.isEmpty() ? null : String.join("; ", reasons);
	}

	private String verify(String owner, MethodNode method) {
		// the redirects may need a deeper stack than the method declared (the ClassWriter computes the real maximum when
		// the class is written); checked with room, or a fine method counts as broken and is stubbed
		int maxStack = method.maxStack;
		method.maxStack = maxStack + 16;
		var verifier = new Verifier();
		try {
			new Analyzer<>(verifier).analyze(owner, method);
			return null;
		} catch (AnalyzerException | RuntimeException e) {
			return "bytecode doesn't verify: " + e.getMessage() + (verifier.mismatch != null ? " (" + verifier.mismatch + ")" : "");
		} finally {
			method.maxStack = maxStack;
		}
	}

	/** Whether a stack map frame of the method names a class that doesn't exist (locals or stack). */
	private boolean framesNameMissing(MethodNode method) {
		for (AbstractInsnNode insn : method.instructions) {
			if (insn instanceof FrameNode frame) {
				for (List<Object> types : java.util.Arrays.asList(frame.local, frame.stack)) {
					if (types == null) {
						continue;
					}
					for (Object type : types) {
						if (type instanceof String name) {
							String element = name.startsWith("[") ? Type.getType(name).getElementType().getSort() == Type.OBJECT
									? Type.getType(name).getElementType().getInternalName() : null : name;
							if (element != null && !this.exists(element)) {
								return true;
							}
						}
					}
				}
			}
		}
		return false;
	}

	/** {@code RuntimeStubs.missing(message)}: logs once and tells the self-test (no effect on the stack). */
	private static InsnList reportMissing(String message) {
		var list = new InsnList();
		list.add(new LdcInsnNode(message.length() > 1000 ? message.substring(0, 1000) + "..." : message));
		list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/runtime/RuntimeStubs", "missing", "(Ljava/lang/String;)V", false));
		return list;
	}

	private static MethodNode copy(MethodNode method) {
		var copy = new MethodNode(Opcodes.ASM9, method.access, method.name, method.desc, method.signature,
				method.exceptions == null ? null : method.exceptions.toArray(new String[0]));
		method.accept(copy);
		return copy;
	}

	/**
	 * Whether a method with missing members may run without them (soften) instead of being stubbed whole: one missing
	 * call in a registration method would cost every block it registers. Not an override of a method the 1.12.2 /
	 * facade super class has (its stub calls that, the safer behaviour), not a constructor whose own super() / this()
	 * call is missing (the object can't be made without it).
	 */
	private boolean canSoften(ClassNode node, MethodNode method) {
		boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
		if ("<init>".equals(method.name)) {
			for (AbstractInsnNode insn : method.instructions) {
				if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESPECIAL && "<init>".equals(call.name)
						&& (call.owner.equals(node.superName) || call.owner.equals(node.name))
						&& !this.hasMember(call.owner, call.name, call.desc, true)) {
					return false;
				}
			}
			return true;
		}
		return isStatic || node.superName == null || !this.hasConcreteMember(node.superName, method.name, method.desc);
	}

	/**
	 * A softened method that still throws (what ran without its missing parts went wrong later: a null where the mod
	 * expects an object, a Java 9 class in a lambda) gives what its stub gave: the error is logged once and the method
	 * returns null / 0 / false. What it did before the error stays done (the entries it registered). Not for
	 * constructors (a handler can't cover the super() call) and static initializers (they have their own guard).
	 */
	private static void guardSoftened(String owner, MethodNode method) {
		boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
		Type[] args = Type.getArgumentTypes(method.desc);
		Type returned = Type.getReturnType(method.desc);
		var locals = new ArrayList<Object>();
		if (!isStatic) {
			locals.add(owner);
		}
		for (Type arg : args) {
			locals.add(switch (arg.getSort()) {
				case Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT -> Opcodes.INTEGER;
				case Type.LONG -> Opcodes.LONG;
				case Type.FLOAT -> Opcodes.FLOAT;
				case Type.DOUBLE -> Opcodes.DOUBLE;
				case Type.ARRAY -> arg.getDescriptor();
				default -> arg.getInternalName();
			});
		}
		var start = new LabelNode();
		var end = new LabelNode();
		var handler = new LabelNode();
		method.instructions.insert(start);
		method.instructions.add(end);
		method.instructions.add(handler);
		method.instructions.add(new FrameNode(Opcodes.F_FULL, locals.size(), locals.toArray(), 1, new Object[] { "java/lang/Throwable" }));
		method.instructions.add(new LdcInsnNode(owner.replace('/', '.') + "." + method.name));
		method.instructions.add(new InsnNode(Opcodes.SWAP));
		method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/runtime/RuntimeStubs", "failed",
				"(Ljava/lang/String;Ljava/lang/Throwable;)V", false));
		if (returned.getSort() == Type.VOID) {
			method.instructions.add(new InsnNode(Opcodes.RETURN));
		} else {
			method.instructions.add(defaultValue(returned));
			method.instructions.add(new InsnNode(returned.getOpcode(Opcodes.IRETURN)));
		}
		// after the method's own handlers: they still catch what the mod catches itself
		method.tryCatchBlocks.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.TryCatchBlockNode(start, end, handler, "java/lang/RuntimeException"));
		method.tryCatchBlocks.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.TryCatchBlockNode(start, end, handler, "java/lang/LinkageError"));
	}

	/**
	 * The new body of a method that can't run: an override calls the super method (the 1.12.2 / facade behaviour); any
	 * other method logs once and returns null / 0 / false; a constructor throws (its object can't be made).
	 */
	private void stub(String owner, String superName, MethodNode method, String reason) {
		this.stubbed.add(owner + "." + method.name + method.desc + ": " + reason);
		// the full reason is in the report; the constant in the class stays far below the 64 KB of a class file string
		String shortReason = reason.length() > 1000 ? reason.substring(0, 1000) + "..." : reason;
		String message = owner.replace('/', '.') + "." + method.name + " (" + shortReason + ")";
		var body = new InsnList();
		boolean isStatic = (method.access & Opcodes.ACC_STATIC) != 0;
		Type returned = Type.getReturnType(method.desc);
		if ("<init>".equals(method.name)) {
			body.add(new TypeInsnNode(Opcodes.NEW, "java/lang/UnsupportedOperationException"));
			body.add(new InsnNode(Opcodes.DUP));
			body.add(new LdcInsnNode("Translator: " + message + " can't run on 1.12.2 yet"));
			body.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "(Ljava/lang/String;)V", false));
			body.add(new InsnNode(Opcodes.ATHROW));
		} else {
			body.add(new LdcInsnNode(message));
			body.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/runtime/RuntimeStubs", "missing", "(Ljava/lang/String;)V", false));
			if (!isStatic && superName != null && this.hasConcreteMember(superName, method.name, method.desc)) {
				body.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode(Opcodes.ALOAD, 0));
				int slot = 1;
				for (Type arg : Type.getArgumentTypes(method.desc)) {
					body.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode(arg.getOpcode(Opcodes.ILOAD), slot));
					slot += arg.getSize();
				}
				Boolean itf = this.isInterface(superName);
				body.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, superName, method.name, method.desc, itf != null && itf));
				body.add(new InsnNode(returned.getOpcode(Opcodes.IRETURN)));
			} else if (returned.getSort() == Type.VOID) {
				body.add(new InsnNode(Opcodes.RETURN));
			} else {
				body.add(defaultValue(returned));
				body.add(new InsnNode(returned.getOpcode(Opcodes.IRETURN)));
			}
		}
		method.instructions = body;
		method.tryCatchBlocks = new ArrayList<>();
		method.localVariables = null;
		method.visibleLocalVariableAnnotations = null;
		method.invisibleLocalVariableAnnotations = null;
	}

	/** A non-abstract method of that name and descriptor in the class or its superclasses (what super calls reach). */
	private boolean hasConcreteMember(String owner, String name, String desc) {
		for (String current = owner; current != null; ) {
			if (isPlatform(current)) {
				return platformHasMember(current, name, desc, true);
			}
			ClassInfo info = this.info(current);
			if (info == null) {
				return false;
			}
			if (info.methods.contains(name + " " + desc) && !info.abstractMethods.contains(name + " " + desc)) {
				return true;
			}
			current = info.superName;
		}
		return false;
	}

	// --- the class hierarchy: the mod, then 1.12.2 / Forge / Translator, then the JDK ----------------------------------

	private static final String ACCESSOR_BRIDGE = "io/github/xponer2/translator/runtime/AccessorBridge";

	/** A value of that type on the stack, boxed (an object stays as it is). */
	private static void boxFrom(InsnList out, Type type) {
		String box = switch (type.getSort()) {
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
		if (box != null) {
			out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, box, "valueOf", "(" + type.getDescriptor() + ")L" + box + ";", false));
		}
	}

	/** An Object on the stack as that type: cast, or unboxed (null reads as 0 / false). */
	private static void unboxTo(InsnList out, Type type) {
		String unbox = switch (type.getSort()) {
			case Type.BOOLEAN -> "booleanValue";
			case Type.BYTE -> "byteValue";
			case Type.CHAR -> "charValue";
			case Type.SHORT -> "shortValue";
			case Type.INT -> "intValue";
			case Type.LONG -> "longValue";
			case Type.FLOAT -> "floatValue";
			case Type.DOUBLE -> "doubleValue";
			default -> null;
		};
		if (unbox != null) {
			out.add(new MethodInsnNode(Opcodes.INVOKESTATIC, ACCESSOR_BRIDGE, unbox, "(Ljava/lang/Object;)" + type.getDescriptor(), false));
		} else {
			out.add(new TypeInsnNode(Opcodes.CHECKCAST, type.getSort() == Type.ARRAY ? type.getDescriptor() : type.getInternalName()));
		}
	}

	/**
	 * The type a facade member's receiver is cast to: the facade, or the current class when the member is protected in
	 * the facade (or a superclass of it) and the current class extends the facade. Another package reaches a protected
	 * member only through its own class's type, javac made sure of that in the original code, and a cast to the facade
	 * failed verification: "Bad access to protected data" in JourneyMap's screens (this.renderables), so JourneyMap's
	 * client never started.
	 */
	private String receiverType(String facade, String name, String desc, String currentClass) {
		if (currentClass == null || currentClass.equals(facade)) {
			return facade;
		}
		String member = name + " " + desc;
		for (String owner = facade; owner != null; ) {
			ClassInfo info = this.info(owner);
			if (info == null) {
				break;
			}
			if (info.protectedMembers.contains(member)) {
				return this.extendsClass(currentClass, facade) ? currentClass : facade;
			}
			if (info.fields.contains(member) || info.methods.contains(member)) {
				break;
			}
			owner = info.superName;
		}
		return facade;
	}

	/** The 1.12.2 names of an entity's tick methods a mod's entity overrides (see isTick). */
	private static final java.util.Set<String> TICKS = com.google.common.collect.ImmutableSet.of("onUpdate", "onLivingUpdate", "updateAITasks",
			"onEntityUpdate");

	/** The tick the world calls on a mod's entity ({@code onUpdate}) or block entity ({@code update}). */
	private boolean isTick(ClassNode node, MethodNode method) {
		if (!"()V".equals(method.desc) || (method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT)) != 0 || !this.mod.containsKey(node.name)) {
			return false;
		}
		// an entity's tick and the parts of it a mob overrides on their own (1.21 aiStep, customServerAiStep, baseTick:
		// Iron's Spells' priest threw in its AI step, which no onUpdate of the mod wraps, and the server crashed)
		return TICKS.contains(method.name) ? this.extendsClass(node.name, "net/minecraft/entity/Entity")
				: "update".equals(method.name) && this.extendsClass(node.name, "net/minecraft/tileentity/TileEntity");
	}

	/** Whether the class is {@code wanted} or a subclass of it (the mod's classes first). */
	private boolean extendsClass(String name, String wanted) {
		for (String current = name; current != null; ) {
			if (current.equals(wanted)) {
				return true;
			}
			ClassInfo info = this.info(current);
			if (info == null) {
				return false;
			}
			current = info.superName;
		}
		return false;
	}

	private ClassInfo info(String name) {
		ClassInfo info = this.mod.get(name);
		return info != null ? info : this.target.get(name);
	}

	private boolean exists(String name) {
		if (name.startsWith("[")) {
			Type element = Type.getType(name).getElementType();
			return element.getSort() != Type.OBJECT || this.exists(element.getInternalName());
		}
		return isPlatform(name) ? platformClass(name) != null : this.info(name) != null;
	}

	private boolean hasMember(String owner, String name, String desc, boolean method) {
		if (owner.startsWith("[")) {
			return true;
		}
		if (isPlatform(owner)) {
			if (this.redirects.containsKey(owner)) {
				// a JDK class a 1.21 class is mapped to (RandomSource is java.util.Random): its 1.21 members
				// (RandomSource.create, nextIntBetweenInclusive) are missing there and go to its helpers
				return platformHasMember(owner, name, desc, method);
			}
			// the JDK members are the downgrader's business (it stubs the Java 9+ API)
			return platformClass(owner) != null || owner.startsWith("java/lang/invoke/");
		}
		if (method && "<init>".equals(name)) {
			// constructors aren't inherited: only the class itself (Object.<init>()V made every missing no-argument
			// constructor look present: NeoForge's BaseFlowingFluid calls Fluid.<init>() 1.12.2 lacks, a
			// NoSuchMethodError at run time that took Registrate's whole item registration down)
			ClassInfo info = this.info(owner);
			return info != null && info.methods.contains(name + " " + desc);
		}
		var seen = new java.util.HashSet<String>();
		var todo = new java.util.ArrayDeque<String>();
		todo.add(owner);
		while (!todo.isEmpty()) {
			String current = todo.poll();
			if (!seen.add(current)) {
				continue;
			}
			if (isPlatform(current)) {
				if (platformHasMember(current, name, desc, method)) {
					return true;
				}
				continue;
			}
			ClassInfo info = this.info(current);
			if (info == null) {
				return false;
			}
			if (method ? info.methods.contains(name + " " + desc) : info.fields.contains(name + " " + desc)) {
				return true;
			}
			todo.addAll(info.supertypes());
		}
		return false;
	}

	private static boolean isPlatform(String name) {
		return name.startsWith("java/") || name.startsWith("javax/") || name.startsWith("sun/") || name.startsWith("jdk/");
	}

	private static Class<?> platformClass(String name) {
		try {
			return Class.forName(name.replace('/', '.'), false, ClassLoader.getSystemClassLoader());
		} catch (ClassNotFoundException | LinkageError e) {
			return null;
		}
	}

	private static boolean platformHasMember(String owner, String name, String desc, boolean method) {
		Class<?> type = platformClass(owner);
		if (type == null) {
			return false;
		}
		try {
			if (!method) {
				for (Class<?> c = type; c != null; c = c.getSuperclass()) {
					for (var field : c.getDeclaredFields()) {
						if (field.getName().equals(name)) {
							return true;
						}
					}
				}
				return false;
			}
			for (Class<?> c = type; c != null; c = c.getSuperclass()) {
				for (var m : c.getDeclaredMethods()) {
					if (m.getName().equals(name) && Type.getMethodDescriptor(m).equals(desc)) {
						return true;
					}
				}
				for (var ctor : c.getDeclaredConstructors()) {
					if ("<init>".equals(name) && Type.getConstructorDescriptor(ctor).equals(desc)) {
						return true;
					}
				}
			}
			for (Class<?> i : type.getInterfaces()) {
				if (platformHasMember(Type.getInternalName(i), name, desc, true)) {
					return true;
				}
			}
			return false;
		} catch (LinkageError | SecurityException e) {
			return true;
		}
	}

	/** SimpleVerifier on the converted class hierarchy, without loading any class. */
	/** The descriptor of {@code expected.of(x)} taking the 1.12.2 type (or a supertype of it), null when it has none. */
	@javax.annotation.Nullable
	private String adapter(String expected, String actual) {
		String owner = Type.getType(expected).getInternalName();
		ClassInfo info = this.info(owner);
		if (info == null) {
			return null;
		}
		for (String member : info.methods) {
			if (member.startsWith("of (") && member.endsWith(")" + expected)) {
				Type[] params = Type.getArgumentTypes(member.substring(3));
				if (params.length == 1 && this.isSubtype(actual, params[0].getDescriptor())) {
					return member.substring(3);
				}
			}
		}
		return null;
	}

	private boolean isInterfaceType(String name) {
		ClassInfo info = this.info(name);
		return info != null && info.isInterface();
	}

	/** Object, Cloneable or Serializable: the supertypes of every array. */
	private static boolean isArraySupertype(Type type) {
		String name = type.getInternalName();
		return "java/lang/Object".equals(name) || "java/lang/Cloneable".equals(name) || "java/io/Serializable".equals(name);
	}

	private final class Verifier extends SimpleVerifier {
		Verifier() {
			super(Opcodes.ASM9, null, null, null, false);
		}

		@Override
		protected boolean isInterface(Type type) {
			if (isPlatform(type.getInternalName())) {
				Class<?> c = platformClass(type.getInternalName());
				return c != null && c.isInterface();
			}
			ClassInfo info = MethodGuard.this.info(type.getInternalName());
			return info != null && info.isInterface();
		}

		@Override
		protected Type getSuperClass(Type type) {
			String name = type.getInternalName();
			if (isPlatform(name)) {
				Class<?> c = platformClass(name);
				return c == null || c.getSuperclass() == null ? null : Type.getType(c.getSuperclass());
			}
			ClassInfo info = MethodGuard.this.info(name);
			return info == null || info.superName == null ? null : Type.getObjectType(info.superName);
		}

		@Override
		protected boolean isAssignableFrom(Type type, Type other) {
			if (type.equals(other) || "java/lang/Object".equals(type.getInternalName())) {
				return true;
			}
			if (type.getSort() == Type.ARRAY || other.getSort() == Type.ARRAY) {
				if (type.getSort() != Type.ARRAY) {
					// an array is an Object, a Cloneable and a Serializable
					return other.getSort() == Type.ARRAY && isArraySupertype(type);
				}
				if (other.getSort() != Type.ARRAY) {
					return false;
				}
				Type a = type.getElementType();
				Type b = other.getElementType();
				if (type.getDimensions() == other.getDimensions()) {
					return a.getSort() == Type.OBJECT && b.getSort() == Type.OBJECT && this.isAssignableFrom(a, b) || a.equals(b);
				}
				// int[][] is an Object[] (its elements, int[], are Objects); never the other way
				return other.getDimensions() > type.getDimensions() && a.getSort() == Type.OBJECT && isArraySupertype(a);
			}
			if (this.isInterface(type)) {
				// the JVM verifier lets any object stand for an interface type
				return true;
			}
			for (Type current = other; current != null; current = this.getSuperClass(current)) {
				if (current.equals(type)) {
					return true;
				}
			}
			return false;
		}

		/** The last value that wasn't of the expected class type: told with the error (with where its chain stopped). */
		String mismatch;

		/**
		 * Whether the value is of the expected type, by the class hierarchy the guard reads (never by loading classes).
		 * Only an answer: the basic verifier also asks it to choose (BALOAD: a boolean[] or else a byte[]).
		 */
		@Override
		protected boolean isSubTypeOf(xyz.wagyourtail.jvmdg.shade.asm.tree.analysis.BasicValue value,
				xyz.wagyourtail.jvmdg.shade.asm.tree.analysis.BasicValue expected) {
			Type expectedType = expected.getType();
			Type type = value.getType();
			if (expectedType != null && type != null && (expectedType.getSort() == Type.OBJECT || expectedType.getSort() == Type.ARRAY)
					&& (type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY)) {
				if ("null".equals(type.getInternalName()) || this.isAssignableFrom(expectedType, type)) {
					return true;
				}
				if (expectedType.getSort() == Type.OBJECT || expectedType.getElementType().getSort() == Type.OBJECT) {
					this.mismatch = type + " isn't a " + expectedType + this.unreadable(type);
				}
				return false;
			}
			return super.isSubTypeOf(value, expected);
		}

		private String unreadable(Type type) {
			Type element = type.getSort() == Type.ARRAY ? type.getElementType() : type;
			for (Type current = element; current != null && current.getSort() == Type.OBJECT; ) {
				String name = current.getInternalName();
				if (!isPlatform(name) && MethodGuard.this.info(name) == null) {
					return " (" + name + " can't be read)";
				}
				current = this.getSuperClass(current);
			}
			return "";
		}

		@Override
		protected Class<?> getClass(Type type) {
			throw new IllegalStateException("not loading " + type);
		}
	}
}
