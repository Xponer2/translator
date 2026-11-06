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

import io.github.xponer2.translator.item.ItemProperties;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;

/**
 * Facade of the 1.21 {@code BucketItem} of a mod's fluid: a bucket that places the fluid's block (found when used: the
 * fluid's block may be registered after the bucket) and gives the empty bucket back.
 */
public class BucketItem extends ItemBucket {
	/** The bucket of each mod fluid (by its name): what an empty bucket gives on the fluid's source. */
	private static final java.util.Map<String, BucketItem> BY_FLUID = new java.util.concurrent.ConcurrentHashMap<>();

	private Fluid content;

	public BucketItem(Fluid content, ItemProperties properties) {
		super(Blocks.AIR);
		this.content = content;
		properties.applyTo(this);
		this.setContainerItem(Items.BUCKET);
		if (content != null) {
			BY_FLUID.putIfAbsent(content.getName(), this);
		}
	}

	/** The fluid's block: its own when set, else the mod's LiquidBlock of that fluid (registered after the bucket). */
	@javax.annotation.Nullable
	private Block fluidBlock() {
		Fluid content = this.getFluid();
		if (content == null) {
			return null;
		}
		if (content.getBlock() != null) {
			return content.getBlock();
		}
		for (Block block : net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS) {
			if (block instanceof net.minecraftforge.fluids.IFluidBlock fluidBlock && fluidBlock.getFluid() != null
					&& content.getName().equals(fluidBlock.getFluid().getName())) {
				return block;
			}
		}
		if (!this.reported) {
			// what the mod's fluid blocks hold instead (the bucket of a fluid whose block wasn't found places nothing)
			this.reported = true;
			var seen = new StringBuilder();
			String namespace = this.getRegistryName() == null ? "" : this.getRegistryName().getNamespace();
			for (Block block : net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS) {
				if (block instanceof net.minecraftforge.fluids.IFluidBlock fluidBlock && block.getRegistryName() != null
						&& namespace.equals(block.getRegistryName().getNamespace())) {
					Fluid held = fluidBlock.getFluid();
					seen.append(' ').append(block.getRegistryName()).append('=').append(held == null ? "null" : held.getName());
				}
			}
			org.apache.logging.log4j.LogManager.getLogger("Translator Runtime").warn("No block for the fluid {} of the bucket {} (fluid {}); the mod's fluid blocks:{}",
					content.getName(), this.getRegistryName(), System.identityHashCode(content), seen);
		}
		return null;
	}

	private boolean reported;

	/**
	 * 1.21 BucketPickup: an empty bucket on the source of a mod's fluid gives that fluid's bucket (1.12.2's bucket
	 * knows water and lava only: blood couldn't be picked up).
	 */
	public static final class Pickup {
		@net.minecraftforge.fml.common.eventhandler.SubscribeEvent
		public void onFillBucket(net.minecraftforge.event.entity.player.FillBucketEvent event) {
			RayTraceResult hit = event.getTarget();
			if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK || event.getEmptyBucket().getItem() != Items.BUCKET) {
				return;
			}
			World world = event.getWorld();
			BlockPos pos = hit.getBlockPos();
			net.minecraft.block.state.IBlockState state = world.getBlockState(pos);
			if (!(state.getBlock() instanceof LiquidBlock liquid) || state.getValue(net.minecraftforge.fluids.BlockFluidBase.LEVEL) != 0) {
				return;
			}
			Fluid fluid = liquid.getFluid();
			BucketItem bucket = fluid == null ? null : BY_FLUID.get(fluid.getName());
			if (bucket == null) {
				return;
			}
			if (!world.isRemote) {
				world.setBlockToAir(pos);
			}
			event.setFilledBucket(new ItemStack(bucket));
			event.setResult(net.minecraftforge.fml.common.eventhandler.Event.Result.ALLOW);
		}
	}

	public Fluid getFluid() {
		if (this.content == null) {
			// made before its fluid was (a mod creating its items first): the fluid of the item's name ("blood_bucket")
			var id = this.getRegistryName();
			if (id != null) {
				String name = id.getPath().endsWith("_bucket") ? id.getPath().substring(0, id.getPath().length() - 7) : id.getPath();
				Fluid found = net.minecraftforge.fluids.FluidRegistry.getFluid(name) != null ? net.minecraftforge.fluids.FluidRegistry.getFluid(name)
						: net.minecraftforge.fluids.FluidRegistry.getFluid(id.getNamespace() + ":" + name);
				if (found != null) {
					this.content = found;
					BY_FLUID.putIfAbsent(found.getName(), this);
				}
			}
		}
		return this.content;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		Block block = this.fluidBlock();
		RayTraceResult hit = this.rayTrace(world, player, false);
		if (block == null || hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
			return new ActionResult<>(EnumActionResult.PASS, stack);
		}
		BlockPos pos = hit.getBlockPos();
		if (!world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
			pos = pos.offset(hit.sideHit);
		}
		if (!world.isBlockModifiable(player, pos) || !player.canPlayerEdit(pos, hit.sideHit, stack)
				|| !world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
			return new ActionResult<>(EnumActionResult.FAIL, stack);
		}
		if (!world.isRemote) {
			world.setBlockState(pos, block.getDefaultState(), 11);
		}
		return new ActionResult<>(EnumActionResult.SUCCESS, player.capabilities.isCreativeMode ? stack : new ItemStack(Items.BUCKET));
	}

	/** 1.21 {@code getName(stack)}: the name by the 1.21 rules (1.12.2's vanilla items show "Blood Bucket.name"). */
	public net.minecraft.util.text.ITextComponent getName(net.minecraft.item.ItemStack stack) {
		return new net.minecraft.util.text.TextComponentString(io.github.xponer2.translator.text.ItemNames.displayName(this, stack));
	}

	@Override
	public String getItemStackDisplayName(net.minecraft.item.ItemStack stack) {
		try {
			return this.getName(stack).getFormattedText();
		} catch (Throwable t) {
			io.github.xponer2.translator.runtime.RuntimeStubs.failed("the name of " + this.getClass().getName(), t);
			return io.github.xponer2.translator.text.ItemNames.displayName(this, stack);
		}
	}

	/** Every item of a 1.21.1 mod is in the search tab, as in 1.21 (Forge lists an ingredient of any damage through it). */
	@Override
	protected boolean isInCreativeTab(net.minecraft.creativetab.CreativeTabs tab) {
		return tab == net.minecraft.creativetab.CreativeTabs.SEARCH || super.isInCreativeTab(tab);
	}
}
