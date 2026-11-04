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

/** 1.21 {@code Rect2i}: a rectangle of screen pixels (JEI's exclusion areas, the screens of many mods). */
public class Rect2i {
	private int xPos;
	private int yPos;
	private int width;
	private int height;

	public Rect2i(int x, int y, int width, int height) {
		this.xPos = x;
		this.yPos = y;
		this.width = width;
		this.height = height;
	}

	public Rect2i intersect(Rect2i other) {
		int x0 = Math.max(this.xPos, other.xPos);
		int y0 = Math.max(this.yPos, other.yPos);
		int x1 = Math.min(this.xPos + this.width, other.xPos + other.width);
		int y1 = Math.min(this.yPos + this.height, other.yPos + other.height);
		this.xPos = x0;
		this.yPos = y0;
		this.width = Math.max(0, x1 - x0);
		this.height = Math.max(0, y1 - y0);
		return this;
	}

	public int getX() {
		return this.xPos;
	}

	public int getY() {
		return this.yPos;
	}

	public void setX(int x) {
		this.xPos = x;
	}

	public void setY(int y) {
		this.yPos = y;
	}

	public int getWidth() {
		return this.width;
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

	public void setPosition(int x, int y) {
		this.xPos = x;
		this.yPos = y;
	}

	public boolean contains(int x, int y) {
		return x >= this.xPos && x <= this.xPos + this.width && y >= this.yPos && y <= this.yPos + this.height;
	}
}
