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
		LOGGER.error("Mixins into {} left out: with them the class doesn't load ({})", transformedName, problem);
		return basicClass;
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
