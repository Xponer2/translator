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

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelFluid;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The model of a 1.21.1 mod's fluid in the world. 1.21 draws a liquid block with its fluid renderer (the block's own
 * model names only a particle texture); 1.12.2 draws a Forge fluid block with its model, so every state of the block
 * gets Forge's fluid model of its fluid (its still and flowing textures, tint, level corners and flow). The particle
 * model drew nothing: a placed fluid was invisible.
 */
@SideOnly(Side.CLIENT)
public final class FluidModels implements ICustomModelLoader {
	/** The variant every state of a liquid block maps to (in the block's own blockstate location, which exists). */
	static final String VARIANT = "translator_fluid";
	private static final FluidModels INSTANCE = new FluidModels();
	private static boolean registered;

	private FluidModels() {
	}

	/** In ModelRegistryEvent: the block's states to its fluid model. */
	static void map(Block block) {
		if (!registered) {
			registered = true;
			ModelLoaderRegistry.registerLoader(INSTANCE);
		}
		ModelResourceLocation location = new ModelResourceLocation(block.getRegistryName(), VARIANT);
		ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
			@Override
			protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
				return location;
			}
		});
	}

	@Override
	public boolean accepts(ResourceLocation location) {
		return location instanceof ModelResourceLocation variant && VARIANT.equals(variant.getVariant());
	}

	@Override
	public IModel loadModel(ResourceLocation location) {
		Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(location.getNamespace(), location.getPath()));
		Fluid fluid = block instanceof BlockFluidBase liquid ? liquid.getFluid() : null;
		return new ModelFluid(fluid != null ? fluid : FluidRegistry.WATER);
	}

	@Override
	public void onResourceManagerReload(IResourceManager manager) {
	}
}
