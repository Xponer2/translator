/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.attribute;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.annotation.Nullable;

import ladder.rung121.Dispatch;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.TooltipFlag;

/**
 * NeoForge 21.1's attribute tooltip text on 1.21 (NeoForge 21.0 has no IAttributeExtension at all: the converter passes
 * the Attribute itself, the game class that implements it above): each member asked of an
 * attribute (the X rows, and AttributeUtil) is a converted mod attribute's own override when it has one, else 21.1's
 * default; the defaults are the O rows' super calls too.
 */
public final class AttributeText {
    private static final DecimalFormat FORMAT = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
    private static final TextColor MERGED_RED = TextColor.fromRgb(0xF93131);
    private static final TextColor MERGED_BLUE = TextColor.fromRgb(0x7A7AF9);
    private static final TextColor MERGED_GRAY = TextColor.fromRgb(0xCCCCCC);

    private AttributeText() {
    }

    static AttributeTextApi own(Object attribute, String name, Class<?>... parameters) {
        return attribute instanceof AttributeTextApi api && Dispatch.declares(attribute.getClass(), name, parameters) ? api : null;
    }

    // ---------------------------------------------------------------- the calls (an override, else the default)

    public static MutableComponent toValueComponent(Attribute self, @Nullable AttributeModifier.Operation op, double value, TooltipFlag flag) {
        AttributeTextApi api = own(self, "toValueComponent", AttributeModifier.Operation.class, double.class, TooltipFlag.class);
        return api != null ? api.toValueComponent(op, value, flag) : defaultValueComponent(op, value);
    }

    public static MutableComponent toComponent(Attribute self, AttributeModifier modifier, TooltipFlag flag) {
        AttributeTextApi api = own(self, "toComponent", AttributeModifier.class, TooltipFlag.class);
        return api != null ? api.toComponent(modifier, flag) : defaultComponent(self, modifier, flag);
    }

    public static Component getDebugInfo(Attribute self, AttributeModifier modifier, TooltipFlag flag) {
        AttributeTextApi api = own(self, "getDebugInfo", AttributeModifier.class, TooltipFlag.class);
        return api != null ? api.getDebugInfo(modifier, flag) : defaultDebugInfo(modifier, flag);
    }

    @Nullable
    public static ResourceLocation getBaseId(Attribute self) {
        AttributeTextApi api = own(self, "getBaseId");
        return api != null ? api.getBaseId() : defaultBaseId(self);
    }

    public static MutableComponent toBaseComponent(Attribute self, double value, double entityBase, boolean merged, TooltipFlag flag) {
        AttributeTextApi api = own(self, "toBaseComponent", double.class, double.class, boolean.class, TooltipFlag.class);
        return api != null ? api.toBaseComponent(value, entityBase, merged, flag) : defaultBaseComponent(self, value, entityBase, merged, flag);
    }

    public static TextColor getMergedStyle(Attribute self, boolean isPositive) {
        AttributeTextApi api = own(self, "getMergedStyle", boolean.class);
        return api != null ? api.getMergedStyle(isPositive) : defaultMergedStyle(self, isPositive);
    }

    // ---------------------------------------------------------------- the O rows' defaults (a mod attribute's super calls)

    public static MutableComponent superValueComponent(Attribute self, @Nullable AttributeModifier.Operation op, double value, TooltipFlag flag) {
        return defaultValueComponent(op, value);
    }

    public static MutableComponent superComponent(Attribute self, AttributeModifier modifier, TooltipFlag flag) {
        return defaultComponent(self, modifier, flag);
    }

    public static Component superDebugInfo(Attribute self, AttributeModifier modifier, TooltipFlag flag) {
        return defaultDebugInfo(modifier, flag);
    }

    @Nullable
    public static ResourceLocation superBaseId(Attribute self) {
        return defaultBaseId(self);
    }

    public static MutableComponent superBaseComponent(Attribute self, double value, double entityBase, boolean merged, TooltipFlag flag) {
        return defaultBaseComponent(self, value, entityBase, merged, flag);
    }

    public static TextColor superMergedStyle(Attribute self, boolean isPositive) {
        return defaultMergedStyle(self, isPositive);
    }

    // ---------------------------------------------------------------- 21.1's defaults

