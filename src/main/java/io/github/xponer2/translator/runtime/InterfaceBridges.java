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
	private static final String GROWABLE = "net/minecraft/block/IGrowable";
	private static final String WORLD = "Lnet/minecraft/world/World;";
	private static final String POS = "Lnet/minecraft/util/math/BlockPos;";
	private static final String STATE = "Lnet/minecraft/block/state/IBlockState;";
	private static final String RANDOM = "Ljava/util/Random;";
	private static final String COMPLEX_SPAWN = "net/neoforged/neoforge/entity/IEntityWithComplexSpawn";
	private static final String ADDITIONAL_SPAWN = "net/minecraftforge/fml/common/registry/IEntityAdditionalSpawnData";
	private static final String FRIENDLY = "io/github/xponer2/translator/network/RegistryFriendlyByteBuf";
	private static final String NETTY = "io/netty/buffer/ByteBuf";
	private static final String SPAWNS = "io/github/xponer2/translator/runtime/redirect/EntityGapBridges";

	private InterfaceBridges() {
	}

	static byte[] add(byte[] bytes) {
		var reader = new ClassReader(bytes);
		String[] interfaces = reader.getInterfaces();
		boolean fluid = false;
		boolean growable = false;
		boolean spawn = false;
		for (String itf : interfaces) {
			fluid |= FLUID_HANDLER.equals(itf) || FLUID_HANDLER_ITEM.equals(itf);
			growable |= GROWABLE.equals(itf);
			spawn |= COMPLEX_SPAWN.equals(itf);
		}
		// a 1.21 entity that reads its spawn packet's data (recreateFromPacket): bridged below unless it sends its own
		boolean packet = !spawn && (reader.getAccess() & Opcodes.ACC_INTERFACE) == 0
				&& new String(bytes, java.nio.charset.StandardCharsets.ISO_8859_1).contains("recreateFromPacket");
		if (!(fluid || growable || spawn || packet) || (reader.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
			return bytes;
		}
		var node = new ClassNode();
		reader.accept(node, 0);
		boolean changed = false;
		if (fluid) {
			changed |= bridge(node, "getTankProperties", "()[Lnet/minecraftforge/fluids/capability/IFluidTankProperties;", "properties");
			changed |= bridge(node, "fill", "(" + STACK + "Z)I", "fill");
			changed |= bridge(node, "drain", "(" + STACK + "Z)" + STACK, "drain");
			changed |= bridge(node, "drain", "(IZ)" + STACK, "drain");
		}
		if (growable) {
			// 1.21 BonemealableBlock under 1.12.2's IGrowable: isValidBonemealTarget has no isClient, performBonemeal a
			// server level. The game asked canGrow, which the converter made a stub (false): bone meal did nothing
			changed |= self(node, "canGrow", "(" + WORLD + POS + STATE + "Z)Z", "isValidBonemealTarget", "(" + WORLD + POS + STATE + ")Z", false);
			changed |= self(node, "grow", "(" + WORLD + RANDOM + POS + STATE + ")V", "performBonemeal",
					"(Lnet/minecraft/world/WorldServer;" + RANDOM + POS + STATE + ")V", true);
			changed |= self(node, "canUseBonemeal", "(" + WORLD + RANDOM + POS + STATE + ")Z", "isBonemealSuccess", "(" + WORLD + RANDOM + POS + STATE + ")Z",
					false);
		}
		if (spawn) {
			// NeoForge's IEntityWithComplexSpawn (the entity's own data sent with its spawn, through a registry friendly
			// buffer) as Forge's IEntityAdditionalSpawnData, which 1.12.2's FML spawn message writes and reads: the
			// mods' entities came to the client without it (Iron's Spells' spells and bosses, Create's contraptions and
			// seats, Twilight Forest's chain blocks)
			boolean wrote = spawnData(node, "writeSpawnData");
			boolean read = spawnData(node, "readSpawnData");
			if ((wrote || read) && !node.interfaces.contains(ADDITIONAL_SPAWN)) {
				node.interfaces.add(ADDITIONAL_SPAWN);
			}
			changed |= wrote || read;
		}
		if (packet && declares(node, "recreateFromPacket", "(Lnet/minecraft/network/play/server/SPacketSpawnObject;)V")
				&& !node.interfaces.contains(ADDITIONAL_SPAWN)) {
			// the data of 1.21's spawn packet (a falling block's state, a painting's variant: Twilight Forest's falling ice,
			// thrown blocks and magic paintings) with FML's spawn message, read back by the entity's recreateFromPacket
			var write = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "writeSpawnData", "(L" + NETTY + ";)V", null, null);
			write.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
			write.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
			write.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SPAWNS, "spawnPacketData", "(Ljava/lang/Object;)I", false));
			write.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, NETTY, "writeInt", "(I)L" + NETTY + ";", false));
			write.instructions.add(new InsnNode(Opcodes.POP));
			write.instructions.add(new InsnNode(Opcodes.RETURN));
			var read = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, "readSpawnData", "(L" + NETTY + ";)V", null, null);
			read.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
			read.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
			read.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, NETTY, "readInt", "()I", false));
			read.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, SPAWNS, "recreateFromSpawnData", "(Ljava/lang/Object;I)V", false));
			read.instructions.add(new InsnNode(Opcodes.RETURN));
			if (!declares(node, "writeSpawnData", "(L" + NETTY + ";)V") && !declares(node, "readSpawnData", "(L" + NETTY + ";)V")) {
				node.methods.add(write);
				node.methods.add(read);
				node.interfaces.add(ADDITIONAL_SPAWN);
				changed = true;
			}
		}
		if (!changed) {
			return bytes;
		}
		var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		node.accept(writer);
		return writer.toByteArray();
	}

	/**
	 * A public method {name desc} calling the class's own {target targetDesc} with the leading arguments (the world
	 * cast to a server world, skipped on the client, when {@code serverWorld}), when the class has the target and not
	 * the method.
	 */
	private static boolean self(ClassNode node, String name, String desc, String target, String targetDesc, boolean serverWorld) {
		boolean hasTarget = false;
		for (MethodNode method : node.methods) {
			if (method.name.equals(name) && method.desc.equals(desc)) {
				return false;
			}
			hasTarget |= method.name.equals(target) && method.desc.equals(targetDesc) && (method.access & Opcodes.ACC_STATIC) == 0;
		}
		if (!hasTarget) {
			return false;
		}
		var method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, desc, null, null);
		var skip = new xyz.wagyourtail.jvmdg.shade.asm.tree.LabelNode();
		if (serverWorld) {
			method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
			method.instructions.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.TypeInsnNode(Opcodes.INSTANCEOF, "net/minecraft/world/WorldServer"));
			method.instructions.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.JumpInsnNode(Opcodes.IFEQ, skip));
		}
		method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
		Type[] args = Type.getArgumentTypes(desc);
		Type[] targetArgs = Type.getArgumentTypes(targetDesc);
		int slot = 1;
		for (int i = 0; i < targetArgs.length; i++) {
			method.instructions.add(new VarInsnNode(args[i].getOpcode(Opcodes.ILOAD), slot));
			if (i == 0 && serverWorld) {
				method.instructions.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.TypeInsnNode(Opcodes.CHECKCAST, "net/minecraft/world/WorldServer"));
			}
			slot += args[i].getSize();
		}
		method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, node.name, target, targetDesc, false));
		Type returns = Type.getReturnType(desc);
		method.instructions.add(new InsnNode(returns.getOpcode(Opcodes.IRETURN)));
		if (serverWorld) {
			method.instructions.add(skip);
			method.instructions.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.FrameNode(Opcodes.F_SAME, 0, null, 0, null));
			method.instructions.add(new InsnNode(Opcodes.RETURN));
		}
		node.methods.add(method);
		return true;
	}

	/**
	 * {@code name(ByteBuf)} (Forge's) calling the class's {@code name(RegistryFriendlyByteBuf)} (NeoForge's) on the
	 * buffer wrapped, when the class declares the NeoForge one and not the Forge one.
	 */
	private static boolean spawnData(ClassNode node, String name) {
		String own = "(L" + FRIENDLY + ";)V";
		String forge = "(L" + NETTY + ";)V";
		boolean declares = false;
		for (MethodNode method : node.methods) {
			if (method.name.equals(name) && method.desc.equals(forge)) {
				return false;
			}
			declares |= method.name.equals(name) && method.desc.equals(own) && (method.access & Opcodes.ACC_STATIC) == 0;
		}
		if (!declares) {
			return false;
		}
		var method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, name, forge, null, null);
		method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
		method.instructions.add(new xyz.wagyourtail.jvmdg.shade.asm.tree.TypeInsnNode(Opcodes.NEW, FRIENDLY));
		method.instructions.add(new InsnNode(Opcodes.DUP));
		method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
		method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, FRIENDLY, "<init>", forge, false));
		method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, node.name, name, own, false));
		method.instructions.add(new InsnNode(Opcodes.RETURN));
		node.methods.add(method);
		return true;
	}

	private static boolean declares(ClassNode node, String name, String desc) {
		for (MethodNode method : node.methods) {
			if (method.name.equals(name) && method.desc.equals(desc) && (method.access & Opcodes.ACC_STATIC) == 0) {
				return true;
			}
		}
		return false;
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
