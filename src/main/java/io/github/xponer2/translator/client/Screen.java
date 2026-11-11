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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import io.github.xponer2.translator.runtime.neoforge.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;

/**
 * 1.21 screen on the 1.12.2 GuiScreen: the 1.12.2 calls go to the 1.21 init, render, tick, removed and input
 * methods. Widgets (GuiButtons) are drawn as renderables and clicked through the 1.12.2 button list; other
 * listeners (panels) get the mouse from the 1.21 input methods. Keys are GLFW codes, like in 1.21.
 */
public abstract class Screen extends GuiScreen implements ContainerEventHandler, Renderable {
	private static final int GLFW_KEY_ESCAPE = 256;

	protected final ITextComponent title;
	protected Minecraft minecraft;
	protected FontRenderer font;
	protected final List<Renderable> renderables = new ArrayList<>();
	private final List<GuiEventListener> children = new ArrayList<>();
	@Nullable
	private GuiEventListener focused;
	private boolean dragging;
	private int dragX;
	private int dragY;

	protected Screen(ITextComponent title) {
		this.title = title;
	}

	@Override
	public void initGui() {
		// 1.21 calls added() each time the screen is shown, then init(minecraft, width, height), which mods override
		// (JourneyMap's screens set themselves up there): 1.12.2 inits a screen it shows, sized already
		this.added();
		this.init(this.mc, this.width, this.height);
	}

	/** 1.21 init: adds the widgets; runs again when the window is resized. */
	protected void init() {
	}

	private boolean initialized;

	/** 1.21 {@code init(minecraft, width, height)}: sized, the widgets made again, the first one focused the first time. */
	public void init(Minecraft minecraft, int width, int height) {
		this.mc = minecraft;
		this.minecraft = minecraft;
		this.fontRenderer = minecraft.fontRenderer;
		this.font = this.fontRenderer;
		this.itemRender = minecraft.getRenderItem();
		this.width = width;
		this.height = height;
		this.clearWidgets();
		this.init();
		if (!this.initialized) {
			this.initialized = true;
			this.setInitialFocus();
		}
	}

