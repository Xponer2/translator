/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.core;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import xyz.wagyourtail.jvmdg.shade.asm.ClassReader;
import xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.analysis.Analyzer;
import xyz.wagyourtail.jvmdg.shade.asm.tree.analysis.AnalyzerException;
import xyz.wagyourtail.jvmdg.shade.asm.tree.analysis.BasicVerifier;

/**
 * Mixin's class transformer, checked: a Minecraft class that a mixin of a 1.21.1 mod made invalid (a converted
 * handler that reads a local it doesn't have) would stop the game when it loads (VerifyError in EntityPlayer). The
 * class Mixin gives back is verified; if it doesn't verify, the class loads as it was before Mixin (without the
 * mixins into it) and the problem is logged.
 */
final class SafeMixins implements IClassTransformer {
	private static final Logger LOGGER = LogManager.getLogger("Translator Runtime");
	private static final String PROXY = "org.spongepowered.asm.mixin.transformer.Proxy";
	private final IClassTransformer mixin;
	private static boolean reported;

	private SafeMixins(IClassTransformer mixin) {
		this.mixin = mixin;
	}

	/** Wraps Mixin's transformer in the class loader's list; false when Mixin hasn't registered it (yet). */
	@SuppressWarnings("unchecked")
	static boolean install() {
		try {
			var field = LaunchClassLoader.class.getDeclaredField("transformers");
			field.setAccessible(true);
			var transformers = (List<IClassTransformer>) field.get(Launch.classLoader);
			int wrapped = 0;
			var names = new java.util.ArrayList<String>();
			for (int i = 0; i < transformers.size(); i++) {
				IClassTransformer transformer = transformers.get(i);
				names.add(transformer.getClass().getName());
				if (transformer.getClass().getName().equals(PROXY)) {
					transformers.set(i, new SafeMixins(transformer));
					wrapped++;
				}
			}
			if (wrapped > 0) {
				LOGGER.info("Mixin's transformer is checked now (mixins that break a class are left out), transformers: {}", names);
				return true;
			}
			if (names.contains(SafeMixins.class.getName())) {
				return true;
			}
			if (!reported) {
				reported = true;
				LOGGER.info("Mixin's transformer isn't registered yet, transformers: {}", names);
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			LOGGER.warn("Can't check Mixin's classes: {}", e.toString());
		}
		return false;
	}

	/**
	 * The coremod's transformer: wraps Mixin's as soon as it is in the list (whenever Mixin registers it), then does
	 * nothing. Replacing a list element doesn't disturb the class loader going through the list.
	 */
	public static final class Installer implements IClassTransformer {
		// installing loads classes, which come through here again
		private static final ThreadLocal<Boolean> BUSY = new ThreadLocal<>();

		@Override
		public byte[] transform(String name, String transformedName, byte[] basicClass) {
			// every time: Mixin registers a new transformer later (after the deobfuscation) and retires the old one
			if (BUSY.get() == null) {
				BUSY.set(Boolean.TRUE);
				try {
					synchronized (Installer.class) {
						install();
					}
				} finally {
					BUSY.remove();
				}
			}
			return basicClass;
		}
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		byte[] out;
		try {
			out = this.mixin.transform(name, transformedName, basicClass);
		} catch (Throwable t) {
			// a critical injection failure (a callback whose captured locals don't match on 1.12.2): Mixin gives up on
			// the class; it loads without the mixins instead of not at all
			if (basicClass == null || !transformedName.startsWith("net.minecraft.")) {
				throw t;
			}
			LOGGER.error("Mixins into {} left out: Mixin failed on it ({})", transformedName, t.getCause() != null ? t.getCause() : t);
			return basicClass;
		}
		if (out == null || out == basicClass || basicClass == null) {
			return out;
		}
		String problem = verify(out);
		if (problem == null) {
			problem = unresolvedShadows(basicClass, out);
		}
		if (problem == null) {
			return out;
		}
		// the members the mixins of one mod need and 1.12.2 lacks are given defaults, so the other mixins into the class
		// (other mods') stay: only if that doesn't make the class load are they all left out
		byte[] repaired = MixinRepair.repair(basicClass, out, transformedName);
		if (repaired != null) {
			String left = verify(repaired);
			if (left == null) {
				left = unresolvedShadows(basicClass, repaired);
			}
			if (left == null) {
				LOGGER.warn("Mixins into {}: {} (repaired: what 1.12.2 lacks does nothing there)", transformedName, problem);
				return repaired;
			}
			problem = problem + "; after repair: " + left;
		}
		// a callback of one mod that doesn't verify (Chat Heads' in the client's packet listener): that callback does
		// nothing, the class keeps every other mixin. Leaving all out lost Xaero's map session (its maps drew other
		// worlds' chunks and lagged) along with Chat Heads'
		byte[] emptied = emptyBrokenCallbacks(repaired != null ? repaired : out, transformedName);
		if (emptied != null) {
			String left = verify(emptied);
			if (left == null) {
				left = unresolvedShadows(basicClass, emptied);
			}
			if (left == null) {
				LOGGER.warn("Mixins into {}: {} (that callback does nothing, the other mixins stay)", transformedName, problem);
				return emptied;
			}
			problem = problem + "; with the broken callbacks emptied: " + left;
		}
		LOGGER.error("Mixins into {} left out: with them the class doesn't load ({})", transformedName, problem);
		return basicClass;
	}

	/**
	 * The class with each @Inject callback Mixin merged into it ({@code handler$...}, returning void) that doesn't verify
	 * made empty (it returns at once); null if there is none.
	 */
	static byte[] emptyBrokenCallbacks(byte[] bytes, String className) {
		var node = new ClassNode();
		try {
			new ClassReader(bytes).accept(node, 0);
		} catch (RuntimeException e) {
			return null;
		}
		boolean changed = false;
		for (MethodNode method : node.methods) {
			if (method.instructions.size() == 0) {
				continue;
			}
			var args = xyz.wagyourtail.jvmdg.shade.asm.Type.getArgumentTypes(method.desc);
			var returned = xyz.wagyourtail.jvmdg.shade.asm.Type.getReturnType(method.desc);
			boolean callback = method.name.startsWith("handler$") && method.desc.endsWith(")V");
			// a value modifier (@ModifyArg, @ModifyVariable, @ModifyConstant, MixinExtras' @ModifyExpressionValue and
			// @ModifyReturnValue) takes the value first and returns it: a broken one gives the value back unchanged.
			// Chat Heads' modify handler in the client's packet listener didn't verify, and every mixin of that class
			// was left out with it: Xaero's maps lost their world and drew the chunks of other worlds
			boolean value = valueModifier(method.name) && args.length > 0 && args[0].equals(returned);
			// MixinExtras' @WrapOperation: the wrapped call's receiver and arguments, then the Operation: a broken one
			// makes the original call (Accessories' took every mixin of GuiContainer down, Supplementaries' BlockFire's)
			int operation = -1;
			if (!callback && !value && method.name.startsWith("wrapOperation$")) {
				for (int i = 0; i < args.length; i++) {
					if (OPERATION.equals(args[i].getDescriptor())) {
						operation = i;
						break;
					}
				}
			}
			if (!callback && !value && operation < 0) {
				continue;
			}
			try {
				new Analyzer<>(new BasicVerifier()).analyze(node.name, method);
			} catch (AnalyzerException | RuntimeException e) {
				method.instructions.clear();
				method.tryCatchBlocks.clear();
				method.localVariables = null;
				method.visibleLocalVariableAnnotations = null;
				method.invisibleLocalVariableAnnotations = null;
				if (callback) {
					method.instructions.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.InsnNode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.RETURN));
				} else if (operation >= 0) {
					callOriginal(method, args, returned, operation);
				} else {
					int slot = (method.access & xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_STATIC) != 0 ? 0 : 1;
					method.instructions.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode(args[0].getOpcode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ILOAD), slot));
					method.instructions.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.InsnNode(returned.getOpcode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.IRETURN)));
				}
				changed = true;
				LOGGER.warn("Mixin {} {}.{} doesn't verify on 1.12.2 ({}): {}", callback ? "callback" : operation >= 0 ? "wrapper" : "value modifier", className,
						method.name, e.getMessage(), callback ? "it does nothing" : operation >= 0 ? "the original call runs" : "it gives the value back unchanged");
			}
		}
		if (!changed) {
			return null;
		}
		var writer = new xyz.wagyourtail.jvmdg.shade.asm.ClassWriter(xyz.wagyourtail.jvmdg.shade.asm.ClassWriter.COMPUTE_MAXS);
		try {
			node.accept(writer);
			return writer.toByteArray();
		} catch (RuntimeException e) {
			return null;
		}
	}

	private static final String OPERATION = "Lcom/llamalad7/mixinextras/injector/wrapoperation/Operation;";

	/** A wrapper's body made {@code return original.call(receiver and arguments)}. */
	private static void callOriginal(MethodNode method, xyz.wagyourtail.jvmdg.shade.asm.Type[] args, xyz.wagyourtail.jvmdg.shade.asm.Type returned,
			int operation) {
		var code = method.instructions;
		int slot = (method.access & xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_STATIC) != 0 ? 0 : 1;
		int[] slots = new int[args.length];
		for (int i = 0; i < args.length; i++) {
			slots[i] = slot;
			slot += args[i].getSize();
		}
		code.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ALOAD, slots[operation]));
		code.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.LdcInsnNode(operation));
		code.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.TypeInsnNode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ANEWARRAY, "java/lang/Object"));
		for (int i = 0; i < operation; i++) {
			code.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.InsnNode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.DUP));
			code.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.LdcInsnNode(i));
			code.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode(args[i].getOpcode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ILOAD), slots[i]));
			String box = box(args[i]);
			if (box != null) {
				code.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.INVOKESTATIC, box, "valueOf",
						"(" + args[i].getDescriptor() + ")L" + box + ";", false));
			}
			code.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.InsnNode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.AASTORE));
		}
		code.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.INVOKEINTERFACE,
				"com/llamalad7/mixinextras/injector/wrapoperation/Operation", "call", "([Ljava/lang/Object;)Ljava/lang/Object;", true));
		if (returned.getSort() == xyz.wagyourtail.jvmdg.shade.asm.Type.VOID) {
			code.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.InsnNode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.POP));
			code.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.InsnNode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.RETURN));
			return;
		}
		String box = box(returned);
		if (box != null) {
			code.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.TypeInsnNode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.CHECKCAST, box));
			code.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.INVOKEVIRTUAL, box,
					returned.getClassName() + "Value", "()" + returned.getDescriptor(), false));
		} else {
			code.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.TypeInsnNode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.CHECKCAST, returned.getInternalName()));
		}
		code.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.InsnNode(returned.getOpcode(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.IRETURN)));
	}

	private static String box(xyz.wagyourtail.jvmdg.shade.asm.Type type) {
		switch (type.getSort()) {
			case xyz.wagyourtail.jvmdg.shade.asm.Type.BOOLEAN:
				return "java/lang/Boolean";
			case xyz.wagyourtail.jvmdg.shade.asm.Type.CHAR:
				return "java/lang/Character";
			case xyz.wagyourtail.jvmdg.shade.asm.Type.BYTE:
				return "java/lang/Byte";
			case xyz.wagyourtail.jvmdg.shade.asm.Type.SHORT:
				return "java/lang/Short";
			case xyz.wagyourtail.jvmdg.shade.asm.Type.INT:
				return "java/lang/Integer";
			case xyz.wagyourtail.jvmdg.shade.asm.Type.FLOAT:
				return "java/lang/Float";
			case xyz.wagyourtail.jvmdg.shade.asm.Type.LONG:
				return "java/lang/Long";
			case xyz.wagyourtail.jvmdg.shade.asm.Type.DOUBLE:
				return "java/lang/Double";
			default:
				return null;
		}
	}

	/** The names Mixin gives the handlers that take a value first and return its new value. */
	private static boolean valueModifier(String name) {
		return name.startsWith("modify$") || name.startsWith("localvar$") || name.startsWith("constant$") || name.startsWith("modifyExpressionValue$")
				|| name.startsWith("modifyReturnValue$");
	}

	/**
	 * Abstract methods Mixin added: a @Shadow of a method 1.12.2 doesn't have stays abstract in the target, and the
	 * first call of the handler that uses it stops the game (AbstractMethodError when a chicken jumps). Null if none.
	 */
	static String unresolvedShadows(byte[] before, byte[] after) {
		var original = new ClassNode();
		new ClassReader(before).accept(original, ClassReader.SKIP_CODE);
		var had = new java.util.HashSet<String>();
		for (MethodNode method : original.methods) {
			had.add(method.name + method.desc);
		}
		var added = new java.util.ArrayList<String>();
		var node = new ClassNode();
		new ClassReader(after).accept(node, ClassReader.SKIP_CODE);
		for (MethodNode method : node.methods) {
			if ((method.access & xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_ABSTRACT) != 0 && !had.contains(method.name + method.desc)) {
				added.add(method.name + method.desc);
			}
		}
		// an interface Mixin added (a mixin's own interface) whose methods neither the class nor its superclasses have
		if ((node.access & xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_INTERFACE) == 0) {
			for (String itf : node.interfaces) {
				if (!original.interfaces.contains(itf)) {
					for (String missing : unimplemented(itf, node)) {
						added.add(itf + "." + missing);
					}
				}
			}
		}
		return added.isEmpty() ? null : "methods 1.12.2 doesn't have, left abstract: " + added;
	}

	/** The abstract methods of an interface (and its superinterfaces) that the class and its superclasses don't have. */
	static java.util.List<String> unimplemented(String itf, ClassNode node) {
		var out = new java.util.ArrayList<String>();
		var todo = new java.util.ArrayDeque<String>();
		todo.add(itf);
		var seen = new java.util.HashSet<String>();
		while (!todo.isEmpty()) {
			ClassNode type = read(todo.poll(), seen);
			if (type == null) {
				continue;
			}
			todo.addAll(type.interfaces);
			for (MethodNode method : type.methods) {
				if ((method.access & xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_ABSTRACT) != 0 && !implemented(node, method.name + method.desc)) {
					out.add(method.name + method.desc);
				}
			}
		}
		return out;
	}

	private static boolean implemented(ClassNode node, String member) {
		var seen = new java.util.HashSet<String>();
		for (ClassNode current = node; current != null; current = current.superName == null ? null : read(current.superName, seen)) {
			for (MethodNode method : current.methods) {
				if ((method.name + method.desc).equals(member) && (method.access & xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_ABSTRACT) == 0) {
					return true;
				}
			}
		}
		return false;
	}

	static ClassNode read(String name, java.util.Set<String> seen) {
		if (!seen.add(name) || name.startsWith("java/")) {
			return null;
		}
		try {
			byte[] bytes = Launch.classLoader.getClassBytes(name.replace('/', '.'));
			if (bytes == null) {
				return null;
			}
			var node = new ClassNode();
			new ClassReader(bytes).accept(node, ClassReader.SKIP_CODE);
			return node;
		} catch (java.io.IOException | RuntimeException e) {
			return null;
		}
	}

	/** The first method that doesn't verify (stack and local types, no class hierarchy), or null. */
	static String verify(byte[] bytes) {
		var node = new ClassNode();
		try {
			new ClassReader(bytes).accept(node, 0);
		} catch (RuntimeException e) {
			return e.toString();
		}
		for (MethodNode method : node.methods) {
			if (method.instructions.size() == 0) {
				continue;
			}
			try {
				new Analyzer<>(new BasicVerifier()).analyze(node.name, method);
			} catch (AnalyzerException | RuntimeException e) {
				return method.name + method.desc + ": " + e.getMessage();
			}
		}
		return null;
	}
}
