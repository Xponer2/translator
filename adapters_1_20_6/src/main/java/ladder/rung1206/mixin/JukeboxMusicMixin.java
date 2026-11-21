/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import ladder.rung1206.jukebox.Jukebox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.RecordItem;

/** The client plays the song of an item with 1.21's jukebox_playable when a jukebox starts it (level event 1010). */
@Mixin(LevelRenderer.class)
abstract class JukeboxMusicMixin {
    @Shadow
    private ClientLevel level;

    @Shadow
    private void playStreamingMusic(SoundEvent sound, BlockPos pos) {
    }

    @Inject(method = "levelEvent", at = @At("HEAD"), cancellable = true)
    private void ladder$song(int type, BlockPos pos, int data, CallbackInfo ci) {
        if (type != 1010 || this.level == null) {
            return;
        }
        Item item = Item.byId(data);
        if (item instanceof RecordItem) {
            return;
        }
        Jukebox.song(this.level.registryAccess(), new ItemStack(item)).ifPresent(song -> {
            this.playStreamingMusic(song.value().soundEvent().value(), pos);
            Minecraft.getInstance().gui.setNowPlaying(song.value().description());
            ci.cancel();
        });
    }
}
