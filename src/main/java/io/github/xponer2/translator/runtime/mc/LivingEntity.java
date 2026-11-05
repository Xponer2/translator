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
package io.github.xponer2.translator.runtime.mc;

import io.github.xponer2.translator.entity.AttributeSupplier;
import io.github.xponer2.translator.entity.SynchedDataBuilder;
import io.github.xponer2.translator.registry.EntityType;

import net.minecraft.world.World;

/**
 * 1.21 {@code LivingEntity} for the mods' living entities that extend it: made with their EntityType (1.12.2's EntityLivingBase takes the
 * world only), their size and attributes from the type. Without it their constructor couldn't run and no such entity
 * existed in the game.
 */
public abstract class LivingEntity extends net.minecraft.entity.EntityLivingBase {
	private final EntityType<?> type;

	protected LivingEntity(EntityType<?> type, World world) {
		super(world);
		this.type = type;
		if (type != null) {
			this.setSize(type.getWidth(), type.getHeight());
			AttributeSupplier.apply(this, type);
		}
	}

	public EntityType<?> getType() {
		return this.type;
	}

	/**
	 * 1.21 {@code defineSynchedData(builder)}: a mod's mob defines its data here (its override keeps the 1.21 name, the
	 * facade has it) and calls it as super. Under the table's name (entityInit) the mod's override was never called and
	 * every mob with data crashed when it ticked (the Aether's aerbunny).
	 */
	protected void defineSynchedData(SynchedDataBuilder builder) {
	}

	/** 1.12.2 defines the data here: the 1.12.2 data first, then the mod's. */
	@Override
	protected void entityInit() {
		super.entityInit();
		this.defineSynchedData(SynchedDataBuilder.of(this));
	}

	/** 1.21 {@code LivingEntity.createLivingAttributes()}. */
	public static AttributeSupplier.Builder createLivingAttributes() {
		return AttributeSupplier.living();
	}

	/** The drops of the mob's 1.21 loot table (entities/<type>), else 1.12.2's. */
	@Override
	protected void dropLoot(boolean recentlyHit, int looting, net.minecraft.util.DamageSource source) {
		net.minecraft.util.ResourceLocation id = this.type == null ? null : EntityType.getKey(this.type);
		net.minecraft.util.ResourceLocation table = id == null ? null : new net.minecraft.util.ResourceLocation(id.getNamespace(), "entities/" + id.getPath());
		boolean byPlayer = recentlyHit && source != null && source.getTrueSource() instanceof net.minecraft.entity.player.EntityPlayer;
		if (!io.github.xponer2.translator.loot.BlockLoot.entityDrops(table, this, byPlayer, looting, stack -> this.entityDropItem(stack, 0.0F))) {
			super.dropLoot(recentlyHit, looting, source);
		}
	}
}
