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
package io.github.xponer2.translator.runtime.redirect;

import io.github.xponer2.translator.client.RecipeBookComponent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.recipebook.GuiRecipeBook;

/**
 * Static stand-ins for 1.21 GUI and client calls on 1.12.2 classes (neoforge-1.21.1-redirects.tsv): the receiver comes
 * first.
 * Client only.
 */
public final class GuiBridges {
	private GuiBridges() {
	}

	/**
	 * 1.21 {@code recipeBook.init(width, height, minecraft, widthTooNarrow, menu)} on a field of the book's type (six
	 * mods' inventory screens: Curios, the Aether, Twilight Forest's uncrafting table): the facade's, which keeps the
	 * book of a 1.21 menu closed; a 1.12.2 book is left as it is.
	 */
	public static void init(GuiRecipeBook book, int width, int height, Minecraft minecraft, boolean widthTooNarrow, Object menu) {
		if (book instanceof RecipeBookComponent component) {
			component.init(width, height, minecraft, widthTooNarrow, menu);
		}
	}

	/**
	 * 1.21 {@code minecraft.getLastInputType()} (the carried InputType): always the mouse, 1.12.2 screens have no
	 * keyboard navigation. Jade's config lists asked it while drawing and weren't drawn at all.
	 */
	/**
	 * NeoForge {@code renderLivingEvent.getRenderer()}: the renderer as the 1.21 one mods expect (a mod's own, or a
	 * view of the 1.12.2 one; see VanillaRendererView). Mekanism, Twilight Forest and Artifacts pose models in it.
	 */
	@javax.annotation.Nullable
	public static <R> R getRenderer(net.minecraftforge.client.event.RenderLivingEvent<?> event) {
		return io.github.xponer2.translator.client.entity.VanillaRendererView.of(event.getRenderer());
	}

	/** 1.21 {@code modelResourceLocation.id()}: the model's location without its variant (Mekanism's and the Aether's model bakes). */
	public static net.minecraft.util.ResourceLocation id(net.minecraft.client.renderer.block.model.ModelResourceLocation location) {
		return new net.minecraft.util.ResourceLocation(location.getNamespace(), location.getPath());
	}

	/** 1.21 {@code itemDisplayContext.firstPerson()}: one of the first person hands. */
	public static boolean firstPerson(net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType context) {
		return context == net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND
				|| context == net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND;
	}

	/**
	 * 1.21 {@code itemDisplayContext.getSerializedName()}: the JSON key of the transform ({@code thirdperson_lefthand},
	 * {@code gui}, ...), what the mods' model loaders read and write.
	 */
	public static String getSerializedName(net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType context) {
		return context.name().toLowerCase(java.util.Locale.ROOT).replace("_person_", "person_").replace("_hand", "hand");
	}

	/** 1.21 {@code itemDisplayContext.getId()}: 1.21's ids are 1.12.2's order. */
	public static byte getId(net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType context) {
		return (byte) context.ordinal();
	}

	/** NeoForge {@code isModded()}: a context a mod added; 1.12.2 has only vanilla's. */
	public static boolean isModded(net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType context) {
		return false;
	}

	/**
	 * NeoForge {@code new RenderTooltipEvent.Color(stack, graphics, x, y, font, background, borderStart, borderEnd,
	 * components)}: Create and Ars Nouveau ask the mods for the colors of their own tooltips with it.
	 */
	public static net.minecraftforge.client.event.RenderTooltipEvent.Color newRenderTooltipEvent_Color(net.minecraft.item.ItemStack stack,
			@javax.annotation.Nullable Object graphics, int x, int y, net.minecraft.client.gui.FontRenderer font, int background, int borderStart,
			int borderEnd, @javax.annotation.Nullable java.util.List<?> components) {
		return new net.minecraftforge.client.event.RenderTooltipEvent.Color(stack, java.util.Collections.emptyList(), x, y, font, background, borderStart,
				borderEnd);
	}

	/** NeoForge {@code setBackgroundEnd(color)}: 1.12.2's tooltip has one background color; the start one is it. */
	public static void setBackgroundEnd(net.minecraftforge.client.event.RenderTooltipEvent.Color event, int color) {
	}

	/** 1.21 {@code new PackMetadataSection(description, format, supportedFormats)}: 1.12.2's has no range of formats. */
	public static net.minecraft.client.resources.data.PackMetadataSection newPackMetadataSection(net.minecraft.util.text.ITextComponent description,
			int format, @javax.annotation.Nullable java.util.Optional<?> supportedFormats) {
		return new net.minecraft.client.resources.data.PackMetadataSection(description, format);
	}

	/** 1.21 {@code supportedFormats()}: none. */
	public static java.util.Optional<?> supportedFormats(net.minecraft.client.resources.data.PackMetadataSection section) {
		return java.util.Optional.empty();
	}

