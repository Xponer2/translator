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
