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
 * The Translator's banner on Forge's loading screen: in the splash thread's loop, before each frame is shown
 * (Display.update), client.TranslatorSplash.draw().
 */
public final class SplashBanner implements IClassTransformer {
	private static boolean off;

	/** Called by the splash thread: the banner, and if anything about it fails (its class too), never again. */
	public static void draw() {
		if (off) {
			return;
		}
		try {
			io.github.xponer2.translator.client.TranslatorSplash.draw();
		} catch (Throwable t) {
			off = true;
			org.apache.logging.log4j.LogManager.getLogger("Translator").warn("The loading screen banner is off", t);
		}
	}

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || transformedName == null || !transformedName.startsWith("net.minecraftforge.fml.client.SplashProgress$")) {
			return basicClass;
		}
		var node = new ClassNode();
		new ClassReader(basicClass).accept(node, 0);
		boolean changed = false;
		for (MethodNode method : node.methods) {
			if (!"run".equals(method.name) || !"()V".equals(method.desc)) {
				continue;
			}
			for (AbstractInsnNode insn : method.instructions.toArray()) {
				if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESTATIC && "org/lwjgl/opengl/Display".equals(call.owner)
						&& "update".equals(call.name) && "()V".equals(call.desc)) {
					method.instructions.insertBefore(call, new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/core/SplashBanner", "draw", "()V", false));
					changed = true;
				}
			}
		}
		if (!changed) {
			return basicClass;
		}
		org.apache.logging.log4j.LogManager.getLogger("Translator").info("Loading screen banner added to {}", transformedName);
		// a call that takes and leaves nothing on the stack: no frames to compute
		var writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}
}
