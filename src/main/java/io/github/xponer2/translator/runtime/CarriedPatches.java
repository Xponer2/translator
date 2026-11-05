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

import xyz.wagyourtail.jvmdg.shade.asm.ClassReader;
import xyz.wagyourtail.jvmdg.shade.asm.ClassWriter;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;
import xyz.wagyourtail.jvmdg.shade.asm.tree.AbstractInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;

/**
 * Changes to single carried 1.21.1 classes whose code assumes something 1.12.2 doesn't give: several 1.21 blocks are
 * one 1.12.2 block (the planks of every wood are PLANKS with a variant), so a check that each block is used once fails
 * there for the whole class.
 */
final class CarriedPatches {
	private static final String BRIDGES = "io/github/xponer2/translator/runtime/redirect/CarriedBridges";

	private CarriedPatches() {
	}

	/** The carried class (its path in the API jar), changed when it has a patch. */
	static byte[] apply(String path, byte[] bytes) {
		if ("net/minecraft/data/BlockFamilies.class".equals(path)) {
			return mapPutFirst(bytes, "familyBuilder");
		}
		if ("net/minecraft/world/level/block/grower/TreeGrower.class".equals(path)) {
			return callInstead(bytes, "growTree", "io/github/xponer2/translator/world/TreeGrowing");
		}
		return bytes;
	}

	/**
	 * The method's body becomes a call of the helper's static method of the same name (this and the arguments, as
	 * Object). TreeGrower.growTree places a configured feature of the 1.21 registries, which don't run here: a sapling
	 * of a 1.21.1 mod never grew (bone meal did nothing); the helper grows the tree of the mod's worldgen data.
	 */
	private static byte[] callInstead(byte[] bytes, String methodName, String helper) {
		var node = new ClassNode();
		new ClassReader(bytes).accept(node, 0);
		boolean changed = false;
		for (MethodNode method : node.methods) {
			if (!methodName.equals(method.name) || (method.access & (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC)) != 0) {
				continue;
			}
			var insns = new xyz.wagyourtail.jvmdg.shade.asm.tree.InsnList();
			insns.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode(Opcodes.ALOAD, 0));
			var helperDesc = new StringBuilder("(Ljava/lang/Object;");
			int slot = 1;
			for (var arg : xyz.wagyourtail.jvmdg.shade.asm.Type.getArgumentTypes(method.desc)) {
				insns.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode(arg.getOpcode(Opcodes.ILOAD), slot));
				slot += arg.getSize();
				boolean object = arg.getSort() == xyz.wagyourtail.jvmdg.shade.asm.Type.OBJECT || arg.getSort() == xyz.wagyourtail.jvmdg.shade.asm.Type.ARRAY;
				helperDesc.append(object ? "Ljava/lang/Object;" : arg.getDescriptor());
			}
			var returns = xyz.wagyourtail.jvmdg.shade.asm.Type.getReturnType(method.desc);
			boolean objectReturn = returns.getSort() == xyz.wagyourtail.jvmdg.shade.asm.Type.OBJECT || returns.getSort() == xyz.wagyourtail.jvmdg.shade.asm.Type.ARRAY;
			helperDesc.append(')').append(objectReturn ? "Ljava/lang/Object;" : returns.getDescriptor());
			insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC, helper, methodName, helperDesc.toString(), false));
			if (objectReturn) {
				insns.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.TypeInsnNode(Opcodes.CHECKCAST, returns.getInternalName()));
			}
			insns.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.InsnNode(returns.getOpcode(Opcodes.IRETURN)));
			method.instructions = insns;
			method.tryCatchBlocks.clear();
			if (method.localVariables != null) {
				method.localVariables.clear();
			}
			changed = true;
		}
		if (!changed) {
			return bytes;
		}
		var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		node.accept(writer);
		return writer.toByteArray();
	}

	/**
	 * BlockFamilies keeps one family per base block and throws "Duplicate family definition" for a second one: on
	 * 1.12.2 the planks of all woods are one block, so its static initializer failed at birch and every family after
	 * it was null (Sophisticated Storage's wood storage, so the mod, failed). The first family stays for the shared
	 * block; each family is still made and in its own field.
	 */
	private static byte[] mapPutFirst(byte[] bytes, String methodName) {
		var node = new ClassNode();
		new ClassReader(bytes).accept(node, 0);
		boolean changed = false;
		for (MethodNode method : node.methods) {
			if (!methodName.equals(method.name)) {
				continue;
			}
			for (AbstractInsnNode insn : method.instructions.toArray()) {
				if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKEINTERFACE && "java/util/Map".equals(call.owner)
						&& "put".equals(call.name) && "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;".equals(call.desc)) {
					method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGES, "putFirst",
							"(Ljava/util/Map;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false));
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
}
