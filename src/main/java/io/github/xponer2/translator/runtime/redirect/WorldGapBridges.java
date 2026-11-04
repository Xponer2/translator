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

import java.util.Set;

import io.github.xponer2.translator.shape.VoxelShape;
import io.github.xponer2.translator.util.ItemInteractionResult;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;

/**
 * World calls of 1.21.1 mods on 1.12.2 classes: a block state's support and interaction shapes and its use (the
 * block's own 1.21 code when it is a 1.21.1 mod's block, else the 1.12.2 activation), an empty chunk section, a mob's
 * goals.
 */
public final class WorldGapBridges {
	private WorldGapBridges() {
	}

	// --- BlockState -----------------------------------------------------------------------------------------------------

	public static VoxelShape getBlockSupportShape(IBlockState state, IBlockAccess world, BlockPos pos) {
		return ApiBridges.getCollisionShape(state, world, pos);
	}

	public static VoxelShape getInteractionShape(IBlockState state, IBlockAccess world, BlockPos pos) {
		return ApiBridges.getShape(state, world, pos);
	}

	/** 1.21 {@code state.useItemOn(stack, level, player, hand, hit)}. */
	public static ItemInteractionResult useItemOn(IBlockState state, ItemStack stack, World level, EntityPlayer player, EnumHand hand, RayTraceResult hit) {
		BlockPos pos = hit.getBlockPos();
		if (state.getBlock() instanceof io.github.xponer2.translator.runtime.mc.Block block) {
			return block.useItemOn(stack, state, level, pos, player, hand, hit);
		}
		return activate(state, level, pos, player, hand, hit) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
	}

	/** 1.21 {@code state.useWithoutItem(level, player, hit)}. */
	public static EnumActionResult useWithoutItem(IBlockState state, World level, EntityPlayer player, RayTraceResult hit) {
		BlockPos pos = hit.getBlockPos();
		if (state.getBlock() instanceof io.github.xponer2.translator.runtime.mc.Block block) {
			return block.useWithoutItem(state, level, pos, player, hit);
		}
		return activate(state, level, pos, player, EnumHand.MAIN_HAND, hit) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
	}

	private static boolean activate(IBlockState state, World level, BlockPos pos, EntityPlayer player, EnumHand hand, RayTraceResult hit) {
		EnumFacing side = hit.sideHit == null ? EnumFacing.UP : hit.sideHit;
		var vec = hit.hitVec == null ? new net.minecraft.util.math.Vec3d(pos) : hit.hitVec;
		return state.getBlock().onBlockActivated(level, pos, state, player, hand, side, (float) (vec.x - pos.getX()), (float) (vec.y - pos.getY()),
				(float) (vec.z - pos.getZ()));
	}

	public static VoxelShape getFaceOcclusionShape(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face) {
		return state.isOpaqueCube() ? ApiBridges.getShape(state, world, pos).getFaceShape(face) : io.github.xponer2.translator.shape.Shapes.empty();
	}

	public static VoxelShape getVisualShape(IBlockState state, IBlockAccess world, BlockPos pos, io.github.xponer2.translator.shape.CollisionContext context) {
		return ApiBridges.getCollisionShape(state, world, pos);
	}

	// --- entities -------------------------------------------------------------------------------------------------------

	/** 1.21 {@code lightningBolt.setVisualOnly(visual)}: 1.12.2's effect-only bolt (no fire, no damage). */
	public static void setVisualOnly(net.minecraft.entity.effect.EntityLightningBolt bolt, boolean visual) {
		setField(net.minecraft.entity.effect.EntityLightningBolt.class, "field_184529_d", "effectOnly", bolt, visual);
	}

	/** 1.21 {@code arrow.getPierceLevel()}: 1.12.2 arrows don't pierce. */
	public static byte getPierceLevel(net.minecraft.entity.projectile.EntityArrow arrow) {
		return 0;
	}

