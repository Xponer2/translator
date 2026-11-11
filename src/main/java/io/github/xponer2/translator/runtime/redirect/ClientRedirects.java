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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** Static stand-ins for 1.21 client methods 1.12.2 client classes lack (runtime; client classes only). */
@SideOnly(Side.CLIENT)
public final class ClientRedirects {
	// the HUD's heights (NeoForge Gui.leftHeight / rightHeight): Forge 1.12.2's GuiIngameForge.left_height / right_height,
	// which mods raise for the bars they draw over the hotbar (AppleSkin, Farmer's Delight, Mekanism)
	/**
	 * 1.21 {@code sprite.contents().byMipLevel}: the sprite's pixels per mip level (1.12.2's first frame, ARGB) as native
	 * images (JourneyMap takes its map colors from them: with none every block was colorless and nothing was mapped).
	 */
	public static io.github.xponer2.translator.client.NativeImage[] byMipLevel(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
		int width = sprite.getIconWidth();
		int height = sprite.getIconHeight();
		if (sprite.getFrameCount() == 0 || width <= 0 || height <= 0) {
			return new io.github.xponer2.translator.client.NativeImage[] {new io.github.xponer2.translator.client.NativeImage(1, 1, true)};
		}
		int[][] levels = sprite.getFrameTextureData(0);
		var images = new io.github.xponer2.translator.client.NativeImage[Math.max(1, levels.length)];
		for (int level = 0; level < images.length; level++) {
			int w = Math.max(1, width >> level);
			int h = Math.max(1, height >> level);
			var image = new io.github.xponer2.translator.client.NativeImage(w, h, true);
			int[] pixels = level < levels.length ? levels[level] : null;
			if (pixels != null) {
				for (int y = 0; y < h; y++) {
					for (int x = 0; x < w && y * w + x < pixels.length; x++) {
						int argb = pixels[y * w + x];
						// NativeImage keeps ABGR
						int abgr = (argb & 0xFF00FF00) | (argb & 0xFF) << 16 | (argb >> 16) & 0xFF;
						image.setPixelRGBA(x, y, abgr);
					}
				}
			}
			images[level] = image;
		}
		return images;
	}

	/** 1.21 {@code gui.getDebugOverlay()} (1.12.2 keeps it private): the overlay of the F3 screen. */
	public static net.minecraft.client.gui.GuiOverlayDebug getDebugOverlay(net.minecraft.client.gui.GuiIngame gui) {
		return ScreenBridges.getDebugOverlay(Minecraft.getMinecraft());
	}

	/** NeoForge {@code gui.getLayerCount()}: the HUD layers drawn so far; 1.12.2 draws its HUD in one. */
	public static int getLayerCount(net.minecraft.client.gui.GuiIngame gui) {
		return 0;
	}

	public static int leftHeight(net.minecraft.client.gui.GuiIngame gui) {
		return net.minecraftforge.client.GuiIngameForge.left_height;
	}

	public static void leftHeight(net.minecraft.client.gui.GuiIngame gui, int height) {
		net.minecraftforge.client.GuiIngameForge.left_height = height;
	}

	public static int rightHeight(net.minecraft.client.gui.GuiIngame gui) {
		return net.minecraftforge.client.GuiIngameForge.right_height;
	}

	public static void rightHeight(net.minecraft.client.gui.GuiIngame gui, int height) {
		net.minecraftforge.client.GuiIngameForge.right_height = height;
	}

	/** 1.21 {@code playerInfo.getSkin()}: the skin, cape and model of a player in the tab list (Chat Heads). */
	public static io.github.xponer2.translator.client.PlayerSkin getSkin(net.minecraft.client.network.NetworkPlayerInfo info) {
		return new io.github.xponer2.translator.client.PlayerSkin(info.getLocationSkin(), null, info.getLocationCape(), info.getLocationElytra(),
				io.github.xponer2.translator.client.PlayerSkin.Model.byName(info.getSkinType()), false);
	}

	/** 1.21 {@code humanoidModel.swimAmount}: 1.12.2's players don't swim. */
	public static float swimAmount(net.minecraft.client.model.ModelBiped model) {
		return 0.0F;
	}

