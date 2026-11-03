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

import java.util.List;
import java.util.Optional;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.Holder;
import io.github.xponer2.translator.text.LiteralComponent;
import io.github.xponer2.translator.text.MutableComponent;
import io.github.xponer2.translator.text.TranslatableComponent;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagLongArray;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.chunk.Chunk;

/**
 * Static stand-ins for 1.21 methods of small 1.12.2 classes (ids, positions, boxes, NBT, text, dyes, blocks, block
 * entities, chunks, living entities) that they lack (runtime, neoforge-1.21.1-redirects.tsv).
 */
public final class MiscBridges {
	private MiscBridges() {
	}

	// --- constructors and constants 1.12.2 lacks ---------------------------------------------------------------------

	/** Things that are no creature (projectiles, items): 1.12.2 has no such category, ambient counts least. */
	public static final net.minecraft.entity.EnumCreatureType MISC = net.minecraft.entity.EnumCreatureType.AMBIENT;

	/** 1.21 {@code new RangedAttribute(descriptionId, default, min, max)}: no parent. */
	public static net.minecraft.entity.ai.attributes.RangedAttribute newRangedAttribute(String name, double defaultValue, double min, double max) {
		return new net.minecraft.entity.ai.attributes.RangedAttribute(null, name, defaultValue, min, max);
	}

	/** 1.21 {@code new ChunkPos(packed)}. */
	public static net.minecraft.util.math.ChunkPos newChunkPos(long packed) {
		return new net.minecraft.util.math.ChunkPos((int) packed, (int) (packed >>> 32));
	}

	public static long toLong(net.minecraft.util.math.ChunkPos pos) {
		return net.minecraft.util.math.ChunkPos.asLong(pos.x, pos.z);
	}

	// --- DamageSource and PotionEffect -------------------------------------------------------------------------------

	/** A 1.21 damage type (in_fire) against the 1.12.2 damage type (inFire). */
	public static boolean is(net.minecraft.util.DamageSource source, io.github.xponer2.translator.registry.ResourceKey<?> type) {
		return damageType(type.location().getPath()).equalsIgnoreCase(source.getDamageType());
	}

	/** Damage type tags: is_fire, is_explosion, is_projectile, bypasses_armor, ... by the 1.12.2 source's kind. */
	public static boolean is(net.minecraft.util.DamageSource source, io.github.xponer2.translator.util.TagKey<?> tag) {
		switch (tag.location().getPath()) {
		case "is_fire":
			return source.isFireDamage();
		case "is_explosion":
			return source.isExplosion();
		case "is_projectile":
			return source.isProjectile();
		case "bypasses_armor":
			return source.isUnblockable();
		case "bypasses_invulnerability":
			return source.canHarmInCreative();
		case "is_fall":
			return source == net.minecraft.util.DamageSource.FALL;
		case "is_drowning":
			return source == net.minecraft.util.DamageSource.DROWN;
		case "witch_resistant_to":
		case "bypasses_effects":
			return source.isMagicDamage();
		default:
			return false;
		}
	}

	private static String damageType(String path) {
		switch (path) {
		case "player_attack":
			return "player";
		case "mob_attack":
		case "mob_attack_no_aggro":
			return "mob";
		case "player_explosion":
			return "explosion.player";
		default:
			var name = new StringBuilder();
			boolean upper = false;
			for (char c : path.toCharArray()) {
				if (c == '_') {
					upper = true;
				} else {
					name.append(upper ? Character.toUpperCase(c) : c);
					upper = false;
				}
			}
			return name.toString();
		}
	}

