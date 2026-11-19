/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.mixin;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import com.mojang.authlib.GameProfile;

import net.minecraft.world.level.block.entity.SkullBlockEntity;

/** 1.20.4's private skull profile lookup by name (public in 1.20.5), through its own cache. */
@Mixin(SkullBlockEntity.class)
public interface SkullBlockEntityAccess {
    @Invoker("fetchGameProfile")
    static CompletableFuture<Optional<GameProfile>> ladder$fetchGameProfile(String name) {
        throw new AssertionError();
    }
}
