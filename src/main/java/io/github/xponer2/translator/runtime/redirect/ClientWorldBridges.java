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

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.chat.NarratorChatListener;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The 1.21 members of the client's world, game and level renderer that 1.12.2 has under other names or not at all
 * (R rows of WorldClient, Minecraft, RenderGlobal and NarratorChatListener): the dimension's special effects, the
 * narrator, the overlay, leaving a world.
 */
@SideOnly(Side.CLIENT)
public final class ClientWorldBridges {
	private static final org.apache.logging.log4j.Logger LOGGER = org.apache.logging.log4j.LogManager.getLogger("Translator Runtime");
	private static final String EFFECTS_MANAGER = "net.neoforged.neoforge.client.DimensionSpecialEffectsManager";
	private static final ResourceLocation OVERWORLD = new ResourceLocation("minecraft", "overworld");
	private static final ResourceLocation NETHER = new ResourceLocation("minecraft", "the_nether");
	private static final ResourceLocation END = new ResourceLocation("minecraft", "the_end");

	private static boolean effectsInitialized;
	@Nullable
	private static java.lang.reflect.Method getForType;
	private static boolean wireframe;
	private static boolean sectionPath;
	private static boolean sectionVisibility;
	private static int rainSoundTime;

	private ClientWorldBridges() {
	}

	// --- the dimension's special effects -----------------------------------------------------------------------------

	/**
	 * NeoForge's {@code DimensionSpecialEffectsManager.init()}: the vanilla effects, then the mods' own
	 * (RegisterDimensionSpecialEffectsEvent: the Aether's and Twilight Forest's skies, clouds and fog). Once, in the
	 * client's init; never posted before.
	 */
	public static synchronized void initDimensionEffects() {
		if (effectsInitialized) {
			return;
		}
		effectsInitialized = true;
		try {
			Class<?> manager = Class.forName(EFFECTS_MANAGER, true, ClientWorldBridges.class.getClassLoader());
			manager.getMethod("init").invoke(null);
			getForType = manager.getMethod("getForType", ResourceLocation.class);
		} catch (ClassNotFoundException e) {
			// no API jar: nothing asks for effects either
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			LOGGER.warn("The dimension effects of the mods weren't registered", e instanceof java.lang.reflect.InvocationTargetException ite ? ite.getCause() : e);
		}
	}

	/** The 1.21 effects id of a dimension: vanilla's three, a mod dimension's type's {@code effects}, else the overworld's. */
	public static ResourceLocation effectsLocation(int dimension) {
		switch (dimension) {
			case -1:
				return NETHER;
			case 1:
				return END;
			case 0:
				return OVERWORLD;
			default:
				ResourceLocation effects = io.github.xponer2.translator.world.DataDimensions.effects(dimension);
				return effects != null ? effects : OVERWORLD;
		}
	}

	/**
	 * 1.21 {@code clientLevel.effects()}: the DimensionSpecialEffects (carried) of the world's dimension. The mods ask
	 * whether it is their own (their sky, clouds and weather) and read its cloud height, ground and light.
	 */
	@Nullable
	public static Object effects(WorldClient world) {
		initDimensionEffects();
		if (getForType == null) {
			return null;
		}
		try {
			return getForType.invoke(null, effectsLocation(world.provider.getDimension()));
		} catch (ReflectiveOperationException e) {
			return null;
		}
	}

	// --- the client's world ------------------------------------------------------------------------------------------

	/** 1.21 {@code setSectionDirtyWithNeighbors(x, y, z)}: the section and its neighbours are built again. */
	public static void setSectionDirtyWithNeighbors(WorldClient world, int sectionX, int sectionY, int sectionZ) {
		world.markBlockRangeForRenderUpdate(sectionX * 16 - 16, sectionY * 16 - 16, sectionZ * 16 - 16, sectionX * 16 + 31, sectionY * 16 + 31,
				sectionZ * 16 + 31);
	}

	/** 1.21 {@code removeEntity(id, reason)}. */
	public static void removeEntity(WorldClient world, int id, @Nullable Object reason) {
		world.removeEntityFromWorld(id);
	}

	/** 1.21 {@code pollLightUpdates()}: 1.12.2 lights as blocks change. */
	public static void pollLightUpdates(WorldClient world) {
	}

	/** 1.21 {@code getCloudColor(partialTick)}. */
	public static Vec3d getCloudColor(WorldClient world, float partialTick) {
		return world.getCloudColour(partialTick);
	}

	// --- the game ----------------------------------------------------------------------------------------------------

	/** 1.21 {@code getNarrator()}: the game's narrator (GameNarrator is 1.12.2's NarratorChatListener). */
	public static NarratorChatListener getNarrator(Minecraft minecraft) {
		return NarratorChatListener.INSTANCE;
	}

	/** 1.21 {@code narrator.sayNow(text)}: said if the narrator is on for the game's messages. */
	public static void sayNow(NarratorChatListener narrator, @Nullable String text) {
		if (text != null && !text.isEmpty()) {
			narrator.say(ChatType.SYSTEM, new TextComponentString(text));
		}
	}

