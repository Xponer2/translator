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

import java.util.List;

import javax.annotation.Nullable;

import io.github.xponer2.translator.item.ItemProperties;
import io.github.xponer2.translator.item.TooltipContext;
import io.github.xponer2.translator.text.ItemNames;
import io.github.xponer2.translator.util.UseOnContext;

import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Facade of the 1.21 {@code Item} for original 1.21.1 mods (runtime): built from {@code Item.Properties}, with the
 * 1.21 methods a mod overrides, each called from the 1.12.2 method that does the same. The defaults do what 1.12.2
 * does. In descriptors 1.21 {@code Item} stays the 1.12.2 {@code Item}: this class is only what mod items extend.
 */
public class Item extends net.minecraft.item.Item implements TooltipContext.HoverText, io.github.xponer2.translator.util.ItemLike {
	/**
	 * 1.21 {@code Item.getPlayerPOVHitResult}: the block the player looks at within reach, fluids as asked (spawn
	 * eggs, buckets).
	 */
	public static net.minecraft.util.math.RayTraceResult getPlayerPOVHitResult(World world, EntityPlayer player,
			io.github.xponer2.translator.util.ClipContext.Fluid fluid) {
		net.minecraft.util.math.Vec3d eyes = player.getPositionEyes(1.0F);
		net.minecraft.util.math.Vec3d look = player.getLook(1.0F);
		double reach = player.getEntityAttribute(EntityPlayer.REACH_DISTANCE).getAttributeValue();
		net.minecraft.util.math.Vec3d end = eyes.add(look.x * reach, look.y * reach, look.z * reach);
		return io.github.xponer2.translator.util.ClipContext.clip(world, new io.github.xponer2.translator.util.ClipContext(eyes, end,
				io.github.xponer2.translator.util.ClipContext.Block.OUTLINE, fluid, player));
	}

	public Item(ItemProperties properties) {
		// null when a builder call couldn't run (requiredFeatures of 1.21 feature flags): the plain item's defaults
		if (properties != null) {
			properties.applyTo(this);
		}
	}

	// --- 1.21 methods (defaults: the 1.12.2 behaviour) ---------------------------------------------------------

	public ActionResult<ItemStack> use(World level, EntityPlayer player, EnumHand hand) {
		var food = this.food();
		if (food != null) {
			ItemStack stack = player.getHeldItem(hand);
			if (player.canEat(food.canAlwaysEat())) {
				player.setActiveHand(hand);
				return new ActionResult<>(net.minecraft.util.EnumActionResult.SUCCESS, stack);
			}
			return new ActionResult<>(net.minecraft.util.EnumActionResult.FAIL, stack);
		}
		return super.onItemRightClick(level, player, hand);
	}

	/** The food of the item's properties, null when it isn't food. */
	@Nullable
	private io.github.xponer2.translator.item.FoodProperties food() {
		ItemProperties properties = ItemProperties.of(this);
		return properties == null ? null : properties.getFood();
	}

	public EnumActionResult useOn(UseOnContext context) {
		return EnumActionResult.PASS;
	}

