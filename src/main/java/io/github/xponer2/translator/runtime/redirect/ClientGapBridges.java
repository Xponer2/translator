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

import javax.annotation.Nullable;

import io.github.xponer2.translator.util.particle.ParticleOptions;
import io.github.xponer2.translator.util.particle.VanillaParticle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.renderer.block.model.ModelManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Client calls of 1.21.1 mods the 1.12.2 classes name differently or lack: the texture atlas (1.12.2 has one for
 * blocks and items), frame and vertex buffers, the creative inventory tab, the server of the connection, rebuilding a
 * screen's widgets, a particle made from 1.21 particle options.
 */
@SideOnly(Side.CLIENT)
public final class ClientGapBridges {
	private ClientGapBridges() {
	}

	/** 1.21 {@code modelManager.getAtlas(id)}: 1.12.2 stitches blocks and items into one atlas. */
	public static TextureMap getAtlas(ModelManager manager, ResourceLocation atlas) {
		return Minecraft.getMinecraft().getTextureMapBlocks();
	}

	/** 1.21 {@code renderTarget.clear(onMac)}. */
	public static void clear(Framebuffer framebuffer, boolean onMac) {
		framebuffer.framebufferClear();
	}

	/** 1.21 {@code vertexBuffer.close()}: frees the GL buffer. */
	public static void close(VertexBuffer buffer) {
		buffer.deleteGlBuffers();
	}

	/** 1.21 {@code creativeScreen.isInventoryOpen()}: the survival inventory tab is selected. */
	public static boolean isInventoryOpen(GuiContainerCreative screen) {
		return screen.getSelectedTabIndex() == CreativeTabs.INVENTORY.getIndex();
	}

	/** 1.21 {@code connection.getServerData()}: the server the client is on, null in single player. */
	@Nullable
	public static ServerData getServerData(NetHandlerPlayClient connection) {
		return Minecraft.getMinecraft().getCurrentServerData();
	}

	/** 1.21 {@code screen.rebuildWidgets()}: the widgets made again (1.12.2 does it when the screen is resized). */
	public static void rebuildWidgets(GuiScreen screen) {
		screen.setWorldAndResolution(screen.mc != null ? screen.mc : Minecraft.getMinecraft(), screen.width, screen.height);
	}

	/** 1.21 {@code localPlayer.clientLevel}: the client world the player is in. */
	public static net.minecraft.client.multiplayer.WorldClient clientLevel(net.minecraft.client.entity.EntityPlayerSP player) {
		return (net.minecraft.client.multiplayer.WorldClient) player.world;
	}

	/** 1.21 {@code gameRenderer.getRenderDistance()}: in blocks. */
	public static float getRenderDistance(net.minecraft.client.renderer.EntityRenderer renderer) {
		return Minecraft.getMinecraft().gameSettings.renderDistanceChunks * 16.0F;
	}

	/** 1.21 {@code screen.handleComponentClicked(style)}: 1.12.2 handles the click of a component with that style. */
	public static boolean handleComponentClicked(GuiScreen screen, @Nullable net.minecraft.util.text.Style style) {
		if (style == null) {
			return false;
		}
		var component = new net.minecraft.util.text.TextComponentString("");
		component.setStyle(style);
		return screen.handleComponentClick(component);
	}

	/** 1.21 {@code narrator.sayNow(text)}: the 1.12.2 narrator reads it when it is on. */
	public static void sayNow(net.minecraft.client.gui.chat.NarratorChatListener narrator, net.minecraft.util.text.ITextComponent text) {
		narrator.say(net.minecraft.util.text.ChatType.SYSTEM, text);
	}

	public static void sayNow(net.minecraft.client.gui.chat.NarratorChatListener narrator, String text) {
		sayNow(narrator, new net.minecraft.util.text.TextComponentString(text));
	}

	// --- HumanoidModel (AgeableListModel) baby proportions: 1.12.2 ModelBiped draws children with these values -------------

	public static float babyHeadScale(net.minecraft.client.model.ModelBiped model) {
		return 2.0F;
	}

	public static float babyBodyScale(net.minecraft.client.model.ModelBiped model) {
		return 2.0F;
	}

	public static float babyYHeadOffset(net.minecraft.client.model.ModelBiped model) {
		return 16.0F;
	}

	public static float babyZHeadOffset(net.minecraft.client.model.ModelBiped model) {
		return 0.0F;
	}

	public static float bodyYOffset(net.minecraft.client.model.ModelBiped model) {
		return 24.0F;
	}

	public static boolean scaleHead(net.minecraft.client.model.ModelBiped model) {
		return true;
	}

	// --- RenderTarget ---------------------------------------------------------------------------------------------------

	public static int getColorTextureId(Framebuffer framebuffer) {
		return framebuffer.framebufferTexture;
	}

	/** 1.21 {@code target.resize(width, height, clearError)}: made again at that size. */
	public static void resize(Framebuffer framebuffer, int width, int height, boolean clearError) {
		framebuffer.createBindFramebuffer(width, height);
	}

	// --- LevelRenderer, GameRenderer -------------------------------------------------------------------------------------

	/** 1.21 {@code levelRenderer.getTicks()}: the renderer's tick count (1.12.2 counts it for the clouds). */
	public static int getTicks(net.minecraft.client.renderer.RenderGlobal renderer) {
		for (String name : new String[] {"field_72773_u", "cloudTickCounter"}) {
			try {
				var field = net.minecraft.client.renderer.RenderGlobal.class.getDeclaredField(name);
				field.setAccessible(true);
				return field.getInt(renderer);
			} catch (ReflectiveOperationException | RuntimeException e) {
				// the other name
			}
		}
		return (int) Minecraft.getMinecraft().world.getTotalWorldTime();
	}

