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

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.util.text.ITextComponent;

/**
 * 1.21 container screen on the 1.12.2 GuiContainer: the 1.12.2 draw calls go to the 1.21 render, renderBg and
 * renderLabels with a GuiGraphics, and the 1.21 fields (menu, title, font, minecraft, label positions) are kept.
 * Widgets (GuiButtons) go to the 1.12.2 button list; other listeners (panels) are drawn over the background and get
 * the mouse from the 1.21 input methods.
 */
public abstract class AbstractContainerScreen<T extends Container> extends GuiContainer implements ContainerEventHandler, Renderable {
	/** 1.21 {@code Screen.getTooltipFromItem(minecraft, stack)} reached through a container screen (Sophisticated's). */
	public static java.util.List<net.minecraft.util.text.ITextComponent> getTooltipFromItem(net.minecraft.client.Minecraft minecraft,
			net.minecraft.item.ItemStack stack) {
		return Screen.getTooltipFromItem(minecraft, stack);
	}

	/** 1.21 {@code removed()}: the screen closed (1.12.2's onGuiClosed calls it). */
	public void removed() {
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		ScreenFailures.removed(this);
	}

	protected final T menu;
	protected final ITextComponent title;
	protected final InventoryPlayer playerInventory;
	protected final ITextComponent playerInventoryTitle;
	protected FontRenderer font;
	protected Minecraft minecraft;
	protected int titleLabelX = 8;
	protected int titleLabelY = 6;
	protected int inventoryLabelX = 8;
	protected int inventoryLabelY;
	private final List<GuiEventListener> children = new ArrayList<>();
	private final List<Renderable> panels = new ArrayList<>();
	/** 1.21 {@code renderables}: every renderable widget (the panels and the buttons), which mods' screens draw and read. */
	protected final List<Renderable> renderables = new ArrayList<>();
	@Nullable
	private GuiEventListener focused;
	private boolean dragging;

	public AbstractContainerScreen(T menu, InventoryPlayer playerInventory, ITextComponent title) {
		super(menu);
		this.menu = menu;
		this.title = title;
		this.playerInventory = playerInventory;
		this.playerInventoryTitle = playerInventory.getDisplayName();
		this.inventoryLabelY = this.ySize - 94;
	}

	@Override
	public void initGui() {
		super.initGui();
		this.font = this.fontRenderer;
		this.minecraft = this.mc;
		this.inventoryLabelY = this.ySize - 94;
		this.children.clear();
		this.panels.clear();
		this.init();
	}

	/** 1.21 init, after the 1.12.2 one has placed the screen. */
	protected void init() {
	}

	/** 1.21 per-tick work of a container screen (a cooking pot's progress, a slot's animation). */
	protected void containerTick() {
	}

	/** 1.21 {@code tick}: the container tick. */
	public void tick() {
		this.containerTick();
	}

	@Override
	public void updateScreen() {
		try {
			super.updateScreen();
			this.tick();
		} catch (RuntimeException | LinkageError e) {
			ScreenFailures.closeBroken(this, "tick", e);
		}
	}

	@Override
	public void handleInput() throws java.io.IOException {
		try {
			super.handleInput();
		} catch (RuntimeException | LinkageError e) {
			ScreenFailures.closeBroken(this, "input", e);
		}
	}

