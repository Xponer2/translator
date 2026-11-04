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
package io.github.xponer2.translator.runtime;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import xyz.wagyourtail.jvmdg.shade.asm.Type;
import xyz.wagyourtail.jvmdg.shade.asm.commons.Remapper;

/**
 * Names of a NeoForge 1.21.1 mod's bytecode on 1.12.2: classes through the table (Translator classes first), members
 * of 1.12.2 classes to their MCP names. A member is renamed only when the 1.12.2 class (or a supertype) really has it
 * with the remapped descriptor, so the mod's own methods keep their names. Translator classes keep the 1.21 names.
 */
final class ModRemapper extends Remapper {
	private static final String TRANSLATOR = "io/github/xponer2/translator/";
	private final RemapTable table;
	/** the mod's classes by their (unchanged) names. */
	private final Map<String, ClassInfo> mod;
	private final TargetClasses target;
	/** 1.21 classes without a 1.12.2 / Translator class: what the mod can't use yet (for the report). */
	final Map<String, Integer> missingClasses = new TreeMap<>();

	ModRemapper(RemapTable table, Map<String, ClassInfo> mod, TargetClasses target) {
		super(xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ASM9);
		this.table = table;
		this.mod = mod;
		this.target = target;
	}

	/** The classes a mixin class of the jar is applied to (1.21 names), empty for other classes. */
	java.util.List<String> mixinTargets(String internalName) {
		ClassInfo info = this.mod.get(internalName);
		return info == null ? java.util.Collections.emptyList() : info.mixinTargets;
	}

	/**
	 * A method name without a descriptor, as a mixin writes it ({@code method = "tick"}): its 1.12.2 name when the
	 * table gives one name for it in the class (or its supertypes in the table), else the name.
	 */
	String mapBareMethodName(String owner, String name) {
		var todo = new ArrayDeque<String>();
		var seen = new HashSet<String>();
		todo.add(owner);
		while (!todo.isEmpty()) {
			String current = todo.poll();
			if (!seen.add(current)) {
				continue;
			}
			Map<String, String> members = this.table.methods.get(current);
			String found = null;
			if (members != null) {
				for (var entry : members.entrySet()) {
					if (entry.getKey().startsWith(name + "(")) {
						if (found != null && !found.equals(entry.getValue())) {
							return name;
						}
						found = entry.getValue();
					}
				}
			}
			if (found != null) {
				return found;
			}
		}
		return name;
	}

	/** A class of the jar being converted (its name stays). */
	boolean isOwn(String internalName) {
		return this.mod.containsKey(internalName);
	}

	private static boolean isPlatform(String name) {
		return name.startsWith("java/") || name.startsWith("javax/") || name.startsWith("sun/") || name.startsWith("jdk/");
	}

	@Override
	public String map(String internalName) {
		if (this.mod.containsKey(internalName) || isPlatform(internalName)) {
			return internalName;
		}
		String mapped = this.table.classes.get(internalName);
		if (mapped != null && (!mapped.startsWith(TRANSLATOR) || this.target.exists(mapped))) {
			return mapped;
		}
		// a Translator class the table names but the Translator doesn't have (yet): the 1.21.1 class is carried
		int dollar = internalName.lastIndexOf('$');
		if (dollar > 0 && !Character.isDigit(internalName.charAt(dollar + 1))) {
			// (not an anonymous class: BlockPos$1 of 1.21 and BlockPos$1 of 1.12.2 have nothing in common)
			// a nested class of a mapped class, when the target has it under the same name
			String outer = internalName.substring(0, dollar);
			String mappedOuter = this.map(outer);
			ClassInfo nested = mappedOuter.equals(outer) ? null : this.target.get(mappedOuter + internalName.substring(dollar));
			// only a public one: a package-private 1.12.2 class of that name (GameRules$Value) is another thing
			if (nested != null && (nested.access & xyz.wagyourtail.jvmdg.shade.asm.Opcodes.ACC_PUBLIC) != 0) {
				return mappedOuter + internalName.substring(dollar);
			}
		}
		// many NeoForge classes are the 1.12.2 Forge classes under a new package (items.IItemHandler, ...)
		String forge = internalName.startsWith("net/neoforged/neoforge/") ? "net/minecraftforge/" + internalName.substring(23)
				: internalName.startsWith("net/neoforged/fml/") ? "net/minecraftforge/fml/" + internalName.substring(18) : null;
		if (forge != null && this.target.exists(forge)) {
			return forge;
		}
		if (!this.target.exists(internalName) && (internalName.startsWith("net/minecraft/") || internalName.startsWith("com/mojang/")
				|| internalName.startsWith("net/neoforged/"))) {
			this.missingClasses.merge(internalName, 1, Integer::sum);
		}
		return internalName;
	}