	public EnumActionResult interactLivingEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
		return EnumActionResult.PASS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<ITextComponent> tooltip, ITooltipFlag flag) {
	}

	public ITextComponent getName(ItemStack stack) {
		return new TextComponentString(ItemNames.displayName(this, stack));
	}

	public boolean isFoil(ItemStack stack) {
		return super.hasEffect(stack);
	}

	public ItemStack finishUsingItem(ItemStack stack, World level, EntityLivingBase entity) {
		var food = this.food();
		return food != null ? food.eat(stack, level, entity) : super.onItemUseFinish(stack, level, entity);
	}

	public void releaseUsing(ItemStack stack, World level, EntityLivingBase entity, int timeLeft) {
		super.onPlayerStoppedUsing(stack, level, entity, timeLeft);
	}

	public void onUseTick(World level, EntityLivingBase entity, ItemStack stack, int remaining) {
	}

	/** 1.21 passes the user; 1.12.2 asks without one. */
	public int getUseDuration(ItemStack stack, @Nullable EntityLivingBase entity) {
		var food = this.food();
		return food != null ? food.eatDurationTicks() : super.getMaxItemUseDuration(stack);
	}

	public EnumAction getUseAnimation(ItemStack stack) {
		return this.food() != null ? EnumAction.EAT : super.getItemUseAction(stack);
	}

	public boolean isBarVisible(ItemStack stack) {
		return super.showDurabilityBar(stack);
	}

	/** 0..13 pixels in 1.21; 1.12.2 asks for the damage fraction. */
	public int getBarWidth(ItemStack stack) {
		return (int) Math.round(13.0 - super.getDurabilityForDisplay(stack) * 13.0);
	}

	public int getBarColor(ItemStack stack) {
		return super.getRGBDurabilityForDisplay(stack);
	}

	public String getDescriptionId() {
		return this.getTranslationKey();
	}

	public String getDescriptionId(ItemStack stack) {
		return this.getTranslationKey(stack);
	}

	public ItemStack getDefaultInstance() {
		return new ItemStack(this);
	}

	public net.minecraft.item.Item asItem() {
		return this;
	}

	// --- the 1.12.2 methods, calling the 1.21 ones ----------------------------------------------------------------

	@Override
	public ActionResult<ItemStack> onItemRightClick(World level, EntityPlayer player, EnumHand hand) {
		var result = this.guarded("use", () -> this.use(level, player, hand), null);
		return result != null ? result : new ActionResult<>(EnumActionResult.PASS, player.getHeldItem(hand));
	}

	/**
	 * A 1.12.2 call into the mod's 1.21 code: what fails there is logged once and the 1.12.2 default is used, instead
	 * of a crash of the game.
	 */
	private <T> T guarded(String what, java.util.function.Supplier<T> call, T fallback) {
		try {
			return call.get();
		} catch (Throwable t) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed(this.getClass().getName() + "." + what, t);
			return fallback;
		}
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		var result = this.guarded("useOn", () -> this.useOn(UseOnContext.of(player, world, pos, hand, facing, hitX, hitY, hitZ)), null);
		return result != null ? result : EnumActionResult.PASS;
	}

	@Override
	public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase target, EnumHand hand) {
		return this.guarded("interactLivingEntity", () -> this.interactLivingEntity(stack, player, target, hand), null) == EnumActionResult.SUCCESS;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> tooltip, ITooltipFlag flag) {
		try {
			TooltipContext.addInformation(this, stack, world, tooltip, flag);
		} catch (Throwable t) {
			// the search tree asks for every tooltip when the server stops: a throw there kills the server thread
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the tooltip of " + this.getClass().getName(), t);
		}
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		try {
			return this.getName(stack).getFormattedText();
		} catch (Throwable t) {
			// the game asks every item's name (statistics, search trees) when the mod may not be ready for it
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the name of " + this.getClass().getName(), t);
			return net.minecraft.util.text.translation.I18n.translateToLocal(this.getUnlocalizedNameInefficiently(stack) + ".name").trim();
		}
	}

	@Override
	public boolean hasEffect(ItemStack stack) {
		return this.isFoil(stack);
	}

	@Override
	public ItemStack onItemUseFinish(ItemStack stack, World level, EntityLivingBase entity) {
		var result = this.guarded("finishUsingItem", () -> this.finishUsingItem(stack, level, entity), null);
		return result != null ? result : stack;
	}

	@Override
	public void onPlayerStoppedUsing(ItemStack stack, World level, EntityLivingBase entity, int timeLeft) {
		this.releaseUsing(stack, level, entity, timeLeft);
	}

	@Override
	public void onUsingTick(ItemStack stack, EntityLivingBase entity, int count) {
		this.onUseTick(entity.world, entity, stack, count);
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		return this.getUseDuration(stack, null);
	}

	@Override
	public EnumAction getItemUseAction(ItemStack stack) {
		return this.getUseAnimation(stack);
	}

	@Override
	public boolean showDurabilityBar(ItemStack stack) {
		return this.isBarVisible(stack);
	}

	@Override
	public double getDurabilityForDisplay(ItemStack stack) {
		return 1.0 - this.getBarWidth(stack) / 13.0;
	}

	@Override
	public int getRGBDurabilityForDisplay(ItemStack stack) {
		return this.getBarColor(stack);
	}
}
