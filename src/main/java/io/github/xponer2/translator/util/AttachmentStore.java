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
package io.github.xponer2.translator.util;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The NeoForge data attachments of a 1.12.2 entity, block entity, world or chunk, kept in the holder itself (a
 * capability): they go with it. In a map keyed weakly by the holder, an attachment that refers to its holder (Iron's
 * Spells' magic data of every mob) kept the holder forever: every mob ever made stayed in memory.
 */
public final class AttachmentStore {
	@CapabilityInject(AttachmentStore.class)
	@Nullable
	public static Capability<AttachmentStore> CAPABILITY = null;
	private static final ResourceLocation ID = new ResourceLocation("translator", "attachments");

	final Map<Object, Object> values = new HashMap<>();

	/** Registers the capability and attaches a store to every holder; in pre-init, before any holder is made. */
	public static void register() {
		CapabilityManager.INSTANCE.register(AttachmentStore.class, new Capability.IStorage<AttachmentStore>() {
			@Override
			@Nullable
			public NBTBase writeNBT(Capability<AttachmentStore> capability, AttachmentStore instance, EnumFacing side) {
				return null;
			}

			@Override
			public void readNBT(Capability<AttachmentStore> capability, AttachmentStore instance, EnumFacing side, NBTBase nbt) {
			}
		}, AttachmentStore::new);
		MinecraftForge.EVENT_BUS.register(new Events());
	}

	/** The store of a holder, null when it has none (not a capability holder, or made before the registration). */
	@Nullable
	static Map<Object, Object> of(Object holder) {
		if (CAPABILITY != null && holder instanceof ICapabilityProvider provider) {
			try {
				AttachmentStore store = provider.getCapability(CAPABILITY, null);
				return store == null ? null : store.values;
			} catch (RuntimeException e) {
				return null;
			}
		}
		return null;
	}

	public static final class Events {
		@SubscribeEvent
		public void entity(AttachCapabilitiesEvent<Entity> event) {
			event.addCapability(ID, new Provider());
		}

		@SubscribeEvent
		public void tile(AttachCapabilitiesEvent<TileEntity> event) {
			event.addCapability(ID, new Provider());
		}

		@SubscribeEvent
		public void world(AttachCapabilitiesEvent<World> event) {
			event.addCapability(ID, new Provider());
		}

		@SubscribeEvent
		public void chunk(AttachCapabilitiesEvent<Chunk> event) {
			event.addCapability(ID, new Provider());
		}
	}

	private static final class Provider implements ICapabilityProvider {
		private final AttachmentStore store = new AttachmentStore();

		@Override
		public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
			return capability == CAPABILITY;
		}

		@Override
		@Nullable
		public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
			return capability == CAPABILITY ? CAPABILITY.cast(this.store) : null;
		}
	}
}
