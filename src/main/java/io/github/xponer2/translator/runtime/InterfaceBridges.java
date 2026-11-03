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
package io.github.xponer2.translator.runtime;

import xyz.wagyourtail.jvmdg.shade.asm.ClassReader;
import xyz.wagyourtail.jvmdg.shade.asm.ClassWriter;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;
import xyz.wagyourtail.jvmdg.shade.asm.Type;
import xyz.wagyourtail.jvmdg.shade.asm.tree.ClassNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.InsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode;

/**
 * The 1.12.2 methods of an interface a 1.21.1 mod class implements under its 1.21 method names (converter pre-pass):
 * a fluid handler (NeoForge's IFluidHandler is Forge's) has getTanks/getFluidInTank/fill(stack, action), the game asks
 * getTankProperties/fill(stack, doFill). The added methods call the mod's through {@link FluidHandlers}.
 */
final class InterfaceBridges {
	private static final String FLUID_HANDLER = "net/minecraftforge/fluids/capability/IFluidHandler";
	private static final String FLUID_HANDLER_ITEM = "net/minecraftforge/fluids/capability/IFluidHandlerItem";
	private static final String HELPER = "io/github/xponer2/translator/runtime/FluidHandlers";
	private static final String STACK = "Lnet/minecraftforge/fluids/FluidStack;";

	private InterfaceBridges() {
	}

	static byte[] add(byte[] bytes) {
		var reader = new ClassReader(bytes);
		String[] interfaces = reader.getInterfaces();
		boolean fluid = false;
		for (String itf : interfaces) {
			fluid |= FLUID_HANDLER.equals(itf) || FLUID_HANDLER_ITEM.equals(itf);
		}
		if (!fluid || (reader.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
			return bytes;
		}
		var node = new ClassNode();
		reader.accept(node, 0);
		boolean changed = false;
		changed |= bridge(node, "getTankProperties", "()[Lnet/minecraftforge/fluids/capability/IFluidTankProperties;", "properties");
		changed |= bridge(node, "fill", "(" + STACK + "Z)I", "fill");
		changed |= bridge(node, "drain", "(" + STACK + "Z)" + STACK, "drain");
		changed |= bridge(node, "drain", "(IZ)" + STACK, "drain");
		if (!changed) {
			return bytes;
		}
		var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		node.accept(writer);
		return writer.toByteArray();
	}

	/** A public method {name desc} calling the helper's {helperName (Object this, args)}, unless the class has it. */
	private static boolean bridge(ClassNode node, String name, String desc, String helperName) {
		for (MethodNode method : node.methods) {
			if (method.name.equals(name) && method.desc.equals(desc)) {
				return false;
			}
		}
		var method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, desc, null, null);
		method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
		int slot = 1;
		for (Type arg : Type.getArgumentTypes(desc)) {
			method.instructions.add(new VarInsnNode(arg.getOpcode(Opcodes.ILOAD), slot));
			slot += arg.getSize();
		}
		String helperDesc = "(Ljava/lang/Object;" + desc.substring(1);
		method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HELPER, helperName, helperDesc, false));
		method.instructions.add(new InsnNode(Type.getReturnType(desc).getOpcode(Opcodes.IRETURN)));
		node.methods.add(method);
		return true;
	}
}
