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
import xyz.wagyourtail.jvmdg.shade.asm.ClassVisitor;
import xyz.wagyourtail.jvmdg.shade.asm.ClassWriter;
import xyz.wagyourtail.jvmdg.shade.asm.FieldVisitor;
import xyz.wagyourtail.jvmdg.shade.asm.MethodVisitor;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;

/**
 * Minecraft's classes and their members public (an access transformer for the 1.21.1 mods): the converted code
 * reaches 1.12.2 classes from other packages (the Translator's shadows of 1.21 classes, its own facades) and members
 * 1.21 has public (Minecraft.cameraEntity is the private renderViewEntity, MapColor's constructor, BlockRedstoneWire's
 * EnumAttachPosition). What would change how the game runs stays: the static final fields of net.minecraft.init (Forge
 * takes the public ones of Blocks, Items, ... for object holders) and package-private methods (public, a subclass in another package with a
 * method of that name would override them). Private methods stay called directly (invokespecial); they lose final, so
 * a subclass's method of the same name still loads.
 */
public final class AccessWidener implements IClassTransformer {
	private static final int HIDING = Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED;

	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || !transformedName.startsWith("net.minecraft.")) {
			return basicClass;
		}
		var reader = new ClassReader(basicClass);
		boolean[] changed = {false};
		var writer = new ClassWriter(reader, 0);
		reader.accept(new ClassVisitor(Opcodes.ASM9, writer) {
			private String self;

			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				this.self = name;
				if ("net.minecraft.item.Item".equals(transformedName) || "net.minecraft.block.Block".equals(transformedName)) {
					// every item and block is a 1.21 ItemLike: new ItemLike[] {Items.WHITE_WOOL} (Twilight Forest's
					// Questing Ram) threw ArrayStoreException, a cast to ItemLike ClassCastException
					interfaces = java.util.Arrays.copyOf(interfaces, interfaces.length + 1);
					interfaces[interfaces.length - 1] = "io/github/xponer2/translator/util/ItemLike";
					changed[0] = true;
				}
				if ("net.minecraft.util.ObjectIntIdentityMap".equals(transformedName)) {
					// a 1.21 IdMap (the block states' ids, the mods' IdMappers), as the Translator's registries are: one type
					// for both where 1.21 takes an IdMap (its default methods reach this class's own)
					interfaces = java.util.Arrays.copyOf(interfaces, interfaces.length + 1);
					interfaces[interfaces.length - 1] = "io/github/xponer2/translator/registry/IdMap";
					changed[0] = true;
				}
				super.visit(version, widen(access, changed), name, signature, superName, interfaces);
			}

			@Override
			public void visitInnerClass(String name, String outerName, String innerName, int access) {
				// the class's own entry (and its nested classes'): what reflection and the compiler see
				super.visitInnerClass(name, outerName, innerName,
						name.equals(this.self) || name.startsWith(this.self + "$") ? widen(access, changed) : access);
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				// package-private ones too (1.12.2 LanguageMap.getInstance(): the 1.21 Language.getInstance() mods call,
				// an IllegalAccessError in Jade's config codec)
				if ("<init>".equals(name) || (access & Opcodes.ACC_PUBLIC) == 0 && !"<clinit>".equals(name)) {
					access = widen((access & Opcodes.ACC_PRIVATE) != 0 ? access & ~Opcodes.ACC_FINAL : access, changed);
				}
				return super.visitMethod(access, name, descriptor, signature, exceptions);
			}

			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				// the static final fields of Blocks, Items, ... stay: Forge fills the public ones as object holders. Those
				// of other classes are widened: mods widen them with access transformers (BOP's boats read
				// EntityBoat.BOAT_TYPE, 1.21 Boat.DATA_ID_TYPE)
				boolean holder = (access & Opcodes.ACC_STATIC) != 0 && (access & Opcodes.ACC_FINAL) != 0
						&& transformedName.startsWith("net.minecraft.init.");
				if ((access & Opcodes.ACC_FINAL) != 0 && ASSIGNED.contains(transformedName + "." + name)) {
					// final on 1.12.2, assigned by 1.21.1 mods (Java 8 refuses the write: IllegalAccessError)
					access &= ~Opcodes.ACC_FINAL;
					changed[0] = true;
				}
				return super.visitField(holder ? access : widen(access, changed), name, descriptor, signature, value);
			}
		}, 0);
		return changed[0] ? writer.toByteArray() : basicClass;
	}

	/**
	 * Instance fields final on 1.12.2 that 1.21 has assignable, which mods set: Mob.lookControl is EntityLiving's final
	 * lookHelper (Iron's Spells' casting mobs set their own in the constructor: every one spawned crashed the server).
	 * Development and SRG names.
	 */
	private static final java.util.Set<String> ASSIGNED = new java.util.HashSet<>(java.util.Arrays.asList(
			"net.minecraft.entity.EntityLiving.lookHelper", "net.minecraft.entity.EntityLiving.field_70749_g"));

	private static int widen(int access, boolean[] changed) {
		if ((access & Opcodes.ACC_PUBLIC) != 0) {
			return access;
		}
		changed[0] = true;
		return access & ~HIDING | Opcodes.ACC_PUBLIC;
	}
}
