/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.selftest;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import io.github.xponer2.translator.client.HudLayers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * What the mods that change the game's interface actually do, checked on the client (a mod that loads without an error
 * can still do nothing: JEI, Mouse Tweaks and Controlling were "working" in the report and dead in the game):
 * <ul>
 * <li>a mod's HUD layers (AppleSkin's hunger and saturation) are drawn, frame after frame, without failing;</li>
 * <li>JEI starts, shows its item list beside the inventory and opens its recipe screen with crafting recipes;</li>
 * <li>Controlling's screen replaces 1.12.2's controls screen;</li>
 * <li>Mouse Tweaks handles 1.12.2's own chest screen.</li>
 * </ul>
 */
final class InterfaceChecks {
	/** While a check runs: the automatic test leaves the screens it opens (the controls screen pauses the game). */
	static volatile boolean running;

	/** The mods with a check of their own (any mod with HUD layers gets the layer check). */
	static final String[] MODS = {"appleskin", "jei", "controlling", "mousetweaks"};

	private InterfaceChecks() {
	}

	static boolean wanted(String modId) {
		return java.util.Arrays.asList(MODS).contains(modId) || !HudLayers.layersOf(modId).isEmpty();
	}

	static AtomicBoolean check(SelfTest.Run run, String modId) {
		AtomicBoolean done = new AtomicBoolean();
		Minecraft.getMinecraft().addScheduledTask(() -> {
			running = true;
			MinecraftForge.EVENT_BUS.register(new Checker(run, modId, done));
		});
		return done;
	}

	/** One step after another on the client's ticks: an action, then the ticks to wait before the next. */
	private static final class Checker {
		private final SelfTest.Run run;
		private final String modId;
		private final String id;
		private final AtomicBoolean done;
		private final List<Runnable> steps = new ArrayList<>();
		private final List<Integer> waits = new ArrayList<>();
		private int index;
		private int wait;

		Checker(SelfTest.Run run, String modId, AtomicBoolean done) {
			this.run = run;
			this.modId = modId;
			this.id = modId + ":interface";
			this.done = done;
			this.plan();
		}

		private void step(int ticksAfter, Runnable action) {
			this.steps.add(action);
			this.waits.add(ticksAfter);
		}

		private void problem(String text) {
			this.run.addClientProblem("mod", this.id, text);
		}

		private void note(String text) {
			this.run.addClientNote("mod", this.id, text);
		}

