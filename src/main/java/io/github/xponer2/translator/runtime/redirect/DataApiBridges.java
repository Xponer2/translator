/*
 * Translator: the Minecraft 1.21.1 / NeoForge API on Minecraft 1.12.2
 * Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 *
 * Licensed under the PolyForm Noncommercial License 1.0.0 (see LICENSE or
 * https://polyformproject.org/licenses/noncommercial/1.0.0): free to use, change and share for any noncommercial
 * purpose, with credit to the author. This notice stays in this file and in every copy or changed version of it.
 *
 * Required Notice: Copyright (c) 2026 Xponer2 (https://github.com/Xponer2/translator)
 */
package io.github.xponer2.translator.runtime.redirect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import io.github.xponer2.translator.advancement.DataAdvancements;
import io.github.xponer2.translator.block.LootContextParams;
import io.github.xponer2.translator.block.LootParams;
import io.github.xponer2.translator.util.ItemLike;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.ICriterionInstance;
import net.minecraft.advancements.ICriterionTrigger;
import net.minecraft.advancements.critereon.AbstractCriterionInstance;
import net.minecraft.advancements.critereon.DistancePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.ItemPredicate;
import net.minecraft.advancements.critereon.LocationPredicate;
import net.minecraft.advancements.critereon.MobEffectsPredicate;
import net.minecraft.advancements.critereon.NBTPredicate;
import net.minecraft.advancements.critereon.RecipeUnlockedTrigger;
import net.minecraft.command.FunctionObject;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

/**
 * The 1.21 data API on 1.12.2's advancements, predicates, loot contexts and commands: what datagen code, the mods'
 * triggers and their codecs call.
 * <ul>
 * <li>1.21's {@code Advancement.Builder} is mutable and builds an {@code AdvancementHolder}; 1.12.2's is filled once
 * by its constructor. The calls set its fields (the 1.21 requirements strategy and telemetry flag kept aside), build a
 * 1.12.2 Advancement and hold it in the carried AdvancementHolder.</li>
 * <li>The predicate codecs (EntityPredicate.ADVANCEMENT_CODEC and CODEC, ItemPredicate.CODEC, ...) read 1.21's JSON
 * into 1.12.2's predicates (tags, several items, flags and biome lists as predicates of their own); an entity
 * predicate of a criterion is the carried ContextAwarePredicate whose loot conditions test 1.12.2's loot context.
 * Without them the static initializers of the mods' trigger instances were left empty.</li>
 * <li>A 1.21 criterion of a 1.12.2 trigger is that trigger's criterion; one of a mod's own trigger holds it.</li>
 * <li>{@code new LootContext.Builder(params).create(..)}: 1.12.2's loot context of the same parameters.</li>
 * <li>{@code server.getCommands()}: the carried Commands over the dispatcher of the mods' commands.</li>
 * </ul>
 */
public final class DataApiBridges {
	private static final Logger LOGGER = LogManager.getLogger("Translator Runtime");

	private DataApiBridges() {
	}

	// --- reflection -------------------------------------------------------------------------------------------------

	private static Class<?> carried(String name) throws ClassNotFoundException {
		return Class.forName(name, true, Launch.classLoader);
	}

	/** The public no-argument method of that name, or null when there is none or it throws. */
	@Nullable
	private static Object call(Object target, String name) {
		try {
			Method method = target.getClass().getMethod(name);
			method.setAccessible(true);
			return method.invoke(target);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}

	@Nullable
	private static Object field(Object target, String name) {
		for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
			try {
				Field field = type.getDeclaredField(name);
				field.setAccessible(true);
				return field.get(target);
			} catch (NoSuchFieldException e) {
				// its superclass
			} catch (ReflectiveOperationException | RuntimeException e) {
				return null;
			}
		}
		return null;
	}

	// --- Advancement.Builder ----------------------------------------------------------------------------------------

	/** 1.12.2's builder: its constructor and fields (parentId, parent, display, rewards, criteria, requirements). */
	private static final class Builders {
		static final Constructor<Advancement.Builder> NEW;
		static final Field PARENT_ID;
		static final Field PARENT;
		static final Field DISPLAY;
		static final Field REWARDS;
		static final Field CRITERIA;
		static final Field REQUIREMENTS;

		static {
			try {
				NEW = Advancement.Builder.class.getDeclaredConstructor(ResourceLocation.class, DisplayInfo.class, AdvancementRewards.class, Map.class,
						String[][].class);
				NEW.setAccessible(true);
				PARENT_ID = ObfuscationReflectionHelper.findField(Advancement.Builder.class, "field_192061_a");
				PARENT = ObfuscationReflectionHelper.findField(Advancement.Builder.class, "field_192062_b");
				DISPLAY = ObfuscationReflectionHelper.findField(Advancement.Builder.class, "field_192063_c");
				REWARDS = ObfuscationReflectionHelper.findField(Advancement.Builder.class, "field_192064_d");
				CRITERIA = ObfuscationReflectionHelper.findField(Advancement.Builder.class, "field_192065_e");
				REQUIREMENTS = ObfuscationReflectionHelper.findField(Advancement.Builder.class, "field_192066_f");
			} catch (ReflectiveOperationException e) {
				throw new IllegalStateException("1.12.2's Advancement.Builder isn't as expected", e);
			}
		}

