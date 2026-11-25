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
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
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
    /** NeoForge 21.1's onEntitySwing with the hand: the hand it was swung with, recorded */
    static String swungWith = "-";
    static final DeferredHolder<Item, Item> SWING_STICK = ITEMS.register("swing_stick", () -> new Item(new Item.Properties()) {
        @Override
        public boolean onEntitySwing(ItemStack stack, net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.InteractionHand hand) {
            swungWith = hand.name();
            return super.onEntitySwing(stack, entity, hand);
        }
    });
    /** 1.21's use duration with the user: longer for a zombie */
    static final DeferredHolder<Item, Item> USE_STICK = ITEMS.register("use_stick", () -> new Item(new Item.Properties()) {
        @Override
        public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
            return entity instanceof Zombie ? 23 : 5;
        }

        @Override
        public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
            return net.minecraft.world.item.UseAnim.BOW;
        }
    });
    /** 1.21's postHurtEnemy: called after a hit that counted */
    static String hits = "";
    static final DeferredHolder<Item, Item> HIT_STICK = ITEMS.register("hit_stick", () -> new Item(new Item.Properties()) {
        @Override
        public boolean hurtEnemy(ItemStack stack, net.minecraft.world.entity.LivingEntity target, net.minecraft.world.entity.LivingEntity attacker) {
            hits += "hurt;";
            return true;
        }

        @Override
        public void postHurtEnemy(ItemStack stack, net.minecraft.world.entity.LivingEntity target, net.minecraft.world.entity.LivingEntity attacker) {
            hits += "post;";
        }
    });
    /** NeoForge 21.1's supportsEnchantment: a stick that takes sharpness, the rest as any item */
    static final DeferredHolder<Item, Item> SHARP_STICK = ITEMS.register("sharp_stick", () -> new Item(new Item.Properties()) {
        @Override
        public boolean supportsEnchantment(ItemStack stack, Holder<Enchantment> enchantment) {
            return enchantment.is(Enchantments.SHARPNESS) || super.supportsEnchantment(stack, enchantment);
        }
    });
    /** NeoForge 21.1's canFitInsideContainerItems(stack): a pouch that fits a container unless it is named */
    static final DeferredHolder<Item, Item> POUCH = ITEMS.register("test_pouch", () -> new Item(new Item.Properties()) {
        @Override
        public boolean canFitInsideContainerItems(ItemStack stack) {
            return !stack.has(DataComponents.CUSTOM_NAME);
        }
    });
    static final DeferredHolder<Item, Item> BLADE = ITEMS.register("test_blade",
            () -> new SwordItem(Tiers.IRON, new Item.Properties().attributes(SwordItem.createAttributes(Tiers.IRON, 4, -2.0F))));
    /** a food of the mod's own, with an effect */
    // armour: a material of the mod's own (a record in the armour material registry) and two pieces of it
    static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, "ladder_test_b");
    static final DeferredHolder<ArmorMaterial, ArmorMaterial> STEEL = MATERIALS.register("steel", () -> new ArmorMaterial(
            net.minecraft.Util.make(new java.util.EnumMap<>(ArmorItem.Type.class), m -> {
                m.put(ArmorItem.Type.BOOTS, 2);
                m.put(ArmorItem.Type.LEGGINGS, 5);
                m.put(ArmorItem.Type.CHESTPLATE, 7);
                m.put(ArmorItem.Type.HELMET, 3);
                m.put(ArmorItem.Type.BODY, 9);
            }), 12, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.of(Items.IRON_INGOT),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("ladder_test_b", "steel"))), 1.5F, 0.05F));
    /** a tier of the mod's own: iron's incorrect blocks (1.20.5's Tier.getIncorrectBlocksForDrops) */
    enum BronzeTier implements net.minecraft.world.item.Tier {
        BRONZE;

        @Override
        public int getUses() {
            return 200;
        }

        @Override
        public float getSpeed() {
            return 5.0F;
        }

        @Override
        public float getAttackDamageBonus() {
            return 1.5F;
        }

        @Override
        public net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> getIncorrectBlocksForDrops() {
            return net.minecraft.tags.BlockTags.INCORRECT_FOR_IRON_TOOL;
        }

        @Override
        public int getEnchantmentValue() {
            return 10;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(Items.COPPER_INGOT);
        }
    }

    static final DeferredHolder<Item, net.minecraft.world.item.PickaxeItem> BRONZE_PICKAXE = ITEMS.register("bronze_pickaxe",
            () -> new net.minecraft.world.item.PickaxeItem(BronzeTier.BRONZE, new Item.Properties()));

    static final DeferredHolder<Item, ArmorItem> STEEL_HELMET = ITEMS.register("steel_helmet",
            () -> new ArmorItem(STEEL, ArmorItem.Type.HELMET, new Item.Properties().durability(ArmorItem.Type.HELMET.getDurability(20))));
    static final DeferredHolder<Item, ArmorItem> STEEL_CHESTPLATE = ITEMS.register("steel_chestplate",
            () -> new SteelChestplate(new Item.Properties().durability(ArmorItem.Type.CHESTPLATE.getDurability(20))));
    // no durability (1.20.5's armour takes none of its material: the stack size stays), a field of its own named as ArmorItem's
    static final DeferredHolder<Item, ArmorItem> STEEL_BOOTS = ITEMS.register("steel_boots", () -> new SteelBoots(new Item.Properties().stacksTo(16)));
    static final DeferredHolder<Item, net.minecraft.world.item.AnimalArmorItem> STEEL_HORSE_ARMOR = ITEMS.register("steel_horse_armor",
            () -> new net.minecraft.world.item.AnimalArmorItem(STEEL, net.minecraft.world.item.AnimalArmorItem.BodyType.EQUESTRIAN, false,
                    new Item.Properties().stacksTo(1)));

    static final class SteelBoots extends ArmorItem {
        final Holder<ArmorMaterial> material = ArmorMaterials.GOLD;

        SteelBoots(Item.Properties properties) {
            super(STEEL, ArmorItem.Type.BOOTS, properties);
        }

        String ownMaterial() {
            return this.material.unwrapKey().map(k -> k.location().toString()).orElse("?") + "/" + (((ArmorItem) this).getMaterial() == STEEL);
        }
    }

    /** a mod's armour item of its own class (its super call takes the material holder) */
    static final class SteelChestplate extends ArmorItem {
        SteelChestplate(Item.Properties properties) {
            super(STEEL, ArmorItem.Type.CHESTPLATE, properties);
        }

        int defenseOfMaterial() {
            return this.material.value().getDefense(this.type);
        }
    }

    static final DeferredHolder<Item, Item> SNACK = ITEMS.register("test_snack", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .nutrition(3).saturationModifier(0.5F).effect(() -> new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 100, 0), 0.5F).build())));

    /** entities of the mod's own with synched data: one right under Entity, one under the zombie */
    static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, "ladder_test_b");
    static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<Marker>> MARKER = ENTITIES.register("marker",
            () -> net.minecraft.world.entity.EntityType.Builder.<Marker>of(Marker::new, net.minecraft.world.entity.MobCategory.MISC).sized(0.5F, 0.5F).build("marker"));
    static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<BigZombie>> BIG_ZOMBIE = ENTITIES.register("big_zombie",
            () -> net.minecraft.world.entity.EntityType.Builder.<BigZombie>of(BigZombie::new, net.minecraft.world.entity.MobCategory.MONSTER).sized(0.6F, 1.95F)
                    .build("big_zombie"));

    static final class Marker extends net.minecraft.world.entity.Entity {
        static final net.minecraft.network.syncher.EntityDataAccessor<Integer> MARK = net.minecraft.network.syncher.SynchedEntityData.defineId(Marker.class,
                net.minecraft.network.syncher.EntityDataSerializers.INT);

        Marker(net.minecraft.world.entity.EntityType<? extends Marker> type, net.minecraft.world.level.Level level) {
            super(type, level);
        }

        @Override
        protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
            builder.define(MARK, 7);
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) {
            this.entityData.set(MARK, tag.getInt("mark"));
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {
            tag.putInt("mark", this.entityData.get(MARK));
        }
    }

    /** a mod's abstract base right under Entity, and a concrete entity of it */
    abstract static class MarkerBase extends net.minecraft.world.entity.Entity {
        static final net.minecraft.network.syncher.EntityDataAccessor<Integer> LEVEL = net.minecraft.network.syncher.SynchedEntityData.defineId(MarkerBase.class,
                net.minecraft.network.syncher.EntityDataSerializers.INT);

        MarkerBase(net.minecraft.world.entity.EntityType<?> type, net.minecraft.world.level.Level level) {
            super(type, level);
        }

        @Override
        protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
            builder.define(LEVEL, 3);
        }

        @Override
        protected void readAdditionalSaveData(CompoundTag tag) {
        }

        @Override
        protected void addAdditionalSaveData(CompoundTag tag) {
        }
    }

    static final class SubMarker extends MarkerBase {
        SubMarker(net.minecraft.world.entity.EntityType<? extends SubMarker> type, net.minecraft.world.level.Level level) {
            super(type, level);
        }
    }

    static final DeferredHolder<net.minecraft.world.entity.EntityType<?>, net.minecraft.world.entity.EntityType<SubMarker>> SUB_MARKER = ENTITIES.register("sub_marker",
            () -> net.minecraft.world.entity.EntityType.Builder.<SubMarker>of(SubMarker::new, net.minecraft.world.entity.MobCategory.MISC).sized(0.5F, 0.5F).build("sub_marker"));

    static final class BigZombie extends net.minecraft.world.entity.monster.Zombie {
        static final net.minecraft.network.syncher.EntityDataAccessor<String> TITLE = net.minecraft.network.syncher.SynchedEntityData.defineId(BigZombie.class,
                net.minecraft.network.syncher.EntityDataSerializers.STRING);

        BigZombie(net.minecraft.world.entity.EntityType<? extends BigZombie> type, net.minecraft.world.level.Level level) {
            super(type, level);
        }

        @Override
        protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
            super.defineSynchedData(builder);
            builder.define(TITLE, "boss");
        }

        /** what finalizeSpawn saw (1.20.4 passes a spawn NBT besides, which 1.20.5 dropped) and gave back */
        String spawnSeen = "-";

        @Override
        public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
                net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType type,
                @javax.annotation.Nullable net.minecraft.world.entity.SpawnGroupData data) {
            net.minecraft.world.entity.SpawnGroupData out = super.finalizeSpawn(level, difficulty, type, data);
            this.spawnSeen = type + " " + (data == null ? "null" : data instanceof net.minecraft.world.entity.monster.Zombie.ZombieGroupData z
                    ? "zombie baby=" + z.isBaby + " jockey=" + z.canSpawnJockey : data.getClass().getSimpleName())
                    + " -> " + (out == null ? "null" : out.getClass().getSimpleName()) + " baby=" + this.isBaby();
            return out;
        }
    }

    record Owner(String name, int level) {
        static final Codec<Owner> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.STRING.fieldOf("name").forGetter(Owner::name), Codec.INT.fieldOf("level").forGetter(Owner::level)).apply(i, Owner::new));
    }

    public LadderTestB(IEventBus modBus) {
        COMPONENTS.register(modBus);
        MATERIALS.register(modBus);
        ITEMS.register(modBus);
        ENTITIES.register(modBus);
        modBus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) ->
                event.put(BIG_ZOMBIE.get(), net.minecraft.world.entity.monster.Zombie.createAttributes().build()));
        NeoForge.EVENT_BUS.register(this);
        // NeoForge 21's damage events: the incoming damage halved for a named zombie, the damage taken recorded
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) -> {
            if ("halved".equals(event.getEntity().getName().getString())) {
                incomingSeen = event.getAmount();
                event.setAmount(event.getAmount() / 2);
            }
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Pre event) -> {
            if ("halved".equals(event.getEntity().getName().getString())) {
                damageSeen = event.getNewDamage();
            }
        });
    }

    static volatile float incomingSeen = -1;
    static volatile float damageSeen = -1;

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
        // a player's hit with the stick: hurtEnemy, then postHurtEnemy (1.21)
        net.minecraft.server.level.ServerPlayer hitter = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        hitter.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(HIT_STICK.get()));
        Zombie target = EntityType.ZOMBIE.create(level);
        target.setPos(8.5, 141.0, 8.5);
        hitter.attack(target);
        results.put("postHurt", hits);
        // damage through NeoForge 21's events: 10 coming in, halved, taken
        Zombie halved = EntityType.ZOMBIE.create(level);
        halved.setPos(8.5, 141.0, 8.5);
        halved.setCustomName(Component.literal("halved"));
        float before = halved.getHealth();
        halved.hurt(level.damageSources().generic(), 10.0F);
        results.put("damageEvents", incomingSeen + "/" + damageSeen + "/" + (before - halved.getHealth()));
        // the use duration the game asks with the user (startUsingItem), and the item's answer for the zombie
        ItemStack use = new ItemStack(USE_STICK.get());
        zombie.setItemSlot(EquipmentSlot.MAINHAND, use);
        zombie.startUsingItem(net.minecraft.world.InteractionHand.MAIN_HAND);
        results.put("useDuration", use.getUseDuration(zombie) + "/" + zombie.getUseItemRemainingTicks() + "/" + zombie.isUsingItem());
        zombie.stopUsingItem();
        // attribute modifiers named by a ResourceLocation (1.21)
        net.minecraft.world.entity.ai.attributes.AttributeInstance speed = zombie.getAttribute(Attributes.MOVEMENT_SPEED);
        ResourceLocation hurry = ResourceLocation.fromNamespaceAndPath("ladder_test_b", "hurry");
        speed.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(hurry, 0.5,
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        net.minecraft.world.entity.ai.attributes.AttributeModifier got = speed.getModifier(hurry);
        results.put("attributeIds", (got != null && got.id().equals(hurry)) + "/" + speed.hasModifier(hurry) + "/" + (got != null && got.is(hurry)) + "/"
                + speed.removeModifier(hurry) + "/" + speed.hasModifier(hurry) + "/" + speed.removeModifier(hurry));

        blockComponents(results, level);
        try {
            Marker marker = MARKER.get().create(level);
            results.put("entityMark", String.valueOf(marker.getEntityData().get(Marker.MARK)));
            marker.getEntityData().set(Marker.MARK, 9);
            CompoundTag markerTag = new CompoundTag();
            marker.saveWithoutId(markerTag);
            Marker markerBack = MARKER.get().create(level);
            markerBack.load(markerTag);
            results.put("entityMarkSaved", String.valueOf(markerBack.getEntityData().get(Marker.MARK)));
            results.put("entitySubMarker", String.valueOf(SUB_MARKER.get().create(level).getEntityData().get(MarkerBase.LEVEL)));
            BigZombie big = BIG_ZOMBIE.get().create(level);
            results.put("entityTitle", big.getEntityData().get(BigZombie.TITLE) + "/" + big.isBaby() + "/" + big.getEntityData().getNonDefaultValues().stream().map(v -> v.id() + "=" + v.value()).toList());
            big.setBaby(true);
            results.put("entityBaby", big.isBaby() + "/" + big.getEntityData().get(BigZombie.TITLE));
        } catch (RuntimeException | LinkageError e) {
            results.put("entity.error", e.toString());
        }
        try {
            lootAndCodecs(results, level);
            mining(results);
            spawning(results, level);
        } catch (RuntimeException | LinkageError e) {
            results.put("loot.error", e.toString());
        }
        results.put("rarity", new ItemStack(Items.ENCHANTED_GOLDEN_APPLE).get(DataComponents.RARITY) + "/" + new ItemStack(Items.STICK).get(DataComponents.RARITY));

        // armour: a mod's material and pieces, the game's materials, body armour
        ArmorItem helmet = STEEL_HELMET.get();
        SteelChestplate chestplate = (SteelChestplate) STEEL_CHESTPLATE.get();
        results.put("armourDefense", helmet.getDefense() + "/" + chestplate.getDefense() + "/" + helmet.getToughness() + "/" + chestplate.defenseOfMaterial());
        results.put("armourDurability", new ItemStack(helmet).getMaxDamage() + "/" + new ItemStack(chestplate).getMaxDamage() + "/"
                + ArmorItem.Type.BODY.getDurability(4) + "/" + ArmorItem.Type.BOOTS.getDurability(3));
        results.put("armourEnchantability", String.valueOf(helmet.getEnchantmentValue()));
        results.put("armourRepair", helmet.isValidRepairItem(new ItemStack(helmet), new ItemStack(Items.IRON_INGOT)) + "/"
                + helmet.isValidRepairItem(new ItemStack(helmet), new ItemStack(Items.STICK)));
        Holder<ArmorMaterial> steel = helmet.getMaterial();
        results.put("armourMaterial", (steel == STEEL) + "/" + steel.value().defense().get(ArmorItem.Type.HELMET) + "/" + steel.value().getDefense(ArmorItem.Type.BODY)
                + "/" + steel.value().enchantmentValue() + "/" + steel.value().toughness() + "/" + steel.value().knockbackResistance());
        results.put("armourTexture", steel.value().layers().get(0).texture(false) + " " + steel.value().layers().get(0).texture(true));
        results.put("armourSound", String.valueOf(steel.value().equipSound().unwrapKey().map(k -> k.location().toString()).orElse("?")));
        results.put("armourAttributes", attributes(new ItemStack(chestplate).get(DataComponents.ATTRIBUTE_MODIFIERS)));
        Zombie armoured = EntityType.ZOMBIE.create(level);
        armoured.setPos(8.5, 141.0, 8.5);
        armoured.setItemSlot(EquipmentSlot.CHEST, new ItemStack(chestplate));
        armoured.setItemSlot(EquipmentSlot.HEAD, new ItemStack(helmet));
        armoured.tick();
        results.put("armourZombie", armoured.getAttributeValue(Attributes.ARMOR) + "/" + armoured.getAttributeValue(Attributes.ARMOR_TOUGHNESS) + "/"
                + armoured.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        ArmorItem ironHelmet = (ArmorItem) Items.IRON_HELMET;
        results.put("vanillaMaterial", (ironHelmet.getMaterial() == ArmorMaterials.IRON) + "/" + ArmorMaterials.IRON.unwrapKey().map(k -> k.location().toString()).orElse("?")
                + "/" + ArmorMaterials.IRON.value().getDefense(ArmorItem.Type.CHESTPLATE) + "/" + ArmorMaterials.IRON.value().getDefense(ArmorItem.Type.BODY)
                + "/" + ArmorMaterials.DIAMOND.value().toughness() + "/" + ArmorMaterials.NETHERITE.value().knockbackResistance()
                + "/" + ArmorMaterials.GOLD.value().enchantmentValue() + "/" + ArmorMaterials.LEATHER.value().layers().size());
        results.put("vanillaRepair", ArmorMaterials.DIAMOND.value().repairIngredient().get().test(new ItemStack(Items.DIAMOND)) + "/"
                + ArmorMaterials.DIAMOND.value().repairIngredient().get().test(new ItemStack(Items.IRON_INGOT)));
        results.put("armourTypes", ArmorItem.Type.values().length + "/" + ArmorItem.Type.BODY.getName() + "/" + ArmorItem.Type.valueOf("BODY").getName());
        results.put("armourRegistry", String.valueOf(level.registryAccess().registryOrThrow(Registries.ARMOR_MATERIAL).containsKey(
                ResourceLocation.fromNamespaceAndPath("ladder_test_b", "steel"))));
        Horse horse = EntityType.HORSE.create(level);
        horse.setPos(8.5, 141.0, 8.5);
        results.put("bodyArmourEmpty", horse.isWearingBodyArmor() + "/" + horse.getBodyArmorItem().isEmpty());
        horse.setBodyArmorItem(new ItemStack(Items.IRON_HORSE_ARMOR));
        results.put("bodyArmour", horse.isWearingBodyArmor() + "/" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(horse.getBodyArmorItem().getItem())
                + "/" + horse.isBodyArmorItem(new ItemStack(Items.IRON_HORSE_ARMOR)) + "/" + horse.isBodyArmorItem(new ItemStack(Items.IRON_HELMET)));
        SteelBoots boots = (SteelBoots) STEEL_BOOTS.get();
        results.put("armourStack", new ItemStack(boots).getMaxStackSize() + "/" + new ItemStack(boots).getMaxDamage() + "/" + boots.ownMaterial()
                + "/" + boots.getDefense());
        // body armour worn: its protection, after a save and a load, a mod's horse armour
        horse.tick();
        results.put("bodyArmourProtection", horse.getAttributeValue(Attributes.ARMOR) + "/" + horse.getAttributeValue(Attributes.ARMOR_TOUGHNESS));
        results.put("bodyArmourReload", reloaded(level, horse));
        Horse modHorse = EntityType.HORSE.create(level);
        modHorse.setPos(8.5, 141.0, 8.5);
        ItemStack steelHorseArmour = new ItemStack(STEEL_HORSE_ARMOR.get());
        modHorse.setBodyArmorItem(steelHorseArmour);
        modHorse.tick();
        results.put("modBodyArmour", modHorse.isBodyArmorItem(steelHorseArmour) + "/" + modHorse.isWearingBodyArmor() + "/"
                + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(modHorse.getBodyArmorItem().getItem()) + "/"
                + modHorse.getAttributeValue(Attributes.ARMOR) + "/" + modHorse.getAttributeValue(Attributes.ARMOR_TOUGHNESS) + "/"
                + STEEL_HORSE_ARMOR.get().getBodyType());
        results.put("modBodyArmourReload", reloaded(level, modHorse));
        net.minecraft.world.entity.animal.horse.Llama llama = EntityType.LLAMA.create(level);
        llama.setPos(8.5, 141.0, 8.5);
        ItemStack carpet = new ItemStack(Items.RED_CARPET);
        llama.setBodyArmorItem(carpet);
        CompoundTag llamaTag = new CompoundTag();
        llama.saveWithoutId(llamaTag);
        net.minecraft.world.entity.animal.horse.Llama llamaCopy = EntityType.LLAMA.create(level);
        llamaCopy.load(llamaTag);
        results.put("llamaBodyArmour", llama.isBodyArmorItem(carpet) + "/" + llama.isWearingBodyArmor() + "/"
                + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(llama.getBodyArmorItem().getItem()) + "/" + llama.getSwag() + "/"
                + llamaCopy.isWearingBodyArmor() + "/" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(llamaCopy.getBodyArmorItem().getItem())
                + "/" + llamaCopy.getSwag());
    }

    /** a horse saved and loaded again: what it wears, its protection */
    static String reloaded(ServerLevel level, Horse horse) {
        CompoundTag tag = new CompoundTag();
        horse.saveWithoutId(tag);
        Horse copy = EntityType.HORSE.create(level);
        copy.load(tag);
        copy.setPos(8.5, 141.0, 8.5);
        copy.tick();
        return copy.isWearingBodyArmor() + "/" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(copy.getBodyArmorItem().getItem()) + "/"
                + copy.getAttributeValue(Attributes.ARMOR);
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

    /** a mod mob's finalizeSpawn: through the game's spawn (no spawn data), and called by the mod with spawn data */
    static void spawning(Map<String, String> results, ServerLevel level) {
        BlockPos pos = new BlockPos(8, 141, 8);
        BigZombie spawned = (BigZombie) BIG_ZOMBIE.get().spawn(level, pos, net.minecraft.world.entity.MobSpawnType.SPAWN_EGG);
        // (not whether it came out a baby: the game's spawn draws that at random)
        results.put("spawnByGame", spawned == null ? "none" : spawned.spawnSeen.replaceAll(" baby=(true|false)$", ""));
        if (spawned != null) {
            spawned.discard();
        }
        BigZombie big = BIG_ZOMBIE.get().create(level);
        big.setPos(8.5, 141.0, 8.5);
        net.minecraft.world.entity.SpawnGroupData back = big.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), net.minecraft.world.entity.MobSpawnType.COMMAND,
                new net.minecraft.world.entity.monster.Zombie.ZombieGroupData(true, false));
        results.put("spawnByMod", big.spawnSeen + " | returned " + (back == null ? "null" : back.getClass().getSimpleName()));
        // a swing of the mod's item in the off hand: its override sees the hand
        big.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new ItemStack(SWING_STICK.get()));
        big.swing(net.minecraft.world.InteractionHand.OFF_HAND);
        results.put("swingHand", swungWith + "/" + new ItemStack(SWING_STICK.get()).onEntitySwing(big, net.minecraft.world.InteractionHand.MAIN_HAND) + "/" + swungWith);
        // tamed animals flying to their owner (1.21: canFlyToOwner, the parrot's and a mod animal's own), their follow goal
        FlyingCat cat = new FlyingCat(EntityType.CAT, level);
        net.minecraft.world.entity.ai.goal.FollowOwnerGoal goal = new net.minecraft.world.entity.ai.goal.FollowOwnerGoal(cat, 1.0, 10.0F, 2.0F);
        results.put("flyToOwner", cat.canFlyToOwner() + "/" + cat.superFlies() + "/" + (goal != null));
    }

    /** a tamed animal of the mod's own that flies to its owner */
    static final class FlyingCat extends net.minecraft.world.entity.animal.Cat {
        FlyingCat(EntityType<? extends net.minecraft.world.entity.animal.Cat> type, net.minecraft.world.level.Level level) {
            super(type, level);
        }

        @Override
        public boolean canFlyToOwner() {
            return true;
        }

        /** the game's own answer for a cat (a super call) */
        boolean superFlies() {
            return super.canFlyToOwner();
        }
    }

    /** which blocks a tool drops: a mod tier's pickaxe (its incorrect tag), the game's own; the 1.20.5+ tags */
    static void mining(Map<String, String> results) {
        net.minecraft.world.level.block.Block[] blocks = {Blocks.OBSIDIAN, Blocks.DIAMOND_ORE, Blocks.IRON_ORE, Blocks.STONE, Blocks.DIRT};
        for (Item tool : new Item[] {BRONZE_PICKAXE.get(), Items.IRON_PICKAXE, Items.WOODEN_PICKAXE, Items.NETHERITE_PICKAXE}) {
            ItemStack stack = new ItemStack(tool);
            StringBuilder b = new StringBuilder();
            for (net.minecraft.world.level.block.Block block : blocks) {
                b.append(stack.isCorrectToolForDrops(block.defaultBlockState()) ? "1" : "0");
            }
            results.put("mining." + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(tool).getPath(), b.toString());
        }
        results.put("miningTierTags", BronzeTier.BRONZE.getIncorrectBlocksForDrops().location() + " "
                + net.minecraft.world.item.Tiers.IRON.getIncorrectBlocksForDrops().location() + " " + net.minecraft.world.item.Tiers.GOLD.getIncorrectBlocksForDrops().location());
        results.put("tagsNew", Blocks.OBSIDIAN.defaultBlockState().is(net.minecraft.tags.BlockTags.INCORRECT_FOR_IRON_TOOL) + "/"
                + Blocks.STONE.defaultBlockState().is(net.minecraft.tags.BlockTags.INCORRECT_FOR_WOODEN_TOOL) + "/"
                + Blocks.DIAMOND_ORE.defaultBlockState().is(net.minecraft.tags.BlockTags.INCORRECT_FOR_NETHERITE_TOOL) + "/"
                + Blocks.BEEHIVE.defaultBlockState().is(net.minecraft.tags.BlockTags.DOES_NOT_BLOCK_HOPPERS) + "/"
                + new ItemStack(Items.DIAMOND_SWORD).is(net.minecraft.tags.ItemTags.SWORD_ENCHANTABLE) + "/"
                + new ItemStack(Items.IRON_AXE).is(net.minecraft.tags.ItemTags.WEAPON_ENCHANTABLE) + "/"
                + new ItemStack(Items.STICK).is(net.minecraft.tags.ItemTags.WEAPON_ENCHANTABLE));
    }

    /** a loot result: its item, count and the components the loot functions touch */
    static String lootStack(ItemStack s) {
        net.minecraft.world.item.component.ItemLore lore = s.get(DataComponents.LORE);
        PotionContents potion = s.get(DataComponents.POTION_CONTENTS);
        return s.getCount() + "x" + net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem())
                + " name=" + (s.has(DataComponents.CUSTOM_NAME) ? s.get(DataComponents.CUSTOM_NAME).getString() : "-")
                + " itemName=" + (s.has(DataComponents.ITEM_NAME) ? s.get(DataComponents.ITEM_NAME).getString() : "-") + " shown=" + s.getHoverName().getString()
                + " lore=" + (lore == null ? "-" : lore.lines().stream().map(Component::getString).toList())
                + " potion=" + (potion == null ? "-" : potion.potion().flatMap(p -> p.unwrapKey()).map(k -> k.location().toString()).orElse("?") + "/"
                        + potion.customColor().map(String::valueOf).orElse("-"))
                + " damage=" + s.getDamageValue() + " model=" + (s.has(DataComponents.CUSTOM_MODEL_DATA) ? s.get(DataComponents.CUSTOM_MODEL_DATA).value() : "-")
                + " attributes=" + s.has(DataComponents.ATTRIBUTE_MODIFIERS) + " rarity=" + s.getRarity();
    }

    /** a loot table by its key (1.20.5's reloadable registries), and stacks through 1.21's codecs */
    static void lootAndCodecs(Map<String, String> results, ServerLevel level) {
        net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> key = net.minecraft.resources.ResourceKey.create(
                Registries.LOOT_TABLE, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ladder_test_b", "test"));
        net.minecraft.world.level.storage.loot.LootTable table = level.getServer().reloadableRegistries().getLootTable(key);
        net.minecraft.world.level.storage.loot.LootParams params = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.CHEST);
        List<String> loot = new ArrayList<>();
        table.getRandomItems(params, 42L).forEach(s -> loot.add(s.getCount() + "x" + s.getItem()));
        results.put("lootByKey", loot.toString());
        // set_components (values, a removal, in turn, under conditions), set_lore modes, set_name's target; copy_components
        net.minecraft.world.level.storage.loot.LootTable components = level.getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(
                Registries.LOOT_TABLE, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ladder_test_b", "components")));
        List<String> made = new ArrayList<>();
        components.getRandomItems(params, 42L).forEach(s -> made.add(lootStack(s)));
        results.put("lootComponents", String.join(" | ", made));
        for (String name : new String[] {"Named chest", null}) {
            net.minecraft.world.level.block.entity.ChestBlockEntity chest = new net.minecraft.world.level.block.entity.ChestBlockEntity(BlockPos.ZERO,
                    Blocks.CHEST.defaultBlockState());
            if (name != null) {
                CompoundTag tag = new CompoundTag();
                tag.putString("CustomName", Component.Serializer.toJson(Component.literal(name), level.registryAccess()));
                chest.loadCustomOnly(tag, level.registryAccess());
            }
            net.minecraft.world.level.storage.loot.LootParams blockParams = new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, net.minecraft.world.phys.Vec3.ZERO)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE, Blocks.CHEST.defaultBlockState())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.TOOL, ItemStack.EMPTY)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY, chest)
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.BLOCK);
            List<String> drops = new ArrayList<>();
            level.getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ladder_test_b", "named_block"))).getRandomItems(blockParams, 42L)
                    .forEach(s -> drops.add(lootStack(s)));
            results.put(name == null ? "lootCopyUnnamed" : "lootCopyNamed", drops.toString());
        }
        results.put("lootKeys", String.valueOf(level.getServer().reloadableRegistries().getKeys(Registries.LOOT_TABLE)
                .contains(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ladder_test_b", "test"))));
        results.put("lootMissing", String.valueOf(level.getServer().reloadableRegistries().getLootTable(net.minecraft.resources.ResourceKey.create(
                Registries.LOOT_TABLE, net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ladder_test_b", "none")))
                == net.minecraft.world.level.storage.loot.LootTable.EMPTY));

        com.mojang.serialization.DynamicOps<com.google.gson.JsonElement> ops = level.registryAccess().createSerializationContext(
                com.mojang.serialization.JsonOps.INSTANCE);
        ItemStack gem = new ItemStack(Items.DIAMOND, 5);
        gem.set(DataComponents.CUSTOM_NAME, Component.literal("Gem"));
        com.google.gson.JsonElement json = ItemStack.CODEC.encodeStart(ops, gem).result().orElseThrow();
        results.put("codecJson", json.toString().replace("\\", ""));
        ItemStack back = ItemStack.CODEC.parse(ops, json).result().orElseThrow();
        results.put("codecBack", back.getCount() + "/" + back.getHoverName().getString() + "/" + ItemStack.isSameItemSameComponents(back, gem));
        results.put("codecEmpty", String.valueOf(ItemStack.OPTIONAL_CODEC.encodeStart(ops, ItemStack.EMPTY).result().orElseThrow()));
        com.google.gson.JsonElement plain = com.google.gson.JsonParser.parseString("{\"id\":\"minecraft:stick\"}");
        results.put("codecPlain", String.valueOf(ItemStack.CODEC.parse(ops, plain).result().map(s -> s.getCount() + "x" + s.getItem()).orElse("none")));
        ItemStack apple = new ItemStack(Items.APPLE, 3);
        apple.consume(1, null);
        results.put("consume", String.valueOf(apple.getCount()));
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
            // 1.21's curses as enchantment effects and a tag
            Holder<Enchantment> binding = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.BINDING_CURSE);
            ItemStack cursed = new ItemStack(Items.IRON_HELMET);
            cursed.enchant(binding, 1);
            results.put("curses", EnchantmentHelper.has(cursed, net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE) + "/"
                    + EnchantmentHelper.has(cursed, net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP) + "/"
                    + binding.is(net.minecraft.tags.EnchantmentTags.CURSE) + "/" + sharpness.is(net.minecraft.tags.EnchantmentTags.CURSE));
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
            Holder<Enchantment> efficiency = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.EFFICIENCY);
            ItemStack sharpStick = new ItemStack(SHARP_STICK.get());
            results.put("supports.sharpStick", sharpStick.supportsEnchantment(sharpness) + "/" + sharpStick.supportsEnchantment(efficiency));
            results.put("supports.canEnchant", sharpness.value().canEnchant(sharpStick) + "/" + efficiency.value().canEnchant(sharpStick));
            results.put("supports.book", new ItemStack(Items.ENCHANTED_BOOK).supportsEnchantment(efficiency) + "/"
                    + new ItemStack(Items.IRON_PICKAXE).supportsEnchantment(efficiency) + "/" + new ItemStack(Items.STICK).supportsEnchantment(sharpness));
            try {
                // the anvil asks the stack (21.1), so the stick takes a sharpness book and not an efficiency one
                net.minecraft.server.level.ServerPlayer smith = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
                StringBuilder anvil = new StringBuilder();
                for (Holder<Enchantment> e : List.of(sharpness, efficiency)) {
                    net.minecraft.world.inventory.AnvilMenu menu = new net.minecraft.world.inventory.AnvilMenu(0, smith.getInventory(),
                            net.minecraft.world.inventory.ContainerLevelAccess.create(level, BlockPos.ZERO));
                    ItemStack enchBook = new ItemStack(Items.ENCHANTED_BOOK);
                    ItemEnchantments.Mutable bookEnch = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                    bookEnch.set(e, 2);
                    enchBook.set(DataComponents.STORED_ENCHANTMENTS, bookEnch.toImmutable());
                    menu.getSlot(0).set(new ItemStack(SHARP_STICK.get()));
                    menu.getSlot(1).set(enchBook);
                    menu.createResult();
                    ItemStack anvilOut = menu.getSlot(2).getItem();
                    anvil.append(anvilOut.isEmpty() ? "none" : String.valueOf(anvilOut.getEnchantments().getLevel(e))).append(';');
                }
                results.put("supports.anvil", anvil.toString());
            } catch (RuntimeException | LinkageError e) {
                results.put("supports.anvil", "error " + e);
            }
            try {
                // vanilla mending: an experience orb a player touches repairs a damaged mending item in the hand, the
                // rest of its experience going to the player (the first orb all into the pickaxe, the second partly)
                Holder<Enchantment> mending = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.MENDING);
                net.minecraft.server.level.ServerPlayer mender = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,
                        new com.mojang.authlib.GameProfile(java.util.UUID.fromString("6c2a4f5e-6b1d-4e2a-9f0c-1d2e3f4a5b6c"), "ladder_mender"));
                ItemStack pick = new ItemStack(Items.DIAMOND_PICKAXE);
                pick.enchant(mending, 1);
                pick.setDamageValue(100);
                mender.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, pick);
                StringBuilder mended = new StringBuilder();
                for (int value : new int[] {30, 30, 7}) {
                    net.minecraft.world.entity.ExperienceOrb orb = new net.minecraft.world.entity.ExperienceOrb(level, 8.5, 141.0, 8.5, value);
                    mender.takeXpDelay = 0;
                    orb.playerTouch(mender);
                    ItemStack held = mender.getMainHandItem();
                    mended.append(held.getDamageValue()).append('/').append(mender.totalExperience).append('/').append(orb.isRemoved()).append(';');
                }
                results.put("mending", mended.toString());
            } catch (RuntimeException | LinkageError e) {
                results.put("mending", "error " + e);
            }
            try {
                // the containers ask the stack (21.1): a named pouch fits neither a shulker box nor a bundle
                ItemStack pouch = new ItemStack(POUCH.get());
                ItemStack namedPouch = new ItemStack(POUCH.get());
                namedPouch.set(DataComponents.CUSTOM_NAME, Component.literal("full"));
                net.minecraft.world.inventory.ShulkerBoxSlot slot = new net.minecraft.world.inventory.ShulkerBoxSlot(new net.minecraft.world.SimpleContainer(27), 0, 0, 0);
                results.put("fit.shulkerSlot", slot.mayPlace(pouch) + "/" + slot.mayPlace(namedPouch));
                results.put("fit.bundle", new net.minecraft.world.item.component.BundleContents.Mutable(net.minecraft.world.item.component.BundleContents.EMPTY).tryInsert(pouch.copy())
                        + "/" + new net.minecraft.world.item.component.BundleContents.Mutable(net.minecraft.world.item.component.BundleContents.EMPTY).tryInsert(namedPouch.copy()));
                results.put("fit.stack", pouch.canFitInsideContainerItems() + "/" + namedPouch.canFitInsideContainerItems());
            } catch (RuntimeException | LinkageError e) {
                results.put("fit.error", e.toString());
            }
            try {
                // an enchantment of the mod's data (1.21: a data registry; below: made from the file)
                Holder<Enchantment> keen = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                        .getHolderOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,
                                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("ladder_test_b", "keen")));
                results.put("dataEnchant", keen.value().getMaxLevel() + "/" + keen.value().canEnchant(new ItemStack(Items.IRON_SWORD)) + "/"
                        + keen.value().canEnchant(new ItemStack(Items.STICK)) + "/" + Enchantment.areCompatible(keen, sharpness));
            } catch (RuntimeException | LinkageError e) {
                results.put("dataEnchant", "error " + e);
            }
            try {
                // the entity tick events around a tick (the second Pre cancelled: no tick, no Post)
                net.minecraft.world.entity.decoration.ArmorStand stand = new net.minecraft.world.entity.decoration.ArmorStand(level, 0.5, 70, 0.5);
                int[] seen = new int[2];
                java.util.function.Consumer<net.neoforged.neoforge.event.tick.EntityTickEvent.Pre> pre = tickPre -> {
                    if (tickPre.getEntity() == stand && ++seen[0] == 2) {
                        tickPre.setCanceled(true);
                    }
                };
                java.util.function.Consumer<net.neoforged.neoforge.event.tick.EntityTickEvent.Post> post = tickPost -> {
                    if (tickPost.getEntity() == stand) {
                        seen[1]++;
                    }
                };
                NeoForge.EVENT_BUS.addListener(pre);
                NeoForge.EVENT_BUS.addListener(post);
                level.tickNonPassenger(stand);
                level.tickNonPassenger(stand);
                NeoForge.EVENT_BUS.unregister(pre);
                NeoForge.EVENT_BUS.unregister(post);
                results.put("entityTick", seen[0] + "/" + seen[1] + "/" + stand.tickCount);
                // the annotated subscribers (found by FML's scan of @EventBusSubscriber): one on the game bus, one on the mod bus
                results.put("busSubscriber", Subscribers.GAME.get() + "/" + Subscribers.MOD.get());
            } catch (RuntimeException | LinkageError e) {
                results.put("entityTick", "error " + e);
            }
            try {
                // the game's versions as the mod asks them (a mod checks its own data against them)
                results.put("gameVersions", net.neoforged.fml.ModList.get().getModContainerById("minecraft").orElseThrow().getModInfo().getVersion()
                        + "/" + net.minecraft.SharedConstants.getCurrentVersion().getName() + "/" + net.neoforged.fml.loading.FMLLoader.versionInfo().mcVersion()
                        + "/" + net.neoforged.fml.ModList.get().getModContainerById("neoforge").orElseThrow().getModInfo().getVersion().getMajorVersion());
            } catch (RuntimeException | LinkageError e) {
                results.put("gameVersions", "error " + e);
            }
            try {
                // a trade by villager type whose table lacks types (a mod's villager types): no error, no offer for those
                net.minecraft.world.entity.npc.VillagerTrades.ItemListing byType = new net.minecraft.world.entity.npc.VillagerTrades.EmeraldsForVillagerTypeItem(
                        1, 12, 30, java.util.Map.of(net.minecraft.world.entity.npc.VillagerType.PLAINS, Items.STICK));
                net.minecraft.world.entity.npc.Villager desert = new net.minecraft.world.entity.npc.Villager(EntityType.VILLAGER, level,
                        net.minecraft.world.entity.npc.VillagerType.DESERT);
                net.minecraft.world.entity.npc.Villager plains = new net.minecraft.world.entity.npc.Villager(EntityType.VILLAGER, level,
                        net.minecraft.world.entity.npc.VillagerType.PLAINS);
                net.minecraft.world.item.trading.MerchantOffer none = byType.getOffer(desert, level.random);
                net.minecraft.world.item.trading.MerchantOffer stick = byType.getOffer(plains, level.random);
                results.put("villagerTypeTrade", (none == null) + "/" + (stick == null ? "null" : stick.getResult().getItem() + "x" + stick.getResult().getCount()));
            } catch (RuntimeException | LinkageError e) {
                results.put("villagerTypeTrade", "error " + e);
            }
            try {
                // a chest's loot table (container_loot) kept over a save; fire resistance as the items have it
                ItemStack lootChest = new ItemStack(Items.CHEST);
                lootChest.set(DataComponents.CONTAINER_LOOT, new net.minecraft.world.item.component.SeededContainerLoot(
                        net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON, 5L));
                net.minecraft.world.item.component.SeededContainerLoot lootBack = savedBack(level, lootChest).get(DataComponents.CONTAINER_LOOT);
                results.put("containerLoot", lootBack == null ? "none" : lootBack.lootTable().location() + "/" + lootBack.seed());
                results.put("fireResistant", new ItemStack(Items.NETHERITE_SWORD).has(DataComponents.FIRE_RESISTANT) + "/"
                        + new ItemStack(Items.IRON_SWORD).has(DataComponents.FIRE_RESISTANT));
            } catch (RuntimeException | LinkageError e) {
                results.put("containerLoot", "error " + e);
            }
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

    /** subscribers FML finds by the annotation (a converted mod's must keep the annotation the lower FML scans for) */
    @net.neoforged.fml.common.EventBusSubscriber(modid = "ladder_test_b")
    public static final class Subscribers {
        static final java.util.concurrent.atomic.AtomicInteger GAME = new java.util.concurrent.atomic.AtomicInteger();

        @SubscribeEvent
        public static void serverStarting(net.neoforged.neoforge.event.server.ServerStartingEvent event) {
            GAME.incrementAndGet();
        }

        static final java.util.concurrent.atomic.AtomicInteger MOD = ModSubscribers.MOD;
    }

    @net.neoforged.fml.common.EventBusSubscriber(modid = "ladder_test_b", bus = net.neoforged.fml.common.EventBusSubscriber.Bus.MOD)
    public static final class ModSubscribers {
        static final java.util.concurrent.atomic.AtomicInteger MOD = new java.util.concurrent.atomic.AtomicInteger();

        @SubscribeEvent
        public static void commonSetup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event) {
            MOD.incrementAndGet();
        }
    }
}
