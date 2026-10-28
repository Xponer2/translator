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
package io.github.xponer2.translator.entity;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import net.minecraft.util.ResourceLocation;

/**
 * 1.21 attribute modifier (identified by a ResourceLocation) on the 1.12.2 one (identified by a UUID, made from the
 * id so the same id always gives the same UUID).
 */
public final class AttributeModifier {
	private final ResourceLocation id;
	private final double amount;
	private final Operation operation;

	public AttributeModifier(ResourceLocation id, double amount, Operation operation) {
		this.id = id;
		this.amount = amount;
		this.operation = operation;
	}

	public ResourceLocation id() {
		return this.id;
	}

	public double amount() {
		return this.amount;
	}

	public Operation operation() {
		return this.operation;
	}

	public static UUID uuidOf(ResourceLocation id) {
		return UUID.nameUUIDFromBytes(id.toString().getBytes(StandardCharsets.UTF_8));
	}

	public net.minecraft.entity.ai.attributes.AttributeModifier forge() {
		return new net.minecraft.entity.ai.attributes.AttributeModifier(uuidOf(this.id), this.id.toString(), this.amount, this.operation.ordinal())
				.setSaved(false);
	}

	public enum Operation {
		ADD_VALUE,
		ADD_MULTIPLIED_BASE,
		ADD_MULTIPLIED_TOTAL
	}
}
