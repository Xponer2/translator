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
package io.github.xponer2.translator.util;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

/**
 * 1.21 {@code FluidState}: the fluid of a block state. 1.12.2 has fluids only as blocks (water, lava, Forge fluid
 * blocks), so this is the Forge fluid of the block and whether it is a source.
 */
public final class FluidState {
	/** What 1.21 calls Fluids.EMPTY: a Forge fluid that is never registered, so it equals no real fluid. */
	public static final Fluid EMPTY_FLUID = new Fluid("translator_empty", null, null);
	public static final FluidState EMPTY = new FluidState(EMPTY_FLUID, false, 0);

	/**
	 * 1.21 {@code FluidState.CODEC} ({@code {"Name": "minecraft:lava", "Properties": {...}}}): the fluid by its name, a
	 * source. The carried one failed (its StateHolder.codec isn't here), and with it every feature class (springs read
	 * it): no mod's feature could be made.
	 */
	public static final com.mojang.serialization.Codec<FluidState> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(
			instance -> instance.group(
					com.mojang.serialization.Codec.STRING.fieldOf("Name").forGetter(FluidState::name),
					com.mojang.serialization.Codec.unboundedMap(com.mojang.serialization.Codec.STRING, com.mojang.serialization.Codec.STRING)
							.optionalFieldOf("Properties", java.util.Collections.emptyMap()).forGetter(state -> java.util.Collections.emptyMap()))
					.apply(instance, (name, properties) -> byName(name)));

	/** The fluid of a 1.21 fluid name (water, lava, empty, a mod's fluid by its Forge name). */
	public static FluidState byName(String name) {
		String path = name.substring(name.indexOf(':') + 1);
		if ("empty".equals(path)) {
			return EMPTY;
		}
		Fluid fluid = "water".equals(path) || "flowing_water".equals(path) ? FluidRegistry.WATER
				: "lava".equals(path) || "flowing_lava".equals(path) ? FluidRegistry.LAVA
				: FluidRegistry.getFluid(path) != null ? FluidRegistry.getFluid(path) : FluidRegistry.getFluid(name);
		return fluid == null ? EMPTY : new FluidState(fluid, !path.startsWith("flowing_"), 8);
	}

	private String name() {
		if (this.fluid == EMPTY_FLUID) {
			return "minecraft:empty";
		}
		if (this.fluid == FluidRegistry.WATER || this.fluid == FluidRegistry.LAVA) {
			return "minecraft:" + this.fluid.getName();
		}
		return this.fluid.getName();
	}

	private final Fluid fluid;
	private final boolean source;
	private final int amount;

	private FluidState(Fluid fluid, boolean source, int amount) {
		this.fluid = fluid;
		this.source = source;
		this.amount = amount;
	}

	public static FluidState of(IBlockState state) {
		var block = state.getBlock();
		if (block instanceof BlockLiquid) {
			// still and flowing water / lava are one fluid each (1.21 Fluid.isSame)
			Fluid fluid = state.getMaterial() == Material.LAVA ? FluidRegistry.LAVA : FluidRegistry.WATER;
			int level = state.getValue(BlockLiquid.LEVEL);
			return new FluidState(fluid, level == 0, level >= 8 ? 8 : 8 - level);
		}
		Fluid fluid = FluidRegistry.lookupFluidForBlock(block);
		if (fluid == null) {
			return EMPTY;
		}
		boolean source = !(block instanceof BlockFluidBase) || state.getValue(BlockFluidBase.LEVEL) == 0;
		return new FluidState(fluid, source, 8);
	}

	/** The state of a fluid itself (1.21 fluid.defaultFluidState(), getSource(falling), getFlowing(amount, falling)). */
	public static FluidState of(Fluid fluid, boolean source, int amount) {
		return fluid == null || fluid == EMPTY_FLUID ? EMPTY : new FluidState(fluid, source, amount);
	}

	public boolean is(Fluid fluid) {
		return this.fluid == fluid;
	}