	@Override
	public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
		try {
			this.render(new GuiGraphics(this.mc), mouseX, mouseY, partialTicks);
		} catch (RuntimeException | LinkageError e) {
			ScreenFailures.closeBroken(this, e);
		}
	}

	/** Draws background, slots, widgets and labels; 1.21 subclasses call it as super.render. */
	@Override
	public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
		this.drawDefaultBackground();
	}

	protected void renderTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
		this.renderHoveredToolTip(mouseX, mouseY);
	}

	@Override
	protected final void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		GuiGraphics gfx = new GuiGraphics(this.mc);
		this.renderBg(gfx, partialTicks, mouseX, mouseY);
		// 1.21 draws the renderables after the background, under the slots
		for (Renderable panel : new ArrayList<>(this.panels)) {
			panel.render(gfx, mouseX, mouseY, partialTicks);
		}
	}

	protected abstract void renderBg(GuiGraphics gfx, float partialTicks, int mouseX, int mouseY);

	@Override
	protected final void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		this.renderLabels(new GuiGraphics(this.mc), mouseX, mouseY);
	}

	protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
		gfx.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x404040, false);
		gfx.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
	}

	protected <W extends GuiEventListener> W addRenderableWidget(W widget) {
		if (!(widget instanceof GuiButton) && widget instanceof Renderable) {
			this.panels.add((Renderable) widget);
		}
		if (widget instanceof Renderable renderable) {
			this.renderables.add(renderable);
		}
		return this.addWidget(widget);
	}

	/** 1.21 {@code addRenderableOnly(renderable)}: drawn, not clicked. */
	protected <W extends Renderable> W addRenderableOnly(W renderable) {
		this.panels.add(renderable);
		this.renderables.add(renderable);
		return renderable;
	}

	protected <W extends GuiEventListener> W addWidget(W listener) {
		if (listener instanceof GuiButton) {
			this.buttonList.add((GuiButton) listener);
		}
		this.children.add(listener);
		return listener;
	}

	protected void removeWidget(GuiEventListener listener) {
		this.panels.remove(listener);
		this.renderables.remove(listener);
		this.buttonList.remove(listener);
		this.children.remove(listener);
	}

	/** 1.21 {@code clearWidgets()}: Create's package port screen rebuilds its widgets with it. */
	protected void clearWidgets() {
		this.panels.clear();
		this.renderables.clear();
		this.buttonList.clear();
		this.children.clear();
	}

	@Override
	public List<? extends GuiEventListener> children() {
		return this.children;
	}

	@Nullable
	@Override
	public GuiEventListener getFocused() {
		return this.focused;
	}

	@Override
	public void setFocused(@Nullable GuiEventListener focused) {
		if (this.focused != null) {
			this.focused.setFocused(false);
		}
		if (focused != null) {
			focused.setFocused(true);
		}
		this.focused = focused;
	}

	@Override
	public boolean isDragging() {
		return this.dragging;
	}

	@Override
	public void setDragging(boolean dragging) {
		this.dragging = dragging;
	}

	// 1.21 Screen focus methods, for container screens (Curios, the Aether's accessories, Tom's crafting terminal focus
	// their search box with setInitialFocus; the whole init was left out without it)

	public void clearFocus() {
		this.setFocused(null);
	}

	protected void setInitialFocus() {
	}

	protected void setInitialFocus(GuiEventListener listener) {
		this.setFocused(listener);
	}

	protected void changeFocus(Object path) {
		this.clearFocus();
		ScreenRectangles.applyFocus(path, true);
	}

	protected void rebuildWidgets() {
		this.clearWidgets();
		this.clearFocus();
		this.init();
	}

	protected void repositionElements() {
		this.rebuildWidgets();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> R getRectangle() {
		return (R) ScreenRectangles.of(0, 0, this.width, this.height);
	}

	/** The white highlight of a hovered slot. */
	public static void renderSlotHighlight(GuiGraphics gfx, int x, int y, int z) {
		net.minecraft.client.renderer.GlStateManager.disableLighting();
		net.minecraft.client.renderer.GlStateManager.disableDepth();
		gfx.fill(x, y, x + 16, y + 16, 0x80FFFFFF);
		net.minecraft.client.renderer.GlStateManager.enableDepth();
	}

	protected boolean isHovering(int x, int y, int width, int height, double mouseX, double mouseY) {
		return this.isPointInRegion(x, y, width, height, (int) mouseX, (int) mouseY);
	}

	/**
	 * NeoForge {@code renderSlotHighlight(gfx, x, y, z, color)}: the hovered slot's highlight in that color. Screens that
	 * draw their own slots (Sophisticated's backpacks) call it; without it no slot lit up under the mouse.
	 */
	public static void renderSlotHighlight(GuiGraphics gfx, int x, int y, int z, int color) {
		net.minecraft.client.renderer.GlStateManager.disableLighting();
		net.minecraft.client.renderer.GlStateManager.disableDepth();
		gfx.fill(x, y, x + 16, y + 16, color);
		net.minecraft.client.renderer.GlStateManager.enableDepth();
	}

	/** NeoForge {@code getSlotColor(index)}: the highlight color of a slot (the vanilla white unless the screen sets one). */
	public int getSlotColor(int index) {
		return this.slotColor;
	}

	public static final int SLOT_ITEM_BLIT_OFFSET = 100;
	/** NeoForge: the hovered slot's highlight color. */
	protected int slotColor = 0x80FFFFFF;

	/**
	 * 1.21 {@code renderSlot(gfx, slot)}: a slot and its item drawn as the container draws its own (screens that draw
	 * their slots themselves call it: Sophisticated's backpacks, Tom's terminals); 1.12.2's drawSlot is private.
	 */
	protected void renderSlot(GuiGraphics gfx, net.minecraft.inventory.Slot slot) {
		net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
		net.minecraft.client.renderer.GlStateManager.enableRescaleNormal();
		SlotDrawing.drawVanilla(this, slot);
	}

	/** Screen classes whose own renderSlot failed: drawn the 1.12.2 way from then on. */
	private static final java.util.Set<Class<?>> FAILED_SLOTS = java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<>());

	/**
	 * A slot 1.12.2's drawScreen draws, drawn by the mod's own renderSlot (client.SlotDrawing); false if it failed (the
	 * 1.12.2 way then, and for this screen class from then on).
	 */
	boolean renderSlotOfMod(net.minecraft.inventory.Slot slot) {
		if (FAILED_SLOTS.contains(this.getClass())) {
			return false;
		}
		int depth = PoseStack.depth();
		try {
			this.renderSlot(SlotDrawing.graphics(), slot);
			return true;
		} catch (RuntimeException | LinkageError e) {
			FAILED_SLOTS.add(this.getClass());
			PoseStack.restore(depth);
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + ".renderSlot", e);
			return false;
		} finally {
			// the state 1.12.2's slot loop draws the next slot in
			net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
		}
	}

	/** 1.21 {@code renderSlotContents(gfx, stack, slot, count)}: the item of a slot with its count text. */
	protected void renderSlotContents(GuiGraphics gfx, net.minecraft.item.ItemStack stack, net.minecraft.inventory.Slot slot, @Nullable String count) {
		if (stack.isEmpty()) {
			return;
		}
		gfx.renderItem(stack, slot.xPos, slot.yPos);
		gfx.renderItemDecorations(this.fontRenderer, stack, slot.xPos, slot.yPos, count);
	}

	protected java.util.List<ITextComponent> getTooltipFromContainerItem(net.minecraft.item.ItemStack stack) {
		return getTooltipFromItem(this.mc, stack);
	}

	/** 1.21 hotbar keys over a slot (1 to 9 swap the slot with the hotbar): the scan code is the 1.12.2 key code here. */
	protected boolean checkHotbarKeyPressed(int keyCode, int scanCode) {
		return this.checkHotbarKeys(scanCode);
	}

	/** 1.21 hotbar swaps bound to mouse buttons: 1.12.2 binds the hotbar to keys only. */
	protected void checkHotbarMouseClicked(int button) {
	}

	public void clearDraggingState() {
	}

	/** 1.21 crafter slots toggled on and off: no such slots in 1.12.2. */
	protected void handleSlotStateChanged(int slotId, int containerId, boolean enabled) {
	}

	public boolean isPauseScreen() {
		return false;
	}

	/** 1.21 {@code onClose}: the container closed on the server too. */
	public void onClose() {
		if (this.mc.player != null) {
			this.mc.player.closeScreen();
		} else {
			this.mc.displayGuiScreen(null);
		}
	}

	/**
	 * 1.21 {@code renderFloatingItem(gfx, stack, x, y, text)} (the tables name it 1.12.2's drawItemStack, which is private
	 * and takes no GuiGraphics): the stack on the cursor or being dragged, drawn over everything. Screens that draw
	 * themselves (Sophisticated's backpacks) call it; without it a picked-up item vanished from sight.
	 */
	protected void drawItemStack(GuiGraphics gfx, net.minecraft.item.ItemStack stack, int x, int y, @Nullable String text) {
		this.renderFloatingItem(gfx, stack, x, y, text);
	}

	protected void renderFloatingItem(GuiGraphics gfx, net.minecraft.item.ItemStack stack, int x, int y, @Nullable String text) {
		if (stack.isEmpty()) {
			return;
		}
		net.minecraft.client.renderer.GlStateManager.pushMatrix();
		net.minecraft.client.renderer.GlStateManager.translate(0.0F, 0.0F, 232.0F);
		net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting();
		this.zLevel = 200.0F;
		this.itemRender.zLevel = 200.0F;
		FontRenderer itemFont = stack.getItem().getFontRenderer(stack);
		if (itemFont == null) {
			itemFont = this.fontRenderer;
		}
		this.itemRender.renderItemAndEffectIntoGUI(stack, x, y);
		this.itemRender.renderItemOverlayIntoGUI(itemFont, stack, x, y, text);
		this.zLevel = 0.0F;
		this.itemRender.zLevel = 0.0F;
		net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting();
		net.minecraft.client.renderer.GlStateManager.popMatrix();
	}

	/** 1.21 {@code findSlot(mouseX, mouseY)}: the slot under the mouse, null if none. */
	@Nullable
	protected net.minecraft.inventory.Slot findSlot(double mouseX, double mouseY) {
		for (net.minecraft.inventory.Slot slot : this.inventorySlots.inventorySlots) {
			if (slot.isEnabled() && this.isHovering(slot, mouseX, mouseY)) {
				return slot;
			}
		}
		return null;
	}

	/** 1.21 {@code hasClickedOutside}: the click is outside the screen's panel (the carried stack is thrown out). */
	protected boolean hasClickedOutside(double mouseX, double mouseY, int left, int top, int button) {
		return mouseX < left || mouseY < top || mouseX >= left + this.xSize || mouseY >= top + this.ySize;
	}

	/** 1.21 {@code isHovering(slot, mouseX, mouseY)}: the mouse over the slot (Sophisticated finds the clicked slot so). */
	protected boolean isHovering(net.minecraft.inventory.Slot slot, double mouseX, double mouseY) {
		return this.isHovering(slot.xPos, slot.yPos, 16, 16, mouseX, mouseY);
	}

	/** 1.21 {@code Screen.hasShiftDown()} and friends, for screens that are containers. */
	public static boolean hasShiftDown() {
		return net.minecraft.client.gui.GuiScreen.isShiftKeyDown();
	}

	public static boolean hasControlDown() {
		return net.minecraft.client.gui.GuiScreen.isCtrlKeyDown();
	}

	public static boolean hasAltDown() {
		return net.minecraft.client.gui.GuiScreen.isAltKeyDown();
	}

	/** 1.21 {@code renderTransparentBackground(gfx)}: the dark gradient behind a screen. */
	public void renderTransparentBackground(GuiGraphics gfx) {
		this.drawDefaultBackground();
	}

	public T getMenu() {
		return this.menu;
	}

	public ITextComponent getTitle() {
		return this.title;
	}

	public Minecraft getMinecraft() {
		return this.mc;
	}

	// --- input: the 1.12.2 handlers call the 1.21 ones (which a subclass overrides); their defaults give the event to the
	// listeners that aren't buttons (panels), then to the 1.12.2 handling. Keys are GLFW codes in 1.21.

	private int dragX;
	private int dragY;

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		for (GuiEventListener child : new ArrayList<>(this.children)) {
			if (!(child instanceof GuiButton) && child.mouseClicked(mouseX, mouseY, button)) {
				this.setFocused(child);
				if (button == 0) {
					this.dragging = true;
				}
				return true;
			}
		}
		try {
			super.mouseClicked((int) mouseX, (int) mouseY, button);
		} catch (java.io.IOException e) {
			throw new java.io.UncheckedIOException(e);
		}
		return true;
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) throws java.io.IOException {
		this.dragX = mouseX;
		this.dragY = mouseY;
		this.mouseClicked((double) mouseX, (double) mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0 && this.dragging) {
			this.dragging = false;
			if (this.focused != null && !(this.focused instanceof GuiButton) && this.focused.mouseReleased(mouseX, mouseY, button)) {
				return true;
			}
		}
		super.mouseReleased((int) mouseX, (int) mouseY, button);
		return true;
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		this.mouseReleased((double) mouseX, (double) mouseY, state);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		if (this.focused != null && !(this.focused instanceof GuiButton) && this.dragging && button == 0
				&& this.focused.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
			return true;
		}
		super.mouseClickMove((int) mouseX, (int) mouseY, button, 0L);
		return true;
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceLastClick) {
		this.mouseDragged(mouseX, mouseY, button, mouseX - this.dragX, mouseY - this.dragY);
		this.dragX = mouseX;
		this.dragY = mouseY;
	}

	/** Wheel up is a positive scrollY (1.12.2 has no horizontal wheel). */
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		return ContainerEventHandler.super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void handleMouseInput() throws java.io.IOException {
		super.handleMouseInput();
		int wheel = org.lwjgl.input.Mouse.getEventDWheel();
		if (wheel != 0) {
			double mouseX = org.lwjgl.input.Mouse.getEventX() * this.width / (double) this.mc.displayWidth;
			double mouseY = this.height - org.lwjgl.input.Mouse.getEventY() * this.height / (double) this.mc.displayHeight - 1;
			this.mouseScrolled(mouseX, mouseY, 0.0D, wheel > 0 ? 1.0D : -1.0D);
		}
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return ContainerEventHandler.super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		return ContainerEventHandler.super.charTyped(codePoint, modifiers);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws java.io.IOException {
		int glfw = io.github.xponer2.translator.runtime.neoforge.KeyMapping.toGlfw(keyCode);
		if (this.keyPressed(glfw, keyCode, 0)) {
			return;
		}
		if (typedChar >= ' ' && this.charTyped(typedChar, 0)) {
			return;
		}
		if (keyCode == org.lwjgl.input.Keyboard.KEY_ESCAPE || this.mc.gameSettings.keyBindInventory.isActiveAndMatches(keyCode)) {
			// closed through onClose, as 1.21 (a screen that saves its state there overrides it)
			this.onClose();
			return;
		}
		super.keyTyped(typedChar, keyCode);
	}
}
