/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121.mixin;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerDataHolder;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * NeoForge 21.1's trade by villager type: a type with no item in the table (a mod's own villager type) is no error, the
 * trade just offers nothing to that villager. 21.0 throws when the table is made, which stops the server's data load.
 */
@Mixin(VillagerTrades.EmeraldsForVillagerTypeItem.class)
abstract class VillagerTypeTradeMixin {
    @Shadow
    @Final
    private Map<VillagerType, Item> trades;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"))
    private void ladder$noMissingTypeError(Optional<VillagerType> missing, Consumer<VillagerType> error) {
    }

    @Inject(method = "getOffer", at = @At("HEAD"), cancellable = true)
    private void ladder$noOfferForMissingType(Entity trader, RandomSource random, CallbackInfoReturnable<MerchantOffer> cir) {
        if (trader instanceof VillagerDataHolder holder && trades.get(holder.getVillagerData().getType()) == null) {
            cir.setReturnValue(null);
        }
    }
}