    static boolean flatOrNone(@Nullable AttributeModifier.Operation op) {
        return op == null || op == AttributeModifier.Operation.ADD_VALUE;
    }

    /** a value shown flat, or a multiplier shown as a percentage */
    static MutableComponent defaultValueComponent(@Nullable AttributeModifier.Operation op, double value) {
        return flatOrNone(op) ? Component.translatable("neoforge.value.flat", FORMAT.format(value))
                : Component.translatable("neoforge.value.percent", FORMAT.format(value * 100));
    }

    /** "+value name" or "value name", in the attribute's colour for the sign, with the debug part when advanced */
    static MutableComponent defaultComponent(Attribute attribute, AttributeModifier modifier, TooltipFlag flag) {
        double value = modifier.amount();
        Component name = Component.translatable(attribute.getDescriptionId());
        Component shown = toValueComponent(attribute, modifier.operation(), value, flag);
        MutableComponent line = Component.translatable(value > 0 ? "neoforge.modifier.plus" : "neoforge.modifier.take", shown, name)
                .withStyle(attribute.getStyle(value > 0));
        return line.append(getDebugInfo(attribute, modifier, flag));
    }

    /** with advanced tooltips: the operation and the real amount, grey ([+2], [+0.5x], [x1.2]) */
    static Component defaultDebugInfo(AttributeModifier modifier, TooltipFlag flag) {
        if (!flag.isAdvanced()) {
            return CommonComponents.EMPTY;
        }
        double amount = (modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL ? 1 : 0) + modifier.amount();
        String value = FORMAT.format(amount);
        String text = switch (modifier.operation()) {
            case ADD_VALUE -> String.format(Locale.ROOT, amount > 0 ? "[+%s]" : "[%s]", value);
            case ADD_MULTIPLIED_BASE -> String.format(Locale.ROOT, amount > 0 ? "[+%sx]" : "[%sx]", value);
            case ADD_MULTIPLIED_TOTAL -> String.format(Locale.ROOT, "[x%s]", value);
        };
        return Component.literal(" ").append(Component.literal(text).withStyle(ChatFormatting.GRAY));
    }

    /** the id of the attribute's "base" modifier (attack damage, attack speed, entity reach) */
    @Nullable
    static ResourceLocation defaultBaseId(Attribute attribute) {
        if (attribute == Attributes.ATTACK_DAMAGE.value()) {
            return AttributeUtil.BASE_ATTACK_DAMAGE_ID;
        } else if (attribute == Attributes.ATTACK_SPEED.value()) {
            return AttributeUtil.BASE_ATTACK_SPEED_ID;
        } else if (attribute == Attributes.ENTITY_INTERACTION_RANGE.value()) {
            return AttributeUtil.BASE_ENTITY_REACH_ID;
        }
        return null;
    }

    /** "value name" of a base modifier (with the entity's base value, and the debug part when advanced and not merged) */
    static MutableComponent defaultBaseComponent(Attribute attribute, double value, double entityBase, boolean merged, TooltipFlag flag) {
        MutableComponent line = Component.translatable("attribute.modifier.equals.0", toValueComponent(attribute, null, value, flag),
                Component.translatable(attribute.getDescriptionId()));
        if (flag.isAdvanced() && !merged) {
            double bonus = value - entityBase;
            String bonusText = String.format(Locale.ROOT, bonus > 0 ? " + %s" : " - %s", FORMAT.format(Math.abs(bonus)));
            line.append(CommonComponents.SPACE).append(Component.translatable("neoforge.attribute.debug.base", FORMAT.format(entityBase), bonusText)
                    .withStyle(ChatFormatting.GRAY));
        }
        return line;
    }

    /** the colour of a merged line: by the attribute's sentiment (good blue, bad red, neutral grey) */
    static TextColor defaultMergedStyle(Attribute attribute, boolean isPositive) {
        // 1.21's sentiment is private: its style for a positive value tells it (positive blue, negative red, neutral grey)
        ChatFormatting good = attribute.getStyle(true);
        if (good == ChatFormatting.BLUE) {
            return isPositive ? MERGED_BLUE : MERGED_RED;
        } else if (good == ChatFormatting.RED) {
            return isPositive ? MERGED_RED : MERGED_BLUE;
        }
        return MERGED_GRAY;
    }
}