	@Override
	public final void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.mouseX = mouseX;
		this.mouseY = mouseY;
		try {
			this.renderWithTooltip(new GuiGraphics(this.mc), mouseX, mouseY, partialTicks);
		} catch (RuntimeException | LinkageError e) {
			ScreenFailures.closeBroken(this, e);
		}
	}

	private int mouseX;
	private int mouseY;
	@Nullable
	private Tooltip deferredTooltip;

	/** 1.21 {@code renderWithTooltip}: the screen, then the tooltip a widget asked for during it. */
	public void renderWithTooltip(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
		this.render(gfx, mouseX, mouseY, partialTick);
		if (this.deferredTooltip != null) {
			Tooltip.show(this.deferredTooltip, mouseX, mouseY);
			this.deferredTooltip = null;
		}
	}

	/** 1.21 {@code setTooltipForNextRenderPass(text)} or {@code (lines)} (one method: they have one parameter each). */
	public void setTooltipForNextRenderPass(Object textOrLines) {
		if (textOrLines instanceof ITextComponent text) {
			this.deferredTooltip = Tooltip.create(text);
		} else if (textOrLines instanceof List<?> lines && !lines.isEmpty()) {
			var joined = new net.minecraft.util.text.TextComponentString("");
			for (int i = 0; i < lines.size(); i++) {
				if (i > 0) {
					joined.appendText("\n");
				}
				Object line = lines.get(i);
				joined.appendSibling(line instanceof ITextComponent component ? component : new net.minecraft.util.text.TextComponentString(String.valueOf(line)));
			}
			this.deferredTooltip = Tooltip.create(joined);
		}
	}

	public void setTooltipForNextRenderPass(Tooltip tooltip, Object positioner, boolean override) {
		if (override || this.deferredTooltip == null) {
			this.deferredTooltip = tooltip;
		}
	}

	protected void clearTooltipForNextRenderPass() {
		this.deferredTooltip = null;
	}

	@Override
	public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(gfx, mouseX, mouseY, partialTick);
		for (Renderable renderable : new ArrayList<>(this.renderables)) {
			renderable.render(gfx, mouseX, mouseY, partialTick);
		}
	}

	public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
		this.drawDefaultBackground();
	}

	// --- widgets

	protected <T extends GuiEventListener> T addRenderableWidget(T widget) {
		if (widget instanceof Renderable) {
			this.renderables.add((Renderable) widget);
		}
		return this.addWidget(widget);
	}

	protected <T extends Renderable> T addRenderableOnly(T renderable) {
		this.renderables.add(renderable);
		return renderable;
	}

	protected <T extends GuiEventListener> T addWidget(T listener) {
		if (listener instanceof GuiButton) {
			this.buttonList.add((GuiButton) listener);
		}
		this.children.add(listener);
		return listener;
	}

	protected void removeWidget(GuiEventListener listener) {
		this.renderables.remove(listener);
		this.buttonList.remove(listener);
		this.children.remove(listener);
	}

	protected void clearWidgets() {
		this.renderables.clear();
		this.children.clear();
		this.buttonList.clear();
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

	// --- life cycle

	public void tick() {
	}

	@Override
	public void updateScreen() {
		try {
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

	/** The screen was closed or replaced. */
	public void removed() {
	}

	@Override
	public void onGuiClosed() {
		ScreenFailures.removed(this);
	}

	public void onClose() {
		this.mc.displayGuiScreen(null);
	}

	public boolean shouldCloseOnEsc() {
		return true;
	}

	public boolean isPauseScreen() {
		return true;
	}

	@Override
	public boolean doesGuiPauseGame() {
		return this.isPauseScreen();
	}

	public ITextComponent getTitle() {
		return this.title;
	}

	public Minecraft getMinecraft() {
		return this.mc;
	}

	/** 1.21 {@code renderTransparentBackground(gfx)}: the dark gradient behind a screen (JourneyMap's popups draw it). */
	public void renderTransparentBackground(GuiGraphics gfx) {
		this.drawDefaultBackground();
	}

	/** 1.21 {@code Screen.getTooltipFromItem(minecraft, stack)}: the stack's tooltip lines as text components. */
	public static List<ITextComponent> getTooltipFromItem(Minecraft minecraft, net.minecraft.item.ItemStack stack) {
		var lines = new ArrayList<ITextComponent>();
		for (String line : stack.getTooltip(minecraft.player, minecraft.gameSettings.advancedItemTooltips
				? net.minecraft.client.util.ITooltipFlag.TooltipFlags.ADVANCED : net.minecraft.client.util.ITooltipFlag.TooltipFlags.NORMAL)) {
			lines.add(new net.minecraft.util.text.TextComponentString(line));
		}
		return lines;
	}

	public static boolean hasShiftDown() {
		return isShiftKeyDown();
	}

	public static boolean hasControlDown() {
		return isCtrlKeyDown();
	}

	public static boolean hasAltDown() {
		return isAltKeyDown();
	}

	/** GLFW modifier bits of the keys held now (shift 1, control 2, alt 4). */
	static int modifiers() {
		return (isShiftKeyDown() ? 1 : 0) | (isCtrlKeyDown() ? 2 : 0) | (isAltKeyDown() ? 4 : 0);
	}

	// --- input: the 1.12.2 handlers call the 1.21 ones (which a subclass overrides); their defaults give the event
	// to the listeners that aren't buttons, then to the 1.12.2 handling (buttons).

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		for (GuiEventListener child : new ArrayList<>(this.children)) {
			if (!(child instanceof GuiButton) && child.mouseClicked(mouseX, mouseY, button)) {
				this.setFocused(child);
				if (button == 0) {
					this.setDragging(true);
				}
				return true;
			}
		}
		try {
			super.mouseClicked((int) mouseX, (int) mouseY, button);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
		for (GuiButton widget : this.buttonList) {
			if (widget.visible && widget.enabled && mouseX >= widget.x && mouseY >= widget.y && mouseX < widget.x + widget.width
					&& mouseY < widget.y + widget.height) {
				return true;
			}
		}
		return false;
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) throws IOException {
		this.dragX = mouseX;
		this.dragY = mouseY;
		this.mouseClicked((double) mouseX, (double) mouseY, button);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		boolean handled = false;
		if (button == 0 && this.dragging) {
			this.dragging = false;
			if (this.focused != null && !(this.focused instanceof GuiButton)) {
				handled = this.focused.mouseReleased(mouseX, mouseY, button);
			}
		}
		super.mouseReleased((int) mouseX, (int) mouseY, button);
		return handled;
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		this.mouseReleased((double) mouseX, (double) mouseY, state);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
		return this.focused != null && !(this.focused instanceof GuiButton) && this.dragging && button == 0
				&& this.focused.mouseDragged(mouseX, mouseY, button, dragX, dragY);
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
	public void handleMouseInput() throws IOException {
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
		if (keyCode == GLFW_KEY_ESCAPE && this.shouldCloseOnEsc()) {
			this.onClose();
			return true;
		}
		return ContainerEventHandler.super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		// Escape closes the screen in keyPressed (1.21), not in the 1.12.2 keyTyped
		if (!this.keyPressed(KeyMapping.toGlfw(keyCode), keyCode, modifiers()) && typedChar >= ' ') {
			this.charTyped(typedChar, modifiers());
		}
	}

	// --- the rest of the 1.21 screen API ------------------------------------------------------------------------------

	public static final net.minecraft.util.ResourceLocation MENU_BACKGROUND = new net.minecraft.util.ResourceLocation("textures/gui/menu_background.png");
	public static final net.minecraft.util.ResourceLocation HEADER_SEPARATOR = new net.minecraft.util.ResourceLocation("textures/gui/header_separator.png");
	public static final net.minecraft.util.ResourceLocation FOOTER_SEPARATOR = new net.minecraft.util.ResourceLocation("textures/gui/footer_separator.png");
	public static final net.minecraft.util.ResourceLocation INWORLD_MENU_BACKGROUND = new net.minecraft.util.ResourceLocation(
			"textures/gui/inworld_menu_background.png");
	public static final net.minecraft.util.ResourceLocation INWORLD_HEADER_SEPARATOR = new net.minecraft.util.ResourceLocation(
			"textures/gui/inworld_header_separator.png");
	public static final net.minecraft.util.ResourceLocation INWORLD_FOOTER_SEPARATOR = new net.minecraft.util.ResourceLocation(
			"textures/gui/inworld_footer_separator.png");

	/** 1.21 {@code screenExecutor}: runs a task on the game thread while this screen is still shown. */
	protected final java.util.concurrent.Executor screenExecutor = task -> Minecraft.getMinecraft().addScheduledTask(() -> {
		if (Minecraft.getMinecraft().currentScreen == this) {
			task.run();
		}
	});

	/** Shown (1.21 setScreen calls it before init). */
	public void added() {
	}

	/** 1.21 {@code resize}: the new size, the widgets placed again (repositionElements, which mods override). */
	public void resize(Minecraft minecraft, int width, int height) {
		this.width = width;
		this.height = height;
		this.repositionElements();
	}

	@Override
	public void onResize(Minecraft minecraft, int width, int height) {
		this.mc = minecraft;
		this.resize(minecraft, width, height);
	}

	protected void repositionElements() {
		this.rebuildWidgets();
	}

	protected void rebuildWidgets() {
		this.clearWidgets();
		this.clearFocus();
		this.init();
	}

	public void clearFocus() {
		this.setFocused(null);
	}

	/** 1.21 {@code changeFocus(path)}: the focus moved along a (carried) component path. */
	protected void changeFocus(Object path) {
		this.clearFocus();
		ScreenRectangles.applyFocus(path, true);
	}

	/** 1.21 picks the first widget for keyboard users; 1.12.2 screens start with nothing focused. */
	protected void setInitialFocus() {
	}

	/** 1.21 {@code setInitialFocus(listener)}: that listener focused (a search box takes the typing at once). */
	protected void setInitialFocus(GuiEventListener listener) {
		this.setFocused(listener);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R> R getRectangle() {
		return (R) ScreenRectangles.of(0, 0, this.width, this.height);
	}

	public ITextComponent getNarrationMessage() {
		return this.getTitle();
	}

	protected ITextComponent getUsageNarration() {
		return new net.minecraft.util.text.TextComponentString("");
	}

	/** 1.21 {@code handleComponentClicked(style)}: the click event of a chat style (open a link, run a command), as 1.12.2. */
	public boolean handleComponentClicked(@Nullable net.minecraft.util.text.Style style) {
		return style != null && this.handleComponentClick(new net.minecraft.util.text.TextComponentString("").setStyle(style));
	}

	protected void insertText(String text, boolean overwrite) {
	}

	public static boolean isCut(int keyCode) {
		return keyCode == 88 && hasControlDown() && !hasShiftDown() && !hasAltDown();
	}

	public static boolean isPaste(int keyCode) {
		return keyCode == 86 && hasControlDown() && !hasShiftDown() && !hasAltDown();
	}

	public static boolean isCopy(int keyCode) {
		return keyCode == 67 && hasControlDown() && !hasShiftDown() && !hasAltDown();
	}

	public static boolean isSelectAll(int keyCode) {
		return keyCode == 65 && hasControlDown() && !hasShiftDown() && !hasAltDown();
	}

	/** 1.21 {@code isValidCharacterForName}: the characters of a resource location (a world or pack name). */
	protected boolean isValidCharacterForName(String text, char character, int cursor) {
		int colon = text.indexOf(':');
		int slash = text.indexOf('/');
		if (character == ':') {
			return (slash == -1 || cursor <= slash) && colon == -1;
		}
		if (character == '/') {
			return cursor > colon;
		}
		return character == '_' || character == '-' || character >= 'a' && character <= 'z' || character >= '0' && character <= '9' || character == '.';
	}

	public void onFilesDrop(List<java.nio.file.Path> files) {
	}

	/** 1.12.2 has no blur behind screens. */
	protected void renderBlurredBackground(float partialTick) {
	}

	/** 1.21 menu background: the 1.12.2 dirt in the menus, the dark gradient over a world. */
	protected void renderMenuBackground(GuiGraphics gfx) {
		this.renderMenuBackground(gfx, 0, 0, this.width, this.height);
	}

	protected void renderMenuBackground(GuiGraphics gfx, int x, int y, int width, int height) {
		renderMenuBackgroundTexture(gfx, this.mc != null && this.mc.world != null ? INWORLD_MENU_BACKGROUND : MENU_BACKGROUND, x, y, 0.0F, 0.0F, width, height);
	}

	/**
	 * 1.21 {@code renderMenuBackgroundTexture}: its menu textures are missing in 1.12.2, so the menu one is the darkened
	 * dirt of 1.12.2 menus and the in-world one a dark see-through fill; a mod's own texture is tiled as asked.
	 */
	public static void renderMenuBackgroundTexture(GuiGraphics gfx, net.minecraft.util.ResourceLocation texture, int x, int y, float u, float v, int width,
			int height) {
		if (INWORLD_MENU_BACKGROUND.equals(texture)) {
			gfx.fill(x, y, x + width, y + height, 0xA0101010);
			return;
		}
		var drawn = MENU_BACKGROUND.equals(texture) ? OPTIONS_BACKGROUND : texture;
		net.minecraft.client.renderer.GlStateManager.enableBlend();
		Minecraft.getMinecraft().getTextureManager().bindTexture(drawn);
		net.minecraft.client.renderer.GlStateManager.color(0.25F, 0.25F, 0.25F, 1.0F);
		net.minecraft.client.gui.Gui.drawModalRectWithCustomSizedTexture(x, y, u, v, width, height, 32.0F, 32.0F);
		net.minecraft.client.renderer.GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	/** The title screen panorama of 1.21: the menu background here. */
	protected void renderPanorama(GuiGraphics gfx, float partialTick) {
		this.renderMenuBackground(gfx);
	}

	// narration: 1.12.2 has none for screens, the Translator never narrates

	public void afterKeyboardAction() {
	}

	public void afterMouseAction() {
	}

	public void afterMouseMove() {
	}

	public void handleDelayedNarration() {
	}

	public void triggerImmediateNarration(boolean onlyChanged) {
	}

	public void updateNarratorStatus(boolean enabled) {
	}

	protected boolean shouldNarrateNavigation() {
		return true;
	}

	protected void updateNarrationState(NarrationElementOutput output) {
	}

	protected void updateNarratedWidget(NarrationElementOutput output) {
	}

	/** 1.21 {@code wrapScreenError}: a failure in a screen's action reported as a crash naming the screen. */
	public static void wrapScreenError(Runnable action, String description, String screenName) {
		try {
			action.run();
		} catch (Throwable t) {
			var report = net.minecraft.crash.CrashReport.makeCrashReport(t, description);
			report.makeCategory("Affected screen").addCrashSection("Screen name", screenName);
			throw new net.minecraft.util.ReportedException(report);
		}
	}
}
