/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.rung1206.misc;

import java.nio.file.Path;

import net.minecraft.CrashReport;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.decoration.PaintingVariant;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.maps.MapId;

/**
 * Small members of 1.21 on 1.20.6 (X, K and D rows): the server directory as a path, colours as ARGB ints (a sheep's
 * wool, a beacon beam's section), an enchantment's full name of its holder, paintings sized in blocks, the creative
 * tab's background texture, a framed map's id of a stack, the enchantment registry's lookup, a block of its state.
 */
public final class SmallAdapters {
    private SmallAdapters() {
    }

    /** 1.21's {@code server.getServerDirectory()}: a Path (1.20.6: a File) */
    public static Path getServerDirectory(MinecraftServer server) {
        return server.getServerDirectory().toPath();
    }

    /** 1.21's {@code Sheep.getColor(dye)}: the wool's ARGB colour (white's own, else the dye's at three quarters) */
    public static int sheepColor(DyeColor dye) {
        if (dye == DyeColor.WHITE) {
            return -1644826;
        }
        float[] c = dye.getTextureDiffuseColors();
        return 0xFF000000 | Mth.floor(Math.round(c[0] * 255.0F) * 0.75F) << 16 | Mth.floor(Math.round(c[1] * 255.0F) * 0.75F) << 8
                | Mth.floor(Math.round(c[2] * 255.0F) * 0.75F);
    }

    /** 1.21's {@code section.getColor()}: the ARGB colour of 1.20.6's float colour */
    public static int beamColor(BeaconBlockEntity.BeaconBeamSection section) {
        float[] c = section.getColor();
        return 0xFF000000 | Mth.floor(c[0] * 255.0F) << 16 | Mth.floor(c[1] * 255.0F) << 8 | Mth.floor(c[2] * 255.0F);
    }

    /** 1.21's static {@code Enchantment.getFullname(holder, level)} */
    public static Component getFullname(Holder<Enchantment> enchantment, int level) {
        return enchantment.value().getFullname(level);
    }

    /** 1.21's painting of a width and height in blocks (K row): 1.20.6 counts pixels */
    public static int paintingWidth(int width, int height, ResourceLocation assetId) {
        return width * 16;
    }

    public static int paintingHeight(int width, int height, ResourceLocation assetId) {
        return height * 16;
    }

    /** 1.21's {@code variant.width()}, in blocks */
    public static int paintingWidth(PaintingVariant variant) {
        return variant.getWidth() / 16;
    }

    public static int paintingHeight(PaintingVariant variant) {
        return variant.getHeight() / 16;
    }

    /** 1.21's {@code variant.assetId()}: 1.20.6 draws a painting by its registry id */
    public static ResourceLocation paintingAsset(PaintingVariant variant) {
        return BuiltInRegistries.PAINTING_VARIANT.getKey(variant);
    }

    /** 1.21's {@code tab.getBackgroundTexture()} (NeoForge 20.6 has the location under another name) */
    public static ResourceLocation getBackgroundTexture(CreativeModeTab tab) {
        return tab.getBackgroundLocation();
    }

    /** 1.21's {@code builder.backgroundTexture(texture)} */
    public static CreativeModeTab.Builder backgroundTexture(CreativeModeTab.Builder builder, ResourceLocation texture) {
        return builder.withBackgroundLocation(texture);
    }

    /** 1.21's {@code frame.getFramedMapId(stack)}: the map id of that stack */
    public static MapId getFramedMapId(net.minecraft.world.entity.decoration.ItemFrame frame, ItemStack stack) {
        return stack.get(DataComponents.MAP_ID);
    }

    /** the enchantments' registry lookup 1.21's getAllEnchantments takes (1.20.6: the built-in registry) */
    public static HolderLookup.RegistryLookup<Enchantment> enchantmentLookup() {
        return BuiltInRegistries.ENCHANTMENT.asLookup();
    }

    /** a block state's block (1.21's CropBlock.getGrowthSpeed takes the state, 1.20.6's the block) */
    public static Block block(BlockState state) {
        return state.getBlock();
    }

    /** 1.21's {@code report.getFriendlyReport(type)}: 1.20.6's report (its type's header is 1.21's own) */
    public static String getFriendlyReport(CrashReport report, ladder.carried_1_20_6.net.minecraft.ReportType type) {
        return report.getFriendlyReport();
    }

