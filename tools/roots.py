"""What holds the corpus back, by mechanism: every problem of every mod (the converter's batch report) goes to one
fundamental mechanism; a mod is free under a set of mechanisms when all its problems are in them and the mods it
requires are free too. Prints, per mechanism, the mods it touches and frees alone, the greedy order (which mechanism
frees the most mods next, dependencies included) and the long tail. Writes the full table as markdown.

usage: python -I tools/roots.py <convert-batch.tsv> <folder of the corpus jars> [out.md]
"""
import collections
import re
import sys
import tomllib
import zipfile
from pathlib import Path

# (key, title, how it can be solved, pattern over a problem's text); the first that matches wins
MECHANISMS = [
    ("mixin_other", "Mixins aimed at other Minecraft versions (optional, multi-version mods)", "converter: honour optional mixins",
     r"matches nothing in 1\.21\.1|becomes several methods"),
    ("components", "Item data components (an ItemStack's new state)", "emulate on NBT + carry the component records",
     r"core/component/|world/item/component/|DataComponent|ItemEnchantments|CustomData|data components in|custom_data|"
     r"ItemStack\.(set|get|has|update|remove|getOrDefault|getComponents|getComponentsPatch|applyComponents|isComponentsPatchEmpty|getPrototype|copyWithCount)\b|"
     r"Item\$Properties\.component|DATA_COMPONENT|item sub-predicate|component\.|FoodProperties|Tool\b"),
    ("network", "1.20.5 network payloads and stream codecs", "emulate on 20.4's payloads + carry StreamCodec",
     r"network/codec/|CustomPacketPayload|PayloadHandlersEvent|PayloadRegistrar|IPayload|STREAM_CODEC|StreamCodec|neoforge/network/|"
     r"RegistryFriendlyByteBuf|streamCodec|common/custom/|ClientboundCustom|ServerboundCustom|DisconnectionDetails|ConnectionProtocol|network/protocol/"),
    ("provider", "Registries passed to saving and loading (HolderLookup.Provider)", "generated: added/removed parameter bridges",
     r"HolderLookup\$Provider|saveAdditional|loadAdditional|getUpdateTag|saveWithoutMetadata|saveWithFullMetadata|saveWithId|saveCustomOnly|"
     r"ItemStack\.(save|parse|parseOptional)|createSerializationContext|Component\$Serializer|SavedData|RegistryOps"),
    ("enchant", "Data-driven enchantments of 1.21", "engine: carry 1.21's enchantment effects and hook them in",
     r"world/item/enchantment/|/enchantment/|Enchantments\.|EnchantmentHelper|ENCHANTMENT"),
    ("recipe", "Recipes on RecipeInput", "emulate RecipeInput over Container + override bridges",
     r"RecipeInput|CraftingInput|SingleRecipeInput|crafting/|RecipeHolder|RecipeManager|Ingredient"),
    ("loot", "Loot tables by key, reloadable registries", "emulate key <-> location + convert loot data",
     r"loot/|LOOT_TABLE|LootTable|ReloadableServerRegistries|reloadableRegistries|lootTable"),
    ("vanilla_content", "Vanilla content new in 1.20.5-1.21 (blocks, items, entities, sounds...)", "engine: register 1.21's content from the player's own 1.21 jar",
     r"missing F net/minecraft/world/(level/block/Blocks|item/Items|entity/EntityType)\.|missing F net/minecraft/sounds/SoundEvents\.|"
     r"missing F net/minecraft/core/particles/ParticleTypes\.|Armadillo|Breeze|Bogged|WindCharge|Crafter|TrialSpawner|Vault|OminousItemSpawner|wolf_variant|WolfVariant"),
    ("render", "1.21 rendering (vertex API, models, render types, particles)", "emulate (adapters) + generated arity bridges",
     r"blaze3d|client/renderer|client/model|ModelPart|RenderType|client/particle|client/resources|BakedModel|texture/|Sheets|"
     r"RenderLevelStageEvent|RegisterShadersEvent|ModelEvent|client/color|EntityRenderer|RenderStateShard"),
    ("gui", "GUI and HUD (DeltaTracker, layers, screens)", "emulate (GUI layers on overlays) + handler bridges",
     r"client/gui|DeltaTracker|LayeredDraw|GuiLayer|RenderGuiLayerEvent|RegisterGuiLayersEvent|client/Minecraft|client/Options|KeyMapping|"
     r"client/multiplayer|Screen|client/player|client/KeyMapping|client/sounds|client/main|client/Camera|client/MouseHandler"),
    ("entity", "Entities: synched data builder, attributes by id, spawning", "emulate + generated bridges",
     r"world/entity/|SynchedEntityData|attributes/|EntityType|MobSpawnType|Leashable|SpawnPlacement"),
    ("worldgen", "World generation and chunks (codec fields, chunk status, structures)", "generated field-write bridges + carry",
     r"levelgen|world/level/chunk|structure|biome|feature|dimension|placement|no bridge for writes|world/level/storage|ChunkHolder|server/level"),
    ("neoforge_api", "Other NeoForge 20.5-21 API (events, config screens, capabilities...)", "emulate: provided classes and event bridges",
     r"net/neoforged/"),
    ("dfu", "DFU / Brigadier members", "adapters (as done for DFU 8 -> 6)",
     r"com/mojang/(serialization|datafixers|brigadier)"),
    ("data_files", "Data files 1.20.4 has no form for", "convert data formats",
     r"data file with no|no 1\.20\.4 form|data/"),
    ("block_item", "Blocks and items (other members)", "generated bridges + adapters",
     r"world/level/block|world/item/|world/level/material|world/food|BlockBehaviour"),
    ("core_misc", "Other game API (core, server, commands, tags, resources...)", "generated bridges + adapters, case by case",
     r"net/minecraft/"),
    ("other", "Other", "case by case", r"."),
]


