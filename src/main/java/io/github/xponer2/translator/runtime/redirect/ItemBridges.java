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

import java.util.Optional;

import javax.annotation.Nullable;

import io.github.xponer2.translator.util.FluidState;
import io.github.xponer2.translator.util.UseOnContext;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;

/**
 * Static stand-ins for 1.21 methods of items and fluids that 1.12.2 lacks, called on any item (the item of a stack,
 * a vanilla item) or fluid (runtime, neoforge-1.21.1-redirects.tsv). Mod items extending the Translator's Item facade
 * have most of these themselves.
 */
public final class ItemBridges {
	/** The ids of the base attack modifiers of weapons (1.21 Item.BASE_ATTACK_DAMAGE_ID, BASE_ATTACK_SPEED_ID). */
	public static final ResourceLocation BASE_ATTACK_DAMAGE_ID = new ResourceLocation("minecraft", "base_attack_damage");
	public static final ResourceLocation BASE_ATTACK_SPEED_ID = new ResourceLocation("minecraft", "base_attack_speed");

	private ItemBridges() {
	}

	// --- Item --------------------------------------------------------------------------------------------------------

	public static ITextComponent getDescription(Item item) {
		return new TextComponentTranslation(item.getTranslationKey() + ".name");
	}

	public static String getDescriptionId(Item item) {
		return item.getTranslationKey();
	}

	/** 1.21 {@code getDescriptionId(stack)}: the key of that stack (Powah names its blocks with it). */
	public static String getDescriptionId(Item item, ItemStack stack) {
		return item.getTranslationKey(stack);
	}

	public static ITextComponent getName(Item item, ItemStack stack) {
		return new TextComponentString(item.getItemStackDisplayName(stack));
	}

	/**
	 * 1.21 {@code isComplex()}: whether the item sends its own data packet (a map). It isn't 1.12.2's {@code isMap()},
	 * which means an ItemMapBase that the player's tick casts to (Twilight Forest's maps crashed it): a 1.21 class that
	 * declares it is asked, a 1.12.2 one answers with isMap.
	 */
	public static boolean isComplex(Item item) {
		try {
			return (Boolean) item.getClass().getMethod("isComplex").invoke(item);
		} catch (ReflectiveOperationException | ClassCastException e) {
			return item.isMap();
		}
	}

	public static Item asItem(Item item) {
		return item;
	}

	public static io.github.xponer2.translator.item.FoodProperties getFoodProperties(Item item, ItemStack stack, @Nullable EntityLivingBase entity) {
		return VanillaRedirects.getFoodProperties(stack, entity);
	}

	public static void onCraftedBy(Item item, ItemStack stack, World world, EntityPlayer player) {
		item.onCreated(stack, world, player);
	}

	public static EnumActionResult useOn(Item item, UseOnContext context) {
		EntityPlayer player = context.getPlayer();
		if (player == null) {
			return EnumActionResult.PASS;
		}
		return item.onItemUse(player, context.getLevel(), context.getClickedPos(), context.getHand(), context.getClickedFace(), context.hitX(), context.hitY(),
				context.hitZ());
	}

	public static ActionResult<ItemStack> use(Item item, World world, EntityPlayer player, EnumHand hand) {
		return item.onItemRightClick(world, player, hand);
	}

	public static ItemStack finishUsingItem(Item item, ItemStack stack, World world, EntityLivingBase entity) {
		return item.onItemUseFinish(stack, world, entity);
	}

	/** NeoForge item abilities (axe strip, shovel flatten): 1.12.2 items have none. */
	public static boolean canPerformAction(Item item, ItemStack stack, Object ability) {
		return false;
	}

	public static int getBarWidth(Item item, ItemStack stack) {
		return Math.round(13.0F - (float) item.getDurabilityForDisplay(stack) * 13.0F);
	}

	public static int getBarColor(Item item, ItemStack stack) {
		return item.getRGBDurabilityForDisplay(stack);
	}

	public static boolean isBarVisible(Item item, ItemStack stack) {
		return item.showDurabilityBar(stack);
	}

	public static boolean canBeHurtBy(Item item, ItemStack stack, DamageSource source) {
		return true;
	}

	public static int getMaxStackSize(Item item, ItemStack stack) {
		return item.getItemStackLimit(stack);
	}

	public static int getDefaultMaxStackSize(Item item) {
		return item.getItemStackLimit();
	}

	public static <T extends EntityLivingBase> int damageItem(Item item, ItemStack stack, int amount, @Nullable T entity,
			java.util.function.Consumer<Item> onBroken) {
		return amount;
	}

	// --- Fluid -------------------------------------------------------------------------------------------------------

	public static FluidState defaultFluidState(Fluid fluid) {
		return FluidState.of(fluid, true, 8);
	}

	public static FluidState getSource(Fluid fluid, boolean falling) {
		return FluidState.of(fluid, true, 8);
	}

	public static FluidState getFlowing(Fluid fluid, int amount, boolean falling) {
		return FluidState.of(fluid, false, amount);
	}

	public static Fluid getSource(Fluid fluid) {
		return fluid;
	}

	public static Fluid getFlowing(Fluid fluid) {
		return fluid;
	}

	/** Ticks between flow updates: 5 for water, 30 for lava (1.12.2 viscosity 1000 and 6000). */
	public static int getTickDelay(Fluid fluid, World world) {
		return Math.max(1, fluid.getViscosity() / 200);
	}

	/** Fluid tags: minecraft:water and minecraft:lava are water and lava, the mods' tags name fluids by id. */
	public static boolean is(Fluid fluid, io.github.xponer2.translator.util.TagKey<?> tag) {
		String path = tag.location().getPath();
		if ("water".equals(path)) {
			return fluid == FluidRegistry.WATER;
		}
		if ("lava".equals(path)) {
			return fluid == FluidRegistry.LAVA;
		}
		return fluid.getName().equals(path) || fluid.getName().endsWith("_" + path);
	}

	public static Item getBucket(Fluid fluid) {
		return FluidUtil.getFilledBucket(new FluidStack(fluid, 1000)).getItem();
	}

	public static Optional<SoundEvent> getPickupSound(Fluid fluid) {
		return Optional.ofNullable(fluid.getFillSound());
	}

	public static boolean isSame(Fluid fluid, Fluid other) {
		return fluid == other;
	}

	/**
	 * 1.21 {@code Item.getPlayerPOVHitResult(level, player, fluid)} for any item (a static the carried items call on
	 * themselves: lily pads' PlaceOnWaterBlockItem, buckets, boats): the Item facade's.
	 */
	public static net.minecraft.util.math.RayTraceResult getPlayerPOVHitResult(World world, EntityPlayer player,
			io.github.xponer2.translator.util.ClipContext.Fluid fluid) {
		return io.github.xponer2.translator.runtime.mc.Item.getPlayerPOVHitResult(world, player, fluid);
	}
}
