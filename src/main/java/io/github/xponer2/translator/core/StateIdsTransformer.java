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
 * 1.12.2's chunk section storage ({@code BlockStateContainer}) with the extra block state ids of
 * {@code world.StateIds}: the global palette's bits come from the largest id ({@code StateIds.paletteSize}), not from the
 * number of states ({@code BLOCK_STATE_IDS.size()}), and the save ({@code getDataForNBT}) writes a state's metadata id
 * ({@code StateIds.saveId}): its block and metadata, the full state goes next to it. Found by the calls on the id map's
 * class (by its unobfuscated name), so the same with or without obfuscation.
 */
public final class StateIdsTransformer implements IClassTransformer {
	private static final String HELPER = "io/github/xponer2/translator/world/StateIds";

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || !"net.minecraft.world.chunk.BlockStateContainer".equals(transformedName) || BlockIdLimit.otherExtender()) {
			return basicClass;
		}
		try {
			return container(basicClass);
		} catch (RuntimeException | LinkageError e) {
			LogManager.getLogger("Translator").warn("BlockStateContainer isn't as expected ({}): block states of the 1.21.1 mods past their metadata aren't kept",
					e.toString());
			return basicClass;
		}
	}

	private static String unmap(String internal) {
		return net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper.INSTANCE.unmap(internal);
	}

	private static byte[] container(byte[] basicClass) {
		String ids = unmap("net/minecraft/util/ObjectIntIdentityMap");
		String nibbles = unmap("net/minecraft/world/chunk/NibbleArray");
		// getDataForNBT(byte[] blockIds, NibbleArray data): NibbleArray
		String save = "([BL" + nibbles + ";)L" + nibbles + ";";
		var node = new ClassNode();
		new ClassReader(basicClass).accept(node, 0);
		int sizes = 0;
		int saves = 0;
		for (MethodNode method : node.methods) {
			boolean saving = save.equals(method.desc);
			for (AbstractInsnNode insn : method.instructions.toArray()) {
				if (!(insn instanceof MethodInsnNode call) || call.getOpcode() != Opcodes.INVOKEVIRTUAL || !ids.equals(call.owner)) {
					continue;
				}
				if ("()I".equals(call.desc)) {
					// BLOCK_STATE_IDS.size(): the only method of the map without arguments that gives an int
					method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "paletteSize", "(Ljava/lang/Object;)I", false));
					sizes++;
				} else if (saving && "(Ljava/lang/Object;)I".equals(call.desc)) {
					// BLOCK_STATE_IDS.get(state)
					method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, "saveId", "(Ljava/lang/Object;Ljava/lang/Object;)I", false));
					saves++;
				}
			}
		}
		if (sizes == 0 || saves != 1) {
			throw new IllegalStateException(sizes + " size calls, " + saves + " save id calls");
		}
		// the same stack: no frames to compute
		var writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}
}