	/**
	 * A class name in a string (reflection: {@code Class.forName("org.lwjgl.stb.STBImageResize")}, JourneyMap's image
	 * scaling) is the class the table maps it to, as in the code. Only exact table entries: other text stays.
	 */
	@Override
	public Object mapValue(Object value) {
		if (value instanceof String text && text.indexOf('.') > 0 && CLASS_NAME.matcher(text).matches()) {
			String mapped = this.table.classes.get(text.replace('.', '/'));
			if (mapped != null) {
				return mapped.replace('/', '.');
			}
		}
		return super.mapValue(value);
	}

	private static final java.util.regex.Pattern CLASS_NAME = java.util.regex.Pattern.compile("[A-Za-z_$][\\w$]*(\\.[A-Za-z_$][\\w$]*)+");

	@Override
	public String mapMethodName(String owner, String name, String descriptor) {
		if (name.startsWith("<") || owner.startsWith("[")) {
			return name;
		}
		String mapped = this.member(owner, name, descriptor, true);
		if (!mapped.equals(name) && this.declaredIn121(owner, mapped, descriptor)) {
			// the class has both in 1.21 (Container.getItem and ItemStackHandler.getStackInSlot): renaming one to the
			// other would merge two methods (getItem calling getStackInSlot then calls itself). Keep the 1.21 name; the
			// 1.12.2 interface method is implemented by the method that has that name already.
			return name;
		}
		return mapped;
	}