	/** 1.21 {@code blockEntityRenderDispatcher.camera}: the game's camera. */
	public static io.github.xponer2.translator.client.Camera camera(net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher dispatcher) {
		return io.github.xponer2.translator.client.Camera.main();
	}

	/** NeoForge {@code connection.hasChannel(payload)} on the client: the Translator's channels are on both sides. */
	public static boolean hasChannel(net.minecraft.client.network.NetHandlerPlayClient connection, Object type) {
		return true;
	}

	/** 1.21 {@code entityRenderDispatcher.camera}: the game's camera. */
	public static io.github.xponer2.translator.client.Camera camera(net.minecraft.client.renderer.entity.RenderManager manager) {
		return io.github.xponer2.translator.client.Camera.main();
	}

	/** 1.21 {@code entityRenderDispatcher.cameraOrientation()}: the camera's rotation (billboards turn by it). */
	public static org.joml.Quaternionf cameraOrientation(net.minecraft.client.renderer.entity.RenderManager manager) {
		return io.github.xponer2.translator.client.Camera.main().rotation();
	}

	private static final java.util.Map<net.minecraft.client.gui.FontRenderer, io.github.xponer2.translator.client.StringSplitter> SPLITTERS =
			new java.util.WeakHashMap<>();

	/** 1.21 {@code font.getSplitter()}: widths and line breaks on the 1.12.2 font. */
	public static io.github.xponer2.translator.client.StringSplitter getSplitter(net.minecraft.client.gui.FontRenderer font) {
		synchronized (SPLITTERS) {
			return SPLITTERS.computeIfAbsent(font, io.github.xponer2.translator.client.StringSplitter::new);
		}
	}

	/** 1.21 {@code model.copyPropertiesTo(other)}: 1.12.2's {@code other.setModelAttributes(model)}, the other way round. */
	public static void copyPropertiesTo(net.minecraft.client.model.ModelBiped model, net.minecraft.client.model.ModelBiped other) {
		other.setModelAttributes(model);
	}

	private ClientRedirects() {
	}

	public static boolean consumeClick(KeyBinding key) {
		return key.isPressed();
	}

	public static io.github.xponer2.translator.runtime.neoforge.InputConstants.Key getKey(KeyBinding key) {
		return io.github.xponer2.translator.runtime.neoforge.InputConstants.fromLwjgl(key.getKeyCode());
	}

	/** 1.21 {@code keyMapping.getDefaultKey()} (key binding screens reset to it: Controlling, Jade, Cloth Config). */
	public static io.github.xponer2.translator.runtime.neoforge.InputConstants.Key getDefaultKey(KeyBinding key) {
		return io.github.xponer2.translator.runtime.neoforge.InputConstants.fromLwjgl(key.getKeyCodeDefault());
	}

	public static boolean isDown(KeyBinding key) {
		return key.isKeyDown();
	}

	/** The key of a screen's keyPressed (GLFW code) against a binding (LWJGL code). */
	public static boolean isActiveAndMatches(KeyBinding key, io.github.xponer2.translator.runtime.neoforge.InputConstants.Key input) {
		return key.isActiveAndMatches(io.github.xponer2.translator.runtime.neoforge.KeyMapping.toLwjgl(input.getValue()));
	}

	// --- Minecraft ---------------------------------------------------------------------------------------------------

	public static io.github.xponer2.translator.client.Window getWindow(Minecraft minecraft) {
		return io.github.xponer2.translator.client.Window.get();
	}

	/** The ids of the 1.21 fonts (Minecraft.DEFAULT_FONT, UNIFORM_FONT): 1.12.2 has one font renderer. */
	public static final net.minecraft.util.ResourceLocation DEFAULT_FONT = new net.minecraft.util.ResourceLocation("minecraft", "default");
	public static final net.minecraft.util.ResourceLocation UNIFORM_FONT = new net.minecraft.util.ResourceLocation("minecraft", "uniform");
	public static final net.minecraft.util.ResourceLocation ALT_FONT = new net.minecraft.util.ResourceLocation("minecraft", "alt");

