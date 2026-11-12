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

import javax.annotation.Nullable;

import io.github.xponer2.translator.registry.BlockEntityType;

import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.level.block.entity.SignText;

/**
 * 1.21 {@code SignBlockEntity} (and the hanging sign's, a subclass): 1.12.2's sign block entity, made with the 1.21
 * constructors (a mod's sign block entity calls super(type, pos, state): 1.12.2's has none, so no mod sign had its block
 * entity and none was drawn). Its text is 1.12.2's; {@code client.SignRenderers} draws it with the wood's texture.
 */
public class SignBlockEntity extends TileEntitySign {
	@Nullable
	private final BlockEntityType<?> type;

	/** 1.12.2 makes a saved one with this. */
	public SignBlockEntity() {
		this.type = null;
	}

	public SignBlockEntity(BlockPos pos, IBlockState state) {
		this(null, pos, state);
	}

	public SignBlockEntity(@Nullable BlockEntityType<?> type, BlockPos pos, IBlockState state) {
		this.type = type;
		this.setPos(pos.toImmutable());
		try {
			this.frontText = this.createDefaultSignText();
			this.backText = this.createDefaultSignText();
		} catch (LinkageError e) {
			// no carried 1.21 API: the 1.12.2 text only
		}
	}

	// --- 1.21 text: the front is 1.12.2's lines with a color and glow (SignTexts) -----------------------------------------

	/** 1.21's front and back text (mods set them in their constructors: Farmer's Delight's dark canvas signs write white). */
	@Nullable
	protected SignText frontText;
	@Nullable
	protected SignText backText;

	/** Protected as in 1.21: a mod's override (Farmer's Delight's hanging canvas sign) may keep it protected. */
	protected SignText createDefaultSignText() {
		return new SignText();
	}

	public SignText getFrontText() {
		return SignTexts.getFrontText(this);
	}

	public SignText getBackText() {
		return SignTexts.getBackText(this);
	}

	public SignText getText(boolean front) {
		return SignTexts.getText(this, front);
	}

	public boolean setText(SignText text, boolean front) {
		return SignTexts.setText(this, text, front);
	}

	public int getMaxTextLineWidth() {
		return SignTexts.getMaxTextLineWidth(this);
	}

	public int getTextLineHeight() {
		return SignTexts.getTextLineHeight(this);
	}

	/** The front's color and glow, kept with 1.12.2's lines. */
	@Override
	public void readFromNBT(net.minecraft.nbt.NBTTagCompound tag) {
		super.readFromNBT(tag);
		if (this.frontText == null) {
			this.frontText = this.createDefaultSignText();
		}
		try {
			if (tag.hasKey("translator_color")) {
				this.frontText = this.frontText.setColor(net.minecraft.item.EnumDyeColor.byMetadata(tag.getInteger("translator_color")));
			}
			if (tag.hasKey("translator_glow")) {
				this.frontText = this.frontText.setHasGlowingText(tag.getBoolean("translator_glow"));
			}
		} catch (RuntimeException | LinkageError e) {
			// the carried text class is not there: 1.12.2's lines only
		}
	}

	@Nullable
	public BlockEntityType<?> getType() {
		return this.type;
	}

	/**
	 * Saved and sent with its class's id: a class its type didn't register (its sample was another class, or none could
	 * be made) gets the type's id, else one of its own; 1.12.2 threw for it ("missing a mapping") and the server stopped.
	 */
	@Override
	@SuppressWarnings("deprecation")
	public net.minecraft.nbt.NBTTagCompound writeToNBT(net.minecraft.nbt.NBTTagCompound tag) {
		if (net.minecraft.tileentity.TileEntity.getKey(this.getClass()) == null) {
			net.minecraft.util.ResourceLocation id = this.type == null ? null
					: io.github.xponer2.translator.util.BuiltInRegistries.byKey(io.github.xponer2.translator.util.Registries.BLOCK_ENTITY_TYPE).getKey(this.type);
			if (id == null) {
				id = new net.minecraft.util.ResourceLocation("translator", "sign_" + this.getClass().getName().toLowerCase(java.util.Locale.ROOT).replace('.', '_').replace('$', '_'));
			}
			net.minecraftforge.fml.common.registry.GameRegistry.registerTileEntity(this.getClass(), id);
		}
		if (this.frontText != null) {
			try {
				tag.setInteger("translator_color", this.frontText.getColor().getMetadata());
				tag.setBoolean("translator_glow", this.frontText.hasGlowingText());
			} catch (RuntimeException | LinkageError e) {
				// 1.12.2's lines only
			}
		}
		return super.writeToNBT(tag);
	}
}
