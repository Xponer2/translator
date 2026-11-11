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
package io.github.xponer2.translator.client;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;

/**
 * The slots of a container screen drawn by 1.12.2's {@code GuiContainer.drawScreen} (see {@code core.SlotDrawing}): a
 * 1.21.1 mod's screen that overrides {@code renderSlot} draws them itself, as in 1.21; every other screen the 1.12.2
 * way. Client only.
 */
public final class SlotDrawing {
	/** 1.12.2's private {@code GuiContainer.drawSlot(slot)}. */
	@Nullable
	private static final MethodHandle DRAW_SLOT = drawSlotHandle();

	/** Whether a screen class (below the facade) declares its own renderSlot. */
	private static final ClassValue<Boolean> OWN_SLOTS = new ClassValue<>() {
		@Override
		protected Boolean computeValue(Class<?> type) {
			for (Class<?> c = type; c != null && c != AbstractContainerScreen.class && c != GuiContainer.class; c = c.getSuperclass()) {
				try {
					c.getDeclaredMethod("renderSlot", GuiGraphics.class, Slot.class);
					return true;
				} catch (NoSuchMethodException | LinkageError e) {
					// not this class: its superclass
				}
			}
			return false;
		}
	};

	private SlotDrawing() {
	}

	public static void drawSlot(GuiContainer screen, Slot slot) {
		if (screen instanceof AbstractContainerScreen<?> facade && OWN_SLOTS.get(screen.getClass()) && facade.renderSlotOfMod(slot)) {
			return;
		}
		drawVanilla(screen, slot);
	}

	/** 1.12.2's own slot drawing (AbstractContainerScreen.renderSlot draws a slot so too). */
	static void drawVanilla(GuiContainer screen, Slot slot) {
		if (DRAW_SLOT == null) {
			return;
		}
		try {
			DRAW_SLOT.invokeExact(screen, slot);
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable t) {
			throw new IllegalStateException(t);
		}
	}

	@Nullable
	private static MethodHandle drawSlotHandle() {
		// found by its form (private, void, one Slot), so the game's obfuscated name and the development one both work
		for (Method method : GuiContainer.class.getDeclaredMethods()) {
			if (Modifier.isPrivate(method.getModifiers()) && !Modifier.isStatic(method.getModifiers()) && method.getReturnType() == void.class
					&& method.getParameterCount() == 1 && method.getParameterTypes()[0] == Slot.class) {
				try {
					method.setAccessible(true);
					return MethodHandles.lookup().unreflect(method);
				} catch (ReflectiveOperationException | RuntimeException e) {
					return null;
				}
			}
		}
		return null;
	}

	/** A GuiGraphics for the slot drawing of this frame. */
	static GuiGraphics graphics() {
		return new GuiGraphics(Minecraft.getMinecraft());
	}
}
