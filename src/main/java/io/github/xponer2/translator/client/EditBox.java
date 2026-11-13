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

import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import io.github.xponer2.translator.runtime.neoforge.KeyMapping;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiPageButtonList;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The 1.21 text field ({@code EditBox}, a widget of a screen) on the 1.12.2 {@link GuiTextField}: built from a font
 * and a position, it takes the screen's input events (GLFW key codes, see {@link Screen}), renders itself and tells a
 * responder about every change of its text.
 */
@SideOnly(Side.CLIENT)
public class EditBox extends GuiTextField implements GuiEventListener, Renderable, NarratableEntry {
	public boolean active = true;
	@Nullable
	public Consumer<String> responder;
	private final FontRenderer font;
	private ITextComponent message;
	private boolean editable = true;
	private boolean hovered;
	/** 1.21's field (a mod's own box reads it: JourneyMap's text boxes), kept with the hover state. */
	protected boolean isHovered;
	@Nullable
	private String suggestion;
	@Nullable
	private ITextComponent hint;

	public EditBox(FontRenderer font, int x, int y, int width, int height, ITextComponent message) {
		super(0, font, x, y, width, height);
		this.font = font;
		this.message = message;
		this.setGuiResponder(new GuiPageButtonList.GuiResponder() {
			@Override
			public void setEntryValue(int id, boolean value) {
			}

			@Override
			public void setEntryValue(int id, float value) {
			}

			@Override
			public void setEntryValue(int id, String value) {
				EditBox.this.changed(value);
			}
		});
	}

	public EditBox(FontRenderer font, int width, int height, ITextComponent message) {
		this(font, 0, 0, width, height, message);
	}

	public EditBox(FontRenderer font, int x, int y, int width, int height, @Nullable EditBox previous, ITextComponent message) {
		this(font, x, y, width, height, message);
		if (previous != null) {
			this.setText(previous.getText());
		}
	}

	private void changed(String value) {
		if (this.responder != null) {
			this.responder.accept(value);
		}
	}

	@Override
	public void setText(String text) {
		super.setText(text);
		this.changed(this.getText());
	}

	public void setResponder(@Nullable Consumer<String> responder) {
		this.responder = responder;
	}

	public void setFilter(Predicate<String> filter) {
		this.setValidator(filter::test);
	}

	public void setFormatter(BiFunction<String, Integer, ?> formatter) {
	}

	public void setSuggestion(@Nullable String suggestion) {
		this.suggestion = suggestion;
	}

	public void setHint(ITextComponent hint) {
		this.hint = hint;
	}

	public ITextComponent getMessage() {
		return this.message;
	}

	public void setMessage(ITextComponent message) {
		this.message = message;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
		this.setEnabled(editable);
	}

	public boolean isEditable() {
		return this.editable;
	}

	public boolean canConsumeInput() {
		return this.getVisible() && this.isFocused() && this.editable;
	}

	// --- cursor ------------------------------------------------------------------------------------------------------

	public void moveCursorToStart(boolean select) {
		this.setCursorPositionZero();
	}

	public void moveCursorToEnd(boolean select) {
		this.setCursorPositionEnd();
	}

	public void moveCursorTo(int position, boolean select) {
		this.setCursorPosition(position);
	}

	public int getScreenX(int index) {
		String text = this.getText();
		return this.x + (this.getEnableBackgroundDrawing() ? 4 : 0) + this.font.getStringWidth(text.substring(0, Math.max(0, Math.min(index, text.length()))));
	}

	// --- position and size (AbstractWidget) --------------------------------------------------------------------------

	public int getX() {
		return this.x;
	}

	public int getY() {
		return this.y;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

	public void setPosition(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public int getHeight() {
		return this.height;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public void setSize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	public int getRight() {
		return this.x + this.width;
	}

	public int getBottom() {
		return this.y + this.height;
	}

	public boolean isHovered() {
		return this.hovered;
	}

	/** 1.21 {@code getInnerWidth()}: the width the text has inside the border. */
	public int getInnerWidth() {
		return this.getWidth();
	}

	public boolean isHoveredOrFocused() {
		return this.hovered || this.isFocused();
	}

	public boolean isActive() {
		return this.getVisible() && this.active;
	}

	// --- rendering ---------------------------------------------------------------------------------------------------

	@Override
	public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
		if (this.getVisible()) {
			this.hovered = this.isMouseOver(mouseX, mouseY);
			this.isHovered = this.hovered;
			this.renderWidget(gfx, mouseX, mouseY, partialTick);
		}
	}

	public void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
		this.drawTextBox();
		int textX = this.x + (this.getEnableBackgroundDrawing() ? 4 : 0);
		int textY = this.y + (this.getEnableBackgroundDrawing() ? (this.height - 8) / 2 : 0);
		if (this.getText().isEmpty() && this.hint != null && !this.isFocused()) {
			this.font.drawStringWithShadow(this.hint.getFormattedText(), textX, textY, 0x707070);
		} else if (this.suggestion != null && this.getCursorPosition() == this.getText().length()) {
			this.font.drawStringWithShadow(this.suggestion, textX + this.font.getStringWidth(this.getText()), textY, 0x808080);
		}
	}

	// --- input (GLFW key codes from the screen, see Screen) ----------------------------------------------------------

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		return this.getVisible() && this.mouseClicked((int) mouseX, (int) mouseY, button) && this.isMouseOver(mouseX, mouseY);
	}

	@Override
	public boolean isMouseOver(double mouseX, double mouseY) {
		return this.getVisible() && mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return this.canConsumeInput() && this.textboxKeyTyped('\0', KeyMapping.toLwjgl(keyCode));
	}

	@Override
	public boolean charTyped(char codePoint, int modifiers) {
		return this.canConsumeInput() && this.textboxKeyTyped(codePoint, 0);
	}

	// --- narration ---------------------------------------------------------------------------------------------------

	@Override
	public NarrationPriority narrationPriority() {
		return this.isFocused() ? NarrationPriority.FOCUSED : NarrationPriority.NONE;
	}

	@Override
	public void updateNarration(NarrationElementOutput output) {
	}

	public ITextComponent createNarrationMessage() {
		return new TextComponentString(this.getText());
	}

	public void updateWidgetNarration(NarrationElementOutput output) {
	}

	// --- the rest of the 1.21 text field API -------------------------------------------------------------------------

	public static final int BACKWARDS = -1;
	public static final int FORWARDS = 1;
	public static final int DEFAULT_TEXT_COLOR = 14737632;
	private boolean textShadow = true;

	/** 1.21 {@code deleteCharsToPos(pos)}: the text between the cursor and pos removed (the selection first). */
	public void deleteCharsToPos(int pos) {
		this.deleteFromCursor(pos - this.getCursorPosition());
	}

	public boolean getTextShadow() {
		return this.textShadow;
	}

	public void setTextShadow(boolean shadow) {
		this.textShadow = shadow;
	}

	/** 1.21 {@code onClick(mouseX, mouseY)}: the cursor put where the field was clicked. */
	public void onClick(double mouseX, double mouseY) {
		this.mouseClicked((int) mouseX, (int) mouseY, 0);
	}

	/** A text field makes no click sound. */
	public void playDownSound(net.minecraft.client.audio.SoundHandler sounds) {
	}
}
