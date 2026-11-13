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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.logging.log4j.LogManager;

import net.minecraft.launchwrapper.IClassTransformer;
import xyz.wagyourtail.jvmdg.shade.asm.ClassReader;
import xyz.wagyourtail.jvmdg.shade.asm.ClassWriter;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;
import xyz.wagyourtail.jvmdg.shade.asm.tree.AbstractInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.InvokeDynamicInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;

/**
 * fastutil 8's default methods in the game's fastutil 7 interfaces. 1.12.2 has fastutil 7, whose functions are
 * whole map-like interfaces with every method abstract; fastutil 8, which the 1.21.1 mods are made for, gives all but
 * one of them a body, so a mod writes a function as a lambda. Such a lambda on fastutil 7 had only its one method:
 * any other call (GeckoLib's easings asked their Double2DoubleFunction for get(Object)) failed with
 * AbstractMethodError, and GeckoLib's animated mobs weren't drawn. A method abstract in fastutil 7 (in the interface or
 * in a fastutil interface it extends) and default in fastutil 8 gets fastutil 8's body here, with the default methods
 * of the interface that body calls. The classes of fastutil 7 keep their own methods (1.12.2 and its mods use them).
 * fastutil 8 is read from the libraries of the player's 1.21.1 game, as the carried API is.
 */