	private static final io.github.xponer2.translator.client.OverlayTexture OVERLAY = new io.github.xponer2.translator.client.OverlayTexture();

	/** 1.21 {@code gameRenderer.overlayTexture()}: 1.12.2 has no hurt overlay texture, its setup does nothing. */
	public static io.github.xponer2.translator.client.OverlayTexture overlayTexture(net.minecraft.client.renderer.EntityRenderer renderer) {
		return OVERLAY;
	}

	// --- toasts, sounds -------------------------------------------------------------------------------------------------

	/** 1.21 {@code toast.width()} / {@code height()}: the 1.12.2 toast background is 160 x 32. */
	public static int width(net.minecraft.client.gui.toasts.IToast toast) {
		return 160;
	}

	public static int height(net.minecraft.client.gui.toasts.IToast toast) {
		return 32;
	}

	/** 1.21 {@code sound.getAttenuationDistance()}: 1.12.2 sounds fade out over 16 blocks. */
	public static int getAttenuationDistance(net.minecraft.client.audio.Sound sound) {
		return 16;
	}

	// --- Options: the carried 1.21 enums of the 1.12.2 settings ---------------------------------------------------------

	/** 1.21 {@code options.getBackgroundColor(opacity)}: black at that opacity (1.12.2 has no text background setting). */
	public static int getBackgroundColor(net.minecraft.client.settings.GameSettings options, float opacity) {
		return (int) (Math.max(0.0F, Math.min(1.0F, opacity)) * 255.0F) << 24;
	}

	public static int getBackgroundColor(net.minecraft.client.settings.GameSettings options, int color) {
		return color;
	}

	/** 1.21 {@code options.getCloudsType()}: OFF, FAST or FANCY (1.12.2 clouds 0, 1, 2). */
	@Nullable
	public static Object getCloudsType(net.minecraft.client.settings.GameSettings options) {
		return CarriedBridges.constant("net.minecraft.client.CloudStatus",
				new String[] {"OFF", "FAST", "FANCY"}[Math.max(0, Math.min(2, options.clouds))]);
	}

	/** 1.21 {@code options.particles()}: ALL, DECREASED or MINIMAL (1.12.2 particleSetting 0, 1, 2). */
	public static io.github.xponer2.translator.client.OptionInstance<Object> particles(net.minecraft.client.settings.GameSettings options) {
		return new io.github.xponer2.translator.client.OptionInstance<>(
				() -> CarriedBridges.constant("net.minecraft.client.ParticleStatus",
						new String[] {"ALL", "DECREASED", "MINIMAL"}[Math.max(0, Math.min(2, options.particleSetting))]),
				value -> {
					if (value instanceof Enum<?> status) {
						options.particleSetting = Math.min(2, status.ordinal());
					}
				});
	}

	/** 1.21 {@code options.graphicsMode()}: FANCY or FAST (1.12.2 has no FABULOUS). */
	public static io.github.xponer2.translator.client.OptionInstance<Object> graphicsMode(net.minecraft.client.settings.GameSettings options) {
		return new io.github.xponer2.translator.client.OptionInstance<>(
				() -> CarriedBridges.constant("net.minecraft.client.GraphicsStatus", options.fancyGraphics ? "FANCY" : "FAST"),
				value -> {
					if (value instanceof Enum<?> status) {
						options.fancyGraphics = !"FAST".equals(status.name());
					}
				});
	}

	/** 1.21 {@code levelRenderer.setSectionDirty(x, y, z)}: the 16x16x16 section is drawn again. */
	public static void setSectionDirty(net.minecraft.client.renderer.RenderGlobal renderer, int x, int y, int z) {
		renderer.markBlockRangeForRenderUpdate(x << 4, y << 4, z << 4, (x << 4) + 15, (y << 4) + 15, (z << 4) + 15);
	}

	public static void setSectionDirty(net.minecraft.client.renderer.RenderGlobal renderer, int x, int y, int z, boolean important) {
		setSectionDirty(renderer, x, y, z);
	}

	/** 1.21 {@code gameRenderer.getDarkenWorldAmount(partialTicks)}: 1.12.2 darkens for the wither only, by itself. */
	public static float getDarkenWorldAmount(net.minecraft.client.renderer.EntityRenderer renderer, float partialTicks) {
		return 0.0F;
	}

	public static java.util.Locale getJavaLocale(net.minecraft.client.resources.LanguageManager languages) {
		return java.util.Locale.forLanguageTag(languages.getCurrentLanguage().getLanguageCode().replace('_', '-'));
	}

	/** 1.21 {@code options.setCameraType(type)}: first person, third person back, front (1.12.2's thirdPersonView). */
	public static void setCameraType(net.minecraft.client.settings.GameSettings options, Object type) {
		if (type instanceof Enum<?> value) {
			options.thirdPersonView = value.ordinal();
		}
	}

	/** 1.21 {@code particleEngine.createParticle(options, ...)}: a vanilla particle made and added; others aren't drawn yet. */
	@Nullable
	public static Particle createParticle(ParticleManager engine, ParticleOptions options, double x, double y, double z, double xSpeed,
			double ySpeed, double zSpeed) {
		if (options instanceof VanillaParticle vanilla) {
			return engine.spawnEffectParticle(vanilla.legacyType().getParticleID(), x, y, z, xSpeed, ySpeed, zSpeed, vanilla.legacyArguments());
		}
		return null;
	}
}
