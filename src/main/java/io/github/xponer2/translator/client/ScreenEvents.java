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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import io.github.xponer2.translator.runtime.neoforge.KeyMapping;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * NeoForge's screen events (the carried 1.21 {@code ScreenEvent}s) from 1.12.2 Forge's: a screen opening or closing,
 * its init, its drawing and background, the mouse (pressed, released, dragged, scrolled) and the keyboard (pressed,
 * released, typed), a container screen's labels drawn. None was posted: JEI's overlay (it builds and draws itself on
 * these), Mouse Tweaks (its drag and wheel tweaks are mouse events) and Controlling (it swaps the controls screen when
 * it opens) did nothing, on any screen. A Pre event a mod cancels cancels 1.12.2's own handling.
 */
@SideOnly(Side.CLIENT)
public final class ScreenEvents {
	private static final String EVENTS = "net.neoforged.neoforge.client.event.";
	private static final Map<String, Constructor<?>> CONSTRUCTORS = new ConcurrentHashMap<>();
	private static final java.util.Set<String> MISSING = ConcurrentHashMap.newKeySet();

	/** The button pressed last (for drags) and where the mouse was then. */
	private int dragButton = -1;
	private double lastX;
	private double lastY;

	public static void register() {
		MinecraftForge.EVENT_BUS.register(new ScreenEvents());
	}

	// --- opening, closing, init -----------------------------------------------------------------------------------------

	@SubscribeEvent(priority = EventPriority.HIGH)
	public void onOpen(GuiOpenEvent event) {
		GuiScreen current = Minecraft.getMinecraft().currentScreen;
		GuiScreen next = event.getGui();
		if (next == null) {
			if (current != null) {
				post("ScreenEvent$Closing", current);
			}
			return;
		}
		Event opening = post("ScreenEvent$Opening", current, next);
		if (opening == null) {
			return;
		}
		if (opening.isCanceled()) {
			event.setCanceled(true);
			return;
		}
		Object replaced = call(opening, "getNewScreen");
		if (replaced instanceof GuiScreen screen && screen != next) {
			event.setGui(screen);
		}
	}