	/** The 1.21 resource manager of the client (assets, listed from the 1.21.1 mod jars). */
	public static io.github.xponer2.translator.resources.ResourceManager getResourceManager(Minecraft minecraft) {
		return io.github.xponer2.translator.resources.ResourceManager.client();
	}

	public static boolean isWindowActive(Minecraft minecraft) {
		return org.lwjgl.opengl.Display.isActive();
	}

	public static void setWindowActive(Minecraft minecraft, boolean active) {
	}

	public static com.mojang.authlib.GameProfile getGameProfile(Minecraft minecraft) {
		return minecraft.getSession().getProfile();
	}

	public static net.minecraft.profiler.Profiler getProfiler(Minecraft minecraft) {
		return minecraft.profiler;
	}

	public static boolean isEnforceUnicode(Minecraft minecraft) {
		return minecraft.gameSettings.forceUnicodeFont;
	}

	public static java.util.Locale getLocale(Minecraft minecraft) {
		return java.util.Locale.getDefault();
	}

	public static boolean shouldEntityAppearGlowing(Minecraft minecraft, net.minecraft.entity.Entity entity) {
		return entity.isGlowing();
	}

	public static boolean isTextFilteringEnabled(Minecraft minecraft) {
		return false;
	}

	public static void tell(Minecraft minecraft, Runnable task) {
		minecraft.addScheduledTask(task);
	}

	public static java.util.concurrent.CompletableFuture<Void> submit(Minecraft minecraft, Runnable task) {
		var future = new java.util.concurrent.CompletableFuture<Void>();
		minecraft.addScheduledTask(() -> {
			try {
				task.run();
				future.complete(null);
			} catch (Throwable t) {
				future.completeExceptionally(t);
			}
		});
		return future;
	}

	public static <T> java.util.concurrent.CompletableFuture<T> submit(Minecraft minecraft, java.util.function.Supplier<T> task) {
		var future = new java.util.concurrent.CompletableFuture<T>();
		minecraft.addScheduledTask(() -> {
			try {
				future.complete(task.get());
			} catch (Throwable t) {
				future.completeExceptionally(t);
			}
		});
		return future;
	}

	public static io.github.xponer2.translator.client.RenderBuffers renderBuffers(Minecraft minecraft) {
		return io.github.xponer2.translator.client.RenderBuffers.INSTANCE;
	}

	public static io.github.xponer2.translator.client.DeltaTracker getTimer(Minecraft minecraft) {
		return io.github.xponer2.translator.client.DeltaTracker.INSTANCE;
	}

	/** The 1.21 field {@code mouseHandler}. */
	public static io.github.xponer2.translator.client.MouseHandler mouseHandler(Minecraft minecraft) {
		return io.github.xponer2.translator.client.MouseHandler.INSTANCE;
	}

	/** A sprite of an atlas by its 1.21 name (block/stone is blocks/stone on 1.12.2). */
	public static java.util.function.Function<net.minecraft.util.ResourceLocation, net.minecraft.client.renderer.texture.TextureAtlasSprite> getTextureAtlas(
			Minecraft minecraft, net.minecraft.util.ResourceLocation atlas) {
		return sprite -> {
			String path = sprite.getPath().startsWith("block/") ? "blocks/" + sprite.getPath().substring(6)
					: sprite.getPath().startsWith("item/") ? "items/" + sprite.getPath().substring(5) : sprite.getPath();
			return minecraft.getTextureMapBlocks().getAtlasSprite(sprite.getNamespace() + ":" + path);
		};
	}

	public static net.minecraft.client.renderer.block.model.ModelManager getModelManager(Minecraft minecraft) {
		return minecraft.getBlockRendererDispatcher().getBlockModelShapes().getModelManager();
	}

	public static net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher getBlockEntityRenderDispatcher(Minecraft minecraft) {
		return net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher.instance;
	}

	public static boolean useShaderTransparency(Minecraft minecraft) {
		return false;
	}

	/** Minecraft.useShaderTransparency() is static in 1.21 (fabulous graphics): never on 1.12.2. */
	public static boolean useShaderTransparency() {
		return false;
	}

