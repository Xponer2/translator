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

import org.apache.logging.log4j.LogManager;

import net.minecraft.launchwrapper.IClassTransformer;
import xyz.wagyourtail.jvmdg.shade.asm.ClassReader;
import xyz.wagyourtail.jvmdg.shade.asm.ClassWriter;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;
import xyz.wagyourtail.jvmdg.shade.asm.tree.AbstractInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;

/**
 * More than 4096 blocks (many 1.21.1 mods together): the world generator's {@code ChunkPrimer} keeps a block state
 * id in a {@code char}, so states of blocks past 4095 get theirs from {@code BlockIds} instead. Found by the shape of
 * the code (the {@code char} array store and load), not by names, so it is the same with or without obfuscation. The
 * registry limit and the chunk save format are {@code io.github.xponer2.translator.world.BlockIds}.
 */
public final class BlockIdLimit implements IClassTransformer {
	private static final String HELPER = "io/github/xponer2/translator/world/BlockIds";

	/** JustEnoughIDs / RoughlyEnoughIDs (same package) extend the ids themselves: then nothing here. */
	static boolean otherExtender() {
		try {
			return net.minecraft.launchwrapper.Launch.classLoader.getClassBytes("org.dimdev.jeid.JEID") != null;
		} catch (java.io.IOException | RuntimeException e) {
			return false;
		}
	}

	private static Boolean skip;

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || !transformedName.equals("net.minecraft.block.Block") && !transformedName.equals("net.minecraft.world.chunk.ChunkPrimer")
				&& !transformedName.equals("net.minecraft.stats.StatList")) {
			return basicClass;
		}
		if (skip == null) {
			skip = otherExtender();
		}
		if (skip) {
			return basicClass;
		}
		if ("net.minecraft.block.Block".equals(transformedName)) {
			return block(basicClass);
		}
		if ("net.minecraft.stats.StatList".equals(transformedName)) {
			return stats(basicClass);
		}
		if (basicClass == null || !"net.minecraft.world.chunk.ChunkPrimer".equals(transformedName)) {
			return basicClass;
		}
		var node = new ClassNode();
		new ClassReader(basicClass).accept(node, 0);
		int stores = 0;
		int loads = 0;
		for (MethodNode method : node.methods) {
			for (AbstractInsnNode insn : method.instructions.toArray()) {
				if (insn.getOpcode() == Opcodes.CASTORE && insn.getPrevious() != null && insn.getPrevious().getOpcode() == Opcodes.I2C) {
					// setBlockState: data[index] = (char) id
					method.instructions.remove(insn.getPrevious());
					method.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "primerStore", "([CII)V", false));
					stores++;
				} else if (insn.getOpcode() == Opcodes.CALOAD && method.desc.startsWith("(III)L")) {
					// getBlockState: the id of data[index]
					method.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "primerLoad", "([CI)I", false));
					loads++;
				}
			}
		}
		if (stores != 1 || loads != 1) {
			LogManager.getLogger("Translator").warn("ChunkPrimer isn't as expected ({} stores, {} loads): world generation keeps 4096 blocks",
					stores, loads);
			return basicClass;
		}
		var writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}

	/**
	 * Block.getStateId / getStateById (particles, the block break effect, falling blocks) pack the block id in 12 bits:
	 * they go through {@code BlockIds}, which packs ids past 4095 another way. Found by shape: the static method that
	 * masks with 4095 (getStateById) and the static one that shifts the meta by 12 (getStateId).
	 */
	private static byte[] block(byte[] basicClass) {
		var node = new ClassNode();
		new ClassReader(basicClass).accept(node, 0);
		MethodNode byId = null;
		MethodNode toId = null;
		for (MethodNode method : node.methods) {
			if ((method.access & Opcodes.ACC_STATIC) == 0) {
				continue;
			}
			if (method.desc.startsWith("(I)L") && has(method, Opcodes.SIPUSH, 4095)) {
				byId = byId == null ? method : null;
			} else if (method.desc.startsWith("(L") && method.desc.endsWith(")I") && has(method, Opcodes.BIPUSH, 12)) {
				toId = toId == null ? method : null;
			}
		}
		if (byId == null || toId == null) {
			LogManager.getLogger("Translator").warn("Block.getStateId isn't as expected: blocks past 4095 show other blocks' particles");
			return basicClass;
		}
		body(byId, new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "stateById", "(I)Lnet/minecraft/block/state/IBlockState;", false),
				Opcodes.ILOAD, Opcodes.ARETURN);
		body(toId, new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "stateId", "(Lnet/minecraft/block/state/IBlockState;)I", false),
				Opcodes.ALOAD, Opcodes.IRETURN);
		var writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}

	/**
	 * StatList's mining stats are an array of 4096 by block id (the id mapping event filled it: ArrayIndexOutOfBounds
	 * 4096 as the game started): as long as the block ids go.
	 */
	private static byte[] stats(byte[] basicClass) {
		var node = new ClassNode();
		new ClassReader(basicClass).accept(node, 0);
		int changed = 0;
		for (MethodNode method : node.methods) {
			if (!"<clinit>".equals(method.name)) {
				continue;
			}
			for (AbstractInsnNode insn : method.instructions.toArray()) {
				if (insn.getOpcode() == Opcodes.SIPUSH && ((xyz.wagyourtail.jvmdg.shade.asm.tree.IntInsnNode) insn).operand == 4096
						&& insn.getNext() != null && insn.getNext().getOpcode() == Opcodes.ANEWARRAY) {
					method.instructions.set(insn, new xyz.wagyourtail.jvmdg.shade.asm.tree.LdcInsnNode(io.github.xponer2.translator.core.BlockIdLimit.MAX_BLOCKS));
					changed++;
				}
			}
		}
		if (changed != 1) {
			LogManager.getLogger("Translator").warn("StatList isn't as expected: the mining stats keep 4096 blocks");
			return basicClass;
		}
		var writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}

	/** Block ids 0 to 65535 (BlockIds.MAX_BLOCK_ID); not BlockIds itself: the coremod loads before Minecraft's classes. */
	static final int MAX_BLOCKS = 65536;

	private static boolean has(MethodNode method, int opcode, int operand) {
		for (AbstractInsnNode insn : method.instructions.toArray()) {
			if (insn.getOpcode() == opcode && insn instanceof xyz.wagyourtail.jvmdg.shade.asm.tree.IntInsnNode push && push.operand == operand) {
				return true;
			}
		}
		return false;
	}

	/** The method's body: its one argument to the helper, the helper's result returned. */
	private static void body(MethodNode method, MethodInsnNode call, int load, int ret) {
		method.instructions.clear();
		method.tryCatchBlocks.clear();
		if (method.localVariables != null) {
			method.localVariables.clear();
		}
		method.instructions.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode(load, 0));
		method.instructions.add(call);
		method.instructions.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.InsnNode(ret));
		method.maxStack = Math.max(method.maxStack, 1);
	}
}
