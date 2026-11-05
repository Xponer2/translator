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
package io.github.xponer2.translator.selftest;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.Nullable;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The client half of the self-test (single player): the open screen and clicks in it, the models and their faces,
 * items used on the client.
 */
@SideOnly(Side.CLIENT)
final class SelfTestClient {
	private static final Method MOUSE_CLICKED = ObfuscationReflectionHelper.findMethod(GuiScreen.class, "func_73864_a", void.class,
			int.class, int.class, int.class);
	private static final Method MOUSE_RELEASED = ObfuscationReflectionHelper.findMethod(GuiScreen.class, "func_146286_b", void.class,
			int.class, int.class, int.class);
	private static final Field GUI_LEFT = ObfuscationReflectionHelper.findField(GuiContainer.class, "field_147003_i");
	private static final Field GUI_TOP = ObfuscationReflectionHelper.findField(GuiContainer.class, "field_147009_r");
	private static final Field LAST_CLICK_TIME = ObfuscationReflectionHelper.findField(GuiContainer.class, "field_146997_J");

	private SelfTestClient() {
	}

	/** The class of the screen the client shows now (the server opened a menu a tick ago), null for none. */
	@Nullable
	static String currentScreen() {
		GuiScreen screen = Minecraft.getMinecraft().currentScreen;
		return screen == null ? null : screen.getClass().getSimpleName();
	}

	/** On the client thread: items and blocks whose model is the missing one (the purple and black cube). */
	static void checkModels(List<Block> blocks, List<Item> items) {
		Minecraft mc = Minecraft.getMinecraft();
		mc.addScheduledTask(() -> {
			SelfTest.Run run = SelfTest.running();
			if (run == null) {
				return;
			}
			IBakedModel missing = mc.getRenderItem().getItemModelMesher().getModelManager().getMissingModel();
			for (Item item : items) {
				try {
					IBakedModel model = mc.getRenderItem().getItemModelWithOverrides(new ItemStack(item), null, null);
					if (model == missing) {
						run.addClientProblem("item", String.valueOf(item.getRegistryName()), "item model: missing (purple and black)");
					} else if (model.getQuads(null, null, 0L).isEmpty() && !model.isBuiltInRenderer() && emptyAllSides(model)) {
						run.addClientProblem("item", String.valueOf(item.getRegistryName()), "item model: no faces (invisible)");
					}
				} catch (Throwable t) {
					run.addClientProblem("item", String.valueOf(item.getRegistryName()), "item model: " + SelfTest.describe(t));
				}
			}
			for (Block block : blocks) {
				try {
					IBakedModel model = mc.getBlockRendererDispatcher().getModelForState(block.getDefaultState());
					if (model == missing) {
						run.addClientProblem("block", String.valueOf(block.getRegistryName()), "block model: missing (purple and black)");
					}
				} catch (Throwable t) {
					run.addClientProblem("block", String.valueOf(block.getRegistryName()), "block model: " + SelfTest.describe(t));
				}
			}
		});
	}

	/**
	 * On the client thread: every slot of the open screen of that menu clicked with the left button as a player does,
	 * the screen's own click code included (a mod's screen that breaks on a click crashes a player's game): picked up and
	 * put back. Set when done, also when there was nothing to click (the screen closed, another one opened).
	 */
	static AtomicBoolean clickSlots(SelfTest.Run run, String kind, String id, int windowId) {
		AtomicBoolean done = new AtomicBoolean();
		Minecraft mc = Minecraft.getMinecraft();
		mc.addScheduledTask(() -> {
			try {
				if (!(mc.currentScreen instanceof GuiContainer screen) || screen.inventorySlots.windowId != windowId) {
					return;
				}
				int left = GUI_LEFT.getInt(screen);
				int top = GUI_TOP.getInt(screen);
				for (Slot slot : new ArrayList<>(screen.inventorySlots.inventorySlots)) {
					if (mc.currentScreen != screen) {
						break;
					}
					if (!slot.isEnabled()) {
						continue;
					}
					int x = left + slot.xPos + 8;
					int y = top + slot.yPos + 8;
					try {
						for (int click = 0; click < 2 && mc.currentScreen == screen; click++) {
							// two clicks, not a double click (that gathers every stack of the kind)
							LAST_CLICK_TIME.setLong(screen, 0L);
							MOUSE_CLICKED.invoke(screen, x, y, 0);
							MOUSE_RELEASED.invoke(screen, x, y, 0);
						}
					} catch (InvocationTargetException e) {
						run.addClientProblem(kind, id, "screen slot " + slot.slotNumber + " click: " + SelfTest.describe(e.getCause()));
					}
				}
			} catch (Throwable t) {
				run.addClientProblem(kind, id, "screen slot clicks: " + SelfTest.describe(t));
			} finally {
				done.set(true);
			}
		});
		return done;
	}