	public static boolean isSameThread(Minecraft minecraft) {
		return minecraft.isCallingFromMinecraftThread();
	}

	public static void execute(Minecraft minecraft, Runnable task) {
		minecraft.addScheduledTask(task);
	}

	// --- sounds (SoundInstance is ISound, SimpleSoundInstance is PositionedSoundRecord) ---------------------------------

	public static void play(net.minecraft.client.audio.SoundHandler sounds, net.minecraft.client.audio.ISound sound) {
		sounds.playSound(sound);
	}

	public static void playDelayed(net.minecraft.client.audio.SoundHandler sounds, net.minecraft.client.audio.ISound sound, int delay) {
		sounds.playDelayedSound(sound, delay);
	}

	public static void queueTickingSound(net.minecraft.client.audio.SoundHandler sounds, net.minecraft.client.audio.ITickableSound sound) {
		sounds.playSound(sound);
	}

	public static void stop(net.minecraft.client.audio.SoundHandler sounds, net.minecraft.client.audio.ISound sound) {
		sounds.stopSound(sound);
	}

	public static void stop(net.minecraft.client.audio.SoundHandler sounds, @javax.annotation.Nullable net.minecraft.util.ResourceLocation sound,
			@javax.annotation.Nullable net.minecraft.util.SoundCategory category) {
		sounds.stop(sound == null ? "" : sound.toString(), category);
	}

	public static boolean isActive(net.minecraft.client.audio.SoundHandler sounds, net.minecraft.client.audio.ISound sound) {
		return sounds.isSoundPlaying(sound);
	}

	/** 1.21 {@code SimpleSoundInstance.forUI(holder, pitch)}: a sound of the interface. */
	public static net.minecraft.client.audio.PositionedSoundRecord forUI(io.github.xponer2.translator.registry.Holder<net.minecraft.util.SoundEvent> sound,
			float pitch) {
		return net.minecraft.client.audio.PositionedSoundRecord.getMasterRecord(sound.value(), pitch);
	}

	public static net.minecraft.client.audio.PositionedSoundRecord forUI(io.github.xponer2.translator.registry.Holder<net.minecraft.util.SoundEvent> sound,
			float pitch, float volume) {
		return net.minecraft.client.audio.PositionedSoundRecord.getRecord(sound.value(), pitch, volume);
	}

	/** 1.21 {@code new SimpleSoundInstance(sound, source, volume, pitch, random, pos)}. */
	public static net.minecraft.client.audio.PositionedSoundRecord newPositionedSoundRecord(net.minecraft.util.SoundEvent sound,
			net.minecraft.util.SoundCategory category, float volume, float pitch, java.util.Random random, net.minecraft.util.math.BlockPos pos) {
		return new net.minecraft.client.audio.PositionedSoundRecord(sound, category, volume, pitch, pos);
	}

	// --- TextureAtlasSprite (SpriteContents is the sprite, neoforge-1.21.1-runtime.tsv) ---------------------------------

	public static net.minecraft.client.renderer.texture.TextureAtlasSprite contents(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
		return sprite;
	}

	public static int width(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
		return sprite.getIconWidth();
	}

	public static int height(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
		return sprite.getIconHeight();
	}

	public static net.minecraft.util.ResourceLocation name(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
		return new net.minecraft.util.ResourceLocation(sprite.getIconName());
	}

	/** A 1.21 texture coordinate: a fraction (0 to 1) of the sprite, 1.12.2 interpolates sixteenths. */
	public static float getU(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, float fraction) {
		return sprite.getInterpolatedU(fraction * 16.0);
	}

	public static float getV(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, float fraction) {
		return sprite.getInterpolatedV(fraction * 16.0);
	}

	public static float getU0(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
		return sprite.getMinU();
	}

	public static float getU1(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
		return sprite.getMaxU();
	}

	public static float getV0(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
		return sprite.getMinV();
	}

	public static float getV1(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
		return sprite.getMaxV();
	}

	public static net.minecraft.util.ResourceLocation atlasLocation(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
		// a GUI sprite is a texture of its own (GuiSpriteManager)
		return sprite instanceof io.github.xponer2.translator.client.GuiSprite gui ? gui.texture()
				: net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE;
	}

