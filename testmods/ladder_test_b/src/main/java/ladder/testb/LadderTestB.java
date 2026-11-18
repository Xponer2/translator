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

        results.put("rarity", new ItemStack(Items.ENCHANTED_GOLDEN_APPLE).get(DataComponents.RARITY) + "/" + new ItemStack(Items.STICK).get(DataComponents.RARITY));
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
