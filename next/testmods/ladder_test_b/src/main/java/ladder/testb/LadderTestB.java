/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 * Licensed under the MIT License (see LICENSE).
 */
package ladder.testb;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
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
            // handed to another mod in a chest (once: a restart finds it there)
            if (level.getBlockEntity(CHEST) instanceof ChestBlockEntity chest) {
                results.put("run", "again");
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
