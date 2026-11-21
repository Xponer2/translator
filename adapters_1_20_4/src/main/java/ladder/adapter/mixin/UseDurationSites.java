/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import ladder.adapter.item.UseDurations;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.level.Level;

/**
 * The game's other uses of a stack's use duration where the user is known (1.21 passes it: getUseDuration(stack,
 * entity)): a converted mod item's override gets it there too (UseDurations).
 */
final class UseDurationSites {
    private UseDurationSites() {
    }

    @Mixin(BowItem.class)
    abstract static class Bow {
        @Redirect(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BowItem;getUseDuration(Lnet/minecraft/world/item/ItemStack;)I"))
        private int ladder$withUser(BowItem self, ItemStack stack, ItemStack same, Level level, LivingEntity entity, int left) {
            return UseDurations.itemDuration(self, stack, entity);
        }
    }

    @Mixin(TridentItem.class)
    abstract static class Trident {
        @Redirect(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/TridentItem;getUseDuration(Lnet/minecraft/world/item/ItemStack;)I"))
        private int ladder$withUser(TridentItem self, ItemStack stack, ItemStack same, Level level, LivingEntity entity, int left) {
            return UseDurations.itemDuration(self, stack, entity);
        }
    }

    @Mixin(CrossbowItem.class)
    abstract static class Crossbow {
        @Redirect(method = "releaseUsing", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/CrossbowItem;getUseDuration(Lnet/minecraft/world/item/ItemStack;)I"))
        private int ladder$withUser(CrossbowItem self, ItemStack stack, ItemStack same, Level level, LivingEntity entity, int left) {
            return UseDurations.itemDuration(self, stack, entity);
        }

        @Redirect(method = "onUseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseDuration()I"))
        private int ladder$tickWithUser(ItemStack stack, Level level, LivingEntity entity, ItemStack same, int left) {
            return UseDurations.stackDuration(stack, entity);
        }
    }

    @Mixin(BrushItem.class)
    abstract static class Brush {
        @Redirect(method = "onUseTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/BrushItem;getUseDuration(Lnet/minecraft/world/item/ItemStack;)I"))
        private int ladder$withUser(BrushItem self, ItemStack stack, Level level, LivingEntity entity, ItemStack same, int left) {
            return UseDurations.itemDuration(self, stack, entity);
        }
    }

    @Mixin(net.minecraft.server.level.ServerPlayerGameMode.class)
    abstract static class GameMode {
        @Redirect(method = "useItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseDuration()I"))
        private int ladder$withUser(ItemStack stack, ServerPlayer player, Level level, ItemStack same, InteractionHand hand) {
            return UseDurations.stackDuration(stack, player);
        }
    }

    @Mixin(net.minecraft.world.entity.monster.Witch.class)
    abstract static class Witch {
        @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseDuration()I"))
        private int ladder$withUser(ItemStack stack) {
            return UseDurations.stackDuration(stack, (LivingEntity) (Object) this);
        }
    }
}
