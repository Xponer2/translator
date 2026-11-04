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

import java.util.UUID;

import javax.annotation.Nullable;

import io.github.xponer2.translator.text.LiteralComponent;
import io.github.xponer2.translator.text.MutableComponent;
import io.github.xponer2.translator.text.TranslatableComponent;
import io.github.xponer2.translator.util.ResourceLocations;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

/**
 * Static stand-ins for 1.21 methods the 1.12.2 classes don't have (runtime, neoforge-1.21.1-redirects.tsv): a call
 * {@code receiver.name(args)} the 1.12.2 class lacks becomes {@code VanillaRedirects.name(receiver, args)}; a static
 * call keeps its arguments. What the source converter does with rewrite rules, for bytecode.
 */
public final class VanillaRedirects {
	private VanillaRedirects() {
	}

	// --- Component (static) -------------------------------------------------------------------------------------------

	/** A key a call the Translator skipped left null is an empty one: 1.12.2's text would throw when drawn. */
	public static MutableComponent translatable(@Nullable String key) {
		return new TranslatableComponent(key == null ? "" : key);
	}

	public static MutableComponent translatable(@Nullable String key, Object[] args) {
		return new TranslatableComponent(key == null ? "" : key, args);
	}

	public static MutableComponent translatableWithFallback(String key, @Nullable String fallback) {
		return new TranslatableComponent(key);
	}

	public static MutableComponent literal(String text) {
		return new LiteralComponent(text);
	}

	public static MutableComponent empty() {
		return new LiteralComponent("");
	}

	// --- ResourceLocation (static) ------------------------------------------------------------------------------------

