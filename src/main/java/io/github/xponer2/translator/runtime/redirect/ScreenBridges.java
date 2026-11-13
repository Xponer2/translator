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

import io.github.xponer2.translator.client.GuiGraphics;
import io.github.xponer2.translator.client.MultiBufferSource;
import io.github.xponer2.translator.client.PoseStack;
import io.github.xponer2.translator.runtime.neoforge.KeyMapping;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.color.BlockColors;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketChatMessage;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Static stand-ins for 1.21 client methods of screens, item rendering, textures, block colors, key bindings, the
 * client world and its connection that the 1.12.2 classes lack (runtime, neoforge-1.21.1-redirects.tsv).
 */
@SideOnly(Side.CLIENT)
public final class ScreenBridges {
	private ScreenBridges() {
	}

	// --- small client members ----------------------------------------------------------------------------------------

	public static int byteSize(net.minecraft.client.renderer.vertex.VertexFormatElement element) {
		return element.getSize();
	}

	public static String getSelected(net.minecraft.client.resources.LanguageManager languages) {
		return languages.getCurrentLanguage().getLanguageCode();
	}

	public static double getLastViewDistance(net.minecraft.client.renderer.RenderGlobal renderer) {
		return Minecraft.getMinecraft().gameSettings.renderDistanceChunks;
	}

	public static ItemStack getItemStack(net.minecraftforge.client.event.RenderTooltipEvent event) {
		return event.getStack();
	}

	/** The 1.21 GUI graphics of an overlay event: drawn at once, the 1.12.2 way. */
	public static GuiGraphics getGuiGraphics(net.minecraftforge.client.event.RenderGameOverlayEvent event) {
		return new GuiGraphics();
	}

	public static java.util.List<net.minecraftforge.client.settings.KeyModifier> getActiveModifiers() {
		var active = new java.util.ArrayList<net.minecraftforge.client.settings.KeyModifier>();
		for (var modifier : net.minecraftforge.client.settings.KeyModifier.values()) {
			if (modifier != net.minecraftforge.client.settings.KeyModifier.NONE && modifier.isActive(null)) {
				active.add(modifier);
			}
		}
		return active;
	}

	// --- RenderManager (EntityRenderDispatcher) ----------------------------------------------------------------------

	/** An entity drawn at a position relative to the camera (the pose is the GL matrix). */
	public static void render(net.minecraft.client.renderer.entity.RenderManager manager, Entity entity, double x, double y, double z, float yaw,
			float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
		MultiBufferSource.INSTANCE.endBatch();
		manager.renderEntity(entity, x, y, z, yaw, partialTick, false);
	}

	public static int getPackedLightCoords(net.minecraft.client.renderer.entity.RenderManager manager, Entity entity, float partialTick) {
		return entity.getBrightnessForRender();
	}

	public static double distanceToSqr(net.minecraft.client.renderer.entity.RenderManager manager, Entity entity) {
		return entity.getDistanceSq(manager.viewerPosX, manager.viewerPosY, manager.viewerPosZ);
	}

	public static net.minecraft.client.renderer.ItemRenderer getItemInHandRenderer(net.minecraft.client.renderer.entity.RenderManager manager) {
		return Minecraft.getMinecraft().getItemRenderer();
	}

	// --- GameSettings (Options): 1.21 options are OptionInstances of the 1.12.2 settings -------------------------------

	public static io.github.xponer2.translator.client.OptionInstance<Integer> renderDistance(net.minecraft.client.settings.GameSettings options) {
		return new io.github.xponer2.translator.client.OptionInstance<>(() -> options.renderDistanceChunks, value -> options.renderDistanceChunks = value);
	}

	public static io.github.xponer2.translator.client.OptionInstance<Integer> simulationDistance(net.minecraft.client.settings.GameSettings options) {
		return renderDistance(options);
	}

	public static io.github.xponer2.translator.client.OptionInstance<Integer> fov(net.minecraft.client.settings.GameSettings options) {
		return new io.github.xponer2.translator.client.OptionInstance<>(() -> (int) options.fovSetting, value -> options.fovSetting = value);
	}

	public static io.github.xponer2.translator.client.OptionInstance<Double> gamma(net.minecraft.client.settings.GameSettings options) {
		return new io.github.xponer2.translator.client.OptionInstance<>(() -> (double) options.gammaSetting, value -> options.gammaSetting = value.floatValue());
	}

