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

	/**
	 * 1.21's two sides of text (the carried SignText), as the mods' sign block entities set them in their constructors:
	 * Farmer's Delight's dark canvas signs set {@code frontText = createDefaultSignText().setColor(WHITE)}, which threw
	 * without them (no block entity for any of them). The text drawn and saved is still 1.12.2's.
	 */
	@Nullable
	protected net.minecraft.world.level.block.entity.SignText frontText;
	@Nullable
	protected net.minecraft.world.level.block.entity.SignText backText;

	protected net.minecraft.world.level.block.entity.SignText createDefaultSignText() {
		return new net.minecraft.world.level.block.entity.SignText();
	}

	@Nullable
	public net.minecraft.world.level.block.entity.SignText getFrontText() {
		return this.frontText;
	}

	@Nullable
	public net.minecraft.world.level.block.entity.SignText getBackText() {
		return this.backText;
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
		return super.writeToNBT(tag);
	}
}
