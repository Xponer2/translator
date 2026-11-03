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

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;

/**
 * 1.21 slider: `value` from 0 to 1, set by clicking and dragging; subclasses show it in updateMessage and use it in
 * applyValue. Drawn like the 1.12.2 option sliders (the disabled button look with a handle).
 */
public abstract class AbstractSliderButton extends AbstractWidget {
	protected static final int GLFW_KEY_RIGHT = 262;
	protected static final int GLFW_KEY_LEFT = 263;

	protected double value;

	public AbstractSliderButton(int x, int y, int width, int height, ITextComponent message, double value) {
		super(x, y, width, height, message);
		this.value = value;
	}

	protected abstract void updateMessage();

	protected abstract void applyValue();

	@Override
	public void onClick(double mouseX, double mouseY) {
		this.setValueFromMouse(mouseX);
	}

	@Override
	protected void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
		this.setValueFromMouse(mouseX);
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == GLFW_KEY_LEFT || keyCode == GLFW_KEY_RIGHT) {
			float step = keyCode == GLFW_KEY_LEFT ? -1.0F : 1.0F;
			this.setValue(this.value + step / (this.width - 8));
			return true;
		}
		return false;
	}

	private void setValueFromMouse(double mouseX) {
		this.setValue((mouseX - (this.x + 4)) / (this.width - 8));
	}

	private void setValue(double value) {
		double old = this.value;
		this.value = MathHelper.clamp(value, 0.0D, 1.0D);
		if (old != this.value) {
			this.applyValue();
		}
		this.updateMessage();
	}

	@Override
	protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float partialTicks) {
		Minecraft mc = Minecraft.getMinecraft();
		mc.getTextureManager().bindTexture(BUTTON_TEXTURES);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
				GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
		GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
		// the track: the disabled button row of the texture, as GuiOptionSlider
		this.drawTexturedModalRect(this.x, this.y, 0, 46, this.width / 2, this.height);
		this.drawTexturedModalRect(this.x + this.width / 2, this.y, 200 - this.width / 2, 46, this.width / 2, this.height);
		int handle = this.x + (int) (this.value * (this.width - 8));
		int handleV = this.isHovered || this.isFocused() ? 86 : 66;
		this.drawTexturedModalRect(handle, this.y, 0, handleV, 4, this.height);
		this.drawTexturedModalRect(handle + 4, this.y, 196, handleV, 4, this.height);
		int color = this.packedFGColor != UNSET_FG_COLOR ? this.packedFGColor : !this.active ? 0xA0A0A0 : this.isHovered ? 0xFFFFA0 : 0xE0E0E0;
		this.drawCenteredString(mc.fontRenderer, this.displayString, this.x + this.width / 2, this.y + (this.height - 8) / 2, color);
	}
}
