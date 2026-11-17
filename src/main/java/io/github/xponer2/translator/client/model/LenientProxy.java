/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.client.model;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import xyz.wagyourtail.jvmdg.shade.asm.ClassWriter;
import xyz.wagyourtail.jvmdg.shade.asm.MethodVisitor;
import xyz.wagyourtail.jvmdg.shade.asm.Opcodes;
import xyz.wagyourtail.jvmdg.shade.asm.Type;

/**
 * An interface implemented by a handler, as {@link java.lang.reflect.Proxy} does, without the cast of what a method
 * returns when that is an interface. A carried 1.21 interface returns carried interfaces 1.12.2's objects don't
 * implement: ModelBaker's {@code getModel} gives an UnbakedModel, and the model the mods cast it to (BlockModel) is
 * 1.12.2's ModelBlock. Proxy's cast failed; the bytecode verifier lets any object stand for an interface type.
 */
final class LenientProxy {
	private static final Map<Class<?>, Factory> FACTORIES = new ConcurrentHashMap<>();
	private static final AtomicInteger COUNTER = new AtomicInteger();

	private LenientProxy() {
	}

	static Object create(Class<?> type, InvocationHandler handler) throws ReflectiveOperationException {
		Factory factory = FACTORIES.get(type);
		if (factory == null) {
			factory = factory(type);
			FACTORIES.put(type, factory);
		}
		return factory.constructor.newInstance(handler);
	}

	private static final class Factory {
		final java.lang.reflect.Constructor<?> constructor;

		Factory(java.lang.reflect.Constructor<?> constructor) {
			this.constructor = constructor;
		}
	}

	private static Factory factory(Class<?> type) throws ReflectiveOperationException {
		// every public instance method of the interface and its parents, one per name and descriptor
		Map<String, Method> methods = new LinkedHashMap<>();
		for (Method method : type.getMethods()) {
			if (!Modifier.isStatic(method.getModifiers()) && method.getDeclaringClass().isInterface()) {
				methods.putIfAbsent(method.getName() + Type.getMethodDescriptor(method), method);
			}
		}
		List<Method> list = new ArrayList<>(methods.values());
		String name = "io/github/xponer2/translator/client/model/LenientProxy$" + type.getSimpleName() + COUNTER.incrementAndGet();
		String handler = Type.getInternalName(InvocationHandler.class);
		String methodType = Type.getDescriptor(Method.class);

		ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
		writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER, name, null, "java/lang/Object",
				new String[] {Type.getInternalName(type)});
		writer.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "h", "L" + handler + ";", null, null).visitEnd();
		writer.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "m", "[" + methodType, null, null).visitEnd();

		MethodVisitor init = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(L" + handler + ";)V", null, null);
		init.visitCode();
		init.visitVarInsn(Opcodes.ALOAD, 0);
		init.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		init.visitVarInsn(Opcodes.ALOAD, 0);
		init.visitVarInsn(Opcodes.ALOAD, 1);
		init.visitFieldInsn(Opcodes.PUTFIELD, name, "h", "L" + handler + ";");
		init.visitInsn(Opcodes.RETURN);
		init.visitMaxs(0, 0);
		init.visitEnd();

		for (int index = 0; index < list.size(); index++) {
			Method method = list.get(index);
			MethodVisitor code = writer.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), Type.getMethodDescriptor(method), null, null);
			code.visitCode();
			code.visitVarInsn(Opcodes.ALOAD, 0);
			code.visitFieldInsn(Opcodes.GETFIELD, name, "h", "L" + handler + ";");
			code.visitVarInsn(Opcodes.ALOAD, 0);
			code.visitFieldInsn(Opcodes.GETSTATIC, name, "m", "[" + methodType);
			code.visitLdcInsn(index);
			code.visitInsn(Opcodes.AALOAD);
			Class<?>[] parameters = method.getParameterTypes();
			code.visitLdcInsn(parameters.length);
			code.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
			int slot = 1;
			for (int i = 0; i < parameters.length; i++) {
				Type parameter = Type.getType(parameters[i]);
				code.visitInsn(Opcodes.DUP);
				code.visitLdcInsn(i);
				code.visitVarInsn(parameter.getOpcode(Opcodes.ILOAD), slot);
				box(code, parameter);
				code.visitInsn(Opcodes.AASTORE);
				slot += parameter.getSize();
			}
			code.visitMethodInsn(Opcodes.INVOKEINTERFACE, handler, "invoke", "(Ljava/lang/Object;" + methodType + "[Ljava/lang/Object;)Ljava/lang/Object;",
					true);
			Class<?> returned = method.getReturnType();
			Type returnType = Type.getType(returned);
			if (returned == void.class) {
				code.visitInsn(Opcodes.POP);
				code.visitInsn(Opcodes.RETURN);
			} else if (returned.isPrimitive()) {
				unbox(code, returnType);
				code.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
			} else {
				// the leniency: no cast to an interface (nor to Object)
				if (!returned.isInterface() && returned != Object.class) {
					code.visitTypeInsn(Opcodes.CHECKCAST, returnType.getInternalName());
				}
				code.visitInsn(Opcodes.ARETURN);
			}
			code.visitMaxs(0, 0);
			code.visitEnd();
		}
		writer.visitEnd();

		Class<?> made = new Definer(type.getClassLoader()).define(name.replace('/', '.'), writer.toByteArray());
		made.getField("m").set(null, list.toArray(new Method[0]));
		return new Factory(made.getConstructor(InvocationHandler.class));
	}

	private static void box(MethodVisitor code, Type type) {
		String wrapper = wrapper(type);
		if (wrapper != null) {
			code.visitMethodInsn(Opcodes.INVOKESTATIC, wrapper, "valueOf", "(" + type.getDescriptor() + ")L" + wrapper + ";", false);
		}
	}

	private static void unbox(MethodVisitor code, Type type) {
		String wrapper = wrapper(type);
		code.visitTypeInsn(Opcodes.CHECKCAST, wrapper);
		code.visitMethodInsn(Opcodes.INVOKEVIRTUAL, wrapper, type.getClassName() + "Value", "()" + type.getDescriptor(), false);
	}

	private static String wrapper(Type type) {
		switch (type.getSort()) {
			case Type.BOOLEAN:
				return "java/lang/Boolean";
			case Type.CHAR:
				return "java/lang/Character";
			case Type.BYTE:
				return "java/lang/Byte";
			case Type.SHORT:
				return "java/lang/Short";
			case Type.INT:
				return "java/lang/Integer";
			case Type.FLOAT:
				return "java/lang/Float";
			case Type.LONG:
				return "java/lang/Long";
			case Type.DOUBLE:
				return "java/lang/Double";
			default:
				return null;
		}
	}

	private static final class Definer extends ClassLoader {
		Definer(ClassLoader parent) {
			super(parent);
		}

		Class<?> define(String name, byte[] bytes) {
			return this.defineClass(name, bytes, 0, bytes.length);
		}
	}
}
