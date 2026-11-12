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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import com.mojang.authlib.GameProfile;

import io.github.xponer2.translator.component.DataComponentType;
import io.github.xponer2.translator.component.StackComponents;
import io.github.xponer2.translator.item.ArmorMaterialData;
import io.github.xponer2.translator.item.ArmorType;
import io.github.xponer2.translator.registry.Holder;
import io.github.xponer2.translator.registry.Registry;
import io.github.xponer2.translator.registry.ResourceKey;
import io.github.xponer2.translator.runtime.RuntimeStubs;
import io.github.xponer2.translator.util.BuiltInRegistries;
import io.github.xponer2.translator.util.Registries;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockTNT;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.state.BlockWorldState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.AbstractAttributeMap;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.tileentity.TileEntitySkull;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.ForgeHooks;

/**
 * 1.21 members of items, stacks, armor, block states, blocks, signs, skulls, chests and effects that 1.12.2's classes
 * lack (R rows: the receiver first). 1.21 classes the Translator carries (SignText, ItemEnchantments,
 * ResolvableProfile) are made by reflection and given as Object (the caller casts them back).
 */
public final class ItemBlockApiBridges {
	private ItemBlockApiBridges() {
	}

	// --- signs: SignText over 1.12.2's four lines -----------------------------------------------------------------------

	/** The sign data 1.12.2 has no field for (back side, colors, glow, wax): in the sign's Forge data, saved and synced with it. */
	private static final String SIGN_TAG = "translator_sign";
	private static final int LINES = 4;

	/** 1.21 {@code sign.getFrontText()}: the 1.12.2 lines, with the color and glow kept beside them. */
	@Nullable
	public static Object getFrontText(TileEntitySign sign) {
		NBTTagCompound extra = signData(sign, false);
		return signText(sign.signText.clone(), color(extra, "front_color"), extra != null && extra.getBoolean("front_glow"));
	}

	/** 1.21 {@code sign.getBackText()}: 1.12.2 signs have one side; the back is kept in the sign's data (empty until set). */
	@Nullable
	public static Object getBackText(TileEntitySign sign) {
		NBTTagCompound extra = signData(sign, false);
		ITextComponent[] lines = new ITextComponent[LINES];
		NBTTagList saved = extra == null ? null : extra.getTagList("back", net.minecraftforge.common.util.Constants.NBT.TAG_STRING);
		for (int i = 0; i < LINES; i++) {
			ITextComponent line = null;
			if (saved != null && i < saved.tagCount()) {
				try {
					line = ITextComponent.Serializer.jsonToComponent(saved.getStringTagAt(i));
				} catch (RuntimeException e) {
					line = null;
				}
			}
			lines[i] = line == null ? new TextComponentString("") : line;
		}
		return signText(lines, color(extra, "back_color"), extra != null && extra.getBoolean("back_glow"));
	}

	/** The field read {@code sign.frontText}. */
	@Nullable
	public static Object frontText(TileEntitySign sign) {
		return getFrontText(sign);
	}

	@Nullable
	public static Object backText(TileEntitySign sign) {
		return getBackText(sign);
	}

	/** 1.21 {@code sign.getText(front)}. */
	@Nullable
	public static Object getText(TileEntitySign sign, boolean front) {
		return front ? getFrontText(sign) : getBackText(sign);
	}

