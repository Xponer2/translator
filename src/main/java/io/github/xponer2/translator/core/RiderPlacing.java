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
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode;

/**
 * 1.21's rider placing in every entity: 1.12.2's {@code Entity.updatePassenger(passenger)} first asks
 * {@code EntityApiBridges.placesRiders}, which places the rider by the vehicle's own 1.21 positionRider /
 * getPassengerRidingPosition / getPassengerAttachmentPoint when its class has one (1.12.2 never calls them), and returns
 * when it did. The method is found by its descriptor and its SRG name (func_184232_k) in the names of the running game.
 */
public final class RiderPlacing implements IClassTransformer {
	private static final String BRIDGES = "io/github/xponer2/translator/runtime/redirect/EntityApiBridges";

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || !"net.minecraft.entity.Entity".equals(transformedName)) {
			return basicClass;
		}
		try {
			return entity(basicClass);
		} catch (RuntimeException | LinkageError e) {
			LogManager.getLogger("Translator").warn("Entity.updatePassenger isn't as expected ({}): the 1.21.1 mods' vehicles seat their riders as 1.12.2 does",
					e.toString());
			return basicClass;
		}
	}

	private static byte[] entity(byte[] basicClass) {
		var node = new ClassNode();
		new ClassReader(basicClass).accept(node, 0);
		String desc = "(L" + node.name + ";)V";
		MethodNode update = null;
		for (MethodNode method : node.methods) {
			if ((method.access & Opcodes.ACC_STATIC) != 0 || !desc.equals(method.desc)) {
				continue;
			}
			String srg = net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(node.name, method.name,
					method.desc);
			if ("updatePassenger".equals(method.name) || "func_184232_k".equals(method.name) || "func_184232_k".equals(srg)) {
				update = method;
			}
		}
		if (update == null) {
			throw new IllegalStateException("no method updatePassenger" + desc);
		}
		// in the class's own names (this runs before the deobfuscation, which renames them with the rest of the class)
		InsnList head = new InsnList();
		LabelNode normal = new LabelNode();
		head.add(new VarInsnNode(Opcodes.ALOAD, 0));
		head.add(new VarInsnNode(Opcodes.ALOAD, 1));
		head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGES, "placesRiders", "(L" + node.name + ";L" + node.name + ";)Z", false));
		head.add(new JumpInsnNode(Opcodes.IFEQ, normal));
		head.add(new InsnNode(Opcodes.RETURN));
		head.add(normal);
		head.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
		update.instructions.insert(head);
		update.maxStack = Math.max(update.maxStack, 2);
		var writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}
}
