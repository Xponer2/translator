/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package ladder.adapter.misc;

import ladder.adapter.mixin.EntityAccess;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

/** Small members of the newer versions, on 1.20.4 (receiver first): each in 1.20.4's own terms. */
public final class MiscAdapters {
    private MiscAdapters() {
    }

    /** 1.20.5's {@code Entity.getRandom()}: the entity's own random (1.20.4 has it, protected). */
    public static RandomSource getRandom(Entity entity) {
        return ((EntityAccess) entity).ladder$random();
    }

    /** 1.20.5's {@code Entity.registryAccess()}: its level's. */
    public static RegistryAccess registryAccess(Entity entity) {
        return entity.level().registryAccess();
    }

    /** 1.20.5's {@code hurtAndBreak(amount, entity, slot)}: 1.20.4's with the break event of that slot. */
    public static void hurtAndBreak(ItemStack stack, int amount, LivingEntity entity, EquipmentSlot slot) {
        stack.hurtAndBreak(amount, entity, e -> e.broadcastBreakEvent(slot));
    }

    /** 1.20.5's {@code DyeColor.getTextureDiffuseColor()}: the ARGB of 1.20.4's three floats. */
    public static int getTextureDiffuseColor(DyeColor color) {
        float[] rgb = color.getTextureDiffuseColors();
        return FastColor.ARGB32.color(255, Math.round(rgb[0] * 255.0F), Math.round(rgb[1] * 255.0F), Math.round(rgb[2] * 255.0F));
    }
}