	public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
		return new ResourceLocation(namespace, path);
	}

	public static ResourceLocation withDefaultNamespace(String path) {
		return new ResourceLocation("minecraft", path);
	}

	public static ResourceLocation parse(String id) {
		return ResourceLocations.parse(id);
	}

	@Nullable
	public static ResourceLocation tryParse(String id) {
		try {
			return ResourceLocations.parse(id);
		} catch (RuntimeException e) {
			return null;
		}
	}

	// --- InteractionResultHolder (static factories of 1.12.2 ActionResult) ----------------------------------------------

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static net.minecraft.util.ActionResult pass(Object value) {
		return new net.minecraft.util.ActionResult(net.minecraft.util.EnumActionResult.PASS, value);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static net.minecraft.util.ActionResult success(Object value) {
		return new net.minecraft.util.ActionResult(net.minecraft.util.EnumActionResult.SUCCESS, value);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static net.minecraft.util.ActionResult consume(Object value) {
		return new net.minecraft.util.ActionResult(net.minecraft.util.EnumActionResult.SUCCESS, value);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static net.minecraft.util.ActionResult fail(Object value) {
		return new net.minecraft.util.ActionResult(net.minecraft.util.EnumActionResult.FAIL, value);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static net.minecraft.util.ActionResult sidedSuccess(Object value, boolean clientSide) {
		return new net.minecraft.util.ActionResult(net.minecraft.util.EnumActionResult.SUCCESS, value);
	}

	// --- entities and worlds ------------------------------------------------------------------------------------------

	public static World level(Entity entity) {
		return entity.world;
	}

	@Nullable
	public static EntityPlayer getPlayerByUUID(World world, UUID id) {
		return world.getPlayerEntityByUUID(id);
	}

	// --- sounds -------------------------------------------------------------------------------------------------------

	public static net.minecraft.util.SoundEvent createVariableRangeEvent(ResourceLocation id) {
		return new net.minecraft.util.SoundEvent(id);
	}

	public static net.minecraft.util.SoundEvent createFixedRangeEvent(ResourceLocation id, float range) {
		return new net.minecraft.util.SoundEvent(id);
	}

	// --- block states ---------------------------------------------------------------------------------------------------

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Object setValue(IBlockState state, net.minecraft.block.properties.IProperty property, Comparable value) {
		// null only comes from a method the Translator couldn't run (1.21 never sets null): the state stays
		return value == null || !state.getPropertyKeys().contains(property) ? state : state.withProperty(property, value);
	}

	@SuppressWarnings({"rawtypes", "unchecked"})
	public static boolean hasProperty(IBlockState state, net.minecraft.block.properties.IProperty property) {
		return state.getPropertyKeys().contains(property);
	}

	// --- text ---------------------------------------------------------------------------------------------------------

	public static String getString(ITextComponent text) {
		return text.getUnformattedText();
	}

	/** 1.21 FormattedCharSequence is ITextComponent here (neoforge-1.21.1-runtime.tsv): the text itself. */
	public static ITextComponent getVisualOrderText(ITextComponent text) {
		return text;
	}

	// --- more entity and world methods -------------------------------------------------------------------------------

	public static boolean hasEffect(Entity entity, io.github.xponer2.translator.registry.Holder<?> effect) {
		return getEffect(entity, effect) != null;
	}

	@Nullable
	public static net.minecraft.potion.PotionEffect getEffect(Entity entity, io.github.xponer2.translator.registry.Holder<?> effect) {
		return entity instanceof net.minecraft.entity.EntityLivingBase living && effect.value() instanceof net.minecraft.potion.Potion potion
				? living.getActivePotionEffect(potion) : null;
	}

	@Nullable
	public static net.minecraft.server.MinecraftServer getServer(World world) {
		return world.getMinecraftServer();
	}

	public static net.minecraft.entity.player.InventoryPlayer getInventory(EntityPlayer player) {
		return player.inventory;
	}

	/** 1.21 {@code Entity.pick}: the block the entity looks at (1.12.2 rayTrace is client-only). */
	@Nullable
	public static net.minecraft.util.math.RayTraceResult pick(Entity entity, double distance, float partialTicks, boolean fluids) {
		var eyes = entity.getPositionEyes(partialTicks);
		var look = entity.getLook(partialTicks);
		var end = eyes.add(look.x * distance, look.y * distance, look.z * distance);
		var hit = entity.world.rayTraceBlocks(eyes, end, fluids, false, true);
		return hit != null ? hit : new net.minecraft.util.math.RayTraceResult(net.minecraft.util.math.RayTraceResult.Type.MISS, end,
				net.minecraft.util.EnumFacing.getFacingFromVector((float) look.x, (float) look.y, (float) look.z), new net.minecraft.util.math.BlockPos(end));
	}

	/** NeoForge {@code level.getCapability(capability, pos, state, blockEntity, context)}. */
	@Nullable
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Object getCapability(World world, io.github.xponer2.translator.capabilities.BlockCapability capability,
			net.minecraft.util.math.BlockPos pos, @Nullable IBlockState state, @Nullable TileEntity tile, @Nullable Object context) {
		return capability.getCapability(world, pos, context);
	}

	@Nullable
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Object getCapability(World world, io.github.xponer2.translator.capabilities.BlockCapability capability,
			net.minecraft.util.math.BlockPos pos, @Nullable Object context) {
		return capability.getCapability(world, pos, context);
	}

	/** 1.21 {@code ClientboundBlockEntityDataPacket.create(blockEntity)}. */
	public static net.minecraft.network.play.server.SPacketUpdateTileEntity create(TileEntity tile) {
		return new net.minecraft.network.play.server.SPacketUpdateTileEntity(tile.getPos(), 0, tile.getUpdateTag());
	}

	// --- ray tracing ---------------------------------------------------------------------------------------------------

	public static net.minecraft.util.math.RayTraceResult clip(World world, io.github.xponer2.translator.util.ClipContext context) {
		return io.github.xponer2.translator.util.ClipContext.clip(world, context);
	}

	public static net.minecraft.util.math.Vec3d getLocation(net.minecraft.util.math.RayTraceResult hit) {
		return hit.hitVec;
	}

	public static net.minecraft.util.EnumFacing getDirection(net.minecraft.util.math.RayTraceResult hit) {
		return hit.sideHit;
	}

	public static net.minecraft.util.math.RayTraceResult.Type getType(net.minecraft.util.math.RayTraceResult hit) {
		return hit.typeOfHit;
	}

	public static net.minecraft.util.math.Vec3d getLookAngle(Entity entity) {
		return entity.getLookVec();
	}

	public static double getX(Entity entity) {
		return entity.posX;
	}

	public static double getY(Entity entity) {
		return entity.posY;
	}

	public static double getZ(Entity entity) {
		return entity.posZ;
	}

	public static double getEyeY(Entity entity) {
		return entity.posY + entity.getEyeHeight();
	}

	public static net.minecraft.util.math.Vec3d position(Entity entity) {
		return entity.getPositionVector();
	}

	// --- worlds ------------------------------------------------------------------------------------------------------

	/** 1.21 Level.isClientSide(), and the isClientSide field (read through this getter): 1.12.2 isRemote. */
	public static boolean isClientSide(World world) {
		return world.isRemote;
	}

	// --- block states ------------------------------------------------------------------------------------------------

	public static boolean isAir(IBlockState state) {
		return state.getMaterial() == net.minecraft.block.material.Material.AIR;
	}

	@SuppressWarnings("deprecation")
	public static float getDestroySpeed(IBlockState state, net.minecraft.world.IBlockAccess world, net.minecraft.util.math.BlockPos pos) {
		return state.getBlockHardness(world instanceof World level ? level : null, pos);
	}

	public static boolean canBeReplaced(IBlockState state, @Nullable net.minecraftforge.fluids.Fluid fluid) {
		return state.getMaterial().isReplaceable();
	}

	public static boolean canBeReplaced(IBlockState state) {
		return state.getMaterial().isReplaceable();
	}

	public static boolean isSolid(IBlockState state) {
		return state.getMaterial().isSolid();
	}

	public static boolean liquid(IBlockState state) {
		return state.getMaterial().isLiquid();
	}

	public static boolean is(IBlockState state, Block block) {
		return state.getBlock() == block;
	}

	public static boolean hasBlockEntity(IBlockState state) {
		return state.getBlock().hasTileEntity(state);
	}

	// --- players ------------------------------------------------------------------------------------------------------

	public static UUID getUUID(Entity entity) {
		return entity.getUniqueID();
	}

	public static net.minecraft.util.EnumFacing getDirection(Entity entity) {
		return entity.getHorizontalFacing();
	}

	public static java.util.OptionalInt openMenu(EntityPlayer player, io.github.xponer2.translator.menu.MenuProvider provider, net.minecraft.util.math.BlockPos pos) {
		io.github.xponer2.translator.menu.Menus.openMenu(player, provider, pos);
		return java.util.OptionalInt.empty();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static java.util.OptionalInt openMenu(EntityPlayer player, io.github.xponer2.translator.menu.MenuProvider provider,
			java.util.function.Consumer extraData) {
		io.github.xponer2.translator.menu.Menus.openMenu(player, provider, extraData);
		return java.util.OptionalInt.empty();
	}

	public static java.util.OptionalInt openMenu(EntityPlayer player, io.github.xponer2.translator.menu.MenuProvider provider) {
		io.github.xponer2.translator.menu.Menus.openMenu(player, provider);
		return java.util.OptionalInt.empty();
	}

	// --- item stacks and NBT --------------------------------------------------------------------------------------------

	public static NBTBase save(ItemStack stack, io.github.xponer2.translator.registry.HolderLookup.Provider registries, NBTBase into) {
		return stack.writeToNBT(into instanceof NBTTagCompound tag ? tag : new NBTTagCompound());
	}

	public static NBTBase save(ItemStack stack, io.github.xponer2.translator.registry.HolderLookup.Provider registries) {
		return stack.writeToNBT(new NBTTagCompound());
	}

	public static java.util.Optional<ItemStack> parse(io.github.xponer2.translator.registry.HolderLookup.Provider registries, NBTBase tag) {
		if (!(tag instanceof NBTTagCompound compound)) {
			return java.util.Optional.empty();
		}
		var stack = new ItemStack(compound);
		return stack.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(stack);
	}

	public static ItemStack parseOptional(io.github.xponer2.translator.registry.HolderLookup.Provider registries, NBTTagCompound tag) {
		return tag.isEmpty() ? ItemStack.EMPTY : new ItemStack(tag);
	}

	public static boolean listMatches(java.util.List<ItemStack> first, java.util.List<ItemStack> second) {
		if (first.size() != second.size()) {
			return false;
		}
		for (int i = 0; i < first.size(); i++) {
			if (!ItemStack.areItemStacksEqual(first.get(i), second.get(i))) {
				return false;
			}
		}
		return true;
	}

	// --- blocks -------------------------------------------------------------------------------------------------------

	public static io.github.xponer2.translator.shape.VoxelShape box(double x1, double y1, double z1, double x2, double y2, double z2) {
		return io.github.xponer2.translator.shape.Shapes.box16(x1, y1, z1, x2, y2, z2);
	}

	public static IBlockState any(net.minecraft.block.state.BlockStateContainer container) {
		return container.getBaseState();
	}

	public static IBlockState defaultBlockState(Block block) {
		return block.getDefaultState();
	}

	public static IBlockState getBlockState(TileEntity tile) {
		return tile.getWorld() == null ? tile.getBlockType().getDefaultState() : tile.getWorld().getBlockState(tile.getPos());
	}

	// --- items --------------------------------------------------------------------------------------------------------

	/** {@code new ItemStack(ItemLike)}: 1.12.2 items and blocks don't implement the Translator's ItemLike. */
	public static ItemStack newItemStack(io.github.xponer2.translator.util.ItemLike like) {
		return newItemStack(like, 1);
	}

	public static ItemStack newItemStack(io.github.xponer2.translator.util.ItemLike like, int count) {
		Object value = like;
		if (value instanceof net.minecraft.item.Item item) {
			return new ItemStack(item, count);
		}
		if (value instanceof Block block) {
			return new ItemStack(block, count);
		}
		return value == null ? ItemStack.EMPTY : new ItemStack(like.asItem(), count);
	}

	public static int hashStackList(java.util.List<ItemStack> stacks) {
		int hash = 0;
		for (ItemStack stack : stacks) {
			hash = hash * 31 + (stack.isEmpty() ? 0 : 31 * (31 * net.minecraft.item.Item.getIdFromItem(stack.getItem()) + stack.getMetadata())
					+ stack.getCount() + (stack.hasTagCompound() ? stack.getTagCompound().hashCode() : 0));
		}
		return hash;
	}

	/** NeoForge {@code stack.getCapability(capability)}: the providers registered for the item. */
	@Nullable
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Object getCapability(ItemStack stack, io.github.xponer2.translator.capabilities.ItemCapability capability) {
		return capability.getCapability(stack);
	}

	@Nullable
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static Object getCapability(ItemStack stack, io.github.xponer2.translator.capabilities.ItemCapability capability, Object context) {
		return capability.getCapability(stack, context);
	}

	/** 1.21 {@code slot.onTake(player, stack)} returns nothing (1.12.2 returns the stack). */
	public static void onTake(net.minecraft.inventory.Slot slot, EntityPlayer player, ItemStack stack) {
		slot.onTake(player, stack);
	}

	/** DFU 4 has no size-limited list codec: the plain list (the limit only guards against huge packets). */
	public static <T> com.mojang.serialization.Codec<java.util.List<T>> sizeLimitedListOf(com.mojang.serialization.Codec<T> codec, int maxSize) {
		return codec.listOf();
	}

	// --- effects and food ------------------------------------------------------------------------------------------------

	/** {@code new MobEffectInstance(holder, ...)}: the 1.12.2 effect of the holder. */
	public static net.minecraft.potion.PotionEffect newPotionEffect(io.github.xponer2.translator.registry.Holder<?> effect, int duration) {
		return new net.minecraft.potion.PotionEffect((net.minecraft.potion.Potion) effect.value(), duration);
	}

	public static net.minecraft.potion.PotionEffect newPotionEffect(io.github.xponer2.translator.registry.Holder<?> effect, int duration, int amplifier) {
		return new net.minecraft.potion.PotionEffect((net.minecraft.potion.Potion) effect.value(), duration, amplifier);
	}

	public static net.minecraft.potion.PotionEffect newPotionEffect(io.github.xponer2.translator.registry.Holder<?> effect, int duration, int amplifier,
			boolean ambient, boolean visible) {
		return new net.minecraft.potion.PotionEffect((net.minecraft.potion.Potion) effect.value(), duration, amplifier, ambient, visible);
	}

	public static net.minecraft.potion.PotionEffect newPotionEffect(io.github.xponer2.translator.registry.Holder<?> effect, int duration, int amplifier,
			boolean ambient, boolean visible, boolean showIcon) {
		return new net.minecraft.potion.PotionEffect((net.minecraft.potion.Potion) effect.value(), duration, amplifier, ambient, visible);
	}

	/** 1.21 {@code stack.getFoodProperties(entity)}: the food of a 1.21.1 mod's item, else null. */
	@Nullable
	public static io.github.xponer2.translator.item.FoodProperties getFoodProperties(ItemStack stack, @Nullable EntityLivingBase entity) {
		var properties = io.github.xponer2.translator.item.ItemProperties.of(stack.getItem());
		return properties == null ? null : properties.getFood();
	}

	/** 1.21 {@code item.builtInRegistryHolder()}: the item's holder in the item registry. */
	public static io.github.xponer2.translator.registry.Registry.Reference<net.minecraft.item.Item> builtInRegistryHolder(net.minecraft.item.Item item) {
		return io.github.xponer2.translator.util.BuiltInRegistries.ITEM.createIntrusiveHolder(item);
	}

	public static io.github.xponer2.translator.registry.Registry.Reference<Block> builtInRegistryHolder(Block block) {
		return io.github.xponer2.translator.util.BuiltInRegistries.BLOCK.createIntrusiveHolder(block);
	}

	public static io.github.xponer2.translator.registry.Registry.Reference<net.minecraftforge.fluids.Fluid> builtInRegistryHolder(
			net.minecraftforge.fluids.Fluid fluid) {
		@SuppressWarnings({ "unchecked", "rawtypes" })
		var registry = (io.github.xponer2.translator.registry.ResourceKey) io.github.xponer2.translator.util.Registries.FLUID;
		@SuppressWarnings("unchecked")
		io.github.xponer2.translator.registry.ResourceKey<net.minecraftforge.fluids.Fluid> key = io.github.xponer2.translator.registry.ResourceKey.create(registry,
				new net.minecraft.util.ResourceLocation(fluid.getName()));
		return new io.github.xponer2.translator.registry.Registry.Reference<>(key, fluid);
	}

	/** Shulker boxes don't go into other containers' items. */
	public static boolean canFitInsideContainerItems(net.minecraft.item.Item item) {
		return !(item instanceof net.minecraft.item.ItemShulkerBox);
	}

	/** 1.21 {@code menu.slots}, a NonNullList (the Translator menu's slot list is one). */
	@SuppressWarnings("unchecked")
	public static net.minecraft.util.NonNullList<net.minecraft.inventory.Slot> inventorySlots(io.github.xponer2.translator.menu.AbstractContainerMenu menu) {
		return (net.minecraft.util.NonNullList<net.minecraft.inventory.Slot>) menu.inventorySlots;
	}

	public static net.minecraft.util.NonNullList<net.minecraft.inventory.Slot> slots(io.github.xponer2.translator.menu.AbstractContainerMenu menu) {
		return inventorySlots(menu);
	}

	/** 1.21 {@code player.containerMenu}: the open menu, a vanilla 1.12.2 container as a view of it (VanillaMenu). */
	@Nullable
	public static io.github.xponer2.translator.menu.AbstractContainerMenu containerMenu(EntityPlayer player) {
		return io.github.xponer2.translator.menu.VanillaMenu.of(player);
	}

	public static ITextComponent getHoverName(ItemStack stack) {
		return new LiteralComponent(stack.getDisplayName());
	}

	// --- NBT ------------------------------------------------------------------------------------------------------------

	@Nullable
	public static NBTBase put(NBTTagCompound tag, String key, NBTBase value) {
		NBTBase old = tag.getTag(key);
		if (value == null) {
			// a part the Translator can't save yet gave null: 1.12.2 throws (in a block entity's save, every tick)
			io.github.xponer2.translator.runtime.RuntimeStubs.missing("CompoundTag.put(" + key + ", null): a 1.21.1 mod saved nothing there");
			return old;
		}
		tag.setTag(key, value);
		return old;
	}

	/** 1.21 {@code NbtUtils.writeBlockPos}: the position as an int array [x, y, z] (Powah's energizing rods). */
	public static NBTBase writeBlockPos(net.minecraft.util.math.BlockPos pos) {
		return new net.minecraft.nbt.NBTTagIntArray(new int[] {pos.getX(), pos.getY(), pos.getZ()});
	}

	/** 1.21 {@code NbtUtils.readBlockPos(tag, key)}: the int array at the key (or a compound with X, Y, Z), if any. */
	public static java.util.Optional<net.minecraft.util.math.BlockPos> readBlockPos(NBTTagCompound tag, String key) {
		if (tag.hasKey(key, 11)) {
			int[] xyz = tag.getIntArray(key);
			if (xyz.length == 3) {
				return java.util.Optional.of(new net.minecraft.util.math.BlockPos(xyz[0], xyz[1], xyz[2]));
			}
		}
		if (tag.hasKey(key, 10)) {
			return java.util.Optional.of(net.minecraft.nbt.NBTUtil.getPosFromTag(tag.getCompoundTag(key)));
		}
		return java.util.Optional.empty();
	}

	/** The same, under the name the table gives readBlockPos. */
	public static java.util.Optional<net.minecraft.util.math.BlockPos> getPosFromTag(NBTTagCompound tag, String key) {
		return readBlockPos(tag, key);
	}

	/**
	 * 1.21 {@code CustomData.EMPTY}: CustomData is an NBT compound on 1.12.2 (the carried class's EMPTY stayed null:
	 * spawn eggs couldn't read their entity). Not to be changed: 1.21 code copies a CustomData's tag before it does.
	 */
	public static final NBTTagCompound EMPTY = new NBTTagCompound();

	public static int size(NBTTagList list) {
		return list.tagCount();
	}

	public static boolean add(NBTTagList list, NBTBase value) {
		if (value == null) {
			// a part the Translator can't save yet gave null (Create's fluid tanks): 1.12.2's list throws on it, in the
			// save of a block entity every tick; an empty tag keeps the list's order
			io.github.xponer2.translator.runtime.RuntimeStubs.missing("ListTag.add(null): a 1.21.1 mod saved nothing for an entry");
			value = new NBTTagCompound();
		}
		list.appendTag(value);
		return true;
	}

	/** The erased {@code List.add} a 1.21 {@code ListTag} is called with. */
	public static boolean add(NBTTagList list, Object value) {
		return add(list, (NBTBase) value);
	}

	/** A 1.21 {@code ListTag} is a {@code List}: 1.12.2's is only {@code Iterable}. */
	public static java.util.stream.Stream<NBTBase> stream(NBTTagList list) {
		return java.util.stream.StreamSupport.stream(list.spliterator(), false);
	}

	public static boolean isEmpty(NBTTagList list) {
		return list.isEmpty();
	}

	public static NBTBase remove(NBTTagList list, int index) {
		return list.removeTag(index);
	}

	/** The erased {@code List.get} (a method reference {@code list::get} is to {@code AbstractList}'s). */
	public static Object get(NBTTagList list, int index) {
		return list.get(index);
	}

	/** 1.21 {@code NbtUtils.readBlockState(HolderGetter<Block>, tag)}: the block registry lookup isn't needed. */
	public static IBlockState readBlockState(io.github.xponer2.translator.registry.Registry<?> blocks, NBTTagCompound tag) {
		return net.minecraft.nbt.NBTUtil.readBlockState(tag);
	}

	public static NBTTagCompound writeBlockState(IBlockState state) {
		return net.minecraft.nbt.NBTUtil.writeBlockState(new NBTTagCompound(), state);
	}

	// --- registries -----------------------------------------------------------------------------------------------------

	public static io.github.xponer2.translator.registry.RegistryAccess registryAccess(net.minecraft.server.MinecraftServer server) {
		return io.github.xponer2.translator.registry.RegistryAccess.INSTANCE;
	}

	public static io.github.xponer2.translator.registry.RegistryAccess registryAccess(World world) {
		return io.github.xponer2.translator.registry.RegistryAccess.INSTANCE;
	}

	/** 1.21 {@code stack.enchant(holder, level)}: the holder of a 1.12.2 enchantment. */
	public static void enchant(ItemStack stack, io.github.xponer2.translator.registry.Holder<?> enchantment, int level) {
		stack.addEnchantment((net.minecraft.enchantment.Enchantment) enchantment.value(), level);
	}

	// --- positions and vectors ----------------------------------------------------------------------------------------

	public static double get(net.minecraft.util.math.Vec3d vec, net.minecraft.util.EnumFacing.Axis axis) {
		switch (axis) {
			case X:
				return vec.x;
			case Y:
				return vec.y;
			default:
				return vec.z;
		}
	}

	public static int get(net.minecraft.util.math.Vec3i vec, net.minecraft.util.EnumFacing.Axis axis) {
		switch (axis) {
			case X:
				return vec.getX();
			case Y:
				return vec.getY();
			default:
				return vec.getZ();
		}
	}

	public static net.minecraft.util.math.Vec3d multiply(net.minecraft.util.math.Vec3d vec, double x, double y, double z) {
		return new net.minecraft.util.math.Vec3d(vec.x * x, vec.y * y, vec.z * z);
	}

	public static net.minecraft.util.math.Vec3d multiply(net.minecraft.util.math.Vec3d vec, net.minecraft.util.math.Vec3d by) {
		return multiply(vec, by.x, by.y, by.z);
	}

	// --- world and players --------------------------------------------------------------------------------------------

	/** The light at a position: block light or sky light less the night darkening, whichever is brighter. */
	public static int getMaxLocalRawBrightness(World world, net.minecraft.util.math.BlockPos pos) {
		return world.getLight(pos, false);
	}

	public static void giveExperiencePoints(EntityPlayer player, int amount) {
		player.addExperience(amount);
	}
}
