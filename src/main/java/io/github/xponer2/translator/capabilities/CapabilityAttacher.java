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
package io.github.xponer2.translator.capabilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.BlockEntityType;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Attaches the providers of {@link RegisterCapabilitiesEvent} to the 1.12.2 tile entities (by tile entity type)
 * and stacks (by item). Register an instance on the Forge event bus in pre-init, call {@link #registerAll} in init.
 */
public final class CapabilityAttacher {
	private static final ResourceLocation KEY = new ResourceLocation("cucumber", "capabilities");
	private static final List<BlockEntry<?, ?, ?>> BLOCK_ENTRIES = new ArrayList<>();
	private static final Map<Class<?>, List<BlockEntry<?, ?, ?>>> BY_TILE_CLASS = new HashMap<>();
	private static final Map<Item, List<ItemEntry<?, ?>>> BY_ITEM = new IdentityHashMap<>();
	private static final List<EntityEntry<?, ?>> ENTITY_ENTRIES = new ArrayList<>();
	private static boolean registered;

	public static void registerAll() {
		if (!registered) {
			registered = true;
			MinecraftForge.EVENT_BUS.post(new RegisterCapabilitiesEvent());
		}
	}

	/**
	 * A registration with no capability or provider (a mod's capability field left null by a part the Translator
	 * lacks): left out and logged once. Kept, it failed for every stack of the items, even drawing a dropped one.
	 */
	private static boolean missing(@Nullable Object capability, @Nullable Object provider, Object target) {
		if (capability != null && provider != null) {
			return false;
		}
		io.github.xponer2.translator.runtime.RuntimeStubs.failed("a capability of " + target + " (" + (capability == null ? "no capability" : "no provider")
				+ ")", new IllegalStateException("registered with " + (capability == null ? "a null capability" : "a null provider")));
		return true;
	}

	static synchronized <T, C, BE extends TileEntity> void addBlockEntity(BlockCapability<T, C> capability, BlockEntityType<BE> type,
			ICapabilityProvider<? super BE, C, T> provider) {
		if (missing(capability, provider, type)) {
			return;
		}
		BLOCK_ENTRIES.add(new BlockEntry<>(capability, type, provider));
		BY_TILE_CLASS.clear();
	}

	/** A provider for the tile entities of a class (NeoForge registerBlock: the tile entity class of the blocks). */
	@SuppressWarnings({"unchecked", "rawtypes"})
	static synchronized <T, C> void addTileClass(BlockCapability<T, C> capability, Class<? extends TileEntity> tileClass,
			ICapabilityProvider<TileEntity, C, T> provider) {
		if (missing(capability, provider, tileClass.getName())) {
			return;
		}
		BLOCK_ENTRIES.add(new BlockEntry(capability, tileClass, provider));
		BY_TILE_CLASS.clear();
	}

	static synchronized <T, C> void addItem(ItemCapability<T, C> capability, Item item, ICapabilityProvider<ItemStack, C, T> provider) {
		if (item == null || missing(capability, provider, "the items")) {
			return;
		}
		BY_ITEM.computeIfAbsent(item, i -> new ArrayList<>()).add(new ItemEntry<>(capability, provider));
	}

	/** The entity class of a 1.21 type: the player has no 1.12.2 entity entry, other types without one match nothing. */
	@Nullable
	private static Class<?> entityClassOf(io.github.xponer2.translator.registry.EntityType<?> type) {
		if (type.entry() != null) {
			return type.getEntityClass();
		}
		var key = io.github.xponer2.translator.registry.EntityType.getKey(type);
		return key != null && "minecraft".equals(key.getNamespace()) && "player".equals(key.getPath())
				? net.minecraft.entity.player.EntityPlayer.class : null;
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	static synchronized <T, C> void addEntity(EntityCapability<T, C> capability, io.github.xponer2.translator.registry.EntityType<?> type,
			ICapabilityProvider provider) {
		if (type == null || missing(capability, provider, type)) {
			return;
		}
		Class<?> entityClass = entityClassOf(type);
		if (entityClass != null) {
			ENTITY_ENTRIES.add(new EntityEntry<>(capability, entityClass, provider));
		}
	}

	@SubscribeEvent
	public void onAttachEntity(AttachCapabilitiesEvent<net.minecraft.entity.Entity> event) {
		var entries = new ArrayList<EntityEntry<?, ?>>();
		synchronized (CapabilityAttacher.class) {
			for (var entry : ENTITY_ENTRIES) {
				if (entry.entityClass.isInstance(event.getObject())) {
					entries.add(entry);
				}
			}
		}
		if (!entries.isEmpty()) {
			event.addCapability(KEY, new EntityProvider(event.getObject(), entries));
		}
	}

	private static synchronized List<BlockEntry<?, ?, ?>> entriesOf(Class<?> tileClass) {
		return BY_TILE_CLASS.computeIfAbsent(tileClass, c -> {
			var list = new ArrayList<BlockEntry<?, ?, ?>>();
			for (var entry : BLOCK_ENTRIES) {
				if (entry.tileClass == c) {
					list.add(entry);
				}
			}
			return list;
		});
	}

	@SubscribeEvent
	public void onAttachTileEntity(AttachCapabilitiesEvent<TileEntity> event) {
		var entries = entriesOf(event.getObject().getClass());
		if (!entries.isEmpty()) {
			event.addCapability(KEY, new TileProvider(event.getObject(), entries));
		}
	}

	@SubscribeEvent
	public void onAttachItemStack(AttachCapabilitiesEvent<ItemStack> event) {
		List<ItemEntry<?, ?>> entries;
		synchronized (CapabilityAttacher.class) {
			entries = BY_ITEM.get(event.getObject().getItem());
		}
		if (entries != null) {
			event.addCapability(KEY, new StackProvider(event.getObject(), entries));
		}
	}

	private static final class BlockEntry<T, C, BE extends TileEntity> {
		final BlockCapability<T, C> capability;
		final Class<?> tileClass;
		final ICapabilityProvider<? super BE, C, T> provider;

		BlockEntry(BlockCapability<T, C> capability, BlockEntityType<BE> type, ICapabilityProvider<? super BE, C, T> provider) {
			this(capability, type.getTileClass(), provider);
		}

		BlockEntry(BlockCapability<T, C> capability, Class<?> tileClass, ICapabilityProvider<? super BE, C, T> provider) {
			this.capability = capability;
			this.tileClass = tileClass;
			this.provider = provider;
		}
	}

	private static final class ItemEntry<T, C> {
		final ItemCapability<T, C> capability;
		final ICapabilityProvider<ItemStack, C, T> provider;

		ItemEntry(ItemCapability<T, C> capability, ICapabilityProvider<ItemStack, C, T> provider) {
			this.capability = capability;
			this.provider = provider;
		}
	}

	private static final class EntityEntry<T, C> {
		final EntityCapability<T, C> capability;
		final Class<?> entityClass;
		final ICapabilityProvider<net.minecraft.entity.Entity, C, T> provider;

		EntityEntry(EntityCapability<T, C> capability, Class<?> entityClass, ICapabilityProvider<net.minecraft.entity.Entity, C, T> provider) {
			this.capability = capability;
			this.entityClass = entityClass;
			this.provider = provider;
		}
	}

	private static final class EntityProvider implements net.minecraftforge.common.capabilities.ICapabilityProvider {
		private final net.minecraft.entity.Entity entity;
		private final List<EntityEntry<?, ?>> entries;

		EntityProvider(net.minecraft.entity.Entity entity, List<EntityEntry<?, ?>> entries) {
			this.entity = entity;
			this.entries = entries;
		}

		@Override
		public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
			return this.getCapability(capability, facing) != null;
		}

		@Override
		@Nullable
		@SuppressWarnings({"unchecked", "rawtypes"})
		public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
			for (var entry : this.entries) {
				if (entry.capability.forge() == capability) {
					Object value;
					try {
						value = ((ICapabilityProvider) entry.provider).getCapability(this.entity, facing);
					} catch (RuntimeException | LinkageError e) {
						io.github.xponer2.translator.runtime.RuntimeStubs.failed(entry.provider.getClass().getName() + " (a capability provider)", e);
						continue;
					}
					if (value != null) {
						return (T) value;
					}
				}
			}
			return null;
		}
	}

	private static final class TileProvider implements net.minecraftforge.common.capabilities.ICapabilityProvider {
		private final TileEntity tile;
		private final List<BlockEntry<?, ?, ?>> entries;

		TileProvider(TileEntity tile, List<BlockEntry<?, ?, ?>> entries) {
			this.tile = tile;
			this.entries = entries;
		}

		@Override
		public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
			return this.getCapability(capability, facing) != null;
		}

		@Override
		@Nullable
		@SuppressWarnings({"unchecked", "rawtypes"})
		public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
			for (var entry : this.entries) {
				if (entry.capability.forge() == capability) {
					Object value;
					try {
						value = ((ICapabilityProvider) entry.provider).getCapability(this.tile, facing);
					} catch (RuntimeException | LinkageError e) {
						// asked while the game draws or ticks: logged once, as if the provider had nothing
						io.github.xponer2.translator.runtime.RuntimeStubs.failed(entry.provider.getClass().getName() + " (a capability provider)", e);
						continue;
					}
					if (value != null) {
						return (T) value;
					}
				}
			}
			return null;
		}
	}

	private static final class StackProvider implements net.minecraftforge.common.capabilities.ICapabilityProvider {
		private final ItemStack stack;
		private final List<ItemEntry<?, ?>> entries;

		StackProvider(ItemStack stack, List<ItemEntry<?, ?>> entries) {
			this.stack = stack;
			this.entries = entries;
		}

		@Override
		public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
			return this.getCapability(capability, facing) != null;
		}

		@Override
		@Nullable
		@SuppressWarnings({"unchecked", "rawtypes"})
		public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
			for (var entry : this.entries) {
				if (entry.capability.forge() == capability) {
					Object value;
					try {
						value = ((ICapabilityProvider) entry.provider).getCapability(this.stack, null);
					} catch (RuntimeException | LinkageError e) {
						// asked while the game draws or ticks: logged once, as if the provider had nothing
						io.github.xponer2.translator.runtime.RuntimeStubs.failed(entry.provider.getClass().getName() + " (a capability provider)", e);
						continue;
					}
					if (value != null) {
						return (T) value;
					}
				}
			}
			return null;
		}
	}
}