	/**
	 * Whether the class already has a method of that name in 1.21: a mod class of its superclass chain declares it, or
	 * the mod's 1.21 code calls it on a class of that chain (InventoryCosArmor.getItem calls the getStackInSlot it
	 * inherits from NeoForge's ItemStackHandler).
	 */
	private boolean declaredIn121(String owner, String name, String desc) {
		var chain = new java.util.ArrayList<String>();
		var mods = new java.util.ArrayList<ClassInfo>();
		for (String current = owner; current != null && !chain.contains(current); ) {
			chain.add(current);
			ClassInfo modClass = this.mod.get(current);
			if (modClass == null) {
				break;
			}
			if (modClass.methods.contains(name + " " + desc)) {
				return true;
			}
			mods.add(modClass);
			current = modClass.superName;
		}
		for (ClassInfo modClass : mods) {
			for (String type : chain) {
				if (modClass.calls.contains(type + " " + name + " " + desc)) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public String mapFieldName(String owner, String name, String descriptor) {
		return this.member(owner, name, descriptor, false);
	}

	@Override
	public String mapInvokeDynamicMethodName(String name, String descriptor) {
		// the method of the functional interface a lambda implements (the call site returns the interface); the
		// method's own descriptor isn't known here, so only a name the table gives once for that interface is used
		Type returned = Type.getReturnType(descriptor);
		if (returned.getSort() != Type.OBJECT) {
			return name;
		}
		Map<String, String> members = this.table.methods.get(returned.getInternalName());
		if (members == null) {
			return name;
		}
		String found = null;
		for (var entry : members.entrySet()) {
			if (entry.getKey().startsWith(name + "(")) {
				if (found != null && !found.equals(entry.getValue())) {
					return name;
				}
				found = entry.getValue();
			}
		}
		return found != null && !this.map(returned.getInternalName()).startsWith(TRANSLATOR) ? found : name;
	}

	private String member(String owner, String name, String desc, boolean method) {
		var todo = new ArrayDeque<String>();
		var seen = new HashSet<String>();
		todo.add(owner);
		String mappedDesc = method ? this.mapMethodDesc(desc) : this.mapDesc(desc);
		while (!todo.isEmpty()) {
			String current = todo.poll();
			if (current == null || !seen.add(current) || isPlatform(current)) {
				continue;
			}
			ClassInfo modClass = this.mod.get(current);
			if (modClass != null) {
				// declared or inherited: what matters is the first class outside the mod; a mixin's members (@Shadow,
				// @Overwrite) are those of the classes it is applied to
				todo.addAll(modClass.mixinTargets);
				todo.addAll(modClass.supertypes());
				continue;
			}
			// a facade the mod class extends keeps the 1.21 name (it bridges the 1.12.2 method to it)
			String facade = this.table.supers.get(current);
			if (facade != null && (this.target.hasMember(facade, name, mappedDesc, method)
					|| this.target.hasTranslatorMember(facade, name, mappedDesc, method))) {
				return name;
			}
			Map<String, String> members = (method ? this.table.methods : this.table.fields).get(current);
			String key = method ? RemapTable.methodKey(name, desc) : name;
			String mappedOwner = this.map(current);
			if (mappedOwner.startsWith(TRANSLATOR) && members != null && members.containsKey(key)
					&& !this.target.hasMember(mappedOwner, name, mappedDesc, method)
					&& this.target.hasMember(mappedOwner, members.get(key), mappedDesc, method)) {
				// a runtime-table name for a Translator method whose 1.21 name has another descriptor there
				// (StringRepresentable.fromEnum returns Codec for the ports, fromEnumCodec the EnumCodec 1.21.1 mods get):
				// the name check below matches by parameter count and would keep the 1.21 name (Cristellib's ConfigType)
				return members.get(key);
			}
			if (mappedOwner.startsWith(TRANSLATOR) && (this.target.hasMember(mappedOwner, name, mappedDesc, method)
					|| this.target.hasTranslatorMember(mappedOwner, name, mappedDesc, method))) {
				// Translator classes keep the 1.21 names (Containers.dropItemStack, not a 1.12.2 name from the table)
				return name;
			}
			if (members != null && members.containsKey(key)) {
				String candidate = members.get(key);
				if (mappedOwner.startsWith(TRANSLATOR) || this.target.hasMember(mappedOwner, candidate, mappedDesc, method)) {
					return candidate;
				}
				// a field of another type there (HumanoidModel.head is a ModelPart, ModelBiped.bipedHead a ModelRenderer):
				// the table's name; MethodGuard reads it as the 1.12.2 type and casts it back (12 mods' models read
				// their humanoid parts so, and found nothing under the 1.21 name)
				if (!method && this.target.hasFieldNamed(mappedOwner, candidate)) {
					return candidate;
				}
			}
			// the same name on the target (Translator classes keep the 1.21 names; Forge kept many NeoForge names)
			if (this.target.hasMember(mappedOwner, name, mappedDesc, method)) {
				return name;
			}
			String fallback = (method ? this.table.methodNames : this.table.fieldNames).get(name);
			if (fallback != null && this.target.hasMember(mappedOwner, fallback, mappedDesc, method)) {
				return fallback;
			}
			// inherited from a 1.12.2 supertype (NeoForge SlotItemHandler inherits Slot.set): the table rows of its 1.21 names
			ClassInfo targetClass = this.target.get(mappedOwner);
			if (targetClass != null) {
				this.targetSupertypes(targetClass, todo, new HashSet<>());
			}
		}
		return name;
	}

	/**
	 * The 1.21 names of a 1.12.2 class's supertypes; one without a 1.21 name (1.12.2 Gui: its name is the 1.21 HUD, whose
	 * tick is GuiIngame.updateTick, so Create's buttons lost their tick) is passed for its own supertypes.
	 */
	private void targetSupertypes(ClassInfo targetClass, ArrayDeque<String> todo, Set<String> seen) {
		for (String supertype : targetClass.supertypes()) {
			if (!seen.add(supertype)) {
				continue;
			}
			var names = this.table.namesOf(supertype);
			if (!names.isEmpty()) {
				todo.addAll(names);
				continue;
			}
			ClassInfo next = this.target.get(supertype);
			if (next != null) {
				this.targetSupertypes(next, todo, seen);
			}
		}
	}
}
