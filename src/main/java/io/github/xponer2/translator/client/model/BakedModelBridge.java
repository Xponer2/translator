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
package io.github.xponer2.translator.client.model;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import io.github.xponer2.translator.client.ModelData;
import io.github.xponer2.translator.client.RenderType;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Added by the converter to every mod class that is a 1.21 BakedModel (an IBakedModel after the tables): 1.12.2 asks
 * for the quads with a long seed, the mod's model gives them with a random (BakedModel) or with a random, its model
 * data and a render type (NeoForge's IDynamicBakedModel). The other methods have their 1.12.2 names already.
 * Without it, 1.12.2's call was an AbstractMethodError and every such model drew nothing (Sophisticated Backpacks,
 * AE2's cables, Mekanism's and Create's dynamic models).
 */
@SideOnly(Side.CLIENT)
public interface BakedModelBridge extends IBakedModel {
	/** 1.21 {@code getQuads(state, side, random)}: the mod's (or IDynamicBakedModel's, which asks the next one). */
	List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, Random random);

	/** NeoForge {@code getQuads(state, side, random, data, renderType)}: the plain ones unless the mod's model has these. */
	default List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, Random random, ModelData data,
			@Nullable RenderType renderType) {
		return this.getQuads(state, side, random);
	}

	/** NeoForge {@code getModelData(level, pos, state, data)}: the block entity's data as it is (a mod's model adds to it). */
	default ModelData getModelData(net.minecraft.world.IBlockAccess level, net.minecraft.util.math.BlockPos pos, IBlockState state, ModelData data) {
		return data;
	}

	/**
	 * 1.12.2's quads: the mod's, without a missing one. A quad the mod couldn't make (a part of 1.21 the Translator
	 * lacked) is a null in its list, which crashed the chunk being built ("Tesselating block model": Twilight Forest's
	 * coronation carpet); a model that throws draws nothing. Either way the game goes on.
	 */
	@Override
	default List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
		try {
			return withoutNulls(this.getQuads(state, side, new Random(rand), modelData(state), null), this.getClass());
		} catch (RuntimeException | LinkageError e) {
			// a part of 1.21 the model needs and the Translator lacks: nothing drawn, the game goes on
			if (GeometryLoaders.reportOnce(this.getClass())) {
				io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + ".getQuads", e);
			}
			return java.util.Collections.emptyList();
		}
	}

	/** The list without null quads (and an empty one for none); the model's class reported once. */
	static List<BakedQuad> withoutNulls(@Nullable List<BakedQuad> quads, Class<?> model) {
		if (quads == null) {
			return java.util.Collections.emptyList();
		}
		if (!quads.contains(null)) {
			return quads;
		}
		if (GeometryLoaders.reportOnce(model)) {
			io.github.xponer2.translator.runtime.RuntimeStubs.missing(model.getName() + ".getQuads (a null quad, left out)");
		}
		var kept = new java.util.ArrayList<BakedQuad>(quads.size());
		for (BakedQuad quad : quads) {
			if (quad != null) {
				kept.add(quad);
			}
		}
		return kept;
	}

	/** The model data a block put on its extended state (the Translator's blocks: their block entity's data). */
	static ModelData modelData(@Nullable IBlockState state) {
		if (state instanceof IExtendedBlockState extended && extended.getUnlistedNames().contains(ModelDataProperty.INSTANCE)) {
			ModelData data = extended.getValue(ModelDataProperty.INSTANCE);
			if (data != null) {
				return data;
			}
		}
		return ModelData.EMPTY;
	}
}
