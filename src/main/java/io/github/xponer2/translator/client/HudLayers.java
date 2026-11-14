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
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The mods' HUD layers (NeoForge {@code RegisterGuiLayersEvent}: AppleSkin, Farmer's Delight, Create, Artifacts,
 * Supplementaries, Patchouli and ~20 more of the popular mods register theirs so). The carried event is posted once
 * with 1.21's vanilla layers in their order; each mod layer is then drawn after the 1.12.2 HUD element of the vanilla
 * layer it follows (food_level: after Forge's FOOD element), the ones 1.12.2 has no element for, or whose element
 * wasn't drawn this frame (health in creative, an element a mod cancelled), at the end of the HUD. NeoForge's
 * {@code RenderGuiLayerEvent.Pre/Post} are posted around the vanilla elements and the mod layers. Before, the event
 * was never posted: none of these layers was drawn.
 */
@SideOnly(Side.CLIENT)
public final class HudLayers {
	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Translator");
	/** 1.21.1's vanilla layers in their order, with the Forge 1.12.2 element each one is. */
	private static final String[] VANILLA = {"camera_overlays", "crosshair", "hotbar", "jump_meter", "experience_bar", "player_health",
			"armor_level", "food_level", "vehicle_health", "air_level", "selected_item_name", "spectator_tooltip", "experience_level", "effects",
			"boss_overlay", "sleep_overlay", "demo_overlay", "debug_overlay", "scoreboard_sidebar", "overlay_message", "title", "chat", "tab_list",
			"subtitle_overlay", "saving_indicator"};
	private static final Map<String, ElementType> ELEMENTS = new LinkedHashMap<>();

	static {
		ELEMENTS.put("camera_overlays", ElementType.PORTAL);
		ELEMENTS.put("crosshair", ElementType.CROSSHAIRS);
		ELEMENTS.put("hotbar", ElementType.HOTBAR);
		ELEMENTS.put("jump_meter", ElementType.JUMPBAR);
		ELEMENTS.put("experience_bar", ElementType.EXPERIENCE);
		ELEMENTS.put("player_health", ElementType.HEALTH);
		ELEMENTS.put("armor_level", ElementType.ARMOR);
		ELEMENTS.put("food_level", ElementType.FOOD);
		ELEMENTS.put("vehicle_health", ElementType.HEALTHMOUNT);
		ELEMENTS.put("air_level", ElementType.AIR);
		ELEMENTS.put("experience_level", ElementType.EXPERIENCE);
		ELEMENTS.put("effects", ElementType.POTION_ICONS);
		ELEMENTS.put("boss_overlay", ElementType.BOSSHEALTH);
		ELEMENTS.put("debug_overlay", ElementType.DEBUG);
		ELEMENTS.put("chat", ElementType.CHAT);
		ELEMENTS.put("tab_list", ElementType.PLAYER_LIST);
		ELEMENTS.put("subtitle_overlay", ElementType.SUBTITLES);
	}

	/** A mod layer and the vanilla layer it is drawn after ("" before them all). */
	private static final class Layer {
		final ResourceLocation id;
		final Object layer;
		final String after;
		boolean failed;

		Layer(ResourceLocation id, Object layer, String after) {
			this.id = id;
			this.layer = layer;
			this.after = after;
		}
	}

	/** The mod layers by the vanilla layer they follow, in the final order. */
	private final Map<String, List<Layer>> groups = new LinkedHashMap<>();
	/** The vanilla layers each 1.12.2 element stands for (experience is two). */
	private final Map<ElementType, List<String>> byElement = new EnumMap<>(ElementType.class);
	/** The groups drawn this frame. */
	private final java.util.Set<String> drawn = new java.util.HashSet<>();
	private final EnumSet<ElementType> elementsDrawn = EnumSet.noneOf(ElementType.class);
	private final Object marker;
	private final Method render;
	@Nullable
	private final Constructor<?> layerPre;
	@Nullable
	private final Constructor<?> layerPost;

	private HudLayers(Object marker, Method render, @Nullable Constructor<?> layerPre, @Nullable Constructor<?> layerPost) {
		this.marker = marker;
		this.render = render;
		this.layerPre = layerPre;
		this.layerPost = layerPost;
		ELEMENTS.forEach((id, element) -> this.byElement.computeIfAbsent(element, ignored -> new ArrayList<>()).add(id));
	}

	/** At client setup, once: the mods register their layers, and they are drawn from then on. */
	public static void register() {
		ClassLoader loader = net.minecraft.launchwrapper.Launch.classLoader;
		try {
			Class<?> layerType = Class.forName("net.minecraft.client.gui.LayeredDraw$Layer", true, loader);
			Class<?> namedType = Class.forName("net.neoforged.neoforge.client.gui.GuiLayerManager$NamedLayer", true, loader);
			Class<?> eventType = Class.forName("net.neoforged.neoforge.client.event.RegisterGuiLayersEvent", true, loader);
			Method render = layerType.getMethod("render", GuiGraphics.class, DeltaTracker.class);
			Object marker = Proxy.newProxyInstance(layerType.getClassLoader(), new Class<?>[] {layerType}, (proxy, method, args) -> {
				switch (method.getName()) {
					case "hashCode":
						return System.identityHashCode(proxy);
					case "equals":
						return args != null && args.length == 1 && args[0] == proxy;
					case "toString":
						return "vanilla HUD layer";
					default:
						return null;
				}
			});
			Constructor<?> named = namedType.getConstructor(ResourceLocation.class, layerType);
			List<Object> layers = new ArrayList<>();
			for (String id : VANILLA) {
				layers.add(named.newInstance(new ResourceLocation("minecraft", id), marker));
			}
			MinecraftForge.EVENT_BUS.post((Event) eventType.getConstructor(List.class).newInstance(layers));

			HudLayers hud = new HudLayers(marker, render, event("Pre", layerType, loader), event("Post", layerType, loader));
			Method name = namedType.getMethod("name");
			Method layer = namedType.getMethod("layer");
			String after = "";
			int count = 0;
			for (Object entry : layers) {
				ResourceLocation id = (ResourceLocation) name.invoke(entry);
				Object drawn = layer.invoke(entry);
				boolean vanilla = "minecraft".equals(id.getNamespace()) && java.util.Arrays.asList(VANILLA).contains(id.getPath());
				if (vanilla) {
					after = id.getPath();
					if (drawn == marker) {
						continue;
					}
					// a vanilla layer a mod replaced or wrapped: 1.12.2 still draws its own, the mod's goes after it
				}
				hud.groups.computeIfAbsent(after, ignored -> new ArrayList<>()).add(new Layer(id, drawn, after));
				count++;
			}
			MinecraftForge.EVENT_BUS.register(hud);
			if (count > 0) {
				LOGGER.info("HUD: {} layers of the 1.21.1 mods", count);
			}
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			Throwable cause = e instanceof java.lang.reflect.InvocationTargetException invocation && invocation.getCause() != null ? invocation.getCause() : e;
			LOGGER.warn("The 1.21.1 mods' HUD layers weren't registered: {}", cause.toString());
		}
	}

	@Nullable
	private static Constructor<?> event(String phase, Class<?> layerType, ClassLoader loader) {
		try {
			return Class.forName("net.neoforged.neoforge.client.event.RenderGuiLayerEvent$" + phase, true, loader).getConstructor(GuiGraphics.class,
					DeltaTracker.class, ResourceLocation.class, layerType);
		} catch (ReflectiveOperationException | LinkageError e) {
			return null;
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void before(RenderGameOverlayEvent.Pre event) {
		if (event.getType() == ElementType.ALL) {
			this.drawn.clear();
			this.elementsDrawn.clear();
			this.drawGroup("");
			return;
		}
		for (String id : this.byElement.getOrDefault(event.getType(), java.util.Collections.emptyList())) {
			this.post(this.layerPre, new ResourceLocation("minecraft", id), this.marker);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOWEST)
	public void after(RenderGameOverlayEvent.Post event) {
		if (event.getType() == ElementType.ALL) {
			// the groups no drawn element took (1.12.2 has no element of their layer, or it wasn't drawn)
			for (String group : this.groups.keySet()) {
				this.drawGroup(group);
			}
			return;
		}
		if (!this.elementsDrawn.add(event.getType())) {
			return;
		}
		for (String id : this.byElement.getOrDefault(event.getType(), java.util.Collections.emptyList())) {
			this.post(this.layerPost, new ResourceLocation("minecraft", id), this.marker);
			this.drawGroup(id);
		}
	}

	private void drawGroup(String group) {
		List<Layer> layers = this.groups.get(group);
		if (layers == null || !this.drawn.add(group)) {
			return;
		}
		for (Layer layer : layers) {
			this.draw(layer);
		}
	}

	private void draw(Layer layer) {
		if (layer.failed) {
			return;
		}
		Event pre = this.post(this.layerPre, layer.id, layer.layer);
		if (pre != null && pre.isCanceled()) {
			return;
		}
		GuiGraphics gfx = new GuiGraphics(Minecraft.getMinecraft());
		GlStateGuard.run(() -> {
			try {
				this.render.invoke(layer.layer, gfx, DeltaTracker.INSTANCE);
				gfx.flush();
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				Throwable cause = e instanceof java.lang.reflect.InvocationTargetException invocation && invocation.getCause() != null ? invocation.getCause() : e;
				layer.failed = true;
				io.github.xponer2.translator.runtime.RuntimeStubs.failed("the HUD layer " + layer.id, cause);
			}
		});
		this.post(this.layerPost, layer.id, layer.layer);
	}

	@Nullable
	private Event post(@Nullable Constructor<?> type, ResourceLocation id, Object layer) {
		if (type == null) {
			return null;
		}
		try {
			Event event = (Event) type.newInstance(new GuiGraphics(Minecraft.getMinecraft()), DeltaTracker.INSTANCE, id, layer);
			MinecraftForge.EVENT_BUS.post(event);
			return event;
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}
}
