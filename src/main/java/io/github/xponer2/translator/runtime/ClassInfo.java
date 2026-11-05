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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import xyz.wagyourtail.jvmdg.shade.asm.ClassReader;
import xyz.wagyourtail.jvmdg.shade.asm.ClassVisitor;
import xyz.wagyourtail.jvmdg.shade.asm.FieldVisitor;
import xyz.wagyourtail.jvmdg.shade.asm.MethodVisitor;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;

/** Name, supertypes and members of a class file (any class file version). */
final class ClassInfo {
	final String name;
	final String superName;
	final int access;
	final List<String> interfaces = new ArrayList<>();
	/** "name desc" of the methods. */
	final Set<String> methods = new HashSet<>();
	/** "name desc" of the abstract methods. */
	final Set<String> abstractMethods = new HashSet<>();
	/** "name desc" of the public instance fields (Vec3d.x: what a record accessor Vec3.x() reads). */
	final Set<String> publicInstanceFields = new HashSet<>();
	/** "name desc" of the final instance methods (not private): a subclass can't declare them. */
	final Set<String> finalMethods = new HashSet<>();
	/**
	 * "name desc" of the protected fields and methods: another package reaches them only through its own class's type
	 * (the guard casts a facade member's receiver to that class, see MethodGuard.receiverType).
	 */
	final Set<String> protectedMembers = new HashSet<>();
	/** "name desc" of the fields (a GETSTATIC resolves by both: 1.21 Enchantments.SILK_TOUCH is a key, not an Enchantment). */
	final Set<String> fields = new HashSet<>();
	/**
	 * "name desc" of the fields and methods whose (return) type is a type variable: {@code T menu} of
	 * {@code Screen<T extends Container>} is erased to Container, the mod's to its own bound.
	 */
	final Set<String> typeVariables = new HashSet<>();
	/** The classes a mixin class is applied to ({@code @Mixin} value and targets): its members are theirs. */
	final List<String> mixinTargets = new ArrayList<>();
	/** "owner name desc" of the methods its code calls (mod classes only, see {@link #readWithCalls}). */
	final Set<String> calls = new HashSet<>();

	private ClassInfo(String name, String superName, int access) {
		this.name = name;
		this.superName = superName;
		this.access = access;
	}

	static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";

	boolean isInterface() {
		return (this.access & Opcodes.ACC_INTERFACE) != 0;
	}

	static ClassInfo read(byte[] bytes) {
		return read(bytes, false);
	}

	/** With the methods the code calls: what a mod class's 1.21 code shows of the 1.21 API it was written against. */
	static ClassInfo readWithCalls(byte[] bytes) {
		return read(bytes, true);
	}

	private static ClassInfo read(byte[] bytes, boolean withCalls) {
		var holder = new ClassInfo[1];
		new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				holder[0] = new ClassInfo(name, superName, access);
				if (interfaces != null) {
					holder[0].interfaces.addAll(java.util.Arrays.asList(interfaces));
				}
			}

			@Override
			public xyz.wagyourtail.jvmdg.shade.asm.AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
				if (!MIXIN.equals(descriptor)) {
					return null;
				}
				return new xyz.wagyourtail.jvmdg.shade.asm.AnnotationVisitor(Opcodes.ASM9) {
					@Override
					public xyz.wagyourtail.jvmdg.shade.asm.AnnotationVisitor visitArray(String name) {
						return new xyz.wagyourtail.jvmdg.shade.asm.AnnotationVisitor(Opcodes.ASM9) {
							@Override
							public void visit(String ignored, Object value) {
								if (value instanceof xyz.wagyourtail.jvmdg.shade.asm.Type type) {
									holder[0].mixinTargets.add(type.getInternalName());
								} else if (value instanceof String target) {
									holder[0].mixinTargets.add(target.replace('.', '/'));
								}
							}
						};
					}
				};
			}

			@Override
			public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
				holder[0].fields.add(name + " " + descriptor);
				if ((access & Opcodes.ACC_PROTECTED) != 0) {
					holder[0].protectedMembers.add(name + " " + descriptor);
				}
				if ((access & (Opcodes.ACC_STATIC | Opcodes.ACC_PUBLIC)) == Opcodes.ACC_PUBLIC) {
					holder[0].publicInstanceFields.add(name + " " + descriptor);
				}
				if (signature != null && signature.startsWith("T")) {
					holder[0].typeVariables.add(name + " " + descriptor);
				}
				return null;
			}

			@Override
			public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
				holder[0].methods.add(name + " " + descriptor);
				if ((access & Opcodes.ACC_PROTECTED) != 0) {
					holder[0].protectedMembers.add(name + " " + descriptor);
				}
				if (signature != null && signature.charAt(signature.lastIndexOf(')') + 1) == 'T') {
					holder[0].typeVariables.add(name + " " + descriptor);
				}
				if ((access & Opcodes.ACC_ABSTRACT) != 0) {
					holder[0].abstractMethods.add(name + " " + descriptor);
				}
				if ((access & Opcodes.ACC_FINAL) != 0 && (access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) == 0) {
					holder[0].finalMethods.add(name + " " + descriptor);
				}
				return !withCalls ? null : new MethodVisitor(Opcodes.ASM9) {
					@Override
					public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
						holder[0].calls.add(owner + " " + name + " " + descriptor);
					}
				};
			}
		}, (withCalls ? 0 : ClassReader.SKIP_CODE) | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		return holder[0];
	}

	List<String> supertypes() {
		var out = new ArrayList<String>();
		if (this.superName != null) {
			out.add(this.superName);
		}
		out.addAll(this.interfaces);
		return out;
	}
}