    /** 1.21's {@code report.saveToFile(path, type)} */
    public static boolean saveToFile(CrashReport report, Path path, ladder.carried_1_20_6.net.minecraft.ReportType type) {
        return report.saveToFile(path.toFile());
    }

    /** 1.21's {@code pos.getChessboardDistance(x, z)} */
    public static int getChessboardDistance(ChunkPos pos, int x, int z) {
        return Math.max(Math.abs(pos.x - x), Math.abs(pos.z - z));
    }

    /** 1.21's {@code result.components()}: a patch over the item's prototype (1.20.6 parses a map of the set ones) */
    public static DataComponentPatch components(ItemParser.ItemResult result) {
        DataComponentPatch.Builder patch = DataComponentPatch.builder();
        for (TypedDataComponent<?> component : result.components()) {
            set(patch, component);
        }
        return patch.build();
    }

    private static <T> void set(DataComponentPatch.Builder patch, TypedDataComponent<T> component) {
        patch.set(component.type(), component.value());
    }

    /** 1.21's {@code new ItemInput(item, patch)} (1.20.6 takes a map of the set components) */
    public static net.minecraft.commands.arguments.item.ItemInput itemInput(Holder<net.minecraft.world.item.Item> item, DataComponentPatch patch) {
        DataComponentMap.Builder map = DataComponentMap.builder();
        for (var entry : patch.entrySet()) {
            entry.getValue().ifPresent(value -> put(map, entry.getKey(), value));
        }
        return new net.minecraft.commands.arguments.item.ItemInput(item, map.build());
    }

    @SuppressWarnings("unchecked")
    private static <T> void put(DataComponentMap.Builder map, net.minecraft.core.component.DataComponentType<T> type, Object value) {
        map.set(type, (T) value);
    }

    /** 1.21's {@code damageSources.campfire()}: the adapters' data gives the campfire damage type below */
    public static net.minecraft.world.damagesource.DamageSource campfire(net.minecraft.world.damagesource.DamageSources sources) {
        return sources.source(CAMPFIRE);
    }

    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> CAMPFIRE = net.minecraft.resources.ResourceKey
            .create(net.minecraft.core.registries.Registries.DAMAGE_TYPE, new ResourceLocation("minecraft", "campfire"));

    /** 1.21's {@code Unit.CODEC} */
    public static com.mojang.serialization.Codec<net.minecraft.util.Unit> unitCodec() {
        return com.mojang.serialization.Codec.unit(net.minecraft.util.Unit.INSTANCE);
    }

    /** NeoForge 21's {@code Tags.Items.TOOLS_SHEAR} (20.6's c:tools/shears) */
    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> toolsShear() {
        return net.neoforged.neoforge.common.Tags.Items.TOOLS_SHEARS;
    }

    /** the liquid settings 1.21 passes to jigsaw placement (1.20.6 always applies waterlogging) */
    public static ladder.carried_1_20_6.net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings liquidSettings() {
        return ladder.carried_1_20_6.net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings.APPLY_WATERLOGGING;
    }

    /** a beam's ARGB colour (1.21) as 1.20.6's red, green, blue floats */
    public static float[] colorArray(int argb) {
        return new float[] {(argb >> 16 & 0xFF) / 255.0F, (argb >> 8 & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F};
    }

    /** 1.20.6's beam colour floats as 1.21's ARGB int (opaque) */
    public static int colorInt(float[] rgb) {
        return 0xFF000000 | Mth.floor(rgb[0] * 255.0F) << 16 | Mth.floor(rgb[1] * 255.0F) << 8 | Mth.floor(rgb[2] * 255.0F);
    }

    /** the slot group of a slot (1.21's forEachModifier of a group where 1.20.6's tooltips ask it per slot) */
    public static net.minecraft.world.entity.EquipmentSlotGroup slotGroup(net.minecraft.world.entity.EquipmentSlot slot) {
        return net.minecraft.world.entity.EquipmentSlotGroup.bySlot(slot);
    }

    /** 1.21's {@code dye.getTextureDiffuseColor()}: the ARGB of 1.20.6's float colour */
    public static int textureDiffuseColor(DyeColor dye) {
        float[] c = dye.getTextureDiffuseColors();
        return 0xFF000000 | Math.round(c[0] * 255.0F) << 16 | Math.round(c[1] * 255.0F) << 8 | Math.round(c[2] * 255.0F);
    }
}
