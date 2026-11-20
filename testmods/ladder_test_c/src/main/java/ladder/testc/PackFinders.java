/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testc;

import java.nio.file.Path;
import java.util.Optional;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.AddPackFindersEvent;

/**
 * A built-in resource pack and a data pack the mod ships (resourcepacks/extra, datapacks/extra), always on: the
 * resource pack through NeoForge's addPackFinders, the data pack made by hand as mods make theirs (a location, a path
 * supplier: its NeoForge overlays applied, a selection config).
 */
final class PackFinders {
    static final String DATA = "ladder_test_c/extra_data";

    private PackFinders() {
    }

    static void register(IEventBus modBus) {
        modBus.addListener(AddPackFindersEvent.class, event -> {
            event.addPackFinders(Payloads.id("resourcepacks/extra"), PackType.CLIENT_RESOURCES, Component.literal("Ladder extra resources"),
                    PackSource.BUILT_IN, true, Pack.Position.TOP);
            if (event.getPackType() == PackType.SERVER_DATA) {
                Path path = ModList.get().getModFileById(LadderTestC.MODID).getFile().findResource("datapacks/extra");
                Pack pack = Pack.readMetaAndCreate(new PackLocationInfo(DATA, Component.literal("Ladder extra data"), PackSource.BUILT_IN, Optional.empty()),
                        new PathPackResources.PathResourcesSupplier(path), PackType.SERVER_DATA, new PackSelectionConfig(true, Pack.Position.TOP, false));
                event.addRepositorySource(packs -> packs.accept(pack));
            }
        });
    }
}
