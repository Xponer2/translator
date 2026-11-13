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
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode;

/**
 * A block model's texture names resolved as 1.21 resolves them (ModelBlock.resolveTextureName gives the work to
 * {@link io.github.xponer2.translator.client.ModelTextureNames}): a reference to a reference of the same model
 * ("east": "#side" with "side" next to it) was an "upward reference" to 1.12.2 and the face got the missing texture
 * (Rechiseled's connecting blocks, Twilight Forest's, Create's: thousands of warnings and purple-black faces).
 */
public final class ModelTextureRefs implements IClassTransformer {
	private static final String HOOK = "io/github/xponer2/translator/client/ModelTextureNames";

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || !"net.minecraft.client.renderer.block.model.ModelBlock".equals(transformedName)) {
			return basicClass;
		}
		try {
			return modelBlock(basicClass);
		} catch (RuntimeException | LinkageError e) {
			LogManager.getLogger("Translator").warn("ModelBlock's texture names aren't as expected ({}): references stay 1.12.2's", e.toString());
			return basicClass;
		}
	}

	private static byte[] modelBlock(byte[] basicClass) {
		var node = new ClassNode();
		new ClassReader(basicClass).accept(node, 0);
		MethodNode resolve = null;
		for (MethodNode method : node.methods) {
			if ((method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)) == Opcodes.ACC_PUBLIC && "(Ljava/lang/String;)Ljava/lang/String;".equals(method.desc)) {
				if (resolve != null) {
					throw new IllegalStateException("two public methods (String)String");
				}
				resolve = method;
			}
		}
		if (resolve == null) {
			throw new IllegalStateException("no public method (String)String");
		}
		// return ModelTextureNames.resolve(this, name)
		var body = new InsnList();
		body.add(new VarInsnNode(Opcodes.ALOAD, 0));
		body.add(new VarInsnNode(Opcodes.ALOAD, 1));
		body.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "resolve", "(Ljava/lang/Object;Ljava/lang/String;)Ljava/lang/String;", false));
		body.add(new InsnNode(Opcodes.ARETURN));
		resolve.instructions = body;
		resolve.tryCatchBlocks.clear();
		resolve.localVariables = null;
		resolve.maxStack = 2;
		resolve.maxLocals = 2;
		var writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}
}
