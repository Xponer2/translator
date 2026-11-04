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

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import xyz.wagyourtail.jvmdg.shade.asm.ClassReader;
import xyz.wagyourtail.jvmdg.shade.asm.ClassWriter;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;
import xyz.wagyourtail.jvmdg.shade.asm.Type;
import xyz.wagyourtail.jvmdg.shade.asm.tree.AbstractInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;

/**
 * Calls through a 1.21 interface that a 1.21 class implements but its 1.12.2 counterpart doesn't (ItemStack is a
 * DataComponentHolder in 1.21): {@code invokeinterface} on such an object is an IncompatibleClassChangeError. They
 * become static calls of a helper that takes the object first and handles both kinds of receiver.
 */
final class InterfaceDispatch {
	/** Interface (the name after remapping) -> helper class. */
	private static final Map<String, String> HELPERS = new java.util.HashMap<>();

	static {
		String components = "io/github/xponer2/translator/component/ComponentHolders";
		HELPERS.put("net/minecraft/core/component/DataComponentHolder", components);
		HELPERS.put("net/neoforged/neoforge/common/MutableDataComponentHolder", components);
		HELPERS.put("net/neoforged/neoforge/common/extensions/IDataComponentHolderExtension", components);
	}
	/** The helpers' static methods ("owner name desc"), read once from the classes. */
	private static volatile Set<String> methods;

	private InterfaceDispatch() {
	}

	static byte[] rewrite(String className, byte[] bytes) {
		if (HELPERS.containsKey(className) || !mentions(bytes)) {
			// the interfaces themselves keep their calls (their default methods call the holder's own methods)
			return bytes;
		}
		var node = new ClassNode();
		new ClassReader(bytes).accept(node, 0);
		boolean changed = false;
		for (MethodNode method : node.methods) {
			for (AbstractInsnNode insn : method.instructions.toArray()) {
				if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKEINTERFACE && HELPERS.containsKey(call.owner)) {
					String helper = HELPERS.get(call.owner);
					String desc = "(Ljava/lang/Object;" + call.desc.substring(1);
					if (helperMethods().contains(helper + " " + call.name + " " + desc)) {
						method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, helper, call.name, desc, false));
						changed = true;
					}
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

	private static boolean mentions(byte[] bytes) {
		String text = new String(bytes, StandardCharsets.ISO_8859_1);
		for (String name : HELPERS.keySet()) {
			if (text.contains(name)) {
				return true;
			}
		}
		return false;
	}

	private static Set<String> helperMethods() {
		Set<String> known = methods;
		if (known == null) {
			known = new HashSet<>();
			for (String helper : new HashSet<>(HELPERS.values())) {
				try {
					for (var method : Class.forName(helper.replace('/', '.'), false, InterfaceDispatch.class.getClassLoader()).getMethods()) {
						if (java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
							known.add(helper + " " + method.getName() + " " + Type.getMethodDescriptor(method));
						}
					}
				} catch (ClassNotFoundException | LinkageError e) {
					Conversion.LOGGER.warn("Translator: interface helper {} missing: {}", helper, e.toString());
				}
			}
			methods = known;
		}
		return known;
	}
}
