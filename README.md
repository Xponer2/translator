# Translator

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg?style=flat-square)](LICENSE)
[![Status: prototype](https://img.shields.io/badge/status-prototype-orange.svg?style=flat-square)](#where-it-is)
[![NeoForge 1.21.1 to 1.20.4](https://img.shields.io/badge/NeoForge-1.21.1%20%E2%86%92%201.20.4-green.svg?style=flat-square)](#how-a-step-works)
![Made with AI (Claude)](https://img.shields.io/badge/made%20with-AI%20(Claude)-8A2BE2?style=flat-square)

Run Minecraft mods made for **newer versions on older versions**, and mods of **different versions together** in one
game: a target version with any number of mods made for the versions above it.

The Translator steps a mod down a **ladder of versions**, one step at a time between neighbouring versions
(1.21.1 → 1.21 → 1.20.6 → 1.20.4 → …). Each mod enters at its own version, so mods of different versions can meet on
the same target. Most of each step is **generated** from the official mappings; code is **written by hand only where
Minecraft really changed**, once for every mod and every lower version.

**Made with AI:** the Translator is made entirely with AI (Claude), from the code to the in-game testing.

**Status: prototype.** The first steps, NeoForge 1.21.1 → 1.20.4, are being built and checked against the real game.
There is no release for players yet.

## How a step works

1. **Generated renames.** For each pair of neighbouring versions, Mojang's official mappings of both are joined through
   Fabric's intermediary names (the same in every version). A class or member whose Mojang name changed is found
   that way; the rest keep their names.
2. **Carried code.** A class or static method that is new in the upper version is copied down as it is when its own
   code works on the lower version: it needs only other carried code, reaches only public members below (a private
   static helper of its own class comes along), registers nothing, is no event (the lower game never posts it) and
   keeps the lower version's interface contracts. All carried code goes into one library per target
   (`ladder-carried-1.20.4.jar`, a game library in its own package tree), shared by every converted mod.
3. **Generated bridges.** A member whose types changed between the versions, every change a known conversion
   (`Holder<X>` / `X`, `MapCodec` / `Codec`, `HolderLookup.Provider` / `RegistryAccess`, `ResourceKey<X>` / `X`,
   `ResourceKey` / `ResourceLocation`, `X` / `Optional<X>`, a subtype below), gets a generated bridge that converts the
   values and calls the lower member: field reads, method calls, method references and constructors (`new X(...)`
   becomes the bridge's factory). A mod's **override** of such a method gets the lower method generated in its own
   class (it converts the values and calls the mod's method), so the game calls it; a super call goes to the lower
   method through a private helper; a **mixin** into such a method gets its selector moved and its handler wrapped. A
   context parameter the lower version doesn't pass (the registries of 1.20.5's saving and loading) is dropped from
   calls and supplied to overrides. The conversions themselves are written once.
4. **Hand-written adapters only for the roots** (`adapters_1_20_4`, a mod for the real 1.20.4): where an existing class
   gained new state (an `ItemStack`'s data components), a method was split (1.20.5's `useItemOn` / `useWithoutItem`),
   a registry or a loader API changed. The converter applies them from a rules file (`ladder-rules.tsv`): provided
   classes, overrides of newer methods (the mod class gets the adapters' interface, 1.20.4's call sites call it through
   mixins), call redirects. Each rule's target is checked against the built adapters.
5. **One pass.** The renames of all steps compose, so a mod is converted once, whatever its version.
6. **Mojang's libraries are steps too.** DataFixerUpper and Brigadier are walked like the game, each version with its
   own jars (DFU 8 in 1.21, DFU 6 in 1.20.4); the adapters cover what changed, including what the descriptors hide (a
   dispatch codec's types are map codecs in DFU 8, codecs in DFU 6). A call takes the bottom version's kind of its
   owner (DFU 8's `DataResult` is an interface, DFU 6's a class).
7. **The loader.** The newer metadata becomes the older one's; embedded jars are converted with the mod; a mod's entry
   points the older loader can't take (`@Mod(dist = ...)`, several `@Mod` classes, a mod with none) get one generated
   entry class that constructs the mod's own classes as the newer loader would.

**Shared objects.** Mods of different versions must see the same stack, block or entity. Five rules make that so:
one target class per source class for every mod; shared types are mapped to the game's own classes, never copied;
`extends`, super calls and `new` go to a facade, every other use (descriptors, `instanceof`, casts, arrays) to the
game's class; one copy of carried code per target; a member the game's class lacks is called through a dispatcher.
`tools/identity-test` checks them on the JVM (12 cases; without the dispatcher the first new method fails).

**Strict.** A mod counts as converted only when nothing was stubbed, skipped or left unresolved, mixin selectors
included. Formats the converter can't check yet (a mod's own recipe types) are listed apart.

## Where it is

Over the 300 most popular NeoForge 1.21.1 mods on Modrinth:

| Step | Resolved by generated renames | Of the rest, carried down as real code | Places that need hand-written code |
|---|---|---|---|
| 1.21.1 → 1.21 | 99.9% | 75% | 8 |
| 1.21 → 1.20.6 | 97.7% | 66% | 198 |
| 1.20.6 → 1.20.4 (item data components) | 93.2% | 39% | 409 |

**Converted and checked in the real 1.20.4** (dedicated server and client, with the carried library and the adapters):

- **Macaw's Bridges, Fences and Windows** (635 blocks, 641 items, 793 recipes): their blocks, items and recipes are
  recorded on 1.20.4 by the same probes as on the real 1.21.1 and compared: **7948 of 7948 facts as in 1.21.1**
  (hardness, blast resistance, light, collision, drops, block entities, stack sizes, recipe results). A right click
  with an empty hand opens and closes a shutter (its 1.20.5 `useItemOn`, called from 1.20.4 through the adapters); with
  its own item in hand nothing happens, as in 1.21.1.
- **Alternate Current** (redstone): the same redstone line with the mod on and off: the powers are vanilla's both times,
  and the mod's own update path runs (1 + 1 neighbour notifications against vanilla's 106 + 449).
- **Data of the converted mods**: every data file that names a registry entry, a tag, a loot table, a recipe or an
  advancement must be in the running game: **3019 of 3019** load, among them Trek's 901 (structures, template pools,
  processors), Geophilic's 296 (biome features) and Explorify's 131.
- **On the client**: 21 converted mods together (Mouse Tweaks, Resourceful Config, Prism, spark, Cherished Worlds, Searchables,
  Cubes Without Borders, Fast IP Ping, Ambient Environment, Just Enough Characters, Macaw's, Trek and others): the 1.20.4
  client makes a fresh world, enters it, draws it and opens the inventory, with no error.
- **Test mod A** (`testmods/ladder_test_a`, made for NeoForge 1.21.1): mob effects and attributes through holders, a
  branch inside a bridged constructor's arguments, method references (bound, unbound, constructor), overrides of a
  changed method on two levels with super calls, an FML 4 extension point of an ordinary class, DFU 8 codecs (a
  dispatch over map codecs, validation, error messages), holders' names, food values, mixins into a method whose
  parameter changed, a block entity saving and loading through 1.20.5's registry-passing methods: **the same 48 results** natively on 1.21.1 and converted on
  1.20.4. On the client, 1.21's vertex API (`Tesselator.begin`, `addVertex` with its setters in any order, a wrapper
  over two buffers, builders interleaved, posed normals, a missing element, `MeshData`) and `DeltaTracker`: **the same
  15 results, the written vertex bytes included**.
- **Item data components** (the first compatibility module; test mod B, made for 1.21.1): its own component types,
  an item's defaults, the game's own components; values set, updated, removed, copied, saved and read back; custom
  data whose keys clash with the game's; a stream codec; the stack handed to another mod in a chest and read by the
  type's id, across a server restart: **the same 28 results** natively on 1.21.1 and converted on 1.20.4. A stack's
  components live in its NBT (the one place its data is kept), with 1.21's defaults, overrides and removal markers.
- **Network payloads** (the second compatibility module; test mod C, made for 1.21.1): 1.20.5's payload types and
  registrar, stream codecs (`ByteBufCodecs` and the game's own `STREAM_CODEC`s carried down as real code), handlers on
  the main thread, replies, `PacketDistributor`: **the same 25 results** natively on 1.21.1 and converted on 1.20.4,
  in a single-player world and over a real connection to a dedicated server.

- **Mods of two versions share one item** (test mod E): a mod made for 1.20.4 and running as it is, not converted,
  takes the stack a converted 1.21.1 mod (test mod B) left in a chest and changes it with 1.20.4's own API (an
  enchantment, a key of the stack's data, a new name). After a restart both mods read the same stack, and their
  results are those of the same steps on the real 1.21.1 (with E's 1.21.1 twin): **B 41 + 46, E 6 + 6 results equal**.
  The game's own item components 1.20.4 keeps elsewhere (dyed colour, enchantments, an enchanted book's stored ones)
  are read and written where 1.20.4 keeps them, so 1.20.4's code sees them too.
- **Mixins into methods that changed below** (test mod D, made for 1.21.1): every injector of a converted mod is
  checked against the lower game's method bodies, and one whose target moved (a parameter added or dropped, a call
  inside retyped, a parameter's type changed) is repaired by [Sinytra Adapter](https://github.com/Sinytra/Adapter)'s
  dynamic mixin patches with the ladder's value conversions: **the same 12 results** natively on 1.21.1 and converted
  on 1.20.4.

Over the whole corpus, 46 of 300 mods convert with nothing left (strict: overrides of changed methods, jars embedded
outside `META-INF/jarjar` and Mojang's libraries are checked too); the problems left went from 14887 to 10846 (the
network module took the mods held back by payloads from 123 to 74; the mixin check then made 949 hidden ones visible,
injectors that would have injected nothing on 1.20.4, and the repair fixed about 200 of them). What holds the others back most, in order:
mixins into game methods whose parameters changed (the 1.21 render and GUI methods most), item data components (in
code and in loot tables), `HolderLookup.Provider` in saving and loading, recipes on `RecipeInput`, loot tables by key.

1.20.5 has no step of its own: NeoForge only had betas for it, and 1.20.6 is its hotfix, so the step 1.20.6 → 1.20.4
holds its changes.

## Layout

| Path | What |
|---|---|
| `v1_21_1`, `v1_21`, `v1_20_6`, `v1_20_4` | One Gradle project per version (ModDevGradle): the Mojang-named game + NeoForge jar of that version |
| `v1_20_4/src` | `LadderProbe`, `LadderProbeClient`, `LadderFacts`: behaviour tests of converted mods on a real 1.20.4 server and client, and the facts compared with the 1.21.1 reference |
| `adapters_1_20_4` | The hand-written adapters of the step down to 1.20.4 and their rules (`ladder-rules.tsv`) |
| `testmods/ladder_test_a` | A test mod for NeoForge 1.21.1: run on its own version and converted, the results compared |
| `tools/stage0/Stage0.java` | Walks every reference of a mod corpus down the steps; writes per step the generated rows, what stops, what can be carried and the roots left for hand-written code |
| `tools/convert/Convert.java` | Converts a mod (or a folder of mods) down to 1.20.4 in one pass: classes, carried code, bridges, the adapters' rules, mixin selectors, embedded jars, metadata, data (folders, recipe results, loot predicates); strict |
| `tools/identity-test` | The shared-object rules as a bytecode rewrite and a checker |
| `tools/run-probe.sh` | Runs the 1.20.4 server or client with the probes and a watchdog |
| `tools/fetch_mappings.py` | Downloads Mojang's mappings and the intermediary names to `mappings/` (stays on your machine) |

## Running it

Needs a JDK 21, Python 3, Git Bash or another POSIX shell, and an internet connection for the first build (ModDevGradle
downloads Minecraft and NeoForge for each version). Nothing of Minecraft is in this repository.

```bash
./gradlew createMinecraftArtifacts printLoaderJars
python tools/fetch_mappings.py
tools/ladder.sh convert <folder of NeoForge 1.21.1 mod jars> out/converted out/report.tsv
./gradlew :adapters_1_20_4:jar
tools/ladder.sh convert <folder of NeoForge 1.21.1 mod jars> out/converted out/report.tsv
tools/identity-test/run.sh
```

The first conversion makes the carried library the adapters are compiled against; the second applies the adapters'
rules. To run converted mods: put them with `out/converted/ladder-carried-1.20.4.jar` and
`adapters_1_20_4/build/libs/ladder-adapters-1.20.4-*.jar` in `v1_20_4/run-server/mods/` (or `run-client/mods/`) and run
`tools/run-probe.sh server` (or `client`, or `client-world` to make and enter a world first); the results go to
`v1_20_4/run-server/probe.json`, `facts.json` and `data.json` (or `run-client/probe-client.json`).

## Roadmap

| Stage | What | State |
|---|---|---|
| 1 | Generated steps 1.21.1 → 1.20.4, the one-pass converter, strict checks, behaviour tests in the real 1.20.4 | ✅ done |
| 2 | The hand-written parts of 1.20.6 → 1.20.4: block/item method splits, changed types and overrides of them, loader entry points, render buffers, mixins into changed methods, item data components, network payloads, mixins whose targets moved below (done); the game's other item components, recipe inputs, loot keys (next) | 🔨 **now** |
| 3 | Steps down to 1.20.1 (the version with the most mods), then 1.19.2, 1.18.2, 1.16.5 | ⬜ planned |
| 4 | The step down to 1.12.2 | ⬜ planned |
| 5 | Older mods brought up, Fabric mods, targets below 1.12.2 | ⬜ later |

## The first build (discontinued)

The first Translator ran NeoForge 1.21.1 mods on Forge 1.12.2 in one jump, with hand-written bridges for each gap. It
is discontinued: its code is kept in the [`legacy-1.12.2`](https://github.com/Xponer2/translator/tree/legacy-1.12.2)
branch and its last build is on the [releases](https://github.com/Xponer2/translator/releases) page. 1.12.2 comes
back later as a step of the ladder.

## Contributing

Help is welcome: see [CONTRIBUTING.md](CONTRIBUTING.md).

## License

[MIT](LICENSE), © Xponer2: free for anyone to use, change and share, in any project. Nothing of Minecraft is in this
repository: the tables are made on your machine from the official downloads.
