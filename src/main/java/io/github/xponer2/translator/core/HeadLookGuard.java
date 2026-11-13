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
import xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.FrameNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.InsnList;
import xyz.wagyourtail.jvmdg.shade.asm.tree.InsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.JumpInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.LabelNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode;

/**
 * A head turn packet that comes after the client's world is gone (the player left the world or changed dimension)
 * finds no entity instead of failing: 1.12.2 handles it in a scheduled task, which threw a NullPointerException for
 * each (1200 "Error executing task" in a test run, the client lagging behind).
 */
public final class HeadLookGuard implements IClassTransformer {
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || !"net.minecraft.network.play.server.SPacketEntityHeadLook".equals(transformedName)) {
			return basicClass;
		}
		try {
			return packet(basicClass);
		} catch (RuntimeException | LinkageError e) {
			LogManager.getLogger("Translator").warn("SPacketEntityHeadLook isn't as expected ({}): a head turn after leaving a world still fails", e.toString());
			return basicClass;
		}
	}

	private static String unmap(String internal) {
		return net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper.INSTANCE.unmap(internal);
	}

	private static byte[] packet(byte[] basicClass) {
		String desc = "(L" + unmap("net/minecraft/world/World") + ";)L" + unmap("net/minecraft/entity/Entity") + ";";
		var node = new ClassNode();
		new ClassReader(basicClass).accept(node, 0);
		MethodNode getEntity = null;
		for (MethodNode method : node.methods) {
			if ((method.access & Opcodes.ACC_STATIC) == 0 && desc.equals(method.desc)) {
				getEntity = method;
			}
		}
		if (getEntity == null) {
			throw new IllegalStateException("no method " + desc);
		}
		// if (world == null) return null;
		var guard = new InsnList();
		var go = new LabelNode();
		guard.add(new VarInsnNode(Opcodes.ALOAD, 1));
		guard.add(new JumpInsnNode(Opcodes.IFNONNULL, go));
		guard.add(new InsnNode(Opcodes.ACONST_NULL));
		guard.add(new InsnNode(Opcodes.ARETURN));
		guard.add(go);
		// the frame of the branch target: the method's first one (this and the world, nothing on the stack)
		guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
		getEntity.instructions.insert(guard);
		getEntity.maxStack = Math.max(getEntity.maxStack, 1);
		var writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}
}