	/** 1.21 {@code hurtByTargetGoal.setAlertOthers(types)}: the mob calls others for help (1.12.2's constructor flag). */
	public static net.minecraft.entity.ai.EntityAIHurtByTarget setAlertOthers(net.minecraft.entity.ai.EntityAIHurtByTarget goal, Class<?>[] except) {
		setField(net.minecraft.entity.ai.EntityAIHurtByTarget.class, "field_75312_a", "entityCallsForHelp", goal, true);
		return goal;
	}

	// --- structures -----------------------------------------------------------------------------------------------------

	private static final java.util.Map<net.minecraft.world.gen.structure.template.PlacementSettings, BlockPos> PIVOTS = java.util.Collections
			.synchronizedMap(new java.util.WeakHashMap<>());

	/** 1.21 {@code settings.setRotationPivot(pos)}: kept for getRotationPivot (1.12.2 rotates around the origin). */
	public static net.minecraft.world.gen.structure.template.PlacementSettings setRotationPivot(
			net.minecraft.world.gen.structure.template.PlacementSettings settings, BlockPos pivot) {
		PIVOTS.put(settings, pivot);
		return settings;
	}

	public static BlockPos getRotationPivot(net.minecraft.world.gen.structure.template.PlacementSettings settings) {
		return PIVOTS.getOrDefault(settings, BlockPos.ORIGIN);
	}

	/** 1.21 {@code templateManager.getOrCreate(id)}: the 1.12.2 template of that name (empty when there is none). */
	public static net.minecraft.world.gen.structure.template.Template getOrCreate(net.minecraft.world.gen.structure.template.TemplateManager manager,
			net.minecraft.util.ResourceLocation id) {
		var server = net.minecraftforge.fml.common.FMLCommonHandler.instance().getMinecraftServerInstance();
		return manager.getTemplate(server, id);
	}

	// --- ResourceLocation -----------------------------------------------------------------------------------------------

	/**
	 * 1.21 {@code id.compareNamespaced(other)}: by namespace, then path (Mekanism sorts its QIO list with a reference to
	 * it; without one the comparator was null and Mekanism's config, so the whole mod, failed).
	 */
	public static int compareNamespaced(net.minecraft.util.ResourceLocation id, net.minecraft.util.ResourceLocation other) {
		int namespace = id.getNamespace().compareTo(other.getNamespace());
		return namespace != 0 ? namespace : id.getPath().compareTo(other.getPath());
	}

	/**
	 * 1.21 {@code settings.addProcessor(processor)}: 1.12.2 templates take their processor at placement (an
	 * ITemplateProcessor), a 1.21 one isn't applied: the template is placed as it is.
	 */
	public static net.minecraft.world.gen.structure.template.PlacementSettings addProcessor(
			net.minecraft.world.gen.structure.template.PlacementSettings settings, Object processor) {
		return settings;
	}

	// --- NBT, containers, connections ---------------------------------------------------------------------------------

	/** 1.21 {@code tag.sizeInBytes()}: the size the tag takes written (mods keep packets under a limit with it). */
	public static int sizeInBytes(net.minecraft.nbt.NBTBase tag) {
		if (tag instanceof net.minecraft.nbt.NBTTagCompound compound) {
			var bytes = new java.io.ByteArrayOutputStream();
			try (var out = new java.io.DataOutputStream(bytes)) {
				net.minecraft.nbt.CompressedStreamTools.write(compound, out);
				return bytes.size();
			} catch (java.io.IOException e) {
				// a byte array stream doesn't fail
			}
		}
		return tag.toString().length();
	}

	/** 1.21 {@code simpleContainer.getItems()}: the container's own list (changes go to it). */
	@SuppressWarnings("unchecked")
	public static net.minecraft.util.NonNullList<ItemStack> getItems(net.minecraft.inventory.InventoryBasic container) {
		for (String name : new String[] {"field_70482_c", "inventoryContents"}) {
			try {
				var field = net.minecraft.inventory.InventoryBasic.class.getDeclaredField(name);
				field.setAccessible(true);
				return (net.minecraft.util.NonNullList<ItemStack>) field.get(container);
			} catch (ReflectiveOperationException | RuntimeException e) {
				// the other name
			}
		}
		var copy = net.minecraft.util.NonNullList.withSize(container.getSizeInventory(), ItemStack.EMPTY);
		for (int i = 0; i < copy.size(); i++) {
			copy.set(i, container.getStackInSlot(i));
		}
		return copy;
	}