	/** In a fluid tag: minecraft:water and minecraft:lava, the mods' fluid tags (data/<ns>/tags/fluid) by fluid name. */
	public boolean is(io.github.xponer2.translator.util.TagKey<?> tag) {
		if (this.isEmpty()) {
			return false;
		}
		if (io.github.xponer2.translator.runtime.redirect.ItemBridges.is(this.fluid, tag)) {
			return true;
		}
		for (var id : io.github.xponer2.translator.tags.TagManager.ids("fluid", tag.location())) {
			String path = id.getPath().startsWith("flowing_") ? id.getPath().substring(8) : id.getPath();
			if (path.equals(this.fluid.getName()) || ("minecraft".equals(id.getNamespace()) && FluidRegistry.getFluid(path) == this.fluid)) {
				return true;
			}
		}
		return false;
	}

	public boolean isSourceOfType(Fluid fluid) {
		return this.source && this.fluid == fluid;
	}

	/** The block of this fluid (1.12.2 still or flowing water and lava with their level, a Forge fluid's block). */
	@SuppressWarnings("deprecation")
	public IBlockState createLegacyBlock() {
		if (this.isEmpty()) {
			return net.minecraft.init.Blocks.AIR.getDefaultState();
		}
		int level = this.source ? 0 : Math.max(1, Math.min(7, 8 - this.amount));
		if (this.fluid == FluidRegistry.WATER) {
			return (this.source ? net.minecraft.init.Blocks.WATER : net.minecraft.init.Blocks.FLOWING_WATER).getDefaultState().withProperty(BlockLiquid.LEVEL,
					level);
		}
		if (this.fluid == FluidRegistry.LAVA) {
			return (this.source ? net.minecraft.init.Blocks.LAVA : net.minecraft.init.Blocks.FLOWING_LAVA).getDefaultState().withProperty(BlockLiquid.LEVEL,
					level);
		}
		var block = this.fluid.getBlock();
		return block == null ? net.minecraft.init.Blocks.AIR.getDefaultState() : block.getDefaultState();
	}

	public float getOwnHeight() {
		return this.amount / 9.0F;
	}

	public float getHeight(net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
		return this.getOwnHeight();
	}

	public net.minecraft.util.math.Vec3d getFlow(net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
		return net.minecraft.util.math.Vec3d.ZERO;
	}

	public float getExplosionResistance() {
		return this.isEmpty() ? 0.0F : 100.0F;
	}

	public java.util.Collection<net.minecraft.block.properties.IProperty<?>> getProperties() {
		return java.util.Collections.emptyList();
	}

	public void animateTick(net.minecraft.world.World world, net.minecraft.util.math.BlockPos pos, java.util.Random random) {
	}

	public boolean isRandomlyTicking() {
		return false;
	}

	public boolean isEmpty() {
		return this.fluid == EMPTY_FLUID;
	}

	public Fluid getType() {
		return this.fluid;
	}

	public boolean isSource() {
		return this.source;
	}

	public int getAmount() {
		return this.amount;
	}

	/**
	 * 1.21 {@code state.getValue(property)} of a fluid state: its level (the amount) and falling (never here); a
	 * property the Translator's fluid states don't keep gives its first value. A mod's flowing fluid reads its level so
	 * (NeoForge BaseFlowingFluid.Flowing.getAmount).
	 */
	@SuppressWarnings("unchecked")
	public <T extends Comparable<T>> T getValue(net.minecraft.block.properties.IProperty<T> property) {
		if ("level".equals(property.getName()) && property.getValueClass() == Integer.class) {
			int level = this.amount;
			for (T value : property.getAllowedValues()) {
				if (value instanceof Integer allowed && allowed == level) {
					return value;
				}
			}
			return (T) Integer.valueOf(Math.max(1, Math.min(8, level)));
		}
		if ("falling".equals(property.getName()) && property.getValueClass() == Boolean.class) {
			return (T) Boolean.FALSE;
		}
		return property.getAllowedValues().iterator().next();
	}

	/**
	 * 1.21 {@code state.setValue(property, value)}: a level sets the amount; other properties aren't kept. It returns
	 * Object as 1.21's StateHolder.setValue does once erased (S), so the mods' calls find it.
	 */
	public <T extends Comparable<T>, V extends T> Object setValue(net.minecraft.block.properties.IProperty<T> property, V value) {
		if ("level".equals(property.getName()) && value instanceof Integer level) {
			return of(this.fluid, this.source, level);
		}
		return this;
	}

	/** 1.21 {@code state.hasProperty(property)}: level and falling. */
	public boolean hasProperty(net.minecraft.block.properties.IProperty<?> property) {
		return "level".equals(property.getName()) || "falling".equals(property.getName());
	}
}
