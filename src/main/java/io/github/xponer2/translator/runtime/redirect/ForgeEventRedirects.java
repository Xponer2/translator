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
package io.github.xponer2.translator.runtime.redirect;

import io.github.xponer2.translator.runtime.neoforge.ItemEntityPickupEvent;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;

/**
 * The NeoForge 1.21 getters of events the 1.12.2 Forge events name differently (runtime): getEntity() of player and
 * living events, getLevel() of interaction and block events; the {@code EventHooks} fire methods the Translator's
 * EventHooks lacks.
 */
public final class ForgeEventRedirects {
	private ForgeEventRedirects() {
	}

	// --- PlayerInteractEvent.RightClickBlock / LeftClickBlock --------------------------------------------------------

	/** The 1.21 block hit (1.12.2 gives the hit point, the face and the position apart). */
	public static net.minecraft.util.math.RayTraceResult getHitVec(PlayerInteractEvent.RightClickBlock event) {
		var hit = event.getHitVec() != null ? event.getHitVec()
				: new net.minecraft.util.math.Vec3d(event.getPos().getX() + 0.5, event.getPos().getY() + 0.5, event.getPos().getZ() + 0.5);
		return new net.minecraft.util.math.RayTraceResult(hit, event.getFace() != null ? event.getFace() : net.minecraft.util.EnumFacing.UP, event.getPos());
	}

	public static void setUseBlock(PlayerInteractEvent.RightClickBlock event, io.github.xponer2.translator.runtime.neoforge.TriState state) {
		event.setUseBlock(result(state));
	}

	public static void setUseItem(PlayerInteractEvent.RightClickBlock event, io.github.xponer2.translator.runtime.neoforge.TriState state) {
		event.setUseItem(result(state));
	}

	public static io.github.xponer2.translator.runtime.neoforge.TriState getUseItem(PlayerInteractEvent.RightClickBlock event) {
		return triState(event.getUseItem());
	}

	public static io.github.xponer2.translator.runtime.neoforge.TriState getUseBlock(PlayerInteractEvent.RightClickBlock event) {
		return triState(event.getUseBlock());
	}

	private static net.minecraftforge.fml.common.eventhandler.Event.Result result(io.github.xponer2.translator.runtime.neoforge.TriState state) {
		if (state == io.github.xponer2.translator.runtime.neoforge.TriState.TRUE) {
			return net.minecraftforge.fml.common.eventhandler.Event.Result.ALLOW;
		}
		return state == io.github.xponer2.translator.runtime.neoforge.TriState.FALSE ? net.minecraftforge.fml.common.eventhandler.Event.Result.DENY
				: net.minecraftforge.fml.common.eventhandler.Event.Result.DEFAULT;
	}

	private static io.github.xponer2.translator.runtime.neoforge.TriState triState(net.minecraftforge.fml.common.eventhandler.Event.Result result) {
		switch (result) {
		case ALLOW:
			return io.github.xponer2.translator.runtime.neoforge.TriState.TRUE;
		case DENY:
			return io.github.xponer2.translator.runtime.neoforge.TriState.FALSE;
		default:
			return io.github.xponer2.translator.runtime.neoforge.TriState.DEFAULT;
		}
	}

	/** The 1.21 logical side of the event (NeoForge's LogicalSide, carried with the API): its world's side. */
	public static Object getSide(PlayerInteractEvent event) {
		return carriedEnum("net.neoforged.fml.LogicalSide", event.getWorld().isRemote ? "CLIENT" : "SERVER");
	}

	/** The 1.21 action of a left click (START: 1.12.2 fires it when the player starts hitting the block). */
	public static Object getAction(PlayerInteractEvent.LeftClickBlock event) {
		return carriedEnum("net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$LeftClickBlock$Action", "START");
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object carriedEnum(String type, String name) {
		try {
			return Enum.valueOf((Class) Class.forName(type, true, net.minecraft.launchwrapper.Launch.classLoader), name);
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	public static EntityPlayer getEntity(PlayerEvent event) {
		return event.getEntityPlayer();
	}

	public static EntityLivingBase getEntity(LivingEvent event) {
		return event.getEntityLiving();
	}

	public static World getLevel(PlayerInteractEvent event) {
		return event.getWorld();
	}

	public static World getLevel(BlockEvent event) {
		return event.getWorld();
	}

	/** A mount event's world (Ars Nouveau's jar handler read it for every seat Handcrafted's chairs made). */
	public static World getLevel(net.minecraftforge.event.entity.EntityMountEvent event) {
		return event.getWorldObj();
	}

	// --- EventHooks (static) ------------------------------------------------------------------------------------------

	public static ItemEntityPickupEvent.Pre fireItemPickupPre(EntityItem item, EntityPlayer player) {
		return ItemEntityPickupEvent.fire(item, player);
	}
}
