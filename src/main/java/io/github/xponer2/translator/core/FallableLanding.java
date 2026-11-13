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
import xyz.wagyourtail.jvmdg.shade.asm.tree.InsnList;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.TypeInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode;

/**
 * 1.21's landing of a falling block in 1.12.2's falling block entity: 1.12.2 tells only a BlockFalling where it landed
 * ({@code onEndFalling}) or broke ({@code onBroken}); the 1.21.1 mods' falling blocks (the Translator's FallingBlock and
 * any 1.21 Fallable: Supplementaries' ash, sacks and urns) heard nothing. Each {@code block instanceof BlockFalling}
 * becomes {@code FallingBlock.landsAsFallable(block)} and the two calls go through {@code FallingBlock.landed} and
 * {@code FallingBlock.broken}, with the entity. Found by owner and descriptor in the names of the running game.
 */
public final class FallableLanding implements IClassTransformer {
	private static final String HOOKS = "io/github/xponer2/translator/runtime/mc/FallingBlock";

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || !"net.minecraft.entity.item.EntityFallingBlock".equals(transformedName)) {
			return basicClass;
		}
		try {
			return entity(basicClass);
		} catch (RuntimeException | LinkageError e) {
			LogManager.getLogger("Translator").warn("EntityFallingBlock isn't as expected ({}): the 1.21.1 mods' falling blocks aren't told where they land",
					e.toString());
			return basicClass;
		}
	}

	private static String unmap(String internal) {
		return net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper.INSTANCE.unmap(internal);
	}

	private static byte[] entity(byte[] basicClass) {
		String falling = unmap("net/minecraft/block/BlockFalling");
		String block = "L" + unmap("net/minecraft/block/Block") + ";";
		String world = "L" + unmap("net/minecraft/world/World") + ";";
		String pos = "L" + unmap("net/minecraft/util/math/BlockPos") + ";";
		String state = "L" + unmap("net/minecraft/block/state/IBlockState") + ";";
		String endFalling = "(" + world + pos + state + state + ")V";
		String broken = "(" + world + pos + ")V";
		var node = new ClassNode();
		new ClassReader(basicClass).accept(node, 0);
		String self = "L" + node.name + ";";
		int landed = 0;
		for (MethodNode method : node.methods) {
			for (AbstractInsnNode insn : method.instructions.toArray()) {
				if (insn instanceof TypeInsnNode type && falling.equals(type.desc) && type.getOpcode() == Opcodes.INSTANCEOF) {
					method.instructions.set(type, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "landsAsFallable", "(Ljava/lang/Object;)Z", false));
				} else if (insn instanceof TypeInsnNode cast && falling.equals(cast.desc) && cast.getOpcode() == Opcodes.CHECKCAST) {
					// kept as the Block it is: the hooks take any block
					method.instructions.remove(cast);
				} else if (insn instanceof MethodInsnNode call && falling.equals(call.owner) && call.getOpcode() == Opcodes.INVOKEVIRTUAL
						&& (endFalling.equals(call.desc) || broken.equals(call.desc))) {
					boolean end = endFalling.equals(call.desc);
					InsnList hook = new InsnList();
					hook.add(new VarInsnNode(Opcodes.ALOAD, 0));
					hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, end ? "landed" : "broken",
							"(" + block + world + pos + (end ? state + state : "") + self + ")V", false));
					method.instructions.insertBefore(call, hook);
					method.instructions.remove(call);
					method.maxStack = Math.max(method.maxStack, end ? 7 : 5);
					landed++;
				}
			}
		}
		if (landed != 2) {
			throw new IllegalStateException(landed + " BlockFalling calls, not 2");
		}
		var writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}
}