	/**
	 * Whether a click on the screen reaches the server: a test stack (put in the player's inventory by the server) is
	 * picked up through the screen's own click code, and after a moment the server's cursor must hold it too; then it
	 * is put back and both menus must match. A screen that clicks only on the client (Sophisticated's backpacks moved
	 * items there and nowhere else) is a problem of the item whose menu it is.
	 */
	static AtomicBoolean checkClickSync(SelfTest.Run run, String kind, String id, int windowId, net.minecraft.entity.player.EntityPlayerMP server) {
		AtomicBoolean done = new AtomicBoolean();
		Minecraft mc = Minecraft.getMinecraft();
		mc.addScheduledTask(() -> {
			try {
				if (!(mc.currentScreen instanceof GuiContainer screen) || screen.inventorySlots.windowId != windowId) {
					done.set(true);
					return;
				}
				Slot test = null;
				for (Slot slot : screen.inventorySlots.inventorySlots) {
					if (slot.inventory == mc.player.inventory && slot.isEnabled() && slot.getStack().getItem() == net.minecraft.init.Items.STICK) {
						test = slot;
						break;
					}
				}
				if (test == null) {
					done.set(true);
					return;
				}
				Slot clicked = test;
				int x = GUI_LEFT.getInt(screen) + clicked.xPos + 8;
				int y = GUI_TOP.getInt(screen) + clicked.yPos + 8;
				LAST_CLICK_TIME.setLong(screen, 0L);
				MOUSE_CLICKED.invoke(screen, x, y, 0);
				MOUSE_RELEASED.invoke(screen, x, y, 0);
				ItemStack client = mc.player.inventory.getItemStack().copy();
				// the server gets the packet on its own thread: looked at a moment later, on the client thread again
				Thread later = new Thread(() -> {
					try {
						Thread.sleep(500L);
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
					}
					mc.addScheduledTask(() -> {
						try {
							ItemStack held = server.inventory.getItemStack();
							// the server went on to the next item meanwhile (its menu closed, the cursor emptied): nothing to compare
							boolean sameMenu = server.openContainer != null && server.openContainer.windowId == windowId;
							if (sameMenu && !ItemStack.areItemStacksEqual(client, held)) {
								run.addClientProblem(kind, id, "menu click didn't reach the server: the client holds " + client + ", the server " + held);
							}
							if (mc.currentScreen == screen) {
								LAST_CLICK_TIME.setLong(screen, 0L);
								MOUSE_CLICKED.invoke(screen, x, y, 0);
								MOUSE_RELEASED.invoke(screen, x, y, 0);
							}
						} catch (Throwable t) {
							run.addClientProblem(kind, id, "menu click check: " + SelfTest.describe(t));
						} finally {
							done.set(true);
						}
					});
				}, "Translator self-test click check");
				later.setDaemon(true);
				later.start();
			} catch (Throwable t) {
				run.addClientProblem(kind, id, "menu click check: " + SelfTest.describe(t));
				done.set(true);
			}
		});
		return done;
	}

	/** Whether text draws after the HUD of the mods (a mod's overlay that leaves GL state wrong hides every text). */
	static final List<String> RENDER_PROBLEMS = java.util.Collections.synchronizedList(new ArrayList<>());

