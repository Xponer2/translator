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
import java.util.function.UnaryOperator;

import javax.annotation.Nullable;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementManager;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.advancements.PlayerAdvancements;
import net.minecraft.inventory.InventoryCraftResult;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/**
 * Calls of the 1.21.1 mods that 1.12.2's classes lack, found often across the mods (runtime, a missing call on those
 * classes comes here): a rarity's name style (tooltips: Farmer's Delight), a block snapshot, the recipe a crafting
 * result came from, a container's fluid as an Optional, and the advancements by their 1.21 holders (a guide or a
 * machine checking that the player has one: Mekanism).
 */
public final class CommonGapBridges {
	private CommonGapBridges() {
	}

	/** NeoForge {@code Rarity.getStyleModifier()}: the name's style in the rarity's colour. */
	public static UnaryOperator<Style> getStyleModifier(EnumRarity rarity) {
		return style -> style.createShallowCopy().setColor(rarity.color);
	}

	/** NeoForge {@code BlockSnapshot.create(dimension, level, pos)}: 1.12.2 Forge's snapshot of that block. */
	public static BlockSnapshot create(Object dimension, World world, BlockPos pos) {
		return BlockSnapshot.getBlockSnapshot(world, pos);
	}

	public static BlockSnapshot create(Object dimension, World world, BlockPos pos, int flags) {
		return BlockSnapshot.getBlockSnapshot(world, pos, flags);
	}

	/** 1.21 {@code resultContainer.setRecipeUsed(holder)}: the 1.12.2 recipe registered under the holder's id. */
	public static void setRecipeUsed(InventoryCraftResult result, @Nullable io.github.xponer2.translator.recipe.RecipeHolder<?> holder) {
		IRecipe recipe = holder == null ? null : ForgeRegistries.RECIPES.getValue(holder.id());
		result.setRecipeUsed(recipe);
	}

	/**
	 * 1.21 {@code RecipeCraftingHolder.setRecipeUsed(level, player, holder)}: whether the player may craft it (1.12.2's
	 * limited crafting asks the recipe book), then kept as the used recipe.
	 */
	public static boolean setRecipeUsed(InventoryCraftResult result, World world, net.minecraft.entity.player.EntityPlayerMP player,
			@Nullable io.github.xponer2.translator.recipe.RecipeHolder<?> holder) {
		IRecipe recipe = holder == null ? null : ForgeRegistries.RECIPES.getValue(holder.id());
		if (recipe != null && !recipe.isDynamic() && world.getGameRules().getBoolean("doLimitedCrafting")
				&& !player.getRecipeBook().isUnlocked(recipe)) {
			return false;
		}
		result.setRecipeUsed(recipe);
		return true;
	}

	/** 1.21 {@code RecipeCraftingHolder.awardUsedRecipes(player, items)}: the used recipe unlocked in the player's book. */
	public static void awardUsedRecipes(InventoryCraftResult result, net.minecraft.entity.player.EntityPlayer player, java.util.List<?> items) {
		IRecipe recipe = result.getRecipeUsed();
		if (recipe != null && !recipe.isDynamic()) {
			player.unlockRecipes(java.util.Collections.singletonList(recipe));
		}
	}

	/** 1.21 {@code serverPlayer.triggerRecipeCrafted(holder, items)}: 1.12.2 has no recipe-crafted trigger. */
	public static void triggerRecipeCrafted(net.minecraft.entity.player.EntityPlayerMP player, @Nullable Object holder, java.util.List<?> items) {
	}

	/** NeoForge {@code FluidUtil.getFluidHandler(level, pos, side)}: the block's fluid handler, empty when it has none. */
	public static Optional<net.minecraftforge.fluids.capability.IFluidHandler> getFluidHandler(World world, BlockPos pos,
			@Nullable net.minecraft.util.EnumFacing side) {
		return Optional.ofNullable(FluidUtil.getFluidHandler(world, pos, side));
	}

	/** NeoForge {@code FluidUtil.tryPlaceFluid(player, level, hand, pos, handler, fluid)}: 1.12.2's takes no hand. */
	public static boolean tryPlaceFluid(@Nullable net.minecraft.entity.player.EntityPlayer player, World world, net.minecraft.util.EnumHand hand,
			BlockPos pos, net.minecraftforge.fluids.capability.IFluidHandler handler, FluidStack fluid) {
		return FluidUtil.tryPlaceFluid(player, world, pos, handler, fluid);
	}

	/** NeoForge {@code IShearable.isShearable(player, stack, level, pos)}: 1.12.2's asks the stack, world and position. */
	public static boolean isShearable(net.minecraftforge.common.IShearable shearable, @Nullable net.minecraft.entity.player.EntityPlayer player,
			ItemStack stack, World world, BlockPos pos) {
		return shearable.isShearable(stack, world, pos);
	}

	/** NeoForge {@code FluidUtil.getFluidContained(stack)}: the fluid of a container item, empty when none. */
	public static Optional<FluidStack> getFluidContained(ItemStack stack) {
		return Optional.ofNullable(FluidUtil.getFluidContained(stack));
	}

	// --- advancements: a 1.21 AdvancementHolder is (id, advancement); 1.12.2's Advancement carries both ---------------

	/** 1.21 {@code server.getAdvancements().get(id)}: the advancement's holder, null when there is none. */
	@Nullable
	public static Object get(AdvancementManager manager, ResourceLocation id) {
		Advancement advancement = manager.getAdvancement(id);
		return advancement == null ? null : holder(advancement);
	}

	/** 1.21 {@code playerAdvancements.getOrStartProgress(holder)}. */
	public static AdvancementProgress getOrStartProgress(PlayerAdvancements advancements, Object holder) {
		return advancements.getProgress(advancement(holder));
	}

	/** 1.21 {@code playerAdvancements.award(holder, criterion)}. */
	public static boolean award(PlayerAdvancements advancements, Object holder, String criterion) {
		return advancements.grantCriterion(advancement(holder), criterion);
	}

	/** 1.21 {@code playerAdvancements.revoke(holder, criterion)}. */
	public static boolean revoke(PlayerAdvancements advancements, Object holder, String criterion) {
		return advancements.revokeCriterion(advancement(holder), criterion);
	}

	private static Advancement advancement(Object holder) {
		if (holder instanceof Advancement advancement) {
			return advancement;
		}
		try {
			return (Advancement) holder.getClass().getMethod("value").invoke(holder);
		} catch (ReflectiveOperationException | RuntimeException e) {
			throw new IllegalArgumentException("Not an advancement: " + holder, e);
		}
	}

	/** The carried 1.21 AdvancementHolder of an advancement (its record: id, advancement); the advancement if there is none. */
	private static Object holder(Advancement advancement) {
		try {
			Class<?> type = Class.forName("net.minecraft.advancements.AdvancementHolder", true, net.minecraft.launchwrapper.Launch.classLoader);
			return type.getConstructor(ResourceLocation.class, Advancement.class).newInstance(advancement.getId(), advancement);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return advancement;
		}
	}
}