	public static void sayNow(NarratorChatListener narrator, @Nullable ITextComponent text) {
		if (text != null) {
			narrator.say(ChatType.SYSTEM, text);
		}
	}

	/** 1.21 {@code sayChat(text)}. */
	public static void sayChat(NarratorChatListener narrator, @Nullable ITextComponent text) {
		if (text != null) {
			narrator.say(ChatType.CHAT, text);
		}
	}

	/**
	 * 1.21 {@code getOverlay()}: the loading overlay over the screen. 1.12.2 loads before the first screen and has none
	 * after it; the mods wait for it to be gone (Jade's overlay, GuideMe's start, the Aether's menus).
	 */
	@Nullable
	public static Object getOverlay(Minecraft minecraft) {
		return null;
	}

	/** 1.21 {@code setOverlay(overlay)}: none on 1.12.2. */
	public static void setOverlay(Minecraft minecraft, @Nullable Object overlay) {
	}

	/** 1.21 {@code getChatListener()}: the game's messages to the player (1.12.2's chat). */
	public static io.github.xponer2.translator.client.ChatListener getChatListener(Minecraft minecraft) {
		return io.github.xponer2.translator.client.ChatListener.INSTANCE;
	}

	/** 1.21 {@code getFps()}. */
	public static int getFps(Minecraft minecraft) {
		return Minecraft.getDebugFPS();
	}

	/** 1.21 {@code forceSetScreen(screen)}: the screen without the checks of setScreen. */
	public static void forceSetScreen(Minecraft minecraft, @Nullable GuiScreen screen) {
		minecraft.displayGuiScreen(screen);
	}

	/** 1.21 {@code disconnect()}: leave the world (and the integrated server), back to the title screen. */
	public static void disconnect(Minecraft minecraft) {
		disconnect(minecraft, null);
	}

	/** 1.21 {@code disconnect(nextScreen)}: leave the world, then that screen. */
	public static void disconnect(Minecraft minecraft, @Nullable GuiScreen next) {
		if (minecraft.world != null) {
			minecraft.world.sendQuittingDisconnectingPacket();
		}
		minecraft.loadWorld(null);
		minecraft.displayGuiScreen(next != null ? next : new GuiMainMenu());
	}

	/** 1.21 {@code disconnect(nextScreen, keepResourcePacks)}. */
	public static void disconnect(Minecraft minecraft, @Nullable GuiScreen next, boolean keepResourcePacks) {
		disconnect(minecraft, next);
	}

	/**
	 * 1.21 {@code Minecraft.addCustomNbtData(stack, blockEntity, registries)}: the block entity's data on the picked
	 * stack, as 1.12.2's pick block with control does.
	 */
	public static void addCustomNbtData(ItemStack stack, @Nullable TileEntity blockEntity, @Nullable Object registries) {
		if (stack.isEmpty() || blockEntity == null) {
			return;
		}
		NBTTagCompound tag = blockEntity.writeToNBT(new NBTTagCompound());
		tag.removeTag("x");
		tag.removeTag("y");
		tag.removeTag("z");
		tag.removeTag("id");
		stack.setTagInfo("BlockEntityTag", tag);
	}

	/** The debug flags of 1.21 (wireframe, the section path and visibility): kept, nothing draws them. */
	public static boolean wireframe(Minecraft minecraft) {
		return wireframe;
	}

	public static void wireframe(Minecraft minecraft, boolean value) {
		wireframe = value;
	}

	public static boolean sectionPath(Minecraft minecraft) {
		return sectionPath;
	}

	public static void sectionPath(Minecraft minecraft, boolean value) {
		sectionPath = value;
	}

	public static boolean sectionVisibility(Minecraft minecraft) {
		return sectionVisibility;
	}

	public static void sectionVisibility(Minecraft minecraft, boolean value) {
		sectionVisibility = value;
	}

	// --- the level renderer ------------------------------------------------------------------------------------------

	/** 1.21 {@code levelRenderer.renderBuffers}: the game's buffers (GeckoLib's renderers draw into them). */
	public static io.github.xponer2.translator.client.RenderBuffers renderBuffers(RenderGlobal renderer) {
		return io.github.xponer2.translator.client.RenderBuffers.INSTANCE;
	}

	/** 1.21 {@code rainSoundTime}: the ticks to the next rain sound (Twilight Forest's weather renderer counts it). */
	public static int rainSoundTime(RenderGlobal renderer) {
		return rainSoundTime;
	}

	public static void rainSoundTime(RenderGlobal renderer, int value) {
		rainSoundTime = value;
	}

	/** 1.21 {@code captureFrustum()} / {@code killFrustum()}: a debug view of the frustum, none on 1.12.2. */
	public static void captureFrustum(RenderGlobal renderer) {
	}

	public static void killFrustum(RenderGlobal renderer) {
	}
}
