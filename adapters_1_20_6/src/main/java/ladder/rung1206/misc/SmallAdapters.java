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

    /** 1.21's {@code settings.setLiquidSettings(liquids)}: 1.20.6 keeps liquids (waterlogs) or not */
    public static net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings setLiquidSettings(
            net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings settings,
            ladder.carried_1_20_6.net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings liquids) {
        return settings.setKeepLiquids(liquids != ladder.carried_1_20_6.net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings.IGNORE_WATERLOGGING);
    }

    /** 1.21's {@code JigsawStructure.DEFAULT_LIQUID_SETTINGS} */
    public static ladder.carried_1_20_6.net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings defaultLiquidSettings() {
        return liquidSettings();
    }

    /** 1.21's {@code JigsawStructure.DEFAULT_DIMENSION_PADDING} (none) */
    public static ladder.carried_1_20_6.net.minecraft.world.level.levelgen.structure.pools.DimensionPadding defaultDimensionPadding() {
        return ladder.carried_1_20_6.net.minecraft.world.level.levelgen.structure.pools.DimensionPadding.ZERO;
    }

    /** a colour int (ARGB) as 1.20.6's red, green, blue floats, and back (a value bridge's conversion) */
    public static float[] rgb(int argb) {
        return new float[] {(argb >> 16 & 0xFF) / 255.0F, (argb >> 8 & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F};
    }

    public static int argb(float[] rgb) {
        return rgb == null ? -1 : 0xFF000000 | Math.round(rgb[0] * 255.0F) << 16 | Math.round(rgb[1] * 255.0F) << 8 | Math.round(rgb[2] * 255.0F);
    }

    /** a beam section's colour floats of 1.21's ARGB (a K row's: new BeaconBeamSection(color)) */
    public static float[] beamRgb(int argb) {
        return rgb(argb);
    }

    /**
     * 1.21's plant state as 20.6's IPlantable (canSustainPlant): the plant's block when it is one (a bush, a crop), else
     * the state itself, of the plain type
     */
    public static net.neoforged.neoforge.common.IPlantable plantable(net.minecraft.world.level.block.state.BlockState plant) {
        if (plant.getBlock() instanceof net.neoforged.neoforge.common.IPlantable plantable) {
            return plantable;
        }
        return (level, pos) -> plant;
    }

    /** 20.6's IPlantable as 1.21's plant state: the plant on the soil's side */
    public static net.minecraft.world.level.block.state.BlockState plant(net.neoforged.neoforge.common.IPlantable plantable,
            net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos soil, net.minecraft.core.Direction facing) {
        return plantable.getPlant(level, soil.relative(facing));
    }

    private static final net.minecraft.tags.TagKey<net.minecraft.world.item.Item> MUSIC_DISCS = net.minecraft.tags.ItemTags.create(
            new net.minecraft.resources.ResourceLocation("c", "music_discs"));

    /** NeoForge 21's {@code Tags.Items.MUSIC_DISCS}: c:music_discs (the adapters' data puts the game's discs in it) */
    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> musicDiscs() {
        return MUSIC_DISCS;
    }

    /** 1.21's {@code Checkbox.Builder.maxWidth(width)}: 1.20.6's checkbox label isn't wrapped, its width is the text's */
    public static net.minecraft.client.gui.components.Checkbox.Builder maxWidth(net.minecraft.client.gui.components.Checkbox.Builder builder,
            int width) {
        return builder;
    }

    /** NeoForge 21's colour Integer of 20.6's red, green, blue floats, and back (null: no colour, both ways) */
    public static Integer argbOrNull(float[] rgb) {
        return rgb == null ? null : argb(rgb);
    }

    public static float[] rgbOrNull(Integer argb) {
        return argb == null ? null : rgb(argb);
    }

    /**
     * 1.21's {@code SinglePoolElement.overrideLiquidSettingsCodec()}: the element's optional liquid settings field (1.20.6's
     * elements have none: read and dropped, written as absent)
     */
    public static <E> com.mojang.serialization.codecs.RecordCodecBuilder<E, java.util.Optional<
            ladder.carried_1_20_6.net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings>> overrideLiquidSettingsCodec() {
        return ladder.carried_1_20_6.net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings.CODEC
                .optionalFieldOf("override_liquid_settings").forGetter(element -> java.util.Optional.empty());
    }

    /** 1.21's {@code report.getSaveFile()} as a path (1.20.6 keeps a file; null while unsaved) */
    public static java.nio.file.Path getSaveFile(net.minecraft.CrashReport report) {
        java.io.File file = report.getSaveFile();
        return file == null ? null : file.toPath();
    }

    /** the path of a file (a mixin handler's argument where 1.21 passes a path) */
    public static java.nio.file.Path path(java.io.File file) {
        return file == null ? null : file.toPath();
    }

    /** the kind of report 1.20.6 saves: a crash */
    public static ladder.carried_1_20_6.net.minecraft.ReportType crashReport() {
        return ladder.carried_1_20_6.net.minecraft.ReportType.CRASH;
    }

    /** the file of a path (a call's argument where 1.20.6 takes a file) */
    public static java.io.File file(java.nio.file.Path path) {
        return path == null ? null : path.toFile();
    }

    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> AIR = net.minecraft.tags.BlockTags.create(
            new net.minecraft.resources.ResourceLocation("minecraft", "air"));

    /** 1.21's {@code BlockTags.AIR} (the adapters' data gives 1.20.6 the tag: air, void air, cave air) */
    public static net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> airTag() {
        return AIR;
    }

    /** 1.21's {@code attribute.setSentiment(sentiment)}: the colour of its tooltip's bonus (1.20.6 colours them itself) */
    public static net.minecraft.world.entity.ai.attributes.Attribute setSentiment(net.minecraft.world.entity.ai.attributes.Attribute attribute,
            ladder.carried_1_20_6.net.minecraft.world.entity.ai.attributes.Attribute$Sentiment sentiment) {
        return attribute;
    }

    /** 1.21's {@code MultiLineLabel.create(font, width, lines...)}: the lines, each split to the width */
    public static net.minecraft.client.gui.components.MultiLineLabel multiLineLabel(net.minecraft.client.gui.Font font, int width,
            net.minecraft.network.chat.Component... lines) {
        return net.minecraft.client.gui.components.MultiLineLabel.create(font, net.minecraft.network.chat.CommonComponents.joinLines(lines), width);
    }

    /** 1.21's {@code MultiLineLabel.create(font, width, maxRows, lines...)} */
    public static net.minecraft.client.gui.components.MultiLineLabel multiLineLabel(net.minecraft.client.gui.Font font, int width, int maxRows,
            net.minecraft.network.chat.Component... lines) {
        return net.minecraft.client.gui.components.MultiLineLabel.create(font, net.minecraft.network.chat.CommonComponents.joinLines(lines), width,
                maxRows);
    }

    // 1.21's small fireball made with its movement as a vector (1.20.6: its three parts); the K rows' values
    public static double movementX(net.minecraft.world.level.Level level, double x, double y, double z, net.minecraft.world.phys.Vec3 movement) {
        return movement.x;
    }

    public static double movementY(net.minecraft.world.level.Level level, double x, double y, double z, net.minecraft.world.phys.Vec3 movement) {
        return movement.y;
    }

    public static double movementZ(net.minecraft.world.level.Level level, double x, double y, double z, net.minecraft.world.phys.Vec3 movement) {
        return movement.z;
    }

    public static double movementX(net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity owner,
            net.minecraft.world.phys.Vec3 movement) {
        return movement.x;
    }

    public static double movementY(net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity owner,
            net.minecraft.world.phys.Vec3 movement) {
        return movement.y;
    }

    public static double movementZ(net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity owner,
            net.minecraft.world.phys.Vec3 movement) {
        return movement.z;
    }

    /** the client's tesselator (1.21 passes it where 1.20.6 passes its builder: the cloud mesh) */
    public static com.mojang.blaze3d.vertex.Tesselator tesselator() {
        return com.mojang.blaze3d.vertex.Tesselator.getInstance();
    }

    /** 1.21's {@code Util.parseAndValidateUntrustedUri(text)}: a link a player may open, of the web's protocols only */
    public static java.net.URI parseAndValidateUntrustedUri(String text) throws java.net.URISyntaxException {
        java.net.URI uri = new java.net.URI(text);
        String scheme = uri.getScheme();
        if (scheme == null) {
            throw new java.net.URISyntaxException(text, "Missing protocol in URI: " + text);
        }
        String protocol = scheme.toLowerCase(java.util.Locale.ROOT);
        if (!protocol.equals("http") && !protocol.equals("https")) {
            throw new java.net.URISyntaxException(text, "Unsupported protocol in URI: " + text);
        }
        return uri;
    }

    /** 1.21's sound of a location (1.20.6 keeps its location's text; the K row's value) */
    public static String soundLocation(net.minecraft.resources.ResourceLocation location, net.minecraft.util.valueproviders.SampledFloat volume,
            net.minecraft.util.valueproviders.SampledFloat pitch, int weight, net.minecraft.client.resources.sounds.Sound.Type type, boolean stream,
            boolean preload, int attenuation) {
        return location.toString();
    }

    /** 1.21's use-item packet with the player's rotation (1.20.6's server takes the player's own) */
    public static net.minecraft.network.protocol.game.ServerboundUseItemPacket useItemPacket(net.minecraft.world.InteractionHand hand, int sequence,
            float yRot, float xRot) {
        return new net.minecraft.network.protocol.game.ServerboundUseItemPacket(hand, sequence);
    }

    /** 1.21's disconnection details of 1.20.6's reason (a handler's argument) */
    public static ladder.carried_1_20_6.net.minecraft.network.DisconnectionDetails disconnection(net.minecraft.network.chat.Component reason) {
        return new ladder.carried_1_20_6.net.minecraft.network.DisconnectionDetails(reason);
    }

    /** 1.21's reason of disconnection details (a call where 1.20.6 takes the reason) */
    public static net.minecraft.network.chat.Component reason(ladder.carried_1_20_6.net.minecraft.network.DisconnectionDetails details) {
        return details.reason();
    }

    // 1.21's camera directions (its constants; a mod's read of one)
    public static org.joml.Vector3f cameraForwards() {
        return new org.joml.Vector3f(0.0F, 0.0F, -1.0F);
    }

    public static org.joml.Vector3f cameraUp() {
        return new org.joml.Vector3f(0.0F, 1.0F, 0.0F);
    }

    public static org.joml.Vector3f cameraLeft() {
        return new org.joml.Vector3f(-1.0F, 0.0F, 0.0F);
    }

    /** NeoForge 21's block element face with its parent holder (1.20.6's element sets itself as its faces' parent) */
    public static net.minecraft.client.renderer.block.model.BlockElementFace blockElementFace(net.minecraft.core.Direction cull, int tint, String texture,
            net.minecraft.client.renderer.block.model.BlockFaceUV uv, net.neoforged.neoforge.client.model.ExtraFaceData faceData,
            org.apache.commons.lang3.mutable.MutableObject<?> parent) {
        return new net.minecraft.client.renderer.block.model.BlockElementFace(cull, tint, texture, uv, faceData);
    }

    // NeoForge 21's food tags of one kind (20.6 names them in the plural: c:foods/berries)
    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> foodsFruit() {
        return net.neoforged.neoforge.common.Tags.Items.FOODS_FRUITS;
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> foodsVegetable() {
        return net.neoforged.neoforge.common.Tags.Items.FOODS_VEGETABLES;
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> foodsBerry() {
        return net.neoforged.neoforge.common.Tags.Items.FOODS_BERRIES;
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> foodsBread() {
        return net.neoforged.neoforge.common.Tags.Items.FOODS_BREADS;
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> foodsCookie() {
        return net.neoforged.neoforge.common.Tags.Items.FOODS_COOKIES;
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> foodsRawMeat() {
        return net.neoforged.neoforge.common.Tags.Items.FOODS_RAW_MEATS;
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> foodsCookedMeat() {
        return net.neoforged.neoforge.common.Tags.Items.FOODS_COOKED_MEATS;
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> foodsRawFish() {
        return net.neoforged.neoforge.common.Tags.Items.FOODS_RAW_FISHES;
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> foodsCookedFish() {
        return net.neoforged.neoforge.common.Tags.Items.FOODS_COOKED_FISHES;
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> foodsSoup() {
        return net.neoforged.neoforge.common.Tags.Items.FOODS_SOUPS;
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> foodsCandy() {
        return net.neoforged.neoforge.common.Tags.Items.FOODS_CANDIES;
    }

    /** DataFixerUpper 8's {@code result.hasResultOrPartial()} */
    public static boolean hasResultOrPartial(com.mojang.serialization.DataResult<?> result) {
        return result.resultOrPartial().isPresent();
    }

    /** 1.21's {@code DamageType.STREAM_CODEC} (a holder of the damage type registry) */
    public static net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf,
            net.minecraft.core.Holder<net.minecraft.world.damagesource.DamageType>> damageTypeStreamCodec() {
        return net.minecraft.network.codec.ByteBufCodecs.holderRegistry(net.minecraft.core.registries.Registries.DAMAGE_TYPE);
    }

    /** 1.21's {@code MobEffect.STREAM_CODEC} */
    public static net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf,
            net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect>> mobEffectStreamCodec() {
        return net.minecraft.network.codec.ByteBufCodecs.holderRegistry(net.minecraft.core.registries.Registries.MOB_EFFECT);
    }

    /** 1.21's {@code DamageTypeTags.BURN_FROM_STEPPING} (the adapters' data gives 1.20.6 the tag) */
    public static net.minecraft.tags.TagKey<net.minecraft.world.damagesource.DamageType> burnFromStepping() {
        return net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                new net.minecraft.resources.ResourceLocation("minecraft", "burn_from_stepping"));
    }

    /** 1.21's {@code list.clampScrollAmount()}: 1.20.6 clamps the amount it is set to */
    public static void clampScrollAmount(net.minecraft.client.gui.components.AbstractSelectionList<?> list) {
        list.setScrollAmount(list.getScrollAmount());
    }

    /** 1.21's {@code source.getWeaponItem()}: the attacker's main hand (1.20.6's sources keep no weapon) */
    public static net.minecraft.world.item.ItemStack getWeaponItem(net.minecraft.world.damagesource.DamageSource source) {
        return source.getEntity() instanceof net.minecraft.world.entity.LivingEntity living ? living.getMainHandItem()
                : net.minecraft.world.item.ItemStack.EMPTY;
    }

    /** 1.21's {@code entity.getWeaponItem()} of any entity: a living one's main hand, an arrow's none kept, else null (1.21's) */
    public static net.minecraft.world.item.ItemStack getWeaponItem(net.minecraft.world.entity.Entity entity) {
        if (entity instanceof net.minecraft.world.entity.projectile.AbstractArrow) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        return entity instanceof net.minecraft.world.entity.LivingEntity living ? living.getMainHandItem() : null;
    }

    /** 1.21's {@code foodData.eat(food)}: its nutrition and saturation (1.20.6 takes the saturation as a modifier) */
    public static void eat(net.minecraft.world.food.FoodData data, net.minecraft.world.food.FoodProperties food) {
        int nutrition = food.nutrition();
        data.eat(nutrition, nutrition == 0 ? 0.0F : food.saturation() / (2.0F * nutrition));
    }

    /** 1.21's {@code new FoodProperties(..., usingConvertsTo, effects)}: 1.20.6's has no remainder (the item's own code gives it) */
    public static net.minecraft.world.food.FoodProperties foodProperties(int nutrition, float saturation, boolean canAlwaysEat, float eatSeconds,
            java.util.Optional<?> usingConvertsTo, java.util.List<net.minecraft.world.food.FoodProperties.PossibleEffect> effects) {
        return new net.minecraft.world.food.FoodProperties(nutrition, saturation, canAlwaysEat, eatSeconds, effects);
    }

    // NeoForge 21's tool tags (20.6 names some in the plural; the new ones come with the adapters' data)
    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tagToolsBow() {
        return net.neoforged.neoforge.common.Tags.Items.TOOLS_BOWS;
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tagToolsBrush() {
        return net.neoforged.neoforge.common.Tags.Items.TOOLS_BRUSHES;
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tagToolsCrossbow() {
        return net.neoforged.neoforge.common.Tags.Items.TOOLS_CROSSBOWS;
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tagToolsFishingRod() {
        return net.neoforged.neoforge.common.Tags.Items.TOOLS_FISHING_RODS;
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tagToolsShield() {
        return net.neoforged.neoforge.common.Tags.Items.TOOLS_SHIELDS;
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tagToolsSpear() {
        return net.neoforged.neoforge.common.Tags.Items.TOOLS_SPEARS;
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tagToolsMace() {
        return net.minecraft.tags.ItemTags.create(new net.minecraft.resources.ResourceLocation("c", "tools/mace"));
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tagMeleeWeaponTools() {
        return net.minecraft.tags.ItemTags.create(new net.minecraft.resources.ResourceLocation("c", "tools/melee_weapon"));
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tagMiningToolTools() {
        return net.minecraft.tags.ItemTags.create(new net.minecraft.resources.ResourceLocation("c", "tools/mining_tool"));
    }

    public static net.minecraft.tags.TagKey<net.minecraft.world.item.Item> tagRangedWeaponTools() {
        return net.minecraft.tags.ItemTags.create(new net.minecraft.resources.ResourceLocation("c", "tools/ranged_weapon"));
    }

    /** NeoForge 21's {@code player.isFakePlayer()} (20.6: a FakePlayer's class) */
    public static boolean isFakePlayer(net.neoforged.neoforge.common.extensions.IPlayerExtension player) {
        return player instanceof net.neoforged.neoforge.common.util.FakePlayer;
    }

    /** 1.21's {@code VertexFormatElement.UV} (the first texture coordinates) */
    public static com.mojang.blaze3d.vertex.VertexFormatElement vertexUv() {
        return com.mojang.blaze3d.vertex.DefaultVertexFormat.ELEMENT_UV;
    }
}
