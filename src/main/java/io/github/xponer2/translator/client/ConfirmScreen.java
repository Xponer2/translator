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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.21 {@code ConfirmScreen} (S row): a title, a message and yes / no buttons that answer the mod's callback (a
 * fastutil BooleanConsumer, taken as it is). A 1.21 screen as 1.21's: the mods extend it and add buttons of their own
 * (Cloth Config's restart screen, JourneyMap's world deletion, Xaero's resets), which 1.12.2's GuiYesNo had no place
 * for.
 */
@SideOnly(Side.CLIENT)
public class ConfirmScreen extends Screen {
	protected final ITextComponent message;
	protected ITextComponent yesButton;
	protected ITextComponent noButton;
	/** The mod's BooleanConsumer (or the Translator's own Consumer of Boolean). */
	protected final Object callback;
	private final List<Button> exitButtons = new ArrayList<>();
	private List<String> lines = new ArrayList<>();
	private int delayTicker;

	public ConfirmScreen(Object callback, ITextComponent title, ITextComponent message) {
		this(callback, title, message, new TextComponentTranslation("gui.yes"), new TextComponentTranslation("gui.no"));
	}

	public ConfirmScreen(Object callback, ITextComponent title, ITextComponent message, ITextComponent yesButton, ITextComponent noButton) {
		super(title);
		this.callback = callback;
		this.message = message;
		this.yesButton = yesButton;
		this.noButton = noButton;
	}

	/** The answer to the callback: a fastutil BooleanConsumer's accept(boolean), or a Consumer of Boolean. */
	@SuppressWarnings("unchecked")
	protected void answer(boolean value) {
		if (this.callback instanceof java.util.function.Consumer<?> consumer) {
			((java.util.function.Consumer<Boolean>) consumer).accept(value);
			return;
		}
		if (this.callback == null) {
			return;
		}
		try {
			Method accept = this.callback.getClass().getMethod("accept", boolean.class);
			accept.setAccessible(true);
			accept.invoke(this.callback, value);
		} catch (java.lang.reflect.InvocationTargetException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + " callback", e.getCause());
		} catch (ReflectiveOperationException | RuntimeException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + " callback", e);
		}
	}

	@Override
	protected void init() {
		super.init();
		this.lines = this.font.listFormattedStringToWidth(this.message.getFormattedText(), this.width - 50);
		int y = Math.max(this.height / 6 + 96, Math.min(this.messageTop() + this.messageHeight() + 20, this.height - 24));
		this.exitButtons.clear();
		this.addButtons(y);
	}

	/** 1.21 {@code addButtons(y)}: yes and no side by side (a mod's override adds its own). */
	protected void addButtons(int y) {
		this.addExitButton(Button.builder(this.yesButton, button -> this.answer(true)).bounds(this.width / 2 - 155, y, 150, 20).build());
		this.addExitButton(Button.builder(this.noButton, button -> this.answer(false)).bounds(this.width / 2 - 155 + 160, y, 150, 20).build());
	}

	/** 1.21 {@code addExitButton(button)}: a button the delay holds back. */
	protected void addExitButton(Button button) {
		this.exitButtons.add(this.addRenderableWidget(button));
	}

	private int titleTop() {
		int top = (this.height - this.messageHeight()) / 2;
		return Math.max(10, Math.min(top - 20 - 9, 80));
	}

	private int messageTop() {
		return this.titleTop() + 20;
	}

	private int messageHeight() {
		return this.lines.size() * 9;
	}

	@Override
	public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
		super.render(gfx, mouseX, mouseY, partialTick);
		gfx.drawCenteredString(this.font, this.title, this.width / 2, this.titleTop(), 0xFFFFFF);
		int y = this.messageTop();
		for (String line : this.lines) {
			gfx.drawCenteredString(this.font, line, this.width / 2, y, 0xFFFFFF);
			y += 9;
		}
	}

	/** 1.21 {@code setDelay(ticks)}: the buttons wait that long. */
	public void setDelay(int ticks) {
		this.delayTicker = ticks;
		for (Button button : this.exitButtons) {
			button.active = false;
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (this.delayTicker > 0 && --this.delayTicker == 0) {
			for (Button button : this.exitButtons) {
				button.active = true;
			}
		}
	}

	@Override
	public boolean shouldCloseOnEsc() {
		return false;
	}

	/** Escape answers no (GLFW 256). */
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (keyCode == 256) {
			this.answer(false);
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Nullable
	public ITextComponent getMessage() {
		return this.message;
	}
}