		static Object get(Field field, Advancement.Builder builder) {
			try {
				return field.get(builder);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}

		static void set(Field field, Advancement.Builder builder, @Nullable Object value) {
			try {
				field.set(builder, value);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	/** What 1.21's builder has and 1.12.2's hasn't. */
	private static final class Extra {
		@Nullable
		Object strategy;
		@Nullable
		Object requirements;
		boolean telemetry;
	}

	private static final Map<Advancement.Builder, Extra> EXTRA = Collections.synchronizedMap(new WeakHashMap<>());
	/** The parent id of an advancement built without its parent (1.21 parent(id)). */
	private static final Map<Advancement, ResourceLocation> PARENT_IDS = Collections.synchronizedMap(new WeakHashMap<>());

	private static Extra extra(Advancement.Builder builder) {
		return EXTRA.computeIfAbsent(builder, b -> new Extra());
	}

	private static Advancement.Builder newBuilder() {
		try {
			return Builders.NEW.newInstance(null, null, AdvancementRewards.EMPTY, new LinkedHashMap<String, Criterion>(), null);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException(e);
		}
	}

	/** 1.21 {@code new Advancement.Builder()}. */
	public static Advancement.Builder newAdvancement_Builder() {
		return newBuilder();
	}

	/** 1.21 {@code Advancement.Builder.advancement()}: a builder whose advancement sends telemetry. */
	public static Advancement.Builder advancement() {
		Advancement.Builder builder = newBuilder();
		extra(builder).telemetry = true;
		return builder;
	}

	public static Advancement.Builder recipeAdvancement() {
		return newBuilder();
	}

	/** 1.21 {@code parent(AdvancementHolder)} and {@code parent(ResourceLocation)}. */
	public static Advancement.Builder parent(Advancement.Builder builder, Object parent) {
		ResourceLocation id;
		Advancement value = null;
		if (parent instanceof ResourceLocation location) {
			id = location;
		} else if (parent instanceof Advancement advancement) {
			value = advancement;
			id = advancement.getId();
		} else {
			id = (ResourceLocation) call(parent, "id");
			value = call(parent, "value") instanceof Advancement advancement ? advancement : null;
		}
		Builders.set(Builders.PARENT_ID, builder, id);
		Builders.set(Builders.PARENT, builder, value);
		return builder;
	}

	public static Advancement.Builder display(Advancement.Builder builder, ItemStack icon, ITextComponent title, ITextComponent description,
			@Nullable ResourceLocation background, FrameType frame, boolean showToast, boolean announceToChat, boolean hidden) {
		return display(builder, new DisplayInfo(icon, title, description, background, frame, showToast, announceToChat, hidden));
	}

	public static Advancement.Builder display(Advancement.Builder builder, ItemLike icon, ITextComponent title, ITextComponent description,
			@Nullable ResourceLocation background, FrameType frame, boolean showToast, boolean announceToChat, boolean hidden) {
		Item item = ItemLike.item(icon);
		return display(builder, new ItemStack(item), title, description, background, frame, showToast, announceToChat, hidden);
	}

	public static Advancement.Builder display(Advancement.Builder builder, DisplayInfo display) {
		Builders.set(Builders.DISPLAY, builder, display);
		return builder;
	}

	/** 1.21 {@code new DisplayInfo(icon, title, description, Optional background, type, toast, chat, hidden)}. */
	public static DisplayInfo newDisplayInfo(ItemStack icon, ITextComponent title, ITextComponent description, Optional<ResourceLocation> background,
			FrameType frame, boolean showToast, boolean announceToChat, boolean hidden) {
		return new DisplayInfo(icon, title, description, background == null ? null : background.orElse(null), frame, showToast, announceToChat, hidden);
	}

	public static Advancement.Builder rewards(Advancement.Builder builder, AdvancementRewards rewards) {
		Builders.set(Builders.REWARDS, builder, rewards == null ? AdvancementRewards.EMPTY : rewards);
		return builder;
	}

	/** 1.21 {@code rewards(AdvancementRewards.Builder)}: the carried builder's experience, loot tables, recipes and function. */
	public static Advancement.Builder rewards(Advancement.Builder builder, Object rewards) {
		return rewards(builder, rewardsOf(rewards));
	}

	private static AdvancementRewards rewardsOf(Object builder) {
		if (builder instanceof AdvancementRewards rewards) {
			return rewards;
		}
		Object experience = field(builder, "experience");
		return newAdvancementRewards(experience instanceof Integer xp ? xp : 0, listOf(field(builder, "loot")), listOf(field(builder, "recipes")),
				field(builder, "function") instanceof Optional<?> function ? function : Optional.empty());
	}

	/** An ImmutableList.Builder's list (or a collection's). */
	private static List<?> listOf(@Nullable Object value) {
		if (value instanceof com.google.common.collect.ImmutableList.Builder<?> builder) {
			return builder.build();
		}
		return value instanceof Collection<?> collection ? new ArrayList<>(collection) : Collections.emptyList();
	}

	/** 1.21 {@code new AdvancementRewards(experience, loot table keys, recipe ids, Optional function)}. */
	public static AdvancementRewards newAdvancementRewards(int experience, List<?> loot, List<?> recipes, Optional<?> function) {
		FunctionObject.CacheableFunction cacheable = FunctionObject.CacheableFunction.EMPTY;
		Object value = function == null ? null : function.orElse(null);
		ResourceLocation functionId = value instanceof ResourceLocation id ? id : value == null ? null : asLocation(call(value, "getId"));
		if (functionId != null) {
			cacheable = new FunctionObject.CacheableFunction(functionId);
		}
		return new AdvancementRewards(experience, locations(loot), locations(recipes), cacheable);
	}

	private static ResourceLocation[] locations(List<?> values) {
		var out = new ArrayList<ResourceLocation>();
		for (Object value : values) {
			ResourceLocation id = value instanceof ResourceLocation location ? location : asLocation(call(value, "location"));
			if (id != null) {
				out.add(id);
			}
		}
		return out.toArray(new ResourceLocation[0]);
	}

	@Nullable
	private static ResourceLocation asLocation(@Nullable Object value) {
		return value instanceof ResourceLocation id ? id : null;
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Criterion> criteriaOf(Advancement.Builder builder) {
		Object criteria = Builders.get(Builders.CRITERIA, builder);
		if (criteria instanceof LinkedHashMap<?, ?> || criteria instanceof java.util.HashMap<?, ?>) {
			return (Map<String, Criterion>) criteria;
		}
		// one 1.12.2 made (read from its JSON or the network): a map of its own to add to
		var copy = new LinkedHashMap<String, Criterion>();
		if (criteria instanceof Map<?, ?> map) {
			copy.putAll((Map<String, Criterion>) map);
		}
		Builders.set(Builders.CRITERIA, builder, copy);
		return copy;
	}

	public static Advancement.Builder addCriterion(Advancement.Builder builder, String key, Criterion criterion) {
		criteriaOf(builder).put(key, criterion);
		return builder;
	}

	/** 1.21 {@code requirements(AdvancementRequirements.Strategy)} and {@code requirements(AdvancementRequirements)}. */
	public static Advancement.Builder requirements(Advancement.Builder builder, Object requirements) {
		if (call(requirements, "requirements") instanceof List<?>) {
			extra(builder).requirements = requirements;
		} else {
			extra(builder).strategy = requirements;
		}
		return builder;
	}

	public static Advancement.Builder sendsTelemetryEvent(Advancement.Builder builder) {
		extra(builder).telemetry = true;
		return builder;
	}

	/** 1.21 {@code build(id)}: the carried AdvancementHolder of a 1.12.2 advancement. */
	public static Object build(Advancement.Builder builder, ResourceLocation id) {
		return holder(id, advancementOf(builder, id));
	}

	/** 1.21 {@code save(output, id)}: built, given to the output (datagen's), returned. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Object save(Advancement.Builder builder, Consumer output, String id) {
		Object holder = build(builder, new ResourceLocation(id));
		output.accept(holder);
		return holder;
	}

	/** NeoForge's {@code save(output, id, existingFileHelper)}. */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static Object save(Advancement.Builder builder, Consumer output, ResourceLocation id, Object existingFileHelper) {
		Object holder = build(builder, id);
		output.accept(holder);
		return holder;
	}

	private static Advancement advancementOf(Advancement.Builder builder, ResourceLocation id) {
		Map<String, Criterion> criteria = criteriaOf(builder);
		Extra extra = EXTRA.get(builder);
		String[][] requirements = (String[][]) Builders.get(Builders.REQUIREMENTS, builder);
		if (extra != null && extra.requirements != null) {
			requirements = requirementsArray(call(extra.requirements, "requirements"));
		} else if (requirements == null || extra != null && extra.strategy != null) {
			requirements = strategyRequirements(extra == null ? null : extra.strategy, criteria.keySet());
		}
		Object rewards = Builders.get(Builders.REWARDS, builder);
		Advancement parent = (Advancement) Builders.get(Builders.PARENT, builder);
		var advancement = new Advancement(id, parent, (DisplayInfo) Builders.get(Builders.DISPLAY, builder),
				rewards instanceof AdvancementRewards given ? given : AdvancementRewards.EMPTY, criteria, requirements);
		Object parentId = Builders.get(Builders.PARENT_ID, builder);
		if (parent == null && parentId instanceof ResourceLocation location) {
			PARENT_IDS.put(advancement, location);
		}
		return advancement;
	}

	/** The requirements of 1.21's strategy for the criteria: AND (each its own, the default), OR (one of them), or the strategy's own. */
	private static String[][] strategyRequirements(@Nullable Object strategy, Collection<String> names) {
		if (strategy != null) {
			try {
				Method create = strategy.getClass().getMethod("create", Collection.class);
				create.setAccessible(true);
				Object requirements = create.invoke(strategy, new ArrayList<>(names));
				String[][] out = requirementsArray(call(requirements, "requirements"));
				if (out != null) {
					return out;
				}
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				// AND
			}
		}
		String[][] out = new String[names.size()][];
		int i = 0;
		for (String name : names) {
			out[i++] = new String[] {name};
		}
		return out;
	}

	@Nullable
	private static String[][] requirementsArray(@Nullable Object lists) {
		if (!(lists instanceof List<?> list)) {
			return null;
		}
		String[][] out = new String[list.size()][];
		for (int i = 0; i < out.length; i++) {
			List<?> one = list.get(i) instanceof List<?> names ? names : Collections.emptyList();
			out[i] = one.stream().map(String::valueOf).toArray(String[]::new);
		}
		return out;
	}

	private static volatile Constructor<?> newHolder;

	/** The carried {@code AdvancementHolder(id, advancement)}; the advancement when it can't be made. */
	public static Object holder(ResourceLocation id, Advancement advancement) {
		try {
			Constructor<?> constructor = newHolder;
			if (constructor == null) {
				constructor = carried("net.minecraft.advancements.AdvancementHolder").getConstructor(ResourceLocation.class, Advancement.class);
				newHolder = constructor;
			}
			return constructor.newInstance(id, advancement);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return advancement;
		}
	}

	// --- Advancement (1.21 is a record) -----------------------------------------------------------------------------

	public static Optional<DisplayInfo> display(Advancement advancement) {
		return Optional.ofNullable(advancement.getDisplay());
	}

	public static Optional<ResourceLocation> parent(Advancement advancement) {
		Advancement parent = advancement.getParent();
		return parent != null ? Optional.of(parent.getId()) : Optional.ofNullable(PARENT_IDS.get(advancement));
	}

	public static AdvancementRewards rewards(Advancement advancement) {
		return advancement.getRewards();
	}

	public static Map<String, Criterion> criteria(Advancement advancement) {
		return advancement.getCriteria();
	}

	/** The carried AdvancementRequirements of 1.12.2's requirement arrays. */
	@Nullable
	public static Object requirements(Advancement advancement) {
		var lists = new ArrayList<List<String>>();
		for (String[] names : advancement.getRequirements()) {
			lists.add(java.util.Arrays.asList(names));
		}
		try {
			return carried("net.minecraft.advancements.AdvancementRequirements").getConstructor(List.class).newInstance(lists);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}

	public static boolean sendsTelemetryEvent(Advancement advancement) {
		return false;
	}

	public static boolean isRoot(Advancement advancement) {
		return advancement.getParent() == null && !PARENT_IDS.containsKey(advancement);
	}

	/** The record's name: its display title decorated as 1.12.2 shows it in chat, when it has a display. */
	public static Optional<ITextComponent> name(Advancement advancement) {
		return advancement.getDisplay() == null ? Optional.empty() : Optional.of(advancement.getDisplayText());
	}

	/** 1.21 {@code Advancement.name(holder)}: the chat name of the holder's advancement. */
	public static ITextComponent name(Object holder) {
		Object value = holder instanceof Advancement ? holder : call(holder, "value");
		if (value instanceof Advancement advancement) {
			return advancement.getDisplayText();
		}
		return new net.minecraft.util.text.TextComponentString(String.valueOf(call(holder, "id")));
	}

	// --- criteria -----------------------------------------------------------------------------------------------------

	/** A criterion of a mod's own (1.21) trigger: 1.12.2's criterion needs an instance with the trigger's id. */
	public static final class ModInstance extends AbstractCriterionInstance {
		final Object trigger;
		final Object instance;

		ModInstance(ResourceLocation id, Object trigger, Object instance) {
			super(id);
			this.trigger = trigger;
			this.instance = instance;
		}
	}

	/** 1.21 {@code new Criterion(trigger, instance)}. */
	public static Criterion newCriterion(Object trigger, Object instance) {
		if (instance instanceof ICriterionInstance own) {
			return new Criterion(own);
		}
		return new Criterion(new ModInstance(triggerId(trigger), trigger, instance));
	}

	/** 1.21 {@code trigger.createCriterion(instance)} of a 1.12.2 trigger. */
	public static Criterion createCriterion(ICriterionTrigger<?> trigger, Object instance) {
		return newCriterion(trigger, instance);
	}

	private static ResourceLocation triggerId(@Nullable Object trigger) {
		if (trigger instanceof ICriterionTrigger<?> own) {
			return own.getId();
		}
		try {
			ResourceLocation id = trigger == null ? null : io.github.xponer2.translator.util.BuiltInRegistries.TRIGGER_TYPES.getKey(trigger);
			if (id != null) {
				return id;
			}
		} catch (RuntimeException | LinkageError e) {
			// unnamed
		}
		return new ResourceLocation("translator", "mod_trigger");
	}

	/**
	 * 1.21 {@code criterion.trigger()}: the mod's own trigger; a 1.12.2 trigger only when it is a 1.21 one too (the
	 * caller casts to the carried CriterionTrigger), else null.
	 */
	@Nullable
	public static Object trigger(Criterion criterion) {
		ICriterionInstance instance = criterion.getCriterionInstance();
		if (instance instanceof ModInstance mod) {
			return mod.trigger;
		}
		Object trigger = instance == null ? null : CriteriaTriggers.get(instance.getId());
		return isCarried(trigger, "net.minecraft.advancements.CriterionTrigger") ? trigger : null;
	}

	/** 1.21 {@code criterion.triggerInstance()}: the mod's instance (a 1.12.2 one only when it is a 1.21 one too). */
	@Nullable
	public static Object triggerInstance(Criterion criterion) {
		ICriterionInstance instance = criterion.getCriterionInstance();
		if (instance instanceof ModInstance mod) {
			return mod.instance;
		}
		return isCarried(instance, "net.minecraft.advancements.CriterionTriggerInstance") ? instance : null;
	}

	private static boolean isCarried(@Nullable Object value, String type) {
		try {
			return value != null && carried(type).isInstance(value);
		} catch (ClassNotFoundException | LinkageError e) {
			return false;
		}
	}

	/** 1.21 {@code InventoryChangeTrigger.trigger(player, inventory, changed stack)}: 1.12.2's checks the whole inventory. */
	public static void trigger(ICriterionTrigger<?> trigger, EntityPlayerMP player, net.minecraft.entity.player.InventoryPlayer inventory, ItemStack changed) {
		if (trigger instanceof net.minecraft.advancements.critereon.InventoryChangeTrigger inventoryChange) {
			inventoryChange.trigger(player, inventory);
		}
	}

	/** 1.21 {@code EffectsChangedTrigger.trigger(player, source)}: 1.12.2's looks at the player's effects only. */
	public static void trigger(ICriterionTrigger<?> trigger, EntityPlayerMP player, @Nullable Entity source) {
		if (trigger instanceof net.minecraft.advancements.critereon.EffectsChangedTrigger effects) {
			effects.trigger(player);
		}
	}

	/** 1.21 {@code listener.trigger()}: the listener's instance when it is a 1.21 one (a mod's), else null. */
	@Nullable
	public static Object trigger(ICriterionTrigger.Listener<?> listener) {
		ICriterionInstance instance = listener.getCriterionInstance();
		if (instance instanceof ModInstance mod) {
			return mod.instance;
		}
		return isCarried(instance, "net.minecraft.advancements.CriterionTriggerInstance") ? instance : null;
	}

	/** 1.21 {@code NbtPredicate.tag()}: the compound 1.12.2's predicate tests (null for ANY). */
	@Nullable
	public static net.minecraft.nbt.NBTTagCompound tag(NBTPredicate predicate) {
		try {
			Field field = NbtTag.FIELD;
			return field == null ? null : (net.minecraft.nbt.NBTTagCompound) field.get(predicate);
		} catch (IllegalAccessException | RuntimeException e) {
			return null;
		}
	}

	private static final class NbtTag {
		static final Field FIELD = find();

		@Nullable
		private static Field find() {
			try {
				return ObfuscationReflectionHelper.findField(NBTPredicate.class, "field_193480_b");
			} catch (RuntimeException e) {
				return null;
			}
		}
	}

	/** 1.21 {@code server.getAdvancements().getAllAdvancements()}: the holders of 1.12.2's advancements. */
	public static Collection<Object> getAllAdvancements(net.minecraft.advancements.AdvancementManager manager) {
		var out = new ArrayList<Object>();
		for (Advancement advancement : manager.getAdvancements()) {
			out.add(holder(advancement.getId(), advancement));
		}
		return out;
	}

	/** 1.21 {@code RecipeUnlockedTrigger.unlocked(recipe id)}: 1.12.2's criterion for the crafting recipe of that id. */
	public static Criterion unlocked(ResourceLocation recipe) {
		return new Criterion(new RecipeUnlockedTrigger.Instance(CraftingManager.getRecipe(recipe)));
	}

	// --- ingredients --------------------------------------------------------------------------------------------------

	/**
	 * 1.21 {@code ingredient.getValues()} (and its {@code values} field): one carried 1.21 {@code Ingredient.ItemValue}
	 * per stack 1.12.2's ingredient matches (Create's blueprint filters, Mekanism's and Sophisticated Core's readers).
	 */
	@Nullable
	public static Object getValues(net.minecraft.item.crafting.Ingredient ingredient) {
		try {
			Class<?> value = carried("net.minecraft.world.item.crafting.Ingredient$Value");
			Constructor<?> item = carried("net.minecraft.world.item.crafting.Ingredient$ItemValue").getConstructor(ItemStack.class);
			ItemStack[] stacks = ingredient.getMatchingStacks();
			Object values = java.lang.reflect.Array.newInstance(value, stacks.length);
			for (int i = 0; i < stacks.length; i++) {
				java.lang.reflect.Array.set(values, i, item.newInstance(stacks[i].copy()));
			}
			return values;
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}

	@Nullable
	public static Object values(net.minecraft.item.crafting.Ingredient ingredient) {
		return getValues(ingredient);
	}

	// --- loot contexts ------------------------------------------------------------------------------------------------

	/** 1.21 {@code new LootContext.Builder(params)}: 1.12.2's builder with the world, entity, player, damage and luck of the params. */
	public static LootContext.Builder newLootContext_Builder(LootParams params) {
		World level = params.getLevel();
		WorldServer world = level instanceof WorldServer server ? server : overworld();
		var builder = new LootContext.Builder(world).withLuck(params.getLuck());
		Entity entity = params.getOptionalParameter(LootContextParams.THIS_ENTITY);
		if (entity != null) {
			builder.withLootedEntity(entity);
		}
		EntityPlayer player = params.getOptionalParameter(LootContextParams.LAST_DAMAGE_PLAYER);
		if (player == null && entity instanceof EntityPlayer self) {
			player = self;
		}
		if (player != null) {
			builder.withPlayer(player);
		}
		DamageSource source = params.getOptionalParameter(LootContextParams.DAMAGE_SOURCE);
		if (source != null) {
			builder.withDamageSource(source);
		}
		return builder;
	}

	@Nullable
	private static WorldServer overworld() {
		MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
		return server == null ? null : server.getWorld(0);
	}

	/** 1.21 {@code create(Optional random sequence)}. */
	public static LootContext create(LootContext.Builder builder, Optional<?> sequence) {
		return builder.build();
	}

	/** 1.21 {@code withOptionalRandomSeed(seed)}: 1.12.2 takes the random where a table is rolled. */
	public static LootContext.Builder withOptionalRandomSeed(LootContext.Builder builder, long seed) {
		return builder;
	}

	public static LootContext.Builder withQueriedLootTableId(LootContext.Builder builder, ResourceLocation id) {
		return builder;
	}

	/** 1.21's direct attacker target: 1.12.2's killer (the direct one when there is no other). */
	public static final LootContext.EntityTarget DIRECT_ATTACKER = LootContext.EntityTarget.KILLER;
	public static final LootContext.EntityTarget ATTACKER = LootContext.EntityTarget.KILLER;
	public static final LootContext.EntityTarget ATTACKING_PLAYER = LootContext.EntityTarget.KILLER_PLAYER;

	/** 1.21 {@code target.getParam()}: the loot parameter of the target. */
	public static LootContextParams.Param<?> getParam(LootContext.EntityTarget target) {
		switch (target) {
			case KILLER:
				return LootContextParams.ATTACKING_ENTITY;
			case KILLER_PLAYER:
				return LootContextParams.LAST_DAMAGE_PLAYER;
			default:
				return LootContextParams.THIS_ENTITY;
		}
	}

	// --- commands -----------------------------------------------------------------------------------------------------

	private static volatile Object commands;
	private static volatile Object commandsDispatcher;

	/** 1.21 {@code server.getCommands()}: the carried Commands over the mods' dispatcher (its vanilla tree isn't built). */
	@Nullable
	public static Object getCommands(MinecraftServer server) {
		Object dispatcher = io.github.xponer2.translator.runtime.neoforge.ModCommands.dispatcher(server);
		if (dispatcher == null) {
			return null;
		}
		Object made = commands;
		if (made != null && commandsDispatcher == dispatcher) {
			return made;
		}
		try {
			Class<?> type = carried("net.minecraft.commands.Commands");
			Class<?> unsafeType = Class.forName("sun.misc.Unsafe");
			Field unsafeField = unsafeType.getDeclaredField("theUnsafe");
			unsafeField.setAccessible(true);
			made = unsafeType.getMethod("allocateInstance", Class.class).invoke(unsafeField.get(null), type);
			Field field = type.getDeclaredField("dispatcher");
			field.setAccessible(true);
			field.set(made, dispatcher);
			commands = made;
			commandsDispatcher = dispatcher;
			return made;
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			LOGGER.warn("1.21 server.getCommands(): no Commands over the mods' dispatcher: {}", e.toString());
			return null;
		}
	}

	/**
	 * 1.21 {@code commands.performPrefixedCommand(source, line)}: a command of the mods through their dispatcher, any
	 * other through 1.12.2's command manager as the source's entity (the server when it has none).
	 */
	public static void performPrefixedCommand(Object commands, Object source, String line) {
		String command = line.startsWith("/") ? line.substring(1) : line;
		String root = command.split(" ", 2)[0];
		Object dispatcher = commands == null ? null : field(commands, "dispatcher");
		try {
			Object node = dispatcher == null ? null : call(call(dispatcher, "getRoot"), "getChild", root);
			if (node != null) {
				for (Method method : dispatcher.getClass().getMethods()) {
					if ("execute".equals(method.getName()) && method.getParameterCount() == 2 && method.getParameterTypes()[0] == String.class) {
						method.invoke(dispatcher, command, source);
						return;
					}
				}
			}
		} catch (java.lang.reflect.InvocationTargetException e) {
			LOGGER.debug("Command /{} failed: {}", command, String.valueOf(e.getCause()));
			return;
		} catch (ReflectiveOperationException | RuntimeException e) {
			LOGGER.debug("Command /{} couldn't run through the mods' dispatcher: {}", command, e.toString());
		}
		MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
		if (server == null) {
			return;
		}
		Object entity = source == null ? null : field(source, "entity");
		ICommandSender sender = entity instanceof ICommandSender own ? own : server;
		server.getCommandManager().executeCommand(sender, command);
	}

	@Nullable
	private static Object call(@Nullable Object target, String name, Object argument) {
		if (target == null) {
			return null;
		}
		for (Method method : target.getClass().getMethods()) {
			if (name.equals(method.getName()) && method.getParameterCount() == 1 && method.getParameterTypes()[0].isInstance(argument)) {
				try {
					method.setAccessible(true);
					return method.invoke(target, argument);
				} catch (ReflectiveOperationException | RuntimeException e) {
					return null;
				}
			}
		}
		return null;
	}

	// --- predicates ---------------------------------------------------------------------------------------------------

	/** The 1.21 JSON each decoded value was read from (encoding gives it back). */
	private static final Map<Object, JsonElement> SOURCES = Collections.synchronizedMap(new WeakHashMap<>());

	/** A codec reading 1.21's JSON (from any ops) into a value; encoding gives back the JSON it was read from. */
	static <A> Codec<A> jsonCodec(String what, Function<JsonElement, A> decoder) {
		return new Codec<A>() {
			@Override
			public <T> DataResult<Pair<A, T>> decode(DynamicOps<T> ops, T input) {
				try {
					JsonElement json = ops.convertTo(JsonOps.INSTANCE, input);
					A value = decoder.apply(json);
					if (value == null) {
						return DataResult.error("Not a " + what + ": " + json);
					}
					SOURCES.put(value, json);
					return DataResult.success(Pair.of(value, ops.empty()));
				} catch (RuntimeException e) {
					return DataResult.error("Not a " + what + ": " + e);
				}
			}

			@Override
			public <T> DataResult<T> encode(A input, DynamicOps<T> ops, T prefix) {
				JsonElement json = SOURCES.get(input);
				return DataResult.success(JsonOps.INSTANCE.convertTo(ops, json != null ? json : new JsonObject()));
			}

			@Override
			public String toString() {
				return "Translator " + what + " codec";
			}
		};
	}

	/** A 1.21 entity predicate (JSON) as 1.12.2's, with what 1.12.2's lacks (tags, flags, biome lists) checked on its own. */
	static EntityPredicate entityPredicate(@Nullable JsonElement json) {
		if (json == null || json.isJsonNull() || json.isJsonObject() && json.getAsJsonObject().size() == 0) {
			return EntityPredicate.ANY;
		}
		JsonObject in = json.getAsJsonObject();
		var out = new JsonObject();
		var checks = new ArrayList<Predicate<Entity>>();
		if (in.has("type")) {
			Set<ResourceLocation> types = entityTypes(in.get("type"));
			if (types.isEmpty()) {
				return Matching.NEVER;
			}
			checks.add(entity -> types.contains(EntityList.getKey(entity)));
		}
		for (String same : new String[] {"distance", "effects", "nbt"}) {
			if (in.has(same)) {
				out.add(same, in.get(same));
			}
		}
		if (in.has("location")) {
			JsonObject location = locationJson(in.get("location"), checks);
			if (location == null) {
				return Matching.NEVER;
			}
			out.add("location", location);
		}
		if (in.has("flags") && in.get("flags").isJsonObject()) {
			for (Map.Entry<String, JsonElement> flag : in.getAsJsonObject("flags").entrySet()) {
				Predicate<Entity> test = flag(flag.getKey());
				if (test != null) {
					boolean wanted = flag.getValue().getAsBoolean();
					checks.add(entity -> test.test(entity) == wanted);
				}
			}
		}
		EntityPredicate base;
		try {
			base = out.size() == 0 ? EntityPredicate.ANY : EntityPredicate.deserialize(out);
		} catch (RuntimeException e) {
			return Matching.NEVER;
		}
		return checks.isEmpty() ? base : new Matching(base, checks);
	}

	/** The 1.12.2 entity ids of a 1.21 entity type predicate: an id, a list or a #tag. */
	private static Set<ResourceLocation> entityTypes(JsonElement type) {
		var out = new java.util.HashSet<ResourceLocation>();
		var names = new ArrayList<String>();
		if (type.isJsonArray()) {
			for (JsonElement one : type.getAsJsonArray()) {
				names.add(one.getAsString());
			}
		} else if (type.isJsonPrimitive()) {
			names.add(type.getAsString());
		}
		for (String name : names) {
			if (name.startsWith("#")) {
				out.addAll(io.github.xponer2.translator.tags.TagManager.ids("entity_type", new ResourceLocation(name.substring(1))));
			} else {
				out.add(new ResourceLocation(name));
			}
		}
		return out;
	}

	@Nullable
	private static Predicate<Entity> flag(String name) {
		switch (name) {
			case "is_on_fire":
				return Entity::isBurning;
			case "is_sneaking":
				return Entity::isSneaking;
			case "is_sprinting":
				return Entity::isSprinting;
			case "is_swimming":
				return Entity::isInWater;
			case "is_on_ground":
				return entity -> entity.onGround;
			case "is_baby":
				return entity -> entity instanceof EntityLivingBase living && living.isChild();
			case "is_flying":
				return entity -> entity instanceof EntityPlayer player && player.capabilities.isFlying;
			default:
				return null;
		}
	}

	/**
	 * A 1.21 location predicate as 1.12.2's JSON (position, the one biome, the dimension); several biomes are a check
	 * of their own. Null when it can't hold here (a dimension or biomes 1.12.2 hasn't).
	 */
	@Nullable
	private static JsonObject locationJson(JsonElement json, List<Predicate<Entity>> checks) {
		var out = new JsonObject();
		if (!json.isJsonObject()) {
			return out;
		}
		JsonObject in = json.getAsJsonObject();
		if (in.has("position")) {
			out.add("position", in.get("position"));
		}
		if (in.has("dimension")) {
			String dimension = DataAdvancements.dimension(in.get("dimension").getAsString());
			if (dimension == null) {
				return null;
			}
			out.addProperty("dimension", dimension);
		}
		if (in.has("biomes")) {
			List<String> biomes = DataAdvancements.biomes(in.get("biomes"));
			if (biomes.isEmpty()) {
				return null;
			}
			if (biomes.size() == 1) {
				out.addProperty("biome", biomes.get(0));
			} else {
				var ids = new java.util.HashSet<ResourceLocation>();
				for (String id : biomes) {
					ids.add(new ResourceLocation(id));
				}
				checks.add(entity -> {
					Biome biome = entity.world.getBiome(entity.getPosition());
					return biome != null && ids.contains(biome.getRegistryName());
				});
			}
		}
		return out;
	}

	/** A 1.12.2 entity predicate with checks 1.12.2's JSON can't say (or one that never holds). */
	public static final class Matching extends EntityPredicate {
		static final Matching NEVER = new Matching(EntityPredicate.ANY, Collections.singletonList(entity -> false));
		private final EntityPredicate base;
		private final List<Predicate<Entity>> checks;

		Matching(EntityPredicate base, List<Predicate<Entity>> checks) {
			super(null, DistancePredicate.ANY, LocationPredicate.ANY, MobEffectsPredicate.ANY, NBTPredicate.ANY);
			this.base = base;
			this.checks = checks;
		}

		@Override
		public boolean test(EntityPlayerMP player, @Nullable Entity entity) {
			if (entity == null) {
				return false;
			}
			for (Predicate<Entity> check : this.checks) {
				if (!check.test(entity)) {
					return false;
				}
			}
			return this.base.test(player, entity);
		}
	}

	/** Whether the entity predicate holds for the target of a 1.12.2 loot context (the player for distances). */
	static boolean test(EntityPredicate predicate, LootContext context, LootContext.EntityTarget target) {
		Entity entity = context.getEntity(target);
		Entity killer = context.getKillerPlayer();
		EntityPlayerMP player = killer instanceof EntityPlayerMP mp ? mp : entity instanceof EntityPlayerMP self ? self : null;
		if (predicate == EntityPredicate.ANY) {
			return true;
		}
		try {
			return player != null ? predicate.test(player, entity) : entity != null && predicate instanceof Matching && predicate.test(null, entity);
		} catch (RuntimeException e) {
			return false;
		}
	}

	private static volatile Class<?> conditionType;
	private static volatile Constructor<?> newContextPredicate;

	/** A carried 1.21 LootItemCondition that tests 1.12.2's loot context. */
	@Nullable
	static Object condition(Predicate<LootContext> test, String name) {
		try {
			Class<?> type = conditionType;
			if (type == null) {
				type = carried("net.minecraft.world.level.storage.loot.predicates.LootItemCondition");
				conditionType = type;
			}
			return Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, (proxy, method, args) -> {
				switch (method.getName()) {
					case "test":
						return args != null && args.length == 1 && args[0] instanceof LootContext context && test.test(context);
					case "getReferencedContextParams":
						return Collections.emptySet();
					case "hashCode":
						return System.identityHashCode(proxy);
					case "equals":
						return proxy == args[0];
					case "toString":
						return "Translator loot condition " + name;
					default:
						return method.getReturnType() == boolean.class ? Boolean.FALSE : null;
				}
			});
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}

	/** The carried ContextAwarePredicate of the conditions (all must hold); null when it can't be made. */
	@Nullable
	static Object contextPredicate(List<Object> conditions) {
		try {
			Constructor<?> constructor = newContextPredicate;
			if (constructor == null) {
				constructor = carried("net.minecraft.advancements.critereon.ContextAwarePredicate").getConstructor(List.class);
				newContextPredicate = constructor;
			}
			return constructor.newInstance(conditions);
		} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
			return null;
		}
	}

	/** A condition on the context's entity (1.21 entity_properties). */
	@Nullable
	static Object entityCondition(EntityPredicate predicate, LootContext.EntityTarget target) {
		return condition(context -> test(predicate, context, target), "entity_properties");
	}

	/** 1.21 loot conditions (JSON) as conditions on 1.12.2's context; one this can't check never holds. */
	@Nullable
	static Object lootCondition(JsonElement json) {
		Predicate<LootContext> test = conditionTest(json);
		return condition(test, json.isJsonObject() && json.getAsJsonObject().has("condition") ? json.getAsJsonObject().get("condition").getAsString() : "?");
	}

	private static Predicate<LootContext> conditionTest(JsonElement json) {
		if (json.isJsonArray()) {
			var all = new ArrayList<Predicate<LootContext>>();
			for (JsonElement one : json.getAsJsonArray()) {
				all.add(conditionTest(one));
			}
			return context -> all.stream().allMatch(test -> test.test(context));
		}
		if (!json.isJsonObject()) {
			return context -> false;
		}
		JsonObject in = json.getAsJsonObject();
		String type = in.has("condition") ? in.get("condition").getAsString() : "";
		switch (type.startsWith("minecraft:") ? type.substring("minecraft:".length()) : type) {
			case "entity_properties": {
				EntityPredicate predicate = entityPredicate(in.get("predicate"));
				LootContext.EntityTarget target = target(in.has("entity") ? in.get("entity").getAsString() : "this");
				return context -> test(predicate, context, target);
			}
			case "inverted": {
				Predicate<LootContext> term = conditionTest(in.get("term"));
				return term.negate();
			}
			case "any_of":
			case "alternative": {
				var any = new ArrayList<Predicate<LootContext>>();
				for (JsonElement one : in.getAsJsonArray("terms")) {
					any.add(conditionTest(one));
				}
				return context -> any.stream().anyMatch(test -> test.test(context));
			}
			case "all_of":
				return conditionTest(in.get("terms"));
			case "random_chance": {
				float chance = in.has("chance") && in.get("chance").isJsonPrimitive() ? in.get("chance").getAsFloat() : 0.0F;
				return context -> Math.random() < chance;
			}
			case "killed_by_player":
				return context -> context.getKillerPlayer() != null;
			default:
				return context -> false;
		}
	}

	private static LootContext.EntityTarget target(String name) {
		switch (name) {
			case "attacker":
			case "direct_attacker":
				return LootContext.EntityTarget.KILLER;
			case "attacking_player":
				return LootContext.EntityTarget.KILLER_PLAYER;
			default:
				return LootContext.EntityTarget.THIS;
		}
	}

	/** {@code EntityPredicate}'s 1.21 statics. */
	public static final class EntityPredicates {
		/** 1.21's predicate of a criterion's player or entity: a list of loot conditions, or an entity predicate (wrapped). */
		public static final Codec<Object> ADVANCEMENT_CODEC = jsonCodec("entity predicate of a criterion", json -> {
			if (json.isJsonArray()) {
				var conditions = new ArrayList<Object>();
				for (JsonElement one : json.getAsJsonArray()) {
					Object condition = lootCondition(one);
					if (condition == null) {
						return null;
					}
					conditions.add(condition);
				}
				return contextPredicate(conditions);
			}
			return wrap(entityPredicate(json));
		});
		@SuppressWarnings({"unchecked", "rawtypes"})
		public static final Codec<EntityPredicate> CODEC = (Codec) jsonCodec("entity predicate", DataApiBridges::entityPredicate);

		private EntityPredicates() {
		}

		/**
		 * 1.21 {@code EntityPredicate.wrap(..)}: a predicate (or its builder) as the carried ContextAwarePredicate of a
		 * criterion; an Optional of one, an array of builders a list of them.
		 */
		@Nullable
		public static Object wrap(@Nullable Object predicate) {
			if (predicate instanceof Optional<?> optional) {
				return optional.map(EntityPredicates::wrap);
			}
			if (predicate instanceof Object[] many) {
				var out = new ArrayList<Object>();
				for (Object one : many) {
					out.add(wrap(one));
				}
				return out;
			}
			if (predicate == null || predicate == EntityPredicate.ANY) {
				return contextPredicate(new ArrayList<>());
			}
			if (predicate instanceof EntityPredicate entity) {
				Object condition = entityCondition(entity, LootContext.EntityTarget.THIS);
				return condition == null ? null : contextPredicate(new ArrayList<>(Collections.singletonList(condition)));
			}
			// a builder (the carried EntityPredicate.Builder)
			return wrap(call(predicate, "build"));
		}

		/** 1.21 {@code EntityPredicate.createContext(player, entity)}: 1.12.2's loot context of the entity, with the player. */
		public static LootContext createContext(EntityPlayerMP player, Entity entity) {
			return new LootContext.Builder(player.getServerWorld()).withLootedEntity(entity).withPlayer(player).build();
		}

		public static boolean matches(EntityPredicate predicate, EntityPlayerMP player, @Nullable Entity entity) {
			return predicate.test(player, entity);
		}
	}

	/** A 1.12.2 item predicate holding for any of several (1.21's item lists and tags). */
	public static final class AnyItem extends ItemPredicate {
		static final AnyItem NONE = new AnyItem(Collections.emptyList());
		private final List<ItemPredicate> any;

		AnyItem(List<ItemPredicate> any) {
			this.any = any;
		}

		@Override
		public boolean test(ItemStack stack) {
			for (ItemPredicate one : this.any) {
				if (one.test(stack)) {
					return true;
				}
			}
			return false;
		}
	}

	/** A 1.21 item predicate (JSON) as 1.12.2's: one per item of its list or tag; none here, one that never holds. */
	static ItemPredicate itemPredicate(JsonElement json) {
		var list = new ArrayList<ItemPredicate>();
		for (JsonObject one : DataAdvancements.itemPredicates(json)) {
			list.add(ItemPredicate.deserialize(one));
		}
		return list.isEmpty() ? AnyItem.NONE : list.size() == 1 ? list.get(0) : new AnyItem(list);
	}

	/** {@code ItemPredicate.CODEC}. */
	public static final class ItemPredicates {
		@SuppressWarnings({"unchecked", "rawtypes"})
		public static final Codec<ItemPredicate> CODEC = (Codec) jsonCodec("item predicate", DataApiBridges::itemPredicate);

		private ItemPredicates() {
		}
	}

	/** A 1.12.2 location predicate that never holds (a 1.21 one naming a place 1.12.2 hasn't). */
	public static final class NoLocation extends LocationPredicate {
		static final NoLocation INSTANCE = new NoLocation();

		NoLocation() {
			super(net.minecraft.advancements.critereon.MinMaxBounds.UNBOUNDED, net.minecraft.advancements.critereon.MinMaxBounds.UNBOUNDED,
					net.minecraft.advancements.critereon.MinMaxBounds.UNBOUNDED, null, null, null);
		}

		@Override
		public boolean test(WorldServer world, double x, double y, double z) {
			return false;
		}

		@Override
		public boolean test(WorldServer world, float x, float y, float z) {
			return false;
		}
	}

	/** {@code LocationPredicate.CODEC}: position, dimension and one biome (several or none here: never). */
	public static final class LocationPredicates {
		@SuppressWarnings({"unchecked", "rawtypes"})
		public static final Codec<LocationPredicate> CODEC = (Codec) jsonCodec("location predicate", json -> {
			var checks = new ArrayList<Predicate<Entity>>();
			JsonObject location = locationJson(json, checks);
			return location == null || !checks.isEmpty() ? NoLocation.INSTANCE : LocationPredicate.deserialize(location);
		});

		private LocationPredicates() {
		}
	}

	/** {@code MobEffectsPredicate.CODEC} (1.21's JSON is 1.12.2's: effect id to amplifier, duration, ambient, visible). */
	public static final class MobEffectsPredicates {
		@SuppressWarnings({"unchecked", "rawtypes"})
		public static final Codec<MobEffectsPredicate> CODEC = (Codec) jsonCodec("mob effects predicate", MobEffectsPredicate::deserialize);

		private MobEffectsPredicates() {
		}
	}

	/** {@code DistancePredicate.CODEC} (1.21's JSON is 1.12.2's). */
	public static final class DistancePredicates {
		@SuppressWarnings({"unchecked", "rawtypes"})
		public static final Codec<DistancePredicate> CODEC = (Codec) jsonCodec("distance predicate", DistancePredicate::deserialize);

		private DistancePredicates() {
		}
	}
}