def mechanism(problem):
    for key, _, _, rx in MECHANISMS:
        if re.search(rx, problem):
            return key
    return "other"


def read_report(path):
    mods = {}
    for line in open(path, encoding="utf-8"):
        parts = line.rstrip("\n").split("\t")
        if len(parts) < 2:
            continue
        probs = [p.strip() for p in parts[4].split(" | ") if p.strip()] if len(parts) > 4 else []
        mods[parts[1]] = [re.sub(r"^in META-INF/[^:]*: ", "", p) for p in probs]
    return mods


def read_deps(folder):
    ids, needs = {}, {}
    for jar in Path(folder).glob("*.jar"):
        try:
            z = zipfile.ZipFile(jar)
            names = z.namelist()
            meta = next((z.read(n).decode("utf-8", "replace") for n in ("META-INF/neoforge.mods.toml", "META-INF/mods.toml") if n in names), None)
            data = tomllib.loads(meta) if meta else {}
        except Exception:
            data = {}
        own = [m.get("modId") for m in data.get("mods", []) if isinstance(m, dict)]
        req = set()
        for owner, lst in (data.get("dependencies") or {}).items():
            for d in (lst if isinstance(lst, list) else [lst]):
                if isinstance(d, dict) and (d.get("mandatory") is True or str(d.get("type", "")).lower() == "required"):
                    req.add(d.get("modId"))
        for i in own:
            ids[i] = jar.name
        needs[jar.name] = req - set(own) - {"minecraft", "neoforge", "forge", "java", None}
    return ids, needs


def free_under(mods, ids, needs, solved):
    """the mods free when the mechanisms in solved are done (their own problems, and their required mods)"""
    own_ok = {m for m, probs in mods.items() if all(mechanism(p) in solved for p in probs)}
    # the greatest set closed under requirements (mods requiring each other are free together)
    free = set(own_ok)
    changed = True
    while changed:
        changed = False
        for m in list(free):
            if not all(d in ids and ids[d] in free for d in needs.get(m, set())):
                free.discard(m)
                changed = True
    return free


def main():
    report, corpus = sys.argv[1], sys.argv[2]
    out = Path(sys.argv[3]) if len(sys.argv) > 3 else None
    mods = read_report(report)
    ids, needs = read_deps(corpus)
    titles = {k: (t, h) for k, t, h, _ in MECHANISMS}
    per_mod = {m: collections.Counter(mechanism(p) for p in probs) for m, probs in mods.items()}
    clean = free_under(mods, ids, needs, set())
    clean_own = {m for m, probs in mods.items() if not probs}
    outside = {m for m in mods if any(d not in ids for d in needs.get(m, set()))}
    lines = []
    w = lines.append
    w(f"# What holds the corpus back\n\n{len(mods)} mods; {len(clean_own)} convert with no problem of their own, "
      f"{len(clean)} of them with every required mod converted too; {len(outside)} require a mod that isn't in the corpus.\n")
    w("| Mechanism | How | Mods touched | Problems | Freed alone (own problems) | Freed alone (with deps) |")
    w("|---|---|---|---|---|---|")
    rows = []
    for key, title, how, _ in MECHANISMS:
        touched = sum(1 for c in per_mod.values() if c[key])
        problems = sum(c[key] for c in per_mod.values())
        alone_own = sum(1 for m, c in per_mod.items() if c and set(c) == {key})
        alone = len(free_under(mods, ids, needs, {key}) - clean)
        rows.append((touched, key))
        w(f"| {title} | {how} | {touched} | {problems} | {alone_own} | {alone} |")
    # greedy: the mechanism that frees the most mods next
    w("\n## Greedy order: each step the mechanism that frees the most mods (dependencies included)\n")
    w("| Step | Mechanism | Mods free after it |")
    w("|---|---|---|")
    solved = set()
    remaining = [k for k, *_ in MECHANISMS]
    step = 0
    while remaining:
        best = max(remaining, key=lambda k: (len(free_under(mods, ids, needs, solved | {k})), sum(per_mod[m][k] for m in mods)))
        solved.add(best)
        remaining.remove(best)
        step += 1
        w(f"| {step} | {titles[best][0]} | {len(free_under(mods, ids, needs, solved))} |")
    # long tail: problems by member, how many mods each
    keys = collections.Counter()
    for m, probs in mods.items():
        for k in {re.sub(r" \(1\.2[^)]*\)$", "", p)[:200] for p in probs}:
            keys[k] += 1
    single = sum(1 for k, n in keys.items() if n == 1)
    w(f"\n## The long tail\n\n{len(keys)} distinct problems; {single} of them in one mod only, "
      f"{sum(1 for n in keys.values() if n >= 5)} in five mods or more.\n")
    only_single = [m for m, probs in mods.items() if probs and all(keys[re.sub(r' \(1\.2[^)]*\)$', '', p)[:200]] == 1 for p in probs)]
    w(f"Mods whose every problem is theirs alone: {len(only_single)}.\n")
    w("\n## Per mod\n\n| Mod | Problems | By mechanism | Requires (not free) |\n|---|---|---|---|")
    for m in sorted(mods, key=lambda x: len(mods[x])):
        c = per_mod[m]
        deps = ", ".join(sorted(d for d in needs.get(m, set()) if not (d in ids and ids[d] in clean)))
        w(f"| {m} | {len(mods[m])} | {', '.join(f'{k} {n}' for k, n in c.most_common())} | {deps} |")
    text = "\n".join(lines) + "\n"
    if out:
        out.write_text(text, encoding="utf-8")
    print(text.split("## Per mod")[0])


if __name__ == "__main__":
    main()
