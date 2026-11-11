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
 * 1.21's own slot drawing of the mods' container screens: 1.21 draws each slot with {@code renderSlot}, which mods
 * override (Sophisticated's backpacks draw the counts of their big stacks, Mekanism its virtual slots). 1.12.2's
 * {@code GuiContainer.drawScreen} draws them with its private {@code drawSlot}, which no mod can override: its calls
 * there go to {@code client.SlotDrawing.drawSlot(screen, slot)}, which asks the mod's override and draws the 1.12.2
 * way for every other screen. The private method is found by its descriptor in the names of the running game.
 */
public final class SlotDrawing implements IClassTransformer {
	private static final String HOOK = "io/github/xponer2/translator/client/SlotDrawing";

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || !"net.minecraft.client.gui.inventory.GuiContainer".equals(transformedName)) {
			return basicClass;
		}
		try {
			return guiContainer(basicClass);
		} catch (RuntimeException | LinkageError e) {
			LogManager.getLogger("Translator").warn("GuiContainer.drawSlot isn't as expected ({}): the 1.21.1 mods' own slot drawing isn't used", e.toString());
			return basicClass;
		}
	}

	private static String unmap(String internal) {
		return net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper.INSTANCE.unmap(internal);
	}

	private static byte[] guiContainer(byte[] basicClass) {
		String slot = unmap("net/minecraft/inventory/Slot");
		String desc = "(L" + slot + ";)V";
		var node = new ClassNode();
		new ClassReader(basicClass).accept(node, 0);
		// in the class's own names (this runs before the deobfuscation, which renames them with the rest of the class)
		MethodNode drawSlot = null;
		for (MethodNode method : node.methods) {
			if ((method.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) == Opcodes.ACC_PRIVATE && desc.equals(method.desc)) {
				if (drawSlot != null) {
					throw new IllegalStateException("two private methods " + desc);
				}
				drawSlot = method;
			}
		}
		if (drawSlot == null) {
			throw new IllegalStateException("no private method " + desc);
		}
		int replaced = 0;
		for (MethodNode method : node.methods) {
			for (AbstractInsnNode insn : method.instructions.toArray()) {
				if (insn instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESPECIAL && call.owner.equals(node.name)
						&& call.name.equals(drawSlot.name) && call.desc.equals(desc)) {
					// the same stack (the screen, the slot): a static call in its place
					method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "drawSlot", "(L" + node.name + ";L" + slot + ";)V", false));
					replaced++;
				}
			}
		}
		if (replaced == 0) {
			throw new IllegalStateException("no call of " + drawSlot.name + desc);
		}
		var writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}
}
