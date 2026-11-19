/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.block;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Player profiles of 1.20.5 on 1.20.4: ExtraCodecs.PLAYER_NAME (new in 1.20.5) and SkullBlockEntity.fetchGameProfile by
 * id (1.20.4 fetches by name only): the running server's session service is asked, as 1.20.5's profile cache asks it.
 */
public final class ProfileAdapters {
    private static final Codec<String> PLAYER_NAME = Codec.STRING.flatXmap(ProfileAdapters::checkName, ProfileAdapters::checkName);

    private ProfileAdapters() {
    }

    /** 1.20.5's StringUtil.isValidPlayerName: at most 16 characters, none of them space or control */
    static DataResult<String> checkName(String name) {
        return name.length() <= 16 && name.chars().allMatch(c -> c > 32 && c < 127)
                ? DataResult.success(name) : DataResult.error(() -> "Player name contained disallowed characters: '" + name + "'");
    }

    public static Codec<String> playerName() {
        return PLAYER_NAME;
    }

    /**
     * SkullBlockEntity.fetchGameProfile(name) (private in 1.20.4): the name's profile from the server's profile cache,
     * then its textures by id, as 1.20.5 does it
     */
    public static CompletableFuture<Optional<GameProfile>> fetchGameProfile(String name) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || server.getProfileCache() == null) {
            // a client of another server: 1.20.4's own skull lookup by name (its cache is set up on the client too)
            return ladder.adapter.mixin.SkullBlockEntityAccess.ladder$fetchGameProfile(name);
        }
        return server.getProfileCache().getAsync(name).thenCompose(found -> found.isEmpty() ? CompletableFuture.completedFuture(found)
                : fetchGameProfile(found.get().getId()).thenApply(full -> full.isPresent() ? full : found));
    }

    /** SkullBlockEntity.fetchGameProfile(id): the profile with its textures, or empty when there is no server to ask */
    public static CompletableFuture<Optional<GameProfile>> fetchGameProfile(UUID id) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        com.mojang.authlib.minecraft.MinecraftSessionService sessions = server != null ? server.getSessionService()
                : net.neoforged.fml.loading.FMLEnvironment.dist.isClient() ? ClientProfiles.sessionService() : null;
        if (sessions == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return CompletableFuture.supplyAsync(() -> {
            ProfileResult result = sessions.fetchProfile(id, true);
            return Optional.ofNullable(result).map(ProfileResult::profile);
        }, Util.backgroundExecutor());
    }

    /** SkullBlockEntity.getOwnerProfile(): 1.20.4 keeps a GameProfile, 1.20.5 a ResolvableProfile */
    public static ladder.carried.net.minecraft.world.item.component.ResolvableProfile getOwnerProfile(
            net.minecraft.world.level.block.entity.SkullBlockEntity skull) {
        GameProfile owner = skull.getOwnerProfile();
        return owner == null ? null : new ladder.carried.net.minecraft.world.item.component.ResolvableProfile(owner);
    }

    /** SkullBlockEntity.setOwner(profile) */
    public static void setOwner(net.minecraft.world.level.block.entity.SkullBlockEntity skull,
            ladder.carried.net.minecraft.world.item.component.ResolvableProfile profile) {
        skull.setOwner(profile == null ? null : profile.gameProfile());
    }
}