	public static io.github.xponer2.translator.client.OptionInstance<Boolean> bobView(net.minecraft.client.settings.GameSettings options) {
		return new io.github.xponer2.translator.client.OptionInstance<>(() -> options.viewBobbing, value -> options.viewBobbing = value);
	}

	public static io.github.xponer2.translator.client.OptionInstance<Boolean> entityShadows(net.minecraft.client.settings.GameSettings options) {
		return new io.github.xponer2.translator.client.OptionInstance<>(() -> options.entityShadows, value -> options.entityShadows = value);
	}

	public static io.github.xponer2.translator.client.OptionInstance<Boolean> ambientOcclusion(net.minecraft.client.settings.GameSettings options) {
		return new io.github.xponer2.translator.client.OptionInstance<>(() -> options.ambientOcclusion != 0, value -> options.ambientOcclusion = value ? 2 : 0);
	}

	public static io.github.xponer2.translator.client.OptionInstance<Boolean> chatLinksPrompt(net.minecraft.client.settings.GameSettings options) {
		return new io.github.xponer2.translator.client.OptionInstance<>(() -> options.chatLinksPrompt, value -> options.chatLinksPrompt = value);
	}

	public static io.github.xponer2.translator.client.OptionInstance<Integer> biomeBlendRadius(net.minecraft.client.settings.GameSettings options) {
		return io.github.xponer2.translator.client.OptionInstance.fixed(0);
	}

	public static io.github.xponer2.translator.client.OptionInstance<Double> fovEffectScale(net.minecraft.client.settings.GameSettings options) {
		return io.github.xponer2.translator.client.OptionInstance.fixed(1.0D);
	}

	public static io.github.xponer2.translator.client.OptionInstance<Double> screenEffectScale(net.minecraft.client.settings.GameSettings options) {
		return io.github.xponer2.translator.client.OptionInstance.fixed(1.0D);
	}

	public static io.github.xponer2.translator.client.OptionInstance<Double> darknessEffectScale(net.minecraft.client.settings.GameSettings options) {
		return io.github.xponer2.translator.client.OptionInstance.fixed(1.0D);
	}

	public static io.github.xponer2.translator.client.OptionInstance<Double> damageTiltStrength(net.minecraft.client.settings.GameSettings options) {
		return io.github.xponer2.translator.client.OptionInstance.fixed(1.0D);
	}

	public static io.github.xponer2.translator.client.OptionInstance<Double> glintSpeed(net.minecraft.client.settings.GameSettings options) {
		return io.github.xponer2.translator.client.OptionInstance.fixed(0.5D);
	}

	public static io.github.xponer2.translator.client.OptionInstance<Double> glintStrength(net.minecraft.client.settings.GameSettings options) {
		return io.github.xponer2.translator.client.OptionInstance.fixed(0.75D);
	}

	public static io.github.xponer2.translator.client.OptionInstance<Double> textBackgroundOpacity(net.minecraft.client.settings.GameSettings options) {
		return io.github.xponer2.translator.client.OptionInstance.fixed(0.5D);
	}

	public static io.github.xponer2.translator.client.OptionInstance<Boolean> backgroundForChatOnly(net.minecraft.client.settings.GameSettings options) {
		return io.github.xponer2.translator.client.OptionInstance.fixed(true);
	}

	public static io.github.xponer2.translator.client.OptionInstance<Boolean> hideLightningFlash(net.minecraft.client.settings.GameSettings options) {
		return io.github.xponer2.translator.client.OptionInstance.fixed(false);
	}

	public static io.github.xponer2.translator.client.OptionInstance<Boolean> highContrast(net.minecraft.client.settings.GameSettings options) {
		return io.github.xponer2.translator.client.OptionInstance.fixed(false);
	}

	public static int getEffectiveRenderDistance(net.minecraft.client.settings.GameSettings options) {
		return options.renderDistanceChunks;
	}

	public static float getBackgroundOpacity(net.minecraft.client.settings.GameSettings options, float fallback) {
		return fallback;
	}

	public static boolean isModelPartEnabled(net.minecraft.client.settings.GameSettings options, net.minecraft.entity.player.EnumPlayerModelParts part) {
		return options.getModelParts().contains(part);
	}