	public static net.minecraft.network.NetworkManager getConnection(net.minecraft.network.NetHandlerPlayServer handler) {
		return handler.getNetworkManager();
	}

	// --- text -----------------------------------------------------------------------------------------------------------

	/** 1.21 {@code style.applyTo(other)}: this style over the other (what this one leaves unset comes from the other). */
	public static net.minecraft.util.text.Style applyTo(net.minecraft.util.text.Style style, net.minecraft.util.text.Style other) {
		var result = style.createShallowCopy();
		result.setParentStyle(other);
		return result;
	}

	// --- Level ----------------------------------------------------------------------------------------------------------

	/** 1.21 {@code level.dimensionTypeRegistration()}: the holder of the world's dimension type. */
	public static io.github.xponer2.translator.registry.Holder<net.minecraft.world.DimensionType> dimensionTypeRegistration(World world) {
		return io.github.xponer2.translator.registry.Holder.direct(world.provider.getDimensionType());
	}

	private static void setField(Class<?> owner, String srg, String mcp, Object target, Object value) {
		try {
			java.lang.reflect.Field field;
			try {
				field = owner.getDeclaredField(mcp);
			} catch (NoSuchFieldException e) {
				field = owner.getDeclaredField(srg);
			}
			field.setAccessible(true);
			java.lang.reflect.Field modifiers = java.lang.reflect.Field.class.getDeclaredField("modifiers");
			modifiers.setAccessible(true);
			modifiers.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
			field.set(target, value);
		} catch (ReflectiveOperationException e) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("setting " + owner.getSimpleName() + "." + mcp, e);
		}
	}

	/** 1.21 {@code level.dayTime()}: the world time of day (ticks). */
	public static long dayTime(World world) {
		return world.getWorldTime();
	}

	public static int getLogicalHeight(World world) {
		return world.getActualHeight();
	}

	// --- ServerChunkCache -----------------------------------------------------------------------------------------------

	@javax.annotation.Nullable
	public static net.minecraft.world.chunk.Chunk getChunkNow(net.minecraft.world.gen.ChunkProviderServer chunks, int x, int z) {
		return chunks.getLoadedChunk(x, z);
	}

	/** 1.21 {@code isPositionTicking(chunkPos)}: the chunk (packed x, z) is loaded. */
	public static boolean isPositionTicking(net.minecraft.world.gen.ChunkProviderServer chunks, long chunkPos) {
		return chunks.chunkExists((int) chunkPos, (int) (chunkPos >> 32));
	}

	// --- containers -----------------------------------------------------------------------------------------------------

	public static boolean hasAnyMatching(net.minecraft.inventory.IInventory inventory, java.util.function.Predicate<ItemStack> test) {
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (!stack.isEmpty() && test.test(stack)) {
				return true;
			}
		}
		return false;
	}

	/** 1.21 {@code container.canTakeItem(target, slot, stack)}: 1.12.2 inventories don't refuse it. */
	public static boolean canTakeItem(net.minecraft.inventory.IInventory inventory, net.minecraft.inventory.IInventory target, int slot, ItemStack stack) {
		return true;
	}

	// --- damage, food, attributes ---------------------------------------------------------------------------------------

	/** 1.21 {@code source.isDirect()}: the entity that hit is the one that caused it (no projectile between). */
	public static boolean isDirect(net.minecraft.util.DamageSource source) {
		return source.getImmediateSource() == source.getTrueSource();
	}

	public static void setExhaustion(net.minecraft.util.FoodStats food, float exhaustion) {
		setField(net.minecraft.util.FoodStats.class, "field_75126_c", "foodExhaustionLevel", food, exhaustion);
	}

	public static net.minecraft.nbt.NBTTagCompound save(net.minecraft.entity.ai.attributes.AttributeModifier modifier) {
		return net.minecraft.entity.SharedMonsterAttributes.writeAttributeModifierToNBT(modifier);
	}

	/**
	 * 1.21 {@code stack.forEachModifier(slotGroup, action)}: the stack's attribute modifiers in the slots of the group,
	 * as 1.21 holders and modifiers (1.12.2 gives them by attribute name per slot).
	 */
	public static void forEachModifier(ItemStack stack, io.github.xponer2.translator.entity.EquipmentSlotGroup group,
			java.util.function.BiConsumer<io.github.xponer2.translator.registry.Holder<io.github.xponer2.translator.entity.Attribute>,
					io.github.xponer2.translator.entity.AttributeModifier> action) {
		var seen = new java.util.HashSet<java.util.UUID>();
		for (net.minecraft.inventory.EntityEquipmentSlot slot : net.minecraft.inventory.EntityEquipmentSlot.values()) {
			if (group.test(slot)) {
				modifiers(stack, slot, seen, action);
			}
		}
	}

	public static void forEachModifier(ItemStack stack, net.minecraft.inventory.EntityEquipmentSlot slot,
			java.util.function.BiConsumer<io.github.xponer2.translator.registry.Holder<io.github.xponer2.translator.entity.Attribute>,
					io.github.xponer2.translator.entity.AttributeModifier> action) {
		modifiers(stack, slot, new java.util.HashSet<>(), action);
	}

	private static void modifiers(ItemStack stack, net.minecraft.inventory.EntityEquipmentSlot slot, Set<java.util.UUID> seen,
			java.util.function.BiConsumer<io.github.xponer2.translator.registry.Holder<io.github.xponer2.translator.entity.Attribute>,
					io.github.xponer2.translator.entity.AttributeModifier> action) {
		var operations = io.github.xponer2.translator.entity.AttributeModifier.Operation.values();
		for (var entry : stack.getAttributeModifiers(slot).entries()) {
			var modifier = entry.getValue();
			if (!seen.add(modifier.getID())) {
				continue;
			}
			String path = modifier.getName().toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9/._-]", "_");
			action.accept(io.github.xponer2.translator.entity.Attributes.byForgeName(entry.getKey()),
					new io.github.xponer2.translator.entity.AttributeModifier(new net.minecraft.util.ResourceLocation("minecraft",
							path.isEmpty() ? modifier.getID().toString() : path), modifier.getAmount(),
							operations[Math.max(0, Math.min(operations.length - 1, modifier.getOperation()))]));
		}
	}

	/** 1.21 {@code goalSelector.availableGoals}: the 1.12.2 task entries. */
	public static Set<EntityAITasks.EntityAITaskEntry> availableGoals(EntityAITasks tasks) {
		return tasks.taskEntries;
	}

	/** 1.21 {@code liquidBlock.fluid}: the fluid of a 1.21.1 mod's fluid block. */
	public static net.minecraftforge.fluids.Fluid fluid(io.github.xponer2.translator.runtime.mc.LiquidBlock block) {
		return block.getFluid();
	}

	// --- profiler -------------------------------------------------------------------------------------------------------

	/** 1.12.2's profiler has no tick bounds. */
	public static void startTick(net.minecraft.profiler.Profiler profiler) {
	}

	public static void endTick(net.minecraft.profiler.Profiler profiler) {
	}

	// --- StackedContents -----------------------------------------------------------------------------------------------

	/** 1.21 {@code stackedContents.accountSimpleStack(stack)}: a stack without data counted for recipe matching. */
	public static void accountSimpleStack(net.minecraft.client.util.RecipeItemHelper contents, ItemStack stack) {
		contents.accountStack(stack);
	}

	// --- LevelChunkSection ----------------------------------------------------------------------------------------------

	public static boolean hasOnlyAir(ExtendedBlockStorage section) {
		return section.isEmpty();
	}

	// --- GoalSelector ---------------------------------------------------------------------------------------------------

	/** 1.21 {@code goalSelector.getAvailableGoals()}: the 1.12.2 task entries (each with its goal and priority). */
	public static Set<EntityAITasks.EntityAITaskEntry> getAvailableGoals(EntityAITasks tasks) {
		return tasks.taskEntries;
	}
}