	/**
	 * 1.21 {@code blockEntityRenderDispatcher.renderItem(blockEntity, pose, buffers, light, overlay)}: a block entity
	 * drawn as an item in the current pose (the item renderers of Powah, Ars Nouveau, Iron's Spells, Supplementaries,
	 * Twilight Forest draw their blocks' models so), through its 1.12.2 renderer as the item renderer draws chests.
	 */
	public static boolean renderItem(net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher dispatcher,
			net.minecraft.tileentity.TileEntity blockEntity, Object pose, Object buffers, int light, int overlay) {
		if (blockEntity == null || dispatcher.getRenderer(blockEntity) == null) {
			return false;
		}
		io.github.xponer2.translator.client.MultiBufferSource.INSTANCE.endBatch();
		dispatcher.render(blockEntity, 0.0D, 0.0D, 0.0D, Minecraft.getMinecraft().getRenderPartialTicks());
		return true;
	}

	/** 1.21 {@code options.mainHand()}: the main hand option over 1.12.2's field. */
	public static io.github.xponer2.translator.client.OptionInstance<net.minecraft.util.EnumHandSide> mainHand(net.minecraft.client.settings.GameSettings options) {
		return new io.github.xponer2.translator.client.OptionInstance<>(() -> options.mainHand, value -> options.mainHand = value);
	}

	/** NeoForge {@code renderTooltipEvent.getGraphics()}: the drawing of the tooltip (JEI, Patchouli, Ars Nouveau draw into it). */
	public static io.github.xponer2.translator.client.GuiGraphics getGraphics(net.minecraftforge.client.event.RenderTooltipEvent event) {
		return new io.github.xponer2.translator.client.GuiGraphics(Minecraft.getMinecraft());
	}

	/**
	 * 1.21 {@code texture.setFilter(blur, mipmap)}: the filter of that texture, bound first as 1.21 does. The tables
	 * named it 1.12.2's setBlurMipmapDirect, which sets the texture bound now: a mod's blurred texture blurred the title
	 * screen's logo, drawn just before.
	 */
	public static void setFilter(net.minecraft.client.renderer.texture.AbstractTexture texture, boolean blur, boolean mipmap) {
		net.minecraft.client.renderer.GlStateManager.bindTexture(texture.getGlTextureId());
		texture.setBlurMipmapDirect(blur, mipmap);
	}

	/** 1.21 {@code gameRenderer.lightTexture()}: the light map 1.12.2's entity renderer keeps. */
	public static io.github.xponer2.translator.client.LightTexture lightTexture(net.minecraft.client.renderer.EntityRenderer renderer) {
		return io.github.xponer2.translator.client.LightTexture.INSTANCE;
	}

	/** 1.21 {@code frustum.isVisible(box)}: 1.12.2's frustum test (Create and the Aether cull their renders with it). */
	public static boolean isVisible(net.minecraft.client.renderer.culling.ICamera frustum, net.minecraft.util.math.AxisAlignedBB box) {
		return frustum.isBoundingBoxInFrustum(box);
	}

	/** 1.21 {@code spriteContents.metadata()} (the contents are the sprite here): the "gui" section of a GUI sprite. */
	@javax.annotation.Nullable
	public static Object metadata(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
		return io.github.xponer2.translator.client.GuiSprite.carriedMetadata(sprite);
	}

	/** 1.21 {@code minecraft.getGuiSprites()}: JEI's, Jade's and GuideME's GUI sprites (each its own texture here). */
	public static io.github.xponer2.translator.client.GuiSpriteManager getGuiSprites(Minecraft minecraft) {
		return io.github.xponer2.translator.client.GuiSpriteManager.INSTANCE;
	}

	@javax.annotation.Nullable
	public static Object getLastInputType(Minecraft minecraft) {
		return CarriedBridges.constant("net.minecraft.client.InputType", "MOUSE");
	}

	/**
	 * 1.21 {@code blockEntityRenderDispatcher.renderItem(blockEntity, pose, buffers, light, overlay)}: a block entity
	 * drawn as an item (Twilight Forest's trophies and chests, Powah's reactor, Supplementaries' item renderers) by
	 * its 1.12.2 renderer at the GL matrix's origin.
	 */
	public static void renderItem(net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher dispatcher, net.minecraft.tileentity.TileEntity entity,
			io.github.xponer2.translator.client.PoseStack pose, io.github.xponer2.translator.client.MultiBufferSource buffers, int light, int overlay) {
		if (entity == null) {
			return;
		}
		buffers.endBatch();
		try {
			dispatcher.render(entity, 0.0D, 0.0D, 0.0D, 0.0F);
		} catch (RuntimeException | LinkageError e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("BlockEntityRenderDispatcher.renderItem " + entity.getClass().getName(), e);
		}
		buffers.endBatch();
	}
}