	/** The camera (1.21 CameraType, carried with the API) of the 1.12.2 third person view setting. */
	public static Object getCameraType(net.minecraft.client.settings.GameSettings options) {
		String name = options.thirdPersonView == 1 ? "THIRD_PERSON_BACK" : options.thirdPersonView == 2 ? "THIRD_PERSON_FRONT" : "FIRST_PERSON";
		try {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Object type = Enum.valueOf((Class) Class.forName("net.minecraft.client.CameraType", true, net.minecraft.launchwrapper.Launch.classLoader), name);
			return type;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	// --- debug screen, atlas, single blocks, client recipes ----------------------------------------------------------

	/** The debug overlay (F3) is part of the 1.12.2 in-game GUI: shown when the setting is on. */
	@javax.annotation.Nullable
	public static net.minecraft.client.gui.GuiOverlayDebug getDebugOverlay(Minecraft minecraft) {
		return null;
	}

	public static boolean showDebugScreen(@javax.annotation.Nullable net.minecraft.client.gui.GuiOverlayDebug overlay) {
		return Minecraft.getMinecraft().gameSettings.showDebugInfo;
	}

	public static boolean showProfilerChart(@javax.annotation.Nullable net.minecraft.client.gui.GuiOverlayDebug overlay) {
		return Minecraft.getMinecraft().gameSettings.showDebugProfilerChart;
	}

	/** A sprite of the blocks atlas by its 1.21 name (block/stone is blocks/stone on 1.12.2). */
	public static net.minecraft.client.renderer.texture.TextureAtlasSprite getSprite(net.minecraft.client.renderer.texture.TextureMap atlas,
			ResourceLocation sprite) {
		String path = sprite.getPath().startsWith("block/") ? "blocks/" + sprite.getPath().substring(6)
				: sprite.getPath().startsWith("item/") ? "items/" + sprite.getPath().substring(5) : sprite.getPath();
		return atlas.getAtlasSprite(sprite.getNamespace() + ":" + path);
	}

	public static ResourceLocation location(net.minecraft.client.renderer.texture.TextureMap atlas) {
		return net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE;
	}

	/** A block state drawn at the GL matrix's origin (1.21 renderSingleBlock): the 1.12.2 block renderer. */
	public static void renderSingleBlock(net.minecraft.client.renderer.BlockRendererDispatcher blocks, IBlockState state, PoseStack pose,
			MultiBufferSource buffers, int light, int overlay) {
		MultiBufferSource.INSTANCE.endBatch();
		Minecraft.getMinecraft().getTextureManager().bindTexture(net.minecraft.client.renderer.texture.TextureMap.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.pushMatrix();
		blocks.renderBlockBrightness(state, 1.0F);
		GlStateManager.popMatrix();
	}

	public static void renderSingleBlock(net.minecraft.client.renderer.BlockRendererDispatcher blocks, IBlockState state, PoseStack pose,
			MultiBufferSource buffers, int light, int overlay, io.github.xponer2.translator.client.ModelData data,
			@javax.annotation.Nullable io.github.xponer2.translator.client.RenderType type) {
		renderSingleBlock(blocks, state, pose, buffers, light, overlay);
	}

	public static io.github.xponer2.translator.recipe.RecipeManager getRecipeManager(NetHandlerPlayClient connection) {
		return ApiBridges.getRecipeManager(Minecraft.getMinecraft().world);
	}

	// --- GuiScreen ---------------------------------------------------------------------------------------------------

	public static void init(GuiScreen screen, Minecraft minecraft, int width, int height) {
		screen.setWorldAndResolution(minecraft, width, height);
	}

	public static void resize(GuiScreen screen, Minecraft minecraft, int width, int height) {
		screen.onResize(minecraft, width, height);
	}

	public static void render(GuiScreen screen, GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
		screen.drawScreen(mouseX, mouseY, partialTick);
	}

	public static void onClose(GuiScreen screen) {
		Minecraft.getMinecraft().displayGuiScreen(null);
	}

	public static boolean isPauseScreen(GuiScreen screen) {
		return screen.doesGuiPauseGame();
	}

	public static Minecraft getMinecraft(GuiScreen screen) {
		return screen.mc != null ? screen.mc : Minecraft.getMinecraft();
	}

	/** The 1.21 field {@code font}. */
	public static FontRenderer font(GuiScreen screen) {
		return Minecraft.getMinecraft().fontRenderer;
	}

	public static ITextComponent getTitle(GuiScreen screen) {
		return new TextComponentString("");
	}

	// --- RenderItem (ItemRenderer) -----------------------------------------------------------------------------------

	public static IBakedModel getModel(RenderItem items, ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity, int seed) {
		return items.getItemModelWithOverrides(stack, world, entity);
	}

	/** An item drawn at the GL matrix's origin, centered, for a display context (1.21 renderStatic). */
	public static void renderStatic(RenderItem items, ItemStack stack, ItemCameraTransforms.TransformType type, int light, int overlay, PoseStack pose,
			MultiBufferSource buffers, @Nullable World world, int seed) {
		if (stack.isEmpty()) {
			return;
		}
		MultiBufferSource.INSTANCE.endBatch();
		GlStateManager.pushMatrix();
		RenderHelper.enableStandardItemLighting();
		items.renderItem(stack, type);
		RenderHelper.disableStandardItemLighting();
		GlStateManager.popMatrix();
	}

	public static void renderStatic(RenderItem items, @Nullable EntityLivingBase entity, ItemStack stack, ItemCameraTransforms.TransformType type,
			boolean leftHand, PoseStack pose, MultiBufferSource buffers, @Nullable World world, int light, int overlay, int seed) {
		if (stack.isEmpty()) {
			return;
		}
		MultiBufferSource.INSTANCE.endBatch();
		GlStateManager.pushMatrix();
		if (entity != null) {
			items.renderItem(stack, entity, type, leftHand);
		} else {
			items.renderItem(stack, type);
		}
		GlStateManager.popMatrix();
	}

	/** A model of an item drawn for a display context (its transformation applied first). */
	public static void render(RenderItem items, ItemStack stack, ItemCameraTransforms.TransformType type, boolean leftHand, PoseStack pose,
			MultiBufferSource buffers, int light, int overlay, IBakedModel model) {
		if (stack.isEmpty()) {
			return;
		}
		MultiBufferSource.INSTANCE.endBatch();
		Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		GlStateManager.pushMatrix();
		GlStateManager.enableRescaleNormal();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		IBakedModel transformed = ForgeHooksClient.handleCameraTransforms(model, type, leftHand);
		items.renderItem(stack, transformed);
		GlStateManager.disableRescaleNormal();
		GlStateManager.disableBlend();
		GlStateManager.popMatrix();
	}

	// --- TextureManager ----------------------------------------------------------------------------------------------

	@Nullable
	public static AbstractTexture getTexture(TextureManager textures, ResourceLocation location) {
		var texture = textures.getTexture(location);
		if (texture == null) {
			var loaded = new net.minecraft.client.renderer.texture.SimpleTexture(location);
			textures.loadTexture(location, loaded);
			texture = textures.getTexture(location);
		}
		return texture instanceof AbstractTexture abstractTexture ? abstractTexture : null;
	}

	public static AbstractTexture getTexture(TextureManager textures, ResourceLocation location, AbstractTexture fallback) {
		var texture = textures.getTexture(location);
		return texture instanceof AbstractTexture abstractTexture ? abstractTexture : fallback;
	}

	public static void register(TextureManager textures, ResourceLocation location, AbstractTexture texture) {
		textures.loadTexture(location, texture);
	}

	public static void release(TextureManager textures, ResourceLocation location) {
		textures.deleteTexture(location);
	}

	// --- BlockColors -------------------------------------------------------------------------------------------------

	public static int getColor(BlockColors colors, IBlockState state, @Nullable IBlockAccess world, @Nullable BlockPos pos, int tint) {
		return colors.colorMultiplier(state, world, pos, tint);
	}

	// --- TextureMap (1.21 TextureAtlas) ------------------------------------------------------------------------------

	/** 1.21 {@code atlas.getWidth()} / {@code getHeight()}: the size of its GL texture (AE2's blitter maps its sprites' UVs with it). */
	public static int getWidth(net.minecraft.client.renderer.texture.TextureMap atlas) {
		return atlasLevelParameter(atlas, org.lwjgl.opengl.GL11.GL_TEXTURE_WIDTH);
	}

	public static int getHeight(net.minecraft.client.renderer.texture.TextureMap atlas) {
		return atlasLevelParameter(atlas, org.lwjgl.opengl.GL11.GL_TEXTURE_HEIGHT);
	}

	private static int atlasLevelParameter(net.minecraft.client.renderer.texture.TextureMap atlas, int parameter) {
		int bound = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D);
		net.minecraft.client.renderer.GlStateManager.bindTexture(atlas.getGlTextureId());
		int value = net.minecraft.client.renderer.GlStateManager.glGetTexLevelParameteri(org.lwjgl.opengl.GL11.GL_TEXTURE_2D, 0, parameter);
		net.minecraft.client.renderer.GlStateManager.bindTexture(bound);
		return value;
	}

	// --- KeyBinding --------------------------------------------------------------------------------------------------

	public static ITextComponent getTranslatedKeyMessage(KeyBinding key) {
		return new TextComponentString(key.getDisplayName());
	}

	/** A key event (GLFW key code, see Screen) for this binding. */
	public static boolean matches(KeyBinding key, int keyCode, int scanCode) {
		int lwjgl = KeyMapping.toLwjgl(keyCode);
		return lwjgl != 0 && key.getKeyCode() == lwjgl;
	}

	public static boolean matchesMouse(KeyBinding key, int button) {
		return key.getKeyCode() == button - 100;
	}

	public static boolean isUnbound(KeyBinding key) {
		return key.getKeyCode() == 0;
	}

	public static void setDown(KeyBinding key, boolean down) {
		KeyBinding.setKeyBindState(key.getKeyCode(), down);
	}

	public static boolean isConflictContextAndModifierActive(KeyBinding key) {
		return key.getKeyConflictContext().isActive() && key.getKeyModifier().isActive(key.getKeyConflictContext());
	}

	/**
	 * 1.21 {@code keyMapping.key}: the bound key (1.12.2 keeps a key code; the key binding screens of Controlling,
	 * Cloth Config and Jade read it). Both names: the table maps the field to 1.12.2's keyCode.
	 */
	public static io.github.xponer2.translator.runtime.neoforge.InputConstants.Key key(KeyBinding key) {
		return io.github.xponer2.translator.runtime.neoforge.InputConstants.fromLwjgl(key.getKeyCode());
	}

	public static io.github.xponer2.translator.runtime.neoforge.InputConstants.Key keyCode(KeyBinding key) {
		return key(key);
	}

	/** 1.21 {@code keyMapping.key = input}. */
	public static void key(KeyBinding key, io.github.xponer2.translator.runtime.neoforge.InputConstants.Key input) {
		key.setKeyCode(input.lwjgl());
	}

	public static void keyCode(KeyBinding key, io.github.xponer2.translator.runtime.neoforge.InputConstants.Key input) {
		key(key, input);
	}

	/** 1.21 {@code setKey(input)}: bound to that key (the screens then reset the bindings' map and save). */
	public static void setKey(KeyBinding key, io.github.xponer2.translator.runtime.neoforge.InputConstants.Key input) {
		key.setKeyCode(input.lwjgl());
	}

	/** NeoForge {@code setKeyModifierAndCode(modifier, input)}. */
	public static void setKeyModifierAndCode(KeyBinding key, net.minecraftforge.client.settings.KeyModifier modifier,
			io.github.xponer2.translator.runtime.neoforge.InputConstants.Key input) {
		key.setKeyModifierAndCode(modifier, input.lwjgl());
	}

	/** 1.21 {@code isDefault()}: bound to its default key (and modifier). */
	public static boolean isDefault(KeyBinding key) {
		return key.isSetToDefaultValue();
	}

	/** 1.21 {@code same(other)}: bound to the same key. */
	public static boolean same(KeyBinding key, KeyBinding other) {
		return key.getKeyCode() == other.getKeyCode();
	}

	/** NeoForge {@code hasKeyModifierConflict(other)}. */
	public static boolean hasKeyModifierConflict(KeyBinding key, KeyBinding other) {
		return key.hasKeyCodeModifierConflict(other);
	}

	/** 1.21 {@code options.setKey(key, input)}: bound, the bindings' map made again and the options saved. */
	public static void setKey(net.minecraft.client.settings.GameSettings options, KeyBinding key,
			io.github.xponer2.translator.runtime.neoforge.InputConstants.Key input) {
		options.setOptionKeyBinding(key, input.lwjgl());
	}

	/** NeoForge {@code KeyModifier.isKeyCodeModifier(input)}: a control, shift or alt key. */
	public static boolean isKeyCodeModifier(io.github.xponer2.translator.runtime.neoforge.InputConstants.Key input) {
		return net.minecraftforge.client.settings.KeyModifier.isKeyCodeModifier(input.lwjgl());
	}

	/**
	 * 1.21 {@code itemRenderer.renderQuadList(pose, consumer, quads, stack, light, overlay)}: the quads tinted by the
	 * item's colors (Mekanism's, Create's and Twilight Forest's item renderers).
	 */
	public static void renderQuadList(net.minecraft.client.renderer.RenderItem renderer, Object pose, io.github.xponer2.translator.client.VertexConsumer consumer,
			java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> quads, ItemStack stack, int light, int overlay) {
		var colors = Minecraft.getMinecraft().getItemColors();
		for (var quad : quads) {
			int color = !stack.isEmpty() && quad.hasTintIndex() ? colors.colorMultiplier(stack, quad.getTintIndex()) : -1;
			consumer.putBulkData(pose, quad, (color >> 16 & 0xFF) / 255.0F, (color >> 8 & 0xFF) / 255.0F, (color & 0xFF) / 255.0F, 1.0F, light, overlay);
		}
	}

	/** NeoForge {@code getDisplayName()}: the key with its modifier, as a component (1.12.2's gives the text). */
	public static ITextComponent getDisplayName(KeyBinding key) {
		return new TextComponentString(key.getDisplayName());
	}

	// --- NetHandlerPlayClient (ClientPacketListener) -----------------------------------------------------------------

	public static void sendCommand(NetHandlerPlayClient connection, String command) {
		connection.sendPacket(new CPacketChatMessage(command.startsWith("/") ? command : "/" + command));
	}

	public static boolean sendUnsignedCommand(NetHandlerPlayClient connection, String command) {
		sendCommand(connection, command);
		return true;
	}

	public static void sendChat(NetHandlerPlayClient connection, String message) {
		connection.sendPacket(new CPacketChatMessage(message));
	}

	public static net.minecraft.network.NetworkManager getConnection(NetHandlerPlayClient connection) {
		return connection.getNetworkManager();
	}

	public static io.github.xponer2.translator.registry.RegistryAccess registryAccess(NetHandlerPlayClient connection) {
		return io.github.xponer2.translator.registry.RegistryAccess.INSTANCE;
	}

	public static void send(NetHandlerPlayClient connection, io.github.xponer2.translator.network.CustomPacketPayload payload) {
		io.github.xponer2.translator.network.PacketDistributor.sendToServer(payload);
	}

	// --- WorldClient (ClientLevel) -----------------------------------------------------------------------------------

	public static Iterable<Entity> entitiesForRendering(WorldClient world) {
		return world.loadedEntityList;
	}

	public static float getSkyDarken(WorldClient world, float partialTick) {
		return world.getSunBrightness(partialTick);
	}

	public static Vec3d getSkyColor(WorldClient world, Vec3d position, float partialTick) {
		Entity viewer = Minecraft.getMinecraft().getRenderViewEntity();
		return viewer == null ? new Vec3d(0.5, 0.7, 1.0) : world.getSkyColor(viewer, partialTick);
	}

	public static BlockPos getSharedSpawnPos(World world) {
		return world.getSpawnPoint();
	}

	public static void addEntity(WorldClient world, Entity entity) {
		world.addEntityToWorld(entity.getEntityId(), entity);
	}

	public static int getSkyFlashTime(WorldClient world) {
		return world.getLastLightningBolt();
	}

	// --- Camera ------------------------------------------------------------------------------------------------------

	/** 1.21 {@code GameRenderer.getMainCamera()}. */
	public static io.github.xponer2.translator.client.Camera getMainCamera(net.minecraft.client.renderer.EntityRenderer renderer) {
		return io.github.xponer2.translator.client.Camera.main();
	}

	/** 1.21 {@code EntityRenderDispatcher.camera}. */
	public static io.github.xponer2.translator.client.Camera camera(net.minecraft.client.renderer.entity.RenderManager manager) {
		return io.github.xponer2.translator.client.Camera.main();
	}

	/**
	 * 1.21 {@code blockRenderDispatcher.blockColors} (a field mods widen): the game's block colors (Twilight Forest's
	 * jar renderer, Supplementaries' candle holders tint their blocks with them).
	 */
	public static net.minecraft.client.renderer.color.BlockColors blockColors(net.minecraft.client.renderer.BlockRendererDispatcher dispatcher) {
		return net.minecraft.client.Minecraft.getMinecraft().getBlockColors();
	}
}
