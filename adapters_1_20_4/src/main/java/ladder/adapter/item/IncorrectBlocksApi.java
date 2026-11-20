/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.item;

import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/**
 * 1.20.5's {@code Tier.getIncorrectBlocksForDrops()} (the blocks a tool of the tier doesn't drop): a converted mod's
 * tier implements it (an O row). 1.20.4 asks a tier for its level instead: given here, as the vanilla incorrect tag's
 * level (the same blocks fail as on 1.20.4 with that level); a digger of such a tier checks the tag itself
 * (DiggerItemMixin, as 1.21 does).
 */
public interface IncorrectBlocksApi {
    TagKey<Block> getIncorrectBlocksForDrops();

    default int getLevel() {
        return ToolAdapters.levelOf(this.getIncorrectBlocksForDrops());
    }

    /** a super call of 1.20.5's abstract method: none can be made */
    static TagKey<Block> incorrectBlocks(net.minecraft.world.item.Tier self) {
        throw new AbstractMethodError("Tier.getIncorrectBlocksForDrops");
    }
}
