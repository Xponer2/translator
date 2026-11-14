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

import java.lang.reflect.Method;

import javax.annotation.Nullable;

import net.minecraft.block.BlockChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code ChestRenderer} (S row) for the mods' chest renderers (Twilight Forest's: a material per
 * wood). It draws 1.21's chest: a single chest, or the left or right half of a double one, each with its own 64x64
 * texture (the material getMaterial gives), the lid opening as the chest's. 1.12.2's chest renderer draws a double chest
 * as one model with one texture, and 1.12.2 had no such renderer for a mod: Twilight Forest's chests were vanilla
 * chests. TesrAdapter routes the chests of a mod's block to it, vanilla's keep 1.12.2's.
 */
@SideOnly(Side.CLIENT)
public class ChestRenderer<T extends TileEntity> implements BlockEntityRenderer<T> {
	/** 1.21 {@code xmasTextures} (F row): the Christmas textures (1.12.2's flag). */
	protected boolean isChristmas;

	private static final Part SINGLE = new Part(1.0F, 14.0F, 7.0F, 2.0F);
	private static final Part LEFT = new Part(0.0F, 15.0F, 0.0F, 1.0F);
	private static final Part RIGHT = new Part(1.0F, 15.0F, 15.0F, 1.0F);

	public ChestRenderer(BlockEntityRendererProvider.Context context) {
	}

	/** 1.21 {@code getMaterial(entity, type)}: the texture of the chest; a vanilla chest's by default. */
	protected net.minecraft.client.resources.model.Material getMaterial(T entity, net.minecraft.world.level.block.state.properties.ChestType type) {
		String name = String.valueOf(type).toLowerCase(java.util.Locale.ROOT);
		String path = "entity/chest/normal" + ("single".equals(name) ? "" : "_" + name);
		return (net.minecraft.client.resources.model.Material) io.github.xponer2.translator.runtime.redirect.ModelBakeBridges.newMaterial(
				new ResourceLocation("textures/atlas/chest.png"), new ResourceLocation(path));
	}

	@Override
	public void render(T entity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
		World world = entity.getWorld();
		BlockPos pos = entity.getPos();
		IBlockState state = world != null ? world.getBlockState(pos) : null;
		EnumFacing facing = state != null && state.getPropertyKeys().contains(BlockChest.FACING) ? state.getValue(BlockChest.FACING) : EnumFacing.SOUTH;
		String type = "SINGLE";
		if (state != null && world != null) {
			if (world.getBlockState(pos.offset(facing.rotateY())).getBlock() == state.getBlock()) {
				type = "LEFT";
			} else if (world.getBlockState(pos.offset(facing.rotateYCCW())).getBlock() == state.getBlock()) {
				type = "RIGHT";
			}
		}
		ResourceLocation texture = texture(this.getMaterial(entity, (net.minecraft.world.level.block.state.properties.ChestType) chestType(type)));
		if (texture == null) {
			return;
		}
		float open = 0.0F;
		if (entity instanceof TileEntityChest chest) {
			open = chest.prevLidAngle + (chest.lidAngle - chest.prevLidAngle) * partialTick;
		}
		open = 1.0F - open;
		open = 1.0F - open * open * open;
		Part part = "LEFT".equals(type) ? LEFT : "RIGHT".equals(type) ? RIGHT : SINGLE;
		Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
		GlStateManager.pushMatrix();
		GlStateManager.enableRescaleNormal();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.translate(0.5F, 0.5F, 0.5F);
		GlStateManager.rotate(-facing.getHorizontalAngle(), 0.0F, 1.0F, 0.0F);
		GlStateManager.translate(-0.5F, -0.5F, -0.5F);
		part.lid.rotateAngleX = -(open * ((float) Math.PI / 2.0F));
		part.lock.rotateAngleX = part.lid.rotateAngleX;
		part.lid.render(0.0625F);
		part.lock.render(0.0625F);
		part.bottom.render(0.0625F);
		GlStateManager.disableRescaleNormal();
		GlStateManager.popMatrix();
	}

	@Nullable
	private static Object chestType(String name) {
		try {
			@SuppressWarnings({"unchecked", "rawtypes"})
			Object value = Enum.valueOf((Class) Class.forName("net.minecraft.world.level.block.state.properties.ChestType", true,
					ChestRenderer.class.getClassLoader()), name);
			return value;
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}

	/** The texture file of a 1.21 material: ns:entity/chest/x is ns:textures/entity/chest/x.png. */
	@Nullable
	private static ResourceLocation texture(@Nullable Object material) {
		if (material == null) {
			return null;
		}
		try {
			Method texture = material.getClass().getMethod("texture");
			if (texture.invoke(material) instanceof ResourceLocation location) {
				return new ResourceLocation(location.getNamespace(), "textures/" + location.getPath() + ".png");
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			// no texture: not drawn
		}
		return null;
	}

	/** 1.21's chest geometry (y up, in pixels, a 64x64 texture): the bottom, the lid and the lock of one shape. */
	private static final class Part {
		final ModelRenderer bottom;
		final ModelRenderer lid;
		final ModelRenderer lock;

		Part(float x, float width, float lockX, float lockWidth) {
			ModelBase model = new ModelBase() {
			};
			model.textureWidth = 64;
			model.textureHeight = 64;
			this.bottom = new ModelRenderer(model, 0, 19);
			this.bottom.addBox(x, 0.0F, 1.0F, (int) width, 10, 14);
			this.lid = new ModelRenderer(model, 0, 0);
			this.lid.addBox(x, 0.0F, 0.0F, (int) width, 5, 14);
			this.lid.setRotationPoint(0.0F, 9.0F, 1.0F);
			this.lock = new ModelRenderer(model, 0, 0);
			this.lock.addBox(lockX, -2.0F, 14.0F, (int) lockWidth, 4, 1);
			this.lock.setRotationPoint(0.0F, 9.0F, 1.0F);
		}
	}
}
