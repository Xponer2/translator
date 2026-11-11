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

import javax.annotation.Nullable;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 texture and model data calls 1.12.2's classes lack. {@code texture.bind()}: a mod binds its own texture before it
 * draws with it (JourneyMap's map tiles: the call was left out and every tile drew with whatever was bound last).
 */
@SideOnly(Side.CLIENT)
public final class TextureBridges {
	private TextureBridges() {
	}

	/** 1.21 {@code AbstractTexture.bind()}. */
	public static void bind(AbstractTexture texture) {
		GlStateManager.bindTexture(texture.getGlTextureId());
	}

	/** 1.21 {@code AbstractTexture.getId()}. */
	public static int getId(AbstractTexture texture) {
		return texture.getGlTextureId();
	}

	/** 1.21 {@code AbstractTexture.releaseId()}. */
	public static void releaseId(AbstractTexture texture) {
		texture.deleteGlTexture();
	}

	/**
	 * NeoForge {@code level.getModelDataManager().getAt(pos)}: what the block entity there tells its block's model, empty
	 * when there is none. 1.12.2 has no manager (the receiver is whatever the call before gave, also null).
	 */
	public static ModelData getAt(@Nullable Object manager, @Nullable BlockPos pos) {
		var world = net.minecraft.client.Minecraft.getMinecraft().world;
		if (world == null || pos == null || !world.isBlockLoaded(pos)) {
			return ModelData.EMPTY;
		}
		try {
			TileEntity tile = world.getTileEntity(pos);
			if (tile instanceof io.github.xponer2.translator.block.BlockEntity entity) {
				ModelData data = entity.getModelData();
				return data == null ? ModelData.EMPTY : data;
			}
		} catch (RuntimeException e) {
			// asked off the client thread while the chunk changes: nothing known
		}
		return ModelData.EMPTY;
	}
}