	public static int getX(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
		return sprite.getOriginX();
	}

	public static int getY(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
		return sprite.getOriginY();
	}

	public static float uvShrinkRatio(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
		return 0.0F;
	}

	public static io.github.xponer2.translator.client.VertexConsumer wrap(net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
			io.github.xponer2.translator.client.VertexConsumer consumer) {
		return consumer;
	}

	// --- IBakedModel -------------------------------------------------------------------------------------------------

	public static List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(net.minecraft.client.renderer.block.model.IBakedModel model,
			@javax.annotation.Nullable net.minecraft.block.state.IBlockState state, @javax.annotation.Nullable net.minecraft.util.EnumFacing side,
			java.util.Random random) {
		if (model instanceof io.github.xponer2.translator.client.model.BakedModelBridge bridge) {
			return bridge.getQuads(state, side, random == null ? new java.util.Random(0L) : random);
		}
		return model.getQuads(state, side, random == null ? 0L : random.nextLong());
	}

	public static List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(net.minecraft.client.renderer.block.model.IBakedModel model,
			@javax.annotation.Nullable net.minecraft.block.state.IBlockState state, @javax.annotation.Nullable net.minecraft.util.EnumFacing side,
			java.util.Random random, io.github.xponer2.translator.client.ModelData data,
			@javax.annotation.Nullable io.github.xponer2.translator.client.RenderType type) {
		if (model instanceof io.github.xponer2.translator.client.model.BakedModelBridge bridge) {
			// a mod's model (a backpack's parts): with the data it asked for
			return bridge.getQuads(state, side, random == null ? new java.util.Random(0L) : random,
					data == null ? io.github.xponer2.translator.client.ModelData.EMPTY : data, type);
		}
		return getQuads(model, state, side, random);
	}

	public static io.github.xponer2.translator.client.ModelData getModelData(net.minecraft.client.renderer.block.model.IBakedModel model,
			net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos, net.minecraft.block.state.IBlockState state,
			io.github.xponer2.translator.client.ModelData data) {
		return data;
	}

	/** The model for a display context, its transformation applied to the GL matrix (the PoseStack is it). */
	public static net.minecraft.client.renderer.block.model.IBakedModel applyTransform(net.minecraft.client.renderer.block.model.IBakedModel model,
			net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType type, io.github.xponer2.translator.client.PoseStack stack,
			boolean leftHand) {
		var perspective = model.handlePerspective(type);
		if (perspective.getRight() != null) {
			net.minecraftforge.client.ForgeHooksClient.multiplyCurrentGlMatrix(perspective.getRight());
		}
		return perspective.getLeft();
	}

	public static boolean usesBlockLight(net.minecraft.client.renderer.block.model.IBakedModel model) {
		return model.isGui3d();
	}

	public static net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon(net.minecraft.client.renderer.block.model.IBakedModel model,
			io.github.xponer2.translator.client.ModelData data) {
		return model.getParticleTexture();
	}

	public static net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon(net.minecraft.client.renderer.block.model.IBakedModel model) {
		return model.getParticleTexture();
	}

	public static List<net.minecraft.client.renderer.block.model.IBakedModel> getRenderPasses(net.minecraft.client.renderer.block.model.IBakedModel model,
			net.minecraft.item.ItemStack stack, boolean fabulous) {
		return java.util.Collections.singletonList(model);
	}

	public static List<io.github.xponer2.translator.client.RenderType> getRenderTypes(net.minecraft.client.renderer.block.model.IBakedModel model,
			net.minecraft.item.ItemStack stack, boolean fabulous) {
		return java.util.Collections.singletonList(io.github.xponer2.translator.client.RenderType.cutout());
	}

	public static io.github.xponer2.translator.runtime.neoforge.TriState useAmbientOcclusion(net.minecraft.client.renderer.block.model.IBakedModel model,
			net.minecraft.block.state.IBlockState state, io.github.xponer2.translator.client.ModelData data,
			io.github.xponer2.translator.client.RenderType type) {
		return model.isAmbientOcclusion(state) ? io.github.xponer2.translator.runtime.neoforge.TriState.TRUE
				: io.github.xponer2.translator.runtime.neoforge.TriState.FALSE;
	}

