package io.github.xponer2.translator.item;

import javax.annotation.Nullable;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.stats.StatList;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

import io.github.xponer2.translator.util.Platform;

/**
 * 1.21 {@code CrossbowItem} (1.12.2 has no crossbow): hold use to load an arrow (25 ticks), use again to shoot it. The
 * loaded arrow is kept in the stack NBT ({@code ChargedProjectiles}, {@code Charged}). Fireworks and the 1.14+
 * crossbow enchantments and sounds don't exist in 1.12.2: only arrows are loaded, and the sounds are close 1.12.2
 * ones. {@link #customArrow} is the NeoForge hook for the shot arrows.
 */
public class CrossbowItem extends Item {
	private static final int CHARGE_DURATION = 25;
	private static final String CHARGED = "Charged";
	private static final String PROJECTILES = "ChargedProjectiles";
	protected boolean startSoundPlayed = false;
	protected boolean midLoadSoundPlayed = false;

	public CrossbowItem(ItemProperties properties) {
		this.setMaxStackSize(1);
		this.setMaxDamage(465);
		properties.applyTo(this);
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(World level, EntityPlayer player, EnumHand hand) {
		var stack = player.getHeldItem(hand);
		if (isCharged(stack)) {
			this.shoot(level, player, stack);
			return new ActionResult<>(EnumActionResult.SUCCESS, stack);
		}

		if (!findArrows(player).isEmpty() || player.capabilities.isCreativeMode) {
			this.startSoundPlayed = false;
			this.midLoadSoundPlayed = false;
			player.setActiveHand(hand);
			return new ActionResult<>(EnumActionResult.SUCCESS, stack);
		}

		return new ActionResult<>(EnumActionResult.FAIL, stack);
	}

	// 1.21 onUseTick
	@Override
	public void onUsingTick(ItemStack stack, EntityLivingBase entity, int timeLeft) {
		var level = entity.world;
		if (level.isRemote)
			return;

		float f = (float) (this.getMaxItemUseDuration(stack) - timeLeft) / (float) getChargeDuration(stack, entity);
		if (f < 0.2F) {
			this.startSoundPlayed = false;
			this.midLoadSoundPlayed = false;
		}

		if (f >= 0.2F && !this.startSoundPlayed) {
			this.startSoundPlayed = true;
			playSound(level, entity, SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, 0.5F);
		}

		if (f >= 0.5F && !this.midLoadSoundPlayed) {
			this.midLoadSoundPlayed = true;
			playSound(level, entity, SoundEvents.ITEM_ARMOR_EQUIP_CHAIN, 0.5F);
		}
	}

	// 1.21 releaseUsing
	@Override
	public void onPlayerStoppedUsing(ItemStack stack, World level, EntityLivingBase entity, int timeLeft) {
		int i = this.getMaxItemUseDuration(stack) - timeLeft;
		this.loadIfCharged(stack, level, entity, i);
	}

	/** Loads an arrow if the crossbow was drawn for {@code drawn} ticks or longer. */
	protected void loadIfCharged(ItemStack stack, World level, EntityLivingBase entity, int drawn) {
		if (drawn >= getChargeDuration(stack, entity) && !isCharged(stack) && entity instanceof EntityPlayer player && tryLoadProjectile(player, stack)) {
			playSound(level, entity, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, 1.0F);
		}
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		return 72000;
	}

	@Override
	public EnumAction getItemUseAction(ItemStack stack) {
		return EnumAction.BOW;
	}

	@Override
	public int getItemEnchantability() {
		return 1;
	}

	/** NeoForge {@code ProjectileWeaponItem#customArrow}: changes a shot arrow. */
	public EntityArrow customArrow(EntityArrow arrow, ItemStack projectile, ItemStack weapon) {
		return arrow;
	}

	private static boolean tryLoadProjectile(EntityPlayer player, ItemStack crossbow) {
		var ammo = findArrows(player);
		var creative = player.capabilities.isCreativeMode;
		if (ammo.isEmpty()) {
			if (!creative)
				return false;
			ammo = new ItemStack(Items.ARROW);
		}

		var loaded = ammo.copy();
		loaded.setCount(1);
		if (!creative) {
			ammo.shrink(1);
			if (ammo.isEmpty()) {
				player.inventory.deleteStack(ammo);
			}
		}

		var projectiles = new NBTTagList();
		projectiles.appendTag(loaded.writeToNBT(new NBTTagCompound()));
		var tag = crossbow.hasTagCompound() ? crossbow.getTagCompound() : new NBTTagCompound();
		tag.setTag(PROJECTILES, projectiles);
		tag.setBoolean(CHARGED, true);
		crossbow.setTagCompound(tag);
		return true;
	}

	private void shoot(World level, EntityPlayer player, ItemStack crossbow) {
		var tag = crossbow.getTagCompound();
		var projectiles = tag.getTagList(PROJECTILES, 10);
		if (!level.isRemote) {
			for (int i = 0; i < projectiles.tagCount(); i++) {
				var ammo = new ItemStack(projectiles.getCompoundTagAt(i));
				var arrowItem = ammo.getItem() instanceof ItemArrow a ? a : (ItemArrow) Items.ARROW;
				var arrow = arrowItem.createArrow(level, ammo, player);
				arrow.shoot(player, player.rotationPitch, player.rotationYaw, 0.0F, 3.15F, 1.0F);
				arrow.setIsCritical(true);
				arrow = this.customArrow(arrow, ammo, crossbow);
				if (player.capabilities.isCreativeMode) {
					arrow.pickupStatus = EntityArrow.PickupStatus.CREATIVE_ONLY;
				}

				Platform.addFreshEntity(level, arrow);
				crossbow.damageItem(1, player);
			}
		}

		level.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS,
				1.0F, 1.0F / (itemRand.nextFloat() * 0.4F + 1.2F) + 0.5F);
		player.addStat(StatList.getObjectUseStats(this));
		tag.removeTag(PROJECTILES);
		tag.setBoolean(CHARGED, false);
	}

	// off hand, main hand, then the inventory, like the 1.12.2 bow
	private static ItemStack findArrows(EntityPlayer player) {
		if (player.getHeldItemOffhand().getItem() instanceof ItemArrow)
			return player.getHeldItemOffhand();
		if (player.getHeldItemMainhand().getItem() instanceof ItemArrow)
			return player.getHeldItemMainhand();

		for (int i = 0; i < player.inventory.getSizeInventory(); ++i) {
			var stack = player.inventory.getStackInSlot(i);
			if (stack.getItem() instanceof ItemArrow)
				return stack;
		}

		return ItemStack.EMPTY;
	}

	protected static void playSound(World level, EntityLivingBase entity, SoundEvent sound, float volume) {
		level.playSound(null, entity.posX, entity.posY, entity.posZ, sound, SoundCategory.PLAYERS, volume, 1.0F);
	}

	public static boolean isCharged(ItemStack stack) {
		return stack.hasTagCompound() && stack.getTagCompound().getBoolean(CHARGED);
	}

	public static int getChargeDuration(ItemStack stack, @Nullable EntityLivingBase entity) {
		return CHARGE_DURATION;
	}
}
