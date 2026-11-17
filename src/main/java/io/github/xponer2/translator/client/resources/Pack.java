/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package io.github.xponer2.translator.client.resources;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

/** 1.21 {@code Pack}: a resource pack of the game's list (its id, title and description). */
public final class Pack {
	private final String id;
	private final ITextComponent title;
	private final ITextComponent description;
	private final boolean required;

	Pack(String id, String title, String description, boolean required) {
		this.id = id;
		this.title = new TextComponentString(title);
		this.description = new TextComponentString(description);
		this.required = required;
	}

	public String getId() {
		return this.id;
	}

	public ITextComponent getTitle() {
		return this.title;
	}

	public ITextComponent getDescription() {
		return this.description;
	}

	public boolean isRequired() {
		return this.required;
	}

	public boolean isFixedPosition() {
		return this.required;
	}

	@Override
	public String toString() {
		return this.id;
	}
}
