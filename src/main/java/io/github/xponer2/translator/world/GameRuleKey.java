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
	public static final GameRuleKey RULE_DO_IMMEDIATE_RESPAWN = new GameRuleKey("doImmediateRespawn", false, 0);
	public static final GameRuleKey RULE_DROWNING_DAMAGE = new GameRuleKey("drowningDamage", true, 0);
	public static final GameRuleKey RULE_FALL_DAMAGE = new GameRuleKey("fallDamage", true, 0);
	public static final GameRuleKey RULE_FIRE_DAMAGE = new GameRuleKey("fireDamage", true, 0);
	public static final GameRuleKey RULE_DOINSOMNIA = new GameRuleKey("doInsomnia", true, 0);
	// 1.21's rules 1.12.2 hasn't: read as their 1.21 defaults (Iron's Spells' anger reset read universalAnger as true)
	public static final GameRuleKey RULE_UNIVERSAL_ANGER = new GameRuleKey("universalAnger", false, 0);
	public static final GameRuleKey RULE_FORGIVE_DEAD_PLAYERS = new GameRuleKey("forgiveDeadPlayers", true, 0);
	public static final GameRuleKey RULE_FREEZE_DAMAGE = new GameRuleKey("freezeDamage", true, 0);
	public static final GameRuleKey RULE_DO_PATROL_SPAWNING = new GameRuleKey("doPatrolSpawning", true, 0);
	public static final GameRuleKey RULE_DO_TRADER_SPAWNING = new GameRuleKey("doTraderSpawning", true, 0);
	public static final GameRuleKey RULE_DO_WARDEN_SPAWNING = new GameRuleKey("doWardenSpawning", true, 0);
	public static final GameRuleKey RULE_DISABLE_RAIDS = new GameRuleKey("disableRaids", false, 0);
	public static final GameRuleKey RULE_BLOCK_EXPLOSION_DROP_DECAY = new GameRuleKey("blockExplosionDropDecay", true, 0);
	public static final GameRuleKey RULE_MOB_EXPLOSION_DROP_DECAY = new GameRuleKey("mobExplosionDropDecay", true, 0);
	public static final GameRuleKey RULE_TNT_EXPLOSION_DROP_DECAY = new GameRuleKey("tntExplosionDropDecay", false, 0);
	public static final GameRuleKey RULE_WATER_SOURCE_CONVERSION = new GameRuleKey("waterSourceConversion", true, 0);
	public static final GameRuleKey RULE_LAVA_SOURCE_CONVERSION = new GameRuleKey("lavaSourceConversion", false, 0);
	public static final GameRuleKey RULE_GLOBAL_SOUND_EVENTS = new GameRuleKey("globalSoundEvents", true, 0);
	public static final GameRuleKey RULE_DO_VINES_SPREAD = new GameRuleKey("doVinesSpread", true, 0);
	public static final GameRuleKey RULE_ENDER_PEARLS_VANISH_ON_DEATH = new GameRuleKey("enderPearlsVanishOnDeath", true, 0);
	public static final GameRuleKey RULE_SNOW_ACCUMULATION_HEIGHT = new GameRuleKey("snowAccumulationHeight", true, 1);
	public static final GameRuleKey RULE_PLAYERS_SLEEPING_PERCENTAGE = new GameRuleKey("playersSleepingPercentage", true, 100);
	public static final GameRuleKey RULE_SPAWN_CHUNK_RADIUS = new GameRuleKey("spawnChunkRadius", true, 2);
	public static final GameRuleKey RULE_MAX_COMMAND_FORK_COUNT = new GameRuleKey("maxCommandForkCount", true, 65536);
	public static final GameRuleKey RULE_COMMAND_MODIFICATION_BLOCK_LIMIT = new GameRuleKey("commandModificationBlockLimit", true, 32768);
	public static final GameRuleKey RULE_PLAYERS_NETHER_PORTAL_DEFAULT_DELAY = new GameRuleKey("playersNetherPortalDefaultDelay", true, 80);
	public static final GameRuleKey RULE_PLAYERS_NETHER_PORTAL_CREATIVE_DELAY = new GameRuleKey("playersNetherPortalCreativeDelay", true, 1);

	private final String id;
	/** The value of a rule the world doesn't have (1.21's default; true for the switches of mods' own rules). */
	private final boolean defaultBoolean;
	private final int defaultInt;
	/** Whether the defaults are 1.21's own (its vanilla rules); a mod's rule's aren't known. */
	private final boolean known;

	public GameRuleKey(String id) {
		this.id = id;
		this.defaultBoolean = true;
		this.defaultInt = 0;
		this.known = false;
	}

	private GameRuleKey(String id, boolean defaultBoolean, int defaultInt) {
		this.id = id;
		this.defaultBoolean = defaultBoolean;
		this.defaultInt = defaultInt;
		this.known = true;
	}

	/** The 1.21 default of a vanilla rule 1.12.2 hasn't, as the world keeps it (a number rule's has a number); null if unknown. */
	@javax.annotation.Nullable
	String defaultValue() {
		return !this.known ? null : this.defaultInt != 0 ? String.valueOf(this.defaultInt) : String.valueOf(this.defaultBoolean);
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
	/** 1.21 {@code gameRules.getRule(key)}: the rule's value object. */
	public static GameRuleValue getRule(GameRules rules, GameRuleKey key) {
		return GameRuleValue.of(rules, key);
	}

	public static boolean getBoolean(GameRules rules, GameRuleKey key) {
		return rules.hasRule(key.id) ? rules.getBoolean(key.id) : key.defaultBoolean;
	}

	public static int getInt(GameRules rules, GameRuleKey key) {
		return rules.hasRule(key.id) ? rules.getInt(key.id) : key.defaultInt;
	}
}
