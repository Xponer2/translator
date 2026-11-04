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
package io.github.xponer2.translator.fluids;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.RegistrationCallback;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

/**
 * Facade of the 1.21 {@code Fluid} (what a mod's fluid, NeoForge's BaseFlowingFluid, extends): a Forge 1.12.2
 * {@link Fluid}, made without a name as 1.21 makes fluids. Registered under its 1.21 id, it takes that id as its name
 * and is registered with Forge, so stacks, tanks and buckets of it work; its textures come with the mod's client
 * extensions of its fluid type ({@link #setTextures}). Create's fluids couldn't be made at all (no Fluid()).
 */
public class FluidBase extends Fluid implements RegistrationCallback {
	private static final AtomicInteger UNNAMED = new AtomicInteger();
	private static final Map<ResourceLocation, FluidBase> BY_ID = new ConcurrentHashMap<>();
	@Nullable
	private ResourceLocation id;

	protected FluidBase() {
		// water's textures until the mod's client extensions name its own (Forge stitches the fluids' textures)
		super("translator_unregistered_" + UNNAMED.incrementAndGet(), new ResourceLocation("blocks/water_still"),
				new ResourceLocation("blocks/water_flow"));
	}

	@Override
	public void onRegistered(ResourceLocation id) {
		this.id = id;
		BY_ID.put(id, this);
		String name = id.getNamespace() + "." + id.getPath();
		set(this, "fluidName", name);
		this.setUnlocalizedName(id.getNamespace() + "." + id.getPath());
		if (!FluidRegistry.isFluidRegistered(name)) {
			FluidRegistry.registerFluid(this);
		}
	}

	/** Its 1.21 id, null until it is registered. */
	@Nullable
	public ResourceLocation id() {
		return this.id;
	}

	@Nullable
	public static FluidBase byId(ResourceLocation id) {
		return BY_ID.get(id);
	}

	public static Iterable<FluidBase> all() {
		return BY_ID.values();
	}

	/** The still and flowing textures and tint of the fluid (1.21 client extensions; 1.12.2 names them in Fluid). */
	public void setTextures(@Nullable ResourceLocation still, @Nullable ResourceLocation flowing, int tint) {
		if (still != null) {
			set(this, "still", blockTexture(still));
		}
		if (flowing != null) {
			set(this, "flowing", blockTexture(flowing));
		}
		if (tint != -1 && tint != 0) {
			this.setColor(tint);
		}
	}

	/** 1.21 names block textures {@code block/x}; the 1.12.2 atlas has them as {@code blocks/x}. */
	private static ResourceLocation blockTexture(ResourceLocation texture) {
		String path = texture.getPath();
		return "minecraft".equals(texture.getNamespace()) && path.startsWith("block/")
				? new ResourceLocation("minecraft", "blocks/" + path.substring(6)) : texture;
	}

	static void set(Fluid fluid, String field, Object value) {
		try {
			Field f = Fluid.class.getDeclaredField(field);
			f.setAccessible(true);
			Field modifiers = Field.class.getDeclaredField("modifiers");
			modifiers.setAccessible(true);
			modifiers.setInt(f, f.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
			f.set(fluid, value);
		} catch (ReflectiveOperationException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the Forge fluid's " + field, e);
		}
	}
}
