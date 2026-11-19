/*
 * Translator: mods of newer Minecraft versions on older ones, versions mixed
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testb;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.ItemLore;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Made for NeoForge 1.21.1: item data components. Its own types (a number, a record), an item of its own with a
 * default value, the game's own components; values set, updated, removed, copied, saved and read back; a stack handed
 * to another mod (test mod A reads it from a chest at {@link #CHEST}, by the type's id), and found again after a
 * restart. The results go to {@code -Dladder.test.b.out}.
 */
@Mod("ladder_test_b")
public final class LadderTestB {
    public static final BlockPos CHEST = new BlockPos(8, 140, 8);
    static final DeferredRegister.DataComponents COMPONENTS = DeferredRegister.createDataComponents("ladder_test_b");
    static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> CHARGE =
            COMPONENTS.registerComponentType("charge", builder -> builder.persistent(Codec.INT));
    static final DeferredHolder<DataComponentType<?>, DataComponentType<Owner>> OWNER =
            COMPONENTS.registerComponentType("owner", builder -> builder.persistent(Owner.CODEC));
    static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "ladder_test_b");
    static final DeferredHolder<Item, Item> CHARGED = ITEMS.register("charged_stick", () -> new Item(new Item.Properties().component(CHARGE.get(), 3)));
    static final DeferredHolder<Item, Item> NAMED = ITEMS.register("named_stick",
            () -> new Item(new Item.Properties().component(DataComponents.CUSTOM_NAME, Component.literal("Named by default"))));

    /** a sword of the mod's own: its damage and speed are the properties' attribute modifiers */
    static final DeferredHolder<Item, Item> BLADE = ITEMS.register("test_blade",
            () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 4, -2.0F))));
    /** a food of the mod's own, with an effect */
    static final DeferredHolder<Item, Item> SNACK = ITEMS.register("test_snack", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .nutrition(3).saturationModifier(0.5F).effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0), 0.5F).build())));

    record Owner(String name, int level) {
        static final Codec<Owner> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Owner::name), Codec.INT.fieldOf("level").forGetter(Owner::level)).apply(i, Owner::new));
    }

    public LadderTestB(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.register(this);
    }

    static int chargeOf(net.minecraft.core.component.DataComponentHolder holder) {
        return holder.getOrDefault(CHARGE.get(), -1) + (holder.getComponents().has(CHARGE.get()) ? 100 : 0);
    }

    static String effects(Iterable<MobEffectInstance> effects) {
        List<String> all = new ArrayList<>();
        for (MobEffectInstance effect : effects) {
            all.add(effect.getDescriptionId() + "/" + effect.getDuration() + "/" + effect.getAmplifier());
        }
        return all.toString();
    }

    static String attributes(ItemAttributeModifiers modifiers) {
        List<String> all = new ArrayList<>();
        for (ItemAttributeModifiers.Entry entry : modifiers.modifiers()) {
            all.add(entry.attribute().unwrapKey().map(k -> k.location().toString()).orElse("?") + "@" + entry.slot().getSerializedName() + "="
                    + entry.modifier().amount() + "/" + entry.modifier().operation().id());
        }
        java.util.Collections.sort(all);
        return all + (modifiers.showInTooltip() ? "" : " hidden");
    }

    static ItemStack savedBack(ServerLevel level, ItemStack stack) {
        return ItemStack.parseOptional(level.registryAccess(), (CompoundTag) stack.save(level.registryAccess()));
    }

    /** the game's other components: potions, food, lore, the model number, attribute modifiers, rarity */
    static void moreComponents(Map<String, String> results, ServerLevel level) {
        ItemStack potion = PotionContents.createItemStack(Items.POTION, Potions.HEALING);
        PotionContents contents = potion.get(DataComponents.POTION_CONTENTS);
        results.put("potion", contents.potion().flatMap(Holder::unwrapKey).map(k -> k.location().toString()).orElse("none"));
        results.put("potionIs", contents.is(Potions.HEALING) + "/" + contents.is(Potions.SWIFTNESS));
        results.put("potionEffects", effects(contents.getAllEffects()));
        results.put("potionColor", Integer.toHexString(contents.getColor()));
        results.put("potionDefault", String.valueOf(new ItemStack(Items.POTION).get(DataComponents.POTION_CONTENTS).potion().isPresent()));
        results.put("stickNoPotion", String.valueOf(new ItemStack(Items.STICK).get(DataComponents.POTION_CONTENTS)));
        potion.set(DataComponents.POTION_CONTENTS, contents.withEffectAdded(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 200, 1)));
        results.put("potionSaved", effects(savedBack(level, potion).get(DataComponents.POTION_CONTENTS).getAllEffects()));
        ItemStack colored = new ItemStack(Items.SPLASH_POTION);
        colored.set(DataComponents.POTION_CONTENTS, new PotionContents(Optional.empty(), Optional.of(0x112233), List.of()));
        results.put("potionCustomColor", Integer.toHexString(savedBack(level, colored).get(DataComponents.POTION_CONTENTS).getColor()));

        FoodProperties apple = new ItemStack(Items.GOLDEN_APPLE).get(DataComponents.FOOD);
        results.put("food", apple.nutrition() + "/" + apple.saturation() + "/" + apple.canAlwaysEat() + "/" + apple.eatSeconds());
        List<String> appleEffects = new ArrayList<>();
        for (FoodProperties.PossibleEffect effect : apple.effects()) {
            appleEffects.add(effect.effect().getDescriptionId() + "/" + effect.effect().getAmplifier() + "@" + effect.probability());
        }
        results.put("foodEffects", appleEffects.toString());
        results.put("stickNoFood", String.valueOf(new ItemStack(Items.STICK).get(DataComponents.FOOD)));
        FoodProperties snack = new ItemStack(SNACK.get()).get(DataComponents.FOOD);
        results.put("foodOwn", snack.nutrition() + "/" + snack.saturation() + "/" + snack.effects().size() + "/"
                + snack.effects().get(0).effect().getDescriptionId() + "@" + snack.effects().get(0).probability());

        ItemStack paper = new ItemStack(Items.PAPER);
        results.put("loreDefault", String.valueOf(paper.get(DataComponents.LORE).lines().size()));
        paper.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("first"), Component.literal("second"))));
        List<String> lines = new ArrayList<>();
        for (Component line : savedBack(level, paper).get(DataComponents.LORE).lines()) {
            lines.add(line.getString());
        }
        results.put("lore", lines.toString());
        results.put("loreAdded", String.valueOf(paper.get(DataComponents.LORE).withLineAdded(Component.literal("third")).lines().size()));

        ItemStack modeled = new ItemStack(Items.STICK);
        results.put("modelDataDefault", String.valueOf(modeled.get(DataComponents.CUSTOM_MODEL_DATA)));
        modeled.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(42));
        results.put("modelData", String.valueOf(savedBack(level, modeled).get(DataComponents.CUSTOM_MODEL_DATA).value()));

        results.put("swordAttributes", attributes(new ItemStack(Items.DIAMOND_SWORD).get(DataComponents.ATTRIBUTE_MODIFIERS)));
        results.put("stickAttributes", attributes(new ItemStack(Items.STICK).get(DataComponents.ATTRIBUTE_MODIFIERS)));
        ItemStack rod = new ItemStack(Items.STICK);
        rod.set(DataComponents.ATTRIBUTE_MODIFIERS, SwordItem.createAttributes(Tiers.STONE, 5, -3.0F).withTooltip(false));
        results.put("setAttributes", attributes(savedBack(level, rod).get(DataComponents.ATTRIBUTE_MODIFIERS)));
        results.put("bladeAttributes", attributes(new ItemStack(BLADE.get()).get(DataComponents.ATTRIBUTE_MODIFIERS)));
        // what the game gives an entity holding them (equipment changes are applied on its tick)
        Zombie zombie = EntityType.ZOMBIE.create(level);
        zombie.setPos(8.5, 141.0, 8.5);
        zombie.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(BLADE.get()));
        zombie.tick();
        results.put("bladeZombie", String.valueOf(zombie.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        zombie.setItemSlot(EquipmentSlot.MAINHAND, rod.copy());
        zombie.tick();
        results.put("rodZombie", String.valueOf(zombie.getAttributeValue(Attributes.ATTACK_DAMAGE)));

        blockComponents(results, level);
        results.put("rarity", new ItemStack(Items.ENCHANTED_GOLDEN_APPLE).get(DataComponents.RARITY) + "/" + new ItemStack(Items.STICK).get(DataComponents.RARITY));
    }

    /** containers, block entity and entity data, block states, a bundle, flags, the stack patch API */
    static void blockComponents(Map<String, String> results, ServerLevel level) {
        ItemStack box = new ItemStack(Items.SHULKER_BOX);
        results.put("boxDefault", String.valueOf(box.get(DataComponents.CONTAINER).stream().count()));
        net.minecraft.world.item.component.ItemContainerContents chestContents = new ItemStack(Items.CHEST).get(DataComponents.CONTAINER);
        results.put("chestContainer", chestContents == null ? "null" : String.valueOf(chestContents.stream().count()));
        results.put("potNoContainer", String.valueOf(new ItemStack(Items.DECORATED_POT).get(DataComponents.CONTAINER)));
        box.set(DataComponents.CONTAINER, net.minecraft.world.item.component.ItemContainerContents.fromItems(
                List.of(new ItemStack(Items.DIAMOND, 2), ItemStack.EMPTY, new ItemStack(Items.APPLE, 5))));
        List<String> boxItems = new ArrayList<>();
        savedBack(level, box).get(DataComponents.CONTAINER).stream().forEach(s -> boxItems.add(s.getCount() + "x" + s.getItem()));
        results.put("boxItems", boxItems.toString());
        results.put("boxNonEmpty", String.valueOf(box.get(DataComponents.CONTAINER).nonEmptyStream().count()));

        ItemStack furnace = new ItemStack(Items.FURNACE);
        CompoundTag furnaceData = new CompoundTag();
        furnaceData.putString("id", "minecraft:furnace");
        furnaceData.putShort("BurnTime", (short) 40);
        furnace.set(DataComponents.BLOCK_ENTITY_DATA, CustomData.of(furnaceData));
        CompoundTag furnaceBack = savedBack(level, furnace).get(DataComponents.BLOCK_ENTITY_DATA).copyTag();
        results.put("blockEntityData", furnaceBack.getString("id") + "/" + furnaceBack.getShort("BurnTime"));

        ItemStack egg = new ItemStack(Items.ZOMBIE_SPAWN_EGG);
        CompoundTag eggData = new CompoundTag();
        eggData.putString("id", "minecraft:zombie");
        eggData.putBoolean("NoAI", true);
        egg.set(DataComponents.ENTITY_DATA, CustomData.of(eggData));
        CompoundTag eggBack = savedBack(level, egg).get(DataComponents.ENTITY_DATA).copyTag();
        results.put("entityData", eggBack.getString("id") + "/" + eggBack.getBoolean("NoAI"));

        ItemStack lamp = new ItemStack(Items.REDSTONE_LAMP);
        lamp.set(DataComponents.BLOCK_STATE, new net.minecraft.world.item.component.BlockItemStateProperties(Map.of("lit", "true")));
        net.minecraft.world.item.component.BlockItemStateProperties lampState = savedBack(level, lamp).get(DataComponents.BLOCK_STATE);
        results.put("blockState", lampState.properties() + "/" + lampState.apply(Blocks.REDSTONE_LAMP.defaultBlockState())
                .getValue(net.minecraft.world.level.block.RedstoneLampBlock.LIT));

        ItemStack bundle = new ItemStack(Items.BUNDLE);
        results.put("bundleDefault", String.valueOf(bundle.get(DataComponents.BUNDLE_CONTENTS).isEmpty()));
        bundle.set(DataComponents.BUNDLE_CONTENTS, new net.minecraft.world.item.component.BundleContents(
                List.of(new ItemStack(Items.STONE, 3), new ItemStack(Items.STICK, 1))));
        net.minecraft.world.item.component.BundleContents bundleBack = savedBack(level, bundle).get(DataComponents.BUNDLE_CONTENTS);
        results.put("bundle", bundleBack.size() + "/" + bundleBack.weight() + "/" + bundleBack.getItemUnsafe(0).getItem());

        ItemStack hive = new ItemStack(Items.BEEHIVE);
        results.put("beesDefault", String.valueOf(hive.get(DataComponents.BEES)));
        CompoundTag beeData = new CompoundTag();
        beeData.putString("id", "minecraft:bee");
        hive.set(DataComponents.BEES, List.of(new net.minecraft.world.level.block.entity.BeehiveBlockEntity.Occupant(CustomData.of(beeData), 10, 600)));
        List<net.minecraft.world.level.block.entity.BeehiveBlockEntity.Occupant> beesBack = savedBack(level, hive).get(DataComponents.BEES);
        results.put("bees", beesBack.size() + "/" + beesBack.get(0).entityData().copyTag().getString("id") + "/" + beesBack.get(0).ticksInHive()
                + "/" + beesBack.get(0).minTicksInHive());

        ItemStack hard = new ItemStack(Items.IRON_SWORD);
        hard.set(DataComponents.UNBREAKABLE, new net.minecraft.world.item.component.Unbreakable(false));
        ItemStack hardBack = savedBack(level, hard);
        results.put("unbreakable", hardBack.get(DataComponents.UNBREAKABLE) + "/" + hardBack.isDamageableItem());
        results.put("repairCostDefault", String.valueOf(hard.get(DataComponents.REPAIR_COST)));
        hard.set(DataComponents.REPAIR_COST, 3);
        hard.set(DataComponents.HIDE_ADDITIONAL_TOOLTIP, net.minecraft.util.Unit.INSTANCE);
        ItemStack hardBack2 = savedBack(level, hard);
        results.put("repairCost", hardBack2.get(DataComponents.REPAIR_COST) + "/" + hardBack2.has(DataComponents.HIDE_ADDITIONAL_TOOLTIP));

        // fireworks and the glint
        net.minecraft.world.item.component.FireworkExplosion burst = new net.minecraft.world.item.component.FireworkExplosion(
                net.minecraft.world.item.component.FireworkExplosion.Shape.STAR, it.unimi.dsi.fastutil.ints.IntList.of(0xFF0000, 0x00FF00),
                it.unimi.dsi.fastutil.ints.IntList.of(0x0000FF), true, false);
        ItemStack star = new ItemStack(Items.FIREWORK_STAR);
        star.set(DataComponents.FIREWORK_EXPLOSION, burst);
        results.put("fireworkStar", String.valueOf(savedBack(level, star).get(DataComponents.FIREWORK_EXPLOSION).equals(burst)));
        ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
        net.minecraft.world.item.component.Fireworks rocketDefault = rocket.get(DataComponents.FIREWORKS);
        results.put("rocketDefault", rocketDefault.flightDuration() + "/" + rocketDefault.explosions().size());
        rocket.set(DataComponents.FIREWORKS, new net.minecraft.world.item.component.Fireworks(2, List.of(burst, burst)));
        net.minecraft.world.item.component.Fireworks rocketBack = savedBack(level, rocket).get(DataComponents.FIREWORKS);
        results.put("rocket", rocketBack.flightDuration() + "/" + rocketBack.explosions().size() + "/" + rocketBack.explosions().get(0).equals(burst));
        ItemStack apple = new ItemStack(Items.ENCHANTED_GOLDEN_APPLE);
        results.put("glintDefault", apple.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE) + "/" + apple.hasFoil());
        apple.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        ItemStack shiny = new ItemStack(Items.STICK);
        shiny.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        results.put("glint", savedBack(level, apple).hasFoil() + "/" + savedBack(level, shiny).hasFoil() + "/" + new ItemStack(Items.STICK).hasFoil());

        // a player's head
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        java.util.UUID someone = java.util.UUID.fromString("8667ba71-b85a-4004-af54-457a9734eed7");
        head.set(DataComponents.PROFILE, new net.minecraft.world.item.component.ResolvableProfile(Optional.of("Steve"), Optional.of(someone),
                new com.mojang.authlib.properties.PropertyMap()));
        net.minecraft.world.item.component.ResolvableProfile headBack = savedBack(level, head).get(DataComponents.PROFILE);
        results.put("profile", headBack.name().orElse("?") + "/" + headBack.id().map(Object::toString).orElse("?") + "/"
                + headBack.gameProfile().getName() + "/" + headBack.isResolved());
        ItemStack named = new ItemStack(Items.PLAYER_HEAD);
        named.set(DataComponents.PROFILE, new net.minecraft.world.item.component.ResolvableProfile(Optional.of("Alex"), Optional.empty(),
                new com.mojang.authlib.properties.PropertyMap()));
        net.minecraft.world.item.component.ResolvableProfile namedBack = savedBack(level, named).get(DataComponents.PROFILE);
        results.put("profileName", namedBack.name().orElse("?") + "/" + namedBack.id().isPresent());
        results.put("headName", savedBack(level, head).getHoverName().getString());

        // an armour trim
        ItemStack plate = new ItemStack(Items.DIAMOND_CHESTPLATE);
        net.minecraft.world.item.armortrim.ArmorTrim trim = new net.minecraft.world.item.armortrim.ArmorTrim(
                level.registryAccess().registryOrThrow(Registries.TRIM_MATERIAL).getHolderOrThrow(net.minecraft.world.item.armortrim.TrimMaterials.GOLD),
                level.registryAccess().registryOrThrow(Registries.TRIM_PATTERN).getHolderOrThrow(net.minecraft.world.item.armortrim.TrimPatterns.COAST));
        plate.set(DataComponents.TRIM, trim);
        net.minecraft.world.item.armortrim.ArmorTrim trimBack = savedBack(level, plate).get(DataComponents.TRIM);
        results.put("trim", trimBack.material().unwrapKey().map(k -> k.location().toString()).orElse("?") + "/"
                + trimBack.pattern().unwrapKey().map(k -> k.location().toString()).orElse("?") + "/" + trimBack.equals(trim));
        results.put("trimDefault", String.valueOf(new ItemStack(Items.DIAMOND_CHESTPLATE).get(DataComponents.TRIM)));

        // a crossbow's projectiles, a book's pages, a map's number, a stew's effects
        ItemStack crossbow = new ItemStack(Items.CROSSBOW);
        results.put("crossbowDefault", String.valueOf(crossbow.get(DataComponents.CHARGED_PROJECTILES).isEmpty()));
        crossbow.set(DataComponents.CHARGED_PROJECTILES, net.minecraft.world.item.component.ChargedProjectiles.of(new ItemStack(Items.ARROW)));
        ItemStack crossbowBack = savedBack(level, crossbow);
        results.put("crossbow", crossbowBack.get(DataComponents.CHARGED_PROJECTILES).getItems().size() + "/"
                + crossbowBack.get(DataComponents.CHARGED_PROJECTILES).contains(Items.ARROW) + "/" + net.minecraft.world.item.CrossbowItem.isCharged(crossbowBack));
        ItemStack quill = new ItemStack(Items.WRITABLE_BOOK);
        results.put("bookDefaultPages", String.valueOf(quill.get(DataComponents.WRITABLE_BOOK_CONTENT).pages().size()));
        quill.set(DataComponents.WRITABLE_BOOK_CONTENT, new net.minecraft.world.item.component.WritableBookContent(
                List.of(net.minecraft.server.network.Filterable.passThrough("page one"), net.minecraft.server.network.Filterable.passThrough("page two"))));
        List<String> pages = new ArrayList<>();
        savedBack(level, quill).get(DataComponents.WRITABLE_BOOK_CONTENT).getPages(false).forEach(pages::add);
        results.put("bookPages", pages.toString());
        ItemStack map = new ItemStack(Items.FILLED_MAP);
        map.set(DataComponents.MAP_ID, new net.minecraft.world.level.saveddata.maps.MapId(7));
        results.put("mapId", String.valueOf(savedBack(level, map).get(DataComponents.MAP_ID).id()));
        ItemStack stew = new ItemStack(Items.SUSPICIOUS_STEW);
        results.put("stewDefault", String.valueOf(stew.get(DataComponents.SUSPICIOUS_STEW_EFFECTS).effects().size()));
        stew.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, new net.minecraft.world.item.component.SuspiciousStewEffects(
                List.of(new net.minecraft.world.item.component.SuspiciousStewEffects.Entry(MobEffects.NIGHT_VISION, 100))));
        net.minecraft.world.item.component.SuspiciousStewEffects stewBack = savedBack(level, stew).get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        results.put("stew", stewBack.effects().size() + "/" + effects(List.of(stewBack.effects().get(0).createEffectInstance())));

        // a fish in a bucket
        ItemStack bucket = new ItemStack(Items.AXOLOTL_BUCKET);
        results.put("bucketDefault", String.valueOf(bucket.get(DataComponents.BUCKET_ENTITY_DATA).isEmpty()));
        CompoundTag fish = new CompoundTag();
        fish.putInt("Variant", 3);
        fish.putFloat("Health", 9.0F);
        bucket.set(DataComponents.BUCKET_ENTITY_DATA, CustomData.of(fish));
        ItemStack bucketBack = savedBack(level, bucket);
        CompoundTag fishBack = bucketBack.get(DataComponents.BUCKET_ENTITY_DATA).copyTag();
        results.put("bucket", fishBack.getInt("Variant") + "/" + fishBack.getFloat("Health") + "/" + bucketBack.has(DataComponents.CUSTOM_DATA));

        // a goat horn, a head's note block sound, a shield's colour
        ItemStack horn = new ItemStack(Items.GOAT_HORN);
        horn.set(DataComponents.INSTRUMENT, level.registryAccess().registryOrThrow(Registries.INSTRUMENT)
                .getHolderOrThrow(net.minecraft.world.item.Instruments.SEEK_GOAT_HORN));
        results.put("instrument", savedBack(level, horn).get(DataComponents.INSTRUMENT).unwrapKey().map(k -> k.location().toString()).orElse("?"));
        ItemStack skull = new ItemStack(Items.PLAYER_HEAD);
        skull.set(DataComponents.NOTE_BLOCK_SOUND, net.minecraft.resources.ResourceLocation.withDefaultNamespace("block.note_block.bell"));
        results.put("noteBlockSound", String.valueOf(savedBack(level, skull).get(DataComponents.NOTE_BLOCK_SOUND)));
        ItemStack shield = new ItemStack(Items.SHIELD);
        shield.set(DataComponents.BASE_COLOR, net.minecraft.world.item.DyeColor.LIME);
        results.put("baseColor", String.valueOf(savedBack(level, shield).get(DataComponents.BASE_COLOR)));

        // edge cases: a long flight, custom data beside stew effects, a trim on a stick
        ItemStack longRocket = new ItemStack(Items.FIREWORK_ROCKET);
        longRocket.set(DataComponents.FIREWORKS, new net.minecraft.world.item.component.Fireworks(200, List.of()));
        results.put("longFlight", String.valueOf(savedBack(level, longRocket).get(DataComponents.FIREWORKS).flightDuration()));
        ItemStack stewData = new ItemStack(Items.SUSPICIOUS_STEW);
        stewData.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, new net.minecraft.world.item.component.SuspiciousStewEffects(
                List.of(new net.minecraft.world.item.component.SuspiciousStewEffects.Entry(MobEffects.NIGHT_VISION, 100))));
        CompoundTag mine = new CompoundTag();
        mine.putInt("mine", 1);
        stewData.set(DataComponents.CUSTOM_DATA, CustomData.of(mine));
        stewData.remove(DataComponents.CUSTOM_DATA);
        results.put("stewKept", stewData.get(DataComponents.SUSPICIOUS_STEW_EFFECTS).effects().size() + "/" + stewData.has(DataComponents.CUSTOM_DATA));
        ItemStack hiddenTrim = new ItemStack(Items.IRON_CHESTPLATE);
        hiddenTrim.set(DataComponents.TRIM, trim.withTooltip(false));
        net.minecraft.world.item.armortrim.ArmorTrim hiddenBack = savedBack(level, hiddenTrim).get(DataComponents.TRIM);
        results.put("trimHidden", hiddenBack.equals(trim.withTooltip(false)) + "/" + hiddenBack.equals(trim) + "/" + trim.equals(hiddenBack));
        // a long flight's rocket lives as long (10 ticks per flight level, plus up to 11)
        net.minecraft.world.entity.projectile.FireworkRocketEntity flying = new net.minecraft.world.entity.projectile.FireworkRocketEntity(
                level, 8.5, 141.0, 8.5, longRocket);
        CompoundTag flyingData = new CompoundTag();
        flying.addAdditionalSaveData(flyingData);
        int lifeTime = flyingData.getInt("LifeTime");
        results.put("longFlightLife", String.valueOf(lifeTime >= 2010 && lifeTime <= 2021));
        ItemStack trimmedStick = new ItemStack(Items.STICK);
        trimmedStick.set(DataComponents.TRIM, trim);
        results.put("trimStick", String.valueOf(savedBack(level, trimmedStick).get(DataComponents.TRIM).equals(trim)));

        // the stack patch API
        ItemStack marked = new ItemStack(Items.STICK);
        results.put("patchEmpty", String.valueOf(marked.isComponentsPatchEmpty()));
        marked.set(CHARGE.get(), 4);
        marked.set(DataComponents.CUSTOM_NAME, Component.literal("Patched"));
        net.minecraft.core.component.DataComponentPatch patch = marked.getComponentsPatch();
        List<String> patched = new ArrayList<>();
        patch.entrySet().forEach(e -> patched.add(String.valueOf(e.getKey())));
        java.util.Collections.sort(patched);
        results.put("patch", patched + "/" + marked.isComponentsPatchEmpty());
        ItemStack applied = new ItemStack(Items.STICK);
        applied.applyComponents(patch);
        results.put("patchApplied", applied.get(CHARGE.get()) + "/" + applied.getHoverName().getString() + "/"
                + ItemStack.isSameItemSameComponents(applied, marked));
        ItemStack made = new ItemStack(Items.STICK.builtInRegistryHolder(), 2, patch);
        results.put("patchMade", made.getCount() + "/" + made.get(CHARGE.get()));
        ItemStack rod = marked.transmuteCopy(Items.BLAZE_ROD, 3);
        results.put("transmuted", rod.getItem() + "/" + rod.getCount() + "/" + rod.get(CHARGE.get()) + "/" + rod.getHoverName().getString());
        ItemStack mapped = new ItemStack(Items.STICK);
        mapped.applyComponents(marked.getComponents());
        results.put("mapApplied", mapped.get(CHARGE.get()) + "/" + mapped.getHoverName().getString());
        results.put("typed", String.valueOf(marked.getComponents().getTyped(CHARGE.get()).value()));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onStarted(ServerStartedEvent event) {
        String out = System.getProperty("ladder.test.b.out");
        if (out == null) {
            return;
        }
        Map<String, String> results = new TreeMap<>();
        try {
            ServerLevel level = event.getServer().overworld();
            ItemStack stack = new ItemStack(Items.STICK);
            results.put("plainHas", String.valueOf(stack.has(CHARGE.get())));
            stack.set(CHARGE.get(), 5);
            results.put("afterSet", String.valueOf(stack.get(CHARGE.get())));
            stack.update(CHARGE.get(), 0, value -> value + 1);
            results.put("afterUpdate", String.valueOf(stack.get(CHARGE.get())));
            stack.set(OWNER.get(), new Owner("ana", 2));
            results.put("owner", String.valueOf(stack.get(OWNER.get())));
            stack.set(DataComponents.CUSTOM_NAME, Component.literal("Ladder stick"));
            CompoundTag own = new CompoundTag();
            own.putInt("k", 7);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(own));
            results.put("name", stack.getHoverName().getString());
            results.put("customData", String.valueOf(stack.get(DataComponents.CUSTOM_DATA).copyTag().getInt("k")));
            // a copy is equal and separate
            ItemStack copy = stack.copy();
            results.put("copyCharge", String.valueOf(copy.get(CHARGE.get())));
            results.put("copySame", String.valueOf(ItemStack.isSameItemSameComponents(stack, copy)));
            copy.remove(CHARGE.get());
            results.put("copyRemoved", String.valueOf(copy.has(CHARGE.get())));
            results.put("originalKept", String.valueOf(stack.get(CHARGE.get())));
            results.put("copyStillSame", String.valueOf(ItemStack.isSameItemSameComponents(stack, copy)));
            // saved and read back
            ItemStack back = ItemStack.parseOptional(level.registryAccess(), (CompoundTag) stack.save(level.registryAccess()));
            results.put("savedBack", back.get(CHARGE.get()) + "/" + back.get(OWNER.get()) + "/" + back.getHoverName().getString());
            // an item's own value: read, set to itself (nothing kept), removed (kept as removed), saved
            ItemStack charged = new ItemStack(CHARGED.get());
            results.put("defaultCharge", String.valueOf(charged.get(CHARGE.get())));
            charged.set(CHARGE.get(), 3);
            results.put("defaultSetSame", String.valueOf(ItemStack.isSameItemSameComponents(charged, new ItemStack(CHARGED.get()))));
            charged.remove(CHARGE.get());
            results.put("defaultRemoved", String.valueOf(charged.get(CHARGE.get())));
            ItemStack removedBack = ItemStack.parseOptional(level.registryAccess(), (CompoundTag) charged.save(level.registryAccess()));
            results.put("defaultRemovedSaved", String.valueOf(removedBack.get(CHARGE.get())));
            results.put("defaultItemComponent", String.valueOf(CHARGED.get().components().get(CHARGE.get())));
            // custom data whose keys are also the game's own (on a damageable item): kept apart from its damage
            ItemStack sword = new ItemStack(Items.IRON_SWORD);
            CompoundTag clash = new CompoundTag();
            clash.putInt("Damage", 7);
            clash.putString("display", "mine");
            sword.set(DataComponents.CUSTOM_DATA, CustomData.of(clash));
            results.put("clashCustom", sword.get(DataComponents.CUSTOM_DATA).copyTag().toString());
            results.put("clashDamage", String.valueOf(sword.get(DataComponents.DAMAGE)));
            results.put("clashName", sword.getHoverName().getString());
            // the game's component as an item's default: read, shown, removed, saved
            ItemStack named = new ItemStack(NAMED.get());
            results.put("defaultName", String.valueOf(named.get(DataComponents.CUSTOM_NAME).getString()));
            results.put("defaultNameShown", named.getHoverName().getString());
            named.remove(DataComponents.CUSTOM_NAME);
            results.put("defaultNameRemoved", String.valueOf(named.get(DataComponents.CUSTOM_NAME)));
            ItemStack namedBack = ItemStack.parseOptional(level.registryAccess(), (CompoundTag) named.save(level.registryAccess()));
            results.put("defaultNameRemovedSaved", String.valueOf(namedBack.get(DataComponents.CUSTOM_NAME)));
            // a component's stream codec (derived from its codec), through a buffer
            net.minecraft.network.RegistryFriendlyByteBuf buffer = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),
                    level.registryAccess());
            @SuppressWarnings("unchecked")
            net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Integer> stream =
                    (net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, Integer>) CHARGE.get().streamCodec();
            stream.encode(buffer, 9);
            results.put("streamBack", String.valueOf(stream.decode(buffer)));
            // a stack as the holder interface
            results.put("holderCharge", String.valueOf(chargeOf(stack)));
            results.put("holderIs", String.valueOf(((Object) stack) instanceof net.minecraft.core.component.DataComponentHolder));
            // the game's own components 1.20.4 keeps elsewhere: a dyed colour, enchantments, an enchanted book's
            ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
            results.put("dyedDefault", String.valueOf(boots.get(DataComponents.DYED_COLOR)));
            boots.set(DataComponents.DYED_COLOR, new DyedItemColor(0x336699, false));
            results.put("dyed", String.valueOf(boots.get(DataComponents.DYED_COLOR)));
            results.put("dyedOrDefault", Integer.toHexString(DyedItemColor.getOrDefault(boots, -1)));
            ItemStack bootsBack = ItemStack.parseOptional(level.registryAccess(), (CompoundTag) boots.save(level.registryAccess()));
            results.put("dyedSaved", String.valueOf(bootsBack.get(DataComponents.DYED_COLOR)));
            Holder<Enchantment> sharpness = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.SHARPNESS);
            ItemStack blade = new ItemStack(Items.DIAMOND_SWORD);
            results.put("enchDefault", String.valueOf(blade.get(DataComponents.ENCHANTMENTS).isEmpty()));
            blade.enchant(sharpness, 3);
            results.put("enchComponent", String.valueOf(blade.get(DataComponents.ENCHANTMENTS).getLevel(sharpness)));
            results.put("enchHelper", String.valueOf(EnchantmentHelper.getItemEnchantmentLevel(sharpness, blade)));
            ItemEnchantments.Mutable more = new ItemEnchantments.Mutable(blade.get(DataComponents.ENCHANTMENTS));
            more.set(sharpness, 5);
            blade.set(DataComponents.ENCHANTMENTS, more.toImmutable().withTooltip(false));
            ItemStack bladeBack = ItemStack.parseOptional(level.registryAccess(), (CompoundTag) blade.save(level.registryAccess()));
            ItemEnchantments bladeEnchantments = bladeBack.get(DataComponents.ENCHANTMENTS);
            results.put("enchSaved", bladeEnchantments.getLevel(sharpness) + " hidden=" + bladeEnchantments.equals(bladeEnchantments.withTooltip(false)));
            results.put("enchSavedHelper", String.valueOf(EnchantmentHelper.getItemEnchantmentLevel(sharpness, bladeBack)));
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            results.put("bookDefault", String.valueOf(book.get(DataComponents.STORED_ENCHANTMENTS).isEmpty()));
            ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            stored.set(sharpness, 2);
            book.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
            ItemStack bookBack = ItemStack.parseOptional(level.registryAccess(), (CompoundTag) book.save(level.registryAccess()));
            results.put("bookSaved", String.valueOf(bookBack.get(DataComponents.STORED_ENCHANTMENTS).getLevel(sharpness)));
            results.put("bookNotOnItself", String.valueOf(bookBack.get(DataComponents.ENCHANTMENTS).getLevel(sharpness)));
            results.put("stickNoBook", String.valueOf(new ItemStack(Items.STICK).get(DataComponents.STORED_ENCHANTMENTS)));
            try {
                moreComponents(results, level);
            } catch (RuntimeException | LinkageError e) {
                results.put("more.error", e + (e.getCause() == null ? "" : " / " + e.getCause()));
            }
            // handed to another mod in a chest (once: a restart finds it there)
            if (level.getBlockEntity(CHEST) instanceof ChestBlockEntity chest) {
                results.put("run", "again");
                // the stack after a restart, as another mod (test mod E, of either version) changed it on the first run
                ItemStack kept = chest.getItem(0);
                results.put("again.charge", String.valueOf(kept.get(CHARGE.get())));
                results.put("again.owner", String.valueOf(kept.get(OWNER.get())));
                results.put("again.name", kept.getHoverName().getString());
                CompoundTag keptData = kept.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
                results.put("again.custom", keptData.getInt("k") + "/" + keptData.getInt("e_mark"));
                results.put("again.sharpness", String.valueOf(kept.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(sharpness)));
            } else {
                level.setBlock(CHEST, Blocks.CHEST.defaultBlockState(), 3);
                ((ChestBlockEntity) level.getBlockEntity(CHEST)).setItem(0, stack.copy());
                results.put("run", "first");
            }
        } catch (RuntimeException | LinkageError e) {
            results.put("error", e + (e.getCause() == null ? "" : " / " + e.getCause()));
        }
        StringBuilder json = new StringBuilder("{\n");
        results.forEach((k, v) -> json.append("  \"").append(k).append("\": \"").append(v.replace("\"", "'")).append("\",\n"));
        json.setLength(json.length() - 2);
        json.append("\n}\n");
        try {
            Files.writeString(Path.of(out), json, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
