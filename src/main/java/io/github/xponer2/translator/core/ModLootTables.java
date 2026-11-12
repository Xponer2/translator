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
import xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode;

/**
 * The 1.21 loot tables of the mods through 1.12.2's {@code LootTableManager.getLootTableFromLocation} (found by its
 * descriptor: development, SRG and obfuscated names alike): a table it
 * doesn't find (1.12.2 reads only {@code loot_tables} of its own format) is the mod's {@code data/<mod>/loot_table}
 * one when there is one ({@code JsonLootTable.orModTable}). Vanilla chests the mods' structures fill
 * ({@code setLootTable(key, seed)} of a 1.12.2 chest) were opened empty.
 */
public final class ModLootTables implements IClassTransformer {
	private static final String MANAGER = "net.minecraft.world.storage.loot.LootTableManager";

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || !MANAGER.equals(transformedName)) {
			return basicClass;
		}
		try {
			return manager(basicClass);
		} catch (RuntimeException | LinkageError e) {
			LogManager.getLogger("Translator").warn("LootTableManager isn't as expected ({}): chests of the 1.21.1 mods' tables stay empty", e.toString());
			return basicClass;
		}
	}

	/** A class's name as the bytes have it now (this runs before the deobfuscation, which renames the whole class). */
	private static String unmap(String internal) {
		return net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper.INSTANCE.unmap(internal);
	}

	private static byte[] manager(byte[] basicClass) {
		String table = "L" + unmap("net/minecraft/world/storage/loot/LootTable") + ";";
		String id = "L" + unmap("net/minecraft/util/ResourceLocation") + ";";
		String getDesc = "(" + id + ")" + table;
		var node = new ClassNode();
		new ClassReader(basicClass).accept(node, 0);
		MethodNode get = null;
		for (MethodNode method : node.methods) {
			if ((method.access & Opcodes.ACC_STATIC) == 0 && getDesc.equals(method.desc) && !method.name.startsWith("<")) {
				if (get != null) {
					throw new IllegalStateException("more than one getLootTableFromLocation");
				}
				get = method;
			}
		}
		if (get == null) {
			throw new IllegalStateException("no getLootTableFromLocation");
		}
		for (AbstractInsnNode insn : get.instructions.toArray()) {
			if (insn.getOpcode() == Opcodes.ARETURN) {
				var hook = new InsnList();
				hook.add(new VarInsnNode(Opcodes.ALOAD, 1));
				// in the class's own names: the deobfuscation renames this call's descriptor with the rest
				hook.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/loot/JsonLootTable", "orModTable",
						"(" + table + id + ")" + table, false));
				get.instructions.insertBefore(insn, hook);
			}
		}
		get.maxStack += 1;
		var writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}
}