	@SubscribeEvent
	public void onInitPre(GuiScreenEvent.InitGuiEvent.Pre event) {
		Event init = post("ScreenEvent$Init$Pre", event.getGui(), listeners(event.getGui()), adder(event.getGui(), event.getButtonList()),
				remover(event.getGui(), event.getButtonList()));
		if (init != null && init.isCanceled()) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onInitPost(GuiScreenEvent.InitGuiEvent.Post event) {
		post("ScreenEvent$Init$Post", event.getGui(), listeners(event.getGui()), adder(event.getGui(), event.getButtonList()),
				remover(event.getGui(), event.getButtonList()));
	}

	/** The screen's widgets as 1.21 listeners: a 1.21 screen's children, or the 1.12.2 buttons that are 1.21 widgets. */
	private static List<GuiEventListener> listeners(GuiScreen screen) {
		var out = new ArrayList<GuiEventListener>();
		Object children = call(screen, "children");
		if (children instanceof List<?> list) {
			for (Object child : list) {
				if (child instanceof GuiEventListener listener) {
					out.add(listener);
				}
			}
		}
		return out;
	}

	/** A widget a mod adds at init: to a 1.21 screen as its own, to a 1.12.2 screen's buttons when it is a button. */
	private static Consumer<GuiEventListener> adder(GuiScreen screen, List<GuiButton> buttons) {
		return listener -> {
			if (invoke(screen, "addRenderableWidget", listener)) {
				return;
			}
			if (listener instanceof GuiButton button && !buttons.contains(button)) {
				buttons.add(button);
			}
		};
	}

	private static Consumer<GuiEventListener> remover(GuiScreen screen, List<GuiButton> buttons) {
		return listener -> {
			if (!invoke(screen, "removeWidget", listener) && listener instanceof GuiButton button) {
				buttons.remove(button);
			}
		};
	}

	// --- drawing --------------------------------------------------------------------------------------------------------

	@SubscribeEvent
	public void onDrawPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
		GuiGraphics gfx = new GuiGraphics(Minecraft.getMinecraft());
		Event render = post("ScreenEvent$Render$Pre", event.getGui(), gfx, event.getMouseX(), event.getMouseY(), event.getRenderPartialTicks());
		gfx.flush();
		if (render != null && render.isCanceled()) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onDrawPost(GuiScreenEvent.DrawScreenEvent.Post event) {
		GuiGraphics gfx = new GuiGraphics(Minecraft.getMinecraft());
		GlStateGuard.run(() -> post("ScreenEvent$Render$Post", event.getGui(), gfx, event.getMouseX(), event.getMouseY(), event.getRenderPartialTicks()));
		gfx.flush();
	}

	@SubscribeEvent
	public void onBackground(GuiScreenEvent.BackgroundDrawnEvent event) {
		GuiGraphics gfx = new GuiGraphics(Minecraft.getMinecraft());
		GlStateGuard.run(() -> post("ScreenEvent$BackgroundRendered", event.getGui(), gfx));
		gfx.flush();
	}

	@SubscribeEvent
	public void onForeground(GuiContainerEvent.DrawForeground event) {
		GuiGraphics gfx = new GuiGraphics(Minecraft.getMinecraft());
		GuiContainer screen = event.getGuiContainer();
		GlStateGuard.run(() -> post("ContainerScreenEvent$Render$Foreground", screen, gfx, event.getMouseX(), event.getMouseY()));
		gfx.flush();
	}

	// --- mouse ----------------------------------------------------------------------------------------------------------

	@SubscribeEvent
	public void onMousePre(GuiScreenEvent.MouseInputEvent.Pre event) {
		GuiScreen screen = event.getGui();
		double x = mouseX(screen);
		double y = mouseY(screen);
		int button = Mouse.getEventButton();
		Event posted;
		if (button >= 0) {
			if (Mouse.getEventButtonState()) {
				this.dragButton = button;
				posted = post("ScreenEvent$MouseButtonPressed$Pre", screen, x, y, button);
			} else {
				this.dragButton = -1;
				posted = post("ScreenEvent$MouseButtonReleased$Pre", screen, x, y, button);
			}
		} else if (Mouse.getEventDWheel() != 0) {
			posted = post("ScreenEvent$MouseScrolled$Pre", screen, x, y, 0.0D, Mouse.getEventDWheel() > 0 ? 1.0D : -1.0D);
		} else if (this.dragButton >= 0 && Mouse.isButtonDown(this.dragButton) && (x != this.lastX || y != this.lastY)) {
			posted = post("ScreenEvent$MouseDragged$Pre", screen, x, y, this.dragButton, x - this.lastX, y - this.lastY);
		} else {
			posted = null;
		}
		this.lastX = x;
		this.lastY = y;
		if (posted != null && posted.isCanceled()) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onMousePost(GuiScreenEvent.MouseInputEvent.Post event) {
		GuiScreen screen = event.getGui();
		int button = Mouse.getEventButton();
		if (button >= 0) {
			post(Mouse.getEventButtonState() ? "ScreenEvent$MouseButtonPressed$Post" : "ScreenEvent$MouseButtonReleased$Post", screen, mouseX(screen), mouseY(screen),
					button, false);
		} else if (Mouse.getEventDWheel() != 0) {
			post("ScreenEvent$MouseScrolled$Post", screen, mouseX(screen), mouseY(screen), 0.0D, Mouse.getEventDWheel() > 0 ? 1.0D : -1.0D);
		}
	}

	private static double mouseX(GuiScreen screen) {
		Minecraft mc = Minecraft.getMinecraft();
		return Mouse.getEventX() * (double) screen.width / mc.displayWidth;
	}

	private static double mouseY(GuiScreen screen) {
		Minecraft mc = Minecraft.getMinecraft();
		return screen.height - Mouse.getEventY() * (double) screen.height / mc.displayHeight - 1.0D;
	}

	// --- keyboard -------------------------------------------------------------------------------------------------------

	@SubscribeEvent
	public void onKeyPre(GuiScreenEvent.KeyboardInputEvent.Pre event) {
		GuiScreen screen = event.getGui();
		int key = KeyMapping.toGlfw(Keyboard.getEventKey());
		int modifiers = modifiers();
		boolean canceled = false;
		if (Keyboard.getEventKeyState()) {
			Event pressed = post("ScreenEvent$KeyPressed$Pre", screen, key, Keyboard.getEventKey(), modifiers);
			canceled = pressed != null && pressed.isCanceled();
			char typed = Keyboard.getEventCharacter();
			if (!canceled && typed >= ' ' && typed != 127) {
				Event character = post("ScreenEvent$CharacterTyped$Pre", screen, typed, modifiers);
				canceled = character != null && character.isCanceled();
			}
		} else {
			Event released = post("ScreenEvent$KeyReleased$Pre", screen, key, Keyboard.getEventKey(), modifiers);
			canceled = released != null && released.isCanceled();
		}
		if (canceled) {
			event.setCanceled(true);
		}
	}

	@SubscribeEvent
	public void onKeyPost(GuiScreenEvent.KeyboardInputEvent.Post event) {
		if (Keyboard.getEventKeyState()) {
			post("ScreenEvent$KeyPressed$Post", event.getGui(), KeyMapping.toGlfw(Keyboard.getEventKey()), Keyboard.getEventKey(), modifiers());
		}
	}

	/** GLFW's modifier bits: shift 1, control 2, alt 4. */
	private static int modifiers() {
		int bits = 0;
		if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
			bits |= 1;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)) {
			bits |= 2;
		}
		if (Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU)) {
			bits |= 4;
		}
		return bits;
	}

	// --- the carried events ---------------------------------------------------------------------------------------------

	/** The carried NeoForge event of that name made with these arguments and posted; null when it can't be made. */
	@Nullable
	private static Event post(String name, Object... args) {
		Constructor<?> constructor = constructor(name, args.length);
		if (constructor == null) {
			return null;
		}
		try {
			Event event = (Event) constructor.newInstance(args);
			MinecraftForge.EVENT_BUS.post(event);
			return event;
		} catch (java.lang.reflect.InvocationTargetException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the screen event " + name, e.getCause());
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the screen event " + name, e);
		}
		return null;
	}

	@Nullable
	private static Constructor<?> constructor(String name, int count) {
		String key = name + "/" + count;
		Constructor<?> found = CONSTRUCTORS.get(key);
		if (found != null || MISSING.contains(key)) {
			return found;
		}
		try {
			Class<?> type = Class.forName(EVENTS + name, true, net.minecraft.launchwrapper.Launch.classLoader);
			for (Constructor<?> constructor : type.getConstructors()) {
				if (constructor.getParameterCount() == count) {
					CONSTRUCTORS.put(key, constructor);
					return constructor;
				}
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			// not carried: the event isn't posted
		}
		MISSING.add(key);
		return null;
	}

	@Nullable
	private static Object call(Object target, String name) {
		for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
			for (Method method : type.getDeclaredMethods()) {
				if (method.getName().equals(name) && method.getParameterCount() == 0) {
					try {
						method.setAccessible(true);
						return method.invoke(target);
					} catch (ReflectiveOperationException | RuntimeException e) {
						return null;
					}
				}
			}
		}
		return null;
	}

	/** A one-argument method of the screen's own classes (a 1.21 screen's protected addRenderableWidget), run; false without one. */
	private static boolean invoke(Object target, String name, Object argument) {
		for (Class<?> type = target.getClass(); type != null && type != GuiScreen.class; type = type.getSuperclass()) {
			for (Method method : type.getDeclaredMethods()) {
				if (method.getName().equals(name) && method.getParameterCount() == 1 && method.getParameterTypes()[0].isInstance(argument)) {
					try {
						method.setAccessible(true);
						method.invoke(target, argument);
						return true;
					} catch (ReflectiveOperationException | RuntimeException e) {
						return false;
					}
				}
			}
		}
		return false;
	}
}
