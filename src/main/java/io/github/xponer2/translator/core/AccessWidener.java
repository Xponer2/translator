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
 * EnumAttachPosition). What would change how the game runs stays: static final fields (Forge takes the public ones of
 * Blocks, Items, ... for object holders) and package-private methods (public, a subclass in another package with a
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
				if ("<init>".equals(name) || (access & HIDING) != 0 && !"<clinit>".equals(name)) {
					access = widen((access & Opcodes.ACC_PRIVATE) != 0 ? access & ~Opcodes.ACC_FINAL : access, changed);
				}
				return super.visitMethod(access, name, descriptor, signature, exceptions);
			}

			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				boolean constant = (access & Opcodes.ACC_STATIC) != 0 && (access & Opcodes.ACC_FINAL) != 0;
				return super.visitField(constant ? access : widen(access, changed), name, descriptor, signature, value);
			}
		}, 0);
		return changed[0] ? writer.toByteArray() : basicClass;
	}

	private static int widen(int access, boolean[] changed) {
		if ((access & Opcodes.ACC_PUBLIC) != 0) {
			return access;
		}
		changed[0] = true;
		return access & ~HIDING | Opcodes.ACC_PUBLIC;
	}
}
