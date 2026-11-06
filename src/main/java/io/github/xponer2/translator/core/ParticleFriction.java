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
import xyz.wagyourtail.jvmdg.shade.asm.tree.LdcInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodInsnNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.MethodNode;
import xyz.wagyourtail.jvmdg.shade.asm.tree.VarInsnNode;

/**
 * 1.21's particle friction in 1.12.2's {@code Particle.onUpdate}: its three {@code motion *= 0.98} take the particle's
 * own friction (1.21 {@code Particle.friction}, which a particle sets in its constructor and the Translator keeps in
 * {@code ParticleFields}). Without it every 1.21 particle moved with 1.12.2's 0.98 (Biomes O' Plenty's falling leaves,
 * the snowflakes and spells of the carried 1.21 particles).
 */
public final class ParticleFriction implements IClassTransformer {
	private static final double VANILLA = 0.9800000190734863D;

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || !"net.minecraft.client.particle.Particle".equals(transformedName)) {
			return basicClass;
		}
		try {
			return particle(basicClass);
		} catch (RuntimeException | LinkageError e) {
			LogManager.getLogger("Translator").warn("Particle.onUpdate isn't as expected ({}): the particles of the 1.21.1 mods move with 1.12.2's friction",
					e.toString());
			return basicClass;
		}
	}

	private static byte[] particle(byte[] basicClass) {
		var node = new ClassNode();
		new ClassReader(basicClass).accept(node, 0);
		MethodNode update = null;
		for (MethodNode method : node.methods) {
			if ((method.access & Opcodes.ACC_STATIC) != 0 || !"()V".equals(method.desc) || frictionConstants(method) != 3) {
				continue;
			}
			if (update != null) {
				throw new IllegalStateException("more than one method with three motion *= 0.98");
			}
			update = method;
		}
		if (update == null) {
			throw new IllegalStateException("no method with three motion *= 0.98");
		}
		for (AbstractInsnNode insn : update.instructions.toArray()) {
			if (isFriction(insn)) {
				var factor = new InsnList();
				factor.add(new VarInsnNode(Opcodes.ALOAD, 0));
				factor.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/runtime/redirect/ParticleFields", "motionFactor",
						"(Ljava/lang/Object;)D", false));
				update.instructions.insertBefore(insn, factor);
				update.instructions.remove(insn);
			}
		}
		update.maxStack += 1;
		var writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}

	private static int frictionConstants(MethodNode method) {
		int count = 0;
		for (AbstractInsnNode insn : method.instructions.toArray()) {
			if (isFriction(insn)) {
				count++;
			}
		}
		return count;
	}

	private static boolean isFriction(AbstractInsnNode insn) {
		return insn instanceof LdcInsnNode && ((LdcInsnNode) insn).cst instanceof Double && (Double) ((LdcInsnNode) insn).cst == VANILLA;
	}
}