public final class FastutilDefaults implements IClassTransformer {
	private static final String PREFIX = "it.unimi.dsi.fastutil.";
	private static final String NEWER = "it/unimi/dsi/fastutil/8.5.12/fastutil-8.5.12.jar";
	private static final AtomicInteger CHANGED = new AtomicInteger();
	private static final Object LOCK = new Object();
	private static boolean looked;
	private static ZipFile newer;

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || transformedName == null || !transformedName.startsWith(PREFIX)) {
			return basicClass;
		}
		try {
			var reader = new ClassReader(basicClass);
			if ((reader.getAccess() & Opcodes.ACC_INTERFACE) == 0) {
				return basicClass;
			}
			byte[] newerClass = newerClass(reader.getClassName());
			if (newerClass == null) {
				return basicClass;
			}
			byte[] changed = withDefaults(basicClass, reader, newerClass);
			if (changed != basicClass && CHANGED.getAndIncrement() == 0) {
				LogManager.getLogger("Translator").info("fastutil 7 interfaces get the default methods of fastutil 8 (first: {})", transformedName);
			}
			return changed;
		} catch (RuntimeException | LinkageError | IOException e) {
			LogManager.getLogger("Translator").warn("fastutil 8 defaults for {} not added ({})", transformedName, e.toString());
			return basicClass;
		}
	}

	private static byte[] withDefaults(byte[] basicClass, ClassReader reader, byte[] newerClass) {
		var game = new ClassNode();
		reader.accept(game, 0);
		// a class made for Java 5 has no stack map frames, which the Java 8 class version the defaults need asks for
		if (game.version < Opcodes.V1_6 && hasCode(game)) {
			return basicClass;
		}
		var newerNode = new ClassNode();
		new ClassReader(newerClass).accept(newerNode, 0);

		Map<String, MethodNode> own = new HashMap<>();
		for (MethodNode method : game.methods) {
			own.put(method.name + method.desc, method);
		}
		Set<String> inheritedAbstract = inheritedAbstract(game);
		Map<String, MethodNode> defaults = new HashMap<>();
		for (MethodNode method : newerNode.methods) {
			if ((method.access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_PRIVATE)) == 0 && method.instructions.size() > 0
					&& !method.name.startsWith("<") && simple(method)) {
				defaults.put(method.name + method.desc, method);
			}
		}

		// the bodies: methods abstract in fastutil 7 (here or above), then the methods of this interface they call
		Map<String, MethodNode> chosen = new HashMap<>();
		var todo = new ArrayDeque<String>();
		for (String key : defaults.keySet()) {
			MethodNode mine = own.get(key);
			if (mine != null ? (mine.access & Opcodes.ACC_ABSTRACT) != 0 : inheritedAbstract.contains(key)) {
				todo.add(key);
			}
		}
		while (!todo.isEmpty()) {
			String key = todo.poll();
			if (chosen.containsKey(key)) {
				continue;
			}
			MethodNode method = defaults.get(key);
			chosen.put(key, method);
			for (AbstractInsnNode insn : method.instructions) {
				if (insn instanceof MethodInsnNode call && call.owner.equals(game.name)) {
					String called = call.name + call.desc;
					MethodNode mine = own.get(called);
					if ((mine == null || (mine.access & Opcodes.ACC_ABSTRACT) != 0) && defaults.containsKey(called) && !inheritedAbstract.contains(called)) {
						todo.add(called);
					}
				}
			}
		}
		if (chosen.isEmpty()) {
			return basicClass;
		}
		for (var entry : chosen.entrySet()) {
			MethodNode mine = own.get(entry.getKey());
			if (mine != null) {
				game.methods.remove(mine);
			}
			game.methods.add(entry.getValue());
		}
		game.version = Math.max(game.version & 0xFFFF, Opcodes.V1_8);
		var writer = new ClassWriter(0);
		game.accept(writer);
		return writer.toByteArray();
	}

	/** A body without lambdas (their methods and bootstrap stay in fastutil 8's class). */
	private static boolean simple(MethodNode method) {
		for (AbstractInsnNode insn : method.instructions) {
			if (insn instanceof InvokeDynamicInsnNode) {
				return false;
			}
		}
		return true;
	}

	private static boolean hasCode(ClassNode node) {
		for (MethodNode method : node.methods) {
			if (method.instructions.size() > 0) {
				return true;
			}
		}
		return false;
	}

	/** The abstract methods of the fastutil 7 interfaces this one extends, all the way up. */
	private static Set<String> inheritedAbstract(ClassNode node) {
		Set<String> out = new HashSet<>();
		Set<String> seen = new HashSet<>();
		var todo = new ArrayDeque<String>(node.interfaces);
		while (!todo.isEmpty()) {
			String name = todo.poll();
			if (!name.startsWith("it/unimi/dsi/fastutil/") || !seen.add(name)) {
				continue;
			}
			byte[] bytes;
			try {
				bytes = classBytes(name);
			} catch (IOException e) {
				continue;
			}
			if (bytes == null) {
				continue;
			}
			var parent = new ClassNode();
			new ClassReader(bytes).accept(parent, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
			for (MethodNode method : parent.methods) {
				if ((method.access & Opcodes.ACC_ABSTRACT) != 0) {
					out.add(method.name + method.desc);
				}
			}
			todo.addAll(parent.interfaces);
		}
		return out;
	}

	/** The game's own (untransformed) class file. */
	private static byte[] classBytes(String internalName) throws IOException {
		net.minecraft.launchwrapper.LaunchClassLoader launch;
		try {
			launch = net.minecraft.launchwrapper.Launch.classLoader;
		} catch (LinkageError e) {
			// not launched by launchwrapper (a check outside the game)
			launch = null;
		}
		if (launch != null) {
			return launch.getClassBytes(internalName.replace('/', '.'));
		}
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		try (InputStream in = loader == null ? null : loader.getResourceAsStream(internalName + ".class")) {
			return in == null ? null : readAll(in);
		}
	}

	private static byte[] readAll(InputStream in) throws IOException {
		var out = new java.io.ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		for (int read; (read = in.read(buffer)) > 0;) {
			out.write(buffer, 0, read);
		}
		return out.toByteArray();
	}

	private static byte[] newerClass(String internalName) throws IOException {
		ZipFile zip;
		synchronized (LOCK) {
			if (!looked) {
				looked = true;
				File jar = findNewer();
				newer = jar == null ? null : new ZipFile(jar);
				if (newer == null) {
					LogManager.getLogger("Translator").warn("fastutil 8.5.12 not found in the 1.21.1 libraries: fastutil lambdas of the mods stay one-method");
				}
			}
			zip = newer;
		}
		if (zip == null) {
			return null;
		}
		ZipEntry entry = zip.getEntry(internalName + ".class");
		if (entry == null) {
			return null;
		}
		try (InputStream in = zip.getInputStream(entry)) {
			return readAll(in);
		}
	}

	/** fastutil 8.5.12 where the Translator's carried API finds the 1.21.1 libraries (ApiCarry.libraries). */
	private static File findNewer() {
		String appData = System.getenv("APPDATA");
		if (appData != null) {
			for (String root : new String[] { "PrismLauncher/libraries", ".minecraft/libraries" }) {
				File jar = new File(new File(appData, root), NEWER);
				if (jar.isFile()) {
					return jar;
				}
			}
		}
		File[] hashes = new File(System.getProperty("user.home"), ".gradle/caches/modules-2/files-2.1/it.unimi.dsi/fastutil/8.5.12").listFiles();
		for (File hash : hashes == null ? new File[0] : hashes) {
			File jar = new File(hash, "fastutil-8.5.12.jar");
			if (jar.isFile()) {
				return jar;
			}
		}
		return null;
	}
}