	// --- Font (FormattedText and FormattedCharSequence are ITextComponent, neoforge-1.21.1-runtime.tsv) ----------------

	/** 1.21 batched text, drawn at once: the pose is the GL matrix (RenderBridges), display mode and light are the GL state. */
	public static int drawInBatch(FontRenderer font, ITextComponent text, float x, float y, int color, boolean shadow, org.joml.Matrix4f pose,
			io.github.xponer2.translator.client.MultiBufferSource buffers, Object mode, int background, int light) {
		return font.drawString(formatted(text), x, y, color, shadow);
	}

	public static int drawInBatch(FontRenderer font, String text, float x, float y, int color, boolean shadow, org.joml.Matrix4f pose,
			io.github.xponer2.translator.client.MultiBufferSource buffers, Object mode, int background, int light) {
		return font.drawString(text, x, y, color, shadow);
	}

	public static int drawInBatch(FontRenderer font, String text, float x, float y, int color, boolean shadow, org.joml.Matrix4f pose,
			io.github.xponer2.translator.client.MultiBufferSource buffers, Object mode, int background, int light, boolean bidirectional) {
		return font.drawString(text, x, y, color, shadow);
	}

	public static float renderText(FontRenderer font, String text, float x, float y, int color, boolean shadow, org.joml.Matrix4f pose,
			io.github.xponer2.translator.client.MultiBufferSource buffers, Object mode, int background, int light) {
		return font.drawString(text, x, y, color, shadow);
	}

	/** Text with an outline (glowing signs): the outline color drawn around it, then the text. */
	public static void drawInBatch8xOutline(FontRenderer font, ITextComponent text, float x, float y, int color, int outline, org.joml.Matrix4f pose,
			io.github.xponer2.translator.client.MultiBufferSource buffers, int light) {
		String plain = net.minecraft.util.text.TextFormatting.getTextWithoutFormattingCodes(formatted(text));
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				if (dx != 0 || dy != 0) {
					font.drawString(plain, x + dx, y + dy, outline, false);
				}
			}
		}
		font.drawString(formatted(text), x, y, color, false);
	}

	public static ITextComponent substrByWidth(FontRenderer font, ITextComponent text, int width) {
		return new TextComponentString(font.trimStringToWidth(formatted(text), width));
	}

	public static String plainSubstrByWidth(FontRenderer font, String text, int width) {
		return font.trimStringToWidth(text, width);
	}

	public static String plainSubstrByWidth(FontRenderer font, String text, int width, boolean tail) {
		return font.trimStringToWidth(text, width, tail);
	}

	public static ITextComponent ellipsize(FontRenderer font, ITextComponent text, int width) {
		String formatted = formatted(text);
		if (font.getStringWidth(formatted) <= width) {
			return text;
		}
		return new TextComponentString(font.trimStringToWidth(formatted, width - font.getStringWidth("...")) + "...");
	}

	public static int wordWrapHeight(FontRenderer font, ITextComponent text, int width) {
		return font.getWordWrappedHeight(formatted(text), width);
	}

	public static int wordWrapHeight(FontRenderer font, String text, int width) {
		return font.getWordWrappedHeight(text, width);
	}

	public static boolean isBidirectional(FontRenderer font) {
		return font.getBidiFlag();
	}

	public static int width(FontRenderer font, ITextComponent text) {
		return font.getStringWidth(formatted(text));
	}

	/** The lines of a text at most that wide, each as a component with the text's formatting codes. */
	public static List<ITextComponent> split(FontRenderer font, ITextComponent text, int width) {
		var lines = new ArrayList<ITextComponent>();
		for (String line : font.listFormattedStringToWidth(formatted(text), width)) {
			lines.add(new TextComponentString(line));
		}
		return lines;
	}

	/** A text's formatted string; "" for none (a mod's label it couldn't make, AE2's screen styles): a screen draws on. */
	private static String formatted(@javax.annotation.Nullable ITextComponent text) {
		return text == null ? "" : text.getFormattedText();
	}
}
