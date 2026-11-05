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
 * 1.21's item click behaviour in every menu: 1.21 {@code AbstractContainerMenu.doClick} first asks the carried item
 * ({@code overrideStackedOnOther}) and the clicked one ({@code overrideOtherStackedOnMe}) whether they take the click
 * themselves (a backpack takes the item clicked onto it, a bundle). 1.12.2's {@code Container.slotClick} has no such
 * step: it starts with {@code menu.ClickBridges.overridesClick}, and returns what 1.12.2 returns for the click when an
 * item took it. The method is found by its descriptor in the names of the running game (obfuscated or not).
 */
public final class MenuClicks implements IClassTransformer {
	private static final String BRIDGES = "io/github/xponer2/translator/menu/ClickBridges";

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || !"net.minecraft.inventory.Container".equals(transformedName)) {
			return basicClass;
		}
		try {
			return container(basicClass);
		} catch (RuntimeException | LinkageError e) {
			LogManager.getLogger("Translator").warn("Container.slotClick isn't as expected ({}): items of the 1.21.1 mods don't take clicks onto them in 1.12.2's menus",
					e.toString());
			return basicClass;
		}
	}

	private static String unmap(String internal) {
		return net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper.INSTANCE.unmap(internal);
	}

	private static byte[] container(byte[] basicClass) {
		String desc = "(IIL" + unmap("net/minecraft/inventory/ClickType") + ";L" + unmap("net/minecraft/entity/player/EntityPlayer") + ";)L"
				+ unmap("net/minecraft/item/ItemStack") + ";";
		var node = new ClassNode();
		new ClassReader(basicClass).accept(node, 0);
		MethodNode slotClick = null;
		for (MethodNode method : node.methods) {
			if ((method.access & Opcodes.ACC_STATIC) == 0 && desc.equals(method.desc)) {
				slotClick = slotClick == null ? method : null;
			}
		}
		if (slotClick == null) {
			throw new IllegalStateException("no single method " + desc);
		}
		// in the class's own names (this runs before the deobfuscation, which renames them with the rest of the class)
		InsnList head = new InsnList();
		LabelNode normal = new LabelNode();
		head.add(new VarInsnNode(Opcodes.ALOAD, 0));
		head.add(new VarInsnNode(Opcodes.ILOAD, 1));
		head.add(new VarInsnNode(Opcodes.ILOAD, 2));
		head.add(new VarInsnNode(Opcodes.ALOAD, 3));
		head.add(new VarInsnNode(Opcodes.ALOAD, 4));
		head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGES, "overridesClick",
				"(L" + node.name + ";IIL" + unmap("net/minecraft/inventory/ClickType") + ";L" + unmap("net/minecraft/entity/player/EntityPlayer") + ";)Z", false));
		head.add(new JumpInsnNode(Opcodes.IFEQ, normal));
		head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, BRIDGES, "clickResult", "()L" + unmap("net/minecraft/item/ItemStack") + ";", false));
		head.add(new InsnNode(Opcodes.ARETURN));
		head.add(normal);
		head.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
		slotClick.instructions.insert(head);
		slotClick.maxStack = Math.max(slotClick.maxStack, 5);
		var writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}
}
