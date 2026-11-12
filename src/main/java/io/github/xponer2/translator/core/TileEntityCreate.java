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

import net.minecraft.launchwrapper.IClassTransformer;

import xyz.wagyourtail.jvmdg.shade.asm.ClassReader;
import xyz.wagyourtail.jvmdg.shade.asm.ClassWriter;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;
import xyz.wagyourtail.jvmdg.shade.asm.tree.AbstractInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;

/**
 * 1.12.2's {@code TileEntity.create}, which makes a saved block entity with its class's no-argument constructor
 * ({@code Class.newInstance}): the call goes to {@code registry.BlockEntityDefaults.newInstance}, which makes a 1.21.1
 * mod's block entity without one by its type's factory. Found by the call (Class.newInstance in TileEntity), so the same
 * with or without obfuscation.
 */
public final class TileEntityCreate implements IClassTransformer {
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || !"net.minecraft.tileentity.TileEntity".equals(transformedName)) {
			return basicClass;
		}
		var node = new ClassNode();
		new ClassReader(basicClass).accept(node, 0);
		int changed = 0;
		for (MethodNode method : node.methods) {
			if ((method.access & Opcodes.ACC_STATIC) == 0) {
				continue;
			}
			for (AbstractInsnNode insn : method.instructions.toArray()) {
				if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKEVIRTUAL && "java/lang/Class".equals(call.owner)
						&& "newInstance".equals(call.name) && "()Ljava/lang/Object;".equals(call.desc)) {
					method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/registry/BlockEntityDefaults",
							"newInstance", "(Ljava/lang/Class;)Ljava/lang/Object;", false));
					changed++;
				}
			}
		}
		if (changed == 0) {
			return basicClass;
		}
		// the same stack and locals: no frames to compute
		var writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}
}
