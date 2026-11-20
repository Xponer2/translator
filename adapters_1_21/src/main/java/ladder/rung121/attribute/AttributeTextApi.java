/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.attribute;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.TooltipFlag;

/**
 * NeoForge 21.1's tooltip members of {@code IAttributeExtension}: a converted mod's attribute class gets this interface
 * (the O rows), so its overrides are what AttributeText (and AttributeUtil's lines) ask.
 */
public interface AttributeTextApi {
    default MutableComponent toValueComponent(@Nullable AttributeModifier.Operation op, double value, TooltipFlag flag) {
        return AttributeText.defaultValueComponent(op, value);
    }

    default MutableComponent toComponent(AttributeModifier modifier, TooltipFlag flag) {
        return AttributeText.defaultComponent((Attribute) this, modifier, flag);
    }

    default Component getDebugInfo(AttributeModifier modifier, TooltipFlag flag) {
        return AttributeText.defaultDebugInfo(modifier, flag);
    }

    @Nullable
    default ResourceLocation getBaseId() {
        return AttributeText.defaultBaseId((Attribute) this);
    }

    default MutableComponent toBaseComponent(double value, double entityBase, boolean merged, TooltipFlag flag) {
        return AttributeText.defaultBaseComponent((Attribute) this, value, entityBase, merged, flag);
    }

    default TextColor getMergedStyle(boolean isPositive) {
        return AttributeText.defaultMergedStyle((Attribute) this, isPositive);
    }
}
