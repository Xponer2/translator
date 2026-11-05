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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.xponer2.translator.runtime.mc.CeilingHangingSignBlock;
import io.github.xponer2.translator.runtime.mc.SignBlockEntity;
import io.github.xponer2.translator.runtime.mc.StandingSignBlock;
import io.github.xponer2.translator.runtime.mc.WallHangingSignBlock;
import io.github.xponer2.translator.runtime.mc.WallSignBlock;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelSign;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The signs of the 1.21.1 mods drawn as 1.21 draws them: standing and wall signs with the board of their wood
 * ({@code <mod>:textures/entity/signs/<wood>.png}, the layout of 1.12.2's sign), hanging signs on their chains or plank
 * ({@code entity/signs/hanging/<wood>.png}), the text on the front. A 1.21 sign's block has no model: nothing showed.
 */
@SideOnly(Side.CLIENT)
public final class SignRenderers extends TileEntitySpecialRenderer<SignBlockEntity> {
	private static final ResourceLocation VANILLA = new ResourceLocation("textures/entity/sign.png");
	private static final Map<String, ResourceLocation> TEXTURES = new ConcurrentHashMap<>();

	private final ModelSign sign = new ModelSign();
	private final HangingModel hanging = new HangingModel();

	public static void register() {
		ClientRegistry.bindTileEntitySpecialRenderer(SignBlockEntity.class, new SignRenderers());
	}

	@Override
	public void render(SignBlockEntity te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
		if (te.getWorld() == null) {
			return;
		}
		IBlockState state = te.getWorld().getBlockState(te.getPos());
		Block block = state.getBlock();
		GlStateManager.pushMatrix();
		try {
			if (block instanceof CeilingHangingSignBlock || block instanceof WallHangingSignBlock) {
				this.renderHanging(te, state, block, x, y, z, destroyStage);
			} else {
				this.renderSign(te, state, block, x, y, z, destroyStage);
			}
		} finally {
			GlStateManager.popMatrix();
		}
	}

	private void renderSign(SignBlockEntity te, IBlockState state, Block block, double x, double y, double z, int destroyStage) {
		float scale = 0.6666667F;
		GlStateManager.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
		if (block instanceof StandingSignBlock && state.getPropertyKeys().contains(StandingSignBlock.ROTATION)) {
			GlStateManager.rotate(-(state.getValue(StandingSignBlock.ROTATION) * 360) / 16.0F, 0.0F, 1.0F, 0.0F);
			this.sign.signStick.showModel = true;
		} else {
			GlStateManager.rotate(-angle(facing(state)), 0.0F, 1.0F, 0.0F);
			GlStateManager.translate(0.0F, -0.3125F, -0.4375F);
			this.sign.signStick.showModel = false;
		}
		this.bind(destroyStage, texture(block, false));
		GlStateManager.enableRescaleNormal();
		GlStateManager.pushMatrix();
		GlStateManager.scale(scale, -scale, -scale);
		this.sign.renderSign();
		GlStateManager.popMatrix();
		this.unbind(destroyStage);
		// the text, as 1.12.2's sign renderer draws it
		GlStateManager.translate(0.0F, 0.33333334F, 0.046666667F);
		GlStateManager.scale(0.010416667F, -0.010416667F, 0.010416667F);
		this.text(te, 90, 10, -te.signText.length * 5);
		GlStateManager.disableRescaleNormal();
	}

	/** 1.21 HangingSignRenderer: pivot near the top, the plank for a wall sign, two chains (or one "attached" V). */
	private void renderHanging(SignBlockEntity te, IBlockState state, Block block, double x, double y, double z, int destroyStage) {
		boolean wall = block instanceof WallHangingSignBlock;
		boolean attached = !wall && state.getPropertyKeys().contains(CeilingHangingSignBlock.ATTACHED) && state.getValue(CeilingHangingSignBlock.ATTACHED);
		GlStateManager.translate((float) x + 0.5F, (float) y + 0.9375F, (float) z + 0.5F);
		float angle = wall ? angle(facing(state))
				: state.getPropertyKeys().contains(StandingSignBlock.ROTATION) ? state.getValue(StandingSignBlock.ROTATION) * 22.5F : 0.0F;
		GlStateManager.rotate(-angle, 0.0F, 1.0F, 0.0F);
		GlStateManager.translate(0.0F, -0.3125F, 0.0F);
		this.hanging.plank.showModel = wall;
		this.hanging.vChains.showModel = attached;
		for (ModelRenderer chain : this.hanging.chains) {
			chain.showModel = !attached;
		}
		this.bind(destroyStage, texture(block, true));
		GlStateManager.enableRescaleNormal();
		GlStateManager.disableCull();
		GlStateManager.pushMatrix();
		GlStateManager.scale(1.0F, -1.0F, -1.0F);
		this.hanging.render(0.0625F);
		GlStateManager.popMatrix();
		GlStateManager.enableCull();
		this.unbind(destroyStage);
		// the text on the board's front (1.21: lines of 9, 0.9 of the sign's text size)
		GlStateManager.translate(0.0F, -0.32F, 0.0675F);
		float text = 0.0140625F;
		GlStateManager.scale(text, -text, text);
		this.text(te, 60, 9, -te.signText.length * 9 / 2 + 1);
		GlStateManager.disableRescaleNormal();
	}

	private void text(SignBlockEntity te, int width, int lineHeight, int top) {
		FontRenderer font = this.getFontRenderer();
		if (font == null) {
			return;
		}
		GlStateManager.glNormal3f(0.0F, 0.0F, -0.010416667F);
		GlStateManager.depthMask(false);
		for (int i = 0; i < te.signText.length; i++) {
			ITextComponent line = te.signText[i];
			if (line == null) {
				continue;
			}
			List<ITextComponent> parts = GuiUtilRenderComponents.splitText(line, width, font, false, true);
			String shown = parts != null && !parts.isEmpty() ? parts.get(0).getFormattedText() : "";
			if (i == te.lineBeingEdited) {
				shown = "> " + shown + " <";
			}
			font.drawString(shown, -font.getStringWidth(shown) / 2, top + i * lineHeight, 0);
		}
		GlStateManager.depthMask(true);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private void bind(int destroyStage, ResourceLocation texture) {
		if (destroyStage >= 0) {
			this.bindTexture(DESTROY_STAGES[destroyStage]);
			GlStateManager.matrixMode(5890);
			GlStateManager.pushMatrix();
			GlStateManager.scale(4.0F, 2.0F, 1.0F);
			GlStateManager.translate(0.0625F, 0.0625F, 0.0625F);
			GlStateManager.matrixMode(5888);
		} else {
			this.bindTexture(texture);
		}
	}

	private void unbind(int destroyStage) {
		if (destroyStage >= 0) {
			GlStateManager.matrixMode(5890);
			GlStateManager.popMatrix();
			GlStateManager.matrixMode(5888);
		}
	}

	private static EnumFacing facing(IBlockState state) {
		return state.getPropertyKeys().contains(WallSignBlock.FACING) ? state.getValue(WallSignBlock.FACING) : EnumFacing.NORTH;
	}

	/** 1.21 Direction.toYRot: south 0, west 90, north 180, east 270. */
	private static float angle(EnumFacing facing) {
		return facing.getHorizontalIndex() * 90.0F;
	}

	/** The texture of the sign's wood: the mod's {@code entity/signs[/hanging]/<wood>}, 1.12.2's sign when it has none. */
	private static ResourceLocation texture(Block block, boolean hanging) {
		Object type = block instanceof StandingSignBlock standing ? standing.type() : block instanceof WallSignBlock wall ? wall.type() : null;
		String name = woodName(type);
		if (name == null) {
			return VANILLA;
		}
		return TEXTURES.computeIfAbsent(name + (hanging ? "#hanging" : ""), key -> {
			ResourceLocation id = new ResourceLocation(name);
			ResourceLocation texture = new ResourceLocation(id.getNamespace(), "textures/entity/signs/" + (hanging ? "hanging/" : "") + id.getPath() + ".png");
			try (var resource = Minecraft.getMinecraft().getResourceManager().getResource(texture)) {
				return texture;
			} catch (Exception e) {
				return VANILLA;
			}
		});
	}

	/** 1.21 {@code WoodType.name()} ("biomesoplenty:fir"). */
	@javax.annotation.Nullable
	private static String woodName(@javax.annotation.Nullable Object type) {
		if (type == null) {
			return null;
		}
		try {
			Object name = type.getClass().getMethod("name").invoke(type);
			return name instanceof String text ? text : null;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	/** 1.21 HangingSignRenderer's layer: board, plank, two pairs of chains, a V of chains (64 by 32). */
	private static final class HangingModel extends ModelBase {
		final ModelRenderer board;
		final ModelRenderer plank;
		final ModelRenderer vChains;
		final ModelRenderer[] chains;

		HangingModel() {
			this.textureWidth = 64;
			this.textureHeight = 32;
			this.board = new ModelRenderer(this, 0, 12);
			this.board.addBox(-7.0F, 0.0F, -1.0F, 14, 10, 2);
			this.plank = new ModelRenderer(this, 0, 0);
			this.plank.addBox(-8.0F, -6.0F, -2.0F, 16, 2, 4);
			this.vChains = new ModelRenderer(this, 14, 6);
			this.vChains.addBox(-6.0F, -6.0F, 0.0F, 12, 6, 0);
			this.chains = new ModelRenderer[] {chain(0, -5.0F, -1.0F), chain(6, -5.0F, 1.0F), chain(0, 5.0F, -1.0F), chain(6, 5.0F, 1.0F)};
		}

		private ModelRenderer chain(int u, float x, float turn) {
			var chain = new ModelRenderer(this, u, 6);
			chain.addBox(-1.5F, 0.0F, 0.0F, 3, 6, 0);
			chain.setRotationPoint(x, -6.0F, 0.0F);
			chain.rotateAngleY = turn * (float) Math.PI / 4.0F;
			return chain;
		}

		void render(float scale) {
			this.board.render(scale);
			this.plank.render(scale);
			this.vChains.render(scale);
			for (ModelRenderer chain : this.chains) {
				chain.render(scale);
			}
		}
	}
}
