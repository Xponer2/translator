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

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.EntityType;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;

/** What the entity facades over 1.12.2's concrete entities share. */
final class MobFacades {
	private MobFacades() {
	}

	/** The entity's type: the one it was made with, else the one registered for its class. */
	@Nullable
	static EntityType<?> type(@Nullable EntityType<?> type, Entity entity) {
		return type != null ? type : EntityType.of(entity);
	}

	/** Drops the mob's 1.21 loot table (entities/<type>); false when it has none (1.12.2's drops then). */
	static boolean dropLoot(EntityLiving mob, @Nullable EntityType<?> type, boolean recentlyHit, int looting, @Nullable DamageSource source) {
		ResourceLocation id = type == null ? null : EntityType.getKey(type);
		ResourceLocation table = id == null ? null : new ResourceLocation(id.getNamespace(), "entities/" + id.getPath());
		boolean byPlayer = recentlyHit && source != null && source.getTrueSource() instanceof EntityPlayer;
		return io.github.xponer2.translator.loot.BlockLoot.entityDrops(table, mob, byPlayer, looting, stack -> mob.entityDropItem(stack, 0.0F));
	}
}
