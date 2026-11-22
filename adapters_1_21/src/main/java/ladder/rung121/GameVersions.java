/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung121;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import net.minecraft.SharedConstants;
import net.minecraft.WorldVersion;
import net.neoforged.fml.loading.VersionInfo;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * The game's versions as a converted mod asks them (X rows): Minecraft 1.21.1 and NeoForge 21.1, the versions it was made
 * for and whose API the adapters give. A mod checks them against its own data (TaCZ's gun packs name the Minecraft
 * versions they are for: on 1.21 they were all left out).
 */
public final class GameVersions {
    public static final String MINECRAFT = "1.21.1";
    public static final String NEOFORGE = "21.1.247";

    private GameVersions() {
    }

    /** {@code IModInfo.getVersion()}: Minecraft's and NeoForge's own entries give the versions above */
    public static ArtifactVersion modVersion(IModInfo info) {
        return switch (info.getModId()) {
            case "minecraft" -> new DefaultArtifactVersion(MINECRAFT);
            case "neoforge" -> new DefaultArtifactVersion(NEOFORGE);
            default -> info.getVersion();
        };
    }

    /** {@code WorldVersion.getName()}: the running game's own version gives Minecraft's version above */
    public static String versionName(WorldVersion version) {
        return version == SharedConstants.getCurrentVersion() ? MINECRAFT : version.getName();
    }

    /** {@code VersionInfo.mcVersion()} */
    public static String mcVersion(VersionInfo info) {
        return MINECRAFT;
    }

    /** {@code VersionInfo.neoForgeVersion()} */
    public static String neoForgeVersion(VersionInfo info) {
        return NEOFORGE;
    }
}
