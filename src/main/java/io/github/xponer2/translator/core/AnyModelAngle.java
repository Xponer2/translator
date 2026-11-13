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
import xyz.wagyourtail.jvmdg.shade.asm.tree.InsnList;
import xyz.wagyourtail.jvmdg.shade.asm.tree.InsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.LdcInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode;

/**
 * A block model element's rotation of any angle, as NeoForge reads them: 1.12.2's BlockPart deserializer threw for
 * anything but -45, -22.5, 0, 22.5 and 45 ("Invalid rotation 7.5 found"), and the models of the 1.21.1 mods that turn
 * an element by another angle (Handcrafted's benches and couches) didn't load: missing-texture cubes, thousands of
 * errors in the log. 1.12.2's face baker turns by any angle.
 */
public final class AnyModelAngle implements IClassTransformer {
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || !"net.minecraft.client.renderer.block.model.BlockPart$Deserializer".equals(transformedName)) {
			return basicClass;
		}
		try {
			return deserializer(basicClass);
		} catch (RuntimeException | LinkageError e) {
			LogManager.getLogger("Translator").warn("BlockPart's angle parsing isn't as expected ({}): model rotations stay -45 to 45 by 22.5", e.toString());
			return basicClass;
		}
	}

	private static byte[] deserializer(byte[] basicClass) {
		var node = new ClassNode();
		new ClassReader(basicClass).accept(node, 0);
		MethodNode parseAngle = null;
		for (MethodNode method : node.methods) {
			if ((method.access & Opcodes.ACC_PRIVATE) != 0 && "(Lcom/google/gson/JsonObject;)F".equals(method.desc)) {
				if (parseAngle != null) {
					throw new IllegalStateException("two private methods (JsonObject)F");
				}
				parseAngle = method;
			}
		}
		if (parseAngle == null) {
			throw new IllegalStateException("no private method (JsonObject)F");
		}
		// return json.get("angle").getAsFloat()
		int json = (parseAngle.access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
		var body = new InsnList();
		body.add(new VarInsnNode(Opcodes.ALOAD, json));
		body.add(new LdcInsnNode("angle"));
		body.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "com/google/gson/JsonObject", "get", "(Ljava/lang/String;)Lcom/google/gson/JsonElement;", false));
		body.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "com/google/gson/JsonElement", "getAsFloat", "()F", false));
		body.add(new InsnNode(Opcodes.FRETURN));
		parseAngle.instructions = body;
		parseAngle.tryCatchBlocks.clear();
		parseAngle.localVariables = null;
		parseAngle.maxStack = 2;
		parseAngle.maxLocals = json + 1;
		var writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}
}
