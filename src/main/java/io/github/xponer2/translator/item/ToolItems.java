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
package io.github.xponer2.translator.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.UUID;

/**
 * Helpers for the 1.12.2 versions of the 1.21 tiered tools.
 */
public final class ToolItems {
    private ToolItems() { }

    /**
     * The main hand modifiers of a 1.21 tool ({@code createAttributes(tier, attackDamage, attackSpeed)}): the
     * modifier ids are the protected ones of {@code Item}, passed in by the tool.
     */
    public static Multimap<String, AttributeModifier> mainHandModifiers(EntityEquipmentSlot slot, UUID damageId, UUID speedId,
                                                                        float attackDamage, float attackSpeed) {
        Multimap<String, AttributeModifier> modifiers = HashMultimap.create();
        if (slot == EntityEquipmentSlot.MAINHAND) {
            modifiers.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(damageId, "Tool modifier", attackDamage, 0));
            modifiers.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(speedId, "Tool modifier", attackSpeed, 0));
        }
        return modifiers;
    }

    public static boolean isRepairable(Tier tier, ItemStack repair) {
        return tier.getRepairIngredient().apply(repair);
    }
}
