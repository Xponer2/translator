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

import net.minecraft.world.GameRules;

/**
 * A game rule by name (1.21 {@code GameRules.Key}, neoforge-1.21.1-runtime.tsv): the 1.12.2 rule of that name. The
 * vanilla keys (GameRules.RULE_KEEPINVENTORY, ...) are fields here (neoforge-1.21.1-redirects.tsv), and so are the
 * reads of a rule by key.
 */
public final class GameRuleKey {
	public static final GameRuleKey RULE_DOFIRETICK = new GameRuleKey("doFireTick");
	public static final GameRuleKey RULE_MOBGRIEFING = new GameRuleKey("mobGriefing");
	public static final GameRuleKey RULE_KEEPINVENTORY = new GameRuleKey("keepInventory");
	public static final GameRuleKey RULE_DOMOBSPAWNING = new GameRuleKey("doMobSpawning");
	public static final GameRuleKey RULE_DOMOBLOOT = new GameRuleKey("doMobLoot");
	public static final GameRuleKey RULE_DOBLOCKDROPS = new GameRuleKey("doTileDrops");
	public static final GameRuleKey RULE_DOENTITYDROPS = new GameRuleKey("doEntityDrops");
	public static final GameRuleKey RULE_COMMANDBLOCKOUTPUT = new GameRuleKey("commandBlockOutput");
	public static final GameRuleKey RULE_NATURAL_REGENERATION = new GameRuleKey("naturalRegeneration");
	public static final GameRuleKey RULE_DAYLIGHT = new GameRuleKey("doDaylightCycle");
	public static final GameRuleKey RULE_LOGADMINCOMMANDS = new GameRuleKey("logAdminCommands");
	public static final GameRuleKey RULE_SHOWDEATHMESSAGES = new GameRuleKey("showDeathMessages");
	public static final GameRuleKey RULE_RANDOMTICKING = new GameRuleKey("randomTickSpeed");
	public static final GameRuleKey RULE_SENDCOMMANDFEEDBACK = new GameRuleKey("sendCommandFeedback");
	public static final GameRuleKey RULE_REDUCEDDEBUGINFO = new GameRuleKey("reducedDebugInfo");
	public static final GameRuleKey RULE_SPECTATORSGENERATECHUNKS = new GameRuleKey("spectatorsGenerateChunks");
	public static final GameRuleKey RULE_SPAWN_RADIUS = new GameRuleKey("spawnRadius");
	public static final GameRuleKey RULE_DISABLE_ELYTRA_MOVEMENT_CHECK = new GameRuleKey("disableElytraMovementCheck");
	public static final GameRuleKey RULE_MAX_ENTITY_CRAMMING = new GameRuleKey("maxEntityCramming");
	public static final GameRuleKey RULE_WEATHER_CYCLE = new GameRuleKey("doWeatherCycle");
	public static final GameRuleKey RULE_ANNOUNCE_ADVANCEMENTS = new GameRuleKey("announceAdvancements");
	public static final GameRuleKey RULE_MAX_COMMAND_CHAIN_LENGTH = new GameRuleKey("maxCommandChainLength");
	public static final GameRuleKey RULE_LIMITED_CRAFTING = new GameRuleKey("doLimitedCrafting");
	public static final GameRuleKey RULE_DO_IMMEDIATE_RESPAWN = new GameRuleKey("doImmediateRespawn");
	public static final GameRuleKey RULE_DROWNING_DAMAGE = new GameRuleKey("drowningDamage");
	public static final GameRuleKey RULE_FALL_DAMAGE = new GameRuleKey("fallDamage");
	public static final GameRuleKey RULE_FIRE_DAMAGE = new GameRuleKey("fireDamage");
	public static final GameRuleKey RULE_DOINSOMNIA = new GameRuleKey("doInsomnia");

	private final String id;

	public GameRuleKey(String id) {
		this.id = id;
	}

	/** A mod's rule (1.21 {@code new GameRules.Key(name, category)}): read by name like the vanilla ones. */
	public GameRuleKey(String id, Object category) {
		this(id);
	}

	public Object getCategory() {
		return null;
	}

	public String getId() {
		return this.id;
	}

	@Override
	public String toString() {
		return this.id;
	}

	/**
	 * A mod's rule (1.21 {@code GameRules.register(name, category, type)}): its key; the rule is read by name (the
	 * 1.21 default when the world doesn't have it).
	 */
	public static GameRuleKey register(String name, Object category, Object type) {
		return new GameRuleKey(name, category);
	}

	/** A rule 1.12.2 doesn't have is the 1.21 default: true for the switches (fall damage, ...). */
	public static boolean getBoolean(GameRules rules, GameRuleKey key) {
		return rules.hasRule(key.id) ? rules.getBoolean(key.id) : true;
	}

	public static int getInt(GameRules rules, GameRuleKey key) {
		return rules.hasRule(key.id) ? rules.getInt(key.id) : 0;
	}
}
