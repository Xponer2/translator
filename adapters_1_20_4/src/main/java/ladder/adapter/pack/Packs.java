/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.adapter.pack;

import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.Nullable;

import ladder.carried.net.minecraft.server.packs.PackLocationInfo;
import ladder.carried.net.minecraft.server.packs.PackSelectionConfig;
import ladder.carried.net.minecraft.server.packs.repository.KnownPack;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.AbstractPackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.BuiltInPackSource;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforgespi.language.IModInfo;

/**
 * 1.20.5's pack identity on 1.20.4. A pack is located by a PackLocationInfo there (id, title, source, the known pack
 * it is), by its id here (the title and source are the Pack's own): the bridges pass the id down and the location up
 * ({@link #id}, {@link #location(String)}), a location seen once is remembered by its id, so a mod gets back the one
 * it gave. A selection config is the Pack's required / position / fixed flags.
 */
public final class Packs {
    private static final Map<String, PackLocationInfo> KNOWN = new ConcurrentHashMap<>();
    /** the location each pack was made with (by the pack itself: two packs may share an id across repositories) */
    private static final Map<Pack, PackLocationInfo> MADE = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private Packs() {
    }

    // ---------------------------------------------------------------- the bridges' conversion

    /** a location -> its id (remembered) */
    @Nullable
    public static String id(@Nullable Object location) {
        if (location == null) {
            return null;
        }
        PackLocationInfo info = (PackLocationInfo) location;
        KNOWN.put(info.id(), info);
        return info.id();
    }

    /** an id -> the location given for it, else one of the id alone (its title the id, a built-in pack) */
    @Nullable
    public static Object location(@Nullable String id) {
        if (id == null) {
            return null;
        }
        PackLocationInfo known = KNOWN.get(id);
        return known != null ? known : new PackLocationInfo(id, Component.literal(id), PackSource.BUILT_IN, Optional.empty());
    }

    private static PackLocationInfo info(String id, Component title, PackSource source) {
        PackLocationInfo known = KNOWN.get(id);
        return known != null ? known : new PackLocationInfo(id, title, source, Optional.empty());
    }

    // ---------------------------------------------------------------- Pack

    /** {@code Pack.readMetaAndCreate(location, resources, type, selection)} */
    @Nullable
    public static Pack readMetaAndCreate(PackLocationInfo location, Pack.ResourcesSupplier resources, PackType type, PackSelectionConfig selection) {
        String id = id(location);
        Pack.Info info = Pack.readPackInfo(id, resources, SharedConstants.getCurrentVersion().getPackVersion(type));
        return info == null ? null : create(location, resources, info, selection);
    }

    /** {@code new Pack(location, resources, metadata, selection)} */
    public static Pack create(PackLocationInfo location, Pack.ResourcesSupplier resources, Pack.Info metadata, PackSelectionConfig selection) {
        Pack pack = Pack.create(id(location), location.title(), selection.required(), resources, metadata, selection.defaultPosition(),
                selection.fixedPosition(), location.source());
        MADE.put(pack, location);
        return pack;
    }

    /** {@code pack.location()} */
    public static PackLocationInfo location(Pack pack) {
        PackLocationInfo made = MADE.get(pack);
        // a pack 1.20.4 made: its own id, title and source (no known pack: 1.20.4 has none)
        return made != null ? made : new PackLocationInfo(pack.getId(), pack.getTitle(), pack.getPackSource(), Optional.empty());
    }

    /** {@code pack.selectionConfig()} */
    public static PackSelectionConfig selectionConfig(Pack pack) {
        return new PackSelectionConfig(pack.isRequired(), pack.getDefaultPosition(), pack.isFixedPosition());
    }

    // ---------------------------------------------------------------- PackResources

    /** {@code resources.location()} */
    public static PackLocationInfo location(PackResources resources) {
        // (1.20.4's pack resources know their id alone: the location last given for it, else one of the id)
        return info(resources.packId(), Component.literal(resources.packId()), resources.isBuiltin() ? PackSource.BUILT_IN : PackSource.DEFAULT);
    }

    /** {@code resources.knownPackInfo()} */
    public static Optional<KnownPack> knownPackInfo(PackResources resources) {
        return location(resources).knownPackInfo();
    }

    // the constructors that take a location (K rows: the id and whether the pack is built in)

    public static String id(PackLocationInfo location) {
        return id((Object) location);
    }

    public static String id(PackLocationInfo location, Path path) {
        return id((Object) location);
    }

    public static boolean builtin(PackLocationInfo location) {
        return location.source() == PackSource.BUILT_IN;
    }

    public static boolean builtin(PackLocationInfo location, Path path) {
        return builtin(location);
    }

    /** 1.20.5's suppliers take no built-in flag (the location has the source): a pack opened by them is not built in */
    public static boolean notBuiltin(Path path) {
        return false;
    }

    public static boolean notBuiltin(File file) {
        return false;
    }

    /** {@code BuiltInPackSource.fromName(location -> resources)}: 1.20.4 passes the id */
    @SuppressWarnings("unchecked")
    public static Pack.ResourcesSupplier fromName(Function<?, PackResources> open) {
        Function<Object, PackResources> function = (Function<Object, PackResources>) open;
        return BuiltInPackSource.fromName(id -> function.apply(location(id)));
    }

    // ---------------------------------------------------------------- AddPackFindersEvent

    /** NeoForge 20.5's {@code event.addPackFinders(location, type, title, source, alwaysActive, position)} */
    public static void addPackFinders(AddPackFindersEvent event, ResourceLocation packLocation, PackType packType, Component title, PackSource source,
            boolean alwaysActive, Pack.Position position) {
        if (event.getPackType() != packType) {
            return;
        }
        IModInfo mod = ModList.get().getModContainerById(packLocation.getNamespace())
                .orElseThrow(() -> new IllegalArgumentException("Mod not found: " + packLocation.getNamespace())).getModInfo();
        Path path = mod.getOwningFile().getFile().findResource(packLocation.getPath());
        String id = "mod/" + packLocation;
        PackLocationInfo location = new PackLocationInfo(id, title, source, Optional.of(new KnownPack("neoforge", id, mod.getVersion().toString())));
        Pack pack = readMetaAndCreate(location, BuiltInPackSource.fromName(name -> new PathPackResources(name, path, true)), packType,
                new PackSelectionConfig(alwaysActive, position, false));
        event.addRepositorySource(packs -> packs.accept(pack));
    }
}