	public static Holder<net.minecraft.potion.Potion> getEffect(net.minecraft.potion.PotionEffect effect) {
		return io.github.xponer2.translator.util.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect.getPotion());
	}

	// --- IBlockState -------------------------------------------------------------------------------------------------

	public static boolean is(IBlockState state, Holder<Block> block) {
		return block != null && block.value() == state.getBlock();
	}

	public static Holder<Block> getBlockHolder(IBlockState state) {
		return io.github.xponer2.translator.util.BuiltInRegistries.BLOCK.createIntrusiveHolder(state.getBlock());
	}

	public static boolean canBeReplaced(IBlockState state, io.github.xponer2.translator.util.BlockPlaceContext context) {
		return state.getBlock().isReplaceable(context.getLevel(), context.getClickedPos());
	}

	/** The state after a neighbour changed (1.21 shape updates): 1.12.2 blocks compute theirs in getActualState. */
	public static IBlockState updateShape(IBlockState state, EnumFacing direction, IBlockState neighbour, net.minecraft.world.World world, BlockPos pos,
			BlockPos neighbourPos) {
		return state;
	}

	public static void updateNeighbourShapes(IBlockState state, net.minecraft.world.World world, BlockPos pos, int flags) {
	}

	public static void updateIndirectNeighbourShapes(IBlockState state, net.minecraft.world.World world, BlockPos pos, int flags) {
	}

	public static void handleNeighborChanged(IBlockState state, net.minecraft.world.World world, BlockPos pos, Block block, BlockPos from, boolean moving) {
		state.neighborChanged(world, pos, block, from);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <T extends Comparable<T>> Optional<T> getOptionalValue(IBlockState state, net.minecraft.block.properties.IProperty<T> property) {
		return state.getPropertyKeys().contains(property) ? Optional.of(state.getValue(property)) : Optional.empty();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static Object trySetValue(IBlockState state, net.minecraft.block.properties.IProperty property, Comparable value) {
		return state.getPropertyKeys().contains(property) ? state.withProperty(property, value) : state;
	}

	public static java.util.Map<net.minecraft.block.properties.IProperty<?>, Comparable<?>> getValues(IBlockState state) {
		return state.getProperties();
	}

	public static boolean blocksMotion(IBlockState state) {
		return state.getMaterial().blocksMovement();
	}

	public static boolean isEmpty(IBlockState state) {
		return state.getMaterial() == net.minecraft.block.material.Material.AIR;
	}

	public static float getFriction(IBlockState state, net.minecraft.world.World world, BlockPos pos, @Nullable net.minecraft.entity.Entity entity) {
		return state.getBlock().getSlipperiness(state, world, pos, entity);
	}

	public static boolean skipRendering(IBlockState state, IBlockState neighbour, EnumFacing side) {
		return false;
	}

	public static void randomTick(IBlockState state, net.minecraft.world.WorldServer world, BlockPos pos, java.util.Random random) {
		state.getBlock().randomTick(world, pos, state, random);
	}

	public static ItemStack getCloneItemStack(IBlockState state, RayTraceResult hit, net.minecraft.world.World world, BlockPos pos,
			net.minecraft.entity.player.EntityPlayer player) {
		return state.getBlock().getPickBlock(state, hit, world, pos, player);
	}

	public static IBlockState getAppearance(IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos, EnumFacing side,
			@Nullable IBlockState queryState, @Nullable BlockPos queryPos) {
		return state;
	}

	public static io.github.xponer2.translator.runtime.neoforge.TriState canSustainPlant(IBlockState state, net.minecraft.world.IBlockAccess world,
			BlockPos pos, EnumFacing facing, IBlockState plant) {
		return io.github.xponer2.translator.runtime.neoforge.TriState.DEFAULT;
	}

	public static boolean canEntityDestroy(IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos, net.minecraft.entity.Entity entity) {
		return state.getBlock().canEntityDestroy(state, world, pos, entity);
	}

	public static boolean canHarvestBlock(IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos,
			net.minecraft.entity.player.EntityPlayer player) {
		return state.getBlock().canHarvestBlock(world, pos, player);
	}

	public static float getExplosionResistance(IBlockState state, net.minecraft.world.IBlockAccess world, BlockPos pos,
			net.minecraft.world.Explosion explosion) {
		return world instanceof net.minecraft.world.World level ? state.getBlock().getExplosionResistance(level, pos, null, explosion)
				: getExplosionResistance(state.getBlock());
	}

	// --- constructors of 1.21 forms ----------------------------------------------------------------------------------

	public static ItemStack newItemStack(Holder<net.minecraft.item.Item> item) {
		return new ItemStack(item.value());
	}

	public static ItemStack newItemStack(Holder<net.minecraft.item.Item> item, int count) {
		return new ItemStack(item.value(), count);
	}

	public static ItemStack newItemStack(Holder<net.minecraft.item.Item> item, int count, io.github.xponer2.translator.component.DataComponentPatch patch) {
		ItemStack stack = new ItemStack(item.value(), count);
		io.github.xponer2.translator.component.StackComponents.applyComponents(stack, patch);
		return stack;
	}

	public static RayTraceResult newRayTraceResult(Vec3d hit, EnumFacing face, BlockPos pos, boolean inside) {
		return new RayTraceResult(hit, face, pos);
	}

	// --- small members 1.12.2 lacks ----------------------------------------------------------------------------------

	public static Optional<Float> fixedRange(net.minecraft.util.SoundEvent sound) {
		return Optional.empty();
	}

	public static double choose(EnumFacing.Axis axis, double x, double y, double z) {
		return axis == EnumFacing.Axis.X ? x : axis == EnumFacing.Axis.Y ? y : z;
	}

	public static int choose(EnumFacing.Axis axis, int x, int y, int z) {
		return axis == EnumFacing.Axis.X ? x : axis == EnumFacing.Axis.Y ? y : z;
	}

	public static Object byIdOrThrow(net.minecraft.util.ObjectIntIdentityMap<?> map, int id) {
		Object value = map.getByValue(id);
		if (value == null) {
			throw new IllegalArgumentException("No value with id " + id);
		}
		return value;
	}

	@Nullable
	public static net.minecraft.entity.Entity getOwner(net.minecraft.entity.item.EntityItem item) {
		String owner = item.getOwner();
		return owner == null ? null : item.world.getPlayerEntityByName(owner);
	}

	public static ItemStack getWeaponItem(EntityLivingBase entity) {
		return entity.getHeldItemMainhand();
	}

	public static ITextComponent description(net.minecraft.enchantment.Enchantment enchantment) {
		return new net.minecraft.util.text.TextComponentTranslation(enchantment.getName());
	}

	public static boolean isPublished(net.minecraft.server.integrated.IntegratedServer server) {
		return server.getPublic();
	}

	// --- libraries Minecraft 1.12.2 has older versions of (fastutil 7, gson 2.2) ----------------------------------------

	public static int putIfAbsent(it.unimi.dsi.fastutil.objects.Object2IntMap<Object> map, Object key, int value) {
		if (map.containsKey(key)) {
			return map.getInt(key);
		}
		map.put(key, value);
		return map.defaultReturnValue();
	}

	public static int computeIntIfAbsent(it.unimi.dsi.fastutil.objects.Object2IntMap<Object> map, Object key, java.util.function.ToIntFunction<Object> function) {
		if (map.containsKey(key)) {
			return map.getInt(key);
		}
		int value = function.applyAsInt(key);
		map.put(key, value);
		return value;
	}

	/*
	 * computeIfAbsent with a fastutil function (fastutil 8): its default asks the function containsKey first, which a
	 * lambda made for fastutil 7's interface doesn't implement (AbstractMethodError in AE2's block entity ticking, on
	 * every machine placed). The lambda's own method only.
	 */

	public static int computeIfAbsent(it.unimi.dsi.fastutil.objects.Object2IntMap<Object> map, Object key,
			it.unimi.dsi.fastutil.objects.Object2IntFunction<Object> function) {
		if (map.containsKey(key)) {
			return map.getInt(key);
		}
		int value = function.getInt(key);
		map.put(key, value);
		return value;
	}

	public static Object computeIfAbsent(it.unimi.dsi.fastutil.longs.Long2ObjectMap<Object> map, long key,
			it.unimi.dsi.fastutil.longs.Long2ObjectFunction<Object> function) {
		if (map.containsKey(key)) {
			return map.get(key);
		}
		Object value = function.get(key);
		map.put(key, value);
		return value;
	}

	public static Object computeIfAbsent(it.unimi.dsi.fastutil.ints.Int2ObjectMap<Object> map, int key,
			it.unimi.dsi.fastutil.ints.Int2ObjectFunction<Object> function) {
		if (map.containsKey(key)) {
			return map.get(key);
		}
		Object value = function.get(key);
		map.put(key, value);
		return value;
	}

	public static Object computeIfAbsent(it.unimi.dsi.fastutil.objects.Object2ObjectMap<Object, Object> map, Object key,
			it.unimi.dsi.fastutil.objects.Object2ObjectFunction<Object, Object> function) {
		if (map.containsKey(key)) {
			return map.get(key);
		}
		Object value = function.get(key);
		map.put(key, value);
		return value;
	}

	public static Object computeIfAbsent(it.unimi.dsi.fastutil.objects.Reference2ObjectMap<Object, Object> map, Object key,
			it.unimi.dsi.fastutil.objects.Reference2ObjectFunction<Object, Object> function) {
		if (map.containsKey(key)) {
			return map.get(key);
		}
		Object value = function.get(key);
		map.put(key, value);
		return value;
	}

	public static Object computeIfAbsent(it.unimi.dsi.fastutil.longs.Long2ObjectMap<Object> map, long key,
			java.util.function.LongFunction<Object> function) {
		if (map.containsKey(key)) {
			return map.get(key);
		}
		Object value = function.apply(key);
		map.put(key, value);
		return value;
	}

	public static Object computeIfAbsent(it.unimi.dsi.fastutil.ints.Int2ObjectMap<Object> map, int key,
			java.util.function.IntFunction<Object> function) {
		if (map.containsKey(key)) {
			return map.get(key);
		}
		Object value = function.apply(key);
		map.put(key, value);
		return value;
	}

	public static boolean isEmpty(com.google.gson.JsonArray array) {
		return array.size() == 0;
	}

	// --- TextFormatting, PotionEffect, Ingredient --------------------------------------------------------------------

	private static final int[] FORMAT_COLORS = {0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 0x555555, 0x5555FF,
			0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

	/** The RGB of a color code, null for the format codes. */
	@Nullable
	public static Integer getColor(TextFormatting format) {
		int index = format.getColorIndex();
		return index >= 0 && index < FORMAT_COLORS.length ? FORMAT_COLORS[index] : null;
	}

	public static boolean endsWithin(net.minecraft.potion.PotionEffect effect, int ticks) {
		return effect.getDuration() <= ticks;
	}

	public static boolean showIcon(net.minecraft.potion.PotionEffect effect) {
		return true;
	}

	public static boolean is(net.minecraft.potion.PotionEffect effect, Holder<net.minecraft.potion.Potion> potion) {
		return potion != null && potion.value() == effect.getPotion();
	}

	public static boolean isInfiniteDuration(net.minecraft.potion.PotionEffect effect) {
		return false;
	}

	public static java.util.Set<Object> getCures(net.minecraft.potion.PotionEffect effect) {
		return new java.util.HashSet<>();
	}

	public static boolean isEmpty(net.minecraft.item.crafting.Ingredient ingredient) {
		return ingredient == net.minecraft.item.crafting.Ingredient.EMPTY || ingredient.getMatchingStacks().length == 0;
	}

	public static boolean hasNoItems(net.minecraft.item.crafting.Ingredient ingredient) {
		return isEmpty(ingredient);
	}

	public static boolean isCustom(net.minecraft.item.crafting.Ingredient ingredient) {
		return false;
	}

	@Nullable
	public static io.github.xponer2.translator.recipe.ingredient.ICustomIngredient getCustomIngredient(net.minecraft.item.crafting.Ingredient ingredient) {
		return null;
	}

	// --- ItemStack ---------------------------------------------------------------------------------------------------

	/** The tooltip lines of a stack as text (1.12.2 tooltips are strings). */
	public static List<ITextComponent> getTooltipLines(ItemStack stack, io.github.xponer2.translator.item.TooltipContext context,
			@Nullable net.minecraft.entity.player.EntityPlayer player, net.minecraft.client.util.ITooltipFlag flag) {
		var lines = new java.util.ArrayList<ITextComponent>();
		for (String line : stack.getTooltip(player, flag)) {
			lines.add(new TextComponentString(line));
		}
		return lines;
	}

	public static boolean canPerformAction(ItemStack stack, Object ability) {
		return false;
	}

	public static int getBurnTime(ItemStack stack, @Nullable Object recipeType) {
		return net.minecraft.tileentity.TileEntityFurnace.getItemBurnTime(stack);
	}

	public static Holder<net.minecraft.item.Item> getItemHolder(ItemStack stack) {
		return io.github.xponer2.translator.util.BuiltInRegistries.ITEM.createIntrusiveHolder(stack.getItem());
	}

	public static int getEnchantmentLevel(ItemStack stack, Holder<net.minecraft.enchantment.Enchantment> enchantment) {
		return enchantment == null ? 0 : net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(enchantment.value(), stack);
	}

	public static boolean supportsEnchantment(ItemStack stack, Holder<net.minecraft.enchantment.Enchantment> enchantment) {
		return enchantment != null && enchantment.value().canApply(stack);
	}

	public static java.util.stream.Stream<io.github.xponer2.translator.util.TagKey<net.minecraft.item.Item>> getTags(ItemStack stack) {
		return io.github.xponer2.translator.tags.TagManager.getTags(stack);
	}

	public static boolean isComponentsPatchEmpty(ItemStack stack) {
		return !stack.hasTagCompound();
	}

	public static net.minecraft.util.EnumActionResult useOn(ItemStack stack, io.github.xponer2.translator.util.UseOnContext context) {
		var player = context.getPlayer();
		if (player == null) {
			return net.minecraft.util.EnumActionResult.PASS;
		}
		return stack.onItemUse(player, context.getLevel(), context.getClickedPos(), context.getHand(), context.getClickedFace(), context.hitX(),
				context.hitY(), context.hitZ());
	}

	public static Optional<Object> getTooltipImage(ItemStack stack) {
		return Optional.empty();
	}

	public static io.github.xponer2.translator.item.ItemAttributeModifiers getAttributeModifiers(ItemStack stack) {
		var properties = io.github.xponer2.translator.item.ItemProperties.of(stack.getItem());
		var modifiers = properties == null ? null : properties.attributeModifiers();
		return modifiers == null ? io.github.xponer2.translator.item.ItemAttributeModifiers.EMPTY : modifiers;
	}

	public static float getXpRepairRatio(ItemStack stack) {
		return stack.getItem().getXpRepairRatio(stack);
	}

	public static ItemStack copyAndClear(ItemStack stack) {
		ItemStack copy = stack.copy();
		stack.setCount(0);
		return copy;
	}

	public static boolean isBarVisible(ItemStack stack) {
		return stack.getItem().showDurabilityBar(stack);
	}

	public static boolean is(ItemStack stack, java.util.function.Predicate<Holder<net.minecraft.item.Item>> test) {
		return test.test(getItemHolder(stack));
	}

	public static boolean canEquip(ItemStack stack, net.minecraft.inventory.EntityEquipmentSlot slot, EntityLivingBase entity) {
		return stack.getItem().isValidArmor(stack, slot, entity);
	}

	// --- EntityPlayer ------------------------------------------------------------------------------------------------

	public static int getBlockY(net.minecraft.entity.Entity entity) {
		return net.minecraft.util.math.MathHelper.floor(entity.posY);
	}

	public static int getBlockX(net.minecraft.entity.Entity entity) {
		return net.minecraft.util.math.MathHelper.floor(entity.posX);
	}

	public static int getBlockZ(net.minecraft.entity.Entity entity) {
		return net.minecraft.util.math.MathHelper.floor(entity.posZ);
	}

	/** Eats a stack: its food (a 1.12.2 food item, or the item's 1.21 food properties), one less. */
	public static ItemStack eat(net.minecraft.entity.player.EntityPlayer player, net.minecraft.world.World world, ItemStack stack) {
		if (stack.getItem() instanceof net.minecraft.item.ItemFood food) {
			player.getFoodStats().addStats(food, stack);
		} else {
			var properties = io.github.xponer2.translator.item.ItemProperties.of(stack.getItem());
			var food = properties == null ? null : properties.getFood();
			if (food != null) {
				// 1.12.2 takes the saturation modifier (saturation = nutrition * modifier * 2)
				player.getFoodStats().addStats(food.nutrition(), food.nutrition() > 0 ? food.saturation() / (food.nutrition() * 2.0F) : 0.0F);
			}
		}
		if (!player.capabilities.isCreativeMode) {
			stack.shrink(1);
		}
		return stack;
	}

	public static int awardRecipes(net.minecraft.entity.player.EntityPlayer player, java.util.Collection<?> recipes) {
		return 0;
	}

	public static void onEquipItem(EntityLivingBase entity, net.minecraft.inventory.EntityEquipmentSlot slot, ItemStack previous, ItemStack current) {
	}

	// --- FluidStack (1.12.2 has null for no fluid: the calls take a null stack as empty) --------------------------------

	public static boolean isEmpty(@Nullable net.minecraftforge.fluids.FluidStack stack) {
		return stack == null || stack.amount <= 0;
	}

	public static int getAmount(@Nullable net.minecraftforge.fluids.FluidStack stack) {
		return stack == null ? 0 : stack.amount;
	}

	public static void setAmount(@Nullable net.minecraftforge.fluids.FluidStack stack, int amount) {
		if (stack != null) {
			stack.amount = amount;
		}
	}

	public static void grow(@Nullable net.minecraftforge.fluids.FluidStack stack, int amount) {
		if (stack != null) {
			stack.amount += amount;
		}
	}

	public static void shrink(@Nullable net.minecraftforge.fluids.FluidStack stack, int amount) {
		if (stack != null) {
			stack.amount -= amount;
		}
	}

	@Nullable
	public static net.minecraftforge.fluids.FluidStack copyWithAmount(@Nullable net.minecraftforge.fluids.FluidStack stack, int amount) {
		return stack == null ? null : new net.minecraftforge.fluids.FluidStack(stack, amount);
	}

	public static ITextComponent getHoverName(@Nullable net.minecraftforge.fluids.FluidStack stack) {
		return new TextComponentString(stack == null ? "" : stack.getLocalizedName());
	}

	public static String getDescriptionId(@Nullable net.minecraftforge.fluids.FluidStack stack) {
		return stack == null ? "" : stack.getUnlocalizedName();
	}

	public static boolean is(@Nullable net.minecraftforge.fluids.FluidStack stack, net.minecraftforge.fluids.Fluid fluid) {
		return stack != null && stack.getFluid() == fluid;
	}

	public static boolean is(@Nullable net.minecraftforge.fluids.FluidStack stack, io.github.xponer2.translator.util.TagKey<?> tag) {
		return stack != null && ItemBridges.is(stack.getFluid(), tag);
	}

	public static Holder<net.minecraftforge.fluids.Fluid> getFluidHolder(net.minecraftforge.fluids.FluidStack stack) {
		return VanillaRedirects.builtInRegistryHolder(stack.getFluid());
	}

	public static io.github.xponer2.translator.component.DataComponentPatch getComponentsPatch(net.minecraftforge.fluids.FluidStack stack) {
		return io.github.xponer2.translator.component.DataComponentPatch.EMPTY;
	}

	public static net.minecraft.nbt.NBTBase saveOptional(@Nullable net.minecraftforge.fluids.FluidStack stack,
			io.github.xponer2.translator.registry.HolderLookup.Provider registries) {
		return stack == null ? new NBTTagCompound() : stack.writeToNBT(new NBTTagCompound());
	}

	@Nullable
	public static Object get(net.minecraftforge.fluids.FluidStack stack, io.github.xponer2.translator.component.DataComponentType<?> type) {
		return null;
	}

	public static Object getOrDefault(net.minecraftforge.fluids.FluidStack stack, io.github.xponer2.translator.component.DataComponentType<?> type,
			Object fallback) {
		return fallback;
	}

	@Nullable
	public static Object set(net.minecraftforge.fluids.FluidStack stack, io.github.xponer2.translator.component.DataComponentType<?> type, Object value) {
		return null;
	}

	@Nullable
	public static Object remove(net.minecraftforge.fluids.FluidStack stack, io.github.xponer2.translator.component.DataComponentType<?> type) {
		return null;
	}

	/** 1.21 {@code new FluidStack(holder, amount, patch)}. */
	public static net.minecraftforge.fluids.FluidStack newFluidStack(Holder<net.minecraftforge.fluids.Fluid> fluid, int amount,
			io.github.xponer2.translator.component.DataComponentPatch patch) {
		return new net.minecraftforge.fluids.FluidStack(fluid.value(), amount);
	}

	public static net.minecraftforge.fluids.FluidStack newFluidStack(Holder<net.minecraftforge.fluids.Fluid> fluid, int amount) {
		return new net.minecraftforge.fluids.FluidStack(fluid.value(), amount);
	}

	// --- EnumActionResult, DimensionType, EnumFacing, Vec3d ----------------------------------------------------------

	/** 1.21 CONSUME (the action happened, no hand swing): 1.12.2's SUCCESS. */
	public static final net.minecraft.util.EnumActionResult CONSUME = net.minecraft.util.EnumActionResult.SUCCESS;
	public static final net.minecraft.util.EnumActionResult SUCCESS_NO_ITEM_USED = net.minecraft.util.EnumActionResult.SUCCESS;
	public static final net.minecraft.util.EnumActionResult CONSUME_PARTIAL = net.minecraft.util.EnumActionResult.SUCCESS;

	public static boolean consumesAction(net.minecraft.util.EnumActionResult result) {
		return result == net.minecraft.util.EnumActionResult.SUCCESS;
	}

	public static boolean shouldSwing(net.minecraft.util.EnumActionResult result) {
		return result == net.minecraft.util.EnumActionResult.SUCCESS;
	}

	public static boolean indicateItemUse(net.minecraft.util.EnumActionResult result) {
		return result == net.minecraft.util.EnumActionResult.SUCCESS;
	}

	public static int minY(net.minecraft.world.DimensionType type) {
		return 0;
	}

	public static int height(net.minecraft.world.DimensionType type) {
		return 256;
	}

	public static int logicalHeight(net.minecraft.world.DimensionType type) {
		return type == net.minecraft.world.DimensionType.NETHER ? 128 : 256;
	}

	public static boolean hasSkyLight(net.minecraft.world.DimensionType type) {
		return type == net.minecraft.world.DimensionType.OVERWORLD;
	}

	public static boolean hasCeiling(net.minecraft.world.DimensionType type) {
		return type == net.minecraft.world.DimensionType.NETHER;
	}

	public static boolean ultraWarm(net.minecraft.world.DimensionType type) {
		return type == net.minecraft.world.DimensionType.NETHER;
	}

	public static boolean natural(net.minecraft.world.DimensionType type) {
		return type == net.minecraft.world.DimensionType.OVERWORLD;
	}

	public static boolean bedWorks(net.minecraft.world.DimensionType type) {
		return type == net.minecraft.world.DimensionType.OVERWORLD;
	}

	public static boolean respawnAnchorWorks(net.minecraft.world.DimensionType type) {
		return type == net.minecraft.world.DimensionType.NETHER;
	}

	public static boolean piglinSafe(net.minecraft.world.DimensionType type) {
		return type == net.minecraft.world.DimensionType.NETHER;
	}

	public static double coordinateScale(net.minecraft.world.DimensionType type) {
		return type == net.minecraft.world.DimensionType.NETHER ? 8.0D : 1.0D;
	}

	public static float ambientLight(net.minecraft.world.DimensionType type) {
		return type == net.minecraft.world.DimensionType.NETHER ? 0.1F : 0.0F;
	}

	public static EnumFacing getClockWise(EnumFacing facing, EnumFacing.Axis axis) {
		return facing.getAxis() == axis ? facing : facing.rotateAround(axis);
	}

	public static EnumFacing getCounterClockWise(EnumFacing facing, EnumFacing.Axis axis) {
		return facing.getAxis() == axis ? facing : facing.rotateAround(axis).rotateAround(axis).rotateAround(axis);
	}

	public static org.joml.Vector3f step(EnumFacing facing) {
		return new org.joml.Vector3f(facing.getXOffset(), facing.getYOffset(), facing.getZOffset());
	}

	public static Vec3i getNormal(EnumFacing facing) {
		return facing.getDirectionVec();
	}

	/** The rotation from up to the facing (1.21 Direction.getRotation). */
	public static org.joml.Quaternionf getRotation(EnumFacing facing) {
		float half = (float) Math.PI / 2.0F;
		switch (facing) {
		case DOWN:
			return new org.joml.Quaternionf().rotationX((float) Math.PI);
		case UP:
			return new org.joml.Quaternionf();
		case NORTH:
			return new org.joml.Quaternionf().rotationXYZ(half, 0.0F, (float) Math.PI);
		case SOUTH:
			return new org.joml.Quaternionf().rotationX(half);
		case WEST:
			return new org.joml.Quaternionf().rotationXYZ(half, 0.0F, half);
		default:
			return new org.joml.Quaternionf().rotationXYZ(half, 0.0F, -half);
		}
	}

	public static Vec3d relative(Vec3d vec, EnumFacing facing, double distance) {
		return vec.add(facing.getXOffset() * distance, facing.getYOffset() * distance, facing.getZOffset() * distance);
	}

	public static Vec3d lerp(Vec3d vec, Vec3d to, double delta) {
		return new Vec3d(vec.x + (to.x - vec.x) * delta, vec.y + (to.y - vec.y) * delta, vec.z + (to.z - vec.z) * delta);
	}

	public static double horizontalDistance(Vec3d vec) {
		return Math.sqrt(vec.x * vec.x + vec.z * vec.z);
	}

	public static double horizontalDistanceSqr(Vec3d vec) {
		return vec.x * vec.x + vec.z * vec.z;
	}

	public static Vec3d reverse(Vec3d vec) {
		return vec.scale(-1.0D);
	}

	public static boolean closerThan(Vec3d vec, Object position, double distance) {
		return position instanceof Vec3d other && vec.squareDistanceTo(other) < distance * distance;
	}

	public static Vec3d multiply(Vec3d vec, double x, double y, double z) {
		return new Vec3d(vec.x * x, vec.y * y, vec.z * z);
	}

	public static Vec3d multiply(Vec3d vec, Vec3d factor) {
		return multiply(vec, factor.x, factor.y, factor.z);
	}

	public static Vec3d horizontal(Vec3d vec) {
		return new Vec3d(vec.x, 0.0D, vec.z);
	}

	// --- IBlockAccess (BlockGetter, BlockAndTintGetter) --------------------------------------------------------------

	public static io.github.xponer2.translator.client.ModelData getModelData(net.minecraft.world.IBlockAccess world, BlockPos pos) {
		return io.github.xponer2.translator.client.ModelData.EMPTY;
	}

	public static int getMinBuildHeight(net.minecraft.world.IBlockAccess world) {
		return 0;
	}

	public static int getMaxBuildHeight(net.minecraft.world.IBlockAccess world) {
		return 256;
	}

	public static int getHeight(net.minecraft.world.IBlockAccess world) {
		return 256;
	}

	public static float getShade(net.minecraft.world.IBlockAccess world, EnumFacing face, boolean shade) {
		return WorldBridges.getShade(null, face, shade);
	}

	/** A 1.21 color resolver (biome, x, z -> color, carried with the API) for the biome at the position. */
	public static int getBlockTint(net.minecraft.world.IBlockAccess world, BlockPos pos, Object resolver) {
		try {
			for (var method : resolver.getClass().getMethods()) {
				if (method.getParameterCount() == 3 && method.getReturnType() == int.class) {
					method.setAccessible(true);
					return (int) method.invoke(resolver, world.getBiome(pos), (double) pos.getX(), (double) pos.getZ());
				}
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			// below
		}
		return 0xFFFFFF;
	}

	// --- TileEntity, Holder, BlockSource, EntityPlayerMP, MinecraftServer --------------------------------------------

	/** The block entity type of a block entity (one whose type the Translator made: by its class). */
	@Nullable
	public static io.github.xponer2.translator.registry.BlockEntityType<?> getType(TileEntity tile) {
		if (tile instanceof io.github.xponer2.translator.block.BlockEntity blockEntity) {
			return blockEntity.getType();
		}
		var registry = io.github.xponer2.translator.util.BuiltInRegistries.byKey(io.github.xponer2.translator.util.Registries.BLOCK_ENTITY_TYPE);
		for (Object type : registry) {
			if (type instanceof io.github.xponer2.translator.registry.BlockEntityType<?> blockEntityType
					&& blockEntityType.getTileClass() == tile.getClass()) {
				return blockEntityType;
			}
		}
		return null;
	}

	public static NBTTagCompound getPersistentData(TileEntity tile) {
		return tile.getTileData();
	}

	public static void requestModelDataUpdate(TileEntity tile) {
	}

	public static void handleUpdateTag(TileEntity tile, NBTTagCompound tag, io.github.xponer2.translator.registry.HolderLookup.Provider registries) {
		tile.handleUpdateTag(tag);
	}

	@Nullable
	public static io.github.xponer2.translator.registry.ResourceKey<?> getKey(Holder<?> holder) {
		return holder.unwrapKey().orElse(null);
	}

	public static boolean is(Holder<?> holder, Holder<?> other) {
		return other != null && holder.value() == other.value();
	}

	public static net.minecraft.world.WorldServer level(io.github.xponer2.translator.block.BlockSource source) {
		return (net.minecraft.world.WorldServer) source.level();
	}

	public static Vec3d center(io.github.xponer2.translator.block.BlockSource source) {
		BlockPos pos = source.pos();
		return new Vec3d(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
	}

	public static void sendSystemMessage(net.minecraft.entity.player.EntityPlayerMP player, ITextComponent message, boolean actionBar) {
		player.sendStatusMessage(message, actionBar);
	}

	public static void sendSystemMessage(net.minecraft.entity.player.EntityPlayer player, ITextComponent message) {
		player.sendMessage(message);
	}

	/** Moves the player to a place in a level (another dimension through Forge's teleporter). */
	public static void teleportTo(net.minecraft.entity.player.EntityPlayerMP player, net.minecraft.world.WorldServer level, double x, double y, double z,
			float yaw, float pitch) {
		int dimension = level.provider.getDimension();
		if (player.dimension != dimension) {
			player.changeDimension(dimension, (world, entity, entityYaw) -> entity.setLocationAndAngles(x, y, z, yaw, pitch));
		}
		player.connection.setPlayerLocation(x, y, z, yaw, pitch);
	}

	public static double entityInteractionRange(net.minecraft.entity.player.EntityPlayer player) {
		return 3.0D;
	}

	public static double blockInteractionRange(net.minecraft.entity.player.EntityPlayer player) {
		return player.getEntityAttribute(net.minecraft.entity.player.EntityPlayer.REACH_DISTANCE).getAttributeValue();
	}

	public static io.github.xponer2.translator.recipe.RecipeManager getRecipeManager(net.minecraft.server.MinecraftServer server) {
		return ApiBridges.getRecipeManager(server.getWorld(0));
	}

	// --- text colors, effects, crafting, levels' damage sources, server connections -----------------------------------

	public static Style withColor(Style style, @Nullable io.github.xponer2.translator.text.TextColor color) {
		return style.createShallowCopy().setColor(color == null ? null : color.nearest());
	}

	@Nullable
	public static io.github.xponer2.translator.text.TextColor getColor(Style style) {
		return io.github.xponer2.translator.text.TextColor.fromLegacyFormat(style.getColor());
	}

	public static io.github.xponer2.translator.entity.MobEffectCategory getCategory(net.minecraft.potion.Potion potion) {
		return io.github.xponer2.translator.entity.MobEffectCategory.of(potion);
	}

	public static io.github.xponer2.translator.recipe.CraftingInput asCraftInput(net.minecraft.inventory.InventoryCrafting inventory) {
		return io.github.xponer2.translator.recipe.CraftingInput.of(inventory);
	}

	public static io.github.xponer2.translator.recipe.CraftingInput.Positioned asPositionedCraftInput(net.minecraft.inventory.InventoryCrafting inventory) {
		return io.github.xponer2.translator.recipe.CraftingInput.ofPositioned(inventory);
	}

	public static io.github.xponer2.translator.world.DamageSources damageSources(net.minecraft.world.World world) {
		return io.github.xponer2.translator.world.DamageSources.INSTANCE;
	}

	public static io.github.xponer2.translator.world.DamageSources damageSources(net.minecraft.entity.Entity entity) {
		return io.github.xponer2.translator.world.DamageSources.INSTANCE;
	}

	public static void send(net.minecraft.network.NetHandlerPlayServer connection, io.github.xponer2.translator.network.CustomPacketPayload payload) {
		io.github.xponer2.translator.network.PacketDistributor.sendToPlayer(connection.player, payload);
	}

	/** Right-to-left text (1.21 visual order): 1.12.2 draws text as it is. */
	public static List<?> getVisualOrder(net.minecraft.util.text.translation.LanguageMap language, List<?> texts) {
		return texts;
	}

	public static ITextComponent getVisualOrder(net.minecraft.util.text.translation.LanguageMap language, ITextComponent text) {
		return text;
	}

	/** The operating system (1.21 Util.getPlatform / OS, carried with the API) the game runs on. */
	public static Object getOSType() {
		String os = System.getProperty("os.name", "").toLowerCase(java.util.Locale.ROOT);
		String name = os.contains("win") ? "WINDOWS" : os.contains("mac") ? "OSX" : os.contains("linux") || os.contains("unix") ? "LINUX" : "UNKNOWN";
		try {
			@SuppressWarnings({ "unchecked", "rawtypes" })
			Object value = Enum.valueOf((Class) Class.forName("net.minecraft.Util$OS", true, net.minecraft.launchwrapper.Launch.classLoader), name);
			return value;
		} catch (ReflectiveOperationException | RuntimeException e) {
			return null;
		}
	}

	public static Object getPlatform() {
		return getOSType();
	}

	// --- the loader's versions, id maps ------------------------------------------------------------------------------

	/** The versions NeoForge reports (1.21 FMLLoader.versionInfo(), a record carried with the API): 1.21.1's. */
	@Nullable
	public static Object versionInfo() {
		try {
			Class<?> type = Class.forName("net.neoforged.fml.loading.VersionInfo", true, net.minecraft.launchwrapper.Launch.classLoader);
			for (var constructor : type.getDeclaredConstructors()) {
				Class<?>[] parameters = constructor.getParameterTypes();
				if (parameters.length == 4 && java.util.Arrays.stream(parameters).allMatch(p -> p == String.class)) {
					constructor.setAccessible(true);
					return constructor.newInstance("21.1.77", "4.0.24", "1.21.1", "20240808.144430");
				}
			}
		} catch (ReflectiveOperationException | RuntimeException e) {
			// none
		}
		return null;
	}

	public static int getIdOrThrow(net.minecraft.util.ObjectIntIdentityMap<Object> map, Object value) {
		int id = map.get(value);
		if (id < 0) {
			throw new IllegalArgumentException("No id for " + value);
		}
		return id;
	}

	public static int getId(net.minecraft.util.ObjectIntIdentityMap<Object> map, Object value) {
		return map.get(value);
	}

	// --- inventories, slots, structure boxes, baked quads, fluid handlers --------------------------------------------

	/** Into the player's inventory, dropped at the player when it doesn't fit. */
	public static void placeItemBackInInventory(net.minecraft.entity.player.InventoryPlayer inventory, ItemStack stack) {
		if (!stack.isEmpty() && !inventory.addItemStackToInventory(stack) && !stack.isEmpty()) {
			inventory.player.dropItem(stack, false);
		}
	}

	public static void placeItemBackInInventory(net.minecraft.entity.player.InventoryPlayer inventory, ItemStack stack, boolean sendPacket) {
		placeItemBackInInventory(inventory, stack);
	}

	public static int countItem(net.minecraft.inventory.IInventory inventory, net.minecraft.item.Item item) {
		int count = 0;
		for (int i = 0; i < inventory.getSizeInventory(); i++) {
			ItemStack stack = inventory.getStackInSlot(i);
			if (stack.getItem() == item) {
				count += stack.getCount();
			}
		}
		return count;
	}

	public static int getContainerSlot(net.minecraft.inventory.Slot slot) {
		return slot.getSlotIndex();
	}

	public static boolean allowModification(net.minecraft.inventory.Slot slot, net.minecraft.entity.player.EntityPlayer player) {
		return slot.isItemValid(slot.getStack()) || slot.canTakeStack(player);
	}

	public static boolean mayPickup(net.minecraft.inventory.Slot slot, net.minecraft.entity.player.EntityPlayer player) {
		return slot.canTakeStack(player);
	}

	public static boolean mayPlace(net.minecraft.inventory.Slot slot, ItemStack stack) {
		return slot.isItemValid(stack);
	}

	/** Puts what fits of a stack into the slot, gives back the rest. */
	public static ItemStack safeInsert(net.minecraft.inventory.Slot slot, ItemStack stack, int max) {
		if (stack.isEmpty() || !slot.isItemValid(stack)) {
			return stack;
		}
		ItemStack current = slot.getStack();
		int room = Math.min(Math.min(max, stack.getCount()), slot.getItemStackLimit(stack) - current.getCount());
		if (room <= 0) {
			return stack;
		}
		if (current.isEmpty()) {
			slot.putStack(stack.splitStack(room));
		} else if (ItemStack.areItemsEqual(current, stack) && ItemStack.areItemStackTagsEqual(current, stack)) {
			stack.shrink(room);
			current.grow(room);
			slot.putStack(current);
		}
		return stack;
	}

	public static ItemStack safeInsert(net.minecraft.inventory.Slot slot, ItemStack stack) {
		return safeInsert(slot, stack, stack.getCount());
	}

	public static ItemStack safeTake(net.minecraft.inventory.Slot slot, int count, int max, net.minecraft.entity.player.EntityPlayer player) {
		if (!slot.canTakeStack(player)) {
			return ItemStack.EMPTY;
		}
		ItemStack taken = slot.decrStackSize(Math.min(count, max));
		if (!taken.isEmpty()) {
			slot.onTake(player, taken);
		}
		return taken;
	}

	public static int getXSpan(net.minecraft.world.gen.structure.StructureBoundingBox box) {
		return box.getXSize();
	}

	public static int getYSpan(net.minecraft.world.gen.structure.StructureBoundingBox box) {
		return box.getYSize();
	}

	public static int getZSpan(net.minecraft.world.gen.structure.StructureBoundingBox box) {
		return box.getZSize();
	}

	public static int minX(net.minecraft.world.gen.structure.StructureBoundingBox box) {
		return box.minX;
	}

	public static int minY(net.minecraft.world.gen.structure.StructureBoundingBox box) {
		return box.minY;
	}

	public static int minZ(net.minecraft.world.gen.structure.StructureBoundingBox box) {
		return box.minZ;
	}

	public static int maxX(net.minecraft.world.gen.structure.StructureBoundingBox box) {
		return box.maxX;
	}

	public static int maxY(net.minecraft.world.gen.structure.StructureBoundingBox box) {
		return box.maxY;
	}

	public static int maxZ(net.minecraft.world.gen.structure.StructureBoundingBox box) {
		return box.maxZ;
	}

	public static BlockPos getCenter(net.minecraft.world.gen.structure.StructureBoundingBox box) {
		return new BlockPos(box.minX + (box.maxX - box.minX + 1) / 2, box.minY + (box.maxY - box.minY + 1) / 2, box.minZ + (box.maxZ - box.minZ + 1) / 2);
	}

	public static net.minecraft.world.gen.structure.StructureBoundingBox inflatedBy(net.minecraft.world.gen.structure.StructureBoundingBox box, int amount) {
		return new net.minecraft.world.gen.structure.StructureBoundingBox(box.minX - amount, box.minY - amount, box.minZ - amount, box.maxX + amount,
				box.maxY + amount, box.maxZ + amount);
	}

	public static net.minecraft.world.gen.structure.StructureBoundingBox moved(net.minecraft.world.gen.structure.StructureBoundingBox box, int x, int y,
			int z) {
		return new net.minecraft.world.gen.structure.StructureBoundingBox(box.minX + x, box.minY + y, box.minZ + z, box.maxX + x, box.maxY + y,
				box.maxZ + z);
	}

	public static boolean isInside(net.minecraft.world.gen.structure.StructureBoundingBox box, Vec3i pos) {
		return box.isVecInside(pos);
	}

	public static boolean intersects(net.minecraft.world.gen.structure.StructureBoundingBox box, net.minecraft.world.gen.structure.StructureBoundingBox other) {
		return box.intersectsWith(other);
	}

	/** 1.21 {@code new BoundingBox(pos)}: the box of one block. */
	public static net.minecraft.world.gen.structure.StructureBoundingBox newStructureBoundingBox(BlockPos pos) {
		return new net.minecraft.world.gen.structure.StructureBoundingBox(pos, pos);
	}

	public static boolean isShade(net.minecraft.client.renderer.block.model.BakedQuad quad) {
		return quad.shouldApplyDiffuseLighting();
	}

	public static boolean hasAmbientOcclusion(net.minecraft.client.renderer.block.model.BakedQuad quad) {
		return true;
	}

	public static int getDirection(net.minecraft.client.renderer.block.model.BakedQuad quad) {
		return quad.getFace().getIndex();
	}

	/** 1.21 {@code new BakedQuad(vertices, tint, face, sprite, shade)}: the 1.12.2 item vertex format. */
	public static net.minecraft.client.renderer.block.model.BakedQuad newBakedQuad(int[] vertices, int tint, EnumFacing face,
			net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, boolean shade) {
		return new net.minecraft.client.renderer.block.model.BakedQuad(vertices, tint, face, sprite, shade,
				net.minecraft.client.renderer.vertex.DefaultVertexFormats.ITEM);
	}

	public static net.minecraft.client.renderer.block.model.BakedQuad newBakedQuad(int[] vertices, int tint, EnumFacing face,
			net.minecraft.client.renderer.texture.TextureAtlasSprite sprite, boolean shade, boolean ambientOcclusion) {
		return newBakedQuad(vertices, tint, face, sprite, shade);
	}

	// IFluidHandler: the 1.21 FluidAction (EXECUTE, SIMULATE; carried with the API) is 1.12.2's doFill / doDrain

	private static boolean execute(Object action) {
		return action == null || "EXECUTE".equals(action.toString());
	}

	public static int getTanks(net.minecraftforge.fluids.capability.IFluidHandler handler) {
		var properties = handler.getTankProperties();
		return properties == null ? 0 : properties.length;
	}

	@Nullable
	public static net.minecraftforge.fluids.FluidStack getFluidInTank(net.minecraftforge.fluids.capability.IFluidHandler handler, int tank) {
		var properties = handler.getTankProperties();
		return properties == null || tank < 0 || tank >= properties.length ? null : properties[tank].getContents();
	}

	public static int getTankCapacity(net.minecraftforge.fluids.capability.IFluidHandler handler, int tank) {
		var properties = handler.getTankProperties();
		return properties == null || tank < 0 || tank >= properties.length ? 0 : properties[tank].getCapacity();
	}

	public static boolean isFluidValid(net.minecraftforge.fluids.capability.IFluidHandler handler, int tank,
			@Nullable net.minecraftforge.fluids.FluidStack stack) {
		var properties = handler.getTankProperties();
		return properties != null && tank >= 0 && tank < properties.length && stack != null && properties[tank].canFillFluidType(stack);
	}

	public static int fill(net.minecraftforge.fluids.capability.IFluidHandler handler, @Nullable net.minecraftforge.fluids.FluidStack stack, Object action) {
		return stack == null ? 0 : handler.fill(stack, execute(action));
	}

	@Nullable
	public static net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.capability.IFluidHandler handler,
			@Nullable net.minecraftforge.fluids.FluidStack stack, Object action) {
		return stack == null ? null : handler.drain(stack, execute(action));
	}

	@Nullable
	public static net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.capability.IFluidHandler handler, int amount, Object action) {
		return handler.drain(amount, execute(action));
	}

	// --- ResourceLocation --------------------------------------------------------------------------------------------

	public static ResourceLocation withPrefix(ResourceLocation id, String prefix) {
		return new ResourceLocation(id.getNamespace(), prefix + id.getPath());
	}

	public static ResourceLocation withSuffix(ResourceLocation id, String suffix) {
		return new ResourceLocation(id.getNamespace(), id.getPath() + suffix);
	}

	public static ResourceLocation withPath(ResourceLocation id, String path) {
		return new ResourceLocation(id.getNamespace(), path);
	}

	public static ResourceLocation withPath(ResourceLocation id, UnaryOperator<String> path) {
		return new ResourceLocation(id.getNamespace(), path.apply(id.getPath()));
	}

	public static String toLanguageKey(ResourceLocation id) {
		return id.getNamespace() + "." + id.getPath();
	}

	public static String toLanguageKey(ResourceLocation id, String prefix) {
		return prefix + "." + toLanguageKey(id);
	}

	public static String toLanguageKey(ResourceLocation id, String prefix, String suffix) {
		return prefix + "." + toLanguageKey(id) + "." + suffix;
	}

	public static String toShortLanguageKey(ResourceLocation id) {
		return "minecraft".equals(id.getNamespace()) ? id.getPath() : toLanguageKey(id);
	}

	public static String toDebugFileName(ResourceLocation id) {
		return id.toString().replace('/', '_').replace(':', '_');
	}

	// --- BlockPos.MutableBlockPos (the setters return the position in 1.21) ------------------------------------------

	public static BlockPos.MutableBlockPos setX(BlockPos.MutableBlockPos pos, int x) {
		return pos.setPos(x, pos.getY(), pos.getZ());
	}

	public static BlockPos.MutableBlockPos setY(BlockPos.MutableBlockPos pos, int y) {
		return pos.setPos(pos.getX(), y, pos.getZ());
	}

	public static BlockPos.MutableBlockPos setZ(BlockPos.MutableBlockPos pos, int z) {
		return pos.setPos(pos.getX(), pos.getY(), z);
	}

	public static BlockPos.MutableBlockPos set(BlockPos.MutableBlockPos pos, long packed) {
		return pos.setPos(BlockPos.fromLong(packed));
	}

	public static BlockPos.MutableBlockPos set(BlockPos.MutableBlockPos pos, int x, int y, int z) {
		return pos.setPos(x, y, z);
	}

	public static BlockPos.MutableBlockPos set(BlockPos.MutableBlockPos pos, Vec3i other) {
		return pos.setPos(other);
	}

	public static BlockPos.MutableBlockPos move(BlockPos.MutableBlockPos pos, int x, int y, int z) {
		return pos.setPos(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
	}

	public static BlockPos.MutableBlockPos move(BlockPos.MutableBlockPos pos, Vec3i offset) {
		return move(pos, offset.getX(), offset.getY(), offset.getZ());
	}

	public static BlockPos.MutableBlockPos setWithOffset(BlockPos.MutableBlockPos pos, Vec3i origin, int x, int y, int z) {
		return pos.setPos(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
	}

	public static BlockPos.MutableBlockPos setWithOffset(BlockPos.MutableBlockPos pos, Vec3i origin, EnumFacing face) {
		return pos.setPos(origin.getX() + face.getXOffset(), origin.getY() + face.getYOffset(), origin.getZ() + face.getZOffset());
	}

	public static BlockPos.MutableBlockPos setWithOffset(BlockPos.MutableBlockPos pos, Vec3i origin, Vec3i offset) {
		return setWithOffset(pos, origin, offset.getX(), offset.getY(), offset.getZ());
	}

	// --- AxisAlignedBB -----------------------------------------------------------------------------------------------

	public static Optional<Vec3d> clip(AxisAlignedBB box, Vec3d from, Vec3d to) {
		RayTraceResult hit = box.calculateIntercept(from, to);
		return hit == null ? Optional.empty() : Optional.of(hit.hitVec);
	}

	public static boolean contains(AxisAlignedBB box, double x, double y, double z) {
		return x >= box.minX && x < box.maxX && y >= box.minY && y < box.maxY && z >= box.minZ && z < box.maxZ;
	}

	public static double getXsize(AxisAlignedBB box) {
		return box.maxX - box.minX;
	}

	public static double getYsize(AxisAlignedBB box) {
		return box.maxY - box.minY;
	}

	public static double getZsize(AxisAlignedBB box) {
		return box.maxZ - box.minZ;
	}

	public static AxisAlignedBB expandTowards(AxisAlignedBB box, Vec3d motion) {
		return box.expand(motion.x, motion.y, motion.z);
	}

	public static AxisAlignedBB expandTowards(AxisAlignedBB box, double x, double y, double z) {
		return box.expand(x, y, z);
	}

	public static AxisAlignedBB deflate(AxisAlignedBB box, double x, double y, double z) {
		return box.grow(-x, -y, -z);
	}

	public static AxisAlignedBB deflate(AxisAlignedBB box, double amount) {
		return box.grow(-amount);
	}

	public static AxisAlignedBB inflate(AxisAlignedBB box, double amount) {
		return box.grow(amount);
	}

	public static AxisAlignedBB inflate(AxisAlignedBB box, double x, double y, double z) {
		return box.grow(x, y, z);
	}

	public static AxisAlignedBB move(AxisAlignedBB box, Vec3d offset) {
		return box.offset(offset);
	}

	// --- NBTTagCompound ----------------------------------------------------------------------------------------------

	public static NBTTagCompound copyTag(NBTTagCompound tag) {
		return tag.copy();
	}

	public static NBTTagCompound getUnsafe(NBTTagCompound tag) {
		return tag;
	}

	public static NBTTagCompound merge(NBTTagCompound tag, NBTTagCompound other) {
		tag.merge(other);
		return tag;
	}

	public static void putLongArray(NBTTagCompound tag, String key, long[] values) {
		tag.setTag(key, new NBTTagLongArray(values));
	}

	public static void putLongArray(NBTTagCompound tag, String key, List<Long> values) {
		tag.setTag(key, new NBTTagLongArray(values));
	}

	public static void putIntArray(NBTTagCompound tag, String key, List<Integer> values) {
		tag.setIntArray(key, values.stream().mapToInt(Integer::intValue).toArray());
	}

	private static final java.lang.reflect.Field LONGS = longsField();

	private static java.lang.reflect.Field longsField() {
		for (var field : NBTTagLongArray.class.getDeclaredFields()) {
			if (field.getType() == long[].class) {
				field.setAccessible(true);
				return field;
			}
		}
		return null;
	}

	/** The long array of a key (1.12.2 has the tag but no getter), empty if there is none. */
	public static long[] getLongArray(NBTTagCompound tag, String key) {
		var value = tag.getTag(key);
		if (value instanceof NBTTagLongArray && LONGS != null) {
			try {
				return (long[]) LONGS.get(value);
			} catch (IllegalAccessException e) {
				// below
			}
		}
		return new long[0];
	}

	// --- text --------------------------------------------------------------------------------------------------------

	/** A copy of the text without its siblings (1.21 plainCopy): its own content and style. */
	public static MutableComponent plainCopy(ITextComponent text) {
		ITextComponent copy;
		if (text instanceof net.minecraft.util.text.TextComponentTranslation translation) {
			copy = new TranslatableComponent(translation.getKey(), translation.getFormatArgs());
		} else {
			copy = new LiteralComponent(text.getUnformattedComponentText());
		}
		copy.setStyle(text.getStyle().createShallowCopy());
		return (MutableComponent) copy;
	}

	public static String getString(ITextComponent text, int maxLength) {
		String plain = text.getUnformattedText();
		return plain.length() <= maxLength ? plain : plain.substring(0, maxLength);
	}

	public static Style applyFormat(Style style, TextFormatting format) {
		return format(style.createShallowCopy(), format);
	}

	public static Style applyFormats(Style style, TextFormatting... formats) {
		Style copy = style.createShallowCopy();
		for (TextFormatting format : formats) {
			copy = format(copy, format);
		}
		return copy;
	}

	private static Style format(Style style, TextFormatting format) {
		switch (format) {
		case BOLD:
			return style.setBold(true);
		case ITALIC:
			return style.setItalic(true);
		case UNDERLINE:
			return style.setUnderlined(true);
		case STRIKETHROUGH:
			return style.setStrikethrough(true);
		case OBFUSCATED:
			return style.setObfuscated(true);
		case RESET:
			return new Style();
		default:
			return style.setColor(format);
		}
	}

	public static Style withUnderlined(Style style, @Nullable Boolean value) {
		return style.createShallowCopy().setUnderlined(value);
	}

	public static Style withStrikethrough(Style style, @Nullable Boolean value) {
		return style.createShallowCopy().setStrikethrough(value);
	}

	public static Style withObfuscated(Style style, @Nullable Boolean value) {
		return style.createShallowCopy().setObfuscated(value);
	}

	public static Style withInsertion(Style style, @Nullable String insertion) {
		return style.createShallowCopy().setInsertion(insertion);
	}

	/** 1.12.2 text has one font. */
	public static Style withFont(Style style, @Nullable ResourceLocation font) {
		return style;
	}

	/** 1.21 {@code new HoverEvent(action, value)}: text as it is, anything else (items, entities) by its text. */
	public static HoverEvent newHoverEvent(HoverEvent.Action action, Object value) {
		if (value instanceof ITextComponent text) {
			return new HoverEvent(action, text);
		}
		if (value instanceof ItemStack stack) {
			return new HoverEvent(HoverEvent.Action.SHOW_ITEM, new TextComponentString(stack.writeToNBT(new NBTTagCompound()).toString()));
		}
		return new HoverEvent(action, new TextComponentString(String.valueOf(value)));
	}

	public static Object getValue(HoverEvent event, HoverEvent.Action action) {
		return event.getAction() == action ? event.getValue() : null;
	}

	// --- EnumDyeColor ------------------------------------------------------------------------------------------------

	public static int getTextureDiffuseColor(EnumDyeColor color) {
		return 0xFF000000 | color.getColorValue();
	}

	public static int getTextColor(EnumDyeColor color) {
		return color.getColorValue();
	}

	public static int getFireworkColor(EnumDyeColor color) {
		return color.getColorValue();
	}

	public static int getId(EnumDyeColor color) {
		return color.getMetadata();
	}

	public static MapColor getMapColor(EnumDyeColor color) {
		return MapColor.getBlockColor(color);
	}

	public static io.github.xponer2.translator.util.TagKey<net.minecraft.item.Item> getTag(EnumDyeColor color) {
		return io.github.xponer2.translator.util.TagKey.create(io.github.xponer2.translator.util.Registries.ITEM,
				new ResourceLocation("c", "dyes/" + color.getName()));
	}

	// --- Block -------------------------------------------------------------------------------------------------------

	/** The map color of the block's default state (its material's when the block needs a world for it). */
	@SuppressWarnings("deprecation")
	public static MapColor defaultMapColor(Block block) {
		IBlockState state = block.getDefaultState();
		try {
			return block.getMapColor(state, null, BlockPos.ORIGIN);
		} catch (RuntimeException e) {
			return state.getMaterial().getMaterialMapColor();
		}
	}

	public static MutableComponent getName(Block block) {
		return new TranslatableComponent(block.getTranslationKey() + ".name");
	}

	public static String getDescriptionId(Block block) {
		return block.getTranslationKey();
	}

	@SuppressWarnings("deprecation")
	public static float getExplosionResistance(Block block) {
		return block.getExplosionResistance(null) * 5.0F / 3.0F;
	}

	/** The state with the properties of another state that this block has too. */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static IBlockState withPropertiesOf(Block block, IBlockState other) {
		IBlockState state = block.getDefaultState();
		for (var property : other.getPropertyKeys()) {
			if (state.getPropertyKeys().contains(property)) {
				state = state.withProperty((net.minecraft.block.properties.IProperty) property, (Comparable) other.getValue(property));
			}
		}
		return state;
	}

	// --- TileEntity --------------------------------------------------------------------------------------------------

	public static void setChanged(TileEntity tile) {
		tile.markDirty();
	}

	public static io.github.xponer2.translator.client.ModelData getModelData(TileEntity tile) {
		return io.github.xponer2.translator.client.ModelData.EMPTY;
	}

	public static void loadWithComponents(TileEntity tile, NBTTagCompound tag, io.github.xponer2.translator.registry.HolderLookup.Provider registries) {
		tile.readFromNBT(tag);
	}

	public static NBTTagCompound getUpdateTag(TileEntity tile, io.github.xponer2.translator.registry.HolderLookup.Provider registries) {
		return tile.getUpdateTag();
	}

	public static NBTTagCompound saveWithFullMetadata(TileEntity tile, io.github.xponer2.translator.registry.HolderLookup.Provider registries) {
		return tile.writeToNBT(new NBTTagCompound());
	}

	public static NBTTagCompound saveWithId(TileEntity tile, io.github.xponer2.translator.registry.HolderLookup.Provider registries) {
		return tile.writeToNBT(new NBTTagCompound());
	}

	public static NBTTagCompound saveWithoutMetadata(TileEntity tile, io.github.xponer2.translator.registry.HolderLookup.Provider registries) {
		NBTTagCompound tag = tile.writeToNBT(new NBTTagCompound());
		tag.removeTag("id");
		tag.removeTag("x");
		tag.removeTag("y");
		tag.removeTag("z");
		return tag;
	}

	public static void invalidateCapabilities(TileEntity tile) {
	}

	/** The state a block entity keeps in 1.21: 1.12.2 asks the world. */
	public static void setBlockState(TileEntity tile, IBlockState state) {
	}

	// --- Chunk -------------------------------------------------------------------------------------------------------

	public static int getHeight(Chunk chunk, Object heightmap, int x, int z) {
		return chunk.getHeightValue(x & 15, z & 15);
	}

	public static int getMinBuildHeight(Chunk chunk) {
		return 0;
	}

	public static int getMaxBuildHeight(Chunk chunk) {
		return 256;
	}

	public static int getMinSection(Chunk chunk) {
		return 0;
	}

	public static int getSectionIndex(Chunk chunk, int y) {
		return y >> 4;
	}

	@Nullable
	public static net.minecraft.world.chunk.storage.ExtendedBlockStorage getSection(Chunk chunk, int index) {
		var sections = chunk.getBlockStorageArray();
		return index >= 0 && index < sections.length ? sections[index] : null;
	}

	@Nullable
	public static IBlockState setBlockState(Chunk chunk, BlockPos pos, IBlockState state, boolean moved) {
		return chunk.setBlockState(pos, state);
	}

	public static void markPosForPostprocessing(Chunk chunk, BlockPos pos) {
	}

	public static void setUnsaved(Chunk chunk, boolean unsaved) {
		chunk.setModified(unsaved);
	}

	// --- EntityLivingBase --------------------------------------------------------------------------------------------

	public static void knockback(EntityLivingBase entity, double strength, double x, double z) {
		entity.knockBack(entity, (float) strength, x, z);
	}

	public static boolean isCrouching(EntityLivingBase entity) {
		return entity.isSneaking();
	}

	public static boolean removeEffect(EntityLivingBase entity, Holder<net.minecraft.potion.Potion> effect) {
		boolean had = entity.isPotionActive(effect.value());
		entity.removePotionEffect(effect.value());
		return had;
	}

	public static boolean randomTeleport(EntityLivingBase entity, double x, double y, double z, boolean particles) {
		return entity.attemptTeleport(x, y, z);
	}

	public static net.minecraft.util.SoundEvent getEatingSound(EntityLivingBase entity, ItemStack stack) {
		return net.minecraft.init.SoundEvents.ENTITY_GENERIC_EAT;
	}

	public static net.minecraft.util.SoundEvent getDrinkingSound(EntityLivingBase entity, ItemStack stack) {
		return net.minecraft.init.SoundEvents.ENTITY_GENERIC_DRINK;
	}

	public static boolean isEyeInFluid(EntityLivingBase entity, io.github.xponer2.translator.util.TagKey<?> fluid) {
		String path = fluid.location().getPath();
		return entity.isInsideOfMaterial("lava".equals(path) ? net.minecraft.block.material.Material.LAVA : net.minecraft.block.material.Material.WATER);
	}

	// --- Guava -------------------------------------------------------------------------------------------------------

	/**
	 * Guava's {@code Files.fileTraverser()} (newer than the game's Guava 21): {@code Traverser.forTree} over the
	 * directories, with the newer Guava's graph classes the mod's libraries bring (typed Object here, cast back).
	 */
	public static Object fileTraverser() {
		try {
			ClassLoader loader = MiscBridges.class.getClassLoader();
			Class<?> successors = Class.forName("com.google.common.graph.SuccessorsFunction", true, loader);
			Object tree = java.lang.reflect.Proxy.newProxyInstance(loader, new Class<?>[] {successors}, (proxy, method, args) -> {
				switch (method.getName()) {
				case "successors": {
					java.io.File[] files = ((java.io.File) args[0]).isDirectory() ? ((java.io.File) args[0]).listFiles() : null;
					return files == null ? java.util.Collections.emptyList() : java.util.Collections.unmodifiableList(java.util.Arrays.asList(files));
				}
				case "equals":
					return proxy == args[0];
				case "hashCode":
					return System.identityHashCode(proxy);
				default:
					return "Files.fileTraverser()";
				}
			});
			return Class.forName("com.google.common.graph.Traverser", true, loader).getMethod("forTree", successors).invoke(null, tree);
		} catch (ReflectiveOperationException e) {
			throw new UnsupportedOperationException("Translator: Files.fileTraverser needs Guava's graph classes", e);
		}
	}
}