	/** 1.21 {@code sign.setText(text, front)}: the lines (front: 1.12.2's own), color and glow; true when anything changed. */
	public static boolean setText(TileEntitySign sign, @Nullable Object text, boolean front) {
		if (text == null) {
			return false;
		}
		ITextComponent[] lines;
		EnumDyeColor color;
		boolean glow;
		try {
			Class<?> type = text.getClass();
			lines = (ITextComponent[]) type.getMethod("getMessages", boolean.class).invoke(text, false);
			color = (EnumDyeColor) type.getMethod("getColor").invoke(text);
			glow = (Boolean) type.getMethod("hasGlowingText").invoke(text);
		} catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
			RuntimeStubs.failed("reading a sign text", e);
			return false;
		}
		boolean changed = false;
		NBTTagCompound extra = signData(sign, true);
		if (front) {
			for (int i = 0; i < LINES && i < sign.signText.length; i++) {
				ITextComponent line = i < lines.length && lines[i] != null ? lines[i] : new TextComponentString("");
				if (!line.equals(sign.signText[i])) {
					sign.signText[i] = line;
					changed = true;
				}
			}
		} else {
			NBTTagList saved = new NBTTagList();
			for (int i = 0; i < LINES; i++) {
				ITextComponent line = i < lines.length && lines[i] != null ? lines[i] : new TextComponentString("");
				saved.appendTag(new NBTTagString(ITextComponent.Serializer.componentToJson(line)));
			}
			changed = !saved.equals(extra.getTag("back"));
			extra.setTag("back", saved);
		}
		String side = front ? "front" : "back";
		if (color(extra, side + "_color") != color || extra.getBoolean(side + "_glow") != glow) {
			changed = true;
		}
		extra.setString(side + "_color", (color == null ? EnumDyeColor.BLACK : color).getName());
		extra.setBoolean(side + "_glow", glow);
		if (changed) {
			markSignUpdated(sign);
		}
		return changed;
	}

	/** 1.21 {@code sign.updateText(change, front)}: setText of the changed text. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static boolean updateText(TileEntitySign sign, UnaryOperator change, boolean front) {
		return setText(sign, change.apply(getText(sign, front)), front);
	}

	public static boolean isWaxed(TileEntitySign sign) {
		NBTTagCompound extra = signData(sign, false);
		return extra != null && extra.getBoolean("waxed");
	}

	/** 1.21 {@code sign.setWaxed(waxed)}: true when it changed. */
	public static boolean setWaxed(TileEntitySign sign, boolean waxed) {
		if (isWaxed(sign) == waxed) {
			return false;
		}
		signData(sign, true).setBoolean("waxed", waxed);
		markSignUpdated(sign);
		return true;
	}

	/** 1.21 {@code sign.isFacingFrontText(player)}: the player stands within 90 degrees of the way the sign faces. */
	public static boolean isFacingFrontText(TileEntitySign sign, EntityPlayer player) {
		World world = sign.getWorld();
		if (world == null) {
			return true;
		}
		IBlockState state = world.getBlockState(sign.getPos());
		Float yaw = signYaw(state);
		if (yaw == null) {
			return true;
		}
		double centerX = 0.5;
		double centerZ = 0.5;
		try {
			AxisAlignedBB box = state.getBoundingBox(world, sign.getPos());
			centerX = (box.minX + box.maxX) / 2.0;
			centerZ = (box.minZ + box.maxZ) / 2.0;
		} catch (RuntimeException e) {
			// the block's middle
		}
		double dx = player.posX - (sign.getPos().getX() + centerX);
		double dz = player.posZ - (sign.getPos().getZ() + centerZ);
		float toPlayer = (float) (MathHelper.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
		return Math.abs(MathHelper.wrapDegrees(yaw - toPlayer)) <= 90.0F;
	}

	/** 1.21 {@code sign.getMaxTextLineWidth()}: 90, a hanging sign's 60. */
	public static int getMaxTextLineWidth(TileEntitySign sign) {
		return isHanging(sign) ? 60 : 90;
	}

	/** 1.21 {@code sign.getTextLineHeight()}: 10, a hanging sign's 9. */
	public static int getTextLineHeight(TileEntitySign sign) {
		return isHanging(sign) ? 9 : 10;
	}

	/** 1.21 {@code sign.setAllowedPlayerEditor(uuid)}: the player of that id, if in the sign's world (null clears it). */
	public static void setAllowedPlayerEditor(TileEntitySign sign, @Nullable UUID player) {
		World world = sign.getWorld();
		sign.setPlayer(player == null || world == null ? null : world.getPlayerEntityByUUID(player));
	}

	@Nullable
	private static Float signYaw(IBlockState state) {
		Block block = state.getBlock();
		try {
			if (block instanceof BlockStandingSign) {
				return state.getValue(BlockStandingSign.ROTATION) * 22.5F;
			}
			if (block instanceof BlockWallSign) {
				return state.getValue(BlockWallSign.FACING).getHorizontalAngle();
			}
			if (block instanceof io.github.xponer2.translator.runtime.mc.StandingSignBlock) {
				return state.getValue(io.github.xponer2.translator.runtime.mc.StandingSignBlock.ROTATION) * 22.5F;
			}
			if (block instanceof io.github.xponer2.translator.runtime.mc.WallSignBlock) {
				EnumFacing facing = state.getValue(io.github.xponer2.translator.runtime.mc.WallSignBlock.FACING);
				return facing.getHorizontalAngle();
			}
		} catch (IllegalArgumentException e) {
			return null;
		}
		return null;
	}

	private static boolean isHanging(TileEntitySign sign) {
		World world = sign.getWorld();
		if (world == null) {
			return false;
		}
		Block block = world.getBlockState(sign.getPos()).getBlock();
		return block instanceof io.github.xponer2.translator.runtime.mc.CeilingHangingSignBlock
				|| block instanceof io.github.xponer2.translator.runtime.mc.WallHangingSignBlock;
	}

	@Nullable
	private static NBTTagCompound signData(TileEntitySign sign, boolean create) {
		NBTTagCompound data = sign.getTileData();
		if (!data.hasKey(SIGN_TAG, net.minecraftforge.common.util.Constants.NBT.TAG_COMPOUND)) {
			if (!create) {
				return null;
			}
			data.setTag(SIGN_TAG, new NBTTagCompound());
		}
		return data.getCompoundTag(SIGN_TAG);
	}

	private static EnumDyeColor color(@Nullable NBTTagCompound extra, String key) {
		if (extra != null && extra.hasKey(key)) {
			for (EnumDyeColor color : EnumDyeColor.values()) {
				if (color.getName().equals(extra.getString(key))) {
					return color;
				}
			}
		}
		return EnumDyeColor.BLACK;
	}

	private static void markSignUpdated(TileEntitySign sign) {
		sign.markDirty();
		World world = sign.getWorld();
		if (world != null) {
			IBlockState state = world.getBlockState(sign.getPos());
			world.notifyBlockUpdate(sign.getPos(), state, state, 3);
		}
	}

	private static volatile Constructor<?> signTextConstructor;

	@Nullable
	private static Object signText(ITextComponent[] lines, EnumDyeColor color, boolean glow) {
		try {
			Constructor<?> constructor = signTextConstructor;
			if (constructor == null) {
				constructor = CarriedBridges.carried("net.minecraft.world.level.block.entity.SignText")
						.getConstructor(ITextComponent[].class, ITextComponent[].class, EnumDyeColor.class, boolean.class);
				signTextConstructor = constructor;
			}
			ITextComponent[] own = new ITextComponent[LINES];
			for (int i = 0; i < LINES; i++) {
				own[i] = i < lines.length && lines[i] != null ? lines[i] : new TextComponentString("");
			}
			return constructor.newInstance(own, own.clone(), color, glow);
		} catch (ReflectiveOperationException | LinkageError e) {
			RuntimeStubs.failed("making a sign text", e);
			return null;
		}
	}

	// --- skulls: ResolvableProfile ----------------------------------------------------------------------------------------

	/** 1.21 {@code skull.getOwnerProfile()}: the 1.12.2 profile as a ResolvableProfile (null without one). */
	@Nullable
	public static Object getOwnerProfile(TileEntitySkull skull) {
		GameProfile profile = skull.getPlayerProfile();
		if (profile == null) {
			return null;
		}
		try {
			return CarriedBridges.carried("net.minecraft.world.item.component.ResolvableProfile").getConstructor(GameProfile.class).newInstance(profile);
		} catch (ReflectiveOperationException | LinkageError e) {
			RuntimeStubs.failed("making a skull owner profile", e);
			return null;
		}
	}

	/** 1.21 {@code skull.setOwner(profile)}: the profile's game profile (a player head). */
	public static void setOwner(TileEntitySkull skull, @Nullable Object profile) {
		GameProfile game = null;
		if (profile != null) {
			try {
				game = (GameProfile) profile.getClass().getMethod("gameProfile").invoke(profile);
			} catch (ReflectiveOperationException | ClassCastException | LinkageError e) {
				RuntimeStubs.failed("reading a skull owner profile", e);
			}
		}
		skull.setPlayerProfile(game);
	}

	// --- chests -----------------------------------------------------------------------------------------------------------

	/** 1.21 {@code container.setLootTable(key, seed)}: 1.12.2's loot table of the key's location. */
	public static void setLootTable(TileEntityLockableLoot container, @Nullable ResourceKey<?> table, long seed) {
		container.setLootTable(table == null ? null : table.location(), seed);
	}

	/**
	 * The 1.21 field {@code chest.openersCounter}: a counter whose getOpenerCount is the chest's players (1.21's own
	 * counter class of the chest, carried, its count set from 1.12.2's numPlayersUsing).
	 */
	@Nullable
	public static Object openersCounter(net.minecraft.tileentity.TileEntityChest chest) {
		try {
			if (openersConstructor == null) {
				Constructor<?> constructor = CarriedBridges.carried("net.minecraft.world.level.block.entity.ChestBlockEntity$1").getDeclaredConstructors()[0];
				constructor.setAccessible(true);
				java.lang.reflect.Field count = CarriedBridges.carried("net.minecraft.world.level.block.entity.ContainerOpenersCounter").getDeclaredField("openCount");
				count.setAccessible(true);
				openersCount = count;
				openersConstructor = constructor;
			}
			Object counter = openersConstructor.newInstance(chest);
			openersCount.setInt(counter, chest.numPlayersUsing);
			return counter;
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			RuntimeStubs.failed("the openers of a chest", e);
			return null;
		}
	}

	private static volatile Constructor<?> openersConstructor;
	private static volatile java.lang.reflect.Field openersCount;

	// --- armor --------------------------------------------------------------------------------------------------------------

	private static final Map<ItemArmor.ArmorMaterial, ArmorMaterialData> VANILLA_MATERIALS = new ConcurrentHashMap<>();
	private static final Map<Object, Holder<ArmorMaterialData>> MATERIAL_HOLDERS = new ConcurrentHashMap<>();

	/**
	 * 1.21 {@code armor.getMaterial()}: a holder of the material. A 1.21.1 mod's armor has its own; 1.12.2's armor gets
	 * one made from its 1.12.2 material (defense by slot, enchantability, sound, repair item, toughness, its texture).
	 */
	public static Holder<ArmorMaterialData> getMaterial(ItemArmor armor) {
		ArmorMaterialData material = armor instanceof io.github.xponer2.translator.item.ArmorItem own ? own.getMaterial()
				: VANILLA_MATERIALS.computeIfAbsent(armor.getArmorMaterial(), ItemBlockApiBridges::fromVanilla);
		return MATERIAL_HOLDERS.computeIfAbsent(material, key -> {
			String name = material.layers().isEmpty() ? null : material.layers().get(0).assetName().toString();
			return name == null ? Holder.direct(material)
					: new Registry.Reference<>(ResourceKey.create(Registries.ARMOR_MATERIAL, new ResourceLocation(name)), material);
		});
	}

	private static ArmorMaterialData fromVanilla(ItemArmor.ArmorMaterial vanilla) {
		var defense = new java.util.EnumMap<ArmorType, Integer>(ArmorType.class);
		for (ArmorType type : ArmorType.values()) {
			defense.put(type, vanilla.getDamageReductionAmount(type.getSlot()));
		}
		String asset = vanilla.getName();
		ResourceLocation location = asset.contains(":") ? new ResourceLocation(asset) : new ResourceLocation("minecraft", asset);
		Supplier<Ingredient> repair = () -> {
			ItemStack stack = vanilla.getRepairItemStack();
			return stack.isEmpty() ? Ingredient.EMPTY : Ingredient.fromStacks(stack);
		};
		return new ArmorMaterialData(defense, vanilla.getEnchantability(), vanilla.getSoundEvent(), repair,
				java.util.Collections.singletonList(new ArmorMaterialData.Layer(location)), vanilla.getToughness(), 0.0F);
	}

	/** 1.21 {@code armor.getDefense()}. */
	public static int getDefense(ItemArmor armor) {
		return armor.damageReduceAmount;
	}

	/** 1.21 {@code armor.getToughness()}. */
	public static float getToughness(ItemArmor armor) {
		return armor.toughness;
	}

	/** 1.21 {@code material.equipSound()}: a holder of the sound (the Translator's material gives the sound itself). */
	@Nullable
	public static Holder<SoundEvent> equipSound(ArmorMaterialData material) {
		SoundEvent sound = material.equipSound();
		return sound == null ? null : BuiltInRegistries.SOUND_EVENT.wrapAsHolder(sound);
	}

	/** NeoForge {@code item.canEquip(stack, slot, entity)} (a super call): 1.12.2's isValidArmor. */
	public static boolean canEquip(Item item, ItemStack stack, EntityEquipmentSlot slot, EntityLivingBase entity) {
		return item.isValidArmor(stack, slot, entity);
	}

	// --- enchantments: ItemEnchantments over 1.12.2's enchantment list ---------------------------------------------------

	/** 1.21 {@code stack.getEnchantments()}: the stack's enchantments (not an enchanted book's stored ones). */
	@Nullable
	public static Object getEnchantments(ItemStack stack) {
		var levels = new java.util.LinkedHashMap<Enchantment, Integer>();
		NBTTagList list = stack.getEnchantmentTagList();
		for (int i = 0; i < list.tagCount(); i++) {
			NBTTagCompound entry = list.getCompoundTagAt(i);
			Enchantment enchantment = Enchantment.getEnchantmentByID(entry.getShort("id"));
			if (enchantment != null) {
				levels.merge(enchantment, (int) entry.getShort("lvl"), Math::max);
			}
		}
		return itemEnchantments(levels);
	}

	/** NeoForge {@code stack.getTagEnchantments()}: the same. */
	@Nullable
	public static Object getTagEnchantments(ItemStack stack) {
		return getEnchantments(stack);
	}

	/**
	 * NeoForge {@code stack.getAllEnchantments(lookup)}: the item's own answer (a 1.21.1 mod's item adds its built-in
	 * ones), else the stack's enchantments.
	 */
	@Nullable
	public static Object getAllEnchantments(ItemStack stack, @Nullable Object lookup) {
		Item item = stack.getItem();
		for (Method method : item.getClass().getMethods()) {
			if ("getAllEnchantments".equals(method.getName()) && method.getParameterCount() == 2
					&& method.getParameterTypes()[0] == ItemStack.class && method.getDeclaringClass() != Item.class) {
				try {
					return method.invoke(item, stack, lookup);
				} catch (ReflectiveOperationException | IllegalArgumentException e) {
					RuntimeStubs.failed(item.getClass().getName() + ".getAllEnchantments", e);
				}
				break;
			}
		}
		return getEnchantments(stack);
	}

	/** NeoForge {@code item.getAllEnchantments(stack, lookup)} (a mod item's super call): the stack's enchantments. */
	@Nullable
	public static Object getAllEnchantments(Item item, ItemStack stack, @Nullable Object lookup) {
		return getEnchantments(stack);
	}

	/** 1.21 {@code stack.isPrimaryItemFor(enchantment)}: the enchantment goes on the item at the enchanting table. */
	public static boolean isPrimaryItemFor(ItemStack stack, Holder<Enchantment> enchantment) {
		return enchantment != null && enchantment.value() != null && enchantment.value().canApplyAtEnchantingTable(stack);
	}

	private static final Map<Enchantment, Holder<Enchantment>> ENCHANTMENT_HOLDERS = new ConcurrentHashMap<>();

	@Nullable
	private static Object itemEnchantments(Map<Enchantment, Integer> levels) {
		try {
			Class<?> immutable = CarriedBridges.carried("net.minecraft.world.item.enchantment.ItemEnchantments");
			Object empty = immutable.getField("EMPTY").get(null);
			if (levels.isEmpty()) {
				return empty;
			}
			Class<?> mutableClass = CarriedBridges.carried("net.minecraft.world.item.enchantment.ItemEnchantments$Mutable");
			Object mutable = mutableClass.getConstructor(immutable).newInstance(empty);
			Method set = mutableClass.getMethod("set", Holder.class, int.class);
			for (var entry : levels.entrySet()) {
				set.invoke(mutable, ENCHANTMENT_HOLDERS.computeIfAbsent(entry.getKey(), BuiltInRegistries.ENCHANTMENT::wrapAsHolder), entry.getValue());
			}
			return mutableClass.getMethod("toImmutable").invoke(mutable);
		} catch (ReflectiveOperationException | LinkageError e) {
			RuntimeStubs.failed("making the enchantments of a stack", e);
			return null;
		}
	}

	// --- stacks -----------------------------------------------------------------------------------------------------------

	/** 1.21 {@code stack.interactLivingEntity(player, target, hand)}: 1.12.2's interactWithEntity. */
	public static EnumActionResult interactLivingEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
		return stack.interactWithEntity(player, target, hand) ? EnumActionResult.SUCCESS : EnumActionResult.PASS;
	}

	/** 1.21 {@code stack.isPiglinCurrency()}: gold ingots. */
	public static boolean isPiglinCurrency(ItemStack stack) {
		return stack.getItem() == Items.GOLD_INGOT;
	}

	/** NeoForge {@code stack.isEnderMask(player, enderman)}: a carved pumpkin (1.12.2's pumpkin) on the head. */
	public static boolean isEnderMask(ItemStack stack, EntityPlayer player, EntityEnderman enderman) {
		return stack.getItem() == Item.getItemFromBlock(Blocks.PUMPKIN);
	}

	/** NeoForge {@code stack.makesPiglinsNeutral(wearer)}: golden armor. */
	public static boolean makesPiglinsNeutral(ItemStack stack, EntityLivingBase wearer) {
		return stack.getItem() instanceof ItemArmor armor && armor.getArmorMaterial() == ItemArmor.ArmorMaterial.GOLD;
	}

	/** NeoForge {@code stack.canWalkOnPowderedSnow(wearer)}: leather boots. */
	public static boolean canWalkOnPowderedSnow(ItemStack stack, EntityLivingBase wearer) {
		return stack.getItem() == Items.LEATHER_BOOTS;
	}

	/** 1.21 {@code stack.hurtEnemy(target, attacker)}: the item's hitEntity, the use counted when it was used. */
	public static boolean hurtEnemy(ItemStack stack, EntityLivingBase target, EntityPlayer attacker) {
		Item item = stack.getItem();
		boolean used = item.hitEntity(stack, target, attacker);
		if (used && attacker != null) {
			attacker.addStat(net.minecraft.stats.StatList.getObjectUseStats(item));
		}
		return used;
	}

	/** 1.21 {@code stack.getEntityRepresentation()}: the item frame holding it, else null. */
	@Nullable
	public static Entity getEntityRepresentation(ItemStack stack) {
		return stack.getItemFrame();
	}

	/** 1.21 {@code stack.canBreakBlockInAdventureMode(block)}: the stack's CanDestroy list. */
	public static boolean canBreakBlockInAdventureMode(ItemStack stack, BlockWorldState block) {
		return block != null && block.getBlockState() != null && stack.canDestroy(block.getBlockState().getBlock());
	}

	/** 1.21 {@code stack.canPlaceOnBlockInAdventureMode(block)}: the stack's CanPlaceOn list. */
	public static boolean canPlaceOnBlockInAdventureMode(ItemStack stack, BlockWorldState block) {
		return block != null && block.getBlockState() != null && stack.canPlaceOn(block.getBlockState().getBlock());
	}

	/** 1.21 {@code stack.onCraftedBySystem(level)}: nothing on 1.12.2 (its crafting callback needs a player). */
	public static void onCraftedBySystem(ItemStack stack, World world) {
	}

	/** NeoForge {@code stack.onStopUsing(entity, count)}: nothing on 1.12.2 (no such callback). */
	public static void onStopUsing(ItemStack stack, EntityLivingBase entity, int count) {
	}

	/** NeoForge {@code stack.getHighlightTip(name)}: the name as is. */
	public static ITextComponent getHighlightTip(ItemStack stack, ITextComponent name) {
		return name;
	}

	/** 1.21 {@code stack.getBarWidth()}. */
	public static int getBarWidth(ItemStack stack) {
		return Math.round(13.0F - (float) stack.getItem().getDurabilityForDisplay(stack) * 13.0F);
	}

	/** 1.21 {@code stack.getBarColor()}. */
	public static int getBarColor(ItemStack stack) {
		return stack.getItem().getRGBDurabilityForDisplay(stack);
	}

	/**
	 * 1.21 {@code stack.addToTooltip(type, context, lines, flag)}: the stack's component of that type adds its lines
	 * when it is a TooltipProvider (the context and flag as they come).
	 */
	public static void addToTooltip(ItemStack stack, DataComponentType<?> type, Object context, Consumer<?> lines, Object flag) {
		Object value = StackComponents.get(stack, type);
		if (value == null) {
			return;
		}
		for (Method method : value.getClass().getMethods()) {
			if ("addToTooltip".equals(method.getName()) && method.getParameterCount() == 3 && method.getParameterTypes()[1] == Consumer.class) {
				try {
					method.invoke(value, context, lines, flag);
				} catch (ReflectiveOperationException | IllegalArgumentException e) {
					RuntimeStubs.failed(value.getClass().getName() + ".addToTooltip", e);
				}
				return;
			}
		}
	}

	public static void addToTooltip(ItemStack stack, Supplier<? extends DataComponentType<?>> type, Object context, Consumer<?> lines, Object flag) {
		addToTooltip(stack, type.get(), context, lines, flag);
	}

	/** 1.21 {@code stack.getPrototype()}: the item's default components. */
	public static io.github.xponer2.translator.component.DataComponentMap getPrototype(ItemStack stack) {
		return StackComponents.getComponents(new ItemStack(stack.getItem()));
	}

	/** 1.21 {@code stack.copyFrom(source, types...)}: those components of a source stack. */
	public static void copyFrom(ItemStack stack, @Nullable Object source, DataComponentType<?>[] types) {
		if (source instanceof ItemStack from) {
			for (DataComponentType<?> type : types) {
				StackComponents.copyFrom(stack, type, from);
			}
		}
	}

	/** 1.21 {@code stack.applyComponentsAndValidate(patch)}: the patch applied (1.12.2 stacks need no validation). */
	public static void applyComponentsAndValidate(ItemStack stack, io.github.xponer2.translator.component.DataComponentPatch patch) {
		StackComponents.applyComponents(stack, patch);
	}

	/** NeoForge {@code stack.onItemUseFirst(context)}: 1.12.2's onItemUseFirst of the item with the context's click. */
	public static EnumActionResult onItemUseFirst(ItemStack stack, io.github.xponer2.translator.util.UseOnContext context) {
		EntityPlayer player = context.getPlayer();
		if (player == null) {
			return EnumActionResult.PASS;
		}
		return stack.getItem().onItemUseFirst(player, context.getLevel(), context.getClickedPos(), context.getClickedFace(), context.hitX(),
				context.hitY(), context.hitZ(), context.getHand());
	}

	// --- items ------------------------------------------------------------------------------------------------------------

	/** 1.21 {@code item.getDrinkingSound()}: the generic drink. */
	public static SoundEvent getDrinkingSound(Item item) {
		return SoundEvents.ENTITY_GENERIC_DRINK;
	}

	/** 1.21 {@code item.components()}: the components of a new stack of it (its defaults). */
	public static io.github.xponer2.translator.component.DataComponentMap components(Item item) {
		return StackComponents.getComponents(new ItemStack(item));
	}

	/** NeoForge {@code item.getBurnTime(stack, recipeType)} (a mod item's super call): the furnace's burn time. */
	public static int getBurnTime(Item item, ItemStack stack, @Nullable Object recipeType) {
		return TileEntityFurnace.getItemBurnTime(stack);
	}

	/** 1.21 {@code item.hurtEnemy(stack, target, attacker)} (a super call): 1.12.2's hitEntity of the item. */
	public static boolean hurtEnemy(Item item, ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
		return item.hitEntity(stack, target, attacker);
	}

	/** 1.21 {@code item.isPrimaryItemFor(stack, enchantment)}. */
	public static boolean isPrimaryItemFor(Item item, ItemStack stack, Holder<Enchantment> enchantment) {
		return isPrimaryItemFor(stack, enchantment);
	}

	// --- blocks -----------------------------------------------------------------------------------------------------------

	/**
	 * 1.21 {@code Block.OCCLUSION_CACHE}: the per-thread cache of face occlusion between two states that mods with their
	 * own face culling share (as 1.21 makes it: 2048 entries, 127 for none).
	 */
	public static final ThreadLocal<it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap<Object>> OCCLUSION_CACHE = ThreadLocal.withInitial(() -> {
		var map = new it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap<Object>(2048, 0.25F) {
			@Override
			protected void rehash(int size) {
			}
		};
		map.defaultReturnValue((byte) 127);
		return map;
	});

	/** 1.21 {@code block.properties()} / the field: a full copy of the block's properties. */
	public static io.github.xponer2.translator.block.BlockProperties properties(Block block) {
		return io.github.xponer2.translator.block.BlockProperties.ofFullCopy(block);
	}

	/** The 1.21 field {@code block.item}: the block's item (null without one). */
	@Nullable
	public static Item item(Block block) {
		Item item = Item.getItemFromBlock(block);
		return item == Items.AIR ? null : item;
	}

	/** 1.21 {@code block.getFriction()}. */
	public static float getFriction(Block block) {
		return block.slipperiness;
	}

	/** 1.21 {@code block.isEnabled(flags)}: every block is. */
	public static boolean isEnabled(Block block, @Nullable Object flags) {
		return true;
	}

	/** NeoForge {@code block.canHarvestBlock(state, level, pos, player)} (a super call): Forge's harvest check. */
	public static boolean canHarvestBlock(Block block, IBlockState state, IBlockAccess world, BlockPos pos, EntityPlayer player) {
		return ForgeHooks.canHarvestBlock(block, player, world, pos);
	}

	/** NeoForge {@code block.onCaughtFire(state, level, pos, face, igniter)} (a super call): TNT goes off, as on 1.21. */
	public static void onCaughtFire(Block block, IBlockState state, World world, BlockPos pos, @Nullable EnumFacing face, @Nullable EntityLivingBase igniter) {
		onCaughtFire(state, world, pos, face, igniter);
	}

	/** NeoForge {@code state.onCaughtFire(level, pos, face, igniter)}: TNT goes off (1.12.2 does it in the fire block). */
	public static void onCaughtFire(IBlockState state, World world, BlockPos pos, @Nullable EnumFacing face, @Nullable EntityLivingBase igniter) {
		if (state.getBlock() instanceof BlockTNT tnt && !world.isRemote) {
			tnt.explode(world, pos, state.withProperty(BlockTNT.EXPLODE, true), igniter);
			world.setBlockToAir(pos);
		}
	}

	/** NeoForge {@code state.isLadder(level, pos, entity)}. */
	public static boolean isLadder(IBlockState state, World world, BlockPos pos, EntityLivingBase entity) {
		return state.getBlock().isLadder(state, world, pos, entity);
	}

	/** 1.21 {@code state.entityInside(level, pos, entity)}: 1.12.2's collision callback. */
	public static void entityInside(IBlockState state, World world, BlockPos pos, Entity entity) {
		state.getBlock().onEntityCollision(world, pos, state, entity);
	}

	/** 1.21 {@code block.entityInside(state, level, pos, entity)} (a super call): nothing, as 1.21's Block does. */
	public static void entityInside(Block block, IBlockState state, World world, BlockPos pos, Entity entity) {
	}

	/**
	 * 1.21 {@code state.isPathfindable(type)}: land (and air) paths go through what doesn't block movement, water
	 * paths through water.
	 */
	public static boolean isPathfindable(IBlockState state, @Nullable Object type) {
		String kind = type instanceof Enum<?> e ? e.name() : "LAND";
		if ("WATER".equals(kind)) {
			return state.getMaterial() == net.minecraft.block.material.Material.WATER;
		}
		return !state.getMaterial().blocksMovement();
	}

	/** NeoForge {@code state.getFlammability(level, pos, face)}. */
	public static int getFlammability(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face) {
		return state.getBlock().getFlammability(world, pos, face);
	}

	/** NeoForge {@code state.getFireSpreadSpeed(level, pos, face)}. */
	public static int getFireSpreadSpeed(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing face) {
		return state.getBlock().getFireSpreadSpeed(world, pos, face);
	}

	/** 1.21 {@code state.getShadeBrightness(level, pos)}: the ambient occlusion light value. */
	public static float getShadeBrightness(IBlockState state, IBlockAccess world, BlockPos pos) {
		return state.getAmbientOcclusionLightValue();
	}

	/** 1.21 {@code state.isValidSpawn(level, pos, type)}: a solid top. */
	public static boolean isValidSpawn(IBlockState state, IBlockAccess world, BlockPos pos, @Nullable Object type) {
		return state.isSideSolid(world, pos, EnumFacing.UP) && state.getBlock() != Blocks.BEDROCK && state.getBlock() != Blocks.BARRIER;
	}

	/** NeoForge {@code state.isFertile(level, pos)}: Forge's isFertile (needs a World). */
	public static boolean isFertile(IBlockState state, IBlockAccess world, BlockPos pos) {
		return world instanceof World level && state.getBlock().isFertile(level, pos);
	}

	/** NeoForge {@code state.isStickyBlock()}. */
	public static boolean isStickyBlock(IBlockState state) {
		return state.getBlock().isStickyBlock(state);
	}

	/** NeoForge {@code state.addRunningEffects(level, pos, entity)}. */
	public static boolean addRunningEffects(IBlockState state, World world, BlockPos pos, Entity entity) {
		return state.getBlock().addRunningEffects(state, world, pos, entity);
	}

	/** NeoForge {@code state.addLandingEffects(level, pos, state, entity, particles)}. */
	public static boolean addLandingEffects(IBlockState state, WorldServer world, BlockPos pos, IBlockState other, EntityLivingBase entity, int particles) {
		return state.getBlock().addLandingEffects(state, world, pos, other, entity, particles);
	}

	/** NeoForge {@code state.onDestroyedByPushReaction(level, pos, direction, fluid)}: the block goes (to its fluid's, else air). */
	public static void onDestroyedByPushReaction(IBlockState state, World world, BlockPos pos, @Nullable EnumFacing direction, @Nullable Object fluid) {
		world.setBlockToAir(pos);
	}

	/** 1.21 {@code state.getTicker(level, type)}: the block's own, when it is a 1.21 entity block. */
	@Nullable
	public static io.github.xponer2.translator.block.BlockEntityTicker<?> getTicker(IBlockState state, World world,
			io.github.xponer2.translator.registry.BlockEntityType<?> type) {
		if (state.getBlock() instanceof io.github.xponer2.translator.block.EntityBlock block) {
			return block.getTicker(world, state, cast(type));
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static <T> T cast(Object value) {
		return (T) value;
	}

	// --- effects ----------------------------------------------------------------------------------------------------------

	/** 1.21 {@code new MobEffectInstance(effect)}: no duration, the lowest level. */
	public static PotionEffect newPotionEffect(Holder<?> effect) {
		return new PotionEffect((Potion) effect.value(), 0, 0);
	}

	/**
	 * 1.21 {@code effect.addAttributeModifier(attribute, id, amount, operation)} on an effect typed as MobEffect (1.12.2's
	 * Potion): the Translator's MobEffect does it itself, any other effect gets 1.12.2's modifier.
	 */
	public static Potion addAttributeModifier(Potion potion, Holder<io.github.xponer2.translator.entity.Attribute> attribute, ResourceLocation id,
			double amount, io.github.xponer2.translator.entity.AttributeModifier.Operation operation) {
		if (potion instanceof io.github.xponer2.translator.runtime.mc.MobEffect effect) {
			return effect.addAttributeModifier(attribute, id, amount, operation);
		}
		var forge = attribute.value().forge();
		if (forge != null) {
			potion.registerPotionAttributeModifier(forge, io.github.xponer2.translator.entity.AttributeModifier.uuidOf(id).toString(), amount, operation.ordinal());
		}
		return potion;
	}

	/** 1.21 {@code effect.addAttributeModifiers(attributes, amplifier)}: 1.12.2's (it doesn't need the entity). */
	public static void addAttributeModifiers(Potion potion, AbstractAttributeMap attributes, int amplifier) {
		potion.applyAttributesModifiersToEntity(null, attributes, amplifier);
	}

	/** 1.21 {@code effect.removeAttributeModifiers(attributes)}. */
	public static void removeAttributeModifiers(Potion potion, AbstractAttributeMap attributes) {
		potion.removeAttributesModifiersFromEntity(null, attributes, 0);
	}

	/** 1.21 {@code effect.getDescriptionId()}: 1.12.2's name (the translation key). */
	public static String getDescriptionId(Potion potion) {
		return potion.getName();
	}

	/** 1.21 {@code effect.getDisplayName()}. */
	public static ITextComponent getDisplayName(Potion potion) {
		return new TextComponentTranslation(potion.getName());
	}

	/** 1.21 {@code instance.setDetailsFrom(other)}: the other's duration, level, ambience and particles. */
	public static void setDetailsFrom(PotionEffect effect, PotionEffect other) {
		try {
			setField(effect, other.getDuration(), "duration", "field_76460_b");
			setField(effect, other.getAmplifier(), "amplifier", "field_76461_c");
			setField(effect, other.getIsAmbient(), "isAmbient", "field_82724_e");
			setField(effect, other.doesShowParticles(), "showParticles", "field_188421_h");
		} catch (RuntimeException e) {
			effect.combine(other);
		}
	}

	private static void setField(PotionEffect effect, Object value, String... names) {
		net.minecraftforge.fml.common.ObfuscationReflectionHelper.setPrivateValue(PotionEffect.class, effect, value, names);
	}
}
