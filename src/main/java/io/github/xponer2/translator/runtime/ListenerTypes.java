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

import xyz.wagyourtail.jvmdg.shade.asm.ClassVisitor;
import xyz.wagyourtail.jvmdg.shade.asm.Handle;
import xyz.wagyourtail.jvmdg.shade.asm.MethodVisitor;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;
import xyz.wagyourtail.jvmdg.shade.asm.Type;

/**
 * NeoForge finds the event type of {@code bus.addListener(this::onEvent)} from the lambda's generic type at runtime.
 * Here it comes from the bytecode: the lambda created just before the call names its event parameter, and the call
 * becomes the Translator overload with that class as the last argument. Runs on the 1.21 names, before remapping.
 */
final class ListenerTypes extends ClassVisitor {
	private static final String BUS = "net/neoforged/bus/api/IEventBus";

	ListenerTypes(ClassVisitor next) {
		super(Opcodes.ASM9, next);
	}

	@Override
	public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
		return new MethodVisitor(Opcodes.ASM9, super.visitMethod(access, name, descriptor, signature, exceptions)) {
			private Type lastEvent;
			// a listener kept in a local first (Registrate: Consumer<RegisterEvent> onRegister = this::onRegister;
			// bus.addListener(EventPriority.LOWEST, onRegister)): its event type, by local
			private final java.util.Map<Integer, Type> locals = new java.util.HashMap<>();

			@Override
			public void visitVarInsn(int opcode, int var) {
				if (opcode == Opcodes.ASTORE) {
					if (this.lastEvent != null) {
						this.locals.put(var, this.lastEvent);
					} else {
						this.locals.remove(var);
					}
					this.lastEvent = null;
				} else if (opcode == Opcodes.ALOAD) {
					this.lastEvent = this.locals.get(var);
				}
				super.visitVarInsn(opcode, var);
			}

			@Override
			public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
				// the priority before the listener (GETSTATIC EventPriority.LOWEST) keeps the listener's type
				if (!(opcode == Opcodes.GETSTATIC && descriptor.endsWith("/EventPriority;"))) {
					this.lastEvent = null;
				}
				super.visitFieldInsn(opcode, owner, name, descriptor);
			}

			@Override
			public void visitInvokeDynamicInsn(String name, String descriptor, Handle bootstrap, Object... args) {
				this.lastEvent = null;
				if ("java/lang/invoke/LambdaMetafactory".equals(bootstrap.getOwner())
						&& "Ljava/util/function/Consumer;".equals(Type.getReturnType(descriptor).getDescriptor())
						&& args.length >= 3 && args[2] instanceof Type instantiated) {
					Type[] params = instantiated.getArgumentTypes();
					if (params.length == 1 && params[0].getSort() == Type.OBJECT) {
						this.lastEvent = params[0];
					}
				}
				super.visitInvokeDynamicInsn(name, descriptor, bootstrap, args);
			}

			@Override
			public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
				if (BUS.equals(owner) && "addListener".equals(name) && descriptor.endsWith("Ljava/util/function/Consumer;)V")
						&& !descriptor.contains("Ljava/lang/Class;") && this.lastEvent != null) {
					super.visitLdcInsn(this.lastEvent);
					descriptor = descriptor.replace("Ljava/util/function/Consumer;)V", "Ljava/util/function/Consumer;Ljava/lang/Class;)V");
				}
				this.lastEvent = null;
				super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
			}
		};
	}
}