	static void checkTextOnce() {
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new TextCheck());
	}

	public static final class TextCheck {
		private int frames;

		@net.minecraftforge.fml.common.eventhandler.SubscribeEvent(priority = net.minecraftforge.fml.common.eventhandler.EventPriority.LOWEST)
		public void onOverlay(net.minecraftforge.client.event.RenderGameOverlayEvent.Post event) {
			if (event.getType() != net.minecraftforge.client.event.RenderGameOverlayEvent.ElementType.ALL || ++this.frames < 40) {
				return;
			}
			net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(this);
			Minecraft mc = Minecraft.getMinecraft();
			try {
				int scale = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
				mc.fontRenderer.drawStringWithShadow("TRANSLATOR TEXT CHECK", 2, 2, 0xFFFFFF);
				int width = mc.fontRenderer.getStringWidth("TRANSLATOR TEXT CHECK") * scale;
				int height = 8 * scale;
				var pixels = org.lwjgl.BufferUtils.createByteBuffer(width * height * 4);
				org.lwjgl.opengl.GL11.glReadPixels(2 * scale, mc.displayHeight - 2 * scale - height, width, height, org.lwjgl.opengl.GL11.GL_RGBA,
						org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE, pixels);
				int white = 0;
				for (int i = 0; i < width * height; i++) {
					int r = pixels.get(i * 4) & 0xFF;
					int g = pixels.get(i * 4 + 1) & 0xFF;
					int b = pixels.get(i * 4 + 2) & 0xFF;
					if (r > 230 && g > 230 && b > 230) {
						white++;
					}
				}
				if (white < 20) {
					RENDER_PROBLEMS.add("text drawn after the HUD isn't visible (" + white + " white pixels): a mod's overlay left the GL state wrong");
				}
				int error = org.lwjgl.opengl.GL11.glGetError();
				if (error != org.lwjgl.opengl.GL11.GL_NO_ERROR) {
					RENDER_PROBLEMS.add("GL error after the HUD: " + error);
				}
			} catch (Throwable t) {
				RENDER_PROBLEMS.add("text check: " + SelfTest.describe(t));
			}
		}
	}

	/**
	 * On the client thread: the faces of the block at {@code pos} as the client draws it with air around. A face toward
	 * air that the block hides ({@code shouldSideBeRendered}), and a solid model with faces toward one side of an axis
	 * but none toward the other (a fence post without its top) are problems.
	 */
	static AtomicBoolean checkFaces(SelfTest.Run run, String id, BlockPos pos, Block block) {
		AtomicBoolean done = new AtomicBoolean();
		Minecraft mc = Minecraft.getMinecraft();
		mc.addScheduledTask(() -> {
			try {
				WorldClient world = mc.world;
				IBlockState state = world == null ? null : world.getBlockState(pos);
				if (state == null || state.getBlock() != block) {
					return;
				}
				IBlockState actual = state.getActualState(world, pos);
				if (actual.getRenderType() != EnumBlockRenderType.MODEL) {
					return;
				}
				IBakedModel model = mc.getBlockRendererDispatcher().getModelForState(actual);
				IBlockState extended = block.getExtendedState(actual, world, pos);
				Set<EnumFacing> facing = EnumSet.noneOf(EnumFacing.class);
				float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
				float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
				boolean box = true;
				List<String> hidden = new ArrayList<>();
				Set<TextureAtlasSprite> sprites = new java.util.LinkedHashSet<>();
				for (int i = -1; i < EnumFacing.VALUES.length; i++) {
					EnumFacing side = i < 0 ? null : EnumFacing.VALUES[i];
					List<BakedQuad> quads = model.getQuads(extended, side, 0L);
					if (side != null && !quads.isEmpty() && world.isAirBlock(pos.offset(side)) && !actual.shouldSideBeRendered(world, pos, side)) {
						hidden.add(side.getName());
					}
					for (BakedQuad quad : quads) {
						box &= addQuad(quad, facing, min, max);
						sprites.add(quad.getSprite());
					}
				}
				if (!hidden.isEmpty()) {
					run.addClientProblem("block", id, "block faces: hidden next to air toward " + String.join(", ", hidden));
				}
				// drawn solid with a texture that has holes: the holes show as a white or black background
				if (block.getRenderLayer() == BlockRenderLayer.SOLID && block.canRenderInLayer(actual, BlockRenderLayer.SOLID)
						&& !block.canRenderInLayer(actual, BlockRenderLayer.CUTOUT) && !block.canRenderInLayer(actual, BlockRenderLayer.CUTOUT_MIPPED)) {
					for (TextureAtlasSprite sprite : sprites) {
						if (sprite != null && transparent(sprite)) {
							run.addClientProblem("block", id, "render layer: drawn solid, its texture " + sprite.getIconName()
									+ " has transparent pixels (seen as a background)");
							break;
						}
					}
				}
				if (box && !facing.isEmpty()) {
					List<String> missing = new ArrayList<>();
					for (EnumFacing side : EnumFacing.VALUES) {
						int axis = side.getAxis().ordinal();
						if (!facing.contains(side) && facing.contains(side.getOpposite()) && max[axis] - min[axis] > 1 / 16F) {
							missing.add(side.getName());
						}
					}
					if (!missing.isEmpty()) {
						run.addClientProblem("block", id, "block model: no faces toward " + String.join(", ", missing) + " (see-through)");
					}
				}
			} catch (Throwable t) {
				run.addClientProblem("block", id, "block faces: " + SelfTest.describe(t));
			} finally {
				done.set(true);
			}
		});
		return done;
	}

	/** On the client thread: whether an entity class has a renderer of its own (else it is drawn as a white box or not at all). */
	static AtomicBoolean checkEntityRenderer(SelfTest.Run run, String id, Class<? extends net.minecraft.entity.Entity> type) {
		AtomicBoolean done = new AtomicBoolean();
		Minecraft mc = Minecraft.getMinecraft();
		mc.addScheduledTask(() -> {
			try {
				var render = mc.getRenderManager().getEntityClassRenderObject(type);
				if (render == null || render.getClass() == net.minecraft.client.renderer.entity.RenderEntity.class) {
					run.addClientProblem("entity", id, "renderer: none (drawn as a white box or not at all)");
				}
			} catch (Throwable t) {
				run.addClientProblem("entity", id, "renderer: " + SelfTest.describe(t));
			} finally {
				done.set(true);
			}
		});
		return done;
	}

	/** A sprite with fully transparent pixels in its first frame (a tenth of them or more: the cut-out shape of a plant). */
	private static boolean transparent(TextureAtlasSprite sprite) {
		if (sprite.getFrameCount() == 0) {
			return false;
		}
		int[][] frame = sprite.getFrameTextureData(0);
		if (frame.length == 0 || frame[0] == null || frame[0].length == 0) {
			return false;
		}
		int clear = 0;
		for (int pixel : frame[0]) {
			if (pixel >>> 24 == 0) {
				clear++;
			}
		}
		return clear * 10 >= frame[0].length;
	}

	/**
	 * One quad of a model: its direction when it faces along an axis (false when it doesn't: a crossed plant, a rotated
	 * part, so the model isn't checked as a solid), its corners into the bounds.
	 */
	private static boolean addQuad(BakedQuad quad, Set<EnumFacing> facing, float[] min, float[] max) {
		EnumFacing face = quad.getFace();
		if (face == null || !quad.getFormat().getElement(0).isPositionElement()) {
			return false;
		}
		int[] data = quad.getVertexData();
		int stride = quad.getFormat().getIntegerSize();
		float[][] corners = new float[4][3];
		for (int corner = 0; corner < 4; corner++) {
			for (int axis = 0; axis < 3; axis++) {
				float value = Float.intBitsToFloat(data[corner * stride + axis]);
				corners[corner][axis] = value;
				min[axis] = Math.min(min[axis], value);
				max[axis] = Math.max(max[axis], value);
			}
		}
		// the normal, across the diagonals
		float[] a = {corners[2][0] - corners[0][0], corners[2][1] - corners[0][1], corners[2][2] - corners[0][2]};
		float[] b = {corners[3][0] - corners[1][0], corners[3][1] - corners[1][1], corners[3][2] - corners[1][2]};
		float[] normal = {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
		float length = (float) Math.sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]);
		if (length < 1.0E-6F) {
			return true;
		}
		if (Math.abs(normal[face.getAxis().ordinal()]) / length < 0.999F) {
			return false;
		}
		facing.add(face);
		return true;
	}

	/**
	 * On the client thread: the item used in the air as a player's right-click runs it on the client (held for a second
	 * when it is used over time), then swung. A screen it opens only on the client (a guide book) is closed again.
	 */
	static AtomicBoolean useItem(SelfTest.Run run, String id, Item item) {
		AtomicBoolean done = new AtomicBoolean();
		Minecraft mc = Minecraft.getMinecraft();
		mc.addScheduledTask(() -> {
			GuiScreen before = mc.currentScreen;
			EntityPlayerSP player = mc.player;
			try {
				if (player == null) {
					return;
				}
				ItemStack stack = new ItemStack(item);
				player.setHeldItem(EnumHand.MAIN_HAND, stack);
				clientAttempt(run, id, "client use", () -> stack.useItemRightClick(mc.world, player, EnumHand.MAIN_HAND));
				if (player.isHandActive()) {
					ItemStack active = player.getActiveItemStack();
					clientAttempt(run, id, "client use: held", () -> {
						int duration = active.getMaxItemUseDuration();
						for (int tick = 0; tick < 20 && !active.isEmpty(); tick++) {
							active.getItem().onUsingTick(active, player, duration - tick);
						}
					});
					clientAttempt(run, id, "client use: let go", () -> active.onPlayerStoppedUsing(mc.world, player,
							Math.max(0, active.getMaxItemUseDuration() - 20)));
					player.resetActiveHand();
				}
				clientAttempt(run, id, "client swing", () -> item.onEntitySwing(player, stack));
			} finally {
				if (mc.currentScreen != null && mc.currentScreen != before && !(mc.currentScreen instanceof GuiContainer)) {
					mc.displayGuiScreen(null);
				}
				done.set(true);
			}
		});
		return done;
	}

	private static void clientAttempt(SelfTest.Run run, String id, String what, SelfTest.ThrowingRunnable action) {
		try {
			action.run();
		} catch (Throwable t) {
			run.addClientProblem("item", id, what + ": " + SelfTest.describe(t));
		}
	}

	private static boolean emptyAllSides(IBakedModel model) {
		for (net.minecraft.util.EnumFacing side : net.minecraft.util.EnumFacing.values()) {
			if (!model.getQuads(null, side, 0L).isEmpty()) {
				return false;
			}
		}
		return true;
	}
}
