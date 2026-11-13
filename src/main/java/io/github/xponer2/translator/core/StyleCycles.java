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

import java.lang.reflect.Field;

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
 * A text style is never its own parent. 1.12.2's styles are changed in place and inherit what they leave unset from
 * their parent; 1.21 styles are values a mod shares freely (Style.EMPTY in many components), and a shared style put
 * under a component of the same style became its own ancestor: every later look at the chain (getBold) recursed until
 * the stack overflowed (Twilight Forest's armor tooltips). {@code setParentStyle} now leaves a parent that would close a
 * loop out.
 */
public final class StyleCycles implements IClassTransformer {
	@Override
	public byte[] transform(String name, String transformedName, byte[] basicClass) {
		if (basicClass == null || !"net.minecraft.util.text.Style".equals(transformedName)) {
			return basicClass;
		}
		try {
			return style(basicClass);
		} catch (RuntimeException | LinkageError e) {
			LogManager.getLogger("Translator").warn("Style isn't as expected ({}): a style loop still overflows the stack", e.toString());
			return basicClass;
		}
	}

	private static byte[] style(byte[] basicClass) {
		var node = new ClassNode();
		new ClassReader(basicClass).accept(node, 0);
		String self = node.name;
		String desc = "(L" + self + ";)L" + self + ";";
		MethodNode setParent = null;
		for (MethodNode method : node.methods) {
			if ((method.access & Opcodes.ACC_STATIC) == 0 && desc.equals(method.desc) && !method.name.startsWith("<")) {
				if (setParent != null) {
					throw new IllegalStateException("two methods " + desc);
				}
				setParent = method;
			}
		}
		if (setParent == null) {
			throw new IllegalStateException("no method " + desc);
		}
		// if (StyleCycles.loops(this, parent)) return this;
		var guard = new InsnList();
		var go = new LabelNode();
		guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
		guard.add(new VarInsnNode(Opcodes.ALOAD, 1));
		guard.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "io/github/xponer2/translator/core/StyleCycles", "loops", "(Ljava/lang/Object;Ljava/lang/Object;)Z",
				false));
		guard.add(new JumpInsnNode(Opcodes.IFEQ, go));
		guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
		guard.add(new InsnNode(Opcodes.ARETURN));
		guard.add(go);
		guard.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
		setParent.instructions.insert(guard);
		setParent.maxStack = Math.max(setParent.maxStack, 2);
		var writer = new ClassWriter(0);
		node.accept(writer);
		return writer.toByteArray();
	}

	private static volatile Field parentField;

	/** Whether the style would be its own ancestor with this parent (a very long chain counts as one too). */
	public static boolean loops(Object style, Object parent) {
		if (parent == null) {
			return false;
		}
		if (parent == style) {
			return true;
		}
		Field field = parentField(style.getClass());
		if (field == null) {
			return false;
		}
		try {
			int depth = 0;
			for (Object at = field.get(parent); at != null; at = field.get(at)) {
				if (at == style || ++depth > 256) {
					return true;
				}
			}
		} catch (IllegalAccessException e) {
			return false;
		}
		return false;
	}

	private static Field parentField(Class<?> type) {
		Field field = parentField;
		if (field != null) {
			return field;
		}
		for (Class<?> at = type; at != null && at != Object.class; at = at.getSuperclass()) {
			for (String name : new String[] { "field_150249_a", "parentStyle" }) {
				try {
					field = at.getDeclaredField(name);
					field.setAccessible(true);
					parentField = field;
					return field;
				} catch (NoSuchFieldException | RuntimeException e) {
					// the other name, or the class above
				}
			}
		}
		return null;
	}
}
