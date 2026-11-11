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

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

/**
 * 1.21 button: a widget that calls its OnPress when clicked. Draws the vanilla 1.12.2 button unless a subclass
 * draws its own in renderWidget.
 */
public class Button extends AbstractWidget {
	public static final int SMALL_WIDTH = 120;
	public static final int DEFAULT_WIDTH = 150;
	public static final int DEFAULT_HEIGHT = 20;
	public static final int BIG_WIDTH = 200;
	public static final int DEFAULT_SPACING = 8;
	public static final CreateNarration DEFAULT_NARRATION = Supplier::get;
	protected static final WidgetSprites SPRITES = new WidgetSprites(new ResourceLocation("widget/button"),
			new ResourceLocation("widget/button_disabled"), new ResourceLocation("widget/button_highlighted"));

	protected final OnPress onPress;
	protected final CreateNarration createNarration;

	protected Button(int x, int y, int width, int height, ITextComponent message, OnPress onPress, CreateNarration createNarration) {
		super(x, y, width, height, message);
		this.onPress = onPress;
		this.createNarration = createNarration;
	}

	protected Button(Builder builder) {
		this(builder.x, builder.y, builder.width, builder.height, builder.message, builder.onPress, builder.createNarration);
		this.setTooltip(builder.tooltip);
	}

	public static Builder builder(ITextComponent message, OnPress onPress) {
		return new Builder(message, onPress);
	}

	public void onPress() {
		this.onPress.onPress(this);
	}

	@Override
	public void onClick(double mouseX, double mouseY) {
		this.onPress();
	}

	@Override
	protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
		this.renderDefaultButton(Minecraft.getMinecraft(), mouseX, mouseY, partialTicks);
	}

	@Override
	protected ITextComponent createNarrationMessage() {
		return this.createNarration.createNarrationMessage(super::createNarrationMessage);
	}

	@FunctionalInterface
	public interface OnPress {
		void onPress(Button button);
	}

	@FunctionalInterface
	public interface CreateNarration {
		ITextComponent createNarrationMessage(Supplier<ITextComponent> message);
	}

	public static class Builder {
		private final ITextComponent message;
		private final OnPress onPress;
		private int x;
		private int y;
		private int width = DEFAULT_WIDTH;
		private int height = DEFAULT_HEIGHT;
		private CreateNarration createNarration = DEFAULT_NARRATION;
		@javax.annotation.Nullable
		private Tooltip tooltip;

		public Builder(ITextComponent message, OnPress onPress) {
			this.message = message;
			this.onPress = onPress;
		}

		public Builder pos(int x, int y) {
			this.x = x;
			this.y = y;
			return this;
		}

		public Builder width(int width) {
			this.width = width;
			return this;
		}

		public Builder size(int width, int height) {
			this.width = width;
			this.height = height;
			return this;
		}

		public Builder bounds(int x, int y, int width, int height) {
			return this.pos(x, y).size(width, height);
		}

		public Builder tooltip(@javax.annotation.Nullable Tooltip tooltip) {
			this.tooltip = tooltip;
			return this;
		}

		public Builder createNarration(CreateNarration createNarration) {
			this.createNarration = createNarration;
			return this;
		}

		public Button build() {
			return this.build(Button::new);
		}

		/** NeoForge: a Button subclass built from this builder. */
		public Button build(java.util.function.Function<Builder, Button> factory) {
			return factory.apply(this);
		}
	}
}