		private void plan() {
			Minecraft mc = Minecraft.getMinecraft();
			List<String> layers = HudLayers.layersOf(this.modId);
			if (!layers.isEmpty()) {
				int[] before = new int[layers.size()];
				this.step(60, () -> {
					mc.displayGuiScreen(null);
					if ("appleskin".equals(this.modId)) {
						// hungry, bread in hand: what AppleSkin shows (the bread's food and saturation on the bar)
						var server = mc.getIntegratedServer();
						if (server != null) {
							server.addScheduledTask(() -> {
								this.run.player.getFoodStats().setFoodLevel(10);
								this.run.player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, new net.minecraft.item.ItemStack(net.minecraft.init.Items.BREAD));
							});
						}
					}
					for (int i = 0; i < layers.size(); i++) {
						before[i] = HudLayers.draws(layers.get(i));
					}
				});
				this.step(0, () -> {
					int drawn = 0;
					for (int i = 0; i < layers.size(); i++) {
						String layer = layers.get(i);
						if (HudLayers.failed(layer)) {
							this.problem("its HUD layer " + layer + " failed (no longer drawn)");
						} else if (HudLayers.draws(layer) <= before[i]) {
							this.problem("its HUD layer " + layer + " isn't drawn (3 s with the HUD shown)");
						} else {
							drawn++;
						}
					}
					this.note(drawn + " of " + layers.size() + " HUD layers drawn");
					if ("appleskin".equals(this.modId)) {
						var server = mc.getIntegratedServer();
						if (server != null) {
							server.addScheduledTask(() -> {
								this.run.player.getFoodStats().setFoodLevel(20);
								this.run.player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, net.minecraft.item.ItemStack.EMPTY);
							});
						}
					}
				});
			} else if ("appleskin".equals(this.modId)) {
				this.step(0, () -> this.problem("no HUD layers registered (its hunger and saturation overlays never draw)"));
			}
			switch (this.modId) {
				case "jei" -> this.planJei(mc);
				case "controlling" -> this.planControlling(mc);
				case "mousetweaks" -> this.planMouseTweaks(mc);
				default -> {
				}
			}
			this.step(0, () -> mc.displayGuiScreen(null));
		}

		private void planJei(Minecraft mc) {
			Object[] runtime = new Object[1];
			this.step(60, () -> mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiInventory(mc.player)));
			this.step(0, () -> {
				try {
					runtime[0] = call(Class.forName("mezz.jei.common.Internal", true, net.minecraft.launchwrapper.Launch.classLoader), null, "getJeiRuntime");
				} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
					Throwable cause = e instanceof java.lang.reflect.InvocationTargetException i && i.getCause() != null ? i.getCause() : e;
					this.problem("JEI didn't start: no runtime (" + SelfTest.describe(cause) + ")");
					return;
				}
				try {
					Object overlay = call(runtime[0].getClass(), runtime[0], "getIngredientListOverlay");
					if (!Boolean.TRUE.equals(call(overlay.getClass(), overlay, "isListDisplayed"))) {
						this.problem("its item list doesn't show beside the inventory");
					}
					Object filter = call(runtime[0].getClass(), runtime[0], "getIngredientFilter");
					Object stacks = call(filter.getClass(), filter, "getFilteredItemStacks");
					int count = stacks instanceof List<?> list ? list.size() : -1;
					if (count == 0) {
						this.problem("its item list is empty");
					} else {
						this.note("item list: " + count + " items");
					}
					Class<?> types = Class.forName("mezz.jei.api.constants.RecipeTypes", true, net.minecraft.launchwrapper.Launch.classLoader);
					Object crafting = types.getField("CRAFTING").get(null);
					Object manager = call(runtime[0].getClass(), runtime[0], "getRecipeManager");
					Object lookup = call(manager.getClass(), manager, "createRecipeLookup", crafting);
					Object stream = call(lookup.getClass(), lookup, "get");
					long recipes = stream instanceof java.util.stream.Stream<?> s ? s.count() : -1;
					if (recipes == 0) {
						this.problem("no crafting recipes in JEI");
					} else {
						this.note(recipes + " crafting recipes in JEI");
					}
					Object gui = call(runtime[0].getClass(), runtime[0], "getRecipesGui");
					call(gui.getClass(), gui, "showTypes", java.util.Collections.singletonList(crafting));
				} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
					this.problem("JEI: " + SelfTest.describe(e instanceof java.lang.reflect.InvocationTargetException i && i.getCause() != null ? i.getCause() : e));
				}
			});
			this.waits.set(this.waits.size() - 1, 30);
			this.step(0, () -> {
				if (runtime[0] == null) {
					return;
				}
				GuiScreen screen = mc.currentScreen;
				if (screen == null || !screen.getClass().getName().toLowerCase(java.util.Locale.ROOT).contains("recipe")) {
					this.problem("its recipe screen doesn't open (" + (screen == null ? "no screen" : screen.getClass().getSimpleName()) + ")");
				} else {
					this.note("recipe screen " + screen.getClass().getSimpleName());
				}
			});
		}

		private void planControlling(Minecraft mc) {
			this.step(30, () -> mc.displayGuiScreen(new net.minecraft.client.gui.GuiControls(new net.minecraft.client.gui.GuiOptions(null, mc.gameSettings),
					mc.gameSettings)));
			this.step(0, () -> {
				GuiScreen screen = mc.currentScreen;
				String name = screen == null ? "no screen" : screen.getClass().getName();
				if (!name.startsWith("com.blamejared.controlling")) {
					this.problem("the controls screen isn't Controlling's (" + name + ")");
				} else {
					int widgets = buttons(screen) + children(screen);
					if (widgets == 0) {
						this.problem("Controlling's screen is empty (no buttons, no key list)");
					} else {
						this.note("Controlling's screen " + screen.getClass().getSimpleName() + ", " + widgets + " widgets");
					}
				}
			});
		}

		private void planMouseTweaks(Minecraft mc) {
			this.step(20, () -> mc.displayGuiScreen(new net.minecraft.client.gui.inventory.GuiChest(mc.player.inventory,
					new net.minecraft.inventory.InventoryBasic("Mouse Tweaks check", false, 27))));
			this.step(0, () -> {
				try {
					Class<?> main = Class.forName("yalter.mousetweaks.Main", true, net.minecraft.launchwrapper.Launch.classLoader);
					Method find = null;
					for (Method method : main.getDeclaredMethods()) {
						if (method.getName().equals("findHandler") && method.getParameterCount() == 1) {
							find = method;
						}
					}
					if (find == null) {
						this.problem("Mouse Tweaks has no findHandler (changed?)");
						return;
					}
					find.setAccessible(true);
					Object handler = find.invoke(null, mc.currentScreen);
					if (handler == null) {
						this.problem("Mouse Tweaks doesn't handle 1.12.2's chest screen (its tweaks do nothing on vanilla containers)");
					} else {
						this.note("handles the chest screen with " + handler.getClass().getSimpleName());
					}
				} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
					this.problem("Mouse Tweaks: " + SelfTest.describe(e instanceof java.lang.reflect.InvocationTargetException i && i.getCause() != null ? i.getCause() : e));
				}
			});
		}

		@SubscribeEvent
		public void onTick(TickEvent.ClientTickEvent event) {
			if (event.phase != TickEvent.Phase.END) {
				return;
			}
			if (this.wait > 0) {
				this.wait--;
				return;
			}
			if (this.index >= this.steps.size() || Minecraft.getMinecraft().player == null) {
				MinecraftForge.EVENT_BUS.unregister(this);
				running = false;
				this.done.set(true);
				return;
			}
			try {
				this.steps.get(this.index).run();
			} catch (Throwable t) {
				this.problem("check step " + (this.index + 1) + ": " + SelfTest.describe(t));
			}
			this.wait = this.waits.get(this.index);
			this.index++;
		}
	}

	@Nullable
	private static Object call(Class<?> type, @Nullable Object target, String name, Object... args) throws ReflectiveOperationException {
		for (Class<?> current = type; current != null; current = current.getSuperclass()) {
			for (Method method : current.getMethods()) {
				if (method.getName().equals(name) && method.getParameterCount() == args.length) {
					method.setAccessible(true);
					return method.invoke(target, args);
				}
			}
		}
		throw new NoSuchMethodException(type.getName() + "." + name);
	}

	/** A 1.12.2 screen's buttons (its protected list). */
	private static int buttons(GuiScreen screen) {
		java.lang.reflect.Field field = net.minecraftforge.fml.common.ObfuscationReflectionHelper.findField(GuiScreen.class, "field_146292_n");
		try {
			Object list = field.get(screen);
			return list instanceof java.util.Collection<?> c ? c.size() : 0;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return 0;
		}
	}

	/** A 1.21 screen's children (the facade's list), 0 for a plain 1.12.2 screen. */
	private static int children(GuiScreen screen) {
		try {
			Object list = call(screen.getClass(), screen, "children");
			return list instanceof java.util.Collection<?> c ? c.size() : 0;
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return 0;
		}
	}
}
