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
package io.github.xponer2.translator.world;

import javax.annotation.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;

/**
 * 1.21 {@code GameRules.Value} ({@code gameRules.getRule(key)}): a rule of the world's 1.12.2 game rules, which keeps
 * them as strings.
 */
public class GameRuleValue {
	protected final GameRules rules;
	protected final String id;

	protected GameRuleValue(GameRules rules, String id) {
		this.rules = rules;
		this.id = id;
	}

	/** The value of the rule as its kind: a boolean rule ("true"/"false") or a number. */
	public static GameRuleValue of(GameRules rules, GameRuleKey key) {
		if (!rules.hasRule(key.getId()) && key.defaultValue() != null) {
			// a 1.21 rule 1.12.2 hasn't: the world gets it with its 1.21 default (and /gamerule can change it)
			rules.setOrCreateGameRule(key.getId(), key.defaultValue());
		}
		String value = rules.getString(key.getId());
		return "true".equals(value) || "false".equals(value) ? new BooleanValue(rules, key.getId()) : new IntegerValue(rules, key.getId());
	}

	public String serialize() {
		return this.rules.getString(this.id);
	}

	@Override
	public String toString() {
		return this.serialize();
	}

	/** 1.21 {@code GameRules.BooleanValue}. */
	public static class BooleanValue extends GameRuleValue {
		BooleanValue(GameRules rules, String id) {
			super(rules, id);
		}

		public boolean get() {
			return this.rules.getBoolean(this.id);
		}

		public void set(boolean value, @Nullable MinecraftServer server) {
			this.rules.setOrCreateGameRule(this.id, String.valueOf(value));
		}

		public int getCommandResult() {
			return this.get() ? 1 : 0;
		}
	}

	/** 1.21 {@code GameRules.IntegerValue}. */
	public static class IntegerValue extends GameRuleValue {
		IntegerValue(GameRules rules, String id) {
			super(rules, id);
		}

		public int get() {
			return this.rules.getInt(this.id);
		}

		public void set(int value, @Nullable MinecraftServer server) {
			this.rules.setOrCreateGameRule(this.id, String.valueOf(value));
		}

		public int getCommandResult() {
			return this.get();
		}
	}
}
