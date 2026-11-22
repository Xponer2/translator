/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testc;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;

/** HUD layers of 1.20.5 (a layer of the mod's own above the hotbar, the game's layers' render events) and a config screen. */
final class GuiClient {
    static final ResourceLocation PROBE = ResourceLocation.fromNamespaceAndPath(LadderTestC.MODID, "probe");
    /** an extra (standalone) model of the mod's own, registered and looked up by NeoForge 21's model location */
    static final net.minecraft.client.resources.model.ModelResourceLocation EXTRA = net.minecraft.client.resources.model.ModelResourceLocation
            .standalone(ResourceLocation.fromNamespaceAndPath(LadderTestC.MODID, "extra/standalone"));
    static final Set<String> SEEN = new TreeSet<>();
    static int probeFrames;

    static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    static final ModConfigSpec.BooleanValue FANCY = BUILDER.comment("Fancy mode").define("fancy", true);
    static final ModConfigSpec.IntValue RADIUS = BUILDER.comment("A radius").defineInRange("radius", 8, 1, 64);
    static final ModConfigSpec SPEC = BUILDER.build();

    private GuiClient() {
    }

    static void register(IEventBus modBus) {
        modBus.addListener((net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) -> event.register(EXTRA));
        modBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) -> event.registerEntityRenderer(SpawnProbe.TYPE.get(),
                net.minecraft.client.renderer.entity.NoopRenderer::new));
        modBus.addListener((RegisterGuiLayersEvent event) -> event.registerAbove(VanillaGuiLayers.HOTBAR, PROBE, (graphics, delta) -> probeFrames++));
        NeoForge.EVENT_BUS.addListener((RenderGuiLayerEvent.Pre event) -> SEEN.add(event.getName().toString()));
        ModList.get().getModContainerById(LadderTestC.MODID).ifPresent(container -> container.registerConfig(ModConfig.Type.CLIENT, SPEC));
    }

    static void record(Map<String, String> results) {
        results.put("guiProbeDrawn", String.valueOf(probeFrames > 0));
        net.minecraft.client.resources.model.ModelManager models = Minecraft.getInstance().getModelManager();
        results.put("standaloneModel", (models.getModel(EXTRA) != models.getMissingModel()) + "/" + EXTRA.id() + "/" + EXTRA.variant());
        for (String name : new String[] {"minecraft:hotbar", "minecraft:crosshair", "minecraft:chat", "minecraft:food_level", "minecraft:player_health",
                "minecraft:experience_bar", LadderTestC.MODID + ":probe"}) {
            results.put("guiLayer " + name, String.valueOf(SEEN.contains(name)));
        }
        try {
            ModContainer container = ModList.get().getModContainerById(LadderTestC.MODID).orElseThrow();
            ConfigurationScreen screen = new ConfigurationScreen(container, null);
            Minecraft minecraft = Minecraft.getInstance();
            screen.init(minecraft, 400, 300);
            results.put("configScreen", "ok " + FANCY.get() + "/" + RADIUS.get());
        } catch (RuntimeException | LinkageError e) {
            // (its cause in the log: a converted run's failure is found from there)
            e.printStackTrace();
            results.put("configScreen", e.toString());
        }
    }
}
