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
package io.github.xponer2.translator.runtime.mc;

import java.util.Random;

import javax.annotation.Nullable;

import io.github.xponer2.translator.block.BlockProperties;
import io.github.xponer2.translator.block.BlockStateProperties;
import io.github.xponer2.translator.shape.CollisionContext;
import io.github.xponer2.translator.shape.Shapes;
import io.github.xponer2.translator.shape.VoxelShape;
import io.github.xponer2.translator.util.BlockPlaceContext;

import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code FlowerPotBlock}: a pot with a plant in it (a block of its own per plant, as in 1.21). Plants
 * can't be put in or taken out by hand here.
 */
public class FlowerPotBlock extends Block {
	private static final java.util.Map<net.minecraft.util.ResourceLocation, java.util.function.Supplier<? extends net.minecraft.block.Block>> PLANTS =
			new java.util.concurrent.ConcurrentHashMap<>();
	@Nullable
	private final java.util.function.Supplier<? extends net.minecraft.block.Block> content;
	@Nullable
	private final java.util.function.Supplier<FlowerPotBlock> emptyPot;

	public FlowerPotBlock(@Nullable java.util.function.Supplier<FlowerPotBlock> emptyPot, java.util.function.Supplier<? extends net.minecraft.block.Block> plant,
			BlockProperties properties) {
		super(properties.noOcclusion());
		this.content = plant;
		this.emptyPot = emptyPot;
	}

	/** NeoForge {@code isEmpty()}: no plant in the pot (Twilight Forest's special pots take a plant when empty). */
	public boolean isEmpty() {
		return this.getPotted() == net.minecraft.init.Blocks.AIR;
	}

	/** NeoForge {@code getEmptyPot()}: the pot without its plant (this one when it has none named). */
	public FlowerPotBlock getEmptyPot() {
		FlowerPotBlock empty = this.emptyPot == null ? null : this.emptyPot.get();
		return empty == null ? this : empty;
	}

	public FlowerPotBlock(net.minecraft.block.Block plant, BlockProperties properties) {
		this(null, () -> plant, properties);
	}

	public net.minecraft.block.Block getPotted() {
		var plant = this.content == null ? null : this.content.get();
		return plant == null ? net.minecraft.init.Blocks.AIR : plant;
	}

	public net.minecraft.block.Block getContent() {
		return this.getPotted();
	}

	/** The empty vanilla pot as this class (not a registered block): its addPlant is the shared table above. */
	@Nullable
	private static volatile FlowerPotBlock vanillaPot;

	/**
	 * A cast to this class (the converter calls this for {@code (FlowerPotBlock) x}): 1.12.2's own flower pot is no
	 * FlowerPotBlock, yet the mods cast {@code Blocks.FLOWER_POT} to add their potted plants (Aether's common setup
	 * stopped at it, Supplementaries' pots too): for it, a stand-in of the empty vanilla pot.
	 */
	@Nullable
	public static FlowerPotBlock translatorCast(@Nullable Object value) {
		if (value == null || value instanceof FlowerPotBlock) {
			return (FlowerPotBlock) value;
		}
		if (value instanceof net.minecraft.block.BlockFlowerPot) {
			FlowerPotBlock pot = vanillaPot;
			if (pot == null) {
				pot = new FlowerPotBlock(null, () -> net.minecraft.init.Blocks.AIR, BlockProperties.of());
				vanillaPot = pot;
			}
			return pot;
		}
		throw new ClassCastException(value.getClass().getName() + " cannot be cast to " + FlowerPotBlock.class.getName());
	}

	public void addPlant(net.minecraft.util.ResourceLocation plant, java.util.function.Supplier<? extends net.minecraft.block.Block> pot) {
		PLANTS.put(plant, pot);
	}

	@Override
	public VoxelShape getShape(IBlockState state, @Nullable IBlockAccess level, @Nullable BlockPos pos, CollisionContext context) {
		return Shapes.box16(5, 0, 5, 11, 6, 11);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public BlockRenderLayer getRenderLayer() {
		return BlockRenderLayer.CUTOUT;
	}
}
