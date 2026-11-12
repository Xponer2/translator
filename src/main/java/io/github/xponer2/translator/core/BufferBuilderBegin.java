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
import xyz.wagyourtail.jvmdg.shade.asm.tree.InsnList;
import xyz.wagyourtail.jvmdg.shade.asm.tree.LdcInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode;

/**
 * 1.12.2's {@code BufferBuilder.begin} throws "Already building!" when the buffer is still in a batch: a 1.21.1 mod's
 * render that failed between its begin and its draw (JourneyMap's waypoint beacons, a block entity renderer) left the
 * shared Tessellator so, and the next vanilla renderer crashed the game (a bed in the test world). begin first calls
 * {@code client.TessellatorGuard.beforeBegin(this)}, which ends the left batch (its vertices dropped, logged once).
 * Found by the method's "Already building!" text, so the same with or without obfuscation.
 */
public final class BufferBuilderBegin implements IClassTransformer {
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || !"net.minecraft.client.renderer.BufferBuilder".equals(transformedName)) {
			return basicClass;
		}
		var node = new ClassNode();
		new ClassReader(basicClass).accept(node, 0);
		boolean changed = false;
		for (MethodNode method : node.methods) {
			if ((method.access & Opcodes.ACC_STATIC) != 0 || !method.desc.startsWith("(I")) {
				continue;
			}
			boolean begin = false;
			for (AbstractInsnNode insn : method.instructions.toArray()) {
				begin |= insn instanceof LdcInsnNode ldc && "Already building!".equals(ldc.cst);
			}
			if (begin) {
				var head = new InsnList();
				head.add(new VarInsnNode(Opcodes.ALOAD, 0));
				head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/client/TessellatorGuard", "beforeBegin",
						"(Ljava/lang/Object;)V", false));
				method.instructions.insert(head);
				method.maxStack = Math.max(method.maxStack, 1);
				changed = true;
			}
		}
		if (!changed) {
			return basicClass;
		}
		// a call at the method's start that leaves the stack empty: no frames to compute
		var writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}
}
